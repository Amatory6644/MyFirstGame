package com.example.myapplication.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class GameNotificationService : Service() {
    
    private val gameRepository: GameRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "GameNotificationService"
        const val ACTION_START_NOTIFICATIONS = "START_NOTIFICATIONS"
        const val EXTRA_PLAYER_LOGIN = "PLAYER_LOGIN"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand вызван с action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_NOTIFICATIONS -> {
                val playerLogin = intent.getStringExtra(EXTRA_PLAYER_LOGIN)
                Log.d(TAG, "Получен логин игрока: $playerLogin")
                if (playerLogin != null) {
                    Log.d(TAG, "Запуск слушателя уведомлений для: $playerLogin")
                    startNotificationListener(playerLogin)
                }
            }
        }
        return START_STICKY
    }
    
    private fun startNotificationListener(playerLogin: String) {
        Log.d(TAG, "Запуск слушателя уведомлений для игрока: $playerLogin")
        
        // Подключаемся к WebSocket для получения уведомлений
        serviceScope.launch {
            try {
                Log.d(TAG, "Подключение к WebSocket для игрока: $playerLogin")
                gameRepository.connectForNotifications(playerLogin).collect { }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка подключения к WebSocket: $e")
            }
        }
        
        // Слушаем уведомления о начале игры
        serviceScope.launch {
            Log.d(TAG, "Начинаем слушать уведомления для игрока: $playerLogin")
            gameRepository.gameStartedNotifications.collect { notification ->
                Log.d(TAG, "Получено уведомление о начале игры: $notification")
                
                // Отправляем broadcast для всех активностей
                val broadcastIntent = Intent("GAME_STARTED")
                broadcastIntent.putExtra("groupId", notification.groupId)
                broadcastIntent.putExtra("message", notification.message)
                sendBroadcast(broadcastIntent)
                
                Log.d(TAG, "Отправлен broadcast для группы: ${notification.groupId}")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "GameNotificationService остановлен")
    }
}
