package com.kgtts.app.overlay

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.content.pm.ShortcutInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewConfiguration
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.kgtts.app.R
import com.kgtts.app.data.UserPrefs
import com.kgtts.app.overlay.RealtimeRuntimeBridge
import com.kgtts.app.ui.QuickCard
import com.kgtts.app.ui.QuickCardType
import com.kgtts.app.util.AppLogger
import com.kgtts.app.util.QuickCardRenderCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FloatingOverlayService : Service() {
    private val overlayRadiusDp = 4f
    private val defaultQuickSubtitleText = "快捷字幕\n大字幕"
    private val FAB_EDGE_LEFT = "left"
    private val FAB_EDGE_RIGHT = "right"
    private val fabIdleDockDelayMs = 3000L
    private val fabIdleDockAlpha = 0.56f
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val iconTypeface: Typeface? by lazy {
        ResourcesCompat.getFont(this, R.font.material_symbols_sharp)
    }

    private var settings = UserPrefs.AppSettings()
    private var settingsJob: Job? = null

    private var fabRoot: LinearLayout? = null
    private var fabParams: WindowManager.LayoutParams? = null
    private var bubbleRow: LinearLayout? = null
    private var bubbleTextView: TextView? = null
    private var fabButton: FrameLayout? = null
    private var fabIconView: TextView? = null
    private var panelRoot: FrameLayout? = null
    private var panelContent: LinearLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var panelVisible = false
    private var panelPageIndex = 0
    private var panelStatusTextView: TextView? = null
    private var panelStatusLogoView: ImageView? = null
    private var panelStatusMicContainer: LinearLayout? = null
    private var panelStatusEqContainer: LinearLayout? = null
    private var panelStatusMicIconView: TextView? = null
    private var panelStatusEqIconView: TextView? = null
    private var panelStatusMicProgressView: ProgressBar? = null
    private var panelStatusEqProgressView: ProgressBar? = null
    private var panelStatusTriggerContainer: LinearLayout? = null
    private var panelTopStripView: LinearLayout? = null
    private var panelStatusDetailRefs: OverlayStatusDetailRefs? = null
    private var panelBuiltInContainer: LinearLayout? = null
    private var panelShortcutContainer: LinearLayout? = null
    private var panelShortcutScroll: ScrollView? = null
    private var panelIndicatorContainer: LinearLayout? = null
    private var panelActionFab: FrameLayout? = null
    private var panelActionFabIconView: TextView? = null
    private var panelPager: ViewPager2? = null
    private var panelPagerAdapter: OverlayLauncherPageAdapter? = null
    private var panelEditButtonView: TextView? = null
    private var panelOpenButtonView: TextView? = null
    private var panelPrevPageButtonView: View? = null
    private var panelNextPageButtonView: View? = null
    private var panelPickerOverlay: FrameLayout? = null
    private var panelPickerParams: WindowManager.LayoutParams? = null
    private var panelPickerListContainer: LinearLayout? = null
    private var panelPickerSearchInput: EditText? = null
    private var miniRoot: FrameLayout? = null
    private var miniContent: LinearLayout? = null
    private var miniParams: WindowManager.LayoutParams? = null
    private var miniVisible = false
    private var miniStatusTextView: TextView? = null
    private var miniStatusLogoView: ImageView? = null
    private var miniStatusMicContainer: LinearLayout? = null
    private var miniStatusEqContainer: LinearLayout? = null
    private var miniStatusMicIconView: TextView? = null
    private var miniStatusEqIconView: TextView? = null
    private var miniStatusMicProgressView: ProgressBar? = null
    private var miniStatusEqProgressView: ProgressBar? = null
    private var miniStatusTriggerContainer: LinearLayout? = null
    private var miniTopStripView: LinearLayout? = null
    private var miniStatusDetailRefs: OverlayStatusDetailRefs? = null
    private var miniSubtitleTextView: TextView? = null
    private var miniSubtitleSeekBar: SeekBar? = null
    private var miniQuickItemsContainer: FrameLayout? = null
    private var miniQuickItemsRecyclerView: RecyclerView? = null
    private var miniQuickItemsAdapter: MiniQuickTextAdapter? = null
    private var miniQuickItemsLeftFadeView: View? = null
    private var miniQuickItemsRightFadeView: View? = null
    private var miniQuickRow: LinearLayout? = null
    private var miniGroupIconView: TextView? = null
    private var miniQuickCollapseButton: TextView? = null
    private var miniSubtitleBody: LinearLayout? = null
    private var miniQuickCardBody: LinearLayout? = null
    private var miniQuickCardPreviewContainer: FrameLayout? = null
    private var miniQuickCardIndicatorContainer: LinearLayout? = null
    private var miniQuickCardItemsContainer: LinearLayout? = null
    private var miniQuickCardPager: ViewPager2? = null
    private var miniQuickCardPagerAdapter: MiniQuickCardPagerAdapter? = null
    private var miniActionFab: FrameLayout? = null
    private var miniActionFabIconView: TextView? = null
    private var miniBackButtonView: TextView? = null
    private var miniOpenButtonView: TextView? = null
    private var miniPreviewOverlay: FrameLayout? = null
    private var miniPreviewHost: FrameLayout? = null

    private var confirmOverlay: FrameLayout? = null
    private var confirmTextCardView: FrameLayout? = null
    private var confirmClipContainer: FrameLayout? = null
    private var confirmParams: WindowManager.LayoutParams? = null
    private var confirmTextView: TextView? = null
    private var leftActionButton: FrameLayout? = null
    private var rightActionButton: FrameLayout? = null

    private var running = false
    private var pttPressed = false
    private var pttTemporaryStart = false
    private var pttSessionLastText = ""
    private var pttStreamingText = ""
    private var latestRecognizedText = ""
    private var currentDragAction = OverlayReleaseAction.SendToSubtitle
    private var overlayDarkTheme = false
    private var overlayStatusExpanded = false
    private var overlayInputLevel = 0f
    private var overlayPlaybackProgress = 0f
    private var overlayInputDeviceLabel = ""
    private var overlayOutputDeviceLabel = ""
    private var fabSnapAnimator: ValueAnimator? = null
    private var fabIdleDockJob: Job? = null
    private var fabIdleDocked = false
    private var portraitFabAnchor: OverlayFabAnchor? = null
    private var landscapeFabAnchor: OverlayFabAnchor? = null
    private var currentFabOrientation = Configuration.ORIENTATION_PORTRAIT
    private var lastFabDisplayWidth = 0
    private var lastFabDisplayHeight = 0
    private var downRawX = 0f
    private var downRawY = 0f
    private var downWinX = 0
    private var downWinY = 0
    private var draggingFab = false
    private val runtimeBridgeListener =
        object : RealtimeRuntimeBridge.Listener {
            override fun onAppRuntimeChanged() {
                scope.launch {
                    updateFabUi()
                    if (pttPressed && confirmOverlay?.visibility == View.VISIBLE) {
                        updateConfirmVisuals(currentDragAction)
                    }
                }
            }
        }

    private data class QuickSubtitleGroupConfig(
        val id: Long,
        val title: String,
        val icon: String,
        val items: List<String>
    )

    private var quickSubtitleGroups = defaultQuickSubtitleGroups()
    private var quickSubtitleSelectedGroupId = 1L
    private var quickSubtitleCurrentText = defaultQuickSubtitleText
    private var quickSubtitleFontSizeSp = 56f
    private var quickSubtitlePlayOnSend = true
    private var quickSubtitleBold = true
    private var quickSubtitleCentered = false
    private var quickSubtitleInputText = ""
    private var quickSubtitleNextGroupId = 4L
    private var quickSubtitleSaving = false
    private var miniQuickItemsCollapsed = false
    private var miniMode = MiniOverlayMode.Subtitle
    private var miniPreviewMode = MiniPreviewMode.None
    private var miniPreviewQuickCardId: Long? = null
    private var quickCards: List<QuickCard> = emptyList()
    private var quickCardSelectedIndex = 0
    private var quickCardConfigRawCache = ""
    private var overlayShortcutSaving = false
    private var overlayLauncherLayoutLoaded = false
    private var overlayShortcuts = mutableListOf<OverlayAppShortcut>()
    private var overlayLauncherOrder = mutableListOf<String>()
    private var launchableAppsCache: List<OverlayAppShortcut> = emptyList()
    private var launchableAppsLoaded = false
    private var launchableAppsLoading = false
    private var panelPickerSearchQuery = ""
    private val shortcutIconStateCache = linkedMapOf<String, Drawable.ConstantState?>()
    private var panelEditMode = false
    private var panelPageCount = 1
    private var panelDragPendingDirection = 0
    private var panelDragSwitchRunnable: Runnable? = null
    private var panelDraggedKey: String? = null
    private var panelDragHoverIndex = -1
    private var panelDragPreviewOrder: MutableList<String>? = null
    private var panelUiRefreshPosted = false
    private var panelUiRefreshSyncPager = false

    private enum class OverlayReleaseAction {
        SendToSubtitle,
        SendToInput,
        Cancel
    }

    private enum class MiniOverlayMode {
        Subtitle,
        QuickCard
    }

    private enum class MiniPreviewMode {
        None,
        Subtitle,
        QuickCard
    }

    private data class OverlayAppShortcut(
        val packageName: String,
        val className: String,
        val label: String
    )

    private data class OverlayFabAnchor(
        val edge: String,
        val verticalRatio: Float
    )

    private data class ManifestShortcutSpec(
        val id: String,
        val title: String,
        val intents: List<Intent> = emptyList()
    )

    private data class OverlayLauncherTile(
        val key: String,
        val label: String,
        val icon: String,
        val shortcut: OverlayAppShortcut? = null,
        val isAddButton: Boolean = false,
        val isPlaceholder: Boolean = false
    )

    private data class OverlayStatusDetailRefs(
        val card: LinearLayout,
        val inputProgress: ProgressBar,
        val playbackProgress: ProgressBar,
        val inputLabel: TextView,
        val outputLabel: TextView,
        val pttIcon: TextView,
        val volumeLabel: TextView,
        val volumeSeekBar: SeekBar
    )

    private inner class MiniQuickCardPagerAdapter :
        RecyclerView.Adapter<MiniQuickCardPagerAdapter.PageViewHolder>() {
        private var items: List<QuickCard?> = listOf(null)

        init {
            setHasStableIds(true)
        }

        inner class PageViewHolder(val container: FrameLayout) : RecyclerView.ViewHolder(container)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            return PageViewHolder(
                FrameLayout(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    clipChildren = false
                    clipToPadding = false
                }
            )
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.container.removeAllViews()
            holder.container.addView(
                createMiniQuickCardPage(items.getOrNull(position)),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        override fun getItemCount(): Int = items.size

        override fun getItemId(position: Int): Long =
            items.getOrNull(position)?.id ?: Long.MIN_VALUE

        fun submitCards(cards: List<QuickCard>) {
            items = if (cards.isEmpty()) listOf(null) else cards
            notifyDataSetChanged()
        }
    }

    private inner class MiniQuickTextAdapter :
        RecyclerView.Adapter<MiniQuickTextAdapter.TextViewHolder>() {
        private var items: List<String> = emptyList()

        inner class TextViewHolder(
            val root: FrameLayout,
            val textView: TextView
        ) : RecyclerView.ViewHolder(root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
            val textView =
                TextView(parent.context).apply {
                    setTextColor(overlayOnSurfaceColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    typeface = Typeface.DEFAULT_BOLD
                    maxLines = 3
                    ellipsize = TextUtils.TruncateAt.END
                    includeFontPadding = false
                    setLineSpacing(0f, 1f)
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }
            val root =
                FrameLayout(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(dp(112), dp(104))
                    setPadding(dp(14), dp(8), dp(14), dp(20))
                    minimumWidth = dp(112)
                    minimumHeight = dp(104)
                    clipChildren = true
                    clipToPadding = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        foreground = selectableDrawable()
                    }
                    addView(
                        textView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.START
                        )
                    )
                }
            return TextViewHolder(root, textView)
        }

        override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
            val item = items.getOrNull(position).orEmpty()
            holder.textView.text = item
            holder.root.setOnClickListener {
                if (item.isBlank()) return@setOnClickListener
                submitQuickSubtitleText(item)
            }
        }

        override fun getItemCount(): Int = items.size

        fun submitItems(newItems: List<String>) {
            items = newItems.toList()
            notifyDataSetChanged()
        }
    }

    private fun normalizeFabOrientation(orientation: Int): Int =
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Configuration.ORIENTATION_LANDSCAPE
        } else {
            Configuration.ORIENTATION_PORTRAIT
        }

    private fun currentOrientationFabAnchor(): OverlayFabAnchor? =
        if (currentFabOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            landscapeFabAnchor
        } else {
            portraitFabAnchor
        }

    private fun oppositeOrientationFabAnchor(): OverlayFabAnchor? =
        if (currentFabOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            portraitFabAnchor
        } else {
            landscapeFabAnchor
        }

    private fun setFabAnchorForOrientation(orientation: Int, anchor: OverlayFabAnchor?) {
        if (normalizeFabOrientation(orientation) == Configuration.ORIENTATION_LANDSCAPE) {
            landscapeFabAnchor = anchor
        } else {
            portraitFabAnchor = anchor
        }
    }

    private fun updateFabDisplaySnapshot() {
        currentFabOrientation = normalizeFabOrientation(resources.configuration.orientation)
        lastFabDisplayWidth = displayWidth()
        lastFabDisplayHeight = displayHeight()
    }

    private fun canAutoDockFab(): Boolean {
        val root = fabRoot ?: return false
        return settings.floatingOverlayAutoDock &&
            !panelVisible &&
            !miniVisible &&
            !pttPressed &&
            root.visibility == View.VISIBLE
    }

    private fun fabEdgePaddingPx(): Int = 0

    private fun fabMinY(): Int = dp(48)

    private fun fabMaxY(screenHeight: Int = displayHeight()): Int =
        max(fabMinY(), screenHeight - dp(220))

    private fun fabMaxX(screenWidth: Int = displayWidth()): Int =
        max(0, screenWidth - max(fabRoot?.measuredWidth ?: 0, dp(FAB_SIZE_DP)))

    private fun fabSnapLeftX(): Int = fabEdgePaddingPx()

    private fun fabSnapRightX(screenWidth: Int = displayWidth()): Int =
        max(fabEdgePaddingPx(), fabMaxX(screenWidth) - fabEdgePaddingPx())

    private fun buildFabAnchor(
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int
    ): OverlayFabAnchor {
        val leftX = fabSnapLeftX()
        val rightX = fabSnapRightX(screenWidth)
        val edge =
            if (abs(x - leftX) <= abs(x - rightX)) FAB_EDGE_LEFT else FAB_EDGE_RIGHT
        val minY = fabMinY()
        val maxY = fabMaxY(screenHeight)
        val verticalRatio =
            if (maxY <= minY) {
                1f
            } else {
                ((y.coerceIn(minY, maxY) - minY).toFloat() / (maxY - minY)).coerceIn(0f, 1f)
            }
        return OverlayFabAnchor(edge = edge, verticalRatio = verticalRatio)
    }

    private fun captureCurrentFabAnchor(screenWidth: Int, screenHeight: Int): OverlayFabAnchor? {
        val params = fabParams ?: return null
        return buildFabAnchor(params.x, params.y, screenWidth, screenHeight)
    }

    private fun saveFabAnchorForOrientation(
        orientation: Int,
        screenWidth: Int,
        screenHeight: Int,
        persist: Boolean = true
    ) {
        val anchor = captureCurrentFabAnchor(screenWidth, screenHeight) ?: return
        setFabAnchorForOrientation(orientation, anchor)
        if (persist) saveOverlayLauncherLayout()
    }

    private fun applyFabAnchor(
        params: WindowManager.LayoutParams,
        anchor: OverlayFabAnchor,
        screenWidth: Int,
        screenHeight: Int
    ) {
        params.x =
            if (anchor.edge == FAB_EDGE_LEFT) {
                fabSnapLeftX()
            } else {
                fabSnapRightX(screenWidth)
            }
        val minY = fabMinY()
        val maxY = fabMaxY(screenHeight)
        params.y =
            if (maxY <= minY) {
                minY
            } else {
                (minY + (maxY - minY) * anchor.verticalRatio.coerceIn(0f, 1f)).roundToInt()
            }
    }

    private fun restoreFabPositionForCurrentOrientation(allowOppositeConversion: Boolean) {
        val params = fabParams ?: return
        val anchor = currentOrientationFabAnchor()
        val converted = if (anchor == null && allowOppositeConversion) {
            oppositeOrientationFabAnchor()
        } else {
            null
        }
        val resolved = anchor ?: converted
        if (resolved != null) {
            applyFabAnchor(
                params = params,
                anchor = resolved,
                screenWidth = lastFabDisplayWidth.takeIf { it > 0 } ?: displayWidth(),
                screenHeight = lastFabDisplayHeight.takeIf { it > 0 } ?: displayHeight()
            )
            setFabAnchorForOrientation(currentFabOrientation, resolved)
        } else {
            clampFabToScreen()
        }
        fabRoot?.let { root ->
            runCatching { windowManager.updateViewLayout(root, params) }
        }
        if (converted != null) saveOverlayLauncherLayout()
    }

    private fun fabButtonHalfExposureOffset(): Int {
        val rootWidth =
            fabRoot?.measuredWidth?.takeIf { it > 0 }
                ?: fabRoot?.width?.takeIf { it > 0 }
                ?: (dp(FAB_SIZE_DP) + dp(28))
        val buttonWidth =
            fabButton?.measuredWidth?.takeIf { it > 0 }
                ?: fabButton?.width?.takeIf { it > 0 }
                ?: dp(FAB_SIZE_DP)
        val rightPadding = fabRoot?.paddingRight ?: dp(14)
        val byButton = rightPadding + buttonWidth / 2
        val byRootHalf = rootWidth / 2
        return max(byButton, byRootHalf)
    }

    private fun dockedFabXForEdge(edge: String, screenWidth: Int): Int {
        val exposure = fabButtonHalfExposureOffset()
        return if (edge == FAB_EDGE_LEFT) {
            -exposure
        } else {
            screenWidth - exposure
        }
    }

    private fun restoreFabFromIdleDock() {
        if (!fabIdleDocked) return
        val params = fabParams ?: return
        val root = fabRoot ?: return
        fabIdleDockJob?.cancel()
        fabIdleDockJob = null
        fabSnapAnimator?.cancel()
        val anchor = currentOrientationFabAnchor()
            ?: buildFabAnchor(params.x, params.y, displayWidth(), displayHeight())
        applyFabAnchor(params, anchor, displayWidth(), displayHeight())
        fabIdleDocked = false
        root.alpha = 1f
        runCatching { windowManager.updateViewLayout(root, params) }
    }

    private fun applyFabIdleDockVisualState() {
        val root = fabRoot ?: return
        if (fabIdleDocked) {
            bubbleRow?.visibility = View.GONE
            root.alpha = fabIdleDockAlpha
        } else {
            root.alpha = 1f
        }
    }

    private fun cancelFabIdleDock(restoreFab: Boolean) {
        fabIdleDockJob?.cancel()
        fabIdleDockJob = null
        if (restoreFab) restoreFabFromIdleDock()
    }

    private fun dockFabIdleNow() {
        val params = fabParams ?: return
        val root = fabRoot ?: return
        val anchor = currentOrientationFabAnchor()
            ?: buildFabAnchor(params.x, params.y, displayWidth(), displayHeight())
        setFabAnchorForOrientation(currentFabOrientation, anchor)
        bubbleRow?.visibility = View.GONE
        root.requestLayout()
        root.post {
            val liveParams = fabParams ?: return@post
            val liveRoot = fabRoot ?: return@post
            val targetX = dockedFabXForEdge(anchor.edge, displayWidth())
            val targetY = liveParams.y
            val startX = liveParams.x
            val startY = liveParams.y
            fabSnapAnimator?.cancel()
            fabIdleDocked = true
            if (startX == targetX && startY == targetY) {
                liveParams.x = targetX
                liveParams.y = targetY
                liveRoot.alpha = fabIdleDockAlpha
                runCatching { windowManager.updateViewLayout(liveRoot, liveParams) }
                return@post
            }
            fabSnapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 220L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    val fraction = animator.animatedFraction
                    liveParams.x = (startX + (targetX - startX) * fraction).roundToInt()
                    liveParams.y = (startY + (targetY - startY) * fraction).roundToInt()
                    liveRoot.alpha = 1f + (fabIdleDockAlpha - 1f) * fraction
                    runCatching { windowManager.updateViewLayout(liveRoot, liveParams) }
                }
            }.also { it.start() }
        }
    }

    private fun scheduleFabIdleDock() {
        fabIdleDockJob?.cancel()
        if (!canAutoDockFab() || fabIdleDocked) {
            applyFabIdleDockVisualState()
            return
        }
        fabIdleDockJob = scope.launch {
            delay(fabIdleDockDelayMs)
            if (canAutoDockFab() && !draggingFab && !fabIdleDocked) {
                dockFabIdleNow()
            }
        }
    }

    private fun refreshFabIdleDockState() {
        if (!settings.floatingOverlayAutoDock || panelVisible || miniVisible || pttPressed) {
            cancelFabIdleDock(restoreFab = true)
            applyFabIdleDockVisualState()
            return
        }
        if (fabIdleDocked) {
            dockFabIdleNow()
        } else {
            scheduleFabIdleDock()
        }
    }

    private inner class OverlayLauncherPageAdapter :
        RecyclerView.Adapter<OverlayLauncherPageAdapter.PageViewHolder>() {
        private var pages: List<List<OverlayLauncherTile>> = emptyList()

        inner class PageViewHolder(val container: FrameLayout) : RecyclerView.ViewHolder(container)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            return PageViewHolder(
                FrameLayout(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    clipChildren = false
                    clipToPadding = false
                    layoutTransition = overlayLayoutTransition()
                }
            )
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            android.transition.TransitionManager.beginDelayedTransition(holder.container)
            holder.container.removeAllViews()
            holder.container.addView(
                buildPanelPageView(position, pages.getOrElse(position) { emptyList() }),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        override fun getItemCount(): Int = pages.size

        fun submitPages(nextPages: List<List<OverlayLauncherTile>>) {
            pages = nextPages
            notifyDataSetChanged()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("FloatingOverlayService.onCreate")
        if (!canDrawOverlays(this)) {
            stopSelf()
            return
        }
        overlayDarkTheme = isOverlayDarkTheme()
        updateFabDisplaySnapshot()
        startForegroundInternal()
        ensureWindows()
        RealtimeRuntimeBridge.addListener(runtimeBridgeListener)
        observeSettings()
        scope.launch {
            loadQuickSubtitleConfig()
            loadOverlayShortcuts()
            loadOverlayLauncherLayout()
            restoreFabPositionForCurrentOrientation(allowOppositeConversion = true)
            loadLaunchableApps()
            refreshPanelUi()
            refreshQuickSubtitleUi()
            updateFabUi()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> {
                scope.launch {
                    settings = UserPrefs.getSettings(this@FloatingOverlayService)
                    loadQuickSubtitleConfig()
                    loadOverlayShortcuts()
                    loadOverlayLauncherLayout()
                    restoreFabPositionForCurrentOrientation(allowOppositeConversion = true)
                    loadLaunchableApps()
                    refreshPanelUi()
                    refreshQuickSubtitleUi()
                    updateFabUi()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        settingsJob = null
        fabIdleDockJob?.cancel()
        fabIdleDockJob = null
        RealtimeRuntimeBridge.removeListener(runtimeBridgeListener)
        hideConfirmOverlay()
        removeWindows()
        scope.cancel()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        saveFabAnchorForOrientation(
            currentFabOrientation,
            lastFabDisplayWidth.takeIf { it > 0 } ?: displayWidth(),
            lastFabDisplayHeight.takeIf { it > 0 } ?: displayHeight()
        )
        currentFabOrientation = normalizeFabOrientation(newConfig.orientation)
        lastFabDisplayWidth = displayWidth()
        lastFabDisplayHeight = displayHeight()
        val darkNow = isOverlayDarkTheme()
        if (darkNow != overlayDarkTheme) {
            overlayDarkTheme = darkNow
            rebuildWindowsPreservingState()
            return
        }
        fabSnapAnimator?.cancel()
        restoreFabPositionForCurrentOrientation(allowOppositeConversion = true)
        updatePanelPosition()
        updateMiniPanelPosition()
        updatePickerLayout()
        updateConfirmLayout()
        refreshFabIdleDockState()
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = scope.launch {
            UserPrefs.observeSettings(this@FloatingOverlayService).collectLatest { next ->
                settings = next
                if (!next.floatingOverlayEnabled) {
                    stopSelf()
                    return@collectLatest
                }
                refreshQuickSubtitleUi()
                refreshStatusDetailUi()
                updateFabUi()
                refreshFabIdleDockState()
            }
        }
    }

    private fun startForegroundInternal() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "悬浮窗",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                    description = "KIGTTS 悬浮窗正在运行"
                }
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KIGTTS 悬浮窗")
            .setContentText("悬浮窗正在运行")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ensureWindows() {
        if (fabRoot != null) return

        bubbleTextView = TextView(this).apply {
            setTextColor(overlayOnSurfaceColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        bubbleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedRectDrawable(overlayRadiusDp, overlayBubbleColor())
            setPadding(dp(14), dp(10), dp(14), dp(10))
            visibility = View.GONE
            addView(symbolTextView("graphic_eq", 18f, overlayOnSurfaceColor()))
            addView(spaceView(dp(10), 1))
            addView(
                bubbleTextView,
                LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT)
            )
            setOnClickListener {
                val text = latestRecognizedText.trim()
                if (text.isNotEmpty()) {
                    launchQuickSubtitle(OverlayBridge.TARGET_SUBTITLE, text)
                }
            }
        }

        fabIconView = symbolTextView("play_arrow", 30f, Color.WHITE)
        fabButton = FrameLayout(this).apply {
            background = circleDrawable(overlayPrimaryColor())
            elevation = dp(8).toFloat()
            addView(
                fabIconView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            setOnTouchListener { _, event -> handleFabTouch(event) }
        }

        fabRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            gravity = Gravity.END
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(
                bubbleRow,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(spaceView(1, dp(12)))
            addView(
                fabButton,
                LinearLayout.LayoutParams(dp(FAB_SIZE_DP), dp(FAB_SIZE_DP))
            )
        }

        fabParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = displayWidth() - dp(96)
            y = displayHeight() - dp(180)
        }
        windowManager.addView(fabRoot, fabParams)

        val panelTopMicIcon = symbolTextView("mic", 24f, overlayOnSurfaceColor()).also {
            panelStatusMicIconView = it
        }
        panelStatusMicContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(panelTopMicIcon)
            addView(
                ProgressBar(this@FloatingOverlayService, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 1000
                    progress = 0
                    progressTintList = ColorStateList.valueOf(overlayOnSurfaceColor())
                    progressBackgroundTintList =
                        ColorStateList.valueOf(ColorUtils.setAlphaComponent(overlayOnSurfaceColor(), 61))
                    panelStatusMicProgressView = this
                },
                LinearLayout.LayoutParams(dp(18), dp(2)).apply {
                    topMargin = dp(3)
                }
            )
        }
        val panelTopEqIcon = symbolTextView("graphic_eq", 24f, overlayOnSurfaceColor()).also {
            panelStatusEqIconView = it
        }
        panelStatusEqContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(panelTopEqIcon)
            addView(
                ProgressBar(this@FloatingOverlayService, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 1000
                    progress = 0
                    progressTintList = ColorStateList.valueOf(overlayOnSurfaceColor())
                    progressBackgroundTintList =
                        ColorStateList.valueOf(ColorUtils.setAlphaComponent(overlayOnSurfaceColor(), 61))
                    panelStatusEqProgressView = this
                },
                LinearLayout.LayoutParams(dp(18), dp(2)).apply {
                    topMargin = dp(3)
                }
            )
        }
        panelStatusTextView = TextView(this).apply {
            setTextColor(overlayOnSurfaceColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            text = ""
        }
        panelStatusLogoView = ImageView(this).apply {
            setImageResource(if (overlayDarkTheme) R.drawable.logo_white else R.drawable.logo_black)
            adjustViewBounds = true
        }
        panelStatusTriggerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = selectableDrawable()
            }
            setPadding(dp(4), dp(2), dp(4), dp(2))
            addView(panelStatusMicContainer)
            addView(spaceView(dp(12), 1))
            addView(panelStatusEqContainer)
            setOnClickListener { toggleOverlayStatusExpanded() }
        }
        panelStatusDetailRefs = createOverlayStatusDetailCard()
        val panelTopStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(6).toFloat()
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(symbolTextView("arrow_back", 22f, overlayOnSurfaceColor()).apply {
                setOnClickListener { hidePanel() }
            })
            addView(spaceView(dp(10), 1))
            addView(
                FrameLayout(this@FloatingOverlayService).apply {
                    addView(
                        panelStatusTextView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL or Gravity.START
                        )
                    )
                    addView(
                        panelStatusLogoView,
                        FrameLayout.LayoutParams(
                            dp(120),
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL or Gravity.START
                        )
                    )
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(spaceView(dp(10), 1))
            addView(panelStatusTriggerContainer)
        }
        panelTopStripView = panelTopStrip

        panelBuiltInContainer = null
        panelShortcutContainer = null
        panelShortcutScroll = null
        panelPagerAdapter = OverlayLauncherPageAdapter()
        val panelPageHost = ViewPager2(this).apply {
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            overScrollMode = View.OVER_SCROLL_NEVER
            offscreenPageLimit = 1
            adapter = panelPagerAdapter
            registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        collapseOverlayStatusExpanded()
                        val nextIndex = position.coerceIn(0, max(0, panelPageCount - 1))
                        if (panelPageIndex != nextIndex) {
                            panelPageIndex = nextIndex
                            refreshPanelIndicators()
                        }
                        refreshPanelEditPageButtons()
                    }
                }
            )
            setOnDragListener { view, event ->
                when (event.action) {
                    android.view.DragEvent.ACTION_DRAG_STARTED -> true
                    android.view.DragEvent.ACTION_DRAG_LOCATION -> {
                        handlePanelDragLocation(view.width, event.x)
                        true
                    }
                    android.view.DragEvent.ACTION_DROP -> true
                    android.view.DragEvent.ACTION_DRAG_ENDED -> {
                        cancelPendingPanelPageSwitch()
                        true
                    }
                    else -> true
                }
            }
        }
        panelPager = panelPageHost
        val panelPrevPageButton = FrameLayout(this).apply {
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(4).toFloat()
            alpha = 0f
            visibility = View.GONE
            addView(
                symbolTextView("chevron_left", 22f, overlayOnSurfaceColor()),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = selectableDrawable()
            }
            setOnClickListener { switchPanelPageBy(-1) }
        }
        panelPrevPageButtonView = panelPrevPageButton
        val panelNextPageButton = FrameLayout(this).apply {
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(4).toFloat()
            alpha = 0f
            visibility = View.GONE
            addView(
                symbolTextView("chevron_right", 22f, overlayOnSurfaceColor()),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = selectableDrawable()
            }
            setOnClickListener { switchPanelPageBy(1) }
        }
        panelNextPageButtonView = panelNextPageButton

        panelIndicatorContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        panelActionFabIconView = symbolTextView("play_arrow", 30f, Color.WHITE)
        panelActionFab = FrameLayout(this).apply {
            background = circleDrawable(overlayPrimaryColor())
            elevation = dp(8).toFloat()
            addView(
                panelActionFabIconView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            setOnTouchListener { _, event -> handlePanelActionTouch(event) }
        }
        val panelBottomBar = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            elevation = 0f
            clipChildren = false
            clipToPadding = false
            minimumHeight = dp(90)
            addView(
                symbolTextView("edit", 24f, overlayOnSurfaceColor()).apply {
                    panelEditButtonView = this
                    setOnClickListener {
                        panelEditMode = !panelEditMode
                        refreshPanelUi()
                    }
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.START
                ).apply {
                    leftMargin = dp(28)
                }
            )
            addView(
                symbolTextView("open_in_new", 24f, overlayOnSurfaceColor()).apply {
                    panelOpenButtonView = this
                    setOnClickListener {
                        hidePanel()
                        launchQuickSubtitlePage()
                    }
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.END
                ).apply {
                    rightMargin = dp(28)
                }
            )
            addView(
                panelActionFab,
                FrameLayout.LayoutParams(dp(74), dp(74), Gravity.CENTER)
            )
        }

        val panelCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(8).toFloat()
            clipChildren = false
            clipToPadding = false
            setPadding(dp(14), dp(14), dp(14), dp(14))
            val pageHostFrame = FrameLayout(this@FloatingOverlayService).apply {
                clipChildren = false
                clipToPadding = false
                addView(
                    panelPageHost,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
                addView(
                    panelPrevPageButton,
                    FrameLayout.LayoutParams(dp(40), dp(40), Gravity.START or Gravity.CENTER_VERTICAL).apply {
                        marginStart = dp(-6)
                    }
                )
                addView(
                    panelNextPageButton,
                    FrameLayout.LayoutParams(dp(40), dp(40), Gravity.END or Gravity.CENTER_VERTICAL).apply {
                        marginEnd = dp(-6)
                    }
                )
            }
            addView(
                pageHostFrame,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(spaceView(1, dp(8)))
            addView(
                panelIndicatorContainer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(spaceView(1, dp(10)))
            addView(
                panelBottomBar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        panelContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(10), dp(10), dp(10), dp(10))
            visibility = View.GONE
            setOnClickListener { }
            addView(
                panelTopStrip,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(spaceView(1, dp(12)))
            addView(
                panelCard,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        panelRoot = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            isClickable = true
            isFocusable = false
            setOnClickListener {
                if (overlayStatusExpanded) {
                    collapseOverlayStatusExpanded()
                } else {
                    hidePanel()
                }
            }
            addView(
                panelContent,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                panelStatusDetailRefs!!.card,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        windowManager.addView(panelRoot, panelParams)

        val pickerListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        panelPickerListContainer = pickerListContainer
        val pickerSearchInput = EditText(this).apply {
            setTextColor(overlayOnSurfaceColor())
            setHintTextColor(overlayOnSurfaceVariantColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            hint = "搜索应用"
            isSingleLine = true
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            setPadding(dp(14), dp(12), dp(14), dp(12))
            addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        panelPickerSearchQuery = s?.toString().orEmpty()
                        refreshShortcutPickerUi()
                    }
                    override fun afterTextChanged(s: Editable?) = Unit
                }
            )
        }
        panelPickerSearchInput = pickerSearchInput
        panelPickerOverlay = FrameLayout(this).apply {
            visibility = View.GONE
            alpha = 0f
            setBackgroundColor(overlayScrimColor())
            isClickable = true
            isFocusable = false
            setOnClickListener { hideShortcutPicker() }
            addView(
                LinearLayout(this@FloatingOverlayService).apply {
                    orientation = LinearLayout.VERTICAL
                    background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
                    elevation = dp(8).toFloat()
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                    addView(
                        TextView(this@FloatingOverlayService).apply {
                            setTextColor(overlayOnSurfaceColor())
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                            typeface = Typeface.DEFAULT_BOLD
                            text = "添加软件快捷方式"
                        }
                    )
                    addView(spaceView(1, dp(12)))
                    addView(
                        pickerSearchInput,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
                    addView(spaceView(1, dp(12)))
                    addView(
                        ScrollView(this@FloatingOverlayService).apply {
                            isVerticalScrollBarEnabled = false
                            overScrollMode = View.OVER_SCROLL_NEVER
                            addView(
                                pickerListContainer,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            )
                        },
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(360)
                        )
                    )
                },
                FrameLayout.LayoutParams(
                    min(displayWidth() - dp(32), dp(320)),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        panelPickerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }
        windowManager.addView(panelPickerOverlay, panelPickerParams)

        val topStatusMicIcon = symbolTextView("mic", 24f, overlayOnSurfaceColor()).also {
            miniStatusMicIconView = it
        }
        miniStatusMicContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(topStatusMicIcon)
            addView(
                ProgressBar(this@FloatingOverlayService, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 1000
                    progress = 0
                    progressTintList = ColorStateList.valueOf(overlayOnSurfaceColor())
                    progressBackgroundTintList =
                        ColorStateList.valueOf(ColorUtils.setAlphaComponent(overlayOnSurfaceColor(), 61))
                    miniStatusMicProgressView = this
                },
                LinearLayout.LayoutParams(dp(18), dp(2)).apply {
                    topMargin = dp(3)
                }
            )
        }
        val topStatusEqIcon = symbolTextView("graphic_eq", 24f, overlayOnSurfaceColor()).also {
            miniStatusEqIconView = it
        }
        miniStatusEqContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(topStatusEqIcon)
            addView(
                ProgressBar(this@FloatingOverlayService, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 1000
                    progress = 0
                    progressTintList = ColorStateList.valueOf(overlayOnSurfaceColor())
                    progressBackgroundTintList =
                        ColorStateList.valueOf(ColorUtils.setAlphaComponent(overlayOnSurfaceColor(), 61))
                    miniStatusEqProgressView = this
                },
                LinearLayout.LayoutParams(dp(18), dp(2)).apply {
                    topMargin = dp(3)
                }
            )
        }
        miniStatusTextView = TextView(this).apply {
            setTextColor(overlayOnSurfaceColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            text = ""
        }
        miniStatusLogoView = ImageView(this).apply {
            setImageResource(if (overlayDarkTheme) R.drawable.logo_white else R.drawable.logo_black)
            adjustViewBounds = true
        }
        miniStatusTriggerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = selectableDrawable()
            }
            setPadding(dp(4), dp(2), dp(4), dp(2))
            addView(miniStatusMicContainer)
            addView(spaceView(dp(12), 1))
            addView(miniStatusEqContainer)
            setOnClickListener { toggleOverlayStatusExpanded() }
        }
        miniStatusDetailRefs = createOverlayStatusDetailCard()

        val topStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(6).toFloat()
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(symbolTextView("arrow_back", 22f, overlayOnSurfaceColor()))
            addView(spaceView(dp(10), 1))
            addView(
                FrameLayout(this@FloatingOverlayService).apply {
                    addView(
                        miniStatusTextView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL or Gravity.START
                        )
                    )
                    addView(
                        miniStatusLogoView,
                        FrameLayout.LayoutParams(
                            dp(120),
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL or Gravity.START
                        )
                    )
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(spaceView(dp(10), 1))
            addView(miniStatusTriggerContainer)
            setOnClickListener { returnFromMiniToPanel() }
        }
        miniTopStripView = topStrip

        miniSubtitleTextView = TextView(this).apply {
            setTextColor(overlayOnSurfaceColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, quickSubtitleFontSizeSp)
            typeface = if (quickSubtitleBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            gravity = Gravity.START or Gravity.TOP
            maxLines = 5
            ellipsize = TextUtils.TruncateAt.END
            text = quickSubtitleCurrentText
        }
        miniSubtitleSeekBar = SeekBar(this).apply {
            max = 68
            progress = (quickSubtitleFontSizeSp - 28f).roundToInt().coerceIn(0, 68)
            thumbTintList = ColorStateList.valueOf(overlayPrimaryColor())
            progressTintList = ColorStateList.valueOf(overlayPrimaryColor())
            progressBackgroundTintList = ColorStateList.valueOf(overlaySliderTrackColor())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    quickSubtitleFontSizeSp = (28 + progress).toFloat().coerceIn(28f, 96f)
                    miniSubtitleTextView?.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        quickSubtitleFontSizeSp
                    )
                    saveQuickSubtitleConfig()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        val subtitleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(8).toFloat()
            clipChildren = false
            clipToPadding = false
            setPadding(dp(16), dp(16), dp(16), dp(16))
            isClickable = true
            addView(
                miniSubtitleTextView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(180)
                )
            )
            addView(spaceView(1, dp(12)))
            addView(
                LinearLayout(this@FloatingOverlayService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(symbolTextView("zoom_in", 20f, overlayOnSurfaceVariantColor()))
                    addView(spaceView(dp(10), 1))
                    addView(
                        miniSubtitleSeekBar,
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    )
                    addView(spaceView(dp(8), 1))
                    addView(
                        symbolTextView("expand_more", 20f, overlayOnSurfaceColor()).apply {
                            miniQuickCollapseButton = this
                            gravity = Gravity.CENTER
                            setOnClickListener {
                                miniQuickItemsCollapsed = !miniQuickItemsCollapsed
                                refreshQuickSubtitleUi()
                                updateMiniPanelPosition()
                            }
                        },
                        LinearLayout.LayoutParams(dp(28), dp(28))
                    )
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            setOnClickListener { openMiniSubtitlePreview() }
            setOnLongClickListener {
                launchQuickSubtitlePage()
                true
            }
        }

        miniQuickItemsAdapter = MiniQuickTextAdapter()
        miniQuickItemsRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@FloatingOverlayService, RecyclerView.HORIZONTAL, false)
            adapter = miniQuickItemsAdapter
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = true
            clipToPadding = true
            setHasFixedSize(true)
            itemAnimator = null
            setPadding(dp(10), dp(8), dp(10), dp(8))
            val dividerWidth = dp(1).coerceAtLeast(1)
            val dividerInset = dp(12)
            val dividerPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = dividerWidth.toFloat()
                    color =
                        ColorUtils.setAlphaComponent(
                            overlayOutlineColor(),
                            if (overlayDarkTheme) 138 else 112
                        )
                }
            addItemDecoration(
                object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        val position = parent.getChildAdapterPosition(view)
                        outRect.top = 0
                        outRect.bottom = 0
                        outRect.left = if (position <= 0) 0 else dividerWidth
                        outRect.right = 0
                    }

                    override fun onDrawOver(
                        c: Canvas,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        val lineTop = parent.paddingTop + dividerInset
                        val lineBottom = parent.height - parent.paddingBottom - dividerInset
                        if (lineBottom <= lineTop) return
                        for (index in 0 until parent.childCount) {
                            val child = parent.getChildAt(index)
                            val position = parent.getChildAdapterPosition(child)
                            if (position <= 0) continue
                            val lineX = child.left - (dividerWidth / 2f)
                            c.drawLine(lineX, lineTop.toFloat(), lineX, lineBottom.toFloat(), dividerPaint)
                        }
                    }
                }
            )
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        updateMiniQuickItemsEdgeFade(true)
                    }
                }
            )
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updateMiniQuickItemsEdgeFade(false)
            }
        }
        miniQuickItemsContainer = FrameLayout(this).apply {
            clipChildren = true
            clipToPadding = true
            setPadding(0, 0, 0, 0)
            addView(
                miniQuickItemsRecyclerView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            miniQuickItemsLeftFadeView = View(this@FloatingOverlayService).apply {
                alpha = 0f
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(overlayCardColor(), Color.TRANSPARENT)
                    )
            }
            miniQuickItemsRightFadeView = View(this@FloatingOverlayService).apply {
                alpha = 0f
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.RIGHT_LEFT,
                        intArrayOf(overlayCardColor(), Color.TRANSPARENT)
                    )
            }
            addView(
                miniQuickItemsLeftFadeView,
                FrameLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START)
            )
            addView(
                miniQuickItemsRightFadeView,
                FrameLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END)
            )
        }
        val quickItemsScroller = FrameLayout(this).apply {
            clipChildren = true
            clipToPadding = true
            addView(
                miniQuickItemsContainer,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(104)
                )
            )
        }

        miniGroupIconView = symbolTextView("sentiment_satisfied", 22f, overlayOnSurfaceColor()).apply {
            gravity = Gravity.CENTER
            minWidth = dp(36)
            isClickable = true
            isFocusable = true
            setOnTouchListener(createMiniGroupSwipeTouchListener())
        }
        val quickSwitcher = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(2), dp(4), dp(2), dp(4))
            minimumWidth = dp(44)
            addView(
                symbolTextView("keyboard_arrow_up", 20f, overlayOnSurfaceColor()).apply {
                    gravity = Gravity.CENTER
                    setOnClickListener { shiftQuickSubtitleGroup(-1) }
                },
                LinearLayout.LayoutParams(dp(36), 0, 1f)
            )
            addView(
                miniGroupIconView,
                LinearLayout.LayoutParams(dp(36), dp(28)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            )
            addView(
                symbolTextView("keyboard_arrow_down", 20f, overlayOnSurfaceColor()).apply {
                    gravity = Gravity.CENTER
                    setOnClickListener { shiftQuickSubtitleGroup(1) }
                },
                LinearLayout.LayoutParams(dp(36), 0, 1f)
            )
        }

        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = true
            clipToPadding = true
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(6).toFloat()
            setPadding(dp(8), dp(8), dp(8), dp(8))
            addView(
                quickItemsScroller,
                LinearLayout.LayoutParams(0, dp(104), 1f)
            )
            addView(
                View(this@FloatingOverlayService).apply {
                    setBackgroundColor(ColorUtils.setAlphaComponent(overlayOutlineColor(), if (overlayDarkTheme) 112 else 92))
                },
                LinearLayout.LayoutParams(
                    dp(1),
                    dp(84)
                ).apply {
                    leftMargin = dp(8)
                    rightMargin = dp(8)
                }
            )
            addView(
                quickSwitcher,
                LinearLayout.LayoutParams(
                    dp(44),
                    dp(104)
                )
            )
        }
        miniQuickRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(0, 0, 0, 0)
            addView(
                quickRow,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        miniSubtitleBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            addView(
                subtitleCard,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                miniQuickRow,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
            )
        }

        miniQuickCardPreviewContainer =
            object : FrameLayout(this) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val pager = miniQuickCardPager
                    val recycler = pager?.getChildAt(0) as? RecyclerView
                    val currentChild =
                        recycler?.findViewHolderForAdapterPosition(pager.currentItem)?.itemView
                            ?: recycler?.getChildAt(0)
                    val contentWidth = (
                        MeasureSpec.getSize(widthMeasureSpec) -
                            paddingLeft -
                            paddingRight
                        ).coerceAtLeast(0)
                    val targetHeight = if (currentChild != null) {
                        val childWidthSpec = MeasureSpec.makeMeasureSpec(
                            contentWidth,
                            MeasureSpec.EXACTLY
                        )
                        val childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                        currentChild.measure(childWidthSpec, childHeightSpec)
                        currentChild.measuredHeight + paddingTop + paddingBottom
                    } else {
                        dp(320) + paddingTop + paddingBottom
                    }
                    val exactHeight = MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY)
                    super.onMeasure(widthMeasureSpec, exactHeight)
                }
            }.apply {
                clipChildren = false
                clipToPadding = false
                setPadding(dp(2), 0, dp(2), 0)
                miniQuickCardPagerAdapter = MiniQuickCardPagerAdapter()
                miniQuickCardPager = ViewPager2(this@FloatingOverlayService).apply {
                    adapter = miniQuickCardPagerAdapter
                    offscreenPageLimit = 1
                    overScrollMode = View.OVER_SCROLL_NEVER
                    (getChildAt(0) as? RecyclerView)?.apply {
                        overScrollMode = View.OVER_SCROLL_NEVER
                        clipChildren = false
                        clipToPadding = false
                    }
                    registerOnPageChangeCallback(
                        object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                collapseOverlayStatusExpanded()
                                if (quickCards.isEmpty()) return
                                val safeIndex = position.coerceIn(0, quickCards.lastIndex)
                                if (safeIndex != quickCardSelectedIndex) {
                                    quickCardSelectedIndex = safeIndex
                                    saveQuickCardSelectedIndex()
                                }
                                prefetchQuickCardAssets()
                                miniQuickCardPreviewContainer?.requestLayout()
                                refreshMiniQuickCardIndicators()
                            }
                        }
                    )
                }
                addView(
                    miniQuickCardPager,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
        miniQuickCardIndicatorContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        miniQuickCardBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            clipChildren = false
            clipToPadding = false
            addView(
                miniQuickCardPreviewContainer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                miniQuickCardIndicatorContainer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(10)
                }
            )
        }
        val miniBodyHost = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            addView(
                miniSubtitleBody,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                miniQuickCardBody,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        miniActionFabIconView = symbolTextView("play_arrow", 30f, Color.WHITE)
        miniActionFab = FrameLayout(this).apply {
            background = circleDrawable(overlayPrimaryColor())
            elevation = dp(8).toFloat()
            addView(
                miniActionFabIconView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            setOnTouchListener { _, event -> handleMiniActionTouch(event) }
        }
        val bottomBar = FrameLayout(this).apply {
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(6).toFloat()
            minimumHeight = dp(90)
            addView(
                symbolTextView("arrow_back", 26f, overlayOnSurfaceColor()).apply {
                    miniBackButtonView = this
                    setOnClickListener { returnFromMiniToPanel() }
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.START
                ).apply {
                    leftMargin = dp(28)
                }
            )
            addView(
                symbolTextView("open_in_new", 26f, overlayOnSurfaceColor()).apply {
                    miniOpenButtonView = this
                    setOnClickListener {
                        if (miniMode == MiniOverlayMode.QuickCard) {
                            launchQuickCardPage()
                        } else {
                            launchQuickSubtitlePage()
                        }
                    }
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.END
                ).apply {
                    rightMargin = dp(28)
                }
            )
            addView(
                miniActionFab,
                FrameLayout.LayoutParams(dp(74), dp(74), Gravity.CENTER)
            )
        }

        miniContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(10), dp(10), dp(10), dp(10))
            visibility = View.GONE
            setOnClickListener { }
            addView(
                topStrip,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(spaceView(1, dp(12)))
            addView(
                miniBodyHost,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                bottomBar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
            )
        }

        miniRoot = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            isClickable = true
            isFocusable = false
            setOnClickListener {
                if (overlayStatusExpanded) {
                    collapseOverlayStatusExpanded()
                } else {
                    hideMiniPanel()
                }
            }
            addView(
                miniContent,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        miniPreviewHost = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            isClickable = true
            setOnClickListener { }
        }
        miniPreviewOverlay = FrameLayout(this).apply {
            visibility = View.GONE
            alpha = 0f
            setBackgroundColor(overlayPreviewScrimColor())
            clipChildren = false
            clipToPadding = false
            isClickable = true
            setOnClickListener { closeMiniPreview() }
            addView(
                miniPreviewHost,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        miniRoot?.addView(
            miniStatusDetailRefs!!.card,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        miniRoot?.addView(
            miniPreviewOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        miniParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        windowManager.addView(miniRoot, miniParams)

        confirmTextView = TextView(this).apply {
            setTextColor(overlayOnSurfaceColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            maxLines = 5
            includeFontPadding = false
            text = "正在识别…"
        }

        leftActionButton = circleActionButton(symbolTextView("open_in_new", 26f, Color.WHITE), 64)
        rightActionButton = circleActionButton(symbolTextView("close", 26f, Color.WHITE), 64)
        confirmTextCardView = FrameLayout(this).apply {
            background = roundedRectDrawable(overlayRadiusDp, ColorUtils.setAlphaComponent(overlayCardColor(), 255))
            elevation = dp(6).toFloat()
            alpha = 1f
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(
                confirmTextView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        confirmClipContainer = FrameLayout(this).apply {
            clipChildren = true
            clipToPadding = true
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            addView(
                confirmTextCardView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                leftActionButton,
                FrameLayout.LayoutParams(dp(64), dp(64))
            )
            addView(
                rightActionButton,
                FrameLayout.LayoutParams(dp(64), dp(64))
            )
        }

        confirmOverlay = FrameLayout(this).apply {
            visibility = View.GONE
            isClickable = false
            isFocusable = false
            setBackgroundColor(Color.TRANSPARENT)
            addView(
                confirmClipContainer,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            alpha = 0f
        }
        confirmParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        windowManager.addView(confirmOverlay, confirmParams)
        refreshQuickSubtitleUi()
        updateFabUi()
    }

    private fun removeWindows() {
        fabSnapAnimator?.cancel()
        fabSnapAnimator = null
        confirmOverlay?.let { runCatching { windowManager.removeView(it) } }
        confirmOverlay = null
        confirmTextCardView = null
        confirmClipContainer = null
        confirmTextView = null
        leftActionButton = null
        rightActionButton = null
        panelPickerOverlay?.let { runCatching { windowManager.removeView(it) } }
        panelPickerOverlay = null
        panelPickerListContainer = null
        panelPickerSearchInput = null
        panelRoot?.let { runCatching { windowManager.removeView(it) } }
        panelRoot = null
        panelContent = null
        panelStatusTextView = null
        panelStatusLogoView = null
        panelStatusMicContainer = null
        panelStatusEqContainer = null
        panelStatusMicIconView = null
        panelStatusEqIconView = null
        panelStatusMicProgressView = null
        panelStatusEqProgressView = null
        panelStatusTriggerContainer = null
        panelStatusDetailRefs = null
        panelIndicatorContainer = null
        panelActionFab = null
        panelActionFabIconView = null
        panelEditButtonView = null
        panelOpenButtonView = null
        panelPrevPageButtonView = null
        panelNextPageButtonView = null
        panelPager = null
        panelPagerAdapter = null
        miniRoot?.let { runCatching { windowManager.removeView(it) } }
        miniRoot = null
        miniContent = null
        miniStatusTextView = null
        miniStatusLogoView = null
        miniStatusMicContainer = null
        miniStatusEqContainer = null
        miniStatusMicIconView = null
        miniStatusEqIconView = null
        miniStatusMicProgressView = null
        miniStatusEqProgressView = null
        miniStatusTriggerContainer = null
        miniStatusDetailRefs = null
        miniSubtitleTextView = null
        miniSubtitleSeekBar = null
        miniQuickItemsContainer = null
        miniQuickItemsLeftFadeView = null
        miniQuickItemsRightFadeView = null
        miniQuickRow = null
        miniGroupIconView = null
        miniQuickCollapseButton = null
        miniQuickCardBody = null
        miniQuickCardPreviewContainer = null
        miniQuickCardIndicatorContainer = null
        miniQuickCardItemsContainer = null
        miniQuickCardPager = null
        miniQuickCardPagerAdapter = null
        miniPreviewOverlay = null
        miniPreviewHost = null
        miniActionFab = null
        miniActionFabIconView = null
        miniBackButtonView = null
        miniOpenButtonView = null
        fabRoot?.let { runCatching { windowManager.removeView(it) } }
        fabRoot = null
        fabButton = null
        fabIconView = null
        bubbleRow = null
        bubbleTextView = null
    }

    private fun rebuildWindowsPreservingState() {
        saveFabAnchorForOrientation(
            currentFabOrientation,
            lastFabDisplayWidth.takeIf { it > 0 } ?: displayWidth(),
            lastFabDisplayHeight.takeIf { it > 0 } ?: displayHeight()
        )
        val wasPanelVisible = panelVisible
        val wasMiniVisible = miniVisible
        val wasPickerVisible = panelPickerOverlay?.visibility == View.VISIBLE
        removeWindows()
        ensureWindows()
        restoreFabPositionForCurrentOrientation(allowOppositeConversion = false)
        panelVisible = wasPanelVisible
        miniVisible = wasMiniVisible
        if (wasPanelVisible) updatePanelPosition()
        if (wasMiniVisible) updateMiniPanelPosition()
        if (wasPickerVisible) showShortcutPicker()
        refreshPanelUi()
        refreshQuickSubtitleUi()
        refreshQuickCardUi()
        refreshMiniPreviewUi()
        updateFabUi()
        refreshFabIdleDockState()
    }

    private fun updateFabUi() {
        val runningState = effectiveRunningState()
        val latestText = effectiveLatestRecognizedText()
        val inputLevel = effectiveInputLevel()
        val playbackProgress = effectivePlaybackProgress()
        val pttPressedState = effectivePttPressedState()
        val icon = when {
            settings.pushToTalkMode && pttPressedState -> "settings_voice"
            settings.pushToTalkMode -> "mic"
            runningState -> "stop"
            else -> "play_arrow"
        }
        fabIconView?.text = icon
        panelActionFabIconView?.text = icon
        miniActionFabIconView?.text = icon
        bubbleTextView?.text = latestText
        bubbleRow?.visibility = View.GONE
        val hasLatestResult = latestText.isNotBlank()
        val topMicIcon = if (settings.pushToTalkMode && pttPressedState) "settings_voice" else "mic"
        panelStatusTextView?.text = latestText
        panelStatusTextView?.visibility = if (hasLatestResult) View.VISIBLE else View.INVISIBLE
        panelStatusLogoView?.visibility = if (hasLatestResult) View.GONE else View.VISIBLE
        panelStatusMicIconView?.text = topMicIcon
        panelStatusEqIconView?.text = "graphic_eq"
        panelStatusMicProgressView?.progress = (inputLevel * 1000f).roundToInt().coerceIn(0, 1000)
        panelStatusEqProgressView?.progress = (playbackProgress * 1000f).roundToInt().coerceIn(0, 1000)
        panelStatusMicContainer?.alpha = if (settings.pushToTalkMode || pttPressedState) 1f else 0.68f
        panelStatusEqContainer?.alpha = if (runningState || hasLatestResult) 1f else 0.68f
        miniStatusTextView?.text = latestText
        miniStatusTextView?.visibility = if (hasLatestResult) View.VISIBLE else View.INVISIBLE
        miniStatusLogoView?.visibility = if (hasLatestResult) View.GONE else View.VISIBLE
        miniStatusMicIconView?.text = topMicIcon
        miniStatusEqIconView?.text = "graphic_eq"
        miniStatusMicProgressView?.progress = (inputLevel * 1000f).roundToInt().coerceIn(0, 1000)
        miniStatusEqProgressView?.progress = (playbackProgress * 1000f).roundToInt().coerceIn(0, 1000)
        miniStatusMicContainer?.alpha = if (settings.pushToTalkMode || pttPressedState) 1f else 0.68f
        miniStatusEqContainer?.alpha = if (runningState || hasLatestResult) 1f else 0.68f
        refreshStatusDetailUi()
        animateFabVisibility(!(miniVisible || panelVisible))
        fabButton?.alpha = if (pttPressedState) 0.94f else 1f
        panelActionFab?.alpha = if (pttPressedState) 0.94f else 1f
        miniActionFab?.alpha = if (pttPressedState) 0.94f else 1f
        if (fabIdleDocked) {
            bubbleRow?.visibility = View.GONE
        }
        refreshFabIdleDockState()
    }

    private fun updateConfirmLayout() {
        if (confirmOverlay?.visibility == View.VISIBLE) {
            updateConfirmVisuals(currentDragAction)
            layoutConfirmOverlayContents()
        }
        if (panelVisible) updatePanelPosition()
        if (miniVisible) updateMiniPanelPosition()
    }

    private fun showConfirmOverlay() {
        val overlay = confirmOverlay ?: return
        syncConfirmOverlayToActiveWindow()
        overlay.visibility = View.VISIBLE
        updateConfirmHostChrome(true)
        overlay.animate().cancel()
        overlay.animate().alpha(1f).setDuration(140L).start()
        updateConfirmVisuals(currentDragAction)
        overlay.post { layoutConfirmOverlayContents() }
    }

    private fun hideConfirmOverlay() {
        val overlay = confirmOverlay ?: return
        overlay.animate().cancel()
        overlay.alpha = 0f
        overlay.visibility = View.GONE
        updateConfirmHostChrome(false)
    }

    private fun updateConfirmHostChrome(visible: Boolean) {
        val sideVisibility = if (visible) View.INVISIBLE else View.VISIBLE
        panelEditButtonView?.visibility = sideVisibility
        panelOpenButtonView?.visibility = sideVisibility
        miniBackButtonView?.visibility = sideVisibility
        miniOpenButtonView?.visibility = sideVisibility
    }

    private fun updateConfirmVisuals(action: OverlayReleaseAction) {
        val prompt = when (action) {
            OverlayReleaseAction.SendToSubtitle -> "松手悬浮窗上屏"
            OverlayReleaseAction.SendToInput -> "松手打开快捷字幕并输入"
            OverlayReleaseAction.Cancel -> "松手取消发送"
        }
        confirmTextView?.text = effectivePttStreamingText().ifBlank { prompt }
        leftActionButton?.alpha = if (action == OverlayReleaseAction.SendToInput) 1f else 0.58f
        rightActionButton?.alpha = if (action == OverlayReleaseAction.Cancel) 1f else 0.58f
        activeConfirmFab()?.alpha = if (action == OverlayReleaseAction.SendToSubtitle) 1f else 0.84f
    }

    private fun mapCommitAction(action: OverlayReleaseAction): RealtimeRuntimeBridge.PttCommitAction =
        when (action) {
            OverlayReleaseAction.SendToSubtitle -> RealtimeRuntimeBridge.PttCommitAction.SendToSubtitle
            OverlayReleaseAction.SendToInput -> RealtimeRuntimeBridge.PttCommitAction.SendToInput
            OverlayReleaseAction.Cancel -> RealtimeRuntimeBridge.PttCommitAction.Cancel
        }

    private fun beginPttSession() {
        pttPressed = true
        pttTemporaryStart = !effectiveRunningState()
        pttSessionLastText = ""
        pttStreamingText = ""
        currentDragAction = OverlayReleaseAction.SendToSubtitle
        updateFabUi()
        if (settings.pushToTalkConfirmInput) showConfirmOverlay()
        scope.launch {
            val appDelegate = RealtimeRuntimeBridge.currentAppDelegate()
            if (appDelegate == null) {
                pttPressed = false
                pttTemporaryStart = false
                hideConfirmOverlay()
                latestRecognizedText = "请先打开主界面"
                updateFabUi()
                return@launch
            }
            if (pttTemporaryStart) appDelegate.startRealtime()
            appDelegate.beginPushToTalkSession()
            appDelegate.setPushToTalkPressed(true)
        }
    }

    private fun finishPttSession(action: OverlayReleaseAction) {
        val text = effectivePttStreamingText().trim()
        val shouldStop = pttTemporaryStart
        pttPressed = false
        pttTemporaryStart = false
        pttSessionLastText = ""
        pttStreamingText = ""
        hideConfirmOverlay()
        scope.launch {
            val appDelegate = RealtimeRuntimeBridge.currentAppDelegate()
            if (appDelegate != null) {
                appDelegate.commitPushToTalkSession(mapCommitAction(action))
                appDelegate.setPushToTalkPressed(false)
                if (shouldStop) appDelegate.stopRealtime()
            } else if (text.isNotEmpty()) {
                when (action) {
                    OverlayReleaseAction.SendToSubtitle -> {
                        launchQuickSubtitle(OverlayBridge.TARGET_SUBTITLE, text)
                    }
                    OverlayReleaseAction.SendToInput -> {
                        launchQuickSubtitle(OverlayBridge.TARGET_INPUT, text)
                    }
                    OverlayReleaseAction.Cancel -> Unit
                }
            }
        }
        if (action == OverlayReleaseAction.SendToInput && text.isNotEmpty()) {
            launchQuickSubtitle(OverlayBridge.TARGET_OPEN, "")
        }
        updateFabUi()
    }

    private fun submitQuickSubtitleText(text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        latestRecognizedText = normalized
        quickSubtitleCurrentText = normalized
        saveQuickSubtitleConfig()
        refreshQuickSubtitleUi()
        updateFabUi()
        val appDelegate = RealtimeRuntimeBridge.currentAppDelegate()
        if (appDelegate != null) {
            appDelegate.submitQuickSubtitle(OverlayBridge.TARGET_SUBTITLE, normalized)
        } else {
            launchQuickSubtitle(OverlayBridge.TARGET_SUBTITLE, normalized)
        }
    }

    private suspend fun startListeningInternal(showFailureInBubble: Boolean): Boolean {
        if (effectiveRunningState()) return true
        val appDelegate = RealtimeRuntimeBridge.currentAppDelegate()
        if (appDelegate == null) {
            if (showFailureInBubble) {
                latestRecognizedText = "请先打开主界面"
                updateFabUi()
            }
            return false
        }
        appDelegate.startRealtime()
        running = true
        updateFabUi()
        return true
    }

    private suspend fun stopListeningInternal() {
        val appDelegate = RealtimeRuntimeBridge.currentAppDelegate()
        if (appDelegate != null) {
            appDelegate.stopRealtime()
        }
        running = false
        updateFabUi()
    }

    private fun toggleContinuousMode() {
        scope.launch {
            if (effectiveRunningState()) {
                stopListeningInternal()
            } else {
                startListeningInternal(true)
            }
        }
    }

    private fun launchQuickSubtitle(target: String, text: String) {
        runCatching {
            startActivity(OverlayBridge.buildQuickSubtitleIntent(this, target, text))
        }.onFailure {
            AppLogger.e("FloatingOverlayService.launchQuickSubtitle failed", it)
        }
    }

    private fun launchQuickSubtitlePage() {
        hideMiniPanel()
        launchQuickSubtitle(OverlayBridge.TARGET_OPEN, "")
    }

    private fun launchQuickCardPage() {
        hideMiniPanel()
        launchAppPage(OverlayBridge.TARGET_OPEN_QUICK_CARD)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleFabTouch(event: MotionEvent): Boolean {
        val params = fabParams ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelFabIdleDock(restoreFab = true)
                fabSnapAnimator?.cancel()
                downRawX = event.rawX
                downRawY = event.rawY
                downWinX = params.x
                downWinY = params.y
                draggingFab = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - downRawX).roundToInt()
                val dy = (event.rawY - downRawY).roundToInt()
                if (!draggingFab && (abs(dx) > dp(6) || abs(dy) > dp(6))) draggingFab = true
                if (draggingFab) {
                    params.x = downWinX + dx
                    params.y = downWinY + dy
                    clampFabToScreen()
                    windowManager.updateViewLayout(fabRoot, params)
                    if (!panelVisible) updatePanelPosition()
                    if (!miniVisible) updateMiniPanelPosition()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!draggingFab) togglePanel() else snapFabToEdge()
                draggingFab = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (draggingFab) snapFabToEdge()
                draggingFab = false
                refreshFabIdleDockState()
                return true
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handlePanelActionTouch(event: MotionEvent): Boolean {
        return handleSharedActionTouch(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleMiniActionTouch(event: MotionEvent): Boolean {
        return handleSharedActionTouch(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleSharedActionTouch(event: MotionEvent): Boolean {
        if (settings.pushToTalkMode) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    beginPttSession()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (settings.pushToTalkConfirmInput) {
                        currentDragAction = resolveConfirmAction(event.rawX, event.rawY)
                        updateConfirmVisuals(currentDragAction)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val action = if (settings.pushToTalkConfirmInput) {
                        resolveConfirmAction(event.rawX, event.rawY)
                    } else {
                        OverlayReleaseAction.SendToSubtitle
                    }
                    finishPttSession(action)
                    return true
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_OUTSIDE -> {
                    finishPttSession(OverlayReleaseAction.Cancel)
                    return true
                }
            }
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            toggleContinuousMode()
            return true
        }
        return true
    }

    private fun togglePanel() {
        if (panelVisible) hidePanel() else showPanel()
    }

    private fun showPanel() {
        scope.launch {
            collapseOverlayStatusExpanded(animate = false)
            loadOverlayShortcuts()
            loadOverlayLauncherLayout()
            refreshPanelUi()
            updatePanelPosition()
            panelRoot?.visibility = View.VISIBLE
            if (miniVisible) {
                panelVisible = true
                updateFabUi()
                animateOverlaySwitch(
                    outgoing = miniContent,
                    incoming = panelContent,
                    outgoingEnd = {
                        miniVisible = false
                        miniRoot?.visibility = View.GONE
                        updateFabUi()
                    }
                )
            } else {
                panelVisible = true
                miniVisible = false
                updateFabUi()
                animateOverlayIn(panelContent, fromBottom = true)
            }
            updateFabUi()
        }
    }

    private fun hidePanel() {
        if (!panelVisible) return
        panelEditMode = false
        cancelPendingPanelPageSwitch()
        hideShortcutPicker()
        animateOverlayOut(panelContent) {
            panelVisible = false
            panelRoot?.visibility = View.GONE
            updateFabUi()
        }
    }

    private fun showMiniPanel(mode: MiniOverlayMode = MiniOverlayMode.Subtitle) {
        scope.launch {
            collapseOverlayStatusExpanded(animate = false)
            miniMode = mode
            when (miniMode) {
                MiniOverlayMode.Subtitle -> {
                    loadQuickSubtitleConfig()
                    refreshQuickSubtitleUi()
                }
                MiniOverlayMode.QuickCard -> {
                    loadQuickCardConfig()
                    refreshQuickCardUi()
                }
            }
            refreshMiniModeUi()
            updateMiniPanelPosition()
            refreshMiniPreviewUi()
            miniRoot?.visibility = View.VISIBLE
            if (panelVisible) {
                miniVisible = true
                updateFabUi()
                animateOverlaySwitch(
                    outgoing = panelContent,
                    incoming = miniContent,
                    outgoingEnd = {
                        panelVisible = false
                        panelEditMode = false
                        panelRoot?.visibility = View.GONE
                        hideShortcutPicker()
                        updateFabUi()
                    }
                )
            } else {
                miniVisible = true
                updateFabUi()
                animateOverlayIn(miniContent, fromBottom = false)
            }
        }
    }

    private fun hideMiniPanel() {
        if (!miniVisible) return
        closeMiniPreview()
        animateOverlayOut(miniContent) {
            miniVisible = false
            miniRoot?.visibility = View.GONE
            updateFabUi()
        }
    }

    private fun returnFromMiniToPanel() {
        closeMiniPreview()
        showPanel()
    }

    private fun refreshMiniModeUi() {
        miniSubtitleBody?.visibility = if (miniMode == MiniOverlayMode.Subtitle) View.VISIBLE else View.GONE
        miniQuickCardBody?.visibility = if (miniMode == MiniOverlayMode.QuickCard) View.VISIBLE else View.GONE
    }

    private fun animateOverlayIn(view: View?, fromBottom: Boolean) {
        view ?: return
        view.animate().cancel()
        view.alpha = 0f
        view.translationY = if (fromBottom) dp(36).toFloat() else dp(16).toFloat()
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220L)
            .start()
    }

    private fun animateOverlayOut(view: View?, endAction: (() -> Unit)? = null) {
        view ?: run {
            endAction?.invoke()
            return
        }
        view.animate().cancel()
        view.animate()
            .alpha(0f)
            .translationY(dp(18).toFloat())
            .setDuration(180L)
            .withEndAction {
                view.alpha = 1f
                view.translationY = 0f
                view.visibility = View.GONE
                endAction?.invoke()
            }
            .start()
    }

    private fun animateFabVisibility(show: Boolean) {
        val root = fabRoot ?: return
        root.animate().cancel()
        if (show) {
            if (root.visibility != View.VISIBLE) {
                root.alpha = 0f
                root.scaleX = 0.88f
                root.scaleY = 0.88f
                root.visibility = View.VISIBLE
            }
            root.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .start()
        } else if (root.visibility == View.VISIBLE) {
            root.animate()
                .alpha(0f)
                .scaleX(0.88f)
                .scaleY(0.88f)
                .setDuration(160L)
                .withEndAction {
                    if (!panelVisible && !miniVisible) {
                        root.alpha = 1f
                        root.scaleX = 1f
                        root.scaleY = 1f
                    } else {
                        root.visibility = View.GONE
                        root.alpha = 1f
                        root.scaleX = 1f
                        root.scaleY = 1f
                    }
                }
                .start()
        }
    }

    private fun animateOverlaySwitch(
        outgoing: View?,
        incoming: View?,
        outgoingEnd: (() -> Unit)? = null
    ) {
        incoming ?: return
        outgoing?.animate()?.cancel()
        incoming.animate().cancel()
        incoming.alpha = 0f
        incoming.translationY = dp(18).toFloat()
        incoming.visibility = View.VISIBLE
        incoming.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220L)
            .start()
        outgoing?.animate()
            ?.alpha(0f)
            ?.translationY(dp(42).toFloat())
            ?.setDuration(220L)
            ?.withEndAction {
                outgoing.alpha = 1f
                outgoing.translationY = 0f
                outgoing.visibility = View.VISIBLE
                outgoingEnd?.invoke()
            }
            ?.start() ?: outgoingEnd?.invoke()
    }

    private fun updatePanelPosition() {
        val root = panelRoot ?: return
        val content = panelContent ?: return
        val params = panelParams ?: return
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.x = 0
        params.y = 0
        val contentWidth = overlayContentWidthPx(phoneMaxDp = 360, tabletMaxDp = 400)
        content.measure(
            View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(displayHeight(), View.MeasureSpec.AT_MOST)
        )
        val targetHeight = content.measuredHeight
        val targetX = overlayContentLeftPx(contentWidth)
        val targetY = overlayContentTopPx(targetHeight)
        content.layoutParams = FrameLayout.LayoutParams(
            contentWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = targetX
            topMargin = targetY
        }
        positionStatusDetailCard(
            content = content,
            topStrip = panelTopStripView,
            card = panelStatusDetailRefs?.card
        )
        runCatching { windowManager.updateViewLayout(root, params) }
        syncConfirmOverlayToActiveWindow()
    }

    private fun updateMiniPanelPosition() {
        val root = miniRoot ?: return
        val content = miniContent ?: return
        val params = miniParams ?: return
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.x = 0
        params.y = 0
        val contentWidth = overlayContentWidthPx(phoneMaxDp = 360, tabletMaxDp = 400)
        content.measure(
            View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(displayHeight(), View.MeasureSpec.AT_MOST)
        )
        val targetHeight = content.measuredHeight
        val targetX = overlayContentLeftPx(contentWidth)
        val targetY = overlayContentTopPx(targetHeight)
        content.layoutParams = FrameLayout.LayoutParams(
            contentWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = targetX
            topMargin = targetY
        }
        positionStatusDetailCard(
            content = content,
            topStrip = miniTopStripView,
            card = miniStatusDetailRefs?.card
        )
        runCatching { windowManager.updateViewLayout(root, params) }
        syncConfirmOverlayToActiveWindow()
    }

    private fun positionStatusDetailCard(
        content: LinearLayout,
        topStrip: View?,
        card: LinearLayout?
    ) {
        card ?: return
        val contentLp = content.layoutParams as? FrameLayout.LayoutParams ?: return
        val insetLeft = content.paddingLeft
        val insetRight = content.paddingRight
        val insetTop = content.paddingTop
        val cardWidth = (
            content.measuredWidth.takeIf { it > 0 }
                ?: content.width.takeIf { it > 0 }
                ?: ((contentLp.width.takeIf { it > 0 } ?: overlayContentWidthPx(phoneMaxDp = 360, tabletMaxDp = 400)))
            ) - insetLeft - insetRight
        val topStripHeight = topStrip?.measuredHeight?.takeIf { it > 0 } ?: 0
        card.layoutParams = ((card.layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)).apply {
            width = cardWidth.coerceAtLeast(dp(240))
            gravity = Gravity.TOP or Gravity.START
            leftMargin = contentLp.leftMargin + insetLeft
            topMargin = contentLp.topMargin + insetTop + topStripHeight + dp(8)
        }
        card.bringToFront()
    }

    private fun isTabletUi(): Boolean = resources.configuration.smallestScreenWidthDp >= 600

    private fun isLandscapeUi(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun isTabletLandscapeUi(): Boolean = isTabletUi() && isLandscapeUi()

    private fun isFabAnchoredRight(): Boolean {
        val params = fabParams
        val centerX = (params?.x ?: (displayWidth() - dp(96))) + dp(FAB_SIZE_DP) / 2
        return centerX >= displayWidth() / 2
    }

    private fun overlayContentWidthPx(phoneMaxDp: Int, tabletMaxDp: Int): Int {
        val sideMargin = dp(16)
        val phoneMax = dp(phoneMaxDp)
        val tabletMax = dp(tabletMaxDp)
        return if (isTabletLandscapeUi()) {
            min(displayWidth() / 2 - sideMargin * 2, tabletMax)
        } else {
            min(displayWidth() - sideMargin * 2, phoneMax)
        }.coerceAtLeast(dp(280))
    }

    private fun overlayContentLeftPx(contentWidth: Int): Int {
        val sideMargin = dp(16)
        if (!isTabletLandscapeUi()) {
            return ((displayWidth() - contentWidth) / 2).coerceAtLeast(sideMargin)
        }
        val regionCenter = if (isFabAnchoredRight()) {
            displayWidth() * 3 / 4
        } else {
            displayWidth() / 4
        }
        return (regionCenter - contentWidth / 2)
            .coerceIn(sideMargin, max(sideMargin, displayWidth() - contentWidth - sideMargin))
    }

    private fun overlayContentTopPx(contentHeight: Int): Int {
        val topBottomMargin = dp(20)
        return ((displayHeight() - contentHeight) / 2)
            .coerceIn(topBottomMargin, max(topBottomMargin, displayHeight() - contentHeight - topBottomMargin))
    }

    private suspend fun loadQuickSubtitleConfig() {
        val raw = UserPrefs.getQuickSubtitleConfig(this)
        if (raw.isNullOrBlank()) {
            quickSubtitleGroups = defaultQuickSubtitleGroups()
            quickSubtitleSelectedGroupId = quickSubtitleGroups.first().id
            quickSubtitleCurrentText = defaultQuickSubtitleText
            quickSubtitleInputText = ""
            quickSubtitleFontSizeSp = 56f
            quickSubtitlePlayOnSend = true
            quickSubtitleBold = true
            quickSubtitleCentered = false
            quickSubtitleNextGroupId = 4L
            return
        }
        runCatching { parseQuickSubtitleConfig(raw) }
            .onFailure { AppLogger.e("FloatingOverlayService.parseQuickSubtitleConfig failed", it) }
    }

    private fun parseQuickSubtitleConfig(raw: String) {
        val root = JSONObject(raw)
        val groupsArr = root.optJSONArray("groups") ?: JSONArray()
        val parsedGroups = mutableListOf<QuickSubtitleGroupConfig>()
        var maxId = 0L
        for (i in 0 until groupsArr.length()) {
            val obj = groupsArr.optJSONObject(i) ?: continue
            val id = obj.optLong("id", i.toLong() + 1L).coerceAtLeast(1L)
            val title = obj.optString("title", "未命名分组").ifBlank { "未命名分组" }
            val icon = obj.optString("icon", "sentiment_satisfied").ifBlank { "sentiment_satisfied" }
            val itemsArr = obj.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (j in 0 until itemsArr.length()) {
                    val text = itemsArr.optString(j, "").trim()
                    if (text.isNotEmpty()) add(text)
                }
            }.ifEmpty { listOf("请输入常用短句") }
            parsedGroups += QuickSubtitleGroupConfig(
                id = id,
                title = title,
                icon = icon,
                items = items
            )
            maxId = max(maxId, id)
        }
        val finalGroups = if (parsedGroups.isNotEmpty()) parsedGroups else defaultQuickSubtitleGroups()
        quickSubtitleGroups = finalGroups
        quickSubtitleSelectedGroupId =
            finalGroups.firstOrNull { it.id == root.optLong("selectedGroupId", finalGroups.first().id) }?.id
                ?: finalGroups.first().id
        quickSubtitleFontSizeSp = root.optDouble("fontSizeSp", 56.0).toFloat().coerceIn(28f, 96f)
        quickSubtitleCurrentText =
            root.optString("currentText", defaultQuickSubtitleText).ifBlank { defaultQuickSubtitleText }
        quickSubtitleInputText = root.optString("inputText", "")
        quickSubtitlePlayOnSend = root.optBoolean("playOnSend", true)
        quickSubtitleBold = root.optBoolean("fontBold", true)
        quickSubtitleCentered = root.optBoolean("textCentered", false)
        quickSubtitleNextGroupId = maxOf(maxId + 1L, 4L)
    }

    private fun saveQuickSubtitleConfig() {
        if (quickSubtitleSaving) return
        quickSubtitleSaving = true
        val payload = JSONObject().apply {
            put("selectedGroupId", quickSubtitleSelectedGroupId)
            put("fontSizeSp", quickSubtitleFontSizeSp.toDouble())
            put("currentText", quickSubtitleCurrentText)
            put("inputText", quickSubtitleInputText)
            put("playOnSend", quickSubtitlePlayOnSend)
            put("fontBold", quickSubtitleBold)
            put("textCentered", quickSubtitleCentered)
            val groupsArray = JSONArray()
            quickSubtitleGroups.forEach { group ->
                val itemsArray = JSONArray()
                group.items.forEach { item -> itemsArray.put(item) }
                groupsArray.put(
                    JSONObject().apply {
                        put("id", group.id)
                        put("title", group.title)
                        put("icon", group.icon)
                        put("items", itemsArray)
                    }
                )
            }
            put("groups", groupsArray)
        }.toString()
        scope.launch(Dispatchers.IO) {
            try {
                UserPrefs.setQuickSubtitleConfig(this@FloatingOverlayService, payload)
            } finally {
                quickSubtitleSaving = false
            }
        }
    }

    private fun selectedQuickSubtitleGroup(): QuickSubtitleGroupConfig {
        return quickSubtitleGroups.firstOrNull { it.id == quickSubtitleSelectedGroupId }
            ?: quickSubtitleGroups.firstOrNull()
            ?: defaultQuickSubtitleGroups().first()
    }

    private fun shiftQuickSubtitleGroup(delta: Int) {
        if (quickSubtitleGroups.isEmpty()) return
        val currentIndex = quickSubtitleGroups.indexOfFirst { it.id == quickSubtitleSelectedGroupId }
            .coerceAtLeast(0)
        val nextIndex = (currentIndex + delta).floorMod(quickSubtitleGroups.size)
        quickSubtitleSelectedGroupId = quickSubtitleGroups[nextIndex].id
        saveQuickSubtitleConfig()
        refreshQuickSubtitleUi()
    }

    private fun refreshQuickSubtitleUi() {
        val group = selectedQuickSubtitleGroup()
        miniSubtitleTextView?.apply {
            text = quickSubtitleCurrentText.ifBlank { defaultQuickSubtitleText }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, quickSubtitleFontSizeSp)
            gravity = if (quickSubtitleCentered) {
                Gravity.CENTER
            } else {
                Gravity.START or Gravity.TOP
            }
            typeface = if (quickSubtitleBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            layoutParams = (layoutParams as? LinearLayout.LayoutParams)?.apply {
                height = if (miniQuickItemsCollapsed) dp(296) else dp(180)
            }
            requestLayout()
        }
        miniSubtitleSeekBar?.progress =
            (quickSubtitleFontSizeSp - 28f).roundToInt().coerceIn(0, 68)
        miniGroupIconView?.text = group.icon
        miniQuickCollapseButton?.text =
            if (miniQuickItemsCollapsed) "expand_less" else "expand_more"
        miniQuickRow?.visibility = if (miniQuickItemsCollapsed) View.GONE else View.VISIBLE
        miniQuickRow?.requestLayout()
        miniQuickItemsAdapter?.submitItems(group.items)
        miniQuickItemsRecyclerView?.scrollToPosition(0)
        miniQuickItemsRecyclerView?.post { updateMiniQuickItemsEdgeFade(false) }
        refreshMiniPreviewUi()
    }

    private fun updateMiniQuickItemsEdgeFade(animate: Boolean) {
        val recycler = miniQuickItemsRecyclerView ?: return
        val leftFade = miniQuickItemsLeftFadeView ?: return
        val rightFade = miniQuickItemsRightFadeView ?: return
        val hasScrollableContent =
            (miniQuickItemsAdapter?.itemCount ?: 0) > 0 &&
                recycler.computeHorizontalScrollRange() > recycler.width
        if (!hasScrollableContent) {
            applyMiniQuickFadeAlpha(leftFade, 0f, animate)
            applyMiniQuickFadeAlpha(rightFade, 0f, animate)
            return
        }
        applyMiniQuickFadeAlpha(
            leftFade,
            if (recycler.canScrollHorizontally(-1)) 1f else 0f,
            animate
        )
        applyMiniQuickFadeAlpha(
            rightFade,
            if (recycler.canScrollHorizontally(1)) 1f else 0f,
            animate
        )
    }

    private fun applyMiniQuickFadeAlpha(view: View, targetAlpha: Float, animate: Boolean) {
        val target = targetAlpha.coerceIn(0f, 1f)
        if (!animate) {
            view.animate().cancel()
            view.alpha = target
            return
        }
        if (abs(view.alpha - target) < 0.02f) {
            view.alpha = target
            return
        }
        view.animate()
            .alpha(target)
            .setDuration(140L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun refreshMiniQuickCardIndicators() {
        val container = miniQuickCardIndicatorContainer ?: return
        container.removeAllViews()
        val count = max(1, quickCards.size)
        repeat(count) { index ->
            container.addView(
                View(this).apply {
                    background = circleDrawable(
                        if (index == quickCardSelectedIndex.coerceIn(0, count - 1)) {
                            overlayPrimaryColor()
                        } else {
                            overlayIndicatorInactiveColor()
                        }
                    )
                    setOnClickListener {
                        if (quickCards.isNotEmpty()) {
                            miniQuickCardPager?.setCurrentItem(index.coerceIn(0, quickCards.lastIndex), true)
                        }
                    }
                },
                LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                    if (index > 0) leftMargin = dp(8)
                }
            )
        }
    }

    private fun createMiniGroupSwipeTouchListener(): View.OnTouchListener {
        val threshold = max(
            ViewConfiguration.get(this).scaledTouchSlop.toFloat() * 1.25f,
            dp(18).toFloat()
        )
        return object : View.OnTouchListener {
            var lastY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastY = event.rawY
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = event.rawY - lastY
                        if (abs(deltaY) >= threshold) {
                            shiftQuickSubtitleGroup(if (deltaY > 0f) 1 else -1)
                            lastY = event.rawY
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                        v.performClick()
                        return true
                    }
                }
                return false
            }
        }
    }

    private suspend fun loadQuickCardConfig() {
        val raw = UserPrefs.getQuickCardConfig(this)
        if (raw.isNullOrBlank()) {
            quickCards = emptyList()
            quickCardSelectedIndex = 0
            quickCardConfigRawCache = ""
            return
        }
        if (raw == quickCardConfigRawCache) return
        runCatching { parseQuickCardConfig(raw) }
            .onFailure { AppLogger.e("FloatingOverlayService.parseQuickCardConfig failed", it) }
    }

    private fun parseQuickCardConfig(raw: String) {
        val root = JSONObject(raw)
        val cardsArr = root.optJSONArray("cards") ?: JSONArray()
        val parsedCards = mutableListOf<QuickCard>()
        for (i in 0 until cardsArr.length()) {
            val obj = cardsArr.optJSONObject(i) ?: continue
            val id = obj.optLong("id", i.toLong() + 1L).coerceAtLeast(1L)
            parsedCards += QuickCard(
                id = id,
                type = QuickCardType.fromWire(obj.optString("type", QuickCardType.Text.wireValue)),
                title = obj.optString("title", "名片名字"),
                note = obj.optString("note", ""),
                themeColor = normalizeQuickCardColor(obj.optString("themeColor", "#038387")),
                link = obj.optString("link", "").trim(),
                portraitImagePath = obj.optString("portraitImagePath", ""),
                landscapeImagePath = obj.optString("landscapeImagePath", "")
            )
        }
        quickCards = parsedCards
        quickCardSelectedIndex = if (quickCards.isEmpty()) {
            0
        } else {
            root.optInt("selectedIndex", 0).coerceIn(0, quickCards.lastIndex)
        }
        quickCardConfigRawCache = raw
        prefetchQuickCardAssets()
    }

    private fun saveQuickCardSelectedIndex() {
        val root = JSONObject().apply {
            put("selectedIndex", quickCardSelectedIndex)
            put(
                "cards",
                JSONArray().apply {
                    quickCards.forEach { card ->
                        put(
                            JSONObject().apply {
                                put("id", card.id)
                                put("type", card.type.wireValue)
                                put("title", card.title)
                                put("note", card.note)
                                put("themeColor", card.themeColor)
                                put("link", card.link)
                                put("portraitImagePath", card.portraitImagePath)
                                put("landscapeImagePath", card.landscapeImagePath)
                            }
                        )
                    }
                }
            )
        }.toString()
        quickCardConfigRawCache = root
        scope.launch(Dispatchers.IO) {
            UserPrefs.setQuickCardConfig(this@FloatingOverlayService, root)
        }
    }

    private fun normalizeQuickCardColor(raw: String): String {
        val value = raw.trim()
        return if (Regex("^#[0-9a-fA-F]{6}$").matches(value)) value.lowercase(Locale.US) else "#038387"
    }

    private fun prefetchQuickCardAssets() {
        val cardsSnapshot = quickCards
        if (cardsSnapshot.isEmpty()) return
        val selected = quickCardSelectedIndex.coerceIn(0, cardsSnapshot.lastIndex)
        val candidateIndices = listOf(selected, selected - 1, selected + 1).distinct()
        scope.launch(Dispatchers.IO) {
            candidateIndices.forEach { index ->
                val card = cardsSnapshot.getOrNull(index) ?: return@forEach
                card.portraitImagePath.takeIf { it.isNotBlank() }?.let { QuickCardRenderCache.loadImage(it) }
                card.landscapeImagePath.takeIf { it.isNotBlank() }?.let { QuickCardRenderCache.loadImage(it) }
                card.link.takeIf { it.isNotBlank() }?.let { QuickCardRenderCache.loadQr(it) }
            }
        }
    }

    private fun refreshQuickCardUi() {
        val previewHost = miniQuickCardPreviewContainer ?: return
        val pager = miniQuickCardPager ?: return
        val adapter = miniQuickCardPagerAdapter ?: return
        adapter.submitCards(quickCards)
        previewHost.requestLayout()
        val safeIndex =
            if (quickCards.isEmpty()) {
                0
            } else {
                quickCardSelectedIndex.coerceIn(0, quickCards.lastIndex)
            }
        if (safeIndex != quickCardSelectedIndex) {
            quickCardSelectedIndex = safeIndex
            saveQuickCardSelectedIndex()
        }
        if (pager.currentItem != safeIndex) {
            pager.setCurrentItem(safeIndex, false)
        }
        refreshMiniQuickCardIndicators()
        refreshMiniPreviewUi()
    }

    private fun createMiniQuickCardPage(card: QuickCard?): View {
        val contentWidth = min(
            dp(244),
            max(dp(196), overlayContentWidthPx(phoneMaxDp = 360, tabletMaxDp = 400) - dp(76))
        )
        return FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            setPadding(0, dp(10), 0, dp(10))
            addView(
                buildOverlayQuickCardCard(
                    card = card,
                    landscape = false,
                    contentWidthPx = contentWidth,
                    interactive = card != null,
                    onCardClick = { card?.let { openMiniQuickCardPreview(it.id) } },
                    onCardLongClick = {
                        card?.let {
                            val targetIndex = quickCards.indexOfFirst { item -> item.id == it.id }
                            if (targetIndex >= 0) quickCardSelectedIndex = targetIndex
                        }
                        launchQuickCardPage()
                    }
                ),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
    }

    private fun openMiniQuickCardPreview(cardId: Long) {
        miniPreviewMode = MiniPreviewMode.QuickCard
        miniPreviewQuickCardId = cardId
        refreshMiniPreviewUi()
    }

    private fun openMiniSubtitlePreview() {
        miniPreviewMode = MiniPreviewMode.Subtitle
        refreshMiniPreviewUi()
    }

    private fun closeMiniPreview() {
        miniPreviewMode = MiniPreviewMode.None
        miniPreviewQuickCardId = null
        refreshMiniPreviewUi()
    }

    private fun refreshMiniPreviewUi() {
        val overlay = miniPreviewOverlay ?: return
        val host = miniPreviewHost ?: return
        if (!miniVisible || miniPreviewMode == MiniPreviewMode.None) {
            val shouldHideCompletely = !miniVisible || miniPreviewMode == MiniPreviewMode.None
            val activeChild = host.getChildAt(0)
            activeChild?.animate()?.cancel()
            activeChild?.animate()
                ?.alpha(0f)
                ?.translationY(dp(12).toFloat())
                ?.setDuration(150L)
                ?.start()
            overlay.animate().cancel()
            if (overlay.visibility != View.VISIBLE) {
                overlay.alpha = 0f
                overlay.visibility = View.GONE
                host.removeAllViews()
                return
            }
            overlay.animate()
                .alpha(0f)
                .setDuration(180L)
                .withEndAction {
                    if (shouldHideCompletely && miniPreviewMode == MiniPreviewMode.None) {
                        overlay.visibility = View.GONE
                        overlay.alpha = 1f
                        host.removeAllViews()
                    }
                }
                .start()
            return
        }
        val previewView =
            when (miniPreviewMode) {
                MiniPreviewMode.QuickCard -> {
                    val cardId = miniPreviewQuickCardId ?: quickCards.getOrNull(quickCardSelectedIndex)?.id
                    val card = quickCards.firstOrNull { it.id == cardId }
                    if (card == null) {
                        miniPreviewMode = MiniPreviewMode.None
                        miniPreviewQuickCardId = null
                        null
                    } else {
                        val width =
                            if (isLandscapeUi()) {
                                min(dp(420), overlayContentWidthPx(phoneMaxDp = 520, tabletMaxDp = 560) - dp(24))
                            } else {
                                min(dp(300), overlayContentWidthPx(phoneMaxDp = 380, tabletMaxDp = 420) - dp(20))
                            }
                        buildOverlayQuickCardCard(
                            card = card,
                            landscape = isLandscapeUi(),
                            contentWidthPx = width,
                            interactive = true,
                            onCardClick = { closeMiniPreview() },
                            onCardLongClick = {
                                val targetIndex = quickCards.indexOfFirst { it.id == card.id }
                                if (targetIndex >= 0) {
                                    quickCardSelectedIndex = targetIndex
                                    saveQuickCardSelectedIndex()
                                }
                                launchQuickCardPage()
                            }
                        )
                    }
                }

                MiniPreviewMode.Subtitle ->
                    buildMiniSubtitlePreviewCard(
                        onCardClick = { closeMiniPreview() },
                        onCardLongClick = { launchQuickSubtitlePage() }
                    )
                MiniPreviewMode.None -> null
            }
        if (previewView == null) {
            overlay.alpha = 0f
            overlay.visibility = View.GONE
            host.removeAllViews()
            return
        }
        host.removeAllViews()
        previewView.alpha = 0f
        previewView.translationY = dp(12).toFloat()
        if (miniPreviewMode == MiniPreviewMode.Subtitle) {
            host.isClickable = false
            host.setOnClickListener(null)
            host.addView(
                previewView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        } else {
            host.isClickable = true
            host.setOnClickListener { }
            host.addView(
                previewView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        if (overlay.visibility != View.VISIBLE) {
            overlay.setBackgroundColor(overlayPreviewScrimColor())
            overlay.alpha = 0f
            overlay.visibility = View.VISIBLE
            overlay.animate().cancel()
            overlay.animate().alpha(1f).setDuration(180L).start()
        } else {
            overlay.setBackgroundColor(overlayPreviewScrimColor())
            overlay.alpha = 1f
        }
        previewView.animate().cancel()
        previewView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(200L)
            .start()
    }

    private fun buildMiniSubtitlePreviewCard(
        onCardClick: () -> Unit,
        onCardLongClick: () -> Unit
    ): View {
        return FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            addView(
                FrameLayout(this@FloatingOverlayService).apply {
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    addView(
                        FrameLayout(this@FloatingOverlayService).apply {
                            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
                            elevation = dp(10).toFloat()
                            clipChildren = false
                            clipToPadding = false
                            isClickable = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                foreground = selectableDrawable()
                            }
                            setOnClickListener { onCardClick() }
                            setOnLongClickListener {
                                onCardLongClick()
                                true
                            }
                            addView(
                                ScrollView(this@FloatingOverlayService).apply {
                                    isFillViewport = true
                                    clipChildren = false
                                    clipToPadding = false
                                    setPadding(dp(16), dp(16), dp(16), dp(16))
                                    addView(
                                        TextView(this@FloatingOverlayService).apply {
                                            setTextColor(overlayOnSurfaceColor())
                                            setTextSize(
                                                TypedValue.COMPLEX_UNIT_SP,
                                                (quickSubtitleFontSizeSp * 1.25f).coerceIn(36f, 140f)
                                            )
                                            typeface =
                                                if (quickSubtitleBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                                            gravity =
                                                if (quickSubtitleCentered) {
                                                    Gravity.CENTER_HORIZONTAL or Gravity.TOP
                                                } else {
                                                    Gravity.START or Gravity.TOP
                                                }
                                            textAlignment =
                                                if (quickSubtitleCentered) {
                                                    View.TEXT_ALIGNMENT_CENTER
                                                } else {
                                                    View.TEXT_ALIGNMENT_VIEW_START
                                                }
                                            text = quickSubtitleCurrentText.ifBlank { defaultQuickSubtitleText }
                                        },
                                        FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                    )
                                },
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        },
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    private fun createOverlayQuickCardLogoView(): ImageView =
        ImageView(this).apply {
            setImageResource(if (overlayDarkTheme) R.drawable.logo_white else R.drawable.logo_black)
            adjustViewBounds = true
            minimumWidth = dp(72)
            maxWidth = dp(84)
        }

    private fun createOverlayQuickCardActionSymbol(
        name: String,
        tint: Int,
        onClick: () -> Unit
    ): TextView =
        symbolTextView(name, 22f, tint).apply {
            isClickable = true
            isFocusable = true
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnClickListener { onClick() }
        }

    private fun QuickCard.overlayHeroImagePath(landscape: Boolean): String =
        if (landscape) {
            landscapeImagePath.ifBlank { portraitImagePath }
        } else {
            portraitImagePath.ifBlank { landscapeImagePath }
        }

    private fun normalizeOverlayWebUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        return if (!trimmed.contains("://") && trimmed.contains('.') && !trimmed.contains(' ')) {
            "https://$trimmed"
        } else {
            null
        }
    }

    private fun openOverlayQuickCardLink(rawLink: String) {
        val normalized = normalizeOverlayWebUrl(rawLink) ?: return
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            AppLogger.e("FloatingOverlayService.openOverlayQuickCardLink failed", it)
        }
    }

    private fun shareOverlayPlainText(content: String, chooserTitle: String) {
        val text = content.trim()
        if (text.isEmpty()) return
        runCatching {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    },
                    chooserTitle
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            AppLogger.e("FloatingOverlayService.shareOverlayPlainText failed", it)
        }
    }

    private fun buildOverlayQuickCardCard(
        card: QuickCard?,
        landscape: Boolean,
        contentWidthPx: Int,
        interactive: Boolean,
        onCardClick: (() -> Unit)?,
        onCardLongClick: (() -> Unit)?
    ): View {
        val outerPadding = dp(10)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(8).toFloat()
            clipChildren = false
            clipToPadding = false
            setPadding(outerPadding, outerPadding, outerPadding, outerPadding)
            if (interactive && (onCardClick != null || onCardLongClick != null)) {
                isClickable = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    foreground = selectableDrawable()
                }
                setOnClickListener { onCardClick?.invoke() }
                if (onCardLongClick != null) {
                    setOnLongClickListener {
                        onCardLongClick()
                        true
                    }
                }
            }
            if (card == null) {
                addView(
                    FrameLayout(this@FloatingOverlayService).apply {
                        minimumHeight = dp(260)
                        addView(
                            TextView(this@FloatingOverlayService).apply {
                                setTextColor(overlayOnSurfaceVariantColor())
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                                typeface = Typeface.DEFAULT_BOLD
                                gravity = Gravity.CENTER
                                text = "暂无快捷名片"
                            },
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    },
                    LinearLayout.LayoutParams(
                        contentWidthPx,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            } else if (landscape) {
                val heroWidth = min(dp(250), max(dp(176), (contentWidthPx * 0.62f).roundToInt()))
                val heroHeight = (heroWidth / 1.67f).roundToInt()
                addView(
                    LinearLayout(this@FloatingOverlayService).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.TOP
                        val hero = FrameLayout(this@FloatingOverlayService).apply {
                            background = roundedRectDrawable(overlayRadiusDp, parseQuickCardThemeColor(card.themeColor))
                            clipChildren = true
                            clipToPadding = true
                        }
                        populateOverlayQuickCardHero(
                            container = hero,
                            card = card,
                            themeColor = parseQuickCardThemeColor(card.themeColor),
                            onThemeColor = quickCardOnColor(parseQuickCardThemeColor(card.themeColor)),
                            landscape = true
                        )
                        addView(hero, LinearLayout.LayoutParams(heroWidth, heroHeight))
                        addView(spaceView(dp(10), 1))
                        addView(
                            LinearLayout(this@FloatingOverlayService).apply {
                                orientation = LinearLayout.VERTICAL
                                addView(
                                    LinearLayout(this@FloatingOverlayService).apply {
                                        orientation = LinearLayout.HORIZONTAL
                                        gravity = Gravity.TOP
                                        addView(
                                            LinearLayout(this@FloatingOverlayService).apply {
                                                orientation = LinearLayout.VERTICAL
                                                addView(
                                                    TextView(this@FloatingOverlayService).apply {
                                                        setTextColor(overlayOnSurfaceColor())
                                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                                                        typeface = Typeface.DEFAULT_BOLD
                                                        maxLines = 1
                                                        ellipsize = TextUtils.TruncateAt.END
                                                        text = card.title.ifBlank { "名片名字" }
                                                    }
                                                )
                                                addView(
                                                    TextView(this@FloatingOverlayService).apply {
                                                        setTextColor(overlayOnSurfaceVariantColor())
                                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                                                        maxLines = 3
                                                        ellipsize = TextUtils.TruncateAt.END
                                                        text = card.note.ifBlank { "愿你的生活充满诗与远方" }
                                                    }
                                                )
                                            },
                                            LinearLayout.LayoutParams(
                                                0,
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                1f
                                            )
                                        )
                                        addView(spaceView(dp(8), 1))
                                        addView(createOverlayQuickCardLogoView())
                                    }
                                )
                            },
                            LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        )
                    },
                    LinearLayout.LayoutParams(contentWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
                )
            } else {
                val themeColor = parseQuickCardThemeColor(card.themeColor)
                val onThemeColor = quickCardOnColor(themeColor)
                val hero = FrameLayout(this@FloatingOverlayService).apply {
                    background = roundedRectDrawable(overlayRadiusDp, themeColor)
                    clipChildren = true
                    clipToPadding = true
                }
                populateOverlayQuickCardHero(hero, card, themeColor, onThemeColor, false)
                addView(
                    hero,
                    LinearLayout.LayoutParams(
                        contentWidthPx,
                        (contentWidthPx * 1.67f).roundToInt()
                    )
                )
                addView(
                    LinearLayout(this@FloatingOverlayService).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.TOP
                        setPadding(0, dp(8), 0, 0)
                        addView(
                            LinearLayout(this@FloatingOverlayService).apply {
                                orientation = LinearLayout.VERTICAL
                                addView(
                                    TextView(this@FloatingOverlayService).apply {
                                        setTextColor(overlayOnSurfaceColor())
                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                                        typeface = Typeface.DEFAULT_BOLD
                                        maxLines = 1
                                        ellipsize = TextUtils.TruncateAt.END
                                        text = card.title.ifBlank { "名片名字" }
                                    }
                                )
                                addView(
                                    TextView(this@FloatingOverlayService).apply {
                                        setTextColor(overlayOnSurfaceVariantColor())
                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                        maxLines = 2
                                        ellipsize = TextUtils.TruncateAt.END
                                        text = card.note.ifBlank { "愿你的生活充满诗与远方" }
                                    }
                                )
                            },
                            LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        )
                        addView(spaceView(dp(8), 1))
                        addView(createOverlayQuickCardLogoView())
                    },
                    LinearLayout.LayoutParams(contentWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
                )
            }
        }
    }

    private fun populateOverlayQuickCardHero(
        container: FrameLayout,
        card: QuickCard,
        themeColor: Int,
        onThemeColor: Int,
        landscape: Boolean
    ) {
        val linkText = card.link.trim()
        val imagePath = card.overlayHeroImagePath(landscape)
        when (card.type) {
            QuickCardType.Image -> {
                val imageView = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    tag = imagePath
                }
                val placeholder = TextView(this).apply {
                    setTextColor(onThemeColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    gravity = Gravity.CENTER
                    text = if (imagePath.isBlank()) "未设置图片" else "加载图片中..."
                }
                container.addView(
                    imageView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                container.addView(
                    placeholder,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                if (linkText.isNotBlank()) {
                    container.addView(
                        View(this).apply {
                            background = GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(Color.TRANSPARENT, ColorUtils.setAlphaComponent(Color.BLACK, 110))
                            )
                        },
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            if (landscape) dp(80) else dp(96),
                            Gravity.BOTTOM
                        )
                    )
                    container.addView(
                        LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(dp(10), dp(8), dp(10), dp(8))
                            addView(
                                TextView(this@FloatingOverlayService).apply {
                                    setTextColor(Color.WHITE)
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                                    maxLines = 1
                                    ellipsize = TextUtils.TruncateAt.END
                                    text = linkText
                                },
                                LinearLayout.LayoutParams(
                                    0,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    1f
                                )
                            )
                            addView(createOverlayQuickCardActionSymbol("open_in_new", Color.WHITE) {
                                openOverlayQuickCardLink(linkText)
                            })
                            addView(spaceView(dp(2), 1))
                            addView(createOverlayQuickCardActionSymbol("share", Color.WHITE) {
                                shareOverlayPlainText(linkText, "分享链接")
                            })
                        },
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM
                        )
                    )
                }
                if (imagePath.isNotBlank()) {
                    scope.launch {
                        val bitmap = withContext(Dispatchers.IO) { decodeBitmapFromPath(imagePath) }
                        if (imageView.tag == imagePath) {
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap)
                                placeholder.visibility = View.GONE
                                imageView.requestLayout()
                                miniQuickCardPreviewContainer?.requestLayout()
                            } else {
                                placeholder.text = "未设置图片"
                            }
                        }
                    }
                }
            }

            QuickCardType.Qr -> {
                val body = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                }
                val qrFrame = FrameLayout(this).apply {
                    background = roundedRectDrawable(overlayRadiusDp, Color.WHITE)
                }
                val qrImage = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    tag = linkText
                }
                val qrPlaceholder = TextView(this).apply {
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    text = if (linkText.isBlank()) "未设置链接" else "生成二维码中..."
                }
                qrFrame.addView(
                    qrImage,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ).apply {
                        leftMargin = dp(16)
                        topMargin = dp(16)
                        rightMargin = dp(16)
                        bottomMargin = dp(16)
                    }
                )
                qrFrame.addView(
                    qrPlaceholder,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                body.addView(
                    qrFrame,
                    LinearLayout.LayoutParams(
                        if (landscape) dp(116) else dp(146),
                        if (landscape) dp(116) else dp(146)
                    )
                )
                if (linkText.isNotBlank()) {
                    body.addView(spaceView(1, dp(10)))
                    body.addView(
                        TextView(this).apply {
                            setTextColor(onThemeColor)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            text = linkText
                            gravity = Gravity.CENTER
                        }
                    )
                    body.addView(spaceView(1, dp(4)))
                    body.addView(
                        LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER
                            addView(createOverlayQuickCardActionSymbol("open_in_new", onThemeColor) {
                                openOverlayQuickCardLink(linkText)
                            })
                            addView(spaceView(dp(2), 1))
                            addView(createOverlayQuickCardActionSymbol("share", onThemeColor) {
                                shareOverlayPlainText(linkText, "分享链接")
                            })
                        }
                    )
                }
                container.addView(
                    body,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                if (linkText.isNotBlank()) {
                    scope.launch {
                        val qrBitmap = withContext(Dispatchers.Default) {
                            generateQuickCardQrBitmap(linkText)
                        }
                        if (qrImage.tag == linkText) {
                            if (qrBitmap != null) {
                                qrImage.setImageBitmap(qrBitmap)
                                qrPlaceholder.visibility = View.GONE
                                qrImage.requestLayout()
                                miniQuickCardPreviewContainer?.requestLayout()
                            } else {
                                qrPlaceholder.text = "未设置链接"
                            }
                        }
                    }
                }
            }

            QuickCardType.Text -> {
                val watermarkText = card.title.ifBlank { "名片名字" }
                if (landscape) {
                    container.addView(
                        TextView(this).apply {
                            setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 56))
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 62f)
                            typeface = Typeface.DEFAULT_BOLD
                            maxLines = 1
                            ellipsize = null
                            isSingleLine = true
                            setHorizontallyScrolling(true)
                            includeFontPadding = false
                            text = watermarkText
                            gravity = Gravity.START or Gravity.BOTTOM
                        },
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.BOTTOM or Gravity.START
                        ).apply {
                            leftMargin = dp(8)
                            bottomMargin = dp(4)
                            rightMargin = dp(48)
                        }
                    )
                    container.addView(
                        View(this).apply {
                            background = GradientDrawable(
                                GradientDrawable.Orientation.LEFT_RIGHT,
                                intArrayOf(Color.TRANSPARENT, themeColor)
                            )
                        },
                        FrameLayout.LayoutParams(dp(68), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END)
                    )
                } else {
                    val watermark = TextView(this).apply {
                        setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 56))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 96f)
                        typeface = Typeface.DEFAULT_BOLD
                        maxLines = 1
                        ellipsize = null
                        isSingleLine = true
                        setHorizontallyScrolling(true)
                        includeFontPadding = false
                        text = watermarkText
                    }
                    container.addView(
                        watermark,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP or Gravity.END
                        )
                    )
                    watermark.post {
                        watermark.pivotX = watermark.measuredWidth.toFloat()
                        watermark.pivotY = 0f
                        watermark.rotation = 90f
                        watermark.translationY = watermark.measuredWidth.toFloat() + dp(10).toFloat()
                    }
                    container.addView(
                        View(this).apply {
                            background = GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(Color.TRANSPARENT, themeColor)
                            )
                        },
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(72),
                            Gravity.BOTTOM
                        )
                    )
                }
                container.addView(
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(12), dp(16), dp(12), dp(12))
                        addView(
                            TextView(this@FloatingOverlayService).apply {
                                setTextColor(onThemeColor)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
                                typeface = Typeface.DEFAULT_BOLD
                                maxLines = 1
                                ellipsize = TextUtils.TruncateAt.END
                                text = watermarkText
                            }
                        )
                        if (card.note.isNotBlank()) {
                            addView(
                                TextView(this@FloatingOverlayService).apply {
                                    setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 230))
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                    maxLines = 2
                                    ellipsize = TextUtils.TruncateAt.END
                                    text = card.note
                                }
                            )
                        }
                    },
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP or Gravity.START
                    )
                )
                if (linkText.isNotBlank()) {
                    container.addView(
                        LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(dp(12), dp(8), dp(12), dp(8))
                            addView(
                                TextView(this@FloatingOverlayService).apply {
                                    setTextColor(onThemeColor)
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                                    maxLines = 1
                                    ellipsize = TextUtils.TruncateAt.END
                                    text = linkText
                                },
                                LinearLayout.LayoutParams(
                                    0,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    1f
                                )
                            )
                            addView(createOverlayQuickCardActionSymbol("open_in_new", onThemeColor) {
                                openOverlayQuickCardLink(linkText)
                            })
                            addView(spaceView(dp(2), 1))
                            addView(createOverlayQuickCardActionSymbol("share", onThemeColor) {
                                shareOverlayPlainText(linkText, "分享链接")
                            })
                        },
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM
                        )
                    )
                }
            }
        }
    }

    private fun populateMiniQuickCardHero(
        container: FrameLayout,
        card: QuickCard,
        themeColor: Int,
        onThemeColor: Int
    ) {
        when (card.type) {
            QuickCardType.Image -> {
                val imagePath = if (card.portraitImagePath.isNotBlank()) card.portraitImagePath else card.landscapeImagePath
                val imageView = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    tag = imagePath
                }
                val placeholder = TextView(this).apply {
                    setTextColor(onThemeColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    gravity = Gravity.CENTER
                    text = if (imagePath.isBlank()) "未设置图片" else "加载图片中..."
                }
                container.addView(
                    imageView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                container.addView(
                    placeholder,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                if (imagePath.isNotBlank()) {
                    scope.launch {
                        val bitmap = withContext(Dispatchers.IO) {
                            QuickCardRenderCache.loadImage(imagePath)
                        }
                        if (imageView.tag == imagePath && imageView.isAttachedToWindow) {
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap)
                                placeholder.visibility = View.GONE
                                imageView.requestLayout()
                                miniQuickCardPreviewContainer?.requestLayout()
                            } else {
                                placeholder.text = "未设置图片"
                            }
                        }
                    }
                }
            }
            QuickCardType.Qr -> {
                container.addView(
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(dp(16), dp(16), dp(16), dp(16))
                        addView(
                            FrameLayout(this@FloatingOverlayService).apply {
                                background = roundedRectDrawable(overlayRadiusDp, Color.WHITE)
                                val qrHolder = FrameLayout(this@FloatingOverlayService)
                                val qrImageView = ImageView(this@FloatingOverlayService).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    tag = card.link
                                }
                                val qrPlaceholder = TextView(this@FloatingOverlayService).apply {
                                    setTextColor(Color.BLACK)
                                    gravity = Gravity.CENTER
                                    text = if (card.link.isBlank()) "无链接" else "生成二维码中..."
                                }
                                qrHolder.addView(
                                    qrImageView,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                                qrHolder.addView(
                                    qrPlaceholder,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                                addView(
                                    qrHolder,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    ).apply {
                                        marginStart = dp(16)
                                        topMargin = dp(16)
                                        marginEnd = dp(16)
                                        bottomMargin = dp(16)
                                    }
                                )
                                if (card.link.isNotBlank()) {
                                    scope.launch {
                                        val qrBitmap = withContext(Dispatchers.Default) {
                                            QuickCardRenderCache.loadQr(card.link)
                                        }
                                        if (qrImageView.tag == card.link && qrImageView.isAttachedToWindow) {
                                            if (qrBitmap != null) {
                                                qrImageView.setImageBitmap(qrBitmap)
                                                qrPlaceholder.visibility = View.GONE
                                            } else {
                                                qrPlaceholder.text = "无链接"
                                            }
                                        }
                                    }
                                }
                            },
                            LinearLayout.LayoutParams(dp(128), dp(128))
                        )
                        if (card.link.isNotBlank()) {
                            addView(spaceView(1, dp(10)))
                            addView(
                                TextView(this@FloatingOverlayService).apply {
                                    setTextColor(onThemeColor)
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                    maxLines = 1
                                    ellipsize = TextUtils.TruncateAt.END
                                    text = card.link
                                }
                            )
                        }
                    },
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
            QuickCardType.Text -> {
                container.addView(
                    TextView(this).apply {
                        setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 56))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 54f)
                        typeface = Typeface.DEFAULT_BOLD
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        text = card.title.ifBlank { "名片名字" }
                    },
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ).apply {
                        marginStart = dp(8)
                        topMargin = dp(8)
                        marginEnd = dp(8)
                        bottomMargin = dp(8)
                    }
                )
                container.addView(
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(14), dp(14), dp(14), dp(14))
                        addView(
                            TextView(this@FloatingOverlayService).apply {
                                setTextColor(onThemeColor)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                                typeface = Typeface.DEFAULT_BOLD
                                maxLines = 1
                                ellipsize = TextUtils.TruncateAt.END
                                text = card.title.ifBlank { "名片名字" }
                            }
                        )
                        if (card.note.isNotBlank()) {
                            addView(
                                TextView(this@FloatingOverlayService).apply {
                                    setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 230))
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                    maxLines = 2
                                    ellipsize = TextUtils.TruncateAt.END
                                    text = card.note
                                }
                            )
                        }
                    },
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP or Gravity.START
                    )
                )
            }
        }
    }

    private fun parseQuickCardThemeColor(raw: String): Int {
        return runCatching { Color.parseColor(normalizeQuickCardColor(raw)) }.getOrDefault(overlayPrimaryColor())
    }

    private fun quickCardOnColor(themeColor: Int): Int {
        return if (ColorUtils.calculateLuminance(themeColor) > 0.35) Color.BLACK else Color.WHITE
    }

    private fun overlayPreviewScrimColor(): Int {
        return if (overlayDarkTheme) {
            ColorUtils.setAlphaComponent(Color.BLACK, 168)
        } else {
            ColorUtils.setAlphaComponent(Color.BLACK, 132)
        }
    }

    private fun decodeBitmapFromPath(path: String): Bitmap? {
        return QuickCardRenderCache.loadImage(path)
    }

    private fun generateQuickCardQrBitmap(content: String): Bitmap? {
        return QuickCardRenderCache.loadQr(content, dp(160))
    }

    private suspend fun loadOverlayShortcuts() {
        val raw = UserPrefs.getFloatingOverlayShortcuts(this)
        if (raw.isNullOrBlank()) {
            overlayShortcuts = mutableListOf()
            return
        }
        runCatching {
            val arr = JSONArray(raw)
            val parsed = mutableListOf<OverlayAppShortcut>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val packageName = obj.optString("packageName", "").trim()
                val className = obj.optString("className", "").trim()
                val label = obj.optString("label", "").trim()
                if (packageName.isEmpty() || className.isEmpty()) continue
                parsed += OverlayAppShortcut(
                    packageName = packageName,
                    className = className,
                    label = label.ifEmpty { className.substringAfterLast('.') }
                )
            }
            overlayShortcuts = parsed
        }.onFailure {
            overlayShortcuts = mutableListOf()
            AppLogger.e("FloatingOverlayService.loadOverlayShortcuts failed", it)
        }
    }

    private suspend fun loadOverlayLauncherLayout() {
        overlayLauncherLayoutLoaded = false
        val raw = UserPrefs.getFloatingOverlayLayout(this)
        portraitFabAnchor = null
        landscapeFabAnchor = null
        if (raw.isNullOrBlank()) {
            overlayLauncherOrder = mutableListOf()
            overlayLauncherLayoutLoaded = true
            return
        }
        runCatching {
            val trimmed = raw.trim()
            if (trimmed.startsWith("[")) {
                overlayLauncherOrder = parseOverlayLauncherOrder(JSONArray(trimmed))
            } else {
                val payload = JSONObject(trimmed)
                overlayLauncherOrder = parseOverlayLauncherOrder(payload.optJSONArray("order"))
                val fabAnchors = payload.optJSONObject("fabAnchors")
                portraitFabAnchor = parseOverlayFabAnchor(fabAnchors?.optJSONObject("portrait"))
                landscapeFabAnchor = parseOverlayFabAnchor(fabAnchors?.optJSONObject("landscape"))
            }
        }.onFailure {
            overlayLauncherOrder = mutableListOf()
            portraitFabAnchor = null
            landscapeFabAnchor = null
            AppLogger.e("FloatingOverlayService.loadOverlayLauncherLayout failed", it)
        }
        overlayLauncherLayoutLoaded = true
    }

    private fun saveOverlayShortcuts() {
        if (overlayShortcutSaving) return
        overlayShortcutSaving = true
        val payload = JSONArray().apply {
            overlayShortcuts.forEach { item ->
                put(
                    JSONObject().apply {
                        put("packageName", item.packageName)
                        put("className", item.className)
                        put("label", item.label)
                    }
                )
            }
        }.toString()
        scope.launch(Dispatchers.IO) {
            try {
                UserPrefs.setFloatingOverlayShortcuts(this@FloatingOverlayService, payload)
            } finally {
                overlayShortcutSaving = false
            }
        }
    }

    private fun saveOverlayLauncherLayout() {
        if (!overlayLauncherLayoutLoaded) return
        val payload = JSONObject().apply {
            put(
                "order",
                JSONArray().apply {
                    overlayLauncherOrder.forEach { put(it) }
                }
            )
            put(
                "fabAnchors",
                JSONObject().apply {
                    buildOverlayFabAnchorPayload(portraitFabAnchor)?.let { put("portrait", it) }
                    buildOverlayFabAnchorPayload(landscapeFabAnchor)?.let { put("landscape", it) }
                }
            )
        }.toString()
        scope.launch(Dispatchers.IO) {
            UserPrefs.setFloatingOverlayLayout(this@FloatingOverlayService, payload)
        }
    }

    private fun parseOverlayLauncherOrder(arr: JSONArray?): MutableList<String> {
        val parsed = mutableListOf<String>()
        if (arr == null) return parsed
        for (i in 0 until arr.length()) {
            val key = arr.optString(i).trim()
            if (key.isNotEmpty() && key !in parsed) parsed += key
        }
        return parsed
    }

    private fun parseOverlayFabAnchor(obj: JSONObject?): OverlayFabAnchor? {
        if (obj == null) return null
        val edge = obj.optString("edge").trim()
        if (edge != FAB_EDGE_LEFT && edge != FAB_EDGE_RIGHT) return null
        return OverlayFabAnchor(
            edge = edge,
            verticalRatio = obj.optDouble("verticalRatio", 1.0).toFloat().coerceIn(0f, 1f)
        )
    }

    private fun buildOverlayFabAnchorPayload(anchor: OverlayFabAnchor?): JSONObject? {
        if (anchor == null) return null
        return JSONObject().apply {
            put("edge", anchor.edge)
            put("verticalRatio", anchor.verticalRatio.toDouble())
        }
    }

    private suspend fun loadLaunchableApps() {
        if (launchableAppsLoading) return
        if (launchableAppsLoaded && launchableAppsCache.isNotEmpty()) {
            if (panelPickerOverlay?.visibility == View.VISIBLE) refreshShortcutPickerUi()
            return
        }
        launchableAppsLoading = true
        try {
            launchableAppsCache = withContext(Dispatchers.IO) {
                val intents = listOf(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                )
                val infos = buildList {
                    intents.forEach { launcherIntent ->
                        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            packageManager.queryIntentActivities(
                                launcherIntent,
                                PackageManager.ResolveInfoFlags.of(0)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.queryIntentActivities(launcherIntent, 0)
                        }
                        addAll(resolved)
                    }
                }
                infos.mapNotNull(::resolveOverlayShortcut)
                    .filterNot { it.packageName == packageName }
                    .distinctBy { "${it.packageName}/${it.className}" }
                    .sortedBy { it.label.lowercase() }
            }
            launchableAppsLoaded = true
        } finally {
            launchableAppsLoading = false
        }
        if (panelPickerOverlay?.visibility == View.VISIBLE) {
            refreshShortcutPickerUi()
        }
    }

    private fun resolveOverlayShortcut(info: ResolveInfo): OverlayAppShortcut? {
        val activityInfo = info.activityInfo ?: return null
        val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
        return OverlayAppShortcut(
            packageName = activityInfo.packageName.orEmpty(),
            className = activityInfo.name.orEmpty(),
            label = label.ifEmpty { activityInfo.name.substringAfterLast('.') }
        )
    }

    private fun builtInLauncherTiles(): List<OverlayLauncherTile> = listOf(
        OverlayLauncherTile("builtin_subtitles", "快捷字幕", "subtitles"),
        OverlayLauncherTile("builtin_quick_card", "快捷名片", "id_card"),
        OverlayLauncherTile("builtin_drawing", "画板", "draw"),
        OverlayLauncherTile("builtin_scanner", "二维码扫描", "qr_code_2"),
        OverlayLauncherTile("builtin_settings", "设置", "tune")
    )

    private fun shortcutKey(shortcut: OverlayAppShortcut): String =
        "app:${shortcut.packageName}/${shortcut.className}"

    private fun buildOverlayLauncherTileMap(): LinkedHashMap<String, OverlayLauncherTile> {
        val tileMap = linkedMapOf<String, OverlayLauncherTile>()
        builtInLauncherTiles().forEach { tileMap[it.key] = it }
        overlayShortcuts.forEach { shortcut ->
            val key = shortcutKey(shortcut)
            tileMap[key] = OverlayLauncherTile(
                key = key,
                label = shortcut.label,
                icon = "android",
                shortcut = shortcut
            )
        }
        return tileMap
    }

    private fun normalizeOverlayLauncherOrder(
        sourceOrder: List<String>,
        tileMap: Map<String, OverlayLauncherTile>,
        persist: Boolean
    ): MutableList<String> {
        val normalizedOrder = mutableListOf<String>()
        sourceOrder.forEach { key ->
            if (tileMap.containsKey(key) && key !in normalizedOrder) {
                normalizedOrder += key
            }
        }
        tileMap.keys.forEach { key ->
            if (key !in normalizedOrder) {
                normalizedOrder += key
            }
        }
        if (persist && normalizedOrder != overlayLauncherOrder) {
            overlayLauncherOrder = normalizedOrder.toMutableList()
            saveOverlayLauncherLayout()
        }
        return normalizedOrder
    }

    private fun buildOverlayLauncherTiles(): List<OverlayLauncherTile> {
        val tileMap = buildOverlayLauncherTileMap()
        val previewOrder = normalizeOverlayLauncherOrder(overlayLauncherOrder, tileMap, persist = true)
        val displayTiles = mutableListOf<OverlayLauncherTile>()
        previewOrder.mapNotNullTo(displayTiles) { tileMap[it] }
        return displayTiles + OverlayLauncherTile(
            key = "builtin_add_app",
            label = "添加应用",
            icon = "add",
            isAddButton = true
        )
    }

    private fun refreshPanelUi(syncPagerPosition: Boolean = true) {
        val tiles = buildOverlayLauncherTiles()
        val pageSize = 6
        val pages = tiles.chunked(pageSize).ifEmpty { listOf(emptyList()) }
        panelPageCount = pages.size
        panelPageIndex = panelPageIndex.coerceIn(0, panelPageCount - 1)
        panelEditButtonView?.text = if (panelEditMode) "check" else "edit"
        panelPagerAdapter?.submitPages(pages)
        panelPager?.isUserInputEnabled = !panelEditMode
        refreshPanelIndicators()
        refreshPanelEditPageButtons()
        if (panelPickerOverlay?.visibility == View.VISIBLE) {
            refreshShortcutPickerUi()
        }
        if (syncPagerPosition) {
            panelPager?.post { scrollPanelToPage(panelPageIndex, animate = false) }
        }
    }

    private fun requestPanelUiRefresh(syncPagerPosition: Boolean = true) {
        panelUiRefreshSyncPager = panelUiRefreshSyncPager || syncPagerPosition
        if (panelUiRefreshPosted) return
        panelUiRefreshPosted = true
        val host = panelPager ?: panelRoot ?: fabRoot
        host?.post {
            panelUiRefreshPosted = false
            val syncPager = panelUiRefreshSyncPager
            panelUiRefreshSyncPager = false
            refreshPanelUi(syncPagerPosition = syncPager)
        } ?: run {
            panelUiRefreshPosted = false
            val syncPager = panelUiRefreshSyncPager
            panelUiRefreshSyncPager = false
            refreshPanelUi(syncPagerPosition = syncPager)
        }
    }

    private fun refreshPanelEditPageButtons() {
        val prev = panelPrevPageButtonView ?: return
        val next = panelNextPageButtonView ?: return
        val shouldShow = panelEditMode && panelPageCount > 1
        val updateVisibility: (View, Boolean) -> Unit = { view, visible ->
            if (!shouldShow) {
                view.animate().cancel()
                view.visibility = View.GONE
                view.alpha = 0f
                view.isEnabled = false
            } else {
                val targetAlpha = if (visible) 1f else 0.38f
                if (view.visibility != View.VISIBLE) {
                    view.alpha = 0f
                    view.visibility = View.VISIBLE
                }
                view.isEnabled = visible
                view.animate()
                    .alpha(targetAlpha)
                    .setDuration(140L)
                    .start()
            }
        }
        updateVisibility(prev, panelPageIndex > 0)
        updateVisibility(next, panelPageIndex < panelPageCount - 1)
    }

    private fun buildPanelPageView(pageIndex: Int, pageTiles: List<OverlayLauncherTile>): View {
        val pageSize = 6
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            layoutTransition = overlayLayoutTransition()
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnDragListener { _, event ->
                when (event.action) {
                    android.view.DragEvent.ACTION_DRAG_STARTED -> panelEditMode
                    android.view.DragEvent.ACTION_DRAG_LOCATION -> {
                        handlePanelDragLocation(width, event.x)
                        true
                    }
                    android.view.DragEvent.ACTION_DROP -> {
                        if (panelEditMode) {
                            val draggedKey = extractDraggedTileKey(event)
                            if (draggedKey != null && panelDragPreviewOrder != null && panelDraggedKey == draggedKey) {
                                val targetIndex = min((pageIndex + 1) * pageSize, panelDragPreviewOrder?.size ?: overlayLauncherOrder.size)
                                updatePanelDragPreview(draggedKey, targetIndex)
                                commitPanelDragPreview(draggedKey)
                            }
                        }
                        true
                    }
                    android.view.DragEvent.ACTION_DRAG_ENDED -> {
                        cancelPendingPanelPageSwitch()
                        (event.localState as? View)?.alpha = 1f
                        if (panelDragPreviewOrder != null) {
                            clearPanelDragPreview(refresh = true)
                        }
                        true
                    }
                    else -> panelEditMode
                }
            }
            pageTiles.chunked(3).forEachIndexed { rowIndex, rowTiles ->
                addView(
                    LinearLayout(this@FloatingOverlayService).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        clipChildren = false
                        clipToPadding = false
                        layoutTransition = overlayLayoutTransition()
                        rowTiles.forEachIndexed { tileIndex, tile ->
                            val globalIndex = pageIndex * pageSize + tileIndex + rowIndex * 3
                            addView(
                                createPanelLauncherTileView(
                                    tile = tile,
                                    targetIndex = if (tile.isAddButton) overlayLauncherOrder.size else globalIndex
                                ),
                                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                                    if (tileIndex > 0) leftMargin = dp(12)
                                }
                            )
                        }
                        if (rowTiles.size < 3) {
                            repeat(3 - rowTiles.size) { fillerIndex ->
                                addView(
                                    spaceView(0, 0),
                                    LinearLayout.LayoutParams(0, 1, 1f).apply {
                                        if (rowTiles.isNotEmpty() || fillerIndex > 0) leftMargin = dp(12)
                                    }
                                )
                            }
                        }
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        if (rowIndex > 0) topMargin = dp(12)
                    }
                )
            }
        }
    }

    private fun createPanelLauncherTileView(tile: OverlayLauncherTile, targetIndex: Int): View {
        if (tile.isPlaceholder) {
            return FrameLayout(this).apply {
                tag = tile.key
                minimumHeight = dp(88)
                clipChildren = false
                clipToPadding = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        }
        val tileContent = if (tile.shortcut != null) {
            createAppEntryTile(tile.shortcut)
        } else {
            createSymbolEntryTile(
                label = tile.label,
                icon = tile.icon,
                onClick = { handlePanelLauncherTileClick(tile) },
                plain = true
            )
        }
        tileContent.tag = tile.key
        tileContent.setOnClickListener { handlePanelLauncherTileClick(tile) }
        tileContent.setOnLongClickListener { handlePanelLauncherTileLongClick(tile, tileContent) }
        val tileView = FrameLayout(this).apply {
            tag = tile.key
            clipChildren = false
            clipToPadding = false
            addView(
                tileContent,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val showInsertMarker =
            panelEditMode && panelDraggedKey != null && panelDragPreviewOrder != null && panelDragHoverIndex == targetIndex
        if (showInsertMarker) {
            tileView.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    clipChildren = false
                    clipToPadding = false
                    addView(
                        View(this@FloatingOverlayService).apply {
                            background = roundedRectDrawable(1f, overlayPrimaryColor())
                        },
                        LinearLayout.LayoutParams(dp(14), dp(3))
                    )
                    addView(
                        View(this@FloatingOverlayService).apply {
                            background = roundedRectDrawable(1f, overlayPrimaryColor())
                        },
                        LinearLayout.LayoutParams(dp(3), dp(34)).apply {
                            topMargin = dp(2)
                            bottomMargin = dp(2)
                        }
                    )
                    addView(
                        View(this@FloatingOverlayService).apply {
                            background = roundedRectDrawable(1f, overlayPrimaryColor())
                        },
                        LinearLayout.LayoutParams(dp(14), dp(3))
                    )
                },
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
                    leftMargin = -dp(7)
                }
            )
        }
        if (panelEditMode && tile.shortcut != null) {
            tileView.addView(
                circleActionButton(symbolTextView("close", 16f, overlayOnSurfaceColor())).apply {
                    alpha = 0.92f
                    setOnClickListener {
                        overlayShortcuts = overlayShortcuts.filterNot { shortcutKey(it) == tile.key }.toMutableList()
                        overlayLauncherOrder.remove(tile.key)
                        saveOverlayShortcuts()
                        saveOverlayLauncherLayout()
                        beginLauncherReorderTransition()
                        refreshPanelUi()
                    }
                },
                FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP or Gravity.END).apply {
                    topMargin = dp(2)
                    rightMargin = dp(2)
                }
            )
        }
        if (tile.shortcut != null) {
            tileView.setOnClickListener { handlePanelLauncherTileClick(tile) }
        }
        tileView.setOnLongClickListener { handlePanelLauncherTileLongClick(tile, tileView) }
        if (panelEditMode && !tile.isAddButton) {
            tileContent.setOnTouchListener { view, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    tileView.alpha = 0.35f
                    val dragData = android.content.ClipData.newPlainText(tile.key, tile.key)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        view.startDragAndDrop(dragData, View.DragShadowBuilder(tileView), tileView, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        view.startDrag(dragData, View.DragShadowBuilder(tileView), tileView, 0)
                    }
                    true
                } else {
                    false
                }
            }
        } else {
            installPanelTileGestureDelegate(tileView)
            installPanelTileGestureDelegate(tileContent)
        }
        tileView.setOnDragListener { _, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> {
                    if (!panelEditMode) return@setOnDragListener false
                    val draggedKey = extractDraggedTileKey(event)
                    if (draggedKey != null && panelDragPreviewOrder == null) {
                        beginPanelDragPreview(draggedKey)
                    }
                    true
                }
                android.view.DragEvent.ACTION_DRAG_LOCATION -> {
                    handlePanelDragWithinPager(tileView, event.x)
                    if (panelEditMode) {
                        val draggedKey = extractDraggedTileKey(event)
                        if (draggedKey != null) {
                            if (tile.isAddButton) {
                                updatePanelDragPreview(draggedKey, panelDragPreviewOrder?.size ?: overlayLauncherOrder.size)
                            } else {
                                updatePanelDragPreviewForTile(draggedKey, tile.key)
                            }
                        }
                    }
                    true
                }
                android.view.DragEvent.ACTION_DROP -> {
                    if (panelEditMode) {
                        val draggedKey = extractDraggedTileKey(event)
                        if (draggedKey != null && panelDragPreviewOrder != null && panelDraggedKey == draggedKey) {
                            if (tile.isAddButton) {
                                updatePanelDragPreview(draggedKey, panelDragPreviewOrder?.size ?: overlayLauncherOrder.size)
                            } else {
                                updatePanelDragPreviewForTile(draggedKey, tile.key)
                            }
                            commitPanelDragPreview(draggedKey)
                        }
                    }
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    (event.localState as? View)?.alpha = 1f
                    if (panelDragPreviewOrder != null) {
                        clearPanelDragPreview(refresh = true)
                    } else if (panelEditMode) {
                        refreshPanelUi()
                    }
                    true
                }
                else -> panelEditMode
            }
        }
        return tileView
    }

    private fun installPanelTileGestureDelegate(view: View) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()
        val pageSwitchThreshold = max(dp(36), (touchSlop * 3f).roundToInt()).toFloat()
        val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
        var downX = 0f
        var downY = 0f
        var swiping = false
        var longPressed = false
        val longPressRunnable = Runnable {
            if (!swiping && view.isPressed && view.isLongClickable) {
                longPressed = view.performLongClick()
                if (longPressed) {
                    view.isPressed = false
                }
            }
        }
        view.setOnTouchListener { touchedView, event ->
            if (panelEditMode) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    swiping = false
                    longPressed = false
                    touchedView.isPressed = true
                    if (touchedView.isLongClickable) {
                        touchedView.removeCallbacks(longPressRunnable)
                        touchedView.postDelayed(longPressRunnable, longPressTimeout)
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!swiping && abs(dx) > touchSlop * 1.5f && abs(dx) > abs(dy)) {
                        swiping = true
                        touchedView.isPressed = false
                        touchedView.removeCallbacks(longPressRunnable)
                    } else if (!swiping && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        touchedView.removeCallbacks(longPressRunnable)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - downX
                    touchedView.removeCallbacks(longPressRunnable)
                    touchedView.isPressed = false
                    when {
                        longPressed -> {
                            longPressed = false
                            true
                        }
                        swiping -> {
                            when {
                                dx <= -pageSwitchThreshold -> switchPanelPageBy(1)
                                dx >= pageSwitchThreshold -> switchPanelPageBy(-1)
                                else -> snapPanelToNearestPage()
                            }
                            true
                        }
                        else -> {
                            touchedView.performClick()
                            true
                        }
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    touchedView.removeCallbacks(longPressRunnable)
                    touchedView.isPressed = false
                    swiping = false
                    longPressed = false
                    true
                }

                else -> false
            }
        }
    }

    private fun extractDraggedTileKey(event: android.view.DragEvent): String? {
        val localView = event.localState as? View ?: return null
        return localView.tag as? String
    }

    private fun beginPanelDragPreview(draggedKey: String) {
        val tileMap = buildOverlayLauncherTileMap()
        val normalized = normalizeOverlayLauncherOrder(overlayLauncherOrder, tileMap, persist = true)
        panelDraggedKey = draggedKey
        panelDragPreviewOrder = normalized.toMutableList()
        panelDragHoverIndex = normalized.indexOf(draggedKey).coerceIn(0, normalized.size)
        requestPanelUiRefresh(syncPagerPosition = false)
    }

    private fun updatePanelDragPreview(draggedKey: String, requestedIndex: Int) {
        if (panelDraggedKey != draggedKey || panelDragPreviewOrder == null) {
            beginPanelDragPreview(draggedKey)
        }
        val previewOrder = panelDragPreviewOrder ?: return
        val insertIndex = requestedIndex.coerceIn(0, previewOrder.size)
        if (panelDragHoverIndex == insertIndex) return
        panelDragHoverIndex = insertIndex
        requestPanelUiRefresh(syncPagerPosition = false)
    }

    private fun updatePanelDragPreviewForTile(draggedKey: String, targetKey: String) {
        if (targetKey == draggedKey) return
        if (panelDraggedKey != draggedKey || panelDragPreviewOrder == null) {
            beginPanelDragPreview(draggedKey)
        }
        val previewOrder = panelDragPreviewOrder ?: return
        val targetIndex = previewOrder.indexOf(targetKey).let { index ->
            if (index >= 0) index else previewOrder.size
        }
        updatePanelDragPreview(draggedKey, targetIndex)
    }

    private fun commitPanelDragPreview(draggedKey: String, requestedIndex: Int? = null) {
        val previewOrder = panelDragPreviewOrder?.toMutableList() ?: return
        val insertIndex = (requestedIndex ?: panelDragHoverIndex).coerceIn(0, previewOrder.size)
        val currentOrder = overlayLauncherOrder.toList()
        panelDraggedKey = null
        panelDragPreviewOrder = null
        panelDragHoverIndex = -1
        val targetOrder = currentOrder.toMutableList().apply {
            remove(draggedKey)
            val safeInsertIndex = when {
                insertIndex <= 0 -> 0
                insertIndex >= size -> size
                currentOrder.indexOf(draggedKey) < insertIndex -> insertIndex - 1
                else -> insertIndex
            }
            add(safeInsertIndex, draggedKey)
        }
        if (targetOrder == currentOrder) {
            requestPanelUiRefresh(syncPagerPosition = false)
            return
        }
        reorderOverlayLauncherTile(draggedKey, insertIndex)
    }

    private fun clearPanelDragPreview(refresh: Boolean) {
        if (panelDraggedKey == null && panelDragPreviewOrder == null && panelDragHoverIndex < 0) return
        panelDraggedKey = null
        panelDragPreviewOrder = null
        panelDragHoverIndex = -1
        if (refresh) {
            requestPanelUiRefresh(syncPagerPosition = false)
        }
    }

    private fun reorderOverlayLauncherTile(draggedKey: String, requestedIndex: Int) {
        val fromIndex = overlayLauncherOrder.indexOf(draggedKey)
        if (fromIndex < 0) return
        val mutable = overlayLauncherOrder.toMutableList()
        mutable.removeAt(fromIndex)
        val insertIndex = when {
            requestedIndex <= 0 -> 0
            requestedIndex >= mutable.size -> mutable.size
            fromIndex < requestedIndex -> requestedIndex - 1
            else -> requestedIndex
        }
        mutable.add(insertIndex, draggedKey)
        if (mutable == overlayLauncherOrder) return
        overlayLauncherOrder = mutable
        saveOverlayLauncherLayout()
        beginLauncherReorderTransition()
        refreshPanelUi()
    }

    private fun handlePanelDragLocation(hostWidth: Int, x: Float) {
        if (!panelEditMode || panelPageCount <= 1) return
        val threshold = dp(40).toFloat()
        val direction = when {
            x <= threshold -> -1
            x >= hostWidth - threshold -> 1
            else -> 0
        }
        if (direction == panelDragPendingDirection) return
        cancelPendingPanelPageSwitch()
        if (direction == 0) return
        panelDragPendingDirection = direction
        schedulePendingPanelPageSwitch(direction)
    }

    private fun schedulePendingPanelPageSwitch(direction: Int) {
        if (direction == 0) return
        val pager = panelPager ?: return
        val runnable = Runnable {
            if (panelDragPendingDirection != direction) return@Runnable
            val nextPage = (panelPageIndex + direction).coerceIn(0, panelPageCount - 1)
            if (nextPage != panelPageIndex) {
                panelPageIndex = nextPage
                val draggedKey = panelDraggedKey
                if (draggedKey != null && panelDragPreviewOrder != null) {
                    val previewSize = panelDragPreviewOrder?.size ?: 0
                    val edgeIndex = if (direction > 0) {
                        min((nextPage + 1) * 6, previewSize)
                    } else {
                        (nextPage * 6).coerceIn(0, previewSize)
                    }
                    panelDragHoverIndex = edgeIndex
                }
                refreshPanelIndicators()
                beginLauncherReorderTransition()
                requestPanelUiRefresh(syncPagerPosition = false)
                scrollPanelToPage(nextPage, animate = true)
            }
            if (panelDragPendingDirection == direction) {
                schedulePendingPanelPageSwitch(direction)
            }
        }
        panelDragSwitchRunnable = runnable
        pager.postDelayed(runnable, 220L)
    }

    private fun cancelPendingPanelPageSwitch() {
        panelDragPendingDirection = 0
        val pager = panelPager ?: return
        panelDragSwitchRunnable?.let { pager.removeCallbacks(it) }
        panelDragSwitchRunnable = null
    }

    private fun switchPanelPageBy(delta: Int) {
        collapseOverlayStatusExpanded()
        if (panelPageCount <= 1) {
            snapPanelToNearestPage()
            return
        }
        val targetIndex = (panelPageIndex + delta).coerceIn(0, panelPageCount - 1)
        if (targetIndex == panelPageIndex) {
            snapPanelToNearestPage()
            return
        }
        panelPageIndex = targetIndex
        refreshPanelIndicators()
        scrollPanelToPage(targetIndex, animate = true)
    }

    private fun handlePanelLauncherTileClick(tile: OverlayLauncherTile) {
        if (panelEditMode && !tile.isAddButton) return
        when {
            tile.isAddButton -> showShortcutPicker()
            tile.shortcut != null -> {
                hidePanel()
                launchExternalShortcut(tile.shortcut)
            }
            tile.key == "builtin_subtitles" -> {
                showMiniPanel(MiniOverlayMode.Subtitle)
            }
            tile.key == "builtin_quick_card" -> {
                showMiniPanel(MiniOverlayMode.QuickCard)
            }
            tile.key == "builtin_drawing" -> {
                hidePanel()
                launchAppPage(OverlayBridge.TARGET_OPEN_DRAWING)
            }
            tile.key == "builtin_scanner" -> {
                hidePanel()
                launchAppPage(OverlayBridge.TARGET_OPEN_QR_SCANNER)
            }
            tile.key == "builtin_settings" -> {
                hidePanel()
                launchAppPage(OverlayBridge.TARGET_OPEN_SETTINGS)
            }
        }
    }

    private fun handlePanelLauncherTileLongClick(tile: OverlayLauncherTile, anchor: View): Boolean {
        if (panelEditMode || tile.isAddButton) return false
        tile.shortcut?.let {
            showAppShortcutsMenu(anchor, it)
            return true
        }
        return when (tile.key) {
            "builtin_subtitles" -> {
                hidePanel()
                launchQuickSubtitlePage()
                true
            }
            "builtin_quick_card" -> {
                hidePanel()
                launchQuickCardPage()
                true
            }
            else -> {
                when (tile.label) {
                    "快捷字幕" -> {
                        hidePanel()
                        launchQuickSubtitlePage()
                        true
                    }
                    "快捷名片" -> {
                        hidePanel()
                        launchQuickCardPage()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun showAppShortcutsMenu(anchor: View, shortcut: OverlayAppShortcut) {
        val popupStyle = overlayPopupMenuStyleRes()
        val menu =
            PopupMenu(
                ContextThemeWrapper(this, popupStyle),
                anchor,
                Gravity.NO_GRAVITY,
                0,
                popupStyle
            )
        val queriedShortcuts = queryLauncherShortcuts(shortcut)
        val manifestShortcuts =
            if (queriedShortcuts.isEmpty()) {
                queryManifestShortcuts(shortcut)
            } else {
                emptyList()
            }
        var nextId = 1
        if (queriedShortcuts.isNotEmpty()) {
            queriedShortcuts.forEach { info ->
                val title =
                    when {
                        !info.shortLabel.isNullOrBlank() -> info.shortLabel.toString()
                        !info.longLabel.isNullOrBlank() -> info.longLabel.toString()
                        else -> info.id
                    }
                menu.menu.add(0, nextId++, 0, title)
            }
            menu.menu.add(0, Int.MAX_VALUE - 1, 1, "打开应用")
        } else if (manifestShortcuts.isNotEmpty()) {
            manifestShortcuts.forEach { spec ->
                menu.menu.add(0, nextId++, 0, spec.title)
            }
            menu.menu.add(0, Int.MAX_VALUE - 1, 2, "打开应用")
        } else {
            menu.menu.add(0, Int.MAX_VALUE - 1, 0, "打开应用")
        }
        menu.setOnMenuItemClickListener { item ->
            if (item.itemId == Int.MAX_VALUE - 1) {
                hidePanel()
                launchExternalShortcut(shortcut)
                return@setOnMenuItemClickListener true
            }
            val index = item.itemId - 1
            val launcherTarget = queriedShortcuts.getOrNull(index)
            val manifestTarget =
                if (launcherTarget == null) manifestShortcuts.getOrNull(index) else null
            if (launcherTarget != null || manifestTarget != null) {
                hidePanel()
                when {
                    launcherTarget != null -> launchExternalLauncherShortcut(shortcut, launcherTarget)
                    manifestTarget != null -> launchExternalManifestShortcut(shortcut, manifestTarget)
                }
                true
            } else {
                false
            }
        }
        runCatching { menu.show() }.onFailure {
            AppLogger.e("FloatingOverlayService.showAppShortcutsMenu failed", it)
        }
    }

    private fun queryLauncherShortcuts(shortcut: OverlayAppShortcut): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        val launcherApps = getSystemService(LauncherApps::class.java) ?: return emptyList()
        val activity = android.content.ComponentName(shortcut.packageName, shortcut.className)
        return runCatching {
            val query =
                LauncherApps.ShortcutQuery().apply {
                    setPackage(shortcut.packageName)
                    setActivity(activity)
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )
                }
            launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty()
                .sortedWith(compareBy<ShortcutInfo>({ it.rank }, { it.shortLabel?.toString() ?: it.id }))
        }.onFailure {
            AppLogger.e("FloatingOverlayService.queryLauncherShortcuts failed", it)
        }.getOrElse { emptyList() }
    }

    private fun queryManifestShortcuts(shortcut: OverlayAppShortcut): List<ManifestShortcutSpec> {
        return runCatching {
            val component = android.content.ComponentName(shortcut.packageName, shortcut.className)
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                } else {
                    null
                }
            val activityInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getActivityInfo(component, flags!!)
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getActivityInfo(component, PackageManager.GET_META_DATA)
                }
            val shortcutsResId = activityInfo.metaData?.getInt("android.app.shortcuts", 0) ?: 0
            if (shortcutsResId == 0) return@runCatching emptyList()
            val foreign = createPackageContext(shortcut.packageName, Context.CONTEXT_IGNORE_SECURITY)
            val parser = foreign.resources.getXml(shortcutsResId)
            val androidNs = "http://schemas.android.com/apk/res/android"
            val result = mutableListOf<ManifestShortcutSpec>()
            var currentId = ""
            var currentTitle = ""
            var currentEnabled = true
            var currentIntents = mutableListOf<Intent>()
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when {
                    eventType == XmlPullParser.START_TAG && parser.name == "shortcut" -> {
                        currentId = parser.getAttributeValue(androidNs, "shortcutId").orEmpty().trim()
                        val labelResId = parser.getAttributeResourceValue(androidNs, "shortcutShortLabel", 0)
                        val rawLabel = parser.getAttributeValue(androidNs, "shortcutShortLabel").orEmpty().trim()
                        currentTitle =
                            when {
                                labelResId != 0 -> runCatching { foreign.resources.getString(labelResId) }.getOrDefault("")
                                rawLabel.startsWith("@string/") -> rawLabel.removePrefix("@string/")
                                else -> rawLabel
                            }.trim()
                        currentEnabled = parser.getAttributeBooleanValue(androidNs, "enabled", true)
                        currentIntents = mutableListOf()
                    }
                    eventType == XmlPullParser.START_TAG &&
                        parser.name == "intent" &&
                        currentId.isNotBlank() -> {
                        parseManifestShortcutIntent(
                            shortcut = shortcut,
                            parser = parser,
                            androidNs = androidNs
                        )?.let(currentIntents::add)
                    }
                    eventType == XmlPullParser.END_TAG && parser.name == "shortcut" -> {
                        if (currentEnabled && currentId.isNotBlank() && currentTitle.isNotBlank()) {
                            result += ManifestShortcutSpec(
                                id = currentId,
                                title = currentTitle,
                                intents = currentIntents.toList()
                            )
                        }
                        currentId = ""
                        currentTitle = ""
                        currentEnabled = true
                        currentIntents = mutableListOf()
                    }
                }
                eventType = parser.next()
            }
            result.distinctBy { it.id }
        }.onFailure {
            AppLogger.e("FloatingOverlayService.queryManifestShortcuts failed", it)
        }.getOrElse { emptyList() }
    }

    private fun parseManifestShortcutIntent(
        shortcut: OverlayAppShortcut,
        parser: XmlPullParser,
        androidNs: String
    ): Intent? {
        val action = parser.getAttributeValue(androidNs, "action")?.trim()
        val targetPackage = parser.getAttributeValue(androidNs, "targetPackage")?.trim()
        val targetClass = parser.getAttributeValue(androidNs, "targetClass")?.trim()
        val data = parser.getAttributeValue(androidNs, "data")?.trim()
        val mimeType = parser.getAttributeValue(androidNs, "mimeType")?.trim()
        val category = parser.getAttributeValue(androidNs, "targetCategory")?.trim()
        if (
            action.isNullOrBlank() &&
            targetPackage.isNullOrBlank() &&
            targetClass.isNullOrBlank() &&
            data.isNullOrBlank()
        ) {
            return null
        }
        return Intent().apply {
            if (!action.isNullOrBlank()) {
                setAction(action)
            }
            if (!data.isNullOrBlank() && !mimeType.isNullOrBlank()) {
                setDataAndType(Uri.parse(data), mimeType)
            } else if (!data.isNullOrBlank()) {
                setData(Uri.parse(data))
            } else if (!mimeType.isNullOrBlank()) {
                setType(mimeType)
            }
            if (!category.isNullOrBlank()) {
                addCategory(category)
            }
            when {
                !targetPackage.isNullOrBlank() && !targetClass.isNullOrBlank() -> {
                    component = android.content.ComponentName(targetPackage, targetClass)
                }
                !targetPackage.isNullOrBlank() -> {
                    `package` = targetPackage
                }
                !targetClass.isNullOrBlank() -> {
                    component = android.content.ComponentName(shortcut.packageName, targetClass)
                }
                else -> {
                    `package` = shortcut.packageName
                }
            }
        }
    }

    private fun launchExternalLauncherShortcut(
        shortcut: OverlayAppShortcut,
        shortcutInfo: ShortcutInfo
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            launchExternalShortcut(shortcut)
            return
        }
        val launcherApps = getSystemService(LauncherApps::class.java)
        if (launcherApps == null) {
            launchExternalShortcut(shortcut)
            return
        }
        runCatching {
            launcherApps.startShortcut(
                shortcut.packageName,
                shortcutInfo.id,
                null,
                null,
                Process.myUserHandle()
            )
        }.onFailure {
            AppLogger.e("FloatingOverlayService.launchExternalLauncherShortcut failed", it)
            launchExternalShortcut(shortcut)
        }
    }

    private fun launchExternalManifestShortcut(
        shortcut: OverlayAppShortcut,
        spec: ManifestShortcutSpec
    ) {
        val intents =
            spec.intents.map { source ->
                Intent(source).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (component == null && `package`.isNullOrBlank()) {
                        `package` = shortcut.packageName
                    }
                }
            }
        if (intents.isEmpty()) {
            launchExternalShortcut(shortcut)
            return
        }
        runCatching {
            if (intents.size == 1) {
                startActivity(intents.first())
            } else {
                startActivities(intents.toTypedArray())
            }
        }.onFailure {
            AppLogger.e("FloatingOverlayService.launchExternalManifestShortcut failed", it)
            launchExternalShortcut(shortcut)
        }
    }

    private fun refreshPanelIndicators() {
        val container = panelIndicatorContainer ?: return
        container.removeAllViews()
        repeat(panelPageCount) { index ->
            container.addView(
                View(this).apply {
                    background = circleDrawable(
                        if (panelPageIndex == index) overlayPrimaryColor() else overlayIndicatorInactiveColor()
                    )
                    setOnClickListener {
                        panelPageIndex = index
                        refreshPanelIndicators()
                        scrollPanelToPage(index, animate = true)
                    }
                },
                LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                    if (index > 0) leftMargin = dp(8)
                }
            )
        }
    }

    private fun refreshShortcutPickerUi() {
        val container = panelPickerListContainer ?: return
        container.removeAllViews()
        if (launchableAppsLoading) {
            container.addView(
                TextView(this).apply {
                    setTextColor(overlayOnSurfaceVariantColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    gravity = Gravity.CENTER
                    text = "正在加载应用..."
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            return
        }
        val query = panelPickerSearchQuery.trim().lowercase(Locale.getDefault())
        val items =
            if (query.isEmpty()) {
                launchableAppsCache
            } else {
                launchableAppsCache.filter { shortcut ->
                    shortcut.label.lowercase(Locale.getDefault()).contains(query) ||
                        shortcut.packageName.lowercase(Locale.getDefault()).contains(query) ||
                        shortcut.className.lowercase(Locale.getDefault()).contains(query)
                }
            }
        if (items.isEmpty()) {
            container.addView(
                TextView(this).apply {
                    setTextColor(overlayOnSurfaceVariantColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    gravity = Gravity.CENTER
                    text = if (query.isEmpty()) "未找到可添加的应用" else "没有匹配的应用"
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            return
        }
        items.forEachIndexed { index, shortcut ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
                setPadding(dp(10), dp(10), dp(10), dp(10))
                addView(
                    ImageView(this@FloatingOverlayService).apply {
                        val icon = resolveShortcutIcon(shortcut)
                        if (icon != null) setImageDrawable(icon)
                    },
                    LinearLayout.LayoutParams(dp(24), dp(24))
                )
                addView(spaceView(dp(12), 1))
                addView(
                    TextView(this@FloatingOverlayService).apply {
                        setTextColor(overlayOnSurfaceColor())
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                        text = shortcut.label
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                )
                if (overlayShortcuts.any {
                        it.packageName == shortcut.packageName && it.className == shortcut.className
                    }) {
                    addView(spaceView(dp(8), 1))
                    addView(symbolTextView("check", 18f, overlayPrimaryColor()))
                }
                setOnClickListener {
                    addOverlayShortcut(shortcut)
                    hideShortcutPicker()
                }
            }
            container.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) topMargin = dp(8)
                }
            )
        }
    }

    private fun addOverlayShortcut(shortcut: OverlayAppShortcut) {
        val exists = overlayShortcuts.any {
            it.packageName == shortcut.packageName && it.className == shortcut.className
        }
        if (exists) return
        overlayShortcuts = (overlayShortcuts + shortcut).toMutableList()
        saveOverlayShortcuts()
        val key = shortcutKey(shortcut)
        if (key !in overlayLauncherOrder) {
            overlayLauncherOrder.add(key)
            saveOverlayLauncherLayout()
        }
        refreshPanelUi()
    }

    private fun showShortcutPicker() {
        scope.launch {
            panelPickerSearchInput?.setText(panelPickerSearchQuery)
            updatePickerLayout()
            val overlay = panelPickerOverlay ?: return@launch
            overlay.visibility = View.VISIBLE
            overlay.animate().cancel()
            overlay.animate().alpha(1f).setDuration(160L).start()
            refreshShortcutPickerUi()
            if (!launchableAppsLoaded || launchableAppsCache.isEmpty()) {
                loadLaunchableApps()
            }
        }
    }

    private fun hideShortcutPicker() {
        val overlay = panelPickerOverlay ?: return
        panelPickerSearchQuery = ""
        panelPickerSearchInput?.setText("")
        overlay.animate().cancel()
        overlay.alpha = 0f
        overlay.visibility = View.GONE
    }

    private fun updatePickerLayout() {
        val params = panelPickerParams ?: return
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        panelPickerOverlay?.let { runCatching { windowManager.updateViewLayout(it, params) } }
    }

    private fun beginLauncherReorderTransition() {
        val pagerRecycler = panelPager?.getChildAt(0) as? ViewGroup ?: return
        android.transition.TransitionManager.beginDelayedTransition(
            pagerRecycler,
            android.transition.AutoTransition().apply { duration = 110L }
        )
    }

    private fun handlePanelDragWithinPager(sourceView: View, localX: Float) {
        val pager = panelPager ?: return
        val pagerLocation = IntArray(2)
        val viewLocation = IntArray(2)
        pager.getLocationOnScreen(pagerLocation)
        sourceView.getLocationOnScreen(viewLocation)
        val xInPager = (viewLocation[0] - pagerLocation[0] + localX).coerceIn(0f, pager.width.toFloat())
        handlePanelDragLocation(pager.width, xInPager)
    }

    private fun setOverlayStatusExpanded(expanded: Boolean, animate: Boolean = true) {
        if (overlayStatusExpanded == expanded) {
            if (!expanded && !animate) {
                listOfNotNull(panelStatusDetailRefs?.card, miniStatusDetailRefs?.card).forEach { view ->
                    view.animate().cancel()
                    view.alpha = 1f
                    view.translationY = 0f
                    view.visibility = View.GONE
                }
            }
            return
        }
        overlayStatusExpanded = expanded
        if (expanded) {
            if (panelVisible) updatePanelPosition()
            if (miniVisible) updateMiniPanelPosition()
        }
        listOfNotNull(panelStatusDetailRefs?.card, miniStatusDetailRefs?.card).forEach { view ->
            if (animate) {
                animateStatusCardVisibility(view, expanded)
            } else {
                view.animate().cancel()
                view.alpha = 1f
                view.translationY = 0f
                view.visibility = if (expanded) View.VISIBLE else View.GONE
            }
        }
        panelStatusTriggerContainer?.alpha = if (overlayStatusExpanded) 1f else 0.94f
        miniStatusTriggerContainer?.alpha = if (overlayStatusExpanded) 1f else 0.94f
    }

    private fun collapseOverlayStatusExpanded(animate: Boolean = true) {
        setOverlayStatusExpanded(expanded = false, animate = animate)
    }

    private fun toggleOverlayStatusExpanded() {
        setOverlayStatusExpanded(expanded = !overlayStatusExpanded, animate = true)
    }

    private fun animateStatusCardVisibility(view: View, show: Boolean) {
        view.animate().cancel()
        if (show) {
            if (view.visibility != View.VISIBLE) {
                view.alpha = 0f
                view.translationY = -dp(10).toFloat()
                view.visibility = View.VISIBLE
            }
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180L)
                .start()
        } else {
            if (view.visibility != View.VISIBLE) return
            view.animate()
                .alpha(0f)
                .translationY(-dp(10).toFloat())
                .setDuration(160L)
                .withEndAction {
                    view.alpha = 1f
                    view.translationY = 0f
                    view.visibility = View.GONE
                }
                .start()
        }
    }

    private fun refreshStatusDetailUi() {
        val inputLabel = effectiveInputDeviceLabel()
        val outputLabel = effectiveOutputDeviceLabel()
        val inputLevel = effectiveInputLevel()
        val playbackProgress = effectivePlaybackProgress()
        listOfNotNull(panelStatusDetailRefs, miniStatusDetailRefs).forEach { refs ->
            refs.inputProgress.progress = (inputLevel * 1000f).roundToInt().coerceIn(0, 1000)
            refs.playbackProgress.progress = (playbackProgress * 1000f).roundToInt().coerceIn(0, 1000)
            refs.inputLabel.text = inputLabel
            refs.outputLabel.text = outputLabel
            refs.pttIcon.text = if (settings.pushToTalkMode) "toggle_on" else "toggle_off"
            refs.pttIcon.setTextColor(if (settings.pushToTalkMode) overlayPrimaryColor() else overlayOnSurfaceVariantColor())
            refs.volumeLabel.text = "音量倍率：${settings.playbackGainPercent}%"
            refs.volumeSeekBar.progress = settings.playbackGainPercent.coerceIn(0, 1000)
        }
        panelStatusTriggerContainer?.alpha = if (overlayStatusExpanded) 1f else 0.94f
        miniStatusTriggerContainer?.alpha = if (overlayStatusExpanded) 1f else 0.94f
    }

    private fun createOverlayStatusDetailCard(): OverlayStatusDetailRefs {
        fun createProgressRow(icon: String): Pair<LinearLayout, ProgressBar> {
            val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 1000
                progress = 0
                progressTintList = ColorStateList.valueOf(overlayPrimaryColor())
                progressBackgroundTintList = ColorStateList.valueOf(overlaySliderTrackColor())
            }
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(symbolTextView(icon, 18f, overlayOnSurfaceColor()))
                addView(spaceView(dp(10), 1))
                addView(
                    progress,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                )
            } to progress
        }

        fun createDeviceRow(
            icon: String,
            onSelect: (Int) -> Unit,
            labels: List<Pair<Int, String>>
        ): Pair<LinearLayout, TextView> {
            val labelView = TextView(this).apply {
                setTextColor(overlayOnSurfaceColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    foreground = selectableDrawable()
                }
                setPadding(dp(2), dp(2), dp(2), dp(2))
                addView(symbolTextView(icon, 18f, overlayOnSurfaceColor()))
                addView(spaceView(dp(8), 1))
                addView(labelView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(spaceView(dp(4), 1))
                addView(symbolTextView("expand_more", 18f, overlayOnSurfaceVariantColor()))
                setOnClickListener { anchor ->
                    val popupStyle = overlayPopupMenuStyleRes()
                    PopupMenu(
                        ContextThemeWrapper(this@FloatingOverlayService, popupStyle),
                        anchor,
                        Gravity.NO_GRAVITY,
                        0,
                        popupStyle
                    ).apply {
                        labels.forEachIndexed { index, (_, text) -> menu.add(0, index, index, text) }
                        setOnMenuItemClickListener { item ->
                            labels.getOrNull(item.itemId)?.first?.let(onSelect)
                            true
                        }
                    }.show()
                }
            }
            return row to labelView
        }

        val (inputRow, inputProgress) = createProgressRow("mic")
        val (playbackRow, playbackProgress) = createProgressRow("graphic_eq")
        val (inputDeviceRow, inputDeviceLabel) = createDeviceRow(
            icon = "mic",
            onSelect = { value ->
                scope.launch(Dispatchers.IO) {
                    UserPrefs.setPreferredInputType(this@FloatingOverlayService, value)
                }
            },
            labels = preferredInputTypeOptions()
        )
        val (outputDeviceRow, outputDeviceLabel) = createDeviceRow(
            icon = "volume_up",
            onSelect = { value ->
                scope.launch(Dispatchers.IO) {
                    UserPrefs.setPreferredOutputType(this@FloatingOverlayService, value)
                }
            },
            labels = preferredOutputTypeOptions()
        )
        val pttIcon = symbolTextView("toggle_off", 28f, overlayOnSurfaceVariantColor())
        val pttRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = selectableDrawable()
            }
            setPadding(dp(2), dp(2), dp(2), dp(2))
            addView(symbolTextView("mic", 18f, overlayOnSurfaceColor()))
            addView(spaceView(dp(8), 1))
            addView(
                TextView(this@FloatingOverlayService).apply {
                    setTextColor(overlayOnSurfaceColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    text = "按住说话"
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(pttIcon)
            setOnClickListener {
                scope.launch(Dispatchers.IO) {
                    UserPrefs.setPushToTalkMode(this@FloatingOverlayService, !settings.pushToTalkMode)
                }
            }
        }
        val volumeLabel = TextView(this).apply {
            setTextColor(overlayOnSurfaceVariantColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        val volumeSeekBar = SeekBar(this).apply {
            max = 1000
            progress = settings.playbackGainPercent
            thumbTintList = ColorStateList.valueOf(overlayPrimaryColor())
            progressTintList = ColorStateList.valueOf(overlayPrimaryColor())
            progressBackgroundTintList = ColorStateList.valueOf(overlaySliderTrackColor())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val snapped = snapPlaybackGainPercent(progress)
                    if (seekBar?.progress != snapped) seekBar?.progress = snapped
                    scope.launch(Dispatchers.IO) {
                        UserPrefs.setPlaybackGainPercent(this@FloatingOverlayService, snapped)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(6).toFloat()
            clipChildren = false
            clipToPadding = false
            visibility = if (overlayStatusExpanded) View.VISIBLE else View.GONE
            alpha = if (overlayStatusExpanded) 1f else 0f
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { }
            addView(inputRow)
            addView(spaceView(1, dp(10)))
            addView(playbackRow)
            addView(spaceView(1, dp(12)))
            addView(inputDeviceRow)
            addView(spaceView(1, dp(8)))
            addView(outputDeviceRow)
            addView(spaceView(1, dp(8)))
            addView(pttRow)
            addView(spaceView(1, dp(8)))
            addView(volumeLabel)
            addView(
                volumeSeekBar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        return OverlayStatusDetailRefs(
            card = card,
            inputProgress = inputProgress,
            playbackProgress = playbackProgress,
            inputLabel = inputDeviceLabel,
            outputLabel = outputDeviceLabel,
            pttIcon = pttIcon,
            volumeLabel = volumeLabel,
            volumeSeekBar = volumeSeekBar
        )
    }

    private fun preferredInputTypeOptions(): List<Pair<Int, String>> = listOf(
        com.kgtts.app.audio.AudioRoutePreference.INPUT_AUTO to "自动",
        com.kgtts.app.audio.AudioRoutePreference.INPUT_BUILTIN_MIC to "内置麦克风/话筒",
        com.kgtts.app.audio.AudioRoutePreference.INPUT_USB to "USB 麦克风",
        com.kgtts.app.audio.AudioRoutePreference.INPUT_BLUETOOTH to "蓝牙麦克风",
        com.kgtts.app.audio.AudioRoutePreference.INPUT_WIRED to "有线麦克风"
    )

    private fun preferredOutputTypeOptions(): List<Pair<Int, String>> = listOf(
        com.kgtts.app.audio.AudioRoutePreference.OUTPUT_AUTO to "自动",
        com.kgtts.app.audio.AudioRoutePreference.OUTPUT_SPEAKER to "扬声器",
        com.kgtts.app.audio.AudioRoutePreference.OUTPUT_EARPIECE to "听筒",
        com.kgtts.app.audio.AudioRoutePreference.OUTPUT_BLUETOOTH to "蓝牙音频",
        com.kgtts.app.audio.AudioRoutePreference.OUTPUT_USB to "USB 音频",
        com.kgtts.app.audio.AudioRoutePreference.OUTPUT_WIRED to "有线耳机/线路"
    )

    private fun preferredInputTypeLabel(type: Int): String =
        preferredInputTypeOptions().firstOrNull { it.first == type }?.second ?: "自动"

    private fun preferredOutputTypeLabel(type: Int): String =
        preferredOutputTypeOptions().firstOrNull { it.first == type }?.second ?: "自动"

    private fun snapPlaybackGainPercent(value: Int): Int {
        val clamped = value.coerceIn(0, 1000)
        return if (abs(clamped - 100) <= 20) 100 else clamped
    }

    private fun overlayLayoutTransition(): LayoutTransition =
        LayoutTransition().apply {
            setDuration(110L)
            disableTransitionType(LayoutTransition.APPEARING)
            disableTransitionType(LayoutTransition.DISAPPEARING)
            enableTransitionType(LayoutTransition.CHANGING)
            setAnimateParentHierarchy(false)
        }

    private fun createSymbolEntryTile(
        label: String,
        icon: String,
        onClick: () -> Unit,
        plain: Boolean = false
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = if (plain) null else roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = if (plain) 0f else dp(4).toFloat()
            minimumHeight = dp(if (plain) 88 else 108)
            setPadding(dp(10), dp(if (plain) 10 else 16), dp(10), dp(if (plain) 10 else 16))
            addView(
                FrameLayout(this@FloatingOverlayService).apply {
                    clipChildren = false
                    clipToPadding = false
                    addView(
                        symbolTextView(icon, if (plain) 34f else 28f, overlayOnSurfaceColor()),
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER
                        )
                    )
                },
                LinearLayout.LayoutParams(dp(40), dp(40))
            )
            addView(spaceView(1, dp(10)))
            addView(
                TextView(this@FloatingOverlayService).apply {
                    setTextColor(overlayOnSurfaceColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    gravity = Gravity.CENTER
                    minLines = 2
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    text = label
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = selectableDrawable()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createAppEntryTile(shortcut: OverlayAppShortcut): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = null
            elevation = 0f
            minimumHeight = dp(88)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            addView(
                FrameLayout(this@FloatingOverlayService).apply {
                    addView(
                        ImageView(this@FloatingOverlayService).apply {
                            val icon = resolveShortcutIcon(shortcut)
                            if (icon != null) setImageDrawable(icon)
                        },
                        FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER)
                    )
                },
                LinearLayout.LayoutParams(dp(40), dp(40))
            )
            addView(spaceView(1, dp(10)))
            addView(
                TextView(this@FloatingOverlayService).apply {
                    setTextColor(overlayOnSurfaceColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    gravity = Gravity.CENTER
                    minLines = 2
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    text = shortcut.label
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = selectableDrawable()
            }
        }
    }

    private fun resolveShortcutIcon(shortcut: OverlayAppShortcut): Drawable? {
        val key = shortcutKey(shortcut)
        shortcutIconStateCache[key]?.let { state ->
            return runCatching { state.newDrawable(resources).mutate() }.getOrNull()
        }
        val drawable = runCatching {
            val component = android.content.ComponentName(shortcut.packageName, shortcut.className)
            packageManager.getActivityIcon(component)
        }.getOrNull()
        shortcutIconStateCache[key] = drawable?.constantState
        return drawable
    }

    private fun launchAppPage(target: String) {
        runCatching {
            startActivity(OverlayBridge.buildOpenPageIntent(this, target))
        }.onFailure {
            AppLogger.e("FloatingOverlayService.launchAppPage failed", it)
        }
    }

    private fun launchExternalShortcut(shortcut: OverlayAppShortcut) {
        runCatching {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = android.content.ComponentName(shortcut.packageName, shortcut.className)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }.onFailure {
            AppLogger.e("FloatingOverlayService.launchExternalShortcut failed", it)
        }
    }

    private fun scrollPanelToPage(index: Int, animate: Boolean) {
        val pager = panelPager ?: return
        val targetIndex = index.coerceIn(0, max(0, panelPageCount - 1))
        if (pager.currentItem != targetIndex || !animate) {
            pager.setCurrentItem(targetIndex, animate)
        }
    }

    private fun snapPanelToNearestPage() {
        val targetIndex = panelPageIndex.coerceIn(0, max(0, panelPageCount - 1))
        panelPageIndex = targetIndex
        refreshPanelIndicators()
        scrollPanelToPage(targetIndex, animate = true)
    }

    private fun defaultQuickSubtitleGroups(): List<QuickSubtitleGroupConfig> = listOf(
        QuickSubtitleGroupConfig(
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
        QuickSubtitleGroupConfig(
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
        QuickSubtitleGroupConfig(
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

    private fun Int.floorMod(mod: Int): Int {
        if (mod == 0) return 0
        val r = this % mod
        return if (r >= 0) r else r + mod
    }

    private fun resolveConfirmAction(rawX: Float, rawY: Float): OverlayReleaseAction {
        val overlay = confirmOverlay ?: return OverlayReleaseAction.SendToSubtitle
        val params = confirmParams ?: return OverlayReleaseAction.SendToSubtitle
        val overlayWidth = overlay.width.takeIf { it > 0 } ?: params.width
        val overlayHeight = overlay.height.takeIf { it > 0 } ?: params.height
        if (overlayWidth <= 0 || overlayHeight <= 0) return OverlayReleaseAction.SendToSubtitle

        val overlayLeft = params.x.toFloat()
        val overlayTop = params.y.toFloat()
        val overlayRight = overlayLeft + overlayWidth
        val overlayBottom = overlayTop + overlayHeight
        val leftBoundary = leftActionButton?.let { actionView ->
            val location = IntArray(2)
            actionView.getLocationOnScreen(location)
            location[0] + (actionView.width / 2f)
        } ?: (overlayLeft + overlayWidth * 0.35f)
        val rightBoundary = rightActionButton?.let { actionView ->
            val location = IntArray(2)
            actionView.getLocationOnScreen(location)
            location[0] + (actionView.width / 2f)
        } ?: (overlayRight - overlayWidth * 0.25f)

        if (rawY >= overlayBottom) return OverlayReleaseAction.SendToSubtitle
        if (rawX <= leftBoundary || rawX <= overlayLeft) return OverlayReleaseAction.SendToInput
        if (rawX >= rightBoundary || rawX >= overlayRight) return OverlayReleaseAction.Cancel
        return OverlayReleaseAction.SendToSubtitle
    }

    private fun activeOverlayContent(): View? = when {
        miniVisible -> miniContent
        panelVisible -> panelContent
        else -> null
    }

    private fun activeConfirmFab(): View? = when {
        miniVisible -> miniActionFab
        panelVisible -> panelActionFab
        else -> fabButton
    }

    private fun syncConfirmOverlayToActiveWindow() {
        val overlay = confirmOverlay ?: return
        val params = confirmParams ?: return
        val content = activeOverlayContent() ?: return
        val contentWidth = content.width.takeIf { it > 0 }
            ?: (content.layoutParams?.width ?: 0).takeIf { it > 0 }
            ?: return
        content.measure(
            View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(displayHeight(), View.MeasureSpec.AT_MOST)
        )
        val targetWidth = contentWidth
        val targetHeight = content.height.takeIf { it > 0 } ?: content.measuredHeight.takeIf { it > 0 } ?: return
        val layoutParams = content.layoutParams as? FrameLayout.LayoutParams ?: return
        params.width = targetWidth
        params.height = targetHeight
        params.x = layoutParams.leftMargin
        params.y = layoutParams.topMargin
        runCatching { windowManager.updateViewLayout(overlay, params) }
        overlay.post { layoutConfirmOverlayContents() }
    }

    private fun layoutConfirmOverlayContents() {
        val overlay = confirmOverlay ?: return
        val params = confirmParams ?: return
        val clip = confirmClipContainer ?: return
        val card = confirmTextCardView ?: return
        val left = leftActionButton ?: return
        val right = rightActionButton ?: return
        val fab = activeConfirmFab() ?: return
        if (overlay.width <= 0 || overlay.height <= 0) return

        val overlayLoc = IntArray(2)
        val fabLoc = IntArray(2)
        overlay.getLocationOnScreen(overlayLoc)
        fab.getLocationOnScreen(fabLoc)

        val fabWidth = fab.width.takeIf { it > 0 } ?: dp(74)
        val fabHeight = fab.height.takeIf { it > 0 } ?: dp(74)
        val fabCenterX = fabLoc[0] - overlayLoc[0] + fabWidth / 2f
        val fabCenterY = fabLoc[1] - overlayLoc[1] + fabHeight / 2f
        val actionSize = dp(64)
        val contentPadding = dp(12)
        val sideGap = dp(16)
        val sideCenterOffset = (fabWidth / 2f) + (actionSize / 2f) + sideGap
        val actionCenterY =
            fabCenterY.coerceIn((contentPadding + actionSize / 2).toFloat(), (overlay.height - contentPadding - actionSize / 2).toFloat())
        val actionTop = (actionCenterY - actionSize / 2f).roundToInt()

        left.measure(
            View.MeasureSpec.makeMeasureSpec(actionSize, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(actionSize, View.MeasureSpec.EXACTLY)
        )
        right.measure(
            View.MeasureSpec.makeMeasureSpec(actionSize, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(actionSize, View.MeasureSpec.EXACTLY)
        )

        left.x = (fabCenterX - sideCenterOffset - actionSize / 2f)
            .coerceAtLeast(contentPadding.toFloat())
        left.y = actionTop.toFloat()
        right.x = (fabCenterX + sideCenterOffset - actionSize / 2f)
            .coerceAtMost((overlay.width - actionSize - contentPadding).toFloat())
        right.y = actionTop.toFloat()

        val cardWidth = (overlay.width - dp(24)).coerceAtLeast(dp(168))
        card.measure(
            View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val cardHeight = card.measuredHeight
        val cardLeft = (fabCenterX - cardWidth / 2f)
            .coerceIn(contentPadding.toFloat(), (overlay.width - cardWidth - contentPadding).toFloat())
        val cardTop = (actionTop - cardHeight - dp(14)).coerceAtLeast(contentPadding).toFloat()
        card.x = cardLeft
        card.y = cardTop

        clip.invalidate()
    }

    private fun snapFabToEdge() {
        val params = fabParams ?: return
        val root = fabRoot ?: return
        fabIdleDockJob?.cancel()
        fabIdleDockJob = null
        fabIdleDocked = false
        root.alpha = 1f
        val screenWidth = displayWidth()
        val screenHeight = displayHeight()
        val maxX = fabMaxX(screenWidth)
        val leftDistance = params.x
        val rightDistance = maxX - params.x
        val targetX =
            if (leftDistance <= rightDistance) fabSnapLeftX() else fabSnapRightX(screenWidth)
        val targetY = params.y.coerceIn(fabMinY(), fabMaxY(screenHeight))
        val targetAnchor = buildFabAnchor(targetX, targetY, screenWidth, screenHeight)
        setFabAnchorForOrientation(currentFabOrientation, targetAnchor)
        saveOverlayLauncherLayout()
        val startX = params.x
        val startY = params.y
        fabSnapAnimator?.cancel()
        if (startX == targetX && startY == targetY) {
            params.x = targetX
            params.y = targetY
            windowManager.updateViewLayout(root, params)
            refreshFabIdleDockState()
            return
        }
        fabSnapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                params.x = (startX + (targetX - startX) * fraction).roundToInt()
                params.y = (startY + (targetY - startY) * fraction).roundToInt()
                runCatching { windowManager.updateViewLayout(root, params) }
            }
            addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        refreshFabIdleDockState()
                    }
                }
            )
            start()
        }
    }

    private fun clampFabToScreen() {
        val params = fabParams ?: return
        val maxX = fabMaxX(displayWidth())
        val maxY = max(0, displayHeight() - max(fabRoot?.measuredHeight ?: 0, dp(FAB_SIZE_DP)))
        params.x = params.x.coerceIn(0, maxX)
        params.y = params.y.coerceIn(dp(32), maxY)
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun circleActionButton(iconView: TextView, sizeDp: Int = 72): FrameLayout {
        return FrameLayout(this).apply {
            background = circleDrawable(overlayNeutralCircleColor())
            elevation = dp(6).toFloat()
            addView(
                iconView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            minimumWidth = dp(sizeDp)
            minimumHeight = dp(sizeDp)
        }
    }

    private fun isOverlayDarkTheme(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun overlayPrimaryColor(): Int = 0xFF038387.toInt()

    private fun overlayCardColor(): Int =
        if (overlayDarkTheme) 0xFF1D2023.toInt() else Color.WHITE

    private fun overlayOnSurfaceColor(): Int =
        if (overlayDarkTheme) 0xFFE4E8EB.toInt() else 0xFF111417.toInt()

    private fun overlayOnSurfaceVariantColor(): Int =
        if (overlayDarkTheme) 0xFFB6BEC4.toInt() else 0xFF495156.toInt()

    private fun overlayOutlineColor(): Int =
        if (overlayDarkTheme) 0xFF757F87.toInt() else 0xFF9CA5AC.toInt()

    private fun overlayIndicatorInactiveColor(): Int =
        if (overlayDarkTheme) overlayOutlineColor() else 0xFFB0BEC5.toInt()

    private fun overlayBubbleColor(): Int =
        if (overlayDarkTheme) 0xEE262A2E.toInt() else 0xF5FFFFFF.toInt()

    private fun overlayScrimColor(): Int =
        if (overlayDarkTheme) 0x88000000.toInt() else 0x55000000

    private fun overlaySliderTrackColor(): Int =
        if (overlayDarkTheme) 0x40E4E8EB.toInt() else 0x33038387

    private fun overlayNeutralCircleColor(): Int =
        if (overlayDarkTheme) 0xD93A3A3A.toInt() else 0xD960666B.toInt()

    private fun overlayConfirmGradientColors(): IntArray =
        if (overlayDarkTheme) {
            intArrayOf(0xC0000000.toInt(), 0x66000000, 0x00000000)
        } else {
            intArrayOf(0x8C000000.toInt(), 0x22000000, 0x00000000)
        }

    private fun overlayPopupMenuStyleRes(): Int =
        if (overlayDarkTheme) R.style.ThemeOverlay_KGTTS_OverlayPopup_Dark
        else R.style.ThemeOverlay_KGTTS_OverlayPopup_Light

    private fun roundedRectDrawable(radiusDp: Float, color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }

    private fun selectableDrawable(): Drawable =
        RippleDrawable(
            ColorStateList.valueOf(
                if (overlayDarkTheme) ColorUtils.setAlphaComponent(Color.WHITE, 40)
                else ColorUtils.setAlphaComponent(overlayPrimaryColor(), 38)
            ),
            null,
            roundedRectDrawable(overlayRadiusDp, Color.WHITE)
        )

    private fun circleDrawable(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }

    private fun symbolTextView(name: String, sp: Float, color: Int): TextView =
        TextView(this).apply {
            text = name
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            typeface = iconTypeface
            includeFontPadding = false
        }

    private fun spaceView(width: Int, height: Int): View =
        View(this).apply { layoutParams = ViewGroup.LayoutParams(width, height) }

    private fun usesAppRealtimeBridge(): Boolean =
        RealtimeRuntimeBridge.currentAppDelegate() != null

    private fun effectiveRunningState(): Boolean =
        if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().running
        } else {
            running
        }

    private fun effectiveLatestRecognizedText(): String =
        if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().latestRecognizedText.ifBlank { latestRecognizedText }
        } else {
            latestRecognizedText
        }

    private fun effectivePttStreamingText(): String =
        if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().pushToTalkStreamingText.ifBlank { pttStreamingText }
        } else {
            pttStreamingText
        }

    private fun effectivePttPressedState(): Boolean =
        if (usesAppRealtimeBridge()) {
            pttPressed || RealtimeRuntimeBridge.currentSnapshot().pushToTalkPressed
        } else {
            pttPressed
        }

    private fun effectiveInputLevel(): Float =
        if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().inputLevel.coerceIn(0f, 1f)
        } else {
            overlayInputLevel.coerceIn(0f, 1f)
        }

    private fun effectivePlaybackProgress(): Float =
        if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().playbackProgress.coerceIn(0f, 1f)
        } else {
            overlayPlaybackProgress.coerceIn(0f, 1f)
        }

    private fun effectiveInputDeviceLabel(): String =
        if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().inputDeviceLabel.ifBlank {
                preferredInputTypeLabel(settings.preferredInputType)
            }
        } else {
            overlayInputDeviceLabel.ifBlank { preferredInputTypeLabel(settings.preferredInputType) }
        }

    private fun effectiveOutputDeviceLabel(): String =
        if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().outputDeviceLabel.ifBlank {
                preferredOutputTypeLabel(settings.preferredOutputType)
            }
        } else {
            overlayOutputDeviceLabel.ifBlank { preferredOutputTypeLabel(settings.preferredOutputType) }
        }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value).roundToInt()
    private fun dp(value: Float): Int = (resources.displayMetrics.density * value).roundToInt()
    private fun displayWidth(): Int = resources.displayMetrics.widthPixels
    private fun displayHeight(): Int = resources.displayMetrics.heightPixels

    companion object {
        private const val CHANNEL_ID = "floating_overlay"
        private const val NOTIFICATION_ID = 3204
        private const val OWNER_TAG = "overlay"
        private const val FAB_SIZE_DP = 56
        private const val ACTION_STOP = "com.kgtts.app.action.OVERLAY_STOP"
        private const val ACTION_REFRESH = "com.kgtts.app.action.OVERLAY_REFRESH"

        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun refresh(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_REFRESH
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }
}
