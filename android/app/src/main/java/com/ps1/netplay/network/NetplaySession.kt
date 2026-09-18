package com.ps1.netplay.network

 
import  android.widget.*
import  kotlin.random.Random

data class RoomMember(
    val id: String,
    val name: String,
    val isHost: Boolean,
    val pingMs: Long = 24L,
    val role: String = if (isHost) "مضيف (Host)" else "لاعب (Player)",
    var isMuted: Boolean = false,
    var isCoHost: Boolean = false,
    var playerNumber: Int = 0, // 0 = لم يتم التعيين، 1 = اللاعب 1، 2 = اللاعب 2
    val avatarColor: String = if (isHost) "#3B82F6" else "#10B981")
data class ChatMessage(
    val id: String,
    val senderName: String,
    val isHost: Boolean,
    val text: String,
    val timestamp: String)
data class RoomInfo(
    val roomCode: String,
    val roomName: String = "غرفة قتال PS1",
    val password: String = "",
    val isPasswordProtected: Boolean = false,
    val isHost: Boolean = true,
    var isGameStarted: Boolean = false, // حالة بدء اللعبة
    val peerName: String = "Player 1 (Host)",
    val pingMs: Long = 18L,
    val packetLossPercent: Float = 0.0f,
    val gameTitle: String = "Combat 3 Arena",
    val members: MutableList<RoomMember> = mutableListOf(),
    val messages: MutableList<ChatMessage> = mutableListOf())
class NetplaySession {
    var currentRoom: RoomInfo? = null
        private set
    var myPlayerName: String = "لاعب أندرويد"
    private var transport: PeerTransport? = null
    /**
     * Standard local/mock create room for backward compatibility
     */
    fun createRoom(
        roomName: String = "غرفة Al-Mahalla",
        password: String = "",
        gameTitle: String = "Combat 3 Arena"
    ): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
val initialMembers = mutableListOf(
            RoomMember(
                id = "host-1",
                name = myPlayerName,
                isHost = true,
                pingMs = 20L,
                role = "المضيف (Host)"
            )
        )
val welcomeMessages = mutableListOf(
            ChatMessage(
                id = "msg-1",
                senderName = "النظام",
                isHost = true,
                text = "تم إنشاء الغرفة بنجاح! كود الغرفة: $code",
                timestamp = "الآن"
            )
        )
        currentRoom = RoomInfo(
            roomCode = code,
            roomName = roomName.ifBlank { "غرفة PS1 #$code" },
            password = password,
            isPasswordProtected = password.isNotBlank(),
            isHost = true,
            peerName = myPlayerName,
            gameTitle = gameTitle,
            members = initialMembers,
            messages = welcomeMessages
        )
        return code
    }
    /**
     * Standard local/mock join room for backward compatibility
     */
    fun joinRoom(
        roomCodeOrName: String,
        enteredPassword: String = ""
    ): Boolean {
        val code = roomCodeOrName.trim().uppercase()
val membersList = mutableListOf(
            RoomMember(
                id = "guest-me",
                name = myPlayerName,
                isHost = false,
                pingMs = 28L,
                role = "لاعب (Guest)"
            )
        )
val welcomeMessages = mutableListOf(
            ChatMessage(
                id = "msg-1",
                senderName = "النظام",
                isHost = true,
                text = "تم الانضمام للغرفة $code بنجاح.",
                timestamp = "الآن"
            )
        )
        currentRoom = RoomInfo(
            roomCode = code,
            roomName = "غرفة $code",
            password = enteredPassword,
            isPasswordProtected = enteredPassword.isNotBlank(),
            isHost = false,
            peerName = myPlayerName,
            members = membersList,
            messages = welcomeMessages
        )
        return true
    }
    /**
     * Creates a room with custom name, optional password, and game title over the global Internet
     */
    fun createGlobalRoomWithTransport(
        transportEngine: GlobalInternetNetplayEngine,
        roomName: String = "غرفة Al-Mahalla",
        password: String = "",
        gameTitle: String = "Combat 3 Arena",
        onReady: (Boolean, String) -> Unit
    ): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
val initialMembers = mutableListOf(
            RoomMember(
                id = "host-1",
                name = myPlayerName,
                isHost = true,
                pingMs = 20L,
                role = "المضيف (Host)"
            )
        )
val welcomeMessages = mutableListOf(
            ChatMessage(
                id = "msg-1",
                senderName = "النظام",
                isHost = true,
                text = "تم إنشاء الغرفة العالمية بنجاح! كود الغرفة: $code",
                timestamp = "الآن"
            )
        )
        currentRoom = RoomInfo(
            roomCode = code,
            roomName = roomName.ifBlank { "غرفة PS1 #$code" },
            password = password,
            isPasswordProtected = password.isNotBlank(),
            isHost = true,
            peerName = myPlayerName,
            gameTitle = gameTitle,
            members = initialMembers,
            messages = welcomeMessages
        )
        setTransport(transportEngine)
        transportEngine.startGlobalHost(code, roomName, password, gameTitle) { success, msg ->
            onReady(success, msg)
        }
        return code
    }
