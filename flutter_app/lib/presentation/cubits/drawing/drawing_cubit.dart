import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/entities/draw_stroke.dart';
import 'drawing_state.dart';

/// Cubit managing drawing canvas state.
class DrawingCubit extends Cubit<DrawingState> {
  DrawingCubit() : super(const DrawingState());

  DrawStroke? _currentStroke;

  void startStroke(double x, double y) {
    _currentStroke = DrawStroke(
      points: [DrawPoint(x: x, y: y)],
      color: state.isEraser ? 0x00000000 : state.currentColor,
      strokeWidth: state.strokeWidth,
      isEraser: state.isEraser,
    );
  }

  void addPoint(double x, double y) {
    if (_currentStroke == null) return;
    final points = [..._currentStroke!.points, DrawPoint(x: x, y: y)];
    _currentStroke = _currentStroke!.copyWith(points: points);
    // Emit with current stroke appended for live preview
    emit(state.copyWith(
      strokes: [...state.strokes, _currentStroke!],
    ));
  }

  void endStroke() {
    if (_currentStroke != null && _currentStroke!.points.length >= 2) {
      final committed = [...state.strokes];
      // Replace preview with final
      if (committed.isNotEmpty) {
        committed[committed.length - 1] = _currentStroke!;
      }
      emit(state.copyWith(strokes: committed));
    }
    _currentStroke = null;
  }

  void setColor(int color) {
    emit(state.copyWith(currentColor: color, isEraser: false));
  }

  void setStrokeWidth(double width) {
    emit(state.copyWith(strokeWidth: width));
  }

  void toggleEraser() {
    emit(state.copyWith(isEraser: !state.isEraser));
  }

  void undo() {
    if (state.strokes.isEmpty) return;
    final strokes = [...state.strokes]..removeLast();
    emit(state.copyWith(strokes: strokes));
  }

  void clear() {
    emit(state.copyWith(strokes: []));
  }
}
