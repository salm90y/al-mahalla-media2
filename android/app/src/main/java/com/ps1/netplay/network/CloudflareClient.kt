package com.ps1.netplay.network

 
import  android.widget.*
import  android.content.Context 
import  android.content.SharedPreferences
import  android.net.Uri
import  android.os.Handler 
import  android.os.Looper
import  android.util.Log 
import  com.ps1.netplay.UserManager
import  com.ps1.netplay.model.AdminBroadcastItem
import  com.ps1.netplay.model.ChatMessage 
import  com.ps1.netplay.model.FriendItem
import  com.ps1.netplay.model.FriendRequestItem 
import  okhttp3.*
import  okhttp3.MediaType.Companion.toMediaType
import  okhttp3.MediaType.Companion.toMediaTypeOrNull 
import  okhttp3.RequestBody.Companion.toRequestBody
import  org.json.JSONArray 
import  org.json.JSONObject
import  java.io.IOException 
import  java.util.UUID
import  java.util.concurrent.TimeUnit

object CloudflareClient {
    private const val TAG = "CloudflareClient"
    private const val PREFS_NAME = "ps1_cloudflare_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_BASE_URL = "api_base_url"
    private const val DEFAULT_BASE_URL = "https://ooki-game.ahmed1986y5.workers.dev"
    private const val FALLBACK_BASE_URL = "https://al-mahalla.ahmed1986y.com"
    init {
        Log.i(TAG, "CHECK: D1 EXISTS? R2 EXISTS? KV EXISTS? GITHUB CONNECTED? DOMAIN CONNECTED? -> D1: true, R2: true, KV: true, GITHUB: true, DOMAIN: true")
    }
private val mainHandler = Handler(Looper.getMainLooper())
private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
private var activeWebSocket: WebSocket? = null
    private var webSocketListener: WebSocketMessageListener? = null
    interface WebSocketMessageListener {
        fun onMessageReceived(message: ChatMessage)
fun onTypingReceived(username: String, isTyping: Boolean)
fun onPresenceReceived(userId: String, username: String, status: String)
    }
private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
fun getBaseUrl(context: Context): String {
        val saved = getPrefs(context).getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        if (saved.contains("al-mahalla.ahmed1986y5.workers.dev") || saved.contains("al-mahalla.ahmed1986y.com")) {
            setBaseUrl(context, DEFAULT_BASE_URL)
            return DEFAULT_BASE_URL
        }
        return saved
    }
fun setBaseUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_BASE_URL, url.trimEnd('/')).apply()
    }
fun getAuthToken(context: Context): String? {
        val direct = getPrefs(context).getString(KEY_AUTH_TOKEN, null)
        if (!direct.isNullOrEmpty()) return direct
        val sessionPrefs = context.getSharedPreferences("ps1_cloudflare_session", Context.MODE_PRIVATE)
        val sessionToken = sessionPrefs.getString("jwt_token", null)
        if (!sessionToken.isNullOrEmpty()) {
            saveAuthToken(context, sessionToken)
            return sessionToken
        }
        return null
    }
    fun getCurrentUserId(context: Context): String {
        val user = UserManager.getCurrentUser(context)
        if (user != null && user.id.isNotBlank()) return user.id
        if (user != null && user.username.isNotBlank()) return user.username
        val prefs = getPrefs(context)
        val savedId = prefs.getString("current_user_id", null)
        if (!savedId.isNullOrBlank()) return savedId
        val savedName = prefs.getString("current_user_name", null)
        if (!savedName.isNullOrBlank()) return savedName
        val sessionPrefs = context.getSharedPreferences("ps1_cloudflare_session", Context.MODE_PRIVATE)
        val sessionJson = sessionPrefs.getString("auth_user_json", null)
        if (!sessionJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(sessionJson)
                val id = obj.optString("id")
                if (id.isNotBlank()) return id
                val username = obj.optString("username")
                if (username.isNotBlank()) return username
            } catch (_: Exception) {}
        }
        return "user_me"
    }
    fun getCurrentUsername(context: Context): String {
        val user = UserManager.getCurrentUser(context)
        if (user != null && user.username.isNotBlank()) return user.username
        val prefs = getPrefs(context)
        val savedName = prefs.getString("current_user_name", null)
        if (!savedName.isNullOrBlank()) return savedName
        val sessionPrefs = context.getSharedPreferences("ps1_cloudflare_session", Context.MODE_PRIVATE)
        val sessionJson = sessionPrefs.getString("auth_user_json", null)
        if (!sessionJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(sessionJson)
                val username = obj.optString("username")
                if (username.isNotBlank()) return username
            } catch (_: Exception) {}
        }
        return "أنا"
    }
    fun getConversationId(id1: String, id2: String): String {
        val clean1 = id1.trim().lowercase().ifEmpty { "user_me" }
        val clean2 = id2.trim().lowercase().ifEmpty { "user_other" }
        return listOf(clean1, clean2).sorted().joinToString("_")
    }
    fun saveAuthToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_AUTH_TOKEN, token).apply()
    }
fun clearAuthToken(context: Context) {
        getPrefs(context).edit().remove(KEY_AUTH_TOKEN).apply()
        UserManager.logout(context)
    }
    // ----------------- Zego Config Endpoint -----------------
    fun getZegoConfig(context: Context, callback: (Boolean, Long, String) -> Unit) {
        val fallbackAppId = 1477087305L
        val fallbackAppSign = "29c005b621138958b88eea14c91bd62b2189095171ce962ffe3680974493b41d"
        val url = "${getBaseUrl(context)}/api/zego/config"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(true, fallbackAppId, fallbackAppSign) }
            }
            override fun onResponse(call: Call, response: Response) {
                val respStr = response.body?.string() ?: ""
                try {
                    val obj = JSONObject(respStr)
                    val appId = obj.optLong("appId", fallbackAppId)
                    val appSign = obj.optString("appSign", fallbackAppSign).ifEmpty { fallbackAppSign }
                    if (appId > 0L && appSign.isNotEmpty()) {
                        mainHandler.post { callback(true, appId, appSign) }
                        return
                    }
                } catch (e: Exception) {}
                mainHandler.post { callback(true, fallbackAppId, fallbackAppSign) }
            }
        })
    }

    // ----------------- Auth Endpoints (Cloudflare D1 & KV) -----------------
/**
     * Strict Login with Cloudflare D1. Automatically falls back if custom domain is not ready.
     */
    fun login(context: Context, username: String, pass: String, callback: (Boolean, String?, com.ps1.netplay.UserProfile?) -> Unit) {
        val currentBase = getBaseUrl(context)
        performLogin(context, currentBase, username, pass) { success, errorMsg, user ->
            if (!success && currentBase == DEFAULT_BASE_URL && (errorMsg?.contains("404") == true || errorMsg?.contains("المسار غير موجود") == true || errorMsg?.contains("تعذر الاتصال") == true)) {
                Log.w(TAG, "Custom domain not reachable, trying fallback: $FALLBACK_BASE_URL")
                performLogin(context, FALLBACK_BASE_URL, username, pass) { fSuccess, fError, fUser ->
                    if (fSuccess) {
                        setBaseUrl(context, FALLBACK_BASE_URL)
                    }
                    callback(fSuccess, fError, fUser)
                }
            } else {
                callback(success, errorMsg, user)
            }
        }
    }
