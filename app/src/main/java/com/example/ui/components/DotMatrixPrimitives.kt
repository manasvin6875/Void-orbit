package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskCategory
import com.example.ui.theme.LocalNothingPalette
import com.example.ui.theme.NdDotMatrixFontFamily
import com.example.ui.theme.PixelBlockFontFamily
import com.example.ui.theme.TechnicalMonoFontFamily
import kotlin.math.floor

/**
 * Full-bleed fixed dot-matrix background inspired by Nothing's official website and phone app-grid.
 * The dots remain fixed in viewport coordinates regardless of content scrolling.
 */
@Composable
fun FixedDotMatrixBackground(
    modifier: Modifier = Modifier,
    gridSpacingDp: Dp = 16.dp,
    dotRadiusDp: Dp = 1.25.dp
) {
    val palette = LocalNothingPalette.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val stepPx = gridSpacingDp.toPx().coerceAtLeast(8f)
        val radiusPx = dotRadiusDp.toPx()
        val cols = (size.width / stepPx).toInt() + 1
        val rows = (size.height / stepPx).toInt() + 1
        val startX = (size.width - (cols - 1) * stepPx) * 0.5f
        val startY = (size.height - (rows - 1) * stepPx) * 0.5f

        for (r in 0 until rows) {
            val y = startY + r * stepPx
            for (c in 0 until cols) {
                val x = startX + c * stepPx
                drawCircle(
                    color = palette.dotMatrixGrid,
                    radius = radiusPx,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Nothing-style Dot Matrix Progress Bar:
 * - Completed / already elapsed time dots: White (Dark Mode) or Black (Light Mode)
 * - Remaining time left dots: Nothing Red (#D71921)
 */
@Composable
fun DotMatrixProgressBar(
    label: String,
    remainingPercent: Int,
    doneFraction: Float,
    modifier: Modifier = Modifier,
    columns: Int = 20,
    rows: Int = 2,
    testTagName: String = "dot_matrix_progress"
) {
    val palette = LocalNothingPalette.current
    val totalDots = columns * rows
    val doneDots = (doneFraction.coerceIn(0f, 1f) * totalDots).toInt().coerceIn(0, totalDots)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTagName)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                fontFamily = PixelBlockFontFamily,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = palette.mutedInk
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = palette.nothingRed)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$remainingPercent% LEFT",
                    fontFamily = NdDotMatrixFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.nothingRed
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height((rows * 7 + (rows - 1) * 4).dp)
        ) {
            val colSpacing = size.width / columns.toFloat()
            val rowSpacing = size.height / rows.toFloat()
            val dotRadius = (minOf(colSpacing, rowSpacing) * 0.34f).coerceIn(2.2f, 6.5f)

            var dotIndex = 0
            for (c in 0 until columns) {
                for (r in 0 until rows) {
                    val cx = c * colSpacing + colSpacing * 0.5f
                    val cy = r * rowSpacing + rowSpacing * 0.5f
                    val isAlreadyDone = dotIndex < doneDots
                    val dotColor = if (isAlreadyDone) {
                        palette.primaryInk // White in Dark mode, Black in Light mode
                    } else {
                        palette.nothingRed // Red for time left!
                    }
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(cx, cy)
                    )
                    dotIndex++
                }
            }
        }
    }
}

/**
 * Perforated Coil-Diary Folded Paper Cross (+) Add Button:
 * - Looks like a square sheet of coil-diary paper with perforated semicircular edge notches.
 * - Its 4 triangular flaps are folded inward toward the center over a bright Nothing Red interior.
 * - Where the sides meet, an intentional slight gap exposes the inner red and forms a dotted red cross (+).
 * - Fits harmoniously beside the Today's Date block without triggering visual OCD.
 */
