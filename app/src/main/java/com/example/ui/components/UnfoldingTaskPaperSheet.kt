package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DifficultyTier
import com.example.data.TaskCategory
import com.example.data.TimeFormatUtils
import com.example.ui.TaskDraftState
import com.example.ui.VernierCapsuleMode
import com.example.ui.theme.GeometricSansFontFamily
import com.example.ui.theme.LocalNothingPalette
import com.example.ui.theme.NdDotMatrixFontFamily
import com.example.ui.theme.NothingRed
import com.example.ui.theme.PixelBlockFontFamily
import com.example.ui.theme.TechnicalMonoFontFamily
import com.example.ui.theme.VernierElectricBlue
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Unfolding Perforated Red Paper Task Creation Sheet:
 * - Unfolds from the top-left perforated cross button with iOS / PixelOS container-transform spring physics
 *   while opening 4 origami corner flaps to reveal the bright Nothing Red paper interior.
 * - Includes:
 *   1) Top Circle-to-Bar spring "ADD TASK" input with per-character onboarding bounce & keyboard auto-open
 *   2) Top-right minimization fold button reversing the animation back into the cross
 *   3) Tiny "TIM" and "DUR" dot-matrix capsules with high-fps color-spread animation
 *   4) @uix.vikram Dual-Track Vernier Slide-Rule Scrubber for task time & duration
 *   5) 10-Category Pixel-Art Tag Vernier Scrubber
 *   6) C-to-S Tier iOS Spring Difficulty Slider (no glassmorphism)
 */
@Composable
fun UnfoldingTaskPaperOverlay(
    isOpen: Boolean,
    draft: TaskDraftState,
    vernierOffsetHours: Int,
    hapticsEnabled: Boolean,
    onMinimizeClose: () -> Unit,
    onExpandTitleBar: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onSelectCapsule: (VernierCapsuleMode) -> Unit,
    onUpdateStartMinutes: (Int) -> Unit,
    onUpdateDurationMinutes: (Int) -> Unit,
    onCycleVernierOffset: () -> Unit,
    onSelectCategory: (TaskCategory) -> Unit,
    onSelectDifficulty: (DifficultyTier) -> Unit,
    onCommitTask: () -> Unit
) {
    val unfoldProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 340f
        ),
        label = "paper_unfold_spring"
    )

    if (unfoldProgress <= 0.01f && !isOpen) return

    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f * unfoldProgress))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* consume backdrop taps */ }
            )
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("unfolded_task_paper_overlay"),
        contentAlignment = Alignment.TopStart
    ) {
        // PixelOS / iOS app-open style container interpolation from top-left button origin
        val scaleProgress = (0.16f + 0.84f * unfoldProgress).coerceIn(0.16f, 1f)
        val flapFoldProgress = ((1f - unfoldProgress) * 1.4f).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scaleProgress
                    scaleY = scaleProgress
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.28f, 0.08f)
                    alpha = (unfoldProgress * 1.35f).coerceIn(0f, 1f)
                }
        ) {
            // 1. Bright Nothing Red Paper Substrate with Coil-Diary Perforated Edge Notches (Dents)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val paperRect = Rect(0f, 0f, w, h)
                val basePath = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = paperRect,
                            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                        )
                    )
                }

                // Coil-diary perforated semicircular notches along all 4 sides
                val notches = Path()
                val topCount = 16
                val sideCount = 26
                val notchRad = 3.5.dp.toPx()

                for (i in 1 until topCount) {
                    val nx = (w / topCount) * i
                    notches.addOval(Rect(Offset(nx, 0f), notchRad))
                    notches.addOval(Rect(Offset(nx, h), notchRad))
                }
                for (j in 1 until sideCount) {
                    val ny = (h / sideCount) * j
                    notches.addOval(Rect(Offset(0f, ny), notchRad))
                    notches.addOval(Rect(Offset(w, ny), notchRad))
                }

                val perforatedSheet = Path.combine(PathOperation.Difference, basePath, notches)
                drawPath(path = perforatedSheet, color = NothingRed)
                drawPath(
                    path = perforatedSheet,
                    color = Color.White.copy(alpha = 0.32f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // 2. Scrollable Unfolded Red Paper Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TOP ROW: Circle-to-Bar Expanding "ADD TASK" + Top-Right Minimize Fold Button
                TopAddTaskSpringHeader(
                    title = draft.title,
                    isExpanded = draft.isTitleBarExpanded,
                    hapticsEnabled = hapticsEnabled,
                    onExpand = onExpandTitleBar,
                    onTitleChange = onUpdateTitle,
                    onMinimizeClose = {
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onMinimizeClose()
                    }
                )

                // SECOND ROW: Tiny "TIM" and "DUR" Dot-Matrix Capsules side by side
                TimDurCapsuleRow(
                    activeMode = draft.activeCapsule,
                    startMinutes = draft.startMinutes,
                    durationMinutes = draft.durationMinutes,
                    hapticsEnabled = hapticsEnabled,
                    onSelectMode = onSelectCapsule
                )

                // THIRD ROW: @uix.vikram Dual-Track Vernier Slide-Rule Scrubber (Time & Duration)
                VikramVernierTimeScrubberCard(
                    activeMode = draft.activeCapsule,
                    startMinutes = draft.startMinutes,
                    durationMinutes = draft.durationMinutes,
                    offsetHours = vernierOffsetHours,
                    hapticsEnabled = hapticsEnabled,
                    onUpdateStartMinutes = onUpdateStartMinutes,
                    onUpdateDurationMinutes = onUpdateDurationMinutes,
                    onCycleOffset = onCycleVernierOffset
                )

                // FOURTH ROW: 10-Category Pixel-Art Tag Vernier Scrubber
                PixelTagVernierScrubberCard(
                    selectedCategory = draft.selectedCategory,
                    hapticsEnabled = hapticsEnabled,
                    onSelectCategory = onSelectCategory
                )

                // FIFTH ROW: C-to-S Tier iOS Spring Difficulty Slider (Zero Glassmorphism)
                DifficultySpringSliderCard(
                    selectedDifficulty = draft.selectedDifficulty,
                    selectedCategory = draft.selectedCategory,
                    hapticsEnabled = hapticsEnabled,
                    onSelectDifficulty = onSelectDifficulty
                )

                // FINAL ACTION: Deploy Task Planet to Working Day Orbit
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.backgroundCanvas)
                        .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                        .clickable {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onCommitTask()
                        }
                        .padding(vertical = 12.dp)
                        .testTag("deploy_task_planet_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ LOCK ORBIT & GENERATE PLANET",
                        fontFamily = NdDotMatrixFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color.White
                    )
                }
            }

            // 3. Origami 4-Corner Flaps Unfolding / Folding Animation Layer
            if (flapFoldProgress > 0.02f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w * 0.5f * flapFoldProgress
                    val cy = h * 0.5f * flapFoldProgress
                    val flapColor = palette.backgroundCanvas.copy(alpha = flapFoldProgress)

                    // Top-Left flap
                    drawRect(color = flapColor, topLeft = Offset(0f, 0f), size = Size(cx, cy))
                    // Top-Right flap
                    drawRect(color = flapColor, topLeft = Offset(w - cx, 0f), size = Size(cx, cy))
                    // Bottom-Left flap
                    drawRect(color = flapColor, topLeft = Offset(0f, h - cy), size = Size(cx, cy))
                    // Bottom-Right flap
                    drawRect(color = flapColor, topLeft = Offset(w - cx, h - cy), size = Size(cx, cy))
                }
            }
        }
    }
}

/**
 * Top Circle-to-Bar Expanding "ADD TASK" Input + Minimize Button:
 * - Tapping the circle springs it into a full-width horizontal bar with iOS spring physics.
 * - Each character of "ADD TASK" is sequentially pushed/bounced and onboarded onto the bar
 *   as the expanding bar sweeps past its position, then the keyboard opens immediately.
 */
