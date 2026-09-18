
package com.ps1.netplay
import  android.widget.*
import  android.Manifest 
import  android.content.pm.PackageManager
import  android.graphics.Color 
import  android.net.Uri
import  android.os.Build 
import  android.os.Bundle
import  android.provider.OpenableColumns 
import  android.view.KeyEvent
import  android.view.MotionEvent 
import  android.view.TextureView
import  android.view.View 
import  android.view.WindowInsets
import  android.view.WindowInsetsController 
import  android.widget.Button
import  android.widget.EditText 
import  android.widget.FrameLayout
import  android.widget.ImageButton 
import  android.widget.ImageView
import  android.widget.ScrollView 
import  android.widget.TextView
import  android.widget.Toast 
import  androidx.activity.result.ActivityResultLauncher
import  androidx.activity.result.contract.ActivityResultContracts 
import  androidx.appcompat.app.AppCompatActivity
import  androidx.core.content.ContextCompat 
import  com.ps1.netplay.core.CoreManager
import  com.ps1.netplay.core.NativeCoreBridge 
import  com.ps1.netplay.core.TemporaryStateStore
import  com.ps1.netplay.input.GamepadManager 
import  com.ps1.netplay.network.DirectSocketNetplayEngine
import  com.ps1.netplay.network.GlobalInternetNetplayEngine 
import  com.ps1.netplay.network.LiveKitNetplayManager
import  com.ps1.netplay.network.MatchCoordinator 
import  com.ps1.netplay.network.NetplaySession
import  com.ps1.netplay.ui.GameSurfaceView 
import  com.ps1.netplay.ui.IsolatedSettingsBottomSheet
import  com.ps1.netplay.ui.NativeFeatureDialogs 
import  com.ps1.netplay.ui.RoomCameraHelper
import  com.ps1.netplay.ui.VerticalVoiceMeterView 
import  com.ps1.netplay.ui.VirtualTouchOverlayView
import  kotlinx.coroutines.CoroutineScope 
import  kotlinx.coroutines.Dispatchers
import  kotlinx.coroutines.Job 
import  kotlinx.coroutines.cancel
import  kotlinx.coroutines.delay 
import  kotlinx.coroutines.launch
import  kotlinx.coroutines.yield

class MainActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    // 1. Home Hub Views
// 2. Active Room Views
    private lateinit var layoutRoomActive: View
    private lateinit var gameContainer: FrameLayout
    private lateinit var emptyState: TextView
    private var layoutGuestStreamBadge: View? = null
    private var txtGuestStreamStatus: TextView? = null
    // Top Bar Floating Glass Chips (Icons Only, No Text)
    private lateinit var chipLightning: FrameLayout
    private lateinit var chipCrown: FrameLayout
    private lateinit var chipLive: FrameLayout
    private var currentPingMs: Long = 18L
    private var currentSyncStatusString: String = "🟢 متزامن تماماً (شاشة المضيف هي المرجع)"
    // Bottom Floating Dock Buttons (Icons Only)
    private lateinit var btnDockChat: ImageButton
    private lateinit var btnDockCamera: ImageButton
    private lateinit var btnDockVoice: ImageButton
    private lateinit var btnDockGamepad: ImageButton
    private lateinit var btnDockMembers: ImageButton
    private lateinit var btnDockSettings: ImageButton
    private lateinit var voiceMeterDock: VerticalVoiceMeterView
    // Camera Helper
    private var roomCameraHelper: RoomCameraHelper? = null
    private var cameraPermissionLauncher: ActivityResultLauncher<String>? = null
    // Core & Input Managers
    private var gameSurfaceView: GameSurfaceView? = null
    private var virtualTouchOverlay: VirtualTouchOverlayView? = null
    private var gamepadManager: GamepadManager? = null
    private var coreManager: CoreManager? = null
    private var netplaySession: NetplaySession? = null
    private var matchCoordinator: MatchCoordinator? = null
    private var temporaryStateStore: TemporaryStateStore? = null
    private var ps1AudioPlayer: com.ps1.netplay.core.PS1AudioPlayer? = null
    // Pickers & State
    private var romPickerLauncher: ActivityResultLauncher<String>? = null
    private var biosPickerLauncher: ActivityResultLauncher<String>? = null
    private var activityScope: CoroutineScope? = null
    private var currentRomName = "Combat 3 (المدمجة)"
    private var currentBiosName = "HLE / BIOS تلقائي"
    private var isGameRunning = false
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        runCatching {
 hideSystemUI() 
}

        try {

            romPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
 uri ->
                if (uri != null) handleRomSelected(uri)
            
}

            biosPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
 uri ->
                if (uri != null) handleBiosSelected(uri)
            
}

            cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
 granted ->
                if (granted) {

                    showCameraArenaDialog()
                
}
 else {

                    Toast.makeText(this, "يرجى منح إذن الكاميرا لتشغيل بث الفيديو", Toast.LENGTH_SHORT).show()
                
}
}

            roomCameraHelper = RoomCameraHelper(this)
            setContentView(R.layout.activity_main)
            root = findViewById(R.id.main_root)
            // Bind Active Room Views (New Minimal Esports Design)
