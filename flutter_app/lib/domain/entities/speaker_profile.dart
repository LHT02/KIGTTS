import 'package:freezed_annotation/freezed_annotation.dart';

part 'speaker_profile.freezed.dart';
part 'speaker_profile.g.dart';

/// A speaker verification profile with embedding vector.
@freezed
abstract class SpeakerProfile with _$SpeakerProfile {
  const factory SpeakerProfile({
    required String id,
    required String name,
    required List<double> vector,
  }) = _SpeakerProfile;

  factory SpeakerProfile.fromJson(Map<String, dynamic> json) =>
      _$SpeakerProfileFromJson(json);
}