@Composable
private fun TopAddTaskSpringHeader(
    title: String,
    isExpanded: Boolean,
    hapticsEnabled: Boolean,
    onExpand: () -> Unit,
    onTitleChange: (String) -> Unit,
    onMinimizeClose: () -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.66f,
            stiffness = 430f
        ),
        label = "circle_to_bar_spring"
    )

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Expanding Circle-to-Bar Container
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isExpanded) {
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onExpand()
                    }
                }
                .testTag("expanding_add_task_bar_trigger")
        ) {
            val fullWidthDp = maxWidth
            val circleSizeDp = 44.dp
            val currentBarWidthDp = circleSizeDp + (fullWidthDp - circleSizeDp) * expansionProgress.coerceIn(0f, 1f)

            // The expanding bar growing from a circle on the left into a full stadium bar
            Box(
                modifier = Modifier
                    .width(currentBarWidthDp)
                    .height(44.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.backgroundCanvas)
                    .border(1.5.dp, Color.White, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                // Plus glyph inside the initial circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontFamily = NdDotMatrixFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed
                    )
                }
            }

            // "ADD TASK" characters that stay in their horizontal position and get pushed/onboarded
            // onto the bar as the bar's leading edge sweeps across them!
            val promptChars = "ADD TASK"
            if (title.isEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 54.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    promptChars.forEachIndexed { index, ch ->
                        val charThreshold = (index + 1).toFloat() / (promptChars.length + 1).toFloat()
                        val isOnboarded = expansionProgress >= charThreshold
                        val distance = (expansionProgress - charThreshold)
                        // Snappy wave bounce as the bar sweeps under each character
                        val waveBounceDp = if (distance in -0.12f..0.22f) {
                            (-7f * (1f - abs(distance - 0.05f) / 0.17f).coerceIn(0f, 1f)).dp
                        } else {
                            0.dp
                        }
                        val charScale = if (distance in -0.10f..0.20f) 1.22f else 1.0f

                        Text(
                            text = ch.toString(),
                            fontFamily = NdDotMatrixFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnboarded) {
                                palette.primaryInk
                            } else {
                                Color.White.copy(alpha = 0.92f)
                            },
                            modifier = Modifier
                                .offset(y = waveBounceDp)
                                .scale(charScale)
                        )
                    }
                }
            }

            // Live Keyboard Input Field inside the expanded bar
            if (isExpanded) {
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = NdDotMatrixFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    ),
                    cursorBrush = SolidColor(NothingRed),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart)
                        .padding(start = 54.dp, end = 14.dp)
                        .focusRequester(focusRequester)
                        .testTag("add_task_text_input")
                )
            }
        }

        // Top-Right Minimization Button (folds the red paper back into the cross button)
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.backgroundCanvas)
                .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                .clickable(onClick = onMinimizeClose)
                .testTag("minimize_fold_paper_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val stroke = 2.dp.toPx()
                // Inward folding arrows / minimize cross icon
                drawLine(
                    color = Color.White,
                    start = Offset(2f, size.height * 0.5f),
                    end = Offset(size.width - 2f, size.height * 0.5f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = NothingRed,
                    radius = 2.5.dp.toPx(),
                    center = Offset(size.width * 0.5f, size.height * 0.5f)
                )
            }
        }
    }
}

/**
 * Two tiny capsules side by side ("TIM" and "DUR") in bold dot-matrix font.
 * Features a subtle high-fps touch-origin radial color-spreading animation.
 */
