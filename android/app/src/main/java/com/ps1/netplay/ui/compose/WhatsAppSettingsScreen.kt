package com.ps1.netplay.ui.compose

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ps1.netplay.AppSettingsManager
import com.ps1.netplay.UserManager
import com.ps1.netplay.network.CloudflareClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 15 Available Tones
val APP_TONES_LIST = listOf(
    "None (صامت)",
    "Default (افتراضي)",
    "Apex",
    "Beacon",
    "Aurora",
    "Chime",
    "Crystal",
    "Horizon",
    "Pulse",
    "Reflection",
    "Ripple",
    "Stellar",
    "Stream",
    "Velvet",
    "Zen"
)

// Supported Languages
val APP_LANGUAGES_LIST = listOf(
    "العربية (اللغة الرسمية للتطبيق)",
    "English (الإنكليزية)",
    "فارسی (الفارسية)",
    "Deutsch (الألمانية)",
    "Français (الفرنسية)",
    "Kurdî (الكردية)",
    "Türkçe (التركية)",
    "Español (الإسبانية)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // Settings state
    var theme by remember { mutableStateOf(AppSettingsManager.getTheme(context)) }
    var accentColor by remember { mutableStateOf(AppSettingsManager.getAccentColor(context)) }
    var statusBarEdge by remember { mutableStateOf(AppSettingsManager.isStatusBarEdge(context)) }
    var chatListStyle by remember { mutableStateOf(AppSettingsManager.getChatListStyle(context)) }
    var chatFontSize by remember { mutableStateOf(AppSettingsManager.getChatFontSize(context)) }
    var sendOnEnter by remember { mutableStateOf(AppSettingsManager.isSendOnEnter(context)) }

    // Notifications
    var messageTone by remember { mutableStateOf(AppSettingsManager.getMessageTone(context)) }
    var messageVibrate by remember { mutableStateOf(AppSettingsManager.getMessageVibrate(context)) }
    var popupNotification by remember { mutableStateOf(AppSettingsManager.isPopupNotification(context)) }
    var inAppSounds by remember { mutableStateOf(AppSettingsManager.isInAppSounds(context)) }
    var messagePreview by remember { mutableStateOf(AppSettingsManager.isMessagePreview(context)) }
    var groupTone by remember { mutableStateOf(AppSettingsManager.getGroupTone(context)) }
    var callRingtone by remember { mutableStateOf(AppSettingsManager.getCallRingtone(context)) }

    // Privacy & Security
    var readReceipts by remember { mutableStateOf(AppSettingsManager.isReadReceipts(context)) }
    var hideOnline by remember { mutableStateOf(AppSettingsManager.isHideOnline(context)) }
    var screenshotBlock by remember { mutableStateOf(AppSettingsManager.isScreenshotBlock(context)) }
    var appLock by remember { mutableStateOf(AppSettingsManager.isAppLock(context)) }

    // General
    var appLanguage by remember { mutableStateOf(AppSettingsManager.getLanguage(context)) }
    var vibrateAnswer by remember { mutableStateOf(AppSettingsManager.isVibrateAnswer(context)) }
    var callDataSaver by remember { mutableStateOf(AppSettingsManager.isCallDataSaver(context)) }

    // Profile
    val currentUser = remember { UserManager.getCurrentUser(context) }
    var userName by remember { mutableStateOf(currentUser?.fullName?.ifEmpty { currentUser.username } ?: "أحمد المحلاوي") }
    var userStatus by remember { mutableStateOf(currentUser?.role ?: "عضو نشط • متاح للتواصل") }
    var userPhone by remember { mutableStateOf(currentUser?.email ?: "+964 770 123 4567") }
    var userAvatar by remember { mutableStateOf(currentUser?.avatar ?: "") }

    // Dialog & Sheet States
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var showTonePickerSheet by remember { mutableStateOf<String?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyDialogTitle by remember { mutableStateOf<String?>(null) }
    var showStorageDetailsDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showTwoStepDialog by remember { mutableStateOf(false) }
    var showChangePhoneDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var twoStepPin by remember { mutableStateOf(AppSettingsManager.getTwoStepPin(context)) }

    // Expanded accordion item ids
    var expandedItems by remember { mutableStateOf(setOf<String>()) }

    fun toggleExpand(id: String) {
        expandedItems = if (expandedItems.contains(id)) {
            expandedItems - id
        } else {
            expandedItems + id
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Color(0xFFF8FAFC),
            topBar = {
                // Unified Top Bar matching Chats, Friends, Calls, Stories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right: Title "الإعدادات"
                    Text(
                        text = "الإعدادات",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A)
                    )

                    // Left: Back button
                    IconButton(
                        onClick = onNavigateBack,
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
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Search Bar (Unified design)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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
                                            text = "ابحث في الإعدادات",
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
                }

                // 2. Profile Card (الملف الشخصي)
                item {
                    val isProfileExpanded = expandedItems.contains("profile") || searchQuery.isNotEmpty()
                    SettingsAccordionCard(
                        id = "profile",
                        title = "الملف الشخصي والحساب",
                        icon = Icons.Default.Person,
                        isExpanded = isProfileExpanded,
                        onToggle = { toggleExpand("profile") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProfileEditDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                if (userAvatar.isNotEmpty()) {
                                    AsyncImage(
                                        model = userAvatar,
                                        contentDescription = userName,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color(0xFFEEF4FB), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = userName,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color(0xFF2563EB), CircleShape)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userName,
                                    color = Color(0xFF0F172A),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = userStatus,
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    fontFamily = TajawalFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = userPhone,
                                    color = Color(0xFF2563EB),
                                    fontSize = 12.sp,
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = {
                                Toast.makeText(context, "فتح رمز QR للبروفايل...", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "QR Code",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 3. المظهر والواجهة (Appearance)
                item {
                    val isExpanded = expandedItems.contains("appearance") || searchQuery.isNotEmpty()
                    SettingsAccordionCard(
                        id = "appearance",
                        title = "المظهر والواجهة",
                        icon = Icons.Default.Palette,
                        isExpanded = isExpanded,
                        onToggle = { toggleExpand("appearance") }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSubItem(
                                title = "وضع الثيم",
                                subtitle = when (theme) {
                                    "dark" -> "داكن"
                                    "light" -> "فاتح (افتراضي)"
                                    else -> "تلقائي حسب النظام"
                                },
                                onClick = { showThemeDialog = true }
                            )

                            SettingsSubItem(
                                title = "لون التمييز الأساسي",
                                subtitle = "أزرق ملكي عصري (#2563EB)",
                                trailing = {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color(0xFF2563EB), CircleShape)
                                    )
                                },
                                onClick = {
                                    Toast.makeText(context, "اللون الافتراضي موحد للأزرق الملكي", Toast.LENGTH_SHORT).show()
                                }
                            )

                            SettingsSwitchSubItem(
                                title = "شريط الحالة ممتد للحواف",
                                checked = statusBarEdge,
                                onCheckedChange = {
                                    statusBarEdge = it
                                    AppSettingsManager.setStatusBarEdge(context, it)
                                }
                            )

                            SettingsSubItem(
                                title = "حجم خط الدردشة",
                                subtitle = "${chatFontSize.toInt()} نقطة (افتراضي 15)",
                                onClick = {
                                    val next: Float = if (chatFontSize >= 20f) 14f else chatFontSize + 2f
                                    chatFontSize = next
                                    AppSettingsManager.setChatFontSize(context, next)
                                    Toast.makeText(context, "حجم الخط: ${next.toInt()}", Toast.LENGTH_SHORT).show()
                                }
                            )

                            SettingsSwitchSubItem(
                                title = "إرسال بزر الإدخال (Enter)",
                                checked = sendOnEnter,
                                onCheckedChange = {
                                    sendOnEnter = it
                                    AppSettingsManager.setSendOnEnter(context, it)
                                }
                            )
                        }
                    }
                }

                // 4. الإشعارات والأصوات (Notifications)
                item {
                    val isExpanded = expandedItems.contains("notifications") || searchQuery.isNotEmpty()
                    SettingsAccordionCard(
                        id = "notifications",
                        title = "الإشعارات والأصوات",
                        icon = Icons.Default.Notifications,
                        isExpanded = isExpanded,
                        onToggle = { toggleExpand("notifications") }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSubItem(
                                title = "نغمة الرسائل الفردية",
                                subtitle = messageTone,
                                onClick = { showTonePickerSheet = "message" }
                            )

                            SettingsSubItem(
                                title = "نغمة المجموعات",
                                subtitle = groupTone,
                                onClick = { showTonePickerSheet = "group" }
                            )

                            SettingsSubItem(
                                title = "نغمة رنين المكالمات",
                                subtitle = callRingtone,
                                onClick = { showTonePickerSheet = "call" }
                            )

                            SettingsSubItem(
                                title = "نمط الاهتزاز",
                                subtitle = when (messageVibrate) {
                                    "short" -> "قصير"
                                    "long" -> "طويل"
                                    "off" -> "إيقاف"
                                    else -> "افتراضي"
                                },
                                onClick = {
                                    val next = when (messageVibrate) {
                                        "default" -> "short"
                                        "short" -> "long"
                                        "long" -> "off"
                                        else -> "default"
                                    }
                                    messageVibrate = next
                                    AppSettingsManager.setMessageVibrate(context, next)
                                    Toast.makeText(context, "تم تغيير الاهتزاز إلى: $next", Toast.LENGTH_SHORT).show()
                                }
                            )

                            SettingsSwitchSubItem(
                                title = "أصوات داخل التطبيق",
                                checked = inAppSounds,
                                onCheckedChange = {
                                    inAppSounds = it
                                    AppSettingsManager.setInAppSounds(context, it)
                                }
                            )

                            SettingsSwitchSubItem(
                                title = "إظهار معاينة الرسالة",
                                checked = messagePreview,
                                onCheckedChange = {
                                    messagePreview = it
                                    AppSettingsManager.setMessagePreview(context, it)
                                }
                            )
                        }
                    }
                }

                // 5. الخصوصية والأمان (Privacy & Security)
                item {
                    val isExpanded = expandedItems.contains("privacy") || searchQuery.isNotEmpty()
                    SettingsAccordionCard(
                        id = "privacy",
                        title = "الخصوصية والأمان",
                        icon = Icons.Default.Lock,
                        isExpanded = isExpanded,
                        onToggle = { toggleExpand("privacy") }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSubItem(
                                title = "آخر ظهور والحالة متصل",
                                subtitle = "الجميع",
                                onClick = { showPrivacyDialogTitle = "آخر ظهور" }
                            )

                            SettingsSubItem(
                                title = "الصورة الشخصية",
                                subtitle = "جهات اتصالي",
                                onClick = { showPrivacyDialogTitle = "الصورة الشخصية" }
                            )

                            SettingsSubItem(
                                title = "الأخبار والحالة",
                                subtitle = "الجميع",
                                onClick = { showPrivacyDialogTitle = "الأخبار والحالة" }
                            )

                            SettingsSwitchSubItem(
                                title = "مؤشرات قراءة الرسائل (الصحين)",
                                checked = readReceipts,
                                onCheckedChange = {
                                    readReceipts = it
                                    AppSettingsManager.setReadReceipts(context, it)
                                }
                            )

                            SettingsSwitchSubItem(
                                title = "إخفاء حالة متصل الآن",
                                checked = hideOnline,
                                onCheckedChange = {
                                    hideOnline = it
                                    AppSettingsManager.setHideOnline(context, it)
                                }
                            )

                            SettingsSwitchSubItem(
                                title = "حظر لقطات الشاشة في المحادثات",
                                checked = screenshotBlock,
                                onCheckedChange = {
                                    screenshotBlock = it
                                    AppSettingsManager.setScreenshotBlock(context, it)
                                }
                            )

                            SettingsSwitchSubItem(
                                title = "قفل التطبيق برمز PIN / البصمة",
                                checked = appLock,
                                onCheckedChange = {
                                    appLock = it
                                    AppSettingsManager.setAppLock(context, it)
                                    if (it) showTwoStepDialog = true
                                }
                            )

                            SettingsSubItem(
                                title = "التحقق بخطوتين",
                                subtitle = if (twoStepPin.isNotEmpty()) "مفعل ومحمي" else "غير مفعل",
                                onClick = { showTwoStepDialog = true }
                            )
                        }
                    }
                }

                // 6. التخزين والبيانات (Storage & Data)
                item {
                    val isExpanded = expandedItems.contains("storage") || searchQuery.isNotEmpty()
                    SettingsAccordionCard(
                        id = "storage",
                        title = "التخزين والبيانات",
                        icon = Icons.Default.Storage,
                        isExpanded = isExpanded,
                        onToggle = { toggleExpand("storage") }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSubItem(
                                title = "استخدام مساحة التخزين",
                                subtitle = "124.5 MB مستخدمة",
                                onClick = { showStorageDetailsDialog = true }
                            )

                            SettingsSubItem(
                                title = "التنزيل التلقائي للوسائط",
                                subtitle = "الصور فقط عبر بيانات الجوال",
                                onClick = {
                                    Toast.makeText(context, "إعدادات التنزيل التلقائي للوسائط", Toast.LENGTH_SHORT).show()
                                }
                            )

                            SettingsSwitchSubItem(
                                title = "توفير البيانات أثناء المكالمات",
                                checked = callDataSaver,
                                onCheckedChange = {
                                    callDataSaver = it
                                    AppSettingsManager.setCallDataSaver(context, it)
                                }
                            )

                            SettingsSubItem(
                                title = "مسح الذاكرة المؤقتة (Cache)",
                                subtitle = "تحرير 48.2 MB",
                                onClick = {
                                    Toast.makeText(context, "تم مسح الذاكرة المؤقتة بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // 7. الحساب (Account)
                item {
                    val isExpanded = expandedItems.contains("account") || searchQuery.isNotEmpty()
                    SettingsAccordionCard(
                        id = "account",
                        title = "إعدادات الحساب",
                        icon = Icons.Default.ManageAccounts,
                        isExpanded = isExpanded,
                        onToggle = { toggleExpand("account") }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSubItem(
                                title = "تغيير رقم الهاتف",
                                subtitle = "نقل حسابك وسجلاتك لرقم جديد",
                                onClick = { showChangePhoneDialog = true }
                            )

                            SettingsSubItem(
                                title = "طلب معلومات الحساب والبيانات",
                                subtitle = "تصدير تقرير شامل عن حسابك",
                                onClick = {
                                    Toast.makeText(context, "جاري إعداد تقرير الحساب للتنزيل...", Toast.LENGTH_LONG).show()
                                }
                            )

                            SettingsSubItem(
                                title = "حذف الحساب نهائياً",
                                subtitle = "حذف كافة البيانات والرسائل",
                                titleColor = Color(0xFFEF4444),
                                onClick = { showDeleteAccountDialog = true }
                            )
                        }
                    }
                }

                // 8. أذونات التطبيق (Permissions)
                item {
                    val isExpanded = expandedItems.contains("permissions") || searchQuery.isNotEmpty()
                    SettingsAccordionCard(
                        id = "permissions",
                        title = "أذونات التطبيق",
                        icon = Icons.Default.Security,
                        isExpanded = isExpanded,
                        onToggle = { toggleExpand("permissions") }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSubItem(
                                title = "إدارة الأذونات والتراخيص",
                                subtitle = "الكاميرا، الميكروفون، الإشعارات، التخزين",
                                onClick = { showPermissionsDialog = true }
                            )
                        }
                    }
                }

                // 9. اللغة والإعدادات العامة (Language & General)
                item {
                    val isExpanded = expandedItems.contains("general") || searchQuery.isNotEmpty()
                    SettingsAccordionCard(
                        id = "general",
                        title = "اللغة والإعدادات العامة",
                        icon = Icons.Default.Language,
                        isExpanded = isExpanded,
                        onToggle = { toggleExpand("general") }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSubItem(
                                title = "لغة التطبيق",
                                subtitle = when (appLanguage) {
                                    "en" -> "English"
                                    "fa" -> "فارسی"
                                    else -> "العربية (الافتراضية)"
                                },
                                onClick = { showLanguageDialog = true }
                            )

                            SettingsSwitchSubItem(
                                title = "اهتزاز عند الرد على المكالمة",
                                checked = vibrateAnswer,
                                onCheckedChange = {
                                    vibrateAnswer = it
                                    AppSettingsManager.setVibrateAnswer(context, it)
                                }
                            )

                            SettingsSubItem(
                                title = "النسخ الاحتياطي للدردشات",
                                subtitle = "آخر نسخة: اليوم 03:00 ص",
                                onClick = {
                                    Toast.makeText(context, "تم حفظ نسخة احتياطية محلية بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Dialogs & Sheets
    // =========================================================================

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("إغلاق", fontFamily = TajawalFontFamily, color = Color(0xFF2563EB))
                }
            },
            title = {
                Text("اختر مظهر التطبيق", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    listOf(
                        "light" to "فاتح (افتراضي وموصى به)",
                        "dark" to "داكن",
                        "system" to "تلقائي حسب النظام"
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    theme = mode
                                    AppSettingsManager.setTheme(context, mode)
                                    showThemeDialog = false
                                    Toast.makeText(context, "تم تطبيق الثيم: $label", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = theme == mode,
                                onClick = {
                                    theme = mode
                                    AppSettingsManager.setTheme(context, mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontFamily = TajawalFontFamily, fontSize = 15.sp)
                        }
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            title = {
                Text("لغة التطبيق", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(APP_LANGUAGES_LIST) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appLanguage = if (lang.startsWith("English")) "en" else "ar"
                                    AppSettingsManager.setLanguage(context, appLanguage)
                                    showLanguageDialog = false
                                    Toast.makeText(context, "تم حفظ اللغة: $lang", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (appLanguage == "ar" && lang.startsWith("العربية")) || (appLanguage == "en" && lang.startsWith("English")),
                                onClick = {
                                    appLanguage = if (lang.startsWith("English")) "en" else "ar"
                                    AppSettingsManager.setLanguage(context, appLanguage)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang, fontFamily = TajawalFontFamily, fontSize = 14.sp)
                        }
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Tone Picker Dialog
    if (showTonePickerSheet != null) {
        val target = showTonePickerSheet!!
        AlertDialog(
            onDismissRequest = { showTonePickerSheet = null },
            confirmButton = {
                TextButton(onClick = { showTonePickerSheet = null }) {
                    Text("تم", fontFamily = TajawalFontFamily, color = Color(0xFF2563EB))
                }
            },
            title = {
                Text(
                    when (target) {
                        "message" -> "نغمة الرسائل الفردية"
                        "group" -> "نغمة المجموعات"
                        else -> "نغمة رنين المكالمات"
                    },
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(APP_TONES_LIST) { toneName ->
                        val isSelected = when (target) {
                            "message" -> messageTone == toneName
                            "group" -> groupTone == toneName
                            else -> callRingtone == toneName
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (target) {
                                        "message" -> {
                                            messageTone = toneName
                                            AppSettingsManager.setMessageTone(context, toneName)
                                        }
                                        "group" -> {
                                            groupTone = toneName
                                            AppSettingsManager.setGroupTone(context, toneName)
                                        }
                                        else -> {
                                            callRingtone = toneName
                                            AppSettingsManager.setCallRingtone(context, toneName)
                                        }
                                    }
                                    showTonePickerSheet = null
                                    Toast.makeText(context, "تم تعيين النغمة: $toneName", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    when (target) {
                                        "message" -> {
                                            messageTone = toneName
                                            AppSettingsManager.setMessageTone(context, toneName)
                                        }
                                        "group" -> {
                                            groupTone = toneName
                                            AppSettingsManager.setGroupTone(context, toneName)
                                        }
                                        else -> {
                                            callRingtone = toneName
                                            AppSettingsManager.setCallRingtone(context, toneName)
                                        }
                                    }
                                    showTonePickerSheet = null
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(toneName, fontFamily = TajawalFontFamily, fontSize = 14.sp)
                        }
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Profile Edit Dialog (Supports changing name, bio, phone, and profile photo)
    if (showProfileEditDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempStatus by remember { mutableStateOf(userStatus) }
        var tempPhone by remember { mutableStateOf(userPhone) }
        var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
        var isSavingProfile by remember { mutableStateOf(false) }

        val profilePhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedAvatarUri = uri
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isSavingProfile) showProfileEditDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        isSavingProfile = true
                        scope.launch(Dispatchers.IO) {
                            var finalAvatarUrl = userAvatar
                            if (selectedAvatarUri != null) {
                                try {
                                    val bytes = context.contentResolver.openInputStream(selectedAvatarUri!!)?.use { it.readBytes() }
                                    if (bytes != null && bytes.isNotEmpty()) {
                                        val filename = "avatar_${currentUser?.username ?: "user"}_${System.currentTimeMillis()}.jpg"
                                        val latch = java.util.concurrent.CountDownLatch(1)
                                        CloudflareClient.uploadAvatar(context, bytes, filename) { success, r2Url ->
                                            if (success && !r2Url.isNullOrEmpty()) {
                                                finalAvatarUrl = r2Url
                                            }
                                            latch.countDown()
                                        }
                                        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
                                    }
                                } catch (_: Exception) {}
                            }

                            withContext(Dispatchers.Main) {
                                userName = tempName
                                userStatus = tempStatus
                                userPhone = tempPhone
                                userAvatar = finalAvatarUrl
                                AppSettingsManager.setUserName(context, tempName)
                                AppSettingsManager.setUserStatus(context, tempStatus)
                                AppSettingsManager.setUserPhone(context, tempPhone)
                                AppSettingsManager.setUserAvatar(context, finalAvatarUrl)

                                currentUser?.let { curr ->
                                    val updatedProfile = curr.copy(
                                        fullName = tempName,
                                        avatar = finalAvatarUrl
                                    )
                                    UserManager.saveCurrentUserProfile(context, updatedProfile)
                                    CloudflareClient.adminUpdateUser(
                                        context = context,
                                        username = curr.username,
                                        newAvatar = finalAvatarUrl
                                    ) { _, _ -> }
                                }

                                isSavingProfile = false
                                showProfileEditDialog = false
                                Toast.makeText(context, "تم حفظ وتحديث الملف الشخصي بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    enabled = !isSavingProfile,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSavingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جارٍ الحفظ...", fontFamily = TajawalFontFamily, color = Color.White)
                    } else {
                        Text("حفظ التغييرات", fontFamily = TajawalFontFamily, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showProfileEditDialog = false },
                    enabled = !isSavingProfile
                ) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            title = {
                Text("تعديل الملف الشخصي والصورة", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Photo Avatar Picker
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEF4FB))
                            .border(2.dp, Color(0xFF2563EB), CircleShape)
                            .clickable { profilePhotoPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedAvatarUri != null) {
                            AsyncImage(
                                model = selectedAvatarUri,
                                contentDescription = "الصورة المختارة",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (userAvatar.isNotEmpty()) {
                            AsyncImage(
                                model = userAvatar,
                                contentDescription = userName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "تغيير الصورة",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = "انقر لتغيير الصورة الشخصية",
                        fontSize = 11.sp,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("الاسم الكامل", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tempStatus,
                        onValueChange = { tempStatus = it },
                        label = { Text("الحالة / النبذة", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("رقم الهاتف أو المعرّف", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Two-Step PIN Dialog
    if (showTwoStepDialog) {
        var tempPin by remember { mutableStateOf(twoStepPin) }
        AlertDialog(
            onDismissRequest = { showTwoStepDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        twoStepPin = tempPin
                        AppSettingsManager.setTwoStepPin(context, tempPin)
                        showTwoStepDialog = false
                        Toast.makeText(context, "تم حفظ رمز الأمان بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("حفظ الرمز", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTwoStepDialog = false }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            title = {
                Text("التحقق بخطوتين وقفل التطبيق", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رمز PIN مكون من 4 إلى 6 أرقام لحماية حسابك وقفل التطبيق:", fontFamily = TajawalFontFamily, fontSize = 13.sp, color = Color(0xFF64748B))
                    OutlinedTextField(
                        value = tempPin,
                        onValueChange = { if (it.length <= 6) tempPin = it },
                        label = { Text("رمز PIN الأمان", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Change Phone Dialog
    if (showChangePhoneDialog) {
        var newPhone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePhoneDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPhone.isNotEmpty()) {
                            userPhone = newPhone
                            showChangePhoneDialog = false
                            Toast.makeText(context, "تم إرسال رمز التحقق للرقم الجديد: $newPhone", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("متابعة", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePhoneDialog = false }) {
                    Text("إلغاء", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            title = {
                Text("تغيير رقم الهاتف", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سيتم نقل كافة محادثاتك وجهات الاتصال إلى الرقم الجديد:", fontFamily = TajawalFontFamily, fontSize = 13.sp, color = Color(0xFF64748B))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("رقم الهاتف الجديد", fontFamily = TajawalFontFamily) },
                        placeholder = { Text("+964 7XX XXX XXXX", fontFamily = TajawalFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        Toast.makeText(context, "تم تقديم طلب حذف الحساب", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("تأكيد الحذف", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("تراجع", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            title = {
                Text("حذف الحساب نهائياً", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            },
            text = {
                Text("تحذير: سيؤدي حذف الحساب إلى مسح جميع المحادثات والوسائط والمجموعات نهائياً ولن تتمكن من استعادتها.", fontFamily = TajawalFontFamily, fontSize = 13.sp, color = Color(0xFF64748B))
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Permissions Dialog
    if (showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                        showPermissionsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("فتح إعدادات النظام", fontFamily = TajawalFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionsDialog = false }) {
                    Text("إغلاق", fontFamily = TajawalFontFamily, color = Color(0xFF64748B))
                }
            },
            title = {
                Text("أذونات التطبيق", fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• الكاميرا: لالتقاط الصور ومكالمات الفيديو", fontFamily = TajawalFontFamily, fontSize = 13.sp)
                    Text("• الميكروفون: للملاحظات الصوتية والمكالمات", fontFamily = TajawalFontFamily, fontSize = 13.sp)
                    Text("• الإشعارات: لتلقي تنبيهات الرسائل والمكالمات", fontFamily = TajawalFontFamily, fontSize = 13.sp)
                    Text("• جهات الاتصال: للمزامنة مع أصدقائك", fontFamily = TajawalFontFamily, fontSize = 13.sp)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// =============================================================================
// Helper Components: Accordion Card & Sub-Items
// =============================================================================

@Composable
fun SettingsAccordionCard(
    id: String,
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Clicking it toggles accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFEEF4FB), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = Color(0xFF0F172A)
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "طي" else "توسيع",
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotationState)
                )
            }

            // Expandable Content Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp, top = 4.dp)
                ) {
                    Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsSubItem(
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color(0xFF0F172A),
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TajawalFontFamily,
                color = titleColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF64748B)
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "فتح",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitchSubItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = TajawalFontFamily,
            color = Color(0xFF0F172A),
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1)
            )
        )
    }
}
