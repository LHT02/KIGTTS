import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../cubits/quick_subtitle/quick_subtitle_cubit.dart';
import '../../../cubits/realtime/realtime_cubit.dart';
import '../../../cubits/realtime/realtime_state.dart';
import '../../../cubits/settings/settings_cubit.dart';
import '../../../cubits/settings/settings_state.dart';

/// Input mode for the subtitle input bar.
enum InputBarMode { keyboard, mic }

class SubtitleInputBarController {
  _SubtitleInputBarState? _state;

  void _attach(_SubtitleInputBarState state) {
    _state = state;
  }

  void _detach(_SubtitleInputBarState state) {
    if (identical(_state, state)) {
      _state = null;
    }
  }

  void moveCursorLeft() {
    _state?._moveCursor(-1);
  }

  void moveCursorRight() {
    _state?._moveCursor(1);
  }
}

/// Bottom input bar with keyboard/mic toggle on the left.
///
/// Left icon toggles keyboard ↔ mic.
/// Centre area shows either:
/// - **keyboard mode**: text field + send button
/// - **mic mode**: recording button — whose *gesture behaviour* is controlled
///   by settings (continuous listen / simple PTT / confirm PTT)
///
/// Gesture behaviour in mic mode (driven by settings):
/// | `pushToTalkMode` | `pushToTalkConfirmInput` | Behaviour |
/// |---|---|---|
/// | false | — | **Continuous listen**: tap toggles start/stop (ASR+TTS) |
/// | true  | false | **Simple PTT**: hold → record → release → auto-TTS |
/// | true  | true  | **Confirm PTT**: hold → drag to choose target → release |
class SubtitleInputBar extends StatefulWidget {
  const SubtitleInputBar({
    super.key,
    this.onPttOverlayChanged,
    this.controller,
  });

  /// Called with `true` when confirm-PTT overlay should show.
  final ValueChanged<bool>? onPttOverlayChanged;
  final SubtitleInputBarController? controller;

  @override
  State<SubtitleInputBar> createState() => _SubtitleInputBarState();
}

class _SubtitleInputBarState extends State<SubtitleInputBar> {
  late TextEditingController _controller;
  late FocusNode _inputFocusNode;
  InputBarMode _mode = InputBarMode.mic;
  Offset? _pttStartOffset;

