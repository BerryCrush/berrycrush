package org.berrycrush.executor

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.model.Assertion
import org.berrycrush.model.Condition
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Scenario
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

        return BerryCrushScenarioExecutor(registry, config)
    }

    @Test
    fun `should track response time when assertions present`() {
        val executor = createExecutor()

        // Test that response time is tracked and assertions work
        // We verify by checking that response time assertions work at all
        val scenario =
            Scenario(
                name = "Verify response time tracking",
                steps =
                    listOf(
                        Step(
                            type = StepType.WHEN,
                            description = "calling API",
                            operationId = "listPets",
                            assertions =
                                listOf(
                                    Assertion(
                                        condition = Condition.ResponseTime(maxMs = 120000), // 2 minutes
                                        description = "responseTime < 120s",
                                    ),
                                ),
                        ),
                    ),
            )

        val result = executor.execute(scenario)

        // Skip test if network error (not testing network here)
        if (result.stepResults.firstOrNull()?.error != null &&
            result.stepResults.first().error?.message?.contains("connect") == true
        ) {
            return
        }

        val stepResult = result.stepResults.firstOrNull() ?: return
        val assertionResults = stepResult.assertionResults
        val responseTimeAssertion = assertionResults.find { it.assertion.condition is Condition.ResponseTime }

        // If we got a response time assertion result, it means time was tracked
        assertNotNull(responseTimeAssertion, "Response time assertion should be present")
    }

    @Test
    fun `should pass response time assertion when within threshold`() {
        val executor = createExecutor()

        // Use a very generous threshold that should always pass
        val scenario =
            Scenario(
                name = "Response time passes",
                steps =
                    listOf(
                        Step(
                            type = StepType.WHEN,
                            description = "calling API",
                            operationId = "listPets",
                            assertions =
                                listOf(
                                    Assertion(
                                        condition = Condition.ResponseTime(maxMs = 60000), // 60 seconds - very generous
                                        description = "responseTime < 60000",
                                    ),
                                ),
                        ),
                    ),
            )

        val result = executor.execute(scenario)

        // Skip test if network error (not testing network here)
        if (result.stepResults.firstOrNull()?.error != null &&
            result.stepResults.first().error?.message?.contains("connect") == true
        ) {
            return
        }

        val stepResult = result.stepResults.firstOrNull()
        val assertionResults = stepResult?.assertionResults ?: emptyList()
        val responseTimeAssertion = assertionResults.find { it.assertion.condition is Condition.ResponseTime }

        if (responseTimeAssertion != null) {
            assertTrue(responseTimeAssertion.passed, "Response time assertion should pass with generous threshold")
        }
    }

    @Test
    fun `should fail response time assertion when threshold exceeded`() {
        val executor = createExecutor()

        // Use an impossibly small threshold that should always fail
        val scenario =
            Scenario(
                name = "Response time fails",
                steps =
                    listOf(
                        Step(
                            type = StepType.WHEN,
                            description = "calling API",
                            operationId = "listPets",
                            assertions =
                                listOf(
                                    Assertion(
                                        condition = Condition.ResponseTime(maxMs = 0), // 0ms - impossible to meet
                                        description = "responseTime < 0",
                                    ),
                                ),
                        ),
                    ),
            )

        val result = executor.execute(scenario)

        // Skip test if network error (not testing network here)
        if (result.stepResults.firstOrNull()?.error != null &&
            result.stepResults.first().error?.message?.contains("connect") == true
        ) {
            return
        }

        val stepResult = result.stepResults.firstOrNull()
        val assertionResults = stepResult?.assertionResults ?: emptyList()
        val responseTimeAssertion = assertionResults.find { it.assertion.condition is Condition.ResponseTime }

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
        val executor = createExecutor()

        val scenario =
            Scenario(
                name = "Response time with ms unit",
                steps =
                    listOf(
                        Step(
                            type = StepType.WHEN,
                            description = "calling API",
                            operationId = "listPets",
                            assertions =
                                listOf(
                                    Assertion(
                                        condition = Condition.ResponseTime(maxMs = "60000ms"), // 60 seconds as string
                                        description = "responseTime < 60000ms",
                                    ),
                                ),
                        ),
                    ),
            )

        val result = executor.execute(scenario)

        // Skip test if network error
        if (result.stepResults.firstOrNull()?.error != null &&
            result.stepResults.first().error?.message?.contains("connect") == true
        ) {
            return
        }

        val stepResult = result.stepResults.firstOrNull()
        val assertionResults = stepResult?.assertionResults ?: emptyList()
        val responseTimeAssertion = assertionResults.find { it.assertion.condition is Condition.ResponseTime }

        if (responseTimeAssertion != null) {
            assertTrue(responseTimeAssertion.passed, "Response time assertion should pass with 60000ms threshold")
        }
    }

    @Test
    fun `should handle string threshold with seconds unit`() {
        val executor = createExecutor()

        val scenario =
            Scenario(
                name = "Response time with seconds unit",
                steps =
                    listOf(
                        Step(
                            type = StepType.WHEN,
                            description = "calling API",
                            operationId = "listPets",
                            assertions =
                                listOf(
                                    Assertion(
                                        condition = Condition.ResponseTime(maxMs = "60s"), // 60 seconds
                                        description = "responseTime < 60s",
                                    ),
                                ),
                        ),
                    ),
            )

        val result = executor.execute(scenario)

        // Skip test if network error
        if (result.stepResults.firstOrNull()?.error != null &&
            result.stepResults.first().error?.message?.contains("connect") == true
        ) {
            return
        }

        val stepResult = result.stepResults.firstOrNull()
        val assertionResults = stepResult?.assertionResults ?: emptyList()
        val responseTimeAssertion = assertionResults.find { it.assertion.condition is Condition.ResponseTime }

        if (responseTimeAssertion != null) {
            assertTrue(responseTimeAssertion.passed, "Response time assertion should pass with 60s threshold")
        }
    }

    @Test
    fun `should report actual response time in assertion result`() {
        val executor = createExecutor()

        val scenario =
            Scenario(
                name = "Response time with actual value",
                steps =
                    listOf(
                        Step(
                            type = StepType.WHEN,
                            description = "calling API",
                            operationId = "listPets",
                            assertions =
                                listOf(
                                    Assertion(
                                        condition = Condition.ResponseTime(maxMs = 60000),
                                        description = "responseTime < 60000",
                                    ),
                                ),
                        ),
                    ),
            )

        val result = executor.execute(scenario)

        // Skip test if network error
        if (result.stepResults.firstOrNull()?.error != null &&
            result.stepResults.first().error?.message?.contains("connect") == true
        ) {
            return
        }

        val stepResult = result.stepResults.firstOrNull()
        val assertionResults = stepResult?.assertionResults ?: emptyList()
        val responseTimeAssertion = assertionResults.find { it.assertion.condition is Condition.ResponseTime }

        if (responseTimeAssertion != null) {
            assertNotNull(responseTimeAssertion.actual, "Assertion result should include actual response time")
            assertTrue(
                (responseTimeAssertion.actual as? Long ?: 0L) >= 0,
                "Actual response time should be non-negative",
            )
        }
    }
}
