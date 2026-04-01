import 'package:freezed_annotation/freezed_annotation.dart';

part 'quick_subtitle.freezed.dart';
part 'quick_subtitle.g.dart';

/// A single quick subtitle item.
@freezed
abstract class QuickSubtitleItem with _$QuickSubtitleItem {
  const factory QuickSubtitleItem({
    required String id,
    required String text,
    @Default(0) int order,
  }) = _QuickSubtitleItem;

  factory QuickSubtitleItem.fromJson(Map<String, dynamic> json) =>
      _$QuickSubtitleItemFromJson(json);
}

/// A group of quick subtitle items.
@freezed
abstract class QuickSubtitleGroup with _$QuickSubtitleGroup {
  const factory QuickSubtitleGroup({
    required String id,
    required String name,
    @Default([]) List<QuickSubtitleItem> items,
  }) = _QuickSubtitleGroup;

  factory QuickSubtitleGroup.fromJson(Map<String, dynamic> json) =>
      _$QuickSubtitleGroupFromJson(json);
}

/// Full quick subtitle configuration.
@freezed
abstract class QuickSubtitleConfig with _$QuickSubtitleConfig {
  const factory QuickSubtitleConfig({
    @Default([]) List<QuickSubtitleGroup> groups,
    @Default(28.0) double fontSize,
    @Default(false) bool bold,
    @Default(true) bool centered,
    @Default(true) bool playOnSend,
  }) = _QuickSubtitleConfig;

  factory QuickSubtitleConfig.fromJson(Map<String, dynamic> json) =>
      _$QuickSubtitleConfigFromJson(json);
}
