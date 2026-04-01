package com.kgtts.kgtts_app.channels

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.kgtts.kgtts_app.audio.EspeakNative

class EspeakChannel(
    flutterEngine: FlutterEngine,
    private val context: Context
) : MethodChannel.MethodCallHandler {

    private val channel = MethodChannel(
        flutterEngine.dartExecutor.binaryMessenger,
        "com.kgtts.app/espeak"
    )

    init {
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> {
                val dataPath = call.argument<String>("dataPath")
                    ?: return result.error("ARGS", "missing dataPath", null)
                try {
                    val ok = EspeakNative.ensureInit(dataPath)
                    result.success(ok)
                } catch (e: Exception) {
                    result.error("INIT_FAILED", e.message, null)
                }
            }
            "phonemize" -> {
                val text = call.argument<String>("text")
                    ?: return result.error("ARGS", "missing text", null)
                val voice = call.argument<String>("voice") ?: "en-us"
                try {
                    val phonemes = EspeakNative.phonemize(text, voice)
                    result.success(phonemes)
                } catch (e: Exception) {
                    result.error("PHONEMIZE_FAILED", e.message, null)
                }
            }
            else -> result.notImplemented()
        }
    }
}
