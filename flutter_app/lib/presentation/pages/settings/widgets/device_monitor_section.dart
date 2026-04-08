import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../cubits/realtime/realtime_cubit.dart';
import '../../../cubits/realtime/realtime_state.dart';
import '../../../widgets/section_card.dart';

/// Settings section: 设备监控 (Device Monitor).
/// Displays current input/output device labels and AEC3 status.
class DeviceMonitorSection extends StatelessWidget {
  const DeviceMonitorSection({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) =>
          p.inputDeviceLabel != c.inputDeviceLabel ||
          p.outputDeviceLabel != c.outputDeviceLabel ||
          p.aec3Status != c.aec3Status ||
          p.running != c.running,
      builder: (context, state) {
        return SectionCard(
          title: '设备监控',
          children: [
            _DeviceRow(
              icon: Icons.mic_sharp,
              label: '输入设备',
              value: state.inputDeviceLabel,
            ),
            const SizedBox(height: 8),
            _DeviceRow(
              icon: Icons.volume_up_sharp,
              label: '输出设备',
              value: state.outputDeviceLabel,
            ),
            const SizedBox(height: 8),
            _DeviceRow(
              icon: Icons.hearing_sharp,
              label: 'AEC3 状态',
              value: state.aec3Status,
            ),
            const SizedBox(height: 8),
            _StatusBadge(
              label: state.running ? '运行中' : '已停止',
              active: state.running,
            ),
          ],
        );
      },
    );
  }
}

class _DeviceRow extends StatelessWidget {
  const _DeviceRow({
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
        Icon(icon, size: 18, color: theme.colorScheme.onSurfaceVariant),
        const SizedBox(width: 8),
        Text(label, style: theme.textTheme.bodySmall),
        const Spacer(),
        Flexible(
          child: Text(
            value,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
              fontWeight: FontWeight.w500,
            ),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.label, required this.active});
  final String label;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Align(
      alignment: Alignment.centerRight,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
        decoration: BoxDecoration(
          color: active
              ? AppColors.success.withValues(alpha: 0.15)
              : theme.colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(4),
        ),
        child: Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: active ? AppColors.success : theme.colorScheme.outline,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}
