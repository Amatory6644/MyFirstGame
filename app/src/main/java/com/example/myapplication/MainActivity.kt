package com.example.myapplication

import android.content.res.Resources
import android.util.Log
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.UUID


import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.PendingIntentCompat.send
import androidx.lifecycle.lifecycleScope
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
//val mapWidth = 2000f // ширина карты
//val mapHeight = 2000f // высота карты

@Serializable
data class Player(
    val id: String,
    var x: Float,
    var y: Float
)

class MainActivity : AppCompatActivity() {
    private lateinit var client: HttpClient
    private lateinit var player: Player
    private lateinit var playerImage: ImageView
    private lateinit var connectButton: Button
    private lateinit var joystick: JoystickView
    private lateinit var mainLayout: RelativeLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Убедитесь, что у вас есть этот файл

        playerImage = findViewById(R.id.player_image) // Ваш ImageView для игрока
        connectButton = findViewById(R.id.connect_button)
        joystick = findViewById(R.id.joystick)
        mainLayout = findViewById(R.id.main_layout)


        client = HttpClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true; prettyPrint = true
                })
            }
        }

        val playerId = generatePlayerId()
        player = Player(playerId, 500f, 500f) // Начальные координаты игрока


// Запуск корутины для WebSocket соединения
        connectButton.setOnClickListener{
            lifecycleScope.launch {
                connectToWebSocket()
            }
        }

        joystick.onJoystickMoveListener = {dx, dy ->
            updatePlayerPosition(dx, dy)
        }

    }

    private suspend fun connectToWebSocket() {
        client.webSocket("ws://192.168.0.105:8080/game") {
            Log.d("WebSocket", "Connected to server")

            // Отправка игрока на сервер
            send(Json.encodeToString(player))

            // Запуск корутины для обработки входящих сообщений от сервера
            launch {
                try {
                    for (message in incoming) {
                        when (message) {
                            is Frame.Text -> {
                                // Получаем обновления игроков от сервера
                                val updatedPlayers: List<Player> =
                                    Json.decodeFromString(message.readText())
                                Log.d("WebSocket", "Received updated players: $updatedPlayers")
                                // Обновите интерфейс при необходимости
                            }

                            is Frame.Close -> {
                                Log.d("WebSocket", "Connection closed: $message")
                                return@launch
                            }

                            is Frame.Binary -> TODO()
                            is Frame.Ping -> TODO()
                            is Frame.Pong -> TODO()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error while processing incoming messages: ${e.message}")
                }
            }

            // Цикл для обновления позиции игрока
            while (true) {
                try {
// Здесь вы можете изменить координаты игрока через джойстик
                    // Например, получаем обновления от джойстика:
                    val dx =
                        0f // Получите значение из вашего джойстика (например, cos(angle) * strength)
                    val dy =
                        0f // Получите значение из вашего джойстика (например, sin(angle) * strength)

                    updatePlayerPosition(dx, dy)
                    // Отправляем обновленные координаты игрока на сервер
                    send(Json.encodeToString(player))

                    delay(1000L / 60) // Задержка перед следующей отправкой
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error while sending player updates: ${e.message}")
                    break // Выходим из цикла при возникновении ошибки
                }
            }
        }

        // Закрываем клиент после завершения работы
        client.close()
    }
    private fun updatePlayerPosition(dx: Float, dy: Float) {
        player.x += dx
        player.y += dy

        // Обновляем позицию ImageView для игрока
        playerImage.x = player.x - (playerImage.width / 2)
        playerImage.y = player.y - (playerImage.height / 2)

        // Центрируем камеру после обновления позиции игрока
        centerCamera()
        // Отправляем обновленные координаты игрока на сервер
    }
    private fun centerCamera() {
        // Логгируем размеры карты и позиции игрока
        Log.d("Camera", "playerX: ${player.x}")
        Log.d("Camera", "playerY: ${player.y}")

        // Получаем размеры экрана
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Вычисляем координаты для установки центра камеры
        val cameraX = player.x + playerImage.width / 2f - (screenWidth / 2f)
        Log.d("Camera", "cameraX: $cameraX")

        val cameraY = player.y + playerImage.height / 2f - (screenHeight / 2f)
        Log.d("Camera", "cameraY: $cameraY")

        // Перемещаем родительский элемент "камеры"
        val main = findViewById<RelativeLayout>(R.id.main_layout)
        main.translationX = -cameraX // Тут убрана коррекция камер
        main.translationY = -cameraY // Тут убрана коррекция камер
        // Логируем окончательные значения
        Log.d("Camera", "main translationX: ${main.translationX}, translationY: ${main.translationY}")
    }

    private fun generatePlayerId() = UUID.randomUUID().toString()
}

