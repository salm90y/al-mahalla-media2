package com.ps1.netplay.ui.compose

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. DATA MODELS & ENUMS
// ----------------------------------------------------
enum class GamesRoomSubTab {
    CHAT,
    CAMERA,
    INTERCOM,
    GAMEPAD,
    USERS,
    SETTINGS
}

data class GameRomItem(
    val id: String,
    val title: String,
    val englishTitle: String,
    val size: String,
    val genre: String,
    val rating: String,
    val isDownloaded: Boolean = false,
    val bannerColor: Color = Color(0xFF2563EB)
)

data class RoomChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val time: String,
    val isMe: Boolean = false,
    val avatarColor: Color = Color(0xFF2563EB)
)

data class RoomCameraFeed(
    val id: String,
    val name: String,
    val location: String,
    val resolution: String,
    val isOnline: Boolean = true
)

data class RoomUserItem(
    val id: String,
    val name: String,
    val role: String,
    val isOnline: Boolean = true,
    val avatarBg: Color = Color(0xFF2563EB)
)

// Default Cloudflare ROMs Library
val CLOUDFLARE_ROMS_LIBRARY = listOf(
    GameRomItem("tekken3", "تيكن 3", "Tekken 3", "485 MB", "قتال • أكشن", "9.8", true, Color(0xFFDC2626)),
    GameRomItem("crash3", "كراش بانديكوت 3: واربد", "Crash Bandicoot 3", "340 MB", "مغامرات • منصات", "9.7", true, Color(0xFFF97316)),
    GameRomItem("we2002", "وينينج إليفن 2002 (اليابانية)", "Winning Eleven 2002", "512 MB", "رياضة • كرة قدم", "9.9", false, Color(0xFF16A34A)),
    GameRomItem("pepsiman", "بيبسي مان", "Pepsiman", "120 MB", "ركض • ترفيه", "9.4", false, Color(0xFF2563EB)),
    GameRomItem("re2", "ريزدنت إيفل 2", "Resident Evil 2", "750 MB", "رعب • بقاء", "9.8", false, Color(0xFF475569)),
    GameRomItem("jackiechan", "جاكي شان: ستانت ماستر", "Jackie Chan Stuntmaster", "410 MB", "قتال شوارع • مغامرات", "9.6", false, Color(0xFFEA580C))
)

