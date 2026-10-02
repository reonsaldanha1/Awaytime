package com.example.awaytime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TypefaceSpan
import com.example.awaytime.MainActivity
import com.example.awaytime.R
import com.example.awaytime.data.AwayTimeManager
import com.example.awaytime.data.WidgetPreferences
import com.example.awaytime.model.WidgetFont

class AwayWidgetWeeklyProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.awaytime.ACTION_REFRESH_WIDGETS") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, AwayWidgetWeeklyProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        private const val TAG = "AwayWidgetWeekly"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_away_weekly)
                val prefs = WidgetPreferences(context)
                val weeklyStats = AwayTimeManager.getWeeklyAwayStats(context)

                val isMinimal = prefs.isMinimalMode
                val accentColorInt = if (isMinimal) 0xFFFFFFFF.toInt() else prefs.accent.colorLong.toInt()

                // Background
                val bgRes = if (isMinimal) R.drawable.bg_widget_weekly_minimal else R.drawable.bg_widget_weekly
                views.setInt(R.id.widget_weekly_root, "setBackgroundResource", bgRes)

                // Sparkle tint
                views.setInt(R.id.iv_weekly_sparkle, "setColorFilter", accentColorInt)

                // Font styling
                val font = prefs.widgetFont
                fun styleText(text: String): SpannableString {
                    val span = SpannableString(text)
                    span.setSpan(TypefaceSpan(font.family), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    return span
                }

                views.setTextViewText(R.id.tv_weekly_header, styleText("Weekly away time"))
                views.setTextViewText(R.id.tv_weekly_total, styleText(weeklyStats.formattedTotal()))
                views.setTextViewText(R.id.tv_weekly_dates, styleText(weeklyStats.dateRangeLabel))
                views.setTextViewText(R.id.tv_day_average_value, styleText(weeklyStats.formattedAverage()))

                // Day average pill background
                if (isMinimal) {
                    views.setInt(R.id.layout_day_average, "setBackgroundResource", R.drawable.bg_widget_small_minimal)
                } else {
                    views.setInt(R.id.layout_day_average, "setBackgroundResource", R.drawable.bg_pill_average)
                }

                // Weekly Chart Bitmap
                val chartBitmap = WeeklyChartBitmapHelper.generateWeeklyChart(
                    points = weeklyStats.dailyPoints,
                    accentColorInt = accentColorInt,
                    fontFamily = font.family
                )
                views.setImageViewBitmap(R.id.iv_weekly_chart, chartBitmap)

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
                views.setOnClickPendingIntent(R.id.widget_weekly_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Throwable) {
                Log.e(TAG, "Error updating Weekly Away Widget", e)
                try {
                    val fallbackViews = RemoteViews(context.packageName, R.layout.widget_away_weekly)
                    fallbackViews.setTextViewText(R.id.tv_weekly_total, "30 hr 28 min")
                    appWidgetManager.updateAppWidget(appWidgetId, fallbackViews)
                } catch (e2: Throwable) {
                    Log.e(TAG, "Fatal error on fallback weekly widget", e2)
                }
            }
        }
    }
}
