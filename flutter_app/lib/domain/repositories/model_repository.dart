import '../entities/voice_pack_info.dart';
import '../entities/asr_model_info.dart';

/// Repository interface for voice pack and ASR model management.
abstract class ModelRepository {
  /// List all imported voice packs, sorted by pinned > order > name.
  Future<List<VoicePackInfo>> listVoicePacks();

  /// List all ASR models.
  Future<List<AsrModelInfo>> listAsrModels();

  /// Import a voice pack from file path. Returns the new pack's dir name.
  Future<String> importVoice(String filePath);

  /// Import an ASR model from file path. Returns the model's dir name.
  Future<String> importAsr(String filePath);

  /// Delete a voice pack by directory name.
  Future<void> deleteVoicePack(String dirName);

  /// Update voice pack metadata.
  Future<void> updateVoiceMeta(String dirName, VoicePackMeta meta);

  /// Update voice pack avatar image.
  Future<void> updateVoiceAvatar(String dirName, String imagePath);

  /// Export a voice pack to destination path.
  Future<void> exportVoicePack(String dirName, String destPath);

  /// Ensure the bundled ASR model is extracted. Returns its directory path.
  Future<String?> ensureBundledAsr();

  /// Ensure the bundled voice pack is extracted. Returns its directory path.
  Future<String?> ensureBundledVoice();

  /// Get the last selected voice pack name.
  Future<String?> getLastVoiceName();

  /// Save the last selected voice pack name.
  Future<void> setLastVoiceName(String name);

  /// Get the last selected ASR model name.
  Future<String?> getLastAsrName();

  /// Save the last selected ASR model name.
  Future<void> setLastAsrName(String name);

  /// Get system TTS ordering timestamp.
  Future<int?> getSystemTtsOrder();

  /// Save system TTS ordering timestamp.
  Future<void> setSystemTtsOrder(int order);
}
