package com.testbird.signalsecurevpn.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout

/*蒙层view*/
class MaskView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    var clickListener: (() -> Unit)? = null
    override fun dispatchDraw(canvas: Canvas?) {
        val paint = Paint()
        paint.color = Color.parseColor("#B3000000")
        canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        super.dispatchDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        clickListener?.invoke()
        return true
    }

}