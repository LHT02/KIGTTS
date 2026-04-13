import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
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
      height: 80,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        itemCount: items.length,
        itemBuilder: (_, i) {
          final item = items[i];
          final isDisplayed = item.text == displayText;

          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: _PhraseChip(
              item: item,
              isDisplayed: isDisplayed,
              onTap: () {
                cubit.selectItem(i);
                cubit.sendItem(item);
              },
              onLongPress: () => cubit.sendItem(item),
            ),
          );
        },
      ),
    );
  }
}

class _PhraseChip extends StatelessWidget {
  const _PhraseChip({
    required this.item,
    required this.isDisplayed,
    required this.onTap,
    required this.onLongPress,
  });

  final QuickSubtitleItem item;
  final bool isDisplayed;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return SizedBox(
      width: 110,
      child: Material(
        color: isDisplayed
            ? AppColors.primary.withValues(alpha: 0.12)
            : isDark
                ? theme.colorScheme.surfaceContainerHighest
                : theme.colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(14),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onTap,
          onLongPress: onLongPress,
          splashColor: AppColors.primary.withValues(alpha: 0.15),
          highlightColor: AppColors.primary.withValues(alpha: 0.08),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            decoration: isDisplayed
                ? BoxDecoration(
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(
                      color: AppColors.primary.withValues(alpha: 0.5),
                      width: 1.5,
                    ),
                  )
                : null,
            alignment: Alignment.center,
            child: Text(
              item.text,
              style: theme.textTheme.bodySmall?.copyWith(
                color: isDisplayed
                    ? AppColors.primary
                    : theme.colorScheme.onSurface,
                fontWeight:
                    isDisplayed ? FontWeight.w600 : FontWeight.normal,
                height: 1.3,
              ),
              textAlign: TextAlign.center,
              maxLines: 3,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ),
      ),
    );
  }
}
