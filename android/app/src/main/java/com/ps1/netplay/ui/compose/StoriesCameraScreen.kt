package com.ps1.netplay.ui.compose

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.ps1.netplay.SettingsActivity

@Composable
fun StoriesCameraScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: حالتي, 1: المشاهدة
    val tabs = listOf("حالتي", "المشاهدة")
    var searchQuery by remember { mutableStateOf("") }
    var userStories by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // 1. Top Bar: Title "الحالات" on Right (Start in RTL), Action Icons on Left (End in RTL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right: Title "الحالات"
            Text(
                text = "الحالات",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                color = Color(0xFF0F172A)
            )

            // Left Action Icons (Add Story & Settings)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Story Icon Button
                IconButton(
                    onClick = {
                        Toast.makeText(context, "إضافة حالة جديدة", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "إضافة حالة",
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
                                text = "ابحث في الحالات",
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

        // 3. Category Tabs (حالتي / المشاهدة - Starts with حالتي on Right)
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
                            .width(28.dp)
                            .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent, CircleShape)
                    )
                }
            }
        }

        // 4. Content (Empty State)
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Stories",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "لا توجد حالات بعد",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "شارك لحظاتك مع أصدقائك",
                    fontSize = 14.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "الكاميرا مهيأة لإضافة حالة جديدة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إضافة حالة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color.White
                    )
                }
            }
        }
    }
}
