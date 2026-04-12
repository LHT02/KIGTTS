import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_dimensions.dart';
import '../../../core/router/app_router.dart';
import '../../../domain/repositories/overlay_repository.dart';
import '../../../injection.dart';
import '../../cubits/overlay/overlay_cubit.dart';
import '../../cubits/overlay/overlay_state.dart' as app;
import 'widgets/device_dropdown.dart';
import '../../cubits/settings/settings_cubit.dart';
import '../../cubits/settings/settings_state.dart';
import '../../widgets/section_card.dart';
import '../../widgets/staggered_card.dart';
import '../settings/widgets/playback_settings_section.dart';

/// Floating overlay control page (P1).
/// 3 staggered cards: 悬浮窗状态, 交互模式, 音频与设备.
class OverlayControlPage extends StatelessWidget {
  const OverlayControlPage({super.key});

  @override
  Widget build(BuildContext context) {
    // SettingsCubit is provided globally in app.dart
    return BlocProvider(
      create: (_) =>
          OverlayCubit(overlayRepository: getIt<OverlayRepository>())
            ..initialize(),
      child: const _OverlayControlView(),
    );
  }
}

class _OverlayControlView extends StatelessWidget {
  const _OverlayControlView();

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.only(
        top: AppDimensions.pageTopBlank,
        bottom: AppDimensions.pageBottomBlank,
      ),
      children: const [
        // Card 1: 悬浮窗状态
        StaggeredCard(index: 0, child: _OverlayStatusSection()),
        // Card 2: 交互模式
        StaggeredCard(index: 1, child: _InteractionModeSection()),
        // Card 3: 音频与设备
        StaggeredCard(index: 2, child: _AudioDeviceSection()),
      ],
    );
  }
}

/// Card 1: Toggle, permission, refresh, auto-dock.
class _OverlayStatusSection extends StatelessWidget {
  const _OverlayStatusSection();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final overlayRepo = getIt<OverlayRepository>();
    return BlocBuilder<OverlayCubit, app.OverlayState>(
      builder: (context, state) {
        final cubit = context.read<OverlayCubit>();
        return SectionCard(
          title: '悬浮窗状态',
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Switch(
                  value: state.isShowing,
                  onChanged: (_) => cubit.toggle(),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text('启用独立悬浮窗', style: theme.textTheme.bodyMedium),
                ),
              ],
            ),
            FutureBuilder<bool>(
              future: overlayRepo.hasPermission(),
              builder: (context, snapshot) {
                final granted = snapshot.data ?? false;
                return Text(
                  '权限状态：${granted ? '已授权' : '未授权'}',
                  style: theme.textTheme.bodySmall,
                );
              },
            ),
            Text(
              '运行状态：${state.isShowing ? '已启用' : '未启用'}',
              style: theme.textTheme.bodySmall,
            ),
            if (state.error != null) ...[
              const SizedBox(height: 8),
              Text(
                state.error!,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.error,
                ),
              ),
            ],
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => cubit.openPermissionSettings(),
                    icon: const Icon(Icons.open_in_new_sharp, size: 18),
                    label: const Text('打开权限设置'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: state.isShowing
                        ? () => cubit.initialize()
                        : null,
                    icon: const Icon(Icons.refresh_sharp, size: 18),
                    label: const Text('刷新悬浮窗'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              '悬浮窗可吸附到屏幕边缘，并可在软件外直接触发快捷字幕输入。',
              style: theme.textTheme.bodySmall,
            ),
            const SizedBox(height: 8),
            BlocBuilder<SettingsCubit, SettingsState>(
              buildWhen: (p, c) =>
                  p.settings.floatingOverlayAutoDock !=
                  c.settings.floatingOverlayAutoDock,
              builder: (context, settingsState) {
                return SwitchListTile(
                  title: const Text('长时间不操作时自动贴边'),
                  subtitle: const Text('开启后，悬浮 FAB 会自动吸附边缘并降低透明度'),
                  value: settingsState.settings.floatingOverlayAutoDock,
                  onChanged: (value) {
                    context.read<SettingsCubit>().setFloatingOverlayAutoDock(
                      value,
                    );
                    context.read<OverlayCubit>().updateConfig({
                      'floating_overlay_auto_dock': value,
                    });
                  },
                  contentPadding: EdgeInsets.zero,
                );
              },
            ),
          ],
        );
      },
    );
  }
}

/// Card 2: Read-only PTT / keepalive status.
class _InteractionModeSection extends StatelessWidget {
  const _InteractionModeSection();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) =>
          p.settings.pushToTalkMode != c.settings.pushToTalkMode ||
          p.settings.pushToTalkConfirmInput !=
              c.settings.pushToTalkConfirmInput ||
          p.settings.keepAlive != c.settings.keepAlive,
      builder: (context, state) {
        final s = state.settings;
        return SectionCard(
          title: '交互模式',
          children: [
            Text(
              '以下交互设置与主设置页完全同步，这里仅显示当前状态。',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '按住说话模式：${s.pushToTalkMode ? '已开启' : '未开启'}',
              style: theme.textTheme.bodySmall,
            ),
            Text(
              '按下输入文本确认：${s.pushToTalkMode ? (s.pushToTalkConfirmInput ? '已开启' : '未开启') : '按住说话未开启'}',
              style: theme.textTheme.bodySmall,
            ),
            Text(
              '保持后台运行：${s.keepAlive ? '已开启' : '未开启'}',
              style: theme.textTheme.bodySmall,
            ),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed: () {
                context.go(AppRoutes.settings);
              },
              icon: const Icon(Icons.tune_sharp, size: 18),
              label: const Text('前往主设置修改'),
            ),
          ],
        );
      },
    );
  }
}

/// Card 3: Gain slider (snap@100%), device dropdowns.
class _AudioDeviceSection extends StatelessWidget {
  const _AudioDeviceSection();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) =>
          p.settings.playbackGainPercent != c.settings.playbackGainPercent ||
          p.settings.preferredInputType != c.settings.preferredInputType ||
          p.settings.preferredOutputType != c.settings.preferredOutputType,
      builder: (context, state) {
        final s = state.settings;
        final cubit = context.read<SettingsCubit>();
        return SectionCard(
          title: '音频与设备',
          children: [
            SnapGainSlider(
              value: s.playbackGainPercent,
              onChanged: (v) => cubit.setPlaybackGainPercent(v),
            ),
            const Divider(height: 24),
            DeviceDropdown(
              label: '输入设备类型',
              icon: Icons.mic_sharp,
              value: s.preferredInputType,
              items: const {0: '默认', 1: 'VOICE_COMMUNICATION', 6: 'MIC'},
              onChanged: (v) {
                if (v != null) cubit.setPreferredInputType(v);
              },
            ),
            const SizedBox(height: 8),
            DeviceDropdown(
              label: '输出设备类型',
              icon: Icons.volume_up_sharp,
              value: s.preferredOutputType,
              items: const {
                100: '默认 (MUSIC)',
                0: 'VOICE_CALL',
                2: 'RING',
                3: 'ALARM',
              },
              onChanged: (v) {
                if (v != null) cubit.setPreferredOutputType(v);
              },
            ),
          ],
        );
      },
    );
  }
}
