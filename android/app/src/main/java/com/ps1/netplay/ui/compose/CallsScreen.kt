package com.ps1.netplay.ui.compose

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
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
import org.json.JSONArray
import org.json.JSONObject

enum class CallStatus(val label: String) {
    OUTGOING("مكالمة صادرة"),
    INCOMING("مكالمة واردة"),
    MISSED("فائتة")
}

data class RealCallRecord(
    val id: String,
    val name: String,
    val avatar: String,
    val time: String,
    val isVideo: Boolean,
    val status: CallStatus,
    val timestamp: Long = System.currentTimeMillis()
)

object CallHistoryManager {
    private const val PREFS_NAME = "call_history_prefs"
    private const val KEY_HISTORY = "history_records"

    fun getHistory(context: Context): List<RealCallRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<RealCallRecord>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val statusStr = obj.optString("status", "OUTGOING")
                val status = try { CallStatus.valueOf(statusStr) } catch (e: Exception) { CallStatus.OUTGOING }
                val timeStr = obj.optString("time")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis() - i * 60000L)
                list.add(
                    RealCallRecord(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        avatar = obj.optString("avatar"),
                        time = timeStr,
                        isVideo = obj.optBoolean("isVideo", false),
                        status = status,
                        timestamp = timestamp
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun addRecord(context: Context, record: RealCallRecord) {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.id == record.id }
        current.add(0, record)
        val array = JSONArray()
        for (r in current.take(50)) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("avatar", r.avatar)
                put("time", r.time)
                put("isVideo", r.isVideo)
                put("status", r.status.name)
                put("timestamp", r.timestamp)
            }
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }
}

@Composable
fun CallsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: الكل, 1: الفائتة
    val tabs = listOf("الكل", "الفائتة")
    var searchQuery by remember { mutableStateOf("") }
    var callRecords by remember { mutableStateOf<List<RealCallRecord>>(emptyList()) }
    var showStartCallDialog by remember { mutableStateOf(false) }

    fun refreshHistory() {
        callRecords = CallHistoryManager.getHistory(context)
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    val filteredCalls = callRecords.filter {
        val matchTab = if (selectedTab == 1) it.status == CallStatus.MISSED else true
        val matchSearch = searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
        matchTab && matchSearch
    }

    if (showStartCallDialog) {
        var inputName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showStartCallDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val nameToCall = inputName.trim().ifEmpty { "صديق" }
                        val intent = Intent(context, CallActivity::class.java).apply {
                            putExtra("callID", "call_${System.currentTimeMillis()}")
                            putExtra("isVideo", false)
                            putExtra("targetUserName", nameToCall)
                        }
                        showStartCallDialog = false
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("اتصال الآن", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartCallDialog = false }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            title = {
                Text("بدء مكالمة جديدة", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    placeholder = { Text("أدخل اسم الصديق", fontFamily = TajawalFontFamily) },
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
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // 1. Unified Top Bar: Title "المكالمات" on Right (Start in RTL), Start Call Icon on Left (End in RTL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title "المكالمات"
            Text(
                text = "المكالمات",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                color = Color(0xFF0F172A)
            )

            // Start Call Icon Button (Free/flat icon, size 38dp)
            IconButton(
                onClick = { showStartCallDialog = true },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "بدء مكالمة",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2. Category Tabs (الكل / الفائتة - Starts with الكل on Right)
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

        // 4. Content (Empty State or Calls List)
        if (filteredCalls.isEmpty()) {
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
                                imageVector = Icons.Default.Call,
                                contentDescription = "Calls",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "لا توجد مكالمات",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ستظهر مكالماتك هنا عند إجرائها .",
                        fontSize = 14.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { showStartCallDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بدء مكالمة",
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
                items(filteredCalls) { call ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Right Side (Start in RTL): Avatar + Name & Subtitle
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFEEF4FB), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = call.name.take(1).uppercase().ifEmpty { "ص" },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        fontFamily = TajawalFontFamily
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = call.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        fontFamily = TajawalFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${call.status.label} • ${call.time}",
                                        fontSize = 12.sp,
                                        color = if (call.status == CallStatus.MISSED) Color(0xFFEF4444) else Color(0xFF64748B),
                                        fontFamily = TajawalFontFamily
                                    )
                                }
                            }

                            // Left Side (End in RTL): Call action button
                            IconButton(
                                onClick = {
                                    val intent = Intent(context, CallActivity::class.java).apply {
                                        putExtra("callID", "call_${call.name.hashCode()}")
                                        putExtra("isVideo", call.isVideo)
                                        putExtra("targetUserName", call.name)
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFEEF4FB), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = "اتصال",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
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
