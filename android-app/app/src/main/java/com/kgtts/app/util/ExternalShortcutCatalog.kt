package com.lhtstudio.kigtts.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Build
import android.os.Process
import com.lhtstudio.kigtts.app.data.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

data class ExternalShortcutChoice(
    val packageName: String,
    val className: String,
    val appLabel: String,
    val shortcutId: String,
    val shortcutTitle: String
)

object ExternalShortcutCatalog {
    const val APP_LAUNCH_SHORTCUT_ID = "__app_launch__"

    private data class ConfiguredAppShortcut(
        val packageName: String,
        val className: String,
        val label: String
    )

    private data class ManifestShortcutSpec(
        val id: String,
        val title: String
    )

    private data class HardcodedShortcutSpec(
        val id: String,
        val title: String,
        val dataUri: String? = null,
        val targetClass: String? = null,
        val booleanExtras: Map<String, Boolean> = emptyMap(),
        val stringExtras: Map<String, String> = emptyMap()
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

    suspend fun loadAllShortcutChoices(context: Context): List<ExternalShortcutChoice> =
        withContext(Dispatchers.IO) {
            val apps = loadConfiguredApps(context)
            val allowHardcodedSupplement =
                UserPrefs.getSettings(context).floatingOverlayHardcodedShortcutSupplement
            val result = mutableListOf<ExternalShortcutChoice>()
            val seen = linkedSetOf<String>()
            apps.forEach { app ->
                queryShortcutChoices(context, app, allowHardcodedSupplement).forEach { choice ->
                    val key = shortcutKey(choice.packageName, choice.shortcutId)
                    if (seen.add(key)) {
                        result += choice
                    }
                }
            }
            result.sortedWith(
                compareBy<ExternalShortcutChoice>({ it.appLabel.lowercase(Locale.getDefault()) }, { it.shortcutTitle.lowercase(Locale.getDefault()) })
            )
        }

    fun launchChoice(context: Context, choice: ExternalShortcutChoice) {
        if (choice.shortcutId == APP_LAUNCH_SHORTCUT_ID) {
            launchApp(context, choice.packageName, choice.className)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            if (launcherApps != null) {
                runCatching {
                    launcherApps.startShortcut(
                        choice.packageName,
                        choice.shortcutId,
                        null,
                        null,
                        Process.myUserHandle()
                    )
                }.onSuccess {
                    return
                }.onFailure {
                    AppLogger.e("ExternalShortcutCatalog.launchChoice failed", it)
                }
            }
        }
        findHardcodedShortcutSpec(choice.packageName, choice.shortcutId)?.let { spec ->
            if (launchHardcodedShortcut(context, choice.packageName, spec)) {
                return
            }
        }
        launchApp(context, choice.packageName, choice.className)
    }

    private suspend fun loadConfiguredApps(context: Context): List<ConfiguredAppShortcut> {
        val raw = UserPrefs.getFloatingOverlayShortcuts(context)
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (index in 0 until arr.length()) {
                    val obj = arr.optJSONObject(index) ?: continue
                    val packageName = obj.optString("packageName", "").trim()
                    val className = obj.optString("className", "").trim()
                    val label = obj.optString("label", "").trim()
                    if (packageName.isBlank() || className.isBlank()) continue
                    add(
                        ConfiguredAppShortcut(
                            packageName = packageName,
                            className = className,
                            label = label.ifBlank { packageName }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun queryShortcutChoices(
        context: Context,
        app: ConfiguredAppShortcut,
        allowHardcodedSupplement: Boolean
    ): List<ExternalShortcutChoice> {
        val result = mutableListOf<ExternalShortcutChoice>()
        val seen = linkedSetOf<String>()
        if (seen.add(shortcutKey(app.packageName, APP_LAUNCH_SHORTCUT_ID))) {
            result += ExternalShortcutChoice(
                packageName = app.packageName,
                className = app.className,
                appLabel = app.label,
                shortcutId = APP_LAUNCH_SHORTCUT_ID,
                shortcutTitle = "打开应用"
            )
        }
        queryLauncherShortcuts(context, app).forEach { info ->
            val title = shortcutTitle(info.shortLabel?.toString(), info.longLabel?.toString(), info.id)
            if (
                title.isNotBlank() &&
                shouldIncludeShortcut(app.packageName, title, info.id) &&
                seen.add(shortcutKey(app.packageName, info.id))
            ) {
                result += ExternalShortcutChoice(
                    packageName = app.packageName,
                    className = app.className,
                    appLabel = app.label,
                    shortcutId = info.id,
                    shortcutTitle = title
                )
            }
        }
        if (result.isEmpty()) {
            queryManifestShortcuts(context, app).forEach { spec ->
                if (
                    spec.title.isNotBlank() &&
                    shouldIncludeShortcut(app.packageName, spec.title, spec.id) &&
                    seen.add(shortcutKey(app.packageName, spec.id))
                ) {
                    result += ExternalShortcutChoice(
                        packageName = app.packageName,
                        className = app.className,
                        appLabel = app.label,
                        shortcutId = spec.id,
                        shortcutTitle = spec.title
                    )
                }
            }
        }
        hardcodedLauncherShortcuts[app.packageName].orEmpty()
            .filter { spec -> shouldUseHardcodedShortcutSupplement(app.packageName, spec.id, allowHardcodedSupplement) }
            .forEach { spec ->
                if (
                    spec.title.isNotBlank() &&
                    shouldIncludeShortcut(app.packageName, spec.title, spec.id) &&
                    seen.add(shortcutKey(app.packageName, spec.id))
                ) {
                    result += ExternalShortcutChoice(
                        packageName = app.packageName,
                        className = app.className,
                        appLabel = app.label,
                        shortcutId = spec.id,
                        shortcutTitle = spec.title
                    )
                }
            }
        return result
    }

    private fun shouldUseHardcodedShortcutSupplement(
        packageName: String,
        shortcutId: String,
        allowHardcodedSupplement: Boolean
    ): Boolean {
        return allowHardcodedSupplement || isAlwaysRetainedHardcodedShortcut(packageName, shortcutId)
    }

    private fun isAlwaysRetainedHardcodedShortcut(packageName: String, shortcutId: String): Boolean {
        return (packageName == "com.tencent.mm" && shortcutId == "launch_type_scan_qrcode") ||
            (packageName == AlipayScannerSupport.ALIPAY_PACKAGE_NAME && shortcutId == "1001")
    }

    private fun queryLauncherShortcuts(
        context: Context,
        app: ConfiguredAppShortcut
    ): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        val activity = ComponentName(app.packageName, app.className)
        return runCatching {
            val query =
                LauncherApps.ShortcutQuery().apply {
                    setPackage(app.packageName)
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
            AppLogger.e("ExternalShortcutCatalog.queryLauncherShortcuts failed", it)
        }.getOrElse { emptyList() }
    }

    private fun queryManifestShortcuts(
        context: Context,
        app: ConfiguredAppShortcut
    ): List<ManifestShortcutSpec> {
        return runCatching {
            val component = ComponentName(app.packageName, app.className)
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                } else {
                    null
                }
            val activityInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getActivityInfo(component, flags!!)
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getActivityInfo(component, PackageManager.GET_META_DATA)
                }
            val shortcutsResId = activityInfo.metaData?.getInt("android.app.shortcuts", 0) ?: 0
            if (shortcutsResId == 0) return@runCatching emptyList()
            val foreign = context.createPackageContext(app.packageName, Context.CONTEXT_IGNORE_SECURITY)
            val parser = foreign.resources.getXml(shortcutsResId)
            val androidNs = "http://schemas.android.com/apk/res/android"
            val result = mutableListOf<ManifestShortcutSpec>()
            var currentId = ""
            var currentTitle = ""
            var currentEnabled = true
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
                    }
                    eventType == XmlPullParser.END_TAG && parser.name == "shortcut" -> {
                        if (currentEnabled && currentId.isNotBlank() && currentTitle.isNotBlank()) {
                            result += ManifestShortcutSpec(
                                id = currentId,
                                title = currentTitle
                            )
                        }
                        currentId = ""
                        currentTitle = ""
                        currentEnabled = true
                    }
                }
                eventType = parser.next()
            }
            result.distinctBy { it.id }
        }.onFailure {
            AppLogger.e("ExternalShortcutCatalog.queryManifestShortcuts failed", it)
        }.getOrElse { emptyList() }
    }

