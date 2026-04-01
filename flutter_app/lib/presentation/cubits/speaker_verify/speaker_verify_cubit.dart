import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/repositories/settings_repository.dart';
import '../../../core/constants/prefs_keys.dart';
import 'speaker_verify_state.dart';

/// Cubit managing speaker verification profiles and enrollment.
class SpeakerVerifyCubit extends Cubit<SpeakerVerifyState> {
  SpeakerVerifyCubit({
    required SettingsRepository settingsRepository,
  })  : _settingsRepo = settingsRepository,
        super(const SpeakerVerifyState());

  final SettingsRepository _settingsRepo;

  Future<void> initialize() async {
    try {
      final settings = await _settingsRepo.getSettings();
      emit(state.copyWith(
        enabled: settings.speakerVerifyEnabled,
        threshold: settings.speakerVerifyThreshold,
      ));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> setEnabled(bool enabled) async {
    emit(state.copyWith(enabled: enabled));
    await _settingsRepo.updateSetting(
      PrefsKeys.speakerVerifyEnabled,
      enabled,
    );
  }

  Future<void> setThreshold(double threshold) async {
    emit(state.copyWith(threshold: threshold));
    await _settingsRepo.updateSetting(
      PrefsKeys.speakerVerifyThreshold,
      threshold,
    );
  }

  void clearError() => emit(state.copyWith(error: null));
}
