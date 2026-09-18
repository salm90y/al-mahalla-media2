package com.ps1.netplay.network

import android.content.Context
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.DataPublishReliability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object LiveKitNetplayManager {

    private const val TAG = "LiveKitNetplay"

    private var activeRoom: Room? = null

    private var eventsJob: Job? = null

    var onActiveSpeakersChanged: ((List<String>) -> Unit)? = null

    fun connectToRoom(

        context: Context,

        roomName: String,

        identity: String,

        displayName: String,

        scope: CoroutineScope,

        onConnected: (() -> Unit)? = null,

        onDataReceived: ((String) -> Unit)? = null,

        onError: ((String) -> Unit)? = null,

        onBinaryReceived: ((ByteArray) -> Unit)? = null

    ) {

        disconnect()

        scope.launch(Dispatchers.IO) {

            try {

                val room = LiveKit.create(context.applicationContext)

                activeRoom = room

                eventsJob = launch {

                    room.events.collect { event ->

                        when (event) {

                            is RoomEvent.ActiveSpeakersChanged -> {

                                val speakers = event.speakers.mapNotNull { it.name }

                                onActiveSpeakersChanged?.invoke(speakers)

                            }

                            is RoomEvent.DataReceived -> {

                                try {

                                    val data = event.data

                                    if (data.size >= 2 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && onBinaryReceived != null) {

                                        onBinaryReceived(data)

                                    } else {

                                        val text = String(data, Charsets.UTF_8)

                                        onDataReceived?.invoke(text)

                                    }

                                } catch (e: Exception) {

                                    Log.e(TAG, "Error decoding LiveKit data: ${e.message}")

                                }

                            }

                            is RoomEvent.Disconnected -> {

                                Log.d(TAG, "LiveKit room disconnected: ${event.error?.message}")

                            }

                            else -> {}

                        }

                    }

                }

                CloudflareClient.getLiveKitToken(context, roomName, false) { serverUrl, token ->

                    if (token != null) {

                        scope.launch(Dispatchers.IO) {

                            try {

                                Log.d(TAG, "Connecting to LiveKit room: $roomName ...")

                                room.connect(serverUrl ?: "wss://ps1-netplay.livekit.cloud", token)

                                Log.d(TAG, "Successfully connected to LiveKit room: $roomName")

                                onConnected?.invoke()

                            } catch (e: Exception) {

                                onError?.invoke(e.message ?: "فشل الاتصال بـ LiveKit")

                            }

                        }

                    } else {

                        onError?.invoke("فشل جلب رمز الدخول")

                    }

                }

            } catch (e: Exception) {

                Log.e(TAG, "LiveKit connection failed: ${e.message}", e)

                onError?.invoke(e.message ?: "فشل الاتصال بـ LiveKit")

            }

        }

    }

    fun publishData(message: String) {

        publishData(message.toByteArray(Charsets.UTF_8), reliable = true)

    }

    fun publishData(bytes: ByteArray, reliable: Boolean = true) {

        val room = activeRoom ?: return

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val rel = if (reliable) DataPublishReliability.RELIABLE else DataPublishReliability.LOSSY

                room.localParticipant.publishData(bytes, rel)

            } catch (e: Exception) {

                Log.e(TAG, "Failed to publish data: ${e.message}")

            }

        }

    }

    var isMicEnabled: Boolean = false

        private set

    fun setMicrophoneEnabled(enabled: Boolean) {

        val room = activeRoom ?: return

        isMicEnabled = enabled

        CoroutineScope(Dispatchers.IO).launch {

            try {

                room.localParticipant.setMicrophoneEnabled(enabled)

            } catch (e: Exception) {

                Log.e(TAG, "Failed to set microphone: ${e.message}")

            }

        }

    }

    fun setParticipantMuted(identity: String, muted: Boolean) {

        val room = activeRoom ?: return

        CoroutineScope(Dispatchers.IO).launch {

            try {

                // UI handles mutable state

            } catch (e: Exception) {

                Log.e(TAG, "Failed to mute participant: ${e.message}")

            }

        }

    }

    fun disconnect() {

        try {

            eventsJob?.cancel()

            eventsJob = null

            activeRoom?.disconnect()

            activeRoom = null

        } catch (e: Exception) {

            Log.e(TAG, "Error disconnecting: ${e.message}")

        }

    }
}
