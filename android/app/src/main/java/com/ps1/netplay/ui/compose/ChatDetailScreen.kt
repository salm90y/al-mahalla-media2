package com.ps1.netplay.ui.compose
import android.widget.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Intent
import com.ps1.netplay.CallActivity
import com.ps1.netplay.SettingsActivity
import com.ps1.netplay.DashboardActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
data class Photo(val urls: List<String>) : MessageContent()
data class Document(val name: String, val size: String, val type: String) : MessageContent()
data class Location(val address: String, val city: String, val country: String) : MessageContent()
data class Snap(val timer: Int) : MessageContent()}
data class Message(val id: String, val content: MessageContent, val timestamp: String, val isOutgoing: Boolean)
@Composable
fun ChatDetailScreen(
    targetUserId: String = "",
    userName: String = "محادثة",
    isOnline: Boolean = true,
    isTyping: Boolean = false,
    avatarUrl: String = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=100&h=100&fit=crop",
    onNavigateBack: () -> Unit,
    initialMessages: List<Message> = emptyList()) {
    
    val context = LocalContext.current
    var messages by remember { mutableStateOf(initialMessages) }
    
    val listState = rememberLazyListState()
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    var showProfileSheet by remember { mutableStateOf(false) }

    LaunchedEffect(targetUserId) {
        if (targetUserId.isNotEmpty()) {
            try {
                val myId = com.ps1.netplay.UserManager.getCurrentUser(context)?.id ?: ""
                val convId = listOf(myId, targetUserId).sorted().joinToString("_")
                val response = com.ps1.netplay.network.ApiService.get(context, "/api/messages/$convId")
                val jsonObj = org.json.JSONObject(response)
                val msgsArray = jsonObj.getJSONArray("messages")
                val loadedMsgs = mutableListOf<Message>()
                for (i in 0 until msgsArray.length()) {
                    val m = msgsArray.getJSONObject(i)
                    val isOut = m.optString("sender_id") == myId
                    val type = m.optString("type", "text")
                    val textContent = m.optString("content", "")
                    val mediaUrl = m.optString("media_url", "")
                    
                    val content = when (type) {
                        "image" -> MessageContent.Photo(listOf(mediaUrl))
                        else -> MessageContent.Text(textContent)
                    }
                    loadedMsgs.add(Message(m.optString("id"), content, "الآن", isOut))
                }
                if (loadedMsgs.isNotEmpty()) {
                    messages = loadedMsgs
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    if (showProfileSheet) {
        ContactProfileSheet(
            userName = userName,
            avatarUrl = avatarUrl,
            isOnline = isOnline,
            onDismiss = { showProfileSheet = false }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // 1. FIXED SLIM TOP BAR
        ChatTopBar(
            userName = userName,
            isOnline = isOnline,
            avatarUrl = avatarUrl,
            onAvatarClick = { showProfileSheet = true },
            onBack = onNavigateBack
        )
        // 2. DYNAMIC SCROLLABLE MESSAGES
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            items(messages) { message ->
                when (message.content) {
                    is MessageContent.Text -> TextBubble(message.content.text, message.timestamp, message.isOutgoing)
                    is MessageContent.Photo -> {
                        if (message.content.urls.size == 1) {
                            SinglePhotoBubble(message.content.urls.first(), message.timestamp, message.isOutgoing)
                        } else {
                            MultiPhotoBubble(message.content.urls, message.timestamp, message.isOutgoing)
                        }
                    }
                    is MessageContent.Document -> DocumentBubble(message.content, message.timestamp, message.isOutgoing)
                    is MessageContent.Location -> MapCardBubble(message.content, message.timestamp, message.isOutgoing)
                    is MessageContent.Snap -> SnapMediaBubble(message.content, message.timestamp, message.isOutgoing)
                }
            }
        }
        // 3. FIXED INPUT BAR WITH IME PADDING
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            ChatInputBar(
                onSendText = { text ->
                    val newMsg = Message(
                        id = System.currentTimeMillis().toString(),
                        content = MessageContent.Text(text),
                        timestamp = "الآن",
                        isOutgoing = true
                    )
                    messages = messages + newMsg
                    
                    // Send to server
                    if (targetUserId.isNotEmpty()) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                val body = org.json.JSONObject().apply {
                                    put("receiver_id", targetUserId)
                                    put("type", "text")
                                    put("content", text)
                                }
                                com.ps1.netplay.network.ApiService.post(context, "/api/messages/send", body.toString())
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                },
                onSendFile = { uri, mimeType ->
                    var displayName = "file_${System.currentTimeMillis()}"
                    try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                val n = cursor.getString(nameIndex)
                                if (!n.isNullOrBlank()) displayName = n
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }

                    if (!displayName.contains(".")) {
                        val ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        if (!ext.isNullOrBlank()) displayName = "$displayName.$ext"
                    }

                    val msgType = when {
                        mimeType.startsWith("image/") -> "image"
                        mimeType.startsWith("audio/") -> "audio"
                        mimeType.startsWith("video/") -> "video"
                        else -> "file"
                    }

                    val newMsg = Message(
                        id = System.currentTimeMillis().toString(),
                        content = when (msgType) {
                            "image" -> MessageContent.Photo(listOf(uri.toString()))
                            "audio" -> MessageContent.Document(displayName, "ملف صوتي", "audio")
                            "video" -> MessageContent.Document(displayName, "مقطع فيديو", "video")
                            else -> MessageContent.Document(displayName, "مستند", "file")
                        },
                        timestamp = "الآن",
                        isOutgoing = true
                    )
                    messages = messages + newMsg
                    
                    if (targetUserId.isNotEmpty()) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val bytes = inputStream?.readBytes()
                                if (bytes != null) {
                                    com.ps1.netplay.network.CloudflareClient.uploadMediaFile(context, bytes, displayName, mimeType) { success, url ->
                                        if (success && url != null) {
                                            val body = org.json.JSONObject().apply {
                                                put("receiver_id", targetUserId)
                                                put("type", msgType)
                                                put("media_url", url)
                                                put("file_name", displayName)
                                                put("content", displayName)
                                            }
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                try {
                                                    com.ps1.netplay.network.ApiService.post(context, "/api/messages/send", body.toString())
                                                } catch (e: Exception) { e.printStackTrace() }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                },
                onTyping = {}
            )
        }
    }}
@Composable
fun ChatTopBar(
    userName: String = "سامر الأتروني",
    isOnline: Boolean = true,
    isTyping: Boolean = false,
    avatarUrl: String = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=100&h=100&fit=crop",
    onAvatarClick: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showAdminDialog by remember { mutableStateOf(false) }

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
                Text("لوحة التحكم وإدارة الاتصال الفوري نشطة.", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Compact Avatar
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable { onAvatarClick() }
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f), CircleShape),
                contentScale = ContentScale.Crop
            )
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF10B981), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onAvatarClick() }
        ) {
            Text(
                text = userName,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = TajawalFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isTyping) {
                Text(
                    text = "يكتب الآن...",
                    color = Color(0xFF2563EB),
                    fontSize = 11.sp,
                    fontFamily = TajawalFontFamily,
                    maxLines = 1
                )
            } else if (isOnline) {
                Text(
                    text = "متصل",
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontFamily = TajawalFontFamily,
                    maxLines = 1
                )
            }
        }

        // Icon: Control Panel (Dashboard) - Pure icon, no text
        IconButton(
            onClick = { showAdminDialog = true },
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFFEEF4FB), CircleShape)
        ) {
            Icon(
                Icons.Default.Dashboard,
                contentDescription = "لوحة التحكم",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Icon: App Settings - Pure icon, no text
        IconButton(
            onClick = {
                val intent = Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFFEEF4FB), CircleShape)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "إعدادات التطبيق",
                tint = Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun TextBubble(text: String, timestamp: String, isOutgoing: Boolean, modifier: Modifier = Modifier) {
    val dynamicFontSize = com.ps1.netplay.AppSettingsManager.chatFontSizeState.value.sp
    val accentHex = com.ps1.netplay.AppSettingsManager.accentColorState.value
    val showReceipts = com.ps1.netplay.AppSettingsManager.isReadReceiptsState.value
    val outgoingColor = try {
        Color(android.graphics.Color.parseColor(accentHex))
    } catch (e: Exception) {
        Color(0xFF6C5CE7)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOutgoing) 16.dp else 4.dp,
                    bottomEnd = if (isOutgoing) 4.dp else 16.dp
                ))
                .background(if (isOutgoing) outgoingColor else Color(0xFF1B2338))
                .border(
                    width = 1.dp,
                    color = if (isOutgoing) Color(0x33FFFFFF) else Color(0xFF263352),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = if (isOutgoing) Color.White else Color(0xFFF1F2F6),
                fontSize = dynamicFontSize
            )
            Text(
                "$timestamp ${if(isOutgoing) (if (showReceipts) "✓✓" else "✓") else ""}",
                color = if (isOutgoing) Color(0xFFD0D5FF) else Color(0xFF8896A6),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 3.dp)
            )
        }
    }}
