package com.ps1.netplay.ui.compose

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. DATA MODELS & ENUMS FOR YOUTUBE WATCH-PARTY ROOM
// ----------------------------------------------------
enum class YouTubeRoomSubTab {
    PLAYER,
    CHAT,
    LIVE_SYNC,
    INTERCOM,
    USERS,
    SETTINGS
}

data class YouTubeVideoItem(
    val id: String,
    val title: String,
    val channelTitle: String,
    val duration: String,
    val viewCount: String,
    val publishedTime: String,
    val thumbnailUrl: String,
    val category: String,
    val isLive: Boolean = false,
    val is4K: Boolean = true
)

data class YouTubeChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val time: String,
    val isMe: Boolean = false,
    val avatarColor: Color = Color(0xFF2563EB)
)

data class YouTubeRoomUser(
    val id: String,
    val name: String,
    val role: String,
    val isHost: Boolean = false,
    val isOnline: Boolean = true,
    val isSpeaking: Boolean = false,
    val avatarBg: Color = Color(0xFF2563EB)
)

// Curated 25+ Comprehensive Initial YouTube & Cloudflare Video Library
val INITIAL_YOUTUBE_CATALOG = listOf(
    YouTubeVideoItem(
        id = "yK4U4XkMhFk",
        title = "تلاوة خاشعة ومريحة للأعصاب من سورة مريم بصوت القارئ إسلام صبحي",
        channelTitle = "تلاوات القرآن الكريم المباركة",
        duration = "32:45",
        viewCount = "14.2M مشاهدة",
        publishedTime = "منذ 3 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=600&auto=format&fit=crop&q=80",
        category = "القرآن الكريم",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "jfKfPfyJRdk",
        title = "بث مباشر 24/7 للحرم المكي الشريف وقناة القرآن الكريم بجودة 4K Ultra HD",
        channelTitle = "قناة القرآن الكريم الرسمية",
        duration = "بث مباشر",
        viewCount = "48.9K يشاهدون الآن",
        publishedTime = "مباشر الآن",
        thumbnailUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=600&auto=format&fit=crop&q=80",
        category = "بث مباشر",
        isLive = true
    ),
    YouTubeVideoItem(
        id = "dQw4w9WgXcQ",
        title = "وثائقي مذهل: كيف غيرت الحوسبة السحابية وشبكات Cloudflare خوادم الإنترنت؟",
        channelTitle = "عالم التكنولوجيا والأفلام الوثائقية",
        duration = "24:18",
        viewCount = "2.1M مشاهدة",
        publishedTime = "منذ أسبوعين",
        thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80",
        category = "وثائقيات",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "live_makkah",
        title = "بث مباشر للمسجد النبوي الشريف - المدينة المنورة على مدار الساعة",
        channelTitle = "قناة السنة النبوية الفضائية",
        duration = "بث حي",
        viewCount = "22.4K يشاهدون الآن",
        publishedTime = "مباشر الآن",
        thumbnailUrl = "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=600&auto=format&fit=crop&q=80",
        category = "بث مباشر",
        isLive = true
    ),
    YouTubeVideoItem(
        id = "podcast_101",
        title = "بودكاست فنجان: أسرار بناء المستقبل والذكاء الاصطناعي مع نخبة المهندسين",
        channelTitle = "ثمانية / Thmanyah",
        duration = "1:42:10",
        viewCount = "3.8M مشاهدة",
        publishedTime = "منذ شهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&auto=format&fit=crop&q=80",
        category = "بودكاست",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "nature_relax_4k",
        title = "رحلة ساحرة عبر جبال الألب السويسرية والطبيعة الخلابة بجودة 4K 60FPS",
        channelTitle = "Earth Relax Studio",
        duration = "45:00",
        viewCount = "8.4M مشاهدة",
        publishedTime = "منذ 4 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80",
        category = "طبيعة وراحة",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "tech_future",
        title = "مراجعة شاملة: أفضل هواتف وتقنيات عام 2026 مقارنة بالذكاء الاصطناعي",
        channelTitle = "فيصل السيف | Tech Voice",
        duration = "19:50",
        viewCount = "1.6M مشاهدة",
        publishedTime = "منذ 5 أيام",
        thumbnailUrl = "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=600&auto=format&fit=crop&q=80",
        category = "تقنية",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "dua_kumayl",
        title = "دعاء كميل بن زياد بصوت خاشع ومؤثر مع الترجمة والكتابة الواضحة",
        channelTitle = "أدعية ومناجاة الصالحين",
        duration = "28:15",
        viewCount = "5.3M مشاهدة",
        publishedTime = "منذ 6 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=600&auto=format&fit=crop&q=80",
        category = "أدعية وزيارات",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "latmiya_arbaeen",
        title = "قصيدة يا راحلين إلى كربلاء - إصدار الأربعين التاريخي بصوت مهيب",
        channelTitle = "ميديا العتبة المقدسة",
        duration = "14:22",
        viewCount = "9.1M مشاهدة",
        publishedTime = "منذ شهرين",
        thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
        category = "لطميات ومراثي",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "space_james_webb",
        title = "وثائقي تلسكوب جيمس ويب الفضائي: أعمق صور التقطت لبدايات نشأة الكون",
        channelTitle = "ناشيونال جيوغرافيك الوثائقية",
        duration = "52:14",
        viewCount = "4.7M مشاهدة",
        publishedTime = "منذ 3 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&auto=format&fit=crop&q=80",
        category = "وثائقيات",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "islamic_history_baghdad",
        title = "بيت الحكمة في بغداد: العصر الذهبي للعلوم والترجمة والطب الإسلامي",
        channelTitle = "تاريخ وحضارة",
        duration = "36:40",
        viewCount = "1.9M مشاهدة",
        publishedTime = "منذ سنة",
        thumbnailUrl = "https://images.unsplash.com/photo-1569336415962-a4bd9f69cd83?w=600&auto=format&fit=crop&q=80",
        category = "وثائقيات",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "gaming_esports_2026",
        title = "نهائي بطولة العالم للألعاب الإلكترونية 2026: أقوى اللحظات والريمونتادا",
        channelTitle = "Esports Arabia",
        duration = "22:05",
        viewCount = "3.2M مشاهدة",
        publishedTime = "منذ 4 أيام",
        thumbnailUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=600&auto=format&fit=crop&q=80",
        category = "ألعاب وتحديات",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "nasheed_peace",
        title = "أنشودة سلام يا مهدي - الأداء المليوني العالمي الموحد بجودة استوديو",
        channelTitle = "أناشيد الهدى المباركة",
        duration = "08:50",
        viewCount = "28.5M مشاهدة",
        publishedTime = "منذ سنة",
        thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
        category = "أناشيد ومواليد",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "cooking_chef_iraq",
        title = "أصول طبخ القوزي العراقي والتمن العنبر على الطريقة التراثية الأصلية",
        channelTitle = "شيف بغداد الأصيل",
        duration = "21:30",
        viewCount = "2.4M مشاهدة",
        publishedTime = "منذ شهرين",
        thumbnailUrl = "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=600&auto=format&fit=crop&q=80",
        category = "طبخ وتراث",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "coding_fullstack_2026",
        title = "تعلم برمجة تطبيقات أندرويد وCompose وCloudflare من الصفر للاحتراف",
        channelTitle = "أكاديمية المطور العربي",
        duration = "1:15:30",
        viewCount = "890K مشاهدة",
        publishedTime = "منذ أسبوع",
        thumbnailUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&auto=format&fit=crop&q=80",
        category = "شروحات وتقنية",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "tajweed_rules_pro",
        title = "دورة أحكام التجويد الميسرة: مخارج الحروف والصفات بأسلوب عصري تفاعلي",
        channelTitle = "أكاديمية ترتيل القرآن",
        duration = "40:12",
        viewCount = "1.3M مشاهدة",
        publishedTime = "منذ 5 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1585036156171-384164a8c675?w=600&auto=format&fit=crop&q=80",
        category = "القرآن الكريم",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "planet_earth_oceans",
        title = "عجائب المحيطات العميقة: أسرار الكائنات البحرية المضيئة في ظلمات البحر",
        channelTitle = "عالم البحار والمحيطات",
        duration = "33:19",
        viewCount = "6.1M مشاهدة",
        publishedTime = "منذ 8 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=600&auto=format&fit=crop&q=80",
        category = "وثائقيات",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "dua_tawassul",
        title = "دعاء التوسل بصوت جماعي مهيب من الروضة الحيدرية المقدسة في النجف الأشرف",
        channelTitle = "العتبة العلوية المقدسة",
        duration = "19:40",
        viewCount = "7.2M مشاهدة",
        publishedTime = "منذ سنة",
        thumbnailUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=600&auto=format&fit=crop&q=80",
        category = "أدعية وزيارات",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "ai_robotics_future",
        title = "ثورة الروبوتات الذكية لعام 2026: كيف ستغير حياتنا والوظائف المستقبلية؟",
        channelTitle = "علوم المستقبل والتكنولوجيا",
        duration = "27:55",
        viewCount = "1.8M مشاهدة",
        publishedTime = "منذ 3 أسابيع",
        thumbnailUrl = "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600&auto=format&fit=crop&q=80",
        category = "تقنية",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "car_review_supercar",
        title = "تجربة قيادة أسرع سيارة كهربائية فائقة في العالم على حلبة دبي أوتودروم",
        channelTitle = "عرب جي تي | ArabGT",
        duration = "23:44",
        viewCount = "2.9M مشاهدة",
        publishedTime = "منذ أسبوعين",
        thumbnailUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=600&auto=format&fit=crop&q=80",
        category = "سيارات وسرعة",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "nahj_al_balagha",
        title = "شرح عهد الإمام علي (ع) لمالك الأشتر: أروع دستور إنساني لإدارة الدولة",
        channelTitle = "دروس ونهج البلاغة",
        duration = "48:20",
        viewCount = "1.1M مشاهدة",
        publishedTime = "منذ 7 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80",
        category = "محاضرات ودروس",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "quran_surah_yusuf",
        title = "سورة يوسف كاملة بصوت القارئ الشيخ عبد الباسط عبد الصمد رحمه الله جودة نادرة",
        channelTitle = "نوادر التلاوات الخالدة",
        duration = "58:10",
        viewCount = "33.4M مشاهدة",
        publishedTime = "منذ سنتين",
        thumbnailUrl = "https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=600&auto=format&fit=crop&q=80",
        category = "القرآن الكريم",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "astronomy_black_holes",
        title = "أسرار الثقوب السوداء وآفاق الحدث: رحلة إلى أقصى ما يعرفه علم الفلك",
        channelTitle = "الدحيح | ناشيونال وثائقي",
        duration = "26:15",
        viewCount = "4.2M مشاهدة",
        publishedTime = "منذ 4 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=600&auto=format&fit=crop&q=80",
        category = "وثائقيات",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "cyber_security_protect",
        title = "كيف تحمي هاتفك وبياناتك الشخصية من الاختراق والتجسس؟ نصائح خبير أمني",
        channelTitle = "الأمن السيبراني العربي",
        duration = "18:40",
        viewCount = "1.5M مشاهدة",
        publishedTime = "منذ 6 أيام",
        thumbnailUrl = "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=600&auto=format&fit=crop&q=80",
        category = "تقنية",
        isLive = false
    ),
    YouTubeVideoItem(
        id = "ziyarat_ashura",
        title = "زيارة عاشوراء بصوت الحاج ميثم التمار مع دعاء علقمة بجودة صوتية عالية",
        channelTitle = "صوت الحسين عليه السلام",
        duration = "24:50",
        viewCount = "12.8M مشاهدة",
        publishedTime = "منذ 9 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
        category = "أدعية وزيارات",
        isLive = false
    )
)

