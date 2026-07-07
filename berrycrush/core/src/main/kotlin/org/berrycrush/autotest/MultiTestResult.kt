package org.berrycrush.autotest

import org.berrycrush.model.HttpResponse
import java.time.Duration

/**
 * Result of a multi-request idempotency test.
 *
 * Contains the results of all requests executed during a multi-test,
 * along with aggregate information about timing and success.
 *
 * @property mode The execution mode (sequential or concurrent)
 * @property requestCount The number of requests that were executed
 * @property results Individual results for each request
 * @property totalDuration Total time taken for all requests (wall clock time)
 * @property passed Whether the test passed (all requests met criteria)
 * @property failureReason Description of why the test failed, if applicable
 */
data class MultiTestResult(
    val mode: MultiTestType,
    val requestCount: Int,
    val results: List<RequestResult>,
    val totalDuration: Duration,
    val passed: Boolean,
    val failureReason: String? = null,
) {
    /**
     * Check if all responses have the same status code.
     */
    fun hasConsistentStatusCodes(): Boolean = results.map { it.response?.statusCode }.distinct().size == 1

    /**
     * Get all unique status codes from the responses.
     */
    fun getStatusCodes(): Set<Int> = results.mapNotNull { it.response?.statusCode }.toSet()

    /**
     * Get the average response time in milliseconds.
     */
    fun averageResponseTimeMs(): Double = if (results.isEmpty()) 0.0 else results.mapNotNull { it.response?.duration?.toMillis() }.average()

    /**
     * Get the minimum response time in milliseconds.
     */
    fun minResponseTimeMs(): Long = results.mapNotNull { it.response?.duration?.toMillis() }.minOrNull() ?: 0L

    /**
     * Get the maximum response time in milliseconds.
     */
    fun maxResponseTimeMs(): Long = results.mapNotNull { it.response?.duration?.toMillis() }.maxOrNull() ?: 0L
}

/**
 * Result of a single request in a multi-request test.
 *
 * Captures all relevant information about a single HTTP request/response
 * for comparison and verification.
 *
 * @property requestIndex Zero-based index of this request in the sequence
 * @property response HTTP response (if available)
 * @property duration Time taken for this request
 * @property threadName Name of the thread that executed this request (for concurrent tests)
 */
data class RequestResult(
    val requestIndex: Int,
    val response: HttpResponse?,
    val threadName: String? = null,
    val duration: Duration,
) {
    companion object {
        /**
         * Create a RequestResult for the current thread.
         */
        fun create(
            requestIndex: Int,
            response: HttpResponse? = null,
            duration: Duration? = null,
        ): RequestResult =
            RequestResult(
                requestIndex = requestIndex,
                response = response,
                threadName = Thread.currentThread().name,
                duration = response?.duration ?: duration ?: Duration.ZERO,
            )
    }
}
