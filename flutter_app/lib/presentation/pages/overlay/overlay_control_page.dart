import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_dimensions.dart';
import '../../../domain/repositories/overlay_repository.dart';
import '../../../injection.dart';
import '../../cubits/overlay/overlay_cubit.dart';
import '../../cubits/overlay/overlay_state.dart' as app;

/// Floating overlay control page (P1).
class OverlayControlPage extends StatelessWidget {
  const OverlayControlPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => OverlayCubit(
        overlayRepository: getIt<OverlayRepository>(),
      )..initialize(),
      child: const _OverlayControlView(),
    );
  }
}

class _OverlayControlView extends StatelessWidget {
  const _OverlayControlView();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<OverlayCubit, app.OverlayState>(
      builder: (context, state) {
        final cubit = context.read<OverlayCubit>();
        return Padding(
          padding: const EdgeInsets.all(AppDimensions.spacingLg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('悬浮窗控制', style: theme.textTheme.titleLarge),
              const SizedBox(height: AppDimensions.spacingXl),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(AppDimensions.spacingLg),
                  child: Row(
                    children: [
                      Icon(
                        Icons.open_in_new,
                        size: 48,
                        color: state.isShowing
                            ? AppColors.primary
                            : theme.colorScheme.outline,
                      ),
                      const SizedBox(width: AppDimensions.spacingLg),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              state.isShowing ? '悬浮窗已开启' : '悬浮窗已关闭',
                              style: theme.textTheme.titleMedium,
                            ),
                            Text(
                              '显示实时识别结果和快捷控制',
                              style: theme.textTheme.bodySmall?.copyWith(
                                color: theme.colorScheme.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Switch(
                        value: state.isShowing,
                        onChanged: (_) => cubit.toggle(),
                      ),
                    ],
                  ),
                ),
              ),
              if (state.error != null)
                Padding(
                  padding:
                      const EdgeInsets.only(top: AppDimensions.spacingMd),
                  child: Text(
                    state.error!,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.error,
                    ),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}
