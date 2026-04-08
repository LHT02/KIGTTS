import 'dart:io';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../../domain/entities/voice_pack_info.dart';

/// Card displaying a voice pack in the reorderable list.
///
/// Layout: [72x72 Avatar] [8dp gap] [Name+Remark col] [Spacer] [Badges] [drag_indicator]
class VoicePackCard extends StatelessWidget {
  const VoicePackCard({
    super.key,
    required this.pack,
    required this.isSelected,
    required this.onTap,
  });

  final VoicePackInfo pack;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: AppDimensions.spacingSm),
      child: Card(
        elevation: AppDimensions.elevationCard,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
        ),
        color: isSelected
            ? AppColors.primary.withValues(alpha: 0.08)
            : theme.cardTheme.color,
        child: InkWell(
          borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppDimensions.spacingMd,
              vertical: AppDimensions.spacingSm,
            ),
            child: Row(
              children: [
                _Avatar(avatarPath: pack.avatarPath),
                const SizedBox(width: AppDimensions.spacingSm),
                Expanded(
                  child: _PackInfo(
                    name: pack.meta.name,
                    remark: pack.meta.remark,
                    isPinned: pack.meta.pinned,
                  ),
                ),
                if (isSelected) _buildCurrentBadge(theme),
                const SizedBox(width: AppDimensions.spacingSm),
                Icon(
                  Icons.drag_indicator,
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildCurrentBadge(ThemeData theme) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: AppColors.primary,
        borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
      ),
      child: Text(
        '当前',
        style: theme.textTheme.labelSmall?.copyWith(
          color: Colors.white,
          fontWeight: FontWeight.w500,
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
    const size = AppDimensions.voicePackAvatarSize;
    if (avatarPath != null) {
      final file = File(avatarPath!);
      if (file.existsSync()) {
        return ClipRRect(
          borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
          child: Image.file(
            file,
            width: size,
            height: size,
            fit: BoxFit.cover,
          ),
        );
      }
    }
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: AppColors.primary.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
      ),
      child: const Icon(
        Icons.record_voice_over,
        color: AppColors.primary,
        size: size * 0.45,
      ),
    );
  }
}

class _PackInfo extends StatelessWidget {
  const _PackInfo({
    required this.name,
    required this.remark,
    required this.isPinned,
  });

  final String name;
  final String remark;
  final bool isPinned;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Row(
          children: [
            if (isPinned)
              const Padding(
                padding: EdgeInsets.only(right: 4),
                child: Icon(
                  Icons.push_pin,
                  size: 14,
                  color: AppColors.primary,
                ),
              ),
            Expanded(
              child: Text(
                name,
                style: theme.textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
        if (remark.isNotEmpty)
          Text(
            remark,
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
