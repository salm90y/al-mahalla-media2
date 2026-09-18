package com.ps1.netplay.ui
import android.widget.*
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.ps1.netplay.input.PS1InputFrame

/** * High-End PlayStation 1 Digital / DualShock On-Screen Touch Controller Overlay. * Designed with modern translucent glass aesthetic, vector-rendered PS1 symbols, * ergonomic shoulder triggers (L1, L2, R1, R2), tactile D-Pad, and instant haptic feedback. */class VirtualTouchOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    var dpadScale: Float = 1.0f
    var actionScale: Float = 1.0f
    var controlsAlpha: Float = 0.75f
    var hapticEnabled: Boolean = true
    var isControlsVisible: Boolean = false // Hidden by default on Home
    var onInputMaskChanged: ((Int) -> Unit)? = null
    private var currentTouchMask = 0
    // Paints
    private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(175, 12, 16, 28) // OLED Glass Obsidian
}
private val buttonActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(235, 139, 92, 246) // Electric Neon Violet (#8B5CF6)
}
private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.8f
        color = Color.argb(190, 139, 92, 246) // Neon Violet accent border
}
private val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5.0f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    // PlayStation Luxury Gold Esports Colors (#D4AF37 / #FFD700)
    private val colorNeonGold = Color.rgb(255, 215, 0)
     // #FFD700 Bright Gold
    private val colorGoldTrigger = Color.rgb(212, 175, 55) // #D4AF37 Primary Gold
private val colorGoldGlow = Color.rgb(255, 215, 0)
     // #FFD700 Gold Inner Glow
    private val colorTranslucentDark = Color.argb(225, 26, 26, 46) // #1A1A2E translucent dark glass
private val colorDarkCross = Color.argb(235, 14, 18, 30) // Deep obsidian glass
    private val triangleColor = Color.rgb(16, 185, 129) // Neon Emerald Green (#10B981)
private val circleColor = Color.rgb(239, 68, 68)
    // Neon Red (#EF4444)
    private val crossColor = Color.rgb(59, 130, 246)
    // Neon Blue (#3B82F6)
    private val squareColor = Color.rgb(236, 72, 153)   // Radiant Magenta Pink (#EC4899)
private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    // Geometry Rectangles & Paths
    private val rectDpadCrossH = RectF()
private val rectDpadCrossV = RectF()
private val rectUp = RectF()
private val rectDown = RectF()
private val rectLeft = RectF()
private val rectRight = RectF()
private val rectTriangle = RectF()
private val rectCircle = RectF()
private val rectCross = RectF()
private val rectSquare = RectF()
private val rectL1 = RectF()
private val rectL2 = RectF()
private val rectR1 = RectF()
private val rectR2 = RectF()
private val rectSelect = RectF()
private val rectStart = RectF()
    // Reusable Path
    private val pathBuffer = Path()
override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateBounds(w, h)
    }
