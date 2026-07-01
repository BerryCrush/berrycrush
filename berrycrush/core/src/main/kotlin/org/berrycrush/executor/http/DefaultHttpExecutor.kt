package org.berrycrush.executor.http

import org.berrycrush.executor.BerryCrushConfigurationProvider
import org.berrycrush.executor.HttpRequestBuilder
import org.berrycrush.executor.resolvers.DefaultRequestResolver
import org.berrycrush.executor.resolvers.RequestResolver
import org.berrycrush.model.HttpRequest
import org.berrycrush.model.HttpResponse
import org.berrycrush.model.Step
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.plugin.StepContext
import org.berrycrush.plugin.adapter.ScenarioContextAdapter
import org.berrycrush.plugin.adapter.StepContextAdapter
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant

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
    private val requestResolver: RequestResolver = DefaultRequestResolver(configuration, httpBuilder, objectMapper),
) : HttpExecutor,
    RequestResolver by requestResolver {
    override fun execute(
        request: HttpRequest,
        context: StepContext,
    ): HttpResponse {
        if (context is StepContextAdapter) {
            context.setRequest(request)
        }
        logRequest(request)

        // Record request start time for logging
        val requestStartTime = Instant.now()

        // Execute the HTTP request
        val rawResponse =
            httpBuilder.execute(
                method = request.method,
                url = request.url,
                headers = request.headers,
                body = request.body,
            )
        val requestEndTime = Instant.now()
        val duration = Duration.between(requestStartTime, requestEndTime)
        val response =
            HttpResponse(
                statusCode = rawResponse.statusCode(),
                statusMessage = HTTP_STATUS_MESSAGES[rawResponse.statusCode()] ?: "",
                headers = rawResponse.headers().map(),
                body = rawResponse.body(),
                duration = duration,
                timestamp = requestEndTime,
                request = request,
            )
        // Log response if enabled
        logResponse(request, response, duration)

        if (context is StepContextAdapter) {
            context.setResponse(response)
            context.updateResponseTime(duration)
        }
        val scenarioContext = context.scenarioContext
        if (scenarioContext is ScenarioContextAdapter) {
            scenarioContext.addAudit(request, response)
        }
        return response
    }

    override fun resolve(
        step: Step,
        specRegistry: SpecRegistry,
    ) = specRegistry.resolve(step.operationId!!, step.specName, configuration.bindings)

    // ========== Logging ==========

    /**
     * Log HTTP request if enabled.
     */
    private fun logRequest(request: HttpRequest) {
        if (configuration.logRequests) {
            configuration.getEffectiveHttpLogger().logRequest(request.method, request.url, request.headers, request.body)
        }
    }

    /**
     * Log HTTP response if enabled.
     */
    private fun logResponse(
        request: HttpRequest,
        response: HttpResponse,
        duration: Duration,
    ) {
        if (configuration.logResponses) {
            configuration.getEffectiveHttpLogger().logResponse(request.method, request.url, response, duration.toMillis())
        }
    }
}

/**
 * HTTP status code to message mapping.
 */
@Suppress("MagicNumber")
private val HTTP_STATUS_MESSAGES =
    mapOf(
        200 to "OK",
        201 to "Created",
        204 to "No Content",
        400 to "Bad Request",
        401 to "Unauthorized",
        403 to "Forbidden",
        404 to "Not Found",
        405 to "Method Not Allowed",
        409 to "Conflict",
        422 to "Unprocessable Entity",
        500 to "Internal Server Error",
        502 to "Bad Gateway",
        503 to "Service Unavailable",
    )