// ----------------------------------------------------
// 2. MAIN COMPOSABLE: GamesRoomScreen
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesRoomScreen(
    onBack: () -> Unit,
    onStartPs1: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Active sub-tab state (Gamepad is default matching screenshot)
    var activeSubTab by remember { mutableStateOf(GamesRoomSubTab.GAMEPAD) }

    // ROMs Library Modal State
    var isRomsModalOpen by remember { mutableStateOf(false) }
    val romsList = remember { mutableStateListOf<GameRomItem>().apply { addAll(CLOUDFLARE_ROMS_LIBRARY) } }
    var activeGame by remember { mutableStateOf<GameRomItem?>(romsList.firstOrNull { it.isDownloaded }) }

    // Interactive 60fps PS1 Combat Emulator Engine State (Toggled by tapping top card)
    var isLiveEngineActive by remember { mutableStateOf(false) }
    var playerHp by remember { mutableStateOf(100) }
    var enemyHp by remember { mutableStateOf(85) }
    var roundTimer by remember { mutableStateOf(99) }
    var gameScore by remember { mutableStateOf(14200) }
    var comboCount by remember { mutableStateOf(0) }
    var activePlayerAction by remember { mutableStateOf("IDLE") } // "IDLE", "PUNCH", "KICK", "SPECIAL", "JUMP"
    var feedbackText by remember { mutableStateOf("اضغط الأزرار لبدء القتال!") }

    // Sound & Settings state
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var privacyEnabled by remember { mutableStateOf(false) }
    var graphicsResolution by remember { mutableStateOf("2x HD (1080p)") }
    var rendererEngine by remember { mutableStateOf("Vulkan Core") }
    var screenShader by remember { mutableStateOf("CRT Scanlines") }
    var audioSync by remember { mutableStateOf("DSP Low-Latency") }

    // Chat messages state
    val chatMessages = remember {
        mutableStateListOf(
            RoomChatMessage("1", "أحمد (المضيف)", "مرحباً بالجميع في غرفة الألعاب! 🎮", "10:14 م", false, Color(0xFF2563EB)),
            RoomChatMessage("2", "سارة", "سأفوز عليك هذه المرة في تيكن 3! 🔥", "10:15 م", false, Color(0xFF10B981)),
            RoomChatMessage("3", "محمد", "سيرفر NetPlay مستقر والبنق 18ms ⚡", "10:16 م", false, Color(0xFF8B5CF6)),
            RoomChatMessage("4", "ليان", "أنا جاهزة للجولة القادمة!", "10:17 م", false, Color(0xFFEC4899))
        )
    }
    var chatInputText by remember { mutableStateOf("") }

    // Camera feeds state
    val cameraFeeds = listOf(
        RoomCameraFeed("cam3", "كاميرا 3", "غرفة الجلوس", "1080p • 60fps", true),
        RoomCameraFeed("cam2", "كاميرا 2", "المطبخ", "720p • 30fps", true),
        RoomCameraFeed("cam1", "كاميرا 1", "غرفة النوم", "1080p • 60fps", true)
    )
    var selectedCameraId by remember { mutableStateOf<String?>("cam3") }

    // Walkie-Talkie State
    var isIntercomTalking by remember { mutableStateOf(false) }

    // Users list state
    val roomUsers = listOf(
        RoomUserItem("u1", "أحمد", "مضيف الغرفة", true, Color(0xFF2563EB)),
        RoomUserItem("u2", "سارة", "لاعب 2", true, Color(0xFF10B981)),
        RoomUserItem("u3", "محمد", "مشاهد", true, Color(0xFF8B5CF6)),
        RoomUserItem("u4", "ليان", "لاعب 3", true, Color(0xFFEC4899))
    )

    // Audio beep player
    fun playButtonBeep(type: Int = ToneGenerator.TONE_PROP_BEEP) {
        if (!soundEnabled) return
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
            toneGen.startTone(type, 60)
        } catch (_: Exception) {}
    }

    // Trigger action in live engine
    fun triggerEngineAction(action: String) {
        if (vibrationEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        playButtonBeep()
        activePlayerAction = action

        when (action) {
            "TRIANGLE" -> {
                feedbackText = "ركلة علوية قاضية! (Triangle Hit)"
                comboCount += 1
                enemyHp = (enemyHp - 8).coerceAtLeast(0)
                gameScore += 250
            }
            "CIRCLE" -> {
                feedbackText = "لكمة سريعة! (Circle Strike)"
                comboCount += 1
                enemyHp = (enemyHp - 6).coerceAtLeast(0)
                gameScore += 180
            }
            "CROSS" -> {
                feedbackText = "حركة قفز ودفاع! (Cross Jump)"
                comboCount = 0
                gameScore += 100
            }
            "SQUARE" -> {
                feedbackText = "ضربة قاضية خاصة! (Square Special)"
                comboCount += 2
                enemyHp = (enemyHp - 12).coerceAtLeast(0)
                gameScore += 400
            }
            "UP" -> feedbackText = "تحرك للأعلى / قفز"
            "DOWN" -> feedbackText = "انحناء ودفاع سفلي"
            "LEFT" -> feedbackText = "تراجع للخلف"
            "RIGHT" -> feedbackText = "تقدم للأمام نحو الخصم"
            "L1" -> feedbackText = "صد هجوم (L1 Guard)"
            "L2" -> feedbackText = "تبديل وضع الكاميرا (L2 View)"
            "R1" -> feedbackText = "هجوم مركب (R1 Combo)"
            "R2" -> feedbackText = "طاقة إضافية (R2 Turbo)"
            "SELECT" -> feedbackText = "قائمة الخيارات (Select)"
            "START" -> feedbackText = "إيقاف / استئناف (Start)"
        }

        if (enemyHp <= 0) {
            feedbackText = "🏆 فوز ساحق! K.O! تم حسم الجولة!"
            coroutineScope.launch {
                delay(2000)
                enemyHp = 100
                playerHp = 100
                comboCount = 0
            }
        }

        coroutineScope.launch {
            delay(350)
            if (activePlayerAction == action) {
                activePlayerAction = "IDLE"
            }
        }
    }

    // Background and full layout container
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F8FD)) // Match soft icy-blue/white background
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ====================================================
                // 1. TOP HEADER: Title "غرفة الألعاب" (تصغير) + كود الغرفة القابل للنسخ + زر المكتبة السحابية الصغير
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Center: Room Code Chip (كود الغرفة مع ميزة النسخ المباشر للمشاركة)
                    Surface(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("كود الغرفة", "#PS1-8842")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود الغرفة (#PS1-8842) بنجاح! شاركه مع أصدقائك 🎮", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF5FF),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ كود الغرفة",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "#PS1-8842",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }

                    // Title + Small Icon Button beside it (NO TEXT, opens Cloudflare R2 ROMs Hub)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Small icon button without name/text as requested
                        IconButton(
                            onClick = { isRomsModalOpen = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFEEF5FF), CircleShape)
                                .border(1.dp, Color(0xFFDBEAFE), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = "مكتبة الرومات",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Room Name - Shrunken as requested (تصغير اسم الغرفة)
                        Text(
                            text = "غرفة الألعاب",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ====================================================
                // 2. LARGE TOP DISPLAY CARD (تكبير مربع الألعاب إلى الأسفل - 275.dp)
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(275.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFEEF5FF), Color(0xFFE4EFFF))
                            )
                        )
                        .border(1.5.dp, Color(0xFFDBEAFE), RoundedCornerShape(32.dp))
                        .clickable { isLiveEngineActive = !isLiveEngineActive },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isLiveEngineActive) {
                        // ====================================================
                        // 2A. ILLUSTRATED CONTROLLER VIEW (100% MATCHING SCREENSHOT)
                        // ====================================================
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Soft background circular glow
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .offset(x = (-30).dp, y = (-10).dp)
                                    .background(Color(0x203B82F6), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .offset(x = 40.dp, y = (-20).dp)
                                    .background(Color(0x183B82F6), CircleShape)
                            )

                            // Underneath soft oval shadow
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(18.dp)
                                    .offset(y = 44.dp)
                                    .background(Color(0x3093C5FD), CircleShape)
                            )

                            // Radiating burst pills on upper left & right
                            // Upper Left 3 Pills
                            Row(
                                modifier = Modifier
                                    .offset(x = (-56).dp, y = (-42).dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(14.dp)
                                        .background(Color(0xFF60A5FA), RoundedCornerShape(3.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(10.dp)
                                        .offset(y = 6.dp)
                                        .background(Color(0xFF93C5FD), RoundedCornerShape(3.dp))
                                )
                            }
                            // Upper Right 3 Pills
                            Row(
                                modifier = Modifier
                                    .offset(x = 56.dp, y = (-42).dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(10.dp)
                                        .offset(y = 6.dp)
                                        .background(Color(0xFF93C5FD), RoundedCornerShape(3.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(14.dp)
                                        .background(Color(0xFF60A5FA), RoundedCornerShape(3.dp))
                                )
                            }

                            // Center Controller Graphic
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(78.dp)
                                    .background(Color(0xFF3B82F6), RoundedCornerShape(26.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // D-Pad White Plus
                                    Box(
                                        modifier = Modifier.size(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(7.dp)
                                                .height(22.dp)
                                                .background(Color.White, RoundedCornerShape(3.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(22.dp)
                                                .height(7.dp)
                                                .background(Color.White, RoundedCornerShape(3.dp))
                                        )
                                    }

                                    // Center dots
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(Color(0xCCFFFFFF), CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(Color(0xCCFFFFFF), CircleShape)
                                        )
                                    }

                                    // Action 4 White Dots
                                    Box(
                                        modifier = Modifier.size(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .offset(y = (-8).dp)
                                                .background(Color.White, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .offset(x = 8.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .offset(y = 8.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .offset(x = (-8).dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                            }

                            // Tap to switch to live playable view
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp)
                                    .background(Color(0x40FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "اضغط لتشغيل المحاكي التفاعلي 🎮",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                        }
                    } else {
                        // ====================================================
                        // 2B. LIVE PS1 COMBAT ENGINE VIEWPORT (60 FPS SIMULATOR)
                        // ====================================================
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top HUD: Player HP, Timer, Enemy HP
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Player HP Bar (Green)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "أحمد (JIN)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF334155))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(playerHp / 100f)
                                                    .fillMaxHeight()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(Color(0xFF10B981), Color(0xFF34D399))
                                                        )
                                                    )
                                            )
                                        }
                                    }

                                    // Round Timer Badge
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .size(34.dp)
                                            .background(Color(0xFFEAB308), CircleShape)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$roundTimer",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }

                                    // Enemy HP Bar (Red)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "PAUL (CPU)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF87171)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF334155))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(enemyHp / 100f)
                                                    .fillMaxHeight()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(Color(0xFFEF4444), Color(0xFFF87171))
                                                        )
                                                    )
                                            )
                                        }
                                    }
                                }

                                // Arena Combat Center Animation
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Player Fighter Avatar
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .background(
                                                        if (activePlayerAction != "IDLE") Color(0xFF2563EB) else Color(0xFF1E293B),
                                                        CircleShape
                                                    )
                                                    .border(2.dp, Color(0xFF60A5FA), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SportsKabaddi,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Text(
                                                text = if (activePlayerAction != "IDLE") activePlayerAction else "مستعد",
                                                fontSize = 9.sp,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Action / Sparks / VS Indicator
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = if (comboCount > 0) "COMBO x$comboCount!" else "VS",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (comboCount > 0) Color(0xFFFBBF24) else Color.White
                                            )
                                            Text(
                                                text = "النقاط: $gameScore",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        // Enemy Fighter Avatar
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .background(Color(0xFF7F1D1D), CircleShape)
                                                    .border(2.dp, Color(0xFFF87171), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SportsMartialArts,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Text(
                                                text = if (enemyHp <= 0) "K.O" else "دفاع",
                                                fontSize = 9.sp,
                                                color = Color(0xFFF87171),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Bottom Action Text
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = feedbackText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDE047)
                                    )
                                    Text(
                                        text = "اضغط للعودة للشعار ↺",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ====================================================
                // 3. FLOATING DOCK (6 ICONS PILL: Chat, Camera, Intercom, Gamepad, Users, Settings)
                // ====================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2EAFD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Chat Tab
                        DockIconButton(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            isActive = activeSubTab == GamesRoomSubTab.CHAT,
                            onClick = { activeSubTab = GamesRoomSubTab.CHAT }
                        )

                        // 2. Camera Tab
                        DockIconButton(
                            icon = Icons.Outlined.PhotoCamera,
                            isActive = activeSubTab == GamesRoomSubTab.CAMERA,
                            onClick = { activeSubTab = GamesRoomSubTab.CAMERA }
                        )

                        // 3. Walkie-Talkie / Intercom Tab
                        DockIconButton(
                            icon = Icons.Outlined.PhoneInTalk,
                            isActive = activeSubTab == GamesRoomSubTab.INTERCOM,
                            onClick = { activeSubTab = GamesRoomSubTab.INTERCOM }
                        )

                        // 4. Gamepad Tab (Active by default matching screenshot)
                        DockIconButton(
                            icon = Icons.Outlined.SportsEsports,
                            isActive = activeSubTab == GamesRoomSubTab.GAMEPAD,
                            onClick = { activeSubTab = GamesRoomSubTab.GAMEPAD }
                        )

                        // 5. Users Tab
                        DockIconButton(
                            icon = Icons.Outlined.Group,
                            isActive = activeSubTab == GamesRoomSubTab.USERS,
                            onClick = { activeSubTab = GamesRoomSubTab.USERS }
                        )

                        // 6. Settings Tab
                        DockIconButton(
                            icon = Icons.Outlined.Settings,
                            isActive = activeSubTab == GamesRoomSubTab.SETTINGS,
                            onClick = { activeSubTab = GamesRoomSubTab.SETTINGS }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ====================================================
                // 4. SUB-TAB VIEW CONTENT
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (activeSubTab) {
                        // ====================================================
                        // 4A. GAMEPAD SUB-VIEW (100% MATCHING SCREENSHOT)
                        // ====================================================
                        GamesRoomSubTab.GAMEPAD -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 1. Top Shoulder Buttons (L2, L1, R1, R2) in a row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ShoulderPillButton(label = "L2", modifier = Modifier.weight(1f)) {
                                        triggerEngineAction("L2")
                                    }
                                    ShoulderPillButton(label = "L1", modifier = Modifier.weight(1f)) {
                                        triggerEngineAction("L1")
                                    }
                                    ShoulderPillButton(label = "R1", modifier = Modifier.weight(1f)) {
                                        triggerEngineAction("R1")
                                    }
                                    ShoulderPillButton(label = "R2", modifier = Modifier.weight(1f)) {
                                        triggerEngineAction("R2")
                                    }
                                }

                                // 2. Center Controls: D-Pad on Left & Action Buttons on Right
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // LEFT: Golden-Yellow Border D-Pad
                                    GoldenDPadView(
                                        onDirectionClick = { dir -> triggerEngineAction(dir) }
                                    )

                                    // RIGHT: PlayStation 4 Action Buttons (Triangle, Circle, Cross, Square)
                                    PlayStationButtonsDiamond(
                                        onButtonClick = { btn -> triggerEngineAction(btn) }
                                    )
                                }

                                // 3. Bottom Row: SELECT and START Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PillActionButton(label = "SELECT") {
                                            triggerEngineAction("SELECT")
                                        }
                                        PillActionButton(label = "START") {
                                            triggerEngineAction("START")
                                        }
                                    }
                                }
                            }
                        }

                        // ====================================================
                        // 4B. CHAT SUB-VIEW (معالجة مشكلة الشريط السفلي وظهور لوحة المفاتيح والرسائل أثناء الكتابة)
                        // ====================================================
                        GamesRoomSubTab.CHAT -> {
                            val chatListState = rememberLazyListState()
                            
                            // التمرير التلقائي لآخر رسالة عند وصول أي رسالة جديدة
                            LaunchedEffect(chatMessages.size) {
                                if (chatMessages.isNotEmpty()) {
                                    chatListState.animateScrollToItem(chatMessages.size - 1)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .border(1.dp, Color(0xFFE2EAFD), RoundedCornerShape(24.dp))
                                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
                                    .navigationBarsPadding()
                                    .imePadding()
                            ) {
                                LazyColumn(
                                    state = chatListState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(chatMessages) { msg ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (msg.isMe) Arrangement.Start else Arrangement.End,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            if (!msg.isMe) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(msg.avatarColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = msg.sender.take(1),
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .background(
                                                        if (msg.isMe) Color(0xFF2563EB) else Color(0xFFEEF5FF),
                                                        RoundedCornerShape(16.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = msg.sender,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (msg.isMe) Color.White else Color(0xFF1E3A8A)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = msg.text,
                                                    fontSize = 13.sp,
                                                    color = if (msg.isMe) Color.White else Color(0xFF334155)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = msg.time,
                                                    fontSize = 9.sp,
                                                    color = if (msg.isMe) Color(0xCCFFFFFF) else Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Chat Input Bar - مرتفع تماماً فوق شريط التنقل ولوحة المفاتيح
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F6FB), RoundedCornerShape(20.dp))
                                        .border(1.dp, Color(0xFFE2EAFD), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = chatInputText,
                                        onValueChange = { 
                                            chatInputText = it 
                                            // ضمان التمرير الفوري عند البدء بالكتابة لرؤية الرسائل
                                            if (chatMessages.isNotEmpty()) {
                                                coroutineScope.launch {
                                                    chatListState.scrollToItem(chatMessages.size - 1)
                                                }
                                            }
                                        },
                                        placeholder = {
                                            Text("اكتب رسالة...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = Color(0xFF0F172A),
                                            unfocusedTextColor = Color(0xFF0F172A),
                                            cursorColor = Color(0xFF2563EB)
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontFamily = TajawalFontFamily, color = Color(0xFF0F172A))
                                    )

                                    IconButton(
                                        onClick = {
                                            if (chatInputText.trim().isNotEmpty()) {
                                                chatMessages.add(
                                                    RoomChatMessage(
                                                        id = System.currentTimeMillis().toString(),
                                                        sender = "أنا",
                                                        text = chatInputText.trim(),
                                                        time = "الآن",
                                                        isMe = true
                                                    )
                                                )
                                                chatInputText = ""
                                                coroutineScope.launch {
                                                    if (chatMessages.isNotEmpty()) {
                                                        chatListState.animateScrollToItem(chatMessages.size - 1)
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF2563EB), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "إرسال",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ====================================================
                        // 4C. CAMERAS SUB-VIEW (Matching camera screenshot)
                        // ====================================================
                        GamesRoomSubTab.CAMERA -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .border(1.dp, Color(0xFFE2EAFD), RoundedCornerShape(24.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "كاميرات الغرفة والمراقبة المباشرة",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )

                                cameraFeeds.forEach { cam ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedCameraId = cam.id },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selectedCameraId == cam.id) Color(0xFFEEF5FF) else Color(0xFFF8FAFC),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selectedCameraId == cam.id) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(Color(0xFF2563EB), RoundedCornerShape(12.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Videocam,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = cam.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = "${cam.location} • ${cam.resolution}",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    selectedCameraId = cam.id
                                                    Toast.makeText(context, "تم فتح بث ${cam.name}", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF2563EB)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("فتح الكاميرا", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ====================================================
                        // 4D. WALKIE-TALKIE / INTERCOM SUB-VIEW
                        // ====================================================
                        GamesRoomSubTab.INTERCOM -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .border(1.dp, Color(0xFFE2EAFD), RoundedCornerShape(24.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .background(
                                            if (isIntercomTalking) Color(0xFFDC2626) else Color(0xFFEEF5FF),
                                            CircleShape
                                        )
                                        .border(2.dp, Color(0xFF2563EB), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = if (isIntercomTalking) Color.White else Color(0xFF2563EB),
                                        modifier = Modifier.size(54.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (isIntercomTalking) "جاري البث الصوتي المباشر..." else "اللاسلكي جاهز للتحدث",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIntercomTalking) Color(0xFFDC2626) else Color(0xFF1E3A8A)
                                )

                                Text(
                                    text = "قناة الغرفة الموحدة • تشفير فوري مباشر",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { isIntercomTalking = !isIntercomTalking },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isIntercomTalking) Color(0xFFDC2626) else Color(0xFF2563EB)
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isIntercomTalking) Icons.Default.Mic else Icons.Default.MicNone,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isIntercomTalking) "إيقاف التحدث" else "اضغط للتحدث",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // ====================================================
                        // 4E. USERS SUB-VIEW (Matching members screenshot)
                        // ====================================================
                        GamesRoomSubTab.USERS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .border(1.dp, Color(0xFFE2EAFD), RoundedCornerShape(24.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المتواجدون في الغرفة",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFEEF5FF), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "4 متواجدون",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                }

                                roomUsers.forEach { user ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
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
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = user.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = user.role,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(0xFF10B981), CircleShape)
                                            )
                                            Text("متصل", fontSize = 10.sp, color = Color(0xFF10B981))
                                        }
                                    }
                                }
                            }
                        }

                        // ====================================================
                        // 4F. SETTINGS SUB-VIEW (Full PS1 Emulator & NetPlay)
                        // ====================================================
                        GamesRoomSubTab.SETTINGS -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .border(1.dp, Color(0xFFE2EAFD), RoundedCornerShape(24.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text(
                                        text = "إعدادات غرفة الألعاب والمحاكي",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }

                                item {
                                    SettingToggleRow(
                                        title = "تشغيل الصوت والمؤثرات",
                                        subtitle = "أصوات أزرار يد التحكم ومحاكي PS1",
                                        checked = soundEnabled,
                                        onCheckedChange = { soundEnabled = it }
                                    )
                                }

                                item {
                                    SettingToggleRow(
                                        title = "الاهتزاز اللمسي (Haptics)",
                                        subtitle = "اهتزاز خفيف عند الضغط على الأزرار",
                                        checked = vibrationEnabled,
                                        onCheckedChange = { vibrationEnabled = it }
                                    )
                                }

                                item {
                                    SettingToggleRow(
                                        title = "إشعارات الغرفة والطلبات",
                                        subtitle = "تنبيهات دخول اللاعبين وبدء التحدي",
                                        checked = notificationsEnabled,
                                        onCheckedChange = { notificationsEnabled = it }
                                    )
                                }

                                item {
                                    SettingToggleRow(
                                        title = "وضع الخصوصية",
                                        subtitle = "إخفاء حالتك داخل الغرفة عن غير الأصدقاء",
                                        checked = privacyEnabled,
                                        onCheckedChange = { privacyEnabled = it }
                                    )
                                }

                                item {
                                    Divider(color = Color(0xFFE2EAFD))
                                }

                                item {
                                    Text(
                                        text = "محرك محاكي PS1 وسيرفر Cloudflare NetPlay",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }

                                item {
                                    SettingDetailRow("دقة الرسوميات", graphicsResolution)
                                }
                                item {
                                    SettingDetailRow("محرك التصيير", rendererEngine)
                                }
                                item {
                                    SettingDetailRow("مرشح الشاشة (Shader)", screenShader)
                                }
                                item {
                                    SettingDetailRow("سيرفر NetPlay", "Cloudflare Global Edge • 18ms")
                                }
                            }
                        }
                    }
                }
            }

            // ====================================================
            // 5. CLOUDFLARE ROMS LIBRARY MODAL
            // ====================================================
            if (isRomsModalOpen) {
                AlertDialog(
                    onDismissRequest = { isRomsModalOpen = false },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مكتبة ألعاب ورومات Cloudflare",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF1E3A8A)
                            )
                            IconButton(onClick = { isRomsModalOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                    },
                    text = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(romsList) { rom ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2EAFD))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
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
                                                    .size(36.dp)
                                                    .background(rom.bannerColor, RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.SportsEsports,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = rom.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "${rom.englishTitle} • ${rom.size}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                activeGame = rom
                                                isRomsModalOpen = false
                                                isLiveEngineActive = true
                                                Toast.makeText(context, "تم تحميل وتشغيل ${rom.title} بنجاح!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2563EB)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("تشغيل", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { isRomsModalOpen = false },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("إغلاق المكتبة", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 3. REUSABLE SUB-COMPONENTS
// ----------------------------------------------------

@Composable
fun DockIconButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .background(
                if (isActive) Color(0xFFEEF5FF) else Color.Transparent,
                CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color(0xFF2563EB) else Color(0xFF64748B),
            modifier = Modifier.size(22.dp)
        )
    }
}

// Shoulder Pill Button (L2, L1, R1, R2)
@Composable
fun ShoulderPillButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(21.dp),
        color = Color(0xFFF0F6FF),
        border = BorderStroke(1.5.dp, Color(0xFFBFDBFE))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
            )
        }
    }
}

// Reusable Select/Start Pill Action Button
@Composable
fun PillActionButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(108.dp)
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF0F6FF),
        border = BorderStroke(1.5.dp, Color(0xFFBFDBFE))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
            )
        }
    }
}

