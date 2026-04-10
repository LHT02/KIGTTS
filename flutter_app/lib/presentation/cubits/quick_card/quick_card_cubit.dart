import 'dart:convert';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/constants/prefs_keys.dart';
import '../../../domain/entities/quick_card.dart';
import '../../../domain/repositories/settings_repository.dart';
import 'quick_card_state.dart';

/// Cubit managing quick cards (image/QR/text).
class QuickCardCubit extends Cubit<QuickCardState> {
  QuickCardCubit({required SettingsRepository settingsRepository})
      : _settingsRepo = settingsRepository,
        super(const QuickCardState());

  final SettingsRepository _settingsRepo;

  Future<void> initialize() async {
    emit(state.copyWith(loading: true));
    try {
      final json = await _settingsRepo.getJsonSetting(
        PrefsKeys.quickCardConfig,
      );
      if (json != null && json.isNotEmpty) {
        final list = (jsonDecode(json) as List)
            .map((e) => QuickCard.fromJson(e as Map<String, dynamic>))
            .toList();
        emit(state.copyWith(cards: list, loading: false));
      } else {
        emit(state.copyWith(loading: false));
      }
    } catch (e) {
      emit(state.copyWith(loading: false, error: e.toString()));
    }
  }

  Future<void> _persist() async {
    try {
      final json = jsonEncode(state.cards.map((c) => c.toJson()).toList());
      await _settingsRepo.setJsonSetting(PrefsKeys.quickCardConfig, json);
    } catch (_) {}
  }

  void selectCard(int index) {
    emit(state.copyWith(selectedIndex: index));
  }

  Future<void> addCard(QuickCard card) async {
    final cards = [...state.cards, card];
    emit(state.copyWith(cards: cards));
    await _persist();
  }

  Future<void> removeCard(String id) async {
    final cards = state.cards.where((c) => c.id != id).toList();
    emit(state.copyWith(cards: cards, selectedIndex: 0));
    await _persist();
  }

  Future<void> updateCard(QuickCard updated) async {
    final cards = state.cards.map((c) {
      return c.id == updated.id ? updated : c;
    }).toList();
    emit(state.copyWith(cards: cards));
    await _persist();
  }

  void clearError() => emit(state.copyWith(error: null));
}
