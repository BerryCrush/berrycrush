package org.berrycrush.executor

import java.time.Duration
import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.MultiMode
import org.berrycrush.autotest.MultiTestResult
import org.berrycrush.autotest.ParameterLocation
import org.berrycrush.autotest.RequestResult
import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.model.AutoTestResult
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Scenario
import org.berrycrush.model.ScenarioResult
import org.berrycrush.model.Step
import org.berrycrush.model.StepResult
import org.berrycrush.model.StepType
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.scenario.AutoTestType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BerryCrushExecutionListenerTest {
    @Test
    fun `default listener methods should be callable without exceptions`() {
        val listener = object : BerryCrushExecutionListener {}
        val scenario = Scenario(name = "listener scenario")
        val step = Step(type = StepType.THEN, description = "listener step")
        val stepResult = StepResult(step = step, status = ResultStatus.PASSED)
        val scenarioResult = ScenarioResult(scenario = scenario, status = ResultStatus.PASSED)
        val testCase =
            AutoTestCase(
                type = AutoTestType.INVALID,
                fieldName = "name",
                invalidValue = null,
                description = "invalid name",
                location = ParameterLocation.BODY,
                tag = "invalid",
            )
        val autoTestResult = AutoTestResult(testCase = testCase, passed = true)
        val multiTestResult =
            MultiTestResult(
                mode = MultiMode.SEQUENTIAL,
                requestCount = 1,
                results =
                    listOf(
                        RequestResult.create(
                            requestIndex = 0,
                        ),
                    ),
                totalDuration = Duration.ofMillis(10),
                passed = true,
            )

        val thrown =
            runCatching {
                listener.onScenarioStarting(scenario)
                listener.onStepStarting(step)
                listener.onAutoTestStarting(testCase)
                listener.onAutoTestCompleted(autoTestResult)
                listener.onMultiTestStarting(MultiMode.SEQUENTIAL, 1)
                listener.onMultiTestCompleted(multiTestResult)
                listener.onStepCompleted(stepResult)
                listener.onScenarioCompleted(scenario, scenarioResult)
            }.exceptionOrNull()

        assertNull(thrown)
    }

    @Test
    fun `noop listener should be available`() {
        val noop = BerryCrushExecutionListener.NOOP

        assertNotNull(noop)
    }

    @Test
    fun `executor should notify listener in scenario and step order`() {
        val registry = SpecRegistry()
        registry.registerDefault(petstoreSpecPath())
        val executor = BerryCrushScenarioExecutor(registry, BerryCrushConfigurationProvider.from(BerryCrushConfiguration()))
        val events = mutableListOf<String>()

        val listener =
            object : BerryCrushExecutionListener {
                override fun onScenarioStarting(scenario: Scenario) {
                    events += "scenario:start:${scenario.name}"
                }

                override fun onStepStarting(step: Step) {
                    events += "step:start:${step.description}"
                }

                override fun onStepCompleted(result: StepResult) {
                    events += "step:end:${result.step.description}:${result.status}"
                }

                override fun onScenarioCompleted(
                    scenario: Scenario,
                    result: ScenarioResult,
                ) {
                    events += "scenario:end:${scenario.name}:${result.status}"
                }
            }

        val scenario =
            Scenario(
                name = "listener execution",
                steps =
                    listOf(
                        Step(type = StepType.GIVEN, description = "first step"),
                        Step(type = StepType.THEN, description = "second step"),
                    ),
            )

        val result = executor.execute(scenario, executionListener = listener)

        assertEquals(ResultStatus.PASSED, result.status)
        assertEquals(
            listOf(
                "scenario:start:listener execution",
                "step:start:first step",
                "step:end:first step:PASSED",
                "step:start:second step",
                "step:end:second step:PASSED",
                "scenario:end:listener execution:PASSED",
            ),
            events,
        )
        assertTrue(result.stepResults.all { it.status == ResultStatus.PASSED })
    }

    private fun petstoreSpecPath(): String =
        javaClass.getResource("/petstore.yaml")?.path
            ?: error("petstore.yaml not found")
}
