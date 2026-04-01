import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/entities/realtime_event.dart';
import '../../../domain/entities/recognized_item.dart';
import '../../../domain/repositories/keepalive_repository.dart';
import '../../../domain/repositories/model_repository.dart';
import '../../../domain/repositories/realtime_repository.dart';
import '../../../domain/repositories/settings_repository.dart';
import 'realtime_state.dart';

/// Cubit managing the real-time ASR/TTS conversion state.
class RealtimeCubit extends Cubit<RealtimeState> {
  RealtimeCubit({
    required RealtimeRepository realtimeRepository,
    required ModelRepository modelRepository,
    required SettingsRepository settingsRepository,
    required KeepaliveRepository keepaliveRepository,
  })  : _realtimeRepo = realtimeRepository,
        _modelRepo = modelRepository,
        _settingsRepo = settingsRepository,
        _keepaliveRepo = keepaliveRepository,
        super(const RealtimeState());

  final RealtimeRepository _realtimeRepo;
  final ModelRepository _modelRepo;
  final SettingsRepository _settingsRepo;
  final KeepaliveRepository _keepaliveRepo;

  StreamSubscription<RealtimeEvent>? _eventSub;
  StreamSubscription<dynamic>? _settingsSub;

  /// Initialize: load bundled ASR, last voice, start listening to events.
  Future<void> initialize() async {
    emit(state.copyWith(loading: true, status: '初始化中...'));
    try {
      // Ensure bundled ASR is extracted
      final asrDir = await _modelRepo.ensureBundledAsr();

      // Get last used voice pack
      final lastVoiceName = await _modelRepo.getLastVoiceName();
      String? voiceDir;
      if (lastVoiceName != null) {
        final packs = await _modelRepo.listVoicePacks();
        final match = packs.where((p) => p.dirName == lastVoiceName);
        if (match.isNotEmpty) {
          voiceDir = match.first.dirPath;
        }
      }

      // Push current settings to native
      final settings = await _settingsRepo.getSettings();
      await _realtimeRepo.updateSettings(settings);

      // Listen to settings changes
      _settingsSub = _settingsRepo.observeSettings().listen((s) {
        _realtimeRepo.updateSettings(s);
      });

      // Subscribe to native events
      _subscribeEvents();

      emit(state.copyWith(
        loading: false,
        status: '就绪',
        currentAsrDir: asrDir,
        currentVoiceDir: voiceDir,
      ));
    } catch (e) {
      emit(state.copyWith(
        loading: false,
        error: e.toString(),
        status: '初始化失败',
      ));
    }
  }

  void _subscribeEvents() {
    _eventSub?.cancel();
    _eventSub = _realtimeRepo.events.listen(_handleEvent);
  }

  void _handleEvent(RealtimeEvent event) {
    switch (event) {
      case RealtimeResult(:final id, :final text):
        final item = RecognizedItem(id: id, text: text);
        final updated = [...state.recognized, item];
        // Keep max 100 items
        final trimmed =
            updated.length > 100 ? updated.sublist(updated.length - 100) : updated;
        emit(state.copyWith(recognized: trimmed));

      case RealtimeStreaming(:final text):
        emit(state.copyWith(pttStreamingText: text));

      case RealtimeProgress(:final id, :final value):
        final items = state.recognized.map((item) {
          if (item.id == id) {
            return item.copyWith(
              progress: value,
              playing: value < 1.0,
              completed: value >= 1.0,
            );
          }
          return item;
        }).toList();
        emit(state.copyWith(
          recognized: items,
          playingId: value < 1.0 ? id : -1,
          playbackProgress: value,
        ));

      case RealtimeLevel(:final value):
        emit(state.copyWith(inputLevel: value));

      case RealtimeInputDevice(:final label):
        emit(state.copyWith(inputDeviceLabel: label));

      case RealtimeOutputDevice(:final label):
        emit(state.copyWith(outputDeviceLabel: label));

      case RealtimeAec3Status(:final status):
        emit(state.copyWith(aec3Status: status));

      case RealtimeSpeakerVerify(:final similarity):
        emit(state.copyWith(speakerLastSimilarity: similarity));

      case RealtimeError(:final message):
        emit(state.copyWith(error: message));
    }
  }

  /// Start the realtime pipeline.
  Future<void> start() async {
    if (state.running) return;
    final asrDir = state.currentAsrDir;
    final voiceDir = state.currentVoiceDir;
    if (asrDir == null || voiceDir == null) {
      emit(state.copyWith(error: '请先加载 ASR 模型和语音包'));
      return;
    }
    try {
      emit(state.copyWith(status: '启动中...'));
      final ok = await _realtimeRepo.start(
        asrDir: asrDir,
        voiceDir: voiceDir,
      );
      if (ok) {
        // Start keepalive service if enabled
        final settings = await _settingsRepo.getSettings();
        if (settings.keepAlive) {
          await _keepaliveRepo.start();
        }
        emit(state.copyWith(running: true, status: '运行中', error: null));
      } else {
        emit(state.copyWith(status: '启动失败', error: '启动失败'));
      }
    } catch (e) {
      emit(state.copyWith(status: '启动失败', error: e.toString()));
    }
  }

  /// Stop the realtime pipeline.
  Future<void> stop() async {
    if (!state.running) return;
    try {
      await _realtimeRepo.stop();
      await _keepaliveRepo.stop();
      emit(state.copyWith(
        running: false,
        status: '已停止',
        inputLevel: 0,
        playbackProgress: 0,
      ));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  /// Toggle start/stop.
  Future<void> toggle() async {
    if (state.running) {
      await stop();
    } else {
      await start();
    }
  }

  /// Enqueue text for TTS playback.
  Future<void> enqueueTts(String text) async {
    try {
      await _realtimeRepo.enqueueTts(text);
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  /// Load a new ASR model.
  Future<void> loadAsr(String dir) async {
    try {
      final ok = await _realtimeRepo.loadAsr(dir);
      if (ok) {
        emit(state.copyWith(currentAsrDir: dir));
      }
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  /// Load a new voice pack.
  Future<void> loadVoice(String dirPath, String dirName) async {
    try {
      final ok = await _realtimeRepo.loadVoice(dirPath);
      if (ok) {
        emit(state.copyWith(currentVoiceDir: dirPath));
        await _modelRepo.setLastVoiceName(dirName);
      }
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  /// Set PTT pressed state.
  Future<void> setPttPressed(bool pressed) async {
    emit(state.copyWith(pttPressed: pressed));
    await _realtimeRepo.setPttPressed(pressed);
  }

  /// Clear error message.
  void clearError() {
    emit(state.copyWith(error: null));
  }

  /// Clear all recognized items.
  void clearRecognized() {
    emit(state.copyWith(recognized: []));
  }

  @override
  Future<void> close() {
    _eventSub?.cancel();
    _settingsSub?.cancel();
    return super.close();
  }
}
