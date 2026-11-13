package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DifficultyTier
import com.example.data.TaskEntity
import com.example.data.TimeFormatUtils
import com.example.ui.OrbitUiState
import com.example.ui.theme.GeometricSansFontFamily
import com.example.ui.theme.LocalNothingPalette
import com.example.ui.theme.NdDotMatrixFontFamily
import com.example.ui.theme.PixelBlockFontFamily
import com.example.ui.theme.TechnicalMonoFontFamily
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun OrbitalHomeScreen(
    uiState: OrbitUiState,
    onOpenTaskPaper: () -> Unit,
    onPlanetTap: (TaskEntity) -> Unit,
    onToggleTimeSim: (Boolean, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // LEFT COLUMN: Asymmetrical-yet-symmetrical Nothing Date Block + Perforated Add Button
        // + Month Dot-Matrix Progress Bar + Day Dot-Matrix Progress Bar underneath + Telemetry Legend
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NothingAsymmetricDateBlock(
                uiState = uiState,
                onAddTap = {
                    if (uiState.settings.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onOpenTaskPaper()
                }
            )

            // Progress Bars Container: Month on top, Day directly underneath
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.hairlineBorder, RoundedCornerShape(6.dp))
                    .background(palette.backgroundCanvas.copy(alpha = 0.78f), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DotMatrixProgressBar(
                    label = "MONTH MATRIX",
                    remainingPercent = uiState.monthRemainingPercent,
                    doneFraction = uiState.monthProgressDoneFraction,
                    columns = 16,
                    rows = 2,
                    testTagName = "month_dot_matrix_bar"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.hairlineBorder)
                )

                DotMatrixProgressBar(
                    label = "DAY MATRIX",
                    remainingPercent = uiState.dayRemainingPercent,
                    doneFraction = uiState.dayProgressDoneFraction,
                    columns = 16,
                    rows = 2,
                    testTagName = "day_dot_matrix_bar"
                )
            }

            // Orbital Telemetry & Difficulty X-Axis Key
            OrbitalTelemetryCard(
                uiState = uiState,
                onPlanetTap = onPlanetTap,
                onToggleTimeSim = onToggleTimeSim
            )
        }

        // RIGHT COLUMN: 07:00 - 22:00 Working Day Meteor Trajectory & Alien Planets
        Box(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .border(1.dp, palette.hairlineBorder, RoundedCornerShape(8.dp))
                .background(palette.backgroundCanvas.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            RightOrbitalMeteorLane(
                uiState = uiState,
                onPlanetTap = { task ->
                    if (uiState.settings.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onPlanetTap(task)
                }
            )
        }
    }
}

@Composable
private fun NothingAsymmetricDateBlock(
    uiState: OrbitUiState,
    onAddTap: () -> Unit
) {
    val palette = LocalNothingPalette.current
    val date = uiState.selectedDate
    val dayNumberStr = "%02d".format(date.dayOfMonth)
    val dayOfWeekShort = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).uppercase()
    val monthShort = date.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).uppercase()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(6.dp))
            .background(palette.backgroundCanvas.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        // Top micro-telemetry header row (symmetrical frame, asymmetrical weight)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "N-OS // ${date.year}",
                fontFamily = TechnicalMonoFontFamily,
                fontSize = 9.sp,
                color = palette.mutedInk
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(5.dp)) {
                    drawCircle(color = palette.nothingRed)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dayOfWeekShort,
                    fontFamily = PixelBlockFontFamily,
                    fontSize = 10.sp,
                    color = palette.primaryInk
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Date + Harmonious Perforated Folded-Paper Cross Button beside the day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayNumberStr,
                    fontFamily = NdDotMatrixFontFamily,
                    fontSize = 48.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk
                )
                Text(
                    text = "$monthShort · SEC-${date.dayOfYear}",
                    fontFamily = PixelBlockFontFamily,
                    fontSize = 11.sp,
                    color = palette.mutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // OCD-safe Perforated Folded-Paper Red Cross Add Button right beside the day
            PerforatedFoldedPaperAddButton(
                onClick = onAddTap
            )
        }
    }
}

@Composable
private fun OrbitalTelemetryCard(
    uiState: OrbitUiState,
    onPlanetTap: (TaskEntity) -> Unit,
    onToggleTimeSim: (Boolean, Int) -> Unit
) {
    val palette = LocalNothingPalette.current
    val completedCount = uiState.selectedDateTasks.count { it.isCompleted }
    val totalCount = uiState.selectedDateTasks.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(6.dp))
            .background(palette.backgroundCanvas.copy(alpha = 0.78f), RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ORBITAL QUESTS",
                fontFamily = PixelBlockFontFamily,
                fontSize = 10.sp,
                color = palette.mutedInk
            )
            Text(
                text = "$completedCount/$totalCount ACTIVE",
                fontFamily = NdDotMatrixFontFamily,
                fontSize = 14.sp,
                color = palette.primaryInk
            )
        }

        // Difficulty X-Axis Orbit Guide (C -> B -> A -> S)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DifficultyTier.entries.forEach { tier ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tier.code,
                        fontFamily = NdDotMatrixFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tier == DifficultyTier.S) palette.nothingRed else palette.primaryInk
                    )
                    Text(
                        text = "${(tier.orbitXFraction * 100).toInt()}%X",
                        fontFamily = TechnicalMonoFontFamily,
                        fontSize = 8.sp,
                        color = palette.mutedInk
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.hairlineBorder)
        )

        // Interactive Meteor Time Scrubber so user can test meteor trail at any working hour
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "METEOR CLOCK",
                fontFamily = TechnicalMonoFontFamily,
                fontSize = 9.sp,
                color = palette.mutedInk
            )
            Text(
                text = TimeFormatUtils.format24HourColon(uiState.currentMinutesFromMidnight),
                fontFamily = NdDotMatrixFontFamily,
                fontSize = 15.sp,
                color = palette.nothingRed
            )
        }

        Slider(
            value = uiState.currentMinutesFromMidnight.toFloat(),
            onValueChange = { newMin ->
                onToggleTimeSim(true, newMin.roundToInt())
            },
            valueRange = (7 * 60f)..(22 * 60f),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .testTag("meteor_time_slider"),
            colors = SliderDefaults.colors(
                thumbColor = palette.nothingRed,
                activeTrackColor = palette.nothingRed,
                inactiveTrackColor = palette.hairlineBorder
            )
        )

        // Compact Planet Directory list for quick one-tap access
        uiState.selectedDateTasks.forEach { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.surfaceElevated.copy(alpha = 0.65f))
                    .clickable { onPlanetTap(task) }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Canvas(modifier = Modifier.size(6.dp)) {
                        drawCircle(
                            color = if (task.isCompleted) palette.nothingRed else task.categoryEnum.timelineColor
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = task.title,
                        fontFamily = GeometricSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = task.difficultyEnum.code,
                    fontFamily = PixelBlockFontFamily,
                    fontSize = 10.sp,
                    color = if (task.difficultyEnum == DifficultyTier.S) palette.nothingRed else palette.mutedInk
                )
            }
        }
    }
}

