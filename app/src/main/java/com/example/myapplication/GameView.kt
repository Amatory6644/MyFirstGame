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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.repository.GameRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent
import java.util.concurrent.CopyOnWriteArrayList

class GameView(
    context: Context, 
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), Runnable {
    
    private var playerViewModel: PlayerViewModel? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private val gameRepository: GameRepository by lazy { 
        KoinJavaComponent.get(GameRepository::class.java) as GameRepository 
    }
    private var currentPlayerLogin: String = ""
    
    private val otherPlayerData = CopyOnWriteArrayList<PlayerData>()
    private lateinit var thread: Thread
    private var isPlaying = true
    private val paint = Paint()
    private val bullets = CopyOnWriteArrayList<Bullet>()

    var playerX = 500f // Начальная позиция игрока по X
    var playerY = 500f // Начальная позиция игрока по Y
    private val playerColor = Color.BLUE // Цвет вашего игрока
    private val otherPlayerColor = Color.GREEN // Цвет других игроков
    private lateinit var mapTexture: Bitmap
    private lateinit var gameMap: Rug

    init {
        mapTexture = BitmapFactory.decodeResource(resources, R.drawable.background)
        gameMap = Rug(x = 0f, y = 0f, width = 1000f, height = 1000f, bitmap = mapTexture)
        
        // Инициализация с PlayerViewModel будет выполнена после установки ViewModel
    }
    
    fun setPlayerViewModel(viewModel: PlayerViewModel) {
        this.playerViewModel = viewModel
        setupPlayerViewModel()
    }
    
    fun setLifecycleOwner(owner: LifecycleOwner) {
        this.lifecycleOwner = owner
        // Если ViewModel уже установлен, перезапускаем setup
        if (playerViewModel != null) {
            setupPlayerViewModel()
        }
    }
    
    fun setCurrentPlayerLogin(login: String) {
        this.currentPlayerLogin = login
    }
    
    private fun setupPlayerViewModel() {
        playerViewModel?.let { viewModel ->
            lifecycleOwner?.let { owner ->
                // Наблюдаем за позицией игрока
                owner.lifecycleScope.launch {
                    viewModel.playerData.collect { playerData ->
                        playerX = playerData.x
                        playerY = playerData.y
                        Log.d("GameView", "Player position updated: $playerX, $playerY")
                    }
                }
                
                // Наблюдаем за другими игроками
                owner.lifecycleScope.launch {
                    viewModel.otherPlayers.collect { otherPlayers ->
                        updateOtherPlayers(otherPlayers)
                    }
                }
                
                // Наблюдаем за статусом соединения
                owner.lifecycleScope.launch {
                    viewModel.connectionStatus.collect { status ->
                        when (status) {
                            is GameRepository.ConnectionStatus.Connected -> {
                                Log.d("GameView", "Connected to game server")
                            }
                            is GameRepository.ConnectionStatus.Disconnected -> {
                                Log.d("GameView", "Disconnected from game server")
                            }
                            is GameRepository.ConnectionStatus.Error -> {
                                Log.e("GameView", "Connection error: ${status.message}")
                            }
                        }
                    }
                }
            }
        }
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

    private fun drawBullet(canvas: Canvas?, bullet: Bullet) {
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
            paint.color = playerColor
            canvas.drawCircle(centerX, centerY, 20f, paint) // Рисуем вашего игрока как круг

            // Рисуем других игроков с учетом групп
            for (otherPlayer in otherPlayerData) {
                // Определяем цвет игрока в зависимости от группы
                val playerColor = getPlayerColor(otherPlayer.login)
                paint.color = playerColor
                
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
    
    private fun getPlayerColor(playerLogin: String): Int {
        return try {
            // Получаем информацию о группе игрока
            val groups = runBlocking { gameRepository.getAllGroups() }
            val playerGroup = groups.find { group ->
                group.players.contains(playerLogin)
            }
            
            when {
                playerGroup != null -> {
                    if (playerLogin == currentPlayerLogin) {
                        Color.BLACK // Текущий игрок в группе
                    } else {
                        Color.GREEN // Другие игроки в группе
                    }
                }
                else -> otherPlayerColor // Игроки не в группе
            }
        } catch (e: Exception) {
            otherPlayerColor // В случае ошибки используем стандартный цвет
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

    // Обновленная функция для работы с PlayerViewModel
    fun updatePlayerPosition(dx: Float, dy: Float) {
        // Обновляем позицию через ViewModel
        playerViewModel?.updatePosition(dx, dy)
        
        // Локальное обновление для плавности
        playerX += dx
        playerY += dy
        
        Log.d("GameView", "Player position updated: $playerX, $playerY")
    }
    
    // Функция для подключения к игре
    fun connectToGame(route: String) {
        lifecycleOwner?.lifecycleScope?.launch {
            playerViewModel?.connectToGame(route)
        }
    }
    
    // Функция для отключения от игры
    fun disconnectFromGame() {
        playerViewModel?.disconnectFromGame()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mapTexture.recycle() // Убираем текстуру карты из памяти
        disconnectFromGame() // Отключаемся от игры при закрытии
    }
}

