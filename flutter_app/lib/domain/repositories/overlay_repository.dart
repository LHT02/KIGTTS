/// Repository interface for floating overlay service control.
abstract class OverlayRepository {
  /// Check whether SYSTEM_ALERT_WINDOW permission is granted.
  Future<bool> hasPermission();

  /// Open system overlay permission settings page.
  Future<void> openPermissionSettings();

  /// Show the floating overlay.
  Future<void> show();

  /// Hide the floating overlay.
  Future<void> hide();

  /// Check if overlay is currently showing.
  Future<bool> isShowing();

  /// Update overlay configuration.
  Future<void> updateConfig(Map<String, dynamic> config);
}
