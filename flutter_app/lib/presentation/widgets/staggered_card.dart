import 'package:flutter/material.dart';

/// Wrapper that applies a staggered fade+slide entrance animation.
/// [index] determines the delay offset for the stagger effect.
class StaggeredCard extends StatefulWidget {
  const StaggeredCard({
    super.key,
    required this.index,
    required this.child,
  });

  final int index;
  final Widget child;

  @override
  State<StaggeredCard> createState() => _StaggeredCardState();
}

class _StaggeredCardState extends State<StaggeredCard>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );

    final delay = (widget.index * 0.15).clamp(0.0, 0.6);
    final curve = CurvedAnimation(
      parent: _controller,
      curve: Interval(delay, 1.0, curve: Curves.easeOut),
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(curve);
    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.08),
      end: Offset.zero,
    ).animate(curve);

    _controller.forward();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _fadeAnimation,
      child: SlideTransition(
        position: _slideAnimation,
        child: widget.child,
      ),
    );
  }
}
