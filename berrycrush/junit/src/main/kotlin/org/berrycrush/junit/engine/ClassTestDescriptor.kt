package org.berrycrush.junit.engine

import org.berrycrush.junit.BerryCrushBindings
import org.berrycrush.junit.BerryCrushConfiguration
import org.berrycrush.junit.BerryCrushScenarios
import org.berrycrush.junit.BerryCrushSpec
import org.berrycrush.junit.BerryCrushSpecs
import org.berrycrush.junit.BerryCrushTags
import org.berrycrush.junit.ParallelExecutionMode
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations

/**
 * Test descriptor representing a test class annotated with @BerryCrushScenarios.
 *
 * This descriptor holds information about the test class including:
 * - The scenario file locations to search
 * - The custom bindings class (if configured)
 * - Configuration annotation settings
 * - Tag filtering configuration
 */
class ClassTestDescriptor(
    uniqueId: UniqueId,
    val testClass: KClass<*>,
) : AbstractTestDescriptor(uniqueId, testClass.java.simpleName, ClassSource.from(testClass.java)) {
    companion object {
        /** Default timeout in milliseconds for scenario execution. */
        const val DEFAULT_TIMEOUT_MS = 30_000L

        /** Default spec name. */
        const val DEFAULT_SPEC_NAME = BerryCrushBindings.DEFAULT_BINDING_NAME
    }

    /**
     * Scenario file location patterns from @BerryCrushScenarios annotation.
     */
    val locations: Array<out String>
        get() = testClass.findAnnotation<BerryCrushScenarios>()?.locations ?: emptyArray()

    /**
     * Fragment file location patterns from @BerryCrushScenarios annotation.
     */
    val fragmentLocations: Array<out String>
        get() =
            testClass.findAnnotation<BerryCrushScenarios>()?.fragments
                ?: emptyArray()

    /**
     * Optional custom bindings class from @BerryCrushConfiguration annotation.
     */
    val bindingsClass: Class<out BerryCrushBindings>?

    /**
     * OpenAPI spec path from @BerryCrushSpec annotation (if any).
     * Priority:
     * 1. @BerryCrushSpec with name="default"
     * 2. First @BerryCrushSpec
     */
    val openApiSpec: String?

    /**
     * All OpenAPI specs from @BerryCrushSpec annotations.
     * Key is the spec name, value is the spec annotation.
     */
    val specs: Map<String, BerryCrushSpec>

    /**
     * Timeout in milliseconds for scenario execution.
     */
    val timeout: Long

    /**
     * Parallel execution mode for scenarios in this class.
     *
     * Controls whether scenarios can run concurrently when JUnit parallel
     * execution is enabled. See [ParallelExecutionMode] for details.
     */
    val parallelExecution: ParallelExecutionMode

    /**
     * Tags to include when filtering scenarios (empty means include all).
     */
    val includeTags: Set<String>

    /**
     * Tags to exclude when filtering scenarios.
     */
    val excludeTags: Set<String>

    init {
        val config = testClass.findAnnotation<BerryCrushConfiguration>()
        val tagsAnnotation = testClass.findAnnotation<BerryCrushTags>()

        bindingsClass = config?.bindings?.java
        timeout = config?.timeout ?: DEFAULT_TIMEOUT_MS
        parallelExecution = config?.parallelExecution ?: ParallelExecutionMode.CONCURRENT

        // Collect all @BerryCrushSpec annotations (supports repeatable)
        specs = collectSpecs()

        // OpenAPI spec resolution priority:
        // 1. @BerryCrushSpec with name="default"
        // 2. First @BerryCrushSpec
        openApiSpec = resolveDefaultOpenApiSpec()

        // Tag filtering
        includeTags = tagsAnnotation?.include?.toSet() ?: emptySet()
        excludeTags = tagsAnnotation?.exclude?.toSet() ?: emptySet()
    }

    /**
     * Collects all @BerryCrushSpec annotations from the test class.
     * Supports both single and repeatable annotations via @BerryCrushSpecs container.
     */
    private fun collectSpecs(): Map<String, BerryCrushSpec> {
        val result = mutableMapOf<String, BerryCrushSpec>()

        // Check for container annotation (@BerryCrushSpecs)
        testClass.findAnnotation<BerryCrushSpecs>()?.value?.forEach { spec ->
            result[spec.name] = spec
        }

        // Check for @BerryCrushSpec
        testClass.findAnnotations<BerryCrushSpec>().forEach { spec ->
            if (spec.name !in result) {
                result[spec.name] = spec
            }
        }

        return result
    }

    /**
     * Resolves the default OpenAPI spec path with the following priority:
     * 1. @BerryCrushSpec with name="default"
     * 2. First @BerryCrushSpec
     */
    private fun resolveDefaultOpenApiSpec(): String? {
        // Priority 1: @BerryCrushSpec with name="default"
        specs[DEFAULT_SPEC_NAME]?.paths?.firstOrNull()?.let { return it }

        // Priority 2: First @BerryCrushSpec
        return specs.values
            .firstOrNull()
            ?.paths
            ?.firstOrNull()
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
