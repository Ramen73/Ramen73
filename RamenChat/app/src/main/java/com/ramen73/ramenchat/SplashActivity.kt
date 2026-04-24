package com.ramen73.ramenchat

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.NotificationHelper

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        NotificationHelper.createChannel(this)

        Handler(Looper.getMainLooper()).postDelayed({
            if (FirebaseUtils.auth.currentUser != null) {
                startActivity(Intent(this, ChatListActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 1800)
    }
}
