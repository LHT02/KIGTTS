import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../cubits/model_manager/model_manager_cubit.dart';
import '../../../cubits/model_manager/model_manager_state.dart';

/// Bottom sheet showing voice pack details with edit, delete, export actions.
class VoicePackDetailSheet extends StatefulWidget {
  const VoicePackDetailSheet({super.key, required this.dirName});
  final String dirName;

  @override
  State<VoicePackDetailSheet> createState() => _VoicePackDetailSheetState();
}

class _VoicePackDetailSheetState extends State<VoicePackDetailSheet> {
  late TextEditingController _nameController;
  late TextEditingController _remarkController;

  @override
  void initState() {
    super.initState();
    final state = context.read<ModelManagerCubit>().state;
    final pack = state.voicePacks
        .where((p) => p.dirName == widget.dirName)
        .firstOrNull;
    _nameController = TextEditingController(text: pack?.meta.name ?? '');
    _remarkController = TextEditingController(text: pack?.meta.remark ?? '');
  }

  @override
  void dispose() {
    _nameController.dispose();
    _remarkController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<ModelManagerCubit, ModelManagerState>(
      builder: (context, state) {
        final pack = state.voicePacks
            .where((p) => p.dirName == widget.dirName)
            .firstOrNull;
        if (pack == null) return const SizedBox.shrink();
        final cubit = context.read<ModelManagerCubit>();

        return DraggableScrollableSheet(
          initialChildSize: 0.5,
          minChildSize: 0.3,
          maxChildSize: 0.8,
          expand: false,
          builder: (_, scrollController) {
            return SingleChildScrollView(
              controller: scrollController,
              child: Padding(
                padding: const EdgeInsets.all(AppDimensions.spacingLg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildHandle(theme),
                    const SizedBox(height: AppDimensions.spacingLg),
                    Text('编辑语音包', style: theme.textTheme.titleMedium),
                    const SizedBox(height: AppDimensions.spacingLg),
                    _buildNameField(cubit),
                    const SizedBox(height: AppDimensions.spacingMd),
                    _buildRemarkField(cubit),
                    const SizedBox(height: AppDimensions.spacingLg),
                    _SavePinRow(
                      cubit: cubit,
                      dirName: widget.dirName,
                      nameController: _nameController,
                      remarkController: _remarkController,
                      isPinned: pack.meta.pinned,
                      onTogglePin: () => cubit.togglePin(pack),
                    ),
                    const SizedBox(height: AppDimensions.spacingSm),
                    _LoadDeleteRow(
                      cubit: cubit,
                      pack: pack,
                      dirName: widget.dirName,
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildHandle(ThemeData theme) {
    return Center(
      child: Container(
        width: 40,
        height: 4,
        decoration: BoxDecoration(
          color: theme.colorScheme.outline.withValues(alpha: 0.3),
          borderRadius: BorderRadius.circular(2),
        ),
      ),
    );
  }

  Widget _buildNameField(ModelManagerCubit cubit) {
    return TextField(
      controller: _nameController,
      decoration: const InputDecoration(
        labelText: '名称',
        border: OutlineInputBorder(),
      ),
      onSubmitted: (v) => cubit.updateName(widget.dirName, v),
    );
  }

  Widget _buildRemarkField(ModelManagerCubit cubit) {
    return TextField(
      controller: _remarkController,
      decoration: const InputDecoration(
        labelText: '备注',
        border: OutlineInputBorder(),
      ),
      maxLines: 3,
      onSubmitted: (v) => cubit.updateRemark(widget.dirName, v),
    );
  }
}

class _SavePinRow extends StatelessWidget {
  const _SavePinRow({
    required this.cubit,
    required this.dirName,
    required this.nameController,
    required this.remarkController,
    required this.isPinned,
    required this.onTogglePin,
  });

  final ModelManagerCubit cubit;
  final String dirName;
  final TextEditingController nameController;
  final TextEditingController remarkController;
  final bool isPinned;
  final VoidCallback onTogglePin;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: OutlinedButton.icon(
            onPressed: () {
              cubit.updateName(dirName, nameController.text);
              cubit.updateRemark(dirName, remarkController.text);
              Navigator.pop(context);
            },
            icon: const Icon(Icons.save),
            label: const Text('保存'),
          ),
        ),
        const SizedBox(width: AppDimensions.spacingSm),
        Expanded(
          child: OutlinedButton.icon(
            onPressed: onTogglePin,
            icon: Icon(isPinned ? Icons.push_pin : Icons.push_pin_outlined),
            label: Text(isPinned ? '取消置顶' : '置顶'),
          ),
        ),
      ],
    );
  }
}

class _LoadDeleteRow extends StatelessWidget {
  const _LoadDeleteRow({
    required this.cubit,
    required this.pack,
    required this.dirName,
  });

  final ModelManagerCubit cubit;
  final dynamic pack;
  final String dirName;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Expanded(
          child: OutlinedButton.icon(
            onPressed: () {
              cubit.selectVoice(pack);
              Navigator.pop(context);
            },
            icon: const Icon(Icons.play_arrow),
            label: const Text('加载'),
            style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.primary,
            ),
          ),
        ),
        const SizedBox(width: AppDimensions.spacingSm),
        Expanded(
          child: OutlinedButton.icon(
            onPressed: () => _confirmDelete(context),
            icon: const Icon(Icons.delete),
            label: const Text('删除'),
            style: OutlinedButton.styleFrom(
              foregroundColor: theme.colorScheme.error,
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _confirmDelete(BuildContext context) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('删除语音包'),
        content: Text('确定要删除 "${pack.meta.name}" 吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirm == true && context.mounted) {
      cubit.deleteVoice(dirName);
      Navigator.pop(context);
    }
  }
}
