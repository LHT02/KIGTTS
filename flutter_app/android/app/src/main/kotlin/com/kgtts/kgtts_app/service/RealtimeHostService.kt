package com.kgtts.kgtts_app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import com.kgtts.kgtts_app.audio.RealtimeController
import com.kgtts.kgtts_app.data.ModelRepository
import com.kgtts.kgtts_app.data.UserPrefs
import com.kgtts.kgtts_app.overlay.OverlayBridge
import com.kgtts.kgtts_app.overlay.RealtimeRuntimeBridge
import com.kgtts.kgtts_app.util.AppLogger
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Long-lived realtime host process for overlay/app integration.
 *
 * The service exposes realtime controls and continuously publishes runtime
 * snapshot updates to [RealtimeRuntimeBridge] so FloatingOverlayService can
 * render status and trigger quick actions.
 */
class RealtimeHostService : Service(), RealtimeRuntimeBridge.AppDelegate {
    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var prefs: SharedPreferences
    private lateinit var modelRepo: ModelRepository

    private var controller: RealtimeController? = null
    private var cachedSettings: Map<String, Any?> = emptyMap()
    private var asrDir: File? = null
    private var voiceDir: File? = null

    @Volatile
    private var running = false

    @Volatile
    private var latestRecognizedText = ""

    @Volatile
    private var inputLevel = 0f

    @Volatile
    private var playbackProgress = 0f

    @Volatile
    private var inputDeviceLabel = ""

    @Volatile
    private var outputDeviceLabel = ""

    @Volatile
    private var pushToTalkPressed = false

