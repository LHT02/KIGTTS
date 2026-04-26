package com.lhtstudio.kigtts.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import com.lhtstudio.kigtts.app.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object AudioDenoiserMode {
    const val OFF = 0
    const val RNNOISE = 1
    const val SPEEX = 2

    val options: List<Pair<Int, String>> = listOf(
        OFF to "关闭",
        RNNOISE to "RNNoise",
        SPEEX to "Speex"
    )

    fun labelOf(mode: Int): String {
        return options.firstOrNull { it.first == mode }?.second ?: options.first().second
    }
}

object SpeechEnhancementMode {
    const val OFF = 0
    const val GTCRN_OFFLINE = 1
    const val GTCRN_STREAMING = 2
    const val DPDFNET2_STREAMING = 3
    const val DPDFNET4_STREAMING = 4

    val options: List<Pair<Int, String>> = listOf(
        OFF to "关闭",
        GTCRN_OFFLINE to "GTCRN 句级增强",
        GTCRN_STREAMING to "GTCRN 流式增强",
        DPDFNET2_STREAMING to "DPDFNet2 流式增强",
        DPDFNET4_STREAMING to "DPDFNet4 流式增强"
    )

    fun clamp(mode: Int): Int {
        return options.firstOrNull { it.first == mode }?.first ?: OFF
    }

    fun isEnabled(mode: Int): Boolean = clamp(mode) != OFF

    fun isStreaming(mode: Int): Boolean {
        return when (clamp(mode)) {
            GTCRN_STREAMING, DPDFNET2_STREAMING, DPDFNET4_STREAMING -> true
            else -> false
        }
    }

    fun labelOf(mode: Int): String {
        return options.firstOrNull { it.first == clamp(mode) }?.second ?: options.first().second
    }
}

object VadMode {
    const val CLASSIC = 0
    const val SILERO = 1
    const val HYBRID = 2

    val options: List<Pair<Int, String>> = listOf(
        CLASSIC to "阈值式VAD",
        SILERO to "SileroVAD",
        HYBRID to "混合VAD"
    )

    fun clamp(mode: Int): Int {
        return options.firstOrNull { it.first == mode }?.first ?: CLASSIC
    }

    fun fromFlags(classicEnabled: Boolean, sileroEnabled: Boolean): Int {
        return when {
            classicEnabled && sileroEnabled -> HYBRID
            sileroEnabled -> SILERO
            else -> CLASSIC
        }
    }

    fun toFlags(mode: Int): Pair<Boolean, Boolean> {
        return when (clamp(mode)) {
            SILERO -> false to true
            HYBRID -> true to true
            else -> true to false
        }
    }

    fun labelOf(mode: Int): String {
        return options.firstOrNull { it.first == clamp(mode) }?.second ?: options.first().second
    }
}

private class RnNoiseNative private constructor() {
    companion object {
        private val loaded by lazy {
            runCatching {
                System.loadLibrary("rnnoise_jni")
            }.onFailure {
                AppLogger.e("RNNoise load failed", it)
            }.isSuccess
        }

        @JvmStatic external fun nativeCreate(): Long
        @JvmStatic external fun nativeDestroy(handle: Long)
        @JvmStatic external fun nativeFrameSize(): Int
        @JvmStatic external fun nativeProcessFrame(handle: Long, input: FloatArray, output: FloatArray): Float

        fun isAvailable(): Boolean = loaded
    }
}

private class SpeexNative private constructor() {
    companion object {
        private val loaded by lazy {
            runCatching {
                System.loadLibrary("speex_jni")
            }.onFailure {
                AppLogger.e("Speex load failed", it)
            }.isSuccess
        }

        @JvmStatic external fun nativeCreate(frameSize: Int, sampleRate: Int): Long
        @JvmStatic external fun nativeDestroy(handle: Long)
        @JvmStatic external fun nativeProcessFrame(handle: Long, input: FloatArray, output: FloatArray)

        fun isAvailable(): Boolean = loaded
    }
}

