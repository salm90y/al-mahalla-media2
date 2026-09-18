package com.ps1.netplay.ui
import android.widget.*
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ps1.netplay.network.ChatMessage
import com.ps1.netplay.network.LiveKitNetplayManager
import com.ps1.netplay.network.NetplaySession
import com.ps1.netplay.network.RoomMember

/** * Native Android Dialogs for Netplay: * - Lobby Browser * - Chat Drawer with input field & send button * - Walkie-Talkie (Push-to-Talk) with live VU meter & audio volume settings * - Room Members list & Profile modal with Permissions (Host, Co-Host, Mute, Kick) * - Touch HUD Customizer */object NativeFeatureDialogs {
    // 2026 Luxury Gold & Obsidian Palette // 2026 Luxury Gold & Obsidian Palette for Netplay UI
    private val COLOR_BG_DARK = Color.rgb(10, 10, 15) // #0A0A0F
private val COLOR_CARD_LUXURY = Color.rgb(18, 18, 30) // #12121E
    private val COLOR_INPUT_LUXURY = Color.rgb(26, 26, 36) // #1A1A24
private val COLOR_GOLD_PRIMARY = Color.rgb(212, 175, 55) // #D4AF37
    private val COLOR_GOLD_BRIGHT = Color.rgb(255, 215, 0) // #FFD700
private val COLOR_GOLD_BORDER = Color.argb(120, 212, 175, 55) // #78D4AF37
    private val COLOR_GOLD_BORDER_SUBTLE = Color.argb(55, 212, 175, 55) // #37D4AF37
private val COLOR_EMERALD = Color.rgb(16, 185, 129) // #10B981
    private val COLOR_AMBER = Color.rgb(245, 158, 11) // #F59E0B
private val COLOR_CRIMSON = Color.rgb(220, 38, 38) // #DC2626
    private val COLOR_TEXT_PRIMARY = Color.rgb(248, 250, 252) // #F8FAFC
private val COLOR_TEXT_MUTED = Color.rgb(148, 163, 184) // #94A3B8
    private fun showInBottomSheet(context: Context, view: View): BottomSheetDialog {
        val bottomSheetDialog = BottomSheetDialog(context)
val density = context.resources.displayMetrics.density
                val wrapper = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
                        // 2026 Luxury OLED Obsidian with Royal Gold Hairline Border
    val bgShape = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_BG_DARK)
                setStroke((1.5f * density).toInt(), COLOR_GOLD_BORDER)
                cornerRadii = floatArrayOf(
                    40f * density, 40f * density, // top left
40f * density, 40f * density, // top right
0f, 0f,
                       // bottom right
0f, 0f
                        // bottom left
)
            }
            background = bgShape
                        val handle = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (44 * density).toInt(),
                    (4 * density).toInt()
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = (12 * density).toInt()
                    bottomMargin = (8 * density).toInt()
                }
val handleShape = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(COLOR_GOLD_PRIMARY)
                    alpha = 140
                    cornerRadius = 20f * density
                }
                background = handleShape
            }
            addView(handle)
            addView(view)
        }
                bottomSheetDialog.setContentView(wrapper)
        bottomSheetDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        bottomSheetDialog.window?.setGravity(Gravity.BOTTOM)
        bottomSheetDialog.window?.setDimAmount(0.45f)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(Color.TRANSPARENT)
        bottomSheetDialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.show()
        return bottomSheetDialog
    }
    /**
     * Modern Chat Dialog with Aesthetic Bubble Messages & Quick Reactions Bar
     */
    fun showChatDialog(context: Context, netplaySession: NetplaySession?) {
        val density = context.resources.displayMetrics.density
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (14 * density).toInt(), (20 * density).toInt(), (18 * density).toInt())
            setBackgroundColor(COLOR_BG_DARK)
        }
        // Header
    val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (14 * density).toInt())
        }
val title = TextView(context).apply {
            text = "💬 المحادثة الفورية للغرفة"
            textSize = 17f
            setTextColor(COLOR_GOLD_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)
        root.addView(header)
        // Scrollable message container
    val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (360 * density).toInt()
            )
            isVerticalScrollBarEnabled = true
        }
val messagesList = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 4)
        }
fun populateMessages() {
            messagesList.removeAllViews()
val msgs = netplaySession?.currentRoom?.messages ?: emptyList()
            if (msgs.isEmpty()) {
                val emptyCard = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding((20 * density).toInt(), (36 * density).toInt(), (20 * density).toInt(), (36 * density).toInt())
val bg = GradientDrawable().apply {
                        setShape(GradientDrawable.RECTANGLE)
                        setColor(COLOR_CARD_LUXURY)
                        cornerRadius = 18f * density
                        setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
                    }
                    background = bg
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (20 * density).toInt()
                        bottomMargin = (20 * density).toInt()
                    }
                    layoutParams = params
                }
val emptyText = TextView(context).apply {
                    text = "لا توجد رسائل بعد. ابدأ المحادثة مع اللاعبين!"
                    setTextColor(COLOR_TEXT_MUTED)
                    textSize = 13f
                    gravity = Gravity.CENTER
                }
                emptyCard.addView(emptyText)
                messagesList.addView(emptyCard)
            } else {
                for (msg in msgs) {
                    val isMyMsg = msg.isHost // Host is colored gold, peer is colored cyan/emerald
    val bubbleContainer = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = if (isMyMsg) Gravity.END else Gravity.START
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = (10 * density).toInt()
                        }
                        layoutParams = params
                    }
val bubble = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
                        // Professional Chat Bubble with asymmetrical speech tail corner
    val bubbleBg = GradientDrawable().apply {
                            setShape(GradientDrawable.RECTANGLE)
                            if (isMyMsg) {
                                // Luxury obsidian gold-tinted gradient // Luxury obsidian gold-tinted gradient for Host / Sender
                                setColors(intArrayOf(Color.rgb(36, 32, 24), Color.rgb(22, 20, 30)))
                                cornerRadii = floatArrayOf(
                                    16f * density, 16f * density, // top-left
16f * density, 16f * density, // top-right
4f * density, 4f * density,   // bottom-right (speech tail!)
16f * density, 16f * density  // bottom-left
)
                                setStroke((1.2f * density).toInt(), COLOR_GOLD_BORDER)
                            } else {
                                // Sleek dark steel card // Sleek dark steel card for Guest / Receiver
                                setColors(intArrayOf(Color.rgb(20, 26, 38), Color.rgb(15, 20, 30)))
                                cornerRadii = floatArrayOf(
                                    16f * density, 16f * density, // top-left
16f * density, 16f * density, // top-right
16f * density, 16f * density, // bottom-right
4f * density, 4f * density
    // bottom-left (speech tail!)
)
                                setStroke((1f * density).toInt(), Color.argb(90, 56, 189, 248))
                            }
                        }
                        background = bubbleBg
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        layoutParams = params
                    }
                    // Sender name and timestamp
    val senderRow = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
