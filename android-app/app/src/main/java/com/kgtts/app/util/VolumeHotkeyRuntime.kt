package com.lhtstudio.kigtts.app.util

import android.content.Context
import android.os.SystemClock
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.overlay.FloatingOverlayService
import com.lhtstudio.kigtts.app.overlay.OverlayBridge

class VolumeHotkeySequenceDetector {
    private var pendingDirection = 0
    private var pendingDirectionAtMs = 0L

    fun reset() {
        pendingDirection = 0
        pendingDirectionAtMs = 0L
    }

    fun handleDirection(
        direction: Int,
        settings: UserPrefs.AppSettings,
        onTrigger: (VolumeHotkeyActionSpec) -> Unit
    ) {
        val now = SystemClock.uptimeMillis()
        val previousDirection = pendingDirection
        val windowMs = settings.volumeHotkeyWindowMs.toLong()
        if (
            previousDirection != 0 &&
            previousDirection != direction &&
            now - pendingDirectionAtMs <= windowMs
        ) {
            when {
                previousDirection > 0 && direction < 0 && settings.volumeHotkeyUpDownEnabled ->
                    onTrigger(settings.volumeHotkeyUpDownAction)

                previousDirection < 0 && direction > 0 && settings.volumeHotkeyDownUpEnabled ->
                    onTrigger(settings.volumeHotkeyDownUpAction)
            }
            reset()
            return
        }
        pendingDirection = direction
        pendingDirectionAtMs = now
    }
}

object VolumeHotkeyActionExecutor {
    fun hasEnabledHotkeys(settings: UserPrefs.AppSettings): Boolean {
        return settings.volumeHotkeyUpDownEnabled || settings.volumeHotkeyDownUpEnabled
    }

    fun execute(context: Context, action: VolumeHotkeyActionSpec) {
        when (action.kind) {
            VolumeHotkeyActions.KIND_INTERNAL -> executeInternalAction(context, action.target)
            VolumeHotkeyActions.KIND_OVERLAY -> executeOverlayAction(context, action.target)
            VolumeHotkeyActions.KIND_EXTERNAL -> {
                if (action.packageName.isNotBlank() && action.shortcutId.isNotBlank()) {
                    ExternalShortcutCatalog.launchChoice(
                        context,
                        ExternalShortcutChoice(
                            packageName = action.packageName,
                            className = action.className,
                            appLabel = action.appLabel,
                            shortcutId = action.shortcutId,
                            shortcutTitle = action.shortcutTitle
                        )
                    )
                }
            }
        }
    }

    private fun executeInternalAction(context: Context, target: String) {
        val appTarget = when (target) {
            VolumeHotkeyActions.TARGET_QUICK_CARD -> OverlayBridge.TARGET_OPEN_QUICK_CARD
            VolumeHotkeyActions.TARGET_DRAWING -> OverlayBridge.TARGET_OPEN_DRAWING
            VolumeHotkeyActions.TARGET_SOUNDBOARD -> OverlayBridge.TARGET_OPEN_SOUNDBOARD
            VolumeHotkeyActions.TARGET_VOICE_PACK -> OverlayBridge.TARGET_OPEN_VOICE_PACK
            VolumeHotkeyActions.TARGET_QR_SCANNER -> OverlayBridge.TARGET_OPEN_QR_SCANNER
            else -> OverlayBridge.TARGET_OPEN
        }
        context.startActivity(OverlayBridge.buildOpenPageIntent(context, appTarget))
    }

    private fun executeOverlayAction(context: Context, target: String) {
        if (!FloatingOverlayService.canDrawOverlays(context)) {
            context.startActivity(
                OverlayBridge.buildOpenPageIntent(context, OverlayBridge.TARGET_OPEN_OVERLAY)
            )
            return
        }
        when (target) {
            VolumeHotkeyActions.TARGET_OPEN_MINI_QUICK_SUBTITLE ->
                FloatingOverlayService.openMiniQuickSubtitle(context)

            VolumeHotkeyActions.TARGET_OPEN_MINI_QUICK_CARD ->
                FloatingOverlayService.openMiniQuickCard(context)

            else -> FloatingOverlayService.openPanel(context)
        }
    }
}
