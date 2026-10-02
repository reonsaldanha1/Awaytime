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
        height: Int = 260,
        accentColorInt: Int = Color.WHITE,
        fontFamily: String = "monospace"
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val tf = try {
            Typeface.create(fontFamily, Typeface.NORMAL)
        } catch (e: Exception) {
            Typeface.MONOSPACE
        }

        val count = points.size.coerceAtLeast(1)
        val horizontalPadding = width * 0.09f
        val chartWidth = width - (horizontalPadding * 2)
        val step = if (count > 1) chartWidth / (count - 1) else chartWidth

        val topMargin = height * 0.16f
        val bottomMargin = height * 0.28f
        val chartHeight = height - topMargin - bottomMargin
        val maxHours = 24f

        // 1. Target guide line across the chart at 18h (standard away detox goal)
        val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1FFFFFFF")
            strokeWidth = 1.8f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 8f), 0f)
        }
        val target18Y = (height - bottomMargin) - ((18f / maxHours) * chartHeight)
        canvas.drawLine(horizontalPadding * 0.5f, target18Y, width - (horizontalPadding * 0.5f), target18Y, guidePaint)

        // 2. Vertical guide tracks for each day
        val verticalTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#26FFFFFF")
            strokeWidth = 2.0f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(4f, 6f), 0f)
        }

        // 3. Line connecting the points for smooth visual flow
        val trendPath = android.graphics.Path()
        val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                70,
                Color.red(accentColorInt),
                Color.green(accentColorInt),
                Color.blue(accentColorInt)
            )
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }

        val dotCoords = mutableListOf<android.graphics.PointF>()

        for (i in points.indices) {
            val point = points[i]
            val x = horizontalPadding + (i * step)
            val normalizedY = (point.awayHours / maxHours).coerceIn(0.12f, 0.94f)
            val y = (height - bottomMargin) - (normalizedY * chartHeight)
            dotCoords.add(android.graphics.PointF(x, y))

            if (i == 0) {
                trendPath.moveTo(x, y)
            } else {
                trendPath.lineTo(x, y)
            }

            // Draw vertical track line
            canvas.drawLine(x, topMargin, x, height - bottomMargin, verticalTrackPaint)
        }

        // Draw trend line
        canvas.drawPath(trendPath, pathPaint)

        // 4. Dot paints: outer aura ring + inner core
        val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                55,
                Color.red(accentColorInt),
                Color.green(accentColorInt),
                Color.blue(accentColorInt)
            )
            style = Paint.Style.FILL
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }

        val dotCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#15171E")
            style = Paint.Style.FILL
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D0FFFFFF")
            textSize = 21f
            textAlign = Paint.Align.CENTER
            typeface = tf
        }

        val hourValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80FFFFFF")
            textSize = 15f
            textAlign = Paint.Align.CENTER
            typeface = tf
        }

        for (i in points.indices) {
            val point = points[i]
            val pt = dotCoords[i]

            // Glow ring
            canvas.drawCircle(pt.x, pt.y, 11f, outerGlowPaint)

            // Outer filled circle
            canvas.drawCircle(pt.x, pt.y, 6.5f, dotPaint)

            // Inner center dot for a high-precision futuristic / Nothing OS look
            canvas.drawCircle(pt.x, pt.y, 2.5f, dotCenterPaint)

            // Day label at bottom (e.g. 23, 24, 25...)
            canvas.drawText(point.dayNumber, pt.x, height - 12f, labelPaint)

            // Detail: subtle rounded hour value right above each dot (e.g. 19h, 18h)
            val hourInt = point.awayHours.toInt()
            canvas.drawText("${hourInt}h", pt.x, pt.y - 15f, hourValuePaint)
        }

        return bitmap
    }
}