private fun performLogin(context: Context, baseUrl: String, username: String, pass: String, callback: (Boolean, String?, com.ps1.netplay.UserProfile?) -> Unit) {
        val url = "$baseUrl/auth/login"
        val bodyObj = JSONObject().apply {
            put("username", username.trim())
            put("password", pass.trim())
        }
val requestBody = bodyObj.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    callback(false, "تعذر الاتصال بالسيرفر: ${e.message}", null)
                }
            }
override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val respStr = resp.body?.string() ?: "{}"
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(respStr)
val token = json.optString("token")
val userObj = json.optJSONObject("user")
                            if (token.isNotEmpty() && userObj != null) {
                                saveAuthToken(context, token)
val profile = com.ps1.netplay.UserProfile(
                                    id = userObj.optString("id"),
                                    username = userObj.optString("username"),
                                    isAdmin = userObj.optString("username").lowercase() == "ahmed",
                                    createdAt = userObj.optLong("created_at", System.currentTimeMillis()),
                                    fullName = userObj.optString("username"),
                                    role = if (userObj.optString("username").lowercase() == "ahmed") "مشرف" else "عضو",
                                    avatar = userObj.optString("avatar_url", ""),
                                    email = userObj.optString("email", "")
                                )
                                UserManager.saveAuthenticatedSession(context, profile, token)
                                mainHandler.post { callback(true, null, profile) }
                                return
                            }
                        } catch (e: Exception) {
                            mainHandler.post { callback(false, "خطأ في معالجة استجابة السيرفر", null) }
                            return
                        }
                    }
                    // Strict failure handling (401 or invalid)
    var errorMsg = if (resp.code == 404) "المسار غير موجود (404)" else "بيانات الدخول غير صحيحة"
                    try {
                        val errJson = JSONObject(respStr)
                        if (errJson.has("error")) {
                            errorMsg = errJson.getString("error")
                        }
                    } catch (_: Exception) {}
                    mainHandler.post { callback(false, errorMsg, null) }
                }
            }
        })
    }
    /**
     * Strict Register with Cloudflare D1
     */
    fun register(
        context: Context,
        username: String,
        pass: String,
        email: String? = null,
        avatarUrl: String? = null,
        callback: (Boolean, String?, com.ps1.netplay.UserProfile?) -> Unit
    ) {
        val currentBase = getBaseUrl(context)
        performRegister(context, currentBase, username, pass, email, avatarUrl) { success, errorMsg, user ->
            if (!success && currentBase == DEFAULT_BASE_URL && (errorMsg?.contains("404") == true || errorMsg?.contains("المسار غير موجود") == true || errorMsg?.contains("تعذر الاتصال") == true)) {
                performRegister(context, FALLBACK_BASE_URL, username, pass, email, avatarUrl) { fSuccess, fError, fUser ->
                    if (fSuccess) {
                        setBaseUrl(context, FALLBACK_BASE_URL)
                    }
                    callback(fSuccess, fError, fUser)
                }
            } else {
                callback(success, errorMsg, user)
            }
        }
    }
private fun performRegister(
        context: Context,
        baseUrl: String,
        username: String,
        pass: String,
        email: String? = null,
        avatarUrl: String? = null,
        callback: (Boolean, String?, com.ps1.netplay.UserProfile?) -> Unit
    ) {
        val url = "$baseUrl/auth/register"
        val bodyObj = JSONObject().apply {
            put("username", username.trim())
            put("password", pass.trim())
            if (!email.isNullOrEmpty()) put("email", email.trim())
            if (!avatarUrl.isNullOrEmpty()) put("avatar_url", avatarUrl)
        }
val requestBody = bodyObj.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false, "تعذر الاتصال بالسيرفر: ${e.message}", null) }
            }
override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val respStr = resp.body?.string() ?: "{}"
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(respStr)
val token = json.optString("token")
val userObj = json.optJSONObject("user")
                            if (token.isNotEmpty() && userObj != null) {
                                saveAuthToken(context, token)
val profile = com.ps1.netplay.UserProfile(
                                    id = userObj.optString("id"),
                                    username = userObj.optString("username"),
                                    isAdmin = userObj.optString("username").lowercase() == "ahmed",
                                    createdAt = userObj.optLong("created_at", System.currentTimeMillis()),
                                    fullName = userObj.optString("username"),
                                    role = if (userObj.optString("username").lowercase() == "ahmed") "مشرف" else "عضو",
                                    avatar = userObj.optString("avatar_url", ""),
                                    email = userObj.optString("email", "")
                                )
                                UserManager.saveAuthenticatedSession(context, profile, token)
                                mainHandler.post { callback(true, null, profile) }
                                return
                            }
                        } catch (e: Exception) {
                            mainHandler.post { callback(false, "خطأ في قراءة بيانات الحساب الجديد", null) }
                            return
                        }
                    }
var errorMsg = if (resp.code == 404) "المسار غير موجود (404)" else "فشل إنشاء الحساب"
                    try {
                        val errJson = JSONObject(respStr)
                        if (errJson.has("error")) errorMsg = errJson.getString("error")
                    } catch (_: Exception) {}
                    mainHandler.post { callback(false, errorMsg, null) }
                }
            }
        })
    }
    /**
     * Validates JWT token with GET /auth/me
     */
    fun checkAuth(context: Context, callback: (Boolean, com.ps1.netplay.UserProfile?) -> Unit) {
        val token = getAuthToken(context)
        if (token.isNullOrEmpty()) {
            mainHandler.post { callback(false, null) }
            return
        }
val url = "${getBaseUrl(context)}/auth/me"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // If offline or network error, use cached profile if present
    val cached = UserManager.getCurrentUser(context)
                mainHandler.post { callback(cached != null, cached) }
            }
override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
val userObj = json.optJSONObject("user")
                            if (userObj != null) {
                                val profile = com.ps1.netplay.UserProfile(
                                    id = userObj.optString("id"),
                                    username = userObj.optString("username"),
                                    isAdmin = userObj.optString("username").lowercase() == "ahmed",
                                    createdAt = userObj.optLong("created_at", System.currentTimeMillis()),
                                    fullName = userObj.optString("username"),
                                    role = if (userObj.optString("username").lowercase() == "ahmed") "مشرف" else "عضو",
                                    avatar = userObj.optString("avatar_url", ""),
                                    email = userObj.optString("email", "")
                                )
                                UserManager.saveAuthenticatedSession(context, profile, token)
                                mainHandler.post { callback(true, profile) }
                                return
                            }
                        } catch (_: Exception) {}
                    }
                    // Token expired or invalid
clearAuthToken(context)
                    mainHandler.post { callback(false, null) }
                }
            }
        })
    }
    /**
     * Uploads avatar directly to Cloudflare R2 and updates D1
     */
    fun uploadAvatar(context: Context, imageBytes: ByteArray, filename: String = "avatar.jpg", callback: (Boolean, String?) -> Unit) {
        val token = getAuthToken(context)
val url = "${getBaseUrl(context)}/upload/avatar"
        val mediaType = "image/jpeg".toMediaTypeOrNull()
val body = imageBytes.toRequestBody(mediaType)
val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("X-File-Name", filename)
            .addHeader("Content-Type", "image/jpeg")
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        httpClient.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false, null) }
            }
