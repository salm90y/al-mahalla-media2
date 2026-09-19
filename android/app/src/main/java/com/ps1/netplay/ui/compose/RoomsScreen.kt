package com.ps1.netplay.ui.compose

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
    val icon: ImageVector,
    val color: Color = Color(0xFF2563EB)
)

@Composable
fun RoomsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoomForExperience by remember { mutableStateOf<RoomType?>(null) }

    val allContentRooms = listOf(
        RoomItemData(RoomType.QURAN, "قرآن كريم", Icons.Default.MenuBook),
        RoomItemData(RoomType.DUAS, "أدعية وازيارات", Icons.Default.VolunteerActivism),
        RoomItemData(RoomType.LATMIYAT, "لطميات", Icons.Default.MusicNote),
        RoomItemData(RoomType.MAJALIS, "مجالس", Icons.Default.Groups),
        RoomItemData(RoomType.FATAWA, "فتاوى", Icons.Default.HelpOutline),
        RoomItemData(RoomType.AFRAH, "أفراح", Icons.Default.AutoAwesome),
        RoomItemData(RoomType.TV_CHANNELS, "قنوات تلفزيونية", Icons.Default.Tv),
        RoomItemData(RoomType.MOVIES_SERIES, "أفلام ومسلسلات", Icons.Default.Theaters),
        RoomItemData(RoomType.MEDIA, "ميديا", Icons.Default.PlayCircleFilled),
        RoomItemData(RoomType.GAMES, "ألعاب", Icons.Default.SportsEsports)
    )

    val filteredRooms = allContentRooms.filter {
        searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true)
    }

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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
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
                            text = "مرحباً بك في غرفة ${currentRoom?.title}. الغرفة جاهزة ومجهزة بالبث الصوتي والمرئي عالي النقاء.",
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
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // 1. Top Bar: Title "الغرف" on Right (Start in RTL), Back navigation icon on Left (End in RTL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // محتويات الغرفة (2-Column Grid of Pills)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val chunked = filteredRooms.chunked(2)
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { room ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFEEF6FF))
                                .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(20.dp))
                                .clickable {
                                    if (room.type == RoomType.GAMES) {
                                        navController.navigate("games_room")
                                    } else if (room.type == RoomType.QURAN) {
                                        navController.navigate("quran_home")
                                    } else {
                                        selectedRoomForExperience = room.type
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = room.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF1E40AF),
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF2563EB), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = room.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // الغرف النشطة (Active Rooms) - أسفل محتويات الغرفة
        Text(
            text = "الغرف النشطة",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = TajawalFontFamily,
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            textAlign = TextAlign.Right
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Active room pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFEBF3FF))
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(24.dp))
                    .clickable { selectedRoomForExperience = RoomType.DUAS }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أدعية وازيارات",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0284C7)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF0284C7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolunteerActivism,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
