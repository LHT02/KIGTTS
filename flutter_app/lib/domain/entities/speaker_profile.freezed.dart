// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'speaker_profile.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

SpeakerProfile _$SpeakerProfileFromJson(Map<String, dynamic> json) {
  return _SpeakerProfile.fromJson(json);
}

/// @nodoc
mixin _$SpeakerProfile {
  String get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  List<double> get vector => throw _privateConstructorUsedError;

  /// Serializes this SpeakerProfile to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of SpeakerProfile
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $SpeakerProfileCopyWith<SpeakerProfile> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $SpeakerProfileCopyWith<$Res> {
  factory $SpeakerProfileCopyWith(
    SpeakerProfile value,
    $Res Function(SpeakerProfile) then,
  ) = _$SpeakerProfileCopyWithImpl<$Res, SpeakerProfile>;
  @useResult
  $Res call({String id, String name, List<double> vector});
}

/// @nodoc
class _$SpeakerProfileCopyWithImpl<$Res, $Val extends SpeakerProfile>
    implements $SpeakerProfileCopyWith<$Res> {
  _$SpeakerProfileCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of SpeakerProfile
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? id = null, Object? name = null, Object? vector = null}) {
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as String,
            name: null == name
                ? _value.name
                : name // ignore: cast_nullable_to_non_nullable
                      as String,
            vector: null == vector
                ? _value.vector
                : vector // ignore: cast_nullable_to_non_nullable
                      as List<double>,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$SpeakerProfileImplCopyWith<$Res>
    implements $SpeakerProfileCopyWith<$Res> {
  factory _$$SpeakerProfileImplCopyWith(
    _$SpeakerProfileImpl value,
    $Res Function(_$SpeakerProfileImpl) then,
  ) = __$$SpeakerProfileImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String id, String name, List<double> vector});
}

/// @nodoc
class __$$SpeakerProfileImplCopyWithImpl<$Res>
    extends _$SpeakerProfileCopyWithImpl<$Res, _$SpeakerProfileImpl>
    implements _$$SpeakerProfileImplCopyWith<$Res> {
  __$$SpeakerProfileImplCopyWithImpl(
    _$SpeakerProfileImpl _value,
    $Res Function(_$SpeakerProfileImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of SpeakerProfile
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? id = null, Object? name = null, Object? vector = null}) {
    return _then(
      _$SpeakerProfileImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as String,
        name: null == name
            ? _value.name
            : name // ignore: cast_nullable_to_non_nullable
                  as String,
        vector: null == vector
            ? _value._vector
            : vector // ignore: cast_nullable_to_non_nullable
                  as List<double>,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$SpeakerProfileImpl implements _SpeakerProfile {
  const _$SpeakerProfileImpl({
    required this.id,
    required this.name,
    required final List<double> vector,
  }) : _vector = vector;

  factory _$SpeakerProfileImpl.fromJson(Map<String, dynamic> json) =>
      _$$SpeakerProfileImplFromJson(json);

  @override
  final String id;
  @override
  final String name;
  final List<double> _vector;
  @override
  List<double> get vector {
    if (_vector is EqualUnmodifiableListView) return _vector;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_vector);
  }

  @override
  String toString() {
    return 'SpeakerProfile(id: $id, name: $name, vector: $vector)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$SpeakerProfileImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            const DeepCollectionEquality().equals(other._vector, _vector));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
    runtimeType,
    id,
    name,
    const DeepCollectionEquality().hash(_vector),
  );

  /// Create a copy of SpeakerProfile
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$SpeakerProfileImplCopyWith<_$SpeakerProfileImpl> get copyWith =>
      __$$SpeakerProfileImplCopyWithImpl<_$SpeakerProfileImpl>(
        this,
        _$identity,
      );

  @override
  Map<String, dynamic> toJson() {
    return _$$SpeakerProfileImplToJson(this);
  }
}

abstract class _SpeakerProfile implements SpeakerProfile {
  const factory _SpeakerProfile({
    required final String id,
    required final String name,
    required final List<double> vector,
  }) = _$SpeakerProfileImpl;

  factory _SpeakerProfile.fromJson(Map<String, dynamic> json) =
      _$SpeakerProfileImpl.fromJson;

  @override
  String get id;
  @override
  String get name;
  @override
  List<double> get vector;

  /// Create a copy of SpeakerProfile
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$SpeakerProfileImplCopyWith<_$SpeakerProfileImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
