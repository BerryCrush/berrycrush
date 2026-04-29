package org.berrycrush.autotest

/**
 * Constants for multi-test configuration parameters.
 *
 * These parameters are used in the `parameters` block of scenario files
 * or Kotlin DSL to configure multi-request idempotency tests.
 *
 * ## Usage
 *
 * ### Scenario File
 * ```
 * parameters:
 *   multiTestSequentialCount: 5
 *   multiTestConcurrentCount: 10
 * ```
 *
 * ### Kotlin DSL
 * ```kotlin
 * parameters {
 *     multiTestSequentialCount = 5
 *     multiTestConcurrentCount = 10
 * }
 * ```
 */
object MultiTestParameters {
    /**
     * Parameter name for sequential request count.
     *
     * Controls how many requests are sent sequentially in multi-tests.
     * Default value: 3
     */
    const val SEQUENTIAL_COUNT = "multiTestSequentialCount"

    /**
     * Parameter name for concurrent request count.
     *
     * Controls how many requests are sent concurrently in multi-tests.
     * Default value: 5
     */
    const val CONCURRENT_COUNT = "multiTestConcurrentCount"

    /**
     * Default values for multi-test parameters.
     */
    val DEFAULTS: Map<String, Int> =
        mapOf(
            SEQUENTIAL_COUNT to 3,
            CONCURRENT_COUNT to 5,
        )

    /**
     * Get the sequential count from a parameters map.
     *
     * @param parameters The parameters map to read from
     * @return The sequential count, or the default if not specified
     */
    fun getSequentialCount(parameters: Map<String, Any?>): Int =
        (parameters[SEQUENTIAL_COUNT] as? Number)?.toInt()
            ?: DEFAULTS.getValue(SEQUENTIAL_COUNT)

    /**
     * Get the concurrent count from a parameters map.
     *
     * @param parameters The parameters map to read from
     * @return The concurrent count, or the default if not specified
     */
    fun getConcurrentCount(parameters: Map<String, Any?>): Int =
        (parameters[CONCURRENT_COUNT] as? Number)?.toInt()
            ?: DEFAULTS.getValue(CONCURRENT_COUNT)
}
