import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/router/app_router.dart';
import '../../../cubits/settings/settings_cubit.dart';
import '../../../cubits/settings/settings_state.dart';
import '../../../widgets/section_card.dart';

/// UI and misc settings section.
class UiSettingsSection extends StatelessWidget {
  const UiSettingsSection({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      builder: (context, state) {
        final s = state.settings;
        final cubit = context.read<SettingsCubit>();
        return SectionCard(
          title: 'UI & 杂项',
          children: [
            SwitchListTile(
              title: const Text('实体顶栏'),
              value: s.solidTopBar,
              onChanged: cubit.setSolidTopBar,
              contentPadding: EdgeInsets.zero,
            ),
            SwitchListTile(
              title: const Text('后台保活'),
              subtitle: const Text('锁屏后继续 ASR/TTS'),
              value: s.keepAlive,
              onChanged: cubit.setKeepAlive,
              contentPadding: EdgeInsets.zero,
            ),
            SwitchListTile(
              title: const Text('按住说话模式'),
              value: s.pushToTalkMode,
              onChanged: cubit.setPushToTalkMode,
              contentPadding: EdgeInsets.zero,
            ),
            SwitchListTile(
              title: const Text('说话人验证'),
              subtitle: const Text('仅目标说话人触发 TTS'),
              value: s.speakerVerifyEnabled,
              onChanged: cubit.setSpeakerVerifyEnabled,
              contentPadding: EdgeInsets.zero,
            ),
            const Divider(),
            ListTile(
              title: const Text('查看日志'),
              trailing: const Icon(Icons.chevron_right),
              contentPadding: EdgeInsets.zero,
              onTap: () => context.push(AppRoutes.log),
            ),
          ],
        );
      },
    );
  }
}
