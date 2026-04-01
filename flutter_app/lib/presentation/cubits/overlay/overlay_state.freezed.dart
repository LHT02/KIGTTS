// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'overlay_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$OverlayState {
  bool get isShowing => throw _privateConstructorUsedError;
  bool get loading => throw _privateConstructorUsedError;
  String? get error => throw _privateConstructorUsedError;

  /// Create a copy of OverlayState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $OverlayStateCopyWith<OverlayState> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $OverlayStateCopyWith<$Res> {
  factory $OverlayStateCopyWith(
    OverlayState value,
    $Res Function(OverlayState) then,
  ) = _$OverlayStateCopyWithImpl<$Res, OverlayState>;
  @useResult
  $Res call({bool isShowing, bool loading, String? error});
}

/// @nodoc
class _$OverlayStateCopyWithImpl<$Res, $Val extends OverlayState>
    implements $OverlayStateCopyWith<$Res> {
  _$OverlayStateCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of OverlayState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? isShowing = null,
    Object? loading = null,
    Object? error = freezed,
  }) {
    return _then(
      _value.copyWith(
            isShowing: null == isShowing
                ? _value.isShowing
                : isShowing // ignore: cast_nullable_to_non_nullable
                      as bool,
            loading: null == loading
                ? _value.loading
                : loading // ignore: cast_nullable_to_non_nullable
                      as bool,
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
abstract class _$$OverlayStateImplCopyWith<$Res>
    implements $OverlayStateCopyWith<$Res> {
  factory _$$OverlayStateImplCopyWith(
    _$OverlayStateImpl value,
    $Res Function(_$OverlayStateImpl) then,
  ) = __$$OverlayStateImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({bool isShowing, bool loading, String? error});
}

/// @nodoc
class __$$OverlayStateImplCopyWithImpl<$Res>
    extends _$OverlayStateCopyWithImpl<$Res, _$OverlayStateImpl>
    implements _$$OverlayStateImplCopyWith<$Res> {
  __$$OverlayStateImplCopyWithImpl(
    _$OverlayStateImpl _value,
    $Res Function(_$OverlayStateImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of OverlayState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? isShowing = null,
    Object? loading = null,
    Object? error = freezed,
  }) {
    return _then(
      _$OverlayStateImpl(
        isShowing: null == isShowing
            ? _value.isShowing
            : isShowing // ignore: cast_nullable_to_non_nullable
                  as bool,
        loading: null == loading
            ? _value.loading
            : loading // ignore: cast_nullable_to_non_nullable
                  as bool,
        error: freezed == error
            ? _value.error
            : error // ignore: cast_nullable_to_non_nullable
                  as String?,
      ),
    );
  }
}

/// @nodoc

class _$OverlayStateImpl implements _OverlayState {
  const _$OverlayStateImpl({
    this.isShowing = false,
    this.loading = false,
    this.error,
  });

  @override
  @JsonKey()
  final bool isShowing;
  @override
  @JsonKey()
  final bool loading;
  @override
  final String? error;

  @override
  String toString() {
    return 'OverlayState(isShowing: $isShowing, loading: $loading, error: $error)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$OverlayStateImpl &&
            (identical(other.isShowing, isShowing) ||
                other.isShowing == isShowing) &&
            (identical(other.loading, loading) || other.loading == loading) &&
            (identical(other.error, error) || other.error == error));
  }

  @override
  int get hashCode => Object.hash(runtimeType, isShowing, loading, error);

  /// Create a copy of OverlayState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$OverlayStateImplCopyWith<_$OverlayStateImpl> get copyWith =>
      __$$OverlayStateImplCopyWithImpl<_$OverlayStateImpl>(this, _$identity);
}

abstract class _OverlayState implements OverlayState {
  const factory _OverlayState({
    final bool isShowing,
    final bool loading,
    final String? error,
  }) = _$OverlayStateImpl;

  @override
  bool get isShowing;
  @override
  bool get loading;
  @override
  String? get error;

  /// Create a copy of OverlayState
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$OverlayStateImplCopyWith<_$OverlayStateImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
