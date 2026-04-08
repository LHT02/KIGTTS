import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../core/theme/app_colors.dart';
import '../cubits/realtime/realtime_cubit.dart';
import '../cubits/realtime/realtime_state.dart';

/// Global running strip showing realtime ASR/TTS status.
/// Displayed at the top of the content area across all pages.
class RunningStrip extends StatelessWidget {
  const RunningStrip({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) =>
          p.running != c.running ||
          p.inputLevel != c.inputLevel ||
          p.status != c.status,
      builder: (context, state) {
        if (!state.running) return const SizedBox.shrink();
        return Container(
          height: 36,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          decoration: BoxDecoration(
            color: AppColors.primary.withValues(alpha: 0.08),
            border: Border(
              bottom: BorderSide(
                color: AppColors.primary.withValues(alpha: 0.2),
              ),
            ),
          ),
          child: Row(
            children: [
              _MicLevelIndicator(level: state.inputLevel),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  state.status,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w500,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              _StopButton(
                onStop: () => context.read<RealtimeCubit>().stop(),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _MicLevelIndicator extends StatelessWidget {
  const _MicLevelIndicator({required this.level});
  final double level;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 60,
      height: 8,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(4),
        child: LinearProgressIndicator(
          value: level.clamp(0.0, 1.0),
          backgroundColor: AppColors.primary.withValues(alpha: 0.1),
          valueColor: AlwaysStoppedAnimation(
            level > 0.7 ? Colors.red : AppColors.primary,
          ),
        ),
      ),
    );
  }
}

class _StopButton extends StatelessWidget {
  const _StopButton({required this.onStop});
  final VoidCallback onStop;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 28,
      height: 28,
      child: IconButton(
        padding: EdgeInsets.zero,
        iconSize: 18,
        icon: Icon(Icons.stop, color: Colors.red.shade400),
        onPressed: onStop,
        tooltip: '停止',
      ),
    );
  }
}
