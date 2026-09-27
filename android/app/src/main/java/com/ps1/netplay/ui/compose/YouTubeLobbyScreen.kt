package com.ps1.netplay.ui.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.ps1.netplay.network.CloudflareClient
import kotlinx.coroutines.launch

// ----------------------------------------------------
// MAIN COMPOSABLE: YouTubeLobbyScreen
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeLobbyScreen(
    onBack: () -> Unit,
    onEnterRoom: (roomId: String, videoId: String, roomTitle: String, roomCode: String, isStealth: Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val currentUserId = remember { CloudflareClient.getCurrentUserId(context) }
    val currentUserName = remember { CloudflareClient.getCurrentUsername(context) }

    // Disable click sound effects as requested: "احذف الصوت عند النقر ع اي شي"
    val localView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val prevSound = localView.isSoundEffectsEnabled
        localView.isSoundEffectsEnabled = false
        onDispose {
            localView.isSoundEffectsEnabled = prevSound
        }
    }

    // App Owner detection (Owner has stealth mode & force entry capabilities)
    val isAppOwner = remember { YouTubeRoomManager.isAppOwner(context) }
    var isStealthModeActive by remember { mutableStateOf(false) }

    // Real active public rooms state (NO fake items)
    val publicRooms = remember { mutableStateListOf<PublicYouTubeRoom>() }
    var isLoadingRooms by remember { mutableStateOf(true) }

    // Dialog States
    var isCreateRoomDialogOpen by remember { mutableStateOf(false) }
    var isJoinRoomDialogOpen by remember { mutableStateOf(false) }
    var isCreatingRoom by remember { mutableStateOf(false) }
    var isVerifyingJoin by remember { mutableStateOf(false) }
    var joinErrorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog Inputs
    var newRoomTitle by remember { mutableStateOf("") }
    var newRoomPrivacy by remember { mutableStateOf<RoomPrivacyMode>(RoomPrivacyMode.PUBLIC) }
    var joinRoomCodeInput by remember { mutableStateOf("") }

    // Search query within active rooms
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Refresh real rooms from backend & local storage
    fun refreshRooms() {
        isLoadingRooms = true
        YouTubeRoomManager.fetchRealPublicRooms(context) { rooms ->
            val myRooms = YouTubeRoomManager.loadRoomsLocally(context).filter {
                it.hostId == currentUserId || (currentUserName.isNotBlank() && it.hostName.equals(currentUserName, ignoreCase = true))
            }
            val combined = (rooms + myRooms).distinctBy { it.roomId }
            publicRooms.clear()
            publicRooms.addAll(combined)
            isLoadingRooms = false
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        refreshRooms()
    }

    // Filtered real rooms
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

    // Total online viewers across real rooms
    val totalOnlineViewers = remember(publicRooms.size) {
        publicRooms.sumOf { it.viewersCount }
    }

    // Full RTL Layout matching the app theme 100%
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
                // 1. TOP HEADER: Back Button + Title + Quick Actions
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

                    // Title
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "غرف اليوتيوب",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (publicRooms.isEmpty()) "المشاهدة الجماعية المتزامنة" else "$totalOnlineViewers متصل الآن في الغرف",
                            fontSize = 10.sp,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Top Action Icons (Search + Refresh)
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
                                refreshRooms()
                                Toast.makeText(context, "تم تحديث الغرف النشطة", Toast.LENGTH_SHORT).show()
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
                            placeholder = { Text("ابحث برمز الغرفة أو العنوان أو المضيف...", fontSize = 11.sp, fontFamily = TajawalFontFamily) },
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

                // App Owner & Room Host Stealth Mode Control Banner
                val isHostOfAnyRoom = publicRooms.any { it.hostId == currentUserId || (currentUserName.isNotBlank() && it.hostName.equals(currentUserName, ignoreCase = true)) }
                if (isAppOwner || isHostOfAnyRoom) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isStealthModeActive) Color(0xFF0F172A) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isStealthModeActive) Color(0xFF334155) else Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isStealthModeActive) "👻 وضع التخفي مفعّل (دخول مخفي 100% بدون ظهور الاسم)" else if (isAppOwner) "🛡️ مالك التطبيق (دخول إجباري وتخفي متاح)" else "👑 مالك الغرفة (دخول متخفي متاح)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = if (isStealthModeActive) Color(0xFF38BDF8) else Color(0xFF1E40AF)
                                )
                            }
                            Switch(
                                checked = isStealthModeActive,
                                onCheckedChange = {
                                    isStealthModeActive = it
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ====================================================
                // 2. ULTRA-MODERN ACTION CARD (زر إنشاء غرفة + زر انضمام إلى غرفة برموز بدون كتابة)
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Modern Row of ONLY 2 Icon-Only Action Buttons (أنيقة وصغيرة برموز بدون كتابة)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. زر إنشاء غرفة (Create Room: Red Icon Button)
                            Surface(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val currentUserName = CloudflareClient.getCurrentUsername(context).ifBlank { "أحمد" }
                                    newRoomTitle = "غرفة $currentUserName"
                                    isCreateRoomDialogOpen = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFDC2626),
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(width = 84.dp, height = 48.dp)
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
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    joinRoomCodeInput = ""
                                    joinErrorMessage = null
                                    isJoinRoomDialogOpen = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF2563EB),
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(width = 84.dp, height = 48.dp)
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ====================================================
                // 3. LIST OF REAL PUBLIC ROOMS (الغرف العامة النشطة أونلاين حالياً)
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
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Online Rooms List or Empty State
                if (isLoadingRooms) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2563EB),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (filteredRooms.isEmpty()) {
                    // Clean Modern Empty State
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartDisplay,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "لا توجد غرف عامة نشطة حالياً",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "كن أول من ينشئ غرفة مشاهدة يوتيوب حقيقية وشارك الكود مع أصدقائك للاستمتاع بالمشاهدة الجماعية المتزامنة.",
                                fontSize = 12.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredRooms, key = { it.roomId }) { room ->
                            Surface(
                                onClick = {
                                    onEnterRoom(room.roomId, room.videoId, room.title, room.roomCode, isStealthModeActive)
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
                                        // Room Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(width = 112.dp, height = 76.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF0F172A))
                                        ) {
                                            AsyncImage(
                                                model = room.thumbnailUrl,
                                                contentDescription = room.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .background(Color(0xCCDC2626), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "مباشر",
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

                                            // Host Info + Stealth badge if owner
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

                                                if (isAppOwner && isStealthModeActive) {
                                                    Text(
                                                        text = "• 👻 متخفي",
                                                        fontSize = 10.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF0284C7)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
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

                                        // Enter Room Icon Button
                                        IconButton(
                                            onClick = {
                                                onEnterRoom(room.roomId, room.videoId, room.title, room.roomCode, isStealthModeActive)
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(Color(0xFF2563EB), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
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
        }

        // ====================================================
        // 4. CREATE ROOM MODAL DIALOG (إنشاء غرفة حقيقية بكود حقيقي)
        // ====================================================
        if (isCreateRoomDialogOpen) {
            Dialog(
                onDismissRequest = { if (!isCreatingRoom) isCreateRoomDialogOpen = false },
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
                                modifier = Modifier.size(30.dp),
                                enabled = !isCreatingRoom
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
                            singleLine = true,
                            enabled = !isCreatingRoom
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
                                    onClick = { if (!isCreatingRoom) newRoomPrivacy = mode },
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
                                if (isCreatingRoom) return@Button
                                isCreatingRoom = true
                                YouTubeRoomManager.createRealRoom(
                                    context = context,
                                    title = newRoomTitle,
                                    privacyMode = newRoomPrivacy,
                                    initialVideoId = "dQw4w9WgXcQ",
                                    initialVideoTitle = "مشاهدة متزامنة عبر Cloudflare"
                                ) { result ->
                                    isCreatingRoom = false
                                    isCreateRoomDialogOpen = false
                                    result.onSuccess { createdRoom ->
                                        if (createdRoom.privacyMode == RoomPrivacyMode.PUBLIC) {
                                            publicRooms.removeAll { it.roomId == createdRoom.roomId }
                                            publicRooms.add(0, createdRoom)
                                        }
                                        Toast.makeText(context, "تم إنشاء الغرفة بنجاح! كود الغرفة: ${createdRoom.roomCode} 🍿", Toast.LENGTH_LONG).show()
                                        onEnterRoom(createdRoom.roomId, createdRoom.videoId, createdRoom.title, createdRoom.roomCode, isStealthModeActive)
                                    }.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "فشل إنشاء الغرفة", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            enabled = !isCreatingRoom
                        ) {
                            if (isCreatingRoom) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
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
        }

        // ====================================================
        // 5. JOIN ROOM BY CODE MODAL DIALOG (الانضمام الحقيقي والتحقق الصارم من الكود)
        // ====================================================
        if (isJoinRoomDialogOpen) {
            Dialog(
                onDismissRequest = { if (!isVerifyingJoin) isJoinRoomDialogOpen = false },
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
                                modifier = Modifier.size(30.dp),
                                enabled = !isVerifyingJoin
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }

                        // Room Code Input
                        OutlinedTextField(
                            value = joinRoomCodeInput,
                            onValueChange = {
                                joinRoomCodeInput = it
                                joinErrorMessage = null
                            },
                            label = { Text("كود الغرفة", fontSize = 12.sp, fontFamily = TajawalFontFamily) },
                            placeholder = { Text("أدخل الكود المكون من 6 أرقام (مثال: #YT-492104)", fontSize = 11.sp, fontFamily = TajawalFontFamily) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            isError = joinErrorMessage != null,
                            enabled = !isVerifyingJoin,
                            trailingIcon = {
                                IconButton(onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        if (clip.isNotEmpty()) {
                                            joinRoomCodeInput = clip
                                            joinErrorMessage = null
                                        }
                                    } catch (_: Exception) {}
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                }
                            }
                        )

                        // Strict Error Message if code is invalid
                        joinErrorMessage?.let { errMsg ->
                            Text(
                                text = errMsg,
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily
                            )
                        }

                        // Confirm Join Button (Validates strictly against real database/network)
                        Button(
                            onClick = {
                                val code = joinRoomCodeInput.trim()
                                if (code.isEmpty() && !isAppOwner) {
                                    joinErrorMessage = "يرجى إدخال كود الغرفة"
                                    return@Button
                                }
                                isVerifyingJoin = true
                                joinErrorMessage = null

                                YouTubeRoomManager.joinRealRoomByCode(context, code, forceOwnerBypass = isAppOwner) { result ->
                                    isVerifyingJoin = false
                                    result.onSuccess { matchedRoom ->
                                        isJoinRoomDialogOpen = false
                                        Toast.makeText(context, "تم الانضمام للغرفة بنجاح! 🎬", Toast.LENGTH_SHORT).show()
                                        onEnterRoom(matchedRoom.roomId, matchedRoom.videoId, matchedRoom.title, matchedRoom.roomCode, isStealthModeActive)
                                    }.onFailure { error ->
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        joinErrorMessage = error.message ?: "رمز الغرفة غير صحيح أو الغرفة غير موجودة"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            enabled = !isVerifyingJoin
                        ) {
                            if (isVerifyingJoin) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = if (isAppOwner && joinRoomCodeInput.isBlank()) "دخول إجباري كمالك التطبيق 🛡️" else "انضمام الآن 🎬",
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
}
