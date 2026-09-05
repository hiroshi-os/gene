package com.gene.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.gene.app.MainActivity
import com.gene.app.data.GeneDatabase
import kotlin.math.abs
import kotlin.math.roundToInt

class FloatingCaptureService : Service() {
    private var windowManager: WindowManager? = null
    private var bubble: TextView? = null
    private var menu: LinearLayout? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private val prefs by lazy { getSharedPreferences("gene_bubble", MODE_PRIVATE) }
    private val density by lazy { resources.displayMetrics.density }

    private fun dp(value: Int): Int = (value * density).roundToInt()
    private fun overlayType() = if (android.os.Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun showBubble() {
        val initialX = prefs.getInt("x", dp(18))
        val initialY = prefs.getInt("y", dp(180))
        val params = WindowManager.LayoutParams(dp(58), dp(58), overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        bubbleParams = params
        bubble = TextView(this).apply {
            text = "G"
            textSize = 22f
            setTextColor(Color.rgb(247, 246, 243)) // Notion light bg
            gravity = Gravity.CENTER
            // Notion charcoal pill active
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(55, 53, 47))
            }
            elevation = dp(10).toFloat()
            contentDescription = "Gene floating bubble"
            setOnTouchListener(BubbleTouchListener())
        }
        try { windowManager?.addView(bubble, params) } catch (_: Exception) { stopSelf() }
    }

    private inner class BubbleTouchListener : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0
        private var moved = false
        private var downAt = 0L

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = bubbleParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX; downRawY = event.rawY
                    startX = params.x; startY = params.y
                    downAt = System.currentTimeMillis(); moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    if (moved) {
                        params.x = (startX + dx).roundToInt().coerceAtLeast(0)
                        params.y = (startY + dy).roundToInt().coerceAtLeast(dp(12))
                        try { windowManager?.updateViewLayout(bubble, params) } catch (_: Exception) {}
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val held = System.currentTimeMillis() - downAt
                    if (moved) prefs.edit().putInt("x", params.x).putInt("y", params.y).apply()
                    else if (held >= 450L) showMenu()
                    else openGene()
                    return true
                }
            }
            return true
        }
    }

    private fun openGene(openCapture: Boolean = true) {
        val selectedId = prefs.getLong("selected_person_id", -1L)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (selectedId > 0) {
                putExtra("person_id", selectedId)
                putExtra("open_capture", openCapture)
            }
        }
        startActivity(intent)
    }

    private fun showMenu() {
        if (menu != null) { closeMenu(); return }
        val selectedId = prefs.getLong("selected_person_id", -1L)
        val selectedName = prefs.getString("selected_person_name", "No persona selected").orEmpty()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(10))
            // Notion surface + hairline border
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.rgb(247, 246, 243))
                setStroke(dp(1), Color.rgb(227, 226, 224))
            }
            elevation = dp(14).toFloat()
        }
        addMenuLabel(container, "Gene")
        addMenuLabel(container, if (selectedId > 0) "Persona · $selectedName" else "Choose a persona", secondary = true)
        val people = try { GeneDatabase(applicationContext).people() } catch (_: Exception) { emptyList() }
        if (people.isEmpty()) addAction(container, "＋  Add a person") { openGene(); closeMenu() }
        else people.take(5).forEach { person ->
            addAction(container, if (person.id == selectedId) "✓  ${person.name}" else "○  ${person.name}") {
                prefs.edit().putLong("selected_person_id", person.id).putString("selected_person_name", person.name).apply()
                closeMenu()
            }
        }
        addRule(container)
        addAction(container, "↗  Open Gene") { openGene(); closeMenu() }
        addAction(container, "×  Close bubble") { getSharedPreferences("gene_settings", MODE_PRIVATE).edit().putBoolean("bubble_enabled", false).apply(); closeMenu(); stopSelf() }

        val p = WindowManager.LayoutParams(dp(244), WindowManager.LayoutParams.WRAP_CONTENT, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (bubbleParams?.x ?: dp(18)).coerceAtMost(resources.displayMetrics.widthPixels - dp(260))
            y = (bubbleParams?.y ?: dp(180)) + dp(68)
        }
        menu = container
        try { windowManager?.addView(container, p) } catch (_: Exception) { menu = null }
    }

    private fun addMenuLabel(parent: LinearLayout, text: String, secondary: Boolean = false) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = if (secondary) 13f else 15f
            setTextColor(if (secondary) Color.rgb(120, 119, 116) else Color.rgb(55, 53, 47))
            setPadding(0, dp(4), 0, dp(4))
        })
    }

    private fun addAction(parent: LinearLayout, text: String, action: () -> Unit) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.rgb(55, 53, 47))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
            isClickable = true
            setOnClickListener { action() }
        })
    }

    private fun addRule(parent: LinearLayout) {
        parent.addView(View(this).apply {
            setBackgroundColor(Color.rgb(227, 226, 224))
            layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply {
                topMargin = dp(5)
                bottomMargin = dp(5)
            }
        })
    }

    private fun closeMenu() {
        menu?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        menu = null
    }

    override fun onDestroy() {
        closeMenu()
        bubble?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        bubble = null
        bubbleParams = null
        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

