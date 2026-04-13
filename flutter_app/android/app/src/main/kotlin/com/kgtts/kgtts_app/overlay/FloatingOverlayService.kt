package com.kgtts.kgtts_app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kgtts.kgtts_app.data.UserPrefs
import com.kgtts.kgtts_app.service.RealtimeHostService
import com.kgtts.kgtts_app.util.AppLogger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Floating overlay with drag/snap/dock behavior and quick action panel.
 */
class FloatingOverlayService : Service(), RealtimeRuntimeBridge.Listener {
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val handler = Handler(Looper.getMainLooper())

    private var fabRoot: LinearLayout? = null
    private var fabParams: WindowManager.LayoutParams? = null
    private var fabIcon: ImageView? = null
    private var fabStatusText: TextView? = null

    private var panelRoot: LinearLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var panelVisible = false
    private var panelStatusText: TextView? = null
    private var panelInputLabel: TextView? = null
    private var panelOutputLabel: TextView? = null
    private var panelInputProgress: ProgressBar? = null
    private var panelPlaybackProgress: ProgressBar? = null
    private var panelToggleRealtimeAction: TextView? = null

    private var downRawX = 0f
    private var downRawY = 0f
    private var downWinX = 0
    private var downWinY = 0
    private var draggingFab = false
    private var edgeRight = true
    private var idleDocked = false

    private var autoDockEnabled = UserPrefs.Defaults.FLOATING_OVERLAY_AUTO_DOCK

    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    private var hostService: RealtimeHostService? = null
    private var hostBound = false

    private val idleDockRunnable = Runnable {
        dockFabIfIdle()
    }

