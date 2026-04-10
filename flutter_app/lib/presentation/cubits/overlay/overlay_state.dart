import 'package:freezed_annotation/freezed_annotation.dart';

part 'overlay_state.freezed.dart';

@freezed
abstract class OverlayState with _$OverlayState {
  const factory OverlayState({
    @Default(false) bool isShowing,
    @Default(false) bool loading,
    String? error,
  }) = _OverlayState;
}
