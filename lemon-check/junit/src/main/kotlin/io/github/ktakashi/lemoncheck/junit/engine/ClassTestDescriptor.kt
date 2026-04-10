package io.github.ktakashi.lemoncheck.junit.engine

import io.github.ktakashi.lemoncheck.junit.LemonCheckBindings
import io.github.ktakashi.lemoncheck.junit.LemonCheckConfiguration
import io.github.ktakashi.lemoncheck.junit.LemonCheckScenarios
import io.github.ktakashi.lemoncheck.junit.LemonCheckSpec
import io.github.ktakashi.lemoncheck.junit.LemonCheckTags
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource

/**
 * Test descriptor representing a test class annotated with @LemonCheckScenarios.
 *
 * This descriptor holds information about the test class including:
 * - The scenario file locations to search
 * - The custom bindings class (if configured)
 * - Configuration annotation settings
 * - Tag filtering configuration
 */
class ClassTestDescriptor(
    uniqueId: UniqueId,
    val testClass: Class<*>,
) : AbstractTestDescriptor(uniqueId, testClass.simpleName, ClassSource.from(testClass)) {
    /**
     * Scenario file location patterns from @LemonCheckScenarios annotation.
     */
    val locations: Array<out String>
        get() = testClass.getAnnotation(LemonCheckScenarios::class.java)?.locations ?: emptyArray()

    /**
     * Fragment file location patterns from @LemonCheckScenarios annotation.
     */
    val fragmentLocations: Array<out String>
        get() =
            testClass.getAnnotation(LemonCheckScenarios::class.java)?.fragments
                ?: emptyArray()

    /**
     * Optional custom bindings class from @LemonCheckConfiguration annotation.
     */
    val bindingsClass: Class<out LemonCheckBindings>?

    /**
     * OpenAPI spec path from configuration (if any).
     * Priority: @LemonCheckConfiguration.openApiSpec > @LemonCheckSpec.paths[0]
     */
    val openApiSpec: String?

    /**
     * Timeout in milliseconds for scenario execution.
     */
    val timeout: Long

    /**
     * Tags to include when filtering scenarios (empty means include all).
     */
    val includeTags: Set<String>

    /**
     * Tags to exclude when filtering scenarios.
     */
    val excludeTags: Set<String>

    init {
        val config = testClass.getAnnotation(LemonCheckConfiguration::class.java)
        val spec = testClass.getAnnotation(LemonCheckSpec::class.java)
        val tagsAnnotation = testClass.getAnnotation(LemonCheckTags::class.java)

        bindingsClass = config?.bindings?.java
        timeout = config?.timeout ?: 30_000L

        // OpenAPI spec: prefer @LemonCheckConfiguration, fallback to @LemonCheckSpec
        openApiSpec = config?.openApiSpec?.takeIf { it.isNotBlank() }
            ?: spec?.paths?.firstOrNull()

        // Tag filtering
        includeTags = tagsAnnotation?.include?.toSet() ?: emptySet()
        excludeTags = tagsAnnotation?.exclude?.toSet() ?: emptySet()
    }

    /**
     * Checks if a scenario should be executed based on tag filtering.
     *
     * A scenario is executed if:
     * - It doesn't have any excluded tags
     * - If includeTags is specified, the scenario must have at least one of them
     *
     * @param scenarioTags The tags on the scenario
     * @return true if the scenario should be executed
     */
    fun shouldExecuteScenario(scenarioTags: Set<String>): Boolean {
        // Check if scenario has any excluded tags
        if (excludeTags.isNotEmpty() && scenarioTags.any { it in excludeTags }) {
            return false
        }

        // If include tags are specified, scenario must have at least one
        if (includeTags.isNotEmpty() && scenarioTags.none { it in includeTags }) {
            return false
        }

        return true
    }

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER
}
