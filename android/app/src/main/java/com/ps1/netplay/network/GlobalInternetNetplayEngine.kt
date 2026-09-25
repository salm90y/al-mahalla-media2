package com.ps1.netplay.network

 
import  android.widget.*
import  android.content.Context 
import  android.os.Handler
import  android.os.Looper 
import  android.util.Base64
import  android.util.Log 
import  kotlinx.coroutines.*
import  okhttp3.* 
import  okhttp3.MediaType.Companion.toMediaType
import  okhttp3.RequestBody.Companion.toRequestBody 
import  org.json.JSONObject
import  java.io.BufferedReader 
import  java.io.ByteArrayInputStream
import  java.io.ByteArrayOutputStream 
import  java.io.InputStreamReader
import  java.io.PrintWriter 
import  java.net.*
import  java.util.concurrent.ConcurrentHashMap 
import  java.util.concurrent.TimeUnit
import  java.util.concurrent.atomic.AtomicBoolean 
import  java.util.zip.GZIPInputStream
import  java.util.zip.GZIPOutputStream

/** * High-Reliability Global Internet Netplay Engine * Powered by Realtime Topic-Based WebSocket Channels (ntfy.sh relay) + HTTP Polling Fallback + Local Direct TCP/UDP */class GlobalInternetNetplayEngine(
    private val context: Context,
    private val playerName: String) : PeerTransport {
    companion object {
        private const val TAG = "PS1_Netplay"
        const val DEFAULT_PORT = 8990
        const val BROADCAST_PORT = 8991
        private val discoveredRooms = ConcurrentHashMap<String, DiscoveredRoomInfo>()
private fun getTopicName(roomCode: String): String {
            val sanitized = roomCode.trim().lowercase().filter { it.isLetterOrDigit() }
            return "ps1_combat3_$sanitized"
        }
    }
data class DiscoveredRoomInfo(
        val roomCode: String,
        val hostIp: String,
        val hostPort: Int = DEFAULT_PORT,
        val timestamp: Long = System.currentTimeMillis()
    )
private val mainHandler = Handler(Looper.getMainLooper())
private val isRunning = AtomicBoolean(false)
private var isHost = false
    private var currentRoomCode = ""
    private var currentPassword = ""
    var isPeerConnected = false
    // Local Socket
    private var serverSocket: ServerSocket? = null
    private var directSocket: Socket? = null
    private var directWriter: PrintWriter? = null
    private var directReader: BufferedReader? = null
    // OkHttp Client
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
private var webSocket: WebSocket? = null
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var broadcastJob: Job? = null
    private var pollJob: Job? = null
    private var heartbeatJob: Job? = null
    private var timeoutJob: Job? = null
    // Callbacks
    private var onInputReceived: ((frameIndex: Long, inputMask: Int) -> Unit)? = null
    private var onConnectionState: ((connected: Boolean, pingMs: Long) -> Unit)? = null
    var onChatMessageReceived: ((sender: String, isHost: Boolean, message: String) -> Unit)? = null
    var onPeerJoined: ((peerName: String, isHost: Boolean) -> Unit)? = null
    var onPeerLeft: ((peerName: String) -> Unit)? = null
    // State Synchronization Callbacks
    private val chunkBuffers = ConcurrentHashMap<String, Array<String?>>()
private var onStateSyncReceived: ((hostFrame: Long, stateBytes: ByteArray?) -> Unit)? = null
    private var onSyncGameStartReceived: ((startFrame: Long) -> Unit)? = null
    private var onStateSyncRequested: (() -> Unit)? = null
    var onRemoteGameInfoReceived: ((gameTitle: String) -> Unit)? = null
    private var hostGameTitle: String = "Combat 3 (Built-in)"
    private var pendingJoinCallback: ((Boolean, String) -> Unit)? = null
    fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
    /**
     * Start Hosting a Room
     */
    fun startGlobalHost(
        roomCode: String,
        roomName: String,
        password: String,
        gameTitle: String,
        onReady: (Boolean, String) -> Unit
    ) {
        stop()
        isHost = true
        hostGameTitle = gameTitle.ifBlank { "Combat 3 (المدمجة)" }
        currentRoomCode = roomCode.trim().uppercase()
        currentPassword = password.trim()
        isRunning.set(true)
        isPeerConnected = false
        val localIp = getLocalIpAddress()
        discoveredRooms[currentRoomCode] = DiscoveredRoomInfo(currentRoomCode, localIp, DEFAULT_PORT)
        // 1. Immediately notify caller so host UI starts with 0 delay
mainHandler.post {
            onReady(true, "الغرفة جاهزة بنجاح (كود: $currentRoomCode)")
            onConnectionState?.invoke(true, 15L)
        }
val scope = CoroutineScope(Dispatchers.IO)
        // 2. Start Local TCP / UDP Discovery ( // (for instant Wi-Fi/Hotspot play)
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(DEFAULT_PORT).apply { reuseAddress = true }
                startLocalBroadcast(currentRoomCode, roomName, localIp, gameTitle)
                while (isRunning.get()) {
                    val socket = serverSocket?.accept() ?: break
                    handleIncomingDirectClient(socket)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Local server socket stopped: ${e.message}")
            }
        }
        // 3. Connect to Global WebSocket Topic and Announce Host
        val topic = getTopicName(currentRoomCode)
        scope.launch {
            connectWebSocketTopic(topic) {
                // Announce Host Online immediately upon connection
sendAnnounce(roomName, gameTitle, localIp)
                startHeartbeat()
            }
            // Also send HTTP POST announce in parallel to ensure message persistence in topic
sendAnnounce(roomName, gameTitle, localIp)
        }
        // 5. Background Keep-Alive & Discovery Refresh
scope.launch {
            while (isRunning.get() && isHost) {
                delay(3000)
                if (isRunning.get()) {
                    sendAnnounce(roomName, gameTitle, localIp)
                }
            }
        }
    }
