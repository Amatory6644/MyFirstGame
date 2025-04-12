package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class JoystickView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var outerCirclePaint: Paint = Paint()
    private var innerCirclePaint: Paint = Paint()
    private var innerCircleRadius = 100f
    private var outerCircleRadius = 150f
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    var onJoystickMoveListener: ((dx: Float, dy: Float) -> Unit)? = null

    init {
        outerCirclePaint.color = Color.LTGRAY
        outerCirclePaint.style = Paint.Style.FILL
        innerCirclePaint.color = Color.BLUE
        innerCirclePaint.style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Ограничиваем движение внутреннего круга радиусом внешнего круга
                if (distance < outerCircleRadius) {
                    innerCircleRadius = distance
                } else {
                    innerCircleRadius = outerCircleRadius
                }

                invalidate()
                onJoystickMoveListener?.invoke(dx, dy)
            }
            MotionEvent.ACTION_UP -> {
                innerCircleRadius = 0f
                invalidate()
                onJoystickMoveListener?.invoke(0f, 0f)
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        centerX = width / 2f
        centerY = height / 2f
        // Рисуем внешний круг
        canvas.drawCircle(centerX, centerY, outerCircleRadius, outerCirclePaint)
        // Рисуем внутренний круг
        canvas.drawCircle(centerX + innerCircleRadius, centerY + innerCircleRadius, innerCircleRadius, innerCirclePaint)
    }
}


//class JoystickView(context: Context, attrs: AttributeSet) : View(context, attrs) {
//    private var centerX: Float = 0f
//    private var centerY: Float = 0f
//    private var radius: Float = 100f
//    private var stickX: Float = centerX
//    private var stickY: Float = centerY
//
//    init {
//        // Инициализация, установка размеров и т.д.
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//        // Рисуем джойстик и его палку
//        val paint = Paint().apply {
//
//            color = Color.GRAY
//        }
//        canvas.drawCircle(centerX, centerY, radius, paint)
//
//        paint.color = Color.RED
//        canvas.drawCircle(stickX, stickY, radius / 3, paint)
//    }
//}