import 'package:freezed_annotation/freezed_annotation.dart';
import '../../../domain/entities/asr_model_info.dart';
import '../../../domain/entities/voice_pack_info.dart';

part 'model_manager_state.freezed.dart';

/// State for the model manager page.
@freezed
abstract class ModelManagerState with _$ModelManagerState {
  const factory ModelManagerState({
    @Default([]) List<VoicePackInfo> voicePacks,
    @Default([]) List<AsrModelInfo> asrModels,
    String? currentVoiceDirName,
    String? currentAsrDirName,
    @Default(false) bool importing,
    String? importError,
    @Default(false) bool loading,
  }) = _ModelManagerState;
}
