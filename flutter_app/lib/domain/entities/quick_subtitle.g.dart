// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'quick_subtitle.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$QuickSubtitleItemImpl _$$QuickSubtitleItemImplFromJson(
  Map<String, dynamic> json,
) => _$QuickSubtitleItemImpl(
  id: json['id'] as String,
  text: json['text'] as String,
  order: (json['order'] as num?)?.toInt() ?? 0,
);

Map<String, dynamic> _$$QuickSubtitleItemImplToJson(
  _$QuickSubtitleItemImpl instance,
) => <String, dynamic>{
  'id': instance.id,
  'text': instance.text,
  'order': instance.order,
};

_$QuickSubtitleGroupImpl _$$QuickSubtitleGroupImplFromJson(
  Map<String, dynamic> json,
) => _$QuickSubtitleGroupImpl(
  id: json['id'] as String,
  name: json['name'] as String,
  icon: json['icon'] as String? ?? '',
  items:
      (json['items'] as List<dynamic>?)
          ?.map((e) => QuickSubtitleItem.fromJson(e as Map<String, dynamic>))
          .toList() ??
      const [],
);

Map<String, dynamic> _$$QuickSubtitleGroupImplToJson(
  _$QuickSubtitleGroupImpl instance,
) => <String, dynamic>{
  'id': instance.id,
  'name': instance.name,
  'icon': instance.icon,
  'items': instance.items,
};

_$QuickSubtitleConfigImpl _$$QuickSubtitleConfigImplFromJson(
  Map<String, dynamic> json,
) => _$QuickSubtitleConfigImpl(
  groups:
      (json['groups'] as List<dynamic>?)
          ?.map((e) => QuickSubtitleGroup.fromJson(e as Map<String, dynamic>))
          .toList() ??
      const [],
  fontSize: (json['fontSize'] as num?)?.toDouble() ?? 48.0,
  bold: json['bold'] as bool? ?? false,
  centered: json['centered'] as bool? ?? false,
  playOnSend: json['playOnSend'] as bool? ?? true,
);

Map<String, dynamic> _$$QuickSubtitleConfigImplToJson(
  _$QuickSubtitleConfigImpl instance,
) => <String, dynamic>{
  'groups': instance.groups,
  'fontSize': instance.fontSize,
  'bold': instance.bold,
  'centered': instance.centered,
  'playOnSend': instance.playOnSend,
};
