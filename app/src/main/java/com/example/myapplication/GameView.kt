package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs),
    Runnable {

    private lateinit var thread: Thread
    private var isPlaying = true
    private val paint = Paint()
    private var playerX = 100f // Начальная позиция игрока по X
    private var playerY = 100f // Начальная позиция игрока по Y
    private  var playerBitmap: Bitmap? = null


    init {
        try {
            Log.d("6644GameView", "1")
            playerBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
            Log.d("6644GameView", "2")
            Log.d("6644GameView", "Player bitmap loaded successfully.")
            if (playerBitmap == null) {
                Log.e("6644GameView", "playerBitmap is null after decoding")
            }
        } catch (e: Exception) {
            Log.d("6644GameView", "3")
            Log.e("6644GameView", "Error loading player bitmap: ${e.message}")
        }
    }

    override fun run() {
        while (isPlaying) {
            drawGame()
        }
    }

    private fun drawGame() {
        Log.d("6644GameView", "4")

        if (holder.surface.isValid) {
            Log.d("6644GameView", "5")
            val canvas = holder.lockCanvas()
            Log.d("6644GameView", "6")
            canvas.drawColor(Color.BLACK) // Очистить экран
            playerBitmap?.let {
                Log.d("6644GameView", "7")

                canvas.drawBitmap(it, playerX, playerY, paint)
            } ?: Log.e("6644GameView", "playerBitmap is not initialized")
            Log.d("6644GameView", "8")
            holder.unlockCanvasAndPost(canvas)
        }
    }

    fun updatePlayerPosition(dx: Float, dy: Float) {
        playerX += dx
        playerY += dy
    }

    fun pause() {
        isPlaying = false
        if (::thread.isInitialized) {
            thread.join()
        }
    }

    fun resume() {
        if (!::thread.isInitialized || !isPlaying) {
            isPlaying = true
            thread = Thread(this)
            thread.start()
        }

    }
}
