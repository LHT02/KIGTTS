import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../../domain/entities/recognized_item.dart';
import '../../../cubits/realtime/realtime_cubit.dart';
import '../../../cubits/realtime/realtime_state.dart';

/// Scrollable list of ASR recognition results with TTS progress.
class RecognizedList extends StatelessWidget {
  const RecognizedList({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) => p.recognized != c.recognized,
      builder: (context, state) {
        if (state.recognized.isEmpty) {
          return Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.graphic_eq,
                  size: 64,
                  color: Theme.of(context)
                      .colorScheme
                      .outline
                      .withValues(alpha: 0.3),
                ),
                const SizedBox(height: AppDimensions.spacingMd),
                Text(
                  '等待语音输入...',
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        color: Theme.of(context).colorScheme.outline,
                      ),
                ),
              ],
            ),
          );
        }
        return ListView.builder(
          reverse: true,
          padding: const EdgeInsets.symmetric(
            horizontal: AppDimensions.spacingLg,
            vertical: AppDimensions.spacingSm,
          ),
          itemCount: state.recognized.length,
          itemBuilder: (context, index) {
            final item =
                state.recognized[state.recognized.length - 1 - index];
            return _RecognizedTile(item: item);
          },
        );
      },
    );
  }
}

class _RecognizedTile extends StatelessWidget {
  const _RecognizedTile({required this.item});
  final RecognizedItem item;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: AppDimensions.spacingSm),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(AppDimensions.spacingMd),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      item.text,
                      style: theme.textTheme.bodyMedium,
                    ),
                  ),
                  if (item.playing)
                    const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  if (item.completed)
                    const Icon(
                      Icons.check_circle,
                      size: 16,
                      color: AppColors.success,
                    ),
                ],
              ),
              if (item.progress > 0 && item.progress < 1.0) ...[
                const SizedBox(height: AppDimensions.spacingXs),
                ClipRRect(
                  borderRadius: BorderRadius.circular(2),
                  child: LinearProgressIndicator(
                    value: item.progress,
                    minHeight: 3,
                    backgroundColor: theme.colorScheme.outline
                        .withValues(alpha: 0.2),
                    valueColor: const AlwaysStoppedAnimation(
                      AppColors.primary,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
