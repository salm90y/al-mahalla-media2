package com.ps1.netplay.ui.compose
import android.widget.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.InsertPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    modifier: Modifier = Modifier,
    onSendText: (String) -> Unit = {}, onTyping: () -> Unit = {}, onSendFile: (Uri, String) -> Unit = {_,_->}) {
        val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var showAttachments by remember { mutableStateOf(false) }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "*/*"
            onSendFile(uri, mimeType)
            showAttachments = false
        }
    }
val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
val coroutineScope = rememberCoroutineScope()
val rotation by animateFloatAsState(
        targetValue = if (showAttachments) 45f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "plus_rotation"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left + Button
Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(6.dp, CircleShape)
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFF00D2D3), Color(0xFF6C5CE7))),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable {
                        coroutineScope.launch {
                            if (showAttachments) {
                                sheetState.hide()
                                showAttachments = false
                            } else {
                                showAttachments = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attachments",
                    tint = Color.White,
                    modifier = Modifier.rotate(rotation)
                )
            }
            // Center Text Field & Right Icons
Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .shadow(4.dp, RoundedCornerShape(26.dp))
                    .background(Color(0xFF161E33), RoundedCornerShape(26.dp))
                    .border(1.dp, Color(0xFF263352), RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it; onTyping() },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 14.5.sp, color = Color.White),
                    cursorBrush = SolidColor(Color(0xFF00D2D3)),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text("اكتب رسالة...", color = Color(0xFF8E9BAE), fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (text.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF00D2D3),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                onSendText(text)
                                text = ""
                            }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = Color(0xFF8E9BAE),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { Toast.makeText(context, "Voice recording coming soon", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
    }
    if (showAttachments) {
        ModalBottomSheet(
            onDismissRequest = { showAttachments = false },
            sheetState = sheetState,
            containerColor = Color(0xFF10172A),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = null
        ) {
            AttachmentGrid(onPickFile = { fileLauncher.launch(it) })
        }
    }}
@Composable
fun AttachmentGrid(onPickFile: (String) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AttachmentIcon("Gallery", Color(0xFFFF4757), Icons.Default.InsertPhoto) { onPickFile("image/*") }
            AttachmentIcon("Snap Camera", Color(0xFFFFA502), Icons.Default.CameraAlt)
            AttachmentIcon("Document", Color(0xFF2ED573), Icons.Default.InsertDriveFile) { onPickFile("*/*") }
            AttachmentIcon("Location", Color(0xFF1E90FF), Icons.Default.LocationOn)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AttachmentIcon("Rooms", Color(0xFF70A1FF), Icons.Default.SportsEsports)
            AttachmentIcon("Audio", Color(0xFF5352ED), Icons.Default.Mic) { onPickFile("audio/*") }
            AttachmentIcon("Contact", Color(0xFFFF6B81), Icons.Default.Person)
            AttachmentIcon("Poll", Color(0xFF222F3E), Icons.Default.Poll)
        }
        Spacer(modifier = Modifier.height(48.dp))
    }}
@Composable
fun AttachmentIcon(label: String, color: Color, icon: ImageVector, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp))
                .background(color, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }}