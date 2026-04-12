package com.lhtstudio.kigtts.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import com.lhtstudio.kigtts.app.audio.RealtimeController
import com.lhtstudio.kigtts.app.audio.SpeakerEnrollResult
import com.lhtstudio.kigtts.app.data.ModelRepository
import com.lhtstudio.kigtts.app.data.SYSTEM_TTS_VOICE_NAME
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.data.isSystemTtsVoiceDir
import com.lhtstudio.kigtts.app.overlay.OverlayBridge
import com.lhtstudio.kigtts.app.overlay.RealtimeOwnerGate
import com.lhtstudio.kigtts.app.overlay.RealtimeRuntimeBridge
import com.lhtstudio.kigtts.app.ui.ExternalQuickSubtitleRequest
import com.lhtstudio.kigtts.app.ui.RecognizedItem
import com.lhtstudio.kigtts.app.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

data class RealtimeHostState(
    val asrDir: File? = null,
    val voiceDir: File? = null,
    val running: Boolean = false,
    val status: String = "",
    val recognized: List<RecognizedItem> = emptyList(),
    val inputLevel: Float = 0f,
    val playbackProgress: Float = 0f,
    val inputDeviceLabel: String = "",
    val outputDeviceLabel: String = "",
    val pushToTalkPressed: Boolean = false,
    val pushToTalkStreamingText: String = "",
    val aec3Status: String = "未启用",
    val aec3Diag: String = "AEC3 诊断：未启用",
    val speakerLastSimilarity: Float = -1f
)

class RealtimeHostService : Service(), RealtimeRuntimeBridge.AppDelegate {
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repo: ModelRepository

    private var controller: RealtimeController? = null
    private var settingsJob: Job? = null
    private var currentSettings = UserPrefs.AppSettings()
    private var speakerProfiles = mutableListOf<UserPrefs.SpeakerVerifyProfile>()
    private val lastProgressUpdateAtMs = mutableMapOf<Long, Long>()
    private var lastLevelUpdateAtMs = 0L
    private var pttSessionLastText = ""
    private var pttSessionCommitConsumed = false
    private var lastPttHistoryTextKey = ""
    private var lastPttHistoryAtMs = 0L
    private var manualRecognizedIdSeed = -1L
    private var quickSubtitlePlayOnSend = true