// Extra batch for "Search More / بحث أكثر" expansion
val EXTRA_EXPANDED_YOUTUBE_CATALOG = listOf(
    YouTubeVideoItem(
        id = "quran_rahman",
        title = "سورة الرحمن عروس القرآن الكريم بصوت شجي يدخل القلب مباشرة",
        channelTitle = "تلاوات مباركة",
        duration = "22:15",
        viewCount = "9.7M مشاهدة",
        publishedTime = "منذ 5 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=600&auto=format&fit=crop&q=80",
        category = "القرآن الكريم"
    ),
    YouTubeVideoItem(
        id = "cloudflare_edge_speed",
        title = "كيف تعمل شبكات تسريع الفيديو والبث التفاعلي عبر سيرفرات Cloudflare Edge؟",
        channelTitle = "شبكات وسحابة المطورين",
        duration = "16:30",
        viewCount = "640K مشاهدة",
        publishedTime = "منذ أسبوع",
        thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80",
        category = "تقنية"
    ),
    YouTubeVideoItem(
        id = "documentary_civilization",
        title = "تاريخ بلاد الرافدين: مهد الحضارات والكتابة والقوانين الأولى في العالم",
        channelTitle = "كنوز التاريخ والحضارات",
        duration = "44:10",
        viewCount = "3.1M مشاهدة",
        publishedTime = "منذ 8 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1569336415962-a4bd9f69cd83?w=600&auto=format&fit=crop&q=80",
        category = "وثائقيات"
    ),
    YouTubeVideoItem(
        id = "podcast_health_brain",
        title = "كيف تبني ذاكرة خارقة وتحافظ على صحة عقلك؟ استشارات دكتور أعصاب",
        channelTitle = "صحة وحياة",
        duration = "55:20",
        viewCount = "2.0M مشاهدة",
        publishedTime = "منذ أسبوعين",
        thumbnailUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&auto=format&fit=crop&q=80",
        category = "بودكاست"
    ),
    YouTubeVideoItem(
        id = "dua_iftitah",
        title = "دعاء الافتتاح بصوت الشيخ جمعة حامد في ليالي شهر رمضان المبارك",
        channelTitle = "مكتبة الأدعية الرمضانية",
        duration = "26:30",
        viewCount = "4.5M مشاهدة",
        publishedTime = "منذ 6 أشهر",
        thumbnailUrl = "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=600&auto=format&fit=crop&q=80",
        category = "أدعية وزيارات"
    ),
    YouTubeVideoItem(
        id = "esports_championship_live",
        title = "تحديات ومهارات كروية استثنائية: أفضل أهداف وتمريرات الموسم الكروي",
        channelTitle = "كرة القدم العالمية HD",
        duration = "17:45",
        viewCount = "5.8M مشاهدة",
        publishedTime = "منذ 3 أيام",
        thumbnailUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=600&auto=format&fit=crop&q=80",
        category = "رياضة"
    )
)

