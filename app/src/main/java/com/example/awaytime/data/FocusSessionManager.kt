package com.example.awaytime.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import com.example.awaytime.service.FocusMonitorService

data class FocusAppInfo(
    val packageName: String,
    val appName: String,
    val isAllowed: Boolean,
    val isSystemEssential: Boolean = false
)

object FocusSessionManager {
    private const val PREFS_NAME = "awaytime_focus_prefs"
    private const val KEY_IS_FOCUS_ACTIVE = "is_focus_active"
    private const val KEY_FOCUS_END_TIME = "focus_end_time"
    private const val KEY_FOCUS_DURATION_MINUTES = "focus_duration_minutes"
    private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
    private const val KEY_COMPLETED_SESSIONS = "completed_sessions"
    private const val KEY_TOTAL_FOCUS_MINUTES = "total_focus_minutes"

    const val ACTION_FOCUS_STATE_CHANGED = "com.example.awaytime.ACTION_FOCUS_STATE_CHANGED"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFocusActive(context: Context): Boolean {
        val prefs = getPrefs(context)
        val active = prefs.getBoolean(KEY_IS_FOCUS_ACTIVE, false)
        if (!active) return false
        val endTime = prefs.getLong(KEY_FOCUS_END_TIME, 0L)
        if (System.currentTimeMillis() >= endTime) {
            // Expired
            stopFocusSession(context, completed = true)
            return false
        }
        return true
    }

