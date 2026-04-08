import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../cubits/realtime/realtime_cubit.dart';
import '../../../cubits/realtime/realtime_state.dart';

/// Status bar showing current state, device info, and AEC status.
class StatusBar extends StatelessWidget {
  const StatusBar({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) =>
          p.status != c.status ||
          p.inputDeviceLabel != c.inputDeviceLabel ||
          p.outputDeviceLabel != c.outputDeviceLabel ||
          p.aec3Status != c.aec3Status ||
          p.error != c.error,
      builder: (context, state) {
        return Container(
          padding: const EdgeInsets.symmetric(
            horizontal: AppDimensions.spacingLg,
            vertical: AppDimensions.spacingSm,
          ),
          decoration: BoxDecoration(
            color: theme.colorScheme.surface,
            border: Border(
              bottom: BorderSide(
                color: theme.dividerTheme.color ??
                    theme.colorScheme.outline.withValues(alpha: 0.2),
              ),
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _StatusChip(
                    label: state.status,
                    color: state.running
                        ? AppColors.success
                        : theme.colorScheme.outline,
                  ),
                  const SizedBox(width: AppDimensions.spacingSm),
                  if (state.running) ...[
                    _InfoChip(
                      icon: Icons.mic,
                      label: state.inputDeviceLabel,
                    ),
                    const SizedBox(width: AppDimensions.spacingSm),
                    _InfoChip(
                      icon: Icons.volume_up,
                      label: state.outputDeviceLabel,
                    ),
                  ],
                ],
              ),
              if (state.error != null)
                Padding(
                  padding:
                      const EdgeInsets.only(top: AppDimensions.spacingXs),
                  child: Row(
                    children: [
                      Icon(
                        Icons.error_outline,
                        size: 14,
                        color: theme.colorScheme.error,
                      ),
                      const SizedBox(width: 4),
                      Expanded(
                        child: Text(
                          state.error!,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.error,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      InkWell(
                        onTap: () =>
                            context.read<RealtimeCubit>().clearError(),
                        child: Icon(
                          Icons.close,
                          size: 14,
                          color: theme.colorScheme.error,
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.label, required this.color});
  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: color,
              fontWeight: FontWeight.w600,
            ),
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({required this.icon, required this.label});
  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 12, color: theme.colorScheme.onSurfaceVariant),
        const SizedBox(width: 2),
        Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}
