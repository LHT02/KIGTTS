package com.lhtstudio.kigtts.app.ui

import android.app.Activity
import android.os.Bundle
import com.lhtstudio.kigtts.app.util.LauncherMenuShortcuts

class LauncherShortcutProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntentAndFinish()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentAndFinish()
    }

    private fun handleIntentAndFinish() {
        LauncherMenuShortcuts.handleProxyIntent(this, intent)
        finish()
    }
}
