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
    @Default('') String icon,
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
    @Default(48.0) double fontSize,
    @Default(false) bool bold,
    @Default(false) bool centered,
    @Default(true) bool playOnSend,
  }) = _QuickSubtitleConfig;

  factory QuickSubtitleConfig.fromJson(Map<String, dynamic> json) =>
      _$QuickSubtitleConfigFromJson(json);
}

/// Built-in default presets matching the original Android app.
QuickSubtitleConfig defaultQuickSubtitleConfig() {
  return const QuickSubtitleConfig(
    fontSize: 48.0,
    groups: [
      QuickSubtitleGroup(
        id: 'common',
        name: '常用',
        icon: 'emoji_emotions',
        items: [
          QuickSubtitleItem(
            id: 'c1',
            text: '您好，我现在不太方便说话',
            order: 0,
          ),
          QuickSubtitleItem(
            id: 'c2',
            text: '您好，可以加个好友吗',
            order: 1,
          ),
          QuickSubtitleItem(
            id: 'c3',
            text: '稍等一下，我马上回复',
            order: 2,
          ),
          QuickSubtitleItem(
            id: 'c4',
            text: '谢谢你的帮助',
            order: 3,
          ),
          QuickSubtitleItem(
            id: 'c5',
            text: '我不太方便说话，请等我一下……',
            order: 4,
          ),
          QuickSubtitleItem(
            id: 'c6',
            text: '不好意思，让你久等了',
            order: 5,
          ),
        ],
      ),
      QuickSubtitleGroup(
        id: 'game',
        name: '游戏',
        icon: 'sports_esports',
        items: [
          QuickSubtitleItem(
            id: 'g1',
            text: '集合了，快过来',
            order: 0,
          ),
          QuickSubtitleItem(
            id: 'g2',
            text: '小心敌人，注意隐蔽',
            order: 1,
          ),
          QuickSubtitleItem(
            id: 'g3',
            text: '好的，收到',
            order: 2,
          ),
          QuickSubtitleItem(
            id: 'g4',
            text: '打得好！继续保持',
            order: 3,
          ),
        ],
      ),
      QuickSubtitleGroup(
        id: 'work',
        name: '办公',
        icon: 'work',
        items: [
          QuickSubtitleItem(
            id: 'w1',
            text: '收到，我马上处理',
            order: 0,
          ),
          QuickSubtitleItem(
            id: 'w2',
            text: '请稍等，我确认一下',
            order: 1,
          ),
          QuickSubtitleItem(
            id: 'w3',
            text: '好的，没问题',
            order: 2,
          ),
          QuickSubtitleItem(
            id: 'w4',
            text: '这个问题我需要再研究一下',
            order: 3,
          ),
        ],
      ),
    ],
  );
}
