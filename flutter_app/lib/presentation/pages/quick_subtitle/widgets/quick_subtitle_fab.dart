import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../cubits/realtime/realtime_cubit.dart';
import '../../../cubits/realtime/realtime_state.dart';
import '../../../cubits/settings/settings_cubit.dart';
import '../../../cubits/settings/settings_state.dart';

/// Floating action button for the quick-subtitle page.
///
/// Behaviour depends on two settings:
/// - **Continuous listening** (`!pttMode`): tap toggles start/stop.
/// - **Simple PTT** (`pttMode && !confirmInput`): hold → record → release → TTS.
/// - **Confirm PTT** (`pttMode && confirmInput`): hold → record → drag to
///   choose target (SendToSubtitle / SendToInput / Cancel) → release.
class QuickSubtitleMicFab extends StatefulWidget {
  const QuickSubtitleMicFab({super.key, this.onPttOverlayChanged});

  /// Called with `true` when confirm-PTT overlay should show, `false` to hide.
  final ValueChanged<bool>? onPttOverlayChanged;

  @override
  State<QuickSubtitleMicFab> createState() => _QuickSubtitleMicFabState();
}

class _QuickSubtitleMicFabState extends State<QuickSubtitleMicFab> {
  Offset? _startOffset;

  // --- Drag-zone logic (confirm PTT) ---

  static const double _dragThresholdDp = 48;
  static const double _cancelRightThresholdDp = 18;

  String _computeDragTarget(Offset delta) {
    if (delta.dy <= -_dragThresholdDp && delta.dx < _cancelRightThresholdDp) {
      return 'SendToSubtitle';
    }
    return 'Cancel';
  }

  // --- Gesture callbacks ---

  void _onPointerDown(
    PointerDownEvent e,
    RealtimeCubit cubit,
    bool ptt,
    bool confirmInput,
  ) {
    if (!ptt) return; // continuous-listen: handled by onTap
    _startOffset = e.localPosition;
    cubit.beginPttSession();
    if (confirmInput) {
      cubit.setPttDragTarget('Cancel');
      widget.onPttOverlayChanged?.call(true);
    }
  }

  void _onPointerMove(
    PointerMoveEvent e,
    RealtimeCubit cubit,
    bool confirmInput,
  ) {
    if (_startOffset == null) return;
    if (!confirmInput) return;
    final delta = e.localPosition - _startOffset!;
    final target = _computeDragTarget(delta);
    cubit.setPttDragTarget(target);
  }

  void _onPointerUp(
    PointerUpEvent e,
    RealtimeCubit cubit,
    bool ptt,
    bool confirmInput,
  ) {
    if (!ptt || _startOffset == null) return;
    widget.onPttOverlayChanged?.call(false);

    if (confirmInput) {
      final delta = e.localPosition - _startOffset!;
      final target = _computeDragTarget(delta);
      cubit.commitPttSession(target);
    } else {
      // simple PTT → always speak
      cubit.commitPttSession('speak');
    }
    _startOffset = null;
  }

  void _onPointerCancel(PointerCancelEvent e, RealtimeCubit cubit, bool ptt) {
    if (!ptt || _startOffset == null) return;
    widget.onPttOverlayChanged?.call(false);
    cubit.commitPttSession('Cancel');
    _startOffset = null;
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) =>
          p.settings.pushToTalkMode != c.settings.pushToTalkMode ||
          p.settings.pushToTalkConfirmInput !=
              c.settings.pushToTalkConfirmInput,
      builder: (context, settingsState) {
        final ptt = settingsState.settings.pushToTalkMode;
        final confirmInput = settingsState.settings.pushToTalkConfirmInput;

        return BlocBuilder<RealtimeCubit, RealtimeState>(
          buildWhen: (p, c) =>
              p.running != c.running ||
              p.pttPressed != c.pttPressed ||
              p.loading != c.loading,
          builder: (context, rtState) {
            final cubit = context.read<RealtimeCubit>();
            final isPressed = rtState.pttPressed;
            final isRunning = rtState.running;

            return Listener(
              onPointerDown: (e) => _onPointerDown(e, cubit, ptt, confirmInput),
              onPointerMove: (e) => _onPointerMove(e, cubit, confirmInput),
              onPointerUp: (e) => _onPointerUp(e, cubit, ptt, confirmInput),
              onPointerCancel: (e) => _onPointerCancel(e, cubit, ptt),
              child: _FabBody(
                ptt: ptt,
                isPressed: isPressed,
                isRunning: isRunning,
                isLoading: rtState.loading,
                onTap: ptt ? null : () => cubit.toggle(),
              ),
            );
          },
        );
      },
    );
  }
}

/// Visual FAB body with animated icon cross-fade.
class _FabBody extends StatelessWidget {
  const _FabBody({
    required this.ptt,
    required this.isPressed,
    required this.isRunning,
    required this.isLoading,
    required this.onTap,
  });

  final bool ptt;
  final bool isPressed;
  final bool isRunning;
  final bool isLoading;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const FloatingActionButton(
        heroTag: 'quickSubtitleFab',
        onPressed: null,
        backgroundColor: AppColors.darkSurfaceVariant,
        child: SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      );
    }

    // Icon selection
    final Widget icon;
    if (ptt) {
      // Cross-fade icon based on pressed state
      icon = AnimatedSwitcher(
        duration: const Duration(milliseconds: 180),
        child: Icon(
          isPressed ? Icons.settings_voice_sharp : Icons.mic_sharp,
          key: ValueKey(isPressed),
          color: Colors.white,
        ),
      );
    } else {
      icon = AnimatedSwitcher(
        duration: const Duration(milliseconds: 180),
        child: Icon(
          isRunning ? Icons.stop_sharp : Icons.play_arrow_sharp,
          key: ValueKey(isRunning),
          color: Colors.white,
        ),
      );
    }

    final bgColor = isPressed
        ? Colors.red
        : isRunning
        ? AppColors.darkError
        : AppColors.primary;

    return FloatingActionButton(
      heroTag: 'quickSubtitleFab',
      onPressed: onTap,
      backgroundColor: bgColor,
      elevation: 6,
      child: icon,
    );
  }
}
