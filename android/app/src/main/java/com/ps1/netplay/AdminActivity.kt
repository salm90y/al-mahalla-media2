package com.ps1.netplay

import android.widget.*
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ps1.netplay.network.CloudflareClient

class AdminActivity : AppCompatActivity() {
    private val density by lazy { resources.displayMetrics.density }
private val COLOR_NEON_CYAN = Color.parseColor("#00E5FF")
private val COLOR_TEXT_PRIMARY = Color.parseColor("#FFFFFF")
private val COLOR_TEXT_SECONDARY = Color.parseColor("#94A3B8")
private lateinit var accountsContainer: LinearLayout
    private var allUsersList = mutableListOf<UserProfile>()
private var searchQuery = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
val currentUser = UserManager.getCurrentUser(this)
        if (currentUser == null || (!currentUser.isAdmin && currentUser.role != "مشرف" && currentUser.role != "admin")) {
            Toast.makeText(this, "هذه الصفحة مخصصة لمدير النظام فقط", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_premium_geometric)
        }
        // ================= HEADER =================
    val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((20 * density).toInt(), (36 * density).toInt(), (20 * density).toInt(), (14 * density).toInt())
        }
val backBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_back_arrow)
            setColorFilter(COLOR_NEON_CYAN)
            layoutParams = LinearLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt()).apply {
                marginEnd = (14 * density).toInt()
            }
            setOnClickListener { finish() }
        }
        header.addView(backBtn)
val logoText = TextView(this).apply {
            text = "المَحَلَّة"
            textSize = 26f
            setTextColor(COLOR_NEON_CYAN)
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(15f, 0f, 0f, COLOR_NEON_CYAN)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(logoText)
        root.addView(header)
        // ================= SEARCH BAR =================
    val searchContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(Color.parseColor("#4D1E293B"))
                cornerRadius = 24f * density
                setStroke((1 * density).toInt(), Color.parseColor("#3338BDF8"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * density).toInt()
            ).apply {
                setMargins((20 * density).toInt(), 0, (20 * density).toInt(), (14 * density).toInt())
            }
            setPadding((16 * density).toInt(), 0, (16 * density).toInt(), 0)
        }
val searchIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_search)
            layoutParams = LinearLayout.LayoutParams((20 * density).toInt(), (20 * density).toInt()).apply {
                marginEnd = (10 * density).toInt()
            }
            setColorFilter(COLOR_NEON_CYAN)
        }
        searchContainer.addView(searchIcon)
val searchInput = EditText(this).apply {
            hint = "بحث عن مستخدم..."
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            textSize = 14f
            background = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString()?.trim() ?: ""
                    renderAccounts()
                }
override fun afterTextChanged(s: Editable?) {}
            })
        }
        searchContainer.addView(searchInput)
        root.addView(searchContainer)
        // ================= SCROLL CONTENT =================
    val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), 0, (20 * density).toInt(), (32 * density).toInt())
        }
        // ================= ACCOUNTS DASHBOARD HEADER + ELEGANT CREATE BUTTON =================
    val sectionHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (18 * density).toInt()
            }
        }
val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
val titleText = TextView(this).apply {
            text = "إدارة الحسابات"
            textSize = 19f
            setTextColor(Color.parseColor("#A5F3FC"))
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(8f, 0f, 0f, Color.parseColor("#A5F3FC"))
        }
val subtitleText = TextView(this).apply {
            text = "لوحة التحكم"
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
        }
        titleCol.addView(titleText)
        titleCol.addView(subtitleText)
        sectionHeaderRow.addView(titleCol)
        // زر صغير أنيق لإنشاء حساب جديد يفتح نافذة منبثقة مخصصة
    val btnOpenCreateDialog = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2600E5FF"))
                cornerRadius = 20f * density
                setStroke((1 * density).toInt(), COLOR_NEON_CYAN)
            }
            setPadding((14 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
            setOnClickListener {
                showCreateAccountDialog()
            }
        }
val plusIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_plus_circle_neon)
            setColorFilter(COLOR_NEON_CYAN)
            layoutParams = LinearLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt()).apply {
                marginEnd = (6 * density).toInt()
            }
        }
