package com.ps1.netplay.ui.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ps1.netplay.model.AdminBroadcastItem
import com.ps1.netplay.model.FriendItem
import com.ps1.netplay.network.CloudflareClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class AppNotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val type: String, // "friend_request", "call", "chat", "system"
    val isRead: Boolean = false,
    val senderName: String = "",
    val senderId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(0) } // 0: الكل, 1: الصداقة, 2: المكالمات, 3: النظام
    val filterTabs = listOf("الكل", "طلبات الصداقة", "المكالمات", "النظام")

    var notifications by remember { mutableStateOf<List<AppNotificationItem>>(emptyList()) }

    fun refreshRealNotifications() {
        val list = mutableListOf<AppNotificationItem>()

        // 1. Admin Broadcast Announcements (General Administrative Alerts)
        CloudflareClient.getAdminBroadcasts(context) { broadcasts: List<AdminBroadcastItem> ->
            broadcasts.forEach { b: AdminBroadcastItem ->
                val timeStr = if (b.createdAt > 0) {
                    val diff = System.currentTimeMillis() - b.createdAt
                    when {
                        diff < 60_000 -> "الآن"
                        diff < 3600_000 -> "منذ ${diff / 60_000} دقيقة"
                        diff < 86400_000 -> "منذ ${diff / 3600_000} ساعة"
                        else -> "منذ ${diff / 86400_000} يوم"
                    }
                } else "تنبيه عام"

                list.add(
                    AppNotificationItem(
                        id = "admin_${b.id}",
                        title = b.title,
                        description = b.content,
                        time = "تنبيه إداري • $timeStr",
                        type = "system",
                        isRead = false,
                        senderName = b.author
                    )
                )
            }

            // 2. Real incoming friend requests
            CloudflareClient.getIncomingRequests(context) { requests ->
                val incoming = requests ?: CloudflareClient.getLocalIncomingRequests(context)
                incoming.forEach { req ->
                    list.add(
                        AppNotificationItem(
                            id = "req_${req.id}",
                            title = "طلب صداقة جديد",
                            description = "أرسل لك ${req.fromUsername} طلب صداقة جديد",
                            time = "طلب معلّق",
                            type = "friend_request",
                            isRead = false,
                            senderName = req.fromUsername,
                            senderId = req.fromUserId
                        )
                    )
                }

                // 3. Real missed calls
                val missedCalls = CallHistoryManager.getHistory(context).filter { it.status == CallStatus.MISSED }
                missedCalls.forEach { call ->
                    list.add(
                        AppNotificationItem(
                            id = "call_${call.id}",
                            title = "مكالمة فائتة",
                            description = "مكالمة ${if (call.isVideo) "فيديو" else "صوتية"} فائتة من ${call.name}",
                            time = call.time.ifEmpty { "مؤخراً" },
                            type = "call",
                            isRead = false,
                            senderName = call.name
                        )
                    )
                }

                notifications = list
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshRealNotifications()
        while (isActive) {
            delay(5000)
            refreshRealNotifications()
        }
    }

    val filteredNotifications = notifications.filter {
        when (selectedFilter) {
            1 -> it.type == "friend_request"
            2 -> it.type == "call"
            3 -> it.type == "system"
            else -> true
        }
    }

    Scaffold(
        containerColor = getAppScreenBackground(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                // Unified Top Bar: Title "الإشعارات" on Right (Start in RTL), Actions on Left
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "رجوع",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "الإشعارات",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                notifications = notifications.map { it.copy(isRead = true) }
                                Toast.makeText(context, "تم تعيين الإشعارات كمقروءة", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "قراءة الكل",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                notifications = emptyList()
                                Toast.makeText(context, "تم مسح قائمة الإشعارات", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح الكل",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterTabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedFilter == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9))
                                .clickable { selectedFilter = index }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = TajawalFontFamily,
                                color = if (isSelected) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    ) { paddingValues ->
        if (filteredNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "لا توجد إشعارات جديدة حالياً",
                        fontFamily = TajawalFontFamily,
                        fontSize = 15.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNotifications, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onAcceptFriend = {
                            if (item.senderName.isNotEmpty()) {
                                CloudflareClient.acceptFriendRequest(context, item.senderName) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    // Add to local friends
                                    val newFriend = FriendItem(
                                        id = item.senderId.ifEmpty { "u_${System.currentTimeMillis()}" },
                                        username = item.senderName,
                                        avatarUrl = "",
                                        status = "online",
                                        createdAt = System.currentTimeMillis()
                                    )
                                    CloudflareClient.addLocalFriend(context, newFriend)
                                    refreshRealNotifications()
                                }
                            }
                        },
                        onDeclineFriend = {
                            if (item.senderName.isNotEmpty()) {
                                CloudflareClient.rejectFriendRequest(context, item.senderName) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    refreshRealNotifications()
                                }
                            }
                        },
                        onClick = {
                            notifications = notifications.map { if (it.id == item.id) it.copy(isRead = true) else it }
                            if (item.type == "friend_request") {
                                navController.navigate("friend_requests")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: AppNotificationItem,
    onAcceptFriend: () -> Unit,
    onDeclineFriend: () -> Unit,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (item.type) {
        "friend_request" -> Icons.Default.PersonAdd
        "call" -> Icons.Default.PhoneMissed
        "chat" -> Icons.Default.ChatBubbleOutline
        else -> Icons.Default.Shield
    }

    val iconBgColor: Color = when (item.type) {
        "friend_request" -> Color(0xFF2563EB)
        "call" -> Color(0xFFEF4444)
        "chat" -> Color(0xFF10B981)
        else -> Color(0xFF7C3AED)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) Color.White else Color(0xFFF0F7FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBgColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconBgColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = item.time,
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.description,
                        fontFamily = TajawalFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    if (item.type == "friend_request") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onAcceptFriend,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("قبول", fontFamily = TajawalFontFamily, fontSize = 12.sp, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = onDeclineFriend,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("تراجع", fontFamily = TajawalFontFamily, fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        }
    }
}