// Golden D-Pad View matching screenshot c66b0da0-b0e5-11f1-b69e-1b0131b102c0.png
@Composable
fun GoldenDPadView(
    onDirectionClick: (String) -> Unit
) {
    val dpadBg = Color(0xFFFFFDF0)
    val dpadBorder = Color(0xFFFACC15)
    val arrowYellow = Color(0xFFEAB308)

    Box(
        modifier = Modifier.size(136.dp),
        contentAlignment = Alignment.Center
    ) {
        // Horizontal bar
        Box(
            modifier = Modifier
                .width(136.dp)
                .height(44.dp)
                .background(dpadBg, RoundedCornerShape(12.dp))
                .border(2.5.dp, dpadBorder, RoundedCornerShape(12.dp))
        )

        // Vertical bar
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(136.dp)
                .background(dpadBg, RoundedCornerShape(12.dp))
                .border(2.5.dp, dpadBorder, RoundedCornerShape(12.dp))
        )

        // Center seamless cover
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(dpadBg)
        )

        // Up Arrow
        Box(
            modifier = Modifier
                .size(44.dp)
                .offset(y = (-46).dp)
                .clickable { onDirectionClick("UP") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropUp,
                contentDescription = "أعلى",
                tint = arrowYellow,
                modifier = Modifier.size(34.dp)
            )
        }

        // Down Arrow
        Box(
            modifier = Modifier
                .size(44.dp)
                .offset(y = 46.dp)
                .clickable { onDirectionClick("DOWN") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "أسفل",
                tint = arrowYellow,
                modifier = Modifier.size(34.dp)
            )
        }

        // Left Arrow
        Box(
            modifier = Modifier
                .size(44.dp)
                .offset(x = (-46).dp)
                .clickable { onDirectionClick("LEFT") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowLeft,
                contentDescription = "يسار",
                tint = arrowYellow,
                modifier = Modifier.size(34.dp)
            )
        }

        // Right Arrow
        Box(
            modifier = Modifier
                .size(44.dp)
                .offset(x = 46.dp)
                .clickable { onDirectionClick("RIGHT") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowRight,
                contentDescription = "يمين",
                tint = arrowYellow,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

// PlayStation Buttons Diamond Layout matching screenshot
@Composable
fun PlayStationButtonsDiamond(
    onButtonClick: (String) -> Unit
) {
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top: Triangle (Mint Green)
        Box(
            modifier = Modifier
                .size(52.dp)
                .offset(y = (-44).dp)
                .background(Color(0xFFF0FDF4), CircleShape)
                .border(2.5.dp, Color(0xFF10B981), CircleShape)
                .clickable { onButtonClick("TRIANGLE") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "△",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF10B981)
            )
        }

        // Right: Circle (Coral Red)
        Box(
            modifier = Modifier
                .size(52.dp)
                .offset(x = 44.dp)
                .background(Color(0xFFFEF2F2), CircleShape)
                .border(2.5.dp, Color(0xFFEF4444), CircleShape)
                .clickable { onButtonClick("CIRCLE") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "◯",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFEF4444)
            )
        }

        // Bottom: Cross (Cobalt Blue)
        Box(
            modifier = Modifier
                .size(52.dp)
                .offset(y = 44.dp)
                .background(Color(0xFFEFF6FF), CircleShape)
                .border(2.5.dp, Color(0xFF2563EB), CircleShape)
                .clickable { onButtonClick("CROSS") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF2563EB)
            )
        }

        // Left: Square (Pink/Magenta)
        Box(
            modifier = Modifier
                .size(52.dp)
                .offset(x = (-44).dp)
                .background(Color(0xFFFDF2F8), CircleShape)
                .border(2.5.dp, Color(0xFFEC4899), CircleShape)
                .clickable { onButtonClick("SQUARE") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▢",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFEC4899)
            )
        }
    }
}

// Reusable Setting Toggle Row
@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF64748B))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB)
            )
        )
    }
}

// Reusable Setting Detail Row
@Composable
fun SettingDetailRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
    }
}
