import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../core/theme/app_colors.dart';
import '../cubits/realtime/realtime_cubit.dart';
import '../cubits/realtime/realtime_state.dart';

/// Toggle button in the top bar that expands/collapses the RunningStripPanel.
/// Shows a running indicator icon when realtime is active.
class RunningStripToggle extends StatelessWidget {
  const RunningStripToggle({
    super.key,
    required this.expanded,
    required this.onToggle,
  });

  final bool expanded;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) => p.running != c.running,
      builder: (context, state) {
        if (!state.running) return const SizedBox.shrink();
        return IconButton(
          icon: AnimatedSwitcher(
            duration: const Duration(milliseconds: 200),
            child: Icon(
              expanded
                  ? Icons.expand_less_sharp
                  : Icons.expand_more_sharp,
              key: ValueKey(expanded),
              size: 22,
              color: AppColors.primary,
            ),
          ),
          onPressed: onToggle,
          tooltip: expanded ? '收起状态面板' : '展开状态面板',
          padding: EdgeInsets.zero,
          constraints: const BoxConstraints(minWidth: 36, minHeight: 36),
        );
      },
    );
  }
}