private fun sendAnnounce(roomName: String, gameTitle: String, localIp: String) {
        val json = JSONObject().apply {
            put("type", "HOST_ONLINE")
            put("room", currentRoomCode)
            put("hostName", playerName)
            put("roomName", roomName)
            put("gameTitle", gameTitle)
            put("hasPassword", currentPassword.isNotEmpty())
            put("localIp", localIp)
            put("time", System.currentTimeMillis())
        }
        publishToTopic(json.toString())
    }
    /**
     * Join an existing room via Global Internet or Local LAN
     */
    fun joinGlobalRoom(
        roomCodeOrIp: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        stop()
        isHost = false
        currentRoomCode = roomCodeOrIp.trim().uppercase()
        currentPassword = password.trim()
        isRunning.set(true)
        isPeerConnected = false
        this.pendingJoinCallback = onResult
        val scope = CoroutineScope(Dispatchers.IO)
val target = roomCodeOrIp.trim()
val hostIp = when {
            target.contains(".") -> target
            discoveredRooms.containsKey(target.uppercase()) -> discoveredRooms[target.uppercase()]!!.hostIp
            else -> null
        }
        // Method 1: Check Local Direct TCP if IP is known or on same network
        if (hostIp != null) {
            clientJob = scope.launch {
                try {
                    val socket = Socket()
                    socket.tcpNoDelay = true
                    socket.connect(InetSocketAddress(hostIp, DEFAULT_PORT), 1000)
val writer = PrintWriter(socket.getOutputStream(), true)
val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
val req = JSONObject().apply {
                        put("type", "JOIN_REQ")
                        put("roomCode", target.uppercase())
                        put("password", password)
                        put("playerName", playerName)
                    }
                    writer.println(req.toString())
val responseLine = reader.readLine()
                    if (responseLine != null) {
                        val resJson = JSONObject(responseLine)
                        if (resJson.optString("type") == "JOIN_ACCEPT") {
                            directSocket = socket
                            directWriter = writer
                            directReader = reader
                            isPeerConnected = true
                            timeoutJob?.cancel()
val hostName = resJson.optString("hostName", "المضيف")
                            mainHandler.post {
                                pendingJoinCallback?.invoke(true, "✅ تم الاتصال بنجاح مع $hostName!")
                                pendingJoinCallback = null
                                onPeerJoined?.invoke(hostName, true)
                                onConnectionState?.invoke(true, 12L)
                            }
                            listenToDirectSocket(reader)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    // Fall back to Internet
                }
            }
        }
        // Method 2: Cloudflare WebSocket & HTTP Topic Connect (Worldwide Relay)
        val topic = getTopicName(currentRoomCode)
                // Connect to WebSocket topic
connectWebSocketTopic(topic) {
            // Send Join Request immediately and repeatedly until connected
CoroutineScope(Dispatchers.IO).launch {
                var attempts = 0
                while (isRunning.get() && !isPeerConnected && attempts < 8) {
                    sendJoinRequest()
                    delay(1200)
                    attempts++
                }
            }
        }
        // Also send an initial Join Request via HTTP POST immediately
sendJoinRequest()
        // Method 4: Timeout after 14 seconds if no response
        timeoutJob = scope.launch {
            delay(12000)
            if (isRunning.get() && !isPeerConnected) {
                mainHandler.post {
                    pendingJoinCallback?.invoke(
                        false,
                        "❌ لم يتم العثور على الغرفة '$currentRoomCode'. تأكد من صحة الكود ومشاركة المضيف للغرفة."
                    )
                    pendingJoinCallback = null
                }
            }
        }
    }
private fun sendJoinRequest() {
        val joinReq = JSONObject().apply {
            put("type", "JOIN_REQUEST")
            put("room", currentRoomCode)
            put("guestName", playerName)
            put("password", currentPassword)
            put("guestIp", getLocalIpAddress())
            put("time", System.currentTimeMillis())
        }
        publishToTopic(joinReq.toString())
    }
private fun connectWebSocketTopic(topic: String, onOpenCallback: () -> Unit) {
        val room = currentRoomCode.ifEmpty { topic }
val wsUrl = "wss://ooki-game.ahmed1986y5.workers.dev/$room"
    val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Connected to Cloudflare WebSocket: $wsUrl")
                onOpenCallback()
            }
override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingPayload(text)
            }
override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Cloudflare WebSocket failure: ${t.message}")
            }
