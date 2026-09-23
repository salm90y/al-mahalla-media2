package com.ps1.netplay.ui.compose

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ps1.netplay.CallActivity
import com.ps1.netplay.UserManager
import com.ps1.netplay.network.CloudflareClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Photo(val urls: List<String>) : MessageContent()
    data class Document(val name: String, val size: String, val type: String) : MessageContent()
    data class Location(val address: String, val city: String, val country: String) : MessageContent()
    data class Snap(val timer: Int) : MessageContent()
    data class Call(val callId: String, val isVideo: Boolean, val statusText: String) : MessageContent()
}

data class Message(
    val id: String,
    val content: MessageContent,
    val timestamp: String,
    val isOutgoing: Boolean,
    val status: MessageStatus = MessageStatus.DELIVERED
)

data class FullscreenPhotoViewerData(
    val url: String,
    val messageId: String,
    val timestamp: String,
    val isOutgoing: Boolean,
    val senderName: String
)

object ChatMediaCache {
    private val memoryMap = ConcurrentHashMap<String, String>()

    fun register(context: Context, key: String, localPath: String) {
        if (key.isBlank() || localPath.isBlank()) return
        val f = File(localPath)
        if (!f.exists() || f.length() == 0L) return
        memoryMap[key] = localPath
        try {
            context.getSharedPreferences("chat_media_registry", Context.MODE_PRIVATE)
                .edit()
                .putString(key, localPath)
                .apply()
        } catch (_: Exception) {}
    }

    fun get(context: Context, key: String): String? {
        if (key.isBlank()) return null
        val mem = memoryMap[key]
        if (!mem.isNullOrEmpty()) {
            val f = File(mem)
            if (f.exists() && f.length() > 0) return mem
        }
        return try {
            val disk = context.getSharedPreferences("chat_media_registry", Context.MODE_PRIVATE)
                .getString(key, null)
            if (!disk.isNullOrEmpty()) {
                val f = File(disk)
                if (f.exists() && f.length() > 0) {
                    memoryMap[key] = disk
                    disk
                } else null
            } else null
        } catch (_: Exception) { null }
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = cm.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        networkInfo.isConnected
    }
}

fun resolveMediaUrlString(context: Context, rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return ""
    val baseUrl = CloudflareClient.getBaseUrl(context).trimEnd('/')
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        if (trimmed.contains("/media/")) {
            val mediaPath = trimmed.substringAfter("/media/").trimStart('/')
            return "$baseUrl/media/$mediaPath"
        }
        if (trimmed.contains("ahmed1986y.com") || trimmed.contains("ahmed1986y5.workers.dev")) {
            val key = trimmed.substringAfterLast("/").trimStart('/')
            return "$baseUrl/media/uploads/$key"
        }
        return trimmed
    }
    val cleanKey = trimmed.removePrefix("/media/").removePrefix("media/").removePrefix("/")
    return "$baseUrl/media/$cleanKey"
}

fun ensureMediaCachedLocally(context: Context, rawUrl: String, messageId: String? = null) {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank() || trimmed.startsWith("/") || trimmed.startsWith("file://") || trimmed.startsWith("content://") || trimmed.startsWith("data:image")) return

    val resolvedUrl = resolveMediaUrlString(context, trimmed)
    if (!resolvedUrl.startsWith("http://") && !resolvedUrl.startsWith("https://")) return

    val mediaDir = File(context.filesDir, "chat_media").apply { mkdirs() }
    val safeHash = Math.abs(resolvedUrl.hashCode()).toString()
    val simpleName = resolvedUrl.substringAfterLast("/").substringBefore("?")
    val localFile = File(mediaDir, "cache_${safeHash}_$simpleName")

    if (localFile.exists() && localFile.length() > 0) {
        ChatMediaCache.register(context, trimmed, localFile.absolutePath)
        ChatMediaCache.register(context, resolvedUrl, localFile.absolutePath)
        if (!messageId.isNullOrEmpty()) ChatMediaCache.register(context, messageId, localFile.absolutePath)
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val req = okhttp3.Request.Builder().url(resolvedUrl).build()
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bytes = resp.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val tempFile = File(mediaDir, "temp_${safeHash}_$simpleName")
                        tempFile.writeBytes(bytes)
                        tempFile.renameTo(localFile)
                        ChatMediaCache.register(context, trimmed, localFile.absolutePath)
                        ChatMediaCache.register(context, resolvedUrl, localFile.absolutePath)
                        if (!messageId.isNullOrEmpty()) ChatMediaCache.register(context, messageId, localFile.absolutePath)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}

