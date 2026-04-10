// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'draw_stroke.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$DrawPoint {
  double get x => throw _privateConstructorUsedError;
  double get y => throw _privateConstructorUsedError;

  /// Create a copy of DrawPoint
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DrawPointCopyWith<DrawPoint> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DrawPointCopyWith<$Res> {
  factory $DrawPointCopyWith(DrawPoint value, $Res Function(DrawPoint) then) =
      _$DrawPointCopyWithImpl<$Res, DrawPoint>;
  @useResult
  $Res call({double x, double y});
}

/// @nodoc
class _$DrawPointCopyWithImpl<$Res, $Val extends DrawPoint>
    implements $DrawPointCopyWith<$Res> {
  _$DrawPointCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DrawPoint
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? x = null, Object? y = null}) {
    return _then(
      _value.copyWith(
            x: null == x
                ? _value.x
                : x // ignore: cast_nullable_to_non_nullable
                      as double,
            y: null == y
                ? _value.y
                : y // ignore: cast_nullable_to_non_nullable
                      as double,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$DrawPointImplCopyWith<$Res>
    implements $DrawPointCopyWith<$Res> {
  factory _$$DrawPointImplCopyWith(
    _$DrawPointImpl value,
    $Res Function(_$DrawPointImpl) then,
  ) = __$$DrawPointImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({double x, double y});
}

/// @nodoc
class __$$DrawPointImplCopyWithImpl<$Res>
    extends _$DrawPointCopyWithImpl<$Res, _$DrawPointImpl>
    implements _$$DrawPointImplCopyWith<$Res> {
  __$$DrawPointImplCopyWithImpl(
    _$DrawPointImpl _value,
    $Res Function(_$DrawPointImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of DrawPoint
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? x = null, Object? y = null}) {
    return _then(
      _$DrawPointImpl(
        x: null == x
            ? _value.x
            : x // ignore: cast_nullable_to_non_nullable
                  as double,
        y: null == y
            ? _value.y
            : y // ignore: cast_nullable_to_non_nullable
                  as double,
      ),
    );
  }
}

/// @nodoc

class _$DrawPointImpl implements _DrawPoint {
  const _$DrawPointImpl({required this.x, required this.y});

  @override
  final double x;
  @override
  final double y;

  @override
  String toString() {
    return 'DrawPoint(x: $x, y: $y)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DrawPointImpl &&
            (identical(other.x, x) || other.x == x) &&
            (identical(other.y, y) || other.y == y));
  }

  @override
  int get hashCode => Object.hash(runtimeType, x, y);

  /// Create a copy of DrawPoint
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DrawPointImplCopyWith<_$DrawPointImpl> get copyWith =>
      __$$DrawPointImplCopyWithImpl<_$DrawPointImpl>(this, _$identity);
}

abstract class _DrawPoint implements DrawPoint {
  const factory _DrawPoint({required final double x, required final double y}) =
      _$DrawPointImpl;

  @override
  double get x;
  @override
  double get y;

  /// Create a copy of DrawPoint
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DrawPointImplCopyWith<_$DrawPointImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
mixin _$DrawStroke {
  List<DrawPoint> get points => throw _privateConstructorUsedError;
  int get color => throw _privateConstructorUsedError;
  double get strokeWidth => throw _privateConstructorUsedError;
  bool get isEraser => throw _privateConstructorUsedError;

  /// Create a copy of DrawStroke
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DrawStrokeCopyWith<DrawStroke> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DrawStrokeCopyWith<$Res> {
  factory $DrawStrokeCopyWith(
    DrawStroke value,
    $Res Function(DrawStroke) then,
  ) = _$DrawStrokeCopyWithImpl<$Res, DrawStroke>;
  @useResult
  $Res call({
    List<DrawPoint> points,
    int color,
    double strokeWidth,
    bool isEraser,
  });
}

/// @nodoc
class _$DrawStrokeCopyWithImpl<$Res, $Val extends DrawStroke>
    implements $DrawStrokeCopyWith<$Res> {
  _$DrawStrokeCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DrawStroke
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? points = null,
    Object? color = null,
    Object? strokeWidth = null,
    Object? isEraser = null,
  }) {
    return _then(
      _value.copyWith(
            points: null == points
                ? _value.points
                : points // ignore: cast_nullable_to_non_nullable
                      as List<DrawPoint>,
            color: null == color
                ? _value.color
                : color // ignore: cast_nullable_to_non_nullable
                      as int,
            strokeWidth: null == strokeWidth
                ? _value.strokeWidth
                : strokeWidth // ignore: cast_nullable_to_non_nullable
                      as double,
            isEraser: null == isEraser
                ? _value.isEraser
                : isEraser // ignore: cast_nullable_to_non_nullable
                      as bool,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$DrawStrokeImplCopyWith<$Res>
    implements $DrawStrokeCopyWith<$Res> {
  factory _$$DrawStrokeImplCopyWith(
    _$DrawStrokeImpl value,
    $Res Function(_$DrawStrokeImpl) then,
  ) = __$$DrawStrokeImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    List<DrawPoint> points,
    int color,
    double strokeWidth,
    bool isEraser,
  });
}

/// @nodoc
class __$$DrawStrokeImplCopyWithImpl<$Res>
    extends _$DrawStrokeCopyWithImpl<$Res, _$DrawStrokeImpl>
    implements _$$DrawStrokeImplCopyWith<$Res> {
  __$$DrawStrokeImplCopyWithImpl(
    _$DrawStrokeImpl _value,
    $Res Function(_$DrawStrokeImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of DrawStroke
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? points = null,
    Object? color = null,
    Object? strokeWidth = null,
    Object? isEraser = null,
  }) {
    return _then(
      _$DrawStrokeImpl(
        points: null == points
            ? _value._points
            : points // ignore: cast_nullable_to_non_nullable
                  as List<DrawPoint>,
        color: null == color
            ? _value.color
            : color // ignore: cast_nullable_to_non_nullable
                  as int,
        strokeWidth: null == strokeWidth
            ? _value.strokeWidth
            : strokeWidth // ignore: cast_nullable_to_non_nullable
                  as double,
        isEraser: null == isEraser
            ? _value.isEraser
            : isEraser // ignore: cast_nullable_to_non_nullable
                  as bool,
      ),
    );
  }
}

/// @nodoc

class _$DrawStrokeImpl implements _DrawStroke {
  const _$DrawStrokeImpl({
    final List<DrawPoint> points = const [],
    this.color = 0xFF000000,
    this.strokeWidth = 3.0,
    this.isEraser = false,
  }) : _points = points;

  final List<DrawPoint> _points;
  @override
  @JsonKey()
  List<DrawPoint> get points {
    if (_points is EqualUnmodifiableListView) return _points;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_points);
  }

  @override
  @JsonKey()
  final int color;
  @override
  @JsonKey()
  final double strokeWidth;
  @override
  @JsonKey()
  final bool isEraser;

  @override
  String toString() {
    return 'DrawStroke(points: $points, color: $color, strokeWidth: $strokeWidth, isEraser: $isEraser)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DrawStrokeImpl &&
            const DeepCollectionEquality().equals(other._points, _points) &&
            (identical(other.color, color) || other.color == color) &&
            (identical(other.strokeWidth, strokeWidth) ||
                other.strokeWidth == strokeWidth) &&
            (identical(other.isEraser, isEraser) ||
                other.isEraser == isEraser));
  }

  @override
  int get hashCode => Object.hash(
    runtimeType,
    const DeepCollectionEquality().hash(_points),
    color,
    strokeWidth,
    isEraser,
  );

  /// Create a copy of DrawStroke
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DrawStrokeImplCopyWith<_$DrawStrokeImpl> get copyWith =>
      __$$DrawStrokeImplCopyWithImpl<_$DrawStrokeImpl>(this, _$identity);
}

abstract class _DrawStroke implements DrawStroke {
  const factory _DrawStroke({
    final List<DrawPoint> points,
    final int color,
    final double strokeWidth,
    final bool isEraser,
  }) = _$DrawStrokeImpl;

  @override
  List<DrawPoint> get points;
  @override
  int get color;
  @override
  double get strokeWidth;
  @override
  bool get isEraser;

  /// Create a copy of DrawStroke
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DrawStrokeImplCopyWith<_$DrawStrokeImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
