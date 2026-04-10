import 'package:flutter/material.dart';

/// Dropdown selector for audio device type.
/// Used in overlay control page for input/output device selection.
class DeviceDropdown extends StatelessWidget {
  const DeviceDropdown({
    super.key,
    required this.label,
    required this.icon,
    required this.value,
    required this.items,
    required this.onChanged,
  });

  final String label;
  final IconData icon;
  final int value;
  final Map<int, String> items;
  final ValueChanged<int?> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Icon(icon, size: 20, color: theme.colorScheme.onSurfaceVariant),
        const SizedBox(width: 8),
        Expanded(
          child: Text(label, style: theme.textTheme.bodyMedium),
        ),
        DropdownButton<int>(
          value: items.containsKey(value) ? value : items.keys.first,
          underline: const SizedBox.shrink(),
          isDense: true,
          menuMaxHeight: 200,
          items: items.entries
              .map(
                (e) => DropdownMenuItem<int>(
                  value: e.key,
                  child: Text(
                    e.value,
                    style: theme.textTheme.bodySmall,
                  ),
                ),
              )
              .toList(),
          onChanged: onChanged,
        ),
      ],
    );
  }
}
