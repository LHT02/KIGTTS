import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_dimensions.dart';
import '../../cubits/settings/settings_cubit.dart';
import '../../cubits/settings/settings_state.dart';
import '../../widgets/staggered_card.dart';
import 'widgets/device_monitor_section.dart';
import 'widgets/model_resource_section.dart';
import 'widgets/recognition_section.dart';
import 'widgets/system_layout_section.dart';

/// Application settings page (P0).
/// 4 staggered cards: 模型与资源, 设备监控, 系统与布局, 识别与转换.
class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    // SettingsCubit is provided globally in app.dart
    return const _SettingsView();
  }
}

class _SettingsView extends StatelessWidget {
  const _SettingsView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) => p.loading != c.loading,
      builder: (context, state) {
        if (state.loading) {
          return const Center(child: CircularProgressIndicator());
        }
        return ListView(
          padding: const EdgeInsets.only(
            top: AppDimensions.pageTopBlank,
            bottom: AppDimensions.spacingLg,
          ),
          children: const [
            // Card 1: 模型与资源
            StaggeredCard(
              index: 0,
              child: ModelResourceSection(),
            ),
            // Card 2: 设备监控
            StaggeredCard(
              index: 1,
              child: DeviceMonitorSection(),
            ),
            // Card 3: 系统与布局
            StaggeredCard(
              index: 2,
              child: SystemLayoutSection(),
            ),
            // Card 4: 识别与转换
            StaggeredCard(
              index: 3,
              child: RecognitionSection(),
            ),
          ],
        );
      },
    );
  }
}
