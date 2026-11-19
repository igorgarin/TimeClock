package com.example.timeclock

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private enum class ColorScheme { DARK, LIGHT, NEUTRAL_GRAY }

    private val supportedFps = listOf(24, 25, 30, 50, 60, 100)
    private val zenerAssets = listOf(
        "Zener_1_Circle.svg",
        "Zener_2_TriangleUp.svg",
        "Zener_3_Cross.svg",
        "Zener_4_TriangleDown.svg",
        "Zener_5_Star.svg"
    )
    private val zenerRingAssets = (1..12).map { "Zener_Ring_$it.svg" }

    private var selectedFps = 30
    private var colorScheme = ColorScheme.NEUTRAL_GRAY
    private var showingFpsScreen = false
    private var loopRunning = false
    private var lastDate = ""
    private var zenerEpochFrameNanos: Long? = null
    private var lastZenerIndex: Int? = Int.MIN_VALUE
    private var lastRingIndex = Int.MIN_VALUE
    private var clapperBreathingAnimator: ValueAnimator? = null

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var tvDate: TextView
    private lateinit var tvHour: TextView
    private lateinit var tvMinute: TextView
    private lateinit var tvSecond: TextView
    private lateinit var tvColon1: TextView
    private lateinit var tvColon2: TextView
    private lateinit var tvFrameCounter: TextView
    private lateinit var tvFps: TextView
    private lateinit var zenerContainer: FrameLayout
    private lateinit var imgZener: SvgCanvasView
    private lateinit var imgZenerRing: SvgCanvasView
    private lateinit var imgLogo: SvgCanvasView
    private lateinit var imgThemes: SvgCanvasView
    private lateinit var imgClapper: SvgCanvasView

    private lateinit var frameClock: FrameClockCalculator
    private lateinit var zenerDrawables: List<android.graphics.drawable.Drawable>
    private lateinit var zenerRingDrawables: List<android.graphics.drawable.Drawable>
    private lateinit var logoDrawable: android.graphics.drawable.Drawable
    private lateinit var clapperDrawable: android.graphics.drawable.Drawable
    private lateinit var themesDrawable: android.graphics.drawable.Drawable

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val hourFormat = SimpleDateFormat("HH", Locale.getDefault())
    private val minuteFormat = SimpleDateFormat("mm", Locale.getDefault())
    private val secondFormat = SimpleDateFormat("ss", Locale.getDefault())

    private val frameCallback: Choreographer.FrameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        if (loopRunning && !showingFpsScreen) {
            updateScreen(frameTimeNanos)
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    private fun updateScreen(frameTimeNanos: Long) {
        val wallNow = System.currentTimeMillis()
        val elapsedNow = SystemClock.elapsedRealtimeNanos()
        val date = dateFormat.format(Date(wallNow))
        if (date != lastDate) {
            tvDate.text = date
            lastDate = date
        }

        val now = Date(wallNow)
        tvHour.text = hourFormat.format(now)
        tvMinute.text = minuteFormat.format(now)
        tvSecond.text = secondFormat.format(now)
        tvFrameCounter.text = "%02d".format(
            frameClock.frameIndex(selectedFps, wallNow, elapsedNow)
        )

        val epoch = zenerEpochFrameNanos ?: frameTimeNanos.also {
            zenerEpochFrameNanos = it
        }
        val elapsedInCycle = frameTimeNanos - epoch
        val figureIndex = ZenerCycle.index(elapsedInCycle)
        if (figureIndex != lastZenerIndex) {
            imgZener.setSvgDrawable(figureIndex?.let(zenerDrawables::get))
            lastZenerIndex = figureIndex
        }

        val ringIndex = ZenerRingCycle.index(elapsedInCycle)
        if (ringIndex != lastRingIndex) {
            imgZenerRing.setSvgDrawable(zenerRingDrawables[ringIndex])
            lastRingIndex = ringIndex
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        frameClock = FrameClockCalculator(
            System.currentTimeMillis(),
            SystemClock.elapsedRealtimeNanos()
        )
        zenerDrawables = zenerAssets.map { SvgAssetLoader.load(this, it) }
        zenerRingDrawables = zenerRingAssets.map { SvgAssetLoader.load(this, it) }
        logoDrawable = SvgAssetLoader.load(this, "IGRN_logo.svg")
        clapperDrawable = SvgAssetLoader.load(this, "clapperboard_V2_03.svg")
        themesDrawable = SvgAssetLoader.load(this, "icon_themes.svg")

        showMainScreen()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showingFpsScreen) {
                    showMainScreen()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        startTimeLoop()
    }

    override fun onPause() {
        stopTimeLoop()
        stopClapperBreathing()
        super.onPause()
    }

    private fun startTimeLoop() {
        if (loopRunning || showingFpsScreen) return
        loopRunning = true
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopTimeLoop() {
        loopRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun stopClapperBreathing() {
        clapperBreathingAnimator?.cancel()
        clapperBreathingAnimator = null
    }

    private fun showMainScreen() {
        showingFpsScreen = false
        stopClapperBreathing()
        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
        tvDate = findViewById(R.id.tvDate)
        lastDate = dateFormat.format(Date())
        tvDate.text = lastDate
        tvHour = findViewById(R.id.tvHour)
        tvColon1 = findViewById(R.id.tvColon1)
        tvMinute = findViewById(R.id.tvMinute)
        tvColon2 = findViewById(R.id.tvColon2)
        tvSecond = findViewById(R.id.tvSecond)
        tvFrameCounter = findViewById(R.id.tvFrameCounter)
        tvFps = findViewById(R.id.tvFps)
        zenerContainer = findViewById(R.id.zenerContainer)
        imgZener = findViewById(R.id.imgZener)
        imgZenerRing = findViewById(R.id.imgZenerRing)
        imgLogo = findViewById(R.id.imgLogo)
        imgThemes = findViewById(R.id.imgThemes)
        imgClapper = findViewById(R.id.imgClapper)
        lastZenerIndex = Int.MIN_VALUE
        lastRingIndex = Int.MIN_VALUE

        findViewById<View>(R.id.btnInvert).visibility = View.GONE
        imgLogo.setSvgDrawable(logoDrawable)
        imgThemes.setSvgDrawable(themesDrawable)
        imgClapper.setSvgDrawable(clapperDrawable)
        imgClapper.groupAlpha = TimeClockUiConfig.clapperboard.opacity
        imgClapper.rotation = -12f
        tvFps.text = "${selectedFps}fps"

        tvFps.setOnClickListener { showFpsScreen() }
        imgThemes.setOnClickListener { cycleColorScheme() }

        applyReferenceGeometry()
        applyColorScheme()
        startClapperBreathing(imgClapper)
        startTimeLoop()
    }

    private fun applyReferenceGeometry() {
        rootLayout.post {
            val scale = TimeClockUiConfig.referenceScale(rootLayout.width, rootLayout.height)
            fun px(value: Float) = TimeClockUiConfig.px(value, scale)

            (imgLogo.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.logo.width)
                height = px(TimeClockUiConfig.logo.height)
                marginStart = px(TimeClockUiConfig.logo.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.logo.bottom ?: 0f)
            }.also { imgLogo.layoutParams = it }

            (zenerContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.zenerFigure.width)
                height = px(TimeClockUiConfig.zenerFigure.height)
                marginStart = px(TimeClockUiConfig.zenerFigure.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.zenerFigure.bottom ?: 0f)
            }.also { zenerContainer.layoutParams = it }

            (tvFrameCounter.layoutParams as ConstraintLayout.LayoutParams).apply {
                marginEnd = px(TimeClockUiConfig.frameCounter.right ?: 48f)
                bottomMargin = px(-20f)
            }.also { tvFrameCounter.layoutParams = it }
            tvFrameCounter.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                px(TimeClockUiConfig.frameCounter.textSize).toFloat()
            )

            (tvFps.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = px(TimeClockUiConfig.FPS_LABEL_TOP)
            }.also { tvFps.layoutParams = it }

            (imgThemes.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.colorSchemeButton.width)
                height = px(TimeClockUiConfig.colorSchemeButton.height)
                topMargin = px(TimeClockUiConfig.FPS_TOP)
            }.also { imgThemes.layoutParams = it }

            rootLayout.requestLayout()
        }
    }

    private fun cycleColorScheme() {
        colorScheme = when (colorScheme) {
            ColorScheme.NEUTRAL_GRAY -> ColorScheme.DARK
            ColorScheme.DARK -> ColorScheme.LIGHT
            ColorScheme.LIGHT -> ColorScheme.NEUTRAL_GRAY
        }
        applyColorScheme()
    }

    private fun applyColorScheme() {
        val background: Int
        val dateColor: Int
        val timeColor: Int
        val markerColor: Int
        when (colorScheme) {
            ColorScheme.NEUTRAL_GRAY -> {
                background = Color.rgb(118, 118, 118)
                dateColor = Color.rgb(51, 51, 51)
                timeColor = Color.WHITE
                markerColor = Color.BLACK
            }
            ColorScheme.DARK -> {
                background = Color.BLACK
                dateColor = Color.rgb(255, 168, 0)
                timeColor = Color.rgb(251, 247, 42)
                markerColor = Color.rgb(62, 213, 45)
            }
            ColorScheme.LIGHT -> {
                background = Color.WHITE
                dateColor = Color.rgb(0, 37, 148)
                timeColor = Color.rgb(0, 51, 204)
                markerColor = Color.rgb(255, 43, 255)
            }
        }

        rootLayout.setBackgroundColor(background)
        tvDate.setTextColor(dateColor)
        tvFps.setTextColor(dateColor)
        imgThemes.tintColor = dateColor
        listOf(tvHour, tvMinute, tvSecond, tvColon1, tvColon2).forEach {
            it.setTextColor(timeColor)
        }
        tvFrameCounter.setTextColor(markerColor)
        imgZener.tintColor = markerColor
        imgZenerRing.tintColor = markerColor
        imgLogo.tintColor = null
    }

    private fun showFpsScreen() {
        showingFpsScreen = true
        stopTimeLoop()
        stopClapperBreathing()

        val screen = ConstraintLayout(this).apply {
            setBackgroundColor(Color.rgb(118, 118, 118))
            keepScreenOn = true
        }
        val clapper = SvgCanvasView(this).apply {
            setSvgDrawable(clapperDrawable)
            rotation = -12f
        }
        val logo = SvgCanvasView(this).apply {
            setSvgDrawable(logoDrawable)
            groupAlpha = TimeClockUiConfig.logo.opacity
        }
        val backButton = TextView(this).apply {
            text = "BACK"
            setTextColor(Color.rgb(51, 51, 51))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            includeFontPadding = false
            isClickable = true
            isFocusable = true
            setOnClickListener { showMainScreen() }
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.rgb(74, 74, 74))
                cornerRadius = 32f
            }
        }

        supportedFps.forEach { fps ->
            val option = TextView(this).apply {
                text = fps.toString()
                setTextColor(if (fps == selectedFps) Color.WHITE else Color.BLACK)
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                gravity = Gravity.CENTER
                includeFontPadding = false
                setOnClickListener {
                    selectedFps = fps
                    tvFps.text = "${selectedFps}fps"
                    updateFpsSelection(list)
                }
                tag = fps
            }
            list.addView(option, LinearLayout.LayoutParams(0, -1, 1f))
        }

        screen.addView(clapper, ConstraintLayout.LayoutParams(dp(420), dp(420)).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = dp(64)
            bottomMargin = dp(48)
        })
        screen.addView(logo, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        })
        screen.addView(backButton, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        })
        screen.addView(list, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        })

        setContentView(screen)
        screen.post {
            val scale = TimeClockUiConfig.referenceScale(screen.width, screen.height)
            fun px(value: Float) = TimeClockUiConfig.px(value, scale)

            (logo.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.logo.width)
                height = px(TimeClockUiConfig.logo.height)
                marginStart = px(TimeClockUiConfig.logo.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.logo.bottom ?: 0f)
            }.also { logo.layoutParams = it }

            (backButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.fpsBack.width)
                height = px(TimeClockUiConfig.fpsBack.height)
                marginStart = px(TimeClockUiConfig.fpsBack.left ?: 0f)
                topMargin = px(TimeClockUiConfig.fpsBack.top ?: 0f)
            }.also { backButton.layoutParams = it }
            backButton.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                px(TimeClockUiConfig.fpsBack.textSize).toFloat()
            )

            list.background = GradientDrawable().apply {
                setColor(Color.rgb(74, 74, 74))
                cornerRadius = px(32f).toFloat()
            }
            (list.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.fpsBand.width)
                height = px(TimeClockUiConfig.fpsBand.height)
                marginStart = px(TimeClockUiConfig.fpsBand.left ?: 0f)
                topMargin = px(TimeClockUiConfig.fpsBand.top ?: 0f)
            }.also { list.layoutParams = it }
            for (index in 0 until list.childCount) {
                (list.getChildAt(index) as TextView).setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    px(160f).toFloat()
                )
            }
        }
        startClapperBreathing(clapper)
    }

    private fun updateFpsSelection(list: LinearLayout) {
        for (index in 0 until list.childCount) {
            val option = list.getChildAt(index) as TextView
            option.setTextColor(if (option.tag == selectedFps) Color.WHITE else Color.BLACK)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun startClapperBreathing(view: SvgCanvasView) {
        clapperBreathingAnimator?.cancel()
        clapperBreathingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 6000L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val scale = 1.5f + 0.5f * progress
                view.scaleX = scale
                view.scaleY = scale
                view.groupAlpha = 0.10f + 0.10f * progress
            }
            start()
        }
    }
}
