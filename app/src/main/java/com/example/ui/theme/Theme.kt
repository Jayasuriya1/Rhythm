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

@Composable
fun RhythmFocusTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Preserve crafted Calming Sage mood by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
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
  RhythmFocusTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
