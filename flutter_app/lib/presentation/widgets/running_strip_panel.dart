import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_dimensions.dart';
import '../cubits/realtime/realtime_cubit.dart';
import '../cubits/realtime/realtime_state.dart';

/// Collapsible panel showing realtime status info (recognized text, levels).
/// Displayed at the top of the main content area on the home page.
class RunningStripPanel extends StatelessWidget {
  const RunningStripPanel({
    super.key,
    required this.onCollapse,
  });

  final VoidCallback onCollapse;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      builder: (context, state) {
        return Container(
          width: double.infinity,
          constraints: const BoxConstraints(
            minHeight: AppDimensions.runningStripHeight,
          ),
          decoration: BoxDecoration(
            color: theme.colorScheme.surfaceContainerHighest,
            border: Border(
              bottom: BorderSide(
                color: theme.colorScheme.outline.withValues(alpha: 0.2),
              ),
            ),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppDimensions.spacingMd,
              vertical: AppDimensions.spacingXs,
            ),
            child: Row(
              children: [
                // Status icon
                Icon(
                  state.running
                      ? Icons.graphic_eq_sharp
                      : Icons.stop_circle_outlined,
                  size: 20,
                  color: state.running
                      ? AppColors.primary
                      : theme.colorScheme.outline,
                ),
                const SizedBox(width: 8),
                // Status text
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        state.status,
                        style: theme.textTheme.labelMedium?.copyWith(
                          color: state.running
                              ? AppColors.primary
                              : theme.colorScheme.onSurfaceVariant,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      if (state.running && state.recognized.isNotEmpty)
                        Text(
                          state.recognized.last.text,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                    ],
                  ),
                ),
                // Input level indicator
                if (state.running) ...[
                  SizedBox(
                    width: 48,
                    height: 4,
                    child: LinearProgressIndicator(
                      value: state.inputLevel.clamp(0.0, 1.0),
                      backgroundColor:
                          theme.colorScheme.surfaceContainerHighest,
                      valueColor: const AlwaysStoppedAnimation(
                        AppColors.primary,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                ],
                // Collapse button
                IconButton(
                  icon: const Icon(Icons.expand_less_sharp, size: 20),
                  onPressed: onCollapse,
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(
                    minWidth: 32,
                    minHeight: 32,
                  ),
                  tooltip: '收起',
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