// Autocomplete suggestion keywords
val SEARCH_AUTOCOMPLETE_SUGGESTIONS = listOf(
    "تلاوة القارئ إسلام صبحي سورة مريم",
    "بث مباشر الحرم المكي 4K",
    "وثائقي الحوسبة وشبكات Cloudflare",
    "بودكاست فنجان ثمانية",
    "طبيعة ساحرة 4K جبال الألب",
    "مراجعة هواتف وتقنيات الذكاء الاصطناعي",
    "دعاء كميل بصوت خاشع",
    "قصيدة يا راحلين إلى كربلاء",
    "تلسكوب جيمس ويب الفضائي",
    "حضارة بيت الحكمة والعلوم الإسلامية",
    "نهائي بطولة الألعاب الإلكترونية",
    "أنشودة سلام يا مهدي",
    "طبخ القوزي العراقي والتمن العنبر",
    "برمجة تطبيقات أندرويد Jetpack Compose",
    "أحكام التجويد ومخارج الحروف",
    "عجائب المحيطات العميقة",
    "دعاء التوسل العتبة العلوية المقدسة",
    "ثورة الروبوتات والذكاء الاصطناعي 2026",
    "شرح عهد الإمام علي لمالك الأشتر",
    "سورة يوسف الشيخ عبد الباسط عبد الصمد",
    "أسرار الثقوب السوداء في الفضاء",
    "الأمن السيبراني وحماية الهواتف",
    "زيارة عاشوراء ميثم التمار"
)