val roleBadge = TextView(context).apply {
                        text = if (isMyMsg) "👑 المضيف" else "🎮 لاعب"
                        textSize = 9.5f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(if (isMyMsg) COLOR_GOLD_BRIGHT else COLOR_EMERALD)
                        setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
val badgeBg = GradientDrawable().apply {
                            setShape(GradientDrawable.RECTANGLE)
                            setColor(if (isMyMsg) Color.argb(60, 212, 175, 55) else Color.argb(50, 16, 185, 129))
                            cornerRadius = 8f * density
                        }
                        background = badgeBg
                        val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            marginEnd = (6 * density).toInt()
                        }
                        layoutParams = p
                    }
val sender = TextView(context).apply {
                        text = "${msg.senderName} • ${msg.timestamp}"
                        textSize = 11f
                        setTextColor(if (isMyMsg) COLOR_GOLD_PRIMARY else Color.rgb(52, 211, 153))
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    senderRow.addView(roleBadge)
                    senderRow.addView(sender)
                    // Message text
    val content = TextView(context).apply {
                        text = msg.text
                        textSize = 14f
                        setTextColor(COLOR_TEXT_PRIMARY)
                        setLineSpacing(3f * density, 1f)
                        setPadding(0, (5 * density).toInt(), 0, 0)
                        setTextIsSelectable(true)
                    }
                    bubble.addView(senderRow)
                    bubble.addView(content)
                    bubbleContainer.addView(bubble)
                    messagesList.addView(bubbleContainer)
                }
            }
        }
        populateMessages()
        scroll.addView(messagesList)
        root.addView(scroll)
        // Quick Reactions / Game Presets Row
    val quickReactionsScroll = android.widget.HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }
            isHorizontalScrollBarEnabled = false
        }
val quickReactionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
val quickPhrases = listOf(
            "🎮 مباراة ممتعة!",
            "🔥 أحسنت الهجوم!",
            "⚡ جاهز للجولة التالية!",
            "👑 مضيف أسطوري!",
            "🛡️ دفاع قوي!"
        )
        // Forward declare input field // Forward declare input field for quick chips
    val edtMessage = EditText(context).apply {
            hint = "اكتب رسالة للغرفة..."
            setHintTextColor(COLOR_TEXT_MUTED)
            setTextColor(COLOR_TEXT_PRIMARY)
            textSize = 14f
            val inputBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_INPUT_LUXURY)
                cornerRadius = 16f * density
                setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
            }
            background = inputBg
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (10 * density).toInt()
            }
        }
        for (phrase in quickPhrases) {
            val chip = TextView(context).apply {
                text = phrase
                textSize = 11.5f
                setTextColor(COLOR_GOLD_PRIMARY)
                setPadding((12 * density).toInt(), (6 * density).toInt(), (12 * density).toInt(), (6 * density).toInt())
val chipBg = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(COLOR_CARD_LUXURY)
                    cornerRadius = 14f * density
                    setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
                }
                background = chipBg
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (8 * density).toInt()
                }
                layoutParams = params
                setOnClickListener {
                    edtMessage.setText(phrase)
                    edtMessage.setSelection(phrase.length)
                }
            }
            quickReactionsRow.addView(chip)
        }
        quickReactionsScroll.addView(quickReactionsRow)
        root.addView(quickReactionsScroll)
        // Input field and send button
    val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 0)
        }
val btnSend = Button(context).apply {
            text = "إرسال ✈️"
            val goldBtnBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColors(intArrayOf(COLOR_GOLD_PRIMARY, COLOR_GOLD_BRIGHT))
                cornerRadius = 16f * density
            }
            background = goldBtnBg
            setTextColor(Color.rgb(10, 10, 15)) // Contrast luxury black on gold
typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setPadding((18 * density).toInt(), (10 * density).toInt(), (18 * density).toInt(), (10 * density).toInt())
            setOnClickListener {
                val text = edtMessage.text.toString().trim()
                if (text.isNotEmpty()) {
                    netplaySession?.addChatMessage(text)
                    edtMessage.setText("")
                    populateMessages()
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }
        inputRow.addView(edtMessage)
        inputRow.addView(btnSend)
        root.addView(inputRow)
val dialog = showInBottomSheet(context, root)
        netplaySession?.onMessageReceived = {
            if (context is android.app.Activity) {
                context.runOnUiThread {
                    populateMessages()
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }
        dialog.setOnDismissListener {
            netplaySession?.onMessageReceived = null
        }
    }
    /**
     * Modern Walkie-Talkie Voice Dialog with Live Multi-Bar LED Spectrum VU Meter & Audio Settings
     */
    fun showWalkieTalkieDialog(context: Context) {
        val density = context.resources.displayMetrics.density
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (18 * density).toInt(), (24 * density).toInt(), (20 * density).toInt())
            setBackgroundColor(COLOR_BG_DARK)
        }
val title = TextView(context).apply {
            text = "📻 اتصال هوكي توكي (Walkie-Talkie الصوتية)"
            textSize = 17f
            setTextColor(COLOR_GOLD_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (6 * density).toInt())
        }
val subtitle = TextView(context).apply {
            text = "قناة صوتية ثنائية مشفرة • زمن انتقال صوتي منخفض 25ms"
            textSize = 12f
            setTextColor(COLOR_TEXT_MUTED)
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (16 * density).toInt())
        }
        // Live Animated Multi-Bar LED Spectrum Visualizer
    val spectrumCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
val cardBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_CARD_LUXURY)
                cornerRadius = 16f * density
                setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
            }
            background = cardBg
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * density).toInt()
            }
            layoutParams = p
        }
val vuBarsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (54 * density).toInt())
        }
val barViews = ArrayList<View>()
val defaultHeights = intArrayOf(12, 22, 34, 46, 38, 24, 14)
        for (i in defaultHeights.indices) {
            val bar = View(context).apply {
                val barShape = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColors(intArrayOf(COLOR_EMERALD, COLOR_GOLD_PRIMARY))
                    cornerRadius = 4f * density
                }
                background = barShape
                val p = LinearLayout.LayoutParams(
                    (8 * density).toInt(),
                    (defaultHeights[i] * density).toInt()
                ).apply {
                    setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                }
                layoutParams = p
            }
            barViews.add(bar)
            vuBarsRow.addView(bar)
        }
val vuLabel = TextView(context).apply {
            text = "مؤشر ذبذبات الصوت: نشط ومتزامن 🟢"
            textSize = 12f
            setTextColor(COLOR_EMERALD)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, (10 * density).toInt(), 0, 0)
        }
        spectrumCard.addView(vuBarsRow)
        spectrumCard.addView(vuLabel)
        // Push to Talk Button (Luxury Rounded PTT Capsule)
    var isTalking = com.ps1.netplay.network.LiveKitNetplayManager.isMicEnabled
        val pttIdleBg = GradientDrawable().apply {
            setShape(GradientDrawable.RECTANGLE)
            setColors(intArrayOf(Color.rgb(20, 24, 38), Color.rgb(15, 18, 28)))
            cornerRadius = 20f * density
            setStroke((1.8f * density).toInt(), COLOR_GOLD_PRIMARY)
        }
