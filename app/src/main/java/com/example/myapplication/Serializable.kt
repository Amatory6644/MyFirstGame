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

@Serializable
data class GroupInfo(
    val groupId: String,
    val players: MutableList<String>,
    val maxPlayers: Int = 5,
    var status: String = "waiting" // waiting, playing, finished
)

@Serializable
data class GroupInviteRequest(
    val sender: String,
    val receivers: List<String>,
    val groupId: String,
    val message: String = "Приглашение в игру"
)

@Serializable
data class GroupInviteResponse(
    val status: String,
    val groupId: String,
    val message: String
)

@Serializable
data class GroupInviteAcceptRequest(
    val playerLogin: String,
    val groupId: String,
    val accept: Boolean
)

@Serializable
data class GroupStats(
    val totalGroups: Int,
    val groups: Map<String, GroupStatsInfo>
)

@Serializable
data class GroupStatsInfo(
    val groupId: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val status: String,
    val players: List<String>
)

@Serializable
data class PlayerGroupStatus(
    val login: String,
    val groupId: String?,
    val status: String // "online", "in_group", "in_game"
)