    private val _state = MutableStateFlow(RealtimeHostState())
    private val _quickSubtitleRequests = MutableStateFlow<ExternalQuickSubtitleRequest?>(null)

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("RealtimeHostService.onCreate")
        repo = ModelRepository(applicationContext)
        RealtimeRuntimeBridge.registerAppDelegate(this)
        observeSettings()
        initializeSelections()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        settingsJob = null
        RealtimeRuntimeBridge.unregisterAppDelegate(this)
        val activeController = controller
        controller = null
        if (activeController != null) {
            runCatching {
                kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                    activeController.stop()
                }
            }.onFailure {
                AppLogger.e("RealtimeHostService controller stop failed", it)
            }
        }
        RealtimeOwnerGate.release(APP_OWNER_TAG)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun stateFlow(): StateFlow<RealtimeHostState> = _state.asStateFlow()

    fun quickSubtitleRequestFlow(): StateFlow<ExternalQuickSubtitleRequest?> = _quickSubtitleRequests.asStateFlow()

    fun consumeQuickSubtitleRequest(requestId: Long) {
        if (_quickSubtitleRequests.value?.requestId == requestId) {
            _quickSubtitleRequests.value = null
        }
    }

    suspend fun updateSelectedAsrDir(dir: File?, status: String? = null, preload: Boolean = true) {
        updateState {
            it.copy(
                asrDir = dir,
                status = status ?: it.status
            )
        }
        if (preload && dir != null) {
            val loaded = withContext(Dispatchers.IO) { ensureController().loadAsr(dir) }
            if (!loaded && currentState().asrDir?.absolutePath == dir.absolutePath) {
                updateStatus("ASR 模型加载失败")
            }
        }
    }

    suspend fun updateSelectedVoiceDir(dir: File?, status: String? = null, preload: Boolean = true) {
        updateState {
            it.copy(
                voiceDir = dir,
                status = status ?: it.status
            )
        }
        if (preload && dir != null) {
            val loaded = withContext(Dispatchers.IO) { ensureController().loadTts(dir) }
            if (!loaded && currentState().voiceDir?.absolutePath == dir.absolutePath) {
                updateStatus(
                    if (isSystemTtsVoiceDir(dir)) {
                        "系统 TTS 初始化失败，请先完成系统 TTS 设置"
                    } else {
                        "音色包加载失败"
                    }
                )
            }
        }
    }

    suspend fun speakText(text: String): Long? {
        val message = text.trim()
        if (message.isEmpty()) return null
        val voice = currentState().voiceDir ?: return null
        return withContext(Dispatchers.IO) {
            val activeController = ensureController()
            if (!activeController.loadTts(voice)) return@withContext null
            activeController.enqueueSpeakText(message)
        }
    }

    suspend fun enrollSpeaker(
        durationSec: Float,
        onCapture: ((progress: Float, level: Float) -> Unit)? = null
    ): SpeakerEnrollResult {
        return withContext(Dispatchers.IO) {
            ensureController().enrollSpeaker(durationSec) { progress, level ->
                if (onCapture != null) {
                    serviceScope.launch(Dispatchers.Main.immediate) {
                        onCapture(progress, level)
                    }
                }
            }
        }
    }

    fun isMicActive(): Boolean = controller?.isMicActive() == true

    fun setQuickSubtitlePlayOnSend(enabled: Boolean) {
        quickSubtitlePlayOnSend = enabled
    }

    fun setSuppressWhilePlaying(enabled: Boolean) {
        controller?.setSuppressWhilePlaying(enabled)
    }

    fun setSuppressDelaySec(seconds: Float) {
        controller?.setSuppressDelaySec(seconds)
    }

    fun setMinVolumePercent(percent: Int) {
        controller?.setMinVolumePercent(percent)
    }

    fun setPlaybackGainPercent(percent: Int) {
        controller?.setPlaybackGainPercent(percent)
    }

    fun setPiperNoiseScale(value: Float) {
        controller?.setPiperNoiseScale(value)
    }

    fun setPiperLengthScale(value: Float) {
        controller?.setPiperLengthScale(value)
    }

    fun setPiperNoiseW(value: Float) {
        controller?.setPiperNoiseW(value)
    }

    fun setPiperSentenceSilenceSec(value: Float) {
        controller?.setPiperSentenceSilenceSec(value)
    }

    fun setUseVoiceCommunication(enabled: Boolean) {
        controller?.setUseVoiceCommunication(enabled)
    }

    fun setCommunicationMode(enabled: Boolean) {
        controller?.setCommunicationMode(enabled)
    }

    fun setPreferredInputType(type: Int) {
        controller?.setPreferredInputType(type)
    }

    fun setPreferredOutputType(type: Int) {
        controller?.setPreferredOutputType(type)
    }

    fun setUseAec3(enabled: Boolean) {
        controller?.setUseAec3(enabled)
    }

    fun setDenoiserMode(mode: Int) {
        controller?.setDenoiserMode(mode)
    }

    fun setNumberReplaceMode(mode: Int) {
        controller?.setNumberReplaceMode(mode)
    }

    fun setPushToTalkStreamingEnabled(enabled: Boolean) {
        controller?.setPushToTalkStreamingEnabled(enabled)
    }

    fun setSuppressAsrAutoSpeak(enabled: Boolean) {
        controller?.setSuppressAsrAutoSpeak(enabled)
    }

    fun setSpeakerVerifyEnabled(enabled: Boolean) {
        controller?.setSpeakerVerifyEnabled(enabled)
    }

    fun setSpeakerVerifyThreshold(threshold: Float) {
        controller?.setSpeakerVerifyThreshold(threshold)
    }

    fun setSpeakerProfiles(profiles: List<FloatArray>) {
        controller?.setSpeakerProfiles(profiles)
    }

    fun clearSpeakerProfiles() {
        controller?.clearSpeakerProfiles()
    }

    suspend fun restartRecorder() {
        withContext(Dispatchers.IO) {
            controller?.restartRecorder()
        }
    }

    suspend fun stopForVoicePackDeletion() {
        withContext(Dispatchers.IO) {
            controller?.stopMic()
        }
        RealtimeOwnerGate.release(APP_OWNER_TAG)
        KeepAliveService.stop(applicationContext)
        lastProgressUpdateAtMs.clear()
        lastLevelUpdateAtMs = 0L
        pttSessionLastText = ""
        pttSessionCommitConsumed = false
        updateState {
            it.copy(
                running = false,
                status = "当前语音包已删除，麦克风已停止",
                inputLevel = 0f,
                playbackProgress = 0f,
                pushToTalkPressed = false,
                pushToTalkStreamingText = ""
            )
        }
    }

    override fun startRealtime() {
        serviceScope.launch {
            startRealtimeInternal()
        }
    }

    override fun stopRealtime() {
        serviceScope.launch {
            stopRealtimeInternal()
        }
    }

    override fun submitQuickSubtitle(target: String, text: String) {
        val normalized = text.trim()
        if (!isOverlayOpenTarget(target) && normalized.isEmpty()) return
        when (target) {
            OverlayBridge.TARGET_INPUT -> {
                if (normalized.isNotEmpty()) {
                    appendRecognizedHistory(normalized)
                }
            }
            OverlayBridge.TARGET_SUBTITLE -> {
                if (normalized.isNotEmpty()) {
                    if (quickSubtitlePlayOnSend) {
                        serviceScope.launch {
                            enqueueSpeakAndAppendHistory(normalized)
                        }
                    } else {
                        appendRecognizedHistory(normalized)
                    }
                }
            }
        }
        emitQuickSubtitleRequest(target, normalized, navigateToPage = false)
    }

    private fun emitQuickSubtitleRequest(
        target: String,
        text: String,
        navigateToPage: Boolean = false
    ) {
        val request = ExternalQuickSubtitleRequest(
            requestId = SystemClock.uptimeMillis(),
            target = target,
            text = text,
            navigateToPage = navigateToPage
        )
        _quickSubtitleRequests.value = request
    }

    override fun beginPushToTalkSession() {
        pttSessionLastText = ""
        pttSessionCommitConsumed = false
        resetPttHistoryDedup()
        updateState { it.copy(pushToTalkStreamingText = "") }
    }

    override fun setPushToTalkPressed(pressed: Boolean) {
        updateState {
            it.copy(
                pushToTalkPressed = pressed,
                pushToTalkStreamingText = if (pressed) it.pushToTalkStreamingText else ""
            )
        }
        controller?.setPushToTalkStreamingEnabled(
            currentSettings.pushToTalkMode &&
                currentSettings.pushToTalkConfirmInput &&
                pressed
        )
    }

    override fun commitPushToTalkSession(action: RealtimeRuntimeBridge.PttCommitAction) {
        if (!currentSettings.pushToTalkConfirmInput) return
        if (pttSessionCommitConsumed) return
        pttSessionCommitConsumed = true
        val text = currentState().pushToTalkStreamingText.trim().ifBlank { pttSessionLastText.trim() }
        when (action) {
            RealtimeRuntimeBridge.PttCommitAction.SendToSubtitle -> {
                if (text.isNotEmpty()) {
                    if (!quickSubtitlePlayOnSend) {
                        appendRecognizedHistory(text)
                    } else {
                        serviceScope.launch {
                            enqueueSpeakAndAppendHistory(text)
                        }
                    }
                    emitQuickSubtitleRequest(
                        OverlayBridge.TARGET_SUBTITLE,
                        text,
                        navigateToPage = false
                    )
                }
            }
            RealtimeRuntimeBridge.PttCommitAction.SendToInput -> {
                if (text.isNotEmpty()) {
                    appendRecognizedHistory(text)
                    emitQuickSubtitleRequest(
                        OverlayBridge.TARGET_INPUT,
                        text,
                        navigateToPage = false
                    )
                }
            }
            RealtimeRuntimeBridge.PttCommitAction.Cancel -> Unit
        }
        pttSessionLastText = ""
        resetPttHistoryDedup()
        updateState {
            it.copy(
                pushToTalkPressed = false,
                pushToTalkStreamingText = ""
            )
        }
        controller?.setPushToTalkStreamingEnabled(false)
    }

    private fun initializeSelections() {
        serviceScope.launch {
            val settings = UserPrefs.getSettings(applicationContext)
            currentSettings = settings
            speakerProfiles = UserPrefs.parseSpeakerVerifyProfiles(settings.speakerVerifyProfileCsv).toMutableList()
            quickSubtitlePlayOnSend = loadQuickSubtitlePlayOnSend()
            val asrDir = loadInitialAsrDir()
            val voiceDir = loadInitialVoiceDir()
            updateState {
                it.copy(
                    asrDir = asrDir,
                    voiceDir = voiceDir,
                    status = buildList {
                        if (asrDir != null) add("已加载 ASR")
                        if (voiceDir != null) add(if (isSystemTtsVoiceDir(voiceDir)) "已加载系统 TTS" else "已加载音色包")
                    }.joinToString(" / ")
                )
            }
            if (asrDir != null) {
                withContext(Dispatchers.IO) {
                    ensureController().loadAsr(asrDir)
                }
            }
            if (voiceDir != null) {
                val loaded = withContext(Dispatchers.IO) {
                    ensureController().loadTts(voiceDir)
                }
                if (!loaded) {
                    updateStatus(
                        if (isSystemTtsVoiceDir(voiceDir)) {
                            "系统 TTS 初始化失败，请先完成系统 TTS 设置"
                        } else {
                            "音色包加载失败"
                        }
                    )
                }
            }
        }
    }

    private suspend fun loadQuickSubtitlePlayOnSend(): Boolean {
        return runCatching {
            val raw = UserPrefs.getQuickSubtitleConfig(applicationContext)
            if (raw.isNullOrBlank()) true else JSONObject(raw).optBoolean("playOnSend", true)
        }.getOrDefault(true)
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = serviceScope.launch {
            UserPrefs.observeSettings(this@RealtimeHostService).collectLatest { next ->
                val previous = currentSettings
                currentSettings = next
                speakerProfiles = UserPrefs.parseSpeakerVerifyProfiles(next.speakerVerifyProfileCsv).toMutableList()
                applySettingsToController(next)
                if (
                    controller != null &&
                    (previous.echoSuppression != next.echoSuppression ||
                        previous.communicationMode != next.communicationMode ||
                        previous.preferredInputType != next.preferredInputType ||
                        previous.preferredOutputType != next.preferredOutputType)
                ) {
                    withContext(Dispatchers.IO) {
                        controller?.restartRecorder()
                    }
                }
                if (currentState().running) {
                    if (next.keepAlive) KeepAliveService.start(applicationContext)
                    else KeepAliveService.stop(applicationContext)
                }
            }
        }
    }

    private fun ensureController(): RealtimeController {
        controller?.let { return it }
        val created = RealtimeController(
            applicationContext,
            serviceScope,
            onResult = { id, text ->
                val normalized = text.trim()
                val isPttConfirmMode = currentSettings.pushToTalkMode && currentSettings.pushToTalkConfirmInput
                val isPttConfirmSessionOpen = isPttConfirmMode && !pttSessionCommitConsumed
                val isPttConfirmPressed = isPttConfirmSessionOpen && currentState().pushToTalkPressed
                if (!isPttConfirmMode && normalized.isNotEmpty()) {
                    appendRecognizedHistory(normalized, id)
                    if (currentSettings.asrSendToQuickSubtitle) {
                    emitQuickSubtitleRequest(
                        OverlayBridge.TARGET_SUBTITLE,
                        normalized,
                        navigateToPage = false
                    )
                }
            }
                if (isPttConfirmMode) {
                    if (isPttConfirmPressed && normalized.isNotEmpty()) {
                        appendPttFinalTranscript(normalized)
                    }
                }
            },
            onStreamingResult = { text ->
                val normalized = text.trim()
                if (normalized.isEmpty()) return@RealtimeController
                if (
                    currentSettings.pushToTalkMode &&
                    currentSettings.pushToTalkConfirmInput &&
                    !pttSessionCommitConsumed &&
                    currentState().pushToTalkPressed
                ) {
                    updatePttPreviewTranscript(normalized)
                }
            },
            onProgress = { id, progress ->
                val items = currentState().recognized
                val idx = items.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    val current = items[idx]
                    val nextProgress = maxOf(current.progress, progress.coerceIn(0f, 1f))
                    val progressDelta = nextProgress - current.progress
                    val now = SystemClock.elapsedRealtime()
                    val last = lastProgressUpdateAtMs[id] ?: 0L
                    val intervalReady = (now - last) >= PROGRESS_UPDATE_INTERVAL_MS || nextProgress >= 0.99f
                    if (progressDelta >= PROGRESS_UPDATE_DELTA && intervalReady) {
                        lastProgressUpdateAtMs[id] = now
                        val next = items.toMutableList()
                        next[idx] = current.copy(progress = nextProgress)
                        updateState {
                            it.copy(
                                recognized = next,
                                playbackProgress = progress.coerceIn(0f, 1f)
                            )
                        }
                    } else {
                        updateState { it.copy(playbackProgress = progress.coerceIn(0f, 1f)) }
                    }
                } else {
                    updateState { it.copy(playbackProgress = progress.coerceIn(0f, 1f)) }
                }
            },
            onLevel = { level ->
                val next = level.coerceIn(0f, 1f)
                val now = SystemClock.elapsedRealtime()
                val prev = currentState().inputLevel
                val delta = abs(prev - next)
                val intervalReady = (now - lastLevelUpdateAtMs) >= LEVEL_UPDATE_INTERVAL_MS
                if (delta >= LEVEL_UPDATE_DELTA || intervalReady) {
                    lastLevelUpdateAtMs = now
                    updateState { it.copy(inputLevel = next) }
                }
            },
            onInputDevice = { label ->
                if (label != currentState().inputDeviceLabel) {
                    updateState { it.copy(inputDeviceLabel = label) }
                }
            },
            onOutputDevice = { label ->
                if (label != currentState().outputDeviceLabel) {
                    updateState { it.copy(outputDeviceLabel = label) }
                }
            },
            onAec3Status = { status ->
                if (status != currentState().aec3Status) {
                    updateState { it.copy(aec3Status = status) }
                }
            },
            onAec3Diag = { diag ->
                if (diag != currentState().aec3Diag) {
                    updateState { it.copy(aec3Diag = diag) }
                }
            },
            onSpeakerVerify = { similarity, passed ->
                updateState { it.copy(speakerLastSimilarity = similarity) }
                if (!passed && currentSettings.speakerVerifyEnabled) {
                    updateStatus("说话人验证未通过(${String.format("%.2f", similarity)})")
                }
            },
            onError = { msg ->
                updateState {
                    it.copy(
                        running = false,
                        status = msg
                    )
                }
            },
            initialSuppressWhilePlaying = currentSettings.muteWhilePlaying,
            initialUseVoiceCommunication = currentSettings.echoSuppression,
            initialCommunicationMode = currentSettings.communicationMode,
            initialMinVolumePercent = currentSettings.minVolumePercent,
            initialPlaybackGainPercent = currentSettings.playbackGainPercent,
            initialPiperNoiseScale = currentSettings.piperNoiseScale,
            initialPiperLengthScale = currentSettings.piperLengthScale,
            initialPiperNoiseW = currentSettings.piperNoiseW,
            initialPiperSentenceSilenceSec = currentSettings.piperSentenceSilence,
            initialSuppressDelaySec = currentSettings.muteWhilePlayingDelaySec,
            initialPreferredInputType = currentSettings.preferredInputType,
            initialPreferredOutputType = currentSettings.preferredOutputType,
            initialUseAec3 = currentSettings.aec3Enabled,
            initialDenoiserMode = currentSettings.denoiserMode,
            initialNumberReplaceMode = currentSettings.numberReplaceMode,
            initialAllowSystemAecWithAec3 = currentSettings.allowSystemAecWithAec3,
            initialSpeakerVerifyEnabled = currentSettings.speakerVerifyEnabled && speakerProfiles.isNotEmpty(),
            initialSpeakerVerifyThreshold = currentSettings.speakerVerifyThreshold,
            initialSpeakerProfiles = speakerProfiles.map { it.vector.copyOf() }
        )
        created.setPushToTalkStreamingEnabled(
            currentSettings.pushToTalkMode &&
                currentSettings.pushToTalkConfirmInput &&
                currentState().pushToTalkPressed
        )
        created.setSuppressAsrAutoSpeak(
            currentSettings.pushToTalkMode && currentSettings.pushToTalkConfirmInput
        )
        controller = created
        return created
    }

    private suspend fun startRealtimeInternal(): Boolean {
        val asr = currentState().asrDir
        val voice = currentState().voiceDir
        if (asr == null || voice == null) {
            updateStatus("请先导入 ASR 模型和 voicepack")
            return false
        }
        if (currentState().running) return true
        if (!RealtimeOwnerGate.acquire(APP_OWNER_TAG)) {
            updateStatus("麦克风已被其它入口占用")
            return false
        }
        updateState {
            it.copy(
                running = true,
                status = "启动麦克风中",
                inputLevel = 0f,
                playbackProgress = 0f
            )
        }
        lastProgressUpdateAtMs.clear()
        lastLevelUpdateAtMs = 0L
        val started = withContext(Dispatchers.IO) {
            val activeController = ensureController()
            if (!activeController.loadAsr(asr)) return@withContext false
            if (!activeController.loadTts(voice)) return@withContext false
            activeController.startMic()
        }
        if (started && currentState().running) {
            updateStatus("运行中")
            if (currentSettings.keepAlive) {
                KeepAliveService.start(applicationContext)
            }
            return true
        }
        RealtimeOwnerGate.release(APP_OWNER_TAG)
        KeepAliveService.stop(applicationContext)
        updateState {
            it.copy(
                running = false,
                status = "麦克风启动失败",
                inputLevel = 0f,
                playbackProgress = 0f
            )
        }
        return false
    }

    private suspend fun stopRealtimeInternal() {
        withContext(Dispatchers.IO) {
            controller?.stopMic()
        }
        RealtimeOwnerGate.release(APP_OWNER_TAG)
        KeepAliveService.stop(applicationContext)
        pttSessionLastText = ""
        pttSessionCommitConsumed = false
        resetPttHistoryDedup()
        updateState {
            it.copy(
                running = false,
                status = "麦克风已停止",
                inputLevel = 0f,
                playbackProgress = 0f,
                pushToTalkPressed = false,
                pushToTalkStreamingText = ""
            )
        }
    }

    private fun applySettingsToController(settings: UserPrefs.AppSettings) {
        controller?.setSuppressWhilePlaying(settings.muteWhilePlaying)
        controller?.setSuppressDelaySec(settings.muteWhilePlayingDelaySec)
        controller?.setMinVolumePercent(settings.minVolumePercent)
        controller?.setPlaybackGainPercent(settings.playbackGainPercent)
        controller?.setPiperNoiseScale(settings.piperNoiseScale)
        controller?.setPiperLengthScale(settings.piperLengthScale)
        controller?.setPiperNoiseW(settings.piperNoiseW)
        controller?.setPiperSentenceSilenceSec(settings.piperSentenceSilence)
        controller?.setUseAec3(settings.aec3Enabled)
        controller?.setUseVoiceCommunication(settings.echoSuppression)
        controller?.setCommunicationMode(settings.communicationMode)
        controller?.setPreferredInputType(settings.preferredInputType)
        controller?.setPreferredOutputType(settings.preferredOutputType)
        controller?.setDenoiserMode(settings.denoiserMode)
        controller?.setNumberReplaceMode(settings.numberReplaceMode)
        controller?.setAllowSystemAecWithAec3(settings.allowSystemAecWithAec3)
        controller?.setSpeakerVerifyEnabled(settings.speakerVerifyEnabled && speakerProfiles.isNotEmpty())
        controller?.setSpeakerVerifyThreshold(settings.speakerVerifyThreshold)
        controller?.setSpeakerProfiles(speakerProfiles.map { it.vector.copyOf() })
        controller?.setSuppressAsrAutoSpeak(
            settings.pushToTalkMode && settings.pushToTalkConfirmInput
        )
        controller?.setPushToTalkStreamingEnabled(
            settings.pushToTalkMode &&
                settings.pushToTalkConfirmInput &&
                currentState().pushToTalkPressed
        )
    }

    private suspend fun loadInitialAsrDir(): File? {
        val lastName = UserPrefs.getLastAsrName(applicationContext)
        val resolved = lastName?.let { repo.resolveAsr(it) }
        return resolved ?: withContext(Dispatchers.IO) { repo.ensureBundledAsr() }
    }

    private suspend fun loadInitialVoiceDir(): File? {
        val lastName = UserPrefs.getLastVoiceName(applicationContext)
        val resolved = when (lastName) {
            SYSTEM_TTS_VOICE_NAME -> repo.systemTtsVirtualDir()
            null -> null
            else -> repo.resolveVoicePack(lastName)
        }
        return resolved ?: repo.systemTtsVirtualDir()
    }

    private fun appendRecognizedHistory(text: String, id: Long? = null) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        val historyId = id ?: manualRecognizedIdSeed--
        if (id != null && currentState().recognized.any { it.id == id }) return
        val item = RecognizedItem(id = historyId, text = normalized)
        val next = (listOf(item) + currentState().recognized).take(MAX_RECOGNIZED_ITEMS)
        lastProgressUpdateAtMs.keys.retainAll(next.asSequence().map { it.id }.toSet())
        updateState { it.copy(recognized = next) }
    }

    private suspend fun enqueueSpeakAndAppendHistory(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return
        val queuedId = speakText(message)
        if (queuedId != null) {
            appendRecognizedHistory(message, queuedId)
            updateStatus("已加入朗读队列")
        } else {
            appendRecognizedHistory(message)
        }
    }

    private fun mergePttTranscript(existing: String, incoming: String): String {
        val a = existing.trim()
        val b = incoming.trim()
        if (a.isEmpty()) return b
        if (b.isEmpty()) return a
        if (a == b) return a
        if (b.startsWith(a)) return b
        if (a.startsWith(b)) return a
        if (a.contains(b)) return a
        if (b.contains(a)) return b
        val overlapMax = minOf(a.length, b.length)
        for (k in overlapMax downTo 1) {
            if (a.regionMatches(a.length - k, b, 0, k, ignoreCase = false)) {
                return (a + b.substring(k)).trim()
            }
        }
        return (a + b).replace(Regex("\\s+"), "").trim()
    }

    private fun appendPttFinalTranscript(text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        val merged = mergePttTranscript(pttSessionLastText, normalized)
        pttSessionLastText = merged
        if (merged != currentState().pushToTalkStreamingText) {
            updateState { it.copy(pushToTalkStreamingText = merged) }
        }
    }

    private fun updatePttPreviewTranscript(text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        val preview = mergePttTranscript(pttSessionLastText, normalized)
        if (preview != currentState().pushToTalkStreamingText) {
            updateState { it.copy(pushToTalkStreamingText = preview) }
        }
    }

    private fun normalizePttHistoryKey(text: String): String {
        return text.trim().trimEnd('。', '！', '？', '!', '?', '，', ',', '；', ';', '、', '.')
    }

    private fun resetPttHistoryDedup() {
        lastPttHistoryTextKey = ""
        lastPttHistoryAtMs = 0L
    }

    private fun updateStatus(status: String) {
        updateState { it.copy(status = status) }
    }

    private fun updateState(transform: (RealtimeHostState) -> RealtimeHostState) {
        _state.update(transform)
        val snapshot = _state.value
        RealtimeRuntimeBridge.updateAppSnapshot(
            RealtimeRuntimeBridge.Snapshot(
                running = snapshot.running,
                latestRecognizedText = snapshot.recognized.firstOrNull()?.text.orEmpty(),
                inputLevel = snapshot.inputLevel,
                playbackProgress = snapshot.playbackProgress,
                inputDeviceLabel = snapshot.inputDeviceLabel,
                outputDeviceLabel = snapshot.outputDeviceLabel,
                pushToTalkPressed = snapshot.pushToTalkPressed,
                pushToTalkStreamingText = snapshot.pushToTalkStreamingText
            )
        )
    }

    private fun currentState(): RealtimeHostState = _state.value

    private fun isOverlayOpenTarget(target: String): Boolean {
        return target == OverlayBridge.TARGET_OPEN || target == OverlayBridge.TARGET_INPUT
    }

    inner class LocalBinder : Binder() {
        fun getService(): RealtimeHostService = this@RealtimeHostService
    }

    companion object {
        private const val APP_OWNER_TAG = RealtimeRuntimeBridge.APP_OWNER_TAG
        private const val MAX_RECOGNIZED_ITEMS = 100
        private const val LEVEL_UPDATE_INTERVAL_MS = 33L
        private const val LEVEL_UPDATE_DELTA = 0.02f
        private const val PROGRESS_UPDATE_INTERVAL_MS = 48L
        private const val PROGRESS_UPDATE_DELTA = 0.02f

        fun ensureStarted(context: Context) {
            runCatching {
                context.startService(Intent(context, RealtimeHostService::class.java))
            }.onFailure {
                AppLogger.e("RealtimeHostService.ensureStarted failed", it)
            }
        }
    }
}
