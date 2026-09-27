package com.ps1.netplay.ui.compose

import android.content.Context
import android.media.AudioManager
import android.view.TextureView
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    SMALL(105.dp, "صغير"),
    MEDIUM(145.dp, "متوسط"),
    LARGE(190.dp, "كبير")
}

enum class CameraBoxShape(val label: String) {
    ROUNDED("حواف مستديرة"),
    SQUARE("مربع"),
    CIRCLE("دائري")
}

enum class RoomPrivacyMode {
    PUBLIC,
    FRIENDS,
    INVITE_ONLY,
    ONLY_ME
}

data class YouTubeVideoItem(
    val id: String,
    val title: String,
    val channelTitle: String,
    val duration: String,
    val viewCount: String,
    val publishedTime: String,
    val thumbnailUrl: String,
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
    val avatarColor: Color = Color(0xFF2563EB)
)

data class YouTubeRoomUser(
    val id: String,
    val name: String,
    var role: String,
    val isHost: Boolean = false,
    val isOnline: Boolean = true,
    val isSpeaking: Boolean = false,
    val hasCameraActive: Boolean = false,
    val avatarBg: Color = Color(0xFF2563EB),
    val canChangeVideo: Boolean = false,
    val isMutedVoice: Boolean = false,
    val isMutedChat: Boolean = false
)

data class VideoChangeRequest(
    val requesterId: String,
    val requesterName: String,
    val videoId: String,
    val videoTitle: String
)

// Initial catalog
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

