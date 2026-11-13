package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CalendarSubView
import com.example.ui.OrbitCalendarViewModel
import com.example.ui.OrbitUiState
import com.example.ui.PrimaryAppSection
import com.example.ui.components.FixedDotMatrixBackground
import com.example.ui.components.OrbitalHomeScreen
import com.example.ui.components.RetroQuestWindowModal
import com.example.ui.components.StructuredCalendarScreen
import com.example.ui.components.UnfoldingTaskPaperOverlay
import com.example.ui.theme.LocalNothingPalette
import com.example.ui.theme.NOrbitTheme
import com.example.ui.theme.NdDotMatrixFontFamily
import com.example.ui.theme.PixelBlockFontFamily
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val orbitViewModel: OrbitCalendarViewModel = viewModel()
            val uiState by orbitViewModel.uiState.collectAsStateWithLifecycle()

            NOrbitTheme(themeMode = uiState.settings.themeMode) {
                NOrbitRootApp(
                    uiState = uiState,
                    viewModel = orbitViewModel
                )
            }
        }
    }
}

@Composable
fun NOrbitRootApp(
    uiState: OrbitUiState,
    viewModel: OrbitCalendarViewModel
) {
    val palette = LocalNothingPalette.current

    // Handle system Back navigation cleanly across modals and secondary sections
    BackHandler(
        enabled = uiState.isPaperTaskCreatorOpen ||
            uiState.activeQuestPlanetTask != null ||
            uiState.primarySection == PrimaryAppSection.FULL_CALENDAR
    ) {
        when {
            uiState.isPaperTaskCreatorOpen -> viewModel.closeTaskPaper()
            uiState.activeQuestPlanetTask != null -> viewModel.closeQuestPlanetModal()
            uiState.primarySection == PrimaryAppSection.FULL_CALENDAR -> {
                if (uiState.calendarSubView != CalendarSubView.WEEK) {
                    viewModel.selectCalendarSubView(CalendarSubView.WEEK)
                } else {
                    viewModel.selectPrimarySection(PrimaryAppSection.ORBIT_HOME)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.backgroundCanvas,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.backgroundCanvas)
        ) {
            // 1. Fixed Viewport Dot-Matrix Background (like Nothing's official website & app grid)
            FixedDotMatrixBackground(
                modifier = Modifier.fillMaxSize(),
                gridSpacingDp = 16.dp,
                dotRadiusDp = 1.25.dp
            )

            // 2. Foreground Content Column + Bottom 2-Option Spring Switcher
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Optional Orbit Locked notification banner
                uiState.lastCreatedPlanetBanner?.let { banner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .background(palette.nothingRed, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("orbit_locked_banner"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = banner,
                            fontFamily = NdDotMatrixFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Main Content Area (ORBIT_HOME vs FULL_CALENDAR)
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = uiState.primarySection,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(260)) +
                                scaleIn(
                                    initialScale = 0.95f,
                                    animationSpec = spring(
                                        dampingRatio = 0.76f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )) togetherWith
                                (fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.96f))
                        },
                        label = "primary_section_transition"
                    ) { section ->
                        when (section) {
                            PrimaryAppSection.ORBIT_HOME -> {
                                OrbitalHomeScreen(
                                    uiState = uiState,
                                    onOpenTaskPaper = { viewModel.openTaskPaper() },
                                    onPlanetTap = { task -> viewModel.openQuestPlanetModal(task) },
                                    onToggleTimeSim = { enabled, min ->
                                        viewModel.setSimulatedTime(enabled, min)
                                    }
                                )
                            }

                            PrimaryAppSection.FULL_CALENDAR -> {
                                StructuredCalendarScreen(
                                    uiState = uiState,
                                    onSelectSubView = { viewModel.selectCalendarSubView(it) },
                                    onSelectDate = { viewModel.selectDate(it) },
                                    onTaskTap = { viewModel.openQuestPlanetModal(it) },
                                    onToggleTaskComplete = { viewModel.toggleTaskCompleted(it) },
                                    onRescheduleTask = { task, newStart ->
                                        viewModel.rescheduleTaskTime(task, newStart)
                                    },
                                    onOpenTaskPaperWithSlot = { startMin, durMin ->
                                        viewModel.openTaskPaper(startMin, durMin)
                                    },
                                    onToggleReminder = { viewModel.togglePinnedReminder(it) },
                                    onSetThemeMode = { viewModel.setThemeMode(it) },
                                    onSetHaptics = { viewModel.setHapticsEnabled(it) },
                                    onSetSimulatedTime = { enabled, min ->
                                        viewModel.setSimulatedTime(enabled, min)
                                    },
                                    onResetDemoData = { viewModel.resetDemoSchedule() }
                                )
                            }
                        }
                    }
                }

                // 3. Bottom 2-Option Spring Switcher (ORBIT // DAY vs FULL CALENDAR) + Overlapping + FAB
                BottomSpringModeSwitcher(
                    activeSection = uiState.primarySection,
                    calendarSubView = uiState.calendarSubView,
                    hapticsEnabled = uiState.settings.hapticsEnabled,
                    onSelectSection = { viewModel.selectPrimarySection(it) },
                    onFabAddClick = { viewModel.openTaskPaper() }
                )
            }

            // 4. Unfolding Red Paper Task Creator Overlay (Origami 4-Flap + Vernier Scrubbers)
            UnfoldingTaskPaperOverlay(
                isOpen = uiState.isPaperTaskCreatorOpen,
                draft = uiState.taskDraft,
                vernierOffsetHours = uiState.settings.vernierOffsetHours,
                hapticsEnabled = uiState.settings.hapticsEnabled,
                onMinimizeClose = { viewModel.closeTaskPaper() },
                onExpandTitleBar = { viewModel.expandDraftTitleBar() },
                onUpdateTitle = { viewModel.updateDraftTitle(it) },
                onSelectCapsule = { viewModel.selectDraftCapsule(it) },
                onUpdateStartMinutes = { viewModel.updateDraftStartMinutes(it) },
                onUpdateDurationMinutes = { viewModel.updateDraftDurationMinutes(it) },
                onCycleVernierOffset = { viewModel.cycleVernierOffsetHours() },
                onSelectCategory = { viewModel.selectDraftCategory(it) },
                onSelectDifficulty = { viewModel.selectDraftDifficulty(it) },
                onCommitTask = { viewModel.commitNewTaskFromPaper() }
            )

            // 5. Retro OS Quest Window Modal when a Planet or Task is tapped
            uiState.activeQuestPlanetTask?.let { activeTask ->
                RetroQuestWindowModal(
                    task = activeTask,
                    hapticsEnabled = uiState.settings.hapticsEnabled,
                    onDismiss = { viewModel.closeQuestPlanetModal() },
                    onToggleComplete = { viewModel.toggleTaskCompleted(it) },
                    onToggleSubtask = { task, subId -> viewModel.toggleSubtask(task, subId) },
                    onDeleteTask = { viewModel.deleteTask(it) }
                )
            }
        }
    }
}

