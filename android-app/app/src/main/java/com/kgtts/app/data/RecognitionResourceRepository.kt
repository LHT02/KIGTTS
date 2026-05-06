package com.lhtstudio.kigtts.app.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.lhtstudio.kigtts.app.util.AppLogger
import org.apache.commons.compress.MemoryLimitException
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class RecognitionResourceProgress(
    val stage: String,
    val fraction: Float = -1f
)

data class RecognitionResourceStatus(
    val installed: Boolean,
    val name: String = "未安装",
    val version: String = "",
    val installedAtMs: Long = 0L,
    val rootDir: File? = null,
    val asrDir: File? = null
)

class RecognitionResourceRepository(private val context: Context) {
    private val root = File(context.filesDir, "models/recognition_resources")
    private val installRoot = File(root, "installed")
    private val cacheRoot = File(context.cacheDir, "recognition_resources")
    private val activeFile = File(root, ACTIVE_FILE_NAME)

    init {
        root.mkdirs()
        installRoot.mkdirs()
        cacheRoot.mkdirs()
        cleanupStaleImports()
    }

    fun status(): RecognitionResourceStatus {
        val active = readActiveInfo() ?: return RecognitionResourceStatus(installed = false)
        val dir = File(installRoot, active.dirName)
        if (!dir.isDirectory) return RecognitionResourceStatus(installed = false)
        val manifest = readManifestJson(dir)
        val name = active.name.ifBlank {
            manifest?.optString("name")?.takeIf { it.isNotBlank() } ?: "语音识别资源包"
        }
        val version = active.version.ifBlank {
            manifest?.optString("version")?.takeIf { it.isNotBlank() }.orEmpty()
        }
        return RecognitionResourceStatus(
            installed = true,
            name = name,
            version = version,
            installedAtMs = active.installedAtMs,
            rootDir = dir,
            asrDir = resolveAsrDir(dir, manifest)
        )
    }

    fun installedAsrDir(): File? = status().asrDir

