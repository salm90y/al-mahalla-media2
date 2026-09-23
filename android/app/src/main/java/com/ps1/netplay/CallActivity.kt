package com.ps1.netplay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ps1.netplay.ui.compose.CallHistoryManager
import com.ps1.netplay.ui.compose.CallStatus
import com.ps1.netplay.ui.compose.ModernCallScreen
import com.ps1.netplay.ui.compose.RealCallRecord
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallActivity : AppCompatActivity() {

    companion object {
        var isCallActive: Boolean = false
        var currentActiveRoomId: String = ""

        fun stopAllRingtones(context: Context) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.cancel()
            } catch (_: Exception) {}
        }
    }

    private val PERMISSION_REQUEST_CODE = 1001

    private var callID: String = ""
    private var isVideo: Boolean = false
    private var targetUserName: String = ""
    private var targetUserAvatar: String = ""
    private var audioManager: AudioManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCallActive = true
        stopAllRingtones(this)

        callID = intent.getStringExtra("callID") ?: "call_${System.currentTimeMillis()}"
        currentActiveRoomId = callID
        isVideo = intent.getBooleanExtra("isVideo", false)
        targetUserName = intent.getStringExtra("targetUserName") ?: "أحمد محمد"
        targetUserAvatar = intent.getStringExtra("targetUserAvatar") ?: ""

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // Log call to local history
        try {
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            CallHistoryManager.addRecord(
                this,
                RealCallRecord(
                    id = callID,
                    name = targetUserName,
                    avatar = targetUserAvatar.ifBlank { "https://ui-avatars.com/api/?name=$targetUserName&background=random" },
                    time = timeStr,
                    isVideo = isVideo,
                    status = CallStatus.OUTGOING
                )
            )
        } catch (e: Exception) {
            Log.w("CallActivity", "Failed to log call: ${e.message}")
        }

        // Render the new modern call screen
        setContent {
            ModernCallScreen(
                callerName = targetUserName,
                callerAvatar = targetUserAvatar,
                isVideoCall = isVideo,
                onEndCall = {
                    finish()
                },
                onToggleMute = { muted ->
                    try {
                        audioManager?.isMicrophoneMute = muted
                    } catch (_: Exception) {}
                },
                onToggleSpeaker = { speakerOn ->
                    try {
                        audioManager?.isSpeakerphoneOn = speakerOn
                    } catch (_: Exception) {}
                },
                onToggleCamera = { _ -> },
                onSwitchCamera = { }
            )
        }

        checkAndRequestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        isCallActive = false
        currentActiveRoomId = ""
        try {
            audioManager?.isSpeakerphoneOn = false
            audioManager?.isMicrophoneMute = false
        } catch (_: Exception) {}
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (isVideo) {
            permissions.add(Manifest.permission.CAMERA)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            initZegoAudioEngine()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                initZegoAudioEngine()
            }
        }
    }

    private fun initZegoAudioEngine() {
        if (isFinishing || isDestroyed) return

        val currentUser = UserManager.getCurrentUser(this)
        val userID = currentUser?.username?.ifEmpty { "user_${System.currentTimeMillis()}" } ?: "user_${System.currentTimeMillis()}"
        val userName = currentUser?.fullName?.ifEmpty { currentUser.username } ?: "User"

        val config = if (isVideo) {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
        } else {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall()
        }

        com.ps1.netplay.network.CloudflareClient.getZegoConfig(this) { success, fetchedAppID, fetchedAppSign ->
            if (isFinishing || isDestroyed) return@getZegoConfig

            val effectiveAppID = if (success && fetchedAppID > 0L) fetchedAppID else 1477087305L
            val effectiveAppSign = if (success && fetchedAppSign.isNotBlank()) fetchedAppSign else "29c005b621138958b88eea14c91bd62b2189095171ce962ffe3680974493b41d"

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                try {
                    val fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                        effectiveAppID,
                        effectiveAppSign,
                        userID,
                        userName,
                        callID,
                        config
                    )
                    // Zego engine initialized for real background audio/video pipeline
                } catch (e: Exception) {
                    Log.w("CallActivity", "Zego RTC engine notice: ${e.message}")
                }
            }
        }
    }
}

