import 'dart:async';

import 'package:shared_preferences/shared_preferences.dart';

import '../../core/constants/prefs_keys.dart';
import '../../domain/entities/app_settings.dart';

/// Data source wrapping SharedPreferences for settings storage.
class SettingsLocalDataSource {
  SettingsLocalDataSource();

  SharedPreferences? _prefs;
  final _controller = StreamController<AppSettings>.broadcast();

  Future<SharedPreferences> get _sharedPrefs async {
    _prefs ??= await SharedPreferences.getInstance();
    return _prefs!;
  }

  /// Load all settings from SharedPreferences into AppSettings.
  Future<AppSettings> getSettings() async {
    final prefs = await _sharedPrefs;
    return AppSettings(
      muteWhilePlaying:
          prefs.getBool(PrefsKeys.muteWhilePlaying) ?? false,
      muteWhilePlayingDelaySec:
          prefs.getDouble(PrefsKeys.muteWhilePlayingDelaySec) ?? 0.0,
      echoSuppression:
          prefs.getBool(PrefsKeys.echoSuppression) ?? false,
      communicationMode:
          prefs.getBool(PrefsKeys.communicationMode) ?? false,
      preferredInputType:
          prefs.getInt(PrefsKeys.preferredInputType) ?? 0,
      preferredOutputType:
          prefs.getInt(PrefsKeys.preferredOutputType) ?? 100,
      aec3Enabled: prefs.getBool(PrefsKeys.aec3Enabled) ?? false,
      minVolumePercent:
          prefs.getInt(PrefsKeys.minVolumePercent) ?? 0,
      playbackGainPercent:
          prefs.getInt(PrefsKeys.playbackGainPercent) ?? 100,
      piperNoiseScale:
          prefs.getDouble(PrefsKeys.piperNoiseScale) ?? 0.667,
      piperLengthScale:
          prefs.getDouble(PrefsKeys.piperLengthScale) ?? 1.0,
      piperNoiseW: prefs.getDouble(PrefsKeys.piperNoiseW) ?? 0.8,
      piperSentenceSilence:
          prefs.getDouble(PrefsKeys.piperSentenceSilence) ?? 0.2,
      keepAlive: prefs.getBool(PrefsKeys.keepAlive) ?? false,
      numberReplaceMode:
          prefs.getInt(PrefsKeys.numberReplaceMode) ?? 0,
      landscapeDrawerMode:
          prefs.getInt(PrefsKeys.landscapeDrawerMode) ?? 1,
      solidTopBar: prefs.getBool(PrefsKeys.solidTopBar) ?? true,
      drawingSaveRelativePath:
          prefs.getString(PrefsKeys.drawingSaveRelativePath) ??
              'Pictures/KGTTS/Drawings',
      quickCardAutoSaveOnExit:
          prefs.getBool(PrefsKeys.quickCardAutoSaveOnExit) ?? false,
      asrSendToQuickSubtitle:
          prefs.getBool(PrefsKeys.asrSendToQuickSubtitle) ?? true,
      pushToTalkMode:
          prefs.getBool(PrefsKeys.pushToTalkMode) ?? false,
      pushToTalkConfirmInput:
          prefs.getBool(PrefsKeys.pushToTalkConfirmInput) ?? false,
      floatingOverlayEnabled:
          prefs.getBool(PrefsKeys.floatingOverlayEnabled) ?? false,
      floatingOverlayAutoDock:
          prefs.getBool(PrefsKeys.floatingOverlayAutoDock) ?? false,
      speakerVerifyEnabled:
          prefs.getBool(PrefsKeys.speakerVerifyEnabled) ?? false,
      speakerVerifyThreshold:
          prefs.getDouble(PrefsKeys.speakerVerifyThreshold) ?? 0.72,
    );
  }

  /// Observe settings changes.
  Stream<AppSettings> observeSettings() => _controller.stream;

  /// Update a single setting and notify listeners.
  Future<void> updateSetting(String key, dynamic value) async {
    final prefs = await _sharedPrefs;
    if (value is bool) {
      await prefs.setBool(key, value);
    } else if (value is int) {
      await prefs.setInt(key, value);
    } else if (value is double) {
      await prefs.setDouble(key, value);
    } else if (value is String) {
      await prefs.setString(key, value);
    }
    // Notify observers with updated settings
    final updated = await getSettings();
    _controller.add(updated);
  }

  /// Update multiple settings at once.
  Future<void> updateSettings(Map<String, dynamic> settings) async {
    final prefs = await _sharedPrefs;
    for (final entry in settings.entries) {
      final value = entry.value;
      if (value is bool) {
        await prefs.setBool(entry.key, value);
      } else if (value is int) {
        await prefs.setInt(entry.key, value);
      } else if (value is double) {
        await prefs.setDouble(entry.key, value);
      } else if (value is String) {
        await prefs.setString(entry.key, value);
      }
    }
    final updated = await getSettings();
    _controller.add(updated);
  }

  /// Get a JSON string setting.
  Future<String?> getJsonSetting(String key) async {
    final prefs = await _sharedPrefs;
    return prefs.getString(key);
  }

  /// Set a JSON string setting.
  Future<void> setJsonSetting(String key, String value) async {
    final prefs = await _sharedPrefs;
    await prefs.setString(key, value);
  }

  void dispose() {
    _controller.close();
  }
}
