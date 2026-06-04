package org.berrycrush.executor

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.context.ExecutionContext
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Step
import org.berrycrush.model.StepResult
import org.berrycrush.model.StepType
import org.berrycrush.model.WebhookConfig
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.scenario.WebhookScope
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for webhook step execution in BerryCrushScenarioExecutor.
 */
class WebhookStepExecutorTest {
    private val httpClient = HttpClient.newHttpClient()
    private val specRegistry = SpecRegistry()
    private val config = BerryCrushConfiguration()
    private val executor = BerryCrushScenarioExecutor(specRegistry, config)
    private val context = ExecutionContext()

    @AfterTest
    fun cleanup() {
        context.cleanupWebhookServers()
    }

    @Test
    fun `should start webhook server on dynamic port`() {
        val step =
            createWebhookStep(
                name = "testServer",
                port = 0,
                hooks = listOf("onEvent"),
            )

        val result = executeWebhookStep(step)

        assertEquals(ResultStatus.PASSED, result.status)
        assertNotNull(context.getWebhookServer("testServer"))
        assertTrue(context.getWebhookServer("testServer")!!.getPort() > 0)
    }

    @Test
    fun `should register multiple hooks`() {
        val step =
            createWebhookStep(
                name = "multiHook",
                port = 0,
                hooks = listOf("hook1", "hook2", "hook3"),
            )

        val result = executeWebhookStep(step)

        assertEquals(ResultStatus.PASSED, result.status)
        val server = context.getWebhookServer("multiHook")
        assertNotNull(server)

        // Verify all hooks are accessible
        listOf("hook1", "hook2", "hook3").forEach { hook ->
            val url = server.getWebhookUrl(hook)
            assertTrue(url.contains("/webhook/$hook"))
        }
    }

    @Test
    fun `should receive webhook calls`() {
        val step =
            createWebhookStep(
                name = "receiver",
                port = 0,
                hooks = listOf("onPayment"),
            )

        executeWebhookStep(step)
        val server = context.getWebhookServer("receiver")!!

        // Send a webhook call
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(server.getWebhookUrl("onPayment")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""{"amount": 100}"""))
                .build()

        httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(1, server.getReceivedCount("onPayment"))
    }

    @Test
    fun `should support multiple concurrent servers`() {
        val step1 = createWebhookStep("server1", 0, listOf("event1"))
        val step2 = createWebhookStep("server2", 0, listOf("event2"))

        executeWebhookStep(step1)
        executeWebhookStep(step2)

        val server1 = context.getWebhookServer("server1")
        val server2 = context.getWebhookServer("server2")

        assertNotNull(server1)
        assertNotNull(server2)
        assertTrue(server1.getPort() != server2.getPort())
    }

    @Test
    fun `should cleanup servers on context cleanup`() {
        val step = createWebhookStep("cleanup", 0, listOf("test"))
        executeWebhookStep(step)

        assertTrue(context.webhookServerNames().contains("cleanup"))

        context.cleanupWebhookServers()

        assertTrue(context.webhookServerNames().isEmpty())
    }

    @Test
    fun `should provide webhook URL via interpolation`() {
        val step = createWebhookStep("api", 0, listOf("callback"))
        executeWebhookStep(step)

        val url = context.interpolate("{{api.callback}}")

        assertTrue(url.startsWith("http://localhost:"))
        assertTrue(url.contains("/webhook/callback"))
    }

    private fun createWebhookStep(
        name: String,
        port: Int,
        hooks: List<String>,
        scope: WebhookScope = WebhookScope.SCENARIO,
    ): Step =
        Step(
            type = StepType.GIVEN,
            description = "webhook server is running",
            webhookConfig =
                WebhookConfig(
                    name = name,
                    port = port,
                    hooks = hooks,
                    scope = scope,
                ),
        )

    private fun executeWebhookStep(step: Step): StepResult = executor.executeNonOperationStep(step, context, Instant.now())
}
