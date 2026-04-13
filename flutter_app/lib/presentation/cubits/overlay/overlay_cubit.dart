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
        emit(state.copyWith(isShowing: false, error: null));
        return;
      }

      final hasPermission = await _overlayRepo.hasPermission();
      if (!hasPermission) {
        await _overlayRepo.openPermissionSettings();
        emit(state.copyWith(error: '请先授予“悬浮窗显示在其他应用上层”权限，然后再开启悬浮窗。'));
        return;
      }
      await _overlayRepo.show();
      emit(state.copyWith(isShowing: true, error: null));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> show() async {
    try {
      final hasPermission = await _overlayRepo.hasPermission();
      if (!hasPermission) {
        await _overlayRepo.openPermissionSettings();
        emit(state.copyWith(error: '请先授予“悬浮窗显示在其他应用上层”权限，然后再开启悬浮窗。'));
        return;
      }
      await _overlayRepo.show();
      emit(state.copyWith(isShowing: true, error: null));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> hide() async {
    try {
      await _overlayRepo.hide();
      emit(state.copyWith(isShowing: false, error: null));
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> updateConfig(Map<String, dynamic> config) async {
    try {
      await _overlayRepo.updateConfig(config);
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }

  Future<void> openPermissionSettings() async {
    try {
      await _overlayRepo.openPermissionSettings();
    } catch (e) {
      emit(state.copyWith(error: e.toString()));
    }
  }
}
