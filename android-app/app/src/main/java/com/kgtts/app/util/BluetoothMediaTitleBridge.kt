package com.lhtstudio.kigtts.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

object BluetoothMediaTitleBridge {
    private const val SESSION_TAG = "KIGTTS.BluetoothMediaTitle"
    private const val ARTIST = "KIGTTS"
    private const val ALBUM = "便捷字幕"
    private const val MAX_TITLE_LENGTH = 64
    private const val PLAYBACK_END_DWELL_MS = 5_000L
    private const val PLAYBACK_END_DWELL_REFRESH_INTERVAL_MS = 500L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var enabled = false
    private var session: MediaSession? = null
    private var lastTitle = ""
    private var lastUpdateAtMs = 0L
    private var dwellUntilMs = 0L
    private var dwellGeneration = 0L
    private var dwellFocusRelease: (() -> Unit)? = null

    fun setEnabled(context: Context, value: Boolean) {
        val appContext = context.applicationContext
        mainHandler.post {
            synchronized(lock) {
                enabled = value
                if (value) {
                    ensureSessionLocked(appContext)
                    if (lastTitle.isNotBlank()) {
                        publishTitleLocked(appContext, lastTitle, force = true)
                    }
                } else {
                    lastTitle = ""
                    lastUpdateAtMs = 0L
                    dwellUntilMs = 0L
                    dwellGeneration++
                    releaseDwellFocusLocked()
                    session?.isActive = false
                    session?.release()
                    session = null
                }
            }
        }
    }

    fun updateSubtitle(context: Context, text: String) {
        val title = normalizeTitle(text)
        if (title.isBlank()) return
        val appContext = context.applicationContext
        mainHandler.post {
            synchronized(lock) {
                if (!enabled) return@synchronized
                dwellUntilMs = 0L
                dwellGeneration++
                releaseDwellFocusLocked()
                publishTitleLocked(appContext, title, force = true)
            }
        }
    }

    fun extendAfterPlaybackEnd(context: Context) {
        val appContext = context.applicationContext
        mainHandler.post {
            synchronized(lock) {
                if (!enabled || lastTitle.isBlank()) return@synchronized
                val now = SystemClock.elapsedRealtime()
                dwellUntilMs = maxOf(dwellUntilMs, now + PLAYBACK_END_DWELL_MS)
                val generation = ++dwellGeneration
                publishTitleLocked(appContext, lastTitle, force = true)
                acquireDwellFocusLocked(appContext)
                postDwellRefreshLocked(appContext, generation)
            }
        }
    }

    private fun ensureSessionLocked(context: Context): MediaSession {
        session?.let { return it }
        val created = MediaSession(context.applicationContext, SESSION_TAG).apply {
            setPlaybackState(createPlayingState())
            isActive = true
        }
        session = created
        return created
    }

    private fun publishTitleLocked(context: Context, title: String, force: Boolean) {
        if (!force && title == lastTitle) return
        val activeSession = ensureSessionLocked(context)
        activeSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, ARTIST)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, ALBUM)
                .build()
        )
        activeSession.setPlaybackState(createPlayingState())
        activeSession.setQueueTitle(title)
        activeSession.isActive = true
        lastTitle = title
        lastUpdateAtMs = SystemClock.elapsedRealtime()
    }

    private fun postDwellRefreshLocked(context: Context, generation: Long) {
        val now = SystemClock.elapsedRealtime()
        val remaining = dwellUntilMs - now
        if (remaining <= 0L) return
        val delayMs = minOf(remaining, PLAYBACK_END_DWELL_REFRESH_INTERVAL_MS)
        mainHandler.postDelayed({
            synchronized(lock) {
                if (!enabled || generation != dwellGeneration || lastTitle.isBlank()) return@synchronized
                if (SystemClock.elapsedRealtime() >= dwellUntilMs) {
                    releaseDwellFocusLocked()
                    return@synchronized
                }
                publishTitleLocked(context, lastTitle, force = true)
                postDwellRefreshLocked(context, generation)
            }
        }, delayMs)
    }

    private fun acquireDwellFocusLocked(context: Context) {
        releaseDwellFocusLocked()
        val manager = context.getSystemService(AudioManager::class.java) ?: return
        val listener = AudioManager.OnAudioFocusChangeListener { }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(listener)
                .setWillPauseWhenDucked(false)
                .build()
            val granted = manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (granted) {
                dwellFocusRelease = { runCatching { manager.abandonAudioFocusRequest(request) } }
            } else {
                AppLogger.i("Bluetooth subtitle dwell audio focus denied")
            }
        } else {
            @Suppress("DEPRECATION")
            val granted = manager.requestAudioFocus(
                listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (granted) {
                dwellFocusRelease = {
                    @Suppress("DEPRECATION")
                    runCatching { manager.abandonAudioFocus(listener) }
                }
            } else {
                AppLogger.i("Bluetooth subtitle dwell audio focus denied")
            }
        }
    }

    private fun releaseDwellFocusLocked() {
        val release = dwellFocusRelease ?: return
        dwellFocusRelease = null
        release()
    }

    private fun createPlayingState(): PlaybackState {
        return PlaybackState.Builder()
            .setActions(0L)
            .setState(
                PlaybackState.STATE_PLAYING,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                1f,
                SystemClock.elapsedRealtime()
            )
            .build()
    }

    private fun normalizeTitle(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")
            .trim()
            .let { normalized ->
                if (normalized.length <= MAX_TITLE_LENGTH) {
                    normalized
                } else {
                    normalized.take(MAX_TITLE_LENGTH - 1) + "…"
                }
            }
    }
}
