package org.berrycrush.junit.engine.adapter

import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.MultiMode
import org.berrycrush.autotest.MultiTestResult
import org.berrycrush.autotest.ParameterLocation
import org.berrycrush.junit.engine.AutoTestDescriptor
import org.berrycrush.junit.engine.IndividualScenarioDescriptor
import org.berrycrush.junit.engine.MultiTestDescriptor
import org.berrycrush.model.AutoTestResult
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Scenario
import org.berrycrush.model.ScenarioResult
import org.berrycrush.model.Step
import org.berrycrush.model.StepResult
import org.berrycrush.model.StepType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.mockito.Answers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

internal class JUnitExecutionListenerAdapterTest {
    val scenarioDescriptor: IndividualScenarioDescriptor = mock()
    val listener: EngineExecutionListener = mock()
    val listenerAdapter: JUnitExecutionListenerAdapter = JUnitExecutionListenerAdapter(scenarioDescriptor, listener)

    private val scenario: Scenario = mock()
    private val step: Step = mock()
    private val scenarioResult: ScenarioResult = mock()
    private val autoTestCase: AutoTestCase = mock()
    private val uniqueId =
        mock<UniqueId> {
            on { append(any(), any<String>()) } doAnswer { it.mock as UniqueId }
        }

    @Test
    fun `on scenario starting`() {
        listenerAdapter.onScenarioStarting(scenario)
        verify(listener).executionStarted(scenarioDescriptor)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "PASSED,0,0",
            "SKIPPED,0,0",
            "FAILED,0,0",
            "ERROR,1,0",
            "ERROR,0,1",
        ],
    )
    fun `on scenario completed`(
        status: String,
        autoTest: Int,
        multiTest: Int,
    ) {
        val stubAutoTestResults = (0..autoTest).map { mock<AutoTestResult>() }
        val stubMultiTestResults = (0..multiTest).map { mock<MultiTestResult>() }
        val stepResult =
            mock<StepResult>(defaultAnswer = Answers.RETURNS_DEEP_STUBS) {
                on { autoTestResults } doReturn stubAutoTestResults
                on { multiTestResults } doReturn stubMultiTestResults
                on { step.type } doReturn StepType.THEN
            }
        whenever { scenario.name } doReturn "Test on complete scenario"

        val resultStatus = ResultStatus.valueOf(status)
        whenever { scenarioResult.status } doReturn resultStatus
        whenever { scenarioResult.stepResults } doReturn listOf(stepResult)

        listenerAdapter.onScenarioCompleted(scenario, scenarioResult)

        val captor = argumentCaptor<TestExecutionResult>()
        verify(listener).executionFinished(eq(scenarioDescriptor), captor.capture())

        when (resultStatus) {
            ResultStatus.PASSED -> assert(captor.firstValue.status == TestExecutionResult.Status.SUCCESSFUL)
            ResultStatus.SKIPPED -> assert(captor.firstValue.status == TestExecutionResult.Status.ABORTED)
            else -> assert(captor.firstValue.status == TestExecutionResult.Status.FAILED)
        }
    }

    @Test
    fun `on step starting`() {
        listenerAdapter.onStepStarting(step)
    }

    @Test
    fun `on step completed`() {
        val result = mock<StepResult>()
        listenerAdapter.onStepCompleted(step, result)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "true,BODY",
            "false,PATH",
            "false,QUERY",
            "false,HEADER",
        ],
    )
    fun `on auto test starting-completed`(
        passed: Boolean,
        parameterLocation: ParameterLocation,
    ) {
        whenever { autoTestCase.location } doReturn parameterLocation
        whenever { autoTestCase.description } doReturn "Test on complete scenario"
        whenever { scenarioDescriptor.uniqueId } doReturn uniqueId

        listenerAdapter.onAutoTestStarting(autoTestCase)
        val captor = argumentCaptor<AutoTestDescriptor>()
        verify(scenarioDescriptor).addChild(captor.capture())
        val v = captor.firstValue
        verify(listener).dynamicTestRegistered(v)
        verify(listener).executionStarted(v)

        val result =
            mock<AutoTestResult> {
                on { this.passed } doReturn passed
                on { testCase } doReturn autoTestCase
                on { error } doReturn "error"
            }
        listenerAdapter.onAutoTestCompleted(autoTestCase, result)
        if (passed) {
            verify(listener).executionFinished(eq(v), eq(TestExecutionResult.successful()))
        } else {
            val captor = argumentCaptor<TestExecutionResult>()
            verify(listener).executionFinished(eq(v), captor.capture())
            assertEquals(TestExecutionResult.Status.FAILED, captor.firstValue.status)
        }
    }

    @Test
    fun `on auto test completed (invalid)`() {
        assertThrows<IllegalStateException> { listenerAdapter.onAutoTestCompleted(autoTestCase, mock()) }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "SEQUENTIAL,true",
            "SEQUENTIAL,false",
            "CONCURRENT,true",
            "CONCURRENT,false",
        ],
    )
    fun `on multi test starting - completed`(
        mode: MultiMode,
        passed: Boolean,
    ) {
        whenever { scenarioDescriptor.uniqueId } doReturn uniqueId
        listenerAdapter.onMultiTestStarting(mode, 1)

        val captor = argumentCaptor<MultiTestDescriptor>()
        verify(scenarioDescriptor).addChild(captor.capture())
        val v = captor.firstValue
        verify(listener).dynamicTestRegistered(v)
        verify(listener).executionStarted(v)

        val result =
            mock<MultiTestResult> {
                on { this.passed } doReturn passed
                on { this.mode } doReturn mode
            }
        listenerAdapter.onMultiTestCompleted(result)
        if (passed) {
            verify(listener).executionFinished(eq(v), eq(TestExecutionResult.successful()))
        } else {
            val captor = argumentCaptor<TestExecutionResult>()
            verify(listener).executionFinished(eq(v), captor.capture())
            assertEquals(TestExecutionResult.Status.FAILED, captor.firstValue.status)
        }
    }

    @Test
    fun `on multi test completed (invalid)`() {
        assertThrows<IllegalStateException> { listenerAdapter.onMultiTestCompleted(mock()) }
    }
}
