import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../core/router/app_router.dart';
import '../../core/utils/snap_utils.dart';
import '../cubits/realtime/realtime_cubit.dart';
import '../cubits/realtime/realtime_state.dart';
import '../cubits/settings/settings_cubit.dart';
import '../cubits/settings/settings_state.dart';

/// Global notifier that fires when the fullscreen button is tapped.
///
/// The AppBar's FullscreenAction bumps the counter; QuickSubtitlePage
/// listens and opens the fullscreen dialog.  We use a simple counter
/// (ValueNotifier of int) so each tap is always detected as a change.
final fullscreenActionNotifier = ValueNotifier<int>(0);

const _inputRouteLabels = <int, String>{
  0: '自动',
  1: '内置麦克风',
  2: 'USB 麦克风',
  3: '蓝牙麦克风',
  4: '有线麦克风',
};

const _outputRouteLabels = <int, String>{
  100: '自动',
  101: '扬声器',
  102: '听筒',
  103: '蓝牙输出',
  104: 'USB 输出',
  105: '有线输出',
};

class QuickSubtitleQuickSettingsAction extends StatelessWidget {
  const QuickSubtitleQuickSettingsAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      tooltip: '快捷设置',
      icon: const Icon(Icons.tune_sharp, size: 22),
      onPressed: () => _showQuickSettingsPanel(context),
    );
  }
}

Future<void> _showQuickSettingsPanel(BuildContext context) async {
  await showGeneralDialog<void>(
    context: context,
    barrierLabel: '快捷设置',
    barrierDismissible: true,
    barrierColor: Colors.black38,
    transitionDuration: const Duration(milliseconds: 180),
    pageBuilder: (dialogContext, _, _) {
      final width = MediaQuery.sizeOf(dialogContext).width;
      return SafeArea(
        child: Align(
          alignment: Alignment.topRight,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minWidth: 280,
                maxWidth: (width - 24).clamp(280, 420).toDouble(),
                maxHeight: MediaQuery.sizeOf(dialogContext).height * 0.72,
              ),
              child: const _QuickSettingsPanel(),
            ),
          ),
        ),
      );
    },
    transitionBuilder: (context, animation, _, child) {
      final eased = CurvedAnimation(
        parent: animation,
        curve: Curves.easeOutCubic,
      );
      return FadeTransition(
        opacity: eased,
        child: ScaleTransition(
          alignment: Alignment.topRight,
          scale: Tween<double>(begin: 0.96, end: 1).animate(eased),
          child: child,
        ),
      );
    },
  );
}

class _QuickSettingsPanel extends StatefulWidget {
  const _QuickSettingsPanel();

  @override
  State<_QuickSettingsPanel> createState() => _QuickSettingsPanelState();
}

class _QuickSettingsPanelState extends State<_QuickSettingsPanel> {
  double? _draftGain;
  double? _draftMinVolume;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cubit = context.read<SettingsCubit>();

    return Material(
      color: theme.colorScheme.surface,
      elevation: 12,
      borderRadius: BorderRadius.circular(16),
      child: BlocBuilder<SettingsCubit, SettingsState>(
        buildWhen: (p, c) =>
            p.settings.preferredInputType != c.settings.preferredInputType ||
            p.settings.preferredOutputType != c.settings.preferredOutputType ||
            p.settings.pushToTalkMode != c.settings.pushToTalkMode ||
            p.settings.playbackGainPercent != c.settings.playbackGainPercent ||
            p.settings.minVolumePercent != c.settings.minVolumePercent,
        builder: (context, state) {
          final s = state.settings;

          _draftGain ??= s.playbackGainPercent.toDouble();
          _draftMinVolume ??= s.minVolumePercent.toDouble();

          final displayGain = snapPlaybackGain(_draftGain!.round());

          return SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(
                      Icons.graphic_eq_sharp,
                      color: theme.colorScheme.primary,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '快捷设置',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const Spacer(),
                    IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.keyboard_arrow_up_sharp),
                      visualDensity: VisualDensity.compact,
                      tooltip: '收起',
                    ),
                  ],
                ),
                BlocBuilder<RealtimeCubit, RealtimeState>(
                  buildWhen: (p, c) =>
                      p.inputLevel != c.inputLevel ||
                      p.playbackProgress != c.playbackProgress ||
                      p.currentVoiceDir != c.currentVoiceDir,
                  builder: (context, rtState) {
                    final voiceLoaded = rtState.currentVoiceDir != null;
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          voiceLoaded ? '已加载语音包' : '未加载语音包',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 8),
                        _LevelBar(
                          icon: Icons.mic_sharp,
                          value: rtState.inputLevel.clamp(0, 1),
                        ),
                        const SizedBox(height: 8),
                        _LevelBar(
                          icon: Icons.graphic_eq_sharp,
                          value: rtState.playbackProgress.clamp(0, 1),
                        ),
                      ],
                    );
                  },
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: _RouteSelector(
                        icon: Icons.mic_sharp,
                        value: s.preferredInputType,
                        items: _inputRouteLabels,
                        onChanged: (v) {
                          if (v != null) cubit.setPreferredInputType(v);
                        },
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _RouteSelector(
                        icon: Icons.volume_up_sharp,
                        value: s.preferredOutputType,
                        items: _outputRouteLabels,
                        onChanged: (v) {
                          if (v != null) cubit.setPreferredOutputType(v);
                        },
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  dense: true,
                  title: const Text('按住说话'),
                  value: s.pushToTalkMode,
                  onChanged: (v) => cubit.setPushToTalkMode(v),
                  secondary: const Icon(Icons.mic_sharp),
                ),
                const SizedBox(height: 4),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('麦克风阈值', style: theme.textTheme.bodyMedium),
                    Text('${_draftMinVolume!.round()}%'),
                  ],
                ),
                Slider(
                  value: _draftMinVolume!.clamp(0, 100),
                  min: 0,
                  max: 100,
                  divisions: 100,
                  onChanged: (v) => setState(() => _draftMinVolume = v),
                  onChangeEnd: (v) => cubit.setMinVolumePercent(v.round()),
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('音量倍率', style: theme.textTheme.bodyMedium),
                    Text(
                      '$displayGain%',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: displayGain == 100
                            ? theme.colorScheme.primary
                            : null,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
                Slider(
                  value: _draftGain!.clamp(0, 1000),
                  min: 0,
                  max: 1000,
                  divisions: 1000,
                  onChanged: (v) => setState(() => _draftGain = v),
                  onChangeEnd: (v) {
                    final snapped = snapPlaybackGain(v.round());
                    setState(() => _draftGain = snapped.toDouble());
                    cubit.setPlaybackGainPercent(snapped);
                  },
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _LevelBar extends StatelessWidget {
  const _LevelBar({required this.icon, required this.value});

  final IconData icon;
  final double value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 20),
        const SizedBox(width: 10),
        Expanded(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: LinearProgressIndicator(value: value, minHeight: 7),
          ),
        ),
      ],
    );
  }
}

