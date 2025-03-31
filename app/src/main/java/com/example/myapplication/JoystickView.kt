package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class JoystickView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var radius: Float = 100f
    private var stickX: Float = centerX
    private var stickY: Float = centerY

    init {
        // Инициализация, установка размеров и т.д.
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем джойстик и его палку
        val paint = Paint().apply {

            color = Color.GRAY
        }
        canvas.drawCircle(centerX, centerY, radius, paint)

        paint.color = Color.RED
        canvas.drawCircle(stickX, stickY, radius / 3, paint)
    }
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        when (event.action) {
//            MotionEvent.ACTION_MOVE -> {
//                val dx = event.x - stickX
//                val dy = event.y - stickY
//                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
//
//                // Если джойстик слишком близко к краю, продолжаем движение персонажа
//                if (distance > joystickRadius) {
//                    stickX = stickX + (dx / distance * joystickRadius)
//                    stickY = stickY + (dy / distance * joystickRadius)
//                    // Позиция игрока обновляется в соответствии с направлением движения
//                    playerX += (dx / distance * 5) // Скорость движения
//                    playerY += (dy / distance * 5) // Скорость движения
//                } else {
//                    stickX = event.x
//                    stickY = event.y
//                    // Также обновляем позицию игрока
//                    playerX += (dx / 10) // Замените 10 на значение, соответствующее вашей желаемой скорости
//                    playerY += (dy / 10)
//                }
//            }
//            MotionEvent.ACTION_UP -> {
//                stickX = stickX
//                stickY = stickY
//            }
//        }
//        return true
//    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                stickX = event.x
                stickY = event.y
                invalidate() // Перерисуем вид
                // Здесь добавьте свою логику движения персонажа
                // Например:
                handleMovement()
            }
            MotionEvent.ACTION_UP -> {
                stickX = centerX
                stickY = centerY
                invalidate()
            }
        }
        return true
    }

    private fun handleMovement() {
        // Логика движения персонажа на основе положения stickX и stickY
    }
}
