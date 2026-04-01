import 'package:flutter/services.dart';

import '../../core/constants/channel_names.dart';

/// Data source wrapping MethodChannel for overlay control.
class OverlayChannelDataSource {
  OverlayChannelDataSource();

  final _method = const MethodChannel(ChannelNames.overlay);

  Future<void> show() async {
    try {
      await _method.invokeMethod<void>('show');
    } on PlatformException catch (e) {
      throw Exception('Failed to show overlay: ${e.message}');
    }
  }

  Future<void> hide() async {
    try {
      await _method.invokeMethod<void>('hide');
    } on PlatformException catch (e) {
      throw Exception('Failed to hide overlay: ${e.message}');
    }
  }

  Future<bool> isShowing() async {
    try {
      return await _method.invokeMethod<bool>('isShowing') ?? false;
    } on PlatformException catch (e) {
      throw Exception('Failed to check overlay: ${e.message}');
    }
  }

  Future<void> updateConfig(Map<String, dynamic> config) async {
    try {
      await _method.invokeMethod<void>('updateConfig', config);
    } on PlatformException catch (e) {
      throw Exception('Failed to update overlay config: ${e.message}');
    }
  }
}
