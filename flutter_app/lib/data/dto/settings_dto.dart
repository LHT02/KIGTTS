import '../../core/constants/prefs_keys.dart';
import '../../domain/entities/app_settings.dart';

/// DTO for converting AppSettings to/from Maps for MethodChannel.
abstract final class SettingsDto {
  /// Convert AppSettings to a Map for sending to native via MethodChannel.
  static Map<String, dynamic> toChannelMap(AppSettings s) {
    return {
      PrefsKeys.muteWhilePlaying: s.muteWhilePlaying,
      PrefsKeys.muteWhilePlayingDelaySec: s.muteWhilePlayingDelaySec,
      PrefsKeys.echoSuppression: s.echoSuppression,
      PrefsKeys.communicationMode: s.communicationMode,
      PrefsKeys.preferredInputType: s.preferredInputType,
      PrefsKeys.preferredOutputType: s.preferredOutputType,
      PrefsKeys.aec3Enabled: s.aec3Enabled,
      PrefsKeys.minVolumePercent: s.minVolumePercent,
      PrefsKeys.playbackGainPercent: s.playbackGainPercent,
      PrefsKeys.piperNoiseScale: s.piperNoiseScale,
      PrefsKeys.piperLengthScale: s.piperLengthScale,
      PrefsKeys.piperNoiseW: s.piperNoiseW,
      PrefsKeys.piperSentenceSilence: s.piperSentenceSilence,
      PrefsKeys.keepAlive: s.keepAlive,
      PrefsKeys.numberReplaceMode: s.numberReplaceMode,
      PrefsKeys.pushToTalkMode: s.pushToTalkMode,
      PrefsKeys.speakerVerifyEnabled: s.speakerVerifyEnabled,
      PrefsKeys.speakerVerifyThreshold: s.speakerVerifyThreshold,
    };
  }
}
