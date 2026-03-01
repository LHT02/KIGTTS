package com.kgtts.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.*
import androidx.compose.material.DropdownMenuItem as M2DropdownMenuItem
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kgtts.app.R
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.kgtts.app.audio.AudioRoutePreference
import com.kgtts.app.audio.RealtimeController
import com.kgtts.app.data.ModelRepository
import com.kgtts.app.data.VoicePackInfo
import com.kgtts.app.data.UserPrefs
import com.kgtts.app.service.KeepAliveService
import com.kgtts.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

private fun isXiaomiFamilyDevice(): Boolean {
    val m = Build.MANUFACTURER?.lowercase() ?: return false
    return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")
}

data class UiState(
    val asrDir: File? = null,
    val voiceDir: File? = null,
    val voicePacks: List<VoicePackInfo> = emptyList(),
    val recognized: List<RecognizedItem> = emptyList(),
    val running: Boolean = false,
    val status: String = "待命",
    val muteWhilePlaying: Boolean = false,
    val muteWhilePlayingDelaySec: Float = 0f,
    val echoSuppression: Boolean = false,
    val communicationMode: Boolean = false,
    val preferredInputType: Int = AudioRoutePreference.INPUT_AUTO,
    val preferredOutputType: Int = AudioRoutePreference.OUTPUT_AUTO,
    val aec3Enabled: Boolean = false,
    val aec3Status: String = "未启用",
    val aec3Diag: String = "AEC3 诊断：未启用",
    val minVolumePercent: Int = 0,
    val playbackGainPercent: Int = 100,
    val keepAlive: Boolean = false,
    val numberReplaceMode: Int = 0,
    val landscapeDrawerMode: Int = UserPrefs.DRAWER_MODE_PERMANENT,
    val solidTopBar: Boolean = true,
    val inputLevel: Float = 0f,
    val inputDeviceLabel: String = "未知",
    val outputDeviceLabel: String = "未知"
)

data class RecognizedItem(
    val id: Long,
    val text: String,
    val progress: Float = 0f
)

data class QuickSubtitleGroup(
    val id: Long,
    val title: String,
    val icon: String,
    val items: List<String>
)

data class DrawPoint(
    val x: Float,
    val y: Float
)

data class DrawStrokeData(
    val points: List<DrawPoint>,
    val color: Color,
    val width: Float,
    val eraser: Boolean
)

private fun defaultQuickSubtitleGroups(): List<QuickSubtitleGroup> = listOf(
    QuickSubtitleGroup(
        id = 1L,
        title = "常用",
        icon = "sentiment_satisfied",
        items = listOf(
            "您好，我现在不太方便说话",
            "您好，可以加个好友吗",
            "稍等一下，我马上回复您",
            "感谢理解，辛苦了"
        )
    ),
    QuickSubtitleGroup(
        id = 2L,
        title = "游戏",
        icon = "sports_esports",
        items = listOf(
            "我在组队，语音不方便",
            "请跟我走这边",
            "注意右侧有人",
            "这把打得很好"
        )
    ),
    QuickSubtitleGroup(
        id = 3L,
        title = "办公",
        icon = "work",
        items = listOf(
            "我在开会，稍后回复",
            "请把需求再发我一份",
            "这个我今天内处理",
            "收到，谢谢"
        )
    )
)

