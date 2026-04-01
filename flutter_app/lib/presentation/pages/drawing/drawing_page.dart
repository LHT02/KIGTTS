import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_dimensions.dart';
import '../../../domain/entities/draw_stroke.dart';
import '../../cubits/drawing/drawing_cubit.dart';
import '../../cubits/drawing/drawing_state.dart';

/// Drawing canvas page (P1).
class DrawingPage extends StatelessWidget {
  const DrawingPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => DrawingCubit(),
      child: const _DrawingView(),
    );
  }
}

class _DrawingView extends StatelessWidget {
  const _DrawingView();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const _DrawingToolbar(),
        Expanded(
          child: BlocBuilder<DrawingCubit, DrawingState>(
            builder: (context, state) {
              return GestureDetector(
                onPanStart: (d) {
                  final pos = d.localPosition;
                  context
                      .read<DrawingCubit>()
                      .startStroke(pos.dx, pos.dy);
                },
                onPanUpdate: (d) {
                  final pos = d.localPosition;
                  context
                      .read<DrawingCubit>()
                      .addPoint(pos.dx, pos.dy);
                },
                onPanEnd: (_) =>
                    context.read<DrawingCubit>().endStroke(),
                child: CustomPaint(
                  painter: _StrokePainter(strokes: state.strokes),
                  size: Size.infinite,
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _DrawingToolbar extends StatelessWidget {
  const _DrawingToolbar();

  static const _colors = [
    0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00,
    0xFF0000FF, 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF,
  ];

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<DrawingCubit, DrawingState>(
      buildWhen: (p, c) =>
          p.currentColor != c.currentColor ||
          p.isEraser != c.isEraser ||
          p.strokeWidth != c.strokeWidth,
      builder: (context, state) {
        final cubit = context.read<DrawingCubit>();
        return Container(
          padding: const EdgeInsets.symmetric(
            horizontal: AppDimensions.spacingMd,
            vertical: AppDimensions.spacingSm,
          ),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surface,
            border: Border(
              bottom: BorderSide(
                color: Theme.of(context)
                    .colorScheme
                    .outline
                    .withValues(alpha: 0.2),
              ),
            ),
          ),
          child: Row(
            children: [
              ..._colors.map(
                (c) => _ColorDot(
                  color: c,
                  isSelected:
                      state.currentColor == c && !state.isEraser,
                  onTap: () => cubit.setColor(c),
                ),
              ),
              const SizedBox(width: 8),
              IconButton(
                icon: Icon(
                  state.isEraser
                      ? Icons.auto_fix_high
                      : Icons.auto_fix_off,
                  color: state.isEraser ? AppColors.primary : null,
                ),
                onPressed: cubit.toggleEraser,
                tooltip: '橡皮擦',
              ),
              IconButton(
                icon: const Icon(Icons.undo),
                onPressed: cubit.undo,
                tooltip: '撤销',
              ),
              IconButton(
                icon: const Icon(Icons.delete_outline),
                onPressed: cubit.clear,
                tooltip: '清除',
              ),
            ],
          ),
        );
      },
    );
  }
}

class _ColorDot extends StatelessWidget {
  const _ColorDot({
    required this.color,
    required this.isSelected,
    required this.onTap,
  });

  final int color;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 28,
        height: 28,
        margin: const EdgeInsets.symmetric(horizontal: 2),
        decoration: BoxDecoration(
          color: Color(color),
          shape: BoxShape.circle,
          border: Border.all(
            color: isSelected ? AppColors.primary : Colors.transparent,
            width: 2,
          ),
        ),
      ),
    );
  }
}

class _StrokePainter extends CustomPainter {
  _StrokePainter({required this.strokes});
  final List<DrawStroke> strokes;

  @override
  void paint(Canvas canvas, Size size) {
    for (final stroke in strokes) {
      if (stroke.points.length < 2) continue;
      final paint = Paint()
        ..color = stroke.isEraser ? Colors.white : Color(stroke.color)
        ..strokeWidth = stroke.strokeWidth
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..style = PaintingStyle.stroke;

      final path = ui.Path();
      path.moveTo(stroke.points.first.x, stroke.points.first.y);
      for (var i = 1; i < stroke.points.length; i++) {
        path.lineTo(stroke.points[i].x, stroke.points[i].y);
      }
      canvas.drawPath(path, paint);
    }
  }

  @override
  bool shouldRepaint(_StrokePainter old) => true;
}
