package org.berrycrush.junit.engine.adapter

import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.MultiMode
import org.berrycrush.autotest.MultiTestResult
import org.berrycrush.exception.ErrorContextConfig
import org.berrycrush.executor.BerryCrushExecutionListener
import org.berrycrush.junit.engine.AutoTestDescriptor
import org.berrycrush.junit.engine.IndividualScenarioDescriptor
import org.berrycrush.junit.engine.MultiTestDescriptor
import org.berrycrush.model.AutoTestResult
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Scenario
import org.berrycrush.model.ScenarioResult
import org.berrycrush.model.Step
import org.berrycrush.model.StepResult
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestExecutionResult

/**
 * Adapter that bridges [BerryCrushExecutionListener] to JUnit's [EngineExecutionListener].
 *
 * This adapter centralizes all JUnit event reporting by implementing the
 * [BerryCrushExecutionListener] interface and firing corresponding JUnit events:
 * - [onScenarioStarting] → `executionStarted(scenarioDescriptor)`
 * - [onScenarioCompleted] → `executionFinished(scenarioDescriptor, result)`
 * - [onAutoTestStarting] → `dynamicTestRegistered`, `executionStarted`
 * - [onAutoTestCompleted] → `executionFinished`
 *
 * This allows IDEs like IntelliJ to show test output in real-time.
 */
