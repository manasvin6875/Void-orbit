package com.example.data

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.TimelineBurntOrange
import com.example.ui.theme.TimelineCerulean
import com.example.ui.theme.TimelineCoral
import com.example.ui.theme.TimelineLeafGreen
import com.example.ui.theme.TimelineWarmAmber
import kotlin.math.abs

enum class TaskCategory(
    val code: String,
    val label: String,
    val shortCode: String,
    val timelineColor: Color,
    val secondaryColor: Color,
    val biomeArchetype: String
) {
    WORK(
        code = "WORK",
        label = "DEEP WORK",
        shortCode = "WRK",
        timelineColor = TimelineCerulean,
        secondaryColor = Color(0xFF1F4E79),
        biomeArchetype = "CYBER-SILICA CORE"
    ),
    STUDY(
        code = "STUDY",
        label = "RESEARCH",
        shortCode = "STD",
        timelineColor = TimelineCerulean,
        secondaryColor = Color(0xFF2E75B6),
        biomeArchetype = "CRYSTALLINE ARCHIVE"
    ),
    FITNESS(
        code = "FITNESS",
        label = "TRAINING",
        shortCode = "FIT",
        timelineColor = TimelineLeafGreen,
        secondaryColor = Color(0xFF385723),
        biomeArchetype = "HIGH-GRAVITY BASALT"
    ),
    HEALTH(
        code = "HEALTH",
        label = "VITALITY",
        shortCode = "HLT",
        timelineColor = TimelineCoral,
        secondaryColor = Color(0xFF9E2A2B),
        biomeArchetype = "BIOLUMINESCENT OCEAN"
    ),
    CREATIVE(
        code = "CREATIVE",
        label = "SYNTH ART",
        shortCode = "CRT",
        timelineColor = TimelineWarmAmber,
        secondaryColor = Color(0xFFB45F06),
        biomeArchetype = "NEBULA CHROMOSPHERE"
    ),
    SOCIAL(
        code = "SOCIAL",
        label = "COMMS",
        shortCode = "SOC",
        timelineColor = TimelineLeafGreen,
        secondaryColor = Color(0xFF274E13),
        biomeArchetype = "TWIN-TIDAL RELAY"
    ),
    ERRANDS(
        code = "ERRANDS",
        label = "LOGISTICS",
        shortCode = "ERR",
        timelineColor = TimelineBurntOrange,
        secondaryColor = Color(0xFF783F04),
        biomeArchetype = "RUST-DUNE EXPANSE"
    ),
    FINANCE(
        code = "FINANCE",
        label = "LEDGER",
        shortCode = "FIN",
        timelineColor = TimelineWarmAmber,
        secondaryColor = Color(0xFF7F6000),
        biomeArchetype = "AURUM RINGVAULT"
    ),
    MIND(
        code = "MIND",
        label = "MINDFUL",
        shortCode = "MND",
        timelineColor = TimelineCoral,
        secondaryColor = Color(0xFF660000),
        biomeArchetype = "ZERO-KELVIN AETHER"
    ),
    GAMING(
        code = "GAMING",
        label = "ARCADE",
        shortCode = "GMG",
        timelineColor = TimelineBurntOrange,
        secondaryColor = Color(0xFF85200C),
        biomeArchetype = "PLASMA PULSAR WORLD"
    );

    companion object {
        fun fromCode(code: String): TaskCategory =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: WORK
    }
}

enum class DifficultyTier(
    val code: String,
    val title: String,
    val orbitXFraction: Float,
    val planetPixelGrid: Int,
    val hasRings: Boolean,
    val hasMoon: Boolean,
    val classification: String
) {
    C(
        code = "C",
        title = "C-TIER // ROUTINE",
        orbitXFraction = 0.22f,
        planetPixelGrid = 18,
        hasRings = false,
        hasMoon = false,
        classification = "CLASS-C DWARF PROTO-WORLD"
    ),
    B(
        code = "B",
        title = "B-TIER // STANDARD",
        orbitXFraction = 0.44f,
        planetPixelGrid = 22,
        hasRings = false,
        hasMoon = true,
        classification = "CLASS-B TERRESTRIAL EXOPLANET"
    ),
    A(
        code = "A",
        title = "A-TIER // DEMANDING",
        orbitXFraction = 0.67f,
        planetPixelGrid = 26,
        hasRings = true,
        hasMoon = false,
        classification = "CLASS-A RINGED SUPER-GIANT"
    ),
    S(
        code = "S",
        title = "S-TIER // CRITICAL",
        orbitXFraction = 0.86f,
        planetPixelGrid = 30,
        hasRings = true,
        hasMoon = true,
        classification = "CLASS-S SINGULARITY SOVEREIGN"
    );

    companion object {
        fun fromCode(code: String): DifficultyTier =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: B
    }
}

data class SubtaskItem(
    val id: String,
    val text: String,
    val isCompleted: Boolean
)

