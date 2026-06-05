package org.berrycrush.executor.http

import org.berrycrush.config.RetryConfig
import org.berrycrush.context.ExecutionContext
import org.berrycrush.exception.RetryExhaustedException
import org.berrycrush.logging.HttpLogger
import org.berrycrush.model.Step
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.ResolvedOperation
import java.net.http.HttpResponse
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger

/**
 * HTTP executor decorator that adds retry functionality.
 *
 * This executor wraps another [HttpExecutor] and automatically retries
 * failed requests based on the [RetryConfig] settings.
 *
 * Thread-safety: This executor is thread-safe when:
 * - The delegate [HttpExecutor] is thread-safe
 * - The [RetryConfig] is immutable (it is a data class)
 * - No mutable instance state is used
 *
 * @property delegate The underlying HTTP executor to use for actual requests
 * @property config The retry configuration
 * @property httpLogger Optional HTTP logger for logging retry attempts
 */
class RetryingHttpExecutor(
    private val delegate: HttpExecutor,
    private val config: RetryConfig,
    private val httpLogger: HttpLogger? = null,
) : HttpExecutor by delegate {
    private val logger = Logger.getLogger(RetryingHttpExecutor::class.java.name)

    override fun execute(
        step: Step,
        spec: LoadedSpec,
        operation: ResolvedOperation,
        context: ExecutionContext,
    ): HttpResponse<String> {
        // If retry is disabled, delegate directly
        if (!config.isEnabled) {
            return delegate.execute(step, spec, operation, context)
        }

        var lastException: Exception? = null
        var lastResponse: HttpResponse<String>? = null

        // Total attempts = 1 (initial) + maxAttempts (retries)
        val totalAttempts = config.maxAttempts + 1

        for (attempt in 0 until totalAttempts) {
            try {
                val response = delegate.execute(step, spec, operation, context)

                if (config.shouldRetryStatus(response.statusCode())) {
                    lastResponse = response
                    if (attempt < config.maxAttempts) {
                        logRetryAttempt(operation, attempt + 1, config.maxAttempts, response.statusCode())
                        sleep(config.calculateDelay(attempt))
                    }
                } else {
                    // Success or non-retryable status
                    if (attempt > 0) {
                        logRetrySuccess(operation, attempt + 1)
                    }
                    return response
                }
            } catch (e: Exception) {
                if (config.shouldRetryException(e)) {
                    lastException = e
                    if (attempt < config.maxAttempts) {
                        logRetryAttempt(operation, attempt + 1, config.maxAttempts, e)
                        sleep(config.calculateDelay(attempt))
                    }
                } else {
                    // Non-retryable exception
                    throw e
                }
            }
        }

        // All attempts exhausted
        throw RetryExhaustedException(
            attempts = totalAttempts,
            lastResponse = lastResponse,
            lastException = lastException,
        )
    }

    private fun logRetryAttempt(
        operation: ResolvedOperation,
        attempt: Int,
        maxAttempts: Int,
        statusCode: Int,
    ) {
        logger.log(
            Level.INFO,
            "Retry attempt $attempt/$maxAttempts for ${operation.method} ${operation.path} " +
                "(status: $statusCode)",
        )
    }

    private fun logRetryAttempt(
        operation: ResolvedOperation,
        attempt: Int,
        maxAttempts: Int,
        exception: Exception,
    ) {
        logger.log(
            Level.INFO,
            "Retry attempt $attempt/$maxAttempts for ${operation.method} ${operation.path} " +
                "(error: ${exception.javaClass.simpleName}: ${exception.message})",
        )
    }

    private fun logRetrySuccess(
        operation: ResolvedOperation,
        totalAttempts: Int,
    ) {
        logger.log(
            Level.INFO,
            "${operation.method} ${operation.path} succeeded after $totalAttempts attempt(s)",
        )
    }

    private fun sleep(duration: Duration) {
        try {
            Thread.sleep(duration.toMillis())
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
