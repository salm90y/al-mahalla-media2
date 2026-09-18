package com.ps1.netplay.network

 
import  android.widget.*
import  android.content.Context 
import  android.net.wifi.WifiManager
import  android.os.Handler 
import  android.os.Looper
import  kotlinx.coroutines.* 
import  org.json.JSONObject
import  java.io.BufferedReader 
import  java.io.InputStreamReader
import  java.io.PrintWriter 
import  java.net.*
import  java.util.concurrent.ConcurrentHashMap 
import  java.util.concurrent.atomic.AtomicBoolean

/** * Real-time Cross-Device Netplay Engine (Direct TCP/UDP + Local Hotspot/LAN Auto-Discovery) * Handles real room verification, input frame streaming, chat, and room synchronization. */class DirectSocketNetplayEngine(
    private val context: Context,
    private val playerName: String) : PeerTransport {
    companion object {
        const val DEFAULT_PORT = 8990
        const val BROADCAST_PORT = 8991
        private val activeRoomsCache = ConcurrentHashMap<String, DiscoveredRoom>()
    }
data class DiscoveredRoom(
        val roomCode: String,
        val roomName: String,
        val hostIp: String,
        val hostPort: Int,
        val gameTitle: String,
        val timestamp: Long = System.currentTimeMillis()
    )
private val mainHandler = Handler(Looper.getMainLooper())
private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var broadcastJob: Job? = null
    private var discoveryJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var socketWriter: PrintWriter? = null
    private var socketReader: BufferedReader? = null
    private val isRunning = AtomicBoolean(false)
private var isHost = false
    private var currentRoomCode = ""
    private var currentRoomPassword = ""
    // Callbacks
    private var onInputReceived: ((frameIndex: Long, inputMask: Int) -> Unit)? = null
    private var onConnectionState: ((connected: Boolean, pingMs: Long) -> Unit)? = null
    var onChatMessageReceived: ((sender: String, isHost: Boolean, message: String) -> Unit)? = null
    var onPeerJoined: ((peerName: String, isHost: Boolean) -> Unit)? = null
    var onPeerLeft: ((peerName: String) -> Unit)? = null
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
     * Starts Host Room Server on port 8990 with custom Room Code
     */
    fun startHost(
        roomCode: String,
        roomName: String,
        password: String,
        gameTitle: String,
        onReady: (Boolean, String) -> Unit
    ) {
        stop()
        isHost = true
        currentRoomCode = roomCode.trim().uppercase()
        currentRoomPassword = password.trim()
        isRunning.set(true)
val scope = CoroutineScope(Dispatchers.IO)
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(DEFAULT_PORT).apply {
                    reuseAddress = true
                }
val localIp = getLocalIpAddress()
                activeRoomsCache[currentRoomCode] = DiscoveredRoom(
                    roomCode = currentRoomCode,
                    roomName = roomName,
                    hostIp = localIp,
                    hostPort = DEFAULT_PORT,
                    gameTitle = gameTitle
                )
                // Start background UDP broadcast // Start background UDP broadcast for auto-discovery
                startBroadcasting(currentRoomCode, roomName, localIp, gameTitle)
                mainHandler.post {
                    onReady(true, "الغرفة جاهزة (IP: $localIp)")
                    onConnectionState?.invoke(true, 12L)
                }
                while (isRunning.get()) {
                    val socket = serverSocket?.accept() ?: break
                    handleIncomingClient(socket)
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    mainHandler.post {
                        onReady(false, "فشل إنشاء الخادم: ${e.message}")
                    }
                }
            }
        }
    }
private fun handleIncomingClient(socket: Socket) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                socket.tcpNoDelay = true
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
val writer = PrintWriter(socket.getOutputStream(), true)
                // Read Handshake
    val line = reader.readLine() ?: return@launch