fun resolveMediaUrl(context: Context, rawUrl: String, messageId: String? = null): Any {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty() && messageId.isNullOrEmpty()) return ""

    // 0. Check ChatMediaCache first for instant zero-latency loading
    if (!messageId.isNullOrEmpty()) {
        val cached = ChatMediaCache.get(context, messageId)
        if (!cached.isNullOrEmpty()) {
            val f = File(cached)
            if (f.exists() && f.length() > 0) return f
        }
    }
    if (trimmed.isNotEmpty()) {
        val cached = ChatMediaCache.get(context, trimmed)
        if (!cached.isNullOrEmpty()) {
            val f = File(cached)
            if (f.exists() && f.length() > 0) return f
        }
    }

    // 1. Direct local absolute file or file:// path
    if (trimmed.startsWith("/") || trimmed.startsWith("file://")) {
        val cleanPath = if (trimmed.startsWith("file://")) trimmed.removePrefix("file://") else trimmed
        val file = File(cleanPath)
        if (file.exists() && file.length() > 0) {
            if (!messageId.isNullOrEmpty()) ChatMediaCache.register(context, messageId, file.absolutePath)
            ChatMediaCache.register(context, trimmed, file.absolutePath)
            return file
        }
    }

    if (trimmed.startsWith("content://")) {
        return Uri.parse(trimmed)
    }

    // 2. Base64
    if (trimmed.startsWith("data:image")) {
        return try {
            val base64Data = trimmed.substringAfter("base64,")
            android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        } catch (_: Exception) {
            trimmed
        }
    }

    // 3. Search local media storage directories & cache
    try {
        val mediaDirs = listOf(
            File(context.filesDir, "chat_media"),
            File(context.cacheDir, "chat_media"),
            context.filesDir,
            context.cacheDir
        )
        val simpleName = trimmed.substringAfterLast("/")
        val safeHash = Math.abs(trimmed.hashCode()).toString()
        val candidateNames = mutableListOf(
            trimmed,
            simpleName,
            "cache_${safeHash}_$simpleName",
            "img_$trimmed",
            "img_$simpleName"
        )
        if (!messageId.isNullOrEmpty()) {
            candidateNames.add("img_$messageId")
            candidateNames.add(messageId)
            candidateNames.add("img_${messageId}_$simpleName")
            candidateNames.add("img_${messageId}_$trimmed")
        }

        for (dir in mediaDirs) {
            if (!dir.exists()) continue
            for (candidate in candidateNames) {
                val f = File(dir, candidate)
                if (f.exists() && f.length() > 0) {
                    if (!messageId.isNullOrEmpty()) ChatMediaCache.register(context, messageId, f.absolutePath)
                    ChatMediaCache.register(context, trimmed, f.absolutePath)
                    return f
                }
            }
            if (!messageId.isNullOrEmpty()) {
                val matching = dir.listFiles { file ->
                    file.name.contains(messageId) && file.length() > 0
                }
                if (!matching.isNullOrEmpty()) {
                    val f = matching[0]
                    ChatMediaCache.register(context, messageId, f.absolutePath)
                    return f
                }
            }
        }
    } catch (_: Exception) {}

    // Trigger local background download & permanent cache for remote URL
    ensureMediaCachedLocally(context, trimmed, messageId)

    return resolveMediaUrlString(context, trimmed)
}

fun saveImageToGallery(context: Context, model: Any, onResult: (Boolean, String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            var bitmap: Bitmap? = null
            if (model is File && model.exists()) {
                bitmap = BitmapFactory.decodeFile(model.absolutePath)
            } else if (model is String && model.startsWith("/")) {
                val f = File(model)
                if (f.exists()) bitmap = BitmapFactory.decodeFile(f.absolutePath)
            }

            if (bitmap == null) {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(model)
                    .allowHardware(false)
                    .build()
                val result = (loader.execute(req) as? SuccessResult)?.drawable
                if (result is BitmapDrawable) {
                    bitmap = result.bitmap
                }
            }

            if (bitmap != null) {
                val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AlMahalla")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }
                    withContext(Dispatchers.Main) {
                        onResult(true, "تم حفظ الصورة في استوديو الصور بنجاح")
                    }
                    return@launch
                }
            }
            withContext(Dispatchers.Main) {
                onResult(false, "تعذر حفظ الصورة في المعرض")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onResult(false, "حدث خطأ أثناء حفظ الصورة")
            }
        }
    }
}

