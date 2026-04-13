import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_dimensions.dart';
import '../model_manager/model_manager_page.dart';
import '../overlay/overlay_control_page.dart';
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
    return const _CategorizedSettingsView();
  }
}

enum _SettingsCategory {
  audio('识别音频'),
  system('系统布局'),
  voicepacks('语音包'),
  overlay('悬浮窗');

  const _SettingsCategory(this.label);
  final String label;
}

class _CategorizedSettingsView extends StatefulWidget {
  const _CategorizedSettingsView();

  @override
  State<_CategorizedSettingsView> createState() =>
      _CategorizedSettingsViewState();
}

class _CategorizedSettingsViewState extends State<_CategorizedSettingsView> {
  _SettingsCategory _category = _SettingsCategory.audio;

  List<Widget> _buildSections() {
    switch (_category) {
      case _SettingsCategory.audio:
        return const [
          ModelResourceSection(),
          DeviceMonitorSection(),
          RecognitionSection(),
        ];
      case _SettingsCategory.system:
        return const [SystemLayoutSection()];
      case _SettingsCategory.voicepacks:
      case _SettingsCategory.overlay:
        return const [];
    }
  }

  Widget _buildCategoryBody() {
    if (_category == _SettingsCategory.voicepacks) {
      return const ModelManagerPage();
    }
    if (_category == _SettingsCategory.overlay) {
      return const OverlayControlPage();
    }

    final sections = _buildSections();
    return ListView(
      padding: const EdgeInsets.only(bottom: AppDimensions.spacingLg),
      children: [
        ...sections.asMap().entries.map(
          (entry) => StaggeredCard(index: entry.key, child: entry.value),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) => p.loading != c.loading,
      builder: (context, state) {
        if (state.loading) {
          return const Center(child: CircularProgressIndicator());
        }
        return Column(
          children: [
            const SizedBox(height: AppDimensions.pageTopBlank),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: SegmentedButton<_SettingsCategory>(
                  showSelectedIcon: false,
                  segments: _SettingsCategory.values
                      .map(
                        (c) => ButtonSegment<_SettingsCategory>(
                          value: c,
                          label: Text(c.label),
                        ),
                      )
                      .toList(),
                  selected: {_category},
                  onSelectionChanged: (selection) {
                    if (selection.isEmpty) return;
                    setState(() => _category = selection.first);
                  },
                ),
              ),
            ),
            const SizedBox(height: 8),
            Expanded(child: _buildCategoryBody()),
          ],
        );
      },
    );
  }
}
