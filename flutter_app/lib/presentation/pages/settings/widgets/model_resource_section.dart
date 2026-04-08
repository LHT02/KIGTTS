import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../cubits/settings/settings_cubit.dart';
import '../../../cubits/settings/settings_state.dart';
import '../../../widgets/section_card.dart';

/// Settings section: 模型与资源 (Model & Resource).
/// Shows ASR model path, voice pack path, and drawing save path.
class ModelResourceSection extends StatelessWidget {
  const ModelResourceSection({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) =>
          p.settings.drawingSaveRelativePath !=
          c.settings.drawingSaveRelativePath,
      builder: (context, state) {
        final s = state.settings;
        return SectionCard(
          title: '模型与资源',
          children: [
            const _InfoRow(
              icon: Icons.hearing_sharp,
              label: 'ASR 模型',
              value: '在语音包页面管理',
            ),
            const SizedBox(height: 8),
            const _InfoRow(
              icon: Icons.record_voice_over_sharp,
              label: '语音包',
              value: '在语音包页面管理',
            ),
            const SizedBox(height: 8),
            _InfoRow(
              icon: Icons.folder_sharp,
              label: '画板保存路径',
              value: s.drawingSaveRelativePath,
            ),
          ],
        );
      },
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Icon(icon, size: 18, color: AppColors.primary),
        const SizedBox(width: 8),
        Text(label, style: theme.textTheme.bodySmall),
        const Spacer(),
        Flexible(
          child: Text(
            value,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }
}
