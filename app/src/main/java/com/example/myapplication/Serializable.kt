package com.example.myapplication

import kotlinx.serialization.Serializable

@Serializable
data class Bullet(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    val creationTime: Long
)


@Serializable
data class PlayerData(
    var login: String,
    var x: Float,
    var y: Float
)


@Serializable
data class OtherPlayer(val player: List<PlayerData>
)

val players = mutableMapOf<String, PlayerData>()

@Serializable
data class InviteRequest(val sender: String?, val receiver: String?)

@Serializable
data class AuthResponse(val status: String, val message: String)

