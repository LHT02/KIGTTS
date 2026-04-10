package com.kgtts.kgtts_app.audio

import android.util.Log

object EspeakNative {
    private var loaded = false
    private var initialized = false

    init {
        try {
            System.loadLibrary("espeak_jni")
            loaded = true
        } catch (e: Throwable) {
            Log.e("EspeakNative", "Failed to load espeak_jni", e)
        }
    }

    private external fun nativeInit(dataPath: String): Boolean
    private external fun nativePhonemize(text: String, voice: String): String

    fun ensureInit(dataPath: String): Boolean {
        if (!loaded) return false
        if (initialized) return true
        initialized = nativeInit(dataPath)
        return initialized
    }

    fun phonemize(text: String, voice: String): String {
        if (!loaded || !initialized) return ""
        return nativePhonemize(text, voice)
    }
}
