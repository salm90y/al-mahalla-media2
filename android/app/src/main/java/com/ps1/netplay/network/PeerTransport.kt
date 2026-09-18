package com.ps1.netplay.network

 
import  android.widget.*

/** * Low-latency P2P Transport Interface (WebRTC DataChannel or Direct UDP Socket) */interface PeerTransport {
    fun sendFrameInput(frameIndex: Long, inputMask: Int)
fun setOnInputReceivedListener(listener: (frameIndex: Long, inputMask: Int) -> Unit)
fun setOnConnectionStateListener(listener: (connected: Boolean, pingMs: Long) -> Unit)
fun disconnect()
    // Host & Guest Moment Synchronization Methods (Default empty to preserve compatibility)
    fun sendStateSnapshot(hostFrame: Long, stateData: ByteArray?) {}
fun setOnStateSyncReceivedListener(listener: (hostFrame: Long, stateBytes: ByteArray?) -> Unit) {}
fun requestStateSync() {}
fun setOnStateSyncRequestedListener(listener: () -> Unit) {}
fun sendGameStartSync(startFrame: Long) {}
fun setOnGameStartSyncListener(listener: (startFrame: Long) -> Unit) {}}