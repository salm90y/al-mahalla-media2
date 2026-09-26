package com.ps1.netplay.ui.compose

import android.content.Context
import android.media.AudioManager
import android.view.TextureView
import android.view.ViewGroup
import android.webkit.CookieManager
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.ps1.netplay.ui.RoomCameraHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. DATA MODELS & ENUMS FOR YOUTUBE WATCH-PARTY ROOM
// ----------------------------------------------------
enum class YouTubeRoomSubTab {
    PLAYER,
    CHAT,
    LIVE_SYNC,
    INTERCOM,
    USERS,
    SETTINGS
}

enum class RoomPrivacyMode {
    PUBLIC,      // مشاهدة عامة
    FRIENDS,     // مشاهدة للأصدقاء
    INVITE_ONLY, // مشاهدة للدعوة
    ONLY_ME      // مشاهدة فقط أنا
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

// Initial Library of videos
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

    // Active currently playing video (Synced with server and host)
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

    // Playback and Volume states
    var isPlaying by remember { mutableStateOf(true) }
    var videoVolume by remember { mutableStateOf(1.0f) }
    var isPlayerFullscreen by remember { mutableStateOf(false) }
    var isDirectStreamMode by remember { mutableStateOf(false) }
    var liveViewerCount by remember { mutableStateOf(if (isStealthMode) 1480 else 1481) }

    // Privacy Mode (رموز بدون كتابة)
    var roomPrivacyMode by remember { mutableStateOf(RoomPrivacyMode.PUBLIC) }
    var isPrivacyDropdownOpen by remember { mutableStateOf(false) }

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

    // Intercom / Voice Room State
    var isIntercomTalking by remember { mutableStateOf(false) }

