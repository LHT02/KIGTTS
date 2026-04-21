package com.lhtstudio.kigtts.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object QqScannerSupport {
    const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"
    const val QQ_LAUNCHER_ACTIVITY = "com.tencent.mobileqq.activity.SplashActivity"
    const val QQ_BROWSER_FALLBACK_URL = "https://im.qq.com/"

    fun isQqQrContent(raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false
        val parsed = runCatching { Uri.parse(text) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase(Locale.US).orEmpty()
        val host = parsed?.host?.lowercase(Locale.US).orEmpty()
        if (
            scheme == "mqq" ||
            scheme == "mqqapi" ||
            scheme == "mqqopensdkapi" ||
            scheme == "mqqwpa" ||
            scheme == "qqwallet" ||
            scheme == "qpay"
        ) {
            return true
        }
        if (
            host.matchesQqQrHost("qm.qq.com") ||
            host.matchesQqQrHost("ti.qq.com") ||
            host.matchesQqQrHost("qun.qq.com") ||
            host.matchesQqQrHost("qianbao.qq.com") ||
            host.matchesQqQrHost("pay.qq.com") ||
            host.matchesQqQrHost("qpay.qq.com") ||
            host.matchesQqQrHost("mqq.tenpay.com")
        ) {
            return true
        }
        val lower = text.lowercase(Locale.US)
        return lower.startsWith("https://qm.qq.com/") ||
            lower.startsWith("http://qm.qq.com/") ||
            lower.startsWith("https://ti.qq.com/") ||
            lower.startsWith("http://ti.qq.com/") ||
            lower.startsWith("https://qun.qq.com/") ||
            lower.startsWith("http://qun.qq.com/") ||
            lower.startsWith("https://qianbao.qq.com/") ||
            lower.startsWith("http://qianbao.qq.com/") ||
            lower.startsWith("https://pay.qq.com/") ||
            lower.startsWith("http://pay.qq.com/") ||
            lower.startsWith("https://qpay.qq.com/") ||
            lower.startsWith("http://qpay.qq.com/") ||
            lower.startsWith("https://mqq.tenpay.com/") ||
            lower.startsWith("http://mqq.tenpay.com/")
    }

    fun launchQq(context: Context): Boolean {
        val explicit = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(QQ_PACKAGE_NAME, QQ_LAUNCHER_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        if (
            runCatching {
                context.startActivity(explicit)
                true
            }.getOrDefault(false)
        ) {
            return true
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(QQ_PACKAGE_NAME)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) }
        return runCatching {
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    private fun String.matchesQqQrHost(host: String): Boolean {
        return this == host || this.endsWith(".$host")
    }
}
