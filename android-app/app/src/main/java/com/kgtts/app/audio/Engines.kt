package com.lhtstudio.kigtts.app.audio

import android.content.Context
import android.content.Intent
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.k2fsa.sherpa.onnx.DenoisedAudio
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiser
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserDpdfNetModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserGtcrnModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OnlineSpeechDenoiser
import com.k2fsa.sherpa.onnx.OnlineSpeechDenoiserConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.lhtstudio.kigtts.app.data.EspeakData
import com.lhtstudio.kigtts.app.data.isSystemTtsVoiceDir
import com.lhtstudio.kigtts.app.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.Locale

object AudioRoutePreference {
    const val INPUT_AUTO = 0
    const val INPUT_BUILTIN_MIC = 1
    const val INPUT_USB = 2
    const val INPUT_BLUETOOTH = 3
    const val INPUT_WIRED = 4

    const val OUTPUT_AUTO = 100
    const val OUTPUT_SPEAKER = 101
    const val OUTPUT_EARPIECE = 102
    const val OUTPUT_BLUETOOTH = 103
    const val OUTPUT_USB = 104
    const val OUTPUT_WIRED = 105
}

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
    override fun createTts(context: Context, packDir: File): TtsModule {
        return if (isSystemTtsVoiceDir(packDir)) {
            SystemTtsEngine(context)
        } else {
            PiperTtsEngine(context, packDir)
        }
    }
}

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

private fun buildIds(phones: List<String>, idMap: Map<String, List<Int>>): IntArray {
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

class PiperTtsEngine(context: Context, packDir: File) : TtsModule {
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

private data class PendingSystemUtterance(
    val file: File,
    val doneLatch: CountDownLatch = CountDownLatch(1),
    @Volatile var success: Boolean = false
)

class SystemTtsEngine(context: Context) : TtsModule {
    companion object {
        private const val INIT_STATUS_PENDING = Int.MIN_VALUE
        private const val INIT_TIMEOUT_MS = 4000L
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val initLatch = CountDownLatch(1)
    private val synthLock = Any()
    private val pendingUtterances = ConcurrentHashMap<String, PendingSystemUtterance>()
    private val initStatus = AtomicInteger(INIT_STATUS_PENDING)
    private val initFinalized = AtomicBoolean(false)
    @Volatile private var initSuccess = false
    @Volatile private var sentenceSilenceSec: Float = 0.0f
    @Volatile private var speechRate: Float = 1.0f
    @Volatile private var currentSampleRate: Int = 22050
    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var selectedEnginePackage: String? = null

    init {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            initializeOnMainThread()
        } else {
            val posted = mainHandler.post {
                initializeOnMainThread()
            }
            if (!posted) {
                initLatch.countDown()
                throw IllegalStateException("系统 TTS 初始化失败")
            }
        }
        waitForInit()
    }

    override val sampleRate: Int
        get() = currentSampleRate

    override fun setSynthesisTuning(
        noiseScale: Float,
        lengthScale: Float,
        noiseW: Float,
        sentenceSilenceSec: Float
    ) {
        this.sentenceSilenceSec = sentenceSilenceSec.coerceIn(0f, 2f)
        this.speechRate = (1f / lengthScale.coerceIn(0.1f, 5f)).coerceIn(0.2f, 4f)
    }

    override fun synthesize(text: String): FloatArray = synthesizeInternal(text, null)

    override fun synthesize(text: String, sentenceSilenceSec: Float): FloatArray {
        return synthesizeInternal(text, sentenceSilenceSec.coerceAtLeast(0f))
    }

    private fun synthesizeInternal(text: String, sentenceSilenceOverride: Float?): FloatArray {
        val content = text.trim()
        if (content.isEmpty()) return FloatArray(0)
        waitForInit()
        val currentTts = tts ?: throw IllegalStateException("系统 TTS 不可用")
        synchronized(synthLock) {
            currentTts.setSpeechRate(speechRate)
            val outFile = File.createTempFile("system_tts_", ".wav", appContext.cacheDir)
            val utteranceId = "system-tts-${System.nanoTime()}"
            val pending = PendingSystemUtterance(outFile)
            pendingUtterances[utteranceId] = pending
            val result = currentTts.synthesizeToFile(content, Bundle(), outFile, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                pendingUtterances.remove(utteranceId)
                outFile.delete()
                throw IllegalStateException("系统 TTS 合成失败")
            }
            if (!pending.doneLatch.await(20, TimeUnit.SECONDS) || !pending.success) {
                pendingUtterances.remove(utteranceId)
                outFile.delete()
                throw IllegalStateException("系统 TTS 合成超时")
            }
            val (sr, samples) = readWavToMonoFloat(outFile)
            outFile.delete()
            currentSampleRate = sr
            val silenceSec = sentenceSilenceOverride ?: sentenceSilenceSec
            return appendSentenceSilence(samples, silenceSec, sr)
        }
    }

    private fun waitForInit() {
        if (!initLatch.await(5, TimeUnit.SECONDS) || !initSuccess) {
            throw IllegalStateException("系统 TTS 初始化失败")
        }
    }

    private fun initializeOnMainThread() {
        if (!initFinalized.compareAndSet(false, true)) return
        try {
            val candidates = buildEngineCandidates()
            AppLogger.i(
                "SystemTtsEngine candidates=" +
                    candidates.joinToString(prefix = "[", postfix = "]") { it ?: "<default>" }
            )
            tryCreateEngineAsync(candidates, 0)
        } catch (e: Throwable) {
            finishInit(false)
            AppLogger.e("SystemTtsEngine create failed", e)
        }
    }

    private fun buildEngineCandidates(): List<String?> {
        val candidates = LinkedHashSet<String?>()
        val configured = runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
        if (configured != null) {
            candidates += configured
        }
        candidates += null
        val services = runCatching {
            @Suppress("DEPRECATION")
            appContext.packageManager.queryIntentServices(
                Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),
                0
            )
        }.getOrNull().orEmpty()
        services.mapNotNullTo(candidates) { it.serviceInfo?.packageName?.takeIf(String::isNotBlank) }
        return candidates.toList()
    }

    private fun finishInit(success: Boolean) {
        if (initLatch.count == 0L) return
        initSuccess = success
        initLatch.countDown()
    }

    private fun tryCreateEngineAsync(candidates: List<String?>, index: Int) {
        if (index >= candidates.size) {
            finishInit(false)
            return
        }
        val enginePackage = candidates[index]
        val finished = AtomicBoolean(false)
        var instance: TextToSpeech? = null
        lateinit var timeoutRunnable: Runnable

        fun tryNextOrFinish() {
            tryCreateEngineAsync(candidates, index + 1)
        }

        fun handleResult(status: Int) {
            if (!finished.compareAndSet(false, true)) return
            mainHandler.removeCallbacks(timeoutRunnable)
            val currentInstance = instance
            if (status != TextToSpeech.SUCCESS || currentInstance == null) {
                AppLogger.e("SystemTtsEngine init status=$status engine=${enginePackage ?: "<default>"}")
                currentInstance?.shutdown()
                tryNextOrFinish()
                return
            }
            selectedEnginePackage = enginePackage
            initStatus.set(status)
            tts = currentInstance
            val configured = configureInitializedEngine(currentInstance)
            if (configured) {
                AppLogger.i(
                    "SystemTtsEngine init success engine=${selectedEnginePackage ?: "<default>"}"
                )
                finishInit(true)
            } else {
                currentInstance.shutdown()
                tts = null
                tryNextOrFinish()
            }
        }

        timeoutRunnable = Runnable {
            if (!finished.compareAndSet(false, true)) return@Runnable
            AppLogger.e("SystemTtsEngine init timeout engine=${enginePackage ?: "<default>"}")
            instance?.shutdown()
            tryNextOrFinish()
        }

        val statusCallback: (Int) -> Unit = { status ->
            mainHandler.post {
                if (instance == null) {
                    mainHandler.post { handleResult(status) }
                } else {
                    handleResult(status)
                }
            }
        }

        try {
            instance = if (enginePackage.isNullOrBlank()) {
                TextToSpeech(appContext) { status ->
                    statusCallback(status)
                }
            } else {
                TextToSpeech(appContext, { status ->
                    statusCallback(status)
                }, enginePackage)
            }
            mainHandler.postDelayed(timeoutRunnable, INIT_TIMEOUT_MS)
        } catch (e: Throwable) {
            AppLogger.e(
                "SystemTtsEngine create failed engine=${enginePackage ?: "<default>"}",
                e
            )
            tryNextOrFinish()
        }
    }

    private fun configureInitializedEngine(currentTts: TextToSpeech): Boolean {
        return try {
            val targetLocale = Locale.getDefault()
            if (currentTts.isLanguageAvailable(targetLocale) >= TextToSpeech.LANG_AVAILABLE) {
                currentTts.language = targetLocale
            }
            currentTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    utteranceId?.let { id ->
                        pendingUtterances.remove(id)?.apply {
                            success = true
                            doneLatch.countDown()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    utteranceId?.let { id ->
                        pendingUtterances.remove(id)?.doneLatch?.countDown()
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    utteranceId?.let { id ->
                        pendingUtterances.remove(id)?.doneLatch?.countDown()
                    }
                }
            })
            true
        } catch (e: Throwable) {
            AppLogger.e("SystemTtsEngine init failed", e)
            false
        }
    }

    private fun appendSentenceSilence(samples: FloatArray, sec: Float, sampleRate: Int): FloatArray {
        val silenceSec = sec.coerceAtLeast(0f)
        if (silenceSec <= 0f || samples.isEmpty()) return samples
        val silenceSamples = (sampleRate * silenceSec).roundToInt()
        if (silenceSamples <= 0) return samples
        val out = FloatArray(samples.size + silenceSamples)
        System.arraycopy(samples, 0, out, 0, samples.size)
        return out
    }

    private fun readWavToMonoFloat(file: File): Pair<Int, FloatArray> {
        val bytes = file.readBytes()
        fun leInt(offset: Int): Int {
            return (bytes[offset].toInt() and 0xff) or
                    ((bytes[offset + 1].toInt() and 0xff) shl 8) or
                    ((bytes[offset + 2].toInt() and 0xff) shl 16) or
                    ((bytes[offset + 3].toInt() and 0xff) shl 24)
        }
        fun leShort(offset: Int): Int {
            return (bytes[offset].toInt() and 0xff) or
                    ((bytes[offset + 1].toInt() and 0xff) shl 8)
        }

        if (bytes.size < 44 || String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") {
            throw IllegalStateException("系统 TTS 输出格式不支持")
        }

        var offset = 12
        var sampleRate = 22050
        var channels = 1
        var bitsPerSample = 16
        var format = 1
        var dataOffset = -1
        var dataSize = 0
        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4)
            val chunkSize = leInt(offset + 4)
            val chunkData = offset + 8
            if (chunkData + chunkSize > bytes.size) break
            when (chunkId) {
                "fmt " -> {
                    format = leShort(chunkData)
                    channels = leShort(chunkData + 2).coerceAtLeast(1)
                    sampleRate = leInt(chunkData + 4).coerceAtLeast(8000)
                    bitsPerSample = leShort(chunkData + 14)
                }
                "data" -> {
                    dataOffset = chunkData
                    dataSize = chunkSize
                }
            }
            offset = chunkData + chunkSize + (chunkSize and 1)
        }
        if (dataOffset < 0 || dataSize <= 0) {
            throw IllegalStateException("系统 TTS 输出无音频数据")
        }
        if (format != 1 || bitsPerSample != 16) {
            throw IllegalStateException("系统 TTS 输出格式不支持")
        }
        val frameCount = dataSize / (channels * 2)
        val out = FloatArray(frameCount)
        var cursor = dataOffset
        for (i in 0 until frameCount) {
            var mixed = 0f
            repeat(channels) {
                val sample = leShort(cursor)
                val signed = if (sample >= 0x8000) sample - 0x10000 else sample
                mixed += (signed / 32768f)
                cursor += 2
            }
            out[i] = (mixed / channels.toFloat()).coerceIn(-1f, 1f)
        }
        return sampleRate to out
    }
}

