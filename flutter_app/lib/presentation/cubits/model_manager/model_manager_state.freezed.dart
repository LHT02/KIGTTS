// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'model_manager_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$ModelManagerState {
  List<VoicePackInfo> get voicePacks => throw _privateConstructorUsedError;
  List<AsrModelInfo> get asrModels => throw _privateConstructorUsedError;
  String? get currentVoiceDirName => throw _privateConstructorUsedError;
  String? get currentAsrDirName => throw _privateConstructorUsedError;
  bool get importing => throw _privateConstructorUsedError;
  String? get importError => throw _privateConstructorUsedError;
  bool get loading => throw _privateConstructorUsedError;

  /// Create a copy of ModelManagerState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $ModelManagerStateCopyWith<ModelManagerState> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $ModelManagerStateCopyWith<$Res> {
  factory $ModelManagerStateCopyWith(
    ModelManagerState value,
    $Res Function(ModelManagerState) then,
  ) = _$ModelManagerStateCopyWithImpl<$Res, ModelManagerState>;
  @useResult
  $Res call({
    List<VoicePackInfo> voicePacks,
    List<AsrModelInfo> asrModels,
    String? currentVoiceDirName,
    String? currentAsrDirName,
    bool importing,
    String? importError,
    bool loading,
  });
}

