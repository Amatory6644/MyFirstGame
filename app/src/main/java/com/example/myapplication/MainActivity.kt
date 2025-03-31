package com.example.myapplication

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView
    private lateinit var thread: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = GameView(this)
        setContentView(gameView)

        thread = Thread(gameView)
        thread.start()
    }

    override fun onPause() {
        super.onPause()
        gameView.stop()
        thread.join()
    }

    override fun onResume() {
        super.onResume()
        thread = Thread(gameView)
        thread.start()
    }
}
