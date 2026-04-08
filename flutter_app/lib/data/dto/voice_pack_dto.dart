import '../../domain/entities/voice_pack_info.dart';

/// DTO for converting native channel maps to VoicePackInfo entities.
abstract final class VoicePackDto {
  static VoicePackInfo fromMap(Map<String, dynamic> map) {
    return VoicePackInfo(
      dirName: map['dirName'] as String,
      dirPath: map['dirPath'] as String,
      meta: VoicePackMeta(
        name: map['name'] as String? ?? '未命名',
        remark: map['remark'] as String? ?? '',
        avatar: map['avatar'] as String? ?? 'avatar.png',
        pinned: map['pinned'] as bool? ?? false,
        order: (map['order'] as num?)?.toInt() ?? 0,
      ),
      avatarPath: map['avatarPath'] as String?,
    );
  }

  static Map<String, dynamic> metaToMap(VoicePackMeta meta) {
    return {
      'name': meta.name,
      'remark': meta.remark,
      'avatar': meta.avatar,
      'pinned': meta.pinned,
      'order': meta.order,
    };
  }
}
