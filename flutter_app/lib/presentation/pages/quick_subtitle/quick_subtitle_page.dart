import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_dimensions.dart';
import '../../../domain/repositories/realtime_repository.dart';
import '../../../domain/repositories/settings_repository.dart';
import '../../../injection.dart';
import '../../cubits/quick_subtitle/quick_subtitle_cubit.dart';
import '../../cubits/quick_subtitle/quick_subtitle_state.dart';
import '../../cubits/realtime/realtime_cubit.dart';
import '../../cubits/realtime/realtime_state.dart';
import 'widgets/subtitle_category_tabs.dart';
import 'widgets/subtitle_display_card.dart';
import 'widgets/subtitle_input_bar.dart';
import 'widgets/subtitle_phrase_row.dart';

/// Quick subtitle page — main page of the app.
///
/// Original Android layout (top → bottom):
/// 1. Display card (Expanded)
/// 2. Phrase cards (horizontal, 120dp) ← toggleable
/// 3. Category tabs (😊常用|🎮游戏|📁办公|✏️) ← toggleable
/// 4. Bottom toolbar (← → 🔊 📺 ▶) + mic FAB overlapping
/// 5. Input bar (text field + ▷ send)
class QuickSubtitlePage extends StatelessWidget {
  const QuickSubtitlePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => QuickSubtitleCubit(
        realtimeRepository: getIt<RealtimeRepository>(),
        settingsRepository: getIt<SettingsRepository>(),
      )..initialize(),
      child: const _Body(),
    );
  }
}

class _Body extends StatelessWidget {
  const _Body();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<QuickSubtitleCubit, QuickSubtitleState>(
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
                            // Phrase cards
                            SubtitlePhraseRow(
                              items: selectedGroup.items,
                              selectedIndex: state.selectedItemIndex,
                              displayText: state.displayText,
                              groupId: selectedGroup.id,
                            ),
                            const SizedBox(height: 4),
                            // Category tabs
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
                // 5. Input bar
                const Padding(
                  padding: EdgeInsets.fromLTRB(12, 0, 12, 0),
                  child: SubtitleInputBar(),
                ),
                SizedBox(height: bottomPad + 8),
              ],
            ),
            // PTT Mic FAB
            Positioned(
              right: 16,
              bottom: 44 + 48 + 8 + bottomPad,
              child: const _PttMicFab(),
            ),
          ],
        );
      },
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
      height: 44,
      child: Row(
        children: [
          const SizedBox(width: 4),
          IconButton(
            icon: Icon(Icons.arrow_back_sharp, color: iconColor, size: 22),
            onPressed: cubit.navigatePrev,
            tooltip: '上一条',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 44, minHeight: 44),
          ),
          IconButton(
            icon:
                Icon(Icons.arrow_forward_sharp, color: iconColor, size: 22),
            onPressed: cubit.navigateNext,
            tooltip: '下一条',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 44, minHeight: 44),
          ),
          const Spacer(),
          IconButton(
            icon: Icon(Icons.volume_up_sharp, color: iconColor, size: 22),
            onPressed: () {},
            tooltip: '播放开关',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 44, minHeight: 44),
          ),
          // Subtitles toggle — shows/hides preset section
          IconButton(
            icon: Icon(
              presetsVisible
                  ? Icons.subtitles_sharp
                  : Icons.subtitles_off_sharp,
              color: iconColor,
              size: 22,
            ),
            onPressed: cubit.togglePresetsVisible,
            tooltip: presetsVisible ? '隐藏预设' : '显示预设',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 44, minHeight: 44),
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
                  size: 22,
                ),
                onPressed: st.displayText.isNotEmpty
                    ? cubit.sendDisplayText
                    : null,
                tooltip: '播放',
                padding: EdgeInsets.zero,
                constraints:
                    const BoxConstraints(minWidth: 44, minHeight: 44),
              );
            },
          ),
          const SizedBox(width: 56), // Space for mic FAB
        ],
      ),
    );
  }
}

/// PTT Mic FAB — large teal circle with mic icon.
class _PttMicFab extends StatelessWidget {
  const _PttMicFab();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<RealtimeCubit, RealtimeState>(
      buildWhen: (p, c) =>
          p.running != c.running ||
          p.pttPressed != c.pttPressed ||
          p.loading != c.loading,
      builder: (context, state) {
        final isRecording = state.pttPressed;
        final cubit = context.read<RealtimeCubit>();

        return SizedBox(
          width: 64,
          height: 64,
          child: GestureDetector(
            onLongPressStart: (_) => cubit.setPttPressed(true),
            onLongPressEnd: (_) => cubit.setPttPressed(false),
            child: FloatingActionButton.large(
              heroTag: 'pttMic',
              onPressed: state.loading ? null : () => cubit.toggle(),
              backgroundColor:
                  isRecording ? Colors.red : AppColors.primary,
              elevation: AppDimensions.elevationFab,
              shape: const CircleBorder(),
              tooltip: isRecording ? '松开发送' : '按住说话',
              child: state.loading
                  ? const SizedBox(
                      width: 28,
                      height: 28,
                      child: CircularProgressIndicator(
                        color: Colors.white,
                        strokeWidth: 2.5,
                      ),
                    )
                  : const Icon(
                      Icons.mic_sharp,
                      color: Colors.white,
                      size: 32,
                    ),
            ),
          ),
        );
      },
    );
  }
}
