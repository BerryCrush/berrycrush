package org.berrycrush.samples.tictactoe.service

import org.berrycrush.samples.tictactoe.model.GameStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

/**
 * Service for sending webhook notifications when game state changes.
 *
 * The webhook URL is configurable via the `tictactoe.webhook.url` property.
 * If the URL is not set or is blank, webhooks are disabled.
 */
@Service
class WebhookService(
    @param:Value("\${tictactoe.webhook.url:}") private val webhookUrl: String,
) {
    private val logger = LoggerFactory.getLogger(WebhookService::class.java)
    private val restClient = RestClient.create()

    /**
     * Sends a webhook notification with the current game status.
     *
     * @param status The game status to send
     * @param operationId The operation ID (for webhook identification)
     */
    fun sendStatusWebhook(
        status: GameStatus,
        operationId: String = "markOperationWebhook",
    ) {
        if (webhookUrl.isBlank()) {
            logger.debug("Webhook URL not configured, skipping webhook")
            return
        }

        try {
            val url =
                if (webhookUrl.endsWith("/")) {
                    "$webhookUrl$operationId"
                } else {
                    "$webhookUrl/$operationId"
                }

            logger.info("Sending webhook to: $url")

            restClient
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(status)
                .retrieve()
                .toBodilessEntity()

            logger.info("Webhook sent successfully to $url")
        } catch (e: Exception) {
            logger.warn("Failed to send webhook to $webhookUrl: ${e.message}")
            // Don't fail the main operation if webhook fails
        }
    }

    /**
     * Sends a status callback to the specified URL (from progressUrl header).
     *
     * This implements the OpenAPI 3.1 callback pattern where the callback URL
     * is provided in the request header.
     *
     * @param status The game status to send
     * @param callbackUrl The URL to send the callback to
     */
    fun sendStatusCallback(
        status: GameStatus,
        callbackUrl: String,
    ) {
        try {
            logger.info("Sending status callback to: $callbackUrl")

            restClient
                .post()
                .uri(callbackUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(status)
                .retrieve()
                .toBodilessEntity()

            logger.info("Status callback sent successfully to $callbackUrl")
        } catch (e: Exception) {
            logger.warn("Failed to send status callback to $callbackUrl: ${e.message}")
            // Don't fail the main operation if callback fails
        }
    }
}
