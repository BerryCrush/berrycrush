package org.berrycrush.junit.engine

import org.berrycrush.model.Scenario
import org.berrycrush.scenario.SourceLocation
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import org.junit.platform.engine.support.descriptor.UriSource
import java.io.File
import java.net.URL

/**
 * Test descriptor representing a .scenario file (container for individual scenarios).
 *
 * Each scenario file discovered from the locations specified in
 * @BerryCrushScenarios becomes a ScenarioFileDescriptor, which contains
 * individual [IndividualScenarioDescriptor] children for each scenario in the file,
 * or [FeatureDescriptor] children for feature blocks.
 *
 * In JUnit reports, this appears as a test suite within the class test suite.
 */
class ScenarioFileDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    val scenarioPath: String,
    val scenarioSource: URL,
) : AbstractTestDescriptor(uniqueId, displayName, UriSource.from(scenarioSource.toURI())) {
    /**
     * Container type allows this descriptor to have child test descriptors.
     */
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER
}

/**
 * Test descriptor representing a feature block within a .scenario file.
 *
 * Feature blocks group related scenarios together and support background
 * steps that run before each scenario in the feature. Features can have
 * their own parameters that override file-level parameters.
 *
 * The descriptor includes source location information to enable IDE navigation
 * from test results to the feature definition in the source file.
 *
 * In JUnit reports, this appears as a nested container within the file.
 */
class FeatureDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    val featureName: String,
    val parameters: Map<String, Any> = emptyMap(),
    private val testSource: TestSource? = null,
) : AbstractTestDescriptor(uniqueId, displayName, testSource) {
    /**
     * Container type allows this descriptor to have child scenario descriptors.
     */
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER

    companion object {
        /**
         * Create a FileSource with position for IDE navigation.
         *
         * @param scenarioFile The scenario file containing this feature
         * @param sourceLocation The source location (line, column) of the feature
         * @return TestSource for JUnit Platform navigation, or null if not available
         */
        fun createTestSource(
            scenarioFile: File?,
            sourceLocation: org.berrycrush.scenario.SourceLocation?,
        ): TestSource? {
            scenarioFile ?: return null
            if (!scenarioFile.exists()) return null

            return sourceLocation?.let { loc ->
                FileSource.from(scenarioFile, FilePosition.from(loc.line, loc.column))
            } ?: FileSource.from(scenarioFile)
        }
    }
}

/**
 * Test descriptor representing a single scenario within a .scenario file.
 *
 * Each scenario defined in a scenario file becomes an IndividualScenarioDescriptor.
 * This can be either a leaf test or a container for auto-tests.
 * When auto-tests are present, it becomes a container with child tests.
 *
 * The descriptor includes source location information to enable IDE navigation
 * from test results to the scenario definition in the source file.
 *
 * In JUnit reports, this appears as an individual test case or container.
 */
class IndividualScenarioDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    val scenario: Scenario,
    val hasAutoTests: Boolean = false,
    private val testSource: TestSource? = null,
) : AbstractTestDescriptor(uniqueId, displayName, testSource) {
    /**
     * For auto-test scenarios, use CONTAINER_AND_TEST to hold child tests.
     * For regular scenarios, use TEST for simpler IDE handling.
     */
    override fun getType(): TestDescriptor.Type = if (hasAutoTests) TestDescriptor.Type.CONTAINER_AND_TEST else TestDescriptor.Type.TEST

    companion object {
        /**
         * Create a FileSource with position for IDE navigation.
         *
         * @param scenarioFile The scenario file containing this scenario
         * @param sourceLocation The source location (line, column) of the scenario
         * @return TestSource for JUnit Platform navigation, or null if not available
         */
        fun createTestSource(
            scenarioFile: File?,
            sourceLocation: SourceLocation?,
        ): TestSource? {
            scenarioFile ?: return null
            if (!scenarioFile.exists()) {
                return null
            }

            return sourceLocation?.let { loc ->
                FileSource.from(scenarioFile, FilePosition.from(loc.line, loc.column))
            } ?: FileSource.from(scenarioFile)
        }
    }
}
