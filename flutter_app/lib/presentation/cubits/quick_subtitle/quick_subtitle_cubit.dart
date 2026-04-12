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
  }) : _realtimeRepo = realtimeRepository,
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
      emit(
        state.copyWith(
          config: config,
          loading: false,
          // Auto-select first item of first group
          displayText:
              config.groups.isNotEmpty && config.groups.first.items.isNotEmpty
              ? config.groups.first.items.first.text
              : '',
          selectedItemIndex:
              config.groups.isNotEmpty && config.groups.first.items.isNotEmpty
              ? 0
              : -1,
        ),
      );
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

  List<QuickSubtitleItem> _reindexItems(List<QuickSubtitleItem> items) {
    return [for (var i = 0; i < items.length; i++) items[i].copyWith(order: i)];
  }

  void selectGroup(int index) {
    final groups = state.config.groups;
    if (index < 0 || index >= groups.length) return;
    final items = groups[index].items;
    emit(
      state.copyWith(
        selectedGroupIndex: index,
        selectedItemIndex: items.isNotEmpty ? 0 : -1,
        displayText: items.isNotEmpty ? items.first.text : '',
      ),
    );
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
    emit(
      state.copyWith(selectedItemIndex: index, displayText: items[index].text),
    );
  }

  void navigatePrev() {
    final groups = state.config.groups;
    if (state.selectedGroupIndex >= groups.length) return;
    final items = groups[state.selectedGroupIndex].items;
    if (items.isEmpty) return;
    final currentIndex = state.selectedItemIndex < 0
        ? 0
        : state.selectedItemIndex;
    final newIndex = currentIndex <= 0 ? items.length - 1 : currentIndex - 1;
    selectItem(newIndex);
  }

  void navigateNext() {
    final groups = state.config.groups;
    if (state.selectedGroupIndex >= groups.length) return;
    final items = groups[state.selectedGroupIndex].items;
    if (items.isEmpty) return;
    final currentIndex = state.selectedItemIndex < 0
        ? -1
        : state.selectedItemIndex;
    final newIndex = currentIndex >= items.length - 1 ? 0 : currentIndex + 1;
    selectItem(newIndex);
  }

  /// Send text to TTS for playback.
  ///
  /// If the pipeline is not running, starts TTS-only mode (no mic/ASR)
  /// by calling [RealtimeCubit.startTtsOnly]. This avoids accidentally
  /// opening the microphone when the user just wants to play text.
  Future<void> sendText(String text) async {
    final normalized = text.trim();
    if (normalized.isEmpty) return;
    await _appendHistory(normalized);
    if (!state.config.playOnSend) {
      setDisplayText(normalized);
      return;
    }
    try {
      final realtimeCubit = _realtimeCubitGetter?.call();
      if (realtimeCubit != null && !realtimeCubit.state.running) {
        await realtimeCubit.startTtsOnly();
      }
      await _realtimeRepo.enqueueTts(normalized);
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

  Future<void> addGroup(String name, {String icon = 'emoji_emotions'}) async {
    final normalized = name.trim();
    if (normalized.isEmpty) return;
    final newGroup = QuickSubtitleGroup(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      name: normalized,
      icon: icon,
    );
    final groups = [...state.config.groups, newGroup];
    emit(
      state.copyWith(
        config: state.config.copyWith(groups: groups),
        selectedGroupIndex: groups.length - 1,
        selectedItemIndex: -1,
        displayText: '',
      ),
    );
    await _persist();
  }

  Future<void> updateGroup(String groupId, {String? name, String? icon}) async {
    final normalizedName = name?.trim();
    final groups = state.config.groups.map((g) {
      if (g.id != groupId) return g;
      return g.copyWith(
        name: normalizedName == null || normalizedName.isEmpty
            ? g.name
            : normalizedName,
        icon: icon?.trim().isNotEmpty == true ? icon!.trim() : g.icon,
      );
    }).toList();
    emit(state.copyWith(config: state.config.copyWith(groups: groups)));
    await _persist();
  }

  Future<void> reorderGroups(int oldIndex, int newIndex) async {
    final groups = [...state.config.groups];
    if (oldIndex < 0 || oldIndex >= groups.length) return;
    final targetIndex = newIndex > oldIndex ? newIndex - 1 : newIndex;
    if (targetIndex < 0 || targetIndex >= groups.length) return;

    final moving = groups.removeAt(oldIndex);
    groups.insert(targetIndex, moving);

    final selectedGroupId =
        state.selectedGroupIndex >= 0 &&
            state.selectedGroupIndex < state.config.groups.length
        ? state.config.groups[state.selectedGroupIndex].id
        : null;

    final selectedIndex = selectedGroupId == null
        ? 0
        : groups.indexWhere((g) => g.id == selectedGroupId);

    emit(
      state.copyWith(
        config: state.config.copyWith(groups: groups),
        selectedGroupIndex: selectedIndex < 0 ? 0 : selectedIndex,
      ),
    );
    await _persist();
  }

  Future<void> addItem(String groupId, String text) async {
    final normalized = text.trim();
    if (normalized.isEmpty) return;
    final groups = state.config.groups.map((g) {
      if (g.id == groupId) {
        final items = [
          ...g.items,
          QuickSubtitleItem(
            id: DateTime.now().millisecondsSinceEpoch.toString(),
            text: normalized,
            order: g.items.length,
          ),
        ];
        return g.copyWith(items: _reindexItems(items));
      }
      return g;
    }).toList();
    final selectedGroup = groups.indexWhere((g) => g.id == groupId);
    final addedItemIndex = selectedGroup >= 0
        ? groups[selectedGroup].items.length - 1
        : state.selectedItemIndex;
    emit(
      state.copyWith(
        config: state.config.copyWith(groups: groups),
        selectedGroupIndex: selectedGroup >= 0
            ? selectedGroup
            : state.selectedGroupIndex,
        selectedItemIndex: addedItemIndex,
        displayText: normalized,
      ),
    );
    await _persist();
  }

  Future<void> updateItem(String groupId, String itemId, String text) async {
    final normalized = text.trim();
    if (normalized.isEmpty) return;
    final groups = state.config.groups.map((g) {
      if (g.id != groupId) return g;
      final items = g.items.map((i) {
        if (i.id != itemId) return i;
        return i.copyWith(text: normalized);
      }).toList();
      return g.copyWith(items: _reindexItems(items));
    }).toList();

    var displayText = state.displayText;
    final selectedGroupIndex = groups.indexWhere((g) => g.id == groupId);
    if (selectedGroupIndex >= 0) {
      final selectedGroupItems = groups[selectedGroupIndex].items;
      final selectedItemIndex = state.selectedItemIndex;
      if (selectedItemIndex >= 0 &&
          selectedItemIndex < selectedGroupItems.length) {
        final selectedItem = selectedGroupItems[selectedItemIndex];
        if (selectedItem.id == itemId) {
          displayText = normalized;
        }
      }
    }

    emit(
      state.copyWith(
        config: state.config.copyWith(groups: groups),
        displayText: displayText,
      ),
    );
    await _persist();
  }

  Future<void> reorderItems(String groupId, int oldIndex, int newIndex) async {
    final groups = [...state.config.groups];
    final groupIndex = groups.indexWhere((g) => g.id == groupId);
    if (groupIndex < 0) return;

    final items = [...groups[groupIndex].items];
    if (oldIndex < 0 || oldIndex >= items.length) return;
    final targetIndex = newIndex > oldIndex ? newIndex - 1 : newIndex;
    if (targetIndex < 0 || targetIndex >= items.length) return;

    final moving = items.removeAt(oldIndex);
    items.insert(targetIndex, moving);
    final reindexed = _reindexItems(items);
    groups[groupIndex] = groups[groupIndex].copyWith(items: reindexed);

    final selectedItemId =
        state.selectedItemIndex >= 0 &&
            state.selectedGroupIndex == groupIndex &&
            state.selectedItemIndex <
                state.config.groups[groupIndex].items.length
        ? state.config.groups[groupIndex].items[state.selectedItemIndex].id
        : null;
    final selectedItemIndex = selectedItemId == null
        ? state.selectedItemIndex
        : reindexed.indexWhere((i) => i.id == selectedItemId);

    emit(
      state.copyWith(
        config: state.config.copyWith(groups: groups),
        selectedGroupIndex: state.selectedGroupIndex == groupIndex
            ? groupIndex
            : state.selectedGroupIndex,
        selectedItemIndex: selectedItemIndex,
      ),
    );
    await _persist();
  }

  Future<void> removeItem(String groupId, String itemId) async {
    final groups = [...state.config.groups];
    final groupIndex = groups.indexWhere((g) => g.id == groupId);
    if (groupIndex < 0) return;

    final oldItems = groups[groupIndex].items;
    final nextItems = oldItems.where((i) => i.id != itemId).toList();
    if (nextItems.length == oldItems.length) return;

    groups[groupIndex] = groups[groupIndex].copyWith(
      items: _reindexItems(nextItems),
    );

    var selectedItemIndex = state.selectedItemIndex;
    var displayText = state.displayText;
    if (state.selectedGroupIndex == groupIndex) {
      final removedIndex = oldItems.indexWhere((i) => i.id == itemId);
      if (removedIndex >= 0) {
        if (state.selectedItemIndex == removedIndex) {
          if (nextItems.isEmpty) {
            selectedItemIndex = -1;
            displayText = '';
          } else {
            final safeIndex = removedIndex.clamp(0, nextItems.length - 1);
            selectedItemIndex = safeIndex;
            displayText = nextItems[safeIndex].text;
          }
        } else if (state.selectedItemIndex > removedIndex) {
          selectedItemIndex = state.selectedItemIndex - 1;
        }
      }
    }

    emit(
      state.copyWith(
        config: state.config.copyWith(groups: groups),
        selectedItemIndex: selectedItemIndex,
        displayText: displayText,
      ),
    );
    await _persist();
  }

  Future<void> removeGroup(String groupId) async {
    final groups = state.config.groups.where((g) => g.id != groupId).toList();
    if (groups.isEmpty) return;

    final nextSelected = state.selectedGroupIndex.clamp(0, groups.length - 1);
    final nextItems = groups[nextSelected].items;

    emit(
      state.copyWith(
        config: state.config.copyWith(groups: groups),
        selectedGroupIndex: nextSelected,
        selectedItemIndex: nextItems.isNotEmpty ? 0 : -1,
        displayText: nextItems.isNotEmpty ? nextItems.first.text : '',
      ),
    );
    await _persist();
  }

  void togglePresetsVisible() {
    emit(state.copyWith(presetsVisible: !state.presetsVisible));
  }

  Future<void> setFontSize(double size) async {
    emit(state.copyWith(config: state.config.copyWith(fontSize: size)));
    await _persist();
  }

  Future<void> setBold(bool bold) async {
    emit(state.copyWith(config: state.config.copyWith(bold: bold)));
    await _persist();
  }

  Future<void> setCentered(bool centered) async {
    emit(state.copyWith(config: state.config.copyWith(centered: centered)));
    await _persist();
  }

  /// Toggle play-on-send and persist it.
  Future<void> setPlayOnSend(bool enabled) async {
    emit(state.copyWith(config: state.config.copyWith(playOnSend: enabled)));
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

  Future<void> _appendHistory(String text) async {
    try {
      final raw = await _settingsRepo.getJsonSetting(
        PrefsKeys.quickSubtitleHistory,
      );
      final list = raw == null || raw.isEmpty
          ? <String>[]
          : (jsonDecode(raw) as List<dynamic>).whereType<String>().toList();
      final merged = <String>[text, ...list.where((e) => e != text)];
      if (merged.length > 100) {
        merged.removeRange(100, merged.length);
      }
      await _settingsRepo.setJsonSetting(
        PrefsKeys.quickSubtitleHistory,
        jsonEncode(merged),
      );
    } catch (_) {}
  }

  void clearError() => emit(state.copyWith(error: null));
}
