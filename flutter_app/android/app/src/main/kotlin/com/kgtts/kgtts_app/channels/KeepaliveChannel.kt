package com.kgtts.kgtts_app.channels

import android.content.Context
import android.content.Intent
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.kgtts.kgtts_app.service.KeepAliveService

class KeepaliveChannel(
    flutterEngine: FlutterEngine,
    private val context: Context
) : MethodChannel.MethodCallHandler {

    private val channel = MethodChannel(
        flutterEngine.dartExecutor.binaryMessenger,
        "com.kgtts.app/keepalive"
    )

    init {
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "start" -> {
                try {
                    val intent = Intent(context, KeepAliveService::class.java)
                    context.startForegroundService(intent)
                    result.success(null)
                } catch (e: Exception) {
                    result.error("START_FAILED", e.message, null)
                }
            }
            "stop" -> {
                try {
                    val intent = Intent(context, KeepAliveService::class.java)
                    context.stopService(intent)
                    result.success(null)
                } catch (e: Exception) {
                    result.error("STOP_FAILED", e.message, null)
                }
            }
            else -> result.notImplemented()
        }
    }
}
