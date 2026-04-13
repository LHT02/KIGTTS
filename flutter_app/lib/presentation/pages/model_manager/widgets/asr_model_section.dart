import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../cubits/model_manager/model_manager_cubit.dart';
import '../../../cubits/model_manager/model_manager_state.dart';

/// Section displaying loaded ASR model info.
class AsrModelSection extends StatelessWidget {
  const AsrModelSection({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<ModelManagerCubit, ModelManagerState>(
      buildWhen: (p, c) =>
          p.asrModels != c.asrModels ||
          p.currentAsrDirName != c.currentAsrDirName,
      builder: (context, state) {
        final cubit = context.read<ModelManagerCubit>();
        return Padding(
          padding: const EdgeInsets.all(AppDimensions.spacingLg),
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(AppDimensions.spacingLg),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'ASR 模型',
                    style: theme.textTheme.titleSmall?.copyWith(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: AppDimensions.spacingSm),
                  if (state.asrModels.isEmpty)
                    _EmptyLabel(theme: theme)
                  else
                    ...state.asrModels.map(
                      (model) => _AsrModelRow(
                        dirName: model.dirName,
                        isBundled: model.isBundled,
                        isSelected: model.dirName == state.currentAsrDirName,
                        onTap: () =>
                            cubit.selectAsr(model.dirPath, model.dirName),
                      ),
                    ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _EmptyLabel extends StatelessWidget {
  const _EmptyLabel({required this.theme});
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return Text(
      '未找到 ASR 模型',
      style: theme.textTheme.bodySmall?.copyWith(
        color: theme.colorScheme.onSurfaceVariant,
      ),
    );
  }
}

class _AsrModelRow extends StatelessWidget {
  const _AsrModelRow({
    required this.dirName,
    required this.isBundled,
    required this.isSelected,
    required this.onTap,
  });
  final String dirName;
  final bool isBundled;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(8),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
            child: Row(
              children: [
                Icon(
                  isSelected ? Icons.radio_button_checked : Icons.hearing,
                  size: 16,
                  color: AppColors.primary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(dirName, style: theme.textTheme.bodySmall),
                ),
                if (isBundled)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 6,
                      vertical: 1,
                    ),
                    decoration: BoxDecoration(
                      color: AppColors.primary.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      '内置',
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: AppColors.primary,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
