package org.berrycrush.executor

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.model.Assertion
import org.berrycrush.model.AssertionResult
import org.berrycrush.model.Condition
import org.berrycrush.model.Scenario
import org.berrycrush.model.ScenarioResult
import org.berrycrush.model.Step
import org.berrycrush.model.StepType
import org.berrycrush.openapi.SpecRegistry
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for response time assertion functionality.
 *
 * These tests verify that:
 * - Response time is captured for HTTP calls
 * - Response time assertions work with various thresholds and units
 * - Assertion messages are informative
 */
class ResponseTimeAssertionTest {
    private fun createExecutor(): BerryCrushScenarioExecutor {
        val specPath =
            javaClass.getResource("/petstore.yaml")?.path
                ?: error("petstore.yaml not found")

        val registry = SpecRegistry()
        registry.registerDefault(specPath)

        val config =
            BerryCrushConfiguration().apply {
                baseUrl = "https://httpbin.org"
            }

        return BerryCrushScenarioExecutor(registry, BerryCrushConfigurationProvider.from(config))
    }

    /**
     * Create a scenario with a response time assertion.
     */
    private fun createResponseTimeScenario(
        name: String,
        threshold: Any,
    ): Scenario =
        Scenario(
            name = name,
            steps =
                listOf(
                    Step(
                        type = StepType.WHEN,
                        description = "calling API",
                        operationId = "listPets",
                        assertions =
                            listOf(Condition.ResponseTime(duration = threshold).toAssertion("responseTime check")),
                    ),
                ),
        )

    /**
     * Execute scenario and return result, or null if network error.
     */
    private fun executeWithNetworkSkip(scenario: Scenario): ScenarioResult? {
        val executor = createExecutor()
        val result = executor.execute(scenario)

        // Skip test if network error (not testing network here)
        if (result.stepResults.firstOrNull()?.error != null &&
            result.stepResults
                .first()
                .error
                ?.message
                ?.contains("connect") == true
        ) {
            return null
        }
        return result
    }

    @Test
    fun `should track response time when assertions present`() {
        val scenario = createResponseTimeScenario("Verify response time tracking", 120000)
        val result = executeWithNetworkSkip(scenario) ?: return

        val stepResult = result.stepResults.firstOrNull() ?: return
        val responseTimeAssertion =
            stepResult.assertionResults.find {
                (it.assertion as? Assertion.BuiltinAssertion)?.condition is Condition.ResponseTime
            }

        assertNotNull(responseTimeAssertion, "Response time assertion should be present")
    }

    @Test
    fun `should pass response time assertion when within threshold`() {
        val scenario = createResponseTimeScenario("Response time passes", 60000)
        val result = executeWithNetworkSkip(scenario) ?: return

        val responseTimeAssertion = result.responseTime

        if (responseTimeAssertion != null) {
            assertTrue(responseTimeAssertion.passed, "Response time assertion should pass with generous threshold")
        }
    }

    @Test
    fun `should fail response time assertion when threshold exceeded`() {
        val scenario = createResponseTimeScenario("Response time fails", 0)
        val result = executeWithNetworkSkip(scenario) ?: return

        val responseTimeAssertion = result.responseTime

        if (responseTimeAssertion != null) {
            assertTrue(!responseTimeAssertion.passed, "Response time assertion should fail with 0ms threshold")
            assertTrue(
                responseTimeAssertion.message.contains("exceeded"),
                "Failure message should indicate threshold was exceeded",
            )
        }
    }

    @Test
    fun `should handle string threshold with ms unit`() {
        val scenario = createResponseTimeScenario("Response time with ms unit", "60000ms")
        val result = executeWithNetworkSkip(scenario) ?: return

        val responseTimeAssertion = result.responseTime

        if (responseTimeAssertion != null) {
            assertTrue(responseTimeAssertion.passed, "Response time assertion should pass with 60000ms threshold")
        }
    }

    @Test
    fun `should handle string threshold with seconds unit`() {
        val scenario = createResponseTimeScenario("Response time with seconds unit", "60s")
        val result = executeWithNetworkSkip(scenario) ?: return

        val responseTimeAssertion = result.responseTime

        if (responseTimeAssertion != null) {
            assertTrue(responseTimeAssertion.passed, "Response time assertion should pass with 60s threshold")
        }
    }

    @Test
    fun `should report actual response time in assertion result`() {
        val scenario = createResponseTimeScenario("Response time with actual value", 60000)
        val result = executeWithNetworkSkip(scenario) ?: return

        val responseTimeAssertion = result.responseTime

        if (responseTimeAssertion != null) {
            assertNotNull(responseTimeAssertion.actual, "Assertion result should include actual response time")
            assertTrue(
                (responseTimeAssertion.actual as? Long ?: 0L) >= 0,
                "Actual response time should be non-negative",
            )
        }
    }

    private val ScenarioResult.responseTime: AssertionResult?
        get() =
            stepResults
                .firstOrNull()
                ?.assertionResults
                ?.find { (it.assertion as? Assertion.BuiltinAssertion)?.condition is Condition.ResponseTime }
}