/**
 * Bottom 2-Option Spring Selection Bar (ORBIT // DAY vs FULL CALENDAR)
 * with iOS spring physics and an overlapping primary (+) FAB in the right-handed thumb zone.
 */
@Composable
private fun BottomSpringModeSwitcher(
    activeSection: PrimaryAppSection,
    calendarSubView: CalendarSubView,
    hapticsEnabled: Boolean,
    onSelectSection: (PrimaryAppSection) -> Unit,
    onFabAddClick: () -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val springOffset by animateFloatAsState(
        targetValue = if (activeSection == PrimaryAppSection.ORBIT_HOME) 0f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f, // Snappy iOS spring motion
            stiffness = 410f
        ),
        label = "bottom_mode_spring_switcher"
    )

    val activeHighlightColor by animateColorAsState(
        targetValue = if (activeSection == PrimaryAppSection.FULL_CALENDAR && calendarSubView == CalendarSubView.TODAY) {
            palette.nothingYellow
        } else {
            palette.nothingRed
        },
        animationSpec = tween(420),
        label = "bottom_pill_color_morph"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 2-Option Spring Pill Switcher
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.surfaceElevated)
                .border(1.dp, palette.hairlineBorder, RoundedCornerShape(999.dp))
                .padding(3.dp)
        ) {
            val halfWidthDp = maxWidth / 2
            val halfWidthPx = with(density) { halfWidthDp.toPx() }

            // Spring-loaded sliding indicator capsule
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(x = (springOffset * halfWidthPx).roundToInt(), y = 0)
                    }
                    .width(halfWidthDp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(activeHighlightColor)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // Option 1: ORBIT // DAY
                val isOrbit = activeSection == PrimaryAppSection.ORBIT_HOME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSelectSection(PrimaryAppSection.ORBIT_HOME)
                        }
                        .testTag("bottom_tab_orbit_home"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "● ORBIT // DAY",
                        fontFamily = PixelBlockFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOrbit) Color.White else palette.mutedInk
                    )
                }

                // Option 2: FULL CALENDAR
                val isCal = activeSection == PrimaryAppSection.FULL_CALENDAR
                val calTextColor = when {
                    isCal && calendarSubView == CalendarSubView.TODAY -> Color(0xFF0A0A0C)
                    isCal -> Color.White
                    else -> palette.mutedInk
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSelectSection(PrimaryAppSection.FULL_CALENDAR)
                        }
                        .testTag("bottom_tab_full_calendar"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⊞ FULL CALENDAR",
                        fontFamily = PixelBlockFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = calTextColor
                    )
                }
            }
        }

        // Right-hand thumb zone (+) FAB matching active Red / Yellow accent
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(46.dp)
                .clip(CircleShape)
                .background(activeHighlightColor)
                .border(1.5.dp, palette.primaryInk, CircleShape)
                .clickable {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onFabAddClick()
                }
                .testTag("bottom_fab_add_task"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontFamily = NdDotMatrixFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (activeSection == PrimaryAppSection.FULL_CALENDAR && calendarSubView == CalendarSubView.TODAY) {
                    Color(0xFF0A0A0C)
                } else {
                    Color.White
                }
            )
        }
    }
}
