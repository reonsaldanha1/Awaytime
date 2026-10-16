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
        showLines: Boolean = true,
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

        val dividerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgContainerColor
            strokeWidth = 2f
            style = Paint.Style.STROKE
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
        val containerPath = android.graphics.Path().apply {
            addRoundRect(rect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Clip everything inside the rounded container
        canvas.save()
        canvas.clipPath(containerPath)

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

        if (!showLines) {
            // Option to disable various lines in the widget bar:
            // Renders a smooth, seamless solid progress fill without interval cuts, divider lines, or cursor line
            val totalAwayMillis = if (intervals.isNotEmpty()) {
                intervals.filter { it.isAway }.sumOf {
                    val s = it.startMillis.coerceIn(startOfDay, now)
                    val e = it.endMillis.coerceIn(startOfDay, now)
                    (e - s).coerceAtLeast(0L)
                }
            } else {
                ((now - startOfDay) * 3 / 4).coerceAtLeast(0L)
            }
            val awayRatio = (totalAwayMillis.toFloat() / totalDayDuration).coerceIn(0f, 1f)
            val solidRight = width * awayRatio.coerceAtLeast(currentRatio * 0.5f).coerceAtMost(currentRatio)
            if (solidRight > 0f) {
                canvas.drawRect(0f, 0f, solidRight, height.toFloat(), awayPaint)
            }
        } else {
            // Segmented intervals with divider lines and cursor
            if (intervals.isNotEmpty()) {
                for (interval in intervals) {
                    if (interval.isAway) {
                        val startRatio = ((interval.startMillis - startOfDay).toFloat() / totalDayDuration).coerceIn(0f, currentRatio)
                        val endRatio = ((interval.endMillis - startOfDay).toFloat() / totalDayDuration).coerceIn(0f, currentRatio)
                        val left = width * startRatio
                        val right = width * endRatio
                        if (right > left) {
                            canvas.drawRect(left, 0f, right, height.toFloat(), awayPaint)
                            // Clean divider line at boundary
                            if (left > 1f) {
                                canvas.drawLine(left, 0f, left, height.toFloat(), dividerLinePaint)
                            }
                            if (right < currentX - 1f) {
                                canvas.drawLine(right, 0f, right, height.toFloat(), dividerLinePaint)
                            }
                        }
                    }
                }
            } else {
                // When intervals are not yet populated, fill away time proportionally up to current hour
                val hourOfDay = (currentRatio * 24f)
                if (hourOfDay > 0.5f) {
                    val sleepEnd = minOf(7f / 24f, currentRatio)
                    canvas.drawRect(0f, 0f, width * sleepEnd, height.toFloat(), awayPaint)

                    if (currentRatio > 8f / 24f) {
                        var blockStart = 8.5f / 24f
                        while (blockStart < currentRatio) {
                            val blockEnd = minOf(blockStart + (1.2f / 24f), currentRatio)
                            canvas.drawRect(width * blockStart, 0f, width * blockEnd, height.toFloat(), awayPaint)
                            canvas.drawLine(width * blockStart, 0f, width * blockStart, height.toFloat(), dividerLinePaint)
                            blockStart += (2.4f / 24f)
                        }
                    }
                }
            }

            // Draw current time vertical cursor line exactly at the current hour marker
            if (showCursor && currentX > 2f) {
                canvas.drawLine(currentX, 4f, currentX, height.toFloat() - 4f, cursorPaint)
            }
        }

        canvas.restore()
        return bitmap
    }
}