  static const double _dragThresholdDp = 48;
  static const double _cancelRightThresholdDp = 18;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController();
    _inputFocusNode = FocusNode();
    widget.controller?._attach(this);
  }

  @override
  void didUpdateWidget(covariant SubtitleInputBar oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.controller != widget.controller) {
      oldWidget.controller?._detach(this);
      widget.controller?._attach(this);
    }
  }

  @override
  void dispose() {
    widget.controller?._detach(this);
    _inputFocusNode.dispose();
    _controller.dispose();
    super.dispose();
  }

  void _moveCursor(int delta) {
    void applyMove() {
      final text = _controller.text;
      final selection = _controller.selection;
      var cursor = selection.isValid ? selection.baseOffset : text.length;
      if (cursor < 0) {
        cursor = text.length;
      }
      final next = (cursor + delta).clamp(0, text.length);
      _controller.selection = TextSelection.collapsed(offset: next);
      _inputFocusNode.requestFocus();
    }

    if (_mode != InputBarMode.keyboard) {
      setState(() {
        _mode = InputBarMode.keyboard;
      });
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        applyMove();
      });
      return;
    }
    applyMove();
  }

  void _send(QuickSubtitleCubit cubit) {
    final text = _controller.text.trim();
    if (text.isEmpty) return;
    cubit.setDisplayText(text);
    cubit.sendText(text);
    _controller.clear();
  }

  void _toggleMode() {
    setState(() {
      _mode = _mode == InputBarMode.keyboard
          ? InputBarMode.mic
          : InputBarMode.keyboard;
    });
  }

  String _computeDragTarget(Offset delta) {
    // Confirm-input mode only has two outcomes:
    // - Drag up (or up-left) to confirm and send to subtitle
    // - Keep near origin or drag up-right to cancel
    if (delta.dy <= -_dragThresholdDp && delta.dx < _cancelRightThresholdDp) {
      return 'SendToSubtitle';
    }
    return 'Cancel';
  }

  // --- PTT pointer handlers ---

  void _onPttPointerDown(
    PointerDownEvent e,
    RealtimeCubit cubit,
    bool confirmInput,
  ) {
    _pttStartOffset = e.localPosition;
    cubit.beginPttSession();
    if (confirmInput) {
      // Confirm mode requires an explicit drag-to-confirm gesture.
      cubit.setPttDragTarget('Cancel');
      widget.onPttOverlayChanged?.call(true);
    }
  }

  void _onPttPointerMove(
    PointerMoveEvent e,
    RealtimeCubit cubit,
    bool confirmInput,
  ) {
    if (_pttStartOffset == null || !confirmInput) return;
    final delta = e.localPosition - _pttStartOffset!;
    cubit.setPttDragTarget(_computeDragTarget(delta));
  }

  void _onPttPointerUp(
    PointerUpEvent e,
    RealtimeCubit cubit,
    bool confirmInput,
  ) {
    if (_pttStartOffset == null) return;
    widget.onPttOverlayChanged?.call(false);
    if (confirmInput) {
      final delta = e.localPosition - _pttStartOffset!;
      cubit.commitPttSession(_computeDragTarget(delta));
    } else {
      cubit.commitPttSession('speak');
    }
    _pttStartOffset = null;
  }

  void _onPttPointerCancel(PointerCancelEvent e, RealtimeCubit cubit) {
    if (_pttStartOffset == null) return;
    widget.onPttOverlayChanged?.call(false);
    cubit.commitPttSession('Cancel');
    _pttStartOffset = null;
  }

  @override
  Widget build(BuildContext context) {
    final subtitleCubit = context.read<QuickSubtitleCubit>();

    return Row(
      children: [
        // Left icon: always keyboard ↔ mic toggle
        _ModeToggleButton(mode: _mode, onToggle: _toggleMode),
        const SizedBox(width: 6),
        // Centre area
        Expanded(
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 200),
            transitionBuilder: (child, anim) =>
                FadeTransition(opacity: anim, child: child),
            child: _mode == InputBarMode.keyboard
                ? _KeyboardInput(
                    key: const ValueKey('keyboard'),
                    controller: _controller,
                    focusNode: _inputFocusNode,
                    onSend: () => _send(subtitleCubit),
                    onChanged: (t) => subtitleCubit.setInputText(t),
                  )
                : _MicButton(
                    key: const ValueKey('mic'),
                    onPttPointerDown: _onPttPointerDown,
                    onPttPointerMove: _onPttPointerMove,
                    onPttPointerUp: _onPttPointerUp,
                    onPttPointerCancel: _onPttPointerCancel,
                  ),
          ),
        ),
        const SizedBox(width: 6),
        if (_mode == InputBarMode.keyboard)
          IconButton(
            icon: const Icon(Icons.send_sharp, color: AppColors.primary),
            onPressed: () => _send(subtitleCubit),
            tooltip: '发送',
            constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
          ),
        IconButton(
          icon: const Icon(Icons.brush_sharp, color: AppColors.primary),
          onPressed: () => context.push(AppRoutes.quickSubtitleDrawing),
          tooltip: '画板',
          constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// Sub-widgets
// ---------------------------------------------------------------------------

/// Left-side mode toggle button (keyboard <-> microphone icon).
class _ModeToggleButton extends StatelessWidget {
  const _ModeToggleButton({required this.mode, required this.onToggle});

  final InputBarMode mode;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    final (icon, tooltip) = mode == InputBarMode.keyboard
        ? (Icons.keyboard_sharp, '输入模式：键盘（点击切换）')
        : (Icons.mic_sharp, '输入模式：语音（点击切换）');

    return IconButton(
      icon: AnimatedSwitcher(
        duration: const Duration(milliseconds: 200),
        child: Icon(
          icon,
          key: ValueKey(mode),
          color: AppColors.primary,
          size: 24,
        ),
      ),
      onPressed: onToggle,
      tooltip: tooltip,
      constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
    );
  }
}

/// Text field input (keyboard mode).
class _KeyboardInput extends StatelessWidget {
  const _KeyboardInput({
    super.key,
    required this.controller,
    required this.focusNode,
    required this.onSend,
    required this.onChanged,
  });

  final TextEditingController controller;
  final FocusNode focusNode;
  final VoidCallback onSend;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return TextField(
      controller: controller,
      focusNode: focusNode,
      decoration: InputDecoration(
        hintText: '输入要显示/播报的内容...',
        hintStyle: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
        isDense: true,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 12,
          vertical: 10,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: theme.colorScheme.outline),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(
            color: theme.colorScheme.outline.withValues(alpha: 0.5),
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.5),
        ),
      ),
      textInputAction: TextInputAction.send,
      onSubmitted: (_) => onSend(),
      onChanged: onChanged,
    );
  }
}

/// Mic-mode recording button.
///
/// Reads settings to decide gesture behaviour:
/// - Continuous listen (`!pttMode`): tap toggles start/stop
/// - Simple PTT: hold → release → speak
/// - Confirm PTT: hold → drag → release with target
class _MicButton extends StatelessWidget {
  const _MicButton({
    super.key,
    required this.onPttPointerDown,
    required this.onPttPointerMove,
    required this.onPttPointerUp,
    required this.onPttPointerCancel,
  });

