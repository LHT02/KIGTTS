/// SharedPreferences key constants.
/// Keys and defaults match original UserPrefs.kt exactly.
abstract final class PrefsKeys {
  static const lastVoiceName = 'last_voice_name';
  static const muteWhilePlaying = 'mute_while_playing';
  static const muteWhilePlayingDelaySec = 'mute_delay_sec';
  static const echoSuppression = 'echo_suppression';
  static const communicationMode = 'communication_mode';
  static const preferredInputType = 'preferred_input_type';
  static const preferredOutputType = 'preferred_output_type';
  static const aec3Enabled = 'aec3_enabled';
  static const minVolumePercent = 'min_volume_percent';
  static const playbackGainPercent = 'playback_gain_percent';
  static const piperNoiseScale = 'piper_noise_scale';
  static const piperLengthScale = 'piper_length_scale';
  static const piperNoiseW = 'piper_noise_w';
  static const piperSentenceSilence = 'piper_sentence_silence';
  static const keepAlive = 'keep_alive';
  static const numberReplaceMode = 'number_replace_mode';
  static const landscapeDrawerMode = 'landscape_drawer_mode';
  static const solidTopBar = 'solid_top_bar';
  static const drawingSaveRelativePath = 'drawing_save_relative_path';
  static const quickCardAutoSaveOnExit = 'quick_card_auto_save_on_exit';
  static const asrSendToQuickSubtitle = 'asr_send_to_quick_subtitle';
  static const pushToTalkMode = 'push_to_talk_mode';
  static const pushToTalkConfirmInput = 'push_to_talk_confirm_input';
  static const floatingOverlayEnabled = 'floating_overlay_enabled';
  static const floatingOverlayAutoDock = 'floating_overlay_auto_dock';
  static const floatingOverlayShortcuts = 'floating_overlay_shortcuts';
  static const floatingOverlayLayout = 'floating_overlay_layout';
  static const quickSubtitleConfig = 'quick_subtitle_config';
  static const quickCardConfig = 'quick_card_config';
  static const speakerVerifyEnabled = 'speaker_verify_enabled';
  static const speakerVerifyThreshold = 'speaker_verify_threshold';
  static const speakerVerifyProfile = 'speaker_verify_profile';
}
