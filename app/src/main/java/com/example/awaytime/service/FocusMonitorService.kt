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
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.awaytime.MainActivity
import com.example.awaytime.R
import com.example.awaytime.data.AppTimerManager
import com.example.awaytime.data.DistractionManager
import com.example.awaytime.data.FocusSessionManager
import com.example.awaytime.ui.focus.AppLimitBlockActivity
import com.example.awaytime.ui.focus.FocusBlockActivity

class FocusMonitorService : Service() {

    enum class OverlayMode {
        FOCUS,
        APP_LIMIT,
        DISTRACTION_LOCK
    }

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
    private var overlayTitleTv: TextView? = null
    private var overlaySubTv: TextView? = null
    private var overlayIconIv: ImageView? = null
    private var isOverlayAttached = false
    private var lastBlockedPkg: String? = null
    private var currentMode = OverlayMode.FOCUS

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
        val hasTimers = AppTimerManager.hasAnyTimer(this)
        val focusActive = FocusSessionManager.isFocusActive(this)
        val hasDistractions = DistractionManager.getBlockedPackages(this).isNotEmpty()

        if (action == ACTION_STOP_FOCUS) {
            if (!hasTimers && !focusActive && !hasDistractions) {
                stopFocusMonitoring()
                return START_NOT_STICKY
            }
        }

        if (!focusActive && !hasTimers && !hasDistractions) {
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
        val focusActive = FocusSessionManager.isFocusActive(this)
        val hasTimers = AppTimerManager.hasAnyTimer(this)
        val blockedDistractions = DistractionManager.getBlockedPackages(this)

        if (!focusActive && !hasTimers && blockedDistractions.isEmpty()) {
            stopFocusMonitoring()
            return
        }

        val fgPkg = FocusSessionManager.getForegroundPackage(this) ?: return

        // Always allow own app, system UI, Android system, and launchers
        if (fgPkg == packageName || fgPkg == "com.android.systemui" || fgPkg == "android") {
            if (isOverlayAttached) hideRestrictionOverlay()
            lastBlockedPkg = null
            return
        }
        if (FocusSessionManager.getHomeLaunchers(this).contains(fgPkg)) {
            if (isOverlayAttached) hideRestrictionOverlay()
            lastBlockedPkg = null
            return
        }

        // 1. Focus Session Check
        if (focusActive) {
            val isAllowed = FocusSessionManager.isAppAllowed(this, fgPkg)
            if (!isAllowed) {
                lastBlockedPkg = fgPkg
                showFocusRestrictionOverlay(fgPkg)
                return
            }
        }

        // 2. Distraction Page Locked/Blocked Apps Check
        if (blockedDistractions.contains(fgPkg)) {
            lastBlockedPkg = fgPkg
            showDistractionLockRestriction(fgPkg)
            return
        }

        // 3. App Timer Check (daily limit enforcement)
        if (hasTimers) {
            val timerMin = AppTimerManager.getTimerMinutes(this, fgPkg)
            if (timerMin > 0) {
                val usedMillis = AppTimerManager.getTodayUsageForPackage(this, fgPkg)
                if (usedMillis >= (timerMin * 60_000L)) {
                    lastBlockedPkg = fgPkg
                    showAppLimitRestriction(fgPkg, timerMin, usedMillis)
                    return
                }
            }
        }

        // Allowed app
        if (isOverlayAttached) {
            hideRestrictionOverlay()
        }
        lastBlockedPkg = null
    }

