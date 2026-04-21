package com.lhtstudio.kigtts.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object AlipayScannerSupport {
    const val ALIPAY_PACKAGE_NAME = "com.eg.android.AlipayGphone"
    const val ALIPAY_SCANNER_URI = "alipayqr://platformapi/startapp?saId=10000007"
    const val ALIPAY_BROWSER_FALLBACK_URL = "https://www.alipay.com/"

    fun isAlipayQrContent(raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false
        val parsed = runCatching { Uri.parse(text) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase(Locale.US).orEmpty()
        val host = parsed?.host?.lowercase(Locale.US).orEmpty()
        if (scheme == "alipay" || scheme == "alipayqr" || scheme == "alipays") return true
        if (
            host.matchesAlipayQrHost("qr.alipay.com") ||
            host.matchesAlipayQrHost("render.alipay.com") ||
            host.matchesAlipayQrHost("openapi.alipay.com") ||
            host.matchesAlipayQrHost("mapi.alipay.com")
        ) {
            return true
        }
        val lower = text.lowercase(Locale.US)
        return lower.startsWith("https://qr.alipay.com/") ||
            lower.startsWith("http://qr.alipay.com/") ||
            lower.startsWith("https://render.alipay.com/") ||
            lower.startsWith("http://render.alipay.com/") ||
            lower.startsWith("https://openapi.alipay.com/") ||
            lower.startsWith("http://openapi.alipay.com/") ||
            lower.startsWith("https://mapi.alipay.com/") ||
            lower.startsWith("http://mapi.alipay.com/")
    }

    fun launchScanner(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ALIPAY_SCANNER_URI)).apply {
            setPackage(ALIPAY_PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (
            runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        ) {
            return true
        }
        val fallbackIntent = context.packageManager.getLaunchIntentForPackage(ALIPAY_PACKAGE_NAME)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return runCatching {
            if (fallbackIntent != null) {
                context.startActivity(fallbackIntent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    private fun String.matchesAlipayQrHost(host: String): Boolean {
        return this == host || this.endsWith(".$host")
    }
}
