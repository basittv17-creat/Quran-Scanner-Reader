package org.quranscan.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import org.quranscan.reader.core.CropBounds

/** Fit-center preview with selection mapped back to the bounded decoded image. */
class GalleryCropView(context: Context, private val image: Bitmap) : View(context) {
    private val imageRect = RectF()
    private val selectionRect = RectF()
    private val imagePaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(216, 173, 86); style = Paint.Style.STROKE
        strokeWidth = 2 * resources.displayMetrics.density
    }
    private val shade = Paint().apply { color = Color.argb(125, 0, 0, 0) }
    private var selection = CropBounds(0, 0, image.width, image.height)
    private var startX = 0f
    private var startY = 0f
    private var dragging = false
    private var scale = 1f

    init {
        isFocusable = true
        contentDescription = "Image crop preview. Drag to select text, or use Scan full image."
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        scale = minOf(w.toFloat() / image.width, h.toFloat() / image.height).coerceAtLeast(.0001f)
        val dw = image.width * scale
        val dh = image.height * scale
        imageRect.set((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(image, null, imageRect, imagePaint)
        selectionRect.set(imageRect.left + selection.left * scale, imageRect.top + selection.top * scale,
            imageRect.left + selection.right * scale, imageRect.top + selection.bottom * scale)
        canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, selectionRect.top, shade)
        canvas.drawRect(imageRect.left, selectionRect.bottom, imageRect.right, imageRect.bottom, shade)
        canvas.drawRect(imageRect.left, selectionRect.top, selectionRect.left, selectionRect.bottom, shade)
        canvas.drawRect(selectionRect.right, selectionRect.top, imageRect.right, selectionRect.bottom, shade)
        canvas.drawRect(selectionRect, border)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!imageRect.contains(event.x, event.y)) return false
                startX = (event.x - imageRect.left) / scale
                startY = (event.y - imageRect.top) / scale
                dragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                if (!dragging) return false
                CropBounds.fromDrag(image.width, image.height, startX, startY,
                    (event.x - imageRect.left) / scale, (event.y - imageRect.top) / scale)?.let {
                    selection = it; invalidate()
                }
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    dragging = false; parent?.requestDisallowInterceptTouchEvent(false); performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false; parent?.requestDisallowInterceptTouchEvent(false); return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    fun croppedBitmap(): Bitmap = Bitmap.createBitmap(image, selection.left, selection.top,
        selection.width, selection.height)
}
