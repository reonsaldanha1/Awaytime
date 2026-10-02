package com.example.awaytime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TypefaceSpan
import com.example.awaytime.MainActivity
import com.example.awaytime.R
import com.example.awaytime.data.AwayTimeManager
import com.example.awaytime.data.WidgetPreferences
import com.example.awaytime.model.WidgetFont

class AwayWidgetSmallProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.awaytime.ACTION_REFRESH_WIDGETS") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, AwayWidgetSmallProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        private const val TAG = "AwayWidgetSmall"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_away_small)
                val prefs = WidgetPreferences(context)
                val stats = AwayTimeManager.getTodayAwayStats(context)

                val isMinimal = prefs.isMinimalMode
                val accentColorInt = if (isMinimal) 0xFFFFFFFF.toInt() else prefs.accent.colorLong.toInt()

                // Background (Minimal pure black or Default rounded dark)
                val bgRes = if (isMinimal) R.drawable.bg_widget_small_minimal else R.drawable.bg_widget_small
                views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)

                // Font-styled text
                val font = prefs.widgetFont
                fun styleText(text: String): SpannableString {
                    val span = SpannableString(text)
                    span.setSpan(TypefaceSpan(font.family), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    return span
                }

                views.setTextViewText(R.id.tv_away_label, styleText("Away"))
                views.setTextViewText(R.id.tv_away_time, styleText(stats.formattedAwayTime()))

                if (isMinimal) {
                    views.setTextColor(R.id.tv_away_label, 0xFFAAAAAA.toInt())
                    views.setTextColor(R.id.tv_away_time, 0xFFFFFFFF.toInt())
                } else {
                    views.setTextColor(R.id.tv_away_label, 0xFF9EACB9.toInt())
                    views.setTextColor(R.id.tv_away_time, 0xFFFFFFFF.toInt())
                }

                // Update Timeline bar
                if (prefs.showTimeline) {
                    views.setViewVisibility(R.id.iv_timeline, View.VISIBLE)
                    val timelineBitmap = TimelineBitmapHelper.generateTimelineBar(
                        intervals = stats.intervals,
                        accentColorInt = accentColorInt,
                        bgContainerColor = if (isMinimal) 0xFF1C1D22.toInt() else 0xFF2A2B33.toInt()
                    )
                    views.setImageViewBitmap(R.id.iv_timeline, timelineBitmap)
                } else {
                    views.setViewVisibility(R.id.iv_timeline, View.GONE)
                }

                // Sparkle icon visibility and accent tint
                if (prefs.showSparkle) {
                    views.setViewVisibility(R.id.iv_sparkle, View.VISIBLE)
                    views.setInt(R.id.iv_sparkle, "setColorFilter", accentColorInt)
                } else {
                    views.setViewVisibility(R.id.iv_sparkle, View.GONE)
                }

                // Pending intent to open MainActivity
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Throwable) {
                Log.e(TAG, "Error updating Small Away Widget", e)
                try {
                    val fallbackViews = RemoteViews(context.packageName, R.layout.widget_away_small)
                    fallbackViews.setTextViewText(R.id.tv_away_time, "8h 19m")
                    appWidgetManager.updateAppWidget(appWidgetId, fallbackViews)
                } catch (e2: Throwable) {
                    Log.e(TAG, "Fatal error on fallback widget", e2)
                }
            }
        }
    }
}