private abstract class StreamingFrameProcessor(
    private val frameSize: Int
) {
    private var carry = FloatArray(0)

    protected abstract fun processFrame(input: FloatArray, output: FloatArray)

    fun processInPlace(buffer: FloatArray, length: Int = buffer.size) {
        if (length <= 0) return
        val safeLength = length.coerceAtMost(buffer.size)
        val carrySize = carry.size
        val combined = FloatArray(carrySize + safeLength)
        if (carrySize > 0) {
            System.arraycopy(carry, 0, combined, 0, carrySize)
        }
        System.arraycopy(buffer, 0, combined, carrySize, safeLength)
        val processLen = (combined.size / frameSize) * frameSize
        if (processLen <= 0) {
            carry = combined
            return
        }
        val processed = FloatArray(processLen)
        val inputFrame = FloatArray(frameSize)
        val outputFrame = FloatArray(frameSize)
        var offset = 0
        while (offset < processLen) {
            System.arraycopy(combined, offset, inputFrame, 0, frameSize)
            processFrame(inputFrame, outputFrame)
            System.arraycopy(outputFrame, 0, processed, offset, frameSize)
            offset += frameSize
        }
        val copyStart = carrySize.coerceAtMost(processed.size)
        val available = (processed.size - copyStart).coerceAtLeast(0)
        val copyCount = min(safeLength, available)
        if (copyCount > 0) {
            System.arraycopy(processed, copyStart, buffer, 0, copyCount)
        }
        carry = if (processLen < combined.size) {
            combined.copyOfRange(processLen, combined.size)
        } else {
            FloatArray(0)
        }
    }

    fun reset() {
        carry = FloatArray(0)
    }
}

class RnNoiseProcessor {
    private val nativeFrameSize by lazy {
        RnNoiseNative.nativeFrameSize().also {
            check(it == 480) { "Unexpected RNNoise frame size: $it" }
        }
    }
    private val handle by lazy {
        RnNoiseNative.nativeCreate().also {
            check(it != 0L) { "RNNoise init failed" }
        }
    }
    private val work48In by lazy { FloatArray(nativeFrameSize) }
    private val work48Out by lazy { FloatArray(nativeFrameSize) }
    private val frame16 = 160
    private val lock = Any()
    @Volatile private var released = false
    private val processor by lazy {
        object : StreamingFrameProcessor(frame16) {
            override fun processFrame(input: FloatArray, output: FloatArray) {
                upsample16kTo48k(input, work48In)
                RnNoiseNative.nativeProcessFrame(handle, work48In, work48Out)
                downsample48kTo16k(work48Out, output)
            }
        }
    }

    init {
        check(RnNoiseNative.isAvailable()) { "RNNoise native library unavailable" }
        nativeFrameSize
        handle
    }

    fun processInPlace(buffer: FloatArray, length: Int = buffer.size) {
        synchronized(lock) {
            if (released) return
            processor.processInPlace(buffer, length)
        }
    }

    fun reset() {
        synchronized(lock) {
            if (released) return
            processor.reset()
        }
    }

    fun release() {
        synchronized(lock) {
            if (released) return
            released = true
            processor.reset()
            RnNoiseNative.nativeDestroy(handle)
        }
    }

    private fun upsample16kTo48k(input: FloatArray, output: FloatArray) {
        val lastIndex = input.lastIndex
        for (i in input.indices) {
            val next = input[min(i + 1, lastIndex)]
            val base = i * 3
            output[base] = input[i]
            output[base + 1] = (input[i] * 2f + next) / 3f
            output[base + 2] = (input[i] + next * 2f) / 3f
        }
    }

    private fun downsample48kTo16k(input: FloatArray, output: FloatArray) {
        for (i in output.indices) {
            val base = i * 3
            output[i] = (input[base] + input[base + 1] + input[base + 2]) / 3f
        }
    }
}

