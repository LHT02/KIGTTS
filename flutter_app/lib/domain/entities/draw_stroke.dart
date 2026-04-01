import 'package:freezed_annotation/freezed_annotation.dart';

part 'draw_stroke.freezed.dart';

/// A single point on the drawing canvas.
@freezed
abstract class DrawPoint with _$DrawPoint {
  const factory DrawPoint({
    required double x,
    required double y,
  }) = _DrawPoint;
}

/// A stroke (series of points) with color and width.
@freezed
abstract class DrawStroke with _$DrawStroke {
  const factory DrawStroke({
    @Default([]) List<DrawPoint> points,
    @Default(0xFF000000) int color,
    @Default(3.0) double strokeWidth,
    @Default(false) bool isEraser,
  }) = _DrawStroke;
}
