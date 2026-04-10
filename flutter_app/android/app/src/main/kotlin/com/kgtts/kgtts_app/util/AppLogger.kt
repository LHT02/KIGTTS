package com.kgtts.kgtts_app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object AppLogger {
    private const val TAG = "KGTTs"
    private const val MAX_LOG_FILES = 10
    private val started = AtomicBoolean(false)
    private var logFile: File? = null

    fun init(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val dir = File(context.filesDir, "logs")
        dir.mkdirs()
        val current = File(dir, "app.log")
        if (current.exists() && current.length() > 0) {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val rotated = File(dir, "app-$stamp.log")
            if (!rotated.exists()) {
                current.renameTo(rotated)
            }
        }
        logFile = current
        pruneOldLogs(dir, current, MAX_LOG_FILES)
        i("logger.init path=${logFile?.absolutePath ?: "unknown"}")
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            e("uncaught thread=${t.name}", e)
            prev?.uncaughtException(t, e)
        }
    }

    fun logFilePath(): String? = logFile?.absolutePath

    fun listLogFiles(context: Context): List<File> {
        val dir = File(context.filesDir, "logs")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.isFile && f.extension.lowercase() == "log" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun readLog(file: File, maxBytes: Int = 512_000): String {
        if (!file.exists()) return ""
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val len = raf.length()
                val start = if (len > maxBytes) len - maxBytes else 0L
                raf.seek(start)
                val bytes = ByteArray((len - start).toInt())
                raf.readFully(bytes)
                String(bytes, Charsets.UTF_8)
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun d(msg: String) {
        Log.d(TAG, msg)
        write("D", msg, null)
    }

    fun i(msg: String) {
        Log.i(TAG, msg)
        write("I", msg, null)
    }

    fun w(msg: String, tr: Throwable? = null) {
        Log.w(TAG, msg, tr)
        write("W", msg, tr)
    }

    fun e(msg: String, tr: Throwable? = null) {
        Log.e(TAG, msg, tr)
        write("E", msg, tr)
    }

    private fun write(level: String, msg: String, tr: Throwable?) {
        val file = logFile ?: return
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val builder = StringBuilder()
        builder.append(time).append(" ").append(level).append(" ").append(msg)
        if (tr != null) {
            val sw = StringWriter()
            tr.printStackTrace(PrintWriter(sw))
            builder.append("\n").append(sw.toString().trimEnd())
        }
        builder.append("\n")
        try {
            file.appendText(builder.toString())
        } catch (_: Exception) {
        }
    }

    private fun pruneOldLogs(dir: File, current: File, keepCount: Int) {
        val logs = dir.listFiles { f -> f.isFile && f.extension.lowercase() == "log" }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        var kept = 0
        for (file in logs) {
            if (file.absolutePath == current.absolutePath) {
                kept++
                continue
            }
            if (kept < keepCount) {
                kept++
                continue
            }
            runCatching { file.delete() }
        }
    }
}
