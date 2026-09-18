package com.ps1.netplay.ui.compose

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ps1.netplay.SettingsActivity

data class ChatConversation(
    val id: String,
    val name: String,
    val lastMsg: String,
    val time: String,
    val avatar: String,
    val isOnline: Boolean = true,
    val unreadCount: Int = 0,
    val isRead: Boolean = true,
    val category: String = "All"
)

@Composable
fun ChatListScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: الكل, 1: المجموعات, 2: القنوات
    val tabs = listOf("الكل", "المجموعات", "القنوات")
    var searchQuery by remember { mutableStateOf("") }
    var conversations by remember { mutableStateOf<List<ChatConversation>>(emptyList()) }
    var showAdminDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val response = com.ps1.netplay.network.ApiService.get(context, "/api/conversations")
            val jsonArray = org.json.JSONObject(response).getJSONArray("conversations")
            val list = mutableListOf<ChatConversation>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val targetId = obj.optString("other_user_id", obj.optString("id"))
                val targetName = obj.optString("other_username", obj.optString("name", "مستخدم"))
                val avatar = obj.optString("other_avatar", obj.optString("avatar_url", ""))
                val lastText = obj.optString("last_message_text", "محادثة جديدة")
                list.add(ChatConversation(targetId, targetName, lastText, "الآن", avatar, unreadCount = 0, isRead = true, category = "All"))
            }
            conversations = list
        } catch(e: Exception) {
            // Keep empty list to show clean empty state
        }
    }

    val filteredConversations = conversations.filter {
        searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.lastMsg.contains(searchQuery, ignoreCase = true)
    }

    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = { showAdminDialog = false },
            confirmButton = {
                Button(
                    onClick = { showAdminDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("إغلاق", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            title = {
                Text("لوحة التحكم", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            },
            text = {
                Column {
                    Text("لوحة إدارة النظام والحسابات نشطة ومهيأة بأعلى معايير الأداء والسرعة.", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // 1. Top Bar: Title "الدردشات" on Right (Start in RTL), Action Icons on Left (End in RTL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right: Title "الدردشات"
            Text(
                text = "الدردشات",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                color = Color(0xFF0F172A)
            )

            // Left Action Icons (Dashboard & Settings)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Control Panel / Dashboard Icon Button
                IconButton(
                    onClick = {
                        showAdminDialog = true
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "لوحة التحكم",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Settings Icon Button
                IconButton(
                    onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 2. Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFEEF4FB))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "ابحث في الدردشات",
                                fontSize = 14.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // 3. Category Tabs (الكل / المجموعات / القنوات - Starts with الكل on Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            tabs.forEachIndexed { index, tabName ->
                val isSelected = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedTab = index
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tabName,
                        fontSize = 15.sp,
                        fontFamily = TajawalFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(32.dp)
                            .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent, CircleShape)
                    )
                }
            }
        }

        // 4. Content (Empty State or Conversations List)
        if (filteredConversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    // Circular Illustration
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(Color(0xFFEBF3FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(8.dp, RoundedCornerShape(22.dp))
                                .background(Color(0xFF2563EB), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "لا توجد محادثات",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ابدأ محادثة جديدة للتواصل مع أصدقائك.",
                        fontSize = 14.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { /* Start new chat */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "محادثة جديدة",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredConversations) { conv ->
                    val encodedName = try { java.net.URLEncoder.encode(conv.name, "UTF-8") } catch (_: Exception) { conv.name }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                            .clickable {
                                navController.navigate("chat_detail/${conv.id}/$encodedName")
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Right Side (Start in RTL): Avatar + Name and Last Message
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFFEEF4FB), CircleShape)
                                    .clickable {
                                        navController.navigate("user_profile/${conv.id}/$encodedName")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conv.name.take(1).uppercase().ifEmpty { "ص" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    fontFamily = TajawalFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = conv.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontFamily = TajawalFontFamily
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = conv.lastMsg,
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    fontFamily = TajawalFontFamily,
                                    maxLines = 1
                                )
                            }
                        }

                        // 2. Left Side (End in RTL): Time
                        Text(
                            text = conv.time,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = TajawalFontFamily
                        )
                    }
                }
            }
        }
    }
}