    private fun shortcutTitle(shortLabel: String?, longLabel: String?, fallbackId: String): String {
        return when {
            !shortLabel.isNullOrBlank() -> shortLabel
            !longLabel.isNullOrBlank() -> longLabel
            else -> fallbackId
        }
    }

    private fun shouldIncludeShortcut(packageName: String, title: String, shortcutId: String): Boolean {
        val haystack = "$packageName|$title|$shortcutId".lowercase(Locale.getDefault())
        return blockedShortcutKeywords.none { keyword -> haystack.contains(keyword.lowercase(Locale.getDefault())) }
    }

    private fun shortcutKey(packageName: String, shortcutId: String): String = "$packageName:$shortcutId"

    private fun findHardcodedShortcutSpec(packageName: String, shortcutId: String): HardcodedShortcutSpec? {
        return hardcodedLauncherShortcuts[packageName].orEmpty().firstOrNull { it.id == shortcutId }
    }

    private fun launchHardcodedShortcut(
        context: Context,
        packageName: String,
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
        val explicitIntent = buildHardcodedIntent(context, packageName, spec, useLaunchIntent = false)
        if (explicitIntent != null) {
            val launched =
                runCatching {
                    context.startActivity(explicitIntent)
                    true
                }.onFailure {
                    AppLogger.e("ExternalShortcutCatalog.launchHardcodedShortcut explicit failed", it)
                }.getOrDefault(false)
            if (launched) return true
        }
        val launchIntent = buildHardcodedIntent(context, packageName, spec, useLaunchIntent = true)
        return runCatching {
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        }.onFailure {
            AppLogger.e("ExternalShortcutCatalog.launchHardcodedShortcut launchIntent failed", it)
        }.getOrDefault(false)
    }

    private fun buildHardcodedIntent(
        context: Context,
        packageName: String,
        spec: HardcodedShortcutSpec,
        useLaunchIntent: Boolean
    ): Intent? {
        val rawIntent: Intent? =
            if (useLaunchIntent) {
                context.packageManager.getLaunchIntentForPackage(packageName)
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

    private fun launchApp(context: Context, packageName: String, className: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(packageName, className)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
            )
        }.onFailure {
            AppLogger.e("ExternalShortcutCatalog.launchApp failed", it)
        }
    }
}