val plusText = TextView(this).apply {
            text = "+ حساب جديد"
            textSize = 13f
            setTextColor(COLOR_NEON_CYAN)
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(8f, 0f, 0f, COLOR_NEON_CYAN)
        }
        btnOpenCreateDialog.addView(plusIcon)
        btnOpenCreateDialog.addView(plusText)
        sectionHeaderRow.addView(btnOpenCreateDialog)
        content.addView(sectionHeaderRow)
        // Container where account grid cards are dynamically rendered
accountsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        content.addView(accountsContainer)
        scrollView.addView(content)
        root.addView(scrollView)
        setContentView(root)
        loadAccounts()
    }
private fun loadAccounts() {
        CloudflareClient.getAllUsers(this) { success, list ->
            if (success && list != null) {
                allUsersList = list.toMutableList()
            } else {
                allUsersList = UserManager.getAllUsers(this@AdminActivity).toMutableList()
            }
            renderAccounts()
        }
    }
private fun renderAccounts() {
        accountsContainer.removeAllViews()
val filtered = if (searchQuery.isEmpty()) {
            allUsersList
        } else {
            allUsersList.filter {
                it.username.contains(searchQuery, ignoreCase = true) ||
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true)
            }
        }
        if (filtered.isEmpty()) {
            val emptyMsg = TextView(this).apply {
                text = if (searchQuery.isEmpty()) "لا يوجد حسابات حالياً" else "لم يتم العثور على نتائج للبحث"
                textSize = 14f
                setTextColor(COLOR_TEXT_SECONDARY)
                gravity = Gravity.CENTER
                setPadding(0, (40 * density).toInt(), 0, (40 * density).toInt())
            }
            accountsContainer.addView(emptyMsg)
            return
        }
        // Display in rows of 3 columns
    var currentRow: LinearLayout? = null
        for (i in filtered.indices) {
            if (i % 3 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = (12 * density).toInt()
                    }
                }
                accountsContainer.addView(currentRow)
            }
val user = filtered[i]
            val isLastInRow = (i % 3 == 2) || (i == filtered.size - 1)
            currentRow?.addView(createAccountCard(user, isLastInRow))
        }
    }
private fun createAccountCard(user: UserProfile, isLast: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#331E293B"))
                cornerRadius = 16f * density
                setStroke((1 * density).toInt(), Color.parseColor("#1A38BDF8"))
            }
            setPadding((6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (!isLast) marginEnd = (10 * density).toInt()
            }
        }
        // Avatar
    val avatar = TextView(this).apply {
            text = user.avatar.ifEmpty { "👤" }
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#331E293B"))
                setStroke((1 * density).toInt(), Color.parseColor("#8000E5FF"))
            }
            setShadowLayer(12f, 0f, 0f, Color.parseColor("#6600E5FF"))
            layoutParams = LinearLayout.LayoutParams((46 * density).toInt(), (46 * density).toInt()).apply {
                bottomMargin = (8 * density).toInt()
            }
        }
        card.addView(avatar)
        // Name
    val nameText = TextView(this).apply {
            text = user.fullName.ifEmpty { user.username }
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
        }
        card.addView(nameText)
        // Role & Username
    val roleText = TextView(this).apply {
            text = "${user.role} (@${user.username})"
            textSize = 10f
            setTextColor(Color.parseColor("#8067E8F9"))
            gravity = Gravity.CENTER
            setPadding(0, (2 * density).toInt(), 0, (8 * density).toInt())
            maxLines = 1
        }
        card.addView(roleText)
        // Action Buttons Row (Edit | Delete)
    val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
val editBtn = TextView(this).apply {
            text = "تعديل"
            textSize = 10f
            setTextColor(Color.parseColor("#67E8F9"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3300E5FF"))
                cornerRadius = 14f * density
                setStroke((1 * density).toInt(), Color.parseColor("#4D22D3EE"))
            }
            setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (4 * density).toInt()
            }
            gravity = Gravity.CENTER
            setOnClickListener {
                showUpdateAccountDialog(user)
            }
        }
        btnRow.addView(editBtn)
