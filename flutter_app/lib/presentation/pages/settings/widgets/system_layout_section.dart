import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../cubits/settings/settings_cubit.dart';
import '../../../cubits/settings/settings_state.dart';
import '../../../widgets/section_card.dart';

/// Settings section: 系统与布局 (System & Layout).
/// Contains toggles for top bar, drawer, keepalive, PTT, overlay, etc.
class SystemLayoutSection extends StatelessWidget {
  const SystemLayoutSection({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) =>
          p.settings.solidTopBar != c.settings.solidTopBar ||
          p.settings.keepAlive != c.settings.keepAlive ||
          p.settings.pushToTalkMode != c.settings.pushToTalkMode ||
          p.settings.pushToTalkConfirmInput !=
              c.settings.pushToTalkConfirmInput ||
          p.settings.floatingOverlayEnabled !=
              c.settings.floatingOverlayEnabled ||
          p.settings.speakerVerifyEnabled !=
              c.settings.speakerVerifyEnabled ||
          p.settings.speakerVerifyThreshold !=
              c.settings.speakerVerifyThreshold,
      builder: (context, state) {
        final s = state.settings;
        final cubit = context.read<SettingsCubit>();
        return SectionCard(
          title: '系统与布局',
          children: [
            SwitchListTile(
              title: const Text('实心顶栏'),
              subtitle: const Text('关闭后顶栏透明'),
              value: s.solidTopBar,
              onChanged: (v) => cubit.setSolidTopBar(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            SwitchListTile(
              title: const Text('后台保活'),
              subtitle: const Text('运行时保持前台服务'),
              value: s.keepAlive,
              onChanged: (v) => cubit.setKeepAlive(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            const Divider(height: 16),
            SwitchListTile(
              title: const Text('按住说话模式'),
              subtitle: const Text('开启后需长按录音'),
              value: s.pushToTalkMode,
              onChanged: (v) => cubit.setPushToTalkMode(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            SwitchListTile(
              title: const Text('PTT 确认输入'),
              subtitle: const Text('松手后弹出确认框'),
              value: s.pushToTalkConfirmInput,
              onChanged: (v) => cubit.setPushToTalkConfirmInput(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            const Divider(height: 16),
            SwitchListTile(
              title: const Text('悬浮窗'),
              subtitle: const Text('启用后可在其他应用上显示'),
              value: s.floatingOverlayEnabled,
              onChanged: (v) => cubit.setFloatingOverlayEnabled(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            SwitchListTile(
              title: const Text('说话人验证'),
              subtitle: const Text('非目标说话人不触发 TTS'),
              value: s.speakerVerifyEnabled,
              onChanged: (v) => cubit.setSpeakerVerifyEnabled(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            if (s.speakerVerifyEnabled) ...[
              const SizedBox(height: 4),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '验证阈值',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  Text(
                    s.speakerVerifyThreshold.toStringAsFixed(2),
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                ],
              ),
              Slider(
                value: s.speakerVerifyThreshold,
                min: 0.3,
                max: 0.95,
                divisions: 65,
                onChanged: (v) => cubit.setSpeakerVerifyThreshold(v),
              ),
            ],
          ],
        );
      },
    );
  }
}