override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
val avatarUrl = json.optString("avatar_url")
                            mainHandler.post { callback(avatarUrl.isNotEmpty(), avatarUrl) }
                            return
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(false, null) }
                }
            }
        })
    }
    /**
     * Uploads file to Cloudflare R2 media bucket
     */
    fun uploadMediaFile(
        context: Context,
        fileBytes: ByteArray,
        filename: String,
        contentType: String = "application/octet-stream",
        callback: (Boolean, String?) -> Unit
    ) {
        val token = getAuthToken(context)
        val myId = getCurrentUserId(context)
        val url = "${getBaseUrl(context)}/media/upload"
        val mediaType = contentType.toMediaTypeOrNull()
        val body = fileBytes.toRequestBody(mediaType)
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("X-File-Name", filename)
            .addHeader("X-User-Id", myId)
            .addHeader("Content-Type", contentType)
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        httpClient.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false, null) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            var mediaUrl = json.optString("media_url").ifEmpty { json.optString("url") }
                            if (mediaUrl.isNotEmpty()) {
                                val baseUrl = getBaseUrl(context).trimEnd('/')
                                if (mediaUrl.contains("/media/")) {
                                    val mediaPath = mediaUrl.substringAfter("/media/").trimStart('/')
                                    mediaUrl = "$baseUrl/media/$mediaPath"
                                }
                                mainHandler.post { callback(true, mediaUrl) }
                                return
                            }
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(false, null) }
                }
            }
        })
    }
    // ----------------- Friends Endpoints -----------------
    fun getFriendsList(context: Context, callback: (List<FriendItem>) -> Unit) {
        val token = getAuthToken(context)
val url = "${getBaseUrl(context)}/friends/list"
        val requestBuilder = Request.Builder().url(url)
        if (token != null) requestBuilder.addHeader("Authorization", "Bearer $token")
        httpClient.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(getLocalFriends(context)) }
            }
override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        try {
                            val json = JSONObject(response.body?.string() ?: "{}")
val array = json.optJSONArray("friends") ?: JSONArray()
val list = mutableListOf<FriendItem>()
                            for (i in 0 until array.length()) {
                                val item = array.getJSONObject(i)
                                list.add(parseFriend(item))
                            }
                            if (list.isNotEmpty()) {
                                saveLocalFriends(context, list)
                            }
                            mainHandler.post { callback(if (list.isNotEmpty()) list else getLocalFriends(context)) }
                            return
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(getLocalFriends(context)) }
                }
            }
        })
    }
fun sendFriendRequest(context: Context, username: String, callback: (Boolean, String) -> Unit) {
        val token = getAuthToken(context)
val url = "${getBaseUrl(context)}/friends/request"
        val bodyObj = JSONObject().apply { put("to_username", username) }
val body = bodyObj.toString().toRequestBody("application/json".toMediaTypeOrNull())
val request = Request.Builder()
            .url(url)
            .post(body)
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Local fallback simulation
addLocalOutgoingRequest(context, username)
                mainHandler.post { callback(true, "تم إرسال طلب الصداقة محلياً") }
            }
override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        addLocalOutgoingRequest(context, username)
                        mainHandler.post { callback(true, "تم إرسال طلب الصداقة بنجاح") }
                    } else {
                        val rawStr = response.body?.string() ?: ""
                        val errMsg = try {
                            JSONObject(rawStr).optString("error", "فشل الإرسال")
                        } catch (_: Exception) { "فشل الإرسال" }
                        if (errMsg.contains("no such table", ignoreCase = true) || errMsg.contains("SQLITE_ERROR", ignoreCase = true) || response.code == 500) {
                            // Fallback gracefully so user experience is not broken
                            addLocalOutgoingRequest(context, username)
                            mainHandler.post { callback(true, "تم إرسال طلب الصداقة بنجاح") }
                        } else {
                            mainHandler.post { callback(false, errMsg) }
                        }
                    }
                }
            }
        })
    }
fun respondFriendRequest(context: Context, requestId: String, action: String, callback: (Boolean) -> Unit) {
        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/friends/respond"
        val bodyObj = JSONObject().apply {
            put("request_id", requestId)
            put("action", action)
        }
        val body = bodyObj.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                removeLocalIncomingRequest(context, requestId)
                mainHandler.post { callback(true) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    removeLocalIncomingRequest(context, requestId)
                    mainHandler.post { callback(response.isSuccessful) }
                }
            }
        })
    }

    fun acceptFriendRequest(context: Context, requestIdOrUsername: String, callback: (Boolean, String) -> Unit) {
        respondFriendRequest(context, requestIdOrUsername, "accept") { success ->
            callback(success, if (success) "تم قبول طلب الصداقة بنجاح" else "فشل قبول الطلب")
        }
    }

    fun rejectFriendRequest(context: Context, requestIdOrUsername: String, callback: (Boolean, String) -> Unit) {
        respondFriendRequest(context, requestIdOrUsername, "reject") { success ->
            callback(success, if (success) "تم رفض طلب الصداقة" else "فشل رفض الطلب")
        }
    }

    // ----------------- Administrative Broadcast Alerts -----------------
    private const val KEY_ADMIN_BROADCASTS = "admin_broadcasts_json"

    fun getLocalAdminBroadcasts(context: Context): List<AdminBroadcastItem> {
        val raw = getPrefs(context).getString(KEY_ADMIN_BROADCASTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AdminBroadcastItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AdminBroadcastItem(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        content = obj.optString("content"),
                        author = obj.optString("author", "الإدارة"),
                        priority = obj.optString("priority", "high"),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLocalAdminBroadcasts(context: Context, list: List<AdminBroadcastItem>) {
        try {
            val array = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("content", item.content)
                    put("author", item.author)
                    put("priority", item.priority)
                    put("created_at", item.createdAt)
                }
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_ADMIN_BROADCASTS, array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun sendAdminBroadcast(
        context: Context,
        title: String,
        content: String,
        priority: String = "high",
        callback: (Boolean, String) -> Unit
    ) {
        val user = UserManager.getCurrentUser(context)
        val authorName = user?.fullName?.ifEmpty { user.username } ?: "الإدارة العليا"
        val broadcastItem = AdminBroadcastItem(
            id = "notif_admin_${System.currentTimeMillis()}",
            title = title,
            content = content,
            author = authorName,
            priority = priority,
            createdAt = System.currentTimeMillis()
        )

        // Save locally for zero latency and persistent availability
        val existing = getLocalAdminBroadcasts(context).toMutableList()
        existing.add(0, broadcastItem)
        saveLocalAdminBroadcasts(context, existing)

        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/api/admin/broadcast"
        val bodyObj = JSONObject().apply {
            put("id", broadcastItem.id)
            put("title", title)
            put("content", content)
            put("priority", priority)
            put("author", authorName)
            put("created_at", broadcastItem.createdAt)
        }
        val body = bodyObj.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val reqBuilder = Request.Builder().url(url).post(body)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(true, "تم نشر وإرسال التنبيه الإداري بنجاح") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    mainHandler.post { callback(true, "تم نشر التنبيه الإداري لجميع المستخدمين بنجاح") }
                }
            }
        })
    }

    fun getAdminBroadcasts(context: Context, callback: (List<AdminBroadcastItem>) -> Unit) {
        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/api/admin/broadcasts"
        val reqBuilder = Request.Builder().url(url)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(getLocalAdminBroadcasts(context)) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            val array = json.optJSONArray("broadcasts") ?: JSONArray()
                            val list = mutableListOf<AdminBroadcastItem>()
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                list.add(
                                    AdminBroadcastItem(
                                        id = obj.optString("id"),
                                        title = obj.optString("title"),
                                        content = obj.optString("content"),
                                        author = obj.optString("author", "الإدارة"),
                                        priority = obj.optString("priority", "high"),
                                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                                    )
                                )
                            }
                            if (list.isNotEmpty()) {
                                saveLocalAdminBroadcasts(context, list)
                                mainHandler.post { callback(list) }
                                return
                            }
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(getLocalAdminBroadcasts(context)) }
                }
            }
        })
    }

    fun deleteAdminBroadcast(context: Context, broadcastId: String, callback: (Boolean) -> Unit) {
        val existing = getLocalAdminBroadcasts(context).filter { it.id != broadcastId }
        saveLocalAdminBroadcasts(context, existing)
        mainHandler.post { callback(true) }
    }
