package com.example.myapplication.di

import com.example.myapplication.GroupViewModel
import com.example.myapplication.LoginViewModel
import com.example.myapplication.PlayerViewModel
import com.example.myapplication.logging.GameLogger
import com.example.myapplication.network.NetworkManager
import com.example.myapplication.repository.GameRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    
    // Game Logger
    single<GameLogger> {
        GameLogger(androidContext())
    }
    
    // HTTP Client
    single<HttpClient> {
        HttpClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json { 
                    ignoreUnknownKeys = true 
                    prettyPrint = true 
                })
            }
        }
    }
    
    // Repository
    single<GameRepository> {
        GameRepository(
            httpClient = get(),
            gameLogger = get(),
            baseUrl = "http://192.168.0.105:8080",
            wsBaseUrl = "ws://192.168.0.105:8080"
        )
    }
    
    // Network Manager
    single<NetworkManager> {
        NetworkManager(
            httpClient = get(),
            gameRepository = get(),
            gameLogger = get()
        )
    }
    
    // ViewModels
    viewModel<LoginViewModel> {
        LoginViewModel(
            gameRepository = get(),
            gameLogger = get()
        )
    }
    
    viewModel<PlayerViewModel> {
        PlayerViewModel(
            gameRepository = get(),
            networkManager = get(),
            gameLogger = get()
        )
    }
    
    viewModel<GroupViewModel> {
        GroupViewModel(
            gameRepository = get(),
            gameLogger = get()
        )
    }
}
