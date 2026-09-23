package com.ps1.netplay.ui.compose

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * High-fidelity, modern Call Screen matching the exact reference design:
 * - Soft pastel blue/white gradient background
 * - Top bar with circular back & 3-dots buttons
 * - Large circular avatar with double border ring & green online badge
 * - Bold user name in Tajawal font, status label, and live elapsed timer
 * - 6-button rounded action card: Mute, Speaker, Camera, Add Person, Keypad, More
 * - Floating circular red End-Call button at bottom
 */
@Composable
fun ModernCallScreen(
    callerName: String,
    callerAvatar: String = "",
    isVideoCall: Boolean = false,
    onEndCall: () -> Unit,
    onToggleMute: (Boolean) -> Unit = {},
    onToggleSpeaker: (Boolean) -> Unit = {},
    onToggleCamera: (Boolean) -> Unit = {},
    onSwitchCamera: () -> Unit = {}
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(isVideoCall) }
    var isCameraOn by remember { mutableStateOf(isVideoCall) }
    var isConnected by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showKeypad by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAddPersonDialog by remember { mutableStateOf(false) }

    // Live call duration counter
    LaunchedEffect(Unit) {
        delay(1500) // Simulated connection handshake
        isConnected = true
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7FBFE),
                        Color(0xFFEFF5FA),
                        Color(0xFFE4F0F9)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP BAR (Circular back on right, 3-dots on left for RTL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: 3-dots Menu Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        .clickable { showMoreMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Right: Back / Minimize Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        .clickable { onEndCall() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "رجوع",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. CENTER PROFILE SECTION (Avatar, Name, Status, Timer)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // Large Avatar with Glow and Status Dot
                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Soft Ring
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f))
                            .border(2.dp, Color(0xFFDCEBFA), CircleShape)
                    )

                    // Inner Avatar Container
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD6E8FA)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (callerAvatar.isNotBlank()) {
                            AsyncImage(
                                model = callerAvatar,
                                contentDescription = callerName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(76.dp)
                            )
                        }
                    }

                    // Green Online Status Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 14.dp, bottom = 12.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .border(3.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Caller Name
                Text(
                    text = callerName.ifBlank { "أحمد محمد" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F2942),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Status Text
                Text(
                    text = if (isConnected) "متصل الآن" else "جارٍ الاتصال...",
                    fontSize = 15.sp,
                    fontFamily = TajawalFontFamily,
                    color = if (isConnected) Color(0xFF64748B) else Color(0xFF3B82F6),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Timer Display
                Text(
                    text = if (isConnected) timeFormatted else "00:00",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF1E3A5F),
                    textAlign = TextAlign.Center
                )
            }

            // 3. ACTION CONTROL PANEL (Rounded White Card with 6 Buttons)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(32.dp), spotColor = Color(0x1A0F2942)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Row 1: Mute | Speaker | Camera
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. كتم الصوت (Mute)
                        CallActionButton(
                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = "كتم الصوت",
                            isActive = isMuted,
                            activeBgColor = Color(0xFFFEE2E2),
                            activeIconColor = Color(0xFFEF4444),
                            onClick = {
                                isMuted = !isMuted
                                onToggleMute(isMuted)
                            }
                        )

                        // 2. مكبر الصوت (Speaker)
                        CallActionButton(
                            icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            label = "مكبر الصوت",
                            isActive = isSpeakerOn,
                            activeBgColor = Color(0xFFEFF6FF),
                            activeIconColor = Color(0xFF2563EB),
                            onClick = {
                                isSpeakerOn = !isSpeakerOn
                                onToggleSpeaker(isSpeakerOn)
                            }
                        )

                        // 3. الكاميرا (Camera)
                        CallActionButton(
                            icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = "الكاميرا",
                            isActive = isCameraOn,
                            activeBgColor = Color(0xFF3B82F6),
                            activeIconColor = Color.White,
                            onClick = {
                                isCameraOn = !isCameraOn
                                onToggleCamera(isCameraOn)
                            }
                        )
                    }

                    // Row 2: Add Person | Keypad | More
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 4. إضافة شخص (Add Person)
                        CallActionButton(
                            icon = Icons.Default.PersonAdd,
                            label = "إضافة شخص",
                            isActive = false,
                            onClick = { showAddPersonDialog = true }
                        )

                        // 5. لوحة الأرقام (Keypad)
                        CallActionButton(
                            icon = Icons.Default.Dialpad,
                            label = "لوحة الأرقام",
                            isActive = showKeypad,
                            onClick = { showKeypad = true }
                        )

                        // 6. المزيد (More)
                        CallActionButton(
                            icon = Icons.Default.MoreVert,
                            label = "المزيد",
                            isActive = false,
                            onClick = { showMoreMenu = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. BOTTOM END-CALL BUTTON
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(72.dp)
                    .shadow(12.dp, CircleShape, spotColor = Color(0x4DEF4444))
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .clickable { onEndCall() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "إنهاء المكالمة",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }

    // Keypad Dialog
    if (showKeypad) {
        DialpadBottomSheet(
            onDismiss = { showKeypad = false },
            onDigitClick = { digit ->
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 60)
                    toneGen.startTone(ToneGenerator.TONE_DTMF_1, 150)
                } catch (_: Exception) {}
            }
        )
    }

    // More Menu Bottom Sheet
    if (showMoreMenu) {
        CallMoreOptionsDialog(
            onDismiss = { showMoreMenu = false },
            onSwitchCamera = {
                onSwitchCamera()
                showMoreMenu = false
                Toast.makeText(context, "تم تبديل الكاميرا", Toast.LENGTH_SHORT).show()
            },
            onToggleRecording = {
                showMoreMenu = false
                Toast.makeText(context, "تم بدء تسجيل المكالمة محلياً", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add Person Dialog
    if (showAddPersonDialog) {
        AddPersonToCallDialog(
            onDismiss = { showAddPersonDialog = false },
            onInvite = { name ->
                showAddPersonDialog = false
                Toast.makeText(context, "تم إرسال دعوة الانضمام إلى $name", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Reusable Circular Action Button with Label underneath
 */
@Composable
private fun CallActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    activeBgColor: Color = Color(0xFF3B82F6),
    activeIconColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(84.dp)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (isActive) activeBgColor else Color(0xFFF3F7FA))
                .border(
                    1.dp,
                    if (isActive) activeBgColor else Color(0xFFE2E8F0),
                    CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeIconColor else Color(0xFF3B82F6),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Interactive Dialpad overlay
 */
@Composable
fun DialpadBottomSheet(
    onDismiss: () -> Unit,
    onDigitClick: (String) -> Unit
) {
    var dialString by remember { mutableStateOf("") }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "لوحة الأرقام",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = dialString.ifEmpty { "..." },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(260.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(keys) { key ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable {
                                    dialString += key
                                    onDigitClick(key)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * More options bottom sheet dialog
 */
@Composable
fun CallMoreOptionsDialog(
    onDismiss: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleRecording: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "خيارات المكالمة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F172A)
                )

                ListItem(
                    headlineContent = { Text("تبديل الكاميرا (الأمامية / الخلفية)", fontFamily = TajawalFontFamily) },
                    leadingContent = { Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, tint = Color(0xFF3B82F6)) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSwitchCamera() }
                )

                ListItem(
                    headlineContent = { Text("تسجيل المكالمة", fontFamily = TajawalFontFamily) },
                    leadingContent = { Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color(0xFFEF4444)) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleRecording() }
                )

                ListItem(
                    headlineContent = { Text("جودة الصوت: فائقة الوضوح (HD Audio)", fontFamily = TajawalFontFamily) },
                    leadingContent = { Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color(0xFF10B981)) }
                )
            }
        }
    }
}

/**
 * Add Person to Call dialog
 */
@Composable
fun AddPersonToCallDialog(
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit
) {
    var personName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "إضافة شخص للمكالمة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F172A)
                )

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("اسم أو معرف الصديق", fontFamily = TajawalFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", fontFamily = TajawalFontFamily)
                    }
                    Button(
                        onClick = {
                            if (personName.isNotBlank()) onInvite(personName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إرسال دعوة", fontFamily = TajawalFontFamily, color = Color.White)
                    }
                }
            }
        }
    }
}