@Composable
fun PerforatedFoldedPaperAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNothingPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "paper_pulse")
    val crossGlow by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cross_glow"
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .semantics {
                contentDescription = "Open perforated red task paper"
                role = Role.Button
            }
            .testTag("perforated_add_task_button")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(42.dp)) {
            val w = size.width
            val h = size.height
            val pad = 2.dp.toPx()
            val paperRect = Rect(pad, pad, w - pad, h - pad)

            // Build base square path and subtract coil-diary semicircular edge perforations
            val baseSquare = Path().apply { addRect(paperRect) }
            val notchesPath = Path()
            val notchCount = 5
            val stepX = paperRect.width / notchCount
            val stepY = paperRect.height / notchCount
            val notchRadius = 2.1.dp.toPx()

            for (i in 0 until notchCount) {
                val nx = paperRect.left + (i + 0.5f) * stepX
                val ny = paperRect.top + (i + 0.5f) * stepY
                // Top & bottom coil-diary teeth
                notchesPath.addOval(Rect(Offset(nx, paperRect.top), notchRadius))
                notchesPath.addOval(Rect(Offset(nx, paperRect.bottom), notchRadius))
                // Left & right coil-diary teeth
                notchesPath.addOval(Rect(Offset(paperRect.left, ny), notchRadius))
                notchesPath.addOval(Rect(Offset(paperRect.right, ny), notchRadius))
            }

            val perforatedPaper = Path.combine(PathOperation.Difference, baseSquare, notchesPath)

            // 1. Inner bright Nothing Red core of the paper
            drawPath(
                path = perforatedPaper,
                color = palette.nothingRed.copy(alpha = crossGlow)
            )

            // 2. Four corner flaps folded inward toward the center, leaving an intentional gap
            // along the horizontal & vertical axes that exposes the inner red as a dotted cross (+)
            val cx = w * 0.5f
            val cy = h * 0.5f
            val gapHalf = 3.2.dp.toPx() // Intentional gap exposing the inner red cross
            val flapSurface = palette.backgroundCanvas
            val flapBorder = palette.primaryInk.copy(alpha = 0.65f)

            val quadrantRects = listOf(
                // Top-Left folded flap
                Rect(paperRect.left, paperRect.top, cx - gapHalf, cy - gapHalf),
                // Top-Right folded flap
                Rect(cx + gapHalf, paperRect.top, paperRect.right, cy - gapHalf),
                // Bottom-Left folded flap
                Rect(paperRect.left, cy + gapHalf, cx - gapHalf, paperRect.bottom),
                // Bottom-Right folded flap
                Rect(cx + gapHalf, cy + gapHalf, paperRect.right, paperRect.bottom)
            )

            quadrantRects.forEach { qRect ->
                val qPath = Path().apply { addRect(qRect) }
                val clippedFlap = Path.combine(PathOperation.Intersect, perforatedPaper, qPath)
                drawPath(path = clippedFlap, color = flapSurface)
                drawPath(
                    path = clippedFlap,
                    color = flapBorder,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 3. Draw crisp dot-matrix nodes inside the exposed red cross gap
            // so the exposed inner red explicitly forms a Nothing dot-matrix cross (+)
            val dotStep = 3.6.dp.toPx()
            val dotRad = 1.25.dp.toPx()
            // Mask the cross background slightly so individual red matrix dots pop
            drawRect(
                color = palette.backgroundCanvas,
                topLeft = Offset(cx - gapHalf, paperRect.top + 2.dp.toPx()),
                size = Size(gapHalf * 2f, paperRect.height - 4.dp.toPx())
            )
            drawRect(
                color = palette.backgroundCanvas,
                topLeft = Offset(paperRect.left + 2.dp.toPx(), cy - gapHalf),
                size = Size(paperRect.width - 4.dp.toPx(), gapHalf * 2f)
            )

            for (step in -4..4) {
                // Vertical arm of red dot-matrix cross
                drawCircle(
                    color = palette.nothingRed,
                    radius = if (step == 0) dotRad * 1.25f else dotRad,
                    center = Offset(cx, cy + step * dotStep)
                )
                // Horizontal arm of red dot-matrix cross
                if (step != 0) {
                    drawCircle(
                        color = palette.nothingRed,
                        radius = dotRad,
                        center = Offset(cx + step * dotStep, cy)
                    )
                }
            }

            // Diagonal crease hairlines hinting at the 4 folded corners
            val creaseColor = palette.mutedInk.copy(alpha = 0.35f)
            drawLine(
                color = creaseColor,
                start = Offset(paperRect.left + 3.dp.toPx(), paperRect.top + 3.dp.toPx()),
                end = Offset(cx - gapHalf, cy - gapHalf),
                strokeWidth = 0.8.dp.toPx()
            )
            drawLine(
                color = creaseColor,
                start = Offset(paperRect.right - 3.dp.toPx(), paperRect.top + 3.dp.toPx()),
                end = Offset(cx + gapHalf, cy - gapHalf),
                strokeWidth = 0.8.dp.toPx()
            )
            drawLine(
                color = creaseColor,
                start = Offset(paperRect.left + 3.dp.toPx(), paperRect.bottom - 3.dp.toPx()),
                end = Offset(cx - gapHalf, cy + gapHalf),
                strokeWidth = 0.8.dp.toPx()
            )
            drawLine(
                color = creaseColor,
                start = Offset(paperRect.right - 3.dp.toPx(), paperRect.bottom - 3.dp.toPx()),
                end = Offset(cx + gapHalf, cy + gapHalf),
                strokeWidth = 0.8.dp.toPx()
            )
        }
    }
}

