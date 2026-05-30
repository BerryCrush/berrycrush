package org.berrycrush.config

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [RetryConfig] and [BackoffStrategy].
 */
class RetryConfigTest {
    @Test
    fun `DISABLED has zero max attempts`() {
        val config = RetryConfig.DISABLED
        assertFalse(config.isEnabled)
        assertEquals(0, config.maxAttempts)
    }

    @Test
    fun `DEFAULT has three max attempts`() {
        val config = RetryConfig.DEFAULT
        assertTrue(config.isEnabled)
        assertEquals(3, config.maxAttempts)
    }

    @Test
    fun `isEnabled returns true when maxAttempts greater than zero`() {
        val config = RetryConfig(maxAttempts = 1)
        assertTrue(config.isEnabled)
    }

    @Test
    fun `isEnabled returns false when maxAttempts is zero`() {
        val config = RetryConfig(maxAttempts = 0)
        assertFalse(config.isEnabled)
    }

    @Test
    fun `shouldRetryStatus returns true for default retry codes`() {
        val config = RetryConfig.DEFAULT

        // Should retry these
        assertTrue(config.shouldRetryStatus(429))
        assertTrue(config.shouldRetryStatus(502))
        assertTrue(config.shouldRetryStatus(503))
        assertTrue(config.shouldRetryStatus(504))

        // Should not retry these
        assertFalse(config.shouldRetryStatus(200))
        assertFalse(config.shouldRetryStatus(400))
        assertFalse(config.shouldRetryStatus(404))
        assertFalse(config.shouldRetryStatus(500))
    }

    @Test
    fun `shouldRetryStatus respects custom status codes`() {
        val config =
            RetryConfig(
                maxAttempts = 3,
                retryOnStatusCodes = setOf(500, 501),
            )

        assertTrue(config.shouldRetryStatus(500))
        assertTrue(config.shouldRetryStatus(501))
        assertFalse(config.shouldRetryStatus(502))
        assertFalse(config.shouldRetryStatus(429))
    }

    @Test
    fun `shouldRetryException returns true for default exceptions`() {
        val config = RetryConfig.DEFAULT

        assertTrue(config.shouldRetryException(SocketTimeoutException("timeout")))
        assertTrue(config.shouldRetryException(ConnectException("connection refused")))
        assertTrue(config.shouldRetryException(SocketException("socket error")))

        assertFalse(config.shouldRetryException(IllegalArgumentException("bad arg")))
        assertFalse(config.shouldRetryException(RuntimeException("generic error")))
    }

    @Test
    fun `calculateDelay with FIXED strategy returns constant delay`() {
        val config =
            RetryConfig(
                maxAttempts = 3,
                delay = Duration.ofSeconds(1),
                backoff = BackoffStrategy.FIXED,
                jitter = false,
            )

        assertEquals(Duration.ofSeconds(1), config.calculateDelay(0))
        assertEquals(Duration.ofSeconds(1), config.calculateDelay(1))
        assertEquals(Duration.ofSeconds(1), config.calculateDelay(2))
    }

    @Test
    fun `calculateDelay with LINEAR strategy increases linearly`() {
        val config =
            RetryConfig(
                maxAttempts = 3,
                delay = Duration.ofSeconds(1),
                backoff = BackoffStrategy.LINEAR,
                jitter = false,
            )

        assertEquals(Duration.ofSeconds(1), config.calculateDelay(0))
        assertEquals(Duration.ofSeconds(2), config.calculateDelay(1))
        assertEquals(Duration.ofSeconds(3), config.calculateDelay(2))
    }

    @Test
    fun `calculateDelay with EXPONENTIAL strategy doubles each time`() {
        val config =
            RetryConfig(
                maxAttempts = 5,
                delay = Duration.ofSeconds(1),
                backoff = BackoffStrategy.EXPONENTIAL,
                jitter = false,
            )

        assertEquals(Duration.ofSeconds(1), config.calculateDelay(0))
        assertEquals(Duration.ofSeconds(2), config.calculateDelay(1))
        assertEquals(Duration.ofSeconds(4), config.calculateDelay(2))
        assertEquals(Duration.ofSeconds(8), config.calculateDelay(3))
    }

    @Test
    fun `calculateDelay respects maxDelay cap`() {
        val config =
            RetryConfig(
                maxAttempts = 10,
                delay = Duration.ofSeconds(10),
                maxDelay = Duration.ofSeconds(30),
                backoff = BackoffStrategy.EXPONENTIAL,
                jitter = false,
            )

        // 10 * 2^3 = 80, but capped at 30
        assertEquals(Duration.ofSeconds(30), config.calculateDelay(3))
    }

    @Test
    fun `calculateDelay with jitter adds randomness`() {
        val config =
            RetryConfig(
                maxAttempts = 3,
                delay = Duration.ofSeconds(10),
                backoff = BackoffStrategy.FIXED,
                jitter = true,
            )

        // With jitter, delay should be within ±20% of base delay
        val delays = (0..10).map { config.calculateDelay(0) }

        // All delays should be between 8s and 12s (10s ± 20%)
        assertTrue(delays.all { it >= Duration.ofMillis(8000) && it <= Duration.ofMillis(12000) })

        // With 11 samples, we should see some variation (not all exactly the same)
        val uniqueDelays = delays.toSet()
        assertTrue(uniqueDelays.size > 1, "Expected jitter to produce varied delays")
    }

    @Test
    fun `copy preserves immutability`() {
        val original = RetryConfig.DEFAULT
        val modified = original.copy(maxAttempts = 5)

        assertEquals(3, original.maxAttempts)
        assertEquals(5, modified.maxAttempts)
    }
}
