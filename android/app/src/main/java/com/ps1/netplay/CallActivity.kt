package com.ps1.netplay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ps1.netplay.network.CallSignalingManager
import com.ps1.netplay.ui.compose.ModernCallScreen
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

class CallActivity : AppCompatActivity() {

    companion object {
        var isCallActive: Boolean = false
        var currentActiveRoomId: String = ""

        fun stopAllRingtones(context: Context) {
            CallSignalingManager.stopAllSounds(context)
        }
    }

    private val PERMISSION_REQUEST_CODE = 1001

    private var callID: String = ""
    private var isVideo: Boolean = false
    private var isIncoming: Boolean = false
    private var targetUserId: String = ""
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
        isIncoming = intent.getBooleanExtra("isIncoming", false)
        targetUserId = intent.getStringExtra("targetUserId") ?: intent.getStringExtra("targetUserName") ?: "user"
        targetUserName = intent.getStringExtra("targetUserName") ?: "مستخدم"
        targetUserAvatar = intent.getStringExtra("targetUserAvatar") ?: ""

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // Start Call Signaling Session
        if (isIncoming) {
            CallSignalingManager.acceptIncomingCall(
                context = this,
                callerId = targetUserId,
                callerName = targetUserName,
                callerAvatar = targetUserAvatar,
                roomId = callID,
                isVideo = isVideo
            )
        } else {
            CallSignalingManager.startOutgoingCall(
                context = this,
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                targetUserAvatar = targetUserAvatar,
                isVideo = isVideo,
                roomId = callID
            )
        }

        // Render Modern Call Screen
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
        CallSignalingManager.stopAllSounds(this)
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
                } catch (e: Exception) {
                    Log.w("CallActivity", "Zego RTC engine notice: ${e.message}")
                }
            }
        }
    }
}
