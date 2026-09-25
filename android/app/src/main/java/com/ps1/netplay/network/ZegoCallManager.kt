package com.ps1.netplay.network

import android.app.Application
import android.content.Context
import android.util.Log
import com.ps1.netplay.UserManager
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoUpdateType
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoStream
import im.zego.zegoexpress.entity.ZegoUser
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ZegoCallManager: Official Voice & Video Calling Engine for Al-Mahalla.
 * Handles Zego Cloud initialization, room login, audio/video streaming,
 * microphone mute/unmute, speaker switching, camera controls, and clean call lifecycle.
 */
object ZegoCallManager {
    private const val TAG = "ZegoCallManager"
    private const val DEFAULT_APP_ID = 1477087305L
    private const val DEFAULT_APP_SIGN = "29c005b621138958b88eea14c91bd62b2189095171ce962ffe3680974493b41d"

    private var zegoEngine: ZegoExpressEngine? = null
    private val isInitialized = AtomicBoolean(false)
    private var currentRoomId: String = ""
    private val isPublishing = AtomicBoolean(false)
    private val isMicMuted = AtomicBoolean(false)
    private val isSpeakerOn = AtomicBoolean(false)
    private val isCameraOn = AtomicBoolean(false)
    private val isFrontCamera = AtomicBoolean(true)

    var onCallConnected: (() -> Unit)? = null
    var onCallDisconnected: ((String) -> Unit)? = null
    var onRemoteUserJoined: ((String) -> Unit)? = null
    var onRemoteUserLeft: ((String) -> Unit)? = null

    /**
     * Initialize Zego Express Engine
     */
    fun initEngine(context: Context, callback: ((Boolean) -> Unit)? = null) {
        if (isInitialized.get() && zegoEngine != null) {
            callback?.invoke(true)
            return
        }

        CloudflareClient.getZegoConfig(context) { _, appId, appSign ->
            val finalAppId = if (appId > 0L) appId else DEFAULT_APP_ID
            val finalAppSign = if (appSign.isNotEmpty()) appSign else DEFAULT_APP_SIGN

            try {
                val app = context.applicationContext as Application
                val profile = ZegoEngineProfile().apply {
                    this.appID = finalAppId
                    this.appSign = finalAppSign
                    this.scenario = ZegoScenario.DEFAULT
                    this.application = app
                }

                zegoEngine = ZegoExpressEngine.createEngine(profile, object : IZegoEventHandler() {
                    override fun onRoomStateChanged(
                        roomID: String?,
                        reason: ZegoRoomStateChangedReason?,
                        errorCode: Int,
                        extendedData: JSONObject?
                    ) {
                        Log.d(TAG, "Zego room state changed: room=$roomID, reason=$reason, code=$errorCode")
                        if (errorCode == 0 && reason == ZegoRoomStateChangedReason.LOGINED) {
                            onCallConnected?.invoke()
                        } else if (reason == ZegoRoomStateChangedReason.LOGOUT) {
                            onCallDisconnected?.invoke("تم إنهاء جلسة Zego")
                        } else if (errorCode != 0) {
                            Log.w(TAG, "Zego room error: $errorCode")
                        }
                    }

                    override fun onRoomStreamUpdate(
                        roomID: String?,
                        updateType: ZegoUpdateType?,
                        streamList: ArrayList<ZegoStream>?,
                        extendedData: JSONObject?
                    ) {
                        if (streamList == null) return
                        for (stream in streamList) {
                            if (updateType == ZegoUpdateType.ADD) {
                                Log.d(TAG, "Remote Zego stream added: ${stream.streamID}")
                                zegoEngine?.startPlayingStream(stream.streamID)
                                onRemoteUserJoined?.invoke(stream.user.userID)
                            } else if (updateType == ZegoUpdateType.DELETE) {
                                Log.d(TAG, "Remote Zego stream removed: ${stream.streamID}")
                                zegoEngine?.stopPlayingStream(stream.streamID)
                                onRemoteUserLeft?.invoke(stream.user.userID)
                            }
                        }
                    }
                })

                isInitialized.set(true)
                Log.d(TAG, "ZegoExpressEngine successfully initialized with appId=$finalAppId")
                callback?.invoke(true)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize Zego engine: ${e.message}", e)
                callback?.invoke(false)
            }
        }
    }

