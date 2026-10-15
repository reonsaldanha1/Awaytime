package com.example.awaytime.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.awaytime.service.FocusMonitorService
import java.util.Calendar

data class AppTimerItem(
    val packageName: String,
    val appName: String,
    val usageTodayMillis: Long,
    val timerMinutes: Int,
    val iconDrawable: Drawable? = null
) {
    val isTimerSet: Boolean get() = timerMinutes > 0
    val isLimitReached: Boolean get() = isTimerSet && usageTodayMillis >= (timerMinutes * 60_000L)
    val remainingMillis: Long get() = if (isTimerSet) ((timerMinutes * 60_000L) - usageTodayMillis).coerceAtLeast(0L) else 0L

    fun formattedUsage(): String {
        val totalSecs = usageTodayMillis / 1000L
        val hours = totalSecs / 3600L
        val mins = (totalSecs % 3600L) / 60L
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            mins > 0 -> "${mins}m"
            else -> "<1m"
        }
    }

    fun formattedRemaining(): String {
        if (!isTimerSet) return ""
        if (isLimitReached) return "Limit reached"
        val totalSecs = remainingMillis / 1000L
        val hours = totalSecs / 3600L
        val mins = (totalSecs % 3600L) / 60L
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m left"
            hours > 0 -> "${hours}h left"
            mins > 0 -> "${mins}m left"
            else -> "<1m left"
        }
    }
}

object AppTimerManager {
    private const val PREFS_NAME = "awaytime_app_timers"
    private const val KEY_PREFIX = "timer_min_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTimerMinutes(context: Context, packageName: String): Int {
        return getPrefs(context).getInt(KEY_PREFIX + packageName, 0)
    }

    fun setTimerMinutes(context: Context, packageName: String, minutes: Int) {
        if (minutes <= 0) {
            removeTimer(context, packageName)
        } else {
            getPrefs(context).edit().putInt(KEY_PREFIX + packageName, minutes).apply()
            startOrUpdateMonitoring(context)
        }
    }

    fun removeTimer(context: Context, packageName: String) {
        getPrefs(context).edit().remove(KEY_PREFIX + packageName).apply()
        startOrUpdateMonitoring(context)
    }

    fun getAllTimers(context: Context): Map<String, Int> {
        val all = getPrefs(context).all
        val result = mutableMapOf<String, Int>()
        for ((key, value) in all) {
            if (key.startsWith(KEY_PREFIX) && value is Int && value > 0) {
                result[key.removePrefix(KEY_PREFIX)] = value
            }
        }
        return result
    }

    fun hasAnyTimer(context: Context): Boolean {
        return getAllTimers(context).isNotEmpty()
    }

    fun isTimerReached(context: Context, packageName: String, usageMillis: Long): Boolean {
        val timerMin = getTimerMinutes(context, packageName)
        if (timerMin <= 0) return false
        return usageMillis >= (timerMin * 60_000L)
    }

    fun formatTimerMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    private var lastUsageCheckTime = 0L
    private val usageCache = mutableMapOf<String, Long>()

    fun invalidateUsageCache() {
        usageCache.clear()
        lastUsageCheckTime = 0L
    }

    fun getTodayUsageForPackage(context: Context, packageName: String): Long {
        val now = System.currentTimeMillis()
        if (now - lastUsageCheckTime < 2500L && usageCache.containsKey(packageName)) {
            return usageCache[packageName] ?: 0L
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val usageMap = AwayTimeManager.getForegroundUsageByPackage(context, startOfDay, now)
        val duration = usageMap[packageName] ?: 0L

        usageCache.clear()
        usageCache.putAll(usageMap)
        lastUsageCheckTime = now
        return duration
    }

    fun getInstalledAppsWithUsage(context: Context): List<AppTimerItem> {
        val pm = context.packageManager
        val timers = getAllTimers(context)

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val usageMap = AwayTimeManager.getForegroundUsageByPackage(context, startOfDay, now)

        // Query all launchable apps
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val seen = mutableSetOf<String>()
        val result = mutableListOf<AppTimerItem>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || pkg == "com.android.systemui") continue
            if (!seen.add(pkg)) continue

            val appName = try {
                info.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }

            val icon = try {
                info.loadIcon(pm)
            } catch (e: Exception) {
                null
            }

            val usedToday = usageMap[pkg] ?: 0L
            val timerMin = timers[pkg] ?: 0

            result.add(
                AppTimerItem(
                    packageName = pkg,
                    appName = appName,
                    usageTodayMillis = usedToday,
                    timerMinutes = timerMin,
                    iconDrawable = icon
                )
            )
        }

        // Sort: Apps with limit set first, then by usage today descending, then alphabetically
        return result.sortedWith(
            compareByDescending<AppTimerItem> { it.isLimitReached }
                .thenByDescending { it.isTimerSet }
                .thenByDescending { it.usageTodayMillis }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun startOrUpdateMonitoring(context: Context) {
        val hasTimers = hasAnyTimer(context)
        val isFocusActive = FocusSessionManager.isFocusActive(context)
        val hasBlockedDistractions = DistractionManager.getBlockedPackages(context).isNotEmpty()

        if (hasTimers || isFocusActive || hasBlockedDistractions) {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                action = FocusMonitorService.ACTION_START_FOCUS
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {}
        } else {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                action = FocusMonitorService.ACTION_STOP_FOCUS
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {}
        }
    }
}
