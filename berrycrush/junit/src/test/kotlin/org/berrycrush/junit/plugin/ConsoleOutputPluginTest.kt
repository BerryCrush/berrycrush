package org.berrycrush.junit.plugin

import org.berrycrush.plugin.AssertionFailure
import org.berrycrush.plugin.ExecutionContext
import org.berrycrush.plugin.HttpResponse
import org.berrycrush.plugin.ResultStatus
import org.berrycrush.plugin.ScenarioContext
import org.berrycrush.plugin.ScenarioResult
import org.berrycrush.plugin.StepContext
import org.berrycrush.plugin.StepOperation
import org.berrycrush.plugin.StepResult
import org.berrycrush.plugin.StepType
import org.junit.jupiter.api.Test
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.mockito.Mockito.mock
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConsoleOutputPluginTest {
    @Test
    fun `plugin should print scenario details and track failures`() {
        val output = ByteArrayOutputStream()
        val plugin = ConsoleOutputPlugin(PrintStream(output))
        val scenarioContext = testScenarioContext("failed scenario")

        plugin.onScenarioStart(scenarioContext)
        plugin.onStepEnd(
            context = testStepContext(scenarioContext, "call pets", 500),
            result =
                testStepResult(
                    status = ResultStatus.FAILED,
                    error = IllegalStateException("backend unavailable"),
                    failure =
                        AssertionFailure(
                            message = "expected 200 but got 500",
                            stepDescription = "call pets",
                            assertionType = "status",
                        ),
                ),
        )
        plugin.onScenarioEnd(
            context = scenarioContext,
            result = testScenarioResult(ResultStatus.FAILED),
        )

        val console = output.toString(Charsets.UTF_8)
        assertTrue(console.contains("=== Scenario: failed scenario ==="))
        assertTrue(console.contains("HTTP Status: 500"))
        assertTrue(console.contains("expected 200 but got 500"))
        assertTrue(console.contains("Error: backend unavailable"))
        assertTrue(console.contains("Result: FAILED"))
        assertTrue(!plugin.isAllPassed())
        assertNotNull(plugin.getFailureReason())
    }

    @Test
    fun `reportResult should notify listener with successful result`() {
        val listener = RecordingEngineListener()
        val descriptor = EngineDescriptor(UniqueId.forEngine("berrycrush"), "suite")
        val plugin = ConsoleOutputPlugin(PrintStream(ByteArrayOutputStream()), listener, descriptor)

        plugin.onScenarioStart(testScenarioContext("passing scenario"))
        plugin.onScenarioEnd(
            context = testScenarioContext("passing scenario"),
            result = testScenarioResult(ResultStatus.PASSED),
        )
        plugin.reportResult()

        assertEquals(1, listener.finishedEvents.size)
        val finished = listener.finishedEvents.single()
        assertEquals(descriptor, finished.first)
        assertEquals(TestExecutionResult.Status.SUCCESSFUL, finished.second.status)
    }

    @Test
    fun `reportResult should notify listener with first failure`() {
        val listener = RecordingEngineListener()
        val descriptor = EngineDescriptor(UniqueId.forEngine("berrycrush"), "suite")
        val plugin = ConsoleOutputPlugin(PrintStream(ByteArrayOutputStream()), listener, descriptor)

        plugin.onScenarioStart(testScenarioContext("failing scenario"))
        plugin.onScenarioEnd(
            context = testScenarioContext("failing scenario"),
            result = testScenarioResult(ResultStatus.FAILED),
        )
        plugin.reportResult()

        assertEquals(1, listener.finishedEvents.size)
        assertEquals(
            TestExecutionResult.Status.FAILED,
            listener.finishedEvents
                .single()
                .second.status,
        )
        assertNotNull(
            listener.finishedEvents
                .single()
                .second.throwable
                .orElse(null),
        )
    }

    @Test
    fun `plugin should keep first failure reason across scenarios`() {
        val plugin = ConsoleOutputPlugin(PrintStream(ByteArrayOutputStream()))

        plugin.onScenarioStart(testScenarioContext("first"))
        plugin.onScenarioEnd(testScenarioContext("first"), testScenarioResult(ResultStatus.FAILED))
        val firstReason = plugin.getFailureReason()

        plugin.onScenarioStart(testScenarioContext("second"))
        plugin.onScenarioEnd(testScenarioContext("second"), testScenarioResult(ResultStatus.ERROR))

        assertEquals(firstReason, plugin.getFailureReason())
        assertTrue(!plugin.isAllPassed())
    }

    @Test
    fun `plugin should print all status icons`() {
        val output = ByteArrayOutputStream()
        val plugin = ConsoleOutputPlugin(PrintStream(output))
        val scenarioContext = testScenarioContext("status icons")

        plugin.onScenarioStart(scenarioContext)
        plugin.onStepEnd(testStepContext(scenarioContext, "passed", 200), testStepResult(ResultStatus.PASSED))
        plugin.onStepEnd(testStepContext(scenarioContext, "failed", 400), testStepResult(ResultStatus.FAILED))
        plugin.onStepEnd(testStepContext(scenarioContext, "error", 500), testStepResult(ResultStatus.ERROR))
        plugin.onStepEnd(testStepContext(scenarioContext, "skipped", 204), testStepResult(ResultStatus.SKIPPED))
        plugin.onScenarioEnd(scenarioContext, testScenarioResult(ResultStatus.PASSED))

        val console = output.toString(Charsets.UTF_8)
        assertTrue(console.contains("✓ passed: PASSED"))
        assertTrue(console.contains("✗ failed: FAILED"))
        assertTrue(console.contains("! error: ERROR"))
        assertTrue(console.contains("- skipped: SKIPPED"))
    }

    @Test
    fun `reportResult should no-op when listener or descriptor missing`() {
        val pluginWithoutListener = ConsoleOutputPlugin(PrintStream(ByteArrayOutputStream()))
        pluginWithoutListener.reportResult()

        val listener = RecordingEngineListener()
        val pluginWithoutDescriptor =
            ConsoleOutputPlugin(
                output = PrintStream(ByteArrayOutputStream()),
                listener = listener,
                scenarioDescriptor = null,
            )
        pluginWithoutDescriptor.reportResult()

        assertTrue(listener.finishedEvents.isEmpty())
    }

    @Test
    fun `onScenarioEnd should handle missing step collection gracefully`() {
        val output = ByteArrayOutputStream()
        val plugin = ConsoleOutputPlugin(PrintStream(output))

        plugin.onScenarioEnd(
            context = testScenarioContext("no-start"),
            result = testScenarioResult(ResultStatus.PASSED),
        )

        val console = output.toString(Charsets.UTF_8)
        assertTrue(console.contains("=== Scenario: no-start ==="))
        assertTrue(console.contains("Result: PASSED"))
    }

    @Test
    fun `step with no response should not print http status`() {
        val output = ByteArrayOutputStream()
        val plugin = ConsoleOutputPlugin(PrintStream(output))
        val scenarioContext = testScenarioContext("no-response")

        plugin.onScenarioStart(scenarioContext)
        plugin.onStepEnd(
            context = testStepContextWithoutResponse(scenarioContext, "assert local state"),
            result = testStepResult(ResultStatus.PASSED),
        )
        plugin.onScenarioEnd(scenarioContext, testScenarioResult(ResultStatus.PASSED))

        val console = output.toString(Charsets.UTF_8)
        assertTrue(console.contains("assert local state"))
        assertTrue(!console.contains("HTTP Status:"))
    }

    private fun testScenarioContext(name: String): ScenarioContext =
        object : ScenarioContext {
            override val scenarioName: String = name
            override val scenarioFile: Path = Path.of("scenarios/$name.scenario")
            override val variables: MutableMap<String, Any> = mutableMapOf()
            override val metadata: Map<String, String> = emptyMap()
            override val startTime: Instant = Instant.now()
            override val tags: Set<String> = emptySet()
            override val audits: List<ScenarioContext.HttpAudit>
                get() = TODO("Not yet implemented")

            override val executionContext: ExecutionContext
                get() = TODO("Not yet implemented")
            override val operations: List<StepOperation>
                get() = listOf()
        }

    private fun testStepContext(
        scenarioContext: ScenarioContext,
        stepDescription: String,
        statusCode: Int,
    ): StepContext =
        object : StepContext {
            override val stepDescription: String = stepDescription
            override val stepType: StepType = StepType.CALL
            override val stepIndex: Int = 0
            override val scenarioContext: ScenarioContext = scenarioContext
            override val request = null
            override val response =
                HttpResponse(
                    statusCode = statusCode,
                    statusMessage = "status",
                    headers = emptyMap(),
                    body = null,
                    duration = Duration.ofMillis(10),
                    timestamp = Instant.now(),
                    request = mock(),
                )
            override val operationId: String = "listPets"
            override val responseTime: Duration? = null
            override val operation: StepOperation?
                get() = null
        }

    private fun testStepContextWithoutResponse(
        scenarioContext: ScenarioContext,
        stepDescription: String,
    ): StepContext =
        object : StepContext {
            override val stepDescription: String = stepDescription
            override val stepType: StepType = StepType.ASSERT
            override val stepIndex: Int = 0
            override val scenarioContext: ScenarioContext = scenarioContext
            override val request = null
            override val response = null
            override val operationId: String? = null
            override val responseTime: Duration? = null
            override val operation: StepOperation?
                get() = null
        }

    private fun testStepResult(
        status: ResultStatus,
        error: Throwable? = null,
        failure: AssertionFailure? = null,
    ): StepResult =
        object : StepResult {
            override val status: ResultStatus = status
            override val duration: Duration = Duration.ofMillis(20)
            override val failure: AssertionFailure? = failure
            override val error: Throwable? = error
            override val stepDescription: String = "step"
            override val response: HttpResponse? = null
            override val isCustomStep: Boolean = false
        }

    private fun testScenarioResult(status: ResultStatus): ScenarioResult =
        object : ScenarioResult {
            override val status: ResultStatus = status
            override val duration: Duration = Duration.ofMillis(42)
            override val failedStep: Int = if (status == ResultStatus.PASSED) -1 else 0
            override val error: Throwable? = null
            override val stepResults: List<StepResult> = emptyList()
        }

    private class RecordingEngineListener : EngineExecutionListener {
        val finishedEvents = mutableListOf<Pair<TestDescriptor, TestExecutionResult>>()

        override fun dynamicTestRegistered(testDescriptor: TestDescriptor) = Unit

        override fun executionSkipped(
            testDescriptor: TestDescriptor,
            reason: String,
        ) = Unit

        override fun executionStarted(testDescriptor: TestDescriptor) = Unit

        override fun executionFinished(
            testDescriptor: TestDescriptor,
            testExecutionResult: TestExecutionResult,
        ) {
            finishedEvents += testDescriptor to testExecutionResult
        }

        override fun reportingEntryPublished(
            testDescriptor: TestDescriptor,
            entry: ReportEntry,
        ) = Unit
    }
}
