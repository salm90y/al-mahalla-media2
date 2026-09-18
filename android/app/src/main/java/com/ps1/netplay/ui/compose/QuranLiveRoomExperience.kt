package com.ps1.netplay.ui.compose

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.togetherWith
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.PI

/**
 * مؤثرات حركية خاصة بمربع عرض الفيديو للمصحف الشريف
 */
enum class QuranDisplayVfx(val title: String) {
    GOLDEN_AURA("هالة نورانية"),
    PARTICLES("ذرات مشعة"),
    CELESTIAL_SHIMMER("إضاءة سينمائية"),
    OFF("عادي")
}

/**
 * شاشة مجلس القرآن الكريم التفاعلية المباشرة (Quran Live Room Experience)
 * مصممة بدقة متطابقة بنسبة 100% مع التصميم المرجعي الفاخر:
 * - شريط علوي نحيف ومرن (40-44dp)
 * - بطاقة مشغل القرآن بنمط الفيديو (نسبة 16:9 + زر التشغيل العائم النابض)
 * - حاويات سفلية مدمجة وقابلة للسحب (Docked Bottom Containers)
 * - شريط تنقل سفلي أنيق (72dp) بخمس أيقونات
 */
enum class QuranRoomDockedTab {
    SETTINGS,
    PARTICIPANTS,
    MIC,
    VIDEO,
    CHAT
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun QuranLiveRoomExperience(
    room: QuranLiveRoom,
    onLeaveRoom: () -> Unit,
    isNightMode: Boolean = true,
    onToggleNightMode: () -> Unit = {}
) {
    val context = LocalContext.current

    // State management
    var currentDockedTab by remember { mutableStateOf(QuranRoomDockedTab.MIC) }
    var isPlaying by remember { mutableStateOf(room.isPlaying) }
    var isMicMuted by remember { mutableStateOf(false) }
    var isHandRaised by remember { mutableStateOf(false) }
    var isNoiseCancellationOn by remember { mutableStateOf(true) }
    var audioVolume by remember { mutableFloatStateOf(0.70f) }
    var isCameraActive by remember { mutableStateOf(false) }
    var isScreenSharing by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0.24f) }

    // Dynamic authentic Quran Ayahs based on the room's current Surah
    val surahAyahs = remember(room.currentSurah.id) {
        QuranRepository.getAyahsForSurah(room.currentSurah.id)
    }
    var currentAyahIdx by remember(room.currentSurah.id) {
        val initialIdx = surahAyahs.indexOfFirst { it.number == room.currentAyahNumber }
        mutableIntStateOf(if (initialIdx >= 0) initialIdx else 0)
    }
    val currentAyah = surahAyahs.getOrElse(currentAyahIdx.coerceIn(0, surahAyahs.lastIndex)) { surahAyahs[0] }

    // Visual Effects Mode for the Quran display box
    var currentVfx by remember { mutableStateOf(QuranDisplayVfx.GOLDEN_AURA) }

    // Settings tab states
    var recitationSpeed by remember { mutableStateOf("1.0x") }
    var repeatAyah by remember { mutableStateOf(false) }
    var fontSizeLabel by remember { mutableStateOf("متوسط") }
    var tafsirEnabled by remember { mutableStateOf(true) }
    var selectedReaderName by remember { mutableStateOf(room.reader.name.ifEmpty { "محمد صديق المنشاوي" }) }

    // Auto-advance playback when playing
    LaunchedEffect(isPlaying, currentAyahIdx) {
        if (isPlaying) {
            while (isPlaying) {
                delay(1000)
                if (seekProgress < 1.0f) {
                    seekProgress += 0.012f
                } else {
                    seekProgress = 0f
                    if (currentAyahIdx < surahAyahs.lastIndex) {
                        currentAyahIdx++
                    } else if (repeatAyah) {
                        currentAyahIdx = 0
                    }
                }
            }
        }
    }

    val totalDurationSeconds = 168
    val currentSec = (seekProgress * totalDurationSeconds).toInt()
    val remainingSec = (totalDurationSeconds - currentSec).coerceAtLeast(0)
    val curTimeStr = String.format("%02d:%02d", currentSec / 60, currentSec % 60)
    val remTimeStr = String.format("-%02d:%02d", remainingSec / 60, remainingSec % 60)

    // Chat tab state
    var chatMessageInput by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf(
            Triple("أحمد", "منذ دقيقتين", "السلام عليكم ورحمة الله، هل نستمر الآن في قراءة الآيات؟"),
            Triple("فاطمة", "منذ دقيقة", "وعليكم السلام، نعم بارك الله فيك مستعدون"),
            Triple("خالد", "الآن", "ما شاء الله، صوته جميل جداً جزاكم الله خيراً 🙏")
        )
    }

    // Colors according to design specification
    val bgDarkNavy = Color(0xFF0A1220)
    val cardBackground = Color(0xFF121E33)
    val containerBg = Color(0xFF0F1B2E)
    val primaryTeal = Color(0xFF2DB39E)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFF8A94A6)
    val trackBg = Color(0xFF2A3A4F)
    val borderDark = Color(0xFF1E2D4A)

    // Pulsing animation for floating audio button
    val infiniteTransition = rememberInfiniteTransition(label = "PulseGlow")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // VFX animations for the video display box
    val vfxTransition = rememberInfiniteTransition(label = "QuranVfx")
    val vfxHaloRadius by vfxTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "vfxHaloRadius"
    )
    val vfxShimmerOffset by vfxTransition.animateFloat(
        initialValue = -300f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "vfxShimmerOffset"
    )
    val vfxParticleOffset by vfxTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "vfxParticleOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDarkNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ========================================================
            // 1. CRITICAL FIXES - THIN TOP BAR (Height 40-44dp only)
            // ========================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT: Back arrow + Reader Name with mic icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLeaveRoom() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (room.reader.name.isNotBlank()) room.reader.name else "محمد صديق المنشاوي",
                            color = textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // CENTER: Room Name
                    Text(
                        text = if (room.name.isNotBlank()) room.name else "fff",
                        color = textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // RIGHT: Room Code + share icon + LIVE badge red dot + Listeners
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Room code & share
                        Text(
                            text = if (room.roomCode.isNotBlank()) room.roomCode else "QR-6872",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = textSecondary,
                            modifier = Modifier
                                .size(13.dp)
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "انضم إلى مجلس القرآن الكريم المبارك: ${room.name} (رمز: ${room.roomCode})"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة المجلس"))
                                }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Red LIVE Dot & Text
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "مباشر",
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Listeners count
                        Text(
                            text = "${if (room.listenersCount > 0) room.listenersCount else 128} مستمع",
                            color = textSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // ========================================================
            // 2. MAIN QURAN PLAYER CARD (VIDEO-LIKE BOX WITH VFX & AUTHENTIC AYAH)
            // ========================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Video-like Quran Display Box with Motion VFX
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(98.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF080D18))
                    ) {
                        // Background image
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=800&auto=format&fit=crop&q=80",
                            contentDescription = "المصحف الشريف",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Dark gradient overlay for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x77000000),
                                            Color(0x33000000),
                                            Color(0x88000000)
                                        )
                                    )
                                )
                        )

                        // MOTION EFFECTS LAYER (المؤثرات الحركية)
                        when (currentVfx) {
                            QuranDisplayVfx.GOLDEN_AURA -> {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    // Glowing radial halo
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF2DB39E).copy(alpha = 0.35f),
                                                Color(0xFFD4AF37).copy(alpha = 0.18f),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = size.width * 0.42f * vfxHaloRadius
                                        )
                                    )
                                    // Animated outer pulse ring
                                    drawCircle(
                                        color = Color(0xFF2DB39E).copy(alpha = (0.45f * (1.3f - vfxHaloRadius)).coerceIn(0.05f, 0.45f)),
                                        radius = size.width * 0.32f * vfxHaloRadius,
                                        center = center,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }
                            QuranDisplayVfx.PARTICLES -> {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val particleCoords = listOf(
                                        0.12f to 0.8f, 0.22f to 0.4f, 0.32f to 0.9f, 0.48f to 0.6f,
                                        0.62f to 0.85f, 0.72f to 0.3f, 0.84f to 0.7f, 0.92f to 0.5f,
                                        0.18f to 0.25f, 0.40f to 0.15f, 0.68f to 0.2f, 0.58f to 0.45f
                                    )
                                    particleCoords.forEachIndexed { i, (px, py) ->
                                        val curY = ((py - vfxParticleOffset + 1f) % 1f) * size.height
                                        val curX = px * size.width
                                        val pAlpha = (sin(vfxParticleOffset * PI * 2 + i).toFloat() * 0.3f + 0.6f).coerceIn(0.2f, 0.95f)
                                        drawCircle(
                                            color = if (i % 2 == 0) Color(0xFFFFD700).copy(alpha = pAlpha) else Color(0xFF2DB39E).copy(alpha = pAlpha),
                                            radius = (2.2f + (i % 3)).dp.toPx(),
                                            center = Offset(curX, curY)
                                        )
                                    }
                                }
                            }
                            QuranDisplayVfx.CELESTIAL_SHIMMER -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFF2DB39E).copy(alpha = 0.25f),
                                                    Color(0xFFFFD700).copy(alpha = 0.20f),
                                                    Color.Transparent
                                                ),
                                                start = Offset(vfxShimmerOffset, 0f),
                                                end = Offset(vfxShimmerOffset + 240f, 180f)
                                            )
                                        )
                                )
                            }
                            QuranDisplayVfx.OFF -> {
                                // No visual effect
                            }
                        }

                        // Top-Start: Surah & Juz Badge
                        Surface(
                            color = Color(0xBB0A1220),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0x332DB39E)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "سورة ${room.currentSurah.name} • جزء ${room.currentSurah.juzNumber}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        // Top-End: AESTHETIC VFX BUTTON (زر المؤثرات الحركية لمربع العرض)
                        Surface(
                            color = Color(0xCC0A1220),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (currentVfx != QuranDisplayVfx.OFF) primaryTeal else Color(0x44FFFFFF)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clickable {
                                    currentVfx = when (currentVfx) {
                                        QuranDisplayVfx.GOLDEN_AURA -> QuranDisplayVfx.PARTICLES
                                        QuranDisplayVfx.PARTICLES -> QuranDisplayVfx.CELESTIAL_SHIMMER
                                        QuranDisplayVfx.CELESTIAL_SHIMMER -> QuranDisplayVfx.OFF
                                        QuranDisplayVfx.OFF -> QuranDisplayVfx.GOLDEN_AURA
                                    }
                                    Toast.makeText(context, "مؤثر العرض: ${currentVfx.title}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "مؤثرات حركية",
                                    tint = if (currentVfx != QuranDisplayVfx.OFF) Color(0xFFFFD700) else Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentVfx.title,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Authentic Ayah Navigation & Counter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Ayah Button
                        IconButton(
                            onClick = {
                                if (currentAyahIdx > 0) {
                                    currentAyahIdx--
                                    seekProgress = 0f
                                }
                            },
                            enabled = currentAyahIdx > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "الآية السابقة",
                                tint = if (currentAyahIdx > 0) primaryTeal else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Center: Authentic Surah & Ayah Badge
                        Text(
                            text = "سورة ${room.currentSurah.name} • الآية ${currentAyah.number} من ${surahAyahs.size}",
                            color = primaryTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Next Ayah Button
                        IconButton(
                            onClick = {
                                if (currentAyahIdx < surahAyahs.lastIndex) {
                                    currentAyahIdx++
                                    seekProgress = 0f
                                }
                            },
                            enabled = currentAyahIdx < surahAyahs.lastIndex,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "الآية التالية",
                                tint = if (currentAyahIdx < surahAyahs.lastIndex) primaryTeal else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Authentic Quranic Text in Uthmani Script
                    Text(
                        text = "${currentAyah.textUthmani} ۝",
                        color = textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 27.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // Tafsir / Meaning
                    if (currentAyah.tafsir.isNotBlank()) {
                        Text(
                            text = currentAyah.tafsir,
                            color = textSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Audio Seekbar with Floating Aesthetic Pulsing Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Track & Time Labels
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = curTimeStr,
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = remTimeStr,
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            // Seekbar 3dp Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(trackBg)
                                    .clickable {
                                        seekProgress = (seekProgress + 0.2f) % 1f
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(seekProgress)
                                        .background(primaryTeal)
                                )
                            }
                        }

                        // FLOATING BUTTON (Overlaps center of track with glowing pulse animation)
                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer Glow Shadow Circle
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(if (isPlaying) buttonScale else 1.0f)
                                    .clip(CircleShape)
                                    .background(primaryTeal.copy(alpha = glowAlpha))
                            )

                            // Main Floating Button
                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        isPlaying = !isPlaying
                                        Toast.makeText(
                                            context,
                                            if (isPlaying) "تم استئناف التلاوة" else "إيقاف مؤقت",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                shape = CircleShape,
                                color = primaryTeal,
                                shadowElevation = 6.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل/إيقاف",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 3. BOTTOM CONTAINERS - DOCKED, NOT POPUP (Weight 1f)
            // ========================================================
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = containerBg,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Drag Handle on Top (small gray pill 40dp x 4dp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF475569))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // AnimatedContent switching between the 5 docked containers
                    AnimatedContent(
                        targetState = currentDockedTab,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220)) +
                                    slideInVertically(animationSpec = tween(220)) { height -> height / 5 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(180)) +
                                            slideOutVertically(animationSpec = tween(180)) { height -> -height / 5 }
                                )
                        },
                        label = "DockedContainerTransition"
                    ) { tab ->
                        when (tab) {
                            QuranRoomDockedTab.MIC -> {
                                QuranContainerMicrophone(
                                    isMuted = isMicMuted,
                                    onToggleMute = { isMicMuted = !isMicMuted },
                                    isHandRaised = isHandRaised,
                                    onToggleHand = {
                                        isHandRaised = !isHandRaised
                                        Toast.makeText(
                                            context,
                                            if (isHandRaised) "تم رفع اليد لطلب الكلمة ✋" else "تم إنزال اليد",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    isNoiseCancellationOn = isNoiseCancellationOn,
                                    onToggleNoise = { isNoiseCancellationOn = it },
                                    volume = audioVolume,
                                    onVolumeChange = { audioVolume = it }
                                )
                            }
                            QuranRoomDockedTab.VIDEO -> {
                                QuranContainerVideo(
                                    isCameraActive = isCameraActive,
                                    onToggleCamera = { isCameraActive = !isCameraActive },
                                    isScreenSharing = isScreenSharing,
                                    onToggleScreenShare = { isScreenSharing = !isScreenSharing },
                                    isRecording = isRecording,
                                    onToggleRecording = { isRecording = !isRecording }
                                )
                            }
                            QuranRoomDockedTab.PARTICIPANTS -> {
                                QuranContainerParticipants(
                                    onMuteAll = {
                                        Toast.makeText(context, "تم كتم جميع المستمعين", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            QuranRoomDockedTab.SETTINGS -> {
                                QuranContainerSettings(
                                    speed = recitationSpeed,
                                    onSpeedChange = { recitationSpeed = it },
                                    repeat = repeatAyah,
                                    onRepeatChange = { repeatAyah = it },
                                    fontSize = fontSizeLabel,
                                    onFontSizeChange = { fontSizeLabel = it },
                                    tafsir = tafsirEnabled,
                                    onTafsirChange = { tafsirEnabled = it },
                                    reader = selectedReaderName,
                                    onReaderChange = { selectedReaderName = it },
                                    isNight = isNightMode,
                                    onToggleNight = onToggleNightMode
                                )
                            }
                            QuranRoomDockedTab.CHAT -> {
                                QuranContainerChat(
                                    messages = chatMessages,
                                    input = chatMessageInput,
                                    onInputChange = { chatMessageInput = it },
                                    onSend = {
                                        if (chatMessageInput.isNotBlank()) {
                                            chatMessages.add(Triple("أنت", "الآن", chatMessageInput))
                                            chatMessageInput = ""
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 4. BOTTOM NAVIGATION (Height 58dp, elevated above mobile system navigation)
            // ========================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                color = bgDarkNavy,
                border = BorderStroke(1.dp, Color(0xFF1A2A44))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. الإعدادات
                    QuranBottomNavItem(
                        title = "الإعدادات",
                        icon = Icons.Default.Settings,
                        isSelected = currentDockedTab == QuranRoomDockedTab.SETTINGS,
                        onClick = { currentDockedTab = QuranRoomDockedTab.SETTINGS }
                    )

                    // 2. المشاركون
                    QuranBottomNavItem(
                        title = "المشاركون",
                        icon = Icons.Default.People,
                        isSelected = currentDockedTab == QuranRoomDockedTab.PARTICIPANTS,
                        onClick = { currentDockedTab = QuranRoomDockedTab.PARTICIPANTS }
                    )

                    // 3. الميكروفون
                    QuranBottomNavItem(
                        title = "الميكروفون",
                        icon = Icons.Default.Mic,
                        isSelected = currentDockedTab == QuranRoomDockedTab.MIC,
                        onClick = { currentDockedTab = QuranRoomDockedTab.MIC }
                    )

                    // 4. الفيديو
                    QuranBottomNavItem(
                        title = "الفيديو",
                        icon = Icons.Default.Videocam,
                        isSelected = currentDockedTab == QuranRoomDockedTab.VIDEO,
                        onClick = { currentDockedTab = QuranRoomDockedTab.VIDEO }
                    )

                    // 5. الدردشة
                    QuranBottomNavItem(
                        title = "الدردشة",
                        icon = Icons.Default.ChatBubble,
                        isSelected = currentDockedTab == QuranRoomDockedTab.CHAT,
                        onClick = { currentDockedTab = QuranRoomDockedTab.CHAT }
                    )
                }
            }
        }
    }
}

// ====================================================================
// SUB-COMPONENTS: THE 5 DOCKED CONTAINERS (Exact to uploaded images)
// ====================================================================

/**
 * CONTAINER 1 - MICROPHONE (الميكروفون)
 */
@Composable
fun QuranContainerMicrophone(
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isHandRaised: Boolean,
    onToggleHand: () -> Unit,
    isNoiseCancellationOn: Boolean,
    onToggleNoise: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    val primaryTeal = Color(0xFF2DB39E)
    val cardNavy = Color(0xFF162032)
    val borderDark = Color(0xFF1E2D4A)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Row: Title + "متصل" Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الميكروفون",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // "متصل" Badge with Green Dot
            Surface(
                color = Color(0xFF0F3D38),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!isMuted) "متصل" else "مكتوم",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle Status
        Text(
            text = if (!isMuted) "الميكروفون مفعّل" else "الميكروفون مغلق",
            color = if (!isMuted) primaryTeal else Color(0xFFEF4444),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Large Glowing Concentric Mic Button (120dp)
        Box(
            modifier = Modifier
                .size(110.dp)
                .clickable { onToggleMute() },
            contentAlignment = Alignment.Center
        ) {
            // Outer Ring 1
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(if (!isMuted) primaryTeal.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.12f))
            )
            // Middle Ring 2
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(if (!isMuted) primaryTeal.copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.25f))
            )
            // Core Button (72dp)
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = if (!isMuted) primaryTeal else Color(0xFFEF4444),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (!isMuted) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "مايك",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Two Action Buttons: "كتم" & "رفع اليد" with subtle divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // كتم
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onToggleMute() }
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(0xFF162238),
                    border = BorderStroke(1.dp, borderDark)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "كتم",
                            tint = if (isMuted) Color(0xFFEF4444) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isMuted) "إلغاء الكتم" else "كتم",
                    color = Color(0xFF8A94A6),
                    fontSize = 11.sp
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(borderDark)
            )

            // رفع اليد
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onToggleHand() }
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = if (isHandRaised) primaryTeal else Color(0xFF162238),
                    border = BorderStroke(1.dp, if (isHandRaised) primaryTeal else borderDark)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FrontHand,
                            contentDescription = "رفع اليد",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isHandRaised) "إنزال اليد" else "رفع اليد",
                    color = if (isHandRaised) primaryTeal else Color(0xFF8A94A6),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Noise Cancellation & Volume Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardNavy,
            border = BorderStroke(1.dp, borderDark)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Noise cancellation toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إلغاء الضوضاء",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Switch(
                        checked = isNoiseCancellationOn,
                        onCheckedChange = onToggleNoise,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryTeal,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2A3A4F)
                        )
                    )
                }

                // Volume slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = null,
                        tint = Color(0xFF8A94A6),
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = primaryTeal,
                            activeTrackColor = primaryTeal,
                            inactiveTrackColor = Color(0xFF2A3A4F)
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color(0xFF8A94A6),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "مستوى الصوت ${(volume * 100).toInt()}%",
                    color = Color(0xFF8A94A6),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * CONTAINER 2 - VIDEO (الفيديو)
 */
