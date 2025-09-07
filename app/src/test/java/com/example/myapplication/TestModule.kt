package com.example.myapplication

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

//object TestModule {
//
//    // Тестовый модуль для unit тестов
//    val testModule = module {
//        single<HttpClient> {
//            HttpClient(MockEngine) {
//                engine {
//                    addHandler { request ->
//                        respond(
//                            content = "{\"result\":\"ok\"}",
//                            headers = headersOf("Content-Type" to listOf("application/json"))
//                        )
//                    }
//                }
//                install(ContentNegotiation) {
//                    json(Json { ignoreUnknownKeys = true })
//                }
//                install(WebSockets)
//            }
//        }
//    }
//}