class MainViewModel(
    private val repo: ModelRepository,
    private val appContext: ComponentActivity
) : ViewModel() {
    var uiState by mutableStateOf(UiState())
        private set
    var realtimeRecognized by mutableStateOf<List<RecognizedItem>>(emptyList())
        private set
    var realtimeInputLevel by mutableFloatStateOf(0f)
        private set
    var realtimePlaybackProgress by mutableFloatStateOf(0f)
        private set

    private var controller: RealtimeController? = null
    private var restartJob: Job? = null
    private val lastProgressUpdateAtMs = mutableMapOf<Long, Long>()
    private var lastLevelUpdateAtMs = 0L

    private companion object {
        private const val LEVEL_UPDATE_INTERVAL_MS = 33L
        private const val LEVEL_UPDATE_DELTA = 0.02f
        private const val PROGRESS_UPDATE_INTERVAL_MS = 48L
        private const val PROGRESS_UPDATE_DELTA = 0.02f
        private const val MAX_RECOGNIZED_ITEMS = 100
    }
    val drawStrokes = mutableStateListOf<DrawStrokeData>()
    var drawColor by mutableStateOf(UiTokens.Primary)
        private set
    var drawBrushSize by mutableStateOf(12f)
        private set
    var drawEraserSize by mutableStateOf(12f)
        private set
    var drawEraser by mutableStateOf(false)
        private set
    var quickSubtitleGroups by mutableStateOf(defaultQuickSubtitleGroups())
        private set
    var quickSubtitleSelectedGroupId by mutableLongStateOf(1L)
        private set
    var quickSubtitleCurrentText by mutableStateOf("您好，我现在\n不太方便\n说话")
        private set
    var quickSubtitleInputText by mutableStateOf("")
        private set
    var quickSubtitlePlayOnSend by mutableStateOf(true)
        private set
    var quickSubtitleFontSizeSp by mutableFloatStateOf(56f)
        private set
    private var quickSubtitleNextGroupId = 4L
    private var quickSubtitleSaving = false

    init {
        loadQuickSubtitleConfig()
    }

    private fun loadQuickSubtitleConfig() {
        viewModelScope.launch {
            val raw = UserPrefs.getQuickSubtitleConfig(appContext)
            if (raw.isNullOrBlank()) return@launch
            runCatching {
                parseQuickSubtitleConfig(raw)
            }
        }
    }

    private fun parseQuickSubtitleConfig(raw: String) {
        val root = JSONObject(raw)
        val groupsArr = root.optJSONArray("groups") ?: JSONArray()
        val parsedGroups = mutableListOf<QuickSubtitleGroup>()
        var maxId = 0L
        for (i in 0 until groupsArr.length()) {
            val g = groupsArr.optJSONObject(i) ?: continue
            val id = g.optLong("id", i.toLong() + 1L).coerceAtLeast(1L)
            val title = g.optString("title", "未命名分组").ifBlank { "未命名分组" }
            val icon = g.optString("icon", "sentiment_satisfied").ifBlank { "sentiment_satisfied" }
            val itemsArr = g.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<String>()
            for (j in 0 until itemsArr.length()) {
                val text = itemsArr.optString(j, "").trim()
                if (text.isNotEmpty()) items.add(text)
            }
            if (items.isEmpty()) items.add("请输入常用短句")
            parsedGroups.add(
                QuickSubtitleGroup(
                    id = id,
                    title = title,
                    icon = icon,
                    items = items
                )
            )
            if (id > maxId) maxId = id
        }
        val finalGroups = if (parsedGroups.isNotEmpty()) parsedGroups else defaultQuickSubtitleGroups()
        val selectedId = root.optLong("selectedGroupId", finalGroups.first().id)
        val fontSize = root.optDouble("fontSizeSp", 56.0).toFloat().coerceIn(28f, 96f)
        val currentText = root.optString("currentText", quickSubtitleCurrentText).ifBlank { quickSubtitleCurrentText }
        val inputText = root.optString("inputText", "")
        val playOnSend = root.optBoolean("playOnSend", true)
        quickSubtitleGroups = finalGroups
        quickSubtitleSelectedGroupId =
            finalGroups.firstOrNull { it.id == selectedId }?.id ?: finalGroups.first().id
        quickSubtitleFontSizeSp = fontSize
        quickSubtitleCurrentText = currentText
        quickSubtitleInputText = inputText
        quickSubtitlePlayOnSend = playOnSend
        quickSubtitleNextGroupId = maxOf(maxId + 1L, (finalGroups.maxOfOrNull { it.id } ?: 0L) + 1L)
    }

    private fun saveQuickSubtitleConfig() {
        if (quickSubtitleSaving) return
        quickSubtitleSaving = true
        val root = JSONObject().apply {
            put("selectedGroupId", quickSubtitleSelectedGroupId)
            put("fontSizeSp", quickSubtitleFontSizeSp.toDouble())
            put("currentText", quickSubtitleCurrentText)
            put("inputText", quickSubtitleInputText)
            put("playOnSend", quickSubtitlePlayOnSend)
            val groupsArr = JSONArray()
            quickSubtitleGroups.forEach { g ->
                groupsArr.put(
                    JSONObject().apply {
                        put("id", g.id)
                        put("title", g.title)
                        put("icon", g.icon)
                        val itemsArr = JSONArray()
                        g.items.forEach { itemsArr.put(it) }
                        put("items", itemsArr)
                    }
                )
            }
            put("groups", groupsArr)
        }
        val payload = root.toString()
        viewModelScope.launch {
            try {
                UserPrefs.setQuickSubtitleConfig(appContext, payload)
            } finally {
                quickSubtitleSaving = false
            }
        }
    }

    fun currentQuickSubtitleGroupIndex(): Int {
        val idx = quickSubtitleGroups.indexOfFirst { it.id == quickSubtitleSelectedGroupId }
        return if (idx >= 0) idx else 0
    }

    fun selectQuickSubtitleGroup(index: Int) {
        val clamped = index.coerceIn(0, quickSubtitleGroups.lastIndex.coerceAtLeast(0))
        val target = quickSubtitleGroups.getOrNull(clamped) ?: return
        quickSubtitleSelectedGroupId = target.id
        saveQuickSubtitleConfig()
    }

    fun applyQuickSubtitleText(text: String, enqueueSpeak: Boolean = true) {
        val message = text.trim()
        if (message.isEmpty()) return
        quickSubtitleCurrentText = message
        if (enqueueSpeak) speakText(message)
        saveQuickSubtitleConfig()
    }

    fun updateQuickSubtitleInputText(text: String) {
        quickSubtitleInputText = text
    }

    fun submitQuickSubtitleInput(playVoice: Boolean = quickSubtitlePlayOnSend) {
        val message = quickSubtitleInputText.trim()
        if (message.isEmpty()) return
        quickSubtitleCurrentText = message
        quickSubtitleInputText = ""
        if (playVoice) {
            speakText(message)
        } else {
            uiState = uiState.copy(status = "已更新字幕文本")
        }
        saveQuickSubtitleConfig()
    }

    fun setQuickSubtitleFontSize(size: Float) {
        quickSubtitleFontSizeSp = size.coerceIn(28f, 96f)
        saveQuickSubtitleConfig()
    }

    fun updateQuickSubtitlePlayOnSend(enabled: Boolean) {
        quickSubtitlePlayOnSend = enabled
        saveQuickSubtitleConfig()
    }

    fun updateQuickSubtitleGroupMeta(index: Int, title: String, icon: String) {
        if (index !in quickSubtitleGroups.indices) return
        val next = quickSubtitleGroups.toMutableList()
        val prev = next[index]
        next[index] = prev.copy(
            title = title.trim().ifEmpty { "未命名分组" },
            icon = icon.ifBlank { "sentiment_satisfied" }
        )
        quickSubtitleGroups = next
        saveQuickSubtitleConfig()
    }

    fun addQuickSubtitleGroup() {
        val newId = quickSubtitleNextGroupId++
        quickSubtitleGroups = quickSubtitleGroups + QuickSubtitleGroup(
            id = newId,
            title = "新分组",
            icon = "sentiment_neutral",
            items = listOf("请输入常用短句")
        )
        quickSubtitleSelectedGroupId = newId
        saveQuickSubtitleConfig()
    }

    fun removeQuickSubtitleGroup(index: Int) {
        if (quickSubtitleGroups.size <= 1) return
        if (index !in quickSubtitleGroups.indices) return
        val removedId = quickSubtitleGroups[index].id
        val next = quickSubtitleGroups.toMutableList().apply { removeAt(index) }
        quickSubtitleGroups = next
        if (quickSubtitleSelectedGroupId == removedId) {
            quickSubtitleSelectedGroupId = next[index.coerceAtMost(next.lastIndex)].id
        }
        saveQuickSubtitleConfig()
    }

    fun moveQuickSubtitleGroup(from: Int, to: Int) {
        if (from !in quickSubtitleGroups.indices || to !in quickSubtitleGroups.indices) return
        if (from == to) return
        val next = quickSubtitleGroups.toMutableList()
        val item = next.removeAt(from)
        next.add(to, item)
        quickSubtitleGroups = next
        saveQuickSubtitleConfig()
    }

    fun addQuickSubtitleItem(groupIndex: Int) {
        if (groupIndex !in quickSubtitleGroups.indices) return
        val next = quickSubtitleGroups.toMutableList()
        val g = next[groupIndex]
        next[groupIndex] = g.copy(items = g.items + "新快捷文本")
        quickSubtitleGroups = next
        saveQuickSubtitleConfig()
    }

    fun removeQuickSubtitleItem(groupIndex: Int, itemIndex: Int) {
        if (groupIndex !in quickSubtitleGroups.indices) return
        val g = quickSubtitleGroups[groupIndex]
        if (itemIndex !in g.items.indices) return
        if (g.items.size <= 1) return
        val items = g.items.toMutableList().apply { removeAt(itemIndex) }
        val next = quickSubtitleGroups.toMutableList()
        next[groupIndex] = g.copy(items = items)
        quickSubtitleGroups = next
        saveQuickSubtitleConfig()
    }

    fun moveQuickSubtitleItem(groupIndex: Int, from: Int, to: Int) {
        if (groupIndex !in quickSubtitleGroups.indices) return
        val g = quickSubtitleGroups[groupIndex]
        if (from !in g.items.indices || to !in g.items.indices || from == to) return
        val items = g.items.toMutableList()
        val item = items.removeAt(from)
        items.add(to, item)
        val next = quickSubtitleGroups.toMutableList()
        next[groupIndex] = g.copy(items = items)
        quickSubtitleGroups = next
        saveQuickSubtitleConfig()
    }

    fun updateQuickSubtitleItem(groupIndex: Int, itemIndex: Int, value: String) {
        if (groupIndex !in quickSubtitleGroups.indices) return
        val g = quickSubtitleGroups[groupIndex]
        if (itemIndex !in g.items.indices) return
        val items = g.items.toMutableList()
        items[itemIndex] = value
        val next = quickSubtitleGroups.toMutableList()
        next[groupIndex] = g.copy(items = items)
        quickSubtitleGroups = next
        saveQuickSubtitleConfig()
    }

    private fun ensureController(): RealtimeController {
        controller?.let { return it }
        val created = RealtimeController(
            appContext,
            viewModelScope,
            onResult = { id, text ->
                val item = RecognizedItem(id = id, text = text)
                val next = (listOf(item) + realtimeRecognized).take(MAX_RECOGNIZED_ITEMS)
                realtimeRecognized = next
                val validIds = next.asSequence().map { it.id }.toSet()
                lastProgressUpdateAtMs.keys.retainAll(validIds)
            },
            onProgress = { id, progress ->
                val items = realtimeRecognized
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
                        val updated = current.copy(progress = nextProgress)
                        val next = items.toMutableList()
                        next[idx] = updated
                        realtimeRecognized = next
                    }
                }
                realtimePlaybackProgress = progress.coerceIn(0f, 1f)
            },
            onLevel = { level ->
                val next = level.coerceIn(0f, 1f)
                val now = SystemClock.elapsedRealtime()
                val prev = realtimeInputLevel
                val delta = kotlin.math.abs(prev - next)
                val intervalReady = (now - lastLevelUpdateAtMs) >= LEVEL_UPDATE_INTERVAL_MS
                if (delta >= LEVEL_UPDATE_DELTA || intervalReady) {
                    realtimeInputLevel = next
                    lastLevelUpdateAtMs = now
                }
            },
            onInputDevice = { label ->
                if (label != uiState.inputDeviceLabel) {
                    uiState = uiState.copy(inputDeviceLabel = label)
                }
            },
            onOutputDevice = { label ->
                if (label != uiState.outputDeviceLabel) {
                    uiState = uiState.copy(outputDeviceLabel = label)
                }
            },
            onAec3Status = { status ->
                if (status != uiState.aec3Status) {
                    uiState = uiState.copy(aec3Status = status)
                }
            },
            onAec3Diag = { diag ->
                if (diag != uiState.aec3Diag) {
                    uiState = uiState.copy(aec3Diag = diag)
                }
            },
            onError = { msg -> uiState = uiState.copy(status = msg, running = false) },
            initialSuppressWhilePlaying = uiState.muteWhilePlaying,
            initialUseVoiceCommunication = uiState.echoSuppression,
            initialCommunicationMode = uiState.communicationMode,
            initialMinVolumePercent = uiState.minVolumePercent,
            initialPlaybackGainPercent = uiState.playbackGainPercent,
            initialSuppressDelaySec = uiState.muteWhilePlayingDelaySec,
            initialPreferredInputType = uiState.preferredInputType,
            initialPreferredOutputType = uiState.preferredOutputType,
            initialUseAec3 = uiState.aec3Enabled,
            initialNumberReplaceMode = uiState.numberReplaceMode,
            initialAllowSystemAecWithAec3 = true
        )
        controller = created
        return created
    }

    private fun preloadAsr(asrDir: File?) {
        if (asrDir == null) return
        val activeController = ensureController()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                activeController.loadAsr(asrDir)
            }
        }
    }

    private fun preloadTts(voiceDir: File?) {
        if (voiceDir == null) return
        val activeController = ensureController()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                activeController.loadTts(voiceDir)
            }
        }
    }

    fun loadBundledAsr() {
        if (uiState.asrDir != null) return
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { repo.ensureBundledAsr() }
            if (dir != null) {
                uiState = uiState.copy(asrDir = dir, status = "已加载内置 ASR 模型")
                preloadAsr(dir)
            } else {
                uiState = uiState.copy(status = "未找到内置 ASR 模型")
            }
        }
    }

    fun loadLastVoice() {
        viewModelScope.launch {
            val lastName = UserPrefs.getLastVoiceName(appContext)
            val lastDir = lastName?.let { repo.resolveVoicePack(it) }
            uiState = uiState.copy(
                voiceDir = lastDir ?: uiState.voiceDir,
                status = if (lastDir != null) "已加载上次音色包" else uiState.status
            )
            preloadTts(lastDir)
            refreshVoicePacks()
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            val settings = UserPrefs.getSettings(appContext)
            uiState = uiState.copy(
                muteWhilePlaying = settings.muteWhilePlaying,
                muteWhilePlayingDelaySec = settings.muteWhilePlayingDelaySec,
                echoSuppression = settings.echoSuppression,
                communicationMode = settings.communicationMode,
                preferredInputType = settings.preferredInputType,
                preferredOutputType = settings.preferredOutputType,
                aec3Enabled = settings.aec3Enabled,
                aec3Status = if (settings.aec3Enabled) "待启动" else "未启用",
                aec3Diag = if (settings.aec3Enabled) "AEC3 诊断：待启动" else "AEC3 诊断：未启用",
                minVolumePercent = settings.minVolumePercent,
                playbackGainPercent = settings.playbackGainPercent,
                keepAlive = settings.keepAlive,
                numberReplaceMode = settings.numberReplaceMode,
                landscapeDrawerMode = settings.landscapeDrawerMode,
                solidTopBar = settings.solidTopBar
            )
            applySettingsToController(settings)
        }
    }

    fun importAsr(uri: android.net.Uri) {
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { repo.importAsr(uri, appContext.contentResolver) }
            uiState = uiState.copy(asrDir = dir, status = "ASR 模型导入完成")
            preloadAsr(dir)
        }
    }

    fun importVoice(uri: android.net.Uri) {
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { repo.importVoice(uri, appContext.contentResolver) }
            UserPrefs.setLastVoiceName(appContext, dir.name)
            uiState = uiState.copy(voiceDir = dir, status = "音色包导入完成")
            preloadTts(dir)
            refreshVoicePacks()
        }
    }

    fun selectVoice(dir: File) {
        viewModelScope.launch {
            UserPrefs.setLastVoiceName(appContext, dir.name)
            uiState = uiState.copy(voiceDir = dir, status = "已选择音色包")
            preloadTts(dir)
            refreshVoicePacks()
        }
    }

    fun refreshVoicePacks() {
        viewModelScope.launch {
            val packs = withContext(Dispatchers.IO) { repo.listVoicePacks() }
            uiState = uiState.copy(voicePacks = packs)
        }
    }

    fun updateVoiceMeta(pack: VoicePackInfo, name: String, remark: String) {
        val trimmedName = name.trim().ifEmpty { "未命名" }
        val trimmedRemark = remark.trim()
        viewModelScope.launch {
            repo.updateVoiceMeta(pack.dir) { meta ->
                meta.copy(name = trimmedName, remark = trimmedRemark)
            }
            refreshVoicePacks()
        }
    }

    fun updateVoiceAvatar(pack: VoicePackInfo, uri: android.net.Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.updateVoiceAvatar(pack.dir, appContext.contentResolver, uri, "avatar.png")
            }
            refreshVoicePacks()
        }
    }

    fun toggleVoicePin(pack: VoicePackInfo) {
        viewModelScope.launch {
            repo.updateVoiceMeta(pack.dir) { meta ->
                meta.copy(pinned = !meta.pinned)
            }
            refreshVoicePacks()
        }
    }

    fun moveVoice(pack: VoicePackInfo, delta: Int) {
        val list = uiState.voicePacks
        val idx = list.indexOfFirst { it.dir == pack.dir }
        if (idx < 0) return
        val newIdx = idx + delta
        if (newIdx !in list.indices) return
        val a = list[idx]
        val b = list[newIdx]
        if (a.meta.pinned != b.meta.pinned) return
        viewModelScope.launch {
            repo.updateVoiceMeta(a.dir) { meta -> meta.copy(order = b.meta.order) }
            repo.updateVoiceMeta(b.dir) { meta -> meta.copy(order = a.meta.order) }
            refreshVoicePacks()
        }
    }

    fun reorderVoicePacks(newOrder: List<VoicePackInfo>) {
        viewModelScope.launch {
            newOrder.forEachIndexed { index, pack ->
                repo.updateVoiceMeta(pack.dir) { meta ->
                    meta.copy(order = index.toLong())
                }
            }
            refreshVoicePacks()
        }
    }

    fun deleteVoice(pack: VoicePackInfo) {
        val current = uiState.voiceDir?.absolutePath == pack.dir.absolutePath
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.deleteVoicePack(pack.dir)
            }
            if (current) {
                uiState = uiState.copy(voiceDir = null)
            }
            refreshVoicePacks()
        }
    }

    fun shareVoice(pack: VoicePackInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val shareDir = File(appContext.cacheDir, "share")
            val fileName = "${pack.dir.name}.zip"
            val outZip = File(shareDir, fileName)
            repo.zipVoicePack(pack.dir, outZip)
            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(
                    appContext,
                    appContext.packageName + ".fileprovider",
                    outZip
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                appContext.startActivity(Intent.createChooser(intent, "分享语音包"))
            }
        }
    }

    fun setMuteWhilePlaying(enabled: Boolean) {
        uiState = uiState.copy(muteWhilePlaying = enabled)
        controller?.setSuppressWhilePlaying(enabled)
        viewModelScope.launch {
            UserPrefs.setMuteWhilePlaying(appContext, enabled)
        }
    }

    fun setMuteWhilePlayingDelay(seconds: Float) {
        val clamped = seconds.coerceIn(0f, 5f)
        uiState = uiState.copy(muteWhilePlayingDelaySec = clamped)
        controller?.setSuppressDelaySec(clamped)
        viewModelScope.launch {
            UserPrefs.setMuteWhilePlayingDelaySec(appContext, clamped)
        }
    }

    fun setMinVolumePercent(percent: Int) {
        uiState = uiState.copy(minVolumePercent = percent)
        controller?.setMinVolumePercent(percent)
        viewModelScope.launch {
            UserPrefs.setMinVolumePercent(appContext, percent)
        }
    }

    fun setPlaybackGainPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 1000)
        uiState = uiState.copy(playbackGainPercent = clamped)
        controller?.setPlaybackGainPercent(clamped)
        viewModelScope.launch {
            UserPrefs.setPlaybackGainPercent(appContext, clamped)
        }
    }

    fun setKeepAlive(enabled: Boolean) {
        val running = uiState.running
        uiState = uiState.copy(keepAlive = enabled)
        viewModelScope.launch {
            UserPrefs.setKeepAlive(appContext, enabled)
        }
        if (running) {
            if (enabled) {
                KeepAliveService.start(appContext)
            } else {
                KeepAliveService.stop(appContext)
            }
        }
    }

    fun setNumberReplaceMode(mode: Int) {
        val clamped = mode.coerceIn(0, 2)
        uiState = uiState.copy(numberReplaceMode = clamped)
        controller?.setNumberReplaceMode(clamped)
        viewModelScope.launch {
            UserPrefs.setNumberReplaceMode(appContext, clamped)
        }
    }

    fun setLandscapeDrawerMode(mode: Int) {
        val clamped = mode.coerceIn(UserPrefs.DRAWER_MODE_HIDDEN, UserPrefs.DRAWER_MODE_PERMANENT)
        uiState = uiState.copy(landscapeDrawerMode = clamped)
        viewModelScope.launch {
            UserPrefs.setLandscapeDrawerMode(appContext, clamped)
        }
    }

    fun setSolidTopBar(enabled: Boolean) {
        uiState = uiState.copy(solidTopBar = enabled)
        viewModelScope.launch {
            UserPrefs.setSolidTopBar(appContext, enabled)
        }
    }

    fun updateDrawColor(color: Color) {
        drawColor = color
        drawEraser = false
    }

    fun updateDrawBrushSize(size: Float) {
        val clamped = size.coerceIn(2f, 48f)
        if (drawEraser) {
            drawEraserSize = clamped
        } else {
            drawBrushSize = clamped
        }
    }

    fun updateDrawEraser(enabled: Boolean) {
        drawEraser = enabled
    }

    fun clearDrawingBoard() {
        drawStrokes.clear()
    }

    fun appendDrawingStroke(points: List<DrawPoint>) {
        if (points.size < 2) return
        val effectiveWidth = if (drawEraser) drawEraserSize * 5f else drawBrushSize
        drawStrokes.add(
            DrawStrokeData(
                points = points,
                color = drawColor,
                width = effectiveWidth,
                eraser = drawEraser
            )
        )
    }

    fun setEchoSuppression(enabled: Boolean) {
        val wasRunning = uiState.running
        uiState = uiState.copy(echoSuppression = enabled)
        controller?.setUseVoiceCommunication(enabled)
        viewModelScope.launch {
            UserPrefs.setEchoSuppression(appContext, enabled)
        }
        if (wasRunning) {
            restartJob?.cancel()
            restartJob = viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    controller?.restartRecorder()
                }
            }
        }
    }

    fun setCommunicationMode(enabled: Boolean) {
        val wasRunning = uiState.running
        uiState = uiState.copy(communicationMode = enabled)
        controller?.setCommunicationMode(enabled)
        viewModelScope.launch {
            UserPrefs.setCommunicationMode(appContext, enabled)
        }
        if (wasRunning) {
            restartJob?.cancel()
            restartJob = viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    controller?.restartRecorder()
                }
            }
        }
    }

    fun setPreferredOutputType(type: Int) {
        val wasRunning = uiState.running
        uiState = uiState.copy(preferredOutputType = type)
        controller?.setPreferredOutputType(type)
        viewModelScope.launch {
            UserPrefs.setPreferredOutputType(appContext, type)
        }
        if (wasRunning) {
            restartJob?.cancel()
            restartJob = viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    controller?.restartRecorder()
                }
            }
        }
    }

    fun setPreferredInputType(type: Int) {
        val wasRunning = uiState.running
        uiState = uiState.copy(preferredInputType = type)
        controller?.setPreferredInputType(type)
        viewModelScope.launch {
            UserPrefs.setPreferredInputType(appContext, type)
        }
        if (wasRunning) {
            restartJob?.cancel()
            restartJob = viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    controller?.restartRecorder()
                }
            }
        }
    }

    fun setAec3Enabled(enabled: Boolean) {
        uiState = uiState.copy(
            aec3Enabled = enabled,
            aec3Status = if (enabled) "初始化中" else "未启用"
        )
        controller?.setUseAec3(enabled)
        viewModelScope.launch {
            UserPrefs.setAec3Enabled(appContext, enabled)
        }
    }

    fun speakText(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return
        val voice = uiState.voiceDir
        if (voice == null) {
            uiState = uiState.copy(status = "请先选择语音包")
            return
        }
        val activeController = ensureController()
        viewModelScope.launch {
            val queued = withContext(Dispatchers.IO) {
                if (!activeController.loadTts(voice)) return@withContext false
                activeController.enqueueSpeakText(message) != null
            }
            if (queued) {
                uiState = uiState.copy(status = "已加入朗读队列")
            }
        }
    }

    fun start() {
        val asr = uiState.asrDir
        val voice = uiState.voiceDir
        if (asr == null || voice == null) {
            uiState = uiState.copy(status = "请先导入 ASR 模型和 voicepack")
            return
        }
        restartJob?.cancel()
        restartJob = null
        val activeController = ensureController()
        // 每次启动新会话前清空上次识别列表，避免新旧会话复用同一结果ID导致UI键冲突。
        realtimeRecognized = emptyList()
        realtimeInputLevel = 0f
        realtimePlaybackProgress = 0f
        lastProgressUpdateAtMs.clear()
        lastLevelUpdateAtMs = 0L
        uiState = uiState.copy(running = true, status = "启动麦克风中")
        viewModelScope.launch {
            val started = withContext(Dispatchers.IO) {
                if (!activeController.loadAsr(asr)) return@withContext false
                if (!activeController.loadTts(voice)) return@withContext false
                activeController.startMic()
            }
            if (started && uiState.running) {
                uiState = uiState.copy(status = "运行中")
                if (uiState.keepAlive) {
                    KeepAliveService.start(appContext)
                }
            } else {
                realtimeInputLevel = 0f
                realtimePlaybackProgress = 0f
                KeepAliveService.stop(appContext)
                if (uiState.running) {
                    uiState = uiState.copy(running = false, status = "麦克风启动失败")
                }
            }
        }
    }

    fun stop() {
        restartJob?.cancel()
        restartJob = null
        val activeController = controller ?: run {
            uiState = uiState.copy(running = false, status = "麦克风已停止")
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                activeController.stopMic()
            }
            KeepAliveService.stop(appContext)
            realtimeInputLevel = 0f
            realtimePlaybackProgress = 0f
            uiState = uiState.copy(running = false, status = "麦克风已停止")
        }
    }

    override fun onCleared() {
        val activeController = controller
        controller = null
        if (activeController != null) {
            runCatching {
                kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                    activeController.stop()
                }
            }
        }
        super.onCleared()
    }

    private fun applySettingsToController(settings: UserPrefs.AppSettings) {
        controller?.setSuppressWhilePlaying(settings.muteWhilePlaying)
        controller?.setSuppressDelaySec(settings.muteWhilePlayingDelaySec)
        controller?.setMinVolumePercent(settings.minVolumePercent)
        controller?.setPlaybackGainPercent(settings.playbackGainPercent)
        controller?.setUseAec3(settings.aec3Enabled)
        controller?.setUseVoiceCommunication(settings.echoSuppression)
        controller?.setCommunicationMode(settings.communicationMode)
        controller?.setPreferredInputType(settings.preferredInputType)
        controller?.setPreferredOutputType(settings.preferredOutputType)
        controller?.setNumberReplaceMode(settings.numberReplaceMode)
        controller?.setAllowSystemAecWithAec3(true)
    }
}

