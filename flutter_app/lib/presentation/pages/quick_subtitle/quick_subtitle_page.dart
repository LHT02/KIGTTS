import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_dimensions.dart';
import '../../../domain/entities/quick_subtitle.dart';
import '../../../domain/repositories/realtime_repository.dart';
import '../../../domain/repositories/settings_repository.dart';
import '../../../injection.dart';
import '../../cubits/quick_subtitle/quick_subtitle_cubit.dart';
import '../../cubits/quick_subtitle/quick_subtitle_state.dart';

/// Quick subtitle page for text-to-speech shortcuts (P1).
class QuickSubtitlePage extends StatelessWidget {
  const QuickSubtitlePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => QuickSubtitleCubit(
        realtimeRepository: getIt<RealtimeRepository>(),
        settingsRepository: getIt<SettingsRepository>(),
      )..initialize(),
      child: const _QuickSubtitleView(),
    );
  }
}

class _QuickSubtitleView extends StatelessWidget {
  const _QuickSubtitleView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<QuickSubtitleCubit, QuickSubtitleState>(
      builder: (context, state) {
        final cubit = context.read<QuickSubtitleCubit>();
        final groups = state.config.groups;
        final selectedGroup =
            groups.isNotEmpty && state.selectedGroupIndex < groups.length
                ? groups[state.selectedGroupIndex]
                : null;

        return Column(
          children: [
            // Group tabs
            if (groups.isNotEmpty)
              SizedBox(
                height: 48,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppDimensions.spacingMd,
                  ),
                  itemCount: groups.length + 1,
                  itemBuilder: (_, i) {
                    if (i == groups.length) {
                      return Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        child: ActionChip(
                          label: const Icon(Icons.add, size: 18),
                          onPressed: () => _addGroup(context),
                        ),
                      );
                    }
                    final isSelected = i == state.selectedGroupIndex;
                    return Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      child: ChoiceChip(
                        label: Text(groups[i].name),
                        selected: isSelected,
                        onSelected: (_) => cubit.selectGroup(i),
                        selectedColor:
                            AppColors.primary.withValues(alpha: 0.2),
                      ),
                    );
                  },
                ),
              ),
            // Items grid
            Expanded(
              child: selectedGroup == null || selectedGroup.items.isEmpty
                  ? _EmptyState(hasGroups: groups.isNotEmpty)
                  : _ItemsGrid(
                      selectedGroup: selectedGroup,
                      fontSize: state.config.fontSize,
                      bold: state.config.bold,
                      cubit: cubit,
                    ),
            ),
            // Input bar
            _InputBar(
              onSend: (text) => cubit.sendText(text),
              onAdd: selectedGroup != null
                  ? (text) => cubit.addItem(selectedGroup.id, text)
                  : null,
            ),
          ],
        );
      },
    );
  }

  void _addGroup(BuildContext context) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('新建分组'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(hintText: '分组名称'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                context
                    .read<QuickSubtitleCubit>()
                    .addGroup(controller.text);
              }
              Navigator.pop(ctx);
            },
            child: const Text('创建'),
          ),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.hasGroups});
  final bool hasGroups;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.subtitles,
            size: 48,
            color: theme.colorScheme.outline.withValues(alpha: 0.3),
          ),
          const SizedBox(height: 8),
          Text(
            hasGroups ? '该分组暂无项目' : '点击右下角添加分组',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.outline,
            ),
          ),
        ],
      ),
    );
  }
}

class _ItemsGrid extends StatelessWidget {
  const _ItemsGrid({
    required this.selectedGroup,
    required this.fontSize,
    required this.bold,
    required this.cubit,
  });

  final QuickSubtitleGroup selectedGroup;
  final double fontSize;
  final bool bold;
  final QuickSubtitleCubit cubit;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return GridView.builder(
      padding: const EdgeInsets.all(AppDimensions.spacingMd),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        mainAxisSpacing: 8,
        crossAxisSpacing: 8,
        childAspectRatio: 2.5,
      ),
      itemCount: selectedGroup.items.length,
      itemBuilder: (_, i) {
        final item = selectedGroup.items[i];
        return Material(
          color: theme.colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(AppDimensions.radiusMedium),
          child: InkWell(
            borderRadius: BorderRadius.circular(AppDimensions.radiusMedium),
            onTap: () => cubit.sendItem(item),
            onLongPress: () =>
                cubit.removeItem(selectedGroup.id, item.id),
            child: Center(
              child: Text(
                item.text,
                style: TextStyle(
                  fontSize: fontSize.clamp(12, 48),
                  fontWeight: bold ? FontWeight.bold : FontWeight.normal,
                ),
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ),
        );
      },
    );
  }
}

class _InputBar extends StatefulWidget {
  const _InputBar({required this.onSend, this.onAdd});
  final ValueChanged<String> onSend;
  final ValueChanged<String>? onAdd;

  @override
  State<_InputBar> createState() => _InputBarState();
}

class _InputBarState extends State<_InputBar> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(AppDimensions.spacingMd),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _controller,
              decoration: const InputDecoration(
                hintText: '输入文字...',
                border: OutlineInputBorder(),
                contentPadding:
                    EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              ),
              onSubmitted: (text) {
                widget.onSend(text);
                _controller.clear();
              },
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            icon: const Icon(Icons.send),
            color: AppColors.primary,
            onPressed: () {
              widget.onSend(_controller.text);
              _controller.clear();
            },
          ),
          if (widget.onAdd != null)
            IconButton(
              icon: const Icon(Icons.add_circle_outline),
              onPressed: () {
                if (_controller.text.isNotEmpty) {
                  widget.onAdd!(_controller.text);
                  _controller.clear();
                }
              },
            ),
        ],
      ),
    );
  }
}
