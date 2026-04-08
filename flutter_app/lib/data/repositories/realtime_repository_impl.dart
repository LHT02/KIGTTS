import '../../domain/entities/app_settings.dart';
import '../../domain/entities/realtime_event.dart';
import '../../domain/repositories/realtime_repository.dart';
import '../datasources/realtime_channel.dart';
import '../dto/settings_dto.dart';

/// Implementation of RealtimeRepository using MethodChannel.
class RealtimeRepositoryImpl implements RealtimeRepository {
  RealtimeRepositoryImpl({required this.dataSource});

  final RealtimeChannelDataSource dataSource;

  @override
  Future<bool> start({
    required String asrDir,
    required String voiceDir,
  }) {
    return dataSource.start(asrDir, voiceDir);
  }

  @override
  Future<void> stop() => dataSource.stop();

  @override
  Future<void> updateSettings(AppSettings settings) {
    return dataSource.updateSettings(SettingsDto.toChannelMap(settings));
  }

  @override
  Future<void> enqueueTts(String text) => dataSource.enqueueTts(text);

  @override
  Future<bool> loadAsr(String dir) => dataSource.loadAsr(dir);

  @override
  Future<bool> loadVoice(String dir) => dataSource.loadVoice(dir);

  @override
  Future<void> setPttPressed(bool pressed) {
    return dataSource.setPttPressed(pressed);
  }

  @override
  Future<void> beginPttSession() => dataSource.beginPttSession();

  @override
  Future<void> commitPttSession(String action) {
    return dataSource.commitPttSession(action);
  }

  @override
  Stream<RealtimeEvent> get events => dataSource.events;
}
