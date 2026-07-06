package org.berrycrush.model

import java.time.Duration
import java.time.Instant

/**
 * Snapshot of HTTP request details at a point in time.
 *
 * Immutable representation of an HTTP request for logging, reporting, and debugging.
 *
 * @property method HTTP method (GET, POST, PUT, DELETE, etc.)
 * @property url Complete URL target of the request
 * @property headers Request headers (header name -> list of values)
 * @property body Request body content (null if no body)
 * @property timestamp When the request was sent
 */
data class HttpRequest(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String>,
    val body: String? = null,
    val timestamp: Instant = Instant.now(),
)

/**
 * Snapshot of HTTP response details at a point in time.
 *
 * Immutable representation of an HTTP response for logging, reporting, and debugging.
 *
 * @property statusCode HTTP status code (200, 404, 500, etc.)
 * @property statusMessage HTTP status message ("OK", "Not Found", etc.)
 * @property headers Response headers (header name -> list of values)
 * @property body Response body content (null if no body)
 * @property duration Time elapsed to receive the response
 * @property timestamp When the response was received
 */
data class HttpResponse(
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, List<String>>,
    val body: String? = null,
    val duration: Duration,
    val timestamp: Instant,
    val request: HttpRequest,
    val error: Throwable? = null,
)
