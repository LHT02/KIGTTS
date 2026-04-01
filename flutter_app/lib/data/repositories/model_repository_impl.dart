import '../../domain/entities/asr_model_info.dart';
import '../../domain/entities/voice_pack_info.dart';
import '../../domain/repositories/model_repository.dart';
import '../datasources/model_channel.dart';
import '../dto/voice_pack_dto.dart';

/// Implementation of ModelRepository using MethodChannel.
class ModelRepositoryImpl implements ModelRepository {
  ModelRepositoryImpl({required this.dataSource});

  final ModelChannelDataSource dataSource;

  @override
  Future<List<VoicePackInfo>> listVoicePacks() async {
    final maps = await dataSource.listVoicePacks();
    return maps.map(VoicePackDto.fromMap).toList();
  }

  @override
  Future<List<AsrModelInfo>> listAsrModels() async {
    final maps = await dataSource.listAsrModels();
    return maps
        .map((m) => AsrModelInfo(
              dirName: m['dirName'] as String,
              dirPath: m['dirPath'] as String,
              isBundled: m['isBundled'] as bool? ?? false,
            ))
        .toList();
  }

  @override
  Future<String> importVoice(String filePath) async {
    final result = await dataSource.importVoice(filePath);
    return result['dirName'] as String;
  }

  @override
  Future<String> importAsr(String filePath) async {
    final result = await dataSource.importAsr(filePath);
    return result['dirName'] as String;
  }

  @override
  Future<void> deleteVoicePack(String dirName) {
    return dataSource.deleteVoice(dirName);
  }

  @override
  Future<void> updateVoiceMeta(String dirName, VoicePackMeta meta) {
    return dataSource.updateVoiceMeta(
      dirName,
      VoicePackDto.metaToMap(meta),
    );
  }

  @override
  Future<void> updateVoiceAvatar(String dirName, String imagePath) {
    return dataSource.updateVoiceAvatar(dirName, imagePath);
  }

  @override
  Future<void> exportVoicePack(String dirName, String destPath) {
    return dataSource.exportVoice(dirName, destPath);
  }

  @override
  Future<String?> ensureBundledAsr() => dataSource.ensureBundledAsr();

  @override
  Future<String?> getLastVoiceName() => dataSource.getLastVoiceName();

  @override
  Future<void> setLastVoiceName(String name) {
    return dataSource.setLastVoiceName(name);
  }
}
