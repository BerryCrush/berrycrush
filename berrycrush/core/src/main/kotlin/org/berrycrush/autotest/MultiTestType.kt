package org.berrycrush.autotest

interface MultiTestType {
    val name: String
    val value: String
}

/**
 * Execution mode for multi-request idempotency tests.
 *
 * Multi-tests verify that an API operation produces consistent results
 * when invoked multiple times, either in sequence or concurrently.
 */
enum class MultiMode(
    override val value: String,
) : MultiTestType {
    /**
     * Send requests one after another.
     *
     * Each request completes before the next begins.
     * Useful for testing that repeated calls produce idempotent results.
     */
    SEQUENTIAL("sequential"),

    /**
     * Send requests concurrently.
     *
     * All requests are dispatched in parallel.
     * Useful for detecting race conditions and concurrent access issues.
     */
    CONCURRENT("concurrent"),
}
