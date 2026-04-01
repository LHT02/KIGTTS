import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_dimensions.dart';
import '../../../injection.dart';
import '../../cubits/settings/settings_cubit.dart';
import '../../cubits/settings/settings_state.dart';
import 'widgets/audio_settings_section.dart';
import 'widgets/piper_settings_section.dart';
import 'widgets/playback_settings_section.dart';
import 'widgets/ui_settings_section.dart';

/// Application settings page (P0).
class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => getIt<SettingsCubit>()..initialize(),
      child: const _SettingsView(),
    );
  }
}

class _SettingsView extends StatelessWidget {
  const _SettingsView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SettingsCubit, SettingsState>(
      buildWhen: (p, c) => p.loading != c.loading,
      builder: (context, state) {
        if (state.loading) {
          return const Center(child: CircularProgressIndicator());
        }
        return ListView(
          padding: const EdgeInsets.only(
            top: AppDimensions.pageTopBlank,
            bottom: AppDimensions.pageBottomBlank,
          ),
          children: const [
            AudioSettingsSection(),
            PiperSettingsSection(),
            PlaybackSettingsSection(),
            UiSettingsSection(),
          ],
        );
      },
    );
  }
}
