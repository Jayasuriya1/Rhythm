package com.example.ui.screens.study

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Aesthetic focus backdrop themes inspired by Flocus (flocus.com) and Wonderspace (wonderspace.app).
 * Designed for deep, calm concentration with soothing ambient lighting and zero distraction.
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
    ZEN_FOREST(
        id = "zen_forest",
        displayName = "Zen Bamboo & Forest",
        subtitle = "Misty emerald rain, tranquil evergreen and bamboo serenity",
        primaryAccent = Color(0xFF10B981),
        secondaryAccent = Color(0xFF34D399),
        backgroundTop = Color(0xFF03160F),
        backgroundBottom = Color(0xFF010A07),
        cardBackground = Color(0xFF062319)
    ),
    LOFI_CAFE(
        id = "lofi_cafe",
        displayName = "Lofi Cozy Cafe",
        subtitle = "Warm amber lamplight, espresso steam & golden study vibes",
        primaryAccent = Color(0xFFF59E0B),
        secondaryAccent = Color(0xFFD97706),
        backgroundTop = Color(0xFF1C1309),
        backgroundBottom = Color(0xFF0E0A04),
        cardBackground = Color(0xFF261A0C)
    ),
    MIDNIGHT_CYBER(
        id = "midnight_cyber",
        displayName = "Midnight Cyberpunk",
        subtitle = "Electric neon emerald and cyan pulse in deep obsidian space",
        primaryAccent = Color(0xFF06B6D4),
        secondaryAccent = Color(0xFF10B981),
        backgroundTop = Color(0xFF040E14),
        backgroundBottom = Color(0xFF010406),
        cardBackground = Color(0xFF081820)
    ),
    DEEP_SPACE(
        id = "deep_space",
        displayName = "Cosmic Starlight",
        subtitle = "Celestial violet nebula, deep space quiet and flow state",
        primaryAccent = Color(0xFFA78BFA),
        secondaryAccent = Color(0xFF818CF8),
        backgroundTop = Color(0xFF0E091F),
        backgroundBottom = Color(0xFF05030A),
        cardBackground = Color(0xFF160F2E)
    ),
    SUNSET_DUSK(
        id = "sunset_dusk",
        displayName = "Terracotta Sunset",
        subtitle = "Golden twilight glow, warm embers and peaceful evening focus",
        primaryAccent = Color(0xFFFB923C),
        secondaryAccent = Color(0xFFF43F5E),
        backgroundTop = Color(0xFF1C0D11),
        backgroundBottom = Color(0xFF0B0407),
        cardBackground = Color(0xFF2B131B)
    ),
    MINIMAL_DARK(
        id = "minimal_dark",
        displayName = "Pure Minimal Obsidian",
        subtitle = "Zero distraction OLED obsidian with crisp mint precision",
        primaryAccent = Color(0xFF10B981),
        secondaryAccent = Color(0xFF6EE7B7),
        backgroundTop = Color(0xFF090D0B),
        backgroundBottom = Color(0xFF000000),
        cardBackground = Color(0xFF0F1512)
    )
}

/**
 * Animated Canvas backdrop rendering subtle ambient orbs and breathing gradients
 * at a buttery 60 FPS without recomposition overhead.
 */
@Composable
fun AestheticBackdropCanvas(
    theme: FocusAestheticTheme,
    modifier: Modifier = Modifier,
    isTimerRunning: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AestheticMotion")

    // Slow, soothing 12-second cyclic animation for floating ambient light orbs
    val animationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Breathing pulse for radial glows
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
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

            // Orb 1: Upper Primary Glow
            val orb1X = width * 0.35f + cos(animationPhase.toDouble()).toFloat() * (width * 0.12f)
            val orb1Y = height * 0.30f + sin(animationPhase.toDouble()).toFloat() * (height * 0.08f)
            val orb1Radius = (width * 0.45f) * (if (isTimerRunning) pulseScale else 1f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.primaryAccent.copy(alpha = 0.22f),
                        theme.primaryAccent.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                center = Offset(orb1X, orb1Y),
                radius = orb1Radius
            )

            // Orb 2: Lower Secondary Glow
            val orb2X = width * 0.70f - sin((animationPhase * 0.8).toDouble()).toFloat() * (width * 0.15f)
            val orb2Y = height * 0.65f - cos((animationPhase * 0.8).toDouble()).toFloat() * (height * 0.10f)
            val orb2Radius = (width * 0.50f) * (if (isTimerRunning) (2f - pulseScale) else 1f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.secondaryAccent.copy(alpha = 0.18f),
                        theme.secondaryAccent.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                center = Offset(orb2X, orb2Y),
                radius = orb2Radius
            )
        }
    }
}
