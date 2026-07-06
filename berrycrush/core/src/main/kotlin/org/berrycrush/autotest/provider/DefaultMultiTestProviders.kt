package org.berrycrush.autotest.provider

import org.berrycrush.autotest.MultiMode
import org.berrycrush.autotest.MultiTestResult
import org.berrycrush.autotest.RequestResult
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Built-in multi-test providers for idempotency testing.
 *
 * These providers implement the two core multi-test modes:
 * - Sequential: Sends requests one after another
 * - Concurrent: Sends requests in parallel using a thread pool
 */
object DefaultMultiTestProviders {
    /**
     * All built-in multi-test providers.
     */
    val all: List<MultiTestProvider> =
        listOf(
            SequentialMultiTestProvider,
            ConcurrentMultiTestProvider,
        )
}

/**
 * Sequential multi-test provider.
 *
 * Executes requests one after another, waiting for each to complete
 * before starting the next. Useful for testing that repeated sequential
 * calls produce idempotent results.
 */
object SequentialMultiTestProvider : MultiTestProvider {
    override val testType: String = "sequential"
    override val displayName: String = "Sequential Idempotency"

    override fun executeMultiTest(
        count: Int,
        executor: (requestIndex: Int) -> RequestResult,
    ): MultiTestResult {
        val startTime = Instant.now()
        val results =
            (0 until count).map { index ->
                executor(index)
            }
        val totalDuration = Duration.between(startTime, Instant.now())

        return buildResult(
            mode = MultiMode.SEQUENTIAL,
            results = results,
            totalDuration = totalDuration,
        )
    }
}

/**
 * Concurrent multi-test provider.
 *
 * Executes all requests in parallel using a thread pool.
 * Useful for detecting race conditions and concurrent access issues.
 */
object ConcurrentMultiTestProvider : MultiTestProvider {
    override val testType: String = "concurrent"
    override val displayName: String = "Concurrent Idempotency"

    override fun executeMultiTest(
        count: Int,
        executor: (requestIndex: Int) -> RequestResult,
    ): MultiTestResult {
        val startTime = Instant.now()
        val threadPool = Executors.newFixedThreadPool(count.coerceAtMost(MAX_THREADS))
        try {
            val futures =
                (0 until count).map { index ->
                    threadPool.submit(Callable { executor(index) })
                }
            val results = futures.map { it.get() }
            val totalDuration = Duration.between(startTime, Instant.now())

            return buildResult(
                mode = MultiMode.CONCURRENT,
                results = results,
                totalDuration = totalDuration,
            )
        } finally {
            threadPool.shutdown()
        }
    }

    private const val MAX_THREADS = 20
}

/**
 * Build a MultiTestResult from the collected results.
 */
private fun buildResult(
    mode: MultiMode,
    results: List<RequestResult>,
    totalDuration: Duration,
): MultiTestResult {
    // Default verification: all responses should have the same status code
    val statusCodes = results.mapNotNull { it.response?.statusCode }.distinct()
    val passed = statusCodes.size == 1
    val failureReason =
        if (!passed) {
            "Inconsistent status codes: ${statusCodes.joinToString(", ")}"
        } else {
            null
        }

    return MultiTestResult(
        mode = mode,
        requestCount = results.size,
        results = results,
        totalDuration = totalDuration,
        passed = passed,
        failureReason = failureReason,
    )
}