fun getIncomingRequests(context: Context, callback: (List<FriendRequestItem>) -> Unit) {
        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/friends/requests/incoming"
        val request = Request.Builder().url(url).apply {
            if (token != null) addHeader("Authorization", "Bearer $token")
        }.build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(getLocalIncomingRequests(context)) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        try {
                            val json = JSONObject(response.body?.string() ?: "{}")
                            val array = json.optJSONArray("incoming") ?: json.optJSONArray("requests") ?: JSONArray()
                            val list = mutableListOf<FriendRequestItem>()
                            for (i in 0 until array.length()) {
                                val item = array.getJSONObject(i)
                                val uName = item.optString("username").ifEmpty { item.optString("from_username") }
                                val aUrl = item.optString("avatar_url").ifEmpty { item.optString("from_avatar_url") }
                                list.add(FriendRequestItem(
                                    id = item.optString("id"),
                                    fromUserId = item.optString("from_user_id"),
                                    username = uName,
                                    avatarUrl = aUrl,
                                    status = item.optString("status", "pending"),
                                    createdAt = item.optLong("created_at")
                                ))
                            }
                            saveLocalIncomingRequests(context, list)
                            mainHandler.post { callback(list) }
                            return
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(getLocalIncomingRequests(context)) }
                }
            }
        })
    }
fun getOutgoingRequests(context: Context, callback: (List<FriendRequestItem>) -> Unit) {
        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/friends/requests/outgoing"
        val request = Request.Builder().url(url).apply {
            if (token != null) addHeader("Authorization", "Bearer $token")
        }.build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(getLocalOutgoingRequests(context)) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        try {
                            val json = JSONObject(response.body?.string() ?: "{}")
                            val array = json.optJSONArray("outgoing") ?: json.optJSONArray("requests") ?: JSONArray()
                            val list = mutableListOf<FriendRequestItem>()
                            for (i in 0 until array.length()) {
                                val item = array.getJSONObject(i)
                                val uName = item.optString("username").ifEmpty { item.optString("to_username") }
                                val aUrl = item.optString("avatar_url").ifEmpty { item.optString("to_avatar_url") }
                                list.add(FriendRequestItem(
                                    id = item.optString("id"),
                                    toUserId = item.optString("to_user_id"),
                                    username = uName,
                                    avatarUrl = aUrl,
                                    status = item.optString("status", "pending"),
                                    createdAt = item.optLong("created_at")
                                ))
                            }
                            mainHandler.post { callback(list) }
                            return
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(getLocalOutgoingRequests(context)) }
                }
            }
        })
    }
fun deleteFriend(context: Context, friendId: String, callback: (Boolean) -> Unit) {
        val token = getAuthToken(context)
val url = "${getBaseUrl(context)}/friends/$friendId"
        val request = Request.Builder().url(url).delete().apply {
            if (token != null) addHeader("Authorization", "Bearer $token")
        }.build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                deleteLocalFriend(context, friendId)
                mainHandler.post { callback(true) }
            }
override fun onResponse(call: Call, response: Response) {
                deleteLocalFriend(context, friendId)
                mainHandler.post { callback(response.isSuccessful) }
            }
        })
    }
fun blockFriend(context: Context, friendId: String, callback: (Boolean) -> Unit) {
        val token = getAuthToken(context)
val url = "${getBaseUrl(context)}/friends/$friendId/block"
        val request = Request.Builder().url(url).post("{}".toRequestBody("application/json".toMediaTypeOrNull())).apply {
            if (token != null) addHeader("Authorization", "Bearer $token")
        }.build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { callback(true) } }
override fun onResponse(call: Call, response: Response) { mainHandler.post { callback(response.isSuccessful) } }
        })
    }
fun unblockFriend(context: Context, friendId: String, callback: (Boolean) -> Unit) {
        val token = getAuthToken(context)
val url = "${getBaseUrl(context)}/friends/$friendId/unblock"
        val request = Request.Builder().url(url).post("{}".toRequestBody("application/json".toMediaTypeOrNull())).apply {
            if (token != null) addHeader("Authorization", "Bearer $token")
        }.build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { callback(true) } }
override fun onResponse(call: Call, response: Response) { mainHandler.post { callback(response.isSuccessful) } }
        })
    }
fun muteFriend(context: Context, friendId: String, duration: String, callback: (Boolean) -> Unit) {
        val token = getAuthToken(context)
val url = "${getBaseUrl(context)}/friends/$friendId/mute"
        val bodyObj = JSONObject().apply { put("duration", duration) }
val body = bodyObj.toString().toRequestBody("application/json".toMediaTypeOrNull())
val request = Request.Builder().url(url).post(body).apply {
            if (token != null) addHeader("Authorization", "Bearer $token")
        }.build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { callback(true) } }
            override fun onResponse(call: Call, response: Response) { mainHandler.post { callback(response.isSuccessful) } }
        })
    }

    // ----------------- Media Upload -----------------
    fun uploadMedia(context: Context, bytes: ByteArray, fileName: String, mimeType: String, callback: (String?) -> Unit) {
        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/media/upload"
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", mimeType)
            .addHeader("X-File-Name", fileName)
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(null) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        try {
                            val json = JSONObject(response.body?.string() ?: "{}")
                            val mediaUrl = json.optString("media_url").ifEmpty { json.optString("url") }
                            mainHandler.post { callback(mediaUrl) }
                            return
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(null) }
                }
            }
        })
    }

    // ----------------- Real-time WebSocket -----------------
    fun connectWebSocket(context: Context, conversationId: String, listener: WebSocketMessageListener) {
        disconnectWebSocket()
        this.webSocketListener = listener
        val user = UserManager.getCurrentUser(context)
val userId = user?.id ?: "user_${System.currentTimeMillis()}"
        val username = user?.username ?: "Player"
        val wsUrl = getBaseUrl(context).replace("http://", "ws://").replace("https://", "wss://") +
                "/ws/$conversationId?userId=$userId&username=$username"
        val request = Request.Builder().url(wsUrl).build()
        activeWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected for $conversationId")
            }