@Entity(tableName = "orbit_tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val dateIso: String, // YYYY-MM-DD
    val startMinutes: Int, // 07:00 = 420 .. 22:00 = 1320
    val durationMinutes: Int, // 0, 15, 30, 45, 60, 90, 120...
    val tagCategory: String,
    val difficultyTier: String,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val subtasksRaw: String = "", // format: "id|title|1;id2|title2|0"
    val planetSeed: Int = 101,
    val planetName: String = "XYLOS-01",
    val planetCharacteristic: String = "Dense silicate crust with crimson ion storms",
    val specialGlyph: String = "" // Optional override: "ALARM", "SUN", "PAW", "COFFEE", "SLEEP", "PHONE", "CAKE"
) {
    val categoryEnum: TaskCategory
        get() = TaskCategory.fromCode(tagCategory)

    val difficultyEnum: DifficultyTier
        get() = DifficultyTier.fromCode(difficultyTier)

    val endMinutes: Int
        get() = (startMinutes + durationMinutes).coerceAtMost(23 * 60 + 45)

    fun parseSubtasks(): List<SubtaskItem> {
        if (subtasksRaw.isBlank()) return emptyList()
        return subtasksRaw.split(";").mapNotNull { chunk ->
            val parts = chunk.split("|")
            if (parts.size >= 3) {
                SubtaskItem(
                    id = parts[0],
                    text = parts[1],
                    isCompleted = parts[2] == "1"
                )
            } else null
        }
    }

    companion object {
        fun serializeSubtasks(items: List<SubtaskItem>): String =
            items.joinToString(";") { "${it.id}|${it.text}|${if (it.isCompleted) "1" else "0"}" }
    }
}

data class PinnedReminderTile(
    val id: String,
    val title: String,
    val shortTitle: String,
    val twoLineTop: String,
    val twoLineBottom: String,
    val accentColor: Color,
    val progressArc: Float, // 0f..1f
    val glyphType: String, // "SNOWFLAKE", "PHONE", "CAKE"
    val statusDetail: String,
    val isDone: Boolean = false
)

object PlanetLoreGenerator {
    private val prefixes = listOf(
        "XYLOS", "KRYPT", "AETHER", "VORTEX", "NYX",
        "SOLARIS", "CHRONO", "ZETA", "ORION", "TALOS",
        "VALKYR", "HELIOS", "HYPERION", "NEXUS", "OBSIDIAN"
    )
    private val atmospheres = listOf(
        "Sub-zero nitrogen geysers & quantized silica rings",
        "High-density magnetosphere with crimson dot-matrix aurorae",
        "Synchronized tidal lock; tectonic crystal formations",
        "Supercritical neon cloud bands & low-orbit metallic moons",
        "Ancient basalt monoliths humming at 432Hz orbital resonance",
        "Bioluminescent spore oceans beneath a carbon-lattice sky",
        "Hyper-conductive iron core warping local gravity trajectories",
        "Diamond-rain stratosphere with dual retrograde satellites"
    )

    fun generatePlanetIdentity(
        seed: Int,
        category: TaskCategory,
        difficulty: DifficultyTier,
        durationMinutes: Int
    ): Pair<String, String> {
        val safeSeed = abs(seed)
        val prefix = prefixes[safeSeed % prefixes.size]
        val sectorNum = (safeSeed % 89) + 10
        val name = "$prefix-$sectorNum [${difficulty.code}·${category.shortCode}]"
        val atmo = atmospheres[(safeSeed + category.ordinal + difficulty.ordinal) % atmospheres.size]
        val gravity = when (difficulty) {
            DifficultyTier.C -> "0.6G"
            DifficultyTier.B -> "1.1G"
            DifficultyTier.A -> "2.4G"
            DifficultyTier.S -> "4.8G"
        }
        val characteristic = "${category.biomeArchetype} • MASS $gravity • ORBIT ${durationMinutes.coerceAtLeast(15)}M • $atmo"
        return name to characteristic
    }
}

object TimeFormatUtils {
    fun format24HourColon(minutesFromMidnight: Int): String {
        val clamped = minutesFromMidnight.coerceIn(0, 24 * 60 - 1)
        val h = clamped / 60
        val m = clamped % 60
        return "%02d:%02d".format(h, m)
    }

    fun format12HourAmPm(minutesFromMidnight: Int): String {
        val clamped = ((minutesFromMidnight % (24 * 60)) + (24 * 60)) % (24 * 60)
        val h24 = clamped / 60
        val m = clamped % 60
        val amPm = if (h24 < 12) "AM" else "PM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return "%02d:%02d %s".format(h12, m, amPm)
    }

    fun formatDurationReadable(durationMinutes: Int): String {
        if (durationMinutes <= 0) return "Point Event"
        val hrs = durationMinutes / 60
        val mins = durationMinutes % 60
        return when {
            hrs > 0 && mins > 0 -> "$hrs hr, $mins min"
            hrs > 0 -> "$hrs hr"
            else -> "$mins min"
        }
    }
}
