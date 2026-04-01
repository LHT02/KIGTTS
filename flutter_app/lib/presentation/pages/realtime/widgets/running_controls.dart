import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../cubits/realtime/realtime_cubit.dart';
import '../../../cubits/realtime/realtime_state.dart';

/// Bottom control strip with mic level, playback progress, and start/stop.
class RunningControls extends StatelessWidget {
  const RunningControls({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      height: AppDimensions.runningStripHeight + 16,
      padding: const EdgeInsets.symmetric(
        horizontal: AppDimensions.spacingLg,
        vertical: AppDimensions.spacingSm,
      ),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        border: Border(
          top: BorderSide(
            color: theme.dividerTheme.color ??
                theme.colorScheme.outline.withValues(alpha: 0.2),
          ),
        ),
      ),
      child: Row(
        children: [
          // Mic level indicator
          Expanded(
            child: BlocBuilder<RealtimeCubit, RealtimeState>(
              buildWhen: (p, c) => p.inputLevel != c.inputLevel,
              builder: (context, state) {
                return _LevelBar(
                  value: state.inputLevel,
                  label: '输入',
                  color: AppColors.primary,
                );
              },
            ),
          ),
          const SizedBox(width: AppDimensions.spacingMd),
          // Playback progress
          Expanded(
            child: BlocBuilder<RealtimeCubit, RealtimeState>(
              buildWhen: (p, c) =>
                  p.playbackProgress != c.playbackProgress,
              builder: (context, state) {
                return _LevelBar(
                  value: state.playbackProgress,
                  label: '播放',
                  color: AppColors.secondary,
                );
              },
            ),
          ),
          const SizedBox(width: AppDimensions.spacingLg),
          // Start/Stop button
          BlocBuilder<RealtimeCubit, RealtimeState>(
            buildWhen: (p, c) =>
                p.running != c.running || p.loading != c.loading,
            builder: (context, state) {
              return _ControlButton(
                running: state.running,
                loading: state.loading,
                onPressed: () => context.read<RealtimeCubit>().toggle(),
              );
            },
          ),
        ],
      ),
    );
  }
}

class _LevelBar extends StatelessWidget {
  const _LevelBar({
    required this.value,
    required this.label,
    required this.color,
  });
  final double value;
  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 2),
        ClipRRect(
          borderRadius: BorderRadius.circular(2),
          child: LinearProgressIndicator(
            value: value.clamp(0.0, 1.0),
            minHeight: 6,
            backgroundColor:
                theme.colorScheme.outline.withValues(alpha: 0.2),
            valueColor: AlwaysStoppedAnimation(color),
          ),
        ),
      ],
    );
  }
}

class _ControlButton extends StatelessWidget {
  const _ControlButton({
    required this.running,
    required this.loading,
    required this.onPressed,
  });
  final bool running;
  final bool loading;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const SizedBox(
        width: 48,
        height: 48,
        child: Center(child: CircularProgressIndicator()),
      );
    }
    return SizedBox(
      width: 48,
      height: 48,
      child: FloatingActionButton(
        heroTag: 'realtimeControl',
        mini: true,
        onPressed: onPressed,
        backgroundColor:
            running ? AppColors.darkError : AppColors.primary,
        child: Icon(
          running ? Icons.stop : Icons.mic,
          color: Colors.white,
        ),
      ),
    );
  }
}
