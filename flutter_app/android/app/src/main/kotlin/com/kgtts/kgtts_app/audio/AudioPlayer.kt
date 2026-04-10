package com.kgtts.kgtts_app.audio

import android.content.Context
import android.media.*
import android.os.Build
import com.kgtts.kgtts_app.util.AppLogger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AudioPlayer(private val context: Context) {
    @Volatile var isPlaying: Boolean = false
        private set
    @Volatile private var useCommunicationAttributes: Boolean = false
    @Volatile private var preferredOutputType: Int = AudioRoutePreference.OUTPUT_AUTO
    @Volatile private var playbackGain: Float = 1.0f
    private var onOutputDevice: ((String) -> Unit)? = null
    private var onRender: ((FloatArray, Int, Int, Int) -> Unit)? = null

    fun setOnOutputDevice(callback: ((String) -> Unit)?) {
        onOutputDevice = callback
    }

    fun setOnRender(callback: ((FloatArray, Int, Int, Int) -> Unit)?) {
        onRender = callback
    }

    fun setUseCommunicationAttributes(enabled: Boolean) {
        useCommunicationAttributes = enabled
    }

    fun setPreferredOutputType(type: Int) {
        preferredOutputType = type
    }

    fun setPlaybackGainPercent(percent: Int) {
        playbackGain = (percent.coerceIn(0, 1000) / 100.0f).coerceAtLeast(0f)
    }

    fun play(samples: FloatArray, sampleRate: Int, onProgress: ((Float) -> Unit)? = null) {
        if (samples.isEmpty()) return
        val gain = playbackGain
        val scaledSamples = if (abs(gain - 1.0f) < 0.0001f) {
            samples
        } else {
            FloatArray(samples.size) { idx ->
                (samples[idx] * gain).coerceIn(-1f, 1f)
            }
        }
        val shorts = ShortArray(scaledSamples.size) { idx ->
            val v = max(-1f, min(1f, scaledSamples[idx])) * Short.MAX_VALUE
            v.toInt().toShort()
        }
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val usage = if (useCommunicationAttributes) {
            AudioAttributes.USAGE_VOICE_COMMUNICATION
        } else {
            AudioAttributes.USAGE_MEDIA
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(minBuf, 4096))
            .build()

        val audioManager = context.getSystemService(AudioManager::class.java)
        if (audioManager != null && Build.VERSION.SDK_INT >= 23) {
            try {
                val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val preferred = pickPreferredOutputDevice(outputs, preferredOutputType)
                if (preferred != null) {
                    val ok = track.setPreferredDevice(preferred)
                    AppLogger.i("Prefer output device: ${formatOutputDeviceLabel(preferred)} result=$ok")
                }
            } catch (e: Exception) {
                AppLogger.e("Prefer output device failed", e)
            }
        }

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                track.addOnRoutingChangedListener({ routing ->
                    val device = routing.routedDevice
                    onOutputDevice?.invoke(formatOutputDeviceLabel(device))
                }, null)
            } catch (_: Exception) {
            }
        }
        onOutputDevice?.invoke(formatOutputDeviceLabel(if (Build.VERSION.SDK_INT >= 24) track.routedDevice else null))

        isPlaying = true
        try {
            track.play()
            onProgress?.invoke(0f)
            val total = shorts.size
            var written = 0
            var lastReport = 0f
            while (written < total) {
                val count = min(2048, total - written)
                onRender?.invoke(scaledSamples, written, count, sampleRate)
                val w = track.write(shorts, written, count)
                if (w <= 0) break
                written += w
                val progress = written.toFloat() / total.toFloat()
                if (progress - lastReport >= 0.02f || written == total) {
                    lastReport = progress
                    onProgress?.invoke(progress)
                }
            }
        } finally {
            try {
                track.stop()
            } catch (_: Exception) {
            }
            try {
                track.release()
            } catch (_: Exception) {
            }
            isPlaying = false
            onProgress?.invoke(1f)
        }
    }
}
