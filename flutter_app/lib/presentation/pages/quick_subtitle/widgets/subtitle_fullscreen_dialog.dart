import 'package:flutter/material.dart';

/// Fullscreen dialog showing the current subtitle text.
///
/// Launched from the FullscreenAction top-bar button. Displays the text
/// centered on a black background with large font, suitable for showing
/// to an audience.
class SubtitleFullscreenDialog extends StatelessWidget {
  const SubtitleFullscreenDialog({
    super.key,
    required this.text,
    this.bold = false,
    this.centered = true,
    this.fontSize = 48,
  });

  final String text;
  final bool bold;
  final bool centered;
  final double fontSize;

  /// Show the fullscreen subtitle dialog.
  static Future<void> show(
    BuildContext context, {
    required String text,
    bool bold = false,
    bool centered = true,
    double fontSize = 48,
  }) {
    return showDialog(
      context: context,
      barrierDismissible: true,
      useSafeArea: false,
      builder: (_) => SubtitleFullscreenDialog(
        text: text,
        bold: bold,
        centered: centered,
        fontSize: fontSize,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => Navigator.of(context).pop(),
      child: Scaffold(
        backgroundColor: Colors.black,
        body: SafeArea(
          child: Center(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: text.isEmpty
                  ? Text(
                      '暂无内容',
                      style: TextStyle(
                        color: Colors.white38,
                        fontSize: fontSize.clamp(12, 120),
                      ),
                    )
                  : Text(
                      text,
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: fontSize.clamp(12, 120),
                        fontWeight:
                            bold ? FontWeight.bold : FontWeight.normal,
                      ),
                      textAlign:
                          centered ? TextAlign.center : TextAlign.start,
                    ),
            ),
          ),
        ),
      ),
    );
  }
}