@Composable
private fun TimDurCapsuleRow(
    activeMode: VernierCapsuleMode,
    startMinutes: Int,
    durationMinutes: Int,
    hapticsEnabled: Boolean,
    onSelectMode: (VernierCapsuleMode) -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current

    val timSpread by animateFloatAsState(
        targetValue = if (activeMode == VernierCapsuleMode.TIME) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "tim_spread"
    )
    val durSpread by animateFloatAsState(
        targetValue = if (activeMode == VernierCapsuleMode.DURATION) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "dur_spread"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Capsule 1: "TIM"
            SpreadingDotMatrixCapsule(
                label = "TIM",
                spreadProgress = timSpread,
                activeFillColor = palette.backgroundCanvas,
                activeTextColor = palette.primaryInk,
                inactiveTextColor = Color.White,
                testTagName = "capsule_tim",
                onClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onSelectMode(VernierCapsuleMode.TIME)
                }
            )

            // Capsule 2: "DUR"
            SpreadingDotMatrixCapsule(
                label = "DUR",
                spreadProgress = durSpread,
                activeFillColor = palette.backgroundCanvas,
                activeTextColor = palette.primaryInk,
                inactiveTextColor = Color.White,
                testTagName = "capsule_dur",
                onClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onSelectMode(VernierCapsuleMode.DURATION)
                }
            )
        }

        // Live summary badge of selected TIM + DUR
        Text(
            text = "${TimeFormatUtils.format24HourColon(startMinutes)} · ${durationMinutes}M",
            fontFamily = NdDotMatrixFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SpreadingDotMatrixCapsule(
    label: String,
    spreadProgress: Float,
    activeFillColor: Color,
    activeTextColor: Color,
    inactiveTextColor: Color,
    testTagName: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .width(66.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.2.dp, Color.White, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .testTag(testTagName),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (spreadProgress > 0.01f) {
                val maxRadius = size.width * 0.85f
                drawCircle(
                    color = activeFillColor,
                    radius = maxRadius * spreadProgress,
                    center = Offset(size.width * 0.5f, size.height * 0.5f)
                )
            }
        }
        Text(
            text = label,
            fontFamily = NdDotMatrixFontFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = if (spreadProgress > 0.5f) activeTextColor else inactiveTextColor
        )
    }
}

/**
 * @uix.vikram World Clock & Time-Zone Dual-Track Vernier Scrubber Adapted for Task Time & Duration:
 * 1. Oversized background CLOCK watermark overlapped by an electric-blue GMT +3 / DUR pill and 12:41 PM digital readout.
 * 2. Parallel Top and Bottom hour tracks flanking a precision tick-mark ruler (tall 36px major ticks + 4 minor 12px ticks per hour)
 *    and a centered vertical Viewfinder Loupe (14dp radius) with a 3dp rounded electric-blue vertical indicator needle.
 * 3. High-contrast 09:41 PM Local Time anchor below the scrubber.
 * 4. Circular blue-duotone portrait avatar paired with @uix.vikram along the bottom margin.
 */
