package com.lhtstudio.kigtts.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.util.AppLogger
import com.lhtstudio.kigtts.app.util.QqScannerSupport
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActionExecutor
import com.lhtstudio.kigtts.app.util.VolumeHotkeySequenceDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VolumeHotkeyAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val sequenceDetector = VolumeHotkeySequenceDetector()
    private val qqScanRunnable = Runnable { scanQqWindow() }
    private var settings = UserPrefs.AppSettings()
    private var settingsJob: kotlinx.coroutines.Job? = null
    private var qqAutomationState = QqAutomationState.Idle
    private var qqAutomationDeadline = 0L
    private var lastQqClickUptime = 0L
    private var lastQqClickSignature = ""
    private var qqBackActionCount = 0
    private var lastQqBackUptime = 0L
    private var lastForegroundPackage = ""
    private var lastForegroundClass = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppLogger.i("VolumeHotkeyAccessibilityService.onServiceConnected")
        serviceInfo = serviceInfo.apply {
            flags = flags or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        VolumeHotkeyAccessibilityGuideService.stop(this)
        observeSettings()
        syncVolumeObserverFallback()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString().orEmpty()
        val className = event.className?.toString().orEmpty()
        updateForeground(packageName, className)
        if (qqAutomationState != QqAutomationState.OpenQqScanner) return
        if (SystemClock.uptimeMillis() > qqAutomationDeadline) {
            finishQqAutomation("Timed out while waiting for QQ scanner")
            return
        }
        if (packageName != QqScannerSupport.QQ_PACKAGE_NAME) return
        scheduleQqScan(180)
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!settings.volumeHotkeyAccessibilityEnabled) return false
        if (!VolumeHotkeyActionExecutor.hasEnabledHotkeys(settings)) return false
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return false
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> 1
            KeyEvent.KEYCODE_VOLUME_DOWN -> -1
            else -> 0
        }
        if (direction != 0) {
            sequenceDetector.handleDirection(direction, settings, ::triggerAction)
        }
        return false
    }

    override fun onDestroy() {
        AppLogger.i("VolumeHotkeyAccessibilityService.onDestroy")
        settingsJob?.cancel()
        settingsJob = null
        handler.removeCallbacks(qqScanRunnable)
        sequenceDetector.reset()
        qqAutomationState = QqAutomationState.Idle
        if (instance === this) {
            instance = null
        }
        syncVolumeObserverFallback()
        scope.cancel()
        super.onDestroy()
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = scope.launch {
            UserPrefs.observeSettings(this@VolumeHotkeyAccessibilityService).collectLatest { next ->
                settings = next
                if (!next.volumeHotkeyAccessibilityEnabled) {
                    sequenceDetector.reset()
                }
            }
        }
    }

    private fun triggerAction(action: com.lhtstudio.kigtts.app.util.VolumeHotkeyActionSpec) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                VolumeHotkeyActionExecutor.execute(this@VolumeHotkeyAccessibilityService, action)
            }.onFailure {
                AppLogger.e("VolumeHotkeyAccessibilityService.triggerAction failed", it)
            }
        }
    }

    private fun syncVolumeObserverFallback() {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            VolumeHotkeyService.syncWithSettings(applicationContext)
        }
    }

    private fun startQqScannerAutomationInternal(): Boolean {
        qqAutomationState = QqAutomationState.OpenQqScanner
        qqAutomationDeadline = SystemClock.uptimeMillis() + QQ_AUTOMATION_TIMEOUT_MS
        lastQqClickUptime = 0L
        lastQqClickSignature = ""
        qqBackActionCount = 0
        lastQqBackUptime = 0L
        updateForeground("", "")
        return if (QqScannerSupport.launchQq(this)) {
            scheduleQqScan(900)
            true
        } else {
            finishQqAutomation("Failed to launch QQ main activity")
            false
        }
    }

    private fun scheduleQqScan(delayMs: Long) {
        handler.removeCallbacks(qqScanRunnable)
        handler.postDelayed(qqScanRunnable, delayMs)
    }

    private fun scanQqWindow() {
        if (qqAutomationState != QqAutomationState.OpenQqScanner) return
        if (SystemClock.uptimeMillis() > qqAutomationDeadline) {
            finishQqAutomation("Timed out while scanning QQ UI")
            return
        }
        val root = rootInActiveWindow ?: run {
            scheduleQqScan(300)
            return
        }
        val rootPackage = root.packageName?.toString().orEmpty()
        if (rootPackage.isNotEmpty() && rootPackage != QqScannerSupport.QQ_PACKAGE_NAME) {
            scheduleQqScan(300)
            return
        }
        if (isQqScannerWindow(root)) {
            finishQqAutomation("QQ scanner opened")
            return
        }
        if (clickByExactViewId(root, QQ_MENU_SCAN_ITEM_ID)) return
        if (
            (hasExactText(root, "我的二维码") || hasExactText(root, "扫一扫加我为好友")) &&
            clickByExactText(root, "扫一扫")
        ) {
            return
        }
        if (isQqMessagesTabSelected(root)) {
            qqBackActionCount = 0
            if (
                clickByExactViewId(root, QQ_QUICK_ENTRY_BUTTON_ID) ||
                clickByExactContentDescription(root, QQ_QUICK_ENTRY_DESCRIPTION)
            ) {
                return
            }
            scheduleQqScan(500)
            return
        }
        if (clickQqMessagesTab(root)) return
        if (isLikelyQqChatPage(root)) {
            if (backToQqMessagesPage()) return
            scheduleQqScan(500)
            return
        }
        if (clickByExactText(root, "扫一扫")) return
        if (clickByExactContentDescription(root, "扫码")) return
        if (clickByAnyContentDescription(root, listOf("加号", "更多", "更多功能"))) return
        if (backToQqMessagesPage()) return
        scheduleQqScan(500)
    }

    private fun isQqScannerWindow(root: AccessibilityNodeInfo): Boolean {
        return (lastForegroundPackage == QqScannerSupport.QQ_PACKAGE_NAME &&
            lastForegroundClass == QQ_SCAN_ACTIVITY_OLYMPIC) ||
            hasExactText(root, "请对准需要识别的二维码") ||
            (hasExactContentDescription(root, "扫码") && hasExactContentDescription(root, "相册"))
    }

    private fun clickByExactText(root: AccessibilityNodeInfo, text: String): Boolean {
        val target = findFirstNode(root) { it.text?.toString() == text } ?: return false
        return clickQqNode(target, "text:$text")
    }

    private fun clickByExactViewId(root: AccessibilityNodeInfo, viewId: String): Boolean {
        val target = findFirstNode(root) { it.viewIdResourceName == viewId } ?: return false
        return clickQqNode(target, "id:$viewId")
    }

    private fun clickByExactContentDescription(root: AccessibilityNodeInfo, description: String): Boolean {
        val target =
            findFirstNode(root) { it.contentDescription?.toString() == description } ?: return false
        return clickQqNode(target, "desc:$description")
    }

    private fun clickByAnyContentDescription(
        root: AccessibilityNodeInfo,
        descriptions: List<String>
    ): Boolean {
        val target =
            findFirstNode(root) { node ->
                val description = node.contentDescription?.toString().orEmpty()
                descriptions.any { description.contains(it) }
            } ?: return false
        return clickQqNode(target, "desc:${target.contentDescription}")
    }

    private fun clickQqMessagesTab(root: AccessibilityNodeInfo): Boolean {
        val target =
            findBottomTextNode(root, QQ_MESSAGES_TAB_TEXT) {
                !it.isSelected && !hasSelectedAncestor(it)
            } ?: return false
        return clickQqNode(target, "tab:$QQ_MESSAGES_TAB_TEXT")
    }

    private fun isQqMessagesTabSelected(root: AccessibilityNodeInfo): Boolean {
        return findBottomTextNode(root, QQ_MESSAGES_TAB_TEXT) {
            it.isSelected || hasSelectedAncestor(it)
        } != null
    }

    private fun hasQqMessagesTab(root: AccessibilityNodeInfo): Boolean {
        return findBottomTextNode(root, QQ_MESSAGES_TAB_TEXT) { true } != null
    }

    private fun isLikelyQqChatPage(root: AccessibilityNodeInfo): Boolean {
        return !hasQqMessagesTab(root) && hasAnyTextOrContentDescription(root, QQ_CHAT_PAGE_MARKERS)
    }

    private fun findBottomTextNode(
        root: AccessibilityNodeInfo,
        text: String,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val bottomStart = rootBounds.top + (rootBounds.height() * BOTTOM_TAB_REGION_RATIO).toInt()
        return findFirstNode(root) { node ->
            if (node.text?.toString() != text || !predicate(node)) return@findFirstNode false
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            !bounds.isEmpty && bounds.centerY() >= bottomStart
        }
    }

    private fun hasSelectedAncestor(node: AccessibilityNodeInfo): Boolean {
        var current = node.parent
        while (current != null) {
            if (current.isSelected) return true
            current = current.parent
        }
        return false
    }

    private fun hasAnyTextOrContentDescription(
        root: AccessibilityNodeInfo,
        markers: List<String>
    ): Boolean {
        return findFirstNode(root) { node ->
            val text = node.text?.toString().orEmpty()
            val description = node.contentDescription?.toString().orEmpty()
            markers.any { marker -> text.contains(marker) || description.contains(marker) }
        } != null
    }

    private fun backToQqMessagesPage(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (qqBackActionCount >= MAX_QQ_BACK_ACTIONS || now - lastQqBackUptime < QQ_BACK_ACTION_DEBOUNCE_MS) {
            return false
        }
        val handled = performGlobalAction(GLOBAL_ACTION_BACK)
        if (handled) {
            qqBackActionCount += 1
            lastQqBackUptime = now
            scheduleQqScan(900)
        }
        return handled
    }

    private fun clickQqNode(node: AccessibilityNodeInfo, key: String): Boolean {
        val target = findClickableAncestor(node) ?: node
        val signature = buildQqClickSignature(target, key)
        if (
            signature == lastQqClickSignature &&
            SystemClock.uptimeMillis() - lastQqClickUptime < QQ_CLICK_DEBOUNCE_MS
        ) {
            return false
        }
        val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK) || dispatchQqTap(target)
        if (clicked) {
            lastQqClickSignature = signature
            lastQqClickUptime = SystemClock.uptimeMillis()
            scheduleQqScan(800)
        }
        return clicked
    }

    private fun dispatchQqTap(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return false
        val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
        val gesture =
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
                .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        while (current != null) {
            if (current.isClickable && current.isEnabled) return current
            current = current.parent
        }
        return null
    }

    private fun hasExactText(root: AccessibilityNodeInfo, text: String): Boolean {
        return findFirstNode(root) { it.text?.toString() == text } != null
    }

    private fun hasExactContentDescription(root: AccessibilityNodeInfo, description: String): Boolean {
        return findFirstNode(root) { it.contentDescription?.toString() == description } != null
    }

    private fun findFirstNode(
        root: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) return node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::addLast)
            }
        }
        return null
    }

    private fun buildQqClickSignature(node: AccessibilityNodeInfo, key: String): String {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return "$key|${rect.flattenToString()}|${node.className}"
    }

    private fun finishQqAutomation(message: String) {
        qqAutomationState = QqAutomationState.Idle
        handler.removeCallbacks(qqScanRunnable)
        qqAutomationStatus = message
        AppLogger.i("VolumeHotkeyAccessibilityService.qqAutomation: $message")
    }

    private fun updateForeground(packageName: String, className: String) {
        lastForegroundPackage = packageName
        lastForegroundClass = className
    }

    private enum class QqAutomationState {
        Idle,
        OpenQqScanner
    }

    companion object {
        private const val QQ_AUTOMATION_TIMEOUT_MS = 15_000L
        private const val QQ_CLICK_DEBOUNCE_MS = 1_200L
        private const val QQ_SCAN_ACTIVITY_OLYMPIC =
            "com.tencent.mobileqq.olympic.activity.QQScanActivity"
        private const val QQ_QUICK_ENTRY_BUTTON_ID = "com.tencent.mobileqq:id/ba3"
        private const val QQ_MENU_SCAN_ITEM_ID =
            "com.tencent.mobileqq:string/conversation_options_saoyisao"
        private const val QQ_QUICK_ENTRY_DESCRIPTION = "快捷入口"
        private const val QQ_MESSAGES_TAB_TEXT = "消息"
        private const val MAX_QQ_BACK_ACTIONS = 3
        private const val QQ_BACK_ACTION_DEBOUNCE_MS = 900L
        private const val BOTTOM_TAB_REGION_RATIO = 0.72f
        private val QQ_CHAT_PAGE_MARKERS =
            listOf(
                "发消息",
                "输入消息",
                "按住 说话",
                "按住说话",
                "切换到语音",
                "语音输入",
                "表情",
                "更多功能",
                "发送"
            )

        @Volatile
        private var instance: VolumeHotkeyAccessibilityService? = null

        @Volatile
        private var qqAutomationStatus: String = "QQ scanner automation not connected"

        fun buildSettingsIntent(): Intent {
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        fun isConnected(): Boolean = instance != null

        fun requestOpenQqScanner(context: Context): Boolean {
            if (!isEnabled(context)) return false
            return instance?.startQqScannerAutomationInternal() ?: false
        }

        fun qqAutomationStatusSnapshot(): String = qqAutomationStatus

        fun isEnabled(context: Context): Boolean {
            val accessibilityEnabled =
                runCatching {
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_ENABLED
                    ) == 1
                }.getOrDefault(false)
            if (!accessibilityEnabled) return false
            val enabledServices =
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ).orEmpty()
            if (enabledServices.isBlank()) return false
            val componentName = ComponentName(context, VolumeHotkeyAccessibilityService::class.java)
            val expectedLong = componentName.flattenToString()
            val expectedShort = componentName.flattenToShortString()
            return enabledServices.split(':').any { entry ->
                entry.equals(expectedLong, ignoreCase = true) ||
                    entry.equals(expectedShort, ignoreCase = true)
            }
        }
    }
}
