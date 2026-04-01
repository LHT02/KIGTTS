import '../../domain/repositories/keepalive_repository.dart';
import '../datasources/keepalive_channel.dart';

/// Implementation of KeepaliveRepository using MethodChannel.
class KeepaliveRepositoryImpl implements KeepaliveRepository {
  KeepaliveRepositoryImpl({required this.dataSource});

  final KeepaliveChannelDataSource dataSource;

  @override
  Future<void> start() => dataSource.start();

  @override
  Future<void> stop() => dataSource.stop();
}
