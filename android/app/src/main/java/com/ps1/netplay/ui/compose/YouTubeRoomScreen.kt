package com.ps1.netplay.ui.compose

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ps1.netplay.network.CloudflareClient
import com.ps1.netplay.network.RealVoipEngine
import com.ps1.netplay.network.ZegoCallManager
import com.ps1.netplay.ui.RoomCameraHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. DATA MODELS & ENUMS FOR YOUTUBE WATCH-PARTY ROOM
// ----------------------------------------------------
enum class YouTubeRoomSubTab {
    PLAYER,
    CHAT,
    CAMERAS,
    INTERCOM,
    USERS,
    SETTINGS
}

enum class CameraBoxSize(val sizeDp: androidx.compose.ui.unit.Dp, val label: String) {
    SMALL(92.dp, "صغير"),
    MEDIUM(130.dp, "متوسط"),
    LARGE(170.dp, "كبير")
}

enum class CameraBoxShape(val label: String) {
    ROUNDED("مستدير"),
    SQUARE("مربع"),
    CIRCLE("دائري")
}

data class YouTubeVideoItem(
    val id: String,
    val title: String,
    val channelTitle: String = "يوتيوب",
    val duration: String = "مباشر",
    val viewCount: String = "متزامن",
    val publishedTime: String = "الآن",
    val thumbnailUrl: String = "",
    val category: String = "يوتيوب",
    val isLive: Boolean = false,
    val is4K: Boolean = true
)

data class YouTubeChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val time: String,
    val isMe: Boolean = false,
    val avatarColor: Color = Color(0xFF2563EB),
    val imageUrl: String? = null
)

data class YouTubeRoomUser(
    val id: String,
    val name: String,
    var role: String,
    val isHost: Boolean = false,
    val isOnline: Boolean = true,
    val isSpeaking: Boolean = false,
    val hasCameraActive: Boolean = false,
    val isFrontCamera: Boolean = true,
    val avatarBg: Color = Color(0xFF2563EB),
    val canChangeVideo: Boolean = true,
    val isMutedVoice: Boolean = false,
    val isMutedChat: Boolean = false
)

// Initial library
val INITIAL_YOUTUBE_CATALOG = listOf(
    YouTubeVideoItem(
        id = "yK4U4XkMhFk",
        title = "تلاوة خاشعة ومريحة للأعصاب من سورة مريم بصوت القارئ إسلام صبحي",
        channelTitle = "تلاوات القرآن الكريم المباركة",
        duration = "32:45",
        viewCount = "14.2M مشاهدة",
        publishedTime = "منذ 3 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=600&auto=format&fit=crop&q=80",
        category = "يوتيوب",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "dQw4w9WgXcQ",
        title = "وثائقي مذهل: كيف غيرت الحوسبة السحابية وشبكات Cloudflare خوادم الإنترنت؟",
        channelTitle = "عالم التكنولوجيا والأفلام الوثائقية",
        duration = "24:18",
        viewCount = "2.1M مشاهدة",
        publishedTime = "منذ أسبوعين",
        thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80",
        category = "يوتيوب",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "nature_relax_4k",
        title = "رحلة ساحرة عبر جبال الألب السويسرية والطبيعة الخلابة بجودة 4K 60FPS",
        channelTitle = "Earth Relax Studio",
        duration = "45:00",
        viewCount = "8.4M مشاهدة",
        publishedTime = "منذ 4 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80",
        category = "يوتيوب",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "tech_future",
        title = "مراجعة شاملة: أفضل هواتف وتقنيات عام 2026 مقارنة بالذكاء الاصطناعي",
        channelTitle = "فيصل السيف | Tech Voice",
        duration = "19:50",
        viewCount = "1.6M مشاهدة",
        publishedTime = "منذ 5 أيام",
        thumbnailUrl = "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=600&auto=format&fit=crop&q=80",
        category = "يوتيوب",
        isLive = false
    )
)

private fun formatTime(seconds: Float): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}

