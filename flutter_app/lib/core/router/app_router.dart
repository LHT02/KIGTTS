import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../presentation/pages/realtime/realtime_page.dart';
import '../../presentation/pages/model_manager/model_manager_page.dart';
import '../../presentation/pages/settings/settings_page.dart';
import '../../presentation/pages/quick_subtitle/quick_subtitle_page.dart';
import '../../presentation/pages/quick_card/quick_card_page.dart';
import '../../presentation/pages/drawing/drawing_page.dart';
import '../../presentation/pages/overlay/overlay_control_page.dart';
import '../../presentation/pages/log/log_viewer_page.dart';
import '../../presentation/widgets/app_scaffold.dart';

/// Application route paths.
abstract final class AppRoutes {
  static const realtime = '/';
  static const subtitle = '/subtitle';
  static const overlay = '/overlay';
  static const cards = '/cards';
  static const voicepacks = '/voicepacks';
  static const drawing = '/drawing';
  static const settings = '/settings';
  static const log = '/settings/log';
}

/// GoRouter configuration with ShellRoute for persistent scaffold.
final GoRouter appRouter = GoRouter(
  initialLocation: AppRoutes.realtime,
  routes: [
    ShellRoute(
      builder: (context, state, child) => AppScaffold(child: child),
      routes: [
        GoRoute(
          path: AppRoutes.realtime,
          pageBuilder: (context, state) => _buildPage(
            key: state.pageKey,
            child: const RealtimePage(),
          ),
        ),
        GoRoute(
          path: AppRoutes.subtitle,
          pageBuilder: (context, state) => _buildPage(
            key: state.pageKey,
            child: const QuickSubtitlePage(),
          ),
        ),
        GoRoute(
          path: AppRoutes.overlay,
          pageBuilder: (context, state) => _buildPage(
            key: state.pageKey,
            child: const OverlayControlPage(),
          ),
        ),
        GoRoute(
          path: AppRoutes.cards,
          pageBuilder: (context, state) => _buildPage(
            key: state.pageKey,
            child: const QuickCardPage(),
          ),
        ),
        GoRoute(
          path: AppRoutes.voicepacks,
          pageBuilder: (context, state) => _buildPage(
            key: state.pageKey,
            child: const ModelManagerPage(),
          ),
        ),
        GoRoute(
          path: AppRoutes.drawing,
          pageBuilder: (context, state) => _buildPage(
            key: state.pageKey,
            child: const DrawingPage(),
          ),
        ),
        GoRoute(
          path: AppRoutes.settings,
          pageBuilder: (context, state) => _buildPage(
            key: state.pageKey,
            child: const SettingsPage(),
          ),
        ),
        GoRoute(
          path: AppRoutes.log,
          pageBuilder: (context, state) => _buildPage(
            key: state.pageKey,
            child: const LogViewerPage(),
          ),
        ),
      ],
    ),
  ],
);

/// Build a page with fade transition.
CustomTransitionPage<void> _buildPage({
  required LocalKey key,
  required Widget child,
}) {
  return CustomTransitionPage<void>(
    key: key,
    child: child,
    transitionsBuilder: (context, animation, secondaryAnimation, child) {
      return FadeTransition(
        opacity: CurveTween(curve: Curves.easeInOut).animate(animation),
        child: child,
      );
    },
    transitionDuration: const Duration(milliseconds: 200),
  );
}
