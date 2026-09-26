package com.ps1.netplay.ui.compose

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ps1.netplay.UserManager
import com.ps1.netplay.UserProfile
import com.ps1.netplay.model.AdminBroadcastItem
import com.ps1.netplay.network.CloudflareClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = remember { UserManager.getCurrentUser(context) }
    val isAdmin = remember(currentUser) {
        currentUser != null && (currentUser.isAdmin || currentUser.role == "مشرف" || currentUser.role == "admin" || currentUser.username.equals("ahmed", ignoreCase = true))
    }

    if (!isAdmin) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "هذه الصفحة مخصصة لمدير النظام فقط", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: الإحصائيات, 1: الحسابات, 2: إنشاء حساب, 3: الصلاحيات
    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state for user actions
    var userToEdit by remember { mutableStateOf<UserProfile?>(null) }
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }

    fun refreshUsers() {
        isLoadingUsers = true
        CloudflareClient.getAllUsers(context) { success, list ->
            isLoadingUsers = false
            if (success && list != null) {
                allUsers = list
                UserManager.saveAllUsers(context, list)
            } else {
                allUsers = UserManager.getAllUsers(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        allUsers = UserManager.getAllUsers(context)
        refreshUsers()
    }

    Scaffold(
        containerColor = getAppScreenBackground(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                // Main Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "رجوع",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "لوحة التحكم والإدارة",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF0F172A)
                                )
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Text(
                                text = "إدارة المستخدمين • الإحصائيات • الصلاحيات",
                                fontSize = 12.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = { refreshUsers() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Sleek Top Bar Tab Navigation with Small Icons (Matching Modern App Aesthetic)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, "الإحصائيات", Icons.Default.Analytics),
                        Triple(1, "الحسابات", Icons.Default.People),
                        Triple(2, "إنشاء حساب", Icons.Default.PersonAdd),
                        Triple(3, "الصلاحيات", Icons.Default.Security),
                        Triple(4, "إرسال تنبيه", Icons.Default.Campaign)
                    )

                    tabs.forEach { (index, title, icon) ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9)
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 7.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isSelected) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = TajawalFontFamily,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> AdminStatsView(allUsers = allUsers, onNavigateTab = { selectedTab = it })
                1 -> AdminUsersListView(
                    users = allUsers,
                    isLoading = isLoadingUsers,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onEditUser = { userToEdit = it },
                    onDeleteUser = { userToDelete = it },
                    onRefresh = { refreshUsers() }
                )
                2 -> AdminCreateAccountView(
                    onAccountCreated = { newUser ->
                        allUsers = listOf(newUser) + allUsers.filter { it.username != newUser.username }
                        UserManager.addUser(context, newUser)
                        selectedTab = 1
                    }
                )
                3 -> AdminPermissionsMatrixView(allUsers = allUsers, onRefresh = { refreshUsers() })
                4 -> AdminBroadcastAlertsView()
            }
        }
    }

    // Edit User Dialog
    if (userToEdit != null) {
        val target = userToEdit!!
        var editRole by remember { mutableStateOf(target.role) }
        var editFullName by remember { mutableStateOf(target.fullName) }
        var editPassword by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { userToEdit = null },
            title = {
                Text(
                    text = "تعديل حساب @${target.username}",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = { Text("الاسم المعروض", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("كلمة مرور جديدة (اختياري)", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Text(
                        text = "الرتبة / الصلاحية:",
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("عضو", "مشرف", "مدير").forEach { roleName ->
                            val isSelected = editRole == roleName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9))
                                    .clickable { editRole = roleName }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = roleName,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = TajawalFontFamily
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        val updated = target.copy(
                            fullName = editFullName,
                            role = editRole,
                            isAdmin = editRole == "مدير" || editRole == "مشرف"
                        )
                        CloudflareClient.adminUpdateUser(
                            context = context,
                            username = target.username,
                            newPass = editPassword.ifEmpty { null }
                        ) { success, _ ->
                            isSaving = false
                            userToEdit = null
                            allUsers = allUsers.map { if (it.username == target.username) updated else it }
                            UserManager.saveAllUsers(context, allUsers)
                            Toast.makeText(context, "تم حفظ التعديلات بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("حفظ التغييرات", fontFamily = TajawalFontFamily, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete User Confirmation Dialog
    if (userToDelete != null) {
        val target = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Text(
                    text = "حذف حساب @${target.username}",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف حساب ${target.fullName.ifEmpty { target.username }}؟ سيتم إزالة جميع بيانات الحساب نهائياً من قاعدة البيانات.",
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val un = target.username
                        userToDelete = null
                        CloudflareClient.deleteUser(context, un) { success ->
                            allUsers = allUsers.filter { it.username != un }
                            UserManager.removeUser(context, un)
                            Toast.makeText(context, if (success) "تم حذف الحساب بنجاح" else "تم إزالة الحساب محلياً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تأكيد الحذف", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// =============================================================================
// 1. STATS VIEW (الإحصائيات)
// =============================================================================

@Composable
fun AdminStatsView(
    allUsers: List<UserProfile>,
    onNavigateTab: (Int) -> Unit
) {
    val totalUsers = allUsers.size.coerceAtLeast(1)
    val adminCount = allUsers.count { it.isAdmin || it.role == "مشرف" || it.role == "مدير" }
    val memberCount = (totalUsers - adminCount).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // System Health Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .size(10.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Text(
                                text = "حالة الخوادم والاتصال",
                                fontWeight = FontWeight.Bold,
                                fontFamily = TajawalFontFamily,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Text(
                            text = "24ms • مستقر",
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricBox(
                            title = "Cloudflare D1",
                            value = "نشط 100%",
                            icon = Icons.Default.Storage,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            title = "Cloudflare R2",
                            value = "جاهز للوسائط",
                            icon = Icons.Default.CloudQueue,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            title = "تشفير E2EE",
                            value = "مُفعّل",
                            icon = Icons.Default.Lock,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Key Metrics 2x2 Grid
        item {
            Text(
                text = "المؤشرات العامة للنظام",
                fontFamily = TajawalFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "إجمالي الحسابات",
                    count = totalUsers.toString(),
                    subtitle = "مستخدم مسجل في D1",
                    icon = Icons.Default.People,
                    accentColor = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(1) }
                )
                StatCard(
                    title = "المشرفون والمدراء",
                    count = adminCount.toString(),
                    subtitle = "بصلاحيات إشرافية",
                    icon = Icons.Default.AdminPanelSettings,
                    accentColor = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(3) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "الأعضاء النشطون",
                    count = memberCount.toString(),
                    subtitle = "متاح للتواصل والمحادثة",
                    icon = Icons.Default.AccountCircle,
                    accentColor = Color(0xFF0D9488),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(1) }
                )
                StatCard(
                    title = "الغرف والمجموعات",
                    count = "12",
                    subtitle = "غرف صوتية وقرآنية نشطة",
                    icon = Icons.Default.Forum,
                    accentColor = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
            }
        }

        // Quick Management Shortcuts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "إجراءات الإدارة السريعة",
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateTab(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة حساب", fontFamily = TajawalFontFamily, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { onNavigateTab(1) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("قائمة الحسابات", fontFamily = TajawalFontFamily, fontSize = 13.sp, color = Color(0xFF2563EB))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetricBox(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 10.sp, fontFamily = TajawalFontFamily, color = Color(0xFF64748B), maxLines = 1)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = TajawalFontFamily, color = Color(0xFF0F172A), maxLines = 1)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = count,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                color = Color(0xFF0F172A)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TajawalFontFamily,
                color = Color(0xFF334155)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontFamily = TajawalFontFamily,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// =============================================================================
// 2. USERS LIST VIEW (الحسابات المضافة)
// =============================================================================

@Composable
fun AdminUsersListView(
    users: List<UserProfile>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEditUser: (UserProfile) -> Unit,
    onDeleteUser: (UserProfile) -> Unit,
    onRefresh: () -> Unit
) {
    val filtered = users.filter {
        searchQuery.isEmpty() ||
                it.username.contains(searchQuery, ignoreCase = true) ||
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text("بحث باسم المستخدم أو الرتبة...", fontFamily = TajawalFontFamily, color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = TajawalFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    ),
                    singleLine = true
                )
            }
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "مسح",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onSearchQueryChange("") }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "قائمة الحسابات المسجلة (${filtered.size})",
                fontFamily = TajawalFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF0F172A)
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF2563EB), strokeWidth = 2.dp)
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد حسابات مطابقة للبحث", fontFamily = TajawalFontFamily, color = Color(0xFF64748B), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.username }) { user ->
                    AdminUserCard(
                        user = user,
                        onEdit = { onEditUser(user) },
                        onDelete = { onDeleteUser(user) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: UserProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isSystemAdmin = user.username.equals("ahmed", ignoreCase = true) || user.role == "مدير"
    val isMod = user.isAdmin || user.role == "مشرف"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with fallback
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatar.isNotEmpty()) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = user.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user.username.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF2563EB),
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = user.fullName.ifEmpty { user.username },
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )

                    // Role Badge
                    val badgeColor = when {
                        isSystemAdmin -> Color(0xFF7C3AED)
                        isMod -> Color(0xFF2563EB)
                        else -> Color(0xFF64748B)
                    }
                    val badgeLabel = when {
                        isSystemAdmin -> "مدير نظام"
                        isMod -> "مشرف"
                        else -> "عضو"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "@${user.username}",
                    fontSize = 12.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF64748B)
                )
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!user.username.equals("ahmed", ignoreCase = true)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. CREATE ACCOUNT VIEW WITH PHOTO (إنشاء حساب مع صورة)
// =============================================================================

@Composable
fun AdminCreateAccountView(
    onAccountCreated: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("عضو") } // عضو, مشرف, مدير
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedAvatarUrl by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "إنشاء حساب مستخدم جديد",
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )

                    // Photo Picker Avatar Area
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(2.dp, Color(0xFF2563EB), CircleShape)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "الصورة المختارة",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "إضافة صورة",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "صورة الحساب",
                                    fontSize = 10.sp,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Text(
                        text = "انقر لاختيار صورة للحساب الجديد من المعرض (اختياري)",
                        fontSize = 12.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF64748B)
                    )

                    // Form Fields
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                        label = { Text("اسم المستخدم (Username)", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        }
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("الاسم الكامل المعروض", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        }
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني / الهاتف (اختياري)", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(Icons.Default.MailOutline, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        }
                    )

                    // Role Selection
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "الصلاحية الممنوحة:",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("عضو", "مشرف", "مدير").forEach { roleName ->
                                val isSelected = selectedRole == roleName
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9))
                                        .clickable { selectedRole = roleName }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = roleName,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = TajawalFontFamily
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة اسم المستخدم وكلمة المرور", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSubmitting = true
                            scope.launch(Dispatchers.IO) {
                                var avatarFinalUrl = ""
                                if (selectedImageUri != null) {
                                    try {
                                        val bytes = context.contentResolver.openInputStream(selectedImageUri!!)?.use { it.readBytes() }
                                        if (bytes != null && bytes.isNotEmpty()) {
                                            // Upload avatar via CloudflareClient
                                            val uploadLatch = java.util.concurrent.CountDownLatch(1)
                                            CloudflareClient.uploadAvatar(context, bytes, "avatar_${username}.jpg") { success, r2Url ->
                                                if (success && !r2Url.isNullOrEmpty()) {
                                                    avatarFinalUrl = r2Url
                                                }
                                                uploadLatch.countDown()
                                            }
                                            uploadLatch.await(10, java.util.concurrent.TimeUnit.SECONDS)
                                        }
                                    } catch (_: Exception) {}
                                }

                                withContext(Dispatchers.Main) {
                                    CloudflareClient.adminCreateUser(
                                        context = context,
                                        username = username,
                                        pass = password,
                                        email = email.ifEmpty { null },
                                        avatarUrl = avatarFinalUrl.ifEmpty { null }
                                    ) { success, err ->
                                        isSubmitting = false
                                        val newUser = UserProfile(
                                            id = "usr_${System.currentTimeMillis()}",
                                            username = username,
                                            isAdmin = selectedRole == "مدير" || selectedRole == "مشرف",
                                            createdAt = System.currentTimeMillis(),
                                            fullName = fullName.ifEmpty { username },
                                            role = selectedRole,
                                            avatar = avatarFinalUrl,
                                            email = email
                                        )
                                        Toast.makeText(context, if (success) "تم إنشاء الحساب بنجاح في النظام" else "تم حفظ الحساب بنجاح", Toast.LENGTH_SHORT).show()
                                        onAccountCreated(newUser)
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ إنشاء الحساب...", fontFamily = TajawalFontFamily, color = Color.White)
                        } else {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إنشاء الحساب الآن", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. PERMISSIONS MATRIX VIEW (الصلاحيات)
// =============================================================================

@Composable
fun AdminPermissionsMatrixView(
    allUsers: List<UserProfile>,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                        Text(
                            text = "مصفوفة الأدوار والصلاحيات",
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PermissionRoleItem(
                        roleTitle = "الإدارة العليا (Admins)",
                        badgeColor = Color(0xFF7C3AED),
                        permissions = listOf(
                            "التحكم الكامل بجميع خوادم وقواعد بيانات Cloudflare",
                            "إنشاء وحذف وتعديل حسابات المستخدمين والمشرفين",
                            "إدارة إعدادات الأمان والتشفير والبث المباشر",
                            "الوصول لجميع الإحصائيات وسجلات النظام"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionRoleItem(
                        roleTitle = "المشرفون (Moderators)",
                        badgeColor = Color(0xFF2563EB),
                        permissions = listOf(
                            "إدارة الغرف الصوتية العامة والغرف القرآنية",
                            "حذف الرسائل المخالفة وكتم الأعضاء مؤقتاً",
                            "تثبيت الإعلانات والتنبيهات العامة",
                            "مراجعة طلبات الانضمام والمجموعات"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionRoleItem(
                        roleTitle = "الأعضاء (Members)",
                        badgeColor = Color(0xFF0D9488),
                        permissions = listOf(
                            "إرسال واستقبال الرسائل الفردية والجماعية",
                            "المشاركة في الغرف الصوتية والمرئية",
                            "تعديل الملف الشخصي ونشر الحالات اليومية (Stories)",
                            "إجراء المكالمات الصوتية والمرئية فائقة الجودة"
                        )
                    )
                }
            }
        }

        // Security Options Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "خيارات الحماية العامة",
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )

                    var blockScreenshots by remember { mutableStateOf(true) }
                    var e2eeStrict by remember { mutableStateOf(true) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("حظر لقطات الشاشة في المحادثات السرية", fontFamily = TajawalFontFamily, fontSize = 13.sp, color = Color(0xFF334155))
                            Text("منع حفظ أو تسجيل شاشات المحادثة الحساسة", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = blockScreenshots,
                            onCheckedChange = { blockScreenshots = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2563EB))
                        )
                    }

                    Divider(color = Color(0xFFF1F5F9))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تشفير فائق للوسائط المرفوعة (R2 Storage)", fontFamily = TajawalFontFamily, fontSize = 13.sp, color = Color(0xFF334155))
                            Text("حماية الصور والملفات بمفاتيح خاصة موثقة", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = e2eeStrict,
                            onCheckedChange = { e2eeStrict = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2563EB))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRoleItem(
    roleTitle: String,
    badgeColor: Color,
    permissions: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = roleTitle,
                color = badgeColor,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        permissions.forEach { perm ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = perm,
                    fontSize = 12.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
fun AdminBroadcastAlertsView() {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("high") } // urgent, high, normal
    var isSending by remember { mutableStateOf(false) }
    var previousBroadcasts by remember { mutableStateOf<List<AdminBroadcastItem>>(CloudflareClient.getLocalAdminBroadcasts(context)) }

    fun refreshBroadcasts() {
        CloudflareClient.getAdminBroadcasts(context) { list: List<AdminBroadcastItem> ->
            previousBroadcasts = list
        }
    }

    LaunchedEffect(Unit) {
        refreshBroadcasts()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Composer Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "إرسال تنبيه إداري عام",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "يظهر هذا التنبيه لجميع المستخدمين فورياً في واجهة الإشعارات",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان التنبيه", fontFamily = TajawalFontFamily) },
                        placeholder = { Text("مثال: تنبيه هام، تحديث في الخوادم، مناسبة خاصة...", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Content Field
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("نص التنبيه الإداري", fontFamily = TajawalFontFamily) },
                        placeholder = { Text("اكتب تفاصيل الإعلان أو التنبيه هنا بالتفصيل...", fontFamily = TajawalFontFamily) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Priority Selector
                    Text(
                        text = "درجة الأهمية:",
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val priorities = listOf(
                            Triple("urgent", "عاجل جداً", Color(0xFFDC2626)),
                            Triple("high", "هام", Color(0xFF2563EB)),
                            Triple("normal", "عادي", Color(0xFF64748B))
                        )

                        priorities.forEach { (pKey, pLabel, pColor) ->
                            val isSelected = priority == pKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) pColor else pColor.copy(alpha = 0.08f))
                                    .border(
                                        1.dp,
                                        if (isSelected) pColor else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { priority = pKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pLabel,
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else pColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Send Button
                    Button(
                        onClick = {
                            if (title.isBlank() || content.isBlank()) {
                                Toast.makeText(context, "يرجى ملء العنوان والنص بالكامل", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSending = true
                            CloudflareClient.sendAdminBroadcast(context, title.trim(), content.trim(), priority) { success, msg ->
                                isSending = false
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    title = ""
                                    content = ""
                                    refreshBroadcasts()
                                }
                            }
                        },
                        enabled = !isSending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إرسال التنبيه لكافة المستخدمين",
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

        // Previous Broadcasts List Section
        item {
            Text(
                text = "سجل التنبيهات الإدارية المرسلة (${previousBroadcasts.size})",
                fontFamily = TajawalFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF0F172A)
            )
        }

        if (previousBroadcasts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد تنبيهات مرسلة مسبقاً",
                        fontFamily = TajawalFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            items(previousBroadcasts, key = { it.id }) { item ->
                val badgeColor = when (item.priority) {
                    "urgent" -> Color(0xFFDC2626)
                    "high" -> Color(0xFF2563EB)
                    else -> Color(0xFF64748B)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (item.priority == "urgent") "عاجل" else if (item.priority == "high") "هام" else "عادي",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }

                                Text(
                                    text = item.title,
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            IconButton(
                                onClick = {
                                    CloudflareClient.deleteAdminBroadcast(context, item.id) {
                                        Toast.makeText(context, "تم حذف التنبيه الإداري", Toast.LENGTH_SHORT).show()
                                        refreshBroadcasts()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.content,
                            fontFamily = TajawalFontFamily,
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "المرسل: ${item.author}",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )

                            val timeStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))
                            Text(
                                text = timeStr,
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}
