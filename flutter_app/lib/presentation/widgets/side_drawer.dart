import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:go_router/go_router.dart';
import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_dimensions.dart';

/// Reusable drawer panel content used in both portrait Drawer
/// and landscape rail/expanded overlay.
class DrawerPanel extends StatelessWidget {
  const DrawerPanel({
    super.key,
    required this.expanded,
    required this.currentPath,
    this.onItemTap,
  });

  final bool expanded;
  final String currentPath;
  final VoidCallback? onItemTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      color: theme.colorScheme.surface,
      child: SafeArea(
        right: false,
        child: Column(
          children: [
            // Header
            if (expanded)
              Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppDimensions.spacingLg,
                  vertical: AppDimensions.spacingSm,
                ),
                child: SizedBox(
                  height: 44,
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: SvgPicture.asset(
                      theme.brightness == Brightness.dark
                          ? 'assets/images/logo_white.svg'
                          : 'assets/images/logo_black.svg',
                      fit: BoxFit.contain,
                    ),
                  ),
                ),
              )
            else
              const SizedBox(height: AppDimensions.spacingLg),
            // Menu items
            Expanded(
              child: ListView(
                padding: EdgeInsets.zero,
                children: [
                  _DrawerItem(
                    icon: Icons.subtitles_sharp,
                    label: '便捷字幕',
                    path: AppRoutes.home,
                    currentPath: currentPath,
                    expanded: expanded,
                    onItemTap: onItemTap,
                  ),
                  _DrawerItem(
                    icon: Icons.contact_page_sharp,
                    label: '快捷页面',
                    path: AppRoutes.cards,
                    currentPath: currentPath,
                    expanded: expanded,
                    onItemTap: onItemTap,
                  ),
                  _DrawerItem(
                    icon: Icons.brush_sharp,
                    label: '画板',
                    path: AppRoutes.drawing,
                    currentPath: currentPath,
                    expanded: expanded,
                    onItemTap: onItemTap,
                  ),
                  _DrawerItem(
                    icon: Icons.tune_sharp,
                    label: '设置',
                    path: AppRoutes.settings,
                    currentPath: currentPath,
                    expanded: expanded,
                    onItemTap: onItemTap,
                  ),
                ],
              ),
            ),
          ],
        ),
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
    this.onItemTap,
  });

  final IconData icon;
  final String label;
  final String path;
  final String currentPath;
  final bool expanded;
  final VoidCallback? onItemTap;

  bool get _isSelected => currentPath == path;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    const selectedColor = AppColors.primary;
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
        borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
        child: InkWell(
          borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
          onTap: () {
            context.go(path);
            onItemTap?.call();
          },
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
                      fontWeight: _isSelected
                          ? FontWeight.w600
                          : FontWeight.normal,
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
