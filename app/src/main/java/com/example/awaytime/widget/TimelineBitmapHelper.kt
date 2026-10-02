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
        showCursor: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2A2B33.toInt()
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

        // Draw rounded container pill
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

        // If intervals are empty, draw typical alternating away blocks matching Image 1
        if (intervals.isEmpty()) {
            val sampleBlocks = listOf(
                Pair(0.18f, 0.26f),
                Pair(0.28f, 0.33f),
                Pair(0.35f, 0.42f),
                Pair(0.45f, 0.65f),
                Pair(0.68f, 0.73f)
            )
            for ((startRatio, endRatio) in sampleBlocks) {
                val left = width * startRatio
                val right = width * endRatio
                canvas.drawRect(left, 0f, right, height.toFloat(), awayPaint)
            }
        } else {
            for (interval in intervals) {
                if (interval.isAway) {
                    val startRatio = ((interval.startMillis - startOfDay).toFloat() / totalDayDuration).coerceIn(0f, 1f)
                    val endRatio = ((interval.endMillis - startOfDay).toFloat() / totalDayDuration).coerceIn(0f, 1f)
                    val left = width * startRatio
                    val right = width * endRatio
                    if (right > left) {
                        canvas.drawRect(left, 0f, right, height.toFloat(), awayPaint)
                    }
                }
            }
        }

        // Draw current time vertical cursor line (as in Image 1)
        if (showCursor) {
            val currentRatio = ((now - startOfDay).toFloat() / totalDayDuration).coerceIn(0f, 1f)
            val cursorX = width * (if (currentRatio > 0.1f) currentRatio else 0.72f)
            canvas.drawLine(cursorX, 4f, cursorX, height.toFloat() - 4f, cursorPaint)
        }

        return bitmap
    }
}
