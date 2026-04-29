package org.berrycrush.autotest

/**
 * Result of a multi-request idempotency test.
 *
 * Contains the results of all requests executed during a multi-test,
 * along with aggregate information about timing and success.
 *
 * @property mode The execution mode (sequential or concurrent)
 * @property requestCount The number of requests that were executed
 * @property results Individual results for each request
 * @property totalDurationMs Total time taken for all requests (wall clock time)
 * @property passed Whether the test passed (all requests met criteria)
 * @property failureReason Description of why the test failed, if applicable
 */
data class MultiTestResult(
    val mode: MultiMode,
    val requestCount: Int,
    val results: List<RequestResult>,
    val totalDurationMs: Long,
    val passed: Boolean,
    val failureReason: String? = null,
) {
    /**
     * Check if all responses have the same status code.
     */
    fun hasConsistentStatusCodes(): Boolean = results.map { it.statusCode }.distinct().size == 1

    /**
     * Get all unique status codes from the responses.
     */
    fun getStatusCodes(): Set<Int> = results.map { it.statusCode }.toSet()

    /**
     * Get the average response time in milliseconds.
     */
    fun averageResponseTimeMs(): Double = if (results.isEmpty()) 0.0 else results.map { it.durationMs }.average()

    /**
     * Get the minimum response time in milliseconds.
     */
    fun minResponseTimeMs(): Long = results.minOfOrNull { it.durationMs } ?: 0L

    /**
     * Get the maximum response time in milliseconds.
     */
    fun maxResponseTimeMs(): Long = results.maxOfOrNull { it.durationMs } ?: 0L
}

/**
 * Result of a single request in a multi-request test.
 *
 * Captures all relevant information about a single HTTP request/response
 * for comparison and verification.
 *
 * @property requestIndex Zero-based index of this request in the sequence
 * @property statusCode HTTP status code of the response
 * @property body Response body (parsed or raw)
 * @property headers Response headers
 * @property durationMs Time taken for this request in milliseconds
 * @property threadName Name of the thread that executed this request (for concurrent tests)
 * @property timestamp Unix timestamp when the request completed
 */
data class RequestResult(
    val requestIndex: Int,
    val statusCode: Int,
    val body: Any?,
    val headers: Map<String, String>,
    val durationMs: Long,
    val threadName: String? = null,
    val timestamp: Long,
) {
    companion object {
        /**
         * Create a RequestResult for the current thread.
         */
        fun create(
            requestIndex: Int,
            statusCode: Int,
            body: Any?,
            headers: Map<String, String>,
            durationMs: Long,
        ): RequestResult =
            RequestResult(
                requestIndex = requestIndex,
                statusCode = statusCode,
                body = body,
                headers = headers,
                durationMs = durationMs,
                threadName = Thread.currentThread().name,
                timestamp = System.currentTimeMillis(),
            )
    }
}
