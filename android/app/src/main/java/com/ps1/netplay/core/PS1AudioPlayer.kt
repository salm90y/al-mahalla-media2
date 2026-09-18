package com.ps1.netplay.core
import android.widget.*
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log

/** * High-performance, low-latency Native Audio Output for PlayStation 1 emulation. * Streams 44.1kHz 16-bit Stereo PCM samples directly from Libretro C++ audio ring-buffer. */class PS1AudioPlayer {
    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    private var audioThread: Thread? = null
    private val pcmBuffer = ShortArray(2048)
var currentVolume: Float = 1.0f
        private set
    var isMuted: Boolean = false
        private set
    @Synchronized
fun start() {
        if (isPlaying) return
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(8192)
val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(44100)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufSize * 4)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    44100,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize * 4,
                    AudioTrack.MODE_STREAM
                )
            }
            track.play()
            audioTrack = track
            isPlaying = true
            audioThread = Thread({
                while (isPlaying) {
                    val readCount = NativeCoreBridge.safeReadAudio(pcmBuffer, pcmBuffer.size)
                    if (readCount > 0) {
                        try {
                            audioTrack?.write(pcmBuffer, 0, readCount)
                        } catch (e: Throwable) {
                            Log.e(TAG, "Audio write error", e)
                        }
                    } else {
                        try {
                            Thread.sleep(1)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }
            }, "PS1-AudioStream").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
            Log.i(TAG, "PS1 Audio engine started successfully (44.1kHz Stereo)")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize audio engine", t)
        }
    }
fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        currentVolume = clamped
        NativeCoreBridge.safeSetVolume(clamped)
    }
fun setMuted(muted: Boolean) {
        isMuted = muted
        NativeCoreBridge.safeSetMute(muted)
    }
    @Synchronized
fun stop() {
        isPlaying = false
        audioThread?.interrupt()
        audioThread = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioTrack release notice", t)
        }
        audioTrack = null
        NativeCoreBridge.safeResetAudio()
        Log.i(TAG, "PS1 Audio engine stopped")
    }
    companion object {
        private const val TAG = "PS1AudioPlayer"
    }}