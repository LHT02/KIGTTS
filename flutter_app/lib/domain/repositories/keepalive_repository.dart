/// Repository interface for foreground keep-alive service.
abstract class KeepaliveRepository {
  /// Start the keep-alive foreground service.
  Future<void> start();

  /// Stop the keep-alive foreground service.
  Future<void> stop();
}
