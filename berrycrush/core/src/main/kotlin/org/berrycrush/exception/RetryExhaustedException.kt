package org.berrycrush.exception

import org.berrycrush.plugin.HttpResponse

/**
 * Exception thrown when all retry attempts have been exhausted.
 *
 * This exception is thrown when a request fails repeatedly and the maximum
 * number of retry attempts has been reached without success.
 *
 * @property attempts Total number of attempts made (including the initial request)
 * @property lastResponse The last HTTP response received (if any), may indicate the failure reason
 * @property lastException The last exception caught (if any), may be null if the failure was due to a status code
 */
class RetryExhaustedException(
    val attempts: Int,
    val lastResponse: HttpResponse? = null,
    val lastException: Exception? = null,
) : BerryCrushException(buildMessage(attempts, lastResponse, lastException), lastException) {
    companion object {
        private fun buildMessage(
            attempts: Int,
            lastResponse: HttpResponse?,
            lastException: Exception?,
        ): String {
            val builder = StringBuilder("Retry exhausted after $attempts attempt(s)")

            lastResponse?.let {
                builder.append(". Last response: HTTP ${it.statusCode}")
            }

            lastException?.let {
                builder.append(". Last error: ${it.javaClass.simpleName}: ${it.message}")
            }

            return builder.toString()
        }
    }
}
