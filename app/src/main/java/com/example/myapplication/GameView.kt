package com.example.myapplication


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView



class GameView(context: Context) : SurfaceView(context), Runnable, SurfaceHolder.Callback {
    private val paint: Paint = Paint()
    private var isRunning = false
    private lateinit var thread: Thread
    var playerX = 100f
    var playerY = 100f
    var playerSize = 50f

    private var joystickCenterX = 300f
    private var joystickCenterY = 300f
    private var joystickRadius = 100f
    private var joystickStickX = joystickCenterX
    private var joystickStickY = joystickCenterY

    // Переменные для направления движения
    private var moveDirectionX = 0f
    private var moveDirectionY = 0f

    // Фиксированная скорость движения
    private val playerSpeed = 10f

    init {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        holder.addCallback(this) // Добавляем SurfaceHolder.Callback
    }

    override fun run() {
        while (isRunning) {
            update()
            draw()
        }
    }

    private fun update() {
        // Обновление позиции персонажа в зависимости от направления движения
        playerX += moveDirectionX
        playerY += moveDirectionY
    }

    private fun draw() {
        val canvas: Canvas? = holder.lockCanvas()
        if (canvas != null) {
            try {
                canvas.drawColor(Color.BLACK)
                canvas.drawRect(playerX, playerY, playerX + playerSize, playerY + playerSize, paint)

                // Рисуем джойстик
                paint.color = Color.GRAY
                canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, paint)

                paint.color = Color.RED
                canvas.drawCircle(joystickStickX, joystickStickY, joystickRadius / 3, paint)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - joystickCenterX
                val dy = event.y - joystickCenterY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Настройка направления на основе положения джойстика
                if (distance > joystickRadius) {
                    joystickStickX = joystickCenterX + (dx / distance * joystickRadius)
                    joystickStickY = joystickCenterY + (dy / distance * joystickRadius)
                } else {
                    joystickStickX = event.x
                    joystickStickY = event.y
                }

                // Устанавливаем направление движения
                moveDirectionX = (dx / distance) * playerSpeed
                moveDirectionY = (dy / distance) * playerSpeed
            }

            MotionEvent.ACTION_UP -> {
                joystickStickX = joystickCenterX
                joystickStickY = joystickCenterY

                // Остановить движение
                moveDirectionX = 0f
                moveDirectionY = 0f
            }
        }
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        thread = Thread(this)
        thread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Здесь обычно ничего не делаем
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        thread.join()
    }

    fun stop() {
        isRunning = false
    }
}
