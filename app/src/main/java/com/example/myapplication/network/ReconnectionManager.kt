package com.example.myapplication.network

import android.util.Log
import com.example.myapplication.PlayerData
import com.example.myapplication.logging.GameLogger
import kotlinx.coroutines.*

class ReconnectionManager(
    private val gameLogger: GameLogger
) {
    
    companion object {
        private const val TAG = "ReconnectionManager"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val INITIAL_RECONNECT_DELAY = 1000L // 1 секунда
        private const val MAX_RECONNECT_DELAY = 30000L // 30 секунд
        private const val BACKOFF_MULTIPLIER = 2.0
    }
    
    private var reconnectJob: Job? = null
    private var currentAttempt = 0
    private var isReconnecting = false
    
    // Callback для повторного подключения
    private var reconnectCallback: (suspend () -> Unit)? = null
    
    fun setReconnectCallback(callback: suspend () -> Unit) {
        reconnectCallback = callback
    }
    
    fun startReconnection() {
        if (isReconnecting) {
            Log.d(TAG, "Повторное подключение уже выполняется")
            return
        }
        
        isReconnecting = true
        currentAttempt = 0
        
        gameLogger.log("INFO", TAG, "Начало процесса повторного подключения")
        
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            while (currentAttempt < MAX_RECONNECT_ATTEMPTS && isReconnecting) {
                try {
                    currentAttempt++
                    val delay = calculateDelay(currentAttempt)
                    
                    gameLogger.log(
                        "INFO", 
                        TAG, 
                        "Попытка повторного подключения $currentAttempt/$MAX_RECONNECT_ATTEMPTS через ${delay}ms"
                    )
                    
                    delay(delay)
                    
                    reconnectCallback?.invoke()
                    
                    // Если дошли сюда, значит подключение успешно
                    gameLogger.log("INFO", TAG, "Повторное подключение успешно")
                    isReconnecting = false
                    currentAttempt = 0
                    return@launch
                    
                } catch (e: Exception) {
                    gameLogger.log(
                        "ERROR", 
                        TAG, 
                        "Ошибка при попытке $currentAttempt: ${e.message}"
                    )
                    
                    if (currentAttempt >= MAX_RECONNECT_ATTEMPTS) {
                        gameLogger.log(
                            "ERROR", 
                            TAG, 
                            "Превышено максимальное количество попыток повторного подключения"
                        )
                        isReconnecting = false
                        break
                    }
                }
            }
        }
    }
    
    fun stopReconnection() {
        isReconnecting = false
        reconnectJob?.cancel()
        currentAttempt = 0
        
        gameLogger.log("INFO", TAG, "Остановка процесса повторного подключения")
    }
    
    private fun calculateDelay(attempt: Int): Long {
        val delay = (INITIAL_RECONNECT_DELAY * Math.pow(BACKOFF_MULTIPLIER, (attempt - 1).toDouble())).toLong()
        return minOf(delay, MAX_RECONNECT_DELAY)
    }
    
    fun isReconnecting(): Boolean = isReconnecting
    
    fun getCurrentAttempt(): Int = currentAttempt
    
    fun getMaxAttempts(): Int = MAX_RECONNECT_ATTEMPTS
}