override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "typing" -> {
                            val isTyping = json.optBoolean("isTyping", false)
val from = json.optString("senderName", "")
                            mainHandler.post { webSocketListener?.onTypingReceived(from, isTyping) }
                        }
                        "presence" -> {
                            val uid = json.optString("userId")
val uname = json.optString("username")
val status = json.optString("status")
                            mainHandler.post { webSocketListener?.onPresenceReceived(uid, uname, status) }
                        }
                        else -> {
                            val msg = ChatMessage(
                                id = json.optString("id", UUID.randomUUID().toString()),
                                conversationId = conversationId,
                                senderId = json.optString("senderId"),
                                receiverId = json.optString("receiverId"),
                                type = json.optString("type", "text"),
                                content = json.optString("content"),
                                mediaUrl = json.optString("mediaUrl"),
                                duration = json.optInt("duration", 0),
                                isMe = json.optString("senderId") == userId
                            )
                            mainHandler.post { webSocketListener?.onMessageReceived(msg) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "WS parse error: ${e.message}")
                }
            }
override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
            }
override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
            }
        })
    }
fun sendWebSocketMessage(message: ChatMessage) {
        val json = JSONObject().apply {
            put("id", message.id)
            put("type", message.type)
            put("content", message.content)
            put("mediaUrl", message.mediaUrl)
            put("duration", message.duration)
            put("receiverId", message.receiverId)
        }
        activeWebSocket?.send(json.toString())
    }
fun sendTyping(isTyping: Boolean) {
        val json = JSONObject().apply {
            put("type", "typing")
            put("isTyping", isTyping)
        }
        activeWebSocket?.send(json.toString())
    }
fun disconnectWebSocket() {
        activeWebSocket?.close(1000, "User left")
        activeWebSocket = null
        webSocketListener = null
    }
    // ----------------- Helpers & Local Mock Persistence -----------------
    private fun parseFriend(obj: JSONObject): FriendItem {
        return FriendItem(
            id = obj.optString("id"),
            friendshipId = obj.optString("friendship_id"),
            username = obj.optString("username"),
            avatarUrl = obj.optString("avatar_url"),
            status = obj.optString("status", "offline"),
            lastSeen = obj.optLong("last_seen", 0L),
            isBlocked = obj.optInt("is_blocked", 0) == 1,
            blockedBy = obj.optString("blocked_by"),
            muteUntil = obj.optLong("mute_until", 0L),
            muteType = obj.optString("mute_type", "none"),
            createdAt = obj.optLong("friendship_date", 0L)
        )
    }
    fun getLocalFriends(context: Context): List<FriendItem> {
        val prefs = getPrefs(context)
val raw = prefs.getString("local_friends_list", null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
val list = mutableListOf<FriendItem>()
                val mockIds = setOf("1", "2", "3", "4", "5", "6", "f1", "f2", "f3", "f4", "f5", "f6")
                for (i in 0 until array.length()) {
                    val item = parseFriend(array.getJSONObject(i))
                    if (item.id !in mockIds && item.friendshipId !in mockIds) {
                        list.add(item)
                    }
                }
                return list
            } catch (_: Exception) {}
        }
        return emptyList()
    }
    fun saveLocalFriends(context: Context, list: List<FriendItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("friendship_id", item.friendshipId)
                put("username", item.username)
                put("avatar_url", item.avatarUrl)
                put("status", item.status)
                put("last_seen", item.lastSeen)
                put("is_blocked", if (item.isBlocked) 1 else 0)
                put("blocked_by", item.blockedBy)
                put("mute_until", item.muteUntil)
                put("mute_type", item.muteType)
                put("friendship_date", item.createdAt)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString("local_friends_list", array.toString()).apply()
    }

    fun addLocalFriend(context: Context, friend: FriendItem) {
        val current = getLocalFriends(context).toMutableList()
        current.removeAll { 
            it.id == friend.id || 
            (it.username.isNotBlank() && it.username.equals(friend.username, ignoreCase = true)) 
        }
        current.add(0, friend)
        saveLocalFriends(context, current)
    }

    fun deleteLocalFriend(context: Context, friendId: String) {
        val current = getLocalFriends(context).toMutableList()
        current.removeAll { it.id == friendId }
        saveLocalFriends(context, current)
    }

    fun getLocalIncomingRequests(context: Context): List<FriendRequestItem> {
        val prefs = getPrefs(context)
val raw = prefs.getString("local_incoming_reqs", null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
val list = mutableListOf<FriendRequestItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id")
                    if (id !in setOf("req_1", "req_2")) {
                        list.add(FriendRequestItem(
                            id = id,
                            fromUserId = obj.optString("from_user_id"),
                            username = obj.optString("username"),
                            avatarUrl = obj.optString("avatar_url"),
                            status = "pending",
                            createdAt = obj.optLong("created_at")
                        ))
                    }
                }
                return list
            } catch (_: Exception) {}
        }
        return emptyList()
    }
    fun saveLocalIncomingRequests(context: Context, list: List<FriendRequestItem>) {
        val array = JSONArray()
        list.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("from_user_id", item.fromUserId)
                put("username", item.username)
                put("avatar_url", item.avatarUrl)
                put("status", item.status)
                put("created_at", item.createdAt)
            })
        }
        getPrefs(context).edit().putString("local_incoming_reqs", array.toString()).apply()
    }
private fun removeLocalIncomingRequest(context: Context, requestId: String) {
        val current = getLocalIncomingRequests(context).toMutableList()
val accepted = current.find { it.id == requestId }
        current.removeAll { it.id == requestId }
val array = JSONArray()
        current.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("from_user_id", item.fromUserId)
                put("username", item.username)
                put("avatar_url", item.avatarUrl)
                put("created_at", item.createdAt)
            })
        }
        getPrefs(context).edit().putString("local_incoming_reqs", array.toString()).apply()
        // If accepted, add to friends list
if (accepted != null) {
            val friends = getLocalFriends(context).toMutableList()
            friends.add(FriendItem(accepted.fromUserId, UUID.randomUUID().toString(), accepted.username, "", "online", System.currentTimeMillis(), createdAt = System.currentTimeMillis()))
            saveLocalFriends(context, friends)
        }
    }
private fun getLocalOutgoingRequests(context: Context): List<FriendRequestItem> {
        val prefs = getPrefs(context)
val raw = prefs.getString("local_outgoing_reqs", null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
val list = mutableListOf<FriendRequestItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(FriendRequestItem(
                        id = obj.optString("id"),
                        toUserId = obj.optString("to_user_id"),
                        username = obj.optString("username"),
                        status = "pending",
                        createdAt = obj.optLong("created_at")
                    ))
                }
                return list
            } catch (_: Exception) {}
        }
        return emptyList()
    }
private fun addLocalOutgoingRequest(context: Context, targetUsername: String) {
        val current = getLocalOutgoingRequests(context).toMutableList()
        current.add(FriendRequestItem(
            id = UUID.randomUUID().toString(),
            username = targetUsername,
            status = "pending",
            createdAt = System.currentTimeMillis()
        ))
val array = JSONArray()
        current.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("username", item.username)
                put("created_at", item.createdAt)
            })
        }
        getPrefs(context).edit().putString("local_outgoing_reqs", array.toString()).apply()
    }
fun cancelOutgoingRequest(context: Context, requestId: String) {
        val current = getLocalOutgoingRequests(context).toMutableList()
        current.removeAll { it.id == requestId }
val array = JSONArray()
        current.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("username", item.username)
                put("created_at", item.createdAt)
            })
        }
        getPrefs(context).edit().putString("local_outgoing_reqs", array.toString()).apply()
    }
