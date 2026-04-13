import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../cubits/drawing/drawing_cubit.dart';
import '../../../cubits/drawing/drawing_state.dart';

/// Floating toolbar for drawing page.
/// Color picker opens a small popup panel to avoid horizontal overflow.
class DrawingToolbar extends StatelessWidget {
  const DrawingToolbar({
    super.key,
    this.compact = false,
    this.width,
  });

  final bool compact;
  final double? width;

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
    final theme = Theme.of(context);
    return BlocBuilder<DrawingCubit, DrawingState>(
      builder: (context, state) {
        final cubit = context.read<DrawingCubit>();
        final iconColor = theme.colorScheme.onSurfaceVariant;
        final brushColor = !state.isEraser
            ? Color(state.currentColor)
            : iconColor;
        final eraserColor = state.isEraser ? AppColors.primary : iconColor;
        final canRedo = cubit.canRedo;

        return SizedBox(
          width: width,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: theme.colorScheme.surface.withValues(alpha: 0.9),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: theme.colorScheme.outline.withValues(alpha: 0.22),
              ),
            ),
            child: Padding(
              padding: EdgeInsets.symmetric(
                horizontal: compact ? 6 : 8,
                vertical: 4,
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  _PaletteButton(
                    selectedColor: state.currentColor,
                    onSelected: cubit.setColor,
                  ),
                  _ToolButton(
                    icon: _SymbolLigatureIcon('edit', color: brushColor),
                    color: brushColor,
                    selected: !state.isEraser,
                    onPressed: cubit.setBrush,
                    tooltip: '画笔',
                  ),
                  _ToolButton(
                    icon: _SymbolLigatureIcon('ink_eraser', color: eraserColor),
                    color: eraserColor,
                    selected: state.isEraser,
                    onPressed: cubit.toggleEraser,
                    tooltip: '橡皮擦',
                  ),
                  _ToolButton(
                    icon: const Icon(Icons.undo_sharp),
                    color: iconColor,
                    onPressed: state.strokes.isNotEmpty ? cubit.undo : null,
                    tooltip: '撤销',
                  ),
                  _ToolButton(
                    icon: const Icon(Icons.redo_sharp),
                    color: iconColor,
                    onPressed: canRedo ? cubit.redo : null,
                    tooltip: '重做',
                  ),
                  _ToolButton(
                    icon: const Icon(Icons.delete_sweep_sharp),
                    color: iconColor,
                    onPressed: state.strokes.isNotEmpty ? cubit.clear : null,
                    tooltip: '清空',
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _PaletteButton extends StatelessWidget {
  const _PaletteButton({
    required this.selectedColor,
    required this.onSelected,
  });

  final int selectedColor;
  final ValueChanged<int> onSelected;

  Future<void> _showColorPicker(BuildContext context) async {
    final selected = await showDialog<int>(
      context: context,
      barrierColor: Colors.black26,
      builder: (dialogContext) {
        final theme = Theme.of(dialogContext);
        return Dialog(
          insetPadding: const EdgeInsets.symmetric(horizontal: 56, vertical: 140),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '选择颜色',
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: DrawingToolbar._colors
                      .map(
                        (c) => _ColorDot(
                          color: c,
                          isSelected: selectedColor == c,
                          onTap: () => Navigator.of(dialogContext).pop(c),
                        ),
                      )
                      .toList(),
                ),
              ],
            ),
          ),
        );
      },
    );

    if (selected != null) {
      onSelected(selected);
    }
  }

  @override
  Widget build(BuildContext context) {
    return _ToolButton(
      icon: const Icon(Icons.palette_sharp),
      color: Color(selectedColor),
      tooltip: '选色',
      onPressed: () => _showColorPicker(context),
    );
  }
}

class _ToolButton extends StatelessWidget {
  const _ToolButton({
    required this.icon,
    required this.color,
    required this.tooltip,
    this.selected = false,
    this.onPressed,
  });

  final Widget icon;
  final Color color;
  final String tooltip;
  final bool selected;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      padding: EdgeInsets.zero,
      constraints: const BoxConstraints.tightFor(width: 40, height: 40),
      style: IconButton.styleFrom(
        backgroundColor: selected
            ? AppColors.primary.withValues(alpha: 0.12)
            : null,
      ),
      icon: IconTheme(
        data: IconThemeData(color: color),
        child: icon,
      ),
      onPressed: onPressed,
      tooltip: tooltip,
    );
  }
}

class _SymbolLigatureIcon extends StatelessWidget {
  const _SymbolLigatureIcon(this.name, {required this.color});

  final String name;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Text(
      name,
      style: TextStyle(
        fontFamily: 'MaterialSymbolsSharp',
        fontSize: 22,
        color: color,
        height: 1,
      ),
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
