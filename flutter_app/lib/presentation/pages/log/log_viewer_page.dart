import 'package:flutter/material.dart';
import '../../../core/theme/app_dimensions.dart';

/// Application log viewer page (P2).
class LogViewerPage extends StatefulWidget {
  const LogViewerPage({super.key});

  @override
  State<LogViewerPage> createState() => _LogViewerPageState();
}

class _LogViewerPageState extends State<LogViewerPage> {
  String _logs = '日志加载中...';

  @override
  void initState() {
    super.initState();
    // TODO: Load logs from native via repository layer
    setState(() => _logs =
        '日志功能将在后续版本中完善。\n\n当前版本暂不支持从 Flutter 端读取原生日志。');
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.all(AppDimensions.spacingLg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.article),
              const SizedBox(width: 8),
              Text('应用日志', style: theme.textTheme.titleMedium),
            ],
          ),
          const SizedBox(height: AppDimensions.spacingMd),
          Expanded(
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.all(AppDimensions.spacingMd),
              decoration: BoxDecoration(
                color: theme.colorScheme.surfaceContainerHighest,
                borderRadius:
                    BorderRadius.circular(AppDimensions.radiusMedium),
              ),
              child: SingleChildScrollView(
                child: SelectableText(
                  _logs,
                  style: theme.textTheme.bodySmall?.copyWith(
                    fontFamily: 'monospace',
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