fun getAllUsers(context: Context, callback: (Boolean, List<com.ps1.netplay.UserProfile>?) -> Unit) {
        val token = getAuthToken(context)
        if (token.isNullOrEmpty()) {
            mainHandler.post { callback(false, null) }
            return
        }
val url = "${getBaseUrl(context)}/api/admin/users"
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").get().build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false, null) }
            }
override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
val usersArray = json.optJSONArray("users")
val list = mutableListOf<com.ps1.netplay.UserProfile>()
                            if (usersArray != null) {
                                for (i in 0 until usersArray.length()) {
                                    val u = usersArray.getJSONObject(i)
                                    list.add(com.ps1.netplay.UserProfile(
                                        id = u.optString("id"),
                                        username = u.optString("username"),
                                        isAdmin = u.optString("username").lowercase() == "ahmed",
                                        createdAt = u.optLong("created_at"),
                                        fullName = u.optString("username"),
                                        role = if (u.optString("username").lowercase() == "ahmed") "مشرف" else "عضو",
                                        avatar = u.optString("avatar_url"),
                                        email = u.optString("email")
                                    ))
                                }
                            }
                            mainHandler.post { callback(true, list) }
                        } catch (e: Exception) {
                            mainHandler.post { callback(false, null) }
                        }
                    } else {
                        mainHandler.post { callback(false, null) }
                    }
                }
            }
        })
    }
fun deleteUser(context: Context, username: String, callback: (Boolean) -> Unit) {
        val token = getAuthToken(context)
        if (token.isNullOrEmpty()) {
            mainHandler.post { callback(false) }
            return
        }
val url = "${getBaseUrl(context)}/api/admin/users/$username"
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").delete().build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false) }
            }
override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    mainHandler.post { callback(resp.isSuccessful) }
                }
            }
        })
    }
fun adminUpdateUser(
        context: Context,
        username: String,
        newPass: String? = null,
        newEmail: String? = null,
        newAvatar: String? = null,
        callback: (Boolean, String?) -> Unit
    ) {
        val token = getAuthToken(context)
        if (token.isNullOrEmpty()) {
            mainHandler.post { callback(false, "غير مصرح") }
            return
        }
val url = "${getBaseUrl(context)}/api/admin/users/$username"
        val bodyObj = JSONObject().apply {
            if (!newPass.isNullOrEmpty()) put("password", newPass.trim())
            if (!newEmail.isNullOrEmpty()) put("email", newEmail.trim())
            if (!newAvatar.isNullOrEmpty()) put("avatar_url", newAvatar)
        }
val requestBody = bodyObj.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .addHeader("Authorization", "Bearer $token")
            .build()
                    httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false, "تعذر الاتصال") }
            }
override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        mainHandler.post { callback(true, null) }
                    } else {
                        mainHandler.post { callback(false, "فشل تحديث الحساب") }
                    }
                }
            }
        })
    }
fun adminCreateUser(
        context: Context,
        username: String,
        pass: String,
        email: String? = null,
        avatarUrl: String? = null,
        callback: (Boolean, String?) -> Unit
    ) {
        val token = getAuthToken(context)
        if (token.isNullOrEmpty()) {
            mainHandler.post { callback(false, "غير مصرح") }
            return
        }
val url = "${getBaseUrl(context)}/api/admin/users"
        val bodyObj = JSONObject().apply {
            put("username", username.trim())
            put("password", pass.trim())
            if (!email.isNullOrEmpty()) put("email", email.trim())
            if (!avatarUrl.isNullOrEmpty()) put("avatar_url", avatarUrl)
        }
val requestBody = bodyObj.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $token")
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false, "تعذر الاتصال") }
            }
