package com.example.awaytime.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.awaytime.MainActivity
import com.example.awaytime.R
import com.example.awaytime.data.FocusSessionManager
import com.example.awaytime.ui.focus.FocusBlockActivity

class FocusMonitorService : Service() {

    companion object {
        const val ACTION_START_FOCUS = "com.example.awaytime.ACTION_START_FOCUS"
        const val ACTION_STOP_FOCUS = "com.example.awaytime.ACTION_STOP_FOCUS"
        private const val CHANNEL_ID = "awaytime_focus_channel"
        private const val NOTIFICATION_ID = 9001
        private const val CHECK_INTERVAL_MS = 400L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayTimerTv: TextView? = null
    private var isOverlayAttached = false
    private var lastBlockedPkg: String? = null

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            checkCurrentAppAndEnforce()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_FOCUS) {
            stopFocusMonitoring()
            return START_NOT_STICKY
        }

        if (!FocusSessionManager.isFocusActive(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        if (!isRunning) {
            isRunning = true
            handler.post(checkRunnable)
        }

        return START_STICKY
    }

    private fun checkCurrentAppAndEnforce() {
        if (!FocusSessionManager.isFocusActive(this)) {
            stopFocusMonitoring()
            return
        }

        val fgPkg = FocusSessionManager.getForegroundPackage(this) ?: return
        val isAllowed = FocusSessionManager.isAppAllowed(this, fgPkg)

        if (!isAllowed) {
            // Restricted app detected!
            lastBlockedPkg = fgPkg
            showRestrictionOverlay(fgPkg)
        } else {
            // Allowed app or home or Awaytime
            if (isOverlayAttached) {
                hideRestrictionOverlay()
            }
            lastBlockedPkg = null
        }
    }

    private fun showRestrictionOverlay(packageName: String) {
        // 1. Try WindowManager overlay if permission granted
        if (FocusSessionManager.canDrawOverlays(this)) {
            if (!isOverlayAttached) {
                attachOverlayView()
            } else {
                updateOverlayContent()
            }
        }

        // 2. Also start FocusBlockActivity to guarantee restricted app is stopped
        try {
            val intent = Intent(this, FocusBlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("FocusMonitorService", "Error launching FocusBlockActivity", e)
        }
    }

    private fun attachOverlayView() {
        if (overlayView == null) {
            overlayView = createOverlayView()
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager?.addView(overlayView, params)
            isOverlayAttached = true
        } catch (e: Exception) {
            Log.e("FocusMonitorService", "Error adding overlay view", e)
            isOverlayAttached = false
        }
    }

    private fun updateOverlayContent() {
        val tvTimer = overlayTimerTv ?: return
        val remaining = FocusSessionManager.getRemainingMillis(this)
        val mins = (remaining / 1000L) / 60L
        val secs = (remaining / 1000L) % 60L
        tvTimer.text = String.format("%02d:%02d remaining", mins, secs)
    }

    private fun createOverlayView(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#FA0A0C10"))
            isClickable = true
            isFocusable = true
            // Tapping background navigates Home so user isn't stuck
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        }

        val centerLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = lp
        }

        // Icon
        val ivIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_hourglass)
            setColorFilter(Color.parseColor("#4DA2FF"))
            val iconLp = android.widget.LinearLayout.LayoutParams(160, 160).apply {
                bottomMargin = 48
            }
            layoutParams = iconLp
        }
        centerLayout.addView(ivIcon)

        // Title: PATIENCE IS THE KEY TO SUCCESS (NO BUTTONS on this page)
        val tvTitle = TextView(this).apply {
            text = "PATIENCE IS THE KEY TO SUCCESS"
            setTextColor(Color.WHITE)
            textSize = 21f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            letterSpacing = 0.05f
            val titleLp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            layoutParams = titleLp
        }
        centerLayout.addView(tvTitle)

        // Timer
        val remaining = FocusSessionManager.getRemainingMillis(this)
        val mins = (remaining / 1000L) / 60L
        val secs = (remaining / 1000L) % 60L
        val tvTimer = TextView(this).apply {
            text = String.format("%02d:%02d remaining", mins, secs)
            setTextColor(Color.parseColor("#69E094"))
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val timerLp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
            layoutParams = timerLp
        }
        overlayTimerTv = tvTimer
        centerLayout.addView(tvTimer)

        // Subtitle
        val tvSub = TextView(this).apply {
            text = "The only way to stop this focus timer is through the Awaytime app."
            setTextColor(Color.parseColor("#9AA0B2"))
            textSize = 13f
            gravity = Gravity.CENTER
            setLineSpacing(8f, 1f)
        }
        centerLayout.addView(tvSub)

        root.addView(centerLayout)
        return root
    }

    private fun hideRestrictionOverlay() {
        if (isOverlayAttached && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e("FocusMonitorService", "Error removing overlay view", e)
            }
            isOverlayAttached = false
        }
    }

    private fun stopFocusMonitoring() {
        isRunning = false
        handler.removeCallbacks(checkRunnable)
        hideRestrictionOverlay()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFocusMonitoring()
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hourglass)
            .setContentTitle("Focus Session Active")
            .setContentText("PATIENCE IS THE KEY TO SUCCESS • Only allowed apps can be opened")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Mode Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background enforcement for active Focus sessions"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }
}
