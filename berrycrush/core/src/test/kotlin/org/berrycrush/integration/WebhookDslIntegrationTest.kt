package org.berrycrush.integration

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.context.ExecutionContext
import org.berrycrush.executor.BerryCrushScenarioExecutor
import org.berrycrush.model.ResultStatus
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.scenario.ScenarioLoader
import org.berrycrush.scenario.WebhookScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end integration tests for the Webhook DSL.
 *
 * Tests the complete flow from parsing scenario files with webhook syntax
 * through execution.
 */
class WebhookDslIntegrationTest {
    private val specRegistry = SpecRegistry()
    private val config = BerryCrushConfiguration()
    private val loader = ScenarioLoader()

    @Test
    fun `should parse and execute webhook step successfully`() {
        val source = """
            scenario: Test webhook setup
              given a webhook server is ready
                webhook: payments
                  port: 0
                  hook: onPaymentReceived
        """.trimIndent()

        val scenarios = loader.loadScenariosFromString(source)
        assertEquals(1, scenarios.size)

        val scenario = scenarios.first()
        assertEquals(1, scenario.steps.size)

        // Verify webhook config is parsed correctly
        val step = scenario.steps.first()
        assertNotNull(step.webhookConfig)
        assertEquals("payments", step.webhookConfig!!.name)
        assertEquals(0, step.webhookConfig!!.port)
        assertEquals(listOf("onPaymentReceived"), step.webhookConfig!!.hooks)

        // Execute scenario - should succeed
        val executor = BerryCrushScenarioExecutor(specRegistry, config)
        val result = executor.execute(scenario)
        assertEquals(ResultStatus.PASSED, result.status)
    }

    @Test
    fun `should parse multiple hooks in webhook step`() {
        val source = """
            scenario: Multi-hook webhook
              given notification listeners are active
                webhook: notifications
                  port: 8080
                  hooks:
                    - onEmail
                    - onSms
                    - onPush
        """.trimIndent()

        val scenarios = loader.loadScenariosFromString(source)
        val step = scenarios.first().steps.first()

        assertNotNull(step.webhookConfig)
        assertEquals("notifications", step.webhookConfig!!.name)
        assertEquals(8080, step.webhookConfig!!.port)
        assertEquals(listOf("onEmail", "onSms", "onPush"), step.webhookConfig!!.hooks)
    }

    @Test
    fun `should parse feature scope webhook`() {
        val source = """
            scenario: Feature scope webhook
              given a shared webhook server
                webhook: shared
                  port: 0
                  hook: event
                  scope: feature
        """.trimIndent()

        val scenarios = loader.loadScenariosFromString(source)
        val step = scenarios.first().steps.first()

        assertEquals(WebhookScope.FEATURE, step.webhookConfig!!.scope)
    }

    @Test
    fun `should parse multiple webhook servers in same scenario`() {
        val source = """
            scenario: Multiple webhooks
              given payment webhooks are ready
                webhook: payments
                  port: 0
                  hook: onPayment
              and notification webhooks are ready
                webhook: notifications
                  port: 0
                  hooks:
                    - onEmail
                    - onSms
        """.trimIndent()

        val scenarios = loader.loadScenariosFromString(source)
        assertEquals(1, scenarios.size)
        assertEquals(2, scenarios.first().steps.size)

        val step1 = scenarios.first().steps[0]
        val step2 = scenarios.first().steps[1]

        assertEquals("payments", step1.webhookConfig!!.name)
        assertEquals("notifications", step2.webhookConfig!!.name)
        assertEquals(listOf("onEmail", "onSms"), step2.webhookConfig!!.hooks)
    }

    @Test
    fun `should execute scenario with multiple webhooks successfully`() {
        val source = """
            scenario: Multiple webhooks
              given payment webhooks are ready
                webhook: payments
                  port: 0
                  hook: onPayment
              and notification webhooks are ready
                webhook: notifications
                  port: 0
                  hook: onEmail
        """.trimIndent()

        val scenarios = loader.loadScenariosFromString(source)
        val executor = BerryCrushScenarioExecutor(specRegistry, config)
        val result = executor.execute(scenarios.first())

        assertEquals(ResultStatus.PASSED, result.status)
        assertEquals(2, result.stepResults.size)
        assertTrue(result.stepResults.all { it.status == ResultStatus.PASSED })
    }

    @Test
    fun `should cleanup webhook servers after scenario execution`() {
        val source = """
            scenario: Cleanup test
              given a webhook server exists
                webhook: temp
                  port: 0
                  hook: event
        """.trimIndent()

        val scenarios = loader.loadScenariosFromString(source)
        val localContext = ExecutionContext()
        val executor = BerryCrushScenarioExecutor(specRegistry, config)

        // Execute should succeed
        val result = executor.execute(scenarios.first(), localContext)
        assertEquals(ResultStatus.PASSED, result.status)

        // After execute, the passed context should not have webhook servers
        // (they are registered in a child context and cleaned up)
        assertTrue(localContext.webhookServerNames().isEmpty())
    }
}