    private fun showFocusRestrictionOverlay(packageName: String) {
        if (FocusSessionManager.canDrawOverlays(this)) {
            if (!isOverlayAttached) {
                attachOverlayView(OverlayMode.FOCUS)
            } else {
                updateOverlayContent(OverlayMode.FOCUS)
            }
        } else {
            kickToHomeAndNotify(packageName, isDistractionLock = false, timerMin = 0, usedMillis = 0, isFocus = true)
        }

        try {
            val intent = Intent(this, FocusBlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("FocusMonitorService", "Error launching FocusBlockActivity", e)
        }
    }

    private fun showDistractionLockRestriction(packageName: String) {
        if (FocusSessionManager.canDrawOverlays(this)) {
            if (!isOverlayAttached) {
                attachOverlayView(OverlayMode.DISTRACTION_LOCK)
            } else {
                updateOverlayContent(OverlayMode.DISTRACTION_LOCK)
            }
        } else {
            kickToHomeAndNotify(packageName, isDistractionLock = true, timerMin = 0, usedMillis = 0, isFocus = false)
        }

        try {
            val intent = Intent(this, AppLimitBlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AppLimitBlockActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AppLimitBlockActivity.EXTRA_IS_DISTRACTION_LOCK, true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("FocusMonitorService", "Error launching AppLimitBlockActivity", e)
        }
    }

    private fun showAppLimitRestriction(packageName: String, timerMin: Int, usedMillis: Long) {
        if (FocusSessionManager.canDrawOverlays(this)) {
            if (!isOverlayAttached) {
                attachOverlayView(OverlayMode.APP_LIMIT)
            } else {
                updateOverlayContent(OverlayMode.APP_LIMIT)
            }
        } else {
            kickToHomeAndNotify(packageName, isDistractionLock = false, timerMin = timerMin, usedMillis = usedMillis, isFocus = false)
        }

        try {
            val intent = Intent(this, AppLimitBlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AppLimitBlockActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AppLimitBlockActivity.EXTRA_TIMER_MINUTES, timerMin)
                putExtra(AppLimitBlockActivity.EXTRA_USED_MILLIS, usedMillis)
                putExtra(AppLimitBlockActivity.EXTRA_IS_DISTRACTION_LOCK, false)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("FocusMonitorService", "Error launching AppLimitBlockActivity", e)
        }
    }

    private fun kickToHomeAndNotify(
        packageName: String,
        isDistractionLock: Boolean,
        timerMin: Int,
        usedMillis: Long,
        isFocus: Boolean
    ) {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (e: Exception) {}

        try {
            val appLabel = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            } catch (e: Exception) {
                packageName
            }

            val title = when {
                isFocus -> "Focus Active • Access Restricted"
                isDistractionLock -> "🚫 $appLabel is Locked"
                else -> "⏳ $appLabel Daily Limit Reached"
            }
            val body = when {
                isFocus -> "Only essential apps allowed during Focus mode."
                isDistractionLock -> "This app is locked in Distractions. Tap to view."
                else -> "Daily limit of ${AppTimerManager.formatTimerMinutes(timerMin)} reached. Tap to view."
            }

            val openIntent = if (isFocus) {
                Intent(this, FocusBlockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } else {
                Intent(this, AppLimitBlockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AppLimitBlockActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(AppLimitBlockActivity.EXTRA_TIMER_MINUTES, timerMin)
                    putExtra(AppLimitBlockActivity.EXTRA_USED_MILLIS, usedMillis)
                    putExtra(AppLimitBlockActivity.EXTRA_IS_DISTRACTION_LOCK, isDistractionLock)
                }
            }

            val pi = PendingIntent.getActivity(
                this,
                packageName.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alertNotif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(if (isFocus) R.drawable.ic_hourglass else R.drawable.ic_timer)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(10000 + (packageName.hashCode() and 0x7FFF), alertNotif)
        } catch (e: Exception) {}
    }

    private fun attachOverlayView(mode: OverlayMode) {
        currentMode = mode
        if (overlayView == null) {
            overlayView = createOverlayView(mode)
        } else {
            updateOverlayContent(mode)
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

    private fun updateOverlayContent(mode: OverlayMode) {
        currentMode = mode
        val tvTitle = overlayTitleTv ?: return
        val tvTimer = overlayTimerTv ?: return
        val ivIcon = overlayIconIv

        when (mode) {
            OverlayMode.FOCUS -> {
                ivIcon?.setImageResource(R.drawable.ic_hourglass)
                ivIcon?.setColorFilter(Color.parseColor("#4DA2FF"))
                tvTitle.text = "PATIENCE IS THE KEY TO SUCCESS"
                val remaining = FocusSessionManager.getRemainingMillis(this)
                val mins = (remaining / 1000L) / 60L
                val secs = (remaining / 1000L) % 60L
                tvTimer.text = String.format("%02d:%02d remaining", mins, secs)
                tvTimer.setTextColor(Color.parseColor("#69E094"))
            }
            OverlayMode.DISTRACTION_LOCK -> {
                ivIcon?.setImageResource(R.drawable.ic_timer)
                ivIcon?.setColorFilter(Color.parseColor("#FF5252"))
                tvTitle.text = "APP LOCKED"
                tvTimer.text = "This app is locked by Awaytime to prevent distractions"
                tvTimer.setTextColor(Color.parseColor("#FF8A65"))
            }
            OverlayMode.APP_LIMIT -> {
                ivIcon?.setImageResource(R.drawable.ic_timer)
                ivIcon?.setColorFilter(Color.parseColor("#FF5252"))
                tvTitle.text = "DAILY LIMIT REACHED"
                tvTimer.text = "Daily screen time limit reached • Resets at midnight"
                tvTimer.setTextColor(Color.parseColor("#FF8A65"))
            }
        }
    }

    private fun createOverlayView(mode: OverlayMode): View {
        currentMode = mode
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F5000000"))
            isClickable = true
            isFocusable = true
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
            setImageResource(if (mode == OverlayMode.FOCUS) R.drawable.ic_hourglass else R.drawable.ic_timer)
            setColorFilter(Color.parseColor(if (mode == OverlayMode.FOCUS) "#4DA2FF" else "#FF5252"))
            val iconLp = android.widget.LinearLayout.LayoutParams(160, 160).apply {
                bottomMargin = 44
            }
            layoutParams = iconLp
        }
        overlayIconIv = ivIcon
        centerLayout.addView(ivIcon)

        // Title
        val tvTitle = TextView(this).apply {
            text = when (mode) {
                OverlayMode.FOCUS -> "PATIENCE IS THE KEY TO SUCCESS"
                OverlayMode.DISTRACTION_LOCK -> "APP LOCKED"
                OverlayMode.APP_LIMIT -> "DAILY LIMIT REACHED"
            }
            setTextColor(Color.WHITE)
            textSize = 21f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            letterSpacing = 0.05f
            val titleLp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            layoutParams = titleLp
        }
        overlayTitleTv = tvTitle
        centerLayout.addView(tvTitle)

        // Subtitle / Timer
        val tvTimer = TextView(this).apply {
            when (mode) {
                OverlayMode.FOCUS -> {
                    val remaining = FocusSessionManager.getRemainingMillis(this@FocusMonitorService)
                    val mins = (remaining / 1000L) / 60L
                    val secs = (remaining / 1000L) % 60L
                    text = String.format("%02d:%02d remaining", mins, secs)
                    setTextColor(Color.parseColor("#69E094"))
                }
                OverlayMode.DISTRACTION_LOCK -> {
                    text = "This app is locked by Awaytime to prevent distractions"
                    setTextColor(Color.parseColor("#FF8A65"))
                }
                OverlayMode.APP_LIMIT -> {
                    text = "Daily screen time limit reached • Resets at midnight"
                    setTextColor(Color.parseColor("#FF8A65"))
                }
            }
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val timerLp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 40
            }
            layoutParams = timerLp
        }
        overlayTimerTv = tvTimer
        centerLayout.addView(tvTimer)

        // Close & Return to Home button
        val btnHome = android.widget.Button(this).apply {
            text = "Close & Go to Home"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.parseColor("#FF5252"))
            setPadding(48, 20, 48, 20)
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        }
        centerLayout.addView(btnHome)

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

        val isFocus = FocusSessionManager.isFocusActive(this)
        val title = if (isFocus) "Focus Session Active" else "Awaytime App Limits & Locks Active"
        val content = if (isFocus) "PATIENCE IS THE KEY TO SUCCESS • Only allowed apps can be opened" else "Monitoring daily app timers and blocking locked apps"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (isFocus) R.drawable.ic_hourglass else R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus & App Limit Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background enforcement for active Focus sessions, Distraction locks, and daily App limits"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }
}
