package com.example.myapplication

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

class WebSoketManager(private val baseUrl: String = "ws://192.168.0.105:8080") {
    private val client = HttpClientProvider.create()
    private var session: DefaultClientWebSocketSession? = null
    private var myLogin: String = ""

    // StateFlows для UI/ViewModel
    private val _otherPlayers = MutableStateFlow<List<PlayerData>>(emptyList())
    val otherPlayers: StateFlow<List<PlayerData>> = _otherPlayers.asStateFlow()

    private val _myPlayer = MutableStateFlow<PlayerData?>(null)
    val myPlayer: StateFlow<PlayerData?> = _myPlayer.asStateFlow()

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    // Подключиться в корутине, передав route (например "/game") и свой логин
    suspend fun connect(route: String, login: String) {
        myLogin = login
        _status.value = ConnectionStatus.Connecting
        client.webSocket("$baseUrl$route") {
            session = this
            _status.value = ConnectionStatus.Connected

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        handleText(frame.readText())
                    }
                }
            } catch (e: Exception) {
                _status.value = ConnectionStatus.Error(e)
                Log.e("WebSoketManager", "incoming loop error", e)
            } finally {
                session = null
                _status.value = ConnectionStatus.Disconnected
            }
        }
    }

    // Универсальная обработка текстовых сообщений
    private fun handleText(text: String) {
        try {
            // Сначала попробуем стандартный JSON с полем "type"
            val jsonElem = try {
                Json.parseToJsonElement(text)
            } catch (_: Exception) {
                null
            }

            if (jsonElem is JsonObject && jsonElem["type"] != null) {
                val type = jsonElem["type"]!!.jsonPrimitive.content
                when (type) {
                    "players_list" -> {
                        val data = jsonElem["data"]!!
                        val players = Json.decodeFromJsonElement<List<PlayerData>>(data)
                        _otherPlayers.value = players.filter { it.login != myLogin }
                    }
                    "player_update" -> {
                        val data = jsonElem["data"]!!
                        val player = Json.decodeFromJsonElement<PlayerData>(data)
                        applyPlayerUpdate(player)
                    }
                    // другие типы: auth_result, command_ack и т.д.
                    else -> {
                        Log.d("WebSoketManager", "Unhandled typed message: $type")
                    }
                }
                return
            }

// Fallback: старый формат, где сообщение начинается с префикса "player" или "otherPlayers"

            if (text.startsWith("player")) {
                val payload = text.removePrefix("player").trim()
                val player = Json.decodeFromString<PlayerData>(payload)
                applyPlayerUpdate(player)
                return
            }
            if (text.startsWith("otherPlayers")) {
                val payload = text.removePrefix("otherPlayers").trim()
                // если сервер присылает объект OtherPlayer или список, попробуем оба варианта
                try {
                    val players = Json.decodeFromString<List<PlayerData>>(payload)
                    _otherPlayers.value = players.filter { it.login != myLogin }
                } catch (e: Exception) {
                    try {
                        val other = Json.decodeFromString<OtherPlayer>(payload)
                        _otherPlayers.value = other.player.filter { it.login != myLogin }
                    } catch (ex: Exception) {
                        Log.e("WebSoketManager", "Can't parse otherPlayers payload", ex)
                    }
                }
                return
            }

            // Неизвестный/непарсируемый формат
            Log.w("WebSoketManager", "Unhandled text frame: $text")

        } catch (e: Exception) {
            Log.e("WebSoketManager", "handleText error", e)
        }
    }

    // Вставляет/обновляет игрока в списке otherPlayers или обновляет myPlayer
    private fun applyPlayerUpdate(player: PlayerData) {
        if (player.login == myLogin) {
            _myPlayer.value = player
            return
        }
        val list = _otherPlayers.value.toMutableList()
        val idx = list.indexOfFirst { it.login == player.login }
        if (idx >= 0) list[idx] = player else list.add(player)
        _otherPlayers.value = list
    }

    // Отправка своего обновления (вызывайте из ViewModel)
    suspend fun sendPlayerData(playerData: PlayerData) {
        val obj = buildJsonObject {
            put("type", JsonPrimitive("player_update"))
            put("data", Json.encodeToJsonElement(playerData))
        }
        val text = obj.toString()
        try {
            session?.send(Frame.Text(text))
        } catch (e: Exception) {
            Log.e("WebSoketManager", "send error", e)
            throw e
        }
    }

    // Закрыть соединение (и клиент при необходимости)
    suspend fun close() {
        try { session?.close(CloseReason(CloseReason.Codes.NORMAL, "by client")) } catch (_: Exception) {}
        try { client.close() } catch (_: Exception) {}
        session = null
        _status.value = ConnectionStatus.Disconnected
    }

    sealed class ConnectionStatus {
        object Disconnected : ConnectionStatus()
        object Connecting : ConnectionStatus()
        object Connected : ConnectionStatus()
        data class Error(val throwable: Throwable) : ConnectionStatus()
    }
}
