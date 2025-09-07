package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.logging.GameLogger
import com.example.myapplication.repository.GameRepository
import com.example.myapplication.service.GameNotificationService
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class InvitesActivity : AppCompatActivity() {
    
    private val gameRepository: GameRepository by inject()
    private val gameLogger: GameLogger by inject()
    
    private lateinit var etPlayerLogin: EditText
    private lateinit var btnSendInvite: Button
    private lateinit var btnBack: Button
    private lateinit var rvInvites: RecyclerView
    
    private lateinit var invitesAdapter: InvitesAdapter
    private var playerLogin: String = ""
    private var currentGroupId: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invites)
        
        playerLogin = intent.getStringExtra("LOGIN_EXTRA") ?: ""
        currentGroupId = intent.getStringExtra("GROUP_ID") ?: ""
        
        // Запускаем сервис уведомлений
        startNotificationService()
        
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadInvites()
    }
    
    private fun initializeViews() {
        etPlayerLogin = findViewById(R.id.et_player_login)
        btnSendInvite = findViewById(R.id.btn_send_invite)
        btnBack = findViewById(R.id.btn_back)
        rvInvites = findViewById(R.id.rv_invites)
    }
    
    private fun setupRecyclerView() {
        invitesAdapter = InvitesAdapter(
            onAcceptInvite = { invite ->
                acceptInvite(invite)
            },
            onDeclineInvite = { invite ->
                declineInvite(invite)
            }
        )
        
        rvInvites.layoutManager = LinearLayoutManager(this)
        rvInvites.adapter = invitesAdapter
    }
    
    private fun setupClickListeners() {
        btnSendInvite.setOnClickListener {
            sendInvite()
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun sendInvite() {
        val targetPlayer = etPlayerLogin.text.toString().trim()
        
        if (targetPlayer.isEmpty()) {
            Toast.makeText(this, "Введите логин игрока", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (targetPlayer == playerLogin) {
            Toast.makeText(this, "Нельзя пригласить самого себя", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val inviteRequest = GroupInviteRequest(
                    sender = playerLogin,
                    receivers = listOf(targetPlayer),
                    groupId = currentGroupId,
                    message = "Приглашение в группу"
                )
                
                val result = gameRepository.sendGroupInvite(inviteRequest)
                
                if (result.status == "SUCCESS") {
                    Toast.makeText(this@InvitesActivity, "Приглашение отправлено!", Toast.LENGTH_SHORT).show()
                    gameLogger.logPlayerAction(playerLogin, "INVITE_SENT", "target=$targetPlayer")
                    etPlayerLogin.text.clear()
                } else {
                    Toast.makeText(this@InvitesActivity, "Ошибка отправки: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InvitesActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                gameLogger.logPlayerAction(playerLogin, "INVITE_SEND_ERROR", e.message)
            }
        }
    }
    
    private fun acceptInvite(invite: GroupInviteRequest) {
        lifecycleScope.launch {
            try {
                val acceptRequest = GroupInviteAcceptRequest(
                    playerLogin = playerLogin,
                    groupId = invite.groupId,
                    accept = true
                )
                
                val result = gameRepository.acceptGroupInvite(acceptRequest)
                
                if (result) {
                    Toast.makeText(this@InvitesActivity, "Приглашение принято!", Toast.LENGTH_SHORT).show()
                    gameLogger.logPlayerAction(playerLogin, "INVITE_ACCEPTED", "group=${invite.groupId}")
                    loadInvites() // Обновляем список
                } else {
                    Toast.makeText(this@InvitesActivity, "Ошибка принятия приглашения", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InvitesActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                gameLogger.logPlayerAction(playerLogin, "INVITE_ACCEPT_ERROR", e.message)
            }
        }
    }
    
    private fun declineInvite(invite: GroupInviteRequest) {
        lifecycleScope.launch {
            try {
                val declineRequest = GroupInviteAcceptRequest(
                    playerLogin = playerLogin,
                    groupId = invite.groupId,
                    accept = false
                )
                
                val result = gameRepository.acceptGroupInvite(declineRequest)
                
                if (result) {
                    Toast.makeText(this@InvitesActivity, "Приглашение отклонено", Toast.LENGTH_SHORT).show()
                    gameLogger.logPlayerAction(playerLogin, "INVITE_DECLINED", "group=${invite.groupId}")
                    loadInvites() // Обновляем список
                } else {
                    Toast.makeText(this@InvitesActivity, "Ошибка отклонения приглашения", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InvitesActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                gameLogger.logPlayerAction(playerLogin, "INVITE_DECLINE_ERROR", e.message)
            }
        }
    }
    
    private fun loadInvites() {
        lifecycleScope.launch {
            try {
                val invites = gameRepository.getGroupInvites(playerLogin)
                invitesAdapter.updateInvites(invites)
            } catch (e: Exception) {
                Toast.makeText(this@InvitesActivity, "Ошибка загрузки приглашений: ${e.message}", Toast.LENGTH_SHORT).show()
                gameLogger.logPlayerAction(playerLogin, "INVITES_LOAD_ERROR", e.message)
            }
        }
    }
    
    private fun startNotificationService() {
        if (playerLogin.isNotEmpty()) {
            val serviceIntent = Intent(this, GameNotificationService::class.java)
            serviceIntent.action = GameNotificationService.ACTION_START_NOTIFICATIONS
            serviceIntent.putExtra(GameNotificationService.EXTRA_PLAYER_LOGIN, playerLogin)
            startService(serviceIntent)
        }
    }
}
