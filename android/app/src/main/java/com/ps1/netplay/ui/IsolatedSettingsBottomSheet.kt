package com.ps1.netplay.ui
import android.widget.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ps1.netplay.R

/** * Modern PS5/Android 14 Bottom Sheet Settings Dialog. * Integrates Room Header, PS1 High-Res Audio Controls (Volume/Mute), Controller Layout, ROM/BIOS, and Navigation. */class IsolatedSettingsBottomSheet : BottomSheetDialogFragment() {
    var onLeaveRoomClicked: (() -> Unit)? = null
    var onResetMappingClicked: (() -> Unit)? = null
    var onCustomizeTouchClicked: (() -> Unit)? = null
    var onOpenLobbiesClicked: (() -> Unit)? = null
    var onLoadRomClicked: (() -> Unit)? = null
    var onLoadBiosClicked: (() -> Unit)? = null
    var onVolumeChanged: ((Float) -> Unit)? = null
    var onMuteToggled: ((Boolean) -> Unit)? = null
    var onForceSyncClicked: (() -> Unit)? = null
    var isHost: Boolean = false
    var syncStatusText: String = "🟢 المزامنة التامة مفعلة (شاشة المضيف هي المرجع)"
    var roomTitle: String = "غرفة COMBAT 3 PS1"
    var roomSubtitle: String = "● متصل • 60 FPS • محاكي PlayStation 1 الأصلي"
    var currentRomTitle: String = "Combat 3 (Built-in)"
    var currentBiosTitle: String = "HLE High-Level Emulation (تلقائي)"
    var currentVolume: Float = 1.0f
    var isMuted: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_isolated_settings, container, false)
    }
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 1. Room Header Info
view.findViewById<TextView>(R.id.txt_dialog_room_title)?.text = roomTitle
        view.findViewById<TextView>(R.id.txt_dialog_room_subtitle)?.text = roomSubtitle
        // 2. Audio Settings
    val txtVolumeLabel = view.findViewById<TextView>(R.id.txt_volume_label)
val seekbarVolume = view.findViewById<SeekBar>(R.id.seekbar_game_volume)
val btnToggleMute = view.findViewById<Button>(R.id.btn_toggle_mute)
fun updateMuteButtonState() {
            if (isMuted) {
                btnToggleMute?.text = "🔇 مكتوم"
                txtVolumeLabel?.text = "🔇 مستوى صوت اللعبة: مكتوم"
            } else {
                val progress = seekbarVolume?.progress ?: ((currentVolume * 100).toInt())
                btnToggleMute?.text = "🔊 تشغيل"
                txtVolumeLabel?.text = "🔊 مستوى صوت اللعبة: $progress%"
            }
        }
val initialProgress = (currentVolume * 100).toInt().coerceIn(0, 100)
        seekbarVolume?.progress = initialProgress
        updateMuteButtonState()
        seekbarVolume?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val vol = progress / 100f
                    currentVolume = vol
                    if (isMuted && progress > 0) {
                        isMuted = false
                        onMuteToggled?.invoke(false)
                    }
                    updateMuteButtonState()
                    onVolumeChanged?.invoke(vol)
                }
            }
override fun onStartTrackingTouch(seekBar: SeekBar?) {}
override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        btnToggleMute?.setOnClickListener {
            isMuted = !isMuted
            onMuteToggled?.invoke(isMuted)
            updateMuteButtonState()
        }
        // 3. ROM & BIOS
view.findViewById<TextView>(R.id.txt_rom_status)?.text =
            getString(R.string.status_current_rom, currentRomTitle)
        view.findViewById<TextView>(R.id.txt_bios_status)?.text =
            getString(R.string.status_current_bios, currentBiosTitle)
        view.findViewById<Button>(R.id.btn_load_rom)?.setOnClickListener {
            onLoadRomClicked?.invoke()
            dismiss()
        }
        view.findViewById<Button>(R.id.btn_load_bios)?.setOnClickListener {
            onLoadBiosClicked?.invoke()
            dismiss()
        }
        // 4. Controller Options
view.findViewById<Button>(R.id.btn_customize_touch)?.setOnClickListener {
            onCustomizeTouchClicked?.invoke()
            dismiss()
        }
        view.findViewById<Button>(R.id.btn_reset_controls)?.setOnClickListener {
            onResetMappingClicked?.invoke()
        }
        // 5. State Sync (Host Only button, status // (Host Only button, status for all)
    val syncSection = view.findViewById<View>(R.id.layout_settings_sync)
val txtSyncStatus = view.findViewById<TextView>(R.id.txt_settings_sync_status)
val btnForceSync = view.findViewById<Button>(R.id.btn_settings_force_sync)
        txtSyncStatus?.text = syncStatusText
        if (isHost) {
            btnForceSync?.visibility = View.VISIBLE
            btnForceSync?.setOnClickListener {
                onForceSyncClicked?.invoke()
                txtSyncStatus?.text = "🟢 تم إرسال لقطة شاشة المضيف وفرض المزامنة!"
            }
        } else {
            // Guest does NOT need a forced sync button
btnForceSync?.visibility = View.GONE
        }
        // 6. Navigation
view.findViewById<Button>(R.id.btn_open_lobbies)?.setOnClickListener {
            onOpenLobbiesClicked?.invoke()
            dismiss()
        }
        view.findViewById<Button>(R.id.btn_leave_room)?.setOnClickListener {
            onLeaveRoomClicked?.invoke()
            dismiss()
        }
        view.findViewById<View>(R.id.btn_close_settings)?.setOnClickListener {
            dismiss()
        }
    }
    companion object {
        const val TAG = "IsolatedSettings"
        fun newInstance(): IsolatedSettingsBottomSheet = IsolatedSettingsBottomSheet()
    }}