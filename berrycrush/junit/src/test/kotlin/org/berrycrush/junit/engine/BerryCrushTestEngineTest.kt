package org.berrycrush.junit.engine

import org.berrycrush.junit.discovery.ScenarioDiscovery
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for BerryCrushTestEngine.
 */
class BerryCrushTestEngineTest {
    @Test
    fun `engine has correct ID`() {
        val engine = BerryCrushTestEngine()
        assertEquals("berrycrush", engine.id)
    }

    @Test
    fun `scenario discovery finds scenario files`() {
        val classLoader = Thread.currentThread().contextClassLoader
        val scenarios =
            ScenarioDiscovery.discoverScenarios(
                classLoader,
                arrayOf("scenarios/*.scenario"),
            )

        assertTrue(scenarios.isNotEmpty(), "Should discover at least one scenario file")
        assertTrue(
            scenarios.any { it.name == "simple.scenario" },
            "Should find simple.scenario",
        )
    }
}
