package com.ps1.netplay.ui.compose

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.ps1.netplay.UserManager
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
    val hostId: String = "",
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

enum class RoomMemberRole {
    HOST,
    MODERATOR,
    MEMBER
}

data class RoomMemberPermissions(
    val userId: String,
    val role: RoomMemberRole = RoomMemberRole.MEMBER,
    val canChangeVideo: Boolean = false,
    val isMutedVoice: Boolean = false,
    val isMutedChat: Boolean = false,
    val isKicked: Boolean = false
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
     * Check if current user is the application owner
     */
    fun isAppOwner(context: Context): Boolean {
        val u = UserManager.getCurrentUser(context)
        if (u?.isAdmin == true) return true
        val name = CloudflareClient.getCurrentUsername(context)
        val email = u?.email ?: ""
        return name.equals("ahmed", ignoreCase = true) ||
               name.contains("1986") ||
               email.equals("ahmed1986y5@gmail.com", ignoreCase = true)
    }

    /**
     * Update room's currently playing video in memory, local storage and backend
     */
    fun updateRoomVideo(context: Context, roomId: String, videoId: String, videoTitle: String) {
        val thumb = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

        // 1. In-memory update
        val inMemIndex = activeRealRooms.indexOfFirst { it.roomId == roomId }
        if (inMemIndex >= 0) {
            val r = activeRealRooms[inMemIndex]
            val updated = r.copy(videoId = videoId, currentVideoTitle = videoTitle, thumbnailUrl = thumb)
            activeRealRooms[inMemIndex] = updated
            saveRoomLocally(context, updated)
        } else {
            // Check local storage
            val localList = loadRoomsLocally(context).toMutableList()
            val loc = localList.find { it.roomId == roomId }
            if (loc != null) {
                val updated = loc.copy(videoId = videoId, currentVideoTitle = videoTitle, thumbnailUrl = thumb)
                saveRoomLocally(context, updated)
            }
        }

        // 2. Remote update on Cloudflare Worker
        val baseUrl = CloudflareClient.getBaseUrl(context)
        val jsonBody = JSONObject().apply {
            put("roomId", roomId)
            put("videoId", videoId)
            put("videoTitle", videoTitle)
        }
        val request = Request.Builder()
            .url("$baseUrl/api/youtube/rooms/update")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
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

        val codeNum = Random.nextInt(100000, 999999)
        val roomCode = "#YT-$codeNum"
        val roomId = "yt_room_$codeNum"

        val room = PublicYouTubeRoom(
            roomId = roomId,
            roomCode = roomCode,
            title = cleanTitle,
            hostName = hostName,
            hostId = hostId,
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

        saveRoomLocally(context, room)

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
                mainHandler.post { onComplete(Result.success(room)) }
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
                                    hostId = rObj.optString("hostId", room.hostId),
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
                    } catch (_: Exception) {}
                    mainHandler.post { onComplete(Result.success(room)) }
                }
            }
        })
    }

    /**
     * Fetch all REAL active rooms (App owner sees all rooms including private)
     */
    fun fetchRealPublicRooms(
        context: Context,
        onComplete: (List<PublicYouTubeRoom>) -> Unit
    ) {
        val baseUrl = CloudflareClient.getBaseUrl(context)
        val isOwner = isAppOwner(context)
        val endpoint = if (isOwner) "$baseUrl/api/youtube/rooms/all" else "$baseUrl/api/youtube/rooms/public"

        val request = Request.Builder().url(endpoint).get().build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    val local = loadRoomsLocally(context).filter { isOwner || it.privacyMode == RoomPrivacyMode.PUBLIC }
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
                                        hostId = rObj.optString("hostId"),
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
                    } catch (_: Exception) {}

                    val local = loadRoomsLocally(context).filter { isOwner || it.privacyMode == RoomPrivacyMode.PUBLIC }
                    for (loc in local) {
                        val existingIdx = resultList.indexOfFirst { it.roomId == loc.roomId || it.roomCode == loc.roomCode }
                        if (existingIdx < 0) {
                            resultList.add(loc)
                        }
                    }

                    // Save verified authoritative rooms from server locally
                    resultList.forEach { saveRoomLocally(context, it) }

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
     * Fetch latest authoritative room details by roomId
     */
    fun getRoomLatest(
        context: Context,
        roomId: String,
        onComplete: (PublicYouTubeRoom?) -> Unit
    ) {
        val baseUrl = CloudflareClient.getBaseUrl(context)
        val request = Request.Builder()
            .url("$baseUrl/api/youtube/rooms/get?id=$roomId")
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    val loc = loadRoomsLocally(context).find { it.roomId == roomId }
                    onComplete(loc)
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
                                val vId = rObj.optString("videoId", "")
                                val room = PublicYouTubeRoom(
                                    roomId = rObj.optString("roomId"),
                                    roomCode = rObj.optString("roomCode"),
                                    title = rObj.optString("title"),
                                    hostName = rObj.optString("hostName"),
                                    hostId = rObj.optString("hostId"),
                                    hostAvatarBg = Color(0xFF2563EB),
                                    currentVideoTitle = rObj.optString("currentVideoTitle"),
                                    videoId = vId,
                                    thumbnailUrl = if (vId.isNotEmpty()) "https://img.youtube.com/vi/$vId/hqdefault.jpg" else rObj.optString("thumbnailUrl"),
                                    viewersCount = rObj.optInt("viewersCount", 1),
                                    isLive = rObj.optBoolean("isLive", true),
                                    durationText = "مباشر",
                                    privacyMode = pMode,
                                    createdAt = rObj.optLong("createdAt", System.currentTimeMillis())
                                )
                                saveRoomLocally(context, room)
                                mainHandler.post { onComplete(room) }
                                return
                            }
                        }
                    } catch (_: Exception) {}
                    mainHandler.post {
                        val loc = loadRoomsLocally(context).find { it.roomId == roomId }
                        onComplete(loc)
                    }
                }
            }
        })
    }

    /**
     * Join Room by Code with STRICT validation (App Owner can force enter).
     */
    fun joinRealRoomByCode(
        context: Context,
        inputCode: String,
        forceOwnerBypass: Boolean = false,
        onComplete: (Result<PublicYouTubeRoom>) -> Unit
    ) {
        val digitsOnly = inputCode.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length < 5 && !forceOwnerBypass) {
            onComplete(Result.failure(IllegalArgumentException("رمز الغرفة غير صحيح أو ناقص. يجب أن يتكون من 6 أرقام.")))
            return
        }

        // Check local genuine rooms first
        val localMatch = loadRoomsLocally(context).find {
            it.roomCode.replace(Regex("[^0-9]"), "") == digitsOnly ||
            it.roomId.contains(digitsOnly)
        }
        if (localMatch != null) {
            onComplete(Result.success(localMatch))
            return
        }

        // Query Cloudflare backend
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
                                    hostId = rObj.optString("hostId"),
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
                    } catch (_: Exception) {}
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
                    put("hostId", r.hostId)
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
                        hostId = o.optString("hostId"),
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
// ADVANCED REAL-TIME WEBSOCKET SYNC CLIENT
// ----------------------------------------------------
class YouTubeSyncWebSocket(
    private val context: Context,
    private val roomId: String,
    private val isStealthMode: Boolean = false,
    private val onVideoChangeReceived: (videoId: String, videoTitle: String) -> Unit,
    private val onPlaybackStateReceived: (isPlaying: Boolean, positionSec: Float) -> Unit,
    private val onChatMessageReceived: (YouTubeChatMessage) -> Unit,
    private val onStateRequested: (() -> Unit)? = null,
    private val onVideoChangeRequested: ((requesterId: String, requesterName: String, videoId: String, videoTitle: String) -> Unit)? = null,
    private val onVideoChangeRequestRejected: (() -> Unit)? = null,
    private val onMemberActionReceived: ((targetUserId: String, actionType: String) -> Unit)? = null,
    private val onVoiceStateReceived: ((userId: String, username: String, isTalking: Boolean) -> Unit)? = null
) {
    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect() {
        val baseUrl = CloudflareClient.getBaseUrl(context)
        val wsBase = baseUrl.replace("https://", "wss://").replace("http://", "ws://")
        val userId = if (isStealthMode) "stealth_${System.currentTimeMillis()}" else CloudflareClient.getCurrentUserId(context)
        val username = if (isStealthMode) "مجهول" else CloudflareClient.getCurrentUsername(context)
        val cleanRoomId = roomId.ifBlank { "global_yt_lobby" }

        val wsUrl = "$wsBase/ws/$cleanRoomId?userId=$userId&username=$username&stealth=$isStealthMode"

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("YTSync", "Connected to watch-party room: $cleanRoomId (Stealth: $isStealthMode)")
                if (!isStealthMode) {
                    // Request current playing state from host immediately upon joining
                    requestCurrentState()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val obj = JSONObject(text)
                    val senderId = obj.optString("senderId", "")
                    val myId = CloudflareClient.getCurrentUserId(context)
                    if (senderId == myId) return // Ignore self-echo

                    when (obj.optString("type")) {
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
                        "yt_request_state" -> {
                            // A newcomer asks for the currently playing video
                            mainHandler.post { onStateRequested?.invoke() }
                        }
                        "yt_video_request" -> {
                            // Member asks host permission to change video
                            val reqId = obj.optString("requesterId")
                            val reqName = obj.optString("requesterName")
                            val reqVideoId = obj.optString("videoId")
                            val reqVideoTitle = obj.optString("videoTitle")
                            mainHandler.post {
                                onVideoChangeRequested?.invoke(reqId, reqName, reqVideoId, reqVideoTitle)
                            }
                        }
                        "yt_video_reject" -> {
                            val targetId = obj.optString("targetUserId")
                            if (targetId == myId) {
                                mainHandler.post { onVideoChangeRequestRejected?.invoke() }
                            }
                        }
                        "yt_member_action" -> {
                            val targetId = obj.optString("targetUserId")
                            val actionType = obj.optString("actionType")
                            mainHandler.post { onMemberActionReceived?.invoke(targetId, actionType) }
                        }
                        "yt_voice_state" -> {
                            val uId = obj.optString("userId")
                            val uName = obj.optString("username")
                            val isTalking = obj.optBoolean("isTalking", false)
                            mainHandler.post { onVoiceStateReceived?.invoke(uId, uName, isTalking) }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("YTSync", "Failed parsing sync event: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("YTSync", "WebSocket error: ${t.message}")
            }
        })
    }

    fun requestCurrentState() {
        val payload = JSONObject().apply {
            put("type", "yt_request_state")
            put("requesterId", CloudflareClient.getCurrentUserId(context))
        }
        webSocket?.send(payload.toString())
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

    fun requestVideoChange(videoId: String, videoTitle: String) {
        val payload = JSONObject().apply {
            put("type", "yt_video_request")
            put("videoId", videoId)
            put("videoTitle", videoTitle)
            put("requesterId", CloudflareClient.getCurrentUserId(context))
            put("requesterName", CloudflareClient.getCurrentUsername(context))
        }
        webSocket?.send(payload.toString())
    }

    fun rejectVideoChange(requesterId: String) {
        val payload = JSONObject().apply {
            put("type", "yt_video_reject")
            put("targetUserId", requesterId)
        }
        webSocket?.send(payload.toString())
    }

    fun broadcastMemberAction(targetUserId: String, actionType: String) {
        val payload = JSONObject().apply {
            put("type", "yt_member_action")
            put("targetUserId", targetUserId)
            put("actionType", actionType)
        }
        webSocket?.send(payload.toString())
    }

    fun broadcastVoiceState(isTalking: Boolean) {
        val payload = JSONObject().apply {
            put("type", "yt_voice_state")
            put("isTalking", isTalking)
            put("userId", CloudflareClient.getCurrentUserId(context))
            put("username", CloudflareClient.getCurrentUsername(context))
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
