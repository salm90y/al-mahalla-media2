package com.ps1.netplay.ui.compose

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.ps1.netplay.AppSettingsManager
import com.ps1.netplay.R

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal_light, FontWeight.Light),
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
    Font(R.font.tajawal_black, FontWeight.Black)
)

val PrimaryBlue = Color(0xFF2563EB)
val DarkPrimaryBlue = Color(0xFF3B82F6)
val LightBg = Color(0xFFFFFFFF) // Default clean white background
val LightSurface = Color(0xFFFFFFFF)
val LightPillBg = Color(0xFFF1F5F9)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)
val LightBorder = Color(0xFFE2E8F0)

val PureWhiteBg = Color(0xFFFFFFFF)
val StoriesBg = Color(0xFFF0F6FF)
val LoginBg = Color(0xFFF0F6FF)
val SlateLightBg = Color(0xFFF8FAFC)
val DarkSapphireBg = Color(0xFF0F172A)

// Modern, sleek dark theme (داكن عصري ومريح للعين، ليس أسود قاتم)
val DarkBg = Color(0xFF0F172A) // Rich slate navy background (not pitch black)
val DarkSurface = Color(0xFF1E293B) // Slate-800 for cards, surfaces and toolbars
val DarkSurfaceVariant = Color(0xFF28334E) // Elevated card background
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkBorder = Color(0xFF334155) // Slate-700 border

val PrimaryAccent = Color(0xFF2563EB)
val SecondaryAccent = Color(0xFF3B82F6)
val OnlineIndicator = Color(0xFF10B981)
val UnreadBadge = Color(0xFF2563EB)
val DarkBackground = Color(0xFF0F172A)
val LightBackground = Color(0xFFFFFFFF)

val HeaderGradientStart = Color(0xFF2563EB)
val HeaderGradientEnd = Color(0xFF1D4ED8)
val PrimaryHeaderGradient = Brush.verticalGradient(
    colors = listOf(HeaderGradientStart, HeaderGradientEnd)
)

@Composable
fun isAppInDarkTheme(): Boolean {
    val currentThemeSetting = AppSettingsManager.themeModeState.value
    return when (currentThemeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
}

@Composable
fun getAppScreenBackground(): Color {
    val currentThemeSetting = AppSettingsManager.themeModeState.value
    if (currentThemeSetting == "dark" || (currentThemeSetting == "system" && isSystemInDarkTheme())) {
        return DarkBg
    }
    return when (AppSettingsManager.bgThemeState.value) {
        "login" -> LoginBg
        "stories" -> StoriesBg
        "slate_light" -> SlateLightBg
        else -> PureWhiteBg // Default is Pure White
    }
}

@Composable
fun NetPlayTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        AppSettingsManager.initIfNeeded(view.context)
    }

    val currentThemeSetting = AppSettingsManager.themeModeState.value
    val isDark = when (currentThemeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme() || darkTheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = PrimaryBlue,
            background = DarkBg,
            surface = DarkSurface,
            onPrimary = Color.White,
            onBackground = DarkTextPrimary,
            onSurface = DarkTextPrimary
        )
    } else {
        lightColorScheme(
            primary = PrimaryBlue,
            background = getAppScreenBackground(),
            surface = LightSurface,
            onPrimary = Color.White,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
