package com.ps1.netplay.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standard Compact Al-Mahalla Circular Calligraphic Badge (for headers and cards)
 */
@Composable
fun AlMahallaLogo(
    modifier: Modifier = Modifier,
    height: Dp = 42.dp
) {
    val clampedHeight = height.coerceIn(32.dp, 48.dp)

    Box(
        modifier = modifier.size(clampedHeight),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // 1. Outer Neon Gradient Ring
            val ringBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF00E5FF),
                    Color(0xFF0284C7),
                    Color(0xFFA855F7),
                    Color(0xFF00E5FF)
                ),
                center = center
            )
            drawCircle(
                brush = ringBrush,
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Inner Deep Midnight Core
            val coreBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0F2248),
                    Color(0xFF08122C),
                    Color(0xFF030714)
                ),
                center = center,
                radius = radius - 2.dp.toPx()
            )
            drawCircle(
                brush = coreBrush,
                radius = radius - 2.5.dp.toPx(),
                center = center
            )

            // 3. Top Cyan Crescent Arc
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.85f), Color.Transparent)
                ),
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 3.dp.toPx(), center.y - radius + 3.dp.toPx()),
                size = androidx.compose.ui.geometry.Size((radius - 3.dp.toPx()) * 2f, (radius - 3.dp.toPx()) * 2f),
                style = Stroke(width = 1.4.dp.toPx())
            )

            // 4. Subtle Calligraphic Baseline Glow (Leveling the text optically)
            val baseLineY = center.y + (radius * 0.38f)
            val lineWidth = radius * 0.72f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00E5FF).copy(alpha = 0.75f),
                        Color(0xFFA855F7).copy(alpha = 0.75f),
                        Color.Transparent
                    )
                ),
                start = Offset(center.x - lineWidth / 2, baseLineY),
                end = Offset(center.x + lineWidth / 2, baseLineY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Arabic Calligraphic Text - Perfectly Centered & Leveled
        val emblemTextSize = (clampedHeight.value * 0.32f).sp
        Text(
            text = "المَحَلَّة",
            style = TextStyle(
                color = Color.White,
                fontSize = emblemTextSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = TajawalFontFamily,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                ),
                shadow = Shadow(
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    offset = Offset(0f, 0f),
                    blurRadius = 6f
                )
            ),
            modifier = Modifier.offset(y = (-1.2).dp)
        )
    }
}

/**
 * Hero Al-Mahalla Calligraphic Emblem Logo
 * Features:
 * - Multi-layered glowing neon and cosmic rings
 * - Arabic calligraphic styling with balanced baseline (preventing any sagging letters)
 * - Decorative calligraphic diacritic emblem & crescent flourishes
 */
@Composable
fun AlMahallaHeroLogo(
    modifier: Modifier = Modifier,
    size: Dp = 104.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // 1. Outermost Ambient Glow Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.22f),
                        Color(0xFFA855F7).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // 2. Outer Neon Gradient Ring (Thick & Glowing)
            val ringBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF00E5FF),
                    Color(0xFF0284C7),
                    Color(0xFFA855F7),
                    Color(0xFF00E5FF)
                ),
                center = center
            )
            drawCircle(
                brush = ringBrush,
                radius = radius - 3.dp.toPx(),
                center = center,
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Secondary Inner Ring
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                radius = radius - 7.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Inner Midnight Cosmic Badge Core
            val coreBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF132B5C),
                    Color(0xFF0B1938),
                    Color(0xFF040A18)
                ),
                center = center,
                radius = radius - 8.dp.toPx()
            )
            drawCircle(
                brush = coreBrush,
                radius = radius - 8.dp.toPx(),
                center = center
            )

            // 5. Top Cyan Arc & Bottom Violet Arc
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.95f), Color.Transparent)
                ),
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 11.dp.toPx(), center.y - radius + 11.dp.toPx()),
                size = androidx.compose.ui.geometry.Size((radius - 11.dp.toPx()) * 2f, (radius - 11.dp.toPx()) * 2f),
                style = Stroke(width = 1.8.dp.toPx())
            )
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFFA855F7).copy(alpha = 0.85f))
                ),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 11.dp.toPx(), center.y - radius + 11.dp.toPx()),
                size = androidx.compose.ui.geometry.Size((radius - 11.dp.toPx()) * 2f, (radius - 11.dp.toPx()) * 2f),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // 6. Calligraphic Foundation Line (Ensures perfectly level baseline for the Arabic text)
            val baseLineY = center.y + (radius * 0.32f)
            val lineWidth = radius * 0.85f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00E5FF).copy(alpha = 0.85f),
                        Color(0xFFA855F7).copy(alpha = 0.85f),
                        Color.Transparent
                    )
                ),
                start = Offset(center.x - lineWidth / 2, baseLineY),
                end = Offset(center.x + lineWidth / 2, baseLineY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 7. Decorative Calligraphic Diacritic Rhombus / Diamond Crest Accent
            val diamondCenter = Offset(center.x, center.y - (radius * 0.44f))
            val dSize = 3.5.dp.toPx()
            val diamondPath = Path().apply {
                moveTo(diamondCenter.x, diamondCenter.y - dSize)
                lineTo(diamondCenter.x + dSize, diamondCenter.y)
                lineTo(diamondCenter.x, diamondCenter.y + dSize)
                lineTo(diamondCenter.x - dSize, diamondCenter.y)
                close()
            }
            drawPath(
                path = diamondPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFFA855F7)),
                    center = diamondCenter,
                    radius = dSize * 1.5f
                ),
                style = Fill
            )
        }

        // Arabic Calligraphic Logo Typography - "المَحَلَّة"
        // Styled like a true emblem: bold, elegant, strictly level baseline, no sagging
        Text(
            text = "المَحَلَّة",
            style = TextStyle(
                color = Color.White,
                fontSize = (size.value * 0.285f).sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = TajawalFontFamily,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                ),
                shadow = Shadow(
                    color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                    offset = Offset(0f, 0f),
                    blurRadius = 10f
                )
            ),
            modifier = Modifier.offset(y = (-2.5).dp)
        )
    }
}
