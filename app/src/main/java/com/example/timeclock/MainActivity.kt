package com.example.timeclock

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
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
    private val zenerAssets = listOf(
        "1_Circle.svg",
        "2_TriangleUp.svg",
        "3_Cross.svg",
        "4_TriangleDown.svg",
        "5_Star.svg"
    )

    private var selectedFps = 30
    private var colorScheme = ColorScheme.NEUTRAL_GRAY
    private var showingFpsScreen = false
    private var loopRunning = false
    private var lastDate = ""
    private var zenerEpochNanos = 0L

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var tvDate: TextView
    private lateinit var tvHour: TextView
    private lateinit var tvMinute: TextView
    private lateinit var tvSecond: TextView
    private lateinit var tvColon1: TextView
    private lateinit var tvColon2: TextView
    private lateinit var tvFrameCounter: TextView
    private lateinit var tvFps: TextView
    private lateinit var imgZener: ImageView
    private lateinit var imgLogo: ImageView
    private lateinit var imgThemes: ImageView
    private lateinit var imgClapper: ImageView

    private lateinit var frameClock: FrameClockCalculator
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
            tvFrameCounter.text = "%02d".format(
                frameClock.frameIndex(selectedFps, wallNow, elapsedNow)
            )

            imgZener.setImageDrawable(zenerDrawables[ZenerCycle.index(elapsedNow - zenerEpochNanos)])
            handler.postDelayed(this, 8L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val wallNow = System.currentTimeMillis()
        val elapsedNow = SystemClock.elapsedRealtimeNanos()
        frameClock = FrameClockCalculator(wallNow, elapsedNow)
        zenerEpochNanos = elapsedNow

        zenerDrawables = zenerAssets.map { SvgAssetLoader.load(this, it) }
        logoDrawable = SvgAssetLoader.load(this, "IGRN_logo.svg")
        clapperDrawable = SvgAssetLoader.load(this, "clapperboard.svg")
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

    private fun showMainScreen() {
        showingFpsScreen = false
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
        imgLogo.setImageDrawable(logoDrawable)
        imgThemes.setImageDrawable(themesDrawable)
        imgClapper.setImageDrawable(clapperDrawable)
        imgClapper.alpha = TimeClockUiConfig.clapperboard.opacity
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
            fun px(referencePx: Float) = TimeClockUiConfig.px(referencePx, scale)

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
                bottomMargin = px(90f)
            }.also { tvFrameCounter.layoutParams = it }

            (tvFps.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = px(TimeClockUiConfig.FPS_TOP)
            }.also { tvFps.layoutParams = it }

            (imgThemes.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.colorSchemeButton.width)
                height = px(TimeClockUiConfig.colorSchemeButton.height)
                topMargin = px(TimeClockUiConfig.FPS_TOP)
            }.also { imgThemes.layoutParams = it }

            imgLogo.alpha = TimeClockUiConfig.logo.opacity
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
        val logoColor: Int
        when (colorScheme) {
            ColorScheme.NEUTRAL_GRAY -> {
                background = Color.rgb(118, 118, 118)
                dateColor = Color.rgb(35, 35, 35)
                timeColor = Color.WHITE
                markerColor = Color.BLACK
                logoColor = Color.rgb(74, 74, 74)
            }
            ColorScheme.DARK -> {
                background = Color.BLACK
                dateColor = Color.rgb(255, 168, 0)
                timeColor = Color.rgb(255, 234, 0)
                markerColor = Color.rgb(5, 208, 0)
                logoColor = Color.WHITE
            }
            ColorScheme.LIGHT -> {
                background = Color.WHITE
                dateColor = Color.rgb(0, 51, 204)
                timeColor = Color.rgb(0, 51, 204)
                markerColor = Color.rgb(255, 43, 255)
                logoColor = Color.DKGRAY
            }
        }

        rootLayout.setBackgroundColor(background)
        tvDate.setTextColor(dateColor)
        listOf(tvHour, tvMinute, tvSecond, tvColon1, tvColon2).forEach {
            it.setTextColor(timeColor)
        }
        tvFrameCounter.setTextColor(markerColor)
        tvFps.setTextColor(dateColor)
        imgZener.setColorFilter(markerColor)
        imgLogo.setColorFilter(logoColor)
        imgThemes.setColorFilter(dateColor)
    }

    private fun showFpsScreen() {
        showingFpsScreen = true
        stopTimeLoop()

        val screen = ConstraintLayout(this).apply {
            setBackgroundColor(Color.rgb(118, 118, 118))
            keepScreenOn = true
        }

        val clapper = ImageView(this).apply {
            setImageDrawable(clapperDrawable)
            alpha = TimeClockUiConfig.clapperboard.opacity
            rotation = -12f
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val logo = ImageView(this).apply {
            setImageDrawable(logoDrawable)
            alpha = TimeClockUiConfig.logo.opacity
            setColorFilter(Color.DKGRAY)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        supportedFps.forEach { fps ->
            val option = TextView(this).apply {
                text = fps.toString()
                textSize = 64f
                gravity = Gravity.CENTER
                setPadding(48, 14, 48, 14)
                setOnClickListener {
                    selectedFps = fps
                    updateFpsSelection(list)
                }
                tag = fps
            }
            list.addView(option, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        screen.addView(clapper, ConstraintLayout.LayoutParams(420, 420).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = 64
            bottomMargin = 48
        })
        screen.addView(logo, ConstraintLayout.LayoutParams(360, 148).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = 90
            bottomMargin = 90
        })
        screen.addView(list, ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        })

        setContentView(screen)
        updateFpsSelection(list)
        startClapperBreathing(clapper)
    }

    private fun updateFpsSelection(list: LinearLayout) {
        for (index in 0 until list.childCount) {
            val option = list.getChildAt(index) as TextView
            option.setTextColor(
                if (option.tag == selectedFps) Color.WHITE else Color.BLACK
            )
        }
    }

    private fun startClapperBreathing(view: View) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.5f, 2.0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.5f, 2.0f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.10f, 0.20f)
        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha).apply {
            duration = 6000L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }
}
