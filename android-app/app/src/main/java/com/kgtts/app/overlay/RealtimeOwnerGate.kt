package com.kgtts.app.overlay

import java.util.concurrent.atomic.AtomicReference

object RealtimeOwnerGate {
    private val owner = AtomicReference<String?>(null)

    fun acquire(tag: String): Boolean {
        val current = owner.get()
        if (current == tag) return true
        return owner.compareAndSet(null, tag)
    }

    fun release(tag: String) {
        owner.compareAndSet(tag, null)
    }

    fun currentOwner(): String? = owner.get()
}
