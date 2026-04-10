import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../cubits/settings/settings_cubit.dart';
import '../../../cubits/settings/settings_state.dart';
import '../../../widgets/section_card.dart';
import 'playback_settings_section.dart';
import '../../../../core/theme/app_colors.dart';

/// Settings section: 识别与转换 (Recognition & Conversion).
/// Contains audio settings, Piper TTS params, and playback settings.
class RecognitionSection extends StatelessWidget {
  const RecognitionSection({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) =>
          p.settings.muteWhilePlaying != c.settings.muteWhilePlaying ||
          p.settings.muteWhilePlayingDelaySec !=
              c.settings.muteWhilePlayingDelaySec ||
          p.settings.echoSuppression != c.settings.echoSuppression ||
          p.settings.aec3Enabled != c.settings.aec3Enabled ||
          p.settings.piperNoiseScale != c.settings.piperNoiseScale ||
          p.settings.piperLengthScale != c.settings.piperLengthScale ||
          p.settings.piperNoiseW != c.settings.piperNoiseW ||
          p.settings.piperSentenceSilence !=
              c.settings.piperSentenceSilence ||
          p.settings.numberReplaceMode != c.settings.numberReplaceMode ||
          p.settings.asrSendToQuickSubtitle !=
              c.settings.asrSendToQuickSubtitle ||
          p.settings.playbackGainPercent !=
              c.settings.playbackGainPercent ||
          p.settings.minVolumePercent != c.settings.minVolumePercent ||
          p.settings.pushToTalkMode != c.settings.pushToTalkMode ||
          p.settings.pushToTalkConfirmInput !=
              c.settings.pushToTalkConfirmInput,
      builder: (context, state) {
        final s = state.settings;
        final cubit = context.read<SettingsCubit>();
        return SectionCard(
          title: '识别与转换',
          children: [
            // --- Audio ---
            SwitchListTile(
              title: const Text('播放时静音麦克风'),
              value: s.muteWhilePlaying,
              onChanged: (v) => cubit.setMuteWhilePlaying(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            if (s.muteWhilePlaying) ...[
              _SliderRow(
                label: '静音延迟',
                valueLabel: '${s.muteWhilePlayingDelaySec.toStringAsFixed(1)}s',
                value: s.muteWhilePlayingDelaySec,
                min: 0,
                max: 2,
                divisions: 20,
                onChanged: (v) => cubit.setMuteDelay(v),
              ),
            ],
            SwitchListTile(
              title: const Text('回声抑制'),
              value: s.echoSuppression,
              onChanged: (v) => cubit.setEchoSuppression(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            SwitchListTile(
              title: const Text('AEC3 回声消除'),
              value: s.aec3Enabled,
              onChanged: (v) => cubit.setAec3Enabled(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            SwitchListTile(
              title: const Text('ASR 发送到字幕'),
              subtitle: const Text('识别结果自动显示在字幕页'),
              value: s.asrSendToQuickSubtitle,
              onChanged: (v) => cubit.setAsrSendToQuickSubtitle(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            const Divider(height: 16),
            // --- Recording mode ---
            Text(
              '录音模式',
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w600,
                  ),
            ),
            const SizedBox(height: 4),
            SwitchListTile(
              title: const Text('按住说话 (PTT)'),
              subtitle: const Text('开启后需按住按钮才会录音'),
              value: s.pushToTalkMode,
              onChanged: (v) => cubit.setPushToTalkMode(v),
              contentPadding: EdgeInsets.zero,
              dense: true,
            ),
            if (s.pushToTalkMode)
              SwitchListTile(
                title: const Text('确认输入模式'),
                subtitle: const Text('松手前可拖动选择：发送字幕/输入框/取消'),
                value: s.pushToTalkConfirmInput,
                onChanged: (v) => cubit.setPushToTalkConfirmInput(v),
                contentPadding: EdgeInsets.zero,
                dense: true,
              ),
            const Divider(height: 16),
            // --- Piper TTS params ---
            Text(
              'Piper TTS 参数',
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w600,
                  ),
            ),
            const SizedBox(height: 4),
            _SliderRow(
              label: 'Noise Scale',
              valueLabel: s.piperNoiseScale.toStringAsFixed(3),
              value: s.piperNoiseScale,
              min: 0,
              max: 2,
              divisions: 200,
              onChanged: (v) => cubit.setPiperNoiseScale(v),
            ),
            _SliderRow(
              label: 'Length Scale',
              valueLabel: s.piperLengthScale.toStringAsFixed(3),
              value: s.piperLengthScale,
              min: 0.1,
              max: 3,
              divisions: 290,
              onChanged: (v) => cubit.setPiperLengthScale(v),
            ),
            _SliderRow(
              label: 'Noise W',
              valueLabel: s.piperNoiseW.toStringAsFixed(3),
              value: s.piperNoiseW,
              min: 0,
              max: 2,
              divisions: 200,
              onChanged: (v) => cubit.setPiperNoiseW(v),
            ),
            _SliderRow(
              label: '句间停顿',
              valueLabel: '${s.piperSentenceSilence.toStringAsFixed(2)}s',
              value: s.piperSentenceSilence,
              min: 0,
              max: 2,
              divisions: 200,
              onChanged: (v) => cubit.setPiperSentenceSilence(v),
            ),
            const Divider(height: 16),
            // --- Playback ---
            const PlaybackSettingsContent(),
            const Divider(height: 16),
            // --- Number replace ---
            _NumberReplaceRow(
              mode: s.numberReplaceMode,
              onChanged: (v) => cubit.setNumberReplaceMode(v),
            ),
          ],
        );
      },
    );
  }
}

class _SliderRow extends StatelessWidget {
  const _SliderRow({
    required this.label,
    required this.valueLabel,
    required this.value,
    required this.min,
    required this.max,
    required this.divisions,
    required this.onChanged,
  });

  final String label;
  final String valueLabel;
  final double value;
  final double min;
  final double max;
  final int divisions;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(label, style: Theme.of(context).textTheme.bodySmall),
            Text(
              valueLabel,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
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

class _NumberReplaceRow extends StatelessWidget {
  const _NumberReplaceRow({
    required this.mode,
    required this.onChanged,
  });

  final int mode;
  final ValueChanged<int> onChanged;

  static const _modes = {
    0: '不替换',
    1: '数字→中文',
    2: '中文→数字',
  };

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Text('数字替换', style: theme.textTheme.bodySmall),
        const Spacer(),
        DropdownButton<int>(
          value: _modes.containsKey(mode) ? mode : 0,
          underline: const SizedBox.shrink(),
          isDense: true,
          items: _modes.entries
              .map(
                (e) => DropdownMenuItem<int>(
                  value: e.key,
                  child: Text(e.value, style: theme.textTheme.bodySmall),
                ),
              )
              .toList(),
          onChanged: (v) {
            if (v != null) onChanged(v);
          },
        ),
      ],
    );
  }
}