layoutRoomActive = findViewById(R.id.layout_room_active)
            gameContainer = findViewById(R.id.game_view_container)
            emptyState = findViewById(R.id.game_empty_state)
            layoutGuestStreamBadge = findViewById(R.id.layout_guest_stream_badge)
            txtGuestStreamStatus = findViewById(R.id.txt_guest_stream_status)
            // Top Bar Floating Glass Chips (Icons Only, No Text)
chipLightning = findViewById(R.id.chip_lightning)
            chipCrown = findViewById(R.id.chip_crown)
            chipLive = findViewById(R.id.chip_live)
            // Bind Floating Dock Views (Pill Bar)
btnDockSettings = findViewById(R.id.btn_dock_settings)
            btnDockMembers = findViewById(R.id.btn_dock_members)
            btnDockGamepad = findViewById(R.id.btn_dock_gamepad)
            btnDockVoice = findViewById(R.id.btn_dock_voice)
            voiceMeterDock = findViewById(R.id.voice_meter_dock)
            btnDockCamera = findViewById(R.id.btn_dock_camera)
            btnDockChat = findViewById(R.id.btn_dock_chat)
            // Managers
gamepadManager = GamepadManager()
            coreManager = CoreManager(this)
            netplaySession = NetplaySession()
            ps1AudioPlayer = com.ps1.netplay.core.PS1AudioPlayer()
            temporaryStateStore = TemporaryStateStore(this).also {
 it.clear() 
}

            activityScope = CoroutineScope(Dispatchers.Default + Job())
            // Initialize Virtual Touch Overlay (disabled by default on Home screen)
virtualTouchOverlay = VirtualTouchOverlayView(this).apply {

                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                isControlsVisible = false
                onInputMaskChanged = {
 mask ->
                    gamepadManager?.updateTouchMask(mask)
                
}
}

            root.addView(virtualTouchOverlay)
            handleIntentAction()
            // Setup In-Room Click Handlers
