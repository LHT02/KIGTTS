import 'dart:convert';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/constants/prefs_keys.dart';
import '../../../domain/entities/quick_subtitle.dart';
import '../../../domain/repositories/realtime_repository.dart';
import '../../../domain/repositories/settings_repository.dart';
import 'quick_subtitle_state.dart';

/// Cubit managing quick subtitle groups and TTS playback.
class QuickSubtitleCubit extends Cubit<QuickSubtitleState> {
  QuickSubtitleCubit({
    required RealtimeRepository realtimeRepository,
    required SettingsRepository settingsRepository,
  })  : _realtimeRepo = realtimeRepository,
        _settingsRepo = settingsRepository,
        super(const QuickSubtitleState());

  final RealtimeRepository _realtimeRepo;
  final SettingsRepository _settingsRepo;

  Future<void> initialize() async {
    emit(state.copyWith(loading: true));
    try {
      final json = await _settingsRepo.getJsonSetting(
        PrefsKeys.quickSubtitleConfig,
      );
      if (json != null && json.isNotEmpty) {
        final config = QuickSubtitleConfig.fromJson(
          jsonDecode(json) as Map<String, dynamic>,
        );
        emit(state.copyWith(config: config, loading: false));
      } else {
        emit(state.copyWith(loading: false));
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
    emit(state.copyWith(selectedGroupIndex: index));
  }

  void setInputText(String text) {
    emit(state.copyWith(inputText: text));
  }

  Future<void> sendText(String text) async {
    if (text.trim().isEmpty) return;
    try {
      await _realtimeRepo.enqueueTts(text.trim());
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> sendItem(QuickSubtitleItem item) async {
    await sendText(item.text);
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
    final groups = state.config.groups.where((g) => g.id != groupId).toList();
    emit(state.copyWith(
      config: state.config.copyWith(groups: groups),
      selectedGroupIndex: 0,
    ));
    await _persist();
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

  void clearError() => emit(state.copyWith(error: null));
}
