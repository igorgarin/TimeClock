package com.example.timeclock

/**
 * Geometry for the 2000 x 1200 design canvas.
 *
 * Values are design pixels, not Android dp. Convert them with referenceScale()
 * against the available content area before applying them to Android Views.
 * V1 date/time geometry remains owned by the legacy layout until explicitly
 * approved for redesign.
 */
object TimeClockUiConfig {
    const val REFERENCE_WIDTH = 2000f
    const val REFERENCE_HEIGHT = 1200f

    data class Element(
        val width: Float,
        val height: Float,
        val left: Float? = null,
        val right: Float? = null,
        val top: Float? = null,
        val bottom: Float? = null,
        val opacity: Float = 1f
    )

    const val FPS_TOP = 110f

    val colorSchemeButton = Element(
        width = 60f,
        height = 60f
    )

    val logo = Element(
        width = 360f,
        height = 148f,
        left = 90f,
        bottom = 90f,
        opacity = 0.20f
    )

    val zenerFigure = Element(
        width = 390f,
        height = 390f,
        left = 490f,
        bottom = 90f
    )

    val clapperboard = Element(
        width = 512f,
        height = 512f,
        opacity = 0.15f
    )

    /**
     * Uniform scale preserves the Illustrator canvas proportions.
     * The caller supplies the actual available content width and height
     * after applying the chosen system-bar/insets policy.
     */
    fun referenceScale(contentWidthPx: Int, contentHeightPx: Int): Float {
        require(contentWidthPx > 0 && contentHeightPx > 0)
        return minOf(
            contentWidthPx / REFERENCE_WIDTH,
            contentHeightPx / REFERENCE_HEIGHT
        )
    }

    fun px(value: Float, scale: Float): Int = (value * scale).toInt()
}
