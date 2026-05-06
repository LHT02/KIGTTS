@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.lhtstudio.kigtts.app.ui

import android.annotation.SuppressLint
import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.NumberPicker
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.lhtstudio.kigtts.app.R
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.lhtstudio.kigtts.app.audio.AudioRoutePreference
import com.lhtstudio.kigtts.app.audio.AudioDenoiserMode
import com.lhtstudio.kigtts.app.audio.AudioLoopbackTester
import com.lhtstudio.kigtts.app.audio.AudioTestConfig
import com.lhtstudio.kigtts.app.audio.SoundboardManager
import com.lhtstudio.kigtts.app.audio.SoundboardPlaybackState
import com.lhtstudio.kigtts.app.audio.SpeechEnhancementMode
import com.lhtstudio.kigtts.app.audio.SpeakerEnrollResult
import com.lhtstudio.kigtts.app.audio.VadMode
import com.lhtstudio.kigtts.app.data.ModelRepository
import com.lhtstudio.kigtts.app.data.RecognitionResourceProgress
import com.lhtstudio.kigtts.app.data.RecognitionResourceStatus
import com.lhtstudio.kigtts.app.data.KokoroVoiceStatus
import com.lhtstudio.kigtts.app.data.SoundboardGroup
import com.lhtstudio.kigtts.app.data.SoundboardItem
import com.lhtstudio.kigtts.app.data.SoundboardLayoutMode
import com.lhtstudio.kigtts.app.data.SoundboardConfig
import com.lhtstudio.kigtts.app.data.SoundboardPresetIo
import com.lhtstudio.kigtts.app.data.KOKORO_VOICE_NAME
import com.lhtstudio.kigtts.app.data.SYSTEM_TTS_VOICE_NAME
import com.lhtstudio.kigtts.app.data.VoicePackInfo
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.data.VoicePackMeta
import com.lhtstudio.kigtts.app.data.defaultSoundboardGroups
import com.lhtstudio.kigtts.app.data.isKokoroVoiceDir
import com.lhtstudio.kigtts.app.data.isSystemTtsVoiceDir
import com.lhtstudio.kigtts.app.data.parseSoundboardConfig
import com.lhtstudio.kigtts.app.data.serializeSoundboardConfig
import com.lhtstudio.kigtts.app.data.uniqueImportedGroupTitle
import com.lhtstudio.kigtts.app.overlay.FloatingOverlayService
import com.lhtstudio.kigtts.app.overlay.OverlayBridge
import com.lhtstudio.kigtts.app.overlay.RealtimeOwnerGate
import com.lhtstudio.kigtts.app.overlay.RealtimeRuntimeBridge
import com.lhtstudio.kigtts.app.service.KeepAliveService
import com.lhtstudio.kigtts.app.service.RealtimeHostService
import com.lhtstudio.kigtts.app.service.VolumeHotkeyAccessibilityGuideService
import com.lhtstudio.kigtts.app.service.VolumeHotkeyAccessibilityService
import com.lhtstudio.kigtts.app.service.VolumeHotkeyService
import com.lhtstudio.kigtts.app.util.AlipayScannerSupport
import com.lhtstudio.kigtts.app.util.AppLogger
import com.lhtstudio.kigtts.app.util.BluetoothMediaTitleBridge
import com.lhtstudio.kigtts.app.util.ExternalShortcutCatalog
import com.lhtstudio.kigtts.app.util.ExternalShortcutChoice
import com.lhtstudio.kigtts.app.util.LauncherMenuShortcuts
import com.lhtstudio.kigtts.app.util.QqScannerSupport
import com.lhtstudio.kigtts.app.util.QuickCardRenderCache
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActionSpec
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActions
import com.lhtstudio.kigtts.app.util.VolumeHotkeySequence
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.InvertedLuminanceSource
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
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
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
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

private val qrDecodeHints: Map<DecodeHintType, Any> = mapOf(
    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    DecodeHintType.TRY_HARDER to true
)

private fun createQrMlKitScanner(): BarcodeScanner {
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAllPotentialBarcodes()
        .build()
    return BarcodeScanning.getClient(options)
}

private fun Iterable<Barcode>.decodedQrTexts(): List<String> {
    return mapNotNull { barcode ->
        barcode.rawValue?.trim()?.takeIf { it.isNotEmpty() }
            ?: barcode.displayValue?.trim()?.takeIf { it.isNotEmpty() }
    }.distinct()
}

private fun Iterable<Barcode>.firstDecodedQrText(): String? {
    return decodedQrTexts().firstOrNull()
}

private fun decodeQrWithZxing(source: LuminanceSource): String? {
    val variants = arrayOf(
        BinaryBitmap(HybridBinarizer(source)),
        BinaryBitmap(GlobalHistogramBinarizer(source)),
        BinaryBitmap(HybridBinarizer(InvertedLuminanceSource(source))),
        BinaryBitmap(GlobalHistogramBinarizer(InvertedLuminanceSource(source)))
    )
    for (bitmap in variants) {
        val reader = MultiFormatReader().apply { setHints(qrDecodeHints) }
        val text = runCatching { reader.decodeWithState(bitmap)?.text }
            .getOrNull()
            ?.trim()
            .takeIf { !it.isNullOrEmpty() }
        reader.reset()
        if (text != null) return text
    }
    return null
}

private suspend fun <T> awaitTask(task: Task<T>): T? = suspendCancellableCoroutine { cont ->
    task.addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    task.addOnFailureListener {
        if (cont.isActive) cont.resume(null)
    }
    task.addOnCanceledListener {
        if (cont.isActive) cont.resume(null)
    }
}

private const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
private const val WECHAT_LAUNCHER_ACTIVITY = "com.tencent.mm.ui.LauncherUI"
private const val WECHAT_BROWSER_FALLBACK_URL = "https://weixin.qq.com/"

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

private fun isWeChatQrContent(raw: String): Boolean {
    val text = raw.trim()
    if (text.isEmpty()) return false
    val parsed = runCatching { Uri.parse(text) }.getOrNull()
    val scheme = parsed?.scheme?.lowercase(Locale.US).orEmpty()
    val host = parsed?.host?.lowercase(Locale.US).orEmpty()
    if (scheme == "weixin" || scheme == "wxp" || scheme == "wxpay") return true
    if (host == "weixin.qq.com" || host.endsWith(".weixin.qq.com")) return true
    if (host == "u.wechat.com" || host.endsWith(".u.wechat.com")) return true
    if (host == "wx.tenpay.com" || host.endsWith(".wx.tenpay.com")) return true
    if (host == "payapp.weixin.qq.com" || host.endsWith(".payapp.weixin.qq.com")) return true
    val lower = text.lowercase(Locale.US)
    return lower.startsWith("https://u.wechat.com/") ||
        lower.startsWith("http://u.wechat.com/") ||
        lower.startsWith("https://weixin.qq.com/") ||
        lower.startsWith("http://weixin.qq.com/") ||
        lower.startsWith("https://wx.tenpay.com/") ||
        lower.startsWith("http://wx.tenpay.com/")
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrElse { false }
}

private fun launchWeChatScanner(context: Context): Boolean {
    val explicit = Intent().apply {
        setClassName(WECHAT_PACKAGE_NAME, WECHAT_LAUNCHER_ACTIVITY)
        putExtra("LauncherUI.From.Scaner.Shortcut", true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (runCatching {
            context.startActivity(explicit)
            true
        }.getOrDefault(false)
    ) {
        return true
    }
    val launchIntent = context.packageManager.getLaunchIntentForPackage(WECHAT_PACKAGE_NAME)?.apply {
        putExtra("LauncherUI.From.Scaner.Shortcut", true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }.getOrDefault(false)
}

private fun openExternalBrowser(context: Context, url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
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

private const val SYSTEM_TTS_DEFAULT_LABEL = "系统 TTS"
private const val SYSTEM_TTS_DEFAULT_REMARK = "使用 Android 系统语音服务"

private fun sortVoicePacks(items: List<VoicePackInfo>): List<VoicePackInfo> {
    return items.sortedWith(
        compareByDescending<VoicePackInfo> { it.meta.pinned }
            .thenBy { it.meta.order }
            .thenBy { it.meta.name }
    )
}

data class UiState(
    val asrDir: File? = null,
    val voiceDir: File? = null,
    val voicePacks: List<VoicePackInfo> = emptyList(),
    val recognized: List<RecognizedItem> = emptyList(),
    val running: Boolean = false,
    val status: String = "待命",
    val muteWhilePlaying: Boolean = true,
    val muteWhilePlayingDelaySec: Float = 0.2f,
    val echoSuppression: Boolean = false,
    val communicationMode: Boolean = false,
    val preferredInputType: Int = AudioRoutePreference.INPUT_AUTO,
    val preferredOutputType: Int = AudioRoutePreference.OUTPUT_AUTO,
    val aec3Enabled: Boolean = true,
    val denoiserMode: Int = AudioDenoiserMode.RNNOISE,
    val speechEnhancementMode: Int = SpeechEnhancementMode.DPDFNET4_STREAMING,
    val aec3Status: String = "未启用",
    val aec3Diag: String = "AEC3 诊断：未启用",
    val classicVadEnabled: Boolean = false,
    val sileroVadEnabled: Boolean = true,
    val sileroVadThreshold: Float = UserPrefs.SILERO_VAD_DEFAULT_THRESHOLD,
    val sileroVadPreRollMs: Int = UserPrefs.SILERO_VAD_DEFAULT_PRE_ROLL_MS,
    val recognitionResourceInstalled: Boolean = false,
    val recognitionResourceName: String = "未安装",
    val recognitionResourceVersion: String = "",
    val recognitionResourceStatus: String = "未安装语音识别资源包",
    val recognitionResourceBusy: Boolean = false,
    val recognitionResourceProgressStage: String = "",
    val recognitionResourceProgress: Float = -1f,
    val recognitionResourceModelScopeUrl: String = UserPrefs.DEFAULT_RECOGNITION_RESOURCE_MODELSCOPE_URL,
    val recognitionResourceHuggingFaceUrl: String = UserPrefs.DEFAULT_RECOGNITION_RESOURCE_HUGGINGFACE_URL,
    val recognitionResourcePreferredSource: Int = UserPrefs.RECOGNITION_RESOURCE_SOURCE_MODELSCOPE,
    val kokoroInstalled: Boolean = false,
    val kokoroStatus: String = "未安装 Kokoro 语音包",
    val kokoroBusy: Boolean = false,
    val kokoroProgressStage: String = "",
    val kokoroProgress: Float = -1f,
    val kokoroHfUrl: String = UserPrefs.DEFAULT_KOKORO_HF_URL,
    val kokoroHfMirrorUrl: String = UserPrefs.DEFAULT_KOKORO_HFMIRROR_URL,
    val kokoroModelScopeUrl: String = UserPrefs.DEFAULT_KOKORO_MODELSCOPE_URL,
    val kokoroPreferredSource: Int = UserPrefs.KOKORO_SOURCE_MODELSCOPE,
    val kokoroSpeakerId: Int = UserPrefs.KOKORO_DEFAULT_SPEAKER_ID,
    val minVolumePercent: Int = 2,
    val playbackGainPercent: Int = 100,
    val piperNoiseScale: Float = 0.667f,
    val piperLengthScale: Float = 1.0f,
    val piperNoiseW: Float = 0.8f,
    val piperSentenceSilence: Float = 0.2f,
    val keepAlive: Boolean = true,
    val numberReplaceMode: Int = 0,
    val landscapeDrawerMode: Int = UserPrefs.DRAWER_MODE_PERMANENT,
    val solidTopBar: Boolean = true,
    val themeMode: Int = UserPrefs.THEME_MODE_FOLLOW_SYSTEM,
    val overlayThemeMode: Int = UserPrefs.THEME_MODE_FOLLOW_SYSTEM,
    val fontScaleBlockMode: Int = UserPrefs.FONT_SCALE_BLOCK_ICONS_ONLY,
    val hapticFeedbackEnabled: Boolean = true,
    val drawingSaveRelativePath: String = UserPrefs.DEFAULT_DRAWING_SAVE_RELATIVE_PATH,
    val quickCardAutoSaveOnExit: Boolean = false,
    val useBuiltinFileManager: Boolean = true,
    val useBuiltinGallery: Boolean = true,
    val asrSendToQuickSubtitle: Boolean = true,
    val pushToTalkMode: Boolean = false,
    val pushToTalkConfirmInputMode: Boolean = false,
    val floatingOverlayEnabled: Boolean = false,
    val floatingOverlayAutoDock: Boolean = true,
    val floatingOverlayShowOnLockScreen: Boolean = false,
    val floatingOverlayHardcodedShortcutSupplement: Boolean = false,
    val volumeHotkeyUpDownEnabled: Boolean = false,
    val volumeHotkeyDownUpEnabled: Boolean = false,
    val volumeHotkeyWindowMs: Int = UserPrefs.VOLUME_HOTKEY_DEFAULT_WINDOW_MS,
    val volumeHotkeyAccessibilityEnabled: Boolean = false,
    val volumeHotkeyEnableWarningDismissed: Boolean = false,
    val volumeHotkeyUpDownAction: VolumeHotkeyActionSpec =
        VolumeHotkeyActions.defaultFor(VolumeHotkeySequence.UpDown),
    val volumeHotkeyDownUpAction: VolumeHotkeyActionSpec =
        VolumeHotkeyActions.defaultFor(VolumeHotkeySequence.DownUp),
    val ttsDisabled: Boolean = false,
    val soundboardKeywordTriggerEnabled: Boolean = false,
    val soundboardSuppressTtsOnKeyword: Boolean = false,
    val allowQuickTextTriggerSoundboard: Boolean = false,
    val quickSubtitleInterruptQueue: Boolean = true,
    val quickSubtitleAutoFit: Boolean = true,
    val quickSubtitleCompactControls: Boolean = false,
    val quickSubtitleKeepInputPreview: Boolean = true,
    val bluetoothMediaTitleSubtitle: Boolean = false,
    val drawingKeepCanvasOrientationToDevice: Boolean = true,
    val pushToTalkPressed: Boolean = false,
    val pushToTalkStreamingText: String = "",
    val speakerVerifyEnabled: Boolean = false,
    val speakerVerifyThreshold: Float = 0.5f,
    val speakerProfileReady: Boolean = false,
    val speakerProfiles: List<SpeakerProfileUiItem> = emptyList(),
    val speakerLastSimilarity: Float = -1f,
    val inputLevel: Float = 0f,
    val audioTestRecording: Boolean = false,
    val audioTestPlaying: Boolean = false,
    val audioTestHasClip: Boolean = false,
    val audioTestLevel: Float = 0f,
    val audioTestStatus: String = "未录制测试音频",
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
    val text: String,
    val navigateToPage: Boolean = true
)

data class ExternalRecordAudioPermissionRequest(
    val requestId: Long,
    val startRealtimeOnGrant: Boolean = false
)

data class ExternalAccessibilityExplainRequest(
    val requestId: Long
)

data class ExternalVoicePackInstallRequest(
    val requestId: Long,
    val message: String
)

data class ExternalPresetInstallRequest(
    val requestId: Long,
    val target: PresetInstallTarget,
    val message: String
)

enum class PresetInstallTarget {
    QuickSubtitle,
    Soundboard
}

private fun isOverlayOpenTarget(target: String): Boolean {
    return target == OverlayBridge.TARGET_OPEN ||
            target == OverlayBridge.TARGET_OPEN_OVERLAY ||
            target == OverlayBridge.TARGET_OPEN_QUICK_CARD ||
            target == OverlayBridge.TARGET_OPEN_DRAWING ||
            target == OverlayBridge.TARGET_OPEN_SOUNDBOARD ||
            target == OverlayBridge.TARGET_OPEN_VOICE_PACK ||
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

private val QuickSubtitleGroupIconChoices = listOf(
    "sentiment_satisfied",
    "sentiment_very_satisfied",
    "sentiment_neutral",
    "sentiment_dissatisfied",
    "record_voice_over",
    "chat",
    "forum",
    "sms",
    "alternate_email",
    "emoji_people",
    "person",
    "groups",
    "accessibility_new",
    "support_agent",
    "translate",
    "work",
    "school",
    "home",
    "restaurant",
    "shopping_bag",
    "local_hospital",
    "directions_car",
    "train",
    "flight",
    "location_on",
    "schedule",
    "event",
    "payments",
    "sports_esports",
    "favorite",
    "thumb_up",
    "handshake",
    "celebration",
    "pets",
    "info",
    "warning"
)

private val SoundboardGroupIconChoices = listOf(
    "music_note",
    "library_music",
    "queue_music",
    "album",
    "graphic_eq",
    "equalizer",
    "volume_up",
    "campaign",
    "mic",
    "record_voice_over",
    "radio",
    "piano",
    "notifications",
    "alarm",
    "celebration",
    "movie",
    "theaters",
    "sports_esports",
    "sports_soccer",
    "directions_run",
    "emoji_events",
    "bolt",
    "rocket_launch",
    "mood",
    "favorite",
    "chat",
    "work",
    "school",
    "restaurant",
    "pets"
)

class MainViewModel(
    private val repo: ModelRepository,
    private val appContext: ComponentActivity
) : ViewModel() {
    private data class OverlayShortcutSeedEntry(
        val packageName: String,
        val className: String,
        val label: String
    )

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
    var pendingVoicePackInstallRequest by mutableStateOf<ExternalVoicePackInstallRequest?>(null)
        private set
    var pendingPresetInstallRequest by mutableStateOf<ExternalPresetInstallRequest?>(null)
        private set
    var pendingRecordAudioPermissionRequest by mutableStateOf<ExternalRecordAudioPermissionRequest?>(null)
        private set
    var pendingAccessibilityExplainRequest by mutableStateOf<ExternalAccessibilityExplainRequest?>(null)
        private set

    private var realtimeHost: RealtimeHostService? = null
    private var hostStateJob: Job? = null
    private var hostQuickSubtitleJob: Job? = null
    private var pendingHostAsrDir: File? = null
    private var pendingHostVoiceDir: File? = null
    private var pendingHostStartRequest = false

    private val audioTest = AudioLoopbackTester(appContext, viewModelScope) { snapshot ->
        viewModelScope.launch(Dispatchers.Main) {
            uiState = uiState.copy(
                audioTestRecording = snapshot.recording,
                audioTestPlaying = snapshot.playing,
                audioTestHasClip = snapshot.hasClip,
                audioTestLevel = snapshot.level,
                audioTestStatus = snapshot.status
            )
        }
    }
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
    private val quickSubtitleInterruptRequestSerial = AtomicLong(0L)

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

    private fun appendRecognizedHistory(text: String, id: Long? = null, fromQuickText: Boolean = false) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        val historyId = id ?: manualRecognizedIdSeed--
        if (id != null && realtimeRecognized.any { it.id == id }) return
        val item = RecognizedItem(id = historyId, text = normalized)
        val next = (listOf(item) + realtimeRecognized).take(MAX_RECOGNIZED_ITEMS)
        realtimeRecognized = next
        BluetoothMediaTitleBridge.updateSubtitle(appContext, normalized)
        val validIds = next.asSequence().map { it.id }.toSet()
        lastProgressUpdateAtMs.keys.retainAll(validIds)
        maybeTriggerSoundboardFromText(normalized, fromQuickText = fromQuickText)
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
    var quickSubtitleContentRevision by mutableLongStateOf(0L)
        private set
    var quickSubtitlePlayOnSend by mutableStateOf(true)
        private set
    var quickSubtitleInputCollapsed by mutableStateOf(false)
        private set
    var quickSubtitleBold by mutableStateOf(true)
        private set
    var quickSubtitleCentered by mutableStateOf(false)
        private set
    var quickSubtitleRotated180 by mutableStateOf(false)
        private set
    var quickSubtitleShowActionButtons by mutableStateOf(true)
        private set
    var quickSubtitleFontSizeSp by mutableFloatStateOf(56f)
        private set
    var quickSubtitlePreviewVisible by mutableStateOf(false)
        private set
    var soundboardGroups by mutableStateOf(defaultSoundboardGroups())
        private set
    var soundboardSelectedGroupId by mutableLongStateOf(1L)
        private set
    var soundboardPortraitLayout by mutableStateOf(SoundboardLayoutMode.List)
        private set
    var soundboardLandscapeLayout by mutableStateOf(SoundboardLayoutMode.Grid5)
        private set
    var soundboardPlaybackStates by mutableStateOf<Map<Long, SoundboardPlaybackState>>(emptyMap())
        private set
    var settingsSelectedCategoryName by mutableStateOf(SettingsCategory.Recognition.name)
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
    var drawingManualRotationQuarterTurns by mutableIntStateOf(0)
        private set
    private var quickSubtitleNextGroupId = 4L
    private var quickSubtitleSaving = false
    private var soundboardNextGroupId = 2L
    private var soundboardNextItemId = 1L
    private var soundboardSaving = false
    private var pendingSoundboardSavePayload: String? = null
    private var quickCardsNextId = 1L
    private var quickCardsSaving = false

    init {
        loadQuickSubtitleConfig()
        loadSoundboardConfig()
        loadQuickCardConfig()
        refreshRecognitionResourceStatus()
        refreshKokoroVoiceStatus()
        observeSoundboardPlayback()
        observeSettingsChanges()
    }

    fun ensureInitialFloatingOverlayShortcuts() {
        viewModelScope.launch(Dispatchers.IO) {
            if (UserPrefs.isFloatingOverlayDefaultShortcutsSeeded(appContext)) return@launch
            val existing = UserPrefs.getFloatingOverlayShortcuts(appContext)
            if (!existing.isNullOrBlank()) {
                UserPrefs.setFloatingOverlayDefaultShortcutsSeeded(appContext, true)
                return@launch
            }
            val seeded = buildInitialFloatingOverlayShortcuts()
            if (seeded.isNotEmpty()) {
                val payload = JSONArray().apply {
                    seeded.forEach { item ->
                        put(
                            JSONObject().apply {
                                put("packageName", item.packageName)
                                put("className", item.className)
                                put("label", item.label)
                            }
                        )
                    }
                }.toString()
                UserPrefs.setFloatingOverlayShortcuts(appContext, payload)
            }
            UserPrefs.setFloatingOverlayDefaultShortcutsSeeded(appContext, true)
        }
    }

    fun attachRealtimeHost(service: RealtimeHostService) {
        if (realtimeHost === service) return
        detachRealtimeHost()
        realtimeHost = service
        service.setQuickSubtitlePlayOnSend(quickSubtitlePlayOnSend)
        val attachedSpeakerSimilarity = service.getSpeakerLastSimilarity()
        if (attachedSpeakerSimilarity >= 0f) {
            uiState = uiState.copy(speakerLastSimilarity = attachedSpeakerSimilarity)
        }
        hostStateJob = viewModelScope.launch {
            service.stateFlow().collectLatest { snapshot ->
                realtimeRecognized = snapshot.recognized
                realtimeInputLevel = snapshot.inputLevel.coerceIn(0f, 1f)
                realtimePlaybackProgress = snapshot.playbackProgress.coerceIn(0f, 1f)
                val previousSpeakerSimilarity = uiState.speakerLastSimilarity
                uiState = uiState.copy(
                    asrDir = snapshot.asrDir,
                    voiceDir = snapshot.voiceDir,
                    recognized = snapshot.recognized,
                    running = snapshot.running,
                    status = if (snapshot.status.isNotBlank()) snapshot.status else uiState.status,
                    aec3Status = snapshot.aec3Status,
                    aec3Diag = snapshot.aec3Diag,
                    pushToTalkPressed = snapshot.pushToTalkPressed,
                    pushToTalkStreamingText = snapshot.pushToTalkStreamingText,
                    speakerLastSimilarity = if (snapshot.speakerLastSimilarity >= 0f) {
                        snapshot.speakerLastSimilarity
                    } else {
                        previousSpeakerSimilarity
                    },
                    inputDeviceLabel = snapshot.inputDeviceLabel.ifBlank { uiState.inputDeviceLabel },
                    outputDeviceLabel = snapshot.outputDeviceLabel.ifBlank { uiState.outputDeviceLabel }
                )
            }
        }
        hostQuickSubtitleJob = viewModelScope.launch {
            service.quickSubtitleRequestFlow().collectLatest { request ->
                pendingQuickSubtitleLaunchRequest = request
            }
        }
        pendingHostAsrDir?.let { dir ->
            viewModelScope.launch {
                service.updateSelectedAsrDir(dir, preload = true)
                pendingHostAsrDir = null
            }
        }
        pendingHostVoiceDir?.let { dir ->
            viewModelScope.launch {
                service.updateSelectedVoiceDir(dir, preload = true)
                pendingHostVoiceDir = null
            }
        }
        if (pendingHostStartRequest) {
            pendingHostStartRequest = false
            service.startRealtime()
        }
        refreshVoicePacks()
    }

    fun detachRealtimeHost() {
        hostStateJob?.cancel()
        hostStateJob = null
        hostQuickSubtitleJob?.cancel()
        hostQuickSubtitleJob = null
        realtimeHost = null
    }

    private fun requestRealtimeHost(status: String? = null): RealtimeHostService? {
        val host = realtimeHost
        if (host != null) return host
        RealtimeHostService.ensureStarted(appContext)
        if (status != null) {
            uiState = uiState.copy(status = status)
        }
        return null
    }

    private fun buildInitialFloatingOverlayShortcuts(): List<OverlayShortcutSeedEntry> {
        val shortcuts = buildList {
            resolveLauncherShortcutForPackage("com.tencent.mobileqq")?.let(::add)
            resolveLauncherShortcutForPackage("com.tencent.mm")?.let(::add)
            resolveLauncherShortcutForPackage("com.eg.android.AlipayGphone")?.let(::add)
            resolveCameraLauncherShortcut()?.let(::add)
        }
        return shortcuts.distinctBy { "${it.packageName}/${it.className}" }
    }

    private fun resolveLauncherShortcutForPackage(packageName: String): OverlayShortcutSeedEntry? {
        val pm = appContext.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return null
        val component = launchIntent.component ?: return null
        val label = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
        }.mapCatching { info ->
            pm.getApplicationLabel(info).toString().trim()
        }.getOrDefault("")
        return OverlayShortcutSeedEntry(
            packageName = component.packageName,
            className = component.className,
            label = label.ifBlank { component.className.substringAfterLast('.') }
        )
    }

    private fun resolveCameraLauncherShortcut(): OverlayShortcutSeedEntry? {
        val pm = appContext.packageManager
        val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(cameraIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(cameraIntent, 0)
        } ?: return null
        val packageName = resolved.activityInfo?.packageName?.takeIf { it.isNotBlank() } ?: return null
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        val component = launchIntent?.component ?: ComponentName(
            packageName,
            resolved.activityInfo?.name?.takeIf { it.isNotBlank() } ?: return null
        )
        val label = resolved.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank {
            runCatching {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(packageName, 0)
                }
                pm.getApplicationLabel(info).toString().trim()
            }.getOrDefault("")
        }
        return OverlayShortcutSeedEntry(
            packageName = component.packageName,
            className = component.className,
            label = label.ifBlank { component.className.substringAfterLast('.') }
        )
    }

    private fun applySettingsSnapshot(settings: UserPrefs.AppSettings) {
        FontScaleBlockRuntime.mode = settings.fontScaleBlockMode
        SoundboardManager.setPlaybackGainPercent(settings.playbackGainPercent)
        BluetoothMediaTitleBridge.setEnabled(appContext, settings.bluetoothMediaTitleSubtitle)
        if (settings.bluetoothMediaTitleSubtitle) {
            BluetoothMediaTitleBridge.updateSubtitle(appContext, quickSubtitleCurrentText)
        }
        val needsSpeakerBackendReset =
            settings.speakerVerifyBackendVersion != UserPrefs.SPEAKER_VERIFY_BACKEND_SHERPA_V1 &&
                    (settings.speakerVerifyEnabled || settings.speakerVerifyProfileCsv.isNotBlank())
        if (needsSpeakerBackendReset) {
            viewModelScope.launch(Dispatchers.IO) {
                UserPrefs.resetSpeakerVerifyBackend(appContext, enabled = false)
            }
        }
        speakerProfiles = if (needsSpeakerBackendReset) {
            mutableListOf()
        } else {
            UserPrefs.parseSpeakerVerifyProfiles(settings.speakerVerifyProfileCsv)
                .take(MAX_SPEAKER_PROFILES)
                .toMutableList()
        }
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
            denoiserMode = settings.denoiserMode,
            speechEnhancementMode = settings.speechEnhancementMode,
            aec3Status = nextAec3Status,
            aec3Diag = nextAec3Diag,
            classicVadEnabled = settings.classicVadEnabled,
            sileroVadEnabled = settings.sileroVadEnabled,
            sileroVadThreshold = settings.sileroVadThreshold,
            sileroVadPreRollMs = settings.sileroVadPreRollMs,
            recognitionResourceModelScopeUrl = settings.recognitionResourceModelScopeUrl,
            recognitionResourceHuggingFaceUrl = settings.recognitionResourceHuggingFaceUrl,
            recognitionResourcePreferredSource = settings.recognitionResourcePreferredSource,
            kokoroHfUrl = settings.kokoroHfUrl,
            kokoroHfMirrorUrl = settings.kokoroHfMirrorUrl,
            kokoroModelScopeUrl = settings.kokoroModelScopeUrl,
            kokoroPreferredSource = settings.kokoroPreferredSource,
            kokoroSpeakerId = settings.kokoroSpeakerId,
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
            themeMode = settings.themeMode,
            overlayThemeMode = settings.overlayThemeMode,
            fontScaleBlockMode = settings.fontScaleBlockMode,
            hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
            drawingSaveRelativePath = normalizeDrawingSaveRelativePath(settings.drawingSaveRelativePath),
            quickCardAutoSaveOnExit = settings.quickCardAutoSaveOnExit,
            useBuiltinFileManager = settings.useBuiltinFileManager,
            useBuiltinGallery = settings.useBuiltinGallery,
            asrSendToQuickSubtitle = settings.asrSendToQuickSubtitle,
            pushToTalkMode = settings.pushToTalkMode,
            pushToTalkConfirmInputMode = settings.pushToTalkConfirmInput,
            floatingOverlayEnabled = settings.floatingOverlayEnabled,
            floatingOverlayAutoDock = settings.floatingOverlayAutoDock,
            floatingOverlayShowOnLockScreen = settings.floatingOverlayShowOnLockScreen,
            floatingOverlayHardcodedShortcutSupplement =
                settings.floatingOverlayHardcodedShortcutSupplement,
            volumeHotkeyUpDownEnabled = settings.volumeHotkeyUpDownEnabled,
            volumeHotkeyDownUpEnabled = settings.volumeHotkeyDownUpEnabled,
            volumeHotkeyWindowMs = settings.volumeHotkeyWindowMs,
            volumeHotkeyAccessibilityEnabled = settings.volumeHotkeyAccessibilityEnabled,
            volumeHotkeyEnableWarningDismissed = settings.volumeHotkeyEnableWarningDismissed,
            volumeHotkeyUpDownAction = settings.volumeHotkeyUpDownAction,
            volumeHotkeyDownUpAction = settings.volumeHotkeyDownUpAction,
            ttsDisabled = settings.ttsDisabled,
            soundboardKeywordTriggerEnabled = settings.soundboardKeywordTriggerEnabled,
            soundboardSuppressTtsOnKeyword = settings.soundboardSuppressTtsOnKeyword,
            allowQuickTextTriggerSoundboard = settings.allowQuickTextTriggerSoundboard,
            quickSubtitleInterruptQueue = settings.quickSubtitleInterruptQueue,
            quickSubtitleAutoFit = settings.quickSubtitleAutoFit,
            quickSubtitleCompactControls = settings.quickSubtitleCompactControls,
            quickSubtitleKeepInputPreview = settings.quickSubtitleKeepInputPreview,
            bluetoothMediaTitleSubtitle = settings.bluetoothMediaTitleSubtitle,
            drawingKeepCanvasOrientationToDevice = settings.drawingKeepCanvasOrientationToDevice,
            speakerVerifyEnabled = speakerVerifyEnabled,
            speakerVerifyThreshold = settings.speakerVerifyThreshold,
            speakerProfileReady = hasProfiles,
            speakerProfiles = speakerProfileUiItems(),
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
                VolumeHotkeyService.syncWithSettings(appContext)
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
            val title = g.optString("title", "").trim()
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
        val textRotated180 = root.optBoolean("textRotated180", false)
        val showActionButtons = root.optBoolean("showActionButtons", true)
        quickSubtitleGroups = finalGroups
        quickSubtitleSelectedGroupId =
            finalGroups.firstOrNull { it.id == selectedId }?.id ?: finalGroups.first().id
        quickSubtitleFontSizeSp = fontSize
        quickSubtitleCurrentText = currentText
        quickSubtitleInputText = inputText
        quickSubtitlePlayOnSend = playOnSend
        quickSubtitleBold = fontBold
        quickSubtitleCentered = textCentered
        quickSubtitleRotated180 = textRotated180
        quickSubtitleShowActionButtons = showActionButtons
        quickSubtitleNextGroupId = maxOf(maxId + 1L, (finalGroups.maxOfOrNull { it.id } ?: 0L) + 1L)
        if (uiState.bluetoothMediaTitleSubtitle) {
            BluetoothMediaTitleBridge.updateSubtitle(appContext, currentText)
        }
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
            put("textRotated180", quickSubtitleRotated180)
            put("showActionButtons", quickSubtitleShowActionButtons)
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

    private fun markQuickSubtitleContentSubmitted() {
        quickSubtitleContentRevision++
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
        BluetoothMediaTitleBridge.updateSubtitle(appContext, message)
        markQuickSubtitleContentSubmitted()
        if (enqueueSpeak) {
            speakText(
                message,
                fromQuickText = true,
                interruptCurrent = uiState.quickSubtitleInterruptQueue
            )
        } else {
            maybeTriggerSoundboardFromText(message, fromQuickText = true)
        }
        saveQuickSubtitleConfig()
    }

    fun submitQuickSubtitlePreset(
        text: String,
        hasVoice: Boolean = true,
        interruptCurrent: Boolean = uiState.quickSubtitleInterruptQueue
    ) {
        val message = text.trim()
        if (message.isEmpty()) return
        quickSubtitleCurrentText = message
        BluetoothMediaTitleBridge.updateSubtitle(appContext, message)
        markQuickSubtitleContentSubmitted()
        if (quickSubtitlePlayOnSend && hasVoice) {
            speakText(
                message,
                fromQuickText = true,
                interruptCurrent = interruptCurrent
            )
        } else {
            maybeTriggerSoundboardFromText(message, fromQuickText = true)
            uiState = uiState.copy(status = "已更新字幕文本")
        }
        saveQuickSubtitleConfig()
    }

    fun updateQuickSubtitleInputText(text: String) {
        quickSubtitleInputText = text
    }

    fun submitQuickSubtitleInput(playVoice: Boolean = quickSubtitlePlayOnSend) {
        val message = quickSubtitleInputText.trim()
        if (message.isEmpty()) return
        quickSubtitleCurrentText = message
        BluetoothMediaTitleBridge.updateSubtitle(appContext, message)
        markQuickSubtitleContentSubmitted()
        quickSubtitleInputText = ""
        if (playVoice) {
            speakText(
                message,
                fromQuickText = true,
                interruptCurrent = uiState.quickSubtitleInterruptQueue
            )
        } else {
            maybeTriggerSoundboardFromText(message, fromQuickText = true)
            uiState = uiState.copy(status = "已更新字幕文本")
        }
        saveQuickSubtitleConfig()
    }

    fun handleQuickSubtitleLaunchRequest(
        requestId: Long,
        target: String,
        text: String,
        navigateToPage: Boolean = true
    ) {
        val normalized = text.trim()
        val openPageTarget = isOverlayOpenTarget(target)
        val effectiveRequestId = if (openPageTarget) {
            nextQuickSubtitleLaunchRequestId()
        } else {
            requestId
        }
        if (effectiveRequestId == Long.MIN_VALUE) return
        if (effectiveRequestId == lastHandledQuickSubtitleLaunchRequestId) return
        if (!isOverlayOpenTarget(target) && normalized.isEmpty()) return
        lastHandledQuickSubtitleLaunchRequestId = effectiveRequestId
        pendingQuickSubtitleLaunchRequest = ExternalQuickSubtitleRequest(
            requestId = effectiveRequestId,
            target = target,
            text = normalized,
            navigateToPage = navigateToPage
        )
    }

    private fun nextQuickSubtitleLaunchRequestId(): Long {
        val now = SystemClock.uptimeMillis()
        return if (now <= lastHandledQuickSubtitleLaunchRequestId) {
            lastHandledQuickSubtitleLaunchRequestId + 1
        } else {
            now
        }
    }

    fun consumeQuickSubtitleLaunchRequest(requestId: Long) {
        if (pendingQuickSubtitleLaunchRequest?.requestId == requestId) {
            pendingQuickSubtitleLaunchRequest = null
        }
        realtimeHost?.consumeQuickSubtitleRequest(requestId)
    }

    private fun requestVoicePackInstallNavigation(message: String) {
        pendingVoicePackInstallRequest = ExternalVoicePackInstallRequest(
            requestId = SystemClock.uptimeMillis(),
            message = message
        )
    }

    fun consumeVoicePackInstallRequest(requestId: Long) {
        if (pendingVoicePackInstallRequest?.requestId == requestId) {
            pendingVoicePackInstallRequest = null
        }
    }

    private fun requestPresetInstallNavigation(target: PresetInstallTarget, message: String) {
        pendingPresetInstallRequest = ExternalPresetInstallRequest(
            requestId = SystemClock.uptimeMillis(),
            target = target,
            message = message
        )
    }

    fun consumePresetInstallRequest(requestId: Long) {
        if (pendingPresetInstallRequest?.requestId == requestId) {
            pendingPresetInstallRequest = null
        }
    }

    fun exportQuickSubtitlePresetPackage(groupIds: Set<Long>) {
        if (groupIds.isEmpty()) {
            uiState = uiState.copy(status = "请先选择要导出的便捷字幕分组")
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    writeQuickSubtitlePresetPackage(
                        context = appContext,
                        groups = quickSubtitleGroups.filter { it.id in groupIds }
                    )
                }
            }
            result.onSuccess { file ->
                uiState = uiState.copy(status = "便捷字幕预设已导出：${file.absolutePath}")
                sharePresetFile(file, "application/x-kigtts-quicktext-preset", "分享便捷字幕预设")
            }.onFailure { e ->
                uiState = uiState.copy(status = "便捷字幕预设导出失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun importQuickSubtitlePresetPackage(uri: android.net.Uri, openEditorOnSuccess: Boolean = false) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { readQuickSubtitlePresetPackage(appContext, uri) }
            }
            result.onSuccess { imported ->
                if (imported.isEmpty()) {
                    uiState = uiState.copy(status = "便捷字幕预设包没有可导入分组")
                    return@onSuccess
                }
                val existingTitles = quickSubtitleGroups.map { it.title }.toMutableSet()
                val remapped = imported.map { group ->
                    val title = uniqueQuickSubtitleGroupTitle(group.title, existingTitles)
                    existingTitles += title
                    group.copy(id = quickSubtitleNextGroupId++, title = title)
                }
                quickSubtitleGroups = quickSubtitleGroups + remapped
                quickSubtitleSelectedGroupId = remapped.first().id
                saveQuickSubtitleConfig()
                val message = "便捷字幕预设已安装：${remapped.size} 个分组"
                uiState = uiState.copy(status = message)
                if (openEditorOnSuccess) {
                    requestPresetInstallNavigation(PresetInstallTarget.QuickSubtitle, message)
                }
            }.onFailure { e ->
                uiState = uiState.copy(status = "便捷字幕预设导入失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun requestRecordAudioPermission(startRealtimeOnGrant: Boolean = false) {
        pendingRecordAudioPermissionRequest = ExternalRecordAudioPermissionRequest(
            requestId = SystemClock.uptimeMillis(),
            startRealtimeOnGrant = startRealtimeOnGrant
        )
    }

    fun consumeRecordAudioPermissionRequest(requestId: Long) {
        if (pendingRecordAudioPermissionRequest?.requestId == requestId) {
            pendingRecordAudioPermissionRequest = null
        }
    }

    fun requestAccessibilityExplainDialog() {
        pendingAccessibilityExplainRequest =
            ExternalAccessibilityExplainRequest(requestId = SystemClock.uptimeMillis())
    }

    fun consumeAccessibilityExplainRequest(requestId: Long) {
        if (pendingAccessibilityExplainRequest?.requestId == requestId) {
            pendingAccessibilityExplainRequest = null
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
                quickSubtitleInputCollapsed = false
                quickSubtitleInputText = normalized
                saveQuickSubtitleConfig()
            }
            else -> {
                if (normalized.isEmpty()) return
                quickSubtitleCurrentText = normalized
                BluetoothMediaTitleBridge.updateSubtitle(appContext, normalized)
                markQuickSubtitleContentSubmitted()
                saveQuickSubtitleConfig()
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
        realtimeHost?.setQuickSubtitlePlayOnSend(enabled)
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

    fun updateQuickSubtitleRotated180(enabled: Boolean) {
        quickSubtitleRotated180 = enabled
        saveQuickSubtitleConfig()
    }

    fun updateQuickSubtitleShowActionButtons(showActionButtons: Boolean) {
        quickSubtitleShowActionButtons = showActionButtons
        saveQuickSubtitleConfig()
    }

    fun updateSettingsSelectedCategory(category: SettingsCategory) {
        settingsSelectedCategoryName = category.name
    }

    fun clearQuickSubtitleText() {
        quickSubtitleCurrentText = QUICK_SUBTITLE_CLEARED_HINT
        BluetoothMediaTitleBridge.updateSubtitle(appContext, QUICK_SUBTITLE_CLEARED_HINT)
        saveQuickSubtitleConfig()
    }

    fun updateQuickSubtitleGroupMeta(index: Int, title: String, icon: String) {
        if (index !in quickSubtitleGroups.indices) return
        val next = quickSubtitleGroups.toMutableList()
        val prev = next[index]
        next[index] = prev.copy(
            title = title.trim(),
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

    private fun observeSoundboardPlayback() {
        viewModelScope.launch {
            SoundboardManager.playbackState().collectLatest { next ->
                soundboardPlaybackStates = next
            }
        }
    }

    private fun loadSoundboardConfig() {
        viewModelScope.launch {
            val parsed = parseSoundboardConfig(UserPrefs.getSoundboardConfig(appContext))
            applySoundboardConfig(parsed)
            SoundboardManager.updateCachedConfig(parsed)
        }
    }

    private fun applySoundboardConfig(config: com.lhtstudio.kigtts.app.data.SoundboardConfig) {
        val groups = config.groups.ifEmpty { defaultSoundboardGroups() }
        soundboardGroups = groups
        soundboardSelectedGroupId =
            groups.firstOrNull { it.id == config.selectedGroupId }?.id ?: groups.first().id
        soundboardPortraitLayout = config.portraitLayout
        soundboardLandscapeLayout = config.landscapeLayout
        soundboardNextGroupId = maxOf(2L, (groups.maxOfOrNull { it.id } ?: 0L) + 1L)
        soundboardNextItemId = maxOf(
            1L,
            groups.asSequence().flatMap { it.items.asSequence() }.maxOfOrNull { it.id }?.plus(1L) ?: 1L
        )
    }

    private fun saveSoundboardConfig() {
        val payload = serializeSoundboardConfig(
            SoundboardConfig(
                groups = soundboardGroups,
                selectedGroupId = soundboardSelectedGroupId,
                portraitLayout = soundboardPortraitLayout,
                landscapeLayout = soundboardLandscapeLayout
            )
        )
        val cachedConfig = parseSoundboardConfig(payload)
        SoundboardManager.updateCachedConfig(cachedConfig)
        if (soundboardSaving) {
            pendingSoundboardSavePayload = payload
            return
        }
        soundboardSaving = true
        viewModelScope.launch {
            try {
                var nextPayload = payload
                while (true) {
                    UserPrefs.setSoundboardConfig(appContext, nextPayload)
                    val pending = pendingSoundboardSavePayload
                    if (pending == null || pending == nextPayload) {
                        pendingSoundboardSavePayload = null
                        break
                    }
                    pendingSoundboardSavePayload = null
                    nextPayload = pending
                }
            } finally {
                soundboardSaving = false
            }
        }
    }

    private fun currentSoundboardConfig(): SoundboardConfig {
        return SoundboardConfig(
            groups = soundboardGroups,
            selectedGroupId = soundboardSelectedGroupId,
            portraitLayout = soundboardPortraitLayout,
            landscapeLayout = soundboardLandscapeLayout
        )
    }

    fun exportSoundboardPresetPackage(groupIds: Set<Long>) {
        if (groupIds.isEmpty()) {
            uiState = uiState.copy(status = "请先选择要导出的音效板分组")
            return
        }
        val config = currentSoundboardConfig()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    SoundboardPresetIo.exportPackage(appContext, config, groupIds)
                }
            }
            result.onSuccess { file ->
                uiState = uiState.copy(status = "音效板预设已导出：${file.absolutePath}")
                sharePresetFile(file, "application/x-kigtts-soundboard-preset", "分享音效板预设")
            }.onFailure { e ->
                uiState = uiState.copy(status = "音效板预设导出失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun importSoundboardPresetPackage(uri: android.net.Uri, openEditorOnSuccess: Boolean = false) {
        val current = currentSoundboardConfig()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { SoundboardPresetIo.importPackage(appContext, uri, current) }
            }
            result.onSuccess { config ->
                val importedCount = (config.groups.size - soundboardGroups.size).coerceAtLeast(0)
                applySoundboardConfig(config)
                SoundboardManager.updateCachedConfig(config)
                saveSoundboardConfig()
                val message = if (importedCount > 0) {
                    "音效板预设已安装：$importedCount 个分组"
                } else {
                    "音效板预设已安装"
                }
                uiState = uiState.copy(status = message)
                if (openEditorOnSuccess) {
                    requestPresetInstallNavigation(PresetInstallTarget.Soundboard, message)
                }
            }.onFailure { e ->
                uiState = uiState.copy(status = "音效板预设导入失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun currentSoundboardGroupIndex(): Int {
        val idx = soundboardGroups.indexOfFirst { it.id == soundboardSelectedGroupId }
        return if (idx >= 0) idx else 0
    }

    fun selectSoundboardGroup(index: Int) {
        val clamped = index.coerceIn(0, soundboardGroups.lastIndex.coerceAtLeast(0))
        val target = soundboardGroups.getOrNull(clamped) ?: return
        soundboardSelectedGroupId = target.id
        saveSoundboardConfig()
    }

    fun currentSoundboardLayout(landscape: Boolean): SoundboardLayoutMode {
        return if (landscape) soundboardLandscapeLayout else soundboardPortraitLayout
    }

    fun updateSoundboardLayout(landscape: Boolean, layout: SoundboardLayoutMode) {
        if (landscape) soundboardLandscapeLayout = layout else soundboardPortraitLayout = layout
        saveSoundboardConfig()
    }

    fun updateSoundboardGroupMeta(index: Int, title: String, icon: String) {
        if (index !in soundboardGroups.indices) return
        val next = soundboardGroups.toMutableList()
        val prev = next[index]
        next[index] = prev.copy(
            title = title.trim(),
            icon = icon.ifBlank { "music_note" }
        )
        soundboardGroups = next
        saveSoundboardConfig()
    }

    fun setSoundboardGroupKeywordWakeEnabled(index: Int, enabled: Boolean) {
        if (index !in soundboardGroups.indices) return
        val next = soundboardGroups.toMutableList()
        next[index] = next[index].copy(keywordWakeEnabled = enabled)
        soundboardGroups = next
        saveSoundboardConfig()
    }

    fun addSoundboardGroup() {
        val newId = soundboardNextGroupId++
        soundboardGroups = soundboardGroups + SoundboardGroup(
            id = newId,
            title = "新分组",
            icon = "music_note",
            keywordWakeEnabled = true,
            items = emptyList()
        )
        soundboardSelectedGroupId = newId
        saveSoundboardConfig()
    }

    fun removeSoundboardGroup(index: Int) {
        if (soundboardGroups.size <= 1) return
        if (index !in soundboardGroups.indices) return
        val removedId = soundboardGroups[index].id
        val next = soundboardGroups.toMutableList().apply { removeAt(index) }
        soundboardGroups = next
        if (soundboardSelectedGroupId == removedId) {
            soundboardSelectedGroupId = next[index.coerceAtMost(next.lastIndex)].id
        }
        saveSoundboardConfig()
    }

    fun moveSoundboardGroup(from: Int, to: Int) {
        if (from !in soundboardGroups.indices || to !in soundboardGroups.indices || from == to) return
        val next = soundboardGroups.toMutableList()
        val moved = next.removeAt(from)
        next.add(to, moved)
        soundboardGroups = next
        saveSoundboardConfig()
    }

    fun addSoundboardItem(groupIndex: Int) {
        if (groupIndex !in soundboardGroups.indices) return
        val next = soundboardGroups.toMutableList()
        val group = next[groupIndex]
        next[groupIndex] = group.copy(
            items = group.items + SoundboardItem(
                id = soundboardNextItemId++,
                title = "新音效"
            )
        )
        soundboardGroups = next
        saveSoundboardConfig()
    }

    fun updateSoundboardItem(
        groupIndex: Int,
        itemIndex: Int,
        transform: (SoundboardItem) -> SoundboardItem
    ) {
        if (groupIndex !in soundboardGroups.indices) return
        val group = soundboardGroups[groupIndex]
        if (itemIndex !in group.items.indices) return
        val items = group.items.toMutableList()
        items[itemIndex] = transform(items[itemIndex])
        val next = soundboardGroups.toMutableList()
        next[groupIndex] = group.copy(items = items)
        soundboardGroups = next
        saveSoundboardConfig()
    }

    fun setSoundboardItems(groupIndex: Int, items: List<SoundboardItem>) {
        if (groupIndex !in soundboardGroups.indices) return
        val next = soundboardGroups.toMutableList()
        next[groupIndex] = next[groupIndex].copy(items = items)
        soundboardGroups = next
        saveSoundboardConfig()
    }

    fun removeSoundboardItem(groupIndex: Int, itemIndex: Int) {
        if (groupIndex !in soundboardGroups.indices) return
        val group = soundboardGroups[groupIndex]
        if (itemIndex !in group.items.indices) return
        val items = group.items.toMutableList().apply { removeAt(itemIndex) }
        val next = soundboardGroups.toMutableList()
        next[groupIndex] = group.copy(items = items)
        soundboardGroups = next
        saveSoundboardConfig()
    }

    fun isSoundboardItemPlaying(itemId: Long): Boolean {
        return soundboardPlaybackStates[itemId]?.playing == true
    }

    fun soundboardItemProgress(itemId: Long): Float {
        return soundboardPlaybackStates[itemId]?.progress?.coerceIn(0f, 1f) ?: 0f
    }

    fun playSoundboardItem(item: SoundboardItem) {
        if (item.audioPath.isBlank()) {
            uiState = uiState.copy(status = "请先为该音效选择音频文件")
            return
        }
        viewModelScope.launch {
            val played = SoundboardManager.play(item)
            if (!played) {
                uiState = uiState.copy(status = "音效播放失败")
            }
        }
    }

    fun stopSoundboardItem(itemId: Long) {
        viewModelScope.launch {
            SoundboardManager.stop(itemId)
        }
    }

    fun importSoundboardAudioClip(
        groupIndex: Int,
        itemIndex: Int,
        uri: android.net.Uri,
        startMs: Long,
        endMs: Long
    ) {
        val item = soundboardGroups.getOrNull(groupIndex)?.items?.getOrNull(itemIndex) ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    SoundboardPresetIo.importAudioClip(
                        context = appContext,
                        uri = uri,
                        startMs = startMs,
                        endMs = endMs,
                        titleHint = item.title
                    )
                }
            }
            result.onSuccess { imported ->
                updateSoundboardItem(groupIndex, itemIndex) { current ->
                    current.copy(
                        audioPath = imported.path,
                        durationMs = imported.durationMs,
                        trimStartMs = imported.trimStartMs,
                        trimEndMs = imported.trimEndMs
                    )
                }
                uiState = uiState.copy(status = "音效已导入")
            }.onFailure { e ->
                uiState = uiState.copy(status = "音效导入失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun importSoundboardAudioFiles(groupIndex: Int, uris: List<android.net.Uri>) {
        if (groupIndex !in soundboardGroups.indices || uris.isEmpty()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    uris.map { uri ->
                        val displayName = SoundboardPresetIo.displayName(appContext, uri)
                        val title = displayName.substringBeforeLast('.').ifBlank { "新音效" }
                        val imported = SoundboardPresetIo.importAudioClip(
                            context = appContext,
                            uri = uri,
                            startMs = 0L,
                            endMs = Long.MAX_VALUE,
                            titleHint = title
                        )
                        title to imported
                    }
                }
            }
            result.onSuccess { importedItems ->
                val next = soundboardGroups.toMutableList()
                val group = next.getOrNull(groupIndex) ?: return@onSuccess
                val newItems = importedItems.map { (title, imported) ->
                    SoundboardItem(
                        id = soundboardNextItemId++,
                        title = title.trim().ifBlank { "新音效" },
                        audioPath = imported.path,
                        durationMs = imported.durationMs,
                        trimStartMs = imported.trimStartMs,
                        trimEndMs = imported.trimEndMs
                    )
                }
                next[groupIndex] = group.copy(items = group.items + newItems)
                soundboardGroups = next
                saveSoundboardConfig()
                uiState = uiState.copy(status = "已批量导入 ${newItems.size} 个音效")
            }.onFailure { e ->
                uiState = uiState.copy(status = "批量导入音效失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private fun maybeTriggerSoundboardFromText(text: String, fromQuickText: Boolean) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        if (!uiState.soundboardKeywordTriggerEnabled) return
        if (fromQuickText && !uiState.allowQuickTextTriggerSoundboard) return
        viewModelScope.launch {
            SoundboardManager.triggerByText(appContext, normalized)
        }
    }

    private suspend fun shouldSuppressVoiceTtsForSoundboard(text: String): Boolean {
        val normalized = text.trim()
        if (normalized.isEmpty()) return false
        return uiState.soundboardKeywordTriggerEnabled &&
            uiState.soundboardSuppressTtsOnKeyword &&
            SoundboardManager.hasTriggerMatch(appContext, normalized)
    }

    private fun quickCardDir(): File {
        val dir = File(appContext.filesDir, "quick_cards")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun defaultQuickCardDraft(editId: Long, isNew: Boolean, prefillLink: String = ""): QuickCardDraft {
        return QuickCardDraft(
            editId = editId,
            isNew = isNew,
            type = QuickCardType.Text,
            title = "名片标题",
            note = "名片备注",
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
            val title = obj.optString("title", "")
            val note = obj.optString("note", "")
            val themeColor = obj.optString("themeColor", "#038387")
            val link = obj.optString("link", "")
            val portraitImagePath = obj.optString("portraitImagePath", "")
            val landscapeImagePath = obj.optString("landscapeImagePath", "")
            parsedCards += QuickCard(
                id = id,
                type = QuickCardType.Text,
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
                        put("type", QuickCardType.Text.wireValue)
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

    fun beginCreateQuickCard(@Suppress("UNUSED_PARAMETER") type: QuickCardType, prefillLink: String = "") {
        val id = quickCardsNextId++
        quickCardDraft = defaultQuickCardDraft(editId = id, isNew = true, prefillLink = prefillLink)
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

    private fun normalizedQuickCardFromDraft(draft: QuickCardDraft): QuickCard {
        val id = draft.editId ?: -1L
        val normalized = draft.copy(
            title = draft.title.trim(),
            note = draft.note.trim(),
            themeColor = normalizeQuickCardColor(draft.themeColor),
            link = draft.link.trim()
        )
        return QuickCard(
            id = id,
            type = QuickCardType.Text,
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
            decodeQrWithZxing(source)
        }.getOrNull()
    }

    suspend fun decodeQrContentFromBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val mlKit = createQrMlKitScanner()
        try {
            val mlResult = awaitTask(mlKit.process(InputImage.fromBitmap(bitmap, 0)))
                ?.firstDecodedQrText()
            if (!mlResult.isNullOrEmpty()) {
                return@withContext mlResult
            }
        } finally {
            runCatching { mlKit.close() }
        }
        decodeQrContentFromBitmapInternal(bitmap)
    }

    suspend fun decodeQrContentFromImage(uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        val mlKit = createQrMlKitScanner()
        try {
            val inputImage = runCatching { InputImage.fromFilePath(appContext, uri) }.getOrNull()
            if (inputImage != null) {
                val mlResult = awaitTask(mlKit.process(inputImage))?.firstDecodedQrText()
                if (!mlResult.isNullOrEmpty()) {
                    return@withContext mlResult
                }
            }
        } finally {
            runCatching { mlKit.close() }
        }
        val bmp = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return@withContext null
        decodeQrContentFromBitmapInternal(bmp)
    }

    private fun preloadAsr(asrDir: File?) {
        if (asrDir == null) return
        val host = realtimeHost
        viewModelScope.launch {
            if (host != null) {
                host.updateSelectedAsrDir(asrDir, preload = true)
            } else {
                pendingHostAsrDir = asrDir
                requestRealtimeHost()
            }
        }
    }

    private fun preloadTts(voiceDir: File?) {
        if (voiceDir == null) return
        val host = realtimeHost
        viewModelScope.launch {
            val loaded = if (host != null) {
                host.updateSelectedVoiceDir(voiceDir, preload = true)
                true
            } else {
                pendingHostVoiceDir = voiceDir
                requestRealtimeHost()
                true
            }
            if (!loaded && uiState.voiceDir?.absolutePath == voiceDir.absolutePath) {
                uiState = uiState.copy(
                    status = if (isSystemTtsVoiceDir(voiceDir)) {
                        "系统 TTS 初始化失败，请先完成系统 TTS 设置"
                    } else {
                        "音色包加载失败"
                    }
                )
            }
        }
    }

    fun openSystemTtsSetup(context: Context) {
        val intents = listOf(
            android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_CHECK_TTS_DATA),
            android.content.Intent("com.android.settings.TTS_SETTINGS")
        )
        for (intent in intents) {
            val resolved = runCatching {
                context.packageManager.resolveActivity(intent, 0)
            }.getOrNull()
            if (resolved != null) {
                runCatching {
                    context.startActivity(intent.apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }.onSuccess { return }.onFailure {
                    AppLogger.e("openSystemTtsSetup failed action=${intent.action}", it)
                }
            }
        }
        toast(context, "无法打开系统 TTS 设置")
    }

    private fun systemTtsVoiceDir(): File = repo.systemTtsVirtualDir()

    private fun isSystemTtsVoicePack(pack: VoicePackInfo): Boolean = isSystemTtsVoiceDir(pack.dir)

    private fun isKokoroVoicePack(pack: VoicePackInfo): Boolean = isKokoroVoiceDir(pack.dir)

    private suspend fun resolvePreferredVoiceDir(lastName: String?): File? {
        val resolved = when (lastName) {
            SYSTEM_TTS_VOICE_NAME -> systemTtsVoiceDir()
            KOKORO_VOICE_NAME -> repo.kokoroVoiceDir().takeIf { repo.kokoroVoiceStatus().installed }
            null -> null
            else -> repo.resolveVoicePack(lastName)
        }
        if (resolved != null) return resolved
        return withContext(Dispatchers.IO) { repo.listVoicePacks().firstOrNull()?.dir }
            ?: repo.kokoroVoiceDir().takeIf { repo.kokoroVoiceStatus().installed }
            ?: systemTtsVoiceDir()
    }

    private suspend fun loadSystemTtsVoicePackInfo(existing: List<VoicePackInfo>): VoicePackInfo {
        val defaultOrder = (existing.maxOfOrNull { it.meta.order } ?: -1L) + 1L
        val order = UserPrefs.getSystemTtsOrder(appContext) ?: defaultOrder.also {
            UserPrefs.setSystemTtsOrder(appContext, it)
        }
        return VoicePackInfo(
            dir = systemTtsVoiceDir(),
            meta = VoicePackMeta(
                name = SYSTEM_TTS_DEFAULT_LABEL,
                remark = SYSTEM_TTS_DEFAULT_REMARK,
                avatar = "avatar.png",
                pinned = UserPrefs.getSystemTtsPinned(appContext),
                order = order
            )
        )
    }

    private suspend fun loadVoicePackList(): List<VoicePackInfo> {
        val physical = withContext(Dispatchers.IO) { repo.listVoicePacks() }
        val kokoroStatus = withContext(Dispatchers.IO) { repo.kokoroVoiceStatus() }
        val kokoro = if (kokoroStatus.installed) {
            val defaultOrder = (physical.maxOfOrNull { it.meta.order } ?: -1L) + 1L
            val order = UserPrefs.getKokoroVoiceOrder(appContext) ?: defaultOrder.also {
                UserPrefs.setKokoroVoiceOrder(appContext, it)
            }
            listOf(
                VoicePackInfo(
                    dir = repo.kokoroVoiceDir(),
                    meta = VoicePackMeta(
                        name = "Kokoro",
                        remark = "离线朗读声音，可切换多个声音编号",
                        avatar = "avatar.png",
                        pinned = UserPrefs.getKokoroVoicePinned(appContext),
                        order = order
                    )
                )
            )
        } else {
            emptyList()
        }
        val system = loadSystemTtsVoicePackInfo(physical)
        return sortVoicePacks(kokoro + physical + system)
    }

    private suspend fun findFallbackVoicePack(excludingDir: File): File? {
        val excludedPath = excludingDir.absolutePath
        val listed = withContext(Dispatchers.IO) { repo.listVoicePacks() }
            .firstOrNull { it.dir.absolutePath != excludedPath }
            ?.dir
        if (listed != null) return listed
        val kokoro = repo.kokoroVoiceDir()
        if (kokoro.absolutePath != excludedPath && repo.kokoroVoiceStatus().installed) {
            return kokoro
        }
        return systemTtsVoiceDir().takeIf { it.absolutePath != excludedPath }
    }

    private fun fallbackVoiceStatus(dir: File): String {
        return when {
            isSystemTtsVoiceDir(dir) -> "已切换到系统 TTS"
            isKokoroVoiceDir(dir) -> "已切换备用语音包：Kokoro"
            else -> "已切换备用语音包：${dir.name}"
        }
    }

    private suspend fun stopRealtimeImmediatelyForVoicePackDeletion() {
        realtimeHost?.let { host ->
            host.stopForVoicePackDeletion()
            return
        }
        RealtimeOwnerGate.release(APP_REALTIME_OWNER_TAG)
        KeepAliveService.stop(appContext)
        realtimeInputLevel = 0f
        realtimePlaybackProgress = 0f
        uiState = uiState.copy(
            running = false,
            status = "当前语音包已删除，麦克风已停止",
            pushToTalkPressed = false,
            pushToTalkStreamingText = ""
        )
    }

    fun loadBundledAsr() {
        if (uiState.asrDir != null) return
        viewModelScope.launch {
            val (dir, loadStatus) = withContext(Dispatchers.IO) {
                val resolvedDir = repo.ensureBundledAsr()
                val resourceStatus = repo.recognitionResourceStatus()
                val statusText = if (
                    resolvedDir != null &&
                    resourceStatus.asrDir?.absolutePath == resolvedDir.absolutePath
                ) {
                    "已加载语音识别资源包"
                } else {
                    "已加载语音识别资源包"
                }
                resolvedDir to statusText
            }
            if (dir != null) {
                val host = realtimeHost
                if (host != null) {
                    host.updateSelectedAsrDir(dir, status = loadStatus, preload = true)
                } else {
                    uiState = uiState.copy(asrDir = dir, status = loadStatus)
                    preloadAsr(dir)
                }
            } else {
                uiState = uiState.copy(status = "请先安装语音识别资源包")
            }
        }
    }

    fun refreshRecognitionResourceStatus() {
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) { repo.recognitionResourceStatus() }
            val installedAsrDir = status.asrDir
            val shouldApplyAsrDir =
                installedAsrDir != null &&
                    uiState.asrDir?.absolutePath != installedAsrDir.absolutePath
            uiState = uiState.copy(
                recognitionResourceInstalled = status.installed,
                recognitionResourceName = status.name,
                recognitionResourceVersion = status.version,
                recognitionResourceStatus = recognitionResourceStatusText(status),
                recognitionResourceBusy = false,
                recognitionResourceProgressStage = "",
                recognitionResourceProgress = -1f,
                asrDir = installedAsrDir ?: uiState.asrDir
            )
            if (shouldApplyAsrDir && installedAsrDir != null) {
                val host = realtimeHost
                if (host != null) {
                    host.updateSelectedAsrDir(installedAsrDir, status = "已加载语音识别资源包", preload = true)
                } else {
                    preloadAsr(installedAsrDir)
                }
            }
        }
    }

    fun setRecognitionResourceSources(
        modelScopeUrl: String,
        huggingFaceUrl: String,
        preferredSource: Int
    ) {
        viewModelScope.launch {
            UserPrefs.setRecognitionResourceSources(
                appContext,
                modelScopeUrl = modelScopeUrl,
                huggingFaceUrl = huggingFaceUrl,
                preferredSource = preferredSource
            )
            uiState = uiState.copy(
                recognitionResourceModelScopeUrl = modelScopeUrl.trim(),
                recognitionResourceHuggingFaceUrl = huggingFaceUrl.trim(),
                recognitionResourcePreferredSource = preferredSource.coerceIn(
                    UserPrefs.RECOGNITION_RESOURCE_SOURCE_MODELSCOPE,
                    UserPrefs.RECOGNITION_RESOURCE_SOURCE_HUGGINGFACE
                )
            )
        }
    }

    fun downloadRecognitionResources() {
        if (uiState.recognitionResourceBusy) return
        val url = preferredRecognitionResourceUrl()
        if (url.isBlank()) {
            uiState = uiState.copy(status = "请先配置语音识别资源包下载源")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(
                recognitionResourceBusy = true,
                recognitionResourceProgressStage = "准备下载",
                recognitionResourceProgress = -1f,
                recognitionResourceStatus = "准备下载语音识别资源包"
            )
            try {
                val status = withContext(Dispatchers.IO) {
                    repo.downloadRecognitionResources(url) { progress ->
                        postRecognitionResourceProgress(progress)
                    }
                }
                applyInstalledRecognitionResource(status, "语音识别资源包安装完成")
            } catch (e: Exception) {
                AppLogger.e("downloadRecognitionResources failed", e)
                uiState = uiState.copy(
                    recognitionResourceBusy = false,
                    recognitionResourceProgressStage = "",
                    recognitionResourceProgress = -1f,
                    recognitionResourceStatus = "语音识别资源包安装失败：${e.message ?: "未知错误"}",
                    status = "语音识别资源包安装失败"
                )
            }
        }
    }

    fun installRecognitionResources(uri: android.net.Uri) {
        if (uiState.recognitionResourceBusy) return
        viewModelScope.launch {
            uiState = uiState.copy(
                recognitionResourceBusy = true,
                recognitionResourceProgressStage = "准备安装",
                recognitionResourceProgress = -1f,
                recognitionResourceStatus = "准备安装语音识别资源包"
            )
            try {
                val status = withContext(Dispatchers.IO) {
                    repo.installRecognitionResources(uri, appContext.contentResolver) { progress ->
                        postRecognitionResourceProgress(progress)
                    }
                }
                applyInstalledRecognitionResource(status, "语音识别资源包安装完成")
            } catch (e: Exception) {
                AppLogger.e("installRecognitionResources failed", e)
                uiState = uiState.copy(
                    recognitionResourceBusy = false,
                    recognitionResourceProgressStage = "",
                    recognitionResourceProgress = -1f,
                    recognitionResourceStatus = "语音识别资源包安装失败：${e.message ?: "未知错误"}",
                    status = "语音识别资源包安装失败"
                )
            }
        }
    }

    fun refreshKokoroVoiceStatus() {
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) { repo.kokoroVoiceStatus() }
            uiState = uiState.copy(
                kokoroInstalled = status.installed,
                kokoroStatus = kokoroStatusText(status),
                kokoroBusy = false,
                kokoroProgressStage = "",
                kokoroProgress = -1f
            )
            refreshVoicePacks()
        }
    }

    fun setKokoroSources(hfUrl: String, hfMirrorUrl: String, modelScopeUrl: String, preferredSource: Int) {
        viewModelScope.launch {
            UserPrefs.setKokoroSources(appContext, hfUrl, hfMirrorUrl, modelScopeUrl, preferredSource)
            uiState = uiState.copy(
                kokoroHfUrl = hfUrl.trim(),
                kokoroHfMirrorUrl = hfMirrorUrl.trim(),
                kokoroModelScopeUrl = modelScopeUrl.trim(),
                kokoroPreferredSource = preferredSource.coerceIn(
                    UserPrefs.KOKORO_SOURCE_HF,
                    UserPrefs.KOKORO_SOURCE_MODELSCOPE
                )
            )
        }
    }

    fun setKokoroSpeakerId(speakerId: Int) {
        val normalized = speakerId.coerceIn(UserPrefs.KOKORO_MIN_SPEAKER_ID, UserPrefs.KOKORO_MAX_SPEAKER_ID)
        uiState = uiState.copy(kokoroSpeakerId = normalized)
        realtimeHost?.setKokoroSpeakerId(normalized)
        viewModelScope.launch {
            UserPrefs.setKokoroSpeakerId(appContext, normalized)
        }
    }

    fun downloadKokoroVoice() {
        if (uiState.kokoroBusy) return
        val url = preferredKokoroSourceUrl()
        if (url.isBlank()) {
            uiState = uiState.copy(status = "请先配置 Kokoro 下载源")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(
                kokoroBusy = true,
                kokoroProgressStage = "准备下载",
                kokoroProgress = -1f,
                kokoroStatus = "准备下载 Kokoro 离线语音"
            )
            try {
                val status = withContext(Dispatchers.IO) {
                    repo.downloadKokoroVoice(url) { progress -> postKokoroProgress(progress) }
                }
                applyInstalledKokoroVoice(status, "Kokoro 离线语音安装完成")
            } catch (e: Exception) {
                AppLogger.e("downloadKokoroVoice failed", e)
                uiState = uiState.copy(
                    kokoroBusy = false,
                    kokoroProgressStage = "",
                    kokoroProgress = -1f,
                    kokoroStatus = "Kokoro 离线语音安装失败：${e.message ?: "未知错误"}",
                    status = "Kokoro 离线语音安装失败"
                )
            }
        }
    }

    fun installKokoroVoice(uri: android.net.Uri) {
        if (uiState.kokoroBusy) return
        viewModelScope.launch {
            uiState = uiState.copy(
                kokoroBusy = true,
                kokoroProgressStage = "准备安装",
                kokoroProgress = -1f,
                kokoroStatus = "准备安装 Kokoro 离线语音"
            )
            try {
                val status = withContext(Dispatchers.IO) {
                    repo.installKokoroVoice(uri, appContext.contentResolver) { progress -> postKokoroProgress(progress) }
                }
                applyInstalledKokoroVoice(status, "Kokoro 离线语音安装完成")
            } catch (e: Exception) {
                AppLogger.e("installKokoroVoice failed", e)
                uiState = uiState.copy(
                    kokoroBusy = false,
                    kokoroProgressStage = "",
                    kokoroProgress = -1f,
                    kokoroStatus = "Kokoro 离线语音安装失败：${e.message ?: "未知错误"}",
                    status = "Kokoro 离线语音安装失败"
                )
            }
        }
    }

    private fun preferredKokoroSourceUrl(): String {
        val hf = uiState.kokoroHfUrl.trim()
        val mirror = uiState.kokoroHfMirrorUrl.trim()
        val modelScope = uiState.kokoroModelScopeUrl.trim()
        return when (uiState.kokoroPreferredSource) {
            UserPrefs.KOKORO_SOURCE_HF -> hf.ifBlank { mirror.ifBlank { modelScope } }
            UserPrefs.KOKORO_SOURCE_MODELSCOPE -> modelScope.ifBlank { mirror.ifBlank { hf } }
            else -> mirror.ifBlank { hf.ifBlank { modelScope } }
        }
    }

    private fun postKokoroProgress(progress: RecognitionResourceProgress) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            uiState = uiState.copy(
                kokoroBusy = true,
                kokoroProgressStage = progress.stage,
                kokoroProgress = progress.fraction,
                kokoroStatus = if (progress.fraction in 0f..1f) {
                    "${progress.stage} ${(progress.fraction * 100f).roundToInt()}%"
                } else {
                    progress.stage
                }
            )
        }
    }

    private suspend fun applyInstalledKokoroVoice(status: KokoroVoiceStatus, message: String) {
        uiState = uiState.copy(
            kokoroInstalled = status.installed,
            kokoroStatus = kokoroStatusText(status),
            kokoroBusy = false,
            kokoroProgressStage = "",
            kokoroProgress = -1f,
            status = message
        )
        refreshVoicePacks()
    }

    private fun kokoroStatusText(status: KokoroVoiceStatus): String {
        if (!status.installed) return "未安装 Kokoro 离线语音。安装后可以在语音包页面选择 Kokoro。"
        return buildString {
            append("已安装 ")
            append(status.name)
            if (status.version.isNotBlank()) {
                append(" / ")
                append(status.version)
            }
            append("，可切换多个声音编号。")
        }
    }

    private fun preferredRecognitionResourceUrl(): String {
        val modelScope = uiState.recognitionResourceModelScopeUrl.trim()
        val huggingFace = uiState.recognitionResourceHuggingFaceUrl.trim()
        return if (uiState.recognitionResourcePreferredSource == UserPrefs.RECOGNITION_RESOURCE_SOURCE_HUGGINGFACE) {
            huggingFace.ifBlank { modelScope }
        } else {
            modelScope.ifBlank { huggingFace }
        }
    }

    private fun postRecognitionResourceProgress(progress: RecognitionResourceProgress) {
        appContext.runOnUiThread {
            uiState = uiState.copy(
                recognitionResourceBusy = true,
                recognitionResourceProgressStage = progress.stage,
                recognitionResourceProgress = progress.fraction,
                recognitionResourceStatus = if (progress.fraction in 0f..1f) {
                    "${progress.stage} ${(progress.fraction * 100f).roundToInt()}%"
                } else {
                    progress.stage
                }
            )
        }
    }

    private fun recognitionResourceStatusText(status: RecognitionResourceStatus): String {
        if (!status.installed) return "未安装语音识别资源包，请先从下载源或本地文件安装。"
        val version = status.version.takeIf { it.isNotBlank() }?.let { "，版本 $it" }.orEmpty()
        val asr = if (status.asrDir != null) "，ASR 可用" else "，ASR 未找到"
        return "${status.name}$version 已安装$asr"
    }

    private suspend fun applyInstalledRecognitionResource(
        status: RecognitionResourceStatus,
        message: String
    ) {
        val asrDir = status.asrDir
        if (asrDir != null) {
            UserPrefs.clearLastAsrName(appContext)
            val host = realtimeHost
            if (host != null) {
                host.updateSelectedAsrDir(asrDir, status = message, preload = true)
            } else {
                uiState = uiState.copy(asrDir = asrDir)
                preloadAsr(asrDir)
            }
        }
        uiState = uiState.copy(
            recognitionResourceInstalled = status.installed,
            recognitionResourceName = status.name,
            recognitionResourceVersion = status.version,
            recognitionResourceStatus = recognitionResourceStatusText(status),
            recognitionResourceBusy = false,
            recognitionResourceProgressStage = "",
            recognitionResourceProgress = -1f,
            status = message
        )
    }

    fun loadLastVoice() {
        viewModelScope.launch {
            val lastName = UserPrefs.getLastVoiceName(appContext)
            val lastDir = resolvePreferredVoiceDir(lastName)
            if (lastDir != null) {
                val voiceStatus = when {
                    isSystemTtsVoiceDir(lastDir) -> "已加载系统 TTS"
                    isKokoroVoiceDir(lastDir) -> "已加载 Kokoro"
                    else -> "已加载音色包"
                }
                uiState = uiState.copy(
                    voiceDir = lastDir,
                    status = voiceStatus
                )
                UserPrefs.setLastVoiceName(
                    appContext,
                    when {
                        isSystemTtsVoiceDir(lastDir) -> SYSTEM_TTS_VOICE_NAME
                        isKokoroVoiceDir(lastDir) -> KOKORO_VOICE_NAME
                        else -> lastDir.name
                    }
                )
            }
            val selectedVoice = uiState.voiceDir
            val host = realtimeHost
            if (host != null && selectedVoice != null) {
                host.updateSelectedVoiceDir(selectedVoice, status = uiState.status, preload = true)
            } else {
                preloadTts(selectedVoice)
            }
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
            val host = realtimeHost
            if (host != null) {
                host.updateSelectedAsrDir(dir, status = "ASR 模型导入完成", preload = true)
            } else {
                uiState = uiState.copy(asrDir = dir, status = "ASR 模型导入完成")
                preloadAsr(dir)
            }
        }
    }

    fun importVoice(
        uri: android.net.Uri,
        openVoicePackPageOnSuccess: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val dir = withContext(Dispatchers.IO) { repo.importVoice(uri, appContext.contentResolver) }
                UserPrefs.setLastVoiceName(appContext, dir.name)
                val host = realtimeHost
                if (host != null) {
                    host.updateSelectedVoiceDir(dir, status = "音色包导入完成", preload = true)
                } else {
                    uiState = uiState.copy(voiceDir = dir, status = "音色包导入完成")
                    preloadTts(dir)
                }
                refreshVoicePacks()
                if (uiState.floatingOverlayEnabled) {
                    FloatingOverlayService.refresh(appContext)
                }
                if (openVoicePackPageOnSuccess) {
                    requestVoicePackInstallNavigation("语音包安装完成")
                }
            } catch (e: Exception) {
                uiState = uiState.copy(status = e.message ?: "音色包导入失败")
                AppLogger.e("importVoice failed", e)
            }
        }
    }

    fun selectVoice(dir: File) {
        viewModelScope.launch {
            UserPrefs.setLastVoiceName(
                appContext,
                when {
                    isSystemTtsVoiceDir(dir) -> SYSTEM_TTS_VOICE_NAME
                    isKokoroVoiceDir(dir) -> KOKORO_VOICE_NAME
                    else -> dir.name
                }
            )
            val status = when {
                isSystemTtsVoiceDir(dir) -> "已选择系统 TTS"
                isKokoroVoiceDir(dir) -> "已选择 Kokoro"
                else -> "已选择音色包"
            }
            val host = realtimeHost
            if (host != null) {
                host.updateSelectedVoiceDir(dir, status = status, preload = true)
            } else {
                uiState = uiState.copy(
                    voiceDir = dir,
                    status = status
                )
                preloadTts(dir)
            }
            refreshVoicePacks()
            if (uiState.floatingOverlayEnabled) {
                FloatingOverlayService.refresh(appContext)
            }
        }
    }

    fun refreshVoicePacks() {
        viewModelScope.launch {
            val packs = loadVoicePackList()
            uiState = uiState.copy(voicePacks = packs)
        }
    }

    fun updateVoiceMeta(pack: VoicePackInfo, name: String, remark: String) {
        if (isSystemTtsVoicePack(pack) || isKokoroVoicePack(pack)) return
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
        if (isSystemTtsVoicePack(pack) || isKokoroVoicePack(pack)) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.updateVoiceAvatar(pack.dir, appContext.contentResolver, uri, "avatar.png")
            }
            refreshVoicePacks()
        }
    }

    fun toggleVoicePin(pack: VoicePackInfo) {
        viewModelScope.launch {
            when {
                isSystemTtsVoicePack(pack) -> UserPrefs.setSystemTtsPinned(appContext, !pack.meta.pinned)
                isKokoroVoicePack(pack) -> UserPrefs.setKokoroVoicePinned(appContext, !pack.meta.pinned)
                else -> repo.updateVoiceMeta(pack.dir) { meta ->
                    meta.copy(pinned = !meta.pinned)
                }
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
            if (isSystemTtsVoiceDir(a.dir)) {
                UserPrefs.setSystemTtsOrder(appContext, b.meta.order)
            } else if (isKokoroVoiceDir(a.dir)) {
                UserPrefs.setKokoroVoiceOrder(appContext, b.meta.order)
            } else {
                repo.updateVoiceMeta(a.dir) { meta -> meta.copy(order = b.meta.order) }
            }
            if (isSystemTtsVoiceDir(b.dir)) {
                UserPrefs.setSystemTtsOrder(appContext, a.meta.order)
            } else if (isKokoroVoiceDir(b.dir)) {
                UserPrefs.setKokoroVoiceOrder(appContext, a.meta.order)
            } else {
                repo.updateVoiceMeta(b.dir) { meta -> meta.copy(order = a.meta.order) }
            }
            refreshVoicePacks()
        }
    }

    fun reorderVoicePacks(newOrder: List<VoicePackInfo>) {
        // Optimistically apply UI order to avoid one-frame fallback to stale state.
        uiState = uiState.copy(voicePacks = newOrder)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                newOrder.forEachIndexed { index, pack ->
                    if (isSystemTtsVoiceDir(pack.dir)) {
                        UserPrefs.setSystemTtsOrder(appContext, index.toLong())
                    } else if (isKokoroVoiceDir(pack.dir)) {
                        UserPrefs.setKokoroVoiceOrder(appContext, index.toLong())
                    } else {
                        repo.updateVoiceMeta(pack.dir) { meta ->
                            meta.copy(order = index.toLong())
                        }
                    }
                }
            }
            refreshVoicePacks()
        }
    }

    fun deleteVoice(pack: VoicePackInfo) {
        if (isSystemTtsVoicePack(pack)) {
            uiState = uiState.copy(status = "系统 TTS 不能删除")
            return
        }
        val current = uiState.voiceDir?.absolutePath == pack.dir.absolutePath
        viewModelScope.launch {
            val host = realtimeHost
            if (current) {
                val fallbackVoice = findFallbackVoicePack(pack.dir)
                if (fallbackVoice != null) {
                    val switched = if (host != null) {
                        host.updateSelectedVoiceDir(
                            fallbackVoice,
                            status = fallbackVoiceStatus(fallbackVoice),
                            preload = true
                        )
                        true
                    } else {
                        pendingHostVoiceDir = fallbackVoice
                        requestRealtimeHost()
                        true
                    }
                    if (!switched) {
                        uiState = uiState.copy(status = "切换备用语音包失败，已取消删除")
                        return@launch
                    }
                    UserPrefs.setLastVoiceName(
                        appContext,
                        when {
                            isSystemTtsVoiceDir(fallbackVoice) -> SYSTEM_TTS_VOICE_NAME
                            isKokoroVoiceDir(fallbackVoice) -> KOKORO_VOICE_NAME
                            else -> fallbackVoice.name
                        }
                    )
                    uiState = uiState.copy(
                        voiceDir = fallbackVoice,
                        status = fallbackVoiceStatus(fallbackVoice)
                    )
                } else {
                    if (uiState.running || host?.isMicActive() == true) {
                        stopRealtimeImmediatelyForVoicePackDeletion()
                    }
                    UserPrefs.clearLastVoiceName(appContext)
                    if (host != null) {
                        host.updateSelectedVoiceDir(null, preload = false)
                    }
                    uiState = uiState.copy(voiceDir = null)
                }
            }
            try {
                withContext(Dispatchers.IO) {
                    if (isKokoroVoicePack(pack)) {
                        repo.deleteKokoroVoice()
                    } else {
                        repo.deleteVoicePack(pack.dir)
                    }
                }
            } catch (e: SecurityException) {
                uiState = uiState.copy(status = e.message ?: "语音包删除失败")
                AppLogger.e("deleteVoice failed", e)
                return@launch
            }
            if (!current) {
                uiState = uiState.copy(status = "语音包已删除")
            } else if (uiState.voiceDir != null) {
                uiState = uiState.copy(status = "语音包已删除并切换到备用语音包")
            } else if (uiState.status.isBlank() || !uiState.status.contains("麦克风已停止")) {
                uiState = uiState.copy(status = "语音包已删除")
            }
            refreshVoicePacks()
            refreshKokoroVoiceStatus()
            if (uiState.floatingOverlayEnabled) {
                FloatingOverlayService.refresh(appContext)
            }
        }
    }

    fun shareVoice(pack: VoicePackInfo) {
        if (isSystemTtsVoicePack(pack)) {
            uiState = uiState.copy(status = "系统 TTS 不能分享")
            return
        }
        if (isKokoroVoicePack(pack)) {
            uiState = uiState.copy(status = "Kokoro 离线语音由设置中的资源安装器管理，不能作为普通语音包分享")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val shareDir = File(appContext.cacheDir, "share")
            val fileName = "${repo.sanitizeVoicePackShareName(pack.meta.name, pack.dir.name)}.kigvpk"
            val outZip = File(shareDir, fileName)
            repo.zipVoicePack(pack.dir, outZip)
            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(
                    appContext,
                    appContext.packageName + ".fileprovider",
                    outZip
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/x-kigtts-voicepack"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                appContext.startActivity(Intent.createChooser(intent, "分享语音包"))
            }
        }
    }

    private fun sharePresetFile(file: File, mimeType: String, chooserTitle: String) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                appContext,
                appContext.packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            appContext.startActivity(Intent.createChooser(intent, chooserTitle))
        }.onFailure { e ->
            uiState = uiState.copy(status = "分享失败：${e.message ?: "未知错误"}")
        }
    }

    fun setMuteWhilePlaying(enabled: Boolean) {
        uiState = uiState.copy(muteWhilePlaying = enabled)
        realtimeHost?.setSuppressWhilePlaying(enabled)
        viewModelScope.launch {
            UserPrefs.setMuteWhilePlaying(appContext, enabled)
        }
    }

    fun setMuteWhilePlayingDelay(seconds: Float) {
        val clamped = seconds.coerceIn(0f, 5f)
        uiState = uiState.copy(muteWhilePlayingDelaySec = clamped)
        realtimeHost?.setSuppressDelaySec(clamped)
        viewModelScope.launch {
            UserPrefs.setMuteWhilePlayingDelaySec(appContext, clamped)
        }
    }

    fun setMinVolumePercent(percent: Int) {
        uiState = uiState.copy(minVolumePercent = percent)
        realtimeHost?.setMinVolumePercent(percent)
        viewModelScope.launch {
            UserPrefs.setMinVolumePercent(appContext, percent)
        }
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

    private fun persistVadFlags(classicEnabled: Boolean, sileroEnabled: Boolean) {
        realtimeHost?.setClassicVadEnabled(classicEnabled)
        realtimeHost?.setSileroVadEnabled(sileroEnabled)
        viewModelScope.launch {
            UserPrefs.setVadFlags(appContext, classicEnabled, sileroEnabled)
        }
    }

    fun setClassicVadEnabled(enabled: Boolean) {
        val (classicEnabled, sileroEnabled) = normalizeVadFlags(enabled, uiState.sileroVadEnabled)
        uiState = uiState.copy(
            classicVadEnabled = classicEnabled,
            sileroVadEnabled = sileroEnabled
        )
        persistVadFlags(classicEnabled, sileroEnabled)
    }

    fun setSileroVadEnabled(enabled: Boolean) {
        val (classicEnabled, sileroEnabled) = normalizeVadFlags(uiState.classicVadEnabled, enabled)
        uiState = uiState.copy(
            classicVadEnabled = classicEnabled,
            sileroVadEnabled = sileroEnabled
        )
        persistVadFlags(classicEnabled, sileroEnabled)
    }

    fun setSileroVadThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(
            UserPrefs.SILERO_VAD_MIN_THRESHOLD,
            UserPrefs.SILERO_VAD_MAX_THRESHOLD
        )
        val stepped = (clamped / 0.05f).roundToInt() * 0.05f
        val normalized = stepped.coerceIn(
            UserPrefs.SILERO_VAD_MIN_THRESHOLD,
            UserPrefs.SILERO_VAD_MAX_THRESHOLD
        )
        uiState = uiState.copy(sileroVadThreshold = normalized)
        realtimeHost?.setSileroVadThreshold(normalized)
        viewModelScope.launch {
            UserPrefs.setSileroVadThreshold(appContext, normalized)
        }
    }

    fun setSileroVadPreRollMs(preRollMs: Int) {
        val normalized = ((preRollMs / 50f).roundToInt() * 50).coerceIn(
            UserPrefs.SILERO_VAD_MIN_PRE_ROLL_MS,
            UserPrefs.SILERO_VAD_MAX_PRE_ROLL_MS
        )
        uiState = uiState.copy(sileroVadPreRollMs = normalized)
        realtimeHost?.setSileroVadPreRollMs(normalized)
        viewModelScope.launch {
            UserPrefs.setSileroVadPreRollMs(appContext, normalized)
        }
    }

    fun setVadMode(mode: Int) {
        val (classicEnabled, sileroEnabled) = VadMode.toFlags(mode)
        uiState = uiState.copy(
            classicVadEnabled = classicEnabled,
            sileroVadEnabled = sileroEnabled
        )
        persistVadFlags(classicEnabled, sileroEnabled)
    }

    fun setPlaybackGainPercent(percent: Int) {
        val clamped = snapPlaybackGainPercent(percent)
        uiState = uiState.copy(playbackGainPercent = clamped)
        SoundboardManager.setPlaybackGainPercent(clamped)
        realtimeHost?.setPlaybackGainPercent(clamped)
        viewModelScope.launch {
            UserPrefs.setPlaybackGainPercent(appContext, clamped)
        }
    }

    fun setPiperNoiseScale(value: Float) {
        val clamped = value.coerceIn(0f, 2f)
        uiState = uiState.copy(piperNoiseScale = clamped)
        realtimeHost?.setPiperNoiseScale(clamped)
        viewModelScope.launch {
            UserPrefs.setPiperNoiseScale(appContext, clamped)
        }
    }

    fun setPiperLengthScale(value: Float) {
        val clamped = value.coerceIn(0.1f, 5f)
        uiState = uiState.copy(piperLengthScale = clamped)
        realtimeHost?.setPiperLengthScale(clamped)
        viewModelScope.launch {
            UserPrefs.setPiperLengthScale(appContext, clamped)
        }
    }

    fun setPiperNoiseW(value: Float) {
        val clamped = value.coerceIn(0f, 2f)
        uiState = uiState.copy(piperNoiseW = clamped)
        realtimeHost?.setPiperNoiseW(clamped)
        viewModelScope.launch {
            UserPrefs.setPiperNoiseW(appContext, clamped)
        }
    }

    fun setPiperSentenceSilence(value: Float) {
        val clamped = value.coerceIn(0f, 2f)
        uiState = uiState.copy(piperSentenceSilence = clamped)
        realtimeHost?.setPiperSentenceSilenceSec(clamped)
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
        realtimeHost?.setNumberReplaceMode(clamped)
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
        if (enabled && uiState.running) {
            stop()
        }
        uiState = uiState.copy(
            pushToTalkMode = enabled,
            running = if (enabled) false else uiState.running,
            pushToTalkPressed = false,
            pushToTalkStreamingText = ""
        )
        realtimeHost?.setPushToTalkStreamingEnabled(false)
        realtimeHost?.setSuppressAsrAutoSpeak(uiState.ttsDisabled || (enabled && uiState.pushToTalkConfirmInputMode))
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
        val streamingEnabled = enabled && uiState.pushToTalkMode && uiState.pushToTalkPressed
        realtimeHost?.setPushToTalkStreamingEnabled(streamingEnabled)
        realtimeHost?.setSuppressAsrAutoSpeak(uiState.ttsDisabled || (enabled && uiState.pushToTalkMode))
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

    fun setFloatingOverlayShowOnLockScreen(enabled: Boolean) {
        uiState = uiState.copy(floatingOverlayShowOnLockScreen = enabled)
        viewModelScope.launch {
            UserPrefs.setFloatingOverlayShowOnLockScreen(appContext, enabled)
        }
    }

    fun setFloatingOverlayHardcodedShortcutSupplement(enabled: Boolean) {
        uiState = uiState.copy(floatingOverlayHardcodedShortcutSupplement = enabled)
        viewModelScope.launch {
            UserPrefs.setFloatingOverlayHardcodedShortcutSupplement(appContext, enabled)
        }
    }

    fun setVolumeHotkeyEnabled(sequence: VolumeHotkeySequence, enabled: Boolean) {
        uiState = when (sequence) {
            VolumeHotkeySequence.UpDown -> uiState.copy(volumeHotkeyUpDownEnabled = enabled)
            VolumeHotkeySequence.DownUp -> uiState.copy(volumeHotkeyDownUpEnabled = enabled)
        }
        viewModelScope.launch {
            UserPrefs.setVolumeHotkeyEnabled(appContext, sequence, enabled)
        }
    }

    fun setVolumeHotkeyAction(sequence: VolumeHotkeySequence, action: VolumeHotkeyActionSpec) {
        uiState = when (sequence) {
            VolumeHotkeySequence.UpDown -> uiState.copy(volumeHotkeyUpDownAction = action)
            VolumeHotkeySequence.DownUp -> uiState.copy(volumeHotkeyDownUpAction = action)
        }
        viewModelScope.launch {
            UserPrefs.setVolumeHotkeyAction(appContext, sequence, action)
        }
    }

    fun setVolumeHotkeyWindowMs(windowMs: Int) {
        val normalized = (windowMs / 100) * 100
        val clamped = normalized.coerceIn(
            UserPrefs.VOLUME_HOTKEY_MIN_WINDOW_MS,
            UserPrefs.VOLUME_HOTKEY_MAX_WINDOW_MS
        )
        uiState = uiState.copy(volumeHotkeyWindowMs = clamped)
        viewModelScope.launch {
            UserPrefs.setVolumeHotkeyWindowMs(appContext, clamped)
        }
    }

    fun setVolumeHotkeyAccessibilityEnabled(enabled: Boolean) {
        uiState = uiState.copy(volumeHotkeyAccessibilityEnabled = enabled)
        viewModelScope.launch {
            UserPrefs.setVolumeHotkeyAccessibilityEnabled(appContext, enabled)
        }
    }

    fun setVolumeHotkeyEnableWarningDismissed(dismissed: Boolean) {
        uiState = uiState.copy(volumeHotkeyEnableWarningDismissed = dismissed)
        viewModelScope.launch {
            UserPrefs.setVolumeHotkeyEnableWarningDismissed(appContext, dismissed)
        }
    }

    fun setTtsDisabled(enabled: Boolean) {
        uiState = uiState.copy(ttsDisabled = enabled)
        realtimeHost?.setTtsDisabled(enabled)
        realtimeHost?.setSuppressAsrAutoSpeak(
            enabled || (uiState.pushToTalkMode && uiState.pushToTalkConfirmInputMode)
        )
        viewModelScope.launch {
            UserPrefs.setTtsDisabled(appContext, enabled)
        }
    }

    fun setSoundboardKeywordTriggerEnabled(enabled: Boolean) {
        uiState = uiState.copy(soundboardKeywordTriggerEnabled = enabled)
        viewModelScope.launch {
            UserPrefs.setSoundboardKeywordTriggerEnabled(appContext, enabled)
        }
    }

    fun setSoundboardSuppressTtsOnKeyword(enabled: Boolean) {
        uiState = uiState.copy(soundboardSuppressTtsOnKeyword = enabled)
        viewModelScope.launch {
            UserPrefs.setSoundboardSuppressTtsOnKeyword(appContext, enabled)
        }
    }

    fun setAllowQuickTextTriggerSoundboard(enabled: Boolean) {
        uiState = uiState.copy(allowQuickTextTriggerSoundboard = enabled)
        viewModelScope.launch {
            UserPrefs.setAllowQuickTextTriggerSoundboard(appContext, enabled)
        }
    }

    fun setQuickSubtitleInterruptQueue(enabled: Boolean) {
        uiState = uiState.copy(quickSubtitleInterruptQueue = enabled)
        viewModelScope.launch {
            UserPrefs.setQuickSubtitleInterruptQueue(appContext, enabled)
        }
    }

    fun setQuickSubtitleAutoFit(enabled: Boolean) {
        uiState = uiState.copy(quickSubtitleAutoFit = enabled)
        viewModelScope.launch {
            UserPrefs.setQuickSubtitleAutoFit(appContext, enabled)
        }
    }

    fun setQuickSubtitleCompactControls(enabled: Boolean) {
        uiState = uiState.copy(quickSubtitleCompactControls = enabled)
        viewModelScope.launch {
            UserPrefs.setQuickSubtitleCompactControls(appContext, enabled)
        }
    }

    fun setQuickSubtitleKeepInputPreview(enabled: Boolean) {
        uiState = uiState.copy(quickSubtitleKeepInputPreview = enabled)
        viewModelScope.launch {
            UserPrefs.setQuickSubtitleKeepInputPreview(appContext, enabled)
        }
    }

    fun setBluetoothMediaTitleSubtitle(enabled: Boolean) {
        uiState = uiState.copy(bluetoothMediaTitleSubtitle = enabled)
        BluetoothMediaTitleBridge.setEnabled(appContext, enabled)
        if (enabled) {
            BluetoothMediaTitleBridge.updateSubtitle(appContext, quickSubtitleCurrentText)
        }
        viewModelScope.launch {
            UserPrefs.setBluetoothMediaTitleSubtitle(appContext, enabled)
        }
    }

    fun setDrawingKeepCanvasOrientationToDevice(enabled: Boolean) {
        uiState = uiState.copy(drawingKeepCanvasOrientationToDevice = enabled)
        viewModelScope.launch {
            UserPrefs.setDrawingKeepCanvasOrientationToDevice(appContext, enabled)
        }
    }

    fun setPushToTalkPressed(pressed: Boolean) {
        if (uiState.pushToTalkPressed == pressed) return
        uiState = uiState.copy(
            pushToTalkPressed = pressed,
            pushToTalkStreamingText = if (!pressed) "" else uiState.pushToTalkStreamingText
        )
        val enabled = uiState.pushToTalkMode && uiState.pushToTalkConfirmInputMode && pressed
        val host = requestRealtimeHost()
        if (host != null) {
            host.setPushToTalkPressed(pressed)
        } else if (enabled) {
            pendingHostStartRequest = true
        }
    }

    fun beginPushToTalkSession() {
        if (!uiState.pushToTalkConfirmInputMode) return
        pttSessionLastText = ""
        pttSessionCommitConsumed = false
        resetPttHistoryDedup()
        uiState = uiState.copy(pushToTalkStreamingText = "")
        realtimeHost?.beginPushToTalkSession()
    }

    fun commitPushToTalkSession(action: PttConfirmReleaseAction) {
        if (!uiState.pushToTalkConfirmInputMode) return
        if (pttSessionCommitConsumed) return
        realtimeHost?.let { host ->
            pttSessionCommitConsumed = true
            val mappedAction = when (action) {
                PttConfirmReleaseAction.SendToSubtitle -> RealtimeRuntimeBridge.PttCommitAction.SendToSubtitle
                PttConfirmReleaseAction.SendToInput -> RealtimeRuntimeBridge.PttCommitAction.SendToInput
                PttConfirmReleaseAction.Cancel -> RealtimeRuntimeBridge.PttCommitAction.Cancel
            }
            host.commitPushToTalkSession(mappedAction)
            pttSessionLastText = ""
            resetPttHistoryDedup()
            return
        }
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
        if (uiState.ttsDisabled) {
            appendRecognizedHistory(message)
            uiState = uiState.copy(status = TTS_DISABLED_MESSAGE)
            return
        }
        viewModelScope.launch {
            if (shouldSuppressVoiceTtsForSoundboard(message)) {
                appendRecognizedHistory(message)
                uiState = uiState.copy(status = "已触发音效板，跳过本句朗读")
                return@launch
            }
            val host = requestRealtimeHost("音频宿主初始化中")
            val queuedId = host?.speakText(message)
            if (queuedId != null) {
                appendRecognizedHistory(message, queuedId)
                uiState = uiState.copy(status = "已加入朗读队列")
            } else if (host != null) {
                appendRecognizedHistory(message)
            }
        }
    }

    fun canAddSpeakerProfile(): Boolean {
        return speakerProfiles.size < MAX_SPEAKER_PROFILES
    }

    private fun speakerProfileUiItems(): List<SpeakerProfileUiItem> {
        return speakerProfiles.mapIndexed { index, profile ->
            SpeakerProfileUiItem(id = profile.id, name = "样本 ${index + 1}")
        }
    }

    private fun speakerProfileVectors(): List<FloatArray> {
        return speakerProfiles.map { it.vector.copyOf() }
    }

    fun setSpeakerVerifyEnabled(enabled: Boolean) {
        uiState = uiState.copy(speakerVerifyEnabled = enabled)
        realtimeHost?.setSpeakerVerifyEnabled(enabled)
        viewModelScope.launch {
            UserPrefs.setSpeakerVerifyEnabled(appContext, enabled)
        }
        if (enabled && speakerProfiles.isEmpty()) {
            uiState = uiState.copy(status = "说话人验证已开启，请先采集本人语音样本")
        }
    }

    fun setSpeakerVerifyThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0.05f, 0.95f)
        uiState = uiState.copy(speakerVerifyThreshold = clamped)
        realtimeHost?.setSpeakerVerifyThreshold(clamped)
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
            status = "已清除本人语音样本"
        )
        realtimeHost?.let { host ->
            host.setSpeakerVerifyEnabled(false)
            host.clearSpeakerProfiles()
        }
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
            status = if (hasProfiles) "已移除注册样本" else "已清除本人语音样本"
        )
        realtimeHost?.let { host ->
            host.setSpeakerVerifyEnabled(keepVerify)
            host.setSpeakerProfiles(speakerProfileVectors())
        }
        viewModelScope.launch {
            UserPrefs.setSpeakerVerifyEnabled(appContext, keepVerify)
            UserPrefs.setSpeakerVerifyProfiles(appContext, speakerProfiles)
        }
    }

    fun applySpeakerProfile(profile: FloatArray): Boolean {
        return applySpeakerProfiles(listOf(profile))
    }

    fun applySpeakerProfiles(profiles: List<FloatArray>): Boolean {
        val normalizedProfiles = profiles
            .mapNotNull { profile -> if (profile.isEmpty()) null else profile.copyOf() }
            .take(MAX_SPEAKER_PROFILES)
        if (normalizedProfiles.isEmpty()) {
            return false
        }
        val baseId = SystemClock.elapsedRealtime()
        speakerProfiles = normalizedProfiles.mapIndexed { index, profile ->
            UserPrefs.SpeakerVerifyProfile(
                id = "spk-$baseId-${index + 1}",
                name = "样本 ${index + 1}",
                vector = profile
            )
        }.toMutableList()
        realtimeHost?.setSpeakerProfiles(speakerProfileVectors())
        uiState = uiState.copy(
            speakerProfileReady = true,
            speakerProfiles = speakerProfileUiItems(),
            speakerLastSimilarity = -1f,
            status = "本人语音样本已保存（${speakerProfiles.size}/$MAX_SPEAKER_PROFILES）"
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
            val msg = "请先停止麦克风再采集本人样本"
            uiState = uiState.copy(status = msg)
            return SpeakerEnrollResult(success = false, message = msg)
        }
        uiState = uiState.copy(status = "说话人注册中（请持续说话约${durationSec.toInt()}秒）...")
        val host = requestRealtimeHost("音频宿主初始化中，请稍后重试")
            ?: return SpeakerEnrollResult(
                success = false,
                message = "音频宿主初始化中，请稍后重试"
            )
        val result = host.enrollSpeaker(durationSec, onCapture)
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

    fun setThemeMode(mode: Int) {
        val normalized = UserPrefs.normalizeThemeMode(mode)
        uiState = uiState.copy(themeMode = normalized)
        viewModelScope.launch {
            UserPrefs.setThemeMode(appContext, normalized)
        }
    }

    fun setOverlayThemeMode(mode: Int) {
        val normalized = UserPrefs.normalizeThemeMode(mode)
        uiState = uiState.copy(overlayThemeMode = normalized)
        viewModelScope.launch {
            UserPrefs.setOverlayThemeMode(appContext, normalized)
        }
    }

    fun setFontScaleBlockMode(mode: Int) {
        val normalized = UserPrefs.normalizeFontScaleBlockMode(mode)
        FontScaleBlockRuntime.mode = normalized
        uiState = uiState.copy(fontScaleBlockMode = normalized)
        viewModelScope.launch {
            UserPrefs.setFontScaleBlockMode(appContext, normalized)
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        uiState = uiState.copy(hapticFeedbackEnabled = enabled)
        viewModelScope.launch {
            UserPrefs.setHapticFeedbackEnabled(appContext, enabled)
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

    fun setUseBuiltinFileManager(enabled: Boolean) {
        uiState = uiState.copy(useBuiltinFileManager = enabled)
        viewModelScope.launch {
            UserPrefs.setUseBuiltinFileManager(appContext, enabled)
        }
    }

    fun setUseBuiltinGallery(enabled: Boolean) {
        uiState = uiState.copy(useBuiltinGallery = enabled)
        viewModelScope.launch {
            UserPrefs.setUseBuiltinGallery(appContext, enabled)
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

    fun rotateDrawingCanvasQuarterTurns(delta: Int) {
        drawingManualRotationQuarterTurns =
            ((drawingManualRotationQuarterTurns + delta) % 4 + 4) % 4
    }

    fun clearDrawingBoard() {
        drawStrokes.clear()
    }

    fun appendDrawingStroke(points: List<DrawPoint>, eraserOverride: Boolean? = null) {
        if (points.size < 2) return
        val useEraser = eraserOverride ?: drawEraser
        val effectiveWidth = if (useEraser) drawEraserSize * 5f else drawBrushSize
        drawStrokes.add(
            DrawStrokeData(
                points = points,
                color = drawColor,
                width = effectiveWidth,
                eraser = useEraser
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
        realtimeHost?.setUseVoiceCommunication(enabled)
        viewModelScope.launch {
            UserPrefs.setEchoSuppression(appContext, enabled)
        }
        if (wasRunning) {
            restartJob?.cancel()
            restartJob = viewModelScope.launch {
                realtimeHost?.restartRecorder()
            }
        }
    }

    fun setCommunicationMode(enabled: Boolean) {
        val wasRunning = uiState.running
        uiState = uiState.copy(communicationMode = enabled)
        realtimeHost?.setCommunicationMode(enabled)
        viewModelScope.launch {
            UserPrefs.setCommunicationMode(appContext, enabled)
        }
        if (wasRunning) {
            restartJob?.cancel()
            restartJob = viewModelScope.launch {
                realtimeHost?.restartRecorder()
            }
        }
    }

    fun setPreferredOutputType(type: Int) {
        val wasRunning = uiState.running
        uiState = uiState.copy(preferredOutputType = type)
        realtimeHost?.setPreferredOutputType(type)
        viewModelScope.launch {
            UserPrefs.setPreferredOutputType(appContext, type)
        }
        if (wasRunning) {
            restartJob?.cancel()
            restartJob = viewModelScope.launch {
                realtimeHost?.restartRecorder()
            }
        }
    }

    fun setPreferredInputType(type: Int) {
        val wasRunning = uiState.running
        uiState = uiState.copy(preferredInputType = type)
        realtimeHost?.setPreferredInputType(type)
        viewModelScope.launch {
            UserPrefs.setPreferredInputType(appContext, type)
        }
        if (wasRunning) {
            restartJob?.cancel()
            restartJob = viewModelScope.launch {
                realtimeHost?.restartRecorder()
            }
        }
    }

    fun setAec3Enabled(enabled: Boolean) {
        uiState = uiState.copy(
            aec3Enabled = enabled,
            aec3Status = if (enabled) "初始化中" else "未启用"
        )
        realtimeHost?.setUseAec3(enabled)
        viewModelScope.launch {
            UserPrefs.setAec3Enabled(appContext, enabled)
        }
    }

    fun setDenoiserMode(mode: Int) {
        val normalized = mode.coerceIn(AudioDenoiserMode.OFF, AudioDenoiserMode.SPEEX)
        uiState = uiState.copy(denoiserMode = normalized)
        realtimeHost?.setDenoiserMode(normalized)
        viewModelScope.launch {
            UserPrefs.setDenoiserMode(appContext, normalized)
        }
    }

    fun setSpeechEnhancementMode(mode: Int) {
        val normalized = SpeechEnhancementMode.clamp(mode)
        uiState = uiState.copy(speechEnhancementMode = normalized)
        realtimeHost?.setSpeechEnhancementMode(normalized)
        viewModelScope.launch {
            UserPrefs.setSpeechEnhancementMode(appContext, normalized)
        }
    }

    private fun currentAudioTestConfig(): AudioTestConfig {
        return AudioTestConfig(
            audioSource = if (uiState.echoSuppression) {
                MediaRecorder.AudioSource.VOICE_COMMUNICATION
            } else {
                MediaRecorder.AudioSource.MIC
            },
            preferredInputType = uiState.preferredInputType,
            preferredOutputType = uiState.preferredOutputType,
            useCommunicationMode = uiState.communicationMode,
            speechEnhancementMode = uiState.speechEnhancementMode
        )
    }

    fun startAudioTestRecording() {
        if (uiState.running || realtimeHost?.isMicActive() == true || RealtimeOwnerGate.currentOwner() != null) {
            uiState = uiState.copy(audioTestStatus = "请先停止语音转换再测试录音")
            return
        }
        val started = audioTest.startRecording(currentAudioTestConfig())
        if (!started) {
            uiState = uiState.copy(audioTestStatus = "音频测试：无法开始录制")
        }
    }

    fun stopAudioTestRecording() {
        audioTest.stopRecording()
    }

    fun startAudioTestPlayback() {
        val started = audioTest.play(currentAudioTestConfig())
        if (!started) {
            uiState = uiState.copy(audioTestStatus = "音频测试：请先录制一段测试音频")
        }
    }

    fun stopAudioTestPlayback() {
        audioTest.stopPlayback()
    }

    fun clearAudioTest() {
        audioTest.clear()
    }

    fun speakText(
        text: String,
        fromQuickText: Boolean = false,
        interruptCurrent: Boolean = false
    ) {
        val message = text.trim()
        if (message.isEmpty()) return
        if (uiState.ttsDisabled) {
            appendRecognizedHistory(message, fromQuickText = fromQuickText)
            uiState = uiState.copy(status = TTS_DISABLED_MESSAGE)
            return
        }
        if (uiState.voiceDir == null) {
            uiState = uiState.copy(status = "请先选择语音包")
            return
        }
        val interruptSerial = if (fromQuickText && interruptCurrent) {
            quickSubtitleInterruptRequestSerial.incrementAndGet()
        } else {
            null
        }
        viewModelScope.launch {
            if (interruptSerial != null && interruptSerial != quickSubtitleInterruptRequestSerial.get()) {
                return@launch
            }
            val host = requestRealtimeHost("音频宿主初始化中")
            if (interruptSerial != null && interruptSerial != quickSubtitleInterruptRequestSerial.get()) {
                return@launch
            }
            val queuedId = host?.speakText(message, interruptCurrent = interruptCurrent)
            if (interruptSerial != null && interruptSerial != quickSubtitleInterruptRequestSerial.get()) {
                return@launch
            }
            if (queuedId != null) {
                // 便捷字幕的快速文本/输入框触发朗读时，也要进入历史记录。
                // 使用队列ID绑定，避免与 onResult 回调重复插入。
                appendRecognizedHistory(message, queuedId, fromQuickText = fromQuickText)
                uiState = uiState.copy(status = "已加入朗读队列")
            }
        }
    }

    fun start() {
        val voice = uiState.voiceDir
        val requireVoice = !uiState.ttsDisabled
        if (requireVoice && voice == null) {
            uiState = uiState.copy(
                status = "请先选择语音包"
            )
            return
        }
        val asr = uiState.asrDir
        if (asr == null) {
            uiState = uiState.copy(status = "正在加载语音识别资源包")
            viewModelScope.launch {
                val dir = withContext(Dispatchers.IO) { repo.ensureBundledAsr() }
                if (dir == null) {
                    refreshRecognitionResourceStatus()
                    uiState = uiState.copy(status = "请先安装语音识别资源包")
                    return@launch
                }
                uiState = uiState.copy(asrDir = dir, status = "已加载语音识别资源包")
                val host = realtimeHost
                if (host != null) {
                    host.updateSelectedAsrDir(dir, status = "已加载语音识别资源包", preload = true)
                } else {
                    preloadAsr(dir)
                }
                start()
            }
            return
        }
        val host = requestRealtimeHost("音频宿主初始化中")
        if (host != null) {
            restartJob?.cancel()
            restartJob = null
            host.startRealtime()
            return
        }
        pendingHostStartRequest = true
    }

    fun stop() {
        realtimeHost?.let { host ->
            restartJob?.cancel()
            restartJob = null
            pttSessionLastText = ""
            resetPttHistoryDedup()
            pendingHostStartRequest = false
            host.stopRealtime()
            return
        }
        restartJob?.cancel()
        restartJob = null
        pttSessionLastText = ""
        resetPttHistoryDedup()
        pendingHostStartRequest = false
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

    override fun onCleared() {
        detachRealtimeHost()
        audioTest.release()
        settingsObserveJob?.cancel()
        settingsObserveJob = null
        RealtimeOwnerGate.release(APP_REALTIME_OWNER_TAG)
        super.onCleared()
    }

    private fun applySettingsToController(settings: UserPrefs.AppSettings) {
        realtimeHost?.let { host ->
            host.setSuppressWhilePlaying(settings.muteWhilePlaying)
            host.setSuppressDelaySec(settings.muteWhilePlayingDelaySec)
            host.setMinVolumePercent(settings.minVolumePercent)
            host.setPlaybackGainPercent(settings.playbackGainPercent)
            host.setPiperNoiseScale(settings.piperNoiseScale)
            host.setPiperLengthScale(settings.piperLengthScale)
            host.setPiperNoiseW(0.8f)
            host.setPiperSentenceSilenceSec(settings.piperSentenceSilence)
            host.setUseAec3(settings.aec3Enabled)
            host.setUseVoiceCommunication(settings.echoSuppression)
            host.setCommunicationMode(settings.communicationMode)
            host.setPreferredInputType(settings.preferredInputType)
            host.setPreferredOutputType(settings.preferredOutputType)
            host.setDenoiserMode(settings.denoiserMode)
            host.setSpeechEnhancementMode(settings.speechEnhancementMode)
            host.setClassicVadEnabled(settings.classicVadEnabled)
            host.setSileroVadEnabled(settings.sileroVadEnabled)
            host.setSileroVadThreshold(settings.sileroVadThreshold)
            host.setSileroVadPreRollMs(settings.sileroVadPreRollMs)
            host.setNumberReplaceMode(settings.numberReplaceMode)
            host.setSpeakerVerifyEnabled(uiState.speakerVerifyEnabled)
            host.setSpeakerVerifyThreshold(settings.speakerVerifyThreshold)
            host.setSpeakerProfiles(speakerProfileVectors())
            host.setSuppressAsrAutoSpeak(
                uiState.pushToTalkMode && uiState.pushToTalkConfirmInputMode
            )
            host.setPushToTalkStreamingEnabled(
                uiState.pushToTalkMode &&
                        uiState.pushToTalkConfirmInputMode &&
                        uiState.pushToTalkPressed
            )
            return
        }
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
    val WideContentMaxWidth = 860.dp
    val WideListMaxWidth = 900.dp
    val DrawerWidthExpanded = 216.dp
    val DrawerWidthCollapsed = 72.dp
    val LightCard = Color(0xFFFFFFFF)
    val DarkCard = Color(0xFF1D2023)
}

@Composable
private fun CenteredPageBox(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = horizontalPadding)
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .fillMaxHeight(),
            content = content
        )
    }
}

@Composable
private fun CenteredPageColumn(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    scroll: ScrollState? = null,
    horizontalPadding: Dp = 16.dp,
    contentSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    CenteredPageBox(
        maxWidth = maxWidth,
        modifier = modifier,
        horizontalPadding = horizontalPadding
    ) {
        var columnModifier = Modifier.fillMaxWidth()
        if (scroll != null) {
            columnModifier = columnModifier.verticalScroll(scroll)
        }
        Column(
            modifier = columnModifier,
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content
        )
    }
}

private data class MdShadowLayer(
    val offsetY: Dp,
    val blur: Dp,
    val spread: Dp,
    val alpha: Float
)

private data class MdShadowStyle(
    val umbra: MdShadowLayer,
    val penumbra: MdShadowLayer,
    val ambient: MdShadowLayer
)

private val MdCardShadowStyle = MdShadowStyle(
    umbra = MdShadowLayer(offsetY = 3.dp, blur = 1.dp, spread = (-2).dp, alpha = 0.14f),
    penumbra = MdShadowLayer(offsetY = 2.dp, blur = 2.dp, spread = 0.dp, alpha = 0.098f),
    ambient = MdShadowLayer(offsetY = 1.dp, blur = 5.dp, spread = 0.dp, alpha = 0.084f)
)

private val MdFabShadowStyle = MdShadowStyle(
    umbra = MdShadowLayer(offsetY = 3.dp, blur = 5.dp, spread = (-1).dp, alpha = 0.14f),
    penumbra = MdShadowLayer(offsetY = 6.dp, blur = 10.dp, spread = 0.dp, alpha = 0.098f),
    ambient = MdShadowLayer(offsetY = 1.dp, blur = 18.dp, spread = 0.dp, alpha = 0.084f)
)

private fun Modifier.mdCenteredShadow(
    shape: androidx.compose.ui.graphics.Shape,
    shadowStyle: MdShadowStyle
): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    fun drawLayer(layer: MdShadowLayer) {
        val blurPx = layer.blur.toPx()
        val offsetYPx = layer.offsetY.toPx()
        val spreadPx = layer.spread.toPx()
        val frameworkPaint = Paint().apply {
            isAntiAlias = true
            this.style = Paint.Style.FILL
            color = android.graphics.Color.argb(1, 0, 0, 0)
            setShadowLayer(blurPx, 0f, offsetYPx, Color.Black.copy(alpha = layer.alpha).toArgb())
        }
        drawIntoCanvas { canvas ->
            when (outline) {
                is Outline.Rectangle -> {
                    val rect = RectF(
                        -spreadPx,
                        -spreadPx,
                        size.width + spreadPx,
                        size.height + spreadPx
                    )
                    canvas.nativeCanvas.drawRect(rect, frameworkPaint)
                }
                is Outline.Rounded -> {
                    val roundRect = outline.roundRect
                    val rect = RectF(
                        roundRect.left - spreadPx,
                        roundRect.top - spreadPx,
                        roundRect.right + spreadPx,
                        roundRect.bottom + spreadPx
                    )
                    val radius = (roundRect.topLeftCornerRadius.x + spreadPx).coerceAtLeast(0f)
                    canvas.nativeCanvas.drawRoundRect(rect, radius, radius, frameworkPaint)
                }
                is Outline.Generic -> {
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(0f, 0f)
                    canvas.nativeCanvas.drawPath(outline.path.asAndroidPath(), frameworkPaint)
                    canvas.nativeCanvas.restore()
                }
            }
        }
    }
    drawLayer(shadowStyle.umbra)
    drawLayer(shadowStyle.penumbra)
    drawLayer(shadowStyle.ambient)
}

@Composable
private fun MdShadowCardSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(UiTokens.Radius),
    backgroundColor: Color = md2ElevatedCardContainerColor(),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.mdCenteredShadow(shape = shape, shadowStyle = MdCardShadowStyle)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            color = backgroundColor,
            elevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}

@Composable
private fun MdShadowCircleActionSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.mdCenteredShadow(shape = CircleShape, shadowStyle = MdFabShadowStyle)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = backgroundColor,
            contentColor = contentColor,
            elevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
        }
    }
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
private val LocalSuppressStaggeredFloatIn = staticCompositionLocalOf { false }

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
    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose { onTopBarActionsChange(null) }
    }
    val navigateDecodedQrResult: (String, String) -> Unit = { decoded, popRoute ->
        if (isWeChatQrContent(decoded)) {
            if (isPackageInstalled(context, WECHAT_PACKAGE_NAME)) {
                toast(context, "该二维码为微信二维码，需要使用微信进行扫描")
                if (!launchWeChatScanner(context)) {
                    toast(context, "打开微信失败，请手动打开微信扫一扫")
                }
            } else {
                toast(context, "该二维码为微信二维码，需要安装微信")
                val browserTarget = normalizeQrTextToWebUrl(decoded) ?: WECHAT_BROWSER_FALLBACK_URL
                if (!openExternalBrowser(context, browserTarget)) {
                    toast(context, "无法打开系统浏览器")
                }
            }
            navController.popBackStack(popRoute, inclusive = true)
        } else if (QqScannerSupport.isQqQrContent(decoded)) {
            if (isPackageInstalled(context, QqScannerSupport.QQ_PACKAGE_NAME)) {
                val qqAccessibilityEnabled = VolumeHotkeyAccessibilityService.isEnabled(context)
                if (qqAccessibilityEnabled) {
                    if (
                        VolumeHotkeyAccessibilityService.requestOpenQqScanner(context) ||
                        QqScannerSupport.launchQq(context)
                    ) {
                        toast(context, "该二维码为QQ二维码，请使用QQ进行扫描")
                    } else {
                        toast(context, "打开QQ失败，请手动打开QQ扫一扫")
                    }
                } else {
                    if (QqScannerSupport.launchQq(context)) {
                        toast(context, "该二维码为QQ二维码，已跳转至QQ。直达QQ扫一扫需要开启无障碍权限")
                    } else {
                        toast(context, "打开QQ失败，请手动打开QQ扫一扫")
                    }
                }
            } else {
                toast(context, "该二维码为QQ二维码，需要安装QQ")
                val browserTarget = normalizeQrTextToWebUrl(decoded) ?: QqScannerSupport.QQ_BROWSER_FALLBACK_URL
                if (!openExternalBrowser(context, browserTarget)) {
                    toast(context, "无法打开系统浏览器")
                }
            }
            navController.popBackStack(popRoute, inclusive = true)
        } else if (AlipayScannerSupport.isAlipayQrContent(decoded)) {
            if (isPackageInstalled(context, AlipayScannerSupport.ALIPAY_PACKAGE_NAME)) {
                toast(context, "该二维码为支付宝二维码，需要使用支付宝进行扫描")
                if (!AlipayScannerSupport.launchScanner(context)) {
                    toast(context, "打开支付宝失败，请手动打开支付宝扫一扫")
                }
            } else {
                toast(context, "该二维码为支付宝二维码，需要安装支付宝")
                val browserTarget = normalizeQrTextToWebUrl(decoded)
                    ?: AlipayScannerSupport.ALIPAY_BROWSER_FALLBACK_URL
                if (!openExternalBrowser(context, browserTarget)) {
                    toast(context, "无法打开系统浏览器")
                }
            }
            navController.popBackStack(popRoute, inclusive = true)
        } else {
            val url = normalizeQrTextToWebUrl(decoded)
            if (url.isNullOrEmpty()) {
                navController.navigate(QuickCardRoutes.scanText(decoded)) {
                    popUpTo(popRoute) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                navController.navigate(QuickCardRoutes.web(url)) {
                    popUpTo(popRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    val handleDecodedQrResult: (String) -> Unit = { decoded ->
        navigateDecodedQrResult(decoded, QuickCardRoutes.Scanner)
    }
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
                onResult = handleDecodedQrResult,
                onCandidates = { items ->
                    navController.navigate(QuickCardRoutes.scanCandidates(items)) {
                        popUpTo(QuickCardRoutes.Scanner) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = QuickCardRoutes.ScanCandidates,
            arguments = listOf(navArgument("items") { type = NavType.StringType })
        ) { entry ->
            val items = remember(entry) {
                runCatching {
                    val raw = Uri.decode(entry.arguments?.getString("items").orEmpty())
                    val arr = JSONArray(raw)
                    List(arr.length()) { idx -> arr.optString(idx).trim() }
                        .filter { it.isNotEmpty() }
                }.getOrDefault(emptyList())
            }
            QuickCardScanCandidatesScreen(
                items = items,
                onTopBarActionsChange = onTopBarActionsChange,
                onSelect = { decoded ->
                    navigateDecodedQrResult(decoded, QuickCardRoutes.ScanCandidates)
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
    val previewCardId = viewModel.quickCardPreviewCardId
    val previewCard = remember(cards, previewCardId) {
        previewCardId?.let { id -> cards.firstOrNull { it.id == id } }
    }
    val closePreview: () -> Unit = { viewModel.closeQuickCardPreview() }
    val onCreateCardState = rememberUpdatedState(onCreateCard)
    val onOpenScannerState = rememberUpdatedState(onOpenScanner)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onOpenScannerState.value()
        } else {
            toast(context, "未授予相机权限")
        }
    }

    val topActions = remember(cameraPermissionLauncher, context) {
        QuickCardTopBarActions(
            onNew = { onCreateCardState.value(QuickCardType.Text, "") },
            onScan = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                    onOpenScannerState.value()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, topActions) {
        onTopBarActionsChange(topActions)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onTopBarActionsChange(topActions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pageCount = (cards.size + 1).coerceAtLeast(1) // always keep a trailing "new card" page
    val selectedPage = if (cards.isEmpty()) 0 else viewModel.quickCardSelectedIndex.coerceIn(0, cards.lastIndex)
    var pagerPageIndex by rememberSaveable { mutableIntStateOf(selectedPage) }
    var showSortHint by rememberSaveable { mutableStateOf(false) }
    val canShowSortHint = cards.size > 1
    LaunchedEffect(canShowSortHint) {
        if (canShowSortHint && !quickCardSortHintShownThisProcess) {
            quickCardSortHintShownThisProcess = true
            showSortHint = true
            delay(2_000)
            showSortHint = false
        } else if (!canShowSortHint) {
            showSortHint = false
        }
    }
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
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                        onCreateCard(QuickCardType.Text, "")
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
                            QuickCardSortHintOverlay(
                                visible = showSortHint,
                                landscape = true,
                                onDismiss = { showSortHint = false }
                            )
                        }
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
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                        onCreateCard(QuickCardType.Text, "")
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
                            QuickCardSortHintOverlay(
                                visible = showSortHint,
                                landscape = false,
                                onDismiss = { showSortHint = false }
                            )
                        }
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
                    .background(Color.Black.copy(alpha = 0.52f))
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

}

@Composable
private fun QuickCardSortHintOverlay(
    visible: Boolean,
    landscape: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(170, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        onDismiss()
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        horizontal = if (landscape) 54.dp else 38.dp,
                        vertical = if (landscape) 18.dp else 26.dp
                    )
                    .fillMaxWidth(if (landscape) 0.62f else 0.82f)
                    .heightIn(min = 46.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.42f)
                            )
                        ),
                        shape = RoundedCornerShape(UiTokens.Radius)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "长按对名片进行排序。",
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center
                )
            }
        }
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

    val topActions = remember(onDone) {
        QuickCardTopBarActions(
            onConfirm = onDone,
            canConfirm = true
        )
    }
    LaunchedEffect(topActions) {
        onTopBarActionsChange(topActions)
    }

    CenteredPageColumn(
        maxWidth = UiTokens.WideListMaxWidth,
        contentSpacing = 0.dp
    ) {
            QuickCardSortRecyclerList(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                cards = cards,
                topBlankHeight = topBlank,
                bottomBlankHeight = bottomBlank,
                onReorder = { ids ->
                    viewModel.reorderQuickCardsByIds(ids)
                }
            )
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
                private val edgeAutoScroller = DragEdgeAutoScroller()

                override fun isLongPressDragEnabled(): Boolean = false
                override fun isItemViewSwipeEnabled(): Boolean = false

                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    if (viewHolder.bindingAdapterPosition == 0) {
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
                    val ok = adapter.move(from, to)
                    moved = moved || ok
                    return ok
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun onChildDraw(
                    c: android.graphics.Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                        edgeAutoScroller.update(recyclerView, viewHolder.itemView, dY)
                    } else {
                        edgeAutoScroller.stop()
                    }
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                        adapter.setDraggingPosition(viewHolder.bindingAdapterPosition)
                    } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        edgeAutoScroller.stop()
                        adapter.clearDraggingItem()
                    }
                    adapter.isDragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    edgeAutoScroller.stop()
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

    private companion object {
        const val HEADER_ID = Long.MIN_VALUE + 4601L
        const val HEADER_POSITION = 0
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return if (position == HEADER_POSITION) HEADER_ID else items[position - 1].id
    }

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

    override fun getItemCount(): Int = items.size + 1

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        if (position == HEADER_POSITION) {
            holder.bindHeader()
            return
        }
        val card = items[position - 1]
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
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun move(from: Int, to: Int): Boolean {
        val fromIndex = from - 1
        val toIndex = to - 1
        if (fromIndex == toIndex || fromIndex !in items.indices || toIndex !in items.indices) return false
        val moved = items.removeAt(fromIndex)
        items.add(toIndex, moved)
        notifyItemMoved(from, to)
        return true
    }

    fun snapshotIds(): List<Long> = items.map { it.id }

    fun setDraggingPosition(position: Int) {
        val targetId = items.getOrNull(position - 1)?.id
        if (draggingItemId == targetId) return
        val oldId = draggingItemId
        draggingItemId = targetId
        oldId?.let { id ->
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) notifyItemChanged(idx + 1)
        }
        targetId?.let { id ->
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) notifyItemChanged(idx + 1)
        }
    }

    fun clearDraggingItem() {
        val oldId = draggingItemId ?: return
        draggingItemId = null
        val idx = items.indexOfFirst { it.id == oldId }
        if (idx >= 0) notifyItemChanged(idx + 1)
    }

    class ItemViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bindHeader() {
            composeView.setContent {
                KigttsFontScaleProvider {
                    QuickCardSortHeaderRow()
                }
            }
        }

        fun bind(
            card: QuickCard,
            isDragged: Boolean,
            onStartDrag: () -> Unit
        ) {
            composeView.setContent {
                KigttsFontScaleProvider {
                    QuickCardSortRow(
                        card = card,
                        isDragged = isDragged,
                        onStartDrag = onStartDrag
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCardSortHeaderRow() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
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
    Spacer(Modifier.height(4.dp))
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
                    text = card.title.ifBlank { "未命名名片" },
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "快捷名片",
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
    onResult: (String) -> Unit,
    onCandidates: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val scanner = remember(lifecycleOwner) { createQrMlKitScanner() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analyzerExecutor = remember(lifecycleOwner) { Executors.newSingleThreadExecutor() }
    val scanned = remember { AtomicBoolean(false) }
    val analyzing = remember { AtomicBoolean(false) }
    val disposed = remember(lifecycleOwner) { AtomicBoolean(false) }
    val onResultState = rememberUpdatedState(onResult)
    val onCandidatesState = rememberUpdatedState(onCandidates)
    var cameraReady by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var minZoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var torchEnabled by remember { mutableStateOf(false) }
    var flashAvailable by remember { mutableStateOf(false) }
    val scaleDetector = remember(context) {
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val camera = boundCamera ?: return false
                    val target = (zoomRatio * detector.scaleFactor)
                        .coerceIn(minZoomRatio, maxZoomRatio.coerceAtLeast(minZoomRatio))
                    if (kotlin.math.abs(target - zoomRatio) < 0.01f) return false
                    camera.cameraControl.setZoomRatio(target)
                    zoomRatio = target
                    return true
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        onTopBarActionsChange(null)
    }

    DisposableEffect(previewView, scaleDetector) {
        previewView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            false
        }
        onDispose {
            previewView.setOnTouchListener(null)
        }
    }

    DisposableEffect(previewView, lifecycleOwner) {
        disposed.set(false)
        scanned.set(false)
        analyzing.set(false)
        cameraReady = false
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            if (disposed.get()) return@Runnable
            val provider = runCatching { providerFuture.get() }.getOrNull()
            if (provider == null) {
                if (!disposed.get()) {
                    toast(context, "相机初始化失败")
                    onOpenFailed()
                }
                return@Runnable
            }
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                if (disposed.get() || scanned.get() || !analyzing.compareAndSet(false, true)) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                try {
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !disposed.get()) {
                        val inputImage =
                            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        val mlTexts = runBlocking {
                            awaitTask(scanner.process(inputImage))?.decodedQrTexts().orEmpty()
                        }
                        when {
                            mlTexts.size > 1 && scanned.compareAndSet(false, true) -> {
                                mainExecutor.execute {
                                    if (!disposed.get()) {
                                        onCandidatesState.value(mlTexts)
                                    }
                                }
                            }
                            mlTexts.size == 1 && scanned.compareAndSet(false, true) -> {
                                val result = mlTexts.first()
                                mainExecutor.execute {
                                    if (!disposed.get()) {
                                        onResultState.value(result)
                                    }
                                }
                            }
                        }
                    }
                    if (!scanned.get() && !disposed.get()) {
                        val zxingText = decodeQrFromImageProxy(imageProxy)?.trim().orEmpty()
                        if (zxingText.isNotEmpty() && scanned.compareAndSet(false, true)) {
                            mainExecutor.execute {
                                if (!disposed.get()) {
                                    onResultState.value(zxingText)
                                }
                            }
                        }
                    }
                } finally {
                    analyzing.set(false)
                    imageProxy.close()
                }
            }

            runCatching {
                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                if (disposed.get()) {
                    provider.unbindAll()
                    return@runCatching
                }
                boundCamera = camera
                flashAvailable = camera.cameraInfo.hasFlashUnit()
                torchEnabled = camera.cameraInfo.torchState.value == TorchState.ON
                camera.cameraInfo.zoomState.value?.let { state ->
                    minZoomRatio = state.minZoomRatio
                    maxZoomRatio = state.maxZoomRatio.coerceAtLeast(state.minZoomRatio)
                    zoomRatio = state.zoomRatio.coerceIn(minZoomRatio, maxZoomRatio)
                }
                cameraReady = true
            }.onFailure {
                if (!disposed.get()) {
                    AppLogger.e("quickCard scanner bind failed", it)
                    toast(context, "无法打开相机")
                    onOpenFailed()
                }
            }
        }
        providerFuture.addListener(listener, mainExecutor)
        onDispose {
            disposed.set(true)
            boundCamera = null
            cameraReady = false
            runCatching {
                if (providerFuture.isDone) {
                    providerFuture.get().unbindAll()
                }
            }
            analyzing.set(false)
            runCatching { scanner.close() }
            analyzerExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        val finderSize = if (isLandscape) 214.dp else 260.dp
        val outerMaskColor = Color.Black.copy(alpha = 0.62f)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val sideMaskWidth = ((maxWidth - finderSize) / 2f).coerceAtLeast(0.dp)
            val verticalMaskHeight = ((maxHeight - finderSize) / 2f).coerceAtLeast(0.dp)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(verticalMaskHeight)
                    .background(outerMaskColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(verticalMaskHeight)
                    .background(outerMaskColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(sideMaskWidth)
                    .height(finderSize)
                    .background(outerMaskColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(sideMaskWidth)
                    .height(finderSize)
                    .background(outerMaskColor)
            )

            Text(
                text = if (cameraReady) "将二维码置于取景框内自动识别" else "正在打开相机...",
                color = Color.White,
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -(finderSize / 2f) - if (isLandscape) 18.dp else 24.dp)
                    .padding(horizontal = 16.dp)
            )

            Box(
                modifier = Modifier
                    .size(finderSize)
                    .align(Alignment.Center)
            ) {
                QrScannerFinderFrame(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = finderSize / 2f + if (isLandscape) 24.dp else 32.dp)
                    .padding(horizontal = if (isLandscape) 20.dp else 28.dp)
                    .fillMaxWidth(if (isLandscape) 0.62f else 0.78f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MsIcon("zoom_in", contentDescription = "放大", tint = Color.White)
                    Slider(
                        value = zoomRatio,
                        onValueChange = { target ->
                            val camera = boundCamera ?: return@Slider
                            val resolved = target.coerceIn(minZoomRatio, maxZoomRatio.coerceAtLeast(minZoomRatio))
                            camera.cameraControl.setZoomRatio(resolved)
                            zoomRatio = resolved
                        },
                        valueRange = minZoomRatio..maxZoomRatio.coerceAtLeast(minZoomRatio),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colors.primary,
                            activeTrackColor = MaterialTheme.colors.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                CompositionLocalProvider(
                    LocalContentColor provides if (cameraReady && flashAvailable) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.42f)
                    }
                ) {
                    Md2IconButton(
                        icon = if (torchEnabled) "flash_on" else "flash_off",
                        contentDescription = "手电筒",
                        onClick = {
                            val camera = boundCamera ?: return@Md2IconButton
                            val enabled = !torchEnabled
                            camera.cameraControl.enableTorch(enabled)
                            torchEnabled = enabled
                        },
                        enabled = cameraReady && flashAvailable
                    )
                }
            }
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
        val thick = size.minDimension * 0.018f
        val thin = thick * 0.42f
        val halfThick = thick / 2f
        val left = halfThick
        val right = (w - halfThick).coerceAtLeast(left)
        val top = halfThick
        val bottom = (h - halfThick).coerceAtLeast(top)
        val innerW = (right - left).coerceAtLeast(0f)
        val innerH = (bottom - top).coerceAtLeast(0f)
        val cornerW = innerW * cornerRatio
        val cornerH = innerH * cornerRatio

        fun hSeg(x1: Float, x2: Float, y: Float, stroke: Float) {
            drawLine(
                color = color,
                start = Offset(x1, y),
                end = Offset(x2, y),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
        }

        fun vSeg(x: Float, y1: Float, y2: Float, stroke: Float) {
            drawLine(
                color = color,
                start = Offset(x, y1),
                end = Offset(x, y2),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
        }

        hSeg(left, left + cornerW, top, thick)
        hSeg(left + cornerW, right - cornerW, top, thin)
        hSeg(right - cornerW, right, top, thick)

        hSeg(left, left + cornerW, bottom, thick)
        hSeg(left + cornerW, right - cornerW, bottom, thin)
        hSeg(right - cornerW, right, bottom, thick)

        vSeg(left, top, top + cornerH, thick)
        vSeg(left, top + cornerH, bottom - cornerH, thin)
        vSeg(left, bottom - cornerH, bottom, thick)

        vSeg(right, top, top + cornerH, thick)
        vSeg(right, top + cornerH, bottom - cornerH, thin)
        vSeg(right, bottom - cornerH, bottom, thick)
    }
}

@Composable
private fun QuickCardScanCandidatesScreen(
    items: List<String>,
    onTopBarActionsChange: (QuickCardTopBarActions?) -> Unit,
    onSelect: (String) -> Unit
) {
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        onTopBarActionsChange(null)
    }

    CenteredPageColumn(
        maxWidth = UiTokens.WideContentMaxWidth,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        scroll = scroll,
        horizontalPadding = 20.dp,
        contentSpacing = 12.dp
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "检测到多个二维码",
            style = MaterialTheme.typography.h6
        )
        Text(
            text = "请选择要打开的二维码内容",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f)
        )
        if (items.isEmpty()) {
            Card(
                shape = RoundedCornerShape(UiTokens.Radius),
                backgroundColor = MaterialTheme.colors.surface,
                elevation = UiTokens.CardElevation
            ) {
                Text(
                    text = "没有可用候选项",
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.body1
                )
            }
        } else {
            items.forEachIndexed { index, item ->
                Card(
                    shape = RoundedCornerShape(UiTokens.Radius),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = UiTokens.CardElevation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "候选 ${index + 1}",
                            style = MaterialTheme.typography.overline,
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.body1,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
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
    val onTopBarActionsChangeState = rememberUpdatedState(onTopBarActionsChange)
    val publishWebActions: () -> Unit = remember {
        {
            val webView = webViewRef.value
            val canBack = webView?.canGoBack() == true
            val canForward = webView?.canGoForward() == true
            onTopBarActionsChangeState.value(
                QuickCardTopBarActions(
                    onWebReload = { webViewRef.value?.reload() },
                    onWebBack = if (canBack) ({ webViewRef.value?.goBack() }) else null,
                    onWebForward = if (canForward) ({ webViewRef.value?.goForward() }) else null,
                    canWebReload = webView != null,
                    canWebBack = canBack,
                    canWebForward = canForward
                )
            )
        }
    }
    LaunchedEffect(Unit) {
        publishWebActions()
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
            onTopBarActionsChangeState.value(null)
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
                            publishWebActions()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            publishWebActions()
                        }
                    }
                    loadUrl(url)
                    publishWebActions()
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
    imageProxy: ImageProxy
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
    return decodeQrWithZxing(source)
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
            KigttsFontScaleProvider {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(quickCardDisplayAspect(landscape))
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

        QuickCardUnifiedContent(card = card, landscape = landscape, onEdit = onEdit, onShare = onShare)
    }
}

@Composable
private fun QuickCardBrandLogo(
    modifier: Modifier = Modifier,
    light: Boolean? = null
) {
    val logoRes = if (light ?: currentAppDarkTheme()) R.drawable.logo_white else R.drawable.logo_black
    Image(
        painter = androidx.compose.ui.res.painterResource(id = logoRes),
        contentDescription = "KIGTTS",
        modifier = modifier.height(18.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun QuickCardUnifiedContent(
    card: QuickCard,
    landscape: Boolean,
    onEdit: (QuickCard) -> Unit,
    onShare: (QuickCard) -> Unit
) {
    val context = LocalContext.current
    val theme = quickCardThemeColor(card.themeColor)
    val onTheme = quickCardThemeOnColor(theme)
    val linkText = card.link.trim()
    val imageBitmap = rememberQuickCardBitmap(card.heroImagePath(landscape))
    val qrBitmap = rememberQuickCardQrBitmap(linkText)
    val hasLink = linkText.isNotEmpty()
    val hasImage = imageBitmap != null
    val foreground = if (hasImage) Color.White else onTheme
    val titleText = card.title.trim()
    val noteText = card.note.trim()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(quickCardDisplayAspect(landscape))
            .clip(RoundedCornerShape(UiTokens.Radius))
            .background(theme)
    ) {
        if (hasImage) {
            Image(
                bitmap = imageBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.42f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(86.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.46f))
                        )
                    )
            )
        } else {
            QuickCardDecorativeBackgroundText(
                text = titleText,
                color = foreground.copy(alpha = 0.22f),
                landscape = landscape,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 14.dp, end = 72.dp)
        ) {
            if (titleText.isNotEmpty()) {
                Text(
                    text = titleText,
                    color = foreground,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (noteText.isNotEmpty()) {
                Text(
                    text = noteText,
                    color = foreground.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        QuickCardOverlayIconButton(
            icon = "edit",
            contentDescription = "编辑名片",
            tint = foreground,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
        ) { onEdit(card) }

        if (hasLink) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(if (landscape) 0.28f else 0.46f)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(UiTokens.Radius),
                backgroundColor = Color.White,
                elevation = UiTokens.CardElevation
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "二维码",
                            modifier = Modifier.fillMaxSize(0.86f)
                        )
                    }
                }
            }
        }

        if (hasLink) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(if (landscape) 0.56f else 0.74f)
                    .padding(start = 14.dp, bottom = 10.dp, end = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickCardOverlayIconButton(
                        icon = "open_in_new",
                        contentDescription = "打开链接",
                        tint = foreground
                    ) { openQuickCardLink(context, linkText) }
                    QuickCardOverlayIconButton(
                        icon = "share",
                        contentDescription = "分享链接",
                        tint = foreground
                    ) { onShare(card) }
                }
                Text(
                    text = linkText,
                    color = foreground,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        QuickCardBrandLogo(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp),
            light = hasImage || foreground == Color.White
        )
    }
}

@Composable
private fun QuickCardDecorativeBackgroundText(
    text: String,
    color: Color,
    landscape: Boolean,
    modifier: Modifier = Modifier
) {
    if (text.isBlank()) return
    var textWidthPx by remember(text) { mutableIntStateOf(0) }
    Box(modifier = modifier) {
        if (landscape) {
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer(clip = false)
                    .width(520.dp)
                    .padding(start = 12.dp, bottom = 6.dp)
            )
        } else {
            val topPad = 10.dp
            val topPadPx = with(LocalDensity.current) { topPad.toPx() }
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 88.sp,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                onTextLayout = { textWidthPx = it.size.width },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp)
                    .graphicsLayer(
                        rotationZ = 90f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        translationY = textWidthPx.toFloat() + topPadPx,
                        clip = false
                    )
            )
        }
    }
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
                    if (card.title.isNotBlank()) {
                        Text(
                            card.title.trim(),
                            style = MaterialTheme.typography.h6,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (card.note.isNotBlank()) {
                        Text(
                            card.note.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
    val density = LocalDensity.current
    val designHeight = 180.dp
    val heroWidth = designHeight * QUICK_CARD_ASPECT_LANDSCAPE
    val detailsWidth = 132.dp
    val groupWidth = heroWidth + 10.dp + detailsWidth + 8.dp + 4.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val scale = minOf(
            with(density) { maxWidth.toPx() / groupWidth.toPx() },
            with(density) { maxHeight.toPx() / designHeight.toPx() }
        )
        Box(
            modifier = Modifier.size(groupWidth * scale, designHeight * scale),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .size(groupWidth, designHeight)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        transformOrigin = TransformOrigin.Center
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(width = heroWidth, height = designHeight)
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
                        .width(detailsWidth)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                ) {
                    if (card.title.isNotBlank()) {
                        Text(
                            card.title.trim(),
                            style = MaterialTheme.typography.h6,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (card.note.isNotBlank()) {
                        Text(
                            card.note.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                val watermark = card.title.trim()
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
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 16.dp)
                ) {
                    if (card.title.isNotBlank()) {
                        Text(
                            text = card.title.trim(),
                            color = onTheme,
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
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
    var cropLandscape by rememberSaveable { mutableStateOf(false) }
    var activeCropLandscape by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var showBuiltinGalleryPicker by remember { mutableStateOf(false) }
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
                if (!viewModel.setQuickCardDraftImage(uri, landscape = activeCropLandscape)) {
                    toast(context, "设置图片失败")
                }
            } else {
                toast(context, "裁剪失败：无输出")
            }
        } else {
            toast(context, "裁剪失败")
        }
    }
    fun launchQuickCardCrop(uri: Uri) {
        val targetLandscape = cropLandscape
        activeCropLandscape = targetLandscape
        val aspectX = if (targetLandscape) 16 else 9
        val aspectY = if (targetLandscape) 9 else 16
        val outputWidth = if (targetLandscape) 1920 else 1080
        val outputHeight = if (targetLandscape) 1080 else 1920
        val options = CropImageOptions(
            fixAspectRatio = true,
            aspectRatioX = aspectX,
            aspectRatioY = aspectY,
            activityTitle = if (targetLandscape) {
                "裁剪横屏名片图片（16:9）"
            } else {
                "裁剪竖屏名片图片（9:16）"
            },
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
            outputRequestWidth = outputWidth,
            outputRequestHeight = outputHeight,
            outputRequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_EXACT
        )
        cropLauncher.launch(CropImageContractOptions(uri, options))
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        launchQuickCardCrop(uri)
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
    val requestExitEditorState = rememberUpdatedState { requestExitEditor() }

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
    val editorActions = remember(isExisting, context, viewModel) {
        if (!isExisting) {
            QuickCardTopBarActions(
                onNew = {},
                onScan = {},
                onBackRequest = { requestExitEditorState.value() }
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
                onBackRequest = { requestExitEditorState.value() },
                canCopy = true,
                canDelete = true
            )
        }
    }

    LaunchedEffect(editorActions) {
        onTopBarActionsChange(editorActions)
    }

    CenteredPageColumn(
        maxWidth = UiTokens.WideContentMaxWidth,
        scroll = rememberScrollState()
    ) {
            Spacer(Modifier.height(UiTokens.PageTopBlank))

        Md2SettingsCard("基础信息") {
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

        Md2SettingsCard("背景图片") {
            QuickCardImagePathRow(
                title = "竖屏背景图",
                path = draft.portraitImagePath,
                onClear = { viewModel.clearQuickCardDraftImage(landscape = false) },
                onPick = {
                    cropLandscape = false
                    if (uiState.useBuiltinGallery) {
                        showBuiltinGalleryPicker = true
                    } else {
                        imagePicker.launch("image/*")
                    }
                }
            )
            QuickCardImagePathRow(
                title = "横屏背景图",
                path = draft.landscapeImagePath,
                onClear = { viewModel.clearQuickCardDraftImage(landscape = true) },
                onPick = {
                    cropLandscape = true
                    if (uiState.useBuiltinGallery) {
                        showBuiltinGalleryPicker = true
                    } else {
                        imagePicker.launch("image/*")
                    }
                }
            )
            Text(
                text = "未设置图片时使用主题色背景，并显示装饰文字。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

    if (showBuiltinGalleryPicker) {
        BuiltinGalleryPickerDialog(
            title = "选择图片",
            onDismiss = { showBuiltinGalleryPicker = false },
            onPicked = { uri ->
                showBuiltinGalleryPicker = false
                launchQuickCardCrop(uri)
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
private fun currentAppDarkTheme(): Boolean = !MaterialTheme.colors.isLight

@Composable
private fun md2CardContainerColor(): Color {
    return if (currentAppDarkTheme()) UiTokens.DarkCard else UiTokens.LightCard
}

@Composable
private fun md2ElevatedCardContainerColor(elevation: Dp = UiTokens.CardElevation): Color {
    val base = md2CardContainerColor()
    return LocalElevationOverlay.current?.apply(base, elevation) ?: base
}

private val MaterialSymbolsSharp = FontFamily(
    Font(
        resId = R.font.material_symbols_sharp,
        weight = FontWeight.W500
    )
)

internal object FontScaleBlockRuntime {
    var mode by mutableIntStateOf(UserPrefs.FONT_SCALE_BLOCK_ICONS_ONLY)
}

internal val LocalFontScaleBlockMode = staticCompositionLocalOf {
    UserPrefs.FONT_SCALE_BLOCK_ICONS_ONLY
}

private val LocalKigttsHapticFeedbackEnabled = staticCompositionLocalOf { false }

@Composable
internal fun KigttsFontScaleProvider(
    mode: Int = FontScaleBlockRuntime.mode,
    content: @Composable () -> Unit
) {
    val normalized = UserPrefs.normalizeFontScaleBlockMode(mode)
    val density = LocalDensity.current
    val providedDensity = if (normalized == UserPrefs.FONT_SCALE_BLOCK_ALL) {
        Density(density = density.density, fontScale = 1f)
    } else {
        density
    }
    CompositionLocalProvider(
        LocalFontScaleBlockMode provides normalized,
        LocalDensity provides providedDensity,
        content = content
    )
}

private fun View.performKigttsKeyHaptic(enabled: Boolean) {
    if (!enabled) return
    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}

@Composable
private fun rememberKigttsKeyHaptic(): () -> Unit {
    val enabled = LocalKigttsHapticFeedbackEnabled.current
    val view = LocalView.current
    return remember(view, enabled) {
        { view.performKigttsKeyHaptic(enabled) }
    }
}

@Composable
private fun rememberKigttsHapticClick(onClick: () -> Unit): () -> Unit {
    val performHaptic = rememberKigttsKeyHaptic()
    val currentOnClick by rememberUpdatedState(onClick)
    return remember(performHaptic) {
        {
            performHaptic()
            currentOnClick()
        }
    }
}

@Composable
private fun <T> rememberKigttsHapticValueChange(onValueChange: (T) -> Unit): (T) -> Unit {
    val performHaptic = rememberKigttsKeyHaptic()
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    return remember(performHaptic) {
        { value ->
            performHaptic()
            currentOnValueChange(value)
        }
    }
}

@Composable
private fun MsIcon(
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val fontScaleBlockMode = LocalFontScaleBlockMode.current
    val iconTextSize = if (fontScaleBlockMode == UserPrefs.FONT_SCALE_BLOCK_NONE) {
        24.sp
    } else {
        with(LocalDensity.current) { 24.dp.toSp() }
    }
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
            fontSize = iconTextSize,
            lineHeight = iconTextSize,
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
    val effectiveEnabled = enabled && !LocalSuppressStaggeredFloatIn.current
    var visible by remember(index, effectiveEnabled) { mutableStateOf(!effectiveEnabled) }

    LaunchedEffect(index, effectiveEnabled) {
        if (!effectiveEnabled) {
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
    val onWebReload: (() -> Unit)? = null,
    val onWebBack: (() -> Unit)? = null,
    val onWebForward: (() -> Unit)? = null,
    val onBackRequest: (() -> Unit)? = null,
    val canCopy: Boolean = false,
    val canDelete: Boolean = false,
    val canConfirm: Boolean = false,
    val canWebReload: Boolean = false,
    val canWebBack: Boolean = false,
    val canWebForward: Boolean = false
)

data class PresetTopBarActions(
    val onImport: () -> Unit,
    val onExport: () -> Unit
)

private object QuickSubtitleRoutes {
    const val Main = "quick_subtitle/main"
    const val Editor = "quick_subtitle/editor"
    const val History = "quick_subtitle/history"
}

data class QuickSubtitleFloatingInputPreviewState(
    val text: AnnotatedString,
    val cursorIndex: Int,
    val bottomPadding: Dp
)

private object SoundboardRoutes {
    const val Main = "soundboard/main"
    const val Editor = "soundboard/editor"
}

private object QuickCardRoutes {
    const val Main = "quick_card/main"
    const val Editor = "quick_card/editor"
    const val Sort = "quick_card/sort"
    const val Scanner = "quick_card/scanner"
    private const val ScanCandidatesArg = "items"
    const val ScanCandidates = "quick_card/scan_candidates/{$ScanCandidatesArg}"
    private const val ScanTextArg = "text"
    const val ScanText = "quick_card/scan_text/{$ScanTextArg}"
    private const val WebArg = "url"
    const val Web = "quick_card/web/{$WebArg}"

    fun scanCandidates(items: List<String>): String =
        "quick_card/scan_candidates/${Uri.encode(JSONArray(items).toString())}"
    fun scanText(text: String): String = "quick_card/scan_text/${Uri.encode(text)}"
    fun web(url: String): String = "quick_card/web/${Uri.encode(url)}"
}

private object SettingsRoutes {
    const val Main = "settings/main"
    const val Log = "settings/log"
    const val Licenses = "settings/licenses"
    const val Privacy = "settings/privacy"
}

private val SoundboardAudioFileExtensions = setOf(
    "m4a",
    "mp4",
    "aac",
    "mp3",
    "wav",
    "wave",
    "flac",
    "ogg",
    "oga",
    "opus",
    "amr",
    "awb",
    "3gp",
    "3gpp",
    "webm"
)

private const val TTS_DISABLED_MESSAGE = "TTS已禁用，如需打开，请打开顶部音频状态菜单将“禁用TTS”选项关闭"
private var quickCardSortHintShownThisProcess = false

class MainActivity : ComponentActivity() {
    private var lastDecorFitsSystemWindows: Boolean = false
    private var pendingBackgroundReturnFix: Boolean = false
    private var delayedResumeFixRunnable: Runnable? = null
    private var lastHandledExternalVoicePackIntentKey: String? = null
    private var lastHandledExternalPresetIntentKey: String? = null
    private var realtimeHostBound = false
    private val realtimeHostConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? RealtimeHostService.LocalBinder ?: return
            realtimeHostBound = true
            viewModel.attachRealtimeHost(binder.getService())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            realtimeHostBound = false
            viewModel.detachRealtimeHost()
        }
    }

    private val viewModel: MainViewModel by viewModels {
        val repo = ModelRepository(this@MainActivity)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repo, this@MainActivity) as T
            }
        }
    }

    override fun getResources(): Resources {
        val base = super.getResources()
        if (FontScaleBlockRuntime.mode != UserPrefs.FONT_SCALE_BLOCK_ALL) return base
        val config = Configuration(base.configuration)
        if (config.fontScale == 1f) return base
        config.fontScale = 1f
        return createConfigurationContext(config).resources
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
        RealtimeHostService.ensureStarted(this)
        bindService(
            Intent(this, RealtimeHostService::class.java),
            realtimeHostConnection,
            Context.BIND_AUTO_CREATE
        )
        viewModel.loadSettings()
        viewModel.ensureInitialFloatingOverlayShortcuts()
        lifecycleScope.launch(Dispatchers.Default) {
            LauncherMenuShortcuts.syncAppShortcuts(applicationContext)
        }
        setContent {
            val state = viewModel.uiState
            KigttsFontScaleProvider(state.fontScaleBlockMode) {
                val systemDark = isSystemInDarkTheme()
                val dark = UserPrefs.resolveThemeMode(state.themeMode, systemDark)
                val colors = if (dark) KgtDarkColors else KgtLightColors
                val extraColors = if (dark) KgtDarkExtraColors else KgtLightExtraColors
                val textToolbarState = remember { KigttsTextToolbarState() }
                val textToolbar = remember(textToolbarState) { KigttsTextToolbar(textToolbarState) }
                CompositionLocalProvider(
                    LocalMd2ExtraColors provides extraColors,
                    LocalKigttsHapticFeedbackEnabled provides state.hapticFeedbackEnabled
                ) {
                    CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
                        MaterialTheme(colors = colors, typography = KgtTypography, shapes = Md2Shapes) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    AppScaffold(viewModel)
                                    KigttsTextToolbarPopup(
                                        state = textToolbarState,
                                        darkTheme = dark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        handleLaunchIntent(intent)
    }

    override fun onDestroy() {
        if (realtimeHostBound) {
            unbindService(realtimeHostConnection)
            realtimeHostBound = false
        }
        viewModel.detachRealtimeHost()
        super.onDestroy()
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
        when (intent?.action) {
            OverlayBridge.ACTION_OPEN_QUICK_SUBTITLE -> {
                val requestId = intent.getLongExtra(OverlayBridge.EXTRA_REQUEST_ID, Long.MIN_VALUE)
                if (requestId == Long.MIN_VALUE) return
                val target = intent.getStringExtra(OverlayBridge.EXTRA_TARGET) ?: OverlayBridge.TARGET_SUBTITLE
                val text = intent.getStringExtra(OverlayBridge.EXTRA_TEXT).orEmpty()
                val navigateToPage = intent.getBooleanExtra(OverlayBridge.EXTRA_NAVIGATE_TO_PAGE, true)
                viewModel.handleQuickSubtitleLaunchRequest(requestId, target, text, navigateToPage)
            }

            OverlayBridge.ACTION_REQUEST_RECORD_AUDIO_PERMISSION -> {
                val startRealtimeOnGrant =
                    intent.getBooleanExtra(OverlayBridge.EXTRA_START_REALTIME_ON_GRANT, false)
                viewModel.requestRecordAudioPermission(startRealtimeOnGrant)
            }

            OverlayBridge.ACTION_SHOW_ACCESSIBILITY_GUIDE -> {
                viewModel.requestAccessibilityExplainDialog()
                setIntent(Intent())
            }

            Intent.ACTION_VIEW,
            Intent.ACTION_SEND -> {
                val uri = when (intent.action) {
                    Intent.ACTION_VIEW -> intent.data
                    Intent.ACTION_SEND -> @Suppress("DEPRECATION") (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)
                    else -> null
                } ?: return
                val presetTarget = presetInstallTargetForIntent(intent, uri)
                if (presetTarget != null) {
                    val key = "${intent.action}|$uri|${resolveExternalFileName(uri).orEmpty()}|$presetTarget"
                    if (lastHandledExternalPresetIntentKey == key) return
                    lastHandledExternalPresetIntentKey = key
                    when (presetTarget) {
                        PresetInstallTarget.QuickSubtitle ->
                            viewModel.importQuickSubtitlePresetPackage(uri, openEditorOnSuccess = true)
                        PresetInstallTarget.Soundboard ->
                            viewModel.importSoundboardPresetPackage(uri, openEditorOnSuccess = true)
                    }
                    setIntent(Intent())
                    return
                }
                if (!shouldHandleVoicePackIntent(intent, uri)) return
                val key = "${intent.action}|$uri|${resolveExternalFileName(uri).orEmpty()}"
                if (lastHandledExternalVoicePackIntentKey == key) return
                lastHandledExternalVoicePackIntentKey = key
                viewModel.importVoice(uri, openVoicePackPageOnSuccess = true)
                setIntent(Intent())
            }
        }
    }

    private fun presetInstallTargetForIntent(intent: Intent, uri: Uri): PresetInstallTarget? {
        val name = resolveExternalFileName(uri)?.lowercase().orEmpty()
        val mime = intent.type?.lowercase().orEmpty()
        return when {
            name.endsWith(".kigtpk") || mime == "application/x-kigtts-quicktext-preset" ->
                PresetInstallTarget.QuickSubtitle
            name.endsWith(".kigspk") || mime == "application/x-kigtts-soundboard-preset" ->
                PresetInstallTarget.Soundboard
            else -> null
        }
    }

    private fun shouldHandleVoicePackIntent(intent: Intent, uri: Uri): Boolean {
        val name = resolveExternalFileName(uri)?.lowercase().orEmpty()
        val mime = intent.type?.lowercase().orEmpty()
        return when (intent.action) {
            Intent.ACTION_VIEW ->
                name.endsWith(".kigvpk") || mime == "application/x-kigtts-voicepack"
            Intent.ACTION_SEND ->
                name.endsWith(".kigvpk") || name.endsWith(".zip") || mime == "application/x-kigtts-voicepack"
            else -> false
        }
    }

    private fun resolveExternalFileName(uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            return cursor.getString(index)
                        }
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
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

private class KigttsTextToolbarState {
    var rect by mutableStateOf(Rect.Zero)
    var visible by mutableStateOf(false)
    var onCopyRequested by mutableStateOf<(() -> Unit)?>(null)
    var onPasteRequested by mutableStateOf<(() -> Unit)?>(null)
    var onCutRequested by mutableStateOf<(() -> Unit)?>(null)
    var onSelectAllRequested by mutableStateOf<(() -> Unit)?>(null)

    fun show(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        this.rect = rect
        this.onCopyRequested = onCopyRequested
        this.onPasteRequested = onPasteRequested
        this.onCutRequested = onCutRequested
        this.onSelectAllRequested = onSelectAllRequested
        visible = true
    }

    fun hide() {
        visible = false
    }
}

private class KigttsTextToolbar(
    private val state: KigttsTextToolbarState
) : TextToolbar {
    override val status: TextToolbarStatus
        get() = if (state.visible) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        state.show(
            rect = rect,
            onCopyRequested = onCopyRequested,
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested,
            onSelectAllRequested = onSelectAllRequested
        )
    }

    override fun hide() {
        state.hide()
    }
}

private data class KigttsTextToolbarAction(
    val icon: String,
    val contentDescription: String,
    val onClick: (() -> Unit)?
)

private class KigttsTextToolbarPositionProvider(
    private val anchorRect: IntRect,
    private val marginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val centeredX = anchorRect.left + ((anchorRect.width - popupContentSize.width) / 2)
        val clampedX = centeredX.coerceIn(
            marginPx,
            (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        )
        val aboveY = anchorRect.top - popupContentSize.height - marginPx
        val belowY = anchorRect.bottom + marginPx
        val targetY = if (aboveY >= marginPx) {
            aboveY
        } else {
            belowY.coerceAtMost(
                (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
            )
        }
        return IntOffset(clampedX, targetY)
    }
}

@Composable
private fun KigttsTextToolbarPopup(
    state: KigttsTextToolbarState,
    darkTheme: Boolean
) {
    var rendered by remember { mutableStateOf(state.visible) }
    val menuAlpha by animateFloatAsState(
        targetValue = if (state.visible) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "kigtts_text_toolbar_alpha"
    )
    val menuScale by animateFloatAsState(
        targetValue = if (state.visible) 1f else 0.94f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "kigtts_text_toolbar_scale"
    )
    LaunchedEffect(state.visible) {
        if (state.visible) {
            rendered = true
        } else if (rendered) {
            delay(180L)
            rendered = false
        }
    }
    if (!rendered) return
    val density = LocalDensity.current
    val marginPx = with(density) { 8.dp.roundToPx() }
    val anchorRect = remember(state.rect) {
        IntRect(
            left = state.rect.left.toInt(),
            top = state.rect.top.toInt(),
            right = state.rect.right.toInt(),
            bottom = state.rect.bottom.toInt()
        )
    }
    val actions = remember(
        state.onCopyRequested,
        state.onPasteRequested,
        state.onCutRequested,
        state.onSelectAllRequested
    ) {
        listOf(
            KigttsTextToolbarAction("select_all", "全选", state.onSelectAllRequested),
            KigttsTextToolbarAction("content_cut", "剪切", state.onCutRequested),
            KigttsTextToolbarAction("content_copy", "复制", state.onCopyRequested),
            KigttsTextToolbarAction("content_paste", "粘贴", state.onPasteRequested)
        ).filter { it.onClick != null }
    }
    if (actions.isEmpty()) return

    val backgroundColor = if (darkTheme) Color(0xFF2C2F33) else Color.White
    val contentColor = if (darkTheme) Color(0xFFE9EDF1) else Color(0xFF202428)
    val positionProvider = remember(anchorRect, marginPx) {
        KigttsTextToolbarPositionProvider(anchorRect, marginPx)
    }

    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
    ) {
        KigttsFontScaleProvider {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .graphicsLayer {
                        alpha = menuAlpha
                        scaleX = menuScale
                        scaleY = menuScale
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        clip = false
                    }
            ) {
                Card(
                    modifier = Modifier.wrapContentSize(),
                    shape = RoundedCornerShape(UiTokens.Radius),
                    backgroundColor = backgroundColor,
                    elevation = UiTokens.MenuElevation
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions.forEach { action ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = rememberRipple(bounded = true, radius = 20.dp)
                                    ) {
                                        action.onClick?.invoke()
                                        state.hide()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                MsIcon(
                                    name = action.icon,
                                    contentDescription = action.contentDescription,
                                    tint = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
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
    val hapticOnClick = rememberKigttsHapticClick(onClick)
    Button(
        onClick = hapticOnClick,
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
    val hapticOnClick = rememberKigttsHapticClick(onClick)
    OutlinedButton(
        onClick = hapticOnClick,
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
    val hapticOnClick = rememberKigttsHapticClick(onClick)
    TextButton(
        onClick = hapticOnClick,
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
    val hapticOnClick = rememberKigttsHapticClick(onClick)
    IconButton(
        onClick = hapticOnClick,
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
private fun RecognitionResourceSourceDialog(
    modelScopeUrl: String,
    huggingFaceUrl: String,
    preferredSource: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var modelScope by remember(modelScopeUrl) { mutableStateOf(modelScopeUrl) }
    var huggingFace by remember(huggingFaceUrl) { mutableStateOf(huggingFaceUrl) }
    var preferred by remember(preferredSource) {
        mutableIntStateOf(
            preferredSource.coerceIn(
                UserPrefs.RECOGNITION_RESOURCE_SOURCE_MODELSCOPE,
                UserPrefs.RECOGNITION_RESOURCE_SOURCE_HUGGINGFACE
            )
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(UiTokens.Radius),
        title = { Text("语音识别资源包下载源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "下载的资源包会安装到软件内部目录；安装完成后会自动清理下载得到的 7z 包。本地安装不会删除原文件。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Md2OutlinedField(
                    value = modelScope,
                    onValueChange = { modelScope = it },
                    label = "魔搭下载源",
                    modifier = Modifier.fillMaxWidth()
                )
                Md2OutlinedField(
                    value = huggingFace,
                    onValueChange = { huggingFace = it },
                    label = "Hugging Face 下载源",
                    modifier = Modifier.fillMaxWidth()
                )
                Text("优先下载源", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(UiTokens.Radius))
                        .clickable { preferred = UserPrefs.RECOGNITION_RESOURCE_SOURCE_MODELSCOPE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferred == UserPrefs.RECOGNITION_RESOURCE_SOURCE_MODELSCOPE,
                        onClick = { preferred = UserPrefs.RECOGNITION_RESOURCE_SOURCE_MODELSCOPE }
                    )
                    Text("魔搭")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(UiTokens.Radius))
                        .clickable { preferred = UserPrefs.RECOGNITION_RESOURCE_SOURCE_HUGGINGFACE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferred == UserPrefs.RECOGNITION_RESOURCE_SOURCE_HUGGINGFACE,
                        onClick = { preferred = UserPrefs.RECOGNITION_RESOURCE_SOURCE_HUGGINGFACE }
                    )
                    Text("Hugging Face")
                }
            }
        },
        confirmButton = {
            Md2TextButton(
                onClick = { onConfirm(modelScope, huggingFace, preferred) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Md2TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun KokoroSourceDialog(
    hfUrl: String,
    hfMirrorUrl: String,
    modelScopeUrl: String,
    preferredSource: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int) -> Unit
) {
    var hf by remember(hfUrl) { mutableStateOf(hfUrl) }
    var hfMirror by remember(hfMirrorUrl) { mutableStateOf(hfMirrorUrl) }
    var modelScope by remember(modelScopeUrl) { mutableStateOf(modelScopeUrl) }
    var preferred by remember(preferredSource) {
        mutableIntStateOf(
            preferredSource.coerceIn(
                UserPrefs.KOKORO_SOURCE_HF,
                UserPrefs.KOKORO_SOURCE_MODELSCOPE
            )
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(UiTokens.Radius),
        title = { Text("Kokoro 下载源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "如果默认下载速度慢或下载失败，可以在这里切换下载来源。一般建议保持默认源；如果无法下载，再尝试另一个来源。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "下载完成后会自动安装到软件内部目录。安装成功后，Kokoro 会出现在“语音包”页面。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Md2OutlinedField(
                    value = modelScope,
                    onValueChange = { modelScope = it },
                    label = "ModelScope 下载源",
                    modifier = Modifier.fillMaxWidth()
                )
                Md2OutlinedField(
                    value = hfMirror,
                    onValueChange = { hfMirror = it },
                    label = "HF-Mirror 下载源",
                    modifier = Modifier.fillMaxWidth()
                )
                Md2OutlinedField(
                    value = hf,
                    onValueChange = { hf = it },
                    label = "Hugging Face 下载源",
                    modifier = Modifier.fillMaxWidth()
                )
                Text("优先下载源", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(UiTokens.Radius))
                        .clickable { preferred = UserPrefs.KOKORO_SOURCE_MODELSCOPE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferred == UserPrefs.KOKORO_SOURCE_MODELSCOPE,
                        onClick = { preferred = UserPrefs.KOKORO_SOURCE_MODELSCOPE }
                    )
                    Text("ModelScope（默认）")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(UiTokens.Radius))
                        .clickable { preferred = UserPrefs.KOKORO_SOURCE_HFMIRROR }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferred == UserPrefs.KOKORO_SOURCE_HFMIRROR,
                        onClick = { preferred = UserPrefs.KOKORO_SOURCE_HFMIRROR }
                    )
                    Text("HF-Mirror")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(UiTokens.Radius))
                        .clickable { preferred = UserPrefs.KOKORO_SOURCE_HF }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferred == UserPrefs.KOKORO_SOURCE_HF,
                        onClick = { preferred = UserPrefs.KOKORO_SOURCE_HF }
                    )
                    Text("Hugging Face")
                }
            }
        },
        confirmButton = {
            Md2TextButton(
                onClick = { onConfirm(hf, hfMirror, modelScope, preferred) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Md2TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun KokoroVoiceSettingsDialog(
    state: UiState,
    onDismiss: () -> Unit,
    onSpeakerChange: (Int) -> Unit
) {
    val onSpeakerChangeState = rememberUpdatedState(onSpeakerChange)
    val speakerId = state.kokoroSpeakerId.coerceIn(
        UserPrefs.KOKORO_MIN_SPEAKER_ID,
        UserPrefs.KOKORO_MAX_SPEAKER_ID
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(UiTokens.Radius),
        title = { Text("Kokoro 设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "关于 Kokoro",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Kokoro 是一套本地离线朗读声音。安装后不需要联网即可使用，适合想要使用软件自带朗读声音、不依赖系统朗读声音的场景。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "选择声音",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "编号不是音质等级，只代表不同声音风格。可用范围：0-1 美式女声，2 英式女声，3-57 中文女声，58-102 中文男声。你可以切换编号后试听，选择更适合自己的声音。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("声音编号：$speakerId", style = MaterialTheme.typography.bodyMedium)
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    factory = { ctx ->
                        NumberPicker(ctx).apply {
                            minValue = UserPrefs.KOKORO_MIN_SPEAKER_ID
                            maxValue = UserPrefs.KOKORO_MAX_SPEAKER_ID
                            wrapSelectorWheel = false
                            value = speakerId
                            setOnValueChangedListener { _, _, newVal ->
                                onSpeakerChangeState.value(newVal)
                            }
                        }
                    },
                    update = { picker ->
                        if (picker.minValue != UserPrefs.KOKORO_MIN_SPEAKER_ID) {
                            picker.minValue = UserPrefs.KOKORO_MIN_SPEAKER_ID
                        }
                        if (picker.maxValue != UserPrefs.KOKORO_MAX_SPEAKER_ID) {
                            picker.maxValue = UserPrefs.KOKORO_MAX_SPEAKER_ID
                        }
                        if (picker.value != speakerId) {
                            picker.value = speakerId
                        }
                    }
                )
            }
        },
        confirmButton = {
            Md2TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun RecognitionResourceRequiredDialog(
    state: UiState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onPickLocalPackage: () -> Unit,
    onOpenSources: () -> Unit
) {
    val busy = state.recognitionResourceBusy
    val installed = state.recognitionResourceInstalled
    AlertDialog(
        onDismissRequest = {
            if (!busy) onDismiss()
        },
        shape = RoundedCornerShape(UiTokens.Radius),
        title = { Text(if (installed) "语音识别资源包已安装" else "需要语音识别资源包") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (installed) {
                        "资源包安装完成，可以继续开启语音识别。"
                    } else {
                        "当前未安装语音识别资源包。语音识别、Silero VAD 和 AI 语音增强模型已经从 APK 解耦，需要先下载或从本地安装资源包。"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = state.recognitionResourceStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (busy) {
                    val progress = state.recognitionResourceProgress
                    if (progress in 0f..1f) {
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        text = state.recognitionResourceProgressStage.ifBlank { "处理中" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!installed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Md2Button(
                            onClick = onDownload,
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("下载并安装")
                        }
                        Md2OutlinedButton(
                            onClick = onPickLocalPackage,
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("本地安装")
                        }
                    }
                    Md2TextButton(
                        onClick = onOpenSources,
                        enabled = !busy
                    ) {
                        Text("管理下载源")
                    }
                }
            }
        },
        confirmButton = {
            Md2TextButton(
                onClick = onDismiss,
                enabled = !busy
            ) {
                Text(if (installed) "完成" else "稍后再说")
            }
        }
    )
}

@Composable
private fun Md2Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dark = currentAppDarkTheme()
    val hapticOnCheckedChange = rememberKigttsHapticValueChange(onCheckedChange)
    val uncheckedTrack = if (dark) Color(0xFF697378) else Color(0xFFB3C1C6)
    val uncheckedThumb = if (dark) Color(0xFFE6EFF2) else Color.White
    // Disabled state should look clearly gray, not transparent/faded-out.
    val disabledTrack = if (dark) Color(0xFF4D555B) else Color(0xFFD0D6DB)
    val disabledThumb = if (dark) Color(0xFF99A2A9) else Color(0xFF8E979E)
    Switch(
        checked = checked,
        onCheckedChange = hapticOnCheckedChange,
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
private fun rememberTopEndDropdownPopupPositionProvider(verticalMargin: Dp = 4.dp): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(density, verticalMargin) {
        object : PopupPositionProvider {
            private val verticalMarginPx = with(density) { verticalMargin.roundToPx() }

            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val preferredX = when (layoutDirection) {
                    LayoutDirection.Ltr -> anchorBounds.right - popupContentSize.width
                    LayoutDirection.Rtl -> anchorBounds.left
                }
                val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                val x = preferredX.coerceIn(0, maxX)
                val belowY = anchorBounds.bottom + verticalMarginPx
                val aboveY = anchorBounds.top - verticalMarginPx - popupContentSize.height
                val y = if (belowY + popupContentSize.height <= windowSize.height) {
                    belowY
                } else {
                    aboveY.coerceAtLeast(0)
                }
                return IntOffset(x, y)
            }
        }
    }
}

@Composable
private fun Md2AnimatedOptionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var rendered by remember { mutableStateOf(expanded) }
    val popupPositionProvider = rememberTopEndDropdownPopupPositionProvider()
    val menuAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "md2_option_menu_alpha"
    )
    val menuScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.94f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "md2_option_menu_scale"
    )
    LaunchedEffect(expanded) {
        if (expanded) {
            rendered = true
        } else if (rendered) {
            delay(180L)
            rendered = false
        }
    }
    if (!rendered) return
    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        KigttsFontScaleProvider {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .graphicsLayer {
                        alpha = menuAlpha
                        scaleX = menuScale
                        scaleY = menuScale
                        transformOrigin = TransformOrigin(1f, 0f)
                        clip = false
                    }
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(min = 196.dp, max = 216.dp)
                        .then(modifier),
                    shape = RoundedCornerShape(4.dp),
                    backgroundColor = md2CardContainerColor(),
                    elevation = UiTokens.MenuElevation
                ) {
                    Column(content = content)
                }
            }
        }
    }
}

@Composable
private fun Md2SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null
) {
    val contentAlpha = if (enabled) 1f else 0.56f
    val hapticToggle = rememberKigttsHapticClick { onCheckedChange(!checked) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true)
            ) { hapticToggle() }
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            Md2Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }
    }
}

@Composable
private fun Md2SettingDropdownRow(
    title: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.56f
    val hapticExpand = rememberKigttsHapticClick { onExpandedChange(true) }
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true)
                ) { hapticExpand() }
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MsIcon(
                    name = if (expanded) "expand_less" else "expand_more",
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
            }
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }
        }
        Md2AnimatedOptionMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            menuContent()
        }
    }
}

enum class SettingsCategory(val title: String, val icon: String) {
    Recognition("识别", "graphic_eq"),
    Audio("音频", "volume_up"),
    System("系统", "tune"),
    About("关于", "info")
}

@Composable
private fun SettingsTabsCard(
    selectedCategory: SettingsCategory,
    compact: Boolean,
    onSelect: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    if (compact) {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SettingsCategory.entries.forEach { category ->
                    SettingsTabButton(
                        category = category,
                        selected = selectedCategory == category,
                        compact = true,
                        onClick = { onSelect(category) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(dividerColor)
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SettingsCategory.entries.forEach { category ->
                SettingsTabButton(
                    category = category,
                    selected = selectedCategory == category,
                    compact = false,
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

@Composable
private fun SettingsTabButton(
    category: SettingsCategory,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorColor = MaterialTheme.colorScheme.primary
    val contentColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RectangleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ),
    ) {
        if (compact) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MsIcon(
                    name = category.icon,
                    contentDescription = category.title,
                    tint = contentColor
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.72f)
                        .height(2.dp)
                        .background(indicatorColor)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(start = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MsIcon(
                    name = category.icon,
                    contentDescription = category.title,
                    tint = contentColor
                )
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(indicatorColor)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun AppScaffold(viewModel: MainViewModel) {
    val pageQuickSubtitle = 0
    val pageOverlay = 1
    val pageQuickCard = 2
    val pageVoicePack = 3
    val pageDrawing = 4
    val pageSoundboard = 5
    val pageSettings = 6

    var page by rememberSaveable { mutableStateOf(pageQuickSubtitle) }
    var drawingFullscreen by rememberSaveable { mutableStateOf(false) }
    var quickSubtitleFullscreen by rememberSaveable { mutableStateOf(false) }
    var runningStripCollapsed by rememberSaveable { mutableStateOf(true) }
    var logTopBarActions by remember { mutableStateOf<LogTopBarActions?>(null) }
    var quickCardTopBarActions by remember { mutableStateOf<QuickCardTopBarActions?>(null) }
    var quickSubtitlePresetExportDialog by remember { mutableStateOf(false) }
    var soundboardPresetExportDialog by remember { mutableStateOf(false) }
    var showBuiltinQuickSubtitlePresetPicker by remember { mutableStateOf(false) }
    var showBuiltinSoundboardPresetPicker by remember { mutableStateOf(false) }
    var showBuiltinRecognitionResourcePicker by remember { mutableStateOf(false) }
    var showBuiltinKokoroVoicePicker by remember { mutableStateOf(false) }
    var recognitionResourceSourceDialog by remember { mutableStateOf(false) }
    var kokoroSourceDialog by remember { mutableStateOf(false) }
    var kokoroVoiceSettingsDialog by remember { mutableStateOf(false) }
    var recognitionResourceRequiredDialog by remember { mutableStateOf(false) }
    var startRealtimeAfterRecognitionResourceInstall by remember { mutableStateOf(false) }
    var quickCardWebMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var quickCardNavReady by remember { mutableStateOf(false) }
    var quickSubtitleFloatingInputPreview by remember {
        mutableStateOf<QuickSubtitleFloatingInputPreviewState?>(null)
    }
    var pendingQuickCardOverlayTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val quickSubtitleNavController = rememberNavController()
    val soundboardNavController = rememberNavController()
    val quickCardNavController = rememberNavController()
    val settingsNavController = rememberNavController()
    val quickSubtitleBackStackEntry by quickSubtitleNavController.currentBackStackEntryAsState()
    val quickSubtitleRoute = quickSubtitleBackStackEntry?.destination?.route ?: QuickSubtitleRoutes.Main
    val soundboardBackStackEntry by soundboardNavController.currentBackStackEntryAsState()
    val soundboardRoute = soundboardBackStackEntry?.destination?.route ?: SoundboardRoutes.Main
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
    val density = LocalDensity.current
    val activity = context as? Activity
    val inMultiWindowMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        activity?.isInMultiWindowMode == true
    } else {
        false
    }
    val miuiFloatingTopCompensation = 0.dp
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDarkTheme = currentAppDarkTheme()
    val topBarColor = if (state.solidTopBar) md2CardContainerColor() else MaterialTheme.colorScheme.primary
    val topBarContentColor = if (state.solidTopBar) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    val hiddenDrawerScrimColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDarkTheme) 0.56f else 0.32f
    )
    val desktopCaptionTopInset = with(density) {
        WindowInsets.captionBar.getTop(this).toDp()
    }
    val statusTopInset = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    val topBarDesktopMaximizeInset = when {
        desktopCaptionTopInset > 0.dp -> desktopCaptionTopInset
        inMultiWindowMode && statusTopInset > 0.dp -> statusTopInset
        else -> 0.dp
    }
    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = topBarColor.toArgb()
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = topBarColor.luminance() > 0.5f
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current
    val displayCutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
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
    val landscapeNavBarStart = if (isLandscape && !inMultiWindowMode) {
        navigationBarsPadding.calculateStartPadding(layoutDirection)
    } else {
        0.dp
    }
    val landscapeNavBarEnd = if (isLandscape && !inMultiWindowMode) {
        navigationBarsPadding.calculateEndPadding(layoutDirection)
    } else {
        0.dp
    }
    val landscapeChromeStartInset = maxOf(landscapeCutoutStart, landscapeNavBarStart)
    val landscapeChromeEndInset = maxOf(landscapeCutoutEnd, landscapeNavBarEnd)
    val hiddenDrawerWidth = remember(configuration.screenWidthDp) {
        val screenWidth = configuration.screenWidthDp.dp
        val targetWidth = UiTokens.DrawerWidthExpanded
        // Keep a small right-side visible area on narrow screens to avoid hard overflow.
        val compatEdgeGap = 24.dp
        val maxAllowed = (screenWidth - compatEdgeGap).coerceAtLeast(0.dp)
        if (maxAllowed <= 0.dp) screenWidth else minOf(targetWidth, maxAllowed)
    }
    val permanentDrawerCollapsedWidth = UiTokens.DrawerWidthCollapsed + landscapeChromeStartInset
    val permanentDrawerExpandedWidth = UiTokens.DrawerWidthExpanded + landscapeChromeStartInset
    val hiddenDrawerSurfaceWidth = hiddenDrawerWidth + landscapeChromeStartInset
    val usePermanentDrawer =
        isLandscape && state.landscapeDrawerMode == UserPrefs.DRAWER_MODE_PERMANENT
    val basePage = page
    val quickSubtitleEditorOpen =
        basePage == pageQuickSubtitle && quickSubtitleRoute == QuickSubtitleRoutes.Editor
    val quickSubtitleHistoryOpen =
        basePage == pageQuickSubtitle && quickSubtitleRoute == QuickSubtitleRoutes.History
    val quickSubtitleSubPageOpen =
        basePage == pageQuickSubtitle && quickSubtitleRoute != QuickSubtitleRoutes.Main
    val soundboardEditorOpen =
        basePage == pageSoundboard && soundboardRoute == SoundboardRoutes.Editor
    val soundboardSubPageOpen =
        basePage == pageSoundboard && soundboardRoute != SoundboardRoutes.Main
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
    val settingsLicensesOpen =
        basePage == pageSettings && settingsRoute == SettingsRoutes.Licenses
    val settingsPrivacyOpen =
        basePage == pageSettings && settingsRoute == SettingsRoutes.Privacy
    var lastTopBarBackClickAtMs by remember { mutableLongStateOf(0L) }
    var drawerExpanded by rememberSaveable { mutableStateOf(false) }
    val runningStripEligible = !(drawingFullscreen && basePage == pageDrawing)
    val showRunningStripButton = runningStripEligible
    val showRunningStripPanel = runningStripEligible && !runningStripCollapsed
    val topMicLevel = viewModel.realtimeInputLevel
    val topPlaybackProgress = viewModel.realtimePlaybackProgress
    val drawerItems = listOf(
        DrawerItem(pageQuickSubtitle, "便捷字幕", "subtitles"),
        DrawerItem(pageQuickCard, "快捷名片", "id_card"),
        DrawerItem(pageDrawing, "画板", "draw"),
        DrawerItem(pageSoundboard, "音效板", "library_music"),
        DrawerItem(pageOverlay, "悬浮窗与热键", "open_in_new"),
        DrawerItem(pageVoicePack, "语音包", "record_voice_over"),
        DrawerItem(pageSettings, "设置", "tune")
    )
    val pendingQuickSubtitleLaunchRequest = viewModel.pendingQuickSubtitleLaunchRequest
    val pendingVoicePackInstallRequest = viewModel.pendingVoicePackInstallRequest
    val pendingPresetInstallRequest = viewModel.pendingPresetInstallRequest
    val drawerSelectedPage = basePage
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
    LaunchedEffect(basePage, soundboardRoute) {
        if (basePage != pageSoundboard && soundboardRoute != SoundboardRoutes.Main) {
            soundboardNavController.popBackStack(SoundboardRoutes.Main, inclusive = false)
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
    val pendingAccessibilityExplainRequest = viewModel.pendingAccessibilityExplainRequest
    LaunchedEffect(pendingAccessibilityExplainRequest?.requestId) {
        if (pendingAccessibilityExplainRequest != null) {
            page = pageOverlay
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
            OverlayBridge.TARGET_OPEN_SOUNDBOARD -> {
                page = pageSoundboard
                if (soundboardRoute != SoundboardRoutes.Main) {
                    soundboardNavController.popBackStack(SoundboardRoutes.Main, inclusive = false)
                }
            }
            OverlayBridge.TARGET_OPEN_VOICE_PACK -> {
                page = pageVoicePack
            }
            OverlayBridge.TARGET_OPEN_SETTINGS -> {
                page = pageSettings
                if (settingsRoute != SettingsRoutes.Main) {
                    settingsNavController.popBackStack(SettingsRoutes.Main, inclusive = false)
                }
            }
            else -> {
                if (request.navigateToPage) {
                    quickSubtitleFullscreen = false
                    if (page != pageQuickSubtitle) {
                        page = pageQuickSubtitle
                    }
                    if (quickSubtitleRoute != QuickSubtitleRoutes.Main) {
                        quickSubtitleNavController.popBackStack(QuickSubtitleRoutes.Main, inclusive = false)
                    }
                }
                viewModel.applyExternalQuickSubtitleRequest(request.target, request.text)
            }
        }
        viewModel.consumeQuickSubtitleLaunchRequest(request.requestId)
        if (!usePermanentDrawer) {
            drawerState.close()
        }
    }
    LaunchedEffect(pendingVoicePackInstallRequest?.requestId) {
        val request = pendingVoicePackInstallRequest ?: return@LaunchedEffect
        page = pageVoicePack
        toast(context, request.message)
        viewModel.consumeVoicePackInstallRequest(request.requestId)
        if (!usePermanentDrawer) {
            drawerState.close()
        }
    }
    LaunchedEffect(pendingPresetInstallRequest?.requestId) {
        val request = pendingPresetInstallRequest ?: return@LaunchedEffect
        when (request.target) {
            PresetInstallTarget.QuickSubtitle -> {
                page = pageQuickSubtitle
                if (quickSubtitleRoute != QuickSubtitleRoutes.Editor) {
                    quickSubtitleNavController.navigate(QuickSubtitleRoutes.Editor) {
                        launchSingleTop = true
                    }
                }
            }
            PresetInstallTarget.Soundboard -> {
                page = pageSoundboard
                if (soundboardRoute != SoundboardRoutes.Editor) {
                    soundboardNavController.navigate(SoundboardRoutes.Editor) {
                        launchSingleTop = true
                    }
                }
            }
        }
        toast(context, request.message)
        viewModel.consumePresetInstallRequest(request.requestId)
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
            soundboardSubPageOpen -> {
                soundboardNavController.popBackStack(SoundboardRoutes.Main, inclusive = false)
            }
            settingsLogOpen || settingsLicensesOpen || settingsPrivacyOpen -> {
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

    var startRealtimeAfterPermissionGrant by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val shouldStartRealtime = startRealtimeAfterPermissionGrant
        startRealtimeAfterPermissionGrant = false
        if (granted) {
            if (shouldStartRealtime) {
                if (state.asrDir == null && !state.recognitionResourceInstalled) {
                    startRealtimeAfterRecognitionResourceInstall = true
                    recognitionResourceRequiredDialog = true
                    viewModel.refreshRecognitionResourceStatus()
                } else {
                    viewModel.start()
                }
            }
        } else {
            toast(context, "需要麦克风权限")
        }
    }
    val pendingRecordAudioPermissionRequest = viewModel.pendingRecordAudioPermissionRequest
    LaunchedEffect(pendingRecordAudioPermissionRequest?.requestId) {
        val request = pendingRecordAudioPermissionRequest ?: return@LaunchedEffect
        startRealtimeAfterPermissionGrant = request.startRealtimeOnGrant
        permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        viewModel.consumeRecordAudioPermissionRequest(request.requestId)
    }
    val voicePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.importVoice(uri) else toast(context, "未选择文件")
    }
    val quickSubtitlePresetPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.importQuickSubtitlePresetPackage(uri) else toast(context, "未选择文件")
    }
    val soundboardPresetPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.importSoundboardPresetPackage(uri) else toast(context, "未选择文件")
    }
    val recognitionResourcePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.installRecognitionResources(uri) else toast(context, "未选择文件")
    }
    val kokoroVoicePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.installKokoroVoice(uri) else toast(context, "未选择文件")
    }
    var showBuiltinVoicePicker by remember { mutableStateOf(false) }
    val recognitionResourceMissing = state.asrDir == null && !state.recognitionResourceInstalled

    fun requestRecordAudioPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.start()
        } else {
            startRealtimeAfterPermissionGrant = true
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun requestRealtimeStartWithResourceCheck(autoStartAfterInstall: Boolean) {
        if (recognitionResourceMissing) {
            startRealtimeAfterPermissionGrant = false
            startRealtimeAfterRecognitionResourceInstall = autoStartAfterInstall
            recognitionResourceRequiredDialog = true
            viewModel.refreshRecognitionResourceStatus()
            return
        }
        requestRecordAudioPermissionAndStart()
    }

    LaunchedEffect(
        recognitionResourceRequiredDialog,
        startRealtimeAfterRecognitionResourceInstall,
        state.recognitionResourceInstalled,
        state.recognitionResourceBusy
    ) {
        if (
            recognitionResourceRequiredDialog &&
            startRealtimeAfterRecognitionResourceInstall &&
            state.recognitionResourceInstalled &&
            !state.recognitionResourceBusy
        ) {
            recognitionResourceRequiredDialog = false
            startRealtimeAfterRecognitionResourceInstall = false
            requestRecordAudioPermissionAndStart()
        }
    }

    val onToggleRun = {
        if (!state.running && state.ttsDisabled) {
            toast(context, TTS_DISABLED_MESSAGE)
        }
        if (state.running) {
            viewModel.stop()
        } else {
            requestRealtimeStartWithResourceCheck(autoStartAfterInstall = true)
        }
    }
    var pttConfirmOwnedByMainPanel by remember { mutableStateOf(false) }
    var pttTemporaryStartByMainPanel by remember { mutableStateOf(false) }

    val onPushToTalkPressStart = {
        pttConfirmOwnedByMainPanel = true
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            pttConfirmOwnedByMainPanel = false
            pttTemporaryStartByMainPanel = false
            startRealtimeAfterPermissionGrant = false
            if (recognitionResourceMissing) {
                recognitionResourceRequiredDialog = true
                startRealtimeAfterRecognitionResourceInstall = false
                viewModel.refreshRecognitionResourceStatus()
            } else {
                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else if (recognitionResourceMissing) {
            pttConfirmOwnedByMainPanel = false
            pttTemporaryStartByMainPanel = false
            recognitionResourceRequiredDialog = true
            startRealtimeAfterRecognitionResourceInstall = false
            viewModel.refreshRecognitionResourceStatus()
        } else {
            pttTemporaryStartByMainPanel = !state.running
            if (pttTemporaryStartByMainPanel) {
                viewModel.start()
            }
            viewModel.beginPushToTalkSession()
            viewModel.setPushToTalkPressed(true)
        }
    }
    val onPushToTalkPressEnd: (PttConfirmReleaseAction) -> Unit = { releaseAction ->
        val shouldStop = pttTemporaryStartByMainPanel
        viewModel.commitPushToTalkSession(releaseAction)
        viewModel.setPushToTalkPressed(false)
        pttConfirmOwnedByMainPanel = false
        pttTemporaryStartByMainPanel = false
        if (shouldStop) {
            viewModel.stop()
        }
    }
    LaunchedEffect(state.pushToTalkPressed) {
        if (!state.pushToTalkPressed) {
            pttConfirmOwnedByMainPanel = false
            pttTemporaryStartByMainPanel = false
        }
    }
    if (showBuiltinVoicePicker) {
        BuiltinFilePickerDialog(
            title = "选择语音包文件",
            allowedExtensions = setOf("zip", "kigvpk"),
            onDismiss = { showBuiltinVoicePicker = false },
            onPicked = { uri ->
                showBuiltinVoicePicker = false
                viewModel.importVoice(uri)
            },
            onOpenSystemPicker = {
                showBuiltinVoicePicker = false
                voicePicker.launch("*/*")
            }
        )
    }
    if (showBuiltinQuickSubtitlePresetPicker) {
        BuiltinFilePickerDialog(
            title = "选择便捷字幕预设",
            allowedExtensions = setOf("kigtpk", "zip", "json"),
            onDismiss = { showBuiltinQuickSubtitlePresetPicker = false },
            onPicked = { uri ->
                showBuiltinQuickSubtitlePresetPicker = false
                viewModel.importQuickSubtitlePresetPackage(uri)
            },
            onOpenSystemPicker = {
                showBuiltinQuickSubtitlePresetPicker = false
                quickSubtitlePresetPicker.launch("*/*")
            }
        )
    }
    if (showBuiltinSoundboardPresetPicker) {
        BuiltinFilePickerDialog(
            title = "选择音效板预设",
            allowedExtensions = setOf("kigspk", "zip", "json"),
            onDismiss = { showBuiltinSoundboardPresetPicker = false },
            onPicked = { uri ->
                showBuiltinSoundboardPresetPicker = false
                viewModel.importSoundboardPresetPackage(uri)
            },
            onOpenSystemPicker = {
                showBuiltinSoundboardPresetPicker = false
                soundboardPresetPicker.launch("*/*")
            }
        )
    }
    if (showBuiltinRecognitionResourcePicker) {
        BuiltinFilePickerDialog(
            title = "选择语音识别资源包",
            allowedExtensions = setOf("7z", "zip"),
            onDismiss = { showBuiltinRecognitionResourcePicker = false },
            onPicked = { uri ->
                showBuiltinRecognitionResourcePicker = false
                viewModel.installRecognitionResources(uri)
            },
            onOpenSystemPicker = {
                showBuiltinRecognitionResourcePicker = false
                recognitionResourcePicker.launch("*/*")
            }
        )
    }
    if (showBuiltinKokoroVoicePicker) {
        BuiltinFilePickerDialog(
            title = "选择 Kokoro 离线语音资源",
            allowedExtensions = setOf("zip", "tar", "bz2", "tbz2"),
            onDismiss = { showBuiltinKokoroVoicePicker = false },
            onPicked = { uri ->
                showBuiltinKokoroVoicePicker = false
                viewModel.installKokoroVoice(uri)
            },
            onOpenSystemPicker = {
                showBuiltinKokoroVoicePicker = false
                kokoroVoicePicker.launch("*/*")
            }
        )
    }
    if (recognitionResourceSourceDialog) {
        RecognitionResourceSourceDialog(
            modelScopeUrl = state.recognitionResourceModelScopeUrl,
            huggingFaceUrl = state.recognitionResourceHuggingFaceUrl,
            preferredSource = state.recognitionResourcePreferredSource,
            onDismiss = { recognitionResourceSourceDialog = false },
            onConfirm = { modelScopeUrl, huggingFaceUrl, preferredSource ->
                recognitionResourceSourceDialog = false
                viewModel.setRecognitionResourceSources(modelScopeUrl, huggingFaceUrl, preferredSource)
            }
        )
    }
    if (kokoroSourceDialog) {
        KokoroSourceDialog(
            hfUrl = state.kokoroHfUrl,
            hfMirrorUrl = state.kokoroHfMirrorUrl,
            modelScopeUrl = state.kokoroModelScopeUrl,
            preferredSource = state.kokoroPreferredSource,
            onDismiss = { kokoroSourceDialog = false },
            onConfirm = { hfUrl, hfMirrorUrl, modelScopeUrl, preferredSource ->
                kokoroSourceDialog = false
                viewModel.setKokoroSources(hfUrl, hfMirrorUrl, modelScopeUrl, preferredSource)
            }
        )
    }
    if (kokoroVoiceSettingsDialog) {
        KokoroVoiceSettingsDialog(
            state = state,
            onDismiss = { kokoroVoiceSettingsDialog = false },
            onSpeakerChange = { viewModel.setKokoroSpeakerId(it) }
        )
    }
    if (recognitionResourceRequiredDialog) {
        RecognitionResourceRequiredDialog(
            state = state,
            onDismiss = {
                if (!state.recognitionResourceBusy) {
                    recognitionResourceRequiredDialog = false
                    startRealtimeAfterRecognitionResourceInstall = false
                }
            },
            onDownload = {
                viewModel.downloadRecognitionResources()
            },
            onPickLocalPackage = {
                showBuiltinRecognitionResourcePicker = true
            },
            onOpenSources = {
                recognitionResourceSourceDialog = true
            }
        )
    }
    if (quickSubtitlePresetExportDialog) {
        PresetGroupExportDialog(
            title = "导出便捷字幕预设",
            groups = viewModel.quickSubtitleGroups.map { it.id to it.title.ifBlank { "未命名分组" } },
            onDismiss = { quickSubtitlePresetExportDialog = false },
            onConfirm = { ids ->
                quickSubtitlePresetExportDialog = false
                viewModel.exportQuickSubtitlePresetPackage(ids)
            }
        )
    }
    if (soundboardPresetExportDialog) {
        PresetGroupExportDialog(
            title = "导出音效板预设",
            groups = viewModel.soundboardGroups.map { it.id to it.title.ifBlank { "未命名分组" } },
            onDismiss = { soundboardPresetExportDialog = false },
            onConfirm = { ids ->
                soundboardPresetExportDialog = false
                viewModel.exportSoundboardPresetPackage(ids)
            }
        )
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
        } else if (soundboardEditorOpen) {
            "编辑音效板"
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
        } else if (settingsLicensesOpen) {
            "开源许可证"
        } else if (settingsPrivacyOpen) {
            "隐私政策"
        } else {
            when (basePage) {
                pageQuickSubtitle -> "便捷字幕"
                pageOverlay -> "悬浮窗与热键"
                pageQuickCard -> "快捷名片"
                pageVoicePack -> "语音包"
                pageDrawing -> "画板"
                pageSoundboard -> "音效板"
                pageSettings -> "设置"
                else -> "KIGTTS"
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topBarDesktopMaximizeInset)
                .padding(top = miuiFloatingTopCompensation)
                .zIndex(2f),
            color = topBarColor,
            elevation = UiTokens.TopBarElevation
        ) {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!inMultiWindowMode) Modifier.statusBarsPadding() else Modifier)
                    .padding(start = landscapeChromeStartInset, end = landscapeChromeEndInset),
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
                                ttsDisabled = state.ttsDisabled,
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
                            soundboardSubPageOpen -> 2
                            settingsLogOpen || settingsLicensesOpen || settingsPrivacyOpen -> 3
                            quickCardSubPageOpen -> 4
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
                        if (navMode == 1 || navMode == 2 || navMode == 3 || navMode == 4) {
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
                    val showQuickSubtitleCompactEditorAction =
                        showQuickSubtitleActions &&
                            !isLandscape &&
                            state.quickSubtitleCompactControls
                    val showQuickSubtitleEditorActions =
                        basePage == pageQuickSubtitle && quickSubtitleRoute == QuickSubtitleRoutes.Editor
                    val showSoundboardEditorActions =
                        basePage == pageSoundboard && soundboardRoute == SoundboardRoutes.Editor
                    val showQuickCardMainActions =
                        basePage == pageQuickCard &&
                                quickCardRoute == QuickCardRoutes.Main
                    val showQuickCardEditorActions =
                        basePage == pageQuickCard &&
                                quickCardRoute == QuickCardRoutes.Editor &&
                                viewModel.quickCardDraft?.isNew == false
                    val showQuickCardSortActions =
                        basePage == pageQuickCard &&
                                quickCardRoute == QuickCardRoutes.Sort
                    val showQuickCardWebActions =
                        basePage == pageQuickCard && quickCardRoute == QuickCardRoutes.Web
                    val showDrawingActions = basePage == pageDrawing
                    val showVoicePackActions = basePage == pageVoicePack
                    val showSettingsEntryActions =
                        basePage == pageSettings && settingsRoute == SettingsRoutes.Main
                    val showSettingsLogActions = basePage == pageSettings && settingsLogOpen
                    val settingsActions = logTopBarActions

                    val quickSubtitleAlpha by animateFloatAsState(
                        targetValue = if (showQuickSubtitleActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_quick_subtitle_actions_alpha"
                    )
                    val quickSubtitleEditorAlpha by animateFloatAsState(
                        targetValue = if (showQuickSubtitleEditorActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_quick_subtitle_editor_actions_alpha"
                    )
                    val soundboardEditorAlpha by animateFloatAsState(
                        targetValue = if (showSoundboardEditorActions) 1f else 0f,
                        animationSpec = tween(130, easing = FastOutSlowInEasing),
                        label = "topbar_soundboard_editor_actions_alpha"
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
                        showQuickSubtitleEditorActions || showSoundboardEditorActions -> 96.dp
                        showQuickSubtitleCompactEditorAction -> 96.dp
                        showQuickCardMainActions || showQuickCardEditorActions -> 96.dp
                        showQuickCardSortActions -> 48.dp
                        showQuickCardWebActions -> 48.dp
                        showDrawingActions -> 144.dp
                        showQuickSubtitleActions || showVoicePackActions || showSettingsEntryActions -> 48.dp
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
                                if (showQuickSubtitleCompactEditorAction) {
                                    IconButton(
                                        onClick = {
                                            quickSubtitleNavController.navigate(QuickSubtitleRoutes.Editor)
                                        },
                                        enabled = showQuickSubtitleActions
                                    ) {
                                        MsIcon(
                                            name = "edit",
                                            contentDescription = "编辑快捷文本"
                                        )
                                    }
                                }
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

                        key("topbar_quick_subtitle_editor_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = quickSubtitleEditorAlpha }
                                    .zIndex(if (showQuickSubtitleEditorActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        if (state.useBuiltinFileManager) {
                                            showBuiltinQuickSubtitlePresetPicker = true
                                        } else {
                                            quickSubtitlePresetPicker.launch("*/*")
                                        }
                                    },
                                    enabled = showQuickSubtitleEditorActions
                                ) {
                                    MsIcon("folder_open", contentDescription = "导入便捷字幕预设")
                                }
                                IconButton(
                                    onClick = { quickSubtitlePresetExportDialog = true },
                                    enabled = showQuickSubtitleEditorActions && viewModel.quickSubtitleGroups.isNotEmpty()
                                ) {
                                    MsIcon("share", contentDescription = "导出便捷字幕预设")
                                }
                            }
                        }

                        key("topbar_soundboard_editor_actions_layer") {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer { alpha = soundboardEditorAlpha }
                                    .zIndex(if (showSoundboardEditorActions) 2f else 0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        if (state.useBuiltinFileManager) {
                                            showBuiltinSoundboardPresetPicker = true
                                        } else {
                                            soundboardPresetPicker.launch("*/*")
                                        }
                                    },
                                    enabled = showSoundboardEditorActions
                                ) {
                                    MsIcon("folder_open", contentDescription = "导入音效板预设")
                                }
                                IconButton(
                                    onClick = { soundboardPresetExportDialog = true },
                                    enabled = showSoundboardEditorActions && viewModel.soundboardGroups.isNotEmpty()
                                ) {
                                    MsIcon("share", contentDescription = "导出音效板预设")
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
                                    onClick = { viewModel.rotateDrawingCanvasQuarterTurns(-1) },
                                    enabled = showDrawingActions
                                ) {
                                    MsIcon("rotate_left", contentDescription = "向左旋转画布")
                                }
                                IconButton(
                                    onClick = { viewModel.rotateDrawingCanvasQuarterTurns(1) },
                                    enabled = showDrawingActions
                                ) {
                                    MsIcon("rotate_right", contentDescription = "向右旋转画布")
                                }
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
                                    enabled = showQuickCardSortActions && quickCardActions?.canConfirm == true
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
                                    Md2AnimatedOptionMenu(
                                        expanded = showQuickCardWebActions && quickCardWebMenuExpanded,
                                        onDismissRequest = { quickCardWebMenuExpanded = false }
                                    ) {
                                        M2DropdownMenuItem(
                                            onClick = {
                                                quickCardWebMenuExpanded = false
                                                quickCardActions?.onWebReload?.invoke()
                                            },
                                            enabled = quickCardActions?.canWebReload == true
                                        ) {
                                            Text("刷新")
                                        }
                                        M2DropdownMenuItem(
                                            onClick = {
                                                quickCardWebMenuExpanded = false
                                                quickCardActions?.onWebBack?.invoke()
                                            },
                                            enabled = quickCardActions?.canWebBack == true
                                        ) {
                                            Text("返回上一页")
                                        }
                                        M2DropdownMenuItem(
                                            onClick = {
                                                quickCardWebMenuExpanded = false
                                                quickCardActions?.onWebForward?.invoke()
                                            },
                                            enabled = quickCardActions?.canWebForward == true
                                        ) {
                                            Text("返回下一页")
                                        }
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
                                    onClick = {
                                        if (state.useBuiltinFileManager) {
                                            showBuiltinVoicePicker = true
                                        } else {
                                            voicePicker.launch("*/*")
                                        }
                                    },
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
                        pttConfirmOwnedByMainPanel = pttConfirmOwnedByMainPanel,
                        onFloatingInputPreviewChange = { quickSubtitleFloatingInputPreview = it },
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
                    pageSoundboard -> SoundboardNavHost(
                        navController = soundboardNavController,
                        viewModel = viewModel,
                        state = state
                    )
                    pageSettings -> SettingsNavHost(
                        navController = settingsNavController,
                        viewModel = viewModel,
                        state = state,
                        onTopBarActionsChange = { logTopBarActions = it },
                        onOpenRecognitionResourceSources = { recognitionResourceSourceDialog = true },
                        onPickRecognitionResourcePackage = { showBuiltinRecognitionResourcePicker = true },
                        onDownloadRecognitionResources = { viewModel.downloadRecognitionResources() },
                        onOpenKokoroSources = { kokoroSourceDialog = true },
                        onPickKokoroVoicePackage = { showBuiltinKokoroVoicePicker = true },
                        onDownloadKokoroVoice = { viewModel.downloadKokoroVoice() },
                        onOpenKokoroVoiceSettings = { kokoroVoiceSettingsDialog = true }
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
                            ttsDisabled = state.ttsDisabled,
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
        val controller = WindowCompat.getInsetsController(window, window.decorView)
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
                                    horizontalStartInset = landscapeChromeStartInset,
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
                                .padding(start = animatedContentStartPadding, end = landscapeChromeEndInset)
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
                                    horizontalStartInset = landscapeChromeStartInset,
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
                                horizontalStartInset = landscapeChromeStartInset,
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
                                .padding(start = landscapeChromeStartInset, end = landscapeChromeEndInset)
                        )
                    }
                }
        }
        QuickSubtitleFloatingInputPreviewOverlay(
            preview = if (
                basePage == pageQuickSubtitle &&
                quickSubtitleRoute == QuickSubtitleRoutes.Main
            ) {
                quickSubtitleFloatingInputPreview
            } else {
                null
            },
            textAlign = if (viewModel.quickSubtitleCentered) TextAlign.Center else TextAlign.Start,
            fontWeight = if (viewModel.quickSubtitleBold) FontWeight.Bold else FontWeight.Normal,
            maxFontSizeSp = viewModel.quickSubtitleFontSizeSp,
            autoFitEnabled = state.quickSubtitleAutoFit,
            rotated180 = viewModel.quickSubtitleRotated180,
            startPadding = landscapeChromeStartInset + 16.dp,
            endPadding = landscapeChromeEndInset + 16.dp,
            topPadding = statusTopInset + 6.dp,
            modifier = Modifier
                .matchParentSize()
                .zIndex(20f)
        )
    }
}

@Composable
private fun QuickSubtitleFloatingInputPreviewOverlay(
    preview: QuickSubtitleFloatingInputPreviewState?,
    textAlign: TextAlign,
    fontWeight: FontWeight,
    maxFontSizeSp: Float,
    autoFitEnabled: Boolean,
    rotated180: Boolean,
    startPadding: Dp,
    endPadding: Dp,
    topPadding: Dp,
    modifier: Modifier = Modifier
) {
    var retainedPreview by remember { mutableStateOf<QuickSubtitleFloatingInputPreviewState?>(null) }
    LaunchedEffect(preview) {
        if (preview != null) retainedPreview = preview
    }
    AnimatedVisibility(
        visible = preview != null,
        modifier = modifier.padding(
            start = startPadding,
            end = endPadding,
            top = topPadding,
            bottom = retainedPreview?.bottomPadding ?: 0.dp
        ),
        enter = fadeIn(animationSpec = tween(150)) +
            scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            ),
        exit = fadeOut(animationSpec = tween(120)) +
            scaleOut(
                targetScale = 0.98f,
                animationSpec = tween(140, easing = FastOutSlowInEasing)
            )
    ) {
        val activePreview = retainedPreview
        if (activePreview != null) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .mdCenteredShadow(
                        shape = RoundedCornerShape(UiTokens.Radius),
                        shadowStyle = MdCardShadowStyle
                    ),
                shape = RoundedCornerShape(UiTokens.Radius),
                backgroundColor = md2ElevatedCardContainerColor(UiTokens.MenuElevation),
                elevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    AnimatedContent(
                        targetState = activePreview.text to activePreview.cursorIndex,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn(initialAlpha = 0.45f, animationSpec = tween(140)),
                                initialContentExit = fadeOut(targetAlpha = 0.45f, animationSpec = tween(160)),
                                sizeTransform = null
                            )
                        },
                        label = "quick_subtitle_root_input_preview_text_change"
                    ) { (text, cursorIndex) ->
                        Crossfade(
                            targetState = rotated180,
                            animationSpec = tween(160),
                            label = "quick_subtitle_root_input_preview_rotation"
                        ) { rotated ->
                            QuickSubtitleAdaptiveText(
                                text = text,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = textAlign,
                                fontWeight = fontWeight,
                                maxFontSizeSp = maxFontSizeSp,
                                minFontSizeSp = 14f,
                                lineHeightMultiplier = 1.15f,
                                autoFitEnabled = autoFitEnabled,
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = if (rotated) {
                                    if (textAlign == TextAlign.Center) Alignment.BottomCenter else Alignment.BottomStart
                                } else {
                                    Alignment.TopStart
                                },
                                textRotationZ = if (rotated) 180f else 0f,
                                cursorIndex = cursorIndex,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
    val drawerLogoRes = if (currentAppDarkTheme()) R.drawable.logo_white else R.drawable.logo_black
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
fun ModelScreen(state: UiState) {
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
                Text("ASR 模型由语音识别资源包管理", fontWeight = FontWeight.SemiBold)
                Text("当前 Android 版本不再内置 ASR / 语音增强模型，请在“设置 - 识别”安装语音识别资源包。")
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
                Text(
                    when {
                        isSystemTtsVoiceDir(state.voiceDir) -> SYSTEM_TTS_DEFAULT_LABEL
                        state.voiceDir != null -> state.voiceDir.absolutePath
                        else -> "未选择"
                    }
                )
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
private fun VoicePackAvatarPlaceholder(
    modifier: Modifier,
    isSystemPack: Boolean,
    isKokoroPack: Boolean = false,
    logoSize: Dp = 50.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.Radius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            isSystemPack -> {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    modifier = Modifier.size(logoSize),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(LocalContentColor.current)
                )
            }
            isKokoroPack -> {
                val kokoroIconSize = logoSize * 0.72f
                val iconTextSize = with(LocalDensity.current) { kokoroIconSize.toSp() }
                Text(
                    text = "groups",
                    color = LocalContentColor.current,
                    style = TextStyle(
                        fontFamily = MaterialSymbolsSharp,
                        fontWeight = FontWeight.W400,
                        fontSize = iconTextSize,
                        lineHeight = iconTextSize,
                        letterSpacing = 0.sp,
                        fontFeatureSettings = "'liga' 1"
                    )
                )
            }
            else -> {
                Text("无头像", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
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
    var showBuiltinAvatarGallery by remember { mutableStateOf(false) }
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
    fun launchAvatarCrop(uri: Uri) {
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
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            launchAvatarCrop(uri)
        } else {
            avatarTarget = null
        }
    }
    LaunchedEffect(Unit) {
        viewModel.refreshVoicePacks()
    }

    CenteredPageColumn(
        maxWidth = UiTokens.WideListMaxWidth,
        contentSpacing = 0.dp
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

    if (detailPack != null && isKokoroVoiceDir(detailPack.dir)) {
        KokoroVoiceSettingsDialog(
            state = state,
            onDismiss = { detailPackPath = null },
            onSpeakerChange = { viewModel.setKokoroSpeakerId(it) }
        )
    } else if (detailPack != null) {
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
                            VoicePackAvatarPlaceholder(
                                modifier = Modifier.size(64.dp),
                                isSystemPack = isSystemTtsVoiceDir(detailPack.dir),
                                isKokoroPack = isKokoroVoiceDir(detailPack.dir),
                                logoSize = 50.dp
                            )
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
                                    if (state.useBuiltinGallery) {
                                        showBuiltinAvatarGallery = true
                                    } else {
                                        imagePicker.launch("image/*")
                                    }
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
        val deletingKokoro = deletePack?.let { isKokoroVoiceDir(it.dir) } == true
        AlertDialog(
            onDismissRequest = { deletePack = null },
            title = { Text(if (deletingKokoro) "删除 Kokoro 离线语音" else "删除语音包") },
            text = {
                Text(
                    if (deletingKokoro) {
                        "确定删除 Kokoro 离线语音吗？删除后，“语音包”页面将不再显示 Kokoro，需要重新下载或本地安装后才能使用。"
                    } else {
                        "确定删除该语音包吗？此操作不可撤销。"
                    }
                )
            },
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

    if (showBuiltinAvatarGallery) {
        BuiltinGalleryPickerDialog(
            title = "选择头像",
            onDismiss = {
                showBuiltinAvatarGallery = false
                avatarTarget = null
            },
            onPicked = { uri ->
                showBuiltinAvatarGallery = false
                launchAvatarCrop(uri)
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
                private val edgeAutoScroller = DragEdgeAutoScroller()

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

                override fun onChildDraw(
                    c: android.graphics.Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                        edgeAutoScroller.update(recyclerView, viewHolder.itemView, dY)
                    } else {
                        edgeAutoScroller.stop()
                    }
                }

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
                        edgeAutoScroller.stop()
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
                    edgeAutoScroller.stop()
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
                KigttsFontScaleProvider {
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
    val isSystemPack = isSystemTtsVoiceDir(pack.dir)
    val isKokoroPack = isKokoroVoiceDir(pack.dir)
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
                        VoicePackAvatarPlaceholder(
                            modifier = Modifier.size(72.dp),
                            isSystemPack = isSystemPack,
                            isKokoroPack = isKokoroPack,
                            logoSize = 58.dp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pack.meta.name, fontWeight = FontWeight.SemiBold)
                            if (isSystemPack) {
                                Spacer(Modifier.width(6.dp))
                                Text("系统", style = MaterialTheme.typography.bodySmall)
                            }
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
                        icon = if (isKokoroPack) "settings" else "info",
                        contentDescription = if (isKokoroPack) "Kokoro 设置" else "语音包详细信息",
                        onClick = onDetail,
                        enabled = !isSystemPack
                    )
                    Md2IconButton(
                        icon = "share",
                        contentDescription = "分享语音包",
                        onClick = onShare,
                        enabled = !isSystemPack && !isKokoroPack
                    )
                    Md2IconButton(
                        icon = "delete",
                        contentDescription = "删除语音包",
                        onClick = onDelete,
                        enabled = !isSystemPack
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

private class DragEdgeAutoScroller {
    private var recyclerView: RecyclerView? = null
    private var externalScrollBy: ((Int) -> Boolean)? = null
    private var direction: Int = 0
    private var stepPx: Int = 0
    private var isPosted: Boolean = false

    private val scrollTick = object : Runnable {
        override fun run() {
            val rv = recyclerView
            val dir = direction
            if (rv == null || dir == 0 || !rv.isAttachedToWindow) {
                direction = 0
                stepPx = 0
                recyclerView = null
                externalScrollBy = null
                isPosted = false
                return
            }
            val delta = dir * stepPx.coerceAtLeast(1)
            val consumed = if (rv.canScrollVertically(dir)) {
                rv.scrollBy(0, delta)
                true
            } else {
                externalScrollBy?.invoke(delta) == true
            }
            if (!consumed) {
                direction = 0
                stepPx = 0
                recyclerView = null
                externalScrollBy = null
                isPosted = false
                return
            }
            rv.postOnAnimation(this)
        }
    }

    fun update(
        recyclerView: RecyclerView,
        draggedView: View,
        dY: Float,
        externalScrollBy: ((Int) -> Boolean)? = null
    ) {
        val density = recyclerView.resources.displayMetrics.density
        val edgePx = (72f * density).roundToInt().coerceAtLeast(1)
        val minStepPx = (2f * density).roundToInt().coerceAtLeast(1)
        val maxStepPx = (8f * density).roundToInt().coerceAtLeast(minStepPx)
        val (draggedTop, draggedBottom, topEdge, bottomEdge) = if (externalScrollBy != null) {
            val visibleFrame = android.graphics.Rect()
            val location = IntArray(2)
            recyclerView.getWindowVisibleDisplayFrame(visibleFrame)
            draggedView.getLocationOnScreen(location)
            val itemTop = location[1] + dY
            val itemBottom = location[1] + draggedView.height + dY
            Quad(itemTop, itemBottom, visibleFrame.top + edgePx.toFloat(), visibleFrame.bottom - edgePx.toFloat())
        } else {
            Quad(
                draggedView.top + dY,
                draggedView.bottom + dY,
                recyclerView.paddingTop + edgePx.toFloat(),
                recyclerView.height - recyclerView.paddingBottom - edgePx.toFloat()
            )
        }
        val topOverlap = topEdge - draggedTop
        val bottomOverlap = draggedBottom - bottomEdge
        val nextDirection = when {
            topOverlap > 0f -> -1
            bottomOverlap > 0f -> 1
            else -> 0
        }
        if (nextDirection == 0 || (!recyclerView.canScrollVertically(nextDirection) && externalScrollBy == null)) {
            stop()
            return
        }
        val overlap = if (nextDirection < 0) topOverlap else bottomOverlap
        val factor = (overlap / edgePx).coerceIn(0f, 1f)
        stepPx = (minStepPx + ((maxStepPx - minStepPx) * factor)).roundToInt().coerceAtLeast(minStepPx)
        this.recyclerView = recyclerView
        this.externalScrollBy = externalScrollBy
        direction = nextDirection
        if (!isPosted) {
            isPosted = true
            recyclerView.postOnAnimation(scrollTick)
        }
    }

    fun stop() {
        direction = 0
        stepPx = 0
        recyclerView = null
        externalScrollBy = null
    }

    private data class Quad(
        val draggedTop: Float,
        val draggedBottom: Float,
        val topEdge: Float,
        val bottomEdge: Float
    )
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
    view.animate().setListener(null)
    view.alpha = 0f
    view.translationY = offsetY
    view.animate()
        .alpha(1f)
        .translationY(0f)
        .setStartDelay(delayMs)
        .setDuration(220L)
        .setInterpolator(FastOutSlowInInterpolator())
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                resetVoicePackStaggerView(view)
            }

            override fun onAnimationEnd(animation: Animator) {
                resetVoicePackStaggerView(view)
            }
        })
        .start()
    view.postDelayed(
        { resetVoicePackStaggerView(view) },
        delayMs + 300L
    )
}

private fun resetVoicePackStaggerView(view: View) {
    view.animate().setListener(null)
    view.alpha = 1f
    view.translationY = 0f
}

@Composable
private fun QuickSubtitleNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    state: UiState,
    onToggleMic: () -> Unit,
    onPushToTalkPressStart: () -> Unit,
    onPushToTalkPressEnd: (PttConfirmReleaseAction) -> Unit,
    pttConfirmOwnedByMainPanel: Boolean,
    onFloatingInputPreviewChange: (QuickSubtitleFloatingInputPreviewState?) -> Unit,
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
                pttConfirmOwnedByMainPanel = pttConfirmOwnedByMainPanel,
                onFloatingInputPreviewChange = onFloatingInputPreviewChange,
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

@Composable
private fun SoundboardNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    state: UiState
) {
    NavHost(
        navController = navController,
        startDestination = SoundboardRoutes.Main,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            if (initialState.destination.route == SoundboardRoutes.Main &&
                targetState.destination.route == SoundboardRoutes.Editor
            ) {
                fadeIn(animationSpec = tween(180)) +
                        slideInHorizontally(
                            initialOffsetX = { full -> full / 10 },
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeIn(animationSpec = tween(120))
            }
        },
        exitTransition = {
            if (initialState.destination.route == SoundboardRoutes.Main &&
                targetState.destination.route == SoundboardRoutes.Editor
            ) {
                fadeOut(animationSpec = tween(130)) +
                        slideOutHorizontally(
                            targetOffsetX = { full -> -full / 14 },
                            animationSpec = tween(130, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeOut(animationSpec = tween(90))
            }
        },
        popEnterTransition = {
            if (initialState.destination.route == SoundboardRoutes.Editor &&
                targetState.destination.route == SoundboardRoutes.Main
            ) {
                fadeIn(animationSpec = tween(170)) +
                        slideInHorizontally(
                            initialOffsetX = { full -> -full / 12 },
                            animationSpec = tween(170, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeIn(animationSpec = tween(120))
            }
        },
        popExitTransition = {
            if (initialState.destination.route == SoundboardRoutes.Editor &&
                targetState.destination.route == SoundboardRoutes.Main
            ) {
                fadeOut(animationSpec = tween(130)) +
                        slideOutHorizontally(
                            targetOffsetX = { full -> full / 16 },
                            animationSpec = tween(130, easing = FastOutSlowInEasing)
                        )
            } else {
                fadeOut(animationSpec = tween(90))
            }
        }
    ) {
        composable(SoundboardRoutes.Main) {
            SoundboardScreen(
                viewModel = viewModel,
                onOpenEditor = { navController.navigate(SoundboardRoutes.Editor) }
            )
        }
        composable(SoundboardRoutes.Editor) {
            SoundboardEditorScreen(
                viewModel = viewModel,
                state = state,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PresetGroupExportDialog(
    title: String,
    groups: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var selectedIds by remember(groups) { mutableStateOf(groups.map { it.first }.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("选择需要导出的分组")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(groups, key = { it.first }) { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(UiTokens.Radius))
                                .clickable {
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = id in selectedIds,
                                onCheckedChange = {
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                }
                            )
                            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Md2TextButton(
                onClick = { onConfirm(selectedIds) },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("导出")
            }
        },
        dismissButton = {
            Md2TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SoundboardScreen(
    viewModel: MainViewModel,
    onOpenEditor: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val navBarsBottomInset = navBarsPadding.calculateBottomPadding()
    val groups = viewModel.soundboardGroups
    val selectedGroupIndex = viewModel.currentSoundboardGroupIndex().coerceIn(0, groups.lastIndex.coerceAtLeast(0))
    val layoutMode = viewModel.currentSoundboardLayout(isLandscape)
    val listContent: @Composable (List<SoundboardItem>) -> Unit = { targetItems ->
        if (layoutMode == SoundboardLayoutMode.List) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(targetItems, key = { it.id }) { item ->
                    SoundboardListItem(
                        item = item,
                        playing = viewModel.isSoundboardItemPlaying(item.id),
                        progress = viewModel.soundboardItemProgress(item.id),
                        onPlay = { viewModel.playSoundboardItem(item) },
                        onStop = { viewModel.stopSoundboardItem(item.id) }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(layoutMode.columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(targetItems.size, key = { index -> targetItems[index].id }) { index ->
                    val item = targetItems[index]
                    SoundboardGridItem(
                        item = item,
                        playing = viewModel.isSoundboardItemPlaying(item.id),
                        progress = viewModel.soundboardItemProgress(item.id),
                        onPlay = { viewModel.playSoundboardItem(item) },
                        onStop = { viewModel.stopSoundboardItem(item.id) }
                    )
                }
            }
        }
    }
    val contentCard: @Composable (Modifier) -> Unit = { cardModifier ->
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = UiTokens.CardElevation
        ) {
            AnimatedContent(
                targetState = selectedGroupIndex,
                transitionSpec = {
                    soundboardGroupSwitchTransform(
                        initialIndex = initialState,
                        targetIndex = targetState,
                        isLandscape = isLandscape
                    )
                },
                label = "soundboard_items_switch"
            ) { targetIndex ->
                val targetItems = groups.getOrNull(targetIndex)?.items.orEmpty()
                if (targetItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "当前分组暂无音效，请进入编辑页添加",
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    listContent(targetItems)
                }
            }
        }
    }
    val tabsCard: @Composable (Modifier, Boolean) -> Unit = { tabsModifier, vertical ->
        Card(
            modifier = tabsModifier,
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = UiTokens.CardElevation
        ) {
            if (vertical) {
                Column(
                    modifier = Modifier.fillMaxSize()
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
                                    .clickable { viewModel.selectSoundboardGroup(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                MsIcon(
                                    group.icon,
                                    contentDescription = group.title.ifBlank { "未命名分组" }
                                )
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(onClick = onOpenEditor) {
                                MsIcon(
                                    "edit",
                                    contentDescription = "编辑音效板",
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
                            val tabBg =
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
                            Row(
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(UiTokens.Radius))
                                    .background(tabBg)
                                    .clickable { viewModel.selectSoundboardGroup(index) }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val displayTitle = group.title.ifBlank { "未命名分组" }
                                MsIcon(group.icon, contentDescription = displayTitle)
                                Text(displayTitle, maxLines = 1)
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
                                    contentDescription = "编辑音效板",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    CenteredPageBox(
        maxWidth = UiTokens.WideContentMaxWidth,
        modifier = Modifier.fillMaxSize()
    ) {
        val pageModifier = Modifier
            .fillMaxSize()
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = 12.dp,
                bottom = 12.dp + navBarsBottomInset
            )
        if (isLandscape) {
            Row(
                modifier = pageModifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    contentCard(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                tabsCard(
                    Modifier
                        .width(54.dp)
                        .fillMaxHeight(),
                    true
                )
            }
        } else {
            Column(
                modifier = pageModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                contentCard(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                tabsCard(Modifier.fillMaxWidth(), false)
            }
        }
    }
}

private fun soundboardGroupSwitchTransform(
    initialIndex: Int,
    targetIndex: Int,
    isLandscape: Boolean
): ContentTransform {
    val forward = targetIndex >= initialIndex
    return if (isLandscape) {
        ContentTransform(
            targetContentEnter = fadeIn(animationSpec = tween(200)) +
                slideInVertically(
                    initialOffsetY = { full ->
                        val d = kotlin.math.min(full / 3, 56)
                        if (forward) d else -d
                    },
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ),
            initialContentExit = fadeOut(animationSpec = tween(170)) +
                slideOutVertically(
                    targetOffsetY = { full ->
                        val d = kotlin.math.min(full / 4, 42)
                        if (forward) -d else d
                    },
                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                ),
            sizeTransform = null
        )
    } else {
        ContentTransform(
            targetContentEnter = fadeIn(animationSpec = tween(200)) +
                slideInHorizontally(
                    initialOffsetX = { full ->
                        val d = kotlin.math.min(full / 3, 140)
                        if (forward) d else -d
                    },
                    animationSpec = tween(230, easing = FastOutSlowInEasing)
                ),
            initialContentExit = fadeOut(animationSpec = tween(170)) +
                slideOutHorizontally(
                    targetOffsetX = { full ->
                        val d = kotlin.math.min(full / 4, 104)
                        if (forward) -d else d
                    },
                    animationSpec = tween(190, easing = FastOutSlowInEasing)
                ),
            sizeTransform = null
        )
    }
}

@Composable
private fun SoundboardGridItem(
    item: SoundboardItem,
    playing: Boolean,
    progress: Float,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clickable { onPlay() },
        shape = RoundedCornerShape(UiTokens.Radius),
        backgroundColor = md2CardContainerColor(),
        elevation = UiTokens.CardElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { "未命名音效" },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.wakeWord.isNotBlank()) {
                        Text(
                            text = item.wakeWord,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = { if (playing) onStop() else onPlay() }) {
                    MsIcon(
                        if (playing) "stop" else "play_arrow",
                        contentDescription = if (playing) "停止音效" else "播放音效"
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun SoundboardListItem(
    item: SoundboardItem,
    playing: Boolean,
    progress: Float,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(UiTokens.Radius),
        backgroundColor = md2CardContainerColor(),
        elevation = UiTokens.CardElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { "未命名音效" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.wakeWord.isNotBlank()) {
                        Text(
                            text = item.wakeWord,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }
                IconButton(onClick = { if (playing) onStop() else onPlay() }) {
                    MsIcon(
                        if (playing) "stop" else "play_arrow",
                        contentDescription = if (playing) "停止音效" else "播放音效"
                    )
                }
            }
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun SoundboardLayoutDropdownRow(
    title: String,
    selected: SoundboardLayoutMode,
    options: List<SoundboardLayoutMode>,
    onSelected: (SoundboardLayoutMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(UiTokens.Radius))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selected.label, modifier = Modifier.weight(1f))
                MsIcon("keyboard_arrow_down", contentDescription = "切换排列方式")
            }
            Md2AnimatedOptionMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { mode ->
                    M2DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onSelected(mode)
                        }
                    ) {
                        Text(
                            mode.label,
                            fontWeight = if (mode == selected) FontWeight.SemiBold else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SoundboardEditorScreen(
    viewModel: MainViewModel,
    state: UiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val groups = viewModel.soundboardGroups
    var selectedGroupIndex by remember(groups, viewModel.soundboardSelectedGroupId) {
        mutableIntStateOf(viewModel.currentSoundboardGroupIndex().coerceIn(0, groups.lastIndex.coerceAtLeast(0)))
    }
    val selectedGroup = groups.getOrNull(selectedGroupIndex)
    val portraitLayout = viewModel.soundboardPortraitLayout
    val landscapeLayout = viewModel.soundboardLandscapeLayout
    val portraitLayoutOptions = remember {
        SoundboardLayoutMode.entries.filter {
            it != SoundboardLayoutMode.Grid7 && it != SoundboardLayoutMode.Grid8
        }
    }
    val landscapeLayoutOptions = remember { SoundboardLayoutMode.entries.toList() }
    val iconChoices = remember { SoundboardGroupIconChoices }
    val listState = rememberLazyListState()
    val groupTabsScrollState = rememberScrollState()
    val groupTabsScrollScope = rememberCoroutineScope()
    val pageEdgeScrollScope = rememberCoroutineScope()
    var pendingScrollToNewGroup by remember { mutableIntStateOf(0) }

    suspend fun scrollGroupTabsToEndWhenReady(request: Int) {
        repeat(12) {
            delay(16)
            val maxScroll = groupTabsScrollState.maxValue
            if (maxScroll > 0) {
                groupTabsScrollState.scrollTo(maxScroll)
                if (pendingScrollToNewGroup == request) pendingScrollToNewGroup = 0
                return
            }
        }
        groupTabsScrollState.scrollTo(groupTabsScrollState.maxValue)
        if (pendingScrollToNewGroup == request) pendingScrollToNewGroup = 0
    }

    LaunchedEffect(groups.size, pendingScrollToNewGroup) {
        if (pendingScrollToNewGroup <= 0 || groups.isEmpty()) return@LaunchedEffect
        scrollGroupTabsToEndWhenReady(pendingScrollToNewGroup)
    }

    CenteredPageBox(
        maxWidth = UiTokens.WideContentMaxWidth,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = UiTokens.PageTopBlank, bottom = UiTokens.PageBottomBlank),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "soundboard_settings_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(UiTokens.Radius),
                    backgroundColor = md2CardContainerColor(),
                    elevation = UiTokens.CardElevation
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("关键词触发音效板", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Md2Switch(
                                checked = state.soundboardKeywordTriggerEnabled,
                                onCheckedChange = { viewModel.setSoundboardKeywordTriggerEnabled(it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("触发关键词时不进行朗读", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Md2Switch(
                                checked = state.soundboardSuppressTtsOnKeyword,
                                onCheckedChange = { viewModel.setSoundboardSuppressTtsOnKeyword(it) }
                            )
                        }
                        Text(
                            "命中音效板唤醒词时只上屏并播放音效，跳过本句 TTS。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("禁用TTS", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Md2Switch(
                                checked = state.ttsDisabled,
                                onCheckedChange = { viewModel.setTtsDisabled(it) }
                            )
                        }
                        SoundboardLayoutDropdownRow(
                            title = "竖屏布局样式：",
                            selected = portraitLayout,
                            options = portraitLayoutOptions,
                            onSelected = { viewModel.updateSoundboardLayout(landscape = false, layout = it) }
                        )
                        SoundboardLayoutDropdownRow(
                            title = "横屏布局样式：",
                            selected = landscapeLayout,
                            options = landscapeLayoutOptions,
                            onSelected = { viewModel.updateSoundboardLayout(landscape = true, layout = it) }
                        )
                    }
                }
            }

            item(key = "soundboard_groups_card") {
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
                            Md2TextButton(onClick = {
                                pendingScrollToNewGroup += 1
                                viewModel.addSoundboardGroup()
                                toast(context, "已新增分组")
                            }) {
                                MsIcon("add", contentDescription = "新增分组")
                                Spacer(Modifier.width(4.dp))
                                Text("新增")
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(groupTabsScrollState)
                        ) {
                            Row(
                                modifier = Modifier.onSizeChanged {
                                    val request = pendingScrollToNewGroup
                                    if (request > 0) {
                                        groupTabsScrollScope.launch {
                                            scrollGroupTabsToEndWhenReady(request)
                                        }
                                    }
                                },
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
                                                viewModel.selectSoundboardGroup(idx)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val displayTitle = group.title.ifBlank { "未命名分组" }
                                        MsIcon(group.icon, contentDescription = displayTitle)
                                        Text(displayTitle)
                                        Text("(${group.items.size})", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        if (selectedGroup != null) {
                            Md2OutlinedField(
                                value = selectedGroup.title,
                                onValueChange = { viewModel.updateSoundboardGroupMeta(selectedGroupIndex, it, selectedGroup.icon) },
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
                                                viewModel.updateSoundboardGroupMeta(selectedGroupIndex, selectedGroup.title, icon)
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
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MsIcon("hearing", contentDescription = "参与关键词唤醒")
                                    Text("参与关键词唤醒", style = MaterialTheme.typography.bodySmall)
                                }
                                Md2Switch(
                                    checked = selectedGroup.keywordWakeEnabled,
                                    onCheckedChange = {
                                        viewModel.setSoundboardGroupKeywordWakeEnabled(selectedGroupIndex, it)
                                    }
                                )
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
                                            viewModel.moveSoundboardGroup(selectedGroupIndex, selectedGroupIndex - 1)
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
                                            viewModel.moveSoundboardGroup(selectedGroupIndex, selectedGroupIndex + 1)
                                            selectedGroupIndex += 1
                                        }
                                    },
                                    enabled = selectedGroupIndex < groups.lastIndex
                                )
                                Md2IconButton(
                                    icon = "delete",
                                    contentDescription = "删除分组",
                                    onClick = {
                                        viewModel.removeSoundboardGroup(selectedGroupIndex)
                                        selectedGroupIndex = viewModel.currentSoundboardGroupIndex()
                                    },
                                    enabled = groups.size > 1
                                )
                            }
                        }
                    }
                }
            }

            if (groups.isNotEmpty()) {
                item(key = "soundboard_items_card") {
                    AnimatedContent(
                        targetState = selectedGroupIndex.coerceIn(0, groups.lastIndex.coerceAtLeast(0)),
                        transitionSpec = {
                            soundboardGroupSwitchTransform(
                                initialIndex = initialState,
                                targetIndex = targetState,
                                isLandscape = isLandscape
                            )
                        },
                        label = "soundboard_editor_items_switch"
                    ) { targetIndex ->
                        val targetGroup = groups.getOrNull(targetIndex)
                        if (targetGroup != null) {
                            SoundboardItemsRecyclerCard(
                                viewModel = viewModel,
                                state = viewModel.uiState,
                                groupIndex = targetIndex,
                                items = targetGroup.items,
                                parentEdgeScrollBy = { delta ->
                                    val canScroll = if (delta < 0) listState.canScrollBackward else listState.canScrollForward
                                    if (canScroll) {
                                        pageEdgeScrollScope.launch {
                                            listState.scrollBy(delta.toFloat())
                                        }
                                    }
                                    canScroll
                                },
                                onAdd = {
                                    viewModel.addSoundboardItem(targetIndex)
                                    toast(context, "已新增音效条目")
                                },
                                onItemsChanged = { reordered -> viewModel.setSoundboardItems(targetIndex, reordered) },
                                onItemChanged = { itemIndex, updated ->
                                    viewModel.updateSoundboardItem(targetIndex, itemIndex) { updated }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundboardItemsRecyclerCard(
    viewModel: MainViewModel,
    state: UiState,
    groupIndex: Int,
    items: List<SoundboardItem>,
    parentEdgeScrollBy: ((Int) -> Boolean)? = null,
    onAdd: () -> Unit,
    onItemsChanged: (List<SoundboardItem>) -> Unit,
    onItemChanged: (Int, SoundboardItem) -> Unit
) {
    val context = LocalContext.current
    var editTargetIndex by remember(items) { mutableStateOf<Int?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editWakeWord by remember { mutableStateOf("") }
    var audioTargetIndex by remember { mutableStateOf<Int?>(null) }
    var clipSourceUri by remember { mutableStateOf<Uri?>(null) }
    var showBuiltinAudioPicker by remember { mutableStateOf(false) }
    var showBuiltinBatchAudioPicker by remember { mutableStateOf(false) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            clipSourceUri = uri
        } else {
            audioTargetIndex = null
            toast(context, "未选择音频")
        }
    }
    val batchAudioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importSoundboardAudioFiles(groupIndex, uris)
        } else {
            toast(context, "未选择音频")
        }
    }

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
                Md2CardTitleText("音效条目", modifier = Modifier.weight(1f))
                Md2TextButton(onClick = {
                    if (state.useBuiltinFileManager) {
                        showBuiltinBatchAudioPicker = true
                    } else {
                        batchAudioPicker.launch("audio/*")
                    }
                }) {
                    MsIcon("queue_music", contentDescription = "批量导入")
                    Spacer(Modifier.width(4.dp))
                    Text("批量导入")
                }
                Md2TextButton(onClick = onAdd) {
                    MsIcon("add", contentDescription = "新增音效")
                    Spacer(Modifier.width(4.dp))
                    Text("新增")
                }
            }
            SoundboardItemsRecyclerList(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 92.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                items = items,
                onItemsChanged = onItemsChanged,
                onEditRequested = { index, item ->
                    editTargetIndex = index
                    editTitle = item.title
                    editWakeWord = item.wakeWord
                },
                onAudioRequested = { index ->
                    audioTargetIndex = index
                    if (state.useBuiltinFileManager) {
                        showBuiltinAudioPicker = true
                    } else {
                        audioPicker.launch("audio/*")
                    }
                },
                parentEdgeScrollBy = parentEdgeScrollBy
            )
        }
    }

    if (showBuiltinAudioPicker) {
        BuiltinFilePickerDialog(
            title = "选择音效音频",
            allowedExtensions = SoundboardAudioFileExtensions,
            onDismiss = {
                showBuiltinAudioPicker = false
                audioTargetIndex = null
            },
            onPicked = { uri ->
                showBuiltinAudioPicker = false
                clipSourceUri = uri
            },
            onOpenSystemPicker = {
                showBuiltinAudioPicker = false
                audioPicker.launch("audio/*")
            }
        )
    }

    if (showBuiltinBatchAudioPicker) {
        BuiltinFilePickerDialog(
            title = "批量导入音效音频",
            allowedExtensions = SoundboardAudioFileExtensions,
            multiSelect = true,
            onDismiss = { showBuiltinBatchAudioPicker = false },
            onPicked = { uri ->
                showBuiltinBatchAudioPicker = false
                viewModel.importSoundboardAudioFiles(groupIndex, listOf(uri))
            },
            onPickedMultiple = { uris ->
                showBuiltinBatchAudioPicker = false
                viewModel.importSoundboardAudioFiles(groupIndex, uris)
            },
            onOpenSystemPickerMultiple = {
                showBuiltinBatchAudioPicker = false
                batchAudioPicker.launch("audio/*")
            }
        )
    }

    val clipUri = clipSourceUri
    val targetIndex = audioTargetIndex
    if (clipUri != null && targetIndex != null && targetIndex in items.indices) {
        SoundboardAudioClipDialog(
            uri = clipUri,
            title = items[targetIndex].title.ifBlank { "音效" },
            onDismiss = {
                clipSourceUri = null
                audioTargetIndex = null
            },
            onImport = { startMs, endMs ->
                viewModel.importSoundboardAudioClip(groupIndex, targetIndex, clipUri, startMs, endMs)
                clipSourceUri = null
                audioTargetIndex = null
            }
        )
    }

    val editingIndex = editTargetIndex
    if (editingIndex != null && editingIndex in items.indices) {
        AlertDialog(
            onDismissRequest = { editTargetIndex = null },
            title = { Text("编辑音效条目") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("条目名") },
                        singleLine = true,
                        shape = RoundedCornerShape(UiTokens.Radius)
                    )
                    OutlinedTextField(
                        value = editWakeWord,
                        onValueChange = { editWakeWord = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("唤醒词") },
                        singleLine = true,
                        shape = RoundedCornerShape(UiTokens.Radius)
                    )
                }
            },
            confirmButton = {
                Md2TextButton(onClick = {
                    val idx = editTargetIndex
                    if (idx != null && idx in items.indices) {
                        onItemChanged(
                            idx,
                            items[idx].copy(
                                title = editTitle.trim(),
                                wakeWord = editWakeWord.trim()
                            )
                        )
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
@OptIn(ExperimentalMaterialApi::class)
private fun SoundboardAudioClipDialog(
    uri: Uri,
    title: String,
    onDismiss: () -> Unit,
    onImport: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    val durationMs by produceState(initialValue = 0L, uri) {
        value = withContext(Dispatchers.IO) { SoundboardPresetIo.readDurationMs(context, uri) }
    }
    val durationForUi = durationMs.coerceAtLeast(1000L)
    var startMs by remember(uri, durationForUi) { mutableLongStateOf(0L) }
    var endMs by remember(uri, durationForUi) { mutableLongStateOf(durationForUi) }
    var player by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(uri) { mutableStateOf(false) }
    var previewMs by remember(uri) { mutableLongStateOf(0L) }

    fun stopPreview() {
        playing = false
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        previewMs = startMs
    }

    fun startPreview() {
        stopPreview()
        runCatching {
            MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                seekTo(startMs.toInt())
                start()
                player = this
                playing = true
                previewMs = startMs
            }
        }.onFailure {
            stopPreview()
            toast(context, "音频预览失败")
        }
    }

    LaunchedEffect(playing, startMs, endMs) {
        if (!playing) return@LaunchedEffect
        while (playing) {
            val current = runCatching { player?.currentPosition?.toLong() ?: startMs }.getOrDefault(startMs)
            previewMs = current.coerceIn(startMs, endMs)
            if (current >= endMs) {
                stopPreview()
                break
            }
            delay(48L)
        }
    }
    DisposableEffect(uri) {
        onDispose { stopPreview() }
    }

    AlertDialog(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        title = { Text("音频剪辑") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = ((previewMs - startMs).toFloat() / (endMs - startMs).coerceAtLeast(1L).toFloat())
                        .coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
                RangeSlider(
                    value = startMs.toFloat()..endMs.toFloat(),
                    onValueChange = { range ->
                        startMs = range.start.toLong().coerceIn(0L, durationForUi)
                        endMs = range.endInclusive.toLong().coerceIn((startMs + 100L).coerceAtMost(durationForUi), durationForUi)
                        previewMs = startMs
                        stopPreview()
                    },
                    valueRange = 0f..durationForUi.toFloat()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("起始：${formatDurationMs(startMs)}", style = MaterialTheme.typography.bodySmall)
                        Text("结束：${formatDurationMs(endMs)}", style = MaterialTheme.typography.bodySmall)
                        Text("长度：${formatDurationMs(durationMs)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Md2TextButton(onClick = { if (playing) stopPreview() else startPreview() }) {
                        MsIcon(if (playing) "pause" else "play_arrow", contentDescription = "预览范围")
                        Spacer(Modifier.width(4.dp))
                        Text(if (playing) "暂停" else "预览")
                    }
                }
            }
        },
        confirmButton = {
            Md2TextButton(onClick = {
                stopPreview()
                onImport(startMs, endMs)
            }) {
                Text("导入")
            }
        },
        dismissButton = {
            Md2TextButton(onClick = {
                stopPreview()
                onDismiss()
            }) {
                Text("取消")
            }
        }
    )
}

private fun formatDurationMs(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSec = safe / 1000L
    val min = totalSec / 60L
    val sec = totalSec % 60L
    val frac = (safe % 1000L) / 100L
    return "%d:%02d.%d".format(Locale.US, min, sec, frac)
}

@Composable
private fun SoundboardItemsRecyclerList(
    modifier: Modifier = Modifier,
    items: List<SoundboardItem>,
    onItemsChanged: (List<SoundboardItem>) -> Unit,
    onEditRequested: (Int, SoundboardItem) -> Unit,
    onAudioRequested: (Int) -> Unit,
    parentEdgeScrollBy: ((Int) -> Boolean)? = null
) {
    val parentComposition = rememberCompositionContext()
    val onItemsChangedState = rememberUpdatedState(onItemsChanged)
    val onEditRequestedState = rememberUpdatedState(onEditRequested)
    val onAudioRequestedState = rememberUpdatedState(onAudioRequested)

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
            val adapter = SoundboardItemRecyclerAdapter(
                parentComposition = parentComposition,
                onItemsChanged = { changed -> onItemsChangedState.value(changed) },
                onEditRequested = { index, item -> onEditRequestedState.value(index, item) },
                onAudioRequested = { index -> onAudioRequestedState.value(index) }
            )
            recycler.adapter = adapter
            val touchCallback = object : ItemTouchHelper.Callback() {
                private var moved = false
                private val edgeAutoScroller = DragEdgeAutoScroller()

                override fun isLongPressDragEnabled(): Boolean = false
                override fun isItemViewSwipeEnabled(): Boolean = false
                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val ok = adapter.move(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                    moved = moved || ok
                    return ok
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun onChildDraw(
                    c: android.graphics.Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                        edgeAutoScroller.update(recyclerView, viewHolder.itemView, dY, parentEdgeScrollBy)
                    } else {
                        edgeAutoScroller.stop()
                    }
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    edgeAutoScroller.stop()
                    super.clearView(recyclerView, viewHolder)
                    adapter.isDragging = false
                    adapter.clearDraggingItem()
                    if (moved) {
                        onItemsChangedState.value(adapter.snapshotItems())
                        moved = false
                    }
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    adapter.isDragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                        adapter.setDraggingPosition(viewHolder.bindingAdapterPosition)
                    } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        edgeAutoScroller.stop()
                        adapter.clearDraggingItem()
                    }
                }
            }
            val touchHelper = ItemTouchHelper(touchCallback)
            touchHelper.attachToRecyclerView(recycler)
            adapter.onStartDrag = { vh -> touchHelper.startDrag(vh) }
            recycler
        },
        update = { recycler ->
            val adapter = recycler.adapter as? SoundboardItemRecyclerAdapter ?: return@AndroidView
            adapter.submitFromState(items)
        }
    )
}

private data class SoundboardEditableItem(
    val id: Long,
    var item: SoundboardItem
)

private class SoundboardItemRecyclerAdapter(
    private val parentComposition: CompositionContext,
    private val onItemsChanged: (List<SoundboardItem>) -> Unit,
    private val onEditRequested: (Int, SoundboardItem) -> Unit,
    private val onAudioRequested: (Int) -> Unit
) : RecyclerView.Adapter<SoundboardItemRecyclerAdapter.ItemViewHolder>() {
    private val items = mutableListOf<SoundboardEditableItem>()
    var isDragging: Boolean = false
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
    private var draggingItemId: Long? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = items[position].id
    override fun getItemCount(): Int = items.size

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

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val row = items[position].item
        holder.bind(
            item = row,
            isDragged = draggingItemId == row.id,
            canDelete = true,
            onDelete = {
                val idx = holder.bindingAdapterPosition
                if (idx in items.indices) {
                    items.removeAt(idx)
                    notifyItemRemoved(idx)
                    onItemsChanged(snapshotItems())
                }
            },
            onEdit = {
                val idx = holder.bindingAdapterPosition
                if (idx in items.indices) onEditRequested(idx, items[idx].item)
            },
            onAudio = {
                val idx = holder.bindingAdapterPosition
                if (idx in items.indices) onAudioRequested(idx)
            },
            onStartDrag = {
                if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onStartDrag?.invoke(holder)
                }
            }
        )
    }

    fun submitFromState(newItems: List<SoundboardItem>) {
        if (isDragging) return
        items.clear()
        items.addAll(newItems.map { SoundboardEditableItem(id = it.id, item = it) })
        notifyDataSetChanged()
    }

    fun move(from: Int, to: Int): Boolean {
        if (from !in items.indices || to !in items.indices || from == to) return false
        val moved = items.removeAt(from)
        items.add(to, moved)
        notifyItemMoved(from, to)
        return true
    }

    fun snapshotItems(): List<SoundboardItem> = items.map { it.item }

    fun setDraggingPosition(position: Int) {
        draggingItemId = items.getOrNull(position)?.id
        notifyDataSetChanged()
    }

    fun clearDraggingItem() {
        draggingItemId = null
        notifyDataSetChanged()
    }

    class ItemViewHolder(
        private val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView) {
        fun bind(
            item: SoundboardItem,
            isDragged: Boolean,
            canDelete: Boolean,
            onDelete: () -> Unit,
            onEdit: () -> Unit,
            onAudio: () -> Unit,
            onStartDrag: () -> Unit
        ) {
            composeView.setContent {
                KigttsFontScaleProvider {
                    SoundboardEditableRow(
                        item = item,
                        isDragged = isDragged,
                        canDelete = canDelete,
                        onDelete = onDelete,
                        onEdit = onEdit,
                        onAudio = onAudio,
                        onStartDrag = onStartDrag
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun SoundboardEditableRow(
    item: SoundboardItem,
    isDragged: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onAudio: () -> Unit,
    onStartDrag: () -> Unit
) {
    val rowElevation by animateDpAsState(
        targetValue = if (isDragged) 10.dp else 0.dp,
        animationSpec = tween(
            durationMillis = if (isDragged) 120 else 160,
            easing = FastOutSlowInEasing
        ),
        label = "soundboard_item_elevation"
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "未命名音效" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                val subtitle = buildList {
                    if (item.wakeWord.isNotBlank()) add("唤醒词：${item.wakeWord}")
                    if (item.audioPath.isNotBlank()) add(File(item.audioPath).name)
                }.joinToString(" · ").ifBlank { "未选择音频" }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Md2IconButton(
                icon = "folder_open",
                contentDescription = "选择音频",
                onClick = onAudio
            )
            Md2IconButton(
                icon = "edit",
                contentDescription = "编辑条目",
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
                contentDescription = "删除音效",
                onClick = onDelete,
                enabled = canDelete
            )
        }
    }
}

private const val QUICK_CARD_ASPECT_PORTRAIT = 9f / 16f
private const val QUICK_CARD_ASPECT_LANDSCAPE = 16f / 9f
private const val QUICK_CARD_LANDSCAPE_CARD_WIDTH_FRACTION = 0.94f
private const val QUICK_CARD_CONTENT_ASPECT_PORTRAIT = QUICK_CARD_ASPECT_PORTRAIT
private const val QUICK_CARD_CONTENT_ASPECT_LANDSCAPE = QUICK_CARD_ASPECT_LANDSCAPE

private fun quickCardDisplayAspect(landscape: Boolean): Float =
    if (landscape) QUICK_CARD_ASPECT_LANDSCAPE else QUICK_CARD_ASPECT_PORTRAIT

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
        type = QuickCardType.Text,
        title = title.trim(),
        note = note.trim(),
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
    onTopBarActionsChange: (LogTopBarActions?) -> Unit,
    onOpenRecognitionResourceSources: () -> Unit,
    onPickRecognitionResourcePackage: () -> Unit,
    onDownloadRecognitionResources: () -> Unit,
    onOpenKokoroSources: () -> Unit,
    onPickKokoroVoicePackage: () -> Unit,
    onDownloadKokoroVoice: () -> Unit,
    onOpenKokoroVoiceSettings: () -> Unit
) {
    fun isSettingsSubPage(route: String?): Boolean = route != null && route != SettingsRoutes.Main
    NavHost(
        navController = navController,
        startDestination = SettingsRoutes.Main,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            if (initialState.destination.route == SettingsRoutes.Main &&
                isSettingsSubPage(targetState.destination.route)
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
                isSettingsSubPage(targetState.destination.route)
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
            if (isSettingsSubPage(initialState.destination.route) &&
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
            if (isSettingsSubPage(initialState.destination.route) &&
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
            SettingsScreen(
                viewModel = viewModel,
                state = state,
                onOpenLicenses = {
                    navController.navigate(SettingsRoutes.Licenses) { launchSingleTop = true }
                },
                onOpenPrivacy = {
                    navController.navigate(SettingsRoutes.Privacy) { launchSingleTop = true }
                },
                onOpenRecognitionResourceSources = onOpenRecognitionResourceSources,
                onPickRecognitionResourcePackage = onPickRecognitionResourcePackage,
                onDownloadRecognitionResources = onDownloadRecognitionResources,
                onOpenKokoroSources = onOpenKokoroSources,
                onPickKokoroVoicePackage = onPickKokoroVoicePackage,
                onDownloadKokoroVoice = onDownloadKokoroVoice,
                onOpenKokoroVoiceSettings = onOpenKokoroVoiceSettings
            )
        }
        composable(SettingsRoutes.Log) {
            LogScreen(onTopBarActionsChange = onTopBarActionsChange)
        }
        composable(SettingsRoutes.Licenses) {
            LegalDocumentScreen(
                assetPath = "legal/open_source_licenses.md"
            )
        }
        composable(SettingsRoutes.Privacy) {
            LegalDocumentScreen(
                assetPath = "legal/privacy_policy.md"
            )
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
        modifier = pttModifier.mdCenteredShadow(shape = CircleShape, shadowStyle = MdFabShadowStyle),
        interactionSource = pttInteractionSource,
        backgroundColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
fun QuickSubtitleScreen(
    viewModel: MainViewModel,
    state: UiState,
    onToggleMic: () -> Unit,
    onPushToTalkPressStart: () -> Unit,
    onPushToTalkPressEnd: (PttConfirmReleaseAction) -> Unit,
    pttConfirmOwnedByMainPanel: Boolean,
    onFloatingInputPreviewChange: (QuickSubtitleFloatingInputPreviewState?) -> Unit = {},
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
    val quickItemsScrollState = rememberScrollState()
    val subtitleText = viewModel.quickSubtitleCurrentText
    val subtitleSize = viewModel.quickSubtitleFontSizeSp
    val subtitleBold = viewModel.quickSubtitleBold
    val subtitleCentered = viewModel.quickSubtitleCentered
    val subtitleRotated180 = viewModel.quickSubtitleRotated180
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
    val quickSubtitleAutoFit = state.quickSubtitleAutoFit
    val quickSubtitleContentRevision = viewModel.quickSubtitleContentRevision
    val useCompactQuickTextControls = state.quickSubtitleCompactControls && !isLandscape
    val showQuickSubtitleActionButtons = viewModel.quickSubtitleShowActionButtons
    val density = LocalDensity.current
    val actionPanelToggleIcon =
        if (showQuickSubtitleActionButtons) {
            "search"
        } else if (isLandscape) {
            "more_vert"
        } else {
            "more_horiz"
        }
    val copySubtitleText = {
        val content = subtitleText.trim()
        if (content.isNotEmpty()) {
            clipboard.setText(AnnotatedString(content))
            toast(context, "已复制")
        }
    }
    val addCurrentTextToQuickItems: (Int) -> Unit = { groupIndex ->
        viewModel.addQuickSubtitleItem(groupIndex = groupIndex, value = subtitleText)
        toast(context, "已新增快捷文本")
    }
    val actionPanelToggleDescription =
        if (showQuickSubtitleActionButtons) "切换到字体缩放" else "切换到快捷操作"
    val portraitSubtitleControlAreaHeight = 48.dp
    val portraitSubtitleControlBaselineOffset = (-6).dp
    val compactQuickGroupSwipeThresholdPx = with(density) { 18.dp.toPx() }
    var compactQuickGroupSuppressAnimation by remember { mutableStateOf(false) }
    val currentCompactSelectedGroupIndex by rememberUpdatedState(selectedGroupIndex)
    val performKeyHaptic = rememberKigttsKeyHaptic()
    val rotatedSubtitleText: @Composable (
        text: AnnotatedString,
        color: Color,
        maxFontSizeSp: Float,
        minFontSizeSp: Float,
        lineHeightMultiplier: Float,
        modifier: Modifier,
        cursorIndex: Int?
    ) -> Unit = { text, color, maxFontSizeSp, minFontSizeSp, lineHeightMultiplier, modifier, cursorIndex ->
        Crossfade(
            targetState = subtitleRotated180,
            animationSpec = tween(160),
            label = "quick_subtitle_rotation_fade"
        ) { rotated ->
            QuickSubtitleAdaptiveText(
                text = text,
                color = color,
                textAlign = subtitleAlign,
                fontWeight = if (subtitleBold) FontWeight.Bold else FontWeight.Normal,
                maxFontSizeSp = maxFontSizeSp,
                minFontSizeSp = minFontSizeSp,
                lineHeightMultiplier = lineHeightMultiplier,
                autoFitEnabled = quickSubtitleAutoFit,
                modifier = modifier,
                contentAlignment = if (rotated) {
                    if (subtitleCentered) Alignment.BottomCenter else Alignment.BottomStart
                } else {
                    Alignment.TopStart
                },
                textRotationZ = if (rotated) 180f else 0f,
                cursorIndex = cursorIndex,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        }
    }
    val subtitleActionButtonsColumn: @Composable (Modifier) -> Unit = { modifier ->
        Column(
            modifier = modifier,
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
                icon = "swap_vert",
                contentDescription = if (subtitleRotated180) "恢复字幕方向" else "倒置字幕",
                onClick = { viewModel.updateQuickSubtitleRotated180(!subtitleRotated180) }
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
    val subtitleActionButtonsRow: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Start
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
                icon = "swap_vert",
                contentDescription = if (subtitleRotated180) "恢复字幕方向" else "倒置字幕",
                onClick = { viewModel.updateQuickSubtitleRotated180(!subtitleRotated180) }
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
    var pttDragTarget by remember { mutableStateOf(PttConfirmDragTarget.DefaultSend) }
    val showPttConfirmOverlay =
        state.pushToTalkMode &&
        state.pushToTalkConfirmInputMode &&
        state.pushToTalkPressed &&
        pttConfirmOwnedByMainPanel
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
    val pttImeBottomInset =
        if (compactModeDetectionEnabled) WindowInsets.ime.asPaddingValues().calculateBottomPadding() else 0.dp
    val pttNavBottomInset =
        if (compactModeDetectionEnabled) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp
    val pttBottomObstructionInset =
        if (pttImeBottomInset > pttNavBottomInset) pttImeBottomInset else pttNavBottomInset
    val pttImeVisible = pttImeBottomInset > 0.dp
    val pttPendingText = state.pushToTalkStreamingText.ifBlank { "正在识别..." }
    val pttTopButtonsRequiredHeight = 96.dp
    val pttTopEstimatedAvailableHeight =
        configuration.screenHeightDp.dp - pttBottomObstructionInset -
            (pttFabSize + pttFabBottomOffset + pttStatusStripBottomBleed + 72.dp)
    val compactPttSideButtonsMode =
        compactModeDetectionEnabled && (pttImeVisible || pttTopEstimatedAvailableHeight < pttTopButtonsRequiredHeight)
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
    var inputFieldFocused by remember { mutableStateOf(false) }
    var keyboardSeenWhileInputFocused by remember { mutableStateOf(false) }
    var bottomInputBarHeightPx by remember { mutableIntStateOf(0) }
    var inputPreviewBlockedRevision by remember { mutableLongStateOf(Long.MIN_VALUE) }
    val imeBottomInset = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val keyboardVisible = imeBottomInset > 0.dp
    val bottomInputBarHeight = with(density) { bottomInputBarHeightPx.toDp() }
    val inputTextHasContent = inputFieldValue.text.isNotEmpty()
    LaunchedEffect(quickSubtitleContentRevision) {
        if (quickSubtitleContentRevision > 0L && inputTextHasContent) {
            inputPreviewBlockedRevision = quickSubtitleContentRevision
        }
    }
    LaunchedEffect(inputFieldValue.text) {
        if (inputFieldValue.text.isNotEmpty()) {
            inputPreviewBlockedRevision = Long.MIN_VALUE
        }
    }
    val inputPreviewAllowed =
        inputTextHasContent && inputPreviewBlockedRevision != quickSubtitleContentRevision
    val persistentInputPreviewActive =
        state.quickSubtitleKeepInputPreview && inputPreviewAllowed && !inputFieldFocused
    val editingInputPreviewActive = inputPreviewAllowed && inputFieldFocused
    val screenLongSideDp = maxOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val screenShortSideDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val portraitKeyboardAvailableHeight =
        (configuration.screenHeightDp.dp - imeBottomInset - bottomInputBarHeight - 24.dp)
            .coerceAtLeast(0.dp)
    val phoneUa =
        screenShortSideDp < 600 ||
        screenLongSideDp < 900 ||
        (!isLandscape && keyboardVisible && portraitKeyboardAvailableHeight < 620.dp)
    val landscapePhoneInputPreviewMode = isLandscape && phoneUa && configuration.screenHeightDp < 500
    val portraitPhoneKeyboardInputMode = !isLandscape && phoneUa && keyboardVisible && inputFieldFocused
    val floatingInputPreviewActive = editingInputPreviewActive && landscapePhoneInputPreviewMode
    val inlineInputPreviewActive =
        (editingInputPreviewActive && !landscapePhoneInputPreviewMode) ||
            (persistentInputPreviewActive && !floatingInputPreviewActive)
    val inputPreviewCursorIndex = inputFieldValue.selection.start.coerceIn(0, inputFieldValue.text.length)
    val inputPreviewText = remember(inputFieldValue.text) { AnnotatedString(inputFieldValue.text) }
    val displayedSubtitleText = if (inlineInputPreviewActive) inputPreviewText else AnnotatedString(subtitleText)
    val shouldHideSubtitleControlsForInput =
        !floatingInputPreviewActive && (editingInputPreviewActive && phoneUa || portraitPhoneKeyboardInputMode)
    val subtitleControlsVisible = floatingInputPreviewActive || !shouldHideSubtitleControlsForInput
    LaunchedEffect(inputText) {
        if (inputText != inputFieldValue.text) {
            inputFieldValue = TextFieldValue(
                text = inputText,
                selection = TextRange(inputText.length)
            )
        }
    }
    LaunchedEffect(inputFieldFocused) {
        if (!inputFieldFocused) {
            keyboardSeenWhileInputFocused = false
        }
    }
    LaunchedEffect(keyboardVisible, inputFieldFocused, keyboardSeenWhileInputFocused) {
        if (!inputFieldFocused) return@LaunchedEffect
        if (keyboardVisible) {
            keyboardSeenWhileInputFocused = true
        } else if (keyboardSeenWhileInputFocused) {
            focusManager.clearFocus(force = true)
            inputFieldFocused = false
            keyboardSeenWhileInputFocused = false
        }
    }
    val hasVoice = state.voiceDir != null
    val statusBarInsetTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues()
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
    val quickSubtitleBottomBlankBase = if (isLandscape) {
        UiTokens.PageBottomBlank - 12.dp + navBarsBottomInset
    } else {
        UiTokens.PageBottomBlank + 50.dp + navBarsBottomInset
    }
    val keyboardRaisedBottomBlank = imeBottomInset + bottomInputBarHeight + 8.dp
    val quickSubtitleBottomBlankTarget = if (
        keyboardVisible &&
        keyboardRaisedBottomBlank > quickSubtitleBottomBlankBase
    ) {
        keyboardRaisedBottomBlank
    } else {
        quickSubtitleBottomBlankBase
    }
    val quickSubtitleBottomBlank by animateDpAsState(
        targetValue = quickSubtitleBottomBlankTarget,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "quick_subtitle_bottom_blank"
    )
    LaunchedEffect(
        floatingInputPreviewActive,
        inputPreviewText,
        inputPreviewCursorIndex,
        imeBottomInset,
        bottomInputBarHeight
    ) {
        onFloatingInputPreviewChange(
            if (floatingInputPreviewActive) {
                QuickSubtitleFloatingInputPreviewState(
                    text = inputPreviewText,
                    cursorIndex = inputPreviewCursorIndex,
                    bottomPadding = imeBottomInset + bottomInputBarHeight + 8.dp
                )
            } else {
                null
            }
        )
    }
    DisposableEffect(Unit) {
        onDispose { onFloatingInputPreviewChange(null) }
    }
    val subtitleDisplayContent: @Composable (Boolean, AnnotatedString, Int?, Modifier) -> Unit =
        { preview, displayText, cursorIndex, modifier ->
        AnimatedContent(
            targetState = Triple(preview, displayText, cursorIndex),
            transitionSpec = {
                val previewTextEditTransition = initialState.first && targetState.first
                ContentTransform(
                    targetContentEnter = if (previewTextEditTransition) {
                        fadeIn(initialAlpha = 0.45f, animationSpec = tween(140))
                    } else {
                        fadeIn(animationSpec = tween(180)) +
                            slideInVertically(
                                initialOffsetY = { full -> full / 8 },
                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                        )
                    },
                    initialContentExit = if (previewTextEditTransition) {
                        fadeOut(targetAlpha = 0.45f, animationSpec = tween(160))
                    } else {
                        fadeOut(animationSpec = tween(120))
                    },
                    sizeTransform = null
                )
            },
            label = "quick_subtitle_display_text_change"
        ) { (preview, text, cursorIndex) ->
            val textColor = when {
                preview -> MaterialTheme.colorScheme.onSurface
                text.text == QUICK_SUBTITLE_CLEARED_HINT -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.onSurface
            }
            rotatedSubtitleText(
                text,
                textColor,
                subtitleSize,
                14f,
                1.15f,
                modifier,
                if (preview) cursorIndex else null
            )
        }
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
                            modifier = Modifier
                                .fillMaxSize()
                                .mdCenteredShadow(
                                    shape = RoundedCornerShape(UiTokens.Radius),
                                    shadowStyle = MdCardShadowStyle
                                ),
                            shape = RoundedCornerShape(UiTokens.Radius),
                            backgroundColor = md2ElevatedCardContainerColor(UiTokens.CardElevation),
                            elevation = 0.dp
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
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        subtitleDisplayContent(
                                            inlineInputPreviewActive,
                                            displayedSubtitleText,
                                            if (editingInputPreviewActive && inlineInputPreviewActive) inputPreviewCursorIndex else null,
                                            Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                AnimatedVisibility(
                                    visible = subtitleControlsVisible,
                                    enter = fadeIn(animationSpec = tween(140)) + expandHorizontally(
                                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                                    ),
                                    exit = fadeOut(animationSpec = tween(120)) + shrinkHorizontally(
                                        animationSpec = tween(160, easing = FastOutSlowInEasing)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .wrapContentWidth()
                                            .fillMaxHeight(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(40.dp)
                                                .fillMaxHeight(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                            ) {
                                                Crossfade(
                                                    targetState = showQuickSubtitleActionButtons,
                                                    animationSpec = tween(180),
                                                    label = "quick_subtitle_controls_landscape"
                                                ) { showButtons ->
                                                    if (showButtons) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize(),
                                                            contentAlignment = Alignment.TopCenter
                                                        ) {
                                                            subtitleActionButtonsColumn(
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .verticalScroll(rememberScrollState())
                                                                    .padding(top = 4.dp, bottom = 4.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxSize(),
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
                                            Md2IconButton(
                                                icon = actionPanelToggleIcon,
                                                contentDescription = actionPanelToggleDescription,
                                                onClick = {
                                                    viewModel.updateQuickSubtitleShowActionButtons(!showQuickSubtitleActionButtons)
                                                }
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .mdCenteredShadow(
                                        shape = RoundedCornerShape(UiTokens.Radius),
                                        shadowStyle = MdCardShadowStyle
                                    ),
                                shape = RoundedCornerShape(UiTokens.Radius),
                                backgroundColor = md2ElevatedCardContainerColor(),
                                elevation = 0.dp
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
                                                            .mdCenteredShadow(
                                                                shape = RoundedCornerShape(UiTokens.Radius),
                                                                shadowStyle = MdCardShadowStyle
                                                            )
                                                            .clickable {
                                                                performKeyHaptic()
                                                                viewModel.submitQuickSubtitlePreset(
                                                                    text = text,
                                                                    hasVoice = hasVoice
                                                                )
                                                            },
                                                        shape = RoundedCornerShape(UiTokens.Radius),
                                                        backgroundColor = md2ElevatedCardContainerColor(UiTokens.MenuElevation),
                                                        elevation = 0.dp
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
                                                        .mdCenteredShadow(
                                                            shape = RoundedCornerShape(UiTokens.Radius),
                                                            shadowStyle = MdCardShadowStyle
                                                        )
                                                        .clickable {
                                                            performKeyHaptic()
                                                            addCurrentTextToQuickItems(groupIndex)
                                                        },
                                                    shape = RoundedCornerShape(UiTokens.Radius),
                                                    backgroundColor = md2ElevatedCardContainerColor(UiTokens.MenuElevation),
                                                    elevation = 0.dp
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
                                                        .clickable {
                                                            performKeyHaptic()
                                                            viewModel.selectQuickSubtitleGroup(index)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    MsIcon(
                                                        group.icon,
                                                        contentDescription = group.title.ifBlank { "未命名分组" }
                                                    )
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
                                                IconButton(onClick = {
                                                    performKeyHaptic()
                                                    onOpenEditor()
                                                }) {
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
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    subtitleDisplayContent(
                                        inlineInputPreviewActive,
                                        displayedSubtitleText,
                                        if (editingInputPreviewActive && inlineInputPreviewActive) inputPreviewCursorIndex else null,
                                        Modifier.fillMaxSize()
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = subtitleControlsVisible,
                                enter = fadeIn(animationSpec = tween(140)) +
                                    expandVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)),
                                exit = fadeOut(animationSpec = tween(120)) +
                                    shrinkVertically(animationSpec = tween(160, easing = FastOutSlowInEasing))
                            ) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(portraitSubtitleControlAreaHeight)
                                    ) {
                                        Crossfade(
                                            targetState = showQuickSubtitleActionButtons,
                                            animationSpec = tween(180),
                                            label = "quick_subtitle_controls_portrait"
                                        ) { showButtons ->
                                            if (showButtons) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize(),
                                                    contentAlignment = Alignment.BottomStart
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(end = 44.dp)
                                                            .offset(y = portraitSubtitleControlBaselineOffset),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        subtitleActionButtonsRow(Modifier.weight(1f))
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize(),
                                                    contentAlignment = Alignment.BottomStart
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 4.dp, end = 44.dp)
                                                            .offset(y = portraitSubtitleControlBaselineOffset),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        MsIcon("search", contentDescription = "字体大小")
                                                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                                            Slider(
                                                                value = subtitleSize,
                                                                onValueChange = { viewModel.setQuickSubtitleFontSize(it) },
                                                                valueRange = 28f..96f,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(36.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Md2IconButton(
                                            icon = actionPanelToggleIcon,
                                            contentDescription = actionPanelToggleDescription,
                                            onClick = {
                                                viewModel.updateQuickSubtitleShowActionButtons(!showQuickSubtitleActionButtons)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(y = portraitSubtitleControlBaselineOffset)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isLandscape) {
                AnimatedVisibility(
                    visible = !quickInputCollapsed && !portraitPhoneKeyboardInputMode,
                    enter = fadeIn(animationSpec = tween(140)) +
                        expandVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(120)) +
                        shrinkVertically(animationSpec = tween(160, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Md2StaggeredFloatIn(index = 1, enabled = false) {
                            if (useCompactQuickTextControls) {
                                val compactQuickTextCardColor =
                                    md2ElevatedCardContainerColor(UiTokens.CardElevation)
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 3.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(110.dp),
                                        shape = RoundedCornerShape(UiTokens.Radius),
                                        backgroundColor = compactQuickTextCardColor,
                                        elevation = UiTokens.CardElevation
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            AnimatedContent(
                                                targetState = selectedGroupIndex,
                                                transitionSpec = {
                                                    if (compactQuickGroupSuppressAnimation || groups.size <= 1) {
                                                        ContentTransform(
                                                            targetContentEnter = fadeIn(animationSpec = tween(0)),
                                                            initialContentExit = fadeOut(animationSpec = tween(0)),
                                                            sizeTransform = null
                                                        )
                                                    } else {
                                                        val forward = targetState == if (initialState < groups.lastIndex) initialState + 1 else 0
                                                        ContentTransform(
                                                            targetContentEnter = fadeIn(animationSpec = tween(200)) +
                                                                slideInVertically(
                                                                    initialOffsetY = { full -> if (forward) full / 3 else -full / 3 },
                                                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                                ),
                                                            initialContentExit = fadeOut(animationSpec = tween(170)) +
                                                                slideOutVertically(
                                                                    targetOffsetY = { full -> if (forward) -full / 4 else full / 4 },
                                                                    animationSpec = tween(210, easing = FastOutSlowInEasing)
                                                                ),
                                                            sizeTransform = null
                                                        )
                                                    }
                                                },
                                                label = "quick_subtitle_items_switch_portrait_compact"
                                            ) { groupIndex ->
                                                val animatedQuickItems = groups.getOrNull(groupIndex)?.items.orEmpty()
                                                val compactScrollState = rememberScrollState()
                                                val compactLeftFadeAlpha by animateFloatAsState(
                                                    targetValue = if (
                                                        compactScrollState.maxValue > 0 &&
                                                        compactScrollState.value > 0
                                                    ) 1f else 0f,
                                                    animationSpec = tween(140),
                                                    label = "quick_subtitle_compact_left_fade"
                                                )
                                                val compactRightFadeAlpha by animateFloatAsState(
                                                    targetValue = if (
                                                        compactScrollState.maxValue > 0 &&
                                                        compactScrollState.value < compactScrollState.maxValue
                                                    ) 1f else 0f,
                                                    animationSpec = tween(140),
                                                    label = "quick_subtitle_compact_right_fade"
                                                )
                                                Box(
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .horizontalScroll(compactScrollState),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Spacer(Modifier.width(10.dp))
                                                        animatedQuickItems.forEach { text ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(148.dp)
                                                                    .height(94.dp)
                                                                    .clickable {
                                                                        performKeyHaptic()
                                                                        viewModel.submitQuickSubtitlePreset(
                                                                            text = text,
                                                                            hasVoice = hasVoice,
                                                                            interruptCurrent = state.quickSubtitleInterruptQueue
                                                                        )
                                                                    }
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
                                                            Box(
                                                                modifier = Modifier
                                                                    .height(58.dp)
                                                                    .width(1.dp)
                                                                    .background(
                                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                                                    )
                                                            )
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .width(86.dp)
                                                                .height(94.dp)
                                                                .clickable {
                                                                    performKeyHaptic()
                                                                    addCurrentTextToQuickItems(groupIndex)
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            MsIcon("add", contentDescription = "添加当前文本")
                                                        }
                                                        Spacer(Modifier.width(10.dp))
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.CenterStart)
                                                            .fillMaxHeight()
                                                            .width(18.dp)
                                                            .background(
                                                                Brush.horizontalGradient(
                                                                    listOf(
                                                                        compactQuickTextCardColor,
                                                                        compactQuickTextCardColor.copy(alpha = 0f)
                                                                    )
                                                                )
                                                            )
                                                            .graphicsLayer { alpha = compactLeftFadeAlpha }
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.CenterEnd)
                                                            .fillMaxHeight()
                                                            .width(18.dp)
                                                            .background(
                                                                Brush.horizontalGradient(
                                                                    listOf(
                                                                        compactQuickTextCardColor.copy(alpha = 0f),
                                                                        compactQuickTextCardColor
                                                                    )
                                                                )
                                                            )
                                                            .graphicsLayer { alpha = compactRightFadeAlpha }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    val compactDisplayGroup = groups.getOrNull(selectedGroupIndex)
                                    Card(
                                        modifier = Modifier
                                            .width(56.dp)
                                            .height(110.dp)
                                            .pointerInput(groups.size) {
                                                var accumulatedDrag = 0f
                                                detectDragGestures(
                                                    onDragStart = {
                                                        accumulatedDrag = 0f
                                                        compactQuickGroupSuppressAnimation = true
                                                    },
                                                    onDragEnd = {
                                                        accumulatedDrag = 0f
                                                        compactQuickGroupSuppressAnimation = false
                                                    },
                                                    onDragCancel = {
                                                        accumulatedDrag = 0f
                                                        compactQuickGroupSuppressAnimation = false
                                                    }
                                                ) { change, dragAmount ->
                                                    if (groups.isEmpty()) return@detectDragGestures
                                                    change.consume()
                                                    accumulatedDrag += dragAmount.y
                                                    if (kotlin.math.abs(accumulatedDrag) >= compactQuickGroupSwipeThresholdPx) {
                                                        val target = if (accumulatedDrag > 0f) {
                                                            if (currentCompactSelectedGroupIndex > 0) currentCompactSelectedGroupIndex - 1 else groups.lastIndex
                                                        } else {
                                                            if (currentCompactSelectedGroupIndex < groups.lastIndex) currentCompactSelectedGroupIndex + 1 else 0
                                                        }
                                                        performKeyHaptic()
                                                        viewModel.selectQuickSubtitleGroup(target)
                                                        accumulatedDrag = 0f
                                                    }
                                                }
                                        },
                                        shape = RoundedCornerShape(UiTokens.Radius),
                                        backgroundColor = compactQuickTextCardColor,
                                        elevation = UiTokens.CardElevation
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Md2IconButton(
                                                icon = "keyboard_arrow_up",
                                                contentDescription = "上一分组",
                                                onClick = {
                                                    if (groups.isNotEmpty()) {
                                                        compactQuickGroupSuppressAnimation = false
                                                        val target = if (selectedGroupIndex > 0) {
                                                            selectedGroupIndex - 1
                                                        } else {
                                                            groups.lastIndex
                                                        }
                                                        viewModel.selectQuickSubtitleGroup(target)
                                                    }
                                                }
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (compactDisplayGroup != null) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        MsIcon(
                                                            compactDisplayGroup.icon,
                                                            contentDescription = compactDisplayGroup.title.ifBlank { "当前分组" }
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .width(18.dp)
                                                                .height(2.dp)
                                                                .clip(RoundedCornerShape(999.dp))
                                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                                                        )
                                                    }
                                                }
                                            }
                                            Md2IconButton(
                                                icon = "keyboard_arrow_down",
                                                contentDescription = "下一分组",
                                                onClick = {
                                                    if (groups.isNotEmpty()) {
                                                        compactQuickGroupSuppressAnimation = false
                                                        val target = if (selectedGroupIndex < groups.lastIndex) {
                                                            selectedGroupIndex + 1
                                                        } else {
                                                            0
                                                        }
                                                        viewModel.selectQuickSubtitleGroup(target)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column {
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
                                            Spacer(Modifier.width(8.dp))
                                            animatedQuickItems.forEach { text ->
                                                Card(
                                                    modifier = Modifier
                                                        .padding(vertical = 3.dp)
                                                        .width(148.dp)
                                                        .height(94.dp)
                                                        .clickable {
                                                            performKeyHaptic()
                                                            viewModel.submitQuickSubtitlePreset(
                                                                text = text,
                                                                hasVoice = hasVoice
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
                                                        performKeyHaptic()
                                                        addCurrentTextToQuickItems(groupIndex)
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
                                                        val tabBg =
                                                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
                                                        Row(
                                                            modifier = Modifier
                                                                .height(48.dp)
                                                                .clip(RoundedCornerShape(UiTokens.Radius))
                                                                .background(tabBg)
                                                                .clickable {
                                                                    performKeyHaptic()
                                                                    viewModel.selectQuickSubtitleGroup(index)
                                                                }
                                                                .padding(horizontal = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val displayTitle = group.title.ifBlank { "未命名分组" }
                                                            MsIcon(group.icon, contentDescription = displayTitle)
                                                            Text(displayTitle, maxLines = 1)
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
                                                        IconButton(onClick = {
                                                            performKeyHaptic()
                                                            onOpenEditor()
                                                        }) {
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
                                    Spacer(Modifier.height(3.dp))
                                }
                            }
                        }
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
                    .onSizeChanged { bottomInputBarHeightPx = it.height }
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
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
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { inputFieldFocused = it.isFocused },
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
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { inputFieldFocused = it.isFocused },
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
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
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
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    // Keep same visual slot as the inline landscape FAB (before the send button).
                    .padding(end = pttFabEndInset, bottom = pttFabBottomOffset)
                    .size(pttFabSize)
            } else {
                Modifier
                    .zIndex(7f)
                    .align(Alignment.BottomEnd)
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
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
                        .background(Color.Black.copy(alpha = 0.52f))
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
                        rotatedSubtitleText(
                            AnnotatedString(subtitleText),
                            subtitleTextColor,
                            (subtitleSize * 1.25f).coerceIn(36f, 140f),
                            18f,
                            1.36f,
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            null
                        )
                    }
                }
            }
        }
    }
}

private data class QuickSubtitleFitResult(
    val fontSizeSp: Float,
    val needsScroll: Boolean
)

@Composable
private fun QuickSubtitleAdaptiveText(
    text: AnnotatedString,
    color: Color,
    textAlign: TextAlign,
    fontWeight: FontWeight,
    maxFontSizeSp: Float,
    minFontSizeSp: Float,
    lineHeightMultiplier: Float,
    autoFitEnabled: Boolean,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    textRotationZ: Float = 0f,
    cursorIndex: Int? = null,
    cursorColor: Color = Color.Unspecified,
    cursorWidth: Dp = 2.5.dp
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val scrollState = rememberScrollState()
        val textMeasurer = rememberTextMeasurer()
        var textLayoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
        val cursorStrokeWidthPx = with(density) { cursorWidth.toPx() }
        val maxWidthPx = remember(maxWidth, density) { with(density) { maxWidth.roundToPx() }.coerceAtLeast(1) }
        val maxHeightPx = remember(maxHeight, density) { with(density) { maxHeight.roundToPx() }.coerceAtLeast(1) }
        val boundedMaxFont = maxFontSizeSp.coerceAtLeast(minFontSizeSp)
        val fitResult = remember(
            text,
            color,
            textAlign,
            fontWeight,
            boundedMaxFont,
            minFontSizeSp,
            lineHeightMultiplier,
            autoFitEnabled,
            maxWidthPx,
            maxHeightPx,
            density
        ) {
            if (!autoFitEnabled) {
                QuickSubtitleFitResult(fontSizeSp = boundedMaxFont, needsScroll = true)
            } else {
                fun overflows(sizeSp: Float): Boolean {
                    val lineHeightSp = (sizeSp * lineHeightMultiplier).coerceAtLeast(sizeSp)
                    val result = textMeasurer.measure(
                        text = text,
                        style = TextStyle(
                            fontWeight = fontWeight,
                            fontSize = sizeSp.sp,
                            lineHeight = lineHeightSp.sp,
                            textAlign = textAlign,
                            color = color
                        ),
                        overflow = TextOverflow.Clip,
                        softWrap = true,
                        maxLines = Int.MAX_VALUE,
                        constraints = Constraints(
                            maxWidth = maxWidthPx,
                            maxHeight = maxHeightPx
                        )
                    )
                    return result.hasVisualOverflow || result.didOverflowHeight || result.didOverflowWidth
                }

                val minSize = minFontSizeSp.coerceAtMost(boundedMaxFont)
                if (!overflows(boundedMaxFont)) {
                    QuickSubtitleFitResult(fontSizeSp = boundedMaxFont, needsScroll = false)
                } else if (overflows(minSize)) {
                    QuickSubtitleFitResult(fontSizeSp = minSize, needsScroll = true)
                } else {
                    var low = minSize
                    var high = boundedMaxFont
                    var best = minSize
                    repeat(12) {
                        val mid = (low + high) / 2f
                        if (overflows(mid)) {
                            high = mid
                        } else {
                            best = mid
                            low = mid
                        }
                    }
                    QuickSubtitleFitResult(fontSizeSp = best, needsScroll = false)
                }
            }
        }
        val contentModifier = if (fitResult.needsScroll) {
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        } else {
            Modifier.fillMaxSize()
        }
        Box(
            modifier = contentModifier,
            contentAlignment = contentAlignment
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = fontWeight,
                    fontSize = fitResult.fontSizeSp.sp,
                    lineHeight = (fitResult.fontSizeSp * lineHeightMultiplier).sp
                ),
                color = color,
                textAlign = textAlign,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        val index = cursorIndex?.coerceIn(0, text.text.length) ?: return@drawWithContent
                        if (cursorColor == Color.Unspecified) return@drawWithContent
                        val layout = textLayoutResult ?: return@drawWithContent
                        val rect = layout.getCursorRect(index)
                        val x = rect.left.coerceIn(0f, size.width)
                        drawLine(
                            color = cursorColor,
                            start = Offset(x, rect.top),
                            end = Offset(x, rect.bottom),
                            strokeWidth = cursorStrokeWidthPx
                        )
                    }
                    .then(if (textRotationZ != 0f) Modifier.graphicsLayer(rotationZ = textRotationZ) else Modifier)
            )
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
                            Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
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
                                Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
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
                                modifier = Modifier
                                    .requiredSize(72.dp)
                                    .mdCenteredShadow(
                                        shape = CircleShape,
                                        shadowStyle = MdFabShadowStyle
                                    ),
                                shape = CircleShape,
                                color = if (dragTarget == PttConfirmDragTarget.ToInput) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0xFF202124)
                                },
                                elevation = 0.dp
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
                                .requiredSize(72.dp)
                                .mdCenteredShadow(
                                    shape = CircleShape,
                                        shadowStyle = MdFabShadowStyle
                                ),
                            shape = CircleShape,
                            color = if (dragTarget == PttConfirmDragTarget.Cancel) {
                                Color(0xFFB00020)
                            } else {
                                Color(0xFF202124)
                            },
                            elevation = 0.dp
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
                                Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            } else {
                                Modifier
                            }
                        )
                        .padding(end = fabEndInset + fabSize + sideGap, bottom = fabBottomOffset)
                        .requiredSize(fabSize)
                        .mdCenteredShadow(shape = CircleShape, shadowStyle = MdFabShadowStyle),
                    shape = CircleShape,
                    color = if (dragTarget == PttConfirmDragTarget.ToInput) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(0xFF202124)
                    },
                    elevation = 0.dp
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
                            Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        } else {
                            Modifier
                        }
                )
                    .padding(end = cancelEndInset, bottom = fabBottomOffset)
                    .requiredSize(fabSize)
                    .mdCenteredShadow(shape = CircleShape, shadowStyle = MdFabShadowStyle),
                shape = CircleShape,
                color = if (dragTarget == PttConfirmDragTarget.Cancel) {
                    Color(0xFFB00020)
                } else {
                    Color(0xFF202124)
                },
                elevation = 0.dp
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
            .height(stripHeight)
            .mdCenteredShadow(
                shape = RoundedCornerShape(42.dp),
                shadowStyle = MdFabShadowStyle
            ),
        shape = RoundedCornerShape(42.dp),
        color = md2ElevatedCardContainerColor(UiTokens.FabElevation),
        elevation = 0.dp
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
    val context = LocalContext.current
    val groups = viewModel.quickSubtitleGroups
    val compactControls = viewModel.uiState.quickSubtitleCompactControls
    var selectedGroupIndex by remember(groups, viewModel.quickSubtitleSelectedGroupId) {
        mutableIntStateOf(
            viewModel.currentQuickSubtitleGroupIndex().coerceIn(0, groups.lastIndex.coerceAtLeast(0))
        )
    }
    val selectedGroup = groups.getOrNull(selectedGroupIndex)
    val iconChoices = remember { QuickSubtitleGroupIconChoices }
    val groupNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val bringIntoViewScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val groupTabsScrollState = rememberScrollState()
    val groupTabsScrollScope = rememberCoroutineScope()
    val pageEdgeScrollScope = rememberCoroutineScope()
    var pendingScrollToNewGroup by remember { mutableIntStateOf(0) }

    suspend fun scrollGroupTabsToEndWhenReady(request: Int) {
        repeat(12) {
            delay(16)
            val maxScroll = groupTabsScrollState.maxValue
            if (maxScroll > 0) {
                groupTabsScrollState.scrollTo(maxScroll)
                if (pendingScrollToNewGroup == request) pendingScrollToNewGroup = 0
                return
            }
        }
        groupTabsScrollState.scrollTo(groupTabsScrollState.maxValue)
        if (pendingScrollToNewGroup == request) pendingScrollToNewGroup = 0
    }

    LaunchedEffect(groups.size, pendingScrollToNewGroup) {
        if (pendingScrollToNewGroup <= 0 || groups.isEmpty()) return@LaunchedEffect
        scrollGroupTabsToEndWhenReady(pendingScrollToNewGroup)
    }

    CenteredPageBox(
        maxWidth = UiTokens.WideContentMaxWidth,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(
                top = UiTokens.PageTopBlank,
                bottom = UiTokens.PageBottomBlank
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "quick_subtitle_editor_settings_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(UiTokens.Radius),
                    backgroundColor = md2CardContainerColor(),
                    elevation = UiTokens.CardElevation
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Md2SettingSwitchRow(
                            title = "使用更紧凑的快捷文本控件",
                            checked = compactControls,
                            onCheckedChange = { viewModel.setQuickSubtitleCompactControls(it) },
                            supportingText = "仅影响主界面竖屏便捷字幕。开启后会压缩快捷文本区高度，并把编辑入口移到顶栏。"
                        )
                    }
                }
            }
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
                        Md2TextButton(onClick = {
                            pendingScrollToNewGroup += 1
                            viewModel.addQuickSubtitleGroup()
                            toast(context, "已新增分组")
                        }) {
                            MsIcon("add", contentDescription = "新增分组")
                            Spacer(Modifier.width(4.dp))
                            Text("新增")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(groupTabsScrollState)
                    ) {
                        Row(
                            modifier = Modifier.onSizeChanged {
                                val request = pendingScrollToNewGroup
                                if (request > 0) {
                                    groupTabsScrollScope.launch {
                                        scrollGroupTabsToEndWhenReady(request)
                                    }
                                }
                            },
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
                                    val displayTitle = group.title.ifBlank { "未命名分组" }
                                    MsIcon(group.icon, contentDescription = displayTitle)
                                    Text(displayTitle)
                                    Text("(${group.items.size})", style = MaterialTheme.typography.bodySmall)
                                }
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
                    parentEdgeScrollBy = { delta ->
                        val canScroll = if (delta < 0) listState.canScrollBackward else listState.canScrollForward
                        if (canScroll) {
                            pageEdgeScrollScope.launch {
                                listState.scrollBy(delta.toFloat())
                            }
                        }
                        canScroll
                    },
                    onAdd = {
                        viewModel.addQuickSubtitleItem(selectedGroupIndex)
                        toast(context, "已新增快捷文本")
                    },
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
}

@Composable
private fun QuickSubtitleItemsRecyclerCard(
    items: List<String>,
    parentEdgeScrollBy: ((Int) -> Boolean)? = null,
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
                },
                parentEdgeScrollBy = parentEdgeScrollBy
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
    onEditRequested: (Int, String) -> Unit,
    parentEdgeScrollBy: ((Int) -> Boolean)? = null
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
                private val edgeAutoScroller = DragEdgeAutoScroller()

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

                override fun onChildDraw(
                    c: android.graphics.Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                        edgeAutoScroller.update(recyclerView, viewHolder.itemView, dY, parentEdgeScrollBy)
                    } else {
                        edgeAutoScroller.stop()
                    }
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                        if (activeViewHolder !== viewHolder) activeViewHolder = viewHolder
                        activeViewHolder = viewHolder
                        adapter.setDraggingPosition(viewHolder.bindingAdapterPosition)
                    } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        edgeAutoScroller.stop()
                        activeViewHolder = null
                        adapter.clearDraggingItem()
                    }
                    adapter.isDragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    edgeAutoScroller.stop()
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
            text: String,
            isDragged: Boolean,
            canDelete: Boolean,
            onDelete: () -> Unit,
            onEdit: () -> Unit,
            onStartDrag: () -> Unit
        ) {
            composeView.setContent {
                KigttsFontScaleProvider {
                    QuickSubtitleEditableRow(
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
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun QuickSubtitleEditableRow(
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val overlayPermissionGranted = remember { mutableStateOf(FloatingOverlayService.canDrawOverlays(context)) }
    val accessibilityPermissionGranted =
        remember { mutableStateOf(VolumeHotkeyAccessibilityService.isEnabled(context)) }
    var pendingOverlayPermissionEnable by remember { mutableStateOf(false) }
    var hotkeyActionPickerSequence by remember { mutableStateOf<VolumeHotkeySequence?>(null) }
    var externalShortcutPickerSequence by remember { mutableStateOf<VolumeHotkeySequence?>(null) }
    var externalShortcutSearchQuery by remember { mutableStateOf("") }
    var externalShortcutChoices by remember { mutableStateOf<List<ExternalShortcutChoice>>(emptyList()) }
    var externalShortcutLoading by remember { mutableStateOf(false) }
    var accessibilityExplainDialogOpen by remember { mutableStateOf(false) }
    var pendingVolumeHotkeyEnableSequence by remember { mutableStateOf<VolumeHotkeySequence?>(null) }
    var dismissVolumeHotkeyEnableWarning by remember { mutableStateOf(false) }
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
    val accessibilitySettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val enabled = VolumeHotkeyAccessibilityService.isEnabled(context)
            accessibilityPermissionGranted.value = enabled
            if (enabled) {
                VolumeHotkeyAccessibilityGuideService.stop(context)
            }
            scope.launch {
                VolumeHotkeyService.syncWithSettings(context)
            }
        }
    val pendingAccessibilityExplainRequest = viewModel.pendingAccessibilityExplainRequest
    LaunchedEffect(pendingAccessibilityExplainRequest?.requestId) {
        val request = pendingAccessibilityExplainRequest ?: return@LaunchedEffect
        accessibilityExplainDialogOpen = true
        viewModel.consumeAccessibilityExplainRequest(request.requestId)
    }

    LaunchedEffect(Unit) {
        overlayPermissionGranted.value = FloatingOverlayService.canDrawOverlays(context)
        accessibilityPermissionGranted.value = VolumeHotkeyAccessibilityService.isEnabled(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                overlayPermissionGranted.value = FloatingOverlayService.canDrawOverlays(context)
                accessibilityPermissionGranted.value = VolumeHotkeyAccessibilityService.isEnabled(context)
                if (accessibilityPermissionGranted.value) {
                    VolumeHotkeyAccessibilityGuideService.stop(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(externalShortcutPickerSequence) {
        if (externalShortcutPickerSequence == null) return@LaunchedEffect
        externalShortcutLoading = true
        externalShortcutSearchQuery = ""
        externalShortcutChoices = withContext(Dispatchers.IO) {
            ExternalShortcutCatalog.loadAllShortcutChoices(context)
        }
        externalShortcutLoading = false
    }

    val filteredExternalShortcutChoices =
        remember(externalShortcutChoices, externalShortcutSearchQuery) {
            val query = externalShortcutSearchQuery.trim()
            if (query.isBlank()) {
                externalShortcutChoices
            } else {
                externalShortcutChoices.filter { choice ->
                    val haystack = "${choice.appLabel} ${choice.shortcutTitle} ${choice.packageName}"
                    haystack.contains(query, ignoreCase = true)
                }
            }
        }

    val hotkeyMonitorModeLabel =
        when {
            state.volumeHotkeyAccessibilityEnabled && accessibilityPermissionGranted.value ->
                "无障碍按键监听"

            state.volumeHotkeyAccessibilityEnabled ->
                "等待无障碍授权，当前暂用音量变化监听"

            else -> "系统音量变化监听"
        }
    fun openAccessibilitySettingsWithGuide() {
        viewModel.setVolumeHotkeyAccessibilityEnabled(true)
        if (overlayPermissionGranted.value) {
            VolumeHotkeyAccessibilityGuideService.start(context)
        } else {
            toast(context, "未授予悬浮窗权限，引导悬浮窗不会显示")
        }
        accessibilitySettingsLauncher.launch(
            VolumeHotkeyAccessibilityService.buildSettingsIntent()
        )
    }

    fun requestVolumeHotkeyEnabled(sequence: VolumeHotkeySequence, enabled: Boolean) {
        if (!enabled) {
            viewModel.setVolumeHotkeyEnabled(sequence, false)
            return
        }
        if (accessibilityPermissionGranted.value) {
            viewModel.setVolumeHotkeyAccessibilityEnabled(true)
            viewModel.setVolumeHotkeyEnabled(sequence, true)
            return
        }
        if (state.volumeHotkeyEnableWarningDismissed) {
            viewModel.setVolumeHotkeyEnabled(sequence, true)
            return
        }
        dismissVolumeHotkeyEnableWarning = false
        pendingVolumeHotkeyEnableSequence = sequence
    }

    fun persistVolumeHotkeyEnableWarningChoiceIfNeeded() {
        if (dismissVolumeHotkeyEnableWarning) {
            viewModel.setVolumeHotkeyEnableWarningDismissed(true)
        }
    }

    CenteredPageColumn(
        maxWidth = UiTokens.WideContentMaxWidth,
        scroll = scroll
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
                    "悬浮窗可吸附到屏幕边缘，并可在软件外直接打开快捷字幕、快捷名片、画板和音效板。",
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
                    Text("长时间不操作自动贴边")
                }
                Text(
                    "开启后，悬浮 FAB 在 3 秒无操作时会自动吸附到屏幕边缘，仅露出半边并降低透明度。",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Md2Switch(
                        checked = state.floatingOverlayShowOnLockScreen,
                        onCheckedChange = { viewModel.setFloatingOverlayShowOnLockScreen(it) }
                    )
                    Text("锁屏时显示悬浮窗")
                }
                Text(
                    "开启后会尝试让悬浮窗在锁屏界面上显示并响应操作。部分系统还需要在系统权限中允许锁屏显示或后台弹出界面。",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Md2OutlinedButton(onClick = onOpenMainSettings) {
                    Text("前往主设置页")
                }
            }
        }

        Md2StaggeredFloatIn(index = 1) {
            Md2SettingsCard(title = "音量热键") {
                Text(
                    "序列监听由独立服务处理，不挂在现有悬浮窗服务上。开启无障碍稳定监听后，会优先直接读取音量键事件；未授权时会自动回退到系统音量变化判定。",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Md2Switch(
                        checked = state.volumeHotkeyAccessibilityEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                viewModel.setVolumeHotkeyAccessibilityEnabled(false)
                                VolumeHotkeyAccessibilityGuideService.stop(context)
                                scope.launch {
                                    VolumeHotkeyService.syncWithSettings(context)
                                }
                            } else if (accessibilityPermissionGranted.value) {
                                viewModel.setVolumeHotkeyAccessibilityEnabled(true)
                                scope.launch {
                                    VolumeHotkeyService.syncWithSettings(context)
                                }
                            } else {
                                accessibilityExplainDialogOpen = true
                            }
                        }
                    )
                    Text("优先使用无障碍稳定监听")
                }
                Text(
                    "权限状态：${if (accessibilityPermissionGranted.value) "已开启" else "未开启"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "当前监听方式：$hotkeyMonitorModeLabel",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Md2OutlinedButton(
                        onClick = {
                            if (
                                state.volumeHotkeyAccessibilityEnabled &&
                                !accessibilityPermissionGranted.value
                            ) {
                                accessibilityExplainDialogOpen = true
                            } else {
                                accessibilitySettingsLauncher.launch(
                                    VolumeHotkeyAccessibilityService.buildSettingsIntent()
                                )
                            }
                        }
                    ) {
                        Text("打开无障碍设置")
                    }
                    if (!accessibilityPermissionGranted.value && overlayPermissionGranted.value) {
                        Md2TextButton(
                            onClick = { VolumeHotkeyAccessibilityGuideService.stop(context) }
                        ) {
                            Text("关闭引导悬浮窗")
                        }
                    }
                }
                Divider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                )
                Text(
                    "序列判定时间：${"%.1f".format(Locale.US, state.volumeHotkeyWindowMs / 1000f)}s",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = state.volumeHotkeyWindowMs.toFloat(),
                    onValueChange = { viewModel.setVolumeHotkeyWindowMs(it.roundToInt()) },
                    valueRange = UserPrefs.VOLUME_HOTKEY_MIN_WINDOW_MS.toFloat()..
                        UserPrefs.VOLUME_HOTKEY_MAX_WINDOW_MS.toFloat(),
                    steps = ((UserPrefs.VOLUME_HOTKEY_MAX_WINDOW_MS - UserPrefs.VOLUME_HOTKEY_MIN_WINDOW_MS) / 100) - 1,
                    colors = SliderDefaults.colors(
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
                Text(
                    "默认 1.5 秒。时间越长越容易触发，但误触概率也会更高。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f)
                )
                Spacer(Modifier.height(8.dp))
                VolumeHotkeySettingRow(
                    title = "音量加后减",
                    enabled = state.volumeHotkeyUpDownEnabled,
                    actionLabel = VolumeHotkeyActions.labelOf(state.volumeHotkeyUpDownAction),
                    supportingText = "先按音量加，再在设定时间内按音量减。",
                    onEnabledChange = {
                        requestVolumeHotkeyEnabled(VolumeHotkeySequence.UpDown, it)
                    },
                    onPickAction = { hotkeyActionPickerSequence = VolumeHotkeySequence.UpDown }
                )
                Divider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                )
                VolumeHotkeySettingRow(
                    title = "音量减后加",
                    enabled = state.volumeHotkeyDownUpEnabled,
                    actionLabel = VolumeHotkeyActions.labelOf(state.volumeHotkeyDownUpAction),
                    supportingText = "先按音量减，再在设定时间内按音量加。",
                    onEnabledChange = {
                        requestVolumeHotkeyEnabled(VolumeHotkeySequence.DownUp, it)
                    },
                    onPickAction = { hotkeyActionPickerSequence = VolumeHotkeySequence.DownUp }
                )
            }
        }

        Spacer(Modifier.height(UiTokens.PageBottomBlank))
    }

    pendingVolumeHotkeyEnableSequence?.let { sequence ->
        AlertDialog(
            onDismissRequest = {
                pendingVolumeHotkeyEnableSequence = null
                dismissVolumeHotkeyEnableWarning = false
            },
            title = { Text("建议开启无障碍稳定监听") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("部分系统由于设计原因，首次点按音量键时只会弹出音量调整控件，并不会真正调整音量。")
                    Text("未开启无障碍稳定监听时，KIGTTS 只能通过系统音量数值变化判断按键序列，可能需要多按几次音量键才能触发。")
                    Text("开启无障碍稳定监听后，可以直接读取音量键事件，触发会更稳定。")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = dismissVolumeHotkeyEnableWarning,
                            onCheckedChange = { dismissVolumeHotkeyEnableWarning = it }
                        )
                        Text("下次开启不再提示")
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            pendingVolumeHotkeyEnableSequence = null
                            dismissVolumeHotkeyEnableWarning = false
                        }
                    ) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            persistVolumeHotkeyEnableWarningChoiceIfNeeded()
                            viewModel.setVolumeHotkeyEnabled(sequence, true)
                            pendingVolumeHotkeyEnableSequence = null
                            dismissVolumeHotkeyEnableWarning = false
                        }
                    ) {
                        Text("开启热键")
                    }
                    TextButton(
                        onClick = {
                            persistVolumeHotkeyEnableWarningChoiceIfNeeded()
                            viewModel.setVolumeHotkeyEnabled(sequence, true)
                            pendingVolumeHotkeyEnableSequence = null
                            dismissVolumeHotkeyEnableWarning = false
                            accessibilityExplainDialogOpen = true
                        }
                    ) {
                        Text("开启无障碍")
                    }
                }
            }
        )
    }

    hotkeyActionPickerSequence?.let { sequence ->
        AlertDialog(
            onDismissRequest = { hotkeyActionPickerSequence = null },
            title = {
                Text(
                    if (sequence == VolumeHotkeySequence.UpDown) "音量加后减"
                    else "音量减后加"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("选择这个序列触发后的功能。", style = MaterialTheme.typography.bodySmall)
                    Text("直接打开", fontWeight = FontWeight.Bold)
                    VolumeHotkeyActions.directOptions.forEach { action ->
                        TextButton(
                            onClick = {
                                viewModel.setVolumeHotkeyAction(sequence, action)
                                hotkeyActionPickerSequence = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                VolumeHotkeyActions.labelOf(action),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    Text("悬浮窗", fontWeight = FontWeight.Bold)
                    VolumeHotkeyActions.overlayOptions.forEach { action ->
                        TextButton(
                            onClick = {
                                viewModel.setVolumeHotkeyAction(sequence, action)
                                hotkeyActionPickerSequence = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                VolumeHotkeyActions.labelOf(action),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    TextButton(
                        onClick = {
                            externalShortcutSearchQuery = ""
                            externalShortcutPickerSequence = sequence
                            hotkeyActionPickerSequence = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "第三方快捷方式",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { hotkeyActionPickerSequence = null }) {
                    Text("关闭")
                }
            }
        )
    }

    if (accessibilityExplainDialogOpen) {
        AlertDialog(
            onDismissRequest = { accessibilityExplainDialogOpen = false },
            title = { Text("启用无障碍稳定监听") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("KIGTTS 会通过无障碍服务更稳定地监听音量键上下序列，用来触发你配置的音量热键功能。")
                    Text("该服务还会在你主动打开 QQ 扫一扫时读取 QQ 界面节点并执行点击手势，用于直达 QQ 扫码页。")
                    Text("除上述热键和 QQ 扫一扫直达外，不会读取其它应用内容，也不会替你点击其它流程。")
                    Text("确认后会进入系统无障碍页面，请找到“KIGTTS 音量热键辅助”并开启。若已授予悬浮窗权限，会同时显示一个可拖动的步骤提示窗。")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accessibilityExplainDialogOpen = false
                        openAccessibilitySettingsWithGuide()
                    }
                ) {
                    Text("前往无障碍设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { accessibilityExplainDialogOpen = false }) {
                    Text("取消")
                }
            }
        )
    }

    externalShortcutPickerSequence?.let { sequence ->
        AlertDialog(
            onDismissRequest = {
                externalShortcutPickerSequence = null
                externalShortcutLoading = false
            },
            title = { Text("第三方快捷方式") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "这里读取当前已加入悬浮窗启动器的应用快捷方式；内嵌列表补全关闭时仅保留运行时可查询项和微信“扫一扫”。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = externalShortcutSearchQuery,
                        onValueChange = { externalShortcutSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("搜索应用或快捷方式") }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 320.dp)
                    ) {
                        when {
                            externalShortcutLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            filteredExternalShortcutChoices.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("没有可用快捷方式", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            else -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredExternalShortcutChoices) { choice ->
                                        TextButton(
                                            onClick = {
                                                viewModel.setVolumeHotkeyAction(
                                                    sequence,
                                                    VolumeHotkeyActions.external(choice)
                                                )
                                                externalShortcutPickerSequence = null
                                                externalShortcutLoading = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                ExternalShortcutChoiceIcon(choice)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(choice.shortcutTitle)
                                                    Text(
                                                        choice.appLabel,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f)
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        externalShortcutPickerSequence = null
                        externalShortcutLoading = false
                    }
                ) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun ExternalShortcutChoiceIcon(
    choice: ExternalShortcutChoice,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f)),
        factory = { viewContext ->
            android.widget.ImageView(viewContext).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                setPadding(5, 5, 5, 5)
            }
        },
        update = { imageView ->
            val icon =
                runCatching {
                    val pm = context.packageManager
                    if (choice.className.isNotBlank()) {
                        pm.getActivityIcon(ComponentName(choice.packageName, choice.className))
                    } else {
                        pm.getApplicationIcon(choice.packageName)
                    }
                }.recoverCatching {
                    context.packageManager.getApplicationIcon(choice.packageName)
                }.getOrElse {
                    ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
                }
            imageView.setImageDrawable(icon)
        }
    )
}

@Composable
private fun VolumeHotkeySettingRow(
    title: String,
    enabled: Boolean,
    actionLabel: String,
    supportingText: String,
    onEnabledChange: (Boolean) -> Unit,
    onPickAction: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Md2Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f)
                )
            }
        }
        Text(
            "当前功能：$actionLabel",
            style = MaterialTheme.typography.bodySmall
        )
        Md2OutlinedButton(
            onClick = onPickAction,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("配置触发功能")
        }
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
    ttsDisabled: Boolean,
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
    val micIcon = when {
        ttsDisabled -> "mic_off"
        pushToTalkMode && pushToTalkPressed -> "settings_voice"
        else -> "mic"
    }
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
                    Md2AnimatedOptionMenu(
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
                    Md2AnimatedOptionMenu(
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MsIcon("mic_off", contentDescription = "禁用TTS")
                    Text("禁用TTS", style = MaterialTheme.typography.bodySmall)
                }
                Md2Switch(
                    checked = ttsDisabled,
                    onCheckedChange = { viewModel.setTtsDisabled(it) }
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
    ttsDisabled: Boolean,
    contentColor: Color,
    onToggle: () -> Unit
) {
    val micIcon = when {
        ttsDisabled -> "mic_off"
        pushToTalkMode && pushToTalkPressed -> "settings_voice"
        else -> "mic"
    }
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
    val isDark = currentAppDarkTheme()
    val state = viewModel.uiState
    val deviceRotationDegrees = when (context.display?.rotation ?: Surface.ROTATION_0) {
        Surface.ROTATION_90 -> 90f
        Surface.ROTATION_180 -> 180f
        Surface.ROTATION_270 -> 270f
        else -> 0f
    }
    val autoRotationDegrees = if (state.drawingKeepCanvasOrientationToDevice) deviceRotationDegrees else 0f
    val manualRotationDegrees = viewModel.drawingManualRotationQuarterTurns * 90f
    val rotationDegrees = ((autoRotationDegrees - manualRotationDegrees) % 360f + 360f) % 360f
    val animatedRotationDegrees by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "drawing_canvas_rotation"
    )
    val boardFillColor = if (isDark) Color(0xFF2C3237) else Color(0xFFFCFDFE)
    val currentPoints = remember { mutableStateListOf<DrawPoint>() }
    var currentStrokeEraser by remember { mutableStateOf(false) }
    var viewportScale by rememberSaveable { mutableFloatStateOf(1f) }
    var viewportPanX by rememberSaveable { mutableFloatStateOf(0f) }
    var viewportPanY by rememberSaveable { mutableFloatStateOf(0f) }
    val toolbarCollapsed = viewModel.drawingToolbarCollapsed
    val navigationBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hasPortraitThreeButtonNav = !isLandscape && navigationBarBottomInset >= 32.dp
    val portraitToolbarBottomInset = if (hasPortraitThreeButtonNav) navigationBarBottomInset else 0.dp
    val boardReserveEndTarget = if (isLandscape) {
        if (toolbarCollapsed) 76.dp else 128.dp
    } else {
        0.dp
    }
    val boardReserveBottomTarget = if (isLandscape) {
        0.dp
    } else {
        when {
            toolbarCollapsed && hasPortraitThreeButtonNav -> 76.dp
            toolbarCollapsed -> 66.dp
            hasPortraitThreeButtonNav -> 168.dp
            else -> 120.dp
        }
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
            val activeEraser = viewModel.drawEraser || currentStrokeEraser
            val activeWidth = if (activeEraser) viewModel.drawEraserSize * 5f else viewModel.drawBrushSize
            val containerW = with(density) { maxWidth.toPx() }
            val containerH = with(density) { maxHeight.toPx() }
            val cardOrigin = Offset(
                x = ((containerW - canvasW) * 0.5f).coerceAtLeast(0f),
                y = ((containerH - canvasH) * 0.5f).coerceAtLeast(0f)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(cardWidthDp, cardHeightDp)
                    .graphicsLayer {
                        scaleX = viewportScale
                        scaleY = viewportScale
                        translationX = viewportPanX
                        translationY = viewportPanY
                    }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { boardSize = it }
                        .mdCenteredShadow(
                            shape = RoundedCornerShape(UiTokens.Radius),
                            shadowStyle = MdCardShadowStyle
                        ),
                    shape = RoundedCornerShape(UiTokens.Radius),
                    backgroundColor = md2CardContainerColor(),
                    elevation = 0.dp
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        withTransform({
                            rotate(degrees = -animatedRotationDegrees, pivot = center)
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
                                    color = if (activeEraser) boardFillColor else viewModel.drawColor,
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
                        fun commitCurrentStroke() {
                            if (currentPoints.size > 1) {
                                viewModel.appendDrawingStroke(
                                    points = currentPoints.toList(),
                                    eraserOverride = currentStrokeEraser
                                )
                            }
                        }
                        fun switchCurrentStrokeEraser(useEraser: Boolean) {
                            if (useEraser == currentStrokeEraser) return
                            val anchor = currentPoints.lastOrNull()
                            commitCurrentStroke()
                            currentPoints.clear()
                            anchor?.let { currentPoints.add(it) }
                            currentStrokeEraser = useEraser
                        }

                        awaitPointerEventScope {
                            while (true) {
                                var drawingActive = false
                                var transformActive = false
                                var drawPointerId: androidx.compose.ui.input.pointer.PointerId?
                                var trackedA: androidx.compose.ui.input.pointer.PointerId? = null
                                var trackedB: androidx.compose.ui.input.pointer.PointerId? = null
                                var lastFocus = Offset.Zero
                                var lastSpan = 0f

                                val down = awaitFirstDown(requireUnconsumed = false)
                                drawPointerId = down.id
                                currentStrokeEraser = viewModel.drawEraser || down.type == PointerType.Eraser
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
                                            commitCurrentStroke()
                                        }
                                        currentPoints.clear()
                                        currentStrokeEraser = false
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
                                            if (drawingActive && currentPoints.size > 1) {
                                                commitCurrentStroke()
                                            }
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

                                    val eventEraser = viewModel.drawEraser || event.usesDrawingTemporaryEraser()
                                    if (!drawingActive) {
                                        currentStrokeEraser = eventEraser
                                        val mapped = mapPoint(one.position)
                                        if (mapped.isInsideBoard(left, top, fitW, fitH)) {
                                            currentPoints.clear()
                                            currentPoints.add(mapped.toDrawPoint(left, top, fitW, fitH))
                                            drawingActive = true
                                        }
                                    } else {
                                        switchCurrentStrokeEraser(eventEraser)
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
                .padding(end = 10.dp)
        } else {
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp + portraitToolbarBottomInset)
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
            modifier = (if (isLandscape) Modifier else Modifier.width(portraitToolbarWidth))
                .mdCenteredShadow(
                    shape = RoundedCornerShape(UiTokens.Radius),
                    shadowStyle = MdCardShadowStyle
                ),
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2ElevatedCardContainerColor(),
            elevation = 0.dp
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
            modifier = Modifier.mdCenteredShadow(
                shape = RoundedCornerShape(UiTokens.Radius),
                shadowStyle = MdCardShadowStyle
            ),
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2ElevatedCardContainerColor(),
            elevation = 0.dp
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

@OptIn(ExperimentalComposeUiApi::class)
private fun PointerEvent.usesDrawingTemporaryEraser(): Boolean {
    return changes.any { it.pressed && it.type == PointerType.Eraser } ||
        buttons.hasRawButtonMask(MotionEvent.BUTTON_STYLUS_PRIMARY) ||
        buttons.hasRawButtonMask(MotionEvent.BUTTON_STYLUS_SECONDARY)
}

private fun PointerButtons.hasRawButtonMask(mask: Int): Boolean {
    val packedValue = toString()
        .substringAfter("packedValue=", missingDelimiterValue = "")
        .substringBefore(")")
        .toIntOrNull()
        ?: return false
    return (packedValue and mask) != 0
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
fun SettingsScreen(
    viewModel: MainViewModel,
    state: UiState,
    onOpenLicenses: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenRecognitionResourceSources: () -> Unit,
    onPickRecognitionResourcePackage: () -> Unit,
    onDownloadRecognitionResources: () -> Unit,
    onOpenKokoroSources: () -> Unit,
    onPickKokoroVoicePackage: () -> Unit,
    onDownloadKokoroVoice: () -> Unit,
    onOpenKokoroVoiceSettings: () -> Unit
) {
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
    val denoiserModeOptions = listOf(
        AudioDenoiserMode.OFF to "关闭",
        AudioDenoiserMode.RNNOISE to "RNNoise 噪声抑制",
        AudioDenoiserMode.SPEEX to "Speex 噪声抑制"
    )
    val themeModeOptions = listOf(
        UserPrefs.THEME_MODE_FOLLOW_SYSTEM to "跟随系统",
        UserPrefs.THEME_MODE_LIGHT to "亮色",
        UserPrefs.THEME_MODE_DARK to "暗色"
    )
    val fontScaleBlockModeOptions = listOf(
        UserPrefs.FONT_SCALE_BLOCK_NONE to "图标和字体跟随缩放",
        UserPrefs.FONT_SCALE_BLOCK_ICONS_ONLY to "仅禁用图标大小缩放",
        UserPrefs.FONT_SCALE_BLOCK_ALL to "禁用图标和字体大小缩放"
    )
    var drawerModeExpanded by remember { mutableStateOf(false) }
    var themeModeExpanded by remember { mutableStateOf(false) }
    var overlayThemeModeExpanded by remember { mutableStateOf(false) }
    var fontScaleBlockModeExpanded by remember { mutableStateOf(false) }
    var inputTypeExpanded by remember { mutableStateOf(false) }
    var outputTypeExpanded by remember { mutableStateOf(false) }
    var denoiserModeExpanded by remember { mutableStateOf(false) }
    var speechEnhancementExpanded by remember { mutableStateOf(false) }
    var vadModeExpanded by remember { mutableStateOf(false) }
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
    val drawingDirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            viewModel.setDrawingSavePathFromTreeUri(uri)
        } else {
            toast(context, "未选择目录")
        }
    }
    fun openExternalPage(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            toast(context, "打开主页失败")
        }
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
                    val collectedSamples = speakerEnrollSamples.filterNotNull()
                    if (viewModel.applySpeakerProfiles(collectedSamples)) {
                        if (speakerEnrollOpenedByToggle) {
                            viewModel.setSpeakerVerifyEnabled(true)
                        }
                        speakerEnrollSuccess = true
                        speakerEnrollStep = 4
                        speakerEnrollProgress = 1f
                        speakerEnrollMessage = "本人语音样本采集完成"
                    } else {
                        speakerEnrollSuccess = false
                        speakerEnrollStep = 4
                        speakerEnrollProgress = 0f
                        speakerEnrollMessage = "样本保存失败，请稍后重试"
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
    val selectedCategoryName = viewModel.settingsSelectedCategoryName
    val selectedCategory = remember(selectedCategoryName) { SettingsCategory.valueOf(selectedCategoryName) }
    val numberReplaceOptions = remember { listOf("不替换", "数字替换为中文字符", "数字替换为中文表达") }
    var numberReplaceExpanded by remember { mutableStateOf(false) }
    val isSystemTtsSelected = isSystemTtsVoiceDir(state.voiceDir)
    val isKokoroTtsSelected = isKokoroVoiceDir(state.voiceDir)

    LaunchedEffect(selectedCategory) {
        scroll.animateScrollTo(0)
    }

    @Composable
    fun AboutContributorItem(
        avatarRes: Int,
        name: String,
        homepage: String,
        modifier: Modifier = Modifier,
        avatarSize: Dp = 54.dp
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = avatarRes),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
            )
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                Md2IconButton(
                    icon = "open_in_new",
                    contentDescription = "打开${name}主页",
                    onClick = { openExternalPage(homepage) }
                )
            }
        }
    }

    @Composable
    fun AboutDocumentRow(
        title: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        showDivider: Boolean = true
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = true),
                        onClick = onClick
                    )
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                MsIcon("chevron_right", contentDescription = null)
            }
            if (showDivider) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            }
        }
    }

    @Composable
    fun AboutSettingsContent() {
        val configuration = LocalConfiguration.current
        val isPortrait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val packageInfo = remember(context) {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
        }
        @Suppress("DEPRECATION")
        val versionLabel = remember(packageInfo) {
            val versionName = packageInfo?.versionName ?: "未知版本"
            val versionCode = packageInfo?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    it.longVersionCode
                } else {
                    it.versionCode.toLong()
                }
            } ?: 0L
            "版本 $versionName ($versionCode)"
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Md2StaggeredFloatIn(index = 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(UiTokens.Radius),
                    backgroundColor = md2CardContainerColor(),
                    elevation = UiTokens.CardElevation
                ) {
                    val logoRes = if (currentAppDarkTheme()) R.drawable.logo_white else R.drawable.logo_black
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = logoRes),
                            contentDescription = "KIGTTS Logo",
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(50.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = versionLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Md2StaggeredFloatIn(index = 1) {
                Md2SettingsCard(title = "软件制作") {
                    if (isPortrait) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AboutContributorItem(
                                avatarRes = R.drawable.avatar_lht,
                                name = "LHT",
                                homepage = "https://space.bilibili.com/87244951",
                                modifier = Modifier.fillMaxWidth(),
                                avatarSize = 60.dp
                            )
                            AboutContributorItem(
                                avatarRes = R.drawable.avatar_yuilu,
                                name = "Yui Lu",
                                homepage = "https://space.bilibili.com/23208863",
                                modifier = Modifier.fillMaxWidth(),
                                avatarSize = 60.dp
                            )
                            AboutContributorItem(
                                avatarRes = R.drawable.avatar_huajiang,
                                name = "花酱",
                                homepage = "https://space.bilibili.com/573842321",
                                modifier = Modifier.fillMaxWidth(),
                                avatarSize = 60.dp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AboutContributorItem(
                                avatarRes = R.drawable.avatar_lht,
                                name = "LHT",
                                homepage = "https://space.bilibili.com/87244951",
                                modifier = Modifier.weight(1f),
                                avatarSize = 48.dp
                            )
                            AboutContributorItem(
                                avatarRes = R.drawable.avatar_yuilu,
                                name = "Yui Lu",
                                homepage = "https://space.bilibili.com/23208863",
                                modifier = Modifier.weight(1f),
                                avatarSize = 48.dp
                            )
                            AboutContributorItem(
                                avatarRes = R.drawable.avatar_huajiang,
                                name = "花酱",
                                homepage = "https://space.bilibili.com/573842321",
                                modifier = Modifier.weight(1f),
                                avatarSize = 48.dp
                            )
                        }
                    }
                }
            }

            Md2StaggeredFloatIn(index = 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(UiTokens.Radius),
                    backgroundColor = md2CardContainerColor(),
                    elevation = UiTokens.CardElevation
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AboutDocumentRow(
                            title = "开源许可证",
                            onClick = onOpenLicenses,
                            showDivider = false
                        )
                        AboutDocumentRow(
                            title = "隐私政策",
                            onClick = onOpenPrivacy,
                            showDivider = false
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun RecognitionSettingsContent() {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Md2StaggeredFloatIn(index = 0) {
                Md2SettingsCard(title = "语音识别资源包") {
                    Text(
                        text = state.recognitionResourceStatus,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.recognitionResourceInstalled) {
                        Text(
                            text = buildString {
                                append("当前资源：")
                                append(state.recognitionResourceName)
                                if (state.recognitionResourceVersion.isNotBlank()) {
                                    append(" / ")
                                    append(state.recognitionResourceVersion)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "资源包用于统一管理 ASR、Silero VAD、GTCRN/DPDFNet 语音增强模型；未安装时语音识别与 AI 语音增强不可用。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.recognitionResourceBusy) {
                        val progress = state.recognitionResourceProgress
                        if (progress in 0f..1f) {
                            LinearProgressIndicator(
                                progress = progress.coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            text = state.recognitionResourceProgressStage.ifBlank { "处理中" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Md2Button(
                            onClick = onDownloadRecognitionResources,
                            enabled = !state.recognitionResourceBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("下载资源包")
                        }
                        Md2OutlinedButton(
                            onClick = onPickRecognitionResourcePackage,
                            enabled = !state.recognitionResourceBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("本地安装")
                        }
                    }
                    Md2TextButton(
                        onClick = onOpenRecognitionResourceSources,
                        enabled = !state.recognitionResourceBusy
                    ) {
                        Text("管理下载源")
                    }
                }
            }

            Md2StaggeredFloatIn(index = 1) {
                Md2SettingsCard(title = "识别与转换") {
                    Md2SettingSwitchRow(
                        title = "识别结果自动上屏大字幕",
                        checked = state.asrSendToQuickSubtitle,
                        onCheckedChange = { viewModel.setAsrSendToQuickSubtitle(it) },
                        supportingText = "开启后：语音识别结果会自动更新便捷字幕主文本"
                    )
                    Md2SettingSwitchRow(
                        title = "按住说话模式",
                        checked = state.pushToTalkMode,
                        onCheckedChange = { viewModel.setPushToTalkMode(it) },
                        supportingText = "开启后：实时页 FAB 改为麦克风，按下开始收音，松开停止收音。"
                    )
                    Md2SettingSwitchRow(
                        title = "允许通过快捷文本触发音效板",
                        checked = state.allowQuickTextTriggerSoundboard,
                        onCheckedChange = { viewModel.setAllowQuickTextTriggerSoundboard(it) },
                        supportingText = "关闭后：便捷字幕的快捷文本与输入框只更新字幕/TTS，不触发音效板关键词。"
                    )
                    Md2SettingSwitchRow(
                        title = "快捷文本打断当前语音",
                        checked = state.quickSubtitleInterruptQueue,
                        onCheckedChange = { viewModel.setQuickSubtitleInterruptQueue(it) },
                        supportingText = "开启后：便捷字幕和迷你便捷字幕点按快捷文本时，会打断当前朗读并优先播放新条目。"
                    )
                    Md2SettingSwitchRow(
                        title = "按下输入文本确认",
                        checked = state.pushToTalkConfirmInputMode,
                        enabled = state.pushToTalkMode,
                        onCheckedChange = { viewModel.setPushToTalkConfirmInputMode(it) },
                        supportingText = "开启后：按住说话时识别文本先显示在悬浮条中，松手可上屏；上滑可改为输入到文本框或取消发送。"
                    )
                    Md2SettingSwitchRow(
                        title = "播放时屏蔽录音",
                        checked = state.muteWhilePlaying,
                        onCheckedChange = { viewModel.setMuteWhilePlaying(it) },
                        supportingText = "开启后播放中不进行识别"
                    )
                    Text(
                        "屏蔽结束延迟：${String.format("%.1f", state.muteWhilePlayingDelaySec)}s",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = state.muteWhilePlayingDelaySec,
                        onValueChange = { viewModel.setMuteWhilePlayingDelay(it) },
                        valueRange = 0f..5f
                    )
                    Text(
                        "语音识别最低音量阈值：${state.minVolumePercent}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = state.minVolumePercent.toFloat(),
                        onValueChange = { viewModel.setMinVolumePercent(it.toInt()) },
                        valueRange = 0f..100f
                    )
                    Md2SettingDropdownRow(
                        title = "AI 语音增强",
                        value = SpeechEnhancementMode.labelOf(state.speechEnhancementMode),
                        expanded = speechEnhancementExpanded,
                        onExpandedChange = { speechEnhancementExpanded = it },
                        supportingText = when (state.speechEnhancementMode) {
                            SpeechEnhancementMode.GTCRN_OFFLINE -> "在一句话结束后先增强再识别与说话人验证，最稳但会增加少量延迟。"
                            SpeechEnhancementMode.GTCRN_STREAMING -> "边收音边增强，适合实时字幕，资源占用较低。"
                            SpeechEnhancementMode.DPDFNET2_STREAMING -> "流式增强，降噪更强，资源占用中等。"
                            SpeechEnhancementMode.DPDFNET4_STREAMING -> "流式增强里效果更强，但更吃性能和电量。"
                            else -> "关闭后仅使用原有降噪与 VAD。"
                        }
                    ) {
                        SpeechEnhancementMode.options.forEach { (value, label) ->
                            M2DropdownMenuItem(
                                onClick = {
                                    speechEnhancementExpanded = false
                                    viewModel.setSpeechEnhancementMode(value)
                                }
                            ) { Text(label) }
                        }
                    }
                    val currentVadMode = VadMode.fromFlags(state.classicVadEnabled, state.sileroVadEnabled)
                    Md2SettingDropdownRow(
                        title = "语音活动检测",
                        value = VadMode.labelOf(currentVadMode),
                        expanded = vadModeExpanded,
                        onExpandedChange = { vadModeExpanded = it },
                        supportingText = when (currentVadMode) {
                            VadMode.SILERO -> "仅使用 SileroVAD 做语音活动检测，对轻声和彩噪更稳。"
                            VadMode.HYBRID -> "同时使用阈值式VAD和 SileroVAD，兼顾静音门限与模型断句。"
                            else -> "仅使用现有音量阈值、静音时长和 voiced ratio 断句。"
                        }
                    ) {
                        VadMode.options.forEach { (value, label) ->
                            M2DropdownMenuItem(
                                onClick = {
                                    vadModeExpanded = false
                                    viewModel.setVadMode(value)
                                }
                            ) { Text(label) }
                        }
                    }
                    val sileroVadControlsEnabled = state.sileroVadEnabled
                    Text(
                        "Silero 触发阈值：${String.format("%.2f", state.sileroVadThreshold)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(
                            alpha = if (sileroVadControlsEnabled) 1f else 0.38f
                        )
                    )
                    Slider(
                        value = state.sileroVadThreshold,
                        onValueChange = { viewModel.setSileroVadThreshold(it) },
                        valueRange = UserPrefs.SILERO_VAD_MIN_THRESHOLD..UserPrefs.SILERO_VAD_MAX_THRESHOLD,
                        steps = 17,
                        enabled = sileroVadControlsEnabled,
                        colors = SliderDefaults.colors(
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )
                    Text(
                        "越低越容易触发；轻声吞首字可先试 0.35-0.45。",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(
                            alpha = if (sileroVadControlsEnabled) 0.74f else 0.38f
                        )
                    )
                    Text(
                        "Silero pre-roll：${state.sileroVadPreRollMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(
                            alpha = if (sileroVadControlsEnabled) 1f else 0.38f
                        )
                    )
                    Slider(
                        value = state.sileroVadPreRollMs.toFloat(),
                        onValueChange = { viewModel.setSileroVadPreRollMs(it.roundToInt()) },
                        valueRange = UserPrefs.SILERO_VAD_MIN_PRE_ROLL_MS.toFloat()..
                            UserPrefs.SILERO_VAD_MAX_PRE_ROLL_MS.toFloat(),
                        steps = 15,
                        enabled = sileroVadControlsEnabled,
                        colors = SliderDefaults.colors(
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )
                    Text(
                        "触发前补入一小段录音，改善模型晚触发导致的首字被吞；过大可能带入更多环境音。",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(
                            alpha = if (sileroVadControlsEnabled) 0.74f else 0.38f
                        )
                    )
                    Md2SettingDropdownRow(
                        title = "数字替换",
                        value = numberReplaceOptions.getOrElse(state.numberReplaceMode) { numberReplaceOptions[0] },
                        expanded = numberReplaceExpanded,
                        onExpandedChange = { numberReplaceExpanded = it },
                        supportingText = "示例：2000 → 二零零零 / 两千"
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
            }

            Md2StaggeredFloatIn(index = 2) {
                Md2SettingsCard(title = "说话人验证") {
                    Md2SettingSwitchRow(
                        title = "说话人验证",
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
                                speakerEnrollMessage = "请按页面引导完成本人样本采集。"
                                speakerEnrollRetryDialog = false
                                speakerEnrollOpenedByToggle = true
                                showSpeakerEnrollDialog = true
                            }
                        },
                        supportingText = "样本：${state.speakerProfiles.size}/3"
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
                                    text = profile.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Md2IconButton(
                                    icon = "delete",
                                    contentDescription = "删除样本",
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
                        valueRange = 0.05f..0.95f
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
                            speakerEnrollMessage = "请按页面引导完成本人样本采集。"
                            speakerEnrollRetryDialog = false
                            speakerEnrollOpenedByToggle = false
                            showSpeakerEnrollDialog = true
                        }) {
                            Text(if (state.speakerProfiles.isEmpty()) "采集本人样本" else "重新采集样本")
                        }
                        Md2TextButton(onClick = { viewModel.clearSpeakerProfile() }) {
                            Text("清空样本")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AudioSettingsContent() {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Md2StaggeredFloatIn(index = 0) {
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

            Md2StaggeredFloatIn(index = 1) {
                Md2SettingsCard(title = "播放与合成") {
                    Text(
                        "当前朗读后端：${when {
                            isSystemTtsSelected -> SYSTEM_TTS_DEFAULT_LABEL
                            isKokoroTtsSelected -> "Kokoro"
                            else -> "语音包"
                        }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Md2SettingSwitchRow(
                        title = "禁用TTS",
                        checked = state.ttsDisabled,
                        onCheckedChange = { viewModel.setTtsDisabled(it) },
                        supportingText = "关闭后不会发声，但仍会上屏并可继续触发音效板。"
                    )
                    Text("播放音量倍率：${state.playbackGainPercent}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = state.playbackGainPercent.toFloat(),
                        onValueChange = { viewModel.setPlaybackGainPercent(it.toInt()) },
                        valueRange = 0f..1000f
                    )
                    Text("100% 为原始音量，拖动接近 100% 时会自动吸附。", style = MaterialTheme.typography.bodySmall)
                    if (isSystemTtsSelected) {
                        Text(
                            "系统 TTS 使用设备已安装的语音引擎与音色。音色随机度等 Piper 专属参数在系统 TTS 下不生效。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Md2Button(
                            onClick = {
                                viewModel.openSystemTtsSetup(context)
                            }
                        ) {
                            Text("打开系统 TTS 设置")
                        }
                    } else if (isKokoroTtsSelected) {
                        Text(
                            "Kokoro 使用独立音色编号。Piper 专属随机度参数在 Kokoro 下不生效。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "当前声音编号：${state.kokoroSpeakerId.coerceIn(UserPrefs.KOKORO_MIN_SPEAKER_ID, UserPrefs.KOKORO_MAX_SPEAKER_ID)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Md2Button(
                            onClick = onOpenKokoroVoiceSettings,
                            enabled = state.kokoroInstalled && !state.kokoroBusy
                        ) {
                            Text("选择 Kokoro 音色")
                        }
                    } else {
                        Text("音色随机度：${String.format("%.3f", state.piperNoiseScale)}", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = state.piperNoiseScale,
                            onValueChange = { viewModel.setPiperNoiseScale(it) },
                            valueRange = 0f..2f
                        )
                    }
                    Text(
                        if (isSystemTtsSelected) {
                            "系统语速倍率（越大越慢）：${String.format("%.3f", state.piperLengthScale)}"
                        } else {
                            "语速倍率（越大越慢）：${String.format("%.3f", state.piperLengthScale)}"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = state.piperLengthScale,
                        onValueChange = { viewModel.setPiperLengthScale(it) },
                        valueRange = 0.1f..5f
                    )
                    Text(
                        if (isSystemTtsSelected) {
                            "系统 TTS 句末停顿时长：${String.format("%.2f", state.piperSentenceSilence)}s"
                        } else {
                            "句末停顿时长：${String.format("%.2f", state.piperSentenceSilence)}s"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = state.piperSentenceSilence,
                        onValueChange = { viewModel.setPiperSentenceSilence(it) },
                        valueRange = 0f..2f
                    )
                }
            }

            Md2StaggeredFloatIn(index = 2) {
                Md2SettingsCard(title = "Kokoro 离线语音") {
                    Text(
                        text = state.kokoroStatus,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Kokoro 是一套可选的离线朗读声音。安装后，你可以在“语音包”页面选择 Kokoro，并在设置或语音包页面切换不同声音用于朗读。\n\n由于资源体积较大，Kokoro 需要单独下载或从本地安装，不会直接内置在安装包中。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.kokoroBusy) {
                        val progress = state.kokoroProgress
                        if (progress in 0f..1f) {
                            LinearProgressIndicator(
                                progress = progress.coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            text = state.kokoroProgressStage.ifBlank { "处理中" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Md2Button(
                            onClick = onDownloadKokoroVoice,
                            enabled = !state.kokoroBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (state.kokoroInstalled) "重新下载" else "下载 Kokoro")
                        }
                        Md2OutlinedButton(
                            onClick = onPickKokoroVoicePackage,
                            enabled = !state.kokoroBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("本地安装")
                        }
                    }
                    Md2TextButton(
                        onClick = onOpenKokoroSources,
                        enabled = !state.kokoroBusy
                    ) {
                        Text("下载源设置")
                    }
                }
            }

            Md2StaggeredFloatIn(index = 3) {
                Md2SettingsCard(title = "回声与降噪") {
                    Md2SettingSwitchRow(
                        title = "回声抑制",
                        checked = state.echoSuppression,
                        onCheckedChange = { viewModel.setEchoSuppression(it) },
                        supportingText = "开启后使用通话录音源，可能有回声抑制/降噪效果"
                    )
                    Md2SettingSwitchRow(
                        title = "通话模式降噪",
                        checked = state.communicationMode,
                        onCheckedChange = { viewModel.setCommunicationMode(it) },
                        supportingText = "开启后切换系统通话模式并统一播放属性"
                    )
                    Md2SettingSwitchRow(
                        title = "AEC3 软件回声消除",
                        checked = state.aec3Enabled,
                        onCheckedChange = { viewModel.setAec3Enabled(it) },
                        supportingText = "需渲染参考音频，可能与系统AEC冲突"
                    )
                    Md2SettingDropdownRow(
                        title = "软件噪声抑制",
                        value = denoiserModeOptions.firstOrNull { it.first == state.denoiserMode }?.second
                            ?: denoiserModeOptions.first().second,
                        expanded = denoiserModeExpanded,
                        onExpandedChange = { denoiserModeExpanded = it },
                        supportingText = "关闭时不做软件降噪；RNNoise 更偏语音场景，Speex 更偏传统预处理。"
                    ) {
                        denoiserModeOptions.forEach { (value, label) ->
                            M2DropdownMenuItem(
                                onClick = {
                                    denoiserModeExpanded = false
                                    viewModel.setDenoiserMode(value)
                                }
                            ) { Text(label) }
                        }
                    }
                    Text("AEC3 状态：${state.aec3Status}", style = MaterialTheme.typography.bodySmall)
                    Text(state.aec3Diag, style = MaterialTheme.typography.bodySmall)
                }
            }

            Md2StaggeredFloatIn(index = 4) {
                Md2SettingsCard(title = "设备路由") {
                    Md2SettingDropdownRow(
                        title = "优先选择的音频输入设备类型",
                        value = inputTypeOptions.firstOrNull { it.first == state.preferredInputType }?.second
                            ?: inputTypeOptions.first().second,
                        expanded = inputTypeExpanded,
                        onExpandedChange = { inputTypeExpanded = it },
                        supportingText = "适配内置、USB、蓝牙、有线等输入设备"
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
                    Md2SettingDropdownRow(
                        title = "优先使用的音频输出类型",
                        value = outputTypeOptions.firstOrNull { it.first == state.preferredOutputType }?.second
                            ?: outputTypeOptions.first().second,
                        expanded = outputTypeExpanded,
                        onExpandedChange = { outputTypeExpanded = it },
                        supportingText = "适配扬声器、听筒、蓝牙、USB、有线等输出设备"
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
            }

            Md2StaggeredFloatIn(index = 4) {
                Md2SettingsCard(title = "音频测试") {
                    Text("当前状态：${state.audioTestStatus}", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = state.audioTestLevel.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.audioTestRecording) {
                            Md2Button(
                                onClick = { viewModel.stopAudioTestRecording() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("停止录音")
                            }
                        } else {
                            Md2Button(
                                onClick = { viewModel.startAudioTestRecording() },
                                modifier = Modifier.weight(1f),
                                enabled = !state.audioTestPlaying
                            ) {
                                Text("开始录音")
                            }
                        }
                        if (state.audioTestPlaying) {
                            Md2OutlinedButton(
                                onClick = { viewModel.stopAudioTestPlayback() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("停止回放")
                            }
                        } else {
                            Md2OutlinedButton(
                                onClick = { viewModel.startAudioTestPlayback() },
                                modifier = Modifier.weight(1f),
                                enabled = state.audioTestHasClip && !state.audioTestRecording
                            ) {
                                Text("回放测试")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Md2TextButton(
                            onClick = { viewModel.clearAudioTest() },
                            enabled = state.audioTestHasClip && !state.audioTestRecording && !state.audioTestPlaying
                        ) {
                            Text("清空录音")
                        }
                    }
                    Text(
                        "用于测试当前麦克风收音和本地回放。回放会套用当前 AI 语音增强设置，不会进入识别或朗读队列。测试前请先停止主语音链路。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    @Composable
    fun SystemSettingsContent() {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Md2StaggeredFloatIn(index = 0) {
                Md2SettingsCard(title = "系统与布局") {
                    Md2SettingDropdownRow(
                        title = "主题模式",
                        value = themeModeOptions.firstOrNull { it.first == state.themeMode }?.second
                            ?: themeModeOptions.first().second,
                        expanded = themeModeExpanded,
                        onExpandedChange = { themeModeExpanded = it },
                        supportingText = "默认跟随系统，仅影响主软件界面。"
                    ) {
                        themeModeOptions.forEach { (value, label) ->
                            M2DropdownMenuItem(
                                onClick = {
                                    themeModeExpanded = false
                                    viewModel.setThemeMode(value)
                                }
                            ) { Text(label) }
                        }
                    }
                    Md2SettingDropdownRow(
                        title = "悬浮窗主题模式",
                        value = themeModeOptions.firstOrNull { it.first == state.overlayThemeMode }?.second
                            ?: themeModeOptions.first().second,
                        expanded = overlayThemeModeExpanded,
                        onExpandedChange = { overlayThemeModeExpanded = it },
                        supportingText = "默认跟随系统，可单独控制悬浮窗亮暗色。"
                    ) {
                        themeModeOptions.forEach { (value, label) ->
                            M2DropdownMenuItem(
                                onClick = {
                                    overlayThemeModeExpanded = false
                                    viewModel.setOverlayThemeMode(value)
                                }
                            ) { Text(label) }
                        }
                    }
                    Md2SettingDropdownRow(
                        title = "系统字体大小屏蔽",
                        value = fontScaleBlockModeOptions
                            .firstOrNull { it.first == state.fontScaleBlockMode }?.second
                            ?: fontScaleBlockModeOptions[1].second,
                        expanded = fontScaleBlockModeExpanded,
                        onExpandedChange = { fontScaleBlockModeExpanded = it },
                        supportingText = "默认只固定 Material Symbol 图标大小；选择全部禁用时，主界面和悬浮窗文字也不会跟随系统字体大小缩放。"
                    ) {
                        fontScaleBlockModeOptions.forEach { (value, label) ->
                            M2DropdownMenuItem(
                                onClick = {
                                    fontScaleBlockModeExpanded = false
                                    viewModel.setFontScaleBlockMode(value)
                                }
                            ) { Text(label) }
                        }
                    }
                    Md2SettingSwitchRow(
                        title = "按键震动反馈",
                        checked = state.hapticFeedbackEnabled,
                        onCheckedChange = { viewModel.setHapticFeedbackEnabled(it) },
                        supportingText = "开启后主界面和悬浮窗按键、快捷字幕分组滑动切换会调用系统原生按键触感反馈。"
                    )
                    Md2SettingDropdownRow(
                        title = "横屏抽屉模式",
                        value = drawerModeOptions.firstOrNull { it.first == state.landscapeDrawerMode }?.second
                            ?: drawerModeOptions.first().second,
                        expanded = drawerModeExpanded,
                        onExpandedChange = { drawerModeExpanded = it },
                        supportingText = "竖屏始终为隐藏式；该选项仅影响横屏布局。"
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
                    Md2SettingSwitchRow(
                        title = "使用纯色顶栏",
                        checked = state.solidTopBar,
                        onCheckedChange = { viewModel.setSolidTopBar(it) },
                        supportingText = "开启后顶栏与状态栏颜色改为卡片同款自适应配色。"
                    )
                    Md2SettingSwitchRow(
                        title = "便捷字幕字体大小自适应",
                        checked = state.quickSubtitleAutoFit,
                        onCheckedChange = { viewModel.setQuickSubtitleAutoFit(it) },
                        supportingText = "开启后：主界面与悬浮窗的便捷字幕大字幕和弹窗预览会在内容过多时自动缩小字号，尽量避免需要上下滑动。"
                    )
                    Md2SettingSwitchRow(
                        title = "使用更紧凑的快捷文本控件",
                        checked = state.quickSubtitleCompactControls,
                        onCheckedChange = { viewModel.setQuickSubtitleCompactControls(it) },
                        supportingText = "仅影响主界面竖屏便捷字幕。开启后会改为类似迷你快捷字幕的紧凑快捷文本区，并把编辑入口移到顶栏。"
                    )
                    Md2SettingSwitchRow(
                        title = "输入框内容保持预览",
                        checked = state.quickSubtitleKeepInputPreview,
                        onCheckedChange = { viewModel.setQuickSubtitleKeepInputPreview(it) },
                        supportingText = "开启后输入框有内容时，键盘收起后大字幕仍显示输入预览；直到下一次语音或快捷文本提交前保持。"
                    )
                    Md2SettingSwitchRow(
                        title = "蓝牙媒体标题字幕",
                        checked = state.bluetoothMediaTitleSubtitle,
                        onCheckedChange = { viewModel.setBluetoothMediaTitleSubtitle(it) },
                        supportingText = "实验性兼容模式。开启后会把当前字幕写入系统媒体标题，部分蓝牙歌词屏、车机或小屏会把它显示为歌名；可能覆盖其它媒体标题。"
                    )
                    Text("画板保存路径（相册）", fontWeight = FontWeight.Bold)
                    Text(state.drawingSaveRelativePath, style = MaterialTheme.typography.bodySmall)
                    Md2SettingSwitchRow(
                        title = "将画板画布方向保持设备方向",
                        checked = state.drawingKeepCanvasOrientationToDevice,
                        onCheckedChange = { viewModel.setDrawingKeepCanvasOrientationToDevice(it) },
                        supportingText = "开启后设备旋转时画布会自动反向旋转以保持原有朝向；手动旋转会继续叠加。"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Md2OutlinedButton(onClick = { drawingDirPicker.launch(null) }) {
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
                    Md2SettingSwitchRow(
                        title = "退出名片编辑时自动保存",
                        checked = state.quickCardAutoSaveOnExit,
                        onCheckedChange = { viewModel.setQuickCardAutoSaveOnExit(it) },
                        supportingText = "关闭时将弹窗询问“是否保存名片”"
                    )
                    Md2SettingSwitchRow(
                        title = "使用内建文件管理器",
                        checked = state.useBuiltinFileManager,
                        onCheckedChange = { viewModel.setUseBuiltinFileManager(it) },
                        supportingText = "关闭时使用系统文件选择器。"
                    )
                    Md2SettingSwitchRow(
                        title = "使用内建图库",
                        checked = state.useBuiltinGallery,
                        onCheckedChange = { viewModel.setUseBuiltinGallery(it) },
                        supportingText = "关闭时使用系统图库选择器。"
                    )
                }
            }
            Md2StaggeredFloatIn(index = 1) {
                Md2SettingsCard(title = "启动器快捷方式补全") {
                    Md2SettingSwitchRow(
                        title = "使用内嵌列表补全第三方快捷方式",
                        checked = state.floatingOverlayHardcodedShortcutSupplement,
                        onCheckedChange = { viewModel.setFloatingOverlayHardcodedShortcutSupplement(it) },
                        supportingText = "默认关闭。开启后，悬浮窗启动器里第三方应用的长按菜单会用内置国内常用应用列表补齐缺失项；微信“扫一扫”始终保留。"
                    )
                    Text(
                        "运行时能正常查询到的系统快捷方式不受影响；该开关只控制写死列表的额外增补。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsCategoryContent(category: SettingsCategory) {
        when (category) {
            SettingsCategory.About -> AboutSettingsContent()
            SettingsCategory.Recognition -> RecognitionSettingsContent()
            SettingsCategory.Audio -> AudioSettingsContent()
            SettingsCategory.System -> SystemSettingsContent()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactTabs = maxWidth < 600.dp
        val wideLayout = maxWidth >= 1100.dp
        val horizontalInset = if (wideLayout) 24.dp else 16.dp

        if (compactTabs) {
            CenteredPageColumn(
                maxWidth = UiTokens.WideContentMaxWidth,
                scroll = scroll,
                horizontalPadding = horizontalInset
            ) {
                Spacer(Modifier.height(UiTokens.PageTopBlank))
                SettingsTabsCard(
                    selectedCategory = selectedCategory,
                    compact = true,
                    onSelect = { viewModel.updateSettingsSelectedCategory(it) }
                )
                AnimatedContent(
                    targetState = selectedCategory,
                    modifier = Modifier.padding(vertical = 4.dp),
                    transitionSpec = {
                        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        ContentTransform(
                            targetContentEnter = fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                                    initialOffsetX = { direction * (it / 6) }
                                ),
                            initialContentExit = fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(
                                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                                    targetOffsetX = { -direction * (it / 7) }
                                ),
                            sizeTransform = androidx.compose.animation.SizeTransform(clip = false)
                        )
                    },
                    label = "settings_tabs_content_compact"
                ) { category ->
                    CompositionLocalProvider(LocalSuppressStaggeredFloatIn provides true) {
                        SettingsCategoryContent(category)
                    }
                }
                Spacer(Modifier.height(UiTokens.PageBottomBlank))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalInset),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(if (wideLayout) 156.dp else 144.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(UiTokens.PageTopBlank))
                    SettingsTabsCard(
                        selectedCategory = selectedCategory,
                        compact = false,
                        onSelect = { viewModel.updateSettingsSelectedCategory(it) }
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CenteredPageColumn(
                        maxWidth = UiTokens.WideContentMaxWidth,
                        modifier = Modifier.fillMaxSize(),
                        scroll = scroll,
                        horizontalPadding = 0.dp
                    ) {
                        Spacer(Modifier.height(UiTokens.PageTopBlank))
                        AnimatedContent(
                            targetState = selectedCategory,
                            modifier = Modifier.padding(vertical = 4.dp),
                            transitionSpec = {
                                val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                ContentTransform(
                                    targetContentEnter = fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                        slideInVertically(
                                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                                            initialOffsetY = { direction * (it / 6) }
                                        ),
                                    initialContentExit = fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                        slideOutVertically(
                                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                                            targetOffsetY = { -direction * (it / 7) }
                                        ),
                                    sizeTransform = androidx.compose.animation.SizeTransform(clip = false)
                                )
                            },
                            label = "settings_tabs_content_rail"
                        ) { category ->
                            CompositionLocalProvider(LocalSuppressStaggeredFloatIn provides true) {
                                SettingsCategoryContent(category)
                            }
                        }
                        Spacer(Modifier.height(UiTokens.PageBottomBlank))
                    }
                }
            }
        }
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
                            speakerEnrollMessage = "请按页面引导完成本人样本采集。"
                            speakerEnrollRetryDialog = false
                        }) {
                            Text("重新采集")
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
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<File>>(emptyList()) }
    var selected by remember { mutableStateOf<File?>(null) }
    val logLines = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val logBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
    val onTopBarActionsChangeState = rememberUpdatedState(onTopBarActionsChange)

    fun refreshLogs() {
        logs = AppLogger.listLogFiles(context)
        if (selected == null || selected !in logs) {
            selected = logs.firstOrNull()
        }
        refreshToken++
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    LaunchedEffect(selected?.absolutePath, refreshToken) {
        val file = selected
        listState.scrollToItem(0)
        logLines.clear()
        if (file == null) {
            logLines += "暂无日志"
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        val rawContent = withContext(Dispatchers.IO) {
            AppLogger.readLog(file)
        }.removePrefix("\uFEFF")
        withContext(Dispatchers.Default) {
            val normalizedLines = rawContent
                .ifEmpty { "日志为空" }
                .lineSequence()
                .map { it.ifEmpty { " " } }
                .toList()
            if (normalizedLines.isEmpty()) {
                withContext(Dispatchers.Main) {
                    logLines += "日志为空"
                }
            } else {
                normalizedLines.chunked(200).forEach { chunk ->
                    withContext(Dispatchers.Main) {
                        logLines.addAll(chunk)
                    }
                    yield()
                }
            }
        }
        isLoading = false
    }

    SideEffect {
        onTopBarActionsChangeState.value(
            LogTopBarActions(
                onRefresh = { refreshLogs() },
                onCopy = {
                    scope.launch {
                        val text = selected?.let { file ->
                            withContext(Dispatchers.IO) {
                                AppLogger.readLog(file).ifEmpty { "日志为空" }
                            }
                        } ?: "暂无日志"
                        clipboard.setText(AnnotatedString(text))
                        toast(context, "已复制")
                    }
                },
                onShare = {
                    val file = selected
                    if (file != null) {
                        shareLogFile(context, file)
                    } else {
                        toast(context, "暂无可分享日志")
                    }
                },
                canCopy = selected != null,
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
                    Md2AnimatedOptionMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(UiTokens.Radius),
                backgroundColor = md2CardContainerColor(),
                elevation = UiTokens.CardElevation
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (logLines.isEmpty() && isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    itemsIndexed(logLines) { _, line ->
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (isLoading && logLines.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(logBottomPadding))
    }
}

@Composable
private fun LegalDocumentScreen(assetPath: String) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val legalBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
    val markdownBlocks = remember(assetPath) { mutableStateListOf<MarkdownBlock>() }
    var isParsing by remember(assetPath) { mutableStateOf(true) }

    LaunchedEffect(assetPath) {
        isParsing = true
        markdownBlocks.clear()
        val documentText = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    .removePrefix("\uFEFF")
            }.getOrElse {
                "文档加载失败：${it.message ?: "未知错误"}"
            }
        }
        var emittedAny = false
        withContext(Dispatchers.Default) {
            parseMarkdownBlocksStreaming(documentText, chunkSize = 24) { chunk ->
                withContext(Dispatchers.Main) {
                    markdownBlocks.addAll(chunk)
                    if (!emittedAny) {
                        emittedAny = true
                        isParsing = false
                    }
                }
            }
        }
        if (!emittedAny) {
            markdownBlocks += MarkdownBlock.Paragraph("暂无内容")
        }
        isParsing = false
    }
    fun openExternalUrl(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            toast(context, "打开链接失败")
        }
    }
    CenteredPageBox(
        maxWidth = UiTokens.WideContentMaxWidth,
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = UiTokens.PageTopBlank, bottom = legalBottomPadding),
            shape = RoundedCornerShape(UiTokens.Radius),
            backgroundColor = md2CardContainerColor(),
            elevation = UiTokens.CardElevation
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                if (markdownBlocks.isEmpty() && isParsing) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                itemsIndexed(markdownBlocks) { _, block ->
                    MarkdownBlockView(
                        block = block,
                        onOpenUrl = ::openExternalUrl
                    )
                }
                if (isParsing && markdownBlocks.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class ListBlock(
        val items: List<String>,
        val ordered: Boolean,
        val startIndex: Int = 1
    ) : MarkdownBlock
    data class CodeFence(val code: String) : MarkdownBlock
    data object Divider : MarkdownBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.replace("\r\n", "\n").split('\n')
    val paragraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isEmpty()) return
        val text = paragraphLines.joinToString(" ") { it.trim() }
            .replace(Regex("\\s+"), " ")
            .trim()
        if (text.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(text)
        }
        paragraphLines.clear()
    }

    var index = 0
    while (index < lines.size) {
        val rawLine = lines[index]
        val trimmed = rawLine.trim()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            val codeLines = mutableListOf<String>()
            index++
            while (index < lines.size && !lines[index].trim().startsWith("```")) {
                codeLines += lines[index]
                index++
            }
            blocks += MarkdownBlock.CodeFence(codeLines.joinToString("\n").trimEnd())
            index++
            continue
        }

        if (trimmed.isBlank()) {
            flushParagraph()
            index++
            continue
        }

        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.Heading(
                level = headingMatch.groupValues[1].length,
                text = headingMatch.groupValues[2].trim()
            )
            index++
            continue
        }

        if (trimmed.matches(Regex("^([-*_])\\1{2,}$"))) {
            flushParagraph()
            blocks += MarkdownBlock.Divider
            index++
            continue
        }

        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            flushParagraph()
            val items = mutableListOf<String>()
            while (index < lines.size) {
                val listLine = lines[index].trim()
                if (listLine.startsWith("- ") || listLine.startsWith("* ")) {
                    items += listLine.substring(2).trim()
                    index++
                } else {
                    break
                }
            }
            if (items.isNotEmpty()) {
                blocks += MarkdownBlock.ListBlock(items = items, ordered = false)
            }
            continue
        }

        val orderedMatch = Regex("^(\\d+)\\.\\s+(.+)$").matchEntire(trimmed)
        if (orderedMatch != null) {
            flushParagraph()
            val startIndex = orderedMatch.groupValues[1].toIntOrNull() ?: 1
            val items = mutableListOf<String>()
            while (index < lines.size) {
                val listLine = lines[index].trim()
                val match = Regex("^(\\d+)\\.\\s+(.+)$").matchEntire(listLine)
                if (match != null) {
                    items += match.groupValues[2].trim()
                    index++
                } else {
                    break
                }
            }
            if (items.isNotEmpty()) {
                blocks += MarkdownBlock.ListBlock(items = items, ordered = true, startIndex = startIndex)
            }
            continue
        }

        paragraphLines += rawLine
        index++
    }

    flushParagraph()
    return blocks
}

private data class MarkdownColors(
    val text: Color,
    val heading: Color,
    val link: Color,
    val code: Color,
    val codeBackground: Color,
    val codeBlockBackground: Color,
    val divider: Color,
    val hint: Color
)

private suspend fun parseMarkdownBlocksStreaming(
    markdown: String,
    chunkSize: Int,
    onChunk: suspend (List<MarkdownBlock>) -> Unit
) {
    val lines = markdown.replace("\r\n", "\n").split('\n')
    val paragraphLines = mutableListOf<String>()
    val pendingBlocks = mutableListOf<MarkdownBlock>()

    suspend fun emitBlock(block: MarkdownBlock) {
        pendingBlocks += block
        if (pendingBlocks.size >= chunkSize) {
            onChunk(pendingBlocks.toList())
            pendingBlocks.clear()
            yield()
        }
    }

    suspend fun flushParagraph() {
        if (paragraphLines.isEmpty()) return
        val text = paragraphLines.joinToString(" ") { it.trim() }
            .replace(Regex("\\s+"), " ")
            .trim()
        if (text.isNotEmpty()) {
            emitBlock(MarkdownBlock.Paragraph(text))
        }
        paragraphLines.clear()
    }

    var index = 0
    while (index < lines.size) {
        val rawLine = lines[index]
        val trimmed = rawLine.trim()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            val codeLines = mutableListOf<String>()
            index++
            while (index < lines.size && !lines[index].trim().startsWith("```")) {
                codeLines += lines[index]
                index++
            }
            emitBlock(MarkdownBlock.CodeFence(codeLines.joinToString("\n").trimEnd()))
            index++
            continue
        }

        if (trimmed.isBlank()) {
            flushParagraph()
            index++
            continue
        }

        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            emitBlock(
                MarkdownBlock.Heading(
                    level = headingMatch.groupValues[1].length,
                    text = headingMatch.groupValues[2].trim()
                )
            )
            index++
            continue
        }

        if (trimmed.matches(Regex("^([-*_])\\1{2,}$"))) {
            flushParagraph()
            emitBlock(MarkdownBlock.Divider)
            index++
            continue
        }

        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            flushParagraph()
            val items = mutableListOf<String>()
            while (index < lines.size) {
                val listLine = lines[index].trim()
                if (listLine.startsWith("- ") || listLine.startsWith("* ")) {
                    items += listLine.substring(2).trim()
                    index++
                } else {
                    break
                }
            }
            if (items.isNotEmpty()) {
                emitBlock(MarkdownBlock.ListBlock(items = items, ordered = false))
            }
            continue
        }

        val orderedMatch = Regex("^(\\d+)\\.\\s+(.+)$").matchEntire(trimmed)
        if (orderedMatch != null) {
            flushParagraph()
            val startIndex = orderedMatch.groupValues[1].toIntOrNull() ?: 1
            val items = mutableListOf<String>()
            while (index < lines.size) {
                val listLine = lines[index].trim()
                val match = Regex("^(\\d+)\\.\\s+(.+)$").matchEntire(listLine)
                if (match != null) {
                    items += match.groupValues[2].trim()
                    index++
                } else {
                    break
                }
            }
            if (items.isNotEmpty()) {
                emitBlock(
                    MarkdownBlock.ListBlock(
                        items = items,
                        ordered = true,
                        startIndex = startIndex
                    )
                )
            }
            continue
        }

        paragraphLines += rawLine
        index++
    }

    flushParagraph()
    if (pendingBlocks.isNotEmpty()) {
        onChunk(pendingBlocks.toList())
    }
}

@Composable
private fun MarkdownBlockView(
    block: MarkdownBlock,
    onOpenUrl: (String) -> Unit
) {
    val dark = currentAppDarkTheme()
    val colors = remember(dark) {
        MarkdownColors(
            text = if (dark) Color(0xFFE5ECEF) else Color(0xFF1B1F22),
            heading = if (dark) Color(0xFFF4FAFC) else Color(0xFF101417),
            link = if (dark) Color(0xFF7FD7F1) else Color(0xFF007C91),
            code = if (dark) Color(0xFFF5F7F9) else Color(0xFF1D252B),
            codeBackground = if (dark) Color(0xFF24313A) else Color(0xFFE8EEF2),
            codeBlockBackground = if (dark) Color(0xFF1A232A) else Color(0xFFF3F6F8),
            divider = if (dark) Color(0xFF45525A) else Color(0xFFD6DEE3),
            hint = if (dark) Color(0xFF9FB0BA) else Color(0xFF687780)
        )
    }
    when (block) {
        is MarkdownBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.h5.copy(color = colors.heading)
                2 -> MaterialTheme.typography.h6.copy(color = colors.heading)
                3 -> MaterialTheme.typography.subtitle1.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.heading
                )
                else -> MaterialTheme.typography.body1.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.heading
                )
            }
            MarkdownText(
                text = block.text,
                style = style,
                colors = colors,
                onOpenUrl = onOpenUrl
            )
        }

        is MarkdownBlock.Paragraph -> {
            MarkdownText(
                text = block.text,
                style = MaterialTheme.typography.body2.copy(
                    lineHeight = 20.sp,
                    color = colors.text
                ),
                colors = colors,
                onOpenUrl = onOpenUrl
            )
        }

        is MarkdownBlock.ListBlock -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                block.items.forEachIndexed { itemIndex, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (block.ordered) "${block.startIndex + itemIndex}." else "•",
                            style = MaterialTheme.typography.body2.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 20.sp,
                                color = colors.heading
                            ),
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        MarkdownText(
                            text = item,
                            style = MaterialTheme.typography.body2.copy(
                                lineHeight = 20.sp,
                                color = colors.text
                            ),
                            modifier = Modifier.weight(1f),
                            colors = colors,
                            onOpenUrl = onOpenUrl
                        )
                    }
                }
            }
        }

        is MarkdownBlock.CodeFence -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                backgroundColor = colors.codeBlockBackground,
                elevation = 0.dp
            ) {
                SelectionContainer(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = block.code,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.code,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }

        MarkdownBlock.Divider -> Divider(color = colors.divider)
    }
}

@Composable
private fun MarkdownText(
    text: String,
    style: TextStyle,
    colors: MarkdownColors,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotated = remember(text, colors) {
        buildMarkdownAnnotatedString(
            text = text,
            linkColor = colors.link,
            codeColor = colors.code,
            codeBackground = colors.codeBackground
        )
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style,
        onClick = { offset ->
            annotated
                .getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { onOpenUrl(it.item) }
        }
    )
}

private fun buildMarkdownAnnotatedString(
    text: String,
    linkColor: Color,
    codeColor: Color,
    codeBackground: Color
): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        val linkMatch = Regex("""^\[([^\]]+)]\((https?://[^)]+)\)""").find(text.substring(index))
        if (linkMatch != null && linkMatch.range.first == 0) {
            val label = linkMatch.groupValues[1]
            val url = linkMatch.groupValues[2]
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(label)
            }
            pop()
            index += linkMatch.value.length
            continue
        }

        if (text.startsWith("**", index)) {
            val end = text.indexOf("**", startIndex = index + 2)
            if (end > index + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(index + 2, end))
                }
                index = end + 2
                continue
            }
        }

        if (text[index] == '`') {
            val end = text.indexOf('`', startIndex = index + 1)
            if (end > index + 1) {
                withStyle(
                    SpanStyle(
                        color = codeColor,
                        background = codeBackground,
                        fontFamily = FontFamily.Monospace
                    )
                ) {
                    append(text.substring(index + 1, end))
                }
                index = end + 1
                continue
            }
        }

        append(text[index])
        index++
    }
}

private fun toast(context: android.content.Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

private fun writeQuickSubtitlePresetPackage(context: Context, groups: List<QuickSubtitleGroup>): File {
    require(groups.isNotEmpty()) { "未选择需要导出的分组" }
    val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val out = File(shareDir, "quick_subtitle_$ts.kigtpk")
    val root = JSONObject().apply {
        put("type", "quick_subtitle")
        put("version", 1)
        put(
            "groups",
            JSONArray().apply {
                groups.forEach { group ->
                    put(
                        JSONObject().apply {
                            put("id", group.id)
                            put("title", group.title)
                            put("icon", group.icon)
                            put("items", JSONArray().apply { group.items.forEach { put(it) } })
                        }
                    )
                }
            }
        )
    }
    ZipOutputStream(out.outputStream()).use { zos ->
        zos.putNextEntry(ZipEntry("preset.json"))
        zos.write(root.toString(2).toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }
    return out
}

private fun readQuickSubtitlePresetPackage(context: Context, uri: Uri): List<QuickSubtitleGroup> {
    val json = try {
        var jsonPayload: String? = null
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.replace('\\', '/') == "preset.json") {
                        jsonPayload = zis.readBytes().toString(Charsets.UTF_8)
                    }
                    zis.closeEntry()
                }
            }
        } ?: error("无法打开预设包")
        jsonPayload ?: error("预设包缺少 preset.json")
    } catch (_: java.util.zip.ZipException) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("无法打开预设包")
    }
    val root = JSONObject(json)
    require(root.optString("type") == "quick_subtitle") { "不是便捷字幕预设包" }
    val groups = mutableListOf<QuickSubtitleGroup>()
    val groupArray = root.optJSONArray("groups") ?: JSONArray()
    for (i in 0 until groupArray.length()) {
        val obj = groupArray.optJSONObject(i) ?: continue
        val itemsArray = obj.optJSONArray("items") ?: JSONArray()
        val items = mutableListOf<String>()
        for (j in 0 until itemsArray.length()) {
            val text = itemsArray.optString(j, "").trim()
            if (text.isNotEmpty()) items += text
        }
        groups += QuickSubtitleGroup(
            id = obj.optLong("id", i + 1L),
            title = obj.optString("title", "未命名分组").trim().ifBlank { "未命名分组" },
            icon = obj.optString("icon", "sentiment_neutral").ifBlank { "sentiment_neutral" },
            items = items.ifEmpty { listOf("请输入常用短句") }
        )
    }
    return groups
}

private fun uniqueQuickSubtitleGroupTitle(baseTitle: String, existingTitles: Collection<String>): String {
    val trimmed = baseTitle.trim().ifBlank { "未命名分组" }
    if (trimmed !in existingTitles) return trimmed
    var index = 2
    while (true) {
        val candidate = "$trimmed ($index)"
        if (candidate !in existingTitles) return candidate
        index += 1
    }
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
