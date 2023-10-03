package com.example.mylibrary.common

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics

private const val START_ANGLE = 0f

class PieProgressDrawable:Drawable() {

    private var mPaint :Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 7f }
    private var mBoundsF : RectF? = null
    private var mInnerBoundsF :RectF? = null
    private var mDrawTo = 0f


    fun setBorderWidth(widthDp: Float, dm: DisplayMetrics) {
        val borderWidth = widthDp * dm.density
        mPaint.strokeWidth = borderWidth
    }

    fun setColor(color: Int) {
        mPaint.color = color
    }

    override fun draw(canvas: Canvas) {
        canvas.rotate(-90f, bounds.centerX().toFloat(), bounds.centerY().toFloat())
        mPaint.style = Paint.Style.STROKE
        canvas.drawOval(mBoundsF!!, mPaint)
        mPaint.style = Paint.Style.FILL
        canvas.drawArc(mInnerBoundsF!!, START_ANGLE, mDrawTo, true, mPaint)
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint.colorFilter = cf
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return mPaint.alpha
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        mInnerBoundsF = RectF(bounds)
        mBoundsF = mInnerBoundsF
        val halfBorder = (mPaint.strokeWidth / 2f + 0.5f).toInt()
        mInnerBoundsF!!.inset(halfBorder.toFloat(), halfBorder.toFloat())
    }

    override fun onLevelChange(level: Int): Boolean {
        val drawTo = START_ANGLE + 360.toFloat() * level / 100f
        val update = drawTo != mDrawTo
        mDrawTo = drawTo
        return update
    }
}