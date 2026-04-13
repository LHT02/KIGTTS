import 'package:flutter/material.dart';
import '../../../../domain/entities/draw_stroke.dart';

/// Custom drawing canvas that renders strokes and captures touch input.
class DrawingCanvas extends StatefulWidget {
  const DrawingCanvas({
    super.key,
    required this.strokes,
    required this.boardColor,
    required this.onPanStart,
    required this.onPanUpdate,
    required this.onPanEnd,
  });

  final List<DrawStroke> strokes;
  final Color boardColor;
  final ValueChanged<Offset> onPanStart;
  final ValueChanged<Offset> onPanUpdate;
  final VoidCallback onPanEnd;

  @override
  State<DrawingCanvas> createState() => _DrawingCanvasState();
}

class _DrawingCanvasState extends State<DrawingCanvas> {
  final Set<int> _activePointers = <int>{};
  int? _drawingPointer;
  bool _strokeActive = false;

  void _endStrokeIfNeeded() {
    if (_strokeActive) {
      widget.onPanEnd();
      _strokeActive = false;
    }
  }

  void _onPointerDown(PointerDownEvent event) {
    _activePointers.add(event.pointer);
    if (_activePointers.length == 1) {
      _drawingPointer = event.pointer;
      _strokeActive = true;
      widget.onPanStart(event.localPosition);
      return;
    }

    // Two-finger gesture starts: stop current stroke and allow zoom/pan.
    _drawingPointer = null;
    _endStrokeIfNeeded();
  }

  void _onPointerMove(PointerMoveEvent event) {
    if (_drawingPointer != event.pointer) return;
    if (_activePointers.length != 1) return;
    widget.onPanUpdate(event.localPosition);
  }

  void _onPointerUpOrCancel(PointerEvent event) {
    _activePointers.remove(event.pointer);
    if (_drawingPointer == event.pointer) {
      _drawingPointer = null;
      _endStrokeIfNeeded();
    }
  }

  @override
  Widget build(BuildContext context) {
    return AspectRatio(
      aspectRatio: 1080 / 1920,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Listener(
          onPointerDown: _onPointerDown,
          onPointerMove: _onPointerMove,
          onPointerUp: _onPointerUpOrCancel,
          onPointerCancel: _onPointerUpOrCancel,
          child: CustomPaint(
            painter: _StrokePainter(
              strokes: widget.strokes,
              boardColor: widget.boardColor,
            ),
            size: Size.infinite,
          ),
        ),
      ),
    );
  }
}

class _StrokePainter extends CustomPainter {
  _StrokePainter({
    required this.strokes,
    required this.boardColor,
  });

  final List<DrawStroke> strokes;
  final Color boardColor;

  @override
  void paint(Canvas canvas, Size size) {
    // Draw board background
    canvas.drawRect(
      Offset.zero & size,
      Paint()..color = boardColor,
    );

    // Draw each stroke
    for (final stroke in strokes) {
      if (stroke.points.length < 2) continue;

      final paint = Paint()
        ..color = stroke.isEraser
            ? boardColor
            : Color(stroke.color)
        ..strokeWidth = stroke.strokeWidth
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..style = PaintingStyle.stroke
        ..isAntiAlias = true;

      if (stroke.isEraser) {
        paint.blendMode = BlendMode.src;
      }

      final path = Path();
      path.moveTo(stroke.points.first.x, stroke.points.first.y);
      for (var i = 1; i < stroke.points.length; i++) {
        path.lineTo(stroke.points[i].x, stroke.points[i].y);
      }
      canvas.drawPath(path, paint);
    }
  }

  @override
  bool shouldRepaint(covariant _StrokePainter oldDelegate) {
    return oldDelegate.strokes != strokes ||
        oldDelegate.boardColor != boardColor;
  }
}
