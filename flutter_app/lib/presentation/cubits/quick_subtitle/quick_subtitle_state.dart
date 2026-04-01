import 'package:freezed_annotation/freezed_annotation.dart';
import '../../../domain/entities/quick_subtitle.dart';

part 'quick_subtitle_state.freezed.dart';

@freezed
abstract class QuickSubtitleState with _$QuickSubtitleState {
  const factory QuickSubtitleState({
    @Default(QuickSubtitleConfig()) QuickSubtitleConfig config,
    @Default(0) int selectedGroupIndex,
    @Default('') String inputText,
    @Default(false) bool loading,
    String? error,
  }) = _QuickSubtitleState;
}
