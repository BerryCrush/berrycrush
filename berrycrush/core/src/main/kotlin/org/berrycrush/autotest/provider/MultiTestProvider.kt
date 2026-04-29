package org.berrycrush.autotest.provider

import org.berrycrush.autotest.MultiTestResult
import org.berrycrush.autotest.RequestResult

/**
 * Provider interface for executing multi-request idempotency tests.
 *
 * Implement this interface to add custom multi-test execution strategies.
 * Providers are discovered via ServiceLoader, allowing extensions without
 * modifying the core library.
 *
 * ## Built-in Providers
 *
 * - `sequential` - Executes requests one after another
 * - `concurrent` - Executes requests in parallel
 *
 * ## Example Implementation
 *
 * ```kotlin
 * class DelayedSequentialProvider : MultiTestProvider {
 *     override val testType: String = "delayedSequential"
 *     override val displayName: String = "Delayed Sequential"
 *
 *     override fun executeMultiTest(
 *         count: Int,
 *         executor: () -> RequestResult,
 *     ): MultiTestResult {
 *         val results = (0 until count).map { index ->
 *             Thread.sleep(1000) // Add delay between requests
 *             executor()
 *         }
 *         // ... build result
 *     }
 * }
 * ```
 *
 * ## Registration
 *
 * Add to `META-INF/services/org.berrycrush.autotest.provider.MultiTestProvider`:
 * ```
 * com.example.DelayedSequentialProvider
 * ```
 *
 * @see AutoTestProviderRegistry
 * @see MultiTestResult
 */
interface MultiTestProvider {
    /**
     * Unique identifier for this multi-test mode.
     *
     * Used for:
     * - Display name in test reports: `[multi:{testType}]`
     * - Exclude configuration: `excludes: [{testType}]`
     * - User-provided providers override built-in ones with same testType
     */
    val testType: String

    /**
     * Human-readable display name for test reports.
     *
     * Defaults to [testType] if not overridden.
     */
    val displayName: String get() = testType

    /**
     * Execute multi-request test.
     *
     * The provider should execute the given executor function [count] times
     * according to its execution strategy (e.g., sequential, concurrent).
     *
     * @param count Number of requests to execute
     * @param executor Function that executes a single request and returns the result
     * @return Results of all requests and aggregate information
     */
    fun executeMultiTest(
        count: Int,
        executor: (requestIndex: Int) -> RequestResult,
    ): MultiTestResult

    /**
     * Priority of this provider. Higher values = higher priority.
     *
     * User-provided providers default to 100, built-in providers default to 0.
     * When multiple providers have the same [testType], the one with higher
     * priority is used.
     */
    val priority: Int get() = 0
}
