package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.data.TimeFormatUtils
import com.example.ui.OrbitUiState
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GeometricSansFontFamily
import com.example.ui.theme.LocalNothingPalette
import com.example.ui.theme.NdDotMatrixFontFamily
import com.example.ui.theme.PixelBlockFontFamily
import com.example.ui.theme.TechnicalMonoFontFamily
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Monthly Structural Matrix View:
 * Exposed 7-column modular grid with dot-matrix task density indicators per day.
 */
@Composable
fun MonthlyMatrixCalendarView(
    selectedDate: LocalDate,
    allTasks: List<TaskEntity>,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (Int) -> Unit
) {
    val palette = LocalNothingPalette.current
    val yearMonth = YearMonth.from(selectedDate)
    val firstDayOfMonth = yearMonth.atDay(1)
    val leadingEmptyDays = (firstDayOfMonth.dayOfWeek.value - 1).coerceIn(0, 6)
    val daysInMonth = yearMonth.lengthOfMonth()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(10.dp))
            .background(palette.backgroundCanvas.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("monthly_matrix_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONTHLY STRUCTURAL GRID",
                fontFamily = PixelBlockFontFamily,
                fontSize = 11.sp,
                color = palette.mutedInk
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .border(1.dp, palette.hairlineBorder, RoundedCornerShape(4.dp))
                        .clickable { onChangeMonth(-1) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("◀ PREV", fontFamily = PixelBlockFontFamily, fontSize = 10.sp, color = palette.primaryInk)
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, palette.hairlineBorder, RoundedCornerShape(4.dp))
                        .clickable { onChangeMonth(1) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("NEXT ▶", fontFamily = PixelBlockFontFamily, fontSize = 10.sp, color = palette.primaryInk)
                }
            }
        }

        // Day-of-week header row
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { dow ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dow,
                        fontFamily = TechnicalMonoFontFamily,
                        fontSize = 10.sp,
                        color = palette.mutedInk
                    )
                }
            }
        }

        val totalCells = ((leadingEmptyDays + daysInMonth + 6) / 7) * 7
        val rows = totalCells / 7

        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (c in 0 until 7) {
                    val cellIdx = r * 7 + c
                    val dayNum = cellIdx - leadingEmptyDays + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNum)
                        val isSelected = date == selectedDate
                        val dayTasks = allTasks.filter { it.dateIso == date.toString() }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) palette.nothingRed else palette.hairlineBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .background(
                                    if (isSelected) palette.nothingRed.copy(alpha = 0.16f) else palette.surfaceElevated.copy(alpha = 0.45f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectDate(date) }
                                .padding(5.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "%02d".format(dayNum),
                                fontFamily = NdDotMatrixFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) palette.nothingRed else palette.primaryInk
                            )

                            // Dot-matrix task nodes for that day
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                dayTasks.take(4).forEach { t ->
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (t.isCompleted) palette.nothingRed else t.categoryEnum.timelineColor
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Yearly 12-Month Dot-Matrix Heatmap View:
 * Renders all 12 months of the year as compact Nothing OS dot-matrix arrays.
 */
@Composable
fun YearlyDotMatrixHeatmapView(
    selectedDate: LocalDate,
    allTasks: List<TaskEntity>,
    onSelectMonth: (Int) -> Unit
) {
    val palette = LocalNothingPalette.current
    val year = selectedDate.year
    val taskDatesSet = allTasks.map { it.dateIso }.toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(10.dp))
            .background(palette.backgroundCanvas.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("yearly_heatmap_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YEARLY ORBIT MATRIX // $year",
                fontFamily = NdDotMatrixFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )
            Text(
                text = "365-DOT ARRAY",
                fontFamily = PixelBlockFontFamily,
                fontSize = 10.sp,
                color = palette.nothingRed
            )
        }

        // 3 columns x 4 rows of Monthly Dot Matrices
        for (rowIdx in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (colIdx in 0 until 3) {
                    val monthNum = rowIdx * 3 + colIdx + 1
                    val ym = YearMonth.of(year, monthNum)
                    val isCurrentMonth = monthNum == selectedDate.monthValue
                    val monthShort = ym.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).uppercase()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isCurrentMonth) 1.5.dp else 1.dp,
                                color = if (isCurrentMonth) palette.nothingRed else palette.hairlineBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(palette.surfaceElevated.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .clickable { onSelectMonth(monthNum) }
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = monthShort,
                                fontFamily = PixelBlockFontFamily,
                                fontSize = 10.sp,
                                color = if (isCurrentMonth) palette.nothingRed else palette.primaryInk
                            )
                            Text(
                                text = "%02d".format(monthNum),
                                fontFamily = TechnicalMonoFontFamily,
                                fontSize = 9.sp,
                                color = palette.mutedInk
                            )
                        }

                        // 7x5 Dot-Matrix for the days of the month
                        val daysCount = ym.lengthOfMonth()
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            val cols = 7
                            val rows = 5
                            val stepX = size.width / cols.toFloat()
                            val stepY = size.height / rows.toFloat()
                            for (d in 1..daysCount) {
                                val idx = d - 1
                                val cx = (idx % cols) * stepX + stepX * 0.5f
                                val cy = (idx / cols) * stepY + stepY * 0.5f
                                val iso = ym.atDay(d).toString()
                                val hasTasks = taskDatesSet.contains(iso)
                                val isPast = ym.atDay(d).isBefore(selectedDate)
                                val dotColor = when {
                                    hasTasks -> palette.nothingRed
                                    isPast -> palette.primaryInk.copy(alpha = 0.75f)
                                    else -> palette.mutedInk.copy(alpha = 0.32f)
                                }
                                drawCircle(
                                    color = dotColor,
                                    radius = if (hasTasks) 2.6.dp.toPx() else 1.8.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Nothing OS Inspired Settings Panel:
 * - Theme Posture (OLED Dark Mode, Alabaster Light Mode, System Auto)
 * - Haptic Engine Toggle
 * - Live Meteor Working-Day Time Simulator (test the red dot-matrix trail at any hour from 07:00 to 22:00)
 * - Demo Schedule Re-seeder
 */
@Composable
fun NothingCalendarSettingsView(
    uiState: OrbitUiState,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetSimulatedTime: (Boolean, Int) -> Unit,
    onResetDemoData: () -> Unit
) {
    val palette = LocalNothingPalette.current
    val settings = uiState.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(10.dp))
            .background(palette.backgroundCanvas.copy(alpha = 0.90f), RoundedCornerShape(10.dp))
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
            .testTag("nothing_settings_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "SYSTEM.CFG // NOTHING OS PARAMETERS",
            fontFamily = NdDotMatrixFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryInk
        )

        // 1. UI Theme Mode Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.hairlineBorder, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DISPLAY SUBSTRATE // THEME MODE",
                fontFamily = PixelBlockFontFamily,
                fontSize = 10.sp,
                color = palette.mutedInk
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = settings.themeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) palette.nothingRed else palette.surfaceElevated)
                            .border(1.dp, palette.hairlineBorder, RoundedCornerShape(6.dp))
                            .clickable { onSetThemeMode(mode) }
                            .padding(vertical = 10.dp)
                            .testTag("settings_theme_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            fontFamily = PixelBlockFontFamily,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else palette.primaryInk
                        )
                    }
                }
            }
        }

        // 2. Tactile Haptics Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.hairlineBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TACTILE HAPTIC ACTUATOR",
                    fontFamily = PixelBlockFontFamily,
                    fontSize = 11.sp,
                    color = palette.primaryInk
                )
                Text(
                    text = "Fires physical ticks on Vernier scrubbers, paper unfold & quest completion",
                    fontFamily = GeometricSansFontFamily,
                    fontSize = 12.sp,
                    color = palette.mutedInk
                )
            }
            Switch(
                checked = settings.hapticsEnabled,
                onCheckedChange = onSetHaptics,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = palette.nothingRed
                )
            )
        }

        // 3. Working-Day Meteor Clock Simulator (07:00 - 22:00)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.hairlineBorder, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "METEOR TIME OVERRIDE (07:00 – 22:00)",
                        fontFamily = PixelBlockFontFamily,
                        fontSize = 10.sp,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "Scrub working-day time to inspect red dot-matrix trail & % left bars",
                        fontFamily = GeometricSansFontFamily,
                        fontSize = 11.sp,
                        color = palette.mutedInk
                    )
                }
                Switch(
                    checked = settings.useSimulatedTime,
                    onCheckedChange = { enabled ->
                        onSetSimulatedTime(enabled, uiState.currentMinutesFromMidnight)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.nothingRed
                    )
                )
            }

            Text(
                text = "ACTIVE TELEMETRY TIME: ${TimeFormatUtils.format24HourColon(uiState.currentMinutesFromMidnight)} (${uiState.dayRemainingPercent}% DAY LEFT)",
                fontFamily = NdDotMatrixFontFamily,
                fontSize = 18.sp,
                color = palette.nothingRed
            )

            Slider(
                value = uiState.currentMinutesFromMidnight.toFloat(),
                onValueChange = { newVal ->
                    onSetSimulatedTime(true, newVal.roundToInt())
                },
                valueRange = (7 * 60f)..(22 * 60f),
                colors = SliderDefaults.colors(
                    thumbColor = palette.nothingRed,
                    activeTrackColor = palette.nothingRed,
                    inactiveTrackColor = palette.hairlineBorder
                )
            )
        }

        // 4. Reset & Reseed Orbital Schedule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .border(1.5.dp, palette.nothingRed, RoundedCornerShape(8.dp))
                .clickable { onResetDemoData() }
                .padding(vertical = 12.dp)
                .testTag("reset_demo_schedule_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "↻ RE-SEED DEFAULT ORBITAL & WEEKLY QUESTS",
                fontFamily = NdDotMatrixFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.nothingRed
            )
        }
    }
}
