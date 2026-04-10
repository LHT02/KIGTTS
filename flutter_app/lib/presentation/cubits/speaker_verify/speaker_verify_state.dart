import 'package:freezed_annotation/freezed_annotation.dart';

part 'speaker_verify_state.freezed.dart';

@freezed
abstract class SpeakerVerifyState with _$SpeakerVerifyState {
  const factory SpeakerVerifyState({
    @Default(false) bool enabled,
    @Default(0.72) double threshold,
    @Default([]) List<String> profileIds,
    @Default(false) bool enrolling,
    @Default(0.0) double enrollProgress,
    @Default(0.0) double enrollLevel,
    @Default(-1.0) double lastSimilarity,
    String? enrollResult,
    String? error,
  }) = _SpeakerVerifyState;
}
