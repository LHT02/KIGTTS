// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'voice_pack_info.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

VoicePackMeta _$VoicePackMetaFromJson(Map<String, dynamic> json) {
  return _VoicePackMeta.fromJson(json);
}

/// @nodoc
mixin _$VoicePackMeta {
  String get name => throw _privateConstructorUsedError;
  String get remark => throw _privateConstructorUsedError;
  String get avatar => throw _privateConstructorUsedError;
  bool get pinned => throw _privateConstructorUsedError;
  int get order => throw _privateConstructorUsedError;

  /// Serializes this VoicePackMeta to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of VoicePackMeta
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $VoicePackMetaCopyWith<VoicePackMeta> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $VoicePackMetaCopyWith<$Res> {
  factory $VoicePackMetaCopyWith(
    VoicePackMeta value,
    $Res Function(VoicePackMeta) then,
  ) = _$VoicePackMetaCopyWithImpl<$Res, VoicePackMeta>;
  @useResult
  $Res call({
    String name,
    String remark,
    String avatar,
    bool pinned,
    int order,
  });
}

/// @nodoc
class _$VoicePackMetaCopyWithImpl<$Res, $Val extends VoicePackMeta>
    implements $VoicePackMetaCopyWith<$Res> {
  _$VoicePackMetaCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of VoicePackMeta
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? name = null,
    Object? remark = null,
    Object? avatar = null,
    Object? pinned = null,
    Object? order = null,
  }) {
    return _then(
      _value.copyWith(
            name: null == name
                ? _value.name
                : name // ignore: cast_nullable_to_non_nullable
                      as String,
            remark: null == remark
                ? _value.remark
                : remark // ignore: cast_nullable_to_non_nullable
                      as String,
            avatar: null == avatar
                ? _value.avatar
                : avatar // ignore: cast_nullable_to_non_nullable
                      as String,
            pinned: null == pinned
                ? _value.pinned
                : pinned // ignore: cast_nullable_to_non_nullable
                      as bool,
            order: null == order
                ? _value.order
                : order // ignore: cast_nullable_to_non_nullable
                      as int,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$VoicePackMetaImplCopyWith<$Res>
    implements $VoicePackMetaCopyWith<$Res> {
  factory _$$VoicePackMetaImplCopyWith(
    _$VoicePackMetaImpl value,
    $Res Function(_$VoicePackMetaImpl) then,
  ) = __$$VoicePackMetaImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    String name,
    String remark,
    String avatar,
    bool pinned,
    int order,
  });
}

/// @nodoc
class __$$VoicePackMetaImplCopyWithImpl<$Res>
    extends _$VoicePackMetaCopyWithImpl<$Res, _$VoicePackMetaImpl>
    implements _$$VoicePackMetaImplCopyWith<$Res> {
  __$$VoicePackMetaImplCopyWithImpl(
    _$VoicePackMetaImpl _value,
    $Res Function(_$VoicePackMetaImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of VoicePackMeta
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? name = null,
    Object? remark = null,
    Object? avatar = null,
    Object? pinned = null,
    Object? order = null,
  }) {
    return _then(
      _$VoicePackMetaImpl(
        name: null == name
            ? _value.name
            : name // ignore: cast_nullable_to_non_nullable
                  as String,
        remark: null == remark
            ? _value.remark
            : remark // ignore: cast_nullable_to_non_nullable
                  as String,
        avatar: null == avatar
            ? _value.avatar
            : avatar // ignore: cast_nullable_to_non_nullable
                  as String,
        pinned: null == pinned
            ? _value.pinned
            : pinned // ignore: cast_nullable_to_non_nullable
                  as bool,
        order: null == order
            ? _value.order
            : order // ignore: cast_nullable_to_non_nullable
                  as int,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$VoicePackMetaImpl implements _VoicePackMeta {
  const _$VoicePackMetaImpl({
    this.name = '未命名',
    this.remark = '',
    this.avatar = 'avatar.png',
    this.pinned = false,
    this.order = 0,
  });

  factory _$VoicePackMetaImpl.fromJson(Map<String, dynamic> json) =>
      _$$VoicePackMetaImplFromJson(json);

  @override
  @JsonKey()
  final String name;
  @override
  @JsonKey()
  final String remark;
  @override
  @JsonKey()
  final String avatar;
  @override
  @JsonKey()
  final bool pinned;
  @override
  @JsonKey()
  final int order;

  @override
  String toString() {
    return 'VoicePackMeta(name: $name, remark: $remark, avatar: $avatar, pinned: $pinned, order: $order)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$VoicePackMetaImpl &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.remark, remark) || other.remark == remark) &&
            (identical(other.avatar, avatar) || other.avatar == avatar) &&
            (identical(other.pinned, pinned) || other.pinned == pinned) &&
            (identical(other.order, order) || other.order == order));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, name, remark, avatar, pinned, order);

  /// Create a copy of VoicePackMeta
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$VoicePackMetaImplCopyWith<_$VoicePackMetaImpl> get copyWith =>
      __$$VoicePackMetaImplCopyWithImpl<_$VoicePackMetaImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$VoicePackMetaImplToJson(this);
  }
}

abstract class _VoicePackMeta implements VoicePackMeta {
  const factory _VoicePackMeta({
    final String name,
    final String remark,
    final String avatar,
    final bool pinned,
    final int order,
  }) = _$VoicePackMetaImpl;

  factory _VoicePackMeta.fromJson(Map<String, dynamic> json) =
      _$VoicePackMetaImpl.fromJson;

  @override
  String get name;
  @override
  String get remark;
  @override
  String get avatar;
  @override
  bool get pinned;
  @override
  int get order;

  /// Create a copy of VoicePackMeta
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$VoicePackMetaImplCopyWith<_$VoicePackMetaImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

VoicePackInfo _$VoicePackInfoFromJson(Map<String, dynamic> json) {
  return _VoicePackInfo.fromJson(json);
}

/// @nodoc
mixin _$VoicePackInfo {
  String get dirName => throw _privateConstructorUsedError;
  String get dirPath => throw _privateConstructorUsedError;
  VoicePackMeta get meta => throw _privateConstructorUsedError;
  String? get avatarPath => throw _privateConstructorUsedError;

  /// Serializes this VoicePackInfo to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of VoicePackInfo
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $VoicePackInfoCopyWith<VoicePackInfo> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $VoicePackInfoCopyWith<$Res> {
  factory $VoicePackInfoCopyWith(
    VoicePackInfo value,
    $Res Function(VoicePackInfo) then,
  ) = _$VoicePackInfoCopyWithImpl<$Res, VoicePackInfo>;
  @useResult
  $Res call({
    String dirName,
    String dirPath,
    VoicePackMeta meta,
    String? avatarPath,
  });

  $VoicePackMetaCopyWith<$Res> get meta;
}

/// @nodoc
class _$VoicePackInfoCopyWithImpl<$Res, $Val extends VoicePackInfo>
    implements $VoicePackInfoCopyWith<$Res> {
  _$VoicePackInfoCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of VoicePackInfo
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? dirName = null,
    Object? dirPath = null,
    Object? meta = null,
    Object? avatarPath = freezed,
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
            meta: null == meta
                ? _value.meta
                : meta // ignore: cast_nullable_to_non_nullable
                      as VoicePackMeta,
            avatarPath: freezed == avatarPath
                ? _value.avatarPath
                : avatarPath // ignore: cast_nullable_to_non_nullable
                      as String?,
          )
          as $Val,
    );
  }

  /// Create a copy of VoicePackInfo
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $VoicePackMetaCopyWith<$Res> get meta {
    return $VoicePackMetaCopyWith<$Res>(_value.meta, (value) {
      return _then(_value.copyWith(meta: value) as $Val);
    });
  }
}

/// @nodoc
abstract class _$$VoicePackInfoImplCopyWith<$Res>
    implements $VoicePackInfoCopyWith<$Res> {
  factory _$$VoicePackInfoImplCopyWith(
    _$VoicePackInfoImpl value,
    $Res Function(_$VoicePackInfoImpl) then,
  ) = __$$VoicePackInfoImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    String dirName,
    String dirPath,
    VoicePackMeta meta,
    String? avatarPath,
  });

  @override
  $VoicePackMetaCopyWith<$Res> get meta;
}

