package com.example.timeclock

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.view.MotionEvent
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
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
    private val signalAssetNames = (1..6).map { "Signal$it.wav" }

    private var selectedFps = 30
    private var selectedSignalIndex = 0
    private var selectedVolumeLevel = 10
    private var colorScheme = ColorScheme.NEUTRAL_GRAY
    private var showingSubScreen = false
    private var clackFlashGeneration = 0
    private var soundPool: SoundPool? = null
    private val soundSampleIds = mutableListOf<Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
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
    private lateinit var imgFunctions: SvgCanvasView
    private lateinit var imgThemes: SvgCanvasView
    private lateinit var imgClapper: SvgCanvasView
    private lateinit var imgClack: SvgCanvasView

    private lateinit var frameClock: FrameClockCalculator
    private lateinit var zenerDrawables: List<android.graphics.drawable.Drawable>
    private lateinit var zenerRingDrawables: List<android.graphics.drawable.Drawable>
    private lateinit var clapperDrawable: android.graphics.drawable.Drawable
    private lateinit var subScreenClapperDrawable: android.graphics.drawable.Drawable
    private lateinit var themesDrawable: android.graphics.drawable.Drawable
    private lateinit var functionsDrawable: android.graphics.drawable.Drawable
    private lateinit var clackDrawable: android.graphics.drawable.Drawable

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val hourFormat = SimpleDateFormat("HH", Locale.getDefault())
    private val minuteFormat = SimpleDateFormat("mm", Locale.getDefault())
    private val secondFormat = SimpleDateFormat("ss", Locale.getDefault())

    private val frameCallback: Choreographer.FrameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        if (loopRunning && !showingSubScreen) {
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

        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        selectedSignalIndex = preferences.getInt(PREF_SIGNAL_INDEX, 0).coerceIn(0, signalAssetNames.lastIndex)
        selectedVolumeLevel = preferences.getInt(PREF_VOLUME_LEVEL, 10).coerceIn(1, 10)

        frameClock = FrameClockCalculator(
            System.currentTimeMillis(),
            SystemClock.elapsedRealtimeNanos()
        )
        zenerDrawables = zenerAssets.map { SvgAssetLoader.load(this, it) }
        zenerRingDrawables = zenerRingAssets.map { SvgAssetLoader.load(this, it) }
        clapperDrawable = SvgAssetLoader.load(this, "clapperboard_logo_4.svg")
        subScreenClapperDrawable = SvgAssetLoader.load(this, "clapperboard_logo.svg")
        themesDrawable = SvgAssetLoader.load(this, "icon_themes.svg")
        functionsDrawable = SvgAssetLoader.load(this, "icon_functions.svg")
        clackDrawable = SvgAssetLoader.load(this, "Clack_Button_02.svg")
        initializeSoundPool()

        showMainScreen()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showingSubScreen) {
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

    override fun onDestroy() {
        soundPool?.release()
        soundPool = null
        super.onDestroy()
    }

    override fun onPause() {
        stopTimeLoop()
        stopClapperBreathing()
        super.onPause()
    }

    private fun startTimeLoop() {
        if (loopRunning || showingSubScreen) return
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
        showingSubScreen = false
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
        imgThemes = findViewById(R.id.imgThemes)
        imgFunctions = findViewById(R.id.imgFunctions)
        imgClapper = findViewById(R.id.imgClapper)
        imgClack = findViewById(R.id.imgClack)
        lastZenerIndex = Int.MIN_VALUE
        lastRingIndex = Int.MIN_VALUE

        findViewById<View>(R.id.btnInvert).visibility = View.GONE
        imgThemes.setSvgDrawable(themesDrawable)
        imgFunctions.setSvgDrawable(functionsDrawable)
        imgClack.setSvgDrawable(clackDrawable)
        imgClapper.setSvgDrawable(clapperDrawable)
        imgClapper.groupAlpha = TimeClockUiConfig.clapperboard.opacity
        imgClapper.rotation = -12f
        tvFps.text = "${selectedFps}fps"

        tvFps.setOnClickListener { showFpsScreen() }
        imgFunctions.setOnClickListener { showFunctionsScreen() }
        imgThemes.setOnClickListener { cycleColorScheme() }
        imgClack.setOnClickListener { triggerClack() }
        imgClack.contentDescription = "Play clack sound"
        imgClack.isClickable = true
        imgClack.isFocusable = true
        imgClack.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_MOVE -> true
                else -> true
            }
        }

        applyReferenceGeometry()
        applyColorScheme()
        startClapperBreathing(imgClapper)
        startTimeLoop()
    }

    private fun applyReferenceGeometry() {
        rootLayout.post {
            val scale = TimeClockUiConfig.referenceScale(rootLayout.width, rootLayout.height)
            fun px(value: Float) = TimeClockUiConfig.px(value, scale)

            (imgClack.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.clackButton.width)
                height = px(TimeClockUiConfig.clackButton.height)
                marginStart = px(TimeClockUiConfig.clackButton.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.clackButton.bottom ?: 0f)
            }.also { imgClack.layoutParams = it }

            (zenerContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.zenerFigure.width)
                height = px(TimeClockUiConfig.zenerFigure.height)
                marginStart = px(TimeClockUiConfig.zenerFigure.left ?: 0f)
                bottomMargin = px(TimeClockUiConfig.zenerFigure.bottom ?: 0f)
            }.also { zenerContainer.layoutParams = it }

            (tvFrameCounter.layoutParams as ConstraintLayout.LayoutParams).apply {
                marginEnd = px(TimeClockUiConfig.frameCounter.right ?: 48f)
                bottomMargin = px(TimeClockUiConfig.frameCounter.bottom ?: 0f)
            }.also { tvFrameCounter.layoutParams = it }
            tvFrameCounter.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvHour.textSize)

            (tvFps.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = px(TimeClockUiConfig.FPS_LABEL_TOP)
                marginEnd = px(TimeClockUiConfig.TOP_BUTTON_GAP)
            }.also { tvFps.layoutParams = it }

            (imgFunctions.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.functionsButton.width)
                height = px(TimeClockUiConfig.functionsButton.height)
                topMargin = px(TimeClockUiConfig.functionsButton.top ?: TimeClockUiConfig.FPS_TOP)
                marginEnd = px(TimeClockUiConfig.TOP_BUTTON_GAP)
            }.also { imgFunctions.layoutParams = it }

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
        imgFunctions.tintColor = dateColor
        listOf(tvHour, tvMinute, tvSecond, tvColon1, tvColon2).forEach {
            it.setTextColor(timeColor)
        }
        tvFrameCounter.setTextColor(markerColor)
        imgZener.tintColor = markerColor
        imgZenerRing.tintColor = markerColor
        imgClack.tintColor = markerColor
    }

    private fun showFpsScreen() {
        showingSubScreen = true
        stopTimeLoop()
        stopClapperBreathing()

        val screen = ConstraintLayout(this).apply {
            setBackgroundColor(Color.rgb(118, 118, 118))
            keepScreenOn = true
        }
        val clapper = SvgCanvasView(this).apply {
            setSvgDrawable(subScreenClapperDrawable)
            rotation = -12f
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

    private fun initializeSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attributes)
            .build()
        soundPool = pool
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(loadedSoundIds) {
                    loadedSoundIds.add(sampleId)
                }
            }
        }
        signalAssetNames.forEach { name ->
            val sampleId = assets.openFd("audio/$name").use { descriptor ->
                pool.load(descriptor, 1)
            }
            soundSampleIds.add(sampleId)
        }
    }

    private fun playSelectedSound() {
        if (selectedVolumeLevel == 1) return
        val sampleId = soundSampleIds.getOrNull(selectedSignalIndex) ?: return
        val loaded = synchronized(loadedSoundIds) { sampleId in loadedSoundIds }
        if (!loaded || sampleId == 0) return
        val volume = ((selectedVolumeLevel - 1) / 9f).coerceIn(0f, 1f)
        soundPool?.play(sampleId, volume, volume, 1, 0, 1f)
    }

    private fun saveFunctionSettings() {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
            .putInt(PREF_SIGNAL_INDEX, selectedSignalIndex)
            .putInt(PREF_VOLUME_LEVEL, selectedVolumeLevel)
            .apply()
    }

    private fun triggerClack() {
        val generation = ++clackFlashGeneration
        rootLayout.setBackgroundColor(Color.RED)
        rootLayout.postDelayed({
            if (generation == clackFlashGeneration) applyColorScheme()
        }, CLACK_FLASH_DURATION_MS)
        playSelectedSound()
    }

    private fun showFunctionsScreen() {
        showingSubScreen = true
        stopTimeLoop()
        stopClapperBreathing()

        val screen = ConstraintLayout(this).apply {
            setBackgroundColor(Color.rgb(118, 118, 118))
            keepScreenOn = true
        }
        val clapper = SvgCanvasView(this).apply {
            setSvgDrawable(subScreenClapperDrawable)
            rotation = -12f
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
        val volumeBand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            contentDescription = "Volume slider"
            background = GradientDrawable().apply {
                setColor(Color.rgb(74, 74, 74))
                cornerRadius = 32f
            }
        }
        val volumeBars = mutableListOf<View>()
        repeat(10) {
            val cell = FrameLayout(this)
            val bar = View(this).apply {
                background = GradientDrawable().apply { cornerRadius = 12f }
            }
            cell.addView(bar, FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER))
            volumeBars += bar
            volumeBand.addView(cell, LinearLayout.LayoutParams(0, -1, 1f))
        }
        var volumeGestureActive = false
        var volumePreviewPending = false
        val volumeIdlePreview = Runnable {
            if (volumeGestureActive && volumePreviewPending) {
                volumePreviewPending = false
                playSelectedSound()
            }
        }
        fun updateVolumeFromTouch(x: Float) {
            if (volumeBand.width <= 0) return
            val level = ((x / volumeBand.width) * 10f).toInt().coerceIn(0, 9) + 1
            if (level == selectedVolumeLevel) return
            selectedVolumeLevel = level
            saveFunctionSettings()
            updateVolumeSelection(volumeBars)
            volumePreviewPending = true
            volumeBand.removeCallbacks(volumeIdlePreview)
            volumeBand.postDelayed(volumeIdlePreview, VOLUME_IDLE_PREVIEW_DELAY_MS)
        }
        volumeBand.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    volumeGestureActive = true
                    volumePreviewPending = false
                    updateVolumeFromTouch(event.x)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    updateVolumeFromTouch(event.x)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    updateVolumeFromTouch(event.x)
                    volumeGestureActive = false
                    volumeBand.removeCallbacks(volumeIdlePreview)
                    if (volumePreviewPending) {
                        volumePreviewPending = false
                        playSelectedSound()
                    }
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    volumeGestureActive = false
                    volumePreviewPending = false
                    volumeBand.removeCallbacks(volumeIdlePreview)
                    true
                }
                else -> true
            }
        }

        val signalNumbers = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.rgb(74, 74, 74))
                cornerRadius = 32f
            }
        }
        val signalNumberViews = mutableListOf<TextView>()
        signalAssetNames.indices.forEach { index ->
            val number = TextView(this).apply {
                text = (index + 1).toString()
                gravity = Gravity.CENTER
                includeFontPadding = false
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isClickable = true
                isFocusable = true
                contentDescription = "Signal ${index + 1}"
                setOnClickListener {
                    if (selectedSignalIndex != index) {
                        selectedSignalIndex = index
                        saveFunctionSettings()
                        updateSignalSelection(signalNumberViews)
                        playSelectedSound()
                    }
                }
            }
            signalNumberViews += number
            signalNumbers.addView(number, LinearLayout.LayoutParams(0, -1, 1f))
        }
        updateVolumeSelection(volumeBars)
        updateSignalSelection(signalNumberViews)

        screen.addView(clapper, ConstraintLayout.LayoutParams(dp(420), dp(420)).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = dp(64)
            bottomMargin = dp(48)
        })
        screen.addView(backButton, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        })
        screen.addView(volumeBand, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        })
        screen.addView(signalNumbers, ConstraintLayout.LayoutParams(1, 1).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        })

        setContentView(screen)
        screen.post {
            val scale = TimeClockUiConfig.referenceScale(screen.width, screen.height)
            fun px(value: Float) = TimeClockUiConfig.px(value, scale)

            (backButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = px(TimeClockUiConfig.fpsBack.width)
                height = px(TimeClockUiConfig.fpsBack.height)
                marginStart = px(TimeClockUiConfig.fpsBack.left ?: 0f)
                topMargin = px(TimeClockUiConfig.fpsBack.top ?: 0f)
            }.also { backButton.layoutParams = it }
            backButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(TimeClockUiConfig.fpsBack.textSize).toFloat())

            fun place(view: View, width: Float, height: Float, left: Float, top: Float) {
                (view.layoutParams as ConstraintLayout.LayoutParams).apply {
                    this.width = px(width)
                    this.height = px(height)
                    marginStart = px(left)
                    topMargin = px(top)
                }.also { view.layoutParams = it }
            }
            place(volumeBand, TimeClockUiConfig.fpsBand.width, 211f,
                TimeClockUiConfig.fpsBand.left ?: 67f, 300f)
            volumeBand.background = GradientDrawable().apply {
                setColor(Color.rgb(74, 74, 74))
                cornerRadius = px(32f).toFloat()
            }
            val maxBarHeight = px(169f)
            val minBarHeight = px(27f)
            volumeBars.forEachIndexed { index, bar ->
                val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * index / 9
                bar.layoutParams = FrameLayout.LayoutParams(px(48f), barHeight, Gravity.CENTER)
            }

            place(signalNumbers, TimeClockUiConfig.fpsBand.width, TimeClockUiConfig.fpsBand.height, 67f, 581f)
            signalNumbers.setPadding(px(35f), 0, px(35f), 0)
            signalNumbers.background = null
            signalNumberViews.forEach { number ->
                number.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(160f).toFloat())
            }
            updateVolumeSelection(volumeBars)
            updateSignalSelection(signalNumberViews)
        }
        startClapperBreathing(clapper)
    }

    private fun updateVolumeSelection(bars: List<View>) {
        val litBars = if (selectedVolumeLevel == 10) 10 else
            ((selectedVolumeLevel - 1) * bars.size / 9f).roundToInt()
        bars.forEachIndexed { index, bar ->
            val color = if (index < litBars) Color.WHITE else Color.BLACK
            (bar.background as? GradientDrawable)?.setColor(color)
        }
    }

    private fun updateSignalSelection(numbers: List<TextView>) {
        numbers.forEachIndexed { index, number ->
            number.setTextColor(if (index == selectedSignalIndex) Color.WHITE else Color.BLACK)
        }
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

    private companion object {
        const val PREFERENCES_NAME = "timeclock_functions"
        const val PREF_SIGNAL_INDEX = "signal_index"
        const val PREF_VOLUME_LEVEL = "volume_level"
        const val CLACK_FLASH_DURATION_MS = 100L
        const val VOLUME_IDLE_PREVIEW_DELAY_MS = 300L
    }
}
