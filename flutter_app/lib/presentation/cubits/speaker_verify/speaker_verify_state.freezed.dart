// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'speaker_verify_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$SpeakerVerifyState {
  bool get enabled => throw _privateConstructorUsedError;
  double get threshold => throw _privateConstructorUsedError;
  List<String> get profileIds => throw _privateConstructorUsedError;
  bool get enrolling => throw _privateConstructorUsedError;
  double get enrollProgress => throw _privateConstructorUsedError;
  double get enrollLevel => throw _privateConstructorUsedError;
  double get lastSimilarity => throw _privateConstructorUsedError;
  String? get enrollResult => throw _privateConstructorUsedError;
  String? get error => throw _privateConstructorUsedError;

  /// Create a copy of SpeakerVerifyState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $SpeakerVerifyStateCopyWith<SpeakerVerifyState> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $SpeakerVerifyStateCopyWith<$Res> {
  factory $SpeakerVerifyStateCopyWith(
    SpeakerVerifyState value,
    $Res Function(SpeakerVerifyState) then,
  ) = _$SpeakerVerifyStateCopyWithImpl<$Res, SpeakerVerifyState>;
  @useResult
  $Res call({
    bool enabled,
    double threshold,
    List<String> profileIds,
    bool enrolling,
    double enrollProgress,
    double enrollLevel,
    double lastSimilarity,
    String? enrollResult,
    String? error,
  });
}