val deleteBtn = TextView(this).apply {
            text = "حذف"
            textSize = 10f
            setTextColor(Color.parseColor("#FCA5A5"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33EF4444"))
                cornerRadius = 14f * density
                setStroke((1 * density).toInt(), Color.parseColor("#4DF87171"))
            }
            setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            setOnClickListener {
                CloudflareClient.deleteUser(this@AdminActivity, user.username) { success ->
                    if (success) {
                        Toast.makeText(this@AdminActivity, "تم حذف حساب ${user.username}", Toast.LENGTH_SHORT).show()
                        loadAccounts()
                    } else {
                        Toast.makeText(this@AdminActivity, "فشل حذف الحساب", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        btnRow.addView(deleteBtn)
        card.addView(btnRow)
        return card
    }
    /**
     * نافذة منبثقة أنيقة ومستقلة لحقول إنشاء الحساب وحفظه في السيرفر وقاعدة البيانات
     */
    private fun showCreateAccountDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
val dialogScroll = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
val dialogRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F20F172A")) // Solid Dark Slate #0F172A
cornerRadius = 24f * density
                setStroke((2 * density).toInt(), COLOR_NEON_CYAN)
            }
            setPadding((22 * density).toInt(), (22 * density).toInt(), (22 * density).toInt(), (22 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        // Dialog Header
    val dialogHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * density).toInt()
            }
        }
val dialogTitle = TextView(this).apply {
            text = "إنشاء حساب جديد"
            textSize = 18f
            setTextColor(Color.parseColor("#A5F3FC"))
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(8f, 0f, 0f, COLOR_NEON_CYAN)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        dialogHeader.addView(dialogTitle)
val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(COLOR_TEXT_SECONDARY)
            setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            setOnClickListener { dialog.dismiss() }
        }
        dialogHeader.addView(closeBtn)
        dialogRoot.addView(dialogHeader)
        // Avatar selector
    var selectedAvatar = "🧔🏻"
        val avatars = listOf("🧔🏻", "👩🏻", "👥", "🧕🏻", "🤓", "👩🏻‍🏫", "🎮", "⚡")
val avatarRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * density).toInt()
            }
        }
val avatarViews = mutableListOf<TextView>()
        for (emoji in avatars) {
            val tv = TextView(this).apply {
                text = emoji
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (emoji == selectedAvatar) Color.parseColor("#4D00E5FF") else Color.TRANSPARENT)
                    if (emoji == selectedAvatar) setStroke((2 * density).toInt(), COLOR_NEON_CYAN)
                }
                layoutParams = LinearLayout.LayoutParams((38 * density).toInt(), (38 * density).toInt()).apply {
                    marginEnd = (4 * density).toInt()
                }
                setOnClickListener {
                    selectedAvatar = emoji
                    for (v in avatarViews) {
                        val isSel = v.text == emoji
                        v.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (isSel) Color.parseColor("#4D00E5FF") else Color.TRANSPARENT)
                            if (isSel) setStroke((2 * density).toInt(), COLOR_NEON_CYAN)
                        }
                    }
                }
            }
            avatarViews.add(tv)
            avatarRow.addView(tv)
        }
        dialogRoot.addView(avatarRow)
        // Input fields inside Dialog
    fun createDialogInput(hintText: String, iconRes: Int, isPassword: Boolean = false): Pair<LinearLayout, EditText> {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#331E293B"))
                    cornerRadius = 20f * density
                    setStroke((1 * density).toInt(), Color.parseColor("#3338BDF8"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (48 * density).toInt()).apply {
                    bottomMargin = (10 * density).toInt()
                }
                setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
            }
val icon = ImageView(this).apply {
                setImageResource(iconRes)
                setColorFilter(COLOR_NEON_CYAN)
                layoutParams = LinearLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt()).apply {
                    marginEnd = (10 * density).toInt()
                }
            }
            container.addView(icon)
val editText = EditText(this).apply {
                hint = hintText
                setHintTextColor(Color.parseColor("#64748B"))
                setTextColor(Color.WHITE)
                textSize = 13f
                background = null
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                if (isPassword) {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }
            container.addView(editText)
            return Pair(container, editText)
        }
val (nameBox, inputName) = createDialogInput("الاسم الكامل", R.drawable.ic_user_neon)
        dialogRoot.addView(nameBox)
val (userBox, inputUser) = createDialogInput("اسم المستخدم (اليوزر)", R.drawable.ic_at_neon)
        dialogRoot.addView(userBox)
val (emailBox, inputEmail) = createDialogInput("البريد الإلكتروني (اختياري)", R.drawable.ic_mail_neon)
        dialogRoot.addView(emailBox)
