package com.ps1.netplay.ui.compose

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.ViewGroup
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
    val role: String,
    val isHost: Boolean = false,
    val isOnline: Boolean = true,
    val isSpeaking: Boolean = false,
    val avatarBg: Color = Color(0xFF2563EB)
)

// Initial Library of videos
val INITIAL_YOUTUBE_CATALOG = listOf(
    YouTubeVideoItem(
        id = "jfKfPfyJRdk",
        title = "بث مباشر 24/7 بجودة عالية 4K Ultra HD",
        channelTitle = "قناة البث المباشر الرسمية",
        duration = "بث مباشر",
        viewCount = "48.9K يشاهدون الآن",
        publishedTime = "مباشر الآن",
        thumbnailUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=600&auto=format&fit=crop&q=80",
        category = "بث مباشر",
        isLive = true
    ),
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Sub-tab selection (PLAYER default)
    var activeSubTab by remember { mutableStateOf(YouTubeRoomSubTab.PLAYER) }

    // Video Catalog state
    val videoCatalog = remember {
        mutableStateListOf<YouTubeVideoItem>().apply {
            addAll(INITIAL_YOUTUBE_CATALOG)
        }
    }

    // Active currently playing video
    var currentVideo by remember { mutableStateOf<YouTubeVideoItem>(videoCatalog[0]) }

    // Playback and Volume states
    var isPlaying by remember { mutableStateOf(true) }
    var videoVolume by remember { mutableStateOf(1.0f) } // YouTube player volume 0% - 100%
    var isPlayerFullscreen by remember { mutableStateOf(false) } // Professional fullscreen video mode
    var isSynchronizedWithRoom by remember { mutableStateOf(true) }
    var liveViewerCount by remember { mutableStateOf(1480) }

    // Privacy Mode (رموز بدون كتابة)
    var roomPrivacyMode by remember { mutableStateOf(RoomPrivacyMode.PUBLIC) }
    var isPrivacyDropdownOpen by remember { mutableStateOf(false) }

    // Room Queue Playlist state
    val queuePlaylist = remember {
        mutableStateListOf(
            videoCatalog[1],
            videoCatalog[2]
        )
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

    // Audio beep player
    fun playButtonBeep(type: Int = ToneGenerator.TONE_PROP_BEEP) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
            toneGen.startTone(type, 50)
        } catch (_: Exception) {}
    }

    // Play a video directly in the player box
    fun playSelectedVideo(video: YouTubeVideoItem) {
        playButtonBeep()
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        currentVideo = video
        isPlaying = true
        isSearchModalOpen = false
        isDropdownOpen = false
        Toast.makeText(context, "جاري تشغيل: ${video.title.take(35)}... 🎬", Toast.LENGTH_SHORT).show()
    }

    // Perform Search & Open Results Modal with REAL YouTube Search without reservations
    fun executeSearch(query: String) {
        playButtonBeep()
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
                        // Also add to catalog without duplicates
                        realVideos.reversed().forEach { rv ->
                            videoCatalog.removeAll { it.id == rv.id }
                            videoCatalog.add(0, rv)
                        }
                        Toast.makeText(context, "تم العثور على ${realVideos.size} مقطع حقيقي من YouTube 🎬", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {}
                isSearchingRealYouTube = false
            }
        }
    }

    // Add video to room queue
    fun addVideoToQueue(video: YouTubeVideoItem) {
        playButtonBeep()
        if (!queuePlaylist.any { it.id == video.id }) {
            queuePlaylist.add(video)
            Toast.makeText(context, "تمت إضافة الفيديو إلى قائمة تشغيل الغرفة ✨", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "الفيديو موجود بالفعل في قائمة الانتظار", Toast.LENGTH_SHORT).show()
        }
    }

    // Live Room Chat Messages State
    val chatMessages = remember {
        mutableStateListOf(
            YouTubeChatMessage("1", "أحمد (المضيف)", "أهلاً بالجميع في غرفة سينما اليوتيوب! 🎬🍿", "10:20 م", false, Color(0xFF2563EB)),
            YouTubeChatMessage("2", "سارة", "جودة البث عبر Cloudflare ممتازة وسريعة جداً بدون أي تقطيع ⚡", "10:21 م", false, Color(0xFF10B981)),
            YouTubeChatMessage("3", "محمد", "الصوت متزامن 100% مع الجميع ✨", "10:22 م", false, Color(0xFF8B5CF6))
        )
    }
    var chatInputText by remember { mutableStateOf("") }

    // Intercom / Voice Room State
    var isIntercomTalking by remember { mutableStateOf(false) }
    var roomVolumeLevel by remember { mutableStateOf(0.85f) }

    // Room Participants state
    val roomUsers = remember {
        mutableStateListOf(
            YouTubeRoomUser("u1", "أحمد", "مضيف الغرفة", isHost = true, isOnline = true, isSpeaking = false, Color(0xFF2563EB)),
            YouTubeRoomUser("u2", "سارة", "مشرف", isHost = false, isOnline = true, isSpeaking = false, Color(0xFF10B981)),
            YouTubeRoomUser("u3", "محمد", "مشاهد VIP", isHost = false, isOnline = true, isSpeaking = false, Color(0xFF8B5CF6))
        )
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

                    // Compact, low-height Search Input field merged into the top bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            isDropdownOpen = it.trim().isNotEmpty()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        placeholder = {
                            Text(
                                text = "ابحث في اليوتيوب...",
                                fontSize = 11.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        isDropdownOpen = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchQuery.trim().isNotEmpty()) {
                                executeSearch(searchQuery)
                            }
                        }),
                        shape = RoundedCornerShape(19.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFDBEAFE),
                            cursorColor = Color(0xFF2563EB)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 11.sp,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF0F172A)
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    )

                    // Small Search Icon Button (No text, icon-only, compact)
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
                                val clip = android.content.ClipData.newPlainText("كود الغرفة", "#YT-9024")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود الغرفة (#YT-9024) بنجاح!", Toast.LENGTH_SHORT).show()
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
                                text = "#YT-9024",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
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
                                Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
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
                    // REAL YouTube WebView Player keyed by currentVideo.id
                    key(currentVideo.id) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.allowFileAccess = true
                                    settings.allowContentAccess = true
                                    settings.setSupportZoom(false)
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            return false
                                        }
                                    }
                                    webChromeClient = WebChromeClient()

                                    val embedHtml = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                            <style>
                                                * { margin:0; padding:0; box-sizing:border-box; }
                                                body, html { width:100%; height:100%; background:#000; overflow:hidden; }
                                                iframe { position:absolute; top:0; left:0; width:100%; height:100%; border:none; }
                                            </style>
                                        </head>
                                        <body>
                                            <iframe 
                                                id="ytplayer"
                                                src="https://www.youtube.com/embed/${currentVideo.id}?autoplay=1&playsinline=1&controls=1&enablejsapi=1&fs=0&rel=0&modestbranding=1" 
                                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                                allowfullscreen>
                                            </iframe>
                                        </body>
                                        </html>
                                    """.trimIndent()

                                    loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                val vol = (videoVolume * 100).toInt()
                                webView.evaluateJavascript(
                                    "var f = document.getElementById('ytplayer'); if (f) { f.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"setVolume\",\"args\":[$vol]}', '*'); }",
                                    null
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Player Overlay Controls: Fullscreen button only
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Professional Fullscreen Video Button
                        IconButton(
                            onClick = {
                                isPlayerFullscreen = true
                                playButtonBeep()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xAA000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "تكبير الفيديو",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Bottom info bar overlay on the player box
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC000000))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentVideo.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xAA10B981)
                        ) {
                            Text(
                                text = "مباشر 4K",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ====================================================
                // 3. SUB-TABS NAVIGATION DOCK
                // ====================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        YouTubeDockIconButton(
                            icon = Icons.Default.SmartDisplay,
                            isActive = activeSubTab == YouTubeRoomSubTab.PLAYER,
                            onClick = { activeSubTab = YouTubeRoomSubTab.PLAYER }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            isActive = activeSubTab == YouTubeRoomSubTab.CHAT,
                            onClick = { activeSubTab = YouTubeRoomSubTab.CHAT }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Default.Sensors,
                            isActive = activeSubTab == YouTubeRoomSubTab.LIVE_SYNC,
                            onClick = { activeSubTab = YouTubeRoomSubTab.LIVE_SYNC }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Default.Mic,
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
                // 4. SUB-TAB ACTIVE CONTENT
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (activeSubTab) {
                        // ----------------------------------------------------
                        // 4A. PLAYER SUB-VIEW: Queue Playlist & Related
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.PLAYER -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "قائمة تشغيل الغرفة (${queuePlaylist.size})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Text(
                                            text = "بحث عن مقاطع 🔍",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFFDC2626),
                                            modifier = Modifier.clickable { isSearchModalOpen = true }
                                        )
                                    }
                                }

                                items(queuePlaylist) { item ->
                                    val isCurrent = item.id == currentVideo.id
                                    Surface(
                                        onClick = { playSelectedVideo(item) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isCurrent) Color(0xFFEEF5FF) else Color.White,
                                        border = BorderStroke(1.dp, if (isCurrent) Color(0xFF2563EB) else Color(0xFFE2EAFD)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Thumbnail
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 80.dp, height = 50.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            ) {
                                                AsyncImage(
                                                    model = item.thumbnailUrl,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (isCurrent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color(0x662563EB)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Equalizer,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = if (isCurrent) Color(0xFF2563EB) else Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.channelTitle,
                                                    fontSize = 10.sp,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            IconButton(
                                                onClick = { playSelectedVideo(item) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isCurrent) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                    contentDescription = "تشغيل",
                                                    tint = if (isCurrent) Color(0xFF2563EB) else Color(0xFFDC2626),
                                                    modifier = Modifier.size(26.dp)
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
                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(chatMessages) { msg ->
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
                                    OutlinedTextField(
                                        value = chatInputText,
                                        onValueChange = { chatInputText = it },
                                        placeholder = { Text("اكتب رسالة في المحادثة...", fontSize = 11.sp, fontFamily = TajawalFontFamily) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            if (chatInputText.trim().isNotEmpty()) {
                                                chatMessages.add(
                                                    YouTubeChatMessage(
                                                        id = System.currentTimeMillis().toString(),
                                                        sender = "أنا",
                                                        text = chatInputText.trim(),
                                                        time = "الآن",
                                                        isMe = true
                                                    )
                                                )
                                                chatInputText = ""
                                                playButtonBeep()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(0xFF2563EB), CircleShape)
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
                                            text = "خوادم الحافة (Edge) تضمن تزامن تشغيل مقاطع اليوتيوب بين جميع المتواجدين بدقة أجزاء الثانية.",
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
                        // 4D. INTERCOM SUB-VIEW
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.INTERCOM -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(
                                            if (isIntercomTalking) Color(0xFF10B981) else Color(0xFF2563EB),
                                            CircleShape
                                        )
                                        .border(4.dp, Color.White, CircleShape)
                                        .shadow(8.dp, CircleShape)
                                        .clickable {
                                            isIntercomTalking = !isIntercomTalking
                                            playButtonBeep()
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isIntercomTalking) Icons.Default.Mic else Icons.Default.MicNone,
                                        contentDescription = "تحدث",
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isIntercomTalking) "الميكروفون مفتوح - جاري البث" else "اضغط للتحدث الجماعي في الغرفة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                        }

                        // ----------------------------------------------------
                        // 4E. USERS SUB-VIEW
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.USERS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(roomUsers) { user ->
                                    Surface(
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
                                                        .size(36.dp)
                                                        .background(user.avatarBg, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                Column {
                                                    Text(text = user.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = TajawalFontFamily)
                                                    Text(text = user.role, fontSize = 10.sp, color = Color(0xFF64748B))
                                                }
                                            }
                                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 4F. SETTINGS SUB-VIEW (التحكم بصوت الفيديو + منسدلة الخصوصية برموز فقط)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.SETTINGS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. YouTube Video Volume Controller
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (videoVolume > 0.5f) Icons.Default.VolumeUp else if (videoVolume > 0f) Icons.Default.VolumeDown else Icons.Default.VolumeMute,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "مستوى صوت فيديو اليوتيوب",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF1E3A8A)
                                                )
                                            }
                                            Text(
                                                text = "${(videoVolume * 100).toInt()}%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Slider(
                                            value = videoVolume,
                                            onValueChange = { videoVolume = it },
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFFDC2626),
                                                activeTrackColor = Color(0xFFDC2626)
                                            )
                                        )
                                    }
                                }

                                // 2. Room Privacy Dropdown Selector (رموز فقط بدون كتابة)
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
                                            Text(
                                                text = "خصوصية مشاهدة الغرفة",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFF1E3A8A)
                                            )

                                            // Dropdown Anchor
                                            Box {
                                                Surface(
                                                    onClick = { isPrivacyDropdownOpen = true },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFFEEF5FF),
                                                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        val currentPrivacyIcon = when (roomPrivacyMode) {
                                                            RoomPrivacyMode.PUBLIC -> Icons.Default.Public
                                                            RoomPrivacyMode.FRIENDS -> Icons.Default.Group
                                                            RoomPrivacyMode.INVITE_ONLY -> Icons.Default.MailOutline
                                                            RoomPrivacyMode.ONLY_ME -> Icons.Default.Lock
                                                        }
                                                        Icon(
                                                            imageVector = currentPrivacyIcon,
                                                            contentDescription = null,
                                                            tint = Color(0xFF2563EB),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDropDown,
                                                            contentDescription = null,
                                                            tint = Color(0xFF2563EB),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                // Dropdown Menu: ICONS ONLY WITHOUT TEXT
                                                DropdownMenu(
                                                    expanded = isPrivacyDropdownOpen,
                                                    onDismissRequest = { isPrivacyDropdownOpen = false }
                                                ) {
                                                    val privacyOptions = listOf(
                                                        RoomPrivacyMode.PUBLIC to Icons.Default.Public,
                                                        RoomPrivacyMode.FRIENDS to Icons.Default.Group,
                                                        RoomPrivacyMode.INVITE_ONLY to Icons.Default.MailOutline,
                                                        RoomPrivacyMode.ONLY_ME to Icons.Default.Lock
                                                    )
                                                    privacyOptions.forEach { (mode, icon) ->
                                                        DropdownMenuItem(
                                                            text = { },
                                                            leadingIcon = {
                                                                Icon(
                                                                    imageVector = icon,
                                                                    contentDescription = null,
                                                                    tint = if (roomPrivacyMode == mode) Color(0xFF2563EB) else Color(0xFF64748B),
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            },
                                                            onClick = {
                                                                roomPrivacyMode = mode
                                                                isPrivacyDropdownOpen = false
                                                                playButtonBeep()
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Direct Row of 4 Icons (رموز بدون كتابة)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val privacyOptions = listOf(
                                                RoomPrivacyMode.PUBLIC to Icons.Default.Public,
                                                RoomPrivacyMode.FRIENDS to Icons.Default.Group,
                                                RoomPrivacyMode.INVITE_ONLY to Icons.Default.MailOutline,
                                                RoomPrivacyMode.ONLY_ME to Icons.Default.Lock
                                            )
                                            privacyOptions.forEach { (mode, icon) ->
                                                val isSelected = roomPrivacyMode == mode
                                                Surface(
                                                    onClick = {
                                                        roomPrivacyMode = mode
                                                        playButtonBeep()
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                                    modifier = Modifier.size(44.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = icon,
                                                            contentDescription = null,
                                                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Additional Settings Switches
                                YouTubeSettingsSwitchItem(
                                    title = "تسريع السحابة عبر Cloudflare CDN",
                                    subtitle = "تقليل استهلاك البيانات وتحسين سرعة استجابة الفيديو",
                                    isChecked = true
                                )
                                YouTubeSettingsSwitchItem(
                                    title = "المزامنة التلقائية التامة",
                                    subtitle = "مطابقة توقيت التشغيل مع مضيف الغرفة بدقة 0.1 ثانية",
                                    isChecked = isSynchronizedWithRoom
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====================================================
        // 5. PROFESSIONAL IMMERSIVE FULLSCREEN VIDEO DIALOG
        // ====================================================
        if (isPlayerFullscreen) {
            Dialog(
                onDismissRequest = { isPlayerFullscreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Fullscreen YouTube Player
                    key(currentVideo.id) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
                                    }
                                    webChromeClient = WebChromeClient()
                                    val embedHtml = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                            <style>
                                                * { margin:0; padding:0; box-sizing:border-box; }
                                                body, html { width:100%; height:100%; background:#000; overflow:hidden; }
                                                iframe { position:absolute; top:0; left:0; width:100%; height:100%; border:none; }
                                            </style>
                                        </head>
                                        <body>
                                            <iframe 
                                                id="ytplayer_fs"
                                                src="https://www.youtube.com/embed/${currentVideo.id}?autoplay=1&playsinline=1&controls=1&enablejsapi=1&fs=0&rel=0&modestbranding=1" 
                                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                                allowfullscreen>
                                            </iframe>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                val vol = (videoVolume * 100).toInt()
                                webView.evaluateJavascript(
                                    "var f = document.getElementById('ytplayer_fs'); if (f) { f.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"setVolume\",\"args\":[$vol]}', '*'); }",
                                    null
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Floating Top Bar in Fullscreen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { isPlayerFullscreen = false },
                            shape = CircleShape,
                            color = Color(0xAA000000)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "تصغير",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "تصغير",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = TajawalFontFamily
                                )
                            }
                        }

                        Text(
                            text = currentVideo.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = TajawalFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(Color(0xAA000000), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (videoVolume > 0.5f) Icons.Default.VolumeUp else if (videoVolume > 0f) Icons.Default.VolumeDown else Icons.Default.VolumeMute,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${(videoVolume * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ====================================================
        // 6. RICH SEARCH MODAL DIALOG (UNRESTRICTED REAL YOUTUBE RESULTS)
        // ====================================================
        if (isSearchModalOpen) {
            val displayResults = if (realSearchResults.isNotEmpty()) realSearchResults else videoCatalog

            Dialog(
                onDismissRequest = { isSearchModalOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        color = getAppScreenBackground()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Top Bar of Search Dialog
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { isSearchModalOpen = false },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White, CircleShape)
                                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = Color(0xFF1E3A8A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "نتائج: $searchQuery" else "نتائج مقاطع اليوتيوب",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEEF5FF),
                                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                                ) {
                                    Text(
                                        text = "${displayResults.size} مقطع متاح",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Searching status indicator
                            if (isSearchingRealYouTube) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFFDC2626)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "جاري جلب كافة نتائج YouTube بدون تحفظ...",
                                            fontSize = 11.sp,
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }

                            // Unrestricted Results List
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(displayResults) { item ->
                                    Surface(
                                        onClick = { playSelectedVideo(item) },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                        shadowElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // High-Res Thumbnail with badge
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 115.dp, height = 75.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = item.thumbnailUrl,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(4.dp)
                                                            .background(
                                                                if (item.isLive) Color(0xCCDC2626) else Color(0xCC000000),
                                                                RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = item.duration,
                                                            fontSize = 9.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                // Metadata
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = item.channelTitle,
                                                        fontSize = 10.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF2563EB)
                                                    )
                                                    Text(
                                                        text = "${item.viewCount} • ${item.publishedTime}",
                                                        fontSize = 9.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Action Buttons: Play in Room & Add to Queue
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { playSelectedVideo(item) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFDC2626)
                                                    ),
                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "تشغيل في الغرفة",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = TajawalFontFamily,
                                                            color = Color.White
                                                        )
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = { addVideoToQueue(item) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF2563EB)),
                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlaylistAdd,
                                                            contentDescription = null,
                                                            tint = Color(0xFF2563EB),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "إضافة للدور",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = TajawalFontFamily,
                                                            color = Color(0xFF2563EB)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Load More Results from YouTube
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        onClick = {
                                            if (!isExpandedSearchLoading) {
                                                isExpandedSearchLoading = true
                                                coroutineScope.launch {
                                                    val q = if (searchQuery.trim().isNotEmpty()) searchQuery.trim() else "يوتيوب مباشر"
                                                    val realMore = try {
                                                        YouTubeSearchEngine.searchRealYouTube("$q جديد")
                                                    } catch (_: Exception) {
                                                        emptyList()
                                                    }
                                                    if (realMore.isNotEmpty()) {
                                                        var addedCount = 0
                                                        realMore.forEach { extraItem ->
                                                            if (!realSearchResults.any { it.id == extraItem.id }) {
                                                                realSearchResults.add(extraItem)
                                                                addedCount++
                                                            }
                                                        }
                                                        Toast.makeText(context, "تم جلب $addedCount مقطع فيديو إضافي من YouTube 🚀", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isExpandedSearchLoading = false
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFEEF5FF),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isExpandedSearchLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color(0xFFDC2626)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.TravelExplore,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = if (isExpandedSearchLoading) "جاري البحث المباشر في يوتيوب..." else "✨ بحث أكثر وجلب نتائج إضافية من YouTube",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
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
// 7. HELPER COMPOSABLE SUB-COMPONENTS
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
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color(0xFF94A3B8),
            fontFamily = TajawalFontFamily
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A)
        )
    }
}

@Composable
private fun YouTubeSettingsSwitchItem(
    title: String,
    subtitle: String,
    isChecked: Boolean
) {
    var state by remember { mutableStateOf(isChecked) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF64748B)
                )
            }
            Switch(
                checked = state,
                onCheckedChange = { state = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2563EB)
                )
            )
        }
    }
}
