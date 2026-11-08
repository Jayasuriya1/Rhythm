package com.example.ui.screens.study

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.RhythmMint
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.RhythmMidnightTeal
import com.example.ui.theme.FocusBackdropDeep
import com.example.ui.theme.FocusBackdropMintShade
import kotlin.math.cos
import kotlin.math.sin

/**
 * Type of backdrop displayed behind the focus timer.
 */
enum class FocusBgType {
    OLED,
    BUILTIN,
    CUSTOM
}

/**
 * Curated ambient focus backdrop themes.
 * Designed for deep, calm concentration with soothing ambient aesthetics and zero distraction.
 * Built with ADHD & mood stability in mind (no flashing or hyper-stimulating visuals).
 */
enum class FocusAestheticTheme(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val cardBackground: Color
) {
    // Backdrops may vary visually, but controls and surfaces always use Rhythm's
    // shared teal/mint identity. This prevents each backdrop from recolouring the UI.
    MINIMAL_OLED("minimal_oled", "Minimal OLED Black", "Pure black with a calm mint accent",
        RhythmMint, FocusBackdropMintShade, OledBackground, OledBackground, OledSurface),
    MISTY_FOREST("misty_forest", "Misty Forest", "Soft mist and quiet evergreen atmosphere",
        RhythmMint, FocusBackdropMintShade, RhythmMidnightTeal, FocusBackdropDeep, OledSurface),
    RAINY_WINDOW("rainy_window", "Rainy Window", "Gentle rain-inspired twilight atmosphere",
        RhythmMint, FocusBackdropMintShade, RhythmMidnightTeal, FocusBackdropDeep, OledSurface),
    DARK_BAMBOO("dark_bamboo", "Dark Bamboo Forest", "Quiet botanical shapes in deep teal",
        RhythmMint, FocusBackdropMintShade, RhythmMidnightTeal, FocusBackdropDeep, OledSurface),
    MOUNTAIN_MIST("mountain_mist", "Mountain Mist", "Layered mountain silhouettes and soft mist",
        RhythmMint, FocusBackdropMintShade, RhythmMidnightTeal, FocusBackdropDeep, OledSurface),
    QUIET_OCEAN("quiet_ocean", "Quiet Ocean", "A still horizon in deep teal",
        RhythmMint, FocusBackdropMintShade, RhythmMidnightTeal, FocusBackdropDeep, OledSurface),
    STARRY_NIGHT("starry_night", "Starry Night", "A restrained night-sky ambience",
        RhythmMint, FocusBackdropMintShade, RhythmMidnightTeal, FocusBackdropDeep, OledSurface),
    COZY_STUDY("cozy_study", "Cozy Study Room", "A warm, quiet study atmosphere",
        RhythmMint, FocusBackdropMintShade, RhythmMidnightTeal, FocusBackdropDeep, OledSurface),
    GREEN_GARDEN("green_garden", "Green Garden", "Soft botanical shapes and restorative stillness",
        RhythmMint, FocusBackdropMintShade, RhythmMidnightTeal, FocusBackdropDeep, OledSurface),
    SOFT_SUNSET("soft_sunset", "Soft Sunset", "A muted twilight glow without saturated accents",
        RhythmMint, FocusBackdropMintShade, RhythmMidnightTeal, FocusBackdropDeep, OledSurface);

    companion object {
        // Backward-compatibility aliases for existing calls
        val ZEN_FOREST = DARK_BAMBOO
        val LOFI_CAFE = COZY_STUDY
        val MIDNIGHT_CYBER = QUIET_OCEAN
        val DEEP_SPACE = STARRY_NIGHT
        val SUNSET_DUSK = SOFT_SUNSET
        val MINIMAL_DARK = MINIMAL_OLED

        fun fromId(id: String?): FocusAestheticTheme {
            if (id.isNullOrBlank()) return MINIMAL_OLED
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: when (id.lowercase()) {
                    "zen_forest" -> DARK_BAMBOO
                    "lofi_cafe" -> COZY_STUDY
                    "midnight_cyber" -> QUIET_OCEAN
                    "deep_space" -> STARRY_NIGHT
                    "sunset_dusk" -> SOFT_SUNSET
                    "minimal_dark" -> MINIMAL_OLED
                    else -> MINIMAL_OLED
                }
        }
    }
}
/**
 * Unified Focus Background Layer supporting:
 * 1. Pure OLED Black
 * 2. Curated Built-in Atmospheric Procedural Backdrops (with optional calm breathing animation)
 * 3. Custom Image selected from User Gallery with dimming, opacity, blur, and crop/fit controls
 * 4. Soft Vignette for edge focus
 */
