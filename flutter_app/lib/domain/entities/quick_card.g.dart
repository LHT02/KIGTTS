// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'quick_card.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$QuickCardImpl _$$QuickCardImplFromJson(Map<String, dynamic> json) =>
    _$QuickCardImpl(
      id: json['id'] as String,
      type: $enumDecode(_$QuickCardTypeEnumMap, json['type']),
      title: json['title'] as String? ?? '',
      content: json['content'] as String? ?? '',
      imagePath: json['imagePath'] as String?,
      order: (json['order'] as num?)?.toInt() ?? 0,
    );

Map<String, dynamic> _$$QuickCardImplToJson(_$QuickCardImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'type': _$QuickCardTypeEnumMap[instance.type]!,
      'title': instance.title,
      'content': instance.content,
      'imagePath': instance.imagePath,
      'order': instance.order,
    };

const _$QuickCardTypeEnumMap = {
  QuickCardType.image: 'image',
  QuickCardType.qr: 'qr',
  QuickCardType.text: 'text',
};