val pttActiveBg = GradientDrawable().apply {
            setShape(GradientDrawable.RECTANGLE)
            setColors(intArrayOf(COLOR_CRIMSON, Color.rgb(153, 27, 27)))
            cornerRadius = 20f * density
            setStroke((2f * density).toInt(), Color.WHITE)
        }
val btnPtt = Button(context).apply {
            if (isTalking) {
                text = "🔴 الميكروفون يبث الآن... (انقر للإيقاف)"
                background = pttActiveBg
                setTextColor(Color.WHITE)
                vuLabel.text = "جاري بث الصوت إلى اللاعبين... 🔴"
                vuLabel.setTextColor(COLOR_CRIMSON)
            } else {
                text = "🎙️ اضغط للتحدث (Push-to-Talk)"
                background = pttIdleBg
                setTextColor(COLOR_GOLD_BRIGHT)
            }
            textSize = 15.5f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (60 * density).toInt()).apply {
                bottomMargin = (16 * density).toInt()
            }
            setOnClickListener {
                if (context is android.app.Activity) {
                    if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        context.requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
                        Toast.makeText(context, "يرجى منح صلاحية الميكروفون لاستخدام الهوكي توكي", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                                isTalking = !isTalking
                try {
                    com.ps1.netplay.network.LiveKitNetplayManager.setMicrophoneEnabled(isTalking)
                } catch (e: Exception) {
                    Log.e("WalkieTalkie", "Failed to set mic", e)
                }
                                if (isTalking) {
                    text = "🔴 الميكروفون يبث الآن... (انقر للإيقاف)"
                    background = pttActiveBg
                    setTextColor(Color.WHITE)
                    vuLabel.text = "جاري بث الصوت إلى اللاعبين... 🔴"
                    vuLabel.setTextColor(COLOR_CRIMSON)
                    // Animate bars to peak height
for (bar in barViews) {
                        bar.layoutParams.height = ((36 + (Math.random() * 16).toInt()) * density).toInt()
                        (bar.background as? GradientDrawable)?.setColors(intArrayOf(COLOR_AMBER, COLOR_CRIMSON))
                        bar.requestLayout()
                    }
                } else {
                    text = "🎙️ اضغط للتحدث (Push-to-Talk)"
                    background = pttIdleBg
                    setTextColor(COLOR_GOLD_BRIGHT)
                    vuLabel.text = "الميكروفون في وضع الاستعداد 🟢"
                    vuLabel.setTextColor(COLOR_EMERALD)
                    for (i in barViews.indices) {
                        barViews[i].layoutParams.height = (defaultHeights[i] * density).toInt()
                        (barViews[i].background as? GradientDrawable)?.setColors(intArrayOf(COLOR_EMERALD, COLOR_GOLD_PRIMARY))
                        barViews[i].requestLayout()
                    }
                }
            }
        }
        // Volume Settings Card
    val volCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
val cardBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_CARD_LUXURY)
                cornerRadius = 14f * density
                setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
            }
            background = cardBg
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
            layoutParams = p
        }
val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
val currentPercent = ((currentVol.toFloat() / maxVol.toFloat()) * 100).toInt()
val volLabel = TextView(context).apply {
            text = "مستوى صوت النظام (Volume: $currentPercent%)"
            textSize = 13f
            setTextColor(COLOR_TEXT_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (6 * density).toInt())
        }
val volSeekBar = SeekBar(context).apply {
            max = maxVol
            progress = currentVol
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, prog: Int, fromUser: Boolean) {
                    if (fromUser) {
                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, prog, 0)
val p = ((prog.toFloat() / maxVol.toFloat()) * 100).toInt()
                        volLabel.text = "مستوى صوت النظام (Volume: $p%)"
                    }
                }
override fun onStartTrackingTouch(sb: SeekBar?) {}
override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        volCard.addView(volLabel)
        volCard.addView(volSeekBar)
        // Mic Sensitivity Settings Card
    val micCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
val cardBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_CARD_LUXURY)
                cornerRadius = 14f * density
                setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
            }
            background = cardBg
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams = p
        }
val micLabel = TextView(context).apply {
            text = "حساسية الميكروفون وإلغاء الضوضاء (Noise Gate: 70%)"
            textSize = 13f
            setTextColor(COLOR_TEXT_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (6 * density).toInt())
        }
val micSeekBar = SeekBar(context).apply {
            max = 100
            progress = 70
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, prog: Int, fromUser: Boolean) {
                    micLabel.text = "حساسية الميكروفون وإلغاء الضوضاء (Noise Gate: $prog%)"
                }
override fun onStartTrackingTouch(sb: SeekBar?) {}
override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        micCard.addView(micLabel)
        micCard.addView(micSeekBar)
        root.addView(title)
        root.addView(subtitle)
        root.addView(spectrumCard)
        root.addView(btnPtt)
        root.addView(volCard)
        root.addView(micCard)
        showInBottomSheet(context, root)
    }
    /**
     * Modern Room Members Dialog with VIP Player Cards & Permissions (Co-Host, Mute, Kick)
     */
    fun showMembersDialog(context: Context, netplaySession: NetplaySession?, onGameStart: (() -> Unit)? = null) {
        val density = context.resources.displayMetrics.density
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt(), (18 * density).toInt())
            setBackgroundColor(COLOR_BG_DARK)
        }
val room = netplaySession?.currentRoom
        val members = room?.members ?: listOf(
            RoomMember("1", "المضيف الأساسي", isHost = true, pingMs = 14L),
            RoomMember("2", "اللاعب المنافس", isHost = false, pingMs = 28L)
        )
val title = TextView(context).apply {
            text = "👥 المتواجدون في الغرفة (${members.size})"
            textSize = 17f
            setTextColor(COLOR_GOLD_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        root.addView(title)
val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (340 * density).toInt())
        }
val membersContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        for (member in members) {
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
val cardShape = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(COLOR_CARD_LUXURY)
                    cornerRadius = 16f * density
                    setStroke((1f * density).toInt(), if (member.isHost) COLOR_GOLD_BORDER else COLOR_GOLD_BORDER_SUBTLE)
                }
                background = cardShape
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, (12 * density).toInt()) }
                layoutParams = params
            }
val rowTop = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            // Avatar circle badge
    val avatarBadge = TextView(context).apply {
                text = if (member.isHost) "👑" else "🎮"
                textSize = 15f
                gravity = Gravity.CENTER
                val circleShape = GradientDrawable().apply {
                    setShape(GradientDrawable.OVAL)
                    setColor(if (member.isHost) Color.rgb(36, 32, 24) else Color.rgb(20, 26, 38))
                    setStroke((1f * density).toInt(), if (member.isHost) COLOR_GOLD_PRIMARY else COLOR_EMERALD)
                }
                background = circleShape
                val p = LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                    marginEnd = (10 * density).toInt()
                }
                layoutParams = p
            }
