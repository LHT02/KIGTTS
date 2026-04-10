import '../../domain/entities/app_settings.dart';
import '../../domain/repositories/settings_repository.dart';
import '../datasources/settings_local.dart';

/// Implementation of SettingsRepository using SharedPreferences.
class SettingsRepositoryImpl implements SettingsRepository {
  SettingsRepositoryImpl({required this.dataSource});

  final SettingsLocalDataSource dataSource;

  @override
  Future<AppSettings> getSettings() => dataSource.getSettings();

  @override
  Stream<AppSettings> observeSettings() => dataSource.observeSettings();

  @override
  Future<void> updateSetting(String key, dynamic value) {
    return dataSource.updateSetting(key, value);
  }

  @override
  Future<void> updateSettings(Map<String, dynamic> settings) {
    return dataSource.updateSettings(settings);
  }

  @override
  Future<String?> getJsonSetting(String key) {
    return dataSource.getJsonSetting(key);
  }

  @override
  Future<void> setJsonSetting(String key, String value) {
    return dataSource.setJsonSetting(key, value);
  }
}
