package com.ps1.netplay.ui.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.ps1.netplay.CallActivity
import com.ps1.netplay.UserManager
import com.ps1.netplay.network.CloudflareClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Photo(val urls: List<String>) : MessageContent()
    data class Document(val name: String, val size: String, val type: String) : MessageContent()
    data class Location(val address: String, val city: String, val country: String) : MessageContent()
    data class Snap(val timer: Int) : MessageContent()
}

data class Message(
    val id: String,
    val content: MessageContent,
    val timestamp: String,
    val isOutgoing: Boolean
)

@Composable
fun ChatDetailScreen(
    targetUserId: String = "",
    userName: String = "محادثة",
    isOnline: Boolean = true,
    isTyping: Boolean = false,
    avatarUrl: String = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=100&h=100&fit=crop",
    onNavigateBack: () -> Unit,
    onOpenProfile: (userId: String, userName: String) -> Unit = { _, _ -> },
    initialMessages: List<Message> = emptyList()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val myId = remember { UserManager.getCurrentUser(context)?.id ?: "" }
    val convId = remember(targetUserId) {
        listOf(myId, targetUserId).sorted().joinToString("_")
    }

    // Load initial cached messages immediately without delay
    val initialLocal = remember(convId) {
        val cached = CloudflareClient.getLocalChatMessages(context, convId)
        if (cached.isNotEmpty()) {
            cached.map { m ->
                val content = when (m.type) {
                    "image" -> MessageContent.Photo(listOf(m.mediaUrl.ifEmpty { m.content }))
                    "file", "audio", "video" -> MessageContent.Document(m.fileName.ifEmpty { m.content }, "ملف", m.type)
                    else -> MessageContent.Text(m.content)
                }
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(m.createdAt))
                Message(m.id, content, time, m.isOutgoing)
            }
        } else {
            initialMessages
        }
    }

    var messages by remember { mutableStateOf(initialLocal) }
    val listState = rememberLazyListState()
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    // Story viewing state
    var showStoryDialog by remember { mutableStateOf(false) }
    var activeStoryToView by remember { mutableStateOf<UserStory?>(null) }
    var showNoStoryNotice by remember { mutableStateOf(false) }

    // Live bidirectional sync loop from Cloudflare and R2
    LaunchedEffect(targetUserId) {
        if (targetUserId.isNotEmpty()) {
            while (isActive) {
                CloudflareClient.fetchCloudflareMessages(context, targetUserId) { loaded ->
                    if (loaded.isNotEmpty()) {
                        val mapped = loaded.map { m ->
                            val content = when (m.type) {
                                "image" -> MessageContent.Photo(listOf(m.mediaUrl.ifEmpty { m.content }))
                                "file", "audio", "video" -> MessageContent.Document(m.fileName.ifEmpty { m.content }, "ملف", m.type)
                                else -> MessageContent.Text(m.content)
                            }
                            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(m.createdAt))
                            Message(m.id, content, time, m.isOutgoing)
                        }
                        if (mapped.size != messages.size || mapped != messages) {
                            messages = mapped
                        }
                    }
                }
                delay(2500)
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

    // Story Viewer Dialog
    if (showStoryDialog && activeStoryToView != null) {
        StoryViewerDialog(
            story = activeStoryToView!!,
            onDismiss = {
                showStoryDialog = false
                activeStoryToView = null
            }
        )
    }

    // Notice when contact has no uploaded story
    if (showNoStoryNotice) {
        AlertDialog(
            onDismissRequest = { showNoStoryNotice = false },
            confirmButton = {
                Button(
                    onClick = {
                        showNoStoryNotice = false
                        onOpenProfile(targetUserId, userName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("عرض الحساب", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoStoryNotice = false }) {
                    Text("إغلاق", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            title = {
                Text(
                    text = "الحالة اليومية",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "المستخدم $userName لم يقم بنشر أي حالة جديدة خلال الـ 24 ساعة الماضية.",
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // 1. TOP BAR (Matching App System: Audio Call, Video Call, Name->Profile, Avatar->Story)
        ChatTopBar(
            targetUserId = targetUserId,
            userName = userName,
            isOnline = isOnline,
            isTyping = isTyping,
            avatarUrl = avatarUrl,
            onAvatarClick = {
                val story = StoryManager.getStoryForUser(context, targetUserId, userName)
                if (story != null) {
                    activeStoryToView = story
                    showStoryDialog = true
                } else {
                    showNoStoryNotice = true
                }
            },
            onNameClick = {
                onOpenProfile(targetUserId, userName)
            },
            onBack = onNavigateBack
        )

        // 2. SCROLLABLE MESSAGES
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
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

        // 3. UNIFIED INPUT BAR WITH REAL-TIME CLOUDFLARE/R2 SYNC
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            ChatInputBar(
                onSendText = { text ->
                    val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    val localMsg = Message(
                        id = "msg_${System.currentTimeMillis()}",
                        content = MessageContent.Text(text),
                        timestamp = now,
                        isOutgoing = true
                    )
                    messages = messages + localMsg

                    if (targetUserId.isNotEmpty()) {
                        CloudflareClient.sendCloudflareMessage(
                            context = context,
                            receiverId = targetUserId,
                            text = text,
                            type = "text"
                        ) { _, _ -> }
                    }
                },
                onSendFile = { uri, mimeType ->
                    var displayName = "file_${System.currentTimeMillis()}"
                    try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                val n = cursor.getString(nameIndex)
                                if (!n.isNullOrBlank()) displayName = n
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }

                    if (!displayName.contains(".")) {
                        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        if (!ext.isNullOrBlank()) displayName = "$displayName.$ext"
                    }

                    val msgType = when {
                        mimeType.startsWith("image/") -> "image"
                        mimeType.startsWith("audio/") -> "audio"
                        mimeType.startsWith("video/") -> "video"
                        else -> "file"
                    }

                    val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    val localMsg = Message(
                        id = "msg_${System.currentTimeMillis()}",
                        content = when (msgType) {
                            "image" -> MessageContent.Photo(listOf(uri.toString()))
                            else -> MessageContent.Document(displayName, "مرفق وسائط", msgType)
                        },
                        timestamp = now,
                        isOutgoing = true
                    )
                    messages = messages + localMsg

                    // Upload to Cloudflare R2 bucket and broadcast
                    if (targetUserId.isNotEmpty()) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                if (bytes != null) {
                                    CloudflareClient.uploadMediaFile(context, bytes, displayName, mimeType) { success, r2Url ->
                                        if (success && !r2Url.isNullOrEmpty()) {
                                            CloudflareClient.sendCloudflareMessage(
                                                context = context,
                                                receiverId = targetUserId,
                                                text = displayName,
                                                type = msgType,
                                                mediaUrl = r2Url,
                                                fileName = displayName
                                            ) { _, _ -> }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                },
                onTyping = {}
            )
        }
    }
}

/**
 * Top Bar matching general application design:
 * - Pure icon for voice call & pure icon for video call
 * - Clicking name navigates to user profile
 * - Clicking avatar checks story/status
 * - Clean white background with subtle border
 */
@Composable
fun ChatTopBar(
    targetUserId: String = "",
    userName: String = "سامر الأتروني",
    isOnline: Boolean = true,
    isTyping: Boolean = false,
    avatarUrl: String = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=100&h=100&fit=crop",
    onAvatarClick: () -> Unit = {},
    onNameClick: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hasStory = remember(targetUserId, userName) {
        StoryManager.hasActiveStory(context, targetUserId, userName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .border(1.dp, Color(0xFFE2E8F0))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "رجوع",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Avatar (With green/blue story ring if story exists)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable { onAvatarClick() }
                .padding(if (hasStory) 2.dp else 0.dp)
                .background(
                    if (hasStory) Brush.sweepGradient(listOf(Color(0xFF2563EB), Color(0xFF10B981), Color(0xFF2563EB)))
                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                    CircleShape
                )
                .padding(if (hasStory) 2.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "الصورة الشخصية",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                contentScale = ContentScale.Crop
            )
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF10B981), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Clickable User Name & Status Column -> Opens Personal Profile
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onNameClick() }
        ) {
            Text(
                text = userName,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp,
                fontFamily = TajawalFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isTyping) {
                Text(
                    text = "يكتب الآن...",
                    color = Color(0xFF2563EB),
                    fontSize = 11.5.sp,
                    fontFamily = TajawalFontFamily,
                    maxLines = 1
                )
            } else if (isOnline) {
                Text(
                    text = "متصل الآن",
                    color = Color(0xFF10B981),
                    fontSize = 11.5.sp,
                    fontFamily = TajawalFontFamily,
                    maxLines = 1
                )
            } else {
                Text(
                    text = "نشط مؤخراً",
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    fontFamily = TajawalFontFamily,
                    maxLines = 1
                )
            }
        }

        // 1. Audio Call Button - Small icon without text
        IconButton(
            onClick = {
                val intent = Intent(context, CallActivity::class.java).apply {
                    putExtra("callID", "call_${System.currentTimeMillis()}")
                    putExtra("isVideo", false)
                    putExtra("targetUserName", userName)
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .size(38.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "اتصال صوتي",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 2. Video Call Button - Small icon without text (Same Design)
        IconButton(
            onClick = {
                val intent = Intent(context, CallActivity::class.java).apply {
                    putExtra("callID", "call_${System.currentTimeMillis()}")
                    putExtra("isVideo", true)
                    putExtra("targetUserName", userName)
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .size(38.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "اتصال فيديو",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Story Viewer Dialog
 */
@Composable
fun StoryViewerDialog(
    story: UserStory,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
        ) {
            AsyncImage(
                model = story.mediaUrl.ifEmpty { story.avatarUrl },
                contentDescription = "Story Media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Top gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )

            // Bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Header info & close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = story.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = story.userName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = TajawalFontFamily
                        )
                        Text(
                            text = "الحالة اليومية",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontFamily = TajawalFontFamily
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Caption at bottom
            if (story.caption.isNotBlank()) {
                Text(
                    text = story.caption,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontFamily = TajawalFontFamily,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

// ----------------- Unified Clean Bubbles Matching App Design -----------------

@Composable
fun TextBubble(text: String, timestamp: String, isOutgoing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 16.dp
                    )
                )
                .background(if (isOutgoing) Color(0xFF2563EB) else Color.White)
                .border(
                    width = 1.dp,
                    color = if (isOutgoing) Color(0x22FFFFFF) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                color = if (isOutgoing) Color.White else Color(0xFF0F172A),
                fontFamily = TajawalFontFamily,
                fontSize = 14.5.sp,
                lineHeight = 20.sp
            )
            Text(
                text = "$timestamp ${if (isOutgoing) "✓✓" else ""}",
                color = if (isOutgoing) Color(0xFFDBEAFE) else Color(0xFF64748B),
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun SinglePhotoBubble(url: String, timestamp: String, isOutgoing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(200.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
        ) {
            AsyncImage(
                model = Uri.parse(url),
                contentDescription = "صورة",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )
            Text(
                text = "$timestamp ${if (isOutgoing) "✓✓" else ""}",
                color = Color.White,
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun MultiPhotoBubble(urls: List<String>, timestamp: String, isOutgoing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(100.dp)) {
                    AsyncImage(
                        model = Uri.parse(urls.getOrNull(0) ?: ""),
                        contentDescription = "صورة",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                    AsyncImage(
                        model = Uri.parse(urls.getOrNull(1) ?: ""),
                        contentDescription = "صورة",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentBubble(content: MessageContent.Document, timestamp: String, isOutgoing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(
                    if (isOutgoing) Color(0xFF2563EB) else Color.White,
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    if (isOutgoing) Color(0x33FFFFFF) else Color(0xFFE2E8F0),
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isOutgoing) Color.White.copy(alpha = 0.2f) else Color(0xFFEFF6FF),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (isOutgoing) Color.White else Color(0xFF2563EB),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = content.name,
                    color = if (isOutgoing) Color.White else Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    fontFamily = TajawalFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${content.size} • $timestamp",
                    color = if (isOutgoing) Color(0xFFDBEAFE) else Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = TajawalFontFamily
                )
            }
        }
    }
}

@Composable
fun MapCardBubble(content: MessageContent.Location, timestamp: String, isOutgoing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFE2E8F0))
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=400&h=300&fit=crop",
                    contentDescription = "خريطة",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = content.address,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${content.city} • $timestamp",
                    fontSize = 11.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun SnapMediaBubble(content: MessageContent.Snap, timestamp: String, isOutgoing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Snap",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "رسالة مؤقتة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${content.timer} ثواني",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = TajawalFontFamily
                )
            }
        }
    }
}
