import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/entities/voice_pack_info.dart';
import '../../../domain/repositories/model_repository.dart';
import '../../../domain/repositories/realtime_repository.dart';
import 'model_manager_state.dart';

/// Cubit managing voice pack and ASR model state.
class ModelManagerCubit extends Cubit<ModelManagerState> {
  ModelManagerCubit({
    required ModelRepository modelRepository,
    required RealtimeRepository realtimeRepository,
  }) : _modelRepo = modelRepository,
       _realtimeRepo = realtimeRepository,
       super(const ModelManagerState());

  final ModelRepository _modelRepo;
  final RealtimeRepository _realtimeRepo;
  static const _systemTtsDirName = 'system_tts';

  /// Load all voice packs and ASR models.
  Future<void> loadAll() async {
    emit(state.copyWith(loading: true));
    try {
      final packs = await _modelRepo.listVoicePacks();
      final asrModels = await _modelRepo.listAsrModels();
      final lastVoice = await _modelRepo.getLastVoiceName();
      final systemTtsOrder = await _modelRepo.getSystemTtsOrder();
      final sortedPacks = [...packs]
        ..sort((a, b) {
          if (a.meta.pinned != b.meta.pinned) {
            return a.meta.pinned ? -1 : 1;
          }
          final aOrder =
              (a.dirName == _systemTtsDirName && systemTtsOrder != null)
              ? systemTtsOrder
              : a.meta.order;
          final bOrder =
              (b.dirName == _systemTtsDirName && systemTtsOrder != null)
              ? systemTtsOrder
              : b.meta.order;
          final byOrder = aOrder.compareTo(bOrder);
          if (byOrder != 0) return byOrder;
          return a.meta.name.compareTo(b.meta.name);
        });
      final lastAsr = await _modelRepo.getLastAsrName();
      final hasLastAsr =
          lastAsr != null && asrModels.any((m) => m.dirName == lastAsr);
      final currentAsr = hasLastAsr
          ? lastAsr
          : (asrModels.isNotEmpty ? asrModels.first.dirName : null);
      if (!hasLastAsr && currentAsr != null) {
        await _modelRepo.setLastAsrName(currentAsr);
      }
      emit(
        state.copyWith(
          voicePacks: sortedPacks,
          asrModels: asrModels,
          currentVoiceDirName: lastVoice,
          currentAsrDirName: currentAsr,
          loading: false,
        ),
      );
    } catch (e) {
      emit(state.copyWith(loading: false, importError: e.toString()));
    }
  }

  /// Import a voice pack from file.
  Future<void> importVoice(String filePath) async {
    emit(state.copyWith(importing: true, importError: null));
    try {
      await _modelRepo.importVoice(filePath);
      await loadAll();
    } catch (e) {
      emit(state.copyWith(importing: false, importError: e.toString()));
    }
  }

  /// Import an ASR model from file.
  Future<void> importAsr(String filePath) async {
    emit(state.copyWith(importing: true, importError: null));
    try {
      final dirName = await _modelRepo.importAsr(filePath);
      await loadAll();
      final imported = state.asrModels.where((m) => m.dirName == dirName);
      if (imported.isNotEmpty) {
        await selectAsr(imported.first.dirPath, imported.first.dirName);
      }
    } catch (e) {
      emit(state.copyWith(importing: false, importError: e.toString()));
    }
  }

  /// Select and load an ASR model.
  Future<void> selectAsr(String dirPath, String dirName) async {
    try {
      final ok = await _realtimeRepo.loadAsr(dirPath);
      if (ok) {
        await _modelRepo.setLastAsrName(dirName);
        emit(state.copyWith(currentAsrDirName: dirName));
      }
    } catch (e) {
      emit(state.copyWith(importError: e.toString()));
    }
  }

  /// Select and load a voice pack.
  Future<void> selectVoice(VoicePackInfo pack) async {
    try {
      final ok = await _realtimeRepo.loadVoice(pack.dirPath);
      if (ok) {
        await _modelRepo.setLastVoiceName(pack.dirName);
        emit(state.copyWith(currentVoiceDirName: pack.dirName));
      }
    } catch (e) {
      emit(state.copyWith(importError: e.toString()));
    }
  }

  /// Delete a voice pack.
  Future<void> deleteVoice(String dirName) async {
    try {
      await _modelRepo.deleteVoicePack(dirName);
      await loadAll();
    } catch (e) {
      emit(state.copyWith(importError: e.toString()));
    }
  }

  /// Toggle pin status of a voice pack.
  Future<void> togglePin(VoicePackInfo pack) async {
    try {
      final updatedMeta = pack.meta.copyWith(pinned: !pack.meta.pinned);
      await _modelRepo.updateVoiceMeta(pack.dirName, updatedMeta);
      await loadAll();
    } catch (e) {
      emit(state.copyWith(importError: e.toString()));
    }
  }

  /// Update voice pack name.
  Future<void> updateName(String dirName, String newName) async {
    try {
      final pack = state.voicePacks.firstWhere((p) => p.dirName == dirName);
      final updatedMeta = pack.meta.copyWith(name: newName);
      await _modelRepo.updateVoiceMeta(dirName, updatedMeta);
      await loadAll();
    } catch (e) {
      emit(state.copyWith(importError: e.toString()));
    }
  }

  /// Update voice pack remark.
  Future<void> updateRemark(String dirName, String remark) async {
    try {
      final pack = state.voicePacks.firstWhere((p) => p.dirName == dirName);
      final updatedMeta = pack.meta.copyWith(remark: remark);
      await _modelRepo.updateVoiceMeta(dirName, updatedMeta);
      await loadAll();
    } catch (e) {
      emit(state.copyWith(importError: e.toString()));
    }
  }

  /// Update voice pack avatar.
  Future<void> updateAvatar(String dirName, String imagePath) async {
    try {
      await _modelRepo.updateVoiceAvatar(dirName, imagePath);
      await loadAll();
    } catch (e) {
      emit(state.copyWith(importError: e.toString()));
    }
  }

  /// Export a voice pack.
  Future<void> exportVoice(String dirName, String destPath) async {
    try {
      await _modelRepo.exportVoicePack(dirName, destPath);
    } catch (e) {
      emit(state.copyWith(importError: e.toString()));
    }
  }

  /// Persist a reordered voice-pack list.
  Future<void> reorderVoicePacks(int oldIndex, int newIndex) async {
    final current = [...state.voicePacks];
    if (current.isEmpty) return;
    if (oldIndex < 0 || oldIndex >= current.length) return;
    if (newIndex < 0 || newIndex > current.length) return;

    if (newIndex > oldIndex) {
      newIndex -= 1;
    }
    if (newIndex == oldIndex) return;

    final moved = current.removeAt(oldIndex);
    current.insert(newIndex, moved);

    try {
      for (var i = 0; i < current.length; i++) {
        final pack = current[i];
        if (pack.dirName == _systemTtsDirName) {
          await _modelRepo.setSystemTtsOrder(i);
        } else {
          await _modelRepo.updateVoiceMeta(
            pack.dirName,
            pack.meta.copyWith(order: i),
          );
        }
      }
      emit(state.copyWith(voicePacks: current));
      await loadAll();
    } catch (e) {
      emit(state.copyWith(importError: e.toString()));
    }
  }

  /// Clear error.
  void clearError() {
    emit(state.copyWith(importError: null));
  }
}