private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
private fun recalculateBounds(width: Int, height: Int) {
        val isLandscape = width > height
        // --- 1. Shoulder Triggers (72x36dp each) ---
// L2, L1 on left and R1, R2 on right as small frosted glass pill buttons, 72x36dp
    val triggerW = dpToPx(72f)
val triggerH = dpToPx(36f)
val triggerTop = if (isLandscape) dpToPx(20f) else height - dpToPx(240f)
        rectL2.set(dpToPx(12f), triggerTop, dpToPx(12f) + triggerW, triggerTop + triggerH)
        rectL1.set(dpToPx(16f) + triggerW, triggerTop, dpToPx(16f) + triggerW * 2, triggerTop + triggerH)
        rectR1.set(width - dpToPx(16f) - triggerW * 2, triggerTop, width - dpToPx(16f) - triggerW, triggerTop + triggerH)
        rectR2.set(width - dpToPx(12f) - triggerW, triggerTop, width - dpToPx(12f), triggerTop + triggerH)
        // --- 2. Left side: Unified D-pad as ONE solid cross shape ---
    val dpadCenterX = if (isLandscape) dpToPx(110f) else dpToPx(95f)
val dpadCenterY = height - (if (isLandscape) dpToPx(100f) else dpToPx(115f))
val crossArmW = dpToPx(44f) * dpadScale
        val crossSpan = dpToPx(124f) * dpadScale
        rectDpadCrossH.set(dpadCenterX - crossSpan / 2, dpadCenterY - crossArmW / 2, dpadCenterX + crossSpan / 2, dpadCenterY + crossArmW / 2)
        rectDpadCrossV.set(dpadCenterX - crossArmW / 2, dpadCenterY - crossSpan / 2, dpadCenterX + crossArmW / 2, dpadCenterY + crossSpan / 2)
val hitArmW = crossArmW * 1.1f
        rectUp.set(dpadCenterX - hitArmW / 2, dpadCenterY - crossSpan / 2, dpadCenterX + hitArmW / 2, dpadCenterY - crossArmW / 6)
        rectDown.set(dpadCenterX - hitArmW / 2, dpadCenterY + crossArmW / 6, dpadCenterX + hitArmW / 2, dpadCenterY + crossSpan / 2)
        rectLeft.set(dpadCenterX - crossSpan / 2, dpadCenterY - hitArmW / 2, dpadCenterX - crossArmW / 6, dpadCenterY + hitArmW / 2)
        rectRight.set(dpadCenterX + crossArmW / 6, dpadCenterY - hitArmW / 2, dpadCenterX + crossSpan / 2, dpadCenterY + hitArmW / 2)
        // --- 3. Right side: 4 Action buttons as premium frosted glass circles (64dp) ---
    val actionRadius = dpToPx(32f) * actionScale // 64dp diameter
val actionSpacing = dpToPx(48f) * actionScale
        val actionCenterX = width - (if (isLandscape) dpToPx(110f) else dpToPx(95f))
val actionCenterY = height - (if (isLandscape) dpToPx(100f) else dpToPx(115f))
        rectTriangle.set(actionCenterX - actionRadius, actionCenterY - actionSpacing - actionRadius, actionCenterX + actionRadius, actionCenterY - actionSpacing + actionRadius)
        rectCross.set(actionCenterX - actionRadius, actionCenterY + actionSpacing - actionRadius, actionCenterX + actionRadius, actionCenterY + actionSpacing + actionRadius)
        rectSquare.set(actionCenterX - actionSpacing - actionRadius, actionCenterY - actionRadius, actionCenterX - actionSpacing + actionRadius, actionCenterY + actionRadius)
        rectCircle.set(actionCenterX + actionSpacing - actionRadius, actionCenterY - actionRadius, actionCenterX + actionSpacing + actionRadius, actionCenterY + actionRadius)
        // --- 4. Bottom: Two small frosted pills SELECT and START (80x32dp) ---
    val midX = width / 2f
        val pillW = dpToPx(80f)
val pillH = dpToPx(32f)
val bottomY = height - dpToPx(24f)
        rectSelect.set(midX - pillW - dpToPx(10f), bottomY - pillH / 2, midX - dpToPx(10f), bottomY + pillH / 2)
        rectStart.set(midX + dpToPx(10f), bottomY - pillH / 2, midX + pillW + dpToPx(10f), bottomY + pillH / 2)
    }
private fun isPointInsideControls(px: Float, py: Float): Boolean {
        if (!isControlsVisible) return false
        val pad = dpToPx(8f)
fun check(r: RectF) = px >= r.left - pad && px <= r.right + pad && py >= r.top - pad && py <= r.bottom + pad
        return check(rectUp) || check(rectDown) || check(rectLeft) || check(rectRight) ||
               check(rectTriangle) || check(rectCircle) || check(rectCross) || check(rectSquare) ||
               check(rectL1) || check(rectL2) || check(rectR1) || check(rectR2) ||
               check(rectSelect) || check(rectStart)
    }
