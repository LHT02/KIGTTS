package com.lhtstudio.kigtts.app.overlay

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.content.pm.ShortcutInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Display
import android.view.HapticFeedbackConstants
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewConfiguration
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.lhtstudio.kigtts.app.service.RealtimeHostService
import com.lhtstudio.kigtts.app.service.RealtimeHostState
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.lhtstudio.kigtts.app.R
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.overlay.RealtimeRuntimeBridge
import com.lhtstudio.kigtts.app.service.VolumeHotkeyAccessibilityService
import com.lhtstudio.kigtts.app.ui.QuickCard
import com.lhtstudio.kigtts.app.ui.QuickCardType
import com.lhtstudio.kigtts.app.util.AlipayScannerSupport
import com.lhtstudio.kigtts.app.util.AppLogger
import com.lhtstudio.kigtts.app.util.QqScannerSupport
import com.lhtstudio.kigtts.app.util.QuickCardRenderCache
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
    private val qqAccessibilityScannerShortcutId = "__qq_scanner_accessibility__"
    private val defaultQuickSubtitleText = "快捷字幕\n大字幕"
    private val FAB_EDGE_LEFT = "left"
    private val FAB_EDGE_RIGHT = "right"
    private val fabIdleDockDelayMs = 3000L
    private val fabIdleDockAlpha = 0.56f
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val iconTypeface: Typeface? by lazy {
        runCatching {
            ResourcesCompat.getFont(this, R.font.material_symbols_sharp)
        }.onFailure {
            AppLogger.e("FloatingOverlayService.material symbol font load failed", it)
        }.getOrNull()
    }

    private var settings = UserPrefs.AppSettings()
    private var settingsJob: Job? = null

    private var fabRoot: LinearLayout? = null
    private var fabParams: WindowManager.LayoutParams? = null
    private var bubbleRow: LinearLayout? = null
    private var bubbleTextView: TextView? = null
    private var fabSpacerView: View? = null
    private var fabButtonHost: FrameLayout? = null
    private var fabButton: FrameLayout? = null
    private var fabIconView: ImageView? = null
    private var fabTouchDockEdge: String? = null
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
    private var panelBodyContainer: LinearLayout? = null
    private var panelCardView: LinearLayout? = null
    private var panelBottomBarView: FrameLayout? = null
    private var panelLandscapeRailView: LinearLayout? = null
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
    private var miniBodyContainer: LinearLayout? = null
    private var miniBodyHostView: FrameLayout? = null
    private var miniBottomBarView: FrameLayout? = null
    private var miniLandscapeRailView: LinearLayout? = null
    private var miniSubtitleTextView: TextView? = null
    private var miniSubtitleCardView: LinearLayout? = null
    private var miniSubtitleSeekBar: SeekBar? = null
    private var miniQuickItemsContainer: FrameLayout? = null
    private var miniQuickItemsRecyclerView: RecyclerView? = null
    private var miniQuickItemsLayoutManager: LinearLayoutManager? = null
    private var miniQuickItemsAdapter: MiniQuickTextAdapter? = null
    private var miniQuickItemsLeftFadeView: View? = null
    private var miniQuickItemsRightFadeView: View? = null
    private var miniQuickRow: LinearLayout? = null
    private var miniQuickRowCardView: LinearLayout? = null
    private var miniQuickRowDividerView: View? = null
    private var miniQuickItemsScrollerView: FrameLayout? = null
    private var miniQuickSwitcherView: LinearLayout? = null
    private var miniGroupIconView: TextView? = null
    private var miniGroupPrevButtonView: TextView? = null
    private var miniGroupNextButtonView: TextView? = null
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

    private var pttPressed = false
    private var pttTemporaryStart = false
    private var overlayHintText = ""
    private var currentDragAction = OverlayReleaseAction.SendToSubtitle
    private var overlayDarkTheme = false
    private var overlayStatusExpanded = false
    private var fabSnapAnimator: ValueAnimator? = null
    private var fabIdleDockJob: Job? = null
    private var fabIdleDocked = false
    private var fabVisibilityTarget: Boolean? = null
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
    private var realtimeHost: RealtimeHostService? = null
    private var realtimeHostBound = false
    private var realtimeHostState = RealtimeHostState()
    private var realtimeHostStateJob: Job? = null
    private val pendingRealtimeHostActions = mutableListOf<(RealtimeHostService) -> Unit>()
    private val realtimeHostConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? RealtimeHostService.LocalBinder ?: return
                realtimeHost = binder.getService()
                realtimeHostBound = true
                realtimeHostStateJob?.cancel()
                realtimeHostStateJob = scope.launch {
                    realtimeHost!!.stateFlow().collectLatest { snapshot ->
                        realtimeHostState = snapshot
                        updateFabUi()
                        if (pttPressed && confirmOverlay?.visibility == View.VISIBLE) {
                            updateConfirmVisuals(currentDragAction)
                        }
                    }
                }
                flushPendingRealtimeHostActions()
                scope.launch { updateFabUi() }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                realtimeHostStateJob?.cancel()
                realtimeHostStateJob = null
                realtimeHost = null
                realtimeHostBound = false
                scope.launch {
                    updateFabUi()
                    delay(800L)
                    if (settings.floatingOverlayEnabled && realtimeHost == null) {
                        ensureRealtimeHostBound()
                    }
                }
            }
        }
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

    private data class MiniQuickItemsScrollState(
        val position: Int,
        val offsetPx: Int
    )

    private var quickSubtitleGroups = defaultQuickSubtitleGroups()
    private var quickSubtitleSelectedGroupId = 1L
    private var miniQuickSubtitleSelectedGroupId: Long? = null
    private var quickSubtitleCurrentText = defaultQuickSubtitleText
    private var quickSubtitleFontSizeSp = 56f
    private var quickSubtitlePlayOnSend = true
    private var quickSubtitleBold = true
    private var quickSubtitleCentered = false
    private var quickSubtitleInputText = ""
    private var quickSubtitleNextGroupId = 4L
    private var quickSubtitleSaving = false
    private var quickSubtitleConfigLoaded = false
    private var topStatusLastContent = ""
    private var topStatusShowingText = false
    private var topStatusResetJob: Job? = null
    private var miniQuickItemsCollapsed = false
    private val miniQuickItemsScrollStates = mutableMapOf<String, MiniQuickItemsScrollState>()
    private var miniMode = MiniOverlayMode.Subtitle
    private var miniPreviewMode = MiniPreviewMode.None
    private var miniPreviewQuickCardId: Long? = null
    private var quickCards: List<QuickCard> = emptyList()
    private var quickCardSelectedIndex = 0
    private val miniQuickCardPageHeights = mutableMapOf<Long, Int>()
    private var miniQuickCardMeasureWidth = 0
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

    private data class OverlayQuickSubtitleFitResult(
        val fontSizeSp: Float,
        val needsScroll: Boolean
    )

    private data class OverlayPreviewCardSize(
        val width: Int,
        val height: Int
    )

    private enum class OverlayQuickCardRenderStyle {
        Panel,
        Preview
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

    private data class HardcodedShortcutSpec(
        val id: String,
        val title: String,
        val dataUri: String? = null,
        val targetClass: String? = null,
        val booleanExtras: Map<String, Boolean> = emptyMap(),
        val stringExtras: Map<String, String> = emptyMap()
    )

    private data class ExternalShortcutMenuTarget(
        val title: String,
        val launcherShortcut: ShortcutInfo? = null,
        val manifestShortcut: ManifestShortcutSpec? = null,
        val shortcutId: String = "",
        val hardcodedShortcut: HardcodedShortcutSpec? = null
    )

    private val blockedShortcutKeywords =
        listOf(
            "红包",
            "赚钱",
            "赚金币",
            "金币",
            "签到",
            "福利",
            "任务",
            "卸载",
            "清理",
            "清空缓存",
            "深度卸载",
            "安全卸载",
            "天天赚",
            "新人",
            "reward",
            "bonus",
            "coin",
            "gold",
            "checkin",
            "check-in",
            "sign in",
            "signin",
            "welfare",
            "uninstall",
            "clean",
            "cleanup"
        )

    private val hardcodedLauncherShortcuts =
        mapOf(
            "com.tencent.mobileqq" to listOf(
                HardcodedShortcutSpec("qq_scanner", "扫一扫"),
                HardcodedShortcutSpec("add_contact", "加好友"),
                HardcodedShortcutSpec("create_troop", "发起聊天"),
                HardcodedShortcutSpec("qq_pay", "收付款")
            ),
            "com.tencent.mm" to listOf(
                HardcodedShortcutSpec(
                    id = "launch_type_scan_qrcode",
                    title = "扫一扫",
                    targetClass = "com.tencent.mm.ui.LauncherUI",
                    booleanExtras = mapOf("LauncherUI.From.Scaner.Shortcut" to true)
                ),
                HardcodedShortcutSpec("launch_type_offline_wallet", "收付款"),
                HardcodedShortcutSpec("launch_type_my_qrcode", "我的二维码")
            ),
            "com.eg.android.AlipayGphone" to listOf(
                HardcodedShortcutSpec(
                    id = "1001",
                    title = "扫一扫",
                    dataUri = AlipayScannerSupport.ALIPAY_SCANNER_URI
                ),
                HardcodedShortcutSpec("1002", "收付款"),
                HardcodedShortcutSpec("1005", "乘车码"),
                HardcodedShortcutSpec("1003", "收钱")
            ),
            "com.zhihu.android" to listOf(
                HardcodedShortcutSpec("shortcut_id_search", "搜索"),
                HardcodedShortcutSpec("shortcut_id_qrscan", "扫一扫")
            ),
            "com.xingin.xhs" to listOf(
                HardcodedShortcutSpec("search", "一键搜索")
            ),
            "com.ss.android.ugc.aweme" to listOf(
                HardcodedShortcutSpec("shortcut_search", "一键搜索"),
                HardcodedShortcutSpec("shortcut_scan", "扫一扫"),
                HardcodedShortcutSpec("shortcut_message", "消息")
            ),
            "com.sankuai.meituan" to listOf(
                HardcodedShortcutSpec("scan_shortcut_id", "扫一扫"),
                HardcodedShortcutSpec("search_shortcut_id", "搜索"),
                HardcodedShortcutSpec("order_shortcut_id", "我的订单")
            ),
            "com.sdu.didi.psnger" to listOf(
                HardcodedShortcutSpec("from_here", "从这里出发"),
                HardcodedShortcutSpec("go_home", "回家"),
                HardcodedShortcutSpec("go_company", "去公司"),
                HardcodedShortcutSpec("open_scan", "扫一扫")
            ),
            "com.shizhuang.duapp" to listOf(
                HardcodedShortcutSpec("SEARCH", "一键搜索"),
                HardcodedShortcutSpec("SCAN", "扫一扫"),
                HardcodedShortcutSpec("PIC_SEARCH", "拍照搜同款")
            ),
            "com.netease.cloudmusic" to listOf(
                HardcodedShortcutSpec("melodyIdentify", "听歌识曲"),
                HardcodedShortcutSpec("privateRadio", "私人漫游"),
                HardcodedShortcutSpec("search", "搜索"),
                HardcodedShortcutSpec("playLocalMusic", "播放本地音乐")
            ),
            "com.xunmeng.pinduoduo" to listOf(
                HardcodedShortcutSpec("chat", "聊天消息"),
                HardcodedShortcutSpec("order", "订单详情"),
                HardcodedShortcutSpec("search_express", "查物流"),
                HardcodedShortcutSpec("one_click_search", "一键搜索")
            )
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
        val ttsIcon: TextView,
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
            val card = items.getOrNull(position)
            holder.container.addView(
                createMiniQuickCardPage(card),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            holder.container.post {
                val measuredHeight = holder.container.height
                if (measuredHeight > 0) {
                    miniQuickCardPageHeights[miniQuickCardHeightKey(card)] = measuredHeight
                    miniQuickCardPreviewContainer?.requestLayout()
                }
            }
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
            val landscapePhone = isPhoneLandscapeUi()
            holder.root.layoutParams = RecyclerView.LayoutParams(
                if (landscapePhone) ViewGroup.LayoutParams.MATCH_PARENT else dp(112),
                if (landscapePhone) dp(74) else dp(104)
            )
            holder.root.minimumWidth = if (landscapePhone) 0 else dp(112)
            holder.root.minimumHeight = if (landscapePhone) dp(74) else dp(104)
            holder.root.setPadding(
                if (landscapePhone) dp(12) else dp(14),
                if (landscapePhone) dp(10) else dp(8),
                if (landscapePhone) dp(12) else dp(14),
                if (landscapePhone) dp(10) else dp(20)
            )
            holder.textView.maxLines = if (landscapePhone) 2 else 3
            holder.textView.text = item
            holder.root.setOnClickListener {
                if (item.isBlank()) return@setOnClickListener
                performOverlayKeyHaptic(holder.root)
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
        currentFabOrientation = displayOrientation()
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

    private fun fabVisibleHeightPx(): Int =
        max(
            fabRoot?.measuredHeight?.takeIf { it > 0 } ?: 0,
            max(
                fabRoot?.height?.takeIf { it > 0 } ?: 0,
                dp(FAB_SIZE_DP)
            )
        )

    private fun fabMaxY(screenHeight: Int = displayHeight()): Int =
        max(fabMinY(), screenHeight - fabVisibleHeightPx())

    private fun fabMaxX(screenWidth: Int = displayWidth()): Int =
        max(0, screenWidth - fabWindowWidthPx())

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

    private fun fabDockExposedWidthPx(): Int {
        val buttonWidth =
            fabButton?.measuredWidth?.takeIf { it > 0 }
                ?: fabButton?.width?.takeIf { it > 0 }
                ?: dp(FAB_SIZE_DP)
        return max(1, buttonWidth / 2)
    }

    private fun fabDockOuterPaddingPx(): Int = dp(14)

    private fun fabDockShadowPaddingPx(): Int = dp(8)

    private fun lerpInt(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).roundToInt()
    }

    private fun fabDockVisibleWidthPx(): Int {
        return fabDockExposedWidthPx() + fabDockShadowPaddingPx()
    }

    private fun fabDockWindowWidthPx(): Int {
        return fabDockVisibleWidthPx() + fabDockOuterPaddingPx()
    }

    private fun fabExpandedWindowWidthPx(): Int {
        return dp(FAB_SIZE_DP) + fabDockOuterPaddingPx() * 2
    }

    private fun fabWindowWidthPx(): Int {
        val paramsWidth = fabParams?.width ?: 0
        return when {
            paramsWidth > 0 -> paramsWidth
            fabRoot?.measuredWidth?.takeIf { it > 0 } != null -> fabRoot!!.measuredWidth
            fabRoot?.width?.takeIf { it > 0 } != null -> fabRoot!!.width
            else -> dp(FAB_SIZE_DP) + fabDockOuterPaddingPx() * 2
        }
    }

    private fun cancelFabSnapAnimation() {
        val animator = fabSnapAnimator
        fabSnapAnimator = null
        animator?.cancel()
    }

    private fun dockedFabXForEdge(edge: String, screenWidth: Int): Int {
        val dockWindowWidth = fabDockWindowWidthPx()
        return if (edge == FAB_EDGE_LEFT) {
            0
        } else {
            max(0, screenWidth - dockWindowWidth)
        }
    }

    private fun expandedFabXForEdge(edge: String, screenWidth: Int): Int {
        return if (edge == FAB_EDGE_LEFT) {
            fabSnapLeftX()
        } else {
            max(0, screenWidth - fabExpandedWindowWidthPx())
        }
    }

    private fun expandedFabDragStartX(edge: String, currentX: Int, currentWindowWidth: Int): Int {
        if (edge != FAB_EDGE_RIGHT) return currentX
        val maxExpandedX = max(0, displayWidth() - fabExpandedWindowWidthPx())
        return (currentX + currentWindowWidth - fabExpandedWindowWidthPx())
            .coerceIn(0, maxExpandedX)
    }

    private fun updateFabDockLayout(edge: String) {
        val root = fabRoot ?: return
        val params = fabParams ?: return
        val host = fabButtonHost ?: return
        val button = fabButton ?: return
        val spacer = fabSpacerView
        val hostLayoutParams = host.layoutParams as? LinearLayout.LayoutParams ?: return
        val buttonLayoutParams = button.layoutParams as? FrameLayout.LayoutParams ?: return
        val buttonWidth =
            button.measuredWidth.takeIf { it > 0 }
                ?: button.width.takeIf { it > 0 }
                ?: dp(FAB_SIZE_DP)
        val outerPadding = fabDockOuterPaddingPx()
        if (fabIdleDocked) {
            val exposedWidth = fabDockExposedWidthPx()
            val shadowPadding = fabDockShadowPaddingPx()
            val visibleWidth = fabDockVisibleWidthPx()
            root.clipChildren = false
            root.clipToPadding = false
            root.gravity = Gravity.END
            root.setPadding(
                if (edge == FAB_EDGE_LEFT) 0 else outerPadding,
                dp(14),
                if (edge == FAB_EDGE_LEFT) outerPadding else 0,
                dp(14)
            )
            params.width = fabDockWindowWidthPx()
            spacer?.visibility = View.VISIBLE
            host.clipChildren = true
            host.clipToPadding = true
            hostLayoutParams.width = visibleWidth
            hostLayoutParams.height = dp(FAB_SIZE_DP)
            hostLayoutParams.gravity = Gravity.START
            buttonLayoutParams.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            button.translationX =
                if (edge == FAB_EDGE_LEFT) {
                    -(buttonWidth - exposedWidth).toFloat()
                } else {
                    shadowPadding.toFloat()
                }
        } else {
            root.clipChildren = false
            root.clipToPadding = false
            root.gravity = Gravity.END
            root.setPadding(outerPadding, dp(14), outerPadding, dp(14))
            params.width = fabExpandedWindowWidthPx()
            spacer?.visibility = View.VISIBLE
            host.clipChildren = false
            host.clipToPadding = false
            hostLayoutParams.width = dp(FAB_SIZE_DP)
            hostLayoutParams.height = dp(FAB_SIZE_DP)
            hostLayoutParams.gravity = Gravity.START
            buttonLayoutParams.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            button.translationX = 0f
        }
        host.layoutParams = hostLayoutParams
        button.layoutParams = buttonLayoutParams
        host.requestLayout()
        root.requestLayout()
    }

    private fun restoreFabFromIdleDock(animateRestore: Boolean = false) {
        if (!fabIdleDocked) return
        val params = fabParams ?: return
        val root = fabRoot ?: return
        fabIdleDockJob?.cancel()
        fabIdleDockJob = null
        cancelFabSnapAnimation()
        val anchor = currentOrientationFabAnchor()
            ?: buildFabAnchor(params.x, params.y, displayWidth(), displayHeight())
        val screenWidth = displayWidth()
        val screenHeight = displayHeight()
        val minY = fabMinY()
        val maxY = fabMaxY(screenHeight)
        fabIdleDocked = false
        updateFabDockLayout(anchor.edge)
        params.x = expandedFabXForEdge(anchor.edge, screenWidth)
        params.y =
            if (maxY <= minY) {
                minY
            } else {
                (minY + (maxY - minY) * anchor.verticalRatio.coerceIn(0f, 1f)).roundToInt()
            }
        root.alpha = 1f
        runCatching { windowManager.updateViewLayout(root, params) }
        if (animateRestore) animateFabRestoreFromDockEdge(anchor.edge)
    }

    private fun prepareFabForDockedDrag(edge: String, params: WindowManager.LayoutParams) {
        val root = fabRoot ?: return
        fabIdleDockJob?.cancel()
        fabIdleDockJob = null
        cancelFabSnapAnimation()
        resetFabRestoreAnimation()

        val currentWindowWidth =
            params.width.takeIf { it > 0 }
                ?: fabRoot?.width?.takeIf { it > 0 }
                ?: fabWindowWidthPx()
        val dragStartX = expandedFabDragStartX(edge, params.x, currentWindowWidth)
        fabIdleDocked = false
        updateFabDockLayout(edge)
        params.x = dragStartX
        downWinX = dragStartX
        root.alpha = 1f
        runCatching { windowManager.updateViewLayout(root, params) }
    }

    private fun resetFabRestoreAnimation() {
        fabButton?.animate()?.cancel()
        fabButton?.translationX = 0f
    }

    private fun animateFabRestoreFromDockEdge(edge: String, endAction: (() -> Unit)? = null) {
        val button = fabButton ?: run {
            endAction?.invoke()
            return
        }
        val buttonWidth =
            button.measuredWidth.takeIf { it > 0 }
                ?: button.width.takeIf { it > 0 }
                ?: dp(FAB_SIZE_DP)
        val startTranslation =
            if (edge == FAB_EDGE_LEFT) {
                -(buttonWidth - fabDockExposedWidthPx()).toFloat()
            } else {
                fabDockShadowPaddingPx().toFloat()
            }
        if (abs(startTranslation) < 0.5f) {
            endAction?.invoke()
            return
        }
        button.animate().cancel()
        button.translationX = startTranslation
        button.animate()
            .translationX(0f)
            .setDuration(160L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { endAction?.invoke() }
            .start()
    }

    private fun applyFabIdleDockVisualState() {
        val root = fabRoot ?: return
        if (fabIdleDocked) {
            bubbleRow?.visibility = View.GONE
            updateFabDockLayout(currentOrientationFabAnchor()?.edge ?: FAB_EDGE_RIGHT)
            root.alpha = fabIdleDockAlpha
        } else {
            updateFabDockLayout(currentOrientationFabAnchor()?.edge ?: FAB_EDGE_RIGHT)
            root.alpha = 1f
        }
    }

    private fun cancelFabIdleDock(restoreFab: Boolean, animateRestore: Boolean = false) {
        fabIdleDockJob?.cancel()
        fabIdleDockJob = null
        if (restoreFab) restoreFabFromIdleDock(animateRestore)
    }

    private fun resolveCurrentFabDockEdge(params: WindowManager.LayoutParams): String {
        return currentOrientationFabAnchor()?.edge
            ?: buildFabAnchor(params.x, params.y, displayWidth(), displayHeight()).edge
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
            val host = fabButtonHost ?: return@post
            val button = fabButton ?: return@post
            val spacer = fabSpacerView
            val hostLayoutParams = host.layoutParams as? LinearLayout.LayoutParams ?: return@post
            val buttonLayoutParams = button.layoutParams as? FrameLayout.LayoutParams ?: return@post
            val spacerLayoutParams = spacer?.layoutParams as? LinearLayout.LayoutParams
            val buttonWidth =
                button.measuredWidth.takeIf { it > 0 }
                    ?: button.width.takeIf { it > 0 }
                    ?: dp(FAB_SIZE_DP)
            val outerPadding = fabDockOuterPaddingPx()
            val startPaddingLeft = liveRoot.paddingLeft
            val startPaddingTop = liveRoot.paddingTop
            val startPaddingRight = liveRoot.paddingRight
            val startPaddingBottom = liveRoot.paddingBottom
            val startHostWidth = hostLayoutParams.width.takeIf { it > 0 } ?: dp(FAB_SIZE_DP)
            val startWindowWidth =
                liveParams.width.takeIf { it > 0 }
                    ?: liveRoot.width.takeIf { it > 0 }
                    ?: (dp(FAB_SIZE_DP) + outerPadding * 2)
            val startSpacerHeight =
                spacerLayoutParams?.height?.takeIf { it >= 0 }
                    ?: if (spacer?.visibility == View.VISIBLE) dp(12) else 0
            val startButtonTranslation = button.translationX
            val targetVisibleWidth = fabDockVisibleWidthPx()
            val targetWindowWidth = fabDockWindowWidthPx()
            val targetExposedWidth = fabDockExposedWidthPx()
            val targetShadowPadding = fabDockShadowPaddingPx()
            val targetPaddingLeft = if (anchor.edge == FAB_EDGE_LEFT) 0 else outerPadding
            val targetPaddingRight = if (anchor.edge == FAB_EDGE_LEFT) outerPadding else 0
            val targetPaddingVertical = dp(14)
            val targetSpacerHeight = startSpacerHeight.takeIf { it > 0 } ?: dp(12)
            val targetButtonTranslation =
                if (anchor.edge == FAB_EDGE_LEFT) {
                    -(buttonWidth - targetExposedWidth).toFloat()
                } else {
                    targetShadowPadding.toFloat()
                }
            fabIdleDocked = true
            val targetX = dockedFabXForEdge(anchor.edge, displayWidth())
            val targetY = liveParams.y
            val startX = liveParams.x
            val startY = liveParams.y
            val needsLayoutTransition =
                startHostWidth != targetVisibleWidth ||
                    startWindowWidth != targetWindowWidth ||
                    startPaddingLeft != targetPaddingLeft ||
                    startPaddingRight != targetPaddingRight ||
                    abs(startButtonTranslation - targetButtonTranslation) > 0.5f
            cancelFabSnapAnimation()
            if (startX == targetX && startY == targetY && !needsLayoutTransition) {
                updateFabDockLayout(anchor.edge)
                liveParams.x = targetX
                liveParams.y = targetY
                liveRoot.alpha = fabIdleDockAlpha
                runCatching { windowManager.updateViewLayout(liveRoot, liveParams) }
                return@post
            }
            var wasCancelled = false
            fabSnapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 220L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    val fraction = animator.animatedFraction
                    liveRoot.gravity = Gravity.END
                    liveRoot.clipChildren = false
                    liveRoot.clipToPadding = false
                    liveRoot.setPadding(
                        lerpInt(startPaddingLeft, targetPaddingLeft, fraction),
                        lerpInt(startPaddingTop, targetPaddingVertical, fraction),
                        lerpInt(startPaddingRight, targetPaddingRight, fraction),
                        lerpInt(startPaddingBottom, targetPaddingVertical, fraction)
                    )
                    spacer?.visibility = View.VISIBLE
                    spacer?.alpha = 1f
                    if (spacerLayoutParams != null) {
                        spacerLayoutParams.height = lerpInt(startSpacerHeight, targetSpacerHeight, fraction)
                        spacer.layoutParams = spacerLayoutParams
                    }
                    host.clipChildren = fraction > 0f
                    host.clipToPadding = fraction > 0f
                    hostLayoutParams.width = lerpInt(startHostWidth, targetVisibleWidth, fraction)
                    hostLayoutParams.height = dp(FAB_SIZE_DP)
                    hostLayoutParams.gravity = Gravity.START
                    host.layoutParams = hostLayoutParams
                    buttonLayoutParams.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    button.layoutParams = buttonLayoutParams
                    button.translationX =
                        startButtonTranslation + (targetButtonTranslation - startButtonTranslation) * fraction
                    liveParams.width = lerpInt(startWindowWidth, targetWindowWidth, fraction)
                    liveParams.x = (startX + (targetX - startX) * fraction).roundToInt()
                    liveParams.y = (startY + (targetY - startY) * fraction).roundToInt()
                    liveRoot.alpha = 1f + (fabIdleDockAlpha - 1f) * fraction
                    host.requestLayout()
                    liveRoot.requestLayout()
                    runCatching { windowManager.updateViewLayout(liveRoot, liveParams) }
                }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationCancel(animation: android.animation.Animator) {
                            wasCancelled = true
                        }

                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            if (fabSnapAnimator === animation) {
                                fabSnapAnimator = null
                            }
                            if (wasCancelled) return
                            spacer?.alpha = 1f
                            updateFabDockLayout(anchor.edge)
                            liveParams.x = targetX
                            liveParams.y = targetY
                            liveRoot.alpha = fabIdleDockAlpha
                            runCatching { windowManager.updateViewLayout(liveRoot, liveParams) }
                        }
                    }
                )
            }.also { it.start() }
        }
    }

    private fun scheduleFabIdleDock() {
        if (!canAutoDockFab() || fabIdleDocked) {
            applyFabIdleDockVisualState()
            return
        }
        if (fabIdleDockJob?.isActive == true) {
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
            applyFabIdleDockVisualState()
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
            holder.container.layoutTransition = if (panelEditMode) overlayLayoutTransition() else null
            if (panelEditMode) {
                android.transition.TransitionManager.beginDelayedTransition(holder.container)
            }
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

    override fun getResources(): Resources {
        val base = super.getResources()
        if (settings.fontScaleBlockMode != UserPrefs.FONT_SCALE_BLOCK_ALL) return base
        val config = Configuration(base.configuration)
        if (config.fontScale == 1f) return base
        config.fontScale = 1f
        return createConfigurationContext(config).resources
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("FloatingOverlayService.onCreate")
        if (!canDrawOverlays(this)) {
            stopSelf()
            return
        }
        RealtimeHostService.ensureStarted(this)
        ensureRealtimeHostBound()
        overlayDarkTheme = isOverlayDarkTheme()
        updateFabDisplaySnapshot()
        startForegroundInternal()
        ensureWindows()
        RealtimeRuntimeBridge.addListener(runtimeBridgeListener)
        observeSettings()
        scope.launch {
            settings = UserPrefs.getSettings(this@FloatingOverlayService)
            applyOverlayWindowFlags()
            loadQuickSubtitleConfig()
            loadOverlayShortcuts()
            loadOverlayLauncherLayout()
            restoreFabPositionForCurrentOrientation(allowOppositeConversion = true)
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
            ACTION_OPEN_PANEL -> {
                showPanel()
            }
            ACTION_OPEN_MINI_SUBTITLE -> {
                showMiniPanel(MiniOverlayMode.Subtitle)
            }
            ACTION_OPEN_MINI_QUICK_CARD -> {
                showMiniPanel(MiniOverlayMode.QuickCard)
            }
            ACTION_REFRESH -> {
                scope.launch {
                    settings = UserPrefs.getSettings(this@FloatingOverlayService)
                    applyOverlayWindowFlags()
                    loadQuickSubtitleConfig()
                    loadOverlayShortcuts()
                    loadOverlayLauncherLayout()
                    restoreFabPositionForCurrentOrientation(allowOppositeConversion = true)
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
        if (realtimeHostBound) {
            runCatching { unbindService(realtimeHostConnection) }
            realtimeHostBound = false
        }
        realtimeHostStateJob?.cancel()
        realtimeHostStateJob = null
        realtimeHost = null
        RealtimeRuntimeBridge.removeListener(runtimeBridgeListener)
        hideConfirmOverlay()
        removeWindows()
        scope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (settings.floatingOverlayEnabled && canDrawOverlays(this)) {
            runCatching { start(this) }
                .onFailure { AppLogger.e("FloatingOverlayService restart after task removed failed", it) }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        saveFabAnchorForOrientation(
            currentFabOrientation,
            lastFabDisplayWidth.takeIf { it > 0 } ?: displayWidth(),
            lastFabDisplayHeight.takeIf { it > 0 } ?: displayHeight()
        )
        currentFabOrientation = displayOrientation()
        lastFabDisplayWidth = displayWidth()
        lastFabDisplayHeight = displayHeight()
        val darkNow = isOverlayDarkTheme()
        if (darkNow != overlayDarkTheme) {
            overlayDarkTheme = darkNow
            rebuildWindowsPreservingState()
            return
        }
        cancelFabSnapAnimation()
        restoreFabPositionForCurrentOrientation(allowOppositeConversion = true)
        applyPanelExpandedLayout()
        applyMiniExpandedLayout()
        refreshPanelUi()
        refreshQuickSubtitleUi()
        refreshQuickCardUi()
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
                val previousDarkTheme = overlayDarkTheme
                val previousFontScaleBlockMode = settings.fontScaleBlockMode
                val previousShowOnLockScreen = settings.floatingOverlayShowOnLockScreen
                settings = next
                if (!next.floatingOverlayEnabled) {
                    stopSelf()
                    return@collectLatest
                }
                val darkNow = isOverlayDarkTheme()
                if (darkNow != previousDarkTheme) {
                    overlayDarkTheme = darkNow
                    rebuildWindowsPreservingState()
                    return@collectLatest
                }
                if (next.fontScaleBlockMode != previousFontScaleBlockMode) {
                    rebuildWindowsPreservingState()
                    return@collectLatest
                }
                if (next.floatingOverlayShowOnLockScreen != previousShowOnLockScreen) {
                    applyOverlayWindowFlags()
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
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Suppress("DEPRECATION")
    private fun overlayWindowFlags(
        notFocusable: Boolean = true,
        notTouchable: Boolean = false
    ): Int {
        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (notFocusable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (notTouchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        if (settings.floatingOverlayShowOnLockScreen) {
            flags = flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        }
        return flags
    }

    private fun updateOverlayWindowFlags(
        view: View?,
        params: WindowManager.LayoutParams?,
        notFocusable: Boolean = true,
        notTouchable: Boolean = false
    ) {
        if (view == null || params == null) return
        params.flags = overlayWindowFlags(
            notFocusable = notFocusable,
            notTouchable = notTouchable
        )
        runCatching { windowManager.updateViewLayout(view, params) }
            .onFailure { AppLogger.e("FloatingOverlayService.updateOverlayWindowFlags failed", it) }
    }

    private fun applyOverlayWindowFlags() {
        updateOverlayWindowFlags(fabRoot, fabParams)
        updateOverlayWindowFlags(panelRoot, panelParams)
        updateOverlayWindowFlags(panelPickerOverlay, panelPickerParams, notFocusable = false)
        updateOverlayWindowFlags(miniRoot, miniParams)
        updateOverlayWindowFlags(confirmOverlay, confirmParams, notTouchable = true)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("ClickableViewAccessibility")
    private fun ensureWindows() {
        if (fabRoot != null && panelRoot != null && miniRoot != null && confirmOverlay != null) return
        if (fabRoot != null || panelRoot != null || miniRoot != null || confirmOverlay != null) {
            removeWindows()
        }

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
                val text = effectiveLatestRecognizedText().trim()
                if (text.isNotEmpty()) {
                    launchQuickSubtitle(OverlayBridge.TARGET_SUBTITLE, text)
                }
            }
        }

        fabIconView = launcherFabIconView()
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
        fabButtonHost = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            addView(
                fabButton,
                FrameLayout.LayoutParams(dp(FAB_SIZE_DP), dp(FAB_SIZE_DP), Gravity.START or Gravity.CENTER_VERTICAL)
            )
        }
        fabSpacerView = spaceView(1, dp(12))

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
            addView(fabSpacerView)
            addView(
                fabButtonHost,
                LinearLayout.LayoutParams(dp(FAB_SIZE_DP), dp(FAB_SIZE_DP)).apply {
                    gravity = Gravity.START
                }
            )
        }

        fabParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            overlayWindowFlags(),
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
            alpha = 0f
            visibility = View.INVISIBLE
        }
        panelStatusLogoView = ImageView(this).apply {
            setImageResource(if (overlayDarkTheme) R.drawable.logo_white else R.drawable.logo_black)
            adjustViewBounds = true
            alpha = 1f
            visibility = View.VISIBLE
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        foreground = selectableDrawable()
                    }
                    setOnClickListener { openMainAppAndCollapseOverlay(fromMiniPanel = false) }
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
                        performOverlayKeyHaptic(this)
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
                        performOverlayKeyHaptic(this)
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
        panelBottomBarView = panelBottomBar
        panelLandscapeRailView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(8).toFloat()
            clipChildren = false
            clipToPadding = false
            setPadding(dp(10), dp(18), dp(10), dp(18))
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
        }
        panelCardView = panelCard
        panelBodyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        val panelBodyHost = panelBodyContainer!!

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
                panelBodyHost,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        applyPanelExpandedLayout()

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
            overlayWindowFlags(),
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
            overlayWindowFlags(notFocusable = false),
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
            alpha = 0f
            visibility = View.INVISIBLE
        }
        miniStatusLogoView = ImageView(this).apply {
            setImageResource(if (overlayDarkTheme) R.drawable.logo_white else R.drawable.logo_black)
            adjustViewBounds = true
            alpha = 1f
            visibility = View.VISIBLE
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        foreground = selectableDrawable()
                    }
                    setOnClickListener {
                        performOverlayKeyHaptic(this)
                        openMainAppAndCollapseOverlay(fromMiniPanel = true)
                    }
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
            setOnClickListener {
                performOverlayKeyHaptic(this)
                returnFromMiniToPanel()
            }
        }
        miniTopStripView = topStrip

        miniSubtitleTextView = TextView(this).apply {
            setTextColor(overlayOnSurfaceColor())
            maxLines = 5
            ellipsize = TextUtils.TruncateAt.END
        }
        applyOverlayQuickSubtitleTextAppearance(
            textView = miniSubtitleTextView!!,
            text = quickSubtitleCurrentText.ifBlank { defaultQuickSubtitleText },
            maxFontSizeSp = quickSubtitleFontSizeSp,
            minFontSizeSp = 18f,
            maxLines = 5,
            centerVerticallyWhenCentered = true
        )
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
                    refreshQuickSubtitleUi()
                    saveFloatingOverlayQuickSubtitleFontSize()
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
        miniSubtitleCardView = subtitleCard

        miniQuickItemsAdapter = MiniQuickTextAdapter()
        val quickItemsLayoutManager =
            LinearLayoutManager(this@FloatingOverlayService, RecyclerView.HORIZONTAL, false)
        miniQuickItemsLayoutManager = quickItemsLayoutManager
        miniQuickItemsRecyclerView = RecyclerView(this).apply {
            layoutManager = quickItemsLayoutManager
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
                        val vertical =
                            (parent.layoutManager as? LinearLayoutManager)?.orientation == RecyclerView.VERTICAL
                        val position = parent.getChildAdapterPosition(view)
                        if (vertical) {
                            outRect.top = if (position <= 0) 0 else dividerWidth
                            outRect.bottom = 0
                            outRect.left = 0
                            outRect.right = 0
                        } else {
                            outRect.top = 0
                            outRect.bottom = 0
                            outRect.left = if (position <= 0) 0 else dividerWidth
                            outRect.right = 0
                        }
                    }

                    override fun onDrawOver(
                        c: Canvas,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        val vertical =
                            (parent.layoutManager as? LinearLayoutManager)?.orientation == RecyclerView.VERTICAL
                        for (index in 0 until parent.childCount) {
                            val child = parent.getChildAt(index)
                            val position = parent.getChildAdapterPosition(child)
                            if (position <= 0) continue
                            if (vertical) {
                                val lineLeft = parent.paddingLeft + dividerInset
                                val lineRight = parent.width - parent.paddingRight - dividerInset
                                if (lineRight <= lineLeft) continue
                                val lineY = child.top - (dividerWidth / 2f)
                                c.drawLine(lineLeft.toFloat(), lineY, lineRight.toFloat(), lineY, dividerPaint)
                            } else {
                                val lineTop = parent.paddingTop + dividerInset
                                val lineBottom = parent.height - parent.paddingBottom - dividerInset
                                if (lineBottom <= lineTop) continue
                                val lineX = child.left - (dividerWidth / 2f)
                                c.drawLine(lineX, lineTop.toFloat(), lineX, lineBottom.toFloat(), dividerPaint)
                            }
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
        miniQuickItemsScrollerView = quickItemsScroller

        miniGroupIconView = symbolTextView("sentiment_satisfied", 22f, overlayOnSurfaceColor()).apply {
            gravity = Gravity.CENTER
            minWidth = dp(36)
            isClickable = true
            isFocusable = true
        }
        miniGroupPrevButtonView = symbolTextView("keyboard_arrow_up", 20f, overlayOnSurfaceColor()).apply {
            gravity = Gravity.CENTER
            setOnClickListener { shiftQuickSubtitleGroup(-1) }
        }
        miniGroupNextButtonView = symbolTextView("keyboard_arrow_down", 20f, overlayOnSurfaceColor()).apply {
            gravity = Gravity.CENTER
            setOnClickListener { shiftQuickSubtitleGroup(1) }
        }
        val groupSwipeTouchListener = createMiniGroupSwipeTouchListener()
        miniGroupIconView?.setOnTouchListener(groupSwipeTouchListener)
        miniGroupPrevButtonView?.setOnTouchListener(groupSwipeTouchListener)
        miniGroupNextButtonView?.setOnTouchListener(groupSwipeTouchListener)
        val quickSwitcher = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            isFocusable = true
            setPadding(dp(2), dp(4), dp(2), dp(4))
            minimumWidth = dp(44)
            addView(
                miniGroupPrevButtonView,
                LinearLayout.LayoutParams(dp(36), 0, 1f)
            )
            addView(
                miniGroupIconView,
                LinearLayout.LayoutParams(dp(36), dp(28)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            )
            addView(
                miniGroupNextButtonView,
                LinearLayout.LayoutParams(dp(36), 0, 1f)
            )
        }
        quickSwitcher.setOnTouchListener(groupSwipeTouchListener)

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
                    miniQuickRowDividerView = this
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
        miniQuickRowCardView = quickRow
        miniQuickSwitcherView = quickSwitcher
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
                    if (contentWidth != miniQuickCardMeasureWidth) {
                        miniQuickCardMeasureWidth = contentWidth
                        miniQuickCardPageHeights.clear()
                    }
                    val currentCard =
                        if (quickCards.isEmpty()) null else quickCards.getOrNull(pager?.currentItem ?: 0)
                    val cachedHeight = miniQuickCardPageHeights[miniQuickCardHeightKey(currentCard)]
                    val targetHeight = if (isPhoneLandscapeUi()) {
                        landscapeOverlayContentHeight()
                    } else {
                        when {
                            cachedHeight != null -> cachedHeight + paddingTop + paddingBottom
                            currentChild != null -> {
                                val childWidthSpec = MeasureSpec.makeMeasureSpec(
                                    contentWidth,
                                    MeasureSpec.EXACTLY
                                )
                                val childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                                currentChild.measure(childWidthSpec, childHeightSpec)
                                currentChild.measuredHeight.also {
                                    miniQuickCardPageHeights[miniQuickCardHeightKey(currentCard)] = it
                                } + paddingTop + paddingBottom
                            }
                            else -> {
                                measureMiniQuickCardPageHeight(contentWidth, currentCard).also {
                                    miniQuickCardPageHeights[miniQuickCardHeightKey(currentCard)] = it
                                } + paddingTop + paddingBottom
                            }
                        }.coerceAtLeast(miniQuickCardPortraitPreviewMinHeight())
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
        miniBodyHostView = miniBodyHost

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
        miniBottomBarView = bottomBar
        miniLandscapeRailView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(6).toFloat()
            clipChildren = false
            clipToPadding = false
            setPadding(dp(10), dp(18), dp(10), dp(18))
        }
        miniBodyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        val miniBodyHostContainer = miniBodyContainer!!

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
                miniBodyHostContainer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        applyMiniExpandedLayout()

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
            overlayWindowFlags(),
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

        val solidConfirmActionColor = ColorUtils.setAlphaComponent(overlayNeutralCircleColor(), 255)
        leftActionButton = circleActionButton(
            symbolTextView("open_in_new", 26f, Color.WHITE),
            64,
            solidConfirmActionColor
        ).apply { alpha = 0.58f }
        rightActionButton = circleActionButton(
            symbolTextView("close", 26f, Color.WHITE),
            64,
            solidConfirmActionColor
        ).apply { alpha = 0.58f }
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
            alpha = 1f
            translationY = 0f
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
            overlayWindowFlags(notTouchable = true),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        windowManager.addView(confirmOverlay, confirmParams)
        refreshQuickSubtitleUi()
        updateFabUi()
    }

    private fun removeWindows() {
        cancelFabSnapAnimation()
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
        panelBodyContainer = null
        panelCardView = null
        panelBottomBarView = null
        panelLandscapeRailView = null
        panelIndicatorContainer = null
        panelActionFab = null
        panelActionFabIconView = null
        panelEditButtonView = null
        panelOpenButtonView = null
        panelPrevPageButtonView = null
        panelNextPageButtonView = null
        panelPager = null
        panelPagerAdapter = null
        saveMiniQuickItemsScrollState()
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
        miniBodyContainer = null
        miniBodyHostView = null
        miniBottomBarView = null
        miniLandscapeRailView = null
        miniSubtitleCardView = null
        miniSubtitleTextView = null
        miniSubtitleSeekBar = null
        miniQuickItemsContainer = null
        miniQuickItemsLeftFadeView = null
        miniQuickItemsRightFadeView = null
        miniQuickItemsLayoutManager = null
        miniQuickRow = null
        miniQuickRowCardView = null
        miniQuickRowDividerView = null
        miniQuickItemsScrollerView = null
        miniQuickSwitcherView = null
        miniGroupIconView = null
        miniGroupPrevButtonView = null
        miniGroupNextButtonView = null
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
        fabButtonHost = null
        fabSpacerView = null
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
        val actionIcon = when {
            settings.pushToTalkMode && pttPressedState -> "settings_voice"
            settings.pushToTalkMode -> "mic"
            runningState -> "stop"
            else -> "play_arrow"
        }
        bubbleTextView?.text = latestText
        bubbleRow?.visibility = View.GONE
        val hasLatestResult = latestText.isNotBlank()
        val topMicIcon = when {
            settings.ttsDisabled -> "mic_off"
            settings.pushToTalkMode && pttPressedState -> "settings_voice"
            else -> "mic"
        }
        syncTopStatusContent(latestText)
        panelStatusMicIconView?.text = topMicIcon
        panelStatusEqIconView?.text = "graphic_eq"
        panelStatusMicProgressView?.progress = (inputLevel * 1000f).roundToInt().coerceIn(0, 1000)
        panelStatusEqProgressView?.progress = (playbackProgress * 1000f).roundToInt().coerceIn(0, 1000)
        panelStatusMicContainer?.alpha = if (settings.pushToTalkMode || pttPressedState) 1f else 0.68f
        panelStatusEqContainer?.alpha = if (runningState || hasLatestResult) 1f else 0.68f
        miniStatusMicIconView?.text = topMicIcon
        miniStatusEqIconView?.text = "graphic_eq"
        miniStatusMicProgressView?.progress = (inputLevel * 1000f).roundToInt().coerceIn(0, 1000)
        miniStatusEqProgressView?.progress = (playbackProgress * 1000f).roundToInt().coerceIn(0, 1000)
        miniStatusMicContainer?.alpha = if (settings.pushToTalkMode || pttPressedState) 1f else 0.68f
        miniStatusEqContainer?.alpha = if (runningState || hasLatestResult) 1f else 0.68f
        panelActionFabIconView?.text = actionIcon
        miniActionFabIconView?.text = actionIcon
        refreshStatusDetailUi()
        syncFabVisibility(!(miniVisible || panelVisible))
        fabButton?.alpha = if (pttPressedState) 0.94f else 1f
        panelActionFab?.alpha = if (pttPressedState) 0.94f else 1f
        miniActionFab?.alpha = if (pttPressedState) 0.94f else 1f
        if (fabIdleDocked) {
            bubbleRow?.visibility = View.GONE
        }
    }

    private fun syncFabVisibility(show: Boolean) {
        if (!show) {
            cancelFabIdleDock(restoreFab = true)
        }
        if (fabVisibilityTarget == show) return
        fabVisibilityTarget = show
        animateFabVisibility(show)
        if (show) {
            refreshFabIdleDockState()
        }
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

    private fun showTopStatusLogo() {
        val shouldAnimate = topStatusShowingText
        crossfadeTopStatusViews(
            incoming = panelStatusLogoView,
            outgoing = panelStatusTextView,
            outgoingEndVisibility = View.INVISIBLE,
            animate = shouldAnimate
        )
        crossfadeTopStatusViews(
            incoming = miniStatusLogoView,
            outgoing = miniStatusTextView,
            outgoingEndVisibility = View.INVISIBLE,
            animate = shouldAnimate
        )
        topStatusShowingText = false
    }

    private fun showTopStatusText(text: String) {
        panelStatusTextView?.text = text
        miniStatusTextView?.text = text
        val shouldAnimate = !topStatusShowingText
        crossfadeTopStatusViews(
            incoming = panelStatusTextView,
            outgoing = panelStatusLogoView,
            outgoingEndVisibility = View.GONE,
            animate = shouldAnimate
        )
        crossfadeTopStatusViews(
            incoming = miniStatusTextView,
            outgoing = miniStatusLogoView,
            outgoingEndVisibility = View.GONE,
            animate = shouldAnimate
        )
        topStatusShowingText = true
    }

    private fun crossfadeTopStatusViews(
        incoming: View?,
        outgoing: View?,
        outgoingEndVisibility: Int,
        animate: Boolean
    ) {
        incoming?.animate()?.cancel()
        outgoing?.animate()?.cancel()
        val incomingVisible = incoming?.visibility == View.VISIBLE && incoming.alpha >= 0.98f
        val outgoingHidden = outgoing == null || outgoing.visibility != View.VISIBLE || outgoing.alpha <= 0.02f
        if (!animate || (incomingVisible && outgoingHidden)) {
            incoming?.apply {
                alpha = 1f
                visibility = View.VISIBLE
            }
            outgoing?.apply {
                alpha = 0f
                visibility = outgoingEndVisibility
            }
            return
        }
        incoming?.apply {
            if (visibility != View.VISIBLE) {
                alpha = 0f
                visibility = View.VISIBLE
            }
            animate()
                .alpha(1f)
                .setDuration(180L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        outgoing?.animate()
            ?.alpha(0f)
            ?.setDuration(180L)
            ?.setInterpolator(DecelerateInterpolator())
            ?.withEndAction {
                outgoing.visibility = outgoingEndVisibility
            }
            ?.start()
    }

    private fun syncTopStatusContent(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) {
            topStatusLastContent = ""
            topStatusResetJob?.cancel()
            topStatusResetJob = null
            showTopStatusLogo()
            return
        }
        if (normalized != topStatusLastContent) {
            topStatusLastContent = normalized
            showTopStatusText(normalized)
            topStatusResetJob?.cancel()
            topStatusResetJob = scope.launch {
                delay(3000L)
                if (topStatusLastContent == normalized) {
                    showTopStatusLogo()
                }
            }
            return
        }
        if (topStatusShowingText) {
            showTopStatusText(normalized)
        }
    }

    private fun animateConfirmActionAlpha(view: View?, target: Float) {
        val targetView = view ?: return
        if (abs(targetView.alpha - target) < 0.02f) {
            targetView.alpha = target
            return
        }
        targetView.animate().cancel()
        targetView.animate()
            .alpha(target)
            .setDuration(140L)
            .start()
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
        val inactiveAlpha = 0.58f
        animateConfirmActionAlpha(
            leftActionButton,
            if (action == OverlayReleaseAction.SendToInput) 1f else inactiveAlpha
        )
        animateConfirmActionAlpha(
            rightActionButton,
            if (action == OverlayReleaseAction.Cancel) 1f else inactiveAlpha
        )
        activeConfirmFab()?.alpha = 1f
    }

    private fun mapCommitAction(action: OverlayReleaseAction): RealtimeRuntimeBridge.PttCommitAction =
        when (action) {
            OverlayReleaseAction.SendToSubtitle -> RealtimeRuntimeBridge.PttCommitAction.SendToSubtitle
            OverlayReleaseAction.SendToInput -> RealtimeRuntimeBridge.PttCommitAction.SendToInput
            OverlayReleaseAction.Cancel -> RealtimeRuntimeBridge.PttCommitAction.Cancel
        }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordAudioPermissionFromApp(startRealtimeOnGrant: Boolean) {
        overlayHintText = "需要麦克风权限"
        runCatching {
            startActivity(
                OverlayBridge.buildRequestRecordAudioPermissionIntent(
                    this,
                    startRealtimeOnGrant = startRealtimeOnGrant
                )
            )
        }.onFailure {
            AppLogger.e("FloatingOverlayService.requestRecordAudioPermission failed", it)
        }
    }

    private fun beginPttSession() {
        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermissionFromApp(startRealtimeOnGrant = false)
            updateFabUi()
            return
        }
        pttPressed = true
        pttTemporaryStart = !effectiveRunningState()
        overlayHintText = ""
        currentDragAction = OverlayReleaseAction.SendToSubtitle
        updateFabUi()
        if (settings.pushToTalkConfirmInput) showConfirmOverlay()
        runWithRealtimeHost("音频宿主初始化中") { host ->
            if (pttTemporaryStart) host.startRealtime()
            host.beginPushToTalkSession()
            host.setPushToTalkPressed(true)
        }
    }

    private fun finishPttSession(action: OverlayReleaseAction) {
        val text = effectivePttStreamingText().trim()
        val shouldStop = pttTemporaryStart
        pttPressed = false
        pttTemporaryStart = false
        hideConfirmOverlay()
        runWithRealtimeHost("音频宿主初始化中") { host ->
            host.commitPushToTalkSession(mapCommitAction(action))
            host.setPushToTalkPressed(false)
            if (shouldStop) host.stopRealtime()
        }
        if (action == OverlayReleaseAction.SendToInput && text.isNotEmpty()) {
            launchQuickSubtitle(OverlayBridge.TARGET_OPEN, "")
        }
        updateFabUi()
    }

    private fun submitQuickSubtitleText(text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        saveMiniQuickItemsScrollState()
        overlayHintText = ""
        quickSubtitleCurrentText = normalized
        saveQuickSubtitleConfig()
        refreshQuickSubtitleUi()
        updateFabUi()
        runWithRealtimeHost("音频宿主初始化中") { host ->
            host.submitQuickSubtitle(OverlayBridge.TARGET_SUBTITLE, normalized)
        }
    }

    private suspend fun startListeningInternal(showFailureInBubble: Boolean): Boolean {
        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermissionFromApp(startRealtimeOnGrant = true)
            updateFabUi()
            return false
        }
        if (effectiveRunningState()) return true
        overlayHintText = ""
        val queued = runWithRealtimeHost(
            if (showFailureInBubble) "音频宿主初始化中" else null
        ) { host ->
            host.startRealtime()
        }
        if (!queued) return false
        updateFabUi()
        return true
    }

    private suspend fun stopListeningInternal() {
        runWithRealtimeHost(null) { host ->
            host.stopRealtime()
        }
        updateFabUi()
    }

    private fun toggleContinuousMode() {
        scope.launch {
            if (effectiveRunningState()) {
                stopListeningInternal()
            } else {
                if (settings.ttsDisabled) {
                    overlayHintText = "TTS已禁用，如需打开，请打开顶部音频状态菜单将“禁用TTS”选项关闭"
                    Toast.makeText(this@FloatingOverlayService, overlayHintText, Toast.LENGTH_SHORT).show()
                    updateFabUi()
                }
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

    private fun openMainAppAndCollapseOverlay(fromMiniPanel: Boolean) {
        if (fromMiniPanel) {
            hideMiniPanel()
        } else {
            hidePanel()
        }
        launchAppPage(OverlayBridge.TARGET_OPEN)
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
                performOverlayKeyHaptic(fabButton)
                fabTouchDockEdge = if (fabIdleDocked) resolveCurrentFabDockEdge(params) else null
                cancelFabIdleDock(restoreFab = false, animateRestore = false)
                cancelFabSnapAnimation()
                if (fabTouchDockEdge == null) {
                    resetFabRestoreAnimation()
                }
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
                    val dockEdge = fabTouchDockEdge ?: if (fabIdleDocked) resolveCurrentFabDockEdge(params) else null
                    fabTouchDockEdge = null
                    resetFabRestoreAnimation()
                    cancelFabSnapAnimation()
                    if (dockEdge != null) {
                        prepareFabForDockedDrag(dockEdge, params)
                    }
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
                val tapRestoreEdge = fabTouchDockEdge
                fabTouchDockEdge = null
                if (!draggingFab) {
                    if (tapRestoreEdge != null) {
                        cancelFabIdleDock(restoreFab = true, animateRestore = false)
                        animateFabRestoreFromDockEdge(tapRestoreEdge) { togglePanel() }
                    } else {
                        togglePanel()
                    }
                } else {
                    resetFabRestoreAnimation()
                    snapFabToEdge()
                }
                draggingFab = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                fabTouchDockEdge = null
                resetFabRestoreAnimation()
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
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            performOverlayKeyHaptic(activeConfirmFab())
        }
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
            if (!canDrawOverlays(this@FloatingOverlayService)) {
                stopSelf()
                return@launch
            }
            ensureWindows()
            ensureRealtimeHostBound()
            collapseOverlayStatusExpanded(animate = false)
            val switchingFromMini = miniVisible
            panelVisible = true
            if (!switchingFromMini) {
                miniVisible = false
            }
            syncFabVisibility(false)
            loadOverlayShortcuts()
            loadOverlayLauncherLayout()
            refreshPanelUi()
            applyPanelExpandedLayout()
            updatePanelPosition()
            panelRoot?.visibility = View.VISIBLE
            if (switchingFromMini) {
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
            if (!canDrawOverlays(this@FloatingOverlayService)) {
                stopSelf()
                return@launch
            }
            ensureWindows()
            ensureRealtimeHostBound()
            collapseOverlayStatusExpanded(animate = false)
            val switchingFromPanel = panelVisible
            miniMode = mode
            miniVisible = true
            syncFabVisibility(false)
            when (miniMode) {
                MiniOverlayMode.Subtitle -> {
                    if (!quickSubtitleConfigLoaded) {
                        loadQuickSubtitleConfig()
                    }
                    refreshQuickSubtitleUi()
                }
                MiniOverlayMode.QuickCard -> {
                    loadQuickCardConfig()
                    refreshQuickCardUi()
                }
            }
            refreshMiniModeUi()
            applyMiniExpandedLayout()
            updateMiniPanelPosition()
            refreshMiniPreviewUi()
            miniRoot?.visibility = View.VISIBLE
            if (switchingFromPanel) {
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
                updateFabUi()
                animateOverlayIn(miniContent, fromBottom = false)
            }
        }
    }

    private fun hideMiniPanel() {
        if (!miniVisible) return
        if (miniMode == MiniOverlayMode.Subtitle) {
            saveMiniQuickItemsScrollState()
        }
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

    private fun displayOrientation(): Int =
        if (displayWidth() > displayHeight()) {
            Configuration.ORIENTATION_LANDSCAPE
        } else {
            Configuration.ORIENTATION_PORTRAIT
        }

    private fun displaySmallestWidthDp(): Int =
        (min(displayWidth(), displayHeight()) / resources.displayMetrics.density).roundToInt()

    private fun isTabletUi(): Boolean = displaySmallestWidthDp() >= 600

    private fun isLandscapeUi(): Boolean = displayOrientation() == Configuration.ORIENTATION_LANDSCAPE

    private fun isPhoneLandscapeUi(): Boolean = !isTabletUi() && isLandscapeUi()

    private fun isTabletLandscapeUi(): Boolean = isTabletUi() && isLandscapeUi()

    private fun landscapeOverlayContentHeight(): Int = dp(208)

    private fun reparentView(
        view: View?,
        parent: ViewGroup,
        params: ViewGroup.LayoutParams
    ) {
        val child = view ?: return
        (child.parent as? ViewGroup)?.removeView(child)
        parent.addView(child, params)
    }

    private fun weightedSpacer(): View = View(this)

    private fun applyPanelExpandedLayout() {
        val body = panelBodyContainer ?: return
        val card = panelCardView ?: return
        val bottomBar = panelBottomBarView ?: return
        val rail = panelLandscapeRailView ?: return
        val editButton = panelEditButtonView ?: return
        val openButton = panelOpenButtonView ?: return
        val actionFab = panelActionFab ?: return

        (bottomBar.parent as? ViewGroup)?.removeView(bottomBar)
        (rail.parent as? ViewGroup)?.removeView(rail)
        body.removeAllViews()
        bottomBar.removeAllViews()
        rail.removeAllViews()

        if (isPhoneLandscapeUi()) {
            val railWidth = dp(92)
            val railGap = dp(12)
            val cardWidth = (
                overlayContentWidthPx(phoneMaxDp = 360, tabletMaxDp = 400) -
                    ((panelContent?.paddingLeft ?: 0) + (panelContent?.paddingRight ?: 0)) -
                    railWidth -
                    railGap
                ).coerceAtLeast(dp(280))
            rail.minimumHeight = dp(248)
            reparentView(
                openButton,
                rail,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            )
            reparentView(
                weightedSpacer(),
                rail,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            reparentView(
                actionFab,
                rail,
                LinearLayout.LayoutParams(dp(74), dp(74)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            )
            reparentView(
                weightedSpacer(),
                rail,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            reparentView(
                editButton,
                rail,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            )

            body.orientation = LinearLayout.HORIZONTAL
            body.gravity = Gravity.TOP
            reparentView(
                card,
                body,
                LinearLayout.LayoutParams(
                    cardWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            )
            reparentView(
                rail,
                body,
                LinearLayout.LayoutParams(
                    railWidth,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    leftMargin = railGap
                }
            )
        } else {
            reparentView(
                editButton,
                bottomBar,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.START
                ).apply {
                    leftMargin = dp(28)
                }
            )
            reparentView(
                openButton,
                bottomBar,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.END
                ).apply {
                    rightMargin = dp(28)
                }
            )
            reparentView(
                actionFab,
                bottomBar,
                FrameLayout.LayoutParams(dp(74), dp(74), Gravity.CENTER)
            )

            reparentView(
                bottomBar,
                card,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(10)
                }
            )
            body.orientation = LinearLayout.VERTICAL
            body.gravity = Gravity.NO_GRAVITY
            reparentView(
                card,
                body,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun applyMiniExpandedLayout() {
        val bodyContainer = miniBodyContainer ?: return
        val bodyHost = miniBodyHostView ?: return
        val bottomBar = miniBottomBarView ?: return
        val rail = miniLandscapeRailView ?: return
        val backButton = miniBackButtonView ?: return
        val openButton = miniOpenButtonView ?: return
        val actionFab = miniActionFab ?: return

        (bottomBar.parent as? ViewGroup)?.removeView(bottomBar)
        (rail.parent as? ViewGroup)?.removeView(rail)
        bodyContainer.removeAllViews()
        bottomBar.removeAllViews()
        rail.removeAllViews()

        if (isPhoneLandscapeUi()) {
            rail.minimumHeight = dp(248)
            reparentView(
                openButton,
                rail,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            )
            reparentView(
                weightedSpacer(),
                rail,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            reparentView(
                actionFab,
                rail,
                LinearLayout.LayoutParams(dp(74), dp(74)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            )
            reparentView(
                weightedSpacer(),
                rail,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            reparentView(
                backButton,
                rail,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            )

            bodyContainer.orientation = LinearLayout.HORIZONTAL
            bodyContainer.gravity = Gravity.TOP
            reparentView(
                bodyHost,
                bodyContainer,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
            reparentView(
                rail,
                bodyContainer,
                LinearLayout.LayoutParams(
                    dp(92),
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    leftMargin = dp(12)
                }
            )
        } else {
            reparentView(
                backButton,
                bottomBar,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.START
                ).apply {
                    leftMargin = dp(28)
                }
            )
            reparentView(
                openButton,
                bottomBar,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.END
                ).apply {
                    rightMargin = dp(28)
                }
            )
            reparentView(
                actionFab,
                bottomBar,
                FrameLayout.LayoutParams(dp(74), dp(74), Gravity.CENTER)
            )

            bodyContainer.orientation = LinearLayout.VERTICAL
            bodyContainer.gravity = Gravity.NO_GRAVITY
            reparentView(
                bodyHost,
                bodyContainer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            reparentView(
                bottomBar,
                bodyContainer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
            )
        }
        refreshMiniSubtitleLayoutMetrics()
        refreshMiniQuickCardLayoutMetrics()
    }

    private fun refreshMiniSubtitleLayoutMetrics() {
        val subtitleText = miniSubtitleTextView ?: return
        val landscapePhone = isPhoneLandscapeUi()
        val subtitleBody = miniSubtitleBody
        val subtitleCard = miniSubtitleCardView
        val quickRow = miniQuickRow
        val quickRowCard = miniQuickRowCardView
        val quickSwitcher = miniQuickSwitcherView
        val quickDivider = miniQuickRowDividerView
        val quickItemsFrame = miniQuickItemsContainer
        val quickItemsContainer = miniQuickItemsContainer
        val quickItemsLayoutManager = miniQuickItemsLayoutManager
        val quickItemsRecycler = miniQuickItemsRecyclerView
        val quickFadeStart = miniQuickItemsLeftFadeView
        val quickFadeEnd = miniQuickItemsRightFadeView
        val prevButton = miniGroupPrevButtonView
        val nextButton = miniGroupNextButtonView
        val groupIcon = miniGroupIconView
        val subtitleHeight = when {
            landscapePhone -> landscapeOverlayContentHeight()
            miniQuickItemsCollapsed -> dp(296)
            else -> dp(180)
        }
        val landscapeQuickColumnHeight = subtitleHeight + dp(72)
        (subtitleText.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            if (lp.height != subtitleHeight) {
                lp.height = subtitleHeight
                subtitleText.layoutParams = lp
            }
        }
        subtitleBody?.orientation = if (landscapePhone) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        (subtitleCard?.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            if (landscapePhone) {
                lp.width = 0
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 1f
                lp.topMargin = 0
                lp.leftMargin = 0
            } else {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 0f
                lp.topMargin = 0
                lp.leftMargin = 0
            }
            subtitleCard.layoutParams = lp
        }
        (quickRow?.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            if (landscapePhone) {
                lp.width = dp(152)
                lp.height = landscapeQuickColumnHeight
                lp.weight = 0f
                lp.leftMargin = dp(12)
                lp.topMargin = 0
            } else {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 0f
                lp.leftMargin = 0
                lp.topMargin = dp(12)
            }
            quickRow.layoutParams = lp
        }
        quickRowCard?.minimumHeight = if (landscapePhone) landscapeQuickColumnHeight else 0
        quickRowCard?.orientation = if (landscapePhone) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        quickRowCard?.gravity = if (landscapePhone) Gravity.NO_GRAVITY else Gravity.CENTER_VERTICAL
        quickRowCard?.setPadding(
            if (landscapePhone) dp(4) else dp(8),
            if (landscapePhone) dp(8) else dp(8),
            if (landscapePhone) dp(4) else dp(8),
            if (landscapePhone) dp(8) else dp(8)
        )
        quickItemsRecycler?.setPadding(
            if (landscapePhone) dp(4) else dp(10),
            dp(8),
            if (landscapePhone) dp(4) else dp(10),
            dp(8)
        )
        (miniQuickItemsScrollerView?.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            if (landscapePhone) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.height = 0
                lp.weight = 1f
            } else {
                lp.width = 0
                lp.height = dp(104)
                lp.weight = 1f
            }
            miniQuickItemsScrollerView?.layoutParams = lp
        }
        (quickItemsFrame?.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = if (landscapePhone) ViewGroup.LayoutParams.MATCH_PARENT else dp(104)
            quickItemsFrame.layoutParams = lp
        }
        (miniQuickSwitcherView?.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            if (landscapePhone) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.height = dp(42)
                lp.weight = 0f
            } else {
                lp.width = dp(44)
                lp.height = dp(104)
                lp.weight = 0f
            }
            miniQuickSwitcherView?.layoutParams = lp
        }
        (quickDivider?.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            if (landscapePhone) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.height = dp(1)
                lp.leftMargin = 0
                lp.rightMargin = 0
                lp.topMargin = dp(8)
                lp.bottomMargin = dp(8)
            } else {
                lp.width = dp(1)
                lp.height = dp(84)
                lp.leftMargin = dp(8)
                lp.rightMargin = dp(8)
                lp.topMargin = 0
                lp.bottomMargin = 0
            }
            quickDivider.layoutParams = lp
        }
        if (quickSwitcher != null && prevButton != null && nextButton != null && groupIcon != null) {
            quickSwitcher.removeAllViews()
            quickSwitcher.orientation = if (landscapePhone) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            quickSwitcher.gravity = Gravity.CENTER
            quickSwitcher.setPadding(if (landscapePhone) dp(2) else dp(2), dp(4), if (landscapePhone) dp(2) else dp(2), dp(4))
            quickSwitcher.minimumWidth = if (landscapePhone) 0 else dp(44)
            prevButton.text = if (landscapePhone) "chevron_left" else "keyboard_arrow_up"
            nextButton.text = if (landscapePhone) "chevron_right" else "keyboard_arrow_down"
            if (landscapePhone) {
                reparentView(
                    prevButton,
                    quickSwitcher,
                    LinearLayout.LayoutParams(dp(28), dp(28))
                )
                reparentView(
                    groupIcon,
                    quickSwitcher,
                    LinearLayout.LayoutParams(0, dp(28), 1f).apply { gravity = Gravity.CENTER_VERTICAL }
                )
                reparentView(
                    nextButton,
                    quickSwitcher,
                    LinearLayout.LayoutParams(dp(28), dp(28))
                )
            } else {
                reparentView(
                    prevButton,
                    quickSwitcher,
                    LinearLayout.LayoutParams(dp(36), 0, 1f)
                )
                reparentView(
                    groupIcon,
                    quickSwitcher,
                    LinearLayout.LayoutParams(dp(36), dp(28)).apply { gravity = Gravity.CENTER_HORIZONTAL }
                )
                reparentView(
                    nextButton,
                    quickSwitcher,
                    LinearLayout.LayoutParams(dp(36), 0, 1f)
                )
            }
        }
        quickItemsLayoutManager?.let { layoutManager ->
            val targetOrientation = if (landscapePhone) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
            if (layoutManager.orientation != targetOrientation) {
                layoutManager.orientation = targetOrientation
                miniQuickItemsAdapter?.notifyDataSetChanged()
            }
        }
        quickItemsRecycler?.setPadding(
            if (landscapePhone) dp(8) else dp(10),
            if (landscapePhone) dp(8) else dp(8),
            if (landscapePhone) dp(8) else dp(10),
            if (landscapePhone) dp(8) else dp(8)
        )
        if (quickItemsContainer != null && quickFadeStart != null && quickFadeEnd != null) {
            if (landscapePhone) {
                quickFadeStart.background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(overlayCardColor(), Color.TRANSPARENT)
                )
                quickFadeEnd.background = GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    intArrayOf(overlayCardColor(), Color.TRANSPARENT)
                )
                quickFadeStart.layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(20),
                        Gravity.TOP
                    )
                quickFadeEnd.layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(20),
                        Gravity.BOTTOM
                    )
            } else {
                quickFadeStart.background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(overlayCardColor(), Color.TRANSPARENT)
                )
                quickFadeEnd.background = GradientDrawable(
                    GradientDrawable.Orientation.RIGHT_LEFT,
                    intArrayOf(overlayCardColor(), Color.TRANSPARENT)
                )
                quickFadeStart.layoutParams =
                    FrameLayout.LayoutParams(
                        dp(24),
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.START
                    )
                quickFadeEnd.layoutParams =
                    FrameLayout.LayoutParams(
                        dp(24),
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.END
                    )
            }
        }
        miniQuickRow?.requestLayout()
        miniSubtitleCardView?.requestLayout()
        miniSubtitleBody?.requestLayout()
    }

    private fun isFabAnchoredRight(): Boolean {
        val params = fabParams
        val centerX = (params?.x ?: (displayWidth() - dp(96))) + dp(FAB_SIZE_DP) / 2
        return centerX >= displayWidth() / 2
    }

    private fun overlayContentWidthPx(phoneMaxDp: Int, tabletMaxDp: Int): Int {
        val sideMargin = dp(16)
        val phoneMax = dp(phoneMaxDp)
        val tabletMax = dp(tabletMaxDp)
        val phoneLandscapeMax = dp(max(phoneMaxDp + 180, 520))
        return when {
            isTabletLandscapeUi() -> min(displayWidth() / 2 - sideMargin * 2, tabletMax)
            isPhoneLandscapeUi() -> min(displayWidth() - sideMargin * 2, phoneLandscapeMax)
            else -> min(displayWidth() - sideMargin * 2, phoneMax)
        }.coerceAtLeast(dp(280))
    }

    private fun overlayContentLeftPx(contentWidth: Int): Int {
        val sideMargin = dp(16)
        if (!isTabletLandscapeUi()) {
            val landscapePhoneBias = if (isPhoneLandscapeUi()) dp(16) else 0
            return (((displayWidth() - contentWidth) / 2) - landscapePhoneBias)
                .coerceIn(sideMargin, max(sideMargin, displayWidth() - contentWidth - sideMargin))
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
        val portraitBias = if (!isLandscapeUi()) dp(16) else 0
        return (((displayHeight() - contentHeight) / 2) - portraitBias)
            .coerceIn(topBottomMargin, max(topBottomMargin, displayHeight() - contentHeight - topBottomMargin))
    }

    private suspend fun loadQuickSubtitleConfig() {
        val raw = UserPrefs.getQuickSubtitleConfig(this)
        val overlayFontSize = UserPrefs.getFloatingOverlayQuickSubtitleFontSize(this)
            ?.coerceIn(28f, 96f)
        val sharedFontFallback = raw?.takeIf { it.isNotBlank() }?.let { json ->
            runCatching {
                JSONObject(json).optDouble("fontSizeSp", 56.0).toFloat().coerceIn(28f, 96f)
            }.getOrDefault(56f)
        } ?: 56f
        if (raw.isNullOrBlank()) {
            quickSubtitleGroups = defaultQuickSubtitleGroups()
            quickSubtitleSelectedGroupId = quickSubtitleGroups.first().id
            quickSubtitleCurrentText = defaultQuickSubtitleText
            quickSubtitleInputText = ""
            quickSubtitleFontSizeSp = overlayFontSize ?: 56f
            quickSubtitlePlayOnSend = true
            quickSubtitleBold = true
            quickSubtitleCentered = false
            quickSubtitleNextGroupId = 4L
            reconcileMiniQuickSubtitleState()
            quickSubtitleConfigLoaded = true
            return
        }
        runCatching { parseQuickSubtitleConfig(raw) }
            .onFailure { AppLogger.e("FloatingOverlayService.parseQuickSubtitleConfig failed", it) }
        quickSubtitleFontSizeSp = overlayFontSize ?: sharedFontFallback
        quickSubtitleConfigLoaded = true
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
        quickSubtitleCurrentText =
            root.optString("currentText", defaultQuickSubtitleText).ifBlank { defaultQuickSubtitleText }
        quickSubtitleInputText = root.optString("inputText", "")
        quickSubtitlePlayOnSend = root.optBoolean("playOnSend", true)
        quickSubtitleBold = root.optBoolean("fontBold", true)
        quickSubtitleCentered = root.optBoolean("textCentered", false)
        quickSubtitleNextGroupId = maxOf(maxId + 1L, 4L)
        reconcileMiniQuickSubtitleState()
    }

    private fun reconcileMiniQuickSubtitleState() {
        val validIds = quickSubtitleGroups.map { it.id }.toSet()
        miniQuickSubtitleSelectedGroupId = when {
            miniQuickSubtitleSelectedGroupId?.let { it in validIds } == true -> miniQuickSubtitleSelectedGroupId
            quickSubtitleSelectedGroupId in validIds -> quickSubtitleSelectedGroupId
            else -> quickSubtitleGroups.firstOrNull()?.id
        }
        miniQuickItemsScrollStates.keys
            .filter { key -> key.substringBefore(':').toLongOrNull() !in validIds }
            .forEach { key -> miniQuickItemsScrollStates.remove(key) }
    }

    private fun saveQuickSubtitleConfig() {
        if (quickSubtitleSaving) return
        quickSubtitleSaving = true
        scope.launch(Dispatchers.IO) {
            try {
                val preservedSharedFontSize = UserPrefs.getQuickSubtitleConfig(this@FloatingOverlayService)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { json ->
                        runCatching {
                            JSONObject(json).optDouble("fontSizeSp", 56.0).toFloat().coerceIn(28f, 96f)
                        }.getOrDefault(56f)
                    } ?: 56f
                val payload = JSONObject().apply {
                    put("selectedGroupId", quickSubtitleSelectedGroupId)
                    put("fontSizeSp", preservedSharedFontSize.toDouble())
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
                UserPrefs.setQuickSubtitleConfig(this@FloatingOverlayService, payload)
            } finally {
                quickSubtitleSaving = false
            }
        }
    }

    private fun saveFloatingOverlayQuickSubtitleFontSize() {
        val sizeSp = quickSubtitleFontSizeSp.coerceIn(28f, 96f)
        scope.launch(Dispatchers.IO) {
            UserPrefs.setFloatingOverlayQuickSubtitleFontSize(this@FloatingOverlayService, sizeSp)
        }
    }

    private fun applyOverlayQuickSubtitleTextAppearance(
        textView: TextView,
        text: CharSequence,
        maxFontSizeSp: Float,
        minFontSizeSp: Float,
        maxLines: Int? = null,
        viewportView: View = textView,
        centerVerticallyWhenCentered: Boolean = false
    ) {
        textView.text = text
        textView.typeface = if (quickSubtitleBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textView.gravity = if (quickSubtitleCentered) {
            if (centerVerticallyWhenCentered) Gravity.CENTER else Gravity.CENTER_HORIZONTAL or Gravity.TOP
        } else {
            Gravity.START or Gravity.TOP
        }
        textView.textAlignment = if (quickSubtitleCentered) {
            View.TEXT_ALIGNMENT_CENTER
        } else {
            View.TEXT_ALIGNMENT_VIEW_START
        }
        if (maxLines != null) {
            textView.maxLines = maxLines
            textView.ellipsize = TextUtils.TruncateAt.END
        } else {
            textView.maxLines = Int.MAX_VALUE
            textView.ellipsize = null
        }
        if (!settings.quickSubtitleAutoFit) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, maxFontSizeSp)
            return
        }

        fun applyFitWhenReady(remainingRetries: Int) {
            val availableWidth =
                (viewportView.width - viewportView.paddingLeft - viewportView.paddingRight)
                    .coerceAtLeast(0)
            val availableHeight =
                (viewportView.height - viewportView.paddingTop - viewportView.paddingBottom)
                    .coerceAtLeast(0)
            if (availableWidth <= 0 || availableHeight <= 0) {
                if (remainingRetries > 0) {
                    viewportView.postDelayed({ applyFitWhenReady(remainingRetries - 1) }, 16L)
                    return
                }
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, maxFontSizeSp)
                return
            }
            val fit = measureOverlayQuickSubtitleText(
                text = text,
                widthPx = availableWidth,
                heightPx = availableHeight,
                maxFontSizeSp = maxFontSizeSp,
                minFontSizeSp = minFontSizeSp,
                maxLines = maxLines
            )
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fit.fontSizeSp)
            if (maxLines != null) {
                textView.maxLines = maxLines
                textView.ellipsize = TextUtils.TruncateAt.END
            } else {
                textView.ellipsize = null
            }
        }
        viewportView.post { applyFitWhenReady(6) }
    }

    private fun measureOverlayQuickSubtitleText(
        text: CharSequence,
        widthPx: Int,
        heightPx: Int,
        maxFontSizeSp: Float,
        minFontSizeSp: Float,
        maxLines: Int?
    ): OverlayQuickSubtitleFitResult {
        val boundedMaxSp = maxFontSizeSp.coerceAtLeast(minFontSizeSp)
        val minSp = minFontSizeSp.roundToInt().coerceAtLeast(1)
        val maxSp = boundedMaxSp.roundToInt().coerceAtLeast(minSp)
        val layoutAlignment =
            if (quickSubtitleCentered) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        val textValue = text.ifEmpty { defaultQuickSubtitleText }

        fun fits(sp: Int): Boolean {
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = overlayOnSurfaceColor()
                typeface = if (quickSubtitleBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                textSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    sp.toFloat(),
                    resources.displayMetrics
                )
            }
            val layout = StaticLayout.Builder
                .obtain(textValue, 0, textValue.length, textPaint, widthPx)
                .setAlignment(layoutAlignment)
                .setIncludePad(false)
                .build()
            if (maxLines != null && layout.lineCount > maxLines) {
                return false
            }
            val usedHeight = if (maxLines != null && layout.lineCount > 0) {
                layout.getLineBottom(min(layout.lineCount, maxLines) - 1)
            } else {
                layout.height
            }
            return usedHeight <= heightPx
        }

        var bestSp = minSp
        var low = minSp
        var high = maxSp
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (fits(mid)) {
                bestSp = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return OverlayQuickSubtitleFitResult(
            fontSizeSp = bestSp.toFloat(),
            needsScroll = !fits(bestSp)
        )
    }

    private fun currentMiniQuickSubtitleGroupId(): Long {
        val selectedId = miniQuickSubtitleSelectedGroupId
        if (selectedId != null && quickSubtitleGroups.any { it.id == selectedId }) {
            return selectedId
        }
        val fallbackId =
            quickSubtitleGroups.firstOrNull { it.id == quickSubtitleSelectedGroupId }?.id
                ?: quickSubtitleGroups.firstOrNull()?.id
                ?: defaultQuickSubtitleGroups().first().id
        miniQuickSubtitleSelectedGroupId = fallbackId
        return fallbackId
    }

    private fun selectedQuickSubtitleGroup(): QuickSubtitleGroupConfig {
        val selectedId = currentMiniQuickSubtitleGroupId()
        return quickSubtitleGroups.firstOrNull { it.id == selectedId }
            ?: quickSubtitleGroups.firstOrNull()
            ?: defaultQuickSubtitleGroups().first()
    }

    private fun shiftQuickSubtitleGroup(delta: Int) {
        if (quickSubtitleGroups.isEmpty()) return
        saveMiniQuickItemsScrollState()
        val currentGroupId = currentMiniQuickSubtitleGroupId()
        val currentIndex = quickSubtitleGroups.indexOfFirst { it.id == currentGroupId }
            .coerceAtLeast(0)
        val nextIndex = (currentIndex + delta).floorMod(quickSubtitleGroups.size)
        if (nextIndex == currentIndex) return
        performOverlayKeyHaptic(miniQuickSwitcherView ?: miniGroupIconView)
        miniQuickSubtitleSelectedGroupId = quickSubtitleGroups[nextIndex].id
        refreshQuickSubtitleUi()
    }

    private fun miniQuickItemsScrollKey(groupId: Long): String {
        return "${groupId}:${if (isPhoneLandscapeUi()) "landscape" else "portrait"}"
    }

    private fun saveMiniQuickItemsScrollState(groupId: Long = currentMiniQuickSubtitleGroupId()) {
        val recycler = miniQuickItemsRecyclerView ?: return
        val layoutManager = miniQuickItemsLayoutManager ?: return
        val position = layoutManager.findFirstVisibleItemPosition()
        if (position == RecyclerView.NO_POSITION) return
        val child = layoutManager.findViewByPosition(position) ?: return
        val offset = if (layoutManager.orientation == RecyclerView.VERTICAL) {
            child.top - recycler.paddingTop
        } else {
            child.left - recycler.paddingLeft
        }
        miniQuickItemsScrollStates[miniQuickItemsScrollKey(groupId)] =
            MiniQuickItemsScrollState(position = position, offsetPx = offset)
    }

    private fun restoreMiniQuickItemsScrollState(groupId: Long) {
        val recycler = miniQuickItemsRecyclerView ?: return
        val layoutManager = miniQuickItemsLayoutManager ?: return
        val saved = miniQuickItemsScrollStates[miniQuickItemsScrollKey(groupId)]
        recycler.post {
            val itemCount = miniQuickItemsAdapter?.itemCount ?: 0
            if (itemCount <= 0) {
                updateMiniQuickItemsEdgeFade(false)
                return@post
            }
            val position = (saved?.position ?: 0).coerceIn(0, itemCount - 1)
            val offset = saved?.offsetPx ?: 0
            layoutManager.scrollToPositionWithOffset(position, offset)
            recycler.post { updateMiniQuickItemsEdgeFade(false) }
        }
    }

    private fun refreshQuickSubtitleUi() {
        val group = selectedQuickSubtitleGroup()
        val landscapePhone = isPhoneLandscapeUi()
        miniSubtitleTextView?.apply {
            layoutParams = (layoutParams as? LinearLayout.LayoutParams)?.apply {
                height = when {
                    landscapePhone -> landscapeOverlayContentHeight()
                    miniQuickItemsCollapsed -> dp(296)
                    else -> dp(180)
                }
            }
            requestLayout()
            applyOverlayQuickSubtitleTextAppearance(
                textView = this,
                text = quickSubtitleCurrentText.ifBlank { defaultQuickSubtitleText },
                maxFontSizeSp = quickSubtitleFontSizeSp,
                minFontSizeSp = 18f,
                maxLines = 5,
                centerVerticallyWhenCentered = true
            )
        }
        miniSubtitleSeekBar?.progress =
            (quickSubtitleFontSizeSp - 28f).roundToInt().coerceIn(0, 68)
        miniGroupIconView?.text = group.icon
        miniQuickCollapseButton?.text = when {
            landscapePhone && miniQuickItemsCollapsed -> "chevron_left"
            landscapePhone -> "chevron_right"
            miniQuickItemsCollapsed -> "expand_less"
            else -> "expand_more"
        }
        miniQuickRow?.visibility = if (miniQuickItemsCollapsed) View.GONE else View.VISIBLE
        miniQuickRow?.requestLayout()
        miniQuickItemsAdapter?.submitItems(group.items)
        refreshMiniSubtitleLayoutMetrics()
        restoreMiniQuickItemsScrollState(group.id)
        refreshMiniPreviewUi()
    }

    private fun refreshMiniQuickCardLayoutMetrics() {
        val body = miniQuickCardBody ?: return
        val previewContainer = miniQuickCardPreviewContainer ?: return
        val indicatorContainer = miniQuickCardIndicatorContainer ?: return
        val landscapePhone = isPhoneLandscapeUi()

        body.orientation = if (landscapePhone) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        body.gravity = if (landscapePhone) Gravity.CENTER_VERTICAL else Gravity.NO_GRAVITY

        (previewContainer.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            if (landscapePhone) {
                lp.width = 0
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 1f
                lp.topMargin = 0
                lp.leftMargin = 0
            } else {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 0f
                lp.topMargin = 0
                lp.leftMargin = 0
            }
            previewContainer.layoutParams = lp
        }
        (indicatorContainer.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            if (landscapePhone) {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 0f
                lp.topMargin = 0
                lp.leftMargin = dp(10)
                lp.gravity = Gravity.CENTER_VERTICAL
            } else {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 0f
                lp.topMargin = dp(10)
                lp.leftMargin = 0
                lp.gravity = Gravity.CENTER_HORIZONTAL
            }
            indicatorContainer.layoutParams = lp
        }
        indicatorContainer.orientation =
            if (landscapePhone) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        indicatorContainer.gravity = Gravity.CENTER
        indicatorContainer.background = roundedRectDrawable(dp(999).toFloat(), overlayCardColor())
        indicatorContainer.elevation = dp(4).toFloat()
        indicatorContainer.setPadding(
            if (landscapePhone) dp(6) else dp(8),
            if (landscapePhone) dp(10) else dp(5),
            if (landscapePhone) dp(6) else dp(8),
            if (landscapePhone) dp(10) else dp(5)
        )
        refreshMiniQuickCardIndicators()
        body.requestLayout()
    }

    private fun updateMiniQuickItemsEdgeFade(animate: Boolean) {
        val recycler = miniQuickItemsRecyclerView ?: return
        val leftFade = miniQuickItemsLeftFadeView ?: return
        val rightFade = miniQuickItemsRightFadeView ?: return
        val vertical =
            (recycler.layoutManager as? LinearLayoutManager)?.orientation == RecyclerView.VERTICAL
        val hasScrollableContent =
            (miniQuickItemsAdapter?.itemCount ?: 0) > 0 &&
                if (vertical) {
                    recycler.computeVerticalScrollRange() > recycler.height
                } else {
                    recycler.computeHorizontalScrollRange() > recycler.width
                }
        if (!hasScrollableContent) {
            applyMiniQuickFadeAlpha(leftFade, 0f, animate)
            applyMiniQuickFadeAlpha(rightFade, 0f, animate)
            return
        }
        applyMiniQuickFadeAlpha(
            leftFade,
            if (vertical) {
                if (recycler.canScrollVertically(-1)) 1f else 0f
            } else {
                if (recycler.canScrollHorizontally(-1)) 1f else 0f
            },
            animate
        )
        applyMiniQuickFadeAlpha(
            rightFade,
            if (vertical) {
                if (recycler.canScrollVertically(1)) 1f else 0f
            } else {
                if (recycler.canScrollHorizontally(1)) 1f else 0f
            },
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
        val vertical = isPhoneLandscapeUi()
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
                    if (vertical) {
                        if (index > 0) topMargin = dp(8)
                    } else if (index > 0) {
                        leftMargin = dp(8)
                    }
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
            var lastPrimary = 0f
            var dragged = false

            private fun requestDisallowIntercept(view: View, disallow: Boolean) {
                var parent = view.parent
                while (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(disallow)
                    parent = parent.parent
                }
            }

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val horizontalDrag = isPhoneLandscapeUi()
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastPrimary = if (horizontalDrag) event.rawX else event.rawY
                        dragged = false
                        requestDisallowIntercept(v, true)
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        requestDisallowIntercept(v, true)
                        val currentPrimary = if (horizontalDrag) event.rawX else event.rawY
                        val delta = currentPrimary - lastPrimary
                        if (abs(delta) >= threshold) {
                            shiftQuickSubtitleGroup(if (delta > 0f) 1 else -1)
                            lastPrimary = currentPrimary
                            dragged = true
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        requestDisallowIntercept(v, false)
                        if (event.actionMasked == MotionEvent.ACTION_UP && !dragged) {
                            v.performClick()
                        }
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
                type = QuickCardType.Text,
                title = obj.optString("title", ""),
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
                                put("type", QuickCardType.Text.wireValue)
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
        miniQuickCardPageHeights.clear()
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
        refreshMiniQuickCardLayoutMetrics()
        refreshMiniPreviewUi()
    }

    private fun miniQuickCardHeightKey(card: QuickCard?): Long = card?.id ?: Long.MIN_VALUE

    private fun miniQuickCardPageContentWidth(landscapePhone: Boolean): Int =
        min(
            if (landscapePhone) dp(440) else dp(244),
            max(
                if (landscapePhone) dp(260) else dp(196),
                overlayContentWidthPx(phoneMaxDp = 360, tabletMaxDp = 400) - if (landscapePhone) dp(120) else dp(76)
            )
        )

    private fun miniQuickCardPortraitPreviewMinHeight(): Int {
        val cardWidth = miniQuickCardPageContentWidth(landscapePhone = false).coerceAtLeast(dp(220))
        val cardHeight = (cardWidth / (9f / 16f)).roundToInt().coerceAtLeast(dp(124))
        return cardHeight + dp(20)
    }

    private fun miniSubtitlePreviewCardSize(): OverlayPreviewCardSize {
        val availableWidth = (displayWidth() - dp(32)).coerceAtLeast(dp(240))
        val availableHeight = (displayHeight() - dp(56)).coerceAtLeast(dp(220))
        return if (isLandscapeUi()) {
            val width =
                min(
                    availableWidth,
                    (displayWidth() * if (isTabletLandscapeUi()) 0.64f else 0.76f).roundToInt()
                ).coerceAtLeast(dp(320))
            val height =
                min(
                    availableHeight,
                    (displayHeight() * if (isTabletLandscapeUi()) 0.8f else 0.82f).roundToInt()
                ).coerceAtLeast(dp(220))
            OverlayPreviewCardSize(width = width, height = height)
        } else {
            val width =
                min(
                    availableWidth,
                    (displayWidth() * 0.88f).roundToInt()
                ).coerceAtLeast(dp(260))
            val height =
                min(
                    availableHeight,
                    (displayHeight() * 0.62f).roundToInt()
                ).coerceAtLeast(dp(280))
            OverlayPreviewCardSize(width = width, height = height)
        }
    }

    private fun measureMiniQuickCardPageHeight(contentWidth: Int, card: QuickCard?): Int {
        val previewContainer = miniQuickCardPreviewContainer ?: return miniQuickCardPortraitPreviewMinHeight()
        if (isPhoneLandscapeUi()) {
            return landscapeOverlayContentHeight() - previewContainer.paddingTop - previewContainer.paddingBottom
        }
        val probe = createMiniQuickCardPage(card)
        val childWidthSpec = View.MeasureSpec.makeMeasureSpec(contentWidth.coerceAtLeast(0), View.MeasureSpec.EXACTLY)
        val childHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        probe.measure(childWidthSpec, childHeightSpec)
        return probe.measuredHeight.coerceAtLeast(
            miniQuickCardPortraitPreviewMinHeight() - previewContainer.paddingTop - previewContainer.paddingBottom
        )
    }

    private fun createMiniQuickCardPage(card: QuickCard?): View {
        val landscapePhone = isPhoneLandscapeUi()
        val contentWidth = miniQuickCardPageContentWidth(landscapePhone)
        val verticalPadding = if (landscapePhone) 0 else dp(10)
        return FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            setPadding(0, verticalPadding, 0, verticalPadding)
            addView(
                buildOverlayQuickCardCard(
                    card = card,
                    landscape = landscapePhone,
                    renderStyle = OverlayQuickCardRenderStyle.Panel,
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
                        val width = when {
                            isPhoneLandscapeUi() ->
                                min(displayWidth() - dp(40), dp(560)).coerceAtLeast(dp(320))
                            isTabletLandscapeUi() ->
                                min(displayWidth() - dp(72), dp(640)).coerceAtLeast(dp(360))
                            else ->
                                min(
                                    dp(300),
                                    overlayContentWidthPx(phoneMaxDp = 380, tabletMaxDp = 420) - dp(20)
                                ).coerceAtLeast(dp(240))
                        }
                        buildOverlayQuickCardCard(
                            card = card,
                            landscape = isLandscapeUi(),
                            renderStyle = OverlayQuickCardRenderStyle.Preview,
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
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
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
        val previewCardSize = miniSubtitlePreviewCardSize()
        val previewClickListener = View.OnClickListener { onCardClick() }
        val previewLongClickListener = View.OnLongClickListener {
            onCardLongClick()
            true
        }
        val previewScrollView = ScrollView(this).apply {
            isFillViewport = true
            clipChildren = false
            clipToPadding = false
            setPadding(dp(16), dp(16), dp(16), dp(16))
            isClickable = true
            isLongClickable = true
            setOnClickListener(previewClickListener)
            setOnLongClickListener(previewLongClickListener)
        }
        val previewTextView = TextView(this).apply {
            setTextColor(overlayOnSurfaceColor())
            isClickable = true
            isLongClickable = true
            setOnClickListener(previewClickListener)
            setOnLongClickListener(previewLongClickListener)
        }
        applyOverlayQuickSubtitleTextAppearance(
            textView = previewTextView,
            text = quickSubtitleCurrentText.ifBlank { defaultQuickSubtitleText },
            maxFontSizeSp = (quickSubtitleFontSizeSp * 1.25f).coerceIn(36f, 140f),
            minFontSizeSp = 18f,
            maxLines = null,
            viewportView = previewScrollView
        )
        previewScrollView.addView(
            previewTextView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
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
                            setOnClickListener(previewClickListener)
                            setOnLongClickListener(previewLongClickListener)
                            addView(
                                previewScrollView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        },
                        FrameLayout.LayoutParams(
                            previewCardSize.width,
                            previewCardSize.height
                        )
                    )
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun createOverlayQuickCardLogoView(light: Boolean = overlayDarkTheme): ImageView =
        ImageView(this).apply {
            setImageResource(if (light) R.drawable.logo_white else R.drawable.logo_black)
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
        renderStyle: OverlayQuickCardRenderStyle,
        contentWidthPx: Int,
        interactive: Boolean,
        onCardClick: (() -> Unit)?,
        onCardLongClick: (() -> Unit)?
    ): View {
        val cardAspect = if (landscape) 16f / 9f else 9f / 16f
        val maxHeight =
            if (renderStyle == OverlayQuickCardRenderStyle.Panel && isPhoneLandscapeUi()) {
                landscapeOverlayContentHeight()
            } else {
                Int.MAX_VALUE
            }
        val cardWidth =
            if (maxHeight == Int.MAX_VALUE) {
                contentWidthPx
            } else {
                min(contentWidthPx, (maxHeight * cardAspect).roundToInt())
            }.coerceAtLeast(dp(220))
        val cardHeight = (cardWidth / cardAspect).roundToInt().coerceAtLeast(dp(124))

        return FrameLayout(this).apply {
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
            elevation = dp(8).toFloat()
            clipChildren = false
            clipToPadding = false
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
            addView(
                if (card == null) {
                    buildOverlayQuickCardPlaceholder()
                } else {
                    buildUnifiedOverlayQuickCardContent(
                        card = card,
                        landscape = landscape,
                        cardWidthPx = cardWidth,
                        cardHeightPx = cardHeight,
                        onOpenPageClick = {
                            val targetIndex = quickCards.indexOfFirst { it.id == card.id }
                            if (targetIndex >= 0) {
                                quickCardSelectedIndex = targetIndex
                                saveQuickCardSelectedIndex()
                            }
                            launchQuickCardPage()
                        }
                    )
                },
                FrameLayout.LayoutParams(cardWidth, cardHeight, Gravity.CENTER)
            )
        }
    }

    private fun buildOverlayQuickCardPlaceholder(): View =
        FrameLayout(this).apply {
            background = roundedRectDrawable(overlayRadiusDp, overlayCardColor())
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
        }

    private fun buildUnifiedOverlayQuickCardContent(
        card: QuickCard,
        landscape: Boolean,
        cardWidthPx: Int,
        cardHeightPx: Int,
        onOpenPageClick: () -> Unit
    ): View {
        val themeColor = parseQuickCardThemeColor(card.themeColor)
        val onThemeColor = quickCardOnColor(themeColor)
        val linkText = card.link.trim()
        val imagePath = card.overlayHeroImagePath(landscape)
        return FrameLayout(this).apply {
            background = roundedRectDrawable(overlayRadiusDp, themeColor)
            clipToRoundedOutline(overlayRadiusDp)
            clipChildren = true
            clipToPadding = true
            if (imagePath.isNotBlank()) {
                val imageView = ImageView(this@FloatingOverlayService).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    tag = imagePath
                }
                addView(
                    imageView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                addView(
                    View(this@FloatingOverlayService).apply {
                        background = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(ColorUtils.setAlphaComponent(Color.BLACK, 112), Color.TRANSPARENT)
                        )
                    },
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(92), Gravity.TOP)
                )
                addView(
                    View(this@FloatingOverlayService).apply {
                        background = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(Color.TRANSPARENT, ColorUtils.setAlphaComponent(Color.BLACK, 124))
                        )
                    },
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(86), Gravity.BOTTOM)
                )
                scope.launch {
                    val bitmap = withContext(Dispatchers.IO) { decodeBitmapFromPath(imagePath) }
                    if (imageView.tag == imagePath && bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } else {
                addOverlayQuickCardDecorText(this, card, onThemeColor, landscape, cardHeightPx)
            }

            val foreground = if (imagePath.isNotBlank()) Color.WHITE else onThemeColor
            addOverlayQuickCardHeader(this, card, foreground, onOpenPageClick)
            if (linkText.isNotBlank()) {
                addOverlayQuickCardQr(this, linkText, min(cardWidthPx, cardHeightPx))
            }
            addOverlayQuickCardFooter(this, card, foreground, linkText)
        }
    }

    private fun addOverlayQuickCardDecorText(
        container: FrameLayout,
        card: QuickCard,
        onThemeColor: Int,
        landscape: Boolean,
        cardHeightPx: Int
    ) {
        val watermarkText = card.title.trim()
        if (watermarkText.isEmpty()) return
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
                    leftMargin = dp(10)
                    rightMargin = dp(16)
                    bottomMargin = dp(4)
                }
            )
        } else {
            val maxWatermarkWidth = (cardHeightPx - dp(24)).coerceAtLeast(dp(120))
            val watermark = TextView(this).apply {
                setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 56))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 88f)
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
                ellipsize = null
                isSingleLine = true
                setHorizontallyScrolling(true)
                minWidth = maxWatermarkWidth
                includeFontPadding = false
                text = watermarkText
            }
            container.addView(
                watermark,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.END
                ).apply {
                    rightMargin = dp(10)
                }
            )
            watermark.post {
                watermark.pivotX = watermark.measuredWidth.toFloat()
                watermark.pivotY = 0f
                watermark.rotation = 90f
                watermark.translationY = watermark.measuredWidth.toFloat() + dp(10).toFloat()
            }
        }
    }

    private fun addOverlayQuickCardHeader(
        container: FrameLayout,
        card: QuickCard,
        foreground: Int,
        onOpenPageClick: () -> Unit
    ) {
        container.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                setPadding(dp(14), dp(10), dp(8), 0)
                addView(
                    LinearLayout(this@FloatingOverlayService).apply {
                        orientation = LinearLayout.VERTICAL
                        val titleText = card.title.trim()
                        val noteText = card.note.trim()
                        if (titleText.isNotEmpty()) {
                            addView(
                                TextView(this@FloatingOverlayService).apply {
                                    setTextColor(foreground)
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                                    typeface = Typeface.DEFAULT_BOLD
                                    maxLines = 1
                                    ellipsize = TextUtils.TruncateAt.END
                                    text = titleText
                                }
                            )
                        }
                        if (noteText.isNotEmpty()) {
                            addView(
                                TextView(this@FloatingOverlayService).apply {
                                    setTextColor(ColorUtils.setAlphaComponent(foreground, 230))
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                                    maxLines = 1
                                    ellipsize = TextUtils.TruncateAt.END
                                    text = noteText
                                }
                            )
                        }
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                )
                addView(
                    createOverlayQuickCardActionSymbol("open_in_new", foreground, onOpenPageClick).apply {
                        contentDescription = "打开主软件名片页"
                    },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                )
            },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        )
    }

    private fun addOverlayQuickCardQr(
        container: FrameLayout,
        linkText: String,
        baseSizePx: Int
    ) {
        val qrSize = (baseSizePx * 0.44f).roundToInt().coerceIn(dp(72), dp(150))
        val qrFrame = FrameLayout(this).apply {
            background = roundedRectDrawable(overlayRadiusDp, Color.WHITE)
            elevation = dp(4).toFloat()
            clipToRoundedOutline(overlayRadiusDp)
            clipChildren = true
            clipToPadding = true
        }
        val qrImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            tag = linkText
        }
        val qrPlaceholder = TextView(this).apply {
            setTextColor(ColorUtils.setAlphaComponent(Color.BLACK, 184))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            text = if (linkText.isBlank()) "未设置链接" else "生成二维码中..."
        }
        qrFrame.addView(
            qrImage,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                leftMargin = dp(8)
                topMargin = dp(8)
                rightMargin = dp(8)
                bottomMargin = dp(8)
            }
        )
        qrFrame.addView(
            qrPlaceholder,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        container.addView(qrFrame, FrameLayout.LayoutParams(qrSize, qrSize, Gravity.CENTER))
        if (linkText.isNotBlank()) {
            scope.launch {
                val qrBitmap = withContext(Dispatchers.Default) { QuickCardRenderCache.loadQr(linkText, dp(360)) }
                if (qrImage.tag == linkText) {
                    if (qrBitmap != null) {
                        qrImage.setImageBitmap(qrBitmap)
                        qrPlaceholder.visibility = View.GONE
                    } else {
                        qrPlaceholder.text = "未设置链接"
                    }
                }
            }
        }
    }

    private fun addOverlayQuickCardFooter(
        container: FrameLayout,
        card: QuickCard,
        foreground: Int,
        linkText: String
    ) {
        if (linkText.isBlank()) {
            container.addView(
                createOverlayQuickCardLogoView(light = foreground == Color.WHITE),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.END
                ).apply {
                    rightMargin = dp(14)
                    bottomMargin = dp(12)
                }
            )
            return
        }
        container.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.START
                setPadding(dp(12), 0, dp(10), dp(8))
                addView(
                    LinearLayout(this@FloatingOverlayService).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(createOverlayQuickCardActionSymbol("open_in_new", foreground) {
                            openOverlayQuickCardLink(linkText)
                        })
                        addView(spaceView(dp(2), 1))
                        addView(createOverlayQuickCardActionSymbol("share", foreground) {
                            shareOverlayPlainText(
                                buildString {
                                    append(card.title.ifBlank { "名片" })
                                    if (card.note.isNotBlank()) append("\n${card.note}")
                                    append("\n$linkText")
                                },
                                "分享名片"
                            )
                        })
                    },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                )
                addView(
                    TextView(this@FloatingOverlayService).apply {
                        setTextColor(foreground)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                        text = linkText
                    },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                )
            },
            FrameLayout.LayoutParams(
                (container.width.takeIf { it > 0 } ?: dp(360)) * 3 / 5,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.START
            )
        )
        container.addView(
            createOverlayQuickCardLogoView(light = foreground == Color.WHITE),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply {
                rightMargin = dp(14)
                bottomMargin = dp(12)
            }
        )
    }

    private fun buildLandscapePanelQuickCardContent(card: QuickCard, contentWidthPx: Int): View {
        val themeColor = parseQuickCardThemeColor(card.themeColor)
        val onThemeColor = quickCardOnColor(themeColor)
        val innerHeight = landscapeOverlayContentHeight() - dp(20)
        val footerHeight = dp(58)
        val gap = dp(8)
        val heroHeight = max(dp(104), innerHeight - footerHeight - gap)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            minimumHeight = innerHeight
            val hero = FrameLayout(this@FloatingOverlayService).apply {
                background = roundedRectDrawable(overlayRadiusDp, themeColor)
                clipToRoundedOutline(overlayRadiusDp)
                clipChildren = true
                clipToPadding = true
            }
            populateOverlayQuickCardHero(
                container = hero,
                card = card,
                onThemeColor = onThemeColor,
                landscape = true
            )
            addView(hero, LinearLayout.LayoutParams(contentWidthPx, heroHeight))
            addView(
                LinearLayout(this@FloatingOverlayService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        LinearLayout(this@FloatingOverlayService).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER_VERTICAL
                            if (card.title.isNotBlank()) {
                                addView(
                                    TextView(this@FloatingOverlayService).apply {
                                        setTextColor(overlayOnSurfaceColor())
                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                                        typeface = Typeface.DEFAULT_BOLD
                                        maxLines = 1
                                        ellipsize = TextUtils.TruncateAt.END
                                        text = card.title.trim()
                                    }
                                )
                            }
                            if (card.note.isNotBlank()) {
                                addView(
                                    TextView(this@FloatingOverlayService).apply {
                                        setTextColor(overlayOnSurfaceVariantColor())
                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                        maxLines = 2
                                        ellipsize = TextUtils.TruncateAt.END
                                        text = card.note.trim()
                                    }
                                )
                            }
                        },
                        LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    )
                    addView(spaceView(dp(8), 1))
                    addView(
                        createOverlayQuickCardLogoView(),
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.BOTTOM
                        }
                    )
                },
                LinearLayout.LayoutParams(contentWidthPx, footerHeight).apply {
                    topMargin = gap
                }
            )
        }
    }

    private fun buildLandscapePreviewQuickCardContent(card: QuickCard, contentWidthPx: Int): View {
        val themeColor = parseQuickCardThemeColor(card.themeColor)
        val onThemeColor = quickCardOnColor(themeColor)
        val heroWidth = min(
            if (contentWidthPx >= dp(500)) dp(320) else dp(250),
            max(dp(176), (contentWidthPx * 0.58f).roundToInt())
        )
        val heroHeight = (heroWidth / 1.67f).roundToInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            val hero = FrameLayout(this@FloatingOverlayService).apply {
                background = roundedRectDrawable(overlayRadiusDp, themeColor)
                clipToRoundedOutline(overlayRadiusDp)
                clipChildren = true
                clipToPadding = true
            }
            populateOverlayQuickCardHero(
                container = hero,
                card = card,
                onThemeColor = onThemeColor,
                landscape = true
            )
            addView(hero, LinearLayout.LayoutParams(heroWidth, heroHeight))
            addView(spaceView(dp(10), 1))
            addView(
                LinearLayout(this@FloatingOverlayService).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dp(2), 0, dp(2))
                    if (card.title.isNotBlank()) {
                        addView(
                            TextView(this@FloatingOverlayService).apply {
                                setTextColor(overlayOnSurfaceColor())
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                                typeface = Typeface.DEFAULT_BOLD
                                maxLines = 1
                                ellipsize = TextUtils.TruncateAt.END
                                text = card.title.trim()
                            }
                        )
                    }
                    if (card.note.isNotBlank()) {
                        addView(
                            TextView(this@FloatingOverlayService).apply {
                                setTextColor(overlayOnSurfaceVariantColor())
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                                maxLines = 3
                                ellipsize = TextUtils.TruncateAt.END
                                text = card.note.trim()
                            }
                        )
                    }
                    addView(
                        weightedSpacer(),
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
                            1f
                        )
                    )
                    addView(
                        LinearLayout(this@FloatingOverlayService).apply {
                            gravity = Gravity.END or Gravity.BOTTOM
                            addView(createOverlayQuickCardLogoView())
                        },
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    heroHeight,
                    1f
                )
            )
        }
    }

    private fun populateOverlayQuickCardHero(
        container: FrameLayout,
        card: QuickCard,
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
                val watermarkText = card.title.trim()
                val noteText = card.note.trim()
                if (watermarkText.isNotEmpty()) {
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
                    } else {
                        val watermark = TextView(this).apply {
                            setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 56))
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 96f)
                            typeface = Typeface.DEFAULT_BOLD
                            maxLines = 1
                            ellipsize = null
                            isSingleLine = true
                            setHorizontallyScrolling(true)
                            minWidth = container.height.takeIf { it > 0 } ?: dp(220)
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
                    }
                }
                if (watermarkText.isNotEmpty() || noteText.isNotEmpty()) {
                    container.addView(
                        LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(12), dp(16), dp(12), dp(12))
                            if (watermarkText.isNotEmpty()) {
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
                            }
                            if (noteText.isNotEmpty()) {
                                addView(
                                    TextView(this@FloatingOverlayService).apply {
                                        setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 230))
                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                        maxLines = 2
                                        ellipsize = TextUtils.TruncateAt.END
                                        text = noteText
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
                                        if (qrImageView.tag == card.link) {
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
                val titleText = card.title.trim()
                val noteText = card.note.trim()
                if (titleText.isNotEmpty()) {
                    container.addView(
                        TextView(this).apply {
                            setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 56))
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 54f)
                            typeface = Typeface.DEFAULT_BOLD
                            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                            text = titleText
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
                }
                if (titleText.isNotEmpty() || noteText.isNotEmpty()) {
                    container.addView(
                        LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(14), dp(14), dp(14), dp(14))
                            if (titleText.isNotEmpty()) {
                                addView(
                                    TextView(this@FloatingOverlayService).apply {
                                        setTextColor(onThemeColor)
                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                                        typeface = Typeface.DEFAULT_BOLD
                                        maxLines = 1
                                        ellipsize = TextUtils.TruncateAt.END
                                        text = titleText
                                    }
                                )
                            }
                            if (noteText.isNotEmpty()) {
                                addView(
                                    TextView(this@FloatingOverlayService).apply {
                                        setTextColor(ColorUtils.setAlphaComponent(onThemeColor, 230))
                                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                        maxLines = 2
                                        ellipsize = TextUtils.TruncateAt.END
                                        text = noteText
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
        OverlayLauncherTile("builtin_soundboard", "音效板", "library_music"),
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
            layoutTransition = if (panelEditMode) overlayLayoutTransition() else null
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
                        layoutTransition = if (panelEditMode) overlayLayoutTransition() else null
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
        performOverlayKeyHaptic(panelPager ?: panelRoot)
        panelPageIndex = targetIndex
        refreshPanelIndicators()
        scrollPanelToPage(targetIndex, animate = true)
    }

    private fun handlePanelLauncherTileClick(tile: OverlayLauncherTile) {
        if (panelEditMode && !tile.isAddButton) return
        performOverlayKeyHaptic(panelRoot)
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
            tile.key == "builtin_soundboard" -> {
                hidePanel()
                launchAppPage(OverlayBridge.TARGET_OPEN_SOUNDBOARD)
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
            "builtin_soundboard" -> {
                hidePanel()
                launchAppPage(OverlayBridge.TARGET_OPEN_SOUNDBOARD)
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
                    "音效板" -> {
                        hidePanel()
                        launchAppPage(OverlayBridge.TARGET_OPEN_SOUNDBOARD)
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
        val menuTargets = buildExternalShortcutMenuTargets(shortcut)
        var nextId = 1
        if (menuTargets.isNotEmpty()) {
            menuTargets.forEach { target ->
                menu.menu.add(0, nextId++, 0, target.title)
            }
            menu.menu.add(0, Int.MAX_VALUE - 1, 1, "打开应用")
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
            val target = menuTargets.getOrNull(index)
            if (target != null) {
                hidePanel()
                when {
                    target.launcherShortcut != null -> launchExternalLauncherShortcut(shortcut, target.launcherShortcut)
                    target.manifestShortcut != null -> launchExternalManifestShortcut(shortcut, target.manifestShortcut)
                    target.shortcutId.isNotBlank() -> launchExternalShortcutById(shortcut, target)
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

    private fun buildExternalShortcutMenuTargets(shortcut: OverlayAppShortcut): List<ExternalShortcutMenuTarget> {
        val targets = mutableListOf<ExternalShortcutMenuTarget>()
        val seen = linkedSetOf<String>()
        if (
            shortcut.packageName == QqScannerSupport.QQ_PACKAGE_NAME &&
            seen.add(externalShortcutDedupKey(shortcut.packageName, qqAccessibilityScannerShortcutId))
        ) {
            targets += ExternalShortcutMenuTarget(
                title = "扫一扫（无障碍）",
                shortcutId = qqAccessibilityScannerShortcutId
            )
        }
        val queriedShortcuts = queryLauncherShortcuts(shortcut)
        queriedShortcuts.forEach { info ->
            val title = resolveShortcutTitle(info.shortLabel?.toString(), info.longLabel?.toString(), info.id)
            if (
                title.isNotBlank() &&
                shouldIncludeExternalShortcut(shortcut.packageName, title, info.id) &&
                seen.add(externalShortcutDedupKey(shortcut.packageName, info.id))
            ) {
                targets += ExternalShortcutMenuTarget(
                    title = title,
                    launcherShortcut = info,
                    hardcodedShortcut = findAllowedHardcodedShortcut(shortcut.packageName, info.id)
                )
            }
        }
        if (targets.isEmpty()) {
            queryManifestShortcuts(shortcut).forEach { spec ->
                if (
                    spec.title.isNotBlank() &&
                    shouldIncludeExternalShortcut(shortcut.packageName, spec.title, spec.id) &&
                    seen.add(externalShortcutDedupKey(shortcut.packageName, spec.id))
                ) {
                    targets += ExternalShortcutMenuTarget(
                        title = spec.title,
                        manifestShortcut = spec,
                        hardcodedShortcut = findAllowedHardcodedShortcut(shortcut.packageName, spec.id)
                    )
                }
            }
        }
        hardcodedLauncherShortcuts[shortcut.packageName].orEmpty()
            .filter { spec -> shouldUseHardcodedShortcutSupplement(shortcut.packageName, spec.id) }
            .forEach { spec ->
                if (
                    spec.title.isNotBlank() &&
                    shouldIncludeExternalShortcut(shortcut.packageName, spec.title, spec.id) &&
                    seen.add(externalShortcutDedupKey(shortcut.packageName, spec.id))
                ) {
                    targets += ExternalShortcutMenuTarget(
                        title = spec.title,
                        shortcutId = spec.id,
                        hardcodedShortcut = spec
                    )
                }
            }
        return targets
    }

    private fun shouldUseHardcodedShortcutSupplement(packageName: String, shortcutId: String): Boolean {
        return settings.floatingOverlayHardcodedShortcutSupplement ||
            isAlwaysRetainedHardcodedShortcut(packageName, shortcutId)
    }

    private fun findAllowedHardcodedShortcut(
        packageName: String,
        shortcutId: String
    ): HardcodedShortcutSpec? {
        if (!shouldUseHardcodedShortcutSupplement(packageName, shortcutId)) return null
        return hardcodedLauncherShortcuts[packageName].orEmpty().firstOrNull { it.id == shortcutId }
    }

    private fun isAlwaysRetainedHardcodedShortcut(packageName: String, shortcutId: String): Boolean {
        return (packageName == "com.tencent.mm" && shortcutId == "launch_type_scan_qrcode") ||
            (packageName == AlipayScannerSupport.ALIPAY_PACKAGE_NAME && shortcutId == "1001")
    }

    private fun externalShortcutDedupKey(packageName: String, shortcutId: String): String =
        "$packageName:$shortcutId"

    private fun resolveShortcutTitle(shortLabel: String?, longLabel: String?, fallbackId: String): String {
        return when {
            !shortLabel.isNullOrBlank() -> shortLabel
            !longLabel.isNullOrBlank() -> longLabel
            else -> fallbackId
        }
    }

    private fun shouldIncludeExternalShortcut(packageName: String, title: String, shortcutId: String): Boolean {
        val haystack = "$packageName|$title|$shortcutId".lowercase(Locale.getDefault())
        return blockedShortcutKeywords.none { keyword -> haystack.contains(keyword.lowercase(Locale.getDefault())) }
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

    private fun launchExternalShortcutById(
        shortcut: OverlayAppShortcut,
        target: ExternalShortcutMenuTarget
    ) {
        val shortcutId = target.shortcutId
        if (shortcutId == qqAccessibilityScannerShortcutId) {
            if (!VolumeHotkeyAccessibilityService.isEnabled(this)) {
                runCatching {
                    startActivity(OverlayBridge.buildOpenAccessibilityGuideIntent(this))
                }.onFailure {
                    AppLogger.e("FloatingOverlayService.openAccessibilityGuide failed", it)
                }
                return
            }
            if (VolumeHotkeyAccessibilityService.requestOpenQqScanner(this)) {
                Toast.makeText(this, "正在打开QQ扫一扫", Toast.LENGTH_SHORT).show()
            } else if (QqScannerSupport.launchQq(this)) {
                Toast.makeText(this, "无障碍服务未连接，已打开QQ", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "打开QQ失败，请手动打开QQ扫一扫", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            if (!launchExternalShortcutFallback(shortcut, target)) {
                launchExternalShortcut(shortcut)
            }
            return
        }
        val launcherApps = getSystemService(LauncherApps::class.java)
        if (launcherApps == null) {
            if (!launchExternalShortcutFallback(shortcut, target)) {
                launchExternalShortcut(shortcut)
            }
            return
        }
        runCatching {
            launcherApps.startShortcut(
                shortcut.packageName,
                shortcutId,
                null,
                null,
                Process.myUserHandle()
            )
        }.onFailure {
            AppLogger.e("FloatingOverlayService.launchExternalShortcutById failed", it)
            if (!launchExternalShortcutFallback(shortcut, target)) {
                launchExternalShortcut(shortcut)
            }
        }
    }

    private fun launchExternalShortcutFallback(
        shortcut: OverlayAppShortcut,
        target: ExternalShortcutMenuTarget
    ): Boolean {
        target.manifestShortcut?.takeIf { it.intents.isNotEmpty() }?.let { spec ->
            launchExternalManifestShortcut(shortcut, spec)
            return true
        }
        queryManifestShortcuts(shortcut)
            .firstOrNull { it.id == target.shortcutId && it.intents.isNotEmpty() }
            ?.let { spec ->
                launchExternalManifestShortcut(shortcut, spec)
                return true
            }
        target.hardcodedShortcut?.let { spec ->
            if (launchHardcodedExternalShortcut(shortcut, spec)) {
                return true
            }
        }
        return false
    }

    private fun launchHardcodedExternalShortcut(
        shortcut: OverlayAppShortcut,
        spec: HardcodedShortcutSpec
    ): Boolean {
        if (
            spec.dataUri == null &&
            spec.targetClass == null &&
            spec.booleanExtras.isEmpty() &&
            spec.stringExtras.isEmpty()
        ) {
            return false
        }
        val explicitIntent =
            buildHardcodedExternalIntent(
                packageName = shortcut.packageName,
                spec = spec,
                useLaunchIntent = false
            )
        if (explicitIntent != null) {
            val launched =
                runCatching {
                    startActivity(explicitIntent)
                    true
                }.onFailure {
                    AppLogger.e("FloatingOverlayService.launchHardcodedExternalShortcut explicit failed", it)
                }.getOrDefault(false)
            if (launched) return true
        }
        val launchIntent =
            buildHardcodedExternalIntent(
                packageName = shortcut.packageName,
                spec = spec,
                useLaunchIntent = true
            )
        return runCatching {
            if (launchIntent != null) {
                startActivity(launchIntent)
                true
            } else {
                false
            }
        }.onFailure {
            AppLogger.e("FloatingOverlayService.launchHardcodedExternalShortcut launchIntent failed", it)
        }.getOrDefault(false)
    }

    private fun buildHardcodedExternalIntent(
        packageName: String,
        spec: HardcodedShortcutSpec,
        useLaunchIntent: Boolean
    ): Intent? {
        val rawIntent: Intent? =
            if (useLaunchIntent) {
                packageManager.getLaunchIntentForPackage(packageName)
            } else if (spec.dataUri != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse(spec.dataUri)).apply {
                    setPackage(packageName)
                }
            } else {
                spec.targetClass?.let { targetClass ->
                    Intent().apply { setClassName(packageName, targetClass) }
                }
            }
        val intent = rawIntent ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        spec.booleanExtras.forEach { (key, value) -> intent.putExtra(key, value) }
        spec.stringExtras.forEach { (key, value) -> intent.putExtra(key, value) }
        return intent
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
            refs.ttsIcon.text = if (settings.ttsDisabled) "toggle_on" else "toggle_off"
            refs.ttsIcon.setTextColor(if (settings.ttsDisabled) overlayPrimaryColor() else overlayOnSurfaceVariantColor())
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
        val ttsIcon = symbolTextView("toggle_off", 28f, overlayOnSurfaceVariantColor())
        val ttsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = selectableDrawable()
            }
            setPadding(dp(2), dp(2), dp(2), dp(2))
            addView(symbolTextView("mic_off", 18f, overlayOnSurfaceColor()))
            addView(spaceView(dp(8), 1))
            addView(
                TextView(this@FloatingOverlayService).apply {
                    setTextColor(overlayOnSurfaceColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    text = "禁用TTS"
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(ttsIcon)
            setOnClickListener {
                scope.launch(Dispatchers.IO) {
                    UserPrefs.setTtsDisabled(this@FloatingOverlayService, !settings.ttsDisabled)
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
            addView(ttsRow)
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
            ttsIcon = ttsIcon,
            volumeLabel = volumeLabel,
            volumeSeekBar = volumeSeekBar
        )
    }

    private fun preferredInputTypeOptions(): List<Pair<Int, String>> = listOf(
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.INPUT_AUTO to "自动",
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.INPUT_BUILTIN_MIC to "内置麦克风/话筒",
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.INPUT_USB to "USB 麦克风",
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.INPUT_BLUETOOTH to "蓝牙麦克风",
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.INPUT_WIRED to "有线麦克风"
    )

    private fun preferredOutputTypeOptions(): List<Pair<Int, String>> = listOf(
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.OUTPUT_AUTO to "自动",
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.OUTPUT_SPEAKER to "扬声器",
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.OUTPUT_EARPIECE to "听筒",
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.OUTPUT_BLUETOOTH to "蓝牙音频",
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.OUTPUT_USB to "USB 音频",
        com.lhtstudio.kigtts.app.audio.AudioRoutePreference.OUTPUT_WIRED to "有线耳机/线路"
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
        if (pager.currentItem != targetIndex) {
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
        if (isPhoneLandscapeUi()) {
            val topBoundary = leftActionButton?.let { actionView ->
                val location = IntArray(2)
                actionView.getLocationOnScreen(location)
                location[1] + (actionView.height / 2f)
            } ?: (overlayTop + overlayHeight * 0.3f)
            val bottomBoundary = rightActionButton?.let { actionView ->
                val location = IntArray(2)
                actionView.getLocationOnScreen(location)
                location[1] + (actionView.height / 2f)
            } ?: (overlayBottom - overlayHeight * 0.25f)

            if (rawY <= topBoundary || rawY <= overlayTop) return OverlayReleaseAction.SendToInput
            if (rawY >= bottomBoundary || rawY >= overlayBottom) return OverlayReleaseAction.Cancel
            return OverlayReleaseAction.SendToSubtitle
        }

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
        val extraWidth = if (isPhoneLandscapeUi()) dp(40) else 0
        val extraHeight = if (isPhoneLandscapeUi()) 0 else dp(40)
        params.width = targetWidth + extraWidth
        params.height = targetHeight + extraHeight
        params.x = layoutParams.leftMargin
        params.y = layoutParams.topMargin
        runCatching { windowManager.updateViewLayout(overlay, params) }
        overlay.post { layoutConfirmOverlayContents() }
    }

    private fun layoutConfirmOverlayContents() {
        val overlay = confirmOverlay ?: return
        confirmParams ?: return
        val clip = confirmClipContainer ?: return
        val card = confirmTextCardView ?: return
        val left = leftActionButton ?: return
        val right = rightActionButton ?: return
        val fab = activeConfirmFab() ?: return
        val content = activeOverlayContent() as? ViewGroup ?: return
        if (overlay.width <= 0 || overlay.height <= 0) return

        val fabWidth = fab.width.takeIf { it > 0 } ?: dp(74)
        val fabHeight = fab.height.takeIf { it > 0 } ?: dp(74)
        val fabBounds = Rect(0, 0, fabWidth, fabHeight)
        content.offsetDescendantRectToMyCoords(fab, fabBounds)
        val fabCenterX = fabBounds.exactCenterX()
        val fabCenterY = fabBounds.exactCenterY()
        val landscapePhone = isPhoneLandscapeUi()
        val contentWidth = content.width.takeIf { it > 0 }
            ?: (content.layoutParams?.width ?: overlay.width)
        val landscapeHorizontalBias = if (landscapePhone) fabWidth / 2f else 0f
        val portraitVerticalBias = if (landscapePhone) 0f else fabHeight / 2f
        val landscapeCardHorizontalNudge = if (landscapePhone) dp(11).toFloat() else 0f
        val landscapeActionHorizontalNudge = if (landscapePhone) dp(10).toFloat() else 0f
        val portraitActionVerticalNudge = if (landscapePhone) 0f else dp(10).toFloat()
        val actionSize = dp(64)
        val contentPadding = dp(12)
        val sideGap = dp(16)
        val sideCenterOffset = (fabWidth / 2f) + (actionSize / 2f) + sideGap
        val verticalCenterOffset = (fabHeight / 2f) + (actionSize / 2f) + sideGap
        val minActionLeft = contentPadding.toFloat()
        val maxActionLeft = max(
            minActionLeft,
            (overlay.width - actionSize - contentPadding).toFloat()
        )
        val minActionTop = contentPadding.toFloat()
        val maxActionTop = max(
            minActionTop,
            (overlay.height - actionSize - contentPadding).toFloat()
        )

        left.measure(
            View.MeasureSpec.makeMeasureSpec(actionSize, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(actionSize, View.MeasureSpec.EXACTLY)
        )
        right.measure(
            View.MeasureSpec.makeMeasureSpec(actionSize, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(actionSize, View.MeasureSpec.EXACTLY)
        )

        if (landscapePhone) {
            val actionLeft = (fabCenterX - actionSize / 2f + landscapeHorizontalBias + landscapeActionHorizontalNudge)
                .coerceIn(minActionLeft, maxActionLeft)
            left.x = actionLeft
            right.x = actionLeft
            left.y = (fabCenterY - verticalCenterOffset - actionSize / 2f)
                .coerceIn(minActionTop, maxActionTop)
            right.y = (fabCenterY + verticalCenterOffset - actionSize / 2f)
                .coerceIn(minActionTop, maxActionTop)
        } else {
            val actionCenterY = fabCenterY.coerceIn(
                (contentPadding + actionSize / 2).toFloat(),
                max((contentPadding + actionSize / 2).toFloat(), (overlay.height - contentPadding - actionSize / 2).toFloat())
            )
            val actionTop = (actionCenterY - actionSize / 2f + portraitVerticalBias + portraitActionVerticalNudge)
                .coerceIn(minActionTop, maxActionTop)
            left.x = (fabCenterX - sideCenterOffset - actionSize / 2f)
                .coerceIn(minActionLeft, maxActionLeft)
            left.y = actionTop
            right.x = (fabCenterX + sideCenterOffset - actionSize / 2f)
                .coerceIn(minActionLeft, maxActionLeft)
            right.y = actionTop
        }

        val railReserve = if (landscapePhone) dp(92) + dp(12) else 0
        val cardWidth = (contentWidth - contentPadding * 2 - railReserve).coerceAtLeast(1)
        (card.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
            if (lp.width != cardWidth || lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                lp.width = cardWidth
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                card.layoutParams = lp
            }
        }
        card.measure(
            View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val cardHeight = card.measuredHeight
        val minCardTop = contentPadding.toFloat()
        val maxCardTop = max(
            minCardTop,
            (overlay.height - cardHeight - contentPadding).toFloat()
        )
        val cardLeft = if (landscapePhone) {
            (contentPadding + landscapeHorizontalBias + landscapeCardHorizontalNudge).coerceAtMost(
                max(contentPadding.toFloat(), (overlay.width - cardWidth - contentPadding).toFloat())
            )
        } else {
            ((overlay.width - cardWidth) / 2f).coerceAtLeast(contentPadding.toFloat())
        }
        val cardTop = if (landscapePhone) {
            (fabCenterY - cardHeight / 2f).coerceIn(minCardTop, maxCardTop)
        } else {
            val actionTop = left.y
            (actionTop - cardHeight - dp(14)).coerceIn(minCardTop, maxCardTop)
        }
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
        cancelFabSnapAnimation()
        if (startX == targetX && startY == targetY) {
            params.x = targetX
            params.y = targetY
            windowManager.updateViewLayout(root, params)
            refreshFabIdleDockState()
            return
        }
        var wasCancelled = false
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
                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        wasCancelled = true
                    }

                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (fabSnapAnimator === animation) {
                            fabSnapAnimator = null
                        }
                        if (wasCancelled) return
                        refreshFabIdleDockState()
                    }
                }
            )
            start()
        }
    }

    private fun clampFabToScreen(maxXOverride: Int? = null) {
        val params = fabParams ?: return
        val maxX = maxXOverride ?: fabMaxX(displayWidth())
        val maxY = fabMaxY(displayHeight())
        params.x = params.x.coerceIn(0, maxX)
        params.y = params.y.coerceIn(fabMinY(), maxY)
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun circleActionButton(
        iconView: TextView,
        sizeDp: Int = 72,
        backgroundColor: Int = overlayNeutralCircleColor()
    ): FrameLayout {
        return FrameLayout(this).apply {
            background = circleDrawable(backgroundColor)
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

    private fun isOverlayDarkTheme(): Boolean {
        val systemDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return UserPrefs.resolveThemeMode(settings.overlayThemeMode, systemDark)
    }

    private fun overlayPrimaryColor(): Int = 0xFF038387.toInt()

    private fun performOverlayKeyHaptic(anchor: View? = null) {
        if (!settings.hapticFeedbackEnabled) return
        (anchor ?: fabButton ?: panelActionFab ?: miniActionFab ?: panelRoot ?: miniRoot ?: fabRoot)
            ?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

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

    private fun View.clipToRoundedOutline(radiusDp: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val radiusPx = dp(radiusDp).toFloat()
        outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                }
            }
        clipToOutline = true
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
            setSymbolTextSize(sp)
            typeface = iconTypeface
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fontFeatureSettings = "liga"
            }
            includeFontPadding = false
        }

    private fun TextView.setSymbolTextSize(sp: Float) {
        if (settings.fontScaleBlockMode == UserPrefs.FONT_SCALE_BLOCK_NONE) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        } else {
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                sp,
                super.getResources().displayMetrics
            )
            setTextSize(TypedValue.COMPLEX_UNIT_PX, px)
        }
    }

    private fun launcherFabIconView(): ImageView =
        ImageView(this).apply {
            setImageResource(R.drawable.ic_overlay_fab_foreground)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER)
        }

    private fun spaceView(width: Int, height: Int): View =
        View(this).apply { layoutParams = ViewGroup.LayoutParams(width, height) }

    private fun usesAppRealtimeBridge(): Boolean =
        realtimeHost != null || RealtimeRuntimeBridge.currentAppDelegate() != null

    private fun ensureRealtimeHostBound() {
        if (realtimeHostBound) return
        RealtimeHostService.ensureStarted(this)
        val bound = runCatching {
            bindService(
                Intent(this, RealtimeHostService::class.java),
                realtimeHostConnection,
                Context.BIND_AUTO_CREATE
            )
        }.getOrDefault(false)
        realtimeHostBound = bound
    }

    private fun runWithRealtimeHost(
        pendingStatus: String?,
        action: (RealtimeHostService) -> Unit
    ): Boolean {
        val host = realtimeHost
        if (host != null) {
            action(host)
            return true
        }
        synchronized(pendingRealtimeHostActions) {
            pendingRealtimeHostActions += action
        }
        ensureRealtimeHostBound()
        if (pendingStatus != null) {
            overlayHintText = pendingStatus
            updateFabUi()
        }
        return false
    }

    private fun flushPendingRealtimeHostActions() {
        val host = realtimeHost ?: return
        val pending = synchronized(pendingRealtimeHostActions) {
            pendingRealtimeHostActions.toList().also { pendingRealtimeHostActions.clear() }
        }
        pending.forEach { action ->
            runCatching { action(host) }
                .onFailure { AppLogger.e("FloatingOverlayService pending host action failed", it) }
        }
    }

    private fun effectiveRunningState(): Boolean =
        if (realtimeHost != null) {
            realtimeHostState.running
        } else if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().running
        } else {
            false
        }

    private fun effectiveLatestRecognizedText(): String =
        if (realtimeHost != null) {
            realtimeHostState.recognized.firstOrNull()?.text.orEmpty().ifBlank { overlayHintText }
        } else if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().latestRecognizedText
        } else {
            overlayHintText
        }

    private fun effectivePttStreamingText(): String =
        if (realtimeHost != null) {
            realtimeHostState.pushToTalkStreamingText
        } else if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().pushToTalkStreamingText
        } else {
            ""
        }

    private fun effectivePttPressedState(): Boolean =
        if (realtimeHost != null) {
            pttPressed || realtimeHostState.pushToTalkPressed
        } else if (usesAppRealtimeBridge()) {
            pttPressed || RealtimeRuntimeBridge.currentSnapshot().pushToTalkPressed
        } else {
            pttPressed
        }

    private fun effectiveInputLevel(): Float =
        if (realtimeHost != null) {
            realtimeHostState.inputLevel.coerceIn(0f, 1f)
        } else if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().inputLevel.coerceIn(0f, 1f)
        } else {
            0f
        }

    private fun effectivePlaybackProgress(): Float =
        if (realtimeHost != null) {
            realtimeHostState.playbackProgress.coerceIn(0f, 1f)
        } else if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().playbackProgress.coerceIn(0f, 1f)
        } else {
            0f
        }

    private fun effectiveInputDeviceLabel(): String =
        if (realtimeHost != null) {
            realtimeHostState.inputDeviceLabel.ifBlank {
                preferredInputTypeLabel(settings.preferredInputType)
            }
        } else if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().inputDeviceLabel.ifBlank {
                preferredInputTypeLabel(settings.preferredInputType)
            }
        } else {
            preferredInputTypeLabel(settings.preferredInputType)
        }

    private fun effectiveOutputDeviceLabel(): String =
        if (realtimeHost != null) {
            realtimeHostState.outputDeviceLabel.ifBlank {
                preferredOutputTypeLabel(settings.preferredOutputType)
            }
        } else if (usesAppRealtimeBridge()) {
            RealtimeRuntimeBridge.currentSnapshot().outputDeviceLabel.ifBlank {
                preferredOutputTypeLabel(settings.preferredOutputType)
            }
        } else {
            preferredOutputTypeLabel(settings.preferredOutputType)
        }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value).roundToInt()
    private fun dp(value: Float): Int = (resources.displayMetrics.density * value).roundToInt()

    private fun displayBounds(): Rect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Rect(windowManager.maximumWindowMetrics.bounds)
        }
        val display = getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
        if (display != null) {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)
            return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
        return Rect(0, 0, resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
    }

    private fun displayWidth(): Int = displayBounds().width()
    private fun displayHeight(): Int = displayBounds().height()

    companion object {
        private const val CHANNEL_ID = "floating_overlay"
        private const val NOTIFICATION_ID = 3204
        private const val OWNER_TAG = "overlay"
        private const val FAB_SIZE_DP = 56
        private const val ACTION_STOP = "com.lhtstudio.kigtts.app.action.OVERLAY_STOP"
        private const val ACTION_REFRESH = "com.lhtstudio.kigtts.app.action.OVERLAY_REFRESH"
        private const val ACTION_OPEN_PANEL = "com.lhtstudio.kigtts.app.action.OVERLAY_OPEN_PANEL"
        private const val ACTION_OPEN_MINI_SUBTITLE = "com.lhtstudio.kigtts.app.action.OVERLAY_OPEN_MINI_SUBTITLE"
        private const val ACTION_OPEN_MINI_QUICK_CARD = "com.lhtstudio.kigtts.app.action.OVERLAY_OPEN_MINI_QUICK_CARD"

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

        fun openPanel(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_OPEN_PANEL
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun openMiniQuickSubtitle(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_OPEN_MINI_SUBTITLE
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun openMiniQuickCard(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_OPEN_MINI_QUICK_CARD
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }
}
