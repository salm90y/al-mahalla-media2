package com.ps1.netplay.ui.compose

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ps1.netplay.MainActivity

enum class RoomType {
    QURAN,
    DUAS,
    LATMIYAT,
    MAJALIS,
    FATAWA,
    AFRAH,
    TV_CHANNELS,
    MOVIES_SERIES,
    MEDIA,
    GAMES
}

data class RoomItemData(
    val type: RoomType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color,
    val badgeText: String = "نشط"
)

@Composable
fun RoomsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedRoomForExperience by remember { mutableStateOf<RoomType?>(null) }

    val allContentRooms = listOf(
        RoomItemData(
            type = RoomType.QURAN,
            title = "القرآن الكريم",
            subtitle = "تلاوات واستماع مباشر",
            icon = Icons.Default.MenuBook,
            iconColor = Color(0xFF059669),
            iconBgColor = Color(0xFFECFDF5),
            badgeText = "مباشر"
        ),
        RoomItemData(
            type = RoomType.DUAS,
            title = "أدعية وزيارات",
            subtitle = "مفاتيح الجنان والأذكار",
            icon = Icons.Default.VolunteerActivism,
            iconColor = Color(0xFF2563EB),
            iconBgColor = Color(0xFFEFF6FF),
            badgeText = "متاح"
        ),
        RoomItemData(
            type = RoomType.LATMIYAT,
            title = "لطميات ومراثي",
            subtitle = "إصدارات ومقاطع صوتية",
            icon = Icons.Default.MusicNote,
            iconColor = Color(0xFFDC2626),
            iconBgColor = Color(0xFFFEF2F2),
            badgeText = "جديد"
        ),
        RoomItemData(
            type = RoomType.MAJALIS,
            title = "مجالس حسينية",
            subtitle = "بثوث ومحاضرات مباشرة",
            icon = Icons.Default.Groups,
            iconColor = Color(0xFFD97706),
            iconBgColor = Color(0xFFFFFBEB),
            badgeText = "بث صوتي"
        ),
        RoomItemData(
            type = RoomType.FATAWA,
            title = "فتاوى وأحكام",
            subtitle = "إجابات الأسئلة الشرعية",
            icon = Icons.Default.HelpOutline,
            iconColor = Color(0xFF7C3AED),
            iconBgColor = Color(0xFFF5F3FF),
            badgeText = "إرشادات"
        ),
        RoomItemData(
            type = RoomType.AFRAH,
            title = "أفراح ومناسبات",
            subtitle = "مواليد وأناشيد بهيجة",
            icon = Icons.Default.AutoAwesome,
            iconColor = Color(0xFFDB2777),
            iconBgColor = Color(0xFFFDF2F8),
            badgeText = "مناسبات"
        ),
        RoomItemData(
            type = RoomType.TV_CHANNELS,
            title = "قنوات تلفزيونية",
            subtitle = "بث القنوات الفضائية",
            icon = Icons.Default.Tv,
            iconColor = Color(0xFF0284C7),
            iconBgColor = Color(0xFFF0F9FF),
            badgeText = "بث حي"
        ),
        RoomItemData(
            type = RoomType.MOVIES_SERIES,
            title = "أفلام ومسلسلات",
            subtitle = "مكتبة وثائقية ومرئية",
            icon = Icons.Default.Theaters,
            iconColor = Color(0xFF475569),
            iconBgColor = Color(0xFFF1F5F9),
            badgeText = "HD"
        ),
        RoomItemData(
            type = RoomType.MEDIA,
            title = "ميديا وتغطيات",
            subtitle = "تغطيات وتقارير مصورة",
            icon = Icons.Default.PlayCircleFilled,
            iconColor = Color(0xFF0D9488),
            iconBgColor = Color(0xFFF0FDFA),
            badgeText = "فيديو"
        ),
        RoomItemData(
            type = RoomType.GAMES,
            title = "ألعاب وبلايستيشن",
            subtitle = "ألعاب جماعية وبطولات",
            icon = Icons.Default.SportsEsports,
            iconColor = Color(0xFF4F46E5),
            iconBgColor = Color(0xFFEEF2FF),
            badgeText = "PlayStation"
        )
    )

    // Interactive Room Experience Dialog / Screen
    if (selectedRoomForExperience != null) {
        when (selectedRoomForExperience) {
            RoomType.QURAN -> {
                QuranHomeScreen(
                    navController = navController,
                    onBack = { selectedRoomForExperience = null }
                )
                return
            }
            RoomType.GAMES -> {
                GamesRoomScreen(
                    onBack = { selectedRoomForExperience = null },
                    onStartPs1 = {
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                return
            }
            else -> {
                val currentRoom = allContentRooms.find { it.type == selectedRoomForExperience }
                AlertDialog(
                    onDismissRequest = { selectedRoomForExperience = null },
                    confirmButton = {
                        Button(
                            onClick = { selectedRoomForExperience = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("دخول الغرفة", fontFamily = TajawalFontFamily, color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedRoomForExperience = null }) {
                            Text("رجوع", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                        }
                    },
                    title = {
                        Text(
                            text = currentRoom?.title ?: "غرفة تفاعلية",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    },
                    text = {
                        Text(
                            text = "مرحباً بك في غرفة ${currentRoom?.title}. الغرفة جاهزة ومجهزة بالبث والتواصل الصوتي والمرئي التفاعلي.",
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF64748B)
                        )
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(getAppScreenBackground())
            .statusBarsPadding()
    ) {
        // 1. Unified Top Bar: Title "الغرف" on Right (Start in RTL), Back navigation icon on Left (End in RTL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الغرف",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                color = Color(0xFF0F172A)
            )

            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2. Modern Grid of Room Cards (No Section Headers / Clean Minimalist Design)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(allContentRooms, key = { it.title }) { room ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (room.type == RoomType.GAMES) {
                                navController.navigate("games_room")
                            } else if (room.type == RoomType.QURAN) {
                                navController.navigate("quran_home")
                            } else {
                                selectedRoomForExperience = room.type
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Top row of Card: Icon on right (RTL start) + Badge on left (RTL end)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(room.iconBgColor)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = room.badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = room.iconColor
                                )
                            }

                            // Icon Box
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(room.iconBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = room.icon,
                                    contentDescription = room.title,
                                    tint = room.iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Room Title
                        Text(
                            text = room.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Room Subtitle
                        Text(
                            text = room.subtitle,
                            fontSize = 12.sp,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