  final void Function(PointerDownEvent, RealtimeCubit, bool) onPttPointerDown;
  final void Function(PointerMoveEvent, RealtimeCubit, bool) onPttPointerMove;
  final void Function(PointerUpEvent, RealtimeCubit, bool) onPttPointerUp;
  final void Function(PointerCancelEvent, RealtimeCubit) onPttPointerCancel;

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) =>
          p.settings.pushToTalkMode != c.settings.pushToTalkMode ||
          p.settings.pushToTalkConfirmInput !=
              c.settings.pushToTalkConfirmInput,
      builder: (context, settingsState) {
        final pttMode = settingsState.settings.pushToTalkMode;
        final confirmInput = settingsState.settings.pushToTalkConfirmInput;

        if (pttMode) {
          return _PttBar(
            confirmInput: confirmInput,
            onPointerDown: onPttPointerDown,
            onPointerMove: onPttPointerMove,
            onPointerUp: onPttPointerUp,
            onPointerCancel: onPttPointerCancel,
          );
        } else {
          return const _ContinuousListenBar();
        }
      },
    );
  }
}

/// Continuous-listen bar: tap to toggle start/stop ASR+TTS.
class _ContinuousListenBar extends StatelessWidget {
  const _ContinuousListenBar();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) =>
          p.running != c.running ||
          p.ttsOnly != c.ttsOnly ||
          p.loading != c.loading ||
          p.status != c.status,
      builder: (context, state) {
        final cubit = context.read<RealtimeCubit>();
        final isRunning = state.running;
        final isTtsOnly = state.ttsOnly;

        if (state.loading) {
          return Container(
            height: 40,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.primary.withValues(alpha: 0.05),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
                const SizedBox(width: 8),
                Text(
                  state.status,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: AppColors.primary,
                  ),
                ),
              ],
            ),
          );
        }

        // Label shows current state
        final String label;
        if (isRunning && !isTtsOnly) {
          label = '停止监听';
        } else if (isRunning && isTtsOnly) {
          label = '仅TTS (点击启动ASR)';
        } else {
          label = '开始监听';
        }

        return GestureDetector(
          onTap: () {
            if (isRunning && isTtsOnly) {
              // Currently TTS-only, user wants full ASR — stop then start
              cubit.stop().then((_) => cubit.start());
            } else {
              cubit.toggle();
            }
          },
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 150),
            height: 40,
            decoration: BoxDecoration(
              color: isRunning && !isTtsOnly
                  ? Colors.red.withValues(alpha: 0.15)
                  : AppColors.primary.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: isRunning && !isTtsOnly
                    ? Colors.red.withValues(alpha: 0.5)
                    : AppColors.primary.withValues(alpha: 0.4),
                width: 1.5,
              ),
            ),
            alignment: Alignment.center,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  isRunning && !isTtsOnly
                      ? Icons.stop_sharp
                      : Icons.play_arrow_sharp,
                  color: isRunning && !isTtsOnly
                      ? Colors.red
                      : AppColors.primary,
                  size: 20,
                ),
                const SizedBox(width: 6),
                Text(
                  label,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: isRunning && !isTtsOnly
                        ? Colors.red
                        : AppColors.primary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// Push-to-talk bar with Listener for pointer events.
class _PttBar extends StatelessWidget {
  const _PttBar({
    required this.confirmInput,
    required this.onPointerDown,
    required this.onPointerMove,
    required this.onPointerUp,
    required this.onPointerCancel,
  });

  final bool confirmInput;
  final void Function(PointerDownEvent, RealtimeCubit, bool) onPointerDown;
  final void Function(PointerMoveEvent, RealtimeCubit, bool) onPointerMove;
  final void Function(PointerUpEvent, RealtimeCubit, bool) onPointerUp;
  final void Function(PointerCancelEvent, RealtimeCubit) onPointerCancel;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) =>
          p.pttPressed != c.pttPressed || p.loading != c.loading,
      builder: (context, state) {
        final cubit = context.read<RealtimeCubit>();
        final isPressed = state.pttPressed;

        return Listener(
          onPointerDown: (e) => onPointerDown(e, cubit, confirmInput),
          onPointerMove: (e) => onPointerMove(e, cubit, confirmInput),
          onPointerUp: (e) => onPointerUp(e, cubit, confirmInput),
          onPointerCancel: (e) => onPointerCancel(e, cubit),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 150),
            height: 40,
            decoration: BoxDecoration(
              color: isPressed
                  ? Colors.red.withValues(alpha: 0.15)
                  : AppColors.primary.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: isPressed
                    ? Colors.red.withValues(alpha: 0.5)
                    : AppColors.primary.withValues(alpha: 0.4),
                width: 1.5,
              ),
            ),
            alignment: Alignment.center,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  isPressed ? Icons.settings_voice_sharp : Icons.mic_sharp,
                  color: isPressed ? Colors.red : AppColors.primary,
                  size: 20,
                ),
                const SizedBox(width: 6),
                Text(
                  isPressed ? (confirmInput ? '拖动选择...' : '松开发送') : '按住说话',
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: isPressed ? Colors.red : AppColors.primary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
