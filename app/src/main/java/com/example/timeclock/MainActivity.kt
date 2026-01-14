package com.example.timeclock

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // ================= UI =================
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var tvDate: TextView
    private lateinit var tvHour: TextView
    private lateinit var tvColon1: TextView
    private lateinit var tvMinute: TextView
    private lateinit var tvColon2: TextView
    private lateinit var tvSecond: TextView
    private lateinit var tvMillis: TextView
    private lateinit var tvZener: TextView
    private lateinit var btnInvert: TextView
    private lateinit var imgClapper: ImageView

    // ================= STATE =================
    private var isInverted = false
    private var lastDate = ""

    // ================= COLORS =================
    private val colorDateOrange = Color.parseColor("#FFA800")
    private val colorTimeYellow = Color.parseColor("#FFEA00")
    private val colorInvertBlue = Color.parseColor("#0033CC")
    private val colorGreen = Color.parseColor("#05D000")
    private val colorMagenta = Color.parseColor("#FF2BFF")
    private val colorWhite = Color.WHITE
    private val colorBlack = Color.BLACK

    // ================= FORMATTERS =================
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val hourFormat = SimpleDateFormat("HH", Locale.getDefault())
    private val minuteFormat = SimpleDateFormat("mm", Locale.getDefault())
    private val secondFormat = SimpleDateFormat("ss", Locale.getDefault())

    // ================= ZENER =================
    // ORDER FIXED
    private val zenerShapes = listOf(
        "⬤",   // BLACK LARGE CIRCLE — ⬤ (U+2B24)
        "▲",
        "✚",
        "▼",
        "★"
    )

    private val handler = Handler(Looper.getMainLooper())

    // ================= TIME LOOP =================
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
            }

            // Date (update only when changed)
            val currentDate = dateFormat.format(calendar.time)
            if (currentDate != lastDate) {
                tvDate.text = currentDate
                lastDate = currentDate
            }

            // Time
            tvHour.text = hourFormat.format(calendar.time)
            tvMinute.text = minuteFormat.format(calendar.time)
            tvSecond.text = secondFormat.format(calendar.time)

            // Milliseconds (00–99)
            val millis = ((now % 1000) / 10).toInt()
            tvMillis.text = String.format("%02d", millis)

            // Zener form
            val zenerIndex = (millis / 20) % zenerShapes.size
            tvZener.text = zenerShapes[zenerIndex]

            handler.postDelayed(this, 10)
        }
    }

    // ================= ANTI BURN-IN =================
    private val antiBurnInRunnable = object : Runnable {
        override fun run() {
            val dx = Random.nextInt(-3, 4).toFloat()
            val dy = Random.nextInt(-3, 4).toFloat()

            rootLayout.animate()
                .translationX(dx)
                .translationY(dy)
                .setDuration(800)
                .start()

            handler.postDelayed(this, 5 * 60 * 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen + keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContentView(R.layout.activity_main)

        // Bind UI
        rootLayout = findViewById(R.id.rootLayout)
        tvDate = findViewById(R.id.tvDate)
        tvHour = findViewById(R.id.tvHour)
        tvColon1 = findViewById(R.id.tvColon1)
        tvMinute = findViewById(R.id.tvMinute)
        tvColon2 = findViewById(R.id.tvColon2)
        tvSecond = findViewById(R.id.tvSecond)
        tvMillis = findViewById(R.id.tvMillis)
        tvZener = findViewById(R.id.tvZener)
        btnInvert = findViewById(R.id.btnInvert)
        imgClapper = findViewById(R.id.imgClapper)

        btnInvert.setOnClickListener { toggleInversion() }

        startClapperBreathing()

        handler.post(updateTimeRunnable)
        handler.postDelayed(antiBurnInRunnable, 5 * 60 * 1000L)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTimeRunnable)
        handler.removeCallbacks(antiBurnInRunnable)
    }

    // ================= INVERSION =================
    private fun toggleInversion() {
        isInverted = !isInverted

        if (isInverted) {
            rootLayout.setBackgroundColor(colorWhite)

            tvDate.setTextColor(colorInvertBlue)
            tvHour.setTextColor(colorInvertBlue)
            tvColon1.setTextColor(colorInvertBlue)
            tvMinute.setTextColor(colorInvertBlue)
            tvColon2.setTextColor(colorInvertBlue)
            tvSecond.setTextColor(colorInvertBlue)

            // millis + zener → MAGENTA
            tvMillis.setTextColor(colorMagenta)
            tvZener.setTextColor(colorMagenta)
            tvZener.alpha = 1f

            btnInvert.setTextColor(colorInvertBlue)

        } else {
            rootLayout.setBackgroundColor(colorBlack)

            tvDate.setTextColor(colorDateOrange)
            tvHour.setTextColor(colorTimeYellow)
            tvColon1.setTextColor(colorTimeYellow)
            tvMinute.setTextColor(colorTimeYellow)
            tvColon2.setTextColor(colorTimeYellow)
            tvSecond.setTextColor(colorTimeYellow)

            // millis + zener → GREEN
            tvMillis.setTextColor(colorGreen)
            tvZener.setTextColor(colorGreen)
            tvZener.alpha = 1f

            btnInvert.setTextColor(colorDateOrange)
        }
    }

    // ================= CLAPPER BREATHING =================
    private fun startClapperBreathing() {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.5f, 2.0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.5f, 2.0f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.10f, 0.20f)

        ObjectAnimator.ofPropertyValuesHolder(imgClapper, scaleX, scaleY, alpha).apply {
            duration = 6000
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }
}
