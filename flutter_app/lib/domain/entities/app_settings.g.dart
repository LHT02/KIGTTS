// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'app_settings.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$AppSettingsImpl _$$AppSettingsImplFromJson(
  Map<String, dynamic> json,
) => _$AppSettingsImpl(
  muteWhilePlaying: json['muteWhilePlaying'] as bool? ?? false,
  muteWhilePlayingDelaySec:
      (json['muteWhilePlayingDelaySec'] as num?)?.toDouble() ?? 0.0,
  echoSuppression: json['echoSuppression'] as bool? ?? false,
  communicationMode: json['communicationMode'] as bool? ?? false,
  preferredInputType: (json['preferredInputType'] as num?)?.toInt() ?? 0,
  preferredOutputType: (json['preferredOutputType'] as num?)?.toInt() ?? 100,
  aec3Enabled: json['aec3Enabled'] as bool? ?? false,
  minVolumePercent: (json['minVolumePercent'] as num?)?.toInt() ?? 0,
  playbackGainPercent: (json['playbackGainPercent'] as num?)?.toInt() ?? 100,
  piperNoiseScale: (json['piperNoiseScale'] as num?)?.toDouble() ?? 0.667,
  piperLengthScale: (json['piperLengthScale'] as num?)?.toDouble() ?? 1.0,
  piperNoiseW: (json['piperNoiseW'] as num?)?.toDouble() ?? 0.8,
  piperSentenceSilence:
      (json['piperSentenceSilence'] as num?)?.toDouble() ?? 0.2,
  keepAlive: json['keepAlive'] as bool? ?? false,
  numberReplaceMode: (json['numberReplaceMode'] as num?)?.toInt() ?? 0,
  landscapeDrawerMode: (json['landscapeDrawerMode'] as num?)?.toInt() ?? 1,
  solidTopBar: json['solidTopBar'] as bool? ?? true,
  drawingSaveRelativePath:
      json['drawingSaveRelativePath'] as String? ?? 'Pictures/KGTTS/Drawings',
  quickCardAutoSaveOnExit: json['quickCardAutoSaveOnExit'] as bool? ?? false,
  asrSendToQuickSubtitle: json['asrSendToQuickSubtitle'] as bool? ?? true,
  pushToTalkMode: json['pushToTalkMode'] as bool? ?? false,
  pushToTalkConfirmInput: json['pushToTalkConfirmInput'] as bool? ?? false,
  floatingOverlayEnabled: json['floatingOverlayEnabled'] as bool? ?? false,
  floatingOverlayAutoDock: json['floatingOverlayAutoDock'] as bool? ?? false,
  speakerVerifyEnabled: json['speakerVerifyEnabled'] as bool? ?? false,
  speakerVerifyThreshold:
      (json['speakerVerifyThreshold'] as num?)?.toDouble() ?? 0.72,
);

Map<String, dynamic> _$$AppSettingsImplToJson(_$AppSettingsImpl instance) =>
    <String, dynamic>{
      'muteWhilePlaying': instance.muteWhilePlaying,
      'muteWhilePlayingDelaySec': instance.muteWhilePlayingDelaySec,
      'echoSuppression': instance.echoSuppression,
      'communicationMode': instance.communicationMode,
      'preferredInputType': instance.preferredInputType,
      'preferredOutputType': instance.preferredOutputType,
      'aec3Enabled': instance.aec3Enabled,
      'minVolumePercent': instance.minVolumePercent,
      'playbackGainPercent': instance.playbackGainPercent,
      'piperNoiseScale': instance.piperNoiseScale,
      'piperLengthScale': instance.piperLengthScale,
      'piperNoiseW': instance.piperNoiseW,
      'piperSentenceSilence': instance.piperSentenceSilence,
      'keepAlive': instance.keepAlive,
      'numberReplaceMode': instance.numberReplaceMode,
      'landscapeDrawerMode': instance.landscapeDrawerMode,
      'solidTopBar': instance.solidTopBar,
      'drawingSaveRelativePath': instance.drawingSaveRelativePath,
      'quickCardAutoSaveOnExit': instance.quickCardAutoSaveOnExit,
      'asrSendToQuickSubtitle': instance.asrSendToQuickSubtitle,
      'pushToTalkMode': instance.pushToTalkMode,
      'pushToTalkConfirmInput': instance.pushToTalkConfirmInput,
      'floatingOverlayEnabled': instance.floatingOverlayEnabled,
      'floatingOverlayAutoDock': instance.floatingOverlayAutoDock,
      'speakerVerifyEnabled': instance.speakerVerifyEnabled,
      'speakerVerifyThreshold': instance.speakerVerifyThreshold,
    };