    private val hostConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? RealtimeHostService.LocalBinder ?: return
            hostService = binder.getService()
            hostBound = true
            updateUiFromSnapshot()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hostService = null
            hostBound = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            AppLogger.e("FloatingOverlayService requires overlay permission")
            stopSelf()
            return
        }

        RealtimeHostService.ensureStarted(applicationContext)
        bindHostService()
        RealtimeRuntimeBridge.addListener(this)

        buildFabWindow()
        buildPanelWindow()
        applyConfig(lastConfig)
        showFabWindow()
        updateUiFromSnapshot()

        isShowing = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                showFabWindow()
                updateUiFromSnapshot()
            }

            ACTION_HIDE -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_UPDATE_CONFIG -> {
                val config = extractConfig(intent)
                lastConfig = config
                applyConfig(config)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(idleDockRunnable)
        RealtimeRuntimeBridge.removeListener(this)
        unbindHostService()
        hidePanelWindow()
        hideFabWindow()
        isShowing = false
        super.onDestroy()
    }

    override fun onAppRuntimeChanged() {
        handler.post {
            updateUiFromSnapshot()
        }
    }

    private fun buildFabWindow() {
        if (fabRoot != null) return

        val bubbleSize = dp(56)
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(6))

            val bubbleContainer = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(bubbleSize, bubbleSize)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#FF1F6FEB"))
                }
                elevation = dp(4).toFloat()
            }

            fabIcon = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                setColorFilter(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
            }
            bubbleContainer.addView(fabIcon)

            fabStatusText = TextView(context).apply {
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(dp(8), dp(2), dp(8), dp(2))
                text = "待机"
                maxLines = 1
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(10).toFloat()
                    setColor(Color.parseColor("#CC10151F"))
                }
            }

            addView(bubbleContainer)
            addView(fabStatusText)

            setOnTouchListener { _, event -> handleFabTouch(event) }
            setOnLongClickListener {
                toggleRealtime()
                true
            }
        }

        fabRoot = bubble
        fabParams = createBaseLayoutParams().apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(10)
            y = dp(120)
        }
    }

    private fun buildPanelWindow() {
        if (panelRoot != null) return

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#EE0F141C"))
                setStroke(dp(1), Color.parseColor("#334A5A70"))
            }
            elevation = dp(8).toFloat()

            addView(TextView(context).apply {
                text = "悬浮窗快捷操作"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            })

            panelStatusText = TextView(context).apply {
                setTextColor(Color.parseColor("#B6C7DB"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                text = "等待实时状态"
                setPadding(0, dp(6), 0, dp(6))
                maxLines = 3
            }
            addView(panelStatusText)

            panelInputLabel = TextView(context).apply {
                setTextColor(Color.parseColor("#9FB3CA"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                text = "输入设备: -"
            }
            addView(panelInputLabel)

            panelOutputLabel = TextView(context).apply {
                setTextColor(Color.parseColor("#9FB3CA"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                text = "输出设备: -"
            }
            addView(panelOutputLabel)

            panelInputProgress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = 0
            }
            addView(panelInputProgress, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(6) })

            panelPlaybackProgress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = 0
            }
            addView(panelPlaybackProgress, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(4) })

            panelToggleRealtimeAction = addActionButton("开始实时", ::toggleRealtime)
            addActionButton("上屏最新识别", ::sendLatestToSubtitle)
            addActionButton("填入输入框", ::sendLatestToInput)
            addActionButton("打开便捷字幕", { openPage(OverlayBridge.TARGET_OPEN) })
            addActionButton("打开设置", { openPage(OverlayBridge.TARGET_OPEN_SETTINGS) })
            addActionButton("打开快捷名片", { openPage(OverlayBridge.TARGET_OPEN_QUICK_CARD) })
            addActionButton("打开画板", { openPage(OverlayBridge.TARGET_OPEN_DRAWING) })
            addActionButton("扫码", { openPage(OverlayBridge.TARGET_OPEN_QR_SCANNER) })
        }

        panelRoot = panel
        panelParams = createBaseLayoutParams().apply {
            gravity = Gravity.TOP or Gravity.START
            width = dp(260)
            x = dp(74)
            y = dp(120)
        }
    }

    private fun LinearLayout.addActionButton(
        label: String,
        action: () -> Unit,
    ): TextView {
        val button = TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#24364A"))
            }
            setOnClickListener { action() }
        }
        addView(button, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(6) })
        return button
    }

    private fun handleFabTouch(event: MotionEvent): Boolean {
        val params = fabParams ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(idleDockRunnable)
                restoreFromDock()
                draggingFab = false
                downRawX = event.rawX
                downRawY = event.rawY
                downWinX = params.x
                downWinY = params.y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - downRawX).toInt()
                val dy = (event.rawY - downRawY).toInt()
                if (!draggingFab && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    draggingFab = true
                }
                if (draggingFab) {
                    val size = displaySize()
                    val fabW = fabRoot?.width?.takeIf { it > 0 } ?: dp(68)
                    val fabH = fabRoot?.height?.takeIf { it > 0 } ?: dp(84)
                    params.x = (downWinX + dx).coerceIn(-fabW + dp(20), size.first - dp(20))
                    params.y = (downWinY + dy).coerceIn(0, max(0, size.second - fabH))
                    updateFabLayout()
                    updatePanelPosition()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (draggingFab) {
                    snapFabToEdge()
                } else {
                    togglePanel()
                }
                scheduleIdleDock()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (draggingFab) {
                    snapFabToEdge()
                }
                scheduleIdleDock()
                return true
            }
        }
        return false
    }

    private fun togglePanel() {
        if (panelVisible) {
            hidePanelWindow()
            scheduleIdleDock()
        } else {
            showPanelWindow()
            handler.removeCallbacks(idleDockRunnable)
        }
    }

    private fun showFabWindow() {
        val view = fabRoot ?: return
        val params = fabParams ?: return
        if (view.parent == null) {
            runCatching {
                windowManager.addView(view, params)
            }.onFailure {
                AppLogger.e("add fab failed", it)
            }
        }
    }

    private fun hideFabWindow() {
        val view = fabRoot ?: return
        if (view.parent != null) {
            runCatching {
                windowManager.removeView(view)
            }.onFailure {
                AppLogger.e("remove fab failed", it)
            }
        }
    }

    private fun showPanelWindow() {
        val view = panelRoot ?: return
        val params = panelParams ?: return
        if (view.parent == null) {
            runCatching {
                updatePanelPosition()
                windowManager.addView(view, params)
                panelVisible = true
            }.onFailure {
                AppLogger.e("add panel failed", it)
            }
        } else {
            panelVisible = true
            updatePanelPosition()
        }
    }

    private fun hidePanelWindow() {
        val view = panelRoot ?: return
        if (view.parent != null) {
            runCatching {
                windowManager.removeView(view)
            }.onFailure {
                AppLogger.e("remove panel failed", it)
            }
        }
        panelVisible = false
    }

    private fun updateFabLayout() {
        val view = fabRoot ?: return
        val params = fabParams ?: return
        if (view.parent != null) {
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    private fun updatePanelLayout() {
        val view = panelRoot ?: return
        val params = panelParams ?: return
        if (view.parent != null) {
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    private fun snapFabToEdge() {
        val params = fabParams ?: return
        val view = fabRoot ?: return
        val size = displaySize()
        val width = view.width.takeIf { it > 0 } ?: dp(68)
        val maxX = max(0, size.first - width)
        edgeRight = params.x + width / 2 >= size.first / 2
        params.x = if (edgeRight) maxX else 0
        params.y = params.y.coerceIn(0, max(0, size.second - (view.height.takeIf { it > 0 } ?: dp(84))))
        view.alpha = 1f
        idleDocked = false
        updateFabLayout()
        updatePanelPosition()
    }

    private fun scheduleIdleDock() {
        handler.removeCallbacks(idleDockRunnable)
        if (!autoDockEnabled || panelVisible) return
        handler.postDelayed(idleDockRunnable, FAB_IDLE_DOCK_DELAY_MS)
    }

    private fun dockFabIfIdle() {
        if (!autoDockEnabled || panelVisible || idleDocked) return
        val view = fabRoot ?: return
        val params = fabParams ?: return
        val size = displaySize()
        val width = view.width.takeIf { it > 0 } ?: dp(68)
        val visible = dp(22)
        params.x = if (edgeRight) {
            size.first - visible
        } else {
            -width + visible
        }
        view.alpha = FAB_IDLE_DOCK_ALPHA
        idleDocked = true
        updateFabLayout()
    }

    private fun restoreFromDock() {
        if (!idleDocked) return
        val params = fabParams ?: return
        val view = fabRoot ?: return
        val size = displaySize()
        val width = view.width.takeIf { it > 0 } ?: dp(68)
        val maxX = max(0, size.first - width)
        params.x = if (edgeRight) maxX else 0
        view.alpha = 1f
        idleDocked = false
        updateFabLayout()
    }

    private fun updatePanelPosition() {
        val fab = fabRoot ?: return
        val fabLp = fabParams ?: return
        val panel = panelRoot ?: return
        val panelLp = panelParams ?: return
        if (panel.parent == null) return

        val size = displaySize()
        val panelW = if (panel.width > 0) panel.width else dp(260)
        val panelH = if (panel.height > 0) panel.height else dp(360)
        val fabW = fab.width.takeIf { it > 0 } ?: dp(68)

        val margin = dp(8)
        var x = if (edgeRight) fabLp.x - panelW - margin else fabLp.x + fabW + margin
        x = x.coerceIn(0, max(0, size.first - panelW))
        var y = fabLp.y - panelH / 3
        y = y.coerceIn(0, max(0, size.second - panelH))

        panelLp.x = x
        panelLp.y = y
        updatePanelLayout()
    }

    private fun toggleRealtime() {
        val delegate = RealtimeRuntimeBridge.currentAppDelegate() ?: hostService
        if (delegate == null) {
            AppLogger.e("toggleRealtime ignored: no realtime delegate")
            return
        }
        val snapshot = RealtimeRuntimeBridge.currentSnapshot()
        if (snapshot.running) {
            delegate.stopRealtime()
        } else {
            delegate.startRealtime()
        }
    }

    private fun sendLatestToSubtitle() {
        sendLatestRecognized(OverlayBridge.TARGET_SUBTITLE)
    }

    private fun sendLatestToInput() {
        sendLatestRecognized(OverlayBridge.TARGET_INPUT)
    }

    private fun sendLatestRecognized(target: String) {
        val text = RealtimeRuntimeBridge.currentSnapshot().latestRecognizedText.trim()
        if (text.isEmpty()) {
            AppLogger.i("sendLatestRecognized ignored: empty text")
            return
        }
        val delegate = RealtimeRuntimeBridge.currentAppDelegate() ?: hostService ?: return
        delegate.submitQuickSubtitle(target, text)
    }

    private fun openPage(target: String) {
        runCatching {
            startActivity(OverlayBridge.buildOpenPageIntent(this, target))
        }.onFailure {
            AppLogger.e("open overlay page failed: $target", it)
        }
    }

    private fun updateUiFromSnapshot() {
        val snapshot = RealtimeRuntimeBridge.currentSnapshot()

        fabIcon?.setImageResource(
            if (snapshot.running) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_btn_speak_now
        )

        fabStatusText?.text = if (snapshot.running) {
            val msg = snapshot.latestRecognizedText.trim()
            if (msg.isNotEmpty()) msg.take(16) else "实时运行中"
        } else {
            "实时已停止"
        }

        panelStatusText?.text = buildString {
            append(if (snapshot.running) "状态: 运行中" else "状态: 已停止")
            val text = snapshot.latestRecognizedText.trim()
            if (text.isNotEmpty()) {
                append("\n最新识别: ")
                append(text.take(120))
            }
            val pttText = snapshot.pushToTalkStreamingText.trim()
            if (pttText.isNotEmpty()) {
                append("\nPTT预览: ")
                append(pttText.take(80))
            }
        }

        panelInputLabel?.text = "输入设备: ${snapshot.inputDeviceLabel.ifBlank { "-" }}"
        panelOutputLabel?.text = "输出设备: ${snapshot.outputDeviceLabel.ifBlank { "-" }}"
        panelInputProgress?.progress = (snapshot.inputLevel.coerceIn(0f, 1f) * 100f).toInt()
        panelPlaybackProgress?.progress = (snapshot.playbackProgress.coerceIn(0f, 1f) * 100f).toInt()
        panelToggleRealtimeAction?.text = if (snapshot.running) "停止实时" else "开始实时"
    }

    private fun applyConfig(config: Map<String, Any?>) {
        (config[UserPrefs.KEY_FLOATING_OVERLAY_AUTO_DOCK] as? Boolean)?.let {
            autoDockEnabled = it
        }

        if (!autoDockEnabled) {
            handler.removeCallbacks(idleDockRunnable)
            restoreFromDock()
        } else {
            scheduleIdleDock()
        }
    }

    private fun extractConfig(intent: Intent): Map<String, Any?> {
        val extras = intent.extras ?: return emptyMap()
        val out = mutableMapOf<String, Any?>()
        extras.keySet().forEach { key ->
            out[key] = extras.get(key)
        }
        return out
    }

    private fun bindHostService() {
        if (hostBound) return
        val intent = Intent(this, RealtimeHostService::class.java)
        runCatching {
            bindService(intent, hostConnection, Context.BIND_AUTO_CREATE)
        }.onFailure {
            AppLogger.e("bind RealtimeHostService failed", it)
        }
    }

    private fun unbindHostService() {
        if (!hostBound) return
        runCatching {
            unbindService(hostConnection)
        }.onFailure {
            AppLogger.e("unbind RealtimeHostService failed", it)
        }
        hostBound = false
        hostService = null
    }

    private fun createBaseLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
    }

    private fun displaySize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = resources.displayMetrics
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KIGTTS 悬浮窗")
            .setContentText("悬浮窗服务运行中")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "kgtts_overlay_service"
        private const val NOTIFICATION_ID = 2002
        private const val ACTION_SHOW = "overlay.show"
        private const val ACTION_HIDE = "overlay.hide"
        private const val ACTION_UPDATE_CONFIG = "overlay.update_config"

        private const val FAB_IDLE_DOCK_DELAY_MS = 3000L
        private const val FAB_IDLE_DOCK_ALPHA = 0.56f

        @Volatile
        private var isShowing = false

        @Volatile
        private var lastConfig: Map<String, Any?> = emptyMap()

        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun openOverlayPermissionSettings(context: Context) {
            runCatching {
                if (canDrawOverlays(context)) return
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }.onFailure {
                AppLogger.e("FloatingOverlayService.openOverlayPermissionSettings failed", it)
            }
        }

        fun show(context: Context) {
            if (!canDrawOverlays(context)) {
                AppLogger.e("FloatingOverlayService.show ignored: overlay permission missing")
                return
            }
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure {
                AppLogger.e("FloatingOverlayService.show failed", it)
            }
        }

        fun hide(context: Context) {
            runCatching {
                context.stopService(Intent(context, FloatingOverlayService::class.java))
            }.onFailure {
                AppLogger.e("FloatingOverlayService.hide failed", it)
            }
        }

        fun showing(): Boolean = isShowing

        fun updateConfig(context: Context, config: Map<String, Any?>) {
            lastConfig = config
            if (!showing()) {
                return
            }
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_UPDATE_CONFIG
                config.forEach { (k, v) ->
                    when (v) {
                        is Boolean -> putExtra(k, v)
                        is Int -> putExtra(k, v)
                        is Long -> putExtra(k, v)
                        is Float -> putExtra(k, v)
                        is Double -> putExtra(k, v)
                        is String -> putExtra(k, v)
                    }
                }
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure {
                AppLogger.e("FloatingOverlayService.updateConfig failed", it)
            }
        }
    }
}
