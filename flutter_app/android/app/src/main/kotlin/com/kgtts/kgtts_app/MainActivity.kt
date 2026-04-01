package com.kgtts.kgtts_app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import com.kgtts.kgtts_app.channels.*
import com.kgtts.kgtts_app.util.AppLogger

class MainActivity : FlutterActivity() {
    private var realtimeChannel: RealtimeChannel? = null
    private var modelChannel: ModelChannel? = null
    private var overlayChannel: OverlayChannel? = null
    private var keepaliveChannel: KeepaliveChannel? = null
    private var espeakChannel: EspeakChannel? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        AppLogger.init(applicationContext)
        realtimeChannel = RealtimeChannel(flutterEngine, applicationContext)
        modelChannel = ModelChannel(flutterEngine, applicationContext)
        overlayChannel = OverlayChannel(flutterEngine, applicationContext)
        keepaliveChannel = KeepaliveChannel(flutterEngine, applicationContext)
        espeakChannel = EspeakChannel(flutterEngine, applicationContext)
    }

    override fun cleanUpFlutterEngine(flutterEngine: FlutterEngine) {
        realtimeChannel?.dispose()
        modelChannel?.dispose()
        super.cleanUpFlutterEngine(flutterEngine)
    }
}
