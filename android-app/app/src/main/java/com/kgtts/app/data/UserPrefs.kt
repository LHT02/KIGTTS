package com.lhtstudio.kigtts.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lhtstudio.kigtts.app.audio.AudioDenoiserMode
import com.lhtstudio.kigtts.app.audio.AudioRoutePreference
import com.lhtstudio.kigtts.app.audio.SpeechEnhancementMode
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActionSpec
import com.lhtstudio.kigtts.app.util.VolumeHotkeyActions
import com.lhtstudio.kigtts.app.util.VolumeHotkeySequence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPrefs {
    const val DRAWER_MODE_HIDDEN = 0
    const val DRAWER_MODE_PERMANENT = 1
    const val THEME_MODE_FOLLOW_SYSTEM = 0
    const val THEME_MODE_LIGHT = 1
    const val THEME_MODE_DARK = 2
    const val FONT_SCALE_BLOCK_NONE = 0
    const val FONT_SCALE_BLOCK_ICONS_ONLY = 1
    const val FONT_SCALE_BLOCK_ALL = 2
    const val DEFAULT_DRAWING_SAVE_RELATIVE_PATH = "Pictures/KGTTS/Drawings"
    const val SILERO_VAD_MIN_THRESHOLD = 0.05f
    const val SILERO_VAD_MAX_THRESHOLD = 0.95f
    const val SILERO_VAD_DEFAULT_THRESHOLD = 0.5f
    const val SILERO_VAD_MIN_PRE_ROLL_MS = 0
    const val SILERO_VAD_MAX_PRE_ROLL_MS = 800
    const val SILERO_VAD_DEFAULT_PRE_ROLL_MS = 100
    const val VOLUME_HOTKEY_MIN_WINDOW_MS = 500
    const val VOLUME_HOTKEY_MAX_WINDOW_MS = 3000
    const val VOLUME_HOTKEY_DEFAULT_WINDOW_MS = 1500
    const val RECOGNITION_RESOURCE_SOURCE_MODELSCOPE = 0
    const val RECOGNITION_RESOURCE_SOURCE_HUGGINGFACE = 1
    const val DEFAULT_RECOGNITION_RESOURCE_MODELSCOPE_URL =
        "https://modelscope.cn/models/LHTSTUDIO/KIGTTS_ASR_Resource/resolve/master/kigtts-recognition-resources-20260505.7z"
    const val DEFAULT_RECOGNITION_RESOURCE_HUGGINGFACE_URL =
        "https://huggingface.co/LHT02/KIGTTS_ASR_Resource/resolve/main/kigtts-recognition-resources-20260505.7z"

    private val KEY_LAST_ASR = stringPreferencesKey("last_asr_name")
    private val KEY_LAST_VOICE = stringPreferencesKey("last_voice_name")
    private val KEY_SYSTEM_TTS_ORDER = longPreferencesKey("system_tts_order")
    private val KEY_MUTE_WHILE_PLAYING = booleanPreferencesKey("mute_while_playing")
    private val KEY_MUTE_DELAY_SEC = floatPreferencesKey("mute_delay_sec")
    private val KEY_ECHO_SUPPRESSION = booleanPreferencesKey("echo_suppression")
    private val KEY_COMMUNICATION_MODE = booleanPreferencesKey("communication_mode")
    private val KEY_COMMUNICATION_SPEAKER = booleanPreferencesKey("communication_speaker")
    private val KEY_PREFER_USB_MIC = booleanPreferencesKey("prefer_usb_mic")
    private val KEY_PREFERRED_INPUT_TYPE = intPreferencesKey("preferred_input_type")
    private val KEY_PREFERRED_OUTPUT_TYPE = intPreferencesKey("preferred_output_type")
    private val KEY_AEC3_ENABLED = booleanPreferencesKey("aec3_enabled")
    private val KEY_DENOISER_MODE = intPreferencesKey("denoiser_mode")
    private val KEY_SPEECH_ENHANCEMENT_ENABLED = booleanPreferencesKey("speech_enhancement_enabled")
    private val KEY_SPEECH_ENHANCEMENT_MODE = intPreferencesKey("speech_enhancement_mode")
    private val KEY_CLASSIC_VAD_ENABLED = booleanPreferencesKey("classic_vad_enabled")
    private val KEY_SILERO_VAD_ENABLED = booleanPreferencesKey("silero_vad_enabled")
    private val KEY_SILERO_VAD_THRESHOLD = floatPreferencesKey("silero_vad_threshold")
    private val KEY_SILERO_VAD_PRE_ROLL_MS = intPreferencesKey("silero_vad_pre_roll_ms")
    private val KEY_RECOGNITION_RESOURCE_MODELSCOPE_URL =
        stringPreferencesKey("recognition_resource_modelscope_url")
    private val KEY_RECOGNITION_RESOURCE_HUGGINGFACE_URL =
        stringPreferencesKey("recognition_resource_huggingface_url")
    private val KEY_RECOGNITION_RESOURCE_PREFERRED_SOURCE =
        intPreferencesKey("recognition_resource_preferred_source")
    private val KEY_MIN_VOLUME_PERCENT = intPreferencesKey("min_volume_percent")
    private val KEY_PLAYBACK_GAIN_PERCENT = intPreferencesKey("playback_gain_percent")
    private val KEY_PIPER_NOISE_SCALE = floatPreferencesKey("piper_noise_scale")
    private val KEY_PIPER_LENGTH_SCALE = floatPreferencesKey("piper_length_scale")
    private val KEY_PIPER_NOISE_W = floatPreferencesKey("piper_noise_w")
    private val KEY_PIPER_SENTENCE_SILENCE = floatPreferencesKey("piper_sentence_silence")
    private val KEY_KEEP_ALIVE = booleanPreferencesKey("keep_alive")
    private val KEY_NUMBER_REPLACE_MODE = intPreferencesKey("number_replace_mode")
    private val KEY_LANDSCAPE_DRAWER_MODE = intPreferencesKey("landscape_drawer_mode")
    private val KEY_SOLID_TOP_BAR = booleanPreferencesKey("solid_top_bar")
    private val KEY_THEME_MODE = intPreferencesKey("theme_mode")
    private val KEY_OVERLAY_THEME_MODE = intPreferencesKey("overlay_theme_mode")
    private val KEY_FONT_SCALE_BLOCK_MODE = intPreferencesKey("font_scale_block_mode")
    private val KEY_HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
    private val KEY_DRAWING_SAVE_RELATIVE_PATH = stringPreferencesKey("drawing_save_relative_path")
    private val KEY_QUICK_CARD_AUTO_SAVE_ON_EXIT = booleanPreferencesKey("quick_card_auto_save_on_exit")
    private val KEY_USE_BUILTIN_FILE_MANAGER = booleanPreferencesKey("use_builtin_file_manager")
    private val KEY_USE_BUILTIN_GALLERY = booleanPreferencesKey("use_builtin_gallery")
    private val KEY_ASR_SEND_TO_QUICK_SUBTITLE = booleanPreferencesKey("asr_send_to_quick_subtitle")
    private val KEY_PUSH_TO_TALK_MODE = booleanPreferencesKey("push_to_talk_mode")
    private val KEY_PUSH_TO_TALK_CONFIRM_INPUT = booleanPreferencesKey("push_to_talk_confirm_input")
    private val KEY_FLOATING_OVERLAY_ENABLED = booleanPreferencesKey("floating_overlay_enabled")
    private val KEY_FLOATING_OVERLAY_AUTO_DOCK = booleanPreferencesKey("floating_overlay_auto_dock")
    private val KEY_FLOATING_OVERLAY_SHOW_ON_LOCK_SCREEN =
        booleanPreferencesKey("floating_overlay_show_on_lock_screen")
    private val KEY_FLOATING_OVERLAY_HARDCODED_SHORTCUT_SUPPLEMENT =
        booleanPreferencesKey("floating_overlay_hardcoded_shortcut_supplement")
    private val KEY_VOLUME_HOTKEY_UP_DOWN_ENABLED = booleanPreferencesKey("volume_hotkey_up_down_enabled")
    private val KEY_VOLUME_HOTKEY_DOWN_UP_ENABLED = booleanPreferencesKey("volume_hotkey_down_up_enabled")
    private val KEY_VOLUME_HOTKEY_UP_DOWN_ACTION = stringPreferencesKey("volume_hotkey_up_down_action")
    private val KEY_VOLUME_HOTKEY_DOWN_UP_ACTION = stringPreferencesKey("volume_hotkey_down_up_action")
    private val KEY_VOLUME_HOTKEY_WINDOW_MS = intPreferencesKey("volume_hotkey_window_ms")
    private val KEY_VOLUME_HOTKEY_ACCESSIBILITY_ENABLED =
        booleanPreferencesKey("volume_hotkey_accessibility_enabled")
    private val KEY_VOLUME_HOTKEY_ENABLE_WARNING_DISMISSED =
        booleanPreferencesKey("volume_hotkey_enable_warning_dismissed")
    private val KEY_FLOATING_OVERLAY_SHORTCUTS = stringPreferencesKey("floating_overlay_shortcuts")
    private val KEY_FLOATING_OVERLAY_DEFAULT_SHORTCUTS_SEEDED =
        booleanPreferencesKey("floating_overlay_default_shortcuts_seeded")
    private val KEY_FLOATING_OVERLAY_LAYOUT = stringPreferencesKey("floating_overlay_layout")
    private val KEY_FLOATING_OVERLAY_QUICK_SUBTITLE_FONT_SIZE = floatPreferencesKey("floating_overlay_quick_subtitle_font_size")
    private val KEY_QUICK_SUBTITLE_CONFIG = stringPreferencesKey("quick_subtitle_config")
    private val KEY_SOUNDBOARD_CONFIG = stringPreferencesKey("soundboard_config")
    private val KEY_QUICK_CARD_CONFIG = stringPreferencesKey("quick_card_config")
    private val KEY_TTS_DISABLED = booleanPreferencesKey("tts_disabled")
    private val KEY_SOUNDBOARD_KEYWORD_TRIGGER_ENABLED = booleanPreferencesKey("soundboard_keyword_trigger_enabled")
    private val KEY_SOUNDBOARD_SUPPRESS_TTS_ON_KEYWORD = booleanPreferencesKey("soundboard_suppress_tts_on_keyword")
    private val KEY_ALLOW_QUICK_TEXT_TRIGGER_SOUNDBOARD = booleanPreferencesKey("allow_quick_text_trigger_soundboard")
    private val KEY_QUICK_SUBTITLE_INTERRUPT_QUEUE = booleanPreferencesKey("quick_subtitle_interrupt_queue")
    private val KEY_QUICK_SUBTITLE_AUTO_FIT = booleanPreferencesKey("quick_subtitle_auto_fit")
    private val KEY_QUICK_SUBTITLE_COMPACT_CONTROLS = booleanPreferencesKey("quick_subtitle_compact_controls")
    private val KEY_QUICK_SUBTITLE_KEEP_INPUT_PREVIEW =
        booleanPreferencesKey("quick_subtitle_keep_input_preview")
    private val KEY_DRAWING_KEEP_CANVAS_ORIENTATION_TO_DEVICE =
        booleanPreferencesKey("drawing_keep_canvas_orientation_to_device")
    private val KEY_SPEAKER_VERIFY_ENABLED = booleanPreferencesKey("speaker_verify_enabled")
    private val KEY_SPEAKER_VERIFY_THRESHOLD = floatPreferencesKey("speaker_verify_threshold")
    private val KEY_SPEAKER_VERIFY_PROFILE = stringPreferencesKey("speaker_verify_profile")
    private val KEY_SPEAKER_VERIFY_BACKEND_VERSION = intPreferencesKey("speaker_verify_backend_version")

    const val SPEAKER_VERIFY_BACKEND_SHERPA_V1 = 1

    data class SpeakerVerifyProfile(
        val id: String,
        val name: String,
        val vector: FloatArray
    )

    data class AppSettings(
        val muteWhilePlaying: Boolean = true,
        val muteWhilePlayingDelaySec: Float = 0.2f,
        val echoSuppression: Boolean = false,
        val communicationMode: Boolean = false,
        val preferredInputType: Int = AudioRoutePreference.INPUT_AUTO,
        val preferredOutputType: Int = AudioRoutePreference.OUTPUT_AUTO,
        val aec3Enabled: Boolean = true,
        val denoiserMode: Int = AudioDenoiserMode.RNNOISE,
        val speechEnhancementMode: Int = SpeechEnhancementMode.DPDFNET4_STREAMING,
        val classicVadEnabled: Boolean = false,
        val sileroVadEnabled: Boolean = true,
        val sileroVadThreshold: Float = SILERO_VAD_DEFAULT_THRESHOLD,
        val sileroVadPreRollMs: Int = SILERO_VAD_DEFAULT_PRE_ROLL_MS,
        val recognitionResourceModelScopeUrl: String = DEFAULT_RECOGNITION_RESOURCE_MODELSCOPE_URL,
        val recognitionResourceHuggingFaceUrl: String = DEFAULT_RECOGNITION_RESOURCE_HUGGINGFACE_URL,
        val recognitionResourcePreferredSource: Int = RECOGNITION_RESOURCE_SOURCE_MODELSCOPE,
        val minVolumePercent: Int = 2,
        val playbackGainPercent: Int = 100,
        val piperNoiseScale: Float = 0.667f,
        val piperLengthScale: Float = 1.0f,
        val piperNoiseW: Float = 0.8f,
        val piperSentenceSilence: Float = 0.2f,
        val keepAlive: Boolean = true,
        val numberReplaceMode: Int = 0,
        val landscapeDrawerMode: Int = DRAWER_MODE_PERMANENT,
        val solidTopBar: Boolean = true,
        val themeMode: Int = THEME_MODE_FOLLOW_SYSTEM,
        val overlayThemeMode: Int = THEME_MODE_FOLLOW_SYSTEM,
        val fontScaleBlockMode: Int = FONT_SCALE_BLOCK_ICONS_ONLY,
        val hapticFeedbackEnabled: Boolean = true,
        val drawingSaveRelativePath: String = DEFAULT_DRAWING_SAVE_RELATIVE_PATH,
        val quickCardAutoSaveOnExit: Boolean = false,
        val useBuiltinFileManager: Boolean = true,
        val useBuiltinGallery: Boolean = true,
        val asrSendToQuickSubtitle: Boolean = true,
        val pushToTalkMode: Boolean = false,
        val pushToTalkConfirmInput: Boolean = false,
        val floatingOverlayEnabled: Boolean = false,
        val floatingOverlayAutoDock: Boolean = true,
        val floatingOverlayShowOnLockScreen: Boolean = false,
        val floatingOverlayHardcodedShortcutSupplement: Boolean = false,
        val volumeHotkeyUpDownEnabled: Boolean = false,
        val volumeHotkeyDownUpEnabled: Boolean = false,
        val volumeHotkeyWindowMs: Int = VOLUME_HOTKEY_DEFAULT_WINDOW_MS,
        val volumeHotkeyAccessibilityEnabled: Boolean = false,
        val volumeHotkeyEnableWarningDismissed: Boolean = false,
        val volumeHotkeyUpDownAction: VolumeHotkeyActionSpec =
            VolumeHotkeyActions.defaultFor(VolumeHotkeySequence.UpDown),
        val volumeHotkeyDownUpAction: VolumeHotkeyActionSpec =
            VolumeHotkeyActions.defaultFor(VolumeHotkeySequence.DownUp),
        val ttsDisabled: Boolean = false,
        val soundboardKeywordTriggerEnabled: Boolean = false,
        val soundboardSuppressTtsOnKeyword: Boolean = false,
        val allowQuickTextTriggerSoundboard: Boolean = false,
        val quickSubtitleInterruptQueue: Boolean = true,
        val quickSubtitleAutoFit: Boolean = true,
        val quickSubtitleCompactControls: Boolean = false,
        val quickSubtitleKeepInputPreview: Boolean = true,
        val drawingKeepCanvasOrientationToDevice: Boolean = true,
        val speakerVerifyEnabled: Boolean = false,
        val speakerVerifyThreshold: Float = 0.5f,
        val speakerVerifyProfileCsv: String = "",
        val speakerVerifyBackendVersion: Int = 0,
        val allowSystemAecWithAec3: Boolean = true
    )

    fun normalizeThemeMode(mode: Int): Int =
        mode.coerceIn(THEME_MODE_FOLLOW_SYSTEM, THEME_MODE_DARK)

    fun normalizeFontScaleBlockMode(mode: Int): Int =
        mode.coerceIn(FONT_SCALE_BLOCK_NONE, FONT_SCALE_BLOCK_ALL)

    fun resolveThemeMode(mode: Int, followSystemDark: Boolean): Boolean =
        when (normalizeThemeMode(mode)) {
            THEME_MODE_LIGHT -> false
            THEME_MODE_DARK -> true
            else -> followSystemDark
        }

    suspend fun getLastAsrName(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_LAST_ASR]?.takeIf { it.isNotBlank() }
    }

    suspend fun setLastAsrName(context: Context, name: String) {
        context.dataStore.edit { prefs ->
            if (name.isBlank()) {
                prefs.remove(KEY_LAST_ASR)
            } else {
                prefs[KEY_LAST_ASR] = name
            }
        }
    }

    suspend fun clearLastAsrName(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_ASR)
        }
    }

    suspend fun getLastVoiceName(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_LAST_VOICE]?.takeIf { it.isNotBlank() }
    }

    suspend fun setLastVoiceName(context: Context, name: String) {
        context.dataStore.edit { prefs ->
            if (name.isBlank()) {
                prefs.remove(KEY_LAST_VOICE)
            } else {
                prefs[KEY_LAST_VOICE] = name
            }
        }
    }

    suspend fun clearLastVoiceName(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_VOICE)
        }
    }

    suspend fun getSystemTtsOrder(context: Context): Long? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_SYSTEM_TTS_ORDER]
    }

    suspend fun setSystemTtsOrder(context: Context, order: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SYSTEM_TTS_ORDER] = order
        }
    }

    suspend fun getSettings(context: Context): AppSettings {
        val prefs = context.dataStore.data.first()
        return prefs.toAppSettings()
    }

    fun observeSettings(context: Context): Flow<AppSettings> {
        return context.dataStore.data.map { prefs -> prefs.toAppSettings() }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val legacyPreferUsb = this[KEY_PREFER_USB_MIC] ?: false
        val legacySpeaker = this[KEY_COMMUNICATION_SPEAKER] ?: false
        var classicVadEnabled = this[KEY_CLASSIC_VAD_ENABLED] ?: false
        var sileroVadEnabled = this[KEY_SILERO_VAD_ENABLED] ?: true
        val legacySpeechEnhancementEnabled = this[KEY_SPEECH_ENHANCEMENT_ENABLED] ?: false
        val speechEnhancementMode = if (contains(KEY_SPEECH_ENHANCEMENT_MODE)) {
            SpeechEnhancementMode.clamp(this[KEY_SPEECH_ENHANCEMENT_MODE] ?: SpeechEnhancementMode.OFF)
        } else if (legacySpeechEnhancementEnabled) {
            SpeechEnhancementMode.GTCRN_OFFLINE
        } else {
            SpeechEnhancementMode.DPDFNET4_STREAMING
        }
        if (!classicVadEnabled && !sileroVadEnabled) {
            classicVadEnabled = false
            sileroVadEnabled = true
        }
        return AppSettings(
            muteWhilePlaying = this[KEY_MUTE_WHILE_PLAYING] ?: true,
            muteWhilePlayingDelaySec = this[KEY_MUTE_DELAY_SEC] ?: 0.2f,
            echoSuppression = this[KEY_ECHO_SUPPRESSION] ?: false,
            communicationMode = this[KEY_COMMUNICATION_MODE] ?: false,
            preferredInputType = this[KEY_PREFERRED_INPUT_TYPE]
                ?: if (legacyPreferUsb) AudioRoutePreference.INPUT_USB else AudioRoutePreference.INPUT_AUTO,
            preferredOutputType = this[KEY_PREFERRED_OUTPUT_TYPE]
                ?: if (legacySpeaker) AudioRoutePreference.OUTPUT_SPEAKER else AudioRoutePreference.OUTPUT_AUTO,
            aec3Enabled = this[KEY_AEC3_ENABLED] ?: true,
            denoiserMode = (this[KEY_DENOISER_MODE] ?: AudioDenoiserMode.RNNOISE)
                .coerceIn(AudioDenoiserMode.OFF, AudioDenoiserMode.SPEEX),
            speechEnhancementMode = speechEnhancementMode,
            classicVadEnabled = classicVadEnabled,
            sileroVadEnabled = sileroVadEnabled,
            sileroVadThreshold = (this[KEY_SILERO_VAD_THRESHOLD] ?: SILERO_VAD_DEFAULT_THRESHOLD)
                .coerceIn(SILERO_VAD_MIN_THRESHOLD, SILERO_VAD_MAX_THRESHOLD),
            sileroVadPreRollMs = (this[KEY_SILERO_VAD_PRE_ROLL_MS] ?: SILERO_VAD_DEFAULT_PRE_ROLL_MS)
                .coerceIn(SILERO_VAD_MIN_PRE_ROLL_MS, SILERO_VAD_MAX_PRE_ROLL_MS),
            recognitionResourceModelScopeUrl = this[KEY_RECOGNITION_RESOURCE_MODELSCOPE_URL]
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_RECOGNITION_RESOURCE_MODELSCOPE_URL,
            recognitionResourceHuggingFaceUrl = this[KEY_RECOGNITION_RESOURCE_HUGGINGFACE_URL]
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_RECOGNITION_RESOURCE_HUGGINGFACE_URL,
            recognitionResourcePreferredSource = (this[KEY_RECOGNITION_RESOURCE_PREFERRED_SOURCE]
                ?: RECOGNITION_RESOURCE_SOURCE_MODELSCOPE)
                .coerceIn(RECOGNITION_RESOURCE_SOURCE_MODELSCOPE, RECOGNITION_RESOURCE_SOURCE_HUGGINGFACE),
            minVolumePercent = this[KEY_MIN_VOLUME_PERCENT] ?: 2,
            playbackGainPercent = (this[KEY_PLAYBACK_GAIN_PERCENT] ?: 100).coerceIn(0, 1000),
            piperNoiseScale = (this[KEY_PIPER_NOISE_SCALE] ?: 0.667f).coerceIn(0f, 2f),
            piperLengthScale = (this[KEY_PIPER_LENGTH_SCALE] ?: 1.0f).coerceIn(0.1f, 5f),
            piperNoiseW = (this[KEY_PIPER_NOISE_W] ?: 0.8f).coerceIn(0f, 2f),
            piperSentenceSilence = (this[KEY_PIPER_SENTENCE_SILENCE] ?: 0.2f).coerceIn(0f, 2f),
            keepAlive = true,
            numberReplaceMode = this[KEY_NUMBER_REPLACE_MODE] ?: 0,
            landscapeDrawerMode = (this[KEY_LANDSCAPE_DRAWER_MODE] ?: DRAWER_MODE_PERMANENT)
                .coerceIn(DRAWER_MODE_HIDDEN, DRAWER_MODE_PERMANENT),
            solidTopBar = this[KEY_SOLID_TOP_BAR] ?: true,
            themeMode = normalizeThemeMode(this[KEY_THEME_MODE] ?: THEME_MODE_FOLLOW_SYSTEM),
            overlayThemeMode = normalizeThemeMode(this[KEY_OVERLAY_THEME_MODE] ?: THEME_MODE_FOLLOW_SYSTEM),
            fontScaleBlockMode = normalizeFontScaleBlockMode(
                this[KEY_FONT_SCALE_BLOCK_MODE] ?: FONT_SCALE_BLOCK_ICONS_ONLY
            ),
            hapticFeedbackEnabled = this[KEY_HAPTIC_FEEDBACK_ENABLED] ?: true,
            drawingSaveRelativePath = (this[KEY_DRAWING_SAVE_RELATIVE_PATH]
                ?: DEFAULT_DRAWING_SAVE_RELATIVE_PATH).ifBlank { DEFAULT_DRAWING_SAVE_RELATIVE_PATH },
            quickCardAutoSaveOnExit = this[KEY_QUICK_CARD_AUTO_SAVE_ON_EXIT] ?: false,
            useBuiltinFileManager = this[KEY_USE_BUILTIN_FILE_MANAGER] ?: true,
            useBuiltinGallery = this[KEY_USE_BUILTIN_GALLERY] ?: true,
            asrSendToQuickSubtitle = this[KEY_ASR_SEND_TO_QUICK_SUBTITLE] ?: true,
            pushToTalkMode = this[KEY_PUSH_TO_TALK_MODE] ?: false,
            pushToTalkConfirmInput = this[KEY_PUSH_TO_TALK_CONFIRM_INPUT] ?: false,
            floatingOverlayEnabled = this[KEY_FLOATING_OVERLAY_ENABLED] ?: false,
            floatingOverlayAutoDock = this[KEY_FLOATING_OVERLAY_AUTO_DOCK] ?: true,
            floatingOverlayShowOnLockScreen = this[KEY_FLOATING_OVERLAY_SHOW_ON_LOCK_SCREEN] ?: false,
            floatingOverlayHardcodedShortcutSupplement =
                this[KEY_FLOATING_OVERLAY_HARDCODED_SHORTCUT_SUPPLEMENT] ?: false,
            volumeHotkeyUpDownEnabled = this[KEY_VOLUME_HOTKEY_UP_DOWN_ENABLED] ?: false,
            volumeHotkeyDownUpEnabled = this[KEY_VOLUME_HOTKEY_DOWN_UP_ENABLED] ?: false,
            volumeHotkeyWindowMs = (this[KEY_VOLUME_HOTKEY_WINDOW_MS] ?: VOLUME_HOTKEY_DEFAULT_WINDOW_MS)
                .coerceIn(VOLUME_HOTKEY_MIN_WINDOW_MS, VOLUME_HOTKEY_MAX_WINDOW_MS),
            volumeHotkeyAccessibilityEnabled =
                this[KEY_VOLUME_HOTKEY_ACCESSIBILITY_ENABLED] ?: false,
            volumeHotkeyEnableWarningDismissed =
                this[KEY_VOLUME_HOTKEY_ENABLE_WARNING_DISMISSED] ?: false,
            volumeHotkeyUpDownAction = VolumeHotkeyActions.decode(
                this[KEY_VOLUME_HOTKEY_UP_DOWN_ACTION],
                fallback = VolumeHotkeyActions.defaultFor(VolumeHotkeySequence.UpDown)
            ),
            volumeHotkeyDownUpAction = VolumeHotkeyActions.decode(
                this[KEY_VOLUME_HOTKEY_DOWN_UP_ACTION],
                fallback = VolumeHotkeyActions.defaultFor(VolumeHotkeySequence.DownUp)
            ),
            ttsDisabled = this[KEY_TTS_DISABLED] ?: false,
            soundboardKeywordTriggerEnabled = this[KEY_SOUNDBOARD_KEYWORD_TRIGGER_ENABLED] ?: false,
            soundboardSuppressTtsOnKeyword = this[KEY_SOUNDBOARD_SUPPRESS_TTS_ON_KEYWORD] ?: false,
            allowQuickTextTriggerSoundboard = this[KEY_ALLOW_QUICK_TEXT_TRIGGER_SOUNDBOARD] ?: false,
            quickSubtitleInterruptQueue = this[KEY_QUICK_SUBTITLE_INTERRUPT_QUEUE] ?: true,
            quickSubtitleAutoFit = this[KEY_QUICK_SUBTITLE_AUTO_FIT] ?: true,
            quickSubtitleCompactControls = this[KEY_QUICK_SUBTITLE_COMPACT_CONTROLS] ?: false,
            quickSubtitleKeepInputPreview = this[KEY_QUICK_SUBTITLE_KEEP_INPUT_PREVIEW] ?: true,
            drawingKeepCanvasOrientationToDevice = this[KEY_DRAWING_KEEP_CANVAS_ORIENTATION_TO_DEVICE] ?: true,
            speakerVerifyEnabled = this[KEY_SPEAKER_VERIFY_ENABLED] ?: false,
            speakerVerifyThreshold = (this[KEY_SPEAKER_VERIFY_THRESHOLD] ?: 0.5f).coerceIn(0.05f, 0.95f),
            speakerVerifyProfileCsv = this[KEY_SPEAKER_VERIFY_PROFILE] ?: "",
            speakerVerifyBackendVersion = this[KEY_SPEAKER_VERIFY_BACKEND_VERSION] ?: 0,
            allowSystemAecWithAec3 = true
        )
    }

    suspend fun setMuteWhilePlaying(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MUTE_WHILE_PLAYING] = enabled
        }
    }

    suspend fun setMuteWhilePlayingDelaySec(context: Context, seconds: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MUTE_DELAY_SEC] = seconds
        }
    }

    suspend fun setEchoSuppression(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ECHO_SUPPRESSION] = enabled
        }
    }

    suspend fun setCommunicationMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COMMUNICATION_MODE] = enabled
        }
    }

    suspend fun setCommunicationSpeaker(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COMMUNICATION_SPEAKER] = enabled
        }
    }

    suspend fun setPreferredInputType(context: Context, type: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_INPUT_TYPE] = type
        }
    }

    suspend fun setPreferredOutputType(context: Context, type: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_OUTPUT_TYPE] = type
        }
    }

    suspend fun setAec3Enabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AEC3_ENABLED] = enabled
        }
    }

    suspend fun setDenoiserMode(context: Context, mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DENOISER_MODE] = mode.coerceIn(AudioDenoiserMode.OFF, AudioDenoiserMode.SPEEX)
        }
    }

    suspend fun setSpeechEnhancementMode(context: Context, mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPEECH_ENHANCEMENT_MODE] = SpeechEnhancementMode.clamp(mode)
            prefs[KEY_SPEECH_ENHANCEMENT_ENABLED] = SpeechEnhancementMode.isEnabled(mode)
        }
    }

    suspend fun setClassicVadEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLASSIC_VAD_ENABLED] = enabled
        }
    }

    suspend fun setSileroVadEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SILERO_VAD_ENABLED] = enabled
        }
    }

    suspend fun setVadFlags(context: Context, classicEnabled: Boolean, sileroEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLASSIC_VAD_ENABLED] = classicEnabled
            prefs[KEY_SILERO_VAD_ENABLED] = sileroEnabled
        }
    }

    suspend fun setSileroVadThreshold(context: Context, threshold: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SILERO_VAD_THRESHOLD] = threshold.coerceIn(
                SILERO_VAD_MIN_THRESHOLD,
                SILERO_VAD_MAX_THRESHOLD
            )
        }
    }

    suspend fun setSileroVadPreRollMs(context: Context, preRollMs: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SILERO_VAD_PRE_ROLL_MS] = preRollMs.coerceIn(
                SILERO_VAD_MIN_PRE_ROLL_MS,
                SILERO_VAD_MAX_PRE_ROLL_MS
            )
        }
    }

    suspend fun setRecognitionResourceSources(
        context: Context,
        modelScopeUrl: String,
        huggingFaceUrl: String,
        preferredSource: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RECOGNITION_RESOURCE_MODELSCOPE_URL] = modelScopeUrl.trim()
            prefs[KEY_RECOGNITION_RESOURCE_HUGGINGFACE_URL] = huggingFaceUrl.trim()
            prefs[KEY_RECOGNITION_RESOURCE_PREFERRED_SOURCE] = preferredSource.coerceIn(
                RECOGNITION_RESOURCE_SOURCE_MODELSCOPE,
                RECOGNITION_RESOURCE_SOURCE_HUGGINGFACE
            )
        }
    }

    suspend fun setMinVolumePercent(context: Context, percent: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MIN_VOLUME_PERCENT] = percent
        }
    }

    suspend fun setPlaybackGainPercent(context: Context, percent: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLAYBACK_GAIN_PERCENT] = percent.coerceIn(0, 1000)
        }
    }

    suspend fun setPiperNoiseScale(context: Context, value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIPER_NOISE_SCALE] = value.coerceIn(0f, 2f)
        }
    }

    suspend fun setPiperLengthScale(context: Context, value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIPER_LENGTH_SCALE] = value.coerceIn(0.1f, 5f)
        }
    }

    suspend fun setPiperNoiseW(context: Context, value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIPER_NOISE_W] = value.coerceIn(0f, 2f)
        }
    }

    suspend fun setPiperSentenceSilence(context: Context, value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIPER_SENTENCE_SILENCE] = value.coerceIn(0f, 2f)
        }
    }

    suspend fun setKeepAlive(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_KEEP_ALIVE] = enabled
        }
    }

    suspend fun setNumberReplaceMode(context: Context, mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NUMBER_REPLACE_MODE] = mode
        }
    }

    suspend fun setLandscapeDrawerMode(context: Context, mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANDSCAPE_DRAWER_MODE] =
                mode.coerceIn(DRAWER_MODE_HIDDEN, DRAWER_MODE_PERMANENT)
        }
    }

    suspend fun setSolidTopBar(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOLID_TOP_BAR] = enabled
        }
    }

    suspend fun setThemeMode(context: Context, mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = normalizeThemeMode(mode)
        }
    }

    suspend fun setOverlayThemeMode(context: Context, mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_THEME_MODE] = normalizeThemeMode(mode)
        }
    }

    suspend fun setFontScaleBlockMode(context: Context, mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FONT_SCALE_BLOCK_MODE] = normalizeFontScaleBlockMode(mode)
        }
    }

    suspend fun setHapticFeedbackEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun setDrawingSaveRelativePath(context: Context, path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRAWING_SAVE_RELATIVE_PATH] =
                path.ifBlank { DEFAULT_DRAWING_SAVE_RELATIVE_PATH }
        }
    }

    suspend fun setQuickCardAutoSaveOnExit(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUICK_CARD_AUTO_SAVE_ON_EXIT] = enabled
        }
    }

    suspend fun setUseBuiltinFileManager(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_BUILTIN_FILE_MANAGER] = enabled
        }
    }

    suspend fun setUseBuiltinGallery(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_BUILTIN_GALLERY] = enabled
        }
    }

    suspend fun setAsrSendToQuickSubtitle(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ASR_SEND_TO_QUICK_SUBTITLE] = enabled
        }
    }

    suspend fun setPushToTalkMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PUSH_TO_TALK_MODE] = enabled
        }
    }

    suspend fun setPushToTalkConfirmInput(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PUSH_TO_TALK_CONFIRM_INPUT] = enabled
        }
    }

    suspend fun setFloatingOverlayEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setFloatingOverlayAutoDock(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_AUTO_DOCK] = enabled
        }
    }

    suspend fun setFloatingOverlayShowOnLockScreen(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_SHOW_ON_LOCK_SCREEN] = enabled
        }
    }

    suspend fun setFloatingOverlayHardcodedShortcutSupplement(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_HARDCODED_SHORTCUT_SUPPLEMENT] = enabled
        }
    }

    suspend fun setVolumeHotkeyEnabled(
        context: Context,
        sequence: VolumeHotkeySequence,
        enabled: Boolean
    ) {
        context.dataStore.edit { prefs ->
            when (sequence) {
                VolumeHotkeySequence.UpDown -> prefs[KEY_VOLUME_HOTKEY_UP_DOWN_ENABLED] = enabled
                VolumeHotkeySequence.DownUp -> prefs[KEY_VOLUME_HOTKEY_DOWN_UP_ENABLED] = enabled
            }
        }
    }

    suspend fun setVolumeHotkeyAction(
        context: Context,
        sequence: VolumeHotkeySequence,
        action: VolumeHotkeyActionSpec
    ) {
        val payload = VolumeHotkeyActions.encode(action)
        context.dataStore.edit { prefs ->
            when (sequence) {
                VolumeHotkeySequence.UpDown -> prefs[KEY_VOLUME_HOTKEY_UP_DOWN_ACTION] = payload
                VolumeHotkeySequence.DownUp -> prefs[KEY_VOLUME_HOTKEY_DOWN_UP_ACTION] = payload
            }
        }
    }

    suspend fun setVolumeHotkeyWindowMs(context: Context, windowMs: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VOLUME_HOTKEY_WINDOW_MS] = windowMs.coerceIn(
                VOLUME_HOTKEY_MIN_WINDOW_MS,
                VOLUME_HOTKEY_MAX_WINDOW_MS
            )
        }
    }

    suspend fun setVolumeHotkeyAccessibilityEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VOLUME_HOTKEY_ACCESSIBILITY_ENABLED] = enabled
        }
    }

    suspend fun setVolumeHotkeyEnableWarningDismissed(context: Context, dismissed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VOLUME_HOTKEY_ENABLE_WARNING_DISMISSED] = dismissed
        }
    }

    suspend fun setTtsDisabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TTS_DISABLED] = enabled
        }
    }

    suspend fun setSoundboardKeywordTriggerEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUNDBOARD_KEYWORD_TRIGGER_ENABLED] = enabled
        }
    }

    suspend fun setSoundboardSuppressTtsOnKeyword(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUNDBOARD_SUPPRESS_TTS_ON_KEYWORD] = enabled
        }
    }

    suspend fun setAllowQuickTextTriggerSoundboard(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALLOW_QUICK_TEXT_TRIGGER_SOUNDBOARD] = enabled
        }
    }

    suspend fun setQuickSubtitleInterruptQueue(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUICK_SUBTITLE_INTERRUPT_QUEUE] = enabled
        }
    }

    suspend fun setQuickSubtitleAutoFit(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUICK_SUBTITLE_AUTO_FIT] = enabled
        }
    }

    suspend fun setQuickSubtitleCompactControls(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUICK_SUBTITLE_COMPACT_CONTROLS] = enabled
        }
    }

    suspend fun setQuickSubtitleKeepInputPreview(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUICK_SUBTITLE_KEEP_INPUT_PREVIEW] = enabled
        }
    }

    suspend fun setDrawingKeepCanvasOrientationToDevice(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRAWING_KEEP_CANVAS_ORIENTATION_TO_DEVICE] = enabled
        }
    }

    suspend fun setSpeakerVerifyEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPEAKER_VERIFY_ENABLED] = enabled
        }
    }

    suspend fun setSpeakerVerifyThreshold(context: Context, threshold: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPEAKER_VERIFY_THRESHOLD] = threshold.coerceIn(0.05f, 0.95f)
        }
    }

    suspend fun setSpeakerVerifyProfile(context: Context, vector: FloatArray?) {
        setSpeakerVerifyProfiles(
            context,
            if (vector == null || vector.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    SpeakerVerifyProfile(
                        id = "legacy-1",
                        name = "说话人 1",
                        vector = vector
                    )
                )
            }
        )
    }

    suspend fun setSpeakerVerifyProfiles(context: Context, profiles: List<SpeakerVerifyProfile>) {
        context.dataStore.edit { prefs ->
            val payload = serializeSpeakerVerifyProfiles(profiles)
            prefs[KEY_SPEAKER_VERIFY_PROFILE] = payload
            prefs[KEY_SPEAKER_VERIFY_BACKEND_VERSION] = SPEAKER_VERIFY_BACKEND_SHERPA_V1
        }
    }

    suspend fun resetSpeakerVerifyBackend(context: Context, enabled: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPEAKER_VERIFY_PROFILE] = ""
            prefs[KEY_SPEAKER_VERIFY_ENABLED] = enabled
            prefs[KEY_SPEAKER_VERIFY_BACKEND_VERSION] = SPEAKER_VERIFY_BACKEND_SHERPA_V1
        }
    }

    fun serializeSpeakerVerifyProfiles(profiles: List<SpeakerVerifyProfile>): String {
        if (profiles.isEmpty()) return ""
        val arr = JSONArray()
        profiles.forEach { profile ->
            if (profile.vector.isEmpty()) return@forEach
            val vectorArr = JSONArray()
            profile.vector.forEach { v -> vectorArr.put(v.toDouble()) }
            arr.put(
                JSONObject().apply {
                    put("id", profile.id)
                    put("name", profile.name)
                    put("vector", vectorArr)
                }
            )
        }
        return if (arr.length() <= 0) "" else arr.toString()
    }

    fun parseSpeakerVerifyProfiles(rawPayload: String?): List<SpeakerVerifyProfile> {
        val raw = rawPayload?.trim().orEmpty()
        if (raw.isEmpty()) return emptyList()

        // New format: JSON array of profiles.
        if (raw.startsWith("[")) {
            return runCatching {
                val arr = JSONArray(raw)
                val out = mutableListOf<SpeakerVerifyProfile>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val vecArr = obj.optJSONArray("vector") ?: continue
                    val vec = FloatArray(vecArr.length())
                    var ok = true
                    for (j in 0 until vecArr.length()) {
                        val d = vecArr.optDouble(j, Double.NaN)
                        if (d.isNaN()) {
                            ok = false
                            break
                        }
                        vec[j] = d.toFloat()
                    }
                    if (!ok || vec.isEmpty()) continue
                    val id = obj.optString("id").ifBlank { "profile-${i + 1}" }
                    val name = obj.optString("name").ifBlank { "说话人 ${i + 1}" }
                    out.add(SpeakerVerifyProfile(id = id, name = name, vector = vec))
                }
                out
            }.getOrElse { emptyList() }
        }

        // Legacy format: single CSV vector.
        val legacy = parseSpeakerVerifyProfile(raw)
        return if (legacy == null || legacy.isEmpty()) {
            emptyList()
        } else {
            listOf(
                SpeakerVerifyProfile(
                    id = "legacy-1",
                    name = "说话人 1",
                    vector = legacy
                )
            )
        }
    }

    fun parseSpeakerVerifyProfile(csv: String?): FloatArray? {
        val raw = csv?.trim().orEmpty()
        if (raw.isEmpty()) return null
        if (raw.startsWith("[")) {
            val parsed = parseSpeakerVerifyProfiles(raw)
            return parsed.firstOrNull()?.vector
        }
        val values = raw.split(",")
            .mapNotNull { token -> token.trim().toFloatOrNull() }
            .toFloatArray()
        return if (values.isEmpty()) null else values
    }

    @Deprecated("Use parseSpeakerVerifyProfiles instead")
    fun parseSpeakerVerifyProfileLegacy(csv: String?): FloatArray? {
        val raw = csv?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val values = raw.split(",")
            .mapNotNull { token -> token.trim().toFloatOrNull() }
            .toFloatArray()
        return if (values.isEmpty()) null else values
    }

    @Deprecated("Use setSpeakerVerifyProfiles instead")
    suspend fun setSpeakerVerifyProfileLegacy(context: Context, vector: FloatArray?) {
        context.dataStore.edit { prefs ->
            val csv = if (vector == null || vector.isEmpty()) {
                ""
            } else {
                vector.joinToString(",")
            }
            prefs[KEY_SPEAKER_VERIFY_PROFILE] = csv
        }
    }

    suspend fun getQuickSubtitleConfig(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_QUICK_SUBTITLE_CONFIG]
    }

    suspend fun setQuickSubtitleConfig(context: Context, json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUICK_SUBTITLE_CONFIG] = json
        }
    }

    suspend fun getSoundboardConfig(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_SOUNDBOARD_CONFIG]
    }

    suspend fun setSoundboardConfig(context: Context, json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUNDBOARD_CONFIG] = json
        }
    }

    suspend fun getFloatingOverlayShortcuts(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_FLOATING_OVERLAY_SHORTCUTS]
    }

    suspend fun setFloatingOverlayShortcuts(context: Context, json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_SHORTCUTS] = json
        }
    }

    suspend fun isFloatingOverlayDefaultShortcutsSeeded(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_FLOATING_OVERLAY_DEFAULT_SHORTCUTS_SEEDED] ?: false
    }

    suspend fun setFloatingOverlayDefaultShortcutsSeeded(context: Context, seeded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_DEFAULT_SHORTCUTS_SEEDED] = seeded
        }
    }

    suspend fun getFloatingOverlayLayout(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_FLOATING_OVERLAY_LAYOUT]
    }

    suspend fun setFloatingOverlayLayout(context: Context, json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_LAYOUT] = json
        }
    }

    suspend fun getFloatingOverlayQuickSubtitleFontSize(context: Context): Float? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_FLOATING_OVERLAY_QUICK_SUBTITLE_FONT_SIZE]
    }

    suspend fun setFloatingOverlayQuickSubtitleFontSize(context: Context, sizeSp: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_QUICK_SUBTITLE_FONT_SIZE] = sizeSp
        }
    }

    suspend fun getQuickCardConfig(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_QUICK_CARD_CONFIG]
    }

    suspend fun setQuickCardConfig(context: Context, json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUICK_CARD_CONFIG] = json
        }
    }

}
