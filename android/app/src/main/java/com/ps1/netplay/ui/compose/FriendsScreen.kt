package com.ps1.netplay.ui.compose

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ps1.netplay.CallActivity
import com.ps1.netplay.SettingsActivity
import com.ps1.netplay.UserManager
import com.ps1.netplay.UserProfile
import com.ps1.netplay.model.FriendItem
import com.ps1.netplay.model.FriendRequestItem
import com.ps1.netplay.network.CloudflareClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun FriendsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: الأصدقاء, 1: طلبات الصداقة
    val tabs = listOf("الأصدقاء", "طلبات الصداقة")
    var searchQuery by remember { mutableStateOf("") }
    var friendsList by remember { mutableStateOf<List<FriendItem>>(CloudflareClient.getLocalFriends(context)) }
    var requestsList by remember { mutableStateOf<List<FriendRequestItem>>(CloudflareClient.getLocalIncomingRequests(context)) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    fun loadData() {
        CloudflareClient.getFriendsList(context) { friends ->
            if (friends != null && friends.isNotEmpty()) {
                friendsList = friends
            } else if (friendsList.isEmpty()) {
                friendsList = CloudflareClient.getLocalFriends(context)
            }
        }
        CloudflareClient.getIncomingRequests(context) { requests ->
            if (requests != null) {
                requestsList = requests
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            loadData()
            delay(3500)
        }
    }

    LaunchedEffect(selectedTab) {
        loadData()
    }

    val filteredFriends = friendsList.filter {
        searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
    }

    if (showAddFriendDialog) {
        var inputFriendUsername by remember { mutableStateOf("") }
        var allUsers by remember { mutableStateOf<List<UserProfile>>(UserManager.getAllUsers(context)) }
        var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
        var isSearching by remember { mutableStateOf(false) }
        var hasSearched by remember { mutableStateOf(false) }
        var sentRequests by remember { mutableStateOf<Set<String>>(emptySet()) }

        val currentUserId = UserManager.getCurrentUser(context)?.id ?: ""
        val currentUsername = UserManager.getCurrentUser(context)?.username ?: ""

        fun performSearch(query: String) {
            val q = query.trim()
            if (q.isBlank()) {
                searchResults = emptyList()
                hasSearched = false
                return
            }
            isSearching = true
            hasSearched = true

            val filtered = allUsers.filter { user ->
                (user.id != currentUserId && user.username != currentUsername) &&
                (user.username.contains(q, ignoreCase = true) ||
                 user.fullName.contains(q, ignoreCase = true) ||
                 user.id.contains(q, ignoreCase = true))
            }
            searchResults = filtered
            isSearching = false
        }

        LaunchedEffect(Unit) {
            CloudflareClient.getAllUsers(context) { success, list ->
                if (success && list != null && list.isNotEmpty()) {
                    allUsers = list
                } else {
                    allUsers = UserManager.getAllUsers(context)
                }
            }
        }

        Dialog(
            onDismissRequest = { showAddFriendDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "إضافة صديق جديد",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "ابحث عن الأصدقاء بواسطة الاسم أو اليوزر",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showAddFriendDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Search Input & Search Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputFriendUsername,
                            onValueChange = {
                                inputFriendUsername = it
                                if (it.isBlank()) {
                                    searchResults = emptyList()
                                    hasSearched = false
                                } else {
                                    performSearch(it)
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "أدخل اسم أو يوزر الصديق...",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (inputFriendUsername.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            inputFriendUsername = ""
                                            searchResults = emptyList()
                                            hasSearched = false
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "مسح",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { performSearch(inputFriendUsername) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "بحث",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Dropdown Results List below the field
                    AnimatedVisibility(
                        visible = hasSearched || inputFriendUsername.isNotBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                .padding(8.dp)
                        ) {
                            if (isSearching) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            } else if (searchResults.isNotEmpty()) {
                                Text(
                                    text = "الحسابات المشابهة (${searchResults.size}):",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(searchResults, key = { it.id }) { user ->
                                        val isRequestSent = sentRequests.contains(user.id) || sentRequests.contains(user.username)
                                        val displayName = user.fullName.ifEmpty { user.username }
                                        val encodedName = try {
                                            java.net.URLEncoder.encode(displayName, "UTF-8")
                                        } catch (_: Exception) {
                                            displayName
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // Clicking on name/card opens personal profile
                                                    showAddFriendDialog = false
                                                    navController.navigate("user_profile/${user.id}/$encodedName")
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Mini Avatar
                                                if (user.avatar.isNotBlank()) {
                                                    AsyncImage(
                                                        model = user.avatar,
                                                        contentDescription = displayName,
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(Color(0xFFEEF4FB), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = displayName.take(1).uppercase().ifEmpty { "ص" },
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF2563EB),
                                                            fontFamily = TajawalFontFamily
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                // Name & Username
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            showAddFriendDialog = false
                                                            navController.navigate("user_profile/${user.id}/$encodedName")
                                                        }
                                                ) {
                                                    Text(
                                                        text = displayName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A),
                                                        fontFamily = TajawalFontFamily,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "@${user.username}",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF64748B),
                                                        fontFamily = TajawalFontFamily,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                // Send Friend Request Button
                                                if (isRequestSent) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFFDCFCE7),
                                                        contentColor = Color(0xFF16A34A)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = "تم الإرسال",
                                                                fontSize = 11.sp,
                                                                fontFamily = TajawalFontFamily,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            sentRequests = sentRequests + user.id + user.username
                                                            CloudflareClient.sendFriendRequest(context, user.username) { success, msg ->
                                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                                loadData()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PersonAdd,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(13.dp),
                                                            tint = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "طلب صداقة",
                                                            color = Color.White,
                                                            fontFamily = TajawalFontFamily,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // No direct match in database -> offer direct request to the typed text
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "لم يتم العثور على حساب مطابق في القائمة",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val target = inputFriendUsername.trim()
                                            if (target.isNotBlank()) {
                                                CloudflareClient.sendFriendRequest(context, target) { success, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    loadData()
                                                }
                                                showAddFriendDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "إرسال طلب مباشر لـ '$inputFriendUsername'",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cancel / Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddFriendDialog = false }) {
                            Text(
                                text = "إغلاق",
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFriendDialog = true },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "إضافة صديق",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Unified Top Bar: Title "الأصدقاء"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title "الأصدقاء"
                Text(
                    text = "الأصدقاء",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F172A)
                )
            }

        // 2. Category Tabs (الأصدقاء / طلبات الصداقة)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            tabs.forEachIndexed { index, tabName ->
                val isSelected = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedTab = index
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tabName,
                        fontSize = 15.sp,
                        fontFamily = TajawalFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(36.dp)
                            .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent, CircleShape)
                    )
                }
            }
        }

        // Quick action card when searching for a user not in the friends list
        if (selectedTab == 0 && searchQuery.isNotBlank() && filteredFriends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEEF4FB))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val target = searchQuery.trim()
                            CloudflareClient.sendFriendRequest(context, target) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال طلب", fontFamily = TajawalFontFamily, fontSize = 13.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "إضافة '$searchQuery'",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontFamily = TajawalFontFamily
                        )
                        Text(
                            text = "إرسال طلب صداقة لهذا المستخدم",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontFamily = TajawalFontFamily
                        )
                    }
                }
            }
        }

        // 4. Content (Empty State or Friends List)
        val showEmpty = (selectedTab == 0 && filteredFriends.isEmpty() && searchQuery.isBlank()) || (selectedTab == 1 && requestsList.isEmpty())

        if (showEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    // Circular Illustration
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(Color(0xFFEBF3FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(8.dp, RoundedCornerShape(22.dp))
                                .background(Color(0xFF2563EB), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Friends",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (selectedTab == 0) "لا يوجد أصدقاء بعد" else "لا توجد طلبات صداقة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (selectedTab == 0) "ابدأ بإضافة أصدقائك للتواصل معهم." else "ستظهر طلبات الصداقة الواردة هنا.",
                        fontSize = 14.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { showAddFriendDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إضافة صديق",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (selectedTab == 0) {
                    items(filteredFriends) { friend ->
                        val encodedName = try { java.net.URLEncoder.encode(friend.name.ifEmpty { friend.username }, "UTF-8") } catch (_: Exception) { friend.name }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("chat_detail/${friend.id}/$encodedName")
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Right Side (Start in RTL): Avatar + Name & Online Status
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            navController.navigate("user_profile/${friend.id}/$encodedName")
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFFEEF4FB), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = friend.name.take(1).uppercase().ifEmpty { "ص" },
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB),
                                            fontFamily = TajawalFontFamily
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = friend.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            fontFamily = TajawalFontFamily
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "متصل",
                                            fontSize = 12.sp,
                                            color = Color(0xFF10B981),
                                            fontFamily = TajawalFontFamily
                                        )
                                    }
                                }

                                // 2. Left Side (End in RTL): Actions (Call icon button + Message button)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Small Call Icon Button
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(context, CallActivity::class.java).apply {
                                                    putExtra("callID", "call_${friend.id}_${System.currentTimeMillis()}")
                                                    putExtra("isVideo", false)
                                                    putExtra("targetUserName", friend.name)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "جارٍ الاتصال بـ ${friend.name}...", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(Color(0xFFEEF4FB), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "اتصال",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Message Button
                                    Button(
                                        onClick = {
                                            navController.navigate("chat_detail/${friend.id}/$encodedName")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF4FB)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("مراسلة", color = Color(0xFF2563EB), fontFamily = TajawalFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 82.dp, end = 20.dp),
                                thickness = 0.6.dp,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }
                } else {
                    items(requestsList) { req ->
                        val reqDisplayName = req.fromUsername.ifEmpty { req.username }.ifEmpty { req.name }
                        val encodedReqName = try { java.net.URLEncoder.encode(reqDisplayName, "UTF-8") } catch (_: Exception) { reqDisplayName }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Right Side (Start in RTL): Avatar + Name & Request Subtitle
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            navController.navigate("user_profile/${req.fromUserId}/$encodedReqName")
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFFEEF4FB), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = reqDisplayName.take(1).uppercase().ifEmpty { "ص" },
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB),
                                            fontFamily = TajawalFontFamily
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = reqDisplayName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            fontFamily = TajawalFontFamily
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "يريد إضافتك كصديق",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            fontFamily = TajawalFontFamily
                                        )
                                    }
                                }

                                // 2. Left Side (End in RTL): Accept / Reject Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val friendToAdd = FriendItem(
                                                id = req.fromUserId.ifEmpty { req.id },
                                                username = reqDisplayName,
                                                avatarUrl = req.avatarUrl,
                                                status = "online",
                                                createdAt = System.currentTimeMillis()
                                            )
                                            CloudflareClient.addLocalFriend(context, friendToAdd)
                                            friendsList = CloudflareClient.getLocalFriends(context)
                                            CloudflareClient.respondFriendRequest(context, req.id, "accept") { success ->
                                                Toast.makeText(context, "تم قبول طلب الصداقة وأضيف إلى قائمة أصدقائك", Toast.LENGTH_SHORT).show()
                                                loadData()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("قبول", color = Color.White, fontFamily = TajawalFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            CloudflareClient.respondFriendRequest(context, req.id, "reject") { success ->
                                                Toast.makeText(context, "تم رفض طلب الصداقة", Toast.LENGTH_SHORT).show()
                                                loadData()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Text("رفض", color = Color(0xFF64748B), fontFamily = TajawalFontFamily, fontSize = 13.sp)
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 82.dp, end = 20.dp),
                                thickness = 0.6.dp,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }
                }
            }
        }
    }
}
}