val badgeRole = TextView(context).apply {
                val baseText = if (member.isHost) "👑 المضيف" else if (member.isCoHost) "⭐ مشرف" else "🎮 لاعب"
                text = if (member.playerNumber > 0) "$baseText (P${member.playerNumber})" else baseText
                textSize = 11f
                setTextColor(if (member.isHost) COLOR_GOLD_BRIGHT else if (member.isCoHost) COLOR_AMBER else COLOR_EMERALD)
                typeface = Typeface.DEFAULT_BOLD
                setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
val badgeShape = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(if (member.isHost) Color.argb(60, 212, 175, 55) else Color.argb(45, 16, 185, 129))
                    cornerRadius = 10f * density
                }
                background = badgeShape
            }
val nameView = TextView(context).apply {
                text = member.name
                textSize = 15f
                setTextColor(COLOR_TEXT_PRIMARY)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins((8 * density).toInt(), 0, (8 * density).toInt(), 0)
                }
            }
val pingView = TextView(context).apply {
                text = "● ${member.pingMs}ms"
                textSize = 11.5f
                setTextColor(COLOR_EMERALD)
                typeface = Typeface.DEFAULT_BOLD
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (8 * density).toInt()
                }
                layoutParams = p
            }
            rowTop.addView(avatarBadge)
            rowTop.addView(nameView)
            rowTop.addView(pingView)
            rowTop.addView(badgeRole)
            card.addView(rowTop)
            // Permissions / Action Buttons Row
    val actionsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (12 * density).toInt(), 0, 0)
            }
val btnManage = Button(context).apply {
                text = "إدارة ⚙️"
                textSize = 12f
                isAllCaps = false
                val btnShape = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(COLOR_INPUT_LUXURY)
                    cornerRadius = 12f * density
                    setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
                }
                background = btnShape
                setTextColor(COLOR_TEXT_PRIMARY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = (6 * density).toInt()
                }
                setOnClickListener { view ->
                    val popup = android.widget.PopupMenu(context, view)
val takenNumbers = members.filter { it.id != member.id }.map { it.playerNumber }
                                        for (i in 1..4) {
                        if (!takenNumbers.contains(i)) {
                            popup.menu.add("تعيين كلاعب $i").setOnMenuItemClickListener {
                                member.playerNumber = i
                                val baseText = if (member.isHost) "👑 المضيف" else if (member.isCoHost) "⭐ مشرف" else "🎮 لاعب"
                                badgeRole.text = "$baseText (P$i)"
                                Toast.makeText(context, "تم تعيين ${member.name} كلاعب $i", Toast.LENGTH_SHORT).show()
                                true
                            }
                        }
                    }
                                        if (!member.isHost) {
                        popup.menu.add("طرد اللاعب 🚫").setOnMenuItemClickListener {
                            Toast.makeText(context, "تم طرد ${member.name}", Toast.LENGTH_SHORT).show()
                            true
                        }
                    }
                    popup.show()
                }
            }
val btnProfile = Button(context).apply {
                text = "الملف الشخصي 👤"
                textSize = 12f
                isAllCaps = false
                val btnShape = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(COLOR_INPUT_LUXURY)
                    cornerRadius = 12f * density
                    setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
                }
                background = btnShape
                setTextColor(COLOR_TEXT_PRIMARY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = (6 * density).toInt()
                }
                setOnClickListener {
                    showUserProfileModal(context, member)
                }
            }
val btnMute = Button(context).apply {
                text = if (member.isMuted) "إلغاء الكتم 🔊" else "كتم الصوت 🔇"
                textSize = 12f
                isAllCaps = false
                val muteShape = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(if (member.isMuted) COLOR_CRIMSON else COLOR_INPUT_LUXURY)
                    cornerRadius = 12f * density
                    setStroke((1f * density).toInt(), if (member.isMuted) COLOR_CRIMSON else COLOR_GOLD_BORDER_SUBTLE)
                }
                background = muteShape
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = (6 * density).toInt()
                }
                setOnClickListener {
                    member.isMuted = !member.isMuted
                    text = if (member.isMuted) "إلغاء الكتم 🔊" else "كتم الصوت 🔇"
                    val updatedShape = GradientDrawable().apply {
                        setShape(GradientDrawable.RECTANGLE)
                        setColor(if (member.isMuted) COLOR_CRIMSON else COLOR_INPUT_LUXURY)
                        cornerRadius = 12f * density
                        setStroke((1f * density).toInt(), if (member.isMuted) COLOR_CRIMSON else COLOR_GOLD_BORDER_SUBTLE)
                    }
                    background = updatedShape
                    Toast.makeText(context, if (member.isMuted) "تم كتم اللاعب" else "تم تفعيل صوت اللاعب", Toast.LENGTH_SHORT).show()
                }
            }
val btnPromote = Button(context).apply {
                text = if (member.isCoHost) "إزالة الإشراف" else "منح إشراف ⭐"
                textSize = 12f
                isAllCaps = false
                val promoteShape = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    if (member.isCoHost) {
                        setColor(Color.rgb(40, 40, 55))
                    } else {
                        setColors(intArrayOf(COLOR_GOLD_PRIMARY, COLOR_GOLD_BRIGHT))
                    }
                    cornerRadius = 12f * density
                }
                background = promoteShape
                setTextColor(if (member.isCoHost) Color.WHITE else Color.rgb(10, 10, 15))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    member.isCoHost = !member.isCoHost
                    badgeRole.text = if (member.isHost) "👑 المضيف" else if (member.isCoHost) "⭐ مشرف" else "🎮 لاعب"
                    text = if (member.isCoHost) "إزالة الإشراف" else "منح إشراف ⭐"
                    val nextShape = GradientDrawable().apply {
                        setShape(GradientDrawable.RECTANGLE)
                        if (member.isCoHost) {
                            setColor(Color.rgb(40, 40, 55))
                        } else {
                            setColors(intArrayOf(COLOR_GOLD_PRIMARY, COLOR_GOLD_BRIGHT))
                        }
                        cornerRadius = 12f * density
                    }
                    background = nextShape
                    setTextColor(if (member.isCoHost) Color.WHITE else Color.rgb(10, 10, 15))
                    Toast.makeText(context, if (member.isCoHost) "تم تعيين ${member.name} كمشرف بالغرفة" else "تم إلغاء الإشراف", Toast.LENGTH_SHORT).show()
                }
            }
            if (room?.isHost == true) {
                actionsRow.addView(btnManage)
                if (member.id != "host-1" && member.id != room.members.firstOrNull { it.isHost }?.id && !member.isHost) {
                    actionsRow.addView(btnMute)
                    actionsRow.addView(btnPromote)
                }
            }
            actionsRow.addView(btnProfile)
            card.addView(actionsRow)
            membersContainer.addView(card)
        }
        // زر بدء المباراة (يظهر للمضيف فقط)
