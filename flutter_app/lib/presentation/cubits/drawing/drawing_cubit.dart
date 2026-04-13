import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/entities/draw_stroke.dart';
import 'drawing_state.dart';

/// Cubit managing drawing canvas state.
class DrawingCubit extends Cubit<DrawingState> {
  DrawingCubit() : super(const DrawingState());

  DrawStroke? _currentStroke;
  bool _previewStrokeActive = false;
  final List<DrawStroke> _redoStrokes = [];

  bool get canRedo => _redoStrokes.isNotEmpty;

  void startStroke(double x, double y) {
    if (_redoStrokes.isNotEmpty) {
      _redoStrokes.clear();
    }
    _currentStroke = DrawStroke(
      points: [DrawPoint(x: x, y: y)],
      color: state.isEraser ? 0x00000000 : state.currentColor,
      strokeWidth: state.strokeWidth,
      isEraser: state.isEraser,
    );
    _previewStrokeActive = false;
  }

  void addPoint(double x, double y) {
    if (_currentStroke == null) return;
    final points = [
      ..._currentStroke!.points,
      DrawPoint(x: x, y: y),
    ];
    _currentStroke = _currentStroke!.copyWith(points: points);
    final strokes = [...state.strokes];
    if (_previewStrokeActive && strokes.isNotEmpty) {
      strokes[strokes.length - 1] = _currentStroke!;
    } else {
      strokes.add(_currentStroke!);
      _previewStrokeActive = true;
    }
    emit(state.copyWith(strokes: strokes));
  }

  void endStroke() {
    if (_currentStroke != null && _currentStroke!.points.length >= 2) {
      final committed = [...state.strokes];
      if (_previewStrokeActive && committed.isNotEmpty) {
        committed[committed.length - 1] = _currentStroke!;
        emit(state.copyWith(strokes: committed));
      }
    }
    _currentStroke = null;
    _previewStrokeActive = false;
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

  void setBrush() {
    emit(state.copyWith(isEraser: false));
  }

  void undo() {
    if (state.strokes.isEmpty) return;
    final strokes = [...state.strokes];
    final removed = strokes.removeLast();
    _redoStrokes.add(removed);
    emit(state.copyWith(strokes: strokes));
  }

  void redo() {
    if (_redoStrokes.isEmpty) return;
    final strokes = [...state.strokes, _redoStrokes.removeLast()];
    emit(state.copyWith(strokes: strokes));
  }

  void clear() {
    _redoStrokes.clear();
    emit(state.copyWith(strokes: []));
  }

  void toggleToolbar() {
    emit(state.copyWith(toolbarExpanded: !state.toolbarExpanded));
  }

  void toggleFullscreen() {
    emit(state.copyWith(isFullscreen: !state.isFullscreen));
  }
}
