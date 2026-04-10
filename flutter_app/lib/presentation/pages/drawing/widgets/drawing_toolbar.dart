import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../cubits/drawing/drawing_cubit.dart';
import '../../../cubits/drawing/drawing_state.dart';

/// Toolbar for drawing page with color picker, brush/eraser, undo, clear.
/// Adapts layout for landscape (vertical) and portrait (horizontal).
class DrawingToolbar extends StatelessWidget {
  const DrawingToolbar({
    super.key,
    required this.isLandscape,
  });

  final bool isLandscape;

  static const _colors = [
    0xFF80DEEA, // cyan
    0xFFFF7043, // orange
    0xFF66BB6A, // green
    0xFFEF5350, // red
    0xFFAB47BC, // purple
    0xFFFFEE58, // yellow
    0xFFFFFFFF, // white
    0xFF000000, // black
  ];

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<DrawingCubit, DrawingState>(
      builder: (context, state) {
        final cubit = context.read<DrawingCubit>();
        final children = [
          // Color swatches
          ..._colors.map((c) => _ColorDot(
                color: c,
                isSelected: !state.isEraser && state.currentColor == c,
                onTap: () => cubit.setColor(c),
              )),
          const SizedBox(width: 8, height: 8),
          // Brush
          IconButton(
            icon: Icon(
              Icons.brush_sharp,
              color: !state.isEraser ? AppColors.primary : null,
            ),
            onPressed: cubit.setBrush,
            tooltip: '画笔',
          ),
          // Eraser
          IconButton(
            icon: Icon(
              Icons.auto_fix_high_sharp,
              color: state.isEraser ? AppColors.primary : null,
            ),
            onPressed: cubit.toggleEraser,
            tooltip: '橡皮擦',
          ),
          // Undo
          IconButton(
            icon: const Icon(Icons.undo_sharp),
            onPressed: state.strokes.isNotEmpty ? cubit.undo : null,
            tooltip: '撤销',
          ),
          // Clear
          IconButton(
            icon: const Icon(Icons.delete_outline_sharp),
            onPressed: state.strokes.isNotEmpty ? cubit.clear : null,
            tooltip: '清除',
          ),
        ];

        if (isLandscape) {
          return SizedBox(
            width: 56,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: children,
              ),
            ),
          );
        }
        return SizedBox(
          height: 56,
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: children,
            ),
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
        margin: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          color: Color(color),
          shape: BoxShape.circle,
          border: isSelected
              ? Border.all(color: AppColors.primary, width: 2.5)
              : Border.all(
                  color: Colors.grey.withValues(alpha: 0.3),
                  width: 1,
                ),
        ),
      ),
    );
  }
}
