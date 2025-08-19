package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.example.myapplication.repository.GameRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class Home : AppCompatActivity() {
    
    // Получаем PlayerViewModel через Koin для работы с приглашениями
    private val playerViewModel: PlayerViewModel by inject()
    private val gameRepository: GameRepository by inject()
    
    private lateinit var image1: ImageView
    private lateinit var image2: ImageView
    private lateinit var image3: ImageView
    private lateinit var image4: ImageView
    private lateinit var start: Button
    private lateinit var invite: Button
    private lateinit var groups: Button

    private var login: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        login = intent.getStringExtra("LOGIN_EXTRA")
        if (login.isNullOrEmpty()) {
            Toast.makeText(this@Home, "Ошибка: логин не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        Toast.makeText(this@Home, "Добро пожаловать, $login!", Toast.LENGTH_LONG).show()
        
        // Устанавливаем логин игрока в ViewModel
        playerViewModel.setPlayerLogin(login!!)
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        image1 = findViewById(R.id.imageView1)
        image2 = findViewById(R.id.imageView2)
        image3 = findViewById(R.id.imageView3)
        image4 = findViewById(R.id.imageView4)

        image1.setImageResource(R.drawable.ask)
        image2.setImageResource(R.drawable.ask)
        image3.setImageResource(R.drawable.ask)
        image4.setImageResource(R.drawable.ask)

        start = findViewById(R.id.play)
        invite = findViewById(R.id.invite)
        groups = findViewById(R.id.groups)
        
        // Загружаем информацию о группе игрока
        loadPlayerGroupInfo()
    }
    
    private fun setupClickListeners() {
        start.setOnClickListener {
            val intent = Intent(this@Home, MainActivity::class.java)
            intent.putExtra("LOGIN_EXTRA", login)
            startActivity(intent)
            finish()
        }
        
        invite.setOnClickListener {
            showInviteDialog()
        }

        groups.setOnClickListener {
            val intent = Intent(this@Home, GroupsActivity::class.java)
            intent.putExtra("LOGIN_EXTRA", login)
            startActivity(intent)
        }
    }

    private fun showInviteDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_invite, null)
        val playerLoginEditText = dialogLayout.findViewById<TextInputEditText>(R.id.playerLoginEditText)

        builder.setTitle("Пригласить игрока")
            .setPositiveButton("Отправить") { dialog, which ->
                val playerLogin = playerLoginEditText.text.toString()
                if (playerLogin.isNotEmpty()) {
                    sendInvite(playerLogin)
                } else {
                    Toast.makeText(this, "Введите логин игрока", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена") { dialog, which ->
                dialog.cancel()
            }
            .setView(dialogLayout)
            .show()
    }

    private fun sendInvite(playerLogin: String) {
        lifecycleScope.launch {
            try {
                // Проверяем, что login не null
                val currentLogin = login ?: return@launch
                
                // Создаем приглашение в группу (временная группа)
                val inviteRequest = GroupInviteRequest(
                    sender = currentLogin,
                    receivers = listOf(playerLogin),
                    groupId = "temp_group_${System.currentTimeMillis()}", // Временная группа
                    message = "Приглашение в игру"
                )
                
                // Отправляем приглашение через Repository
                val result = gameRepository.sendGroupInvite(inviteRequest)
                
                if (result.status == "SUCCESS") {
                    Toast.makeText(
                        this@Home, 
                        "Приглашение отправлено игроку: $playerLogin", 
                        Toast.LENGTH_LONG
                    ).show()
                    
                    Log.d("Home", "Приглашение отправлено: $inviteRequest")
                } else {
                    Toast.makeText(
                        this@Home, 
                        "Ошибка отправки приглашения: ${result.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
                
            } catch (e: Exception) {
                Log.e("Home", "Ошибка при отправке приглашения: $e")
                Toast.makeText(
                    this@Home, 
                    "Ошибка при отправке приглашения: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
                    }
    }
    
//    override fun onResume() {
//        super.onResume()
//        // Обновляем отображение игроков в группе при возвращении
//        loadPlayerGroupInfo()
//    }
}
    
    private fun loadPlayerGroupInfo() {
        lifecycleScope.launch {
            try {
                val currentLogin = login ?: return@launch
                
                // Получаем все группы
                val groups = gameRepository.getAllGroups()
                
                // Ищем группу, в которой состоит игрок
                val playerGroup = groups.find { group ->
                    group.players.contains(currentLogin)
                }
                
                if (playerGroup != null) {
                    // Обновляем отображение игроков в группе
                    updateGroupPlayersDisplay(playerGroup)
                }
                
            } catch (e: Exception) {
                Log.e("Home", "Ошибка загрузки информации о группе: $e")
            }
        }
    }
    
    private fun updateGroupPlayersDisplay(group: GroupInfo) {
        val images = listOf(image1, image2, image3, image4)
        
        // Сначала все изображения делаем серыми (не в группе)
        images.forEach { imageView ->
            imageView.setImageResource(R.drawable.ask)
        }
        
        // Затем обновляем изображения игроков в группе
        group.players.take(4).forEachIndexed { index, playerLogin ->
            if (index < images.size) {
                if (playerLogin == login) {
                    // Текущий игрок - черный цвет
                    images[index].setImageResource(R.drawable.player_black)
                } else {
                    // Другие игроки - зеленый цвет
                    images[index].setImageResource(R.drawable.player_green)
                }
            }
        }
    }
}