private object UiTokens {
    val Primary = Color(0xFF038387)
    val Radius = 4.dp
    val TopBarElevation = 8.dp
    val CardElevation = 2.dp
    val FabElevation = 6.dp
    val MenuElevation = 8.dp
    val PageTopBlank = 8.dp
    val PageBottomBlank = 92.dp
    val DrawerWidthExpanded = 216.dp
    val DrawerWidthCollapsed = 72.dp
    val LightCard = Color(0xFFFFFFFF)
    val DarkCard = Color(0xFF1D2023)
}

private val KgtLightColors = lightColors(
    primary = UiTokens.Primary,
    onPrimary = Color.White,
    secondary = UiTokens.Primary,
    onSecondary = Color.White,
    background = Color(0xFFF1F3F5),
    onBackground = Color(0xFF111417),
    surface = UiTokens.LightCard,
    onSurface = Color(0xFF111417)
)

private val KgtDarkColors = darkColors(
    primary = UiTokens.Primary,
    onPrimary = Color.White,
    secondary = UiTokens.Primary,
    onSecondary = Color.White,
    background = Color(0xFF121416),
    onBackground = Color(0xFFE4E8EB),
    surface = UiTokens.DarkCard,
    onSurface = Color(0xFFE4E8EB)
)

private data class Md2ExtraColors(
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color
)

private val KgtLightExtraColors = Md2ExtraColors(
    surfaceVariant = Color(0xFFE8ECEF),
    onSurfaceVariant = Color(0xFF495156),
    outline = Color(0xFF9CA5AC)
)

private val KgtDarkExtraColors = Md2ExtraColors(
    surfaceVariant = Color(0xFF262A2E),
    onSurfaceVariant = Color(0xFFB6BEC4),
    outline = Color(0xFF757F87)
)

private val LocalMd2ExtraColors = staticCompositionLocalOf { KgtLightExtraColors }

private data class Md2ColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color
)

private val MaterialTheme.colorScheme: Md2ColorScheme
    @Composable
    get() {
        val base = MaterialTheme.colors
        val extra = LocalMd2ExtraColors.current
        return Md2ColorScheme(
            primary = base.primary,
            onPrimary = base.onPrimary,
            secondary = base.secondary,
            onSecondary = base.onSecondary,
            background = base.background,
            onBackground = base.onBackground,
            surface = base.surface,
            onSurface = base.onSurface,
            surfaceVariant = extra.surfaceVariant,
            onSurfaceVariant = extra.onSurfaceVariant,
            outline = extra.outline
        )
    }

private val Typography.titleSmall: TextStyle get() = subtitle2
private val Typography.titleMedium: TextStyle get() = h6
private val Typography.bodyLarge: TextStyle get() = body1
private val Typography.bodyMedium: TextStyle get() = body2
private val Typography.bodySmall: TextStyle get() = caption
private val Typography.labelMedium: TextStyle get() = caption
private val Typography.labelSmall: TextStyle get() = overline

private val KgtTypography = Typography(
    h6 = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    subtitle2 = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    body1 = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    body2 = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    button = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    overline = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )
)

private val Md2Shapes = Shapes(
    small = RoundedCornerShape(UiTokens.Radius),
    medium = RoundedCornerShape(UiTokens.Radius),
    large = RoundedCornerShape(UiTokens.Radius)
)

@Composable
private fun md2CardContainerColor(): Color {
    return if (isSystemInDarkTheme()) UiTokens.DarkCard else UiTokens.LightCard
}

private val MaterialSymbolsSharp = FontFamily(
    Font(
        resId = R.font.material_symbols_sharp,
        weight = FontWeight.W500
    )
)

@Composable
private fun MsIcon(
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val a11yModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    Text(
        text = name,
        modifier = a11yModifier,
        color = tint,
        style = TextStyle(
            fontFamily = MaterialSymbolsSharp,
            fontWeight = FontWeight.W500,
            fontSize = 24.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
            fontFeatureSettings = "'liga' 1"
        )
    )
}

private data class DrawerItem(
    val title: String,
    val icon: String
)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val repo = ModelRepository(this@MainActivity)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repo, this@MainActivity) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#038387")),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        applyWindowInsetPolicyForMode()
        AppLogger.init(this)
        AppLogger.i("MainActivity.onCreate")
        viewModel.loadBundledAsr()
        viewModel.loadLastVoice()
        viewModel.loadSettings()
        setContent {
            val dark = isSystemInDarkTheme()
            val colors = if (dark) KgtDarkColors else KgtLightColors
            val extraColors = if (dark) KgtDarkExtraColors else KgtLightExtraColors
            CompositionLocalProvider(LocalMd2ExtraColors provides extraColors) {
                MaterialTheme(colors = colors, typography = KgtTypography, shapes = Md2Shapes) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppScaffold(viewModel)
                    }
                }
            }
        }
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        applyWindowInsetPolicyForMode()
    }

    private fun applyWindowInsetPolicyForMode() {
        val inMultiWindow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInMultiWindowMode
        } else {
            false
        }
        // Multi-window/floating mode: let the framework fit system windows to avoid title-bar overlap.
        // Fullscreen mode: keep edge-to-edge behavior.
        WindowCompat.setDecorFitsSystemWindows(window, inMultiWindow)
    }
}

private val Md2ControlShape = RoundedCornerShape(UiTokens.Radius)

@Composable
private fun Md2Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = Md2ControlShape,
        elevation = ButtonDefaults.elevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp,
            disabledElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
private fun Md2OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = Md2ControlShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
private fun Md2TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = Md2ControlShape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        content = content
    )
}

