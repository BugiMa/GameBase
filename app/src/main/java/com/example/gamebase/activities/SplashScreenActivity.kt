package com.example.gamebase.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.gamebase.R
import com.google.firebase.auth.FirebaseAuth

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        Handler(Looper.getMainLooper()).postDelayed({
            if (user != null) {
                val intentGameBase = Intent(this, GameBaseActivity::class.java)
                startActivity(intentGameBase)
                finish()
            } else {
                val intentLogin = Intent(this, LoginActivity::class.java)
                startActivity(intentLogin)
                finish()
            }
        }, 2000)
    }


}