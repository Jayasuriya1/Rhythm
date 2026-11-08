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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.RhythmMint
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.FocusBackdropMintShade
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
 *
 * Every curated preset provides a distinct visual identity, mood, and atmospheric palette:
 * - Misty Forest: layered evergreen silhouettes, soft fog bands, cool forest mist
 * - Rainy Window: muted twilight gradient, subtle rain streaks, gentle window reflections
 * - Dark Bamboo Forest: distinct bamboo stalk silhouettes, restrained botanical depth
 * - Mountain Mist: layered mountain ridges, atmospheric distance
 * - Quiet Ocean: calm dark teal horizon, subtle water reflections
 * - Starry Night: restrained night sky, sparse static-looking stars, gentle depth
 * - Cozy Study Room: warm muted amber lighting, subtle cozy atmosphere
 * - Green Garden: layered botanical silhouettes, soft natural green
 * - Soft Sunset: muted peach, mauve, warm twilight gradients
 * - Minimal OLED Black: true pure black, zero power, minimal distraction
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
    MINIMAL_OLED(
        id = "minimal_oled",
        displayName = "Minimal OLED Black",
        subtitle = "Pure black with a calm mint accent",
        primaryAccent = RhythmMint,
        secondaryAccent = FocusBackdropMintShade,
        backgroundTop = OledBackground,
        backgroundBottom = OledBackground,
        cardBackground = OledSurface
    ),
    MISTY_FOREST(
        id = "misty_forest",
        displayName = "Misty Forest",
        subtitle = "Soft mist and quiet evergreen atmosphere",
        primaryAccent = Color(0xFF5BA878),
        secondaryAccent = Color(0xFF335E44),
        backgroundTop = Color(0xFF0B2117),
        backgroundBottom = Color(0xFF030D08),
        cardBackground = Color(0xFF0E271C)
    ),
    RAINY_WINDOW(
        id = "rainy_window",
        displayName = "Rainy Window",
        subtitle = "Gentle rain-inspired twilight atmosphere",
        primaryAccent = Color(0xFF6B9AC4),
        secondaryAccent = Color(0xFF3F6485),
        backgroundTop = Color(0xFF0D1B2A),
        backgroundBottom = Color(0xFF050B12),
        cardBackground = Color(0xFF102133)
    ),
    DARK_BAMBOO(
        id = "dark_bamboo",
        displayName = "Dark Bamboo Forest",
        subtitle = "Quiet botanical shapes in deep bamboo olive",
        primaryAccent = Color(0xFF7FA876),
        secondaryAccent = Color(0xFF486A40),
        backgroundTop = Color(0xFF0B1C10),
        backgroundBottom = Color(0xFF030A06),
        cardBackground = Color(0xFF0F2415)
    ),
    MOUNTAIN_MIST(
        id = "mountain_mist",
        displayName = "Mountain Mist",
        subtitle = "Layered mountain silhouettes and soft mist",
        primaryAccent = Color(0xFF8E9EB0),
        secondaryAccent = Color(0xFF526376),
        backgroundTop = Color(0xFF121B26),
        backgroundBottom = Color(0xFF060A0F),
        cardBackground = Color(0xFF162230)
    ),
    QUIET_OCEAN(
        id = "quiet_ocean",
        displayName = "Quiet Ocean",
        subtitle = "A still horizon in deep oceanic teal",
        primaryAccent = Color(0xFF4EAEA7),
        secondaryAccent = Color(0xFF286864),
        backgroundTop = Color(0xFF061A22),
        backgroundBottom = Color(0xFF020C10),
        cardBackground = Color(0xFF09242E)
    ),
    STARRY_NIGHT(
        id = "starry_night",
        displayName = "Starry Night",
        subtitle = "A restrained cosmic night-sky ambience",
        primaryAccent = Color(0xFF9D8EC9),
        secondaryAccent = Color(0xFF56497D),
        backgroundTop = Color(0xFF140F26),
        backgroundBottom = Color(0xFF05040A),
        cardBackground = Color(0xFF1C1536)
    ),
    COZY_STUDY(
        id = "cozy_study",
        displayName = "Cozy Study Room",
        subtitle = "Warm mahogany amber and quiet atmosphere",
        primaryAccent = Color(0xFFD49E58),
        secondaryAccent = Color(0xFF8C5E28),
        backgroundTop = Color(0xFF22150C),
        backgroundBottom = Color(0xFF0D0704),
        cardBackground = Color(0xFF2A1B10)
    ),
    GREEN_GARDEN(
        id = "green_garden",
        displayName = "Green Garden",
        subtitle = "Lush botanical shapes and restorative stillness",
        primaryAccent = Color(0xFF76B580),
        secondaryAccent = Color(0xFF42784A),
        backgroundTop = Color(0xFF0F2014),
        backgroundBottom = Color(0xFF040A06),
        cardBackground = Color(0xFF142819)
    ),
    SOFT_SUNSET(
        id = "soft_sunset",
        displayName = "Soft Sunset",
        subtitle = "Muted dusk peach and mauve twilight glow",
        primaryAccent = Color(0xFFD9886E),
        secondaryAccent = Color(0xFF88495D),
        backgroundTop = Color(0xFF24121A),
        backgroundBottom = Color(0xFF0D0609),
        cardBackground = Color(0xFF2E1722)
    );

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
 * 2. Curated Built-in Atmospheric Procedural Backdrops
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

                    // Overlay darkening for readability on custom images
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

                // For curated procedural backdrops, scale the overlay opacity so that even
                // with the user slider at 0.60f, the rich gradients and atmospheric silhouettes
                // remain clearly distinct and visible, while keeping timer text 100% legible.
                val effectiveBuiltinOverlay = (overlayOpacity * 0.35f).coerceIn(0f, 0.65f)
                if (effectiveBuiltinOverlay > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = effectiveBuiltinOverlay))
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
                                    Color.Black.copy(alpha = 0.45f)
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
                animation = tween(durationMillis = 18000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val pulseScale by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
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
                    // 1. Soft atmospheric canopy glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(width * 0.35f, height * 0.22f),
                            radius = width * 0.65f * pulseScale
                        ),
                        radius = width * 0.65f * pulseScale,
                        center = Offset(width * 0.35f, height * 0.22f)
                    )

                    // 2. Far row of pine tree silhouettes
                    val farTreeColor = theme.secondaryAccent.copy(alpha = 0.30f)
                    val farTreePositions = listOf(0.08f, 0.24f, 0.42f, 0.60f, 0.78f, 0.92f)
                    for (relX in farTreePositions) {
                        val treeX = width * relX
                        val treeBaseY = height * 0.74f
                        val treeHeight = 110.dp.toPx()
                        val treePath = Path().apply {
                            moveTo(treeX, treeBaseY - treeHeight)
                            lineTo(treeX + 28.dp.toPx(), treeBaseY)
                            lineTo(treeX - 28.dp.toPx(), treeBaseY)
                            close()
                        }
                        drawPath(treePath, color = farTreeColor)
                    }

                    // 3. Drifting soft horizontal mist bands across middle
                    val mistY = height * 0.50f + sin(animationPhase.toDouble()).toFloat() * (height * 0.03f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                theme.primaryAccent.copy(alpha = 0.16f),
                                theme.secondaryAccent.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            startY = mistY - 100.dp.toPx(),
                            endY = mistY + 120.dp.toPx()
                        ),
                        topLeft = Offset(0f, mistY - 100.dp.toPx()),
                        size = Size(width, 220.dp.toPx())
                    )

                    // 4. Near row of darker pine silhouettes
                    val nearTreeColor = theme.secondaryAccent.copy(alpha = 0.55f)
                    val nearTreePositions = listOf(0.0f, 0.16f, 0.34f, 0.52f, 0.70f, 0.86f, 1.0f)
                    for (relX in nearTreePositions) {
                        val treeX = width * relX
                        val treeBaseY = height
                        val treeHeight = 150.dp.toPx()
                        val treePath = Path().apply {
                            moveTo(treeX, treeBaseY - treeHeight)
                            lineTo(treeX + 38.dp.toPx(), treeBaseY)
                            lineTo(treeX - 38.dp.toPx(), treeBaseY)
                            close()
                        }
                        drawPath(treePath, color = nearTreeColor)
                    }
                }

                FocusAestheticTheme.RAINY_WINDOW -> {
                    // 1. Ambient rain storm twilight glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(width * 0.65f, height * 0.28f),
                            radius = width * 0.60f * pulseScale
                        ),
                        radius = width * 0.60f * pulseScale,
                        center = Offset(width * 0.65f, height * 0.28f)
                    )

                    // 2. Window pane divider frames (subtle dark structure)
                    val frameColor = Color.Black.copy(alpha = 0.35f)
                    // Vertical pane
                    drawRect(
                        color = frameColor,
                        topLeft = Offset(width * 0.5f - 4.dp.toPx(), 0f),
                        size = Size(8.dp.toPx(), height)
                    )
                    // Horizontal pane
                    drawRect(
                        color = frameColor,
                        topLeft = Offset(0f, height * 0.42f - 4.dp.toPx()),
                        size = Size(width, 8.dp.toPx())
                    )

                    // 3. Angled rain streaks across the window plane
                    val rainOffset = (animationPhase * 70f) % 90f
                    val streakColor = theme.primaryAccent.copy(alpha = 0.25f)
                    for (i in 0..18) {
                        val x = (width * (i / 18f) + (i * 23f)) % width
                        val y = ((i * 59f) + rainOffset * 8f) % height
                        drawLine(
                            color = streakColor,
                            start = Offset(x, y),
                            end = Offset(x - 8.dp.toPx(), y + 42.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // 4. Soft moisture droplet bokeh points
                    val dropletColor = theme.secondaryAccent.copy(alpha = 0.18f)
                    val droplets = listOf(
                        Offset(width * 0.22f, height * 0.20f),
                        Offset(width * 0.78f, height * 0.25f),
                        Offset(width * 0.35f, height * 0.65f),
                        Offset(width * 0.82f, height * 0.70f),
                        Offset(width * 0.15f, height * 0.85f)
                    )
                    for (dp in droplets) {
                        drawCircle(color = dropletColor, radius = 5.dp.toPx(), center = dp)
                    }
                }

                FocusAestheticTheme.DARK_BAMBOO -> {
                    // 1. Filtered bamboo grove ambient glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(width * 0.5f, height * 0.50f),
                            radius = width * 0.60f * pulseScale
                        ),
                        radius = width * 0.60f * pulseScale,
                        center = Offset(width * 0.5f, height * 0.50f)
                    )

                    // 2. Distinct bamboo stalks (vertical silhouettes)
                    val stalkSpecs = listOf(
                        Triple(0.09f, 16.dp.toPx(), 0.35f),
                        Triple(0.20f, 22.dp.toPx(), 0.45f),
                        Triple(0.33f, 14.dp.toPx(), 0.30f),
                        Triple(0.72f, 18.dp.toPx(), 0.40f),
                        Triple(0.85f, 24.dp.toPx(), 0.50f),
                        Triple(0.95f, 16.dp.toPx(), 0.35f)
                    )

                    for ((relX, stalkWidth, stalkAlpha) in stalkSpecs) {
                        val stalkX = width * relX
                        val stalkColor = theme.secondaryAccent.copy(alpha = stalkAlpha)
                        val nodeColor = Color.Black.copy(alpha = 0.45f)

                        // Main stalk
                        drawRect(
                            color = stalkColor,
                            topLeft = Offset(stalkX - stalkWidth / 2f, 0f),
                            size = Size(stalkWidth, height)
                        )

                        // Node rings along stalk
                        for (nodeY in listOf(height * 0.18f, height * 0.38f, height * 0.58f, height * 0.78f, height * 0.94f)) {
                            drawRect(
                                color = nodeColor,
                                topLeft = Offset(stalkX - stalkWidth * 0.6f, nodeY - 2.dp.toPx()),
                                size = Size(stalkWidth * 1.2f, 4.dp.toPx())
                            )
                        }
                    }

                    // 3. Bamboo leaves drooping from top
                    val leafColor = theme.primaryAccent.copy(alpha = 0.28f)
                    val leafPaths = listOf(
                        Path().apply {
                            moveTo(width * 0.10f, 0f)
                            quadraticTo(width * 0.16f, 60.dp.toPx(), width * 0.25f, 80.dp.toPx())
                            quadraticTo(width * 0.18f, 50.dp.toPx(), width * 0.10f, 0f)
                        },
                        Path().apply {
                            moveTo(width * 0.85f, 0f)
                            quadraticTo(width * 0.80f, 70.dp.toPx(), width * 0.72f, 95.dp.toPx())
                            quadraticTo(width * 0.78f, 60.dp.toPx(), width * 0.85f, 0f)
                        }
                    )
                    for (lp in leafPaths) {
                        drawPath(lp, color = leafColor)
                    }
                }

                FocusAestheticTheme.MOUNTAIN_MIST -> {
                    // 1. High-altitude dusk atmospheric glow behind mountains
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(width * 0.5f, height * 0.32f),
                            radius = width * 0.65f * pulseScale
                        ),
                        radius = width * 0.65f * pulseScale,
                        center = Offset(width * 0.5f, height * 0.32f)
                    )

                    // 2. Far mountain ridge silhouette
                    val farRidgeColor = theme.secondaryAccent.copy(alpha = 0.25f)
                    val farRidgePath = Path().apply {
                        moveTo(0f, height * 0.56f)
                        lineTo(width * 0.25f, height * 0.48f)
                        lineTo(width * 0.50f, height * 0.53f)
                        lineTo(width * 0.78f, height * 0.44f)
                        lineTo(width, height * 0.52f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(farRidgePath, color = farRidgeColor)

                    // 3. Valley mist layer nestled between far and mid ridges
                    val mistY = height * 0.62f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                theme.primaryAccent.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            startY = mistY - 40.dp.toPx(),
                            endY = mistY + 60.dp.toPx()
                        ),
                        topLeft = Offset(0f, mistY - 40.dp.toPx()),
                        size = Size(width, 100.dp.toPx())
                    )

                    // 4. Mid mountain ridge silhouette
                    val midRidgeColor = theme.secondaryAccent.copy(alpha = 0.45f)
                    val midRidgePath = Path().apply {
                        moveTo(0f, height * 0.68f)
                        lineTo(width * 0.35f, height * 0.62f)
                        lineTo(width * 0.65f, height * 0.67f)
                        lineTo(width, height * 0.59f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(midRidgePath, color = midRidgeColor)

                    // 5. Near dark ridge
                    val nearRidgeColor = Color.Black.copy(alpha = 0.65f)
                    val nearRidgePath = Path().apply {
                        moveTo(0f, height * 0.82f)
                        lineTo(width * 0.45f, height * 0.77f)
                        lineTo(width, height * 0.84f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(nearRidgePath, color = nearRidgeColor)
                }

                FocusAestheticTheme.QUIET_OCEAN -> {
                    val oceanHorizonY = height * 0.58f

                    // 1. Low celestial glow right on the horizon
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(width * 0.5f, oceanHorizonY),
                            radius = width * 0.50f * pulseScale
                        ),
                        radius = width * 0.50f * pulseScale,
                        center = Offset(width * 0.5f, oceanHorizonY)
                    )

                    // 2. Horizon divider line
                    drawLine(
                        color = theme.primaryAccent.copy(alpha = 0.35f),
                        start = Offset(0f, oceanHorizonY),
                        end = Offset(width, oceanHorizonY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // 3. Ocean body filling lower half with deep gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                theme.secondaryAccent.copy(alpha = 0.35f),
                                theme.backgroundBottom
                            ),
                            startY = oceanHorizonY,
                            endY = height
                        ),
                        topLeft = Offset(0f, oceanHorizonY),
                        size = Size(width, height - oceanHorizonY)
                    )

                    // 4. Subtle horizontal water reflection ripples on the sea
                    val rippleAlpha = 0.22f
                    val waveYOffsets = listOf(0.63f, 0.69f, 0.76f, 0.84f, 0.92f)
                    for ((index, relY) in waveYOffsets.withIndex()) {
                        val waveY = height * relY
                        val waveWidth = width * (0.8f - index * 0.1f)
                        val startX = (width - waveWidth) / 2f
                        val waveAnim = sin(animationPhase.toDouble() + index * 0.8).toFloat() * 12.dp.toPx()

                        drawLine(
                            color = theme.primaryAccent.copy(alpha = rippleAlpha / (index + 1)),
                            start = Offset(startX + waveAnim, waveY),
                            end = Offset(startX + waveWidth + waveAnim, waveY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                FocusAestheticTheme.STARRY_NIGHT -> {
                    // 1. Cosmic lavender nebula cloud
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                theme.primaryAccent.copy(alpha = 0.26f),
                                theme.secondaryAccent.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.45f, height * 0.36f),
                            radius = width * 0.65f * pulseScale
                        ),
                        radius = width * 0.65f * pulseScale,
                        center = Offset(width * 0.45f, height * 0.36f)
                    )

                    // 2. Restrained crisp star points across sky (calm organic breathing twinkle)
                    val starPositions = listOf(
                        Offset(width * 0.10f, height * 0.12f) to 2.2.dp.toPx(),
                        Offset(width * 0.22f, height * 0.18f) to 1.6.dp.toPx(),
                        Offset(width * 0.35f, height * 0.10f) to 2.0.dp.toPx(),
                        Offset(width * 0.48f, height * 0.22f) to 1.4.dp.toPx(),
                        Offset(width * 0.62f, height * 0.14f) to 2.4.dp.toPx(),
                        Offset(width * 0.78f, height * 0.08f) to 1.8.dp.toPx(),
                        Offset(width * 0.90f, height * 0.20f) to 2.0.dp.toPx(),
                        Offset(width * 0.15f, height * 0.32f) to 1.6.dp.toPx(),
                        Offset(width * 0.30f, height * 0.42f) to 2.2.dp.toPx(),
                        Offset(width * 0.70f, height * 0.35f) to 1.5.dp.toPx(),
                        Offset(width * 0.85f, height * 0.45f) to 2.0.dp.toPx(),
                        Offset(width * 0.18f, height * 0.62f) to 1.8.dp.toPx(),
                        Offset(width * 0.82f, height * 0.68f) to 2.2.dp.toPx(),
                        Offset(width * 0.28f, height * 0.78f) to 1.5.dp.toPx(),
                        Offset(width * 0.72f, height * 0.82f) to 1.8.dp.toPx()
                    )

                    for ((index, pair) in starPositions.withIndex()) {
                        val (pos, radius) = pair
                        val twinkle = 0.35f + sin(animationPhase.toDouble() + index * 1.3).toFloat() * 0.25f
                        drawCircle(
                            color = Color.White.copy(alpha = twinkle.coerceIn(0.15f, 0.75f)),
                            radius = radius,
                            center = pos
                        )
                    }

                    // 3. Faint terrestrial horizon silhouette at base
                    drawRect(
                        color = Color.Black.copy(alpha = 0.60f),
                        topLeft = Offset(0f, height * 0.88f),
                        size = Size(width, height * 0.12f)
                    )
                }

                FocusAestheticTheme.COZY_STUDY -> {
                    val lampX = width * 0.84f
                    val lampY = height * 0.14f

                    // 1. Warm amber study lamp radiating glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                theme.primaryAccent.copy(alpha = 0.38f),
                                theme.secondaryAccent.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(lampX, lampY),
                            radius = width * 0.75f * pulseScale
                        ),
                        radius = width * 0.75f * pulseScale,
                        center = Offset(lampX, lampY)
                    )

                    // 2. Reading light cone spreading down-left over desk
                    val lightConePath = Path().apply {
                        moveTo(lampX, lampY)
                        lineTo(width * 0.10f, height)
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = lightConePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                theme.primaryAccent.copy(alpha = 0.15f),
                                theme.secondaryAccent.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startY = lampY,
                            endY = height
                        )
                    )

                    // 3. Warm reading ambient pool on lower center
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(width * 0.45f, height * 0.80f),
                            radius = width * 0.50f
                        ),
                        radius = width * 0.50f,
                        center = Offset(width * 0.45f, height * 0.80f)
                    )

                    // 4. Subtle timber bookshelf edge on left
                    drawRect(
                        color = Color.Black.copy(alpha = 0.40f),
                        topLeft = Offset(0f, 0f),
                        size = Size(width * 0.12f, height)
                    )
                }

                FocusAestheticTheme.GREEN_GARDEN -> {
                    // 1. Dappled sunlight filtering through botanical canopy
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primaryAccent.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(width * 0.40f, height * 0.28f),
                            radius = width * 0.60f * pulseScale
                        ),
                        radius = width * 0.60f * pulseScale,
                        center = Offset(width * 0.40f, height * 0.28f)
                    )

                    // 2. Hanging foliage vines from top-left
                    val vineColor = theme.secondaryAccent.copy(alpha = 0.42f)
                    val leafColor = theme.primaryAccent.copy(alpha = 0.35f)

                    val vinePath = Path().apply {
                        moveTo(0f, 0f)
                        quadraticTo(width * 0.18f, height * 0.15f, width * 0.28f, height * 0.26f)
                        quadraticTo(width * 0.20f, height * 0.12f, 0f, 0f)
                    }
                    drawPath(vinePath, color = vineColor)

                    // Botanical leaf curves
                    val gardenLeaves = listOf(
                        Offset(width * 0.14f, height * 0.10f),
                        Offset(width * 0.22f, height * 0.18f),
                        Offset(width * 0.27f, height * 0.25f),
                        Offset(width * 0.82f, height * 0.12f),
                        Offset(width * 0.88f, height * 0.20f)
                    )
                    for (leafPos in gardenLeaves) {
                        drawCircle(color = leafColor, radius = 16.dp.toPx(), center = leafPos)
                    }

                    // 3. Lower garden foliage bed
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                theme.secondaryAccent.copy(alpha = 0.45f),
                                theme.backgroundBottom
                            ),
                            startY = height * 0.78f,
                            endY = height
                        ),
                        topLeft = Offset(0f, height * 0.78f),
                        size = Size(width, height * 0.22f)
                    )
                }

                FocusAestheticTheme.SOFT_SUNSET -> {
                    val sunsetY = height * 0.70f

                    // 1. Soft setting sun glowing disk
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                theme.primaryAccent.copy(alpha = 0.45f),
                                theme.secondaryAccent.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, sunsetY),
                            radius = width * 0.55f * pulseScale
                        ),
                        radius = width * 0.55f * pulseScale,
                        center = Offset(width * 0.5f, sunsetY)
                    )

                    // 2. Dusk horizon warm twilight gradation
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                theme.primaryAccent.copy(alpha = 0.25f),
                                theme.secondaryAccent.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            startY = sunsetY - 80.dp.toPx(),
                            endY = sunsetY + 60.dp.toPx()
                        ),
                        topLeft = Offset(0f, sunsetY - 80.dp.toPx()),
                        size = Size(width, 140.dp.toPx())
                    )

                    // 3. Low dusk earth horizon band
                    drawRect(
                        color = Color.Black.copy(alpha = 0.65f),
                        topLeft = Offset(0f, sunsetY + 20.dp.toPx()),
                        size = Size(width, height - (sunsetY + 20.dp.toPx()))
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
