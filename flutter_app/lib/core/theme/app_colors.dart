import 'package:flutter/material.dart';

/// Application color tokens for dark and light themes.
/// Matches the original Android app's color scheme.
abstract final class AppColors {
  // Brand
  static const primary = Color(0xFF038387);
  static const primaryVariant = Color(0xFF025E61);
  static const secondary = Color(0xFF03DAC6);

  // Dark theme
  static const darkBackground = Color(0xFF121416);
  static const darkSurface = Color(0xFF1D2023);
  static const darkSurfaceVariant = Color(0xFF262A2E);
  static const darkOnBackground = Color(0xFFE4E8EB);
  static const darkOnSurface = Color(0xFFE4E8EB);
  static const darkOnSurfaceVariant = Color(0xFFB6BEC4);
  static const darkOutline = Color(0xFF757F87);
  static const darkError = Color(0xFFCF6679);

  // Light theme
  static const lightBackground = Color(0xFFF1F3F5);
  static const lightSurface = Color(0xFFFFFFFF);
  static const lightSurfaceVariant = Color(0xFFE8ECEF);
  static const lightOnBackground = Color(0xFF111417);
  static const lightOnSurface = Color(0xFF111417);
  static const lightOnSurfaceVariant = Color(0xFF495156);
  static const lightOutline = Color(0xFF9CA5AC);
  static const lightError = Color(0xFFB00020);

  // Semantic
  static const success = Color(0xFF4CAF50);
  static const warning = Color(0xFFFFC107);
  static const info = Color(0xFF2196F3);
}
