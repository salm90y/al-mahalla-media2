package com.ps1.netplay.ui.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

// ----------------------------------------------------
// 1. DATA MODELS FOR LIVE PUBLIC YOUTUBE ROOMS
// ----------------------------------------------------
data class PublicYouTubeRoom(
    val roomId: String,
    val roomCode: String,
    val title: String,
    val hostName: String,
    val hostAvatarBg: Color,
    val currentVideoTitle: String,
    val videoId: String,
    val thumbnailUrl: String,
    val viewersCount: Int,
    val isLive: Boolean = true,
    val durationText: String = "مباشر",
    val pingMs: String = "12ms",
    val privacyMode: RoomPrivacyMode = RoomPrivacyMode.PUBLIC
)

val INITIAL_PUBLIC_YOUTUBE_ROOMS = listOf(
    PublicYouTubeRoom(
        roomId = "room_1",
        roomCode = "#YT-1042",
        title = "سهرة وثائقيات وتكنولوجيا وفضاء 🚀",
        hostName = "م. حسام العراقي",
        hostAvatarBg = Color(0xFF2563EB),
        currentVideoTitle = "وثائقي تلسكوب جيمس ويب الفضائي: أعمق صور نشأة الكون",
        videoId = "space_james_webb",
        thumbnailUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&auto=format&fit=crop&q=80",
        viewersCount = 142,
        isLive = true,
        durationText = "24:18",
        pingMs = "12ms • Cloudflare"
    ),
    PublicYouTubeRoom(
        roomId = "room_2",
        roomCode = "#YT-8821",
        title = "مجلس التلاوات الخاشعة والقرآن الكريم 4K 🕋",
        hostName = "الشيخ قاسم",
        hostAvatarBg = Color(0xFF10B981),
        currentVideoTitle = "تلاوة خاشعة ومريحة للأعصاب من سورة مريم بصوت القارئ إسلام صبحي",
        videoId = "yK4U4XkMhFk",
        thumbnailUrl = "https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=600&auto=format&fit=crop&q=80",
        viewersCount = 320,
        isLive = true,
        durationText = "32:45",
        pingMs = "8ms • خوادم بغداد"
    ),
    PublicYouTubeRoom(
        roomId = "room_3",
        roomCode = "#YT-5519",
        title = "بودكاست ونقاشات الذكاء الاصطناعي 2026 🤖",
        hostName = "أحمد المطور",
        hostAvatarBg = Color(0xFF8B5CF6),
        currentVideoTitle = "بودكاست فنجان: أسرار بناء المستقبل والذكاء الاصطناعي",
        videoId = "podcast_101",
        thumbnailUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&auto=format&fit=crop&q=80",
        viewersCount = 85,
        isLive = false,
        durationText = "1:42:10",
        pingMs = "14ms • Cloudflare"
    ),
    PublicYouTubeRoom(
        roomId = "room_4",
        roomCode = "#YT-7730",
        title = "أهداف وجنون الساحرة المستديرة العالمية ⚽",
        hostName = "كابتن سجاد",
        hostAvatarBg = Color(0xFFDC2626),
        currentVideoTitle = "تحديات ومهارات كروية استثنائية: أفضل أهداف وتمريرات الموسم",
        videoId = "esports_championship_live",
        thumbnailUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=600&auto=format&fit=crop&q=80",
        viewersCount = 210,
        isLive = true,
        durationText = "17:45",
        pingMs = "15ms • البث السريع"
    ),
    PublicYouTubeRoom(
        roomId = "room_5",
        roomCode = "#YT-3312",
        title = "استرخاء ومناظر طبيعية ساحرة 4K 🌿",
        hostName = "نور الزهراء",
        hostAvatarBg = Color(0xFF06B6D4),
        currentVideoTitle = "رحلة ساحرة عبر جبال الألب السويسرية والطبيعة الخلابة 60FPS",
        videoId = "nature_relax_4k",
        thumbnailUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80",
        viewersCount = 94,
        isLive = false,
        durationText = "45:00",
        pingMs = "10ms • Cloudflare"
    )
)

