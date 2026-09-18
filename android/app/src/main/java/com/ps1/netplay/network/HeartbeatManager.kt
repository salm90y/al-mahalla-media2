package com.ps1.netplay.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HeartbeatManager {

    private val handler = Handler(Looper.getMainLooper())

    private var mContext: Context? = null

    private var isRunning = false

    private val heartbeatRunnable = object : Runnable {

        override fun run() {

            val context = mContext

            if (context != null) {

                CoroutineScope(Dispatchers.IO).launch {

                    try {

                        ApiService.post(context, "/api/users/heartbeat", "{}")

                    } catch (e: Exception) {

                        e.printStackTrace()

                    }

                }

            }

            handler.postDelayed(this, 30000)

        }

    }

    fun startHeartbeat(context: Context) {

        mContext = context.applicationContext

        if (isRunning) return

        isRunning = true

        handler.post(heartbeatRunnable)

    }

    fun stopHeartbeat() {

        isRunning = false

        handler.removeCallbacks(heartbeatRunnable)

    }
}