@Composable
fun FocusBackgroundLayer(
    bgType: FocusBgType,
    builtinTheme: FocusAestheticTheme,
    customImageUri: String?,
    overlayOpacity: Float = 0.60f,
    brightness: Float = 0.85f,
    isBlurEnabled: Boolean = false,
    scaleMode: String = "CROP", // "CROP" or "FIT"
    isAmbientAnimationEnabled: Boolean = true,
    isTimerRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (bgType) {
            FocusBgType.OLED -> {
                // Pure zero-power OLED black
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

            FocusBgType.CUSTOM -> {
                if (!customImageUri.isNullOrBlank()) {
                    val colorMatrix = remember(brightness) {
                        ColorMatrix().apply {
                            setToScale(brightness, brightness, brightness, 1f)
                        }
                    }

                    val imageModifier = Modifier
                        .fillMaxSize()
                        .then(if (isBlurEnabled) Modifier.blur(16.dp) else Modifier)

                    val imageModel = remember(customImageUri) {
                        if (customImageUri.startsWith("content://") || customImageUri.startsWith("file://")) {
                            Uri.parse(customImageUri)
                        } else {
                            java.io.File(customImageUri)
                        }
                    }

                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Custom Focus Background",
                        contentScale = if (scaleMode.equals("FIT", ignoreCase = true)) ContentScale.Fit else ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(colorMatrix),
                        modifier = imageModifier
                    )

                    // Overlay darkening for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = overlayOpacity.coerceIn(0.1f, 0.95f)))
                    )

                    // Soft edge vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                } else {
                    // Fallback to OLED black if custom URI is null
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            }

            FocusBgType.BUILTIN -> {
                // Render themed atmospheric procedural backdrop
                ThemedProceduralBackdrop(
                    theme = builtinTheme,
                    isAnimated = isAmbientAnimationEnabled,
                    isTimerRunning = isTimerRunning
                )

                // User adjustable overlay opacity
                if (overlayOpacity > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = overlayOpacity.coerceIn(0f, 0.9f)))
                    )
                }

                // Vignette around edges for focus tunnel
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Procedural generative canvas backdrop tailored for each curated theme.
 */
