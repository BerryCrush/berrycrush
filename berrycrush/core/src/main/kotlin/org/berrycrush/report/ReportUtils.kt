package org.berrycrush.report

import org.berrycrush.plugin.HttpResponse
import org.berrycrush.plugin.ResultStatus
import org.berrycrush.plugin.StepResult
import java.time.Instant

/**
 * Build a [StepReportEntry] from a [StepResult].
 *
 * This shared function encapsulates the common logic for creating step report entries
 * across different report plugins.
 *
 * @param stepResult The step result to convert
 * @param statusMapper Optional function to map status values (defaults to identity)
 * @return A fully populated [StepReportEntry]
 */
internal fun buildStepReportEntry(
    stepResult: StepResult,
    statusMapper: (ResultStatus) -> ResultStatus = { it },
): StepReportEntry =
    StepReportEntry(
        description = stepResult.stepDescription,
        status = statusMapper(stepResult.status),
        duration = stepResult.duration,
        request = null,
        response =
            if (stepResult.httpStatusCode != null) {
                HttpResponse(
                    statusCode = stepResult.httpStatusCode!!,
                    statusMessage = httpStatusMessage(stepResult.httpStatusCode!!),
                    headers = stepResult.responseHeaders,
                    body = stepResult.responseBody,
                    duration = stepResult.duration,
                    timestamp = Instant.now(),
                )
            } else {
                null
            },
        failure = stepResult.failure,
        isCustomStep = stepResult.isCustomStep,
    )

/**
 * HTTP status code to message mapping.
 */
@Suppress("MagicNumber")
private val HTTP_STATUS_MESSAGES =
    mapOf(
        200 to "OK",
        201 to "Created",
        204 to "No Content",
        400 to "Bad Request",
        401 to "Unauthorized",
        403 to "Forbidden",
        404 to "Not Found",
        405 to "Method Not Allowed",
        409 to "Conflict",
        422 to "Unprocessable Entity",
        500 to "Internal Server Error",
        502 to "Bad Gateway",
        503 to "Service Unavailable",
    )

/**
 * Get HTTP status message for common status codes.
 *
 * This is a shared utility function used across report plugins.
 */
internal fun httpStatusMessage(statusCode: Int): String = HTTP_STATUS_MESSAGES[statusCode] ?: ""