    fun getFocusEndTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_FOCUS_END_TIME, 0L)
    }

    fun getFocusDurationMinutes(context: Context): Int {
        return getPrefs(context).getInt(KEY_FOCUS_DURATION_MINUTES, 25)
    }

    fun getRemainingMillis(context: Context): Long {
        val endTime = getFocusEndTime(context)
        return (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun getAllowedPackages(context: Context): Set<String> {
        val saved = getPrefs(context).getStringSet(KEY_ALLOWED_PACKAGES, null)
        if (saved != null) return saved
        // Default essential packages (e.g. Phone)
        return getDefaultEssentialPackages(context)
    }

    fun setAllowedPackages(context: Context, packages: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_ALLOWED_PACKAGES, packages).apply()
    }

    fun getDefaultEssentialPackages(context: Context): Set<String> {
        val result = mutableSetOf<String>()
        // Default dialer
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val dialer = telecomManager?.defaultDialerPackage
            if (!dialer.isNullOrBlank()) {
                result.add(dialer)
            }
        } catch (e: Exception) {}

        // Add common dialer packages as fallback
        result.add("com.google.android.dialer")
        result.add("com.samsung.android.dialer")
        result.add("com.android.phone")
        return result
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlaySettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {}
        }
    }

    fun startFocusSession(context: Context, durationMinutes: Int, allowedPackages: Set<String>) {
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        getPrefs(context).edit()
            .putBoolean(KEY_IS_FOCUS_ACTIVE, true)
            .putLong(KEY_FOCUS_END_TIME, endTime)
            .putInt(KEY_FOCUS_DURATION_MINUTES, durationMinutes)
            .putStringSet(KEY_ALLOWED_PACKAGES, allowedPackages)
            .apply()

        // Start Foreground monitoring service
        val intent = Intent(context, FocusMonitorService::class.java).apply {
            action = FocusMonitorService.ACTION_START_FOCUS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        notifyStateChanged(context)
    }

    fun stopFocusSession(context: Context, completed: Boolean = false) {
        val prefs = getPrefs(context)
        val wasActive = prefs.getBoolean(KEY_IS_FOCUS_ACTIVE, false)
        val duration = prefs.getInt(KEY_FOCUS_DURATION_MINUTES, 0)

        val editor = prefs.edit()
            .putBoolean(KEY_IS_FOCUS_ACTIVE, false)
            .putLong(KEY_FOCUS_END_TIME, 0L)

        if (completed && wasActive) {
            val completedCount = prefs.getInt(KEY_COMPLETED_SESSIONS, 0) + 1
            val totalMins = prefs.getLong(KEY_TOTAL_FOCUS_MINUTES, 0L) + duration
            editor.putInt(KEY_COMPLETED_SESSIONS, completedCount)
            editor.putLong(KEY_TOTAL_FOCUS_MINUTES, totalMins)
        }
        editor.apply()

        // Stop Foreground monitoring service
        val intent = Intent(context, FocusMonitorService::class.java).apply {
            action = FocusMonitorService.ACTION_STOP_FOCUS
        }
        context.startService(intent)

        notifyStateChanged(context)
    }

    fun getCompletedSessions(context: Context): Int {
        return getPrefs(context).getInt(KEY_COMPLETED_SESSIONS, 0)
    }

    fun getTotalFocusMinutes(context: Context): Long {
        return getPrefs(context).getLong(KEY_TOTAL_FOCUS_MINUTES, 0L)
    }

    private fun notifyStateChanged(context: Context) {
        val intent = Intent(ACTION_FOCUS_STATE_CHANGED).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    fun isAppAllowed(context: Context, packageName: String): Boolean {
        // Own app is ALWAYS allowed
        if (packageName == context.packageName) return true

        // System UI and Android system are always allowed
        if (packageName == "com.android.systemui" || packageName == "android") return true

        // Home launchers are always allowed so user can navigate
        val homeLaunchers = getHomeLaunchers(context)
        if (homeLaunchers.contains(packageName)) return true

        val allowed = getAllowedPackages(context)
        return allowed.contains(packageName)
    }

    fun getHomeLaunchers(context: Context): Set<String> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    fun getForegroundPackage(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        try {
            val events = usageStatsManager.queryEvents(now - 10000L, now)
            val event = UsageEvents.Event()
            var lastForeground: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastForeground = event.packageName
                }
            }
            if (!lastForeground.isNullOrBlank()) {
                return lastForeground
            }
        } catch (e: Exception) {}

        // Fallback: queryUsageStats
        try {
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60000L, now)
            return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {}

        return null
    }

    fun getInstalledLaunchableApps(context: Context): List<FocusAppInfo> {
        val pm = context.packageManager
        val allowedSet = getAllowedPackages(context)
        val defaultEssentials = getDefaultEssentialPackages(context)

        val result = mutableListOf<FocusAppInfo>()
        val seen = mutableSetOf<String>()

        // 1. Query all launcher intent activities (apps visible in launcher drawer)
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PackageManager.MATCH_ALL
            } else {
                0
            }
            val resolveList = pm.queryIntentActivities(mainIntent, flags)
            for (ri in resolveList) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (pkg == context.packageName) continue // Awaytime itself
                if (seen.contains(pkg)) continue
                seen.add(pkg)

                val appName = try {
                    ri.loadLabel(pm).toString().takeIf { it.isNotBlank() }
                } catch (e: Exception) { null } ?: try {
                    val ai = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(ai).toString()
                } catch (e2: Exception) { pkg }

                val isEssential = defaultEssentials.contains(pkg) ||
                        appName.contains("phone", ignoreCase = true) ||
                        appName.contains("call", ignoreCase = true) ||
                        pkg.contains("dialer", ignoreCase = true)

                result.add(
                    FocusAppInfo(
                        packageName = pkg,
                        appName = appName,
                        isAllowed = allowedSet.contains(pkg),
                        isSystemEssential = isEssential
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Comprehensive fallback: Query all installed applications that have a launch intent
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                val pkg = appInfo.packageName ?: continue
                if (pkg == context.packageName) continue
                if (seen.contains(pkg)) continue

                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    seen.add(pkg)
                    val appName = try {
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) { pkg }

                    val isEssential = defaultEssentials.contains(pkg) ||
                            appName.contains("phone", ignoreCase = true) ||
                            appName.contains("call", ignoreCase = true) ||
                            pkg.contains("dialer", ignoreCase = true)

                    result.add(
                        FocusAppInfo(
                            packageName = pkg,
                            appName = appName,
                            isAllowed = allowedSet.contains(pkg),
                            isSystemEssential = isEssential
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result.sortedWith(
            compareByDescending<FocusAppInfo> { it.isSystemEssential }
                .thenBy { it.appName.lowercase() }
        )
    }
}