// ----------------------------------------------------
// 2. MAIN COMPOSABLE: YouTubeRoomScreen
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeRoomScreen(
    roomId: String = "yt_room_default",
    initialVideoId: String = "dQw4w9WgXcQ",
    roomTitle: String = "سينما اليوتيوب",
    roomCode: String = "#YT-9024",
    isStealthMode: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Disable click sound effects globally in room
    val localView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val prevSound = localView.isSoundEffectsEnabled
        localView.isSoundEffectsEnabled = false
        onDispose {
            localView.isSoundEffectsEnabled = prevSound
        }
    }

    // Identity and Roles
    val currentUserId = remember { CloudflareClient.getCurrentUserId(context) }
    val currentUserName = remember { CloudflareClient.getCurrentUsername(context).ifBlank { "أحمد" } }
    val isAppOwner = remember { YouTubeRoomManager.isAppOwner(context) }
    val isHost = remember { isAppOwner || roomTitle.contains(currentUserName) }

    // Sub-tab selection (PLAYER default)
    var activeSubTab by remember { mutableStateOf<YouTubeRoomSubTab>(YouTubeRoomSubTab.PLAYER) }

    // Video Catalog state
    val videoCatalog = remember {
        mutableStateListOf<YouTubeVideoItem>().apply {
            addAll(INITIAL_YOUTUBE_CATALOG)
        }
    }

    // Active currently playing video
    var currentVideo by remember(initialVideoId) {
        val found = videoCatalog.find { it.id == initialVideoId }
        mutableStateOf(
            found ?: YouTubeVideoItem(
                id = initialVideoId.ifBlank { "dQw4w9WgXcQ" },
                title = roomTitle.ifBlank { "فيديو يوتيوب متزامن" },
                channelTitle = "سحابة Cloudflare",
                duration = "مباشر",
                viewCount = "متزامن",
                publishedTime = "الآن",
                thumbnailUrl = "https://img.youtube.com/vi/${initialVideoId.ifBlank { "dQw4w9WgXcQ" }}/hqdefault.jpg"
            )
        )
    }

    // Playback and Synchronization States
    var isPlaying by remember { mutableStateOf(true) }
    var videoVolume by remember { mutableFloatStateOf(1.0f) }
    var currentPositionSec by remember { mutableFloatStateOf(0f) }
    var totalDurationSec by remember { mutableFloatStateOf(100f) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Search Bar & Exit Dialog States
    var searchQuery by remember { mutableStateOf("") }
    var isDropdownOpen by remember { mutableStateOf(false) }
    var isSearchModalOpen by remember { mutableStateOf(false) }
    var isSearchingRealYouTube by remember { mutableStateOf(false) }
    val liveSearchSuggestions = remember { mutableStateListOf<String>() }
    val realSearchResults = remember { mutableStateListOf<YouTubeVideoItem>() }
    var isExitConfirmDialogOpen by remember { mutableStateOf(false) }

    // Walkie-Talkie Intercom State (Real Zego Engine + Loudspeaker)
    var isIntercomTalking by remember { mutableStateOf(false) }
    var isIntercomLoudspeaker by remember { mutableStateOf(true) }
    var isNoiseSuppressionEnabled by remember { mutableStateOf(true) }
    var isEchoCancellationEnabled by remember { mutableStateOf(true) }
    var isAutoGainControlEnabled by remember { mutableStateOf(true) }
    var micSensitivity by remember { mutableFloatStateOf(0.85f) }
    var isPushToTalkMode by remember { mutableStateOf(false) }

    // Cameras Configuration & State: Small by default as requested!
    var isCameraActive by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var cameraBoxSize by remember { mutableStateOf<CameraBoxSize>(CameraBoxSize.SMALL) }
    var cameraBoxShape by remember { mutableStateOf<CameraBoxShape>(CameraBoxShape.ROUNDED) }

    // Dialog & Permission States
    var selectedUserForPermissions by remember { mutableStateOf<YouTubeRoomUser?>(null) }

    // Room Participants state (Real authentic users only)
    val roomUsers = remember {
        mutableStateListOf<YouTubeRoomUser>().apply {
            if (!isStealthMode) {
                add(
                    YouTubeRoomUser(
                        id = currentUserId,
                        name = currentUserName,
                        role = if (isHost) "مضيف الغرفة 👑" else if (isAppOwner) "مالك التطبيق 🛡️" else "مشاهد",
                        isHost = isHost,
                        isOnline = true,
                        isSpeaking = false,
                        hasCameraActive = false,
                        isFrontCamera = true,
                        avatarBg = Color(0xFF2563EB),
                        canChangeVideo = true
                    )
                )
            }
        }
    }

    // Keep camera status in roomUsers updated for local user
    LaunchedEffect(isCameraActive, isFrontCamera) {
        val idx = roomUsers.indexOfFirst { it.id == currentUserId }
        if (idx >= 0) {
            roomUsers[idx] = roomUsers[idx].copy(hasCameraActive = isCameraActive, isFrontCamera = isFrontCamera)
        }
    }

    // Live Room Chat Messages State
    val chatMessages = remember {
        mutableStateListOf(
            YouTubeChatMessage("1", "النظام", "مرحباً بك في غرفة $roomTitle! المشاهدة والدردشة متزامنة بالكامل ⚡", "الآن", false, Color(0xFF2563EB))
        )
    }
    val chatListState = rememberLazyListState()
    var chatInputText by remember { mutableStateOf("") }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // REAL-TIME WEBSOCKET SYNCHRONIZATION CLIENT
    val syncSocket = remember(roomId) {
        YouTubeSyncWebSocket(
            context = context,
            roomId = roomId,
            isStealthMode = isStealthMode,
            onVideoChangeReceived = { vId, vTitle ->
                currentVideo = YouTubeVideoItem(
                    id = vId,
                    title = vTitle.ifBlank { "فيديو يوتيوب متزامن" },
                    channelTitle = "مشاهدة متزامنة",
                    duration = "مباشر",
                    viewCount = "متزامن",
                    publishedTime = "الآن",
                    thumbnailUrl = "https://img.youtube.com/vi/$vId/hqdefault.jpg"
                )
                isPlaying = true
                currentPositionSec = 0f
                // Execute instant video load on WebView for all clients including room owner
                webViewRef?.evaluateJavascript(
                    "if (typeof loadVideoById === 'function') { loadVideoById('$vId'); } else { var ifr = document.getElementById('player'); if (ifr) { ifr.src = 'https://www.youtube.com/embed/$vId?autoplay=1&enablejsapi=1&playsinline=1&controls=0&fs=0&rel=0&modestbranding=1'; } }",
                    null
                )
                Toast.makeText(context, "تم تغيير الفيديو للغرفة: ${vTitle.take(30)} 🎬", Toast.LENGTH_SHORT).show()
            },
            onPlaybackStateReceived = { playState, pos ->
                // Smooth synchronization: only seek if playhead drift exceeds 2.5 seconds to avoid buffer stutter
                if (pos >= 0f && kotlin.math.abs(currentPositionSec - pos) > 2.5f) {
                    currentPositionSec = pos
                    webViewRef?.evaluateJavascript("if (typeof seekTo === 'function') { seekTo($pos); }", null)
                }
                if (playState && !isPlaying) {
                    isPlaying = true
                    webViewRef?.evaluateJavascript("if (typeof playVideo === 'function') { playVideo(); }", null)
                } else if (!playState && isPlaying) {
                    isPlaying = false
                    webViewRef?.evaluateJavascript("if (typeof pauseVideo === 'function') { pauseVideo(); }", null)
                }
            },
            onChatMessageReceived = { newMsg ->
                if (newMsg.text.startsWith("[IMAGE]:")) {
                    val imgUri = newMsg.text.removePrefix("[IMAGE]:")
                    chatMessages.add(newMsg.copy(text = "📷 صورة", imageUrl = imgUri))
                } else {
                    chatMessages.add(newMsg)
                }
            },
            onStateRequested = { client ->
                client.broadcastVideoChange(currentVideo.id, currentVideo.title)
                client.broadcastPlaybackState(isPlaying, currentPositionSec)
            },
            onMemberActionReceived = { targetId, action ->
                if (targetId == currentUserId) {
                    when (action) {
                        "KICK" -> {
                            Toast.makeText(context, "تم إخراجك من الغرفة بواسطة المشرف", Toast.LENGTH_LONG).show()
                            onBack()
                        }
                        "MUTE_VOICE" -> {
                            isIntercomTalking = false
                            ZegoCallManager.setMicrophoneMute(true)
                            RealVoipEngine.setMute(true)
                            Toast.makeText(context, "تم كتم صوت المايكروفون الخاص بك من قبل المشرف 🔇", Toast.LENGTH_SHORT).show()
                        }
                        "ALLOW_VIDEO" -> {
                            val idx = roomUsers.indexOfFirst { it.id == currentUserId }
                            if (idx >= 0) roomUsers[idx] = roomUsers[idx].copy(canChangeVideo = true)
                            Toast.makeText(context, "منحك المشرف صلاحية التحكم في الفيديو 🎬", Toast.LENGTH_SHORT).show()
                        }
                        "RESTRICT_VIDEO" -> {
                            val idx = roomUsers.indexOfFirst { it.id == currentUserId }
                            if (idx >= 0) roomUsers[idx] = roomUsers[idx].copy(canChangeVideo = false)
                            Toast.makeText(context, "تم تقييد صلاحية التحكم في الفيديو 🔒", Toast.LENGTH_SHORT).show()
                        }
                        "SET_MODERATOR" -> {
                            val idx = roomUsers.indexOfFirst { it.id == currentUserId }
                            if (idx >= 0) roomUsers[idx] = roomUsers[idx].copy(role = "مشرف 🛡️", canChangeVideo = true)
                            Toast.makeText(context, "تمت ترقيتك إلى مشرف الغرفة 🛡️", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onVoiceStateReceived = { uId, _, isTalking ->
                val idx = roomUsers.indexOfFirst { it.id == uId }
                if (idx >= 0) {
                    roomUsers[idx] = roomUsers[idx].copy(isSpeaking = isTalking)
                }
            },
            onCameraStateReceived = { uId, _, isCamActive, isFront ->
                val idx = roomUsers.indexOfFirst { it.id == uId }
                if (idx >= 0) {
                    roomUsers[idx] = roomUsers[idx].copy(hasCameraActive = isCamActive, isFrontCamera = isFront)
                }
            }
        )
    }

    // Permission launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isCameraActive = true
            syncSocket.broadcastCameraState(true, isFrontCamera)
            Toast.makeText(context, "تم تفعيل الكاميرا 📹", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "يرجى منح إذن الكاميرا للمتابعة 🔒", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isIntercomTalking = true
            ZegoCallManager.setMicrophoneMute(false)
            ZegoCallManager.setSpeakerEnabled(context, true)
            syncSocket.broadcastVoiceState(true)
            Toast.makeText(context, "تم تفعيل الميكروفون 🎙️", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "يرجى منح إذن الميكروفون للمتابعة 🔒", Toast.LENGTH_SHORT).show()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val msg = YouTubeChatMessage(
                id = System.currentTimeMillis().toString(),
                sender = currentUserName,
                text = "📷 صورة",
                time = "الآن",
                isMe = true,
                imageUrl = uri.toString()
            )
            chatMessages.add(msg)
            syncSocket.broadcastChatMessage("[IMAGE]:$uri")
        }
    }

    fun toggleCameraWithPermission() {
        if (isCameraActive) {
            isCameraActive = false
            syncSocket.broadcastCameraState(false, isFrontCamera)
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                isCameraActive = true
                syncSocket.broadcastCameraState(true, isFrontCamera)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    fun toggleIntercomWithPermission() {
        if (isIntercomTalking) {
            isIntercomTalking = false
            ZegoCallManager.setMicrophoneMute(true)
            syncSocket.broadcastVoiceState(false)
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                isIntercomTalking = true
                ZegoCallManager.setMicrophoneMute(false)
                ZegoCallManager.setSpeakerEnabled(context, true)
                syncSocket.broadcastVoiceState(true)
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Connect WebSocket and fetch authoritative initial state
    LaunchedEffect(roomId) {
        syncSocket.connect()
        YouTubeRoomManager.getRoomLatest(context, roomId) { latestRoom ->
            if (latestRoom != null && latestRoom.videoId.isNotBlank()) {
                currentVideo = YouTubeVideoItem(
                    id = latestRoom.videoId,
                    title = latestRoom.currentVideoTitle.ifBlank { "فيديو يوتيوب متزامن" },
                    channelTitle = "مشاهدة متزامنة",
                    duration = "مباشر",
                    viewCount = "متزامن",
                    publishedTime = "الآن",
                    thumbnailUrl = latestRoom.thumbnailUrl.ifBlank { "https://img.youtube.com/vi/${latestRoom.videoId}/hqdefault.jpg" }
                )
            }
        }
    }

    // REAL ZEGO WALKIE-TALKIE AUDIO ROOM INITIALIZATION WITH LOUDSPEAKER ROUTING
    val zegoAudioRoomId = remember(roomId) { "yt_room_${roomId.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)}" }
    LaunchedEffect(zegoAudioRoomId) {
        // Enforce Loudspeaker on Android AudioManager immediately
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
        } catch (_: Exception) {}

        ZegoCallManager.startCall(
            context = context,
            roomId = zegoAudioRoomId,
            userId = currentUserId,
            userName = currentUserName,
            isVideo = false,
            isOutgoing = true,
            onConnected = {
                ZegoCallManager.setMicrophoneMute(true)
                ZegoCallManager.setSpeakerEnabled(context, true)
            }
        )
    }

    // Periodic Heartbeat Sync: Host synchronizes playhead every 8 seconds to prevent any drift
    LaunchedEffect(isPlaying, isHost, isAppOwner) {
        if ((isHost || isAppOwner) && isPlaying) {
            while (true) {
                delay(8000)
                if (isPlaying) {
                    syncSocket.broadcastPlaybackState(true, currentPositionSec)
                }
            }
        }
    }

    DisposableEffect(roomId, zegoAudioRoomId) {
        onDispose {
            syncSocket.disconnect()
            ZegoCallManager.endCall(context, zegoAudioRoomId)
            RealVoipEngine.stopVoipSession(context)
        }
    }

    // Seeking & Playback Control Functions with TRUE ROOM SYNCHRONIZATION
    fun performSeek(targetSeconds: Float) {
        val myUser = roomUsers.find { it.id == currentUserId }
        val canControl = isHost || isAppOwner || (myUser?.canChangeVideo == true)
        if (!canControl) {
            Toast.makeText(context, "التحكم في تقديم وإيقاف الفيديو مخصص للمشرفين 🔒", Toast.LENGTH_SHORT).show()
            return
        }
        val safeTarget = targetSeconds.coerceIn(0f, totalDurationSec.coerceAtLeast(1f))
        currentPositionSec = safeTarget
        webViewRef?.evaluateJavascript("if (typeof seekTo === 'function') { seekTo($safeTarget); }", null)
        syncSocket.broadcastPlaybackState(isPlaying, safeTarget)
    }

    fun togglePlayback() {
        val myUser = roomUsers.find { it.id == currentUserId }
        val canControl = isHost || isAppOwner || (myUser?.canChangeVideo == true)
        if (!canControl) {
            Toast.makeText(context, "التحكم في تشغيل وإيقاف الفيديو مخصص للمشرفين 🔒", Toast.LENGTH_SHORT).show()
            return
        }
        val nextPlay = !isPlaying
        isPlaying = nextPlay
        if (nextPlay) {
            webViewRef?.evaluateJavascript("if (typeof playVideo === 'function') { playVideo(); }", null)
        } else {
            webViewRef?.evaluateJavascript("if (typeof pauseVideo === 'function') { pauseVideo(); }", null)
        }
        syncSocket.broadcastPlaybackState(nextPlay, currentPositionSec)
    }

    // Helper to extract video ID from direct YouTube URLs or clean strings
    fun extractYouTubeVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length == 11 && trimmed.matches(Regex("[a-zA-Z0-9_-]{11}"))) {
            return trimmed
        }
        val pattern = Regex("""(?:youtu\.be/|youtube\.com/(?:watch\?v=|embed/|v/|shorts/))([a-zA-Z0-9_-]{11})""")
        return pattern.find(trimmed)?.groupValues?.get(1)
    }

    // Direct, Instant Video Change that IMMEDIATELY updates on Room Owner & All Users
    fun playSelectedVideo(video: YouTubeVideoItem) {
        val cleanId = extractYouTubeVideoId(video.id) ?: video.id.trim()
        if (cleanId.isEmpty()) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        currentVideo = video.copy(id = cleanId)
        isPlaying = true
        currentPositionSec = 0f
        isSearchModalOpen = false
        isDropdownOpen = false

        // 1. Direct Webview playback update with instant iframe load
        val jsCmd = """
            (function() {
                if (typeof loadVideoById === 'function') {
                    loadVideoById('$cleanId');
                } else {
                    var ifr = document.getElementById('player');
                    if (ifr) {
                        ifr.src = "https://www.youtube.com/embed/$cleanId?autoplay=1&enablejsapi=1&playsinline=1&controls=0&fs=0&rel=0&modestbranding=1";
                    }
                }
            })();
        """.trimIndent()
        webViewRef?.evaluateJavascript(jsCmd, null)

        // 2. Broadcast to room owner and all participants
        YouTubeRoomManager.updateRoomVideo(context, roomId, cleanId, video.title)
        syncSocket.broadcastVideoChange(cleanId, video.title)
        syncSocket.broadcastPlaybackState(true, 0f)
        Toast.makeText(context, "جاري تشغيل: ${video.title.take(35)}... 🎬", Toast.LENGTH_SHORT).show()
    }

    // Live search suggestions
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            delay(200)
            try {
                val live = YouTubeSearchEngine.getLiveSuggestions(q)
                liveSearchSuggestions.clear()
                if (live.isNotEmpty()) {
                    liveSearchSuggestions.addAll(live.take(8))
                }
            } catch (_: Exception) {}
        } else {
            liveSearchSuggestions.clear()
        }
    }

    fun executeSearch(query: String) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val cleanQuery = query.trim()
        searchQuery = cleanQuery
        isDropdownOpen = false
        activeSubTab = YouTubeRoomSubTab.PLAYER

        if (cleanQuery.isNotEmpty()) {
            val directId = extractYouTubeVideoId(cleanQuery)
            if (directId != null) {
                val directItem = YouTubeVideoItem(
                    id = directId,
                    title = "فيديو يوتيوب مخصص",
                    channelTitle = "رابط مباشر",
                    duration = "مباشر",
                    viewCount = "مباشر",
                    publishedTime = "الآن",
                    thumbnailUrl = "https://img.youtube.com/vi/$directId/hqdefault.jpg"
                )
                videoCatalog.removeAll { it.id == directId }
                videoCatalog.add(0, directItem)
                playSelectedVideo(directItem)
                return
            }

            coroutineScope.launch {
                isSearchingRealYouTube = true
                try {
                    val realVideos = YouTubeSearchEngine.searchRealYouTube(cleanQuery)
                    realSearchResults.clear()
                    if (realVideos.isNotEmpty()) {
                        realSearchResults.addAll(realVideos)
                        realVideos.reversed().forEach { rv ->
                            videoCatalog.removeAll { it.id == rv.id }
                            videoCatalog.add(0, rv)
                        }
                    }
                } catch (_: Exception) {}
                isSearchingRealYouTube = false
            }
        }
    }

    // Camera box shape converter
    val activeBoxShape = when (cameraBoxShape) {
        CameraBoxShape.ROUNDED -> RoundedCornerShape(12.dp)
        CameraBoxShape.SQUARE -> RoundedCornerShape(2.dp)
        CameraBoxShape.CIRCLE -> CircleShape
    }

    // Full RTL Root Layout
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppScreenBackground())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isDark = isAppInDarkTheme()

                // ====================================================
                // 1. DEDICATED TOP VIDEO PLAYER BOX (Comfortably Lowered, Expanded Downwards & Framed)
                // (Larger immersive video view, framed cleanly inside YouTube room)
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 4.dp, end = 4.dp, bottom = 6.dp)
                        .height(265.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.5.dp, if (isDark) DarkBorder else Color(0xFFCBD5E1), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val cookieMgr = CookieManager.getInstance()
                            cookieMgr.setAcceptCookie(true)
                            object : WebView(ctx) {
                                override fun onWindowVisibilityChanged(visibility: Int) {
                                    // Keep running audio and video playback when app is minimized or backgrounded
                                    super.onWindowVisibilityChanged(View.VISIBLE)
                                }
                            }.apply {
                                cookieMgr.setAcceptThirdPartyCookies(this, true)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    allowContentAccess = true
                                    allowFileAccess = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                                }
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                                }
                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun reportTime(curr: Float, dur: Float) {
                                            currentPositionSec = curr
                                            if (dur > 0f) totalDurationSec = dur
                                        }

                                        @JavascriptInterface
                                        fun reportState(state: Int) {
                                            if (state == 1) isPlaying = true
                                            else if (state == 2) isPlaying = false
                                        }
                                    },
                                    "AndroidBridge"
                                )
                                webViewRef = this

                                val initialId = currentVideo.id.ifBlank { "dQw4w9WgXcQ" }
                                val customHtml = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                        <style>
                                            * { margin: 0; padding: 0; box-sizing: border-box; }
                                            html, body { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                            #player-container { width: 100%; height: 100%; position: absolute; top: 0; left: 0; right: 0; bottom: 0; }
                                            iframe { width: 100% !important; height: 100% !important; border: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <div id="player-container">
                                            <iframe id="player"
                                                type="text/html"
                                                src="https://www.youtube.com/embed/$initialId?autoplay=1&enablejsapi=1&playsinline=1&controls=0&fs=0&rel=0&modestbranding=1"
                                                frameborder="0"
                                                allow="autoplay; encrypted-media; picture-in-picture"
                                                allowfullscreen>
                                            </iframe>
                                        </div>
                                        <script>
                                            var playerIframe = document.getElementById('player');
                                            var currentVid = '$initialId';

                                            function sendCommand(func, args) {
                                                try {
                                                    if (playerIframe && playerIframe.contentWindow) {
                                                        playerIframe.contentWindow.postMessage(JSON.stringify({
                                                            "event": "command",
                                                            "func": func,
                                                            "args": args || []
                                                        }), "*");
                                                    }
                                                } catch(e) {}
                                            }

                                            function playVideo() {
                                                sendCommand("playVideo");
                                            }

                                            function pauseVideo() {
                                                sendCommand("pauseVideo");
                                            }

                                            function seekTo(seconds) {
                                                sendCommand("seekTo", [seconds, true]);
                                            }

                                            function setPlayerVolume(volume) {
                                                sendCommand("setVolume", [volume]);
                                            }

                                            function loadVideoById(videoId) {
                                                if (!videoId) return;
                                                currentVid = videoId;
                                                if (playerIframe) {
                                                    playerIframe.src = "https://www.youtube.com/embed/" + videoId + "?autoplay=1&enablejsapi=1&playsinline=1&controls=0&fs=0&rel=0&modestbranding=1";
                                                }
                                            }

                                            window.addEventListener("message", function(event) {
                                                try {
                                                    var data = typeof event.data === "string" ? JSON.parse(event.data) : event.data;
                                                    if (data && data.event === "onStateChange" && window.AndroidBridge) {
                                                        window.AndroidBridge.reportState(data.info);
                                                    }
                                                    if (data && data.info && typeof data.info.currentTime === 'number' && window.AndroidBridge) {
                                                        window.AndroidBridge.reportTime(data.info.currentTime, data.info.duration || 0);
                                                    }
                                                } catch(e) {}
                                            });
                                        </script>
                                    </body>
                                    </html>
                                """.trimIndent()
                                loadDataWithBaseURL("https://www.youtube.com", customHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                            val targetId = currentVideo.id.trim()
                            val vol = (videoVolume * 100).toInt()
                            webView.evaluateJavascript("""
                                (function() {
                                    if (typeof setPlayerVolume === 'function') {
                                        setPlayerVolume($vol);
                                    }
                                    if (typeof currentVid !== 'undefined' && currentVid !== '$targetId') {
                                        loadVideoById('$targetId');
                                    }
                                })();
                            """.trimIndent(), null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Balanced Centered Play/Pause Button
                    IconButton(
                        onClick = { togglePlayback() },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0x80000000), CircleShape)
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ====================================================
                // 3. SUB-TABS DOCK BAR (Slim, reduced height & thickness)
                // ====================================================
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) DarkSurface else Color.White,
                    border = BorderStroke(0.5.dp, if (isDark) DarkBorder else Color(0xFFE2EAFD)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Catalog / Player
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            YouTubeDockIconButton(
                                icon = Icons.Default.PlayCircle,
                                isActive = activeSubTab == YouTubeRoomSubTab.PLAYER,
                                isDark = isDark,
                                onClick = { activeSubTab = YouTubeRoomSubTab.PLAYER }
                            )
                        }
                        // 2. Chat
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            YouTubeDockIconButton(
                                icon = Icons.Default.ChatBubbleOutline,
                                isActive = activeSubTab == YouTubeRoomSubTab.CHAT,
                                isDark = isDark,
                                onClick = { activeSubTab = YouTubeRoomSubTab.CHAT }
                            )
                        }
                        // 3. CAMERAS (Side-by-side free layout)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            YouTubeDockIconButton(
                                icon = Icons.Default.Videocam,
                                isActive = activeSubTab == YouTubeRoomSubTab.CAMERAS,
                                isDark = isDark,
                                onClick = { activeSubTab = YouTubeRoomSubTab.CAMERAS }
                            )
                        }
                        // 4. Intercom / Walkie-Talkie
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            YouTubeDockIconButton(
                                icon = Icons.Default.RecordVoiceOver,
                                isActive = activeSubTab == YouTubeRoomSubTab.INTERCOM,
                                isDark = isDark,
                                onClick = { activeSubTab = YouTubeRoomSubTab.INTERCOM }
                            )
                        }
                        // 5. Participants / Permissions (Shows user count clearly)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            YouTubeDockIconButton(
                                icon = Icons.Default.PeopleOutline,
                                isActive = activeSubTab == YouTubeRoomSubTab.USERS,
                                badgeCount = roomUsers.size,
                                isDark = isDark,
                                onClick = { activeSubTab = YouTubeRoomSubTab.USERS }
                            )
                        }
                        // 6. Settings (Volume + Seek Controls + Play/Pause)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            YouTubeDockIconButton(
                                icon = Icons.Default.Settings,
                                isActive = activeSubTab == YouTubeRoomSubTab.SETTINGS,
                                isDark = isDark,
                                onClick = { activeSubTab = YouTubeRoomSubTab.SETTINGS }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ====================================================
                // 4. SUB-TAB CONTENT CONTAINER
                // ====================================================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeSubTab) {
                        // ----------------------------------------------------
                        // 4A. PLAYER SUB-VIEW (قائمة المقاطع المقترحة مع حقل وزر البحث المدمج بالأعلى)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.PLAYER -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Modern, Elegant Search Bar at the Top of Suggestions Container
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) DarkSurface else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2EAFD)),
                                    shadowElevation = 0.5.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(18.dp)
                                        )

                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 12.sp,
                                                fontFamily = TajawalFontFamily,
                                                color = if (isDark) DarkTextPrimary else Color(0xFF0F172A),
                                                fontWeight = FontWeight.Medium
                                            ),
                                            cursorBrush = SolidColor(Color(0xFF2563EB)),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) }),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { innerTextField ->
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = "ابحث عن أي فيديو في يوتيوب...",
                                                        fontSize = 12.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = if (isDark) DarkTextSecondary else Color(0xFF94A3B8)
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )

                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "مسح",
                                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        // Modern Search Action Button
                                        Surface(
                                            onClick = { executeSearch(searchQuery) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF2563EB)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "بحث",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                // Live Suggestions Chips (if user is typing)
                                if (liveSearchSuggestions.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(liveSearchSuggestions) { suggestion ->
                                            Surface(
                                                onClick = { executeSearch(suggestion) },
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                                                border = BorderStroke(0.5.dp, if (isDark) DarkBorder else Color(0xFFBFDBFE))
                                            ) {
                                                Text(
                                                    text = suggestion,
                                                    fontSize = 10.sp,
                                                    fontFamily = TajawalFontFamily,
                                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Search Loading Indicator
                                if (isSearchingRealYouTube) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Color(0xFF2563EB),
                                        trackColor = if (isDark) DarkBorder else Color(0xFFE2EAFD)
                                    )
                                }

                                // List of Video Catalog / Suggestions
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(videoCatalog) { video ->
                                        val isCurrent = video.id == currentVideo.id
                                        Surface(
                                            onClick = { playSelectedVideo(video) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isCurrent) (if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color(0xFFEFF6FF)) else (if (isDark) DarkSurface else Color.White),
                                            border = BorderStroke(1.dp, if (isCurrent) (if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)) else (if (isDark) DarkBorder else Color(0xFFE2EAFD))),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 75.dp, height = 50.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Black)
                                                ) {
                                                    AsyncImage(
                                                        model = video.thumbnailUrl,
                                                        contentDescription = video.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = video.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = TajawalFontFamily,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = if (isCurrent) (if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)) else (if (isDark) DarkTextPrimary else Color(0xFF0F172A))
                                                    )
                                                    Text(
                                                        text = video.channelTitle,
                                                        fontSize = 10.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = if (isDark) DarkTextSecondary else Color(0xFF64748B)
                                                    )
                                                }
                                                if (isCurrent) {
                                                    Icon(
                                                        imageVector = Icons.Default.Equalizer,
                                                        contentDescription = "مشغل الآن",
                                                        tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4B. CHAT SUB-VIEW
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.CHAT -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .imePadding()
                            ) {
                                LazyColumn(
                                    state = chatListState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(chatMessages, key = { it.id }) { msg ->
                                        if (msg.sender == "النظام") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isDark) Color(0xFF131B2E) else Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = msg.text,
                                                            fontSize = 10.sp,
                                                            fontFamily = TajawalFontFamily,
                                                            color = if (isDark) DarkTextSecondary else Color(0xFF475569)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                // In RTL: Arrangement.Start is RIGHT (Current user), Arrangement.End is LEFT (Other users)
                                                horizontalArrangement = if (msg.isMe) Arrangement.Start else Arrangement.End
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(
                                                        topStart = 14.dp,
                                                        topEnd = 14.dp,
                                                        bottomStart = if (msg.isMe) 14.dp else 3.dp,
                                                        bottomEnd = if (msg.isMe) 3.dp else 14.dp
                                                    ),
                                                    color = if (msg.isMe) Color(0xFF2563EB) else (if (isDark) DarkSurface else Color.White),
                                                    border = BorderStroke(
                                                        width = if (msg.imageUrl != null) 0.5.dp else 1.dp,
                                                        color = if (msg.isMe) {
                                                            if (msg.imageUrl != null) Color(0xFF60A5FA).copy(alpha = 0.35f) else Color(0xFF1D4ED8)
                                                        } else {
                                                            if (isDark) DarkBorder else Color(0xFFE2E8F0)
                                                        }
                                                    ),
                                                    shadowElevation = 0.5.dp,
                                                    modifier = Modifier.widthIn(max = 280.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(
                                                            horizontal = if (msg.imageUrl != null) 3.dp else 10.dp,
                                                            vertical = if (msg.imageUrl != null) 3.dp else 6.dp
                                                        )
                                                    ) {
                                                        if (!msg.isMe) {
                                                            Text(
                                                                text = msg.sender,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                                                fontFamily = TajawalFontFamily,
                                                                modifier = if (msg.imageUrl != null) Modifier.padding(horizontal = 4.dp, vertical = 2.dp) else Modifier
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                        }
                                                        if (msg.imageUrl != null) {
                                                            AsyncImage(
                                                                model = msg.imageUrl,
                                                                contentDescription = "صورة",
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .heightIn(max = 180.dp)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                            )
                                                            Spacer(modifier = Modifier.height(3.dp))
                                                        }
                                                        if (msg.text.isNotBlank() && msg.text != "📷 صورة") {
                                                            Text(
                                                                text = msg.text,
                                                                fontSize = 12.sp,
                                                                fontFamily = TajawalFontFamily,
                                                                color = if (msg.isMe) Color.White else (if (isDark) DarkTextPrimary else Color(0xFF0F172A)),
                                                                lineHeight = 16.sp,
                                                                modifier = if (msg.imageUrl != null) Modifier.padding(horizontal = 4.dp) else Modifier
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                        }
                                                        Text(
                                                            text = msg.time,
                                                            fontSize = 8.sp,
                                                            fontFamily = TajawalFontFamily,
                                                            color = if (msg.isMe) Color(0xCCFFFFFF) else (if (isDark) DarkTextSecondary else Color(0xFF94A3B8)),
                                                            modifier = Modifier
                                                                .align(if (msg.isMe) Alignment.Start else Alignment.End)
                                                                .then(if (msg.imageUrl != null) Modifier.padding(horizontal = 4.dp, vertical = 1.dp) else Modifier)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Integrated Compact Message Input Bar
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = if (isDark) DarkSurface else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val myUser = roomUsers.find { it.id == currentUserId }
                                        val isChatMuted = myUser?.isMutedChat == true

                                        // Attach Image button
                                        IconButton(
                                            onClick = {
                                                if (!isChatMuted) {
                                                    imagePickerLauncher.launch("image/*")
                                                } else {
                                                    Toast.makeText(context, "تم تقييد الدردشة لك 🔇", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(28.dp),
                                            enabled = !isChatMuted
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "صورة",
                                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Intercom / Voice button
                                        IconButton(
                                            onClick = { toggleIntercomWithPermission() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isIntercomTalking) Icons.Default.Mic else Icons.Default.MicNone,
                                                contentDescription = "صوت",
                                                tint = if (isIntercomTalking) Color(0xFF10B981) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Message Input Field
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (chatInputText.isEmpty()) {
                                                Text(
                                                    text = if (isChatMuted) "تم تقييد الدردشة لك 🔇" else "اكتب رسالة...",
                                                    fontSize = 12.sp,
                                                    fontFamily = TajawalFontFamily,
                                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                                )
                                            }
                                            BasicTextField(
                                                value = chatInputText,
                                                onValueChange = { chatInputText = it },
                                                enabled = !isChatMuted,
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    fontSize = 12.sp,
                                                    color = if (isDark) DarkTextPrimary else Color(0xFF0F172A),
                                                    fontFamily = TajawalFontFamily
                                                ),
                                                cursorBrush = SolidColor(if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                                keyboardActions = KeyboardActions(onSend = {
                                                    val text = chatInputText.trim()
                                                    if (text.isNotEmpty() && !isChatMuted) {
                                                        chatMessages.add(
                                                            YouTubeChatMessage(
                                                                id = System.currentTimeMillis().toString(),
                                                                sender = currentUserName,
                                                                text = text,
                                                                time = "الآن",
                                                                isMe = true
                                                            )
                                                        )
                                                        syncSocket.broadcastChatMessage(text)
                                                        chatInputText = ""
                                                    }
                                                }),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        // Compact Send Button (reduced size as requested)
                                        AnimatedVisibility(visible = chatInputText.trim().isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    val text = chatInputText.trim()
                                                    if (text.isNotEmpty() && !isChatMuted) {
                                                        chatMessages.add(
                                                            YouTubeChatMessage(
                                                                id = System.currentTimeMillis().toString(),
                                                                sender = currentUserName,
                                                                text = text,
                                                                time = "الآن",
                                                                isMe = true
                                                            )
                                                        )
                                                        syncSocket.broadcastChatMessage(text)
                                                        chatInputText = ""
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(Color(0xFF2563EB), CircleShape),
                                                enabled = !isChatMuted
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = "إرسال",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4C. CAMERAS SUB-VIEW (حاوية الكاميرات المستقلة الحرة لمربعات الكاميرات فقط بدون أي أشرطة أو عناوين)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.CAMERAS -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopStart
                            ) {
                                // Side-by-side Free Cameras Grid / Row (كامرة بجانب كامرة بدون عزل أو شروحات)
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1. Local User's Camera Box (Small 92dp)
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .size(cameraBoxSize.sizeDp)
                                                .clip(activeBoxShape)
                                                .background(if (isCameraActive) Color.Black else (if (isDark) DarkSurface else Color(0xFFF1F5F9)))
                                                .border(
                                                    2.dp,
                                                    if (isCameraActive) Color(0xFF2563EB) else (if (isDark) DarkBorder else Color(0xFFCBD5E1)),
                                                    activeBoxShape
                                                )
                                                .clickable {
                                                    if (isCameraActive) {
                                                        // Clicking camera flips between front and back camera
                                                        isFrontCamera = !isFrontCamera
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        syncSocket.broadcastCameraState(true, isFrontCamera)
                                                        Toast.makeText(
                                                            context,
                                                            if (isFrontCamera) "تحويل إلى الكاميرا الأمامية 🤳" else "تحويل إلى الكاميرا الخلفية 📸",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        toggleCameraWithPermission()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCameraActive) {
                                                key(isFrontCamera) {
                                                    AndroidView(
                                                        factory = { ctx ->
                                                            TextureView(ctx).apply {
                                                                val camHelper = RoomCameraHelper(ctx)
                                                                camHelper.startCamera(this, front = isFrontCamera)
                                                                this.tag = camHelper
                                                            }
                                                        },
                                                        onRelease = { view ->
                                                            (view.tag as? RoomCameraHelper)?.closeCamera()
                                                        },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                // Top-Start: Direct ON/OFF Button inside the box (زر إطفاء وتشغيل داخل المربع)
                                                IconButton(
                                                    onClick = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        toggleCameraWithPermission()
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(3.dp)
                                                        .size(24.dp)
                                                        .background(Color(0xD9DC2626), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VideocamOff,
                                                        contentDescription = "إطفاء الكاميرا",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }

                                                // Top-End: Flip indicator/button (تبديل أمامية / خلفية)
                                                IconButton(
                                                    onClick = {
                                                        isFrontCamera = !isFrontCamera
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        syncSocket.broadcastCameraState(true, isFrontCamera)
                                                        Toast.makeText(
                                                            context,
                                                            if (isFrontCamera) "تحويل إلى الكاميرا الأمامية 🤳" else "تحويل إلى الكاميرا الخلفية 📸",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(3.dp)
                                                        .size(24.dp)
                                                        .background(Color(0xB3000000), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FlipCameraAndroid,
                                                        contentDescription = "تبديل الكاميرا",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }

                                                // Bottom name tag
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xCC000000),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 3.dp)
                                                ) {
                                                    Text(
                                                        text = if (isFrontCamera) "أنت (أمامي) 🤳" else "أنت (خلفي) 📸",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            } else {
                                                // When Camera is OFF: Sleek ON Button right inside the box
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                                    modifier = Modifier.padding(4.dp)
                                                ) {
                                                    Surface(
                                                        onClick = { toggleCameraWithPermission() },
                                                        shape = CircleShape,
                                                        color = Color(0xFF10B981),
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Videocam,
                                                                contentDescription = "تشغيل الكاميرا",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "تشغيل الكاميرا",
                                                        fontSize = 9.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) DarkTextPrimary else Color(0xFF475569)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 2. All Other Room Participants' Cameras Side-by-Side (Free Row)
                                    items(roomUsers.filter { it.id != currentUserId }) { participant ->
                                        Box(
                                            modifier = Modifier
                                                .size(cameraBoxSize.sizeDp)
                                                .clip(activeBoxShape)
                                                .background(if (participant.hasCameraActive) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                                .border(
                                                    2.dp,
                                                    if (participant.isSpeaking) Color(0xFF10B981) else if (participant.hasCameraActive) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                                    activeBoxShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (participant.hasCameraActive) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .background(participant.avatarBg, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = participant.name.take(1),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                    // Live dot
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .align(Alignment.TopEnd)
                                                            .padding(2.dp)
                                                            .background(Color(0xFF10B981), CircleShape)
                                                    )
                                                }
                                            } else {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(participant.avatarBg.copy(alpha = 0.2f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = participant.name.take(1),
                                                            color = participant.avatarBg,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = "مغلقة",
                                                        fontSize = 8.sp,
                                                        color = Color(0xFF94A3B8),
                                                        fontFamily = TajawalFontFamily
                                                    )
                                                }
                                            }

                                            // Name Tag at bottom
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xCC000000),
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 3.dp)
                                            ) {
                                                Text(
                                                    text = participant.name,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4D. INTERCOM / WALKIE-TALKIE SUB-VIEW (تفعيل حقيقي بالصوت عبر Zego + مكبر صوت خارجي)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.INTERCOM -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val myUser = roomUsers.find { it.id == currentUserId }
                                val isMutedByMod = myUser?.isMutedVoice == true

                                Box(
                                    modifier = Modifier
                                        .size(135.dp)
                                        .background(
                                            if (isMutedByMod) Color(0xFF64748B) else if (isIntercomTalking) Color(0xFF10B981) else Color(0xFF2563EB),
                                            CircleShape
                                        )
                                        .border(4.dp, Color.White, CircleShape)
                                        .shadow(10.dp, CircleShape)
                                        .clickable {
                                            if (isMutedByMod) {
                                                Toast.makeText(context, "المايكروفون مكتوم من قبل المشرف 🔇", Toast.LENGTH_SHORT).show()
                                                return@clickable
                                            }
                                            isIntercomTalking = !isIntercomTalking
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            // REAL ZEGO AUDIO UNMUTE / MUTE WITH LOUDSPEAKER ROUTING
                                            ZegoCallManager.setMicrophoneMute(!isIntercomTalking)
                                            ZegoCallManager.setSpeakerEnabled(context, true)
                                            RealVoipEngine.ensureAudioCaptureStarted(context)
                                            RealVoipEngine.setMute(!isIntercomTalking)
                                            RealVoipEngine.setSpeaker(context, true)
                                            syncSocket.broadcastVoiceState(isIntercomTalking)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMutedByMod) Icons.Default.MicOff else if (isIntercomTalking) Icons.Default.Mic else Icons.Default.MicNone,
                                        contentDescription = "تحدث",
                                        tint = Color.White,
                                        modifier = Modifier.size(52.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (isMutedByMod) "المايك مكتوم من قبل المشرف 🔇" else if (isIntercomTalking) "الميكروفون مفتوح - البث مباشر لجميع الأعضاء 🎙️" else "اضغط للتحدث في الهوكي توكي الصوتي المباشر",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = if (isIntercomTalking) Color(0xFF10B981) else Color(0xFF1E3A8A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الصوت مباشر من السماعة الخارجية لجميع المتواجدين (Zego Audio Engine)",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    fontFamily = TajawalFontFamily
                                )
                            }
                        }

                        // ----------------------------------------------------
                        // 4E. USERS SUB-VIEW (عرض الأعضاء بشكل حر وأنيق بدون مربعات متداخلة)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.USERS -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(roomUsers, key = { it.id }) { user ->
                                    val isSelected = selectedUserForPermissions?.id == user.id
                                    val isMe = user.id == currentUserId
                                    val canManage = (isHost || isAppOwner) && !isMe

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedUserForPermissions = if (isSelected) null else user
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        // Free, unboxed user row: avatar + name + status
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .background(user.avatarBg, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = user.name.take(1),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    if (user.isSpeaking) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .align(Alignment.BottomEnd)
                                                                .background(Color(0xFF10B981), CircleShape)
                                                                .border(1.5.dp, if (isDark) DarkSurface else Color.White, CircleShape)
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = user.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = TajawalFontFamily,
                                                            color = if (isDark) DarkTextPrimary else Color(0xFF0F172A)
                                                        )
                                                        if (user.isSpeaking) {
                                                            Text(
                                                                text = "🎙️ يتحدث",
                                                                fontSize = 9.sp,
                                                                color = Color(0xFF10B981),
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = TajawalFontFamily
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = user.role,
                                                        fontSize = 10.sp,
                                                        color = if (isDark) DarkTextSecondary else Color(0xFF64748B),
                                                        fontFamily = TajawalFontFamily
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (user.isMutedVoice) {
                                                    Icon(Icons.Default.MicOff, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                                                }
                                                if (user.canChangeVideo) {
                                                    Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(15.dp))
                                                }
                                                if (canManage || isMe) {
                                                    Icon(
                                                        imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                        contentDescription = "خيارات",
                                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Smooth direct permission chips if expanded (no nested border boxes)
                                        AnimatedVisibility(visible = isSelected) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 6.dp, start = 48.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (canManage) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        InlinePermissionChip(
                                                            label = if (user.role.contains("مشرف")) "إلغاء الإشراف" else "ترقية لمشرف 🛡️",
                                                            icon = Icons.Default.Shield,
                                                            color = Color(0xFF2563EB),
                                                            modifier = Modifier.weight(1f),
                                                            onClick = {
                                                                val newRole = if (user.role.contains("مشرف")) "مشاهد" else "مشرف 🛡️"
                                                                val updated = user.copy(role = newRole)
                                                                val idx = roomUsers.indexOfFirst { it.id == user.id }
                                                                if (idx >= 0) roomUsers[idx] = updated
                                                                syncSocket.broadcastMemberAction(user.id, if (newRole.contains("مشرف")) "SET_MODERATOR" else "DEMOTE")
                                                                Toast.makeText(context, "تم تعديل رتبة ${user.name}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                        InlinePermissionChip(
                                                            label = if (user.canChangeVideo) "منع الفيديو 🔒" else "سماح بالفيديو 🎬",
                                                            icon = Icons.Default.SmartDisplay,
                                                            color = if (user.canChangeVideo) Color(0xFFDC2626) else Color(0xFF10B981),
                                                            modifier = Modifier.weight(1f),
                                                            onClick = {
                                                                val updated = user.copy(canChangeVideo = !user.canChangeVideo)
                                                                val idx = roomUsers.indexOfFirst { it.id == user.id }
                                                                if (idx >= 0) roomUsers[idx] = updated
                                                                syncSocket.broadcastMemberAction(user.id, if (updated.canChangeVideo) "ALLOW_VIDEO" else "RESTRICT_VIDEO")
                                                                Toast.makeText(context, "تم تعديل صلاحية الفيديو لـ ${user.name}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        InlinePermissionChip(
                                                            label = if (user.isMutedVoice) "تشغيل المايك 🎙️" else "كتم المايك 🔇",
                                                            icon = if (user.isMutedVoice) Icons.Default.Mic else Icons.Default.MicOff,
                                                            color = Color(0xFFD97706),
                                                            modifier = Modifier.weight(1f),
                                                            onClick = {
                                                                val updated = user.copy(isMutedVoice = !user.isMutedVoice)
                                                                val idx = roomUsers.indexOfFirst { it.id == user.id }
                                                                if (idx >= 0) roomUsers[idx] = updated
                                                                syncSocket.broadcastMemberAction(user.id, if (updated.isMutedVoice) "MUTE_VOICE" else "UNMUTE_VOICE")
                                                                Toast.makeText(context, "تم تغيير حالة المايكروفون", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                        InlinePermissionChip(
                                                            label = if (user.isMutedChat) "سماح بالدردشة 💬" else "حظر الدردشة 🚫",
                                                            icon = Icons.Default.ChatBubbleOutline,
                                                            color = Color(0xFF9333EA),
                                                            modifier = Modifier.weight(1f),
                                                            onClick = {
                                                                val updated = user.copy(isMutedChat = !user.isMutedChat)
                                                                val idx = roomUsers.indexOfFirst { it.id == user.id }
                                                                if (idx >= 0) roomUsers[idx] = updated
                                                                syncSocket.broadcastMemberAction(user.id, if (updated.isMutedChat) "MUTE_CHAT" else "UNMUTE_CHAT")
                                                                Toast.makeText(context, "تم تعديل صلاحية الدردشة", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                    InlinePermissionChip(
                                                        label = "طرد العضو من الغرفة 🚪",
                                                        icon = Icons.Default.ExitToApp,
                                                        color = Color(0xFFEF4444),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        onClick = {
                                                            roomUsers.removeAll { it.id == user.id }
                                                            syncSocket.broadcastMemberAction(user.id, "KICK")
                                                            selectedUserForPermissions = null
                                                            Toast.makeText(context, "تم طرد ${user.name} من الغرفة", Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                } else if (isMe) {
                                                    InlinePermissionChip(
                                                        label = "مغادرة الغرفة 🚪",
                                                        icon = Icons.Default.Logout,
                                                        color = Color(0xFFEF4444),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        onClick = { isExitConfirmDialogOpen = true }
                                                    )
                                                } else {
                                                    InlinePermissionChip(
                                                        label = "إرسال تحية 👋",
                                                        icon = Icons.Default.WavingHand,
                                                        color = Color(0xFF2563EB),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        onClick = {
                                                            val text = "مرحباً يا ${user.name}! 👋"
                                                            chatMessages.add(YouTubeChatMessage(System.currentTimeMillis().toString(), currentUserName, text, "الآن", true))
                                                            syncSocket.broadcastChatMessage(text)
                                                            selectedUserForPermissions = null
                                                            Toast.makeText(context, "تم إرسال التحية بنجاح", Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = if (isDark) DarkBorder else Color(0xFFF1F5F9),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4F. SETTINGS SUB-VIEW (معلومات الغرفة، تقديم وتأخير، هوكي توكي، كاميرات، وزر الخروج)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.SETTINGS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Room Name & Identity Card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) DarkSurface else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.MeetingRoom,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = roomTitle,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = if (isDark) DarkTextPrimary else Color(0xFF0F172A)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isHost) (if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                            ) {
                                                Text(
                                                    text = if (isHost) "مضيف الغرفة 👑" else "عضو متصل 👤",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = if (isHost) Color(0xFF2563EB) else (if (isDark) DarkTextSecondary else Color(0xFF64748B)),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "معرّف الغرفة: $roomId • كود الانضمام: $roomCode",
                                            fontSize = 10.sp,
                                            fontFamily = TajawalFontFamily,
                                            color = if (isDark) DarkTextSecondary else Color(0xFF64748B)
                                        )
                                    }
                                }

                                // 2. Video Playback & Seek Controls (تقديم وتأخير وايقاف متزامن)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) DarkSurface else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.FastForward,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "التحكم في تشغيل وتقديم وتأخير الفيديو",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = if (isDark) DarkTextPrimary else Color(0xFF0F172A)
                                                )
                                            }
                                            Text(
                                                text = "${formatTime(currentPositionSec)} / ${formatTime(totalDurationSec)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }

                                        // Seek Slider
                                        Slider(
                                            value = currentPositionSec.coerceIn(0f, totalDurationSec.coerceAtLeast(1f)),
                                            onValueChange = { currentPositionSec = it },
                                            onValueChangeFinished = { performSeek(currentPositionSec) },
                                            valueRange = 0f..totalDurationSec.coerceAtLeast(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFFDC2626),
                                                activeTrackColor = Color(0xFFDC2626),
                                                inactiveTrackColor = if (isDark) DarkBorder else Color(0xFFFEE2E2)
                                            )
                                        )

                                        // Playback Action Buttons Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Rewind 10s
                                            PlaybackControlButton(
                                                icon = Icons.Default.Replay10,
                                                label = "-10 ثوانٍ",
                                                color = if (isDark) DarkTextPrimary else Color(0xFF475569),
                                                onClick = { performSeek(currentPositionSec - 10f) }
                                            )

                                            // Play / Pause Toggle
                                            PlaybackControlButton(
                                                icon = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                                label = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                                color = if (isPlaying) Color(0xFFDC2626) else Color(0xFF10B981),
                                                isLarge = true,
                                                onClick = { togglePlayback() }
                                            )

                                            // Fast Forward 10s
                                            PlaybackControlButton(
                                                icon = Icons.Default.Forward10,
                                                label = "+10 ثوانٍ",
                                                color = if (isDark) DarkTextPrimary else Color(0xFF475569),
                                                onClick = { performSeek(currentPositionSec + 10f) }
                                            )

                                            // Restart from beginning
                                            PlaybackControlButton(
                                                icon = Icons.Default.Replay,
                                                label = "من البداية",
                                                color = Color(0xFF2563EB),
                                                onClick = { performSeek(0f) }
                                            )
                                        }

                                        // Volume Slider
                                        Column(modifier = Modifier.padding(top = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(
                                                        imageVector = if (videoVolume == 0f) Icons.Default.VolumeOff else if (videoVolume > 0.5f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                                        contentDescription = null,
                                                        tint = Color(0xFF2563EB),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "مستوى صوت فيديو يوتيوب",
                                                        fontSize = 11.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = if (isDark) DarkTextPrimary else Color(0xFF0F172A)
                                                    )
                                                }
                                                Text(
                                                    text = "${(videoVolume * 100).toInt()}%",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2563EB)
                                                )
                                            }
                                            Slider(
                                                value = videoVolume,
                                                onValueChange = { videoVolume = it },
                                                valueRange = 0f..1f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFF2563EB),
                                                    activeTrackColor = Color(0xFF2563EB),
                                                    inactiveTrackColor = if (isDark) DarkBorder else Color(0xFFE2EAFD)
                                                )
                                            )
                                        }

                                        // Sync banner
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isDark) Color(0xFF052E16) else Color(0xFFF0FDF4),
                                            border = BorderStroke(1.dp, if (isDark) Color(0xFF14532D) else Color(0xFFBBF7D0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = Color(0xFF16A34A),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "المزامنة الحقيقية نشطة: أي تقديم أو تأخير أو إيقاف يطبق فوراً على جميع المتواجدين.",
                                                    fontSize = 10.sp,
                                                    fontFamily = TajawalFontFamily,
                                                    color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. Professional Walkie-Talkie Audio Settings Card (إعدادات صوت الهوكي توكي الاحترافية)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) DarkSurface else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.RecordVoiceOver,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "إعدادات صوت الهوكي توكي الاحترافية 🎙️",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = if (isDark) DarkTextPrimary else Color(0xFF0F172A)
                                            )
                                        }

                                        // AI Noise Suppression Switch
                                        SettingsSwitchRow(
                                            title = "إلغاء الضوضاء الذكي (AI Noise Suppression)",
                                            subtitle = "تصفية صوت الرياح والتشويش المحيط بالميكروفون",
                                            checked = isNoiseSuppressionEnabled,
                                            isDark = isDark,
                                            onCheckedChange = {
                                                isNoiseSuppressionEnabled = it
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(context, if (it) "تم تفعيل إلغاء الضوضاء" else "تم تعطيل إلغاء الضوضاء", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        // Echo Cancellation Switch
                                        SettingsSwitchRow(
                                            title = "مانع الصدى الصوتي (Echo Cancellation - AEC)",
                                            subtitle = "منع ارتداد صوت الفيديو داخل الميكروفون",
                                            checked = isEchoCancellationEnabled,
                                            isDark = isDark,
                                            onCheckedChange = {
                                                isEchoCancellationEnabled = it
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(context, if (it) "تم تفعيل مانع الصدى" else "تم تعطيل مانع الصدى", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        // Auto Gain Control Switch
                                        SettingsSwitchRow(
                                            title = "التحكم التلقائي بمستوى الصوت (Auto Gain Control)",
                                            subtitle = "موازنة علو وانخفاض صوت المتحدثين تلقائياً",
                                            checked = isAutoGainControlEnabled,
                                            isDark = isDark,
                                            onCheckedChange = {
                                                isAutoGainControlEnabled = it
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )

                                        // Loudspeaker Routing Switch
                                        SettingsSwitchRow(
                                            title = "إخراج الصوت عبر مكبر الصوت الخارجي",
                                            subtitle = "صوت الهوكي توكي يخرج من مكبر الصوت الرئيسي",
                                            checked = isIntercomLoudspeaker,
                                            isDark = isDark,
                                            onCheckedChange = {
                                                isIntercomLoudspeaker = it
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                ZegoCallManager.setSpeakerEnabled(context, it)
                                                RealVoipEngine.setSpeaker(context, it)
                                                Toast.makeText(context, if (it) "مكبر الصوت الخارجي نشط" else "سماعة الأذن نشطة", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        // Push-To-Talk vs Open Mic
                                        SettingsSwitchRow(
                                            title = "وضع الضغط للتحدث (Push-To-Talk)",
                                            subtitle = "التحدث فقط عند الاستمرار بالضغط، أو المايك المفتوح بنقرة واحدة",
                                            checked = isPushToTalkMode,
                                            isDark = isDark,
                                            onCheckedChange = {
                                                isPushToTalkMode = it
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )

                                        // Mic Sensitivity Slider
                                        Column(modifier = Modifier.padding(top = 2.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "حساسية وقوة التقاط الميكروفون",
                                                    fontSize = 11.sp,
                                                    fontFamily = TajawalFontFamily,
                                                    color = if (isDark) DarkTextPrimary else Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "${(micSensitivity * 100).toInt()}%",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                            Slider(
                                                value = micSensitivity,
                                                onValueChange = { micSensitivity = it },
                                                valueRange = 0.5f..1.5f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFF10B981),
                                                    activeTrackColor = Color(0xFF10B981),
                                                    inactiveTrackColor = if (isDark) DarkBorder else Color(0xFFD1FAE5)
                                                )
                                            )
                                        }
                                    }
                                }

                                // 4. Camera Settings Card (ضبط الكاميرات والبث المرئي)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) DarkSurface else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "إعدادات الكاميرات والبث المرئي 📷",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = if (isDark) DarkTextPrimary else Color(0xFF0F172A)
                                            )
                                        }

                                        // Camera toggle button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                onClick = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    toggleCameraWithPermission()
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isCameraActive) Color(0xFFEF4444) else Color(0xFF10B981),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCameraActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isCameraActive) "إيقاف كاميرتي" else "فتح كاميرتي",
                                                        fontSize = 11.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            Surface(
                                                onClick = {
                                                    if (isCameraActive) {
                                                        isFrontCamera = !isFrontCamera
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        syncSocket.broadcastCameraState(true, isFrontCamera)
                                                        Toast.makeText(context, if (isFrontCamera) "كاميرا أمامية 🤳" else "كاميرا خلفية 📸", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "يرجى فتح الكاميرا أولاً", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                                border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFCBD5E1)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FlipCameraAndroid,
                                                        contentDescription = null,
                                                        tint = if (isDark) DarkTextPrimary else Color(0xFF334155),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isFrontCamera) "أمامية 🤳" else "خلفية 📸",
                                                        fontSize = 11.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) DarkTextPrimary else Color(0xFF334155)
                                                    )
                                                }
                                            }
                                        }

                                        // Camera box shape switcher
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "شكل إطار مربعات الكاميرات:",
                                                fontSize = 11.sp,
                                                fontFamily = TajawalFontFamily,
                                                color = if (isDark) DarkTextPrimary else Color(0xFF0F172A)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                CameraBoxShape.values().forEach { shape ->
                                                    val isShapeSelected = cameraBoxShape == shape
                                                    Surface(
                                                        onClick = { cameraBoxShape = shape },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isShapeSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                                        border = BorderStroke(1.dp, if (isShapeSelected) Color(0xFF2563EB) else (if (isDark) DarkBorder else Color(0xFFCBD5E1)))
                                                    ) {
                                                        Text(
                                                            text = when (shape) {
                                                                CameraBoxShape.ROUNDED -> "حواف ناعمة"
                                                                CameraBoxShape.SQUARE -> "مربع"
                                                                CameraBoxShape.CIRCLE -> "دائري"
                                                            },
                                                            fontSize = 10.sp,
                                                            fontFamily = TajawalFontFamily,
                                                            color = if (isShapeSelected) Color.White else (if (isDark) DarkTextPrimary else Color(0xFF334155)),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 5. Exit Room Action Card (زر مغادرة / إنهاء الغرفة في حاوية الضبط)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) DarkSurface else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFFECACA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Logout,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (isHost) "إنهاء الغرفة للمشرف" else "مغادرة الغرفة",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                        Text(
                                            text = if (isHost)
                                                "بصفتك المضيف، سيؤدي خروجك إلى إغلاق الغرفة وإنهاء المشاهدة المتزامنة لجميع المشاركين."
                                            else
                                                "يمكنك مغادرة الغرفة والعودة في أي وقت عبر رابط أو كود الغرفة.",
                                            fontSize = 10.sp,
                                            fontFamily = TajawalFontFamily,
                                            color = if (isDark) DarkTextSecondary else Color(0xFF64748B)
                                        )

                                        Surface(
                                            onClick = { isExitConfirmDialogOpen = true },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFDC2626),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ExitToApp,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isHost) "إنهاء الغرفة وإغلاقها للجميع 🚪" else "مغادرة الغرفة الآن 🚪",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Real YouTube Search Results Modal
            if (isSearchModalOpen) {
                Dialog(
                    onDismissRequest = { isSearchModalOpen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                                    Text(
                                        text = "نتائج بحث يوتيوب المباشرة 🎬",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                IconButton(onClick = { isSearchModalOpen = false }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "ابحث في يوتيوب...",
                                        fontSize = 12.sp,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF64748B)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Medium
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { executeSearch(searchQuery) }) {
                                        Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) })
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isSearchingRealYouTube) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF2563EB))
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(realSearchResults, key = { it.id }) { item ->
                                        Surface(
                                            onClick = { playSelectedVideo(item) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 90.dp, height = 60.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color.Black)
                                                ) {
                                                    AsyncImage(
                                                        model = item.thumbnailUrl,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = item.channelTitle,
                                                        fontSize = 10.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF475569)
                                                    )
                                                }
                                                Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ====================================================
            // EXIT ROOM CONFIRMATION DIALOG (Modern, Theme-Matching)
            // ====================================================
            if (isExitConfirmDialogOpen) {
                Dialog(
                    onDismissRequest = { isExitConfirmDialogOpen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 10.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color(0xFFFEF2F2), CircleShape)
                                    .border(1.dp, Color(0xFFFECACA), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Text(
                                text = if (isHost) "هل تريد الخروج وإنهاء الغرفة؟" else "هل تود الخروج من الغرفة؟",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (isHost)
                                    "سيتم إغلاق الغرفة للجميع وإنهاء المشاهدة المتزامنة."
                                else
                                    "يمكنك العودة للانضمام إلى الغرفة لاحقاً.",
                                fontSize = 11.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { isExitConfirmDialogOpen = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF1F5F9),
                                        contentColor = Color(0xFF334155)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = "لا",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily
                                    )
                                }

                                Button(
                                    onClick = {
                                        isExitConfirmDialogOpen = false
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isHost) {
                                            YouTubeRoomManager.deleteRoomLocally(context, roomId)
                                        }
                                        syncSocket.disconnect()
                                        ZegoCallManager.endCall(context, zegoAudioRoomId)
                                        onBack()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = "نعم",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// HELPER COMPOSABLE SUB-COMPONENTS
// ----------------------------------------------------
@Composable
private fun InlinePermissionChip(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                color = color
            )
        }
    }
}

@Composable
private fun YouTubeDockIconButton(
    icon: ImageVector,
    isActive: Boolean,
    badgeCount: Int? = null,
    isDark: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color(0xFF2563EB) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                modifier = Modifier.size(if (isActive) 21.dp else 19.dp)
            )
        }
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp)
                    .background(Color(0xFFEF4444), RoundedCornerShape(8.dp))
                    .border(1.dp, if (isDark) DarkSurface else Color.White, RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = "$badgeCount",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PlaybackControlButton(
    icon: ImageVector,
    label: String,
    color: Color,
    isLarge: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(if (isLarge) 46.dp else 36.dp)
                .background(color.copy(alpha = 0.12f), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(if (isLarge) 28.dp else 20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = TajawalFontFamily,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    isDark: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                color = if (isDark) DarkTextPrimary else Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                fontFamily = TajawalFontFamily,
                color = if (isDark) DarkTextSecondary else Color(0xFF64748B)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color(0xFFCBD5E1),
                uncheckedTrackColor = if (isDark) DarkBorder else Color(0xFFE2EAFD)
            )
        )
    }
}

