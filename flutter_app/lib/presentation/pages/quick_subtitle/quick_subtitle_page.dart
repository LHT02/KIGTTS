import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/repositories/realtime_repository.dart';
import '../../../domain/repositories/settings_repository.dart';
import '../../../injection.dart';
import '../../cubits/quick_subtitle/quick_subtitle_cubit.dart';
import '../../cubits/quick_subtitle/quick_subtitle_state.dart';
import '../../cubits/realtime/realtime_cubit.dart';
import '../../cubits/realtime/realtime_state.dart';
import '../../cubits/settings/settings_cubit.dart';
import '../../widgets/top_bar_actions.dart';
import 'widgets/quick_subtitle_ptt_overlay.dart';
import 'widgets/subtitle_category_tabs.dart';
import 'widgets/subtitle_display_card.dart';
import 'widgets/subtitle_fullscreen_dialog.dart';
import 'widgets/subtitle_input_bar.dart';
import 'widgets/subtitle_phrase_row.dart';

/// Quick subtitle page — main page of the app.
///
/// Layout (top → bottom):
/// 1. Display card (Expanded)
/// 2. Phrase cards (horizontal, 88dp) ← toggleable
/// 3. Category tabs ← toggleable
/// 4. Bottom toolbar (← → 🔊 📺 ▶)
/// 5. Input bar (keyboard / mic mode toggle)
class QuickSubtitlePage extends StatelessWidget {
  const QuickSubtitlePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => QuickSubtitleCubit(
        realtimeRepository: getIt<RealtimeRepository>(),
        settingsRepository: getIt<SettingsRepository>(),
        realtimeCubitGetter: () => context.read<RealtimeCubit>(),
      )..initialize(),
      child: const _Body(),
    );
  }
}

class _Body extends StatefulWidget {
  const _Body();

  @override
  State<_Body> createState() => _BodyState();
}

class _BodyState extends State<_Body> {
  bool _showPttOverlay = false;
  bool _callbackRegistered = false;

  @override
  void initState() {
    super.initState();
    fullscreenActionNotifier.addListener(_onFullscreen);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_callbackRegistered) {
      _callbackRegistered = true;
      // Connect ASR results → subtitle display
      final realtimeCubit = context.read<RealtimeCubit>();
      realtimeCubit.onAsrResultForSubtitle = (text) {
        if (!mounted) return;
        final settings = context.read<SettingsCubit>().state.settings;
        if (!settings.asrSendToQuickSubtitle) return;
        // Only update the display text — do NOT call sendText() here.
        // TTS playback of ASR results is handled by the native
        // RealtimeController (it auto-enqueues TTS after recognition).
        // Calling sendText() from Flutter would double-enqueue and
        // block the ASR pipeline while TTS plays.
        context.read<QuickSubtitleCubit>().setDisplayText(text);
      };
    }
  }

  @override
  void dispose() {
    fullscreenActionNotifier.removeListener(_onFullscreen);
    // Unregister callback to avoid dangling reference
    try {
      context.read<RealtimeCubit>().onAsrResultForSubtitle = null;
    } catch (_) {}
    super.dispose();
  }

  void _onFullscreen() {
    final cubit = context.read<QuickSubtitleCubit>();
    final st = cubit.state;
    SubtitleFullscreenDialog.show(
      context,
      text: st.displayText,
      bold: st.config.bold,
      centered: st.config.centered,
      fontSize: st.config.fontSize,
    );
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<RealtimeCubit, RealtimeState>(
      listenWhen: (p, c) => p.error != c.error && c.error != null,
      listener: (context, state) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(state.error!),
            duration: const Duration(seconds: 4),
            action: SnackBarAction(
              label: '关闭',
              onPressed: () {
                context.read<RealtimeCubit>().clearError();
              },
            ),
          ),
        );
      },
      child: BlocBuilder<QuickSubtitleCubit, QuickSubtitleState>(
      builder: (context, state) {
        if (state.loading) {
          return const Center(child: CircularProgressIndicator());
        }

        final groups = state.config.groups;
        final selectedGroup =
            groups.isNotEmpty && state.selectedGroupIndex < groups.length
                ? groups[state.selectedGroupIndex]
                : null;

        final bottomPad = MediaQuery.paddingOf(context).bottom;

        return Stack(
          children: [
            // Main content column
            Column(
              children: [
                // 1. Display card
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(12, 4, 12, 0),
                    child: SubtitleDisplayCard(
                      displayText: state.displayText,
                      fontSize: state.config.fontSize,
                      bold: state.config.bold,
                      centered: state.config.centered,
                    ),
                  ),
                ),
                // 2 + 3. Preset section (animated show/hide)
                AnimatedSize(
                  duration: const Duration(milliseconds: 200),
                  curve: Curves.fastOutSlowIn,
                  alignment: Alignment.topCenter,
                  child: state.presetsVisible && selectedGroup != null
                      ? Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const SizedBox(height: 4),
                            SubtitlePhraseRow(
                              items: selectedGroup.items,
                              selectedIndex: state.selectedItemIndex,
                              displayText: state.displayText,
                              groupId: selectedGroup.id,
                            ),
                            const SizedBox(height: 4),
                            SubtitleCategoryTabs(
                              groups: groups,
                              selectedIndex: state.selectedGroupIndex,
                            ),
                          ],
                        )
                      : const SizedBox.shrink(),
                ),
                // 4. Bottom toolbar
                _BottomToolbar(presetsVisible: state.presetsVisible),
                // 5. Input bar (with integrated PTT + continuous listen)
                Padding(
                  padding: const EdgeInsets.fromLTRB(8, 0, 8, 0),
                  child: SubtitleInputBar(
                    onPttOverlayChanged: (show) {
                      setState(() => _showPttOverlay = show);
                    },
                  ),
                ),
                SizedBox(height: bottomPad + 6),
              ],
            ),
            // Confirm-PTT overlay (full-screen, above everything)
            if (_showPttOverlay)
              const Positioned.fill(
                child: QuickSubtitlePttOverlay(),
              ),
          ],
        );
      },
    ),
    );
  }
}