class AudioPlayer(private val context: Context) {
    @Volatile var isPlaying: Boolean = false
        private set
    @Volatile private var useCommunicationAttributes: Boolean = false
    @Volatile private var preferredOutputType: Int = AudioRoutePreference.OUTPUT_AUTO
    @Volatile private var playbackGain: Float = 1.0f
    private var onOutputDevice: ((String) -> Unit)? = null
    private var onRender: ((FloatArray, Int, Int, Int) -> Unit)? = null

    fun setOnOutputDevice(callback: ((String) -> Unit)?) {
        onOutputDevice = callback
    }

    fun setOnRender(callback: ((FloatArray, Int, Int, Int) -> Unit)?) {
        onRender = callback
    }

    fun setUseCommunicationAttributes(enabled: Boolean) {
        useCommunicationAttributes = enabled
    }

    fun setPreferredOutputType(type: Int) {
        preferredOutputType = type
    }

    fun setPlaybackGainPercent(percent: Int) {
        playbackGain = (percent.coerceIn(0, 1000) / 100.0f).coerceAtLeast(0f)
    }

    fun play(samples: FloatArray, sampleRate: Int, onProgress: ((Float) -> Unit)? = null) {
        if (samples.isEmpty()) return
        val gain = playbackGain
        val scaledSamples = if (kotlin.math.abs(gain - 1.0f) < 0.0001f) {
            samples
        } else {
            FloatArray(samples.size) { idx ->
                (samples[idx] * gain).coerceIn(-1f, 1f)
            }
        }
        val shorts = ShortArray(scaledSamples.size) { idx ->
            val v = max(-1f, min(1f, scaledSamples[idx])) * Short.MAX_VALUE
            v.toInt().toShort()
        }
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val usage = if (useCommunicationAttributes) {
            AudioAttributes.USAGE_VOICE_COMMUNICATION
        } else {
            AudioAttributes.USAGE_MEDIA
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(minBuf, 4096))
            .build()

        val audioManager = context.getSystemService(AudioManager::class.java)
        if (audioManager != null && Build.VERSION.SDK_INT >= 23) {
            try {
                val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val preferred = pickPreferredOutputDevice(outputs, preferredOutputType)
                if (preferred != null) {
                    val ok = track.setPreferredDevice(preferred)
                    AppLogger.i("Prefer output device: ${formatOutputDeviceLabel(preferred)} result=$ok")
                }
            } catch (e: Exception) {
                AppLogger.e("Prefer output device failed", e)
            }
        }

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                track.addOnRoutingChangedListener({ routing ->
                    val device = routing.routedDevice
                    onOutputDevice?.invoke(formatOutputDeviceLabel(device))
                }, null)
            } catch (_: Exception) {
            }
        }
        onOutputDevice?.invoke(formatOutputDeviceLabel(if (Build.VERSION.SDK_INT >= 24) track.routedDevice else null))

        isPlaying = true
        try {
            track.play()
            onProgress?.invoke(0f)
            val total = shorts.size
            var written = 0
            var lastReport = 0f
            while (written < total) {
                val count = min(2048, total - written)
                onRender?.invoke(scaledSamples, written, count, sampleRate)
                val w = track.write(shorts, written, count)
                if (w <= 0) break
                written += w
                val progress = written.toFloat() / total.toFloat()
                if (progress - lastReport >= 0.02f || written == total) {
                    lastReport = progress
                    onProgress?.invoke(progress)
                }
            }
        } finally {
            try {
                track.stop()
            } catch (_: Exception) {
            }
            try {
                track.release()
            } catch (_: Exception) {
            }
            isPlaying = false
            onProgress?.invoke(1f)
        }
    }
}

class Aec3Processor(private val captureSampleRate: Int) {
    private val frameSize = max(1, captureSampleRate / 100)
    private val renderFrame = FloatArray(frameSize)
    private val captureFrame = FloatArray(frameSize)
    private val lock = Any()
    @Volatile private var handle: Long = nativeCreate(captureSampleRate, captureSampleRate, 1)

    fun isReady(): Boolean = handle != 0L

    fun processCapture(data: FloatArray, offset: Int, length: Int) {
        if (handle == 0L || length <= 0) return
        synchronized(lock) {
            val h = handle
            if (h == 0L || length <= 0) return
            var idx = 0
            while (idx < length) {
                val remaining = length - idx
                val chunk = min(frameSize, remaining)
                if (chunk == frameSize) {
                    nativeProcessCapture(h, data, offset + idx, chunk)
                } else {
                    java.util.Arrays.fill(captureFrame, 0f)
                    System.arraycopy(data, offset + idx, captureFrame, 0, chunk)
                    nativeProcessCapture(h, captureFrame, 0, frameSize)
                    System.arraycopy(captureFrame, 0, data, offset + idx, chunk)
                }
                idx += chunk
            }
        }
    }

    fun processRender(data: FloatArray, offset: Int, length: Int, inputRate: Int) {
        if (handle == 0L || length <= 0) return
        val src = if (inputRate == captureSampleRate) {
            data.copyOfRange(offset, offset + length)
        } else {
            resampleLinear(data, offset, length, inputRate, captureSampleRate)
        }
        if (src.isEmpty()) return
        synchronized(lock) {
            val h = handle
            if (h == 0L) return
            var idx = 0
            while (idx < src.size) {
                val remaining = src.size - idx
                val chunk = min(frameSize, remaining)
                if (chunk == frameSize) {
                    nativeProcessRender(h, src, idx, chunk)
                } else {
                    java.util.Arrays.fill(renderFrame, 0f)
                    System.arraycopy(src, idx, renderFrame, 0, chunk)
                    nativeProcessRender(h, renderFrame, 0, frameSize)
                }
                idx += chunk
            }
        }
    }

    fun release() {
        synchronized(lock) {
            val h = handle
            if (h != 0L) {
                nativeDestroy(h)
            }
            handle = 0L
        }
    }

    companion object {
        init {
            System.loadLibrary("aec3_jni")
        }
    }

    private external fun nativeCreate(captureSampleRate: Int, renderSampleRate: Int, channels: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeProcessCapture(handle: Long, data: FloatArray, offset: Int, length: Int)
    private external fun nativeProcessRender(handle: Long, data: FloatArray, offset: Int, length: Int)

    private fun resampleLinear(
        data: FloatArray,
        offset: Int,
        length: Int,
        inRate: Int,
        outRate: Int
    ): FloatArray {
        if (length <= 0 || inRate <= 0 || outRate <= 0) return FloatArray(0)
        if (inRate == outRate) {
            return data.copyOfRange(offset, offset + length)
        }
        val ratio = outRate.toDouble() / inRate.toDouble()
        val outLen = max(1, (length * ratio).roundToInt())
        val out = FloatArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = i / ratio
            val idx = srcPos.toInt()
            val frac = (srcPos - idx)
            val i0 = offset + idx
            val i1 = min(offset + length - 1, i0 + 1)
            val s0 = data[i0]
            val s1 = data[i1]
            out[i] = (s0 + (s1 - s0) * frac.toFloat())
        }
        return out
    }
}

data class SpeakerEnrollResult(
    val success: Boolean,
    val message: String,
    val profile: FloatArray? = null
)

private object SpeakerVerifier {
    private const val MODEL_ASSET_PATH = "speaker_verify/3dspeaker_speech_campplus_sv_zh-cn_16k-common.onnx"
    private const val MODEL_FILE_NAME = "3dspeaker_speech_campplus_sv_zh-cn_16k-common.onnx"
    private const val MAX_ANALYZE_SAMPLES = 16000 * 8

    private val lock = Any()
    private var extractor: SpeakerEmbeddingExtractor? = null
    private var cachedModelFile: File? = null

    fun computeEmbedding(context: Context, samples: FloatArray, sampleRate: Int): FloatArray? {
        if (sampleRate <= 0 || samples.isEmpty()) return null
        val usable = min(samples.size, MAX_ANALYZE_SAMPLES)
        val clipped = if (usable == samples.size) samples else samples.copyOfRange(0, usable)
        return synchronized(lock) {
            val activeExtractor = ensureExtractor(context) ?: return@synchronized null
            val stream = activeExtractor.createStream()
            try {
                stream.acceptWaveform(clipped, sampleRate)
                stream.inputFinished()
                if (!activeExtractor.isReady(stream)) {
                    AppLogger.i("Speaker embedding stream not ready samples=${clipped.size} sr=$sampleRate")
                    return@synchronized null
                }
                activeExtractor.compute(stream)
            } catch (t: Throwable) {
                AppLogger.e("Speaker embedding compute failed", t)
                null
            } finally {
                runCatching { stream.release() }
            }
        }
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val n = min(a.size, b.size)
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in 0 until n) {
            val av = a[i].toDouble()
            val bv = b[i].toDouble()
            dot += av * bv
            na += av * av
            nb += bv * bv
        }
        if (na <= 1e-12 || nb <= 1e-12) return 0f
        return (dot / (sqrt(na) * sqrt(nb))).toFloat().coerceIn(-1f, 1f)
    }

    fun release() {
        synchronized(lock) {
            extractor?.release()
            extractor = null
        }
    }

    private fun ensureExtractor(context: Context): SpeakerEmbeddingExtractor? {
        extractor?.let { return it }
        val modelFile = ensureModelFile(context) ?: return null
        return runCatching {
            SpeakerEmbeddingExtractor(
                null,
                SpeakerEmbeddingExtractorConfig(
                    modelFile.absolutePath,
                    2,
                    false,
                    "cpu"
                )
            )
        }.onFailure {
            AppLogger.e("Speaker extractor init failed", it)
        }.getOrNull()?.also {
            extractor = it
            AppLogger.i("Speaker extractor loaded model=${modelFile.absolutePath} dim=${it.dim()}")
        }
    }

    private fun ensureModelFile(context: Context): File? {
        cachedModelFile?.let { existing ->
            if (existing.exists() && existing.length() > 0L) return existing
        }
        return runCatching {
            val outDir = File(context.filesDir, "models/speaker_verify").apply { mkdirs() }
            val outFile = File(outDir, MODEL_FILE_NAME)
            if (!outFile.exists() || outFile.length() <= 0L) {
                context.assets.open(MODEL_ASSET_PATH).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            cachedModelFile = outFile
            outFile
        }.onFailure {
            AppLogger.e("Speaker model prepare failed", it)
        }.getOrNull()
    }
}

