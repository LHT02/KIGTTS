import 'package:freezed_annotation/freezed_annotation.dart';
import '../../../domain/entities/quick_subtitle.dart';

part 'quick_subtitle_state.freezed.dart';

@freezed
abstract class QuickSubtitleState with _$QuickSubtitleState {
  const factory QuickSubtitleState({
    @Default(QuickSubtitleConfig()) QuickSubtitleConfig config,
    @Default(0) int selectedGroupIndex,
    @Default(-1) int selectedItemIndex,
    @Default('') String displayText,
    @Default('') String inputText,
    @Default(true) bool presetsVisible,
    @Default(false) bool loading,
    String? error,
  }) = _QuickSubtitleState;
}