@Composable
fun QuranContainerVideo(
    isCameraActive: Boolean,
    onToggleCamera: () -> Unit,
    isScreenSharing: Boolean,
    onToggleScreenShare: () -> Unit,
    isRecording: Boolean,
    onToggleRecording: () -> Unit
) {
    val primaryTeal = Color(0xFF2DB39E)
    val cardNavy = Color(0xFF162032)
    val borderDark = Color(0xFF1E2D4A)

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الفيديو", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = Color(0xFF0F3D38),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "4",
                    color = primaryTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Grid of Video Windows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: أحمد • الكاميرا
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                color = cardNavy,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A2A44)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF8A94A6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("أحمد • الكاميرا", color = Color(0xFF8A94A6), fontSize = 10.sp)
                }
            }

            // Card 2: فاطمة • الكاميرا
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                color = cardNavy,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A2A44)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = primaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("فاطمة • الكاميرا", color = Color(0xFF8A94A6), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: خالد • الكاميرا (LIVE badge)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                color = cardNavy,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // LIVE pill at top-left
                    Surface(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart),
                        color = Color(0xFF0F3D38),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("مباشر", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120",
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, primaryTeal, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("خالد • الكاميرا", color = Color(0xFF8A94A6), fontSize = 10.sp)
                    }
                }
            }

            // Card 4: القرآن • مشاركة
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                color = cardNavy,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=160",
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("القرآن • مشاركة", color = Color(0xFF8A94A6), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom 3 Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // تشغيل الكاميرا
            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .clickable { onToggleCamera() },
                shape = RoundedCornerShape(12.dp),
                color = if (isCameraActive) primaryTeal else Color(0xFF0F3D38),
                border = BorderStroke(1.dp, primaryTeal)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (isCameraActive) Color.White else primaryTeal,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCameraActive) "إيقاف الكاميرا" else "تشغيل الكاميرا",
                        color = if (isCameraActive) Color.White else primaryTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // مشاركة الشاشة
            Surface(
                modifier = Modifier
                    .weight(1.1f)
                    .clickable { onToggleScreenShare() },
                shape = RoundedCornerShape(12.dp),
                color = if (isScreenSharing) primaryTeal else cardNavy,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenShare,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مشاركة الشاشة",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            // تسجيل
            Surface(
                modifier = Modifier
                    .weight(0.9f)
                    .clickable { onToggleRecording() },
                shape = RoundedCornerShape(12.dp),
                color = cardNavy,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRecording) "إيقاف" else "تسجيل",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * CONTAINER 3 - PARTICIPANTS (المشاركون)
 */
@Composable
fun QuranContainerParticipants(
    onMuteAll: () -> Unit
) {
    val primaryTeal = Color(0xFF2DB39E)
    val cardNavy = Color(0xFF162032)
    val borderDark = Color(0xFF1E2D4A)

    var searchQuery by remember { mutableStateOf("") }

    val participants = listOf(
        Triple("أحمد", "مشرف", "نشط الآن"),
        Triple("فاطمة", "قارئ", "مشغول"),
        Triple("خالد", "مستمع", "انضم منذ دقيقة")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("المشاركون", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = Color(0xFF0F3D38),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "128",
                    color = primaryTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Participants list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            participants.forEach { (name, role, status) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardNavy,
                    border = BorderStroke(1.dp, borderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        AsyncImage(
                            model = when (name) {
                                "أحمد" -> "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100"
                                "فاطمة" -> "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100"
                                else -> "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=100"
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.dp, primaryTeal, CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))

                                // Role badge
                                val (badgeBg, badgeText) = when (role) {
                                    "مشرف" -> Pair(Color(0xFF0F3D38), primaryTeal)
                                    "قارئ" -> Pair(Color(0xFF1E3A5F), Color(0xFF38BDF8))
                                    else -> Pair(Color(0xFF1E293B), Color(0xFF94A3B8))
                                }
                                Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = role,
                                        color = badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(status, color = Color(0xFF8A94A6), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar & Mute All button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = cardNavy,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF8A94A6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "البحث عن مشارك..." else searchQuery,
                        color = Color(0xFF8A94A6),
                        fontSize = 11.sp
                    )
                }
            }

            // Clip / Link button
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = cardNavy,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF8A94A6),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // "كتم الكل" Button
            Surface(
                modifier = Modifier.clickable { onMuteAll() },
                shape = RoundedCornerShape(18.dp),
                color = primaryTeal
            ) {
                Text(
                    text = "كتم الكل",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * CONTAINER 4 - SETTINGS (الإعدادات)
 */
@Composable
fun QuranContainerSettings(
    speed: String,
    onSpeedChange: (String) -> Unit,
    repeat: Boolean,
    onRepeatChange: (Boolean) -> Unit,
    fontSize: String,
    onFontSizeChange: (String) -> Unit,
    tafsir: Boolean,
    onTafsirChange: (Boolean) -> Unit,
    reader: String,
    onReaderChange: (String) -> Unit,
    isNight: Boolean,
    onToggleNight: () -> Unit
) {
    val primaryTeal = Color(0xFF2DB39E)
    val cardNavy = Color(0xFF162032)
    val borderDark = Color(0xFF1E2D4A)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الإعدادات", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = Color(0xFF0F3D38),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = primaryTeal,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("6", color = primaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. سرعة التلاوة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سرعة التلاوة", color = Color.White, fontSize = 14.sp)
            Surface(
                modifier = Modifier.clickable {
                    val nextSpeed = when (speed) {
                        "1.0x" -> "1.25x"
                        "1.25x" -> "1.5x"
                        "1.5x" -> "0.75x"
                        else -> "1.0x"
                    }
                    onSpeedChange(nextSpeed)
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F3D38),
                border = BorderStroke(1.dp, primaryTeal.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(speed, color = primaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = primaryTeal, modifier = Modifier.size(14.dp))
                }
            }
        }

        // 2. تكرار الآية
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تكرار الآية", color = Color.White, fontSize = 14.sp)
            Switch(
                checked = repeat,
                onCheckedChange = onRepeatChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = primaryTeal,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color(0xFF2A3A4F)
                )
            )
        }

        // 3. حجم الخط
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("حجم الخط", color = Color.White, fontSize = 14.sp)
            Surface(
                modifier = Modifier.clickable {
                    val next = when (fontSize) {
                        "صغير" -> "متوسط"
                        "متوسط" -> "كبير"
                        else -> "صغير"
                    }
                    onFontSizeChange(next)
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F3D38),
                border = BorderStroke(1.dp, primaryTeal.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(fontSize, color = primaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = primaryTeal, modifier = Modifier.size(14.dp))
                }
            }
        }

        // 4. تفعيل التفسير
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تفعيل التفسير", color = Color.White, fontSize = 14.sp)
            Switch(
                checked = tafsir,
                onCheckedChange = onTafsirChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = primaryTeal,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color(0xFF2A3A4F)
                )
            )
        }

        // 5. اختيار القارئ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("اختيار القارئ", color = Color.White, fontSize = 14.sp)
            Surface(
                modifier = Modifier.clickable {
                    val nextReader = when (reader) {
                        "محمد صديق المنشاوي" -> "عبد الباسط عبد الصمد"
                        "عبد الباسط عبد الصمد" -> "مشاري راشد العفاسي"
                        else -> "محمد صديق المنشاوي"
                    }
                    onReaderChange(nextReader)
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F3D38),
                border = BorderStroke(1.dp, primaryTeal.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(reader, color = primaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = primaryTeal, modifier = Modifier.size(14.dp))
                }
            }
        }

        // 6. الثيم ليلي/نهاري
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الثيم ليلي/نهاري", color = Color.White, fontSize = 14.sp)
            Surface(
                modifier = Modifier.clickable { onToggleNight() },
                shape = RoundedCornerShape(16.dp),
                color = if (isNight) primaryTeal else Color(0xFF2A3A4F)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isNight) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * CONTAINER 5 - CHAT (الدردشة)
 */
