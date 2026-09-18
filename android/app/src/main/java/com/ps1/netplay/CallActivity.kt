package com.ps1.netplay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ps1.netplay.ui.compose.CallHistoryManager
import com.ps1.netplay.ui.compose.CallStatus
import com.ps1.netplay.ui.compose.RealCallRecord
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1001

    private var callID: String = ""
    private var isVideo: Boolean = true
    private var targetUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        callID = intent.getStringExtra("callID") ?: "call_${System.currentTimeMillis()}"
        isVideo = intent.getBooleanExtra("isVideo", true)
        targetUserName = intent.getStringExtra("targetUserName") ?: "Unknown"

        // Log call to local history
        try {
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            CallHistoryManager.addRecord(
                this,
                RealCallRecord(
                    id = callID,
                    name = targetUserName,
                    avatar = "https://ui-avatars.com/api/?name=$targetUserName&background=random",
                    time = timeStr,
                    isVideo = isVideo,
                    status = CallStatus.OUTGOING
                )
            )
        } catch (e: Exception) {
            Log.w("CallActivity", "Failed to log call: ${e.message}")
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (isVideo) {
            permissions.add(Manifest.permission.CAMERA)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            startZegoCall()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startZegoCall()
            } else {
                Toast.makeText(this, "يرجى منح صلاحيات الصوت والكاميرا للمتابعة", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startZegoCall() {
        if (isFinishing || isDestroyed) return

        val currentUser = UserManager.getCurrentUser(this)
        val userID = currentUser?.username?.ifEmpty { "user_${System.currentTimeMillis()}" } ?: "user_${System.currentTimeMillis()}"
        val userName = currentUser?.fullName?.ifEmpty { currentUser.username } ?: "User"

        val config = if (isVideo) {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
        } else {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall()
        }

        com.ps1.netplay.network.CloudflareClient.getZegoConfig(this) { success, fetchedAppID, fetchedAppSign ->
            if (isFinishing || isDestroyed) return@getZegoConfig

            // If remote config succeeded, use it; otherwise use reliable direct fallback
            val effectiveAppID = if (success && fetchedAppID > 0L) fetchedAppID else 1477087305L
            val effectiveAppSign = if (success && fetchedAppSign.isNotBlank()) fetchedAppSign else "29c005b621138958b88eea14c91bd62b2189095171ce962ffe3680974493b41d"

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                try {
                    val fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                        effectiveAppID,
                        effectiveAppSign,
                        userID,
                        userName,
                        callID,
                        config
                    )
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commitAllowingStateLoss()
                } catch (e: Exception) {
                    Log.e("CallActivity", "Failed to start Zego call: ${e.message}", e)
                    Toast.makeText(this, "عذراً، تعذر بدء المكالمة: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
}
