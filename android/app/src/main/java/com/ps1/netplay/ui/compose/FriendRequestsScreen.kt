package com.ps1.netplay.ui.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ps1.netplay.model.FriendRequestItem
import com.ps1.netplay.network.CloudflareClient

@Composable
fun FriendRequestsScreen(navController: NavController) {
    val context = LocalContext.current
    var requestsList by remember { mutableStateOf<List<FriendRequestItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) } // 0 for Received, 1 for Sent

    fun loadData() {
        isLoading = true
        CloudflareClient.getIncomingRequests(context) { requests ->
            requestsList = requests ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1220)) // Dark background from design
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "طلبات الصداقة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color(0xFF1F2937), RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sent (المرسلة)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selectedTab == 1) Color(0xFF14B8A6) else Color.Transparent)
                    .clickable { selectedTab = 1 },
                contentAlignment = Alignment.Center
            ) {
                Text("المرسلة • 0", color = if (selectedTab == 1) Color.White else Color(0xFF9CA3AF), fontSize = 13.sp)
            }
            
            // Received (الواردة)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selectedTab == 0) Color(0xFF14B8A6) else Color.Transparent)
                    .clickable { selectedTab = 0 },
                contentAlignment = Alignment.Center
            ) {
                Text("الواردة • ${requestsList.size}", color = if (selectedTab == 0) Color.White else Color(0xFF9CA3AF), fontSize = 13.sp)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF14B8A6))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (selectedTab == 0) {
                    item {
                        Text(
                            text = "طلبات واردة",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                    if (requestsList.isEmpty()) {
                        item {
                            Text("لا توجد طلبات واردة", color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }
                    }
                    items(requestsList) { req ->
                        RequestItem(req = req, onAccept = {
                            CloudflareClient.respondFriendRequest(context, req.id, "accepted") { success ->
                                if (success) { Toast.makeText(context, "تم القبول", Toast.LENGTH_SHORT).show(); loadData() }
                            }
                        }, onReject = {
                            CloudflareClient.respondFriendRequest(context, req.id, "rejected") { success ->
                                if (success) { Toast.makeText(context, "تم الرفض", Toast.LENGTH_SHORT).show(); loadData() }
                            }
                        })
                    }
                } else {
                    item {
                        Text(
                            text = "دعوات مرسلة",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                    item {
                        Text("لا توجد دعوات مرسلة", color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
fun RequestItem(
    req: FriendRequestItem,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161E33))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onAccept,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF2563EB), CircleShape)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onReject,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF334155), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = req.username.ifEmpty { req.name },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = TajawalFontFamily
                )
                Text(
                    text = "طلب صداقة جديد",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = TajawalFontFamily
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (req.username.ifEmpty { req.name }).take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
