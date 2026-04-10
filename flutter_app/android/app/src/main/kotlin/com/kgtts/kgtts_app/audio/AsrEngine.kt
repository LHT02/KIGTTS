package com.kgtts.kgtts_app.audio

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.kgtts.kgtts_app.util.AppLogger
import java.io.File

class AsrEngine(private val context: Context, private val modelDir: File) : AsrModule {
    private val recognizer: OfflineRecognizer
    override val sampleRate: Int = 16000

    init {
        val onnxFiles = modelDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() == "onnx" }
            .toList()
        val modelPath = chooseSenseVoiceModel(onnxFiles)
            ?: throw IllegalArgumentException("未在 ${modelDir.absolutePath} 找到 sensevoice onnx 模型")
        val lang = listOf("language.txt", "lang.txt").map { File(modelDir, it) }.firstOrNull { it.exists() }
            ?.readText()?.trim().orEmpty().ifEmpty { "zh" }
        AppLogger.i("ASR init model=$modelPath lang=$lang")

        val feat = FeatureConfig().apply {
            sampleRate = this@AsrEngine.sampleRate
            featureDim = 80
            dither = 0f
        }
        val senseVoiceCfg = OfflineSenseVoiceModelConfig().apply {
            model = modelPath.absolutePath
            language = lang
            useInverseTextNormalization = true
        }
        val tokensPath = File(modelPath.parentFile, "tokens.txt")
        val modelCfg = OfflineModelConfig().apply {
            senseVoice = senseVoiceCfg
            if (tokensPath.exists()) {
                tokens = tokensPath.absolutePath
            }
            modelType = "sense_voice"
            numThreads = 2
            provider = "cpu"
        }
        val recCfg = OfflineRecognizerConfig().apply {
            featConfig = feat
            modelConfig = modelCfg
            decodingMethod = "greedy_search"
            maxActivePaths = 4
            blankPenalty = 0f
        }
        // Use filesystem paths (not assets) for extracted models.
        recognizer = OfflineRecognizer(null, recCfg)
    }

    private fun chooseSenseVoiceModel(onnxFiles: List<File>): File? {
        if (onnxFiles.isEmpty()) return null
        fun isSenseVoice(file: File): Boolean {
            val name = file.name.lowercase()
            if (name.contains("sensevoice")) return true
            val p1 = file.parentFile?.name?.lowercase().orEmpty()
            val p2 = file.parentFile?.parentFile?.name?.lowercase().orEmpty()
            return p1.contains("sensevoice") || p2.contains("sensevoice")
        }
        fun isAux(file: File): Boolean {
            val name = file.name.lowercase()
            return name.contains("punct") || name.contains("vad") || name.contains("silero")
        }
        return onnxFiles.firstOrNull { isSenseVoice(it) }
            ?: onnxFiles.firstOrNull { !isAux(it) }
            ?: onnxFiles.firstOrNull()
    }

    override fun transcribe(samples: FloatArray, sr: Int): String {
        val stream = recognizer.createStream()
        stream.acceptWaveform(samples, sr)
        recognizer.decode(stream)
        val result = recognizer.getResult(stream)
        val text = result?.text ?: ""
        stream.release()
        return text
    }
}
