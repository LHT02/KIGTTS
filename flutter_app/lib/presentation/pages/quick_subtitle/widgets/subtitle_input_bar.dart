import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../cubits/quick_subtitle/quick_subtitle_cubit.dart';


/// Bottom input bar with text field and send button.
class SubtitleInputBar extends StatefulWidget {
  const SubtitleInputBar({super.key});

  @override
  State<SubtitleInputBar> createState() => _SubtitleInputBarState();
}

class _SubtitleInputBarState extends State<SubtitleInputBar> {
  late TextEditingController _controller;

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

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cubit = context.read<QuickSubtitleCubit>();

    return Row(
      children: [
        Expanded(
          child: TextField(
            controller: _controller,
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
                borderRadius: BorderRadius.circular(8),
                borderSide: BorderSide(
                  color: theme.colorScheme.outline,
                ),
              ),
            ),
            textInputAction: TextInputAction.send,
            onSubmitted: (_) => _send(cubit),
            onChanged: (text) => cubit.setInputText(text),
          ),
        ),
        const SizedBox(width: 8),
        IconButton(
          icon: const Icon(
            Icons.send_sharp,
            color: AppColors.primary,
          ),
          onPressed: () => _send(cubit),
          tooltip: '发送',
        ),
      ],
    );
  }
}
