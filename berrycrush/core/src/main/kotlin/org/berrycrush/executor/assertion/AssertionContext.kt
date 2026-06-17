package org.berrycrush.executor.assertion

import org.berrycrush.context.ExecutionContext
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.plugin.HttpResponse

/**
 * Context for assertion evaluation, encapsulating all data needed
 * to evaluate conditions and assertions against HTTP responses.
 *
 * This immutable data class provides a clean interface for assertion
 * evaluation, decoupling the assertion logic from the executor state.
 */
data class AssertionContext(
    /** The HTTP response to evaluate assertions against. */
    val response: HttpResponse?,
    /** The response body as a string, cached for convenience. */
    val responseBody: String?,
    /** Response headers mapped to their values. */
    val responseHeaders: Map<String, List<String>>,
    /** The HTTP status code from the response. */
    val statusCode: Int?,
    /** Response time in milliseconds, if measured. */
    val responseTimeMs: Long?,
    /** All variables available in the current scope. */
    val variables: Map<String, Any?>,
    /** Reference to the full execution context for advanced operations. */
    val executionContext: ExecutionContext,
    /** Current operation being executed, for schema validation. */
    val currentOperation: ResolvedOperation? = null,
) {
    companion object {
        /**
         * Create an AssertionContext from an ExecutionContext.
         * Extracts relevant data from the execution context for assertion evaluation.
         */
        fun from(executionContext: ExecutionContext): AssertionContext {
            val response = executionContext.lastResponse
            val headers = response?.headers?.mapValues { it.value.toList() } ?: emptyMap()

            return AssertionContext(
                response = response,
                responseBody = executionContext.lastResponseBody,
                responseHeaders = headers,
                statusCode = executionContext.lastStatusCode,
                responseTimeMs = executionContext.lastResponseTimeMs,
                variables = executionContext.allVariables(),
                executionContext = executionContext,
                currentOperation = executionContext.currentOperation,
            )
        }
    }
}
