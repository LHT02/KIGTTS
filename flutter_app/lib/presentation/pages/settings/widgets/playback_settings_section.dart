import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/utils/snap_utils.dart';
import '../../../cubits/settings/settings_cubit.dart';
import '../../../cubits/settings/settings_state.dart';
import '../../../widgets/section_card.dart';

/// Playback gain and volume settings with snap-at-100% behavior.
class PlaybackSettingsSection extends StatelessWidget {
  const PlaybackSettingsSection({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      builder: (context, state) {
        final s = state.settings;
        final cubit = context.read<SettingsCubit>();
        return SectionCard(
          title: '播放设置',
          children: [
            _SnapGainSlider(
              value: s.playbackGainPercent,
              onChanged: (v) => cubit.setPlaybackGainPercent(v),
            ),
            const SizedBox(height: 8),
            _MinVolumeSlider(
              value: s.minVolumePercent,
              onChanged: (v) => cubit.setMinVolumePercent(v.round()),
            ),
          ],
        );
      },
    );
  }
}

class _MinVolumeSlider extends StatelessWidget {
  const _MinVolumeSlider({
    required this.value,
    required this.onChanged,
  });

  final int value;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              '最小音量阈值',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            Text(
              '$value%',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
            ),
          ],
        ),
        Slider(
          value: value.toDouble(),
          min: 0,
          max: 100,
          divisions: 100,
          onChanged: onChanged,
        ),
      ],
    );
  }
}

/// Custom slider that snaps to 100% when within +/-20% range.
class _SnapGainSlider extends StatefulWidget {
  const _SnapGainSlider({required this.value, required this.onChanged});
  final int value;
  final ValueChanged<int> onChanged;

  @override
  State<_SnapGainSlider> createState() => _SnapGainSliderState();
}

class _SnapGainSliderState extends State<_SnapGainSlider> {
  late double _currentValue;

  @override
  void initState() {
    super.initState();
    _currentValue = widget.value.toDouble();
  }

  @override
  void didUpdateWidget(_SnapGainSlider oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.value != widget.value) {
      _currentValue = widget.value.toDouble();
    }
  }

  @override
  Widget build(BuildContext context) {
    final displayValue = snapPlaybackGain(_currentValue.round());
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              '播放增益',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            Text(
              '$displayValue%',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: displayValue == 100 ? AppColors.primary : null,
                  ),
            ),
          ],
        ),
        Slider(
          value: _currentValue.clamp(0, 1000),
          min: 0,
          max: 1000,
          divisions: 1000,
          onChanged: (v) {
            setState(() => _currentValue = v);
          },
          onChangeEnd: (v) {
            final snapped = snapPlaybackGain(v.round());
            setState(() => _currentValue = snapped.toDouble());
            widget.onChanged(snapped);
          },
        ),
      ],
    );
  }
}
