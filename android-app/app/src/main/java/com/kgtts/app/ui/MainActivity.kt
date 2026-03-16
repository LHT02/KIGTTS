package com.kgtts.app.ui

import android.annotation.SuppressLint
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
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
import com.kgtts.app.audio.SpeakerEnrollResult
import com.kgtts.app.data.ModelRepository
import com.kgtts.app.data.VoicePackInfo
import com.kgtts.app.data.UserPrefs
import com.kgtts.app.overlay.FloatingOverlayService
import com.kgtts.app.overlay.OverlayBridge
import com.kgtts.app.overlay.RealtimeOwnerGate
import com.kgtts.app.overlay.RealtimeRuntimeBridge
import com.kgtts.app.service.KeepAliveService
import com.kgtts.app.util.AppLogger
import com.kgtts.app.util.QuickCardRenderCache
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.roundToInt

private fun isXiaomiFamilyDevice(): Boolean {
    val m = Build.MANUFACTURER?.lowercase() ?: return false
    return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")
}

private fun softInputModeSummary(mode: Int): String {
    val adjust = when (mode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) {
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE -> "resize"
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN -> "pan"
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING -> "nothing"
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED -> "unspecified"
        else -> "unknown"
    }
    val state = when (mode and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE) {
        WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED -> "unspecified"
        WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED -> "unchanged"
        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN -> "hidden"
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN -> "always_hidden"
        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE -> "visible"
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE -> "always_visible"
        else -> "unknown"
    }
    return "adjust=$adjust,state=$state,raw=0x${mode.toString(16)}"
}

private fun normalizeQrTextToWebUrl(raw: String): String? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    val parsed = runCatching { Uri.parse(text) }.getOrNull() ?: return null
    val scheme = parsed.scheme?.lowercase(Locale.US).orEmpty()
    if (scheme == "http" || scheme == "https") return text
    if (scheme.isNotEmpty()) return null
    if (text.contains(Regex("\\s"))) return null
    return "https://$text"
}

private fun normalizeDrawingSaveRelativePath(raw: String): String {
    val cleaned = raw
        .trim()
        .replace('\\', '/')
        .trim('/')
    if (cleaned.isEmpty()) {
        return UserPrefs.DEFAULT_DRAWING_SAVE_RELATIVE_PATH
    }
    val normalized = cleaned
        .split('/')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "." && it != ".." }
        .joinToString("/")
    return if (normalized.isEmpty()) {
        UserPrefs.DEFAULT_DRAWING_SAVE_RELATIVE_PATH
    } else {
        normalized
    }
}

private const val PLAYBACK_GAIN_SNAP_TARGET = 100
private const val PLAYBACK_GAIN_SNAP_RANGE = 20

private fun snapPlaybackGainPercent(percent: Int): Int {
    val clamped = percent.coerceIn(0, 1000)
    return if (kotlin.math.abs(clamped - PLAYBACK_GAIN_SNAP_TARGET) <= PLAYBACK_GAIN_SNAP_RANGE) {
        PLAYBACK_GAIN_SNAP_TARGET
    } else {
        clamped
    }
}

private fun drawingRelativePathFromTreeUri(uri: android.net.Uri): String? {
    val treeId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return null
    val sep = treeId.indexOf(':')
    if (sep <= 0 || sep >= treeId.length - 1) return null
    val volume = treeId.substring(0, sep).lowercase(Locale.US)
    val rawPath = treeId.substring(sep + 1)
    return when (volume) {
        "primary" -> normalizeDrawingSaveRelativePath(rawPath)
        "home" -> {
            val tail = rawPath.trim().trim('/')
            normalizeDrawingSaveRelativePath(
                if (tail.isEmpty()) "Documents" else "Documents/$tail"
            )
        }
        else -> null
    }
}

private const val QUICK_SUBTITLE_CLEARED_HINT = "我不太方便说话，请等我一下……"

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
    val piperNoiseScale: Float = 0.667f,
    val piperLengthScale: Float = 1.0f,
    val piperNoiseW: Float = 0.8f,
    val piperSentenceSilence: Float = 0.2f,
    val keepAlive: Boolean = false,
    val numberReplaceMode: Int = 0,
    val landscapeDrawerMode: Int = UserPrefs.DRAWER_MODE_PERMANENT,
    val solidTopBar: Boolean = true,
    val drawingSaveRelativePath: String = UserPrefs.DEFAULT_DRAWING_SAVE_RELATIVE_PATH,
    val quickCardAutoSaveOnExit: Boolean = false,
    val asrSendToQuickSubtitle: Boolean = true,
    val pushToTalkMode: Boolean = false,
    val pushToTalkConfirmInputMode: Boolean = false,
    val floatingOverlayEnabled: Boolean = false,
    val floatingOverlayAutoDock: Boolean = false,
    val pushToTalkPressed: Boolean = false,
    val pushToTalkStreamingText: String = "",
    val speakerVerifyEnabled: Boolean = false,
    val speakerVerifyThreshold: Float = 0.72f,
    val speakerProfileReady: Boolean = false,
    val speakerProfiles: List<SpeakerProfileUiItem> = emptyList(),
    val speakerLastSimilarity: Float = -1f,
    val inputLevel: Float = 0f,
    val inputDeviceLabel: String = "未知",
    val outputDeviceLabel: String = "未知"
)

data class SpeakerProfileUiItem(
    val id: String,
    val name: String
)

data class RecognizedItem(
    val id: Long,
    val text: String,
    val progress: Float = 0f
)

data class ExternalQuickSubtitleRequest(
    val requestId: Long,
    val target: String,
    val text: String
)

private fun isOverlayOpenTarget(target: String): Boolean {
    return target == OverlayBridge.TARGET_OPEN ||
            target == OverlayBridge.TARGET_OPEN_OVERLAY ||
            target == OverlayBridge.TARGET_OPEN_QUICK_CARD ||
            target == OverlayBridge.TARGET_OPEN_DRAWING ||
            target == OverlayBridge.TARGET_OPEN_SETTINGS ||
            target == OverlayBridge.TARGET_OPEN_QR_SCANNER
}

enum class PttConfirmReleaseAction {
    SendToSubtitle,
    SendToInput,
    Cancel
}

enum class PttConfirmDragTarget {
    DefaultSend,
    ToInput,
    Cancel
}

data class QuickSubtitleGroup(
    val id: Long,
    val title: String,
    val icon: String,
    val items: List<String>
)

enum class QuickCardType(val wireValue: String) {
    Image("image"),
    Qr("qr"),
    Text("text");

    companion object {
        fun fromWire(raw: String?): QuickCardType {
            return entries.firstOrNull { it.wireValue == raw } ?: Text
        }
    }
}

data class QuickCard(
    val id: Long,
    val type: QuickCardType,
    val title: String,
    val note: String = "",
    val themeColor: String = "#038387",
    val link: String = "",
    val portraitImagePath: String = "",
    val landscapeImagePath: String = ""
)