override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Cloudflare WebSocket closed: $reason")
            }
        })
    }
private fun publishToTopic(message: String) {
        if (currentRoomCode.isEmpty()) return
        // 1. Send via WebSocket if open (Instant Real-time)
        val sentViaWs = webSocket?.send(message) == true
        // 3. For signaling control messages (or fallback // (or fallback if WS closed), send via HTTP POST
        if (!message.startsWith("F:") || !sentViaWs) {
            val url = "https://ooki-game.ahmed1986y5.workers.dev/$currentRoomCode"
    val requestBody = message.toRequestBody("text/plain".toMediaType())
val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    Log.e(TAG, "Failed to publish via HTTP: ${e.message}")
                }
override fun onResponse(call: Call, response: Response) {
                    response.close()
                }
            })
        }
    }
private fun handleIncomingPayload(payload: String) {
        if (!isRunning.get() || payload.isBlank()) return
        try {
            // Frame Input format: F:FRAME:MASK or F:ROOM:FRAME:MASK
if (payload.startsWith("F:")) {
                val parts = payload.split(":")
                if (parts.size == 3) {
                    val frameIdx = parts[1].toLongOrNull() ?: 0L
                    val mask = parts[2].toIntOrNull() ?: 0
                    onInputReceived?.invoke(frameIdx, mask)
                } else if (parts.size >= 4) {
                    val room = parts[1].uppercase()
                    if (room == currentRoomCode) {
                        val frameIdx = parts[2].toLongOrNull() ?: 0L
                        val mask = parts[3].toIntOrNull() ?: 0
                        onInputReceived?.invoke(frameIdx, mask)
                    }
                }
                return
            }
val json = JSONObject(payload)
val msgRoom = json.optString("room", "").uppercase()
            if (msgRoom != currentRoomCode) return
            when (json.optString("type")) {
                "JOIN_REQUEST" -> {
                    if (isHost) {
                        val reqPassword = json.optString("password", "")
val guestName = json.optString("guestName", "لاعب منافس")
                        if (currentPassword.isNotEmpty() && reqPassword != currentPassword) {
                            val rej = JSONObject().apply {
                                put("type", "JOIN_REJECT")
                                put("room", currentRoomCode)
                                put("reason", "كلمة المرور غير صحيحة")
                            }
                            publishToTopic(rej.toString())
                            return
                        }
                        isPeerConnected = true
                        val acceptMsg = JSONObject().apply {
                            put("type", "JOIN_ACCEPTED")
                            put("room", currentRoomCode)
                            put("hostName", playerName)
                            put("hostIp", getLocalIpAddress())
                            put("hostPort", DEFAULT_PORT)
                            put("gameTitle", hostGameTitle)
                        }
                        publishToTopic(acceptMsg.toString())
                        mainHandler.post {
                            onPeerJoined?.invoke(guestName, false)
                            onConnectionState?.invoke(true, 24L)
                        }
                    }
                }
                "JOIN_ACCEPTED" -> {
                    if (!isHost && !isPeerConnected) {
                        isPeerConnected = true
                        timeoutJob?.cancel()
val hostName = json.optString("hostName", "المضيف")
val hostIp = json.optString("hostIp", "")
val remoteGame = json.optString("gameTitle", "")
                        if (remoteGame.isNotEmpty()) {
                            mainHandler.post {
                                onRemoteGameInfoReceived?.invoke(remoteGame)
                            }
                        }
                        if (hostIp.isNotEmpty() && hostIp != "127.0.0.1") {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val socket = Socket()
                                    socket.tcpNoDelay = true
                                    socket.connect(InetSocketAddress(hostIp, DEFAULT_PORT), 1500)
val writer = PrintWriter(socket.getOutputStream(), true)
val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                                    directSocket = socket
                                    directWriter = writer
                                    directReader = reader
                                    listenToDirectSocket(reader)
                                } catch (e: Exception) {
                                    // Continues smoothly over global relay
}
                            }
                        }
                        mainHandler.post {
                            pendingJoinCallback?.invoke(true, "✅ تم الاتصال بنجاح مع $hostName عبر الإنترنت!")
                            pendingJoinCallback = null
                            onPeerJoined?.invoke(hostName, true)
                            onConnectionState?.invoke(true, 28L)
                        }
                        startHeartbeat()
                    }
                }
                "JOIN_REJECT" -> {
                    if (!isHost) {
                        timeoutJob?.cancel()
val reason = json.optString("reason", "تم رفض الاتصال")
                        mainHandler.post {
                            pendingJoinCallback?.invoke(false, "❌ $reason")
                            pendingJoinCallback = null
                        }
                    }
                }
                "CHAT" -> {
                    val sender = json.optString("sender", "لاعب")
val isHostMsg = json.optBoolean("isHost", false)
val chatText = json.optString("text", "")
                    mainHandler.post {
                        onChatMessageReceived?.invoke(sender, isHostMsg, chatText)
                    }
                }
                "PING" -> {
                    val timestamp = json.optLong("time", 0L)
val pong = JSONObject().apply {
                        put("type", "PONG")
                        put("room", currentRoomCode)
                        put("time", timestamp)
                    }
                    publishToTopic(pong.toString())
                }
                "PONG" -> {
                    val sentTime = json.optLong("time", 0L)
                    if (sentTime > 0) {
                        val ping = (System.currentTimeMillis() - sentTime).coerceAtLeast(15L)
                        mainHandler.post {
                            onConnectionState?.invoke(true, ping)
                        }
                    }
                }
                "GAME_INFO" -> {
                    if (!isHost) {
                        val title = json.optString("gameTitle", "")
                        if (title.isNotEmpty()) {
                            mainHandler.post {
                                onRemoteGameInfoReceived?.invoke(title)
                            }
                        }
                    }
                }
                "SYNC_STATE_CHUNK" -> {
                    val transferId = json.optString("transferId", "")
val chunkIndex = json.optInt("chunkIndex", 0)
val totalChunks = json.optInt("totalChunks", 1)
val data = json.optString("data", "")
                    if (transferId.isNotEmpty()) {
                        val buffer = chunkBuffers.computeIfAbsent(transferId) { arrayOfNulls(totalChunks) }
                        if (chunkIndex in buffer.indices) {
                            buffer[chunkIndex] = data
                        }
                    }
                }
                "SYNC_STATE_APPLY" -> {
                    val transferId = json.optString("transferId", "")
val hostFrame = json.optLong("hostFrame", 0L)
val hasBinaryState = json.optBoolean("hasBinaryState", false)
var stateBytes: ByteArray? = null
                    if (hasBinaryState && transferId.isNotEmpty()) {
                        val buffer = chunkBuffers.remove(transferId)
                        if (buffer != null && buffer.all { it != null }) {
                            try {
                                val fullB64 = buffer.filterNotNull().joinToString("")
val compressed = Base64.decode(fullB64, Base64.NO_WRAP)
                                stateBytes = decompressGzip(compressed)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to decompress state snapshot", e)
                            }
                        }
                    }
                    mainHandler.post {
                        onStateSyncReceived?.invoke(hostFrame, stateBytes)
                    }
                }
                "SYNC_GAME_START" -> {
                    val startFrame = json.optLong("startFrame", 0L)
                    mainHandler.post {
                        onSyncGameStartReceived?.invoke(startFrame)
                    }
                }
                "SYNC_STATE_REQ" -> {
                    if (isHost) {
                        mainHandler.post {
                            onStateSyncRequested?.invoke()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // payload parse error
}
    }
private fun handleIncomingDirectClient(socket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket.tcpNoDelay = true
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
val writer = PrintWriter(socket.getOutputStream(), true)
val line = reader.readLine() ?: return@launch
val json = JSONObject(line)
val reqCode = json.optString("roomCode", "").trim().uppercase()
val reqPass = json.optString("password", "").trim()
val peerName = json.optString("playerName", "Guest")
                if (reqCode != currentRoomCode) {
                    val rej = JSONObject().apply {
                        put("type", "JOIN_REJECT")
                        put("reason", "كود الغرفة غير مطابق")
                    }
                    writer.println(rej.toString())
                    socket.close()
                    return@launch
                }
                if (currentPassword.isNotEmpty() && reqPass != currentPassword) {
                    val rej = JSONObject().apply {
                        put("type", "JOIN_REJECT")
                        put("reason", "كلمة المرور غير صحيحة")
                    }
                    writer.println(rej.toString())
                    socket.close()
                    return@launch
                }
                directSocket = socket
                directWriter = writer
                directReader = reader
                isPeerConnected = true
                val ack = JSONObject().apply {
                    put("type", "JOIN_ACCEPT")
                    put("roomCode", currentRoomCode)
                    put("isHost", false)
                    put("hostName", playerName)
                }
                writer.println(ack.toString())
                mainHandler.post {
                    onPeerJoined?.invoke(peerName, false)
                    onConnectionState?.invoke(true, 12L)
                }
                listenToDirectSocket(reader)
            } catch (e: Exception) {
                // direct socket error
}
        }
    }