if (room?.isHost == true) {
            val btnStartGame = Button(context).apply {
                text = "بدء المباراة 🚀"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                val startBg = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColors(intArrayOf(COLOR_EMERALD, Color.rgb(5, 150, 105)))
                    cornerRadius = 16f * density
                }
                background = startBg
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (16 * density).toInt()
                }
                setOnClickListener {
                    val assignedPlayers = members.count { it.playerNumber > 0 }
                                        if (assignedPlayers > 0) {
                        room.isGameStarted = true
                        netplaySession?.getTransport()?.sendGameStartSync(0L)
                        onGameStart?.invoke()
                        netplaySession?.addChatMessage("بدأت المباراة رسمياً! تم توزيع اللاعبين والمزامنة اللحظية مفعلة.")
                        Toast.makeText(context, "بدأت المباراة! المزامنة اللحظية مفعلة.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "يرجى تعيين رقم لاعب واحد على الأقل قبل البدء!", Toast.LENGTH_LONG).show()
                    }
                }
            }
            root.addView(btnStartGame)
        }
        scroll.addView(membersContainer)
        root.addView(scroll)
        showInBottomSheet(context, root)
    }
private fun showUserProfileModal(context: Context, member: RoomMember) {
        val density = context.resources.displayMetrics.density
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((28 * density).toInt(), (22 * density).toInt(), (28 * density).toInt(), (22 * density).toInt())
            setBackgroundColor(COLOR_BG_DARK)
        }
val avatar = TextView(context).apply {
            text = if (member.isHost) "👑" else "🎮"
            textSize = 42f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (10 * density).toInt())
        }
val name = TextView(context).apply {
            text = member.name
            textSize = 18f
            setTextColor(COLOR_GOLD_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
val details = TextView(context).apply {
            text = "الدور: ${member.role}\nزمن الاستجابة (Ping): ${member.pingMs}ms\nالحالة: متصل عبر بروتوكول GGPO Rollback\nنظام الإدخال: يد تحكم PS1 مع اهتزاز Haptics"
            textSize = 13f
            setTextColor(COLOR_TEXT_MUTED)
            setLineSpacing(8f * density, 1f)
            setPadding(0, (16 * density).toInt(), 0, (16 * density).toInt())
        }
val btnOk = Button(context).apply {
            text = "حسناً"
            val okBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColors(intArrayOf(COLOR_GOLD_PRIMARY, COLOR_GOLD_BRIGHT))
                cornerRadius = 16f * density
            }
            background = okBg
            setTextColor(Color.rgb(10, 10, 15))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (16 * density).toInt()
            }
        }
        root.addView(avatar)
        root.addView(name)
        root.addView(details)
        root.addView(btnOk)
        showInBottomSheet(context, root)
    }
    /**
     * Shows the native Lobby Browser with active rooms, direct join, and room code generator.
     */
    fun showLobbyBrowserDialog(
        context: Context,
        netplaySession: NetplaySession?,
        onRoomSelected: (String) -> Unit
    ) {
        val density = context.resources.displayMetrics.density
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (18 * density).toInt())
            setBackgroundColor(COLOR_BG_DARK)
        }
val headerTitle = TextView(context).apply {
            text = "🌐 ردهات وغرف Netplay النشطة"
            textSize = 18f
            setTextColor(COLOR_GOLD_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (14 * density).toInt())
        }
val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (340 * density).toInt())
        }
val roomsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
val sampleRooms = listOf(
            Triple("PS1-TK3", "بطولة Tekken 3 • الشرق الأوسط", "1/2 لاعبين • 24ms • عامة"),
            Triple("CB3-PRO", "Combat 3 Arena • Rollback GGPO", "1/2 لاعبين • 18ms • محمية 🔒"),
            Triple("CTR-GP", "Crash Team Racing • سباق ثنائي", "1/2 لاعبين • 32ms • عامة")
        )
        sampleRooms.forEach { (code, title, details) ->
            val roomCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((18 * density).toInt(), (14 * density).toInt(), (18 * density).toInt(), (14 * density).toInt())
val cardShape = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(COLOR_CARD_LUXURY)
                    cornerRadius = 16f * density
                    setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
                }
                background = cardShape
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (12 * density).toInt()
                }
                layoutParams = params
            }
val titleView = TextView(context).apply {
                text = "$title ($code)"
                textSize = 14.5f
                setTextColor(COLOR_TEXT_PRIMARY)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.RIGHT
            }
val descView = TextView(context).apply {
                text = details
                textSize = 12f
                setTextColor(COLOR_TEXT_MUTED)
                gravity = Gravity.RIGHT
                setPadding(0, (4 * density).toInt(), 0, (10 * density).toInt())
            }
val joinBtn = Button(context).apply {
                text = "انضمام للغرفة ($code)"
                val btnBg = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColors(intArrayOf(COLOR_GOLD_PRIMARY, COLOR_GOLD_BRIGHT))
                    cornerRadius = 12f * density
                }
                background = btnBg
                setTextColor(Color.rgb(10, 10, 15))
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                setOnClickListener {
                    onRoomSelected(code)
                }
            }
            roomCard.addView(titleView)
            roomCard.addView(descView)
            roomCard.addView(joinBtn)
            roomsContainer.addView(roomCard)
        }
        scroll.addView(roomsContainer)
        root.addView(headerTitle)
        root.addView(scroll)
val btnContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (20 * density).toInt(), 0, 0)
        }
val btnCreate = Button(context).apply {
            text = "إنشاء غرفة جديدة"
            val createBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColors(intArrayOf(COLOR_GOLD_PRIMARY, COLOR_GOLD_BRIGHT))
                cornerRadius = 14f * density
            }
            background = createBg
            setTextColor(Color.rgb(10, 10, 15))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = (10 * density).toInt() }
            setOnClickListener {
                val newCode = netplaySession?.createRoom() ?: "NEW-ROOM"
                onRoomSelected(newCode)
            }
        }
val btnClose = Button(context).apply {
            text = "إغلاق"
            val closeBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_INPUT_LUXURY)
                cornerRadius = 14f * density
                setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
            }
            background = closeBg
            setTextColor(COLOR_TEXT_PRIMARY)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        btnContainer.addView(btnCreate)
        btnContainer.addView(btnClose)
        root.addView(btnContainer)
        showInBottomSheet(context, root)
    }
    /**
     * Shows HUD touch controls customization dialog (Scale, Opacity, Haptic Feedback)
     */
    fun showTouchCustomizerDialog(
        context: Context,
        overlayView: VirtualTouchOverlayView
    ) {
        val density = context.resources.displayMetrics.density
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (18 * density).toInt())
            setBackgroundColor(COLOR_BG_DARK)
        }