@Composable
private fun VikramVernierTimeScrubberCard(
    activeMode: VernierCapsuleMode,
    startMinutes: Int,
    durationMinutes: Int,
    offsetHours: Int,
    hapticsEnabled: Boolean,
    onUpdateStartMinutes: (Int) -> Unit,
    onUpdateDurationMinutes: (Int) -> Unit,
    onCycleOffset: () -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val activeHour = (startMinutes / 60).coerceIn(7, 22)
    val activeMinuteOffset = startMinutes % 60
    // Preserve :41 motif or exact selected minutes
    val displayMinute = if (activeMinuteOffset == 0) 41 else activeMinuteOffset
    val targetReadoutMinutes = ((activeHour + offsetHours) * 60 + displayMinute)
    val localReadoutMinutes = (activeHour * 60 + displayMinute)

    val durationSteps = listOf(15, 30, 45, 60, 75, 90, 105, 120, 150, 180, 210, 240)
    val activeDurationIdx = durationSteps.indexOfFirst { it >= durationMinutes }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.backgroundCanvas)
            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("vikram_vernier_time_scrubber"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TIER 1: Ambient Header & Target Zone (Oversized CLOCK watermark + overlapping GMT +3 pill + 12:41 PM readout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Oversized low-contrast watermark wordmark (CLOCK)
            Text(
                text = if (activeMode == VernierCapsuleMode.TIME) "CLOCK" else "SCALE",
                fontFamily = GeometricSansFontFamily,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                color = palette.watermarkText,
                modifier = Modifier.offset(y = (-6).dp)
            )

            // Overlapping Time-Zone / Mode Selector Pill (GMT +3)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(VernierElectricBlue)
                        .clickable { onCycleOffset() }
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                        .testTag("vernier_gmt_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    val sign = if (offsetHours >= 0) "+" else ""
                    Text(
                        text = if (activeMode == VernierCapsuleMode.TIME) {
                            "GMT $sign$offsetHours"
                        } else {
                            "DUR +${durationMinutes}M"
                        },
                        fontFamily = TechnicalMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Target Digital Readout (e.g., 12:41 PM) in electric blue
                Text(
                    text = if (activeMode == VernierCapsuleMode.TIME) {
                        TimeFormatUtils.format12HourAmPm(targetReadoutMinutes)
                    } else {
                        "END ${TimeFormatUtils.format12HourAmPm(startMinutes + durationMinutes)}"
                    },
                    fontFamily = TechnicalMonoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VernierElectricBlue
                )
            }
        }

        // TIER 2: Core Interactive Dual-Track Vernier Ruler with Centered Viewfinder Loupe & Blue Needle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .pointerInput(activeMode, startMinutes, durationMinutes) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount
                            val thresholdPx = 28f
                            if (abs(dragAccumulator) >= thresholdPx) {
                                val steps = (dragAccumulator / thresholdPx).toInt()
                                dragAccumulator -= steps * thresholdPx
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                if (activeMode == VernierCapsuleMode.TIME) {
                                    // Pulling ruler right moves to earlier hour; pulling left advances hour
                                    val newHour = (activeHour - steps).coerceIn(7, 21)
                                    onUpdateStartMinutes(newHour * 60 + (startMinutes % 60))
                                } else {
                                    val nextIdx = (activeDurationIdx - steps).coerceIn(0, durationSteps.lastIndex)
                                    onUpdateDurationMinutes(durationSteps[nextIdx])
                                }
                            }
                        }
                    )
                }
                .testTag("vernier_dual_track_ruler"),
            contentAlignment = Alignment.Center
        ) {
            // Centered Viewfinder Loupe Reticle (border-radius: 14.dp)
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(108.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.loupeContainer)
                    .border(1.dp, palette.hairlineBorder, RoundedCornerShape(14.dp))
            )

            // 7 Visible Columns (-3..+3 around active column)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (relCol in -3..3) {
                    val distance = abs(relCol)
                    // Cylindrical drum edge-fade opacity mask
                    val colAlpha = when (distance) {
                        0 -> 1.0f
                        1 -> 0.85f
                        2 -> 0.52f
                        else -> 0.20f
                    }

                    val topLabel: String
                    val bottomLabel: String
                    if (activeMode == VernierCapsuleMode.TIME) {
                        val localHr = (activeHour + relCol).coerceIn(0, 23)
                        val targetHr = ((localHr + offsetHours) % 24 + 24) % 24
                        topLabel = "%02d".format(targetHr)
                        bottomLabel = "%02d".format(localHr)
                    } else {
                        val stepIdx = (activeDurationIdx + relCol).coerceIn(0, durationSteps.lastIndex)
                        val durVal = durationSteps[stepIdx]
                        val endHr = ((startMinutes + durVal) / 60) % 24
                        topLabel = "${durVal}m"
                        bottomLabel = "%02d".format(endHr)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .alpha(colAlpha)
                            .clickable {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                if (activeMode == VernierCapsuleMode.TIME) {
                                    val pickedHr = (activeHour + relCol).coerceIn(7, 21)
                                    onUpdateStartMinutes(pickedHr * 60)
                                } else {
                                    val pickedIdx = (activeDurationIdx + relCol).coerceIn(0, durationSteps.lastIndex)
                                    onUpdateDurationMinutes(durationSteps[pickedIdx])
                                }
                            }
                    ) {
                        // Top Hour Track (GMT +3 / Target Scale)
                        Text(
                            text = topLabel,
                            fontFamily = TechnicalMonoFontFamily,
                            fontSize = if (relCol == 0) 14.sp else 12.sp,
                            fontWeight = if (relCol == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (relCol == 0) VernierElectricBlue else palette.mutedInk
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Center Tick-Mark Ruler (Major 36px tick + 4 minor 12px ticks between hours)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // Major Tick (transforms into 3dp electric-blue needle inside center loupe)
                            Canvas(
                                modifier = Modifier
                                    .width(if (relCol == 0) 3.dp else 1.5.dp)
                                    .height(34.dp)
                            ) {
                                drawLine(
                                    color = if (relCol == 0) VernierElectricBlue else palette.mutedInk,
                                    start = Offset(size.width * 0.5f, 0f),
                                    end = Offset(size.width * 0.5f, size.height),
                                    strokeWidth = size.width,
                                    cap = StrokeCap.Round
                                )
                            }

                            // 4 Minor Ticks dividing each hour into 5 equal 12-minute segments
                            if (relCol < 3) {
                                for (minor in 1..4) {
                                    Canvas(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(12.dp)
                                    ) {
                                        drawLine(
                                            color = palette.mutedInk.copy(alpha = 0.55f),
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = 1.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom Hour Track (Local Time Scale)
                        Text(
                            text = bottomLabel,
                            fontFamily = TechnicalMonoFontFamily,
                            fontSize = if (relCol == 0) 14.sp else 12.sp,
                            fontWeight = if (relCol == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (relCol == 0) palette.activeLocalHourInk else palette.mutedInk
                        )
                    }
                }
            }
        }

        // TIER 3 & 4: Bottom Local Time Readout (09:41 PM + Local Time) & Creator Attribution (@uix.vikram)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quarter-hour fine tune pills (:00, :15, :30, :45)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0, 15, 30, 45).forEach { quarter ->
                    val isSelectedQuarter = (startMinutes % 60) == quarter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelectedQuarter) VernierElectricBlue else palette.surfaceElevated)
                            .clickable {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onUpdateStartMinutes(activeHour * 60 + quarter)
                            }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ":%02d".format(quarter),
                            fontFamily = TechnicalMonoFontFamily,
                            fontSize = 9.sp,
                            color = if (isSelectedQuarter) Color.White else palette.mutedInk
                        )
                    }
                }
            }

            // Local Time Anchor
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = TimeFormatUtils.format12HourAmPm(localReadoutMinutes),
                    fontFamily = TechnicalMonoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk
                )
                Text(
                    text = "Local Time",
                    fontFamily = GeometricSansFontFamily,
                    fontSize = 9.sp,
                    color = palette.mutedInk
                )
            }

            // Creator Attribution: Circular blue-duotone portrait avatar + @uix.vikram
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.img_creator_avatar_1794567783841),
                    contentDescription = "Creator duotone portrait avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(1.dp, VernierElectricBlue, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "@uix.vikram",
                    fontFamily = TechnicalMonoFontFamily,
                    fontSize = 9.sp,
                    color = palette.mutedInk
                )
            }
        }
    }
}

