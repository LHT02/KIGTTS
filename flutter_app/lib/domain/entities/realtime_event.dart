import 'package:freezed_annotation/freezed_annotation.dart';

part 'realtime_event.freezed.dart';

/// Events streamed from the native RealtimeController via EventChannel.
@freezed
sealed class RealtimeEvent with _$RealtimeEvent {
  /// Final ASR recognition result.
  const factory RealtimeEvent.result({
    required int id,
    required String text,
  }) = RealtimeResult;

  /// Partial/streaming ASR result (PTT mode).
  const factory RealtimeEvent.streaming({
    required String text,
  }) = RealtimeStreaming;

  /// TTS playback progress update.
  const factory RealtimeEvent.progress({
    required int id,
    required double value,
  }) = RealtimeProgress;

  /// Microphone input level (0.0 - 1.0).
  const factory RealtimeEvent.level({
    required double value,
  }) = RealtimeLevel;

  /// Current audio input device label.
  const factory RealtimeEvent.inputDevice({
    required String label,
  }) = RealtimeInputDevice;

  /// Current audio output device label.
  const factory RealtimeEvent.outputDevice({
    required String label,
  }) = RealtimeOutputDevice;

  /// AEC3 echo cancellation status.
  const factory RealtimeEvent.aec3Status({
    required String status,
  }) = RealtimeAec3Status;

  /// Speaker verification result.
  const factory RealtimeEvent.speakerVerify({
    required double similarity,
    required bool accepted,
  }) = RealtimeSpeakerVerify;

  /// Error from native engine.
  const factory RealtimeEvent.error({
    required String message,
  }) = RealtimeError;
}
