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
        if (pcmData.isEmpty()) return
        try {
            val track = audioTrack ?: return
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                    isPlaying.set(true)
                }
                track.write(pcmData, 0, pcmData.size)
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack write error: ${e.message}")
        }
    }

    /**
     * Re-check and start audio capture if permission was just granted
     */
    fun ensureAudioCaptureStarted(context: Context) {
        if (!isRecording.get()) {
            startNativeAudioCaptureAndPlayback(context)
        }
    }

    /**
     * Start native AudioRecord (microphone) and AudioTrack (speaker/earpiece)
     */
    private fun startNativeAudioCaptureAndPlayback(context: Context) {
        voipScope.launch {
            try {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val minRecBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_ENCODING)
                val recBufferSize = (minRecBufferSize * BUFFER_SIZE_FACTOR).coerceAtLeast(2048)

                val minTrackBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, AUDIO_ENCODING)
                val trackBufferSize = (minTrackBufferSize * BUFFER_SIZE_FACTOR).coerceAtLeast(2048)

                // Initialize AudioTrack for incoming remote voice playback
                try {
                    if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
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
                        if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                            audioTrack?.play()
                            isPlaying.set(true)
                            Log.d(TAG, "AudioTrack initialized and playing successfully")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AudioTrack init failed: ${e.message}")
                }

                // If permission not granted yet, wait for permission callback
                if (!hasPermission) {
                    Log.w(TAG, "RECORD_AUDIO permission not granted yet; waiting for user grant")
                    return@launch
                }

                // Initialize AudioRecord with VOICE_COMMUNICATION, fallback to MIC
                try {
                    audioRecord?.release()
                    audioRecord = null

                    var record = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        SAMPLE_RATE,
                        CHANNEL_IN,
                        AUDIO_ENCODING,
                        recBufferSize
                    )

                    if (record.state != AudioRecord.STATE_INITIALIZED) {
                        Log.w(TAG, "VOICE_COMMUNICATION uninitialized; falling back to MIC source")
                        record.release()
                        record = AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            SAMPLE_RATE,
                            CHANNEL_IN,
                            AUDIO_ENCODING,
                            recBufferSize
                        )
                    }

                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        audioRecord = record

                        // Enable Hardware Acoustic Echo Canceler if available
                        if (AcousticEchoCanceler.isAvailable()) {
                            try {
                                echoCanceler = AcousticEchoCanceler.create(record.audioSessionId)?.apply {
                                    enabled = true
                                }
                            } catch (_: Exception) {}
                        }

                        // Enable Hardware Noise Suppressor if available
                        if (NoiseSuppressor.isAvailable()) {
                            try {
                                noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply {
                                    enabled = true
                                }
                            } catch (_: Exception) {}
                        }

                        record.startRecording()
                        isRecording.set(true)
                        Log.d(TAG, "AudioRecord started recording successfully")
                    } else {
                        Log.e(TAG, "AudioRecord could not be initialized")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AudioRecord start failed: ${e.message}")
                }

                // Launch Audio Capture Loop (Local Mic -> Network to Peer)
                recordJob?.cancel()
                recordJob = launch {
                    val buffer = ByteArray(640) // 20ms of 16kHz 16-bit PCM
                    while (isActive && isRecording.get()) {
                        val record = audioRecord ?: break
                        val readBytes = record.read(buffer, 0, buffer.size)
                        if (readBytes > 0 && !isMuted.get()) {
                            val audioSlice = buffer.toByteString(0, readBytes)
                            // Send audio packet via WebSocket relay directly to other party
                            audioWebSocket?.send(audioSlice)
                            // Also send via LiveKit binary channel for dual redundancy
                            LiveKitNetplayManager.publishData(buffer.copyOf(readBytes), reliable = false)
                        } else if (readBytes <= 0) {
                            delay(10)
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

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        if (text.startsWith("pcm:") || text.startsWith("audio:")) {
                            val raw = text.substringAfter(':')
                            val decoded = android.util.Base64.decode(raw, android.util.Base64.NO_WRAP)
                            playIncomingAudio(decoded)
                        }
                    } catch (_: Exception) {}
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
