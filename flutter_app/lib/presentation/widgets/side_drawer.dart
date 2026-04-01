import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_dimensions.dart';

/// Side navigation drawer with expand/collapse support.
/// Portrait: collapsed (72dp mini), Landscape: expanded (216dp).
class SideDrawer extends StatelessWidget {
  const SideDrawer({
    super.key,
    required this.expanded,
    required this.onToggle,
  });

  final bool expanded;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    final currentPath = GoRouterState.of(context).uri.toString();
    final width = expanded
        ? AppDimensions.drawerWidthExpanded
        : AppDimensions.drawerWidthCollapsed;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      curve: Curves.easeInOut,
      width: width,
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          right: BorderSide(
            color: Theme.of(context).dividerTheme.color ??
                Theme.of(context)
                    .colorScheme
                    .outline
                    .withValues(alpha: 0.2),
          ),
        ),
      ),
      child: Column(
        children: [
          const SizedBox(height: AppDimensions.spacingLg),
          _DrawerHeader(expanded: expanded, onToggle: onToggle),
          const SizedBox(height: AppDimensions.spacingSm),
          Expanded(
            child: ListView(
              padding: EdgeInsets.zero,
              children: [
                _DrawerItem(
                  icon: Icons.graphic_eq,
                  label: '实时转换',
                  path: AppRoutes.realtime,
                  currentPath: currentPath,
                  expanded: expanded,
                ),
                _DrawerItem(
                  icon: Icons.subtitles,
                  label: '便捷字幕',
                  path: AppRoutes.subtitle,
                  currentPath: currentPath,
                  expanded: expanded,
                ),
                _DrawerItem(
                  icon: Icons.open_in_new,
                  label: '悬浮窗',
                  path: AppRoutes.overlay,
                  currentPath: currentPath,
                  expanded: expanded,
                ),
                _DrawerItem(
                  icon: Icons.contact_page,
                  label: '快捷名片',
                  path: AppRoutes.cards,
                  currentPath: currentPath,
                  expanded: expanded,
                ),
                _DrawerItem(
                  icon: Icons.record_voice_over,
                  label: '语音包',
                  path: AppRoutes.voicepacks,
                  currentPath: currentPath,
                  expanded: expanded,
                ),
                _DrawerItem(
                  icon: Icons.brush,
                  label: '画板',
                  path: AppRoutes.drawing,
                  currentPath: currentPath,
                  expanded: expanded,
                ),
                _DrawerItem(
                  icon: Icons.settings,
                  label: '设置',
                  path: AppRoutes.settings,
                  currentPath: currentPath,
                  expanded: expanded,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DrawerHeader extends StatelessWidget {
  const _DrawerHeader({required this.expanded, required this.onToggle});

  final bool expanded;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppDimensions.spacingSm,
      ),
      child: Row(
        children: [
          IconButton(
            icon: Icon(expanded ? Icons.menu_open : Icons.menu),
            onPressed: onToggle,
            tooltip: expanded ? '折叠' : '展开',
          ),
          if (expanded) ...[
            const SizedBox(width: AppDimensions.spacingSm),
            Text(
              'KIGTTS',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: AppColors.primary,
                  ),
            ),
          ],
        ],
      ),
    );
  }
}

class _DrawerItem extends StatelessWidget {
  const _DrawerItem({
    required this.icon,
    required this.label,
    required this.path,
    required this.currentPath,
    required this.expanded,
  });

  final IconData icon;
  final String label;
  final String path;
  final String currentPath;
  final bool expanded;

  bool get _isSelected => currentPath == path;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final selectedColor = AppColors.primary;
    final defaultColor = theme.colorScheme.onSurfaceVariant;
    final color = _isSelected ? selectedColor : defaultColor;

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppDimensions.spacingSm,
        vertical: 2,
      ),
      child: Material(
        color: _isSelected
            ? selectedColor.withValues(alpha: 0.12)
            : Colors.transparent,
        borderRadius: BorderRadius.circular(AppDimensions.radiusMedium),
        child: InkWell(
          borderRadius: BorderRadius.circular(AppDimensions.radiusMedium),
          onTap: () => context.go(path),
          child: Padding(
            padding: EdgeInsets.symmetric(
              horizontal: expanded ? AppDimensions.spacingMd : 0,
              vertical: AppDimensions.spacingMd,
            ),
            child: Row(
              mainAxisAlignment: expanded
                  ? MainAxisAlignment.start
                  : MainAxisAlignment.center,
              children: [
                Icon(icon, color: color, size: 24),
                if (expanded) ...[
                  const SizedBox(width: AppDimensions.spacingMd),
                  Text(
                    label,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: color,
                      fontWeight:
                          _isSelected ? FontWeight.w600 : FontWeight.normal,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
