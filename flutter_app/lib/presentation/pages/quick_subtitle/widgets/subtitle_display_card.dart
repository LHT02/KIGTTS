import 'package:flutter/material.dart';
import '../../../../core/theme/app_dimensions.dart';

/// Large display card showing the current subtitle text.
/// Fills the expanded area above the phrase row.
class SubtitleDisplayCard extends StatelessWidget {
  const SubtitleDisplayCard({
    super.key,
    required this.displayText,
    required this.fontSize,
    required this.bold,
    required this.centered,
  });

  final String displayText;
  final double fontSize;
  final bool bold;
  final bool centered;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      elevation: AppDimensions.elevationCard,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppDimensions.radiusMedium),
      ),
      child: Padding(
        padding: const EdgeInsets.all(AppDimensions.spacingLg),
        child: Center(
          child: displayText.isEmpty
              ? Text(
                  '选择或输入要显示的内容',
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                )
              : Text(
                  displayText,
                  style: TextStyle(
                    fontSize: fontSize.clamp(12, 120),
                    fontWeight: bold ? FontWeight.bold : FontWeight.normal,
                    color: theme.colorScheme.onSurface,
                  ),
                  textAlign: centered ? TextAlign.center : TextAlign.start,
                ),
        ),
      ),
    );
  }
}
