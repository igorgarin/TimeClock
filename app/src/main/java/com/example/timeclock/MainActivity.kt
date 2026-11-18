package com.example.timeclock

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private enum class ColorScheme { DARK, LIGHT, NEUTRAL_GRAY }

    private val handler = Handler(Looper.getMainLooper())
    private val supportedFps = listOf(24, 25, 30, 50, 60)
    private val zenerAssets = listOf("1_Circle.svg", "2_TriangleUp.svg", "3_Cross.svg", "4_TriangleDown.svg", "5_Star.svg")

    private var selectedFps = 30
    private var colorScheme = ColorScheme.NEUTRAL_GRAY
    private var showingFpsScreen = false
    private var loopRunning = false
    private var lastDate = ""
    private var zenerEpochNanos = 0L
    private var clapperAnimator: ObjectAnimator? = null

    private lateinit var frameClock: FrameClockCalculator
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var tvDate: TextView
    private lateinit var tvHour: TextView
    private lateinit var tvMinute: TextView
    private lateinit var tvSecond: TextView
    private lateinit var tvColon1: TextView
    private lateinit var tvColon2: TextView
    private lateinit var tvFrameCounter: TextView
    private lateinit var tvFps: TextView
    private lateinit var imgZener: SvgCanvasView
    private lateinit var imgLogo: SvgCanvasView
    private lateinit var imgThemes: SvgCanvasView
    private lateinit var imgClapper: SvgCanvasView

    private lateinit var zenerDrawables: List<android.graphics.drawable.Drawable>
    private lateinit var logoDrawable: android.graphics.drawable.Drawable
    private lateinit var clapperDrawable: android.graphics.drawable.Drawable
    private lateinit var themesDrawable: android.graphics.drawable.Drawable

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val hourFormat = SimpleDateFormat("HH", Locale.getDefault())
    private val minuteFormat = SimpleDateFormat("mm", Locale.getDefault())
    private val secondFormat = SimpleDateFormat("ss", Locale.getDefault())

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!loopRunning || showingFpsScreen) return
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
            tvFrameCounter.text = "%02d".format(frameClock.frameIndex(selectedFps, wallNow, elapsedNow))
            imgZener.setSvgDrawable(zenerDrawables[ZenerCycle.index(elapsedNow - zenerEpochNanos)])
            handler.postDelayed(this, 8L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val wallNow = System.currentTimeMillis()
        val elapsedNow = SystemClock.elapsedRealtimeNanos()
        frameClock = FrameClockCalculator(wallNow, elapsedNow)
        zenerEpochNanos = elapsedNow

        zenerDrawables = zenerAssets.map { SvgAssetLoader.load(this, it) }
        logoDrawable = SvgAssetLoader.load(this, "IGRN_logo.svg")
        clapperDrawable = SvgAssetLoader.load(this, "clapperboard_V2_03.svg")
        themesDrawable = SvgAssetLoader.load(this, "icon_themes.svg")

        showMainScreen()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showingFpsScreen) showMainScreen() else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        startTimeLoop()
        if (::imgClapper.isInitialized) startClapperBreathing(imgClapper)
    }

    override fun onPause() {
        stopTimeLoop()
        stopClapperBreathing()
        super.onPause()
    }

    private fun startTimeLoop() {
        if (loopRunning) return
        loopRunning = true
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    private fun stopTimeLoop() {
        loopRunning = false
        handler.removeCallbacks(updateRunnable)
    }

    private fun stopClapperBreathing() {
        clapperAnimator?.cancel()
        clapperAnimator = null
    }

    private fun showMainScreen() {
        showingFpsScreen = false
        stopClapperBreathing()
        setContentView(R.layout.activity_main)
        rootLayout = findViewById(R.id.rootLayout)
        tvDate = findViewById(R.id.tvDate)
        tvHour = findViewById(R.id.tvHour)
        tvColon1 = findViewById(R.id.tvColon1)
        tvMinute = findViewById(R.id.tvMinute)
        tvColon2 = findViewById(R.id.tvColon2)
        tvSecond = findViewById(R.id.tvSecond)
        tvFrameCounter = findViewById(R.id.tvFrameCounter)
        tvFps = findViewById(R.id.tvFps)
        imgZener = findViewById(R.id.imgZener)
        imgLogo = findViewById(R.id.imgLogo)
        imgThemes = findViewById(R.id.imgThemes)
        imgClapper = findViewById(R.id.imgClapper)

        findViewById<View>(R.id.btnInvert).visibility = View.GONE
        imgLogo.setSvgDrawable(logoDrawable)
        imgThemes.setSvgDrawable(themesDrawable)
        imgClapper.setSvgDrawable(clapperDrawable)
        imgLogo.groupAlpha = TimeClockUiConfig.logo.opacity
        imgClapper.rotation = TimeClockUiConfig.clapperboard.rotation
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
            fun px(v: Float) = TimeClockUiConfig.px(v, scale)

            (imgLogo.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.logo.width)
                height = px(TimeClockUiConfig.logo.height)
                marginStart = px(TimeClockUiConfig.logo.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.logo.bottom ?: 0f)
            }.also { imgLogo.layoutParams = it }

            (imgZener.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.zenerFigure.width)
                height = px(TimeClockUiConfig.zenerFigure.height)
                marginStart = px(TimeClockUiConfig.zenerFigure.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.zenerFigure.bottom ?: 0f)
            }.also { imgZener.layoutParams = it }

            (tvFrameCounter.layoutParams as ConstraintLayout.LayoutParams).apply {
                bottomMargin = px(TimeClockUiConfig.frameCounter.bottom ?: 90f)
                marginEnd = px(TimeClockUiConfig.frameCounter.right ?: 48f)
            }.also { tvFrameCounter.layoutParams = it }
            tvFrameCounter.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(TimeClockUiConfig.frameCounter.textSize).toFloat())

            (tvFps.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = px(TimeClockUiConfig.FPS_TOP)
            }.also { tvFps.layoutParams = it }

            (imgThemes.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.colorSchemeButton.width)
                height = px(TimeClockUiConfig.colorSchemeButton.height)
                topMargin = px(TimeClockUiConfig.FPS_TOP)
            }.also { imgThemes.layoutParams = it }

            (imgClapper.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.clapperboard.width)
                height = px(TimeClockUiConfig.clapperboard.height)
                marginStart = px(TimeClockUiConfig.clapperboard.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.clapperboard.bottom ?: 0f)
            }.also { imgClapper.layoutParams = it }

            imgLogo.groupAlpha = TimeClockUiConfig.logo.opacity
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
        val topRow: Int
        val middleRow: Int
        val bottomRow: Int
        when (colorScheme) {
            ColorScheme.NEUTRAL_GRAY -> {
                background = Color.rgb(118, 118, 118)
                topRow = Color.rgb(35, 35, 35)
                middleRow = Color.WHITE
                bottomRow = Color.BLACK
            }
            ColorScheme.DARK -> {
                background = Color.BLACK
                topRow = Color.rgb(255, 168, 0)
                middleRow = Color.rgb(255, 234, 0)
                bottomRow = Color.rgb(5, 208, 0)
            }
            ColorScheme.LIGHT -> {
                background = Color.WHITE
                topRow = Color.rgb(0, 51, 204)
                middleRow = Color.rgb(0, 51, 204)
                bottomRow = Color.rgb(255, 43, 255)
            }
        }
        rootLayout.setBackgroundColor(background)
        tvDate.setTextColor(topRow)
        tvFps.setTextColor(topRow)
        imgThemes.tintColor = topRow
        listOf(tvHour, tvMinute, tvSecond, tvColon1, tvColon2).forEach { it.setTextColor(middleRow) }
        tvFrameCounter.setTextColor(bottomRow)
        imgZener.tintColor = bottomRow
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
            rotation = TimeClockUiConfig.clapperboard.rotation
        }
        val logo = SvgCanvasView(this).apply {
            setSvgDrawable(logoDrawable)
            groupAlpha = TimeClockUiConfig.logo.opacity
        }
        val back = TextView(this).apply {
            text = "BACK"
            setTextColor(Color.rgb(35, 35, 35))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setOnClickListener { showMainScreen() }
        }
        val band = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.rgb(74, 74, 74))
                cornerRadius = 30f
            }
        }

        supportedFps.forEach { fps ->
            val option = TextView(this).apply {
                text = fps.toString()
                gravity = Gravity.CENTER
                includeFontPadding = false
                tag = fps
                setOnClickListener {
                    selectedFps = fps
                    updateFpsSelection(band)
                }
            }
            band.addView(option, LinearLayout.LayoutParams(0, -1, 1f))
        }

        screen.addView(clapper, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        })
        screen.addView(logo, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        })
        screen.addView(back, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        })
        screen.addView(band, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        })
        setContentView(screen)

        screen.post {
            val scale = TimeClockUiConfig.referenceScale(screen.width, screen.height)
            fun px(v: Float) = TimeClockUiConfig.px(v, scale)

            (clapper.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.clapperboard.width)
                height = px(TimeClockUiConfig.clapperboard.height)
                marginStart = px(TimeClockUiConfig.clapperboard.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.clapperboard.bottom ?: 0f)
            }.also { clapper.layoutParams = it }
            (logo.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.logo.width)
                height = px(TimeClockUiConfig.logo.height)
                marginStart = px(TimeClockUiConfig.logo.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.logo.bottom ?: 0f)
            }.also { logo.layoutParams = it }
            (back.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.fpsBack.width)
                height = px(TimeClockUiConfig.fpsBack.height)
                marginStart = px(TimeClockUiConfig.fpsBack.left)
                topMargin = px(TimeClockUiConfig.fpsBack.top)
            }.also { back.layoutParams = it }
            back.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(TimeClockUiConfig.fpsBack.textSize).toFloat())
            (band.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.fpsBand.width)
                height = px(TimeClockUiConfig.fpsBand.height)
                marginStart = px(TimeClockUiConfig.fpsBand.left)
                topMargin = px(TimeClockUiConfig.fpsBand.top)
            }.also { band.layoutParams = it }
            for (i in 0 until band.childCount) {
                (band.getChildAt(i) as TextView).setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    px(TimeClockUiConfig.fpsBand.textSize).toFloat()
                )
            }
        }
        updateFpsSelection(band)
        startClapperBreathing(clapper)
    }

    private fun updateFpsSelection(band: LinearLayout) {
        for (index in 0 until band.childCount) {
            val option = band.getChildAt(index) as TextView
            option.setTextColor(if (option.tag == selectedFps) Color.WHITE else Color.BLACK)
        }
    }

    private fun startClapperBreathing(view: View) {
        stopClapperBreathing()
        clapperAnimator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.5f, 2.0f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.5f, 2.0f),
            PropertyValuesHolder.ofFloat("groupAlpha", 0.10f, 0.20f)
        ).apply {
            duration = 6000L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }
}