/**
 * Second Vernier Slide-Rule Scrubber for picking one of the 10 Pixel-Art Task Categories.
 */
@Composable
private fun PixelTagVernierScrubberCard(
    selectedCategory: TaskCategory,
    hapticsEnabled: Boolean,
    onSelectCategory: (TaskCategory) -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    val categories = TaskCategory.entries
    val activeIdx = categories.indexOf(selectedCategory).coerceAtLeast(0)
    var dragAccum by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.backgroundCanvas)
            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .pointerInput(activeIdx) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccum += dragAmount
                        val stepPx = 32f
                        if (abs(dragAccum) >= stepPx) {
                            val delta = (dragAccum / stepPx).toInt()
                            dragAccum -= delta * stepPx
                            val nextIdx = ((activeIdx - delta) % categories.size + categories.size) % categories.size
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSelectCategory(categories[nextIdx])
                        }
                    }
                )
            }
            .testTag("pixel_tag_vernier_scrubber"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TAG MATRIX // 10 CATEGORIES",
                fontFamily = PixelBlockFontFamily,
                fontSize = 9.sp,
                color = palette.mutedInk
            )
            Text(
                text = "${selectedCategory.label} [${selectedCategory.shortCode}]",
                fontFamily = NdDotMatrixFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = selectedCategory.timelineColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Vernier Horizontal Drum of 5 visible categories centered on active category inside Loupe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // Centered Viewfinder Loupe
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(62.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.loupeContainer)
                    .border(1.5.dp, selectedCategory.timelineColor, RoundedCornerShape(14.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (offset in -2..2) {
                    val idx = ((activeIdx + offset) % categories.size + categories.size) % categories.size
                    val cat = categories[idx]
                    val isCenter = offset == 0
                    val itemAlpha = when (abs(offset)) {
                        0 -> 1f
                        1 -> 0.72f
                        else -> 0.35f
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .alpha(itemAlpha)
                            .clickable {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onSelectCategory(cat)
                            }
                            .padding(4.dp)
                            .testTag("tag_option_${cat.code}")
                    ) {
                        PixelArtGlyphCanvas(
                            glyphKey = cat.code,
                            tint = if (isCenter) cat.timelineColor else palette.primaryInk,
                            modifier = Modifier.size(if (isCenter) 24.dp else 18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cat.shortCode,
                            fontFamily = TechnicalMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCenter) cat.timelineColor else palette.mutedInk
                        )
                    }
                }
            }
        }
    }
}

