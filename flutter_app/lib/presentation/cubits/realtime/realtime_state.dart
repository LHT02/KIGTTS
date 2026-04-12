import 'package:freezed_annotation/freezed_annotation.dart';
import '../../../domain/entities/recognized_item.dart';

part 'realtime_state.freezed.dart';

/// State for the realtime ASR/TTS conversion page.
@freezed
abstract class RealtimeState with _$RealtimeState {
  const factory RealtimeState({
    @Default(false) bool running,
    @Default(false) bool ttsOnly,                // true = TTS only (no mic/ASR)
    @Default(false) bool recording,              // PTT recording state
    @Default(true) bool pttMode,                 // Push-to-talk mode flag
    @Default('待命') String status,
    @Default([]) List<RecognizedItem> recognized,
    @Default(0.0) double inputLevel,
    @Default(0.0) double playbackProgress,
    @Default(-1) int playingId,
    @Default('未知') String inputDeviceLabel,
    @Default('未知') String outputDeviceLabel,
    @Default('未启用') String aec3Status,
    @Default(false) bool pttConfirmInputMode,
    @Default(false) bool pttPressed,
    @Default('') String pttStreamingText,
    @Default('') String pttDragTarget,  // SendToSubtitle / SendToInput / Cancel
    @Default(-1.0) double speakerLastSimilarity,
    @Default(false) bool loading,
    String? error,
    String? currentAsrDir,
    String? currentVoiceDir,
  }) = _RealtimeState;
}
