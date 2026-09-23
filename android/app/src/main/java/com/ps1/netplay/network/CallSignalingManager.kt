package com.ps1.netplay.network

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.*
import com.ps1.netplay.CallActivity
import com.ps1.netplay.ui.compose.CallHistoryManager
import com.ps1.netplay.ui.compose.CallStatus
import com.ps1.netplay.ui.compose.RealCallRecord
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CallState {
    CONNECTING,   // "يجري الاتصال..."
    RINGING,      // "يرن..."
    CONNECTED,    // "متصل الآن"
    BUSY,         // "الخط مشغول"
    NO_ANSWER,    // "لا يوجد رد / غير متاح"
    DECLINED,     // "تم رفض المكالمة"
    ENDED         // "انتهت المكالمة"
}

data class ActiveCallSession(
    val roomId: String,
    val targetUserId: String,
    val targetUserName: String,
    val targetUserAvatar: String,
    val isVideo: Boolean,
    val isOutgoing: Boolean,
    val startTimeMs: Long = System.currentTimeMillis()
)

object CallSignalingManager {
    private const val TAG = "CallSignalingManager"

    // Observable states for Jetpack Compose
    var currentSession by mutableStateOf<ActiveCallSession?>(null)
    var callState by mutableStateOf(CallState.CONNECTING)
    var callDurationSeconds by mutableIntStateOf(0)
    var isMuted by mutableStateOf(false)
    var isSpeakerOn by mutableStateOf(false)
    var isCameraOn by mutableStateOf(false)
    var endCallNoticeMessage by mutableStateOf("")

    private var toneGenerator: ToneGenerator? = null
    private var ringbackJob: Job? = null
    private var durationJob: Job? = null
    private var signalingJob: Job? = null
    private var incomingRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Start an outgoing call (Caller side)
     */
    fun startOutgoingCall(
        context: Context,
        targetUserId: String,
        targetUserName: String,
        targetUserAvatar: String,
        isVideo: Boolean,
        roomId: String = "call_${System.currentTimeMillis()}"
    ) {
        stopAllSounds(context)

        val session = ActiveCallSession(
            roomId = roomId,
            targetUserId = targetUserId,
            targetUserName = targetUserName,
            targetUserAvatar = targetUserAvatar,
            isVideo = isVideo,
            isOutgoing = true
        )
        currentSession = session
        callState = CallState.CONNECTING
        callDurationSeconds = 0
        isMuted = false
        isSpeakerOn = isVideo
        isCameraOn = isVideo
        endCallNoticeMessage = ""

        // Check target online status: If target is online, quickly switch to RINGING, else stay in CONNECTING
        CloudflareClient.checkUserOnline(context, targetUserId) { isOnline ->
            if (isOnline && callState == CallState.CONNECTING) {
                callState = CallState.RINGING
            }
        }

        // Start Caller Ringback Tone (نغمة انتظار / رنين للمتصل)
        startCallerRingbackTone()

        // Send initial call invite signal to recipient
        sendSignal(
            context = context,
            targetUserId = targetUserId,
            roomId = roomId,
            isVideo = isVideo,
            signalType = "call_init"
        )

        // Start signaling poll loop
        startSignalingPoller(context, roomId, targetUserId, isOutgoing = true)

        // Set call timeout (40 seconds for no-answer)
        scope.launch {
            delay(40000)
            if (callState == CallState.CONNECTING || callState == CallState.RINGING) {
                endCallWithReason(context, CallState.NO_ANSWER, "لا يوجد رد")
            }
        }
    }

    /**
     * Answer an incoming call (Recipient side)
     */
    fun acceptIncomingCall(
        context: Context,
        callerId: String,
        callerName: String,
        callerAvatar: String,
        roomId: String,
        isVideo: Boolean
    ) {
        stopAllSounds(context)

        val session = ActiveCallSession(
            roomId = roomId,
            targetUserId = callerId,
            targetUserName = callerName,
            targetUserAvatar = callerAvatar,
            isVideo = isVideo,
            isOutgoing = false
        )
        currentSession = session
        callState = CallState.CONNECTED
        callDurationSeconds = 0
        isMuted = false
        isSpeakerOn = isVideo
        isCameraOn = isVideo
        endCallNoticeMessage = ""

        // Send acceptance signal to caller
        sendSignal(
            context = context,
            targetUserId = callerId,
            roomId = roomId,
            isVideo = isVideo,
            signalType = "call_accepted"
        )

        // Start live duration counter only when CONNECTED!
        startLiveCallTimer()

        // Start signaling poller to listen for call_ended
        startSignalingPoller(context, roomId, callerId, isOutgoing = false)
    }

