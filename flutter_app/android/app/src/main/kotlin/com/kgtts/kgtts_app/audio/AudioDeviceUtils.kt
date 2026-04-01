package com.kgtts.kgtts_app.audio

import android.media.AudioDeviceInfo

fun pickPreferredInputDevice(devices: Array<AudioDeviceInfo>, pref: Int): AudioDeviceInfo? {
    if (pref == AudioRoutePreference.INPUT_AUTO) return null
    fun find(types: Set<Int>): AudioDeviceInfo? = devices.firstOrNull { it.type in types }
    return when (pref) {
        AudioRoutePreference.INPUT_BUILTIN_MIC -> find(
            setOf(AudioDeviceInfo.TYPE_BUILTIN_MIC, AudioDeviceInfo.TYPE_TELEPHONY)
        )
        AudioRoutePreference.INPUT_USB -> find(
            setOf(AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET)
        )
        AudioRoutePreference.INPUT_BLUETOOTH -> find(
            setOf(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET)
        )
        AudioRoutePreference.INPUT_WIRED -> find(
            setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_LINE_ANALOG)
        )
        else -> null
    }
}

fun pickPreferredOutputDevice(devices: Array<AudioDeviceInfo>, pref: Int): AudioDeviceInfo? {
    if (pref == AudioRoutePreference.OUTPUT_AUTO) return null
    fun find(types: Set<Int>): AudioDeviceInfo? = devices.firstOrNull { it.type in types }
    return when (pref) {
        AudioRoutePreference.OUTPUT_SPEAKER -> find(
            setOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE)
        )
        AudioRoutePreference.OUTPUT_EARPIECE -> find(
            setOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        )
        AudioRoutePreference.OUTPUT_BLUETOOTH -> find(
            setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET)
        )
        AudioRoutePreference.OUTPUT_USB -> find(
            setOf(AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET)
        )
        AudioRoutePreference.OUTPUT_WIRED -> find(
            setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_LINE_ANALOG)
        )
        else -> null
    }
}

fun formatInputDeviceLabel(device: AudioDeviceInfo?): String {
    if (device == null) return "未知"
    val typeName = when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "内置麦克风"
        AudioDeviceInfo.TYPE_TELEPHONY -> "话筒"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB麦克风"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET -> "蓝牙麦克风"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_LINE_ANALOG -> "有线麦克风"
        else -> "设备(${device.type})"
    }
    val name = device.productName?.toString()?.trim().orEmpty()
    return if (name.isNotEmpty()) "$typeName - $name" else typeName
}

fun formatOutputDeviceLabel(device: AudioDeviceInfo?): String {
    if (device == null) return "未知"
    val typeName = when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "扬声器"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "听筒"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_LINE_ANALOG -> "有线耳机"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET -> "蓝牙耳机"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB音频"
        else -> "设备(${device.type})"
    }
    val name = device.productName?.toString()?.trim().orEmpty()
    return if (name.isNotEmpty()) "$typeName - $name" else typeName
}

fun pickOutputDeviceLabel(devices: Array<AudioDeviceInfo>): String {
    if (devices.isEmpty()) return "未知"
    fun find(types: Set<Int>): AudioDeviceInfo? = devices.firstOrNull { it.type in types }
    val device = find(setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
        ?: find(setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
        ?: find(setOf(AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET))
        ?: find(setOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        ?: find(setOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE))
        ?: devices.first()
    return formatOutputDeviceLabel(device)
}
