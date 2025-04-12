package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.InternalAPI
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


class LoginActivite : AppCompatActivity() {
    private val _publicLogin = MutableSharedFlow<String>(replay = 1)
    val publicLogin = _publicLogin.asSharedFlow()

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var client: HttpClient
    private lateinit var login: String
    private lateinit var password: String

    private val loginRegex = "^[a-zA-Z]+$".toRegex()
    private val passwordRegex = "^[0-9]+$".toRegex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        usernameEditText = findViewById(R.id.loginEditText)
        passwordEditText = findViewById(R.id.editPassword)
        loginButton = findViewById(R.id.Login)
        registerButton = findViewById(R.id.registration)

        client = HttpClient {
            install(WebSockets)
        }



        loginButton.setOnClickListener {
            login = usernameEditText.text.toString()
            password = passwordEditText.text.toString()

            lifecycleScope.launch {
                _publicLogin.emit(login)
                authenticateUser(action = "login", login, password)
            }

        }
        registerButton.setOnClickListener {
            login = usernameEditText.text.toString()
            password = passwordEditText.text.toString()


            lifecycleScope.launch {
                authenticateUser(action = "register", login, password)
            }
        }
    }

    @OptIn(InternalAPI::class)
    private suspend fun authenticateUser(action: String, login: String, password: String) {
        try {
            val response: HttpResponse = client.post("http://192.168.0.105:8080/auth") {
                Log.d("HttpResponse", "Подключение к серверу аутентификации")
                contentType(ContentType.Application.FormUrlEncoded)
                body = "action=$action&username=$login&password=$password"
            }
            val responseBody = response.bodyAsText()
            Log.d("Auth", "Получен ответ: $responseBody")
            val authResponse = Json.decodeFromString<AuthResponse>(responseBody)
            // Успешность регистрации
            if (authResponse.status == "REGISTER_SUCCESS") {
                Toast.makeText(
                    this@LoginActivite,
                    authResponse.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Проверка успешности входа
            if (authResponse.status == "LOGIN_SUCCESS") {
                Toast.makeText(
                    this@LoginActivite,
                    authResponse.message,
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this@LoginActivite, Home::class.java)
                intent.putExtra("LOGIN_EXTRA", login)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this@LoginActivite,
                    authResponse.message,
                    Toast.LENGTH_SHORT
                ).show()
                usernameEditText.text.clear()
                passwordEditText.text.clear()
            }
        } catch (e: Exception) {
            Log.e("Auth", "Ошибка при аутентификации", e)
            Toast.makeText(
                this@LoginActivite,
                "Ошибка при подключении к серверу",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onPause() {
        Log.d("888", "onPauseL1")

        super.onPause()
    }

    override fun onDestroy() {
        Log.d("888", "onDestroyL1")
        try {
            client.close()
        }catch (e:Exception){
            Log.w("Lifecycle", "Ошиббка при закрытии HttpClient", e)
        }
        super.onDestroy()
    }

}