override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isControlsVisible) return
        val alphaInt = (controlsAlpha * 255).toInt().coerceIn(20, 255)
        buttonBgPaint.alpha = (alphaInt * 0.70f).toInt()
        buttonActivePaint.alpha = (alphaInt * 0.90f).toInt()
        borderPaint.alpha = alphaInt
        symbolPaint.alpha = alphaInt
        textPaint.alpha = alphaInt
        // 1. Unified Solid Cross D-Pad
drawUnifiedDpad(canvas)
        // 2. PlayStation Action Buttons (64dp circles with glowing neon outlines)
drawActionCircle(canvas, rectTriangle, triangleColor, (currentTouchMask and PS1InputFrame.BTN_TRIANGLE) != 0) { cx, cy ->
            val s = dpToPx(10f)
            pathBuffer.reset()
            pathBuffer.moveTo(cx, cy - s)
            pathBuffer.lineTo(cx + s * 1.05f, cy + s * 0.85f)
            pathBuffer.lineTo(cx - s * 1.05f, cy + s * 0.85f)
            pathBuffer.close()
            symbolPaint.color = triangleColor
            canvas.drawPath(pathBuffer, symbolPaint)
        }
        drawActionCircle(canvas, rectSquare, squareColor, (currentTouchMask and PS1InputFrame.BTN_SQUARE) != 0) { cx, cy ->
            val s = dpToPx(8.5f)
            symbolPaint.color = squareColor
            canvas.drawRect(cx - s, cy - s, cx + s, cy + s, symbolPaint)
        }
        drawActionCircle(canvas, rectCircle, circleColor, (currentTouchMask and PS1InputFrame.BTN_CIRCLE) != 0) { cx, cy ->
            val r = dpToPx(9.5f)
            symbolPaint.color = circleColor
            canvas.drawCircle(cx, cy, r, symbolPaint)
        }
        drawActionCircle(canvas, rectCross, crossColor, (currentTouchMask and PS1InputFrame.BTN_CROSS) != 0) { cx, cy ->
            val s = dpToPx(8.5f)
            symbolPaint.color = crossColor
            canvas.drawLine(cx - s, cy - s, cx + s, cy + s, symbolPaint)
            canvas.drawLine(cx + s, cy - s, cx - s, cy + s, symbolPaint)
        }
        // 3. Frosted Glass Shoulder Triggers (72x36dp)
drawTriggerPill(canvas, rectL2, "L2", (currentTouchMask and PS1InputFrame.BTN_L2) != 0)
        drawTriggerPill(canvas, rectL1, "L1", (currentTouchMask and PS1InputFrame.BTN_L1) != 0)
        drawTriggerPill(canvas, rectR1, "R1", (currentTouchMask and PS1InputFrame.BTN_R1) != 0)
        drawTriggerPill(canvas, rectR2, "R2", (currentTouchMask and PS1InputFrame.BTN_R2) != 0)
        // 4. Frosted Glass System Pills SELECT & START (80x32dp)
drawSystemPill(canvas, rectSelect, "SELECT", (currentTouchMask and PS1InputFrame.BTN_SELECT) != 0)
        drawSystemPill(canvas, rectStart, "START", (currentTouchMask and PS1InputFrame.BTN_START) != 0)
    }
private fun drawUnifiedDpad(canvas: Canvas) {
        val cr = dpToPx(14f)
val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = colorDarkCross
        }
val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(1.5f)
            color = Color.argb(190, 255, 215, 0) // Gold inner glow border #FFD700
}
        // Draw solid cross (horizontal & vertical bars)
