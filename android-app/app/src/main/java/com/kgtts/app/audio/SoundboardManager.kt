package com.lhtstudio.kigtts.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.lhtstudio.kigtts.app.data.SoundboardConfig
import com.lhtstudio.kigtts.app.data.SoundboardItem
import com.lhtstudio.kigtts.app.data.UserPrefs
import com.lhtstudio.kigtts.app.data.defaultSoundboardConfig
import com.lhtstudio.kigtts.app.data.parseSoundboardConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.random.Random

data class SoundboardPlaybackState(
    val playing: Boolean = false,
    val progress: Float = 0f
)

object SoundboardManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stateMutex = Mutex()
    private val playbackStates = MutableStateFlow<Map<Long, SoundboardPlaybackState>>(emptyMap())
    private val players = linkedMapOf<Long, ActivePlayback>()

    @Volatile
    private var cachedConfig: SoundboardConfig = defaultSoundboardConfig()
    @Volatile
    private var hasLoadedConfig = false

    private data class ActivePlayback(
        val itemId: Long,
        val player: MediaPlayer,
        val pollJob: Job,
        val stopJob: Job?
    )

    fun playbackState(): StateFlow<Map<Long, SoundboardPlaybackState>> = playbackStates.asStateFlow()

    suspend fun loadConfig(context: Context): SoundboardConfig {
        val parsed = parseSoundboardConfig(UserPrefs.getSoundboardConfig(context))
        updateCachedConfig(parsed)
        return parsed
    }

    fun updateCachedConfig(config: SoundboardConfig) {
        cachedConfig = config
        hasLoadedConfig = true
        scope.launch {
            cleanupStalePlaybacks(config)
        }
    }

    fun cachedOrDefaultConfig(): SoundboardConfig = cachedConfig

    suspend fun stop(itemId: Long) {
        stateMutex.withLock {
            releasePlaybackLocked(itemId)
        }
    }

    suspend fun stopAll() {
        stateMutex.withLock {
            players.keys.toList().forEach { itemId ->
                releasePlaybackLocked(itemId)
            }
        }
    }

    suspend fun play(item: SoundboardItem): Boolean {
        val path = item.audioPath.trim()
        if (path.isEmpty()) return false
        val targetFile = File(path)
        if (!targetFile.exists()) return false
        stateMutex.withLock {
            releasePlaybackLocked(item.id)
            val mediaPlayer = MediaPlayer()
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            mediaPlayer.setAudioAttributes(audioAttrs)
            mediaPlayer.setDataSource(targetFile.absolutePath)
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration.coerceAtLeast(0)
            val trimStart = item.trimStartMs.coerceIn(0L, duration.toLong()).toInt()
            val trimEnd = when {
                item.trimEndMs > item.trimStartMs -> item.trimEndMs.coerceIn(item.trimStartMs, duration.toLong()).toInt()
                else -> duration
            }
            if (trimStart > 0) {
                mediaPlayer.seekTo(trimStart)
            }
            val pollJob = scope.launch {
                while (true) {
                    val progress = runCatching {
                        val current = mediaPlayer.currentPosition
                        val denom = (trimEnd - trimStart).coerceAtLeast(1)
                        ((current - trimStart).toFloat() / denom.toFloat()).coerceIn(0f, 1f)
                    }.getOrDefault(0f)
                    updatePlaybackState(item.id, SoundboardPlaybackState(playing = true, progress = progress))
                    delay(48L)
                }
            }
            val stopJob = if (trimEnd in 1 until duration) {
                scope.launch {
                    while (true) {
                        delay(24L)
                        val current = runCatching { mediaPlayer.currentPosition }.getOrDefault(trimEnd)
                        if (current >= trimEnd) {
                            stop(item.id)
                            break
                        }
                    }
                }
            } else {
                null
            }
            mediaPlayer.setOnCompletionListener {
                scope.launch { stop(item.id) }
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                scope.launch { stop(item.id) }
                true
            }
            mediaPlayer.start()
            players[item.id] = ActivePlayback(
                itemId = item.id,
                player = mediaPlayer,
                pollJob = pollJob,
                stopJob = stopJob
            )
            setPlaybackStateLocked(item.id, SoundboardPlaybackState(playing = true, progress = 0f))
        }
        return true
    }

    suspend fun triggerByText(context: Context, text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        val config = if (!hasLoadedConfig) loadConfig(context) else cachedConfig
        val matchesByWakeWord = linkedMapOf<String, MutableList<SoundboardItem>>()
        config.groups.forEach { group ->
            if (!group.keywordWakeEnabled) return@forEach
            group.items.forEach { item ->
                val wakeWord = item.wakeWord.trim()
                if (wakeWord.isNotEmpty() &&
                    normalized.contains(wakeWord) &&
                    item.audioPath.isNotBlank() &&
                    File(item.audioPath).exists()
                ) {
                    matchesByWakeWord.getOrPut(wakeWord) { mutableListOf() } += item
                }
            }
        }
        matchesByWakeWord.values.forEach { candidates ->
            val selected = if (candidates.size == 1) candidates.first() else candidates.random(Random.Default)
            play(selected)
        }
    }

    private suspend fun cleanupStalePlaybacks(config: SoundboardConfig) {
        val validItemIds = config.groups.asSequence()
            .flatMap { it.items.asSequence() }
            .map { it.id }
            .toSet()
        stateMutex.withLock {
            players.keys.filterNot { it in validItemIds }.forEach(::releasePlaybackLocked)
            val staleStateIds = playbackStates.value.keys.filterNot { it in validItemIds }
            if (staleStateIds.isNotEmpty()) {
                val next = playbackStates.value.toMutableMap()
                staleStateIds.forEach { next.remove(it) }
                playbackStates.value = next
            }
        }
    }

    private suspend fun updatePlaybackState(itemId: Long, state: SoundboardPlaybackState) {
        stateMutex.withLock {
            if (state.playing && !players.containsKey(itemId)) return
            setPlaybackStateLocked(itemId, state)
        }
    }

    private fun setPlaybackStateLocked(itemId: Long, state: SoundboardPlaybackState) {
        val next = playbackStates.value.toMutableMap()
        next[itemId] = state
        playbackStates.value = next
    }

    private fun releasePlaybackLocked(itemId: Long) {
        val existing = players.remove(itemId) ?: run {
            if (playbackStates.value.containsKey(itemId)) {
                setPlaybackStateLocked(itemId, SoundboardPlaybackState(playing = false, progress = 0f))
            }
            return
        }
        existing.pollJob.cancel()
        existing.stopJob?.cancel()
        runCatching {
            existing.player.setOnCompletionListener(null)
            existing.player.setOnErrorListener(null)
            if (existing.player.isPlaying) existing.player.stop()
        }
        runCatching { existing.player.release() }
        setPlaybackStateLocked(itemId, SoundboardPlaybackState(playing = false, progress = 0f))
    }
}
