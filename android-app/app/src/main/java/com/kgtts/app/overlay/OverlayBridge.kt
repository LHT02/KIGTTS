package com.kgtts.app.overlay

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.kgtts.app.ui.MainActivity

object OverlayBridge {
    const val ACTION_OPEN_QUICK_SUBTITLE = "com.kgtts.app.action.OPEN_QUICK_SUBTITLE"
    const val EXTRA_REQUEST_ID = "overlay_request_id"
    const val EXTRA_TARGET = "overlay_target"
    const val EXTRA_TEXT = "overlay_text"

    const val TARGET_OPEN = "open"
    const val TARGET_SUBTITLE = "subtitle"
    const val TARGET_INPUT = "input"
    const val TARGET_OPEN_OVERLAY = "open_overlay"
    const val TARGET_OPEN_QUICK_CARD = "open_quick_card"
    const val TARGET_OPEN_DRAWING = "open_drawing"
    const val TARGET_OPEN_SETTINGS = "open_settings"
    const val TARGET_OPEN_QR_SCANNER = "open_qr_scanner"

    fun buildQuickSubtitleIntent(
        context: Context,
        target: String,
        text: String
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
        }
    }

    fun buildOpenPageIntent(context: Context, target: String): Intent {
        return buildQuickSubtitleIntent(context, target, "")
    }
}
