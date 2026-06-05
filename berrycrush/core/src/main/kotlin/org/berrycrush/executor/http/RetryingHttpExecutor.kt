package org.berrycrush.executor.http

import org.berrycrush.config.RetryConfig
import org.berrycrush.context.ExecutionContext
import org.berrycrush.exception.RetryExhaustedException
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
 */
class RetryingHttpExecutor(
    private val delegate: HttpExecutor,
    private val config: RetryConfig,
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
                val (success, response) = execute(step, spec, operation, context, attempt)
                if (success) {
                    return response
                } else {
                    lastResponse = response
                }
            } catch (e: Exception) {
                if (RetryCheck.of(e).checkSleep(attempt, operation)) {
                    lastException = e
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

    private fun execute(
        step: Step,
        spec: LoadedSpec,
        operation: ResolvedOperation,
        context: ExecutionContext,
        attempt: Int,
    ): Pair<Boolean, HttpResponse<String>> {
        val response = delegate.execute(step, spec, operation, context)

        return if (RetryCheck.of(response.statusCode()).checkSleep(attempt, operation)) {
            false to response
        } else {
            // Success or non-retryable status
            if (attempt > 0) {
                logRetrySuccess(operation, attempt + 1)
            }
            true to response
        }
    }

    sealed interface RetryCheck {
        fun check(config: RetryConfig): Boolean

        data class ExceptionParam(
            val exception: Exception,
        ) : RetryCheck {
            override fun check(config: RetryConfig) = config.shouldRetryException(exception)
        }

        data class StatusParam(
            val status: Int,
        ) : RetryCheck {
            override fun check(config: RetryConfig) = config.shouldRetryStatus(status)
        }

        companion object {
            fun of(e: Exception) = ExceptionParam(e)

            fun of(status: Int) = StatusParam(status)
        }
    }

    private fun RetryCheck.checkSleep(
        attempt: Int,
        operation: ResolvedOperation,
    ): Boolean =
        if (check(config)) {
            if (attempt < config.maxAttempts) {
                this.logRetryAttempt(operation, attempt + 1, config.maxAttempts)
                sleep(config.calculateDelay(attempt))
            }
            true
        } else {
            false
        }

    private fun RetryCheck.logRetryAttempt(
        operation: ResolvedOperation,
        attempt: Int,
        maxAttempts: Int,
    ) = when (this) {
        is RetryCheck.ExceptionParam -> logRetryAttempt(operation, attempt, maxAttempts, exception)
        is RetryCheck.StatusParam -> logRetryAttempt(operation, attempt, maxAttempts, status)
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
