import 'package:flutter/material.dart';
import '../../../../domain/entities/draw_stroke.dart';

/// Custom drawing canvas that renders strokes and captures touch input.
class DrawingCanvas extends StatelessWidget {
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
  Widget build(BuildContext context) {
    return AspectRatio(
      aspectRatio: 1080 / 1920,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: GestureDetector(
          onPanStart: (details) => onPanStart(details.localPosition),
          onPanUpdate: (details) => onPanUpdate(details.localPosition),
          onPanEnd: (_) => onPanEnd(),
          child: CustomPaint(
            painter: _StrokePainter(
              strokes: strokes,
              boardColor: boardColor,
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
