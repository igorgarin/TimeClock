package com.example.timeclock

/** Centralized reference geometry for the 2000 x 1200 V2 canvas. */
object TimeClockUiConfig {
    const val REFERENCE_WIDTH = 2000f
    const val REFERENCE_HEIGHT = 1200f
    const val FPS_TOP = 110f

    data class Element(
        val width: Float,
        val height: Float,
        val left: Float? = null,
        val right: Float? = null,
        val top: Float? = null,
        val bottom: Float? = null,
        val opacity: Float = 1f,
        val textSize: Float = 0f,
        val rotation: Float = 0f
    )

    const val FPS_LABEL_TOP = 100f

    val colorSchemeButton = Element(60f, 60f)
    val functionsButton = Element(60f, 60f, top = 110f)
    val clackButton = Element(250f, 250f, left = 120f, bottom = 100f)
    const val TOP_BUTTON_GAP = 60f
    val logo = Element(720f, 296f, left = -60f, bottom = 90f, opacity = 0.30f)
    val zenerFigure = Element(500f, 500f, left = 770f, bottom = 25f)
    val frameCounter = Element(400f, 360f, right = 48f, bottom = -28f, textSize = 300f)
    val clapperboard = Element(512f, 512f, opacity = 0.15f)
    val fpsBack = Element(220f, 100f, left = 110f, top = 110f, textSize = 68f)
    val fpsBand = Element(1866f, 234f, left = 67f, top = 320f, textSize = 160f)
    fun referenceScale(contentWidthPx: Int, contentHeightPx: Int): Float {
        require(contentWidthPx > 0 && contentHeightPx > 0)
        return minOf(contentWidthPx / REFERENCE_WIDTH, contentHeightPx / REFERENCE_HEIGHT)
    }

    fun px(value: Float, scale: Float): Int = (value * scale).toInt()
}