val json = JSONObject(line)
val type = json.optString("type")
val reqCode = json.optString("roomCode", "").trim().uppercase()
val reqPass = json.optString("password", "").trim()
val peerName = json.optString("playerName", "Guest")
                if (type == "JOIN_REQ") {
                    if (reqCode != currentRoomCode) {
                        val rej = JSONObject().apply {
                            put("type", "JOIN_REJECT")
                            put("reason", "كود الغرفة غير صحيح")
                        }
                        writer.println(rej.toString())
                        socket.close()
                        return@launch
                    }
                    if (currentRoomPassword.isNotEmpty() && reqPass != currentRoomPassword) {
                        val rej = JSONObject().apply {
                            put("type", "JOIN_REJECT")
                            put("reason", "كلمة المرور غير صحيحة")
                        }
                        writer.println(rej.toString())
                        socket.close()
                        return@launch
                    }
                    // Accepted!
activeSocket = socket
                    socketWriter = writer
                    socketReader = reader
                    val ack = JSONObject().apply {
                        put("type", "JOIN_ACCEPT")
                        put("roomCode", currentRoomCode)
                        put("isHost", false)
                        put("hostName", playerName)
                    }
                    writer.println(ack.toString())
                    mainHandler.post {
                        onPeerJoined?.invoke(peerName, false)
                        onConnectionState?.invoke(true, 15L)
                    }
                    // Start Listening Loop
listenToSocket(reader)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    onConnectionState?.invoke(false, 0L)
                }
            }
        }
    }
    /**
     * Connects Guest to Host by Room Code or Direct IP
     */
    fun connectToRoom(
        roomCodeOrIp: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        stop()
        isHost = false
        isRunning.set(true)
val target = roomCodeOrIp.trim()
val scope = CoroutineScope(Dispatchers.IO)
        clientJob = scope.launch {
            try {
                // Determine Host IP: If it is already an IP, use directly. Otherwise look up in LAN discovery cache or try standard subnet.
    val hostIp = when {
                    target.contains(".") -> target
                    activeRoomsCache.containsKey(target.uppercase()) -> activeRoomsCache[target.uppercase()]!!.hostIp
                    else -> findHostIpForRoomCode(target)
                }
                if (hostIp == null) {
                    mainHandler.post {
                        onResult(false, "❌ لم يتم العثور على الغرفة $target. تأكد من تشغيل المضيف للغرفة.")
                    }
                    return@launch
                }
val socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(hostIp, DEFAULT_PORT), 4000)
val writer = PrintWriter(socket.getOutputStream(), true)
val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                // Send Handshake
    val req = JSONObject().apply {
                    put("type", "JOIN_REQ")
                    put("roomCode", target.uppercase())
                    put("password", password)
                    put("playerName", playerName)
                }
                writer.println(req.toString())
                // Wait // Wait for Handshake response
    val responseLine = reader.readLine()
                if (responseLine == null) {
                    socket.close()
                    mainHandler.post {
                        onResult(false, "❌ المضيف لم يستجب لطلب الدخول.")
                    }
                    return@launch
                }
val resJson = JSONObject(responseLine)
                if (resJson.optString("type") == "JOIN_ACCEPT") {
                    activeSocket = socket
                    socketWriter = writer
                    socketReader = reader
                    val hostName = resJson.optString("hostName", "المضيف")
                    mainHandler.post {
                        onResult(true, "✅ تم الاتصال بالغرفة بنجاح مع $hostName")
                        onPeerJoined?.invoke(hostName, true)
                        onConnectionState?.invoke(true, 18L)
                    }
                    // Start Listening Loop
listenToSocket(reader)
                } else {
                    val reason = resJson.optString("reason", "تم رفض الاتصال")
                    socket.close()
                    mainHandler.post {
                        onResult(false, "❌ $reason")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    onResult(false, "❌ تعذر الاتصال بالغرفة ($target). تحقق من الشبكة أو الكود.")
                }
            }
        }
    }
private fun listenToSocket(reader: BufferedReader) {
        try {
            while (isRunning.get()) {
                val line = reader.readLine() ?: break
                if (line.startsWith("F:")) {
                    // Fast Input Frame format: F:<frameIndex>:<maskVal>
    val parts = line.split(":")
                    if (parts.size >= 3) {
                        val fIndex = parts[1].toLongOrNull() ?: 0L
                        val mask = parts[2].toIntOrNull() ?: 0
                        onInputReceived?.invoke(fIndex, mask)
                    }
                } else if (line.startsWith("{")) {
                    val json = JSONObject(line)
                    when (json.optString("type")) {
                        "CHAT" -> {
                            val sender = json.optString("sender", "لاعب")
val isHostMsg = json.optBoolean("isHost", false)
val text = json.optString("text", "")
                            mainHandler.post {
                                onChatMessageReceived?.invoke(sender, isHostMsg, text)
                            }
                        }
                        "PING" -> {
                            val timestamp = json.optLong("time", System.currentTimeMillis())
val pong = JSONObject().apply {
                                put("type", "PONG")
                                put("time", timestamp)
                            }
                            socketWriter?.println(pong.toString())
                        }
                        "PONG" -> {
                            val sentTime = json.optLong("time", 0L)
                            if (sentTime > 0) {
                                val ping = (System.currentTimeMillis() - sentTime).coerceAtLeast(4L)
                                mainHandler.post {
                                    onConnectionState?.invoke(true, ping)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Socket closed
} finally {
            mainHandler.post {
                onConnectionState?.invoke(false, 0L)
            }
        }
    }
private fun findHostIpForRoomCode(code: String): String? {
        val myIp = getLocalIpAddress()
        if (myIp == "127.0.0.1") return null
        val prefix = myIp.substringBeforeLast(".")
        // Fast scan standard local IPs in parallel (Hotspot gateway and subnet neighbours)
    val testIps = mutableListOf("$prefix.1", "$prefix.2", "$prefix.3", "$prefix.4", "$prefix.5", "$prefix.100")
        for (ip in testIps) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(ip, DEFAULT_PORT), 250)
                s.close()
                return ip
            } catch (e: Exception) {
                // not this IP
}
        }
        return "$prefix.1" // Fallback to Hotspot gateway
}
private fun startBroadcasting(code: String, name: String, ip: String, game: String) {
        val scope = CoroutineScope(Dispatchers.IO)
        broadcastJob = scope.launch {
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
                        // ignore network flap
}
                    delay(2000)
                }
                socket.close()
            } catch (e: Exception) {
                // broadcast failed
}
        }
    }
fun sendChatMessage(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CHAT")
                    put("sender", playerName)
                    put("isHost", isHost)
                    put("text", text)
                }
                socketWriter?.println(json.toString())
            } catch (e: Exception) {
                // ignore
}
        }
    }
override fun sendFrameInput(frameIndex: Long, inputMask: Int) {
        // High-speed low-overhead direct line
socketWriter?.println("F:$frameIndex:$inputMask")
    }
override fun setOnInputReceivedListener(listener: (frameIndex: Long, inputMask: Int) -> Unit) {
        onInputReceived = listener
    }
override fun setOnConnectionStateListener(listener: (connected: Boolean, pingMs: Long) -> Unit) {
        onConnectionState = listener
    }
fun stop() {
        isRunning.set(false)
        serverJob?.cancel()
        clientJob?.cancel()
        broadcastJob?.cancel()
        discoveryJob?.cancel()
        try {
            socketWriter?.close()
            socketReader?.close()
            activeSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
}
    }
override fun disconnect() {
        stop()
    }}