import 'package:flutter/material.dart';
import 'widgets/recognized_list.dart';
import 'widgets/running_controls.dart';
import 'widgets/status_bar.dart';

/// Real-time ASR/TTS conversion page (P0).
/// RealtimeCubit is provided at the app level (see app.dart).
class RealtimePage extends StatelessWidget {
  const RealtimePage({super.key});

  @override
  Widget build(BuildContext context) {
    return const _RealtimeView();
  }
}

class _RealtimeView extends StatelessWidget {
  const _RealtimeView();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const StatusBar(),
        const Expanded(child: RecognizedList()),
        const RunningControls(),
      ],
    );
  }
}
