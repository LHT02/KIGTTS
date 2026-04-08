import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:file_picker/file_picker.dart';
import '../../../injection.dart';
import '../../cubits/model_manager/model_manager_cubit.dart';
import '../../cubits/model_manager/model_manager_state.dart';
import 'widgets/voice_pack_card.dart';
import 'widgets/voice_pack_detail_dialog.dart';
import 'widgets/asr_model_section.dart';

/// Voice pack and ASR model management page (P0).
class ModelManagerPage extends StatelessWidget {
  const ModelManagerPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => getIt<ModelManagerCubit>()..loadAll(),
      child: const _ModelManagerView(),
    );
  }
}

class _ModelManagerView extends StatefulWidget {
  const _ModelManagerView();

  @override
  State<_ModelManagerView> createState() => _ModelManagerViewState();
}

class _ModelManagerViewState extends State<_ModelManagerView>
    with SingleTickerProviderStateMixin {
  late AnimationController _staggerController;
  bool _hasAnimated = false;

  @override
  void initState() {
    super.initState();
    _staggerController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
  }

  @override
  void dispose() {
    _staggerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<ModelManagerCubit, ModelManagerState>(
      listenWhen: (p, c) => !p.loading && c.loading == false && !_hasAnimated,
      listener: (context, state) {
        if (!_hasAnimated && state.voicePacks.isNotEmpty) {
          _hasAnimated = true;
          _staggerController.forward();
        }
      },
      builder: (context, state) {
        if (state.loading) {
          return const Center(child: CircularProgressIndicator());
        }
        if (!_hasAnimated && state.voicePacks.isNotEmpty) {
          _hasAnimated = true;
          _staggerController.forward();
        }
        return Stack(
          children: [
            Column(
              children: [
                const AsrModelSection(),
                _VoicePackHeader(count: state.voicePacks.length),
                Expanded(
                  child: _VoicePackList(
                    packs: state.voicePacks,
                    currentDirName: state.currentVoiceDirName,
                    staggerController: _staggerController,
                  ),
                ),
              ],
            ),
            Positioned(
              right: 16,
              bottom: 16,
              child: _ImportFab(importing: state.importing),
            ),
          ],
        );
      },
    );
  }
}

class _VoicePackHeader extends StatelessWidget {
  const _VoicePackHeader({required this.count});
  final int count;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Align(
        alignment: Alignment.centerLeft,
        child: Text(
          '语音包 ($count)',
          style: theme.textTheme.titleSmall?.copyWith(
            color: theme.colorScheme.primary,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}

class _VoicePackList extends StatelessWidget {
  const _VoicePackList({
    required this.packs,
    required this.currentDirName,
    required this.staggerController,
  });

  final List packs;
  final String? currentDirName;
  final AnimationController staggerController;

  @override
  Widget build(BuildContext context) {
    final cubit = context.read<ModelManagerCubit>();
    return ReorderableListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      itemCount: packs.length,
      onReorder: (oldIndex, newIndex) {
        // TODO: implement reorder persistence in cubit
      },
      proxyDecorator: _proxyDecorator,
      itemBuilder: (context, index) {
        final pack = packs[index];
        final isSelected = pack.dirName == currentDirName;
        final delay = (index * 0.1).clamp(0.0, 0.9);
        final animation = CurvedAnimation(
          parent: staggerController,
          curve: Interval(delay, delay + 0.4, curve: Curves.easeOut),
        );
        return _AnimatedCard(
          key: ValueKey(pack.dirName),
          animation: animation,
          child: VoicePackCard(
            pack: pack,
            isSelected: isSelected,
            onTap: () => _showDetail(context, cubit, pack.dirName),
          ),
        );
      },
    );
  }

  Widget _proxyDecorator(Widget child, int index, Animation<double> anim) {
    return AnimatedBuilder(
      animation: anim,
      builder: (ctx, ch) => Material(
        elevation: 4 * anim.value,
        borderRadius: BorderRadius.circular(4),
        child: ch,
      ),
      child: child,
    );
  }

  void _showDetail(
    BuildContext context,
    ModelManagerCubit cubit,
    String dirName,
  ) {
    showDialog(
      context: context,
      builder: (_) => BlocProvider.value(
        value: cubit,
        child: VoicePackDetailDialog(dirName: dirName),
      ),
    );
  }
}

class _AnimatedCard extends StatelessWidget {
  const _AnimatedCard({
    super.key,
    required this.animation,
    required this.child,
  });

  final Animation<double> animation;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: animation,
      child: SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0, 0.15),
          end: Offset.zero,
        ).animate(animation),
        child: child,
      ),
    );
  }
}

class _ImportFab extends StatelessWidget {
  const _ImportFab({required this.importing});
  final bool importing;

  @override
  Widget build(BuildContext context) {
    if (importing) {
      return const FloatingActionButton(
        heroTag: 'importFab',
        onPressed: null,
        child: SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(
            strokeWidth: 2,
            color: Colors.white,
          ),
        ),
      );
    }
    return FloatingActionButton(
      heroTag: 'importFab',
      onPressed: () => _showImportMenu(context),
      child: const Icon(Icons.add),
    );
  }

  void _showImportMenu(BuildContext context) {
    final cubit = context.read<ModelManagerCubit>();
    showModalBottomSheet(
      context: context,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.record_voice_over),
              title: const Text('导入语音包 (.zip)'),
              onTap: () async {
                Navigator.pop(sheetContext);
                final result = await FilePicker.platform.pickFiles(
                  type: FileType.custom,
                  allowedExtensions: ['zip'],
                );
                if (result != null && result.files.single.path != null) {
                  cubit.importVoice(result.files.single.path!);
                }
              },
            ),
            ListTile(
              leading: const Icon(Icons.hearing),
              title: const Text('导入 ASR 模型 (.zip)'),
              onTap: () async {
                Navigator.pop(sheetContext);
                final result = await FilePicker.platform.pickFiles(
                  type: FileType.custom,
                  allowedExtensions: ['zip'],
                );
                if (result != null && result.files.single.path != null) {
                  cubit.importAsr(result.files.single.path!);
                }
              },
            ),
          ],
        ),
      ),
    );
  }
}