/**
 * C-to-S Tier iOS Spring Difficulty Slider (solid geometric primitive, no glassmorphism):
 * - 4 discrete options on the line: C, B, A, S
 * - Spring motion when picked + live preview of the procedural pixel alien planet & its X-axis orbit offset.
 */
@Composable
private fun DifficultySpringSliderCard(
    selectedDifficulty: DifficultyTier,
    selectedCategory: TaskCategory,
    hapticsEnabled: Boolean,
    onSelectDifficulty: (DifficultyTier) -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    val tiers = DifficultyTier.entries
    val selectedIndex = tiers.indexOf(selectedDifficulty).coerceAtLeast(0)

    val springFraction by animateFloatAsState(
        targetValue = selectedIndex.toFloat() / (tiers.size - 1).toFloat(),
        animationSpec = spring(
            dampingRatio = 0.58f, // Snappy iOS spring bounce
            stiffness = 410f
        ),
        label = "difficulty_ios_spring_slider"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.backgroundCanvas)
            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("difficulty_spring_slider_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DIFFICULTY TIER // X-AXIS ORBIT",
                    fontFamily = PixelBlockFontFamily,
                    fontSize = 9.sp,
                    color = palette.mutedInk
                )
                Text(
                    text = selectedDifficulty.title,
                    fontFamily = NdDotMatrixFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedDifficulty == DifficultyTier.S) NothingRed else palette.primaryInk
                )
            }

            // Live Procedural Pixel Planet Preview for the selected Tag + Difficulty
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "X:${(selectedDifficulty.orbitXFraction * 100).toInt()}%",
                    fontFamily = TechnicalMonoFontFamily,
                    fontSize = 10.sp,
                    color = palette.mutedInk
                )
                Spacer(modifier = Modifier.width(6.dp))
                ProceduralPlanetCanvas(
                    seed = 404 + selectedCategory.ordinal * 19 + selectedDifficulty.ordinal * 37,
                    category = selectedCategory,
                    difficulty = selectedDifficulty,
                    isRotating = true,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // Solid Geometric Spring Slider Track (4 discrete nodes: C, B, A, S)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            val density = LocalDensity.current
            val trackWidthPx = with(density) { maxWidth.toPx() }
            val thumbWidthDp = 64.dp
            val thumbWidthPx = with(density) { thumbWidthDp.toPx() }
            val travelPx = (trackWidthPx - thumbWidthPx).coerceAtLeast(0f)

            // Horizontal solid line connecting C - B - A - S
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
            ) {
                drawLine(
                    color = palette.hairlineBorder,
                    start = Offset(24.dp.toPx(), size.height * 0.5f),
                    end = Offset(size.width - 24.dp.toPx(), size.height * 0.5f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Spring-animated active thumb capsule (solid high-contrast, zero glassmorphism)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(x = (springFraction * travelPx).roundToInt(), y = 0)
                    }
                    .width(thumbWidthDp)
                    .height(34.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedDifficulty == DifficultyTier.S) NothingRed else palette.primaryInk)
            )

            // 4 Tap Targets along the line (C, B, A, S)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tiers.forEach { tier ->
                    val isSelected = tier == selectedDifficulty
                    Box(
                        modifier = Modifier
                            .width(thumbWidthDp)
                            .height(36.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onSelectDifficulty(tier)
                            }
                            .testTag("difficulty_tier_${tier.code}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${tier.code}-TIER",
                            fontFamily = NdDotMatrixFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected && tier == DifficultyTier.S -> Color.White
                                isSelected -> palette.backgroundCanvas
                                else -> palette.mutedInk
                            }
                        )
                    }
                }
            }
        }
    }
}
