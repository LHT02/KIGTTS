import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../domain/entities/quick_subtitle.dart';
import '../../../cubits/quick_subtitle/quick_subtitle_cubit.dart';

/// Horizontal tab bar for switching between subtitle groups.
class SubtitleCategoryTabs extends StatelessWidget {
  const SubtitleCategoryTabs({
    super.key,
    required this.groups,
    required this.selectedIndex,
  });

  final List<QuickSubtitleGroup> groups;
  final int selectedIndex;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cubit = context.read<QuickSubtitleCubit>();

    return SizedBox(
      height: 36,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        itemCount: groups.length + 1, // +1 for add button
        itemBuilder: (_, i) {
          if (i == groups.length) {
            return Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4),
              child: ActionChip(
                avatar: const Icon(Icons.edit_sharp, size: 16),
                label: const Text('编辑'),
                onPressed: () {
                  context.push(AppRoutes.quickSubtitleEditor);
                },
              ),
            );
          }
          final group = groups[i];
          final isSelected = i == selectedIndex;
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: ChoiceChip(
              label: Text('${group.icon.isNotEmpty ? '' : ''}${group.name}'),
              selected: isSelected,
              selectedColor: AppColors.primary.withValues(alpha: 0.2),
              onSelected: (_) => cubit.selectGroup(i),
              labelStyle: TextStyle(
                color: isSelected
                    ? AppColors.primary
                    : theme.colorScheme.onSurface,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
              ),
            ),
          );
        },
      ),
    );
  }
}
