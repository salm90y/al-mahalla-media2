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

data class PublicMoviesRoom(
    val roomId: String,
    val roomCode: String,
    val title: String,
    val hostName: String,
    val hostId: String = "",
    val hostAvatarBg: Color = Color(0xFF2563EB),
    val currentMovieTitle: String,
    val streamUrl: String,
    val posterUrl: String = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
    val viewersCount: Int = 1,
    val isLive: Boolean = true,
    val durationText: String = "مباشر",
    val privacyMode: RoomPrivacyMode = RoomPrivacyMode.PUBLIC,
    val createdAt: Long = System.currentTimeMillis()
)

object MoviesRoomManager {
    private const val TAG = "MoviesRoomManager"
    private const val PREFS_NAME = "ps1_movies_real_rooms"
    private const val KEY_LOCAL_ROOMS = "active_movies_real_rooms_json"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // In-memory list of genuine real active rooms
    val activeRealRooms = mutableListOf<PublicMoviesRoom>()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAppOwner(context: Context): Boolean {
        val u = UserManager.getCurrentUser(context)
        if (u?.isAdmin == true) return true
        val name = CloudflareClient.getCurrentUsername(context)
        val email = u?.email ?: ""
        return name.equals("ahmed", ignoreCase = true) ||
               name.contains("1986") ||
               email.equals("ahmed1986y5@gmail.com", ignoreCase = true)
    }

