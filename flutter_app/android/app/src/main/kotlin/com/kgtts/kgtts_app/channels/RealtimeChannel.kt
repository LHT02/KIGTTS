package com.kgtts.kgtts_app.channels

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import com.kgtts.kgtts_app.audio.*
import com.kgtts.kgtts_app.overlay.OverlayBridge
import com.kgtts.kgtts_app.overlay.RealtimeRuntimeBridge
import com.kgtts.kgtts_app.util.AppLogger
import java.io.File

class RealtimeChannel(
    flutterEngine: FlutterEngine,
    private val context: Context
) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

    private val methodChannel = MethodChannel(
        flutterEngine.dartExecutor.binaryMessenger,
        "com.kgtts.app/realtime"
    )
    private val eventChannel = EventChannel(
        flutterEngine.dartExecutor.binaryMessenger,
        "com.kgtts.app/realtime/events"
    )
    private var eventSink: EventChannel.EventSink? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: RealtimeController? = null
    private var cachedSettings: Map<String, Any?> = emptyMap()
    private var lastAsrDirPath: String? = null
    private var lastVoiceDirPath: String? = null

    @Volatile
    private var runtimeRunning = false

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
    private var pttPressed = false

    @Volatile
    private var pttStreamingText = ""

    private val appDelegate = object : RealtimeRuntimeBridge.AppDelegate {
        override fun startRealtime() {
            scope.launch {
                try {
                    val ctrl = ensureController()
                    val asrPath = lastAsrDirPath
                    val voicePath = lastVoiceDirPath
                    if (!asrPath.isNullOrBlank() && !voicePath.isNullOrBlank()) {
                        withContext(Dispatchers.IO) {
                            ctrl.start(File(asrPath), File(voicePath))
                        }
                    } else {
                        withContext(Dispatchers.IO) {
                            ctrl.startMic()
                        }
                    }
                    runtimeRunning = true
                    publishRuntimeSnapshot()
                } catch (e: Exception) {
                    AppLogger.e("RealtimeChannel delegate.startRealtime failed", e)
                }
            }
        }

        override fun stopRealtime() {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        controller?.stopMic()
                    }
                    runtimeRunning = false
                    pttPressed = false
                    pttStreamingText = ""
                    publishRuntimeSnapshot()
                }.onFailure {
                    AppLogger.e("RealtimeChannel delegate.stopRealtime failed", it)
                }
            }
        }

        override fun submitQuickSubtitle(target: String, text: String) {
            runCatching {
                val intent = OverlayBridge.buildQuickSubtitleIntent(
                    context = context,
                    target = target,
                    text = text,
                    navigateToPage = true,
                )
                context.startActivity(intent)
            }.onFailure {
                AppLogger.e("RealtimeChannel delegate.submitQuickSubtitle failed", it)
            }
        }

        override fun beginPushToTalkSession() {
            val ctrl = controller ?: return
            val pushToTalkMode = cachedSettings["push_to_talk_mode"] as? Boolean ?: false
            val confirmMode = cachedSettings["push_to_talk_confirm_input"] as? Boolean ?: false
            ctrl.setPushToTalkMode(pushToTalkMode)
            ctrl.setPushToTalkSessionActive(pushToTalkMode)
            ctrl.setPushToTalkStreamingEnabled(pushToTalkMode && confirmMode)
            pttPressed = pushToTalkMode
            publishRuntimeSnapshot()
        }

        override fun setPushToTalkPressed(pressed: Boolean) {
            val ctrl = controller ?: return
            val pushToTalkMode = cachedSettings["push_to_talk_mode"] as? Boolean ?: false
            val confirmMode = cachedSettings["push_to_talk_confirm_input"] as? Boolean ?: false
            ctrl.setPushToTalkSessionActive(pressed && pushToTalkMode)
            ctrl.setPushToTalkStreamingEnabled(pressed && pushToTalkMode && confirmMode)
            pttPressed = pressed && pushToTalkMode
            publishRuntimeSnapshot()
        }

        override fun commitPushToTalkSession(action: RealtimeRuntimeBridge.PttCommitAction) {
            val ctrl = controller ?: return
            ctrl.setPushToTalkStreamingEnabled(false)
            ctrl.setPushToTalkSessionActive(false)
            pttPressed = false
            val text = pttStreamingText.trim()
            pttStreamingText = ""
            publishRuntimeSnapshot()
            if (text.isNotEmpty()) {
                when (action) {
                    RealtimeRuntimeBridge.PttCommitAction.SendToSubtitle ->
                        submitQuickSubtitle(OverlayBridge.TARGET_SUBTITLE, text)

                    RealtimeRuntimeBridge.PttCommitAction.SendToInput ->
                        submitQuickSubtitle(OverlayBridge.TARGET_INPUT, text)

                    RealtimeRuntimeBridge.PttCommitAction.Cancel -> Unit
                }
            }
        }
    }

    init {
        methodChannel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    private fun sendEvent(data: Map<String, Any?>) {
        scope.launch(Dispatchers.Main) {
            eventSink?.success(data)
        }
    }

    private fun publishRuntimeSnapshot() {
        RealtimeRuntimeBridge.updateAppSnapshot(
            RealtimeRuntimeBridge.Snapshot(
                running = runtimeRunning,
                latestRecognizedText = latestRecognizedText,
                inputLevel = inputLevel,
                playbackProgress = playbackProgress,
                inputDeviceLabel = inputDeviceLabel,
                outputDeviceLabel = outputDeviceLabel,
                pushToTalkPressed = pttPressed,
                pushToTalkStreamingText = pttStreamingText,
            )
        )
    }

    private fun ensureController(): RealtimeController {
        controller?.let { return it }
        val settings = cachedSettings
        val ctrl = RealtimeController(
            context = context,
            scope = scope,
            onResult = { id, text ->
                sendEvent(mapOf("type" to "result", "id" to id, "text" to text))
                latestRecognizedText = text
                pttStreamingText = ""
                publishRuntimeSnapshot()
            },
            onStreamingResult = { text ->
                sendEvent(mapOf("type" to "streaming", "text" to text))
                pttStreamingText = text
                publishRuntimeSnapshot()
            },
            onProgress = { id, value ->
                sendEvent(mapOf("type" to "progress", "id" to id, "value" to value))
                playbackProgress = value.coerceIn(0f, 1f)
                publishRuntimeSnapshot()
            },
            onLevel = { value ->
                sendEvent(mapOf("type" to "level", "value" to value))
                inputLevel = value.coerceIn(0f, 1f)
                publishRuntimeSnapshot()
            },
            onInputDevice = { label ->
                sendEvent(mapOf("type" to "inputDevice", "label" to label))
                inputDeviceLabel = label
                publishRuntimeSnapshot()
            },
            onOutputDevice = { label ->
                sendEvent(mapOf("type" to "outputDevice", "label" to label))
                outputDeviceLabel = label
                publishRuntimeSnapshot()
            },
            onAec3Status = { status ->
                sendEvent(mapOf("type" to "aec3Status", "status" to status))
            },
            onAec3Diag = { diag ->
                sendEvent(mapOf("type" to "aec3Diag", "diag" to diag))
            },
            onSpeakerVerify = { similarity, passed ->
                sendEvent(mapOf("type" to "speakerVerify", "similarity" to similarity, "accepted" to passed))
            },
            onError = { message ->
                sendEvent(mapOf("type" to "error", "message" to message))
            },
            initialSuppressWhilePlaying = settings["mute_while_playing"] as? Boolean ?: false,
            initialUseVoiceCommunication = settings["echo_suppression"] as? Boolean ?: false,
            initialCommunicationMode = settings["communication_mode"] as? Boolean ?: false,
            initialMinVolumePercent = (settings["min_volume_percent"] as? Number)?.toInt() ?: 0,
            initialPlaybackGainPercent = (settings["playback_gain_percent"] as? Number)?.toInt() ?: 100,
            initialPiperNoiseScale = (settings["piper_noise_scale"] as? Number)?.toFloat() ?: 0.667f,
            initialPiperLengthScale = (settings["piper_length_scale"] as? Number)?.toFloat() ?: 1.0f,
            initialPiperNoiseW = (settings["piper_noise_w"] as? Number)?.toFloat() ?: 0.8f,
            initialPiperSentenceSilenceSec = (settings["piper_sentence_silence"] as? Number)?.toFloat() ?: 0.2f,
            initialSuppressDelaySec = (settings["mute_delay_sec"] as? Number)?.toFloat() ?: 0f,
            initialPreferredInputType = (settings["preferred_input_type"] as? Number)?.toInt() ?: 0,
            initialPreferredOutputType = (settings["preferred_output_type"] as? Number)?.toInt() ?: 100,
            initialUseAec3 = settings["aec3_enabled"] as? Boolean ?: false,
            initialDenoiserMode = (settings["denoiser_mode"] as? Number)?.toInt() ?: 0,
            initialNumberReplaceMode = (settings["number_replace_mode"] as? Number)?.toInt() ?: 0,
            initialAllowSystemAecWithAec3 = false,
            initialSpeakerVerifyEnabled = settings["speaker_verify_enabled"] as? Boolean ?: false,
            initialSpeakerVerifyThreshold = (settings["speaker_verify_threshold"] as? Number)?.toFloat() ?: 0.72f,
            initialSpeakerProfiles = emptyList()
        )
        controller = ctrl
        RealtimeRuntimeBridge.registerAppDelegate(appDelegate)
        publishRuntimeSnapshot()
        return ctrl
    }

    @Suppress("UNCHECKED_CAST")
    private fun applySettingsToController(ctrl: RealtimeController, args: Map<*, *>) {
        (args["mute_while_playing"] as? Boolean)?.let { ctrl.setSuppressWhilePlaying(it) }
        (args["echo_suppression"] as? Boolean)?.let { ctrl.setUseVoiceCommunication(it) }
        (args["communication_mode"] as? Boolean)?.let { ctrl.setCommunicationMode(it) }
        ((args["min_volume_percent"] as? Number))?.let { ctrl.setMinVolumePercent(it.toInt()) }
        ((args["playback_gain_percent"] as? Number))?.let { ctrl.setPlaybackGainPercent(it.toInt()) }
        ((args["piper_noise_scale"] as? Number))?.let { ctrl.setPiperNoiseScale(it.toFloat()) }
        ((args["piper_length_scale"] as? Number))?.let { ctrl.setPiperLengthScale(it.toFloat()) }
        ((args["piper_noise_w"] as? Number))?.let { ctrl.setPiperNoiseW(it.toFloat()) }
        ((args["piper_sentence_silence"] as? Number))?.let { ctrl.setPiperSentenceSilenceSec(it.toFloat()) }
        ((args["mute_delay_sec"] as? Number))?.let { ctrl.setSuppressDelaySec(it.toFloat()) }
        ((args["preferred_input_type"] as? Number))?.let { ctrl.setPreferredInputType(it.toInt()) }
        ((args["preferred_output_type"] as? Number))?.let { ctrl.setPreferredOutputType(it.toInt()) }
        (args["aec3_enabled"] as? Boolean)?.let { ctrl.setUseAec3(it) }
        ((args["denoiser_mode"] as? Number))?.let { ctrl.setDenoiserMode(it.toInt()) }
        ((args["number_replace_mode"] as? Number))?.let { ctrl.setNumberReplaceMode(it.toInt()) }
        (args["speaker_verify_enabled"] as? Boolean)?.let { ctrl.setSpeakerVerifyEnabled(it) }
        ((args["speaker_verify_threshold"] as? Number))?.let { ctrl.setSpeakerVerifyThreshold(it.toFloat()) }

        val pushToTalkMode =
            (args["push_to_talk_mode"] as? Boolean)
                ?: (cachedSettings["push_to_talk_mode"] as? Boolean)
                ?: false
        val pushToTalkConfirmInput =
            (args["push_to_talk_confirm_input"] as? Boolean)
                ?: (cachedSettings["push_to_talk_confirm_input"] as? Boolean)
                ?: false
        val suppressAutoSpeak = pushToTalkMode && pushToTalkConfirmInput
        ctrl.setPushToTalkMode(pushToTalkMode)
        ctrl.setPushToTalkSessionActive(false)
        ctrl.setSuppressAsrAutoSpeak(suppressAutoSpeak)
        if (!suppressAutoSpeak) {
            ctrl.setPushToTalkStreamingEnabled(false)
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "start" -> {
                val asrDir = call.argument<String>("asrDir") ?: return result.error("ARGS", "missing asrDir", null)
                val voiceDir = call.argument<String>("voiceDir") ?: return result.error("ARGS", "missing voiceDir", null)
                lastAsrDirPath = asrDir
                lastVoiceDirPath = voiceDir
                scope.launch {
                    try {
                        val ctrl = ensureController()
                        withContext(Dispatchers.IO) {
                            ctrl.start(File(asrDir), File(voiceDir))
                        }
                        runtimeRunning = true
                        publishRuntimeSnapshot()
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("START_FAILED", e.message, null)
                    }
                }
            }
            "stop" -> {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            controller?.stop()
                        }
                        runtimeRunning = false
                        pttPressed = false
                        pttStreamingText = ""
                        publishRuntimeSnapshot()
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("STOP_FAILED", e.message, null)
                    }
                }
            }
            "updateSettings" -> {
                try {
                    val args = call.arguments as? Map<*, *> ?: emptyMap<String, Any>()
                    // Always cache so ensureController() picks them up later
                    @Suppress("UNCHECKED_CAST")
                    cachedSettings = args as Map<String, Any?>
                    val ctrl = controller
                    if (ctrl != null) {
                        applySettingsToController(ctrl, args)
                    }
                    result.success(null)
                } catch (e: Exception) {
                    result.error("UPDATE_FAILED", e.message, null)
                }
            }
            "enqueueTts" -> {
                val text = call.argument<String>("text") ?: return result.error("ARGS", "missing text", null)
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            controller?.enqueueSpeakText(text)
                        }
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("TTS_FAILED", e.message, null)
                    }
                }
            }
            "loadAsr" -> {
                val dir = call.argument<String>("dir") ?: return result.error("ARGS", "missing dir", null)
                scope.launch {
                    try {
                        val ctrl = ensureController()
                        val ok = withContext(Dispatchers.IO) {
                            ctrl.loadAsr(File(dir))
                        }
                        result.success(ok)
                    } catch (e: Exception) {
                        result.error("LOAD_ASR_FAILED", e.message, null)
                    }
                }
            }
            "loadVoice" -> {
                val dir = call.argument<String>("dir") ?: return result.error("ARGS", "missing dir", null)
                scope.launch {
                    try {
                        val ctrl = ensureController()
                        val ok = withContext(Dispatchers.IO) {
                            ctrl.loadTts(File(dir))
                        }
                        result.success(ok)
                    } catch (e: Exception) {
                        result.error("LOAD_VOICE_FAILED", e.message, null)
                    }
                }
            }
            "setPttPressed" -> {
                val pressed = call.argument<Boolean>("pressed") ?: false
                val ctrl = controller
                if (ctrl != null) {
                    ctrl.setPushToTalkSessionActive(pressed)
                    val pushToTalkMode = cachedSettings["push_to_talk_mode"] as? Boolean ?: false
                    val pushToTalkConfirmInput = cachedSettings["push_to_talk_confirm_input"] as? Boolean ?: false
                    ctrl.setPushToTalkStreamingEnabled(
                        pressed && pushToTalkMode && pushToTalkConfirmInput
                    )
                    pttPressed = pressed && pushToTalkMode
                    publishRuntimeSnapshot()
                }
                result.success(null)
            }
            "beginPttSession" -> {
                val ctrl = ensureController()
                val pushToTalkMode = cachedSettings["push_to_talk_mode"] as? Boolean ?: false
                val pushToTalkConfirmInput = cachedSettings["push_to_talk_confirm_input"] as? Boolean ?: false
                val confirmPtt = pushToTalkMode && pushToTalkConfirmInput
                ctrl.setPushToTalkMode(pushToTalkMode)
                ctrl.setPushToTalkSessionActive(pushToTalkMode)
                ctrl.setSuppressAsrAutoSpeak(confirmPtt)
                ctrl.setPushToTalkStreamingEnabled(confirmPtt)
                pttPressed = pushToTalkMode
                publishRuntimeSnapshot()
                result.success(null)
            }
            "commitPttSession" -> {
                val action = call.argument<String>("action") ?: "cancel"
                val ctrl = controller
                if (ctrl != null) {
                    ctrl.setPushToTalkStreamingEnabled(false)
                    ctrl.setPushToTalkSessionActive(false)
                    val pushToTalkMode = cachedSettings["push_to_talk_mode"] as? Boolean ?: false
                    val pushToTalkConfirmInput = cachedSettings["push_to_talk_confirm_input"] as? Boolean ?: false
                    ctrl.setSuppressAsrAutoSpeak(pushToTalkMode && pushToTalkConfirmInput)
                    if (action.equals("cancel", ignoreCase = true)) {
                        // Keep interface compatibility with original channel action names.
                    }
                    pttPressed = false
                    pttStreamingText = ""
                    publishRuntimeSnapshot()
                }
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    fun dispose() {
        scope.cancel()
        RealtimeRuntimeBridge.unregisterAppDelegate(appDelegate)
        runtimeRunning = false
        pttPressed = false
        pttStreamingText = ""
        publishRuntimeSnapshot()
        controller = null
    }
}
