package com.kgtts.kgtts_app.audio

import com.kgtts.kgtts_app.util.AppLogger
import org.json.JSONObject
import java.io.File

data class VoicePack(
    val manifest: JSONObject,
    val modelPath: File,
    val configPath: File,
    val dictPath: File,
    val sampleRate: Int,
    val phonemeIdMap: Map<String, List<Int>>,
    val phonemeMap: Map<String, List<String>>,
    val phonemeType: String,
    val espeakVoice: String,
    val languageCode: String
)

class PiperVoicePack(private val dir: File) {
    val pack: VoicePack
    init {
        val manifestFile = File(dir, "manifest.json")
        val manifest = JSONObject(manifestFile.readText())
        val files = manifest.getJSONObject("files")
        val modelPath = File(dir, files.getString("model"))
        val configPath = File(dir, files.getString("config"))
        val dictPath = File(dir, files.getString("phonemizer"))
        val configJson = JSONObject(configPath.readText())
        val phonemeType = configJson.optString("phoneme_type", "text").lowercase()
        val espeakVoice = configJson.optJSONObject("espeak")?.optString("voice")?.trim().orEmpty()
        val languageCode = configJson.optJSONObject("language")?.optString("code")?.trim().orEmpty()
        val phonemeMap = configJson.getJSONObject("phoneme_id_map")
        val idMap = mutableMapOf<String, List<Int>>()
        phonemeMap.keys().forEach { key ->
            val raw = phonemeMap.get(key)
            val values = when (raw) {
                is org.json.JSONArray -> {
                    val list = mutableListOf<Int>()
                    for (i in 0 until raw.length()) {
                        list.add(raw.getInt(i))
                    }
                    list
                }
                is Number -> listOf(raw.toInt())
                else -> emptyList()
            }
            if (values.isNotEmpty()) {
                idMap[key] = values
            }
        }
        val rawPhoneMap = configJson.optJSONObject("phoneme_map")
        val phoneMap = mutableMapOf<String, List<String>>()
        rawPhoneMap?.keys()?.forEach { key ->
            val raw = rawPhoneMap.get(key)
            val values = when (raw) {
                is org.json.JSONArray -> {
                    val list = mutableListOf<String>()
                    for (i in 0 until raw.length()) {
                        list.add(raw.getString(i))
                    }
                    list
                }
                is String -> listOf(raw)
                else -> emptyList()
            }
            if (values.isNotEmpty()) {
                phoneMap[key] = values
            }
        }
        val sr = manifest.optInt("sample_rate", configJson.optInt("sample_rate", 22050))
        AppLogger.i("VoicePack init dir=${dir.absolutePath} model=${modelPath.absolutePath} sr=$sr")
        pack = VoicePack(
            manifest = manifest,
            modelPath = modelPath,
            configPath = configPath,
            dictPath = dictPath,
            sampleRate = sr,
            phonemeIdMap = idMap,
            phonemeMap = phoneMap,
            phonemeType = phonemeType,
            espeakVoice = espeakVoice,
            languageCode = languageCode
        )
    }
}
