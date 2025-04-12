package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import java.util.concurrent.CopyOnWriteArrayList

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs),
    Runnable {
    val globalLogin = LoginActivite().publicLogin
    private val otherPlayerData = CopyOnWriteArrayList<PlayerData>()
    private lateinit var thread: Thread
    private var isPlaying = true
    private val paint = Paint()
    private val bullets = CopyOnWriteArrayList<Bullet>()


    var playerX = 0f // Начальная позиция игрока по X
    var playerY = 0f // Начальная позиция игрока по Y
    private val playerColor = Color.BLUE // Цвет вашего игрока
    private val otherPlayerColor = Color.GREEN // Цвет других игроков
    private lateinit var mapTexture: Bitmap
    private lateinit var gameMap: Rug

    init {
        mapTexture = BitmapFactory.decodeResource(resources, R.drawable.background)
        gameMap = Rug(x = 0f, y = 0f, width = 1000f, height = 1000f, bitmap = mapTexture)
    }

    override fun run() {
        while (isPlaying) {
            drawGame()
            try {
                Thread.sleep(16)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun drawBullet(canvas: Canvas?, bullet: Bullet) {// Код для отрисовки пули (например:
        canvas?.drawCircle(bullet.x, bullet.y, 10f, paint)
    }

    @Synchronized

    fun updateBullets() {
        val currentTime = System.currentTimeMillis()
        val bulletsToRemove = mutableListOf<Bullet>()

        for (bullet in bullets) {
            if (currentTime - bullet.creationTime > 2000) {
                bulletsToRemove.add(bullet)
            } else {
                bullet.x += bullet.speedX
                bullet.y += bullet.speedY

                // Проверка на столкновение с краями экрана
                if (bullet.x <= 0 || bullet.x >= width) {
                    bullet.speedX *= -1 // Меняем направление по оси X
                }
                if (bullet.y <= 0 || bullet.y >= height) {
                    bullet.speedY *= -1 // Меняем направление по оси Y
                }
            }
        }
        bullets.removeAll(bulletsToRemove)
    }


    fun shootBullet(x: Float, y: Float, speedX: Float, speedY: Float) {
        synchronized(bullets) {
            bullets.add(Bullet(x, y, speedX, speedY, System.currentTimeMillis()))
        }
    }


    private fun drawGame() {
        updateBullets()
        if (holder.surface.isValid) {
            val canvas = holder.lockCanvas()
            canvas.drawColor(Color.BLACK) // Установить черный фон


            val centerX = width / 2f
            val centerY = height / 2f

            val offsetX = -playerX + centerX
            val offsetY = -playerY + centerY

            gameMap.draw(canvas, offsetX, offsetY)

            // Рисуем вашего игрока
//            paint.color = playerColor
//            canvas.drawCircle(centerX, centerY, 20f, paint) // Рисуем вашего игрока как круг

            // Рисуем других игроков
            paint.color = otherPlayerColor
            for (otherPlayer in otherPlayerData) {
                if (otherPlayer.login != globalLogin)
                    canvas.drawCircle(
                        otherPlayer.x + offsetX,
                        otherPlayer.y + offsetY,
                        40f,
                        paint
                    ) // Рисуем других игроков как кружки
            }
            for (bullet in bullets) {
                drawBullet(canvas, bullet)
            }

            holder.unlockCanvasAndPost(canvas)
        }
    }

    // Обновляем данные о других игроках
    fun updateOtherPlayers(players: List<PlayerData>) {
        otherPlayerData.clear() // очищаем старые данные
        otherPlayerData.addAll(players) // добавляем новые данные
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


    fun updatePlayerPosition(dx: Float, dy: Float) {
        playerX += dx
        Log.d("888", "$playerX")
        playerY += dy
        Log.d("888", "$playerY")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mapTexture.recycle() // Убираем текстуру карты из памяти
    }
}

