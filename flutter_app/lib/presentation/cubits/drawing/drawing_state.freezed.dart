// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'drawing_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$DrawingState {
  List<DrawStroke> get strokes => throw _privateConstructorUsedError;
  int get currentColor => throw _privateConstructorUsedError;
  double get strokeWidth => throw _privateConstructorUsedError;
  bool get isEraser => throw _privateConstructorUsedError;
  bool get saving => throw _privateConstructorUsedError;

  /// Create a copy of DrawingState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DrawingStateCopyWith<DrawingState> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DrawingStateCopyWith<$Res> {
  factory $DrawingStateCopyWith(
    DrawingState value,
    $Res Function(DrawingState) then,
  ) = _$DrawingStateCopyWithImpl<$Res, DrawingState>;
  @useResult
  $Res call({
    List<DrawStroke> strokes,
    int currentColor,
    double strokeWidth,
    bool isEraser,
    bool saving,
  });
}

/// @nodoc
class _$DrawingStateCopyWithImpl<$Res, $Val extends DrawingState>
    implements $DrawingStateCopyWith<$Res> {
  _$DrawingStateCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DrawingState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? strokes = null,
    Object? currentColor = null,
    Object? strokeWidth = null,
    Object? isEraser = null,
    Object? saving = null,
  }) {
    return _then(
      _value.copyWith(
            strokes: null == strokes
                ? _value.strokes
                : strokes // ignore: cast_nullable_to_non_nullable
                      as List<DrawStroke>,
            currentColor: null == currentColor
                ? _value.currentColor
                : currentColor // ignore: cast_nullable_to_non_nullable
                      as int,
            strokeWidth: null == strokeWidth
                ? _value.strokeWidth
                : strokeWidth // ignore: cast_nullable_to_non_nullable
                      as double,
            isEraser: null == isEraser
                ? _value.isEraser
                : isEraser // ignore: cast_nullable_to_non_nullable
                      as bool,
            saving: null == saving
                ? _value.saving
                : saving // ignore: cast_nullable_to_non_nullable
                      as bool,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$DrawingStateImplCopyWith<$Res>
    implements $DrawingStateCopyWith<$Res> {
  factory _$$DrawingStateImplCopyWith(
    _$DrawingStateImpl value,
    $Res Function(_$DrawingStateImpl) then,
  ) = __$$DrawingStateImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    List<DrawStroke> strokes,
    int currentColor,
    double strokeWidth,
    bool isEraser,
    bool saving,
  });
}

/// @nodoc
class __$$DrawingStateImplCopyWithImpl<$Res>
    extends _$DrawingStateCopyWithImpl<$Res, _$DrawingStateImpl>
    implements _$$DrawingStateImplCopyWith<$Res> {
  __$$DrawingStateImplCopyWithImpl(
    _$DrawingStateImpl _value,
    $Res Function(_$DrawingStateImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of DrawingState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? strokes = null,
    Object? currentColor = null,
    Object? strokeWidth = null,
    Object? isEraser = null,
    Object? saving = null,
  }) {
    return _then(
      _$DrawingStateImpl(
        strokes: null == strokes
            ? _value._strokes
            : strokes // ignore: cast_nullable_to_non_nullable
                  as List<DrawStroke>,
        currentColor: null == currentColor
            ? _value.currentColor
            : currentColor // ignore: cast_nullable_to_non_nullable
                  as int,
        strokeWidth: null == strokeWidth
            ? _value.strokeWidth
            : strokeWidth // ignore: cast_nullable_to_non_nullable
                  as double,
        isEraser: null == isEraser
            ? _value.isEraser
            : isEraser // ignore: cast_nullable_to_non_nullable
                  as bool,
        saving: null == saving
            ? _value.saving
            : saving // ignore: cast_nullable_to_non_nullable
                  as bool,
      ),
    );
  }
}

/// @nodoc

class _$DrawingStateImpl implements _DrawingState {
  const _$DrawingStateImpl({
    final List<DrawStroke> strokes = const [],
    this.currentColor = 0xFF000000,
    this.strokeWidth = 3.0,
    this.isEraser = false,
    this.saving = false,
  }) : _strokes = strokes;

  final List<DrawStroke> _strokes;
  @override
  @JsonKey()
  List<DrawStroke> get strokes {
    if (_strokes is EqualUnmodifiableListView) return _strokes;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_strokes);
  }

  @override
  @JsonKey()
  final int currentColor;
  @override
  @JsonKey()
  final double strokeWidth;
  @override
  @JsonKey()
  final bool isEraser;
  @override
  @JsonKey()
  final bool saving;

  @override
  String toString() {
    return 'DrawingState(strokes: $strokes, currentColor: $currentColor, strokeWidth: $strokeWidth, isEraser: $isEraser, saving: $saving)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DrawingStateImpl &&
            const DeepCollectionEquality().equals(other._strokes, _strokes) &&
            (identical(other.currentColor, currentColor) ||
                other.currentColor == currentColor) &&
            (identical(other.strokeWidth, strokeWidth) ||
                other.strokeWidth == strokeWidth) &&
            (identical(other.isEraser, isEraser) ||
                other.isEraser == isEraser) &&
            (identical(other.saving, saving) || other.saving == saving));
  }

  @override
  int get hashCode => Object.hash(
    runtimeType,
    const DeepCollectionEquality().hash(_strokes),
    currentColor,
    strokeWidth,
    isEraser,
    saving,
  );

  /// Create a copy of DrawingState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DrawingStateImplCopyWith<_$DrawingStateImpl> get copyWith =>
      __$$DrawingStateImplCopyWithImpl<_$DrawingStateImpl>(this, _$identity);
}

abstract class _DrawingState implements DrawingState {
  const factory _DrawingState({
    final List<DrawStroke> strokes,
    final int currentColor,
    final double strokeWidth,
    final bool isEraser,
    final bool saving,
  }) = _$DrawingStateImpl;

  @override
  List<DrawStroke> get strokes;
  @override
  int get currentColor;
  @override
  double get strokeWidth;
  @override
  bool get isEraser;
  @override
  bool get saving;

  /// Create a copy of DrawingState
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DrawingStateImplCopyWith<_$DrawingStateImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
