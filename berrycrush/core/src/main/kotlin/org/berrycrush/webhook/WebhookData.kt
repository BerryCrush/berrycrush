package org.berrycrush.webhook

/**
 * Represents an expected webhook call for testing.
 */
data class WebhookExpectation(
    /**
     * Expected request body (can be a pattern or exact match).
     */
    val body: Any? = null,
    /**
     * Expected headers.
     */
    val headers: Map<String, String> = emptyMap(),
    /**
     * Expected content type.
     */
    val contentType: String = "application/json",
    /**
     * How many times we expect this webhook to be called.
     * -1 means any number of times.
     */
    val expectedCount: Int = -1,
)

/**
 * Represents a received webhook call.
 */
data class WebhookCall(
    /**
     * The operation ID of the webhook.
     */
    val operationId: String,
    /**
     * The request body as a string.
     */
    val body: String,
    /**
     * The request headers.
     */
    val headers: Map<String, List<String>>,
    /**
     * The content type of the request.
     */
    val contentType: String?,
    /**
     * Timestamp when the webhook was received.
     */
    val receivedAt: Long = System.currentTimeMillis(),
)
