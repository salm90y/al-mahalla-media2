package com.ps1.netplay.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
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
import androidx.core.app.NotificationCompat
import com.ps1.netplay.CallActivity
import com.ps1.netplay.UserManager
import com.ps1.netplay.ui.compose.CallHistoryManager
import com.ps1.netplay.ui.compose.CallStatus
import com.ps1.netplay.ui.compose.IncomingCallData
import com.ps1.netplay.ui.compose.RealCallRecord
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CallState {
    IDLE,
    OUTGOING,
    RINGING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ENDED,
    REJECTED,
    BUSY,
    TIMEOUT,
    FAILED;

    companion object {
        val DECLINED get() = REJECTED
        val NO_ANSWER get() = TIMEOUT
    }
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
    private const val NOTIFICATION_CHANNEL_ID = "almahalla_calls_channel"
    private const val CALL_NOTIFICATION_ID = 998811

    // Observable states for Jetpack Compose
    var currentSession by mutableStateOf<ActiveCallSession?>(null)
    var callState by mutableStateOf(CallState.IDLE)
    var callDurationSeconds by mutableIntStateOf(0)
    var isMuted by mutableStateOf(false)
    var isSpeakerOn by mutableStateOf(false)
    var isCameraOn by mutableStateOf(false)
    var endCallNoticeMessage by mutableStateOf("")

    // App-wide Incoming Call Alert State
    var globalIncomingCall by mutableStateOf<IncomingCallData?>(null)
    var dismissedCallIds by mutableStateOf(setOf<String>())
    private val handledCallActions = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private var toneGenerator: ToneGenerator? = null
    private var ringbackJob: Job? = null
    private var durationJob: Job? = null
    private var signalingJob: Job? = null
    private var globalWatcherJob: Job? = null
    private var incomingRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Start Global Incoming Call Watcher across the entire application
     */
    fun startGlobalIncomingCallWatcher(context: Context) {
        if (globalWatcherJob != null && globalWatcherJob?.isActive == true) return

        createCallNotificationChannel(context)

        globalWatcherJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val myUser = UserManager.getCurrentUser(context)
                    val myId = myUser?.username ?: CloudflareClient.getCurrentUserId(context)

                    if (myId.isNotBlank() && !CallActivity.isCallActive) {
                        CloudflareClient.checkIncomingCall(context) { callObj ->
                            if (callObj != null) {
                                val roomId = callObj.optString("room_id", "")
                                val callerId = callObj.optString("caller_id", "")
                                val callerName = callObj.optString("caller_name", callerId).ifBlank { "مستخدم" }
                                val callerAvatar = callObj.optString("caller_avatar", "")
                                val isVideo = callObj.optBoolean("is_video", false)
                                val createdAt = callObj.optLong("created_at", System.currentTimeMillis())

                                if (roomId.isNotEmpty() && !dismissedCallIds.contains(roomId) && (System.currentTimeMillis() - createdAt < 45000)) {
                                    val incData = IncomingCallData(
                                        callerId = callerId,
                                        callerName = callerName,
                                        callerAvatar = callerAvatar,
                                        callRoomId = roomId,
                                        isVideo = isVideo,
                                        timestamp = createdAt
                                    )

                                    if (globalIncomingCall?.callRoomId != roomId) {
                                        globalIncomingCall = incData
                                        startRecipientRingtone(context)
                                        showIncomingCallNotification(context, incData)
                                        // Acknowledge ringing back to caller
                                        sendRingingAck(context, callerId, roomId, isVideo)
                                    }
                                }
                            } else {
                                if (globalIncomingCall != null && !CallActivity.isCallActive) {
                                    // Call was cancelled or answered elsewhere
                                    globalIncomingCall = null
                                    stopAllSounds(context)
                                    dismissCallNotification(context)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Watcher error: ${e.message}")
                }
                delay(800)
            }
        }
    }

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
        callState = CallState.OUTGOING
        callDurationSeconds = 0
        isMuted = false
        isSpeakerOn = isVideo
        isCameraOn = isVideo
        endCallNoticeMessage = ""

        // Check target online status: If target is online, quickly switch to RINGING
        CloudflareClient.checkUserOnline(context, targetUserId) { isOnline ->
            if (isOnline && (callState == CallState.OUTGOING || callState == CallState.CONNECTING)) {
                callState = CallState.RINGING
            }
        }

        // Start Caller Ringback Tone (نغمة انتظار / رنين للمتصل)
        startCallerRingbackTone()

        val myUser = UserManager.getCurrentUser(context)
        val myId = myUser?.username ?: CloudflareClient.getCurrentUserId(context)
        val myName = myUser?.fullName?.ifEmpty { myId } ?: myId
        val myAvatar = myUser?.avatarUrl ?: ""

        // Send real-time call signal
        CloudflareClient.sendCallSignal(
            context = context,
            callerId = myId,
            callerName = myName,
            callerAvatar = myAvatar,
            receiverId = targetUserId,
            roomId = roomId,
            isVideo = isVideo,
            signalType = "call_init"
        )

        // Start signaling poll loop
        startSignalingPoller(context, roomId, targetUserId, isOutgoing = true)

        // Start Zego Audio & Video Call Engine
        ZegoCallManager.onRemoteUserJoined = { remoteUserId ->
            scope.launch(Dispatchers.Main) {
                if (callState != CallState.CONNECTED) {
                    stopAllSounds(context)
                    callState = CallState.CONNECTED
                    startLiveCallTimer()
                    RealVoipEngine.startVoipSession(
                        context = context,
                        roomId = roomId,
                        isOutgoing = true,
                        speakerOn = isSpeakerOn
                    )
                }
            }
        }

        ZegoCallManager.startCall(
            context = context,
            roomId = roomId,
            userId = myId,
            userName = myName,
            isVideo = isVideo,
            isOutgoing = true,
            onConnected = {
                // Room joined locally, keep state OUTGOING/RINGING until recipient responds
            }
        )

        // Set call timeout (40 seconds for no-answer)
        scope.launch {
            delay(40000)
            if (callState == CallState.OUTGOING || callState == CallState.CONNECTING || callState == CallState.RINGING) {
                endCallWithReason(context, CallState.TIMEOUT, "لا يوجد رد")
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
        val actionKey = "accept_$roomId"
        if (!handledCallActions.add(actionKey) || (currentSession?.roomId == roomId && callState == CallState.CONNECTED)) {
            return
        }

        stopAllSounds(context)
        dismissCallNotification(context)
        globalIncomingCall = null

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

        // Start Zego Call Session immediately
        val myUserId = CloudflareClient.getCurrentUserId(context)
        val myUserName = UserManager.getCurrentUser(context)?.username ?: "user"
        ZegoCallManager.startCall(
            context = context,
            roomId = roomId,
            userId = myUserId,
            userName = myUserName,
            isVideo = isVideo,
            isOutgoing = false,
            onConnected = {
                callState = CallState.CONNECTED
            }
        )

        // Start Real VoIP Audio Engine immediately
        RealVoipEngine.startVoipSession(
            context = context,
            roomId = roomId,
            isOutgoing = false,
            speakerOn = isVideo
        )

        // Send acceptance signal to caller
        CloudflareClient.respondToCall(context, roomId, "accept")
        CloudflareClient.sendCallSignal(
            context = context,
            callerId = CloudflareClient.getCurrentUserId(context),
            callerName = UserManager.getCurrentUser(context)?.username ?: "user",
            callerAvatar = UserManager.getCurrentUser(context)?.avatarUrl ?: "",
            receiverId = callerId,
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
        val actionKey = "decline_$roomId"
        if (!handledCallActions.add(actionKey)) return

        stopAllSounds(context)
        dismissCallNotification(context)
        globalIncomingCall = null
        dismissedCallIds = dismissedCallIds + roomId

        ZegoCallManager.endCall(context, roomId)
        RealVoipEngine.stopVoipSession(context)

        CloudflareClient.respondToCall(context, roomId, "decline")
        CloudflareClient.sendCallSignal(
            context = context,
            callerId = CloudflareClient.getCurrentUserId(context),
            callerName = UserManager.getCurrentUser(context)?.username ?: "user",
            callerAvatar = "",
            receiverId = callerId,
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
        CloudflareClient.respondToCall(context, roomId, "ringing")
        CloudflareClient.sendCallSignal(
            context = context,
            callerId = CloudflareClient.getCurrentUserId(context),
            callerName = UserManager.getCurrentUser(context)?.username ?: "user",
            callerAvatar = "",
            receiverId = callerId,
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
        dismissCallNotification(context)
        ZegoCallManager.endCall(context, session?.roomId ?: "")
        RealVoipEngine.stopVoipSession(context)

        if (session != null) {
            CloudflareClient.respondToCall(context, session.roomId, "end", formattedTime)
            CloudflareClient.sendCallSignal(
                context = context,
                callerId = CloudflareClient.getCurrentUserId(context),
                callerName = UserManager.getCurrentUser(context)?.username ?: "user",
                callerAvatar = "",
                receiverId = session.targetUserId,
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

        signalingJob?.cancel()
        signalingJob = null
        durationJob?.cancel()
        durationJob = null
        ringbackJob?.cancel()
        ringbackJob = null

        onComplete?.invoke()

        scope.launch {
            delay(1200)
            resetCallState()
        }
    }

    /**
     * End call with specific reason (No Answer, Declined, Busy)
     */
    fun endCallWithReason(context: Context, reason: CallState, notice: String) {
        val session = currentSession
        stopAllSounds(context)
        dismissCallNotification(context)
        ZegoCallManager.endCall(context, session?.roomId ?: "")
        RealVoipEngine.stopVoipSession(context)
        callState = reason
        endCallNoticeMessage = notice

        if (session != null) {
            logCallRecord(
                context,
                session.targetUserName,
                notice,
                session.isVideo,
                CallStatus.MISSED
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
            delay(1500)
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
        signalingJob = scope.launch(Dispatchers.IO) {
            while (isActive && currentSession != null) {
                // 1. Direct active call status polling
                CloudflareClient.getCallStatus(context, roomId) { status ->
                    when (status) {
                        "ringing" -> {
                            if (callState == CallState.CONNECTING || callState == CallState.OUTGOING) {
                                callState = CallState.RINGING
                            }
                        }
                        "calling", "waiting", "unknown" -> {
                            // Call is pending / waiting for recipient response
                        }
                        "connected", "accept" -> {
                            if (callState != CallState.CONNECTED) {
                                stopAllSounds(context)
                                callState = CallState.CONNECTED
                                startLiveCallTimer()
                                RealVoipEngine.startVoipSession(
                                    context = context,
                                    roomId = roomId,
                                    isOutgoing = isOutgoing,
                                    speakerOn = isSpeakerOn
                                )
                            }
                        }
                        "declined" -> {
                            if (callState != CallState.ENDED && callState != CallState.DECLINED) {
                                endCallWithReason(context, CallState.DECLINED, "تم رفض المكالمة")
                            }
                        }
                        "ended" -> {
                            val elapsed = System.currentTimeMillis() - (currentSession?.startTimeMs ?: 0L)
                            if (elapsed > 4000L && callState != CallState.ENDED && (callState == CallState.CONNECTED || callState == CallState.RINGING)) {
                                endCallWithReason(context, CallState.ENDED, "انتهت المكالمة")
                            }
                        }
                    }
                }
                delay(500)
            }
        }
    }

    fun createCallNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "المكالمات الواردة"
            val descriptionText = "إشعارات ونغمات المكالمات الصوتية والفيديو"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                setSound(soundUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showIncomingCallNotification(context: Context, callData: IncomingCallData) {
        try {
            val intent = Intent(context, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("callID", callData.callRoomId)
                putExtra("isVideo", callData.isVideo)
                putExtra("isIncoming", true)
                putExtra("targetUserId", callData.callerId)
                putExtra("targetUserName", callData.callerName)
                putExtra("targetUserAvatar", callData.callerAvatar)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val callTypeStr = if (callData.isVideo) "مكالمة فيديو واردة" else "مكالمة صوتية واردة"
            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle(callTypeStr)
                .setContentText(callData.callerName)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setOngoing(true)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(CALL_NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {}
    }

    fun dismissCallNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(CALL_NOTIFICATION_ID)
        } catch (_: Exception) {}
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
        callState = CallState.IDLE
        callDurationSeconds = 0
        endCallNoticeMessage = ""
        handledCallActions.clear()
    }

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }
}
