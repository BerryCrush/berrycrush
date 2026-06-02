package org.berrycrush.webhook

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WebhookAssertionsTest {
    private lateinit var server: MockWebhookServer
    private lateinit var assertions: WebhookAssertions
    private val httpClient = HttpClient.newHttpClient()

    @BeforeTest
    fun setup() {
        server = MockWebhookServer()
        assertions = WebhookAssertions(server)
    }

    @AfterTest
    fun teardown() {
        server.stop()
    }

    @Test
    fun `assertReceived should pass when webhook was received`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123}""")

        assertions.assertReceived("onPetAdopted")
    }

    @Test
    fun `assertReceived should fail when webhook was not received`() {
        server.expect("onPetAdopted")
        server.start()

        assertFailsWith<AssertionError> {
            assertions.assertReceived("onPetAdopted")
        }
    }

    @Test
    fun `assertNotReceived should pass when webhook was not received`() {
        server.expect("onPetAdopted")
        server.start()

        assertions.assertNotReceived("onPetAdopted")
    }

    @Test
    fun `assertNotReceived should fail when webhook was received`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123}""")

        assertFailsWith<AssertionError> {
            assertions.assertNotReceived("onPetAdopted")
        }
    }

    @Test
    fun `assertReceivedCount should pass with correct count`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"call": 1}""")
        sendWebhook("onPetAdopted", """{"call": 2}""")
        sendWebhook("onPetAdopted", """{"call": 3}""")

        assertions.assertReceivedCount("onPetAdopted", 3)
    }

    @Test
    fun `assertReceivedCount should fail with incorrect count`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"call": 1}""")

        assertFailsWith<AssertionError> {
            assertions.assertReceivedCount("onPetAdopted", 2)
        }
    }

    @Test
    fun `assertBodyContains should pass with matching value`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123, "adopterId": 456}""")

        assertions
            .assertReceived("onPetAdopted")
            .assertBodyContains("$.petId", 123)
            .assertBodyContains("$.adopterId", 456)
    }

    @Test
    fun `assertBodyContains should fail with non-matching value`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123}""")

        assertFailsWith<AssertionError> {
            assertions
                .assertReceived("onPetAdopted")
                .assertBodyContains("$.petId", 999)
        }
    }

    @Test
    fun `assertBodyContains should fail with missing path`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123}""")

        assertFailsWith<AssertionError> {
            assertions
                .assertReceived("onPetAdopted")
                .assertBodyContains("$.nonExistentPath", "value")
        }
    }

    @Test
    fun `assertBodyEquals should pass with matching body`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123, "status": "adopted"}""")

        assertions.assertBodyEquals(
            "onPetAdopted",
            mapOf("petId" to 123, "status" to "adopted"),
        )
    }

    @Test
    fun `assertBodyEquals should fail with mismatched body`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123, "status": "pending"}""")

        assertFailsWith<AssertionError> {
            assertions.assertBodyEquals(
                "onPetAdopted",
                mapOf("petId" to 123, "status" to "adopted"),
            )
        }
    }

    @Test
    fun `assertContentType should pass with matching content type`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123}""", "application/json")

        assertions.assertContentType("onPetAdopted", "application/json")
    }

    @Test
    fun `assertContentType should fail with mismatched content type`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123}""", "text/plain")

        assertFailsWith<AssertionError> {
            assertions.assertContentType("onPetAdopted", "application/json")
        }
    }

    @Test
    fun `assertHeader should pass with matching header`() {
        server.expect("onPetAdopted")
        server.start()

        val request =
            HttpRequest.newBuilder()
                .uri(URI.create(server.getWebhookUrl("onPetAdopted")))
                .header("Content-Type", "application/json")
                .header("X-Webhook-Signature", "abc123")
                .POST(HttpRequest.BodyPublishers.ofString("""{"petId": 123}"""))
                .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertions.assertHeader("onPetAdopted", "X-Webhook-Signature", "abc123")
    }

    @Test
    fun `assertHeader should fail with missing header`() {
        server.expect("onPetAdopted")
        server.start()

        sendWebhook("onPetAdopted", """{"petId": 123}""")

        assertFailsWith<AssertionError> {
            assertions.assertHeader("onPetAdopted", "X-Webhook-Signature", "abc123")
        }
    }

    @Test
    fun `fluent assertions should chain correctly`() {
        server.expect("onPetAdopted")
        server.start()

        val request =
            HttpRequest.newBuilder()
                .uri(URI.create(server.getWebhookUrl("onPetAdopted")))
                .header("Content-Type", "application/json")
                .header("X-Event-Type", "pet.adopted")
                .POST(HttpRequest.BodyPublishers.ofString("""{"petId": 123, "adopterId": 456}"""))
                .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertions
            .assertReceived("onPetAdopted")
            .assertReceivedCount("onPetAdopted", 1)
            .assertBodyContains("$.petId", 123)
            .assertContentType("onPetAdopted", "application/json")
            .assertHeader("onPetAdopted", "X-Event-Type", "pet.adopted")
    }

    private fun sendWebhook(
        operationId: String,
        body: String,
        contentType: String = "application/json",
    ) {
        val request =
            HttpRequest.newBuilder()
                .uri(URI.create(server.getWebhookUrl(operationId)))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