setupRoomListeners()
            // Check if deep linked with ?room=CODE
    val roomParam = intent?.data?.getQueryParameter("room")
            if (!roomParam.isNullOrEmpty()) {

                netplaySession?.joinRoom(roomParam)
                enterRoomScreen(roomParam, isHost = false)
            
}
 else {

                handleIntentAction()
            
}
}
 catch (t: Throwable) {

            android.util.Log.e("MainActivity", "Startup failure", t)
            Toast.makeText(this, "تعذر تهيئة الواجهة: ${t.javaClass.simpleName}", Toast.LENGTH_LONG).show()        
}
}

    private var globalNetplayEngine: GlobalInternetNetplayEngine? = null
    private var directSocketEngine: DirectSocketNetplayEngine? = null
    private fun handleIntentAction() {

        val action = intent?.getStringExtra("ACTION") ?: "SINGLE"
                when (action) {

            "CREATE" -> {

                val roomName = intent?.getStringExtra("ROOM_NAME") ?: ""
                val roomPass = intent?.getStringExtra("ROOM_PASS") ?: ""
                startHostGame(roomName, roomPass)
            
}

            "JOIN" -> {

                val roomCode = intent?.getStringExtra("ROOM_CODE") ?: ""
                val roomPass = intent?.getStringExtra("ROOM_PASS") ?: ""
                startJoinGame(roomCode, roomPass)
            
}

            "SINGLE" -> {

                startSinglePlayerGame()
            
}
}
}

    private fun startHostGame(roomName: String, roomPass: String) {

        val engine = GlobalInternetNetplayEngine(this, netplaySession?.myPlayerName ?: "Player 1")
        globalNetplayEngine = engine
                engine.onPeerJoined = {
 peerName: String, _: Boolean ->
            matchCoordinator?.isPeerConnected = true
            matchCoordinator?.isWaitingForPeer = false
            val state = NativeCoreBridge.safeSaveState()
val currentF = matchCoordinator?.getCurrentFrameNumber() ?: 0L
            engine.sendStateSnapshot(currentF, state)
            engine.sendGameStartSync(currentF)
            runOnUiThread {

                currentSyncStatusString = "🟢 متزامن تماماً مع $peerName (المباراة بدأت)"
                Toast.makeText(this, "🎮 انضم $peerName! تم إرسال لقطة المضيف وبدء التزامن التام", Toast.LENGTH_SHORT).show()
            
}
}

                engine.setOnStateSyncRequestedListener {

            val state = NativeCoreBridge.safeSaveState()
val currentF = matchCoordinator?.getCurrentFrameNumber() ?: 0L
            engine.sendStateSnapshot(currentF, state)
            runOnUiThread {

                Toast.makeText(this, "🔄 تم إرسال لقطة شاشة المضيف وفرضها على الضيف", Toast.LENGTH_SHORT).show()
            
}
}

                val code = netplaySession?.createGlobalRoomWithTransport(
            transportEngine = engine,
            roomName = roomName.ifBlank {
 "غرفة قتال Combat 3" 
}
,
            password = roomPass
        ) {
 success, msg ->
            runOnUiThread {

                val activeCode = netplaySession?.currentRoom?.roomCode ?: "ROOM-1"
                if (success) {

                    try {

                        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("PS1 Room Code", activeCode)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(this, "✅ $msg\nكود الغرفة: $activeCode (تم النسخ تلقائياً)", Toast.LENGTH_LONG).show()
                    
}
 catch (e: Exception) {

                        Toast.makeText(this, "✅ $msg\nكود الغرفة للمشاركة: $activeCode", Toast.LENGTH_LONG).show()
                    
}
}
 else {

                    Toast.makeText(this, "⚠️ $msg", Toast.LENGTH_SHORT).show()
                
}
}
}
 ?: "ROOM-1"
                enterRoomScreen(code, isHost = true)
        virtualTouchOverlay?.isControlsVisible = true
        virtualTouchOverlay?.invalidate()
        startGameSurface()
        startGameLoop()
        ps1AudioPlayer?.start()
    
}

    private fun startJoinGame(code: String, pass: String) {

        if (code.isEmpty()) {

            Toast.makeText(this, "يرجى كتابة رمز الغرفة المكون من 6 رموز أولاً", Toast.LENGTH_SHORT).show()
            return
        
}

                Toast.makeText(this, "🔍 جاري الاتصال بالغرفة العالمية $code والتحقق من المضيف...", Toast.LENGTH_SHORT).show()
val engine = GlobalInternetNetplayEngine(this, netplaySession?.myPlayerName ?: "Player 2")
        globalNetplayEngine = engine
                engine.onRemoteGameInfoReceived = {
 gameTitle ->
            runOnUiThread {

                emptyState.text = "🎮 متصل كـ ضيف\n\nاللعبة الحالية: $gameTitle\nالمحاكي جاهز. يرجى انتظار المضيف لبدء المباراة اللحظية!"
                txtGuestStreamStatus?.text = "المزامنة اللحظية (Lockstep): $gameTitle"
            
}
}

                engine.setOnStateSyncReceivedListener {
 hostFrame, stateBytes ->
            if (stateBytes != null && stateBytes.isNotEmpty()) {

                NativeCoreBridge.safeLoadState(stateBytes)
            
}

            matchCoordinator?.syncToHost(hostFrame)
            matchCoordinator?.isPeerConnected = true
            matchCoordinator?.isWaitingForPeer = false
            runOnUiThread {

                currentSyncStatusString = "🟢 متزامن تماماً مع شاشة المضيف (الإطار: #$hostFrame)"
                Toast.makeText(this, "✅ تم فرض مزامنة شاشة المضيف بنجاح! كلاكما في نفس اللحظة", Toast.LENGTH_SHORT).show()
            
}
}

                engine.setOnGameStartSyncListener {
 startFrame ->
            matchCoordinator?.isGameStarted = true
            netplaySession?.currentRoom?.isGameStarted = true
            matchCoordinator?.syncToHost(startFrame)
            matchCoordinator?.isPeerConnected = true
            matchCoordinator?.isWaitingForPeer = false
            runOnUiThread {

                currentSyncStatusString = "🟢 انطلقت المباراة المتزامنة مع المضيف (الإطار: #$startFrame)"
                Toast.makeText(this, "بدأت المباراة بواسطة المضيف!", Toast.LENGTH_SHORT).show()
            
}
}

                netplaySession?.joinGlobalRoomWithTransport(engine, code, pass) {
 success, msg ->
            runOnUiThread {

                if (success) {

                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    enterRoomScreen(code, isHost = false)
                    virtualTouchOverlay?.isControlsVisible = true
                    virtualTouchOverlay?.invalidate()
                    matchCoordinator?.isRemotePlayGuest = false
                    startGameLoop()
                    ps1AudioPlayer?.start()
                
}
 else {

                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                
}
}
}
}

    private fun startSinglePlayerGame() {

        val code = netplaySession?.createRoom("لعب فردي (Solo)") ?: "SOLO-1"
        enterRoomScreen(code, isHost = true)
        virtualTouchOverlay?.isControlsVisible = true
        virtualTouchOverlay?.invalidate()
        startGameSurface()
        startGameLoop()
        ps1AudioPlayer?.start()
        Toast.makeText(this, "تم بدء تشغيل Combat 3 بنجاح 🎮", Toast.LENGTH_SHORT).show()
    
}

    private fun setupRoomListeners() {

        // Click on game empty state to load ROM directly
emptyState.setOnClickListener {

            romPickerLauncher?.launch("*/*")
        
}

        // --- Top Bar Floating Glass Chips (Icons Only) ---
// 1. Lightning Chip: Ping & Frame Latency
chipLightning.setOnClickListener {
            Toast.makeText(this, "⚡ زمن الاستجابة (Ping): ${currentPingMs} ms • 60 FPS متزامن", Toast.LENGTH_SHORT).show()        
}

        // 2. Crown Chip: Host Role & Room Code Copy
chipCrown.setOnClickListener {

            val activeCode = netplaySession?.currentRoom?.roomCode ?: "SOLO"
            val isHost = matchCoordinator?.isHost == true
            val roleStr = if (isHost) "👑 المضيف (P1)" else "🎮 اللاعب 2 (Guest)"
            try {

                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("PS1 Room Code", activeCode)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(this, "$roleStr • تم نسخ كود الغرفة: $activeCode", Toast.LENGTH_SHORT).show()
            
}
 catch (e: Exception) {

                Toast.makeText(this, "$roleStr • كود الغرفة: $activeCode", Toast.LENGTH_SHORT).show()
            
}
}

        // 3. Live Chip: Stream & Sync Status
chipLive.setOnClickListener {

            Toast.makeText(this, "$currentSyncStatusString\n● البث نشط بدون تأخير", Toast.LENGTH_SHORT).show()
        
}

        // --- Bottom Dock Bar Icons ---
// 1. Settings Gear ⚙️ (Includes Force Sync // (Includes Force Sync for Host, Sound, ROM/BIOS, Controller, Lobbies, Leave Room)
        btnDockSettings.setOnClickListener {

            openIsolatedSettings()
        
}

        // 2. Two people (room participants) 👥
btnDockMembers.setOnClickListener {

            NativeFeatureDialogs.showMembersDialog(this, netplaySession) {

                matchCoordinator?.isGameStarted = true
            
}
}

        // 3. Joystick gamepad 🎮 (Toggle on-screen touch controller)
btnDockGamepad.setOnClickListener {

            val overlay = virtualTouchOverlay ?: return@setOnClickListener
            overlay.isControlsVisible = !overlay.isControlsVisible
            overlay.invalidate()
val state = if (overlay.isControlsVisible) "تم إظهار أزرار التحكم اللمسية ✅" else "تم إخفاء أزرار التحكم اللمسية ❌"
            Toast.makeText(this, "$state (اضغط مطولاً لتخصيص الحجم والشفافية)", Toast.LENGTH_SHORT).show()
        
}

        btnDockGamepad.setOnLongClickListener {

            openTouchCustomizer()
            true
        
}

        // 4. Walkie-talkie radio 📻
btnDockVoice.setOnClickListener {

            NativeFeatureDialogs.showWalkieTalkieDialog(this)
        
}

        // 5. Camera Arena dialog (Icon-only without text) 📷
btnDockCamera.setOnClickListener {

            showCameraArenaDialog()
        
}

        // 6. Chat bubble with dots 💬
btnDockChat.setOnClickListener {

            NativeFeatureDialogs.showChatDialog(this, netplaySession)
        
}
}

    private fun showCameraArenaDialog() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            cameraPermissionLauncher?.launch(Manifest.permission.CAMERA)
            return
        
}

        val isHost = matchCoordinator?.isHost != false
        NativeFeatureDialogs.showCameraRoomDialog(
            context = this,
            cameraHelper = roomCameraHelper,
            isHost = isHost,
            onCameraToggleRequested = {
 targetTexture, updateUiState ->
                val helper = roomCameraHelper ?: return@showCameraRoomDialog
                if (helper.isCameraOn.get()) {

                    helper.closeCamera()
                    btnDockCamera.clearColorFilter()
                    updateUiState(false)
                    Toast.makeText(this, "تم إيقاف الكاميرا ❌", Toast.LENGTH_SHORT).show()
                
}
 else {

                    btnDockCamera.setColorFilter(Color.parseColor("#10B981"))
                    helper.startCamera(targetTexture)
                    updateUiState(true)
                    Toast.makeText(this, "تم تشغيل الكاميرا 📷", Toast.LENGTH_SHORT).show()
                
}
}
,
            onCameraFlipRequested = {
 activeTexture ->
                val helper = roomCameraHelper ?: return@showCameraRoomDialog
                if (helper.isCameraOn.get()) {

                    helper.switchCameraFacing(activeTexture)
                    Toast.makeText(this, "تم تغيير اتجاه الكاميرا 🔄", Toast.LENGTH_SHORT).show()
                
}
 else {

                    Toast.makeText(this, "يرجى تشغيل الكاميرا أولاً لتغيير الاتجاه", Toast.LENGTH_SHORT).show()
                
}
}
,
            onHostCloseAllCameras = {

                roomCameraHelper?.closeCamera()
                btnDockCamera.clearColorFilter()
                netplaySession?.sendChatMessage("[SYSTEM]: 🚫 قام المضيف بإيقاف كاميرات الغرفة")
            
}

        )
    
}

    private fun enterRoomScreen(roomCode: String, isHost: Boolean) {

        matchCoordinator = MatchCoordinator(isHost, netplaySession?.getTransport())
        // Active Speaker Callback // Active Speaker Callback for Dock voice meter
        LiveKitNetplayManager.onActiveSpeakersChanged = {
 speakers ->
            runOnUiThread {

                voiceMeterDock.setSpeaking(speakers.isNotEmpty())
            
}
}

        if (isHost) {

            // Host is authoritative
matchCoordinator?.isWaitingForPeer = false
            currentSyncStatusString = "🟢 وضع المضيف: جاهز للمزامنة اللحظية (Lockstep)"
            layoutGuestStreamBadge?.visibility = View.GONE
            emptyState.text = "🎮 Combat 3 PS1 Arena\n\nمحاكي PlayStation 1 الأصلي جاهز للتشغيل والمزامنة التامة"
                        matchCoordinator?.onAutoSyncRequested = {
 frame ->
                if (matchCoordinator?.isPeerConnected == true) {

                    val state = NativeCoreBridge.safeSaveState()
                    globalNetplayEngine?.sendStateSnapshot(frame, state)
                
}
}
}
 else {

            // Guest must run the ROM locally // Guest must run the ROM locally for lockstep sync
            matchCoordinator?.isWaitingForPeer = true
            matchCoordinator?.isRemotePlayGuest = false
            currentSyncStatusString = "🟡 متصل كضيف: بانتظار إشارة بدء المزامنة من المضيف..."
            layoutGuestStreamBadge?.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            emptyState.text = "🎮 متصل كـ ضيف\n\nالمحاكي جاهز. يرجى انتظار المضيف لتعيين أدوار اللاعبين وبدء المباراة اللحظية!"
        
}

                globalNetplayEngine?.setOnConnectionStateListener {
 connected, pingMs ->
            runOnUiThread {

                currentPingMs = if (connected) pingMs else 999L
            
}
}

        layoutRoomActive.visibility = View.VISIBLE
    
}

    private fun exitRoomScreen() {

        isGameRunning = false
        roomCameraHelper?.closeCamera()
        btnDockCamera.clearColorFilter()
        globalNetplayEngine?.stop()
        directSocketEngine?.stop()
        netplaySession?.leaveRoom()
        layoutGuestStreamBadge?.visibility = View.GONE
        layoutRoomActive.visibility = View.GONE
        virtualTouchOverlay?.isControlsVisible = false
        virtualTouchOverlay?.invalidate()
        ps1AudioPlayer?.stop()
    
}

    private fun startGameSurface() {

        if (gameSurfaceView != null) {

            NativeCoreBridge.safeSetSurface(gameSurfaceView?.holder?.surface)
            return
        
}

        runCatching {

            val surface = GameSurfaceView(this)
            surface.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            gameContainer.addView(surface, 0)
            gameSurfaceView = surface
            emptyState.visibility = View.GONE
            if (surface.holder.surface.isValid) NativeCoreBridge.safeSetSurface(surface.holder.surface)
        
}
.onFailure {

            android.util.Log.e("MainActivity", "Surface creation failed", it)
            Toast.makeText(this, "تعذر تشغيل شاشة اللعبة", Toast.LENGTH_LONG).show()
        
}
}

    private fun startGameLoop() {

        if (isGameRunning) return
        isGameRunning = true
        ps1AudioPlayer?.start()
val scope = activityScope ?: return
        scope.launch(Dispatchers.Default) {

            var nextFrameNs = System.nanoTime()
val frameDurationNs = 16_666_667L // 60 FPS
while (isGameRunning) {

                try {

                    val input = gamepadManager?.getCurrentInputMask() ?: 0
                    matchCoordinator?.tickFrame(input)
                
}
 catch (e: Throwable) {

                    android.util.Log.e("MainActivity", "Frame tick exception", e)
                
}

                nextFrameNs += frameDurationNs
                val sleepNs = nextFrameNs - System.nanoTime()
                if (sleepNs > 2_000_000L) {

                    delay(sleepNs / 1_000_000L)
                
}
 else if (sleepNs > 0) {

                    yield()
                
}
 else {

                    nextFrameNs = System.nanoTime()
                    yield()
                
}
}
}
}

    private fun openIsolatedSettings() {

        runCatching {

            val dialog = IsolatedSettingsBottomSheet.newInstance()
val room = netplaySession?.currentRoom
            if (room != null) {
                dialog.roomTitle = room.roomName
                dialog.roomSubtitle = "● متصل • GGPO Rollback • كود: ${room.roomCode}"            
}

            dialog.currentRomTitle = currentRomName
            dialog.currentBiosTitle = currentBiosName
            dialog.currentVolume = ps1AudioPlayer?.currentVolume ?: 1.0f
            dialog.isMuted = ps1AudioPlayer?.isMuted ?: false
            dialog.isHost = matchCoordinator?.isHost == true
            dialog.syncStatusText = currentSyncStatusString
            dialog.onForceSyncClicked = {

                val isHost = matchCoordinator?.isHost == true
                if (isHost) {

                    val state = NativeCoreBridge.safeSaveState()
val currentF = matchCoordinator?.getCurrentFrameNumber() ?: 0L
                    globalNetplayEngine?.sendStateSnapshot(currentF, state)
                    globalNetplayEngine?.sendGameStartSync(currentF)
                    currentSyncStatusString = "🟢 تم إرسال لقطة المضيف وفرض المزامنة (الإطار: #$currentF)"
                    Toast.makeText(this, "🔄 تم فرض مزامنة شاشة المضيف على جميع الضيوف بنجاح!", Toast.LENGTH_SHORT).show()
                
}
}

            dialog.onVolumeChanged = {
 vol ->
                ps1AudioPlayer?.setVolume(vol)
            
}

            dialog.onMuteToggled = {
 muted ->
                ps1AudioPlayer?.setMuted(muted)
            
}

            dialog.onLoadRomClicked = {
 romPickerLauncher?.launch("*/*") 
}

            dialog.onLoadBiosClicked = {
 biosPickerLauncher?.launch("*/*") 
}

            dialog.onLeaveRoomClicked = {

                netplaySession?.leaveRoom()
                exitRoomScreen()
            
}

            dialog.onResetMappingClicked = {
 gamepadManager?.resetMappingsToDefault() 
}

            dialog.onCustomizeTouchClicked = {
 openTouchCustomizer() 
}

            dialog.onOpenLobbiesClicked = {
 openLobbyBrowser() 
}

            dialog.show(supportFragmentManager, IsolatedSettingsBottomSheet.TAG)
        
}
.onFailure {

            android.util.Log.e("MainActivity", "Settings failed", it)
            Toast.makeText(this, "تعذر فتح الإعدادات", Toast.LENGTH_SHORT).show()
        
}
}

    private fun openLobbyBrowser() {

        NativeFeatureDialogs.showLobbyBrowserDialog(this, netplaySession) {
 selectedCode ->
            netplaySession?.joinRoom(selectedCode)
            enterRoomScreen(selectedCode, isHost = false)
            Toast.makeText(this, "تم الاتصال بالغرفة: $selectedCode", Toast.LENGTH_SHORT).show()
        
}
}

        private fun openTouchCustomizer() {
        val overlay = virtualTouchOverlay ?: return
        NativeFeatureDialogs.showTouchCustomizerDialog(this, overlay)
    }

    private fun handleRomSelected(uri: Uri) {
        val manager = coreManager ?: return
        runCatching {
            val fileName = getFileNameFromUri(uri) ?: "game_${System.currentTimeMillis()}.bin"
            contentResolver.openInputStream(uri)?.use { stream ->
                val success = manager.importAndLoadRom(stream, fileName)
                if (success) {
                    currentRomName = fileName
                    temporaryStateStore?.clear()
                    startGameSurface()
                    startGameLoop()
                    Toast.makeText(this, "تم تحميل اللعبة بنجاح: $fileName", Toast.LENGTH_SHORT).show()
                    if (matchCoordinator?.isHost == true) {
                        globalNetplayEngine?.sendGameInfo(fileName)
                    }
                } else {
                    val detail = manager.getLastError().ifBlank { "الملف غير صالح أو لم يتم العثور على رأس PS1" }
                    Toast.makeText(this, "فشل تحميل اللعبة: $detail", Toast.LENGTH_LONG).show()
                }
            } ?: Toast.makeText(this, "تعذر قراءة ملف اللعبة", Toast.LENGTH_LONG).show()
        }.onFailure {
            android.util.Log.e("MainActivity", "ROM loading failed", it)
            Toast.makeText(this, "حدث خطأ أثناء تحميل اللعبة: ${it.javaClass.simpleName}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleBiosSelected(uri: Uri) {
        val manager = coreManager ?: return
        runCatching {
            val fileName = getFileNameFromUri(uri) ?: "SCPH1001.BIN"
            contentResolver.openInputStream(uri)?.use { stream ->
                if (manager.saveCustomBios(stream, fileName)) {
                    currentBiosName = fileName
                    Toast.makeText(this, "تم حفظ BIOS: $fileName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "تعذر حفظ BIOS", Toast.LENGTH_LONG).show()
                }
            }
        }.onFailure {
            android.util.Log.e("MainActivity", "BIOS loading failed", it)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (i >= 0 && cursor.moveToFirst()) cursor.getString(i) else null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
        if (event != null && gamepadManager?.onKeyDown(keyCode, event) == true) true else super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
        if (event != null && gamepadManager?.onKeyUp(keyCode, event) == true) true else super.onKeyUp(keyCode, event)

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean =
        if (event != null && gamepadManager?.onGenericMotionEvent(event) == true) true else super.onGenericMotionEvent(event)

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            run {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }

    override fun onDestroy() {
        isGameRunning = false
        activityScope?.cancel()
        runCatching { roomCameraHelper?.closeCamera() }
        runCatching { ps1AudioPlayer?.stop() }
        runCatching { netplaySession?.leaveRoom() }
        runCatching { coreManager?.unload() }
        gameSurfaceView = null
        super.onDestroy()
    }
}