class SpeexNoiseSuppressor(
    sampleRate: Int = 16000,
    frameSize: Int = 160
) {
    private val lock = Any()
    @Volatile private var released = false
    private val handle by lazy {
        SpeexNative.nativeCreate(frameSize, sampleRate).also {
            check(it != 0L) { "Speex init failed" }
        }
    }
    private val processor by lazy {
        object : StreamingFrameProcessor(frameSize) {
            private val frameOutput = FloatArray(frameSize)

            override fun processFrame(input: FloatArray, output: FloatArray) {
                SpeexNative.nativeProcessFrame(handle, input, frameOutput)
                System.arraycopy(frameOutput, 0, output, 0, frameSize)
            }
        }
    }

    init {
        check(SpeexNative.isAvailable()) { "Speex native library unavailable" }
        handle
    }

    fun processInPlace(buffer: FloatArray, length: Int = buffer.size) {
        synchronized(lock) {
            if (released) return
            processor.processInPlace(buffer, length)
        }
    }

    fun reset() {
        synchronized(lock) {
            if (released) return
            processor.reset()
        }
    }

    fun release() {
        synchronized(lock) {
            if (released) return
            released = true
            processor.reset()
            SpeexNative.nativeDestroy(handle)
        }
    }
}

data class AudioTestSnapshot(
    val recording: Boolean = false,
    val playing: Boolean = false,
    val hasClip: Boolean = false,
    val status: String = "未录制测试音频",
    val level: Float = 0f
)

data class AudioTestConfig(
    val audioSource: Int,
    val preferredInputType: Int = AudioRoutePreference.INPUT_AUTO,
    val preferredOutputType: Int = AudioRoutePreference.OUTPUT_AUTO,
    val useCommunicationMode: Boolean = false,
    val speechEnhancementMode: Int = SpeechEnhancementMode.OFF
)

