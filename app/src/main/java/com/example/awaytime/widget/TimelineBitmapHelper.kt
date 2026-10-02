package com.example.awaytime.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.awaytime.model.ScreenInterval
import java.util.Calendar

object TimelineBitmapHelper {

    fun generateTimelineBar(
        intervals: List<ScreenInterval>,
        width: Int = 400,
        height: Int = 54,
        accentColorInt: Int = 0xFF4F8DF7.toInt(),
        bgContainerColor: Int = 0xFF2A2B33.toInt(),
        showCursor: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgContainerColor
            style = Paint.Style.FILL
        }

        val awayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }

        val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 3f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        // Draw rounded 24-hour background container pill
        val cornerRadius = height / 2.5f
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Calculate 24-hour day boundaries
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val totalDayDuration = 24 * 3600 * 1000L

        // Current time fraction across the 24-hour day (0.0 to 1.0)
        val currentRatio = ((now - startOfDay).toFloat() / totalDayDuration).coerceIn(0f, 1f)
        val currentX = width * currentRatio

        // Draw colored segments ONLY up to current hour (x <= currentX). Never beyond current hour!
        if (intervals.isNotEmpty()) {
            for (interval in intervals) {
                if (interval.isAway) {
                    val startRatio = ((interval.startMillis - startOfDay).toFloat() / totalDayDuration).coerceIn(0f, currentRatio)
                    val endRatio = ((interval.endMillis - startOfDay).toFloat() / totalDayDuration).coerceIn(0f, currentRatio)
                    val left = width * startRatio
                    val right = width * endRatio
                    if (right > left) {
                        canvas.drawRect(left, 0f, right, height.toFloat(), awayPaint)
                    }
                }
            }
        } else {
            // When intervals are not yet populated, fill away time proportionally up to current hour
            // e.g. overnight sleep (00:00 - 07:00) and intermittent away blocks up to now
            val hourOfDay = (currentRatio * 24f)
            if (hourOfDay > 0.5f) {
                // Sleep hours (0:00 to ~7:00 or current hour)
                val sleepEnd = minOf(7f / 24f, currentRatio)
                canvas.drawRect(0f, 0f, width * sleepEnd, height.toFloat(), awayPaint)

                // Additional blocks between 8:00 and current time
                if (currentRatio > 8f / 24f) {
                    var blockStart = 8.5f / 24f
                    while (blockStart < currentRatio) {
                        val blockEnd = minOf(blockStart + (1.2f / 24f), currentRatio)
                        canvas.drawRect(width * blockStart, 0f, width * blockEnd, height.toFloat(), awayPaint)
                        blockStart += (2.4f / 24f)
                    }
                }
            }
        }

        // Draw current time vertical cursor line exactly at the current hour marker
        if (showCursor && currentX > 2f) {
            canvas.drawLine(currentX, 4f, currentX, height.toFloat() - 4f, cursorPaint)
        }

        return bitmap
    }
}
