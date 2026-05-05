package com.lhtstudio.kigtts.app.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import com.lhtstudio.kigtts.app.util.AppLogger
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val SYSTEM_TTS_VOICE_NAME = "__system_tts__"
private const val SYSTEM_TTS_VIRTUAL_DIR_NAME = "__system_tts_virtual__"

fun isSystemTtsVoiceDir(dir: File?): Boolean {
    return dir?.name == SYSTEM_TTS_VIRTUAL_DIR_NAME
}

data class ModelPaths(
    val asrDir: File?,
    val voiceDir: File?
)

data class VoicePackMeta(
    val name: String = "未命名",
    val remark: String = "",
    val avatar: String = "avatar.png",
    val pinned: Boolean = false,
    val order: Long = System.currentTimeMillis()
)

data class VoicePackInfo(
    val dir: File,
    val meta: VoicePackMeta
)

class ModelRepository(private val context: Context) {
    private val root = File(context.filesDir, "models")
    private val asrRoot = File(root, "asr")
    private val voiceRoot = File(root, "voice")
    private val recognitionResources = RecognitionResourceRepository(context)

    init {
        root.mkdirs()
        asrRoot.mkdirs()
        voiceRoot.mkdirs()
    }

    fun importAsr(uri: Uri, resolver: ContentResolver): File {
        val targetDir = File(asrRoot, safeName(uri))
        targetDir.mkdirs()
        AppLogger.i("importAsr uri=$uri target=${targetDir.absolutePath}")
        unzipToDir(uri, resolver, targetDir)
        AppLogger.i("importAsr done target=${targetDir.absolutePath}")
        return targetDir
    }

    fun importVoice(uri: Uri, resolver: ContentResolver): File {
        val targetDir = File(voiceRoot, safeName(uri, resolver))
        val importDir = File(voiceRoot, ".import-${System.currentTimeMillis()}")
        if (importDir.exists()) {
            importDir.deleteRecursively()
        }
        importDir.mkdirs()
        AppLogger.i("importVoice uri=$uri temp=${importDir.absolutePath} target=${targetDir.absolutePath}")
        try {
            unzipToDir(uri, resolver, importDir)
            validateVoicePack(importDir)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            importDir.parentFile?.mkdirs()
            val moved = importDir.renameTo(targetDir)
            if (!moved) {
                importDir.copyRecursively(targetDir, overwrite = true)
                importDir.deleteRecursively()
            }
            ensureVoiceMeta(targetDir)
            AppLogger.i("importVoice done target=${targetDir.absolutePath}")
            return targetDir
        } catch (e: Exception) {
            importDir.deleteRecursively()
            AppLogger.e("importVoice failed uri=$uri", e)
            throw e
        }
    }

    fun listVoicePacks(): List<VoicePackInfo> {
        val dirs = voiceRoot.listFiles()?.filter { it.isDirectory } ?: emptyList()
        val infos = dirs.map { dir ->
            VoicePackInfo(dir, ensureVoiceMeta(dir))
        }
        return infos.sortedWith(
            compareByDescending<VoicePackInfo> { it.meta.pinned }
                .thenBy { it.meta.order }
                .thenBy { it.meta.name }
        )
    }

    fun systemTtsVirtualDir(): File = File(root, SYSTEM_TTS_VIRTUAL_DIR_NAME)

    fun resolveAsr(name: String): File? {
        val dir = File(asrRoot, name)
        return if (dir.isDirectory) dir else null
    }

    fun recognitionResourceStatus(): RecognitionResourceStatus = recognitionResources.status()

    fun installRecognitionResources(
        uri: Uri,
        resolver: ContentResolver,
        onProgress: (RecognitionResourceProgress) -> Unit
    ): RecognitionResourceStatus {
        return recognitionResources.installFromUri(uri, resolver, onProgress)
    }

    fun downloadRecognitionResources(
        url: String,
        onProgress: (RecognitionResourceProgress) -> Unit
    ): RecognitionResourceStatus {
        return recognitionResources.downloadAndInstall(url, onProgress)
    }

    fun resolveVoicePack(name: String): File? {
        val dir = File(voiceRoot, name)
        return if (dir.isDirectory) dir else null
    }

    fun saveVoiceMeta(dir: File, meta: VoicePackMeta) {
        val file = metaFile(dir)
        val json = JSONObject().apply {
            put("name", meta.name)
            put("remark", meta.remark)
            put("avatar", meta.avatar)
            put("pinned", meta.pinned)
            put("order", meta.order)
        }
        file.writeText(json.toString(2), Charsets.UTF_8)
        ensureAvatar(dir, meta.avatar)
    }

    fun updateVoiceMeta(dir: File, updater: (VoicePackMeta) -> VoicePackMeta) {
        val current = ensureVoiceMeta(dir)
        val next = updater(current)
        saveVoiceMeta(dir, next)
    }

