package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.data.DifficultyTier
import com.example.data.TaskCategory
import com.example.ui.theme.NothingRed
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Procedural Pixel-Art Alien Planet Renderer inspired by Deep-Fold/PixelPlanets.
 * - Synthesizes a quantized pixel sphere using spherical UV mapping + 2D value noise + 4x4 Bayer dithering.
 * - Includes tilted planetary rings for A/S tier tasks and an orbiting pixel moon for B/S tier tasks.
 * - When [isRotating] is true (e.g., completed task or inside the Quest inspection window),
 *   the planet rotates continuously around its axis and its moon orbits in real time.
 */
@Composable
fun ProceduralPlanetCanvas(
    seed: Int,
    category: TaskCategory,
    difficulty: DifficultyTier,
    isRotating: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "planet_axial_spin")
    val spinPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRotating) 5200 else 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_phase"
    )
    val effectivePhase = if (isRotating) spinPhase else (seed % 360) * 0.01745f

    Canvas(modifier = modifier) {
        val gridRes = difficulty.planetPixelGrid
        val cellSize = size.width / (gridRes + 8).toFloat()
        val centerIdx = (gridRes + 8) / 2f
        val planetRadiusCells = gridRes * 0.44f

        val primaryColor = category.timelineColor
        val secondaryColor = category.secondaryColor
        val highlightColor = Color.White.copy(alpha = 0.92f)
        val shadowColor = Color(0xFF0A0B0E)
        val ringColor = if (difficulty == DifficultyTier.S) NothingRed else primaryColor.copy(alpha = 0.85f)

        // 1. Back half of planetary rings (behind the sphere)
        if (difficulty.hasRings) {
            drawPixelRingHalf(
                centerIdx = centerIdx,
                cellSize = cellSize,
                innerRadius = planetRadiusCells * 1.22f,
                outerRadius = planetRadiusCells * 1.68f,
                tilt = 0.34f,
                isFrontHalf = false,
                ringColor = ringColor,
                secondaryRingColor = secondaryColor
            )
        }

        // 2. Quantized Spherified Pixel Planet Body
        val lightDirX = -0.58f
        val lightDirY = -0.45f
        val lightDirZ = 0.68f

        val totalCells = (gridRes + 8)
        for (gy in 0 until totalCells) {
            for (gx in 0 until totalCells) {
                val nx = (gx - centerIdx + 0.5f) / planetRadiusCells
                val ny = (gy - centerIdx + 0.5f) / planetRadiusCells
                val r2 = nx * nx + ny * ny
                if (r2 <= 1.0f) {
                    val nz = sqrt((1.0f - r2).coerceAtLeast(0f))
                    // Spherical coordinates with axial rotation
                    val lon = atan2(nx, nz) + effectivePhase
                    val lat = acos(ny.coerceIn(-1f, 1f))

                    // Multi-octave deterministic alien terrain / gas band noise
                    val n1 = pseudoNoise2D(lon * 2.4f + seed * 0.13f, lat * 3.1f + seed * 0.07f)
                    val n2 = pseudoNoise2D(lon * 5.2f - seed * 0.21f, lat * 6.0f + effectivePhase * 0.3f)
                    val bandWave = sin(lat * 7.5f + n1 * 2.2f + seed)
                    val elevation = (n1 * 0.62f + n2 * 0.38f + bandWave * 0.18f)

                    // Lambertian lighting + Bayer 4x4 ordered dithering
                    val lambert = (nx * lightDirX + ny * lightDirY + nz * lightDirZ).coerceIn(0f, 1f)
                    val bayerThreshold = bayer4x4(gx, gy)

                    // Polar ice / energy caps
                    val isPolarCap = abs(ny) > 0.82f && elevation > -0.1f

                    val basePixelColor = when {
                        isPolarCap -> highlightColor
                        elevation > 0.28f -> primaryColor
                        elevation > -0.12f -> lerpColor(primaryColor, secondaryColor, 0.55f)
                        else -> secondaryColor
                    }

                    // Rim atmosphere glow on completed / S-tier planets
                    val isAtmosphereRim = r2 > 0.84f && lambert > 0.25f
                    val finalPixelColor = when {
                        isAtmosphereRim && isRotating -> highlightColor
                        lambert + (bayerThreshold - 0.5f) * 0.35f < 0.22f -> lerpColor(basePixelColor, shadowColor, 0.72f)
                        lambert + (bayerThreshold - 0.5f) * 0.28f > 0.78f -> lerpColor(basePixelColor, highlightColor, 0.38f)
                        else -> basePixelColor
                    }

                    drawRect(
                        color = finalPixelColor,
                        topLeft = Offset(gx * cellSize, gy * cellSize),
                        size = Size(cellSize * 0.94f, cellSize * 0.94f)
                    )
                }
            }
        }

        // 3. Front half of planetary rings (in front of the sphere)
        if (difficulty.hasRings) {
            drawPixelRingHalf(
                centerIdx = centerIdx,
                cellSize = cellSize,
                innerRadius = planetRadiusCells * 1.22f,
                outerRadius = planetRadiusCells * 1.68f,
                tilt = 0.34f,
                isFrontHalf = true,
                ringColor = ringColor,
                secondaryRingColor = highlightColor
            )
        }

        // 4. Orbiting Pixel Moon for B and S tier tasks
        if (difficulty.hasMoon) {
            val moonAngle = effectivePhase * 1.6f + (seed % 7)
            val moonOrbitRad = planetRadiusCells * 1.45f
            val mx = centerIdx + cos(moonAngle) * moonOrbitRad
            val my = centerIdx + sin(moonAngle) * moonOrbitRad * 0.55f
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (abs(dx) + abs(dy) <= 1) {
                        drawRect(
                            color = if (dx <= 0 && dy <= 0) highlightColor else NothingRed,
                            topLeft = Offset((mx + dx) * cellSize, (my + dy) * cellSize),
                            size = Size(cellSize * 0.95f, cellSize * 0.95f)
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelRingHalf(
    centerIdx: Float,
    cellSize: Float,
    innerRadius: Float,
    outerRadius: Float,
    tilt: Float,
    isFrontHalf: Boolean,
    ringColor: Color,
    secondaryRingColor: Color
) {
    val steps = 54
    val startStep = if (isFrontHalf) 0 else steps / 2
    val endStep = if (isFrontHalf) steps / 2 else steps

    for (i in startStep..endStep) {
        val angle = (i.toFloat() / steps.toFloat()) * (2f * PI.toFloat())
        val cosA = cos(angle)
        val sinA = sin(angle)
        for (band in 0..1) {
            val r = if (band == 0) innerRadius else outerRadius
            // Tilt the ring by -18 degrees for dramatic orbital perspective
            val rx = cosA * r
            val ry = sinA * r * tilt - rx * 0.22f
            val gx = floor(centerIdx + rx)
            val gy = floor(centerIdx + ry)
            drawRect(
                color = if ((i + band) % 2 == 0) ringColor else secondaryRingColor,
                topLeft = Offset(gx * cellSize, gy * cellSize),
                size = Size(cellSize * 0.92f, cellSize * 0.92f)
            )
        }
    }
}

private fun pseudoNoise2D(x: Float, y: Float): Float {
    val xi = floor(x).toInt()
    val yi = floor(y).toInt()
    val xf = x - floor(x)
    val yf = y - floor(y)
    val u = xf * xf * (3f - 2f * xf)
    val v = yf * yf * (3f - 2f * yf)

    val a = hashCoord(xi, yi)
    val b = hashCoord(xi + 1, yi)
    val c = hashCoord(xi, yi + 1)
    val d = hashCoord(xi + 1, yi + 1)

    val x1 = a + u * (b - a)
    val x2 = c + u * (d - c)
    return (x1 + v * (x2 - x1)) * 2f - 1f
}

private fun hashCoord(x: Int, y: Int): Float {
    var n = x * 374761393 + y * 668265263
    n = (n xor (n shr 13)) * 1274126177
    return ((n xor (n shr 16)) and 0x7fffffff).toFloat() / Int.MAX_VALUE.toFloat()
}

private fun bayer4x4(x: Int, y: Int): Float {
    val matrix = intArrayOf(
        0, 8, 2, 10,
        12, 4, 14, 6,
        3, 11, 1, 9,
        15, 7, 13, 5
    )
    val idx = ((y and 3) shl 2) or (x and 3)
    return matrix[idx] / 16f
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * t,
        green = start.green + (stop.green - start.green) * t,
        blue = start.blue + (stop.blue - start.blue) * t,
        alpha = start.alpha + (stop.alpha - start.alpha) * t
    )
}
