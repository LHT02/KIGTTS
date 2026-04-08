import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_dimensions.dart';
import '../../../../domain/entities/quick_card.dart';

/// Displays a single quick card based on its type (text, QR, image).
class CardDisplay extends StatelessWidget {
  const CardDisplay({
    super.key,
    required this.card,
    required this.onEdit,
  });

  final QuickCard card;
  final VoidCallback onEdit;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return GestureDetector(
      onTap: onEdit,
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppDimensions.spacingLg,
          vertical: AppDimensions.spacingSm,
        ),
        child: Card(
          elevation: AppDimensions.elevationCard,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppDimensions.radiusLarge),
          ),
          child: Padding(
            padding: const EdgeInsets.all(AppDimensions.spacingXl),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // Title
                Text(
                  card.title,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: AppDimensions.spacingMd),
                // Content area based on type
                Expanded(child: _buildContent(context)),
                const SizedBox(height: AppDimensions.spacingSm),
                // Type badge
                _TypeBadge(type: card.type),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context) {
    final theme = Theme.of(context);
    switch (card.type) {
      case QuickCardType.text:
        return Center(
          child: Text(
            card.content.isEmpty ? '点击编辑内容' : card.content,
            style: theme.textTheme.headlineSmall,
            textAlign: TextAlign.center,
          ),
        );
      case QuickCardType.qr:
        return Center(
          child: card.content.isEmpty
              ? Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.qr_code,
                      size: 64,
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '点击编辑二维码内容',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                )
              : Icon(
                  Icons.qr_code_2,
                  size: 120,
                  color: theme.colorScheme.onSurface,
                ),
        );
      case QuickCardType.image:
        return Center(
          child: card.imagePath != null
              ? ClipRRect(
                  borderRadius: BorderRadius.circular(
                    AppDimensions.radiusMedium,
                  ),
                  child: Image.asset(
                    card.imagePath!,
                    fit: BoxFit.contain,
                    errorBuilder: (_, e, st) => _imagePlaceholder(theme),
                  ),
                )
              : _imagePlaceholder(theme),
        );
    }
  }

  Widget _imagePlaceholder(ThemeData theme) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(
          Icons.image,
          size: 64,
          color: theme.colorScheme.onSurfaceVariant,
        ),
        const SizedBox(height: 8),
        Text(
          '点击编辑图片',
          style: theme.textTheme.bodySmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}

class _TypeBadge extends StatelessWidget {
  const _TypeBadge({required this.type});
  final QuickCardType type;

  @override
  Widget build(BuildContext context) {
    final (icon, label) = switch (type) {
      QuickCardType.text => (Icons.text_fields, '文本'),
      QuickCardType.qr => (Icons.qr_code, '二维码'),
      QuickCardType.image => (Icons.image, '图片'),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: AppColors.primary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: AppColors.primary),
          const SizedBox(width: 4),
          Text(
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: AppColors.primary,
                ),
          ),
        ],
      ),
    );
  }
}

/// Empty state shown when no quick cards exist.
class QuickCardEmptyState extends StatelessWidget {
  const QuickCardEmptyState({super.key, required this.onAdd});

  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.style_sharp,
            size: 64,
            color: theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.5),
          ),
          const SizedBox(height: AppDimensions.spacingLg),
          Text(
            '暂无卡片',
            style: theme.textTheme.titleMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: AppDimensions.spacingSm),
          Text(
            '点击下方按钮新建快捷名片',
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: AppDimensions.spacingXl),
          FilledButton.icon(
            onPressed: onAdd,
            icon: const Icon(Icons.add),
            label: const Text('新建卡片'),
          ),
        ],
      ),
    );
  }
}
