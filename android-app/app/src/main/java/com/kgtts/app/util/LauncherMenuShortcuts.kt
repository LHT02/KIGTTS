package com.lhtstudio.kigtts.app.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Process
import com.lhtstudio.kigtts.app.R
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.ui.LauncherShortcutProxyActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import kotlin.math.max

object LauncherMenuShortcuts {
    const val ACTION_PROXY_EXTERNAL_SHORTCUT =
        "com.lhtstudio.kigtts.app.action.PROXY_EXTERNAL_SHORTCUT"

    private const val EXTRA_PACKAGE_NAME = "launcher_proxy_package_name"
    private const val EXTRA_CLASS_NAME = "launcher_proxy_class_name"
    private const val EXTRA_APP_LABEL = "launcher_proxy_app_label"
    private const val EXTRA_SHORTCUT_ID = "launcher_proxy_shortcut_id"
    private const val EXTRA_SHORTCUT_TITLE = "launcher_proxy_shortcut_title"
    private const val PROXY_SHORTCUT_PREFIX = "proxy_external:"

    private data class OverlayAppShortcut(
        val packageName: String,
        val className: String,
        val label: String
    )

    private data class ManifestShortcutSpec(
        val id: String,
        val title: String,
        val intents: List<Intent> = emptyList()
    )

    private data class ExternalShortcutTarget(
        val packageName: String,
        val className: String,
        val appLabel: String,
        val shortcutId: String,
        val shortcutTitle: String
    )

    private fun overlayShortcutKey(shortcut: OverlayAppShortcut): String =
        "app:${shortcut.packageName}/${shortcut.className}"

    suspend fun syncFromOverlayShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        withContext(Dispatchers.Default) {
            runCatching {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return@runCatching
                val maxCount = shortcutManager.maxShortcutCountPerActivity.coerceAtLeast(1)
                val preserved =
                    shortcutManager.dynamicShortcuts
                        .filterNot { it.id.startsWith(PROXY_SHORTCUT_PREFIX) }
                        .take(maxCount)
                val availableSlots = (maxCount - preserved.size).coerceAtLeast(0)
                val proxies =
                    if (availableSlots > 0) {
                        loadExternalShortcutTargets(context)
                            .take(availableSlots)
                            .mapIndexed { index, target ->
                                buildDynamicShortcut(context, target, index)
                            }
                    } else {
                        emptyList()
                    }
                shortcutManager.setDynamicShortcuts((preserved + proxies).take(maxCount))
            }.onFailure {
                AppLogger.e("LauncherMenuShortcuts.syncFromOverlayShortcuts failed", it)
            }
        }
    }

    fun handleProxyIntent(activity: Activity, intent: Intent?): Boolean {
        if (intent?.action != ACTION_PROXY_EXTERNAL_SHORTCUT) return false
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty().trim()
        val className = intent.getStringExtra(EXTRA_CLASS_NAME).orEmpty().trim()
        if (packageName.isBlank() || className.isBlank()) return true
        val appLabel =
            intent.getStringExtra(EXTRA_APP_LABEL).orEmpty().ifBlank {
                className.substringAfterLast('.')
            }
        val shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID).orEmpty().trim()
        val shortcut =
            OverlayAppShortcut(
                packageName = packageName,
                className = className,
                label = appLabel
            )
        runCatching {
            val launcherTarget =
                shortcutId.takeIf { it.isNotBlank() }?.let { id ->
                    queryLauncherShortcuts(activity, shortcut).firstOrNull { it.id == id }
                }
            val manifestTarget =
                if (launcherTarget == null && shortcutId.isNotBlank()) {
                    queryManifestShortcuts(activity, shortcut).firstOrNull { it.id == shortcutId }
                } else {
                    null
                }
            when {
                launcherTarget != null -> launchExternalLauncherShortcut(activity, shortcut, launcherTarget)
                manifestTarget != null -> launchExternalManifestShortcut(activity, shortcut, manifestTarget)
                else -> launchExternalShortcut(activity, shortcut)
            }
        }.onFailure {
            AppLogger.e("LauncherMenuShortcuts.handleProxyIntent failed", it)
            runCatching { launchExternalShortcut(activity, shortcut) }
        }
        return true
    }

    private suspend fun loadExternalShortcutTargets(context: Context): List<ExternalShortcutTarget> {
        val overlayShortcuts =
            reorderOverlayShortcuts(
                shortcuts = loadOverlayShortcuts(context),
                orderedKeys = loadOverlayShortcutOrder(context)
            )
        if (overlayShortcuts.isEmpty()) return emptyList()
        val targets = mutableListOf<ExternalShortcutTarget>()
        val seen = linkedSetOf<String>()
        overlayShortcuts.forEach { shortcut ->
            val launcherShortcuts = queryLauncherShortcuts(context, shortcut)
            if (launcherShortcuts.isNotEmpty()) {
                launcherShortcuts.forEach { info ->
                    val title = shortcutTitle(info.shortLabel?.toString(), info.longLabel?.toString(), info.id)
                    val dedupeKey = "launcher:${shortcut.packageName}:${info.id}"
                    if (title.isNotBlank() && seen.add(dedupeKey)) {
                        targets += ExternalShortcutTarget(
                            packageName = shortcut.packageName,
                            className = shortcut.className,
                            appLabel = shortcut.label,
                            shortcutId = info.id,
                            shortcutTitle = title
                        )
                    }
                }
            } else {
                queryManifestShortcuts(context, shortcut).forEach { spec ->
                    val dedupeKey = "manifest:${shortcut.packageName}:${spec.id}"
                    if (spec.title.isNotBlank() && seen.add(dedupeKey)) {
                        targets += ExternalShortcutTarget(
                            packageName = shortcut.packageName,
                            className = shortcut.className,
                            appLabel = shortcut.label,
                            shortcutId = spec.id,
                            shortcutTitle = spec.title
                        )
                    }
                }
            }
        }
        return targets
    }

    private suspend fun loadOverlayShortcuts(context: Context): List<OverlayAppShortcut> {
        val raw = UserPrefs.getFloatingOverlayShortcuts(context)
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val packageName = obj.optString("packageName", "").trim()
                    val className = obj.optString("className", "").trim()
                    val label = obj.optString("label", "").trim()
                    if (packageName.isEmpty() || className.isEmpty()) continue
                    add(
                        OverlayAppShortcut(
                            packageName = packageName,
                            className = className,
                            label = label.ifEmpty { className.substringAfterLast('.') }
                        )
                    )
                }
            }
        }.onFailure {
            AppLogger.e("LauncherMenuShortcuts.loadOverlayShortcuts failed", it)
        }.getOrElse { emptyList() }
    }

    private suspend fun loadOverlayShortcutOrder(context: Context): List<String> {
        val raw = UserPrefs.getFloatingOverlayLayout(context)
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val root = JSONObject(raw)
            val arr = root.optJSONArray("order") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val key = arr.optString(i).trim()
                    if (key.isNotEmpty() && key !in this) add(key)
                }
            }
        }.onFailure {
            AppLogger.e("LauncherMenuShortcuts.loadOverlayShortcutOrder failed", it)
        }.getOrElse { emptyList() }
    }

    private fun reorderOverlayShortcuts(
        shortcuts: List<OverlayAppShortcut>,
        orderedKeys: List<String>
    ): List<OverlayAppShortcut> {
        if (shortcuts.isEmpty() || orderedKeys.isEmpty()) return shortcuts
        val byKey = shortcuts.associateBy(::overlayShortcutKey)
        val ordered = orderedKeys.mapNotNull(byKey::get)
        if (ordered.isEmpty()) return shortcuts
        return ordered + shortcuts.filterNot { overlayShortcutKey(it) in orderedKeys.toSet() }
    }

    private fun buildDynamicShortcut(
        context: Context,
        target: ExternalShortcutTarget,
        rank: Int
    ): ShortcutInfo {
        val label = buildMenuLabel(target.appLabel, target.shortcutTitle)
        return ShortcutInfo.Builder(context, proxyShortcutId(target))
            .setShortLabel(label)
            .setLongLabel(label)
            .setRank(rank)
            .setIcon(resolveTargetIcon(context, target))
            .setIntent(
                Intent(context, LauncherShortcutProxyActivity::class.java).apply {
                    action = ACTION_PROXY_EXTERNAL_SHORTCUT
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    putExtra(EXTRA_PACKAGE_NAME, target.packageName)
                    putExtra(EXTRA_CLASS_NAME, target.className)
                    putExtra(EXTRA_APP_LABEL, target.appLabel)
                    putExtra(EXTRA_SHORTCUT_ID, target.shortcutId)
                    putExtra(EXTRA_SHORTCUT_TITLE, target.shortcutTitle)
                }
            )
            .build()
    }

    private fun buildMenuLabel(appLabel: String, shortcutTitle: String): String {
        val app = appLabel.trim()
        val title = shortcutTitle.trim()
        return when {
            app.isBlank() -> title
            title.isBlank() -> app
            title.contains(app) -> title
            else -> "$app · $title"
        }.ifBlank { "快捷方式" }
    }

    private fun proxyShortcutId(target: ExternalShortcutTarget): String =
        "$PROXY_SHORTCUT_PREFIX${target.packageName}:${target.shortcutId}"

    private fun shortcutTitle(shortLabel: String?, longLabel: String?, fallback: String): String {
        return when {
            !shortLabel.isNullOrBlank() -> shortLabel.trim()
            !longLabel.isNullOrBlank() -> longLabel.trim()
            else -> fallback.trim()
        }
    }

    private fun resolveTargetIcon(context: Context, target: ExternalShortcutTarget): Icon {
        val pm = context.packageManager
        val component = ComponentName(target.packageName, target.className)
        val drawable =
            runCatching { pm.getActivityIcon(component) }.getOrNull()
                ?: runCatching { pm.getApplicationIcon(target.packageName) }.getOrNull()
        return drawable?.let { Icon.createWithBitmap(drawable.toBitmap()) }
            ?: Icon.createWithResource(context, R.mipmap.ic_launcher_round)
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && bitmap != null) return bitmap
        val size = max(96, max(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1)))
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, size, size)
        draw(canvas)
        return bitmap
    }

    private fun queryLauncherShortcuts(
        context: Context,
        shortcut: OverlayAppShortcut
    ): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        val activity = ComponentName(shortcut.packageName, shortcut.className)
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
            AppLogger.e("LauncherMenuShortcuts.queryLauncherShortcuts failed", it)
        }.getOrElse { emptyList() }
    }

    private fun queryManifestShortcuts(
        context: Context,
        shortcut: OverlayAppShortcut
    ): List<ManifestShortcutSpec> {
        return runCatching {
            val component = ComponentName(shortcut.packageName, shortcut.className)
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
            val foreign = context.createPackageContext(shortcut.packageName, Context.CONTEXT_IGNORE_SECURITY)
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
                                labelResId != 0 ->
                                    runCatching { foreign.resources.getString(labelResId) }.getOrDefault("")
                                rawLabel.startsWith("@string/") -> rawLabel.removePrefix("@string/")
                                else -> rawLabel
                            }.trim()
                        currentEnabled = parser.getAttributeBooleanValue(androidNs, "enabled", true)
                        currentIntents = mutableListOf()
                    }
                    eventType == XmlPullParser.START_TAG &&
                        parser.name == "intent" &&
                        currentId.isNotBlank() -> {
                        parseManifestShortcutIntent(shortcut, parser, androidNs)?.let(currentIntents::add)
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
            AppLogger.e("LauncherMenuShortcuts.queryManifestShortcuts failed", it)
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
                    component = ComponentName(targetPackage, targetClass)
                }
                !targetPackage.isNullOrBlank() -> {
                    `package` = targetPackage
                }
                !targetClass.isNullOrBlank() -> {
                    component = ComponentName(shortcut.packageName, targetClass)
                }
                else -> {
                    `package` = shortcut.packageName
                }
            }
        }
    }

    private fun launchExternalLauncherShortcut(
        context: Context,
        shortcut: OverlayAppShortcut,
        shortcutInfo: ShortcutInfo
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            launchExternalShortcut(context, shortcut)
            return
        }
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        if (launcherApps == null) {
            launchExternalShortcut(context, shortcut)
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
            AppLogger.e("LauncherMenuShortcuts.launchExternalLauncherShortcut failed", it)
            launchExternalShortcut(context, shortcut)
        }
    }

    private fun launchExternalManifestShortcut(
        context: Context,
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
            launchExternalShortcut(context, shortcut)
            return
        }
        runCatching {
            if (intents.size == 1) {
                context.startActivity(intents.first())
            } else {
                context.startActivities(intents.toTypedArray())
            }
        }.onFailure {
            AppLogger.e("LauncherMenuShortcuts.launchExternalManifestShortcut failed", it)
            launchExternalShortcut(context, shortcut)
        }
    }

    private fun launchExternalShortcut(context: Context, shortcut: OverlayAppShortcut) {
        runCatching {
            val intent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(shortcut.packageName, shortcut.className)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
            context.startActivity(intent)
        }.onFailure {
            AppLogger.e("LauncherMenuShortcuts.launchExternalShortcut failed", it)
        }
    }
}