@Composable
private fun RightOrbitalMeteorLane(
    uiState: OrbitUiState,
    onPlanetTap: (TaskEntity) -> Unit
) {
    val palette = LocalNothingPalette.current
    val density = LocalDensity.current
    val startMin = uiState.workingDayStartMinutes // 420 (07:00)
    val endMin = uiState.workingDayEndMinutes // 1320 (22:00)
    val totalWorkingMinutes = (endMin - startMin).toFloat()

    // Filter tasks inside working day window (07:00 to 22:00)
    val orbitalTasks = uiState.selectedDateTasks.filter {
        it.startMinutes in startMin..endMin
    }.sortedBy { it.startMinutes }

    val infiniteTransition = rememberInfiniteTransition(label = "meteor_pulse")
    val meteorPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meteor_head_pulse"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Top orbital column header showing C / B / A / S X-axis lanes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 34.dp, end = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "07:00 // ORBITAL LANE",
                fontFamily = TechnicalMonoFontFamily,
                fontSize = 9.sp,
                color = palette.mutedInk
            )
            Text(
                text = "C · B · A · S",
                fontFamily = PixelBlockFontFamily,
                fontSize = 9.sp,
                color = palette.nothingRed
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .testTag("orbital_meteor_canvas_box")
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val gutterPx = with(density) { 34.dp.toPx() }
            val topPadPx = with(density) { 14.dp.toPx() }
            val bottomPadPx = with(density) { 16.dp.toPx() }
            val usableHeightPx = (heightPx - topPadPx - bottomPadPx).coerceAtLeast(100f)
            val usableOrbitWidthPx = (widthPx - gutterPx - with(density) { 14.dp.toPx() }).coerceAtLeast(80f)

            fun timeToY(minutes: Int): Float {
                val frac = ((minutes - startMin).toFloat() / totalWorkingMinutes).coerceIn(0f, 1f)
                return topPadPx + frac * usableHeightPx
            }

            fun tierToX(tier: DifficultyTier): Float {
                return gutterPx + tier.orbitXFraction * usableOrbitWidthPx
            }

            // Build control points from 07:00 -> all scheduled planets -> 22:00
            val defaultAxisX = gutterPx + 0.35f * usableOrbitWidthPx
            val controlPoints = buildList {
                add(Offset(defaultAxisX, timeToY(startMin)))
                orbitalTasks.forEach { task ->
                    add(Offset(tierToX(task.difficultyEnum), timeToY(task.startMinutes)))
                }
                add(Offset(defaultAxisX, timeToY(endMin)))
            }

            val meteorFraction = ((uiState.currentMinutesFromMidnight - startMin).toFloat() / totalWorkingMinutes)
                .coerceIn(0f, 1f)

            // 1. Draw Y-Axis Time Gutter + Curved Predetermined Anti-UI Dot Matrix Path + Red Dot Matrix Meteor Trail
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Vertical structural axis hairline separating time labels from orbital space
                drawLine(
                    color = palette.hairlineBorder,
                    start = Offset(gutterPx - 4.dp.toPx(), topPadPx),
                    end = Offset(gutterPx - 4.dp.toPx(), topPadPx + usableHeightPx),
                    strokeWidth = 1.dp.toPx()
                )

                // Subtle vertical guide hairlines for C, B, A, S difficulty orbits
                DifficultyTier.entries.forEach { tier ->
                    val tx = tierToX(tier)
                    drawLine(
                        color = palette.hairlineBorder.copy(alpha = 0.45f),
                        start = Offset(tx, topPadPx),
                        end = Offset(tx, topPadPx + usableHeightPx),
                        strokeWidth = 0.75.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
                    )
                }

                // Construct smooth cubic Bezier path through all control points
                val fullOrbitPath = Path().apply {
                    val first = controlPoints.first()
                    moveTo(first.x, first.y)
                    for (i in 0 until controlPoints.size - 1) {
                        val p0 = controlPoints[i]
                        val p1 = controlPoints[i + 1]
                        val midY = (p0.y + p1.y) * 0.5f
                        cubicTo(
                            p0.x, midY,
                            p1.x, midY,
                            p1.x, p1.y
                        )
                    }
                }

                // Sample points evenly along the spline to render authentic dot-matrix trails!
                val pathMeasure = PathMeasure()
                pathMeasure.setPath(fullOrbitPath, false)
                val totalLength = pathMeasure.length
                val dotSpacingPx = 6.5.dp.toPx()
                val totalSamples = (totalLength / dotSpacingPx).toInt().coerceAtLeast(2)

                var meteorPos = Offset(defaultAxisX, topPadPx + meteorFraction * usableHeightPx)
                val meteorTargetY = topPadPx + meteorFraction * usableHeightPx

                for (s in 0..totalSamples) {
                    val dist = (s.toFloat() / totalSamples.toFloat()) * totalLength
                    val pos = pathMeasure.getPosition(dist)
                    val isPassedByMeteor = pos.y <= meteorTargetY

                    if (isPassedByMeteor) {
                        meteorPos = pos
                        // RED DOT MATRIX TRAIL left behind by the meteor!
                        drawCircle(
                            color = palette.nothingRed,
                            radius = 2.1.dp.toPx(),
                            center = pos
                        )
                    } else {
                        // PREDETERMINED PATH ahead of the meteor in anti-UI color matrix (White on Dark, Black on Light)
                        drawCircle(
                            color = palette.antiUiInk.copy(alpha = 0.55f),
                            radius = 1.45.dp.toPx(),
                            center = pos
                        )
                    }
                }

                // Draw glowing Dot-Matrix Meteor Head at current working-day time
                val headRadius = 5.5.dp.toPx() * meteorPulse
                drawCircle(
                    color = palette.nothingRed.copy(alpha = 0.28f),
                    radius = headRadius * 1.9f,
                    center = meteorPos
                )
                // Cross-shaped dot-matrix meteor core
                val coreStep = 3.2.dp.toPx()
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        if (dx == 0 || dy == 0) {
                            drawCircle(
                                color = if (dx == 0 && dy == 0) Color.White else palette.nothingRed,
                                radius = 1.9.dp.toPx(),
                                center = Offset(meteorPos.x + dx * coreStep, meteorPos.y + dy * coreStep)
                            )
                        }
                    }
                }
            }

            // 2. OCD-Aligned Tabular Time Labels along Y-Axis (07:00 to 22:00 every 2 hours)
            val labeledHours = listOf(7, 9, 11, 13, 15, 17, 19, 21, 22)
            labeledHours.forEach { hr ->
                val yPx = timeToY(hr * 60)
                val yDp = with(density) { yPx.toDp() } - 6.dp
                Text(
                    text = "%02d⁰⁰".format(hr),
                    fontFamily = TechnicalMonoFontFamily,
                    fontSize = 9.sp,
                    color = palette.mutedInk,
                    modifier = Modifier.offset(x = 2.dp, y = yDp)
                )
            }

            // 3. Interactive Procedural Pixel Alien Planets along the Meteor Path
            orbitalTasks.forEach { task ->
                val cxPx = tierToX(task.difficultyEnum)
                val cyPx = timeToY(task.startMinutes)
                val planetSizeDp = when (task.difficultyEnum) {
                    DifficultyTier.C -> 34.dp
                    DifficultyTier.B -> 40.dp
                    DifficultyTier.A -> 46.dp
                    DifficultyTier.S -> 52.dp
                }
                val halfSizePx = with(density) { (planetSizeDp / 2).toPx() }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (cxPx - halfSizePx).roundToInt(),
                                y = (cyPx - halfSizePx).roundToInt()
                            )
                        }
                        .minimumInteractiveComponentSize()
                        .size(planetSizeDp)
                        .testTag("planet_node_${task.id}")
                        .clickable { onPlanetTap(task) },
                    contentAlignment = Alignment.Center
                ) {
                    ProceduralPlanetCanvas(
                        seed = task.planetSeed,
                        category = task.categoryEnum,
                        difficulty = task.difficultyEnum,
                        isRotating = task.isCompleted,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
