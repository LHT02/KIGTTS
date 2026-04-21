package com.lhtstudio.kigtts.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
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
        Box(
            modifier = Modifier
                .padding(8.dp)
                .graphicsLayer {
                    alpha = menuAlpha
                    scaleX = menuScale
                    scaleY = menuScale
                    transformOrigin = TransformOrigin(1f, 0f)
                    clip = false
                },
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

@Composable
fun BuiltinFilePickerDialog(
    title: String,
    allowedExtensions: Set<String> = emptySet(),
    multiSelect: Boolean = false,
    onDismiss: () -> Unit,
    onPicked: (Uri) -> Unit,
    onPickedMultiple: (List<Uri>) -> Unit = {}
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(BuiltinFileSortOption.TimeDesc) }
    var sortExpanded by remember { mutableStateOf(false) }

    val roots = remember { builtinFileRoots(context) }
    var currentDir by remember(roots) { mutableStateOf(roots.firstOrNull()) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }

    val normalizedExt = remember(allowedExtensions) {
        allowedExtensions.map { it.lowercase(Locale.US).trim('.') }.filter { it.isNotBlank() }.toSet()
    }
    val readPermission = remember(normalizedExt) { builtinReadPermissionForExtensions(normalizedExt) }
    var hasReadPermission by remember(readPermission) {
        mutableStateOf(
            readPermission == null ||
                    ContextCompat.checkSelfPermission(context, readPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasReadPermission = granted
    }

    val fileItems by produceState<List<File>>(
        initialValue = emptyList(),
        currentDir,
        search,
        sortOption,
        normalizedExt,
        hasReadPermission
    ) {
        value = withContext(Dispatchers.IO) {
            loadBuiltinFiles(
                directory = currentDir,
                query = search,
                sortOption = sortOption,
                allowedExtensions = normalizedExt
            )
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.h6)

            if (readPermission != null && !hasReadPermission) {
                Text("需要音频文件读取权限，否则系统媒体目录可能无法显示。", style = MaterialTheme.typography.body2)
                Button(onClick = { permissionLauncher.launch(readPermission) }) {
                    Text("授予音频读取权限")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val parent = currentDir?.parentFile
                        if (parent != null && parent.exists() && parent.canRead()) {
                            currentDir = parent
                        } else {
                            currentDir = null
                        }
                    },
                    enabled = currentDir != null
                ) { Text("上一级") }

                Box {
                    OutlinedButton(onClick = { sortExpanded = true }) { Text(sortOption.label) }
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

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索文件/文件夹") },
                singleLine = true,
                shape = RoundedCornerShape(4.dp)
            )

            Text(
                text = currentDir?.absolutePath ?: "根目录",
                style = MaterialTheme.typography.caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (currentDir == null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(roots) { root ->
                        BuiltinFileItemRow(
                            label = root.absolutePath,
                            isDirectory = true,
                            onClick = { currentDir = root }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(fileItems, key = { it.absolutePath }) { file ->
                        BuiltinFileItemRow(
                            label = file.name,
                            isDirectory = file.isDirectory,
                            selected = selectedPaths.contains(file.absolutePath),
                            multiSelect = multiSelect && !file.isDirectory,
                            onClick = {
                                if (file.isDirectory) {
                                    currentDir = file
                                } else if (multiSelect) {
                                    selectedPaths = if (selectedPaths.contains(file.absolutePath)) {
                                        selectedPaths - file.absolutePath
                                    } else {
                                        selectedPaths + file.absolutePath
                                    }
                                } else {
                                    onPicked(Uri.fromFile(file))
                                }
                            }
                        )
                    }
                }
            }

            if (currentDir != null && fileItems.isEmpty()) {
                Text("无可用文件", style = MaterialTheme.typography.body2)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
                ) {
                if (multiSelect) {
                    TextButton(
                        onClick = {
                            val picked = selectedPaths
                                .map(::File)
                                .filter { it.exists() && it.isFile }
                                .map(Uri::fromFile)
                            if (picked.isNotEmpty()) {
                                onPickedMultiple(picked)
                            }
                        },
                        enabled = selectedPaths.isNotEmpty()
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

@Composable
private fun BuiltinFileItemRow(
    label: String,
    isDirectory: Boolean,
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
        Text(if (isDirectory) "📁" else "📄")
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

private fun builtinFileRoots(context: Context): List<File> {
    val roots = linkedSetOf<File>()
    roots += context.filesDir
    context.getExternalFilesDir(null)?.let { roots += it }
    runCatching { Environment.getExternalStorageDirectory() }.getOrNull()?.let { roots += it }
    runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) }
        .getOrNull()?.let { roots += it }
    runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES) }
        .getOrNull()?.let { roots += it }
    runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS) }
        .getOrNull()?.let { roots += it }
    runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS) }
        .getOrNull()?.let { roots += it }
    runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) }
        .getOrNull()?.let { roots += it }
    runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) }
        .getOrNull()?.let { roots += it }
    runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) }
        .getOrNull()?.let { roots += it }
    return roots.filter { it.exists() && it.canRead() }
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

private fun builtinReadPermissionForExtensions(allowedExtensions: Set<String>): String? {
    val needsAudioAccess = allowedExtensions.any { it in BuiltinAudioFileExtensions }
    if (!needsAudioAccess) return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun loadBuiltinFiles(
    directory: File?,
    query: String,
    sortOption: BuiltinFileSortOption,
    allowedExtensions: Set<String>
): List<File> {
    val dir = directory ?: return emptyList()
    val list = runCatching { dir.listFiles()?.toList().orEmpty() }.getOrDefault(emptyList())
    val q = query.trim().lowercase(Locale.US)
    val filtered = list.filter { file ->
        if (file.isDirectory) {
            q.isEmpty() || file.name.lowercase(Locale.US).contains(q)
        } else {
            val nameHit = q.isEmpty() || file.name.lowercase(Locale.US).contains(q)
            val extHit =
                allowedExtensions.isEmpty() || allowedExtensions.contains(file.extension.lowercase(Locale.US))
            nameHit && extHit
        }
    }

    val comparator = when (sortOption) {
        BuiltinFileSortOption.NameAsc -> compareBy<File> { it.name.lowercase(Locale.US) }
        BuiltinFileSortOption.NameDesc -> compareByDescending<File> { it.name.lowercase(Locale.US) }
        BuiltinFileSortOption.TimeAsc -> compareBy<File> { it.lastModified() }
        BuiltinFileSortOption.TimeDesc -> compareByDescending<File> { it.lastModified() }
    }

    return filtered.sortedWith(compareBy<File> { !it.isDirectory }.then(comparator))
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
