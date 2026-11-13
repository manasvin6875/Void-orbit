package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PinnedReminderTile
import com.example.data.TaskEntity
import com.example.data.TimeFormatUtils
import com.example.ui.CalendarSubView
import com.example.ui.OrbitUiState
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GeometricSansFontFamily
import com.example.ui.theme.LocalNothingPalette
import com.example.ui.theme.NdDotMatrixFontFamily
import com.example.ui.theme.PixelBlockFontFamily
import com.example.ui.theme.TechnicalMonoFontFamily
import com.example.ui.theme.TimelineCerulean
import com.example.ui.theme.TimelineCoral
import com.example.ui.theme.TimelineLeafGreen
import com.example.ui.theme.TimelineWarmAmber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Nothing-Spun Structured Multi-Scale Calendar Screen:
 * - Supports WEEK (7-day Beads-on-a-String swimlanes), TODAY (Expanded Single-Day Agenda with smooth Red -> Yellow morph),
 *   MONTH, YEAR, and SETTINGS.
 * - Includes Long-Press Vertical Drag on tasks in both WEEK and TODAY views to reschedule task times globally across the app.
 */
@Composable
fun StructuredCalendarScreen(
    uiState: OrbitUiState,
    onSelectSubView: (CalendarSubView) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onTaskTap: (TaskEntity) -> Unit,
    onToggleTaskComplete: (TaskEntity) -> Unit,
    onRescheduleTask: (TaskEntity, Int) -> Unit,
    onOpenTaskPaperWithSlot: (Int?, Int?) -> Unit,
    onToggleReminder: (String) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetSimulatedTime: (Boolean, Int) -> Unit,
    onResetDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current

    // Smooth color transition from Nothing Red (in Weekly view) to Nothing Yellow (in Today view)!
    val activeAccentColor by animateColorAsState(
        targetValue = when (uiState.calendarSubView) {
            CalendarSubView.TODAY -> palette.nothingYellow
            else -> palette.nothingRed
        },
        animationSpec = tween(durationMillis = 480),
        label = "red_to_yellow_accent_transition"
    )

    val mondayOfWeek = remember(uiState.selectedDate) {
        uiState.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val weekDates = remember(mondayOfWeek) {
        (0..6).map { mondayOfWeek.plusDays(it.toLong()) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("structured_calendar_screen")
    ) {
        // 1. TOP CALENDAR HEADER: Month/Year Title + Top-Right View Switcher (WEEK, TODAY, MONTH, YEAR, SET)
        TopCalendarHeaderBar(
            selectedDate = uiState.selectedDate,
            activeSubView = uiState.calendarSubView,
            activeAccentColor = activeAccentColor,
            hapticsEnabled = uiState.settings.hapticsEnabled,
            onSelectSubView = onSelectSubView
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2. 7-DAY HORIZONTAL WEEK STRIP + MICRO-BADGE / CYCLE PHASE SUB-HEADER
        if (uiState.calendarSubView == CalendarSubView.WEEK || uiState.calendarSubView == CalendarSubView.TODAY) {
            WeekStripAndPhaseHeader(
                weekDates = weekDates,
                selectedDate = uiState.selectedDate,
                allTasks = uiState.allTasks,
                isExpandedTodayMode = uiState.calendarSubView == CalendarSubView.TODAY,
                activeAccentColor = activeAccentColor,
                onSelectDate = { picked ->
                    if (uiState.settings.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onSelectDate(picked)
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // 3. MAIN BODY WITH PIXEL-OS / SPRING TRANSITION BETWEEN SUB-VIEWS
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = uiState.calendarSubView,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(320)) +
                        scaleIn(
                            initialScale = 0.94f,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )) togetherWith
                        (fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 0.96f))
                },
                label = "calendar_subview_morph"
            ) { targetView ->
                when (targetView) {
                    CalendarSubView.WEEK -> {
                        WeeklyBeadsSwimlaneView(
                            weekDates = weekDates,
                            selectedDate = uiState.selectedDate,
                            allTasks = uiState.allTasks,
                            pinnedReminders = uiState.pinnedReminders,
                            activeAccentColor = activeAccentColor,
                            hapticsEnabled = uiState.settings.hapticsEnabled,
                            onSelectDateAndExpandToday = { date ->
                                onSelectDate(date)
                                onSelectSubView(CalendarSubView.TODAY)
                            },
                            onTaskTap = onTaskTap,
                            onRescheduleTask = onRescheduleTask,
                            onToggleReminder = onToggleReminder,
                            onPullUpDrawerToToday = {
                                onSelectSubView(CalendarSubView.TODAY)
                            }
                        )
                    }

                    CalendarSubView.TODAY -> {
                        TodayExpandedAgendaView(
                            selectedDate = uiState.selectedDate,
                            dayTasks = uiState.selectedDateTasks,
                            pinnedReminders = uiState.pinnedReminders,
                            activeAccentColor = activeAccentColor,
                            hapticsEnabled = uiState.settings.hapticsEnabled,
                            onTaskTap = onTaskTap,
                            onToggleTaskComplete = onToggleTaskComplete,
                            onRescheduleTask = onRescheduleTask,
                            onToggleReminder = onToggleReminder,
                            onFillFreeTimeGap = { gapStartMin, gapDurMin ->
                                onOpenTaskPaperWithSlot(gapStartMin, gapDurMin)
                            },
                            onCollapseToWeek = {
                                onSelectSubView(CalendarSubView.WEEK)
                            }
                        )
                    }

                    CalendarSubView.MONTH -> {
                        MonthlyMatrixCalendarView(
                            selectedDate = uiState.selectedDate,
                            allTasks = uiState.allTasks,
                            onSelectDate = { date ->
                                onSelectDate(date)
                                onSelectSubView(CalendarSubView.TODAY)
                            },
                            onChangeMonth = { deltaMonths ->
                                onSelectDate(uiState.selectedDate.plusMonths(deltaMonths.toLong()))
                            }
                        )
                    }

                    CalendarSubView.YEAR -> {
                        YearlyDotMatrixHeatmapView(
                            selectedDate = uiState.selectedDate,
                            allTasks = uiState.allTasks,
                            onSelectMonth = { monthNum ->
                                val updated = uiState.selectedDate.withMonth(monthNum)
                                onSelectDate(updated)
                                onSelectSubView(CalendarSubView.MONTH)
                            }
                        )
                    }

                    CalendarSubView.SETTINGS -> {
                        NothingCalendarSettingsView(
                            uiState = uiState,
                            onSetThemeMode = onSetThemeMode,
                            onSetHaptics = onSetHaptics,
                            onSetSimulatedTime = onSetSimulatedTime,
                            onResetDemoData = onResetDemoData
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCalendarHeaderBar(
    selectedDate: LocalDate,
    activeSubView: CalendarSubView,
    activeAccentColor: Color,
    hapticsEnabled: Boolean,
    onSelectSubView: (CalendarSubView) -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    val monthName = selectedDate.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left-aligned Month/Year Title with dimmed year hierarchy
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = monthName,
                fontFamily = GeometricSansFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = selectedDate.year.toString(),
                fontFamily = NdDotMatrixFontFamily,
                fontSize = 20.sp,
                color = palette.mutedInk
            )
        }

        // Top-right view selector pills + dynamic morphing Timeline icon indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Morphing Timeline Icon (2x3 block grid in WEEK vs single-column node-list in TODAY)
            MorphingTimelineStateGlyph(
                isSingleDayList = activeSubView == CalendarSubView.TODAY,
                tint = activeAccentColor,
                modifier = Modifier
                    .size(22.dp)
                    .padding(end = 2.dp)
            )

            val options = listOf(
                CalendarSubView.WEEK to "WK",
                CalendarSubView.TODAY to "TDY",
                CalendarSubView.MONTH to "MO",
                CalendarSubView.YEAR to "YR",
                CalendarSubView.SETTINGS to "SET"
            )
            options.forEach { (view, shortLabel) ->
                val isSelected = activeSubView == view
                val pillBg = when {
                    isSelected && view == CalendarSubView.TODAY -> palette.nothingYellow
                    isSelected -> palette.nothingRed
                    else -> palette.surfaceElevated
                }
                val pillFg = when {
                    isSelected && view == CalendarSubView.TODAY -> Color(0xFF0A0A0C)
                    isSelected -> Color.White
                    else -> palette.mutedInk
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(pillBg)
                        .border(1.dp, if (isSelected) pillBg else palette.hairlineBorder, RoundedCornerShape(6.dp))
                        .clickable {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSelectSubView(view)
                        }
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                        .testTag("calendar_subview_tab_${view.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortLabel,
                        fontFamily = PixelBlockFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = pillFg
                    )
                }
            }
        }
    }
}

/**
 * Dynamic Tab Icon Morphing between 2-column x 3-row block grid (7-day Weekly view)
 * and single-column node-and-line list icon (Today Single-Day Agenda view).
 */
@Composable
private fun MorphingTimelineStateGlyph(
    isSingleDayList: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val morphProgress by animateFloatAsState(
        targetValue = if (isSingleDayList) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "timeline_glyph_morph"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (morphProgress < 0.5f) {
            // 2-column x 3-row block grid
            val cellW = w * 0.34f
            val cellH = h * 0.22f
            for (r in 0..2) {
                for (c in 0..1) {
                    drawRect(
                        color = tint,
                        topLeft = Offset(
                            x = c * (cellW + w * 0.16f) + w * 0.08f,
                            y = r * (cellH + h * 0.10f) + h * 0.06f
                        ),
                        size = Size(cellW, cellH)
                    )
                }
            }
        } else {
            // Single-column node-and-line list icon
            val nodeX = w * 0.24f
            drawLine(
                color = tint,
                start = Offset(nodeX, h * 0.15f),
                end = Offset(nodeX, h * 0.85f),
                strokeWidth = 1.5.dp.toPx()
            )
            for (r in 0..2) {
                val cy = h * (0.20f + r * 0.30f)
                drawCircle(color = tint, radius = 2.4.dp.toPx(), center = Offset(nodeX, cy))
                drawLine(
                    color = tint,
                    start = Offset(w * 0.44f, cy),
                    end = Offset(w * 0.90f, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun WeekStripAndPhaseHeader(
    weekDates: List<LocalDate>,
    selectedDate: LocalDate,
    allTasks: List<TaskEntity>,
    isExpandedTodayMode: Boolean,
    activeAccentColor: Color,
    onSelectDate: (LocalDate) -> Unit
) {
    val palette = LocalNothingPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(10.dp))
            .background(palette.backgroundCanvas.copy(alpha = 0.88f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        // 7-Day Horizontal Week Strip (Mon through Sun)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDates.forEachIndexed { idx, date ->
                val isSelected = date == selectedDate
                val dayTasks = allTasks.filter { it.dateIso == date.toString() }
                val dayLabel = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectDate(date) }
                        .testTag("week_strip_day_$idx")
                ) {
                    Text(
                        text = dayLabel,
                        fontFamily = TechnicalMonoFontFamily,
                        fontSize = 10.sp,
                        color = if (isSelected) activeAccentColor else palette.mutedInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Active Day Selection wrapped in solid Coral/Red (Week) or Nothing Yellow (Today) circle
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) activeAccentColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontFamily = GeometricSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected && isExpandedTodayMode -> Color(0xFF0A0A0C)
                                isSelected -> Color.White
                                else -> palette.primaryInk
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Daily Micro-Badge Row / Overlapping Task-Density Micro-Badges
                    if (isExpandedTodayMode) {
                        // Tightly overlapped horizontal cluster of 3-5 colored discs with dark cut-out borders
                        val discColors = dayTasks.take(5).map { it.categoryEnum.timelineColor }
                            .ifEmpty { listOf(TimelineCoral, TimelineCerulean, TimelineLeafGreen) }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-3).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            discColors.forEachIndexed { dIdx, col ->
                                val isCenterAnchor = dIdx == discColors.size / 2 && idx >= 4
                                Box(
                                    modifier = Modifier
                                        .size(if (isCenterAnchor) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(1.dp, palette.backgroundCanvas, CircleShape)
                                )
                            }
                        }
                    } else {
                        // Weekly View: Pairs of tiny circular status icons summarizing key events
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val topTwo = dayTasks.take(2)
                            topTwo.forEach { t ->
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(t.categoryEnum.timelineColor)
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // In Weekly View: Display the Menstrual / Energy Cycle Phase Bar (Mon-Thu Luteal Coral, Fri-Sun Period Blue)
        if (!isExpandedTodayMode) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.hairlineBorder)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Left droplet/phase gutter icon
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(
                        color = TimelineCoral,
                        radius = 4.dp.toPx(),
                        center = Offset(size.width * 0.5f, size.height * 0.6f)
                    )
                }

                // Mon-Thu Luteal Red/Coral Capsule (4/7 width)
                Box(
                    modifier = Modifier
                        .weight(4f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(TimelineCoral.copy(alpha = 0.85f), TimelineCoral)
                            )
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "☾ LUTEAL // MON–THU",
                        fontFamily = PixelBlockFontFamily,
                        fontSize = 9.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                // Fri-Sun Period Sky-Blue Capsule (3/7 width, aligned with Friday transition)
                Box(
                    modifier = Modifier
                        .weight(3f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(TimelineCerulean, TimelineCerulean.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "✻ PERIOD // FRI–SUN",
                        fontFamily = PixelBlockFontFamily,
                        fontSize = 9.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * WEEKLY VIEW: 7-Column Vertical "Beads-on-a-String" Swimlane Timeline
 * + Long-Press Vertical Drag to reschedule tasks across the entire app
 * + Compact Pull-Up Bottom Sheet with 3 Quick-Action Reminder Tiles.
 */
@Composable
private fun WeeklyBeadsSwimlaneView(
    weekDates: List<LocalDate>,
    selectedDate: LocalDate,
    allTasks: List<TaskEntity>,
    pinnedReminders: List<PinnedReminderTile>,
    activeAccentColor: Color,
    hapticsEnabled: Boolean,
    onSelectDateAndExpandToday: (LocalDate) -> Unit,
    onTaskTap: (TaskEntity) -> Unit,
    onRescheduleTask: (TaskEntity, Int) -> Unit,
    onToggleReminder: (String) -> Unit,
    onPullUpDrawerToToday: () -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val startHour = 7
    val endHour = 22
    val totalMinutes = (endHour - startHour) * 60f
    val timelineHeightDp = 560.dp

    Column(modifier = Modifier.fillMaxSize()) {
        // Scrollable 7-Day Vertical Swimlane Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, palette.hairlineBorder, RoundedCornerShape(10.dp))
                .background(palette.backgroundCanvas.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .testTag("weekly_swimlane_scroll_container")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(timelineHeightDp)
            ) {
                // Left Time Gutter: Stacked micro-timestamps (08⁰⁰, 09⁰⁰, 10⁰⁰...)
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight()
                ) {
                    for (hr in startHour..endHour step 2) {
                        val frac = ((hr - startHour) * 60f) / totalMinutes
                        val yDp = timelineHeightDp * frac
                        Text(
                            text = "%02d⁰⁰".format(hr),
                            fontFamily = TechnicalMonoFontFamily,
                            fontSize = 9.sp,
                            color = palette.mutedInk,
                            modifier = Modifier.offset(x = 2.dp, y = yDp)
                        )
                    }
                }

                // 7 Vertical Swimlane Columns (Mon..Sun)
                weekDates.forEachIndexed { colIdx, date ->
                    val dayTasks = allTasks.filter { it.dateIso == date.toString() }
                        .sortedBy { it.startMinutes }
                    val isSelectedCol = date == selectedDate

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelectDateAndExpandToday(date) }
                            .testTag("weekly_swimlane_col_$colIdx"),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val colHeightPx = with(density) { maxHeight.toPx() }
                        val pxPerMinute = colHeightPx / totalMinutes

                        // Vertical Spine (1.5.dp) connecting beads from top to bottom
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawLine(
                                color = if (isSelectedCol) {
                                    activeAccentColor.copy(alpha = 0.55f)
                                } else {
                                    palette.hairlineBorder
                                },
                                start = Offset(size.width * 0.5f, 8.dp.toPx()),
                                end = Offset(size.width * 0.5f, size.height - 8.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        // Duration-Scaled Stadium Pills (Beads on a String) with Long-Press Drag to Reschedule!
                        dayTasks.forEach { task ->
                            var dragOffsetY by remember(task.id, task.startMinutes) { mutableFloatStateOf(0f) }
                            var isDragging by remember(task.id) { mutableStateOf(false) }

                            val effectiveStartMin = (task.startMinutes + (dragOffsetY / pxPerMinute).roundToInt())
                                .coerceIn(startHour * 60, endHour * 60 - 15)
                            val topYFraction = ((effectiveStartMin - startHour * 60).toFloat() / totalMinutes)
                                .coerceIn(0f, 0.95f)
                            val topYDp = timelineHeightDp * topYFraction

                            val pillHeightDp = when {
                                task.durationMinutes <= 0 -> 30.dp
                                else -> (timelineHeightDp * (task.durationMinutes.toFloat() / totalMinutes))
                                    .coerceIn(32.dp, 110.dp)
                            }

                            val isPassiveSleep = task.specialGlyph == "SLEEP"
                            val pillColor = when {
                                isPassiveSleep -> palette.passiveBlockSurface
                                else -> task.categoryEnum.timelineColor
                            }
                            val glyphKey = task.specialGlyph.ifBlank { task.categoryEnum.code }

                            Box(
                                modifier = Modifier
                                    .offset(y = topYDp)
                                    .scale(if (isDragging) 1.12f else 1.0f)
                                    .width(32.dp)
                                    .height(pillHeightDp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(pillColor)
                                    .border(
                                        width = if (isDragging) 2.dp else 1.dp,
                                        color = if (isDragging) Color.White else palette.backgroundCanvas,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .pointerInput(task.id, task.startMinutes) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                isDragging = true
                                                dragOffsetY = 0f
                                                if (hapticsEnabled) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                            },
                                            onDragEnd = {
                                                isDragging = false
                                                val deltaMin = (dragOffsetY / pxPerMinute).roundToInt()
                                                val newStart = task.startMinutes + deltaMin
                                                dragOffsetY = 0f
                                                if (hapticsEnabled) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                                onRescheduleTask(task, newStart)
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                dragOffsetY = 0f
                                            }
                                        )
                                    }
                                    .clickable { onTaskTap(task) }
                                    .testTag("weekly_capsule_${task.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    PixelArtGlyphCanvas(
                                        glyphKey = glyphKey,
                                        tint = if (isPassiveSleep) TimelineCoral else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (isDragging) {
                                        Text(
                                            text = TimeFormatUtils.format24HourColon(effectiveStartMin),
                                            fontFamily = TechnicalMonoFontFamily,
                                            fontSize = 7.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Compact Pull-Up Bottom Sheet with Grabber Pill + 3 Quick-Action Reminder Tiles
        CompactRemindersBottomDrawer(
            reminders = pinnedReminders,
            onToggleReminder = onToggleReminder,
            onExpandToToday = onPullUpDrawerToToday
        )
    }
}

@Composable
private fun CompactRemindersBottomDrawer(
    reminders: List<PinnedReminderTile>,
    onToggleReminder: (String) -> Unit,
    onExpandToToday: () -> Unit
) {
    val palette = LocalNothingPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
            .background(palette.surfaceElevated)
            .border(
                1.dp,
                palette.hairlineBorder,
                RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("compact_reminders_drawer"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered horizontal grabber pill (~36px x 4px) — tapping expands to Today Single-Day Agenda View!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandToToday() }
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.mutedInk.copy(alpha = 0.6f))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            reminders.forEach { tile ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleReminder(tile.id) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("compact_reminder_${tile.id}")
                ) {
                    ReminderCircularGlyphButton(
                        tile = tile,
                        sizeDp = 36.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tile.shortTitle, // e.g. "Kevins Birt..." in compact drawer
                        fontFamily = GeometricSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * TODAY VIEW: Expanded Single-Day Agenda View with Nothing Yellow Accent Transition,
 * Elevated All-Day Pinned Reminders Header (2-line wrapped labels),
 * Colon-delimited Left Time Gutter (08:00, 08:30, 09:00, 09:30, 10:00, 10:15),
 * Proportional Stadium Capsules, Subtask Progress Pills (0/3),
 * and Actionable Free-Time Gap Prompt ("ZzZ Interval over. What's next?").
 */
@Composable
private fun TodayExpandedAgendaView(
    selectedDate: LocalDate,
    dayTasks: List<TaskEntity>,
    pinnedReminders: List<PinnedReminderTile>,
    activeAccentColor: Color,
    hapticsEnabled: Boolean,
    onTaskTap: (TaskEntity) -> Unit,
    onToggleTaskComplete: (TaskEntity) -> Unit,
    onRescheduleTask: (TaskEntity, Int) -> Unit,
    onToggleReminder: (String) -> Unit,
    onFillFreeTimeGap: (Int, Int) -> Unit,
    onCollapseToWeek: () -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    val sortedTasks = remember(dayTasks) { dayTasks.sortedBy { it.startMinutes } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(palette.surfaceElevated.copy(alpha = 0.42f))
            .border(
                1.dp,
                activeAccentColor.copy(alpha = 0.65f),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .testTag("today_expanded_agenda_view")
    ) {
        // 1. ELEVATED ALL-DAY & PINNED REMINDERS HEADER (Pulled up beneath the calendar strip)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surfaceElevated)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered horizontal grabber pill + Nothing Yellow Ribbon Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCollapseToWeek() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY RIBBON // ${selectedDate.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).uppercase()} ${selectedDate.dayOfMonth}",
                    fontFamily = PixelBlockFontFamily,
                    fontSize = 10.sp,
                    color = activeAccentColor
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(activeAccentColor)
                )
                Text(
                    text = "HOLD & DRAG ↕",
                    fontFamily = TechnicalMonoFontFamily,
                    fontSize = 9.sp,
                    color = palette.mutedInk
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Three Circular Quick-Action Tiles with Two-Line Wrapped Labels (Period, Call Mum, Kevins / Birthday)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                pinnedReminders.forEach { tile ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onToggleReminder(tile.id) }
                            .padding(horizontal = 6.dp)
                            .testTag("expanded_reminder_${tile.id}")
                    ) {
                        ReminderCircularGlyphButton(
                            tile = tile,
                            sizeDp = 52.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${tile.twoLineTop}\n${tile.twoLineBottom}",
                            fontFamily = GeometricSansFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.primaryInk,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Full-width 1px horizontal hairline divider separating pinned reminders from single-day timeline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.hairlineBorder)
        )

        // 2. SINGLE-DAY VERTICAL TIMELINE WITH PROPORTIONAL STADIUM CAPSULES & FREE-TIME GAPS
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            sortedTasks.forEachIndexed { index, task ->
                val nextTask = sortedTasks.getOrNull(index + 1)

                SingleDayAgendaTaskRow(
                    task = task,
                    nextTaskColor = nextTask?.categoryEnum?.timelineColor ?: activeAccentColor,
                    activeAccentColor = activeAccentColor,
                    hapticsEnabled = hapticsEnabled,
                    onTaskTap = { onTaskTap(task) },
                    onToggleComplete = {
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onToggleTaskComplete(task)
                    },
                    onRescheduleTask = onRescheduleTask
                )

                // Check for unscheduled Free-Time Gap (>= 25 minutes) between this task's end and the next task's start
                val taskEnd = if (task.durationMinutes <= 0) task.startMinutes else task.endMinutes
                if (nextTask != null && (nextTask.startMinutes - taskEnd) >= 25 && task.durationMinutes > 0) {
                    val gapStart = taskEnd
                    val gapDur = nextTask.startMinutes - taskEnd
                    ActionableFreeTimeGapRow(
                        gapStartMinutes = gapStart,
                        gapEndMinutes = nextTask.startMinutes,
                        onGapClick = { onFillFreeTimeGap(gapStart, gapDur) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleDayAgendaTaskRow(
    task: TaskEntity,
    nextTaskColor: Color,
    activeAccentColor: Color,
    hapticsEnabled: Boolean,
    onTaskTap: () -> Unit,
    onToggleComplete: () -> Unit,
    onRescheduleTask: (TaskEntity, Int) -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    val subtasks = task.parseSubtasks()
    val nodeColor = task.categoryEnum.timelineColor
    val isPointEvent = task.durationMinutes <= 0
    val capsuleHeightDp = when {
        isPointEvent -> 38.dp
        else -> (task.durationMinutes * 1.15f).dp.coerceIn(48.dp, 108.dp)
    }
    val glyphKey = task.specialGlyph.ifBlank { task.categoryEnum.code }

    var dragDeltaY by remember(task.id, task.startMinutes) { mutableFloatStateOf(0f) }
    var isDragging by remember(task.id) { mutableStateOf(false) }

    val previewMinutes = if (isDragging) {
        (task.startMinutes + (dragDeltaY / 3.2f).roundToInt()).coerceIn(7 * 60, 22 * 60 - 15)
    } else {
        task.startMinutes
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isDragging) 1.03f else 1.0f)
            .pointerInput(task.id, task.startMinutes) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragDeltaY = 0f
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDeltaY += dragAmount.y
                    },
                    onDragEnd = {
                        isDragging = false
                        val newMin = task.startMinutes + (dragDeltaY / 3.2f).roundToInt()
                        dragDeltaY = 0f
                        onRescheduleTask(task, newMin)
                    },
                    onDragCancel = {
                        isDragging = false
                        dragDeltaY = 0f
                    }
                )
            }
            .clickable { onTaskTap() }
            .testTag("agenda_task_row_${task.id}"),
        verticalAlignment = Alignment.Top
    ) {
        // 1. Left Time Gutter: Exact colon-delimited timestamps (08:00, 08:30, 09:30, 10:15)
        Column(
            modifier = Modifier
                .width(46.dp)
                .height(capsuleHeightDp + 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = TimeFormatUtils.format24HourColon(previewMinutes),
                fontFamily = TechnicalMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Normal,
                color = if (isDragging) activeAccentColor else palette.mutedInk
            )
            if (!isPointEvent && task.durationMinutes >= 45) {
                Text(
                    text = TimeFormatUtils.format24HourColon(previewMinutes + task.durationMinutes),
                    fontFamily = TechnicalMonoFontFamily,
                    fontSize = 10.sp,
                    color = palette.mutedInk.copy(alpha = 0.65f)
                )
            }
        }

        // 2. Proportional Stadium Capsule Node + Dynamic Color-Shifting Connector Spine
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(capsuleHeightDp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(nodeColor)
                    .border(
                        width = if (isDragging) 2.dp else 0.dp,
                        color = if (isDragging) activeAccentColor else Color.Transparent,
                        shape = RoundedCornerShape(999.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                PixelArtGlyphCanvas(
                    glyphKey = glyphKey,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Continuous ~2px vertical spine shifting color to bridge consecutive events
            Canvas(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(16.dp)
            ) {
                drawLine(
                    brush = Brush.verticalGradient(listOf(nodeColor, nextTaskColor)),
                    start = Offset(size.width * 0.5f, 0f),
                    end = Offset(size.width * 0.5f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 3. Metadata Row + Bold Task Title + Subtask Progress Pill (0/3)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Timestamp range + Recurring (↻) or Calendar Event badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val rangeText = if (isPointEvent) {
                    TimeFormatUtils.format24HourColon(previewMinutes)
                } else {
                    "${TimeFormatUtils.format24HourColon(previewMinutes)}–${TimeFormatUtils.format24HourColon(previewMinutes + task.durationMinutes)} (${TimeFormatUtils.formatDurationReadable(task.durationMinutes)})"
                }
                Text(
                    text = rangeText,
                    fontFamily = TechnicalMonoFontFamily,
                    fontSize = 11.sp,
                    color = palette.mutedInk
                )
                Text(
                    text = if (task.isRecurring) "↻" else "📅",
                    fontSize = 11.sp,
                    color = activeAccentColor
                )
            }

            Text(
                text = task.title,
                fontFamily = GeometricSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )

            // Subtask Progress Pill (e.g. [✓] 1/3)
            if (subtasks.isNotEmpty()) {
                val doneSub = subtasks.count { it.isCompleted }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.backgroundCanvas)
                        .border(1.dp, palette.hairlineBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "☑ $doneSub/${subtasks.size}",
                        fontFamily = TechnicalMonoFontFamily,
                        fontSize = 10.sp,
                        color = activeAccentColor
                    )
                    Text(
                        text = "· ${task.planetName}",
                        fontFamily = TechnicalMonoFontFamily,
                        fontSize = 9.sp,
                        color = palette.mutedInk
                    )
                }
            }
        }

        // 4. Right-Aligned Color-Matched Completion Ring (~22px)
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable(onClick = onToggleComplete)
                .testTag("agenda_complete_ring_${task.id}"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) nodeColor else Color.Transparent)
                    .border(2.dp, nodeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Text(
                        text = "✓",
                        fontFamily = TechnicalMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Actionable Free-Time Gap ("09:00 - 09:30"):
 * Displays inline prompt "ZzZ Interval over. What's next?" along the vertical connector spine.
 * Tapping opens the Unfolding Red Paper Task Creator pre-configured for that free time slot.
 */
@Composable
private fun ActionableFreeTimeGapRow(
    gapStartMinutes: Int,
    gapEndMinutes: Int,
    onGapClick: () -> Unit
) {
    val palette = LocalNothingPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGapClick)
            .padding(vertical = 4.dp)
            .testTag("free_time_gap_prompt_${gapStartMinutes}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = TimeFormatUtils.format24HourColon(gapStartMinutes),
            fontFamily = TechnicalMonoFontFamily,
            fontSize = 11.sp,
            color = palette.mutedInk,
            modifier = Modifier.width(46.dp)
        )

        Box(
            modifier = Modifier
                .width(44.dp)
                .height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = palette.mutedInk.copy(alpha = 0.5f),
                    start = Offset(size.width * 0.5f, 0f),
                    end = Offset(size.width * 0.5f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(palette.backgroundCanvas.copy(alpha = 0.65f))
                .border(1.dp, palette.hairlineBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ZzZ",
                    fontFamily = PixelBlockFontFamily,
                    fontSize = 10.sp,
                    color = TimelineWarmAmber
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Interval over. What's next?",
                    fontFamily = GeometricSansFontFamily,
                    fontSize = 12.sp,
                    color = palette.mutedInk
                )
            }
            Text(
                text = "+ ADD",
                fontFamily = PixelBlockFontFamily,
                fontSize = 9.sp,
                color = palette.nothingYellow
            )
        }
    }
}

@Composable
private fun ReminderCircularGlyphButton(
    tile: PinnedReminderTile,
    sizeDp: androidx.compose.ui.unit.Dp
) {
    val palette = LocalNothingPalette.current

    Box(
        modifier = Modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = palette.backgroundCanvas,
                radius = size.minDimension * 0.48f
            )
            drawCircle(
                color = palette.hairlineBorder,
                radius = size.minDimension * 0.48f,
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Radial progress arc along upper-right rim
            drawArc(
                color = if (tile.isDone) palette.nothingYellow else tile.accentColor,
                startAngle = -75f,
                sweepAngle = 360f * if (tile.isDone) 1f else tile.progressArc,
                useCenter = false,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        PixelArtGlyphCanvas(
            glyphKey = tile.glyphType,
            tint = if (tile.isDone) palette.nothingYellow else tile.accentColor,
            modifier = Modifier.size(sizeDp * 0.42f)
        )
    }
}
