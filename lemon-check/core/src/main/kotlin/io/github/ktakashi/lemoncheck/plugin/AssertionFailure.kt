package io.github.ktakashi.lemoncheck.plugin

import java.time.Instant

/**
 * Detailed failure information for debugging test failures.
 *
 * Captures expected vs actual values, computed diffs, and HTTP request/response
 * snapshots at the time of failure to aid in diagnosing what went wrong.
 *
 * @property message Human-readable failure message explaining what failed
 * @property expected Expected value for comparison assertions (null if not applicable)
 * @property actual Actual value that was received (null if not applicable)
 * @property diff Computed difference between expected and actual (for string/object comparisons)
 * @property stepDescription Description of the step that failed
 * @property assertionType Type of assertion that failed (e.g., "status", "jsonpath", "header")
 * @property requestSnapshot HTTP request details captured at failure time (null if step had no request)
 * @property responseSnapshot HTTP response details captured at failure time (null if step had no response)
 */
data class AssertionFailure(
    val message: String,
    val expected: Any? = null,
    val actual: Any? = null,
    val diff: String? = null,
    val stepDescription: String,
    val assertionType: String,
    val requestSnapshot: HttpRequest? = null,
    val responseSnapshot: HttpResponse? = null,
)

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
    val method: String,
    val url: String,
    val headers: Map<String, List<String>>,
    val body: String? = null,
    val timestamp: Instant,
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
    val duration: java.time.Duration,
    val timestamp: Instant,
)

/**
 * Result status for steps and scenarios.
 *
 * @property PASSED Step or scenario succeeded
 * @property FAILED Assertion failure occurred
 * @property SKIPPED Not executed due to dependency failure
 * @property ERROR Unexpected exception occurred during execution
 */
enum class ResultStatus {
    PASSED,
    FAILED,
    SKIPPED,
    ERROR,
}

/**
 * Step type enumeration.
 *
 * @property CALL HTTP API call step
 * @property ASSERT Assertion/validation step
 * @property EXTRACT Variable extraction step
 * @property CUSTOM User-defined custom step
 */
enum class StepType {
    CALL,
    ASSERT,
    EXTRACT,
    CUSTOM,
}