/// @nodoc
class _$ModelManagerStateCopyWithImpl<$Res, $Val extends ModelManagerState>
    implements $ModelManagerStateCopyWith<$Res> {
  _$ModelManagerStateCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of ModelManagerState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? voicePacks = null,
    Object? asrModels = null,
    Object? currentVoiceDirName = freezed,
    Object? currentAsrDirName = freezed,
    Object? importing = null,
    Object? importError = freezed,
    Object? loading = null,
  }) {
    return _then(
      _value.copyWith(
            voicePacks: null == voicePacks
                ? _value.voicePacks
                : voicePacks // ignore: cast_nullable_to_non_nullable
                      as List<VoicePackInfo>,
            asrModels: null == asrModels
                ? _value.asrModels
                : asrModels // ignore: cast_nullable_to_non_nullable
                      as List<AsrModelInfo>,
            currentVoiceDirName: freezed == currentVoiceDirName
                ? _value.currentVoiceDirName
                : currentVoiceDirName // ignore: cast_nullable_to_non_nullable
                      as String?,
            currentAsrDirName: freezed == currentAsrDirName
                ? _value.currentAsrDirName
                : currentAsrDirName // ignore: cast_nullable_to_non_nullable
                      as String?,
            importing: null == importing
                ? _value.importing
                : importing // ignore: cast_nullable_to_non_nullable
                      as bool,
            importError: freezed == importError
                ? _value.importError
                : importError // ignore: cast_nullable_to_non_nullable
                      as String?,
            loading: null == loading
                ? _value.loading
                : loading // ignore: cast_nullable_to_non_nullable
                      as bool,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$ModelManagerStateImplCopyWith<$Res>
    implements $ModelManagerStateCopyWith<$Res> {
  factory _$$ModelManagerStateImplCopyWith(
    _$ModelManagerStateImpl value,
    $Res Function(_$ModelManagerStateImpl) then,
  ) = __$$ModelManagerStateImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    List<VoicePackInfo> voicePacks,
    List<AsrModelInfo> asrModels,
    String? currentVoiceDirName,
    String? currentAsrDirName,
    bool importing,
    String? importError,
    bool loading,
  });
}

/// @nodoc
class __$$ModelManagerStateImplCopyWithImpl<$Res>
    extends _$ModelManagerStateCopyWithImpl<$Res, _$ModelManagerStateImpl>
    implements _$$ModelManagerStateImplCopyWith<$Res> {
  __$$ModelManagerStateImplCopyWithImpl(
    _$ModelManagerStateImpl _value,
    $Res Function(_$ModelManagerStateImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of ModelManagerState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? voicePacks = null,
    Object? asrModels = null,
    Object? currentVoiceDirName = freezed,
    Object? currentAsrDirName = freezed,
    Object? importing = null,
    Object? importError = freezed,
    Object? loading = null,
  }) {
    return _then(
      _$ModelManagerStateImpl(
        voicePacks: null == voicePacks
            ? _value._voicePacks
            : voicePacks // ignore: cast_nullable_to_non_nullable
                  as List<VoicePackInfo>,
        asrModels: null == asrModels
            ? _value._asrModels
            : asrModels // ignore: cast_nullable_to_non_nullable
                  as List<AsrModelInfo>,
        currentVoiceDirName: freezed == currentVoiceDirName
            ? _value.currentVoiceDirName
            : currentVoiceDirName // ignore: cast_nullable_to_non_nullable
                  as String?,
        currentAsrDirName: freezed == currentAsrDirName
            ? _value.currentAsrDirName
            : currentAsrDirName // ignore: cast_nullable_to_non_nullable
                  as String?,
        importing: null == importing
            ? _value.importing
            : importing // ignore: cast_nullable_to_non_nullable
                  as bool,
        importError: freezed == importError
            ? _value.importError
            : importError // ignore: cast_nullable_to_non_nullable
                  as String?,
        loading: null == loading
            ? _value.loading
            : loading // ignore: cast_nullable_to_non_nullable
                  as bool,
      ),
    );
  }
}

/// @nodoc

class _$ModelManagerStateImpl implements _ModelManagerState {
  const _$ModelManagerStateImpl({
    final List<VoicePackInfo> voicePacks = const [],
    final List<AsrModelInfo> asrModels = const [],
    this.currentVoiceDirName,
    this.currentAsrDirName,
    this.importing = false,
    this.importError,
    this.loading = false,
  }) : _voicePacks = voicePacks,
       _asrModels = asrModels;

  final List<VoicePackInfo> _voicePacks;
  @override
  @JsonKey()
  List<VoicePackInfo> get voicePacks {
    if (_voicePacks is EqualUnmodifiableListView) return _voicePacks;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_voicePacks);
  }

  final List<AsrModelInfo> _asrModels;
  @override
  @JsonKey()
  List<AsrModelInfo> get asrModels {
    if (_asrModels is EqualUnmodifiableListView) return _asrModels;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_asrModels);
  }

  @override
  final String? currentVoiceDirName;
  @override
  final String? currentAsrDirName;
  @override
  @JsonKey()
  final bool importing;
  @override
  final String? importError;
  @override
  @JsonKey()
  final bool loading;

  @override
  String toString() {
    return 'ModelManagerState(voicePacks: $voicePacks, asrModels: $asrModels, currentVoiceDirName: $currentVoiceDirName, currentAsrDirName: $currentAsrDirName, importing: $importing, importError: $importError, loading: $loading)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ModelManagerStateImpl &&
            const DeepCollectionEquality().equals(
              other._voicePacks,
              _voicePacks,
            ) &&
            const DeepCollectionEquality().equals(
              other._asrModels,
              _asrModels,
            ) &&
            (identical(other.currentVoiceDirName, currentVoiceDirName) ||
                other.currentVoiceDirName == currentVoiceDirName) &&
            (identical(other.currentAsrDirName, currentAsrDirName) ||
                other.currentAsrDirName == currentAsrDirName) &&
            (identical(other.importing, importing) ||
                other.importing == importing) &&
            (identical(other.importError, importError) ||
                other.importError == importError) &&
            (identical(other.loading, loading) || other.loading == loading));
  }

  @override
  int get hashCode => Object.hash(
    runtimeType,
    const DeepCollectionEquality().hash(_voicePacks),
    const DeepCollectionEquality().hash(_asrModels),
    currentVoiceDirName,
    currentAsrDirName,
    importing,
    importError,
    loading,
  );

  /// Create a copy of ModelManagerState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$ModelManagerStateImplCopyWith<_$ModelManagerStateImpl> get copyWith =>
      __$$ModelManagerStateImplCopyWithImpl<_$ModelManagerStateImpl>(
        this,
        _$identity,
      );
}

abstract class _ModelManagerState implements ModelManagerState {
  const factory _ModelManagerState({
    final List<VoicePackInfo> voicePacks,
    final List<AsrModelInfo> asrModels,
    final String? currentVoiceDirName,
    final String? currentAsrDirName,
    final bool importing,
    final String? importError,
    final bool loading,
  }) = _$ModelManagerStateImpl;

  @override
  List<VoicePackInfo> get voicePacks;
  @override
  List<AsrModelInfo> get asrModels;
  @override
  String? get currentVoiceDirName;
  @override
  String? get currentAsrDirName;
  @override
  bool get importing;
  @override
  String? get importError;
  @override
  bool get loading;

  /// Create a copy of ModelManagerState
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$ModelManagerStateImplCopyWith<_$ModelManagerStateImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
