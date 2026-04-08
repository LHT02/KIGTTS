import '../entities/asr_model_info.dart';
import '../entities/voice_pack_info.dart';
import '../repositories/model_repository.dart';

/// List all imported voice packs.
class ListVoicePacksUseCase {
  ListVoicePacksUseCase(this._repository);
  final ModelRepository _repository;

  Future<List<VoicePackInfo>> call() => _repository.listVoicePacks();
}

/// Import a voice pack from file path.
class ImportVoicePackUseCase {
  ImportVoicePackUseCase(this._repository);
  final ModelRepository _repository;

  Future<String> call(String filePath) => _repository.importVoice(filePath);
}

/// Delete a voice pack by directory name.
class DeleteVoicePackUseCase {
  DeleteVoicePackUseCase(this._repository);
  final ModelRepository _repository;

  Future<void> call(String dirName) => _repository.deleteVoicePack(dirName);
}

/// List all ASR models.
class ListAsrModelsUseCase {
  ListAsrModelsUseCase(this._repository);
  final ModelRepository _repository;

  Future<List<AsrModelInfo>> call() => _repository.listAsrModels();
}

/// Import an ASR model from file path.
class ImportAsrUseCase {
  ImportAsrUseCase(this._repository);
  final ModelRepository _repository;

  Future<String> call(String filePath) => _repository.importAsr(filePath);
}
