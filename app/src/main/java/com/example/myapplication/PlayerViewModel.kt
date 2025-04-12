package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel:ViewModel() {
    val publicLogin = LoginActivite().publicLogin
    private val _playerData = MutableStateFlow(PlayerData(publicLogin, 500f,500f))
    val playerData:StateFlow<PlayerData> get() = _playerData
    fun updatePosition(dx:Float, dy:Float){
        _playerData.value=_playerData.value.copy(
            x = _playerData.value.x + dx,
            y = _playerData.value.y + dy
        )
    }


}

//fun updateHeal
//fun updateAmmo