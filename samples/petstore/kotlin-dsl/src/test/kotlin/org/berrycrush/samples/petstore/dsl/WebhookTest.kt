package org.berrycrush.samples.petstore.dsl

import org.berrycrush.webhook.MockWebhookServer
import org.berrycrush.webhook.WebhookAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Demonstrates BerryCrush webhook testing capabilities.
 *
 * This test shows how to:
 * - Set up a MockWebhookServer for testing webhook deliveries
 * - Use WebhookAssertions for fluent verification
 * - Test webhook payloads and headers
 *
 * Note: In a real-world scenario, your application would send webhooks
 * to the mock server URL. This example simulates webhook deliveries
 * to demonstrate the testing patterns.
 */
@DisplayName("Webhook Testing Features")
class WebhookTest {
    private lateinit var webhookServer: MockWebhookServer
    private val httpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun setup() {
        webhookServer = MockWebhookServer()
    }

    @AfterEach
    fun teardown() {
        webhookServer.stop()
    }

    @Test
    @DisplayName("Should receive and verify webhook delivery")
    fun `should receive and verify webhook delivery`() {
        // Setup: Register expected webhook
        webhookServer.expect("onPetAdopted")
        webhookServer.start()

        // Simulate: Application sends webhook (in real scenario, your app does this)
        sendWebhook(
            "onPetAdopted",
            """{"petId": 123, "adopterId": 456, "timestamp": "2024-01-15T10:30:00Z"}""",
        )

        // Verify: Use fluent assertions
        WebhookAssertions(webhookServer)
            .assertReceived("onPetAdopted")
            .assertReceivedCount("onPetAdopted", 1)
            .assertBodyContains("$.petId", 123)
            .assertBodyContains("$.adopterId", 456)
            .assertContentType("onPetAdopted", "application/json")
    }

    @Test
    @DisplayName("Should verify webhook was not received")
    fun `should verify webhook was not received`() {
        // Setup
        webhookServer.expect("onPetAdopted")
        webhookServer.expect("onPetReturned")
        webhookServer.start()

        // Only send one webhook
        sendWebhook(
            "onPetAdopted",
            """{"petId": 123}""",
        )

        // Verify
        WebhookAssertions(webhookServer)
            .assertReceived("onPetAdopted")
            .assertNotReceived("onPetReturned")
    }

    @Test
    @DisplayName("Should track multiple webhook calls")
    fun `should track multiple webhook calls`() {
        // Setup
        webhookServer.expect("onPetAdopted")
        webhookServer.start()

        // Send multiple webhooks
        repeat(3) { i ->
            sendWebhook(
                "onPetAdopted",
                """{"eventId": $i, "petId": ${100 + i}}""",
            )
        }

        // Verify
        WebhookAssertions(webhookServer)
            .assertReceivedCount("onPetAdopted", 3)

        // Can also access raw calls for custom assertions
        val calls = webhookServer.getReceived("onPetAdopted")
        assertEquals(3, calls.size)
    }

    @Test
    @DisplayName("Should verify webhook headers")
    fun `should verify webhook headers`() {
        // Setup
        webhookServer.expect("onPetAdopted")
        webhookServer.start()

        // Send webhook with custom header
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(webhookServer.getWebhookUrl("onPetAdopted")))
                .header("Content-Type", "application/json")
                .header("X-Webhook-Signature", "sha256=abc123def456")
                .header("X-Event-Type", "pet.adopted")
                .POST(HttpRequest.BodyPublishers.ofString("""{"petId": 123}"""))
                .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        // Verify headers
        WebhookAssertions(webhookServer)
            .assertReceived("onPetAdopted")
            .assertHeader("onPetAdopted", "X-Webhook-Signature", "sha256=abc123def456")
            .assertHeader("onPetAdopted", "X-Event-Type", "pet.adopted")
    }

    @Test
    @DisplayName("Should handle multiple webhook endpoints")
    fun `should handle multiple webhook endpoints`() {
        // Setup multiple endpoints
        webhookServer.expect("onPetCreated")
        webhookServer.expect("onPetAdopted")
        webhookServer.expect("onPetReturned")
        webhookServer.start()

        // Send to different endpoints
        sendWebhook("onPetCreated", """{"petId": 1, "name": "Fluffy"}""")
        sendWebhook("onPetAdopted", """{"petId": 1, "adopterId": 100}""")

        // Verify each endpoint independently
        WebhookAssertions(webhookServer)
            .assertReceived("onPetCreated")
            .assertBodyContains("$.name", "Fluffy")

        WebhookAssertions(webhookServer)
            .assertReceived("onPetAdopted")
            .assertBodyContains("$.adopterId", 100)

        WebhookAssertions(webhookServer)
            .assertNotReceived("onPetReturned")
    }

    @Test
    @DisplayName("Should clear and reset webhook state")
    fun `should clear and reset webhook state`() {
        // Setup
        webhookServer.expect("onPetAdopted")
        webhookServer.start()

        // First batch
        sendWebhook("onPetAdopted", """{"batch": 1}""")
        assertEquals(1, webhookServer.getReceivedCount("onPetAdopted"))

        // Clear received calls
        webhookServer.clearReceived()
        assertEquals(0, webhookServer.getReceivedCount("onPetAdopted"))

        // Second batch
        sendWebhook("onPetAdopted", """{"batch": 2}""")
        sendWebhook("onPetAdopted", """{"batch": 2}""")
        assertEquals(2, webhookServer.getReceivedCount("onPetAdopted"))
    }

    @Test
    @DisplayName("Should verify expected webhook count")
    fun `should verify expected webhook count`() {
        // Setup with expected count
        webhookServer.expect(
            "onPetAdopted",
            org.berrycrush.webhook.WebhookExpectation(expectedCount = 2),
        )
        webhookServer.start()

        // Send webhooks
        sendWebhook("onPetAdopted", """{"call": 1}""")

        // Not enough calls yet
        assertTrue(!webhookServer.verify("onPetAdopted"), "Should not verify with 1 call")

        // Send another
        sendWebhook("onPetAdopted", """{"call": 2}""")

        // Now it should verify
        assertTrue(webhookServer.verify("onPetAdopted"), "Should verify with 2 calls")
    }

    /**
     * Helper to send a webhook to the mock server.
     */
    private fun sendWebhook(
        operationId: String,
        body: String,
    ) {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(webhookServer.getWebhookUrl(operationId)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