private fun listenToDirectSocket(reader: BufferedReader) {
        try {
            while (isRunning.get()) {
                val line = reader.readLine() ?: break
                if (line.startsWith("F:")) {
                    val parts = line.split(":")
                    if (parts.size >= 3) {
                        val fIndex = parts[1].toLongOrNull() ?: 0L
                        val mask = parts[2].toIntOrNull() ?: 0
                        onInputReceived?.invoke(fIndex, mask)
                    }
                } else if (line.startsWith("{")) {
                    handleIncomingPayload(line)
                }
            }
        } catch (e: Exception) {
            // direct socket closed
}
    }
private fun findHostIpFast(code: String): String? {
        val myIp = getLocalIpAddress()
        if (myIp == "127.0.0.1") return null
        val prefix = myIp.substringBeforeLast(".")
val testIps = listOf("$prefix.1", "$prefix.2", "$prefix.3", "$prefix.4", "$prefix.100")
        for (ip in testIps) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(ip, DEFAULT_PORT), 150)
                s.close()
                return ip
            } catch (e: Exception) {
                // next IP
}
        }
        return null
    }
private fun startLocalBroadcast(code: String, name: String, ip: String, game: String) {
        broadcastJob?.cancel()
        broadcastJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = DatagramSocket().apply { broadcast = true }
val msg = JSONObject().apply {
                    put("code", code)
                    put("name", name)
                    put("ip", ip)
                    put("game", game)
                }.toString().toByteArray()
val packet = DatagramPacket(
                    msg,
                    msg.size,
                    InetAddress.getByName("255.255.255.255"),
                    BROADCAST_PORT
                )
                while (isRunning.get()) {
                    try {
                        socket.send(packet)
                    } catch (e: Exception) {
                        // ignore
}
                    delay(2000)
                }
                socket.close()
            } catch (e: Exception) {
                // ignore
}
        }
    }
