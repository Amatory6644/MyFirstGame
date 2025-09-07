package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import org.koin.android.ext.android.inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {
    var publicLogin = ""
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var radio1: RadioButton
    private lateinit var radio2: RadioButton
    private lateinit var radio3: RadioButton
    private val viewModel: LoginViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        usernameEditText = findViewById(R.id.loginEditText)
        passwordEditText = findViewById(R.id.editPassword)
        loginButton = findViewById(R.id.Login)
        registerButton = findViewById(R.id.registration)
        radio1 = findViewById(R.id.radio1)
        radio2 = findViewById(R.id.radio2)
        radio3 = findViewById(R.id.radio3)


        var login = ""
        var password = ""

        loginButton.setOnClickListener {
            login = usernameEditText.text.toString()
            password = passwordEditText.text.toString()
            viewModel.checkLogin(login, password)
        }
        
        registerButton.setOnClickListener {
            login = usernameEditText.text.toString()
            password = passwordEditText.text.toString()
            viewModel.registerUser(login, password)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state: State ->
                        when (state) {
                            //состояние сохраняется после поворота экрана
                            State.Loading -> {
                                launch {
                                    radio1.isChecked = true
                                    delay(1000)
                                    radio2.isChecked = true
                                    delay(1000)
                                    radio3.isChecked = true
                                }
                            }

                            State.Success -> {
                                radio1.isChecked = false
                                radio2.isChecked = false
                                radio3.isChecked = false
                                
                                // Успешный вход - переходим к игре
                                val login = usernameEditText.text.toString()
                                publicLogin = login
                                
                                // Сохраняем логин в SharedPreferences для использования в BroadcastReceiver
                                val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
                                prefs.edit().putString("player_login", login).apply()
                                
                                val intent = Intent(this@LoginActivity, Home::class.java)
                                intent.putExtra("LOGIN_EXTRA", login)
                                startActivity(intent)
                                finish()
                            }

                            is State.Error -> {
                                Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                            }

                            State.Empty -> {}
                        }
                    }
                }
                
                // Наблюдение за результатом аутентификации
                launch {
                    viewModel.authResult.collect { authResponse ->
                        authResponse?.let { response ->
                            when (response.status) {
                                "LOGIN_SUCCESS" -> {
                                    // Дополнительная логика при успешном входе
                                }
                                "LOGIN_FAILED" -> {
                                    // Дополнительная логика при неудачном входе
                                }
                                "REGISTER_SUCCESS" -> {
                                    // Очищаем поля после успешной регистрации
                                    usernameEditText.text.clear()
                                    passwordEditText.text.clear()
                                }
                                "REGISTER_FAILED" -> {
                                    // Дополнительная логика при неудачной регистрации
                                }
                            }
                        }
                    }
                }
            }
        }


    }

    override fun onPause() {
        Log.d("888", "onPauseL1")

        super.onPause()
    }

    override fun onDestroy() {
        Log.d("888", "onDestroyL1")

        super.onDestroy()
    }

}