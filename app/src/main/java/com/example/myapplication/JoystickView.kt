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
    private var innerCircleRadius = 0f
    private var outerCircleRadius = 150f
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var innerCircleX: Float = 0f
    private var innerCircleY: Float = 0f
    var onJoystickMoveListener: ((dx: Float, dy: Float) -> Unit)? = null

    init {
        outerCirclePaint.color = Color.LTGRAY
        outerCirclePaint.style = Paint.Style.FILL
        innerCirclePaint.color = Color.BLUE
        innerCirclePaint.style = Paint.Style.FILL
        innerCircleRadius = outerCircleRadius / 2 // Установка радиуса внутреннего круга
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Ограничиваем движение внутреннего круга радиусом внешнего круга
                if (distance < outerCircleRadius) {
                    innerCircleX = event.x
                    innerCircleY = event.y
                } else {
                    // Нормализуем значения по углам
                    val angle = Math.atan2(dy.toDouble(), dx.toDouble())
                    innerCircleX = centerX + (Math.cos(angle) * outerCircleRadius).toFloat()
                    innerCircleY = centerY + (Math.sin(angle) * outerCircleRadius).toFloat()
                }
//              было это   onJoystickMoveListener?.invoke(-dx, -dy)// почему то нужен минус чтобы было без инверсии
                // стало это Отправляем нормализованные значения движения
                onJoystickMoveListener?.invoke(dx / outerCircleRadius, dy / outerCircleRadius)

                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                innerCircleX = centerX
                innerCircleY = centerY
                onJoystickMoveListener?.invoke(0f, 0f)

                invalidate()
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
        canvas.drawCircle(centerX, centerY, innerCircleRadius, innerCirclePaint)
    }

}


