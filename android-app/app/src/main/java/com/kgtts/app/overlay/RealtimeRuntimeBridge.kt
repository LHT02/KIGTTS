package com.kgtts.app.overlay

import java.util.concurrent.CopyOnWriteArraySet

object RealtimeRuntimeBridge {
    const val APP_OWNER_TAG = "app"

    data class Snapshot(
        val running: Boolean = false,
        val latestRecognizedText: String = "",
        val inputLevel: Float = 0f,
        val playbackProgress: Float = 0f,
        val inputDeviceLabel: String = "",
        val outputDeviceLabel: String = ""
    )

    interface AppDelegate {
        fun startRealtime()
        fun stopRealtime()
        fun submitQuickSubtitle(target: String, text: String)
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
