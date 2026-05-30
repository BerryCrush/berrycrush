package org.berrycrush.config

import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.reflect.KClass

/**
 * Configuration for HTTP request retry behavior.
 *
 * This configuration controls automatic retries for failed HTTP requests,
 * including which status codes and exceptions trigger retries, how many
 * attempts to make, and the delay between attempts.
 *
 * @property maxAttempts Maximum number of retry attempts (0 = disabled, does not include initial request)
 * @property delay Initial delay between retry attempts
 * @property maxDelay Maximum delay cap (for exponential backoff)
 * @property backoff Strategy for increasing delay between attempts
 * @property jitter Whether to add randomness to delays (prevents thundering herd)
 * @property retryOnStatusCodes HTTP status codes that trigger retry
 * @property retryOnExceptions Exception types that trigger retry
 */
data class RetryConfig(
    val maxAttempts: Int = 0,
    val delay: Duration = DEFAULT_DELAY,
    val maxDelay: Duration = DEFAULT_MAX_DELAY,
    val backoff: BackoffStrategy = BackoffStrategy.EXPONENTIAL,
    val jitter: Boolean = true,
    val retryOnStatusCodes: Set<Int> = DEFAULT_RETRY_STATUS_CODES,
    val retryOnExceptions: Set<KClass<out Exception>> = DEFAULT_RETRY_EXCEPTIONS,
) {
    /**
     * Whether retry is enabled (maxAttempts > 0).
     */
    val isEnabled: Boolean get() = maxAttempts > 0

    /**
     * Calculate delay for a specific attempt number.
     *
     * @param attempt The attempt number (0-based, where 0 is after first failure)
     * @return The delay duration to wait before the next attempt
     */
    fun calculateDelay(attempt: Int): Duration {
        val baseDelay =
            when (backoff) {
                BackoffStrategy.FIXED -> delay
                BackoffStrategy.LINEAR -> delay.multipliedBy((attempt + 1).toLong())
                BackoffStrategy.EXPONENTIAL -> delay.multipliedBy(1L shl attempt.coerceAtMost(MAX_EXPONENT))
            }

        val cappedDelay = if (baseDelay > maxDelay) maxDelay else baseDelay

        return if (jitter) {
            addJitter(cappedDelay)
        } else {
            cappedDelay
        }
    }

    /**
     * Check if a status code should trigger a retry.
     */
    fun shouldRetryStatus(statusCode: Int): Boolean = statusCode in retryOnStatusCodes

    /**
     * Check if an exception should trigger a retry.
     */
    fun shouldRetryException(exception: Exception): Boolean = retryOnExceptions.any { it.isInstance(exception) }

    private fun addJitter(duration: Duration): Duration {
        // Add ±20% jitter using ThreadLocalRandom for thread safety
        val jitterFactor = ThreadLocalRandom.current().nextDouble(JITTER_MIN, JITTER_MAX)
        return Duration.ofMillis((duration.toMillis() * jitterFactor).toLong())
    }

    companion object {
        private const val MAX_EXPONENT = 10
        private const val JITTER_MIN = 0.8
        private const val JITTER_MAX = 1.2
        private const val DEFAULT_DELAY_SECONDS = 1L
        private const val DEFAULT_MAX_DELAY_SECONDS = 30L

        /**
         * Default initial delay between retry attempts.
         */
        val DEFAULT_DELAY: Duration = Duration.ofSeconds(DEFAULT_DELAY_SECONDS)

        /**
         * Default maximum delay cap for backoff.
         */
        val DEFAULT_MAX_DELAY: Duration = Duration.ofSeconds(DEFAULT_MAX_DELAY_SECONDS)

        /**
         * Default status codes that trigger retry.
         * - 429: Too Many Requests (rate limiting)
         * - 502: Bad Gateway
         * - 503: Service Unavailable
         * - 504: Gateway Timeout
         */
        val DEFAULT_RETRY_STATUS_CODES: Set<Int> = setOf(429, 502, 503, 504)

        /**
         * Default exception types that trigger retry.
         */
        val DEFAULT_RETRY_EXCEPTIONS: Set<KClass<out Exception>> =
            setOf(
                java.net.SocketTimeoutException::class,
                java.net.ConnectException::class,
                java.net.SocketException::class,
            )

        /**
         * Disabled retry configuration.
         */
        val DISABLED = RetryConfig(maxAttempts = 0)

        /**
         * Default retry configuration with 3 attempts.
         */
        val DEFAULT = RetryConfig(maxAttempts = 3)
    }
}

/**
 * Strategy for calculating delay between retry attempts.
 */
enum class BackoffStrategy {
    /**
     * Fixed delay: same delay for every attempt.
     * Example: 1s, 1s, 1s, ...
     */
    FIXED,

    /**
     * Linear backoff: delay increases linearly with attempt number.
     * Example: 1s, 2s, 3s, 4s, ...
     */
    LINEAR,

    /**
     * Exponential backoff: delay doubles with each attempt.
     * Example: 1s, 2s, 4s, 8s, ...
     */
    EXPONENTIAL,
}

/**
 * Builder for [RetryConfig] used in DSL configuration.
 *
 * Example:
 * ```kotlin
 * retry {
 *     maxAttempts = 3
 *     delay = Duration.ofSeconds(1)
 *     backoff = BackoffStrategy.EXPONENTIAL
 * }
 * ```
 */
class RetryConfigBuilder(
    baseConfig: RetryConfig = RetryConfig.DISABLED,
) {
    var maxAttempts: Int = baseConfig.maxAttempts
    var delay: Duration = baseConfig.delay
    var maxDelay: Duration = baseConfig.maxDelay
    var backoff: BackoffStrategy = baseConfig.backoff
    var jitter: Boolean = baseConfig.jitter
    var retryOnStatusCodes: Set<Int> = baseConfig.retryOnStatusCodes
    var retryOnExceptions: Set<KClass<out Exception>> = baseConfig.retryOnExceptions

    /**
     * Set delay in milliseconds.
     */
    fun delayMs(milliseconds: Long) {
        delay = Duration.ofMillis(milliseconds)
    }

    /**
     * Set delay in seconds.
     */
    fun delaySeconds(seconds: Long) {
        delay = Duration.ofSeconds(seconds)
    }

    /**
     * Set max delay in seconds.
     */
    fun maxDelaySeconds(seconds: Long) {
        maxDelay = Duration.ofSeconds(seconds)
    }

    /**
     * Add status codes to retry on.
     */
    fun retryOn(vararg statusCodes: Int) {
        retryOnStatusCodes = retryOnStatusCodes + statusCodes.toSet()
    }

    /**
     * Build the [RetryConfig] from this builder.
     */
    fun build(): RetryConfig =
        RetryConfig(
            maxAttempts = maxAttempts,
            delay = delay,
            maxDelay = maxDelay,
            backoff = backoff,
            jitter = jitter,
            retryOnStatusCodes = retryOnStatusCodes,
            retryOnExceptions = retryOnExceptions,
        )
}
