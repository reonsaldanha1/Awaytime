package com.example.awaytime.data

import android.content.Context
import android.content.SharedPreferences
import com.example.awaytime.model.WidgetAccent
import com.example.awaytime.model.WidgetFont
import com.example.awaytime.model.WidgetTheme

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("awaytime_widget_prefs", Context.MODE_PRIVATE)

    var theme: WidgetTheme
        get() {
            val name = prefs.getString("theme", WidgetTheme.PITCH_DARK.name) ?: WidgetTheme.PITCH_DARK.name
            return try { WidgetTheme.valueOf(name) } catch (e: Exception) { WidgetTheme.PITCH_DARK }
        }
        set(value) = prefs.edit().putString("theme", value.name).apply()

    var accent: WidgetAccent
        get() {
            val name = prefs.getString("accent", WidgetAccent.CYBER_BLUE.name) ?: WidgetAccent.CYBER_BLUE.name
            return try { WidgetAccent.valueOf(name) } catch (e: Exception) { WidgetAccent.CYBER_BLUE }
        }
        set(value) = prefs.edit().putString("accent", value.name).apply()

    var widgetFont: WidgetFont
        get() {
            val name = prefs.getString("widget_font", WidgetFont.SANS_SERIF.name) ?: WidgetFont.SANS_SERIF.name
            return try { WidgetFont.valueOf(name) } catch (e: Exception) { WidgetFont.SANS_SERIF }
        }
        set(value) = prefs.edit().putString("widget_font", value.name).apply()

    var isMinimalMode: Boolean
        get() = prefs.getBoolean("is_minimal_mode", false)
        set(value) = prefs.edit().putBoolean("is_minimal_mode", value).apply()

    var showTimeline: Boolean
        get() = prefs.getBoolean("show_timeline", true)
        set(value) = prefs.edit().putBoolean("show_timeline", value).apply()

    var showSparkle: Boolean
        get() = prefs.getBoolean("show_sparkle", true)
        set(value) = prefs.edit().putBoolean("show_sparkle", value).apply()

    var targetGoalHours: Int
        get() = prefs.getInt("target_goal_hours", 8)
        set(value) = prefs.edit().putInt("target_goal_hours", value).apply()

    var mockModeEnabled: Boolean
        get() = prefs.getBoolean("mock_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("mock_mode_enabled", value).apply()
}
