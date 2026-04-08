import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_dimensions.dart';
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
    return ListView(
      padding: const EdgeInsets.only(
        top: AppDimensions.pageTopBlank,
        bottom: AppDimensions.spacingLg,
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
    return BlocBuilder<OverlayCubit, app.OverlayState>(
      builder: (context, state) {
        final cubit = context.read<OverlayCubit>();
        return SectionCard(
          title: '悬浮窗状态',
          children: [
            Row(
              children: [
                Icon(
                  Icons.open_in_new_sharp,
                  size: 40,
                  color: state.isShowing
                      ? AppColors.primary
                      : theme.colorScheme.outline,
                ),
                const SizedBox(width: AppDimensions.spacingMd),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        state.isShowing ? '悬浮窗已开启' : '悬浮窗已关闭',
                        style: theme.textTheme.titleSmall,
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
                    onPressed: () => cubit.initialize(),
                    icon: const Icon(Icons.refresh_sharp, size: 18),
                    label: const Text('刷新状态'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            BlocBuilder<SettingsCubit, SettingsState>(
              buildWhen: (p, c) =>
                  p.settings.floatingOverlayAutoDock !=
                  c.settings.floatingOverlayAutoDock,
              builder: (context, settingsState) {
                return SwitchListTile(
                  title: const Text('自动吸附边缘'),
                  subtitle: const Text('松手后自动贴靠屏幕边缘'),
                  value: settingsState.settings.floatingOverlayAutoDock,
                  onChanged: (_) {}, // TODO: add cubit method
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
          p.settings.keepAlive != c.settings.keepAlive,
      builder: (context, state) {
        final s = state.settings;
        return SectionCard(
          title: '交互模式',
          children: [
            _StatusRow(
              icon: Icons.touch_app_sharp,
              label: '按住说话',
              enabled: s.pushToTalkMode,
            ),
            const SizedBox(height: 8),
            _StatusRow(
              icon: Icons.lock_sharp,
              label: '后台保活',
              enabled: s.keepAlive,
            ),
            const SizedBox(height: 8),
            Text(
              '这些选项在设置页中配置',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
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
          p.settings.playbackGainPercent !=
              c.settings.playbackGainPercent ||
          p.settings.preferredInputType !=
              c.settings.preferredInputType ||
          p.settings.preferredOutputType !=
              c.settings.preferredOutputType,
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
              items: const {
                0: '默认',
                1: 'VOICE_COMMUNICATION',
                6: 'MIC',
              },
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

class _StatusRow extends StatelessWidget {
  const _StatusRow({
    required this.icon,
    required this.label,
    required this.enabled,
  });

  final IconData icon;
  final String label;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Icon(
          icon,
          size: 20,
          color: enabled
              ? AppColors.primary
              : theme.colorScheme.outline,
        ),
        const SizedBox(width: 8),
        Text(label, style: theme.textTheme.bodyMedium),
        const Spacer(),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
          decoration: BoxDecoration(
            color: enabled
                ? AppColors.primary.withValues(alpha: 0.15)
                : theme.colorScheme.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(4),
          ),
          child: Text(
            enabled ? '已启用' : '未启用',
            style: theme.textTheme.labelSmall?.copyWith(
              color: enabled ? AppColors.primary : theme.colorScheme.outline,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }
}

