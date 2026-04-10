import 'package:freezed_annotation/freezed_annotation.dart';

part 'app_settings.freezed.dart';
part 'app_settings.g.dart';

/// Application settings entity.
/// All keys and defaults match original UserPrefs.kt exactly.
@freezed
abstract class AppSettings with _$AppSettings {
  const factory AppSettings({
    @Default(false) bool muteWhilePlaying,
    @Default(0.0) double muteWhilePlayingDelaySec,
    @Default(false) bool echoSuppression,
    @Default(false) bool communicationMode,
    @Default(0) int preferredInputType,
    @Default(100) int preferredOutputType,
    @Default(false) bool aec3Enabled,
    @Default(0) int minVolumePercent,
    @Default(100) int playbackGainPercent,
    @Default(0.667) double piperNoiseScale,
    @Default(1.0) double piperLengthScale,
    @Default(0.8) double piperNoiseW,
    @Default(0.2) double piperSentenceSilence,
    @Default(false) bool keepAlive,
    @Default(0) int numberReplaceMode,
    @Default(1) int landscapeDrawerMode,
    @Default(true) bool solidTopBar,
    @Default('Pictures/KGTTS/Drawings') String drawingSaveRelativePath,
    @Default(false) bool quickCardAutoSaveOnExit,
    @Default(true) bool asrSendToQuickSubtitle,
    @Default(false) bool pushToTalkMode,
    @Default(false) bool pushToTalkConfirmInput,
    @Default(false) bool floatingOverlayEnabled,
    @Default(false) bool floatingOverlayAutoDock,
    @Default(false) bool speakerVerifyEnabled,
    @Default(0.72) double speakerVerifyThreshold,
  }) = _AppSettings;

  factory AppSettings.fromJson(Map<String, dynamic> json) =>
      _$AppSettingsFromJson(json);
}
