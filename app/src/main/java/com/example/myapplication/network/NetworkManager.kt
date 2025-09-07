package com.example.myapplication.network

import android.util.Log
import com.example.myapplication.PlayerData
import com.example.myapplication.logging.GameLogger
import com.example.myapplication.repository.GameRepository
import com.example.myapplication.repository.NetworkException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NetworkManager(
    private val httpClient: HttpClient,
    private val gameRepository: GameRepository,
    private val gameLogger: GameLogger
) {
    
    companion object {
        private const val TAG = "NetworkManager"
    }
    
    private var webSocketJob: Job? = null
    private var updatePositionJob: Job? = null
    
    // SharedFlow для отправки обновлений позиции
    private val _positionUpdates = MutableSharedFlow<PlayerData>()
    val positionUpdates = _positionUpdates.asSharedFlow()
    
    // Статус соединения
    private var isConnected = false
    
    // Менеджер повторного подключения
    private val reconnectionManager = ReconnectionManager(gameLogger)
    
   suspend fun connectToGame(route: String, playerData: PlayerData) { // suspend тут я сам написал
        if (isConnected) {
            gameLogger.log("DEBUG", TAG, "Уже подключено к игре")
            return
        }
        
        gameLogger.log("INFO", TAG, "Подключение к игре: $route", playerData.login)
        gameLogger.logPlayerAction(playerData.login, "CONNECT", "route=$route")
        
        // Настраиваем callback для повторного подключения
        reconnectionManager.setReconnectCallback {
            performConnection(route, playerData)
        }
        
        performConnection(route, playerData) // т.к. тут жалуется на корутины
    }
    
    private suspend fun performConnection(route: String, playerData: PlayerData) {
        webSocketJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                isConnected = true
                gameLogger.log("INFO", TAG, "WebSocket соединение установлено", playerData.login)
                
                gameRepository.connectToGame(route, playerData).collect {
                    // Слушаем статус соединения
                    gameLogger.log("DEBUG", TAG, "WebSocket соединение активно", playerData.login)
                }
            } catch (e: NetworkException) {
                gameLogger.log("ERROR", TAG, "Ошибка сетевого соединения: ${e.message}", playerData.login)
                isConnected = false
                reconnectionManager.startReconnection()
            } catch (e: Exception) {
                gameLogger.log("ERROR", TAG, "Неожиданная ошибка соединения: ${e.message}", playerData.login)
                isConnected = false
                reconnectionManager.startReconnection()
            }
        }
        
        // Запускаем отправку обновлений позиции только после успешного подключения
        if (isConnected) {
            startPositionUpdates(playerData)
        }
    }
    
    private fun startPositionUpdates(initialPlayerData: PlayerData) {
        updatePositionJob = CoroutineScope(Dispatchers.IO).launch {
            var currentPlayerData = initialPlayerData
            
            while (isActive && isConnected) {
                try {
                    // Отправляем текущую позицию игрока
                    sendPlayerPosition(currentPlayerData)
                    delay(16) // 60 FPS
                } catch (e: NetworkException) {
                    Log.e(TAG, "Ошибка отправки позиции: ${e.message}", e)
                    // Пытаемся переподключиться
                    handleNetworkError(e)
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Неожиданная ошибка отправки позиции: ${e.message}", e)
                    break
                }
            }
        }
    }
    
    suspend fun sendPlayerPosition(playerData: PlayerData) {
        try {
            gameLogger.log("DEBUG", TAG, "Отправка позиции игрока: $playerData", playerData.login)
            
            // Логируем движение для античита
            gameLogger.logPlayerAction(
                playerData.login, 
                "MOVE", 
                "x=${playerData.x},y=${playerData.y}"
            )
            
            _positionUpdates.emit(playerData)
            
            // Отправляем через Repository
            gameRepository.sendPlayerUpdate(playerData)
            
        } catch (e: NetworkException) {
            gameLogger.log("ERROR", TAG, "Ошибка отправки позиции через Repository: ${e.message}", playerData.login)
            throw e
        } catch (e: Exception) {
            gameLogger.log("ERROR", TAG, "Неожиданная ошибка отправки позиции: ${e.message}", playerData.login)
            throw NetworkException("Ошибка отправки позиции: ${e.message}", e)
        }
    }
    
    private suspend fun handleNetworkError(error: NetworkException) {
        Log.w(TAG, "Обработка сетевой ошибки: ${error.message}")
        
        // Здесь можно добавить логику повторного подключения
        // Например, попытка переподключения через несколько секунд
        
        try {
            delay(5000) // Ждем 5 секунд перед повторной попыткой
            Log.d(TAG, "Попытка повторного подключения...")
            // TODO: Реализовать логику повторного подключения
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при попытке повторного подключения: ${e.message}", e)
        }
    }
    
    fun disconnect() {
        gameLogger.log("INFO", TAG, "Отключение от игры")
        isConnected = false
        
        // Останавливаем повторное подключение
        reconnectionManager.stopReconnection()
        
        webSocketJob?.cancel()
        updatePositionJob?.cancel()
        
        gameLogger.log("INFO", TAG, "Соединение закрыто")
    }
    
    fun isGameConnected(): Boolean = isConnected
    
    fun isReconnecting(): Boolean = reconnectionManager.isReconnecting()
    
    fun getReconnectionAttempt(): Int = reconnectionManager.getCurrentAttempt()
    
    fun getMaxReconnectionAttempts(): Int = reconnectionManager.getMaxAttempts()
    
    // Функция для проверки состояния сети
    fun checkNetworkStatus(): Boolean {
        return try {
            // Простая проверка доступности сервера
            // В реальном приложении здесь может быть более сложная логика
            isConnected
        } catch (e: Exception) {
            gameLogger.log("ERROR", TAG, "Ошибка проверки состояния сети: ${e.message}")
            false
        }
    }
}