    /**
     * Decline an incoming call
     */
    fun declineIncomingCall(
        context: Context,
        callerId: String,
        roomId: String,
        isVideo: Boolean
    ) {
        stopAllSounds(context)
        sendSignal(
            context = context,
            targetUserId = callerId,
            roomId = roomId,
            isVideo = isVideo,
            signalType = "call_declined"
        )
        // Log missed/declined call
        logCallRecord(context, callerId, "مكالمة واردة مرفوضة", isVideo, CallStatus.MISSED)
        resetCallState()
    }

    /**
     * Acknowledge incoming call and notify caller that device is ringing
     */
    fun sendRingingAck(context: Context, callerId: String, roomId: String, isVideo: Boolean) {
        sendSignal(
            context = context,
            targetUserId = callerId,
            roomId = roomId,
            isVideo = isVideo,
            signalType = "call_ringing"
        )
    }

    /**
     * End active call normally
     */
    fun endCall(context: Context, onComplete: (() -> Unit)? = null) {
        val session = currentSession
        val duration = callDurationSeconds
        val formattedTime = formatDuration(duration)

        stopAllSounds(context)

        if (session != null) {
            // Send end signal to other party with duration
            sendSignal(
                context = context,
                targetUserId = session.targetUserId,
                roomId = session.roomId,
                isVideo = session.isVideo,
                signalType = "call_ended",
                extraInfo = formattedTime
            )

            // Log call record in history
            val status = if (session.isOutgoing) CallStatus.OUTGOING else CallStatus.INCOMING
            val title = if (session.isVideo) "مكالمة فيديو" else "مكالمة صوتية"
            logCallRecord(
                context,
                session.targetUserName,
                "$title • $formattedTime",
                session.isVideo,
                status
            )
        }

        callState = CallState.ENDED
        endCallNoticeMessage = "انتهت المكالمة • $formattedTime"

        scope.launch {
            delay(1200)
            resetCallState()
            onComplete?.invoke()
        }
    }

    /**
     * End call with specific reason (No Answer, Declined, Busy)
     */
    fun endCallWithReason(context: Context, reason: CallState, notice: String) {
        val session = currentSession
        stopAllSounds(context)
        callState = reason
        endCallNoticeMessage = notice

        if (session != null) {
            val status = if (reason == CallState.DECLINED) CallStatus.MISSED else CallStatus.MISSED
            logCallRecord(
                context,
                session.targetUserName,
                notice,
                session.isVideo,
                status
            )
        }

        // Play busy / disconnect tone
        try {
            val tone = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
            Handler(Looper.getMainLooper()).postDelayed({
                try { tone.release() } catch (_: Exception) {}
            }, 700)
        } catch (_: Exception) {}

        scope.launch {
            delay(2000)
            resetCallState()
        }
    }