private object SherpaSpeechEnhancer {
    private const val GTCRN_ASSET_PATH = "speech_enhancement/gtcrn_simple.onnx"
    private const val GTCRN_FILE_NAME = "gtcrn_simple.onnx"
    private const val DPDFNET2_ASSET_PATH = "speech_enhancement/dpdfnet2.onnx"
    private const val DPDFNET2_FILE_NAME = "dpdfnet2.onnx"
    private const val DPDFNET4_ASSET_PATH = "speech_enhancement/dpdfnet4.onnx"
    private const val DPDFNET4_FILE_NAME = "dpdfnet4.onnx"

    private val lock = Any()
    private val cachedModelFiles = mutableMapOf<String, File>()
    private var offlineMode: Int = SpeechEnhancementMode.OFF
    private var offlineDenoiser: OfflineSpeechDenoiser? = null
    private var streamingMode: Int = SpeechEnhancementMode.OFF
    private var streamingDenoiser: OnlineSpeechDenoiser? = null
    private var streamingCarry = FloatArray(0)

    fun processOffline(context: Context, mode: Int, samples: FloatArray, sampleRate: Int): Pair<FloatArray, Int> {
        if (samples.isEmpty()) return samples to sampleRate
        if (SpeechEnhancementMode.clamp(mode) != SpeechEnhancementMode.GTCRN_OFFLINE) {
            return samples to sampleRate
        }
        return synchronized(lock) {
            val denoiser = ensureOfflineDenoiserLocked(context, mode) ?: return@synchronized samples to sampleRate
            val result = denoiser.run(samples, sampleRate)
            result.samples.copyOf() to result.sampleRate
        }
    }

    fun processStreamingChunk(context: Context, mode: Int, samples: FloatArray, sampleRate: Int): FloatArray {
        if (samples.isEmpty()) return FloatArray(0)
        if (!SpeechEnhancementMode.isStreaming(mode)) {
            return samples
        }
        return synchronized(lock) {
            val denoiser = ensureStreamingDenoiserLocked(context, mode) ?: return@synchronized samples
            val frameShift = denoiser.frameShiftInSamples.coerceAtLeast(1)
            val combined = concatFloatArrays(streamingCarry, samples)
            val processLen = (combined.size / frameShift) * frameShift
            if (processLen <= 0) {
                streamingCarry = combined
                return@synchronized FloatArray(0)
            }
            val current = combined.copyOfRange(0, processLen)
            streamingCarry = if (processLen < combined.size) {
                combined.copyOfRange(processLen, combined.size)
            } else {
                FloatArray(0)
            }
            val result = denoiser.run(current, sampleRate)
            result.samples.copyOf()
        }
    }

    fun resetStreaming() {
        synchronized(lock) {
            streamingCarry = FloatArray(0)
            streamingDenoiser?.reset()
        }
    }

    fun release() {
        synchronized(lock) {
            offlineDenoiser?.release()
            offlineDenoiser = null
            streamingDenoiser?.release()
            streamingDenoiser = null
            offlineMode = SpeechEnhancementMode.OFF
            streamingMode = SpeechEnhancementMode.OFF
            streamingCarry = FloatArray(0)
        }
    }

    private fun ensureOfflineDenoiserLocked(context: Context, mode: Int): OfflineSpeechDenoiser? {
        val normalized = SpeechEnhancementMode.clamp(mode)
        if (normalized != SpeechEnhancementMode.GTCRN_OFFLINE) {
            offlineDenoiser?.release()
            offlineDenoiser = null
            offlineMode = SpeechEnhancementMode.OFF
            return null
        }
        if (offlineDenoiser != null && offlineMode == normalized) {
            return offlineDenoiser
        }
        offlineDenoiser?.release()
        offlineDenoiser = OfflineSpeechDenoiser(
            null,
            OfflineSpeechDenoiserConfig(
                buildModelConfigLocked(context, normalized)
            )
        )
        offlineMode = normalized
        return offlineDenoiser
    }

    private fun ensureStreamingDenoiserLocked(context: Context, mode: Int): OnlineSpeechDenoiser? {
        val normalized = SpeechEnhancementMode.clamp(mode)
        if (!SpeechEnhancementMode.isStreaming(normalized)) {
            streamingDenoiser?.release()
            streamingDenoiser = null
            streamingMode = SpeechEnhancementMode.OFF
            streamingCarry = FloatArray(0)
            return null
        }
        if (streamingDenoiser != null && streamingMode == normalized) {
            return streamingDenoiser
        }
        streamingDenoiser?.release()
        streamingCarry = FloatArray(0)
        streamingDenoiser = OnlineSpeechDenoiser(
            null,
            OnlineSpeechDenoiserConfig(
                buildModelConfigLocked(context, normalized)
            )
        )
        streamingMode = normalized
        return streamingDenoiser
    }

    private fun buildModelConfigLocked(context: Context, mode: Int): OfflineSpeechDenoiserModelConfig {
        val normalized = SpeechEnhancementMode.clamp(mode)
        return OfflineSpeechDenoiserModelConfig().apply {
            numThreads = 2
            debug = false
            provider = "cpu"
            when (normalized) {
                SpeechEnhancementMode.GTCRN_OFFLINE,
                SpeechEnhancementMode.GTCRN_STREAMING -> {
                    gtcrn = OfflineSpeechDenoiserGtcrnModelConfig(
                        ensureModelFileLocked(context, GTCRN_ASSET_PATH, GTCRN_FILE_NAME).absolutePath
                    )
                    dpdfnet = OfflineSpeechDenoiserDpdfNetModelConfig()
                }
                SpeechEnhancementMode.DPDFNET2_STREAMING -> {
                    gtcrn = OfflineSpeechDenoiserGtcrnModelConfig()
                    dpdfnet = OfflineSpeechDenoiserDpdfNetModelConfig(
                        ensureModelFileLocked(context, DPDFNET2_ASSET_PATH, DPDFNET2_FILE_NAME).absolutePath
                    )
                }
                SpeechEnhancementMode.DPDFNET4_STREAMING -> {
                    gtcrn = OfflineSpeechDenoiserGtcrnModelConfig()
                    dpdfnet = OfflineSpeechDenoiserDpdfNetModelConfig(
                        ensureModelFileLocked(context, DPDFNET4_ASSET_PATH, DPDFNET4_FILE_NAME).absolutePath
                    )
                }
                else -> {
                    gtcrn = OfflineSpeechDenoiserGtcrnModelConfig()
                    dpdfnet = OfflineSpeechDenoiserDpdfNetModelConfig()
                }
            }
        }
    }