@Composable
fun QuranContainerChat(
    messages: List<Triple<String, String, String>>,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val primaryTeal = Color(0xFF2DB39E)
    val cardNavy = Color(0xFF162032)
    val chatBubbleBg = Color(0xFF1A2A44)
    val borderDark = Color(0xFF1E2D4A)

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الدردشة", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = Color(0xFF0F3D38),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "42",
                    color = primaryTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { (sender, time, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Avatar
                    AsyncImage(
                        model = when (sender) {
                            "أحمد" -> "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100"
                            "فاطمة" -> "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100"
                            else -> "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=100"
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, primaryTeal, CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Message Bubble
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
                        color = chatBubbleBg,
                        border = BorderStroke(1.dp, borderDark)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sender, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(time, color = Color(0xFF8A94A6), fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(text, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = cardNavy,
                border = BorderStroke(1.dp, borderDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (input.isEmpty()) {
                            Text("اكتب رسالة...", color = Color(0xFF8A94A6), fontSize = 12.sp)
                        }
                        BasicTextField(
                            value = input,
                            onValueChange = onInputChange,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مرفق",
                        tint = Color(0xFF8A94A6),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Teal Circular Send Button
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onSend() },
                shape = CircleShape,
                color = primaryTeal
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * شريط التنقل السفلي - العنصر الواحد
 */
@Composable
fun QuranBottomNavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryTeal = Color(0xFF2DB39E)
    val inactiveGray = Color(0xFF8A94A6)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) primaryTeal else inactiveGray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = if (isSelected) primaryTeal else inactiveGray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
