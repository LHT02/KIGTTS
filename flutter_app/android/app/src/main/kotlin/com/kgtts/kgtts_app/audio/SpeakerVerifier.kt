package com.kgtts.kgtts_app.audio

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sqrt

data class SpeakerEnrollResult(
    val success: Boolean,
    val message: String,
    val profile: FloatArray? = null
)

internal object SpeakerVerifier {
    private const val FRAME_SIZE = 128
    private const val HOP_SIZE = 64
    private const val MIN_VOICED_FRAMES = 6
    private const val MIN_RMS = 0.01f
    private const val MAX_ANALYZE_SAMPLES = 24000 // ~1.5s @16kHz

    // 0-4kHz coarse bands for 16kHz speech.
    private val bandRanges = arrayOf(
        1..2,   // 125-250
        3..4,   // 375-500
        5..8,   // 625-1000
        9..16,  // 1125-2000
        17..24, // 2125-3000
        25..32  // 3125-4000
    )

    private val cosTable: Array<DoubleArray> by lazy {
        Array(FRAME_SIZE / 2 + 1) { k ->
            DoubleArray(FRAME_SIZE) { n ->
                cos(2.0 * Math.PI * k * n / FRAME_SIZE)
            }
        }
    }

    private val sinTable: Array<DoubleArray> by lazy {
        Array(FRAME_SIZE / 2 + 1) { k ->
            DoubleArray(FRAME_SIZE) { n ->
                kotlin.math.sin(2.0 * Math.PI * k * n / FRAME_SIZE)
            }
        }
    }

    private val hammingWindow: FloatArray by lazy {
        FloatArray(FRAME_SIZE) { i ->
            (0.54 - 0.46 * cos(2.0 * Math.PI * i / (FRAME_SIZE - 1))).toFloat()
        }
    }

    fun computeEmbedding(samples: FloatArray, sampleRate: Int): FloatArray? {
        if (sampleRate <= 0 || samples.size < FRAME_SIZE) return null
        val usable = min(samples.size, MAX_ANALYZE_SAMPLES)
        val clipped = samples.copyOfRange(0, usable)
        val frameFeatures = ArrayList<FloatArray>(usable / HOP_SIZE + 1)
        var idx = 0
        while (idx + FRAME_SIZE <= clipped.size) {
            val frame = FloatArray(FRAME_SIZE)
            var mean = 0f
            for (i in 0 until FRAME_SIZE) {
                mean += clipped[idx + i]
            }
            mean /= FRAME_SIZE.toFloat()
            var sumSq = 0.0
            for (i in 0 until FRAME_SIZE) {
                val v = (clipped[idx + i] - mean) * hammingWindow[i]
                frame[i] = v
                sumSq += v * v
            }
            val rms = sqrt(sumSq / FRAME_SIZE).toFloat()
            if (rms >= MIN_RMS) {
                val zcr = frameZcr(frame)
                val bands = frameBandEnergies(frame)
                val feat = FloatArray(2 + bands.size)
                feat[0] = rms
                feat[1] = zcr
                for (b in bands.indices) {
                    feat[2 + b] = bands[b]
                }
                frameFeatures.add(feat)
            }
            idx += HOP_SIZE
        }
        if (frameFeatures.size < MIN_VOICED_FRAMES) return null
        return aggregateFeatures(frameFeatures)
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val n = min(a.size, b.size)
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in 0 until n) {
            val av = a[i].toDouble()
            val bv = b[i].toDouble()
            dot += av * bv
            na += av * av
            nb += bv * bv
        }
        if (na <= 1e-12 || nb <= 1e-12) return 0f
        return (dot / (sqrt(na) * sqrt(nb))).toFloat().coerceIn(-1f, 1f)
    }

    private fun frameZcr(frame: FloatArray): Float {
        var count = 0
        for (i in 1 until frame.size) {
            val a = frame[i - 1]
            val b = frame[i]
            if ((a >= 0f && b < 0f) || (a < 0f && b >= 0f)) {
                count++
            }
        }
        return count.toFloat() / frame.size.toFloat()
    }

    private fun frameBandEnergies(frame: FloatArray): FloatArray {
        val bins = FRAME_SIZE / 2
        val spectrum = FloatArray(bins + 1)
        for (k in 1..bins) {
            var re = 0.0
            var im = 0.0
            val cosK = cosTable[k]
            val sinK = sinTable[k]
            for (n in frame.indices) {
                val v = frame[n].toDouble()
                re += v * cosK[n]
                im -= v * sinK[n]
            }
            spectrum[k] = (re * re + im * im).toFloat()
        }
        val out = FloatArray(bandRanges.size)
        for (i in bandRanges.indices) {
            val range = bandRanges[i]
            var sum = 0.0
            var cnt = 0
            for (k in range) {
                if (k in 0..bins) {
                    sum += spectrum[k]
                    cnt++
                }
            }
            val v = if (cnt > 0) sum / cnt else 0.0
            out[i] = ln(1.0 + v).toFloat()
        }
        return out
    }

    private fun aggregateFeatures(features: List<FloatArray>): FloatArray {
        val dim = features.first().size
        val mean = FloatArray(dim)
        val std = FloatArray(dim)
        for (feat in features) {
            for (i in 0 until dim) {
                mean[i] += feat[i]
            }
        }
        for (i in 0 until dim) {
            mean[i] /= features.size.toFloat()
        }
        for (feat in features) {
            for (i in 0 until dim) {
                val d = feat[i] - mean[i]
                std[i] += d * d
            }
        }
        for (i in 0 until dim) {
            std[i] = sqrt(std[i] / features.size.toFloat())
        }
        val out = FloatArray(dim * 2)
        for (i in 0 until dim) {
            out[i] = mean[i]
            out[i + dim] = std[i]
        }
        normalizeInPlace(out)
        return out
    }

    private fun normalizeInPlace(v: FloatArray) {
        var sumSq = 0.0
        for (x in v) {
            sumSq += x * x
        }
        val norm = sqrt(sumSq)
        if (norm <= 1e-8) return
        for (i in v.indices) {
            v[i] = (v[i] / norm).toFloat()
        }
    }
}
