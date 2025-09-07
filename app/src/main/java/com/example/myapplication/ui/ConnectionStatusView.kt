package com.example.myapplication.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.myapplication.repository.GameRepository

class ConnectionStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = 32f
    }
    
    private var connectionStatus: GameRepository.ConnectionStatus = GameRepository.ConnectionStatus.Disconnected
    private var reconnectionAttempt: Int = 0
    private var maxReconnectionAttempts: Int = 0
    private var isReconnecting: Boolean = false
    
    fun updateStatus(
        status: GameRepository.ConnectionStatus,
        reconnectionAttempt: Int = 0,
        maxAttempts: Int = 0,
        isReconnecting: Boolean = false
    ) {
        this.connectionStatus = status
        this.reconnectionAttempt = reconnectionAttempt
        this.maxReconnectionAttempts = maxAttempts
        this.isReconnecting = isReconnecting
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        when (connectionStatus) {
            is GameRepository.ConnectionStatus.Connected -> {
                paint.color = Color.GREEN
                canvas.drawCircle(centerX, centerY, 20f, paint)
                
                paint.color = Color.BLACK
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Подключено", centerX, centerY + 60f, paint)
            }
            
            is GameRepository.ConnectionStatus.Disconnected -> {
                paint.color = Color.RED
                canvas.drawCircle(centerX, centerY, 20f, paint)
                
                paint.color = Color.BLACK
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Отключено", centerX, centerY + 60f, paint)
            }
            
            is GameRepository.ConnectionStatus.Error -> {
                paint.color = Color.RED
                canvas.drawCircle(centerX, centerY, 20f, paint)
                
                paint.color = Color.BLACK
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Ошибка", centerX, centerY + 60f, paint)
                canvas.drawText((connectionStatus as GameRepository.ConnectionStatus.Error).message, centerX, centerY + 100f, paint)
                    //почему то connectionStatus.message превратилось вон в что
            }
        }
        
        // Показываем статус повторного подключения
        if (isReconnecting) {
            paint.color = Color.BLUE
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "Переподключение: $reconnectionAttempt/$maxReconnectionAttempts",
                centerX,
                centerY + 140f,
                paint
            )
        }
    }
}
