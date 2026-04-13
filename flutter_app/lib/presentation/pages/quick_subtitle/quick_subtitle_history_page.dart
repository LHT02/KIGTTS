import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../../../core/constants/prefs_keys.dart';
import '../../../domain/repositories/settings_repository.dart';
import '../../../injection.dart';

class QuickSubtitleHistoryPage extends StatefulWidget {
  const QuickSubtitleHistoryPage({super.key});

  @override
  State<QuickSubtitleHistoryPage> createState() =>
      _QuickSubtitleHistoryPageState();
}

class _QuickSubtitleHistoryPageState extends State<QuickSubtitleHistoryPage> {
  final SettingsRepository _settingsRepo = getIt<SettingsRepository>();

  bool _loading = true;
  List<String> _history = const [];

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  Future<void> _loadHistory() async {
    try {
      final raw = await _settingsRepo.getJsonSetting(
        PrefsKeys.quickSubtitleHistory,
      );
      final list = raw == null || raw.isEmpty
          ? <String>[]
          : (jsonDecode(raw) as List<dynamic>)
                .whereType<String>()
                .where((e) => e.trim().isNotEmpty)
                .toList();
      if (!mounted) return;
      setState(() {
        _history = list;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _history = const [];
        _loading = false;
      });
    }
  }

  Future<void> _persist() async {
    await _settingsRepo.setJsonSetting(
      PrefsKeys.quickSubtitleHistory,
      jsonEncode(_history),
    );
  }

  Future<void> _removeAt(int index) async {
    if (index < 0 || index >= _history.length) return;
    final next = [..._history]..removeAt(index);
    setState(() => _history = next);
    await _persist();
  }

  Future<void> _clearAll() async {
    setState(() => _history = const []);
    await _persist();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_history.isEmpty) {
      return Center(
        child: Text(
          '暂无历史记录',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
      );
    }

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
      child: Column(
        children: [
          Row(
            children: [
              IconButton(
                tooltip: '返回',
                icon: const Icon(Icons.arrow_back_sharp, size: 20),
                onPressed: () {
                  if (context.canPop()) {
                    context.pop();
                  }
                },
              ),
              Text(
                '历史记录 (${_history.length})',
                style: theme.textTheme.titleMedium,
              ),
              const Spacer(),
              TextButton.icon(
                onPressed: _clearAll,
                icon: const Icon(Icons.delete_sweep_sharp, size: 18),
                label: const Text('清空'),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Expanded(
            child: ListView.separated(
              itemCount: _history.length,
              separatorBuilder: (context, index) => const SizedBox(height: 6),
              itemBuilder: (context, index) {
                final text = _history[index];
                return Card(
                  child: ListTile(
                    title: Text(
                      text,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    onTap: () => context.pop(text),
                    onLongPress: () async {
                      await Clipboard.setData(ClipboardData(text: text));
                      if (!context.mounted) return;
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('已复制到剪贴板'),
                          duration: Duration(seconds: 1),
                        ),
                      );
                    },
                    trailing: IconButton(
                      tooltip: '删除',
                      icon: const Icon(Icons.delete_outline_sharp, size: 18),
                      onPressed: () => _removeAt(index),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
