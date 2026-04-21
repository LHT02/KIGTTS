package com.lhtstudio.kigtts.app.util

import org.json.JSONObject

enum class VolumeHotkeySequence {
    UpDown,
    DownUp
}

data class VolumeHotkeyActionSpec(
    val kind: String,
    val target: String,
    val packageName: String = "",
    val className: String = "",
    val shortcutId: String = "",
    val appLabel: String = "",
    val shortcutTitle: String = ""
)

object VolumeHotkeyActions {
    const val KIND_INTERNAL = "internal"
    const val KIND_OVERLAY = "overlay"
    const val KIND_EXTERNAL = "external"

    const val TARGET_QUICK_SUBTITLE = "quick_subtitle"
    const val TARGET_QUICK_CARD = "quick_card"
    const val TARGET_DRAWING = "drawing"
    const val TARGET_SOUNDBOARD = "soundboard"
    const val TARGET_VOICE_PACK = "voice_pack"
    const val TARGET_QR_SCANNER = "qr_scanner"

    const val TARGET_OPEN_OVERLAY = "open_overlay"
    const val TARGET_OPEN_MINI_QUICK_SUBTITLE = "open_mini_quick_subtitle"
    const val TARGET_OPEN_MINI_QUICK_CARD = "open_mini_quick_card"

    val directOptions =
        listOf(
            internal(TARGET_QUICK_SUBTITLE),
            internal(TARGET_QUICK_CARD),
            internal(TARGET_DRAWING),
            internal(TARGET_SOUNDBOARD),
            internal(TARGET_VOICE_PACK),
            internal(TARGET_QR_SCANNER)
        )

    val overlayOptions =
        listOf(
            overlay(TARGET_OPEN_OVERLAY),
            overlay(TARGET_OPEN_MINI_QUICK_SUBTITLE),
            overlay(TARGET_OPEN_MINI_QUICK_CARD)
        )

    fun internal(target: String): VolumeHotkeyActionSpec =
        VolumeHotkeyActionSpec(kind = KIND_INTERNAL, target = target)

    fun overlay(target: String): VolumeHotkeyActionSpec =
        VolumeHotkeyActionSpec(kind = KIND_OVERLAY, target = target)

    fun external(choice: ExternalShortcutChoice): VolumeHotkeyActionSpec =
        VolumeHotkeyActionSpec(
            kind = KIND_EXTERNAL,
            target = choice.shortcutId,
            packageName = choice.packageName,
            className = choice.className,
            shortcutId = choice.shortcutId,
            appLabel = choice.appLabel,
            shortcutTitle = choice.shortcutTitle
        )

    fun defaultFor(sequence: VolumeHotkeySequence): VolumeHotkeyActionSpec =
        when (sequence) {
            VolumeHotkeySequence.UpDown -> internal(TARGET_QUICK_SUBTITLE)
            VolumeHotkeySequence.DownUp -> overlay(TARGET_OPEN_OVERLAY)
        }

    fun encode(action: VolumeHotkeyActionSpec): String {
        return JSONObject().apply {
            put("kind", action.kind)
            put("target", action.target)
            put("packageName", action.packageName)
            put("className", action.className)
            put("shortcutId", action.shortcutId)
            put("appLabel", action.appLabel)
            put("shortcutTitle", action.shortcutTitle)
        }.toString()
    }

    fun decode(raw: String?, fallback: VolumeHotkeyActionSpec): VolumeHotkeyActionSpec {
        if (raw.isNullOrBlank()) return fallback
        return runCatching {
            val obj = JSONObject(raw)
            VolumeHotkeyActionSpec(
                kind = obj.optString("kind", fallback.kind).ifBlank { fallback.kind },
                target = obj.optString("target", fallback.target).ifBlank { fallback.target },
                packageName = obj.optString("packageName", ""),
                className = obj.optString("className", ""),
                shortcutId = obj.optString("shortcutId", ""),
                appLabel = obj.optString("appLabel", ""),
                shortcutTitle = obj.optString("shortcutTitle", "")
            )
        }.getOrDefault(fallback)
    }

    fun labelOf(action: VolumeHotkeyActionSpec): String {
        return when (action.kind) {
            KIND_INTERNAL -> when (action.target) {
                TARGET_QUICK_SUBTITLE -> "快捷字幕"
                TARGET_QUICK_CARD -> "快捷名片"
                TARGET_DRAWING -> "画板"
                TARGET_SOUNDBOARD -> "音效板"
                TARGET_VOICE_PACK -> "语音包"
                TARGET_QR_SCANNER -> "扫一扫"
                else -> "未设置"
            }
            KIND_OVERLAY -> when (action.target) {
                TARGET_OPEN_OVERLAY -> "打开悬浮窗"
                TARGET_OPEN_MINI_QUICK_SUBTITLE -> "打开迷你快捷字幕"
                TARGET_OPEN_MINI_QUICK_CARD -> "打开迷你快捷名片"
                else -> "未设置"
            }
            KIND_EXTERNAL -> {
                val app = action.appLabel.ifBlank { action.packageName }
                val title = action.shortcutTitle.ifBlank { action.shortcutId }
                if (app.isBlank()) title else "$app · $title"
            }
            else -> "未设置"
        }
    }
}
