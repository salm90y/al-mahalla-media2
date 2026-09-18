package com.ps1.netplay.ui
import android.widget.*
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/** * Elegant, compact vertical audio voice visualizer bar. * Displays dynamic animated equalizer bars for active speaking participants. */class VerticalVoiceMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
private var isSpeaking = false
    private var currentLevel = 0.15f // 0.0 to 1.0
    private val numBars = 3
    private val barHeights = FloatArray(numBars) { 0.2f }
private var animator: ValueAnimator? = null
    private val rectF = RectF()
    init {
        startIdleAnimation()
    }
fun setSpeaking(speaking: Boolean, level: Float = 0.8f) {
        if (isSpeaking == speaking && (currentLevel - level) < 0.05f) return
        isSpeaking = speaking
        currentLevel = if (speaking) level.coerceIn(0.3f, 1.0f) else 0.15f
        if (speaking) {
            startSpeakingAnimation()
        } else {
            startIdleAnimation()
        }
    }
private fun startSpeakingAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                for (i in 0 until numBars) {
                    val rnd = Random.nextFloat() * 0.7f + 0.3f
                    barHeights[i] = (rnd * currentLevel).coerceIn(0.2f, 1.0f)
                }
                invalidate()
            }
            start()
        }
    }
private fun startIdleAnimation() {
        animator?.cancel()
        for (i in 0 until numBars) {
            barHeights[i] = 0.2f
        }
        invalidate()
    }
override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
val w = width.toFloat()
val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val barWidth = (w / (numBars * 2.2f)).coerceAtLeast(3f)
val spacing = barWidth * 0.8f
        val totalWidth = (numBars * barWidth) + ((numBars - 1) * spacing)
var startX = (w - totalWidth) / 2f
        for (i in 0 until numBars) {
            val barH = (h * barHeights[i]).coerceIn(4f, h)
val top = h - barH
            val bottom = h
            val right = startX + barWidth
            rectF.set(startX, top, right, bottom)
            // Color grading: Green (#10B981) when speaking, Gold (#FFD700) on high peaks, Dark Slate (#4B5563) on idle
            barPaint.color = when {
                !isSpeaking -> Color.parseColor("#4B5563")
                barHeights[i] > 0.75f -> Color.parseColor("#FFD700")
                else -> Color.parseColor("#10B981")
            }
            canvas.drawRoundRect(rectF, barWidth / 2f, barWidth / 2f, barPaint)
            startX += barWidth + spacing
        }
    }
override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }}