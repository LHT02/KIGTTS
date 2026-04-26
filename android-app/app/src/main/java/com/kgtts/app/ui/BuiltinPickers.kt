package com.lhtstudio.kigtts.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.TextButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.lhtstudio.kigtts.app.R
import com.lhtstudio.kigtts.app.data.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class BuiltinFileSortOption(val label: String) {
    NameAsc("名称升序"),
    NameDesc("名称降序"),
    TimeDesc("时间新到旧"),
    TimeAsc("时间旧到新")
}

data class BuiltinGalleryItem(
    val uri: Uri,
    val bucketId: String,
    val bucketName: String,
    val dateMs: Long
)

private data class BuiltinFileRoot(
    val dir: File,
    val label: String,
    val iconName: String
)

private data class BuiltinSafRoot(
    val treeUri: Uri,
    val label: String,
    val iconName: String,
    val document: DocumentFile
)

private sealed interface BuiltinBrowserLocation {
    data object Root : BuiltinBrowserLocation

    data class FileDirectory(
        val root: BuiltinFileRoot,
        val dir: File
    ) : BuiltinBrowserLocation

    data class SafDirectory(
        val root: BuiltinSafRoot,
        val document: DocumentFile,
        val displayName: String
    ) : BuiltinBrowserLocation
}

private sealed interface BuiltinBrowserEntry {
    val id: String
    val label: String
    val iconName: String
    val isDirectory: Boolean

    data class Directory(
        override val id: String,
        override val label: String,
        override val iconName: String,
        val location: BuiltinBrowserLocation
    ) : BuiltinBrowserEntry {
        override val isDirectory: Boolean = true
    }

    data class FileItem(
        override val id: String,
        override val label: String,
        override val iconName: String,
        val uri: Uri
    ) : BuiltinBrowserEntry {
        override val isDirectory: Boolean = false
    }
}

private val BuiltinMaterialSymbolsSharp = FontFamily(
    Font(
        resId = R.font.material_symbols_sharp,
        weight = FontWeight.W500
    )
)

@Composable
private fun BuiltinMsIcon(
    name: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val fontScaleBlockMode = LocalFontScaleBlockMode.current
    val iconTextSize = if (fontScaleBlockMode == UserPrefs.FONT_SCALE_BLOCK_NONE) {
        22.sp
    } else {
        with(LocalDensity.current) { 22.dp.toSp() }
    }
    val a11yModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    Text(
        text = name,
        modifier = a11yModifier,
        color = tint ?: MaterialTheme.colors.onSurface,
        style = TextStyle(
            fontFamily = BuiltinMaterialSymbolsSharp,
            fontWeight = FontWeight.W500,
            fontSize = iconTextSize,
            lineHeight = iconTextSize,
            letterSpacing = 0.sp,
            fontFeatureSettings = "'liga' 1"
        )
    )
}

@Composable
private fun rememberBuiltinTopEndPopupPositionProvider(verticalMargin: androidx.compose.ui.unit.Dp = 4.dp): PopupPositionProvider {
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
private fun BuiltinAnimatedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var renderMenu by remember { mutableStateOf(expanded) }
    val popupPositionProvider = rememberBuiltinTopEndPopupPositionProvider()
    val menuAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "builtin_dropdown_alpha"
    )
    val menuScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.94f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "builtin_dropdown_scale"
    )
    if (expanded && !renderMenu) renderMenu = true
    LaunchedEffect(expanded) {
        if (!expanded) {
            kotlinx.coroutines.delay(180L)
            renderMenu = false
        }
    }
    if (!renderMenu) return
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
                    modifier = Modifier.widthIn(min = 196.dp, max = 216.dp),
                    shape = RoundedCornerShape(4.dp),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 8.dp
                ) {
                    Column(content = content)
                }
            }
        }
    }
}

@Composable
fun BuiltinFilePickerDialog(
    title: String,
    allowedExtensions: Set<String> = emptySet(),
    multiSelect: Boolean = false,
    onDismiss: () -> Unit,
    onPicked: (Uri) -> Unit,
    onPickedMultiple: (List<Uri>) -> Unit = {},
    onOpenSystemPicker: (() -> Unit)? = null,
    onOpenSystemPickerMultiple: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(BuiltinFileSortOption.TimeDesc) }
    var sortExpanded by remember { mutableStateOf(false) }

    val fileRoots = remember { builtinFileRoots(context) }
    var selectedUris by remember { mutableStateOf<Map<String, Uri>>(emptyMap()) }
    var safRootsRevision by remember { mutableStateOf(0) }
    var navigationStack by remember { mutableStateOf(listOf<BuiltinBrowserLocation>(BuiltinBrowserLocation.Root)) }
    val currentLocation = navigationStack.lastOrNull() ?: BuiltinBrowserLocation.Root

    val normalizedExt = remember(allowedExtensions) {
        allowedExtensions.map { it.lowercase(Locale.US).trim('.') }.filter { it.isNotBlank() }.toSet()
    }
    val readPermission = remember(normalizedExt) { builtinReadPermissionForExtensions(normalizedExt) }
    val usesSharedNonMediaFallback = remember(normalizedExt) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            normalizedExt.any { it !in BuiltinAudioFileExtensions && it !in BuiltinImageFileExtensions }
    }
    val systemPickerAction = remember(multiSelect, onOpenSystemPicker, onOpenSystemPickerMultiple) {
        if (multiSelect) onOpenSystemPickerMultiple ?: onOpenSystemPicker else onOpenSystemPicker
    }
    val readPermissionMessage = remember(readPermission) {
        when (readPermission) {
            Manifest.permission.READ_MEDIA_AUDIO -> "需要音频读取权限，否则共享目录中的音频文件可能无法显示。"
            Manifest.permission.READ_MEDIA_IMAGES -> "需要图片读取权限，否则共享目录中的图片文件可能无法显示。"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "需要存储读取权限，否则共享目录中的文件可能无法显示。"
            else -> null
        }
    }
    var hasReadPermission by remember(readPermission) {
        mutableStateOf(
            readPermission == null ||
                    ContextCompat.checkSelfPermission(context, readPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasReadPermission = granted
    }
    val treePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }
        safRootsRevision += 1
        createBuiltinSafRoot(context, uri)?.let { root ->
            navigationStack = listOf(
                BuiltinBrowserLocation.Root,
                BuiltinBrowserLocation.SafDirectory(
                    root = root,
                    document = root.document,
                    displayName = root.label
                )
            )
        }
    }

    val safRoots by produceState<List<BuiltinSafRoot>>(initialValue = emptyList(), safRootsRevision) {
        value = withContext(Dispatchers.IO) { loadBuiltinSafRoots(context) }
    }

    val fileItems by produceState<List<BuiltinBrowserEntry>>(
        initialValue = emptyList(),
        currentLocation,
        search,
        sortOption,
        normalizedExt,
        hasReadPermission,
        safRoots
    ) {
        value = withContext(Dispatchers.IO) {
            loadBuiltinEntries(
                location = currentLocation,
                fileRoots = fileRoots,
                safRoots = safRoots,
                query = search,
                sortOption = sortOption,
                allowedExtensions = normalizedExt
            )
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        KigttsFontScaleProvider {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            Text(title, style = MaterialTheme.typography.h6)

            if (readPermission != null && !hasReadPermission) {
                Text(readPermissionMessage ?: "需要读取权限。", style = MaterialTheme.typography.body2)
                Button(onClick = { permissionLauncher.launch(readPermission) }) {
                    Text("授予读取权限")
                }
            } else if ((usesSharedNonMediaFallback || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && systemPickerAction != null) {
                Text(
                    "若共享目录中的语音包、预设包、模型或音频文件没有显示，请点击“授权目录”授予目录访问，或直接使用系统文件选择器。",
                    style = MaterialTheme.typography.body2
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { treePermissionLauncher.launch(null) }) {
                        Text("授权目录")
                    }
                    if (systemPickerAction != null) {
                        OutlinedButton(onClick = systemPickerAction) { Text("系统文件选择器") }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BuiltinFlatIconButton(
                        onClick = {
                            if (navigationStack.size > 1) {
                                navigationStack = navigationStack.dropLast(1)
                            }
                        },
                        enabled = navigationStack.size > 1,
                        iconName = "arrow_upward",
                        contentDescription = "上一级"
                    )

                    Box {
                        BuiltinFlatIconButton(
                            onClick = { sortExpanded = true },
                            iconName = "sort",
                            contentDescription = "排序方式"
                        )
                        BuiltinAnimatedDropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            BuiltinFileSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    onClick = {
                                        sortExpanded = false
                                        sortOption = option
                                    }
                                ) {
                                    Text(option.label)
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索文件/文件夹") },
                singleLine = true,
                shape = RoundedCornerShape(4.dp)
            )

            Text(
                text = builtinDisplayPath(navigationStack),
                style = MaterialTheme.typography.caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(fileItems, key = { it.id }) { entry ->
                    BuiltinFileItemRow(
                        label = entry.label,
                        isDirectory = entry.isDirectory,
                        iconName = entry.iconName,
                        selected = selectedUris.containsKey(entry.id),
                        multiSelect = multiSelect && !entry.isDirectory,
                        onClick = {
                            when (entry) {
                                is BuiltinBrowserEntry.Directory -> {
                                    navigationStack = navigationStack + entry.location
                                }

                                is BuiltinBrowserEntry.FileItem -> {
                                    if (multiSelect) {
                                        selectedUris = if (selectedUris.containsKey(entry.id)) {
                                            selectedUris - entry.id
                                        } else {
                                            selectedUris + (entry.id to entry.uri)
                                        }
                                    } else {
                                        onPicked(entry.uri)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (fileItems.isEmpty()) {
                Text(
                    if (currentLocation is BuiltinBrowserLocation.Root) "无可用目录" else "无可用文件",
                    style = MaterialTheme.typography.body2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (multiSelect) {
                    TextButton(
                        onClick = {
                            val picked = selectedUris.values.toList()
                            if (picked.isNotEmpty()) {
                                onPickedMultiple(picked)
                            }
                        },
                        enabled = selectedUris.isNotEmpty()
                    ) {
                        Text("确认选择")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
            }
        }
    }
}

@Composable
fun BuiltinGalleryPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onPicked: (Uri) -> Unit
) {
    val context = LocalContext.current
    val requiredPermission = remember { galleryReadPermission() }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    var selectedAlbumId by remember { mutableStateOf<String?>(null) }
    var albumExpanded by remember { mutableStateOf(false) }

    val allImages by produceState<List<BuiltinGalleryItem>>(initialValue = emptyList(), hasPermission) {
        value = if (hasPermission) {
            withContext(Dispatchers.IO) { queryGalleryImages(context) }
        } else {
            emptyList()
        }
    }

    val albums = remember(allImages) {
        linkedMapOf<String?, String>().apply {
            put(null, "全部相册")
            allImages.forEach { item ->
                if (!containsKey(item.bucketId)) {
                    put(item.bucketId, item.bucketName.ifBlank { "未命名相册" })
                }
            }
        }
    }

    val filtered = remember(allImages, selectedAlbumId) {
        if (selectedAlbumId == null) allImages
        else allImages.filter { it.bucketId == selectedAlbumId }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        KigttsFontScaleProvider {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            Text(title, style = MaterialTheme.typography.h6)

            if (!hasPermission) {
                Text("需要图库读取权限")
                Button(onClick = { permissionLauncher.launch(requiredPermission) }) {
                    Text("授予权限")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            } else {
                Box {
                    OutlinedButton(onClick = { albumExpanded = true }) {
                        Text(albums[selectedAlbumId] ?: "全部相册")
                    }
                    BuiltinAnimatedDropdownMenu(
                        expanded = albumExpanded,
                        onDismissRequest = { albumExpanded = false }
                    ) {
                        albums.forEach { (albumId, name) ->
                            DropdownMenuItem(
                                onClick = {
                                    albumExpanded = false
                                    selectedAlbumId = albumId
                                }
                            ) {
                                Text(name)
                            }
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 96.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.uri.toString() }) { item ->
                        BuiltinGalleryGridItem(
                            item = item,
                            onClick = { onPicked(item.uri) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun BuiltinFlatIconButton(
    onClick: () -> Unit,
    iconName: String,
    contentDescription: String,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        BuiltinMsIcon(
            name = iconName,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = if (enabled) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun BuiltinFileItemRow(
    label: String,
    isDirectory: Boolean,
    iconName: String,
    selected: Boolean = false,
    multiSelect: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (multiSelect) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        }
        BuiltinMsIcon(
            name = iconName,
            contentDescription = if (isDirectory) "文件夹" else "文件",
            modifier = Modifier.size(22.dp)
        )
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BuiltinGalleryGridItem(
    item: BuiltinGalleryItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(item.uri, Size(300, 300), null)
                } else {
                    context.contentResolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("加载中", style = MaterialTheme.typography.caption)
        }
    }
}

private fun builtinFileRoots(context: Context): List<BuiltinFileRoot> {
    val roots = linkedMapOf<String, BuiltinFileRoot>()

    fun add(file: File?, label: String, iconName: String) {
        if (file == null || !file.exists()) return
        val key = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
        if (roots.containsKey(key)) return
        roots[key] = BuiltinFileRoot(dir = file, label = label, iconName = iconName)
    }

    add(runCatching { Environment.getExternalStorageDirectory() }.getOrNull(), "内部存储", "storage")
    add(
        runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) }.getOrNull(),
        "下载",
        "download"
    )
    add(
        runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) }.getOrNull(),
        "文档",
        "description"
    )
    add(
        runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) }.getOrNull(),
        "图片",
        "image"
    )
    add(
        runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) }.getOrNull(),
        "音乐",
        "music_note"
    )
    add(
        runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES) }.getOrNull(),
        "铃声",
        "notifications"
    )
    add(
        runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS) }.getOrNull(),
        "通知音",
        "notifications_active"
    )
    add(
        runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS) }.getOrNull(),
        "闹钟",
        "alarm"
    )
    add(context.getExternalFilesDir(null), "应用外部文件", "folder_open")
    add(context.filesDir, "应用内部文件", "folder")

    return roots.values.toList()
}

private val BuiltinAudioFileExtensions = setOf(
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

private val BuiltinImageFileExtensions = setOf(
    "jpg",
    "jpeg",
    "png",
    "webp",
    "bmp",
    "gif",
    "heic",
    "heif"
)

private val BuiltinArchiveFileExtensions = setOf(
    "zip",
    "json",
    "kigvpk",
    "kigtpk",
    "kigspk"
)

private fun builtinReadPermissionForExtensions(allowedExtensions: Set<String>): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return when {
            allowedExtensions.any { it in BuiltinAudioFileExtensions } -> Manifest.permission.READ_MEDIA_AUDIO
            allowedExtensions.any { it in BuiltinImageFileExtensions } -> Manifest.permission.READ_MEDIA_IMAGES
            else -> null
        }
    } else {
        return Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun builtinItemIconName(name: String, isDirectory: Boolean): String {
    if (isDirectory) return "folder"
    return when (builtinFileExtension(name)) {
        in BuiltinAudioFileExtensions -> "audio_file"
        in BuiltinImageFileExtensions -> "image"
        in BuiltinArchiveFileExtensions -> "folder_zip"
        else -> "description"
    }
}

private fun builtinFileExtension(name: String?): String {
    val fileName = name?.trim().orEmpty()
    if (fileName.isBlank()) return ""
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex <= 0 || dotIndex >= fileName.lastIndex) return ""
    return fileName.substring(dotIndex + 1).lowercase(Locale.US)
}

private fun builtinDisplayPath(navigationStack: List<BuiltinBrowserLocation>): String {
    if (navigationStack.size <= 1) return "根目录"
    return when (val rootLocation = navigationStack.getOrNull(1)) {
        is BuiltinBrowserLocation.FileDirectory -> {
            buildList {
                add(rootLocation.root.label)
                navigationStack.drop(2).forEach { location ->
                    if (location is BuiltinBrowserLocation.FileDirectory) {
                        add(location.dir.name)
                    }
                }
            }.joinToString("/")
        }

        is BuiltinBrowserLocation.SafDirectory -> {
            buildList {
                add(rootLocation.root.label)
                navigationStack.drop(2).forEach { location ->
                    if (location is BuiltinBrowserLocation.SafDirectory) {
                        add(location.displayName)
                    }
                }
            }.joinToString("/")
        }

        else -> "根目录"
    }
}

private fun builtinDisplayNameForSegment(segment: String): String {
    return when (segment.trim().lowercase(Locale.US)) {
        "download", "downloads" -> "下载"
        "documents" -> "文档"
        "pictures" -> "图片"
        "music" -> "音乐"
        "movies" -> "视频"
        "dcim" -> "相机"
        "ringtones" -> "铃声"
        "notifications" -> "通知音"
        "alarms" -> "闹钟"
        "android" -> "Android"
        else -> segment
    }
}

private fun builtinRootIconNameForSegment(segment: String): String {
    return when (segment.trim().lowercase(Locale.US)) {
        "内部存储" -> "storage"
        "下载" -> "download"
        "文档" -> "description"
        "图片", "相机" -> "image"
        "音乐" -> "music_note"
        "铃声" -> "notifications"
        "通知音" -> "notifications_active"
        "闹钟" -> "alarm"
        else -> "folder_open"
    }
}

private fun builtinLabelForTreeDocumentId(documentId: String): String {
    val parts = documentId.split(':', limit = 2)
    val volumeLabel = if (parts.firstOrNull().orEmpty().equals("primary", ignoreCase = true)) {
        "内部存储"
    } else {
        parts.firstOrNull().orEmpty().ifBlank { "已授权目录" }
    }
    val relativePath = parts.getOrNull(1).orEmpty().trim('/')
    if (relativePath.isBlank()) return volumeLabel
    val segments = relativePath
        .split('/')
        .filter { it.isNotBlank() }
        .map(::builtinDisplayNameForSegment)
    return (listOf(volumeLabel) + segments).joinToString("/")
}

private fun createBuiltinSafRoot(context: Context, treeUri: Uri): BuiltinSafRoot? {
    val document = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: return null
    if (!document.exists() || !document.canRead()) return null
    val baseLabel = runCatching {
        builtinLabelForTreeDocumentId(DocumentsContract.getTreeDocumentId(treeUri))
    }.getOrElse {
        document.name?.takeIf { name -> name.isNotBlank() } ?: "已授权目录"
    }
    val label = if (baseLabel.endsWith("（授权）")) baseLabel else "$baseLabel（授权）"
    return BuiltinSafRoot(
        treeUri = treeUri,
        label = label,
        iconName = builtinRootIconNameForSegment(baseLabel.substringAfterLast('/')),
        document = document
    )
}

private fun loadBuiltinSafRoots(context: Context): List<BuiltinSafRoot> {
    return context.contentResolver.persistedUriPermissions
        .filter { it.isReadPermission }
        .mapNotNull { createBuiltinSafRoot(context, it.uri) }
        .distinctBy { it.treeUri.toString() }
        .sortedBy { it.label }
}

private fun loadBuiltinEntries(
    location: BuiltinBrowserLocation,
    fileRoots: List<BuiltinFileRoot>,
    safRoots: List<BuiltinSafRoot>,
    query: String,
    sortOption: BuiltinFileSortOption,
    allowedExtensions: Set<String>
): List<BuiltinBrowserEntry> {
    val q = query.trim().lowercase(Locale.US)
    return when (location) {
        BuiltinBrowserLocation.Root -> buildList {
            fileRoots.forEach { root ->
                if (q.isEmpty() || root.label.lowercase(Locale.US).contains(q)) {
                    add(
                        BuiltinBrowserEntry.Directory(
                            id = "file-root:${root.dir.absolutePath}",
                            label = root.label,
                            iconName = root.iconName,
                            location = BuiltinBrowserLocation.FileDirectory(root = root, dir = root.dir)
                        )
                    )
                }
            }
            safRoots.forEach { root ->
                if (q.isEmpty() || root.label.lowercase(Locale.US).contains(q)) {
                    add(
                        BuiltinBrowserEntry.Directory(
                            id = "saf-root:${root.treeUri}",
                            label = root.label,
                            iconName = root.iconName,
                            location = BuiltinBrowserLocation.SafDirectory(
                                root = root,
                                document = root.document,
                                displayName = root.label
                            )
                        )
                    )
                }
            }
        }

        is BuiltinBrowserLocation.FileDirectory -> {
            val list = runCatching { location.dir.listFiles()?.toList().orEmpty() }.getOrDefault(emptyList())
            val filtered = list.filter { file ->
                if (file.isDirectory) {
                    q.isEmpty() || file.name.lowercase(Locale.US).contains(q)
                } else {
                    val nameHit = q.isEmpty() || file.name.lowercase(Locale.US).contains(q)
                    val extHit = allowedExtensions.isEmpty() || allowedExtensions.contains(file.extension.lowercase(Locale.US))
                    nameHit && extHit
                }
            }
            val comparator = when (sortOption) {
                BuiltinFileSortOption.NameAsc -> compareBy<File> { it.name.lowercase(Locale.US) }
                BuiltinFileSortOption.NameDesc -> compareByDescending<File> { it.name.lowercase(Locale.US) }
                BuiltinFileSortOption.TimeAsc -> compareBy<File> { it.lastModified() }
                BuiltinFileSortOption.TimeDesc -> compareByDescending<File> { it.lastModified() }
            }
            filtered.sortedWith(compareBy<File> { !it.isDirectory }.then(comparator)).map { file ->
                if (file.isDirectory) {
                    BuiltinBrowserEntry.Directory(
                        id = "file-dir:${file.absolutePath}",
                        label = file.name,
                        iconName = builtinItemIconName(file.name, true),
                        location = BuiltinBrowserLocation.FileDirectory(root = location.root, dir = file)
                    )
                } else {
                    BuiltinBrowserEntry.FileItem(
                        id = Uri.fromFile(file).toString(),
                        label = file.name,
                        iconName = builtinItemIconName(file.name, false),
                        uri = Uri.fromFile(file)
                    )
                }
            }
        }

        is BuiltinBrowserLocation.SafDirectory -> {
            val list = runCatching { location.document.listFiles().toList() }.getOrDefault(emptyList())
            val filtered = list.filter { document ->
                val name = document.name?.ifBlank { "未命名项目" } ?: "未命名项目"
                if (document.isDirectory) {
                    q.isEmpty() || name.lowercase(Locale.US).contains(q)
                } else {
                    val nameHit = q.isEmpty() || name.lowercase(Locale.US).contains(q)
                    val extHit = allowedExtensions.isEmpty() || allowedExtensions.contains(builtinFileExtension(name))
                    nameHit && extHit
                }
            }
            val comparator = when (sortOption) {
                BuiltinFileSortOption.NameAsc -> compareBy<DocumentFile> {
                    it.name?.lowercase(Locale.US).orEmpty()
                }

                BuiltinFileSortOption.NameDesc -> compareByDescending<DocumentFile> {
                    it.name?.lowercase(Locale.US).orEmpty()
                }

                BuiltinFileSortOption.TimeAsc -> compareBy<DocumentFile> { it.lastModified() }
                BuiltinFileSortOption.TimeDesc -> compareByDescending<DocumentFile> { it.lastModified() }
            }
            filtered.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.then(comparator)).map { document ->
                val name = document.name?.ifBlank { "未命名项目" } ?: "未命名项目"
                if (document.isDirectory) {
                    BuiltinBrowserEntry.Directory(
                        id = "saf-dir:${document.uri}",
                        label = name,
                        iconName = builtinItemIconName(name, true),
                        location = BuiltinBrowserLocation.SafDirectory(
                            root = location.root,
                            document = document,
                            displayName = name
                        )
                    )
                } else {
                    BuiltinBrowserEntry.FileItem(
                        id = document.uri.toString(),
                        label = name,
                        iconName = builtinItemIconName(name, false),
                        uri = document.uri
                    )
                }
            }
        }
    }
}

private fun galleryReadPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun queryGalleryImages(context: Context): List<BuiltinGalleryItem> {
    val resolver = context.contentResolver
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED
    )
    val result = mutableListOf<BuiltinGalleryItem>()
    resolver.query(
        uri,
        projection,
        null,
        null,
        "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val bucketId = cursor.getString(bucketIdCol) ?: ""
            val bucketName = cursor.getString(bucketNameCol) ?: "未命名相册"
            val dateTaken = cursor.getLong(dateTakenCol)
            val dateAdded = cursor.getLong(dateAddedCol) * 1000L
            val dateMs = if (dateTaken > 0L) dateTaken else dateAdded
            val contentUri = Uri.withAppendedPath(uri, id.toString())
            result += BuiltinGalleryItem(
                uri = contentUri,
                bucketId = bucketId,
                bucketName = bucketName,
                dateMs = dateMs
            )
        }
    }
    return result.sortedByDescending { it.dateMs }
}
