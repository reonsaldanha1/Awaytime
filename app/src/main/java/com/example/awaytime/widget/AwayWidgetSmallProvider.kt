package com.example.awaytime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.awaytime.MainActivity
import com.example.awaytime.R
import com.example.awaytime.data.AwayTimeManager
import com.example.awaytime.data.WidgetPreferences

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
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_away_small)
            val prefs = WidgetPreferences(context)
            val stats = AwayTimeManager.getTodayAwayStats(context)

            // Update Away Time Text (e.g. "8h 19m")
            views.setTextViewText(R.id.tv_away_time, stats.formattedAwayTime())

            // Update Timeline bar
            if (prefs.showTimeline) {
                views.setViewVisibility(R.id.iv_timeline, View.VISIBLE)
                val timelineBitmap = TimelineBitmapHelper.generateTimelineBar(
                    intervals = stats.intervals,
                    accentColorInt = prefs.accent.colorLong.toInt()
                )
                views.setImageViewBitmap(R.id.iv_timeline, timelineBitmap)
            } else {
                views.setViewVisibility(R.id.iv_timeline, View.GONE)
            }

            // Sparkle icon visibility
            views.setViewVisibility(R.id.iv_sparkle, if (prefs.showSparkle) View.VISIBLE else View.GONE)

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
        }
    }
}