    // Camera State
    var isCameraActive by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }

    // Dialog & Permission States
    var pendingVideoChangeRequest by remember { mutableStateOf<VideoChangeRequest?>(null) }
    var selectedUserForPermissions by remember { mutableStateOf<YouTubeRoomUser?>(null) }

    // Room Participants state (Excludes stealth users)
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
                    avatarBg = Color(0xFF8B5CF6),
                    canChangeVideo = false
                )
            )
        }
    }

    // Real-Time WebSocket Synchronization Client
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
                Toast.makeText(context, "تم تغيير الفيديو للغرفة: ${vTitle.take(30)} 🎬", Toast.LENGTH_SHORT).show()
            },
            onPlaybackStateReceived = { playState, _ ->
                isPlaying = playState
            },
            onChatMessageReceived = { newMsg ->
                chatMessages.add(newMsg)
            },
            onStateRequested = {
                // When newcomer joins, reply with current video and playback state
                syncSocket.broadcastVideoChange(currentVideo.id, currentVideo.title)
                syncSocket.broadcastPlaybackState(isPlaying, 0f)
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
                            Toast.makeText(context, "تم طردك من الغرفة بواسطة المشرف", Toast.LENGTH_LONG).show()
                            onBack()
                        }
                        "MUTE_VOICE" -> {
                            isIntercomTalking = false
                            RealVoipEngine.setMute(true)
                            Toast.makeText(context, "تم كتم صوت المايكروفون الخاص بك من قبل المشرف 🔇", Toast.LENGTH_SHORT).show()
                        }
                        "ALLOW_VIDEO" -> {
                            val idx = roomUsers.indexOfFirst { it.id == currentUserId }
                            if (idx >= 0) roomUsers[idx] = roomUsers[idx].copy(canChangeVideo = true)
                            Toast.makeText(context, "منحك المشرف صلاحية تغيير الفيديو مباشرة 🎬", Toast.LENGTH_SHORT).show()
                        }
                        "RESTRICT_VIDEO" -> {
                            val idx = roomUsers.indexOfFirst { it.id == currentUserId }
                            if (idx >= 0) roomUsers[idx] = roomUsers[idx].copy(canChangeVideo = false)
                            Toast.makeText(context, "تم تقييد صلاحية تغيير الفيديو (يلزم إذن المضيف) 🔒", Toast.LENGTH_SHORT).show()
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

    LaunchedEffect(roomId) {
        syncSocket.connect()
    }

    DisposableEffect(roomId) {
        onDispose {
            syncSocket.disconnect()
            RealVoipEngine.stopVoipSession(context)
        }
    }

    // Search Bar & Instant Autocomplete Dropdown State
    var searchQuery by remember { mutableStateOf("") }
    var isDropdownOpen by remember { mutableStateOf(false) }
    var isSearchModalOpen by remember { mutableStateOf(false) }
    var isSearchingRealYouTube by remember { mutableStateOf(false) }
    var isExpandedSearchLoading by remember { mutableStateOf(false) }
    val liveSearchSuggestions = remember { mutableStateListOf<String>() }
    val realSearchResults = remember { mutableStateListOf<YouTubeVideoItem>() }

    // Live real-time YouTube suggestions watcher (Google Suggest API only)
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

    // Play a video directly in the player box or request permission
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
        isSearchModalOpen = false
        isDropdownOpen = false
        YouTubeRoomManager.updateRoomVideo(context, roomId, video.id, video.title)
        syncSocket.broadcastVideoChange(video.id, video.title)
        Toast.makeText(context, "جاري تشغيل: ${video.title.take(35)}... 🎬", Toast.LENGTH_SHORT).show()
    }

    // Perform Search & Open Results Modal with REAL YouTube Search without reservations
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
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ====================================================
                // 1. TOP HEADER & MERGED SLEEK SEARCH BAR
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Compact Search Input field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            isDropdownOpen = it.trim().isNotEmpty()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "ابحث في يوتيوب...",
                                fontSize = 11.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) })
                    )

                    // Small Search Icon Button
                    IconButton(
                        onClick = {
                            if (searchQuery.trim().isNotEmpty()) {
                                executeSearch(searchQuery)
                            } else {
                                isSearchModalOpen = true
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFDC2626), CircleShape)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Room Code Chip next to the search button
                    Surface(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("كود الغرفة", roomCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود الغرفة ($roomCode) بنجاح!", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF5FF),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = roomCode,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }

                    // Stealth indicator if owner entered in stealth mode
                    if (isStealthMode) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(
                                text = "👻",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Autocomplete Suggestions Floating Dropdown Box
                if (isDropdownOpen && liveSearchSuggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            liveSearchSuggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            executeSearch(suggestion)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = suggestion,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.NorthWest,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ====================================================
                // 2. DEDICATED REAL YOUTUBE VIDEO PLAYER BOX
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(235.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black)
                        .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    key(currentVideo.id, isDirectStreamMode) {
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
                                    val safeVideoId = currentVideo.id
                                    val customHtml = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                            <style>
                                                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                                iframe { width: 100vw; height: 100vh; border: none; }
                                            </style>
                                        </head>
                                        <body>
                                            <div id="player"></div>
                                            <script src="https://www.youtube.com/iframe_api"></script>
                                            <script>
                                                var player;
                                                function onYouTubeIframeAPIReady() {
                                                    player = new YT.Player('player', {
                                                        videoId: '$safeVideoId',
                                                        playerVars: {
                                                            'autoplay': 1,
                                                            'controls': 1,
                                                            'playsinline': 1,
                                                            'rel': 0,
                                                            'modestbranding': 1,
                                                            'enablejsapi': 1
                                                        },
                                                        events: {
                                                            'onReady': function(e) { e.target.playVideo(); e.target.setVolume(${ (videoVolume * 100).toInt() }); }
                                                        }
                                                    });
                                                }
                                                function setPlayerVolume(vol) { if (player && player.setVolume) player.setVolume(vol); }
                                            </script>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    loadDataWithBaseURL("https://www.google.com", customHtml, "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                val vol = (videoVolume * 100).toInt()
                                webView.evaluateJavascript("if (typeof setPlayerVolume === 'function') { setPlayerVolume($vol); }", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Player Overlay Controls: Camera + Stream Mode switch + Fullscreen button
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Real Camera Toggle Button
                        IconButton(
                            onClick = {
                                isCameraActive = !isCameraActive
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (isCameraActive) Color(0xFF10B981) else Color(0xAA000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isCameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "الكاميرا",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Stream Mode switch
                        Surface(
                            onClick = {
                                isDirectStreamMode = !isDirectStreamMode
                                Toast.makeText(
                                    context,
                                    if (isDirectStreamMode) "تم تفعيل المشغل المباشر ⚡" else "تم تفعيل مشغل السينما القياسي 🎬",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = CircleShape,
                            color = if (isDirectStreamMode) Color(0xCC10B981) else Color(0xAA000000)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDirectStreamMode) Icons.Default.Bolt else Icons.Default.Language,
                                    contentDescription = "تبديل وضع التشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isDirectStreamMode) "مباشر" else "تخطي القيود",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color.White
                                )
                            }
                        }

                        // Fullscreen expand button
                        IconButton(
                            onClick = { isPlayerFullscreen = true },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xAA000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "ملء الشاشة",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ====================================================
                // 3. SUB-TABS DOCK BAR
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
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        YouTubeDockIconButton(
                            icon = Icons.Default.PlayCircle,
                            isActive = activeSubTab == YouTubeRoomSubTab.PLAYER,
                            onClick = { activeSubTab = YouTubeRoomSubTab.PLAYER }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Default.ChatBubbleOutline,
                            isActive = activeSubTab == YouTubeRoomSubTab.CHAT,
                            onClick = { activeSubTab = YouTubeRoomSubTab.CHAT }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Default.Sync,
                            isActive = activeSubTab == YouTubeRoomSubTab.LIVE_SYNC,
                            onClick = { activeSubTab = YouTubeRoomSubTab.LIVE_SYNC }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Default.RecordVoiceOver,
                            isActive = activeSubTab == YouTubeRoomSubTab.INTERCOM,
                            onClick = { activeSubTab = YouTubeRoomSubTab.INTERCOM }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Default.PeopleOutline,
                            isActive = activeSubTab == YouTubeRoomSubTab.USERS,
                            onClick = { activeSubTab = YouTubeRoomSubTab.USERS }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Default.Settings,
                            isActive = activeSubTab == YouTubeRoomSubTab.SETTINGS,
                            onClick = { activeSubTab = YouTubeRoomSubTab.SETTINGS }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 80.dp, height = 52.dp)
                                                    .clip(RoundedCornerShape(10.dp))
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
                        // 4B. CHAT SUB-VIEW (معالجة لوحة المفاتيح والارتفاع والمزامنة)
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
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(chatMessages, key = { it.id }) { msg ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (msg.isMe) Color(0xFF2563EB) else Color.White,
                                                border = BorderStroke(1.dp, if (msg.isMe) Color(0xFF2563EB) else Color(0xFFE2EAFD))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
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
                                        .padding(top = 6.dp),
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
                                                text = if (isChatMuted) "تم تقييد الدردشة لك من قبل المشرف 🔇" else "اكتب رسالة في المحادثة...",
                                                fontSize = 11.sp,
                                                fontFamily = TajawalFontFamily
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
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
                                            .size(42.dp)
                                            .background(Color(0xFF2563EB), CircleShape),
                                        enabled = !isChatMuted
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4C. LIVE SYNC SUB-VIEW
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.LIVE_SYNC -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "مزامنة البث السحابي عبر Cloudflare CDN",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "خوادم الحافة تضمن تزامن تشغيل مقاطع اليوتيوب بين جميع المتواجدين بدقة الميلي ثانية.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            fontFamily = TajawalFontFamily
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            YouTubeStatMiniBlock(label = "زمن الاستجابة", value = "12 ms")
                                            YouTubeStatMiniBlock(label = "فقد الحزم", value = "0.00%")
                                            YouTubeStatMiniBlock(label = "جودة البث", value = "1080p 60fps")
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4D. INTERCOM / WALKIE-TALKIE SUB-VIEW (تفعيل حقيقي للصوت المباشر)
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
                                        .size(130.dp)
                                        .background(
                                            if (isMutedByMod) Color(0xFF64748B) else if (isIntercomTalking) Color(0xFF10B981) else Color(0xFF2563EB),
                                            CircleShape
                                        )
                                        .border(4.dp, Color.White, CircleShape)
                                        .shadow(8.dp, CircleShape)
                                        .clickable {
                                            if (isMutedByMod) {
                                                Toast.makeText(context, "المايكروفون مكتوم من قبل المشرف 🔇", Toast.LENGTH_SHORT).show()
                                                return@clickable
                                            }
                                            isIntercomTalking = !isIntercomTalking
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (isMutedByMod) "المايك مكتوم من قبل المشرف 🔇" else if (isIntercomTalking) "الميكروفون مفتوح - جاري البث لجميع الأعضاء 🎙️" else "اضغط للتحدث الجماعي في الهوكي توكي",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = if (isIntercomTalking) Color(0xFF10B981) else Color(0xFF1E3A8A)
                                )
                            }
                        }

                        // ----------------------------------------------------
                        // 4E. USERS SUB-VIEW (الصلاحيات والإدارة المتكاملة)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.USERS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(roomUsers, key = { it.id }) { user ->
                                    val canManage = (isHost || isAppOwner) && (user.id != currentUserId)
                                    Surface(
                                        onClick = {
                                            if (canManage) {
                                                selectedUserForPermissions = user
                                            }
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
                                                if (canManage) {
                                                    Icon(Icons.Default.MoreVert, contentDescription = "إدارة", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4F. SETTINGS SUB-VIEW
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.SETTINGS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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
                            }
                        }
                    }
                }
            }

            // Real Camera Floating Dock
            if (isCameraActive) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black,
                    border = BorderStroke(2.dp, Color(0xFF2563EB)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(width = 120.dp, height = 160.dp)
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 65.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
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
                        IconButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            modifier = Modifier.size(26.dp).align(Alignment.TopStart).padding(2.dp)
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "تبديل الكاميرا", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { isCameraActive = false },
                            modifier = Modifier.size(26.dp).align(Alignment.TopEnd).padding(2.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // User Permissions Management Dialog
            if (selectedUserForPermissions != null) {
                val targetUser = selectedUserForPermissions!!
                Dialog(
                    onDismissRequest = { selectedUserForPermissions = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
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
                                        modifier = Modifier.size(42.dp).background(targetUser.avatarBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = targetUser.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Column {
                                        Text(text = targetUser.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = TajawalFontFamily)
                                        Text(text = "الرتبة: ${targetUser.role}", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                                IconButton(onClick = { selectedUserForPermissions = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Text(
                                text = "إدارة الصلاحيات والتحكم ⚙️",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF1E3A8A)
                            )

                            // 1. Promote to Moderator
                            PermissionActionRow(
                                title = if (targetUser.role.contains("مشرف")) "إلغاء صفة المشرف" else "ترقية إلى مشرف الغرفة 🛡️",
                                subtitle = "يمنح المستخدم صلاحيات إدارة الغرفة",
                                icon = Icons.Default.Shield,
                                iconColor = Color(0xFF2563EB),
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

                            // 2. Allow/Restrict Video Change
                            PermissionActionRow(
                                title = if (targetUser.canChangeVideo) "منع تغيير الفيديو 🔒" else "منح صلاحية تغيير الفيديو 🎬",
                                subtitle = if (targetUser.canChangeVideo) "يلزم موافقة المضيف لتشغيل أي مقطع" else "يستطيع تشغيل المقاطع مباشرة",
                                icon = Icons.Default.SmartDisplay,
                                iconColor = Color(0xFFDC2626),
                                onClick = {
                                    val updated = targetUser.copy(canChangeVideo = !targetUser.canChangeVideo)
                                    val idx = roomUsers.indexOfFirst { it.id == targetUser.id }
                                    if (idx >= 0) roomUsers[idx] = updated
                                    syncSocket.broadcastMemberAction(targetUser.id, if (updated.canChangeVideo) "ALLOW_VIDEO" else "RESTRICT_VIDEO")
                                    selectedUserForPermissions = null
                                    Toast.makeText(context, "تم تعديل صلاحية الفيديو لـ ${targetUser.name}", Toast.LENGTH_SHORT).show()
                                }
                            )

                            // 3. Mute Mic
                            PermissionActionRow(
                                title = if (targetUser.isMutedVoice) "إلغاء كتم المايكروفون 🎙️" else "كتم المايكروفون (إسكات الصوت) 🔇",
                                subtitle = "تعطيل خاصية التحدث في الهوكي توكي",
                                icon = if (targetUser.isMutedVoice) Icons.Default.Mic else Icons.Default.MicOff,
                                iconColor = Color(0xFFD97706),
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
                                title = if (targetUser.isMutedChat) "إلغاء حظر الدردشة 💬" else "منع الكتابة في الدردشة 🚫",
                                subtitle = "تعطيل إرسال الرسائل في الشات",
                                icon = Icons.Default.ChatBubbleOutline,
                                iconColor = Color(0xFF7C3AED),
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
                                iconColor = Color(0xFFDC2626),
                                onClick = {
                                    roomUsers.removeAll { it.id == targetUser.id }
                                    syncSocket.broadcastMemberAction(targetUser.id, "KICK")
                                    selectedUserForPermissions = null
                                    Toast.makeText(context, "تم طرد ${targetUser.name} من الغرفة", Toast.LENGTH_SHORT).show()
                                }
                            )
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
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).background(Color(0xFFFEF2F2), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                            }
                            Text(
                                text = "طلب تشغيل فيديو جديد 🎬",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "يريد العضو \"${req.requesterName}\" تغيير الفيديو المشغل للغرفة إلى:\n\"${req.videoTitle}\"\n\nهل توافق على تغيير الفيديو للجميع؟",
                                fontSize = 12.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF475569),
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
                                        YouTubeRoomManager.updateRoomVideo(context, roomId, req.videoId, req.videoTitle)
                                        syncSocket.broadcastVideoChange(req.videoId, req.videoTitle)
                                        pendingVideoChangeRequest = null
                                        Toast.makeText(context, "تمت الموافقة وتغيير الفيديو للغرفة 🎬", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
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
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
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
                                placeholder = { Text("اكتب أي شيء للبحث في يوتيوب...", fontSize = 12.sp, fontFamily = TajawalFontFamily) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
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
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(realSearchResults, key = { it.id }) { item ->
                                        Surface(
                                            onClick = { playSelectedVideo(item) },
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 100.dp, height = 65.dp)
                                                        .clip(RoundedCornerShape(12.dp))
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
                                                        fontSize = 12.sp,
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
                                                Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fullscreen video player dialog
            if (isPlayerFullscreen) {
                Dialog(
                    onDismissRequest = { isPlayerFullscreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                                    }
                                    loadDataWithBaseURL("https://www.google.com", "<iframe width='100%' height='100%' src='https://www.youtube.com/embed/${currentVideo.id}?autoplay=1&controls=1' frameborder='0' allowfullscreen></iframe>", "text/html", "UTF-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { isPlayerFullscreen = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(38.dp).background(Color(0xAA000000), CircleShape)
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "خروج", tint = Color.White, modifier = Modifier.size(22.dp))
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
    onClick: () -> Unit
) {
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
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun YouTubeStatMiniBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = Color(0xFF94A3B8), fontFamily = TajawalFontFamily)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
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
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = TajawalFontFamily, color = Color(0xFF0F172A))
                Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF64748B), fontFamily = TajawalFontFamily)
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        }
    }
}