    @Volatile
    private var pushToTalkStreamingText = ""

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        modelRepo = ModelRepository(applicationContext)
        cachedSettings = loadSettingsFromPrefs()
        RealtimeRuntimeBridge.registerAppDelegate(this)
        publishSnapshot()
        scope.launch {
            initModelSelection()
        }
        AppLogger.i("RealtimeHostService.onCreate")
    }

    override fun onDestroy() {
        val active = controller
        controller = null
        if (active != null) {
            runCatching {
                runBlocking(Dispatchers.IO) {
                    active.stop()
                }
            }.onFailure {
                AppLogger.e("RealtimeHostService stop failed", it)
            }
        }
        running = false
        pushToTalkPressed = false
        pushToTalkStreamingText = ""
        publishSnapshot()
        RealtimeRuntimeBridge.unregisterAppDelegate(this)
        scope.cancel()
        super.onDestroy()
    }

    fun setSelectedAsrDir(path: String?) {
        asrDir = path?.let(::File)
    }

    fun setSelectedVoiceDir(path: String?) {
        voiceDir = path?.let(::File)
    }

    fun updateSettings(settings: Map<String, Any?>) {
        cachedSettings = settings
        controller?.let { applySettings(it, settings) }
    }

    override fun startRealtime() {
        scope.launch {
            try {
                if (!ensureModelsReady()) {
                    AppLogger.e("RealtimeHostService start failed: model not selected")
                    return@launch
                }
                val asr = asrDir ?: return@launch
                val voice = voiceDir ?: return@launch
                val ctrl = ensureController()
                val loadedAsr = withContext(Dispatchers.IO) { ctrl.loadAsr(asr) }
                if (!loadedAsr) return@launch
                val loadedVoice = withContext(Dispatchers.IO) { ctrl.loadTts(voice) }
                if (!loadedVoice) return@launch
                withContext(Dispatchers.IO) {
                    ctrl.startMic()
                }
                running = true
                publishSnapshot()
            } catch (e: Exception) {
                AppLogger.e("RealtimeHostService startRealtime failed", e)
            }
        }
    }

    override fun stopRealtime() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    controller?.stopMic()
                }
                running = false
                pushToTalkPressed = false
                pushToTalkStreamingText = ""
                publishSnapshot()
            }.onFailure {
                AppLogger.e("RealtimeHostService stopRealtime failed", it)
            }
        }
    }

    fun enqueueTts(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ensureController().enqueueSpeakText(message)
                }
            }.onFailure {
                AppLogger.e("RealtimeHostService enqueueTts failed", it)
            }
        }
    }

    override fun submitQuickSubtitle(target: String, text: String) {
        val normalized = text.trim()
        runCatching {
            val intent = OverlayBridge.buildQuickSubtitleIntent(
                context = this,
                target = target,
                text = normalized,
                navigateToPage = true,
            )
            startActivity(intent)
        }.onFailure {
            AppLogger.e("RealtimeHostService submitQuickSubtitle failed", it)
        }
        if (target == OverlayBridge.TARGET_SUBTITLE && normalized.isNotEmpty()) {
            enqueueTts(normalized)
        }
    }

    override fun beginPushToTalkSession() {
        val ctrl = controller ?: return
        val pushToTalkMode = cachedSettings[UserPrefs.KEY_PUSH_TO_TALK_MODE] as? Boolean ?: false
        val confirmMode = cachedSettings[UserPrefs.KEY_PUSH_TO_TALK_CONFIRM_INPUT] as? Boolean ?: false
        ctrl.setPushToTalkMode(pushToTalkMode)
        ctrl.setPushToTalkSessionActive(pushToTalkMode)
        ctrl.setPushToTalkStreamingEnabled(pushToTalkMode && confirmMode)
        pushToTalkPressed = pushToTalkMode
        publishSnapshot()
    }

    override fun setPushToTalkPressed(pressed: Boolean) {
        val ctrl = controller ?: return
        val pushToTalkMode = cachedSettings[UserPrefs.KEY_PUSH_TO_TALK_MODE] as? Boolean ?: false
        val confirmMode = cachedSettings[UserPrefs.KEY_PUSH_TO_TALK_CONFIRM_INPUT] as? Boolean ?: false
        ctrl.setPushToTalkSessionActive(pressed && pushToTalkMode)
        ctrl.setPushToTalkStreamingEnabled(pressed && pushToTalkMode && confirmMode)
        pushToTalkPressed = pressed && pushToTalkMode
        publishSnapshot()
    }

    override fun commitPushToTalkSession(action: RealtimeRuntimeBridge.PttCommitAction) {
        val ctrl = controller ?: return
        ctrl.setPushToTalkStreamingEnabled(false)
        ctrl.setPushToTalkSessionActive(false)
        pushToTalkPressed = false
        val text = pushToTalkStreamingText.trim()
        pushToTalkStreamingText = ""
        publishSnapshot()
        if (text.isEmpty()) return
        when (action) {
            RealtimeRuntimeBridge.PttCommitAction.SendToSubtitle -> submitQuickSubtitle(
                OverlayBridge.TARGET_SUBTITLE,
                text,
            )

            RealtimeRuntimeBridge.PttCommitAction.SendToInput -> submitQuickSubtitle(
                OverlayBridge.TARGET_INPUT,
                text,
            )

            RealtimeRuntimeBridge.PttCommitAction.Cancel -> Unit
        }
    }

    private suspend fun initModelSelection() {
        withContext(Dispatchers.IO) {
            val bundledAsr = modelRepo.ensureBundledAsr()
            val bundledVoice = modelRepo.ensureBundledVoice()

            val selectedAsr = prefs.getString(pref(UserPrefs.KEY_LAST_ASR), null)
            asrDir = selectedAsr
                ?.takeIf { it.isNotBlank() }
                ?.let { File(File(filesDir, "models/asr"), it) }
                ?.takeIf { it.isDirectory }
                ?: bundledAsr

            val selectedVoice = prefs.getString(pref(UserPrefs.KEY_LAST_VOICE), null)
            voiceDir = selectedVoice
                ?.takeIf { it.isNotBlank() }
                ?.let { modelRepo.resolveVoicePack(it) }
                ?: bundledVoice
        }
    }

    private suspend fun ensureModelsReady(): Boolean {
        if (asrDir != null && voiceDir != null) return true
        initModelSelection()
        return asrDir != null && voiceDir != null
    }

    private fun ensureController(): RealtimeController {
        controller?.let { return it }
        val settings = cachedSettings
        val ctrl = RealtimeController(
            context = applicationContext,
            scope = scope,
            onResult = { _, text ->
                latestRecognizedText = text
                pushToTalkStreamingText = ""
                publishSnapshot()
            },
            onStreamingResult = { text ->
                pushToTalkStreamingText = text
                publishSnapshot()
            },
            onProgress = { _, value ->
                playbackProgress = value.coerceIn(0f, 1f)
                publishSnapshot()
            },
            onLevel = { value ->
                inputLevel = value.coerceIn(0f, 1f)
                publishSnapshot()
            },
            onInputDevice = { label ->
                inputDeviceLabel = label
                publishSnapshot()
            },
            onOutputDevice = { label ->
                outputDeviceLabel = label
                publishSnapshot()
            },
            onAec3Status = {},
            onAec3Diag = {},
            onSpeakerVerify = { _, _ -> },
            onError = { msg -> AppLogger.e("RealtimeHostService controller error: $msg") },
            initialSuppressWhilePlaying = settings[UserPrefs.KEY_MUTE_WHILE_PLAYING] as? Boolean
                ?: UserPrefs.Defaults.MUTE_WHILE_PLAYING,
            initialUseVoiceCommunication = settings[UserPrefs.KEY_ECHO_SUPPRESSION] as? Boolean
                ?: UserPrefs.Defaults.ECHO_SUPPRESSION,
            initialCommunicationMode = settings[UserPrefs.KEY_COMMUNICATION_MODE] as? Boolean
                ?: UserPrefs.Defaults.COMMUNICATION_MODE,
            initialMinVolumePercent = (settings[UserPrefs.KEY_MIN_VOLUME_PERCENT] as? Number)?.toInt()
                ?: UserPrefs.Defaults.MIN_VOLUME_PERCENT,
            initialPlaybackGainPercent =
                (settings[UserPrefs.KEY_PLAYBACK_GAIN_PERCENT] as? Number)?.toInt()
                    ?: UserPrefs.Defaults.PLAYBACK_GAIN_PERCENT,
            initialPiperNoiseScale = (settings[UserPrefs.KEY_PIPER_NOISE_SCALE] as? Number)?.toFloat()
                ?: UserPrefs.Defaults.PIPER_NOISE_SCALE,
            initialPiperLengthScale =
                (settings[UserPrefs.KEY_PIPER_LENGTH_SCALE] as? Number)?.toFloat()
                    ?: UserPrefs.Defaults.PIPER_LENGTH_SCALE,
            initialPiperNoiseW = (settings[UserPrefs.KEY_PIPER_NOISE_W] as? Number)?.toFloat()
                ?: UserPrefs.Defaults.PIPER_NOISE_W,
            initialPiperSentenceSilenceSec =
                (settings[UserPrefs.KEY_PIPER_SENTENCE_SILENCE] as? Number)?.toFloat()
                    ?: UserPrefs.Defaults.PIPER_SENTENCE_SILENCE,
            initialSuppressDelaySec = (settings[UserPrefs.KEY_MUTE_DELAY_SEC] as? Number)?.toFloat()
                ?: UserPrefs.Defaults.MUTE_DELAY_SEC,
            initialPreferredInputType =
                (settings[UserPrefs.KEY_PREFERRED_INPUT_TYPE] as? Number)?.toInt()
                    ?: UserPrefs.Defaults.PREFERRED_INPUT_TYPE,
            initialPreferredOutputType =
                (settings[UserPrefs.KEY_PREFERRED_OUTPUT_TYPE] as? Number)?.toInt()
                    ?: UserPrefs.Defaults.PREFERRED_OUTPUT_TYPE,
            initialUseAec3 = settings[UserPrefs.KEY_AEC3_ENABLED] as? Boolean
                ?: UserPrefs.Defaults.AEC3_ENABLED,
            initialDenoiserMode =
                (settings[UserPrefs.KEY_DENOISER_MODE] as? Number)?.toInt() ?: 0,
            initialNumberReplaceMode =
                (settings[UserPrefs.KEY_NUMBER_REPLACE_MODE] as? Number)?.toInt()
                    ?: UserPrefs.Defaults.NUMBER_REPLACE_MODE,
            initialAllowSystemAecWithAec3 = false,
            initialSpeakerVerifyEnabled =
                settings[UserPrefs.KEY_SPEAKER_VERIFY_ENABLED] as? Boolean
                    ?: UserPrefs.Defaults.SPEAKER_VERIFY_ENABLED,
            initialSpeakerVerifyThreshold =
                (settings[UserPrefs.KEY_SPEAKER_VERIFY_THRESHOLD] as? Number)?.toFloat()
                    ?: UserPrefs.Defaults.SPEAKER_VERIFY_THRESHOLD,
            initialSpeakerProfiles = emptyList(),
        )
        applySettings(ctrl, settings)
        controller = ctrl
        return ctrl
    }

    private fun applySettings(ctrl: RealtimeController, settings: Map<String, Any?>) {
        (settings[UserPrefs.KEY_MUTE_WHILE_PLAYING] as? Boolean)?.let { ctrl.setSuppressWhilePlaying(it) }
        (settings[UserPrefs.KEY_ECHO_SUPPRESSION] as? Boolean)?.let { ctrl.setUseVoiceCommunication(it) }
        (settings[UserPrefs.KEY_COMMUNICATION_MODE] as? Boolean)?.let { ctrl.setCommunicationMode(it) }
        (settings[UserPrefs.KEY_MIN_VOLUME_PERCENT] as? Number)?.let { ctrl.setMinVolumePercent(it.toInt()) }
        (settings[UserPrefs.KEY_PLAYBACK_GAIN_PERCENT] as? Number)?.let { ctrl.setPlaybackGainPercent(it.toInt()) }
        (settings[UserPrefs.KEY_PIPER_NOISE_SCALE] as? Number)?.let { ctrl.setPiperNoiseScale(it.toFloat()) }
        (settings[UserPrefs.KEY_PIPER_LENGTH_SCALE] as? Number)?.let { ctrl.setPiperLengthScale(it.toFloat()) }
        (settings[UserPrefs.KEY_PIPER_NOISE_W] as? Number)?.let { ctrl.setPiperNoiseW(it.toFloat()) }
        (settings[UserPrefs.KEY_PIPER_SENTENCE_SILENCE] as? Number)?.let {
            ctrl.setPiperSentenceSilenceSec(it.toFloat())
        }
        (settings[UserPrefs.KEY_MUTE_DELAY_SEC] as? Number)?.let { ctrl.setSuppressDelaySec(it.toFloat()) }
        (settings[UserPrefs.KEY_PREFERRED_INPUT_TYPE] as? Number)?.let {
            ctrl.setPreferredInputType(it.toInt())
        }
        (settings[UserPrefs.KEY_PREFERRED_OUTPUT_TYPE] as? Number)?.let {
            ctrl.setPreferredOutputType(it.toInt())
        }
        (settings[UserPrefs.KEY_AEC3_ENABLED] as? Boolean)?.let { ctrl.setUseAec3(it) }
        (settings[UserPrefs.KEY_DENOISER_MODE] as? Number)?.let { ctrl.setDenoiserMode(it.toInt()) }
        (settings[UserPrefs.KEY_NUMBER_REPLACE_MODE] as? Number)?.let {
            ctrl.setNumberReplaceMode(it.toInt())
        }
        (settings[UserPrefs.KEY_SPEAKER_VERIFY_ENABLED] as? Boolean)?.let {
            ctrl.setSpeakerVerifyEnabled(it)
        }
        (settings[UserPrefs.KEY_SPEAKER_VERIFY_THRESHOLD] as? Number)?.let {
            ctrl.setSpeakerVerifyThreshold(it.toFloat())
        }

        val pushToTalkMode = settings[UserPrefs.KEY_PUSH_TO_TALK_MODE] as? Boolean
            ?: UserPrefs.Defaults.PUSH_TO_TALK_MODE
        val pushToTalkConfirmInput = settings[UserPrefs.KEY_PUSH_TO_TALK_CONFIRM_INPUT] as? Boolean
            ?: UserPrefs.Defaults.PUSH_TO_TALK_CONFIRM_INPUT
        ctrl.setPushToTalkMode(pushToTalkMode)
        ctrl.setSuppressAsrAutoSpeak(pushToTalkMode && pushToTalkConfirmInput)
        ctrl.setPushToTalkStreamingEnabled(false)
        if (!pushToTalkMode) {
            ctrl.setPushToTalkSessionActive(false)
            pushToTalkPressed = false
        }
    }

    private fun loadSettingsFromPrefs(): Map<String, Any?> {
        return mapOf(
            UserPrefs.KEY_MUTE_WHILE_PLAYING to prefs.getBoolean(
                pref(UserPrefs.KEY_MUTE_WHILE_PLAYING),
                UserPrefs.Defaults.MUTE_WHILE_PLAYING,
            ),
            UserPrefs.KEY_MUTE_DELAY_SEC to prefs.getDouble(
                pref(UserPrefs.KEY_MUTE_DELAY_SEC),
                UserPrefs.Defaults.MUTE_DELAY_SEC.toDouble(),
            ).toFloat(),
            UserPrefs.KEY_ECHO_SUPPRESSION to prefs.getBoolean(
                pref(UserPrefs.KEY_ECHO_SUPPRESSION),
                UserPrefs.Defaults.ECHO_SUPPRESSION,
            ),
            UserPrefs.KEY_COMMUNICATION_MODE to prefs.getBoolean(
                pref(UserPrefs.KEY_COMMUNICATION_MODE),
                UserPrefs.Defaults.COMMUNICATION_MODE,
            ),
            UserPrefs.KEY_PREFERRED_INPUT_TYPE to prefs.getIntCompat(
                pref(UserPrefs.KEY_PREFERRED_INPUT_TYPE),
                UserPrefs.Defaults.PREFERRED_INPUT_TYPE,
            ),
            UserPrefs.KEY_PREFERRED_OUTPUT_TYPE to prefs.getIntCompat(
                pref(UserPrefs.KEY_PREFERRED_OUTPUT_TYPE),
                UserPrefs.Defaults.PREFERRED_OUTPUT_TYPE,
            ),
            UserPrefs.KEY_AEC3_ENABLED to prefs.getBoolean(
                pref(UserPrefs.KEY_AEC3_ENABLED),
                UserPrefs.Defaults.AEC3_ENABLED,
            ),
            UserPrefs.KEY_DENOISER_MODE to prefs.getIntCompat(pref(UserPrefs.KEY_DENOISER_MODE), 0),
            UserPrefs.KEY_MIN_VOLUME_PERCENT to prefs.getIntCompat(
                pref(UserPrefs.KEY_MIN_VOLUME_PERCENT),
                UserPrefs.Defaults.MIN_VOLUME_PERCENT,
            ),
            UserPrefs.KEY_PLAYBACK_GAIN_PERCENT to prefs.getIntCompat(
                pref(UserPrefs.KEY_PLAYBACK_GAIN_PERCENT),
                UserPrefs.Defaults.PLAYBACK_GAIN_PERCENT,
            ),
            UserPrefs.KEY_PIPER_NOISE_SCALE to prefs.getDouble(
                pref(UserPrefs.KEY_PIPER_NOISE_SCALE),
                UserPrefs.Defaults.PIPER_NOISE_SCALE.toDouble(),
            ).toFloat(),
            UserPrefs.KEY_PIPER_LENGTH_SCALE to prefs.getDouble(
                pref(UserPrefs.KEY_PIPER_LENGTH_SCALE),
                UserPrefs.Defaults.PIPER_LENGTH_SCALE.toDouble(),
            ).toFloat(),
            UserPrefs.KEY_PIPER_NOISE_W to prefs.getDouble(
                pref(UserPrefs.KEY_PIPER_NOISE_W),
                UserPrefs.Defaults.PIPER_NOISE_W.toDouble(),
            ).toFloat(),
            UserPrefs.KEY_PIPER_SENTENCE_SILENCE to prefs.getDouble(
                pref(UserPrefs.KEY_PIPER_SENTENCE_SILENCE),
                UserPrefs.Defaults.PIPER_SENTENCE_SILENCE.toDouble(),
            ).toFloat(),
            UserPrefs.KEY_NUMBER_REPLACE_MODE to prefs.getIntCompat(
                pref(UserPrefs.KEY_NUMBER_REPLACE_MODE),
                UserPrefs.Defaults.NUMBER_REPLACE_MODE,
            ),
            UserPrefs.KEY_PUSH_TO_TALK_MODE to prefs.getBoolean(
                pref(UserPrefs.KEY_PUSH_TO_TALK_MODE),
                UserPrefs.Defaults.PUSH_TO_TALK_MODE,
            ),
            UserPrefs.KEY_PUSH_TO_TALK_CONFIRM_INPUT to prefs.getBoolean(
                pref(UserPrefs.KEY_PUSH_TO_TALK_CONFIRM_INPUT),
                UserPrefs.Defaults.PUSH_TO_TALK_CONFIRM_INPUT,
            ),
            UserPrefs.KEY_SPEAKER_VERIFY_ENABLED to prefs.getBoolean(
                pref(UserPrefs.KEY_SPEAKER_VERIFY_ENABLED),
                UserPrefs.Defaults.SPEAKER_VERIFY_ENABLED,
            ),
            UserPrefs.KEY_SPEAKER_VERIFY_THRESHOLD to prefs.getDouble(
                pref(UserPrefs.KEY_SPEAKER_VERIFY_THRESHOLD),
                UserPrefs.Defaults.SPEAKER_VERIFY_THRESHOLD.toDouble(),
            ).toFloat(),
        )
    }

    private fun publishSnapshot() {
        RealtimeRuntimeBridge.updateAppSnapshot(
            RealtimeRuntimeBridge.Snapshot(
                running = running,
                latestRecognizedText = latestRecognizedText,
                inputLevel = inputLevel,
                playbackProgress = playbackProgress,
                inputDeviceLabel = inputDeviceLabel,
                outputDeviceLabel = outputDeviceLabel,
                pushToTalkPressed = pushToTalkPressed,
                pushToTalkStreamingText = pushToTalkStreamingText,
            )
        )
    }

    private fun pref(key: String): String = "flutter.$key"

    inner class LocalBinder : Binder() {
        fun getService(): RealtimeHostService = this@RealtimeHostService
    }

    companion object {
        fun ensureStarted(context: Context) {
            runCatching {
                context.startService(Intent(context, RealtimeHostService::class.java))
            }.onFailure {
                AppLogger.e("RealtimeHostService.ensureStarted failed", it)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, RealtimeHostService::class.java))
            }.onFailure {
                AppLogger.e("RealtimeHostService.stop failed", it)
            }
        }

        fun nextOverlayRequestId(): Long = SystemClock.uptimeMillis()
    }
}

private fun SharedPreferences.getDouble(key: String, default: Double): Double {
    val raw = getString(key, null)
    return raw?.toDoubleOrNull() ?: default
}

private fun SharedPreferences.getIntCompat(key: String, default: Int): Int {
    val value = all[key] ?: return default
    return when (value) {
        is Int -> value
        is Long -> value.toInt()
        is Float -> value.toInt()
        is Double -> value.toInt()
        is String -> value.toIntOrNull() ?: default
        else -> default
    }
}
