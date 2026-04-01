import 'package:get_it/get_it.dart';
import 'data/datasources/keepalive_channel.dart';
import 'data/datasources/model_channel.dart';
import 'data/datasources/overlay_channel.dart';
import 'data/datasources/realtime_channel.dart';
import 'data/datasources/settings_local.dart';
import 'data/repositories/keepalive_repository_impl.dart';
import 'data/repositories/model_repository_impl.dart';
import 'data/repositories/overlay_repository_impl.dart';
import 'data/repositories/realtime_repository_impl.dart';
import 'data/repositories/settings_repository_impl.dart';
import 'domain/repositories/keepalive_repository.dart';
import 'domain/repositories/model_repository.dart';
import 'domain/repositories/overlay_repository.dart';
import 'domain/repositories/realtime_repository.dart';
import 'domain/repositories/settings_repository.dart';
import 'presentation/cubits/model_manager/model_manager_cubit.dart';
import 'presentation/cubits/realtime/realtime_cubit.dart';
import 'presentation/cubits/settings/settings_cubit.dart';

final getIt = GetIt.instance;

/// Initialize dependency injection.
/// Called once at app startup before runApp().
Future<void> configureDependencies() async {
  // --- DataSources (Singletons) ---
  getIt.registerSingleton(RealtimeChannelDataSource());
  getIt.registerSingleton(ModelChannelDataSource());
  getIt.registerSingleton(SettingsLocalDataSource());
  getIt.registerSingleton(OverlayChannelDataSource());
  getIt.registerSingleton(KeepaliveChannelDataSource());

  // --- Repositories (LazySingletons) ---
  getIt.registerLazySingleton<RealtimeRepository>(
    () => RealtimeRepositoryImpl(dataSource: getIt()),
  );
  getIt.registerLazySingleton<ModelRepository>(
    () => ModelRepositoryImpl(dataSource: getIt()),
  );
  getIt.registerLazySingleton<SettingsRepository>(
    () => SettingsRepositoryImpl(dataSource: getIt()),
  );
  getIt.registerLazySingleton<OverlayRepository>(
    () => OverlayRepositoryImpl(dataSource: getIt()),
  );
  getIt.registerLazySingleton<KeepaliveRepository>(
    () => KeepaliveRepositoryImpl(dataSource: getIt()),
  );

  // --- Cubits ---
  // RealtimeCubit is a singleton so the global RunningStrip can access it.
  getIt.registerLazySingleton(
    () => RealtimeCubit(
      realtimeRepository: getIt(),
      modelRepository: getIt(),
      settingsRepository: getIt(),
      keepaliveRepository: getIt(),
    ),
  );
  getIt.registerFactory(
    () => ModelManagerCubit(
      modelRepository: getIt(),
      realtimeRepository: getIt(),
    ),
  );
  getIt.registerFactory(
    () => SettingsCubit(
      settingsRepository: getIt(),
      realtimeRepository: getIt(),
    ),
  );
}
