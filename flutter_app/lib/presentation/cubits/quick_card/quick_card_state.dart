import 'package:freezed_annotation/freezed_annotation.dart';
import '../../../domain/entities/quick_card.dart';

part 'quick_card_state.freezed.dart';

@freezed
abstract class QuickCardState with _$QuickCardState {
  const factory QuickCardState({
    @Default([]) List<QuickCard> cards,
    @Default(0) int selectedIndex,
    @Default(false) bool loading,
    String? error,
  }) = _QuickCardState;
}
