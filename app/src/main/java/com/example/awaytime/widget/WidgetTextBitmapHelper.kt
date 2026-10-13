package com.example.awaytime.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.res.ResourcesCompat

object WidgetTextBitmapHelper {

    /**
     * Renders text into a transparent bitmap using a custom font from resources.
     */
    fun renderTextToBitmap(
        context: Context,
        text: String,
        fontResId: Int,
        textSizeSp: Float = 24f,
        textColorInt: Int = Color.WHITE
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorInt
            textSize = textSizeSp * scaledDensity
            try {
                typeface = ResourcesCompat.getFont(context, fontResId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val textWidth = paint.measureText(text)
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        val padX = (4f * density).toInt()
        val padY = (2f * density).toInt()

        val width = (textWidth + (padX * 2)).toInt().coerceAtLeast(1)
        val height = (textHeight + (padY * 2)).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val baseline = padY - fontMetrics.ascent
        canvas.drawText(text, padX.toFloat(), baseline, paint)

        return bitmap
    }
}
