import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:file_picker/file_picker.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../../domain/entities/voice_pack_info.dart';
import '../../../cubits/model_manager/model_manager_cubit.dart';
import '../../../cubits/model_manager/model_manager_state.dart';

/// Dialog showing voice pack details with edit/delete/export actions.
class VoicePackDetailDialog extends StatefulWidget {
  const VoicePackDetailDialog({super.key, required this.dirName});

  final String dirName;

  @override
  State<VoicePackDetailDialog> createState() => _VoicePackDetailDialogState();
}

class _VoicePackDetailDialogState extends State<VoicePackDetailDialog> {
  late TextEditingController _nameCtrl;
  late TextEditingController _remarkCtrl;

  @override
  void initState() {
    super.initState();
    _nameCtrl = TextEditingController();
    _remarkCtrl = TextEditingController();
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _remarkCtrl.dispose();
    super.dispose();
  }

  VoicePackInfo? _findPack(ModelManagerState state) {
    final matches = state.voicePacks.where(
      (p) => p.dirName == widget.dirName,
    );
    return matches.isNotEmpty ? matches.first : null;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<ModelManagerCubit, ModelManagerState>(
      builder: (context, state) {
        final pack = _findPack(state);
        if (pack == null) {
          return AlertDialog(
            title: const Text('语音包不存在'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('关闭'),
              ),
            ],
          );
        }

        // Sync controllers with current data
        if (_nameCtrl.text.isEmpty) _nameCtrl.text = pack.meta.name;
        if (_remarkCtrl.text.isEmpty) _remarkCtrl.text = pack.meta.remark;

        final cubit = context.read<ModelManagerCubit>();
        final isSelected = pack.dirName == state.currentVoiceDirName;

        return AlertDialog(
          title: Row(
            children: [
              Expanded(child: Text(pack.meta.name)),
              if (pack.meta.pinned)
                const Icon(Icons.push_pin, size: 18, color: AppColors.primary),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Avatar
                _AvatarSection(
                  avatarPath: pack.avatarPath,
                  onChangeAvatar: () => _changeAvatar(cubit, pack.dirName),
                ),
                const SizedBox(height: AppDimensions.spacingLg),
                // Name
                TextField(
                  controller: _nameCtrl,
                  decoration: const InputDecoration(
                    labelText: '名称',
                    border: OutlineInputBorder(),
                  ),
                  onSubmitted: (v) => cubit.updateName(pack.dirName, v),
                ),
                const SizedBox(height: AppDimensions.spacingSm),
                // Remark
                TextField(
                  controller: _remarkCtrl,
                  decoration: const InputDecoration(
                    labelText: '备注',
                    border: OutlineInputBorder(),
                  ),
                  maxLines: 2,
                  onSubmitted: (v) => cubit.updateRemark(pack.dirName, v),
                ),
                const SizedBox(height: AppDimensions.spacingMd),
                // Info
                if (isSelected)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: AppColors.primary.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      '当前使用中',
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: AppColors.primary,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          actions: [
            // Select
            TextButton(
              onPressed: isSelected ? null : () {
                cubit.selectVoice(pack);
                Navigator.pop(context);
              },
              child: Text(isSelected ? '已选择' : '使用'),
            ),
            // Pin/Unpin
            TextButton(
              onPressed: () => cubit.togglePin(pack),
              child: Text(pack.meta.pinned ? '取消置顶' : '置顶'),
            ),
            // Save edits
            TextButton(
              onPressed: () {
                cubit.updateName(pack.dirName, _nameCtrl.text);
                cubit.updateRemark(pack.dirName, _remarkCtrl.text);
              },
              child: const Text('保存'),
            ),
            // Delete
            TextButton(
              onPressed: () => _confirmDelete(context, cubit, pack.dirName),
              style: TextButton.styleFrom(
                foregroundColor: theme.colorScheme.error,
              ),
              child: const Text('删除'),
            ),
            // Close
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('关闭'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _changeAvatar(
    ModelManagerCubit cubit,
    String dirName,
  ) async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.image,
    );
    if (result != null && result.files.single.path != null) {
      cubit.updateAvatar(dirName, result.files.single.path!);
    }
  }

  void _confirmDelete(
    BuildContext context,
    ModelManagerCubit cubit,
    String dirName,
  ) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('删除语音包'),
        content: const Text('确定要删除此语音包吗？此操作不可恢复。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              Navigator.pop(context);
              cubit.deleteVoice(dirName);
            },
            style: TextButton.styleFrom(
              foregroundColor: Theme.of(ctx).colorScheme.error,
            ),
            child: const Text('删除'),
          ),
        ],
      ),
    );
  }
}

class _AvatarSection extends StatelessWidget {
  const _AvatarSection({
    this.avatarPath,
    required this.onChangeAvatar,
  });

  final String? avatarPath;
  final VoidCallback onChangeAvatar;

  @override
  Widget build(BuildContext context) {
    const size = AppDimensions.voicePackDetailAvatarSize;
    Widget avatar;
    if (avatarPath != null && File(avatarPath!).existsSync()) {
      avatar = ClipRRect(
        borderRadius: BorderRadius.circular(AppDimensions.radiusMedium),
        child: Image.file(
          File(avatarPath!),
          width: size,
          height: size,
          fit: BoxFit.cover,
        ),
      );
    } else {
      avatar = Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: AppColors.primary.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(AppDimensions.radiusMedium),
        ),
        child: const Icon(
          Icons.record_voice_over,
          color: AppColors.primary,
          size: 28,
        ),
      );
    }

    return GestureDetector(
      onTap: onChangeAvatar,
      child: Column(
        children: [
          avatar,
          const SizedBox(height: 4),
          Text(
            '点击更换头像',
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
          ),
        ],
      ),
    );
  }
}
