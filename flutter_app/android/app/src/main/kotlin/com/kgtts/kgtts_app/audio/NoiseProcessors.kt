package com.kgtts.kgtts_app.audio

import com.kgtts.kgtts_app.util.AppLogger
import kotlin.math.min

object AudioDenoiserMode {
    const val OFF = 0
    const val RNNOISE = 1
    const val SPEEX = 2

    fun normalize(mode: Int): Int {
        return mode.coerceIn(OFF, SPEEX)
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

    fun release() {
        synchronized(lock) {
            if (released) return
            released = true
            processor.reset()
            SpeexNative.nativeDestroy(handle)
        }
    }
}
