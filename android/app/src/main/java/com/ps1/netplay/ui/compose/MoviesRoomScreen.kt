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

enum class MoviesRoomSubTab {
    PLAYER,
    CHAT,
    CAMERAS,
    INTERCOM,
    USERS,
    SETTINGS
}

data class MoviesChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val time: String,
    val isMe: Boolean = false,
    val avatarColor: Color = Color(0xFF2563EB),
    val imageUrl: String? = null
)

data class MoviesRoomUser(
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

private fun formatTime(seconds: Float): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesRoomScreen(
    roomId: String = "mov_room_default",
    initialStreamUrl: String = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/101.mp4",
    roomTitle: String = "سينما الأفلام والمسلسلات",
    roomCode: String = "#MOV-982142",
    isStealthMode: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Disable click sound effects
    val localView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val prevSound = localView.isSoundEffectsEnabled
        localView.isSoundEffectsEnabled = false
        onDispose { localView.isSoundEffectsEnabled = prevSound }
    }

    // Identity and Roles
    val currentUserId = remember { CloudflareClient.getCurrentUserId(context) }
    val currentUserName = remember { CloudflareClient.getCurrentUsername(context).ifBlank { "أحمد" } }
    val isAppOwner = remember { MoviesRoomManager.isAppOwner(context) }

    val isHost = remember(currentUserId, roomTitle) {
        isAppOwner || roomTitle.contains(currentUserName, ignoreCase = true) || roomId.startsWith("mov_room_")
    }

    // Movies State
    var currentMovie by remember {
        mutableStateOf(
            MoviesSearchEngine.FALLBACK_CATALOG.find { it.streamUrl == initialStreamUrl }
                ?: MovieItem(
                    id = "mov_welad_rizk_3",
                    title = "ولاد رزق 3: القاضية",
                    name = "ولاد رزق 3: القاضية",
                    poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
                    streamUrl = initialStreamUrl,
                    category = "أفلام سينما 2024",
                    duration = "2:04:15",
                    year = "2024"
                )
        )
    }

    var isVideoPlaying by remember { mutableStateOf(true) }
    var currentPlayheadSec by remember { mutableFloatStateOf(0f) }
    var totalDurationSec by remember { mutableFloatStateOf(7455f) }
    var isPlayerBuffering by remember { mutableStateOf(false) }
    var videoVolume by remember { mutableFloatStateOf(1.0f) }

    // Navigation and Dock
    var activeSubTab by remember { mutableStateOf(MoviesRoomSubTab.SETTINGS) }
    var isDarkTheme by remember { mutableStateOf(true) }

    // Search Popup State
    var isSearchPopupOpen by remember { mutableStateOf(false) }
    var searchInputQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(MoviesSearchEngine.FALLBACK_CATALOG) }
    var isSearchingMovies by remember { mutableStateOf(false) }

    // Users and Permissions
    val roomUsers = remember {
        mutableStateListOf(
            MoviesRoomUser(
                id = currentUserId,
                name = currentUserName,
                role = if (isHost) "مضيف الغرفة" else "مشاهد",
                isHost = isHost,
                isOnline = true,
                canChangeVideo = isHost || isAppOwner
            )
        )
    }

    // Messages
    val chatMessages = remember {
        mutableStateListOf(
            MoviesChatMessage(
                id = "msg_welcome",
                sender = "النظام",
                text = "مرحباً بكم في صالة السينما المتزامنة. العرض مباشر وبجودة 4K فائقة.",
                time = "الآن",
                avatarColor = Color(0xFF2563EB)
            )
        )
    }
    var chatInputText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    // VoIP and Voice
    var isMicActive by remember { mutableStateOf(false) }
    var isHandRaised by remember { mutableStateOf(false) }

    // Camera features
    var isMyCameraActive by remember { mutableStateOf(false) }
    var isFrontCam by remember { mutableStateOf(true) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Instant volume sync to player
    LaunchedEffect(videoVolume) {
        val volInt = (videoVolume * 100).toInt().coerceIn(0, 100)
        webViewRef?.evaluateJavascript(
            "if (typeof setPlayerVolume === 'function') { setPlayerVolume($volInt); }",
            null
        )
    }

    // REAL-TIME WEBSOCKET SYNCHRONIZATION CLIENT
    val syncSocket = remember(roomId) {
        MoviesSyncWebSocket(
            context = context,
            roomId = roomId,
            isStealthMode = isStealthMode,
            onMovieChangeReceived = { stream, title, poster ->
                if (stream.isNotBlank()) {
                    currentMovie = MovieItem(
                        id = "synced_${System.currentTimeMillis()}",
                        title = title,
                        name = title,
                        poster = poster,
                        streamUrl = stream,
                        category = "سينما متزامنة"
                    )
                    webViewRef?.evaluateJavascript(
                        "if (typeof loadStreamUrl === 'function') { loadStreamUrl('$stream'); }",
                        null
                    )
                }
            },
            onPlaybackStateReceived = { playState, pos ->
                if (pos >= 0f && kotlin.math.abs(currentPlayheadSec - pos) > 2.0f) {
                    currentPlayheadSec = pos
                    webViewRef?.evaluateJavascript("if (typeof seekTo === 'function') { seekTo($pos); }", null)
                }
                if (playState && !isVideoPlaying) {
                    isVideoPlaying = true
                    webViewRef?.evaluateJavascript("if (typeof playVideo === 'function') { playVideo(); }", null)
                } else if (!playState && isVideoPlaying) {
                    isVideoPlaying = false
                    webViewRef?.evaluateJavascript("if (typeof pauseVideo === 'function') { pauseVideo(); }", null)
                }
            },
            onChatMessageReceived = { newMsg ->
                chatMessages.add(newMsg)
            }
        )
    }

    LaunchedEffect(roomId) {
        syncSocket.connect()
    }

    DisposableEffect(roomId) {
        onDispose {
            syncSocket.disconnect()
        }
    }

    // Function to trigger movies search and open popup
    fun performMoviesSearch(q: String) {
        isSearchingMovies = true
        isSearchPopupOpen = true
        coroutineScope.launch {
            val results = MoviesSearchEngine.searchMovies(context, q)
            searchResults = results
            isSearchingMovies = false
        }
    }

    // Playback control and synchronization
    fun performSeek(targetSeconds: Float) {
        val target = targetSeconds.coerceIn(0f, totalDurationSec.coerceAtLeast(60f))
        currentPlayheadSec = target
        webViewRef?.evaluateJavascript(
            "if (typeof seekTo === 'function') { seekTo($target); }",
            null
        )
        syncSocket.broadcastPlaybackState(isVideoPlaying, target)
    }

    fun togglePlayback() {
        val nextPlay = !isVideoPlaying
        isVideoPlaying = nextPlay

        if (nextPlay) {
            webViewRef?.evaluateJavascript("if (typeof playVideo === 'function') { playVideo(); }", null)
        } else {
            webViewRef?.evaluateJavascript("if (typeof pauseVideo === 'function') { pauseVideo(); }", null)
        }
        syncSocket.broadcastPlaybackState(nextPlay, currentPlayheadSec)
    }

    fun playSelectedMovie(movie: MovieItem) {
        currentMovie = movie
        currentPlayheadSec = 0f
        isVideoPlaying = true
        isSearchPopupOpen = false

        MoviesRoomManager.updateRoomMovie(
            context = context,
            roomId = roomId,
            streamUrl = movie.streamUrl,
            movieTitle = movie.title,
            posterUrl = movie.poster
        )

        webViewRef?.evaluateJavascript(
            "if (typeof loadStreamUrl === 'function') { loadStreamUrl('${movie.streamUrl}'); }",
            null
        )

        syncSocket.broadcastMovieChange(movie.streamUrl, movie.title, movie.poster)
        syncSocket.broadcastPlaybackState(true, 0f)

        Toast.makeText(context, "تم تشغيل: ${movie.title}", Toast.LENGTH_SHORT).show()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        ) { paddingVals ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingVals)
            ) {
                // ====================================================
                // 1. TOP BAR: Back + Room Title + Code + Member Count
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = roomTitle,
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFDC2626))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "4K مباشر",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "رمز الصالة: $roomCode",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Search Icon to open movie search popup
                        IconButton(
                            onClick = {
                                performMoviesSearch(searchInputQuery)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF2563EB), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث أفلام",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Theme Toggle
                        IconButton(
                            onClick = { isDarkTheme = !isDarkTheme },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "تغيير المظهر",
                                tint = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // ====================================================
                // 2. VIDEO PLAYER BOX (CLEAN SYNCHRONIZED STREAM PLAYER)
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(
                            1.dp,
                            if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    val streamUrl = currentMovie.streamUrl
                    val initialVolPercent = (videoVolume * 100).toInt()

                    val playerHtml = remember(streamUrl) {
                        """
                        <!DOCTYPE html>
                        <html lang="ar" dir="rtl">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                html, body {
                                    width: 100%; height: 100%; background: #000000; overflow: hidden;
                                    display: flex; align-items: center; justify-content: center;
                                }
                                video {
                                    width: 100%; height: 100%; object-fit: contain; background: #000;
                                }
                                #touch-shield {
                                    position: absolute; top: 0; left: 0; width: 100%; height: 100%;
                                    z-index: 999; background: transparent; cursor: default;
                                }
                            </style>
                        </head>
                        <body>
                            <video id="mediaPlayer" autoplay playsinline preload="auto" src="$streamUrl"></video>
                            <div id="touch-shield"></div>
                            <script>
                                var video = document.getElementById('mediaPlayer');
                                if (video) {
                                    video.volume = $initialVolPercent / 100.0;
                                    video.play().catch(function(e) {});
                                    video.addEventListener('timeupdate', function() {
                                        if (window.AndroidInterface && typeof window.AndroidInterface.onTimeUpdate === 'function') {
                                            window.AndroidInterface.onTimeUpdate(video.currentTime, video.duration || 0);
                                        }
                                    });
                                }

                                function playVideo() {
                                    if (video) { video.play(); }
                                }
                                function pauseVideo() {
                                    if (video) { video.pause(); }
                                }
                                function seekTo(sec) {
                                    if (video) { video.currentTime = sec; }
                                }
                                function setPlayerVolume(volPercent) {
                                    if (video) {
                                        video.volume = Math.max(0, Math.min(100, volPercent)) / 100.0;
                                    }
                                }
                                function loadStreamUrl(newUrl) {
                                    if (video) {
                                        video.src = newUrl;
                                        video.load();
                                        video.play().catch(function(e) {});
                                    }
                                }
                            </script>
                        </body>
                        </html>
                        """.trimIndent()
                    }

                    AndroidView(
                        factory = { ctx ->
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
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                }
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        return false
                                    }
                                }
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onTimeUpdate(currentTime: Float, duration: Float) {
                                        currentPlayheadSec = currentTime
                                        if (duration > 0f) {
                                            totalDurationSec = duration
                                        }
                                    }
                                }, "AndroidInterface")

                                loadDataWithBaseURL("https://maxshowplayer.site", playerHtml, "text/html", "UTF-8", null)
                                webViewRef = this
                            }
                        },
                        update = { view ->
                            webViewRef = view
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Movie Title Bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentMovie.title,
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${formatTime(currentPlayheadSec)} / ${formatTime(totalDurationSec)}",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ====================================================
                // 3. SEARCH BAR FOR MOVIES & SERIES
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchInputQuery,
                        onValueChange = { searchInputQuery = it },
                        placeholder = {
                            Text(
                                text = "ابحث عن أي فلم أو مسلسل...",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchInputQuery.isNotEmpty()) {
                                IconButton(onClick = { searchInputQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            performMoviesSearch(searchInputQuery)
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                            unfocusedContainerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                            focusedTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                        )
                    )

                    Button(
                        onClick = { performMoviesSearch(searchInputQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text(
                            text = "بحث",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ====================================================
                // 4. MAIN DOCKED CONTAINER CONTENT
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDarkTheme) Color(0xFF1E293B) else Color.White)
                        .border(
                            1.dp,
                            if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    when (activeSubTab) {
                        MoviesRoomSubTab.SETTINGS -> {
                            // Synchronized Settings Tab: Volume slider + Seek + Play/Pause
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "التحكم في البث المتزامن",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                )

                                // Unified Playback Control (Seek -10s, Play/Pause, Seek +10s)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 10.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rewind -10s
                                    IconButton(
                                        onClick = { performSeek(currentPlayheadSec - 10f) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "تأخير 10 ثواني",
                                            tint = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Unified Play/Pause Button
                                    Button(
                                        onClick = { togglePlayback() },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isVideoPlaying) Color(0xFFDC2626) else Color(0xFF2563EB)
                                        ),
                                        modifier = Modifier.size(54.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isVideoPlaying) "إيقاف" else "تشغيل",
                                            tint = Color.White,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }

                                    // Forward +10s
                                    IconButton(
                                        onClick = { performSeek(currentPlayheadSec + 10f) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "تقديم 10 ثواني",
                                            tint = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Volume Slider Container
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
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
                                                imageVector = if (videoVolume > 0f) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                                contentDescription = "الصوت",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "شريط صوت الفلم المباشر",
                                                fontFamily = TajawalFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                            )
                                        }
                                        Text(
                                            text = "${(videoVolume * 100).toInt()}%",
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF2563EB)
                                        )
                                    }

                                    Slider(
                                        value = videoVolume,
                                        onValueChange = { newVol ->
                                            videoVolume = newVol
                                            val volInt = (newVol * 100).toInt().coerceIn(0, 100)
                                            webViewRef?.evaluateJavascript(
                                                "if (typeof setPlayerVolume === 'function') { setPlayerVolume($volInt); }",
                                                null
                                            )
                                        },
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF2563EB),
                                            activeTrackColor = Color(0xFF2563EB),
                                            inactiveTrackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                                        )
                                    )
                                }

                                // Quick Playhead Seek Slider
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "مؤشر التقديم والتأخير اللحظي",
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${formatTime(currentPlayheadSec)} / ${formatTime(totalDurationSec)}",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Slider(
                                        value = currentPlayheadSec.coerceIn(0f, totalDurationSec.coerceAtLeast(1f)),
                                        onValueChange = { newSec ->
                                            performSeek(newSec)
                                        },
                                        valueRange = 0f..totalDurationSec.coerceAtLeast(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF10B981),
                                            activeTrackColor = Color(0xFF10B981),
                                            inactiveTrackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                        }

                        MoviesRoomSubTab.CHAT -> {
                            // Chat Tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = chatListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(chatMessages, key = { it.id }) { msg ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (msg.isMe) Color(0xFF2563EB)
                                                        else if (isDarkTheme) Color(0xFF0F172A)
                                                        else Color(0xFFF1F5F9)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Column {
                                                    if (!msg.isMe) {
                                                        Text(
                                                            text = msg.sender,
                                                            fontFamily = TajawalFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF38BDF8)
                                                        )
                                                    }
                                                    Text(
                                                        text = msg.text,
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 13.sp,
                                                        color = if (msg.isMe) Color.White else if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BasicTextField(
                                        value = chatInputText,
                                        onValueChange = { chatInputText = it },
                                        textStyle = TextStyle(
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 13.sp,
                                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                        ),
                                        cursorBrush = SolidColor(Color(0xFF2563EB)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    )

                                    Button(
                                        onClick = {
                                            val t = chatInputText.trim()
                                            if (t.isNotEmpty()) {
                                                chatMessages.add(
                                                    MoviesChatMessage(
                                                        id = "msg_${System.currentTimeMillis()}",
                                                        sender = currentUserName,
                                                        text = t,
                                                        time = "الآن",
                                                        isMe = true
                                                    )
                                                )
                                                syncSocket.broadcastChatMessage(t)
                                                chatInputText = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال", tint = Color.White)
                                    }
                                }
                            }
                        }

                        MoviesRoomSubTab.USERS -> {
                            // Users Tab with permissions and role
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "الأعضاء المتواجدون (${roomUsers.size})",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(roomUsers, key = { it.id }) { u ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(u.avatarBg, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = u.name.take(1),
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = u.name,
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = u.role,
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            if (u.isHost) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF2563EB))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "المضيف",
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 10.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        MoviesRoomSubTab.INTERCOM -> {
                            // Mic / Voice Intercom Tab
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { isMicActive = !isMicActive },
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            if (isMicActive) Color(0xFF10B981) else Color(0xFFEF4444),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                                        contentDescription = "المايكروفون",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (isMicActive) "المايكروفون يعمل - تحدث مع الجميع" else "المايكروفون مغلق - اضغط للتحدث",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }

                        MoviesRoomSubTab.CAMERAS -> {
                            // Cameras Tab
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "كاميرا",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "بث الكاميرا التفاعلي مع الصالة",
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        MoviesRoomSubTab.PLAYER -> {
                            // Library Tab
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(MoviesSearchEngine.FALLBACK_CATALOG, key = { it.id }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { playSelectedMovie(item) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AsyncImage(
                                            model = item.poster,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontFamily = TajawalFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${item.category} • ${item.year}",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Button(
                                            onClick = { playSelectedMovie(item) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("تشغيل", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ====================================================
                // 5. BOTTOM DOCK ICON BAR (MATCHING YOUTUBE DOCK EXACTLY)
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .background(
                            if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoviesDockIconButton(
                        icon = Icons.Default.Settings,
                        label = "الضبط",
                        isActive = activeSubTab == MoviesRoomSubTab.SETTINGS,
                        isDark = isDarkTheme,
                        onClick = { activeSubTab = MoviesRoomSubTab.SETTINGS }
                    )

                    // Members count icon with RED NUMBER ONLY (no red background box/border as requested)
                    MoviesDockIconButton(
                        icon = Icons.Default.Groups,
                        label = "الأعضاء",
                        badgeCount = roomUsers.size,
                        isActive = activeSubTab == MoviesRoomSubTab.USERS,
                        isDark = isDarkTheme,
                        onClick = { activeSubTab = MoviesRoomSubTab.USERS }
                    )

                    MoviesDockIconButton(
                        icon = if (isMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                        label = "الصوت",
                        isActive = activeSubTab == MoviesRoomSubTab.INTERCOM,
                        isDark = isDarkTheme,
                        onClick = { activeSubTab = MoviesRoomSubTab.INTERCOM }
                    )

                    MoviesDockIconButton(
                        icon = Icons.Default.Chat,
                        label = "الدردشة",
                        badgeCount = chatMessages.size,
                        isActive = activeSubTab == MoviesRoomSubTab.CHAT,
                        isDark = isDarkTheme,
                        onClick = { activeSubTab = MoviesRoomSubTab.CHAT }
                    )

                    MoviesDockIconButton(
                        icon = Icons.Default.VideoLibrary,
                        label = "المكتبة",
                        isActive = activeSubTab == MoviesRoomSubTab.PLAYER,
                        isDark = isDarkTheme,
                        onClick = { activeSubTab = MoviesRoomSubTab.PLAYER }
                    )
                }
            }

            // ====================================================
            // 6. MOVIES & SERIES SEARCH RESULTS POPUP MODAL (THEMED)
            // ====================================================
            if (isSearchPopupOpen) {
                Dialog(
                    onDismissRequest = { isSearchPopupOpen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .fillMaxHeight(0.78f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkTheme) Color(0xFF1E293B) else Color.White)
                            .border(
                                1.dp,
                                if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Popup Header
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
                                        imageVector = Icons.Default.Theaters,
                                        contentDescription = "أفلام",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "نتائج البحث عن الأفلام والمسلسلات",
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                    )
                                }

                                IconButton(
                                    onClick = { isSearchPopupOpen = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "اضغط على أي فلم أو مسلسل لنقله مباشرة إلى شاشة العرض ومشاركته مع الجميع بشكل متزامن:",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isSearchingMovies) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF2563EB), modifier = Modifier.size(32.dp))
                                }
                            } else if (searchResults.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لم يتم العثور على نتائج مطابقة",
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(searchResults, key = { it.id }) { movie ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { playSelectedMovie(movie) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                AsyncImage(
                                                    model = movie.poster,
                                                    contentDescription = movie.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(width = 60.dp, height = 75.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Black)
                                                )

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = movie.title,
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "${movie.category} • سنة ${movie.year}",
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFFF59E0B))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "★ ${movie.rating}",
                                                                fontFamily = TajawalFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp,
                                                                color = Color.White
                                                            )
                                                        }
                                                        Text(
                                                            text = movie.duration,
                                                            fontFamily = TajawalFontFamily,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = { playSelectedMovie(movie) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "عرض",
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
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
            }
        }
    }
}

@Composable
private fun MoviesDockIconButton(
    icon: ImageVector,
    label: String,
    badgeCount: Int? = null,
    isActive: Boolean = false,
    isDark: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) Color(0xFF2563EB) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(22.dp)
                )

                // Member count badge: RED NUMBER ONLY (no red background box/border as requested)
                if (badgeCount != null && badgeCount > 0) {
                    Text(
                        text = "$badgeCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                    )
                }
            }
            Text(
                text = label,
                fontFamily = TajawalFontFamily,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp,
                color = if (isActive) Color(0xFF2563EB) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}
