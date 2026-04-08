package com.kgtts.kgtts_app.channels

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import com.kgtts.kgtts_app.audio.*
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

    private fun ensureController(settings: Map<String, Any?>): RealtimeController {
        controller?.let { return it }
        val ctrl = RealtimeController(
            context = context,
            scope = scope,
            onResult = { id, text ->
                sendEvent(mapOf("type" to "result", "id" to id, "text" to text))
            },
            onStreamingResult = { text ->
                sendEvent(mapOf("type" to "streaming", "text" to text))
            },
            onProgress = { id, value ->
                sendEvent(mapOf("type" to "progress", "id" to id, "value" to value))
            },
            onLevel = { value ->
                sendEvent(mapOf("type" to "level", "value" to value))
            },
            onInputDevice = { label ->
                sendEvent(mapOf("type" to "inputDevice", "label" to label))
            },
            onOutputDevice = { label ->
                sendEvent(mapOf("type" to "outputDevice", "label" to label))
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
            initialNumberReplaceMode = (settings["number_replace_mode"] as? Number)?.toInt() ?: 0,
            initialAllowSystemAecWithAec3 = false,
            initialSpeakerVerifyEnabled = settings["speaker_verify_enabled"] as? Boolean ?: false,
            initialSpeakerVerifyThreshold = (settings["speaker_verify_threshold"] as? Number)?.toFloat() ?: 0.72f,
            initialSpeakerProfiles = emptyList()
        )
        controller = ctrl
        return ctrl
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "start" -> {
                val asrDir = call.argument<String>("asrDir") ?: return result.error("ARGS", "missing asrDir", null)
                val voiceDir = call.argument<String>("voiceDir") ?: return result.error("ARGS", "missing voiceDir", null)
                scope.launch {
                    try {
                        val ctrl = ensureController(emptyMap())
                        ctrl.start(File(asrDir), File(voiceDir))
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("START_FAILED", e.message, null)
                    }
                }
            }
            "stop" -> {
                scope.launch {
                    try {
                        controller?.stop()
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("STOP_FAILED", e.message, null)
                    }
                }
            }
            "updateSettings" -> {
                try {
                    val args = call.arguments as? Map<*, *> ?: emptyMap<String, Any>()
                    val ctrl = controller
                    if (ctrl != null) {
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
                        ((args["number_replace_mode"] as? Number))?.let { ctrl.setNumberReplaceMode(it.toInt()) }
                        (args["push_to_talk_mode"] as? Boolean)?.let { ctrl.setPushToTalkStreamingEnabled(it) }
                        (args["speaker_verify_enabled"] as? Boolean)?.let { ctrl.setSpeakerVerifyEnabled(it) }
                        ((args["speaker_verify_threshold"] as? Number))?.let { ctrl.setSpeakerVerifyThreshold(it.toFloat()) }
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
                        controller?.enqueueSpeakText(text)
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
                        val ctrl = ensureController(emptyMap())
                        val ok = ctrl.loadAsr(File(dir))
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
                        val ctrl = ensureController(emptyMap())
                        val ok = ctrl.loadTts(File(dir))
                        result.success(ok)
                    } catch (e: Exception) {
                        result.error("LOAD_VOICE_FAILED", e.message, null)
                    }
                }
            }
            "setPttPressed" -> {
                val pressed = call.argument<Boolean>("pressed") ?: false
                // PTT is handled via streaming in RealtimeController
                result.success(null)
            }
            "beginPttSession" -> {
                controller?.setPushToTalkStreamingEnabled(true)
                result.success(null)
            }
            "commitPttSession" -> {
                val action = call.argument<String>("action") ?: "cancel"
                controller?.setPushToTalkStreamingEnabled(false)
                if (action == "cancel") {
                    controller?.setSuppressAsrAutoSpeak(false)
                }
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    fun dispose() {
        scope.cancel()
        controller = null
    }
}
