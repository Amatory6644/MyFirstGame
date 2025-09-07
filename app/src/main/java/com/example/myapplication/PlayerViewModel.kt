package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.logging.GameLogger
import com.example.myapplication.network.NetworkManager
import com.example.myapplication.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val gameRepository: GameRepository,
    private val networkManager: NetworkManager,
    private val gameLogger: GameLogger
) : ViewModel() {
    
    private val _playerData = MutableStateFlow(PlayerData("", 500f, 500f))
    val playerData: StateFlow<PlayerData> = _playerData.asStateFlow()
    
    private val _otherPlayers = MutableStateFlow<List<PlayerData>>(emptyList())
    val otherPlayers: StateFlow<List<PlayerData>> = _otherPlayers.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow<GameRepository.ConnectionStatus>(GameRepository.ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<GameRepository.ConnectionStatus> = _connectionStatus.asStateFlow()
    
    init {
        // Слушаем обновления от Repository
        viewModelScope.launch {
            gameRepository.playerUpdates.collect { playerData ->
                // Получаем данные других игроков от сервера
                // НЕ обновляем _playerData, так как это данные других игроков
                gameLogger.log("DEBUG", "PlayerViewModel", "Получены данные игрока от сервера: ${playerData.login}")
            }
        }
        
        viewModelScope.launch {
            gameRepository.otherPlayersUpdates.collect { otherPlayers ->
                gameLogger.log("DEBUG", "PlayerViewModel", "🟢 ПОЛУЧЕНЫ ДАННЫЕ ДРУГИХ ИГРОКОВ: ${otherPlayers.size} игроков")
                otherPlayers.forEach { player ->
                    gameLogger.log("DEBUG", "PlayerViewModel", "Игрок: ${player.login} в позиции (${player.x}, ${player.y})")
                }
                _otherPlayers.value = otherPlayers
            }
        }
        
        viewModelScope.launch {
            gameRepository.connectionStatus.collect { status ->
                _connectionStatus.value = status
            }
        }
        
        // Слушаем обновления позиции от NetworkManager (локальные обновления)
        viewModelScope.launch {
            networkManager.positionUpdates.collect { playerData ->
                // Обновляем локальное состояние только для текущего игрока
                if (playerData.login == _playerData.value.login) {
                    _playerData.value = playerData
                }
            }
        }
    }
    
    fun updatePosition(dx: Float, dy: Float) {
        val newPosition = _playerData.value.copy(
            x = _playerData.value.x + dx,
            y = _playerData.value.y + dy
        )
        _playerData.value = newPosition
        
        // Логируем движение для античита
        gameLogger.logPlayerAction(
            newPosition.login,
            "MOVE",
            "dx=$dx,dy=$dy,new_x=${newPosition.x},new_y=${newPosition.y}"
        )
        
        // Отправляем обновление через NetworkManager
        viewModelScope.launch {
            networkManager.sendPlayerPosition(newPosition)
        }
    }
    
    fun setPlayerLogin(login: String) {
        _playerData.value = _playerData.value.copy(login = login)
    }
    
    fun connectToGame(route: String) {
        gameLogger.logPlayerAction(_playerData.value.login, "CONNECT", "route=$route")
        viewModelScope.launch {
            networkManager.connectToGame(route, _playerData.value)
        }
    }
    
    fun disconnectFromGame() {
        gameLogger.logPlayerAction(_playerData.value.login, "DISCONNECT")
        networkManager.disconnect()
    }
    
    fun isReconnecting(): Boolean = networkManager.isReconnecting()
    
    fun getReconnectionAttempt(): Int = networkManager.getReconnectionAttempt()
    
    fun getMaxReconnectionAttempts(): Int = networkManager.getMaxReconnectionAttempts()
    
    override fun onCleared() {
        super.onCleared()
        networkManager.disconnect()
    }
}

//fun updateHeal
//fun updateAmmo