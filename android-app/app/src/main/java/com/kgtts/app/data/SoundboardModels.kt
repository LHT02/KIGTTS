package com.lhtstudio.kigtts.app.data

import org.json.JSONArray
import org.json.JSONObject

enum class SoundboardLayoutMode(
    val wireValue: String,
    val columns: Int,
    val label: String
) {
    List("list", 1, "列表"),
    Grid3("grid_3", 3, "三列宫格"),
    Grid4("grid_4", 4, "四列宫格"),
    Grid5("grid_5", 5, "五列宫格"),
    Grid6("grid_6", 6, "六列宫格");

    companion object {
        fun fromWire(raw: String?): SoundboardLayoutMode {
            return entries.firstOrNull { it.wireValue == raw } ?: Grid3
        }
    }
}

data class SoundboardItem(
    val id: Long,
    val title: String,
    val wakeWord: String = "",
    val audioPath: String = "",
    val durationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L
)

data class SoundboardGroup(
    val id: Long,
    val title: String,
    val icon: String,
    val items: List<SoundboardItem>
)

data class SoundboardConfig(
    val groups: List<SoundboardGroup>,
    val selectedGroupId: Long,
    val portraitLayout: SoundboardLayoutMode,
    val landscapeLayout: SoundboardLayoutMode
)

fun defaultSoundboardGroups(): List<SoundboardGroup> = listOf(
    SoundboardGroup(
        id = 1L,
        title = "常用音效",
        icon = "music_note",
        items = emptyList()
    )
)

fun defaultSoundboardConfig(): SoundboardConfig {
    val groups = defaultSoundboardGroups()
    return SoundboardConfig(
        groups = groups,
        selectedGroupId = groups.first().id,
        portraitLayout = SoundboardLayoutMode.Grid3,
        landscapeLayout = SoundboardLayoutMode.Grid5
    )
}

fun parseSoundboardConfig(raw: String?): SoundboardConfig {
    val source = raw?.trim().orEmpty()
    if (source.isEmpty()) return defaultSoundboardConfig()
    return runCatching {
        val root = JSONObject(source)
        val parsedGroups = mutableListOf<SoundboardGroup>()
        val arr = root.optJSONArray("groups") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val itemsArr = obj.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<SoundboardItem>()
            for (j in 0 until itemsArr.length()) {
                val itemObj = itemsArr.optJSONObject(j) ?: continue
                items += SoundboardItem(
                    id = itemObj.optLong("id", System.nanoTime()),
                    title = itemObj.optString("title", "新音效").ifBlank { "新音效" },
                    wakeWord = itemObj.optString("wakeWord", "").trim(),
                    audioPath = itemObj.optString("audioPath", "").trim(),
                    durationMs = itemObj.optLong("durationMs", 0L).coerceAtLeast(0L),
                    trimStartMs = itemObj.optLong("trimStartMs", 0L).coerceAtLeast(0L),
                    trimEndMs = itemObj.optLong("trimEndMs", 0L).coerceAtLeast(0L)
                )
            }
            parsedGroups += SoundboardGroup(
                id = obj.optLong("id", i.toLong() + 1L),
                title = obj.optString("title", "未命名分组").ifBlank { "未命名分组" },
                icon = obj.optString("icon", "music_note").ifBlank { "music_note" },
                items = items
            )
        }
        val groups = parsedGroups.ifEmpty { defaultSoundboardGroups() }
        val selectedId = root.optLong("selectedGroupId", groups.first().id)
        SoundboardConfig(
            groups = groups,
            selectedGroupId = groups.firstOrNull { it.id == selectedId }?.id ?: groups.first().id,
            portraitLayout = SoundboardLayoutMode.fromWire(root.optString("portraitLayout")),
            landscapeLayout = SoundboardLayoutMode.fromWire(root.optString("landscapeLayout"))
        )
    }.getOrElse { defaultSoundboardConfig() }
}

fun serializeSoundboardConfig(config: SoundboardConfig): String {
    return JSONObject().apply {
        put("selectedGroupId", config.selectedGroupId)
        put("portraitLayout", config.portraitLayout.wireValue)
        put("landscapeLayout", config.landscapeLayout.wireValue)
        put(
            "groups",
            JSONArray().apply {
                config.groups.forEach { group ->
                    put(
                        JSONObject().apply {
                            put("id", group.id)
                            put("title", group.title)
                            put("icon", group.icon)
                            put(
                                "items",
                                JSONArray().apply {
                                    group.items.forEach { item ->
                                        put(
                                            JSONObject().apply {
                                                put("id", item.id)
                                                put("title", item.title)
                                                put("wakeWord", item.wakeWord)
                                                put("audioPath", item.audioPath)
                                                put("durationMs", item.durationMs)
                                                put("trimStartMs", item.trimStartMs)
                                                put("trimEndMs", item.trimEndMs)
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    )
                }
            }
        )
    }.toString()
}

fun uniqueImportedGroupTitle(baseTitle: String, existingTitles: Collection<String>): String {
    val trimmed = baseTitle.trim().ifBlank { "未命名分组" }
    if (trimmed !in existingTitles) return trimmed
    var index = 2
    while (true) {
        val candidate = "$trimmed ($index)"
        if (candidate !in existingTitles) return candidate
        index += 1
    }
}
