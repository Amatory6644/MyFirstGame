package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF

class Rug(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val bitmap: Bitmap
) {
    // Метод для отрисовки объекта на канвасе
    fun draw(canvas: Canvas, offsetX:Float, offsetY:Float) {
        val drawX = x + offsetX
        val drawY = y + offsetY

        // Рисуем объект на канвасе по заданным координатам
        canvas.drawBitmap(bitmap, null, RectF(drawX, drawY, drawX + width, drawY + height), null)

    }
}
