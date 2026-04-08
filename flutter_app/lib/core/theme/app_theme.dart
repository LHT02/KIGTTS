import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'app_colors.dart';
import 'app_dimensions.dart';

/// App theme configuration.
/// Dark theme is default, light theme follows system.
abstract final class AppTheme {
  static ThemeData get dark => _buildTheme(
        brightness: Brightness.dark,
        background: AppColors.darkBackground,
        surface: AppColors.darkSurface,
        surfaceVariant: AppColors.darkSurfaceVariant,
        onBackground: AppColors.darkOnBackground,
        onSurface: AppColors.darkOnSurface,
        onSurfaceVariant: AppColors.darkOnSurfaceVariant,
        outline: AppColors.darkOutline,
        error: AppColors.darkError,
      );

  static ThemeData get light => _buildTheme(
        brightness: Brightness.light,
        background: AppColors.lightBackground,
        surface: AppColors.lightSurface,
        surfaceVariant: AppColors.lightSurfaceVariant,
        onBackground: AppColors.lightOnBackground,
        onSurface: AppColors.lightOnSurface,
        onSurfaceVariant: AppColors.lightOnSurfaceVariant,
        outline: AppColors.lightOutline,
        error: AppColors.lightError,
      );

  static ThemeData _buildTheme({
    required Brightness brightness,
    required Color background,
    required Color surface,
    required Color surfaceVariant,
    required Color onBackground,
    required Color onSurface,
    required Color onSurfaceVariant,
    required Color outline,
    required Color error,
  }) {
    final colorScheme = ColorScheme(
      brightness: brightness,
      primary: AppColors.primary,
      onPrimary: Colors.white,
      primaryContainer: AppColors.primaryVariant,
      onPrimaryContainer: Colors.white,
      secondary: AppColors.secondary,
      onSecondary: Colors.black,
      surface: surface,
      onSurface: onSurface,
      surfaceContainerHighest: surfaceVariant,
      onSurfaceVariant: onSurfaceVariant,
      error: error,
      onError: Colors.white,
      outline: outline,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: background,
      fontFamily: 'Roboto',
      cardTheme: CardThemeData(
        color: surface,
        elevation: AppDimensions.elevationCard,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppDimensions.radiusSmall),
        ),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: surface,
        foregroundColor: onSurface,
        elevation: 0,
        scrolledUnderElevation: AppDimensions.elevationTopBar,
        systemOverlayStyle: brightness == Brightness.dark
            ? SystemUiOverlayStyle.light
            : SystemUiOverlayStyle.dark,
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: AppDimensions.elevationFab,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppDimensions.radiusLarge),
        ),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: surface,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppDimensions.radiusLarge),
          ),
        ),
      ),
      sliderTheme: SliderThemeData(
        activeTrackColor: AppColors.primary,
        thumbColor: AppColors.primary,
        inactiveTrackColor: outline.withValues(alpha: 0.3),
      ),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return AppColors.primary;
          }
          return outline;
        }),
        trackColor: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return AppColors.primary.withValues(alpha: 0.5);
          }
          return outline.withValues(alpha: 0.3);
        }),
      ),
      dividerTheme: DividerThemeData(
        color: outline.withValues(alpha: 0.2),
        thickness: 1,
      ),
    );
  }
}
