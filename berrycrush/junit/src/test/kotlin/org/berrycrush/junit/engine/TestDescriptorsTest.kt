package org.berrycrush.junit.engine

import org.berrycrush.junit.ScenarioTest
import org.berrycrush.model.Scenario
import org.berrycrush.model.SourceLocation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestDescriptorsTest {
    @Test
    fun `scenario file descriptor should be a container`() {
        val resource = requireNotNull(javaClass.getResource("/scenarios/simple.scenario"))
        val descriptor =
            ScenarioFileDescriptor(
                uniqueId = UniqueId.forEngine("berrycrush").append("file", "simple"),
                displayName = "simple.scenario",
                scenarioPath = "scenarios/simple.scenario",
                scenarioSource = resource,
            )

        assertEquals(TestDescriptor.Type.CONTAINER, descriptor.type)
        assertEquals("scenarios/simple.scenario", descriptor.scenarioPath)
    }

    @Test
    fun `feature descriptor test source should handle existing and missing files`() {
        val tempFile =
            kotlin.io.path
                .createTempFile("feature", ".scenario")
                .toFile()
                .apply { deleteOnExit() }
        val sourceLocation = SourceLocation(line = 3, column = 2)

        val sourceWithLocation = FeatureDescriptor.createTestSource(tempFile, sourceLocation)
        val sourceWithoutLocation = FeatureDescriptor.createTestSource(tempFile, null)
        val sourceWithMissingFile = FeatureDescriptor.createTestSource(File("does-not-exist.scenario"), sourceLocation)
        val sourceWithNullFile = FeatureDescriptor.createTestSource(null, sourceLocation)

        assertNotNull(sourceWithLocation)
        assertNotNull(sourceWithoutLocation)
        assertNull(sourceWithMissingFile)
        assertNull(sourceWithNullFile)
    }

    @Test
    fun `individual scenario descriptor should expose correct test types`() {
        val scenario = Scenario(name = "descriptor scenario")

        val regular =
            IndividualScenarioDescriptor(
                uniqueId = UniqueId.forEngine("berrycrush").append("scenario", "regular"),
                displayName = "regular",
                scenario = scenario,
            )
        val withAutoTests =
            IndividualScenarioDescriptor(
                uniqueId = UniqueId.forEngine("berrycrush").append("scenario", "auto"),
                displayName = "auto",
                scenario = scenario,
            )

        assertEquals(TestDescriptor.Type.TEST, regular.type)
        assertEquals(TestDescriptor.Type.TEST, withAutoTests.type)
    }

    @Test
    fun `individual scenario descriptor test source should handle file states`() {
        val tempFile =
            kotlin.io.path
                .createTempFile("scenario", ".scenario")
                .toFile()
                .apply { deleteOnExit() }
        val sourceLocation = SourceLocation(line = 6, column = 1)

        val sourceWithLocation = IndividualScenarioDescriptor.createTestSource(tempFile, sourceLocation)
        val sourceWithoutLocation = IndividualScenarioDescriptor.createTestSource(tempFile, null)
        val sourceMissingFile = IndividualScenarioDescriptor.createTestSource(File("missing.scenario"), sourceLocation)
        val sourceNullFile = IndividualScenarioDescriptor.createTestSource(null, sourceLocation)

        assertNotNull(sourceWithLocation)
        assertNotNull(sourceWithoutLocation)
        assertNull(sourceMissingFile)
        assertNull(sourceNullFile)
    }

    @Test
    fun `scenario method discoverer should skip disabled classes`() {
        val engineDescriptor = object : EngineDescriptor(UniqueId.forEngine("berrycrush"), "engine") {}

        ScenarioMethodDiscoverer.discoverScenariosForClass(engineDescriptor, DisabledScenarioClass::class)

        assertTrue(engineDescriptor.children.isEmpty())
    }

    @Test
    fun `scenario method discoverer should reuse existing class descriptor`() {
        val engineDescriptor = object : EngineDescriptor(UniqueId.forEngine("berrycrush"), "engine") {}
        val classDescriptor =
            ClassTestDescriptor(
                uniqueId = engineDescriptor.uniqueId.append("class", DiscovererScenarioClass::class.java.name),
                testClass = DiscovererScenarioClass::class,
            )
        engineDescriptor.addChild(classDescriptor)

        ScenarioMethodDiscoverer.discoverScenariosForClass(engineDescriptor, DiscovererScenarioClass::class)

        assertEquals(1, engineDescriptor.children.filterIsInstance<ClassTestDescriptor>().size)
        assertTrue(classDescriptor.children.filterIsInstance<ScenarioMethodDescriptor>().isNotEmpty())
    }
}

@Disabled("for discoverer branch coverage")
private class DisabledScenarioClass {
    @ScenarioTest
    fun shouldNotBeDiscovered(): Scenario = Scenario(name = "disabled")
}

private class DiscovererScenarioClass {
    @ScenarioTest
    fun validScenario(): Scenario = Scenario(name = "discoverer")
}
