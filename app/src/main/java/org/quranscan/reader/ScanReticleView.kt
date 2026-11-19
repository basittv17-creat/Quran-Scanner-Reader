package org.quranscan.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Simple passive guide overlay; OCR uses full upright image, not this outlined area. */
class ScanReticleView(context: Context) : View(context) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(216, 173, 86)
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
    }
    private val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = RectF(w * .09f, h * .20f, w * .91f, h * .80f)
        val len = minOf(w, h) * .085f
        // Four corner guides, without obscuring Quran print.
        canvas.drawLine(r.left, r.top, r.left + len, r.top, line)
        canvas.drawLine(r.left, r.top, r.left, r.top + len, line)
        canvas.drawLine(r.right, r.top, r.right - len, r.top, line)
        canvas.drawLine(r.right, r.top, r.right, r.top + len, line)
        canvas.drawLine(r.left, r.bottom, r.left + len, r.bottom, line)
        canvas.drawLine(r.left, r.bottom, r.left, r.bottom - len, line)
        canvas.drawLine(r.right, r.bottom, r.right - len, r.bottom, line)
        canvas.drawLine(r.right, r.bottom, r.right, r.bottom - len, line)
        canvas.drawText("Keep the printed page steady inside the frame", w / 2f, r.top - 18f, hint)
    }
}
