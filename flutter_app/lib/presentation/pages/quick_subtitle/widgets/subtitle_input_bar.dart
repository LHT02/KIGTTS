import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../cubits/quick_subtitle/quick_subtitle_cubit.dart';
import '../../../cubits/realtime/realtime_cubit.dart';
import '../../../cubits/realtime/realtime_state.dart';

/// Input mode for the subtitle input bar.
enum InputBarMode { keyboard, ptt }

/// Bottom input bar with text field / push-to-talk toggle.
///
/// When in keyboard mode: text field + send button (left icon switches to mic).
/// When in PTT mode: "按住说话" button replaces text field (left icon
/// switches back to keyboard).
class SubtitleInputBar extends StatefulWidget {
  const SubtitleInputBar({super.key});

  @override
  State<SubtitleInputBar> createState() => _SubtitleInputBarState();
}

class _SubtitleInputBarState extends State<SubtitleInputBar> {
  late TextEditingController _controller;
  InputBarMode _mode = InputBarMode.keyboard;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
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
          ? InputBarMode.ptt
          : InputBarMode.keyboard;
    });
  }

  @override
  Widget build(BuildContext context) {
    final cubit = context.read<QuickSubtitleCubit>();

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // Main input row
        Row(
          children: [
            // Mode toggle button (keyboard <-> mic)
            _ModeToggleButton(
              mode: _mode,
              onToggle: _toggleMode,
            ),
            const SizedBox(width: 6),
            // Content area: text field OR push-to-talk button
            Expanded(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 200),
                transitionBuilder: (child, anim) {
                  return FadeTransition(opacity: anim, child: child);
                },
                child: _mode == InputBarMode.keyboard
                    ? _KeyboardInput(
                        key: const ValueKey('keyboard'),
                        controller: _controller,
                        onSend: () => _send(cubit),
                        onChanged: (t) => cubit.setInputText(t),
                      )
                    : const _PttButton(key: ValueKey('ptt')),
              ),
            ),
            const SizedBox(width: 6),
            // Send / start-stop button
            if (_mode == InputBarMode.keyboard)
              IconButton(
                icon: const Icon(
                  Icons.send_sharp,
                  color: AppColors.primary,
                ),
                onPressed: () => _send(cubit),
                tooltip: '发送',
                constraints: const BoxConstraints(
                  minWidth: 40,
                  minHeight: 40,
                ),
              )
            else
              // In PTT mode, show a toggle start/stop button
              const _StartStopButton(),
          ],
        ),
      ],
    );
  }
}

/// Left-side mode toggle button (keyboard <-> microphone icon).
class _ModeToggleButton extends StatelessWidget {
  const _ModeToggleButton({
    required this.mode,
    required this.onToggle,
  });

  final InputBarMode mode;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: AnimatedSwitcher(
        duration: const Duration(milliseconds: 200),
        child: Icon(
          mode == InputBarMode.keyboard
              ? Icons.mic_sharp
              : Icons.keyboard_sharp,
          key: ValueKey(mode),
          color: AppColors.primary,
          size: 24,
        ),
      ),
      onPressed: onToggle,
      tooltip: mode == InputBarMode.keyboard ? '按住说话' : '键盘输入',
      constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
    );
  }
}

/// Text field input (keyboard mode).
class _KeyboardInput extends StatelessWidget {
  const _KeyboardInput({
    super.key,
    required this.controller,
    required this.onSend,
    required this.onChanged,
  });

  final TextEditingController controller;
  final VoidCallback onSend;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return TextField(
      controller: controller,
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
          borderSide: BorderSide(
            color: theme.colorScheme.outline,
          ),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(
            color: theme.colorScheme.outline.withValues(alpha: 0.5),
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(
            color: AppColors.primary,
            width: 1.5,
          ),
        ),
      ),
      textInputAction: TextInputAction.send,
      onSubmitted: (_) => onSend(),
      onChanged: onChanged,
    );
  }
}

/// Push-to-talk button (replaces text field in PTT mode).
class _PttButton extends StatelessWidget {
  const _PttButton({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) =>
          p.pttPressed != c.pttPressed || p.loading != c.loading,
      builder: (context, state) {
        final cubit = context.read<RealtimeCubit>();
        final isPressed = state.pttPressed;

        return GestureDetector(
          onLongPressStart: (_) => cubit.setPttPressed(true),
          onLongPressEnd: (_) => cubit.setPttPressed(false),
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
                  isPressed ? Icons.mic_sharp : Icons.mic_none_sharp,
                  color: isPressed ? Colors.red : AppColors.primary,
                  size: 20,
                ),
                const SizedBox(width: 6),
                Text(
                  isPressed ? '松开发送' : '按住说话',
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

/// Small start/stop toggle shown in PTT mode.
class _StartStopButton extends StatelessWidget {
  const _StartStopButton();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) =>
          p.running != c.running || p.loading != c.loading,
      builder: (context, state) {
        final cubit = context.read<RealtimeCubit>();
        if (state.loading) {
          return const SizedBox(
            width: 40,
            height: 40,
            child: Padding(
              padding: EdgeInsets.all(10),
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
          );
        }
        return IconButton(
          icon: Icon(
            state.running ? Icons.stop_sharp : Icons.play_arrow_sharp,
            color: state.running ? Colors.red : AppColors.primary,
          ),
          onPressed: () => cubit.toggle(),
          tooltip: state.running ? '停止' : '启动',
          constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
        );
      },
    );
  }
}
