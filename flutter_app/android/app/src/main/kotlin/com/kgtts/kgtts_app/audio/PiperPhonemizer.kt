package com.kgtts.kgtts_app.audio

import java.io.File

internal fun buildIds(phones: List<String>, idMap: Map<String, List<Int>>): IntArray {
    val ids = mutableListOf<Int>()
    val bos = idMap["^"] ?: emptyList()
    val eos = idMap["$"] ?: emptyList()
    val pad = idMap["_"] ?: emptyList()
    ids.addAll(bos)
    if (pad.isNotEmpty()) {
        ids.addAll(pad)
    }
    for (phone in phones) {
        val mapped = idMap[phone] ?: continue
        ids.addAll(mapped)
        if (pad.isNotEmpty()) {
            ids.addAll(pad)
        }
    }
    ids.addAll(eos)
    return ids.toIntArray()
}

class PiperPhonemizer(
    dictFile: File,
    private val idMap: Map<String, List<Int>>,
    private val phoneMap: Map<String, List<String>>
) {
    private val charToPhones: Map<String, List<String>> = loadDict(dictFile)
    private fun loadDict(file: File): Map<String, List<String>> {
        if (!file.exists()) return emptyMap()
        val map = mutableMapOf<String, List<String>>()
        file.useLines { lines ->
            lines.forEach { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 2) {
                    map[parts[0]] = parts.drop(1)
                }
            }
        }
        return map
    }

    private fun applyPhoneMap(phones: List<String>): List<String> {
        if (phoneMap.isEmpty()) return phones
        val out = mutableListOf<String>()
        for (phone in phones) {
            val mapped = phoneMap[phone]
            if (mapped != null && mapped.isNotEmpty()) {
                out.addAll(mapped)
            } else {
                out.add(phone)
            }
        }
        return out
    }

    fun toIds(text: String): IntArray {
        val phones = mutableListOf<String>()
        text.forEach { ch ->
            val key = ch.toString()
            val entry = charToPhones[key]
            if (entry != null) {
                phones.addAll(entry)
            } else {
                phones.add(key)
            }
        }
        val mappedPhones = applyPhoneMap(phones)
        return buildIds(mappedPhones, idMap)
    }
}

class EspeakPhonemizer(
    private val dataDir: File,
    private val voice: String,
    private val idMap: Map<String, List<Int>>,
    private val phoneMap: Map<String, List<String>>
) {
    init {
        if (!EspeakNative.ensureInit(dataDir.absolutePath)) {
            throw IllegalStateException("espeak-ng 初始化失败")
        }
    }

    private fun applyPhoneMap(phones: List<String>): List<String> {
        if (phoneMap.isEmpty()) return phones
        val out = mutableListOf<String>()
        for (phone in phones) {
            val mapped = phoneMap[phone]
            if (mapped != null && mapped.isNotEmpty()) {
                out.addAll(mapped)
            } else {
                out.add(phone)
            }
        }
        return out
    }

    fun toIds(text: String): IntArray {
        val phonemes = EspeakNative.phonemize(text, voice)
        if (phonemes.isBlank()) return IntArray(0)
        val phones = mutableListOf<String>()
        val cps = phonemes.codePoints().toArray()
        for (cp in cps) {
            phones.add(String(Character.toChars(cp)))
        }
        val mappedPhones = applyPhoneMap(phones)
        return buildIds(mappedPhones, idMap)
    }
}