val (phoneBox, inputPhone) = createDialogInput("رقم الهاتف (+964)", R.drawable.ic_phone_neon)
        inputPhone.setText("+964 ")
        dialogRoot.addView(phoneBox)
val (passBox, inputPass) = createDialogInput("كلمة المرور", R.drawable.ic_lock_neon, true)
        dialogRoot.addView(passBox)
val (confirmBox, inputConfirm) = createDialogInput("تأكيد كلمة المرور", R.drawable.ic_lock_neon, true)
        dialogRoot.addView(confirmBox)
        // Progress indicator
    val progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (8 * density).toInt()
            }
        }
        dialogRoot.addView(progressBar)
        // Action Buttons: Cancel and Save
    val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (6 * density).toInt()
            }
        }
val btnCancel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#331E293B"))
                cornerRadius = 20f * density
                setStroke((1 * density).toInt(), Color.parseColor("#4DFFFFFF"))
            }
            layoutParams = LinearLayout.LayoutParams(0, (46 * density).toInt(), 1f).apply {
                marginEnd = (10 * density).toInt()
            }
            setOnClickListener { dialog.dismiss() }
        }
        btnCancel.addView(TextView(this).apply {
            text = "إلغاء"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        actionRow.addView(btnCancel)
val btnSave = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(COLOR_NEON_CYAN)
                cornerRadius = 20f * density
            }
            layoutParams = LinearLayout.LayoutParams(0, (46 * density).toInt(), 1.5f)
        }
val btnSaveText = TextView(this).apply {
            text = "حفظ الحساب"
            textSize = 14f
            setTextColor(Color.parseColor("#FFFCF8"))
            typeface = Typeface.DEFAULT_BOLD
        }
        btnSave.addView(btnSaveText)
        btnSave.setOnClickListener {
            val name = inputName.text.toString().trim()
val username = inputUser.text.toString().trim().lowercase()
val email = inputEmail.text.toString().trim()
val password = inputPass.text.toString().trim()
val confirm = inputConfirm.text.toString().trim()
            if (username.length < 3) {
                Toast.makeText(this, "اسم المستخدم يجب أن يتكون من 3 أحرف على الأقل", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 4) {
                Toast.makeText(this, "كلمة المرور يجب أن تتكون من 4 أحرف/أرقام على الأقل", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirm) {
                Toast.makeText(this, "كلمة المرور غير متطابقة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false
            // الحفظ الحقيقي في Cloudflare D1
CloudflareClient.adminCreateUser(this, username, password, email.ifEmpty { null }, selectedAvatar) { success, errorMsg ->
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                if (success) {
                    loadAccounts()
                    Toast.makeText(this, "تم حفظ الحساب بنجاح في قاعدة البيانات!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, errorMsg ?: "فشل إنشاء الحساب", Toast.LENGTH_SHORT).show()
                }
            }
        }
        actionRow.addView(btnSave)
        dialogRoot.addView(actionRow)
        dialogScroll.addView(dialogRoot)
        dialog.setContentView(dialogScroll)
        dialog.show()
        // Size dialog width appropriately
dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
private fun showUpdateAccountDialog(user: UserProfile) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
val dialogScroll = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
val dialogRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F20F172A"))
                 cornerRadius = 24f * density
                setStroke((2 * density).toInt(), COLOR_NEON_CYAN)
            }
            setPadding((22 * density).toInt(), (22 * density).toInt(), (22 * density).toInt(), (22 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
val dialogHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * density).toInt()
            }
        }
val dialogTitle = TextView(this).apply {
            text = "تعديل حساب ${user.username}"
            textSize = 18f
            setTextColor(Color.parseColor("#A5F3FC"))
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(8f, 0f, 0f, COLOR_NEON_CYAN)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        dialogHeader.addView(dialogTitle)
val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(COLOR_TEXT_SECONDARY)
            setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            setOnClickListener { dialog.dismiss() }
        }
        dialogHeader.addView(closeBtn)
        dialogRoot.addView(dialogHeader)
var selectedAvatar = user.avatar
        if (selectedAvatar.isEmpty()) selectedAvatar = "🧔🏻"
        val avatars = listOf("🧔🏻", "👩🏻", "👥", "🧕🏻", "🤓", "👩🏻‍🏫", "🎮", "⚡")
val avatarRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (20 * density).toInt()
            }
        }
val avatarViews = mutableListOf<TextView>()
        for (emoji in avatars) {
            val tv = TextView(this).apply {
                text = emoji
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (emoji == selectedAvatar) Color.parseColor("#4D00E5FF") else Color.TRANSPARENT)
                    if (emoji == selectedAvatar) setStroke((2 * density).toInt(), COLOR_NEON_CYAN)
                }
                                setOnClickListener {
                    selectedAvatar = emoji
                    for (v in avatarViews) {
                        val isSel = v.text == emoji
                        v.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (isSel) Color.parseColor("#4D00E5FF") else Color.TRANSPARENT)
                            if (isSel) setStroke((2 * density).toInt(), COLOR_NEON_CYAN)
                        }
                    }
                }
            }
            avatarViews.add(tv)
            avatarRow.addView(tv)
        }
        dialogRoot.addView(avatarRow)