/// @nodoc
class _$SpeakerVerifyStateCopyWithImpl<$Res, $Val extends SpeakerVerifyState>
    implements $SpeakerVerifyStateCopyWith<$Res> {
  _$SpeakerVerifyStateCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of SpeakerVerifyState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? enabled = null,
    Object? threshold = null,
    Object? profileIds = null,
    Object? enrolling = null,
    Object? enrollProgress = null,
    Object? enrollLevel = null,
    Object? lastSimilarity = null,
    Object? enrollResult = freezed,
    Object? error = freezed,
  }) {
    return _then(
      _value.copyWith(
            enabled: null == enabled
                ? _value.enabled
                : enabled // ignore: cast_nullable_to_non_nullable
                      as bool,
            threshold: null == threshold
                ? _value.threshold
                : threshold // ignore: cast_nullable_to_non_nullable
                      as double,
            profileIds: null == profileIds
                ? _value.profileIds
                : profileIds // ignore: cast_nullable_to_non_nullable
                      as List<String>,
            enrolling: null == enrolling
                ? _value.enrolling
                : enrolling // ignore: cast_nullable_to_non_nullable
                      as bool,
            enrollProgress: null == enrollProgress
                ? _value.enrollProgress
                : enrollProgress // ignore: cast_nullable_to_non_nullable
                      as double,
            enrollLevel: null == enrollLevel
                ? _value.enrollLevel
                : enrollLevel // ignore: cast_nullable_to_non_nullable
                      as double,
            lastSimilarity: null == lastSimilarity
                ? _value.lastSimilarity
                : lastSimilarity // ignore: cast_nullable_to_non_nullable
                      as double,
            enrollResult: freezed == enrollResult
                ? _value.enrollResult
                : enrollResult // ignore: cast_nullable_to_non_nullable
                      as String?,
            error: freezed == error
                ? _value.error
                : error // ignore: cast_nullable_to_non_nullable
                      as String?,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$SpeakerVerifyStateImplCopyWith<$Res>
    implements $SpeakerVerifyStateCopyWith<$Res> {
  factory _$$SpeakerVerifyStateImplCopyWith(
    _$SpeakerVerifyStateImpl value,
    $Res Function(_$SpeakerVerifyStateImpl) then,
  ) = __$$SpeakerVerifyStateImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    bool enabled,
    double threshold,
    List<String> profileIds,
    bool enrolling,
    double enrollProgress,
    double enrollLevel,
    double lastSimilarity,
    String? enrollResult,
    String? error,
  });
}

/// @nodoc
class __$$SpeakerVerifyStateImplCopyWithImpl<$Res>
    extends _$SpeakerVerifyStateCopyWithImpl<$Res, _$SpeakerVerifyStateImpl>
    implements _$$SpeakerVerifyStateImplCopyWith<$Res> {
  __$$SpeakerVerifyStateImplCopyWithImpl(
    _$SpeakerVerifyStateImpl _value,
    $Res Function(_$SpeakerVerifyStateImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of SpeakerVerifyState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? enabled = null,
    Object? threshold = null,
    Object? profileIds = null,
    Object? enrolling = null,
    Object? enrollProgress = null,
    Object? enrollLevel = null,
    Object? lastSimilarity = null,
    Object? enrollResult = freezed,
    Object? error = freezed,
  }) {
    return _then(
      _$SpeakerVerifyStateImpl(
        enabled: null == enabled
            ? _value.enabled
            : enabled // ignore: cast_nullable_to_non_nullable
                  as bool,
        threshold: null == threshold
            ? _value.threshold
            : threshold // ignore: cast_nullable_to_non_nullable
                  as double,
        profileIds: null == profileIds
            ? _value._profileIds
            : profileIds // ignore: cast_nullable_to_non_nullable
                  as List<String>,
        enrolling: null == enrolling
            ? _value.enrolling
            : enrolling // ignore: cast_nullable_to_non_nullable
                  as bool,
        enrollProgress: null == enrollProgress
            ? _value.enrollProgress
            : enrollProgress // ignore: cast_nullable_to_non_nullable
                  as double,
        enrollLevel: null == enrollLevel
            ? _value.enrollLevel
            : enrollLevel // ignore: cast_nullable_to_non_nullable
                  as double,
        lastSimilarity: null == lastSimilarity
            ? _value.lastSimilarity
            : lastSimilarity // ignore: cast_nullable_to_non_nullable
                  as double,
        enrollResult: freezed == enrollResult
            ? _value.enrollResult
            : enrollResult // ignore: cast_nullable_to_non_nullable
                  as String?,
        error: freezed == error
            ? _value.error
            : error // ignore: cast_nullable_to_non_nullable
                  as String?,
      ),
    );
  }
}

/// @nodoc

class _$SpeakerVerifyStateImpl implements _SpeakerVerifyState {
  const _$SpeakerVerifyStateImpl({
    this.enabled = false,
    this.threshold = 0.72,
    final List<String> profileIds = const [],
    this.enrolling = false,
    this.enrollProgress = 0.0,
    this.enrollLevel = 0.0,
    this.lastSimilarity = -1.0,
    this.enrollResult,
    this.error,
  }) : _profileIds = profileIds;

  @override
  @JsonKey()
  final bool enabled;
  @override
  @JsonKey()
  final double threshold;
  final List<String> _profileIds;
  @override
  @JsonKey()
  List<String> get profileIds {
    if (_profileIds is EqualUnmodifiableListView) return _profileIds;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_profileIds);
  }

  @override
  @JsonKey()
  final bool enrolling;
  @override
  @JsonKey()
  final double enrollProgress;
  @override
  @JsonKey()
  final double enrollLevel;
  @override
  @JsonKey()
  final double lastSimilarity;
  @override
  final String? enrollResult;
  @override
  final String? error;

  @override
  String toString() {
    return 'SpeakerVerifyState(enabled: $enabled, threshold: $threshold, profileIds: $profileIds, enrolling: $enrolling, enrollProgress: $enrollProgress, enrollLevel: $enrollLevel, lastSimilarity: $lastSimilarity, enrollResult: $enrollResult, error: $error)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$SpeakerVerifyStateImpl &&
            (identical(other.enabled, enabled) || other.enabled == enabled) &&
            (identical(other.threshold, threshold) ||
                other.threshold == threshold) &&
            const DeepCollectionEquality().equals(
              other._profileIds,
              _profileIds,
            ) &&
            (identical(other.enrolling, enrolling) ||
                other.enrolling == enrolling) &&
            (identical(other.enrollProgress, enrollProgress) ||
                other.enrollProgress == enrollProgress) &&
            (identical(other.enrollLevel, enrollLevel) ||
                other.enrollLevel == enrollLevel) &&
            (identical(other.lastSimilarity, lastSimilarity) ||
                other.lastSimilarity == lastSimilarity) &&
            (identical(other.enrollResult, enrollResult) ||
                other.enrollResult == enrollResult) &&
            (identical(other.error, error) || other.error == error));
  }

  @override
  int get hashCode => Object.hash(
    runtimeType,
    enabled,
    threshold,
    const DeepCollectionEquality().hash(_profileIds),
    enrolling,
    enrollProgress,
    enrollLevel,
    lastSimilarity,
    enrollResult,
    error,
  );

  /// Create a copy of SpeakerVerifyState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$SpeakerVerifyStateImplCopyWith<_$SpeakerVerifyStateImpl> get copyWith =>
      __$$SpeakerVerifyStateImplCopyWithImpl<_$SpeakerVerifyStateImpl>(
        this,
        _$identity,
      );
}

abstract class _SpeakerVerifyState implements SpeakerVerifyState {
  const factory _SpeakerVerifyState({
    final bool enabled,
    final double threshold,
    final List<String> profileIds,
    final bool enrolling,
    final double enrollProgress,
    final double enrollLevel,
    final double lastSimilarity,
    final String? enrollResult,
    final String? error,
  }) = _$SpeakerVerifyStateImpl;

  @override
  bool get enabled;
  @override
  double get threshold;
  @override
  List<String> get profileIds;
  @override
  bool get enrolling;
  @override
  double get enrollProgress;
  @override
  double get enrollLevel;
  @override
  double get lastSimilarity;
  @override
  String? get enrollResult;
  @override
  String? get error;

  /// Create a copy of SpeakerVerifyState
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$SpeakerVerifyStateImplCopyWith<_$SpeakerVerifyStateImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
