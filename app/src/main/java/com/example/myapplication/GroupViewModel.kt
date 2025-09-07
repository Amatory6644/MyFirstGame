package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.logging.GameLogger
import com.example.myapplication.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class GroupViewModel(
    private val gameRepository: GameRepository,
    private val gameLogger: GameLogger
) : ViewModel() {
    
    private val _groups = MutableStateFlow<List<GroupInfo>>(emptyList())
    val groups: StateFlow<List<GroupInfo>> = _groups.asStateFlow()
    
    private val _invites = MutableStateFlow<List<GroupInviteRequest>>(emptyList())
    val invites: StateFlow<List<GroupInviteRequest>> = _invites.asStateFlow()
    
    private val _currentGroup = MutableStateFlow<GroupInfo?>(null)
    val currentGroup: StateFlow<GroupInfo?> = _currentGroup.asStateFlow()
    
    private val _state = MutableStateFlow<GroupState>(GroupState.Empty)
    val state: StateFlow<GroupState> = _state.asStateFlow()
    
    sealed class GroupState {
        object Empty : GroupState()
        object Loading : GroupState()
        data class Success(val message: String) : GroupState()
        data class Error(val message: String) : GroupState()
    }
    
    fun loadGroups() {
        viewModelScope.launch {
            _state.value = GroupState.Loading
            try {
                val groupsList = gameRepository.getAllGroups()
                _groups.value = groupsList
                _state.value = GroupState.Success("Группы загружены")
                gameLogger.log("INFO", "GroupViewModel", "Загружено групп: ${groupsList.size}")
            } catch (e: Exception) {
                _state.value = GroupState.Error("Ошибка загрузки групп: ${e.message}")
                gameLogger.log("ERROR", "GroupViewModel", "Ошибка загрузки групп: ${e.message}")
            }
        }
    }
    
    fun loadInvites(playerLogin: String) {
        viewModelScope.launch {
            try {
                val invitesList = gameRepository.getGroupInvites(playerLogin)
                _invites.value = invitesList
                gameLogger.log("INFO", "GroupViewModel", "Загружено приглашений: ${invitesList.size}", playerLogin)
            } catch (e: Exception) {
                gameLogger.log("ERROR", "GroupViewModel", "Ошибка загрузки приглашений: ${e.message}", playerLogin)
            }
        }
    }
    
    fun createGroup(creatorLogin: String, invitedPlayers: List<String>): String {
        val groupId = UUID.randomUUID().toString()
        gameLogger.log("INFO", "GroupViewModel", "Создание группы с ID: $groupId", creatorLogin)
        
        viewModelScope.launch {
            try {
                _state.value = GroupState.Loading
                
                // Сначала создаем группу на сервере
                val createGroupRequest = mapOf(
                    "groupId" to groupId,
                    "creator" to creatorLogin,
                    "groupName" to "Команда $creatorLogin"
                )
                
                gameLogger.log("INFO", "GroupViewModel", "Отправка запроса на создание группы: $createGroupRequest", creatorLogin)
                
                val serverResponse = gameRepository.createRoom(createGroupRequest)
                gameLogger.log("INFO", "GroupViewModel", "Группа создана на сервере: $groupId", creatorLogin)
                
                // Затем отправляем приглашения
                if (invitedPlayers.isNotEmpty()) {
                    val inviteRequest = GroupInviteRequest(
                        sender = creatorLogin,
                        receivers = invitedPlayers,
                        groupId = groupId,
                        message = "Приглашение в группу"
                    )
                    
                    gameLogger.log("INFO", "GroupViewModel", "Отправка приглашений с groupId: $groupId", creatorLogin)
                    
                    val response = gameRepository.sendGroupInvite(inviteRequest)
                    gameLogger.log("INFO", "GroupViewModel", "Приглашения отправлены: $groupId", creatorLogin)
                }
                
                // Обновляем список групп с сервера
                loadGroups()
                
                // Устанавливаем текущую группу
                val createdGroup = _groups.value.find { it.groupId == groupId }
                if (createdGroup != null) {
                    _currentGroup.value = createdGroup
                }
                
                _state.value = GroupState.Success("Группа создана")
                
            } catch (e: Exception) {
                _state.value = GroupState.Error("Ошибка создания группы: ${e.message}")
                gameLogger.log("ERROR", "GroupViewModel", "Ошибка создания группы: ${e.message}", creatorLogin)
            }
        }
        
        return groupId
    }
    
    fun acceptInvite(playerLogin: String, groupId: String) {
        viewModelScope.launch {
            try {
                val acceptRequest = GroupInviteAcceptRequest(
                    playerLogin = playerLogin,
                    groupId = groupId,
                    accept = true
                )
                
                val success = gameRepository.acceptGroupInvite(acceptRequest)
                if (success) {
                    // Обновляем список приглашений
                    val currentInvites = _invites.value.toMutableList()
                    currentInvites.removeAll { it.groupId == groupId }
                    _invites.value = currentInvites
                    
                    // Обновляем группы
                    loadGroups()
                    
                    _state.value = GroupState.Success("Приглашение принято")
                    gameLogger.log("INFO", "GroupViewModel", "Приглашение принято: $groupId", playerLogin)
                }
            } catch (e: Exception) {
                _state.value = GroupState.Error("Ошибка принятия приглашения: ${e.message}")
                gameLogger.log("ERROR", "GroupViewModel", "Ошибка принятия приглашения: ${e.message}", playerLogin)
            }
        }
    }
    
    fun declineInvite(playerLogin: String, groupId: String) {
        viewModelScope.launch {
            try {
                val acceptRequest = GroupInviteAcceptRequest(
                    playerLogin = playerLogin,
                    groupId = groupId,
                    accept = false
                )
                
                val success = gameRepository.acceptGroupInvite(acceptRequest)
                if (success) {
                    // Удаляем приглашение из списка
                    val currentInvites = _invites.value.toMutableList()
                    currentInvites.removeAll { it.groupId == groupId }
                    _invites.value = currentInvites
                    
                    _state.value = GroupState.Success("Приглашение отклонено")
                    gameLogger.log("INFO", "GroupViewModel", "Приглашение отклонено: $groupId", playerLogin)
                }
            } catch (e: Exception) {
                _state.value = GroupState.Error("Ошибка отклонения приглашения: ${e.message}")
                gameLogger.log("ERROR", "GroupViewModel", "Ошибка отклонения приглашения: ${e.message}", playerLogin)
            }
        }
    }
    
    fun startGroupGame(groupId: String) {
        viewModelScope.launch {
            try {
                val success = gameRepository.startGroupGame(groupId)
                if (success) {
                    // Обновляем статус группы
                    val currentGroups = _groups.value.toMutableList()
                    val groupIndex = currentGroups.indexOfFirst { it.groupId == groupId }
                    if (groupIndex != -1) {
                        currentGroups[groupIndex] = currentGroups[groupIndex].copy(status = "playing")
                        _groups.value = currentGroups
                        _currentGroup.value = currentGroups[groupIndex]
                    }
                    
                    _state.value = GroupState.Success("Игра запущена")
                    gameLogger.log("INFO", "GroupViewModel", "Игра запущена: $groupId")
                }
            } catch (e: Exception) {
                _state.value = GroupState.Error("Ошибка запуска игры: ${e.message}")
                gameLogger.log("ERROR", "GroupViewModel", "Ошибка запуска игры: ${e.message}")
            }
        }
    }
    
    fun finishGroupGame(groupId: String) {
        viewModelScope.launch {
            try {
                val success = gameRepository.finishGroupGame(groupId)
                if (success) {
                    // Обновляем статус группы
                    val currentGroups = _groups.value.toMutableList()
                    val groupIndex = currentGroups.indexOfFirst { it.groupId == groupId }
                    if (groupIndex != -1) {
                        currentGroups[groupIndex] = currentGroups[groupIndex].copy(status = "finished")
                        _groups.value = currentGroups
                        _currentGroup.value = null
                    }
                    
                    _state.value = GroupState.Success("Игра завершена")
                    gameLogger.log("INFO", "GroupViewModel", "Игра завершена: $groupId")
                }
            } catch (e: Exception) {
                _state.value = GroupState.Error("Ошибка завершения игры: ${e.message}")
                gameLogger.log("ERROR", "GroupViewModel", "Ошибка завершения игры: ${e.message}")
            }
        }
    }
    
    fun joinGroup(groupId: String) {
        val group = _groups.value.find { it.groupId == groupId }
        if (group != null) {
            _currentGroup.value = group
            gameLogger.log("INFO", "GroupViewModel", "Присоединился к группе: $groupId")
        }
    }
    
    fun leaveGroup() {
        _currentGroup.value = null
        gameLogger.log("INFO", "GroupViewModel", "Покинул группу")
    }
    
    fun resetState() {
        _state.value = GroupState.Empty
    }
}
