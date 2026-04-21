package com.lhtstudio.kigtts.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lhtstudio.kigtts.app.R
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.overlay.FloatingOverlayService
import com.lhtstudio.kigtts.app.overlay.OverlayBridge
import com.lhtstudio.kigtts.app.util.AppLogger
import com.lhtstudio.kigtts.app.util.ExternalShortcutCatalog
import com.lhtstudio.kigtts.app.util.ExternalShortcutChoice
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActionSpec
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VolumeHotkeyService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settings = UserPrefs.AppSettings()
    private var settingsJob: kotlinx.coroutines.Job? = null
    private var audioManager: AudioManager? = null
    private var volumeObserver: ContentObserver? = null
    private var lastMusicVolume = Int.MIN_VALUE
    private var pendingDirection = 0
    private var pendingDirectionAtMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("VolumeHotkeyService.onCreate")
        audioManager = getSystemService(AudioManager::class.java)
        startForegroundInternal()
        registerVolumeObserver()
        observeSettings()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_REFRESH) {
            updateNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        AppLogger.i("VolumeHotkeyService.onDestroy")
        settingsJob?.cancel()
        settingsJob = null
        volumeObserver?.let { runCatching { contentResolver.unregisterContentObserver(it) } }
        volumeObserver = null
        audioManager = null
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundInternal() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "音量热键",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "KIGTTS 音量热键监听正在运行"
                    setShowBadge(false)
                }
            )
        }
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("KIGTTS 音量热键")
            .setContentText(activeHotkeySummary())
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun activeHotkeySummary(): String {
        val labels = buildList {
            if (settings.volumeHotkeyUpDownEnabled) {
                add("音量加后减 -> ${VolumeHotkeyActions.labelOf(settings.volumeHotkeyUpDownAction)}")
            }
            if (settings.volumeHotkeyDownUpEnabled) {
                add("音量减后加 -> ${VolumeHotkeyActions.labelOf(settings.volumeHotkeyDownUpAction)}")
            }
        }
        return labels.joinToString("；").ifBlank { "音量热键未启用" }
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = scope.launch {
            UserPrefs.observeSettings(this@VolumeHotkeyService).collectLatest { next ->
                settings = next
                if (!hasEnabledHotkeys(next)) {
                    stopSelf()
                    return@collectLatest
                }
                updateNotification()
            }
        }
    }

    private fun registerVolumeObserver() {
        lastMusicVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                handleVolumePossiblyChanged()
            }
        }
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver!!)
    }

    private fun handleVolumePossiblyChanged() {
        val manager = audioManager ?: return
        val currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val previousVolume = lastMusicVolume
        lastMusicVolume = currentVolume
        val direction = when {
            currentVolume > previousVolume -> 1
            currentVolume < previousVolume -> -1
            else -> 0
        }
        if (direction != 0) {
            handleDirection(direction)
        }
    }

    private fun handleDirection(direction: Int) {
        val now = SystemClock.uptimeMillis()
        val previousDirection = pendingDirection
        if (previousDirection != 0 && previousDirection != direction && now - pendingDirectionAtMs <= SEQUENCE_WINDOW_MS) {
            when {
                previousDirection > 0 && direction < 0 && settings.volumeHotkeyUpDownEnabled ->
                    triggerAction(settings.volumeHotkeyUpDownAction)
                previousDirection < 0 && direction > 0 && settings.volumeHotkeyDownUpEnabled ->
                    triggerAction(settings.volumeHotkeyDownUpAction)
            }
            pendingDirection = 0
            pendingDirectionAtMs = 0L
            return
        }
        pendingDirection = direction
        pendingDirectionAtMs = now
    }

    private fun triggerAction(action: VolumeHotkeyActionSpec) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                when (action.kind) {
                    VolumeHotkeyActions.KIND_INTERNAL -> executeInternalAction(action.target)
                    VolumeHotkeyActions.KIND_OVERLAY -> executeOverlayAction(action.target)
                    VolumeHotkeyActions.KIND_EXTERNAL -> {
                        if (action.packageName.isNotBlank() && action.shortcutId.isNotBlank()) {
                            ExternalShortcutCatalog.launchChoice(
                                this@VolumeHotkeyService,
                                ExternalShortcutChoice(
                                    packageName = action.packageName,
                                    className = action.className,
                                    appLabel = action.appLabel,
                                    shortcutId = action.shortcutId,
                                    shortcutTitle = action.shortcutTitle
                                )
                            )
                        }
                    }
                }
            }.onFailure {
                AppLogger.e("VolumeHotkeyService.triggerAction failed", it)
            }
        }
    }

    private fun executeInternalAction(target: String) {
        val appTarget = when (target) {
            VolumeHotkeyActions.TARGET_QUICK_CARD -> OverlayBridge.TARGET_OPEN_QUICK_CARD
            VolumeHotkeyActions.TARGET_DRAWING -> OverlayBridge.TARGET_OPEN_DRAWING
            VolumeHotkeyActions.TARGET_SOUNDBOARD -> OverlayBridge.TARGET_OPEN_SOUNDBOARD
            VolumeHotkeyActions.TARGET_VOICE_PACK -> OverlayBridge.TARGET_OPEN_VOICE_PACK
            VolumeHotkeyActions.TARGET_QR_SCANNER -> OverlayBridge.TARGET_OPEN_QR_SCANNER
            else -> OverlayBridge.TARGET_OPEN
        }
        startActivity(OverlayBridge.buildOpenPageIntent(this, appTarget))
    }

    private fun executeOverlayAction(target: String) {
        if (!FloatingOverlayService.canDrawOverlays(this)) {
            startActivity(OverlayBridge.buildOpenPageIntent(this, OverlayBridge.TARGET_OPEN_OVERLAY))
            return
        }
        when (target) {
            VolumeHotkeyActions.TARGET_OPEN_MINI_QUICK_SUBTITLE -> FloatingOverlayService.openMiniQuickSubtitle(this)
            VolumeHotkeyActions.TARGET_OPEN_MINI_QUICK_CARD -> FloatingOverlayService.openMiniQuickCard(this)
            else -> FloatingOverlayService.openPanel(this)
        }
    }

    companion object {
        private const val CHANNEL_ID = "volume_hotkey"
        private const val NOTIFICATION_ID = 3205
        private const val ACTION_REFRESH = "com.lhtstudio.kigtts.app.action.VOLUME_HOTKEY_REFRESH"
        private const val ACTION_STOP = "com.lhtstudio.kigtts.app.action.VOLUME_HOTKEY_STOP"
        private const val SEQUENCE_WINDOW_MS = 1500L

        private fun hasEnabledHotkeys(settings: UserPrefs.AppSettings): Boolean {
            return settings.volumeHotkeyUpDownEnabled || settings.volumeHotkeyDownUpEnabled
        }

        suspend fun syncWithSettings(context: Context) {
            val settings = UserPrefs.getSettings(context)
            val intent = Intent(context, VolumeHotkeyService::class.java)
            if (hasEnabledHotkeys(settings)) {
                ContextCompat.startForegroundService(
                    context,
                    intent.apply { action = ACTION_REFRESH }
                )
            } else {
                context.stopService(intent)
            }
        }
    }
}
