package com.kgtts.app.audio

import android.content.Context
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.kgtts.app.data.EspeakData
import com.kgtts.app.util.AppLogger
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
import java.util.concurrent.atomic.AtomicLong

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
}

interface SpeechModuleFactory {
    fun createAsr(context: Context, modelDir: File): AsrModule
    fun createTts(context: Context, packDir: File): TtsModule
}

object DefaultSpeechModuleFactory : SpeechModuleFactory {
    override fun createAsr(context: Context, modelDir: File): AsrModule = AsrEngine(context, modelDir)
    override fun createTts(context: Context, packDir: File): TtsModule = PiperTtsEngine(context, packDir)
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

    override fun synthesize(text: String): FloatArray {
        val ids = toIds(text)
        if (ids.isEmpty()) return FloatArray(0)
        val inputs = mutableMapOf<String, OnnxTensor>()
        val idLong = ids.map { it.toLong() }.toLongArray()
        val inputName = session.inputNames.firstOrNull { it.contains("input") } ?: session.inputNames.first()
        val lenName = session.inputNames.firstOrNull { it.contains("len") || it.contains("length") } ?: inputName + "_length"
        inputs[inputName] = OnnxTensor.createTensor(env, LongBuffer.wrap(idLong), longArrayOf(1, idLong.size.toLong()))
        inputs[lenName] = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(idLong.size.toLong())), longArrayOf(1))

        val scaleName = session.inputNames.firstOrNull { it.contains("scale") }
        if (scaleName != null) {
            val scales = FloatBuffer.wrap(floatArrayOf(0.667f, 1.0f, 0.8f))
            inputs[scaleName] = OnnxTensor.createTensor(env, scales, longArrayOf(3))
        }
        val sidName = session.inputNames.firstOrNull { it.contains("sid") }
        if (sidName != null) {
            inputs[sidName] = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(0)), longArrayOf(1))
        }

        session.run(inputs).use { results ->
            return unwrapAudio(results[0].value)
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

class RealtimeController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onResult: (Long, String) -> Unit,
    private val onProgress: (Long, Float) -> Unit,
    private val onLevel: (Float) -> Unit,
    private val onInputDevice: (String) -> Unit,
    private val onOutputDevice: (String) -> Unit,
    private val onAec3Status: (String) -> Unit,
    private val onAec3Diag: (String) -> Unit,
    private val onError: (String) -> Unit,
    initialSuppressWhilePlaying: Boolean,
    initialUseVoiceCommunication: Boolean,
    initialCommunicationMode: Boolean,
    initialMinVolumePercent: Int,
    initialPlaybackGainPercent: Int,
    initialSuppressDelaySec: Float,
    initialPreferredInputType: Int,
    initialPreferredOutputType: Int,
    initialUseAec3: Boolean,
    initialNumberReplaceMode: Int,
    initialAllowSystemAecWithAec3: Boolean,
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
    @Volatile private var suppressDelayMs = (initialSuppressDelaySec.coerceIn(0f, 5f) * 1000f).toLong()
    @Volatile private var suppressUntilMs: Long = 0L
    @Volatile private var preferredInputType = initialPreferredInputType
    @Volatile private var preferredOutputType = initialPreferredOutputType
    @Volatile private var useAec3 = initialUseAec3
    @Volatile private var numberReplaceMode = initialNumberReplaceMode.coerceIn(0, 2)
    @Volatile private var allowSystemAecWithAec3 = initialAllowSystemAecWithAec3
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
    private var currentVoiceDir: File? = null
    private var lastLevelReportMs: Long = 0L
    private val recorderMutex = Mutex()
    private var lastAcceptedTtsTextKey: String = ""
    private var lastAcceptedTtsAtMs: Long = 0L
    private val duplicateTtsWindowMs: Long = 1800L

    private data class QueuedTts(
        val id: Long,
        val text: String
    )

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

    fun setAllowSystemAecWithAec3(enabled: Boolean) {
        allowSystemAecWithAec3 = enabled
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
                    val pcm = tts?.synthesize(next.text) ?: FloatArray(0)
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
                notifyError("TTS 加载失败: ${e.message}")
                return@withLock false
            }
            true
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
            stopRecorderOnlyLocked()
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
        try {
            rec?.release()
        } catch (_: Exception) {
        }
        unregisterAudioDeviceCallback()
        restoreOutputRoutePreference()
        restoreCommunicationMode()
        if (aec3 == null) {
            notifyAec3Status(if (useAec3) "待启动" else "未启用")
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
                    for (i in 0 until read) {
                        window.add(floatBuf[i])
                    }
                    val energy = sqrt(window.takeLast(min(400, window.size)).map { it * it }.average())
                    val stepMs = read * 1000 / sampleRate
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
                        val rms = rmsEnergy(audio)
                        val minSegmentEnergy = minSegmentRms
                        if (rms >= minSegmentEnergy) {
                            scope.launch(Dispatchers.IO) asrTask@{
                                val rawText = try {
                                    asr?.transcribe(audio, sampleRate) ?: ""
                                } catch (e: Exception) {
                                    AppLogger.e("ASR failed", e)
                                    notifyError("ASR 失败: ${e.message}")
                                    ""
                                }
                                val text = filterAsrText(rawText, rms)
                                if (text.isNotBlank()) {
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
                    }
                    if (durMs > maxSpeechMs) {
                        window.clear()
                        silenceMs = 0
                        voicedMs = 0
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
