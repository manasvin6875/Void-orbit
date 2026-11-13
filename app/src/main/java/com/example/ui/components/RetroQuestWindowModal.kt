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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TaskEntity
import com.example.data.TimeFormatUtils
import com.example.ui.theme.GeometricSansFontFamily
import com.example.ui.theme.LocalNothingPalette
import com.example.ui.theme.NdDotMatrixFontFamily
import com.example.ui.theme.PixelBlockFontFamily
import com.example.ui.theme.TechnicalMonoFontFamily

/**
 * Philosophically Nothing-inspired Retro OS Window Modal shown when tapping a Planet on the meteor path.
 * Displays the task as an Orbital Quest with a live rotating pixel planet preview, generated planet name
 * & characteristics (based on difficulty, tag, and duration), subtasks, and a tick button that completes
 * the quest and sets the planet into continuous axial rotation.
 */
@Composable
fun RetroQuestWindowModal(
    task: TaskEntity,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onToggleSubtask: (TaskEntity, String) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit
) {
    val palette = LocalNothingPalette.current
    val haptic = LocalHapticFeedback.current
    val subtasks = task.parseSubtasks()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp)
                .testTag("retro_quest_window_modal")
        ) {
            // Retro OS hard pixel offset shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(palette.nothingRed.copy(alpha = 0.85f))
            )

            // Main exposed-wireframe Nothing-meets-Retro-Windows container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.backgroundCanvas)
                    .border(2.dp, palette.primaryInk)
            ) {
                // Retro Window Title Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.primaryInk)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = palette.nothingRed)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "QUEST.SYS // ${task.planetName}",
                            fontFamily = PixelBlockFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.backgroundCanvas
                        )
                    }

                    // Retro [X] Close Box
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .border(1.5.dp, palette.backgroundCanvas)
                            .clickable { onDismiss() }
                            .testTag("quest_window_close_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "X",
                            fontFamily = PixelBlockFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.backgroundCanvas
                        )
                    }
                }

                // Window Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Row: Pixel Alien Planet Viewport + Planet Lore Readout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exposed Structural Planet Specimen Frame
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .border(1.dp, palette.hairlineBorder)
                                .background(Color(0xFF050508))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ProceduralPlanetCanvas(
                                seed = task.planetSeed,
                                category = task.categoryEnum,
                                difficulty = task.difficultyEnum,
                                isRotating = task.isCompleted,
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = if (task.isCompleted) "LIVE SPIN" else "STATIC",
                                fontFamily = TechnicalMonoFontFamily,
                                fontSize = 8.sp,
                                color = if (task.isCompleted) palette.nothingRed else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(2.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = task.difficultyEnum.classification,
                                fontFamily = PixelBlockFontFamily,
                                fontSize = 9.sp,
                                color = palette.nothingRed
                            )
                            Text(
                                text = task.title,
                                fontFamily = GeometricSansFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                            Text(
                                text = "${TimeFormatUtils.format24HourColon(task.startMinutes)} – ${TimeFormatUtils.format24HourColon(task.endMinutes)} (${TimeFormatUtils.formatDurationReadable(task.durationMinutes)})",
                                fontFamily = TechnicalMonoFontFamily,
                                fontSize = 11.sp,
                                color = palette.mutedInk
                            )
                        }
                    }

                    // Generated Planetary Characteristic Telemetry Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.hairlineBorder)
                            .background(palette.surfaceElevated.copy(alpha = 0.55f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PLANETARY TELEMETRY",
                                fontFamily = PixelBlockFontFamily,
                                fontSize = 9.sp,
                                color = palette.mutedInk
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PixelArtGlyphCanvas(
                                    glyphKey = task.categoryEnum.code,
                                    tint = task.categoryEnum.timelineColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = task.categoryEnum.label,
                                    fontFamily = TechnicalMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = task.categoryEnum.timelineColor
                                )
                            }
                        }
                        Text(
                            text = task.planetCharacteristic,
                            fontFamily = TechnicalMonoFontFamily,
                            fontSize = 11.sp,
                            color = palette.primaryInk
                        )
                    }

                    // Nested Subtasks Checklist (if present)
                    if (subtasks.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "QUEST OBJECTIVES (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                                fontFamily = PixelBlockFontFamily,
                                fontSize = 10.sp,
                                color = palette.mutedInk
                            )
                            subtasks.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.hairlineBorder)
                                        .clickable {
                                            if (hapticsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            onToggleSubtask(task, sub.id)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (sub.isCompleted) "[✓]" else "[ ]",
                                        fontFamily = TechnicalMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sub.isCompleted) palette.nothingRed else palette.mutedInk
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sub.text,
                                        fontFamily = GeometricSansFontFamily,
                                        fontSize = 13.sp,
                                        color = palette.primaryInk,
                                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Action Bar: Complete Quest Tick Button + Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .background(
                                    if (task.isCompleted) palette.primaryInk else palette.nothingRed
                                )
                                .border(1.5.dp, palette.primaryInk)
                                .clickable {
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onToggleComplete(task)
                                }
                                .padding(vertical = 12.dp)
                                .testTag("complete_quest_tick_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (task.isCompleted) {
                                    "✓ QUEST COMPLETED // PLANET ALIVE"
                                } else {
                                    "[ TICK QUEST COMPLETE ✓ ]"
                                },
                                fontFamily = NdDotMatrixFontFamily,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (task.isCompleted) palette.backgroundCanvas else Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .border(1.dp, palette.hairlineBorder)
                                .clickable { onDeleteTask(task) }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("delete_quest_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DEL",
                                fontFamily = PixelBlockFontFamily,
                                fontSize = 11.sp,
                                color = palette.mutedInk
                            )
                        }
                    }
                }
            }
        }
    }
}
