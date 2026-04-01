import 'package:flutter/services.dart';

import '../../core/constants/channel_names.dart';

/// Data source wrapping MethodChannel for model management.
class ModelChannelDataSource {
  ModelChannelDataSource();

  final _method = const MethodChannel(ChannelNames.models);

  Future<List<Map<String, dynamic>>> listVoicePacks() async {
    try {
      final result = await _method.invokeMethod<List<dynamic>>(
        'listVoicePacks',
      );
      return result
              ?.map((e) => Map<String, dynamic>.from(e as Map))
              .toList() ??
          [];
    } on PlatformException catch (e) {
      throw Exception('Failed to list voice packs: ${e.message}');
    }
  }

  Future<List<Map<String, dynamic>>> listAsrModels() async {
    try {
      final result = await _method.invokeMethod<List<dynamic>>(
        'listAsrModels',
      );
      return result
              ?.map((e) => Map<String, dynamic>.from(e as Map))
              .toList() ??
          [];
    } on PlatformException catch (e) {
      throw Exception('Failed to list ASR models: ${e.message}');
    }
  }

  Future<Map<String, dynamic>> importVoice(String filePath) async {
    try {
      final result = await _method.invokeMethod<Map<dynamic, dynamic>>(
        'importVoice',
        {'filePath': filePath},
      );
      return Map<String, dynamic>.from(result ?? {});
    } on PlatformException catch (e) {
      throw Exception('Failed to import voice: ${e.message}');
    }
  }

  Future<Map<String, dynamic>> importAsr(String filePath) async {
    try {
      final result = await _method.invokeMethod<Map<dynamic, dynamic>>(
        'importAsr',
        {'filePath': filePath},
      );
      return Map<String, dynamic>.from(result ?? {});
    } on PlatformException catch (e) {
      throw Exception('Failed to import ASR: ${e.message}');
    }
  }

  Future<void> deleteVoice(String dirName) async {
    try {
      await _method.invokeMethod<void>(
        'deleteVoice',
        {'dirName': dirName},
      );
    } on PlatformException catch (e) {
      throw Exception('Failed to delete voice: ${e.message}');
    }
  }

  Future<void> updateVoiceMeta(
    String dirName,
    Map<String, dynamic> meta,
  ) async {
    try {
      await _method.invokeMethod<void>('updateVoiceMeta', {
        'dirName': dirName,
        'meta': meta,
      });
    } on PlatformException catch (e) {
      throw Exception('Failed to update voice meta: ${e.message}');
    }
  }

  Future<void> updateVoiceAvatar(String dirName, String imagePath) async {
    try {
      await _method.invokeMethod<void>('updateVoiceAvatar', {
        'dirName': dirName,
        'imagePath': imagePath,
      });
    } on PlatformException catch (e) {
      throw Exception('Failed to update avatar: ${e.message}');
    }
  }

  Future<void> exportVoice(String dirName, String destPath) async {
    try {
      await _method.invokeMethod<void>('exportVoice', {
        'dirName': dirName,
        'destPath': destPath,
      });
    } on PlatformException catch (e) {
      throw Exception('Failed to export voice: ${e.message}');
    }
  }

  Future<String?> ensureBundledAsr() async {
    try {
      return await _method.invokeMethod<String>('ensureBundledAsr');
    } on PlatformException catch (e) {
      throw Exception('Failed to ensure bundled ASR: ${e.message}');
    }
  }

  Future<String?> getLastVoiceName() async {
    try {
      return await _method.invokeMethod<String>('getLastVoiceName');
    } on PlatformException catch (e) {
      throw Exception('Failed to get last voice name: ${e.message}');
    }
  }

  Future<void> setLastVoiceName(String name) async {
    try {
      await _method.invokeMethod<void>(
        'setLastVoiceName',
        {'name': name},
      );
    } on PlatformException catch (e) {
      throw Exception('Failed to set last voice name: ${e.message}');
    }
  }
}
