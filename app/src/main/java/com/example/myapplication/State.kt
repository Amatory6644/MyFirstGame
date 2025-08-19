package com.example.myapplication

sealed class State{
    object Empty:State()
    object Loading:State()
    object Success:State()
    data class Error(val message:String):State()
}