private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isRunning.get()) {
                try {
                    val pingJson = JSONObject().apply {
                        put("type", "PING")
                        put("room", currentRoomCode)
                        put("time", System.currentTimeMillis())
                    }
                    publishToTopic(pingJson.toString())
                } catch (e: Exception) {
                    // ignore
}
                delay(3000)
            }
        }
    }
fun sendChatMessage(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CHAT")
                    put("room", currentRoomCode)
                    put("sender", playerName)
                    put("isHost", isHost)
                    put("text", text)
                }
                publishToTopic(json.toString())
                directWriter?.println(json.toString())
            } catch (e: Exception) {
                // ignore
}
        }
    }
override fun sendFrameInput(frameIndex: Long, inputMask: Int) {
        if (currentRoomCode.isNotEmpty() && isRunning.get()) {
            directWriter?.println("F:$frameIndex:$inputMask")
            publishToTopic("F:$currentRoomCode:$frameIndex:$inputMask")
        }
    }
fun sendGameInfo(title: String) {
        hostGameTitle = title.ifBlank { "Combat 3 (المدمجة)" }
        if (currentRoomCode.isNotEmpty() && isRunning.get()) {
            val json = JSONObject().apply {
                put("type", "GAME_INFO")
                put("room", currentRoomCode)
                put("gameTitle", hostGameTitle)
            }
            publishToTopic(json.toString())
            directWriter?.println(json.toString())
        }
    }
