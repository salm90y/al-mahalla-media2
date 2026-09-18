package com.ps1.netplay.ui.compose

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ps1.netplay.CallActivity
import com.ps1.netplay.SettingsActivity
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
    var requestsList by remember { mutableStateOf<List<FriendRequestItem>>(emptyList()) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    fun loadData() {
        CloudflareClient.getFriendsList(context) { friends ->
            friendsList = friends ?: emptyList()
        }
        CloudflareClient.getIncomingRequests(context) { requests ->
            requestsList = requests ?: emptyList()
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
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val nameToSend = inputFriendUsername.trim()
                        if (nameToSend.isNotBlank()) {
                            CloudflareClient.sendFriendRequest(context, nameToSend) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                        }
                        showAddFriendDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("إرسال الطلب", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            title = {
                Text("إضافة صديق جديد", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            },
            text = {
                OutlinedTextField(
                    value = inputFriendUsername,
                    onValueChange = { inputFriendUsername = it },
                    placeholder = { Text("اسم المستخدم أو الرقم التعريفي", fontFamily = TajawalFontFamily) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // 1. Top Bar: Title "الأصدقاء" on Right (Start in RTL), Action Icons on Left (End in RTL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right: Title "الأصدقاء"
            Text(
                text = "الأصدقاء",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                color = Color(0xFF0F172A)
            )

            // Left Action Icons (Add Friend & Settings)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Friend Icon Button
                IconButton(
                    onClick = {
                        showAddFriendDialog = true
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "إضافة صديق",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Settings Icon Button
                IconButton(
                    onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 2. Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFEEF4FB))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "ابحث عن صديق",
                                fontSize = 14.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // 3. Category Tabs (الأصدقاء / طلبات الصداقة - Starts with الأصدقاء on Right)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF4FB))
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == 0) {
                    items(filteredFriends) { friend ->
                        val encodedName = try { java.net.URLEncoder.encode(friend.name.ifEmpty { friend.username }, "UTF-8") } catch (_: Exception) { friend.name }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                                .padding(14.dp),
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
                                        .size(44.dp)
                                        .background(Color(0xFFEEF4FB), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = friend.name.take(1).uppercase().ifEmpty { "ص" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        fontFamily = TajawalFontFamily
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = friend.name,
                                        fontSize = 15.sp,
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
                                        .size(36.dp)
                                        .background(Color(0xFFEEF4FB), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "اتصال",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(17.dp)
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
                    }
                } else {
                    items(requestsList) { req ->
                        val reqDisplayName = req.fromUsername.ifEmpty { req.username }.ifEmpty { req.name }
                        val encodedReqName = try { java.net.URLEncoder.encode(reqDisplayName, "UTF-8") } catch (_: Exception) { reqDisplayName }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                                .padding(14.dp),
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
                                        .size(44.dp)
                                        .background(Color(0xFFEEF4FB), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = reqDisplayName.take(1).uppercase().ifEmpty { "ص" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        fontFamily = TajawalFontFamily
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = reqDisplayName,
                                        fontSize = 15.sp,
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
                    }
                }
            }
        }
    }
}