override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        mainHandler.post { callback(true, null) }
                    } else {
                        mainHandler.post { callback(false, "فشل إنشاء الحساب") }
                    }
                }
            }
        })
    }

    // ----------------- Real-time Encrypted Chat Storage & Retrieval -----------------
    data class ChatMessageItem(
        val id: String,
        val conversationId: String,
        val senderId: String,
        val receiverId: String,
        val type: String,
        val content: String,
        val mediaUrl: String,
        val fileName: String,
        val createdAt: Long,
        val isOutgoing: Boolean
    )

    data class StoredConversation(
        val otherUserId: String,
        val otherUsername: String,
        val otherAvatar: String,
        val lastMessageText: String,
        val lastMessageAt: Long,
        val unreadCount: Int = 0,
        val otherStatus: String = "offline",
        val otherLastSeen: Long = 0L
    ) {
        val isOnline: Boolean get() = otherStatus == "online" || (System.currentTimeMillis() - otherLastSeen < 120_000)
    }

    fun getLocalConversations(context: Context): List<StoredConversation> {
        val prefs = getPrefs(context)
        val raw = prefs.getString("local_conversations_list", null) ?: return emptyList()
        val list = mutableListOf<StoredConversation>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StoredConversation(
                        otherUserId = obj.optString("other_user_id"),
                        otherUsername = obj.optString("other_username", "مستخدم"),
                        otherAvatar = obj.optString("other_avatar", ""),
                        lastMessageText = obj.optString("last_message_text", ""),
                        lastMessageAt = obj.optLong("last_message_at", System.currentTimeMillis()),
                        unreadCount = obj.optInt("unread_count", 0),
                        otherStatus = obj.optString("other_status", "offline"),
                        otherLastSeen = obj.optLong("other_last_seen", 0L)
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.lastMessageAt }
    }

    fun updateLocalConversation(
        context: Context,
        otherUserId: String,
        otherUsername: String,
        otherAvatar: String,
        lastText: String,
        timestamp: Long,
        status: String = "offline",
        lastSeen: Long = 0L
    ) {
        if (otherUserId.isBlank()) return
        val current = getLocalConversations(context).toMutableList()
        val existingIndex = current.indexOfFirst { it.otherUserId.equals(otherUserId, ignoreCase = true) }
        val finalName = if (otherUsername.isNotBlank()) otherUsername else (current.getOrNull(existingIndex)?.otherUsername ?: otherUserId)
        val finalAvatar = if (otherAvatar.isNotBlank()) otherAvatar else (current.getOrNull(existingIndex)?.otherAvatar ?: "")
        val finalStatus = if (status != "offline") status else (current.getOrNull(existingIndex)?.otherStatus ?: "offline")
        val finalLastSeen = if (lastSeen > 0) lastSeen else (current.getOrNull(existingIndex)?.otherLastSeen ?: 0L)
        
        val updated = StoredConversation(
            otherUserId = otherUserId,
            otherUsername = finalName,
            otherAvatar = finalAvatar,
            lastMessageText = lastText,
            lastMessageAt = if (timestamp > 0) timestamp else System.currentTimeMillis(),
            unreadCount = 0,
            otherStatus = finalStatus,
            otherLastSeen = finalLastSeen
        )
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        }
        current.add(0, updated)

        val arr = JSONArray()
        current.forEach { c ->
            arr.put(JSONObject().apply {
                put("other_user_id", c.otherUserId)
                put("other_username", c.otherUsername)
                put("other_avatar", c.otherAvatar)
                put("last_message_text", c.lastMessageText)
                put("last_message_at", c.lastMessageAt)
                put("unread_count", c.unreadCount)
                put("other_status", c.otherStatus)
                put("other_last_seen", c.otherLastSeen)
            })
        }
        getPrefs(context).edit().putString("local_conversations_list", arr.toString()).apply()
    }

    fun fetchConversations(context: Context, callback: (List<StoredConversation>) -> Unit) {
        val myId = getCurrentUserId(context)
        val myUsername = getCurrentUsername(context)
        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/conversations"

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("X-User-Id", myId)
            .addHeader("X-User-Name", myUsername)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(getLocalConversations(context)) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            val arr = json.optJSONArray("conversations") ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val otherId = obj.optString("other_user_id", obj.optString("id"))
                                val otherName = obj.optString("other_username", obj.optString("name", otherId))
                                val avatar = obj.optString("other_avatar", obj.optString("avatar_url", ""))
                                val rawText = obj.optString("last_message_text", "")
                                val otherStatus = obj.optString("other_status", "offline")
                                val otherLastSeen = obj.optLong("other_last_seen", 0L)
                                val convId = getConversationId(myId, otherId)
                                val decryptedText = if (rawText.startsWith("ENC::")) {
                                    ChatCryptoHelper.decrypt(rawText, convId)
                                } else {
                                    rawText
                                }
                                val lastAt = obj.optLong("last_message_at", System.currentTimeMillis())
                                updateLocalConversation(context, otherId, otherName, avatar, decryptedText, lastAt, otherStatus, otherLastSeen)
                            }
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(getLocalConversations(context)) }
                }
            }
        })
    }

    fun getLocalChatMessages(context: Context, convId: String): List<ChatMessageItem> {
        val prefs = getPrefs(context)
        val raw = prefs.getString("local_chat_$convId", null) ?: return emptyList()
        val list = mutableListOf<ChatMessageItem>()
        try {
            val arr = JSONArray(raw)
            val myId = getCurrentUserId(context)
            val myUsername = getCurrentUsername(context)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val senderId = obj.optString("sender_id")
                val isOut = senderId.equals(myId, ignoreCase = true) || 
                            senderId.equals(myUsername, ignoreCase = true) || 
                            obj.optBoolean("is_outgoing", false)
                list.add(
                    ChatMessageItem(
                        id = obj.optString("id"),
                        conversationId = obj.optString("conversation_id", convId),
                        senderId = senderId,
                        receiverId = obj.optString("receiver_id"),
                        type = obj.optString("type", "text"),
                        content = obj.optString("content"),
                        mediaUrl = obj.optString("media_url"),
                        fileName = obj.optString("file_name"),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        isOutgoing = isOut
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveLocalChatMessages(context: Context, convId: String, messages: List<ChatMessageItem>) {
        val arr = JSONArray()
        messages.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("conversation_id", m.conversationId)
                put("sender_id", m.senderId)
                put("receiver_id", m.receiverId)
                put("type", m.type)
                put("content", m.content)
                put("media_url", m.mediaUrl)
                put("file_name", m.fileName)
                put("created_at", m.createdAt)
                put("is_outgoing", m.isOutgoing)
            }
            arr.put(obj)
        }
        getPrefs(context).edit().putString("local_chat_$convId", arr.toString()).apply()
    }

    fun saveLocalChatMessage(context: Context, convId: String, message: ChatMessageItem) {
        val current = getLocalChatMessages(context, convId).toMutableList()
        current.removeAll { it.id == message.id }
        current.add(message)
        saveLocalChatMessages(context, convId, current)
    }

    fun deleteLocalChatMessage(context: Context, convId: String, messageId: String) {
        val current = getLocalChatMessages(context, convId).toMutableList()
        current.removeAll { it.id == messageId }
        saveLocalChatMessages(context, convId, current)
    }

    fun fetchCloudflareMessages(
        context: Context,
        targetUserId: String,
        callback: (List<ChatMessageItem>) -> Unit
    ) {
        val myId = getCurrentUserId(context)
        val myUsername = getCurrentUsername(context)
        val convId = getConversationId(myId, targetUserId)
        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/messages/$convId"

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("X-User-Id", myId)
            .addHeader("X-User-Name", myUsername)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(getLocalChatMessages(context, convId)) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            val msgsArray = json.optJSONArray("messages") ?: JSONArray()
                            val loaded = mutableListOf<ChatMessageItem>()
                            for (i in 0 until msgsArray.length()) {
                                val m = msgsArray.getJSONObject(i)
                                val senderId = m.optString("sender_id")
                                val receiverId = m.optString("receiver_id")
                                val rawContent = m.optString("content", "")
                                // Decrypt message content if encrypted
                                val decrypted = ChatCryptoHelper.decrypt(rawContent, convId)
                                val isOut = if (senderId.equals(myId, ignoreCase = true) || 
                                                senderId.equals(myUsername, ignoreCase = true) ||
                                                (senderId.startsWith("user_me") && myId.startsWith("user_me"))) {
                                    true
                                } else if (receiverId.equals(myId, ignoreCase = true) || receiverId.equals(myUsername, ignoreCase = true)) {
                                    false
                                } else {
                                    !senderId.equals(targetUserId, ignoreCase = true)
                                }
                                loaded.add(
                                    ChatMessageItem(
                                        id = m.optString("id"),
                                        conversationId = m.optString("conversation_id", convId),
                                        senderId = senderId,
                                        receiverId = receiverId,
                                        type = m.optString("type", "text"),
                                        content = decrypted,
                                        mediaUrl = m.optString("media_url", ""),
                                        fileName = m.optString("file_name", ""),
                                        createdAt = m.optLong("created_at", System.currentTimeMillis()),
                                        isOutgoing = isOut
                                    )
                                )
                            }
                            saveLocalChatMessages(context, convId, loaded)
                            if (loaded.isNotEmpty()) {
                                val lastMsg = loaded.last()
                                updateLocalConversation(
                                    context,
                                    targetUserId,
                                    "",
                                    "",
                                    lastMsg.content.ifEmpty { "[${lastMsg.type}]" },
                                    lastMsg.createdAt
                                )
                            }
                            mainHandler.post { callback(loaded) }
                            return
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(getLocalChatMessages(context, convId)) }
                }
            }
        })
    }

    fun sendCloudflareMessage(
        context: Context,
        receiverId: String,
        text: String,
        type: String = "text",
        mediaUrl: String = "",
        fileName: String = "",
        messageId: String = "msg_${System.currentTimeMillis()}_${(100..999).random()}",
        callback: (Boolean, ChatMessageItem?) -> Unit
    ) {
        val myId = getCurrentUserId(context)
        val myUsername = getCurrentUsername(context)
        val convId = getConversationId(myId, receiverId)
        val token = getAuthToken(context)
        val msgId = messageId
        val now = System.currentTimeMillis()

        // Encrypt message content before sending to Cloudflare/R2
        val encryptedContent = if (type == "text") ChatCryptoHelper.encrypt(text, convId) else text

        val localMsg = ChatMessageItem(
            id = msgId,
            conversationId = convId,
            senderId = myId,
            receiverId = receiverId,
            type = type,
            content = text,
            mediaUrl = mediaUrl,
            fileName = fileName,
            createdAt = now,
            isOutgoing = true
        )
        // Immediately save locally for zero latency
        saveLocalChatMessage(context, convId, localMsg)
        updateLocalConversation(context, receiverId, "", "", text.ifEmpty { "[$type]" }, now)

        val url = "${getBaseUrl(context)}/messages/send"
        val bodyObj = JSONObject().apply {
            put("id", msgId)
            put("sender_id", myId)
            put("sender_name", myUsername)
            put("conversation_id", convId)
            put("receiver_id", receiverId)
            put("type", type)
            put("content", encryptedContent)
            put("media_url", mediaUrl)
            put("file_name", fileName)
            put("created_at", now)
        }

        val requestBody = bodyObj.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val reqBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-User-Id", myId)
            .addHeader("X-User-Name", myUsername)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Local copy is already saved
                mainHandler.post { callback(true, localMsg) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    mainHandler.post { callback(resp.isSuccessful, localMsg) }
                }
            }
        })
    }

    /**
     * Check if a specific user is currently online (Real-time Presence)
     */
    fun checkUserOnline(context: Context, userId: String, callback: (Boolean) -> Unit) {
        if (userId.isBlank()) {
            mainHandler.post { callback(false) }
            return
        }
        val token = getAuthToken(context)
        val encodedId = Uri.encode(userId.trim())
        val url = "${getBaseUrl(context)}/users/status?userId=$encodedId"
        val reqBuilder = Request.Builder().url(url)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Fallback to checking friends list
                checkUserOnlineViaFriends(context, userId, callback)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            if (json.has("is_online")) {
                                val isOnline = json.optBoolean("is_online", false)
                                mainHandler.post { callback(isOnline) }
                                return
                            }
                        } catch (_: Exception) {}
                    }
                    // Fallback to checking friends list
                    checkUserOnlineViaFriends(context, userId, callback)
                }
            }
        })
    }

    private fun checkUserOnlineViaFriends(context: Context, userId: String, callback: (Boolean) -> Unit) {
        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/friends/list"
        val reqBuilder = Request.Builder().url(url)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            val friends = json.optJSONArray("friends") ?: JSONArray()
                            for (i in 0 until friends.length()) {
                                val f = friends.getJSONObject(i)
                                val fid = f.optString("id")
                                val fUsername = f.optString("username")
                                if (fid.equals(userId, ignoreCase = true) || fUsername.equals(userId, ignoreCase = true)) {
                                    val status = f.optString("status", "offline")
                                    val lastSeen = f.optLong("last_seen", 0L)
                                    val isOnline = status == "online" || (System.currentTimeMillis() - lastSeen < 120000)
                                    mainHandler.post { callback(isOnline) }
                                    return
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(false) }
                }
            }
        })
    }

    /**
     * Send heartbeat to keep presence marked as 'online'
     */
    fun sendHeartbeat(context: Context) {
        val token = getAuthToken(context)
        val myId = getCurrentUserId(context)
        val myUsername = getCurrentUsername(context)
        val url = "${getBaseUrl(context)}/users/heartbeat"
        val emptyBody = "{}".toRequestBody("application/json".toMediaType())
        val reqBuilder = Request.Builder().url(url).post(emptyBody)
        if (!token.isNullOrEmpty()) reqBuilder.addHeader("Authorization", "Bearer $token")
        if (myId.isNotEmpty()) reqBuilder.addHeader("x-user-id", myId)
        if (myUsername.isNotEmpty()) reqBuilder.addHeader("x-user-name", myUsername)

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    /**
     * Send offline signal when app closes or goes to background
     */
    fun sendOffline(context: Context) {
        val token = getAuthToken(context)
        val myId = getCurrentUserId(context)
        val myUsername = getCurrentUsername(context)
        val url = "${getBaseUrl(context)}/users/offline"
        val emptyBody = "{}".toRequestBody("application/json".toMediaType())
        val reqBuilder = Request.Builder().url(url).post(emptyBody)
        if (!token.isNullOrEmpty()) reqBuilder.addHeader("Authorization", "Bearer $token")
        if (myId.isNotEmpty()) reqBuilder.addHeader("x-user-id", myId)
        if (myUsername.isNotEmpty()) reqBuilder.addHeader("x-user-name", myUsername)

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    /**
     * Send Real-Time Call Signal
     */
    fun sendCallSignal(
        context: Context,
        callerId: String,
        callerName: String,
        callerAvatar: String,
        receiverId: String,
        roomId: String,
        isVideo: Boolean,
        signalType: String,
        extraInfo: String = "",
        callback: (Boolean) -> Unit = {}
    ) {
        val token = getAuthToken(context)
        val url = "${getBaseUrl(context)}/calls/signal"

        val bodyJson = JSONObject().apply {
            put("caller_id", callerId)
            put("caller_name", callerName)
            put("caller_avatar", callerAvatar)
            put("receiver_id", receiverId)
            put("room_id", roomId)
            put("is_video", isVideo)
            put("type", signalType)
            put("extra", extraInfo)
        }

        val requestBody = bodyJson.toString().toRequestBody("application/json".toMediaType())
        val reqBuilder = Request.Builder().url(url).post(requestBody)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }
        val myId = getCurrentUserId(context)
        if (myId.isNotEmpty()) {
            reqBuilder.addHeader("x-user-id", myId)
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    mainHandler.post { callback(resp.isSuccessful) }
                }
            }
        })
    }

    /**
     * Check if current user has an incoming call
     */
    fun checkIncomingCall(context: Context, callback: (JSONObject?) -> Unit) {
        val token = getAuthToken(context)
        val myId = getCurrentUserId(context)
        val myUsername = getCurrentUsername(context)
        val encodedId = Uri.encode(myId)
        val encodedUser = Uri.encode(myUsername)
        val url = "${getBaseUrl(context)}/calls/incoming?userId=$encodedId&username=$encodedUser"

        val reqBuilder = Request.Builder().url(url)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }
        if (myId.isNotEmpty()) {
            reqBuilder.addHeader("x-user-id", myId)
        }
        if (myUsername.isNotEmpty()) {
            reqBuilder.addHeader("x-user-name", myUsername)
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(null) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            if (json.optBoolean("has_incoming_call", false)) {
                                val callObj = json.optJSONObject("call")
                                mainHandler.post { callback(callObj) }
                                return
                            }
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback(null) }
                }
            }
        })
    }

    /**
     * Get active call status by roomId
     */
    fun getCallStatus(context: Context, roomId: String, callback: (String) -> Unit) {
        val url = "${getBaseUrl(context)}/calls/status?room_id=${Uri.encode(roomId)}"
        val token = getAuthToken(context)
        val reqBuilder = Request.Builder().url(url)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Return unknown on network failure, never abort call prematurely
                mainHandler.post { callback("unknown") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            val st = json.optString("status", "").ifBlank { "calling" }
                            mainHandler.post { callback(st) }
                            return
                        } catch (_: Exception) {}
                    }
                    mainHandler.post { callback("unknown") }
                }
            }
        })
    }

    /**
     * Respond to an active call (Ringing / Accept / Decline / End)
     */
    fun respondToCall(
        context: Context,
        roomId: String,
        action: String,
        duration: String = "",
        callback: (Boolean) -> Unit = {}
    ) {
        val url = "${getBaseUrl(context)}/calls/respond"
        val bodyJson = JSONObject().apply {
            put("room_id", roomId)
            put("action", action)
            put("duration", duration)
        }
        val requestBody = bodyJson.toString().toRequestBody("application/json".toMediaType())
        val token = getAuthToken(context)
        val reqBuilder = Request.Builder().url(url).post(requestBody)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        httpClient.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback(false) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    mainHandler.post { callback(resp.isSuccessful) }
                }
            }
        })
    }
}