    /**
     * Live duration counter (starts ONLY upon CONNECTED)
     */
    private fun startLiveCallTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (callState == CallState.CONNECTED) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    /**
     * Caller Ringback Tone (نغمة رنين وانتظار المتصل)
     */
    private fun startCallerRingbackTone() {
        ringbackJob?.cancel()
        ringbackJob = scope.launch(Dispatchers.IO) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 75)
                while (isActive && (callState == CallState.CONNECTING || callState == CallState.RINGING)) {
                    // Standard telephone ringback tone: 1.5s tone, 3s silence
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1500)
                    delay(4500)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Tone generator error: ${e.message}")
            } finally {
                try {
                    toneGenerator?.stopTone()
                    toneGenerator?.release()
                    toneGenerator = null
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Stop all audio and vibration
     */
    fun stopAllSounds(context: Context) {
        ringbackJob?.cancel()
        ringbackJob = null
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}

        try {
            incomingRingtone?.stop()
            incomingRingtone = null
        } catch (_: Exception) {}

        try {
            val vib = vibrator ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vib?.cancel()
        } catch (_: Exception) {}
    }

    /**
     * Start incoming ringtone on recipient device
     */
    fun startRecipientRingtone(context: Context) {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            incomingRingtone = RingtoneManager.getRingtone(context, alertUri)
            incomingRingtone?.play()

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1200, 1000, 1200), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 1200, 1000, 1200), 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Recipient ringtone error: ${e.message}")
        }
    }

    /**
     * Real-time signal poller
     */
    private fun startSignalingPoller(
        context: Context,
        roomId: String,
        targetUserId: String,
        isOutgoing: Boolean
    ) {
        signalingJob?.cancel()
        signalingJob = scope.launch {
            val myId = CloudflareClient.getCurrentUserId(context)
            while (isActive && currentSession != null) {
                CloudflareClient.fetchCloudflareMessages(context, targetUserId) { messages ->
                    val recentSignals = messages.filter { m ->
                        (m.type.startsWith("call_") || m.type == "audio_call" || m.type == "video_call") &&
                        (m.mediaUrl == roomId || m.content.contains(roomId) || (System.currentTimeMillis() - m.createdAt < 60000))
                    }

                    for (sig in recentSignals) {
                        if (sig.senderId == myId) continue // ignore own messages

                        when (sig.type) {
                            "call_ringing" -> {
                                if (callState == CallState.CONNECTING) {
                                    callState = CallState.RINGING
                                }
                            }
                            "call_accepted" -> {
                                if (callState != CallState.CONNECTED) {
                                    stopAllSounds(context)
                                    callState = CallState.CONNECTED
                                    startLiveCallTimer()
                                }
                            }
                            "call_declined" -> {
                                if (callState != CallState.ENDED && callState != CallState.DECLINED) {
                                    endCallWithReason(context, CallState.DECLINED, "تم رفض المكالمة")
                                }
                            }
                            "call_ended" -> {
                                if (callState != CallState.ENDED) {
                                    val durationStr = sig.fileName.ifEmpty { formatDuration(callDurationSeconds) }
                                    endCallWithReason(context, CallState.ENDED, "انتهت المكالمة • $durationStr")
                                }
                            }
                        }
                    }
                }
                delay(1500)
            }
        }
    }

    /**
     * Send signal message over Cloudflare / backend
     */
    private fun sendSignal(
        context: Context,
        targetUserId: String,
        roomId: String,
        isVideo: Boolean,
        signalType: String,
        extraInfo: String = ""
    ) {
        val typeLabel = if (isVideo) "مكالمة فيديو" else "مكالمة صوتية"
        val displayText = when (signalType) {
            "call_init" -> "$typeLabel صادرة"
            "call_ringing" -> "رنين..."
            "call_accepted" -> "تم بدء المكالمة"
            "call_declined" -> "مكالمة مرفوضة"
            "call_ended" -> "$typeLabel • $extraInfo"
            else -> signalType
        }

        CloudflareClient.sendCloudflareMessage(
            context = context,
            receiverId = targetUserId,
            text = displayText,
            type = signalType,
            mediaUrl = roomId,
            fileName = extraInfo
        ) { _, _ -> }
    }

    /**
     * Log call record to SQLite/Prefs
     */
    private fun logCallRecord(
        context: Context,
        name: String,
        timeOrDuration: String,
        isVideo: Boolean,
        status: CallStatus
    ) {
        try {
            val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            CallHistoryManager.addRecord(
                context,
                RealCallRecord(
                    id = "call_rec_${System.currentTimeMillis()}",
                    name = name,
                    avatar = "https://ui-avatars.com/api/?name=$name&background=random",
                    time = "$timeFormatted • $timeOrDuration",
                    isVideo = isVideo,
                    status = status
                )
            )
        } catch (_: Exception) {}
    }

    fun resetCallState() {
        signalingJob?.cancel()
        signalingJob = null
        durationJob?.cancel()
        durationJob = null
        ringbackJob?.cancel()
        ringbackJob = null
        currentSession = null
        callState = CallState.CONNECTING
        callDurationSeconds = 0
        endCallNoticeMessage = ""
    }

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }
}
