/// Base failure class for error propagation through layers.
sealed class Failure {
  const Failure(this.message);
  final String message;

  @override
  String toString() => '$runtimeType: $message';
}

/// Failure from platform channel calls.
class PlatformFailure extends Failure {
  const PlatformFailure(super.message);
}

/// Failure from local storage operations.
class StorageFailure extends Failure {
  const StorageFailure(super.message);
}

/// Failure from file operations (import/export).
class FileFailure extends Failure {
  const FileFailure(super.message);
}