/// Bottom toolbar: ← → [spacer] 🔊 📺 ▶
/// 📺 toggles presets section visible/hidden.
class _BottomToolbar extends StatelessWidget {
  const _BottomToolbar({required this.presetsVisible});
  final bool presetsVisible;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final iconColor = theme.colorScheme.onSurfaceVariant;
    final cubit = context.read<QuickSubtitleCubit>();

    return SizedBox(
      height: 40,
      child: Row(
        children: [
          const SizedBox(width: 4),
          IconButton(
            icon: Icon(Icons.arrow_back_sharp, color: iconColor, size: 20),
            onPressed: cubit.navigatePrev,
            tooltip: '上一条',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
          ),
          IconButton(
            icon: Icon(Icons.arrow_forward_sharp, color: iconColor, size: 20),
            onPressed: cubit.navigateNext,
            tooltip: '下一条',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
          ),
          const Spacer(),
          IconButton(
            icon: Icon(Icons.volume_up_sharp, color: iconColor, size: 20),
            onPressed: () {},
            tooltip: '播放开关',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
          ),
          IconButton(
            icon: Icon(
              presetsVisible
                  ? Icons.subtitles_sharp
                  : Icons.subtitles_off_sharp,
              color: iconColor,
              size: 20,
            ),
            onPressed: cubit.togglePresetsVisible,
            tooltip: presetsVisible ? '隐藏预设' : '显示预设',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
          ),
          BlocBuilder<QuickSubtitleCubit, QuickSubtitleState>(
            buildWhen: (p, c) => p.displayText != c.displayText,
            builder: (context, st) {
              return IconButton(
                icon: Icon(
                  Icons.play_arrow_sharp,
                  color: st.displayText.isNotEmpty
                      ? iconColor
                      : iconColor.withValues(alpha: 0.38),
                  size: 20,
                ),
                onPressed: st.displayText.isNotEmpty
                    ? cubit.sendDisplayText
                    : null,
                tooltip: '播放',
                padding: EdgeInsets.zero,
                constraints:
                    const BoxConstraints(minWidth: 40, minHeight: 40),
              );
            },
          ),
          const SizedBox(width: 4),
        ],
      ),
    );
  }
}
