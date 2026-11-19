package com.example.timeclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Picture
import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.caverock.androidsvg.SVG

/**
 * Loads and draws SVG artwork directly from app/src/main/assets/design.
 * Paint effects are applied to a composited layer so path overlaps stay opaque.
 */
object SvgAssetLoader {
    private const val DESIGN_DIRECTORY = "design"

    fun load(context: Context, assetName: String): Drawable {
        val svgText = context.assets.open("$DESIGN_DIRECTORY/$assetName").use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
        val svg = SVG.getFromString(svgText)
        return SvgPictureDrawable(svg.renderToPicture())
    }
}

private class SvgPictureDrawable(
    private val picture: Picture
) : Drawable() {
    private val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val targetBounds = RectF(bounds)
        if (targetBounds.isEmpty || picture.width <= 0 || picture.height <= 0) return

        val saveCount = canvas.saveLayer(targetBounds, layerPaint)
        canvas.translate(targetBounds.left, targetBounds.top)
        canvas.scale(
            targetBounds.width() / picture.width,
            targetBounds.height() / picture.height
        )
        canvas.drawPicture(picture)
        canvas.restoreToCount(saveCount)
    }

    override fun setAlpha(alpha: Int) {
        layerPaint.alpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        layerPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android graphics APIs")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = picture.width

    override fun getIntrinsicHeight(): Int = picture.height
}