    /**
     * Start or join a voice or video call session via Zego
     */
    fun startCall(
        context: Context,
        roomId: String,
        userId: String,
        userName: String,
        isVideo: Boolean,
        isOutgoing: Boolean,
        onConnected: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        currentRoomId = roomId
        onCallConnected = onConnected

        initEngine(context) { success ->
            if (!success || zegoEngine == null) {
                onError?.invoke("تعذر الاتصال بخوادم Zego للمكالمات")
                return@initEngine
            }

            try {
                val engine = zegoEngine ?: return@initEngine
                val user = ZegoUser(userId, userName)
                val roomConfig = ZegoRoomConfig().apply {
                    isUserStatusNotify = true
                }

                // 1. Audio routing
                engine.setAudioRouteToSpeaker(isVideo)
                isSpeakerOn.set(isVideo)

                // 2. Microphone
                engine.muteMicrophone(false)
                isMicMuted.set(false)

                // 3. Camera: strictly enable only for video calls
                engine.enableCamera(isVideo)
                isCameraOn.set(isVideo)

                // 4. Log in to Zego Room
                engine.loginRoom(roomId, user, roomConfig)

                // 5. Start publishing audio/video stream
                val streamId = "stream_${roomId}_${userId}"
                engine.startPublishingStream(streamId)
                isPublishing.set(true)

                Log.d(TAG, "Zego call initiated: room=$roomId, isVideo=$isVideo, streamId=$streamId")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start Zego room session: ${e.message}", e)
                onError?.invoke(e.message ?: "خطأ في بدء جلسة Zego")
            }
        }
    }

    /**
     * Mute / Unmute microphone
     */
    fun setMicrophoneMute(muted: Boolean) {
        isMicMuted.set(muted)
        try {
            zegoEngine?.muteMicrophone(muted)
        } catch (e: Exception) {
            Log.w(TAG, "setMicrophoneMute error: ${e.message}")
        }
    }

    /**
     * Enable / Disable speakerphone
     */
    fun setSpeakerEnabled(context: Context, enabled: Boolean) {
        isSpeakerOn.set(enabled)
        try {
            zegoEngine?.setAudioRouteToSpeaker(enabled)
        } catch (e: Exception) {
            Log.w(TAG, "setSpeakerEnabled error: ${e.message}")
        }
    }

    /**
     * Enable / Disable camera
     */
    fun setCameraEnabled(enabled: Boolean) {
        isCameraOn.set(enabled)
        try {
            zegoEngine?.enableCamera(enabled)
        } catch (e: Exception) {
            Log.w(TAG, "setCameraEnabled error: ${e.message}")
        }
    }

    /**
     * Switch front / back camera
     */
    fun switchCamera() {
        val next = !isFrontCamera.get()
        isFrontCamera.set(next)
        try {
            zegoEngine?.useFrontCamera(next)
        } catch (e: Exception) {
            Log.w(TAG, "switchCamera error: ${e.message}")
        }
    }

    /**
     * Leave call and clean up all room session resources
     */
    fun endCall(context: Context, roomId: String = currentRoomId) {
        try {
            val engine = zegoEngine
            if (engine != null) {
                if (isPublishing.get()) {
                    engine.stopPublishingStream()
                    isPublishing.set(false)
                }
                engine.enableCamera(false)
                engine.muteMicrophone(true)
                if (roomId.isNotEmpty()) {
                    engine.logoutRoom(roomId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error leaving Zego room: ${e.message}")
        } finally {
            currentRoomId = ""
            onCallConnected = null
            onCallDisconnected = null
            onRemoteUserJoined = null
            onRemoteUserLeft = null
        }
    }
}
