package com.example.myapplication.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class LoadingIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    
    private var rotationAngle = 0f
    private var animator: ValueAnimator? = null
    private var isVisible = false
    
    init {
        startAnimation()
    }
    
    fun show() {
        isVisible = true
        visibility = VISIBLE
        startAnimation()
    }
    
    fun hide() {
        isVisible = false
        visibility = GONE
        stopAnimation()
    }
    
    private fun startAnimation() {
        if (animator?.isRunning == true) return
        
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                rotationAngle = animation.animatedValue as Float
                if (isVisible) {
                    invalidate()
                }
            }
            start()
        }
    }
    
    private fun stopAnimation() {
        animator?.cancel()
        animator = null
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isVisible) return
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) / 4f
        
        canvas.save()
        canvas.rotate(rotationAngle, centerX, centerY)
        
        // Рисуем дугу
        canvas.drawArc(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            0f,
            270f,
            false,
            paint
        )
        
        canvas.restore()
        
        // Рисуем текст
        paint.apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText("Загрузка...", centerX, centerY + radius + 40f, paint)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
