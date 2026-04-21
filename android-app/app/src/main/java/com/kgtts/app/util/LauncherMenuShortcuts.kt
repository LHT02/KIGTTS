package com.lhtstudio.kigtts.app.util

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.lhtstudio.kigtts.app.R
import com.lhtstudio.kigtts.app.overlay.OverlayBridge

object LauncherMenuShortcuts {
    private data class AppShortcutSpec(
        val id: String,
        val shortLabel: String,
        val longLabel: String,
        val target: String
    )

    private val appShortcuts =
        listOf(
            AppShortcutSpec(
                id = "app_quick_subtitle",
                shortLabel = "便捷字幕",
                longLabel = "打开便捷字幕",
                target = OverlayBridge.TARGET_OPEN
            ),
            AppShortcutSpec(
                id = "app_quick_card",
                shortLabel = "快捷名片",
                longLabel = "打开快捷名片",
                target = OverlayBridge.TARGET_OPEN_QUICK_CARD
            ),
            AppShortcutSpec(
                id = "app_drawing",
                shortLabel = "画板",
                longLabel = "打开画板",
                target = OverlayBridge.TARGET_OPEN_DRAWING
            ),
            AppShortcutSpec(
                id = "app_soundboard",
                shortLabel = "音效板",
                longLabel = "打开音效板",
                target = OverlayBridge.TARGET_OPEN_SOUNDBOARD
            )
        )

    suspend fun syncAppShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        runCatching {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
            val maxCount =
                shortcutManager.maxShortcutCountPerActivity.takeIf { it > 0 } ?: appShortcuts.size
            shortcutManager.dynamicShortcuts =
                appShortcuts
                    .take(maxCount)
                    .map { spec ->
                        ShortcutInfo.Builder(context, spec.id)
                            .setShortLabel(spec.shortLabel)
                            .setLongLabel(spec.longLabel)
                            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher_round))
                            .setIntent(OverlayBridge.buildOpenPageIntent(context, spec.target))
                            .build()
                    }
        }.onFailure {
            AppLogger.e("LauncherMenuShortcuts.syncAppShortcuts failed", it)
        }
    }
}