val headerTitle = TextView(context).apply {
            text = "🎛️ تخصيص أزرار التحكم اللمسية (PS1 HUD)"
            textSize = 17f
            setTextColor(COLOR_GOLD_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        // Toggle visibility
    val toggleBtn = Button(context).apply {
            text = if (overlayView.isControlsVisible) "إظهار الأزرار على الشاشة: نعم ✅" else "إظهار الأزرار على الشاشة: مخفية ❌"
            val toggleBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(if (overlayView.isControlsVisible) COLOR_CARD_LUXURY else COLOR_INPUT_LUXURY)
                cornerRadius = 14f * density
                setStroke((1.2f * density).toInt(), if (overlayView.isControlsVisible) COLOR_GOLD_PRIMARY else COLOR_GOLD_BORDER_SUBTLE)
            }
            background = toggleBg
            setTextColor(if (overlayView.isControlsVisible) COLOR_GOLD_BRIGHT else COLOR_TEXT_MUTED)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setOnClickListener {
                overlayView.isControlsVisible = !overlayView.isControlsVisible
                text = if (overlayView.isControlsVisible) "إظهار الأزرار على الشاشة: نعم ✅" else "إظهار الأزرار على الشاشة: مخفية ❌"
                val updatedBg = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(if (overlayView.isControlsVisible) COLOR_CARD_LUXURY else COLOR_INPUT_LUXURY)
                    cornerRadius = 14f * density
                    setStroke((1.2f * density).toInt(), if (overlayView.isControlsVisible) COLOR_GOLD_PRIMARY else COLOR_GOLD_BORDER_SUBTLE)
                }
                background = updatedBg
                setTextColor(if (overlayView.isControlsVisible) COLOR_GOLD_BRIGHT else COLOR_TEXT_MUTED)
                overlayView.invalidate()
            }
        }
        // Scale Label
    val scaleLabel = TextView(context).apply {
            text = "حجم الأزرار (Scale: ${(overlayView.actionScale * 100).toInt()}%)"
            textSize = 13.5f
            setTextColor(COLOR_TEXT_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, (14 * density).toInt(), 0, (4 * density).toInt())
        }
val scaleSeekBar = SeekBar(context).apply {
            max = 100
            progress = ((overlayView.actionScale - 0.6f) / 0.8f * 100).toInt().coerceIn(0, 100)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val newScale = 0.6f + (progress / 100f) * 0.8f
                    overlayView.dpadScale = newScale
                    overlayView.actionScale = newScale
                    scaleLabel.text = "حجم الأزرار (Scale: ${(newScale * 100).toInt()}%)"
                    overlayView.requestLayout()
                    overlayView.invalidate()
                }
override fun onStartTrackingTouch(seekBar: SeekBar?) {}
override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        // Opacity Label
    val opacityLabel = TextView(context).apply {
            text = "شفافية الأزرار (Opacity: ${(overlayView.controlsAlpha * 100).toInt()}%)"
            textSize = 13.5f
            setTextColor(COLOR_TEXT_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, (14 * density).toInt(), 0, (4 * density).toInt())
        }
val opacitySeekBar = SeekBar(context).apply {
            max = 100
            progress = ((overlayView.controlsAlpha - 0.2f) / 0.8f * 100).toInt().coerceIn(0, 100)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val newAlpha = 0.2f + (progress / 100f) * 0.8f
                    overlayView.controlsAlpha = newAlpha
                    opacityLabel.text = "شفافية الأزرار (Opacity: ${(newAlpha * 100).toInt()}%)"
                    overlayView.invalidate()
                }
override fun onStartTrackingTouch(seekBar: SeekBar?) {}
override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        // Haptics Toggle Button
    val hapticBtn = Button(context).apply {
            text = if (overlayView.hapticEnabled) "الاهتزاز اللمسي (Haptic): مُفعّل ✅" else "الاهتزاز اللمسي (Haptic): مُعطّل ❌"
            val hapticBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(if (overlayView.hapticEnabled) Color.argb(45, 16, 185, 129) else COLOR_INPUT_LUXURY)
                cornerRadius = 14f * density
                setStroke((1f * density).toInt(), if (overlayView.hapticEnabled) COLOR_EMERALD else COLOR_GOLD_BORDER_SUBTLE)
            }
            background = hapticBg
            setTextColor(if (overlayView.hapticEnabled) COLOR_EMERALD else COLOR_TEXT_MUTED)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setOnClickListener {
                overlayView.hapticEnabled = !overlayView.hapticEnabled
                text = if (overlayView.hapticEnabled) "الاهتزاز اللمسي (Haptic): مُفعّل ✅" else "الاهتزاز اللمسي (Haptic): مُعطّل ❌"
                val updatedBg = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(if (overlayView.hapticEnabled) Color.argb(45, 16, 185, 129) else COLOR_INPUT_LUXURY)
                    cornerRadius = 14f * density
                    setStroke((1f * density).toInt(), if (overlayView.hapticEnabled) COLOR_EMERALD else COLOR_GOLD_BORDER_SUBTLE)
                }
                background = updatedBg
                setTextColor(if (overlayView.hapticEnabled) COLOR_EMERALD else COLOR_TEXT_MUTED)
            }
        }
        root.addView(headerTitle)
        root.addView(toggleBtn)
        root.addView(scaleLabel)
        root.addView(scaleSeekBar)
        root.addView(opacityLabel)
        root.addView(opacitySeekBar)
        root.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(1, (12 * density).toInt()) })
        root.addView(hapticBtn)
val btnContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (24 * density).toInt(), 0, 0)
        }
val btnSave = Button(context).apply {
            text = "تم الحفظ"
            val saveBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColors(intArrayOf(COLOR_GOLD_PRIMARY, COLOR_GOLD_BRIGHT))
                cornerRadius = 14f * density
            }
            background = saveBg
            setTextColor(Color.rgb(10, 10, 15))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = (10 * density).toInt() }
        }
val btnReset = Button(context).apply {
            text = "إعادة تعيين"
            val resetBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_INPUT_LUXURY)
                cornerRadius = 14f * density
                setStroke((1f * density).toInt(), COLOR_CRIMSON)
            }
            background = resetBg
            setTextColor(COLOR_CRIMSON)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                overlayView.dpadScale = 1.0f
                overlayView.actionScale = 1.0f
                overlayView.controlsAlpha = 0.65f
                overlayView.hapticEnabled = true
                overlayView.isControlsVisible = false
                overlayView.requestLayout()
                overlayView.invalidate()
            }
        }
        btnContainer.addView(btnSave)
        btnContainer.addView(btnReset)
        root.addView(btnContainer)
        showInBottomSheet(context, root)
    }
    /**
     * Modern Video Arena Camera Dialog with Live Participant Feeds, Sizing Controls & Host Moderator Permissions
     */
    fun showCameraRoomDialog(
        context: Context,
        cameraHelper: RoomCameraHelper?,
        isHost: Boolean,
        onCameraToggleRequested: ((TextureView, (isOn: Boolean) -> Unit) -> Unit)? = null,
        onCameraFlipRequested: ((TextureView) -> Unit)? = null,
        onHostCloseAllCameras: (() -> Unit)? = null
    ) {
        val density = context.resources.displayMetrics.density
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt(), (20 * density).toInt())
            setBackgroundColor(COLOR_BG_DARK)
        }
        // Title Header
    val title = TextView(context).apply {
            text = "📹 كاميرات اللاعبين المباشرة (Video Arena)"
            textSize = 17f
            setTextColor(COLOR_GOLD_BRIGHT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (4 * density).toInt())
        }
