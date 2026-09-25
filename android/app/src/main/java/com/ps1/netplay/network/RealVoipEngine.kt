package com.ps1.netplay.network

import android.content.Context
import android.media.AudioManager
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * RealVoipEngine: Audio device coordinator for voice and video calling.
 * Cooperates with Zego Express Calling Engine for hardware AEC, NS, and ultra-low latency audio.
 */
object RealVoipEngine {
    private const val TAG = "RealVoipEngine"

    private val isMuted = AtomicBoolean(false)
    private val isSpeaker = AtomicBoolean(false)
    private var currentRoomId: String = ""

    /**
     * Start voice session: configure AudioManager and coordinate with Zego
     */
    fun startVoipSession(
        context: Context,
        roomId: String,
        isOutgoing: Boolean,
        speakerOn: Boolean = false
    ) {
        currentRoomId = roomId
        isSpeaker.set(speakerOn)
        isMuted.set(false)

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = speakerOn
            audioManager?.isMicrophoneMute = false
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting audio manager mode: ${e.message}")
        }
    }

    /**
     * Re-check audio state
     */
    fun ensureAudioCaptureStarted(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isMicrophoneMute = isMuted.get()
            audioManager?.isSpeakerphoneOn = isSpeaker.get()
        } catch (_: Exception) {}
    }

    /**
     * Mute / Unmute local microphone
     */
    fun setMute(muted: Boolean) {
        isMuted.set(muted)
        ZegoCallManager.setMicrophoneMute(muted)
    }

    /**
     * Switch Speaker / Earpiece
     */
    fun setSpeaker(context: Context, speaker: Boolean) {
        isSpeaker.set(speaker)
        ZegoCallManager.setSpeakerEnabled(context, speaker)
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.isSpeakerphoneOn = speaker
        } catch (e: Exception) {
            Log.w(TAG, "Set speaker error: ${e.message}")
        }
    }

    /**
     * Stop and release audio device settings
     */
    fun stopVoipSession(context: Context) {
        isMuted.set(false)
        isSpeaker.set(false)
        currentRoomId = ""

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isSpeakerphoneOn = false
            audioManager?.isMicrophoneMute = false
        } catch (_: Exception) {}
    }
}