    private fun ensureModelFileLocked(context: Context, assetPath: String, fileName: String): File {
        val cached = cachedModelFiles[fileName]
        if (cached != null && cached.exists() && cached.length() > 0L) {
            return cached
        }
        val outDir = File(context.filesDir, "models/speech_enhancement").apply { mkdirs() }
        val outFile = File(outDir, fileName)
        if (!outFile.exists() || outFile.length() <= 0L) {
            context.assets.open(assetPath).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        cachedModelFiles[fileName] = outFile
        return outFile
    }

    private fun concatFloatArrays(first: FloatArray, second: FloatArray): FloatArray {
        if (first.isEmpty()) return second.copyOf()
        if (second.isEmpty()) return first.copyOf()
        val out = FloatArray(first.size + second.size)
        System.arraycopy(first, 0, out, 0, first.size)
        System.arraycopy(second, 0, out, first.size, second.size)
        return out
    }
}

class RealtimeController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onResult: (Long, String) -> Unit,
    private val onStreamingResult: (String) -> Unit,
    private val onProgress: (Long, Float) -> Unit,
    private val onLevel: (Float) -> Unit,
    private val onInputDevice: (String) -> Unit,
    private val onOutputDevice: (String) -> Unit,
    private val onAec3Status: (String) -> Unit,
    private val onAec3Diag: (String) -> Unit,
    private val onSpeakerVerify: (Float, Boolean) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit,
    initialSuppressWhilePlaying: Boolean,
    initialUseVoiceCommunication: Boolean,
    initialCommunicationMode: Boolean,
    initialMinVolumePercent: Int,
    initialPlaybackGainPercent: Int,
    initialDenoiserMode: Int,
    initialSpeechEnhancementMode: Int,
    initialPiperNoiseScale: Float,
    initialPiperLengthScale: Float,
    initialPiperNoiseW: Float,
    initialPiperSentenceSilenceSec: Float,
    initialSuppressDelaySec: Float,
    initialPreferredInputType: Int,
    initialPreferredOutputType: Int,
    initialUseAec3: Boolean,
    initialNumberReplaceMode: Int,
    initialClassicVadEnabled: Boolean,
    initialSileroVadEnabled: Boolean,
    initialAllowSystemAecWithAec3: Boolean,
    initialSpeakerVerifyEnabled: Boolean,
    initialSpeakerVerifyThreshold: Float,
    initialSpeakerProfiles: List<FloatArray>,
    private val moduleFactory: SpeechModuleFactory = DefaultSpeechModuleFactory
) {
    private var recorder: AudioRecord? = null
    private var loopJob: Job? = null
    private var asr: AsrModule? = null
    private var tts: TtsModule? = null
    private val player = AudioPlayer(context)
    private val sampleRate = 16000
    private val queueLock = Any()
    private val ttsQueue = ArrayDeque<QueuedTts>()
    private var ttsJob: Job? = null
    private var nextUtteranceId = 1L
    @Volatile private var suppressWhilePlaying = initialSuppressWhilePlaying
    @Volatile private var useVoiceCommunication = initialUseVoiceCommunication
    @Volatile private var useCommunicationMode = initialCommunicationMode
    @Volatile private var minSegmentRms = (initialMinVolumePercent.coerceIn(0, 100) / 100.0)
    @Volatile private var denoiserMode = initialDenoiserMode.coerceIn(AudioDenoiserMode.OFF, AudioDenoiserMode.SPEEX)
    @Volatile private var speechEnhancementMode = SpeechEnhancementMode.clamp(initialSpeechEnhancementMode)
    @Volatile private var suppressDelayMs = (initialSuppressDelaySec.coerceIn(0f, 5f) * 1000f).toLong()
    @Volatile private var piperNoiseScale = initialPiperNoiseScale.coerceIn(0f, 2f)
    @Volatile private var piperLengthScale = initialPiperLengthScale.coerceIn(0.1f, 5f)
    @Volatile private var piperNoiseW = initialPiperNoiseW.coerceIn(0f, 2f)
    @Volatile private var piperSentenceSilenceSec = initialPiperSentenceSilenceSec.coerceIn(0f, 2f)
    @Volatile private var suppressUntilMs: Long = 0L
    @Volatile private var preferredInputType = initialPreferredInputType
    @Volatile private var preferredOutputType = initialPreferredOutputType
    @Volatile private var useAec3 = initialUseAec3
    @Volatile private var numberReplaceMode = initialNumberReplaceMode.coerceIn(0, 2)
    @Volatile private var classicVadEnabled = initialClassicVadEnabled
    @Volatile private var sileroVadEnabled = initialSileroVadEnabled
    @Volatile private var allowSystemAecWithAec3 = initialAllowSystemAecWithAec3
    @Volatile private var speakerVerifyEnabled = initialSpeakerVerifyEnabled
    @Volatile private var speakerVerifyThreshold = initialSpeakerVerifyThreshold.coerceIn(0.4f, 0.95f)
    @Volatile private var speakerProfiles: List<FloatArray> =
        initialSpeakerProfiles.mapNotNull { p -> if (p.isEmpty()) null else p.copyOf() }
    @Volatile private var speakerLastSimilarity: Float = -1f
    private val lastRenderMs = AtomicLong(0L)
    private val lastCaptureMs = AtomicLong(0L)
    private val renderFrames = AtomicLong(0L)
    private val captureFrames = AtomicLong(0L)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var previousAudioMode: Int? = null
    private var previousSpeakerOn: Boolean? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var aec: AcousticEchoCanceler? = null
    private var aec3: Aec3Processor? = null
    private var currentAsrDir: File? = null
    private var currentSileroVadModelFile: File? = null
    private var currentVoiceDir: File? = null
    private var lastLevelReportMs: Long = 0L
    private val recorderMutex = Mutex()
    private var rnnoiseProcessor: RnNoiseProcessor? = null
    private var speexNoiseProcessor: SpeexNoiseSuppressor? = null
    private var sileroVadProcessor: SileroVadProcessor? = null
    private val denoiserLock = Any()
    private val sileroVadLock = Any()
    private var lastAcceptedTtsTextKey: String = ""
    private var lastAcceptedTtsAtMs: Long = 0L
    private val duplicateTtsWindowMs: Long = 1800L
    @Volatile private var pttStreamingEnabled: Boolean = false
    @Volatile private var suppressAsrAutoSpeak: Boolean = false
    @Volatile private var lastStreamingDecodeAtMs: Long = 0L
    private val streamingDecodeBusy = AtomicBoolean(false)

    private data class QueuedTts(
        val id: Long,
        val text: String
    )

    private data class TtsSynthesisChunk(
        val text: String,
        val pauseSec: Float
    )

    private class SileroVadProcessor(
        context: Context,
        modelFile: File,
        sampleRate: Int,
        numThreads: Int = 2
    ) {
        private val lock = Any()
        private val vad = Vad(
            null,
            VadModelConfig().apply {
                this.sampleRate = sampleRate
                this.numThreads = numThreads
                provider = "cpu"
                debug = false
                sileroVadModelConfig = SileroVadModelConfig().apply {
                    model = modelFile.absolutePath
                    threshold = 0.5f
                    minSilenceDuration = 0.4f
                    minSpeechDuration = 0.2f
                    windowSize = 512
                    maxSpeechDuration = 12.0f
                }
            }
        )

        fun acceptWaveform(samples: FloatArray) {
            synchronized(lock) {
                vad.acceptWaveform(samples)
            }
        }

        fun isSpeechDetected(): Boolean {
            return synchronized(lock) {
                vad.isSpeechDetected()
            }
        }

        fun drainSegments(): List<FloatArray> {
            return synchronized(lock) {
                drainSegmentsLocked()
            }
        }

        fun flushAndDrain(): List<FloatArray> {
            return synchronized(lock) {
                vad.flush()
                drainSegmentsLocked()
            }
        }

        fun reset() {
            synchronized(lock) {
                vad.reset()
                vad.clear()
            }
        }

        fun release() {
            synchronized(lock) {
                vad.release()
            }
        }

        private fun drainSegmentsLocked(): List<FloatArray> {
            val segments = mutableListOf<FloatArray>()
            while (!vad.empty()) {
                val segment = vad.front()
                segments.add(segment.samples.copyOf())
                vad.pop()
            }
            return segments
        }
    }

    private fun normalizePunctuationForTts(text: String): String {
        if (text.isEmpty()) return text
        val out = StringBuilder(text.length)
        for (ch in text) {
            out.append(
                when (ch) {
                    '，', '、' -> ','
                    '。' -> '.'
                    '！' -> '!'
                    '？' -> '?'
                    '；' -> ';'
                    '：' -> ':'
                    else -> ch
                }
            )
        }
        return out.toString()
    }

    private fun splitForPunctuationSynthesis(text: String): List<TtsSynthesisChunk> {
        val normalized = normalizePunctuationForTts(text).trim()
        if (normalized.isEmpty()) return emptyList()
        val longPause = piperSentenceSilenceSec.coerceIn(0f, 2f)
        val shortPause = if (longPause <= 0f) 0f else (longPause * 0.4f).coerceIn(0.04f, longPause)
        val chunks = mutableListOf<TtsSynthesisChunk>()
        val current = StringBuilder()
        fun pushChunk(pause: Float) {
            val part = current.toString().trim()
            if (part.isEmpty()) return
            chunks.add(TtsSynthesisChunk(part, pause))
            current.setLength(0)
        }
        for (ch in normalized) {
            when (ch) {
                ',' -> pushChunk(shortPause)
                '.', '!', '?', ';', ':' -> pushChunk(longPause)
                else -> current.append(ch)
            }
        }
        val tail = current.toString().trim()
        if (tail.isNotEmpty()) {
            chunks.add(TtsSynthesisChunk(tail, 0f))
        }
        return if (chunks.isNotEmpty()) chunks else listOf(TtsSynthesisChunk(normalized, 0f))
    }

    private fun concatAudio(arrays: List<FloatArray>): FloatArray {
        val total = arrays.sumOf { it.size }
        if (total <= 0) return FloatArray(0)
        val out = FloatArray(total)
        var offset = 0
        for (arr in arrays) {
            if (arr.isEmpty()) continue
            System.arraycopy(arr, 0, out, offset, arr.size)
            offset += arr.size
        }
        return out
    }

    private fun synthesizeByPunctuation(ttsEngine: TtsModule, text: String): FloatArray {
        val chunks = splitForPunctuationSynthesis(text)
        if (chunks.isEmpty()) return FloatArray(0)
        if (chunks.size == 1) {
            val only = chunks[0]
            return ttsEngine.synthesize(only.text, only.pauseSec)
        }
        val parts = mutableListOf<FloatArray>()
        for (chunk in chunks) {
            val piece = ttsEngine.synthesize(chunk.text, chunk.pauseSec)
            if (piece.isEmpty()) continue
            parts.add(piece)
        }
        return concatAudio(parts)
    }

    private fun toTtsDedupKey(text: String): String {
        val t = text.trim()
        if (t.isEmpty()) return ""
        return t.trimEnd('。', '！', '？', '!', '?', '，', ',', '；', ';', '、', '.')
    }

    private fun shouldSkipDuplicateTts(text: String): Boolean {
        val key = toTtsDedupKey(text)
        if (key.isEmpty()) return true
        val now = SystemClock.uptimeMillis()
        synchronized(queueLock) {
            val duplicated = key == lastAcceptedTtsTextKey && (now - lastAcceptedTtsAtMs) <= duplicateTtsWindowMs
            if (!duplicated) {
                lastAcceptedTtsTextKey = key
                lastAcceptedTtsAtMs = now
            }
            return duplicated
        }
    }

    private fun notifyResult(id: Long, text: String) {
        scope.launch { onResult(id, text) }
    }

    private fun notifyStreamingResult(text: String) {
        scope.launch { onStreamingResult(text) }
    }

    private fun notifyProgress(id: Long, progress: Float) {
        scope.launch { onProgress(id, progress.coerceIn(0f, 1f)) }
    }

    private fun notifyLevel(level: Float) {
        scope.launch { onLevel(level.coerceIn(0f, 1f)) }
    }

    private fun notifyInputDevice(label: String) {
        scope.launch { onInputDevice(label) }
    }

    private fun notifyOutputDevice(label: String) {
        scope.launch { onOutputDevice(label) }
    }

    private fun notifyAec3Status(status: String) {
        scope.launch { onAec3Status(status) }
    }

    private fun notifyAec3Diag(diag: String) {
        scope.launch { onAec3Diag(diag) }
    }

    private fun notifySpeakerVerify(similarity: Float, passed: Boolean) {
        scope.launch { onSpeakerVerify(similarity, passed) }
    }

    private fun notifyStatus(msg: String) {
        scope.launch { onStatus(msg) }
    }

    private fun notifyError(msg: String) {
        scope.launch { onError(msg) }
    }

    init {
        player.setUseCommunicationAttributes(useCommunicationMode)
        player.setPreferredOutputType(preferredOutputType)
        player.setPlaybackGainPercent(initialPlaybackGainPercent)
        player.setOnOutputDevice { notifyOutputDevice(it) }
        player.setOnRender { data, offset, length, rate ->
            if (useAec3) {
                aec3?.processRender(data, offset, length, rate)
                renderFrames.incrementAndGet()
                lastRenderMs.set(SystemClock.uptimeMillis())
            }
        }
        notifyAec3Status(if (useAec3) "待启动" else "未启用")
    }

    fun setSuppressWhilePlaying(enabled: Boolean) {
        suppressWhilePlaying = enabled
        if (!enabled) {
            suppressUntilMs = 0L
        }
    }

    fun setUseVoiceCommunication(enabled: Boolean) {
        useVoiceCommunication = enabled
    }

    fun setMinVolumePercent(percent: Int) {
        minSegmentRms = (percent.coerceIn(0, 100) / 100.0)
    }

    fun setPlaybackGainPercent(percent: Int) {
        player.setPlaybackGainPercent(percent)
    }

    fun setDenoiserMode(mode: Int) {
        val normalized = mode.coerceIn(AudioDenoiserMode.OFF, AudioDenoiserMode.SPEEX)
        synchronized(denoiserLock) {
            if (denoiserMode == normalized) return
            denoiserMode = normalized
            when (normalized) {
                AudioDenoiserMode.OFF -> releaseNoiseProcessorsLocked()
                AudioDenoiserMode.RNNOISE -> {
                    speexNoiseProcessor?.release()
                    speexNoiseProcessor = null
                    rnnoiseProcessor?.reset()
                }
                AudioDenoiserMode.SPEEX -> {
                    rnnoiseProcessor?.release()
                    rnnoiseProcessor = null
                    speexNoiseProcessor?.reset()
                }
                else -> Unit
            }
        }
    }

    fun setSpeechEnhancementMode(mode: Int) {
        val normalized = SpeechEnhancementMode.clamp(mode)
        if (speechEnhancementMode == normalized) return
        speechEnhancementMode = normalized
        SherpaSpeechEnhancer.release()
    }

    private fun applyTtsSynthesisTuning() {
        tts?.setSynthesisTuning(
            noiseScale = piperNoiseScale,
            lengthScale = piperLengthScale,
            noiseW = piperNoiseW,
            sentenceSilenceSec = piperSentenceSilenceSec
        )
    }

    fun setPiperNoiseScale(value: Float) {
        piperNoiseScale = value.coerceIn(0f, 2f)
        applyTtsSynthesisTuning()
    }

    fun setPiperLengthScale(value: Float) {
        piperLengthScale = value.coerceIn(0.1f, 5f)
        applyTtsSynthesisTuning()
    }

    fun setPiperNoiseW(value: Float) {
        piperNoiseW = value.coerceIn(0f, 2f)
        applyTtsSynthesisTuning()
    }

    fun setPiperSentenceSilenceSec(value: Float) {
        piperSentenceSilenceSec = value.coerceIn(0f, 2f)
        applyTtsSynthesisTuning()
    }

    fun setSuppressDelaySec(seconds: Float) {
        suppressDelayMs = (seconds.coerceIn(0f, 5f) * 1000f).toLong()
        if (suppressDelayMs == 0L) {
            suppressUntilMs = 0L
        }
    }

    fun setCommunicationMode(enabled: Boolean) {
        useCommunicationMode = enabled
        player.setUseCommunicationAttributes(enabled)
        if (recorder != null) {
            applyCommunicationMode(enabled)
        }
    }

    fun setPreferredInputType(type: Int) {
        preferredInputType = type
        recorder?.let { rec ->
            applyInputRoutePreference(rec)
            reportInputDevice(rec)
        }
    }

    fun setPreferredOutputType(type: Int) {
        preferredOutputType = type
        player.setPreferredOutputType(type)
        if (recorder != null) {
            applyOutputRoutePreference()
        }
    }

    fun setUseAec3(enabled: Boolean) {
        useAec3 = enabled
        if (!enabled) {
            aec3?.release()
            aec3 = null
            notifyAec3Status("未启用")
            notifyAec3Diag("AEC3 诊断：未启用")
        } else {
            notifyAec3Status("初始化中")
            ensureAec3()
        }
    }

    fun setNumberReplaceMode(mode: Int) {
        numberReplaceMode = mode.coerceIn(0, 2)
    }

    private fun normalizeVadFlags(
        classicEnabled: Boolean,
        sileroEnabled: Boolean
    ): Pair<Boolean, Boolean> {
        return if (!classicEnabled && !sileroEnabled) {
            true to false
        } else {
            classicEnabled to sileroEnabled
        }
    }

    fun setClassicVadEnabled(enabled: Boolean) {
        val (classicEnabled, sileroEnabled) = normalizeVadFlags(enabled, sileroVadEnabled)
        classicVadEnabled = classicEnabled
        sileroVadEnabled = sileroEnabled
    }

    fun setSileroVadEnabled(enabled: Boolean) {
        val (classicEnabled, sileroEnabled) = normalizeVadFlags(classicVadEnabled, enabled)
        classicVadEnabled = classicEnabled
        sileroVadEnabled = sileroEnabled
        if (!sileroEnabled) {
            resetSileroVadProcessor()
        }
    }

    fun setPushToTalkStreamingEnabled(enabled: Boolean) {
        val wasEnabled = pttStreamingEnabled
        pttStreamingEnabled = enabled
        if (!enabled) {
            lastStreamingDecodeAtMs = 0L
            streamingDecodeBusy.set(false)
            if (wasEnabled && sileroVadEnabled) {
                drainSileroVadSegments(flush = true).forEach { segment ->
                    if (classicVadEnabled && !passesClassicVadGate(segment)) return@forEach
                    processRecognizedSegment(segment)
                }
            }
        }
    }

    fun setSuppressAsrAutoSpeak(enabled: Boolean) {
        suppressAsrAutoSpeak = enabled
    }

    fun isMicActive(): Boolean {
        return recorder != null
    }

    private fun nextResultId(): Long {
        synchronized(queueLock) {
            return nextUtteranceId++
        }
    }

    fun setAllowSystemAecWithAec3(enabled: Boolean) {
        allowSystemAecWithAec3 = enabled
    }

    fun setSpeakerVerifyEnabled(enabled: Boolean) {
        speakerVerifyEnabled = enabled
    }

    fun setSpeakerVerifyThreshold(threshold: Float) {
        speakerVerifyThreshold = threshold.coerceIn(0.4f, 0.95f)
    }

    fun setSpeakerProfiles(profiles: List<FloatArray>) {
        speakerProfiles = profiles.mapNotNull { p -> if (p.isEmpty()) null else p.copyOf() }
        if (speakerProfiles.isEmpty()) {
            speakerLastSimilarity = -1f
        }
    }

    fun clearSpeakerProfiles() {
        speakerProfiles = emptyList()
        speakerLastSimilarity = -1f
    }

    fun hasSpeakerProfiles(): Boolean {
        return speakerProfiles.isNotEmpty()
    }

    fun setSpeakerProfile(profile: FloatArray?) {
        setSpeakerProfiles(if (profile == null || profile.isEmpty()) emptyList() else listOf(profile))
    }

    fun clearSpeakerProfile() {
        clearSpeakerProfiles()
    }

    fun hasSpeakerProfile(): Boolean {
        return hasSpeakerProfiles()
    }

    fun latestSpeakerSimilarity(): Float {
        return speakerLastSimilarity
    }

    private fun enqueueTts(text: String): Long {
        val id = nextUtteranceId++
        synchronized(queueLock) {
            ttsQueue.addLast(QueuedTts(id, text))
        }
        return id
    }

    private fun ensureTtsLoop() {
        if (ttsJob?.isActive == true) return
        ttsJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val next = synchronized(queueLock) {
                    if (ttsQueue.isNotEmpty()) ttsQueue.removeFirst() else null
                } ?: break
                try {
                    notifyProgress(next.id, 0f)
                    val ttsEngine = tts
                    val pcm = if (ttsEngine != null) {
                        synthesizeByPunctuation(ttsEngine, next.text)
                    } else {
                        FloatArray(0)
                    }
                    if (pcm.isNotEmpty()) {
                        player.play(pcm, tts?.sampleRate ?: 22050) { progress ->
                            notifyProgress(next.id, progress)
                        }
                        if (suppressDelayMs > 0L) {
                            suppressUntilMs = SystemClock.uptimeMillis() + suppressDelayMs
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("TTS failed", e)
                    notifyError("TTS 失败: ${e.message}")
                } finally {
                    notifyProgress(next.id, 1f)
                }
            }
        }
    }

    private fun ensureAec3() {
        if (!useAec3) return
        if (aec3 != null) return
        val renderRate = tts?.sampleRate ?: sampleRate
        if (renderRate != sampleRate) {
            AppLogger.i("AEC3 render rate $renderRate resampled to $sampleRate")
        }
        val proc = Aec3Processor(sampleRate)
        if (proc.isReady()) {
            aec3 = proc
            AppLogger.i("AEC3 enabled capture=${sampleRate}")
            notifyAec3Status("已启用")
            notifyAec3Diag("AEC3 诊断：已启用，等待渲染参考")
        } else {
            proc.release()
            AppLogger.e("AEC3 init failed")
            notifyAec3Status("初始化失败")
            notifyAec3Diag("AEC3 诊断：初始化失败")
            notifyError("AEC3 初始化失败")
        }
    }

    suspend fun loadAsr(asrDir: File): Boolean {
        return recorderMutex.withLock {
            try {
                if (asr == null || currentAsrDir?.absolutePath != asrDir.absolutePath) {
                    releaseSileroVadProcessor()
                    currentSileroVadModelFile = resolveSileroVadModel(asrDir)
                    asr = moduleFactory.createAsr(context, asrDir)
                    currentAsrDir = asrDir
                    AppLogger.i("ASR loaded dir=${asrDir.absolutePath}")
                }
            } catch (e: Throwable) {
                AppLogger.e("ASR load failed", e)
                notifyError("ASR 加载失败: ${e.message}")
                return@withLock false
            }
            true
        }
    }

    suspend fun loadTts(voiceDir: File): Boolean {
        return recorderMutex.withLock {
            try {
                if (tts == null || currentVoiceDir?.absolutePath != voiceDir.absolutePath) {
                    ttsJob?.cancel()
                    ttsJob = null
                    synchronized(queueLock) {
                        ttsQueue.clear()
                    }
                    tts = moduleFactory.createTts(context, voiceDir)
                    applyTtsSynthesisTuning()
                    currentVoiceDir = voiceDir
                    AppLogger.i("TTS loaded dir=${voiceDir.absolutePath}")
                    if (useAec3) {
                        aec3?.release()
                        aec3 = null
                        ensureAec3()
                    }
                }
            } catch (e: Throwable) {
                AppLogger.e("TTS load failed", e)
                notifyError(
                    if (isSystemTtsVoiceDir(voiceDir)) {
                        "系统 TTS 初始化失败，请先完成系统 TTS 设置"
                    } else {
                        "TTS 加载失败: ${e.message}"
                    }
                )
                return@withLock false
            }
            true
        }
    }

    suspend fun enrollSpeaker(
        durationSec: Float = 4f,
        onCapture: ((progress: Float, level: Float) -> Unit)? = null
    ): SpeakerEnrollResult {
        return recorderMutex.withLock {
            AppLogger.i("Speaker enroll start durationSec=$durationSec sampleRate=$sampleRate")
            if (recorder != null) {
                return@withLock SpeakerEnrollResult(
                    success = false,
                    message = "请先停止麦克风再注册说话人"
                )
            }
            val seconds = durationSec.coerceIn(2f, 8f)
            val sampleCount = (sampleRate * seconds).roundToInt().coerceAtLeast(sampleRate)
            onCapture?.invoke(0f, 0f)
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val source = if (useVoiceCommunication) {
                MediaRecorder.AudioSource.VOICE_COMMUNICATION
            } else {
                MediaRecorder.AudioSource.MIC
            }
            val rec = AudioRecord(
                source,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minBuf, 4096)
            )
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                return@withLock SpeakerEnrollResult(
                    success = false,
                    message = "说话人注册失败：录音初始化失败"
                )
            }
            applyInputRoutePreference(rec)
            val temp = ShortArray(1024)
            val captured = FloatArray(sampleCount)
            var offset = 0
            var levelEma = 0f
            try {
                rec.startRecording()
                while (offset < sampleCount) {
                    val read = rec.read(temp, 0, min(temp.size, sampleCount - offset))
                    if (read <= 0) continue
                    var sumSq = 0.0
                    for (i in 0 until read) {
                        val v = temp[i] / 32768f
                        captured[offset + i] = v
                        sumSq += v * v
                    }
                    offset += read
                    val chunkRms = if (read > 0) sqrt(sumSq / read).toFloat() else 0f
                    levelEma = levelEma * 0.82f + chunkRms * 0.18f
                    val normalizedLevel = (levelEma / 0.2f).coerceIn(0f, 1f)
                    val progress = (offset.toFloat() / sampleCount.toFloat()).coerceIn(0f, 1f)
                    onCapture?.invoke(progress, normalizedLevel)
                }
            } catch (e: Exception) {
                AppLogger.e("Speaker enroll read failed", e)
                return@withLock SpeakerEnrollResult(
                    success = false,
                    message = "说话人注册失败：${e.message ?: "录音异常"}"
                )
            } finally {
                try {
                    rec.stop()
                } catch (_: Exception) {
                }
                try {
                    rec.release()
                } catch (_: Exception) {
                }
            }
            if (offset < sampleRate / 2) {
                return@withLock SpeakerEnrollResult(
                    success = false,
                    message = "说话人注册失败：录音时长不足"
                )
            }
            onCapture?.invoke(1f, 0f)
            val audio = if (offset == captured.size) captured else captured.copyOf(offset)
            val rms = rmsEnergy(audio)
            AppLogger.i("Speaker enroll captured samples=${audio.size} rms=$rms")
            if (rms < 0.008) {
                return@withLock SpeakerEnrollResult(
                    success = false,
                    message = "说话人注册失败：音量过低，请靠近麦克风"
                )
            }
            val embedding = SpeakerVerifier.computeEmbedding(context, audio, sampleRate)
                ?: return@withLock SpeakerEnrollResult(
                    success = false,
                    message = "说话人注册失败：有效语音不足"
                )
            AppLogger.i("Speaker enroll embedding dim=${embedding.size}")
            SpeakerEnrollResult(
                success = true,
                message = "说话人注册成功",
                profile = embedding
            )
        }
    }

    suspend fun startMic(): Boolean {
        return recorderMutex.withLock {
            if (asr == null || tts == null) {
                notifyError("模型未就绪，请先加载 ASR 和语音包")
                return@withLock false
            }
            synchronized(queueLock) {
                lastAcceptedTtsTextKey = ""
                lastAcceptedTtsAtMs = 0L
            }
            lastStreamingDecodeAtMs = 0L
            streamingDecodeBusy.set(false)
            stopRecorderOnlyLocked()
            SherpaSpeechEnhancer.resetStreaming()
            ensureAec3()
            startRecorderLoop()
            true
        }
    }

    suspend fun stopMic() {
        recorderMutex.withLock {
            stopRecorderOnlyLocked()
        }
    }

    suspend fun enqueueSpeakText(text: String): Long? {
        return recorderMutex.withLock {
            val normalized = text.trim()
            if (normalized.isEmpty()) return@withLock null
            if (tts == null) {
                notifyError("TTS 未就绪，请先选择语音包")
                return@withLock null
            }
            val id = enqueueTts(normalized)
            notifyResult(id, normalized)
            ensureTtsLoop()
            id
        }
    }

    suspend fun start(asrDir: File, voiceDir: File) {
        AppLogger.i("Realtime start asrDir=${asrDir.absolutePath} voiceDir=${voiceDir.absolutePath}")
        if (!loadAsr(asrDir)) return
        if (!loadTts(voiceDir)) return
        startMic()
    }

    suspend fun restartRecorder() {
        recorderMutex.withLock {
            if (asr == null || tts == null) return
            if (recorder == null) return
            stopRecorderOnlyLocked()
            SherpaSpeechEnhancer.resetStreaming()
            ensureAec3()
            startRecorderLoop()
        }
    }

    suspend fun stop() {
        recorderMutex.withLock {
            stopRecorderOnlyLocked()
            ttsJob?.cancel()
            ttsJob = null
            synchronized(queueLock) {
                ttsQueue.clear()
            }
            releaseNoiseProcessors()
            releaseSileroVadProcessor()
            SherpaSpeechEnhancer.release()
            SpeakerVerifier.release()
            aec3?.release()
            aec3 = null
            notifyAec3Status(if (useAec3) "待启动" else "未启用")
            AppLogger.i("Realtime stop")
        }
    }

    private suspend fun stopRecorderOnlyLocked() {
        try {
            aec?.release()
        } catch (_: Exception) {
        }
        aec = null
        val rec = recorder
        recorder = null
        try {
            rec?.stop()
        } catch (_: Exception) {
        }
        val job = loopJob
        loopJob = null
        if (job != null) {
            try {
                job.cancel()
                job.join()
            } catch (_: Exception) {
            }
        }
        streamingDecodeBusy.set(false)
        lastStreamingDecodeAtMs = 0L
        try {
            rec?.release()
        } catch (_: Exception) {
        }
        unregisterAudioDeviceCallback()
        restoreOutputRoutePreference()
        restoreCommunicationMode()
        resetNoiseProcessors()
        resetSileroVadProcessor()
        SherpaSpeechEnhancer.resetStreaming()
        if (aec3 == null) {
            notifyAec3Status(if (useAec3) "待启动" else "未启用")
        }
    }

    private fun ensureRnNoiseProcessor(): RnNoiseProcessor? {
        synchronized(denoiserLock) {
            rnnoiseProcessor?.let { return it }
            return runCatching { RnNoiseProcessor() }
                .onFailure {
                    AppLogger.e("RNNoise init failed", it)
                    notifyError("RNNoise 初始化失败")
                    denoiserMode = AudioDenoiserMode.OFF
                }
                .getOrNull()
                ?.also { rnnoiseProcessor = it }
        }
    }

    private fun ensureSpeexNoiseProcessor(): SpeexNoiseSuppressor? {
        synchronized(denoiserLock) {
            speexNoiseProcessor?.let { return it }
            return runCatching { SpeexNoiseSuppressor(sampleRate = sampleRate, frameSize = 160) }
                .onFailure {
                    AppLogger.e("Speex init failed", it)
                    notifyError("Speex 初始化失败")
                    denoiserMode = AudioDenoiserMode.OFF
                }
                .getOrNull()
                ?.also { speexNoiseProcessor = it }
        }
    }

    private fun applyNoiseSuppression(buffer: FloatArray, length: Int) {
        synchronized(denoiserLock) {
            when (denoiserMode) {
                AudioDenoiserMode.RNNOISE -> ensureRnNoiseProcessorLocked()?.processInPlace(buffer, length)
                AudioDenoiserMode.SPEEX -> ensureSpeexNoiseProcessorLocked()?.processInPlace(buffer, length)
                else -> Unit
            }
        }
    }

    private fun resetNoiseProcessors() {
        synchronized(denoiserLock) {
            rnnoiseProcessor?.reset()
            speexNoiseProcessor?.reset()
        }
    }

    private fun releaseNoiseProcessors() {
        synchronized(denoiserLock) {
            releaseNoiseProcessorsLocked()
        }
    }

    private fun ensureRnNoiseProcessorLocked(): RnNoiseProcessor? {
        rnnoiseProcessor?.let { return it }
        return runCatching { RnNoiseProcessor() }
            .onFailure {
                AppLogger.e("RNNoise init failed", it)
                notifyError("RNNoise 初始化失败")
                denoiserMode = AudioDenoiserMode.OFF
            }
            .getOrNull()
            ?.also { rnnoiseProcessor = it }
    }

    private fun ensureSpeexNoiseProcessorLocked(): SpeexNoiseSuppressor? {
        speexNoiseProcessor?.let { return it }
        return runCatching { SpeexNoiseSuppressor(sampleRate = sampleRate, frameSize = 160) }
            .onFailure {
                AppLogger.e("Speex init failed", it)
                notifyError("Speex 初始化失败")
                denoiserMode = AudioDenoiserMode.OFF
            }
            .getOrNull()
            ?.also { speexNoiseProcessor = it }
    }

    private fun releaseNoiseProcessorsLocked() {
        rnnoiseProcessor?.release()
        rnnoiseProcessor = null
        speexNoiseProcessor?.release()
        speexNoiseProcessor = null
    }

    private fun resolveSileroVadModel(asrDir: File): File? {
        return asrDir.walkTopDown()
            .firstOrNull { it.isFile && it.name.equals("silero_vad.onnx", ignoreCase = true) }
    }

    private fun ensureSileroVadProcessorLocked(): SileroVadProcessor? {
        sileroVadProcessor?.let { return it }
        val modelFile = currentSileroVadModelFile ?: return null
        return runCatching {
            SileroVadProcessor(
                context = context,
                modelFile = modelFile,
                sampleRate = sampleRate
            )
        }.onFailure {
            AppLogger.e("Silero VAD init failed", it)
            notifyError("Silero VAD 初始化失败")
            sileroVadEnabled = false
            classicVadEnabled = true
        }.getOrNull()?.also { sileroVadProcessor = it }
    }

    private fun acceptSileroVadWaveform(samples: FloatArray) {
        if (!sileroVadEnabled) return
        synchronized(sileroVadLock) {
            ensureSileroVadProcessorLocked()?.acceptWaveform(samples)
        }
    }

    private fun isSileroSpeechDetected(): Boolean {
        if (!sileroVadEnabled) return false
        return synchronized(sileroVadLock) {
            ensureSileroVadProcessorLocked()?.isSpeechDetected() == true
        }
    }

    private fun drainSileroVadSegments(flush: Boolean = false): List<FloatArray> {
        if (!sileroVadEnabled) return emptyList()
        return synchronized(sileroVadLock) {
            val processor = ensureSileroVadProcessorLocked() ?: return emptyList()
            if (flush) processor.flushAndDrain() else processor.drainSegments()
        }
    }

    private fun resetSileroVadProcessor() {
        synchronized(sileroVadLock) {
            sileroVadProcessor?.reset()
        }
    }

    private fun releaseSileroVadProcessor() {
        synchronized(sileroVadLock) {
            sileroVadProcessor?.release()
            sileroVadProcessor = null
        }
    }

    private fun disableSpeechEnhancement(mode: Int, cause: Throwable? = null) {
        val label = SpeechEnhancementMode.labelOf(mode)
        if (cause != null) {
            AppLogger.e("Speech enhancement disabled mode=$label", cause)
        } else {
            AppLogger.i("Speech enhancement disabled mode=$label")
        }
        speechEnhancementMode = SpeechEnhancementMode.OFF
        SherpaSpeechEnhancer.release()
        notifyStatus("$label 初始化失败，已回退原始语音")
    }

    private fun prepareSpeechEnhancedAudio(samples: FloatArray, sourceSampleRate: Int): Pair<FloatArray, Int> {
        val currentMode = SpeechEnhancementMode.clamp(speechEnhancementMode)
        if (currentMode != SpeechEnhancementMode.GTCRN_OFFLINE || samples.isEmpty()) {
            return samples to sourceSampleRate
        }
        return runCatching {
            SherpaSpeechEnhancer.processOffline(context, currentMode, samples, sourceSampleRate)
        }.getOrElse { error ->
            disableSpeechEnhancement(currentMode, error)
            samples to sourceSampleRate
        }
    }

    private fun processRealtimeSpeechEnhancement(samples: FloatArray, sourceSampleRate: Int): FloatArray {
        val currentMode = SpeechEnhancementMode.clamp(speechEnhancementMode)
        if (!SpeechEnhancementMode.isStreaming(currentMode) || samples.isEmpty()) {
            return samples
        }
        return runCatching {
            SherpaSpeechEnhancer.processStreamingChunk(context, currentMode, samples, sourceSampleRate)
        }.getOrElse { error ->
            disableSpeechEnhancement(currentMode, error)
            samples
        }
    }

    private fun applyCommunicationMode(enabled: Boolean) {
        val manager = audioManager ?: return
        try {
            if (enabled) {
                if (previousAudioMode == null) {
                    previousAudioMode = manager.mode
                }
                if (manager.mode != AudioManager.MODE_IN_COMMUNICATION) {
                    manager.mode = AudioManager.MODE_IN_COMMUNICATION
                }
            } else {
                restoreCommunicationMode()
            }
        } catch (e: Exception) {
            AppLogger.e("AudioManager mode set failed", e)
        }
    }

    private fun applyOutputRoutePreference() {
        val manager = audioManager ?: return
        try {
            if (!useCommunicationMode) {
                restoreOutputRoutePreference()
                return
            }
            if (previousSpeakerOn == null) {
                previousSpeakerOn = manager.isSpeakerphoneOn
            }
            if (Build.VERSION.SDK_INT >= 31) {
                if (preferredOutputType == AudioRoutePreference.OUTPUT_AUTO) {
                    manager.clearCommunicationDevice()
                } else {
                    val target = pickPreferredOutputDevice(
                        manager.availableCommunicationDevices.toTypedArray(),
                        preferredOutputType
                    )
                    if (target != null) {
                        manager.setCommunicationDevice(target)
                    } else {
                        AppLogger.i("Prefer output route: target type=$preferredOutputType not found")
                    }
                }
            } else {
                when (preferredOutputType) {
                    AudioRoutePreference.OUTPUT_SPEAKER -> manager.isSpeakerphoneOn = true
                    AudioRoutePreference.OUTPUT_EARPIECE, AudioRoutePreference.OUTPUT_AUTO -> manager.isSpeakerphoneOn = false
                    else -> {
                        // Old APIs can only reliably switch speaker/earpiece.
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("AudioManager output route failed", e)
        }
    }

    private fun restoreOutputRoutePreference() {
        val manager = audioManager ?: return
        val prev = previousSpeakerOn
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                manager.clearCommunicationDevice()
            }
            if (prev != null) {
                manager.isSpeakerphoneOn = prev
            }
        } catch (e: Exception) {
            AppLogger.e("AudioManager output route restore failed", e)
        } finally {
            previousSpeakerOn = null
        }
    }

    private fun restoreCommunicationMode() {
        val manager = audioManager ?: return
        val prev = previousAudioMode ?: return
        try {
            if (manager.mode != prev) {
                manager.mode = prev
            }
        } catch (e: Exception) {
            AppLogger.e("AudioManager mode restore failed", e)
        } finally {
            previousAudioMode = null
        }
    }

    private fun passesClassicVadGate(audio: FloatArray): Boolean {
        if (!classicVadEnabled) return true
        val minVoicedMs = 200
        val minVoicedRatio = 0.2
        val speechThreshold = 0.03
        val minSpeechMs = 600
        val maxSpeechMs = 12000
        val durationMs = audio.size * 1000 / sampleRate
        if (durationMs !in minSpeechMs..maxSpeechMs) return false
        val frameSize = 160
        var voicedMs = 0
        var index = 0
        while (index < audio.size) {
            val end = min(index + frameSize, audio.size)
            var sumSq = 0.0
            for (i in index until end) {
                val v = audio[i]
                sumSq += v * v
            }
            val rms = sqrt(sumSq / (end - index).coerceAtLeast(1))
            if (rms > speechThreshold) {
                voicedMs += (end - index) * 1000 / sampleRate
            }
            index = end
        }
        val voicedRatio = if (durationMs > 0) voicedMs.toDouble() / durationMs else 0.0
        return voicedMs >= minVoicedMs && voicedRatio >= minVoicedRatio
    }

    private fun processRecognizedSegment(audio: FloatArray) {
        val rms = rmsEnergy(audio)
        val minSegmentEnergy = minSegmentRms
        if (rms < minSegmentEnergy) return
        scope.launch(Dispatchers.IO) asrTask@{
            val (effectiveAudio, effectiveSampleRate) = prepareSpeechEnhancedAudio(audio, sampleRate)
            if (effectiveAudio.isEmpty()) return@asrTask
            val effectiveRms = max(rms, rmsEnergy(effectiveAudio))
            val profileSnapshot = speakerProfiles
            if (speakerVerifyEnabled && profileSnapshot.isNotEmpty()) {
                val segEmbedding = SpeakerVerifier.computeEmbedding(context, effectiveAudio, effectiveSampleRate)
                    ?: return@asrTask
                var bestSimilarity = -1f
                for (profile in profileSnapshot) {
                    val similarity = SpeakerVerifier.cosineSimilarity(profile, segEmbedding)
                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity
                    }
                }
                speakerLastSimilarity = bestSimilarity
                val passed = bestSimilarity >= speakerVerifyThreshold
                notifySpeakerVerify(bestSimilarity, passed)
                if (!passed) return@asrTask
            }
            val rawText = try {
                asr?.transcribe(effectiveAudio, effectiveSampleRate) ?: ""
            } catch (e: Exception) {
                AppLogger.e("ASR failed", e)
                notifyError("ASR 失败: ${e.message}")
                ""
            }
            val text = filterAsrText(rawText, effectiveRms)
            if (text.isNotBlank()) {
                if (suppressAsrAutoSpeak) {
                    val id = nextResultId()
                    notifyResult(id, text)
                    notifyProgress(id, 1f)
                    return@asrTask
                }
                if (shouldSkipDuplicateTts(text)) {
                    AppLogger.i("Skip duplicate tts text=$text")
                    return@asrTask
                }
                val id = enqueueTts(text)
                notifyResult(id, text)
                ensureTtsLoop()
            }
        }
    }

    private fun maybeDecodeStreamingSenseVoice(window: List<Float>, nowMs: Long) {
        if (!pttStreamingEnabled) return
        if (asr == null) return
        if (!classicVadEnabled && !sileroVadEnabled) return
        val minSamples = sampleRate / 2
        if (window.size < minSamples) return
        val decodeIntervalMs = 260L
        if ((nowMs - lastStreamingDecodeAtMs) < decodeIntervalMs) return
        if (!streamingDecodeBusy.compareAndSet(false, true)) return
        lastStreamingDecodeAtMs = nowMs
        val maxSamples = sampleRate * 3
        val snapshot = if (window.size > maxSamples) {
            window.takeLast(maxSamples).toFloatArray()
        } else {
            window.toFloatArray()
        }
        if (sileroVadEnabled && !isSileroSpeechDetected()) {
            streamingDecodeBusy.set(false)
            return
        }
        val segmentRms = rmsEnergy(snapshot)
        if (classicVadEnabled) {
            val minStreamingRms = kotlin.math.max(0.010, minSegmentRms * 0.85)
            if (segmentRms < minStreamingRms) {
                streamingDecodeBusy.set(false)
                return
            }
            val tailSize = kotlin.math.min(snapshot.size, sampleRate / 4) // ~250ms
            if (tailSize <= 0) {
                streamingDecodeBusy.set(false)
                return
            }
            val tailStart = snapshot.size - tailSize
            var tailSum = 0.0
            var voicedCount = 0
            for (i in tailStart until snapshot.size) {
                val v = snapshot[i].toDouble()
                tailSum += v * v
                if (kotlin.math.abs(snapshot[i]) >= 0.02f) {
                    voicedCount++
                }
            }
            val tailRms = kotlin.math.sqrt(tailSum / tailSize)
            val minTailRms = kotlin.math.max(0.014, minSegmentRms * 0.65)
            val voicedRatio = voicedCount.toDouble() / tailSize.toDouble()
            if (tailRms < minTailRms || voicedRatio < 0.08) {
                streamingDecodeBusy.set(false)
                return
            }
        }
        scope.launch(Dispatchers.IO) {
            try {
                val raw = asr?.transcribe(snapshot, sampleRate).orEmpty()
                val text = filterAsrText(raw, segmentRms)
                if (text.isNotBlank()) {
                    notifyStreamingResult(text)
                }
            } catch (e: Exception) {
                AppLogger.e("ASR streaming failed", e)
            } finally {
                streamingDecodeBusy.set(false)
            }
        }
    }

    private fun startRecorderLoop() {
        applyCommunicationMode(useCommunicationMode)
        applyOutputRoutePreference()
        player.setUseCommunicationAttributes(useCommunicationMode)
        player.setPreferredOutputType(preferredOutputType)
        registerAudioDeviceCallback()
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioSource = if (useVoiceCommunication) {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        } else {
            MediaRecorder.AudioSource.MIC
        }
        val rec = AudioRecord(
            audioSource,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuf, 4096)
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            AppLogger.e("AudioRecord init failed state=${rec.state}")
            rec.release()
            notifyError("录音初始化失败")
            return
        }
        applyInputRoutePreference(rec)
        if (useVoiceCommunication && (!useAec3 || allowSystemAecWithAec3)) {
            try {
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(rec.audioSessionId)?.apply { enabled = true }
                    AppLogger.i("AEC enabled")
                } else {
                    AppLogger.i("AEC not available")
                }
            } catch (e: Exception) {
                AppLogger.e("AEC init failed", e)
            }
        } else if (useVoiceCommunication && useAec3) {
            AppLogger.i("AEC3 active, skip system AEC")
        }
        recorder = rec
        val buffer = ShortArray(2048)
        val window = mutableListOf<Float>()
        loopJob = scope.launch(Dispatchers.Default) {
            try {
                rec.startRecording()
                reportInputDevice(rec)
                updateOutputDeviceFromSystem()
                var silenceMs = 0
                var voicedMs = 0
                val minVoicedMs = 200
                val minVoicedRatio = 0.2
                val silenceThreshold = 0.015
                val speechThreshold = 0.03
                while (isActive) {
                    val read = rec.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    val floatBuf = FloatArray(read)
                    for (i in 0 until read) {
                        floatBuf[i] = buffer[i] / 32768f
                    }
                    if (useAec3) {
                        aec3?.processCapture(floatBuf, 0, read)
                        captureFrames.incrementAndGet()
                        lastCaptureMs.set(SystemClock.uptimeMillis())
                    }
                    applyNoiseSuppression(floatBuf, read)
                    var sumSq = 0.0
                    for (i in 0 until read) {
                        val v = floatBuf[i]
                        sumSq += v * v
                    }
                    val bufRms = if (read > 0) sqrt(sumSq / read) else 0.0
                    val now = SystemClock.uptimeMillis()
                    if (now - lastLevelReportMs >= 60L) {
                        lastLevelReportMs = now
                        notifyLevel(bufRms.toFloat())
                        if (useAec3) {
                            notifyAec3Diag(buildAec3Diag(now))
                        }
                    }
                    if (suppressWhilePlaying && (player.isPlaying || now < suppressUntilMs)) {
                        window.clear()
                        silenceMs = 0
                        voicedMs = 0
                        continue
                    }
                    val recognitionBuf = processRealtimeSpeechEnhancement(floatBuf, sampleRate)
                    if (recognitionBuf.isEmpty()) {
                        continue
                    }
                    for (sample in recognitionBuf) {
                        window.add(sample)
                    }
                    if (sileroVadEnabled) {
                        acceptSileroVadWaveform(recognitionBuf)
                        drainSileroVadSegments(flush = false).forEach { segment ->
                            if (classicVadEnabled && !passesClassicVadGate(segment)) return@forEach
                            processRecognizedSegment(segment)
                        }
                        val maxStreamingWindowSamples = sampleRate * 3
                        if (window.size > maxStreamingWindowSamples) {
                            val overflow = window.size - maxStreamingWindowSamples
                            if (overflow > 0) {
                                window.subList(0, overflow).clear()
                            }
                        }
                    }
                    maybeDecodeStreamingSenseVoice(window, now)
                    if (classicVadEnabled && !sileroVadEnabled) {
                        val energy = sqrt(window.takeLast(min(400, window.size)).map { it * it }.average())
                        val stepMs = recognitionBuf.size * 1000 / sampleRate
                        if (energy < silenceThreshold) {
                            silenceMs += stepMs
                        } else {
                            silenceMs = 0
                        }
                        if (energy > speechThreshold) {
                            voicedMs += stepMs
                        }
                        val minSpeechMs = 600
                        val maxSpeechMs = 12000
                        val durMs = window.size * 1000 / sampleRate
                        if (silenceMs > 400 && durMs in minSpeechMs..maxSpeechMs && !player.isPlaying) {
                            val voicedRatio = if (durMs > 0) voicedMs.toDouble() / durMs else 0.0
                            if (voicedMs < minVoicedMs || voicedRatio < minVoicedRatio) {
                                window.clear()
                                silenceMs = 0
                                voicedMs = 0
                                continue
                            }
                            val audio = window.toFloatArray()
                            window.clear()
                            silenceMs = 0
                            voicedMs = 0
                            processRecognizedSegment(audio)
                        }
                        if (durMs > maxSpeechMs) {
                            window.clear()
                            silenceMs = 0
                            voicedMs = 0
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Realtime loop failed", e)
                notifyError("实时转换异常: ${e.message}")
            }
        }
    }

    private fun applyInputRoutePreference(rec: AudioRecord) {
        if (Build.VERSION.SDK_INT < 23) return
        try {
            val manager = audioManager ?: return
            val devices = manager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val preferred = pickPreferredInputDevice(devices, preferredInputType)
            if (preferred != null) {
                val ok = rec.setPreferredDevice(preferred)
                AppLogger.i("Prefer input device: ${formatInputDeviceLabel(preferred)} result=$ok")
            } else if (preferredInputType != AudioRoutePreference.INPUT_AUTO) {
                AppLogger.i("Prefer input route: target type=$preferredInputType not found")
            }
        } catch (e: Exception) {
            AppLogger.e("Prefer input route failed", e)
        }
    }

    private fun reportInputDevice(rec: AudioRecord) {
        if (Build.VERSION.SDK_INT < 24) {
            notifyInputDevice("未知")
            return
        }
        val device = try {
            rec.routedDevice
        } catch (_: Exception) {
            null
        }
        notifyInputDevice(formatInputDeviceLabel(device))
    }

    private fun registerAudioDeviceCallback() {
        if (audioDeviceCallback != null) return
        if (Build.VERSION.SDK_INT < 23) return
        val manager = audioManager ?: return
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                recorder?.let { reportInputDevice(it) }
                updateOutputDeviceFromSystem()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                recorder?.let { reportInputDevice(it) }
                updateOutputDeviceFromSystem()
            }
        }
        audioDeviceCallback = callback
        val handler = Handler(Looper.getMainLooper())
        manager.registerAudioDeviceCallback(callback, handler)
    }

    private fun unregisterAudioDeviceCallback() {
        if (Build.VERSION.SDK_INT < 23) return
        val manager = audioManager ?: return
        val callback = audioDeviceCallback ?: return
        try {
            manager.unregisterAudioDeviceCallback(callback)
        } catch (_: Exception) {
        }
        audioDeviceCallback = null
    }

    private fun updateOutputDeviceFromSystem() {
        val manager = audioManager ?: return
        val devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        notifyOutputDevice(pickOutputDeviceLabel(devices))
    }

    private fun rmsEnergy(samples: FloatArray): Double {
        if (samples.isEmpty()) return 0.0
        var sum = 0.0
        for (s in samples) {
            sum += (s * s)
        }
        return sqrt(sum / samples.size)
    }

    private fun filterAsrText(raw: String, rms: Double): String {
        val text = raw.trim()
        if (text.isEmpty()) return ""
        val letters = text.count { it.isLetterOrDigit() }
        if (letters == 0) return ""
        if (letters <= 1 && rms < minSegmentRms) return ""
        return replaceNumbers(text)
    }

    private fun buildAec3Diag(nowMs: Long): String {
        val renderAge = nowMs - lastRenderMs.get()
        val captureAge = nowMs - lastCaptureMs.get()
        val renderCount = renderFrames.get()
        val captureCount = captureFrames.get()
        val renderLabel = if (renderCount == 0L) "未收到" else "${renderAge}ms前"
        val captureLabel = if (captureCount == 0L) "未收到" else "${captureAge}ms前"
        return "AEC3 诊断：渲染=$renderLabel($renderCount) 采集=$captureLabel($captureCount)"
    }

    private fun replaceNumbers(text: String): String {
        return when (numberReplaceMode) {
            1 -> text.replace(Regex("\\d+")) { match ->
                digitsToChineseChars(match.value)
            }
            2 -> text.replace(Regex("\\d+")) { match ->
                digitsToChineseExpression(match.value)
            }
            else -> text
        }
    }

    private fun digitsToChineseChars(digits: String): String {
        val map = charArrayOf('零', '一', '二', '三', '四', '五', '六', '七', '八', '九')
        val sb = StringBuilder(digits.length)
        for (ch in digits) {
            val idx = ch - '0'
            if (idx in 0..9) sb.append(map[idx]) else sb.append(ch)
        }
        return sb.toString()
    }

    private fun digitsToChineseExpression(digits: String): String {
        val trimmed = digits.trimStart('0')
        if (trimmed.isEmpty()) return "零"
        val groups = mutableListOf<String>()
        var idx = trimmed.length
        while (idx > 0) {
            val start = kotlin.math.max(0, idx - 4)
            groups.add(0, trimmed.substring(start, idx))
            idx = start
        }
        val values = groups.map { it.toIntOrNull() ?: 0 }
        val bigUnits = arrayOf("", "万", "亿", "兆", "京", "垓", "秭", "穰")
        val sb = StringBuilder()
        var zeroPending = false
        for (i in groups.indices) {
            val value = values[i]
            if (value == 0) {
                zeroPending = true
                continue
            }
            if (zeroPending && sb.isNotEmpty()) {
                sb.append("零")
            }
            var groupText = convertGroup(groups[i])
            val unitIdx = groups.size - 1 - i
            if (groupText == "二" && unitIdx > 0) {
                groupText = "两"
            }
            sb.append(groupText)
            if (unitIdx > 0) {
                sb.append(bigUnits.getOrElse(unitIdx) { "" })
            }
            zeroPending = false
            if (i < groups.lastIndex && value < 1000 && values[i + 1] > 0) {
                zeroPending = true
            }
        }
        return sb.toString()
    }

    private fun convertGroup(group: String): String {
        val units = arrayOf("", "十", "百", "千")
        val digits = group.toCharArray()
        val sb = StringBuilder()
        var zeroPending = false
        val len = digits.size
        for (i in 0 until len) {
            val d = digits[i] - '0'
            val pos = len - 1 - i
            if (d == 0) {
                if (sb.isNotEmpty()) zeroPending = true
                continue
            }
            if (zeroPending) {
                sb.append("零")
                zeroPending = false
            }
            if (pos == 1 && d == 1 && sb.isEmpty()) {
                sb.append("十")
            } else {
                val digitChar = if (d == 2 && pos >= 2) "两" else when (d) {
                    0 -> "零"
                    1 -> "一"
                    2 -> "二"
                    3 -> "三"
                    4 -> "四"
                    5 -> "五"
                    6 -> "六"
                    7 -> "七"
                    8 -> "八"
                    else -> "九"
                }
                sb.append(digitChar).append(units[pos])
            }
        }
        return sb.toString()
    }
}