@Composable
private fun Md2IconButton(
    icon: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(36.dp)
    ) {
        MsIcon(
            name = icon,
            contentDescription = contentDescription,
            tint = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun Md2Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dark = isSystemInDarkTheme()
    val uncheckedTrack = if (dark) Color(0xFF697378) else Color(0xFFB3C1C6)
    val uncheckedThumb = if (dark) Color(0xFFE6EFF2) else Color.White
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
            uncheckedThumbColor = uncheckedThumb,
            uncheckedTrackColor = uncheckedTrack,
            disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            disabledUncheckedThumbColor = uncheckedThumb.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = uncheckedTrack.copy(alpha = 0.3f)
        )
    )
}

@Composable
private fun Md2OutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        shape = Md2ControlShape,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun AppScaffold(viewModel: MainViewModel) {
    val pageRealtime = 0
    val pageQuickSubtitle = 1
    val pageTextToSpeech = 2
    val pageVoicePack = 3
    val pageDrawing = 4
    val pageSettings = 5
    val pageLog = 6
    val pageQuickSubtitleEditor = 7

    var page by rememberSaveable { mutableStateOf(0) }
    var drawingFullscreen by rememberSaveable { mutableStateOf(false) }
    var runningStripCollapsed by rememberSaveable { mutableStateOf(false) }
    val state = viewModel.uiState
    val context = LocalContext.current
    val activity = context as? Activity
    val inMultiWindowMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        activity?.isInMultiWindowMode == true
    } else {
        false
    }
    val miuiFloatingTopCompensation = 0.dp
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    val topBarColor = if (state.solidTopBar) md2CardContainerColor() else MaterialTheme.colorScheme.primary
    val topBarContentColor = if (state.solidTopBar) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    val hiddenDrawerScrimColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDarkTheme) 0.56f else 0.32f
    )
    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = topBarColor.toArgb()
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, window.decorView)
                ?.isAppearanceLightStatusBars = topBarColor.luminance() > 0.5f
            WindowCompat.getInsetsController(window, window.decorView)
                ?.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val hiddenDrawerWidth = remember(configuration.screenWidthDp) {
        val screenWidth = configuration.screenWidthDp.dp
        val targetWidth = UiTokens.DrawerWidthExpanded
        // Keep a small right-side visible area on narrow screens to avoid hard overflow.
        val compatEdgeGap = 24.dp
        val maxAllowed = (screenWidth - compatEdgeGap).coerceAtLeast(0.dp)
        if (maxAllowed <= 0.dp) screenWidth else minOf(targetWidth, maxAllowed)
    }
    val usePermanentDrawer =
        isLandscape && state.landscapeDrawerMode == UserPrefs.DRAWER_MODE_PERMANENT
    val editorOverlayOpen = page == pageQuickSubtitleEditor
    val basePage = if (editorOverlayOpen) pageQuickSubtitle else page
    var drawerExpanded by rememberSaveable { mutableStateOf(false) }
    val showRunningStrip = state.running && !(drawingFullscreen && basePage == pageDrawing)
    val topMicLevel = viewModel.realtimeInputLevel
    val topPlaybackProgress = viewModel.realtimePlaybackProgress
    val drawerItems = listOf(
        DrawerItem("实时转换", "graphic_eq"),
        DrawerItem("便捷字幕", "subtitles"),
        DrawerItem("文字转语音", "text_to_speech"),
        DrawerItem("语音包", "record_voice_over"),
        DrawerItem("画板", "draw"),
        DrawerItem("设置", "tune"),
        DrawerItem("日志", "article")
    )
    val titles = drawerItems.map { it.title }
    val drawerSelectedPage = basePage
    LaunchedEffect(drawerItems.size) {
        if (page !in 0..pageQuickSubtitleEditor) {
            page = pageRealtime
        }
    }
    LaunchedEffect(page) {
        if (page != pageDrawing) drawingFullscreen = false
    }
    LaunchedEffect(drawingFullscreen, page) {
        if (drawingFullscreen && page == pageDrawing && !usePermanentDrawer) {
            scope.launch { drawerState.close() }
        }
    }
    LaunchedEffect(usePermanentDrawer) {
        if (usePermanentDrawer) {
            scope.launch { drawerState.close() }
        }
    }
    LaunchedEffect(state.running) {
        if (!state.running) runningStripCollapsed = false
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.start() else toast(context, "需要麦克风权限")
    }
    val voicePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.importVoice(uri) else toast(context, "未选择文件")
    }

    val onToggleRun = {
        if (state.running) {
            viewModel.stop()
        } else {
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val topBar: @Composable ((() -> Unit)) -> Unit = { onNavClick ->
        val currentTitle = titles.getOrElse(basePage) { "KGTTS" }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!inMultiWindowMode) Modifier.statusBarsPadding() else Modifier)
                .padding(top = miuiFloatingTopCompensation)
                .zIndex(2f),
            color = topBarColor,
            elevation = UiTokens.TopBarElevation
        ) {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(currentTitle)
                        AnimatedVisibility(
                            visible = showRunningStrip,
                            enter = fadeIn(animationSpec = tween(140)) +
                                    androidx.compose.animation.slideInHorizontally(
                                        initialOffsetX = { full -> full / 3 },
                                        animationSpec = tween(140, easing = FastOutSlowInEasing)
                                    ),
                            exit = fadeOut(animationSpec = tween(120)) +
                                    androidx.compose.animation.slideOutHorizontally(
                                        targetOffsetX = { full -> full / 3 },
                                        animationSpec = tween(120, easing = FastOutSlowInEasing)
                                    )
                        ) {
                            RunningStripTopBarToggle(
                                micLevel = topMicLevel,
                                playbackProgress = topPlaybackProgress,
                                expanded = !runningStripCollapsed,
                                contentColor = topBarContentColor,
                                onToggle = { runningStripCollapsed = !runningStripCollapsed }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavClick) {
                        MsIcon("menu", contentDescription = "打开菜单")
                    }
                },
                actions = {
                    if (basePage == pageVoicePack) {
                        IconButton(onClick = { voicePicker.launch("*/*") }) {
                            MsIcon("folder_open", contentDescription = "导入语音包")
                        }
                    }
                },
                backgroundColor = Color.Transparent,
                contentColor = topBarContentColor,
                elevation = 0.dp
            )
        }
    }

    val fab: @Composable () -> Unit = {
        if (basePage == pageRealtime) {
            FloatingActionButton(
                onClick = onToggleRun,
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = UiTokens.FabElevation,
                    pressedElevation = 12.dp
                )
            ) {
                MsIcon(
                    name = if (state.running) "stop" else "play_arrow",
                    contentDescription = if (state.running) "关闭麦克风" else "开启麦克风",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    val contentArea: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier = modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = basePage,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(animationSpec = tween(220, delayMillis = 80)) +
                                slideInVertically(
                                    initialOffsetY = { full -> full / 10 },
                                    animationSpec = tween(220, delayMillis = 80)
                                ),
                        initialContentExit = fadeOut(animationSpec = tween(90)) +
                                slideOutVertically(
                                    targetOffsetY = { full -> -full / 10 },
                                    animationSpec = tween(90)
                                )
                    )
                },
                label = "page_switch"
            ) { current ->
                when (current) {
                    pageRealtime -> RealtimeScreen(viewModel, state)
                    pageQuickSubtitle -> QuickSubtitleScreen(
                        viewModel = viewModel,
                        state = state,
                        onToggleMic = onToggleRun,
                        onOpenEditor = { page = pageQuickSubtitleEditor }
                    )
                    pageTextToSpeech -> TextToSpeechScreen(viewModel, state)
                    pageVoicePack -> VoicePackScreen(viewModel, state)
                    pageDrawing -> DrawingBoardScreen(
                        viewModel = viewModel,
                        fullscreen = drawingFullscreen,
                        onToggleFullscreen = { drawingFullscreen = !drawingFullscreen }
                    )
                    pageSettings -> SettingsScreen(viewModel, state)
                    pageLog -> LogScreen()
                }
            }
            AnimatedVisibility(
                visible = showRunningStrip && !runningStripCollapsed,
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f),
                enter = fadeIn(animationSpec = tween(120)),
                exit = fadeOut(animationSpec = tween(90))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            runningStripCollapsed = true
                        }
                )
            }
            AnimatedVisibility(
                visible = showRunningStrip && !runningStripCollapsed,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .zIndex(2f),
                enter = fadeIn(animationSpec = tween(160)) +
                        slideInVertically(
                            initialOffsetY = { full -> -full / 3 },
                            animationSpec = tween(160, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(120)) +
                        slideOutVertically(
                            targetOffsetY = { full -> -full / 3 },
                            animationSpec = tween(120, easing = FastOutSlowInEasing)
                        )
            ) {
                RunningStatusTopStrip(
                    viewModel = viewModel,
                    status = state.status,
                    onToggleCollapsed = { runningStripCollapsed = !runningStripCollapsed },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    val drawingImmersive = drawingFullscreen && basePage == pageDrawing
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = drawingImmersive,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(animationSpec = tween(220, delayMillis = 40)) +
                            slideInVertically(
                                initialOffsetY = { full -> full / 16 },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ),
                    initialContentExit = fadeOut(animationSpec = tween(160)) +
                            slideOutVertically(
                                targetOffsetY = { full -> -full / 20 },
                                animationSpec = tween(160, easing = FastOutSlowInEasing)
                            )
                )
            },
            label = "drawing_fullscreen_switch"
        ) { immersive ->
            if (immersive) {
                Scaffold(
                    backgroundColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    contentArea(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            } else if (usePermanentDrawer) {
                Scaffold(
                    topBar = { topBar { drawerExpanded = !drawerExpanded } },
                    floatingActionButton = fab,
                    backgroundColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                modifier = Modifier
                                    .width(UiTokens.DrawerWidthCollapsed)
                                    .fillMaxHeight(),
                                shape = RectangleShape,
                                color = MaterialTheme.colorScheme.surface,
                                elevation = UiTokens.MenuElevation
                            ) {
                                AppDrawerContent(
                                    items = drawerItems,
                                    page = drawerSelectedPage,
                                    expanded = false,
                                    applyStatusBarPadding = false,
                                    showHeader = false,
                                    showTopDivider = false,
                                    topInset = 8.dp,
                                    onSelect = { page = it }
                                )
                            }
                            contentArea(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        AnimatedVisibility(
                            visible = drawerExpanded,
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(3f),
                            enter = fadeIn(animationSpec = tween(120)) +
                                    androidx.compose.animation.slideInHorizontally(
                                        initialOffsetX = { -it / 6 },
                                        animationSpec = tween(120, easing = FastOutSlowInEasing)
                                    ),
                            exit = fadeOut(animationSpec = tween(90)) +
                                    androidx.compose.animation.slideOutHorizontally(
                                        targetOffsetX = { -it / 6 },
                                        animationSpec = tween(90, easing = FastOutSlowInEasing)
                                    )
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    modifier = Modifier
                                        .width(UiTokens.DrawerWidthExpanded)
                                        .fillMaxHeight(),
                                    shape = RectangleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    elevation = UiTokens.MenuElevation
                                ) {
                                    AppDrawerContent(
                                        items = drawerItems,
                                        page = drawerSelectedPage,
                                        expanded = true,
                                        applyStatusBarPadding = false,
                                        showHeader = false,
                                        showTopDivider = false,
                                        topInset = 8.dp,
                                        onSelect = {
                                            page = it
                                            drawerExpanded = false
                                        }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            drawerExpanded = false
                                            runningStripCollapsed = true
                                        }
                                )
                            }
                        }
                    }
                }
            } else {
                ModalDrawer(
                    drawerState = drawerState,
                    drawerShape = RectangleShape,
                    drawerBackgroundColor = Color.Transparent,
                    drawerElevation = 0.dp,
                    scrimColor = hiddenDrawerScrimColor,
                    drawerContent = {
                        Row(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .width(hiddenDrawerWidth)
                                    .fillMaxHeight(),
                                shape = RectangleShape,
                                color = MaterialTheme.colorScheme.surface,
                                elevation = UiTokens.MenuElevation
                            ) {
                                AppDrawerContent(
                                    items = drawerItems,
                                    page = drawerSelectedPage,
                                    expanded = true,
                                    applyStatusBarPadding = !inMultiWindowMode,
                                    showHeader = true,
                                    showTopDivider = true,
                                    topInset = 12.dp,
                                    onSelect = {
                                        page = it
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        scope.launch { drawerState.close() }
                                    }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = { topBar { scope.launch { drawerState.open() } } },
                        floatingActionButton = fab,
                        backgroundColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        contentArea(
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = editorOverlayOpen,
            modifier = Modifier
                .matchParentSize()
                .zIndex(50f),
            enter = fadeIn(animationSpec = tween(180)) +
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { full -> full / 3 },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ),
            exit = fadeOut(animationSpec = tween(120)) +
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { full -> full / 4 },
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    )
        ) {
            Scaffold(
                topBar = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (!inMultiWindowMode) Modifier.statusBarsPadding() else Modifier)
                            .padding(top = miuiFloatingTopCompensation),
                        color = topBarColor,
                        elevation = UiTokens.TopBarElevation
                    ) {
                        TopAppBar(
                            title = { Text("编辑便捷字幕") },
                            navigationIcon = {
                                IconButton(onClick = { page = pageQuickSubtitle }) {
                                    MsIcon("arrow_back", contentDescription = "返回")
                                }
                            },
                            backgroundColor = Color.Transparent,
                            contentColor = topBarContentColor,
                            elevation = 0.dp
                        )
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                QuickSubtitleEditorScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun AppDrawerContent(
    items: List<DrawerItem>,
    page: Int,
    expanded: Boolean,
    applyStatusBarPadding: Boolean,
    showHeader: Boolean,
    showTopDivider: Boolean,
    topInset: Dp,
    onSelect: (Int) -> Unit
) {
    val animatedItemStartPadding by animateDpAsState(
        targetValue = if (expanded) 16.dp else 27.dp,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "drawer_item_start_padding"
    )
    val animatedLabelAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "drawer_label_alpha"
    )
    val animatedLabelTranslateX by animateFloatAsState(
        targetValue = if (expanded) 0f else -8f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "drawer_label_tx"
    )
    val animatedLabelSpacer by animateDpAsState(
        targetValue = if (expanded) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "drawer_label_spacer"
    )
    val animatedLabelWidth by animateDpAsState(
        targetValue = if (expanded) 120.dp else 0.dp,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "drawer_label_width"
    )
    val itemStartPadding = animatedItemStartPadding
    val labelAlpha = animatedLabelAlpha
    val labelTranslateX = animatedLabelTranslateX
    val labelSpacer = animatedLabelSpacer
    val labelWidth = animatedLabelWidth

    Column(
        modifier = Modifier
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(topInset))
        if (showHeader && expanded) {
            Text(
                text = "KGTTS",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        } else if (showHeader) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KG",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (showTopDivider) {
            Divider()
        }
        items.forEachIndexed { index, item ->
            val selected = page == index
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val bg = when {
                pressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else -> Color.Transparent
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(bg)
                    .clickable(
                        interactionSource = interaction,
                        indication = rememberRipple(bounded = true)
                    ) { onSelect(index) }
                    .padding(start = itemStartPadding, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                MsIcon(name = item.icon, contentDescription = item.title)
                Spacer(Modifier.width(labelSpacer))
                Box(
                    modifier = Modifier
                        .width(labelWidth)
                        .graphicsLayer {
                            alpha = labelAlpha
                            translationX = labelTranslateX
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ModelScreen(viewModel: MainViewModel, state: UiState) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("模型管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = UiTokens.CardElevation
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("ASR 模型导入已迁移", fontWeight = FontWeight.SemiBold)
                Text("请前往“设置 > 模型与资源”导入或替换 ASR 模型。")
                Spacer(Modifier.height(8.dp))
                Text("当前 ASR 路径：", style = MaterialTheme.typography.labelSmall)
                Text(state.asrDir?.absolutePath ?: "未导入")
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = UiTokens.CardElevation
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("语音包导入已迁移", fontWeight = FontWeight.SemiBold)
                Text("请前往“语音包”页面顶部文件夹按钮导入语音包。")
                Spacer(Modifier.height(8.dp))
                Text("当前语音包路径：", style = MaterialTheme.typography.labelSmall)
                Text(state.voiceDir?.absolutePath ?: "未选择")
            }
        }
        Text("状态：${state.status}")
    }
}

@Composable
private fun rememberAvatarBitmap(file: File): android.graphics.Bitmap? {
    val bitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = file.absolutePath,
        key2 = file.lastModified()
    ) {
        value = withContext(Dispatchers.IO) {
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }
    return bitmap
}

@Composable
fun VoicePackScreen(viewModel: MainViewModel, state: UiState) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val defaultItemStepPx = with(density) { 148.dp.toPx() }
    var editPack by remember { mutableStateOf<VoicePackInfo?>(null) }
    var editName by remember { mutableStateOf("") }
    var editRemark by remember { mutableStateOf("") }
    var deletePack by remember { mutableStateOf<VoicePackInfo?>(null) }
    var avatarTarget by remember { mutableStateOf<VoicePackInfo?>(null) }
    val displayPacks = remember { mutableStateListOf<VoicePackInfo>() }
    var draggingPath by remember { mutableStateOf<String?>(null) }
    var draggingBaseIndex by remember { mutableIntStateOf(-1) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggingRawOffsetY by remember { mutableFloatStateOf(0f) }
    var draggingItemStepPx by remember { mutableFloatStateOf(defaultItemStepPx) }
    var reorderedDuringDrag by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        val target = avatarTarget
        avatarTarget = null
        if (target == null) return@rememberLauncherForActivityResult
        if (result.isSuccessful) {
            val uri = result.uriContent
            if (uri != null) {
                viewModel.updateVoiceAvatar(target, uri)
            } else {
                toast(context, "裁剪失败：无输出")
            }
        } else {
            toast(context, "裁剪失败")
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val options = CropImageOptions(
                fixAspectRatio = true,
                aspectRatioX = 1,
                aspectRatioY = 1,
                outputCompressFormat = android.graphics.Bitmap.CompressFormat.PNG,
                outputCompressQuality = 100,
                outputRequestWidth = 400,
                outputRequestHeight = 400,
                outputRequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_EXACT,
                guidelines = CropImageView.Guidelines.ON
            )
            cropLauncher.launch(CropImageContractOptions(uri, options))
        } else {
            avatarTarget = null
        }
    }
    LaunchedEffect(Unit) {
        viewModel.refreshVoicePacks()
    }
    LaunchedEffect(state.voicePacks) {
        if (draggingPath == null) {
            displayPacks.clear()
            displayPacks.addAll(state.voicePacks)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = UiTokens.PageTopBlank,
            bottom = UiTokens.PageBottomBlank
        )
    ) {
        if (displayPacks.isEmpty()) {
            item {
                Text("暂无语音包，请点击主标题栏导入按钮。")
            }
        } else {
            itemsIndexed(displayPacks, key = { _, item -> item.dir.absolutePath }) { index, pack ->
                    val isCurrent = state.voiceDir?.absolutePath == pack.dir.absolutePath
                    val avatarFile = File(pack.dir, pack.meta.avatar)
                    val avatarBitmap = rememberAvatarBitmap(avatarFile)
                    val isDragging = draggingPath == pack.dir.absolutePath
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .zIndex(if (isDragging) 1f else 0f)
                            .onSizeChanged {
                                if (isDragging && it.height > 0) {
                                    draggingItemStepPx = it.height.toFloat()
                                }
                            }
                            .graphicsLayer {
                                translationY = if (isDragging) {
                                    val step = draggingItemStepPx.takeIf { v -> v > 1f } ?: defaultItemStepPx
                                    draggingRawOffsetY - ((draggingIndex - draggingBaseIndex) * step)
                                } else {
                                    0f
                                }
                            }
                            .pointerInput(pack.dir.absolutePath, displayPacks.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingPath = pack.dir.absolutePath
                                        draggingBaseIndex = index
                                        draggingIndex = index
                                        draggingRawOffsetY = 0f
                                        draggingItemStepPx = defaultItemStepPx
                                        reorderedDuringDrag = false
                                    },
                                    onDragCancel = {
                                        draggingPath = null
                                        draggingBaseIndex = -1
                                        draggingIndex = -1
                                        draggingRawOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        if (reorderedDuringDrag) {
                                            viewModel.reorderVoicePacks(displayPacks.toList())
                                        }
                                        draggingPath = null
                                        draggingBaseIndex = -1
                                        draggingIndex = -1
                                        draggingRawOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (draggingIndex !in displayPacks.indices) return@detectDragGesturesAfterLongPress
                                        val step = draggingItemStepPx.takeIf { v -> v > 1f } ?: defaultItemStepPx
                                        draggingRawOffsetY += dragAmount.y
                                        val targetIndex = (draggingBaseIndex + (draggingRawOffsetY / step).toInt())
                                            .coerceIn(0, displayPacks.lastIndex)

                                        while (draggingIndex < targetIndex) {
                                            val next = draggingIndex + 1
                                            val currentPinned = displayPacks[draggingIndex].meta.pinned
                                            val nextPinned = displayPacks[next].meta.pinned
                                            if (currentPinned != nextPinned) break
                                            displayPacks.move(draggingIndex, next)
                                            draggingIndex = next
                                            reorderedDuringDrag = true
                                        }
                                        while (draggingIndex > targetIndex) {
                                            val prev = draggingIndex - 1
                                            val currentPinned = displayPacks[draggingIndex].meta.pinned
                                            val prevPinned = displayPacks[prev].meta.pinned
                                            if (currentPinned != prevPinned) break
                                            displayPacks.move(draggingIndex, prev)
                                            draggingIndex = prev
                                            reorderedDuringDrag = true
                                        }
                                    }
                                )
                            },
                        shape = RoundedCornerShape(UiTokens.Radius),
                        backgroundColor = md2CardContainerColor(),
                        elevation = UiTokens.CardElevation
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (avatarBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = avatarBitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(UiTokens.Radius))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(UiTokens.Radius))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("无头像", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(pack.meta.name, fontWeight = FontWeight.SemiBold)
                                        if (pack.meta.pinned) {
                                            Spacer(Modifier.width(6.dp))
                                            Text("置顶", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (isCurrent) {
                                            Spacer(Modifier.width(6.dp))
                                            Text("当前", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (pack.meta.remark.isNotBlank()) {
                                        Text(pack.meta.remark, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(pack.dir.name, style = MaterialTheme.typography.bodySmall)
                                    Text("长按卡片可拖动排序", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Md2IconButton(
                                    icon = if (isCurrent) "check_circle" else "play_circle",
                                    contentDescription = if (isCurrent) "当前使用" else "使用该语音包",
                                    onClick = { viewModel.selectVoice(pack.dir) },
                                    enabled = !isCurrent
                                )
                                Md2IconButton(
                                    icon = if (pack.meta.pinned) "keep_off" else "push_pin",
                                    contentDescription = if (pack.meta.pinned) "取消置顶" else "置顶",
                                    onClick = { viewModel.toggleVoicePin(pack) }
                                )
                                Md2IconButton(
                                    icon = "edit",
                                    contentDescription = "编辑名称备注",
                                    onClick = {
                                        editPack = pack
                                        editName = pack.meta.name
                                        editRemark = pack.meta.remark
                                    }
                                )
                                Md2IconButton(
                                    icon = "image",
                                    contentDescription = "更换头像",
                                    onClick = {
                                        avatarTarget = pack
                                        imagePicker.launch("image/*")
                                    }
                                )
                                Md2IconButton(
                                    icon = "share",
                                    contentDescription = "分享语音包",
                                    onClick = { viewModel.shareVoice(pack) }
                                )
                                Md2IconButton(
                                    icon = "delete",
                                    contentDescription = "删除语音包",
                                    onClick = { deletePack = pack }
                                )
                                Md2IconButton(
                                    icon = "drag_indicator",
                                    contentDescription = "长按并拖动排序",
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
        }
    }

    if (editPack != null) {
        AlertDialog(
            onDismissRequest = { editPack = null },
            title = { Text("编辑语音包") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Md2OutlinedField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = "名称"
                    )
                    Md2OutlinedField(
                        value = editRemark,
                        onValueChange = { editRemark = it },
                        label = "备注"
                    )
                }
            },
            confirmButton = {
                Md2TextButton(onClick = {
                    val pack = editPack
                    if (pack != null) {
                        viewModel.updateVoiceMeta(pack, editName, editRemark)
                    }
                    editPack = null
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                Md2TextButton(onClick = { editPack = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (deletePack != null) {
        AlertDialog(
            onDismissRequest = { deletePack = null },
            title = { Text("删除语音包") },
            text = { Text("确定删除该语音包吗？此操作不可撤销。") },
            confirmButton = {
                Md2TextButton(onClick = {
                    val pack = deletePack
                    if (pack != null) {
                        viewModel.deleteVoice(pack)
                    }
                    deletePack = null
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                Md2TextButton(onClick = { deletePack = null }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun MutableList<VoicePackInfo>.move(from: Int, to: Int) {
    if (from == to || from !in indices || to !in indices) return
    val item = removeAt(from)
    add(to, item)
}

@Composable
fun QuickSubtitleScreen(
    viewModel: MainViewModel,
    state: UiState,
    onToggleMic: () -> Unit,
    onOpenEditor: () -> Unit
) {
    val hostActivity = LocalContext.current as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val groups = viewModel.quickSubtitleGroups
    val selectedGroupIndex = viewModel.currentQuickSubtitleGroupIndex().coerceIn(0, groups.lastIndex.coerceAtLeast(0))
    val selectedGroup = groups.getOrNull(selectedGroupIndex)
    val quickItems = selectedGroup?.items ?: emptyList()
    val subtitleText = viewModel.quickSubtitleCurrentText
    val subtitleSize = viewModel.quickSubtitleFontSizeSp
    val inputText = viewModel.quickSubtitleInputText
    val playOnSend = viewModel.quickSubtitlePlayOnSend
    var inputFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = inputText,
                selection = TextRange(inputText.length)
            )
        )
    }
    LaunchedEffect(inputText) {
        if (inputText != inputFieldValue.text) {
            inputFieldValue = TextFieldValue(
                text = inputText,
                selection = TextRange(inputText.length)
            )
        }
    }
    val hasVoice = state.voiceDir != null

    // Keep content fixed while keyboard opens; only bottom composer/FAB follow IME.
    DisposableEffect(hostActivity, lifecycleOwner) {
        val window = hostActivity?.window
        val previousMode = window?.attributes?.softInputMode
        fun applyAdjustNothing() {
            if (window != null && previousMode != null) {
                val newMode =
                    (previousMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE) or
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                window.setSoftInputMode(newMode)
            }
        }
        if (window != null && previousMode != null) {
            applyAdjustNothing()
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                applyAdjustNothing()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (window != null && previousMode != null) {
                window.setSoftInputMode(previousMode)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(UiTokens.PageTopBlank))
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 260.dp),
                shape = RoundedCornerShape(UiTokens.Radius),
                backgroundColor = md2CardContainerColor(),
                elevation = UiTokens.CardElevation
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = subtitleSize.sp,
                                lineHeight = (subtitleSize * 1.15f).sp
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MsIcon("search", contentDescription = "字体大小")
                        Slider(
                            value = subtitleSize,
                            onValueChange = { viewModel.setQuickSubtitleFontSize(it) },
                            valueRange = 28f..96f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Keep frame edges flush; reserve shadow space inside scroll content.
                Spacer(Modifier.width(8.dp))
                quickItems.forEach { text ->
                    Card(
                        modifier = Modifier
                            .padding(vertical = 3.dp)
                            .width(148.dp)
                            .height(94.dp)
                            .clickable {
                                viewModel.applyQuickSubtitleText(
                                    text = text,
                                    enqueueSpeak = hasVoice
                                )
                            },
                        shape = RoundedCornerShape(UiTokens.Radius),
                        backgroundColor = md2CardContainerColor(),
                        elevation = UiTokens.CardElevation
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(UiTokens.Radius),
                backgroundColor = md2CardContainerColor(),
                elevation = UiTokens.CardElevation
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        groups.forEachIndexed { index, group ->
                            val selected = selectedGroupIndex == index
                            val tabBg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
                            Row(
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(UiTokens.Radius))
                                    .background(tabBg)
                                    .clickable { viewModel.selectQuickSubtitleGroup(index) }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MsIcon(group.icon, contentDescription = group.title)
                                Text(group.title, maxLines = 1)
                            }
                            if (index != groups.lastIndex) {
                                Spacer(Modifier.width(2.dp))
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(52.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(onClick = onOpenEditor) {
                                MsIcon(
                                    "edit",
                                    contentDescription = "编辑快捷文本",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(UiTokens.PageBottomBlank + 92.dp))
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding(),
            shape = RectangleShape,
            color = md2CardContainerColor(),
            elevation = UiTokens.CardElevation
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Md2IconButton(
                        icon = "arrow_back",
                        contentDescription = "光标左移",
                        onClick = {
                            val current = inputFieldValue.selection.start.coerceIn(0, inputFieldValue.text.length)
                            val target = (current - 1).coerceAtLeast(0)
                            inputFieldValue = inputFieldValue.copy(selection = TextRange(target))
                        }
                    )
                    Md2IconButton(
                        icon = "arrow_forward",
                        contentDescription = "光标右移",
                        onClick = {
                            val current = inputFieldValue.selection.end.coerceIn(0, inputFieldValue.text.length)
                            val target = (current + 1).coerceAtMost(inputFieldValue.text.length)
                            inputFieldValue = inputFieldValue.copy(selection = TextRange(target))
                        }
                    )
                    Md2IconButton(
                        icon = if (playOnSend) "volume_up" else "volume_off",
                        contentDescription = if (playOnSend) "发送时播放语音：开" else "发送时播放语音：关",
                        onClick = {
                            viewModel.updateQuickSubtitlePlayOnSend(!playOnSend)
                        }
                    )
                    Md2IconButton(
                        icon = "play_arrow",
                        contentDescription = "朗读当前字幕",
                        onClick = {
                            viewModel.applyQuickSubtitleText(subtitleText, enqueueSpeak = hasVoice)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputFieldValue,
                        onValueChange = {
                            inputFieldValue = it
                            viewModel.updateQuickSubtitleInputText(it.text)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("请输入文本") },
                        trailingIcon = {
                            if (inputFieldValue.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        inputFieldValue = TextFieldValue("")
                                        viewModel.updateQuickSubtitleInputText("")
                                    }
                                ) {
                                    MsIcon("close", contentDescription = "清空输入")
                                }
                            }
                        },
                        shape = RoundedCornerShape(UiTokens.Radius),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(
                        onClick = {
                            viewModel.submitQuickSubtitleInput(
                                playVoice = playOnSend && hasVoice
                            )
                            inputFieldValue = TextFieldValue("")
                        },
                        enabled = inputFieldValue.text.trim().isNotEmpty()
                    ) {
                        MsIcon(
                            name = "send",
                            contentDescription = "发送到朗读队列",
                            tint = if (inputFieldValue.text.trim().isNotEmpty()) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onToggleMic,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .imePadding()
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 80.dp),
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = UiTokens.FabElevation,
                pressedElevation = 12.dp
            )
        ) {
            MsIcon(
                name = if (state.running) "stop" else "play_arrow",
                contentDescription = if (state.running) "关闭麦克风" else "开启麦克风",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun QuickSubtitleEditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val groups = viewModel.quickSubtitleGroups
    var selectedGroupIndex by remember(groups, viewModel.quickSubtitleSelectedGroupId) {
        mutableIntStateOf(
            viewModel.currentQuickSubtitleGroupIndex().coerceIn(0, groups.lastIndex.coerceAtLeast(0))
        )
    }
    val selectedGroup = groups.getOrNull(selectedGroupIndex)
    val iconChoices = listOf(
        "sentiment_satisfied",
        "sentiment_very_satisfied",
        "sentiment_neutral",
        "sentiment_dissatisfied",
        "record_voice_over",
        "sports_esports",
        "work",
        "favorite",
        "chat",
        "emoji_people"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = UiTokens.CardElevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("分组", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Md2TextButton(onClick = { viewModel.addQuickSubtitleGroup() }) {
                        MsIcon("add", contentDescription = "新增分组")
                        Spacer(Modifier.width(4.dp))
                        Text("新增")
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groups.forEachIndexed { idx, group ->
                        val selected = idx == selectedGroupIndex
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(UiTokens.Radius))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                                .clickable {
                                    selectedGroupIndex = idx
                                    viewModel.selectQuickSubtitleGroup(idx)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MsIcon(group.icon, contentDescription = group.title)
                            Text(group.title)
                            Text("(${group.items.size})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (selectedGroup != null) {
                    Md2OutlinedField(
                        value = selectedGroup.title,
                        onValueChange = {
                            viewModel.updateQuickSubtitleGroupMeta(
                                selectedGroupIndex,
                                it,
                                selectedGroup.icon
                            )
                        },
                        label = "分组名称",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        iconChoices.forEach { icon ->
                            val selected = icon == selectedGroup.icon
                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.updateQuickSubtitleGroupMeta(
                                            selectedGroupIndex,
                                            selectedGroup.title,
                                            icon
                                        )
                                    },
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    MsIcon(icon, contentDescription = icon)
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Md2IconButton(
                            icon = "arrow_back",
                            contentDescription = "分组左移",
                            onClick = {
                                if (selectedGroupIndex > 0) {
                                    viewModel.moveQuickSubtitleGroup(selectedGroupIndex, selectedGroupIndex - 1)
                                    selectedGroupIndex -= 1
                                }
                            },
                            enabled = selectedGroupIndex > 0
                        )
                        Md2IconButton(
                            icon = "arrow_forward",
                            contentDescription = "分组右移",
                            onClick = {
                                if (selectedGroupIndex < groups.lastIndex) {
                                    viewModel.moveQuickSubtitleGroup(selectedGroupIndex, selectedGroupIndex + 1)
                                    selectedGroupIndex += 1
                                }
                            },
                            enabled = selectedGroupIndex < groups.lastIndex
                        )
                        Md2IconButton(
                            icon = "delete",
                            contentDescription = "删除分组",
                            onClick = {
                                viewModel.removeQuickSubtitleGroup(selectedGroupIndex)
                                selectedGroupIndex = viewModel.currentQuickSubtitleGroupIndex()
                            },
                            enabled = groups.size > 1
                        )
                    }
                }
            }
        }

        if (selectedGroup != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UiTokens.Radius),
                backgroundColor = md2CardContainerColor(),
                elevation = UiTokens.CardElevation
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("快捷文本", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Md2TextButton(onClick = { viewModel.addQuickSubtitleItem(selectedGroupIndex) }) {
                            MsIcon("add", contentDescription = "新增文本")
                            Spacer(Modifier.width(4.dp))
                            Text("新增")
                        }
                    }
                    selectedGroup.items.forEachIndexed { itemIndex, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedTextField(
                                value = item,
                                onValueChange = {
                                    viewModel.updateQuickSubtitleItem(selectedGroupIndex, itemIndex, it)
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = false,
                                maxLines = 2,
                                shape = RoundedCornerShape(UiTokens.Radius),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Column {
                                Md2IconButton(
                                    icon = "arrow_upward",
                                    contentDescription = "上移",
                                    onClick = {
                                        if (itemIndex > 0) {
                                            viewModel.moveQuickSubtitleItem(selectedGroupIndex, itemIndex, itemIndex - 1)
                                        }
                                    },
                                    enabled = itemIndex > 0
                                )
                                Md2IconButton(
                                    icon = "arrow_downward",
                                    contentDescription = "下移",
                                    onClick = {
                                        if (itemIndex < selectedGroup.items.lastIndex) {
                                            viewModel.moveQuickSubtitleItem(selectedGroupIndex, itemIndex, itemIndex + 1)
                                        }
                                    },
                                    enabled = itemIndex < selectedGroup.items.lastIndex
                                )
                            }
                            Md2IconButton(
                                icon = "delete",
                                contentDescription = "删除文本",
                                onClick = {
                                    viewModel.removeQuickSubtitleItem(selectedGroupIndex, itemIndex)
                                },
                                enabled = selectedGroup.items.size > 1
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(UiTokens.PageBottomBlank))
    }
}

@Composable
fun RealtimeScreen(viewModel: MainViewModel, state: UiState) {
    val inputLevel = viewModel.realtimeInputLevel
    val recognized = viewModel.realtimeRecognized
    val bottomPadding = UiTokens.PageBottomBlank
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = UiTokens.PageTopBlank,
            bottom = bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("输入音量", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = inputLevel.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
            )
            Text("当前输入设备：${state.inputDeviceLabel}", style = MaterialTheme.typography.bodySmall)
            Text("当前输出设备：${state.outputDeviceLabel}", style = MaterialTheme.typography.bodySmall)
            Text("状态：${state.status}")
            Spacer(Modifier.height(8.dp))
            Text("识别结果", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
        }

        if (recognized.isEmpty()) {
            item {
                Text("暂无识别结果", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(recognized, key = { it.id }) { item ->
                RecognizedQueueItemCard(item)
            }
        }
    }
}

@Composable
fun TextToSpeechScreen(viewModel: MainViewModel, state: UiState) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val queued = viewModel.realtimeRecognized
    val canSend = inputText.trim().isNotEmpty() && state.voiceDir != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = UiTokens.PageTopBlank,
            bottom = UiTokens.PageBottomBlank
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UiTokens.Radius),
                backgroundColor = md2CardContainerColor(),
                elevation = UiTokens.CardElevation
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("输入要朗读的文本", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        label = { Text("文本内容") },
                        maxLines = 8,
                        shape = RoundedCornerShape(UiTokens.Radius),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Md2Button(
                            onClick = {
                                val text = inputText.trim()
                                if (text.isNotEmpty()) {
                                    viewModel.speakText(text)
                                    inputText = ""
                                }
                            },
                            enabled = canSend
                        ) {
                            MsIcon("send", contentDescription = "发送朗读")
                            Spacer(Modifier.width(6.dp))
                            Text("发送")
                        }
                        Md2TextButton(
                            onClick = { inputText = "" },
                            enabled = inputText.isNotEmpty()
                        ) {
                            MsIcon("close", contentDescription = "清空输入")
                            Spacer(Modifier.width(6.dp))
                            Text("清空")
                        }
                    }
                    if (state.voiceDir == null) {
                        Text("请先在“语音包”页面选择一个语音包。", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("当前语音包：${state.voiceDir.absolutePath}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text("朗读队列", fontWeight = FontWeight.Bold)
        }

        if (queued.isEmpty()) {
            item {
                Text("暂无队列内容", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(queued, key = { it.id }) { item ->
                RecognizedQueueItemCard(item)
            }
        }
    }
}

@Composable
private fun RecognizedQueueItemCard(item: RecognizedItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.Radius),
        backgroundColor = md2CardContainerColor(),
        elevation = UiTokens.CardElevation
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(item.text)
            LinearProgressIndicator(
                progress = item.progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun RunningStatusTopStrip(
    viewModel: MainViewModel,
    status: String,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputLevel = viewModel.realtimeInputLevel
    val playbackProgress = viewModel.realtimePlaybackProgress
    Surface(
        modifier = modifier,
        shape = RectangleShape,
        color = md2CardContainerColor(),
        elevation = UiTokens.MenuElevation
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Md2IconButton(
                    icon = "expand_less",
                    contentDescription = "折叠状态条",
                    onClick = onToggleCollapsed
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MsIcon("mic", contentDescription = "麦克风音量")
                LinearProgressIndicator(
                    progress = inputLevel.coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MsIcon("graphic_eq", contentDescription = "识别进度")
                LinearProgressIndicator(
                    progress = playbackProgress.coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RunningStripTopBarToggle(
    micLevel: Float,
    playbackProgress: Float,
    expanded: Boolean,
    contentColor: Color,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onToggle
            ),
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MsIcon("mic", contentDescription = "麦克风音量", tint = contentColor)
                LinearProgressIndicator(
                    progress = micLevel.coerceIn(0f, 1f),
                    modifier = Modifier
                        .width(30.dp)
                        .height(2.dp),
                    color = contentColor,
                    backgroundColor = contentColor.copy(alpha = 0.24f)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MsIcon("graphic_eq", contentDescription = "播放进度", tint = contentColor)
                LinearProgressIndicator(
                    progress = playbackProgress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .width(30.dp)
                        .height(2.dp),
                    color = contentColor,
                    backgroundColor = contentColor.copy(alpha = 0.24f)
                )
            }
            MsIcon(
                name = if (expanded) "expand_less" else "expand_more",
                contentDescription = if (expanded) "收起状态条" else "展开状态条",
                tint = contentColor
            )
        }
    }
}

@Composable
fun DrawingBoardScreen(
    viewModel: MainViewModel,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isDark = isSystemInDarkTheme()
    val rotationDegrees = when (context.display?.rotation ?: Surface.ROTATION_0) {
        Surface.ROTATION_90 -> 90f
        Surface.ROTATION_180 -> 180f
        Surface.ROTATION_270 -> 270f
        else -> 0f
    }
    val boardOutlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    val boardFillColor = if (isDark) Color(0xFF2C3237) else Color(0xFFFCFDFE)
    val currentPoints = remember { mutableStateListOf<DrawPoint>() }
    val palette = if (isDark) {
        listOf(
            Color(0xFF7DE8EA),
            Color(0xFF90CAF9),
            Color(0xFFFF9E9E),
            Color(0xFFAEE5B3),
            Color(0xFFFFE08A),
            Color(0xFFECEFF1),
            Color(0xFFD1C4E9)
        )
    } else {
        listOf(
            UiTokens.Primary,
            Color(0xFF1E88E5),
            Color(0xFFE53935),
            Color(0xFF43A047),
            Color(0xFFFFA000),
            Color(0xFF212121),
            Color(0xFF5E35B1)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (fullscreen) 0.dp else 16.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasW = constraints.maxWidth.toFloat()
            val canvasH = constraints.maxHeight.toFloat()
            val boardAspect = 1080f / 1920f
            val quarterTurn = rotationDegrees == 90f || rotationDegrees == 270f
            val fitW: Float
            val fitH: Float
            if (quarterTurn) {
                fitH = minOf(canvasW, canvasH / boardAspect)
                fitW = fitH * boardAspect
            } else {
                fitW = minOf(canvasW, canvasH * boardAspect)
                fitH = fitW / boardAspect
            }
            val left = (canvasW - fitW) / 2f
            val top = (canvasH - fitH) / 2f
            val pxScale = fitW / 1080f
            val center = Offset(canvasW / 2f, canvasH / 2f)
            val activeWidth = if (viewModel.drawEraser) viewModel.drawEraserSize * 5f else viewModel.drawBrushSize

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        rotationDegrees,
                        fitW,
                        fitH,
                        left,
                        top,
                        viewModel.drawBrushSize,
                        viewModel.drawEraserSize,
                        viewModel.drawColor,
                        viewModel.drawEraser
                    ) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val mapped = offset.rotateAround(center, rotationDegrees)
                                if (!mapped.isInsideBoard(left, top, fitW, fitH)) return@detectDragGestures
                                currentPoints.clear()
                                currentPoints.add(mapped.toDrawPoint(left, top, fitW, fitH))
                            },
                            onDrag = { change, _ ->
                                if (currentPoints.isNotEmpty()) {
                                    val mapped = change.position.rotateAround(center, rotationDegrees)
                                    currentPoints.add(mapped.toDrawPoint(left, top, fitW, fitH))
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                if (currentPoints.size > 1) {
                                    viewModel.appendDrawingStroke(currentPoints.toList())
                                }
                                currentPoints.clear()
                            },
                            onDragCancel = { currentPoints.clear() }
                        )
                    }
            ) {
                withTransform({
                    rotate(degrees = -rotationDegrees, pivot = center)
                }) {
                    drawRoundRect(
                        color = boardFillColor,
                        topLeft = Offset(left, top),
                        size = Size(fitW, fitH),
                        cornerRadius = CornerRadius(UiTokens.Radius.toPx(), UiTokens.Radius.toPx())
                    )
                    drawRoundRect(
                        color = boardOutlineColor,
                        topLeft = Offset(left, top),
                        size = Size(fitW, fitH),
                        cornerRadius = CornerRadius(UiTokens.Radius.toPx(), UiTokens.Radius.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    viewModel.drawStrokes.forEach { stroke ->
                        drawStrokeOnBoard(
                            points = stroke.points,
                            color = if (stroke.eraser) boardFillColor else stroke.color,
                            width = stroke.width * pxScale,
                            left = left,
                            top = top,
                            widthPx = fitW,
                            heightPx = fitH
                        )
                    }
                    if (currentPoints.size > 1) {
                        drawStrokeOnBoard(
                            points = currentPoints,
                            color = if (viewModel.drawEraser) boardFillColor else viewModel.drawColor,
                            width = activeWidth * pxScale,
                            left = left,
                            top = top,
                            widthPx = fitW,
                            heightPx = fitH
                        )
                    }
                }
            }
        }

        DrawingToolbar(
            modifier = if (isLandscape) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .navigationBarsPadding()
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .navigationBarsPadding()
            },
            isLandscape = isLandscape,
            colors = palette,
            selectedColor = viewModel.drawColor,
            brushSize = viewModel.drawBrushSize,
            eraserSize = viewModel.drawEraserSize,
            eraserEnabled = viewModel.drawEraser,
            fullscreen = fullscreen,
            onToggleFullscreen = onToggleFullscreen,
            onPickColor = { viewModel.updateDrawColor(it) },
            onBrushSize = { viewModel.updateDrawBrushSize(it) },
            onToggleEraser = { viewModel.updateDrawEraser(it) },
            onClear = { viewModel.clearDrawingBoard() }
        )
    }
}

@Composable
private fun DrawingToolbar(
    modifier: Modifier = Modifier,
    isLandscape: Boolean,
    colors: List<Color>,
    selectedColor: Color,
    brushSize: Float,
    eraserSize: Float,
    eraserEnabled: Boolean,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onPickColor: (Color) -> Unit,
    onBrushSize: (Float) -> Unit,
    onToggleEraser: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(UiTokens.Radius),
        backgroundColor = md2CardContainerColor(),
        elevation = 6.dp
    ) {
        val activeSize = if (eraserEnabled) eraserSize else brushSize
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .height(248.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Md2ToolToggle(
                        icon = "edit",
                        selected = !eraserEnabled,
                        onClick = { onToggleEraser(false) },
                        contentDescription = "画笔"
                    )
                    Md2ToolToggle(
                        icon = "ink_eraser",
                        selected = eraserEnabled,
                        onClick = { onToggleEraser(true) },
                        contentDescription = "橡皮擦"
                    )
                    Md2ToolToggle(
                        icon = "delete_sweep",
                        selected = false,
                        onClick = onClear,
                        contentDescription = "清空"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(34.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            colors.forEach { color ->
                                Md2ColorDot(
                                    color = color,
                                    selected = !eraserEnabled && selectedColor == color,
                                    onClick = { onPickColor(color) }
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(52.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Md2VerticalSlider(
                        value = activeSize,
                        onValueChange = onBrushSize,
                        valueRange = 2f..48f,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    Md2ToolToggle(
                        icon = if (fullscreen) "fullscreen_exit" else "fullscreen",
                        selected = false,
                        onClick = onToggleFullscreen,
                        contentDescription = if (fullscreen) "退出全屏" else "进入全屏"
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Md2ToolToggle(
                            icon = "edit",
                            selected = !eraserEnabled,
                            onClick = { onToggleEraser(false) },
                            contentDescription = "画笔"
                        )
                        Md2ToolToggle(
                            icon = "ink_eraser",
                            selected = eraserEnabled,
                            onClick = { onToggleEraser(true) },
                            contentDescription = "橡皮擦"
                        )
                        Md2ToolToggle(
                            icon = "delete_sweep",
                            selected = false,
                            onClick = onClear,
                            contentDescription = "清空"
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { color ->
                            Md2ColorDot(
                                color = color,
                                selected = !eraserEnabled && selectedColor == color,
                                onClick = { onPickColor(color) }
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = activeSize,
                        onValueChange = onBrushSize,
                        valueRange = 2f..48f,
                        modifier = Modifier.weight(1f)
                    )
                    Md2ToolToggle(
                        icon = if (fullscreen) "fullscreen_exit" else "fullscreen",
                        selected = false,
                        onClick = onToggleFullscreen,
                        contentDescription = if (fullscreen) "退出全屏" else "进入全屏"
                    )
                }
            }
        }
    }
}

@Composable
private fun Md2ToolToggle(
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ),
        color = bg,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            MsIcon(icon, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun Md2ColorDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Surface(
        modifier = Modifier
            .size(22.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ),
        shape = CircleShape,
        color = color,
        border = BorderStroke(1.5.dp, borderColor)
    ) {}
}

@Composable
private fun Md2VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val range = (max - min).coerceAtLeast(0.0001f)
    val coerced = value.coerceIn(min, max)
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val activeColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .pointerInput(min, max) {
                fun yToValue(y: Float): Float {
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val frac = (1f - (y / h)).coerceIn(0f, 1f)
                    return (min + frac * range).coerceIn(min, max)
                }
                detectDragGestures(
                    onDragStart = { offset ->
                        onValueChange(yToValue(offset.y))
                    },
                    onDrag = { change, _ ->
                        onValueChange(yToValue(change.position.y))
                        change.consume()
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp)
        ) {
            val trackX = size.width / 2f
            val startY = 0f
            val endY = size.height
            val fraction = ((coerced - min) / range).coerceIn(0f, 1f)
            val thumbY = endY - (endY - startY) * fraction
            val trackW = 4.dp.toPx()

            drawLine(
                color = outlineColor,
                start = Offset(trackX, startY),
                end = Offset(trackX, endY),
                strokeWidth = trackW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = activeColor,
                start = Offset(trackX, endY),
                end = Offset(trackX, thumbY),
                strokeWidth = trackW,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = activeColor,
                radius = 9.dp.toPx(),
                center = Offset(trackX, thumbY)
            )
        }
    }
}

private fun Offset.isInsideBoard(left: Float, top: Float, width: Float, height: Float): Boolean {
    return x >= left && x <= left + width && y >= top && y <= top + height
}

private fun Offset.rotateAround(center: Offset, degrees: Float): Offset {
    if (degrees == 0f) return this
    val rad = Math.toRadians(degrees.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    val dx = x - center.x
    val dy = y - center.y
    return Offset(
        x = center.x + dx * c - dy * s,
        y = center.y + dx * s + dy * c
    )
}

private fun Offset.toDrawPoint(left: Float, top: Float, width: Float, height: Float): DrawPoint {
    return DrawPoint(
        x = ((x - left) / width).coerceIn(0f, 1f),
        y = ((y - top) / height).coerceIn(0f, 1f)
    )
}

private fun DrawPoint.toOffset(left: Float, top: Float, width: Float, height: Float): Offset {
    return Offset(
        x = left + x * width,
        y = top + y * height
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokeOnBoard(
    points: List<DrawPoint>,
    color: Color,
    width: Float,
    left: Float,
    top: Float,
    widthPx: Float,
    heightPx: Float
) {
    if (points.size < 2) return
    for (i in 1 until points.size) {
        drawLine(
            color = color,
            start = points[i - 1].toOffset(left, top, widthPx, heightPx),
            end = points[i].toOffset(left, top, widthPx, heightPx),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, state: UiState) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val drawerModeOptions = listOf(
        UserPrefs.DRAWER_MODE_HIDDEN to "隐藏式抽屉",
        UserPrefs.DRAWER_MODE_PERMANENT to "常驻可折叠"
    )
    val inputTypeOptions = listOf(
        AudioRoutePreference.INPUT_AUTO to "自动",
        AudioRoutePreference.INPUT_BUILTIN_MIC to "内置麦克风/话筒",
        AudioRoutePreference.INPUT_USB to "USB 麦克风",
        AudioRoutePreference.INPUT_BLUETOOTH to "蓝牙麦克风",
        AudioRoutePreference.INPUT_WIRED to "有线麦克风"
    )
    val outputTypeOptions = listOf(
        AudioRoutePreference.OUTPUT_AUTO to "自动",
        AudioRoutePreference.OUTPUT_SPEAKER to "扬声器",
        AudioRoutePreference.OUTPUT_EARPIECE to "听筒",
        AudioRoutePreference.OUTPUT_BLUETOOTH to "蓝牙音频",
        AudioRoutePreference.OUTPUT_USB to "USB 音频",
        AudioRoutePreference.OUTPUT_WIRED to "有线耳机/线路"
    )
    var drawerModeExpanded by remember { mutableStateOf(false) }
    var inputTypeExpanded by remember { mutableStateOf(false) }
    var outputTypeExpanded by remember { mutableStateOf(false) }
    val asrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.importAsr(uri) else toast(context, "未选择文件")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(UiTokens.PageTopBlank))

        Md2SettingsCard(title = "模型与资源") {
            Text("ASR 模型 (sosv-int8.zip)", fontWeight = FontWeight.Bold)
            Text(state.asrDir?.absolutePath ?: "未导入", style = MaterialTheme.typography.bodySmall)
            Md2Button(onClick = { asrPicker.launch("*/*") }) {
                Text("导入 ASR 模型")
            }
        }

        Md2SettingsCard(title = "设备监控") {
            val realtimeInputLevel = viewModel.realtimeInputLevel
            Text("输入音量", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = realtimeInputLevel.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            )
            Text("当前输入设备：${state.inputDeviceLabel}", style = MaterialTheme.typography.bodySmall)
            Text("当前输出设备：${state.outputDeviceLabel}", style = MaterialTheme.typography.bodySmall)
        }

        Md2SettingsCard(title = "系统与布局") {
            Text("横屏抽屉模式", fontWeight = FontWeight.Bold)
            Box {
                Md2OutlinedButton(onClick = { drawerModeExpanded = true }) {
                    val label = drawerModeOptions.firstOrNull { it.first == state.landscapeDrawerMode }?.second
                        ?: drawerModeOptions.first().second
                    Text(label)
                }
                DropdownMenu(
                    expanded = drawerModeExpanded,
                    onDismissRequest = { drawerModeExpanded = false }
                ) {
                    drawerModeOptions.forEach { (value, label) ->
                        M2DropdownMenuItem(
                            onClick = {
                                drawerModeExpanded = false
                                viewModel.setLandscapeDrawerMode(value)
                            }
                        ) { Text(label) }
                    }
                }
            }
            Text("竖屏始终为隐藏式；该选项仅影响横屏布局。", style = MaterialTheme.typography.bodySmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.solidTopBar,
                    onCheckedChange = { viewModel.setSolidTopBar(it) }
                )
                Text("使用纯色顶栏")
            }
            Text("开启后顶栏与状态栏颜色改为卡片同款自适应配色。", style = MaterialTheme.typography.bodySmall)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.keepAlive,
                    onCheckedChange = { viewModel.setKeepAlive(it) }
                )
                Text("保持后台运行")
            }
            Text("开启后启用前台服务，锁屏/息屏也持续工作", style = MaterialTheme.typography.bodySmall)
        }

        Md2SettingsCard(title = "识别与转换") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.muteWhilePlaying,
                    onCheckedChange = { viewModel.setMuteWhilePlaying(it) }
                )
                Text("播放时屏蔽录音")
            }
            Text("开启后播放中不进行识别", style = MaterialTheme.typography.bodySmall)
            Text("屏蔽结束延迟：${String.format("%.1f", state.muteWhilePlayingDelaySec)}s", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.muteWhilePlayingDelaySec,
                onValueChange = { viewModel.setMuteWhilePlayingDelay(it) },
                valueRange = 0f..5f
            )
            Text("语音识别最低音量阈值：${state.minVolumePercent}%", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.minVolumePercent.toFloat(),
                onValueChange = { viewModel.setMinVolumePercent(it.toInt()) },
                valueRange = 0f..100f
            )
            Text("播放音量增益：${state.playbackGainPercent}%", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.playbackGainPercent.toFloat(),
                onValueChange = { viewModel.setPlaybackGainPercent(it.toInt()) },
                valueRange = 0f..1000f
            )
            Text("100% 为原始音量，>100% 为倍率增益", style = MaterialTheme.typography.bodySmall)
            val numberReplaceOptions = listOf("不替换", "数字替换为中文字符", "数字替换为中文表达")
            var numberReplaceExpanded by remember { mutableStateOf(false) }
            Text("数字替换", fontWeight = FontWeight.Bold)
            Box {
                Md2OutlinedButton(onClick = { numberReplaceExpanded = true }) {
                    val label = numberReplaceOptions.getOrElse(state.numberReplaceMode) { numberReplaceOptions[0] }
                    Text(label)
                }
                DropdownMenu(
                    expanded = numberReplaceExpanded,
                    onDismissRequest = { numberReplaceExpanded = false }
                ) {
                    numberReplaceOptions.forEachIndexed { idx, label ->
                        M2DropdownMenuItem(
                            onClick = {
                                numberReplaceExpanded = false
                                viewModel.setNumberReplaceMode(idx)
                            }
                        ) { Text(label) }
                    }
                }
            }
            Text("示例：2000 → 二零零零 / 两千", style = MaterialTheme.typography.bodySmall)
        }

        Md2SettingsCard(title = "回声与降噪") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.echoSuppression,
                    onCheckedChange = { viewModel.setEchoSuppression(it) }
                )
                Text("回声抑制(VOICE_COMMUNICATION)")
            }
            Text("开启后使用通话录音源，可能有回声抑制/降噪效果", style = MaterialTheme.typography.bodySmall)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.communicationMode,
                    onCheckedChange = { viewModel.setCommunicationMode(it) }
                )
                Text("通话模式降噪(MODE_IN_COMMUNICATION)")
            }
            Text("开启后切换系统通话模式并统一播放属性", style = MaterialTheme.typography.bodySmall)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.aec3Enabled,
                    onCheckedChange = { viewModel.setAec3Enabled(it) }
                )
                Text("AEC3 软件回声消除")
            }
            Text("AEC3 状态：${state.aec3Status}", style = MaterialTheme.typography.bodySmall)
            Text(state.aec3Diag, style = MaterialTheme.typography.bodySmall)
            Text("需渲染参考音频，可能与系统AEC冲突", style = MaterialTheme.typography.bodySmall)
        }

        Md2SettingsCard(title = "设备路由") {
            Text("优先选择的音频输入设备类型", style = MaterialTheme.typography.bodySmall)
            Box {
                Md2OutlinedButton(onClick = { inputTypeExpanded = true }) {
                    val label = inputTypeOptions.firstOrNull { it.first == state.preferredInputType }?.second
                        ?: inputTypeOptions.first().second
                    Text(label)
                }
                DropdownMenu(
                    expanded = inputTypeExpanded,
                    onDismissRequest = { inputTypeExpanded = false }
                ) {
                    inputTypeOptions.forEach { (value, label) ->
                        M2DropdownMenuItem(
                            onClick = {
                                inputTypeExpanded = false
                                viewModel.setPreferredInputType(value)
                            }
                        ) { Text(label) }
                    }
                }
            }
            Text("适配内置、USB、蓝牙、有线等输入设备", style = MaterialTheme.typography.bodySmall)

            Text("优先使用的音频输出类型", style = MaterialTheme.typography.bodySmall)
            Box {
                Md2OutlinedButton(onClick = { outputTypeExpanded = true }) {
                    val label = outputTypeOptions.firstOrNull { it.first == state.preferredOutputType }?.second
                        ?: outputTypeOptions.first().second
                    Text(label)
                }
                DropdownMenu(
                    expanded = outputTypeExpanded,
                    onDismissRequest = { outputTypeExpanded = false }
                ) {
                    outputTypeOptions.forEach { (value, label) ->
                        M2DropdownMenuItem(
                            onClick = {
                                outputTypeExpanded = false
                                viewModel.setPreferredOutputType(value)
                            }
                        ) { Text(label) }
                    }
                }
            }
            Text("适配扬声器、听筒、蓝牙、USB、有线等输出设备", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(UiTokens.PageBottomBlank))
    }
}

@Composable
private fun Md2SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.Radius),
        backgroundColor = md2CardContainerColor(),
        elevation = UiTokens.CardElevation
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                content()
            }
        )
    }
}

@Composable
fun LogScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var logs by remember { mutableStateOf<List<File>>(emptyList()) }
    var selected by remember { mutableStateOf<File?>(null) }
    var content by remember { mutableStateOf("加载中...") }
    var expanded by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    fun refreshLogs() {
        logs = AppLogger.listLogFiles(context)
        if (selected == null || selected !in logs) {
            selected = logs.firstOrNull()
        }
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    LaunchedEffect(selected) {
        val file = selected
        content = if (file == null) {
            "暂无日志"
        } else {
            withContext(Dispatchers.IO) {
                AppLogger.readLog(file)
            }.ifEmpty { "日志为空" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                Md2OutlinedButton(onClick = { expanded = true }) {
                    Text(selected?.name ?: "选择日志")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    logs.forEach { file ->
                        M2DropdownMenuItem(
                            onClick = {
                                selected = file
                                expanded = false
                            }
                        ) { Text(file.name) }
                    }
                }
            }
            Md2Button(onClick = { refreshLogs() }) { Text("刷新") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Md2Button(
                onClick = {
                    clipboard.setText(AnnotatedString(content))
                    toast(context, "已复制")
                },
                enabled = content.isNotBlank()
            ) { Text("复制") }
            Md2Button(
                onClick = {
                    val file = selected
                    if (file != null) {
                        shareLogFile(context, file)
                    } else {
                        toast(context, "暂无可分享日志")
                    }
                },
                enabled = selected != null
            ) { Text("分享") }
        }
        if (selected != null) {
            Text("路径：${selected!!.absolutePath}", style = MaterialTheme.typography.bodySmall)
        }
        Divider()
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scroll)
        ) {
            Text(
                content,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun toast(context: android.content.Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

private fun shareLogFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享日志"))
    } catch (e: Exception) {
        toast(context, "分享失败: ${e.message}")
    }
}
