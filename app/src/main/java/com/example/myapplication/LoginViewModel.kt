package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.logging.GameLogger
import com.example.myapplication.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val gameRepository: GameRepository,
    private val gameLogger: GameLogger
) : ViewModel() {
    
    val loginRegex = "^[a-zA-Z]+$".toRegex()
    val passwordRegex = "^[0-9]+$".toRegex()
    
    private val _state = MutableStateFlow<State>(State.Empty)
    val state: StateFlow<State> = _state.asStateFlow()
    
    private val _authResult = MutableStateFlow<AuthResponse?>(null)
    val authResult: StateFlow<AuthResponse?> = _authResult.asStateFlow()
    
    fun checkLogin(login: String, password: String) {
        viewModelScope.launch {
            gameLogger.logPlayerAction(login, "LOGIN_ATTEMPT", "password_length=${password.length}")
            
            if (loginRegex.matches(login) && passwordRegex.matches(password)) {
                _state.value = State.Loading
                
                try {
                    val response = gameRepository.authenticateUser("login", login, password)
                    _authResult.value = response
                    
                    if (response.status == "LOGIN_SUCCESS") {
                        _state.value = State.Success
                        gameLogger.logPlayerAction(login, "LOGIN_SUCCESS")
                    } else {
                        _state.value = State.Error(response.message)
                        gameLogger.logPlayerAction(login, "LOGIN_FAILED", response.message)
                    }
                } catch (e: Exception) {
                    _state.value = State.Error("Ошибка сети: ${e.message}")
                    gameLogger.logPlayerAction(login, "LOGIN_ERROR", e.message)
                }
            } else {
                _state.value = State.Error("Неправильный логин или пароль")
                gameLogger.logPlayerAction(login, "LOGIN_VALIDATION_FAILED")
            }
        }
    }
    
    fun registerUser(login: String, password: String) {
        viewModelScope.launch {
            gameLogger.logPlayerAction(login, "REGISTER_ATTEMPT", "password_length=${password.length}")
            
            if (loginRegex.matches(login) && passwordRegex.matches(password)) {
                _state.value = State.Loading
                
                try {
                    val response = gameRepository.authenticateUser("register", login, password)
                    _authResult.value = response
                    
                    if (response.status == "REGISTER_SUCCESS") {
                        _state.value = State.Success
                        gameLogger.logPlayerAction(login, "REGISTER_SUCCESS")
                    } else {
                        _state.value = State.Error(response.message)
                        gameLogger.logPlayerAction(login, "REGISTER_FAILED", response.message)
                    }
                } catch (e: Exception) {
                    val errorMessage = when {
                        e.message?.contains("Connect timeout") == true -> "Сервер недоступен. Проверьте подключение к интернету."
                        e.message?.contains("timeout") == true -> "Превышено время ожидания ответа от сервера."
                        e.message?.contains("Network") == true -> "Ошибка сети. Проверьте подключение."
                        else -> "Ошибка сети: ${e.message}"
                    }
                    _state.value = State.Error(errorMessage)
                    gameLogger.logPlayerAction(login, "REGISTER_ERROR", e.message)
                }
            } else {
                val errorMessage = when {
                    !loginRegex.matches(login) -> "Логин должен содержать только английские буквы."
                    !passwordRegex.matches(password) -> "Пароль должен содержать только цифры."
                    else -> "Неправильный логин или пароль"
                }
                _state.value = State.Error(errorMessage)
                gameLogger.logPlayerAction(login, "REGISTER_VALIDATION_FAILED")
            }
        }
    }
    
    fun resetState() {
        _state.value = State.Empty
        _authResult.value = null
    }
}