private fun pickPreferredInputDevice(devices: Array<AudioDeviceInfo>, pref: Int): AudioDeviceInfo? {
    if (pref == AudioRoutePreference.INPUT_AUTO) return null
    fun find(types: Set<Int>): AudioDeviceInfo? = devices.firstOrNull { it.type in types }
    return when (pref) {
        AudioRoutePreference.INPUT_BUILTIN_MIC -> find(
            setOf(AudioDeviceInfo.TYPE_BUILTIN_MIC, AudioDeviceInfo.TYPE_TELEPHONY)
        )
        AudioRoutePreference.INPUT_USB -> find(
            setOf(AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET)
        )
        AudioRoutePreference.INPUT_BLUETOOTH -> find(
            setOf(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET)
        )
        AudioRoutePreference.INPUT_WIRED -> find(
            setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_LINE_ANALOG)
        )
        else -> null
    }
}

private fun pickPreferredOutputDevice(devices: Array<AudioDeviceInfo>, pref: Int): AudioDeviceInfo? {
    if (pref == AudioRoutePreference.OUTPUT_AUTO) return null
    fun find(types: Set<Int>): AudioDeviceInfo? = devices.firstOrNull { it.type in types }
    return when (pref) {
        AudioRoutePreference.OUTPUT_SPEAKER -> find(
            setOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE)
        )
        AudioRoutePreference.OUTPUT_EARPIECE -> find(
            setOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        )
        AudioRoutePreference.OUTPUT_BLUETOOTH -> find(
            setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET)
        )
        AudioRoutePreference.OUTPUT_USB -> find(
            setOf(AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET)
        )
        AudioRoutePreference.OUTPUT_WIRED -> find(
            setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_LINE_ANALOG)
        )
        else -> null
    }
}

