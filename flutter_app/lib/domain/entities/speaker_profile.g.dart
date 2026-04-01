// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'speaker_profile.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$SpeakerProfileImpl _$$SpeakerProfileImplFromJson(Map<String, dynamic> json) =>
    _$SpeakerProfileImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      vector: (json['vector'] as List<dynamic>)
          .map((e) => (e as num).toDouble())
          .toList(),
    );

Map<String, dynamic> _$$SpeakerProfileImplToJson(
  _$SpeakerProfileImpl instance,
) => <String, dynamic>{
  'id': instance.id,
  'name': instance.name,
  'vector': instance.vector,
};
