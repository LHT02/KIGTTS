import 'package:freezed_annotation/freezed_annotation.dart';

part 'recognized_item.freezed.dart';

/// A single ASR recognition result with TTS playback progress.
@freezed
abstract class RecognizedItem with _$RecognizedItem {
  const factory RecognizedItem({
    required int id,
    required String text,
    @Default(0.0) double progress,
    @Default(false) bool playing,
    @Default(false) bool completed,
  }) = _RecognizedItem;
}
