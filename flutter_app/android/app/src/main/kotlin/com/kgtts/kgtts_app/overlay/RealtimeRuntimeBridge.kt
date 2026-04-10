package com.kgtts.kgtts_app.overlay

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Bridge between the app's realtime engine and the floating overlay service.
 * Allows the overlay to observe and control the realtime ASR/TTS pipeline.
 */
object RealtimeRuntimeBridge {
    const val APP_OWNER_TAG = "app"

    enum class PttCommitAction {
        SendToSubtitle,
        SendToInput,
        Cancel
    }

    data class Snapshot(
        val running: Boolean = false,
        val latestRecognizedText: String = "",
        val inputLevel: Float = 0f,
        val playbackProgress: Float = 0f,
        val inputDeviceLabel: String = "",
        val outputDeviceLabel: String = "",
        val pushToTalkPressed: Boolean = false,
        val pushToTalkStreamingText: String = ""
    )

    interface AppDelegate {
        fun startRealtime()
        fun stopRealtime()
        fun submitQuickSubtitle(target: String, text: String)
        fun beginPushToTalkSession()
        fun setPushToTalkPressed(pressed: Boolean)
        fun commitPushToTalkSession(action: PttCommitAction)
    }

    interface Listener {
        fun onAppRuntimeChanged()
    }

    @Volatile
    private var snapshot: Snapshot = Snapshot()

    @Volatile
    private var appDelegate: AppDelegate? = null

    private val listeners = CopyOnWriteArraySet<Listener>()

    fun registerAppDelegate(delegate: AppDelegate) {
        appDelegate = delegate
        notifyChanged()
    }

    fun unregisterAppDelegate(delegate: AppDelegate) {
        if (appDelegate === delegate) {
            appDelegate = null
            notifyChanged()
        }
    }

    fun updateAppSnapshot(next: Snapshot) {
        snapshot = next
        notifyChanged()
    }

    fun currentSnapshot(): Snapshot = snapshot

    fun currentAppDelegate(): AppDelegate? = appDelegate

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    private fun notifyChanged() {
        listeners.forEach { it.onAppRuntimeChanged() }
    }
}
