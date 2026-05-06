package com.lhtstudio.kigtts.app.util

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

object BluetoothMediaTitleBridge {
    private const val SESSION_TAG = "KIGTTS.BluetoothMediaTitle"
    private const val ARTIST = "KIGTTS"
    private const val ALBUM = "便捷字幕"
    private const val MAX_TITLE_LENGTH = 64
    private const val UPDATE_INTERVAL_MS = 700L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var enabled = false
    private var session: MediaSession? = null
    private var lastTitle = ""
    private var lastUpdateAtMs = 0L
    private var pendingTitle: String? = null
    private var pendingPosted = false

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
                    pendingTitle = null
                    pendingPosted = false
                    lastTitle = ""
                    lastUpdateAtMs = 0L
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
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - lastUpdateAtMs
                if (title == lastTitle && elapsed < UPDATE_INTERVAL_MS) return@synchronized
                if (elapsed >= UPDATE_INTERVAL_MS || lastTitle.isBlank()) {
                    publishTitleLocked(appContext, title, force = true)
                } else {
                    pendingTitle = title
                    if (!pendingPosted) {
                        pendingPosted = true
                        mainHandler.postDelayed({
                            synchronized(lock) {
                                pendingPosted = false
                                val next = pendingTitle
                                pendingTitle = null
                                if (enabled && !next.isNullOrBlank()) {
                                    publishTitleLocked(appContext, next, force = true)
                                }
                            }
                        }, UPDATE_INTERVAL_MS - elapsed)
                    }
                }
            }
        }
    }

    private fun ensureSessionLocked(context: Context): MediaSession {
        session?.let { return it }
        val created = MediaSession(context.applicationContext, SESSION_TAG).apply {
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(0L)
                    .setState(
                        PlaybackState.STATE_PLAYING,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                        1f
                    )
                    .build()
            )
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
        activeSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(0L)
                .setState(
                    PlaybackState.STATE_PLAYING,
                    PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                    1f
                )
                .build()
        )
        activeSession.isActive = true
        lastTitle = title
        lastUpdateAtMs = SystemClock.elapsedRealtime()
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