// Helper function to format seconds as MM:SS
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
    var activeSubTab by remember { mutableStateOf(YouTubeRoomSubTab.PLAYER) }

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

    // Walkie-Talkie Intercom State (Real Zego Engine)
    var isIntercomTalking by remember { mutableStateOf(false) }

    // Cameras Configuration & State
    var isCameraActive by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var cameraBoxSize by remember { mutableStateOf(CameraBoxSize.MEDIUM) }
    var cameraBoxShape by remember { mutableStateOf(CameraBoxShape.ROUNDED) }
    var isCameraSettingsExpanded by remember { mutableStateOf(false) }

    // Dialog & Permission States
    var pendingVideoChangeRequest by remember { mutableStateOf<VideoChangeRequest?>(null) }
    var selectedUserForPermissions by remember { mutableStateOf<YouTubeRoomUser?>(null) }

    // Room Participants state
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
                        avatarBg = Color(0xFF2563EB),
                        canChangeVideo = isHost || isAppOwner
                    )
                )
            }
            add(
                YouTubeRoomUser(
                    id = "u_sarah",
                    name = "سارة",
                    role = "مشرفة 🛡️",
                    isHost = false,
                    isOnline = true,
                    isSpeaking = false,
                    hasCameraActive = true,
                    avatarBg = Color(0xFF10B981),
                    canChangeVideo = true
                )
            )
            add(
                YouTubeRoomUser(
                    id = "u_guest_vip",
                    name = "محمد علي",
                    role = "مشاهد",
                    isHost = false,
                    isOnline = true,
                    isSpeaking = false,
                    hasCameraActive = false,
                    avatarBg = Color(0xFF8B5CF6),
                    canChangeVideo = false
                )
            )
        }
    }

    // Keep camera status in roomUsers updated
    LaunchedEffect(isCameraActive) {
        val idx = roomUsers.indexOfFirst { it.id == currentUserId }
        if (idx >= 0) {
            roomUsers[idx] = roomUsers[idx].copy(hasCameraActive = isCameraActive)
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

    // Auto-scroll chat to bottom on new message
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
                Toast.makeText(context, "تم تغيير الفيديو للغرفة: ${vTitle.take(30)} 🎬", Toast.LENGTH_SHORT).show()
            },
            onPlaybackStateReceived = { playState, pos ->
                isPlaying = playState
                if (pos >= 0f) {
                    currentPositionSec = pos
                    webViewRef?.evaluateJavascript("if (typeof seekTo === 'function') { seekTo($pos); }", null)
                }
                if (playState) {
                    webViewRef?.evaluateJavascript("if (typeof playVideo === 'function') { playVideo(); }", null)
                } else {
                    webViewRef?.evaluateJavascript("if (typeof pauseVideo === 'function') { pauseVideo(); }", null)
                }
            },
            onChatMessageReceived = { newMsg ->
                chatMessages.add(newMsg)
            },
            onStateRequested = { client ->
                // When newcomer joins, reply with current video and playback state
                client.broadcastVideoChange(currentVideo.id, currentVideo.title)
                client.broadcastPlaybackState(isPlaying, currentPositionSec)
            },
            onVideoChangeRequested = { reqId, reqName, vId, vTitle ->
                if (isHost || isAppOwner) {
                    pendingVideoChangeRequest = VideoChangeRequest(reqId, reqName, vId, vTitle)
                }
            },
            onVideoChangeRequestRejected = {
                Toast.makeText(context, "تم رفض طلب تغيير الفيديو من قبل مضيف الغرفة ❌", Toast.LENGTH_SHORT).show()
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
            }
        )
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

    // REAL ZEGO WALKIE-TALKIE AUDIO ROOM INITIALIZATION
    val zegoAudioRoomId = remember(roomId) { "yt_audio_${roomId.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)}" }
    LaunchedEffect(zegoAudioRoomId) {
        ZegoCallManager.startCall(
            context = context,
            roomId = zegoAudioRoomId,
            userId = currentUserId,
            userName = currentUserName,
            isVideo = false,
            isOutgoing = true,
            onConnected = {
                ZegoCallManager.setMicrophoneMute(true)
            }
        )
    }

    // Periodic Heartbeat Sync: Host synchronizes playhead every 6 seconds to prevent any drift
    LaunchedEffect(isPlaying, isHost, isAppOwner) {
        if ((isHost || isAppOwner) && isPlaying) {
            while (true) {
                delay(6000)
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
            Toast.makeText(context, "التحكم في تقديم وإيقاف الفيديو مخصص لمضيف الغرفة والمشرفين 🔒", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "التحكم في تشغيل وإيقاف الفيديو مخصص لمضيف الغرفة والمشرفين 🔒", Toast.LENGTH_SHORT).show()
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

    // Search Bar States
    var searchQuery by remember { mutableStateOf("") }
    var isDropdownOpen by remember { mutableStateOf(false) }
    var isSearchModalOpen by remember { mutableStateOf(false) }
    var isSearchingRealYouTube by remember { mutableStateOf(false) }
    val liveSearchSuggestions = remember { mutableStateListOf<String>() }
    val realSearchResults = remember { mutableStateListOf<YouTubeVideoItem>() }

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

    fun playSelectedVideo(video: YouTubeVideoItem) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val myUser = roomUsers.find { it.id == currentUserId }
        val canChangeDirectly = isHost || isAppOwner || (myUser?.canChangeVideo == true)

        if (!canChangeDirectly) {
            syncSocket.requestVideoChange(video.id, video.title)
            Toast.makeText(context, "تم إرسال طلب تشغيل الفيديو إلى مضيف الغرفة للموافقة... ⏳", Toast.LENGTH_LONG).show()
            isSearchModalOpen = false
            isDropdownOpen = false
            return
        }

        currentVideo = video
        isPlaying = true
        currentPositionSec = 0f
        isSearchModalOpen = false
        isDropdownOpen = false
        YouTubeRoomManager.updateRoomVideo(context, roomId, video.id, video.title)
        syncSocket.broadcastVideoChange(video.id, video.title)
        Toast.makeText(context, "جاري تشغيل: ${video.title.take(35)}... 🎬", Toast.LENGTH_SHORT).show()
    }

    fun executeSearch(query: String) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val cleanQuery = query.trim()
        searchQuery = cleanQuery
        isDropdownOpen = false
        isSearchModalOpen = true

        if (cleanQuery.isNotEmpty()) {
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
        CameraBoxShape.ROUNDED -> RoundedCornerShape(16.dp)
        CameraBoxShape.SQUARE -> RoundedCornerShape(4.dp)
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
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ====================================================
                // 1. TOP HEADER & COMPACT SLEEK SEARCH BAR
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Compact Sleek Search Input container
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "ابحث في يوتيوب...",
                                    fontSize = 11.sp,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    isDropdownOpen = it.trim().isNotEmpty()
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A),
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(Color(0xFFDC2626)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    isDropdownOpen = false
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Search Button
                    IconButton(
                        onClick = {
                            if (searchQuery.trim().isNotEmpty()) {
                                executeSearch(searchQuery)
                            } else {
                                isSearchModalOpen = true
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFDC2626), CircleShape)
                            .shadow(1.dp, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Live Participant Count Chip
                    Surface(
                        onClick = { activeSubTab = YouTubeRoomSubTab.USERS },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color(0xFF4338CA),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${roomUsers.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4338CA)
                            )
                        }
                    }

                    // Room Code Chip
                    Surface(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("كود الغرفة", roomCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود الغرفة ($roomCode)", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = roomCode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                        )
                    }
                }

                // Autocomplete Suggestions Floating Dropdown
                if (isDropdownOpen && liveSearchSuggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            liveSearchSuggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { executeSearch(suggestion) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = suggestion,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.NorthWest,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ====================================================
                // 2. COMPLETELY CLEAN REAL YOUTUBE VIDEO PLAYER BOX
                // (No overlay buttons or clutter, pure edge-to-edge video)
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(245.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    key(currentVideo.id) {
                        AndroidView(
                            factory = { ctx ->
                                CookieManager.getInstance().setAcceptCookie(true)
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                                    }
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                                    }
                                    // Register Bridge for synchronized playback tracking
                                    addJavascriptInterface(
                                        object {
                                            @JavascriptInterface
                                            fun reportTime(curr: Float, dur: Float) {
                                                currentPositionSec = curr
                                                if (dur > 0f) totalDurationSec = dur
                                            }

                                            @JavascriptInterface
                                            fun reportState(state: Int) {
                                                // 1 = playing, 2 = paused
                                                if (state == 1) isPlaying = true
                                                else if (state == 2) isPlaying = false
                                            }
                                        },
                                        "AndroidBridge"
                                    )
                                    webViewRef = this

                                    val safeVideoId = currentVideo.id
                                    val customHtml = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                            <style>
                                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                                html, body { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                                #player-container { width: 100vw; height: 100vh; pointer-events: none; }
                                                iframe { width: 100% !important; height: 100% !important; border: none; }
                                            </style>
                                        </head>
                                        <body>
                                            <div id="player-container"><div id="player"></div></div>
                                            <script src="https://www.youtube.com/iframe_api"></script>
                                            <script>
                                                var player;
                                                function onYouTubeIframeAPIReady() {
                                                    player = new YT.Player('player', {
                                                        videoId: '$safeVideoId',
                                                        playerVars: {
                                                            'autoplay': 1,
                                                            'controls': 0,
                                                            'playsinline': 1,
                                                            'rel': 0,
                                                            'modestbranding': 1,
                                                            'enablejsapi': 1,
                                                            'disablekb': 1,
                                                            'fs': 0,
                                                            'iv_load_policy': 3
                                                        },
                                                        events: {
                                                            'onReady': function(e) {
                                                                e.target.setVolume(${ (videoVolume * 100).toInt() });
                                                                e.target.playVideo();
                                                            },
                                                            'onStateChange': function(e) {
                                                                if (window.AndroidBridge && window.AndroidBridge.reportState) {
                                                                    window.AndroidBridge.reportState(e.data);
                                                                }
                                                            }
                                                        }
                                                    });
                                                    setInterval(function() {
                                                        if (player && typeof player.getCurrentTime === 'function' && typeof player.getDuration === 'function') {
                                                            if (window.AndroidBridge && window.AndroidBridge.reportTime) {
                                                                window.AndroidBridge.reportTime(player.getCurrentTime(), player.getDuration());
                                                            }
                                                        }
                                                    }, 500);
                                                }
                                                function playVideo() { if (player && player.playVideo) player.playVideo(); }
                                                function pauseVideo() { if (player && player.pauseVideo) player.pauseVideo(); }
                                                function seekTo(sec) { if (player && player.seekTo) player.seekTo(sec, true); }
                                                function setPlayerVolume(vol) { if (player && player.setVolume) player.setVolume(vol); }
                                            </script>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    loadDataWithBaseURL("https://www.google.com", customHtml, "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                webViewRef = webView
                                val vol = (videoVolume * 100).toInt()
                                webView.evaluateJavascript("if (typeof setPlayerVolume === 'function') { setPlayerVolume($vol); }", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ====================================================
                // 3. SUB-TABS DOCK BAR (With Cameras replacing Live Sync)
                // ====================================================
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Catalog / Player
                        YouTubeDockIconButton(
                            icon = Icons.Default.PlayCircle,
                            isActive = activeSubTab == YouTubeRoomSubTab.PLAYER,
                            onClick = { activeSubTab = YouTubeRoomSubTab.PLAYER }
                        )
                        // 2. Chat
                        YouTubeDockIconButton(
                            icon = Icons.Default.ChatBubbleOutline,
                            isActive = activeSubTab == YouTubeRoomSubTab.CHAT,
                            onClick = { activeSubTab = YouTubeRoomSubTab.CHAT }
                        )
                        // 3. CAMERAS (Replaces broadcast sync)
                        YouTubeDockIconButton(
                            icon = Icons.Default.Videocam,
                            isActive = activeSubTab == YouTubeRoomSubTab.CAMERAS,
                            onClick = { activeSubTab = YouTubeRoomSubTab.CAMERAS }
                        )
                        // 4. Intercom / Walkie-Talkie
                        YouTubeDockIconButton(
                            icon = Icons.Default.RecordVoiceOver,
                            isActive = activeSubTab == YouTubeRoomSubTab.INTERCOM,
                            onClick = { activeSubTab = YouTubeRoomSubTab.INTERCOM }
                        )
                        // 5. Participants / Permissions
                        YouTubeDockIconButton(
                            icon = Icons.Default.PeopleOutline,
                            isActive = activeSubTab == YouTubeRoomSubTab.USERS,
                            badgeCount = roomUsers.size,
                            onClick = { activeSubTab = YouTubeRoomSubTab.USERS }
                        )
                        // 6. Settings (With Volume + Seek Controls + Play/Pause)
                        YouTubeDockIconButton(
                            icon = Icons.Default.Settings,
                            isActive = activeSubTab == YouTubeRoomSubTab.SETTINGS,
                            onClick = { activeSubTab = YouTubeRoomSubTab.SETTINGS }
                        )
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
                        // 4A. PLAYER SUB-VIEW (قائمة المقاطع المقترحة)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.PLAYER -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(videoCatalog) { video ->
                                    val isCurrent = video.id == currentVideo.id
                                    Surface(
                                        onClick = { playSelectedVideo(video) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isCurrent) Color(0xFFEFF6FF) else Color.White,
                                        border = BorderStroke(1.dp, if (isCurrent) Color(0xFF2563EB) else Color(0xFFE2EAFD)),
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
                                                    color = if (isCurrent) Color(0xFF2563EB) else Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = video.channelTitle,
                                                    fontSize = 10.sp,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.Equalizer,
                                                    contentDescription = "مشغل الآن",
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(16.dp)
                                                )
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (msg.isMe) Color(0xFF2563EB) else Color.White,
                                                border = BorderStroke(1.dp, if (msg.isMe) Color(0xFF2563EB) else Color(0xFFE2EAFD))
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = msg.sender,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (msg.isMe) Color(0xFFBFDBFE) else Color(0xFF2563EB),
                                                        fontFamily = TajawalFontFamily
                                                    )
                                                    Text(
                                                        text = msg.text,
                                                        fontSize = 12.sp,
                                                        color = if (msg.isMe) Color.White else Color(0xFF0F172A),
                                                        fontFamily = TajawalFontFamily
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val myUser = roomUsers.find { it.id == currentUserId }
                                    val isChatMuted = myUser?.isMutedChat == true

                                    OutlinedTextField(
                                        value = chatInputText,
                                        onValueChange = { chatInputText = it },
                                        placeholder = {
                                            Text(
                                                text = if (isChatMuted) "تم تقييد الدردشة لك 🔇" else "اكتب رسالة...",
                                                fontSize = 11.sp,
                                                fontFamily = TajawalFontFamily
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(18.dp),
                                        singleLine = true,
                                        enabled = !isChatMuted,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        )
                                    )
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
                                            .size(40.dp)
                                            .background(Color(0xFF2563EB), CircleShape),
                                        enabled = !isChatMuted
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4C. CAMERAS SUB-VIEW (مربعات الكاميرات المباشرة للأطراف + ضبط الحجم والشكل)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.CAMERAS -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Top Action & Customization Bar
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Videocam,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "كاميرات الغرفة المباشرة",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF1E3A8A)
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Settings toggle button
                                                IconButton(
                                                    onClick = { isCameraSettingsExpanded = !isCameraSettingsExpanded },
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(if (isCameraSettingsExpanded) Color(0xFFEEF2FF) else Color(0xFFF1F5F9), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Tune,
                                                        contentDescription = "ضبط الكاميرات",
                                                        tint = Color(0xFF2563EB),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }

                                                // Toggle my camera button
                                                Button(
                                                    onClick = {
                                                        isCameraActive = !isCameraActive
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isCameraActive) Color(0xFFEF4444) else Color(0xFF10B981)
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCameraActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isCameraActive) "إيقاف كاميرتي" else "تشغيل كاميرتي",
                                                        fontSize = 10.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }

                                        // Expandable Camera Appearance Settings
                                        AnimatedVisibility(visible = isCameraSettingsExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                            ) {
                                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Size Selection
                                                Text(
                                                    text = "حجم مربع الكاميرا:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF64748B)
                                                )
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    CameraBoxSize.values().forEach { size ->
                                                        val selected = cameraBoxSize == size
                                                        Surface(
                                                            onClick = { cameraBoxSize = size },
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (selected) Color(0xFF2563EB) else Color(0xFFF8FAFC),
                                                            border = BorderStroke(1.dp, if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text(
                                                                text = size.label,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = TajawalFontFamily,
                                                                color = if (selected) Color.White else Color(0xFF334155),
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Shape Selection
                                                Text(
                                                    text = "شكل مربع الكاميرا:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF64748B)
                                                )
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    CameraBoxShape.values().forEach { shape ->
                                                        val selected = cameraBoxShape == shape
                                                        Surface(
                                                            onClick = { cameraBoxShape = shape },
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (selected) Color(0xFF2563EB) else Color(0xFFF8FAFC),
                                                            border = BorderStroke(1.dp, if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text(
                                                                text = shape.label,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = TajawalFontFamily,
                                                                color = if (selected) Color.White else Color(0xFF334155),
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Camera Boxes Grid
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Row 1: Local User's Camera Box
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "كاميرتك ($currentUserName) 👤",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF1E3A8A)
                                                    )
                                                    Text(
                                                        text = if (isCameraActive) "انقر على المربع لقلب الكاميرا 🔄" else "الكاميرا متوقفة",
                                                        fontSize = 10.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = if (isCameraActive) Color(0xFF10B981) else Color(0xFF94A3B8)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // The Camera Frame
                                                Box(
                                                    modifier = Modifier
                                                        .size(cameraBoxSize.sizeDp)
                                                        .clip(activeBoxShape)
                                                        .background(if (isCameraActive) Color.Black else Color(0xFFF1F5F9))
                                                        .border(
                                                            2.dp,
                                                            if (isCameraActive) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                            activeBoxShape
                                                        )
                                                        .clickable {
                                                            if (isCameraActive) {
                                                                isFrontCamera = !isFrontCamera
                                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                Toast.makeText(
                                                                    context,
                                                                    if (isFrontCamera) "تم التحويل للكاميرا الأمامية 🤳" else "تم التحويل للكاميرا الخلفية 📸",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            } else {
                                                                isCameraActive = true
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
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }

                                                        // Overlay buttons for Quick Flip & Quick Close
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .align(Alignment.TopCenter)
                                                                .padding(4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            // Flip button
                                                            IconButton(
                                                                onClick = {
                                                                    isFrontCamera = !isFrontCamera
                                                                },
                                                                modifier = Modifier
                                                                    .size(26.dp)
                                                                    .background(Color(0x99000000), CircleShape)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.FlipCameraAndroid,
                                                                    contentDescription = "تبديل",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }

                                                            // Turn off button
                                                            IconButton(
                                                                onClick = { isCameraActive = false },
                                                                modifier = Modifier
                                                                    .size(26.dp)
                                                                    .background(Color(0x99000000), CircleShape)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "إيقاف",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }

                                                        // Bottom Tag
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color(0xAA000000),
                                                            modifier = Modifier
                                                                .align(Alignment.BottomCenter)
                                                                .padding(bottom = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = if (isFrontCamera) "أمامية 🤳" else "خلفية 📸",
                                                                fontSize = 9.sp,
                                                                color = Color.White,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.CameraAlt,
                                                                contentDescription = null,
                                                                tint = Color(0xFF94A3B8),
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                            Text(
                                                                text = "اضغط للتشغيل",
                                                                fontSize = 10.sp,
                                                                fontFamily = TajawalFontFamily,
                                                                color = Color(0xFF64748B)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Other Participants' Camera Boxes
                                    item {
                                        Text(
                                            text = "كاميرات بقية المتواجدين (${roomUsers.filter { it.id != currentUserId }.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF1E3A8A),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }

                                    items(roomUsers.filter { it.id != currentUserId }) { participant ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Participant Camera Card
                                                Box(
                                                    modifier = Modifier
                                                        .size(cameraBoxSize.sizeDp)
                                                        .clip(activeBoxShape)
                                                        .background(if (participant.hasCameraActive) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                                        .border(
                                                            2.dp,
                                                            if (participant.isSpeaking) Color(0xFF10B981) else Color(0xFFE2E8F0),
                                                            activeBoxShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (participant.hasCameraActive) {
                                                        // Simulated video feed with pulsing live activity
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
                                                                    .size(40.dp)
                                                                    .background(participant.avatarBg, CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = participant.name.take(1),
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color(0xCC10B981),
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(4.dp)
                                                                ) {
                                                                Text(
                                                                    text = "مباشر 🔴",
                                                                    fontSize = 8.sp,
                                                                    color = Color.White,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(
                                                                imageVector = Icons.Default.VideocamOff,
                                                                contentDescription = null,
                                                                tint = Color(0xFF94A3B8),
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                            Text(
                                                                text = "الكاميرا مغلقة",
                                                                fontSize = 9.sp,
                                                                color = Color(0xFF94A3B8),
                                                                fontFamily = TajawalFontFamily
                                                            )
                                                        }
                                                    }
                                                }

                                                // Info details
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = participant.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = participant.role,
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF64748B),
                                                        fontFamily = TajawalFontFamily
                                                    )
                                                    if (participant.isSpeaking) {
                                                        Text(
                                                            text = "🎙️ يتحدث الآن في الهوكي توكي",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF10B981),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4D. INTERCOM / WALKIE-TALKIE SUB-VIEW (تفعيل حقيقي بالصوت عبر Zego)
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
                                            // REAL ZEGO AUDIO UNMUTE / MUTE
                                            ZegoCallManager.setMicrophoneMute(!isIntercomTalking)
                                            RealVoipEngine.ensureAudioCaptureStarted(context)
                                            RealVoipEngine.setMute(!isIntercomTalking)
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
                                    text = if (isMutedByMod) "المايك مكتوم من قبل المشرف 🔇" else if (isIntercomTalking) "ميكروفون Zego مفتوح - الصوت مباشر لجميع الأعضاء 🎙️" else "اضغط للتحدث في الهوكي توكي الصوتي المباشر",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = if (isIntercomTalking) Color(0xFF10B981) else Color(0xFF1E3A8A)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "بث صوتي فائق الدقة بدون تأخير (Zego Express Engine)",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    fontFamily = TajawalFontFamily
                                )
                            }
                        }

                        // ----------------------------------------------------
                        // 4E. USERS SUB-VIEW (الصلاحيات والإدارة وإظهار العدد والمنسدلة)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.USERS -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Header showing count clearly
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(0xFF10B981), CircleShape)
                                            )
                                            Text(
                                                text = "المتواجدون في الغرفة (${roomUsers.size} متصلين الآن)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFF1E3A8A)
                                            )
                                        }
                                        Text(
                                            text = "انقر على أي عضو للإدارة",
                                            fontSize = 10.sp,
                                            color = Color(0xFF2563EB),
                                            fontFamily = TajawalFontFamily
                                        )
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(roomUsers, key = { it.id }) { user ->
                                        Surface(
                                            onClick = {
                                                // Unconditionally open permissions & user management dialog
                                                selectedUserForPermissions = user
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
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
                                                        Text(text = user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(text = user.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = TajawalFontFamily)
                                                            if (user.isSpeaking) {
                                                                Text(text = "🎙️ يتحدث", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        Text(text = user.role, fontSize = 10.sp, color = Color(0xFF64748B))
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (user.isMutedVoice) {
                                                        Icon(Icons.Default.MicOff, contentDescription = "مكتوم", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                                    }
                                                    if (user.canChangeVideo) {
                                                        Icon(Icons.Default.SmartDisplay, contentDescription = "صلاحية الفيديو", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                                    }
                                                    Icon(Icons.Default.MoreVert, contentDescription = "إدارة", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4F. SETTINGS SUB-VIEW (تحكم كامل في الصوت + مؤشر التقديم والتأخير والإيقاف)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.SETTINGS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Volume Card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
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
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "مستوى صوت فيديو يوتيوب",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                            Text(
                                                text = "${(videoVolume * 100).toInt()}%",
                                                fontSize = 12.sp,
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
                                                inactiveTrackColor = Color(0xFFE2EAFD)
                                            )
                                        )
                                    }
                                }

                                // 2. Video Playback & Seek Controls (تقديم وتأخير وايقاف متزامن)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
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
                                                    text = "مؤشر التقديم والتأخير المتزامن",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF0F172A)
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
                                                inactiveTrackColor = Color(0xFFFEE2E2)
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
                                                color = Color(0xFF475569),
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
                                                color = Color(0xFF475569),
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

                                        // Sync banner
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF0FDF4),
                                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
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
                                                    color = Color(0xFF15803D)
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

            // User Permissions & Management Dialog
            if (selectedUserForPermissions != null) {
                val targetUser = selectedUserForPermissions!!
                val isMe = targetUser.id == currentUserId
                val canManage = (isHost || isAppOwner) && !isMe

                Dialog(
                    onDismissRequest = { selectedUserForPermissions = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(targetUser.avatarBg, CircleShape)
                                            .border(2.dp, Color(0xFF38BDF8), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = targetUser.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Column {
                                        Text(text = targetUser.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = TajawalFontFamily, color = Color.White)
                                        Text(text = "الرتبة: ${targetUser.role}", fontSize = 11.sp, color = Color(0xFF38BDF8))
                                    }
                                }
                                IconButton(onClick = { selectedUserForPermissions = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1E293B))

                            if (canManage) {
                                Text(
                                    text = "لوحة إدارة الصلاحيات والتحكم ⚙️",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF38BDF8)
                                )

                                // 1. Promote to Moderator
                                PermissionActionRow(
                                    title = if (targetUser.role.contains("مشرف")) "إلغاء صفة المشرف" else "ترقية إلى مشرف الغرفة 🛡️",
                                    subtitle = "يمنح المستخدم صلاحيات إدارة الغرفة والتحكم",
                                    icon = Icons.Default.Shield,
                                    iconColor = Color(0xFF38BDF8),
                                    onClick = {
                                        val newRole = if (targetUser.role.contains("مشرف")) "مشاهد" else "مشرف الغرفة 🛡️"
                                        val updated = targetUser.copy(role = newRole)
                                        val idx = roomUsers.indexOfFirst { it.id == targetUser.id }
                                        if (idx >= 0) roomUsers[idx] = updated
                                        syncSocket.broadcastMemberAction(targetUser.id, if (newRole.contains("مشرف")) "SET_MODERATOR" else "DEMOTE")
                                        selectedUserForPermissions = null
                                        Toast.makeText(context, "تم تحديث رتبة ${targetUser.name}", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                // 2. Allow/Restrict Video Control
                                PermissionActionRow(
                                    title = if (targetUser.canChangeVideo) "منع التحكم بالفيديو 🔒" else "منح صلاحية التحكم بالفيديو 🎬",
                                    subtitle = if (targetUser.canChangeVideo) "يلزم موافقة المضيف للتقديم أو تغيير الفيديو" else "يستطيع تشغيل وتقديم وتأخير الفيديو للجميع",
                                    icon = Icons.Default.SmartDisplay,
                                    iconColor = Color(0xFFEF4444),
                                    onClick = {
                                        val updated = targetUser.copy(canChangeVideo = !targetUser.canChangeVideo)
                                        val idx = roomUsers.indexOfFirst { it.id == targetUser.id }
                                        if (idx >= 0) roomUsers[idx] = updated
                                        syncSocket.broadcastMemberAction(targetUser.id, if (updated.canChangeVideo) "ALLOW_VIDEO" else "RESTRICT_VIDEO")
                                        selectedUserForPermissions = null
                                        Toast.makeText(context, "تم تعديل صلاحية الفيديو لـ ${targetUser.name}", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                // 3. Mute Mic (Walkie-Talkie)
                                PermissionActionRow(
                                    title = if (targetUser.isMutedVoice) "إلغاء كتم المايكروفون 🎙️" else "كتم المايكروفون (إسكات الصوت) 🔇",
                                    subtitle = "تعطيل أو تمكين خاصية التحدث في الهوكي توكي",
                                    icon = if (targetUser.isMutedVoice) Icons.Default.Mic else Icons.Default.MicOff,
                                    iconColor = Color(0xFFF59E0B),
                                    onClick = {
                                        val updated = targetUser.copy(isMutedVoice = !targetUser.isMutedVoice)
                                        val idx = roomUsers.indexOfFirst { it.id == targetUser.id }
                                        if (idx >= 0) roomUsers[idx] = updated
                                        syncSocket.broadcastMemberAction(targetUser.id, if (updated.isMutedVoice) "MUTE_VOICE" else "UNMUTE_VOICE")
                                        selectedUserForPermissions = null
                                        Toast.makeText(context, "تم تغيير حالة كتم المايك", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                // 4. Mute Chat
                                PermissionActionRow(
                                    title = if (targetUser.isMutedChat) "إلغاء حظر الدردشة 💬" else "إسكات من الدردشة (منع الكتابة) 🚫",
                                    subtitle = "تعطيل إرسال الرسائل في محادثة الغرفة",
                                    icon = Icons.Default.ChatBubbleOutline,
                                    iconColor = Color(0xFFA855F7),
                                    onClick = {
                                        val updated = targetUser.copy(isMutedChat = !targetUser.isMutedChat)
                                        val idx = roomUsers.indexOfFirst { it.id == targetUser.id }
                                        if (idx >= 0) roomUsers[idx] = updated
                                        syncSocket.broadcastMemberAction(targetUser.id, if (updated.isMutedChat) "MUTE_CHAT" else "UNMUTE_CHAT")
                                        selectedUserForPermissions = null
                                        Toast.makeText(context, "تم تحديث صلاحية الدردشة", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                // 5. Kick from room
                                PermissionActionRow(
                                    title = "طرد المستخدم من الغرفة 🚪",
                                    subtitle = "إخراج المستخدم فوراً من جلسة المشاهدة",
                                    icon = Icons.Default.ExitToApp,
                                    iconColor = Color(0xFFEF4444),
                                    onClick = {
                                        roomUsers.removeAll { it.id == targetUser.id }
                                        syncSocket.broadcastMemberAction(targetUser.id, "KICK")
                                        selectedUserForPermissions = null
                                        Toast.makeText(context, "تم طرد ${targetUser.name} من الغرفة", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else if (isMe) {
                                Text(
                                    text = "إعدادات حسابك الشخصي 👤",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF38BDF8)
                                )
                                PermissionActionRow(
                                    title = "مغادرة الغرفة 🚪",
                                    subtitle = "الخروج من جلسة المشاهدة والعودة للرئيسية",
                                    icon = Icons.Default.Logout,
                                    iconColor = Color(0xFFEF4444),
                                    onClick = {
                                        selectedUserForPermissions = null
                                        onBack()
                                    }
                                )
                            } else {
                                Text(
                                    text = "خيارات التفاعل مع العضو 💬",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF38BDF8)
                                )
                                PermissionActionRow(
                                    title = "إرسال تحية 👋",
                                    subtitle = "إرسال إشعار ترحيبي في الدردشة",
                                    icon = Icons.Default.WavingHand,
                                    iconColor = Color(0xFF38BDF8),
                                    onClick = {
                                        chatMessages.add(
                                            YouTubeChatMessage(
                                                id = System.currentTimeMillis().toString(),
                                                sender = currentUserName,
                                                text = "مرحباً يا ${targetUser.name}! 👋",
                                                time = "الآن",
                                                isMe = true
                                            )
                                        )
                                        syncSocket.broadcastChatMessage("مرحباً يا ${targetUser.name}! 👋")
                                        selectedUserForPermissions = null
                                        Toast.makeText(context, "تم إرسال التحية بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Host Video Change Request Approval Dialog
            if (pendingVideoChangeRequest != null) {
                val req = pendingVideoChangeRequest!!
                Dialog(
                    onDismissRequest = {},
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .border(1.5.dp, Color(0xFFEF4444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(26.dp))
                            }
                            Text(
                                text = "طلب تشغيل فيديو جديد 🎬",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color.White
                            )
                            Text(
                                text = "يريد \"${req.requesterName}\" تغيير الفيديو المشغل إلى:\n\"${req.videoTitle}\"\n\nهل توافق على تغيير الفيديو للجميع؟",
                                fontSize = 12.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val vid = YouTubeVideoItem(
                                            id = req.videoId,
                                            title = req.videoTitle,
                                            channelTitle = "مشاهدة متزامنة",
                                            duration = "مباشر",
                                            viewCount = "متزامن",
                                            publishedTime = "الآن",
                                            thumbnailUrl = "https://img.youtube.com/vi/${req.videoId}/hqdefault.jpg"
                                        )
                                        currentVideo = vid
                                        isPlaying = true
                                        currentPositionSec = 0f
                                        YouTubeRoomManager.updateRoomVideo(context, roomId, req.videoId, req.videoTitle)
                                        syncSocket.broadcastVideoChange(req.videoId, req.videoTitle)
                                        pendingVideoChangeRequest = null
                                        Toast.makeText(context, "تمت الموافقة وتغيير الفيديو للغرفة 🎬", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("موافق ✅", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = {
                                        syncSocket.rejectVideoChange(req.requesterId)
                                        pendingVideoChangeRequest = null
                                        Toast.makeText(context, "تم رفض طلب تغيير الفيديو", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                                ) {
                                    Text("رفض ❌", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
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
                                placeholder = { Text("ابحث في يوتيوب...", fontSize = 12.sp, fontFamily = TajawalFontFamily) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { executeSearch(searchQuery) }) {
                                        Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) })
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isSearchingRealYouTube) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFFDC2626))
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
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = item.channelTitle,
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                                Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                                            }
                                        }
                                    }
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
private fun YouTubeDockIconButton(
    icon: ImageVector,
    isActive: Boolean,
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    Box(contentAlignment = Alignment.TopEnd) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isActive) Color(0xFF2563EB) else Color.Transparent,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else Color(0xFF64748B),
                modifier = Modifier.size(19.dp)
            )
        }
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .background(Color(0xFFEF4444), CircleShape)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
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
private fun PermissionActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = TajawalFontFamily, color = Color.White)
                Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8), fontFamily = TajawalFontFamily)
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
        }
    }
}
