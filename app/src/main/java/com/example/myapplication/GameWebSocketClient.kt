package com.example.myapplication

import android.util.Log
import io.ktor.client.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

//
//// Настройка Json с serializersModule
//val json = Json {
//    serializersModule = serializersModule
//    prettyPrint = true
//    isLenient = true
//    ignoreUnknownKeys = true
//    classDiscriminator = "type"
//}
//
//// Определение модуля сериализации
//val module = SerializersModule {
//    polymorphic(PlayerAction::class) {
//        subclass(PlayerAction.MoveAction::class)
//        subclass(PlayerAction.AttackAction::class)
////        subclass(UnknownAction::class)
//    }
//}
//
//class GameWebSocketClient(private val uri: String, private val gameView: GameView) {
//    private val client = HttpClient(CIO) {
//        install(WebSockets)
//        install(ContentNegotiation) {
//            json(json) // Используем настроенный JSON
//        }
//    }
//
//    private var session: DefaultWebSocketSession? = null
//
//    suspend fun connect() {
//        val host = "192.168.0.105"
//        val port = 8080
//        val path = "/game"
//
//        client.webSocket(method = HttpMethod.Get, host = host, port = port, path = path) {
//            session = this
//
//            for (message in incoming) {
//                when (message) {
//                    is Frame.Text -> {
//                        val playerData = parsePlayerData(message.readText())
//                        gameView.updatePlayers(playerData)
//                    }
//
//                    is Frame.Close -> {
//                        println("Connection closed")
//                    }
//
//                    is Frame.Binary -> TODO()
//                    is Frame.Ping -> TODO()
//                    is Frame.Pong -> TODO()
//                }
//            }
//        }
//    }
//
//    private fun parsePlayerData(jsonString: String): Map<String, PlayerData> {
//        return try {
//            json.decodeFromString(
//                MapSerializer(String.serializer(), PlayerData.serializer()),
//                jsonString
//            )
//        } catch (e: Exception) {
//            println("Error parsing player data: ${e.message}")
//            emptyMap()
//        }
//    }
//
//    suspend fun close() {
//        session?.close()
//        client.close()
//    }
//}
//__________________________
//class GameWebSocketClient(private val uri: String, private val gameView: GameView) {
//    private val client = HttpClient(CIO) {
//        install(WebSockets)
//        install(ContentNegotiation) {
//            json(Json {
//                prettyPrint = true
//                isLenient = true
//                ignoreUnknownKeys = true
//            })
//        }
//    }
//
//    private var session: DefaultWebSocketSession? = null
//
//    suspend fun connect() {
//        // Разбор URI для получения хоста, порта и пути
//        val host = "192.168.0.105"  //uri.split(":")[1].removePrefix("ws://")
//        //val portAndPath = uri.split(":")[2]
//        val port =  8080//portAndPath.split("/")[0].toInt() // Получаем только порт
//        val path = "/game"//"/${portAndPath.split("/").drop(1).joinToString("/")}" // Получаем путь после порта
//        Log.d("6644", "connect")
//
//        client.webSocket(method = HttpMethod.Get, host = host, port = port, path = path) {
//            session = this
//            println("Connected to server at ws://$host:$port$path") //$uri
//
//            Log.d("6644", "Connected to server at ws $host:$port$path")
//            Log.d("6644", "connect ready")
//            Log.d("6644", "session $session")
//            Log.d("6644", "client $client")
//
//
//            // Обработка входящих сообщений
//            for (message in incoming) {
//                Log.d("6644", "1Received message type: ${message.frameType}")
//                when (message) {
//                    is Frame.Text -> {
//                        Log.d("6644", "2Received message type: ${message.frameType}")
//                        val playerData = parsePlayerData(message.readText())
//                        Log.d("6644", "text $playerData")
//
//                        gameView.updatePlayers(playerData)
//                        Log.d("6644", "text $gameView")
//                        Log.d("6644", "connect")
//
//                    }
//                    // Обработка других типов сообщений
//                    is Frame.Binary -> {
//                        // Обработка бинарных данных, если это необходимо
//                    }
//                    is Frame.Close -> {
//                        Log.d("6644", "connect end")
//
//                        println("Connection closed")
//                    }
//                    is Frame.Ping -> {
//                        // Обработка ping-сообщений
//                    }
//                    is Frame.Pong -> {
//                        // Обработка pong-сообщений
//                    }
//                }
//            }
//        }
//    }
//
//    private fun parsePlayerData(jsonString: String): Map<String, PlayerData> {
//        return Json.decodeFromString(MapSerializer(String.serializer(), PlayerData.serializer()), jsonString)
//    }
//
//    suspend fun close() {
//        session?.close()
//        client.close()
//    }
//}
