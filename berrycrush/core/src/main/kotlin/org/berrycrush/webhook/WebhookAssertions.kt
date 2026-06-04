package org.berrycrush.webhook

import com.jayway.jsonpath.JsonPath
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * Assertion utilities for verifying webhook deliveries.
 *
 * Provides fluent assertion methods for checking webhook calls
 * captured by [MockWebhookServer].
 *
 * Example usage:
 * ```kotlin
 * val server = MockWebhookServer()
 * server.expect("onPetAdopted")
 * server.start()
 *
 * // ... trigger webhook in your application ...
 *
 * WebhookAssertions(server)
 *     .assertReceived("onPetAdopted")
 *     .assertBodyContains("$.petId", 123)
 *     .assertReceivedCount("onPetAdopted", 1)
 * ```
 */
class WebhookAssertions(
    private val server: MockWebhookServer,
) {
    private val objectMapper = jacksonObjectMapper()
    private var lastOperationId: String? = null

    /**
     * Assert that a webhook was received.
     *
     * @param operationId The webhook operationId to check
     * @return This instance for chaining
     * @throws AssertionError if the webhook was not received
     */
    fun assertReceived(operationId: String): WebhookAssertions {
        lastOperationId = operationId
        if (!server.wasReceived(operationId)) {
            throw AssertionError("Expected webhook '$operationId' to be received, but it was not")
        }
        return this
    }

    /**
     * Assert that a webhook was NOT received.
     *
     * @param operationId The webhook operationId to check
     * @return This instance for chaining
     * @throws AssertionError if the webhook was received
     */
    fun assertNotReceived(operationId: String): WebhookAssertions {
        if (server.wasReceived(operationId)) {
            throw AssertionError("Expected webhook '$operationId' to NOT be received, but it was")
        }
        return this
    }

    /**
     * Assert the number of times a webhook was received.
     *
     * @param operationId The webhook operationId to check
     * @param expectedCount Expected number of times the webhook was called
     * @return This instance for chaining
     * @throws AssertionError if the count doesn't match
     */
    fun assertReceivedCount(
        operationId: String,
        expectedCount: Int,
    ): WebhookAssertions {
        lastOperationId = operationId
        val actual = server.getReceivedCount(operationId)
        if (actual != expectedCount) {
            throw AssertionError(
                "Expected webhook '$operationId' to be received $expectedCount time(s), but was received $actual time(s)",
            )
        }
        return this
    }

    /**
     * Assert that the webhook body contains a specific value at a JSON path.
     *
     * Uses the most recent call for the last checked webhook.
     *
     * @param jsonPath The JSON path to check
     * @param expectedValue The expected value at that path
     * @return This instance for chaining
     * @throws AssertionError if the value doesn't match or path doesn't exist
     */
    @Suppress("ThrowsCount")
    fun assertBodyContains(
        jsonPath: String,
        expectedValue: Any?,
    ): WebhookAssertions {
        val operationId =
            lastOperationId
                ?: throw IllegalStateException("No webhook checked yet. Call assertReceived() first.")

        val calls = server.getReceived(operationId)
        if (calls.isEmpty()) {
            throw AssertionError("No calls received for webhook '$operationId'")
        }

        val lastCall = calls.last()
        val actual =
            try {
                JsonPath.read<Any?>(lastCall.body, jsonPath)
            } catch (e: Exception) {
                throw AssertionError("JSON path '$jsonPath' not found in webhook body: ${lastCall.body}", e)
            }

        if (actual != expectedValue) {
            throw AssertionError(
                "Expected value at '$jsonPath' to be $expectedValue but was $actual",
            )
        }
        return this
    }

    /**
     * Assert that the webhook body matches the expected JSON structure.
     *
     * @param operationId The webhook operationId to check
     * @param expectedBody The expected body as a map or object
     * @return This instance for chaining
     * @throws AssertionError if the body doesn't match
     */
    fun assertBodyEquals(
        operationId: String,
        expectedBody: Map<String, Any?>,
    ): WebhookAssertions {
        lastOperationId = operationId
        val calls = server.getReceived(operationId)
        if (calls.isEmpty()) {
            throw AssertionError("No calls received for webhook '$operationId'")
        }

        val lastCall = calls.last()
        val actualBody = objectMapper.readValue(lastCall.body, Map::class.java)

        expectedBody.forEach { (key, expectedValue) ->
            val actualValue = actualBody[key]
            if (actualValue != expectedValue) {
                throw AssertionError(
                    "Expected '$key' to be $expectedValue but was $actualValue in webhook '$operationId'",
                )
            }
        }
        return this
    }

    /**
     * Assert that the webhook was received with a specific content type.
     *
     * @param operationId The webhook operationId to check
     * @param expectedContentType The expected content type
     * @return This instance for chaining
     * @throws AssertionError if the content type doesn't match
     */
    fun assertContentType(
        operationId: String,
        expectedContentType: String,
    ): WebhookAssertions {
        lastOperationId = operationId
        val calls = server.getReceived(operationId)
        if (calls.isEmpty()) {
            throw AssertionError("No calls received for webhook '$operationId'")
        }

        val lastCall = calls.last()
        val actual = lastCall.contentType

        if (actual == null || !actual.contains(expectedContentType)) {
            throw AssertionError(
                "Expected content type '$expectedContentType' but got '$actual' for webhook '$operationId'",
            )
        }
        return this
    }

    /**
     * Assert that the webhook was received with a specific header.
     *
     * @param operationId The webhook operationId to check
     * @param headerName The header name (case-insensitive)
     * @param expectedValue The expected header value
     * @return This instance for chaining
     * @throws AssertionError if the header doesn't match
     */
    fun assertHeader(
        operationId: String,
        headerName: String,
        expectedValue: String,
    ): WebhookAssertions {
        lastOperationId = operationId
        val calls = server.getReceived(operationId)
        if (calls.isEmpty()) {
            throw AssertionError("No calls received for webhook '$operationId'")
        }

        val lastCall = calls.last()
        val headerValues =
            lastCall.headers.entries
                .firstOrNull { it.key.equals(headerName, ignoreCase = true) }
                ?.value

        if (headerValues == null || !headerValues.contains(expectedValue)) {
            throw AssertionError(
                "Expected header '$headerName' to contain '$expectedValue' but got $headerValues for webhook '$operationId'",
            )
        }
        return this
    }

    /**
     * Get all received calls for a webhook (for custom assertions).
     *
     * @param operationId The webhook operationId
     * @return List of received webhook calls
     */
    fun getCalls(operationId: String): List<WebhookCall> = server.getReceived(operationId)

    /**
     * Get the last received call for a webhook.
     *
     * @param operationId The webhook operationId
     * @return The most recent webhook call, or null if none received
     */
    fun getLastCall(operationId: String): WebhookCall? = server.getReceived(operationId).lastOrNull()
}
