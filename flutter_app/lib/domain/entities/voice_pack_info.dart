import 'package:freezed_annotation/freezed_annotation.dart';

part 'voice_pack_info.freezed.dart';
part 'voice_pack_info.g.dart';

/// Voice pack metadata stored in voicepack.json.
@freezed
abstract class VoicePackMeta with _$VoicePackMeta {
  const factory VoicePackMeta({
    @Default('未命名') String name,
    @Default('') String remark,
    @Default('avatar.png') String avatar,
    @Default(false) bool pinned,
    @Default(0) int order,
  }) = _VoicePackMeta;

  factory VoicePackMeta.fromJson(Map<String, dynamic> json) =>
      _$VoicePackMetaFromJson(json);
}

/// Complete voice pack info with directory path and metadata.
@freezed
abstract class VoicePackInfo with _$VoicePackInfo {
  const factory VoicePackInfo({
    required String dirName,
    required String dirPath,
    required VoicePackMeta meta,
    String? avatarPath,
  }) = _VoicePackInfo;

  factory VoicePackInfo.fromJson(Map<String, dynamic> json) =>
      _$VoicePackInfoFromJson(json);
}
