import 'dart:io';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../../domain/entities/voice_pack_info.dart';

/// Card displaying a voice pack with avatar, name, remark, and pin marker.
class VoicePackCard extends StatelessWidget {
  const VoicePackCard({
    super.key,
    required this.pack,
    required this.isSelected,
    required this.onTap,
    required this.onSelect,
  });

  final VoicePackInfo pack;
  final bool isSelected;
  final VoidCallback onTap;
  final VoidCallback onSelect;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: AppDimensions.spacingSm),
      child: Card(
        color: isSelected
            ? AppColors.primary.withValues(alpha: 0.08)
            : theme.cardTheme.color,
        child: InkWell(
          borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(AppDimensions.spacingMd),
            child: Row(
              children: [
                _Avatar(avatarPath: pack.avatarPath),
                const SizedBox(width: AppDimensions.spacingMd),
                Expanded(child: _PackInfo(pack: pack)),
                _TrailingAction(isSelected: isSelected, onSelect: onSelect),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Avatar extends StatelessWidget {
  const _Avatar({this.avatarPath});
  final String? avatarPath;

  @override
  Widget build(BuildContext context) {
    final size = AppDimensions.voicePackAvatarSize;
    if (avatarPath != null) {
      final file = File(avatarPath!);
      if (file.existsSync()) {
        return ClipRRect(
          borderRadius: BorderRadius.circular(size / 2),
          child: Image.file(
            file,
            width: size,
            height: size,
            fit: BoxFit.cover,
          ),
        );
      }
    }
    return CircleAvatar(
      radius: size / 2,
      backgroundColor: AppColors.primary.withValues(alpha: 0.15),
      child: Icon(
        Icons.record_voice_over,
        color: AppColors.primary,
        size: size * 0.5,
      ),
    );
  }
}

class _PackInfo extends StatelessWidget {
  const _PackInfo({required this.pack});
  final VoicePackInfo pack;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            if (pack.meta.pinned)
              Padding(
                padding: const EdgeInsets.only(right: 4),
                child: Icon(
                  Icons.push_pin,
                  size: 14,
                  color: AppColors.primary,
                ),
              ),
            Expanded(
              child: Text(
                pack.meta.name,
                style: theme.textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
        if (pack.meta.remark.isNotEmpty)
          Text(
            pack.meta.remark,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
      ],
    );
  }
}

class _TrailingAction extends StatelessWidget {
  const _TrailingAction({required this.isSelected, required this.onSelect});
  final bool isSelected;
  final VoidCallback onSelect;

  @override
  Widget build(BuildContext context) {
    if (isSelected) {
      return Icon(Icons.check_circle, color: AppColors.primary, size: 20);
    }
    return IconButton(
      icon: const Icon(Icons.play_arrow, size: 20),
      onPressed: onSelect,
      tooltip: '加载此语音包',
    );
  }
}
