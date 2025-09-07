package com.example.myapplication.repository

import android.util.Log
import com.example.myapplication.PlayerData
import com.example.myapplication.OtherPlayer
import com.example.myapplication.AuthResponse
import com.example.myapplication.InviteRequest
import com.example.myapplication.GroupInfo
import com.example.myapplication.GroupInviteRequest
import com.example.myapplication.GroupInviteResponse
import com.example.myapplication.GroupInviteAcceptRequest
import com.example.myapplication.GroupStats
import com.example.myapplication.GroupStatsInfo
import com.example.myapplication.logging.GameLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameRepository(
    private val httpClient: HttpClient,
    private val gameLogger: GameLogger,
    private val baseUrl: String = "http://192.168.0.105:8080",
    private val wsBaseUrl: String = "ws://192.168.0.105:8080"
) {
    
    companion object {
        private const val TAG = "GameRepository"
    }
    
    // SharedFlow для передачи данных от WebSocket к ViewModel
    private val _playerUpdates = MutableSharedFlow<PlayerData>()
    val playerUpdates = _playerUpdates.asSharedFlow()
    
    private val _otherPlayersUpdates = MutableSharedFlow<List<PlayerData>>()
    val otherPlayersUpdates = _otherPlayersUpdates.asSharedFlow()
    
    private val _connectionStatus = MutableSharedFlow<ConnectionStatus>()
    val connectionStatus = _connectionStatus.asSharedFlow()
    
    private val _gameStartedNotifications = MutableSharedFlow<GameStartedNotification>()
    val gameStartedNotifications = _gameStartedNotifications.asSharedFlow()
    
    // WebSocket сессия для отправки данных
    private var gameWebSocketSession: io.ktor.websocket.WebSocketSession? = null
    
    sealed class ConnectionStatus {
        object Connected : ConnectionStatus()
        object Disconnected : ConnectionStatus()
        data class Error(val message: String, val exception: Exception? = null) : ConnectionStatus()
    }
    
    data class GameStartedNotification(
        val type: String = "game_started",
        val groupId: String,
        val message: String
    )
    
    // HTTP методы с улучшенной обработкой ошибок
    suspend fun authenticateUser(action: String, login: String, password: String): AuthResponse {
        return try {
            Log.d(TAG, "Аутентификация пользователя: $action, $login")
            
            val response = httpClient.post("$baseUrl/auth") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "action" to action,
                    "login" to login,
                    "password" to password
                ))
            }
            
            val authResponse = response.body<AuthResponse>()
            Log.d(TAG, "Ответ аутентификации: $authResponse")
            
            authResponse
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка аутентификации: ${e.message}", e)
            throw NetworkException("Ошибка аутентификации: ${e.message}", e)
        }
    }
    
    suspend fun sendInvite(inviteRequest: InviteRequest): Boolean {
        return try {
            Log.d(TAG, "Отправка приглашения: $inviteRequest")
            
            val response = httpClient.post("$baseUrl/invite") {
                contentType(ContentType.Application.Json)
                setBody(inviteRequest)
            }
            
            val success = response.status.value in 200..299
            Log.d(TAG, "Приглашение отправлено: $success")
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки приглашения: ${e.message}", e)
            throw NetworkException("Ошибка отправки приглашения: ${e.message}", e)
        }
    }
    
    // WebSocket методы с улучшенной обработкой ошибок
    suspend fun connectToGame(route: String, playerData: PlayerData): Flow<Unit> = flow {
        try {
            Log.d(TAG, "🔴 ПОДКЛЮЧЕНИЕ К ИГРЕ: $wsBaseUrl$route")
            Log.d(TAG, "🔴 ДАННЫЕ ИГРОКА: $playerData")
            _connectionStatus.emit(ConnectionStatus.Connected)
            
            httpClient.webSocket("$wsBaseUrl$route") {
                Log.d(TAG, "🟢 WebSocket соединение установлено успешно!")
                
                // Сохраняем сессию для отправки данных
                gameWebSocketSession = this
                
                // Отправляем начальные данные игрока
                send(Json.encodeToString(playerData))
                Log.d(TAG, "Отправлены начальные данные игрока: $playerData")
                
                // Обработка входящих сообщений
                try {
                    Log.d(TAG, "Начинаем слушать входящие сообщения...")
                    Log.d(TAG, "🔵 WebSocket сессия активна: ${this.isActive}")
                    Log.d(TAG, "🔵 Входящий канал: ${incoming}")
                    Log.d(TAG, "🔵 Игрок: ${playerData.login} слушает сообщения...")
                    
                    var messageCount = 0
                    var lastMessageTime = System.currentTimeMillis()
                    
                    // Добавляем периодическую проверку состояния соединения
                    val connectionCheckJob = kotlinx.coroutines.GlobalScope.launch {
                        while (true) {
                            kotlinx.coroutines.delay(5000) // Проверяем каждые 5 секунд
                            val currentTime = System.currentTimeMillis()
                            val timeSinceLastMessage = currentTime - lastMessageTime
                            Log.d(TAG, "🔍 [${playerData.login}] Проверка соединения: сообщений=$messageCount, время с последнего сообщения=${timeSinceLastMessage}ms")
                            
                            if (timeSinceLastMessage > 30000) { // 30 секунд без сообщений
                                Log.w(TAG, "⚠️ [${playerData.login}] Долго нет сообщений от сервера!")
                            }
                        }
                    }
                    
                    for (message in incoming) {
                        messageCount++
                        lastMessageTime = System.currentTimeMillis()
                        Log.d(TAG, "🔵 Получено сообщение #$messageCount от сервера")
                        when (message) {
                            is Frame.Text -> {
                                val text = message.readText()
                                Log.d(TAG, "🔵 ПОЛУЧЕНО СООБЩЕНИЕ ОТ СЕРВЕРА: $text")
                                Log.d(TAG, "Длина сообщения: ${text.length}, первые 50 символов: ${text.take(50)}")
                                processIncomingMessage(text)
                            }
                            is Frame.Close -> {
                                connectionCheckJob.cancel()
                                Log.d(TAG, "🔴 WebSocket соединение закрыто для игрока: ${playerData.login}")
                                Log.d(TAG, "🔴 Всего получено сообщений: $messageCount")
                                Log.d(TAG, "🔴 Время работы соединения: ${System.currentTimeMillis() - lastMessageTime}ms")
                                gameWebSocketSession = null
                                _connectionStatus.emit(ConnectionStatus.Disconnected)
                                break
                            }
                            is Frame.Ping -> {
                                Log.d(TAG, "Получен Ping, отправляем Pong")
                                send(Frame.Pong(message.buffer))
                            }
                            is Frame.Pong -> {
                                Log.d(TAG, "Получен Pong")
                            }
                            else -> { 
                                Log.d(TAG, "Получен неизвестный тип фрейма: ${message::class.simpleName}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "🔴 ОШИБКА ПРИ ОБРАБОТКЕ WebSocket СООБЩЕНИЙ для игрока ${playerData.login}: ${e.message}", e)
                    e.printStackTrace()
                    _connectionStatus.emit(ConnectionStatus.Error("Ошибка обработки сообщений: ${e.message}", e))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔴 ОШИБКА WebSocket СОЕДИНЕНИЯ: ${e.message}", e)
            e.printStackTrace()
            _connectionStatus.emit(ConnectionStatus.Error("Ошибка соединения: ${e.message}", e))
            throw NetworkException("Ошибка WebSocket соединения: ${e.message}", e)
        }
    }
    
    suspend fun sendPlayerUpdate(playerData: PlayerData) {
        try {
            Log.d(TAG, "Отправка обновления позиции игрока: $playerData")
            
            // Отправляем через WebSocket если соединение активно
            gameWebSocketSession?.let { session ->
                val message = Json.encodeToString(playerData)
                session.send(message)
                Log.d(TAG, "Позиция игрока отправлена через WebSocket: $message")
            } ?: run {
                Log.w(TAG, "WebSocket сессия не активна, не можем отправить позицию")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки обновления позиции: ${e.message}", e)
            throw NetworkException("Ошибка отправки обновления позиции: ${e.message}", e)
        }
    }
    
    private suspend fun processIncomingMessage(message: String) {
        try {
            Log.d(TAG, "🟡 НАЧИНАЕМ ОБРАБОТКУ СООБЩЕНИЯ: $message")
            when {
                message.startsWith("player") -> {
                    // Убираем префикс "player" и парсим JSON
                    val jsonPart = message.substring("player".length)
                    Log.d(TAG, "Парсинг данных игрока: $jsonPart")
                    val playerData = Json.decodeFromString<PlayerData>(jsonPart)
                    Log.d(TAG, "Обработаны данные игрока: $playerData")
                    Log.d(TAG, "Эмитим данные игрока ${playerData.login} как данные других игроков")
                    // Эмитим как данные других игроков (одиночный игрок в списке)
                    _otherPlayersUpdates.emit(listOf(playerData))
                }
                message.startsWith("otherPlayers") -> {
                    // Убираем префикс "otherPlayers" и парсим JSON
                    val jsonPart = message.substring("otherPlayers".length)
                    Log.d(TAG, "Парсинг данных других игроков: $jsonPart")
                    val playersData = Json.decodeFromString<OtherPlayer>(jsonPart)
                    Log.d(TAG, "Обработаны данные других игроков: ${playersData.player.size} игроков")
                    _otherPlayersUpdates.emit(playersData.player)
                }
                message.startsWith("[") && message.contains("login") -> {
                    // Обрабатываем массив игроков
                    val playersArray = Json.decodeFromString<List<PlayerData>>(message)
                    Log.d(TAG, "Обработан массив игроков: ${playersArray.size} игроков")
                    _otherPlayersUpdates.emit(playersArray)
                }
                message.contains("\"type\":\"game_started\"") -> {
                    // Обрабатываем уведомление о начале игры
                    val notification = Json.decodeFromString<GameStartedNotification>(message)
                    Log.d(TAG, "Получено уведомление о начале игры: $notification")
                    _gameStartedNotifications.emit(notification)
                }
                else -> {
                    Log.d(TAG, "Неизвестный тип сообщения: $message")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обработки сообщения: ${e.message}", e)
            Log.e(TAG, "Проблемное сообщение: $message")
            // Не прерываем соединение при ошибке парсинга
        }
    }
    
    // Групповые операции
    suspend fun sendGroupInvite(inviteRequest: GroupInviteRequest): GroupInviteResponse {
        return try {
            Log.d(TAG, "Отправка группового приглашения: $inviteRequest")
            
            val response = httpClient.post("$baseUrl/group/invite") {
                contentType(ContentType.Application.Json)
                setBody(inviteRequest)
            }
            
            val inviteResponse = response.body<GroupInviteResponse>()
            Log.d(TAG, "Ответ группового приглашения: $inviteResponse")
            
            inviteResponse
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки группового приглашения: ${e.message}", e)
            throw NetworkException("Ошибка отправки группового приглашения: ${e.message}", e)
        }
    }
    
    suspend fun acceptGroupInvite(acceptRequest: GroupInviteAcceptRequest): Boolean {
        return try {
            Log.d(TAG, "Принятие группового приглашения: $acceptRequest")
            
            val response = httpClient.post("$baseUrl/group/accept") {
                contentType(ContentType.Application.Json)
                setBody(acceptRequest)
            }
            
            val success = response.status.value in 200..299
            Log.d(TAG, "Групповое приглашение принято: $success")
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка принятия группового приглашения: ${e.message}", e)
            throw NetworkException("Ошибка принятия группового приглашения: ${e.message}", e)
        }
    }
    
    suspend fun getAllGroups(): List<GroupInfo> {
        return try {
            Log.d(TAG, "Получение всех групп")
            
            val response = httpClient.get("$baseUrl/groups")
            val groups = response.body<List<GroupInfo>>()
            Log.d(TAG, "Получено групп: ${groups.size}")
            
            groups
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения групп: ${e.message}", e)
            throw NetworkException("Ошибка получения групп: ${e.message}", e)
        }
    }
    
    suspend fun getGroupInvites(playerLogin: String): List<GroupInviteRequest> {
        return try {
            Log.d(TAG, "Получение приглашений для игрока: $playerLogin")
            
            val response = httpClient.get("$baseUrl/group/invites/$playerLogin")
            val invites = response.body<List<GroupInviteRequest>>()
            Log.d(TAG, "Получено приглашений: ${invites.size}")
            
            invites
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения приглашений: ${e.message}", e)
            throw NetworkException("Ошибка получения приглашений: ${e.message}", e)
        }
    }
    
    suspend fun startGroupGame(groupId: String): Boolean {
        return try {
            Log.d(TAG, "Запуск игры для группы: $groupId")
            
            val response = httpClient.post("$baseUrl/group/$groupId/start")
            val success = response.status.value in 200..299
            Log.d(TAG, "Игра для группы запущена: $success")
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска игры для группы: ${e.message}", e)
            throw NetworkException("Ошибка запуска игры для группы: ${e.message}", e)
        }
    }
    
    suspend fun finishGroupGame(groupId: String): Boolean {
        return try {
            Log.d(TAG, "Завершение игры для группы: $groupId")
            
            val response = httpClient.post("$baseUrl/group/$groupId/finish")
            val success = response.status.value in 200..299
            Log.d(TAG, "Игра для группы завершена: $success")
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка завершения игры для группы: ${e.message}", e)
            throw NetworkException("Ошибка завершения игры для группы: ${e.message}", e)
        }
    }
    
    suspend fun createGroup(groupInfo: GroupInfo): GroupInviteResponse {
        return try {
            Log.d(TAG, "Создание группы: $groupInfo")
            
            val response = httpClient.post("$baseUrl/group/create") {
                contentType(ContentType.Application.Json)
                setBody(groupInfo)
            }
            
            val result = response.body<GroupInviteResponse>()
            Log.d(TAG, "Группа создана: $result")
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка создания группы: ${e.message}", e)
            throw NetworkException("Ошибка создания группы: ${e.message}", e)
        }
    }
    
    suspend fun createRoom(createRoomRequest: Map<String, Any>): Map<String, String> {
        return try {
            Log.d(TAG, "Создание комнаты: $createRoomRequest")
            
            val response = httpClient.post("$baseUrl/group/create-room") {
                contentType(ContentType.Application.Json)
                setBody(createRoomRequest)
            }
            
            val roomInfo = response.body<Map<String, String>>()
            Log.d(TAG, "Комната создана: $roomInfo")
            
            roomInfo
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка создания комнаты: ${e.message}", e)
            throw NetworkException("Ошибка создания комнаты: ${e.message}", e)
        }
    }
    
    // Подключение к WebSocket для получения уведомлений из меню
    suspend fun connectForNotifications(playerLogin: String): Flow<Unit> = flow {
        try {
            Log.d(TAG, "Подключение для уведомлений из меню: $wsBaseUrl/game?username=$playerLogin&groupId=menu")
            _connectionStatus.emit(ConnectionStatus.Connected)
            
            httpClient.webSocket("$wsBaseUrl/game?username=$playerLogin&groupId=menu") {
                Log.d(TAG, "WebSocket соединение для меню установлено")
                
                // Обработка входящих сообщений
                try {
                    for (message in incoming) {
                        when (message) {
                            is Frame.Text -> {
                                val text = message.readText()
                                Log.d(TAG, "Получено уведомление из меню: $text")
                                processIncomingMessage(text)
                            }
                            is Frame.Close -> {
                                Log.d(TAG, "WebSocket соединение для меню закрыто")
                                _connectionStatus.emit(ConnectionStatus.Disconnected)
                                break
                            }
                            is Frame.Ping -> {
                                Log.d(TAG, "Получен Ping в меню, отправляем Pong")
                                send(Frame.Pong(message.buffer))
                            }
                            is Frame.Pong -> {
                                Log.d(TAG, "Получен Pong в меню")
                            }
                            else -> { 
                                Log.d(TAG, "Получен неизвестный тип фрейма в меню: ${message::class.simpleName}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при обработке WebSocket уведомлений из меню: ${e.message}", e)
                    _connectionStatus.emit(ConnectionStatus.Error("Ошибка обработки уведомлений: ${e.message}", e))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка WebSocket соединения для меню: ${e.message}", e)
            _connectionStatus.emit(ConnectionStatus.Error("Ошибка соединения для меню: ${e.message}", e))
            throw NetworkException("Ошибка WebSocket соединения для меню: ${e.message}", e)
        }
    }
}

// Кастомный класс исключений для сетевых ошибок
class NetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
