package com.example.awaytime.data

import android.content.Context
import android.content.SharedPreferences

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
        }
    }

    fun removeTimer(context: Context, packageName: String) {
        getPrefs(context).edit().remove(KEY_PREFIX + packageName).apply()
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

    fun isTimerReached(context: Context, packageName: String, usageMillis: Long): Boolean {
        val timerMin = getTimerMinutes(context, packageName)
        if (timerMin <= 0) return false
        return usageMillis >= (timerMin * 60_000L)
    }

    fun formatTimerMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours} h ${mins} m"
            hours > 0 -> "${hours} h"
            else -> "${mins} m"
        }
    }
}
