package com.ps1.netplay.network

import com.ps1.netplay.core.NativeCoreBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates game frames, synchronized inputs, deterministic lockstep,
 * and automatic desync prevention between Host (P1) and Guest (P2).
 */
class MatchCoordinator(
    val isHost: Boolean,
    private val transport: PeerTransport?
) {
    companion object {
        const val INPUT_DELAY_FRAMES = 2 // Standard 2-frame input buffering (~33ms) for zero-stutter lockstep
        const val AUTO_SYNC_INTERVAL_FRAMES = 180L // Periodic State Sync every 3 seconds (180 frames @ 60fps)
    }

    private var currentFrame: Long = 0
    private val localInputBuffer = ConcurrentHashMap<Long, Int>()
    private val remoteInputBuffer = ConcurrentHashMap<Long, Int>()
    private var lastConfirmedRemoteInput: Int = 0

    // Netplay Synchronization & Lockstep States
    var isWaitingForPeer: Boolean = false
    var isPeerConnected: Boolean = false
    var isRemotePlayGuest: Boolean = false
    var isGameStarted: Boolean = false // حالة بدء المباراة
    private var lastReceivedHostFrame: Long = 0

    // Callback for Host to broadcast periodic authoritative state snapshot
    var onAutoSyncRequested: ((frame: Long) -> Unit)? = null

    init {
        transport?.setOnInputReceivedListener { frameIndex, inputMask ->
            remoteInputBuffer[frameIndex] = inputMask
            lastConfirmedRemoteInput = inputMask
            if (!isHost && frameIndex > lastReceivedHostFrame) {
                lastReceivedHostFrame = frameIndex
            }
        }
    }

    /**
     * Executes one frame with strict deterministic lockstep.
     * Guarantees identical joystick movements and zero screen drift between Host and Guest.
     */
    fun tickFrame(localInputMask: Int) {
        // حماية بدء المباراة: اللعبة تبقى ثابتة لدى المضيف والضيف حتى يتم اختيار اللاعبين وبدء اللعب
        if (!isGameStarted) return

        // 1. Queue local input with input delay buffer and send immediately to peer
        val targetInputFrame = currentFrame + INPUT_DELAY_FRAMES
        localInputBuffer[targetInputFrame] = localInputMask
        transport?.sendFrameInput(targetInputFrame, localInputMask)

        // 2. If single player or waiting for peer, advance frame deterministically
        if (!isPeerConnected) {
            val p1 = if (isHost) localInputMask else 0
            val p2 = if (isHost) 0 else localInputMask
            if (NativeCoreBridge.isAvailable()) {
                NativeCoreBridge.safeRunFrame(p1, p2)
            }
            currentFrame++
            return
        }

        // 3. Multi-Player Deterministic Lockstep:
        // Wait until remote input for currentFrame is received so both players simulate the EXACT same state!
        val hasRemoteInput = remoteInputBuffer.containsKey(currentFrame)
        if (!hasRemoteInput) {
            // Strict lockstep: Do NOT advance the game state at all if the remote input for this exact frame hasn't arrived.
            // This prevents ANY desync or drift. The game will visually pause (stutter) until the network catches up.
            return
        }

        // 4. Retrieve exact synchronized inputs for currentFrame
        val localInput = localInputBuffer.remove(currentFrame) ?: localInputMask
        val remoteInput = remoteInputBuffer.remove(currentFrame) ?: lastConfirmedRemoteInput

        // Host is ALWAYS Player 1; Guest is ALWAYS Player 2
        val (p1Mask, p2Mask) = if (isHost) {
            Pair(localInput, remoteInput)
        } else {
            Pair(remoteInput, localInput)
        }

        // 5. Run frame in native PS1 emulator core
        if (NativeCoreBridge.isAvailable()) {
            NativeCoreBridge.safeRunFrame(p1Mask, p2Mask)
        }
        currentFrame++

        // 6. Host periodic heartbeat auto-sync (keeps guest 100% bit-exact across rounds)
        if (isHost && currentFrame % AUTO_SYNC_INTERVAL_FRAMES == 0L) {
            onAutoSyncRequested?.invoke(currentFrame)
        }

        // Cleanup old buffered inputs (older than 60 frames)
        if (currentFrame > 60) {
            val threshold = currentFrame - 60
            localInputBuffer.keys.removeIf { it < threshold }
            remoteInputBuffer.keys.removeIf { it < threshold }
        }
    }

    /**
     * Forces immediate frame synchronization to the Host's exact frame
     */
    fun syncToHost(targetFrame: Long) {
        currentFrame = targetFrame
        lastReceivedHostFrame = targetFrame
        localInputBuffer.clear()
        remoteInputBuffer.clear()
        isWaitingForPeer = false
        isPeerConnected = true
    }

    fun getCurrentFrameNumber(): Long = currentFrame

    fun reset() {
        currentFrame = 0
        lastReceivedHostFrame = 0
        localInputBuffer.clear()
        remoteInputBuffer.clear()
        lastConfirmedRemoteInput = 0
        isWaitingForPeer = false
        isPeerConnected = false
        isGameStarted = false
    }
}