fun joinGlobalRoomWithTransport(
        transportEngine: GlobalInternetNetplayEngine,
        roomCode: String,
        enteredPassword: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        val code = roomCode.trim().uppercase()
val membersList = mutableListOf(
            RoomMember(
                id = "guest-me",
                name = myPlayerName,
                isHost = false,
                pingMs = 28L,
                role = "لاعب (Guest)"
            )
        )
val welcomeMessages = mutableListOf(
            ChatMessage(
                id = "msg-1",
                senderName = "النظام",
                isHost = true,
                text = "جاري الاتصال بالغرفة العالمية $code والتحقق من المضيف...",
                timestamp = "الآن"
            )
        )
        currentRoom = RoomInfo(
            roomCode = code,
            roomName = "غرفة $code",
            password = enteredPassword,
            isPasswordProtected = enteredPassword.isNotBlank(),
            isHost = false,
            peerName = myPlayerName,
            members = membersList,
            messages = welcomeMessages
        )
        setTransport(transportEngine)
        transportEngine.joinGlobalRoom(code, enteredPassword) { success, msg ->
            if (success) {
                welcomeMessages.add(
                    ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        senderName = "النظام",
                        isHost = false,
                        text = "✅ تم الاتصال بالمضيف وبدء مزامنة GGPO بنجاح عبر الإنترنت!",
                        timestamp = "الآن"
                    )
                )
            } else {
                currentRoom = null
            }
            onResult(success, msg)
        }
    }
var onMessageReceived: (() -> Unit)? = null
    fun addChatMessage(text: String): ChatMessage? {
        val room = currentRoom ?: return null
        val msg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderName = myPlayerName,
            isHost = room.isHost,
            text = text,
            timestamp = "الآن"
        )
        room.messages.add(msg)
        onMessageReceived?.invoke()
        when (val t = transport) {
            is GlobalInternetNetplayEngine -> t.sendChatMessage(text)
            is DirectSocketNetplayEngine -> t.sendChatMessage(text)
        }
        return msg
    }
fun sendChatMessage(text: String): ChatMessage? = addChatMessage(text)
fun leaveRoom() {
        transport?.disconnect()
        currentRoom = null
    }
fun setTransport(peerTransport: PeerTransport) {
        this.transport = peerTransport
        if (peerTransport is GlobalInternetNetplayEngine) {
            peerTransport.onChatMessageReceived = { sender, isHost, text ->
                currentRoom?.messages?.add(
                    ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        senderName = sender,
                        isHost = isHost,
                        text = text,
                        timestamp = "الآن"
                    )
                )
                onMessageReceived?.invoke()
            }
            peerTransport.onPeerJoined = { peerName, isHost ->
                // لا تحذف اللاعب المحلي (نفسه)
currentRoom?.members?.removeAll { it.name == peerName && it.id != "guest-me" && it.id != "host-1" }
                                // تحقق من عدم تكرار إضافة المضيف أو الضيف
    val existing = currentRoom?.members?.find { it.isHost == isHost && it.name == peerName }
                if (existing == null) {
                    currentRoom?.members?.add(
                        RoomMember(
                            id = "peer-${System.currentTimeMillis()}",
                            name = peerName,
                            isHost = isHost,
                            pingMs = 28L,
                            role = if (isHost) "المضيف (Host)" else "لاعب (Guest)"
                        )
                    )
                }
            }
        } else if (peerTransport is DirectSocketNetplayEngine) {
            peerTransport.onChatMessageReceived = { sender, isHost, text ->
                currentRoom?.messages?.add(
                    ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        senderName = sender,
                        isHost = isHost,
                        text = text,
                        timestamp = "الآن"
                    )
                )
                onMessageReceived?.invoke()
            }
            peerTransport.onPeerJoined = { peerName, isHost ->
                // لا تحذف اللاعب المحلي (نفسه)
currentRoom?.members?.removeAll { it.name == peerName && it.id != "guest-me" && it.id != "host-1" }
                                // تحقق من عدم تكرار الإضافة
    val existing = currentRoom?.members?.find { it.isHost == isHost && it.name == peerName }
                if (existing == null) {
                    currentRoom?.members?.add(
                        RoomMember(
                            id = "peer-${System.currentTimeMillis()}",
                            name = peerName,
                            isHost = isHost,
                            pingMs = 15L,
                            role = if (isHost) "المضيف (Host)" else "لاعب (Guest)"
                        )
                    )
                }
            }
        }
    }
fun getTransport(): PeerTransport? = transport}