private fun formatInputDeviceLabel(device: AudioDeviceInfo?): String {
    if (device == null) return "未知"
    val typeName = when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "内置麦克风"
        AudioDeviceInfo.TYPE_TELEPHONY -> "话筒"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB麦克风"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET -> "蓝牙麦克风"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_LINE_ANALOG -> "有线麦克风"
        else -> "设备(${device.type})"
    }
    val name = device.productName?.toString()?.trim().orEmpty()
    return if (name.isNotEmpty()) "$typeName - $name" else typeName
}

private fun formatOutputDeviceLabel(device: AudioDeviceInfo?): String {
    if (device == null) return "未知"
    val typeName = when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "扬声器"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "听筒"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_LINE_ANALOG -> "有线耳机"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET -> "蓝牙耳机"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB音频"
        else -> "设备(${device.type})"
    }
    val name = device.productName?.toString()?.trim().orEmpty()
    return if (name.isNotEmpty()) "$typeName - $name" else typeName
}

private fun pickOutputDeviceLabel(devices: Array<AudioDeviceInfo>): String {
    if (devices.isEmpty()) return "未知"
    fun find(types: Set<Int>): AudioDeviceInfo? = devices.firstOrNull { it.type in types }
    val device = find(setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
        ?: find(setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
        ?: find(setOf(AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET))
        ?: find(setOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        ?: find(setOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE))
        ?: devices.first()
    return formatOutputDeviceLabel(device)
}
