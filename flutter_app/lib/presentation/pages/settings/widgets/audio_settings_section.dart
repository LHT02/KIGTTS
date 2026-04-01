import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../cubits/settings/settings_cubit.dart';
import '../../../cubits/settings/settings_state.dart';
import '../../../widgets/section_card.dart';

/// Audio input/output settings section.
class AudioSettingsSection extends StatelessWidget {
  const AudioSettingsSection({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      builder: (context, state) {
        final s = state.settings;
        final cubit = context.read<SettingsCubit>();
        return SectionCard(
          title: '音频设置',
          children: [
            SwitchListTile(
              title: const Text('播放时静音麦克风'),
              subtitle: const Text('TTS 播放期间暂停录音'),
              value: s.muteWhilePlaying,
              onChanged: cubit.setMuteWhilePlaying,
              contentPadding: EdgeInsets.zero,
            ),
            if (s.muteWhilePlaying) ...[
              const SizedBox(height: AppDimensions.spacingSm),
              _SliderTile(
                label: '静音延迟',
                value: s.muteWhilePlayingDelaySec,
                min: 0,
                max: 5,
                divisions: 50,
                suffix: 's',
                onChanged: cubit.setMuteDelay,
              ),
            ],
            const Divider(),
            SwitchListTile(
              title: const Text('回声抑制'),
              subtitle: const Text('使用 VOICE_COMMUNICATION 音源'),
              value: s.echoSuppression,
              onChanged: cubit.setEchoSuppression,
              contentPadding: EdgeInsets.zero,
            ),
            SwitchListTile(
              title: const Text('通信模式'),
              subtitle: const Text('切换至 IN_COMMUNICATION 模式'),
              value: s.communicationMode,
              onChanged: cubit.setCommunicationMode,
              contentPadding: EdgeInsets.zero,
            ),
            SwitchListTile(
              title: const Text('AEC3 回声消除'),
              subtitle: const Text('WebRTC AEC3 算法'),
              value: s.aec3Enabled,
              onChanged: cubit.setAec3Enabled,
              contentPadding: EdgeInsets.zero,
            ),
          ],
        );
      },
    );
  }
}

class _SliderTile extends StatelessWidget {
  const _SliderTile({
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    this.divisions,
    this.suffix = '',
    required this.onChanged,
  });

  final String label;
  final double value;
  final double min;
  final double max;
  final int? divisions;
  final String suffix;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(label, style: Theme.of(context).textTheme.bodyMedium),
            Text(
              '${value.toStringAsFixed(2)}$suffix',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
        Slider(
          value: value.clamp(min, max),
          min: min,
          max: max,
          divisions: divisions,
          onChanged: onChanged,
        ),
      ],
    );
  }
}
