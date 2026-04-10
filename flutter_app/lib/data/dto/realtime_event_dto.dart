import '../../domain/entities/realtime_event.dart';

/// DTO for converting native EventChannel maps to RealtimeEvent entities.
abstract final class RealtimeEventDto {
  /// Parse a map from EventChannel into a RealtimeEvent.
  static RealtimeEvent fromMap(Map<String, dynamic> map) {
    final type = map['type'] as String;
    return switch (type) {
      'result' => RealtimeEvent.result(
          id: (map['id'] as num).toInt(),
          text: map['text'] as String,
        ),
      'streaming' => RealtimeEvent.streaming(
          text: map['text'] as String,
        ),
      'progress' => RealtimeEvent.progress(
          id: (map['id'] as num).toInt(),
          value: (map['value'] as num).toDouble(),
        ),
      'level' => RealtimeEvent.level(
          value: (map['value'] as num).toDouble(),
        ),
      'inputDevice' => RealtimeEvent.inputDevice(
          label: map['label'] as String,
        ),
      'outputDevice' => RealtimeEvent.outputDevice(
          label: map['label'] as String,
        ),
      'aec3Status' => RealtimeEvent.aec3Status(
          status: map['status'] as String,
        ),
      'speakerVerify' => RealtimeEvent.speakerVerify(
          similarity: (map['similarity'] as num).toDouble(),
          accepted: map['accepted'] as bool,
        ),
      'error' => RealtimeEvent.error(
          message: map['message'] as String,
        ),
      _ => RealtimeEvent.error(message: 'Unknown event type: $type'),
    };
  }
}
