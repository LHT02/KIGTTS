import 'package:flutter/material.dart';
import 'running_strip.dart';
import 'side_drawer.dart';

/// Main application scaffold with side drawer and content area.
/// Implements the side drawer + top bar shell layout.
class AppScaffold extends StatefulWidget {
  const AppScaffold({super.key, required this.child});

  final Widget child;

  @override
  State<AppScaffold> createState() => _AppScaffoldState();
}

class _AppScaffoldState extends State<AppScaffold> {
  bool _drawerExpanded = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Auto-expand drawer in landscape
    final isLandscape =
        MediaQuery.of(context).orientation == Orientation.landscape;
    if (isLandscape && !_drawerExpanded) {
      _drawerExpanded = true;
    }
  }

  void _toggleDrawer() {
    setState(() => _drawerExpanded = !_drawerExpanded);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Row(
          children: [
            SideDrawer(
              expanded: _drawerExpanded,
              onToggle: _toggleDrawer,
            ),
            Expanded(
              child: Column(
                children: [
                  const RunningStrip(),
                  Expanded(child: widget.child),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