/// @nodoc
class __$$VoicePackInfoImplCopyWithImpl<$Res>
    extends _$VoicePackInfoCopyWithImpl<$Res, _$VoicePackInfoImpl>
    implements _$$VoicePackInfoImplCopyWith<$Res> {
  __$$VoicePackInfoImplCopyWithImpl(
    _$VoicePackInfoImpl _value,
    $Res Function(_$VoicePackInfoImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of VoicePackInfo
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? dirName = null,
    Object? dirPath = null,
    Object? meta = null,
    Object? avatarPath = freezed,
  }) {
    return _then(
      _$VoicePackInfoImpl(
        dirName: null == dirName
            ? _value.dirName
            : dirName // ignore: cast_nullable_to_non_nullable
                  as String,
        dirPath: null == dirPath
            ? _value.dirPath
            : dirPath // ignore: cast_nullable_to_non_nullable
                  as String,
        meta: null == meta
            ? _value.meta
            : meta // ignore: cast_nullable_to_non_nullable
                  as VoicePackMeta,
        avatarPath: freezed == avatarPath
            ? _value.avatarPath
            : avatarPath // ignore: cast_nullable_to_non_nullable
                  as String?,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$VoicePackInfoImpl implements _VoicePackInfo {
  const _$VoicePackInfoImpl({
    required this.dirName,
    required this.dirPath,
    required this.meta,
    this.avatarPath,
  });

  factory _$VoicePackInfoImpl.fromJson(Map<String, dynamic> json) =>
      _$$VoicePackInfoImplFromJson(json);

  @override
  final String dirName;
  @override
  final String dirPath;
  @override
  final VoicePackMeta meta;
  @override
  final String? avatarPath;

  @override
  String toString() {
    return 'VoicePackInfo(dirName: $dirName, dirPath: $dirPath, meta: $meta, avatarPath: $avatarPath)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$VoicePackInfoImpl &&
            (identical(other.dirName, dirName) || other.dirName == dirName) &&
            (identical(other.dirPath, dirPath) || other.dirPath == dirPath) &&
            (identical(other.meta, meta) || other.meta == meta) &&
            (identical(other.avatarPath, avatarPath) ||
                other.avatarPath == avatarPath));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, dirName, dirPath, meta, avatarPath);

  /// Create a copy of VoicePackInfo
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$VoicePackInfoImplCopyWith<_$VoicePackInfoImpl> get copyWith =>
      __$$VoicePackInfoImplCopyWithImpl<_$VoicePackInfoImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$VoicePackInfoImplToJson(this);
  }
}

abstract class _VoicePackInfo implements VoicePackInfo {
  const factory _VoicePackInfo({
    required final String dirName,
    required final String dirPath,
    required final VoicePackMeta meta,
    final String? avatarPath,
  }) = _$VoicePackInfoImpl;

  factory _VoicePackInfo.fromJson(Map<String, dynamic> json) =
      _$VoicePackInfoImpl.fromJson;

  @override
  String get dirName;
  @override
  String get dirPath;
  @override
  VoicePackMeta get meta;
  @override
  String? get avatarPath;

  /// Create a copy of VoicePackInfo
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$VoicePackInfoImplCopyWith<_$VoicePackInfoImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
