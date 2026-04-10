import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../core/router/app_router.dart';

/// Fullscreen toggle action for the top bar.
/// Currently a placeholder that could toggle immersive mode.
class FullscreenAction extends StatelessWidget {
  const FullscreenAction({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.fullscreen_sharp, size: 22),
      onPressed: () {
        // TODO: implement fullscreen toggle
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
