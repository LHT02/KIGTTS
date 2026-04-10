package com.kgtts.kgtts_app.audio

import android.content.Context
import java.io.File

interface AsrModule {
    val sampleRate: Int
    fun transcribe(samples: FloatArray, sr: Int): String
}

interface TtsModule {
    val sampleRate: Int
    fun synthesize(text: String): FloatArray
    fun synthesize(text: String, sentenceSilenceSec: Float): FloatArray = synthesize(text)
    fun setSynthesisTuning(
        noiseScale: Float,
        lengthScale: Float,
        noiseW: Float,
        sentenceSilenceSec: Float
    ) {}
}

interface SpeechModuleFactory {
    fun createAsr(context: Context, modelDir: File): AsrModule
    fun createTts(context: Context, packDir: File): TtsModule
}

object DefaultSpeechModuleFactory : SpeechModuleFactory {
    override fun createAsr(context: Context, modelDir: File): AsrModule = AsrEngine(context, modelDir)
    override fun createTts(context: Context, packDir: File): TtsModule = PiperTtsEngine(context, packDir)
}