override fun setOnInputReceivedListener(listener: (frameIndex: Long, inputMask: Int) -> Unit) {
        onInputReceived = listener
    }
override fun setOnConnectionStateListener(listener: (connected: Boolean, pingMs: Long) -> Unit) {
        onConnectionState = listener
    }
override fun sendStateSnapshot(hostFrame: Long, stateData: ByteArray?) {
        if (currentRoomCode.isEmpty() || !isRunning.get()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val compressed = if (stateData != null && stateData.isNotEmpty()) {
                    compressGzip(stateData)
                } else {
                    ByteArray(0)
                }
val b64 = if (compressed.isNotEmpty()) {
                    Base64.encodeToString(compressed, Base64.NO_WRAP)
                } else {
                    ""
                }
val transferId = System.currentTimeMillis().toString()
                if (b64.isEmpty()) {
                    val applyJson = JSONObject().apply {
                        put("type", "SYNC_STATE_APPLY")
                        put("room", currentRoomCode)
                        put("transferId", transferId)
                        put("hostFrame", hostFrame)
                        put("totalChunks", 0)
                        put("hasBinaryState", false)
                    }
                    publishToTopic(applyJson.toString())
                    directWriter?.println(applyJson.toString())
                    return@launch
                }
val chunkSize = 16 * 1024
                val totalChunks = (b64.length + chunkSize - 1) / chunkSize
                for (i in 0 until totalChunks) {
                    val start = i * chunkSize
                    val end = minOf(start + chunkSize, b64.length)
val chunkStr = b64.substring(start, end)
val chunkJson = JSONObject().apply {
                        put("type", "SYNC_STATE_CHUNK")
                        put("room", currentRoomCode)
                        put("transferId", transferId)
                        put("chunkIndex", i)
                        put("totalChunks", totalChunks)
                        put("data", chunkStr)
                    }
                    publishToTopic(chunkJson.toString())
                    directWriter?.println(chunkJson.toString())
                    if (i % 4 == 0) delay(8)
                }
val finalApply = JSONObject().apply {
                    put("type", "SYNC_STATE_APPLY")
                    put("room", currentRoomCode)
                    put("transferId", transferId)
                    put("hostFrame", hostFrame)
                    put("totalChunks", totalChunks)
                    put("hasBinaryState", true)
                }
                publishToTopic(finalApply.toString())
                directWriter?.println(finalApply.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending state snapshot", e)
            }
        }
    }
