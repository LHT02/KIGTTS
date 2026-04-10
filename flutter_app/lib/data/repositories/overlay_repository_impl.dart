import '../../domain/repositories/overlay_repository.dart';
import '../datasources/overlay_channel.dart';

/// Implementation of OverlayRepository using MethodChannel.
class OverlayRepositoryImpl implements OverlayRepository {
  OverlayRepositoryImpl({required this.dataSource});

  final OverlayChannelDataSource dataSource;

  @override
  Future<void> show() => dataSource.show();

  @override
  Future<void> hide() => dataSource.hide();

  @override
  Future<bool> isShowing() => dataSource.isShowing();

  @override
  Future<void> updateConfig(Map<String, dynamic> config) {
    return dataSource.updateConfig(config);
  }
}
