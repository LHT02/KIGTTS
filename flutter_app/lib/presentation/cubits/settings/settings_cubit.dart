import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/constants/prefs_keys.dart';
import '../../../domain/entities/app_settings.dart';
import '../../../domain/repositories/realtime_repository.dart';
import '../../../domain/repositories/settings_repository.dart';
import 'settings_state.dart';

/// Cubit managing application settings.
class SettingsCubit extends Cubit<SettingsState> {
  SettingsCubit({
    required SettingsRepository settingsRepository,
    required RealtimeRepository realtimeRepository,
  })  : _settingsRepo = settingsRepository,
        _realtimeRepo = realtimeRepository,
        super(const SettingsState());

  final SettingsRepository _settingsRepo;
  final RealtimeRepository _realtimeRepo;
  StreamSubscription<AppSettings>? _sub;

  /// Load current settings and start observing changes.
  Future<void> initialize() async {
    emit(state.copyWith(loading: true));
    try {
      final settings = await _settingsRepo.getSettings();
      emit(state.copyWith(settings: settings, loading: false));

      _sub = _settingsRepo.observeSettings().listen((s) {
        emit(state.copyWith(settings: s));
      });
    } catch (e) {
      emit(state.copyWith(loading: false, error: e.toString()));
    }
  }

  // --- Convenience setters for each setting ---

  Future<void> setMuteWhilePlaying(bool v) =>
      _update(PrefsKeys.muteWhilePlaying, v);

  Future<void> setMuteDelay(double v) =>
      _update(PrefsKeys.muteWhilePlayingDelaySec, v);

  Future<void> setEchoSuppression(bool v) =>
      _update(PrefsKeys.echoSuppression, v);

  Future<void> setCommunicationMode(bool v) =>
      _update(PrefsKeys.communicationMode, v);

  Future<void> setPreferredInputType(int v) =>
      _update(PrefsKeys.preferredInputType, v);

  Future<void> setPreferredOutputType(int v) =>
      _update(PrefsKeys.preferredOutputType, v);

  Future<void> setAec3Enabled(bool v) =>
      _update(PrefsKeys.aec3Enabled, v);

  Future<void> setMinVolumePercent(int v) =>
      _update(PrefsKeys.minVolumePercent, v);

  Future<void> setPlaybackGainPercent(int v) =>
      _update(PrefsKeys.playbackGainPercent, v);

  Future<void> setPiperNoiseScale(double v) =>
      _update(PrefsKeys.piperNoiseScale, v);

  Future<void> setPiperLengthScale(double v) =>
      _update(PrefsKeys.piperLengthScale, v);

  Future<void> setPiperNoiseW(double v) =>
      _update(PrefsKeys.piperNoiseW, v);

  Future<void> setPiperSentenceSilence(double v) =>
      _update(PrefsKeys.piperSentenceSilence, v);

  Future<void> setKeepAlive(bool v) =>
      _update(PrefsKeys.keepAlive, v);

  Future<void> setNumberReplaceMode(int v) =>
      _update(PrefsKeys.numberReplaceMode, v);

  Future<void> setLandscapeDrawerMode(int v) =>
      _update(PrefsKeys.landscapeDrawerMode, v);

  Future<void> setSolidTopBar(bool v) =>
      _update(PrefsKeys.solidTopBar, v);

  Future<void> setPushToTalkMode(bool v) =>
      _update(PrefsKeys.pushToTalkMode, v);

  Future<void> setPushToTalkConfirmInput(bool v) =>
      _update(PrefsKeys.pushToTalkConfirmInput, v);

  Future<void> setFloatingOverlayEnabled(bool v) =>
      _update(PrefsKeys.floatingOverlayEnabled, v);

  Future<void> setSpeakerVerifyEnabled(bool v) =>
      _update(PrefsKeys.speakerVerifyEnabled, v);

  Future<void> setSpeakerVerifyThreshold(double v) =>
      _update(PrefsKeys.speakerVerifyThreshold, v);

  /// Generic update: persist locally + push to native.
  Future<void> _update(String key, dynamic value) async {
    try {
      await _settingsRepo.updateSetting(key, value);
      // Settings stream will auto-update state
      // Also push to native
      final updated = await _settingsRepo.getSettings();
      await _realtimeRepo.updateSettings(updated);
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  void clearError() => emit(state.copyWith(error: null));

  @override
  Future<void> close() {
    _sub?.cancel();
    return super.close();
  }
}
