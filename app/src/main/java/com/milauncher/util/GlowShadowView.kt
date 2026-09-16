package com.milauncher.util

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class GlowShadowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val insetDp = 60f // Huge margin to prevent blur clipping
    private val cornerRadiusDp = 16f
    
    private val density = context.resources.displayMetrics.density
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
    }
    
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
    }

    private val rect = RectF()

    var glowRadius: Float = 12f
        set(value) {
            field = value
            if (value > 0f) {
                paint.maskFilter = BlurMaskFilter(value * density, BlurMaskFilter.Blur.NORMAL)
            } else {
                paint.maskFilter = null
            }
            invalidate()
        }

    init {
        // BlurMaskFilter requires software layer to composite CLEAR correctly
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        glowRadius = 12f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        
        // Expand the view bounds by insetDp on all sides so the shadow is not clipped
        val insetPx = (insetDp * density).toInt()
        setMeasuredDimension(parentWidth + insetPx * 2, parentHeight + insetPx * 2)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = insetDp * density
        // The rect represents the actual card bounds
        rect.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = cornerRadiusDp * density
        
        // Draw the blurred shadow (which also fills the center)
        canvas.drawRoundRect(rect, radius, radius, paint)
        
        // Erase the center exactly to the card's dimensions
        canvas.drawRoundRect(rect, radius, radius, clearPaint)
    }
}