/**
 * Nothing-style Pixel Art Icon Renderer (11x11 pixel grid) for the 10 Task Categories
 * and special timeline badges (Alarm, Sun, Paw, Coffee, Sleep ZzZ, Phone, Cake, Snowflake).
 */
@Composable
fun PixelArtGlyphCanvas(
    glyphKey: String,
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val grid = remember(glyphKey) { getPixelMatrixForKey(glyphKey) }
    Canvas(modifier = modifier) {
        val rows = grid.size
        val cols = grid.firstOrNull()?.length ?: 11
        val cellW = size.width / cols.toFloat()
        val cellH = size.height / rows.toFloat()
        val pixelSize = minOf(cellW, cellH) * 0.88f

        for (r in 0 until rows) {
            val rowStr = grid[r]
            for (c in 0 until rowStr.length) {
                if (rowStr[c] == '#') {
                    drawRect(
                        color = tint,
                        topLeft = Offset(c * cellW + (cellW - pixelSize) * 0.5f, r * cellH + (cellH - pixelSize) * 0.5f),
                        size = Size(pixelSize, pixelSize)
                    )
                }
            }
        }
    }
}

private fun getPixelMatrixForKey(key: String): List<String> {
    return when (key.uppercase()) {
        "WORK" -> listOf(
            "...#####...",
            "...#...#...",
            ".#########.",
            "#.........#",
            "#...###...#",
            "###########",
            "#...###...#",
            "#.........#",
            "#.........#",
            ".#########.",
            "..........."
        )
        "STUDY" -> listOf(
            ".####.####.",
            "#...#.#...#",
            "#.#.#.#.#.#",
            "#...#.#...#",
            "#.#.#.#.#.#",
            "#...#.#...#",
            "#...#.#...#",
            ".####.####.",
            ".....#.....",
            "...#####...",
            "..........."
        )
        "FITNESS" -> listOf(
            "...........",
            ".##.....##.",
            "####...####",
            "####...####",
            ".##.###.##.",
            "...#####...",
            ".##.###.##.",
            "####...####",
            "####...####",
            ".##.....##.",
            "..........."
        )
        "HEALTH" -> listOf(
            ".###...###.",
            "#####.#####",
            "###########",
            "#####.#####",
            "###.###.###",
            ".####.####.",
            "..#######..",
            "...#####...",
            "....###....",
            ".....#.....",
            "..........."
        )
        "CREATIVE" -> listOf(
            "........##.",
            ".......####",
            "......####.",
            ".....####..",
            "....####...",
            "...####....",
            "..####.....",
            ".####......",
            "###........",
            "##...#####.",
            "..........."
        )
        "SOCIAL" -> listOf(
            ".#######...",
            "#.......#..",
            "#.##.##.#..",
            "#.......###",
            ".#######..#",
            "..#...#...#",
            "....#.#####",
            "........#..",
            ".......#...",
            "...........",
            "..........."
        )
        "ERRANDS" -> listOf(
            "..#######..",
            ".#.......#.",
            "#..#####..#",
            "#.#.....#.#",
            "#.#..#..#.#",
            "#.#.###.#.#",
            "#.#.....#.#",
            "#..#####..#",
            ".#.......#.",
            "..#######..",
            "..........."
        )
        "FINANCE" -> listOf(
            "....###....",
            "..#######..",
            ".##..#..##.",
            ".##..#.... ",
            "..#####....",
            "....#####..",
            "....#..##..",
            ".##..#..##.",
            "..#######..",
            "....###....",
            "..........."
        )
        "MIND" -> listOf(
            ".....#.....",
            "....###....",
            ".#..###..#.",
            "###.###.###",
            "###########",
            ".#########.",
            "..#######..",
            "...#####...",
            ".#########.",
            "...........",
            "..........."
        )
        "GAMING" -> listOf(
            "..#######..",
            ".#########.",
            "###.###.###",
            "###########",
            "#.#######.#",
            "#.#.....#.#",
            "#...###...#",
            ".##.....##.",
            "...........",
            "...........",
            "..........."
        )
        "ALARM" -> listOf(
            "##.......##",
            "###.###.###",
            "..#######..",
            ".##..#..##.",
            ".#...#...#.",
            ".#...###.#.",
            ".#.......#.",
            ".##.....##.",
            "..#######..",
            ".#.......#.",
            "..........."
        )
        "SUN" -> listOf(
            ".....#.....",
            ".#...#...#.",
            "..#.###.#..",
            "...#####...",
            "###.#####.##",
            "...#####...",
            "..#.###.#..",
            ".#...#...#.",
            ".....#.....",
            "...........",
            "..........."
        )
        "PAW" -> listOf(
            ".##.....##.",
            ".##.###.##.",
            "....###....",
            "##.......##",
            "##.#####.##",
            "..#######..",
            ".#########.",
            ".#########.",
            "..###.###..",
            "...........",
            "..........."
        )
        "COFFEE" -> listOf(
            "...#...#...",
            "..#...#....",
            "...#...#...",
            ".#########.",
            ".#.......##",
            ".#.......#.#",
            ".#.......##",
            "..#######..",
            "###########",
            "...........",
            "..........."
        )
        "SLEEP" -> listOf(
            "#####......",
            "...#.......",
            "..#...####.",
            ".#......#..",
            "#####..#...",
            "......####.",
            "........###",
            ".........#.",
            "........###",
            "...........",
            "..........."
        )
        "PHONE" -> listOf(
            "..###......",
            ".####......",
            ".###.......",
            "..###......",
            "...###.....",
            "....###....",
            ".....###...",
            "......####.",
            ".....####..",
            "...........",
            "..........."
        )
        "CAKE" -> listOf(
            ".....#.....",
            "....###....",
            ".....#.....",
            "...#####...",
            "...#####...",
            ".#########.",
            ".#.#.#.#.#.",
            ".#########.",
            "###########",
            "...........",
            "..........."
        )
        "SNOWFLAKE" -> listOf(
            ".....#.....",
            ".#..###..#.",
            "..#.###.#..",
            "...#####...",
            "###########",
            "...#####...",
            "..#.###.#..",
            ".#..###..#.",
            ".....#.....",
            "...........",
            "..........."
        )
        else -> listOf(
            "...#####...",
            ".##.....##.",
            "#....#....#",
            "#...###...#",
            "#..#####..#",
            "#...###...#",
            "#....#....#",
            ".##.....##.",
            "...#####...",
            "...........",
            "..........."
        )
    }
}
