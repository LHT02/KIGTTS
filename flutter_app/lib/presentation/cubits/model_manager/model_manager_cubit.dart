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
  })  : _modelRepo = modelRepository,
        _realtimeRepo = realtimeRepository,
        super(const ModelManagerState());

  final ModelRepository _modelRepo;
  final RealtimeRepository _realtimeRepo;

  /// Load all voice packs and ASR models.
  Future<void> loadAll() async {
    emit(state.copyWith(loading: true));
    try {
      final packs = await _modelRepo.listVoicePacks();
      final asrModels = await _modelRepo.listAsrModels();
      final lastVoice = await _modelRepo.getLastVoiceName();
      emit(state.copyWith(
        voicePacks: packs,
        asrModels: asrModels,
        currentVoiceDirName: lastVoice,
        loading: false,
      ));
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
      await _modelRepo.importAsr(filePath);
      await loadAll();
    } catch (e) {
      emit(state.copyWith(importing: false, importError: e.toString()));
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
      final pack = state.voicePacks.firstWhere(
        (p) => p.dirName == dirName,
      );
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
      final pack = state.voicePacks.firstWhere(
        (p) => p.dirName == dirName,
      );
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

  /// Clear error.
  void clearError() {
    emit(state.copyWith(importError: null));
  }
}
