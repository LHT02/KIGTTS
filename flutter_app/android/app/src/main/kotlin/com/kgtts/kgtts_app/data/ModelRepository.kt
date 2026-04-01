package com.kgtts.kgtts_app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import com.kgtts.kgtts_app.util.AppLogger
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
    private val bundledAsrAsset = "sosv-int8.zip"

    init {
        root.mkdirs()
        asrRoot.mkdirs()
        voiceRoot.mkdirs()
    }

    fun importAsr(uri: Uri): File {
        val resolver = context.contentResolver
        val targetDir = File(asrRoot, safeName(uri))
        targetDir.mkdirs()
        AppLogger.i("importAsr uri=$uri target=${targetDir.absolutePath}")
        unzipToDir(uri, resolver, targetDir)
        AppLogger.i("importAsr done target=${targetDir.absolutePath}")
        return targetDir
    }

    fun importVoice(uri: Uri): File {
        val resolver = context.contentResolver
        val targetDir = File(voiceRoot, safeName(uri))
        targetDir.mkdirs()
        AppLogger.i("importVoice uri=$uri target=${targetDir.absolutePath}")
        unzipToDir(uri, resolver, targetDir)
        ensureVoiceMeta(targetDir)
        AppLogger.i("importVoice done target=${targetDir.absolutePath}")
        return targetDir
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

    fun resolveVoicePack(name: String): File? {
        val dir = File(voiceRoot, name)
        return if (dir.isDirectory) dir else null
    }

    fun readVoiceMeta(dir: File): VoicePackMeta {
        return ensureVoiceMeta(dir)
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

    fun saveVoiceMeta(dirName: String, metaMap: Map<String, Any>) {
        val dir = File(voiceRoot, dirName)
        if (!dir.exists()) return
        val current = ensureVoiceMeta(dir)
        val updated = current.copy(
            name = (metaMap["name"] as? String) ?: current.name,
            remark = (metaMap["remark"] as? String) ?: current.remark,
            avatar = (metaMap["avatar"] as? String) ?: current.avatar,
            pinned = (metaMap["pinned"] as? Boolean) ?: current.pinned,
            order = (metaMap["order"] as? Number)?.toLong() ?: current.order
        )
        saveVoiceMeta(dir, updated)
    }

    fun updateVoiceAvatar(dirName: String, imagePath: String) {
        val dir = File(voiceRoot, dirName)
        if (!dir.exists()) return
        val src = File(imagePath)
        if (!src.exists()) return
        val dest = File(dir, "avatar.png")
        src.copyTo(dest, overwrite = true)
        val current = ensureVoiceMeta(dir)
        saveVoiceMeta(dir, current.copy(avatar = "avatar.png"))
    }

    fun deleteVoicePack(dirName: String) {
        val dir = File(voiceRoot, dirName)
        if (!dir.exists()) return
        dir.deleteRecursively()
    }

    fun zipVoicePack(dirName: String, outZip: File) {
        val dir = File(voiceRoot, dirName)
        if (!dir.exists()) return
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

    fun ensureBundledAsr(): File? {
        val targetDir = File(asrRoot, "bundled-sosv-int8")
        if (hasOnnx(targetDir)) {
            AppLogger.i("bundledAsr already present: ${targetDir.absolutePath}")
            return targetDir
        }
        targetDir.mkdirs()
        return try {
            AppLogger.i("bundledAsr extracting asset=$bundledAsrAsset to ${targetDir.absolutePath}")
            unzipAssetToDir(bundledAsrAsset, targetDir)
            if (hasOnnx(targetDir)) targetDir else null
        } catch (e: Exception) {
            AppLogger.e("bundledAsr extract failed", e)
            null
        }
    }

    private fun safeName(uri: Uri): String {
        val last = uri.lastPathSegment?.substringAfterLast('/') ?: "package"
        return last.removeSuffix(".zip").ifEmpty { "package" }
    }

    private fun unzipToDir(uri: Uri, resolver: android.content.ContentResolver, outDir: File) {
        resolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outPath = File(outDir, entry.name)
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

    private fun unzipAssetToDir(assetName: String, outDir: File) {
        context.assets.open(assetName).use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outPath = File(outDir, entry.name)
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

    private fun hasOnnx(dir: File): Boolean {
        if (!dir.exists()) return false
        return dir.walkTopDown().any { it.isFile && it.extension.lowercase() == "onnx" }
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
