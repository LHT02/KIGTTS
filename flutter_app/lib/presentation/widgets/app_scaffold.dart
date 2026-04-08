import 'package:flutter/material.dart';
import '../../core/router/app_router.dart';
import '../../core/theme/app_dimensions.dart';
import 'running_strip_panel.dart';
import 'running_strip_toggle.dart';
import 'side_drawer.dart';
import 'top_bar.dart';
import 'top_bar_actions.dart';

/// Main application scaffold with responsive drawer and TopBar.
///
/// [currentPath] is passed from the ShellRoute builder to avoid
/// `_dependents.isEmpty` assertion failures during route transitions.
class AppScaffold extends StatefulWidget {
  const AppScaffold({
    super.key,
    required this.child,
    required this.currentPath,
  });

  final Widget child;
  final String currentPath;

  @override
  State<AppScaffold> createState() => _AppScaffoldState();
}

class _AppScaffoldState extends State<AppScaffold> {
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();
  bool _landscapeExpanded = false;
  bool _stripExpanded = false;

  String get _title {
    return switch (widget.currentPath) {
      AppRoutes.home => '便捷字幕',
      AppRoutes.overlay => '悬浮窗',
      AppRoutes.cards => '快捷名片',
      AppRoutes.voicepacks => '语音包',
      AppRoutes.drawing => '画板',
      AppRoutes.settings => '设置',
      AppRoutes.log => '日志',
      AppRoutes.realtime => '实时转换',
      _ => '',
    };
  }

  List<Widget> get _actions {
    return switch (widget.currentPath) {
      AppRoutes.home => const [FullscreenAction()],
      AppRoutes.settings => const [LogAction()],
      _ => const [],
    };
  }

  void _handleMenuPress() {
    final isLandscape =
        MediaQuery.orientationOf(context) == Orientation.landscape;
    if (isLandscape) {
      setState(() => _landscapeExpanded = !_landscapeExpanded);
    } else {
      _scaffoldKey.currentState?.openDrawer();
    }
  }

  @override
  Widget build(BuildContext context) {
    final isLandscape =
        MediaQuery.orientationOf(context) == Orientation.landscape;

    final isHome = widget.currentPath == AppRoutes.home;

    // RunningStripToggle is ALWAYS in the tree (Offstage when not
    // on home page) so its BlocBuilder subscriptions are never
    // torn down during route transitions — this prevents the
    // `_dependents.isEmpty` assertion.
    final toggle = Offstage(
      offstage: !isHome,
      child: RunningStripToggle(
        expanded: _stripExpanded,
        onToggle: () =>
            setState(() => _stripExpanded = !_stripExpanded),
      ),
    );

    if (isLandscape) return _buildLandscape(toggle);
    return _buildPortrait(toggle);
  }

  Widget _bodyWithStrip(Widget child) {
    return Column(
      children: [
        AnimatedSize(
          duration: const Duration(milliseconds: 220),
          curve: Curves.fastOutSlowIn,
          alignment: Alignment.topCenter,
          child: _stripExpanded
              ? RunningStripPanel(
                  onCollapse: () =>
                      setState(() => _stripExpanded = false),
                )
              : const SizedBox.shrink(),
        ),
        Expanded(child: child),
      ],
    );
  }

  Widget _buildPortrait(Widget toggle) {
    return Scaffold(
      key: _scaffoldKey,
      appBar: TopBar(
        title: _title,
        titleTrailing: toggle,
        actions: _actions,
        onMenuPressed: _handleMenuPress,
      ),
      drawer: Drawer(
        width: AppDimensions.drawerWidthExpanded,
        child: DrawerPanel(
          expanded: true,
          currentPath: widget.currentPath,
          onItemTap: () => _scaffoldKey.currentState?.closeDrawer(),
        ),
      ),
      body: SafeArea(
        top: false,
        child: _bodyWithStrip(widget.child),
      ),
    );
  }

  Widget _buildLandscape(Widget toggle) {
    final theme = Theme.of(context);
    return Scaffold(
      key: _scaffoldKey,
      appBar: TopBar(
        title: _title,
        titleTrailing: toggle,
        actions: _actions,
        onMenuPressed: _handleMenuPress,
        landscapeExpanded: _landscapeExpanded,
      ),
      body: SafeArea(
        top: false,
        child: Row(
          children: [
            SizedBox(
              width: 80,
              child: DrawerPanel(
                expanded: false,
                currentPath: widget.currentPath,
                onItemTap: () {
                  if (_landscapeExpanded) {
                    setState(() => _landscapeExpanded = false);
                  }
                },
              ),
            ),
            VerticalDivider(
              width: 1,
              thickness: 1,
              color: theme.colorScheme.outline.withValues(alpha: 0.2),
            ),
            Expanded(
              child: Stack(
                children: [
                  _bodyWithStrip(widget.child),
                  if (_landscapeExpanded) ...[
                    Positioned.fill(
                      child: GestureDetector(
                        onTap: () =>
                            setState(() => _landscapeExpanded = false),
                        child: Container(
                          color: theme.brightness == Brightness.dark
                              ? Colors.black54
                              : Colors.black26,
                        ),
                      ),
                    ),
                    Positioned(
                      left: 0,
                      top: 0,
                      bottom: 0,
                      width: 256,
                      child: Material(
                        elevation: 8,
                        child: DrawerPanel(
                          expanded: true,
                          currentPath: widget.currentPath,
                          onItemTap: () =>
                              setState(() => _landscapeExpanded = false),
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
