import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../cubits/drawing/drawing_cubit.dart';
import '../../cubits/drawing/drawing_state.dart';
import 'widgets/drawing_canvas.dart';
import 'widgets/drawing_toolbar.dart';

/// Drawing canvas page (P1) with adaptive toolbar.
/// Canvas uses 1080:1920 aspect ratio, supports zoom/pan.
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
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final boardColor = isDark
        ? const Color(0xFF2C3237)
        : const Color(0xFFFCFDFE);

    return OrientationBuilder(
      builder: (context, orientation) {
        final isLandscape = orientation == Orientation.landscape;
        return BlocBuilder<DrawingCubit, DrawingState>(
          builder: (context, state) {
            final cubit = context.read<DrawingCubit>();

            final canvas = Expanded(
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: InteractiveViewer(
                  minScale: 1.0,
                  maxScale: 3.5,
                  child: DrawingCanvas(
                    strokes: state.strokes,
                    boardColor: boardColor,
                    onPanStart: (pos) =>
                        cubit.startStroke(pos.dx, pos.dy),
                    onPanUpdate: (pos) =>
                        cubit.addPoint(pos.dx, pos.dy),
                    onPanEnd: () => cubit.endStroke(),
                  ),
                ),
              ),
            );

            final toolbar = DrawingToolbar(isLandscape: isLandscape);

            if (isLandscape) {
              return Row(
                children: [
                  canvas,
                  toolbar,
                ],
              );
            }
            return Column(
              children: [
                canvas,
                toolbar,
              ],
            );
          },
        );
      },
    );
  }
}