canvas.drawRoundRect(rectDpadCrossH, cr, cr, fillPaint)
        canvas.drawRoundRect(rectDpadCrossV, cr, cr, fillPaint)
        canvas.drawRoundRect(rectDpadCrossH, cr, cr, strokePaint)
        canvas.drawRoundRect(rectDpadCrossV, cr, cr, strokePaint)
        // Draw directional active highlights if pressed
    val activeHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(140, 255, 215, 0)
        }
        if ((currentTouchMask and PS1InputFrame.BTN_UP) != 0) canvas.drawRoundRect(rectUp, cr, cr, activeHighlightPaint)
        if ((currentTouchMask and PS1InputFrame.BTN_DOWN) != 0) canvas.drawRoundRect(rectDown, cr, cr, activeHighlightPaint)
        if ((currentTouchMask and PS1InputFrame.BTN_LEFT) != 0) canvas.drawRoundRect(rectLeft, cr, cr, activeHighlightPaint)
        if ((currentTouchMask and PS1InputFrame.BTN_RIGHT) != 0) canvas.drawRoundRect(rectRight, cr, cr, activeHighlightPaint)
        // Draw 4 directional triangles inside
drawDpadTriangle(canvas, rectUp.centerX(), rectUp.centerY(), PS1InputFrame.BTN_UP)
        drawDpadTriangle(canvas, rectDown.centerX(), rectDown.centerY(), PS1InputFrame.BTN_DOWN)
        drawDpadTriangle(canvas, rectLeft.centerX(), rectLeft.centerY(), PS1InputFrame.BTN_LEFT)
        drawDpadTriangle(canvas, rectRight.centerX(), rectRight.centerY(), PS1InputFrame.BTN_RIGHT)
    }
private fun drawDpadTriangle(canvas: Canvas, cx: Float, cy: Float, btnFlag: Int) {
        val isPressed = (currentTouchMask and btnFlag) != 0
        val s = dpToPx(7f)
        pathBuffer.reset()
        when (btnFlag) {
            PS1InputFrame.BTN_UP -> {
                pathBuffer.moveTo(cx, cy - s)
                pathBuffer.lineTo(cx - s, cy + s * 0.8f)
                pathBuffer.lineTo(cx + s, cy + s * 0.8f)
            }
            PS1InputFrame.BTN_DOWN -> {
                pathBuffer.moveTo(cx, cy + s)
                pathBuffer.lineTo(cx - s, cy - s * 0.8f)
                pathBuffer.lineTo(cx + s, cy - s * 0.8f)
            }
            PS1InputFrame.BTN_LEFT -> {
                pathBuffer.moveTo(cx - s, cy)
                pathBuffer.lineTo(cx + s * 0.8f, cy - s)
                pathBuffer.lineTo(cx + s * 0.8f, cy + s)
            }
            PS1InputFrame.BTN_RIGHT -> {
                pathBuffer.moveTo(cx + s, cy)
                pathBuffer.lineTo(cx - s * 0.8f, cy - s)
                pathBuffer.lineTo(cx - s * 0.8f, cy + s)
            }
        }
        pathBuffer.close()
val triPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isPressed) Color.WHITE else Color.argb(220, 255, 215, 0) // Gold triangle #FFD700
}
        canvas.drawPath(pathBuffer, triPaint)
    }
private fun drawActionCircle(canvas: Canvas, rect: RectF, neonColor: Int, isPressed: Boolean, drawSymbol: (Float, Float) -> Unit) {
        val cx = rect.centerX()
val cy = rect.centerY()
val r = rect.width() / 2f
        // Frosted dark glass circle base
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isPressed) Color.argb(160, Color.red(neonColor), Color.green(neonColor), Color.blue(neonColor)) else colorDarkCross
        }
        canvas.drawCircle(cx, cy, r, bgPaint)
        // Thin neon glowing outline
    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(1.8f)
            color = if (isPressed) Color.WHITE else neonColor
        }
        canvas.drawCircle(cx, cy, r, outlinePaint)
        // Draw symbol
drawSymbol(cx, cy)
    }