    fun updateVoiceAvatar(dir: File, resolver: ContentResolver, uri: Uri, fileName: String = "avatar.png") {
        val out = File(dir, fileName)
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output ->
                input.copyTo(output)
            }
        }
        updateVoiceMeta(dir) { meta ->
            meta.copy(avatar = fileName)
        }
    }

    fun deleteVoicePack(dir: File) {
        val target = requireManagedVoicePackDir(dir)
        if (!target.exists()) return
        target.deleteRecursively()
    }

    fun zipVoicePack(dir: File, outZip: File) {
        outZip.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(outZip)).use { zos ->
            dir.walkTopDown().forEach { file ->
                if (file.isDirectory) return@forEach
                val entryName = dir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
                val entry = ZipEntry(entryName)
                zos.putNextEntry(entry)
                file.inputStream().use { input -> input.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    fun sanitizeVoicePackShareName(name: String, fallback: String): String {
        val sanitized = name
            .trim()
            .ifEmpty { fallback }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim('.')
            .ifEmpty { fallback.ifBlank { "voicepack" } }
        return sanitized
    }

    fun ensureBundledAsr(): File? {
        recognitionResources.installedAsrDir()?.let { dir ->
            AppLogger.i("recognitionResourceAsr present: ${dir.absolutePath}")
            return dir
        }
        AppLogger.e("recognitionResourceAsr missing; external resource package required")
        return null
    }

    private fun requireManagedVoicePackDir(dir: File): File {
        val canonicalTarget = dir.canonicalFile
        val canonicalRoot = voiceRoot.canonicalFile
        val rootPath = canonicalRoot.path
        val targetPath = canonicalTarget.path
        val withinRoot = targetPath == rootPath || targetPath.startsWith("$rootPath${File.separator}")
        if (!withinRoot || canonicalTarget == canonicalRoot) {
            throw SecurityException("非法语音包目录：${dir.absolutePath}")
        }
        return canonicalTarget
    }

    private fun safeName(uri: Uri, resolver: ContentResolver? = null): String {
        val last = displayName(uri, resolver)
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "package"
        return stripArchiveSuffix(last).ifBlank { "package" }
    }

    private fun unzipToDir(uri: Uri, resolver: ContentResolver, outDir: File) {
        resolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outPath = entryOutputFile(outDir, entry)
                    if (entry.isDirectory) {
                        outPath.mkdirs()
                    } else {
                        outPath.parentFile?.mkdirs()
                        FileOutputStream(outPath).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    private fun displayName(uri: Uri, resolver: ContentResolver?): String? {
        if (resolver == null || uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        val cursor: Cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx < 0) return null
            return it.getString(idx)
        }
    }

    private fun stripArchiveSuffix(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".kigvpk") -> name.dropLast(7)
            lower.endsWith(".zip") -> name.dropLast(4)
            else -> name
        }
    }

    private fun isValidVoicePackDir(dir: File): Boolean {
        return try {
            validateVoicePack(dir)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun entryOutputFile(outDir: File, entry: ZipEntry): File {
        val normalized = entry.name
            .replace('\\', '/')
            .trim()
            .removePrefix("/")
        if (normalized.isBlank()) {
            throw IOException("无效压缩包条目：空路径")
        }
        val outPath = File(outDir, normalized)
        val canonicalRoot = outDir.canonicalFile
        val canonicalOut = outPath.canonicalFile
        if (
            canonicalOut != canonicalRoot &&
            !canonicalOut.path.startsWith("${canonicalRoot.path}${File.separator}")
        ) {
            throw IOException("无效压缩包条目：${entry.name}")
        }
        return canonicalOut
    }

    private fun validateVoicePack(dir: File) {
        val manifestFile = File(dir, "manifest.json")
        if (!manifestFile.isFile) {
            throw IOException("无效语音包：缺少 manifest.json")
        }
        val json = try {
            JSONObject(manifestFile.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            throw IOException("无效语音包：manifest.json 解析失败", e)
        }
        val files = json.optJSONObject("files")
            ?: throw IOException("无效语音包：manifest.json 缺少 files 配置")
        val requiredPaths = listOf(
            files.optString("model"),
            files.optString("config"),
            files.optString("phonemizer")
        )
        val requiredNames = listOf("model", "config", "phonemizer")
        requiredPaths.forEachIndexed { index, relPath ->
            val normalized = relPath.replace('\\', '/').trim().removePrefix("/")
            if (normalized.isBlank()) {
                throw IOException("无效语音包：manifest.files.${requiredNames[index]} 为空")
            }
            val target = File(dir, normalized)
            if (!target.isFile) {
                throw IOException("无效语音包：缺少必要文件 ${normalized}")
            }
        }
    }

    private fun metaFile(dir: File): File = File(dir, "voicepack.json")

    private fun ensureVoiceMeta(dir: File): VoicePackMeta {
        val file = metaFile(dir)
        val parsed = if (file.exists()) {
            try {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                VoicePackMeta(
                    name = json.optString("name", "未命名").ifBlank { "未命名" },
                    remark = json.optString("remark", ""),
                    avatar = json.optString("avatar", "avatar.png"),
                    pinned = json.optBoolean("pinned", false),
                    order = json.optLong("order", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                VoicePackMeta()
            }
        } else {
            VoicePackMeta()
        }
        saveVoiceMeta(dir, parsed)
        return parsed
    }

    private fun ensureAvatar(dir: File, fileName: String) {
        val file = File(dir, fileName)
        if (file.exists()) return
        val bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply { drawColor(Color.parseColor("#B0B0B0")) }
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
