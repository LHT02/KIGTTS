// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'voice_pack_info.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$VoicePackMetaImpl _$$VoicePackMetaImplFromJson(Map<String, dynamic> json) =>
    _$VoicePackMetaImpl(
      name: json['name'] as String? ?? '未命名',
      remark: json['remark'] as String? ?? '',
      avatar: json['avatar'] as String? ?? 'avatar.png',
      pinned: json['pinned'] as bool? ?? false,
      order: (json['order'] as num?)?.toInt() ?? 0,
    );

Map<String, dynamic> _$$VoicePackMetaImplToJson(_$VoicePackMetaImpl instance) =>
    <String, dynamic>{
      'name': instance.name,
      'remark': instance.remark,
      'avatar': instance.avatar,
      'pinned': instance.pinned,
      'order': instance.order,
    };

_$VoicePackInfoImpl _$$VoicePackInfoImplFromJson(Map<String, dynamic> json) =>
    _$VoicePackInfoImpl(
      dirName: json['dirName'] as String,
      dirPath: json['dirPath'] as String,
      meta: VoicePackMeta.fromJson(json['meta'] as Map<String, dynamic>),
      avatarPath: json['avatarPath'] as String?,
    );

Map<String, dynamic> _$$VoicePackInfoImplToJson(_$VoicePackInfoImpl instance) =>
    <String, dynamic>{
      'dirName': instance.dirName,
      'dirPath': instance.dirPath,
      'meta': instance.meta,
      'avatarPath': instance.avatarPath,
    };
