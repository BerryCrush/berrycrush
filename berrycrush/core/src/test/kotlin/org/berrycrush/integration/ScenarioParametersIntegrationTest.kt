package org.berrycrush.integration

import org.berrycrush.scenario.ScenarioLoader
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for scenario-level parameters feature.
 *
 * Tests that parameters defined at scenario, feature, and file levels
 * are correctly parsed, merged, and accessible.
 */
class ScenarioParametersIntegrationTest {
    private val loader = ScenarioLoader()

    private fun getResourcePath(name: String): java.nio.file.Path {
        val url =
            javaClass.getResource("/scenarios/$name")
                ?: error("Resource not found: /scenarios/$name")
        return Paths.get(url.toURI())
    }

    @Test
    fun `should load file with scenario-level parameters`() {
        val path = getResourcePath("valid/scenario-parameters.scenario")
        val content = loader.loadFileContent(path)

        // File-level parameters
        assertEquals(true, content.parameters["shareVariablesAcrossScenarios"])
        assertEquals(30L, content.parameters["timeout"])

        // Feature group
        assertEquals(1, content.features.size)
        val feature = content.features[0]
        assertEquals("Pet API with parameters", feature.name)
        assertEquals("staging", feature.parameters["environment"])
        assertEquals(60L, feature.parameters["timeout"])

        // Feature scenarios
        assertEquals(2, feature.scenarios.size)

        // First scenario has its own parameters that override feature parameters
        val createScenario = feature.scenarios[0]
        assertEquals("Create pet with custom timeout", createScenario.name)
        assertEquals(120L, createScenario.parameters["timeout"])
        assertEquals(3L, createScenario.parameters["retries"])
        assertEquals("staging", createScenario.parameters["environment"])

        // Second scenario inherits feature parameters
        val listScenario = feature.scenarios[1]
        assertEquals("List pets with default timeout", listScenario.name)
        assertEquals(60L, listScenario.parameters["timeout"])
        assertEquals("staging", listScenario.parameters["environment"])
    }

    @Test
    fun `should merge feature parameters with scenario parameters`() {
        val path = getResourcePath("valid/scenario-parameters.scenario")
        val scenarios = loader.loadScenariosFromFile(path)

        // Find the scenario with overridden timeout
        val createScenario = scenarios.find { it.name == "Create pet with custom timeout" }
        assertNotNull(createScenario)

        // Scenario timeout should override feature timeout
        assertEquals(120L, createScenario.parameters["timeout"])

        // Feature environment should be inherited
        assertEquals("staging", createScenario.parameters["environment"])
    }

    @Test
    fun `should load standalone outline with parameters`() {
        val path = getResourcePath("valid/scenario-parameters.scenario")
        val content = loader.loadFileContent(path)

        // Find the outline in standalone scenarios
        val outline = content.scenarios.find { it.name == "Test multiple pets with parameters" }
        assertNotNull(outline)

        // Should have examples
        assertNotNull(outline.examples)
        assertEquals(3, outline.examples.size)

        // Should have scenario parameters
        assertEquals(90L, outline.parameters["timeout"])
    }

    @Test
    fun `should preserve parameter hierarchy in all scenarios`() {
        val path = getResourcePath("valid/scenario-parameters.scenario")
        val allScenarios = loader.loadScenariosFromFile(path)

        // All scenarios from file: 2 from feature + 1 outline
        assertEquals(3, allScenarios.size)

        // Check that all feature scenarios have inherited the environment parameter
        val featureScenarios = allScenarios.filter { it.parameters.containsKey("environment") }
        assertEquals(2, featureScenarios.size)

        // All feature scenarios should have environment = staging
        assertTrue(
            featureScenarios.all { it.parameters["environment"] == "staging" },
            "All feature scenarios should inherit environment",
        )
    }

    @Test
    fun `should handle scenario without parameters in feature with parameters`() {
        val path = getResourcePath("valid/scenario-parameters.scenario")
        val scenarios = loader.loadScenariosFromFile(path)

        // Find the scenario that only inherits feature parameters
        val listScenario = scenarios.find { it.name == "List pets with default timeout" }
        assertNotNull(listScenario)

        // Should have inherited feature parameters
        assertTrue(listScenario.parameters.isNotEmpty())
        assertEquals("staging", listScenario.parameters["environment"])
        assertEquals(60L, listScenario.parameters["timeout"])
    }

    @Test
    fun `should preserve authored order for mixed top-level execution list`() {
        val source =
            """
            feature: Feature first
              scenario: Feature scenario
                when: run feature
                  call ^featureCall

            scenario: Standalone scenario
              when: run standalone
                call ^standaloneCall

            outline: Standalone outline
              when: run outline "<id>"
                call ^outlineCall
                  id: "<id>"
              examples:
                | id |
                | 1  |
            """.trimIndent()

        val scenarios = loader.loadScenariosFromString(source)

        assertEquals(
            listOf("Feature scenario", "Standalone scenario", "Standalone outline"),
            scenarios.map { it.name },
        )
    }
}
