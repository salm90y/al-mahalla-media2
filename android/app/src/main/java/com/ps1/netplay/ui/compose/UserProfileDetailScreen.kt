package com.ps1.netplay.ui.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ps1.netplay.CallActivity
import com.ps1.netplay.model.FriendItem
import com.ps1.netplay.model.FriendRequestItem
import com.ps1.netplay.network.CloudflareClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDetailScreen(
    userId: String,
    userName: String,
    avatarUrl: String = "",
    onNavigateBack: () -> Unit,
    onOpenChat: (userId: String, userName: String) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var friendsList by remember { mutableStateOf<List<FriendItem>>(CloudflareClient.getLocalFriends(context)) }
    var incomingRequests by remember { mutableStateOf<List<FriendRequestItem>>(emptyList()) }
    var outgoingRequests by remember { mutableStateOf<List<FriendRequestItem>>(emptyList()) }
    var isLoadingAction by remember { mutableStateOf(false) }

    var showMuteDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Resolve current relationship state
    val normalizedTargetName = userName.trim().lowercase()
    val normalizedTargetId = userId.trim().lowercase()

    val matchedFriend = friendsList.find {
        it.id.equals(userId, ignoreCase = true) ||
        it.username.trim().lowercase() == normalizedTargetName ||
        it.id.trim().lowercase() == "u_$normalizedTargetName" ||
        "u_${it.username.trim().lowercase()}" == normalizedTargetId
    }
    val isFriend = matchedFriend != null

    val incomingReq = incomingRequests.find {
        it.fromUserId.equals(userId, ignoreCase = true) ||
        it.username.trim().lowercase() == normalizedTargetName ||
        it.fromUsername.trim().lowercase() == normalizedTargetName
    }
    val isIncomingPending = incomingReq != null

    val outgoingReq = outgoingRequests.find {
        it.toUserId.equals(userId, ignoreCase = true) ||
        it.username.trim().lowercase() == normalizedTargetName
    }
    val isOutgoingPending = outgoingReq != null

    fun reloadData() {
        CloudflareClient.getFriendsList(context) { list ->
            friendsList = list ?: emptyList()
        }
        CloudflareClient.getIncomingRequests(context) { reqs ->
            incomingRequests = reqs ?: emptyList()
        }
        CloudflareClient.getOutgoingRequests(context) { reqs ->
            outgoingRequests = reqs ?: emptyList()
        }
    }

    LaunchedEffect(userId, userName) {
        reloadData()
    }

    // Floating header visibility threshold
    val showStickyInfo by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 220 }
    }

    val displayAvatar = avatarUrl.ifEmpty {
        matchedFriend?.avatarUrl?.ifEmpty { null }
            ?: "https://ui-avatars.com/api/?name=${java.net.URLEncoder.encode(userName.ifEmpty { "User" }, "UTF-8")}&background=2563EB&color=fff&size=200"
    }

    // Call launcher helper
    fun startRealCall(isVideo: Boolean) {
        try {
            val myUserId = UserManager.getCurrentUser(context)?.username ?: "user_me"
            val callRoomId = "call_" + listOf(myUserId, userId.ifBlank { "partner" }).sorted().joinToString("_")

            if (userId.isNotEmpty()) {
                CloudflareClient.sendCloudflareMessage(
                    context = context,
                    receiverId = userId,
                    text = if (isVideo) "مكالمة فيديو واردة" else "مكالمة صوتية واردة",
                    type = if (isVideo) "video_call" else "audio_call",
                    mediaUrl = callRoomId,
                    fileName = userName
                ) { _, _ -> }
            }

            val intent = Intent(context, CallActivity::class.java).apply {
                putExtra("callID", callRoomId)
                putExtra("isVideo", isVideo)
                putExtra("targetUserName", userName)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "جارٍ بدء الاتصال بـ $userName...", Toast.LENGTH_SHORT).show()
        }
    }

    // Copy to clipboard helper
    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ $label بنجاح", Toast.LENGTH_SHORT).show()
    }

    // Dialogs
    if (showMuteDialog) {
        AlertDialog(
            onDismissRequest = { showMuteDialog = false },
            title = {
                Text(
                    text = "كتم إشعارات المحادثة",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر مدة كتم الإشعارات لهذا المستخدم:", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                    val durations = listOf("8 ساعات" to "8_hours", "أسبوع واحد" to "1_week", "دائماً" to "always")
                    durations.forEach { (label, value) ->
                        Button(
                            onClick = {
                                CloudflareClient.muteFriend(context, userId, value) {
                                    Toast.makeText(context, "تم كتم الإشعارات ($label)", Toast.LENGTH_SHORT).show()
                                    showMuteDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF4FB)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(label, color = Color(0xFF2563EB), fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMuteDialog = false }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = {
                Text("حظر المستخدم", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            },
            text = {
                Text("هل أنت متأكد من رغبتك في حظر '$userName'؟ لن يتمكن من مراسلتك أو الاتصال بك.", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
            },
            confirmButton = {
                Button(
                    onClick = {
                        CloudflareClient.blockFriend(context, userId) {
                            Toast.makeText(context, "تم حظر المستخدم", Toast.LENGTH_SHORT).show()
                            showBlockConfirmDialog = false
                            reloadData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حظر", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("حذف الصداقة", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            },
            text = {
                Text("هل تريد إزالة '$userName' من قائمة أصدقائك؟", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
            },
            confirmButton = {
                Button(
                    onClick = {
                        CloudflareClient.deleteFriend(context, userId) {
                            Toast.makeText(context, "تمت إزالة الصداقة", Toast.LENGTH_SHORT).show()
                            showDeleteConfirmDialog = false
                            reloadData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            // UNIFIED TOP BAR (Fixed with Smooth Sticky Floating Header for User)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right (Start in RTL): Back button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Middle: Smooth Animated Floating Name + Avatar when Scrolled
                AnimatedVisibility(
                    visible = showStickyInfo,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = userName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!showStickyInfo) {
                    Text(
                        text = "الملف الشخصي",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A)
                    )
                }

                // Left: Actions (Call / Direct Options)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { startRealCall(false) },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "اتصال صوتي",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // 1. Hero Profile Card with Floating Avatar & Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with Status Badge
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .shadow(8.dp, CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(3.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.take(1).uppercase().ifEmpty { "ص" },
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = TajawalFontFamily
                                )
                            }
                            // Online Green Indicator
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                                    .border(2.5.dp, Color.White, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // User Name
                        Text(
                            text = userName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // User Tag / Status
                        Text(
                            text = "@${userName.replace(" ", "_").lowercase()} • متصل الآن",
                            fontSize = 13.sp,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic Relationship Status Badge
                        when {
                            isFriend -> {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "أنتم أصدقاء على المنصة",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                            }
                            isIncomingPending -> {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "أرسل لك طلب صداقة • معلق",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }
                            }
                            isOutgoingPending -> {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassEmpty,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تم إرسال الطلب • بانتظار الرد",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "غير مضاف في قائمة الأصدقاء",
                                        fontSize = 13.sp,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Dynamic Action Buttons
                        if (isIncomingPending && incomingReq != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isLoadingAction = true
                                        CloudflareClient.respondFriendRequest(context, incomingReq.id, "accept") {
                                            isLoadingAction = false
                                            Toast.makeText(context, "تم قبول الصداقة بنجاح!", Toast.LENGTH_SHORT).show()
                                            reloadData()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تأكيد الصداقة", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isLoadingAction = true
                                        CloudflareClient.respondFriendRequest(context, incomingReq.id, "reject") {
                                            isLoadingAction = false
                                            Toast.makeText(context, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                                            reloadData()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إلغاء الطلب", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                }
                            }
                        } else if (isOutgoingPending) {
                            OutlinedButton(
                                onClick = {
                                    if (outgoingReq != null) {
                                        CloudflareClient.cancelOutgoingRequest(context, outgoingReq.id)
                                        Toast.makeText(context, "تم إلغاء طلب الصداقة", Toast.LENGTH_SHORT).show()
                                        reloadData()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إلغاء طلب الصداقة المعلق", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            }
                        } else if (!isFriend) {
                            Button(
                                onClick = {
                                    isLoadingAction = true
                                    CloudflareClient.sendFriendRequest(context, userName) { success, msg ->
                                        isLoadingAction = false
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        reloadData()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إرسال طلب صداقة", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Primary Chat & Real Calling Action Row
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Message Button
                            Button(
                                onClick = { onOpenChat(userId, userName) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مراسلة", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }

                            // Voice Call Button
                            IconButton(
                                onClick = { startRealCall(false) },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFFEEF4FB), RoundedCornerShape(14.dp))
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "اتصال صوتي", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                            }

                            // Video Call Button
                            IconButton(
                                onClick = { startRealCall(true) },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFFEEF4FB), RoundedCornerShape(14.dp))
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "مكالمة فيديو", tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            // 2. Account Information Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "معلومات الحساب",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF0F172A)
                        )

                        // Info Row: Username
                        ProfileInfoItem(
                            icon = Icons.Outlined.AccountCircle,
                            title = "اسم المستخدم",
                            value = "@${userName.replace(" ", "_").lowercase()}",
                            onCopy = { copyToClipboard("اسم المستخدم", userName) }
                        )

                        Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        // Info Row: User ID
                        ProfileInfoItem(
                            icon = Icons.Outlined.Fingerprint,
                            title = "الرقم التعريفي",
                            value = userId.ifEmpty { "u_${userName.lowercase()}" },
                            onCopy = { copyToClipboard("الرقم التعريفي", userId.ifEmpty { "u_${userName.lowercase()}" }) }
                        )

                        Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        // Info Row: Bio / Status
                        ProfileInfoItem(
                            icon = Icons.Outlined.Info,
                            title = "الحالة والنبذة",
                            value = "عضو في تطبيق المحلة • متاح للتواصل والألعاب"
                        )
                    }
                }
            }

            // 3. Shared Media & Content Statistics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الوسائط والملفات المشتركة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "0 ملف",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                fontFamily = TajawalFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MediaStatPill(icon = Icons.Outlined.Image, count = "0", label = "صور")
                            MediaStatPill(icon = Icons.Outlined.Videocam, count = "0", label = "فيديوهات")
                            MediaStatPill(icon = Icons.Outlined.Description, count = "0", label = "مستندات")
                            MediaStatPill(icon = Icons.Outlined.Mic, count = "0", label = "صوتيات")
                        }
                    }
                }
            }

            // 4. Privacy, Control & Options Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Mute
                        ProfileSettingRow(
                            icon = Icons.Outlined.NotificationsOff,
                            title = "كتم الإشعارات",
                            subtitle = "إيقاف تنبيهات المحادثة لفترة محددة",
                            iconTint = Color(0xFF2563EB),
                            onClick = { showMuteDialog = true }
                        )

                        // Block
                        ProfileSettingRow(
                            icon = Icons.Outlined.Block,
                            title = "حظر جهة الاتصال",
                            subtitle = "منع الرسائل والمكالمات من هذا المستخدم",
                            iconTint = Color(0xFFEF4444),
                            onClick = { showBlockConfirmDialog = true }
                        )

                        // Unfriend (if already friends)
                        if (isFriend) {
                            ProfileSettingRow(
                                icon = Icons.Outlined.PersonRemove,
                                title = "حذف من قائمة الأصدقاء",
                                subtitle = "إلغاء الصداقة المشتركة",
                                iconTint = Color(0xFFEF4444),
                                onClick = { showDeleteConfirmDialog = true }
                            )
                        }

                        // Report
                        ProfileSettingRow(
                            icon = Icons.Outlined.Report,
                            title = "إبلاغ عن الحساب",
                            subtitle = "إرسال تقرير لإدارة النظام",
                            iconTint = Color(0xFF64748B),
                            onClick = {
                                Toast.makeText(context, "تم استلام البلاغ، شكراً لتعاونك", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFFEEF4FB), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, color = Color(0xFF64748B), fontFamily = TajawalFontFamily)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), fontFamily = TajawalFontFamily)
        }

        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MediaStatPill(
    icon: ImageVector,
    count: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = count, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontFamily = TajawalFontFamily)
        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B), fontFamily = TajawalFontFamily)
    }
}

@Composable
private fun ProfileSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontFamily = TajawalFontFamily)
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF64748B), fontFamily = TajawalFontFamily)
        }

        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
    }
}