@Composable
private fun ThemedProceduralBackdrop(
    theme: FocusAestheticTheme,
    isAnimated: Boolean,
    isTimerRunning: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AtmosphericMotion")

    val animationPhase by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28318f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val pulseScale by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(theme.backgroundTop, theme.backgroundBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0 || height <= 0) return@Canvas

            when (theme) {
                FocusAestheticTheme.MINIMAL_OLED -> {
                    // True pure black, nothing to draw
                }

                FocusAestheticTheme.MISTY_FOREST -> {
                    // Layered mist bands & soft evergreen radial glows
                    val mistY = height * 0.45f + sin(animationPhase.toDouble()).toFloat() * (height * 0.04f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                theme.primaryAccent.copy(alpha = 0.08f),
                                theme.secondaryAccent.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = mistY - 120.dp.toPx(),
                            endY = mistY + 140.dp.toPx()
                        ),
                        topLeft = Offset(0f, mistY - 120.dp.toPx()),
                        size = Size(width, 260.dp.toPx())
                    )

                    // Atmospheric canopy glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(width * 0.3f, height * 0.25f),
                            radius = width * 0.6f * pulseScale
                        ),
                        radius = width * 0.6f * pulseScale,
                        center = Offset(width * 0.3f, height * 0.25f)
                    )
                }

                FocusAestheticTheme.RAINY_WINDOW -> {
                    // Rain streaks & cold twilight glows
                    val rainOffset = (animationPhase * 40f) % 60f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(width * 0.5f, height * 0.35f),
                            radius = width * 0.55f * pulseScale
                        ),
                        radius = width * 0.55f * pulseScale,
                        center = Offset(width * 0.5f, height * 0.35f)
                    )

                    // Subtle stylized rain streak lines across background
                    val streakColor = theme.secondaryAccent.copy(alpha = 0.10f)
                    for (i in 0..12) {
                        val x = (width * (i / 12f) + (i * 17f)) % width
                        val y = ((i * 73f) + rainOffset * 6f) % height
                        drawLine(
                            color = streakColor,
                            start = Offset(x, y),
                            end = Offset(x - 6.dp.toPx(), y + 36.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                FocusAestheticTheme.DARK_BAMBOO -> {
                    // Bamboo stalks vertical lines with soft emerald light
                    val stalkColor = theme.secondaryAccent.copy(alpha = 0.07f)
                    val stalkPositions = listOf(0.12f, 0.22f, 0.35f, 0.78f, 0.88f)
                    for (relX in stalkPositions) {
                        drawLine(
                            color = stalkColor,
                            start = Offset(width * relX, 0f),
                            end = Offset(width * relX, height),
                            strokeWidth = 12.dp.toPx()
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(width * 0.5f, height * 0.55f),
                            radius = width * 0.5f * pulseScale
                        ),
                        radius = width * 0.5f * pulseScale,
                        center = Offset(width * 0.5f, height * 0.55f)
                    )
                }

                FocusAestheticTheme.MOUNTAIN_MIST -> {
                    // Layered mist mountain ridges
                    val ridge1 = height * 0.65f
                    val ridge2 = height * 0.80f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(width * 0.6f, height * 0.3f),
                            radius = width * 0.6f * pulseScale
                        ),
                        radius = width * 0.6f * pulseScale,
                        center = Offset(width * 0.6f, height * 0.3f)
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, theme.primaryAccent.copy(alpha = 0.06f), Color.Transparent),
                            startY = ridge1 - 50.dp.toPx(),
                            endY = ridge1 + 80.dp.toPx()
                        ),
                        topLeft = Offset(0f, ridge1 - 50.dp.toPx()),
                        size = Size(width, 130.dp.toPx())
                    )
                }

                FocusAestheticTheme.QUIET_OCEAN -> {
                    // Deep ocean horizon line with calm ambient swell
                    val oceanHorizon = height * 0.6f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, theme.primaryAccent.copy(alpha = 0.08f), theme.secondaryAccent.copy(alpha = 0.12f)),
                            startY = oceanHorizon,
                            endY = height
                        ),
                        topLeft = Offset(0f, oceanHorizon),
                        size = Size(width, height - oceanHorizon)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(width * 0.5f, oceanHorizon),
                            radius = width * 0.45f * pulseScale
                        ),
                        radius = width * 0.45f * pulseScale,
                        center = Offset(width * 0.5f, oceanHorizon)
                    )
                }

                FocusAestheticTheme.STARRY_NIGHT -> {
                    // Nebula center + subtle star points
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                theme.primaryAccent.copy(alpha = 0.18f),
                                theme.secondaryAccent.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.45f, height * 0.4f),
                            radius = width * 0.55f * pulseScale
                        ),
                        radius = width * 0.55f * pulseScale,
                        center = Offset(width * 0.45f, height * 0.4f)
                    )

                    // Distant subtle stars
                    val starPositions = listOf(
                        Offset(width * 0.15f, height * 0.15f),
                        Offset(width * 0.32f, height * 0.22f),
                        Offset(width * 0.75f, height * 0.18f),
                        Offset(width * 0.85f, height * 0.30f),
                        Offset(width * 0.20f, height * 0.72f),
                        Offset(width * 0.80f, height * 0.78f)
                    )
                    for ((index, pos) in starPositions.withIndex()) {
                        val starAlpha = 0.25f + sin(animationPhase.toDouble() + index).toFloat() * 0.15f
                        drawCircle(
                            color = Color.White.copy(alpha = starAlpha.coerceIn(0.1f, 0.5f)),
                            radius = 2.dp.toPx(),
                            center = pos
                        )
                    }
                }

                FocusAestheticTheme.COZY_STUDY -> {
                    // Warm amber lamp light casting downward cone from top-right
                    val lampX = width * 0.75f
                    val lampY = height * 0.2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                theme.primaryAccent.copy(alpha = 0.22f),
                                theme.secondaryAccent.copy(alpha = 0.07f),
                                Color.Transparent
                            ),
                            center = Offset(lampX, lampY),
                            radius = width * 0.65f * pulseScale
                        ),
                        radius = width * 0.65f * pulseScale,
                        center = Offset(lampX, lampY)
                    )
                }

                FocusAestheticTheme.GREEN_GARDEN -> {
                    // Organic leaf canopy glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(width * 0.35f, height * 0.3f),
                            radius = width * 0.5f * pulseScale
                        ),
                        radius = width * 0.5f * pulseScale,
                        center = Offset(width * 0.35f, height * 0.3f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.secondaryAccent.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(width * 0.7f, height * 0.65f),
                            radius = width * 0.55f * (2f - pulseScale)
                        ),
                        radius = width * 0.55f * (2f - pulseScale),
                        center = Offset(width * 0.7f, height * 0.65f)
                    )
                }

                FocusAestheticTheme.SOFT_SUNSET -> {
                    // Low terracotta dusk horizon
                    val sunsetY = height * 0.7f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                theme.secondaryAccent.copy(alpha = 0.12f),
                                theme.primaryAccent.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            startY = sunsetY - 140.dp.toPx(),
                            endY = sunsetY + 100.dp.toPx()
                        ),
                        topLeft = Offset(0f, sunsetY - 140.dp.toPx()),
                        size = Size(width, 240.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Backward compatibility wrapper for AestheticBackdropCanvas
 */
@Composable
fun AestheticBackdropCanvas(
    theme: FocusAestheticTheme,
    modifier: Modifier = Modifier,
    isTimerRunning: Boolean = true
) {
    FocusBackgroundLayer(
        bgType = FocusBgType.BUILTIN,
        builtinTheme = theme,
        customImageUri = null,
        overlayOpacity = 0.55f,
        brightness = 0.85f,
        isBlurEnabled = false,
        isAmbientAnimationEnabled = true,
        isTimerRunning = isTimerRunning,
        modifier = modifier
    )
}
