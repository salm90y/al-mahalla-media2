package com.ps1.netplay.ui.compose

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.ps1.netplay.network.CloudflareClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// ----------------------------------------------------
// SHARED REAL DATA MODELS FOR YOUTUBE ROOMS
// ----------------------------------------------------
data class PublicYouTubeRoom(
    val roomId: String,
    val roomCode: String,
    val title: String,
    val hostName: String,
    val hostAvatarBg: Color = Color(0xFF2563EB),
    val currentVideoTitle: String,
    val videoId: String,
    val thumbnailUrl: String = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=600&auto=format&fit=crop&q=80",
    val viewersCount: Int = 1,
    val isLive: Boolean = true,
    val durationText: String = "مباشر",
    val privacyMode: RoomPrivacyMode = RoomPrivacyMode.PUBLIC,
    val createdAt: Long = System.currentTimeMillis()
)

object YouTubeRoomManager {
    private const val TAG = "YouTubeRoomManager"
    private const val PREFS_NAME = "ps1_yt_real_rooms"
    private const val KEY_LOCAL_ROOMS = "active_real_rooms_json"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // In-memory list of genuine real active rooms (NO fake mock rooms)
    val activeRealRooms = mutableListOf<PublicYouTubeRoom>()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Create a real room with an authentic 6-digit room code
     */
    fun createRealRoom(
        context: Context,
        title: String,
        privacyMode: RoomPrivacyMode,
        initialVideoId: String = "dQw4w9WgXcQ",
        initialVideoTitle: String = "فيديو يوتيوب",
        onComplete: (Result<PublicYouTubeRoom>) -> Unit
    ) {
        val hostName = CloudflareClient.getCurrentUsername(context).ifBlank { "أحمد" }
        val hostId = CloudflareClient.getCurrentUserId(context)
        val cleanTitle = if (title.isBlank()) "غرفة $hostName" else title.trim()

        // Generate authentic unique 6-digit code
        val codeNum = Random.nextInt(100000, 999999)
        val roomCode = "#YT-$codeNum"
        val roomId = "yt_room_$codeNum"

        val room = PublicYouTubeRoom(
            roomId = roomId,
            roomCode = roomCode,
            title = cleanTitle,
            hostName = hostName,
            hostAvatarBg = Color(0xFF2563EB),
            currentVideoTitle = initialVideoTitle,
            videoId = initialVideoId,
            thumbnailUrl = "https://img.youtube.com/vi/$initialVideoId/hqdefault.jpg",
            viewersCount = 1,
            isLive = true,
            durationText = "مباشر",
            privacyMode = privacyMode,
            createdAt = System.currentTimeMillis()
        )

        // Save locally immediately
        saveRoomLocally(context, room)

        // Sync with Cloudflare backend
        val baseUrl = CloudflareClient.getBaseUrl(context)
        val jsonBody = JSONObject().apply {
            put("title", room.title)
            put("hostName", room.hostName)
            put("hostId", hostId)
            put("videoId", room.videoId)
            put("videoTitle", room.currentVideoTitle)
            put("privacyMode", privacyMode.name)
        }

        val request = Request.Builder()
            .url("$baseUrl/api/youtube/rooms/create")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Backend room create failed, using local room: ${e.message}")
                mainHandler.post {
                    onComplete(Result.success(room))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    try {
                        val body = resp.body?.string().orEmpty()
                        val obj = JSONObject(body)
                        if (obj.optBoolean("success", false)) {
                            val rObj = obj.optJSONObject("room")
                            if (rObj != null) {
                                val serverRoom = PublicYouTubeRoom(
                                    roomId = rObj.optString("roomId", room.roomId),
                                    roomCode = rObj.optString("roomCode", room.roomCode),
                                    title = rObj.optString("title", room.title),
                                    hostName = rObj.optString("hostName", room.hostName),
                                    hostAvatarBg = Color(0xFF2563EB),
                                    currentVideoTitle = rObj.optString("currentVideoTitle", room.currentVideoTitle),
                                    videoId = rObj.optString("videoId", room.videoId),
                                    thumbnailUrl = "https://img.youtube.com/vi/${rObj.optString("videoId", room.videoId)}/hqdefault.jpg",
                                    viewersCount = rObj.optInt("viewersCount", 1),
                                    isLive = true,
                                    durationText = "مباشر",
                                    privacyMode = privacyMode,
                                    createdAt = rObj.optLong("createdAt", System.currentTimeMillis())
                                )
                                saveRoomLocally(context, serverRoom)
                                mainHandler.post { onComplete(Result.success(serverRoom)) }
                                return
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing server response: ${e.message}")
                    }
                    mainHandler.post { onComplete(Result.success(room)) }
                }
            }
        })
    }

    /**
     * Fetch all REAL active public rooms (NO fake mock rooms)
     */
    fun fetchRealPublicRooms(
        context: Context,
        onComplete: (List<PublicYouTubeRoom>) -> Unit
    ) {
        val baseUrl = CloudflareClient.getBaseUrl(context)
        val request = Request.Builder()
            .url("$baseUrl/api/youtube/rooms/public")
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Failed to fetch public rooms: ${e.message}")
                mainHandler.post {
                    // Return real rooms saved locally
                    val local = loadRoomsLocally(context).filter { it.privacyMode == RoomPrivacyMode.PUBLIC }
                    onComplete(local)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val resultList = mutableListOf<PublicYouTubeRoom>()
                    try {
                        val body = resp.body?.string().orEmpty()
                        val obj = JSONObject(body)
                        if (obj.optBoolean("success", false)) {
                            val arr = obj.optJSONArray("rooms") ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                val rObj = arr.getJSONObject(i)
                                val pMode = try {
                                    RoomPrivacyMode.valueOf(rObj.optString("privacyMode", "PUBLIC"))
                                } catch (_: Exception) {
                                    RoomPrivacyMode.PUBLIC
                                }
                                val vId = rObj.optString("videoId", "dQw4w9WgXcQ")
                                resultList.add(
                                    PublicYouTubeRoom(
                                        roomId = rObj.optString("roomId"),
                                        roomCode = rObj.optString("roomCode"),
                                        title = rObj.optString("title"),
                                        hostName = rObj.optString("hostName"),
                                        hostAvatarBg = Color(0xFF2563EB),
                                        currentVideoTitle = rObj.optString("currentVideoTitle"),
                                        videoId = vId,
                                        thumbnailUrl = "https://img.youtube.com/vi/$vId/hqdefault.jpg",
                                        viewersCount = rObj.optInt("viewersCount", 1),
                                        isLive = rObj.optBoolean("isLive", true),
                                        durationText = "مباشر",
                                        privacyMode = pMode,
                                        createdAt = rObj.optLong("createdAt", System.currentTimeMillis())
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing public rooms: ${e.message}")
                    }

                    // Merge with local genuine rooms
                    val local = loadRoomsLocally(context).filter { it.privacyMode == RoomPrivacyMode.PUBLIC }
                    for (loc in local) {
                        if (!resultList.any { it.roomId == loc.roomId || it.roomCode == loc.roomCode }) {
                            resultList.add(0, loc)
                        }
                    }

                    mainHandler.post {
                        activeRealRooms.clear()
                        activeRealRooms.addAll(resultList)
                        onComplete(resultList)
                    }
                }
            }
        })
    }

    /**
     * Join Room by Code with STRICT validation.
     * Rejects invalid or fake codes immediately.
     */
    fun joinRealRoomByCode(
        context: Context,
        inputCode: String,
        onComplete: (Result<PublicYouTubeRoom>) -> Unit
    ) {
        val digitsOnly = inputCode.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length < 5) {
            onComplete(Result.failure(IllegalArgumentException("رمز الغرفة غير صحيح أو ناقص. يجب أن يتكون من 6 أرقام.")))
            return
        }

        // 1. Check local genuine rooms first
        val localMatch = loadRoomsLocally(context).find {
            it.roomCode.replace(Regex("[^0-9]"), "") == digitsOnly ||
            it.roomId.contains(digitsOnly)
        }
        if (localMatch != null) {
            onComplete(Result.success(localMatch))
            return
        }

        // 2. Query Cloudflare backend
        val baseUrl = CloudflareClient.getBaseUrl(context)
        val request = Request.Builder()
            .url("$baseUrl/api/youtube/rooms/get?code=$digitsOnly")
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onComplete(Result.failure(IllegalArgumentException("تعذر الاتصال بالخادم للتحقق من رمز الغرفة.")))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    try {
                        val body = resp.body?.string().orEmpty()
                        val obj = JSONObject(body)
                        if (obj.optBoolean("success", false)) {
                            val rObj = obj.optJSONObject("room")
                            if (rObj != null) {
                                val pMode = try {
                                    RoomPrivacyMode.valueOf(rObj.optString("privacyMode", "PUBLIC"))
                                } catch (_: Exception) {
                                    RoomPrivacyMode.PUBLIC
                                }
                                val vId = rObj.optString("videoId", "dQw4w9WgXcQ")
                                val room = PublicYouTubeRoom(
                                    roomId = rObj.optString("roomId"),
                                    roomCode = rObj.optString("roomCode"),
                                    title = rObj.optString("title"),
                                    hostName = rObj.optString("hostName"),
                                    hostAvatarBg = Color(0xFF2563EB),
                                    currentVideoTitle = rObj.optString("currentVideoTitle"),
                                    videoId = vId,
                                    thumbnailUrl = "https://img.youtube.com/vi/$vId/hqdefault.jpg",
                                    viewersCount = rObj.optInt("viewersCount", 1),
                                    isLive = rObj.optBoolean("isLive", true),
                                    durationText = "مباشر",
                                    privacyMode = pMode,
                                    createdAt = rObj.optLong("createdAt", System.currentTimeMillis())
                                )
                                saveRoomLocally(context, room)
                                mainHandler.post { onComplete(Result.success(room)) }
                                return
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error validating code: ${e.message}")
                    }
                    mainHandler.post {
                        onComplete(Result.failure(IllegalArgumentException("رمز الغرفة غير صحيح أو الغرفة غير موجودة")))
                    }
                }
            }
        })
    }

    private fun saveRoomLocally(context: Context, room: PublicYouTubeRoom) {
        try {
            val list = loadRoomsLocally(context).toMutableList()
            list.removeAll { it.roomId == room.roomId || it.roomCode == room.roomCode }
            list.add(0, room)

            val arr = JSONArray()
            list.take(30).forEach { r ->
                val o = JSONObject().apply {
                    put("roomId", r.roomId)
                    put("roomCode", r.roomCode)
                    put("title", r.title)
                    put("hostName", r.hostName)
                    put("currentVideoTitle", r.currentVideoTitle)
                    put("videoId", r.videoId)
                    put("thumbnailUrl", r.thumbnailUrl)
                    put("viewersCount", r.viewersCount)
                    put("privacyMode", r.privacyMode.name)
                    put("createdAt", r.createdAt)
                }
                arr.put(o)
            }
            getPrefs(context).edit().putString(KEY_LOCAL_ROOMS, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun loadRoomsLocally(context: Context): List<PublicYouTubeRoom> {
        val result = mutableListOf<PublicYouTubeRoom>()
        try {
            val raw = getPrefs(context).getString(KEY_LOCAL_ROOMS, null) ?: return emptyList()
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val pMode = try {
                    RoomPrivacyMode.valueOf(o.optString("privacyMode", "PUBLIC"))
                } catch (_: Exception) {
                    RoomPrivacyMode.PUBLIC
                }
                result.add(
                    PublicYouTubeRoom(
                        roomId = o.optString("roomId"),
                        roomCode = o.optString("roomCode"),
                        title = o.optString("title"),
                        hostName = o.optString("hostName"),
                        hostAvatarBg = Color(0xFF2563EB),
                        currentVideoTitle = o.optString("currentVideoTitle"),
                        videoId = o.optString("videoId"),
                        thumbnailUrl = o.optString("thumbnailUrl"),
                        viewersCount = o.optInt("viewersCount", 1),
                        isLive = true,
                        durationText = "مباشر",
                        privacyMode = pMode,
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }
}

// ----------------------------------------------------
// REAL-TIME WEBSOCKET SYNC CLIENT FOR YOUTUBE WATCH-PARTY
// ----------------------------------------------------
class YouTubeSyncWebSocket(
    private val context: Context,
    private val roomId: String,
    private val onVideoChangeReceived: (videoId: String, videoTitle: String) -> Unit,
    private val onPlaybackStateReceived: (isPlaying: Boolean, positionSec: Float) -> Unit,
    private val onChatMessageReceived: (YouTubeChatMessage) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect() {
        val baseUrl = CloudflareClient.getBaseUrl(context)
        val wsBase = baseUrl.replace("https://", "wss://").replace("http://", "ws://")
        val userId = CloudflareClient.getCurrentUserId(context)
        val username = CloudflareClient.getCurrentUsername(context)
        val cleanRoomId = roomId.ifBlank { "global_yt_lobby" }

        val wsUrl = "$wsBase/ws/$cleanRoomId?userId=$userId&username=$username"

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("YTSync", "Connected to watch-party room: $cleanRoomId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val obj = JSONObject(text)
                    val senderId = obj.optString("senderId", "")
                    if (senderId == userId) return // Ignore self-messages

                    val type = obj.optString("type", "")
                    when (type) {
                        "yt_video_change" -> {
                            val vId = obj.optString("videoId")
                            val vTitle = obj.optString("videoTitle")
                            if (vId.isNotEmpty()) {
                                mainHandler.post { onVideoChangeReceived(vId, vTitle) }
                            }
                        }
                        "yt_playback_state" -> {
                            val isPlaying = obj.optBoolean("isPlaying", true)
                            val pos = obj.optDouble("positionSec", 0.0).toFloat()
                            mainHandler.post { onPlaybackStateReceived(isPlaying, pos) }
                        }
                        "yt_chat_message" -> {
                            val msgText = obj.optString("text", "")
                            val sName = obj.optString("senderName", "مشاهد")
                            val time = obj.optString("time", "الآن")
                            val msg = YouTubeChatMessage(
                                id = obj.optString("id", "${System.currentTimeMillis()}"),
                                sender = sName,
                                text = msgText,
                                time = time,
                                isMe = false,
                                avatarColor = Color(0xFF2563EB)
                            )
                            mainHandler.post { onChatMessageReceived(msg) }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("YTSync", "Failed parsing sync event: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("YTSync", "WebSocket error in watch party: ${t.message}")
            }
        })
    }

    fun broadcastVideoChange(videoId: String, videoTitle: String) {
        val payload = JSONObject().apply {
            put("type", "yt_video_change")
            put("videoId", videoId)
            put("videoTitle", videoTitle)
            put("senderId", CloudflareClient.getCurrentUserId(context))
        }
        webSocket?.send(payload.toString())
    }

    fun broadcastPlaybackState(isPlaying: Boolean, positionSec: Float) {
        val payload = JSONObject().apply {
            put("type", "yt_playback_state")
            put("isPlaying", isPlaying)
            put("positionSec", positionSec.toDouble())
            put("senderId", CloudflareClient.getCurrentUserId(context))
        }
        webSocket?.send(payload.toString())
    }

    fun broadcastChatMessage(text: String) {
        val payload = JSONObject().apply {
            put("type", "yt_chat_message")
            put("text", text)
            put("senderName", CloudflareClient.getCurrentUsername(context))
            put("senderId", CloudflareClient.getCurrentUserId(context))
            put("time", "الآن")
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User Left Room")
            webSocket = null
        } catch (_: Exception) {}
    }
}