    fun updateRoomMovie(context: Context, roomId: String, streamUrl: String, movieTitle: String, posterUrl: String) {
        val inMemIndex = activeRealRooms.indexOfFirst { it.roomId == roomId }
        if (inMemIndex >= 0) {
            val r = activeRealRooms[inMemIndex]
            val updated = r.copy(streamUrl = streamUrl, currentMovieTitle = movieTitle, posterUrl = posterUrl)
            activeRealRooms[inMemIndex] = updated
            saveRoomLocally(context, updated)
        } else {
            val localList = loadRoomsLocally(context).toMutableList()
            val loc = localList.find { it.roomId == roomId }
            if (loc != null) {
                val updated = loc.copy(streamUrl = streamUrl, currentMovieTitle = movieTitle, posterUrl = posterUrl)
                saveRoomLocally(context, updated)
            }
        }

        val baseUrl = CloudflareClient.getBaseUrl(context)
        val jsonBody = JSONObject().apply {
            put("roomId", roomId)
            put("streamUrl", streamUrl)
            put("movieTitle", movieTitle)
            put("posterUrl", posterUrl)
        }
        val request = Request.Builder()
            .url("$baseUrl/api/movies/rooms/update")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    fun createRealRoom(
        context: Context,
        title: String,
        privacyMode: RoomPrivacyMode,
        initialStreamUrl: String = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/101.mp4",
        initialMovieTitle: String = "ولاد رزق 3: القاضية",
        initialPosterUrl: String = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
        onComplete: (Result<PublicMoviesRoom>) -> Unit
    ) {
        val hostName = CloudflareClient.getCurrentUsername(context).ifBlank { "أحمد" }
        val hostId = CloudflareClient.getCurrentUserId(context)
        val cleanTitle = if (title.isBlank()) "سينما $hostName" else title.trim()

        val codeNum = Random.nextInt(100000, 999999)
        val roomCode = "#MOV-$codeNum"
        val roomId = "mov_room_$codeNum"

        val room = PublicMoviesRoom(
            roomId = roomId,
            roomCode = roomCode,
            title = cleanTitle,
            hostName = hostName,
            hostId = hostId,
            hostAvatarBg = Color(0xFF2563EB),
            currentMovieTitle = initialMovieTitle,
            streamUrl = initialStreamUrl,
            posterUrl = initialPosterUrl,
            viewersCount = 1,
            isLive = true,
            privacyMode = privacyMode,
            createdAt = System.currentTimeMillis()
        )

        activeRealRooms.add(0, room)
        saveRoomLocally(context, room)

        val baseUrl = CloudflareClient.getBaseUrl(context)
        val jsonBody = JSONObject().apply {
            put("title", cleanTitle)
            put("hostName", hostName)
            put("hostId", hostId)
            put("streamUrl", initialStreamUrl)
            put("movieTitle", initialMovieTitle)
            put("posterUrl", initialPosterUrl)
            put("privacy", privacyMode.name)
        }

        val request = Request.Builder()
            .url("$baseUrl/api/movies/rooms/create")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onComplete(Result.success(room)) }
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string()
                    if (response.isSuccessful && !body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val rObj = json.optJSONObject("room")
                        if (rObj != null) {
                            val serverRoom = parseRoomJson(rObj)
                            activeRealRooms.removeAll { it.roomId == room.roomId }
                            activeRealRooms.add(0, serverRoom)
                            saveRoomLocally(context, serverRoom)
                            mainHandler.post { onComplete(Result.success(serverRoom)) }
                            return
                        }
                    }
                } catch (_: Exception) {}
                mainHandler.post { onComplete(Result.success(room)) }
            }
        })
    }

    fun fetchRealPublicRooms(context: Context, onResult: (List<PublicMoviesRoom>) -> Unit) {
        val baseUrl = CloudflareClient.getBaseUrl(context)
        val request = Request.Builder()
            .url("$baseUrl/api/movies/rooms/public")
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    val local = loadRoomsLocally(context)
                    onResult(local)
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val list = mutableListOf<PublicMoviesRoom>()
                try {
                    val body = response.body?.string()
                    if (response.isSuccessful && !body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val arr = json.optJSONArray("rooms") ?: JSONArray()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(parseRoomJson(obj))
                        }
                    }
                } catch (_: Exception) {}

                mainHandler.post {
                    val local = loadRoomsLocally(context)
                    val combined = (list + local).distinctBy { it.roomId }
                    activeRealRooms.clear()
                    activeRealRooms.addAll(combined)
                    onResult(combined)
                }
            }
        })
    }

    fun getRoomByCodeOrId(context: Context, query: String, onResult: (PublicMoviesRoom?) -> Unit) {
        val clean = query.trim().replace("#", "").replace("MOV-", "").replace("mov-", "")
        
        val localMatch = activeRealRooms.find {
            it.roomCode.replace("#", "").replace("MOV-", "").equals(clean, ignoreCase = true) ||
            it.roomId.equals(query.trim(), ignoreCase = true)
        } ?: loadRoomsLocally(context).find {
            it.roomCode.replace("#", "").replace("MOV-", "").equals(clean, ignoreCase = true) ||
            it.roomId.equals(query.trim(), ignoreCase = true)
        }

        val baseUrl = CloudflareClient.getBaseUrl(context)
        val url = if (clean.all { it.isDigit() }) {
            "$baseUrl/api/movies/rooms/get?code=$clean"
        } else {
            "$baseUrl/api/movies/rooms/get?id=${query.trim()}"
        }

        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onResult(localMatch) }
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string()
                    if (response.isSuccessful && !body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        if (json.optBoolean("success", false)) {
                            val rObj = json.optJSONObject("room")
                            if (rObj != null) {
                                val r = parseRoomJson(rObj)
                                saveRoomLocally(context, r)
                                mainHandler.post { onResult(r) }
                                return
                            }
                        }
                    }
                } catch (_: Exception) {}
                mainHandler.post { onResult(localMatch) }
            }
        })
    }

    fun deleteRoom(context: Context, roomId: String, onComplete: () -> Unit = {}) {
        activeRealRooms.removeAll { it.roomId == roomId }
        val currentLocal = loadRoomsLocally(context).filter { it.roomId != roomId }
        saveAllRoomsLocally(context, currentLocal)

        val baseUrl = CloudflareClient.getBaseUrl(context)
        val jsonBody = JSONObject().apply { put("roomId", roomId) }
        val request = Request.Builder()
            .url("$baseUrl/api/movies/rooms/delete")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { onComplete() } }
            override fun onResponse(call: Call, response: Response) {
                response.close()
                mainHandler.post { onComplete() }
            }
        })
    }

    fun saveRoomLocally(context: Context, room: PublicMoviesRoom) {
        val current = loadRoomsLocally(context).toMutableList()
        val idx = current.indexOfFirst { it.roomId == room.roomId }
        if (idx >= 0) current[idx] = room else current.add(0, room)
        saveAllRoomsLocally(context, current)
    }

    fun loadRoomsLocally(context: Context): List<PublicMoviesRoom> {
        val raw = getPrefs(context).getString(KEY_LOCAL_ROOMS, null) ?: return emptyList()
        val list = mutableListOf<PublicMoviesRoom>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                list.add(parseRoomJson(arr.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveAllRoomsLocally(context: Context, rooms: List<PublicMoviesRoom>) {
        try {
            val arr = JSONArray()
            rooms.take(30).forEach { r ->
                val obj = JSONObject().apply {
                    put("roomId", r.roomId)
                    put("roomCode", r.roomCode)
                    put("title", r.title)
                    put("hostName", r.hostName)
                    put("hostId", r.hostId)
                    put("currentMovieTitle", r.currentMovieTitle)
                    put("streamUrl", r.streamUrl)
                    put("posterUrl", r.posterUrl)
                    put("viewersCount", r.viewersCount)
                    put("isLive", r.isLive)
                    put("durationText", r.durationText)
                    put("privacyMode", r.privacyMode.name)
                    put("createdAt", r.createdAt)
                }
                arr.put(obj)
            }
            getPrefs(context).edit().putString(KEY_LOCAL_ROOMS, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun parseRoomJson(obj: JSONObject): PublicMoviesRoom {
        val privStr = obj.optString("privacyMode", obj.optString("privacy", "PUBLIC"))
        val priv = try { RoomPrivacyMode.valueOf(privStr) } catch (_: Exception) { RoomPrivacyMode.PUBLIC }
        return PublicMoviesRoom(
            roomId = obj.optString("roomId", "mov_room_${Random.nextInt(1000, 9999)}"),
            roomCode = obj.optString("roomCode", "#MOV-${Random.nextInt(100000, 999999)}"),
            title = obj.optString("title", "سينما الأفلام والمسلسلات"),
            hostName = obj.optString("hostName", "المضيف"),
            hostId = obj.optString("hostId", ""),
            hostAvatarBg = Color(0xFF2563EB),
            currentMovieTitle = obj.optString("currentMovieTitle", obj.optString("movieTitle", "ولاد رزق 3")),
            streamUrl = obj.optString("streamUrl", "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/101.mp4"),
            posterUrl = obj.optString("posterUrl", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80"),
            viewersCount = obj.optInt("viewersCount", 1).coerceAtLeast(1),
            isLive = obj.optBoolean("isLive", true),
            durationText = obj.optString("durationText", "مباشر"),
            privacyMode = priv,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
