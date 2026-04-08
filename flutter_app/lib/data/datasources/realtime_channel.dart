import 'dart:async';

import 'package:flutter/services.dart';

import '../../core/constants/channel_names.dart';
import '../../domain/entities/realtime_event.dart';
import '../dto/realtime_event_dto.dart';

/// Data source wrapping MethodChannel + EventChannel for realtime operations.
class RealtimeChannelDataSource {
  RealtimeChannelDataSource();

  final _method = const MethodChannel(ChannelNames.realtime);
  final _event = const EventChannel(ChannelNames.realtimeEvents);

  Stream<RealtimeEvent>? _eventStream;

  /// Stream of real-time events from native engine.
  Stream<RealtimeEvent> get events {
    _eventStream ??= _event
        .receiveBroadcastStream()
        .map((data) => RealtimeEventDto.fromMap(
              Map<String, dynamic>.from(data as Map),
            ))
        .handleError((e) {
      // Log and re-emit as error event
    });
    return _eventStream!;
  }

  Future<bool> start(String asrDir, String voiceDir) async {
    try {
      final result = await _method.invokeMethod<bool>('start', {
        'asrDir': asrDir,
        'voiceDir': voiceDir,
      });
      return result ?? false;
    } on PlatformException catch (e) {
      throw Exception('Failed to start realtime: ${e.message}');
    }
  }

  Future<void> stop() async {
    try {
      await _method.invokeMethod<void>('stop');
    } on PlatformException catch (e) {
      throw Exception('Failed to stop realtime: ${e.message}');
    }
  }

  Future<void> updateSettings(Map<String, dynamic> settings) async {
    try {
      await _method.invokeMethod<void>('updateSettings', settings);
    } on PlatformException catch (e) {
      throw Exception('Failed to update settings: ${e.message}');
    }
  }

  Future<void> enqueueTts(String text) async {
    try {
      await _method.invokeMethod<void>('enqueueTts', {'text': text});
    } on PlatformException catch (e) {
      throw Exception('Failed to enqueue TTS: ${e.message}');
    }
  }

  Future<bool> loadAsr(String dir) async {
    try {
      final result = await _method.invokeMethod<bool>(
        'loadAsr',
        {'dir': dir},
      );
      return result ?? false;
    } on PlatformException catch (e) {
      throw Exception('Failed to load ASR: ${e.message}');
    }
  }

  Future<bool> loadVoice(String dir) async {
    try {
      final result = await _method.invokeMethod<bool>(
        'loadVoice',
        {'dir': dir},
      );
      return result ?? false;
    } on PlatformException catch (e) {
      throw Exception('Failed to load voice: ${e.message}');
    }
  }

  Future<void> setPttPressed(bool pressed) async {
    try {
      await _method.invokeMethod<void>(
        'setPttPressed',
        {'pressed': pressed},
      );
    } on PlatformException catch (e) {
      throw Exception('Failed to set PTT: ${e.message}');
    }
  }

  Future<void> beginPttSession() async {
    try {
      await _method.invokeMethod<void>('beginPttSession');
    } on PlatformException catch (e) {
      throw Exception('Failed to begin PTT session: ${e.message}');
    }
  }

  Future<void> commitPttSession(String action) async {
    try {
      await _method.invokeMethod<void>(
        'commitPttSession',
        {'action': action},
      );
    } on PlatformException catch (e) {
      throw Exception('Failed to commit PTT session: ${e.message}');
    }
  }
}
