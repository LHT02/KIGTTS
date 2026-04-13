import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../cubits/realtime/realtime_cubit.dart';
import '../../cubits/realtime/realtime_state.dart';
import 'widgets/recognized_list.dart';
import 'widgets/running_controls.dart';
import 'widgets/status_bar.dart';

/// Real-time ASR/TTS conversion page (P0).
/// RealtimeCubit is provided at the app level (see app.dart).
class RealtimePage extends StatelessWidget {
  const RealtimePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocListener<RealtimeCubit, RealtimeState>(
      listenWhen: (p, c) => c.error != null && p.error != c.error,
      listener: (context, state) {
        _showDebugErrorDialog(context, state);
      },
      child: const _RealtimeView(),
    );
  }

  void _showDebugErrorDialog(BuildContext context, RealtimeState state) {
    final details = StringBuffer();
    details.writeln('Error: ${state.error}');
    details.writeln('');
    details.writeln('Status: ${state.status}');
    details.writeln('Running: ${state.running}');
    details.writeln('ASR dir: ${state.currentAsrDir ?? "null (NOT LOADED)"}');
    details
        .writeln('Voice dir: ${state.currentVoiceDir ?? "null (NOT LOADED)"}');
    details.writeln('Loading: ${state.loading}');
    details.writeln('Recording: ${state.recording}');
    details.writeln('PTT mode: ${state.pttMode}');

    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Row(
          children: [
            Icon(Icons.bug_report, color: Theme.of(ctx).colorScheme.error),
            const SizedBox(width: 8),
            const Text('Debug: Error'),
          ],
        ),
        content: SingleChildScrollView(
          child: SelectableText(
            details.toString(),
            style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              context.read<RealtimeCubit>().clearError();
            },
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }
}

class _RealtimeView extends StatelessWidget {
  const _RealtimeView();

  @override
  Widget build(BuildContext context) {
    return const Column(
      children: [
        StatusBar(),
        Expanded(child: RecognizedList()),
        RunningControls(),
      ],
    );
  }
}
