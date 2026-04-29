package org.berrycrush.dsl

import org.berrycrush.autotest.MultiMode
import org.berrycrush.autotest.RequestResult

/**
 * Context available during multi-request idempotency test execution.
 *
 * Provides information about the multi-test results, including
 * all responses, timing data, and pass/fail status.
 *
 * Usage in DSL:
 * ```kotlin
 * scenario("Test API") {
 *     when_("Create pet") {
 *         call("createPet") {
 *             autoTest(AutoTestType.MULTI)
 *         }
 *         // During multi-test execution, multiTestContext is available
 *     }
 * }
 * ```
 *
 * @property mode The multi-test mode (SEQUENTIAL or CONCURRENT)
 * @property responses List of all request results
 * @property totalDurationMs Total duration of the multi-test in milliseconds
 * @property passed Whether all responses are consistent
 */
data class MultiTestContext(
    val mode: MultiMode,
    val responses: List<RequestResult>,
    val totalDurationMs: Long,
    val passed: Boolean,
) {
    companion object {
        /**
         * Context key for storing MultiTestContext in ExecutionContext.
         */
        const val CONTEXT_KEY = "__multiTestContext"
    }

    /**
     * Number of successful requests (status code 2xx).
     */
    val successCount: Int
        get() = responses.count { it.statusCode in 200..299 }

    /**
     * Number of failed requests (status code not 2xx).
     */
    val failureCount: Int
        get() = responses.count { it.statusCode !in 200..299 }

    /**
     * Average response time in milliseconds.
     */
    val avgResponseTimeMs: Long
        get() = if (responses.isEmpty()) 0 else responses.sumOf { it.durationMs } / responses.size

    /**
     * Get all unique status codes from responses.
     */
    val uniqueStatusCodes: Set<Int>
        get() = responses.map { it.statusCode }.toSet()

    /**
     * Whether all responses have the same status code.
     */
    val hasConsistentStatusCodes: Boolean
        get() = uniqueStatusCodes.size <= 1
}
