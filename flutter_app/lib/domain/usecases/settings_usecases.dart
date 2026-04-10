import '../entities/app_settings.dart';
import '../repositories/settings_repository.dart';

/// Get current settings snapshot.
class GetSettingsUseCase {
  GetSettingsUseCase(this._repository);
  final SettingsRepository _repository;

  Future<AppSettings> call() => _repository.getSettings();
}

/// Observe settings changes as a stream.
class ObserveSettingsUseCase {
  ObserveSettingsUseCase(this._repository);
  final SettingsRepository _repository;

  Stream<AppSettings> call() => _repository.observeSettings();
}

/// Update a single setting by key.
class UpdateSettingUseCase {
  UpdateSettingUseCase(this._repository);
  final SettingsRepository _repository;

  Future<void> call(String key, dynamic value) =>
      _repository.updateSetting(key, value);
}
