import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_dimensions.dart';
import '../../../domain/entities/quick_card.dart';
import '../../../domain/repositories/settings_repository.dart';
import '../../../injection.dart';
import '../../cubits/quick_card/quick_card_cubit.dart';
import '../../cubits/quick_card/quick_card_state.dart';
import 'widgets/card_display.dart';
import 'widgets/page_dots.dart';

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
        if (state.loading) {
          return const Center(child: CircularProgressIndicator());
        }
        if (state.cards.isEmpty) {
          return QuickCardEmptyState(onAdd: () => _showAddMenu(context));
        }
        return Column(
          children: [
            _TopActions(onAdd: () => _showAddMenu(context)),
            Expanded(
              child: _CardPageView(
                cards: state.cards,
                selectedIndex: state.selectedIndex,
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(bottom: AppDimensions.spacingLg),
              child: PageDots(
                count: state.cards.length,
                current: state.selectedIndex,
              ),
            ),
          ],
        );
      },
    );
  }

  void _showAddMenu(BuildContext context) {
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

class _TopActions extends StatelessWidget {
  const _TopActions({required this.onAdd});
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppDimensions.spacingLg,
        vertical: AppDimensions.spacingSm,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          TextButton.icon(
            onPressed: onAdd,
            icon: const Icon(Icons.add, size: 18),
            label: const Text('新建'),
          ),
          const SizedBox(width: AppDimensions.spacingSm),
          TextButton.icon(
            onPressed: () {
              // TODO: implement QR scan
            },
            icon: const Icon(Icons.qr_code_scanner, size: 18),
            label: const Text('扫描'),
          ),
        ],
      ),
    );
  }
}

class _CardPageView extends StatefulWidget {
  const _CardPageView({
    required this.cards,
    required this.selectedIndex,
  });

  final List<QuickCard> cards;
  final int selectedIndex;

  @override
  State<_CardPageView> createState() => _CardPageViewState();
}

class _CardPageViewState extends State<_CardPageView> {
  late PageController _controller;

  @override
  void initState() {
    super.initState();
    _controller = PageController(initialPage: widget.selectedIndex);
  }

  @override
  void didUpdateWidget(_CardPageView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.cards.length != widget.cards.length) {
      final target = (widget.cards.length - 1).clamp(0, widget.cards.length);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_controller.hasClients && target < widget.cards.length) {
          _controller.animateToPage(
            target,
            duration: const Duration(milliseconds: 300),
            curve: Curves.easeInOut,
          );
        }
      });
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return PageView.builder(
      controller: _controller,
      itemCount: widget.cards.length,
      onPageChanged: (i) => context.read<QuickCardCubit>().selectCard(i),
      itemBuilder: (_, i) {
        final card = widget.cards[i];
        return CardDisplay(
          card: card,
          onEdit: () => _editCard(context, card),
        );
      },
    );
  }

  void _editCard(BuildContext context, QuickCard card) {
    final cubit = context.read<QuickCardCubit>();
    final titleCtrl = TextEditingController(text: card.title);
    final contentCtrl = TextEditingController(text: card.content);
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('编辑卡片'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: titleCtrl,
              decoration: const InputDecoration(
                labelText: '标题',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: AppDimensions.spacingSm),
            TextField(
              controller: contentCtrl,
              decoration: const InputDecoration(
                labelText: '内容',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () {
              cubit.updateCard(card.copyWith(
                title: titleCtrl.text,
                content: contentCtrl.text,
              ));
              Navigator.pop(ctx);
            },
            child: const Text('保存'),
          ),
          TextButton(
            onPressed: () {
              cubit.removeCard(card.id);
              Navigator.pop(ctx);
            },
            style: TextButton.styleFrom(
              foregroundColor: Theme.of(ctx).colorScheme.error,
            ),
            child: const Text('删除'),
          ),
        ],
      ),
    );
  }
}

