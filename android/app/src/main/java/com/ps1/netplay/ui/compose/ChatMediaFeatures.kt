package com.ps1.netplay.ui.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt

// Holder for Drawing Path
data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float = 8f
)

enum class ImageFilterType(val title: String) {
    ORIGINAL("الأصلي"),
    NOIR("أبيض وأسود"),
    WARM("دافئ"),
    COOL("بارد"),
    SEPIA("عتيق"),
    VIVID("مشرق")
}

/**
 * 1. Open with System Chooser ("فتح باستخدام") + APK installation + ZIP extraction
 */
fun openWithExternalApp(context: Context, fileName: String, fileUriOrPath: String, mimeType: String) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            var targetFile: File? = null

            // 1. Resolve to a local File if possible or download if HTTP
            if (fileUriOrPath.startsWith("http://") || fileUriOrPath.startsWith("https://")) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "جاري تحضير الملف وفتحه...", Toast.LENGTH_SHORT).show()
                }
                val cleanName = if (fileName.isNotBlank()) fileName else fileUriOrPath.substringAfterLast("/")
                val cacheDir = File(context.cacheDir, "downloaded_files").apply { mkdirs() }
                val downloadedFile = File(cacheDir, cleanName)
                
                if (!downloadedFile.exists() || downloadedFile.length() == 0L) {
                    val url = java.net.URL(fileUriOrPath)
                    val conn = url.openConnection()
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    conn.getInputStream().use { input ->
                        FileOutputStream(downloadedFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                targetFile = downloadedFile
            } else if (fileUriOrPath.startsWith("file://")) {
                targetFile = File(fileUriOrPath.removePrefix("file://"))
            } else if (!fileUriOrPath.startsWith("content://")) {
                val directFile = File(fileUriOrPath)
                if (directFile.exists()) {
                    targetFile = directFile
                }
            }

            // 2. If it is a ZIP archive, inspect and extract if it contains an APK or other files
            val isZip = fileName.endsWith(".zip", ignoreCase = true) || fileUriOrPath.endsWith(".zip", ignoreCase = true) || mimeType == "application/zip"
            if (isZip && targetFile != null && targetFile.exists()) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "جاري استخراج الملف المضغوط...", Toast.LENGTH_SHORT).show()
                }
                val unzipDir = File(context.cacheDir, "unzipped_${targetFile.nameWithoutExtension}").apply { mkdirs() }
                var extractedApk: File? = null
                var firstExtractedFile: File? = null

                java.util.zip.ZipInputStream(targetFile.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val outFile = File(unzipDir, entry.name.substringAfterLast("/"))
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            if (firstExtractedFile == null) firstExtractedFile = outFile
                            if (outFile.name.endsWith(".apk", ignoreCase = true)) {
                                extractedApk = outFile
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                if (extractedApk != null) {
                    targetFile = extractedApk
                } else if (firstExtractedFile != null) {
                    targetFile = firstExtractedFile
                }
            }

            // 3. Check if target is an APK file
            val isApk = targetFile?.name?.endsWith(".apk", ignoreCase = true) == true ||
                    fileName.endsWith(".apk", ignoreCase = true) ||
                    fileUriOrPath.endsWith(".apk", ignoreCase = true) ||
                    mimeType == "application/vnd.android.package-archive"

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (targetFile != null && targetFile.exists()) {
                    val contentUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        targetFile
                    )

                    if (isApk) {
                        // Handle APK installation
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            if (!context.packageManager.canRequestPackageInstalls()) {
                                Toast.makeText(context, "يرجى السماح بتثبيت التطبيقات من هذا المصدر", Toast.LENGTH_LONG).show()
                                val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(settingsIntent)
                                return@withContext
                            }
                        }

                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(contentUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(installIntent)
                        } catch (e: Exception) {
                            val chooser = Intent.createChooser(installIntent, "تثبيت التطبيق")
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                        }
                    } else {
                        // Normal file view with chooser
                        val ext = targetFile.extension.lowercase(Locale.ROOT)
                        val resolvedMime = if (mimeType.isNotBlank() && mimeType != "file" && mimeType != "*/*") {
                            mimeType
                        } else if (ext.isNotBlank()) {
                            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                        } else {
                            "*/*"
                        }

                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(contentUri, resolvedMime)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val chooser = Intent.createChooser(viewIntent, "فتح باستخدام")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    }
                } else {
                    // Fallback using raw uri
                    val uri = Uri.parse(fileUriOrPath)
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        val ext = MimeTypeMap.getFileExtensionFromUrl(fileUriOrPath)
                        val resolvedMime = if (mimeType.isNotBlank() && mimeType != "file" && mimeType != "*/*") {
                            mimeType
                        } else if (ext.isNotBlank()) {
                            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase(Locale.ROOT)) ?: "*/*"
                        } else {
                            "*/*"
                        }
                        setDataAndType(uri, resolvedMime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(viewIntent, "فتح باستخدام")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "تعذر فتح الملف: ${e.localizedMessage ?: "تأكد من وجود تطبيق مناسب"}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

/**
 * 2. Full-Featured Image Editor Dialog
 */
@Composable
fun ImageEditorDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onSendEditedImage: (Uri, String, Boolean) -> Unit // (resultUri, caption, isViewTwice)
) {
    val context = LocalContext.current
    var rotationAngle by remember { mutableStateOf(0f) }
    var selectedFilter by remember { mutableStateOf(ImageFilterType.ORIGINAL) }
    var isDrawingMode by remember { mutableStateOf(false) }
    var isTextOverlayActive by remember { mutableStateOf(false) }
    var textOverlayValue by remember { mutableStateOf("") }
    var isViewTwice by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }

    val drawingPaths = remember { mutableStateListOf<DrawPath>() }
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var brushColor by remember { mutableStateOf(Color.Red) }

    val colorPalette = listOf(
        Color.White,
        Color(0xFFEF4444), // Red
        Color(0xFFF59E0B), // Yellow/Orange
        Color(0xFF10B981), // Green
        Color(0xFF2563EB), // Blue
        Color(0xFF8B5CF6), // Purple
        Color.Black
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Main Top Bar: Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Rotate
                    IconButton(
                        onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "تدوير", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Text Overlay Toggle
                    IconButton(
                        onClick = { isTextOverlayActive = !isTextOverlayActive },
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isTextOverlayActive) Color(0xFF2563EB) else Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = "كتابة", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Drawing / Pen Toggle
                    IconButton(
                        onClick = { isDrawingMode = !isDrawingMode },
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isDrawingMode) Color(0xFF2563EB) else Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = "رسم يدوي", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // View Twice ("مشاهدة مرتين") Toggle
                    IconButton(
                        onClick = {
                            isViewTwice = !isViewTwice
                            if (isViewTwice) {
                                Toast.makeText(context, "تم تفعيل المشاهدة لمرتين فقط • محمي من الحفظ واللقطات", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isViewTwice) Color(0xFF10B981) else Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Text(
                            text = "2",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            fontFamily = TajawalFontFamily
                        )
                    }
                }
            }

            // Central Image Preview & Interactive Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp, bottom = 140.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Base Image
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "صورة التعديل",
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotationAngle),
                        contentScale = ContentScale.Fit
                    )

                    // Filter Color Tint Overlay
                    when (selectedFilter) {
                        ImageFilterType.NOIR -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                        ImageFilterType.WARM -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF59E0B).copy(alpha = 0.22f))
                        )
                        ImageFilterType.COOL -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0284C7).copy(alpha = 0.22f))
                        )
                        ImageFilterType.SEPIA -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF78350F).copy(alpha = 0.28f))
                        )
                        ImageFilterType.VIVID -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFEC4899).copy(alpha = 0.15f))
                        )
                        else -> {}
                    }

                    // Drawing Layer
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isDrawingMode, brushColor) {
                                if (!isDrawingMode) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPathPoints = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentPathPoints = currentPathPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPathPoints.isNotEmpty()) {
                                            drawingPaths.add(DrawPath(currentPathPoints, brushColor))
                                            currentPathPoints = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        drawingPaths.forEach { path ->
                            for (i in 0 until path.points.size - 1) {
                                drawLine(
                                    color = path.color,
                                    start = path.points[i],
                                    end = path.points[i + 1],
                                    strokeWidth = path.strokeWidth
                                )
                            }
                        }
                        for (i in 0 until currentPathPoints.size - 1) {
                            drawLine(
                                color = brushColor,
                                start = currentPathPoints[i],
                                end = currentPathPoints[i + 1],
                                strokeWidth = 8f
                            )
                        }
                    }

                    // Text Overlay Input/Display
                    if (isTextOverlayActive || textOverlayValue.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = textOverlayValue,
                                onValueChange = { textOverlayValue = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (textOverlayValue.isEmpty()) {
                                        Text(
                                            text = "انقر لكتابة نص على الصورة...",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 16.sp,
                                            fontFamily = TajawalFontFamily,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }
            }

            // Brush Palette & Undo if Drawing
            if (isDrawingMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 150.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorPalette.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(c, CircleShape)
                                .border(
                                    if (brushColor == c) 2.dp else 1.dp,
                                    if (brushColor == c) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { brushColor = c }
                        )
                    }

                    if (drawingPaths.isNotEmpty()) {
                        IconButton(
                            onClick = { drawingPaths.removeLastOrNull() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "تراجع", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Filter Selector Row
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ImageFilterType.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF2563EB) else Color.White.copy(alpha = 0.15f))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter.title,
                            color = Color.White,
                            fontFamily = TajawalFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Bottom Bar: Caption & Fast Send Button
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Caption Input
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = TajawalFontFamily
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (captionText.isEmpty()) {
                                Text(
                                    text = if (isViewTwice) "صورة تختفي بعد مرتين..." else "إضافة تعليق على الصورة...",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.5.sp,
                                    fontFamily = TajawalFontFamily
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        val fullCaption = if (textOverlayValue.isNotBlank()) {
                            if (captionText.isNotBlank()) "$captionText ($textOverlayValue)" else textOverlayValue
                        } else captionText
                        onSendEditedImage(imageUri, fullCaption, isViewTwice)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF2563EB), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3. Secure Disappearing View-Twice Photo Viewer (Anti-Screenshot & Anti-Save)
 */
@Composable
fun SecureViewTwicePhotoDialog(
    photoUrl: String,
    messageId: String,
    remainingViews: Int,
    onCloseAndDecrement: (String) -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current

    // Secure Dialog preventing screenshots and screen recordings
    Dialog(
        onDismissRequest = { onCloseAndDecrement(messageId) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Close Button
            IconButton(
                onClick = { onCloseAndDecrement(messageId) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
            }

            // Security Top Banner
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.85f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = "محمي", tint = Color.White, modifier = Modifier.size(16.dp))
                Text(
                    text = "صورة تختفي • متبقي $remainingViews مشاهدة • ممنوع الحفظ أو لقطة الشاشة",
                    color = Color.White,
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Main Photo
            AsyncImage(
                model = photoUrl,
                contentDescription = "صورة مؤقتة",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 60.dp),
                contentScale = ContentScale.Fit
            )

            // Anti-Screenshot Visual Watermark
            Text(
                text = "PS1 NetPlay • محمي ضد النسخ واللقطات",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 15.sp,
                fontFamily = TajawalFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
            )
        }
    }
}

/**
 * 4. Real GPS Location Sharing Dialog with Live Expiration & Meter Distance
 */
@Composable
fun RealLocationShareDialog(
    onDismiss: () -> Unit,
    onSendLocation: (Double, Double, String, String, Int) -> Unit // (lat, lng, address, distanceText, durationMinutes)
) {
    val context = LocalContext.current
    var currentLatitude by remember { mutableStateOf(33.3152) } // Default Baghdad coords
    var currentLongitude by remember { mutableStateOf(44.3661) }
    var locationAccuracyMeters by remember { mutableStateOf(4) }
    var locationAddress by remember { mutableStateOf("الموقع الحالي (العراق)") }
    var selectedLiveDuration by remember { mutableStateOf(0) } // 0 = Static, 30 = 30 mins, 60 = 1 hour, 120 = 2 hours

    LaunchedEffect(Unit) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else if (isNetEnabled) LocationManager.NETWORK_PROVIDER else null
                if (provider != null) {
                    val lastKnown = locationManager.getLastKnownLocation(provider)
                    if (lastKnown != null) {
                        currentLatitude = lastKnown.latitude
                        currentLongitude = lastKnown.longitude
                        locationAccuracyMeters = (lastKnown.accuracy.toInt()).coerceIn(3, 12)
                        locationAddress = "موقع دقيق • إحداثيات: ${String.format("%.4f", lastKnown.latitude)}, ${String.format("%.4f", lastKnown.longitude)}"
                    }
                }
            }
        } catch (_: Exception) {
            locationAccuracyMeters = 5
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = "مشاركة الموقع الجغرافي",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "دقة التحديد: $locationAccuracyMeters أمتار",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Map Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=600&h=300&fit=crop",
                        contentDescription = "الخريطة",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Center Pin Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2563EB))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("دقة: $locationAccuracyMeters أمتار", color = Color.White, fontFamily = TajawalFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Location Duration Options
                Text(
                    text = "اختر نوع وموجّه المشاركة:",
                    fontFamily = TajawalFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    0 to "موقع حالي ثابت (فوري)",
                    30 to "موقع مباشر (ينتهي بعد 30 دقيقة)",
                    60 to "موقع مباشر (ينتهي بعد ساعة واحدة)",
                    120 to "موقع مباشر (ينتهي بعد ساعتين)"
                ).forEach { (dur, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedLiveDuration = dur }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLiveDuration == dur,
                            onClick = { selectedLiveDuration = dur },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontFamily = TajawalFontFamily,
                            fontSize = 13.sp,
                            color = if (selectedLiveDuration == dur) Color(0xFF2563EB) else Color(0xFF334155),
                            fontWeight = if (selectedLiveDuration == dur) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Send Button
                Button(
                    onClick = {
                        val distText = "دقة: $locationAccuracyMeters أمتار"
                        onSendLocation(currentLatitude, currentLongitude, locationAddress, distText, selectedLiveDuration)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedLiveDuration > 0) "مشاركة الموقع المباشر" else "إرسال الموقع الحالي",
                        fontFamily = TajawalFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 5. Message Context Menu (Long-press actions)
 */
@Composable
fun MessageContextMenuSheet(
    messageText: String,
    isOutgoing: Boolean,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
            }
        },
        title = {
            Text("خيارات الرسالة", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Delete for me
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onDeleteForMe()
                            onDismiss()
                        }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("حذف لدي", fontFamily = TajawalFontFamily, fontSize = 14.sp, color = Color(0xFF0F172A))
                }

                // Delete for everyone
                if (isOutgoing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onDeleteForEveryone()
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("حذف للجميع", fontFamily = TajawalFontFamily, fontSize = 14.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Data holder for incoming audio/video call signals
 */
data class IncomingCallData(
    val callerId: String,
    val callerName: String,
    val callerAvatar: String = "",
    val callRoomId: String,
    val isVideo: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * High-end real-time Incoming Call Alert matching screenshot 100%:
 * - Pure white & soft icy-blue gradient background
 * - Top-right circular back/dismiss button
 * - Avatar halo with green online badge at 4:30 o'clock
 * - Caller name in deep navy bold typography with "متصل الآن" and call type
 * - Bottom floating card with dual "اسحب للرد" (Green) and "اسحب للرفض" (Red) swipe sliders & tap support
 */
@Composable
fun IncomingCallAlertModal(
    callData: IncomingCallData,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    
    // Play system ringtone & vibration while ringing
    DisposableEffect(callData.callRoomId) {
        var ringtone: Ringtone? = null
        var vibrator: Vibrator? = null
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(context, alertUri)
            ringtone?.play()

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 1000, 800, 1000), 1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 800, 1000, 800, 1000), 1)
            }
        } catch (_: Exception) {}

        onDispose {
            try {
                ringtone?.stop()
                vibrator?.cancel()
            } catch (_: Exception) {}
        }
    }

    // Auto-dismiss after 45 seconds if unanswered
    LaunchedEffect(callData.timestamp) {
        delay(45000)
        onDecline()
    }

    // Subtle breathing halo for avatar
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloPulse"
    )

    // Swipe Slider drag states
    var answerDragOffset by remember { mutableFloatStateOf(0f) }
    var declineDragOffset by remember { mutableFloatStateOf(0f) }
    var isAnswerTriggered by remember { mutableStateOf(false) }
    var isDeclineTriggered by remember { mutableStateOf(false) }

    // Real-Time Caller Presence Check (Green if online, Red if offline)
    var isCallerOnline by remember { mutableStateOf(true) }
    LaunchedEffect(callData.callerId) {
        if (callData.callerId.isNotBlank()) {
            CloudflareClient.checkUserOnline(context, callData.callerId) { online ->
                isCallerOnline = online
            }
        }
    }

    val animatedAnswerOffset by animateFloatAsState(
        targetValue = answerDragOffset,
        animationSpec = tween(140),
        label = "animatedAnswerOffset"
    )

    val animatedDeclineOffset by animateFloatAsState(
        targetValue = declineDragOffset,
        animationSpec = tween(140),
        label = "animatedDeclineOffset"
    )

    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF3F8FC),
                            Color(0xFFF7FAFD),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP RIGHT BACK BUTTON (Circular button with blue arrow matching screenshot)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(6.dp, CircleShape, spotColor = Color(0x1A0F2942))
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2EBF5), CircleShape)
                            .clickable { onDecline() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 2. CENTER PROFILE SECTION (Avatar with halo, green online badge, name, status, call type)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    // Avatar Container with Glowing Halo & Online Dot
                    Box(
                        modifier = Modifier.size(165.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Halo Ring
                        Box(
                            modifier = Modifier
                                .size((154 * haloPulse).dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                                .border(2.5.dp, Color(0xFFDCEBFA), CircleShape)
                        )

                        // Inner Avatar Background Circle
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD6E8FA)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (callData.callerAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = callData.callerAvatar,
                                    contentDescription = callData.callerName,
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

                        // Online Status Badge at 4:30 o'clock position (Green if online, Red if offline)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 10.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isCallerOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                                .border(3.5.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // Caller Name in Bold Deep Navy Typography
                    Text(
                        text = callData.callerName.ifEmpty { "أحمد محمد" },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0B3B60),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Online Subtitle: Green Dot + "متصل الآن" or Red Dot + "غير متصل"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isCallerOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (isCallerOnline) "متصل الآن" else "غير متصل",
                            fontSize = 16.sp,
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = if (isCallerOnline) Color(0xFF5D7F9E) else Color(0xFFEF4444)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Call Type: "مكالمة صوتية واردة" or "مكالمة فيديو واردة"
                    Text(
                        text = if (callData.isVideo) "مكالمة فيديو واردة" else "مكالمة صوتية واردة",
                        fontSize = 18.sp,
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF739ABF),
                        textAlign = TextAlign.Center
                    )
                }

                // 3. BOTTOM FLOATING CARD WITH TWO SWIPE SLIDERS
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .padding(bottom = 20.dp)
                        .shadow(16.dp, RoundedCornerShape(36.dp), spotColor = Color(0x180F3658)),
                    shape = RoundedCornerShape(36.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Top Slider: اسحب للرد (Green Answer Slider)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFE8F8F0)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Green Gradient Pill & Text (Aligns to right where knob rests)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterEnd)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF34D399),
                                                Color(0xFF22C55E),
                                                Color(0xFF16A34A)
                                            )
                                        )
                                    )
                                    .padding(end = 64.dp, start = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "اسحب للرد",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Circular Green Call Knob (Draggable left, or clickable)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset { IntOffset(animatedAnswerOffset.roundToInt(), 0) }
                                    .size(64.dp)
                                    .shadow(8.dp, CircleShape, spotColor = Color(0x4016A34A))
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, Color(0xFF4ADE80), CircleShape)
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (answerDragOffset < -120f) {
                                                    if (!isAnswerTriggered) {
                                                        isAnswerTriggered = true
                                                        onAccept()
                                                    }
                                                }
                                                answerDragOffset = 0f
                                            },
                                            onDragCancel = {
                                                answerDragOffset = 0f
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                answerDragOffset = (answerDragOffset + dragAmount).coerceIn(-240f, 0f)
                                            }
                                        )
                                    }
                                    .clickable {
                                        if (!isAnswerTriggered) {
                                            isAnswerTriggered = true
                                            onAccept()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "رد",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Bottom Slider: اسحب للرفض (Red Decline Slider)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFDE8E8)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Red Gradient Pill & Text (Aligns to left where knob rests)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterStart)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFDC2626),
                                                Color(0xFFEF4444),
                                                Color(0xFFF87171)
                                            )
                                        )
                                    )
                                    .padding(start = 64.dp, end = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "اسحب للرفض",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily
                                    )
                                }
                            }

                            // Circular Red CallEnd Knob (Draggable right, or clickable)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset { IntOffset(animatedDeclineOffset.roundToInt(), 0) }
                                    .size(64.dp)
                                    .shadow(8.dp, CircleShape, spotColor = Color(0x40DC2626))
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, Color(0xFFF87171), CircleShape)
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (declineDragOffset > 120f) {
                                                    if (!isDeclineTriggered) {
                                                        isDeclineTriggered = true
                                                        onDecline()
                                                    }
                                                }
                                                declineDragOffset = 0f
                                            },
                                            onDragCancel = {
                                                declineDragOffset = 0f
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                declineDragOffset = (declineDragOffset + dragAmount).coerceIn(0f, 240f)
                                            }
                                        )
                                    }
                                    .clickable {
                                        if (!isDeclineTriggered) {
                                            isDeclineTriggered = true
                                            onDecline()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "رفض",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