    fun installFromUri(
        uri: Uri,
        resolver: ContentResolver,
        onProgress: (RecognitionResourceProgress) -> Unit
    ): RecognitionResourceStatus {
        cacheRoot.mkdirs()
        val displayName = displayName(uri, resolver) ?: "recognition-resources.7z"
        val temp = File(cacheRoot, "local-${System.currentTimeMillis()}-${safeFileName(displayName)}")
        AppLogger.i("Recognition resources local install uri=$uri temp=${temp.absolutePath}")
        try {
            onProgress(RecognitionResourceProgress("复制资源包", -1f))
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("无法读取资源包")
            return installArchiveFile(temp, deleteArchiveOnSuccess = true, onProgress = onProgress)
        } catch (e: Exception) {
            AppLogger.e("Recognition resources local install failed", e)
            throw e
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    fun downloadAndInstall(
        url: String,
        onProgress: (RecognitionResourceProgress) -> Unit
    ): RecognitionResourceStatus {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) throw IOException("下载源链接为空")
        cacheRoot.mkdirs()
        val ext = archiveExtensionFromUrl(normalizedUrl)
        val temp = File(cacheRoot, "download-${System.currentTimeMillis()}.$ext")
        AppLogger.i("Recognition resources download url=$normalizedUrl temp=${temp.absolutePath}")
        var installStarted = false
        try {
            downloadToFile(normalizedUrl, temp, onProgress)
            installStarted = true
            return installArchiveFile(temp, deleteArchiveOnSuccess = true, onProgress = onProgress)
        } catch (e: Exception) {
            AppLogger.e("Recognition resources download/install failed", e)
            throw e
        } finally {
            if (!installStarted && temp.exists()) temp.delete()
        }
    }

    private fun installArchiveFile(
        archive: File,
        deleteArchiveOnSuccess: Boolean,
        onProgress: (RecognitionResourceProgress) -> Unit
    ): RecognitionResourceStatus {
        val importDir = File(root, ".import-${System.currentTimeMillis()}")
        if (importDir.exists()) importDir.deleteRecursively()
        importDir.mkdirs()
        return try {
            extractArchive(archive, importDir, onProgress)
            onProgress(RecognitionResourceProgress("验证资源包", -1f))
            val manifestFile = findManifestFile(importDir)
                ?: throw IOException("无效语音识别资源包：缺少 recognition_resources.json 或 manifest.json")
            val manifestDir = manifestFile.parentFile
                ?: throw IOException("无效语音识别资源包：manifest 路径异常")
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            validateManifest(manifestDir, manifest)

            val installDir = File(installRoot, "resource-${System.currentTimeMillis()}")
            if (installDir.exists()) installDir.deleteRecursively()
            installDir.parentFile?.mkdirs()
            val moved = manifestDir.renameTo(installDir)
            if (!moved) {
                manifestDir.copyRecursively(installDir, overwrite = true)
            }
            if (manifestDir.exists() && manifestDir.absolutePath != installDir.absolutePath) {
                manifestDir.deleteRecursively()
            }
            importDir.deleteRecursively()

            val installedAt = System.currentTimeMillis()
            val name = manifest.optString("name", "语音识别资源包").ifBlank { "语音识别资源包" }
            val version = manifest.optString("version", "")
            writeActiveInfo(
                ActiveInfo(
                    dirName = installDir.name,
                    name = name,
                    version = version,
                    installedAtMs = installedAt
                )
            )
            installRoot.listFiles()
                ?.filter { it.isDirectory && it.name != installDir.name }
                ?.forEach { it.deleteRecursively() }
            if (deleteArchiveOnSuccess && archive.exists()) {
                archive.delete()
            }
            onProgress(RecognitionResourceProgress("安装完成", 1f))
            status()
        } catch (e: Exception) {
            importDir.deleteRecursively()
            throw e
        }
    }

    private fun downloadToFile(
        urlString: String,
        outFile: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15000
            readTimeout = 30000
            requestMethod = "GET"
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("下载失败：HTTP $code")
            }
            val total = connection.contentLengthLong.takeIf { it > 0L } ?: -1L
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
                        if (now - lastEmitAt >= 160L || (total > 0L && copied >= total)) {
                            lastEmitAt = now
                            onProgress(
                                RecognitionResourceProgress(
                                    stage = "下载资源包",
                                    fraction = if (total > 0L) copied.toFloat() / total else -1f
                                )
                            )
                        }
                    }
                }
            }
            if (outFile.length() <= 0L) throw IOException("下载结果为空")
        } finally {
            connection.disconnect()
        }
    }

    private fun extractArchive(
        archive: File,
        outDir: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        try {
            val ext = archive.extension.lowercase(Locale.US)
            when (ext) {
                "zip" -> extractZip(archive, outDir, onProgress)
                "7z" -> extract7z(archive, outDir, onProgress)
                else -> {
                    runCatching {
                        extract7z(archive, outDir, onProgress)
                    }.getOrElse {
                        outDir.deleteRecursively()
                        outDir.mkdirs()
                        extractZip(archive, outDir, onProgress)
                    }
                }
            }
        } catch (e: MemoryLimitException) {
            throw IOException(ANDROID_SAFE_7Z_ERROR, e)
        } catch (e: OutOfMemoryError) {
            throw IOException(ANDROID_SAFE_7Z_ERROR, e)
        }
    }

    private fun extract7z(
        archive: File,
        outDir: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        val total = openSevenZFile(archive).use { seven ->
            var sum = 0L
            var entry = seven.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.size > 0L) sum += entry.size
                entry = seven.nextEntry
            }
            sum.coerceAtLeast(1L)
        }
        var copied = 0L
        var lastEmitAt = 0L
        openSevenZFile(archive).use { seven ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var entry = seven.nextEntry
            while (entry != null) {
                val name = entry.name ?: throw IOException("无效 7z 条目：空路径")
                val outFile = entryOutputFile(outDir, name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output ->
                        var remaining = entry.size
                        while (remaining > 0L) {
                            val read = seven.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            remaining -= read
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastEmitAt >= 160L || copied >= total) {
                                lastEmitAt = now
                                onProgress(
                                    RecognitionResourceProgress(
                                        stage = "解压资源包",
                                        fraction = copied.toFloat() / total
                                    )
                                )
                            }
                        }
                    }
                }
                entry = seven.nextEntry
            }
        }
    }

    private fun extractZip(
        archive: File,
        outDir: File,
        onProgress: (RecognitionResourceProgress) -> Unit
    ) {
        val total = ZipInputStream(FileInputStream(archive)).use { zis ->
            var sum = 0L
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.size > 0L) sum += entry.size
                zis.closeEntry()
                entry = zis.nextEntry
            }
            sum.takeIf { it > 0L } ?: archive.length().coerceAtLeast(1L)
        }
        var copied = 0L
        var lastEmitAt = 0L
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
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastEmitAt >= 160L || copied >= total) {
                                lastEmitAt = now
                                onProgress(
                                    RecognitionResourceProgress(
                                        stage = "解压资源包",
                                        fraction = copied.toFloat() / total
                                    )
                                )
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun openSevenZFile(archive: File): SevenZFile {
        return SevenZFile.builder()
            .setFile(archive)
            .setMaxMemoryLimitKb(SEVEN_Z_MAX_MEMORY_KB)
            .get()
    }

    private fun validateManifest(baseDir: File, manifest: JSONObject) {
        val type = manifest.optString("type", "kigtts-recognition-resources")
        if (type.isNotBlank() && type != "kigtts-recognition-resources") {
            throw IOException("无效语音识别资源包：type=$type")
        }
        val hasAnyOnnx = baseDir.walkTopDown().any { it.isFile && it.extension.equals("onnx", ignoreCase = true) }
        if (!hasAnyOnnx) {
            throw IOException("无效语音识别资源包：未找到 ONNX 模型文件")
        }
    }

    private fun resolveAsrDir(baseDir: File, manifest: JSONObject?): File? {
        val files = manifest?.optJSONObject("files")
        val explicit = listOf(
            files?.optString("asrDir"),
            files?.optString("asr"),
            manifest?.optString("asrDir")
        ).firstOrNull { !it.isNullOrBlank() }
        val explicitDir = explicit?.let { resolveRelativeFile(baseDir, it) }?.takeIf { it.isDirectory }
        if (explicitDir != null && hasAsrModel(explicitDir)) return explicitDir
        return findAsrDirByContent(baseDir)
    }

    private fun findAsrDirByContent(rootDir: File): File? {
        val model = rootDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("onnx", ignoreCase = true) }
            .firstOrNull { file ->
                val name = file.name.lowercase(Locale.US)
                !name.contains("silero") &&
                        !name.contains("vad") &&
                        !name.contains("punct") &&
                        !name.contains("gtcrn") &&
                        !name.contains("dpdfnet")
            }
        return model?.parentFile ?: rootDir.takeIf { hasAsrModel(it) }
    }

    private fun hasAsrModel(dir: File): Boolean {
        if (!dir.exists()) return false
        return dir.walkTopDown().any { file ->
            file.isFile &&
                    file.extension.equals("onnx", ignoreCase = true) &&
                    !file.name.contains("silero", ignoreCase = true) &&
                    !file.name.contains("vad", ignoreCase = true) &&
                    !file.name.contains("gtcrn", ignoreCase = true) &&
                    !file.name.contains("dpdfnet", ignoreCase = true)
        }
    }

    private fun resolveSpeechEnhancementModel(fileName: String): File? {
        val activeDir = activeResourceDir() ?: return null
        val manifest = readManifestJson(activeDir)
        val rel = speechEnhancementRelPath(manifest, fileName)
        val explicit = rel?.let { resolveRelativeFile(activeDir, it) }?.takeIf { it.isFile && it.length() > 0L }
        if (explicit != null) return explicit
        return activeDir.walkTopDown()
            .firstOrNull { it.isFile && it.name.equals(fileName, ignoreCase = true) && it.length() > 0L }
    }

    private fun resolveSileroVadModel(): File? {
        val activeDir = activeResourceDir() ?: return null
        val manifest = readManifestJson(activeDir)
        val files = manifest?.optJSONObject("files")
        val vad = files?.optJSONObject("vad")
        val rel = listOf(
            files?.optString("sileroVad"),
            files?.optString("silero_vad"),
            vad?.optString("silero"),
            manifest?.optString("sileroVad")
        ).firstOrNull { !it.isNullOrBlank() }
        val explicit = rel?.let { resolveRelativeFile(activeDir, it) }?.takeIf { it.isFile && it.length() > 0L }
        if (explicit != null) return explicit
        return activeDir.walkTopDown()
            .firstOrNull { it.isFile && it.name.equals("silero_vad.onnx", ignoreCase = true) && it.length() > 0L }
    }

    private fun speechEnhancementRelPath(manifest: JSONObject?, fileName: String): String? {
        val files = manifest?.optJSONObject("files") ?: return null
        val speech = files.optJSONObject("speechEnhancement")
            ?: files.optJSONObject("speech_enhancement")
        val key = when (fileName.lowercase(Locale.US)) {
            "gtcrn_simple.onnx" -> "gtcrn"
            "dpdfnet2.onnx" -> "dpdfnet2"
            "dpdfnet4.onnx" -> "dpdfnet4"
            else -> fileName
        }
        return listOf(
            speech?.optString(key),
            speech?.optString(fileName),
            files.optString(fileName)
        ).firstOrNull { !it.isNullOrBlank() }
    }

    private fun activeResourceDir(): File? {
        val info = readActiveInfo() ?: return null
        val dir = File(installRoot, info.dirName)
        return dir.takeIf { it.isDirectory }
    }

    private fun findManifestFile(dir: File): File? {
        MANIFEST_FILE_NAMES
            .map { File(dir, it) }
            .firstOrNull { it.isFile }
            ?.let { return it }
        return dir.walkTopDown()
            .firstOrNull { file -> file.isFile && MANIFEST_FILE_NAMES.any { it.equals(file.name, ignoreCase = true) } }
    }

    private fun readManifestJson(dir: File): JSONObject? {
        val file = findManifestFile(dir) ?: return null
        return runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    private fun readActiveInfo(): ActiveInfo? {
        if (!activeFile.isFile) return null
        return runCatching {
            val json = JSONObject(activeFile.readText(Charsets.UTF_8))
            ActiveInfo(
                dirName = json.optString("dirName"),
                name = json.optString("name"),
                version = json.optString("version"),
                installedAtMs = json.optLong("installedAtMs", 0L)
            )
        }.getOrNull()?.takeIf { it.dirName.isNotBlank() }
    }

    private fun writeActiveInfo(info: ActiveInfo) {
        root.mkdirs()
        val json = JSONObject().apply {
            put("dirName", info.dirName)
            put("name", info.name)
            put("version", info.version)
            put("installedAtMs", info.installedAtMs)
        }
        activeFile.writeText(json.toString(2), Charsets.UTF_8)
    }

    private fun resolveRelativeFile(baseDir: File, relativePath: String): File? {
        val normalized = relativePath.replace('\\', '/').trim().removePrefix("/")
        if (normalized.isBlank()) return null
        val target = File(baseDir, normalized)
        return runCatching {
            val canonicalBase = baseDir.canonicalFile
            val canonicalTarget = target.canonicalFile
            val basePath = canonicalBase.path
            val targetPath = canonicalTarget.path
            if (targetPath == basePath || targetPath.startsWith("$basePath${File.separator}")) {
                canonicalTarget
            } else {
                null
            }
        }.getOrNull()
    }

    private fun entryOutputFile(outDir: File, entry: ZipEntry): File = entryOutputFile(outDir, entry.name)

    private fun entryOutputFile(outDir: File, rawName: String): File {
        val normalized = rawName
            .replace('\\', '/')
            .trim()
            .removePrefix("/")
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

    private fun cleanupStaleImports() {
        root.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith(".import-") }
            ?.forEach { it.deleteRecursively() }
    }

    private fun archiveExtensionFromUrl(url: String): String {
        val path = runCatching { URL(url).path }.getOrDefault(url)
        val ext = path.substringAfterLast('.', "").lowercase(Locale.US)
        return when (ext) {
            "zip", "7z" -> ext
            else -> "7z"
        }
    }

    private fun safeFileName(name: String): String {
        return name
            .trim()
            .ifBlank { "recognition-resources.7z" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim('.')
            .ifBlank { "recognition-resources.7z" }
    }

    private data class ActiveInfo(
        val dirName: String,
        val name: String,
        val version: String,
        val installedAtMs: Long
    )

    companion object {
        private const val ACTIVE_FILE_NAME = "active.json"
        private const val SEVEN_Z_MAX_MEMORY_KB = 96 * 1024
        private const val ANDROID_SAFE_7Z_ERROR =
            "资源包 7z 压缩字典过大，Android 端无法安全解压。请使用 32MB 或更小字典重新打包，或改用 zip 资源包。"
        private val MANIFEST_FILE_NAMES = listOf(
            "recognition_resources.json",
            "kigtts_recognition_resources.json",
            "manifest.json"
        )

        fun resolveSpeechEnhancementModel(context: Context, fileName: String): File? {
            return RecognitionResourceRepository(context).resolveSpeechEnhancementModel(fileName)
        }

        fun resolveSileroVadModel(context: Context): File? {
            return RecognitionResourceRepository(context).resolveSileroVadModel()
        }
    }
}
