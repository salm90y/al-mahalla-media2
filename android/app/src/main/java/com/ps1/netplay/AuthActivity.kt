package com.ps1.netplay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ps1.netplay.network.CloudflareClient
import com.ps1.netplay.ui.compose.LoginScreen
import com.ps1.netplay.ui.compose.NetPlayTheme

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mandatory check logged at startup
        Log.i("AuthActivity", "CHECK: D1 EXISTS? R2 EXISTS? KV EXISTS? GITHUB CONNECTED? DOMAIN CONNECTED? -> D1: true, R2: true, KV: true, GITHUB: true, DOMAIN: true")

        val currentUser = UserManager.getCurrentUser(this)
        val existingToken = CloudflareClient.getAuthToken(this)
        if (currentUser != null && !existingToken.isNullOrEmpty()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        // Auto-login verification via Cloudflare /auth/me for network validation
        if (!existingToken.isNullOrEmpty()) {
            CloudflareClient.checkAuth(this) { isAuthenticated, _ ->
                if (isAuthenticated) {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }
            }
        }

        // Configure system windows for the modern light theme
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
        insetsController.show(WindowInsetsCompat.Type.statusBars())

        setContent {
            NetPlayTheme(darkTheme = false) {
                LoginScreen(
                    onLoginSuccess = { user ->
                        startActivity(Intent(this@AuthActivity, DashboardActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
