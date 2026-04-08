import '../entities/app_settings.dart';
import '../entities/realtime_event.dart';

/// Repository interface for real-time ASR/TTS operations.
/// Implemented by data layer using MethodChannel + EventChannel.
abstract class RealtimeRepository {
  /// Start the realtime ASR/TTS pipeline.
  Future<bool> start({required String asrDir, required String voiceDir});

  /// Stop the realtime pipeline.
  Future<void> stop();

  /// Push all current settings to the native RealtimeController.
  Future<void> updateSettings(AppSettings settings);

  /// Enqueue text for TTS playback.
  Future<void> enqueueTts(String text);

  /// Load a new ASR model at runtime.
  Future<bool> loadAsr(String dir);

  /// Load a new voice pack at runtime.
  Future<bool> loadVoice(String dir);

  /// Set push-to-talk pressed state.
  Future<void> setPttPressed(bool pressed);

  /// Begin a push-to-talk session.
  Future<void> beginPttSession();

  /// Commit a push-to-talk session with action (speak/cancel).
  Future<void> commitPttSession(String action);

  /// Stream of real-time events from native engine.
  Stream<RealtimeEvent> get events;
}
