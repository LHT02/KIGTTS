package com.lhtstudio.kigtts.app.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class SoundboardAudioImportResult(
    val path: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L
)

object SoundboardPresetIo {
    private const val PRESET_JSON = "preset.json"
    private const val SOUNDBOARD_TYPE = "soundboard"

    fun audioDir(context: Context): File {
        return File(context.filesDir, "soundboard/audio").apply { mkdirs() }
    }

    fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            val name = cursor.getString(index)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "audio"
    }

    fun readDurationMs(context: Context, uri: Uri): Long {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }
        }.getOrDefault(0L).coerceAtLeast(0L)
    }

    fun readDurationMs(file: File): Long {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }
        }.getOrDefault(0L).coerceAtLeast(0L)
    }

    fun importAudioClip(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        titleHint: String
    ): SoundboardAudioImportResult {
        val duration = readDurationMs(context, uri)
        val safeStart = startMs.coerceIn(0L, duration.coerceAtLeast(0L))
        val safeEnd = if (duration > 0L) {
            endMs.coerceIn(safeStart, duration).takeIf { it > safeStart } ?: duration
        } else {
            endMs.coerceAtLeast(safeStart)
        }
        val base = sanitizeFileSegment(titleHint.ifBlank { displayName(context, uri).substringBeforeLast('.') })
        val outFile = File(audioDir(context), "${base}_${System.currentTimeMillis()}_${UUID.randomUUID()}.m4a")
        val safeStartUs = millisToMicrosClamped(safeStart)
        val safeEndUs = millisToMicrosClamped(safeEnd)
        val fastMuxed = trimAacAudioToM4a(
            context = context,
            uri = uri,
            outFile = outFile,
            startUs = safeStartUs,
            endUs = safeEndUs
        )
        val encoded = fastMuxed || transcodeAudioToAacM4a(
            context = context,
            uri = uri,
            outFile = outFile,
            startUs = safeStartUs,
            endUs = safeEndUs
        )
        if (!encoded) {
            outFile.delete()
            error("音频转 AAC 失败，当前文件可能不是系统解码器支持的音频格式")
        }
        val storedDuration = readDurationMs(outFile).takeIf { it > 0L }
            ?: (safeEnd - safeStart).coerceAtLeast(0L)
        return SoundboardAudioImportResult(
            path = outFile.absolutePath,
            durationMs = storedDuration,
            trimStartMs = 0L,
            trimEndMs = storedDuration
        )
    }

    fun exportPackage(context: Context, config: SoundboardConfig, selectedGroupIds: Set<Long>): File {
        val groups = config.groups.filter { it.id in selectedGroupIds }
        require(groups.isNotEmpty()) { "未选择需要导出的分组" }
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(shareDir, "soundboard_$ts.kigspk")
        val audioEntries = linkedMapOf<String, File>()
        val root = JSONObject().apply {
            put("type", SOUNDBOARD_TYPE)
            put("version", 1)
            put("selectedGroupId", groups.first().id)
            put("portraitLayout", config.portraitLayout.wireValue)
            put("landscapeLayout", config.landscapeLayout.wireValue)
            put(
                "groups",
                JSONArray().apply {
                    groups.forEach { group ->
                        put(
                            JSONObject().apply {
                                put("id", group.id)
                                put("title", group.title)
                                put("icon", group.icon)
                                put(
                                    "items",
                                    JSONArray().apply {
                                        group.items.forEach { item ->
                                            val audioFile = File(item.audioPath)
                                            val audioEntry = if (audioFile.exists()) {
                                                val entryName = "audio/${sanitizeFileSegment(item.title)}_${UUID.randomUUID()}.${audioFile.extension.ifBlank { "audio" }}"
                                                audioEntries[entryName] = audioFile
                                                entryName
                                            } else {
                                                ""
                                            }
                                            put(
                                                JSONObject().apply {
                                                    put("id", item.id)
                                                    put("title", item.title)
                                                    put("wakeWord", item.wakeWord)
                                                    put("durationMs", item.durationMs)
                                                    put("trimStartMs", item.trimStartMs)
                                                    put("trimEndMs", item.trimEndMs)
                                                    put("audioFile", audioEntry)
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            )
        }
        ZipOutputStream(out.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry(PRESET_JSON))
            zos.write(root.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            audioEntries.forEach { (name, file) ->
                zos.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return out
    }

    fun importPackage(context: Context, uri: Uri, current: SoundboardConfig): SoundboardConfig {
        val tempDir = File(context.cacheDir, "soundboard_import_${System.currentTimeMillis()}").apply { mkdirs() }
        val audioFiles = linkedMapOf<String, File>()
        val json = try {
            var jsonPayload: String? = null
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    while (true) {
                        val entry = zis.nextEntry ?: break
                        val safeName = entry.name.replace('\\', '/').trimStart('/')
                        if (entry.isDirectory || safeName.contains("../")) {
                            zis.closeEntry()
                            continue
                        }
                        when {
                            safeName == PRESET_JSON -> {
                                jsonPayload = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            safeName.startsWith("audio/") -> {
                                val out = File(tempDir, safeName.substringAfterLast('/'))
                                out.parentFile?.mkdirs()
                                out.outputStream().use { zis.copyTo(it) }
                                audioFiles[safeName] = out
                            }
                        }
                        zis.closeEntry()
                    }
                }
            } ?: error("无法打开预设包")
            jsonPayload ?: error("预设包缺少 preset.json")
        } catch (_: java.util.zip.ZipException) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("无法打开预设包")
        }

        val root = JSONObject(json)
        require(root.optString("type") == SOUNDBOARD_TYPE) { "不是音效板预设包" }
        val existingTitles = current.groups.map { it.title }.toMutableSet()
        var nextGroupId = (current.groups.maxOfOrNull { it.id } ?: 0L) + 1L
        var nextItemId = current.groups.asSequence()
            .flatMap { it.items.asSequence() }
            .maxOfOrNull { it.id }
            ?.plus(1L) ?: 1L
        val importedGroups = mutableListOf<SoundboardGroup>()
        val groupsArray = root.optJSONArray("groups") ?: JSONArray()
        for (i in 0 until groupsArray.length()) {
            val groupObj = groupsArray.optJSONObject(i) ?: continue
            val title = uniqueImportedGroupTitle(
                groupObj.optString("title", "未命名分组"),
                existingTitles
            )
            existingTitles += title
            val itemsArray = groupObj.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<SoundboardItem>()
            for (j in 0 until itemsArray.length()) {
                val itemObj = itemsArray.optJSONObject(j) ?: continue
                val audioEntry = itemObj.optString("audioFile", "")
                val importedAudio = audioFiles[audioEntry]
                val storedAudioPath = if (importedAudio != null && importedAudio.exists()) {
                    val out = File(
                        audioDir(context),
                        "${sanitizeFileSegment(itemObj.optString("title", "audio"))}_${UUID.randomUUID()}.${importedAudio.extension.ifBlank { "audio" }}"
                    )
                    importedAudio.copyTo(out, overwrite = true)
                    out.absolutePath
                } else {
                    ""
                }
                items += SoundboardItem(
                    id = nextItemId++,
                    title = itemObj.optString("title", "新音效").trim().ifBlank { "新音效" },
                    wakeWord = itemObj.optString("wakeWord", "").trim(),
                    audioPath = storedAudioPath,
                    durationMs = itemObj.optLong("durationMs", 0L).coerceAtLeast(0L),
                    trimStartMs = itemObj.optLong("trimStartMs", 0L).coerceAtLeast(0L),
                    trimEndMs = itemObj.optLong("trimEndMs", 0L).coerceAtLeast(0L)
                )
            }
            importedGroups += SoundboardGroup(
                id = nextGroupId++,
                title = title,
                icon = groupObj.optString("icon", "music_note").ifBlank { "music_note" },
                items = items
            )
        }
        require(importedGroups.isNotEmpty()) { "预设包没有可导入分组" }
        return current.copy(
            groups = current.groups + importedGroups,
            selectedGroupId = importedGroups.first().id
        )
    }

    fun sanitizeFileSegment(raw: String): String {
        return raw.trim()
            .ifBlank { "audio" }
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .take(48)
            .ifBlank { "audio" }
    }

    private fun millisToMicrosClamped(ms: Long): Long {
        if (ms <= 0L) return 0L
        val maxSafeMs = Long.MAX_VALUE / 1000L
        return if (ms >= maxSafeMs) Long.MAX_VALUE else ms * 1000L
    }

    private fun transcodeAudioToAacM4a(
        context: Context,
        uri: Uri,
        outFile: File,
        startUs: Long,
        endUs: Long
    ): Boolean {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var outputTrack = -1
        var encodedSamples = 0
        return runCatching {
            extractor = MediaExtractor().apply { setDataSource(context, uri, null) }
            val source = extractor ?: return@runCatching false
            val trackIndex = (0 until source.trackCount).firstOrNull { index ->
                source.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME)
                    .orEmpty()
                    .startsWith("audio/")
            } ?: return@runCatching false
            val inputFormat = source.getTrackFormat(trackIndex)
            val inputMime = inputFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            if (inputMime.isBlank()) return@runCatching false
            if (inputMime == MediaFormat.MIMETYPE_AUDIO_RAW || inputMime == "audio/raw") {
                return@runCatching encodeRawAudioTrackToAacM4a(
                    source = source,
                    trackIndex = trackIndex,
                    inputFormat = inputFormat,
                    outFile = outFile,
                    startUs = startUs,
                    endUs = endUs
                )
            }

            val sampleRate = runCatching { inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) }
                .getOrDefault(44_100)
                .coerceAtLeast(8_000)
            val channelCount = runCatching { inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }
                .getOrDefault(1)
                .coerceIn(1, 2)
            val bitRate = if (channelCount >= 2) 160_000 else 96_000
            val outputFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                sampleRate,
                channelCount
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024)
            }

            source.selectTrack(trackIndex)
            source.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            decoder = MediaCodec.createDecoderByType(inputMime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val timeoutUs = 10_000L
            val decoderInfo = MediaCodec.BufferInfo()
            val encoderInfo = MediaCodec.BufferInfo()
            var extractorDone = false
            var decoderDone = false
            var encoderDone = false
            var queuedEncoderEos = false

            fun drainEncoder(wait: Boolean) {
                val enc = encoder ?: return
                val mux = muxer ?: return
                while (true) {
                    val status = enc.dequeueOutputBuffer(encoderInfo, if (wait) timeoutUs else 0L)
                    when {
                        status == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                        status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) error("AAC encoder output format changed twice")
                            outputTrack = mux.addTrack(enc.outputFormat)
                            mux.start()
                            muxerStarted = true
                        }
                        status >= 0 -> {
                            val encodedData = enc.getOutputBuffer(status)
                            if (encoderInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                encoderInfo.size = 0
                            }
                            if (encoderInfo.size > 0 && encodedData != null) {
                                if (!muxerStarted) error("AAC muxer has not started")
                                encodedData.position(encoderInfo.offset)
                                encodedData.limit(encoderInfo.offset + encoderInfo.size)
                                mux.writeSampleData(outputTrack, encodedData, encoderInfo)
                                encodedSamples += 1
                            }
                            val eos = encoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            enc.releaseOutputBuffer(status, false)
                            if (eos) {
                                encoderDone = true
                                return
                            }
                        }
                    }
                    if (!wait) {
                        // Non-blocking drain should return once the currently available output is consumed.
                        continue
                    }
                }
            }

            fun queueEncoderInputFromDecoder(decoderOutputIndex: Int, info: MediaCodec.BufferInfo) {
                val dec = decoder ?: return
                val enc = encoder ?: return
                val isDecoderEos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                val presentationUs = info.presentationTimeUs
                val shouldSkip = info.size <= 0 || presentationUs < startUs
                val reachedEnd = !isDecoderEos && presentationUs > endUs
                if (shouldSkip && !isDecoderEos && !reachedEnd) {
                    dec.releaseOutputBuffer(decoderOutputIndex, false)
                    return
                }
                if (queuedEncoderEos) {
                    dec.releaseOutputBuffer(decoderOutputIndex, false)
                    return
                }
                val decoderOutput = dec.getOutputBuffer(decoderOutputIndex)
                var outputPts = (presentationUs - startUs).coerceAtLeast(0L)
                val bytesPerFrame = (channelCount * 2).coerceAtLeast(1)
                var queuedAny = false
                if (!shouldSkip && !reachedEnd && decoderOutput != null && info.size > 0) {
                    decoderOutput.position(info.offset)
                    decoderOutput.limit(info.offset + info.size)
                    var remaining = info.size
                    while (remaining > 0) {
                        var encoderInputIndex = enc.dequeueInputBuffer(timeoutUs)
                        while (encoderInputIndex < 0) {
                            drainEncoder(wait = false)
                            encoderInputIndex = enc.dequeueInputBuffer(timeoutUs)
                        }
                        val encoderInput = enc.getInputBuffer(encoderInputIndex)
                            ?: error("AAC encoder input unavailable")
                        encoderInput.clear()
                        val copySize = minOf(remaining, encoderInput.capacity())
                        val oldLimit = decoderOutput.limit()
                        decoderOutput.limit(decoderOutput.position() + copySize)
                        encoderInput.put(decoderOutput)
                        decoderOutput.limit(oldLimit)
                        remaining -= copySize
                        val isLastChunk = remaining == 0
                        val flags = if (isDecoderEos && isLastChunk) {
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        } else {
                            0
                        }
                        enc.queueInputBuffer(encoderInputIndex, 0, copySize, outputPts, flags)
                        queuedAny = true
                        val frames = copySize / bytesPerFrame
                        if (frames > 0) {
                            outputPts += frames * 1_000_000L / sampleRate
                        }
                    }
                }
                if ((isDecoderEos || reachedEnd) && !queuedEncoderEos) {
                    if (!queuedAny) {
                        var encoderInputIndex = enc.dequeueInputBuffer(timeoutUs)
                        while (encoderInputIndex < 0) {
                            drainEncoder(wait = false)
                            encoderInputIndex = enc.dequeueInputBuffer(timeoutUs)
                        }
                        enc.queueInputBuffer(
                            encoderInputIndex,
                            0,
                            0,
                            if (reachedEnd) (endUs - startUs).coerceAtLeast(0L) else outputPts,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                    }
                    queuedEncoderEos = true
                }
                dec.releaseOutputBuffer(decoderOutputIndex, false)
            }

            while (!encoderDone) {
                drainEncoder(wait = false)

                if (!extractorDone) {
                    val inputIndex = decoder!!.dequeueInputBuffer(timeoutUs)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder!!.getInputBuffer(inputIndex)
                        val sampleTimeUs = source.sampleTime
                        if (sampleTimeUs < 0L || sampleTimeUs > endUs) {
                            decoder!!.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                (endUs - startUs).coerceAtLeast(0L),
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            extractorDone = true
                        } else {
                            inputBuffer?.clear()
                            val sampleSize = source.readSampleData(inputBuffer ?: ByteBuffer.allocate(0), 0)
                            if (sampleSize < 0) {
                                decoder!!.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    (endUs - startUs).coerceAtLeast(0L),
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                extractorDone = true
                            } else {
                                decoder!!.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    sampleTimeUs,
                                    source.sampleFlags
                                )
                                source.advance()
                            }
                        }
                    }
                }

                if (!decoderDone && !queuedEncoderEos) {
                    while (true) {
                        val outputIndex = decoder!!.dequeueOutputBuffer(decoderInfo, 0L)
                        when {
                            outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                            outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                            outputIndex >= 0 -> {
                                queueEncoderInputFromDecoder(outputIndex, decoderInfo)
                                if (decoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                    decoderDone = true
                                    break
                                }
                            }
                        }
                    }
                }

                if (queuedEncoderEos) {
                    drainEncoder(wait = true)
                }
            }

            encodedSamples > 0
        }.getOrDefault(false).also { ok ->
            runCatching {
                if (muxerStarted) muxer?.stop()
            }
            runCatching { muxer?.release() }
            runCatching {
                decoder?.stop()
                decoder?.release()
            }
            runCatching {
                encoder?.stop()
                encoder?.release()
            }
            runCatching { extractor?.release() }
            if (!ok) outFile.delete()
        }
    }

    private fun encodeRawAudioTrackToAacM4a(
        source: MediaExtractor,
        trackIndex: Int,
        inputFormat: MediaFormat,
        outFile: File,
        startUs: Long,
        endUs: Long
    ): Boolean {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var outputTrack = -1
        var encodedSamples = 0
        return runCatching {
            val pcmEncoding = runCatching {
                inputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
            }.getOrDefault(AudioFormat.ENCODING_PCM_16BIT)
            if (pcmEncoding != AudioFormat.ENCODING_PCM_16BIT) return@runCatching false
            val sampleRate = runCatching { inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) }
                .getOrDefault(44_100)
                .coerceAtLeast(8_000)
            val channelCount = runCatching { inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }
                .getOrDefault(1)
                .coerceIn(1, 2)
            val bitRate = if (channelCount >= 2) 160_000 else 96_000
            val outputFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                sampleRate,
                channelCount
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024)
            }

            source.selectTrack(trackIndex)
            source.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            while (source.sampleTime in 0 until startUs) {
                if (!source.advance()) break
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val timeoutUs = 10_000L
            val encoderInfo = MediaCodec.BufferInfo()
            val bytesPerFrame = (channelCount * 2).coerceAtLeast(1)
            val maxDurationUs = (endUs - startUs).coerceAtLeast(0L)
            var inputDone = false
            var encoderDone = false
            var outputPts = 0L

            fun drainEncoder(wait: Boolean) {
                val enc = encoder ?: return
                val mux = muxer ?: return
                while (true) {
                    val status = enc.dequeueOutputBuffer(encoderInfo, if (wait) timeoutUs else 0L)
                    when {
                        status == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                        status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) error("AAC encoder output format changed twice")
                            outputTrack = mux.addTrack(enc.outputFormat)
                            mux.start()
                            muxerStarted = true
                        }
                        status >= 0 -> {
                            val encodedData = enc.getOutputBuffer(status)
                            if (encoderInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                encoderInfo.size = 0
                            }
                            if (encoderInfo.size > 0 && encodedData != null) {
                                if (!muxerStarted) error("AAC muxer has not started")
                                encodedData.position(encoderInfo.offset)
                                encodedData.limit(encoderInfo.offset + encoderInfo.size)
                                mux.writeSampleData(outputTrack, encodedData, encoderInfo)
                                encodedSamples += 1
                            }
                            val eos = encoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            enc.releaseOutputBuffer(status, false)
                            if (eos) {
                                encoderDone = true
                                return
                            }
                        }
                    }
                }
            }

            while (!encoderDone) {
                drainEncoder(wait = false)
                if (!inputDone) {
                    val enc = encoder ?: return@runCatching false
                    val inputIndex = enc.dequeueInputBuffer(timeoutUs)
                    if (inputIndex >= 0) {
                        val sampleTime = source.sampleTime
                        val reachedEnd = sampleTime < 0L ||
                                sampleTime > endUs ||
                                (endUs != Long.MAX_VALUE && outputPts >= maxDurationUs)
                        val inputBuffer = enc.getInputBuffer(inputIndex)
                        if (reachedEnd || inputBuffer == null) {
                            enc.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                outputPts.coerceAtLeast(0L),
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            inputBuffer.clear()
                            val size = source.readSampleData(inputBuffer, 0)
                            if (size <= 0) {
                                enc.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    outputPts.coerceAtLeast(0L),
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                val pts = outputPts.coerceAtLeast(0L)
                                enc.queueInputBuffer(inputIndex, 0, size, pts, 0)
                                val frames = size / bytesPerFrame
                                if (frames > 0) {
                                    outputPts += frames * 1_000_000L / sampleRate
                                }
                                source.advance()
                            }
                        }
                    }
                } else {
                    drainEncoder(wait = true)
                }
            }
            encodedSamples > 0
        }.getOrDefault(false).also { ok ->
            runCatching {
                if (muxerStarted) muxer?.stop()
            }
            runCatching { muxer?.release() }
            runCatching {
                encoder?.stop()
                encoder?.release()
            }
            if (!ok) outFile.delete()
        }
    }

    private fun trimAacAudioToM4a(
        context: Context,
        uri: Uri,
        outFile: File,
        startUs: Long,
        endUs: Long
    ): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var sampleCount = 0
        return runCatching {
            extractor = MediaExtractor().apply { setDataSource(context, uri, null) }
            val source = extractor ?: return@runCatching false
            val trackIndex = (0 until source.trackCount).firstOrNull { index ->
                val mime = source.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
                mime.startsWith("audio/")
            } ?: return@runCatching false
            val format = source.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime != "audio/mp4a-latm") return@runCatching false

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outTrack = muxer!!.addTrack(format)
            muxer!!.start()
            muxerStarted = true
            source.selectTrack(trackIndex)
            source.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val bufferSize = runCatching {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            }.getOrDefault(256 * 1024).coerceAtLeast(64 * 1024)
            val buffer = ByteBuffer.allocate(bufferSize)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val sampleTime = source.sampleTime
                if (sampleTime < 0L || sampleTime > endUs) break
                buffer.clear()
                val size = source.readSampleData(buffer, 0)
                if (size < 0) break
                info.set(0, size, (sampleTime - startUs).coerceAtLeast(0L), source.sampleFlags)
                muxer!!.writeSampleData(outTrack, buffer, info)
                sampleCount += 1
                source.advance()
            }
            sampleCount > 0
        }.getOrDefault(false).also { ok ->
            runCatching {
                if (muxerStarted) muxer?.stop()
            }
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
            if (!ok) outFile.delete()
        }
    }
}
