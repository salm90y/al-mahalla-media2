package com.ps1.netplay

import android.graphics.Color
import android.widget.*
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ps1.netplay.ui.compose.NetPlayTheme
import com.ps1.netplay.ui.compose.WhatsAppSettingsScreen

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure status bar is visible, transparent, and icons are light
WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
        insetsController.show(WindowInsetsCompat.Type.statusBars())
        setContent {
            NetPlayTheme(darkTheme = false) {
                WhatsAppSettingsScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }}