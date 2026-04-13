import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../../core/router/app_router.dart';
import '../../../cubits/quick_subtitle/quick_subtitle_cubit.dart';

/// Large display card showing the current subtitle text.
///
/// Features:
/// - Fills the expanded area above the phrase row
/// - Long-press to copy text to clipboard
/// - Bottom format toolbar: bold / center / clear / history
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

    final cubit = context.read<QuickSubtitleCubit>();

    return GestureDetector(
      onLongPress: displayText.isNotEmpty
          ? () {
              Clipboard.setData(ClipboardData(text: displayText));
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('已复制到剪贴板'),
                  duration: Duration(seconds: 1),
                ),
              );
            }
          : null,
      child: Card(
        elevation: AppDimensions.elevationCard,
        shape: RoundedRectangleBorder(
          borderRadius:
              BorderRadius.circular(AppDimensions.radiusMedium),
        ),
        child: Column(
          children: [
            // Main text area (scrollable for long text, centered for short)
            Expanded(
              child: Stack(
                children: [
                  LayoutBuilder(
                    builder: (context, constraints) {
                      final minHeight =
                          (constraints.maxHeight - AppDimensions.spacingLg * 2)
                              .clamp(0.0, double.infinity)
                              .toDouble();
                      final textWidget = displayText.isEmpty
                          ? Text(
                              '选择或输入要显示的内容',
                              style: theme.textTheme.bodyMedium?.copyWith(
                                color: theme.colorScheme.onSurfaceVariant,
                              ),
                              textAlign: TextAlign.center,
                            )
                          : Text(
                              displayText,
                              style: TextStyle(
                                fontSize: fontSize.clamp(12, 120),
                                fontWeight: bold
                                    ? FontWeight.bold
                                    : FontWeight.normal,
                                color: theme.colorScheme.onSurface,
                              ),
                              textAlign: centered
                                  ? TextAlign.center
                                  : TextAlign.start,
                            );

                      return SingleChildScrollView(
                        padding: const EdgeInsets.fromLTRB(
                          AppDimensions.spacingLg,
                          AppDimensions.spacingLg,
                          AppDimensions.spacingLg,
                          AppDimensions.spacingLg,
                        ),
                        child: ConstrainedBox(
                          constraints: BoxConstraints(
                            minHeight: minHeight,
                          ),
                          child: Center(child: textWidget),
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(10, 0, 10, 2),
              child: Row(
                children: [
                  const Icon(Icons.text_fields_sharp, size: 18),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Slider(
                      value: fontSize.clamp(28, 96),
                      min: 28,
                      max: 96,
                      divisions: 68,
                      label: '${fontSize.round()}sp',
                      onChanged: (value) => cubit.setFontSize(value),
                    ),
                  ),
                  SizedBox(
                    width: 54,
                    child: Text(
                      '${fontSize.round()}sp',
                      textAlign: TextAlign.end,
                      style: theme.textTheme.bodySmall,
                    ),
                  ),
                ],
              ),
            ),
            // Format toolbar
            _FormatToolbar(bold: bold, centered: centered),
          ],
        ),
      ),
    );
  }
}

/// Bottom toolbar row: bold / center / clear / history.
class _FormatToolbar extends StatelessWidget {
  const _FormatToolbar({required this.bold, required this.centered});

  final bool bold;
  final bool centered;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cubit = context.read<QuickSubtitleCubit>();
    final iconColor = theme.colorScheme.onSurfaceVariant;

    return Container(
      height: 36,
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(
            color: theme.colorScheme.outline.withValues(alpha: 0.15),
          ),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _ToolbarButton(
            icon: Icons.format_bold_sharp,
            tooltip: '粗体',
            active: bold,
            activeColor: AppColors.primary,
            inactiveColor: iconColor,
            onPressed: () => cubit.toggleBold(),
          ),
          _ToolbarButton(
            icon: Icons.format_align_center_sharp,
            tooltip: '居中',
            active: centered,
            activeColor: AppColors.primary,
            inactiveColor: iconColor,
            onPressed: () => cubit.toggleCentered(),
          ),
          _ToolbarButton(
            icon: Icons.cleaning_services_sharp,
            tooltip: '清屏',
            active: false,
            activeColor: AppColors.primary,
            inactiveColor: iconColor,
            onPressed: () => cubit.clearDisplay(),
          ),
          _ToolbarButton(
            icon: Icons.history_sharp,
            tooltip: '历史记录',
            active: false,
            activeColor: AppColors.primary,
            inactiveColor: iconColor,
            onPressed: () async {
              final selected = await context.push<String>(
                AppRoutes.quickSubtitleHistory,
              );
              if (!context.mounted || selected == null || selected.isEmpty) {
                return;
              }
              cubit.setDisplayText(selected);
            },
          ),
        ],
      ),
    );
  }
}

class _ToolbarButton extends StatelessWidget {
  const _ToolbarButton({
    required this.icon,
    required this.tooltip,
    required this.active,
    required this.activeColor,
    required this.inactiveColor,
    required this.onPressed,
  });

  final IconData icon;
  final String tooltip;
  final bool active;
  final Color activeColor;
  final Color inactiveColor;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: Icon(
        icon,
        size: 18,
        color: active ? activeColor : inactiveColor,
      ),
      onPressed: onPressed,
      tooltip: tooltip,
      padding: EdgeInsets.zero,
      constraints: const BoxConstraints(minWidth: 36, minHeight: 36),
    );
  }
}
