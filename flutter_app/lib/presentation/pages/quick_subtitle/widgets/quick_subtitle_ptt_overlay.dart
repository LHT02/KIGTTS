import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../cubits/realtime/realtime_cubit.dart';
import '../../../cubits/realtime/realtime_state.dart';

/// Overlay displayed during confirm-PTT hold gesture.
///
/// Shows:
/// - Streaming recognition text at the top
/// - Two target indicators (SendToInput / Cancel) that highlight
///   based on the current drag direction.
class QuickSubtitlePttOverlay extends StatelessWidget {
  const QuickSubtitlePttOverlay({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) =>
          p.pttStreamingText != c.pttStreamingText ||
          p.pttDragTarget != c.pttDragTarget,
      builder: (context, state) {
        final target = state.pttDragTarget;
        return Container(
          color: Colors.black.withValues(alpha: 0.6),
          child: Column(
            children: [
              // Streaming text preview
              Expanded(
                child: Center(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 32),
                    child: Text(
                      state.pttStreamingText.isEmpty
                          ? '正在聆听...'
                          : state.pttStreamingText,
                      style: theme.textTheme.headlineSmall?.copyWith(
                        color: Colors.white,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
              ),
              // Drag target indicators
              Padding(
                padding: const EdgeInsets.only(bottom: 120),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    _TargetIndicator(
                      icon: Icons.keyboard_return_sharp,
                      label: '输入框',
                      active: target == 'SendToInput',
                    ),
                    _TargetIndicator(
                      icon: Icons.subtitles_sharp,
                      label: '字幕',
                      active: target == 'SendToSubtitle',
                    ),
                    _TargetIndicator(
                      icon: Icons.close_sharp,
                      label: '取消',
                      active: target == 'Cancel',
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

class _TargetIndicator extends StatelessWidget {
  const _TargetIndicator({
    required this.icon,
    required this.label,
    required this.active,
  });

  final IconData icon;
  final String label;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final color = active ? AppColors.primary : Colors.white54;
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: active
                ? AppColors.primary.withValues(alpha: 0.25)
                : Colors.white.withValues(alpha: 0.1),
            border: Border.all(color: color, width: active ? 2 : 1),
          ),
          child: Icon(icon, color: color, size: 28),
        ),
        const SizedBox(height: 6),
        Text(
          label,
          style: TextStyle(color: color, fontSize: 12),
        ),
      ],
    );
  }
}