class AudioLoopbackTester(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSnapshot: (AudioTestSnapshot) -> Unit
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val sampleRate = 16000
    private var clip: ShortArray = ShortArray(0)
    private var snapshot = AudioTestSnapshot()
    private var recordJob: Job? = null
    private var playJob: Job? = null
    private var previousAudioMode: Int? = null
    private var previousSpeakerOn: Boolean? = null

    private fun publish(transform: (AudioTestSnapshot) -> AudioTestSnapshot) {
        snapshot = transform(snapshot)
        onSnapshot(snapshot)
    }

    fun startRecording(config: AudioTestConfig): Boolean {
        if (snapshot.recording || snapshot.playing) return false
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            config.audioSource,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuffer, 4096)
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            publish { it.copy(status = "音频测试：录音初始化失败", level = 0f) }
            return false
        }
        applyCommunicationMode(config.useCommunicationMode)
        applyPreferredInput(recorder, config.preferredInputType)
        val chunks = mutableListOf<ShortArray>()
        recordJob = scope.launch(Dispatchers.IO) {
            try {
                recorder.startRecording()
                publish {
                    it.copy(
                        recording = true,
                        playing = false,
                        status = "音频测试：录制中",
                        level = 0f
                    )
                }
                val buffer = ShortArray(1024)
                var totalSamples = 0
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    val copy = buffer.copyOf(read)
                    chunks += copy
                    totalSamples += read
                    var sumSq = 0.0
                    for (sample in copy) {
                        val v = sample / 32768.0
                        sumSq += v * v
                    }
                    val rms = if (read > 0) sqrt(sumSq / read).toFloat() else 0f
                    publish { current -> current.copy(level = rms.coerceIn(0f, 1f)) }
                }
                clip = flattenShortArrays(chunks, totalSamples)
                val seconds = clip.size.toFloat() / sampleRate.toFloat()
                publish {
                    it.copy(
                        recording = false,
                        hasClip = clip.isNotEmpty(),
                        status = if (clip.isNotEmpty()) {
                            "音频测试：已录制 ${String.format("%.1f", seconds)} 秒"
                        } else {
                            "音频测试：未录到音频"
                        },
                        level = 0f
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("AudioLoopbackTester record failed", e)
                publish { it.copy(recording = false, status = "音频测试：录制失败", level = 0f) }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
                restoreCommunicationMode()
                recordJob = null
            }
        }
        return true
    }

    fun stopRecording() {
        recordJob?.cancel()
    }

    fun play(config: AudioTestConfig): Boolean {
        if (snapshot.recording || snapshot.playing || clip.isEmpty()) return false
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            publish { it.copy(status = "音频测试：回放初始化失败") }
            return false
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(
                        if (config.useCommunicationMode) {
                            AudioAttributes.USAGE_VOICE_COMMUNICATION
                        } else {
                            AudioAttributes.USAGE_MEDIA
                        }
                    )
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setSampleRate(sampleRate)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuffer, clip.size * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            publish { it.copy(status = "音频测试：回放初始化失败") }
            return false
        }
        applyCommunicationMode(config.useCommunicationMode)
        applyPreferredOutput(track, config.preferredOutputType)
        val rawData = clip.copyOf()
        playJob = scope.launch(Dispatchers.IO) {
            try {
                val mode = SpeechEnhancementMode.clamp(config.speechEnhancementMode)
                val playbackData = if (SpeechEnhancementMode.isEnabled(mode)) {
                    publish {
                        it.copy(
                            playing = true,
                            status = "音频测试：正在应用${SpeechEnhancementMode.labelOf(mode)}",
                            level = 0f
                        )
                    }
                    val enhanced = preparePlaybackClip(rawData, mode)
                    if (enhanced.isNotEmpty()) {
                        enhanced
                    } else {
                        rawData
                    }
                } else {
                    rawData
                }
                val playbackLabel = if (SpeechEnhancementMode.isEnabled(mode)) {
                    SpeechEnhancementMode.labelOf(mode)
                } else {
                    "原始录音"
                }
                publish {
                    it.copy(
                        playing = true,
                        status = "音频测试：回放中（$playbackLabel）",
                        level = 0f
                    )
                }
                track.play()
                track.write(playbackData, 0, playbackData.size, AudioTrack.WRITE_BLOCKING)
                while (isActive && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    if (track.playbackHeadPosition >= playbackData.size) break
                    delay(30)
                }
                publish { it.copy(playing = false, status = "音频测试：回放完成", level = 0f) }
            } catch (e: Exception) {
                AppLogger.e("AudioLoopbackTester play failed", e)
                publish { it.copy(playing = false, status = "音频测试：回放失败", level = 0f) }
            } finally {
                runCatching { track.stop() }
                track.release()
                restoreCommunicationMode()
                playJob = null
            }
        }
        return true
    }

    fun stopPlayback() {
        playJob?.cancel()
    }

    fun clear() {
        stopRecording()
        stopPlayback()
        clip = ShortArray(0)
        publish {
            it.copy(
                recording = false,
                playing = false,
                hasClip = false,
                status = "未录制测试音频",
                level = 0f
            )
        }
    }

    fun release() {
        clear()
    }

    private fun flattenShortArrays(chunks: List<ShortArray>, totalSamples: Int): ShortArray {
        if (totalSamples <= 0) return ShortArray(0)
        val out = ShortArray(totalSamples)
        var offset = 0
        for (chunk in chunks) {
            System.arraycopy(chunk, 0, out, offset, chunk.size)
            offset += chunk.size
        }
        return out
    }

    private fun preparePlaybackClip(data: ShortArray, mode: Int): ShortArray {
        if (data.isEmpty()) return data
        return try {
            val floatSamples = FloatArray(data.size) { index ->
                data[index].toFloat() / 32768f
            }
            val (enhanced, _) = SherpaSpeechEnhancer.processPreview(
                context = context,
                mode = mode,
                samples = floatSamples,
                sampleRate = sampleRate
            )
            if (enhanced.isEmpty()) {
                data
            } else {
                ShortArray(enhanced.size) { index ->
                    (enhanced[index].coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("AudioLoopbackTester speech enhancement failed", e)
            data
        } finally {
            SherpaSpeechEnhancer.resetStreaming()
        }
    }

    private fun applyCommunicationMode(enabled: Boolean) {
        val manager = audioManager ?: return
        try {
            if (enabled) {
                if (previousAudioMode == null) {
                    previousAudioMode = manager.mode
                }
                if (manager.mode != AudioManager.MODE_IN_COMMUNICATION) {
                    manager.mode = AudioManager.MODE_IN_COMMUNICATION
                }
            } else {
                restoreCommunicationMode()
            }
        } catch (e: Exception) {
            AppLogger.e("AudioLoopbackTester mode set failed", e)
        }
    }

    private fun restoreCommunicationMode() {
        val manager = audioManager ?: return
        val prev = previousAudioMode ?: return
        try {
            if (manager.mode != prev) {
                manager.mode = prev
            }
        } catch (e: Exception) {
            AppLogger.e("AudioLoopbackTester mode restore failed", e)
        } finally {
            previousAudioMode = null
        }
    }

    private fun applyPreferredInput(recorder: AudioRecord, preferredInputType: Int) {
        if (Build.VERSION.SDK_INT < 23) return
        val manager = audioManager ?: return
        val target = pickPreferredInputDevice(manager.getDevices(AudioManager.GET_DEVICES_INPUTS), preferredInputType)
        if (target != null) {
            runCatching { recorder.setPreferredDevice(target) }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyPreferredOutput(track: AudioTrack, preferredOutputType: Int) {
        val manager = audioManager ?: return
        try {
            if (previousSpeakerOn == null) {
                previousSpeakerOn = manager.isSpeakerphoneOn
            }
            if (Build.VERSION.SDK_INT >= 23) {
                val target = pickPreferredOutputDevice(manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS), preferredOutputType)
                if (target != null) {
                    runCatching { track.preferredDevice = target }
                }
            }
            if (Build.VERSION.SDK_INT >= 31) {
                when (preferredOutputType) {
                    AudioRoutePreference.OUTPUT_AUTO -> manager.clearCommunicationDevice()
                    else -> {
                        val target = pickPreferredOutputDevice(
                            manager.availableCommunicationDevices.toTypedArray(),
                            preferredOutputType
                        )
                        if (target != null) {
                            manager.setCommunicationDevice(target)
                        }
                    }
                }
            } else {
                when (preferredOutputType) {
                    AudioRoutePreference.OUTPUT_SPEAKER -> manager.isSpeakerphoneOn = true
                    AudioRoutePreference.OUTPUT_EARPIECE,
                    AudioRoutePreference.OUTPUT_AUTO -> manager.isSpeakerphoneOn = false
                }
            }
        } catch (e: Exception) {
            AppLogger.e("AudioLoopbackTester output route failed", e)
        }
    }

    private fun pickPreferredInputDevice(devices: Array<AudioDeviceInfo>, preferredInputType: Int): AudioDeviceInfo? {
        if (preferredInputType == AudioRoutePreference.INPUT_AUTO) return null
        return devices.firstOrNull { device ->
            when (preferredInputType) {
                AudioRoutePreference.INPUT_BUILTIN_MIC -> device.type == AudioDeviceInfo.TYPE_BUILTIN_MIC
                AudioRoutePreference.INPUT_USB -> device.type == AudioDeviceInfo.TYPE_USB_DEVICE || device.type == AudioDeviceInfo.TYPE_USB_HEADSET
                AudioRoutePreference.INPUT_BLUETOOTH -> device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                AudioRoutePreference.INPUT_WIRED -> device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || device.type == AudioDeviceInfo.TYPE_LINE_ANALOG
                else -> false
            }
        }
    }

    private fun pickPreferredOutputDevice(devices: Array<AudioDeviceInfo>, preferredOutputType: Int): AudioDeviceInfo? {
        if (preferredOutputType == AudioRoutePreference.OUTPUT_AUTO) return null
        return devices.firstOrNull { device ->
            when (preferredOutputType) {
                AudioRoutePreference.OUTPUT_SPEAKER -> device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                AudioRoutePreference.OUTPUT_EARPIECE -> device.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                AudioRoutePreference.OUTPUT_BLUETOOTH -> device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                AudioRoutePreference.OUTPUT_USB -> device.type == AudioDeviceInfo.TYPE_USB_DEVICE || device.type == AudioDeviceInfo.TYPE_USB_HEADSET
                AudioRoutePreference.OUTPUT_WIRED -> device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_LINE_ANALOG
                else -> false
            }
        }
    }
}
