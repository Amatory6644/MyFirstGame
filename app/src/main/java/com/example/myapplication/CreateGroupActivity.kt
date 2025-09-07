package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.logging.GameLogger
import com.example.myapplication.repository.GameRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class CreateGroupActivity : AppCompatActivity() {
    
    private val gameRepository: GameRepository by inject()
    private val gameLogger: GameLogger by inject()
    
    private lateinit var etGroupName: EditText
    private lateinit var etMaxPlayers: EditText
    private lateinit var btnCreate: Button
    private lateinit var btnCancel: Button
    
    private var playerLogin: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)
        
        playerLogin = intent.getStringExtra("LOGIN_EXTRA") ?: ""
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        etGroupName = findViewById(R.id.et_group_name)
        etMaxPlayers = findViewById(R.id.et_max_players)
        btnCreate = findViewById(R.id.btn_create)
        btnCancel = findViewById(R.id.btn_cancel)
        
        // Устанавливаем значение по умолчанию
        etMaxPlayers.setText("5")
    }
    
    private fun setupClickListeners() {
        btnCreate.setOnClickListener {
            createGroup()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun createGroup() {
        val groupName = etGroupName.text.toString().trim()
        val maxPlayersStr = etMaxPlayers.text.toString().trim()
        
        if (groupName.isEmpty()) {
            Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
            return
        }
        
        val maxPlayers = maxPlayersStr.toIntOrNull() ?: 5
        if (maxPlayers < 2 || maxPlayers > 10) {
            Toast.makeText(this, "Количество игроков должно быть от 2 до 10", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val groupInfo = GroupInfo(
                    groupId = playerLogin, // Используем логин создателя как ID группы
                    players = mutableListOf(playerLogin),
                    maxPlayers = maxPlayers,
                    status = "waiting"
                )
                
                // Создаем группу через репозиторий
                val result = gameRepository.createGroup(groupInfo)

                if (result.status == "SUCCESS") {
                    Toast.makeText(this@CreateGroupActivity, "Группа создана успешно!", Toast.LENGTH_SHORT).show()
                    gameLogger.logPlayerAction(playerLogin, "GROUP_CREATED", "group_id=$playerLogin, group_name=$groupName")
                    
                    // Возвращаемся к списку групп
                    val intent = Intent(this@CreateGroupActivity, GroupsActivity::class.java)
                    intent.putExtra("LOGIN_EXTRA", playerLogin)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@CreateGroupActivity, "Ошибка создания группы: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateGroupActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                gameLogger.logPlayerAction(playerLogin, "GROUP_CREATE_ERROR", e.message)
            }
        }
    }
}
