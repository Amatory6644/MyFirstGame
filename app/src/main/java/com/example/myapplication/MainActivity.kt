package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.repository.GameRepository
import com.example.myapplication.ui.ConnectionStatusView
import com.example.myapplication.ui.LoadingIndicator
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    
    // Получаем PlayerViewModel через Koin
    private val playerViewModel: PlayerViewModel by inject()
    
    private lateinit var joystick: JoystickView
    private lateinit var gameView: GameView
    private lateinit var connectionStatusView: ConnectionStatusView
    private lateinit var loadingIndicator: LoadingIndicator
    
    // Получаем логин и группу из Intent
    private var playerLogin: String = ""
    private var groupId: String = "default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка при установке layout: $e")
            return
        }
        
        // Получаем логин и группу из Intent
        playerLogin = intent.getStringExtra("LOGIN_EXTRA") ?: ""
        groupId = intent.getStringExtra("GROUP_ID") ?: "default"
        
        if (playerLogin.isEmpty()) {
            Log.e("MainActivity", "Логин не передан")
            finish()
            return
        }
        
        Log.d("MainActivity", "Подключение игрока $playerLogin к группе $groupId")
        
        // Инициализируем UI компоненты
        joystick = findViewById(R.id.joystick)
        gameView = findViewById(R.id.game_view)
        connectionStatusView = findViewById(R.id.connection_status)
        loadingIndicator = findViewById(R.id.loading_indicator)
        
        // Устанавливаем LifecycleOwner и PlayerViewModel в GameView
        gameView.setLifecycleOwner(this)
        gameView.setPlayerViewModel(playerViewModel)
        gameView.setCurrentPlayerLogin(playerLogin)
        
        // Устанавливаем логин игрока в ViewModel
        playerViewModel.setPlayerLogin(playerLogin)
        
        // Настраиваем джойстик
        setupJoystick()
        
        // Подключаемся к игре
        connectToGame()
        
        // Наблюдаем за состоянием соединения
        observeConnectionStatus()
    }
    
    private fun setupJoystick() {
        joystick.onJoystickMoveListener = { dx, dy ->
            // Обновляем позицию через ViewModel
            playerViewModel.updatePosition(dx, dy)
        }
    }
    
    private fun connectToGame() {
        loadingIndicator.show()
        lifecycleScope.launch {
            try {
                // Подключаемся к игровому серверу с указанием группы
                val gameRoute = "/game?username=$playerLogin&groupId=$groupId"
                Log.d("MainActivity", "🔴 ПОДКЛЮЧЕНИЕ К URL: ws://192.168.0.105:8080$gameRoute")
                Log.d("MainActivity", "🔴 ЛОГИН ИГРОКА: $playerLogin")
                Log.d("MainActivity", "🔴 ID ГРУППЫ: $groupId")
                playerViewModel.connectToGame(gameRoute)
                Log.d("MainActivity", "Подключение к игре инициировано: $gameRoute")
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка при подключении к игре: $e")
                loadingIndicator.hide()
                Toast.makeText(this@MainActivity, "Ошибка подключения: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun observeConnectionStatus() {
        lifecycleScope.launch {
            playerViewModel.connectionStatus.collect { status ->
                when (status) {
                    is GameRepository.ConnectionStatus.Connected -> {
                        Log.d("MainActivity", "Подключено к игровому серверу")
                        loadingIndicator.hide()
                        connectionStatusView.updateStatus(status)
                        Toast.makeText(this@MainActivity, "Подключено к серверу", Toast.LENGTH_SHORT).show()
                    }
                    is GameRepository.ConnectionStatus.Disconnected -> {
                        Log.d("MainActivity", "Отключено от игрового сервера")
                        loadingIndicator.hide()
                        connectionStatusView.updateStatus(status)
                    }
                    is GameRepository.ConnectionStatus.Error -> {
                        Log.e("MainActivity", "Ошибка соединения: ${status.message}")
                        loadingIndicator.hide()
                        connectionStatusView.updateStatus(status)
                        Toast.makeText(this@MainActivity, "Ошибка соединения: ${status.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        // Наблюдаем за статусом повторного подключения
        lifecycleScope.launch {
            // Здесь можно добавить наблюдение за статусом повторного подключения
            // если добавить соответствующий Flow в PlayerViewModel
        }
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Отключаемся от игры при закрытии Activity
        playerViewModel.disconnectFromGame()
    }
}


