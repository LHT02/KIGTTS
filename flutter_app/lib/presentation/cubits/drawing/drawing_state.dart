import 'package:freezed_annotation/freezed_annotation.dart';
import '../../../domain/entities/draw_stroke.dart';

part 'drawing_state.freezed.dart';

@freezed
abstract class DrawingState with _$DrawingState {
  const factory DrawingState({
    @Default([]) List<DrawStroke> strokes,
    @Default(0xFF80DEEA) int currentColor,
    @Default(6.0) double strokeWidth,
    @Default(false) bool isEraser,
    @Default(false) bool saving,
    @Default(true) bool toolbarExpanded,
    @Default(false) bool isFullscreen,
  }) = _DrawingState;
}
