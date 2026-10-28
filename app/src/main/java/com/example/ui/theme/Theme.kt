package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = SageOnPrimaryDark,
    primaryContainer = SagePrimaryContainerDark,
    onPrimaryContainer = SageOnPrimaryContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = SageOnSecondaryDark,
    secondaryContainer = SageSecondaryContainerDark,
    onSecondaryContainer = SageOnSecondaryContainerDark,
    tertiary = SageTertiaryDark,
    onTertiary = SageOnTertiaryDark,
    tertiaryContainer = SageTertiaryContainerDark,
    onTertiaryContainer = SageOnTertiaryContainerDark,
    background = SageBackgroundDark,
    onBackground = SageOnBackgroundDark,
    surface = SageSurfaceDark,
    onSurface = SageOnSurfaceDark,
    surfaceVariant = SageSurfaceVariantDark,
    onSurfaceVariant = SageOnSurfaceVariantDark,
    outline = SageOutlineDark,
    outlineVariant = SageOutlineVariantDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SagePrimaryLight,
    onPrimary = SageOnPrimaryLight,
    primaryContainer = SagePrimaryContainerLight,
    onPrimaryContainer = SageOnPrimaryContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = SageOnSecondaryLight,
    secondaryContainer = SageSecondaryContainerLight,
    onSecondaryContainer = SageOnSecondaryContainerLight,
    tertiary = SageTertiaryLight,
    onTertiary = SageOnTertiaryLight,
    tertiaryContainer = SageTertiaryContainerLight,
    onTertiaryContainer = SageOnTertiaryContainerLight,
    background = SageBackgroundLight,
    onBackground = SageOnBackgroundLight,
    surface = SageSurfaceLight,
    onSurface = SageOnSurfaceLight,
    surfaceVariant = SageSurfaceVariantLight,
    onSurfaceVariant = SageOnSurfaceVariantLight,
    outline = SageOutlineLight,
    outlineVariant = SageOutlineVariantLight,
  )

private val OledDarkColorScheme =
  darkColorScheme(
    primary = OledPrimary,
    onPrimary = OledOnPrimary,
    primaryContainer = OledPrimaryContainer,
    onPrimaryContainer = OledOnPrimaryContainer,
    secondary = OledSecondary,
    onSecondary = OledOnSecondary,
    secondaryContainer = OledSecondaryContainer,
    onSecondaryContainer = OledOnSecondaryContainer,
    tertiary = OledTertiary,
    onTertiary = OledOnTertiary,
    tertiaryContainer = OledTertiaryContainer,
    onTertiaryContainer = OledOnTertiaryContainer,
    background = OledBackground,
    onBackground = OledOnBackground,
    surface = OledSurface,
    onSurface = OledOnSurface,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledOnSurfaceVariant,
    outline = OledOutline,
    outlineVariant = OledOutlineVariant,
  )

enum class AppThemeMode(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Calming Sage Light"),
    DARK("Calming Sage Dark"),
    OLED_DARK("OLED Pitch Black")
}

@Composable
fun RhythmFocusTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val isSystemDark = isSystemInDarkTheme()
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (themeMode == AppThemeMode.OLED_DARK) OledDarkColorScheme
        else if (themeMode == AppThemeMode.DARK || (themeMode == AppThemeMode.SYSTEM && isSystemDark)) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
      }
      themeMode == AppThemeMode.OLED_DARK -> OledDarkColorScheme
      themeMode == AppThemeMode.DARK -> DarkColorScheme
      themeMode == AppThemeMode.LIGHT -> LightColorScheme
      themeMode == AppThemeMode.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
      else -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  RhythmFocusTheme(
    themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
    dynamicColor = dynamicColor,
    content = content
  )
}