internal class JUnitExecutionListenerAdapter(
    private val scenarioDescriptor: IndividualScenarioDescriptor,
    private val listener: EngineExecutionListener,
) : BerryCrushExecutionListener {
    private var testIndex = 0
    private val descriptors = mutableMapOf<AutoTestCase, AutoTestDescriptor>()
    private val multiDescriptors = mutableMapOf<MultiMode, MultiTestDescriptor>()
    private var autoTestFailureCount = 0
    private var multiTestFailureCount = 0
    private var lastScenarioResult: ScenarioResult? = null

    /**
     * Returns true if the scenario execution started.
     */
    var scenarioStarted = false
        private set

    /**
     * Returns true if there were any failures (auto-test, multi-test, or scenario).
     */
    fun hasFailure(): Boolean =
        autoTestFailureCount > 0 ||
            multiTestFailureCount > 0 ||
            lastScenarioResult?.status != ResultStatus.PASSED

    override fun onScenarioStarting(scenario: Scenario) {
        scenarioStarted = true
        listener.executionStarted(scenarioDescriptor)
    }

    override fun onScenarioCompleted(
        scenario: Scenario,
        result: ScenarioResult,
    ) {
        lastScenarioResult = result

        // Determine the appropriate result to report
        val autoTestResults = result.stepResults.flatMap { it.autoTestResults }
        val multiTestResults = result.stepResults.flatMap { it.multiTestResults }
        val totalFailures = autoTestFailureCount + multiTestFailureCount

        val testResult =
            when {
                // Auto-test or multi-test failures
                totalFailures > 0 -> {
                    val totalTests = autoTestResults.size + multiTestResults.size
                    TestExecutionResult.failed(
                        AssertionError("$totalFailures/$totalTests auto/multi-tests failed"),
                    )
                }
                // Scenario passed
                result.status == ResultStatus.PASSED -> {
                    TestExecutionResult.successful()
                }
                // Scenario skipped
                result.status == ResultStatus.SKIPPED -> {
                    TestExecutionResult.aborted(null)
                }
                // Scenario failed
                else -> {
                    val message = buildFailedStepsMessage(scenario.name, result)
                    TestExecutionResult.failed(AssertionError(message))
                }
            }

        listener.executionFinished(scenarioDescriptor, testResult)
    }

    override fun onStepStarting(step: Step) {
        // Step-level reporting not currently used in JUnit
        // Future: Could create step descriptors for fine-grained progress
    }

    override fun onStepCompleted(
        step: Step,
        result: StepResult,
    ) {
        // Step-level reporting not currently used in JUnit
    }

    override fun onAutoTestStarting(testCase: AutoTestCase) {
        val displayName = AutoTestDescriptor.createDisplayName(testCase)
        val testId = scenarioDescriptor.uniqueId.append("auto-test", "${++testIndex}")

        val descriptor =
            AutoTestDescriptor(
                uniqueId = testId,
                displayName = displayName,
                testCase = testCase,
                stepDescription = testCase.description,
            )

        // Register the dynamic test with JUnit
        scenarioDescriptor.addChild(descriptor)
        listener.dynamicTestRegistered(descriptor)

        // Start execution immediately for real-time output
        listener.executionStarted(descriptor)

        descriptors[testCase] = descriptor
    }

    override fun onAutoTestCompleted(
        testCase: AutoTestCase,
        result: AutoTestResult,
    ) {
        val descriptor =
            checkNotNull(descriptors[testCase]) {
                "onAutoTestCompleted called without matching onAutoTestStarting"
            }

        if (result.passed) {
            listener.executionFinished(descriptor, TestExecutionResult.successful())
        } else {
            autoTestFailureCount++
            val errorMessage = buildAutoTestFailureMessage(result)
            listener.executionFinished(
                descriptor,
                TestExecutionResult.failed(AssertionError(errorMessage)),
            )
        }
    }

    override fun onMultiTestStarting(
        mode: MultiMode,
        requestCount: Int,
    ) {
        val displayName = MultiTestDescriptor.createDisplayName(mode, requestCount)
        val testId = scenarioDescriptor.uniqueId.append("multi-test", "${++testIndex}")

        val descriptor =
            MultiTestDescriptor(
                uniqueId = testId,
                displayName = displayName,
                mode = mode,
                requestCount = requestCount,
            )

        // Register the dynamic test with JUnit
        scenarioDescriptor.addChild(descriptor)
        listener.dynamicTestRegistered(descriptor)

        // Start execution immediately for real-time output
        listener.executionStarted(descriptor)

        multiDescriptors[mode] = descriptor
    }

    override fun onMultiTestCompleted(result: MultiTestResult) {
        val descriptor =
            checkNotNull(multiDescriptors[result.mode]) {
                "onMultiTestCompleted called without matching onMultiTestStarting"
            }

        if (result.passed) {
            listener.executionFinished(descriptor, TestExecutionResult.successful())
        } else {
            multiTestFailureCount++
            val errorMessage = MultiTestDescriptor.buildFailureMessage(result)
            listener.executionFinished(
                descriptor,
                TestExecutionResult.failed(AssertionError(errorMessage)),
            )
        }
    }

    private fun buildAutoTestFailureMessage(autoResult: AutoTestResult): String =
        buildString {
            append(AutoTestDescriptor.createDisplayName(autoResult.testCase))
            append("\n")
            if (autoResult.error != null) {
                append("  Error: ${autoResult.error}")
            } else {
                append("  Status: ${autoResult.statusCode ?: "N/A"}")
                autoResult.assertionResults.filter { !it.passed }.forEach { assertion ->
                    append("\n  - ${assertion.message}")
                }
            }
            if (autoResult.responseBody != null) {
                append("\n  Response: ${autoResult.responseBody}")
            }
        }

    /**
     * Builds a formatted error message for failed steps in a scenario.
     */
    fun buildFailedStepsMessage(
        scenarioName: String,
        result: ScenarioResult,
    ): String {
        val failedSteps =
            result.stepResults
                .filter { it.status != ResultStatus.PASSED }
                .mapIndexed { index, step ->
                    val keyword =
                        step.step.type.name
                            .lowercase()
                    val locationInfo =
                        step.step.sourceLocation?.let { " at $it" } ?: ""
                    val header = "  Step ${index + 1} ($keyword): ${step.step.description}$locationInfo"
                    val details =
                        step.assertionResults
                            .filter { !it.passed }
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString("\n") { "      - ${it.message}" }
                            ?: "      - ${step.error?.message ?: "Assertion failed"}"

                    // Build HTTP context for failed steps with responses
                    val httpContext = buildHttpContext(step)

                    "$header\n$details$httpContext"
                }.joinToString("\n")

        return "Scenario '$scenarioName' failed:\n$failedSteps"
    }

    /**
     * Build HTTP response context for a failed step.
     */
    private fun buildHttpContext(step: StepResult): String {
        val statusCode = step.statusCode ?: return ""
        val responseBody = step.responseBody

        return buildString {
            append("\n      ━━━ HTTP Response ━━━")
            append("\n      Status: $statusCode")
            step.responseHeaders.forEach { (name, values) ->
                append("\n      $name: ${values.joinToString(", ")}")
            }
            if (responseBody != null) {
                val maxSize = ErrorContextConfig().maxBodySize
                val displayBody =
                    if (responseBody.length > maxSize) {
                        "${responseBody.take(maxSize)}... (truncated)"
                    } else {
                        responseBody
                    }
                append("\n      Body: $displayBody")
            }
        }
    }
}