private fun drawTriggerPill(canvas: Canvas, rect: RectF, label: String, isPressed: Boolean) {
        val cr = dpToPx(18f) // Frosted pill shape
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isPressed) Color.argb(190, 212, 175, 55) else colorTranslucentDark
        }
        canvas.drawRoundRect(rect, cr, cr, bgPaint)
        // 1px gold border #D4AF37
    val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(1f)
            color = if (isPressed) Color.WHITE else colorGoldTrigger
        }
        canvas.drawRoundRect(rect, cr, cr, borderP)
        // White text
    val prevSize = textPaint.textSize
        textPaint.textSize = dpToPx(12f)
        textPaint.color = Color.WHITE
        canvas.drawText(label, rect.centerX(), rect.centerY() + dpToPx(4f), textPaint)
        textPaint.textSize = prevSize
    }
private fun drawSystemPill(canvas: Canvas, rect: RectF, label: String, isPressed: Boolean) {
        val cr = dpToPx(16f) // 80x32dp pill
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isPressed) Color.argb(180, 255, 215, 0) else colorTranslucentDark
        }
        canvas.drawRoundRect(rect, cr, cr, bgPaint)
val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(1f)
            color = if (isPressed) Color.WHITE else colorNeonGold
        }
        canvas.drawRoundRect(rect, cr, cr, borderP)
val prevSize = textPaint.textSize
        textPaint.textSize = dpToPx(10.5f)
        textPaint.color = if (isPressed) Color.WHITE else Color.rgb(226, 232, 240)
        canvas.drawText(label, rect.centerX(), rect.centerY() + dpToPx(3.5f), textPaint)
        textPaint.textSize = prevSize
    }
override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isControlsVisible) return false
        val pointerCount = event.pointerCount
        var anyPointerInside = false
        for (i in 0 until pointerCount) {
            if (isPointInsideControls(event.getX(i), event.getY(i))) {
                anyPointerInside = true
                break
            }
        }
        if (!anyPointerInside && currentTouchMask == 0 && event.actionMasked == MotionEvent.ACTION_DOWN) {
            return false
        }
var newMask = 0
        if (event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL) {
            for (i in 0 until pointerCount) {
                if (event.actionMasked == MotionEvent.ACTION_POINTER_UP && i == event.actionIndex) {
                    continue
                }
val px = event.getX(i)
val py = event.getY(i)
                // D-Pad
if (rectUp.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_UP
                if (rectDown.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_DOWN
                if (rectLeft.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_LEFT
                if (rectRight.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_RIGHT
                // Action
if (rectTriangle.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_TRIANGLE
                if (rectCircle.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_CIRCLE
                if (rectCross.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_CROSS
                if (rectSquare.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_SQUARE
                // Triggers
if (rectL1.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_L1
                if (rectL2.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_L2
                if (rectR1.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_R1
                if (rectR2.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_R2
                // System
if (rectSelect.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_SELECT
                if (rectStart.contains(px, py)) newMask = newMask or PS1InputFrame.BTN_START
            }
        }
        // Mutual exclusion // Mutual exclusion for opposing D-Pad directions
        if ((newMask and PS1InputFrame.BTN_LEFT) != 0 && (newMask and PS1InputFrame.BTN_RIGHT) != 0) {
            newMask = newMask and PS1InputFrame.BTN_RIGHT.inv()
        }
        if ((newMask and PS1InputFrame.BTN_UP) != 0 && (newMask and PS1InputFrame.BTN_DOWN) != 0) {
            newMask = newMask and PS1InputFrame.BTN_DOWN.inv()
        }
        if (newMask != currentTouchMask) {
            val newlyPressed = (newMask and currentTouchMask.inv()) != 0
            if (newlyPressed && hapticEnabled) {
                triggerHaptic()
            }
            currentTouchMask = newMask
            onInputMaskChanged?.invoke(currentTouchMask)
            invalidate()
        }
        return anyPointerInside || currentTouchMask != 0
    }
private fun triggerHaptic() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18)
            }
        }
    }}