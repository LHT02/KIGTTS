package com.lhtstudio.kigtts.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.util.AppLogger

class PlaybackAudioFocusController(
    context: Context,
    private val contentType: Int
) {
    @Volatile
    private var mode: Int = UserPrefs.AUDIO_FOCUS_AVOID_NONE
    private val appContext = context.applicationContext
    private val audioManager: AudioManager? = appContext.getSystemService(AudioManager::class.java)

    fun setMode(value: Int) {
        mode = UserPrefs.normalizeAudioFocusAvoidanceMode(value)
    }

    fun acquire(): Lease? {
        val manager = audioManager ?: return null
        val focusGain = when (mode) {
            UserPrefs.AUDIO_FOCUS_AVOID_DUCK -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            UserPrefs.AUDIO_FOCUS_AVOID_MUTE -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            UserPrefs.AUDIO_FOCUS_AVOID_PAUSE -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            else -> return null
        }
        val listener = AudioManager.OnAudioFocusChangeListener { }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(focusGain)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(contentType)
                        .build()
                )
                .setOnAudioFocusChangeListener(listener)
                .setWillPauseWhenDucked(false)
                .build()
            val granted = manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (granted) {
                Lease { runCatching { manager.abandonAudioFocusRequest(request) } }
            } else {
                AppLogger.i("Audio focus request denied mode=$mode focusGain=$focusGain")
                null
            }
        } else {
            @Suppress("DEPRECATION")
            val granted = manager.requestAudioFocus(
                listener,
                AudioManager.STREAM_MUSIC,
                focusGain
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (granted) {
                Lease {
                    @Suppress("DEPRECATION")
                    runCatching { manager.abandonAudioFocus(listener) }
                }
            } else {
                AppLogger.i("Audio focus request denied mode=$mode focusGain=$focusGain")
                null
            }
        }
    }

    class Lease internal constructor(
        private val releaseAction: () -> Unit
    ) {
        private var released = false

        fun release() {
            if (released) return
            released = true
            releaseAction()
        }
    }
}
