package com.ps1.netplay.network

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * RealVoipEngine: Low-latency, real-time bidirectional VoIP audio streaming engine.
 * Supports hardware Acoustic Echo Cancellation (AEC), Noise Suppression (NS),
 * LiveKit audio room streaming, and ultra-fast WebSocket audio relay.
 */
object RealVoipEngine {
    private const val TAG = "RealVoipEngine"

    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private const val BUFFER_SIZE_FACTOR = 2

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    private var isRecording = AtomicBoolean(false)
    private var isPlaying = AtomicBoolean(false)
    private var isMuted = AtomicBoolean(false)
    private var isSpeaker = AtomicBoolean(false)

    private var recordJob: Job? = null
    private var playbackJob: Job? = null
    private val voipScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var audioWebSocket: WebSocket? = null
    private var currentRoomId: String = ""

    /**
     * Start real-time bidirectional voice call
     */
    fun startVoipSession(
        context: Context,
        roomId: String,
        isOutgoing: Boolean,
        speakerOn: Boolean = false
    ) {
        stopVoipSession(context)
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

        // 1. Connect to Real-time WebSocket Audio Relay
        connectAudioRelay(context, roomId)

        // 2. Start LiveKit Audio Session if available
        try {
            val myId = CloudflareClient.getCurrentUserId(context)
            LiveKitNetplayManager.connectToRoom(
                context = context,
                roomName = "room_$roomId",
                identity = myId,
                displayName = CloudflareClient.getCurrentUsername(context),
                scope = voipScope,
                onConnected = {
                    LiveKitNetplayManager.setMicrophoneEnabled(!isMuted.get())
                },
                onBinaryReceived = { pcmBytes ->
                    playIncomingAudio(pcmBytes)
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "LiveKit start exception: ${e.message}")
        }

        // 3. Start Native Audio Recording & Playback
        startNativeAudioCaptureAndPlayback(context)
    }

    /**
     * Play incoming raw PCM 16-bit 16kHz audio data with low latency
     */
    fun playIncomingAudio(pcmData: ByteArray) {
        if (pcmData.isNotEmpty() && isPlaying.get()) {
            try {
                audioTrack?.write(pcmData, 0, pcmData.size)
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack write error: ${e.message}")
            }
        }
    }

    /**
     * Start native AudioRecord (microphone) and AudioTrack (speaker/earpiece)
     */
    private fun startNativeAudioCaptureAndPlayback(context: Context) {
        voipScope.launch {
            try {
                val minRecBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_ENCODING)
                val recBufferSize = (minRecBufferSize * BUFFER_SIZE_FACTOR).coerceAtLeast(2048)

                val minTrackBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, AUDIO_ENCODING)
                val trackBufferSize = (minTrackBufferSize * BUFFER_SIZE_FACTOR).coerceAtLeast(2048)

                // Initialize AudioRecord
                try {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        SAMPLE_RATE,
                        CHANNEL_IN,
                        AUDIO_ENCODING,
                        recBufferSize
                    )

                    // Enable Hardware Acoustic Echo Canceler
                    if (AcousticEchoCanceler.isAvailable()) {
                        audioRecord?.audioSessionId?.let { sessionId ->
                            echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                                enabled = true
                            }
                        }
                    }

                    // Enable Hardware Noise Suppressor
                    if (NoiseSuppressor.isAvailable()) {
                        audioRecord?.audioSessionId?.let { sessionId ->
                            noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                                enabled = true
                            }
                        }
                    }

                    audioRecord?.startRecording()
                    isRecording.set(true)
                } catch (e: Exception) {
                    Log.e(TAG, "AudioRecord init failed: ${e.message}")
                }

                // Initialize AudioTrack
                try {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()

                    val audioFormat = AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_OUT)
                        .setEncoding(AUDIO_ENCODING)
                        .build()

                    audioTrack = AudioTrack(
                        audioAttributes,
                        audioFormat,
                        trackBufferSize,
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    )
                    audioTrack?.play()
                    isPlaying.set(true)
                } catch (e: Exception) {
                    Log.e(TAG, "AudioTrack init failed: ${e.message}")
                }

                // Launch Audio Capture Loop (Mic -> Network)
                recordJob = launch {
                    val buffer = ByteArray(640) // 20ms of 16kHz 16-bit PCM
                    while (isActive && isRecording.get()) {
                        val record = audioRecord ?: break
                        val readBytes = record.read(buffer, 0, buffer.size)
                        if (readBytes > 0 && !isMuted.get()) {
                            // Send audio packet via WebSocket relay
                            audioWebSocket?.send(buffer.toByteString(0, readBytes))
                            // Also send via LiveKit binary channel for maximum redundancy
                            LiveKitNetplayManager.publishData(buffer.copyOf(readBytes), reliable = false)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "VoIP capture setup failed: ${e.message}")
            }
        }
    }

    /**
     * Connect to Cloudflare real-time WebSocket audio stream
     */
    private fun connectAudioRelay(context: Context, roomId: String) {
        try {
            val myId = CloudflareClient.getCurrentUserId(context)
            val baseUrl = CloudflareClient.getBaseUrl(context)
                .replace("http://", "ws://")
                .replace("https://", "wss://")
            val wsUrl = "$baseUrl/ws/call_audio_$roomId?userId=$myId"

            val request = Request.Builder().url(wsUrl).build()
            val client = OkHttpClient.Builder().build()

            audioWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "VoIP Audio WebSocket connected for room: $roomId")
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    playIncomingAudio(bytes.toByteArray())
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "VoIP WebSocket failure: ${t.message}")
                    // Reconnect automatically if session still ongoing
                    if (isRecording.get() || isPlaying.get()) {
                        voipScope.launch {
                            delay(1500)
                            if (isRecording.get() || isPlaying.get()) {
                                connectAudioRelay(context, roomId)
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Connect audio relay error: ${e.message}")
        }
    }

    /**
     * Mute / Unmute local microphone
     */
    fun setMute(muted: Boolean) {
        isMuted.set(muted)
        LiveKitNetplayManager.setMicrophoneEnabled(!muted)
    }

    /**
     * Switch Speaker / Earpiece
     */
    fun setSpeaker(context: Context, speaker: Boolean) {
        isSpeaker.set(speaker)
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.isSpeakerphoneOn = speaker
        } catch (e: Exception) {
            Log.w(TAG, "Set speaker error: ${e.message}")
        }
    }

    /**
     * Stop and release all VoIP resources cleanly
     */
    fun stopVoipSession(context: Context) {
        isRecording.set(false)
        isPlaying.set(false)

        recordJob?.cancel()
        recordJob = null
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioWebSocket?.close(1000, "Call ended")
            audioWebSocket = null
        } catch (_: Exception) {}

        try {
            LiveKitNetplayManager.disconnect()
        } catch (_: Exception) {}

        try {
            echoCanceler?.release()
            echoCanceler = null
            noiseSuppressor?.release()
            noiseSuppressor = null
        } catch (_: Exception) {}

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (_: Exception) {}

        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isSpeakerphoneOn = false
            audioManager?.isMicrophoneMute = false
        } catch (_: Exception) {}
    }
}
