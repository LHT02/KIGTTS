package com.kgtts.kgtts_app.channels

import android.content.Context
import android.content.Intent
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class OverlayChannel(
    flutterEngine: FlutterEngine,
    private val context: Context
) : MethodChannel.MethodCallHandler {

    private val channel = MethodChannel(
        flutterEngine.dartExecutor.binaryMessenger,
        "com.kgtts.app/overlay"
    )

    init {
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "show" -> {
                // TODO: Start FloatingOverlayService
                result.success(null)
            }
            "hide" -> {
                // TODO: Stop FloatingOverlayService
                result.success(null)
            }
            "isShowing" -> {
                result.success(false) // TODO: Check service state
            }
            "updateConfig" -> {
                result.success(null) // TODO: Update overlay config
            }
            else -> result.notImplemented()
        }
    }
}
