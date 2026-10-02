package com.example.awaytime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.awaytime.MainActivity
import com.example.awaytime.R
import com.example.awaytime.data.AwayTimeManager

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
                val weeklyStats = AwayTimeManager.getWeeklyAwayStats(context)

                // Total weekly away time e.g. "30 hr 28 min"
                views.setTextViewText(R.id.tv_weekly_total, weeklyStats.formattedTotal())

                // Date Range
                views.setTextViewText(R.id.tv_weekly_dates, weeklyStats.dateRangeLabel)

                // Day Average e.g. "11 hr 19 min"
                views.setTextViewText(R.id.tv_day_average_value, weeklyStats.formattedAverage())

                // Weekly Chart Bitmap
                val chartBitmap = WeeklyChartBitmapHelper.generateWeeklyChart(
                    points = weeklyStats.dailyPoints
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
