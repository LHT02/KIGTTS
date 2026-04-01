import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'injection.dart';
import 'presentation/cubits/realtime/realtime_cubit.dart';

/// Root application widget.
class KgttsApp extends StatelessWidget {
  const KgttsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider<RealtimeCubit>(
      create: (_) => getIt<RealtimeCubit>(),
      child: MaterialApp.router(
        title: 'KIGTTS',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        darkTheme: AppTheme.dark,
        themeMode: ThemeMode.system,
        routerConfig: appRouter,
      ),
    );
  }
}