// ----------------------------------------------------
// 2. MAIN COMPOSABLE: YouTubeRoomScreen
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeRoomScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Sub-tab selection (PLAYER default)
    var activeSubTab by remember { mutableStateOf(YouTubeRoomSubTab.PLAYER) }

    // Video Catalog state with expansion capability
    val videoCatalog = remember {
        mutableStateListOf<YouTubeVideoItem>().apply {
            addAll(INITIAL_YOUTUBE_CATALOG)
        }
    }

    // Active currently playing video
    var currentVideo by remember { mutableStateOf<YouTubeVideoItem>(videoCatalog[0]) }

    // Playback state
    var isPlaying by remember { mutableStateOf(true) }
    var isWebPlayerActive by remember { mutableStateOf(false) } // Toggle between rich Poster HUD & Embedded Web Player
    var currentPositionSeconds by remember { mutableStateOf(245) }
    val totalDurationSeconds by remember { mutableStateOf(1965) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var currentQuality by remember { mutableStateOf("1080p 60fps (Cloudflare CDN)") }
    var isSynchronizedWithRoom by remember { mutableStateOf(true) }
    var liveViewerCount by remember { mutableStateOf(1480) }

    // Room Queue Playlist state
    val queuePlaylist = remember {
        mutableStateListOf(
            videoCatalog[1],
            videoCatalog[2],
            videoCatalog[4],
            videoCatalog[7]
        )
    }

    // Search Bar & Instant Autocomplete Dropdown State
    var searchQuery by remember { mutableStateOf("") }
    var isDropdownOpen by remember { mutableStateOf(false) }
    var isSearchModalOpen by remember { mutableStateOf(false) }
    var selectedFilterCategory by remember { mutableStateOf("الكل") }
    var isExpandedSearchLoading by remember { mutableStateOf(false) }
    var extraResultsCount by remember { mutableStateOf(0) }

    // Filtered suggestions for the live dropdown
    val filteredSuggestions = remember(searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            emptyList()
        } else {
            SEARCH_AUTOCOMPLETE_SUGGESTIONS.filter {
                it.contains(searchQuery.trim(), ignoreCase = true)
            }.take(6)
        }
    }

    // Live Room Chat Messages State
    val chatMessages = remember {
        mutableStateListOf(
            YouTubeChatMessage("1", "أحمد (المضيف)", "أهلاً بالجميع في غرفة سينما اليوتيوب! 🎬🍿", "10:20 م", false, Color(0xFF2563EB)),
            YouTubeChatMessage("2", "سارة", "جودة البث عبر Cloudflare ممتازة وسريعة جداً بدون أي تقطيع ⚡", "10:21 م", false, Color(0xFF10B981)),
            YouTubeChatMessage("3", "محمد", "التلاوة مريحة جداً، شكراً لاختيار المقطع 🙏", "10:22 م", false, Color(0xFF8B5CF6)),
            YouTubeChatMessage("4", "علي", "أضفت مقطع وثائقي جيمس ويب لقائمة الانتظار 🚀", "10:23 م", false, Color(0xFFF59E0B)),
            YouTubeChatMessage("5", "فاطمة", "الصوت متزامن 100% مع الجميع ✨", "10:24 م", false, Color(0xFFEC4899))
        )
    }
    var chatInputText by remember { mutableStateOf("") }

    // Walkie-Talkie & Voice Room State
    var isIntercomTalking by remember { mutableStateOf(false) }
    var isMicMuted by remember { mutableStateOf(false) }
    var roomVolumeLevel by remember { mutableStateOf(0.85f) }

    // Room Participants state
    val roomUsers = remember {
        mutableStateListOf(
            YouTubeRoomUser("u1", "أحمد", "مضيف الغرفة", isHost = true, isOnline = true, isSpeaking = false, Color(0xFF2563EB)),
            YouTubeRoomUser("u2", "سارة", "مشرف", isHost = false, isOnline = true, isSpeaking = false, Color(0xFF10B981)),
            YouTubeRoomUser("u3", "محمد", "مشاهد VIP", isHost = false, isOnline = true, isSpeaking = false, Color(0xFF8B5CF6)),
            YouTubeRoomUser("u4", "علي", "مشاهد", isHost = false, isOnline = true, isSpeaking = false, Color(0xFFF59E0B)),
            YouTubeRoomUser("u5", "فاطمة", "مشاهدة", isHost = false, isOnline = true, isSpeaking = false, Color(0xFFEC4899)),
            YouTubeRoomUser("u6", "حسن", "مشاهد", isHost = false, isOnline = true, isSpeaking = false, Color(0xFF06B6D4)),
            YouTubeRoomUser("u7", "زينب", "مشاهدة", isHost = false, isOnline = true, isSpeaking = false, Color(0xFF14B8A6))
        )
    }

    // Audio beep player
    fun playButtonBeep(type: Int = ToneGenerator.TONE_PROP_BEEP) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
            toneGen.startTone(type, 50)
        } catch (_: Exception) {}
    }

    // Perform Search & Open 20+ Results Modal
    fun executeSearch(query: String) {
        playButtonBeep()
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        searchQuery = query
        isDropdownOpen = false
        isSearchModalOpen = true
    }

    // Play a video directly
    fun playSelectedVideo(video: YouTubeVideoItem) {
        playButtonBeep()
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        currentVideo = video
        isPlaying = true
        currentPositionSeconds = 0
        isSynchronizedWithRoom = true
        Toast.makeText(context, "جاري تشغيل: ${video.title.take(35)}... 🎬", Toast.LENGTH_SHORT).show()
    }

    // Add video to room queue
    fun addVideoToQueue(video: YouTubeVideoItem) {
        playButtonBeep()
        if (!queuePlaylist.any { it.id == video.id }) {
            queuePlaylist.add(video)
            Toast.makeText(context, "تمت إضافة الفيديو إلى قائمة تشغيل الغرفة ✨", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "الفيديو موجود بالفعل في قائمة الانتظار", Toast.LENGTH_SHORT).show()
        }
    }

    // Full RTL Root Layout
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppScreenBackground())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ====================================================
                // 1. TOP HEADER: Back + Room Code Chip + Title & Cloudflare Icon
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button (Circle with soft border)
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Center: Room Code Chip (نسخ كود الغرفة للمشاركة)
                    Surface(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("كود الغرفة", "#YT-9024")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود الغرفة (#YT-9024) بنجاح! شاركه لمشاهدة جماعية 🎬🍿", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF5FF),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ كود الغرفة",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "#YT-9024",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }

                    // Title + Cloudflare Streaming Hub Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Small icon button for Cloudflare CDN stream hub / library
                        IconButton(
                            onClick = {
                                isSearchModalOpen = true
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFFEF2F2), CircleShape)
                                .border(1.dp, Color(0xFFFECACA), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "سحابة البث",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Room Name
                        Text(
                            text = "غرفة اليوتيوب",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = TajawalFontFamily,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ====================================================
                // 2. SEARCH BAR & INSTANT AUTOCOMPLETE DROPDOWN
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search Text Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                isDropdownOpen = it.trim().isNotEmpty()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            placeholder = {
                                Text(
                                    text = "ابحث في اليوتيوب وسيرفرات Cloudflare...",
                                    fontSize = 12.sp,
                                    fontFamily = TajawalFontFamily,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "بحث",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        isDropdownOpen = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "مسح",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFDBEAFE),
                                cursorColor = Color(0xFF2563EB)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                fontFamily = TajawalFontFamily,
                                color = Color(0xFF0F172A)
                            )
                        )

                        // Search Action Button
                        Button(
                            onClick = {
                                executeSearch(if (searchQuery.trim().isEmpty()) "الكل" else searchQuery)
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .shadow(2.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626) // YouTube Brand Red
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TravelExplore,
                                    contentDescription = "بحث فوري",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "بحث",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Autocomplete Suggestions Dropdown Box
                    if (isDropdownOpen && filteredSuggestions.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 54.dp)
                                .shadow(8.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                filteredSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                executeSearch(suggestion)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = suggestion,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.NorthWest,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                                }

                                // View All 20+ Results Link
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            executeSearch(searchQuery)
                                        }
                                        .background(Color(0xFFEEF5FF))
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔍 عرض كافة النتائج (+20 مقطع فيديو)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ====================================================
                // 3. LARGE TOP CINEMA DISPLAY CARD (تكبير مربع اليوتيوب - 275.dp)
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(275.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(32.dp))
                        .clickable {
                            isWebPlayerActive = !isWebPlayerActive
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isWebPlayerActive) {
                        // 3A. LIVE YOUTUBE / CLOUDFLARE WEBVIEW EMBEDDED PLAYER
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.allowFileAccess = true
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    webViewClient = WebViewClient()
                                    webChromeClient = WebChromeClient()
                                    // High performance YouTube iframe with Cloudflare fallback
                                    val htmlData = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                            <style>
                                                body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
                                                iframe { width: 100%; height: 100%; border: none; }
                                            </style>
                                        </head>
                                        <body>
                                            <iframe 
                                                src="https://www.youtube-nocookie.com/embed/${currentVideo.id}?autoplay=1&playsinline=1&controls=1&modestbranding=1&rel=0" 
                                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                                allowfullscreen>
                                            </iframe>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "utf-8", null)
                                }
                            },
                            update = { webView ->
                                val htmlData = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                        <style>
                                            body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
                                            iframe { width: 100%; height: 100%; border: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <iframe 
                                            src="https://www.youtube-nocookie.com/embed/${currentVideo.id}?autoplay=1&playsinline=1&controls=1&modestbranding=1&rel=0" 
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                            allowfullscreen>
                                        </iframe>
                                    </body>
                                    </html>
                                """.trimIndent()
                                webView.loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "utf-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Float Button to switch back to HUD controls
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color(0x99000000), CircleShape)
                                .clickable { isWebPlayerActive = false }
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق المشغل الكامل",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        // 3B. CINEMA THEATER POSTER & INTERACTIVE SYNC HUD
                        Box(modifier = Modifier.fillMaxSize()) {
                            // High-res Video Thumbnail Background
                            AsyncImage(
                                model = currentVideo.thumbnailUrl,
                                contentDescription = currentVideo.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.45f)
                            )

                            // Cinema Dark Gradient Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x990F172A),
                                                Color(0x400F172A),
                                                Color(0xF00F172A)
                                            )
                                        )
                                    )
                            )

                            // Top Badges Row (Live Sync + Viewers Count + Cloudflare CDN Tag)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Live Watch-Party Sync Status
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xCC10B981)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                        Text(
                                            text = "مزامنة الغرفة 100%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Viewers Badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xCCDC2626)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "$liveViewerCount يشاهدون الآن",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Center Play / Pause Big Pulse Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(68.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFFDC2626), Color(0xFF991B1B))
                                        ),
                                        CircleShape
                                    )
                                    .border(2.dp, Color(0xFFFECACA), CircleShape)
                                    .clickable {
                                        isPlaying = !isPlaying
                                        playButtonBeep()
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Bottom HUD: Video Title, Channel, Duration Scrubber & Control Icons
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = currentVideo.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${currentVideo.channelTitle} • ${currentVideo.category}",
                                        fontSize = 11.sp,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF94A3B8)
                                    )

                                    // Tap to toggle webview player
                                    Text(
                                        text = "اضغط للمشغل المباشر 🎬",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF38BDF8)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Progress Bar
                                LinearProgressIndicator(
                                    progress = { 0.35f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFFDC2626),
                                    trackColor = Color(0xFF334155),
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick Controls Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Speed Chip
                                        Surface(
                                            onClick = {
                                                playbackSpeed = when (playbackSpeed) {
                                                    1.0f -> 1.25f
                                                    1.25f -> 1.5f
                                                    1.5f -> 2.0f
                                                    else -> 1.0f
                                                }
                                                Toast.makeText(context, "سرعة التشغيل: ${playbackSpeed}x", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B)
                                        ) {
                                            Text(
                                                text = "${playbackSpeed}x",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Mute Toggle
                                        IconButton(
                                            onClick = { isMuted = !isMuted },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                                contentDescription = "صوت",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Text(
                                            text = "08:15 / ${currentVideo.duration}",
                                            fontSize = 10.sp,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }

                                    // Fullscreen Web player button
                                    IconButton(
                                        onClick = { isWebPlayerActive = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "ملء الشاشة",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ====================================================
                // 4. SUB-TABS NAVIGATION DOCK (مطابق تماماً لغرفة الألعاب)
                // ====================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        YouTubeDockIconButton(
                            icon = Icons.Default.SmartDisplay,
                            isActive = activeSubTab == YouTubeRoomSubTab.PLAYER,
                            onClick = { activeSubTab = YouTubeRoomSubTab.PLAYER }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            isActive = activeSubTab == YouTubeRoomSubTab.CHAT,
                            onClick = { activeSubTab = YouTubeRoomSubTab.CHAT }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Default.Sensors,
                            isActive = activeSubTab == YouTubeRoomSubTab.LIVE_SYNC,
                            onClick = { activeSubTab = YouTubeRoomSubTab.LIVE_SYNC }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Outlined.Mic,
                            isActive = activeSubTab == YouTubeRoomSubTab.INTERCOM,
                            onClick = { activeSubTab = YouTubeRoomSubTab.INTERCOM }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Outlined.PeopleOutline,
                            isActive = activeSubTab == YouTubeRoomSubTab.USERS,
                            onClick = { activeSubTab = YouTubeRoomSubTab.USERS }
                        )
                        YouTubeDockIconButton(
                            icon = Icons.Outlined.Settings,
                            isActive = activeSubTab == YouTubeRoomSubTab.SETTINGS,
                            onClick = { activeSubTab = YouTubeRoomSubTab.SETTINGS }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ====================================================
                // 5. SUB-TAB VIEW CONTENT
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (activeSubTab) {
                        // ----------------------------------------------------
                        // 5A. PLAYER & QUEUE PLAYLIST VIEW
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.PLAYER -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Queue Section Header
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "قائمة الانتظار في الغرفة (${queuePlaylist.size})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        TextButton(onClick = { isSearchModalOpen = true }) {
                                            Text(
                                                text = "+ إضافة مقطع للغرفة",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                    }
                                }

                                // Queue Items
                                items(queuePlaylist) { item ->
                                    Surface(
                                        onClick = { playSelectedVideo(item) },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (item.id == currentVideo.id) Color(0xFFEEF5FF) else Color.White,
                                        border = BorderStroke(
                                            1.dp,
                                            if (item.id == currentVideo.id) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Thumbnail Box
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 80.dp, height = 50.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            ) {
                                                AsyncImage(
                                                    model = item.thumbnailUrl,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(2.dp)
                                                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = item.duration,
                                                        fontSize = 8.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${item.channelTitle} • ${item.viewCount}",
                                                    fontSize = 10.sp,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            if (item.id == currentVideo.id) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = "يعمل الآن",
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Recommended Related Videos Section
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "مقاطع مقترحة سحابياً عبر Cloudflare",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }

                                items(videoCatalog.take(5)) { item ->
                                    if (item.id != currentVideo.id) {
                                        Surface(
                                            onClick = { playSelectedVideo(item) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 70.dp, height = 44.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = item.thumbnailUrl,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF1E293B),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = item.channelTitle,
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { addVideoToQueue(item) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlaylistAdd,
                                                        contentDescription = "إضافة للقائمة",
                                                        tint = Color(0xFF2563EB),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 5B. CHAT SUB-VIEW (نفس شات غرفة الألعاب مع المعالجة)
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.CHAT -> {
                            val chatListState = rememberLazyListState()

                            LaunchedEffect(chatMessages.size) {
                                if (chatMessages.isNotEmpty()) {
                                    chatListState.animateScrollToItem(chatMessages.size - 1)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .border(1.dp, Color(0xFFE2EAFD), RoundedCornerShape(24.dp))
                                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
                                    .imePadding()
                            ) {
                                // Live Messages
                                LazyColumn(
                                    state = chatListState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(chatMessages) { msg ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (msg.isMe) Arrangement.Start else Arrangement.End,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            if (!msg.isMe) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(msg.avatarColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = msg.sender.take(1),
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .background(
                                                        if (msg.isMe) Color(0xFF2563EB) else Color(0xFFEEF5FF),
                                                        RoundedCornerShape(16.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = msg.sender,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (msg.isMe) Color.White else Color(0xFF1E3A8A)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = msg.text,
                                                    fontSize = 13.sp,
                                                    color = if (msg.isMe) Color.White else Color(0xFF334155)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = msg.time,
                                                    fontSize = 9.sp,
                                                    color = if (msg.isMe) Color(0xCCFFFFFF) else Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick Reaction Emojis
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    listOf("🍿", "❤️", "🔥", "😂", "👏", "✨").forEach { emoji ->
                                        Text(
                                            text = emoji,
                                            fontSize = 18.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    chatMessages.add(
                                                        YouTubeChatMessage(
                                                            id = System.currentTimeMillis().toString(),
                                                            sender = "أنا",
                                                            text = emoji,
                                                            time = "الآن",
                                                            isMe = true
                                                        )
                                                    )
                                                    playButtonBeep()
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Input Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F6FB), RoundedCornerShape(20.dp))
                                        .border(1.dp, Color(0xFFE2EAFD), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = chatInputText,
                                        onValueChange = { chatInputText = it },
                                        placeholder = {
                                            Text(
                                                "اكتب تعليقك المباشر في الغرفة...",
                                                fontSize = 12.sp,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFF94A3B8)
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 13.sp,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF0F172A)
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            if (chatInputText.trim().isNotEmpty()) {
                                                chatMessages.add(
                                                    YouTubeChatMessage(
                                                        id = System.currentTimeMillis().toString(),
                                                        sender = "أنا",
                                                        text = chatInputText.trim(),
                                                        time = "الآن",
                                                        isMe = true
                                                    )
                                                )
                                                chatInputText = ""
                                                playButtonBeep()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF2563EB), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "إرسال",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 5C. LIVE & CLOUDFLARE SYNC MONITOR
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.LIVE_SYNC -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Cloudflare CDN Network Status Card
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudSync,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Text(
                                                    text = "سيرفر Cloudflare Edge Stream",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color.White
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0x3310B981)
                                            ) {
                                                Text(
                                                    text = "متصل 12ms",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF34D399),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            YouTubeStatMiniBlock(label = "البروتوكول", value = "HLS / WebRTC")
                                            YouTubeStatMiniBlock(label = "معدل البت", value = "12,400 kbps")
                                            YouTubeStatMiniBlock(label = "الفقد (Loss)", value = "0.0%")
                                            YouTubeStatMiniBlock(label = "المزامنة", value = "Active ⚡")
                                        }
                                    }
                                }

                                // Sync Master Controls
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "إدارة مزامنة البث مع جميع الحاضرين",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                playButtonBeep()
                                                Toast.makeText(context, "تمت إعادة معايرة ومزامنة الفيديو لجميع الحاضرين بالمللي ثانية ⏱️", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "إجبار المزامنة الفورية لجميع الأعضاء",
                                                    fontFamily = TajawalFontFamily,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 5D. WALKIE-TALKIE / INTERCOM VOICE
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.INTERCOM -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Status Card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isIntercomTalking) "🎙️ أنت تتحدث الآن في الغرفة..." else "المايك جاهز للتواصل الصوتي المباشر",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = if (isIntercomTalking) Color(0xFF10B981) else Color(0xFF1E3A8A)
                                            )
                                            Text(
                                                text = "اضغط مع الاستمرار على الزر للتحدث مع الجميع",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Switch(
                                            checked = !isMicMuted,
                                            onCheckedChange = { isMicMuted = !it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF10B981)
                                            )
                                        )
                                    }
                                }

                                // Big Push-To-Talk Intercom Button
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .background(
                                            Brush.radialGradient(
                                                if (isIntercomTalking)
                                                    listOf(Color(0xFF10B981), Color(0xFF047857))
                                                else
                                                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                            ),
                                            CircleShape
                                        )
                                        .border(4.dp, Color.White, CircleShape)
                                        .shadow(8.dp, CircleShape)
                                        .clickable {
                                            isIntercomTalking = !isIntercomTalking
                                            playButtonBeep()
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isIntercomTalking) Icons.Default.Mic else Icons.Default.MicNone,
                                            contentDescription = "تحدث",
                                            tint = Color.White,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isIntercomTalking) "تحدث الآن" else "اضغط للتحدث",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Bottom Volume Balance
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "مستوى توازن صوت الفيديو مع المحادثة",
                                                fontSize = 11.sp,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFF1E3A8A)
                                            )
                                            Text(
                                                text = "${(roomVolumeLevel * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                        Slider(
                                            value = roomVolumeLevel,
                                            onValueChange = { roomVolumeLevel = it },
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF2563EB),
                                                activeTrackColor = Color(0xFF2563EB)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 5E. USERS / PARTICIPANTS SUB-VIEW
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.USERS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "المتواجدون في الغرفة (${roomUsers.size})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "تم إنشاء رابط دعوة مباشر للغرفة 🔗", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("دعوة صديق", fontSize = 11.sp, fontFamily = TajawalFontFamily)
                                        }
                                    }
                                }

                                items(roomUsers) { user ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .background(user.avatarBg, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = user.name.take(1),
                                                        color = Color.White,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = user.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = user.role,
                                                        fontSize = 10.sp,
                                                        color = if (user.isHost) Color(0xFF2563EB) else Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "14ms",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // 5F. SETTINGS SUB-VIEW
                        // ----------------------------------------------------
                        YouTubeRoomSubTab.SETTINGS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                YouTubeSettingsSwitchItem(
                                    title = "تسريع السحابة عبر Cloudflare CDN",
                                    subtitle = "تقليل استهلاك البيانات وتحسين سرعة استجابة الفيديو",
                                    isChecked = true
                                )
                                YouTubeSettingsSwitchItem(
                                    title = "المزامنة التلقائية التامة",
                                    subtitle = "مطابقة توقيت التشغيل مع مضيف الغرفة بدقة 0.1 ثانية",
                                    isChecked = isSynchronizedWithRoom
                                )
                                YouTubeSettingsSwitchItem(
                                    title = "الوضع السينمائي المحيطي (Cinema Glow)",
                                    subtitle = "توهج إضاءة ذكية حول الفيديو حسب ألوان المشهد",
                                    isChecked = true
                                )
                                YouTubeSettingsSwitchItem(
                                    title = "تشغيل الصوت في الخلفية",
                                    subtitle = "استمرار الاستماع للتلاوات والمقاطع عند قفل الشاشة",
                                    isChecked = true
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====================================================
        // 6. RICH FULL POPUP MODAL DIALOG (>20 RESULTS + SEARCH MORE)
        // ====================================================
        if (isSearchModalOpen) {
            val queryFilteredResults = remember(searchQuery, selectedFilterCategory, videoCatalog.size) {
                videoCatalog.filter { item ->
                    val matchesCategory = (selectedFilterCategory == "الكل") || (item.category == selectedFilterCategory)
                    val matchesQuery = (searchQuery.trim().isEmpty() || searchQuery == "الكل") ||
                            item.title.contains(searchQuery.trim(), ignoreCase = true) ||
                            item.channelTitle.contains(searchQuery.trim(), ignoreCase = true) ||
                            item.category.contains(searchQuery.trim(), ignoreCase = true)
                    matchesCategory && matchesQuery
                }
            }

            Dialog(
                onDismissRequest = { isSearchModalOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        color = getAppScreenBackground()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Top Bar of Search Dialog
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { isSearchModalOpen = false },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White, CircleShape)
                                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = Color(0xFF1E3A8A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "نتائج البحث والمكتبة المرئية",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                // Total Results Counter Badge
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEEF5FF),
                                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                                ) {
                                    Text(
                                        text = "${queryFilteredResults.size} نتيجة متاحة",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = TajawalFontFamily,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Category Filter Chips Row
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val categories = listOf("الكل", "القرآن الكريم", "بث مباشر", "وثائقيات", "أدعية وزيارات", "بودكاست", "تقنية", "لطميات ومراثي", "ألعاب وتحديات")
                                items(categories) { cat ->
                                    val isSelected = selectedFilterCategory == cat
                                    Surface(
                                        onClick = {
                                            selectedFilterCategory = cat
                                            playButtonBeep()
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) Color(0xFF2563EB) else Color.White,
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontFamily = TajawalFontFamily,
                                            color = if (isSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 20+ Results List
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(queryFilteredResults) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
                                        shadowElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Large High-Res Thumbnail with badge
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 115.dp, height = 75.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = item.thumbnailUrl,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    // Duration / Live Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(4.dp)
                                                            .background(
                                                                if (item.isLive) Color(0xCCDC2626) else Color(0xCC000000),
                                                                RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = item.duration,
                                                            fontSize = 9.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                // Metadata
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = item.channelTitle,
                                                        fontSize = 10.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF2563EB)
                                                    )
                                                    Text(
                                                        text = "${item.viewCount} • ${item.publishedTime}",
                                                        fontSize = 9.sp,
                                                        fontFamily = TajawalFontFamily,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Action Buttons Row (تشغيل في الغرفة + إضافة للدور)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        playSelectedVideo(item)
                                                        isSearchModalOpen = false
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFDC2626)
                                                    ),
                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "تشغيل في الغرفة",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = TajawalFontFamily,
                                                            color = Color.White
                                                        )
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        addVideoToQueue(item)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF2563EB)),
                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlaylistAdd,
                                                            contentDescription = null,
                                                            tint = Color(0xFF2563EB),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "إضافة للدور",
                                                            fontSize = 11.sp,
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

                                // Load More Results (بحث أكثر وتوسيع النتائج السحابية)
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        onClick = {
                                            if (!isExpandedSearchLoading) {
                                                isExpandedSearchLoading = true
                                                coroutineScope.launch {
                                                    delay(600)
                                                    EXTRA_EXPANDED_YOUTUBE_CATALOG.forEach { extraItem ->
                                                        if (!videoCatalog.any { it.id == extraItem.id }) {
                                                            videoCatalog.add(extraItem)
                                                        }
                                                    }
                                                    isExpandedSearchLoading = false
                                                    extraResultsCount += EXTRA_EXPANDED_YOUTUBE_CATALOG.size
                                                    Toast.makeText(context, "تم جلب ${EXTRA_EXPANDED_YOUTUBE_CATALOG.size} نتائج إضافية من سيرفرات السحابة 🚀", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFEEF5FF),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isExpandedSearchLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color(0xFF2563EB)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.TravelExplore,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = if (isExpandedSearchLoading) "جاري البحث السحابي..." else "✨ بحث أكثر وتوسيع النتائج من Cloudflare",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = TajawalFontFamily,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 7. HELPER COMPOSABLE SUB-COMPONENTS
// ----------------------------------------------------
@Composable
private fun YouTubeDockIconButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                if (isActive) Color(0xFF2563EB) else Color.Transparent,
                CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.White else Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun YouTubeStatMiniBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color(0xFF94A3B8),
            fontFamily = TajawalFontFamily
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun YouTubeSettingsSwitchItem(
    title: String,
    subtitle: String,
    isChecked: Boolean
) {
    var state by remember { mutableStateOf(isChecked) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2EAFD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color(0xFF64748B)
                )
            }
            Switch(
                checked = state,
                onCheckedChange = { state = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2563EB)
                )
            )
        }
    }
}
