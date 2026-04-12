import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../core/router/app_router.dart';

/// Global notifier that fires when the fullscreen button is tapped.
///
/// The AppBar's FullscreenAction bumps the counter; QuickSubtitlePage
/// listens and opens the fullscreen dialog.  We use a simple counter
/// (ValueNotifier of int) so each tap is always detected as a change.
final fullscreenActionNotifier = ValueNotifier<int>(0);

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
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('日志已复制到剪贴板')),
        );
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
