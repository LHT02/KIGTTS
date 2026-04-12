import 'dart:convert';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/constants/prefs_keys.dart';
import '../../../domain/entities/quick_subtitle.dart';
import '../../../domain/repositories/realtime_repository.dart';
import '../../../domain/repositories/settings_repository.dart';
import '../realtime/realtime_cubit.dart';
import 'quick_subtitle_state.dart';

/// Cubit managing quick subtitle groups and TTS playback.
class QuickSubtitleCubit extends Cubit<QuickSubtitleState> {
  QuickSubtitleCubit({
    required RealtimeRepository realtimeRepository,
    required SettingsRepository settingsRepository,
    RealtimeCubit Function()? realtimeCubitGetter,
  })  : _realtimeRepo = realtimeRepository,
        _settingsRepo = settingsRepository,
        _realtimeCubitGetter = realtimeCubitGetter,
        super(const QuickSubtitleState());

  final RealtimeRepository _realtimeRepo;
  final SettingsRepository _settingsRepo;
  final RealtimeCubit Function()? _realtimeCubitGetter;

  Future<void> initialize() async {
    emit(state.copyWith(loading: true));
    try {
      final json = await _settingsRepo.getJsonSetting(
        PrefsKeys.quickSubtitleConfig,
      );
      QuickSubtitleConfig config;
      if (json != null && json.isNotEmpty) {
        config = QuickSubtitleConfig.fromJson(
          jsonDecode(json) as Map<String, dynamic>,
        );
        // If saved config has no groups (stale from before defaults),
        // replace with built-in defaults.
        if (config.groups.isEmpty) {
          config = defaultQuickSubtitleConfig();
        }
      } else {
        // First launch — use built-in defaults
        config = defaultQuickSubtitleConfig();
      }
      emit(state.copyWith(
        config: config,
        loading: false,
        // Auto-select first item of first group
        displayText: config.groups.isNotEmpty &&
                config.groups.first.items.isNotEmpty
            ? config.groups.first.items.first.text
            : '',
        selectedItemIndex:
            config.groups.isNotEmpty &&
                    config.groups.first.items.isNotEmpty
                ? 0
                : -1,
      ));
      // Persist defaults if was empty
      if (json == null || json.isEmpty) {
        await _persist();
      }
    } catch (e) {
      emit(state.copyWith(loading: false, error: e.toString()));
    }
  }

  Future<void> _persist() async {
    try {
      await _settingsRepo.setJsonSetting(
        PrefsKeys.quickSubtitleConfig,
        jsonEncode(state.config.toJson()),
      );
    } catch (_) {}
  }

  void selectGroup(int index) {
    emit(state.copyWith(selectedGroupIndex: index, selectedItemIndex: -1));
  }

  void setInputText(String text) {
    emit(state.copyWith(inputText: text));
  }

  void setDisplayText(String text) {
    emit(state.copyWith(displayText: text));
  }

  void selectItem(int index) {
    final groups = state.config.groups;
    if (state.selectedGroupIndex >= groups.length) return;
    final items = groups[state.selectedGroupIndex].items;
    if (index < 0 || index >= items.length) return;
    emit(state.copyWith(
      selectedItemIndex: index,
      displayText: items[index].text,
    ));
  }

  void navigatePrev() {
    final groups = state.config.groups;
    if (state.selectedGroupIndex >= groups.length) return;
    final items = groups[state.selectedGroupIndex].items;
    if (items.isEmpty) return;
    final newIndex = state.selectedItemIndex <= 0
        ? items.length - 1
        : state.selectedItemIndex - 1;
    selectItem(newIndex);
  }

  void navigateNext() {
    final groups = state.config.groups;
    if (state.selectedGroupIndex >= groups.length) return;
    final items = groups[state.selectedGroupIndex].items;
    if (items.isEmpty) return;
    final newIndex = state.selectedItemIndex >= items.length - 1
        ? 0
        : state.selectedItemIndex + 1;
    selectItem(newIndex);
  }

  /// Send text to TTS for playback.
  ///
  /// If the pipeline is not running, starts TTS-only mode (no mic/ASR)
  /// by calling [RealtimeCubit.startTtsOnly]. This avoids accidentally
  /// opening the microphone when the user just wants to play text.
  Future<void> sendText(String text) async {
    if (text.trim().isEmpty) return;
    try {
      final realtimeCubit = _realtimeCubitGetter?.call();
      if (realtimeCubit != null && !realtimeCubit.state.running) {
        await realtimeCubit.startTtsOnly();
      }
      await _realtimeRepo.enqueueTts(text.trim());
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> sendItem(QuickSubtitleItem item) async {
    emit(state.copyWith(displayText: item.text));
    await sendText(item.text);
  }

  Future<void> sendDisplayText() async {
    await sendText(state.displayText);
  }

  Future<void> addGroup(String name) async {
    final newGroup = QuickSubtitleGroup(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      name: name,
    );
    final groups = [...state.config.groups, newGroup];
    emit(state.copyWith(config: state.config.copyWith(groups: groups)));
    await _persist();
  }

  Future<void> addItem(String groupId, String text) async {
    final groups = state.config.groups.map((g) {
      if (g.id == groupId) {
        final items = [
          ...g.items,
          QuickSubtitleItem(
            id: DateTime.now().millisecondsSinceEpoch.toString(),
            text: text,
            order: g.items.length,
          ),
        ];
        return g.copyWith(items: items);
      }
      return g;
    }).toList();
    emit(state.copyWith(config: state.config.copyWith(groups: groups)));
    await _persist();
  }

  Future<void> removeItem(String groupId, String itemId) async {
    final groups = state.config.groups.map((g) {
      if (g.id == groupId) {
        return g.copyWith(
          items: g.items.where((i) => i.id != itemId).toList(),
        );
      }
      return g;
    }).toList();
    emit(state.copyWith(config: state.config.copyWith(groups: groups)));
    await _persist();
  }

  Future<void> removeGroup(String groupId) async {
    final groups =
        state.config.groups.where((g) => g.id != groupId).toList();
    emit(state.copyWith(
      config: state.config.copyWith(groups: groups),
      selectedGroupIndex: 0,
    ));
    await _persist();
  }

  void togglePresetsVisible() {
    emit(state.copyWith(presetsVisible: !state.presetsVisible));
  }

  Future<void> setFontSize(double size) async {
    emit(state.copyWith(
      config: state.config.copyWith(fontSize: size),
    ));
    await _persist();
  }

  Future<void> setBold(bool bold) async {
    emit(state.copyWith(
      config: state.config.copyWith(bold: bold),
    ));
    await _persist();
  }

  Future<void> setCentered(bool centered) async {
    emit(state.copyWith(
      config: state.config.copyWith(centered: centered),
    ));
    await _persist();
  }

  /// Toggle bold on/off.
  Future<void> toggleBold() => setBold(!state.config.bold);

  /// Toggle centered on/off.
  Future<void> toggleCentered() => setCentered(!state.config.centered);

  /// Clear the display text.
  void clearDisplay() {
    emit(state.copyWith(displayText: '', selectedItemIndex: -1));
  }

  void clearError() => emit(state.copyWith(error: null));
}