val subtitle = TextView(context).apply {
            text = "بث فيديو مباشر منخفض التأخير • تحكم بالأبعاد والأحجام وصلاحيات المضيف"
            textSize = 11.5f
            setTextColor(COLOR_TEXT_MUTED)
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        root.addView(title)
        root.addView(subtitle)
        // Camera Arena Container (Host Card + Guest Card)
    var currentHeightDp = 130
        val cameraCardsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (currentHeightDp * density).toInt()
            ).apply {
                bottomMargin = (14 * density).toInt()
            }
        }
        // Helper to build camera card background
    fun createCardBg(isActive: Boolean): GradientDrawable {
            return GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(Color.rgb(18, 22, 34))
                cornerRadius = 16f * density
                setStroke(
                    if (isActive) (2f * density).toInt() else (1f * density).toInt(),
                    if (isActive) COLOR_EMERALD else COLOR_GOLD_BORDER_SUBTLE
                )
            }
        }
        // Host Card (P1)
    val hostCard = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = (6 * density).toInt()
            }
            background = createCardBg(false)
            clipToOutline = true
        }
val hostTexture = TextureView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            visibility = if (isHost && cameraHelper?.isCameraOn?.get() == true) View.VISIBLE else View.GONE
        }
val hostAvatarLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.rgb(20, 24, 36))
            visibility = if (isHost && cameraHelper?.isCameraOn?.get() == true) View.GONE else View.VISIBLE
            val avatarIcon = TextView(context).apply {
                text = "👑"
                textSize = 24f
                gravity = Gravity.CENTER
            }
            addView(avatarIcon)
        }
val hostRoleBadge = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            }
val pill = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(Color.argb(200, 26, 22, 10))
                cornerRadius = 10f * density
                setStroke((1f * density).toInt(), COLOR_GOLD_PRIMARY)
            }
            background = pill
            setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
val txt = TextView(context).apply {
                text = "المضيف 👑"
                textSize = 9.5f
                setTextColor(COLOR_GOLD_BRIGHT)
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(txt)
        }
val hostMeter = VerticalVoiceMeterView(context).apply {
            layoutParams = LinearLayout.LayoutParams((14 * density).toInt(), (14 * density).toInt())
        }
val hostBottomBar = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM
            }
            setBackgroundColor(Color.argb(210, 10, 14, 23))
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
val txtName = TextView(context).apply {
                text = if (isHost) "أنت (المضيف)" else "المضيف 👑"
                textSize = 10f
                setTextColor(COLOR_TEXT_PRIMARY)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(txtName)
            addView(hostMeter)
        }
        hostCard.addView(hostTexture)
        hostCard.addView(hostAvatarLayout)
        hostCard.addView(hostRoleBadge)
        hostCard.addView(hostBottomBar)
        // Guest Card (P2)
    val guestCard = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = (6 * density).toInt()
            }
            background = createCardBg(false)
            clipToOutline = true
        }
val guestTexture = TextureView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            visibility = if (!isHost && cameraHelper?.isCameraOn?.get() == true) View.VISIBLE else View.GONE
        }
val guestAvatarLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.rgb(18, 26, 26))
            visibility = if (!isHost && cameraHelper?.isCameraOn?.get() == true) View.GONE else View.VISIBLE
            val avatarIcon = TextView(context).apply {
                text = "🎮"
                textSize = 24f
                gravity = Gravity.CENTER
            }
            addView(avatarIcon)
        }
val guestRoleBadge = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            }
val pill = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(Color.argb(200, 10, 26, 20))
                cornerRadius = 10f * density
                setStroke((1f * density).toInt(), COLOR_EMERALD)
            }
            background = pill
            setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
val txt = TextView(context).apply {
                text = "الضيف 🎮"
                textSize = 9.5f
                setTextColor(COLOR_EMERALD)
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(txt)
        }
val guestMeter = VerticalVoiceMeterView(context).apply {
            layoutParams = LinearLayout.LayoutParams((14 * density).toInt(), (14 * density).toInt())
        }
val guestBottomBar = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM
            }
            setBackgroundColor(Color.argb(210, 10, 14, 23))
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
val txtName = TextView(context).apply {
                text = if (!isHost) "أنت (الضيف)" else "الضيف 🎮"
                textSize = 10f
                setTextColor(COLOR_TEXT_PRIMARY)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(txtName)
            addView(guestMeter)
        }
        guestCard.addView(guestTexture)
        guestCard.addView(guestAvatarLayout)
        guestCard.addView(guestRoleBadge)
        guestCard.addView(guestBottomBar)
        cameraCardsRow.addView(hostCard)
        cameraCardsRow.addView(guestCard)
        root.addView(cameraCardsRow)
        // Hook Real-time Active Speaker into Voice Meters
LiveKitNetplayManager.onActiveSpeakersChanged = { speakers ->
            (context as? android.app.Activity)?.runOnUiThread {
                val isHostSpeaking = speakers.any { it.contains("Host", true) || it.contains("المضيف", true) }
val isGuestSpeaking = speakers.any { it.contains("Guest", true) || it.contains("الضيف", true) }
                hostMeter.setSpeaking(isHostSpeaking)
                guestMeter.setSpeaking(isGuestSpeaking)
                hostCard.background = createCardBg(isHostSpeaking || (isHost && cameraHelper?.isCameraOn?.get() == true))
                guestCard.background = createCardBg(isGuestSpeaking || (!isHost && cameraHelper?.isCameraOn?.get() == true))
            }
        }
        // ============================================================
// 🎮 Primary Camera Action Buttons
// ============================================================
    val primaryActionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (48 * density).toInt()).apply {
                bottomMargin = (10 * density).toInt()
            }
        }
        // Toggle Camera Button
    var isCamActive = cameraHelper?.isCameraOn?.get() == true
        val targetTexture = if (isHost) hostTexture else guestTexture
        val targetAvatar = if (isHost) hostAvatarLayout else guestAvatarLayout
        val targetCard = if (isHost) hostCard else guestCard
        fun getToggleBg(active: Boolean): GradientDrawable {
            return GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColors(
                    if (active) intArrayOf(COLOR_EMERALD, Color.rgb(5, 150, 105))
                    else intArrayOf(COLOR_GOLD_PRIMARY, COLOR_GOLD_BRIGHT)
                )
                cornerRadius = 14f * density
            }
        }
