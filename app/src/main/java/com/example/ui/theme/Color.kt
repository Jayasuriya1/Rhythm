package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Rhythm brand and semantic color tokens.
 * This is the only source of truth for app-wide color values.
 * Feature screens must consume MaterialTheme.colorScheme or named semantic tokens.
 */

// Brand identity — deep teal, muted mint, warm champagne and quiet violet.
val RhythmDeepTeal = Color(0xFF123C3A)
val RhythmMidnightTeal = Color(0xFF102B2C)
val RhythmMint = Color(0xFF9ADCC6)
val RhythmSeafoam = Color(0xFF49BBA8)
val RhythmChampagne = Color(0xFFFFE6B9)
val RhythmViolet = Color(0xFFBDA6DC)

// Light theme — warm, low-glare neutrals with deep teal actions.
val SagePrimaryLight = Color(0xFF236B61)
val SageOnPrimaryLight = Color(0xFFFFFFFF)
val SagePrimaryContainerLight = Color(0xFFD5EEE6)
val SageOnPrimaryContainerLight = Color(0xFF092F2A)
val SageSecondaryLight = Color(0xFF596D68)
val SageOnSecondaryLight = Color(0xFFFFFFFF)
val SageSecondaryContainerLight = Color(0xFFE0ECE7)
val SageOnSecondaryContainerLight = Color(0xFF172D28)
val SageTertiaryLight = Color(0xFF75628E)
val SageOnTertiaryLight = Color(0xFFFFFFFF)
val SageTertiaryContainerLight = Color(0xFFEDE2F5)
val SageOnTertiaryContainerLight = Color(0xFF302044)
val SageBackgroundLight = Color(0xFFF5F8F6)
val SageOnBackgroundLight = Color(0xFF17211F)
val SageSurfaceLight = Color(0xFFFAFCFA)
val SageOnSurfaceLight = Color(0xFF17211F)
val SageSurfaceVariantLight = Color(0xFFE2EBE7)
val SageOnSurfaceVariantLight = Color(0xFF455650)
val SageOutlineLight = Color(0xFF74847E)
val SageOutlineVariantLight = Color(0xFFC5D2CC)

// Standard dark theme — layered teal-charcoal, distinct from OLED.
val SagePrimaryDark = RhythmMint
val SageOnPrimaryDark = Color(0xFF07372F)
val SagePrimaryContainerDark = Color(0xFF174D43)
val SageOnPrimaryContainerDark = Color(0xFFD0F2E7)
val SageSecondaryDark = Color(0xFFC2D4CC)
val SageOnSecondaryDark = Color(0xFF263A34)
val SageSecondaryContainerDark = Color(0xFF354B43)
val SageOnSecondaryContainerDark = Color(0xFFDFECE5)
val SageTertiaryDark = Color(0xFFD0B8E8)
val SageOnTertiaryDark = Color(0xFF39264E)
val SageTertiaryContainerDark = Color(0xFF514067)
val SageOnTertiaryContainerDark = Color(0xFFEBDDF8)
val SageBackgroundDark = Color(0xFF101A19)
val SageOnBackgroundDark = Color(0xFFE4EEEA)
val SageSurfaceDark = Color(0xFF14211E)
val SageOnSurfaceDark = Color(0xFFE4EEEA)
val SageSurfaceVariantDark = Color(0xFF293A35)
val SageOnSurfaceVariantDark = Color(0xFFC0D0C9)
val SageOutlineDark = Color(0xFF899B93)
val SageOutlineVariantDark = Color(0xFF3B4C45)

// Functional states — never repurpose brand accents to communicate status.
val StateHealthy = Color(0xFF39866D)
val StateWarning = Color(0xFF9A641F)
val StateInfo = Color(0xFF426F98)
val StateMuted = Color(0xFF77827E)
val StateErrorLight = Color(0xFFBA1A1A)
val StateErrorDark = Color(0xFFFFB4AB)

// OLED theme — true-black canvas with distinct, readable elevated surfaces.
val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF0A1110)
val OledSurfaceVariant = Color(0xFF111B19)
val OledSurfaceElevated = Color(0xFF192522)
val OledOnBackground = Color(0xFFEAF2EE)
val OledOnSurface = Color(0xFFEAF2EE)
val OledOnSurfaceVariant = Color(0xFFAABAB3)
val OledPrimary = RhythmMint
val OledOnPrimary = Color(0xFF092E27)
val OledPrimaryContainer = Color(0xFF1B4037)
val OledOnPrimaryContainer = Color(0xFFD7F2E8)
val OledSecondary = Color(0xFFC5D8CF)
val OledOnSecondary = Color(0xFF1B3029)
val OledSecondaryContainer = Color(0xFF263B33)
val OledOnSecondaryContainer = Color(0xFFE0ECE5)
val OledTertiary = RhythmChampagne
val OledOnTertiary = Color(0xFF352713)
val OledTertiaryContainer = Color(0xFF4A3920)
val OledOnTertiaryContainer = Color(0xFFF5E7CB)
val OledLavender = RhythmViolet
val OledOutline = Color(0xFF35443E)
val OledOutlineVariant = Color(0xFF202C27)

// Health chart series — stable semantic identities across themes.
val HealthMetricSteps = Color(0xFF83C5A5)
val HealthMetricSleep = Color(0xFF9D97C4)
val HealthMetricHeart = Color(0xFFE27B83)
val HealthMetricRestingHeart = Color(0xFFD96D78)
val HealthMetricHrv = Color(0xFFA389D4)
val HealthMetricEnergy = Color(0xFFE5BE82)
val HealthMetricHydration = Color(0xFF76A9CF)
val HealthMetricOxygen = Color(0xFF67B5BE)
val HealthMetricRespiratory = Color(0xFF75BCA7)
val HealthMetricExercise = Color(0xFF67B99A)
