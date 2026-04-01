package com.kgtts.kgtts_app.channels

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import com.kgtts.kgtts_app.data.ModelRepository
import com.kgtts.kgtts_app.util.AppLogger
import java.io.File

class ModelChannel(
    flutterEngine: FlutterEngine,
    private val context: Context
) : MethodChannel.MethodCallHandler {

    private val channel = MethodChannel(
        flutterEngine.dartExecutor.binaryMessenger,
        "com.kgtts.app/models"
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repo = ModelRepository(context)

    init {
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "listVoicePacks" -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val packs = repo.listVoicePacks()
                        val list = packs.map { info ->
                            mapOf(
                                "dirName" to info.dir.name,
                                "dirPath" to info.dir.absolutePath,
                                "name" to info.meta.name,
                                "remark" to info.meta.remark,
                                "avatar" to info.meta.avatar,
                                "pinned" to info.meta.pinned,
                                "order" to info.meta.order,
                                "avatarPath" to File(info.dir, info.meta.avatar).let {
                                    if (it.exists()) it.absolutePath else null
                                }
                            )
                        }
                        withContext(Dispatchers.Main) { result.success(list) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("LIST_FAILED", e.message, null)
                        }
                    }
                }
            }
            "listAsrModels" -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val modelsDir = File(context.filesDir, "models/asr")
                        val models = if (modelsDir.exists()) {
                            modelsDir.listFiles()?.filter { it.isDirectory }?.map { dir ->
                                mapOf(
                                    "dirName" to dir.name,
                                    "dirPath" to dir.absolutePath,
                                    "isBundled" to dir.name.contains("bundled")
                                )
                            } ?: emptyList()
                        } else emptyList()
                        withContext(Dispatchers.Main) { result.success(models) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("LIST_FAILED", e.message, null)
                        }
                    }
                }
            }
            "importVoice" -> {
                val filePath = call.argument<String>("filePath")
                    ?: return result.error("ARGS", "missing filePath", null)
                scope.launch(Dispatchers.IO) {
                    try {
                        val dir = repo.importVoice(android.net.Uri.parse(filePath))
                        val meta = repo.readVoiceMeta(dir)
                        withContext(Dispatchers.Main) {
                            result.success(mapOf(
                                "dirName" to dir.name,
                                "dirPath" to dir.absolutePath,
                                "name" to meta.name
                            ))
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("IMPORT_FAILED", e.message, null)
                        }
                    }
                }
            }
            "importAsr" -> {
                val filePath = call.argument<String>("filePath")
                    ?: return result.error("ARGS", "missing filePath", null)
                scope.launch(Dispatchers.IO) {
                    try {
                        val dir = repo.importAsr(android.net.Uri.parse(filePath))
                        withContext(Dispatchers.Main) {
                            result.success(mapOf(
                                "dirName" to dir.name,
                                "dirPath" to dir.absolutePath
                            ))
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("IMPORT_FAILED", e.message, null)
                        }
                    }
                }
            }
            "deleteVoice" -> {
                val dirName = call.argument<String>("dirName")
                    ?: return result.error("ARGS", "missing dirName", null)
                scope.launch(Dispatchers.IO) {
                    try {
                        repo.deleteVoicePack(dirName)
                        withContext(Dispatchers.Main) { result.success(null) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("DELETE_FAILED", e.message, null)
                        }
                    }
                }
            }
            "updateVoiceMeta" -> {
                val dirName = call.argument<String>("dirName")
                    ?: return result.error("ARGS", "missing dirName", null)
                val meta = call.argument<Map<String, Any>>("meta")
                    ?: return result.error("ARGS", "missing meta", null)
                scope.launch(Dispatchers.IO) {
                    try {
                        repo.saveVoiceMeta(dirName, meta)
                        withContext(Dispatchers.Main) { result.success(null) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("UPDATE_FAILED", e.message, null)
                        }
                    }
                }
            }
            "updateVoiceAvatar" -> {
                val dirName = call.argument<String>("dirName")
                    ?: return result.error("ARGS", "missing dirName", null)
                val imagePath = call.argument<String>("imagePath")
                    ?: return result.error("ARGS", "missing imagePath", null)
                scope.launch(Dispatchers.IO) {
                    try {
                        repo.updateVoiceAvatar(dirName, imagePath)
                        withContext(Dispatchers.Main) { result.success(null) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("UPDATE_FAILED", e.message, null)
                        }
                    }
                }
            }
            "exportVoice" -> {
                val dirName = call.argument<String>("dirName")
                    ?: return result.error("ARGS", "missing dirName", null)
                val destPath = call.argument<String>("destPath")
                    ?: return result.error("ARGS", "missing destPath", null)
                scope.launch(Dispatchers.IO) {
                    try {
                        repo.zipVoicePack(dirName, File(destPath))
                        withContext(Dispatchers.Main) { result.success(null) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("EXPORT_FAILED", e.message, null)
                        }
                    }
                }
            }
            "ensureBundledAsr" -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val dir = repo.ensureBundledAsr()
                        withContext(Dispatchers.Main) {
                            result.success(dir?.absolutePath)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("BUNDLED_FAILED", e.message, null)
                        }
                    }
                }
            }
            "getLastVoiceName" -> {
                val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
                result.success(prefs.getString("flutter.last_voice_name", null))
            }
            "setLastVoiceName" -> {
                val name = call.argument<String>("name") ?: return result.error("ARGS", "missing name", null)
                val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
                prefs.edit().putString("flutter.last_voice_name", name).apply()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    fun dispose() {
        scope.cancel()
    }
}
