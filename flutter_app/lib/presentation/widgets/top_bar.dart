import 'package:flutter/material.dart';
import '../../core/theme/app_dimensions.dart';

/// Top application bar with context-sensitive actions.
class TopBar extends StatelessWidget implements PreferredSizeWidget {
  const TopBar({
    super.key,
    this.title,
    this.actions,
    this.leading,
    this.solid = true,
  });

  final String? title;
  final List<Widget>? actions;
  final Widget? leading;
  final bool solid;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return AppBar(
      title: title != null
          ? Text(
              title!,
              style: theme.textTheme.titleMedium,
            )
          : null,
      leading: leading,
      actions: actions,
      elevation: solid ? AppDimensions.elevationTopBar : 0,
      backgroundColor: solid
          ? theme.colorScheme.surface
          : Colors.transparent,
    );
  }
}
