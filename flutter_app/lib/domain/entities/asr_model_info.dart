import 'package:freezed_annotation/freezed_annotation.dart';

part 'asr_model_info.freezed.dart';
part 'asr_model_info.g.dart';

/// ASR model information.
@freezed
abstract class AsrModelInfo with _$AsrModelInfo {
  const factory AsrModelInfo({
    required String dirName,
    required String dirPath,
    @Default(false) bool isBundled,
  }) = _AsrModelInfo;

  factory AsrModelInfo.fromJson(Map<String, dynamic> json) =>
      _$AsrModelInfoFromJson(json);
}
