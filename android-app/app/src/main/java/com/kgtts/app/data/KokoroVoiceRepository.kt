package com.lhtstudio.kigtts.app.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.lhtstudio.kigtts.app.util.AppLogger
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

const val KOKORO_VOICE_NAME = "__kokoro__"
private const val KOKORO_VOICE_DIR_NAME = "__kokoro_voice__"

fun isKokoroVoiceDir(dir: File?): Boolean {
    return dir?.name == KOKORO_VOICE_DIR_NAME
}

data class KokoroVoiceStatus(
    val installed: Boolean,
    val name: String = "Kokoro",
    val version: String = "",
    val installedAtMs: Long = 0L,
    val rootDir: File? = null
)

class KokoroVoiceRepository(private val context: Context) {
    private val root = File(context.filesDir, "models/kokoro")
    private val installDir = File(root, KOKORO_VOICE_DIR_NAME)
    private val cacheRoot = File(context.cacheDir, "kokoro_voice")

    init {
        root.mkdirs()
        cacheRoot.mkdirs()
    }

    fun voiceDir(): File = installDir

    fun status(): KokoroVoiceStatus {
        if (!installDir.isDirectory) return KokoroVoiceStatus(installed = false)
        val base = findKokoroBaseDir(installDir) ?: return KokoroVoiceStatus(installed = false)
        val manifest = readManifest(base)
        return KokoroVoiceStatus(
            installed = true,
            name = manifest?.optString("name")?.takeIf { it.isNotBlank() } ?: "Kokoro",
            version = manifest?.optString("version")?.takeIf { it.isNotBlank() } ?: "multi-lang v1.1 int8",
            installedAtMs = manifest?.optLong("installedAtMs", installDir.lastModified()) ?: installDir.lastModified(),
            rootDir = installDir
        )
    }

    fun delete() {
        installDir.deleteRecursively()
    }

