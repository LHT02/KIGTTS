package com.lhtstudio.kigtts.app.overlay

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.lhtstudio.kigtts.app.ui.MainActivity

object OverlayBridge {
    const val ACTION_OPEN_QUICK_SUBTITLE = "com.lhtstudio.kigtts.app.action.OPEN_QUICK_SUBTITLE"
    const val ACTION_REQUEST_RECORD_AUDIO_PERMISSION =
        "com.lhtstudio.kigtts.app.action.REQUEST_RECORD_AUDIO_PERMISSION"
    const val EXTRA_REQUEST_ID = "overlay_request_id"
    const val EXTRA_TARGET = "overlay_target"
    const val EXTRA_TEXT = "overlay_text"
    const val EXTRA_NAVIGATE_TO_PAGE = "overlay_navigate_to_page"
    const val EXTRA_START_REALTIME_ON_GRANT = "overlay_start_realtime_on_grant"

    const val TARGET_OPEN = "open"
    const val TARGET_SUBTITLE = "subtitle"
    const val TARGET_INPUT = "input"
    const val TARGET_OPEN_OVERLAY = "open_overlay"
    const val TARGET_OPEN_QUICK_CARD = "open_quick_card"
    const val TARGET_OPEN_DRAWING = "open_drawing"
    const val TARGET_OPEN_SOUNDBOARD = "open_soundboard"
    const val TARGET_OPEN_VOICE_PACK = "open_voice_pack"
    const val TARGET_OPEN_SETTINGS = "open_settings"
    const val TARGET_OPEN_QR_SCANNER = "open_qr_scanner"

    fun buildQuickSubtitleIntent(
        context: Context,
        target: String,
        text: String,
        navigateToPage: Boolean = true
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_QUICK_SUBTITLE
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(EXTRA_REQUEST_ID, SystemClock.uptimeMillis())
            putExtra(EXTRA_TARGET, target)
            putExtra(EXTRA_TEXT, text)
            putExtra(EXTRA_NAVIGATE_TO_PAGE, navigateToPage)
        }
    }

    fun buildOpenPageIntent(context: Context, target: String): Intent {
        return buildQuickSubtitleIntent(context, target, "")
    }

    fun buildRequestRecordAudioPermissionIntent(
        context: Context,
        startRealtimeOnGrant: Boolean = false
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_REQUEST_RECORD_AUDIO_PERMISSION
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(EXTRA_START_REALTIME_ON_GRANT, startRealtimeOnGrant)
        }
    }
}
