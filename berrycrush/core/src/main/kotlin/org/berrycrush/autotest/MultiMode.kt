package org.berrycrush.autotest

/**
 * Execution mode for multi-request idempotency tests.
 *
 * Multi-tests verify that an API operation produces consistent results
 * when invoked multiple times, either in sequence or concurrently.
 */
enum class MultiMode {
    /**
     * Send requests one after another.
     *
     * Each request completes before the next begins.
     * Useful for testing that repeated calls produce idempotent results.
     */
    SEQUENTIAL,

    /**
     * Send requests concurrently.
     *
     * All requests are dispatched in parallel.
     * Useful for detecting race conditions and concurrent access issues.
     */
    CONCURRENT,
}