// ----------------------------------------------------
// 2. MAIN COMPOSABLE: YouTubeLobbyScreen
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeLobbyScreen(
    onBack: () -> Unit,
    onEnterRoom: (roomId: String, videoId: String, roomTitle: String) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    // Audio beep player
    fun playButtonBeep(type: Int = ToneGenerator.TONE_PROP_BEEP) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
            toneGen.startTone(type, 50)
        } catch (_: Exception) {}
    }

    // Live Public Rooms State
    val publicRooms = remember {
        mutableStateListOf<PublicYouTubeRoom>().apply {
            addAll(INITIAL_PUBLIC_YOUTUBE_ROOMS)
        }
    }

    // Dialog States
    var isCreateRoomDialogOpen by remember { mutableStateOf(false) }
    var isJoinRoomDialogOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Dialog Inputs
    var newRoomTitle by remember { mutableStateOf("") }
    var newRoomPrivacy by remember { mutableStateOf(RoomPrivacyMode.PUBLIC) }
    var joinRoomCodeInput by remember { mutableStateOf("") }

    // Filtered rooms
    val filteredRooms = remember(searchQuery, publicRooms.size) {
        if (searchQuery.trim().isEmpty()) {
            publicRooms.toList()
        } else {
            publicRooms.filter {
                it.title.contains(searchQuery.trim(), ignoreCase = true) ||
                it.currentVideoTitle.contains(searchQuery.trim(), ignoreCase = true) ||
                it.hostName.contains(searchQuery.trim(), ignoreCase = true) ||
                it.roomCode.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    // Total online viewers across all public rooms
    val totalOnlineViewers = remember(publicRooms.size) {
        publicRooms.sumOf { it.viewersCount }
    }

    // Full RTL Layout
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // ====================================================
                // 1. TOP HEADER: Back Button + Title + Status + Action Icons
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Title & Online pulse
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                text = "سينما اليوتيوب",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Text(
                            text = "$totalOnlineViewers متصل عبر سحابة Cloudflare",
                            fontSize = 10.sp,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Top Quick Action Icons (Search + Refresh)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Search icon button
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Refresh list icon button
                        IconButton(
                            onClick = {
                                playButtonBeep()
                                Toast.makeText(context, "تم تحديث قائمة الغرف العامة ✨", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Expandable Search Bar
                AnimatedVisibility(visible = isSearchActive) {
                    Column {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث في الغرف العامة أو اسم المضيف...", fontSize = 11.sp, fontFamily = TajawalFontFamily) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFDBEAFE)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ====================================================
                // 2. ULTRA-MODERN ACTION CARD (إنشاء غرفة + انضمام إلى غرفة برموز بدون كتابة)
                // ====================================================
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "المشاهدة الجماعية المتزامنة",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF1E3A8A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "أنشئ غرفتك الخاصة أو انضم لأصدقائك بضغطة زر",
                                    fontSize = 11.sp,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // Cloudflare live tag
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEEF5FF),
                                border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "12ms ⚡",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Modern Row of Icon-Only Action Buttons (أنيقة وصغيرة برموز بدون كتابة)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. زر إنشاء غرفة (Create Room: Red Gradient Icon Button)
                            Surface(
                                onClick = {
                                    playButtonBeep()
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isCreateRoomDialogOpen = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFDC2626),
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(width = 72.dp, height = 48.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "إنشاء غرفة",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // 2. زر انضمام إلى غرفة (Join Room by Code: Primary Blue Icon Button)
                            Surface(
                                onClick = {
                                    playButtonBeep()
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isJoinRoomDialogOpen = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF2563EB),
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(width = 72.dp, height = 48.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = "انضمام إلى غرفة",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // 3. زر بث عشوائي سريع (Quick Instant Stream: Emerald Icon Button)
                            Surface(
                                onClick = {
                                    playButtonBeep()
                                    val randomRoom = publicRooms.random()
                                    onEnterRoom(randomRoom.roomId, randomRoom.videoId, randomRoom.title)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF10B981),
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(width = 72.dp, height = 48.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "دخول سريع",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // 4. زر مشاركة الرابط (Share Hub: Indigo Icon Button)
                            Surface(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("سينما اليوتيوب", "انضم إلينا الآن في سينما اليوتيوب التفاعلية: #YT-9024 🎬")
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ دعوة سينما اليوتيوب للمشاركة! 🍿", Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.size(width = 72.dp, height = 48.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "مشاركة",
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ====================================================
                // 3. LIST OF PUBLIC ROOMS CURRENTLY ONLINE (الغرف العامة أونلاين حالياً)
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
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
                                .background(Color(0xFFDC2626), CircleShape)
                        )
                        Text(
                            text = "الغرف العامة النشطة (${filteredRooms.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Text(
                        text = "تحديث حي 🟢",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Online Rooms LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRooms, key = { it.roomId }) { room ->
                        Surface(
                            onClick = {
                                playButtonBeep()
                                onEnterRoom(room.roomId, room.videoId, room.title)
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Room Video Thumbnail with Live Badge
                                    Box(
                                        modifier = Modifier
                                            .size(width = 112.dp, height = 76.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        AsyncImage(
                                            model = room.thumbnailUrl,
                                            contentDescription = room.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Duration / Live Badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .background(
                                                    if (room.isLive) Color(0xCCDC2626) else Color(0xCC000000),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = room.durationText,
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Room Details
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = room.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = room.currentVideoTitle,
                                            fontSize = 11.sp,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Host Info + Ping
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .background(room.hostAvatarBg, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = room.hostName.take(1),
                                                    fontSize = 9.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = room.hostName,
                                                fontSize = 10.sp,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFF475569)
                                            )
                                            Text(
                                                text = "•",
                                                fontSize = 10.sp,
                                                color = Color(0xFFCBD5E1)
                                            )
                                            Text(
                                                text = room.pingMs,
                                                fontSize = 10.sp,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Bottom Row: Viewers Count + Room Code + Join Icon Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Viewers Counter Badge
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color(0xFFDC2626), CircleShape)
                                            )
                                            Text(
                                                text = "${room.viewersCount} متصل الآن",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }

                                    // Room Code Chip
                                    Surface(
                                        onClick = {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("كود الغرفة", room.roomCode)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "تم نسخ كود الغرفة (${room.roomCode}) بنجاح!", Toast.LENGTH_SHORT).show()
                                            } catch (_: Exception) {}
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFEEF5FF),
                                        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "نسخ",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = room.roomCode,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                    }

                                    // Enter Room Icon Button (Sleek icon only)
                                    IconButton(
                                        onClick = {
                                            playButtonBeep()
                                            onEnterRoom(room.roomId, room.videoId, room.title)
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(Color(0xFF2563EB), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack, // Back in RTL points forward into the room
                                            contentDescription = "دخول الغرفة",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ====================================================
        // 4. CREATE ROOM MODAL DIALOG (نافذة إنشاء غرفة جديدة)
        // ====================================================
        if (isCreateRoomDialogOpen) {
            Dialog(
                onDismissRequest = { isCreateRoomDialogOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Dialog Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إنشاء غرفة مشاهدة جديدة 🎬",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF0F172A)
                            )
                            IconButton(
                                onClick = { isCreateRoomDialogOpen = false },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }

                        // Room Name Input
                        OutlinedTextField(
                            value = newRoomTitle,
                            onValueChange = { newRoomTitle = it },
                            label = { Text("اسم الغرفة", fontSize = 12.sp, fontFamily = TajawalFontFamily) },
                            placeholder = { Text("مثال: سهرة أفلام ومقاطع ممتعة", fontSize = 11.sp, fontFamily = TajawalFontFamily) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        // Privacy Mode (رموز بدون كتابة)
                        Text(
                            text = "خصوصية المشاهدة:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF1E3A8A)
                        )

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
                                val isSelected = newRoomPrivacy == mode
                                Surface(
                                    onClick = { newRoomPrivacy = mode },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.size(46.dp)
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

                        Spacer(modifier = Modifier.height(4.dp))

                        // Confirm Create Button
                        Button(
                            onClick = {
                                val generatedCode = "#YT-${(1000..9999).random()}"
                                val roomTitle = if (newRoomTitle.trim().isNotEmpty()) newRoomTitle.trim() else "غرفة مشاهدة يوتيوب"
                                val newRoom = PublicYouTubeRoom(
                                    roomId = "room_${System.currentTimeMillis()}",
                                    roomCode = generatedCode,
                                    title = roomTitle,
                                    hostName = "أنا (المضيف)",
                                    hostAvatarBg = Color(0xFF2563EB),
                                    currentVideoTitle = "بث مباشر 4K",
                                    videoId = "jfKfPfyJRdk",
                                    thumbnailUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=600&auto=format&fit=crop&q=80",
                                    viewersCount = 1,
                                    privacyMode = newRoomPrivacy
                                )
                                publicRooms.add(0, newRoom)
                                isCreateRoomDialogOpen = false
                                playButtonBeep()
                                Toast.makeText(context, "تم إنشاء الغرفة ($generatedCode) بنجاح! 🍿", Toast.LENGTH_SHORT).show()
                                onEnterRoom(newRoom.roomId, newRoom.videoId, newRoom.title)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text(
                                text = "إنشاء الغرفة وبدء المشاهدة 🚀",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ====================================================
        // 5. JOIN ROOM BY CODE MODAL DIALOG (نافذة الانضمام بكود)
        // ====================================================
        if (isJoinRoomDialogOpen) {
            Dialog(
                onDismissRequest = { isJoinRoomDialogOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الانضمام إلى غرفة بكود 🔑",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF0F172A)
                            )
                            IconButton(
                                onClick = { isJoinRoomDialogOpen = false },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }

                        // Room Code Input
                        OutlinedTextField(
                            value = joinRoomCodeInput,
                            onValueChange = { joinRoomCodeInput = it },
                            label = { Text("كود الغرفة", fontSize = 12.sp, fontFamily = TajawalFontFamily) },
                            placeholder = { Text("مثال: #YT-9024", fontSize = 11.sp, fontFamily = TajawalFontFamily) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        if (clip.isNotEmpty()) {
                                            joinRoomCodeInput = clip
                                        }
                                    } catch (_: Exception) {}
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                }
                            }
                        )

                        // Confirm Join Button
                        Button(
                            onClick = {
                                val code = joinRoomCodeInput.trim()
                                if (code.isNotEmpty()) {
                                    val matched = publicRooms.firstOrNull { it.roomCode.equals(code, ignoreCase = true) }
                                    isJoinRoomDialogOpen = false
                                    playButtonBeep()
                                    if (matched != null) {
                                        Toast.makeText(context, "تم الانضمام للغرفة بنجاح! 🎬", Toast.LENGTH_SHORT).show()
                                        onEnterRoom(matched.roomId, matched.videoId, matched.title)
                                    } else {
                                        Toast.makeText(context, "جاري الدخول إلى الغرفة الخاصة ($code)...", Toast.LENGTH_SHORT).show()
                                        onEnterRoom("room_custom", "yK4U4XkMhFk", "غرفة $code")
                                    }
                                } else {
                                    Toast.makeText(context, "يرجى كتابة كود الغرفة أولاً", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text(
                                text = "انضمام الآن 🎬",
                                fontSize = 13.sp,
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
