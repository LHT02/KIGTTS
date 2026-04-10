import 'package:flutter/material.dart';
import '../../core/theme/app_dimensions.dart';

/// Top application bar with animated title crossfade and hamburger menu.
///
/// Original Android layout:
/// [☰] [title] [RunningStripToggle (optional)] [actions...]
///
/// [titleTrailing] sits in the title row — stable across page changes,
/// unlike [actions] which get recreated on route switches.
class TopBar extends StatelessWidget implements PreferredSizeWidget {
  const TopBar({
    super.key,
    this.title,
    this.actions,
    this.leading,
    this.titleTrailing,
    this.solid = true,
    this.onMenuPressed,
    this.landscapeExpanded = false,
  });

  final String? title;
  final List<Widget>? actions;
  final Widget? leading;

  /// Widget placed after the title text, inside the title area.
  /// Use for widgets with BlocBuilder subscriptions that must remain
  /// stable across page transitions (e.g. RunningStripToggle).
  final Widget? titleTrailing;

  final bool solid;
  final VoidCallback? onMenuPressed;
  final bool landscapeExpanded;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    final leadingWidget = leading ??
        (onMenuPressed != null
            ? IconButton(
                icon: Icon(
                  landscapeExpanded
                      ? Icons.menu_open_sharp
                      : Icons.menu_sharp,
                ),
                onPressed: onMenuPressed,
                tooltip: '菜单',
              )
            : null);

    // Title with optional trailing widget
    Widget titleWidget = AnimatedSwitcher(
      duration: const Duration(milliseconds: 140),
      switchInCurve: Curves.linear,
      switchOutCurve: Curves.linear,
      transitionBuilder: (child, animation) {
        return FadeTransition(opacity: animation, child: child);
      },
      child: Text(
        title ?? '',
        key: ValueKey<String>(title ?? ''),
        style: theme.textTheme.titleMedium,
      ),
    );

    if (titleTrailing != null) {
      titleWidget = Row(
        children: [
          titleWidget,
          const Spacer(),
          titleTrailing!,
        ],
      );
    }

    return AppBar(
      automaticallyImplyLeading: false,
      leading: leadingWidget,
      title: titleWidget,
      actions: [
        if (actions != null) ...actions!,
        const SizedBox(width: 4),
      ],
      elevation: solid ? AppDimensions.elevationTopBar : 0,
      backgroundColor:
          solid ? theme.colorScheme.surface : Colors.transparent,
    );
  }
}
