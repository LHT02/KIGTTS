// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'asr_model_info.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

AsrModelInfo _$AsrModelInfoFromJson(Map<String, dynamic> json) {
  return _AsrModelInfo.fromJson(json);
}

/// @nodoc
mixin _$AsrModelInfo {
  String get dirName => throw _privateConstructorUsedError;
  String get dirPath => throw _privateConstructorUsedError;
  bool get isBundled => throw _privateConstructorUsedError;

  /// Serializes this AsrModelInfo to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of AsrModelInfo
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $AsrModelInfoCopyWith<AsrModelInfo> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $AsrModelInfoCopyWith<$Res> {
  factory $AsrModelInfoCopyWith(
    AsrModelInfo value,
    $Res Function(AsrModelInfo) then,
  ) = _$AsrModelInfoCopyWithImpl<$Res, AsrModelInfo>;
  @useResult
  $Res call({String dirName, String dirPath, bool isBundled});
}

/// @nodoc
class _$AsrModelInfoCopyWithImpl<$Res, $Val extends AsrModelInfo>
    implements $AsrModelInfoCopyWith<$Res> {
  _$AsrModelInfoCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of AsrModelInfo
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? dirName = null,
    Object? dirPath = null,
    Object? isBundled = null,
  }) {
    return _then(
      _value.copyWith(
            dirName: null == dirName
                ? _value.dirName
                : dirName // ignore: cast_nullable_to_non_nullable
                      as String,
            dirPath: null == dirPath
                ? _value.dirPath
                : dirPath // ignore: cast_nullable_to_non_nullable
                      as String,
            isBundled: null == isBundled
                ? _value.isBundled
                : isBundled // ignore: cast_nullable_to_non_nullable
                      as bool,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$AsrModelInfoImplCopyWith<$Res>
    implements $AsrModelInfoCopyWith<$Res> {
  factory _$$AsrModelInfoImplCopyWith(
    _$AsrModelInfoImpl value,
    $Res Function(_$AsrModelInfoImpl) then,
  ) = __$$AsrModelInfoImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String dirName, String dirPath, bool isBundled});
}

/// @nodoc
class __$$AsrModelInfoImplCopyWithImpl<$Res>
    extends _$AsrModelInfoCopyWithImpl<$Res, _$AsrModelInfoImpl>
    implements _$$AsrModelInfoImplCopyWith<$Res> {
  __$$AsrModelInfoImplCopyWithImpl(
    _$AsrModelInfoImpl _value,
    $Res Function(_$AsrModelInfoImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of AsrModelInfo
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? dirName = null,
    Object? dirPath = null,
    Object? isBundled = null,
  }) {
    return _then(
      _$AsrModelInfoImpl(
        dirName: null == dirName
            ? _value.dirName
            : dirName // ignore: cast_nullable_to_non_nullable
                  as String,
        dirPath: null == dirPath
            ? _value.dirPath
            : dirPath // ignore: cast_nullable_to_non_nullable
                  as String,
        isBundled: null == isBundled
            ? _value.isBundled
            : isBundled // ignore: cast_nullable_to_non_nullable
                  as bool,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$AsrModelInfoImpl implements _AsrModelInfo {
  const _$AsrModelInfoImpl({
    required this.dirName,
    required this.dirPath,
    this.isBundled = false,
  });

  factory _$AsrModelInfoImpl.fromJson(Map<String, dynamic> json) =>
      _$$AsrModelInfoImplFromJson(json);

  @override
  final String dirName;
  @override
  final String dirPath;
  @override
  @JsonKey()
  final bool isBundled;

  @override
  String toString() {
    return 'AsrModelInfo(dirName: $dirName, dirPath: $dirPath, isBundled: $isBundled)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$AsrModelInfoImpl &&
            (identical(other.dirName, dirName) || other.dirName == dirName) &&
            (identical(other.dirPath, dirPath) || other.dirPath == dirPath) &&
            (identical(other.isBundled, isBundled) ||
                other.isBundled == isBundled));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, dirName, dirPath, isBundled);

  /// Create a copy of AsrModelInfo
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$AsrModelInfoImplCopyWith<_$AsrModelInfoImpl> get copyWith =>
      __$$AsrModelInfoImplCopyWithImpl<_$AsrModelInfoImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$AsrModelInfoImplToJson(this);
  }
}

abstract class _AsrModelInfo implements AsrModelInfo {
  const factory _AsrModelInfo({
    required final String dirName,
    required final String dirPath,
    final bool isBundled,
  }) = _$AsrModelInfoImpl;

  factory _AsrModelInfo.fromJson(Map<String, dynamic> json) =
      _$AsrModelInfoImpl.fromJson;

  @override
  String get dirName;
  @override
  String get dirPath;
  @override
  bool get isBundled;

  /// Create a copy of AsrModelInfo
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$AsrModelInfoImplCopyWith<_$AsrModelInfoImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
