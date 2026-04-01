package com.kgtts.kgtts_app.audio

import android.content.Context
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.os.Build
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import com.kgtts.kgtts_app.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

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
    private val onError: (String) -> Unit,
    initialSuppressWhilePlaying: Boolean,
    initialUseVoiceCommunication: Boolean,
    initialCommunicationMode: Boolean,
    initialMinVolumePercent: Int,
    initialPlaybackGainPercent: Int,
    initialPiperNoiseScale: Float,
    initialPiperLengthScale: Float,
    initialPiperNoiseW: Float,
    initialPiperSentenceSilenceSec: Float,
    initialSuppressDelaySec: Float,
    initialPreferredInputType: Int,
    initialPreferredOutputType: Int,
    initialUseAec3: Boolean,
    initialNumberReplaceMode: Int,
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
    private var currentVoiceDir: File? = null
    private var lastLevelReportMs: Long = 0L
    private val recorderMutex = Mutex()
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

    fun setPushToTalkStreamingEnabled(enabled: Boolean) {
        pttStreamingEnabled = enabled
        if (!enabled) {
            lastStreamingDecodeAtMs = 0L
            streamingDecodeBusy.set(false)
        }
    }

    fun setSuppressAsrAutoSpeak(enabled: Boolean) {
        suppressAsrAutoSpeak = enabled
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
                notifyError("TTS 加载失败: ${e.message}")
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
            if (rms < 0.008) {
                return@withLock SpeakerEnrollResult(
                    success = false,
                    message = "说话人注册失败：音量过低，请靠近麦克风"
                )
            }
            val embedding = SpeakerVerifier.computeEmbedding(audio, sampleRate)
                ?: return@withLock SpeakerEnrollResult(
                    success = false,
                    message = "说话人注册失败：有效语音不足"
                )
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
        streamingDecodeBusy.set(false)
        lastStreamingDecodeAtMs = 0L
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

    private fun maybeDecodeStreamingSenseVoice(window: List<Float>, nowMs: Long) {
        if (!pttStreamingEnabled) return
        if (asr == null) return
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
        val segmentRms = rmsEnergy(snapshot)
        val minStreamingRms = kotlin.math.max(0.010, minSegmentRms * 0.85)
        if (segmentRms < minStreamingRms) {
            streamingDecodeBusy.set(false)
            return
        }
        val tailSize = kotlin.math.min(snapshot.size, sampleRate / 4)
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
                    maybeDecodeStreamingSenseVoice(window, now)
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
                                val profileSnapshot = speakerProfiles
                                if (speakerVerifyEnabled && profileSnapshot.isNotEmpty()) {
                                    val segEmbedding = SpeakerVerifier.computeEmbedding(audio, sampleRate)
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
                                    if (!passed) {
                                        return@asrTask
                                    }
                                }
                                val rawText = try {
                                    asr?.transcribe(audio, sampleRate) ?: ""
                                } catch (e: Exception) {
                                    AppLogger.e("ASR failed", e)
                                    notifyError("ASR 失败: ${e.message}")
                                    ""
                                }
                                val text = filterAsrText(rawText, rms)
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
