
package com.ps1.netplay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ps1.netplay.network.CallSignalingManager
import com.ps1.netplay.network.HeartbeatManager
import com.ps1.netplay.ui.compose.NetPlayTheme
import com.ps1.netplay.ui.compose.RootAppShell

class DashboardActivity : AppCompatActivity() {

    private val requestAllPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // All permissions processed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize notification channels immediately
        CallSignalingManager.createCallNotificationChannel(this)

        // 2. Request all required app permissions & notifications on install/launch
        checkAndRequestAllPermissions()

        // 3. Start real-time presence heartbeat
        HeartbeatManager.startHeartbeat(this)

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

    private fun checkAndRequestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Only request notifications on launch (Android 13+) so user receives incoming calls & messages
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Camera, Microphone, and Location are strictly requested on-demand when user initiates calls or media
        if (permissionsToRequest.isNotEmpty()) {
            requestAllPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        HeartbeatManager.stopHeartbeat(this)
    }
}
