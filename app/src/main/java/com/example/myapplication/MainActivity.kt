package com.example.myapplication

import android.util.Log
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.HttpClient


class MainActivity : AppCompatActivity() {
    private lateinit var client: HttpClient
    private lateinit var joystick: JoystickView
    private lateinit var gameView: GameView
    val publicLogin = LoginActivite().publicLogin
    lateinit var playerData: PlayerData
    private var players = mutableMapOf<Int, PlayerData>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main) // Убедитесь, что у вас есть этот файл
        } catch (e: Exception) {
            Log.d("888", "Ошибка $e")
        } finally {
        }


        joystick = findViewById(R.id.joystick)
        gameView = findViewById(R.id.game_view)

        client = HttpClientProvider.create()

        playerData = PlayerData(publicLogin, 500f, 500f) // Начальные координаты игрока


        var joyDx = 0f
        var joyDy = 0f
        joystick.onJoystickMoveListener = { dx, dy ->
            gameView.updatePlayerPosition(dx, dy)
            playerData.x += dx // Применять изменения к данным игрока
            playerData.y += dy // Обновляем данные о позиции игрока
        }


    }



    override fun onResume() {
        Log.d("888", "onResume called")
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        Log.d("888", "onPause called")

        super.onPause()
        gameView.pause()
    }

    override fun onDestroy() {
        Log.d("888", "onDestroy called")
        super.onDestroy()
        if (::client.isInitialized) {
            client.close()
        }
    }
}


