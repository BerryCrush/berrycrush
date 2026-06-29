package org.berrycrush.junit.engine.context

import org.berrycrush.assertion.AssertionRegistry
import org.berrycrush.context.ExecutionContext
import org.berrycrush.dsl.BerryCrushSuite
import org.berrycrush.executor.BerryCrushConfigurationProvider
import org.berrycrush.junit.BerryCrushBindings
import org.berrycrush.junit.ParallelExecutionMode
import org.berrycrush.junit.engine.ClassTestDescriptor
import org.berrycrush.junit.engine.ScenarioFileDescriptor
import org.berrycrush.junit.engine.ScenarioMethodDescriptor
import org.berrycrush.junit.engine.ScenarioTestDiscoverer
import org.berrycrush.junit.spi.BindingsProvider
import org.berrycrush.model.FragmentRegistry
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.ScenarioResult
import org.berrycrush.plugin.PluginRegistry
import org.berrycrush.runner.ScenarioRunner
import org.berrycrush.scenario.ScenarioLoader
import org.berrycrush.util.StepRegistry
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestExecutionResult
import java.util.logging.Logger
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName

private val logger = Logger.getLogger(TestExecutionContext::class.java.name)

/**
 * Holds the execution context for a test class.
 */
internal data class TestExecutionContext(
    val suite: BerryCrushSuite,
    val bindings: BerryCrushBindings,
    val pluginRegistry: PluginRegistry,
    val fragmentRegistry: FragmentRegistry,
    val stepRegistry: StepRegistry?,
    val assertionRegistry: AssertionRegistry?,
    val runner: ScenarioRunner,
) {
    fun beginExecution() = runner.beginExecution()

    fun endExecution() = runner.endExecution()

    /**
     * Execute @Scenario methods.
     */
    fun executeScenarioMethods(
        classDescriptor: ClassTestDescriptor,
        listener: EngineExecutionListener,
        provider: BindingsProvider?,
    ) {
        classDescriptor.children
            .filterIsInstance<ScenarioMethodDescriptor>()
            .forEach { scenarioDescriptor ->
                executeScenarioMethod(scenarioDescriptor, classDescriptor, listener, provider)
            }
    }

    fun executeFileDescriptors(
        classDescriptor: ClassTestDescriptor,
        listener: EngineExecutionListener,
    ) {
        classDescriptor.children
            .filterIsInstance<ScenarioFileDescriptor>()
            .forEach { fileDescriptor ->
                executeFileDescriptor(fileDescriptor, classDescriptor, listener)
            }
    }
}

/**
 * Execute a single @Scenario method.
 */
private fun TestExecutionContext.executeScenarioMethod(
    scenarioDescriptor: ScenarioMethodDescriptor,
    classDescriptor: ClassTestDescriptor,
    listener: EngineExecutionListener,
    provider: BindingsProvider?,
) {
    listener.executionStarted(scenarioDescriptor)
    runCatching {
        // Create test instance - use Spring-managed instance if available
        val testInstance =
            provider?.createTestInstance(classDescriptor.testClass.java)
                ?: classDescriptor.testClass.primaryConstructor?.call()
                ?: throw IllegalStateException("Test class ${classDescriptor.testClass.simpleName} not found")

        // Invoke the @Scenario method to get the Scenario
        val scenario = scenarioDescriptor.invokeMethod(testInstance, suite)

        // Check if scenario should be skipped based on tags
        if (!classDescriptor.shouldExecuteScenario(scenario.tags)) {
            ScenarioResult(scenario, ResultStatus.SKIPPED)
        } else {
            // Execute the scenario
            runner.executeScenario(scenario)
        }
    }.onSuccess { result ->
        val testResult =
            when (result.status) {
                ResultStatus.PASSED -> TestExecutionResult.successful()
                ResultStatus.SKIPPED -> TestExecutionResult.aborted(null)
                ResultStatus.FAILED -> result.failed("Scenario failed")
                ResultStatus.ERROR -> result.failed("Scenario got error")
                ResultStatus.PENDING -> TestExecutionResult.aborted(null)
            }
        listener.executionFinished(scenarioDescriptor, testResult)
    }.onFailure { e ->
        listener.executionFinished(scenarioDescriptor, TestExecutionResult.failed(e))
    }
}

private fun ScenarioResult.failed(msg: String): TestExecutionResult {
    val error = stepResults.lastOrNull { it.status == status }?.error ?: AssertionError(msg)
    return TestExecutionResult.failed(error)
}

private fun TestExecutionContext.executeFileDescriptor(
    fileDescriptor: ScenarioFileDescriptor,
    classDescriptor: ClassTestDescriptor,
    listener: EngineExecutionListener,
) {
    listener.executionStarted(fileDescriptor)

    runCatching {
        val fileContext = buildFileContext(fileDescriptor, classDescriptor)
        fileContext.executeFileChildren(fileDescriptor, classDescriptor, listener)
    }.onSuccess { hasFailure ->
        val testResult =
            if (hasFailure) {
                TestExecutionResult.failed(AssertionError("One or more scenarios failed"))
            } else {
                TestExecutionResult.successful()
            }
        listener.executionFinished(fileDescriptor, testResult)
    }.onFailure { e ->
        listener.executionFinished(fileDescriptor, TestExecutionResult.failed(e))
    }
}

private fun TestExecutionContext.buildFileContext(
    fileDescriptor: ScenarioFileDescriptor,
    classDescriptor: ClassTestDescriptor,
): FileExecutionContext {
    val scenarioLoader = ScenarioLoader()
    val fileContent = ScenarioTestDiscoverer.loadScenarioFromUrl(scenarioLoader, fileDescriptor.scenarioSource)

    val fileConfig =
        if (fileContent.parameters.isNotEmpty()) {
            suite.configuration.withParameters(fileContent.parameters)
        } else {
            suite.configuration
        }
    val executionContext = ExecutionContext(fileConfig.shareVariablesAcrossScenarios, fileContent.parameters)
    val newRunner = runner.from(BerryCrushConfigurationProvider.from(fileConfig))

    // Warn if sharing variables across scenarios with concurrent execution mode
    if (classDescriptor.parallelExecution == ParallelExecutionMode.CONCURRENT) {
        logger.warning {
            "File '${fileDescriptor.scenarioPath}' uses shareVariablesAcrossScenarios=true " +
                "but test class '${classDescriptor.testClass.jvmName}' has CONCURRENT parallel execution. " +
                "Consider using @BerryCrushConfiguration(parallelExecution = SAME_THREAD) " +
                "to ensure scenarios execute sequentially."
        }
    }

    return FileExecutionContext(
        runner = newRunner,
        fileContext = executionContext,
        scenarioPath = fileDescriptor.scenarioPath,
    )
}
