package com.example.oopsreportapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.oopsreportapp.R
import com.example.oopsreportapp.ui.dashboard.DashboardActivity
import com.example.oopsreportapp.util.SessionManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay 2 detik sebelum pindah layar
        Handler(Looper.getMainLooper()).postDelayed({
            val sessionManager = SessionManager(this)

            // Cek apakah user sudah login sebelumnya
            val intent = if (sessionManager.isLoggedIn()) {
                Intent(this, DashboardActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            finish() // Biar tombol back nggak balik ke splash screen
        }, 2000) // 2000 ms = 2 detik
    }
}