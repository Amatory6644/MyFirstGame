package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.service.GameNotificationService
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class GroupsActivity : AppCompatActivity() {
    
    private val groupViewModel: GroupViewModel by inject()
    
    private lateinit var btnCreateGroup: Button
    private lateinit var btnInvites: Button
    private lateinit var rvGroups: RecyclerView
    private lateinit var tvStatus: TextView
    private lateinit var groupsAdapter: GroupsAdapter
    
    private var playerLogin: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)
        
        playerLogin = intent.getStringExtra("LOGIN_EXTRA") ?: ""
        if (playerLogin.isEmpty()) {
            Toast.makeText(this, "Ошибка: логин не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        // Запускаем сервис уведомлений
        startNotificationService()
        
        initializeViews()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Загружаем группы
        groupViewModel.loadGroups()
        groupViewModel.loadInvites(playerLogin)
    }
    
    private fun initializeViews() {
        btnCreateGroup = findViewById(R.id.btn_create_group)
        btnInvites = findViewById(R.id.btn_invites)
        rvGroups = findViewById(R.id.rv_groups)
        tvStatus = findViewById(R.id.tv_status)
    }
    
    private fun setupRecyclerView() {
        groupsAdapter = GroupsAdapter { group ->
            // Присоединяемся к группе
            groupViewModel.joinGroup(group.groupId)
            Toast.makeText(this, "Присоединились к группе: ${group.groupId}", Toast.LENGTH_SHORT).show()
        }
        
        rvGroups.layoutManager = LinearLayoutManager(this)
        rvGroups.adapter = groupsAdapter
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            groupViewModel.groups.collect { groups ->
                groupsAdapter.updateGroups(groups)
                tvStatus.text = "Найдено групп: ${groups.size}"
            }
        }
        
        lifecycleScope.launch {
            groupViewModel.state.collect { state ->
                when (state) {
                    is GroupViewModel.GroupState.Loading -> {
                        tvStatus.text = "Загрузка..."
                    }
                    is GroupViewModel.GroupState.Success -> {
                        Toast.makeText(this@GroupsActivity, state.message, Toast.LENGTH_SHORT).show()
                        groupViewModel.resetState()
                    }
                    is GroupViewModel.GroupState.Error -> {
                        Toast.makeText(this@GroupsActivity, state.message, Toast.LENGTH_LONG).show()
                        groupViewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
    
        private fun setupClickListeners() {
        btnCreateGroup.setOnClickListener {
            val intent = Intent(this, CreateGroupActivity::class.java)
            intent.putExtra("LOGIN_EXTRA", playerLogin)
            startActivity(intent)
        }

        btnInvites.setOnClickListener {
            val intent = Intent(this, InvitesActivity::class.java)
            intent.putExtra("LOGIN_EXTRA", playerLogin)
            intent.putExtra("GROUP_ID", "") // Пустая строка для общих приглашений
            startActivity(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на экран
        groupViewModel.loadGroups()
        groupViewModel.loadInvites(playerLogin)
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
