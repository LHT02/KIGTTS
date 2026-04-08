import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../../domain/entities/quick_subtitle.dart';
import '../../../cubits/quick_subtitle/quick_subtitle_cubit.dart';

/// Horizontal row of phrase cards for quick subtitle selection.
class SubtitlePhraseRow extends StatelessWidget {
  const SubtitlePhraseRow({
    super.key,
    required this.items,
    required this.selectedIndex,
    required this.displayText,
    required this.groupId,
  });

  final List<QuickSubtitleItem> items;
  final int selectedIndex;
  final String displayText;
  final String groupId;

  @override
  Widget build(BuildContext context) {
    final cubit = context.read<QuickSubtitleCubit>();
    return SizedBox(
      height: 120,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        itemCount: items.length,
        itemBuilder: (_, i) {
          final item = items[i];
          final isSelected = i == selectedIndex;
          final isDisplayed = item.text == displayText;

          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: _PhraseCard(
              item: item,
              isSelected: isSelected || isDisplayed,
              onTap: () => cubit.selectItem(i),
              onLongPress: () => cubit.sendItem(item),
            ),
          );
        },
      ),
    );
  }
}

class _PhraseCard extends StatelessWidget {
  const _PhraseCard({
    required this.item,
    required this.isSelected,
    required this.onTap,
    required this.onLongPress,
  });

  final QuickSubtitleItem item;
  final bool isSelected;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SizedBox(
      width: 140,
      child: Card(
        elevation: isSelected ? 4 : AppDimensions.elevationCard,
        color: isSelected
            ? AppColors.primary.withValues(alpha: 0.12)
            : theme.cardTheme.color,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppDimensions.radiusMedium),
          side: isSelected
              ? const BorderSide(color: AppColors.primary, width: 1.5)
              : BorderSide.none,
        ),
        child: InkWell(
          borderRadius: BorderRadius.circular(AppDimensions.radiusMedium),
          onTap: onTap,
          onLongPress: onLongPress,
          child: Padding(
            padding: const EdgeInsets.all(AppDimensions.spacingSm),
            child: Center(
              child: Text(
                item.text,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: isSelected
                      ? AppColors.primary
                      : theme.colorScheme.onSurface,
                  fontWeight:
                      isSelected ? FontWeight.w600 : FontWeight.normal,
                ),
                textAlign: TextAlign.center,
                maxLines: 4,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