class _RouteSelector extends StatelessWidget {
  const _RouteSelector({
    required this.icon,
    required this.value,
    required this.items,
    required this.onChanged,
  });

  final IconData icon;
  final int value;
  final Map<int, String> items;
  final ValueChanged<int?> onChanged;

  @override
  Widget build(BuildContext context) {
    final shownValue = items.containsKey(value) ? value : items.keys.first;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Theme.of(context).dividerColor),
      ),
      child: Row(
        children: [
          Icon(icon, size: 18),
          const SizedBox(width: 6),
          Expanded(
            child: DropdownButtonHideUnderline(
              child: DropdownButton<int>(
                value: shownValue,
                isExpanded: true,
                isDense: true,
                menuMaxHeight: 240,
                items: items.entries
                    .map(
                      (e) => DropdownMenuItem<int>(
                        value: e.key,
                        child: Text(e.value, overflow: TextOverflow.ellipsis),
                      ),
                    )
                    .toList(),
                onChanged: onChanged,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Fullscreen toggle action for the top bar (home / quick-subtitle page).
class FullscreenAction extends StatelessWidget {
  const FullscreenAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.fullscreen_sharp, size: 22),
      onPressed: () {
        fullscreenActionNotifier.value++;
      },
      tooltip: '全屏',
    );
  }
}

/// Log page navigation action for the top bar (shown on settings page).
class LogAction extends StatelessWidget {
  const LogAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.article_sharp, size: 22),
      onPressed: () => context.go(AppRoutes.log),
      tooltip: '日志',
    );
  }
}

/// Import voice pack action (shown on voicepacks page).
class VoicePackImportAction extends StatelessWidget {
  const VoicePackImportAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.folder_open_sharp, size: 22),
      onPressed: () {
        // TODO: wire to ModelManagerCubit.importVoicePack()
      },
      tooltip: '导入语音包',
    );
  }
}

/// Save drawing action (shown on drawing page).
class DrawingSaveAction extends StatelessWidget {
  const DrawingSaveAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.save_sharp, size: 22),
      onPressed: () {
        // TODO: wire to DrawingCubit.save()
      },
      tooltip: '保存',
    );
  }
}

/// Add new quick card action.
class QuickCardAddAction extends StatelessWidget {
  const QuickCardAddAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.add_sharp, size: 22),
      onPressed: () {
        // TODO: wire to QuickCardCubit.addCard()
      },
      tooltip: '新建名片',
    );
  }
}

/// QR scan action for quick card page.
class QuickCardScanAction extends StatelessWidget {
  const QuickCardScanAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.qr_code_scanner_sharp, size: 22),
      onPressed: () {
        // TODO: navigate to QR scanner sub-page
      },
      tooltip: '扫码',
    );
  }
}

/// Refresh log action.
class LogRefreshAction extends StatelessWidget {
  const LogRefreshAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.refresh_sharp, size: 22),
      onPressed: () {
        // TODO: wire to log refresh
      },
      tooltip: '刷新',
    );
  }
}

/// Copy logs to clipboard action.
class LogCopyAction extends StatelessWidget {
  const LogCopyAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.content_copy_sharp, size: 22),
      onPressed: () {
        // TODO: wire to log copy
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('日志已复制到剪贴板')));
      },
      tooltip: '复制',
    );
  }
}

/// Share logs action.
class LogShareAction extends StatelessWidget {
  const LogShareAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.share_sharp, size: 22),
      onPressed: () {
        // TODO: wire to platform share
      },
      tooltip: '分享',
    );
  }
}
