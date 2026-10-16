package com.example.awaytime.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

object WidgetSparkleBitmapHelper {

    /**
     * Renders a crisp, anti-aliased 8-point sparkle star directly into an ARGB_8888 Bitmap.
     * This avoids RemoteViews reflection bugs and vector drawable limitations on home screen widgets.
     */
    fun createSparkleBitmap(
        sizePx: Int = 72,
        colorInt: Int = 0xFF4F8DF7.toInt()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }

        val s = sizePx.toFloat()
        val path = Path()

        fun x(v: Float) = (v / 24f) * s
        fun y(v: Float) = (v / 24f) * s

        path.moveTo(x(12f), y(2f))
        path.lineTo(x(13.2f), y(9.2f))
        path.lineTo(x(19.8f), y(5.4f))
        path.lineTo(x(14.8f), y(11.2f))
        path.lineTo(x(22f), y(12f))
        path.lineTo(x(14.8f), y(12.8f))
        path.lineTo(x(19.8f), y(18.6f))
        path.lineTo(x(13.2f), y(14.8f))
        path.lineTo(x(12f), y(22f))
        path.lineTo(x(10.8f), y(14.8f))
        path.lineTo(x(4.2f), y(18.6f))
        path.lineTo(x(9.2f), y(12.8f))
        path.lineTo(x(2f), y(12f))
        path.lineTo(x(9.2f), y(11.2f))
        path.lineTo(x(4.2f), y(5.4f))
        path.lineTo(x(10.8f), y(9.2f))
        path.close()

        canvas.drawPath(path, paint)
        return bitmap
    }
}
