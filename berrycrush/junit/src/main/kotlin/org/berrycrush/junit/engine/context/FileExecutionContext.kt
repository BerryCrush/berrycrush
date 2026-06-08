package org.berrycrush.junit.engine.context

import org.berrycrush.context.ExecutionContext
import org.berrycrush.executor.BerryCrushScenarioExecutor
import org.berrycrush.junit.engine.ClassTestDescriptor
import org.berrycrush.junit.engine.FeatureDescriptor
import org.berrycrush.junit.engine.IndividualScenarioDescriptor
import org.berrycrush.junit.engine.ScenarioFileDescriptor
import org.berrycrush.junit.engine.adapter.JUnitExecutionListenerAdapter
import org.berrycrush.model.Scenario
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestExecutionResult
import java.io.File

/**
 * Holds the execution context for a scenario file.
 */
internal data class FileExecutionContext(
    val executor: BerryCrushScenarioExecutor,
    val sharedContext: ExecutionContext?,
    val scenarioPath: String,
) {
    fun executeFileChildren(
        fileDescriptor: ScenarioFileDescriptor,
        classDescriptor: ClassTestDescriptor,
        listener: EngineExecutionListener,
    ): Boolean {
        check(fileDescriptor.children.isNotEmpty()) {
            "No scenarios found in ${fileDescriptor.scenarioPath}"
        }

        return fileDescriptor.children
            .map { child ->
                when (child) {
                    is IndividualScenarioDescriptor ->
                        executeScenario(child, classDescriptor, listener)
                    is FeatureDescriptor ->
                        executeFeature(child, classDescriptor, listener)
                    else -> false
                }
            }.any { it }
    }
}

private fun FileExecutionContext.executeFeature(
    featureDescriptor: FeatureDescriptor,
    classDescriptor: ClassTestDescriptor,
    listener: EngineExecutionListener,
): Boolean {
    listener.executionStarted(featureDescriptor)

    // Check if feature has its own shareVariablesAcrossScenarios setting
    val featureShareVariables =
        featureDescriptor.parameters["shareVariablesAcrossScenarios"] as? Boolean ?: false

    // Create context with feature parameters pre-loaded
    fun createFeatureContext(): ExecutionContext {
        val ctx = ExecutionContext()
        // Inject feature-level parameters into the context
        // This makes them available via context.allVariables() for multi-test config etc.
        featureDescriptor.parameters.forEach { (key, value) ->
            ctx[key] = value
        }
        return ctx
    }

    // Use feature-level shared context if enabled, otherwise fall back to file-level
    val effectiveContext =
        when {
            featureShareVariables && sharedContext == null -> {
                // Feature enables sharing, but file doesn't - create feature-level context
                copy(sharedContext = createFeatureContext())
            }
            !featureShareVariables && sharedContext != null -> {
                // Feature disables sharing while file enables it - use isolated context
                // Still inject feature parameters into a fresh context for the feature
                copy(sharedContext = createFeatureContext())
            }
            featureShareVariables -> {
                // Feature enables sharing, create separate context to isolate from other features
                copy(sharedContext = createFeatureContext())
            }
            else -> {
                // Use file-level context but with feature parameters
                // Create a new shared context with feature params if feature has any
                if (featureDescriptor.parameters.isNotEmpty()) {
                    copy(sharedContext = createFeatureContext())
                } else {
                    this
                }
            }
        }

    val hasFailure =
        featureDescriptor.children
            .filterIsInstance<IndividualScenarioDescriptor>()
            .map { effectiveContext.executeScenario(it, classDescriptor, listener) }
            .any { it }

    val result =
        if (hasFailure) {
            TestExecutionResult.failed(
                AssertionError("One or more scenarios in feature '${featureDescriptor.featureName}' failed"),
            )
        } else {
            TestExecutionResult.successful()
        }
    listener.executionFinished(featureDescriptor, result)

    return hasFailure
}

/**
 * Execute a single scenario and report results.
 * @return true if the scenario failed
 */
private fun FileExecutionContext.executeScenario(
    scenarioDescriptor: IndividualScenarioDescriptor,
    classDescriptor: ClassTestDescriptor,
    listener: EngineExecutionListener,
): Boolean {
    // Check if scenario should be skipped based on tags
    if (!classDescriptor.shouldExecuteScenario(scenarioDescriptor.scenario.tags)) {
        listener.executionStarted(scenarioDescriptor)
        listener.executionFinished(scenarioDescriptor, TestExecutionResult.aborted(null))
        return false
    }

    // Create execution listener adapter for real-time event reporting
    // This handles scenario start/end as well as auto-test events
    val executionListener = JUnitExecutionListenerAdapter(scenarioDescriptor, listener)

    val sourceFile = File(scenarioPath)

    return runCatching {
        // Create execution context - use shared context if available,
        // or create one for outline scenarios with examples
        val executionContext =
            sharedContext
                ?: if (scenarioDescriptor.scenario.examples?.isNotEmpty() == true) {
                    ExecutionContext()
                } else {
                    null
                }

        // Add example row values to context if this is an outline scenario
        initializeContext(scenarioDescriptor.scenario, executionContext)

        // Execute with execution listener for real-time event reporting
        // All JUnit events (scenario start/end, auto-test start/end) are handled by the listener
        executor.execute(
            scenarioDescriptor.scenario,
            executionContext,
            sourceFile,
            executionListener,
        )
    }.fold(
        onSuccess = { executionListener.hasFailure() },
        onFailure = { e ->
            // Ensure scenario is started before reporting failure
            if (!executionListener.scenarioStarted) {
                listener.executionStarted(scenarioDescriptor)
            }
            listener.executionFinished(scenarioDescriptor, TestExecutionResult.failed(e))
            true
        },
    )
}

/**
 * Initialize execution context with example row values for scenario outlines.
 * For non-outline scenarios, this is a no-op.
 */
private fun initializeContext(
    scenario: Scenario,
    context: ExecutionContext?,
) {
    context ?: return
    val examples = scenario.examples ?: return
    if (examples.isEmpty()) return

    // Use the first (and only) example row - outlines are expanded to one row per scenario
    val row = examples.first()
    row.values.forEach { (key, value) ->
        // Interpolate any variables in example values using existing context
        val resolvedValue =
            when (value) {
                is String -> context.interpolate(value)
                else -> value
            }
        context[key] = resolvedValue
    }
}
