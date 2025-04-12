package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import io.ktor.client.HttpClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class Home : AppCompatActivity() {
    private lateinit var image1 : ImageView
    private lateinit var image2 : ImageView
    private lateinit var image3 : ImageView
    private lateinit var image4 : ImageView
    private lateinit var start : Button
    private lateinit var invite : Button
    private lateinit var client: HttpClient
    private var login:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        login = intent.getStringExtra("LOGIN_EXTRA")
        Toast.makeText(this@Home, "$login",Toast.LENGTH_LONG).show()
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


        start.setOnClickListener{
            val intent = Intent(this@Home, MainActivity::class.java)
            startActivity(intent)
            finish()

        }
        invite.setOnClickListener{
            showInviteDialog()
        }

       client = HttpClientProvider.create()

//        connectToWebSoket()
    }
    private  fun showInviteDialog(){
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_invite,null)
        val playerLoginEditText = dialogLayout.findViewById<TextInputEditText>(R.id.playerLoginEditText)

        builder.setTitle("Invite Player")
            .setPositiveButton("Send"){dialog, which ->
                val playerLogin = playerLoginEditText.text.toString()
                sendInvite(playerLogin)
            }
            .setNegativeButton("Cancel"){dialog, which ->
                dialog.cancel()
            }
            .setView(dialogLayout)
            .show()

    }
    private  fun sendInvite(playerLogin:String){
        val inviteRequest = Json.encodeToString(InviteRequest(login, playerLogin))
        // Отправляем сообщение через WebSocket.
        Toast.makeText(this, inviteRequest,Toast.LENGTH_LONG).show()

    }







    override fun onPause() {
        Log.d("888", "onPauseH1")

        super.onPause()
    }

    override fun onDestroy() {
        Log.d("888", "onDestroyH1")

        super.onDestroy()
    }

}
