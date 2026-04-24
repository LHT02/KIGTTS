package com.lhtstudio.kigtts.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.lhtstudio.kigtts.app.overlay.FloatingOverlayService
import kotlin.math.abs

class VolumeHotkeyAccessibilityGuideService : Service() {
    private var windowManager: WindowManager? = null
    private var rootView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FloatingOverlayService.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        showOverlayIfNeeded()
        return START_STICKY
    }

    override fun onDestroy() {
        rootView?.let { view ->
            runCatching {
                if (view.parent != null) {
                    windowManager?.removeView(view)
                }
            }
        }
        rootView = null
        layoutParams = null
        windowManager = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun showOverlayIfNeeded() {
        if (rootView != null) return
        val manager = getSystemService(WindowManager::class.java) ?: return
        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 16.dp()
                y = 112.dp()
            }
        val overlayView = buildOverlayView(params)
        manager.addView(overlayView, params)
        windowManager = manager
        layoutParams = params
        rootView = overlayView
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildOverlayView(params: WindowManager.LayoutParams): View {
        val dark = isDarkTheme()
        val cardColor = if (dark) 0xFF1D2023.toInt() else Color.WHITE
        val onSurfaceColor = if (dark) 0xFFE6EAED.toInt() else 0xFF111417.toInt()
        val onSurfaceVariantColor = if (dark) 0xFFB8C2CA.toInt() else 0xFF5F6B73.toInt()
        val outlineColor = if (dark) 0x33757F87 else 0x1A111417
        val card =
            object : LinearLayout(this) {
                private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                private var downRawX = 0f
                private var downRawY = 0f
                private var downX = 0
                private var downY = 0
                private var dragging = false

                private fun recordDown(event: MotionEvent) {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downX = params.x
                    downY = params.y
                    dragging = false
                }

                override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> recordDown(ev)
                        MotionEvent.ACTION_MOVE -> {
                            val dx = abs(ev.rawX - downRawX)
                            val dy = abs(ev.rawY - downRawY)
                            if (dx > touchSlop || dy > touchSlop) {
                                dragging = true
                                return true
                            }
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> dragging = false
                    }
                    return super.onInterceptTouchEvent(ev)
                }

                override fun onTouchEvent(event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            recordDown(event)
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            dragging = true
                            val dx = (event.rawX - downRawX).toInt()
                            val dy = (event.rawY - downRawY).toInt()
                            params.x = downX + dx
                            params.y = downY + dy
                            windowManager?.updateViewLayout(this, params)
                            return true
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            val handled = dragging
                            dragging = false
                            return handled
                        }
                    }
                    return super.onTouchEvent(event)
                }
            }.apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14.dp(), 14.dp(), 14.dp(), 14.dp())
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 4.dp().toFloat()
                        setColor(cardColor)
                        setStroke(1.dp(), outlineColor)
                    }
                elevation = 8.dp().toFloat()
            }
        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
        val title =
            TextView(this).apply {
                text = "音量热键无障碍引导"
                setTextColor(onSurfaceColor)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
            }
        val close =
            TextView(this).apply {
                text = "✕"
                setTextColor(onSurfaceVariantColor)
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                minWidth = 32.dp()
                minHeight = 32.dp()
                setPadding(8.dp(), 4.dp(), 0, 4.dp())
                setOnClickListener { stopSelf() }
            }
        header.addView(
            title,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        header.addView(
            close,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        card.addView(header)
        card.addView(instructionText("1. 在系统无障碍列表中找到“KIGTTS 音量热键辅助”。", onSurfaceColor))
        card.addView(instructionText("2. 进入后打开开关，允许 KIGTTS 监听音量按键并辅助直达 QQ 扫一扫。", onSurfaceColor))
        card.addView(instructionText("3. 开启后直接返回 KIGTTS，即会优先改用更稳定的无障碍监听。", onSurfaceColor))
        return card
    }

    private fun instructionText(
        text: String,
        textColor: Int,
        emphasized: Boolean = true
    ): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(textColor)
            textSize = if (emphasized) 13f else 12f
            setLineSpacing(0f, 1.12f)
            setPadding(0, 8.dp(), 0, 0)
        }
    }

    private fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    companion object {
        fun start(context: Context) {
            if (!FloatingOverlayService.canDrawOverlays(context)) return
            context.startService(Intent(context, VolumeHotkeyAccessibilityGuideService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VolumeHotkeyAccessibilityGuideService::class.java))
        }
    }
}
