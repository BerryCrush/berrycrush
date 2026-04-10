package io.github.ktakashi.lemoncheck.junit.engine

import io.github.ktakashi.lemoncheck.model.Scenario
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.UriSource
import java.net.URL

/**
 * Test descriptor representing a .scenario file (container for individual scenarios).
 *
 * Each scenario file discovered from the locations specified in
 * @LemonCheckScenarios becomes a ScenarioFileDescriptor, which contains
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
 * steps that run before each scenario in the feature.
 *
 * In JUnit reports, this appears as a nested container within the file.
 */
class FeatureDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    val featureName: String,
) : AbstractTestDescriptor(uniqueId, displayName) {
    /**
     * Container type allows this descriptor to have child scenario descriptors.
     */
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER
}

/**
 * Test descriptor representing a single scenario within a .scenario file.
 *
 * Each scenario defined in a scenario file becomes an IndividualScenarioDescriptor.
 * This is a leaf node (TEST type) that represents an actual test case.
 *
 * In JUnit reports, this appears as an individual test case.
 */
class IndividualScenarioDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    val scenario: Scenario,
) : AbstractTestDescriptor(uniqueId, displayName) {
    /**
     * TEST type indicates this is a leaf test case.
     */
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST
}
