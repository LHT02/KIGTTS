package com.kgtts.kgtts_app.data

import android.content.Context
import com.kgtts.kgtts_app.util.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object EspeakData {
    private const val assetName = "espeak-ng-data.zip"

    fun ensure(context: Context): File? {
        val targetDir = File(context.filesDir, "espeak-ng-data")
        if (hasData(targetDir)) {
            AppLogger.i("espeak data already present: ${targetDir.absolutePath}")
            return targetDir
        }
        targetDir.mkdirs()
        return try {
            AppLogger.i("espeak data extracting asset=$assetName to ${targetDir.absolutePath}")
            unzipAssetToDir(context, assetName, targetDir)
            if (hasData(targetDir)) targetDir else null
        } catch (e: Exception) {
            AppLogger.e("espeak data extract failed", e)
            null
        }
    }

    private fun hasData(dir: File): Boolean {
        if (!dir.exists()) return false
        return File(dir, "phondata").exists()
    }

    private fun unzipAssetToDir(context: Context, assetName: String, outDir: File) {
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
}
