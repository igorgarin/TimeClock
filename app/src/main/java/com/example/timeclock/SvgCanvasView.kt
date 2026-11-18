package com.example.timeclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

/** Draws a complete SVG into one composited layer before applying tint/alpha. */
class SvgCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var vectorDrawable: Drawable? = null

    var groupAlpha: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var tintColor: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    fun setSvgDrawable(drawable: Drawable) {
        vectorDrawable = drawable
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawable = vectorDrawable ?: return
        if (width <= 0 || height <= 0) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (groupAlpha * 255f).roundToInt()
            tintColor?.let { colorFilter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }
        }
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), paint)
        drawable.bounds = Rect(0, 0, width, height)
        drawable.draw(canvas)
        canvas.restoreToCount(layer)
    }
}
