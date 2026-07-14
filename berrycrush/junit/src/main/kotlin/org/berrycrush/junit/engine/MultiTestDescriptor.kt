package org.berrycrush.junit.engine

import org.berrycrush.autotest.MultiTestResult

/**
 * Test descriptor for a multi-request idempotency test.
 *
 * Represents a single multi-test mode (sequential or concurrent) in the JUnit test tree.
 */
object MultiTestDescriptor {
    /**
     * Create a display name for a multi-test.
     * Format: [multi:{mode}] {count} requests
     */
    fun createDisplayName(
        mode: String,
        requestCount: Int,
    ): String {
        val modeLabel = mode.lowercase()
        return "[multi:$modeLabel] $requestCount requests"
    }

    /**
     * Build a failure message for a multi-test result.
     */
    fun buildFailureMessage(result: MultiTestResult): String =
        buildString {
            append(createDisplayName(result.mode, result.requestCount))
            append("\n")
            append("  ${result.failureReason ?: "Unknown failure"}")
            append("\n  Duration: ${result.totalDuration}ms")
            append("\n  Results:")
            result.results.forEach { requestResult ->
                append("\n    Request #${requestResult.requestIndex + 1}: ")
                append("status=${requestResult.response?.statusCode}, ")
                append("time=${requestResult.response?.duration?.toMillis() ?: 0}ms")
                requestResult.threadName?.let { append(", thread=$it") }
            }
        }
}
