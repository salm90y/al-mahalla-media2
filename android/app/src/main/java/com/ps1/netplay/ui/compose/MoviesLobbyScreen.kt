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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesLobbyScreen(
    onBack: () -> Unit,
    onEnterRoom: (roomId: String, streamUrl: String, roomTitle: String, roomCode: String, isStealth: Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val currentUserId = remember { CloudflareClient.getCurrentUserId(context) }
    val currentUserName = remember { CloudflareClient.getCurrentUsername(context) }

    // Disable click sound effects
    val localView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val prevSound = localView.isSoundEffectsEnabled
        localView.isSoundEffectsEnabled = false
        onDispose { localView.isSoundEffectsEnabled = prevSound }
    }

    // App Owner detection
    val isAppOwner = remember { MoviesRoomManager.isAppOwner(context) }
    var isStealthModeActive by remember { mutableStateOf(false) }

    // Real active public rooms state
    val publicRooms = remember { mutableStateListOf<PublicMoviesRoom>() }
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

    fun refreshRooms() {
        isLoadingRooms = true
        MoviesRoomManager.fetchRealPublicRooms(context) { rooms ->
            val myRooms = MoviesRoomManager.loadRoomsLocally(context).filter {
                it.hostId == currentUserId || (currentUserName.isNotBlank() && it.hostName.equals(currentUserName, ignoreCase = true))
            }
            val combined = (rooms + myRooms).distinctBy { it.roomId }
            publicRooms.clear()
            publicRooms.addAll(combined)
            isLoadingRooms = false
        }
    }

    LaunchedEffect(Unit) {
        refreshRooms()
    }

    val filteredRooms = remember(searchQuery, publicRooms.size) {
        if (searchQuery.trim().isEmpty()) {
            publicRooms.toList()
        } else {
            publicRooms.filter {
                it.title.contains(searchQuery.trim(), ignoreCase = true) ||
                it.currentMovieTitle.contains(searchQuery.trim(), ignoreCase = true) ||
                it.hostName.contains(searchQuery.trim(), ignoreCase = true) ||
                it.roomCode.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    val totalOnlineViewers = remember(publicRooms.size) {
        publicRooms.sumOf { it.viewersCount }
    }

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
                // 1. TOP HEADER: Back Button + Title + Quick Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سينما الأفلام والمسلسلات",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "بث سحابي 4K وإعادة بث m3u مباشر",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAppOwner) {
                            IconButton(
                                onClick = {
                                    isStealthModeActive = !isStealthModeActive
                                    Toast.makeText(
                                        context,
                                        if (isStealthModeActive) "تم تفعيل وضع التخفي للمطور (مجهول)" else "تم إلغاء وضع التخفي",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isStealthModeActive) Color(0xFF0F172A) else Color.White,
                                        CircleShape
                                    )
                                    .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isStealthModeActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "وضع التخفي للمطور",
                                    tint = if (isStealthModeActive) Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { refreshRooms() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. ACTION HERO CARDS (Create Room + Join by Code)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                newRoomTitle = ""
                                joinErrorMessage = null
                                isCreateRoomDialogOpen = true
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2563EB)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "إنشاء غرفة",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "إنشاء صالة سينما",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "بث متزامن مع الأصدقاء",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                joinRoomCodeInput = ""
                                joinErrorMessage = null
                                isJoinRoomDialogOpen = true
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "رمز الغرفة",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "انضمام عبر الرمز",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "إدخال كود الغرفة السري",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. STATS & SEARCH BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = "الغرف السينمائية النشطة (${filteredRooms.size})",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "مشاهدون",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$totalOnlineViewers متصل",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث عن اسم الغرفة، الفلم، أو المضيف...",
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. ACTIVE ROOMS LIST
                if (isLoadingRooms) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2563EB),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (filteredRooms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Theaters,
                                contentDescription = "لا توجد غرف",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "لا توجد غرف سينما نشطة حالياً",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "كن أول من ينشئ صالة سينما ويبث أفلام ومسلسلات لأصدقائك!",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { isCreateRoomDialogOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("إنشاء صالة سينما الآن", fontFamily = TajawalFontFamily, color = Color.White)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(filteredRooms, key = { it.roomId }) { room ->
                            MovieRoomItemCard(
                                room = room,
                                isAppOwner = isAppOwner,
                                onEnter = {
                                    onEnterRoom(
                                        room.roomId,
                                        room.streamUrl,
                                        room.title,
                                        room.roomCode,
                                        isStealthModeActive
                                    )
                                },
                                onCopyCode = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", room.roomCode))
                                    Toast.makeText(context, "تم نسخ رمز الغرفة: ${room.roomCode}", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    MoviesRoomManager.deleteRoom(context, room.roomId) {
                                        refreshRooms()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 5. CREATE ROOM DIALOG
            if (isCreateRoomDialogOpen) {
                Dialog(
                    onDismissRequest = { if (!isCreatingRoom) isCreateRoomDialogOpen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إنشاء صالة سينما جديدة",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F172A)
                                )
                                IconButton(
                                    onClick = { isCreateRoomDialogOpen = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                                }
                            }

                            Text(
                                text = "عنوان الصالة السينمائية",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color(0xFF334155)
                            )
                            OutlinedTextField(
                                value = newRoomTitle,
                                onValueChange = { newRoomTitle = it },
                                placeholder = {
                                    Text("مثال: سينما الأفلام العربية 4K", fontFamily = TajawalFontFamily, fontSize = 13.sp)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )

                            Text(
                                text = "خصوصية الصالة",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color(0xFF334155)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    RoomPrivacyMode.PUBLIC to "عامة (الكل)",
                                    RoomPrivacyMode.INVITE_ONLY to "خاصة (رمز فقط)"
                                ).forEach { (mode, label) ->
                                    val isSelected = newRoomPrivacy == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { newRoomPrivacy = mode }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            if (joinErrorMessage != null) {
                                Text(
                                    text = joinErrorMessage ?: "",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF4444)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    isCreatingRoom = true
                                    joinErrorMessage = null
                                    MoviesRoomManager.createRealRoom(
                                        context = context,
                                        title = newRoomTitle,
                                        privacyMode = newRoomPrivacy
                                    ) { result ->
                                        isCreatingRoom = false
                                        result.onSuccess { createdRoom ->
                                            isCreateRoomDialogOpen = false
                                            onEnterRoom(
                                                createdRoom.roomId,
                                                createdRoom.streamUrl,
                                                createdRoom.title,
                                                createdRoom.roomCode,
                                                isStealthModeActive
                                            )
                                        }.onFailure {
                                            joinErrorMessage = "تعذر إنشاء الغرفة، يرجى المحاولة ثانية"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                enabled = !isCreatingRoom
                            ) {
                                if (isCreatingRoom) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(
                                        text = "تأكيد وبدء العرض المتزامن",
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. JOIN BY CODE DIALOG
            if (isJoinRoomDialogOpen) {
                Dialog(
                    onDismissRequest = { if (!isVerifyingJoin) isJoinRoomDialogOpen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "انضمام لصالة عبر الرمز السري",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F172A)
                                )
                                IconButton(
                                    onClick = { isJoinRoomDialogOpen = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                                }
                            }

                            Text(
                                text = "أدخل رمز الغرفة المكون من 6 أرقام (مثال: #MOV-847291 أو 847291):",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            OutlinedTextField(
                                value = joinRoomCodeInput,
                                onValueChange = { joinRoomCodeInput = it },
                                placeholder = {
                                    Text("#MOV-XXXXXX", fontFamily = TajawalFontFamily, fontSize = 14.sp)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )

                            if (joinErrorMessage != null) {
                                Text(
                                    text = joinErrorMessage ?: "",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF4444)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    val code = joinRoomCodeInput.trim()
                                    if (code.isEmpty()) {
                                        joinErrorMessage = "يرجى كتابة رمز الغرفة"
                                        return@Button
                                    }
                                    isVerifyingJoin = true
                                    joinErrorMessage = null
                                    MoviesRoomManager.getRoomByCodeOrId(context, code) { targetRoom ->
                                        isVerifyingJoin = false
                                        if (targetRoom != null) {
                                            isJoinRoomDialogOpen = false
                                            onEnterRoom(
                                                targetRoom.roomId,
                                                targetRoom.streamUrl,
                                                targetRoom.title,
                                                targetRoom.roomCode,
                                                isStealthModeActive
                                            )
                                        } else {
                                            joinErrorMessage = "رمز الغرفة غير صحيح أو أن الغرفة قد تم إغلاقها."
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                enabled = !isVerifyingJoin
                            ) {
                                if (isVerifyingJoin) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(
                                        text = "الدخول إلى صالة السينما",
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
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

@Composable
private fun MovieRoomItemCard(
    room: PublicMoviesRoom,
    isAppOwner: Boolean,
    onEnter: () -> Unit,
    onCopyCode: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onEnter() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(room.hostAvatarBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = room.hostName.take(1),
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = room.title,
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "المضيف: ${room.hostName}",
                            fontFamily = TajawalFontFamily,
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .clickable { onCopyCode() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = room.roomCode,
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF2563EB)
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ الكود",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    if (isAppOwner) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "حذف الغرفة",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Movie Poster and Current Playing Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = room.posterUrl,
                    contentDescription = room.currentMovieTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.currentMovieTitle,
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFDC2626))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "بث حي",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${room.viewersCount} يشاهدون الآن",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Button(
                    onClick = onEnter,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "دخول",
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
