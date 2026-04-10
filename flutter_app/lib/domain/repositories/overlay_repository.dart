/// Repository interface for floating overlay service control.
abstract class OverlayRepository {
  /// Show the floating overlay.
  Future<void> show();

  /// Hide the floating overlay.
  Future<void> hide();

  /// Check if overlay is currently showing.
  Future<bool> isShowing();

  /// Update overlay configuration.
  Future<void> updateConfig(Map<String, dynamic> config);
}
