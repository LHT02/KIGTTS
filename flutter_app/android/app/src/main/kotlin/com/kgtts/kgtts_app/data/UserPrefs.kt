package com.kgtts.kgtts_app.data

import com.kgtts.kgtts_app.audio.AudioRoutePreference

/**
 * User preferences keys and defaults.
 *
 * In the Flutter rewrite, preferences are managed via shared_preferences on the Dart side.
 * This file preserves the original key names and default values for reference and for any
 * native-side code that needs to read preferences directly.
 */
object UserPrefs {
    const val DRAWER_MODE_HIDDEN = 0
    const val DRAWER_MODE_PERMANENT = 1
    const val DEFAULT_DRAWING_SAVE_RELATIVE_PATH = "Pictures/KGTTS/Drawings"

    // Preference key constants (matching Flutter shared_preferences keys)
    const val KEY_LAST_VOICE = "last_voice_name"
    const val KEY_MUTE_WHILE_PLAYING = "mute_while_playing"
    const val KEY_MUTE_DELAY_SEC = "mute_delay_sec"
    const val KEY_ECHO_SUPPRESSION = "echo_suppression"
    const val KEY_COMMUNICATION_MODE = "communication_mode"
    const val KEY_PREFERRED_INPUT_TYPE = "preferred_input_type"
    const val KEY_PREFERRED_OUTPUT_TYPE = "preferred_output_type"
    const val KEY_AEC3_ENABLED = "aec3_enabled"
    const val KEY_MIN_VOLUME_PERCENT = "min_volume_percent"
    const val KEY_PLAYBACK_GAIN_PERCENT = "playback_gain_percent"
    const val KEY_PIPER_NOISE_SCALE = "piper_noise_scale"
    const val KEY_PIPER_LENGTH_SCALE = "piper_length_scale"
    const val KEY_PIPER_NOISE_W = "piper_noise_w"
    const val KEY_PIPER_SENTENCE_SILENCE = "piper_sentence_silence"
    const val KEY_KEEP_ALIVE = "keep_alive"
    const val KEY_NUMBER_REPLACE_MODE = "number_replace_mode"
    const val KEY_LANDSCAPE_DRAWER_MODE = "landscape_drawer_mode"
    const val KEY_SOLID_TOP_BAR = "solid_top_bar"
    const val KEY_DRAWING_SAVE_RELATIVE_PATH = "drawing_save_relative_path"
    const val KEY_QUICK_CARD_AUTO_SAVE_ON_EXIT = "quick_card_auto_save_on_exit"
    const val KEY_ASR_SEND_TO_QUICK_SUBTITLE = "asr_send_to_quick_subtitle"
    const val KEY_PUSH_TO_TALK_MODE = "push_to_talk_mode"
    const val KEY_PUSH_TO_TALK_CONFIRM_INPUT = "push_to_talk_confirm_input"
    const val KEY_FLOATING_OVERLAY_ENABLED = "floating_overlay_enabled"
    const val KEY_FLOATING_OVERLAY_AUTO_DOCK = "floating_overlay_auto_dock"
    const val KEY_FLOATING_OVERLAY_SHORTCUTS = "floating_overlay_shortcuts"
    const val KEY_FLOATING_OVERLAY_LAYOUT = "floating_overlay_layout"
    const val KEY_QUICK_SUBTITLE_CONFIG = "quick_subtitle_config"
    const val KEY_QUICK_CARD_CONFIG = "quick_card_config"
    const val KEY_SPEAKER_VERIFY_ENABLED = "speaker_verify_enabled"
    const val KEY_SPEAKER_VERIFY_THRESHOLD = "speaker_verify_threshold"
    const val KEY_SPEAKER_VERIFY_PROFILE = "speaker_verify_profile"

    // Default values
    object Defaults {
        const val MUTE_WHILE_PLAYING = false
        const val MUTE_DELAY_SEC = 0f
        const val ECHO_SUPPRESSION = false
        const val COMMUNICATION_MODE = false
        val PREFERRED_INPUT_TYPE = AudioRoutePreference.INPUT_AUTO
        val PREFERRED_OUTPUT_TYPE = AudioRoutePreference.OUTPUT_AUTO
        const val AEC3_ENABLED = false
        const val MIN_VOLUME_PERCENT = 0
        const val PLAYBACK_GAIN_PERCENT = 100
        const val PIPER_NOISE_SCALE = 0.667f
        const val PIPER_LENGTH_SCALE = 1.0f
        const val PIPER_NOISE_W = 0.8f
        const val PIPER_SENTENCE_SILENCE = 0.2f
        const val KEEP_ALIVE = false
        const val NUMBER_REPLACE_MODE = 0
        val LANDSCAPE_DRAWER_MODE = DRAWER_MODE_PERMANENT
        const val SOLID_TOP_BAR = true
        val DRAWING_SAVE_RELATIVE_PATH = DEFAULT_DRAWING_SAVE_RELATIVE_PATH
        const val QUICK_CARD_AUTO_SAVE_ON_EXIT = false
        const val ASR_SEND_TO_QUICK_SUBTITLE = true
        const val PUSH_TO_TALK_MODE = false
        const val PUSH_TO_TALK_CONFIRM_INPUT = false
        const val FLOATING_OVERLAY_ENABLED = false
        const val FLOATING_OVERLAY_AUTO_DOCK = false
        const val SPEAKER_VERIFY_ENABLED = false
        const val SPEAKER_VERIFY_THRESHOLD = 0.72f
    }
}