    fun installFromUri(
        uri: Uri,
        resolver: ContentResolver,
        onProgress: (RecognitionResourceProgress) -> Unit
    ): KokoroVoiceStatus {
        cacheRoot.mkdirs()
        val displayName = displayName(uri, resolver) ?: "kokoro-voice.zip"
        val temp = File(cacheRoot, "local-${System.currentTimeMillis()}-${safeFileName(displayName)}")
        try {
            onProgress(RecognitionResourceProgress("复制 Kokoro 语音包", -1f))
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output -> input.copyTo(output) }
            } ?: throw IOException("无法读取 Kokoro 语音包")
            return installArchiveFile(temp, onProgress)
        } finally {
            temp.delete()
        }
    }

    fun downloadAndInstall(
        sourceUrl: String,
        onProgress: (RecognitionResourceProgress) -> Unit
    ): KokoroVoiceStatus {
        val source = parseRepoSource(sourceUrl)
        cacheRoot.mkdirs()
        val importDir = File(cacheRoot, ".download-${System.currentTimeMillis()}")
        if (importDir.exists()) importDir.deleteRecursively()
        importDir.mkdirs()
        try {
            val remoteFiles = queryRemoteFiles(source)
            if (remoteFiles.isEmpty()) throw IOException("下载源没有可安装文件")
            val totalBytes = remoteFiles.sumOf { it.size.coerceAtLeast(0L) }.takeIf { it > 0L } ?: -1L
            var finishedBytes = 0L
            remoteFiles.forEach { remote ->
                val out = entryOutputFile(importDir, remote.path)
                out.parentFile?.mkdirs()
                downloadToFile(
                    urlString = source.resolveFileUrl(remote.path),
                    outFile = out,
                    stage = "下载 Kokoro：${remote.path.substringAfterLast('/')}",
                    totalBytes = totalBytes,
                    finishedBefore = finishedBytes,
                    onProgress = onProgress
                )
                finishedBytes += remote.size.coerceAtLeast(out.length())
            }
            return installDirectory(importDir, onProgress)
        } catch (e: Exception) {
            importDir.deleteRecursively()
            AppLogger.e("Kokoro voice download/install failed", e)
            throw e
        }
    }

    private fun installArchiveFile(
        archive: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ): KokoroVoiceStatus {
        val importDir = File(cacheRoot, ".import-${System.currentTimeMillis()}")
        if (importDir.exists()) importDir.deleteRecursively()
        importDir.mkdirs()
        return try {
            extractArchive(archive, importDir, onProgress)
            installDirectory(importDir, onProgress)
        } catch (e: Exception) {
            importDir.deleteRecursively()
            throw e
        }
    }

    private fun installDirectory(
        importDir: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ): KokoroVoiceStatus {
        onProgress(RecognitionResourceProgress("验证 Kokoro 语音包", -1f))
        val base = findKokoroBaseDir(importDir)
            ?: throw IOException("无效 Kokoro 语音包：缺少 model/model.int8、voices.bin、tokens.txt 或 espeak-ng-data")
        writeManifest(base)
        val tmpTarget = File(root, ".kokoro-install-${System.currentTimeMillis()}")
        if (tmpTarget.exists()) tmpTarget.deleteRecursively()
        tmpTarget.parentFile?.mkdirs()
        val moved = base.renameTo(tmpTarget)
        if (!moved) {
            base.copyRecursively(tmpTarget, overwrite = true)
        }
        if (installDir.exists()) installDir.deleteRecursively()
        val finalMoved = tmpTarget.renameTo(installDir)
        if (!finalMoved) {
            tmpTarget.copyRecursively(installDir, overwrite = true)
            tmpTarget.deleteRecursively()
        }
        if (importDir.exists() && importDir.absolutePath != installDir.absolutePath) {
            importDir.deleteRecursively()
        }
        onProgress(RecognitionResourceProgress("安装完成", 1f))
        return status()
    }

    private fun queryRemoteFiles(source: RepoSource): List<RemoteFile> {
        val jsonText = readTextUrl(source.treeApiUrl())
        val array = if (source.platform == RepoPlatform.MODELSCOPE) {
            JSONObject(jsonText)
                .optJSONObject("Data")
                ?.optJSONArray("Files")
                ?: JSONArray()
        } else {
            JSONArray(jsonText)
        }
        val result = mutableListOf<RemoteFile>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val isFile = if (source.platform == RepoPlatform.MODELSCOPE) {
                obj.optString("Type") == "blob"
            } else {
                obj.optString("type") == "file"
            }
            if (!isFile) continue
            val path = if (source.platform == RepoPlatform.MODELSCOPE) {
                obj.optString("Path")
            } else {
                obj.optString("path")
            }.trim().replace('\\', '/').removePrefix("/")
            if (path.isBlank()) continue
            val lower = path.lowercase(Locale.US)
            if (
                lower == ".gitattributes" ||
                lower == "readme.md" ||
                lower == "license" ||
                lower.endsWith(".md")
            ) {
                continue
            }
            val size = if (source.platform == RepoPlatform.MODELSCOPE) {
                obj.optLong("Size", -1L)
            } else {
                obj.optLong("size", -1L)
            }
            result += RemoteFile(path = path, size = size)
        }
        return result.sortedBy { it.path }
    }

    private fun downloadToFile(
        urlString: String,
        outFile: File,
        stage: String,
        totalBytes: Long,
        finishedBefore: Long,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        val connection = openConnection(urlString)
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("下载失败：HTTP $code")
            var copied = 0L
            var lastEmitAt = 0L
            connection.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastEmitAt >= 160L || read < buffer.size) {
                            lastEmitAt = now
                            onProgress(
                                RecognitionResourceProgress(
                                    stage = stage,
                                    fraction = if (totalBytes > 0L) {
                                        ((finishedBefore + copied).toFloat() / totalBytes).coerceIn(0f, 1f)
                                    } else {
                                        -1f
                                    }
                                )
                            )
                        }
                    }
                }
            }
            if (outFile.length() <= 0L) throw IOException("下载结果为空：$urlString")
        } finally {
            connection.disconnect()
        }
    }

    private fun readTextUrl(urlString: String): String {
        val connection = openConnection(urlString)
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("请求下载源失败：HTTP $code")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(urlString: String): HttpURLConnection {
        return (URL(urlString).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15000
            readTimeout = 45000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "KIGTTS-Android")
        }
    }

    private fun extractArchive(
        archive: File,
        outDir: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        val lower = archive.name.lowercase(Locale.US)
        when {
            lower.endsWith(".zip") -> extractZip(archive, outDir, onProgress)
            lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") -> extractTarBz2(archive, outDir, onProgress)
            lower.endsWith(".tar") -> extractTar(archive, outDir, onProgress)
            else -> runCatching {
                extractZip(archive, outDir, onProgress)
            }.getOrElse {
                outDir.deleteRecursively()
                outDir.mkdirs()
                extractTarBz2(archive, outDir, onProgress)
            }
        }
    }

    private fun extractZip(
        archive: File,
        outDir: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        val total = archive.length().coerceAtLeast(1L)
        var copied = 0L
        ZipInputStream(FileInputStream(archive)).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (entry != null) {
                val outFile = entryOutputFile(outDir, entry)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output ->
                        while (true) {
                            val read = zis.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            onProgress(RecognitionResourceProgress("解压 Kokoro 语音包", (copied.toFloat() / total).coerceIn(0f, 1f)))
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun extractTarBz2(
        archive: File,
        outDir: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        BufferedInputStream(FileInputStream(archive)).use { input ->
            BZip2CompressorInputStream(input).use { bz ->
                extractTarStream(TarArchiveInputStream(bz), outDir, archive.length().coerceAtLeast(1L), onProgress)
            }
        }
    }

    private fun extractTar(
        archive: File,
        outDir: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        FileInputStream(archive).use { input ->
            extractTarStream(TarArchiveInputStream(input), outDir, archive.length().coerceAtLeast(1L), onProgress)
        }
    }

    private fun extractTarStream(
        tar: TarArchiveInputStream,
        outDir: File,
        total: Long,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        tar.use { tis ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var copied = 0L
            var entry = tis.nextTarEntry
            while (entry != null) {
                val outFile = entryOutputFile(outDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output ->
                        while (true) {
                            val read = tis.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            onProgress(RecognitionResourceProgress("解压 Kokoro 语音包", (copied.toFloat() / total).coerceIn(0f, 1f)))
                        }
                    }
                }
                entry = tis.nextTarEntry
            }
        }
    }

    private fun findKokoroBaseDir(rootDir: File): File? {
        val candidates = rootDir.walkTopDown()
            .filter { it.isDirectory }
            .toList()
            .sortedBy { it.absolutePath.length }
        return candidates.firstOrNull { dir ->
            (File(dir, "model.int8.onnx").isFile || File(dir, "model.onnx").isFile) &&
                File(dir, "voices.bin").isFile &&
                File(dir, "tokens.txt").isFile &&
                File(dir, "espeak-ng-data").isDirectory &&
                File(dir, "lexicon-zh.txt").isFile &&
                File(dir, "lexicon-us-en.txt").isFile
        }
    }

    private fun writeManifest(baseDir: File) {
        val manifest = JSONObject().apply {
            put("type", "kigtts-kokoro-voice")
            put("name", "Kokoro")
            put("version", "multi-lang v1.1 int8")
            put("installedAtMs", System.currentTimeMillis())
            put("sampleRate", 24000)
            put("speakers", 103)
            put("model", if (File(baseDir, "model.int8.onnx").isFile) "model.int8.onnx" else "model.onnx")
        }
        File(baseDir, MANIFEST_FILE_NAME).writeText(manifest.toString(2), Charsets.UTF_8)
    }

    private fun readManifest(baseDir: File): JSONObject? {
        val file = File(baseDir, MANIFEST_FILE_NAME)
        return runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    private fun parseRepoSource(rawUrl: String): RepoSource {
        val normalized = rawUrl.trim().ifBlank { UserPrefs.DEFAULT_KOKORO_HFMIRROR_URL }
        val url = URL(normalized)
        val segments = url.path.trim('/').split('/').filter { it.isNotBlank() }
        val isModelScope = url.host.endsWith("modelscope.cn", ignoreCase = true)
        val repoSegments = if (isModelScope && segments.firstOrNull() == "models") {
            segments.drop(1)
        } else {
            segments
        }
        if (repoSegments.size < 2) throw IOException("下载源链接需要指向模型仓库")
        val repoId = "${repoSegments[0]}/${repoSegments[1]}"
        val resolveIndex = segments.indexOf("resolve")
        val treeIndex = segments.indexOf("tree")
        val revision = when {
            resolveIndex >= 0 && segments.size > resolveIndex + 1 -> segments[resolveIndex + 1]
            treeIndex >= 0 && segments.size > treeIndex + 1 -> segments[treeIndex + 1]
            isModelScope -> "master"
            else -> "main"
        }
        return RepoSource(
            origin = "${url.protocol}://${url.host}",
            repoId = repoId,
            revision = revision,
            platform = if (isModelScope) RepoPlatform.MODELSCOPE else RepoPlatform.HUGGINGFACE
        )
    }

    private fun entryOutputFile(outDir: File, entry: ZipEntry): File = entryOutputFile(outDir, entry.name)

    private fun entryOutputFile(outDir: File, rawName: String): File {
        val normalized = rawName.replace('\\', '/').trim().removePrefix("/")
        if (normalized.isBlank()) throw IOException("无效压缩包条目：空路径")
        val outPath = File(outDir, normalized)
        val canonicalRoot = outDir.canonicalFile
        val canonicalOut = outPath.canonicalFile
        if (
            canonicalOut != canonicalRoot &&
            !canonicalOut.path.startsWith("${canonicalRoot.path}${File.separator}")
        ) {
            throw IOException("无效压缩包条目：$rawName")
        }
        return canonicalOut
    }

    private fun displayName(uri: Uri, resolver: ContentResolver): String? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        val cursor: Cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx < 0) return null
            return it.getString(idx)
        }
    }

    private fun safeFileName(name: String): String {
        return name
            .trim()
            .ifBlank { "kokoro-voice.zip" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim('.')
            .ifBlank { "kokoro-voice.zip" }
    }

    private data class RemoteFile(
        val path: String,
        val size: Long
    )

    private data class RepoSource(
        val origin: String,
        val repoId: String,
        val revision: String,
        val platform: RepoPlatform
    ) {
        fun treeApiUrl(): String {
            return when (platform) {
                RepoPlatform.MODELSCOPE ->
                    "https://www.modelscope.cn/api/v1/models/$repoId/repo/files?Revision=$revision&Recursive=true"
                RepoPlatform.HUGGINGFACE ->
                    "$origin/api/models/$repoId/tree/$revision?recursive=1"
            }
        }

        fun resolveFileUrl(path: String): String {
            return when (platform) {
                RepoPlatform.MODELSCOPE ->
                    "https://modelscope.cn/models/$repoId/resolve/$revision/${encodePath(path)}"
                RepoPlatform.HUGGINGFACE ->
                    "$origin/$repoId/resolve/$revision/${encodePath(path)}"
            }
        }
    }

    private enum class RepoPlatform {
        HUGGINGFACE,
        MODELSCOPE
    }

    companion object {
        private const val MANIFEST_FILE_NAME = "kigtts_kokoro_voice.json"

        private fun encodePath(path: String): String {
            return path.split('/').joinToString("/") { segment ->
                URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            }
        }
    }
}