val inputEmail = EditText(this).apply {
            hint = "البريد الإلكتروني (اختياري)"
            setText(user.email)
            setHintTextColor(COLOR_TEXT_SECONDARY)
            setTextColor(Color.WHITE)
            textSize = 15f
            background = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(Color.parseColor("#331E293B"))
                cornerRadius = 12f * density
                setStroke((1 * density).toInt(), Color.parseColor("#3338BDF8"))
            }
            setPadding((12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }
        dialogRoot.addView(inputEmail)
val inputPhone = EditText(this).apply {
            hint = "رقم الهاتف (+964)"
            setText("+964 ")
            setHintTextColor(COLOR_TEXT_SECONDARY)
            setTextColor(Color.WHITE)
            textSize = 15f
            background = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(Color.parseColor("#331E293B"))
                cornerRadius = 12f * density
                setStroke((1 * density).toInt(), Color.parseColor("#3338BDF8"))
            }
            setPadding((12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }
        dialogRoot.addView(inputPhone)
val inputPass = EditText(this).apply {
            hint = "كلمة المرور الجديدة (اختياري)"
            setHintTextColor(COLOR_TEXT_SECONDARY)
            setTextColor(Color.WHITE)
            textSize = 15f
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            background = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(Color.parseColor("#331E293B"))
                cornerRadius = 12f * density
                setStroke((1 * density).toInt(), Color.parseColor("#3338BDF8"))
            }
            setPadding((12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }
        dialogRoot.addView(inputPass)
val progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * density).toInt()
            }
        }
        dialogRoot.addView(progressBar)
val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * density).toInt()
            }
        }
val btnCancel = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33FFFFFF"))
                cornerRadius = 24f * density
            }
            layoutParams = LinearLayout.LayoutParams(0, (48 * density).toInt(), 1f).apply {
                marginEnd = (8 * density).toInt()
            }
            setOnClickListener { dialog.dismiss() }
        }
        btnCancel.addView(TextView(this).apply {
            text = "إلغاء"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        actionRow.addView(btnCancel)
val btnSave = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(COLOR_NEON_CYAN)
                cornerRadius = 24f * density
            }
            layoutParams = LinearLayout.LayoutParams(0, (48 * density).toInt(), 2f)
        }
val btnSaveText = TextView(this).apply {
            text = "حفظ التعديلات"
            textSize = 14f
            setTextColor(Color.parseColor("#FFFCF8"))
            typeface = Typeface.DEFAULT_BOLD
        }
        btnSave.addView(btnSaveText)
        btnSave.setOnClickListener {
            val email = inputEmail.text.toString().trim()
val password = inputPass.text.toString().trim()
            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false
            CloudflareClient.adminUpdateUser(this, user.username, password.ifEmpty { null }, email.ifEmpty { null }, selectedAvatar) { success, errorMsg ->
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                if (success) {
                    loadAccounts()
                    Toast.makeText(this, "تم تحديث الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, errorMsg ?: "فشل تحديث الحساب", Toast.LENGTH_SHORT).show()
                }
            }
        }
        actionRow.addView(btnSave)
        dialogRoot.addView(actionRow)
        dialogScroll.addView(dialogRoot)
        dialog.setContentView(dialogScroll)
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }}