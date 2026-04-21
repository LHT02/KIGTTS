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
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lhtstudio.kigtts.app.R
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.util.AppLogger
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActionSpec
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActionExecutor
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActions
import com.lhtstudio.kigtts.app.util.VolumeHotkeySequenceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VolumeHotkeyService : Service() {
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.Main.immediate
    )
    private val sequenceDetector = VolumeHotkeySequenceDetector()
    private var settings = UserPrefs.AppSettings()
    private var settingsJob: kotlinx.coroutines.Job? = null
    private var audioManager: AudioManager? = null
    private var volumeObserver: ContentObserver? = null
    private var lastStreamVolumes: Map<Int, Int> = emptyMap()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("VolumeHotkeyService.onCreate")
        audioManager = getSystemService(AudioManager::class.java)
        val foregroundStarted =
            runCatching {
                startForegroundInternal()
                true
            }.onFailure {
                AppLogger.e("VolumeHotkeyService.startForeground failed", it)
            }.getOrDefault(false)
        if (!foregroundStarted) {
            stopSelf()
            return
        }
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
        sequenceDetector.reset()
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
        val manager = audioManager ?: return
        lastStreamVolumes = snapshotStreamVolumes(manager)
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
        val previousVolumes = lastStreamVolumes
        val currentVolumes = snapshotStreamVolumes(manager)
        lastStreamVolumes = currentVolumes
        val direction = resolveDirection(previousVolumes, currentVolumes)
        if (direction != 0) {
            handleDirection(direction)
        }
    }

    private fun snapshotStreamVolumes(manager: AudioManager): Map<Int, Int> {
        return VOLUME_STREAM_PRIORITY.associateWith { stream ->
            runCatching { manager.getStreamVolume(stream) }.getOrDefault(Int.MIN_VALUE)
        }
    }

    private fun resolveDirection(
        previousVolumes: Map<Int, Int>,
        currentVolumes: Map<Int, Int>
    ): Int {
        VOLUME_STREAM_PRIORITY.forEach { stream ->
            val previous = previousVolumes[stream] ?: Int.MIN_VALUE
            val current = currentVolumes[stream] ?: Int.MIN_VALUE
            if (previous == Int.MIN_VALUE || current == Int.MIN_VALUE || previous == current) {
                return@forEach
            }
            return when {
                current > previous -> 1
                current < previous -> -1
                else -> 0
            }
        }
        return 0
    }

    private fun handleDirection(direction: Int) {
        sequenceDetector.handleDirection(direction, settings, ::triggerAction)
    }

    private fun triggerAction(action: VolumeHotkeyActionSpec) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                VolumeHotkeyActionExecutor.execute(this@VolumeHotkeyService, action)
            }.onFailure {
                AppLogger.e("VolumeHotkeyService.triggerAction failed", it)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "volume_hotkey"
        private const val NOTIFICATION_ID = 3205
        private const val ACTION_REFRESH = "com.lhtstudio.kigtts.app.action.VOLUME_HOTKEY_REFRESH"
        private const val ACTION_STOP = "com.lhtstudio.kigtts.app.action.VOLUME_HOTKEY_STOP"
        private val VOLUME_STREAM_PRIORITY =
            listOf(
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_VOICE_CALL
            )

        private fun hasEnabledHotkeys(settings: UserPrefs.AppSettings): Boolean {
            return VolumeHotkeyActionExecutor.hasEnabledHotkeys(settings)
        }

        suspend fun syncWithSettings(context: Context) {
            val settings = UserPrefs.getSettings(context)
            val intent = Intent(context, VolumeHotkeyService::class.java)
            val useAccessibility =
                settings.volumeHotkeyAccessibilityEnabled &&
                    VolumeHotkeyAccessibilityService.isEnabled(context)
            if (hasEnabledHotkeys(settings) && !useAccessibility) {
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
