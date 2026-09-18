
package com.ps1.netplay
import  android.graphics.Color
import  android.widget.* 
import  android.os.Bundle
import  androidx.activity.compose.setContent 
import  androidx.appcompat.app.AppCompatActivity
import  androidx.core.view.WindowCompat 
import  androidx.core.view.WindowInsetsCompat
import  com.ps1.netplay.ui.compose.NetPlayTheme 
import  com.ps1.netplay.ui.compose.RootAppShell

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        com.ps1.netplay.network.HeartbeatManager.startHeartbeat(this)
        // Ensure status bar and navigation bar seamlessly blend with the light theme
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
        insetsController.show(WindowInsetsCompat.Type.statusBars())
        setContent {
            NetPlayTheme(darkTheme = false) {
                RootAppShell()
            }
        }
}

    override fun onDestroy() {

        super.onDestroy()
        com.ps1.netplay.network.HeartbeatManager.stopHeartbeat()
    
}
}
