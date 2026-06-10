package org.berrycrush.executor.http

import org.berrycrush.context.ExecutionContext
import org.berrycrush.executor.BerryCrushConfigurationProvider
import org.berrycrush.executor.HttpRequestBuilder
import org.berrycrush.executor.resolvers.RequestResolver
import org.berrycrush.executor.resolvers.ResolvedRequest
import org.berrycrush.model.Step
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.ResolvedOperation
import tools.jackson.databind.ObjectMapper
import java.net.http.HttpResponse

/**
 * Default implementation of [HttpExecutor] for executing HTTP requests.
 *
 * This implementation handles:
 * - URL building with path and query parameters
 * - Header merging (config defaults + spec defaults + step headers)
 * - Body resolution (inline, structured properties, or file)
 * - HTTP request execution
 * - Request/response logging (if enabled)
 *
 * @property configuration Configuration for base URL, logging, and default headers
 * @property httpBuilder Builder for constructing and executing HTTP requests
 */
class DefaultHttpExecutor(
    private val configuration: BerryCrushConfigurationProvider,
    private val httpBuilder: HttpRequestBuilder = HttpRequestBuilder(configuration),
    objectMapper: ObjectMapper = ObjectMapper(),
) : HttpExecutor {
    private val requestResolver = RequestResolver(configuration, httpBuilder, objectMapper)

    override fun execute(
        step: Step,
        spec: LoadedSpec,
        operation: ResolvedOperation,
        context: ExecutionContext,
    ): HttpResponse<String> {
        val resolvedRequest = requestResolver.resolve(step, spec, operation, context)
        // Log request if enabled
        logRequest(resolvedRequest)

        // Record request start time for logging
        val requestStartTime = System.currentTimeMillis()

        // Execute the HTTP request
        val response =
            httpBuilder.execute(
                method = resolvedRequest.method,
                url = resolvedRequest.url,
                headers = resolvedRequest.headers,
                body = resolvedRequest.body,
            )

        // Log response if enabled
        logResponse(resolvedRequest, response, requestStartTime)

        return response
    }

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "resolveBody is deprecated and will be removed in 2.0.0.",
    )
    override fun resolveBody(
        step: Step,
        operation: ResolvedOperation?,
        context: ExecutionContext,
    ): String? = requestResolver.resolveBody(step, operation, context)

    // ========== Logging ==========

    /**
     * Log HTTP request if enabled.
     */
    private fun logRequest(request: ResolvedRequest) {
        if (configuration.logRequests) {
            configuration.getEffectiveHttpLogger().logRequest(request.method, request.url, request.headers, request.body)
        }
    }

    /**
     * Log HTTP response if enabled.
     */
    private fun logResponse(
        request: ResolvedRequest,
        response: HttpResponse<String>,
        requestStartTime: Long,
    ) {
        if (configuration.logResponses) {
            val durationMs = System.currentTimeMillis() - requestStartTime
            configuration.getEffectiveHttpLogger().logResponse(request.method, request.url, response, durationMs)
        }
    }
}
