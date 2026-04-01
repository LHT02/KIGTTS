import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../cubits/settings/settings_cubit.dart';
import '../../../cubits/settings/settings_state.dart';
import '../../../widgets/section_card.dart';

/// Piper TTS synthesis parameter settings.
class PiperSettingsSection extends StatelessWidget {
  const PiperSettingsSection({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      builder: (context, state) {
        final s = state.settings;
        final cubit = context.read<SettingsCubit>();
        return SectionCard(
          title: 'Piper TTS 参数',
          children: [
            _ParamSlider(
              label: '噪声强度 (noiseScale)',
              value: s.piperNoiseScale,
              min: 0,
              max: 2,
              divisions: 200,
              onChanged: cubit.setPiperNoiseScale,
            ),
            _ParamSlider(
              label: '语速 (lengthScale)',
              value: s.piperLengthScale,
              min: 0.1,
              max: 5,
              divisions: 490,
              onChanged: cubit.setPiperLengthScale,
            ),
            _ParamSlider(
              label: '噪声权重 (noiseW)',
              value: s.piperNoiseW,
              min: 0,
              max: 2,
              divisions: 200,
              onChanged: cubit.setPiperNoiseW,
            ),
            _ParamSlider(
              label: '句间停顿',
              value: s.piperSentenceSilence,
              min: 0,
              max: 2,
              divisions: 200,
              suffix: 's',
              onChanged: cubit.setPiperSentenceSilence,
            ),
          ],
        );
      },
    );
  }
}

class _ParamSlider extends StatelessWidget {
  const _ParamSlider({
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    required this.divisions,
    this.suffix = '',
    required this.onChanged,
  });

  final String label;
  final double value;
  final double min;
  final double max;
  final int divisions;
  final String suffix;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(label, style: Theme.of(context).textTheme.bodySmall),
              Text(
                '${value.toStringAsFixed(3)}$suffix',
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
      ),
    );
  }
}
