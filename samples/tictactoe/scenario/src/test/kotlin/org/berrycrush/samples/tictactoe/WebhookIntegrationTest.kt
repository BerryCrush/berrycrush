package org.berrycrush.samples.tictactoe

import org.berrycrush.webhook.MockWebhookServer
import org.berrycrush.webhook.WebhookAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Integration tests demonstrating BerryCrush's MockWebhookServer usage.
 *
 * These tests show how to set up a mock webhook server to capture
 * webhook calls from TicTacToe application. In a full integration test,
 * the application would be started with the webhook URL pointing to this server.
 */
@DisplayName("TicTacToe Webhook Unit Tests")
class WebhookIntegrationTest {
    private lateinit var webhookServer: MockWebhookServer
    private val httpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun setup() {
        webhookServer = MockWebhookServer()
        webhookServer.expect("markOperationWebhook")
    }

    @AfterEach
    fun teardown() {
        webhookServer.stop()
    }

    @Test
    @DisplayName("MockWebhookServer captures webhook calls")
    fun `should capture webhook calls with game status`() {
        webhookServer.start()

        // Simulate a webhook call that the app would make after a move
        val gameStatus = """{"board":[["X",".","."],[".",".","."],[".",".","."]],"winner":"."}"""
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(webhookServer.getWebhookUrl("markOperationWebhook")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gameStatus))
                .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        assertTrue(webhookServer.wasReceived("markOperationWebhook"))
    }

    @Test
    @DisplayName("WebhookAssertions validates received webhooks")
    fun `should validate webhook with assertions`() {
        webhookServer.start()

        // Simulate webhook call
        val gameStatus = """{"board":[["X","X","X"],[".","O","."],[".","O","."]],"winner":"X"}"""
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(webhookServer.getWebhookUrl("markOperationWebhook")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gameStatus))
                .build()

        httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        // Verify with BerryCrush assertions
        WebhookAssertions(webhookServer)
            .assertReceived("markOperationWebhook")
            .assertReceivedCount("markOperationWebhook", 1)
            .assertBodyContains("$.winner", "X")
            .assertContentType("markOperationWebhook", "application/json")
    }

    @Test
    @DisplayName("Multiple webhook calls are captured")
    fun `should capture multiple webhook calls`() {
        webhookServer.start()
        val webhookUrl = webhookServer.getWebhookUrl("markOperationWebhook")

        // First move - X places at (1,1)
        val status1 = """{"board":[["X",".","."],[".",".","."],[".",".","."]],"winner":"."}"""
        httpClient.send(
            HttpRequest
                .newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(status1))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

        // Second move - O places at (2,2)
        val status2 = """{"board":[["X",".","."],[".",".","."],[".","O","."]],"winner":"."}"""
        httpClient.send(
            HttpRequest
                .newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(status2))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

        // Third move - X wins
        val status3 = """{"board":[["X","X","X"],[".",".","."],[".","O","."]],"winner":"X"}"""
        httpClient.send(
            HttpRequest
                .newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(status3))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

        // All three calls captured
        WebhookAssertions(webhookServer)
            .assertReceivedCount("markOperationWebhook", 3)

        // Last call shows winner
        val calls = webhookServer.getReceived("markOperationWebhook")
        assertTrue(calls.last().body.contains(""""winner":"X""""))
    }

    @Test
    @DisplayName("Webhook server returns correct URL for operation")
    fun `should return correct webhook URL`() {
        val port = webhookServer.start()

        val expectedUrl = "http://localhost:$port/webhook/markOperationWebhook"
        assertEquals(expectedUrl, webhookServer.getWebhookUrl("markOperationWebhook"))
    }
}
