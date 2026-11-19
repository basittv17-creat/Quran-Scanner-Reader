package org.quranscan.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import org.quranscan.reader.core.LiveScanRegion

/** Draws the active OCR selection over the shared CameraX viewport. */
class ScanReticleView(context: Context) : View(context) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(216, 173, 86)
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
    }
    var region = LiveScanRegion.DEFAULT

    private val reticle = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = reticle.apply { set(w * region.left, h * region.top, w * region.right, h * region.bottom) }
        val len = minOf(r.width(), r.height()) * .15f
        // Four corner guides, without obscuring Quran print.
        canvas.drawLine(r.left, r.top, r.left + len, r.top, line)
        canvas.drawLine(r.left, r.top, r.left, r.top + len, line)
        canvas.drawLine(r.right, r.top, r.right - len, r.top, line)
        canvas.drawLine(r.right, r.top, r.right, r.top + len, line)
        canvas.drawLine(r.left, r.bottom, r.left + len, r.bottom, line)
        canvas.drawLine(r.left, r.bottom, r.left, r.bottom - len, line)
        canvas.drawLine(r.right, r.bottom, r.right - len, r.bottom, line)
        canvas.drawLine(r.right, r.bottom, r.right, r.bottom - len, line)
    }
}
