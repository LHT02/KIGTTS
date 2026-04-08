import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/repositories/overlay_repository.dart';
import 'overlay_state.dart';

/// Cubit managing floating overlay service state.
class OverlayCubit extends Cubit<OverlayState> {
  OverlayCubit({required OverlayRepository overlayRepository})
      : _overlayRepo = overlayRepository,
        super(const OverlayState());

  final OverlayRepository _overlayRepo;

  Future<void> initialize() async {
    try {
      final showing = await _overlayRepo.isShowing();
      emit(state.copyWith(isShowing: showing));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> toggle() async {
    try {
      if (state.isShowing) {
        await _overlayRepo.hide();
      } else {
        await _overlayRepo.show();
      }
      emit(state.copyWith(isShowing: !state.isShowing));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> show() async {
    try {
      await _overlayRepo.show();
      emit(state.copyWith(isShowing: true));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> hide() async {
    try {
      await _overlayRepo.hide();
      emit(state.copyWith(isShowing: false));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }
}
