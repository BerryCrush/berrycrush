package org.berrycrush.executor.assertion

import org.berrycrush.context.ExecutionContext
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.plugin.HttpResponse
import java.time.Duration

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
    /** Response time, if measured. */
    val responseTime: Duration?,
    /** All variables available in the current scope. */
    val variables: Map<String, Any?>,
    /** Reference to the full execution context for advanced operations. */
    val executionContext: ExecutionContext,
    /** Current operation being executed, for schema validation. */
    val currentOperation: ResolvedOperation? = null,
) {
    /** The response body as a string, cached for convenience. */
    val responseBody = response?.body

    /** Response headers mapped to their values. */
    val responseHeaders = response?.headers ?: emptyMap()

    /** The HTTP status code from the response. */
    val statusCode = response?.statusCode
}
