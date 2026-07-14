package org.berrycrush.junit.engine.context

import org.berrycrush.context.ExecutionContext
import org.berrycrush.context.propagate
import org.berrycrush.junit.engine.ClassTestDescriptor
import org.berrycrush.junit.engine.ContainerDescriptor
import org.berrycrush.junit.engine.FeatureDescriptor
import org.berrycrush.junit.engine.IndividualScenarioDescriptor
import org.berrycrush.junit.engine.ScenarioFileDescriptor
import org.berrycrush.junit.engine.adapter.JUnitExecutionListenerAdapter
import org.berrycrush.runner.ScenarioRunner
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import java.io.File

/**
 * Holds the execution context for a scenario file.
 */
internal data class FileExecutionContext(
    val runner: ScenarioRunner,
    val fileContext: ExecutionContext,
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
            .map { child -> dispatchExecution(child, classDescriptor, listener, fileContext) }
            .any { it }
    }
}

private fun FileExecutionContext.dispatchExecution(
    child: TestDescriptor,
    classDescriptor: ClassTestDescriptor,
    listener: EngineExecutionListener,
    context: ExecutionContext,
): Boolean =
    when (child) {
        is IndividualScenarioDescriptor ->
            executeScenario(child, classDescriptor, listener, context)

        is FeatureDescriptor ->
            executeFeature(child, classDescriptor, listener, context)

        is ContainerDescriptor -> {
            listener.executionStarted(child)
            val hasFailure =
                child.children
                    .map { dispatchExecution(it, classDescriptor, listener, context) }
                    .any { it }
            val testExecutionResult =
                if (!hasFailure) {
                    TestExecutionResult.successful()
                } else {
                    TestExecutionResult.failed(
                        AssertionError("One or more scenarios in example '${child.displayName}' failed"),
                    )
                }
            listener.executionFinished(child, testExecutionResult)
            hasFailure
        }
        else -> false
    }

private fun FileExecutionContext.executeFeature(
    featureDescriptor: FeatureDescriptor,
    classDescriptor: ClassTestDescriptor,
    listener: EngineExecutionListener,
    parentContext: ExecutionContext,
): Boolean {
    listener.executionStarted(featureDescriptor)

    // Check if feature has its own shareVariablesAcrossScenarios setting
    val featureShareVariables =
        featureDescriptor.parameters["shareVariablesAcrossScenarios"] as? Boolean ?: false

    val featureContext = ExecutionContext(featureShareVariables, featureDescriptor.parameters, parentContext)
    val hasFailure =
        featureDescriptor.children
            .map { child -> dispatchExecution(child, classDescriptor, listener, featureContext) }
            .any { it }
    if (!hasFailure) {
        parentContext.propagate(featureContext)
    }

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
    parentContext: ExecutionContext,
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
    val scenarioContext =
        ExecutionContext(parentContext.shareVariablesAcrossScenarios, scenarioDescriptor.scenario.parameters, parentContext)
    return runCatching {
        // Execute with execution listener for real-time event reporting
        // All JUnit events (scenario start/end, auto-test start/end) are handled by the listener
        runner.executeScenario(
            scenarioDescriptor.scenario,
            sourceFile,
            executionListener,
            scenarioContext,
        )
    }.fold(
        onSuccess = {
            parentContext.propagate(scenarioContext)
            executionListener.hasFailure()
        },
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
