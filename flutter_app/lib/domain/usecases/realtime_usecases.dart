import '../entities/realtime_event.dart';
import '../repositories/realtime_repository.dart';

/// Start the realtime ASR/TTS pipeline.
class StartRealtimeUseCase {
  StartRealtimeUseCase(this._repository);
  final RealtimeRepository _repository;

  Future<bool> call({required String asrDir, required String voiceDir}) {
    return _repository.start(asrDir: asrDir, voiceDir: voiceDir);
  }
}

/// Stop the realtime ASR/TTS pipeline.
class StopRealtimeUseCase {
  StopRealtimeUseCase(this._repository);
  final RealtimeRepository _repository;

  Future<void> call() => _repository.stop();
}

/// Observe realtime events from native engine.
class GetRealtimeEventsUseCase {
  GetRealtimeEventsUseCase(this._repository);
  final RealtimeRepository _repository;

  Stream<RealtimeEvent> call() => _repository.events;
}

/// Enqueue text for TTS playback.
class EnqueueTtsUseCase {
  EnqueueTtsUseCase(this._repository);
  final RealtimeRepository _repository;

  Future<void> call(String text) => _repository.enqueueTts(text);
}