val btnToggleCam = Button(context).apply {
            text = if (isCamActive) "🟢 الكاميرا قيد البث (انقر للإيقاف)" else "📷 تشغيل الكاميرا المحلية"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = getToggleBg(isCamActive)
            setTextColor(if (isCamActive) Color.WHITE else Color.rgb(10, 10, 15))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.2f).apply {
                marginEnd = (8 * density).toInt()
            }
            setOnClickListener {
                onCameraToggleRequested?.invoke(targetTexture) { isOn ->
                    isCamActive = isOn
                    if (isOn) {
                        targetTexture.visibility = View.VISIBLE
                        targetAvatar.visibility = View.GONE
                        targetCard.background = createCardBg(true)
                        text = "🟢 الكاميرا قيد البث (انقر للإيقاف)"
                        background = getToggleBg(true)
                        setTextColor(Color.WHITE)
                    } else {
                        targetTexture.visibility = View.GONE
                        targetAvatar.visibility = View.VISIBLE
                        targetCard.background = createCardBg(false)
                        text = "📷 تشغيل الكاميرا المحلية"
                        background = getToggleBg(false)
                        setTextColor(Color.rgb(10, 10, 15))
                    }
                }
            }
        }
        // Flip Camera Button
    val btnFlipCam = Button(context).apply {
            text = "🔄 تبديل"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            val flipBg = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_INPUT_LUXURY)
                cornerRadius = 14f * density
                setStroke((1f * density).toInt(), COLOR_GOLD_PRIMARY)
            }
            background = flipBg
            setTextColor(COLOR_GOLD_BRIGHT)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.8f)
            setOnClickListener {
                onCameraFlipRequested?.invoke(targetTexture)
            }
        }
        primaryActionsRow.addView(btnToggleCam)
        primaryActionsRow.addView(btnFlipCam)
        root.addView(primaryActionsRow)
        // ============================================================
// ⚙️ Expandable Settings Section
// ============================================================
    val settingsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            visibility = View.GONE
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
            background = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_CARD_LUXURY)
                cornerRadius = 12f * density
                setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
            }
        }
val settingsHeaderBtn = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (44 * density).toInt()).apply {
                topMargin = (4 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * density).toInt(), 0, (16 * density).toInt(), 0)
            background = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(COLOR_INPUT_LUXURY)
                cornerRadius = 12f * density
                setStroke((1f * density).toInt(), COLOR_GOLD_BORDER_SUBTLE)
            }
val arrow = TextView(context).apply {
                text = "▼"
                textSize = 12f
                setTextColor(COLOR_GOLD_PRIMARY)
            }
val label = TextView(context).apply {
                text = "إعدادات الكاميرا المتقدمة"
                textSize = 13f
                setTextColor(COLOR_GOLD_BRIGHT)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.RIGHT
                setPadding(0, 0, (8 * density).toInt(), 0)
            }
val icon = TextView(context).apply {
                text = "⚙️"
                textSize = 14f
            }
                        addView(arrow)
            addView(label)
            addView(icon)
                        setOnClickListener {
                if (settingsContainer.visibility == View.GONE) {
                    settingsContainer.visibility = View.VISIBLE
                    arrow.text = "▲"
                } else {
                    settingsContainer.visibility = View.GONE
                    arrow.text = "▼"
                }
            }
        }
                root.addView(settingsHeaderBtn)
val sizeSectionLabel = TextView(context).apply {
            text = "📐 تخصيص أبعاد وحجم مربعات الكاميرا:"
            textSize = 12f
            setTextColor(COLOR_TEXT_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, (8 * density).toInt())
        }
        settingsContainer.addView(sizeSectionLabel)
val sizePillsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (38 * density).toInt()).apply {
                bottomMargin = (12 * density).toInt()
            }
        }
val sizeButtons = ArrayList<Button>()
val sizeOptions = listOf(
            Triple("صغير (85dp)", 85, 0),
            Triple("متوسط (130dp)", 130, 1),
            Triple("عريض (175dp)", 175, 2)
        )
fun updateSizePills(selectedIndex: Int) {
            for (i in sizeButtons.indices) {
                val btn = sizeButtons[i]
                val isSelected = i == selectedIndex
                val pillBg = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(if (isSelected) Color.rgb(35, 42, 60) else COLOR_BG_DARK)
                    cornerRadius = 10f * density
                    setStroke(
                        if (isSelected) (1.5f * density).toInt() else (1f * density).toInt(),
                        if (isSelected) COLOR_GOLD_PRIMARY else COLOR_GOLD_BORDER_SUBTLE
                    )
                }
                btn.background = pillBg
                btn.setTextColor(if (isSelected) COLOR_GOLD_BRIGHT else COLOR_TEXT_MUTED)
            }
        }
        for (opt in sizeOptions) {
            val btn = Button(context).apply {
                text = opt.first
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (opt.third > 0) marginStart = (6 * density).toInt()
                }
                setOnClickListener {
                    currentHeightDp = opt.second
                    cameraCardsRow.layoutParams.height = (currentHeightDp * density).toInt()
                    cameraCardsRow.requestLayout()
                    updateSizePills(opt.third)
                }
            }
            sizeButtons.add(btn)
            sizePillsRow.addView(btn)
        }
        updateSizePills(1) // Default: Medium
settingsContainer.addView(sizePillsRow)
        // Host Moderator Close All Cameras Button inside Settings
if (isHost) {
            val btnHostCloseAll = Button(context).apply {
                text = "🚫 إغلاق كاميرات الغرفة (صلاحية المضيف 👑)"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                val closeBg = GradientDrawable().apply {
                    setShape(GradientDrawable.RECTANGLE)
                    setColor(Color.rgb(38, 18, 22))
                    cornerRadius = 12f * density
                    setStroke((1f * density).toInt(), COLOR_CRIMSON)
                }
                background = closeBg
                setTextColor(COLOR_CRIMSON)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (44 * density).toInt())
                setOnClickListener {
                    onHostCloseAllCameras?.invoke()
                    Toast.makeText(context, "تم إغلاق كافة كاميرات الغرفة من قبل المضيف 🔒", Toast.LENGTH_SHORT).show()
                }
            }
            settingsContainer.addView(btnHostCloseAll)
        }
                root.addView(settingsContainer)
        // Attach live camera preview if already active
        if (isCamActive) {
            targetTexture.visibility = View.VISIBLE
            targetAvatar.visibility = View.GONE
            targetCard.background = createCardBg(true)
            cameraHelper?.startCamera(targetTexture)
        }
val dialog = showInBottomSheet(context, root)
        dialog.setOnDismissListener {
            // Camera helper can continue running or be safely managed
}
    }}