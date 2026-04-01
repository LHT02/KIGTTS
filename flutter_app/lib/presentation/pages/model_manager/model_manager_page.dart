import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:file_picker/file_picker.dart';
import '../../../injection.dart';
import '../../cubits/model_manager/model_manager_cubit.dart';
import '../../cubits/model_manager/model_manager_state.dart';
import 'widgets/voice_pack_card.dart';
import 'widgets/voice_pack_detail_sheet.dart';
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

class _ModelManagerView extends StatelessWidget {
  const _ModelManagerView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<ModelManagerCubit, ModelManagerState>(
      builder: (context, state) {
        if (state.loading) {
          return const Center(child: CircularProgressIndicator());
        }
        return Stack(
          children: [
            CustomScrollView(
              slivers: [
                const SliverToBoxAdapter(child: AsrModelSection()),
                SliverPadding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  sliver: SliverToBoxAdapter(
                    child: Text(
                      '语音包 (${state.voicePacks.length})',
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                            color: Theme.of(context).colorScheme.primary,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                  ),
                ),
                SliverPadding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  sliver: SliverList.builder(
                    itemCount: state.voicePacks.length,
                    itemBuilder: (context, index) {
                      final pack = state.voicePacks[index];
                      final isSelected =
                          pack.dirName == state.currentVoiceDirName;
                      return VoicePackCard(
                        pack: pack,
                        isSelected: isSelected,
                        onTap: () => _showDetail(context, pack.dirName),
                        onSelect: () =>
                            context.read<ModelManagerCubit>().selectVoice(pack),
                      );
                    },
                  ),
                ),
                const SliverPadding(padding: EdgeInsets.only(bottom: 80)),
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

  void _showDetail(BuildContext context, String dirName) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (_) => BlocProvider.value(
        value: context.read<ModelManagerCubit>(),
        child: VoicePackDetailSheet(dirName: dirName),
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
