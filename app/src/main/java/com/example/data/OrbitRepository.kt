package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.TimelineCerulean
import com.example.ui.theme.TimelineCoral
import com.example.ui.theme.TimelineLeafGreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.norbitDataStore by preferencesDataStore(name = "norbit_preferences")

data class UserSettingsState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val hapticsEnabled: Boolean = true,
    val useSimulatedTime: Boolean = false,
    val simulatedMinutesFromMidnight: Int = 13 * 60 + 40, // 13:40 (1:40 PM)
    val vernierOffsetHours: Int = 3, // GMT +3 default matching specification
    val completedReminderIds: Set<String> = emptySet()
)

class OrbitRepository(
    private val context: Context,
    private val taskDao: TaskDao
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val USE_SIMULATED_TIME = booleanPreferencesKey("use_simulated_time")
        val SIMULATED_MINUTES = intPreferencesKey("simulated_minutes")
        val VERNIER_OFFSET = intPreferencesKey("vernier_offset_hours")
        val COMPLETED_REMINDERS = stringPreferencesKey("completed_reminders")
    }

    val allTasksFlow: Flow<List<TaskEntity>> = taskDao.observeAllTasks()

    val settingsFlow: Flow<UserSettingsState> = context.norbitDataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name
        val theme = runCatching { AppThemeMode.valueOf(themeStr) }.getOrDefault(AppThemeMode.DARK)
        val completedRaw = prefs[Keys.COMPLETED_REMINDERS].orEmpty()
        val completedSet = completedRaw.split(",").filter { it.isNotBlank() }.toSet()
        UserSettingsState(
            themeMode = theme,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            useSimulatedTime = prefs[Keys.USE_SIMULATED_TIME] ?: false,
            simulatedMinutesFromMidnight = prefs[Keys.SIMULATED_MINUTES] ?: (13 * 60 + 40),
            vernierOffsetHours = prefs[Keys.VERNIER_OFFSET] ?: 3,
            completedReminderIds = completedSet
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.norbitDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.norbitDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setSimulatedTime(enabled: Boolean, minutes: Int) {
        context.norbitDataStore.edit {
            it[Keys.USE_SIMULATED_TIME] = enabled
            it[Keys.SIMULATED_MINUTES] = minutes.coerceIn(7 * 60, 22 * 60)
        }
    }

    suspend fun setVernierOffsetHours(offsetHours: Int) {
        context.norbitDataStore.edit {
            it[Keys.VERNIER_OFFSET] = offsetHours.coerceIn(-12, 14)
        }
    }

    suspend fun togglePinnedReminder(reminderId: String) {
        context.norbitDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_REMINDERS]
                .orEmpty()
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (current.contains(reminderId)) {
                current.remove(reminderId)
            } else {
                current.add(reminderId)
            }
            prefs[Keys.COMPLETED_REMINDERS] = current.joinToString(",")
        }
    }

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun ensureSeeded(referenceDate: LocalDate = LocalDate.now()) {
        if (taskDao.getTaskCount() == 0) {
            seedRichWeekSchedule(referenceDate)
        }
    }

    suspend fun resetAndReseed(referenceDate: LocalDate = LocalDate.now()) {
        taskDao.clearAllTasks()
        seedRichWeekSchedule(referenceDate)
    }

    fun getPinnedReminders(completedIds: Set<String>): List<PinnedReminderTile> {
        return listOf(
            PinnedReminderTile(
                id = "rem_period",
                title = "Period",
                shortTitle = "Period",
                twoLineTop = "Period",
                twoLineBottom = "Day 1 · Cycle",
                accentColor = TimelineCerulean,
                progressArc = 0.38f,
                glyphType = "SNOWFLAKE",
                statusDetail = "CYCLE PHASE TRANSITION ACTIVE",
                isDone = completedIds.contains("rem_period")
            ),
            PinnedReminderTile(
                id = "rem_call_mum",
                title = "Call Mum",
                shortTitle = "Call Mum",
                twoLineTop = "Call",
                twoLineBottom = "Mum",
                accentColor = TimelineLeafGreen,
                progressArc = 0.75f,
                glyphType = "PHONE",
                statusDetail = "FAMILY RELAY // 15M CHECK-IN",
                isDone = completedIds.contains("rem_call_mum")
            ),
            PinnedReminderTile(
                id = "rem_kevins_bday",
                title = "Kevins Birthday",
                shortTitle = "Kevins Birt...",
                twoLineTop = "Kevins",
                twoLineBottom = "Birthday",
                accentColor = TimelineCoral,
                progressArc = 0.92f,
                glyphType = "CAKE",
                statusDetail = "ALL-DAY MILESTONE ANCHOR",
                isDone = completedIds.contains("rem_kevins_bday")
            )
        )
    }

    private suspend fun seedRichWeekSchedule(today: LocalDate) {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val seededTasks = mutableListOf<TaskEntity>()

        fun makeTask(
            dayOffset: Int,
            title: String,
            startMin: Int,
            durMin: Int,
            cat: TaskCategory,
            tier: DifficultyTier,
            completed: Boolean = false,
            recurring: Boolean = false,
            subtasks: List<SubtaskItem> = emptyList(),
            seed: Int,
            glyph: String = ""
        ): TaskEntity {
            val dateStr = monday.plusDays(dayOffset.toLong()).toString()
            val (pName, pLore) = PlanetLoreGenerator.generatePlanetIdentity(seed, cat, tier, durMin)
            return TaskEntity(
                title = title,
                dateIso = dateStr,
                startMinutes = startMin,
                durationMinutes = durMin,
                tagCategory = cat.code,
                difficultyTier = tier.code,
                isCompleted = completed,
                isRecurring = recurring,
                subtasksRaw = TaskEntity.serializeSubtasks(subtasks),
                planetSeed = seed,
                planetName = pName,
                planetCharacteristic = pLore,
                specialGlyph = glyph
            )
        }

        // Seed all 7 days of the current week (Mon..Sun) with rich visual rhythm
        // For whichever day is TODAY (Plus Friday), ensure the exact Expanded Agenda + Orbital Planets experience!
        val todayOffset = ((today.dayOfWeek.value - 1).coerceIn(0, 6))

        for (dayIdx in 0..6) {
            val isWeekend = dayIdx >= 5
            val isTodayOrFriday = (dayIdx == todayOffset || dayIdx == 4)

            if (!isWeekend) {
                // 08:00 Wake Up (Zero-duration or 15-min morning anchor)
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Wake Up",
                    startMin = 8 * 60,
                    durMin = 0,
                    cat = TaskCategory.HEALTH,
                    tier = DifficultyTier.C,
                    completed = true,
                    recurring = true,
                    seed = 100 + dayIdx * 17,
                    glyph = "ALARM"
                )
            } else {
                // Weekend Shift: 08:00 - 09:20 Sleep-In ZzZ block
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Recovery Sleep-In",
                    startMin = 8 * 60,
                    durMin = 80,
                    cat = TaskCategory.MIND,
                    tier = DifficultyTier.C,
                    completed = dayIdx < todayOffset,
                    recurring = true,
                    seed = 500 + dayIdx * 23,
                    glyph = "SLEEP"
                )
            }

            if (isTodayOrFriday) {
                // Exact rich sequence for Today / Friday:
                // 1) 08:00 - 08:30 Morning Routine (0/3 subtasks)
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Morning Routine",
                    startMin = 8 * 60,
                    durMin = 30,
                    cat = TaskCategory.MIND,
                    tier = DifficultyTier.C,
                    completed = true,
                    recurring = true,
                    subtasks = listOf(
                        SubtaskItem("s1", "Hydrate 500ml + Electrolytes", true),
                        SubtaskItem("s2", "Sunlight Optic Calibration", false),
                        SubtaskItem("s3", "Zero-Inbox Triage", false)
                    ),
                    seed = 204 + dayIdx,
                    glyph = "SUN"
                )
                // 2) 08:30 - 09:00 Walk Huge (Leaf-green paw print)
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Walk Huge",
                    startMin = 8 * 60 + 30,
                    durMin = 30,
                    cat = TaskCategory.FITNESS,
                    tier = DifficultyTier.B,
                    completed = true,
                    recurring = true,
                    seed = 318 + dayIdx,
                    glyph = "PAW"
                )
                // [09:00 - 09:30 left intentionally unscheduled so the "ZzZ Interval over. What's next?" gap prompt shines!]
                // 3) 09:30 - 10:15 Coffee with George (45-min external event)
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Coffee with George",
                    startMin = 9 * 60 + 30,
                    durMin = 45,
                    cat = TaskCategory.SOCIAL,
                    tier = DifficultyTier.B,
                    completed = false,
                    recurring = false,
                    seed = 442 + dayIdx,
                    glyph = "COFFEE"
                )
                // 4) 10:15 - 11:45 Deep Architecture Sprint
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Nothing OS Kernel & Shader Sprint",
                    startMin = 10 * 60 + 15,
                    durMin = 90,
                    cat = TaskCategory.WORK,
                    tier = DifficultyTier.S,
                    completed = false,
                    recurring = false,
                    subtasks = listOf(
                        SubtaskItem("k1", "Calibrate Vernier Loupe Matrix", true),
                        SubtaskItem("k2", "Synthesize Pixel Planet Shaders", false),
                        SubtaskItem("k3", "Verify Bezier Meteor Spline", false)
                    ),
                    seed = 777 + dayIdx
                )
                // 5) 13:30 - 14:30 Industrial Design Review
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Glyph Matrix Telemetry Sync",
                    startMin = 13 * 60 + 30,
                    durMin = 60,
                    cat = TaskCategory.CREATIVE,
                    tier = DifficultyTier.A,
                    completed = false,
                    recurring = true,
                    seed = 889 + dayIdx
                )
                // 6) 16:30 - 17:30 Heavy Compound Lift
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Zero-G Hypertrophy Session",
                    startMin = 16 * 60 + 30,
                    durMin = 60,
                    cat = TaskCategory.FITNESS,
                    tier = DifficultyTier.A,
                    completed = false,
                    recurring = true,
                    seed = 934 + dayIdx
                )
                // 7) 19:30 - 20:45 Night Synth & Reading
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Orbital Astrophysics & Synth Lab",
                    startMin = 19 * 60 + 30,
                    durMin = 75,
                    cat = TaskCategory.STUDY,
                    tier = DifficultyTier.B,
                    completed = false,
                    recurring = false,
                    seed = 991 + dayIdx
                )
            } else {
                // Other days of the week so the 7-day weekly swimlane has rich colorful beads
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = if (dayIdx % 2 == 0) "System Design Focus" else "Research & Reading",
                    startMin = 9 * 60 + 30,
                    durMin = 75,
                    cat = if (dayIdx % 2 == 0) TaskCategory.WORK else TaskCategory.STUDY,
                    tier = DifficultyTier.A,
                    completed = dayIdx < todayOffset,
                    recurring = true,
                    seed = 610 + dayIdx * 31
                )
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = if (dayIdx % 3 == 0) "Call Mum & Sync" else "Mindful Fuel Break",
                    startMin = 12 * 60 + 30,
                    durMin = 45,
                    cat = if (dayIdx % 3 == 0) TaskCategory.SOCIAL else TaskCategory.CREATIVE,
                    tier = DifficultyTier.B,
                    completed = dayIdx < todayOffset,
                    recurring = false,
                    seed = 710 + dayIdx * 19,
                    glyph = if (dayIdx % 3 == 0) "PHONE" else "COFFEE"
                )
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = if (isWeekend) "Weekend Takeout & Errands" else "Strength & Mobility",
                    startMin = 16 * 60,
                    durMin = 60,
                    cat = if (isWeekend) TaskCategory.ERRANDS else TaskCategory.FITNESS,
                    tier = if (isWeekend) DifficultyTier.B else DifficultyTier.A,
                    completed = dayIdx < todayOffset,
                    recurring = !isWeekend,
                    seed = 810 + dayIdx * 29
                )
                seededTasks += makeTask(
                    dayOffset = dayIdx,
                    title = "Evening Arcade & Ledger",
                    startMin = 19 * 60 + 30,
                    durMin = 60,
                    cat = if (dayIdx % 2 == 0) TaskCategory.FINANCE else TaskCategory.GAMING,
                    tier = DifficultyTier.C,
                    completed = false,
                    recurring = false,
                    seed = 910 + dayIdx * 37
                )
            }
        }

        taskDao.insertTasks(seededTasks)
    }
}
