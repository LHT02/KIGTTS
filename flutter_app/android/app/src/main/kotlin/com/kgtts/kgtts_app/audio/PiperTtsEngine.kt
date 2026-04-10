package com.kgtts.kgtts_app.audio

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.kgtts.kgtts_app.data.EspeakData
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.roundToInt

class PiperTtsEngine(context: Context, packDir: java.io.File) : TtsModule {
    private val voicePack = PiperVoicePack(packDir).pack
    private val toIds: (String) -> IntArray
    init {
        toIds = if (voicePack.phonemeType.contains("espeak")) {
            val dataDir = EspeakData.ensure(context)
                ?: throw IllegalStateException("未找到 espeak-ng 数据")
            val voiceName = voicePack.espeakVoice
                .ifBlank { voicePack.languageCode }
                .ifBlank { "en-us" }
            val phonemizer = EspeakPhonemizer(
                dataDir,
                voiceName,
                voicePack.phonemeIdMap,
                voicePack.phonemeMap
            )
            phonemizer::toIds
        } else {
            val phonemizer = PiperPhonemizer(
                voicePack.dictPath,
                voicePack.phonemeIdMap,
                voicePack.phonemeMap
            )
            phonemizer::toIds
        }
    }
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(voicePack.modelPath.absolutePath, OrtSession.SessionOptions())
    override val sampleRate: Int = voicePack.sampleRate
    @Volatile private var noiseScale: Float = 0.667f
    @Volatile private var lengthScale: Float = 1.0f
    @Volatile private var noiseW: Float = 0.8f
    @Volatile private var sentenceSilenceSec: Float = 0.0f

    override fun setSynthesisTuning(
        noiseScale: Float,
        lengthScale: Float,
        noiseW: Float,
        sentenceSilenceSec: Float
    ) {
        this.noiseScale = noiseScale.coerceIn(0f, 2f)
        this.lengthScale = lengthScale.coerceIn(0.1f, 5f)
        this.noiseW = noiseW.coerceIn(0f, 2f)
        this.sentenceSilenceSec = sentenceSilenceSec.coerceIn(0f, 2f)
    }

    override fun synthesize(text: String): FloatArray {
        return synthesizeInternal(text, null)
    }

    override fun synthesize(text: String, sentenceSilenceSec: Float): FloatArray {
        return synthesizeInternal(text, sentenceSilenceSec.coerceAtLeast(0f))
    }

    private fun synthesizeInternal(text: String, sentenceSilenceOverride: Float?): FloatArray {
        val ids = toIds(text)
        if (ids.isEmpty()) return FloatArray(0)
        val currentNoiseScale = noiseScale
        val currentLengthScale = lengthScale
        val currentNoiseW = noiseW
        val currentSentenceSilenceSec = sentenceSilenceOverride ?: sentenceSilenceSec
        val inputs = mutableMapOf<String, OnnxTensor>()
        val idLong = ids.map { it.toLong() }.toLongArray()
        val inputName = session.inputNames.firstOrNull { it.contains("input") } ?: session.inputNames.first()
        val lenName = session.inputNames.firstOrNull { it.contains("len") || it.contains("length") } ?: inputName + "_length"
        inputs[inputName] = OnnxTensor.createTensor(env, LongBuffer.wrap(idLong), longArrayOf(1, idLong.size.toLong()))
        inputs[lenName] = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(idLong.size.toLong())), longArrayOf(1))

        val scaleName = session.inputNames.firstOrNull { it.contains("scale") }
        if (scaleName != null) {
            val scales = FloatBuffer.wrap(
                floatArrayOf(currentNoiseScale, currentLengthScale, currentNoiseW)
            )
            inputs[scaleName] = OnnxTensor.createTensor(env, scales, longArrayOf(3))
        }
        val sidName = session.inputNames.firstOrNull { it.contains("sid") }
        if (sidName != null) {
            inputs[sidName] = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(0)), longArrayOf(1))
        }

        session.run(inputs).use { results ->
            val raw = unwrapAudio(results[0].value)
            return appendSentenceSilence(raw, currentSentenceSilenceSec)
        }
    }

    private fun unwrapAudio(value: Any?): FloatArray {
        return when (value) {
            is FloatArray -> value
            is Array<*> -> {
                if (value.isNotEmpty()) unwrapAudio(value[0]) else FloatArray(0)
            }
            else -> FloatArray(0)
        }
    }

    private fun appendSentenceSilence(samples: FloatArray, sec: Float): FloatArray {
        val silenceSec = sec.coerceAtLeast(0f)
        if (silenceSec <= 0f || samples.isEmpty()) return samples
        val silenceSamples = (sampleRate * silenceSec).roundToInt()
        if (silenceSamples <= 0) return samples
        val out = FloatArray(samples.size + silenceSamples)
        System.arraycopy(samples, 0, out, 0, samples.size)
        return out
    }
}
