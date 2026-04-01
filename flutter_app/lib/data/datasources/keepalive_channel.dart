import 'package:flutter/services.dart';

import '../../core/constants/channel_names.dart';

/// Data source wrapping MethodChannel for keep-alive service.
class KeepaliveChannelDataSource {
  KeepaliveChannelDataSource();

  final _method = const MethodChannel(ChannelNames.keepalive);

  Future<void> start() async {
    try {
      await _method.invokeMethod<void>('start');
    } on PlatformException catch (e) {
      throw Exception('Failed to start keepalive: ${e.message}');
    }
  }

  Future<void> stop() async {
    try {
      await _method.invokeMethod<void>('stop');
    } on PlatformException catch (e) {
      throw Exception('Failed to stop keepalive: ${e.message}');
    }
  }
}
