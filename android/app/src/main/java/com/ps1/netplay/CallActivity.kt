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
import com.ps1.netplay.ui.compose.NetPlayTheme

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
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = isVideo
        } catch (_: Exception) {}

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

        // Set modern Compose Call Screen matching app's theme and Arabic design
        setContent {
            NetPlayTheme {
                ModernCallScreen(
                    callerName = targetUserName,
                    callerAvatar = targetUserAvatar,
                    isVideoCall = isVideo,
                    onEndCall = {
                        CallSignalingManager.endCall(this@CallActivity) {
                            finish()
                        }
                    },
                    onMinimize = {
                        finish()
                    },
                    onToggleMute = { muted ->
                        try {
                            audioManager?.isMicrophoneMute = muted
                        } catch (_: Exception) {}
                    },
                    onToggleSpeaker = { speaker ->
                        try {
                            audioManager?.isSpeakerphoneOn = speaker
                        } catch (_: Exception) {}
                    }
                )
            }
        }

        checkAndRequestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        isCallActive = false
        currentActiveRoomId = ""
        CallSignalingManager.stopAllSounds(this)
        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
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
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
