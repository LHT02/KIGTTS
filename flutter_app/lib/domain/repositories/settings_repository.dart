import '../entities/app_settings.dart';

/// Repository interface for application settings.
abstract class SettingsRepository {
  /// Get current settings snapshot.
  Future<AppSettings> getSettings();

  /// Observe settings changes as a stream.
  Stream<AppSettings> observeSettings();

  /// Update a single setting by key.
  Future<void> updateSetting(String key, dynamic value);

  /// Update multiple settings at once.
  Future<void> updateSettings(Map<String, dynamic> settings);

  /// Get a JSON string setting (for complex configs like quick subtitle).
  Future<String?> getJsonSetting(String key);

  /// Set a JSON string setting.
  Future<void> setJsonSetting(String key, String value);
}
