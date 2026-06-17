package org.berrycrush.executor.http

import org.berrycrush.config.RetryConfig
import org.berrycrush.exception.RetryExhaustedException
import org.berrycrush.model.Step
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.plugin.HttpRequest
import org.berrycrush.plugin.HttpResponse
import org.berrycrush.plugin.StepContext
import java.net.URI
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
        context: StepContext,
    ): HttpResponse = execute(delegate.resolve(step, spec, operation, context), context)

    override fun execute(
        request: HttpRequest,
        context: StepContext,
    ): HttpResponse {
        if (!config.isEnabled) {
            return delegate.execute(request, context)
        }

        var lastException: Exception? = null
        var lastResponse: HttpResponse? = null

        // Total attempts = 1 (initial) + maxAttempts (retries)
        val totalAttempts = config.maxAttempts + 1

        for (attempt in 0 until totalAttempts) {
            try {
                val (success, response) = execute(request, context, attempt)
                if (success) {
                    return response
                } else {
                    lastResponse = response
                }
            } catch (e: Exception) {
                if (RetryCheck.of(e).checkSleep(attempt, request)) {
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
        request: HttpRequest,
        context: StepContext,
        attempt: Int,
    ): Pair<Boolean, HttpResponse> {
        val response = delegate.execute(request, context)

        return if (RetryCheck.of(response.statusCode).checkSleep(attempt, request)) {
            false to response
        } else {
            // Success or non-retryable status
            if (attempt > 0) {
                logRetrySuccess(request, attempt + 1)
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
        request: HttpRequest,
    ): Boolean =
        if (check(config)) {
            if (attempt < config.maxAttempts) {
                this.logRetryAttempt(request, attempt + 1, config.maxAttempts)
                sleep(config.calculateDelay(attempt))
            }
            true
        } else {
            false
        }

    private fun RetryCheck.logRetryAttempt(
        request: HttpRequest,
        attempt: Int,
        maxAttempts: Int,
    ) = when (this) {
        is RetryCheck.ExceptionParam -> logRetryAttempt(request, attempt, maxAttempts, exception)
        is RetryCheck.StatusParam -> logRetryAttempt(request, attempt, maxAttempts, status)
    }

    private fun logRetryAttempt(
        request: HttpRequest,
        attempt: Int,
        maxAttempts: Int,
        statusCode: Int,
    ) {
        val uri = URI.create(request.url)
        logger.log(
            Level.INFO,
            "Retry attempt $attempt/$maxAttempts for ${request.method} ${uri.path} " +
                "(status: $statusCode)",
        )
    }

    private fun logRetryAttempt(
        request: HttpRequest,
        attempt: Int,
        maxAttempts: Int,
        exception: Exception,
    ) {
        val uri = URI.create(request.url)
        logger.log(
            Level.INFO,
            "Retry attempt $attempt/$maxAttempts for ${request.method} ${uri.path} " +
                "(error: ${exception.javaClass.simpleName}: ${exception.message})",
        )
    }

    private fun logRetrySuccess(
        request: HttpRequest,
        totalAttempts: Int,
    ) {
        val uri = URI.create(request.url)
        logger.log(
            Level.INFO,
            "${request.method} ${uri.path} succeeded after $totalAttempts attempt(s)",
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