override fun setOnStateSyncReceivedListener(listener: (hostFrame: Long, stateBytstateBytes: ByteArray?) -> Unit) {
        onStateSyncReceived = listener
    }
override fun requestStateSync() {
        if (currentRoomCode.isNotEmpty() && isRunning.get()) {
            val json = JSONObject().apply {
                put("type", "SYNC_STATE_REQ")
                put("room", currentRoomCode)
                put("requester", playerName)
                put("time", System.currentTimeMillis())
            }
            publishToTopic(json.toString())
            directWriter?.println(json.toString())
        }
    }
override fun setOnStateSyncRequestedListener(listener: () -> Unit) {
        onStateSyncRequested = listener
    }
override fun sendGameStartSync(startFrame: Long) {
        if (currentRoomCode.isNotEmpty() && isRunning.get()) {
            val json = JSONObject().apply {
                put("type", "SYNC_GAME_START")
                put("room", currentRoomCode)
                put("startFrame", startFrame)
                put("time", System.currentTimeMillis())
            }
            publishToTopic(json.toString())
            directWriter?.println(json.toString())
        }
    }
override fun setOnGameStartSyncListener(listener: (startFrame: Long) -> Unit) {
        onSyncGameStartReceived = listener
    }
private fun compressGzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream(data.size)
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }
private fun decompressGzip(compressed: ByteArray): ByteArray {
        val bis = ByteArrayInputStream(compressed)
val bos = ByteArrayOutputStream()
        GZIPInputStream(bis).use { it.copyTo(bos) }
        return bos.toByteArray()
    }
fun stop() {
        isRunning.set(false)
        isPeerConnected = false
        serverJob?.cancel()
        clientJob?.cancel()
        broadcastJob?.cancel()
        pollJob?.cancel()
        heartbeatJob?.cancel()
        timeoutJob?.cancel()
        try {
            directWriter?.close()
            directReader?.close()
            directSocket?.close()
            serverSocket?.close()
            webSocket?.close(1000, "Closed")
        } catch (e: Exception) {
            // ignore
}
    }
override fun disconnect() {
        stop()
    }}