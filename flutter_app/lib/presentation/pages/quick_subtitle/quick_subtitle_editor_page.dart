import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../domain/entities/quick_subtitle.dart';
import '../../../domain/repositories/realtime_repository.dart';
import '../../../domain/repositories/settings_repository.dart';
import '../../../injection.dart';
import '../../cubits/quick_subtitle/quick_subtitle_cubit.dart';
import '../../cubits/quick_subtitle/quick_subtitle_state.dart';

class QuickSubtitleEditorPage extends StatelessWidget {
  const QuickSubtitleEditorPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => QuickSubtitleCubit(
        realtimeRepository: getIt<RealtimeRepository>(),
        settingsRepository: getIt<SettingsRepository>(),
      )..initialize(),
      child: const _QuickSubtitleEditorView(),
    );
  }
}

class _QuickSubtitleEditorView extends StatelessWidget {
  const _QuickSubtitleEditorView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<QuickSubtitleCubit, QuickSubtitleState>(
      builder: (context, state) {
        if (state.loading) {
          return const Center(child: CircularProgressIndicator());
        }

        final groups = state.config.groups;
        if (groups.isEmpty) {
          return const Center(child: Text('暂无分组'));
        }

        final selectedGroup =
            groups[state.selectedGroupIndex.clamp(0, groups.length - 1)];

        return ListView(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 16),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.category_sharp, size: 20),
                        const SizedBox(width: 8),
                        Text(
                          '分组管理',
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const Spacer(),
                        FilledButton.tonalIcon(
                          onPressed: () => _showGroupDialog(context),
                          icon: const Icon(Icons.add_sharp, size: 18),
                          label: const Text('新增分组'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    ReorderableListView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: groups.length,
                      onReorder: (oldIndex, newIndex) {
                        context.read<QuickSubtitleCubit>().reorderGroups(
                          oldIndex,
                          newIndex,
                        );
                      },
                      itemBuilder: (context, index) {
                        final group = groups[index];
                        final selected = index == state.selectedGroupIndex;
                        return ListTile(
                          key: ValueKey(group.id),
                          contentPadding: EdgeInsets.zero,
                          selected: selected,
                          leading: const Icon(
                            Icons.folder_open_sharp,
                            size: 20,
                          ),
                          title: Text(group.name),
                          subtitle: Text(
                            '图标: ${group.icon.isEmpty ? '未设置' : group.icon}',
                          ),
                          onTap: () => context
                              .read<QuickSubtitleCubit>()
                              .selectGroup(index),
                          trailing: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              IconButton(
                                tooltip: '编辑分组',
                                onPressed: () =>
                                    _showGroupDialog(context, group: group),
                                icon: const Icon(Icons.edit_sharp, size: 18),
                              ),
                              IconButton(
                                tooltip: '删除分组',
                                onPressed: groups.length <= 1
                                    ? null
                                    : () => context
                                          .read<QuickSubtitleCubit>()
                                          .removeGroup(group.id),
                                icon: const Icon(
                                  Icons.delete_outline_sharp,
                                  size: 18,
                                ),
                              ),
                              ReorderableDragStartListener(
                                index: index,
                                child: const Padding(
                                  padding: EdgeInsets.symmetric(horizontal: 8),
                                  child: Icon(
                                    Icons.drag_indicator_sharp,
                                    size: 20,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 10),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.short_text_sharp, size: 20),
                        const SizedBox(width: 8),
                        Text(
                          '短语管理 (${selectedGroup.name})',
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const Spacer(),
                        FilledButton.tonalIcon(
                          onPressed: () =>
                              _showItemDialog(context, selectedGroup),
                          icon: const Icon(Icons.add_sharp, size: 18),
                          label: const Text('新增短语'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    if (selectedGroup.items.isEmpty)
                      Text(
                        '当前分组暂无短语',
                        style: Theme.of(context).textTheme.bodySmall,
                      )
                    else
                      ReorderableListView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        itemCount: selectedGroup.items.length,
                        onReorder: (oldIndex, newIndex) {
                          context.read<QuickSubtitleCubit>().reorderItems(
                            selectedGroup.id,
                            oldIndex,
                            newIndex,
                          );
                        },
                        itemBuilder: (context, index) {
                          final item = selectedGroup.items[index];
                          return ListTile(
                            key: ValueKey(item.id),
                            contentPadding: EdgeInsets.zero,
                            title: Text(
                              item.text,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                            ),
                            onTap: () {
                              final cubit = context.read<QuickSubtitleCubit>();
                              cubit.selectGroup(state.selectedGroupIndex);
                              cubit.selectItem(index);
                            },
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                IconButton(
                                  tooltip: '编辑短语',
                                  onPressed: () => _showItemDialog(
                                    context,
                                    selectedGroup,
                                    item: item,
                                  ),
                                  icon: const Icon(
                                    Icons.edit_note_sharp,
                                    size: 18,
                                  ),
                                ),
                                IconButton(
                                  tooltip: '删除短语',
                                  onPressed: () => context
                                      .read<QuickSubtitleCubit>()
                                      .removeItem(selectedGroup.id, item.id),
                                  icon: const Icon(
                                    Icons.delete_outline_sharp,
                                    size: 18,
                                  ),
                                ),
                                ReorderableDragStartListener(
                                  index: index,
                                  child: const Padding(
                                    padding: EdgeInsets.symmetric(
                                      horizontal: 8,
                                    ),
                                    child: Icon(
                                      Icons.drag_indicator_sharp,
                                      size: 20,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          );
                        },
                      ),
                  ],
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  Future<void> _showGroupDialog(
    BuildContext context, {
    QuickSubtitleGroup? group,
  }) async {
    final nameController = TextEditingController(text: group?.name ?? '');
    var iconValue = group?.icon.isNotEmpty == true
        ? group!.icon
        : 'emoji_emotions';

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setState) {
            return AlertDialog(
              title: Text(group == null ? '新增分组' : '编辑分组'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: nameController,
                    decoration: const InputDecoration(labelText: '分组名称'),
                    autofocus: true,
                  ),
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: iconValue,
                    decoration: const InputDecoration(labelText: '分组图标'),
                    items:
                        const [
                              'emoji_emotions',
                              'sports_esports',
                              'work',
                              'school',
                              'family_restroom',
                              'restaurant',
                              'home',
                              'favorite',
                            ]
                            .map(
                              (e) => DropdownMenuItem<String>(
                                value: e,
                                child: Text(e),
                              ),
                            )
                            .toList(),
                    onChanged: (value) {
                      if (value == null) return;
                      setState(() => iconValue = value);
                    },
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: const Text('取消'),
                ),
                FilledButton(
                  onPressed: () async {
                    final cubit = context.read<QuickSubtitleCubit>();
                    if (group == null) {
                      await cubit.addGroup(
                        nameController.text,
                        icon: iconValue,
                      );
                    } else {
                      await cubit.updateGroup(
                        group.id,
                        name: nameController.text,
                        icon: iconValue,
                      );
                    }
                    if (!dialogContext.mounted) return;
                    Navigator.of(dialogContext).pop();
                  },
                  child: const Text('保存'),
                ),
              ],
            );
          },
        );
      },
    );

    nameController.dispose();
  }

  Future<void> _showItemDialog(
    BuildContext context,
    QuickSubtitleGroup group, {
    QuickSubtitleItem? item,
  }) async {
    final controller = TextEditingController(text: item?.text ?? '');

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: Text(item == null ? '新增短语' : '编辑短语'),
          content: TextField(
            controller: controller,
            maxLines: 4,
            minLines: 2,
            decoration: const InputDecoration(
              labelText: '短语文本',
              hintText: '输入需要显示/播报的文本',
            ),
            autofocus: true,
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () async {
                final cubit = context.read<QuickSubtitleCubit>();
                if (item == null) {
                  await cubit.addItem(group.id, controller.text);
                } else {
                  await cubit.updateItem(group.id, item.id, controller.text);
                }
                if (!dialogContext.mounted) return;
                Navigator.of(dialogContext).pop();
              },
              child: const Text('保存'),
            ),
          ],
        );
      },
    );

    controller.dispose();
  }
}
