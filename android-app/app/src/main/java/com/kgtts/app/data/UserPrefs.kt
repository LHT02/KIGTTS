package com.kgtts.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kgtts.app.audio.AudioRoutePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPrefs {
    const val DRAWER_MODE_HIDDEN = 0
    const val DRAWER_MODE_PERMANENT = 1
    const val DEFAULT_DRAWING_SAVE_RELATIVE_PATH = "Pictures/KGTTS/Drawings"

    private val KEY_LAST_VOICE = stringPreferencesKey("last_voice_name")
    private val KEY_MUTE_WHILE_PLAYING = booleanPreferencesKey("mute_while_playing")
    private val KEY_MUTE_DELAY_SEC = floatPreferencesKey("mute_delay_sec")
    private val KEY_ECHO_SUPPRESSION = booleanPreferencesKey("echo_suppression")
    private val KEY_COMMUNICATION_MODE = booleanPreferencesKey("communication_mode")
    private val KEY_COMMUNICATION_SPEAKER = booleanPreferencesKey("communication_speaker")
    private val KEY_PREFER_USB_MIC = booleanPreferencesKey("prefer_usb_mic")
    private val KEY_PREFERRED_INPUT_TYPE = intPreferencesKey("preferred_input_type")
    private val KEY_PREFERRED_OUTPUT_TYPE = intPreferencesKey("preferred_output_type")
    private val KEY_AEC3_ENABLED = booleanPreferencesKey("aec3_enabled")
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
    private val KEY_DRAWING_SAVE_RELATIVE_PATH = stringPreferencesKey("drawing_save_relative_path")
    private val KEY_QUICK_CARD_AUTO_SAVE_ON_EXIT = booleanPreferencesKey("quick_card_auto_save_on_exit")
    private val KEY_ASR_SEND_TO_QUICK_SUBTITLE = booleanPreferencesKey("asr_send_to_quick_subtitle")
    private val KEY_PUSH_TO_TALK_MODE = booleanPreferencesKey("push_to_talk_mode")
    private val KEY_PUSH_TO_TALK_CONFIRM_INPUT = booleanPreferencesKey("push_to_talk_confirm_input")
    private val KEY_FLOATING_OVERLAY_ENABLED = booleanPreferencesKey("floating_overlay_enabled")
    private val KEY_FLOATING_OVERLAY_AUTO_DOCK = booleanPreferencesKey("floating_overlay_auto_dock")
    private val KEY_FLOATING_OVERLAY_SHORTCUTS = stringPreferencesKey("floating_overlay_shortcuts")
    private val KEY_FLOATING_OVERLAY_LAYOUT = stringPreferencesKey("floating_overlay_layout")
    private val KEY_QUICK_SUBTITLE_CONFIG = stringPreferencesKey("quick_subtitle_config")
    private val KEY_QUICK_CARD_CONFIG = stringPreferencesKey("quick_card_config")
    private val KEY_SPEAKER_VERIFY_ENABLED = booleanPreferencesKey("speaker_verify_enabled")
    private val KEY_SPEAKER_VERIFY_THRESHOLD = floatPreferencesKey("speaker_verify_threshold")
    private val KEY_SPEAKER_VERIFY_PROFILE = stringPreferencesKey("speaker_verify_profile")

    data class SpeakerVerifyProfile(
        val id: String,
        val name: String,
        val vector: FloatArray
    )

    data class AppSettings(
        val muteWhilePlaying: Boolean = false,
        val muteWhilePlayingDelaySec: Float = 0f,
        val echoSuppression: Boolean = false,
        val communicationMode: Boolean = false,
        val preferredInputType: Int = AudioRoutePreference.INPUT_AUTO,
        val preferredOutputType: Int = AudioRoutePreference.OUTPUT_AUTO,
        val aec3Enabled: Boolean = false,
        val minVolumePercent: Int = 0,
        val playbackGainPercent: Int = 100,
        val piperNoiseScale: Float = 0.667f,
        val piperLengthScale: Float = 1.0f,
        val piperNoiseW: Float = 0.8f,
        val piperSentenceSilence: Float = 0.2f,
        val keepAlive: Boolean = false,
        val numberReplaceMode: Int = 0,
        val landscapeDrawerMode: Int = DRAWER_MODE_PERMANENT,
        val solidTopBar: Boolean = true,
        val drawingSaveRelativePath: String = DEFAULT_DRAWING_SAVE_RELATIVE_PATH,
        val quickCardAutoSaveOnExit: Boolean = false,
        val asrSendToQuickSubtitle: Boolean = true,
        val pushToTalkMode: Boolean = false,
        val pushToTalkConfirmInput: Boolean = false,
        val floatingOverlayEnabled: Boolean = false,
        val floatingOverlayAutoDock: Boolean = false,
        val speakerVerifyEnabled: Boolean = false,
        val speakerVerifyThreshold: Float = 0.72f,
        val speakerVerifyProfileCsv: String = "",
        val allowSystemAecWithAec3: Boolean = true
    )

    suspend fun getLastVoiceName(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_LAST_VOICE]
    }

    suspend fun setLastVoiceName(context: Context, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_VOICE] = name
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
        return AppSettings(
            muteWhilePlaying = this[KEY_MUTE_WHILE_PLAYING] ?: false,
            muteWhilePlayingDelaySec = this[KEY_MUTE_DELAY_SEC] ?: 0f,
            echoSuppression = this[KEY_ECHO_SUPPRESSION] ?: false,
            communicationMode = this[KEY_COMMUNICATION_MODE] ?: false,
            preferredInputType = this[KEY_PREFERRED_INPUT_TYPE]
                ?: if (legacyPreferUsb) AudioRoutePreference.INPUT_USB else AudioRoutePreference.INPUT_AUTO,
            preferredOutputType = this[KEY_PREFERRED_OUTPUT_TYPE]
                ?: if (legacySpeaker) AudioRoutePreference.OUTPUT_SPEAKER else AudioRoutePreference.OUTPUT_AUTO,
            aec3Enabled = this[KEY_AEC3_ENABLED] ?: false,
            minVolumePercent = this[KEY_MIN_VOLUME_PERCENT] ?: 0,
            playbackGainPercent = (this[KEY_PLAYBACK_GAIN_PERCENT] ?: 100).coerceIn(0, 1000),
            piperNoiseScale = (this[KEY_PIPER_NOISE_SCALE] ?: 0.667f).coerceIn(0f, 2f),
            piperLengthScale = (this[KEY_PIPER_LENGTH_SCALE] ?: 1.0f).coerceIn(0.1f, 5f),
            piperNoiseW = (this[KEY_PIPER_NOISE_W] ?: 0.8f).coerceIn(0f, 2f),
            piperSentenceSilence = (this[KEY_PIPER_SENTENCE_SILENCE] ?: 0.2f).coerceIn(0f, 2f),
            keepAlive = this[KEY_KEEP_ALIVE] ?: false,
            numberReplaceMode = this[KEY_NUMBER_REPLACE_MODE] ?: 0,
            landscapeDrawerMode = (this[KEY_LANDSCAPE_DRAWER_MODE] ?: DRAWER_MODE_PERMANENT)
                .coerceIn(DRAWER_MODE_HIDDEN, DRAWER_MODE_PERMANENT),
            solidTopBar = this[KEY_SOLID_TOP_BAR] ?: true,
            drawingSaveRelativePath = (this[KEY_DRAWING_SAVE_RELATIVE_PATH]
                ?: DEFAULT_DRAWING_SAVE_RELATIVE_PATH).ifBlank { DEFAULT_DRAWING_SAVE_RELATIVE_PATH },
            quickCardAutoSaveOnExit = this[KEY_QUICK_CARD_AUTO_SAVE_ON_EXIT] ?: false,
            asrSendToQuickSubtitle = this[KEY_ASR_SEND_TO_QUICK_SUBTITLE] ?: true,
            pushToTalkMode = this[KEY_PUSH_TO_TALK_MODE] ?: false,
            pushToTalkConfirmInput = this[KEY_PUSH_TO_TALK_CONFIRM_INPUT] ?: false,
            floatingOverlayEnabled = this[KEY_FLOATING_OVERLAY_ENABLED] ?: false,
            floatingOverlayAutoDock = this[KEY_FLOATING_OVERLAY_AUTO_DOCK] ?: false,
            speakerVerifyEnabled = this[KEY_SPEAKER_VERIFY_ENABLED] ?: false,
            speakerVerifyThreshold = (this[KEY_SPEAKER_VERIFY_THRESHOLD] ?: 0.72f).coerceIn(0.4f, 0.95f),
            speakerVerifyProfileCsv = this[KEY_SPEAKER_VERIFY_PROFILE] ?: "",
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

    suspend fun setSpeakerVerifyEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPEAKER_VERIFY_ENABLED] = enabled
        }
    }

    suspend fun setSpeakerVerifyThreshold(context: Context, threshold: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPEAKER_VERIFY_THRESHOLD] = threshold.coerceIn(0.4f, 0.95f)
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

    suspend fun getFloatingOverlayShortcuts(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_FLOATING_OVERLAY_SHORTCUTS]
    }

    suspend fun setFloatingOverlayShortcuts(context: Context, json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_SHORTCUTS] = json
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
