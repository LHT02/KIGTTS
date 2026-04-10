import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:permission_handler/permission_handler.dart';
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

  /// Callback invoked when ASR produces a final recognized text.
  /// Set by QuickSubtitlePage to forward results to the subtitle display.
  void Function(String text)? onAsrResultForSubtitle;

  /// Initialize: load bundled ASR, last voice, start listening to events.
  Future<void> initialize() async {
    emit(state.copyWith(loading: true, status: '初始化中...'));
    try {
      // Ensure bundled ASR is extracted (may be null if no bundled asset)
      String? asrDir;
      String? asrError;
      try {
        asrDir = await _modelRepo.ensureBundledAsr();
      } catch (e) {
        asrError = e.toString();
      }

      // Ensure bundled voice pack (firefly) is extracted
      String? voiceError;
      try {
        await _modelRepo.ensureBundledVoice();
      } catch (e) {
        voiceError = e.toString();
      }

      // Get last used voice pack
      final lastVoiceName = await _modelRepo.getLastVoiceName();
      String? voiceDir;
      final packs = await _modelRepo.listVoicePacks();
      if (lastVoiceName != null) {
        final match = packs.where((p) => p.dirName == lastVoiceName);
        if (match.isNotEmpty) {
          voiceDir = match.first.dirPath;
        }
      }
      // Fall back to first available voice pack (e.g. bundled firefly)
      if (voiceDir == null && packs.isNotEmpty) {
        voiceDir = packs.first.dirPath;
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

      // Build init diagnostic
      final diag = <String>[];
      if (asrDir == null) {
        diag.add('ASR: not loaded${asrError != null ? " ($asrError)" : ""}');
      }
      if (voiceDir == null) {
        diag.add('Voice: not loaded${voiceError != null ? " ($voiceError)" : ""}');
      }
      final initError = diag.isNotEmpty ? diag.join('\n') : null;

      emit(state.copyWith(
        loading: false,
        status: initError != null ? '部分初始化' : '就绪',
        error: initError,
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
    _eventSub = _realtimeRepo.events.listen(
      _handleEvent,
      onError: (e) {
        emit(state.copyWith(error: '事件流错误: $e'));
      },
    );
  }

  void _handleEvent(RealtimeEvent event) {
    switch (event) {
      case RealtimeResult(:final id, :final text):
        final item = RecognizedItem(id: id, text: text);
        final updated = [...state.recognized, item];
        final trimmed =
            updated.length > 100 ? updated.sublist(updated.length - 100) : updated;
        emit(state.copyWith(recognized: trimmed));
        // Forward to subtitle display if enabled.
        // Do NOT forward during PTT sessions — PTT results are
        // handled separately via commitPttSession.
        if (text.trim().isNotEmpty && !state.pttPressed && !state.recording) {
          onAsrResultForSubtitle?.call(text.trim());
        }

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

  /// Start the realtime pipeline (ASR + TTS + microphone).
  ///
  /// If ASR model is not loaded, shows a clear error to the user
  /// instead of silently falling back to TTS-only mode.
  Future<void> start() async {
    if (state.running) return;
    final asrDir = state.currentAsrDir;
    final voiceDir = state.currentVoiceDir;
    if (voiceDir == null) {
      emit(state.copyWith(error: '请先加载语音包'));
      return;
    }
    if (asrDir == null) {
      emit(state.copyWith(
        error: 'ASR 模型未加载，无法启动语音识别。\n请在语音包页面导入 ASR 模型。',
        status: 'ASR 未就绪',
      ));
      return;
    }
    try {
      emit(state.copyWith(status: '启动中...', loading: true));

      // Request microphone permission (required on Android 6.0+)
      final micStatus = await Permission.microphone.request();
      if (!micStatus.isGranted) {
        emit(state.copyWith(
          loading: false,
          status: '权限被拒绝',
          error: '需要麦克风权限才能进行语音识别。\n'
              '请在系统设置中允许本应用使用麦克风。',
        ));
        return;
      }

      final ok = await _realtimeRepo.start(
        asrDir: asrDir,
        voiceDir: voiceDir,
      );
      if (ok) {
        final settings = await _settingsRepo.getSettings();
        if (settings.keepAlive) {
          await _keepaliveRepo.start();
        }
        emit(state.copyWith(
          running: true,
          ttsOnly: false,
          loading: false,
          status: '运行中',
          error: null,
        ));
      } else {
        emit(state.copyWith(
          loading: false,
          status: '启动失败',
          error: 'start() returned false\n'
              'asrDir: $asrDir\n'
              'voiceDir: $voiceDir',
        ));
      }
    } catch (e) {
      emit(state.copyWith(
        loading: false,
        status: '启动失败',
        error: e.toString(),
      ));
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
        ttsOnly: false,
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

  /// Start TTS-only mode (no microphone / no ASR).
  ///
  /// Used when the user just wants to play text (send subtitle, tap play)
  /// without opening the microphone for recognition.
  Future<void> startTtsOnly() async {
    if (state.running) return;
    final voiceDir = state.currentVoiceDir;
    if (voiceDir == null) {
      emit(state.copyWith(error: '请先加载语音包'));
      return;
    }
    try {
      emit(state.copyWith(status: '加载语音包...'));
      final ok = await _realtimeRepo.loadVoice(voiceDir);
      if (ok) {
        emit(state.copyWith(
          running: true,
          ttsOnly: true,
          status: '仅TTS模式',
          error: null,
        ));
      } else {
        emit(state.copyWith(
          status: '语音包加载失败',
          error: 'loadVoice() returned false\nvoiceDir: $voiceDir',
        ));
      }
    } catch (e) {
      emit(state.copyWith(status: '启动失败', error: e.toString()));
    }
  }

  /// Start PTT (Push-to-Talk) recording.
  /// Updates UI to show recording state and calls repo.
  Future<void> startPTT() async {
    if (state.recording) return;
    
    try {
      emit(state.copyWith(recording: true, status: '录音中...'));
      await _realtimeRepo.beginPttSession();
    } catch (e) {
      emit(state.copyWith(
        recording: false,
        error: e.toString(),
        status: '录音错误',
      ));
    }
  }

  /// Stop PTT (Push-to-Talk) recording.
  /// Updates UI and triggers TTS playback of recorded text.
  Future<void> stopPTT() async {
    if (!state.recording) return;
    
    try {
      emit(state.copyWith(recording: false, status: '停止录音...'));
      await _realtimeRepo.commitPttSession('speak');
      emit(state.copyWith(status: '已停止录音'));
    } catch (e) {
      emit(state.copyWith(
        error: e.toString(),
        status: '停止录音错误',
      ));
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
  /// When pressed: auto-start pipeline if needed, then begin PTT session.
  /// When released: commit PTT session with 'speak' action.
  Future<void> setPttPressed(bool pressed) async {
    emit(state.copyWith(pttPressed: pressed));
    if (pressed) {
      // Auto-start pipeline if not running
      if (!state.running) {
        await start();
        if (!state.running) return; // start failed
      }
      await startPTT();
    } else {
      await stopPTT();
    }
  }

  /// Toggle Push-to-Talk mode on/off.
  void setPttMode(bool enabled) {
    emit(state.copyWith(pttMode: enabled));
  }

  /// Toggle confirm-input sub-mode for PTT.
  void setPttConfirmInputMode(bool enabled) {
    emit(state.copyWith(pttConfirmInputMode: enabled));
  }

  /// Update the drag target during confirm PTT gesture.
  void setPttDragTarget(String target) {
    emit(state.copyWith(pttDragTarget: target));
  }

  /// Begin a PTT session (auto-starts pipeline if needed).
  Future<void> beginPttSession() async {
    if (!state.running) {
      await start();
      if (!state.running) return;
    }
    try {
      emit(state.copyWith(
        pttPressed: true,
        pttStreamingText: '',
        pttDragTarget: 'SendToSubtitle',
        recording: true,
        status: '录音中...',
      ));
      await _realtimeRepo.beginPttSession();
    } catch (e) {
      emit(state.copyWith(
        pttPressed: false,
        error: e.toString(),
      ));
    }
  }

  /// Commit a PTT session with the specified action.
  /// [action] is one of: 'SendToSubtitle', 'SendToInput', 'Cancel', 'speak'
  Future<void> commitPttSession(String action) async {
    try {
      emit(state.copyWith(
        pttPressed: false,
        recording: false,
        pttDragTarget: '',
      ));
      await _realtimeRepo.commitPttSession(action);
      emit(state.copyWith(status: state.running ? '运行中' : '就绪'));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  /// Set preferred input device type and persist.
  Future<void> setPreferredInputType(int typeValue) async {
    try {
      await _settingsRepo.updateSetting('preferred_input_type', typeValue);
    } catch (_) {}
  }

  /// Set preferred output device type and persist.
  Future<void> setPreferredOutputType(int typeValue) async {
    try {
      await _settingsRepo.updateSetting('preferred_output_type', typeValue);
    } catch (_) {}
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
