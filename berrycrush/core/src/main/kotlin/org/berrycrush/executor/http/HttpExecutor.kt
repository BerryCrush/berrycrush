package org.berrycrush.executor.http

import org.berrycrush.context.ExecutionContext
import org.berrycrush.model.Step
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.ResolvedOperation
import java.net.http.HttpResponse

/**
 * Executor for HTTP requests during scenario execution.
 *
 * This interface abstracts HTTP request building and execution from
 * the main scenario executor.
 */
interface HttpExecutor {
    /**
     * Execute an HTTP request for the given step.
     *
     * @param step The step containing the operation and parameters
     * @param spec The loaded OpenAPI specification (for base URL and default headers)
     * @param operation The resolved OpenAPI operation
     * @param context The execution context with variables and state
     * @return The HTTP response from the server
     */
    fun execute(
        step: Step,
        spec: LoadedSpec,
        operation: ResolvedOperation,
        context: ExecutionContext,
    ): HttpResponse<String>

    /**
     * Resolve the request body for a step.
     *
     * @param step The step containing body configuration
     * @param operation The resolved OpenAPI operation (for schema defaults)
     * @param context The execution context with variables
     * @return The resolved body string, or null if no body is required
     */
    fun resolveBody(
        step: Step,
        operation: ResolvedOperation?,
        context: ExecutionContext,
    ): String?
}
