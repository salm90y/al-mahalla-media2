package com.ps1.netplay.model
import android.widget.*

data class FriendItem(
    val id: String,
    val friendshipId: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val status: String = "offline", // online, offline, in_game
    val lastSeen: Long = 0L,
    val isBlocked: Boolean = false,
    val blockedBy: String = "",
    val muteUntil: Long = 0L,
    val muteType: String = "none",
    val createdAt: Long = 0L
) {
    val name: String get() = username
}

data class FriendRequestItem(
    val id: String,
    val fromUserId: String = "",
    val toUserId: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L
) {
    val name: String get() = username
    val fromUsername: String get() = username
}

typealias RequestItem = FriendRequestItem

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val type: String = "text", // text, image, video, file, location, voice, call_log
    val content: String = "",
    val mediaUrl: String = "",
    val duration: Int = 0, // seconds
    val fileName: String = "",
    val fileSize: Long = 0L,
    val locationLat: Double = 0.0,
    val locationLng: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    var msgStatus: String = "sent",
    val isRead: Boolean = false,
    val isDelivered: Boolean = true,
    val isMe: Boolean = false
)

data class ConversationItem(
    val id: String,
    val otherUserId: String,
    val otherUsername: String,
    val otherAvatar: String,
    val otherStatus: String,
    val lastMessageText: String,
    val lastMessageAt: Long,
    val unreadCount: Int = 0
)

data class AdminBroadcastItem(
    val id: String,
    val title: String,
    val content: String,
    val author: String = "الإدارة",
    val priority: String = "high", // high, normal, urgent
    val createdAt: Long = System.currentTimeMillis()
)
