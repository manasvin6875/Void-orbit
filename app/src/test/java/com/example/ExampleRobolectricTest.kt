package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DifficultyTier
import com.example.data.PlanetLoreGenerator
import com.example.data.TaskCategory
import com.example.data.TimeFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and verify N-ORBIT domain logic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("N-ORBIT", appName)

        // Verify 10 distinct task categories
        assertEquals(10, TaskCategory.entries.size)

        // Verify C to S difficulty tiers increase orbital X-axis fraction monotonically
        val tiers = DifficultyTier.entries
        assertEquals(4, tiers.size)
        assertTrue(tiers[0].orbitXFraction < tiers[1].orbitXFraction)
        assertTrue(tiers[1].orbitXFraction < tiers[2].orbitXFraction)
        assertTrue(tiers[2].orbitXFraction < tiers[3].orbitXFraction)

        // Verify procedural planet lore generator
        val (planetName, lore) = PlanetLoreGenerator.generatePlanetIdentity(
            seed = 777,
            category = TaskCategory.WORK,
            difficulty = DifficultyTier.S,
            durationMinutes = 90
        )
        assertTrue(planetName.contains("[S·WRK]"))
        assertTrue(lore.contains("90M"))

        // Verify time formatting
        assertEquals("08:30", TimeFormatUtils.format24HourColon(8 * 60 + 30))
        assertEquals("12:41 PM", TimeFormatUtils.format12HourAmPm(12 * 60 + 41))
    }
}