data class QuickCardDraft(
    val editId: Long? = null,
    val isNew: Boolean = false,
    val type: QuickCardType = QuickCardType.Text,
    val title: String = "",
    val note: String = "",
    val themeColor: String = "#038387",
    val link: String = "",
    val portraitImagePath: String = "",
    val landscapeImagePath: String = ""
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

data class DrawingSaveResult(
    val fullPath: String
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
    var pendingQuickSubtitleLaunchRequest by mutableStateOf<ExternalQuickSubtitleRequest?>(null)
        private set

    private var controller: RealtimeController? = null
    private var restartJob: Job? = null
    private var settingsObserveJob: Job? = null
    private val lastProgressUpdateAtMs = mutableMapOf<Long, Long>()
    private var lastLevelUpdateAtMs = 0L
    private var speakerProfiles = mutableListOf<UserPrefs.SpeakerVerifyProfile>()
    private var pttSessionLastText: String = ""
    private var lastPttHistoryTextKey: String = ""
    private var lastPttHistoryAtMs: Long = 0L
    private var manualRecognizedIdSeed: Long = -1L
    private var pttSessionCommitConsumed: Boolean = false
    private var lastHandledQuickSubtitleLaunchRequestId: Long = Long.MIN_VALUE

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
        val overlapMax = kotlin.math.min(a.length, b.length)
        for (k in overlapMax downTo 1) {
            if (a.regionMatches(a.length - k, b, 0, k, ignoreCase = false)) {
                return (a + b.substring(k)).trim()
            }
        }
        // PTT 流式拼接不自动补空格，避免中文结果出现“断词空格”。
        return (a + b).replace(Regex("\\s+"), "").trim()
    }

    private fun appendPttFinalTranscript(text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        val merged = mergePttTranscript(pttSessionLastText, normalized)
        pttSessionLastText = merged
        if (merged != uiState.pushToTalkStreamingText) {
            uiState = uiState.copy(pushToTalkStreamingText = merged)
        }
    }

    private fun updatePttPreviewTranscript(text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        val preview = mergePttTranscript(pttSessionLastText, normalized)
        if (preview != uiState.pushToTalkStreamingText) {
            uiState = uiState.copy(pushToTalkStreamingText = preview)
        }
    }

    private fun normalizePttHistoryKey(text: String): String {
        return text.trim().trimEnd('。', '！', '？', '!', '?', '，', ',', '；', ';', '、', '.')
    }

    private fun shouldSkipPttDuplicateHistory(text: String): Boolean {
        if (!(uiState.pushToTalkMode && uiState.pushToTalkConfirmInputMode)) return false
        val key = normalizePttHistoryKey(text)
        if (key.isEmpty()) return true
        val now = SystemClock.uptimeMillis()
        val duplicated = key == lastPttHistoryTextKey && (now - lastPttHistoryAtMs) <= 1800L
        if (!duplicated) {
            lastPttHistoryTextKey = key
            lastPttHistoryAtMs = now
        }
        return duplicated
    }

    private fun resetPttHistoryDedup() {
        lastPttHistoryTextKey = ""
        lastPttHistoryAtMs = 0L
    }

    private fun appendRecognizedHistory(text: String, id: Long? = null) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        val historyId = id ?: manualRecognizedIdSeed--
        if (id != null && realtimeRecognized.any { it.id == id }) return
        val item = RecognizedItem(id = historyId, text = normalized)
        val next = (listOf(item) + realtimeRecognized).take(MAX_RECOGNIZED_ITEMS)
        realtimeRecognized = next
        val validIds = next.asSequence().map { it.id }.toSet()
        lastProgressUpdateAtMs.keys.retainAll(validIds)
    }

    private companion object {
        private const val LEVEL_UPDATE_INTERVAL_MS = 33L
        private const val LEVEL_UPDATE_DELTA = 0.02f
        private const val PROGRESS_UPDATE_INTERVAL_MS = 48L
        private const val PROGRESS_UPDATE_DELTA = 0.02f
        private const val MAX_RECOGNIZED_ITEMS = 100
        private const val MAX_SPEAKER_PROFILES = 3
        private const val APP_REALTIME_OWNER_TAG = "app"
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
    var quickSubtitleInputCollapsed by mutableStateOf(false)
        private set
    var quickSubtitleBold by mutableStateOf(true)
        private set
    var quickSubtitleCentered by mutableStateOf(false)
        private set
    var quickSubtitleFontSizeSp by mutableFloatStateOf(56f)
        private set
    var quickSubtitlePreviewVisible by mutableStateOf(false)
        private set
    var quickCards by mutableStateOf<List<QuickCard>>(emptyList())
        private set
    var quickCardSelectedIndex by mutableIntStateOf(0)
        private set
    var quickCardPreviewCardId by mutableStateOf<Long?>(null)
        private set
    var quickCardDraft by mutableStateOf<QuickCardDraft?>(null)
        private set
    var drawingToolbarCollapsed by mutableStateOf(false)
        private set
    private var quickSubtitleNextGroupId = 4L
    private var quickSubtitleSaving = false
    private var quickCardsNextId = 1L
    private var quickCardsSaving = false

    init {
        loadQuickSubtitleConfig()
        loadQuickCardConfig()
        observeSettingsChanges()
    }

    private fun applySettingsSnapshot(settings: UserPrefs.AppSettings) {
        speakerProfiles = UserPrefs.parseSpeakerVerifyProfiles(settings.speakerVerifyProfileCsv)
            .take(MAX_SPEAKER_PROFILES)
            .toMutableList()
        val hasProfiles = speakerProfiles.isNotEmpty()
        val speakerVerifyEnabled = settings.speakerVerifyEnabled && hasProfiles
        val nextAec3Status = if (settings.aec3Enabled) {
            if (uiState.aec3Status == "未启用") "待启动" else uiState.aec3Status
        } else {
            "未启用"
        }
        val nextAec3Diag = if (settings.aec3Enabled) {
            if (uiState.aec3Diag == "AEC3 诊断：未启用") "AEC3 诊断：待启动" else uiState.aec3Diag
        } else {
            "AEC3 诊断：未启用"
        }
        uiState = uiState.copy(
            muteWhilePlaying = settings.muteWhilePlaying,
            muteWhilePlayingDelaySec = settings.muteWhilePlayingDelaySec,
            echoSuppression = settings.echoSuppression,
            communicationMode = settings.communicationMode,
            preferredInputType = settings.preferredInputType,
            preferredOutputType = settings.preferredOutputType,
            aec3Enabled = settings.aec3Enabled,
            aec3Status = nextAec3Status,
            aec3Diag = nextAec3Diag,
            minVolumePercent = settings.minVolumePercent,
            playbackGainPercent = settings.playbackGainPercent,
            piperNoiseScale = settings.piperNoiseScale,
            piperLengthScale = settings.piperLengthScale,
            piperNoiseW = 0.8f,
            piperSentenceSilence = settings.piperSentenceSilence,
            keepAlive = settings.keepAlive,
            numberReplaceMode = settings.numberReplaceMode,
            landscapeDrawerMode = settings.landscapeDrawerMode,
            solidTopBar = settings.solidTopBar,
            drawingSaveRelativePath = normalizeDrawingSaveRelativePath(settings.drawingSaveRelativePath),
            quickCardAutoSaveOnExit = settings.quickCardAutoSaveOnExit,
            asrSendToQuickSubtitle = settings.asrSendToQuickSubtitle,
            pushToTalkMode = settings.pushToTalkMode,
            pushToTalkConfirmInputMode = settings.pushToTalkConfirmInput,
            floatingOverlayEnabled = settings.floatingOverlayEnabled,
            floatingOverlayAutoDock = settings.floatingOverlayAutoDock,
            speakerVerifyEnabled = speakerVerifyEnabled,
            speakerVerifyThreshold = settings.speakerVerifyThreshold,
            speakerProfileReady = hasProfiles,
            speakerProfiles = speakerProfiles.map { SpeakerProfileUiItem(id = it.id, name = it.name) },
            speakerLastSimilarity = if (speakerVerifyEnabled) uiState.speakerLastSimilarity else -1f,
            pushToTalkPressed = if (settings.pushToTalkMode) uiState.pushToTalkPressed else false,
            pushToTalkStreamingText = if (settings.pushToTalkMode) uiState.pushToTalkStreamingText else ""
        )
        applySettingsToController(settings)
        if (settings.speakerVerifyEnabled && !speakerVerifyEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                UserPrefs.setSpeakerVerifyEnabled(appContext, false)
            }
        }
    }

    private fun observeSettingsChanges() {
        settingsObserveJob?.cancel()
        settingsObserveJob = viewModelScope.launch {
            UserPrefs.observeSettings(appContext).collectLatest { settings ->
                applySettingsSnapshot(settings)
            }
        }
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
        val fontBold = root.optBoolean("fontBold", true)
        val textCentered = root.optBoolean("textCentered", false)
        quickSubtitleGroups = finalGroups
        quickSubtitleSelectedGroupId =
            finalGroups.firstOrNull { it.id == selectedId }?.id ?: finalGroups.first().id
        quickSubtitleFontSizeSp = fontSize
        quickSubtitleCurrentText = currentText
        quickSubtitleInputText = inputText
        quickSubtitlePlayOnSend = playOnSend
        quickSubtitleBold = fontBold
        quickSubtitleCentered = textCentered
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
            put("fontBold", quickSubtitleBold)
            put("textCentered", quickSubtitleCentered)
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

    fun handleQuickSubtitleLaunchRequest(
        requestId: Long,
        target: String,
        text: String
    ) {
        val normalized = text.trim()
        if (requestId == lastHandledQuickSubtitleLaunchRequestId) return
        if (!isOverlayOpenTarget(target) && normalized.isEmpty()) return
        lastHandledQuickSubtitleLaunchRequestId = requestId
        pendingQuickSubtitleLaunchRequest = ExternalQuickSubtitleRequest(
            requestId = requestId,
            target = target,
            text = normalized
        )
    }

    fun consumeQuickSubtitleLaunchRequest(requestId: Long) {
        if (pendingQuickSubtitleLaunchRequest?.requestId == requestId) {
            pendingQuickSubtitleLaunchRequest = null
        }
    }

    fun applyExternalQuickSubtitleRequest(target: String, text: String) {
        val normalized = text.trim()
        when (target) {
            OverlayBridge.TARGET_OPEN -> {
                loadQuickSubtitleConfig()
            }
            OverlayBridge.TARGET_INPUT -> {
                if (normalized.isEmpty()) return
                appendRecognizedHistory(normalized)
                quickSubtitleInputCollapsed = false
                quickSubtitleInputText = normalized
                saveQuickSubtitleConfig()
            }
            else -> {
                if (normalized.isEmpty()) return
                if (!quickSubtitlePlayOnSend) {
                    appendRecognizedHistory(normalized)
                    applyQuickSubtitleText(normalized, enqueueSpeak = false)
                } else {
                    applyQuickSubtitleText(normalized, enqueueSpeak = false)
                    enqueuePttSpeakAndAppendHistory(normalized)
                }
            }
        }
    }

    fun setQuickSubtitleFontSize(size: Float) {
        quickSubtitleFontSizeSp = size.coerceIn(28f, 96f)
        saveQuickSubtitleConfig()
    }

    fun openQuickSubtitlePreview() {
        quickSubtitlePreviewVisible = true
    }

    fun closeQuickSubtitlePreview() {
        quickSubtitlePreviewVisible = false
    }

    fun updateQuickSubtitlePlayOnSend(enabled: Boolean) {
        quickSubtitlePlayOnSend = enabled
        saveQuickSubtitleConfig()
    }

    fun updateQuickSubtitleInputCollapsed(collapsed: Boolean) {
        quickSubtitleInputCollapsed = collapsed
    }

    fun updateQuickSubtitleBold(enabled: Boolean) {
        quickSubtitleBold = enabled
        saveQuickSubtitleConfig()
    }

    fun updateQuickSubtitleCentered(enabled: Boolean) {
        quickSubtitleCentered = enabled
        saveQuickSubtitleConfig()
    }

    fun clearQuickSubtitleText() {
        quickSubtitleCurrentText = QUICK_SUBTITLE_CLEARED_HINT
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

    fun addQuickSubtitleItem(groupIndex: Int, value: String = "新快捷文本") {
        if (groupIndex !in quickSubtitleGroups.indices) return
        val text = value.trim().ifEmpty { "新快捷文本" }
        val next = quickSubtitleGroups.toMutableList()
        val g = next[groupIndex]
        next[groupIndex] = g.copy(items = g.items + text)
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

    fun setQuickSubtitleItems(groupIndex: Int, items: List<String>) {
        if (groupIndex !in quickSubtitleGroups.indices) return
        val g = quickSubtitleGroups[groupIndex]
        val next = quickSubtitleGroups.toMutableList()
        next[groupIndex] = g.copy(items = items.toList())
        quickSubtitleGroups = next
        saveQuickSubtitleConfig()
    }

    private fun quickCardDir(): File {
        val dir = File(appContext.filesDir, "quick_cards")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun defaultQuickCardDraft(type: QuickCardType, editId: Long, isNew: Boolean, prefillLink: String = ""): QuickCardDraft {
        return QuickCardDraft(
            editId = editId,
            isNew = isNew,
            type = type,
            title = "名片名字",
            note = "愿你的生活充满诗与远方",
            themeColor = "#038387",
            link = prefillLink.trim(),
            portraitImagePath = "",
            landscapeImagePath = ""
        )
    }

    private fun QuickCard.toDraft(isNew: Boolean = false): QuickCardDraft {
        return QuickCardDraft(
            editId = id,
            isNew = isNew,
            type = type,
            title = title,
            note = note,
            themeColor = themeColor,
            link = link,
            portraitImagePath = portraitImagePath,
            landscapeImagePath = landscapeImagePath
        )
    }

    private fun loadQuickCardConfig() {
        viewModelScope.launch {
            val raw = UserPrefs.getQuickCardConfig(appContext)
            if (raw.isNullOrBlank()) return@launch
            runCatching {
                parseQuickCardConfig(raw)
            }
        }
    }

    private fun parseQuickCardConfig(raw: String) {
        val root = JSONObject(raw)
        val cardsArr = root.optJSONArray("cards") ?: JSONArray()
        val parsedCards = mutableListOf<QuickCard>()
        var maxId = 0L
        for (i in 0 until cardsArr.length()) {
            val obj = cardsArr.optJSONObject(i) ?: continue
            val id = obj.optLong("id", i.toLong() + 1L).coerceAtLeast(1L)
            val type = QuickCardType.fromWire(obj.optString("type", QuickCardType.Text.wireValue))
            val title = obj.optString("title", "名片名字")
            val note = obj.optString("note", "")
            val themeColor = obj.optString("themeColor", "#038387")
            val link = obj.optString("link", "")
            val portraitImagePath = obj.optString("portraitImagePath", "")
            val landscapeImagePath = obj.optString("landscapeImagePath", "")
            parsedCards += QuickCard(
                id = id,
                type = type,
                title = title,
                note = note,
                themeColor = normalizeQuickCardColor(themeColor),
                link = link,
                portraitImagePath = portraitImagePath,
                landscapeImagePath = landscapeImagePath
            )
            if (id > maxId) maxId = id
        }
        quickCards = parsedCards
        quickCardsNextId = maxOf(maxId + 1L, 1L)
        quickCardSelectedIndex = root.optInt("selectedIndex", 0).coerceIn(
            0,
            quickCards.lastIndex.coerceAtLeast(0)
        )
        prefetchQuickCardAssets()
    }

    private fun saveQuickCardConfig() {
        if (quickCardsSaving) return
        quickCardsSaving = true
        val root = JSONObject().apply {
            put("selectedIndex", quickCardSelectedIndex)
            val cardsArr = JSONArray()
            quickCards.forEach { c ->
                cardsArr.put(
                    JSONObject().apply {
                        put("id", c.id)
                        put("type", c.type.wireValue)
                        put("title", c.title)
                        put("note", c.note)
                        put("themeColor", c.themeColor)
                        put("link", c.link)
                        put("portraitImagePath", c.portraitImagePath)
                        put("landscapeImagePath", c.landscapeImagePath)
                    }
                )
            }
            put("cards", cardsArr)
        }
        val payload = root.toString()
        viewModelScope.launch {
            try {
                UserPrefs.setQuickCardConfig(appContext, payload)
            } finally {
                quickCardsSaving = false
            }
        }
        prefetchQuickCardAssets()
    }

    fun updateQuickCardSelectedIndex(index: Int) {
        if (quickCards.isEmpty()) {
            quickCardSelectedIndex = 0
            return
        }
        quickCardSelectedIndex = index.coerceIn(0, quickCards.lastIndex)
        saveQuickCardConfig()
        prefetchQuickCardAssets()
    }

    fun reorderQuickCardsByIds(orderedIds: List<Long>) {
        if (quickCards.size <= 1) return
        val byId = quickCards.associateBy { it.id }
        val seen = hashSetOf<Long>()
        val next = mutableListOf<QuickCard>()
        orderedIds.forEach { id ->
            if (seen.add(id)) {
                byId[id]?.let { next += it }
            }
        }
        quickCards.forEach { card ->
            if (seen.add(card.id)) {
                next += card
            }
        }
        if (next == quickCards) return
        val selectedId = quickCards.getOrNull(quickCardSelectedIndex)?.id
        quickCards = next
        quickCardSelectedIndex = selectedId
            ?.let { id -> quickCards.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?: 0
        saveQuickCardConfig()
        prefetchQuickCardAssets()
    }

    fun getQuickCard(id: Long): QuickCard? {
        return quickCards.firstOrNull { it.id == id }
    }

    fun openQuickCardPreview(cardId: Long) {
        quickCardPreviewCardId = cardId
    }

    fun closeQuickCardPreview() {
        quickCardPreviewCardId = null
    }

    fun beginCreateQuickCard(type: QuickCardType, prefillLink: String = "") {
        val id = quickCardsNextId++
        quickCardDraft = defaultQuickCardDraft(type = type, editId = id, isNew = true, prefillLink = prefillLink)
    }

    fun beginEditQuickCard(cardId: Long) {
        val target = getQuickCard(cardId) ?: return
        quickCardDraft = target.toDraft(isNew = false)
    }

    fun clearQuickCardDraft() {
        quickCardDraft = null
    }

    fun updateQuickCardDraft(update: (QuickCardDraft) -> QuickCardDraft) {
        val old = quickCardDraft ?: return
        quickCardDraft = update(old)
    }

    private fun normalizeQuickCardColor(raw: String): String {
        val v = raw.trim()
        return if (Regex("^#[0-9a-fA-F]{6}$").matches(v)) v.lowercase(Locale.US) else "#038387"
    }

    private fun prefetchQuickCardAssets() {
        val cardsSnapshot = quickCards
        if (cardsSnapshot.isEmpty()) return
        val selected = quickCardSelectedIndex.coerceIn(0, cardsSnapshot.lastIndex)
        val candidateIndices = listOf(selected, selected - 1, selected + 1).distinct()
        viewModelScope.launch(Dispatchers.IO) {
            candidateIndices.forEach { index ->
                val card = cardsSnapshot.getOrNull(index) ?: return@forEach
                card.portraitImagePath.takeIf { it.isNotBlank() }?.let { QuickCardRenderCache.loadImage(it) }
                card.landscapeImagePath.takeIf { it.isNotBlank() }?.let { QuickCardRenderCache.loadImage(it) }
                card.link.takeIf { it.isNotBlank() }?.let { QuickCardRenderCache.loadQr(it) }
            }
        }
    }

    private fun normalizeQuickCardType(draft: QuickCardDraft): QuickCardType {
        val hasPortrait = draft.portraitImagePath.isNotBlank()
        val hasLandscape = draft.landscapeImagePath.isNotBlank()
        var type = draft.type
        if (type == QuickCardType.Image && !hasPortrait && !hasLandscape) {
            type = QuickCardType.Qr
        }
        if (type == QuickCardType.Qr && draft.link.trim().isEmpty()) {
            type = QuickCardType.Text
        }
        return type
    }

    private fun normalizedQuickCardFromDraft(draft: QuickCardDraft): QuickCard {
        val id = draft.editId ?: -1L
        val normalized = draft.copy(
            title = draft.title.trim().ifEmpty { "名片名字" },
            note = draft.note.trim(),
            themeColor = normalizeQuickCardColor(draft.themeColor),
            link = draft.link.trim()
        )
        val type = normalizeQuickCardType(normalized)
        return QuickCard(
            id = id,
            type = type,
            title = normalized.title,
            note = normalized.note,
            themeColor = normalized.themeColor,
            link = normalized.link,
            portraitImagePath = normalized.portraitImagePath,
            landscapeImagePath = normalized.landscapeImagePath
        )
    }

    fun hasQuickCardDraftChanges(): Boolean {
        val draft = quickCardDraft ?: return false
        val targetId = draft.editId ?: return true
        val base = quickCards.firstOrNull { it.id == targetId } ?: return true
        return normalizedQuickCardFromDraft(draft) != base
    }

    fun saveQuickCardDraft(): QuickCard? {
        val draft = quickCardDraft ?: return null
        val saved = normalizedQuickCardFromDraft(draft).let { normalized ->
            if (normalized.id > 0L) normalized else normalized.copy(id = quickCardsNextId++)
        }
        val next = quickCards.toMutableList()
        val idx = next.indexOfFirst { it.id == saved.id }
        if (idx >= 0) {
            next[idx] = saved
            quickCardSelectedIndex = idx
        } else {
            next += saved
            quickCardSelectedIndex = next.lastIndex
        }
        quickCards = next
        quickCardDraft = saved.toDraft(isNew = false)
        saveQuickCardConfig()
        return saved
    }

    fun duplicateEditingQuickCard(): QuickCard? {
        val draft = quickCardDraft ?: return null
        val sourceId = draft.editId ?: return null
        val source = quickCards.firstOrNull { it.id == sourceId } ?: return null
        val id = quickCardsNextId++
        val copied = source.copy(
            id = id,
            title = "${source.title} 副本"
        )
        quickCards = quickCards + copied
        quickCardSelectedIndex = quickCards.lastIndex
        quickCardDraft = copied.toDraft(isNew = false)
        saveQuickCardConfig()
        return copied
    }

    fun deleteEditingQuickCard(): Boolean {
        val draft = quickCardDraft ?: return false
        val id = draft.editId ?: return false
        val idx = quickCards.indexOfFirst { it.id == id }
        if (idx < 0) return false
        val next = quickCards.toMutableList()
        next.removeAt(idx)
        quickCards = next
        quickCardSelectedIndex = quickCardSelectedIndex.coerceIn(0, quickCards.lastIndex.coerceAtLeast(0))
        quickCardDraft = null
        saveQuickCardConfig()
        return true
    }

    private fun copyUriToQuickCardImage(uri: android.net.Uri, fileName: String): String? {
        return runCatching {
            val outFile = File(quickCardDir(), fileName)
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            outFile.absolutePath
        }.getOrNull()
    }

    fun setQuickCardDraftImage(uri: android.net.Uri, landscape: Boolean): Boolean {
        val draft = quickCardDraft ?: return false
        val id = draft.editId ?: return false
        val tag = if (landscape) "landscape" else "portrait"
        val fileName = "card_${id}_${tag}_${System.currentTimeMillis()}.png"
        val path = copyUriToQuickCardImage(uri, fileName) ?: return false
        quickCardDraft = if (landscape) {
            draft.copy(landscapeImagePath = path)
        } else {
            draft.copy(portraitImagePath = path)
        }
        return true
    }

    fun clearQuickCardDraftImage(landscape: Boolean) {
        val draft = quickCardDraft ?: return
        quickCardDraft = if (landscape) {
            draft.copy(landscapeImagePath = "")
        } else {
            draft.copy(portraitImagePath = "")
        }
    }

    private fun decodeQrContentFromBitmapInternal(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null
        return runCatching {
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binary = BinaryBitmap(HybridBinarizer(source))
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
            )
            MultiFormatReader().decode(binary, hints)?.text
        }.getOrNull()
    }

    suspend fun decodeQrContentFromBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        decodeQrContentFromBitmapInternal(bitmap)
    }

    suspend fun decodeQrContentFromImage(uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        val bmp = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return@withContext null
        decodeQrContentFromBitmapInternal(bmp)
    }

    private fun ensureController(): RealtimeController {
        controller?.let { return it }
        val created = RealtimeController(
            appContext,
            viewModelScope,
            onResult = { id, text ->
                val normalized = text.trim()
                val isPttConfirmPressed =
                    uiState.pushToTalkMode &&
                    uiState.pushToTalkConfirmInputMode &&
                    uiState.pushToTalkPressed
                // PTT确认模式下彻底解耦：
                // - 流式/最终识别只更新PTT会话文本
                // - 主历史与任务只在松手提交
                if (!isPttConfirmPressed && normalized.isNotEmpty()) {
                    appendRecognizedHistory(normalized, id)
                }
                if (uiState.pushToTalkMode && uiState.pushToTalkConfirmInputMode) {
                    if (uiState.pushToTalkPressed && normalized.isNotEmpty()) {
                        // 合并“流式片段 + 最终片段”，避免覆盖导致丢字。
                        appendPttFinalTranscript(normalized)
                    }
                } else if (uiState.asrSendToQuickSubtitle) {
                    if (normalized.isNotEmpty() && normalized != quickSubtitleCurrentText) {
                        quickSubtitleCurrentText = normalized
                    }
                }
            },
            onStreamingResult = { text ->
                val normalized = text.trim()
                if (normalized.isEmpty()) return@RealtimeController
                if (uiState.pushToTalkMode &&
                    uiState.pushToTalkConfirmInputMode &&
                    uiState.pushToTalkPressed
                ) {
                    // 流式阶段只刷新预览，不直接累加到最终会话文本，
                    // 避免同一片段被反复拼接。
                    updatePttPreviewTranscript(normalized)
                }
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
            onSpeakerVerify = { similarity, passed ->
                uiState = uiState.copy(speakerLastSimilarity = similarity)
                if (!passed && uiState.speakerVerifyEnabled) {
                    val msg = "说话人验证未通过(${String.format("%.2f", similarity)})"
                    if (uiState.status != msg) {
                        uiState = uiState.copy(status = msg)
                    }
                }
            },
            onError = { msg -> uiState = uiState.copy(status = msg, running = false) },
            initialSuppressWhilePlaying = uiState.muteWhilePlaying,
            initialUseVoiceCommunication = uiState.echoSuppression,
            initialCommunicationMode = uiState.communicationMode,
            initialMinVolumePercent = uiState.minVolumePercent,
            initialPlaybackGainPercent = uiState.playbackGainPercent,
            initialPiperNoiseScale = uiState.piperNoiseScale,
            initialPiperLengthScale = uiState.piperLengthScale,
            initialPiperNoiseW = 0.8f,
            initialPiperSentenceSilenceSec = uiState.piperSentenceSilence,
            initialSuppressDelaySec = uiState.muteWhilePlayingDelaySec,
            initialPreferredInputType = uiState.preferredInputType,
            initialPreferredOutputType = uiState.preferredOutputType,
            initialUseAec3 = uiState.aec3Enabled,
            initialNumberReplaceMode = uiState.numberReplaceMode,
            initialAllowSystemAecWithAec3 = true,
            initialSpeakerVerifyEnabled = uiState.speakerVerifyEnabled,
            initialSpeakerVerifyThreshold = uiState.speakerVerifyThreshold,
            initialSpeakerProfiles = speakerProfiles.map { it.vector.copyOf() }
        )
        created.setPushToTalkStreamingEnabled(
            uiState.pushToTalkMode &&
                    uiState.pushToTalkConfirmInputMode &&
                    uiState.pushToTalkPressed
        )
        created.setSuppressAsrAutoSpeak(
            uiState.pushToTalkMode && uiState.pushToTalkConfirmInputMode
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
            applySettingsSnapshot(settings)
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
            if (uiState.floatingOverlayEnabled) {
                FloatingOverlayService.refresh(appContext)
            }
        }
    }

    fun selectVoice(dir: File) {
        viewModelScope.launch {
            UserPrefs.setLastVoiceName(appContext, dir.name)
            uiState = uiState.copy(voiceDir = dir, status = "已选择音色包")
            preloadTts(dir)
            refreshVoicePacks()
            if (uiState.floatingOverlayEnabled) {
                FloatingOverlayService.refresh(appContext)
            }
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
        // Optimistically apply UI order to avoid one-frame fallback to stale state.
        uiState = uiState.copy(voicePacks = newOrder)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                newOrder.forEachIndexed { index, pack ->
                    repo.updateVoiceMeta(pack.dir) { meta ->
                        meta.copy(order = index.toLong())
                    }
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
        val clamped = snapPlaybackGainPercent(percent)
        uiState = uiState.copy(playbackGainPercent = clamped)
        controller?.setPlaybackGainPercent(clamped)
        viewModelScope.launch {
            UserPrefs.setPlaybackGainPercent(appContext, clamped)
        }
    }

    fun setPiperNoiseScale(value: Float) {
        val clamped = value.coerceIn(0f, 2f)
        uiState = uiState.copy(piperNoiseScale = clamped)
        controller?.setPiperNoiseScale(clamped)
        viewModelScope.launch {
            UserPrefs.setPiperNoiseScale(appContext, clamped)
        }
    }

    fun setPiperLengthScale(value: Float) {
        val clamped = value.coerceIn(0.1f, 5f)
        uiState = uiState.copy(piperLengthScale = clamped)
        controller?.setPiperLengthScale(clamped)
        viewModelScope.launch {
            UserPrefs.setPiperLengthScale(appContext, clamped)
        }
    }

    fun setPiperNoiseW(value: Float) {
        val clamped = value.coerceIn(0f, 2f)
        uiState = uiState.copy(piperNoiseW = clamped)
        controller?.setPiperNoiseW(clamped)
        viewModelScope.launch {
            UserPrefs.setPiperNoiseW(appContext, clamped)
        }
    }

    fun setPiperSentenceSilence(value: Float) {
        val clamped = value.coerceIn(0f, 2f)
        uiState = uiState.copy(piperSentenceSilence = clamped)
        controller?.setPiperSentenceSilenceSec(clamped)
        viewModelScope.launch {
            UserPrefs.setPiperSentenceSilence(appContext, clamped)
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

    fun setAsrSendToQuickSubtitle(enabled: Boolean) {
        uiState = uiState.copy(asrSendToQuickSubtitle = enabled)
        viewModelScope.launch {
            UserPrefs.setAsrSendToQuickSubtitle(appContext, enabled)
        }
    }

    fun setPushToTalkMode(enabled: Boolean) {
        pttSessionLastText = ""
        resetPttHistoryDedup()
        uiState = uiState.copy(
            pushToTalkMode = enabled,
            pushToTalkPressed = false,
            pushToTalkStreamingText = ""
        )
        controller?.setPushToTalkStreamingEnabled(false)
        controller?.setSuppressAsrAutoSpeak(enabled && uiState.pushToTalkConfirmInputMode)
        viewModelScope.launch {
            UserPrefs.setPushToTalkMode(appContext, enabled)
        }
    }

    fun setPushToTalkConfirmInputMode(enabled: Boolean) {
        pttSessionLastText = ""
        resetPttHistoryDedup()
        uiState = uiState.copy(
            pushToTalkConfirmInputMode = enabled,
            pushToTalkStreamingText = if (enabled) uiState.pushToTalkStreamingText else ""
        )
        controller?.setPushToTalkStreamingEnabled(
            enabled && uiState.pushToTalkMode && uiState.pushToTalkPressed
        )
        controller?.setSuppressAsrAutoSpeak(enabled && uiState.pushToTalkMode)
        viewModelScope.launch {
            UserPrefs.setPushToTalkConfirmInput(appContext, enabled)
        }
    }

    fun setFloatingOverlayEnabled(enabled: Boolean) {
        uiState = uiState.copy(floatingOverlayEnabled = enabled)
        viewModelScope.launch {
            UserPrefs.setFloatingOverlayEnabled(appContext, enabled)
        }
    }

    fun setFloatingOverlayAutoDock(enabled: Boolean) {
        uiState = uiState.copy(floatingOverlayAutoDock = enabled)
        viewModelScope.launch {
            UserPrefs.setFloatingOverlayAutoDock(appContext, enabled)
        }
    }

    fun setPushToTalkPressed(pressed: Boolean) {
        if (uiState.pushToTalkPressed == pressed) return
        uiState = uiState.copy(
            pushToTalkPressed = pressed,
            pushToTalkStreamingText = if (!pressed) "" else uiState.pushToTalkStreamingText
        )
        controller?.setPushToTalkStreamingEnabled(
            uiState.pushToTalkMode && uiState.pushToTalkConfirmInputMode && pressed
        )
    }

    fun beginPushToTalkSession() {
        if (!uiState.pushToTalkConfirmInputMode) return
        pttSessionLastText = ""
        pttSessionCommitConsumed = false
        resetPttHistoryDedup()
        uiState = uiState.copy(pushToTalkStreamingText = "")
    }

    fun commitPushToTalkSession(action: PttConfirmReleaseAction) {
        if (!uiState.pushToTalkConfirmInputMode) return
        if (pttSessionCommitConsumed) return
        pttSessionCommitConsumed = true
        val text = uiState.pushToTalkStreamingText.trim().ifBlank { pttSessionLastText.trim() }
        when (action) {
            PttConfirmReleaseAction.SendToSubtitle -> {
                if (text.isNotEmpty()) {
                    if (!quickSubtitlePlayOnSend) {
                        appendRecognizedHistory(text)
                        applyQuickSubtitleText(
                            text = text,
                            enqueueSpeak = false
                        )
                    } else {
                        // 朗读开启时，也只在松手提交：
                        // 先上屏，再手动入历史(绑定真实队列ID)，进度条由 onProgress 驱动。
                        applyQuickSubtitleText(
                            text = text,
                            enqueueSpeak = false
                        )
                        enqueuePttSpeakAndAppendHistory(text)
                    }
                }
            }
            PttConfirmReleaseAction.SendToInput -> {
                if (text.isNotEmpty()) {
                    appendRecognizedHistory(text)
                    quickSubtitleInputText = text
                }
            }
            PttConfirmReleaseAction.Cancel -> Unit
        }
        pttSessionLastText = ""
        resetPttHistoryDedup()
        uiState = uiState.copy(pushToTalkStreamingText = "")
    }

    private fun enqueuePttSpeakAndAppendHistory(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return
        val voice = uiState.voiceDir
        if (voice == null) {
            appendRecognizedHistory(message)
            uiState = uiState.copy(status = "请先选择语音包")
            return
        }
        val activeController = ensureController()
        viewModelScope.launch {
            val queuedId = withContext(Dispatchers.IO) {
                if (!activeController.loadTts(voice)) return@withContext null
                activeController.enqueueSpeakText(message)
            }
            if (queuedId != null) {
                appendRecognizedHistory(message, queuedId)
                uiState = uiState.copy(status = "已加入朗读队列")
            } else {
                appendRecognizedHistory(message)
            }
        }
    }

    fun canAddSpeakerProfile(): Boolean {
        return speakerProfiles.size < MAX_SPEAKER_PROFILES
    }

    private fun speakerProfileUiItems(): List<SpeakerProfileUiItem> {
        return speakerProfiles.map { SpeakerProfileUiItem(id = it.id, name = it.name) }
    }

    private fun speakerProfileVectors(): List<FloatArray> {
        return speakerProfiles.map { it.vector.copyOf() }
    }

    fun setSpeakerVerifyEnabled(enabled: Boolean) {
        uiState = uiState.copy(speakerVerifyEnabled = enabled)
        controller?.setSpeakerVerifyEnabled(enabled)
        viewModelScope.launch {
            UserPrefs.setSpeakerVerifyEnabled(appContext, enabled)
        }
        if (enabled && speakerProfiles.isEmpty()) {
            uiState = uiState.copy(status = "说话人验证已开启，请先注册说话人")
        }
    }

    fun setSpeakerVerifyThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0.4f, 0.95f)
        uiState = uiState.copy(speakerVerifyThreshold = clamped)
        controller?.setSpeakerVerifyThreshold(clamped)
        viewModelScope.launch {
            UserPrefs.setSpeakerVerifyThreshold(appContext, clamped)
        }
    }

    fun clearSpeakerProfile() {
        speakerProfiles.clear()
        uiState = uiState.copy(
            speakerVerifyEnabled = false,
            speakerProfileReady = false,
            speakerProfiles = emptyList(),
            speakerLastSimilarity = -1f,
            status = "已清除说话人注册信息"
        )
        controller?.setSpeakerVerifyEnabled(false)
        controller?.clearSpeakerProfiles()
        viewModelScope.launch {
            UserPrefs.setSpeakerVerifyEnabled(appContext, false)
            UserPrefs.setSpeakerVerifyProfiles(appContext, emptyList())
        }
    }

    fun removeSpeakerProfileAt(index: Int) {
        if (index !in speakerProfiles.indices) return
        speakerProfiles = speakerProfiles.toMutableList().apply { removeAt(index) }
        val hasProfiles = speakerProfiles.isNotEmpty()
        val keepVerify = uiState.speakerVerifyEnabled && hasProfiles
        uiState = uiState.copy(
            speakerVerifyEnabled = keepVerify,
            speakerProfileReady = hasProfiles,
            speakerProfiles = speakerProfileUiItems(),
            speakerLastSimilarity = if (hasProfiles) uiState.speakerLastSimilarity else -1f,
            status = if (hasProfiles) "已移除说话人" else "已清除说话人注册信息"
        )
        controller?.setSpeakerVerifyEnabled(keepVerify)
        controller?.setSpeakerProfiles(speakerProfileVectors())
        viewModelScope.launch {
            UserPrefs.setSpeakerVerifyEnabled(appContext, keepVerify)
            UserPrefs.setSpeakerVerifyProfiles(appContext, speakerProfiles)
        }
    }

    fun applySpeakerProfile(profile: FloatArray): Boolean {
        if (speakerProfiles.size >= MAX_SPEAKER_PROFILES) {
            return false
        }
        val nextIndex = speakerProfiles.size + 1
        val item = UserPrefs.SpeakerVerifyProfile(
            id = "spk-${SystemClock.elapsedRealtime()}-$nextIndex",
            name = "说话人 $nextIndex",
            vector = profile.copyOf()
        )
        speakerProfiles = speakerProfiles.toMutableList().apply { add(item) }
        controller?.setSpeakerProfiles(speakerProfileVectors())
        uiState = uiState.copy(
            speakerProfileReady = true,
            speakerProfiles = speakerProfileUiItems(),
            speakerLastSimilarity = 1f,
            status = "说话人注册成功（${speakerProfiles.size}/$MAX_SPEAKER_PROFILES）"
        )
        viewModelScope.launch {
            UserPrefs.setSpeakerVerifyProfiles(appContext, speakerProfiles)
        }
        return true
    }

    suspend fun enrollSpeakerProfileNow(
        durationSec: Float = 4f,
        onCapture: ((progress: Float, level: Float) -> Unit)? = null,
        persist: Boolean = true
    ): SpeakerEnrollResult {
        if (uiState.running) {
            val msg = "请先停止麦克风再注册说话人"
            uiState = uiState.copy(status = msg)
            return SpeakerEnrollResult(success = false, message = msg)
        }
        val activeController = ensureController()
        uiState = uiState.copy(status = "说话人注册中（请持续说话约${durationSec.toInt()}秒）...")
        val result = withContext(Dispatchers.IO) {
            activeController.enrollSpeaker(durationSec) { progress, level ->
                if (onCapture != null) {
                    viewModelScope.launch(Dispatchers.Main) {
                        onCapture(progress, level)
                    }
                }
            }
        }
        if (result.success && result.profile != null) {
            if (persist) {
                val applied = applySpeakerProfile(result.profile)
                if (applied) {
                    uiState = uiState.copy(status = result.message)
                }
            }
        } else {
            uiState = uiState.copy(status = result.message)
        }
        return result
    }

    fun enrollSpeakerProfile() {
        viewModelScope.launch {
            enrollSpeakerProfileNow(4f)
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

    fun setDrawingSaveRelativePath(path: String) {
        val normalized = normalizeDrawingSaveRelativePath(path)
        uiState = uiState.copy(
            drawingSaveRelativePath = normalized,
            status = "画板保存路径：$normalized"
        )
        viewModelScope.launch {
            UserPrefs.setDrawingSaveRelativePath(appContext, normalized)
        }
    }

    fun setQuickCardAutoSaveOnExit(enabled: Boolean) {
        uiState = uiState.copy(quickCardAutoSaveOnExit = enabled)
        viewModelScope.launch {
            UserPrefs.setQuickCardAutoSaveOnExit(appContext, enabled)
        }
    }

    fun setDrawingSavePathFromTreeUri(uri: android.net.Uri) {
        val resolved = drawingRelativePathFromTreeUri(uri)
        if (resolved == null) {
            uiState = uiState.copy(status = "不支持该目录，请选择内部存储目录")
            return
        }
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(uri, flags)
        }
        setDrawingSaveRelativePath(resolved)
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

    fun updateDrawingToolbarCollapsed(collapsed: Boolean) {
        drawingToolbarCollapsed = collapsed
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

    fun saveDrawingSnapshot() {
        val strokes = drawStrokes.toList()
        if (strokes.isEmpty()) {
            uiState = uiState.copy(status = "画板为空，无可保存内容")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val width = 1080
                val height = 1920
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

                strokes.forEach { stroke ->
                    paint.color = if (stroke.eraser) android.graphics.Color.WHITE else stroke.color.toArgb()
                    paint.strokeWidth = stroke.width
                    val pts = stroke.points
                    for (i in 1 until pts.size) {
                        val p0 = pts[i - 1]
                        val p1 = pts[i]
                        canvas.drawLine(
                            p0.x * width,
                            p0.y * height,
                            p1.x * width,
                            p1.y * height,
                            paint
                        )
                    }
                }

                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "drawing_$ts.png"
                val relativePath = normalizeDrawingSaveRelativePath(uiState.drawingSaveRelativePath)
                val resolver = appContext.contentResolver
                val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(collection, values)
                    ?: error("无法创建图片媒体条目")
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        if (!ok) error("图片编码失败")
                    } ?: error("无法打开图片输出流")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        resolver.update(
                            uri,
                            ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            },
                            null,
                            null
                        )
                    }
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                } finally {
                    bitmap.recycle()
                }

                val fullPath = "/storage/emulated/0/${relativePath.trim('/')}/$fileName"
                runCatching {
                    MediaScannerConnection.scanFile(
                        appContext,
                        arrayOf(fullPath),
                        arrayOf("image/png"),
                        null
                    )
                    appContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
                }
                DrawingSaveResult(fullPath = fullPath)
            }

            result.onSuccess { saved ->
                AppLogger.i("drawing saved: ${saved.fullPath}")
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(status = "画板已保存：${saved.fullPath}")
                    Toast.makeText(appContext, "画板已保存：${saved.fullPath}", Toast.LENGTH_LONG).show()
                }
            }.onFailure { e ->
                AppLogger.e("drawing save failed", e)
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(status = "画板保存失败：${e.message ?: "未知错误"}")
                    Toast.makeText(appContext, "画板保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
            val queuedId = withContext(Dispatchers.IO) {
                if (!activeController.loadTts(voice)) return@withContext null
                activeController.enqueueSpeakText(message)
            }
            if (queuedId != null) {
                // 便捷字幕的快速文本/输入框触发朗读时，也要进入历史记录。
                // 使用队列ID绑定，避免与 onResult 回调重复插入。
                appendRecognizedHistory(message, queuedId)
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
        if (!RealtimeOwnerGate.acquire(APP_REALTIME_OWNER_TAG)) {
            uiState = uiState.copy(status = "麦克风已被悬浮窗占用")
            return
        }
        restartJob?.cancel()
        restartJob = null
        val activeController = ensureController()
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
                RealtimeOwnerGate.release(APP_REALTIME_OWNER_TAG)
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
        pttSessionLastText = ""
        resetPttHistoryDedup()
        val activeController = controller ?: run {
            RealtimeOwnerGate.release(APP_REALTIME_OWNER_TAG)
            uiState = uiState.copy(
                running = false,
                status = "麦克风已停止",
                pushToTalkPressed = false,
                pushToTalkStreamingText = ""
            )
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                activeController.stopMic()
            }
            RealtimeOwnerGate.release(APP_REALTIME_OWNER_TAG)
            KeepAliveService.stop(appContext)
            realtimeInputLevel = 0f
            realtimePlaybackProgress = 0f
            uiState = uiState.copy(
                running = false,
                status = "麦克风已停止",
                pushToTalkPressed = false,
                pushToTalkStreamingText = ""
            )
        }
    }

    override fun onCleared() {
        val activeController = controller
        controller = null
        settingsObserveJob?.cancel()
        settingsObserveJob = null
        if (activeController != null) {
            runCatching {
                kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                    activeController.stop()
                }
            }
        }
        RealtimeOwnerGate.release(APP_REALTIME_OWNER_TAG)
        super.onCleared()
    }

    private fun applySettingsToController(settings: UserPrefs.AppSettings) {
        controller?.setSuppressWhilePlaying(settings.muteWhilePlaying)
        controller?.setSuppressDelaySec(settings.muteWhilePlayingDelaySec)
        controller?.setMinVolumePercent(settings.minVolumePercent)
        controller?.setPlaybackGainPercent(settings.playbackGainPercent)
        controller?.setPiperNoiseScale(settings.piperNoiseScale)
        controller?.setPiperLengthScale(settings.piperLengthScale)
        controller?.setPiperNoiseW(0.8f)
        controller?.setPiperSentenceSilenceSec(settings.piperSentenceSilence)
        controller?.setUseAec3(settings.aec3Enabled)
        controller?.setUseVoiceCommunication(settings.echoSuppression)
        controller?.setCommunicationMode(settings.communicationMode)
        controller?.setPreferredInputType(settings.preferredInputType)
        controller?.setPreferredOutputType(settings.preferredOutputType)
        controller?.setNumberReplaceMode(settings.numberReplaceMode)
        controller?.setAllowSystemAecWithAec3(true)
        controller?.setSpeakerVerifyEnabled(uiState.speakerVerifyEnabled)
        controller?.setSpeakerVerifyThreshold(settings.speakerVerifyThreshold)
        controller?.setSpeakerProfiles(speakerProfileVectors())
        controller?.setSuppressAsrAutoSpeak(
            uiState.pushToTalkMode && uiState.pushToTalkConfirmInputMode
        )
        controller?.setPushToTalkStreamingEnabled(
            uiState.pushToTalkMode &&
                    uiState.pushToTalkConfirmInputMode &&
                    uiState.pushToTalkPressed
        )
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
private fun QuickCardNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    onNavReady: () -> Unit,
    onTopBarActionsChange: (QuickCardTopBarActions?) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = QuickCardRoutes.Main,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            if (initialState.destination.route == QuickCardRoutes.Main &&
                targetState.destination.route == QuickCardRoutes.Editor
            ) {
                fadeIn(animationSpec = tween(170)) +
                        slideInHorizontally(
                            initialOffsetX = { full -> full / 10 },
                            animationSpec = tween(170, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeIn(animationSpec = tween(120))
            }
        },
        exitTransition = {
            if (initialState.destination.route == QuickCardRoutes.Main &&
                targetState.destination.route == QuickCardRoutes.Editor
            ) {
                fadeOut(animationSpec = tween(120)) +
                        slideOutHorizontally(
                            targetOffsetX = { full -> -full / 12 },
                            animationSpec = tween(120, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeOut(animationSpec = tween(90))
            }
        },
        popEnterTransition = {
            if (initialState.destination.route == QuickCardRoutes.Editor &&
                targetState.destination.route == QuickCardRoutes.Main
            ) {
                fadeIn(animationSpec = tween(150)) +
                        slideInHorizontally(
                            initialOffsetX = { full -> -full / 12 },
                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeIn(animationSpec = tween(120))
            }
        },
        popExitTransition = {
            if (initialState.destination.route == QuickCardRoutes.Editor &&
                targetState.destination.route == QuickCardRoutes.Main
            ) {
                fadeOut(animationSpec = tween(120)) +
                        slideOutHorizontally(
                            targetOffsetX = { full -> full / 14 },
                            animationSpec = tween(120, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeOut(animationSpec = tween(90))
            }
        }
    ) {
        composable(QuickCardRoutes.Main) {
            QuickCardMainScreen(
                viewModel = viewModel,
                onTopBarActionsChange = onTopBarActionsChange,
                onOpenEditor = { cardId ->
                    viewModel.beginEditQuickCard(cardId)
                    navController.navigate(QuickCardRoutes.Editor) { launchSingleTop = true }
                },
                onOpenSort = {
                    navController.navigate(QuickCardRoutes.Sort) { launchSingleTop = true }
                },
                onCreateCard = { type, link ->
                    viewModel.beginCreateQuickCard(type, prefillLink = link)
                    navController.navigate(QuickCardRoutes.Editor) { launchSingleTop = true }
                },
                onOpenScanner = {
                    navController.navigate(QuickCardRoutes.Scanner) { launchSingleTop = true }
                }
            )
        }
        composable(QuickCardRoutes.Sort) {
            QuickCardSortScreen(
                viewModel = viewModel,
                onTopBarActionsChange = onTopBarActionsChange,
                onDone = {
                    navController.popBackStack(QuickCardRoutes.Main, inclusive = false)
                }
            )
        }
        composable(QuickCardRoutes.Editor) {
            QuickCardEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onTopBarActionsChange = onTopBarActionsChange
            )
        }
        composable(QuickCardRoutes.Scanner) {
            QuickCardScannerScreen(
                onTopBarActionsChange = onTopBarActionsChange,
                onOpenFailed = { navController.popBackStack() },
                onResult = { decoded ->
                    val url = normalizeQrTextToWebUrl(decoded)
                    if (url.isNullOrEmpty()) {
                        navController.navigate(QuickCardRoutes.scanText(decoded)) {
                            popUpTo(QuickCardRoutes.Scanner) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(QuickCardRoutes.web(url)) {
                            popUpTo(QuickCardRoutes.Scanner) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable(
            route = QuickCardRoutes.ScanText,
            arguments = listOf(navArgument("text") { type = NavType.StringType })
        ) { entry ->
            QuickCardScanTextScreen(
                text = Uri.decode(entry.arguments?.getString("text").orEmpty()),
                onTopBarActionsChange = onTopBarActionsChange
            )
        }
        composable(
            route = QuickCardRoutes.Web,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { entry ->
            QuickCardWebViewScreen(
                url = Uri.decode(entry.arguments?.getString("url").orEmpty()),
                onTopBarActionsChange = onTopBarActionsChange
            )
        }
    }
    LaunchedEffect(navController) {
        onNavReady()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun QuickCardMainScreen(
    viewModel: MainViewModel,
    onTopBarActionsChange: (QuickCardTopBarActions?) -> Unit,
    onOpenEditor: (Long) -> Unit,
    onOpenSort: () -> Unit,
    onCreateCard: (QuickCardType, String) -> Unit,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    val cards = viewModel.quickCards
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showCreateDialog by remember { mutableStateOf(false) }
    val previewCardId = viewModel.quickCardPreviewCardId
    val previewCard = remember(cards, previewCardId) {
        previewCardId?.let { id -> cards.firstOrNull { it.id == id } }
    }
    val closePreview: () -> Unit = { viewModel.closeQuickCardPreview() }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onOpenScanner()
        } else {
            toast(context, "未授予相机权限")
        }
    }

    val topActions = remember(cameraPermissionLauncher, context) {
        QuickCardTopBarActions(
            onNew = { showCreateDialog = true },
            onScan = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                    onOpenScanner()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )
    }
    SideEffect {
        onTopBarActionsChange(topActions)
    }
    DisposableEffect(Unit) {
        onDispose { onTopBarActionsChange(null) }
    }

    val pageCount = (cards.size + 1).coerceAtLeast(1) // always keep a trailing "new card" page
    val selectedPage = if (cards.isEmpty()) 0 else viewModel.quickCardSelectedIndex.coerceIn(0, cards.lastIndex)
    var pagerPageIndex by rememberSaveable { mutableIntStateOf(selectedPage) }
    LaunchedEffect(pageCount, selectedPage) {
        val maxPage = (pageCount - 1).coerceAtLeast(0)
        if (pagerPageIndex > maxPage) {
            pagerPageIndex = maxPage
        } else if (pagerPageIndex < cards.size && pagerPageIndex != selectedPage) {
            // sync real card pages with ViewModel selection; keep trailing placeholder page as-is.
            pagerPageIndex = selectedPage
        }
    }
    val topMargin = UiTokens.PageTopBlank
    val bottomMargin = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(topMargin))
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    key("quick_card_pager_landscape") {
                        QuickCardPagerView(
                            cards = cards,
                            currentIndex = pagerPageIndex,
                            landscape = true,
                            modifier = Modifier.fillMaxSize(),
                            onPageChanged = { page ->
                                val safePage = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                                pagerPageIndex = safePage
                                if (cards.isNotEmpty() && safePage < cards.size) {
                                    viewModel.updateQuickCardSelectedIndex(safePage.coerceIn(0, cards.lastIndex))
                                }
                            },
                            onCardClick = { card ->
                                if (card == null) {
                                    showCreateDialog = true
                                } else {
                                    viewModel.openQuickCardPreview(card.id)
                                }
                            },
                            onCardLongPress = { card ->
                                if (card != null) {
                                    onOpenSort()
                                }
                            },
                            onEdit = { card ->
                                onOpenEditor(card.id)
                            },
                            onShare = { target ->
                                shareQuickCard(context, target, true)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    QuickCardIndicatorRail(
                        count = pageCount,
                        current = pagerPageIndex,
                        vertical = false
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    key("quick_card_pager_portrait") {
                        QuickCardPagerView(
                            cards = cards,
                            currentIndex = pagerPageIndex,
                            landscape = false,
                            modifier = Modifier
                                .fillMaxSize(),
                            onPageChanged = { page ->
                                val safePage = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                                pagerPageIndex = safePage
                                if (cards.isNotEmpty() && safePage < cards.size) {
                                    viewModel.updateQuickCardSelectedIndex(safePage.coerceIn(0, cards.lastIndex))
                                }
                            },
                            onCardClick = { card ->
                                if (card == null) {
                                    showCreateDialog = true
                                } else {
                                    viewModel.openQuickCardPreview(card.id)
                                }
                            },
                            onCardLongPress = { card ->
                                if (card != null) {
                                    onOpenSort()
                                }
                            },
                            onEdit = { card ->
                                onOpenEditor(card.id)
                            },
                            onShare = { target ->
                                shareQuickCard(context, target, false)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    QuickCardIndicatorRail(
                        count = pageCount,
                        current = pagerPageIndex,
                        vertical = false
                    )
                }
            }
            Spacer(Modifier.height(bottomMargin))
        }

    }

    if (previewCardId != null) {
        Dialog(
            onDismissRequest = closePreview,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { closePreview() },
                contentAlignment = Alignment.Center
            ) {
                val dialogCardAspect = if (isLandscape) QUICK_CARD_ASPECT_LANDSCAPE else QUICK_CARD_ASPECT_PORTRAIT
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                val maxCardWidth = if (isLandscape) {
                    maxWidth * QUICK_CARD_LANDSCAPE_CARD_WIDTH_FRACTION
                } else {
                    maxWidth
                }
                val maxCardHeight = maxHeight
                val widthByHeight = maxCardHeight * dialogCardAspect
                val finalWidth = minOf(maxCardWidth, widthByHeight)
                val finalHeight = finalWidth / dialogCardAspect

                    if (previewCard != null) {
                        QuickCardPreviewCard(
                            card = previewCard,
                            landscape = isLandscape,
                            modifier = if (isLandscape) {
                                Modifier.size(width = finalWidth, height = finalHeight)
                            } else {
                                Modifier.width(finalWidth)
                            },
                            onClick = {},
                            onLongClick = {},
                            onEdit = { card ->
                                closePreview()
                                onOpenEditor(card.id)
                            },
                            onShare = { target ->
                                shareQuickCard(context, target, isLandscape)
                            }
                        )
                    } else {
                        Card(
                            modifier = if (isLandscape) {
                                Modifier.size(width = finalWidth, height = finalHeight)
                            } else {
                                Modifier.width(finalWidth)
                            },
                            shape = RoundedCornerShape(UiTokens.Radius),
                            backgroundColor = md2CardContainerColor(),
                            elevation = UiTokens.CardElevation
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建名片") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("选择名片类型")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Md2OutlinedButton(
                            onClick = {
                                showCreateDialog = false
                                onCreateCard(QuickCardType.Image, "")
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("图片名片") }
                        Md2OutlinedButton(
                            onClick = {
                                showCreateDialog = false
                                onCreateCard(QuickCardType.Qr, "")
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("二维码名片") }
                    }
                    Md2OutlinedButton(
                        onClick = {
                            showCreateDialog = false
                            onCreateCard(QuickCardType.Text, "")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("文字名片") }
                }
            },
            confirmButton = {},
            dismissButton = {
                Md2TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun QuickCardSortScreen(
    viewModel: MainViewModel,
    onTopBarActionsChange: (QuickCardTopBarActions?) -> Unit,
    onDone: () -> Unit
) {
    val cards = viewModel.quickCards
    val topBlank = UiTokens.PageTopBlank
    val bottomBlank = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

    SideEffect {
        onTopBarActionsChange(
            QuickCardTopBarActions(
                onConfirm = onDone,
                canConfirm = true
            )
        )
    }
    DisposableEffect(Unit) {
        onDispose { onTopBarActionsChange(null) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(topBlank))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = UiTokens.CardElevation
        ) {
            Text(
                text = "拖动右侧排序按钮调整名片顺序",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        QuickCardSortRecyclerList(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            cards = cards,
            topBlankHeight = 2.dp,
            bottomBlankHeight = 2.dp,
            onReorder = { ids ->
                viewModel.reorderQuickCardsByIds(ids)
            }
        )
        Spacer(Modifier.height(bottomBlank))
    }
}

@Composable
private fun QuickCardSortRecyclerList(
    modifier: Modifier = Modifier,
    cards: List<QuickCard>,
    topBlankHeight: Dp,
    bottomBlankHeight: Dp,
    onReorder: (List<Long>) -> Unit
) {
    val parentComposition = rememberCompositionContext()
    val density = LocalDensity.current
    val topBlankPx = with(density) { topBlankHeight.roundToPx() }
    val bottomBlankPx = with(density) { bottomBlankHeight.roundToPx() }
    val onReorderState = rememberUpdatedState(onReorder)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val recycler = RecyclerView(ctx).apply {
                layoutManager = LinearLayoutManager(ctx)
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                clipToPadding = false
                clipChildren = false
                itemAnimator = DefaultItemAnimator().apply {
                    supportsChangeAnimations = false
                    addDuration = 120L
                    removeDuration = 120L
                    moveDuration = 160L
                    changeDuration = 0L
                }
                setPadding(paddingLeft, topBlankPx, paddingRight, bottomBlankPx)
            }

            val adapter = QuickCardSortRecyclerAdapter(parentComposition = parentComposition)
            recycler.adapter = adapter

            val touchCallback = object : ItemTouchHelper.Callback() {
                private var moved = false

                override fun isLongPressDragEnabled(): Boolean = false
                override fun isItemViewSwipeEnabled(): Boolean = false

                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    return makeMovementFlags(dragFlags, 0)
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    val ok = adapter.move(from, to)
                    moved = moved || ok
                    return ok
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                        adapter.setDraggingPosition(viewHolder.bindingAdapterPosition)
                    } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        adapter.clearDraggingItem()
                    }
                    adapter.isDragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    adapter.isDragging = false
                    adapter.clearDraggingItem()
                    if (moved) {
                        onReorderState.value(adapter.snapshotIds())
                        moved = false
                    }
                }
            }
            val touchHelper = ItemTouchHelper(touchCallback)
            touchHelper.attachToRecyclerView(recycler)
            adapter.onStartDrag = { vh -> touchHelper.startDrag(vh) }
            recycler
        },
        update = { recycler ->
            val adapter = recycler.adapter as? QuickCardSortRecyclerAdapter ?: return@AndroidView
            recycler.setPadding(recycler.paddingLeft, topBlankPx, recycler.paddingRight, bottomBlankPx)
            recycler.post {
                adapter.submitFromState(cards)
            }
        }
    )
}

private class QuickCardSortRecyclerAdapter(
    private val parentComposition: CompositionContext
) : RecyclerView.Adapter<QuickCardSortRecyclerAdapter.ItemViewHolder>() {
    private val items = mutableListOf<QuickCard>()
    var isDragging: Boolean = false
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
    private var draggingItemId: Long? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = items[position].id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setParentCompositionContext(parentComposition)
        }
        return ItemViewHolder(composeView)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val card = items[position]
        holder.bind(
            card = card,
            isDragged = draggingItemId == card.id,
            onStartDrag = {
                if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onStartDrag?.invoke(holder)
                }
            }
        )
    }

    fun submitFromState(newItems: List<QuickCard>) {
        if (isDragging) return
        if (items == newItems) return
        val oldItems = items.toList()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition].id == newItems[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition] == newItems[newItemPosition]
            }
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    fun move(from: Int, to: Int): Boolean {
        if (from == to || from !in items.indices || to !in items.indices) return false
        val moved = items.removeAt(from)
        items.add(to, moved)
        notifyItemMoved(from, to)
        return true
    }

    fun snapshotIds(): List<Long> = items.map { it.id }

    fun setDraggingPosition(position: Int) {
        val targetId = items.getOrNull(position)?.id
        if (draggingItemId == targetId) return
        val oldId = draggingItemId
        draggingItemId = targetId
        oldId?.let { id ->
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) notifyItemChanged(idx)
        }
        targetId?.let { id ->
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) notifyItemChanged(idx)
        }
    }

    fun clearDraggingItem() {
        val oldId = draggingItemId ?: return
        draggingItemId = null
        val idx = items.indexOfFirst { it.id == oldId }
        if (idx >= 0) notifyItemChanged(idx)
    }

    class ItemViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(
            card: QuickCard,
            isDragged: Boolean,
            onStartDrag: () -> Unit
        ) {
            composeView.setContent {
                QuickCardSortRow(
                    card = card,
                    isDragged = isDragged,
                    onStartDrag = onStartDrag
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun QuickCardSortRow(
    card: QuickCard,
    isDragged: Boolean,
    onStartDrag: () -> Unit
) {
    val rowElevation by animateDpAsState(
        targetValue = if (isDragged) 10.dp else UiTokens.CardElevation,
        animationSpec = tween(
            durationMillis = if (isDragged) 120 else 160,
            easing = FastOutSlowInEasing
        ),
        label = "quick_card_sort_item_elevation"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        shape = RoundedCornerShape(UiTokens.Radius),
        backgroundColor = md2CardContainerColor(),
        elevation = rowElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title.ifBlank { "名片名字" },
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (card.type) {
                        QuickCardType.Image -> "图片名片"
                        QuickCardType.Qr -> "二维码名片"
                        QuickCardType.Text -> "文字名片"
                    },
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Md2IconButton(
                icon = "drag_indicator",
                contentDescription = "拖动排序",
                onClick = {},
                modifier = Modifier.pointerInteropFilter { ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            onStartDrag()
                            true
                        }
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> true
                        else -> false
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickCardScannerScreen(
    onTopBarActionsChange: (QuickCardTopBarActions?) -> Unit,
    onOpenFailed: () -> Unit,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanned = remember { AtomicBoolean(false) }
    var cameraReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onTopBarActionsChange(null)
    }

    DisposableEffect(previewView, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = runCatching { providerFuture.get() }.getOrNull()
            if (provider == null) {
                toast(context, "相机初始化失败")
                onOpenFailed()
                return@Runnable
            }
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            val reader = MultiFormatReader().apply {
                setHints(
                    mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                        DecodeHintType.TRY_HARDER to true
                    )
                )
            }

            analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                try {
                    if (!scanned.get()) {
                        val text = decodeQrFromImageProxy(imageProxy, reader)?.trim().orEmpty()
                        if (text.isNotEmpty() && scanned.compareAndSet(false, true)) {
                            mainExecutor.execute { onResult(text) }
                        }
                    }
                } finally {
                    imageProxy.close()
                }
            }

            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                cameraReady = true
            }.onFailure {
                AppLogger.e("quickCard scanner bind failed", it)
                toast(context, "无法打开相机")
                onOpenFailed()
            }
        }
        providerFuture.addListener(listener, mainExecutor)
        onDispose {
            runCatching {
                if (providerFuture.isDone) {
                    providerFuture.get().unbindAll()
                }
            }
            analyzerExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (cameraReady) "将二维码置于取景框内自动识别" else "正在打开相机...",
                color = Color.White,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
        ) {
            QrScannerFinderFrame(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            )
        }
    }
}

@Composable
private fun QrScannerFinderFrame(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerRatio = 0.22f
        val cornerW = w * cornerRatio
        val cornerH = h * cornerRatio
        val thick = size.minDimension * 0.018f
        val thin = thick * 0.42f

        fun hSeg(x1: Float, x2: Float, y: Float, stroke: Float) {
            drawLine(
                color = color,
                start = Offset(x1, y),
                end = Offset(x2, y),
                strokeWidth = stroke,
                cap = StrokeCap.Butt
            )
        }

        fun vSeg(x: Float, y1: Float, y2: Float, stroke: Float) {
            drawLine(
                color = color,
                start = Offset(x, y1),
                end = Offset(x, y2),
                strokeWidth = stroke,
                cap = StrokeCap.Butt
            )
        }

        hSeg(0f, cornerW, 0f, thick)
        hSeg(cornerW, w - cornerW, 0f, thin)
        hSeg(w - cornerW, w, 0f, thick)

        hSeg(0f, cornerW, h, thick)
        hSeg(cornerW, w - cornerW, h, thin)
        hSeg(w - cornerW, w, h, thick)

        vSeg(0f, 0f, cornerH, thick)
        vSeg(0f, cornerH, h - cornerH, thin)
        vSeg(0f, h - cornerH, h, thick)

        vSeg(w, 0f, cornerH, thick)
        vSeg(w, cornerH, h - cornerH, thin)
        vSeg(w, h - cornerH, h, thick)
    }
}

@Composable
private fun QuickCardScanTextScreen(
    text: String,
    onTopBarActionsChange: (QuickCardTopBarActions?) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val content = remember(text) { text.ifBlank { "(空内容)" } }

    LaunchedEffect(Unit) {
        onTopBarActionsChange(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SelectionContainer {
                Text(
                    text = content,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Md2Button(onClick = {
                    clipboard.setText(AnnotatedString(content))
                    toast(context, "已复制")
                }) {
                    Text("复制")
                }
                Md2OutlinedButton(onClick = {
                    sharePlainText(context, content, "分享二维码结果")
                }) {
                    Text("分享")
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun QuickCardWebViewScreen(
    url: String,
    onTopBarActionsChange: (QuickCardTopBarActions?) -> Unit
) {
    var loading by remember(url) { mutableStateOf(true) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(Unit) {
        onTopBarActionsChange(null)
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef.value?.let { webView ->
                runCatching {
                    webView.stopLoading()
                    webView.destroy()
                }
            }
            webViewRef.value = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewRef.value = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean = false

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (!url.equals(webView.url.orEmpty(), ignoreCase = true)) {
                    loading = true
                    webView.loadUrl(url)
                }
            }
        )
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(animationSpec = tween(90)),
            exit = fadeOut(animationSpec = tween(120)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun decodeQrFromImageProxy(
    imageProxy: ImageProxy,
    reader: MultiFormatReader
): String? {
    val width = imageProxy.width
    val height = imageProxy.height
    if (width <= 0 || height <= 0) return null

    val yPlane = imageProxy.planes.firstOrNull() ?: return null
    val rowStride = yPlane.rowStride
    val pixelStride = yPlane.pixelStride
    val yBuffer = yPlane.buffer
    yBuffer.rewind()

    val luminance = ByteArray(width * height)
    if (pixelStride == 1 && rowStride == width) {
        if (yBuffer.remaining() < luminance.size) return null
        yBuffer.get(luminance)
    } else {
        val rowBytes = ByteArray(rowStride)
        var dstOffset = 0
        for (row in 0 until height) {
            val toRead = minOf(rowStride, yBuffer.remaining())
            if (toRead <= 0) break
            yBuffer.get(rowBytes, 0, toRead)
            var col = 0
            var src = 0
            while (col < width && src < toRead) {
                luminance[dstOffset + col] = rowBytes[src]
                col++
                src += pixelStride
            }
            dstOffset += width
        }
    }

    val source = PlanarYUVLuminanceSource(
        luminance,
        width,
        height,
        0,
        0,
        width,
        height,
        false
    )
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    return runCatching { reader.decodeWithState(bitmap)?.text }
        .getOrNull()
        .also { reader.reset() }
}

@Composable
private fun QuickCardPagerView(
    cards: List<QuickCard>,
    currentIndex: Int,
    landscape: Boolean,
    modifier: Modifier = Modifier,
    onPageChanged: (Int) -> Unit,
    onCardClick: (QuickCard?) -> Unit,
    onCardLongPress: (QuickCard?) -> Unit,
    onEdit: (QuickCard) -> Unit,
    onShare: (QuickCard) -> Unit
) {
    val onPageChangedState by rememberUpdatedState(onPageChanged)
    val onCardClickState by rememberUpdatedState(onCardClick)
    val onCardLongPressState by rememberUpdatedState(onCardLongPress)
    val onEditState by rememberUpdatedState(onEdit)
    val onShareState by rememberUpdatedState(onShare)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val density = context.resources.displayMetrics.density
            val edgeGuardPx = (density * if (landscape) 56f else 6f).toInt()
            val pageMarginPx = (density * if (landscape) 2f else 0f).toInt()
            ViewPager2(context).apply {
                orientation = ViewPager2.ORIENTATION_HORIZONTAL
                offscreenPageLimit = if (landscape) 3 else 2
                clipToPadding = false
                clipChildren = false
                setPadding(edgeGuardPx, 0, edgeGuardPx, 0)
                setPageTransformer(MarginPageTransformer(pageMarginPx))
                (getChildAt(0) as? RecyclerView)?.apply {
                    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                    clipToPadding = false
                    clipChildren = false
                    itemAnimator = null
                    setHasFixedSize(true)
                }
                var userGestureInProgress = false
                val callback = object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrollStateChanged(state: Int) {
                        when (state) {
                            ViewPager2.SCROLL_STATE_DRAGGING -> userGestureInProgress = true
                            ViewPager2.SCROLL_STATE_IDLE -> userGestureInProgress = false
                        }
                    }

                    override fun onPageSelected(position: Int) {
                        // Ignore programmatic page changes caused by data updates/reorder.
                        // Only treat user-driven drags as page-selection input.
                        if (userGestureInProgress) {
                            onPageChangedState(position)
                        }
                    }
                }
                registerOnPageChangeCallback(callback)
                tag = callback
                adapter = QuickCardPagerAdapter()
            }
        },
        update = { pager ->
            val adapter = (pager.adapter as? QuickCardPagerAdapter) ?: return@AndroidView
            val density = pager.context.resources.displayMetrics.density
            val edgeGuardPx = (density * if (landscape) 56f else 6f).toInt()
            val pageMarginPx = (density * if (landscape) 2f else 0f).toInt()
            pager.offscreenPageLimit = if (landscape) 3 else 2
            pager.setPadding(edgeGuardPx, 0, edgeGuardPx, 0)
            pager.setPageTransformer(MarginPageTransformer(pageMarginPx))
            adapter.landscape = landscape
            adapter.onCardClick = onCardClickState
            adapter.onCardLongPress = onCardLongPressState
            adapter.onEdit = onEditState
            adapter.onShare = onShareState
            adapter.submitCards(cards)
            val target = currentIndex.coerceIn(0, (adapter.itemCount - 1).coerceAtLeast(0))
            if (pager.currentItem != target) {
                pager.setCurrentItem(target, false)
            }
        }
    )
}

private class QuickCardPagerAdapter : RecyclerView.Adapter<QuickCardPagerAdapter.QuickCardPageViewHolder>() {
    private var items: List<QuickCard?> = listOf(null)
    var landscape: Boolean = false
    var onCardClick: (QuickCard?) -> Unit = {}
    var onCardLongPress: (QuickCard?) -> Unit = {}
    var onEdit: (QuickCard) -> Unit = {}
    var onShare: (QuickCard) -> Unit = {}

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return items[position]?.id ?: Long.MIN_VALUE
    }

    fun submitCards(cards: List<QuickCard>) {
        // Always keep trailing placeholder page for creating a new card.
        val next: List<QuickCard?> = cards + listOf(null)
        if (items == next) return
        items = next
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickCardPageViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        return QuickCardPageViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: QuickCardPageViewHolder, position: Int) {
        val card = items[position]
        val isLandscape = landscape
        val click = onCardClick
        val longPress = onCardLongPress
        val edit = onEdit
        val share = onShare
        holder.composeView.setContent {
            val cardAspect = if (isLandscape) QUICK_CARD_ASPECT_LANDSCAPE else QUICK_CARD_ASPECT_PORTRAIT
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val maxCardWidth = if (isLandscape) {
                    maxWidth * QUICK_CARD_LANDSCAPE_CARD_WIDTH_FRACTION
                } else {
                    maxWidth
                }
                val maxCardHeight = maxHeight
                val widthByHeight = maxCardHeight * cardAspect
                val finalWidth = minOf(maxCardWidth, widthByHeight)
                val finalHeight = finalWidth / cardAspect

                QuickCardPreviewCard(
                    card = card,
                    landscape = isLandscape,
                    modifier = if (isLandscape) {
                        Modifier.size(width = finalWidth, height = finalHeight)
                    } else {
                        Modifier.width(finalWidth)
                    },
                    onClick = { click(card) },
                    onLongClick = { longPress(card) },
                    onEdit = { target -> edit(target) },
                    onShare = { target -> share(target) }
                )
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class QuickCardPageViewHolder(
        val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView)
}

@Composable
private fun QuickCardIndicatorRail(
    count: Int,
    current: Int,
    vertical: Boolean = true
) {
    val safeCount = count.coerceAtLeast(1)
    val dotSize = 6.dp
    val gap = 6.dp
    val contentSpan = dotSize * safeCount + gap * (safeCount - 1)
    val trackModifier = if (vertical) {
        Modifier.width(14.dp).height(contentSpan + 12.dp)
    } else {
        Modifier.height(14.dp).width(contentSpan + 12.dp)
    }
    val arrangement = Arrangement.spacedBy(6.dp)

    Card(
        shape = RoundedCornerShape(50),
        backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        elevation = 0.dp
    ) {
        if (vertical) {
            Column(
                modifier = trackModifier.padding(horizontal = 4.dp, vertical = 6.dp),
                verticalArrangement = arrangement,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(count) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (index == current) UiTokens.Primary else Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        } else {
            Row(
                modifier = trackModifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = arrangement,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(count) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (index == current) UiTokens.Primary else Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun QuickCardPreviewCard(
    card: QuickCard?,
    landscape: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: (QuickCard) -> Unit,
    onShare: (QuickCard) -> Unit
) {
    Card(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(UiTokens.Radius),
        backgroundColor = md2CardContainerColor(),
        elevation = UiTokens.CardElevation
    ) {
        if (card == null) {
            val placeholderModifier = if (landscape) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(QUICK_CARD_CONTENT_ASPECT_PORTRAIT)
            }
            Box(
                modifier = placeholderModifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(UiTokens.Radius))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MsIcon("add_circle", contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("点击以新建名片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Card
        }

        if (landscape) {
            QuickCardLandscapeContent(card = card, onEdit = onEdit, onShare = onShare)
        } else {
            QuickCardPortraitContent(card = card, onEdit = onEdit, onShare = onShare)
        }
    }
}

@Composable
private fun QuickCardBrandLogo(
    modifier: Modifier = Modifier
) {
    val logoRes = if (isSystemInDarkTheme()) R.drawable.logo_white else R.drawable.logo_black
    Image(
        painter = androidx.compose.ui.res.painterResource(id = logoRes),
        contentDescription = "KIGTTS",
        modifier = modifier.height(18.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun QuickCardPortraitContent(
    card: QuickCard,
    onEdit: (QuickCard) -> Unit,
    onShare: (QuickCard) -> Unit
) {
    val theme = quickCardThemeColor(card.themeColor)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(QUICK_CARD_CONTENT_ASPECT_PORTRAIT)
                .clip(RoundedCornerShape(UiTokens.Radius))
        ) {
            QuickCardHeroArea(
                card = card,
                landscape = false,
                modifier = Modifier.fillMaxSize(),
                onShare = onShare
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        card.title.ifBlank { "名片名字" },
                        style = MaterialTheme.typography.h6,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        card.note.ifBlank { "愿你的生活充满诗与远方" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(6.dp))
                Md2IconButton(
                    icon = "edit",
                    contentDescription = "编辑名片",
                    onClick = { onEdit(card) }
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickCardBrandLogo()
            }
            Spacer(Modifier.height(1.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(theme)
        )
    }
}

@Composable
private fun QuickCardLandscapeContent(
    card: QuickCard,
    onEdit: (QuickCard) -> Unit,
    onShare: (QuickCard) -> Unit
) {
    val theme = quickCardThemeColor(card.themeColor)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(QUICK_CARD_CONTENT_ASPECT_LANDSCAPE, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(UiTokens.Radius))
        ) {
            QuickCardHeroArea(
                card = card,
                landscape = true,
                modifier = Modifier.fillMaxSize(),
                onShare = onShare
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 2.dp)
        ) {
            Text(
                card.title.ifBlank { "名片名字" },
                style = MaterialTheme.typography.h6,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                card.note.ifBlank { "愿你的生活充满诗与远方" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Md2IconButton(
                    icon = "edit",
                    contentDescription = "编辑名片",
                    onClick = { onEdit(card) }
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                QuickCardBrandLogo()
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(theme)
        )
    }
}

@Composable
private fun QuickCardHeroArea(
    card: QuickCard,
    landscape: Boolean,
    modifier: Modifier,
    onShare: (QuickCard) -> Unit
) {
    val context = LocalContext.current
    val theme = quickCardThemeColor(card.themeColor)
    val onTheme = quickCardThemeOnColor(theme)
    val linkText = card.link.trim()
    val imagePath = card.heroImagePath(landscape)
    val imageBitmap = rememberQuickCardBitmap(imagePath)
    val qrBitmap = rememberQuickCardQrBitmap(linkText)
    val showLinkShare = linkText.isNotEmpty() && (card.type == QuickCardType.Qr || card.type == QuickCardType.Text)
    val showImageLinkActions = card.type == QuickCardType.Image && linkText.isNotEmpty()

    Box(modifier = modifier.background(theme)) {
        when (card.type) {
            QuickCardType.Image -> {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("未设置图片", color = onTheme.copy(alpha = 0.85f))
                    }
                }
                if (showImageLinkActions) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(if (landscape) 84.dp else 96.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.42f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = linkText,
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuickCardOverlayIconButton(
                                icon = "open_in_new",
                                contentDescription = "打开链接",
                                tint = Color.White
                            ) { openQuickCardLink(context, linkText) }
                            QuickCardOverlayIconButton(
                                icon = "share",
                                contentDescription = "分享链接",
                                tint = Color.White
                            ) { sharePlainText(context, linkText, "分享链接") }
                        }
                    }
                }
            }

            QuickCardType.Qr -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(if (landscape) 116.dp else 146.dp)
                                .clip(RoundedCornerShape(UiTokens.Radius))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "二维码",
                                modifier = Modifier.fillMaxSize(0.86f)
                            )
                        }
                    } else {
                        Text("未设置链接", color = onTheme.copy(alpha = 0.9f))
                    }
                    if (linkText.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = linkText,
                            color = onTheme,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CompositionLocalProvider(LocalContentColor provides onTheme) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Md2IconButton(
                                    icon = "open_in_new",
                                    contentDescription = "打开链接",
                                    onClick = { openQuickCardLink(context, linkText) }
                                )
                                Md2IconButton(
                                    icon = "share",
                                    contentDescription = "分享链接",
                                    onClick = { onShare(card) }
                                )
                            }
                        }
                    }
                }
            }

            QuickCardType.Text -> {
                val watermark = card.title.ifBlank { "名片名字" }
                var portraitWatermarkWidthPx by remember(watermark) { mutableIntStateOf(0) }
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (landscape) {
                        Text(
                            text = watermark,
                            color = onTheme.copy(alpha = 0.22f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp,
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .graphicsLayer(clip = false)
                                .width(maxWidth * 1.8f)
                                .padding(start = 8.dp, bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(68.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, theme)
                                    )
                                )
                        )
                    } else {
                        val topPad = 10.dp
                        val topPadPx = with(LocalDensity.current) { topPad.toPx() }
                        Text(
                            text = watermark,
                            color = onTheme.copy(alpha = 0.22f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 96.sp,
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            onTextLayout = { portraitWatermarkWidthPx = it.size.width },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 10.dp)
                                .graphicsLayer(
                                    rotationZ = 90f,
                                    transformOrigin = TransformOrigin(1f, 0f),
                                    translationY = portraitWatermarkWidthPx.toFloat() + topPadPx,
                                    clip = false
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, theme)
                                    )
                                )
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 16.dp)
                ) {
                    Text(
                        text = card.title.ifBlank { "名片名字" },
                        color = onTheme,
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (card.note.isNotBlank()) {
                        Text(
                            text = card.note,
                            color = onTheme.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (showLinkShare) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = linkText,
                                modifier = Modifier.weight(1f),
                                color = onTheme,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CompositionLocalProvider(LocalContentColor provides onTheme) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Md2IconButton(
                                        icon = "open_in_new",
                                        contentDescription = "打开链接",
                                        onClick = { openQuickCardLink(context, linkText) }
                                    )
                                    Md2IconButton(
                                        icon = "share",
                                        contentDescription = "分享链接",
                                        onClick = { onShare(card) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCardOverlayIconButton(
    icon: String,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
    ) {
        MsIcon(
            name = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Composable
private fun QuickCardTypeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    if (selected) {
        Md2Button(onClick = onClick) { Text(label) }
    } else {
        Md2OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun QuickCardEditorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onTopBarActionsChange: (QuickCardTopBarActions?) -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val draft = viewModel.quickCardDraft
    var cropLandscape by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var themeHexInput by rememberSaveable { mutableStateOf("#038387") }
    var themeHue by rememberSaveable { mutableFloatStateOf(180f) }
    var themeSat by rememberSaveable { mutableFloatStateOf(1f) }
    var themeLight by rememberSaveable { mutableFloatStateOf(0.27f) }
    var exitConfirmAutoSaveChecked by remember { mutableStateOf(false) }
    var suppressNullDraftAutoBack by remember { mutableStateOf(false) }
    val presetColors = remember {
        listOf("#038387", "#ff6d00", "#f4511e", "#8e24aa", "#3949ab", "#2e7d32", "#6d4c41", "#111111")
    }
    fun normalizeHexOrNull(raw: String): String? {
        val v = raw.trim().let { if (it.startsWith("#")) it else "#$it" }
        return if (Regex("^#[0-9a-fA-F]{6}$").matches(v)) v.lowercase(Locale.US) else null
    }

    if (draft == null) {
        LaunchedEffect(suppressNullDraftAutoBack) {
            if (!suppressNullDraftAutoBack) onBack()
        }
        return
    }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            if (uri != null) {
                if (!viewModel.setQuickCardDraftImage(uri, landscape = cropLandscape)) {
                    toast(context, "设置图片失败")
                }
            } else {
                toast(context, "裁剪失败：无输出")
            }
        } else {
            toast(context, "裁剪失败")
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val options = CropImageOptions(
            fixAspectRatio = true,
            aspectRatioX = if (cropLandscape) 5 else 3,
            aspectRatioY = if (cropLandscape) 3 else 5,
            activityTitle = "裁剪名片图片",
            cropMenuCropButtonTitle = "确认",
            activityMenuIconColor = 0xFFFFFFFF.toInt(),
            activityMenuTextColor = 0xFFFFFFFF.toInt(),
            activityBackgroundColor = 0xFF121212.toInt(),
            toolbarColor = 0xFF038387.toInt(),
            toolbarTitleColor = 0xFFFFFFFF.toInt(),
            toolbarBackButtonColor = 0xFFFFFFFF.toInt(),
            toolbarTintColor = 0xFFFFFFFF.toInt(),
            outputCompressFormat = android.graphics.Bitmap.CompressFormat.PNG,
            outputCompressQuality = 100
        )
        cropLauncher.launch(CropImageContractOptions(uri, options))
    }

    fun requestExitEditor() {
        val autoSave = uiState.quickCardAutoSaveOnExit
        val hasChanges = viewModel.hasQuickCardDraftChanges()
        if (!hasChanges) {
            onBack()
            return
        }
        if (autoSave) {
            val saved = viewModel.saveQuickCardDraft()
            if (saved != null) {
                toast(context, "已自动保存名片")
                onBack()
            } else {
                toast(context, "自动保存失败")
            }
            return
        }
        exitConfirmAutoSaveChecked = false
        showExitConfirm = true
    }

    BackHandler {
        requestExitEditor()
    }

    val isExisting = !draft.isNew && draft.editId != null
    LaunchedEffect(draft.themeColor) {
        if (!showThemeColorDialog) {
            themeHexInput = draft.themeColor
            val hsl = composeColorToHsl(quickCardThemeColor(draft.themeColor))
            themeHue = hsl[0]
            themeSat = hsl[1]
            themeLight = hsl[2]
        }
    }
    val editorActions = if (!isExisting) {
        QuickCardTopBarActions(
            onNew = {},
            onScan = {},
            onBackRequest = { requestExitEditor() }
        )
    } else {
        QuickCardTopBarActions(
            onNew = {},
            onScan = {},
            onCopy = {
                val copied = viewModel.duplicateEditingQuickCard()
                if (copied != null) toast(context, "已复制名片")
            },
            onDelete = { showDeleteConfirm = true },
            onBackRequest = { requestExitEditor() },
            canCopy = true,
            canDelete = true
        )
    }

    SideEffect {
        onTopBarActionsChange(editorActions)
    }
    DisposableEffect(Unit) {
        onDispose { onTopBarActionsChange(null) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(UiTokens.PageTopBlank))

        Md2SettingsCard("基础信息") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickCardTypeChip(
                    selected = draft.type == QuickCardType.Image,
                    label = "图片名片",
                    onClick = { viewModel.updateQuickCardDraft { it.copy(type = QuickCardType.Image) } }
                )
                QuickCardTypeChip(
                    selected = draft.type == QuickCardType.Qr,
                    label = "二维码名片",
                    onClick = { viewModel.updateQuickCardDraft { it.copy(type = QuickCardType.Qr) } }
                )
                QuickCardTypeChip(
                    selected = draft.type == QuickCardType.Text,
                    label = "文字名片",
                    onClick = { viewModel.updateQuickCardDraft { it.copy(type = QuickCardType.Text) } }
                )
            }
            Md2OutlinedField(
                value = draft.title,
                onValueChange = { viewModel.updateQuickCardDraft { old -> old.copy(title = it) } },
                label = "标题",
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.note,
                onValueChange = { viewModel.updateQuickCardDraft { old -> old.copy(note = it) } },
                label = { Text("备注（可空）") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = Md2ControlShape,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Md2OutlinedField(
                value = draft.link,
                onValueChange = { viewModel.updateQuickCardDraft { old -> old.copy(link = it) } },
                label = "链接（可空）",
                modifier = Modifier.fillMaxWidth()
            )
        }

        Md2SettingsCard("主题色") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presetColors.forEach { hex ->
                    val c = quickCardThemeColor(hex)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .clickable { viewModel.updateQuickCardDraft { old -> old.copy(themeColor = hex) } }
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFF3D00),
                                    Color(0xFFFFC400),
                                    Color(0xFF00C853),
                                    Color(0xFF00B0FF),
                                    Color(0xFF7C4DFF),
                                    Color(0xFFFF3D00)
                                )
                            )
                        )
                        .clickable {
                            themeHexInput = draft.themeColor
                            val hsl = composeColorToHsl(quickCardThemeColor(draft.themeColor))
                            themeHue = hsl[0]
                            themeSat = hsl[1]
                            themeLight = hsl[2]
                            showThemeColorDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MsIcon("palette", contentDescription = "自定义颜色", tint = Color.White)
                }
            }
        }

        if (draft.type == QuickCardType.Image) {
            Md2SettingsCard("图片") {
                QuickCardImagePathRow(
                    title = "竖屏图片",
                    path = draft.portraitImagePath,
                    onClear = { viewModel.clearQuickCardDraftImage(landscape = false) },
                    onPick = {
                        cropLandscape = false
                        imagePicker.launch("image/*")
                    }
                )
                QuickCardImagePathRow(
                    title = "横屏图片",
                    path = draft.landscapeImagePath,
                    onClear = { viewModel.clearQuickCardDraftImage(landscape = true) },
                    onPick = {
                        cropLandscape = true
                        imagePicker.launch("image/*")
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Md2Button(
                onClick = {
                    val saved = viewModel.saveQuickCardDraft()
                    if (saved != null) {
                        toast(context, "已保存名片")
                        onBack()
                    } else {
                        toast(context, "保存失败")
                    }
                }
            ) {
                Text("保存")
            }
        }
        Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除名片") },
            text = { Text("确定删除当前名片吗？") },
            confirmButton = {
                Md2TextButton(onClick = {
                    showDeleteConfirm = false
                    suppressNullDraftAutoBack = true
                    if (viewModel.deleteEditingQuickCard()) {
                        toast(context, "已删除名片")
                    }
                    onBack()
                }) { Text("删除") }
            },
            dismissButton = {
                Md2TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("名片已编辑") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("是否保存名片后再退出编辑？")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = exitConfirmAutoSaveChecked,
                            onCheckedChange = { exitConfirmAutoSaveChecked = it }
                        )
                        Text("下次退出编辑时自动保存")
                    }
                }
            },
            confirmButton = {
                Md2TextButton(onClick = {
                    showExitConfirm = false
                    if (exitConfirmAutoSaveChecked) {
                        viewModel.setQuickCardAutoSaveOnExit(true)
                    }
                    val saved = viewModel.saveQuickCardDraft()
                    if (saved != null) {
                        toast(context, "已保存名片")
                        onBack()
                    } else {
                        toast(context, "保存失败")
                    }
                }) { Text("保存并退出") }
            },
            dismissButton = {
                Row {
                    Md2TextButton(onClick = {
                        showExitConfirm = false
                        if (exitConfirmAutoSaveChecked) {
                            viewModel.setQuickCardAutoSaveOnExit(true)
                        }
                        onBack()
                    }) { Text("不保存退出") }
                    Md2TextButton(onClick = { showExitConfirm = false }) { Text("取消") }
                }
            }
        )
    }

    if (showThemeColorDialog) {
        AlertDialog(
            onDismissRequest = { showThemeColorDialog = false },
            title = { Text("HSL 滑条取色") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val preview = hslToComposeColor(themeHue, themeSat, themeLight)
                    val hueGradient = Brush.horizontalGradient(
                        listOf(
                            hslToComposeColor(0f, 1f, 0.5f),
                            hslToComposeColor(60f, 1f, 0.5f),
                            hslToComposeColor(120f, 1f, 0.5f),
                            hslToComposeColor(180f, 1f, 0.5f),
                            hslToComposeColor(240f, 1f, 0.5f),
                            hslToComposeColor(300f, 1f, 0.5f),
                            hslToComposeColor(360f, 1f, 0.5f)
                        )
                    )
                    val satGradient = remember(themeHue, themeLight) {
                        Brush.horizontalGradient(
                            listOf(
                                hslToComposeColor(themeHue, 0f, themeLight),
                                hslToComposeColor(themeHue, 1f, themeLight)
                            )
                        )
                    }
                    val lightGradient = remember(themeHue, themeSat) {
                        Brush.horizontalGradient(
                            listOf(
                                hslToComposeColor(themeHue, themeSat, 0f),
                                hslToComposeColor(themeHue, themeSat, 0.5f),
                                hslToComposeColor(themeHue, themeSat, 1f)
                            )
                        )
                    }
                    HslGradientSlider(
                        label = "色相",
                        value = themeHue,
                        valueRange = 0f..360f,
                        gradient = hueGradient,
                        onValueChange = {
                            themeHue = it
                            themeHexInput = colorToHexRgb(hslToComposeColor(themeHue, themeSat, themeLight))
                        }
                    )
                    HslGradientSlider(
                        label = "饱和度",
                        value = themeSat,
                        valueRange = 0f..1f,
                        gradient = satGradient,
                        onValueChange = {
                            themeSat = it
                            themeHexInput = colorToHexRgb(hslToComposeColor(themeHue, themeSat, themeLight))
                        }
                    )
                    HslGradientSlider(
                        label = "亮度",
                        value = themeLight,
                        valueRange = 0f..1f,
                        gradient = lightGradient,
                        onValueChange = {
                            themeLight = it
                            themeHexInput = colorToHexRgb(hslToComposeColor(themeHue, themeSat, themeLight))
                        },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(UiTokens.Radius))
                            .background(preview)
                    )
                    OutlinedTextField(
                        value = themeHexInput,
                        onValueChange = {
                            themeHexInput = it
                            val normalized = normalizeHexOrNull(it)
                            if (normalized != null) {
                                val hsl = composeColorToHsl(quickCardThemeColor(normalized))
                                themeHue = hsl[0]
                                themeSat = hsl[1]
                                themeLight = hsl[2]
                            }
                        },
                        singleLine = true,
                        label = { Text("HEX（#RRGGBB）") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        shape = Md2ControlShape,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "拖动三条滑条设置色相、饱和度和亮度",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Md2TextButton(
                    onClick = {
                        val normalized = normalizeHexOrNull(themeHexInput)
                        if (normalized == null) {
                            toast(context, "HEX 格式错误")
                        } else {
                            viewModel.updateQuickCardDraft { old -> old.copy(themeColor = normalized) }
                            showThemeColorDialog = false
                        }
                    }
                ) { Text("应用") }
            },
            dismissButton = {
                Md2TextButton(onClick = { showThemeColorDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun QuickCardImagePathRow(
    title: String,
    path: String,
    onClear: () -> Unit,
    onPick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (path.isBlank()) "未选择图片" else path,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (path.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Md2IconButton(
                icon = "close",
                contentDescription = "清空图片",
                onClick = onClear,
                enabled = path.isNotBlank()
            )
            Md2IconButton(
                icon = "folder_open",
                contentDescription = "选择图片",
                onClick = onPick
            )
        }
    }
}

private fun composeColorToHsl(color: Color): FloatArray {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    return hsl
}

private fun hslToComposeColor(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val sat = s.coerceIn(0f, 1f)
    val light = l.coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, light)))
}

private fun colorToHexRgb(color: Color): String {
    val argb = color.toArgb()
    val rgb = argb and 0x00FFFFFF
    return String.format(Locale.US, "#%06x", rgb)
}

@Composable
private fun HslGradientSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    gradient: Brush,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .background(gradient, RectangleShape)
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
        }
    }
}

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

@Composable
private fun Md2StaggeredFloatIn(
    index: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var visible by remember(index, enabled) { mutableStateOf(!enabled) }

    LaunchedEffect(index, enabled) {
        if (!enabled) {
            visible = true
            return@LaunchedEffect
        }
        visible = false
        delay((40L * index).coerceAtMost(260L))
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(220)) +
                slideInVertically(
                    initialOffsetY = { full -> (full * 0.12f).toInt() },
                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                ),
        exit = fadeOut(animationSpec = tween(90))
    ) {
        content()
    }
}

private data class DrawerItem(
    val page: Int,
    val title: String,
    val icon: String
)

data class LogTopBarActions(
    val onRefresh: () -> Unit,
    val onCopy: () -> Unit,
    val onShare: () -> Unit,
    val canCopy: Boolean,
    val canShare: Boolean
)

data class QuickCardTopBarActions(
    val onNew: () -> Unit = {},
    val onScan: () -> Unit = {},
    val onCopy: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
    val onConfirm: (() -> Unit)? = null,
    val onBackRequest: (() -> Unit)? = null,
    val canCopy: Boolean = false,
    val canDelete: Boolean = false,
    val canConfirm: Boolean = false
)

private object QuickSubtitleRoutes {
    const val Main = "quick_subtitle/main"
    const val Editor = "quick_subtitle/editor"
    const val History = "quick_subtitle/history"
}

private object QuickCardRoutes {
    const val Main = "quick_card/main"
    const val Editor = "quick_card/editor"
    const val Sort = "quick_card/sort"
    const val Scanner = "quick_card/scanner"
    private const val ScanTextArg = "text"
    const val ScanText = "quick_card/scan_text/{$ScanTextArg}"
    private const val WebArg = "url"
    const val Web = "quick_card/web/{$WebArg}"

    fun scanText(text: String): String = "quick_card/scan_text/${Uri.encode(text)}"
    fun web(url: String): String = "quick_card/web/${Uri.encode(url)}"
}

private object SettingsRoutes {
    const val Main = "settings/main"
    const val Log = "settings/log"
}

class MainActivity : ComponentActivity() {
    private var lastDecorFitsSystemWindows: Boolean = false
    private var pendingBackgroundReturnFix: Boolean = false
    private var delayedResumeFixRunnable: Runnable? = null

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
        handleLaunchIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        applyWindowInsetPolicyForMode()
        AppLogger.i(
            "MainActivity.onMultiWindowModeChanged inMultiWindow=$isInMultiWindowMode " +
                    "decorFits=$lastDecorFitsSystemWindows softInput=${softInputModeSummary(window.attributes.softInputMode)}"
        )
    }

    override fun onResume() {
        super.onResume()
        if (pendingBackgroundReturnFix) {
            pendingBackgroundReturnFix = false
            delayedResumeFixRunnable?.let { window.decorView.removeCallbacks(it) }
            delayedResumeFixRunnable = Runnable {
                val stateMask =
                    window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE
                window.setSoftInputMode(stateMask or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                lastDecorFitsSystemWindows = false
                AppLogger.i(
                    "MainActivity.delayedResumeFix applied delayMs=500 " +
                            "decorFits=$lastDecorFitsSystemWindows softInput=${softInputModeSummary(window.attributes.softInputMode)}"
                )
            }
            window.decorView.postDelayed(delayedResumeFixRunnable, 500L)
            AppLogger.i("MainActivity.delayedResumeFix scheduled delayMs=500")
        }
        AppLogger.i(
            "MainActivity.onResume inMultiWindow=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInMultiWindowMode else false} " +
                    "decorFits=$lastDecorFitsSystemWindows softInput=${softInputModeSummary(window.attributes.softInputMode)}"
        )
        syncFloatingOverlayState()
    }

    override fun onPause() {
        delayedResumeFixRunnable?.let { window.decorView.removeCallbacks(it) }
        delayedResumeFixRunnable = null
        AppLogger.i(
            "MainActivity.onPause inMultiWindow=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInMultiWindowMode else false} " +
                    "decorFits=$lastDecorFitsSystemWindows softInput=${softInputModeSummary(window.attributes.softInputMode)}"
        )
        super.onPause()
    }

    override fun onStop() {
        pendingBackgroundReturnFix = true
        AppLogger.i("MainActivity.onStop markPendingBackgroundReturnFix=true")
        super.onStop()
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
        lastDecorFitsSystemWindows = inMultiWindow
        AppLogger.i(
            "applyWindowInsetPolicyForMode inMultiWindow=$inMultiWindow " +
                    "decorFits=$lastDecorFitsSystemWindows softInput=${softInputModeSummary(window.attributes.softInputMode)}"
        )
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.action != OverlayBridge.ACTION_OPEN_QUICK_SUBTITLE) return
        val requestId = intent.getLongExtra(OverlayBridge.EXTRA_REQUEST_ID, Long.MIN_VALUE)
        if (requestId == Long.MIN_VALUE) return
        val target = intent.getStringExtra(OverlayBridge.EXTRA_TARGET) ?: OverlayBridge.TARGET_SUBTITLE
        val text = intent.getStringExtra(OverlayBridge.EXTRA_TEXT).orEmpty()
        viewModel.handleQuickSubtitleLaunchRequest(requestId, target, text)
    }

    private fun syncFloatingOverlayState() {
        val enabled = viewModel.uiState.floatingOverlayEnabled
        if (!enabled) {
            FloatingOverlayService.stop(this)
            return
        }
        if (!FloatingOverlayService.canDrawOverlays(this)) {
            viewModel.setFloatingOverlayEnabled(false)
            FloatingOverlayService.stop(this)
            return
        }
        FloatingOverlayService.start(this)
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
private fun Md2DropdownButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    expanded: Boolean = false
) {
    Md2OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MsIcon(
                name = if (expanded) "expand_less" else "expand_more",
                contentDescription = null
            )
        }
    }
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
    // Disabled state should look clearly gray, not transparent/faded-out.
    val disabledTrack = if (dark) Color(0xFF4D555B) else Color(0xFFD0D6DB)
    val disabledThumb = if (dark) Color(0xFF99A2A9) else Color(0xFF8E979E)
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
            disabledCheckedThumbColor = disabledThumb,
            disabledCheckedTrackColor = disabledTrack,
            disabledUncheckedThumbColor = disabledThumb,
            disabledUncheckedTrackColor = disabledTrack
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
@OptIn(ExperimentalComposeUiApi::class)
fun AppScaffold(viewModel: MainViewModel) {
    val pageQuickSubtitle = 0
    val pageOverlay = 1
    val pageQuickCard = 2
    val pageVoicePack = 3
    val pageDrawing = 4
    val pageSettings = 5

    var page by rememberSaveable { mutableStateOf(pageQuickSubtitle) }
    var drawingFullscreen by rememberSaveable { mutableStateOf(false) }
    var quickSubtitleFullscreen by rememberSaveable { mutableStateOf(false) }
    var runningStripCollapsed by rememberSaveable { mutableStateOf(true) }
    var logTopBarActions by remember { mutableStateOf<LogTopBarActions?>(null) }
    var quickCardTopBarActions by remember { mutableStateOf<QuickCardTopBarActions?>(null) }
    var quickCardWebMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var quickCardNavReady by remember { mutableStateOf(false) }
    var pendingQuickCardOverlayTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val quickSubtitleNavController = rememberNavController()
    val quickCardNavController = rememberNavController()
    val settingsNavController = rememberNavController()
    val quickSubtitleBackStackEntry by quickSubtitleNavController.currentBackStackEntryAsState()
    val quickSubtitleRoute = quickSubtitleBackStackEntry?.destination?.route ?: QuickSubtitleRoutes.Main
    val quickCardBackStackEntry by quickCardNavController.currentBackStackEntryAsState()
    val quickCardRoute = quickCardBackStackEntry?.destination?.route ?: QuickCardRoutes.Main
    val quickCardWebUrl = remember(quickCardBackStackEntry) {
        Uri.decode(quickCardBackStackEntry?.arguments?.getString("url").orEmpty())
    }
    val settingsBackStackEntry by settingsNavController.currentBackStackEntryAsState()
    val settingsRoute = settingsBackStackEntry?.destination?.route ?: SettingsRoutes.Main
    val state = viewModel.uiState
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val localView = LocalView.current
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
    val layoutDirection = LocalLayoutDirection.current
    val displayCutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val landscapeCutoutStart = if (isLandscape && !inMultiWindowMode) {
        displayCutoutPadding.calculateStartPadding(layoutDirection)
    } else {
        0.dp
    }
    val landscapeCutoutEnd = if (isLandscape && !inMultiWindowMode) {
        displayCutoutPadding.calculateEndPadding(layoutDirection)
    } else {
        0.dp
    }
    val hiddenDrawerWidth = remember(configuration.screenWidthDp) {
        val screenWidth = configuration.screenWidthDp.dp
        val targetWidth = UiTokens.DrawerWidthExpanded
        // Keep a small right-side visible area on narrow screens to avoid hard overflow.
        val compatEdgeGap = 24.dp
        val maxAllowed = (screenWidth - compatEdgeGap).coerceAtLeast(0.dp)
        if (maxAllowed <= 0.dp) screenWidth else minOf(targetWidth, maxAllowed)
    }
    val permanentDrawerCollapsedWidth = UiTokens.DrawerWidthCollapsed + landscapeCutoutStart
    val permanentDrawerExpandedWidth = UiTokens.DrawerWidthExpanded + landscapeCutoutStart
    val hiddenDrawerSurfaceWidth = hiddenDrawerWidth + landscapeCutoutStart
    val usePermanentDrawer =
        isLandscape && state.landscapeDrawerMode == UserPrefs.DRAWER_MODE_PERMANENT
    val basePage = page
    val quickSubtitleEditorOpen =
        basePage == pageQuickSubtitle && quickSubtitleRoute == QuickSubtitleRoutes.Editor
    val quickSubtitleHistoryOpen =
        basePage == pageQuickSubtitle && quickSubtitleRoute == QuickSubtitleRoutes.History
    val quickSubtitleSubPageOpen =
        basePage == pageQuickSubtitle && quickSubtitleRoute != QuickSubtitleRoutes.Main
    val quickCardEditorOpen =
        basePage == pageQuickCard && quickCardRoute == QuickCardRoutes.Editor
    val quickCardSortOpen =
        basePage == pageQuickCard && quickCardRoute == QuickCardRoutes.Sort
    val quickCardScannerOpen =
        basePage == pageQuickCard && quickCardRoute == QuickCardRoutes.Scanner
    val quickCardScanTextOpen =
        basePage == pageQuickCard && quickCardRoute == QuickCardRoutes.ScanText
    val quickCardWebOpen =
        basePage == pageQuickCard && quickCardRoute == QuickCardRoutes.Web
    val quickCardMainOpen =
        basePage == pageQuickCard && quickCardRoute == QuickCardRoutes.Main
    val quickCardSubPageOpen =
        basePage == pageQuickCard && quickCardRoute != QuickCardRoutes.Main
    val settingsLogOpen =
        basePage == pageSettings && settingsRoute == SettingsRoutes.Log
    var lastTopBarBackClickAtMs by remember { mutableLongStateOf(0L) }
    var drawerExpanded by rememberSaveable { mutableStateOf(false) }
    val runningStripEligible = !(drawingFullscreen && basePage == pageDrawing)
    val showRunningStripButton = runningStripEligible
    val showRunningStripPanel = runningStripEligible && !runningStripCollapsed
    val topMicLevel = viewModel.realtimeInputLevel
    val topPlaybackProgress = viewModel.realtimePlaybackProgress
    val drawerItems = listOf(
        DrawerItem(pageQuickSubtitle, "便捷字幕", "subtitles"),
        DrawerItem(pageOverlay, "悬浮窗", "open_in_new"),
        DrawerItem(pageQuickCard, "快捷名片", "id_card"),
        DrawerItem(pageVoicePack, "语音包", "record_voice_over"),
        DrawerItem(pageDrawing, "画板", "draw"),
        DrawerItem(pageSettings, "设置", "tune")
    )
    val pendingQuickSubtitleLaunchRequest = viewModel.pendingQuickSubtitleLaunchRequest
    val drawerSelectedPage = basePage
    DisposableEffect(viewModel) {
        val delegate = object : RealtimeRuntimeBridge.AppDelegate {
            override fun startRealtime() {
                viewModel.start()
            }

            override fun stopRealtime() {
                viewModel.stop()
            }

            override fun submitQuickSubtitle(target: String, text: String) {
                viewModel.applyExternalQuickSubtitleRequest(target, text)
            }
        }
        RealtimeRuntimeBridge.registerAppDelegate(delegate)
        onDispose {
            RealtimeRuntimeBridge.unregisterAppDelegate(delegate)
        }
    }
    SideEffect {
        RealtimeRuntimeBridge.updateAppSnapshot(
            RealtimeRuntimeBridge.Snapshot(
                running = state.running,
                latestRecognizedText = viewModel.realtimeRecognized.firstOrNull()?.text.orEmpty(),
                inputLevel = topMicLevel.coerceIn(0f, 1f),
                playbackProgress = topPlaybackProgress.coerceIn(0f, 1f),
                inputDeviceLabel = state.inputDeviceLabel,
                outputDeviceLabel = state.outputDeviceLabel
            )
        )
    }
    LaunchedEffect(drawerItems.size) {
        val validPages = drawerItems.map { it.page }.toSet()
        if (page !in validPages) {
            page = pageQuickSubtitle
        }
    }
    LaunchedEffect(basePage, quickSubtitleRoute) {
        if (basePage != pageQuickSubtitle && quickSubtitleRoute != QuickSubtitleRoutes.Main) {
            quickSubtitleNavController.popBackStack(QuickSubtitleRoutes.Main, inclusive = false)
        }
    }
    LaunchedEffect(basePage, quickCardRoute) {
        if (basePage != pageQuickCard && quickCardRoute != QuickCardRoutes.Main) {
            quickCardNavController.popBackStack(QuickCardRoutes.Main, inclusive = false)
        }
    }
    LaunchedEffect(basePage) {
        if (basePage != pageQuickCard) {
            quickCardNavReady = false
        }
    }
    LaunchedEffect(basePage, quickCardWebOpen) {
        if (basePage != pageQuickCard || !quickCardWebOpen) {
            quickCardWebMenuExpanded = false
        }
    }
    LaunchedEffect(basePage, quickCardNavReady, pendingQuickCardOverlayTarget, quickCardRoute) {
        if (basePage != pageQuickCard || !quickCardNavReady) return@LaunchedEffect
        when (pendingQuickCardOverlayTarget) {
            OverlayBridge.TARGET_OPEN_QUICK_CARD -> {
                if (quickCardRoute != QuickCardRoutes.Main) {
                    quickCardNavController.popBackStack(QuickCardRoutes.Main, inclusive = false)
                }
                pendingQuickCardOverlayTarget = null
            }
            OverlayBridge.TARGET_OPEN_QR_SCANNER -> {
                if (quickCardRoute != QuickCardRoutes.Main &&
                    quickCardRoute != QuickCardRoutes.Scanner
                ) {
                    quickCardNavController.popBackStack(QuickCardRoutes.Main, inclusive = false)
                }
                if (quickCardRoute != QuickCardRoutes.Scanner) {
                    quickCardNavController.navigate(QuickCardRoutes.Scanner) {
                        launchSingleTop = true
                    }
                }
                pendingQuickCardOverlayTarget = null
            }
        }
    }
    LaunchedEffect(pendingQuickSubtitleLaunchRequest?.requestId) {
        val request = pendingQuickSubtitleLaunchRequest ?: return@LaunchedEffect
        when (request.target) {
            OverlayBridge.TARGET_OPEN_OVERLAY -> {
                page = pageOverlay
            }
            OverlayBridge.TARGET_OPEN_QUICK_CARD -> {
                page = pageQuickCard
                pendingQuickCardOverlayTarget = request.target
            }
            OverlayBridge.TARGET_OPEN_QR_SCANNER -> {
                page = pageQuickCard
                pendingQuickCardOverlayTarget = request.target
            }
            OverlayBridge.TARGET_OPEN_DRAWING -> {
                page = pageDrawing
            }
            OverlayBridge.TARGET_OPEN_SETTINGS -> {
                page = pageSettings
                if (settingsRoute != SettingsRoutes.Main) {
                    settingsNavController.popBackStack(SettingsRoutes.Main, inclusive = false)
                }
            }
            else -> {
                quickSubtitleFullscreen = false
                if (page != pageQuickSubtitle) {
                    page = pageQuickSubtitle
                }
                if (quickSubtitleRoute != QuickSubtitleRoutes.Main) {
                    quickSubtitleNavController.popBackStack(QuickSubtitleRoutes.Main, inclusive = false)
                }
                viewModel.applyExternalQuickSubtitleRequest(request.target, request.text)
            }
        }
        viewModel.consumeQuickSubtitleLaunchRequest(request.requestId)
        if (!usePermanentDrawer) {
            drawerState.close()
        }
    }
    LaunchedEffect(state.floatingOverlayEnabled) {
        if (!state.floatingOverlayEnabled) {
            FloatingOverlayService.stop(context)
        } else if (FloatingOverlayService.canDrawOverlays(context)) {
            FloatingOverlayService.start(context)
        } else {
            viewModel.setFloatingOverlayEnabled(false)
            FloatingOverlayService.stop(context)
        }
    }
    LaunchedEffect(basePage, settingsRoute) {
        if (basePage != pageSettings && settingsRoute != SettingsRoutes.Main) {
            settingsNavController.popBackStack(SettingsRoutes.Main, inclusive = false)
        }
    }
    LaunchedEffect(page) {
        if (page != pageDrawing) drawingFullscreen = false
        if (page != pageQuickSubtitle) quickSubtitleFullscreen = false
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
    LaunchedEffect(basePage, settingsLogOpen) {
        if (!settingsLogOpen) {
            logTopBarActions = null
        }
    }
    LaunchedEffect(basePage, quickCardEditorOpen) {
        if (basePage != pageQuickCard) {
            quickCardTopBarActions = null
        } else if (!quickCardEditorOpen) {
            // main page keeps actions set by QuickCardNavHost
        }
    }
    val baseSoftInputMode = remember(activity) {
        activity?.window?.attributes?.softInputMode
    }
    fun applySoftInputModeForRoute() {
        val window = activity?.window ?: return
        val base = baseSoftInputMode ?: return
        val stateMask = base and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE
        val adjustMask = if (quickSubtitleEditorOpen) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        } else {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }
        window.setSoftInputMode(stateMask or adjustMask)
        AppLogger.i(
            "AppScaffold.applySoftInputModeForRoute page=$basePage route=$quickSubtitleRoute " +
                    "editorOpen=$quickSubtitleEditorOpen softInput=${softInputModeSummary(window.attributes.softInputMode)}"
        )
    }
    fun popSecondaryPageSafely() {
        val now = SystemClock.elapsedRealtime()
        // Guard double taps: avoid consecutive pop during transition.
        if (now - lastTopBarBackClickAtMs < 280L) return
        lastTopBarBackClickAtMs = now
        when {
            quickSubtitleSubPageOpen -> {
                quickSubtitleNavController.popBackStack(QuickSubtitleRoutes.Main, inclusive = false)
            }
            settingsLogOpen -> {
                settingsNavController.popBackStack(SettingsRoutes.Main, inclusive = false)
            }
            quickCardEditorOpen -> {
                val handledByEditor = quickCardTopBarActions?.onBackRequest != null
                quickCardTopBarActions?.onBackRequest?.invoke()
                if (!handledByEditor) {
                    quickCardNavController.popBackStack(QuickCardRoutes.Main, inclusive = false)
                }
            }
            quickCardSubPageOpen -> {
                quickCardNavController.popBackStack(QuickCardRoutes.Main, inclusive = false)
            }
        }
    }

    fun clearFocusAndHideIme(reason: String) {
        activity?.currentFocus?.clearFocus()
        localView.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(localView.windowToken, 0)
        AppLogger.i("AppScaffold.clearFocusAndHideIme reason=$reason")
    }

    SideEffect {
        applySoftInputModeForRoute()
    }
    DisposableEffect(activity, lifecycleOwner, baseSoftInputMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                applySoftInputModeForRoute()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val window = activity?.window
            val base = baseSoftInputMode
            if (window != null && base != null) {
                window.setSoftInputMode(base)
            }
        }
    }
    LaunchedEffect(basePage, quickSubtitleRoute) {
        if (basePage != pageQuickSubtitle || quickSubtitleRoute != QuickSubtitleRoutes.Main) {
            clearFocusAndHideIme("leave_quick_subtitle_main")
        }
    }
    LaunchedEffect(basePage, quickSubtitleRoute, quickSubtitleEditorOpen, inMultiWindowMode) {
        val mode = activity?.window?.attributes?.softInputMode ?: 0
        AppLogger.i(
            "AppScaffold.routeChanged page=$basePage route=$quickSubtitleRoute " +
                    "editorOpen=$quickSubtitleEditorOpen inMultiWindow=$inMultiWindowMode " +
                    "softInput=${softInputModeSummary(mode)}"
        )
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
    val onPushToTalkPressStart = {
        viewModel.setPushToTalkPressed(true)
        viewModel.beginPushToTalkSession()
        if (!state.running) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.start()
            } else {
                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    val onPushToTalkPressEnd: (PttConfirmReleaseAction) -> Unit = { releaseAction ->
        viewModel.commitPushToTalkSession(releaseAction)
        viewModel.setPushToTalkPressed(false)
        if (state.running) {
            viewModel.stop()
        }
    }
    var realtimePttDragTarget by remember { mutableStateOf(PttConfirmDragTarget.DefaultSend) }
    val realtimeConfirmOverlayEnabled = false
    val realtimeShowPttConfirmOverlay =
        realtimeConfirmOverlayEnabled && state.pushToTalkPressed
    val realtimePttFabSize = 56.dp
    val realtimePttFabEndInset = 16.dp
    val realtimePttFabBottomOffset = 16.dp
    val realtimePttStatusStripBottomOffset = realtimePttFabBottomOffset
    val realtimePttStatusStripBottomBleed = 12.dp
    val realtimeCompactModeDetectionEnabled =
        isLandscape && realtimeConfirmOverlayEnabled
    val realtimeImeBottomInset =
        if (realtimeCompactModeDetectionEnabled) WindowInsets.ime.asPaddingValues().calculateBottomPadding() else 0.dp
    val realtimeNavBottomInset =
        if (realtimeCompactModeDetectionEnabled) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp
    val realtimeBottomObstructionInset =
        if (realtimeImeBottomInset > realtimeNavBottomInset) realtimeImeBottomInset else realtimeNavBottomInset
    val realtimeImeVisible = realtimeImeBottomInset > 0.dp
    val realtimePttTopButtonsRequiredHeight = 96.dp
    val realtimePttTopRowBottomReserved = if (isLandscape) 72.dp else 74.dp
    val realtimePttTopEstimatedAvailableHeight =
        configuration.screenHeightDp.dp - realtimeBottomObstructionInset -
            (realtimePttFabSize + realtimePttFabBottomOffset + realtimePttStatusStripBottomBleed + 72.dp)
    val realtimeCompactPttSideButtonsMode =
        realtimeCompactModeDetectionEnabled &&
            (realtimeImeVisible || realtimePttTopEstimatedAvailableHeight < realtimePttTopButtonsRequiredHeight)
    val realtimePttGuideText = when (realtimePttDragTarget) {
        PttConfirmDragTarget.DefaultSend -> "松开手指上屏"
        PttConfirmDragTarget.ToInput -> "松开手指上屏"
        PttConfirmDragTarget.Cancel -> "松开取消发送"
    }
    val realtimePttStripFabReserveWidth = realtimePttFabSize
    val realtimePttStatusStripEndInset = realtimePttFabEndInset
    val realtimePttStatusStripAnchorEndInset = realtimePttStatusStripEndInset + (realtimePttFabSize / 2)
    val realtimePttStatusStripOuterBleed = 12.dp
    val realtimePttStatusStripAnimatedEndInset by animateDpAsState(
        targetValue = if (realtimeShowPttConfirmOverlay) {
            realtimePttStatusStripEndInset
        } else {
            realtimePttStatusStripAnchorEndInset
        },
        animationSpec = if (realtimeShowPttConfirmOverlay) {
            tween(durationMillis = 220, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 180, easing = FastOutSlowInEasing)
        },
        label = "realtime_ptt_status_strip_end_inset"
    )
    val realtimePttStatusStripStartInset = (10.dp - realtimePttStatusStripOuterBleed).coerceAtLeast(0.dp)
    val realtimePttStatusStripTopBleed = (realtimePttStatusStripOuterBleed - 4.dp).coerceAtLeast(0.dp)
    val realtimePttStatusStripAnimatedEndInsetWithBleed =
        (realtimePttStatusStripAnimatedEndInset - realtimePttStatusStripOuterBleed).coerceAtLeast(0.dp)
    val realtimePttStatusStripBottomInset =
        (realtimePttStatusStripBottomOffset - realtimePttStatusStripBottomBleed).coerceAtLeast(0.dp)
    LaunchedEffect(realtimeShowPttConfirmOverlay) {
        if (!realtimeShowPttConfirmOverlay) {
            realtimePttDragTarget = PttConfirmDragTarget.DefaultSend
        }
    }

    val topBar: @Composable ((() -> Unit)) -> Unit = { onNavClick ->
        val currentTitle = if (quickSubtitleEditorOpen) {
            "编辑便捷字幕"
        } else if (quickSubtitleHistoryOpen) {
            "历史记录"
        } else if (quickCardEditorOpen) {
            "编辑快捷名片"
        } else if (quickCardSortOpen) {
            "排序名片"
        } else if (quickCardScannerOpen) {
            "扫描二维码"
        } else if (quickCardScanTextOpen) {
            "二维码结果"
        } else if (quickCardWebOpen) {
            "二维码网页"
        } else if (settingsLogOpen) {
            "日志"
        } else {
            when (basePage) {
                pageQuickSubtitle -> "便捷字幕"
                pageOverlay -> "悬浮窗"
                pageQuickCard -> "快捷名片"
                pageVoicePack -> "语音包"
                pageDrawing -> "画板"
                pageSettings -> "设置"
                else -> "KIGTTS"
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = miuiFloatingTopCompensation)
                .zIndex(2f),
            color = topBarColor,
            elevation = UiTokens.TopBarElevation
        ) {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!inMultiWindowMode) Modifier.statusBarsPadding() else Modifier)
                    .padding(start = landscapeCutoutStart, end = landscapeCutoutEnd),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Crossfade(
                            modifier = Modifier.weight(1f),
                            targetState = currentTitle,
                            animationSpec = tween(140, easing = LinearEasing),
                            label = "topbar_title_switch"
                        ) { titleText ->
                            Text(
                                text = titleText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AnimatedVisibility(
                            visible = showRunningStripButton,
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
                                pushToTalkMode = state.pushToTalkMode,
                                pushToTalkPressed = state.pushToTalkPressed,
                                contentColor = topBarContentColor,
                                onToggle = { runningStripCollapsed = !runningStripCollapsed }
                            )
                        }
                    }
                },
                navigationIcon = {
                    AnimatedContent(
                        targetState = when {
                            quickSubtitleSubPageOpen -> 1
                            settingsLogOpen -> 2
                            quickCardSubPageOpen -> 3
                            else -> 0
                        },
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn(
                                    animationSpec = tween(120, easing = LinearEasing)
                                ),
                                initialContentExit = fadeOut(
                                    animationSpec = tween(120, easing = LinearEasing)
                                )
                            )
                        },
                        label = "topbar_nav_switch"
                    ) { navMode ->
                        if (navMode == 1 || navMode == 2 || navMode == 3) {
                            IconButton(onClick = {
                                popSecondaryPageSafely()
                            }) {
                                MsIcon("arrow_back", contentDescription = "返回")
                            }
                        } else {
                            IconButton(onClick = onNavClick) {
                                MsIcon("menu", contentDescription = "打开菜单")
                            }
                        }
                    }
                },
                actions = {
                    val quickCardActions = quickCardTopBarActions
                    val showQuickSubtitleActions =
                        basePage == pageQuickSubtitle && quickSubtitleRoute == QuickSubtitleRoutes.Main
                    val showQuickCardMainActions =
                        basePage == pageQuickCard &&
                                quickCardRoute == QuickCardRoutes.Main &&
                                quickCardActions != null
                    val showQuickCardEditorActions =
                        basePage == pageQuickCard &&
                                quickCardRoute == QuickCardRoutes.Editor &&
                                quickCardActions != null &&
                                (quickCardActions.canCopy || quickCardActions.canDelete)
                    val showQuickCardSortActions =
                        basePage == pageQuickCard &&
                                quickCardRoute == QuickCardRoutes.Sort &&
                                quickCardActions?.canConfirm == true
                    val showQuickCardWebActions =
                        basePage == pageQuickCard && quickCardRoute == QuickCardRoutes.Web
                    val showDrawingActions = basePage == pageDrawing
                    val showVoicePackActions = basePage == pageVoicePack
                    val showSettingsEntryActions = basePage == pageSettings && !settingsLogOpen
                    val showSettingsLogActions = basePage == pageSettings && settingsLogOpen
                    val settingsActions = logTopBarActions

                    val quickSubtitleAlpha by animateFloatAsState(
                        targetValue = if (showQuickSubtitleActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_quick_subtitle_actions_alpha"
                    )
                    val drawingAlpha by animateFloatAsState(
                        targetValue = if (showDrawingActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_drawing_actions_alpha"
                    )
                    val quickCardMainAlpha by animateFloatAsState(
                        targetValue = if (showQuickCardMainActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_quick_card_main_actions_alpha"
                    )
                    val quickCardEditorAlpha by animateFloatAsState(
                        targetValue = if (showQuickCardEditorActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_quick_card_editor_actions_alpha"
                    )
                    val quickCardSortAlpha by animateFloatAsState(
                        targetValue = if (showQuickCardSortActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_quick_card_sort_actions_alpha"
                    )
                    val quickCardWebAlpha by animateFloatAsState(
                        targetValue = if (showQuickCardWebActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_quick_card_web_actions_alpha"
                    )
                    val voicePackAlpha by animateFloatAsState(
                        targetValue = if (showVoicePackActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_voicepack_actions_alpha"
                    )
                    val settingsEntryAlpha by animateFloatAsState(
                        targetValue = if (showSettingsEntryActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_settings_entry_alpha"
                    )
                    val settingsLogAlpha by animateFloatAsState(
                        targetValue = if (showSettingsLogActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_settings_log_actions_alpha"
                    )
                    val actionsWidthTarget = when {
                        showSettingsLogActions -> 144.dp
                        showQuickCardMainActions || showQuickCardEditorActions -> 96.dp
                        showQuickCardSortActions -> 48.dp
                        showQuickCardWebActions -> 48.dp
                        showQuickSubtitleActions || showDrawingActions || showVoicePackActions || showSettingsEntryActions -> 48.dp
                        else -> 0.dp
                    }
                    val actionsWidth by animateDpAsState(
                        targetValue = actionsWidthTarget,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_actions_width"
                    )

                    Box(
                        modifier = Modifier.width(actionsWidth),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        key("topbar_quick_subtitle_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = quickSubtitleAlpha }
                                    .zIndex(if (showQuickSubtitleActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { quickSubtitleFullscreen = !quickSubtitleFullscreen },
                                    enabled = showQuickSubtitleActions
                                ) {
                                    MsIcon(
                                        name = if (quickSubtitleFullscreen) "fullscreen_exit" else "fullscreen",
                                        contentDescription = if (quickSubtitleFullscreen) "退出全屏" else "进入全屏"
                                    )
                                }
                            }
                        }

                        key("topbar_drawing_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = drawingAlpha }
                                    .zIndex(if (showDrawingActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { viewModel.saveDrawingSnapshot() },
                                    enabled = showDrawingActions && viewModel.drawStrokes.isNotEmpty()
                                ) {
                                    MsIcon("save", contentDescription = "保存画板")
                                }
                            }
                        }

                        key("topbar_quick_card_main_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = quickCardMainAlpha }
                                    .zIndex(if (showQuickCardMainActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { quickCardActions?.onNew?.invoke() },
                                    enabled = showQuickCardMainActions && quickCardActions != null
                                ) {
                                    MsIcon("add", contentDescription = "新建名片")
                                }
                                IconButton(
                                    onClick = { quickCardActions?.onScan?.invoke() },
                                    enabled = showQuickCardMainActions && quickCardActions != null
                                ) {
                                    MsIcon("qr_code_scanner", contentDescription = "扫描二维码")
                                }
                            }
                        }

                        key("topbar_quick_card_editor_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = quickCardEditorAlpha }
                                    .zIndex(if (showQuickCardEditorActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { quickCardActions?.onCopy?.invoke() },
                                    enabled = showQuickCardEditorActions && quickCardActions?.canCopy == true
                                ) {
                                    MsIcon("content_copy", contentDescription = "复制名片")
                                }
                                IconButton(
                                    onClick = { quickCardActions?.onDelete?.invoke() },
                                    enabled = showQuickCardEditorActions && quickCardActions?.canDelete == true
                                ) {
                                    MsIcon("delete", contentDescription = "删除名片")
                                }
                            }
                        }

                        key("topbar_quick_card_sort_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = quickCardSortAlpha }
                                    .zIndex(if (showQuickCardSortActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { quickCardActions?.onConfirm?.invoke() },
                                    enabled = showQuickCardSortActions
                                ) {
                                    MsIcon("check", contentDescription = "保存排序并返回")
                                }
                            }
                        }

                        key("topbar_quick_card_web_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = quickCardWebAlpha }
                                    .zIndex(if (showQuickCardWebActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box {
                                    IconButton(
                                        onClick = { quickCardWebMenuExpanded = true },
                                        enabled = showQuickCardWebActions
                                    ) {
                                        MsIcon("more_vert", contentDescription = "更多")
                                    }
                                    DropdownMenu(
                                        expanded = showQuickCardWebActions && quickCardWebMenuExpanded,
                                        onDismissRequest = { quickCardWebMenuExpanded = false }
                                    ) {
                                        M2DropdownMenuItem(
                                            onClick = {
                                                quickCardWebMenuExpanded = false
                                                if (quickCardWebUrl.isBlank()) {
                                                    toast(context, "链接为空")
                                                } else {
                                                    openQuickCardLink(context, quickCardWebUrl)
                                                }
                                            }
                                        ) {
                                            Text("用浏览器打开")
                                        }
                                        M2DropdownMenuItem(
                                            onClick = {
                                                quickCardWebMenuExpanded = false
                                                if (quickCardWebUrl.isBlank()) {
                                                    toast(context, "链接为空")
                                                } else {
                                                    clipboard.setText(AnnotatedString(quickCardWebUrl))
                                                    toast(context, "已复制链接")
                                                }
                                            }
                                        ) {
                                            Text("复制链接")
                                        }
                                        M2DropdownMenuItem(
                                            onClick = {
                                                quickCardWebMenuExpanded = false
                                                if (quickCardWebUrl.isBlank()) {
                                                    toast(context, "链接为空")
                                                } else {
                                                    sharePlainText(context, quickCardWebUrl, "分享链接")
                                                }
                                            }
                                        ) {
                                            Text("分享")
                                        }
                                    }
                                }
                            }
                        }

                        key("topbar_voicepack_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = voicePackAlpha }
                                    .zIndex(if (showVoicePackActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { voicePicker.launch("*/*") },
                                    enabled = showVoicePackActions
                                ) {
                                    MsIcon("folder_open", contentDescription = "导入语音包")
                                }
                            }
                        }

                        key("topbar_settings_log_entry_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = settingsEntryAlpha }
                                    .zIndex(if (showSettingsEntryActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { settingsNavController.navigate(SettingsRoutes.Log) },
                                    enabled = showSettingsEntryActions
                                ) {
                                    MsIcon("article", contentDescription = "打开日志")
                                }
                            }
                        }

                        key("topbar_settings_log_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = settingsLogAlpha }
                                    .zIndex(if (showSettingsLogActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { settingsActions?.onRefresh?.invoke() },
                                    enabled = showSettingsLogActions && settingsActions != null
                                ) {
                                    MsIcon("refresh", contentDescription = "刷新日志")
                                }
                                IconButton(
                                    onClick = { settingsActions?.onCopy?.invoke() },
                                    enabled = showSettingsLogActions && settingsActions?.canCopy == true
                                ) {
                                    MsIcon("content_copy", contentDescription = "复制日志")
                                }
                                IconButton(
                                    onClick = { settingsActions?.onShare?.invoke() },
                                    enabled = showSettingsLogActions && settingsActions?.canShare == true
                                ) {
                                    MsIcon("share", contentDescription = "分享日志")
                                }
                            }
                        }
                    }
                },
                backgroundColor = Color.Transparent,
                contentColor = topBarContentColor,
                elevation = 0.dp
            )
        }
    }

    val fab: @Composable () -> Unit = {}

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
                    pageOverlay -> FloatingOverlayScreen(
                        viewModel = viewModel,
                        state = state,
                        onOpenMainSettings = {
                            page = pageSettings
                            if (settingsRoute != SettingsRoutes.Main) {
                                settingsNavController.popBackStack(SettingsRoutes.Main, inclusive = false)
                            }
                        }
                    )
                    pageQuickSubtitle -> QuickSubtitleNavHost(
                        navController = quickSubtitleNavController,
                        viewModel = viewModel,
                        state = state,
                        onToggleMic = onToggleRun,
                        onPushToTalkPressStart = onPushToTalkPressStart,
                        onPushToTalkPressEnd = onPushToTalkPressEnd,
                        onOpenHistory = {
                            quickSubtitleNavController.navigate(QuickSubtitleRoutes.History) {
                                launchSingleTop = true
                            }
                        },
                        fullscreenMode = quickSubtitleFullscreen && !quickSubtitleSubPageOpen
                    )
                    pageQuickCard -> QuickCardNavHost(
                        navController = quickCardNavController,
                        viewModel = viewModel,
                        onNavReady = { quickCardNavReady = true },
                        onTopBarActionsChange = { quickCardTopBarActions = it }
                    )
                    pageVoicePack -> VoicePackScreen(viewModel, state)
                    pageDrawing -> DrawingBoardScreen(
                        viewModel = viewModel,
                        fullscreen = drawingFullscreen,
                        onToggleFullscreen = { drawingFullscreen = !drawingFullscreen }
                    )
                    pageSettings -> SettingsNavHost(
                        navController = settingsNavController,
                        viewModel = viewModel,
                        state = state,
                        onTopBarActionsChange = { logTopBarActions = it }
                    )
                }
            }
            AnimatedVisibility(
                visible = showRunningStripPanel,
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
                visible = showRunningStripPanel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .zIndex(2f),
                enter = fadeIn(animationSpec = tween(120)) +
                        slideInVertically(
                            initialOffsetY = { full -> -full },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(90)) +
                        slideOutVertically(
                            targetOffsetY = { full -> -full },
                            animationSpec = tween(170, easing = FastOutSlowInEasing)
                        )
            ) {
                RunningStatusTopStrip(
                    viewModel = viewModel,
                    status = state.status,
                    pushToTalkMode = state.pushToTalkMode,
                    pushToTalkPressed = state.pushToTalkPressed,
                    playbackGainPercent = state.playbackGainPercent,
                    preferredInputType = state.preferredInputType,
                    preferredOutputType = state.preferredOutputType,
                    inputDeviceLabel = state.inputDeviceLabel,
                    outputDeviceLabel = state.outputDeviceLabel,
                    onToggleCollapsed = { runningStripCollapsed = !runningStripCollapsed },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (realtimeConfirmOverlayEnabled) {
                QuickSubtitlePttConfirmOverlay(
                    visible = realtimeShowPttConfirmOverlay,
                    dragTarget = realtimePttDragTarget,
                    streamingText = state.pushToTalkStreamingText,
                    isLandscape = isLandscape,
                    compactPttSideButtonsMode = realtimeCompactPttSideButtonsMode,
                    showInputAction = false,
                    applyNavigationBarsPadding = false,
                    topRowBottomReservedOverride = realtimePttTopRowBottomReserved
                )

                AnimatedVisibility(
                    visible = realtimeShowPttConfirmOverlay,
                    modifier = Modifier
                        .zIndex(6.5f)
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                        .padding(
                            start = realtimePttStatusStripStartInset,
                            end = realtimePttStatusStripAnimatedEndInsetWithBleed,
                            bottom = realtimePttStatusStripBottomInset
                        ),
                    enter = fadeIn(animationSpec = tween(140)),
                    exit = fadeOut(animationSpec = tween(110))
                ) {
                    Box(
                        modifier = Modifier.padding(
                            start = realtimePttStatusStripOuterBleed,
                            top = realtimePttStatusStripTopBleed,
                            end = realtimePttStatusStripOuterBleed,
                            bottom = realtimePttStatusStripBottomBleed
                        )
                    ) {
                        QuickSubtitlePttConfirmBottomStrip(
                            guideText = realtimePttGuideText,
                            reserveFabWidth = realtimePttStripFabReserveWidth,
                            stripHeight = realtimePttFabSize
                        )
                    }
                }

                QuickSubtitlePttCompactSideButtonsOverlay(
                    visible = realtimeShowPttConfirmOverlay && realtimeCompactPttSideButtonsMode,
                    dragTarget = realtimePttDragTarget,
                    fabSize = realtimePttFabSize,
                    fabEndInset = realtimePttFabEndInset,
                    fabBottomOffset = realtimePttFabBottomOffset,
                    showInputAction = false,
                    applyNavigationBarsPadding = false
                )
            }
        }
    }

    val drawingImmersive = drawingFullscreen && basePage == pageDrawing
    val quickSubtitleImmersive =
        quickSubtitleFullscreen && basePage == pageQuickSubtitle && !quickSubtitleSubPageOpen
    val fullScreenImmersive = drawingImmersive || quickSubtitleImmersive
    BackHandler(enabled = drawingImmersive) {
        drawingFullscreen = false
    }
    BackHandler(enabled = quickSubtitleImmersive) {
        quickSubtitleFullscreen = false
    }
    LaunchedEffect(fullScreenImmersive) {
        if (fullScreenImmersive) {
            drawerExpanded = false
            drawerState.close()
        }
    }
    LaunchedEffect(drawingImmersive, inMultiWindowMode) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView) ?: return@LaunchedEffect
        if (drawingImmersive && !inMultiWindowMode) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
            AppLogger.i("AppScaffold.statusBars=hidden drawingImmersive=true")
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
            AppLogger.i("AppScaffold.statusBars=shown drawingImmersive=false")
        }
    }
    val topBarVisible = !fullScreenImmersive
    val animatedPermanentRailWidth by animateDpAsState(
        targetValue = if (topBarVisible) permanentDrawerCollapsedWidth else 0.dp,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "permanent_drawer_rail_width"
    )
    val animatedContentStartPadding by animateDpAsState(
        targetValue = if (fullScreenImmersive) landscapeCutoutStart else 0.dp,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "content_start_padding"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        if (usePermanentDrawer) {
            Scaffold(
                topBar = {
                    AnimatedVisibility(
                        visible = topBarVisible,
                        enter = fadeIn(animationSpec = tween(130)) +
                                expandVertically(
                                    expandFrom = Alignment.Top,
                                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                                ),
                        exit = fadeOut(animationSpec = tween(100)) +
                                shrinkVertically(
                                    shrinkTowards = Alignment.Top,
                                    animationSpec = tween(140, easing = FastOutSlowInEasing)
                                )
                    ) {
                        topBar { drawerExpanded = !drawerExpanded }
                    }
                },
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
                                .width(animatedPermanentRailWidth)
                                .fillMaxHeight()
                                .zIndex(3f),
                            shape = RectangleShape,
                            color = MaterialTheme.colorScheme.surface,
                            elevation = if (animatedPermanentRailWidth > 0.dp) UiTokens.MenuElevation else 0.dp
                        ) {
                            if (animatedPermanentRailWidth > 0.5.dp) {
                                AppDrawerContent(
                                    items = drawerItems,
                                    page = drawerSelectedPage,
                                    expanded = false,
                                    applyStatusBarPadding = false,
                                    showHeader = false,
                                    showTopDivider = false,
                                    topInset = 8.dp,
                                    horizontalStartInset = landscapeCutoutStart,
                                    onSelect = { page = it }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                        contentArea(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer { clip = true }
                                .zIndex(0f)
                                .padding(start = animatedContentStartPadding, end = landscapeCutoutEnd)
                        )
                    }

                    AnimatedVisibility(
                        visible = drawerExpanded && topBarVisible,
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
                                    .width(permanentDrawerExpandedWidth)
                                    .fillMaxHeight()
                                    .zIndex(4f),
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
                                    horizontalStartInset = landscapeCutoutStart,
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
                gesturesEnabled = basePage != pageDrawing &&
                        !state.pushToTalkPressed &&
                        !quickCardMainOpen,
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
                                .width(hiddenDrawerSurfaceWidth)
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
                                horizontalStartInset = landscapeCutoutStart,
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
                    topBar = {
                        AnimatedVisibility(
                            visible = topBarVisible,
                            enter = fadeIn(animationSpec = tween(130)) +
                                    expandVertically(
                                        expandFrom = Alignment.Top,
                                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                                    ),
                            exit = fadeOut(animationSpec = tween(100)) +
                                    shrinkVertically(
                                        shrinkTowards = Alignment.Top,
                                        animationSpec = tween(140, easing = FastOutSlowInEasing)
                                    )
                        ) {
                            topBar { scope.launch { drawerState.open() } }
                        }
                    },
                    floatingActionButton = fab,
                    backgroundColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        contentArea(
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(start = landscapeCutoutStart, end = landscapeCutoutEnd)
                        )
                    }
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
    horizontalStartInset: Dp = 0.dp,
    onSelect: (Int) -> Unit
) {
    val drawerLogoRes = if (isSystemInDarkTheme()) R.drawable.logo_white else R.drawable.logo_black
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
            .padding(start = horizontalStartInset)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(topInset))
        if (showHeader && expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = drawerLogoRes),
                    contentDescription = "KIGTTS",
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(32.dp)
                )
            }
        }
        if (showTopDivider) {
            Divider()
        }
        items.forEach { item ->
            val selected = page == item.page
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
                    ) { onSelect(item.page) }
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
    var detailPackPath by remember { mutableStateOf<String?>(null) }
    var detailName by remember { mutableStateOf("") }
    var detailRemark by remember { mutableStateOf("") }
    var detailEditing by remember { mutableStateOf(false) }
    var deletePack by remember { mutableStateOf<VoicePackInfo?>(null) }
    var avatarTarget by remember { mutableStateOf<VoicePackInfo?>(null) }
    val detailPack = detailPackPath?.let { path ->
        state.voicePacks.firstOrNull { it.dir.absolutePath == path }
    }

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
                activityTitle = "裁剪头像",
                cropMenuCropButtonTitle = "确认",
                activityMenuIconColor = 0xFFFFFFFF.toInt(),
                activityMenuTextColor = 0xFFFFFFFF.toInt(),
                activityBackgroundColor = 0xFF121212.toInt(),
                toolbarColor = 0xFF038387.toInt(),
                toolbarTitleColor = 0xFFFFFFFF.toInt(),
                toolbarBackButtonColor = 0xFFFFFFFF.toInt(),
                toolbarTintColor = 0xFFFFFFFF.toInt(),
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (state.voicePacks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(Modifier.height(UiTokens.PageTopBlank))
                    Text("暂无语音包，请点击主标题栏导入按钮。")
                    Spacer(Modifier.height(UiTokens.PageBottomBlank))
                }
            }
        } else {
            VoicePackRecyclerList(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                packs = state.voicePacks,
                currentVoicePath = state.voiceDir?.absolutePath,
                topBlankHeight = UiTokens.PageTopBlank,
                bottomBlankHeight = UiTokens.PageBottomBlank,
                onSelect = { viewModel.selectVoice(it.dir) },
                onTogglePin = { viewModel.toggleVoicePin(it) },
                onDetail = { pack ->
                    detailPackPath = pack.dir.absolutePath
                    detailName = pack.meta.name
                    detailRemark = pack.meta.remark
                    detailEditing = false
                },
                onShare = { viewModel.shareVoice(it) },
                onDelete = { deletePack = it },
                onReorder = { newOrder -> viewModel.reorderVoicePacks(newOrder) }
            )
        }
    }

    if (detailPack != null) {
        val avatarFile = remember(detailPack.dir.absolutePath, detailPack.meta.avatar) {
            File(detailPack.dir, detailPack.meta.avatar)
        }
        val avatarBitmap = rememberAvatarBitmap(avatarFile)
        AlertDialog(
            onDismissRequest = { detailPackPath = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("语音包详细信息", modifier = Modifier.weight(1f))
                    Md2IconButton(
                        icon = if (detailEditing) "check" else "edit",
                        contentDescription = if (detailEditing) "完成编辑" else "编辑",
                        onClick = { detailEditing = !detailEditing }
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (avatarBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(UiTokens.Radius))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(UiTokens.Radius))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("无头像", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (!detailEditing) {
                                Text("名称：${detailPack.meta.name}", style = MaterialTheme.typography.bodyMedium)
                                val remarkText = detailPack.meta.remark.ifBlank { "无" }
                                Text("备注：$remarkText", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("文件名：${detailPack.dir.name}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (detailEditing) {
                            Md2IconButton(
                                icon = "image",
                                contentDescription = "更换头像",
                                onClick = {
                                    avatarTarget = detailPack
                                    imagePicker.launch("image/*")
                                }
                            )
                        }
                    }
                    if (detailEditing) {
                        Md2OutlinedField(
                            value = detailName,
                            onValueChange = { detailName = it },
                            label = "名称"
                        )
                        Md2OutlinedField(
                            value = detailRemark,
                            onValueChange = { detailRemark = it },
                            label = "备注"
                        )
                    } else {
                        Text("文件名：${detailPack.dir.name}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (detailEditing) {
                    Md2TextButton(onClick = {
                        viewModel.updateVoiceMeta(detailPack, detailName, detailRemark)
                        detailEditing = false
                    }) {
                        Text("保存")
                    }
                }
            },
            dismissButton = {
                Md2TextButton(onClick = {
                    if (detailEditing) {
                        detailEditing = false
                        detailName = detailPack.meta.name
                        detailRemark = detailPack.meta.remark
                    } else {
                        detailPackPath = null
                    }
                }) {
                    Text(if (detailEditing) "取消编辑" else "关闭")
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

@Composable
private fun VoicePackRecyclerList(
    modifier: Modifier = Modifier,
    packs: List<VoicePackInfo>,
    currentVoicePath: String?,
    topBlankHeight: Dp,
    bottomBlankHeight: Dp,
    onSelect: (VoicePackInfo) -> Unit,
    onTogglePin: (VoicePackInfo) -> Unit,
    onDetail: (VoicePackInfo) -> Unit,
    onShare: (VoicePackInfo) -> Unit,
    onDelete: (VoicePackInfo) -> Unit,
    onReorder: (List<VoicePackInfo>) -> Unit
) {
    val parentComposition = rememberCompositionContext()
    val density = LocalDensity.current
    val topBlankPx = with(density) { topBlankHeight.roundToPx() }
    val bottomBlankPx = with(density) { bottomBlankHeight.roundToPx() }

    val onSelectState = rememberUpdatedState(onSelect)
    val onTogglePinState = rememberUpdatedState(onTogglePin)
    val onDetailState = rememberUpdatedState(onDetail)
    val onShareState = rememberUpdatedState(onShare)
    val onDeleteState = rememberUpdatedState(onDelete)
    val onReorderState = rememberUpdatedState(onReorder)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val recycler = RecyclerView(ctx).apply {
                layoutManager = object : LinearLayoutManager(ctx) {
                    override fun supportsPredictiveItemAnimations(): Boolean = false
                }
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                clipToPadding = false
                clipChildren = false
                itemAnimator = DefaultItemAnimator().apply {
                    supportsChangeAnimations = false
                    addDuration = 120L
                    removeDuration = 120L
                    moveDuration = 160L
                    changeDuration = 0L
                }
                setPadding(paddingLeft, topBlankPx, paddingRight, bottomBlankPx)
            }

            val adapter = VoicePackRecyclerAdapter(
                parentComposition = parentComposition,
                onSelect = { onSelectState.value(it) },
                onTogglePin = { onTogglePinState.value(it) },
                onDetail = { onDetailState.value(it) },
                onShare = { onShareState.value(it) },
                onDelete = { onDeleteState.value(it) }
            )
            recycler.adapter = adapter

            val touchCallback = object : ItemTouchHelper.Callback() {
                private var moved = false
                private var activeViewHolder: RecyclerView.ViewHolder? = null

                override fun isLongPressDragEnabled(): Boolean = false
                override fun isItemViewSwipeEnabled(): Boolean = false

                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    if (!adapter.isDraggableAdapterPosition(viewHolder.bindingAdapterPosition)) {
                        return makeMovementFlags(0, 0)
                    }
                    val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    return makeMovementFlags(dragFlags, 0)
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    val ok = adapter.moveWithinPinnedGroupAdapterPositions(from, to)
                    moved = moved || ok
                    return ok
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                        if (activeViewHolder !== viewHolder) {
                            activeViewHolder?.let { animateDragElevation(it.itemView, elevated = false) }
                            (activeViewHolder as? VoicePackRecyclerAdapter.VoicePackViewHolder)?.setDragged(false)
                        }
                        activeViewHolder = viewHolder
                        (viewHolder as? VoicePackRecyclerAdapter.VoicePackViewHolder)?.setDragged(true)
                        animateDragElevation(viewHolder.itemView, elevated = true)
                    } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        activeViewHolder?.let { animateDragElevation(it.itemView, elevated = false) }
                        (activeViewHolder as? VoicePackRecyclerAdapter.VoicePackViewHolder)?.setDragged(false)
                        activeViewHolder = null
                    }
                    // Keep drag-lock until clearView so stale state cannot overwrite moved order.
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        adapter.isDragging = true
                    }
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    animateDragElevation(viewHolder.itemView, elevated = false)
                    (viewHolder as? VoicePackRecyclerAdapter.VoicePackViewHolder)?.setDragged(false)
                    if (activeViewHolder === viewHolder) activeViewHolder = null
                    adapter.isDragging = false
                    if (moved) {
                        val snapshot = adapter.snapshot()
                        adapter.awaitExternalCommit(snapshot)
                        recyclerView.post { onReorderState.value(snapshot) }
                        moved = false
                    }
                }
            }
            val touchHelper = ItemTouchHelper(touchCallback)
            touchHelper.attachToRecyclerView(recycler)
            adapter.onStartDrag = { vh ->
                touchHelper.startDrag(vh)
            }
            recycler
        },
        update = { recycler ->
            val adapter = recycler.adapter as? VoicePackRecyclerAdapter ?: return@AndroidView
            val applyState = {
                recycler.setPadding(recycler.paddingLeft, topBlankPx, recycler.paddingRight, bottomBlankPx)
                adapter.updateCurrentVoicePath(currentVoicePath)
                adapter.submitFromState(packs)
            }
            // Always defer adapter updates to avoid dispatching notify* in an active layout pass.
            recycler.post(applyState)
        }
    )
}

private class VoicePackRecyclerAdapter(
    private val parentComposition: CompositionContext,
    private val onSelect: (VoicePackInfo) -> Unit,
    private val onTogglePin: (VoicePackInfo) -> Unit,
    private val onDetail: (VoicePackInfo) -> Unit,
    private val onShare: (VoicePackInfo) -> Unit,
    private val onDelete: (VoicePackInfo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<VoicePackInfo>()
    private val stagedAppearedIds = hashSetOf<Long>()
    private var runStaggerOnNextBind = true
    private var staggerReleaseScheduled = false
    private var nextStableId = 1L
    private val stableIdsByPath = hashMapOf<String, Long>()
    private var awaitingCommitPaths: List<String>? = null
    private var awaitingCommitDeadlineMs: Long = 0L
    var isDragging: Boolean = false
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    var currentVoicePath: String? = null
        private set

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        if (position !in items.indices) return RecyclerView.NO_ID
        return stableIdForPath(items[position].dir.absolutePath)
    }

    override fun getItemViewType(position: Int): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setParentCompositionContext(parentComposition)
        }
        return VoicePackViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as VoicePackViewHolder
        if (!isDragging) {
            holder.itemView.translationZ = 0f
        }
        val itemId = getItemId(position)
        val shouldStagger = runStaggerOnNextBind && !stagedAppearedIds.contains(itemId)
        if (shouldStagger) {
            stagedAppearedIds.add(itemId)
            val dataIndex = position.coerceAtLeast(0)
            animateVoicePackStaggerEnter(holder.itemView, dataIndex)
            if (!staggerReleaseScheduled) {
                staggerReleaseScheduled = true
                holder.itemView.postDelayed(
                    { runStaggerOnNextBind = false },
                    560L
                )
            }
        } else {
            holder.itemView.animate().cancel()
            holder.itemView.alpha = 1f
            holder.itemView.translationY = 0f
        }
        holder.setDragged(false)
        val dataIndex = position
        if (dataIndex !in items.indices) return
        val pack = items[dataIndex]
        holder.bind(
            pack = pack,
            isCurrent = currentVoicePath == pack.dir.absolutePath,
            onSelect = onSelect,
            onTogglePin = onTogglePin,
            onDetail = onDetail,
            onShare = onShare,
            onDelete = onDelete,
            onStartDrag = {
                if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onStartDrag?.invoke(holder)
                }
            }
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is VoicePackViewHolder) {
            holder.setDragged(false)
        }
        holder.itemView.translationZ = 0f
        holder.itemView.alpha = 1f
        holder.itemView.translationY = 0f
        super.onViewRecycled(holder)
    }

    fun submitFromState(newItems: List<VoicePackInfo>) {
        if (isDragging) {
            // During drag, ignore external list pushes.
            // Final order is committed by onReorder callback on clearView.
            return
        }
        awaitingCommitPaths?.let { pending ->
            val incoming = newItems.map { it.dir.absolutePath }
            when {
                incoming == pending -> {
                    awaitingCommitPaths = null
                    awaitingCommitDeadlineMs = 0L
                }
                SystemClock.uptimeMillis() < awaitingCommitDeadlineMs -> {
                    // Drop stale state while waiting for committed order from ViewModel.
                    return
                }
                else -> {
                    awaitingCommitPaths = null
                    awaitingCommitDeadlineMs = 0L
                }
            }
        }
        if (items == newItems) return
        val shouldRunStagger = items.isEmpty() && newItems.isNotEmpty()
        if (shouldRunStagger) {
            runStaggerOnNextBind = true
            staggerReleaseScheduled = false
            stagedAppearedIds.clear()
        }
        val oldItems = items.toList()
        val oldCount = oldItems.size
        val newCount = newItems.size
        val newPaths = newItems.asSequence().map { it.dir.absolutePath }.toHashSet()
        stableIdsByPath.keys.retainAll(newPaths)

        items.clear()
        items.addAll(newItems)

        if (oldCount == 0 || newCount == 0) {
            notifyDataSetChanged()
            return
        }

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition].dir.absolutePath == newItems[newItemPosition].dir.absolutePath
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = oldItems[oldItemPosition]
                val new = newItems[newItemPosition]
                if (old.dir.absolutePath != new.dir.absolutePath) return false
                // Ignore "order" in content comparison: it is persistence metadata and
                // can change after drag commit without any visible row content change.
                return old.meta.name == new.meta.name &&
                        old.meta.remark == new.meta.remark &&
                        old.meta.avatar == new.meta.avatar &&
                        old.meta.pinned == new.meta.pinned
            }
        })
        diff.dispatchUpdatesTo(this)
    }

    fun updateCurrentVoicePath(path: String?) {
        if (currentVoicePath == path) return
        val oldPath = currentVoicePath
        currentVoicePath = path
        if (isDragging) return
        val oldIdx = oldPath?.let { p -> items.indexOfFirst { it.dir.absolutePath == p } } ?: -1
        val newIdx = path?.let { p -> items.indexOfFirst { it.dir.absolutePath == p } } ?: -1
        if (oldIdx >= 0) notifyItemChanged(oldIdx)
        if (newIdx >= 0 && newIdx != oldIdx) notifyItemChanged(newIdx)
    }

    fun snapshot(): List<VoicePackInfo> = items.toList()

    fun awaitExternalCommit(snapshot: List<VoicePackInfo>) {
        awaitingCommitPaths = snapshot.map { it.dir.absolutePath }
        awaitingCommitDeadlineMs = SystemClock.uptimeMillis() + 1800L
    }

    fun isDraggableAdapterPosition(position: Int): Boolean {
        if (position == RecyclerView.NO_POSITION) return false
        return position in items.indices
    }

    fun moveWithinPinnedGroupAdapterPositions(fromAdapter: Int, toAdapter: Int): Boolean {
        if (!isDraggableAdapterPosition(fromAdapter) || !isDraggableAdapterPosition(toAdapter)) {
            return false
        }
        val from = fromAdapter
        val to = toAdapter
        if (from == to || from !in items.indices || to !in items.indices) return false
        val fromPinned = items[from].meta.pinned
        val toPinned = items[to].meta.pinned
        if (fromPinned != toPinned) return false
        items.move(from, to)
        notifyItemMoved(fromAdapter, toAdapter)
        return true
    }

    private fun stableIdForPath(path: String): Long {
        return stableIdsByPath.getOrPut(path) { nextStableId++ }
    }

    class VoicePackViewHolder(
        private val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView) {
        private val draggedState = mutableStateOf(false)

        fun setDragged(dragged: Boolean) {
            draggedState.value = dragged
        }

        fun bind(
            pack: VoicePackInfo,
            isCurrent: Boolean,
            onSelect: (VoicePackInfo) -> Unit,
            onTogglePin: (VoicePackInfo) -> Unit,
            onDetail: (VoicePackInfo) -> Unit,
            onShare: (VoicePackInfo) -> Unit,
            onDelete: (VoicePackInfo) -> Unit,
            onStartDrag: () -> Unit
        ) {
            composeView.setContent {
                VoicePackCardContent(
                    pack = pack,
                    isCurrent = isCurrent,
                    isDragged = draggedState.value,
                    onSelect = { onSelect(pack) },
                    onTogglePin = { onTogglePin(pack) },
                    onDetail = { onDetail(pack) },
                    onShare = { onShare(pack) },
                    onDelete = { onDelete(pack) },
                    onStartDrag = onStartDrag
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun VoicePackCardContent(
    pack: VoicePackInfo,
    isCurrent: Boolean,
    isDragged: Boolean,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onDetail: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onStartDrag: () -> Unit
) {
    val avatarFile = File(pack.dir, pack.meta.avatar)
    val avatarBitmap = rememberAvatarBitmap(avatarFile)
    val cardElevation by animateDpAsState(
        targetValue = if (isDragged) 10.dp else UiTokens.CardElevation,
        animationSpec = tween(
            durationMillis = if (isDragged) 120 else 160,
            easing = FastOutSlowInEasing
        ),
        label = "voice_pack_card_elevation"
    )

    Box(modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)) {
        Card(
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = cardElevation
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
                    }
                    Md2IconButton(
                        icon = "drag_indicator",
                        contentDescription = "按住拖动排序",
                        onClick = {},
                        modifier = Modifier.pointerInteropFilter { ev ->
                            when (ev.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    onStartDrag()
                                    true
                                }
                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL,
                                MotionEvent.ACTION_MOVE -> true
                                else -> false
                            }
                        }
                    )
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
                        onClick = onSelect,
                        enabled = !isCurrent
                    )
                    Md2IconButton(
                        icon = if (pack.meta.pinned) "keep_off" else "push_pin",
                        contentDescription = if (pack.meta.pinned) "取消置顶" else "置顶",
                        onClick = onTogglePin
                    )
                    Md2IconButton(
                        icon = "info",
                        contentDescription = "语音包详细信息",
                        onClick = onDetail
                    )
                    Md2IconButton(
                        icon = "share",
                        contentDescription = "分享语音包",
                        onClick = onShare
                    )
                    Md2IconButton(
                        icon = "delete",
                        contentDescription = "删除语音包",
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

private fun MutableList<VoicePackInfo>.move(from: Int, to: Int) {
    if (from == to || from !in indices || to !in indices) return
    val item = removeAt(from)
    add(to, item)
}

private fun stablePathId64(path: String): Long {
    // 64-bit FNV-1a hash to reduce stable-id collisions on long paths.
    var h = -3750763034362895579L
    path.forEach { ch ->
        h = h xor ch.code.toLong()
        h *= 1099511628211L
    }
    return h
}

private fun animateDragElevation(view: View, elevated: Boolean) {
    val targetZ = if (elevated) 12f * view.resources.displayMetrics.density else 0f
    val duration = if (elevated) 120L else 160L
    view.animate()
        .cancel()
    view.animate()
        .translationZ(targetZ)
        .setDuration(duration)
        .setInterpolator(FastOutSlowInInterpolator())
        .start()
}

private fun animateVoicePackStaggerEnter(view: View, position: Int) {
    val density = view.resources.displayMetrics.density
    val offsetY = 12f * density
    val delayMs = (position.coerceIn(0, 10) * 36L)
    view.animate().cancel()
    view.alpha = 0f
    view.translationY = offsetY
    view.animate()
        .alpha(1f)
        .translationY(0f)
        .setStartDelay(delayMs)
        .setDuration(220L)
        .setInterpolator(FastOutSlowInInterpolator())
        .start()
}

@Composable
private fun QuickSubtitleNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    state: UiState,
    onToggleMic: () -> Unit,
    onPushToTalkPressStart: () -> Unit,
    onPushToTalkPressEnd: (PttConfirmReleaseAction) -> Unit,
    onOpenHistory: () -> Unit,
    fullscreenMode: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = QuickSubtitleRoutes.Main,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            if (initialState.destination.route == QuickSubtitleRoutes.Main &&
                targetState.destination.route == QuickSubtitleRoutes.Editor
            ) {
                fadeIn(animationSpec = tween(180)) +
                        androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { full -> full / 10 },
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeIn(animationSpec = tween(120))
            }
        },
        exitTransition = {
            if (initialState.destination.route == QuickSubtitleRoutes.Main &&
                targetState.destination.route == QuickSubtitleRoutes.Editor
            ) {
                fadeOut(animationSpec = tween(130)) +
                        androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { full -> -full / 14 },
                            animationSpec = tween(130, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeOut(animationSpec = tween(90))
            }
        },
        popEnterTransition = {
            if (initialState.destination.route == QuickSubtitleRoutes.Editor &&
                targetState.destination.route == QuickSubtitleRoutes.Main
            ) {
                fadeIn(animationSpec = tween(170)) +
                        androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { full -> -full / 12 },
                            animationSpec = tween(170, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeIn(animationSpec = tween(120))
            }
        },
        popExitTransition = {
            if (initialState.destination.route == QuickSubtitleRoutes.Editor &&
                targetState.destination.route == QuickSubtitleRoutes.Main
            ) {
                fadeOut(animationSpec = tween(130)) +
                        androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { full -> full / 16 },
                            animationSpec = tween(130, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeOut(animationSpec = tween(90))
            }
        }
    ) {
        composable(QuickSubtitleRoutes.Main) {
            QuickSubtitleScreen(
                viewModel = viewModel,
                state = state,
                onToggleMic = onToggleMic,
                onPushToTalkPressStart = onPushToTalkPressStart,
                onPushToTalkPressEnd = onPushToTalkPressEnd,
                onOpenHistory = onOpenHistory,
                onOpenEditor = { navController.navigate(QuickSubtitleRoutes.Editor) },
                fullscreenMode = fullscreenMode
            )
        }
        composable(QuickSubtitleRoutes.Editor) {
            QuickSubtitleEditorScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(QuickSubtitleRoutes.History) {
            RealtimeScreen(viewModel)
        }
    }
}

private const val QUICK_CARD_ASPECT_PORTRAIT = 0.52f
private const val QUICK_CARD_ASPECT_LANDSCAPE = 2.75f
private const val QUICK_CARD_LANDSCAPE_CARD_WIDTH_FRACTION = 0.94f
private const val QUICK_CARD_CONTENT_ASPECT_PORTRAIT = 1f / 1.67f
private const val QUICK_CARD_CONTENT_ASPECT_LANDSCAPE = 1.67f

private fun quickCardThemeColor(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { UiTokens.Primary }
}

private fun quickCardThemeOnColor(bg: Color): Color {
    return if (bg.luminance() > 0.56f) Color(0xFF111417) else Color.White
}

private fun QuickCard.heroImagePath(landscape: Boolean): String {
    return if (landscape) {
        landscapeImagePath.ifBlank { portraitImagePath }
    } else {
        portraitImagePath.ifBlank { landscapeImagePath }
    }
}

private fun QuickCardDraft.toPreviewCard(): QuickCard {
    return QuickCard(
        id = editId ?: -1L,
        type = type,
        title = title.ifBlank { "名片名字" },
        note = note,
        themeColor = themeColor,
        link = link,
        portraitImagePath = portraitImagePath,
        landscapeImagePath = landscapeImagePath
    )
}

private fun buildQuickCardShareText(card: QuickCard): String {
    return buildString {
        append(card.title.ifBlank { "名片" })
        if (card.note.isNotBlank()) append("\n${card.note}")
        if (card.link.isNotBlank()) append("\n${card.link}")
    }
}

private fun openQuickCardLink(context: Context, rawLink: String) {
    val normalized = normalizeQrTextToWebUrl(rawLink)
    if (normalized.isNullOrBlank()) {
        toast(context, "链接无效")
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
        context.startActivity(intent)
    } catch (e: Exception) {
        toast(context, "打开链接失败: ${e.message}")
    }
}

private fun shareQuickCard(context: Context, card: QuickCard, landscape: Boolean) {
    try {
        val shareText = buildQuickCardShareText(card)
        if (card.type == QuickCardType.Image) {
            val imagePath = card.heroImagePath(landscape)
            val source = if (imagePath.isBlank()) null else File(imagePath)
            if (source != null && source.exists()) {
                val shareDir = File(context.cacheDir, "share")
                if (!shareDir.exists()) shareDir.mkdirs()
                val out = File(shareDir, "quick_card_${System.currentTimeMillis()}.png")
                source.copyTo(out, overwrite = true)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    out
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "分享名片"))
                return
            }
        }
        val textIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(textIntent, "分享名片"))
    } catch (e: Exception) {
        toast(context, "分享失败: ${e.message}")
    }
}

private fun generateQuickCardQrBitmap(content: String, sizePx: Int = 640): Bitmap? {
    return QuickCardRenderCache.loadQr(content, sizePx)
}

@Composable
private fun rememberQuickCardBitmap(path: String): Bitmap? {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            QuickCardRenderCache.loadImage(path)
        }
    }
    return bitmap
}

@Composable
private fun rememberQuickCardQrBitmap(content: String): Bitmap? {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = content) {
        value = withContext(Dispatchers.Default) {
            QuickCardRenderCache.loadQr(content)
        }
    }
    return bitmap
}

@Composable
private fun SettingsNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    state: UiState,
    onTopBarActionsChange: (LogTopBarActions?) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = SettingsRoutes.Main,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            if (initialState.destination.route == SettingsRoutes.Main &&
                targetState.destination.route == SettingsRoutes.Log
            ) {
                fadeIn(animationSpec = tween(170)) +
                        androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { full -> full / 12 },
                            animationSpec = tween(170, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeIn(animationSpec = tween(120))
            }
        },
        exitTransition = {
            if (initialState.destination.route == SettingsRoutes.Main &&
                targetState.destination.route == SettingsRoutes.Log
            ) {
                fadeOut(animationSpec = tween(120)) +
                        androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { full -> -full / 14 },
                            animationSpec = tween(120, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeOut(animationSpec = tween(90))
            }
        },
        popEnterTransition = {
            if (initialState.destination.route == SettingsRoutes.Log &&
                targetState.destination.route == SettingsRoutes.Main
            ) {
                fadeIn(animationSpec = tween(150)) +
                        androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { full -> -full / 12 },
                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeIn(animationSpec = tween(120))
            }
        },
        popExitTransition = {
            if (initialState.destination.route == SettingsRoutes.Log &&
                targetState.destination.route == SettingsRoutes.Main
            ) {
                fadeOut(animationSpec = tween(120)) +
                        androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { full -> full / 16 },
                            animationSpec = tween(120, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeOut(animationSpec = tween(90))
            }
        }
    ) {
        composable(SettingsRoutes.Main) {
            SettingsScreen(viewModel, state)
        }
        composable(SettingsRoutes.Log) {
            LogScreen(onTopBarActionsChange = onTopBarActionsChange)
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun QuickSubtitleMicFab(
    state: UiState,
    compactPttSideButtonsMode: Boolean = false,
    enableInputAction: Boolean = true,
    onToggleMic: () -> Unit,
    onPushToTalkPressStart: () -> Unit,
    onPushToTalkPressEnd: (PttConfirmReleaseAction) -> Unit,
    onPttDragTargetChanged: (PttConfirmDragTarget) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pushToTalkPressed by remember(state.pushToTalkMode) { mutableStateOf(false) }
    val pttInteractionSource = remember { MutableInteractionSource() }
    var pttPress by remember { mutableStateOf<PressInteraction.Press?>(null) }
    val density = LocalDensity.current
    // Zone split (matching the 3-zone behavior):
    // 1) Bottom zone: release => send to subtitle
    // 2) Top-left large zone: release => send to input box
    // 3) Top-right narrow zone: release => cancel
    val topZoneTriggerPx = remember(density) { with(density) { 56.dp.toPx() } }
    val cancelZoneLeftBiasPx = remember(density) { with(density) { 12.dp.toPx() } }
    val sideZoneTriggerPx = remember(density) { with(density) { 52.dp.toPx() } }
    val sideZoneVerticalTolerancePx = remember(density) { with(density) { 56.dp.toPx() } }
    var downX by remember { mutableFloatStateOf(0f) }
    var downY by remember { mutableFloatStateOf(0f) }
    var dragTarget by remember { mutableStateOf(PttConfirmDragTarget.DefaultSend) }
    fun resolveDragTarget(x: Float, y: Float): PttConfirmDragTarget {
        if (!state.pushToTalkConfirmInputMode) return PttConfirmDragTarget.DefaultSend
        val dx = x - downX
        val dy = y - downY
        if (compactPttSideButtonsMode) {
            val inSideBand = kotlin.math.abs(dy) <= sideZoneVerticalTolerancePx
            return when {
                inSideBand && dx >= sideZoneTriggerPx -> PttConfirmDragTarget.Cancel
                enableInputAction && inSideBand && dx <= -sideZoneTriggerPx -> PttConfirmDragTarget.ToInput
                else -> PttConfirmDragTarget.DefaultSend
            }
        }
        if (dy >= -topZoneTriggerPx) {
            return PttConfirmDragTarget.DefaultSend
        }
        // In top area:
        // - a narrow band near the right side (and around cancel button's left side) => cancel
        // - all remaining left area => send to input box
        return if (dx >= -cancelZoneLeftBiasPx) {
            PttConfirmDragTarget.Cancel
        } else if (enableInputAction) {
            PttConfirmDragTarget.ToInput
        } else {
            PttConfirmDragTarget.DefaultSend
        }
    }
    val pttModifier = if (state.pushToTalkMode) {
        modifier
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        pushToTalkPressed = true
                        downX = event.x
                        downY = event.y
                        dragTarget = PttConfirmDragTarget.DefaultSend
                        onPttDragTargetChanged(PttConfirmDragTarget.DefaultSend)
                        onPushToTalkPressStart()
                        val press = PressInteraction.Press(Offset(event.x, event.y))
                        pttPress = press
                        pttInteractionSource.tryEmit(press)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val nextTarget = resolveDragTarget(event.x, event.y)
                        if (nextTarget != dragTarget) {
                            dragTarget = nextTarget
                            onPttDragTargetChanged(nextTarget)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val nextTarget = resolveDragTarget(event.x, event.y)
                        if (nextTarget != dragTarget) {
                            dragTarget = nextTarget
                            onPttDragTargetChanged(nextTarget)
                        }
                        if (pushToTalkPressed) {
                            pushToTalkPressed = false
                            val releaseAction = when (dragTarget) {
                                PttConfirmDragTarget.DefaultSend -> PttConfirmReleaseAction.SendToSubtitle
                                PttConfirmDragTarget.ToInput -> {
                                    if (enableInputAction) PttConfirmReleaseAction.SendToInput
                                    else PttConfirmReleaseAction.SendToSubtitle
                                }
                                PttConfirmDragTarget.Cancel -> PttConfirmReleaseAction.Cancel
                            }
                            onPushToTalkPressEnd(releaseAction)
                        }
                        dragTarget = PttConfirmDragTarget.DefaultSend
                        onPttDragTargetChanged(PttConfirmDragTarget.DefaultSend)
                        pttPress?.let { press ->
                            pttInteractionSource.tryEmit(PressInteraction.Release(press))
                        }
                        pttPress = null
                        true
                    }
                    MotionEvent.ACTION_CANCEL,
                    MotionEvent.ACTION_OUTSIDE -> {
                        if (pushToTalkPressed) {
                            pushToTalkPressed = false
                            onPushToTalkPressEnd(PttConfirmReleaseAction.Cancel)
                        }
                        dragTarget = PttConfirmDragTarget.DefaultSend
                        onPttDragTargetChanged(PttConfirmDragTarget.DefaultSend)
                        pttPress?.let { press ->
                            pttInteractionSource.tryEmit(PressInteraction.Cancel(press))
                        }
                        pttPress = null
                        true
                    }
                    else -> true
                }
            }
    } else {
        modifier
    }
    FloatingActionButton(
        onClick = if (state.pushToTalkMode) ({}) else onToggleMic,
        modifier = pttModifier,
        interactionSource = pttInteractionSource,
        backgroundColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = UiTokens.FabElevation,
            pressedElevation = 12.dp
        )
    ) {
        if (state.pushToTalkMode) {
            Crossfade(
                targetState = pushToTalkPressed,
                animationSpec = tween(durationMillis = 180),
                label = "quick_subtitle_ptt_fab_icon"
            ) { pressed ->
                MsIcon(
                    name = if (pressed) "settings_voice" else "mic",
                    contentDescription = if (pressed) "按住说话中" else "按住说话",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            MsIcon(
                name = if (state.running) "stop" else "play_arrow",
                contentDescription = if (state.running) "关闭麦克风" else "开启麦克风",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun QuickSubtitleScreen(
    viewModel: MainViewModel,
    state: UiState,
    onToggleMic: () -> Unit,
    onPushToTalkPressStart: () -> Unit,
    onPushToTalkPressEnd: (PttConfirmReleaseAction) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenEditor: () -> Unit,
    fullscreenMode: Boolean
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val groups = viewModel.quickSubtitleGroups
    val selectedGroupIndex = viewModel.currentQuickSubtitleGroupIndex().coerceIn(0, groups.lastIndex.coerceAtLeast(0))
    val selectedGroup = groups.getOrNull(selectedGroupIndex)
    val quickItems = selectedGroup?.items ?: emptyList()
    val quickItemsScrollState = rememberScrollState()
    val subtitleText = viewModel.quickSubtitleCurrentText
    val subtitleSize = viewModel.quickSubtitleFontSizeSp
    val subtitleBold = viewModel.quickSubtitleBold
    val subtitleCentered = viewModel.quickSubtitleCentered
    val subtitleTextColor = if (subtitleText == QUICK_SUBTITLE_CLEARED_HINT) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleAlign = if (subtitleCentered) TextAlign.Center else TextAlign.Start
    val inputText = viewModel.quickSubtitleInputText
    val playOnSend = viewModel.quickSubtitlePlayOnSend
    val quickInputCollapsed = viewModel.quickSubtitleInputCollapsed
    val subtitleFullscreenDialogVisible = viewModel.quickSubtitlePreviewVisible
    val copySubtitleText = {
        val content = subtitleText.trim()
        if (content.isNotEmpty()) {
            clipboard.setText(AnnotatedString(content))
            toast(context, "已复制")
        }
    }
    val orangeActionButtons: @Composable (Boolean) -> Unit = { vertical ->
        if (vertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Md2IconButton(
                    icon = "format_bold",
                    contentDescription = if (subtitleBold) "关闭粗体" else "开启粗体",
                    onClick = { viewModel.updateQuickSubtitleBold(!subtitleBold) }
                )
                Md2IconButton(
                    icon = if (subtitleCentered) "format_align_left" else "format_align_center",
                    contentDescription = if (subtitleCentered) "左对齐文本" else "居中文本",
                    onClick = { viewModel.updateQuickSubtitleCentered(!subtitleCentered) }
                )
                Md2IconButton(
                    icon = "cleaning_services",
                    contentDescription = "清屏",
                    onClick = { viewModel.clearQuickSubtitleText() }
                )
                Md2IconButton(
                    icon = "history",
                    contentDescription = "历史记录",
                    onClick = onOpenHistory
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Md2IconButton(
                    icon = "format_bold",
                    contentDescription = if (subtitleBold) "关闭粗体" else "开启粗体",
                    onClick = { viewModel.updateQuickSubtitleBold(!subtitleBold) }
                )
                Md2IconButton(
                    icon = if (subtitleCentered) "format_align_left" else "format_align_center",
                    contentDescription = if (subtitleCentered) "左对齐文本" else "居中文本",
                    onClick = { viewModel.updateQuickSubtitleCentered(!subtitleCentered) }
                )
                Md2IconButton(
                    icon = "cleaning_services",
                    contentDescription = "清屏",
                    onClick = { viewModel.clearQuickSubtitleText() }
                )
                Md2IconButton(
                    icon = "history",
                    contentDescription = "历史记录",
                    onClick = onOpenHistory
                )
            }
        }
    }
    var pttDragTarget by remember { mutableStateOf(PttConfirmDragTarget.DefaultSend) }
    val showPttConfirmOverlay =
        state.pushToTalkMode &&
        state.pushToTalkConfirmInputMode &&
        state.pushToTalkPressed
    val useFloatingFabPortrait = !isLandscape
    val useFloatingFabLandscapeOverlay =
        isLandscape &&
        state.pushToTalkMode &&
        state.pushToTalkConfirmInputMode
    val useOverlayFab = useFloatingFabPortrait || useFloatingFabLandscapeOverlay
    val pttFabSize = if (isLandscape) 48.dp else 56.dp
    val pttFabEndInset = if (isLandscape) 64.dp else 20.dp
    val pttOverlayBottomOffset = if (isLandscape) 0.dp else 80.dp
    val pttFabBottomOffset = if (isLandscape) 12.dp else pttOverlayBottomOffset
    val pttStatusStripBottomOffset = pttFabBottomOffset
    val pttStatusStripBottomBleed = if (isLandscape) 12.dp else 14.dp
    val compactModeDetectionEnabled =
        isLandscape && state.pushToTalkMode && state.pushToTalkConfirmInputMode
    val imeBottomInset =
        if (compactModeDetectionEnabled) WindowInsets.ime.asPaddingValues().calculateBottomPadding() else 0.dp
    val navBottomInset =
        if (compactModeDetectionEnabled) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp
    val bottomObstructionInset = if (imeBottomInset > navBottomInset) imeBottomInset else navBottomInset
    val imeVisible = imeBottomInset > 0.dp
    val pttPendingText = state.pushToTalkStreamingText.ifBlank { "正在识别..." }
    val pttTopButtonsRequiredHeight = 96.dp
    val pttTopEstimatedAvailableHeight =
        configuration.screenHeightDp.dp - bottomObstructionInset -
            (pttFabSize + pttFabBottomOffset + pttStatusStripBottomBleed + 72.dp)
    val compactPttSideButtonsMode =
        compactModeDetectionEnabled && (imeVisible || pttTopEstimatedAvailableHeight < pttTopButtonsRequiredHeight)
    val pttGuideText = when (pttDragTarget) {
        PttConfirmDragTarget.DefaultSend ->
            if (compactPttSideButtonsMode) pttPendingText else "松开手指上屏"
        PttConfirmDragTarget.ToInput -> "松开输入到文本框"
        PttConfirmDragTarget.Cancel -> "松开取消发送"
    }
    val pttStripFabReserveWidth = if (useOverlayFab) pttFabSize else 0.dp
    val pttStatusStripEndInset = if (useOverlayFab) pttFabEndInset else 10.dp
    val pttStatusStripAnchorEndInset = pttStatusStripEndInset + (pttFabSize / 2)
    val pttStatusStripOuterBleed = 12.dp
    val pttStatusStripAnimatedEndInset by animateDpAsState(
        targetValue = if (showPttConfirmOverlay) pttStatusStripEndInset else pttStatusStripAnchorEndInset,
        animationSpec = if (showPttConfirmOverlay) {
            tween(durationMillis = 220, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 180, easing = FastOutSlowInEasing)
        },
        label = "ptt_status_strip_end_inset"
    )
    val pttStatusStripStartInset = (10.dp - pttStatusStripOuterBleed).coerceAtLeast(0.dp)
    val pttStatusStripTopBleed = (pttStatusStripOuterBleed - 4.dp).coerceAtLeast(0.dp)
    val pttStatusStripAnimatedEndInsetWithBleed =
        (pttStatusStripAnimatedEndInset - pttStatusStripOuterBleed).coerceAtLeast(0.dp)
    val pttStatusStripBottomInset = (pttStatusStripBottomOffset - pttStatusStripBottomBleed).coerceAtLeast(0.dp)
    LaunchedEffect(showPttConfirmOverlay) {
        if (!showPttConfirmOverlay) {
            pttDragTarget = PttConfirmDragTarget.DefaultSend
        }
    }
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
    val statusBarInsetTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val layoutDirection = LocalLayoutDirection.current
    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val navBarsStartInset = navBarsPadding.calculateStartPadding(layoutDirection)
    val navBarsEndInset = navBarsPadding.calculateEndPadding(layoutDirection)
    val navBarsBottomInset = navBarsPadding.calculateBottomPadding()
    val quickSubtitleTopBlankTarget =
        if (fullscreenMode) (statusBarInsetTop + UiTokens.PageTopBlank) else UiTokens.PageTopBlank
    val quickSubtitleTopBlank by animateDpAsState(
        targetValue = quickSubtitleTopBlankTarget,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "quick_subtitle_top_blank"
    )
    val landscapeQuickPanelWidth = 220.dp
    val landscapeQuickPanelGap = 8.dp
    val quickSubtitleBottomBlank = if (isLandscape) {
        UiTokens.PageBottomBlank - 12.dp + navBarsBottomInset
    } else {
        UiTokens.PageBottomBlank + 50.dp + navBarsBottomInset
    }
    val quickPanelExpanded = !quickInputCollapsed
    val quickPanelAnimatedWidth by animateDpAsState(
        targetValue = if (isLandscape && quickPanelExpanded) landscapeQuickPanelWidth else 0.dp,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "quick_subtitle_right_panel_width"
    )
    val quickPanelAnimatedGap by animateDpAsState(
        targetValue = if (isLandscape && quickPanelExpanded) landscapeQuickPanelGap else 0.dp,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "quick_subtitle_right_panel_gap"
    )
    val quickPanelAnimatedAlpha by animateFloatAsState(
        targetValue = if (isLandscape && quickPanelExpanded) 1f else 0f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "quick_subtitle_right_panel_alpha"
    )
    DisposableEffect(lifecycleOwner, focusManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                focusManager.clearFocus(force = true)
                AppLogger.i("QuickSubtitleScreen.onPause clearFocus")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = navBarsStartInset, end = navBarsEndInset)
        ) {
            Spacer(Modifier.height(quickSubtitleTopBlank))
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 260.dp),
                    horizontalArrangement = Arrangement.spacedBy(quickPanelAnimatedGap)
                ) {
                    Md2StaggeredFloatIn(
                        index = 0,
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(UiTokens.Radius),
                            backgroundColor = md2CardContainerColor(),
                            elevation = UiTokens.CardElevation
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .combinedClickable(
                                            onClick = { viewModel.openQuickSubtitlePreview() },
                                            onLongClick = copySubtitleText
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        AnimatedContent(
                                            targetState = subtitleText,
                                            transitionSpec = {
                                                ContentTransform(
                                                    targetContentEnter = fadeIn(animationSpec = tween(180)) +
                                                        slideInVertically(
                                                            initialOffsetY = { full -> full / 6 },
                                                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                                                        ),
                                                    initialContentExit = fadeOut(animationSpec = tween(120)),
                                                    sizeTransform = null
                                                )
                                            },
                                            label = "quick_subtitle_text_change"
                                        ) { text ->
                                            val textColor = if (text == QUICK_SUBTITLE_CLEARED_HINT) {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                            Text(
                                                text = text,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = if (subtitleBold) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = subtitleSize.sp,
                                                    lineHeight = (subtitleSize * 1.15f).sp
                                                ),
                                                color = textColor,
                                                textAlign = subtitleAlign,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .width(38.dp)
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Top
                                    ) {
                                        Spacer(Modifier.height(4.dp))
                                        orangeActionButtons(true)
                                    }
                                    Column(
                                        modifier = Modifier
                                            .width(32.dp)
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        MsIcon("search", contentDescription = "字体大小")
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp, bottom = 4.dp)
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Md2VerticalSlider(
                                                value = subtitleSize,
                                                onValueChange = { viewModel.setQuickSubtitleFontSize(it) },
                                                valueRange = 28f..96f,
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(quickPanelAnimatedWidth)
                            .fillMaxHeight()
                            .graphicsLayer { alpha = quickPanelAnimatedAlpha }
                    ) {
                        Md2StaggeredFloatIn(
                            index = 1,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(UiTokens.Radius),
                                backgroundColor = md2CardContainerColor(),
                                elevation = UiTokens.CardElevation
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp)
                                    ) {
                                        AnimatedContent(
                                            targetState = selectedGroupIndex,
                                            transitionSpec = {
                                                val forward = targetState >= initialState
                                                ContentTransform(
                                                    targetContentEnter = fadeIn(animationSpec = tween(200)) +
                                                        slideInVertically(
                                                            initialOffsetY = { full ->
                                                                val d = kotlin.math.min(full / 3, 28)
                                                                if (forward) d else -d
                                                            },
                                                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                                                        ),
                                                    initialContentExit = fadeOut(animationSpec = tween(170)) +
                                                        slideOutVertically(
                                                            targetOffsetY = { full ->
                                                                val d = kotlin.math.min(full / 4, 22)
                                                                if (forward) -d else d
                                                            },
                                                            animationSpec = tween(160, easing = FastOutSlowInEasing)
                                                        ),
                                                    sizeTransform = androidx.compose.animation.SizeTransform(clip = false)
                                                )
                                            },
                                            label = "quick_subtitle_items_switch_landscape"
                                        ) { groupIndex ->
                                            val animatedQuickItems = groups.getOrNull(groupIndex)?.items.orEmpty()
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 3.dp)
                                                    .verticalScroll(quickItemsScrollState),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Spacer(Modifier.height(3.dp))
                                                animatedQuickItems.forEach { text ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(72.dp)
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
                                                                .padding(horizontal = 8.dp, vertical = 8.dp),
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
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(56.dp)
                                                        .clickable {
                                                            viewModel.addQuickSubtitleItem(
                                                                groupIndex = groupIndex,
                                                                value = subtitleText
                                                            )
                                                        },
                                                    shape = RoundedCornerShape(UiTokens.Radius),
                                                    backgroundColor = md2CardContainerColor(),
                                                    elevation = UiTokens.CardElevation
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        MsIcon("add", contentDescription = "添加当前文本")
                                                    }
                                                }
                                                Spacer(Modifier.height(3.dp))
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(1.dp)
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                    )
                                    Column(
                                        modifier = Modifier
                                            .width(44.dp)
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .verticalScroll(rememberScrollState())
                                                .padding(horizontal = 2.dp, vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            groups.forEachIndexed { index, group ->
                                                val selected = selectedGroupIndex == index
                                                val tabBg =
                                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(UiTokens.Radius))
                                                        .background(tabBg)
                                                        .clickable { viewModel.selectQuickSubtitleGroup(index) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    MsIcon(group.icon, contentDescription = group.title)
                                                }
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp),
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
                            }
                        }
                    }
                }
            } else {
                Md2StaggeredFloatIn(
                    index = 0,
                    enabled = false,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 260.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
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
                                    .combinedClickable(
                                        onClick = { viewModel.openQuickSubtitlePreview() },
                                        onLongClick = copySubtitleText
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    AnimatedContent(
                                        targetState = subtitleText,
                                        transitionSpec = {
                                            ContentTransform(
                                                targetContentEnter = fadeIn(animationSpec = tween(180)) +
                                                    slideInVertically(
                                                        initialOffsetY = { full -> full / 6 },
                                                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                                                    ),
                                                initialContentExit = fadeOut(animationSpec = tween(120)),
                                                sizeTransform = null
                                            )
                                        },
                                        label = "quick_subtitle_text_change"
                                    ) { text ->
                                        val textColor = if (text == QUICK_SUBTITLE_CLEARED_HINT) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (subtitleBold) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = subtitleSize.sp,
                                                lineHeight = (subtitleSize * 1.15f).sp
                                            ),
                                            color = textColor,
                                            textAlign = subtitleAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            orangeActionButtons(false)
                            Spacer(Modifier.height(4.dp))
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
                }
            }

            if (!isLandscape) {
                AnimatedVisibility(
                    visible = !quickInputCollapsed,
                    enter = fadeIn(animationSpec = tween(140)) +
                        expandVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(120)) +
                        shrinkVertically(animationSpec = tween(160, easing = FastOutSlowInEasing))
                ) {
                    Column {
                    Spacer(Modifier.height(8.dp))
                    Md2StaggeredFloatIn(index = 1, enabled = false) {
                        if (isLandscape) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickItems.forEach { text ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
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
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clickable {
                                            viewModel.addQuickSubtitleItem(
                                                groupIndex = selectedGroupIndex,
                                                value = subtitleText
                                            )
                                        },
                                    shape = RoundedCornerShape(UiTokens.Radius),
                                    backgroundColor = md2CardContainerColor(),
                                    elevation = UiTokens.CardElevation
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MsIcon("add", contentDescription = "添加当前文本")
                                    }
                                }
                            }
                        } else {
                            AnimatedContent(
                                targetState = selectedGroupIndex,
                                transitionSpec = {
                                    val forward = targetState >= initialState
                                    ContentTransform(
                                        targetContentEnter = fadeIn(animationSpec = tween(200)) +
                                            slideInHorizontally(
                                                initialOffsetX = { full -> if (forward) full / 3 else -full / 3 },
                                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                                            ),
                                        initialContentExit = fadeOut(animationSpec = tween(170)) +
                                            slideOutHorizontally(
                                                targetOffsetX = { full -> if (forward) -full / 4 else full / 4 },
                                                animationSpec = tween(210, easing = FastOutSlowInEasing)
                                            ),
                                        sizeTransform = null
                                    )
                                },
                                label = "quick_subtitle_items_switch_portrait"
                            ) { groupIndex ->
                                val animatedQuickItems = groups.getOrNull(groupIndex)?.items.orEmpty()
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
                                    animatedQuickItems.forEach { text ->
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
                                    Card(
                                        modifier = Modifier
                                            .padding(vertical = 3.dp)
                                            .width(86.dp)
                                            .height(94.dp)
                                            .clickable {
                                                viewModel.addQuickSubtitleItem(
                                                    groupIndex = groupIndex,
                                                    value = subtitleText
                                                )
                                            },
                                        shape = RoundedCornerShape(UiTokens.Radius),
                                        backgroundColor = md2CardContainerColor(),
                                        elevation = UiTokens.CardElevation
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            MsIcon("add", contentDescription = "添加当前文本")
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Md2StaggeredFloatIn(
                        index = 2,
                        enabled = false,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .fillMaxWidth()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(UiTokens.Radius),
                            backgroundColor = md2CardContainerColor(),
                            elevation = UiTokens.CardElevation
                        ) {
                            if (isLandscape) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        groups.forEachIndexed { index, group ->
                                            val selected = selectedGroupIndex == index
                                            val tabBg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
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
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
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
                            } else {
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
                        }
                    }
                    // Reserve a tiny shadow gutter so tab card elevation won't be clipped
                    // during expand/collapse in portrait mode.
                    Spacer(Modifier.height(3.dp))
                    }
                }
            }
            Spacer(Modifier.height(quickSubtitleBottomBlank))
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
            val sendInput = {
                if (inputFieldValue.text.trim().isNotEmpty()) {
                    viewModel.submitQuickSubtitleInput(
                        playVoice = playOnSend && hasVoice
                    )
                    inputFieldValue = TextFieldValue("")
                }
            }
            val actionButtons: @Composable () -> Unit = {
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
                    icon = if (quickInputCollapsed) "subtitles_off" else "subtitles",
                    contentDescription = if (quickInputCollapsed) "展开快捷输入区域" else "收起快捷输入区域",
                    onClick = {
                        viewModel.updateQuickSubtitleInputCollapsed(!quickInputCollapsed)
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
                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            actionButtons()
                        }
                        OutlinedTextField(
                            value = inputFieldValue,
                            onValueChange = {
                                inputFieldValue = it
                                viewModel.updateQuickSubtitleInputText(it.text)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("请输入文本") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrect = true,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = { sendInput() },
                                onDone = { sendInput() }
                            ),
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
                        if (!useOverlayFab) {
                            QuickSubtitleMicFab(
                                state = state,
                                compactPttSideButtonsMode = compactPttSideButtonsMode,
                                onToggleMic = onToggleMic,
                                onPushToTalkPressStart = onPushToTalkPressStart,
                                onPushToTalkPressEnd = onPushToTalkPressEnd,
                                onPttDragTargetChanged = { pttDragTarget = it },
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                        IconButton(
                            onClick = sendInput,
                            enabled = inputFieldValue.text.trim().isNotEmpty()
                        ) {
                            MsIcon(
                                name = "send",
                                contentDescription = "发送到朗读队列",
                                tint = if (inputFieldValue.text.trim().isNotEmpty()) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        actionButtons()
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
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrect = true,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = { sendInput() },
                                onDone = { sendInput() }
                            ),
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
                            onClick = sendInput,
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
        }

        QuickSubtitlePttConfirmOverlay(
            visible = showPttConfirmOverlay,
            dragTarget = pttDragTarget,
            streamingText = state.pushToTalkStreamingText,
            isLandscape = isLandscape,
            compactPttSideButtonsMode = compactPttSideButtonsMode
        )

        AnimatedVisibility(
            visible = showPttConfirmOverlay,
            modifier = Modifier
                .zIndex(6.5f)
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(
                    start = pttStatusStripStartInset,
                    end = pttStatusStripAnimatedEndInsetWithBleed,
                    bottom = pttStatusStripBottomInset
                ),
            enter = fadeIn(animationSpec = tween(140)),
            exit = fadeOut(animationSpec = tween(110))
        ) {
            Box(
                modifier = Modifier.padding(
                    start = pttStatusStripOuterBleed,
                    top = pttStatusStripTopBleed,
                    end = pttStatusStripOuterBleed,
                    bottom = pttStatusStripBottomBleed
                )
            ) {
                QuickSubtitlePttConfirmBottomStrip(
                    guideText = pttGuideText,
                    reserveFabWidth = pttStripFabReserveWidth,
                    stripHeight = pttFabSize
                )
            }
        }

        QuickSubtitlePttCompactSideButtonsOverlay(
            visible = showPttConfirmOverlay && compactPttSideButtonsMode,
            dragTarget = pttDragTarget,
            fabSize = pttFabSize,
            fabEndInset = pttFabEndInset,
            fabBottomOffset = pttFabBottomOffset
        )

        if (useOverlayFab) {
            val fabModifier = if (isLandscape) {
                Modifier
                    .zIndex(7f)
                    .align(Alignment.BottomEnd)
                    .imePadding()
                    .navigationBarsPadding()
                    // Keep same visual slot as the inline landscape FAB (before the send button).
                    .padding(end = pttFabEndInset, bottom = pttFabBottomOffset)
                    .size(pttFabSize)
            } else {
                Modifier
                    .zIndex(7f)
                    .align(Alignment.BottomEnd)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(end = pttFabEndInset, bottom = pttFabBottomOffset)
                    .size(pttFabSize)
            }
            QuickSubtitleMicFab(
                state = state,
                compactPttSideButtonsMode = compactPttSideButtonsMode,
                onToggleMic = onToggleMic,
                onPushToTalkPressStart = onPushToTalkPressStart,
                onPushToTalkPressEnd = onPushToTalkPressEnd,
                onPttDragTargetChanged = { pttDragTarget = it },
                modifier = fabModifier
            )
        }

        if (subtitleFullscreenDialogVisible) {
            Dialog(
                onDismissRequest = { viewModel.closeQuickSubtitlePreview() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onClick = { viewModel.closeQuickSubtitlePreview() },
                            onLongClick = copySubtitleText
                        )
                        .padding(14.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(UiTokens.Radius),
                        backgroundColor = md2CardContainerColor(),
                        elevation = UiTokens.MenuElevation
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (subtitleBold) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = (subtitleSize * 1.25f).coerceIn(36f, 140f).sp,
                                    lineHeight = (subtitleSize * 1.36f).coerceIn(40f, 152f).sp
                                ),
                                color = subtitleTextColor,
                                textAlign = subtitleAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSubtitlePttConfirmOverlay(
    visible: Boolean,
    dragTarget: PttConfirmDragTarget,
    streamingText: String,
    isLandscape: Boolean,
    compactPttSideButtonsMode: Boolean,
    showInputAction: Boolean = true,
    applyNavigationBarsPadding: Boolean = true,
    topRowBottomReservedOverride: Dp? = null
) {
    val overlayHorizontalPadding = 16.dp
    val topRowBottomReserved = topRowBottomReservedOverride ?: if (isLandscape) 84.dp else 142.dp
    val topRowVerticalPadding = if (isLandscape) 6.dp else 18.dp

    val displayText = streamingText.ifBlank { "正在识别..." }
    val overlayBrush: Brush = if (compactPttSideButtonsMode) {
        SolidColor(Color.Black.copy(alpha = 0.34f))
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = 0.38f)
            )
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(6f),
        enter = fadeIn(animationSpec = tween(120)),
        exit = fadeOut(animationSpec = tween(110))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .imePadding()
                    .then(
                        if (applyNavigationBarsPadding) {
                            Modifier.navigationBarsPadding()
                        } else {
                            Modifier
                        }
                    )
                    .background(overlayBrush)
            )
            if (!compactPttSideButtonsMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = overlayHorizontalPadding, vertical = topRowVerticalPadding)
                        .imePadding()
                        .then(
                            if (applyNavigationBarsPadding) {
                                Modifier.navigationBarsPadding()
                            } else {
                                Modifier
                            }
                        )
                        .padding(bottom = topRowBottomReserved),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedContent(
                            modifier = Modifier.weight(1f),
                            targetState = displayText,
                            transitionSpec = {
                                ContentTransform(
                                    targetContentEnter = fadeIn(
                                        animationSpec = tween(120, easing = LinearEasing)
                                    ),
                                    initialContentExit = fadeOut(
                                        animationSpec = tween(120, easing = LinearEasing)
                                    )
                                )
                            },
                            label = "ptt_confirm_stream_text_top"
                        ) { text ->
                            Text(
                                text = text,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.h6,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = true,
                                overflow = TextOverflow.Clip
                            )
                        }
                        if (showInputAction) {
                            Surface(
                                modifier = Modifier.requiredSize(72.dp),
                                shape = CircleShape,
                                color = if (dragTarget == PttConfirmDragTarget.ToInput) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0xFF202124)
                                },
                                elevation = 6.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    MsIcon(
                                        name = "keyboard_return",
                                        contentDescription = "输入到文本框",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.requiredSize(72.dp),
                            shape = CircleShape,
                            color = if (dragTarget == PttConfirmDragTarget.Cancel) {
                                Color(0xFFB00020)
                            } else {
                                Color(0xFF202124)
                            },
                            elevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                MsIcon(
                                    name = "close",
                                    contentDescription = "取消发送",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSubtitlePttCompactSideButtonsOverlay(
    visible: Boolean,
    dragTarget: PttConfirmDragTarget,
    fabSize: Dp,
    fabEndInset: Dp,
    fabBottomOffset: Dp,
    showInputAction: Boolean = true,
    applyNavigationBarsPadding: Boolean = true
) {
    val sideGap = 10.dp
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(6.9f),
        enter = fadeIn(animationSpec = tween(120)),
        exit = fadeOut(animationSpec = tween(110))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val cancelEndInset = (fabEndInset - fabSize - sideGap).coerceAtLeast(0.dp)
            if (showInputAction) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .imePadding()
                        .then(
                            if (applyNavigationBarsPadding) {
                                Modifier.navigationBarsPadding()
                            } else {
                                Modifier
                            }
                        )
                        .padding(end = fabEndInset + fabSize + sideGap, bottom = fabBottomOffset)
                        .requiredSize(fabSize),
                    shape = CircleShape,
                    color = if (dragTarget == PttConfirmDragTarget.ToInput) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(0xFF202124)
                    },
                    elevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        MsIcon(
                            name = "keyboard_return",
                            contentDescription = "输入到文本框",
                            tint = Color.White
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .imePadding()
                    .then(
                        if (applyNavigationBarsPadding) {
                            Modifier.navigationBarsPadding()
                        } else {
                            Modifier
                        }
                    )
                    .padding(end = cancelEndInset, bottom = fabBottomOffset)
                    .requiredSize(fabSize),
                shape = CircleShape,
                color = if (dragTarget == PttConfirmDragTarget.Cancel) {
                    Color(0xFFB00020)
                } else {
                    Color(0xFF202124)
                },
                elevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MsIcon(
                        name = "close",
                        contentDescription = "取消发送",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSubtitlePttConfirmBottomStrip(
    guideText: String,
    reserveFabWidth: Dp,
    stripHeight: Dp
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(stripHeight),
        shape = RoundedCornerShape(42.dp),
        color = md2CardContainerColor(),
        elevation = UiTokens.FabElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MsIcon("graphic_eq", contentDescription = "识别中")
            AnimatedContent(
                targetState = guideText,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(animationSpec = tween(150)),
                        initialContentExit = fadeOut(animationSpec = tween(120))
                    )
                },
                label = "ptt_confirm_guide_text_strip"
            ) { text ->
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(reserveFabWidth))
        }
    }
}

@Composable
private fun Md2CardTitleText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        fontWeight = FontWeight.Bold
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
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
    val iconChoices = remember {
        listOf(
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
    }
    val groupNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val bringIntoViewScope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = UiTokens.PageTopBlank,
            bottom = UiTokens.PageBottomBlank
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "groups_card") {
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
                        Md2CardTitleText("分组", modifier = Modifier.weight(1f))
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(groupNameBringIntoViewRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        bringIntoViewScope.launch {
                                            groupNameBringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                }
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
        }

        if (selectedGroup != null) {
            item(key = "items_card") {
                QuickSubtitleItemsRecyclerCard(
                    items = selectedGroup.items,
                    onAdd = { viewModel.addQuickSubtitleItem(selectedGroupIndex) },
                    onItemsChanged = { reordered ->
                        viewModel.setQuickSubtitleItems(selectedGroupIndex, reordered)
                    },
                    onItemTextChanged = { itemIndex, value ->
                        viewModel.updateQuickSubtitleItem(selectedGroupIndex, itemIndex, value)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickSubtitleItemsRecyclerCard(
    items: List<String>,
    onAdd: () -> Unit,
    onItemsChanged: (List<String>) -> Unit,
    onItemTextChanged: (Int, String) -> Unit
) {
    var editTargetIndex by remember(items) { mutableStateOf<Int?>(null) }
    var editText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.Radius),
        backgroundColor = md2CardContainerColor(),
        elevation = UiTokens.CardElevation
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Md2CardTitleText("快捷文本", modifier = Modifier.weight(1f))
                Md2TextButton(onClick = onAdd) {
                    MsIcon("add", contentDescription = "新增文本")
                    Spacer(Modifier.width(4.dp))
                    Text("新增")
                }
            }
            QuickSubtitleItemsRecyclerList(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 92.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                items = items,
                onItemsChanged = onItemsChanged,
                onEditRequested = { index, value ->
                    editTargetIndex = index
                    editText = value
                }
            )
        }
    }

    val editingIndex = editTargetIndex
    if (editingIndex != null && editingIndex in items.indices) {
        AlertDialog(
            onDismissRequest = { editTargetIndex = null },
            title = { Text("编辑快捷文本") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(UiTokens.Radius),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                Md2TextButton(onClick = {
                    val idx = editTargetIndex
                    if (idx != null && idx in items.indices) {
                        onItemTextChanged(idx, editText)
                    }
                    editTargetIndex = null
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                Md2TextButton(onClick = { editTargetIndex = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun QuickSubtitleItemsRecyclerList(
    modifier: Modifier = Modifier,
    items: List<String>,
    onItemsChanged: (List<String>) -> Unit,
    onEditRequested: (Int, String) -> Unit
) {
    val parentComposition = rememberCompositionContext()
    val onItemsChangedState = rememberUpdatedState(onItemsChanged)
    val onEditRequestedState = rememberUpdatedState(onEditRequested)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val recycler = RecyclerView(ctx).apply {
                layoutManager = LinearLayoutManager(ctx)
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                clipToPadding = false
                clipChildren = false
                isNestedScrollingEnabled = false
                itemAnimator = DefaultItemAnimator().apply {
                    supportsChangeAnimations = false
                    addDuration = 120L
                    removeDuration = 120L
                    moveDuration = 160L
                    changeDuration = 0L
                }
            }

            val adapter = QuickSubtitleItemRecyclerAdapter(
                parentComposition = parentComposition,
                onItemsChanged = { changed -> onItemsChangedState.value(changed) },
                onEditRequested = { index, value -> onEditRequestedState.value(index, value) }
            )
            recycler.adapter = adapter

            val touchCallback = object : ItemTouchHelper.Callback() {
                private var activeViewHolder: RecyclerView.ViewHolder? = null
                private var moved = false

                override fun isLongPressDragEnabled(): Boolean = false
                override fun isItemViewSwipeEnabled(): Boolean = false

                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    return makeMovementFlags(dragFlags, 0)
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    val ok = adapter.move(from, to)
                    moved = moved || ok
                    return ok
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                        if (activeViewHolder !== viewHolder) activeViewHolder = viewHolder
                        activeViewHolder = viewHolder
                        adapter.setDraggingPosition(viewHolder.bindingAdapterPosition)
                    } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        activeViewHolder = null
                        adapter.clearDraggingItem()
                    }
                    adapter.isDragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    if (activeViewHolder === viewHolder) activeViewHolder = null
                    adapter.isDragging = false
                    adapter.clearDraggingItem()
                    if (moved) {
                        onItemsChangedState.value(adapter.snapshotTexts())
                        moved = false
                    }
                }
            }
            val touchHelper = ItemTouchHelper(touchCallback)
            touchHelper.attachToRecyclerView(recycler)
            adapter.onStartDrag = { vh -> touchHelper.startDrag(vh) }
            recycler
        },
        update = { recycler ->
            val adapter = recycler.adapter as? QuickSubtitleItemRecyclerAdapter ?: return@AndroidView
            adapter.submitFromState(items)
        }
    )
}

private data class QuickSubtitleEditableItem(
    val id: Long,
    var text: String
)

private class QuickSubtitleItemRecyclerAdapter(
    private val parentComposition: CompositionContext,
    private val onItemsChanged: (List<String>) -> Unit,
    private val onEditRequested: (Int, String) -> Unit
) : RecyclerView.Adapter<QuickSubtitleItemRecyclerAdapter.ItemViewHolder>() {

    private val items = mutableListOf<QuickSubtitleEditableItem>()
    private var nextId = 1L
    var isDragging: Boolean = false
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
    private var draggingItemId: Long? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = items[position].id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setParentCompositionContext(parentComposition)
        }
        return ItemViewHolder(composeView)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        if (!isDragging) {
            holder.itemView.translationZ = 0f
        }
        val row = items[position]
        holder.bind(
            itemId = row.id,
            text = row.text,
            isDragged = draggingItemId == row.id,
            canDelete = items.size > 1,
            onDelete = {
                val idx = holder.bindingAdapterPosition
                if (idx in items.indices && items.size > 1) {
                    items.removeAt(idx)
                    notifyItemRemoved(idx)
                    onItemsChanged(snapshotTexts())
                }
            },
            onEdit = {
                val idx = holder.bindingAdapterPosition
                if (idx in items.indices) {
                    onEditRequested(idx, items[idx].text)
                }
            },
            onStartDrag = {
                if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onStartDrag?.invoke(holder)
                }
            }
        )
    }

    fun submitFromState(newItems: List<String>) {
        if (isDragging) return
        val oldItems = items.toList()
        val used = BooleanArray(oldItems.size)
        val mapped = ArrayList<QuickSubtitleEditableItem>(newItems.size)

        for (text in newItems) {
            var matchedIndex = -1
            for (i in oldItems.indices) {
                if (!used[i] && oldItems[i].text == text) {
                    matchedIndex = i
                    break
                }
            }
            if (matchedIndex >= 0) {
                used[matchedIndex] = true
                mapped += oldItems[matchedIndex].copy(text = text)
            } else {
                mapped += QuickSubtitleEditableItem(id = nextId++, text = text)
            }
        }

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size
            override fun getNewListSize(): Int = mapped.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition].id == mapped[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition].text == mapped[newItemPosition].text
            }
        })

        items.clear()
        items.addAll(mapped)
        if (draggingItemId != null && items.none { it.id == draggingItemId }) {
            draggingItemId = null
        }
        diff.dispatchUpdatesTo(this)
    }

    fun move(from: Int, to: Int): Boolean {
        if (from == to || from !in items.indices || to !in items.indices) return false
        val moved = items.removeAt(from)
        items.add(to, moved)
        notifyItemMoved(from, to)
        return true
    }

    fun snapshotTexts(): List<String> = items.map { it.text }

    fun setDraggingPosition(position: Int) {
        val targetId = items.getOrNull(position)?.id
        if (draggingItemId == targetId) return
        val oldId = draggingItemId
        draggingItemId = targetId
        oldId?.let { id ->
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) notifyItemChanged(idx)
        }
        targetId?.let { id ->
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) notifyItemChanged(idx)
        }
    }

    fun clearDraggingItem() {
        val oldId = draggingItemId ?: return
        draggingItemId = null
        val idx = items.indexOfFirst { it.id == oldId }
        if (idx >= 0) notifyItemChanged(idx)
    }

    class ItemViewHolder(
        private val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView) {
        fun bind(
            itemId: Long,
            text: String,
            isDragged: Boolean,
            canDelete: Boolean,
            onDelete: () -> Unit,
            onEdit: () -> Unit,
            onStartDrag: () -> Unit
        ) {
            composeView.setContent {
                QuickSubtitleEditableRow(
                    itemId = itemId,
                    value = text,
                    isDragged = isDragged,
                    canDelete = canDelete,
                    onDelete = onDelete,
                    onEdit = onEdit,
                    onStartDrag = onStartDrag
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun QuickSubtitleEditableRow(
    itemId: Long,
    value: String,
    isDragged: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onStartDrag: () -> Unit
) {
    val rowElevation by animateDpAsState(
        targetValue = if (isDragged) 10.dp else 0.dp,
        animationSpec = tween(
            durationMillis = if (isDragged) 120 else 160,
            easing = FastOutSlowInEasing
        ),
        label = "quick_subtitle_item_elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        shape = RoundedCornerShape(UiTokens.Radius),
        backgroundColor = md2CardContainerColor(),
        elevation = rowElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value.ifBlank { "（空文本）" },
                modifier = Modifier
                    .weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Md2IconButton(
                icon = "edit",
                contentDescription = "编辑文本",
                onClick = onEdit
            )
            Md2IconButton(
                icon = "drag_indicator",
                contentDescription = "拖动排序",
                onClick = {},
                modifier = Modifier.pointerInteropFilter { ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            onStartDrag()
                            true
                        }
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> true
                        else -> false
                    }
                }
            )
            Md2IconButton(
                icon = "delete",
                contentDescription = "删除文本",
                onClick = onDelete,
                enabled = canDelete
            )
        }
    }
}

@Composable
fun RealtimeScreen(viewModel: MainViewModel) {
    val recognized = viewModel.realtimeRecognized
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
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
        if (recognized.isEmpty()) {
            item {
                Text("暂无识别结果", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(recognized, key = { it.id }) { item ->
                RecognizedQueueItemCard(
                    item = item,
                    onLongCopy = {
                        if (item.text.isNotBlank()) {
                            clipboard.setText(AnnotatedString(item.text))
                            toast(context, "已复制")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FloatingOverlayScreen(
    viewModel: MainViewModel,
    state: UiState,
    onOpenMainSettings: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val overlayPermissionGranted = remember { mutableStateOf(FloatingOverlayService.canDrawOverlays(context)) }
    var pendingOverlayPermissionEnable by remember { mutableStateOf(false) }
    var inputTypeExpanded by remember { mutableStateOf(false) }
    var outputTypeExpanded by remember { mutableStateOf(false) }
    val inputTypeOptions = remember {
        listOf(
            AudioRoutePreference.INPUT_AUTO to "自动",
            AudioRoutePreference.INPUT_BUILTIN_MIC to "内置麦克风/话筒",
            AudioRoutePreference.INPUT_USB to "USB 麦克风",
            AudioRoutePreference.INPUT_BLUETOOTH to "蓝牙麦克风",
            AudioRoutePreference.INPUT_WIRED to "有线麦克风"
        )
    }
    val outputTypeOptions = remember {
        listOf(
            AudioRoutePreference.OUTPUT_AUTO to "自动",
            AudioRoutePreference.OUTPUT_SPEAKER to "扬声器",
            AudioRoutePreference.OUTPUT_EARPIECE to "听筒",
            AudioRoutePreference.OUTPUT_BLUETOOTH to "蓝牙音频",
            AudioRoutePreference.OUTPUT_USB to "USB 音频",
            AudioRoutePreference.OUTPUT_WIRED to "有线耳机/线路"
        )
    }
    val overlayPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val granted = FloatingOverlayService.canDrawOverlays(context)
            overlayPermissionGranted.value = granted
            if (granted && pendingOverlayPermissionEnable) {
                viewModel.setFloatingOverlayEnabled(true)
                FloatingOverlayService.start(context)
            } else if (!granted) {
                viewModel.setFloatingOverlayEnabled(false)
                FloatingOverlayService.stop(context)
                toast(context, "需要悬浮窗权限")
            }
            pendingOverlayPermissionEnable = false
        }

    LaunchedEffect(Unit) {
        overlayPermissionGranted.value = FloatingOverlayService.canDrawOverlays(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(UiTokens.PageTopBlank))

        Md2StaggeredFloatIn(index = 0) {
            Md2SettingsCard(title = "悬浮窗状态") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Md2Switch(
                        checked = state.floatingOverlayEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                pendingOverlayPermissionEnable = false
                                viewModel.setFloatingOverlayEnabled(false)
                                FloatingOverlayService.stop(context)
                            } else if (overlayPermissionGranted.value) {
                                viewModel.setFloatingOverlayEnabled(true)
                                FloatingOverlayService.start(context)
                            } else {
                                pendingOverlayPermissionEnable = true
                                overlayPermissionLauncher.launch(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        }
                    )
                    Text("启用独立悬浮窗")
                }
                Text(
                    "权限状态：${if (overlayPermissionGranted.value) "已授权" else "未授权"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "运行状态：${if (state.floatingOverlayEnabled && overlayPermissionGranted.value) "已启用" else "未启用"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Md2OutlinedButton(
                        onClick = {
                            pendingOverlayPermissionEnable = false
                            overlayPermissionLauncher.launch(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    ) {
                        Text("打开权限设置")
                    }
                    Md2TextButton(
                        onClick = { FloatingOverlayService.refresh(context) },
                        enabled = state.floatingOverlayEnabled && overlayPermissionGranted.value
                    ) {
                        Text("刷新悬浮窗")
                    }
                }
                Text(
                    "悬浮窗可吸附到屏幕边缘，并可在软件外直接触发快捷字幕输入。",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Md2Switch(
                        checked = state.floatingOverlayAutoDock,
                        onCheckedChange = { viewModel.setFloatingOverlayAutoDock(it) }
                    )
                    Text("长时间不操作时自动贴边")
                }
                Text(
                    "开启后，悬浮 FAB 在 3 秒无操作时会自动吸附到屏幕边缘，仅露出半边并降低透明度。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Md2StaggeredFloatIn(index = 1) {
            Md2SettingsCard(title = "交互模式") {
                Text(
                    "以下交互设置与主设置页完全同步，这里仅显示当前状态，不再提供第二套独立开关。",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "按住说话模式：${if (state.pushToTalkMode) "已开启" else "未开启"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "按下输入文本确认：${
                        if (state.pushToTalkMode && state.pushToTalkConfirmInputMode) "已开启"
                        else if (state.pushToTalkMode) "未开启"
                        else "按住说话未开启"
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "保持后台运行：${if (state.keepAlive) "已开启" else "未开启"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Md2OutlinedButton(onClick = onOpenMainSettings) {
                    Text("前往主设置修改")
                }
            }
        }

        Md2StaggeredFloatIn(index = 2) {
            Md2SettingsCard(title = "音频与设备") {
                Text("播放音量倍率：${state.playbackGainPercent}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = state.playbackGainPercent.toFloat(),
                    onValueChange = { viewModel.setPlaybackGainPercent(it.toInt()) },
                    valueRange = 0f..1000f
                )
                Text("100% 为原始音量，拖动接近 100% 时会自动吸附。", style = MaterialTheme.typography.bodySmall)

                Text("首选输入设备类型", fontWeight = FontWeight.Bold)
                Box {
                    val label = inputTypeOptions.firstOrNull { it.first == state.preferredInputType }?.second
                        ?: inputTypeOptions.first().second
                    Md2DropdownButton(
                        label = label,
                        onClick = { inputTypeExpanded = true },
                        expanded = inputTypeExpanded
                    )
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
                Text("当前输入设备：${state.inputDeviceLabel}", style = MaterialTheme.typography.bodySmall)

                Text("首选输出设备类型", fontWeight = FontWeight.Bold)
                Box {
                    val label = outputTypeOptions.firstOrNull { it.first == state.preferredOutputType }?.second
                        ?: outputTypeOptions.first().second
                    Md2DropdownButton(
                        label = label,
                        onClick = { outputTypeExpanded = true },
                        expanded = outputTypeExpanded
                    )
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
                Text("当前输出设备：${state.outputDeviceLabel}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(UiTokens.PageBottomBlank))
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RecognizedQueueItemCard(
    item: RecognizedItem,
    onLongCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongCopy
            ),
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
    pushToTalkMode: Boolean,
    pushToTalkPressed: Boolean,
    playbackGainPercent: Int,
    preferredInputType: Int,
    preferredOutputType: Int,
    inputDeviceLabel: String,
    outputDeviceLabel: String,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputLevel = viewModel.realtimeInputLevel
    val playbackProgress = viewModel.realtimePlaybackProgress
    val micIcon = if (pushToTalkMode && pushToTalkPressed) "settings_voice" else "mic"
    var inputExpanded by remember { mutableStateOf(false) }
    var outputExpanded by remember { mutableStateOf(false) }
    val inputTypeOptions = remember {
        listOf(
            AudioRoutePreference.INPUT_AUTO to "自动",
            AudioRoutePreference.INPUT_BUILTIN_MIC to "内置麦克风/话筒",
            AudioRoutePreference.INPUT_USB to "USB 麦克风",
            AudioRoutePreference.INPUT_BLUETOOTH to "蓝牙麦克风",
            AudioRoutePreference.INPUT_WIRED to "有线麦克风"
        )
    }
    val outputTypeOptions = remember {
        listOf(
            AudioRoutePreference.OUTPUT_AUTO to "自动",
            AudioRoutePreference.OUTPUT_SPEAKER to "扬声器",
            AudioRoutePreference.OUTPUT_EARPIECE to "听筒",
            AudioRoutePreference.OUTPUT_BLUETOOTH to "蓝牙音频",
            AudioRoutePreference.OUTPUT_USB to "USB 音频",
            AudioRoutePreference.OUTPUT_WIRED to "有线耳机/线路"
        )
    }
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
                Crossfade(
                    targetState = micIcon,
                    animationSpec = tween(durationMillis = 180),
                    label = "running_strip_panel_mic_icon"
                ) { icon ->
                    MsIcon(icon, contentDescription = "麦克风音量")
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true)
                            ) { inputExpanded = true }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MsIcon("mic", contentDescription = "输入设备")
                        Text(
                            text = inputDeviceLabel,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        MsIcon(
                            name = if (inputExpanded) "expand_less" else "expand_more",
                            contentDescription = "选择首选输入设备"
                        )
                    }
                    DropdownMenu(
                        expanded = inputExpanded,
                        onDismissRequest = { inputExpanded = false }
                    ) {
                        inputTypeOptions.forEach { (value, label) ->
                            M2DropdownMenuItem(
                                onClick = {
                                    inputExpanded = false
                                    viewModel.setPreferredInputType(value)
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (value == preferredInputType) FontWeight.SemiBold else null
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true)
                            ) { outputExpanded = true }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MsIcon("volume_up", contentDescription = "输出设备")
                        Text(
                            text = outputDeviceLabel,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        MsIcon(
                            name = if (outputExpanded) "expand_less" else "expand_more",
                            contentDescription = "选择首选输出设备"
                        )
                    }
                    DropdownMenu(
                        expanded = outputExpanded,
                        onDismissRequest = { outputExpanded = false }
                    ) {
                        outputTypeOptions.forEach { (value, label) ->
                            M2DropdownMenuItem(
                                onClick = {
                                    outputExpanded = false
                                    viewModel.setPreferredOutputType(value)
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (value == preferredOutputType) FontWeight.SemiBold else null
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MsIcon("mic", contentDescription = "按住说话")
                    Text("按住说话", style = MaterialTheme.typography.bodySmall)
                }
                Md2Switch(
                    checked = pushToTalkMode,
                    onCheckedChange = { viewModel.setPushToTalkMode(it) }
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "音量倍率：${playbackGainPercent}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = playbackGainPercent.toFloat(),
                    onValueChange = { viewModel.setPlaybackGainPercent(it.toInt()) },
                    valueRange = 0f..1000f
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
    pushToTalkMode: Boolean,
    pushToTalkPressed: Boolean,
    contentColor: Color,
    onToggle: () -> Unit
) {
    val micIcon = if (pushToTalkMode && pushToTalkPressed) "settings_voice" else "mic"
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
                Crossfade(
                    targetState = micIcon,
                    animationSpec = tween(durationMillis = 180),
                    label = "running_strip_toggle_mic_icon"
                ) { icon ->
                    MsIcon(icon, contentDescription = "麦克风音量", tint = contentColor)
                }
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

@OptIn(ExperimentalComposeUiApi::class)
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
    val boardFillColor = if (isDark) Color(0xFF2C3237) else Color(0xFFFCFDFE)
    val currentPoints = remember { mutableStateListOf<DrawPoint>() }
    var viewportScale by rememberSaveable { mutableFloatStateOf(1f) }
    var viewportPanX by rememberSaveable { mutableFloatStateOf(0f) }
    var viewportPanY by rememberSaveable { mutableFloatStateOf(0f) }
    val toolbarCollapsed = viewModel.drawingToolbarCollapsed
    val boardReserveEndTarget = if (isLandscape) {
        if (toolbarCollapsed) 76.dp else 128.dp
    } else {
        0.dp
    }
    val boardReserveBottomTarget = if (isLandscape) {
        0.dp
    } else {
        if (toolbarCollapsed) 76.dp else 168.dp
    }
    val boardReserveEnd by animateDpAsState(
        targetValue = boardReserveEndTarget,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "drawing_board_reserve_end"
    )
    val boardReserveBottom by animateDpAsState(
        targetValue = boardReserveBottomTarget,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "drawing_board_reserve_bottom"
    )
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val contentHorizontalPadding = if (fullscreen) 0.dp else 16.dp
        val contentVerticalPadding = if (fullscreen) 0.dp else 16.dp
        val paperPadding = if (fullscreen) 8.dp else 16.dp

        val leftActionButtonSize = 36.dp
        val leftColorDotSize = 22.dp
        val leftItemSpacing = 8.dp
        val fixedActionCount = 3
        val fixedColorCount = 7
        val fixedMaxToolbarHeight =
            (leftActionButtonSize * fixedActionCount) +
            (leftItemSpacing * (fixedActionCount - 1)) +
            leftItemSpacing + // action section -> color section gap
            (leftColorDotSize * fixedColorCount) +
            (leftItemSpacing * (fixedColorCount - 1)) +
            (10.dp * 2) // card inner vertical padding

        val landscapeToolbarHeight = remember(maxHeight, fixedMaxToolbarHeight) {
            val verticalSafetyPadding = 16.dp
            val availableHeight = (maxHeight - verticalSafetyPadding * 2).coerceAtLeast(96.dp)
            minOf(availableHeight, fixedMaxToolbarHeight)
        }
        val fixedMaxToolbarWidth =
            (leftActionButtonSize * fixedActionCount) +
            (leftItemSpacing * (fixedActionCount - 1)) +
            10.dp + // left action row -> color row gap
            (leftColorDotSize * fixedColorCount) +
            (leftItemSpacing * (fixedColorCount - 1)) +
            (10.dp * 2) // card inner horizontal padding
        val portraitToolbarWidth = remember(maxWidth, fixedMaxToolbarWidth) {
            val horizontalSafetyPadding = 12.dp
            val availableWidth = (maxWidth - horizontalSafetyPadding * 2).coerceAtLeast(200.dp)
            minOf(availableWidth, fixedMaxToolbarWidth)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding)
                .padding(end = boardReserveEnd, bottom = boardReserveBottom)
        ) {
            val density = LocalDensity.current
            val boardAspect = 1080f / 1920f
            val quarterTurn = rotationDegrees == 90f || rotationDegrees == 270f
            val displayAspect = if (quarterTurn) 1f / boardAspect else boardAspect
            val availableWidth = (maxWidth - paperPadding * 2).coerceAtLeast(0.dp)
            val availableHeight = (maxHeight - paperPadding * 2).coerceAtLeast(0.dp)
            val candidateWidthByHeight = availableHeight * displayAspect
            val useHeightAsBase = candidateWidthByHeight <= availableWidth
            val cardWidthDp = if (useHeightAsBase) candidateWidthByHeight else availableWidth
            val cardHeightDp = if (useHeightAsBase) availableHeight else (availableWidth / displayAspect)

            var boardSize by remember { mutableStateOf(IntSize.Zero) }
            val fallbackW = with(density) { cardWidthDp.toPx() }
            val fallbackH = with(density) { cardHeightDp.toPx() }
            val canvasW = if (boardSize.width > 0) boardSize.width.toFloat() else fallbackW
            val canvasH = if (boardSize.height > 0) boardSize.height.toFloat() else fallbackH
            val fitW: Float
            val fitH: Float
            if (quarterTurn) {
                // Logical board stays portrait; display becomes landscape after rotation.
                fitW = canvasH
                fitH = canvasW
            } else {
                fitW = canvasW
                fitH = canvasH
            }
            val left = (canvasW - fitW) / 2f
            val top = (canvasH - fitH) / 2f
            val pxScale = minOf(fitW / 1080f, fitH / 1920f)
            val center = Offset(canvasW / 2f, canvasH / 2f)
            val activeWidth = if (viewModel.drawEraser) viewModel.drawEraserSize * 5f else viewModel.drawBrushSize
            val containerW = with(density) { maxWidth.toPx() }
            val containerH = with(density) { maxHeight.toPx() }
            val cardOrigin = Offset(
                x = ((containerW - canvasW) * 0.5f).coerceAtLeast(0f),
                y = ((containerH - canvasH) * 0.5f).coerceAtLeast(0f)
            )

            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(cardWidthDp, cardHeightDp)
                    .onSizeChanged { boardSize = it }
                    .graphicsLayer {
                        scaleX = viewportScale
                        scaleY = viewportScale
                        translationX = viewportPanX
                        translationY = viewportPanY
                    },
                shape = RoundedCornerShape(UiTokens.Radius),
                backgroundColor = md2CardContainerColor(),
                elevation = UiTokens.CardElevation
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
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

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(rotationDegrees, fitW, fitH, left, top, canvasW, canvasH, cardOrigin) {
                        fun clampPan(raw: Offset, scale: Float): Offset {
                            val sx = (((canvasW * scale) - canvasW) * 0.5f).coerceAtLeast(0f)
                            val sy = (((canvasH * scale) - canvasH) * 0.5f).coerceAtLeast(0f)
                            return Offset(
                                x = raw.x.coerceIn(-sx, sx),
                                y = raw.y.coerceIn(-sy, sy)
                            )
                        }
                        fun containerToCardLocal(pos: Offset, pan: Offset, scale: Float): Offset {
                            val safeScale = scale.coerceAtLeast(0.0001f)
                            val unpanned = pos - cardOrigin - pan
                            return center + (unpanned - center) / safeScale
                        }
                        fun mapPoint(pos: Offset): Offset {
                            val local = containerToCardLocal(
                                pos = pos,
                                pan = Offset(viewportPanX, viewportPanY),
                                scale = viewportScale
                            )
                            return local.rotateAround(center, rotationDegrees)
                        }

                        awaitPointerEventScope {
                            while (true) {
                                var drawingActive = false
                                var transformActive = false
                                var drawPointerId: androidx.compose.ui.input.pointer.PointerId? = null
                                var trackedA: androidx.compose.ui.input.pointer.PointerId? = null
                                var trackedB: androidx.compose.ui.input.pointer.PointerId? = null
                                var lastFocus = Offset.Zero
                                var lastSpan = 0f

                                val down = awaitFirstDown(requireUnconsumed = false)
                                drawPointerId = down.id
                                val downMapped = mapPoint(down.position)
                                currentPoints.clear()
                                if (downMapped.isInsideBoard(left, top, fitW, fitH)) {
                                    currentPoints.add(downMapped.toDrawPoint(left, top, fitW, fitH))
                                    drawingActive = true
                                }
                                down.consume()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }

                                    if (pressed.isEmpty()) {
                                        if (!transformActive && drawingActive && currentPoints.size > 1) {
                                            viewModel.appendDrawingStroke(currentPoints.toList())
                                        }
                                        currentPoints.clear()
                                        break
                                    }

                                    if (pressed.size >= 2) {
                                        val a = trackedA?.let { id -> pressed.firstOrNull { it.id == id } } ?: pressed[0]
                                        val b = trackedB?.let { id -> pressed.firstOrNull { it.id == id } }
                                            ?: pressed.firstOrNull { it.id != a.id }
                                            ?: pressed[1]
                                        trackedA = a.id
                                        trackedB = b.id
                                        drawPointerId = null

                                        val focus = Offset(
                                            x = (a.position.x + b.position.x) * 0.5f,
                                            y = (a.position.y + b.position.y) * 0.5f
                                        )
                                        val span = (a.position - b.position).getDistance().coerceAtLeast(1f)

                                        if (!transformActive) {
                                            transformActive = true
                                            drawingActive = false
                                            currentPoints.clear()
                                            lastFocus = focus
                                            lastSpan = span
                                        } else {
                                            val oldScale = viewportScale.coerceAtLeast(1f)
                                            val oldPan = Offset(viewportPanX, viewportPanY)
                                            val scaleBy = (span / lastSpan.coerceAtLeast(1f)).coerceIn(0.9f, 1.1f)
                                            val focusDelta = (focus - lastFocus).getDistance()
                                            val scaleDelta = kotlin.math.abs(scaleBy - 1f)
                                            if (focusDelta >= 0.18f || scaleDelta >= 0.0012f) {
                                                val targetScale = (oldScale * scaleBy).coerceIn(1f, 3.5f)
                                                val newScale = if (targetScale < 1.002f) 1f else targetScale
                                                val contentFocus = containerToCardLocal(
                                                    pos = lastFocus,
                                                    pan = oldPan,
                                                    scale = oldScale
                                                )
                                                val rawPan = focus - cardOrigin - center - (contentFocus - center) * newScale
                                                val clamped = if (newScale <= 1f) Offset.Zero else clampPan(rawPan, newScale)
                                                viewportScale = newScale
                                                viewportPanX = clamped.x
                                                viewportPanY = clamped.y
                                            }
                                            lastFocus = focus
                                            lastSpan = span
                                        }
                                        event.changes.forEach { if (it.pressed) it.consume() }
                                        continue
                                    }

                                    val one = drawPointerId?.let { id -> pressed.firstOrNull { it.id == id } } ?: pressed.first()
                                    if (transformActive) {
                                        transformActive = false
                                        drawPointerId = one.id
                                        trackedA = null
                                        trackedB = null
                                        lastSpan = 0f
                                        drawingActive = false
                                        currentPoints.clear()
                                    }

                                    if (!drawingActive) {
                                        val mapped = mapPoint(one.position)
                                        if (mapped.isInsideBoard(left, top, fitW, fitH)) {
                                            currentPoints.clear()
                                            currentPoints.add(mapped.toDrawPoint(left, top, fitW, fitH))
                                            drawingActive = true
                                        }
                                    } else {
                                        val mapped = mapPoint(one.position)
                                        if (mapped.isInsideBoard(left, top, fitW, fitH)) {
                                            currentPoints.add(mapped.toDrawPoint(left, top, fitW, fitH))
                                        }
                                    }
                                    one.consume()
                                }
                            }
                        }
                    }
            )
        }

        val toolbarAnchorModifier = if (isLandscape) {
            Modifier
                .align(Alignment.CenterEnd)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.End))
                .padding(end = 10.dp)
        } else {
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = 10.dp)
        }

        DrawingToolbar(
            modifier = toolbarAnchorModifier,
            isLandscape = isLandscape,
            colors = palette,
            selectedColor = viewModel.drawColor,
            brushSize = viewModel.drawBrushSize,
            eraserSize = viewModel.drawEraserSize,
            eraserEnabled = viewModel.drawEraser,
            visible = !toolbarCollapsed,
            fullscreen = fullscreen,
            onToggleFullscreen = onToggleFullscreen,
            onToggleCollapsed = { viewModel.updateDrawingToolbarCollapsed(!toolbarCollapsed) },
            landscapeToolbarHeight = landscapeToolbarHeight,
            portraitToolbarWidth = portraitToolbarWidth,
            onPickColor = { viewModel.updateDrawColor(it) },
            onBrushSize = { viewModel.updateDrawBrushSize(it) },
            onToggleEraser = { viewModel.updateDrawEraser(it) },
            onClear = { viewModel.clearDrawingBoard() }
        )
        DrawingToolbarMini(
            modifier = toolbarAnchorModifier,
            isLandscape = isLandscape,
            visible = toolbarCollapsed,
            fullscreen = fullscreen,
            onToggleFullscreen = onToggleFullscreen,
            onToggleCollapsed = { viewModel.updateDrawingToolbarCollapsed(!toolbarCollapsed) }
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
    visible: Boolean,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onToggleCollapsed: () -> Unit,
    landscapeToolbarHeight: Dp,
    portraitToolbarWidth: Dp,
    onPickColor: (Color) -> Unit,
    onBrushSize: (Float) -> Unit,
    onToggleEraser: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = if (isLandscape) {
            fadeIn(animationSpec = tween(130)) + androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { full -> full / 2 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        } else {
            fadeIn(animationSpec = tween(130)) + slideInVertically(
                initialOffsetY = { full -> full / 2 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        },
        exit = if (isLandscape) {
            fadeOut(animationSpec = tween(100)) + androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { full -> full / 2 },
                animationSpec = tween(140, easing = FastOutSlowInEasing)
            )
        } else {
            fadeOut(animationSpec = tween(100)) + slideOutVertically(
                targetOffsetY = { full -> full / 2 },
                animationSpec = tween(140, easing = FastOutSlowInEasing)
            )
        }
    ) {
        Card(
            modifier = if (isLandscape) Modifier else Modifier.width(portraitToolbarWidth),
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = 6.dp
        ) {
            val activeSize = if (eraserEnabled) eraserSize else brushSize
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .height(landscapeToolbarHeight),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight(),
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
                                .fillMaxWidth()
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
                            .width(52.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Md2VerticalSlider(
                                value = activeSize,
                                onValueChange = onBrushSize,
                                valueRange = 2f..48f,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Md2ToolToggle(
                            icon = "chevron_right",
                            selected = false,
                            onClick = onToggleCollapsed,
                            contentDescription = "折叠工具栏"
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
                            icon = "expand_more",
                            selected = false,
                            onClick = onToggleCollapsed,
                            contentDescription = "折叠工具栏"
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
}

@Composable
private fun DrawingToolbarMini(
    modifier: Modifier = Modifier,
    isLandscape: Boolean,
    visible: Boolean,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onToggleCollapsed: () -> Unit
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = if (isLandscape) {
            fadeIn(animationSpec = tween(130)) + androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { full -> full / 2 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        } else {
            fadeIn(animationSpec = tween(130)) + slideInVertically(
                initialOffsetY = { full -> full / 2 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        },
        exit = if (isLandscape) {
            fadeOut(animationSpec = tween(100)) + androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { full -> full / 2 },
                animationSpec = tween(140, easing = FastOutSlowInEasing)
            )
        } else {
            fadeOut(animationSpec = tween(100)) + slideOutVertically(
                targetOffsetY = { full -> full / 2 },
                animationSpec = tween(140, easing = FastOutSlowInEasing)
            )
        }
    ) {
        Card(
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = 6.dp
        ) {
            if (isLandscape) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Md2ToolToggle(
                        icon = "chevron_left",
                        selected = false,
                        onClick = onToggleCollapsed,
                        contentDescription = "展开工具栏"
                    )
                    Md2ToolToggle(
                        icon = if (fullscreen) "fullscreen_exit" else "fullscreen",
                        selected = false,
                        onClick = onToggleFullscreen,
                        contentDescription = if (fullscreen) "退出全屏" else "进入全屏"
                    )
                }
            } else {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Md2ToolToggle(
                        icon = "expand_less",
                        selected = false,
                        onClick = onToggleCollapsed,
                        contentDescription = "展开工具栏"
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
    val scope = rememberCoroutineScope()
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
    var showSpeakerEnrollDialog by remember { mutableStateOf(false) }
    var speakerEnrollStep by remember { mutableIntStateOf(0) } // 0准备 1句1 2句2 3句3 4结果
    var speakerEnrollCountingDown by remember { mutableStateOf(false) }
    var speakerEnrollCountdown by remember { mutableIntStateOf(3) }
    var speakerEnrollReading by remember { mutableStateOf(false) }
    var speakerEnrollRemainingSec by remember { mutableFloatStateOf(4f) }
    var speakerEnrollProgress by remember { mutableFloatStateOf(0f) }
    var speakerEnrollLevel by remember { mutableFloatStateOf(0f) }
    var speakerEnrollSuccess by remember { mutableStateOf(false) }
    var speakerEnrollMessage by remember { mutableStateOf("") }
    var speakerEnrollRetryDialog by remember { mutableStateOf(false) }
    var speakerEnrollOpenedByToggle by remember { mutableStateOf(false) }
    val speakerEnrollTexts = remember {
        listOf(
            "清晨的风吹过脸颊，我大步沿着河边走。",
            "远处钟声敲响，心跳也慢慢静下来。",
            "水面浮着天光，闭上眼，听见自己的呼吸。"
        )
    }
    val speakerEnrollSamples = remember { mutableStateListOf<FloatArray?>() }
    val asrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.importAsr(uri) else toast(context, "未选择文件")
    }
    val drawingDirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            viewModel.setDrawingSavePathFromTreeUri(uri)
        } else {
            toast(context, "未选择目录")
        }
    }
    fun combineSpeakerProfiles(samples: List<FloatArray>): FloatArray? {
        if (samples.isEmpty()) return null
        val dim = samples.minOfOrNull { it.size } ?: return null
        if (dim <= 0) return null
        val out = FloatArray(dim)
        samples.forEach { s ->
            for (i in 0 until dim) {
                out[i] += s[i]
            }
        }
        for (i in out.indices) {
            out[i] /= samples.size.toFloat()
        }
        var sumSq = 0.0
        for (v in out) sumSq += v * v
        val norm = kotlin.math.sqrt(sumSq)
        if (norm <= 1e-8) return null
        for (i in out.indices) {
            out[i] = (out[i] / norm).toFloat()
        }
        return out
    }
    fun startSpeakerEnrollStepCapture(step: Int) {
        if (speakerEnrollReading || speakerEnrollCountingDown) return
        if (step !in 1..3) return
        scope.launch {
            speakerEnrollCountingDown = true
            speakerEnrollCountdown = 3
            speakerEnrollMessage = "请准备，第 $step 句即将开始"
            for (i in 3 downTo 1) {
                speakerEnrollCountdown = i
                delay(800)
                if (!showSpeakerEnrollDialog) {
                    speakerEnrollCountingDown = false
                    return@launch
                }
            }
            speakerEnrollCountingDown = false
            speakerEnrollReading = true
            speakerEnrollProgress = 0f
            speakerEnrollLevel = 0f
            speakerEnrollRemainingSec = 4f
            speakerEnrollMessage = "请朗读第 $step 句"
            val result = viewModel.enrollSpeakerProfileNow(
                durationSec = 4f,
                onCapture = { progress, level ->
                    speakerEnrollProgress = progress.coerceIn(0f, 1f)
                    speakerEnrollLevel = level.coerceIn(0f, 1f)
                    speakerEnrollRemainingSec = (4f * (1f - speakerEnrollProgress)).coerceAtLeast(0f)
                },
                persist = false
            )
            speakerEnrollReading = false
            speakerEnrollLevel = 0f
            if (result.success && result.profile != null) {
                val index = step - 1
                while (speakerEnrollSamples.size <= index) {
                    speakerEnrollSamples.add(null)
                }
                speakerEnrollSamples[index] = result.profile
                if (step < 3) {
                    speakerEnrollStep = step + 1
                    speakerEnrollProgress = 0f
                    speakerEnrollRemainingSec = 4f
                    speakerEnrollMessage = "第 $step 句录制成功"
                } else {
                    val combined = combineSpeakerProfiles(speakerEnrollSamples.filterNotNull())
                    if (combined == null) {
                        speakerEnrollRetryDialog = true
                        speakerEnrollMessage = "合并注册信息失败，请重录第三句"
                    } else {
                        if (viewModel.applySpeakerProfile(combined)) {
                            if (speakerEnrollOpenedByToggle) {
                                viewModel.setSpeakerVerifyEnabled(true)
                            }
                            speakerEnrollSuccess = true
                            speakerEnrollStep = 4
                            speakerEnrollProgress = 1f
                            speakerEnrollMessage = "说话人注册成功"
                        } else {
                            speakerEnrollSuccess = false
                            speakerEnrollStep = 4
                            speakerEnrollProgress = 0f
                            speakerEnrollMessage = "注册失败，请稍后重试"
                        }
                    }
                }
            } else {
                speakerEnrollSuccess = false
                speakerEnrollRetryDialog = true
                speakerEnrollMessage = result.message
            }
        }
    }
    fun closeSpeakerEnrollDialog() {
        showSpeakerEnrollDialog = false
        speakerEnrollCountingDown = false
        speakerEnrollReading = false
        val hasRegistered = state.speakerProfileReady || speakerEnrollSuccess
        if (speakerEnrollOpenedByToggle && !hasRegistered) {
            viewModel.setSpeakerVerifyEnabled(false)
        }
        speakerEnrollOpenedByToggle = false
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(UiTokens.PageTopBlank))

        Md2StaggeredFloatIn(index = 0) {
            Md2SettingsCard(title = "模型与资源") {
                Text("ASR 模型 (sosv-int8.zip)", fontWeight = FontWeight.Bold)
                Text(state.asrDir?.absolutePath ?: "未导入", style = MaterialTheme.typography.bodySmall)
                Md2Button(onClick = { asrPicker.launch("*/*") }) {
                    Text("导入 ASR 模型")
                }
            }
        }

        Md2StaggeredFloatIn(index = 1) {
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
        }

        Md2StaggeredFloatIn(index = 2) {
            Md2SettingsCard(title = "系统与布局") {
                Text("横屏抽屉模式", fontWeight = FontWeight.Bold)
                Box {
                    val label = drawerModeOptions.firstOrNull { it.first == state.landscapeDrawerMode }?.second
                        ?: drawerModeOptions.first().second
                    Md2DropdownButton(
                        label = label,
                        onClick = { drawerModeExpanded = true },
                        expanded = drawerModeExpanded
                    )
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

                Text("画板保存路径（相册）", fontWeight = FontWeight.Bold)
                Text(state.drawingSaveRelativePath, style = MaterialTheme.typography.bodySmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Md2OutlinedButton(onClick = {
                        drawingDirPicker.launch(null)
                    }) {
                        Text("选择目录")
                    }
                    Md2TextButton(onClick = {
                        val def = UserPrefs.DEFAULT_DRAWING_SAVE_RELATIVE_PATH
                        viewModel.setDrawingSaveRelativePath(def)
                    }) {
                        Text("恢复默认")
                    }
                }
                Text("通过系统文件管理器选择目录（建议内部存储）", style = MaterialTheme.typography.bodySmall)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Md2Switch(
                        checked = state.quickCardAutoSaveOnExit,
                        onCheckedChange = { viewModel.setQuickCardAutoSaveOnExit(it) }
                    )
                    Text("退出名片编辑时自动保存")
                }
                Text("关闭时将弹窗询问“是否保存名片”", style = MaterialTheme.typography.bodySmall)

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
        }

        Md2StaggeredFloatIn(index = 3) {
            Md2SettingsCard(title = "识别与转换") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.asrSendToQuickSubtitle,
                    onCheckedChange = { viewModel.setAsrSendToQuickSubtitle(it) }
                )
                Text("识别结果自动上屏大字幕")
            }
            Text("开启后：语音识别结果会自动更新便捷字幕主文本", style = MaterialTheme.typography.bodySmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.pushToTalkMode,
                    onCheckedChange = { viewModel.setPushToTalkMode(it) }
                )
                Text("按住说话模式")
            }
            Text("开启后：实时页 FAB 改为麦克风，按下开始收音，松开停止收音。", style = MaterialTheme.typography.bodySmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.pushToTalkConfirmInputMode,
                    enabled = state.pushToTalkMode,
                    onCheckedChange = { viewModel.setPushToTalkConfirmInputMode(it) }
                )
                Text("按下输入文本确认")
            }
            Text(
                "开启后：按住说话时识别文本先显示在悬浮条中，松手可上屏；上滑可改为输入到文本框或取消发送。",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Md2Switch(
                    checked = state.speakerVerifyEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            viewModel.setSpeakerVerifyEnabled(false)
                        } else if (state.speakerProfileReady) {
                            viewModel.setSpeakerVerifyEnabled(true)
                        } else {
                            speakerEnrollSamples.clear()
                            speakerEnrollStep = 0
                            speakerEnrollCountingDown = false
                            speakerEnrollCountdown = 3
                            speakerEnrollReading = false
                            speakerEnrollProgress = 0f
                            speakerEnrollLevel = 0f
                            speakerEnrollRemainingSec = 4f
                            speakerEnrollSuccess = false
                            speakerEnrollMessage = "请按页面引导完成注册。"
                            speakerEnrollRetryDialog = false
                            speakerEnrollOpenedByToggle = true
                            showSpeakerEnrollDialog = true
                        }
                    }
                )
                Text("说话人验证")
            }
            Text(
                "说话人注册：${state.speakerProfiles.size}/3",
                style = MaterialTheme.typography.bodySmall
            )
            state.speakerProfiles.forEachIndexed { idx, profile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    backgroundColor = md2CardContainerColor(),
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}. ${profile.name}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Md2IconButton(
                            icon = "delete",
                            contentDescription = "删除说话人",
                            onClick = { viewModel.removeSpeakerProfileAt(idx) }
                        )
                    }
                }
            }
            Text(
                "验证阈值：${String.format("%.2f", state.speakerVerifyThreshold)}",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = state.speakerVerifyThreshold,
                onValueChange = { viewModel.setSpeakerVerifyThreshold(it) },
                valueRange = 0.4f..0.95f
            )
            if (state.speakerLastSimilarity >= 0f) {
                Text(
                    "最近相似度：${String.format("%.2f", state.speakerLastSimilarity)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Md2OutlinedButton(onClick = {
                    speakerEnrollSamples.clear()
                    speakerEnrollStep = 0
                    speakerEnrollCountingDown = false
                    speakerEnrollCountdown = 3
                    speakerEnrollReading = false
                    speakerEnrollProgress = 0f
                    speakerEnrollLevel = 0f
                    speakerEnrollRemainingSec = 4f
                    speakerEnrollSuccess = false
                    speakerEnrollMessage = "请按页面引导完成注册。"
                    speakerEnrollRetryDialog = false
                    speakerEnrollOpenedByToggle = false
                    showSpeakerEnrollDialog = true
                }, enabled = viewModel.canAddSpeakerProfile()) {
                    Text("注册说话人")
                }
                Md2TextButton(onClick = { viewModel.clearSpeakerProfile() }) {
                    Text("清除注册")
                }
            }
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
            Text("播放音量倍率：${state.playbackGainPercent}%", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.playbackGainPercent.toFloat(),
                onValueChange = { viewModel.setPlaybackGainPercent(it.toInt()) },
                valueRange = 0f..1000f
            )
            Text("100% 为原始音量，拖动接近 100% 时会自动吸附。", style = MaterialTheme.typography.bodySmall)
            Text("音色随机度：${String.format("%.3f", state.piperNoiseScale)}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.piperNoiseScale,
                onValueChange = { viewModel.setPiperNoiseScale(it) },
                valueRange = 0f..2f
            )
            Text("语速倍率（越大越慢）：${String.format("%.3f", state.piperLengthScale)}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.piperLengthScale,
                onValueChange = { viewModel.setPiperLengthScale(it) },
                valueRange = 0.1f..5f
            )
            Text("句末停顿时长：${String.format("%.2f", state.piperSentenceSilence)}s", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.piperSentenceSilence,
                onValueChange = { viewModel.setPiperSentenceSilence(it) },
                valueRange = 0f..2f
            )
            val numberReplaceOptions = listOf("不替换", "数字替换为中文字符", "数字替换为中文表达")
            var numberReplaceExpanded by remember { mutableStateOf(false) }
            Text("数字替换", fontWeight = FontWeight.Bold)
            Box {
                val label = numberReplaceOptions.getOrElse(state.numberReplaceMode) { numberReplaceOptions[0] }
                Md2DropdownButton(
                    label = label,
                    onClick = { numberReplaceExpanded = true },
                    expanded = numberReplaceExpanded
                )
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
        }

        Md2StaggeredFloatIn(index = 4) {
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
        }

        Md2StaggeredFloatIn(index = 5) {
            Md2SettingsCard(title = "设备路由") {
            Text("优先选择的音频输入设备类型", style = MaterialTheme.typography.bodySmall)
            Box {
                val label = inputTypeOptions.firstOrNull { it.first == state.preferredInputType }?.second
                    ?: inputTypeOptions.first().second
                Md2DropdownButton(
                    label = label,
                    onClick = { inputTypeExpanded = true },
                    expanded = inputTypeExpanded
                )
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
                val label = outputTypeOptions.firstOrNull { it.first == state.preferredOutputType }?.second
                    ?: outputTypeOptions.first().second
                Md2DropdownButton(
                    label = label,
                    onClick = { outputTypeExpanded = true },
                    expanded = outputTypeExpanded
                )
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
        }

        Spacer(Modifier.height(UiTokens.PageBottomBlank))
    }

    if (showSpeakerEnrollDialog) {
        val canDismiss = !(speakerEnrollReading || speakerEnrollCountingDown)
        AlertDialog(
            onDismissRequest = {
                if (canDismiss) {
                    closeSpeakerEnrollDialog()
                }
            },
            title = {
                val title = when (speakerEnrollStep) {
                    0 -> "说话人注册（准备）"
                    1, 2, 3 -> "说话人注册（第 ${speakerEnrollStep} 句）"
                    else -> "说话人注册（结果）"
                }
                Text(title)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (speakerEnrollStep) {
                        0 -> {
                            Text("请按顺序朗读三句，每句约 4 秒。")
                            Text("环境尽量安静，手机靠近说话人。", style = MaterialTheme.typography.bodySmall)
                            Text("第一页仅说明，点击“下一步”开始。", style = MaterialTheme.typography.bodySmall)
                        }
                        1, 2, 3 -> {
                            val phrase = speakerEnrollTexts[speakerEnrollStep - 1]
                            Text("请朗读以下文本：")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                backgroundColor = md2CardContainerColor()
                            ) {
                                Text(
                                    text = phrase,
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            if (speakerEnrollCountingDown) {
                                Text(
                                    "倒计时：${speakerEnrollCountdown}",
                                    style = MaterialTheme.typography.h4,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "倒计时结束后将自动开始录音。",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (speakerEnrollReading) {
                                Text(
                                    "录制中，剩余 ${String.format(Locale.US, "%.1f", speakerEnrollRemainingSec)}s",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                LinearProgressIndicator(
                                    progress = speakerEnrollProgress.coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("实时音量", style = MaterialTheme.typography.bodySmall)
                                LinearProgressIndicator(
                                    progress = speakerEnrollLevel.coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    "点击“开始朗读”后会先倒计时，再开始计时录音。",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        else -> {
                            Text(if (speakerEnrollSuccess) "注册完成" else "注册失败")
                            Text(speakerEnrollMessage, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "你可以直接完成，或重新打开注册流程重录。",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                when (speakerEnrollStep) {
                    0 -> {
                        TextButton(onClick = { speakerEnrollStep = 1 }) {
                            Text("下一步")
                        }
                    }
                    1, 2, 3 -> {
                        TextButton(
                            onClick = { startSpeakerEnrollStepCapture(speakerEnrollStep) },
                            enabled = !(speakerEnrollReading || speakerEnrollCountingDown)
                        ) {
                            Text(
                                when {
                                    speakerEnrollCountingDown -> "倒计时中..."
                                    speakerEnrollReading -> "录制中..."
                                    else -> "开始朗读"
                                }
                            )
                        }
                    }
                    else -> {
                        TextButton(onClick = { closeSpeakerEnrollDialog() }) {
                            Text("完成")
                        }
                    }
                }
            },
            dismissButton = {
                when (speakerEnrollStep) {
                    0, 1, 2, 3 -> {
                        TextButton(
                            onClick = { closeSpeakerEnrollDialog() },
                            enabled = !(speakerEnrollReading || speakerEnrollCountingDown)
                        ) {
                            Text("取消")
                        }
                    }
                    else -> {
                        TextButton(onClick = {
                            speakerEnrollSamples.clear()
                            speakerEnrollStep = 0
                            speakerEnrollCountingDown = false
                            speakerEnrollCountdown = 3
                            speakerEnrollReading = false
                            speakerEnrollProgress = 0f
                            speakerEnrollLevel = 0f
                            speakerEnrollRemainingSec = 4f
                            speakerEnrollSuccess = false
                            speakerEnrollMessage = "请按页面引导完成注册。"
                            speakerEnrollRetryDialog = false
                        }) {
                            Text("重新注册")
                        }
                    }
                }
            }
        )
    }

    if (speakerEnrollRetryDialog) {
        AlertDialog(
            onDismissRequest = { speakerEnrollRetryDialog = false },
            title = { Text("录制失败") },
            text = { Text("${speakerEnrollMessage}\n请重录当前句子。") },
            confirmButton = {
                TextButton(onClick = { speakerEnrollRetryDialog = false }) {
                    Text("重录")
                }
            }
        )
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
                Md2CardTitleText(title)
                content()
            }
        )
    }
}

@Composable
fun LogScreen(
    onTopBarActionsChange: (LogTopBarActions?) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var logs by remember { mutableStateOf<List<File>>(emptyList()) }
    var selected by remember { mutableStateOf<File?>(null) }
    var content by remember { mutableStateOf("加载中...") }
    var expanded by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val onTopBarActionsChangeState = rememberUpdatedState(onTopBarActionsChange)

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

    SideEffect {
        onTopBarActionsChangeState.value(
            LogTopBarActions(
                onRefresh = { refreshLogs() },
                onCopy = {
                    clipboard.setText(AnnotatedString(content))
                    toast(context, "已复制")
                },
                onShare = {
                    val file = selected
                    if (file != null) {
                        shareLogFile(context, file)
                    } else {
                        toast(context, "暂无可分享日志")
                    }
                },
                canCopy = content.isNotBlank(),
                canShare = selected != null
            )
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            onTopBarActionsChangeState.value(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(UiTokens.PageTopBlank))
        Md2StaggeredFloatIn(index = 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    Md2DropdownButton(
                        label = selected?.name ?: "选择日志",
                        onClick = { expanded = true },
                        expanded = expanded
                    )
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
            }
        }
        Md2StaggeredFloatIn(index = 1) {
            if (selected != null) {
                Text("路径：${selected!!.absolutePath}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Divider()
        Md2StaggeredFloatIn(
            index = 2,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
            ) {
                Text(
                    content,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(UiTokens.PageBottomBlank))
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

private fun sharePlainText(context: Context, content: String, chooserTitle: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (e: Exception) {
        toast(context, "分享失败: ${e.message}")
    }
}