fun shareImageContent(context: Context, model: Any) {
    try {
        val shareText = if (model is File) "صورة من تطبيق المحلة" else model.toString()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الصورة"))
    } catch (_: Exception) {}
}

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
    val myId = remember { CloudflareClient.getCurrentUserId(context) }
    val myUsername = remember { CloudflareClient.getCurrentUsername(context) }
    val convId = remember(targetUserId, myId) {
        CloudflareClient.getConversationId(myId, targetUserId)
    }

    // Load initial cached messages immediately without delay
    val initialLocal = remember(convId) {
        val cached = CloudflareClient.getLocalChatMessages(context, convId)
        val mediaDir = File(context.filesDir, "chat_media")
        if (cached.isNotEmpty()) {
            cached.map { m ->
                val cachedLocalPath = ChatMediaCache.get(context, m.id)
                    ?: ChatMediaCache.get(context, m.mediaUrl)
                    ?: ChatMediaCache.get(context, m.fileName)

                val localFileCandidate = File(mediaDir, "img_${m.id}")
                val localFileCandidate2 = if (m.fileName.isNotEmpty()) File(mediaDir, "img_${m.id}_${m.fileName}") else null
                val localFileCandidate3 = if (m.fileName.isNotEmpty()) File(mediaDir, m.fileName) else null

                val localPath = when {
                    !cachedLocalPath.isNullOrEmpty() && File(cachedLocalPath).exists() -> cachedLocalPath
                    localFileCandidate.exists() && localFileCandidate.length() > 0 -> localFileCandidate.absolutePath
                    localFileCandidate2 != null && localFileCandidate2.exists() && localFileCandidate2.length() > 0 -> localFileCandidate2.absolutePath
                    localFileCandidate3 != null && localFileCandidate3.exists() && localFileCandidate3.length() > 0 -> localFileCandidate3.absolutePath
                    else -> m.mediaUrl.ifEmpty { m.content }
                }
                if (m.type == "image") {
                    ensureMediaCachedLocally(context, localPath, m.id)
                }
                val content = when (m.type) {
                    "image" -> MessageContent.Photo(listOf(localPath))
                    "call", "audio_call" -> MessageContent.Call(m.mediaUrl.ifBlank { "call_${listOf(myId, targetUserId).sorted().joinToString("_")}" }, isVideo = false, statusText = m.content.ifBlank { "مكالمة صوتية" })
                    "video_call" -> MessageContent.Call(m.mediaUrl.ifBlank { "call_${listOf(myId, targetUserId).sorted().joinToString("_")}" }, isVideo = true, statusText = m.content.ifBlank { "مكالمة فيديو" })
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
    val initialLastIndex = remember { if (initialLocal.isNotEmpty()) initialLocal.size - 1 else 0 }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialLastIndex)
    var isFirstLoad by remember { mutableStateOf(true) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    // Full screen photo viewer state
    var activePhotoViewer by remember { mutableStateOf<FullscreenPhotoViewerData?>(null) }

    // Story viewing state
    var showStoryDialog by remember { mutableStateOf(false) }
    var activeStoryToView by remember { mutableStateOf<UserStory?>(null) }
    var showNoStoryNotice by remember { mutableStateOf(false) }

    // Live Incoming Call state & dismissed cache
    var activeIncomingCall by remember { mutableStateOf<IncomingCallData?>(null) }
    var dismissedCallIds by remember { mutableStateOf(setOf<String>()) }

    // Live bidirectional sync loop from Cloudflare and R2
    LaunchedEffect(targetUserId) {
        if (targetUserId.isNotEmpty()) {
            val mediaDir = File(context.filesDir, "chat_media")
            val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

            while (isActive) {
                CloudflareClient.fetchCloudflareMessages(context, targetUserId) { loaded ->
                    if (loaded.isNotEmpty()) {
                        // Check for recent incoming call signals (last 45 seconds) & ensure not from self
                        if (!CallActivity.isCallActive) {
                            val incomingCallMsg = loaded.lastOrNull { m ->
                                (m.type == "audio_call" || m.type == "video_call" || m.type == "call") &&
                                !m.isOutgoing &&
                                m.senderId != myId &&
                                m.senderId != myUsername &&
                                !m.senderId.startsWith("user_me") &&
                                (System.currentTimeMillis() - m.createdAt < 45000) &&
                                !dismissedCallIds.contains(m.id) &&
                                !dismissedCallIds.contains(m.mediaUrl)
                            }
                            if (incomingCallMsg != null && activeIncomingCall == null) {
                                val isVid = incomingCallMsg.type == "video_call"
                                val callRoom = incomingCallMsg.mediaUrl.ifBlank { "call_${listOf(myId, targetUserId).sorted().joinToString("_")}" }
                                activeIncomingCall = IncomingCallData(
                                    callerId = targetUserId,
                                    callerName = userName,
                                    callerAvatar = avatarUrl,
                                    callRoomId = callRoom,
                                    isVideo = isVid,
                                    timestamp = incomingCallMsg.createdAt
                                )
                            }
                        }

                        val currentPhotoMap = messages.associate { curr ->
                            curr.id to (curr.content as? MessageContent.Photo)?.urls?.firstOrNull()
                        }
                        val mapped = loaded.map { m ->
                            val existingLocal = currentPhotoMap[m.id]
                            val cachedLocalPath = ChatMediaCache.get(context, m.id)
                                ?: ChatMediaCache.get(context, m.mediaUrl)
                                ?: ChatMediaCache.get(context, m.fileName)

                            val localFileCandidate = File(mediaDir, "img_${m.id}")
                            val localFileCandidate2 = if (m.fileName.isNotEmpty()) File(mediaDir, "img_${m.id}_${m.fileName}") else null
                            val localFileCandidate3 = if (m.fileName.isNotEmpty()) File(mediaDir, m.fileName) else null

                            val localPath = when {
                                !existingLocal.isNullOrEmpty() && (existingLocal.startsWith("/") || existingLocal.startsWith("file://") || existingLocal.startsWith("content://")) -> existingLocal
                                !cachedLocalPath.isNullOrEmpty() && File(cachedLocalPath).exists() -> cachedLocalPath
                                localFileCandidate.exists() && localFileCandidate.length() > 0 -> localFileCandidate.absolutePath
                                localFileCandidate2 != null && localFileCandidate2.exists() && localFileCandidate2.length() > 0 -> localFileCandidate2.absolutePath
                                localFileCandidate3 != null && localFileCandidate3.exists() && localFileCandidate3.length() > 0 -> localFileCandidate3.absolutePath
                                else -> m.mediaUrl.ifEmpty { m.content }
                            }
                            val content = when (m.type) {
                                "image" -> MessageContent.Photo(listOf(localPath))
                                "call", "audio_call" -> MessageContent.Call(m.mediaUrl.ifBlank { "call_${listOf(myId, targetUserId).sorted().joinToString("_")}" }, isVideo = false, statusText = m.content.ifBlank { "مكالمة صوتية" })
                                "video_call" -> MessageContent.Call(m.mediaUrl.ifBlank { "call_${listOf(myId, targetUserId).sorted().joinToString("_")}" }, isVideo = true, statusText = m.content.ifBlank { "مكالمة فيديو" })
                                "file", "audio", "video" -> MessageContent.Document(m.fileName.ifEmpty { m.content }, "ملف", m.type)
                                else -> MessageContent.Text(m.content)
                            }
                            val time = timeFormatter.format(Date(m.createdAt))
                            Message(m.id, content, time, m.isOutgoing)
                        }
                        // Avoid unnecessary state re-assignments to keep scrolling 120 FPS buttery smooth
                        val loadedIds = mapped.map { it.id }.toSet()
                        val pending = messages.filter { curr -> curr.isOutgoing && !loadedIds.contains(curr.id) }
                        val newMerged = (mapped + pending).distinctBy { it.id }
                        if (newMerged.size != messages.size || newMerged.map { it.id to it.status } != messages.map { it.id to it.status }) {
                            messages = newMerged
                        }
                    }
                }
                delay(2500)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (isFirstLoad) {
                listState.scrollToItem(messages.size - 1)
                isFirstLoad = false
            } else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastVisible >= messages.size - 3) {
                    listState.scrollToItem(messages.size - 1)
                }
            }
        }
    }

    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    // Full screen interactive photo viewer dialog
    if (activePhotoViewer != null) {
        val photoData = activePhotoViewer!!
        FullscreenPhotoViewerDialog(
            data = photoData,
            onDismiss = { activePhotoViewer = null },
            onSave = {
                saveImageToGallery(context, resolveMediaUrl(context, photoData.url)) { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onShare = {
                shareImageContent(context, resolveMediaUrl(context, photoData.url))
            },
            onDelete = {
                val msgIdToDelete = photoData.messageId
                messages = messages.filter { it.id != msgIdToDelete }
                CloudflareClient.deleteLocalChatMessage(context, convId, msgIdToDelete)
                activePhotoViewer = null
                Toast.makeText(context, "تم حذف الصورة من المحادثة", Toast.LENGTH_SHORT).show()
            }
        )
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

        // 2. SCROLLABLE MESSAGES (Always strictly Left-to-Right for chat bubbles: outgoing on Right, incoming on Left)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id },
                    contentType = { it.content::class.java.simpleName }
                ) { message ->
                    when (message.content) {
                        is MessageContent.Text -> TextBubble(
                            text = message.content.text,
                            timestamp = message.timestamp,
                            isOutgoing = message.isOutgoing,
                            status = message.status
                        )
                        is MessageContent.Photo -> {
                            if (message.content.urls.size == 1) {
                                SinglePhotoBubble(
                                    url = message.content.urls.first(),
                                    timestamp = message.timestamp,
                                    isOutgoing = message.isOutgoing,
                                    status = message.status,
                                    onPhotoClick = { clickedUrl ->
                                        activePhotoViewer = FullscreenPhotoViewerData(
                                            url = clickedUrl,
                                            messageId = message.id,
                                            timestamp = message.timestamp,
                                            isOutgoing = message.isOutgoing,
                                            senderName = if (message.isOutgoing) "أنت" else userName
                                        )
                                    }
                                )
                            } else {
                                MultiPhotoBubble(
                                    urls = message.content.urls,
                                    timestamp = message.timestamp,
                                    isOutgoing = message.isOutgoing,
                                    status = message.status,
                                    onPhotoClick = { clickedUrl ->
                                        activePhotoViewer = FullscreenPhotoViewerData(
                                            url = clickedUrl,
                                            messageId = message.id,
                                            timestamp = message.timestamp,
                                            isOutgoing = message.isOutgoing,
                                            senderName = if (message.isOutgoing) "أنت" else userName
                                        )
                                    }
                                )
                            }
                        }
                        is MessageContent.Document -> DocumentBubble(
                            content = message.content,
                            timestamp = message.timestamp,
                            isOutgoing = message.isOutgoing,
                            status = message.status
                        )
                        is MessageContent.Location -> MapCardBubble(message.content, message.timestamp, message.isOutgoing)
                        is MessageContent.Snap -> SnapMediaBubble(message.content, message.timestamp, message.isOutgoing)
                        is MessageContent.Call -> CallBubble(
                            content = message.content,
                            timestamp = message.timestamp,
                            isOutgoing = message.isOutgoing,
                            targetUserName = userName,
                            onJoinCall = {
                                val intent = Intent(context, CallActivity::class.java).apply {
                                    putExtra("callID", message.content.callId)
                                    putExtra("isVideo", message.content.isVideo)
                                    putExtra("targetUserName", userName)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
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
                    val hasNet = isNetworkAvailable(context)
                    val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    val msgId = "msg_${System.currentTimeMillis()}_${(100..999).random()}"
                    val localMsg = Message(
                        id = msgId,
                        content = MessageContent.Text(text),
                        timestamp = now,
                        isOutgoing = true,
                        status = if (hasNet) MessageStatus.SENDING else MessageStatus.FAILED
                    )
                    messages = messages + localMsg

                    if (!hasNet) {
                        Toast.makeText(context, "تعذر الإرسال: لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show()
                    } else if (targetUserId.isNotEmpty()) {
                        CloudflareClient.updateLocalConversation(context, targetUserId, userName, avatarUrl, text, System.currentTimeMillis())
                        CloudflareClient.sendCloudflareMessage(
                            context = context,
                            receiverId = targetUserId,
                            text = text,
                            type = "text",
                            messageId = msgId
                        ) { success, _ ->
                            messages = messages.map { m ->
                                if (m.id == msgId) {
                                    m.copy(status = if (success) MessageStatus.DELIVERED else MessageStatus.FAILED)
                                } else m
                            }
                            if (!success) {
                                Toast.makeText(context, "تعذر الإرسال: تحقق من الاتصال بالإنترنت", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        messages = messages.map { m ->
                            if (m.id == msgId) m.copy(status = MessageStatus.DELIVERED) else m
                        }
                    }
                },
                onSendFile = { uri, mimeType ->
                    val hasNet = isNetworkAvailable(context)
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
                    val msgId = "msg_${System.currentTimeMillis()}_${(100..999).random()}"

                    // Read bytes immediately and cache to local app storage
                    var localSavedPath = uri.toString()
                    var fileBytes: ByteArray? = null
                    try {
                        fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (fileBytes != null && fileBytes.isNotEmpty()) {
                            val mediaDir = File(context.filesDir, "chat_media").apply { mkdirs() }
                            val localFile = File(mediaDir, "img_${msgId}_$displayName")
                            localFile.writeBytes(fileBytes)
                            localSavedPath = localFile.absolutePath

                            try {
                                val altFile1 = File(mediaDir, "img_$msgId")
                                altFile1.writeBytes(fileBytes)
                                val altFile2 = File(mediaDir, msgId)
                                altFile2.writeBytes(fileBytes)
                                val altFile3 = File(mediaDir, displayName)
                                altFile3.writeBytes(fileBytes)
                            } catch (_: Exception) {}

                            ChatMediaCache.register(context, msgId, localSavedPath)
                            ChatMediaCache.register(context, displayName, localSavedPath)
                            ChatMediaCache.register(context, "img_$msgId", localSavedPath)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val localMsg = Message(
                        id = msgId,
                        content = when (msgType) {
                            "image" -> MessageContent.Photo(listOf(localSavedPath))
                            else -> MessageContent.Document(displayName, "مرفق وسائط", msgType)
                        },
                        timestamp = now,
                        isOutgoing = true,
                        status = if (hasNet) MessageStatus.SENDING else MessageStatus.FAILED
                    )
                    messages = messages + localMsg

                    if (!hasNet) {
                        Toast.makeText(context, "تعذر إرسال الصورة: لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show()
                    } else if (targetUserId.isNotEmpty()) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val bytes = fileBytes ?: context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                if (bytes != null) {
                                    CloudflareClient.uploadMediaFile(context, bytes, displayName, mimeType) { success, r2Url ->
                                        if (success && !r2Url.isNullOrEmpty()) {
                                            ChatMediaCache.register(context, r2Url, localSavedPath)
                                            val r2Key = r2Url.substringAfterLast("/")
                                            ChatMediaCache.register(context, r2Key, localSavedPath)

                                            messages = messages.map { m ->
                                                if (m.id == msgId) {
                                                    m.copy(
                                                        status = MessageStatus.DELIVERED,
                                                        content = when (m.content) {
                                                            is MessageContent.Photo -> MessageContent.Photo(listOf(localSavedPath))
                                                            else -> m.content
                                                        }
                                                    )
                                                } else m
                                            }
                                            CloudflareClient.sendCloudflareMessage(
                                                context = context,
                                                receiverId = targetUserId,
                                                text = displayName,
                                                type = msgType,
                                                mediaUrl = r2Url,
                                                fileName = displayName,
                                                messageId = msgId
                                            ) { sendSuccess, _ ->
                                                if (!sendSuccess) {
                                                    messages = messages.map { m ->
                                                        if (m.id == msgId) m.copy(status = MessageStatus.FAILED) else m
                                                    }
                                                    Toast.makeText(context, "تعذر إرسال المرفق: خطأ في الاتصال", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            // Keep local display and mark failed if network issue
                                            messages = messages.map { m ->
                                                if (m.id == msgId) m.copy(status = MessageStatus.FAILED) else m
                                            }
                                            Toast.makeText(context, "تعذر إرسال الصورة: تحقق من الاتصال بالإنترنت", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    messages = messages.map { m ->
                                        if (m.id == msgId) m.copy(status = MessageStatus.FAILED) else m
                                    }
                                    Toast.makeText(context, "تعذر قراءة الملف المرفق", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                messages = messages.map { m ->
                                    if (m.id == msgId) m.copy(status = MessageStatus.FAILED) else m
                                }
                                Toast.makeText(context, "تعذر الإرسال: فشل النقل", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        messages = messages.map { m ->
                            if (m.id == msgId) m.copy(status = MessageStatus.DELIVERED) else m
                        }
                    }
                },
                onTyping = {}
            )
        }
    }

    // Incoming Call Ringing Overlay
    activeIncomingCall?.let { incCall ->
        IncomingCallAlertModal(
            callData = incCall,
            onAccept = {
                val roomId = incCall.callRoomId
                dismissedCallIds = dismissedCallIds + roomId
                activeIncomingCall = null
                CallActivity.stopAllRingtones(context)

                val intent = Intent(context, CallActivity::class.java).apply {
                    putExtra("callID", roomId)
                    putExtra("isVideo", incCall.isVideo)
                    putExtra("targetUserName", incCall.callerName)
                    putExtra("targetUserAvatar", incCall.callerAvatar)
                }
                context.startActivity(intent)
            },
            onDecline = {
                dismissedCallIds = dismissedCallIds + incCall.callRoomId
                activeIncomingCall = null
                CallActivity.stopAllRingtones(context)
            }
        )
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

        // 1. Audio Call Button - Free Icon
        IconButton(
            onClick = {
                val myUserId = UserManager.getCurrentUser(context)?.username ?: "user_me"
                val callRoomId = "call_" + listOf(myUserId, targetUserId.ifBlank { "partner" }).sorted().joinToString("_")
                
                if (targetUserId.isNotEmpty()) {
                    CloudflareClient.sendCloudflareMessage(
                        context = context,
                        receiverId = targetUserId,
                        text = "مكالمة صوتية واردة",
                        type = "audio_call",
                        mediaUrl = callRoomId,
                        fileName = userName
                    ) { _, _ -> }
                }

                val intent = Intent(context, CallActivity::class.java).apply {
                    putExtra("callID", callRoomId)
                    putExtra("isVideo", false)
                    putExtra("targetUserName", userName)
                    putExtra("targetUserAvatar", avatarUrl)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "اتصال صوتي",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 2. Video Call Button - Free Icon
        IconButton(
            onClick = {
                val myUserId = UserManager.getCurrentUser(context)?.username ?: "user_me"
                val callRoomId = "call_" + listOf(myUserId, targetUserId.ifBlank { "partner" }).sorted().joinToString("_")

                if (targetUserId.isNotEmpty()) {
                    CloudflareClient.sendCloudflareMessage(
                        context = context,
                        receiverId = targetUserId,
                        text = "مكالمة فيديو واردة",
                        type = "video_call",
                        mediaUrl = callRoomId,
                        fileName = userName
                    ) { _, _ -> }
                }

                val intent = Intent(context, CallActivity::class.java).apply {
                    putExtra("callID", callRoomId)
                    putExtra("isVideo", true)
                    putExtra("targetUserName", userName)
                    putExtra("targetUserAvatar", avatarUrl)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "اتصال فيديو",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(24.dp)
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
fun TextBubble(
    text: String, 
    timestamp: String, 
    isOutgoing: Boolean,
    status: MessageStatus = MessageStatus.DELIVERED
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
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
                    .background(
                        if (isOutgoing) {
                            if (status == MessageStatus.FAILED) Color(0xFFEF4444) else Color(0xFF2563EB)
                        } else Color.White
                    )
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
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isOutgoing) {
                        when (status) {
                            MessageStatus.SENDING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(11.dp),
                                    color = Color.White,
                                    strokeWidth = 1.5.dp
                                )
                            }
                            MessageStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "تعذر الإرسال",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            else -> {
                                Text(
                                    text = "✓✓",
                                    color = Color(0xFFDBEAFE),
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Text(
                        text = timestamp,
                        color = if (isOutgoing) Color(0xFFDBEAFE) else Color(0xFF64748B),
                        fontFamily = TajawalFontFamily,
                        fontSize = 10.sp
                    )
                }
            }

            if (isOutgoing && status == MessageStatus.FAILED) {
                Text(
                    text = "تعذر الإرسال (تحقق من الإنترنت)",
                    color = Color(0xFFDC2626),
                    fontFamily = TajawalFontFamily,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SinglePhotoBubble(
    url: String, 
    timestamp: String, 
    isOutgoing: Boolean,
    status: MessageStatus = MessageStatus.DELIVERED,
    onPhotoClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val imageModel = remember(url) { resolveMediaUrl(context, url) }
    val coilRequest = remember(imageModel) {
        ImageRequest.Builder(context)
            .data(imageModel)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    LaunchedEffect(url) {
        ensureMediaCachedLocally(context, url)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(
                        1.5.dp,
                        if (isOutgoing && status == MessageStatus.FAILED) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onPhotoClick(url) }
            ) {
                AsyncImage(
                    model = coilRequest,
                    contentDescription = "صورة",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Bottom gradient for timestamp & status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isOutgoing) {
                        when (status) {
                            MessageStatus.SENDING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(11.dp),
                                    color = Color.White,
                                    strokeWidth = 1.5.dp
                                )
                            }
                            MessageStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "تعذر الإرسال",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            else -> {
                                Text(
                                    text = "✓✓",
                                    color = Color.White,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Text(
                        text = timestamp,
                        color = Color.White,
                        fontFamily = TajawalFontFamily,
                        fontSize = 10.sp
                    )
                }
            }

            if (isOutgoing && status == MessageStatus.FAILED) {
                Row(
                    modifier = Modifier.padding(top = 3.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "تعذر إرسال الصورة (لا يوجد اتصال بالإنترنت)",
                        color = Color(0xFFDC2626),
                        fontFamily = TajawalFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MultiPhotoBubble(
    urls: List<String>, 
    timestamp: String, 
    isOutgoing: Boolean,
    status: MessageStatus = MessageStatus.DELIVERED,
    onPhotoClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(urls) {
        urls.forEach { ensureMediaCachedLocally(context, it) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(
                        1.5.dp, 
                        if (isOutgoing && status == MessageStatus.FAILED) Color(0xFFEF4444) else Color(0xFFE2E8F0), 
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(110.dp)) {
                        urls.take(2).forEach { itemUrl ->
                            val model = remember(itemUrl) { resolveMediaUrl(context, itemUrl) }
                            val coilReq = remember(model) {
                                ImageRequest.Builder(context)
                                    .data(model)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .networkCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { onPhotoClick(itemUrl) }
                            ) {
                                AsyncImage(
                                    model = coilReq,
                                    contentDescription = "صورة",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            if (isOutgoing && status == MessageStatus.FAILED) {
                Text(
                    text = "تعذر إرسال الصور (لا يوجد اتصال بالإنترنت)",
                    color = Color(0xFFDC2626),
                    fontFamily = TajawalFontFamily,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 3.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FullscreenPhotoViewerDialog(
    data: FullscreenPhotoViewerData,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val imageModel = remember(data.url) { resolveMediaUrl(context, data.url) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2090D16))
        ) {
            // Main Photo Display
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 72.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = "عرض الصورة كاملة",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(38.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "جاري تحميل الصورة بدقة عالية...",
                                    color = Color(0xFFE2E8F0),
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "تعذر تحميل الصورة بدقة كاملة",
                                    color = Color(0xFF94A3B8),
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                )
            }

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Close Button & Sender Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = data.senderName,
                            color = Color.White,
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = data.timestamp,
                            color = Color(0xFF94A3B8),
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // Top Quick Action Icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "حفظ",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0x33EF4444), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Bottom Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save Button
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ الصورة",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.5.sp
                        )
                    }

                    // Share Button
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FFFFFF)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مشاركة",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            fontSize = 13.5.sp
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0x33EF4444), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف الصورة",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "حذف الصورة",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف هذه الصورة من سجل المحادثة؟",
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "حذف",
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(
                        text = "إلغاء",
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF64748B)
                    )
                }
            }
        )
    }
}

@Composable
fun DocumentBubble(
    content: MessageContent.Document, 
    timestamp: String, 
    isOutgoing: Boolean,
    status: MessageStatus = MessageStatus.DELIVERED
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isOutgoing) {
                            if (status == MessageStatus.FAILED) Color(0xFFEF4444) else Color(0xFF2563EB)
                        } else Color.White,
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
                    if (isOutgoing && status == MessageStatus.SENDING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else if (isOutgoing && status == MessageStatus.FAILED) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "تعذر الإرسال",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (isOutgoing) Color.White else Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                    }
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

            if (isOutgoing && status == MessageStatus.FAILED) {
                Text(
                    text = "تعذر إرسال الملف (لا يوجد اتصال بالإنترنت)",
                    color = Color(0xFFDC2626),
                    fontFamily = TajawalFontFamily,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 3.dp, end = 4.dp)
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
fun CallBubble(
    content: MessageContent.Call,
    timestamp: String,
    isOutgoing: Boolean,
    targetUserName: String,
    onJoinCall: () -> Unit
) {
    val isVideo = content.isVideo
    val accentColor = if (isVideo) Color(0xFF2563EB) else Color(0xFF10B981)
    val bgLight = if (isVideo) Color(0xFFEFF6FF) else Color(0xFFECFDF5)
    val callTitle = if (isVideo) "مكالمة فيديو" else "مكالمة صوتية"
    val callIcon = if (isVideo) Icons.Default.Videocam else Icons.Default.Call

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 230.dp, max = 290.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bgLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = callIcon,
                        contentDescription = callTitle,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = callTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (isOutgoing) "مكالمة صادرة • $timestamp" else "مكالمة واردة • $timestamp",
                        fontSize = 11.5.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onJoinCall,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = callIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOutgoing) "الانضمام للمكالمة" else "الرد والانضمام",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold
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
                .widthIn(min = 160.dp, max = 220.dp)
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
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "رسالة مؤقتة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${content.timer} ثواني • $timestamp",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = TajawalFontFamily
                )
            }
        }
    }
}


