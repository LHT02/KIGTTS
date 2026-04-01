import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_dimensions.dart';
import '../../../domain/entities/quick_card.dart';
import '../../../domain/repositories/settings_repository.dart';
import '../../../injection.dart';
import '../../cubits/quick_card/quick_card_cubit.dart';
import '../../cubits/quick_card/quick_card_state.dart';

/// Quick card page with image/QR/text cards (P1).
class QuickCardPage extends StatelessWidget {
  const QuickCardPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => QuickCardCubit(
        settingsRepository: getIt<SettingsRepository>(),
      )..initialize(),
      child: const _QuickCardView(),
    );
  }
}

class _QuickCardView extends StatelessWidget {
  const _QuickCardView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<QuickCardCubit, QuickCardState>(
      builder: (context, state) {
        if (state.cards.isEmpty) {
          return _EmptyCardState(onAdd: () => _addCard(context));
        }
        return Stack(
          children: [
            PageView.builder(
              itemCount: state.cards.length,
              onPageChanged: (i) =>
                  context.read<QuickCardCubit>().selectCard(i),
              itemBuilder: (_, i) => _CardView(card: state.cards[i]),
            ),
            Positioned(
              right: 16,
              bottom: 16,
              child: FloatingActionButton(
                heroTag: 'quickCardFab',
                onPressed: () => _addCard(context),
                child: const Icon(Icons.add),
              ),
            ),
          ],
        );
      },
    );
  }

  void _addCard(BuildContext context) {
    final cubit = context.read<QuickCardCubit>();
    showModalBottomSheet(
      context: context,
      builder: (sheetCtx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.text_fields),
              title: const Text('文本卡片'),
              onTap: () {
                Navigator.pop(sheetCtx);
                cubit.addCard(QuickCard(
                  id: DateTime.now().millisecondsSinceEpoch.toString(),
                  type: QuickCardType.text,
                  title: '新文本卡片',
                  content: '',
                ));
              },
            ),
            ListTile(
              leading: const Icon(Icons.qr_code),
              title: const Text('二维码卡片'),
              onTap: () {
                Navigator.pop(sheetCtx);
                cubit.addCard(QuickCard(
                  id: DateTime.now().millisecondsSinceEpoch.toString(),
                  type: QuickCardType.qr,
                  title: '新二维码',
                  content: '',
                ));
              },
            ),
            ListTile(
              leading: const Icon(Icons.image),
              title: const Text('图片卡片'),
              onTap: () {
                Navigator.pop(sheetCtx);
                cubit.addCard(QuickCard(
                  id: DateTime.now().millisecondsSinceEpoch.toString(),
                  type: QuickCardType.image,
                  title: '新图片',
                ));
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyCardState extends StatelessWidget {
  const _EmptyCardState({required this.onAdd});
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.contact_page,
            size: 64,
            color: theme.colorScheme.outline.withValues(alpha: 0.3),
          ),
          const SizedBox(height: AppDimensions.spacingMd),
          Text(
            '暂无名片卡',
            style: theme.textTheme.bodyLarge?.copyWith(
              color: theme.colorScheme.outline,
            ),
          ),
          const SizedBox(height: AppDimensions.spacingLg),
          FilledButton.icon(
            onPressed: onAdd,
            icon: const Icon(Icons.add),
            label: const Text('添加卡片'),
          ),
        ],
      ),
    );
  }
}

class _CardView extends StatelessWidget {
  const _CardView({required this.card});
  final QuickCard card;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.all(AppDimensions.spacingLg),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(AppDimensions.spacingXl),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(card.title, style: theme.textTheme.titleLarge),
              const SizedBox(height: AppDimensions.spacingLg),
              Expanded(child: Center(child: _buildContent(theme))),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildContent(ThemeData theme) {
    return switch (card.type) {
      QuickCardType.text => Text(
          card.content,
          style: theme.textTheme.headlineMedium,
          textAlign: TextAlign.center,
        ),
      QuickCardType.qr => Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.qr_code_2, size: 120, color: AppColors.primary),
            const SizedBox(height: 8),
            Text(card.content, style: theme.textTheme.bodySmall),
          ],
        ),
      QuickCardType.image => card.imagePath != null
          ? Image.asset(card.imagePath!, fit: BoxFit.contain)
          : Icon(
              Icons.image,
              size: 120,
              color: theme.colorScheme.outline.withValues(alpha: 0.3),
            ),
    };
  }
}
