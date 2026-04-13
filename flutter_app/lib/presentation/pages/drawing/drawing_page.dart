import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../cubits/drawing/drawing_cubit.dart';
import '../../cubits/drawing/drawing_state.dart';
import 'widgets/drawing_canvas.dart';
import 'widgets/drawing_toolbar.dart';

/// Drawing canvas page (P1) with adaptive toolbar.
/// Canvas uses 1080:1920 aspect ratio, supports zoom/pan.
class DrawingPage extends StatelessWidget {
  const DrawingPage({super.key, this.fullscreen = false});

  final bool fullscreen;

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => DrawingCubit(),
      child: Material(
        color: Colors.transparent,
        child: _DrawingView(fullscreen: fullscreen),
      ),
    );
  }
}

class _DrawingView extends StatefulWidget {
  const _DrawingView({required this.fullscreen});

  final bool fullscreen;

  @override
  State<_DrawingView> createState() => _DrawingViewState();
}

class _DrawingViewState extends State<_DrawingView> {
  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final boardColor = isDark
        ? const Color(0xFF2C3237)
        : const Color(0xFFFCFDFE);

    return OrientationBuilder(
      builder: (context, orientation) {
        final isLandscape = orientation == Orientation.landscape;
        return LayoutBuilder(
          builder: (context, constraints) {
            final panelWidth = (constraints.maxWidth - 40)
                .clamp(230.0, 320.0)
                .toDouble();

            return BlocBuilder<DrawingCubit, DrawingState>(
              builder: (context, state) {
                final cubit = context.read<DrawingCubit>();

                final canvas = Padding(
                  padding: const EdgeInsets.all(8),
                  child: InteractiveViewer(
                    minScale: 1.0,
                    maxScale: 3.5,
                    panEnabled: true,
                    scaleEnabled: true,
                    child: DrawingCanvas(
                      strokes: state.strokes,
                      boardColor: boardColor,
                      onPanStart: (pos) => cubit.startStroke(pos.dx, pos.dy),
                      onPanUpdate: (pos) => cubit.addPoint(pos.dx, pos.dy),
                      onPanEnd: () => cubit.endStroke(),
                    ),
                  ),
                );

                final floatingTools = Positioned(
                  left: 16,
                  right: 16,
                  bottom: 16,
                  child: Align(
                    alignment: Alignment.bottomCenter,
                    child: DrawingToolbar(
                      compact: isLandscape,
                      width: panelWidth,
                    ),
                  ),
                );

                final floatingStrokeBar = Positioned(
                  left: 16,
                  right: 16,
                  bottom: 86,
                  child: Align(
                    alignment: Alignment.bottomCenter,
                    child: _StrokeWidthBar(
                      width: panelWidth,
                      value: state.strokeWidth,
                      onChanged: cubit.setStrokeWidth,
                    ),
                  ),
                );

                if (widget.fullscreen) {
                  return SafeArea(
                    child: Stack(
                      children: [
                        Positioned.fill(child: canvas),
                        Positioned(
                          top: 14,
                          left: 14,
                          child: _FloatingIconButton(
                            icon: Icons.arrow_back_sharp,
                            tooltip: '返回',
                            onPressed: () => Navigator.of(context).maybePop(),
                          ),
                        ),
                        floatingStrokeBar,
                        floatingTools,
                      ],
                    ),
                  );
                }

                return Stack(
                  children: [
                    Positioned.fill(child: canvas),
                    floatingStrokeBar,
                    floatingTools,
                  ],
                );
              },
            );
          },
        );
      },
    );
  }
}

class _StrokeWidthBar extends StatelessWidget {
  const _StrokeWidthBar({
    required this.width,
    required this.value,
    required this.onChanged,
  });

  final double width;
  final double value;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: theme.colorScheme.surface.withValues(alpha: 0.9),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: theme.colorScheme.outline.withValues(alpha: 0.22),
        ),
      ),
      child: SizedBox(
        width: width,
        child: Row(
          children: [
            const SizedBox(width: 10),
            Icon(
              Icons.line_weight_sharp,
              size: 18,
              color: theme.colorScheme.onSurfaceVariant,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Slider(
                value: value,
                min: 2,
                max: 24,
                divisions: 11,
                label: '${value.toStringAsFixed(0)} px',
                onChanged: onChanged,
              ),
            ),
            const SizedBox(width: 8),
          ],
        ),
      ),
    );
  }
}

class _FloatingIconButton extends StatelessWidget {
  const _FloatingIconButton({
    required this.icon,
    required this.tooltip,
    required this.onPressed,
  });

  final IconData icon;
  final String tooltip;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: theme.colorScheme.surface.withValues(alpha: 0.9),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: theme.colorScheme.outline.withValues(alpha: 0.22),
        ),
      ),
      child: IconButton(
        icon: Icon(icon, color: AppColors.primary),
        onPressed: onPressed,
        tooltip: tooltip,
      ),
    );
  }
}
