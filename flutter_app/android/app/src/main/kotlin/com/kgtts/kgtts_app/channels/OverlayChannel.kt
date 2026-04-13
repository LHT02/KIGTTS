package com.kgtts.kgtts_app.channels

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.kgtts.kgtts_app.overlay.FloatingOverlayService
import com.kgtts.kgtts_app.service.RealtimeHostService

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
        runCatching {
            when (call.method) {
                "show" -> {
                    if (!FloatingOverlayService.canDrawOverlays(context)) {
                        result.error(
                            "OVERLAY_PERMISSION",
                            "Overlay permission is not granted",
                            null,
                        )
                        return
                    }
                    RealtimeHostService.ensureStarted(context)
                    FloatingOverlayService.show(context)
                    result.success(null)
                }

                "hide" -> {
                    FloatingOverlayService.hide(context)
                    result.success(null)
                }

                "isShowing" -> {
                    result.success(FloatingOverlayService.showing())
                }

                "canDrawOverlays" -> {
                    result.success(FloatingOverlayService.canDrawOverlays(context))
                }

                "openOverlayPermissionSettings" -> {
                    FloatingOverlayService.openOverlayPermissionSettings(context)
                    result.success(null)
                }

                "updateConfig" -> {
                    @Suppress("UNCHECKED_CAST")
                    val args = call.arguments as? Map<String, Any?> ?: emptyMap()
                    FloatingOverlayService.updateConfig(context, args)
                    result.success(null)
                }

                else -> result.notImplemented()
            }
        }.onFailure {
            result.error("OVERLAY_CHANNEL_FAILED", it.message, null)
        }
    }
}
