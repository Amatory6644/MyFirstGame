package com.example.myapplication

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientProvider{
    fun create(): HttpClient{
        return HttpClient{
            install(WebSockets)
            install(ContentNegotiation){
                json(Json { ignoreUnknownKeys = true; prettyPrint = true })
            }
        }
    }
}