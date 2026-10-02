package com.example.awaytime.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import com.example.awaytime.model.DayPoint

object WeeklyChartBitmapHelper {

    fun generateWeeklyChart(
        points: List<DayPoint>,
        width: Int = 480,
        height: Int = 260
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val dottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40FFFFFF")
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(4f, 6f), 0f)
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C7FFFFFF")
            textSize = 22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }

        val count = points.size.coerceAtLeast(1)
        val horizontalPadding = width * 0.08f
        val chartWidth = width - (horizontalPadding * 2)
        val step = chartWidth / (count - 1)

        val topMargin = height * 0.15f
        val bottomMargin = height * 0.22f
        val chartHeight = height - topMargin - bottomMargin
        val maxHours = 24f

        for (i in points.indices) {
            val point = points[i]
            val x = horizontalPadding + (i * step)

            // Draw vertical dotted line
            canvas.drawLine(x, topMargin, x, height - bottomMargin, dottedPaint)

            // Draw floating data dot
            val normalizedY = (point.awayHours / maxHours).coerceIn(0.1f, 0.95f)
            val y = (height - bottomMargin) - (normalizedY * chartHeight)
            canvas.drawCircle(x, y, 6.5f, dotPaint)

            // Draw day label text at bottom (e.g. 6, 7, 8, 9, 10, 11, 12)
            canvas.drawText(point.dayNumber, x, height - 10f, labelPaint)
        }

        return bitmap
    }
}
