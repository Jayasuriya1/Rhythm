package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.data.database.AppDatabase
import com.example.data.entity.BrainDumpNoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * Opt-in floating thought capture overlay.
 *
 * Key Capabilities:
 * - Managed as a Foreground Service to reliably survive closing the main Activity.
 * - Premium, ultra-compact floating coin (46dp) with hide/show tuck tab to avoid occupying screen space.
 * - In-overlay capture modal: allows entering and saving thoughts directly into Room WITHOUT leaving third-party apps.
 * - Zero intrusive data collection: never accesses screenshots, screen content, or other apps' inputs.
 */
class FloatingBrainDumpService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var dialogView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private var isTuckedToEdge = false
    private var selectedTag: String = "Distraction"

    companion object {
        const val CHANNEL_ID = "floating_brain_dump_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.example.service.action.START_FLOATING_OVERLAY"
        const val ACTION_STOP = "com.example.service.action.STOP_FLOATING_OVERLAY"

        var isRunning: Boolean = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (bubbleView == null && windowManager != null) {
            setupBubbleView()
        }
        isRunning = true

        // Persist opt-in state so overlay can be restored after device reboot
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getDatabase(applicationContext).settingsDao().saveSetting(
                    com.example.data.entity.AppSettingEntity("floating_overlay_enabled", "true")
                )
            } catch (_: Exception) {}
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Brain Dump Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating thought-capture bubble active over other applications"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("EXTRA_ACTION", "QUICK_BRAIN_DUMP")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingBrainDumpService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Brain Dump Active")
            .setContentText("Tap to capture thoughts or use the floating bubble over other apps.")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Bubble", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun getOverlayFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubbleView() {
        val layoutFlag = getOverlayFlag()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 260
        }
        bubbleParams = params

        // Main Bubble Container
        val root = FrameLayout(this).apply {
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }

        // Sleek circular coin badge
        val coin = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat()
                colors = intArrayOf(0xFF1E3F2A.toInt(), 0xFF142B1D.toInt()) // Deep Emerald gradient
                setStroke(dpToPx(1), 0x7785E0A3.toInt()) // Subtle mint border
            }
            background = bg
            elevation = dpToPx(8).toFloat()
        }

        val iconTv = TextView(this).apply {
            text = "💡"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
        }
        coin.addView(iconTv)

        val tuckTv = TextView(this).apply {
            text = " •"
            setTextColor(0xAA85E0A3.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
        }
        coin.addView(tuckTv)

        root.addView(coin)

        // Drag and Click Handlers
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 12 || Math.abs(deltaY) > 12) {
                        isClick = false
                    }
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    windowManager?.updateViewLayout(root, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        openInOverlayEditor()
                    } else {
                        // Snap toward edge if dragged near edge
                        val screenWidth = resources.displayMetrics.widthPixels
                        if (params.x < dpToPx(50)) {
                            params.x = dpToPx(8)
                            windowManager?.updateViewLayout(root, params)
                        } else if (params.x > screenWidth - dpToPx(90)) {
                            params.x = screenWidth - dpToPx(60)
                            windowManager?.updateViewLayout(root, params)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(root, params)
            bubbleView = root
        } catch (_: Exception) {
            stopSelf()
        }
    }

    /**
     * Opens the in-overlay thought capture and browsing card directly over the current app.
     * Soft keyboard focus is properly enabled via FLAG_NOT_TOUCH_MODAL without FLAG_NOT_FOCUSABLE.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun openInOverlayEditor() {
        if (dialogView != null) return // Already open

        // Temporarily hide the compact bubble while dialog is open
        bubbleView?.visibility = View.GONE

        val layoutFlag = getOverlayFlag()
        val dialogParams = WindowManager.LayoutParams(
            dpToPx(340),
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }

        var editingNote: BrainDumpNoteEntity? = null

        val cardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(18), dpToPx(16), dpToPx(18), dpToPx(16))

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                colors = intArrayOf(0xFF1F2B23.toInt(), 0xFF141E18.toInt()) // Sleek dark sage slate
                setStroke(dpToPx(1), 0x5585E0A3.toInt())
            }
            background = bg
            elevation = dpToPx(16).toFloat()
        }

        // Header Row: Title & Close Button
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(10))
        }

        val titleTv = TextView(this).apply {
            text = "💡 Park Thought"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(titleTv)

        val openAppTv = TextView(this).apply {
            text = "Open App ↗"
            setTextColor(0xFF85E0A3.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener {
                closeInOverlayEditor()
                val appIntent = Intent(this@FloatingBrainDumpService, com.example.MainActivity::class.java).apply {
                    putExtra("EXTRA_ACTION", "QUICK_BRAIN_DUMP")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(appIntent)
            }
        }
        headerRow.addView(openAppTv)

        val closeTv = TextView(this).apply {
            text = "✕"
            setTextColor(0xAAFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener {
                closeInOverlayEditor()
            }
        }
        headerRow.addView(closeTv)
        cardContainer.addView(headerRow)

        // Subtitle / Context Label
        val subLabel = TextView(this).apply {
            text = "Capture a thought or tap an existing one below to edit."
            setTextColor(0x99FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, dpToPx(8))
        }
        cardContainer.addView(subLabel)

        // Category Tags Row (Horizontal Scroll)
        val tagsScrollView = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dpToPx(10))
        }

        val tagsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val availableTags = listOf("Distraction", "Idea", "Study", "Todo", "Health", "Personal")
        val chipViews = mutableListOf<TextView>()

        fun refreshTagChips() {
            chipViews.forEach { v ->
                val currentTag = v.text.toString()
                (v.background as? GradientDrawable)?.let { bgDrawable ->
                    if (currentTag.equals(selectedTag, ignoreCase = true)) {
                        bgDrawable.setColor(0xFF2E6941.toInt())
                        bgDrawable.setStroke(dpToPx(1), 0xFF85E0A3.toInt())
                        v.setTextColor(Color.WHITE)
                    } else {
                        bgDrawable.setColor(0x22FFFFFF.toInt())
                        bgDrawable.setStroke(dpToPx(1), 0x33FFFFFF.toInt())
                        v.setTextColor(0xCCFFFFFF.toInt())
                    }
                }
            }
        }

        availableTags.forEach { tag ->
            val isSelected = tag == selectedTag
            val chip = TextView(this).apply {
                text = tag
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dpToPx(12), dpToPx(5), dpToPx(12), dpToPx(5))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(6)
                }

                val chipBg = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    if (isSelected) {
                        setColor(0xFF2E6941.toInt())
                        setStroke(dpToPx(1), 0xFF85E0A3.toInt())
                    } else {
                        setColor(0x22FFFFFF.toInt())
                        setStroke(dpToPx(1), 0x33FFFFFF.toInt())
                    }
                }
                background = chipBg
                setTextColor(if (isSelected) Color.WHITE else 0xCCFFFFFF.toInt())

                setOnClickListener {
                    selectedTag = tag
                    refreshTagChips()
                }
            }
            chipViews.add(chip)
            tagsContainer.addView(chip)
        }
        tagsScrollView.addView(tagsContainer)
        cardContainer.addView(tagsScrollView)

        // Text Input (EditText)
        val inputEdit = EditText(this).apply {
            hint = "Type your thought or mental distraction..."
            setHintTextColor(0x66FFFFFF.toInt())
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
            maxLines = 5
            gravity = Gravity.TOP or Gravity.START
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))

            val inputBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(0x33000000.toInt())
                setStroke(dpToPx(1), 0x4485E0A3.toInt())
            }
            background = inputBg
        }
        cardContainer.addView(inputEdit)

        // Recent Notes Quick Browse Section
        val recentLabel = TextView(this).apply {
            text = "Recent Thoughts (tap to edit):"
            setTextColor(0x77FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dpToPx(10), 0, dpToPx(4))
        }
        cardContainer.addView(recentLabel)

        val recentScrollView = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dpToPx(8))
        }

        val recentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        recentScrollView.addView(recentContainer)
        cardContainer.addView(recentScrollView)

        // Action Buttons Row
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(8), 0, 0)
        }

        val hideBubbleBtn = TextView(this).apply {
            text = "Stop Bubble"
            setTextColor(0x88FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            setOnClickListener {
                closeInOverlayEditor()
                stopSelf()
            }
        }
        actionsRow.addView(hideBubbleBtn)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }
        actionsRow.addView(spacer)

        val cancelBtn = TextView(this).apply {
            text = "Cancel"
            setTextColor(0xDDFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            setOnClickListener {
                closeInOverlayEditor()
            }
        }
        actionsRow.addView(cancelBtn)

        val saveBtn = TextView(this).apply {
            text = "Save Thought"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))

            val saveBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                colors = intArrayOf(0xFF3F5E48.toInt(), 0xFF2A4232.toInt())
                setStroke(dpToPx(1), 0xFF85E0A3.toInt())
            }
            background = saveBg

            setOnClickListener {
                val thought = inputEdit.text.toString().trim()
                if (thought.isBlank()) {
                    Toast.makeText(this@FloatingBrainDumpService, "Please write a thought first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentEditing = editingNote
                if (currentEditing != null) {
                    // Update existing note without creating duplicates
                    val updatedNote = currentEditing.copy(
                        content = thought,
                        tag = selectedTag,
                        updatedAt = System.currentTimeMillis()
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            AppDatabase.getDatabase(applicationContext).brainDumpDao().updateNote(updatedNote)
                        } catch (_: Exception) {}
                    }
                    Toast.makeText(this@FloatingBrainDumpService, "✓ Updated thought", Toast.LENGTH_SHORT).show()
                } else {
                    // Persist new note to Room
                    val newNote = BrainDumpNoteEntity(
                        content = thought,
                        tag = selectedTag,
                        isPinned = false,
                        isArchived = false,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            AppDatabase.getDatabase(applicationContext).brainDumpDao().insertNote(newNote)
                        } catch (_: Exception) {}
                    }
                    Toast.makeText(
                        this@FloatingBrainDumpService,
                        "✓ Parked: \"${newNote.displayTitle()}\"",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                closeInOverlayEditor()
            }
        }
        actionsRow.addView(saveBtn)
        cardContainer.addView(actionsRow)

        // Asynchronously populate recent thoughts
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val recentNotes = AppDatabase.getDatabase(applicationContext).brainDumpDao().getAllNotesSync().take(6)
                CoroutineScope(Dispatchers.Main).launch {
                    recentContainer.removeAllViews()
                    if (recentNotes.isEmpty()) {
                        val emptyTv = TextView(this@FloatingBrainDumpService).apply {
                            text = "No saved thoughts yet."
                            setTextColor(0x55FFFFFF.toInt())
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        }
                        recentContainer.addView(emptyTv)
                    } else {
                        // "+ New Thought" reset button
                        val newBtn = TextView(this@FloatingBrainDumpService).apply {
                            text = "+ New"
                            setTextColor(0xFF85E0A3.toInt())
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                            val bg = GradientDrawable().apply {
                                cornerRadius = dpToPx(10).toFloat()
                                setColor(0x2285E0A3.toInt())
                                setStroke(dpToPx(1), 0x5585E0A3.toInt())
                            }
                            background = bg
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply { marginEnd = dpToPx(6) }

                            setOnClickListener {
                                editingNote = null
                                titleTv.text = "💡 Park Thought"
                                subLabel.text = "Capture a thought or tap an existing one below to edit."
                                saveBtn.text = "Save Thought"
                                inputEdit.setText("")
                            }
                        }
                        recentContainer.addView(newBtn)

                        recentNotes.forEach { note ->
                            val noteChip = TextView(this@FloatingBrainDumpService).apply {
                                val snippet = if (note.content.length > 20) note.content.take(20) + "…" else note.content
                                text = "[${note.tag}] $snippet"
                                setTextColor(0xDDFFFFFF.toInt())
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                                setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                                val bg = GradientDrawable().apply {
                                    cornerRadius = dpToPx(10).toFloat()
                                    setColor(0x22FFFFFF.toInt())
                                    setStroke(dpToPx(1), 0x33FFFFFF.toInt())
                                }
                                background = bg
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                ).apply { marginEnd = dpToPx(6) }

                                setOnClickListener {
                                    editingNote = note
                                    titleTv.text = "✏️ Edit Thought #${note.id}"
                                    subLabel.text = "Editing saved thought. Modifies existing record."
                                    saveBtn.text = "Update Thought"
                                    inputEdit.setText(note.content)
                                    inputEdit.setSelection(note.content.length)
                                    selectedTag = note.tag ?: "Idea"
                                    refreshTagChips()
                                }
                            }
                            recentContainer.addView(noteChip)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Dismiss when tapping outside the dialog
        cardContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                closeInOverlayEditor()
                true
            } else {
                false
            }
        }

        try {
            windowManager?.addView(cardContainer, dialogParams)
            dialogView = cardContainer

            // Focus the edit text and open keyboard
            inputEdit.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(inputEdit, InputMethodManager.SHOW_IMPLICIT)
        } catch (_: Exception) {
            bubbleView?.visibility = View.VISIBLE
        }
    }

    private fun closeInOverlayEditor() {
        dialogView?.let { view ->
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        dialogView = null
        bubbleView?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        closeInOverlayEditor()
        bubbleView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        bubbleView = null
        isRunning = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        // Update database setting so it doesn't auto-restore on reboot after explicit stop
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getDatabase(applicationContext).settingsDao().saveSetting(
                    com.example.data.entity.AppSettingEntity("floating_overlay_enabled", "false")
                )
            } catch (_: Exception) {}
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