@Composable
fun SinglePhotoBubble(url: String, timestamp: String, isOutgoing: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(200.dp)
                .clip(RoundedCornerShape(18.dp))
        ) {
            AsyncImage(
                model = android.net.Uri.parse(url),
                contentDescription = "Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )
            Text(
                "$timestamp ${if(isOutgoing) "✓✓" else ""}",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }}
@Composable
fun MultiPhotoBubble(urls: List<String>, timestamp: String, isOutgoing: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .clip(RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOutgoing) 16.dp else 4.dp,
                    bottomEnd = if (isOutgoing) 4.dp else 16.dp
                ))
                .background(Color.Transparent) // We just show the images without a surrounding box
) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // If 5 images: 1 on top, 4 below (2x2) or (2x3)?
// Let's do 2 top, 3 bottom like a nice grid.
Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(100.dp)) {
                    AsyncImage(
                        model = android.net.Uri.parse(urls.getOrNull(0) ?: ""),
                        contentDescription = "Photo",
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)),
                        contentScale = ContentScale.Crop
                    )
                    AsyncImage(
                        model = android.net.Uri.parse(urls.getOrNull(1) ?: ""),
                        contentDescription = "Photo",
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(100.dp)) {
                    AsyncImage(
                        model = android.net.Uri.parse(urls.getOrNull(2) ?: ""),
                        contentDescription = "Photo",
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = if (isOutgoing) 16.dp else 4.dp, bottomEnd = 0.dp)),
                        contentScale = ContentScale.Crop
                    )
                    AsyncImage(
                        model = android.net.Uri.parse(urls.getOrNull(3) ?: ""),
                        contentDescription = "Photo",
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(0.dp)),
                        contentScale = ContentScale.Crop
                    )
                    AsyncImage(
                        model = android.net.Uri.parse(urls.getOrNull(4) ?: ""),
                        contentDescription = "Photo",
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = if (isOutgoing) 4.dp else 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }}
@Composable
fun DocumentBubble(content: MessageContent.Document, timestamp: String, isOutgoing: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(if (isOutgoing) Color(0xFF282452) else Color(0xFF1B2338), RoundedCornerShape(18.dp))
                .border(1.dp, if (isOutgoing) Color(0xFF4C428C) else Color(0xFF263352), RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF00D2D3).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF00D2D3).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(content.type, color = Color(0xFF00D2D3), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(content.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(content.size, color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            CircularProgressIndicator(
                progress = 0.7f,
                modifier = Modifier.size(24.dp),
                color = Color(0xFF00D2D3),
                strokeWidth = 2.5.dp
            )
        }
    }}
@Composable
fun MapCardBubble(content: MessageContent.Location, timestamp: String, isOutgoing: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOutgoing) 16.dp else 4.dp,
                    bottomEnd = if (isOutgoing) 4.dp else 16.dp
                ))
                .background(Color(0xFF222F3E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=400&h=300&fit=crop",
                    contentDescription = "Map",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(Color(0xFF6C5CE7), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Pin", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(content.city, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(content.country, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                }
                Text(
                    content.address,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3B4859)) // Lighter dark strip
.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Open in Google Maps", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }}
@Composable
fun SnapMediaBubble(content: MessageContent.Snap, timestamp: String, isOutgoing: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .background(Color(0xFF1C2A52).copy(alpha = 0.9f), RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFF3B2B78), RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Snap", tint = Color(0xFFFFA502), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("Tap to view", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${content.timer}s timer", color = Color.LightGray, fontSize = 11.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChatDetailScreen() {
    NetPlayTheme(darkTheme = true) {
        val dummyMessages = listOf(
            Message("1", MessageContent.Text("مرحباً سامر"), "10:40 AM", false),
            Message("2", MessageContent.Text("أهلاً بك! جاهز للعب Combat 3؟"), "10:41 AM", true),
            Message("3", MessageContent.Document("Project_Final_Assets.zip", "120.4 MB", "ZIP"), "10:42 AM", true),
            Message("4", MessageContent.Location("Al-Jaza'ir St, Basra", "Basra", "Iraq"), "10:42 AM", true),
            Message("5", MessageContent.Snap(10), "10:43 AM", true)
        )
        ChatDetailScreen(onNavigateBack = {}, initialMessages = dummyMessages)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileSheet(
    userName: String,
    avatarUrl: String,
    isOnline: Boolean,
    onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
var isMuted by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1424),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Bar inside profile sheet
Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
                Text(
                    text = "الملف الشخصي",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }
            // Big Avatar & Contact Identity
Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar with 3D Glowing Neon Ring
Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(12.dp, CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF00D2D3), Color(0xFF6C5CE7))
                            ),
                            shape = CircleShape
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = userName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color(0xFF00FF88), CircleShape)
                                .border(2.dp, Color(0xFF0F1424), CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Name
Text(
                    text = userName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Handle & Online Status
Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "@${userName.filter { !it.isWhitespace() }.lowercase()}",
                        color = Color(0xFF00D2D3),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isOnline) "متصل الآن" else "آخر ظهور اليوم",
                        color = if (isOnline) Color(0xFF00FF88) else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Quick Actions (Call, Video, Mute, Share)
Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ContactQuickActionButton(
                    icon = Icons.Default.Call,
                    label = "اتصال",
                    color = Color(0xFF00D2D3)
                ) {}
                ContactQuickActionButton(
                    icon = Icons.Default.Videocam,
                    label = "فيديو",
                    color = Color(0xFF6C5CE7)
                ) {}
                ContactQuickActionButton(
                    icon = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    label = if (isMuted) "مكتوم" else "كتم",
                    color = if (isMuted) Color(0xFFFFA502) else Color(0xFF70A1FF)
                ) {
                    isMuted = !isMuted
                }
                ContactQuickActionButton(
                    icon = Icons.Default.Share,
                    label = "مشاركة",
                    color = Color(0xFF2ED573)
                ) {}
            }
            Spacer(modifier = Modifier.height(20.dp))
            // Details Section Cards
Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Card
ContactInfoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ContactInfoRow(
                            icon = Icons.Default.Phone,
                            title = "رقم الهاتف / المعرف",
                            subtitle = "+964 770 123 4567"
                        )
                        HorizontalDivider(color = Color(0xFF252D4D), thickness = 0.8.dp)
                        ContactInfoRow(
                            icon = Icons.Default.Info,
                            title = "السيرة الذاتية (Bio)",
                            subtitle = "لاعب ومشارك في شبكة المحلة 🎮 | متاح للدردشة والمباريات 🏆"
                        )
                    }
                }
                // Shared Media Preview Card
ContactInfoCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الوسائط المشتركة",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "18 ملف",
                                color = Color(0xFF00D2D3),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val samplePhotos = listOf(
                                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop",
                                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop",
                                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop",
                                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop"
                            )
                            samplePhotos.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFF252D4D), RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                // Settings & Privacy
ContactInfoCard {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF00D2D3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("كتم الإشعارات", color = Color.White, fontSize = 14.sp)
                            }
                            Switch(
                                checked = isMuted,
                                onCheckedChange = { isMuted = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF6C5CE7),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF1E2842)
                                )
                            )
                        }
                        HorizontalDivider(color = Color(0xFF252D4D), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF70A1FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("البحث في المحادثة", color = Color.White, fontSize = 14.sp)
                        }
                        HorizontalDivider(color = Color(0xFF252D4D), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = Color(0xFFFF4757),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("حظر المستخدم", color = Color(0xFFFF4757), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(color = Color(0xFF252D4D), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFF4757),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("حذف سجل المحادثة", color = Color(0xFFFF4757), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactQuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF161C33), CircleShape)
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ContactInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Color(0xFF161C33), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF252D4D), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
fun ContactInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF0F1424), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF252D4D), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00D2D3),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}