package com.kgtts.kgtts_app.audio

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class Aec3Processor(private val captureSampleRate: Int) {
    private val frameSize = max(1, captureSampleRate / 100)
    private val renderFrame = FloatArray(frameSize)
    private val captureFrame = FloatArray(frameSize)
    private val lock = Any()
    @Volatile private var handle: Long = nativeCreate(captureSampleRate, captureSampleRate, 1)

    fun isReady(): Boolean = handle != 0L

    fun processCapture(data: FloatArray, offset: Int, length: Int) {
        if (handle == 0L || length <= 0) return
        synchronized(lock) {
            val h = handle
            if (h == 0L || length <= 0) return
            var idx = 0
            while (idx < length) {
                val remaining = length - idx
                val chunk = min(frameSize, remaining)
                if (chunk == frameSize) {
                    nativeProcessCapture(h, data, offset + idx, chunk)
                } else {
                    java.util.Arrays.fill(captureFrame, 0f)
                    System.arraycopy(data, offset + idx, captureFrame, 0, chunk)
                    nativeProcessCapture(h, captureFrame, 0, frameSize)
                    System.arraycopy(captureFrame, 0, data, offset + idx, chunk)
                }
                idx += chunk
            }
        }
    }

    fun processRender(data: FloatArray, offset: Int, length: Int, inputRate: Int) {
        if (handle == 0L || length <= 0) return
        val src = if (inputRate == captureSampleRate) {
            data.copyOfRange(offset, offset + length)
        } else {
            resampleLinear(data, offset, length, inputRate, captureSampleRate)
        }
        if (src.isEmpty()) return
        synchronized(lock) {
            val h = handle
            if (h == 0L) return
            var idx = 0
            while (idx < src.size) {
                val remaining = src.size - idx
                val chunk = min(frameSize, remaining)
                if (chunk == frameSize) {
                    nativeProcessRender(h, src, idx, chunk)
                } else {
                    java.util.Arrays.fill(renderFrame, 0f)
                    System.arraycopy(src, idx, renderFrame, 0, chunk)
                    nativeProcessRender(h, renderFrame, 0, frameSize)
                }
                idx += chunk
            }
        }
    }

    fun release() {
        synchronized(lock) {
            val h = handle
            if (h != 0L) {
                nativeDestroy(h)
            }
            handle = 0L
        }
    }

    companion object {
        init {
            System.loadLibrary("aec3_jni")
        }
    }

    private external fun nativeCreate(captureSampleRate: Int, renderSampleRate: Int, channels: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeProcessCapture(handle: Long, data: FloatArray, offset: Int, length: Int)
    private external fun nativeProcessRender(handle: Long, data: FloatArray, offset: Int, length: Int)

    private fun resampleLinear(
        data: FloatArray,
        offset: Int,
        length: Int,
        inRate: Int,
        outRate: Int
    ): FloatArray {
        if (length <= 0 || inRate <= 0 || outRate <= 0) return FloatArray(0)
        if (inRate == outRate) {
            return data.copyOfRange(offset, offset + length)
        }
        val ratio = outRate.toDouble() / inRate.toDouble()
        val outLen = max(1, (length * ratio).roundToInt())
        val out = FloatArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = i / ratio
            val idx = srcPos.toInt()
            val frac = (srcPos - idx)
            val i0 = offset + idx
            val i1 = min(offset + length - 1, i0 + 1)
            val s0 = data[i0]
            val s1 = data[i1]
            out[i] = (s0 + (s1 - s0) * frac.toFloat())
        }
        return out
    }
}
