package com.ps1.netplay.ui.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class SurahItem(
    val number: Int,
    val name: String,
    val englishName: String,
    val versesCount: Int,
    val type: String
)

@Composable
fun QuranHomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentSurah by remember { mutableStateOf("سورة الفاتحة") }

    val surahs = listOf(
        SurahItem(1, "الفاتحة", "Al-Fatihah", 7, "مكية"),
        SurahItem(2, "البقرة", "Al-Baqarah", 286, "مدنية"),
        SurahItem(3, "آل عمران", "Ali 'Imran", 200, "مدنية"),
        SurahItem(4, "النساء", "An-Nisa'", 176, "مدنية"),
        SurahItem(5, "المائدة", "Al-Ma'idah", 120, "مدنية"),
        SurahItem(6, "الأنعام", "Al-An'am", 165, "مكية"),
        SurahItem(7, "الأعراف", "Al-A'raf", 206, "مكية"),
        SurahItem(8, "الأنفال", "Al-Anfal", 75, "مدنية"),
        SurahItem(9, "التوبة", "At-Tawbah", 129, "مدنية"),
        SurahItem(36, "يس", "Ya-Sin", 83, "مكية"),
        SurahItem(55, "الرحمن", "Ar-Rahman", 78, "مدنية"),
        SurahItem(67, "الملك", "Al-Mulk", 30, "مكية"),
        SurahItem(112, "الإخلاص", "Al-Ikhlas", 4, "مكية"),
        SurahItem(113, "الفلق", "Al-Falaq", 5, "مكية"),
        SurahItem(114, "الناس", "An-Nas", 6, "مكية")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color(0xFF0F172A))
            }

            Text(
                text = "غرفة القرآن الكريم",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TajawalFontFamily,
                color = Color(0xFF0F172A)
            )
        }

        // Quran Audio Player Card (Light Theme)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFEEF6FF))
                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                        val msg = if (isPlaying) "جاري تشغيل $currentSurah بصوت القارئ عبد الباسط عبد الصمد" else "تم إيقاف التلاوة مؤقتاً"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2563EB), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currentSurah,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF1E3A8A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "بصوت القارئ الشيخ عبد الباسط عبد الصمد",
                        fontSize = 12.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Surahs List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(surahs) { surah ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                        .clickable {
                            currentSurah = "سورة ${surah.name}"
                            isPlaying = true
                            Toast.makeText(context, "تم بدء تلاوة سورة ${surah.name}", Toast.LENGTH_SHORT).show()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${surah.versesCount} آيات • ${surah.type}",
                        fontSize = 12.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "سورة ${surah.name}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = surah.englishName,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEEF4FB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${surah.number}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }
    }
}
