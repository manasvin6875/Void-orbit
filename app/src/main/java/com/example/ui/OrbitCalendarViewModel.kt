package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DifficultyTier
import com.example.data.OrbitDatabase
import com.example.data.OrbitRepository
import com.example.data.PinnedReminderTile
import com.example.data.PlanetLoreGenerator
import com.example.data.SubtaskItem
import com.example.data.TaskCategory
import com.example.data.TaskEntity
import com.example.data.UserSettingsState
import com.example.ui.theme.AppThemeMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PrimaryAppSection {
    ORBIT_HOME,
    FULL_CALENDAR
}

enum class CalendarSubView {
    WEEK,
    TODAY,
    MONTH,
    YEAR,
    SETTINGS
}

enum class VernierCapsuleMode {
    TIME,
    DURATION
}

data class TaskDraftState(
    val title: String = "",
    val isTitleBarExpanded: Boolean = false,
    val activeCapsule: VernierCapsuleMode = VernierCapsuleMode.TIME,
    val startMinutes: Int = 11 * 60, // 11:00 AM default
    val durationMinutes: Int = 45,
    val selectedCategory: TaskCategory = TaskCategory.WORK,
    val selectedDifficulty: DifficultyTier = DifficultyTier.A
)

data class OrbitUiState(
    val primarySection: PrimaryAppSection = PrimaryAppSection.ORBIT_HOME,
    val calendarSubView: CalendarSubView = CalendarSubView.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val todayDate: LocalDate = LocalDate.now(),
    val currentMinutesFromMidnight: Int = 13 * 60 + 41,
    val allTasks: List<TaskEntity> = emptyList(),
    val selectedDateTasks: List<TaskEntity> = emptyList(),
    val pinnedReminders: List<PinnedReminderTile> = emptyList(),
    val settings: UserSettingsState = UserSettingsState(),
    val isPaperTaskCreatorOpen: Boolean = false,
    val taskDraft: TaskDraftState = TaskDraftState(),
    val activeQuestPlanetTask: TaskEntity? = null,
    val lastCreatedPlanetBanner: String? = null
) {
    // Working day window: 07:00 (420) to 22:00 (1320) -> 900 minutes total
    val workingDayStartMinutes: Int = 7 * 60
    val workingDayEndMinutes: Int = 22 * 60

    val dayProgressDoneFraction: Float
        get() {
            val total = (workingDayEndMinutes - workingDayStartMinutes).toFloat()
            val elapsed = (currentMinutesFromMidnight - workingDayStartMinutes).toFloat().coerceIn(0f, total)
            return (elapsed / total).coerceIn(0f, 1f)
        }

    val dayRemainingPercent: Int
        get() = ((1f - dayProgressDoneFraction) * 100f).toInt().coerceIn(0, 100)

    val monthProgressDoneFraction: Float
        get() {
            val ym = YearMonth.from(selectedDate)
            val totalDays = ym.lengthOfMonth().toFloat()
            val done = (selectedDate.dayOfMonth - 1 + dayProgressDoneFraction).coerceIn(0f, totalDays)
            return (done / totalDays).coerceIn(0f, 1f)
        }

    val monthRemainingPercent: Int
        get() = ((1f - monthProgressDoneFraction) * 100f).toInt().coerceIn(0, 100)
}

class OrbitCalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: OrbitRepository = OrbitRepository(
        context = application.applicationContext,
        taskDao = OrbitDatabase.getInstance(application.applicationContext).taskDao()
    )

    private val _primarySection = MutableStateFlow(PrimaryAppSection.ORBIT_HOME)
    private val _calendarSubView = MutableStateFlow(CalendarSubView.WEEK)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _systemClockMinutes = MutableStateFlow(computeSystemMinutes())
    private val _isPaperOpen = MutableStateFlow(false)
    private val _taskDraft = MutableStateFlow(TaskDraftState())
    private val _activeQuestTaskId = MutableStateFlow<Long?>(null)
    private val _bannerMessage = MutableStateFlow<String?>(null)

    private data class InternalUiControllerState(
        val section: PrimaryAppSection,
        val subView: CalendarSubView,
        val selectedDate: LocalDate,
        val clockMin: Int,
        val isPaperOpen: Boolean,
        val draft: TaskDraftState,
        val activeQuestId: Long?,
        val banner: String?
    )

    private val controllerFlow = combine(
        _primarySection,
        _calendarSubView,
        _selectedDate,
        _systemClockMinutes,
        _isPaperOpen
    ) { section, subView, selectedDate, clockMin, isPaperOpen ->
        InternalUiControllerState(
            section = section,
            subView = subView,
            selectedDate = selectedDate,
            clockMin = clockMin,
            isPaperOpen = isPaperOpen,
            draft = TaskDraftState(),
            activeQuestId = null,
            banner = null
        )
    }.combine(_taskDraft) { state, draft ->
        state.copy(draft = draft)
    }.combine(_activeQuestTaskId) { state, questId ->
        state.copy(activeQuestId = questId)
    }.combine(_bannerMessage) { state, banner ->
        state.copy(banner = banner)
    }

    val uiState: StateFlow<OrbitUiState> = combine(
        controllerFlow,
        repository.allTasksFlow,
        repository.settingsFlow
    ) { ctrl, allTasks, settings ->
        val effectiveMinutes = if (settings.useSimulatedTime) {
            settings.simulatedMinutesFromMidnight
        } else {
            ctrl.clockMin
        }
        val dateIso = ctrl.selectedDate.toString()
        val dayTasks = allTasks
            .filter { it.dateIso == dateIso }
            .sortedWith(compareBy<TaskEntity> { it.startMinutes }.thenBy { it.id })
        val activeQuest = allTasks.firstOrNull { it.id == ctrl.activeQuestId }
        OrbitUiState(
            primarySection = ctrl.section,
            calendarSubView = ctrl.subView,
            selectedDate = ctrl.selectedDate,
            todayDate = LocalDate.now(),
            currentMinutesFromMidnight = effectiveMinutes,
            allTasks = allTasks,
            selectedDateTasks = dayTasks,
            pinnedReminders = repository.getPinnedReminders(settings.completedReminderIds),
            settings = settings,
            isPaperTaskCreatorOpen = ctrl.isPaperOpen,
            taskDraft = ctrl.draft,
            activeQuestPlanetTask = activeQuest,
            lastCreatedPlanetBanner = ctrl.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrbitUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded(LocalDate.now())
        }
        viewModelScope.launch {
            while (true) {
                _systemClockMinutes.value = computeSystemMinutes()
                delay(15_000L)
            }
        }
    }

    private fun computeSystemMinutes(): Int {
        val now = LocalTime.now()
        val raw = now.hour * 60 + now.minute
        // Keep the live meteor visibly inside the 07:00 - 22:00 working day track even if opened late at night
        return if (raw in (7 * 60)..(22 * 60)) raw else (14 * 60 + 41)
    }

    fun selectPrimarySection(section: PrimaryAppSection) {
        _primarySection.value = section
    }

    fun selectCalendarSubView(subView: CalendarSubView) {
        _calendarSubView.value = subView
        if (subView == CalendarSubView.TODAY) {
            // Ensure selectedDate stays anchored or jumps to current focus day
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun openTaskPaper(prefilledStartMinutes: Int? = null, prefilledDuration: Int? = null) {
        val defaultStart = prefilledStartMinutes
            ?: uiState.value.currentMinutesFromMidnight
                .let { ((it + 14) / 15) * 15 }
                .coerceIn(7 * 60, 21 * 60 + 30)
        _taskDraft.value = TaskDraftState(
            title = "",
            isTitleBarExpanded = false,
            activeCapsule = VernierCapsuleMode.TIME,
            startMinutes = defaultStart,
            durationMinutes = prefilledDuration ?: 45,
            selectedCategory = TaskCategory.WORK,
            selectedDifficulty = DifficultyTier.A
        )
        _isPaperOpen.value = true
    }

    fun closeTaskPaper() {
        _isPaperOpen.value = false
    }

    fun expandDraftTitleBar() {
        _taskDraft.update { it.copy(isTitleBarExpanded = true) }
    }

    fun updateDraftTitle(newTitle: String) {
        _taskDraft.update { it.copy(title = newTitle) }
    }

    fun selectDraftCapsule(mode: VernierCapsuleMode) {
        _taskDraft.update { it.copy(activeCapsule = mode) }
    }

    fun updateDraftStartMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(7 * 60, 22 * 60 - 15)
        _taskDraft.update { it.copy(startMinutes = clamped) }
    }

    fun updateDraftDurationMinutes(duration: Int) {
        val clamped = duration.coerceIn(15, 240)
        _taskDraft.update { it.copy(durationMinutes = clamped) }
    }

    fun selectDraftCategory(category: TaskCategory) {
        _taskDraft.update { it.copy(selectedCategory = category) }
    }

    fun selectDraftDifficulty(difficulty: DifficultyTier) {
        _taskDraft.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun commitNewTaskFromPaper() {
        val draft = _taskDraft.value
        val finalTitle = draft.title.trim().ifEmpty {
            "${draft.selectedCategory.label} // ${draft.selectedDifficulty.code}-QUEST"
        }
        val seed = abs(Random.nextInt(100, 9999))
        val (planetName, planetLore) = PlanetLoreGenerator.generatePlanetIdentity(
            seed = seed,
            category = draft.selectedCategory,
            difficulty = draft.selectedDifficulty,
            durationMinutes = draft.durationMinutes
        )
        val defaultSubtasks = listOf(
            SubtaskItem("sub_1", "Initialize ${draft.selectedCategory.label.lowercase()} phase", false),
            SubtaskItem("sub_2", "Execute core ${draft.durationMinutes}m objective", false),
            SubtaskItem("sub_3", "Log telemetry & complete orbit", false)
        )
        val entity = TaskEntity(
            title = finalTitle,
            dateIso = _selectedDate.value.toString(),
            startMinutes = draft.startMinutes.coerceIn(7 * 60, 22 * 60 - 15),
            durationMinutes = draft.durationMinutes,
            tagCategory = draft.selectedCategory.code,
            difficultyTier = draft.selectedDifficulty.code,
            isCompleted = false,
            isRecurring = false,
            subtasksRaw = TaskEntity.serializeSubtasks(defaultSubtasks),
            planetSeed = seed,
            planetName = planetName,
            planetCharacteristic = planetLore
        )
        viewModelScope.launch {
            repository.insertTask(entity)
            _isPaperOpen.value = false
            _bannerMessage.value = "ORBIT LOCKED: $planetName"
            delay(3200L)
            _bannerMessage.value = null
        }
    }

    fun openQuestPlanetModal(task: TaskEntity) {
        _activeQuestTaskId.value = task.id
    }

    fun closeQuestPlanetModal() {
        _activeQuestTaskId.value = null
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
        }
    }

    fun toggleSubtask(task: TaskEntity, subtaskId: String) {
        viewModelScope.launch {
            val updatedList = task.parseSubtasks().map { item ->
                if (item.id == subtaskId) item.copy(isCompleted = !item.isCompleted) else item
            }
            val allDone = updatedList.isNotEmpty() && updatedList.all { it.isCompleted }
            val updatedTask = task.copy(
                subtasksRaw = TaskEntity.serializeSubtasks(updatedList),
                isCompleted = if (allDone) true else task.isCompleted
            )
            repository.updateTask(updatedTask)
        }
    }

    fun rescheduleTaskTime(task: TaskEntity, newStartMinutes: Int) {
        val snapped = ((newStartMinutes + 7) / 15) * 15
        val clamped = snapped.coerceIn(7 * 60, 22 * 60 - 15)
        if (clamped == task.startMinutes) return
        viewModelScope.launch {
            repository.updateTask(task.copy(startMinutes = clamped))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            if (_activeQuestTaskId.value == task.id) {
                _activeQuestTaskId.value = null
            }
            repository.deleteTask(task)
        }
    }

    fun togglePinnedReminder(reminderId: String) {
        viewModelScope.launch {
            repository.togglePinnedReminder(reminderId)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticsEnabled(enabled)
        }
    }

    fun setSimulatedTime(enabled: Boolean, minutes: Int) {
        viewModelScope.launch {
            repository.setSimulatedTime(enabled, minutes)
        }
    }

    fun cycleVernierOffsetHours() {
        val current = uiState.value.settings.vernierOffsetHours
        val next = when (current) {
            3 -> 5
            5 -> 8
            8 -> -5
            -5 -> 0
            else -> 3
        }
        viewModelScope.launch {
            repository.setVernierOffsetHours(next)
        }
    }

    fun resetDemoSchedule() {
        viewModelScope.launch {
            repository.resetAndReseed(_selectedDate.value)
            _bannerMessage.value = "ORBIT MATRIX RE-SEEDED"
            delay(2500L)
            _bannerMessage.value = null
        }
    }
}
