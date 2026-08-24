package org.berrycrush.executor.http

import org.berrycrush.config.BindingConfig
import org.berrycrush.exception.HttpExecutionException
import org.berrycrush.executor.BerryCrushConfigurationProvider
import org.berrycrush.executor.HttpRequestBuilder
import org.berrycrush.executor.resolvers.DefaultRequestResolver
import org.berrycrush.executor.resolvers.RequestResolver
import org.berrycrush.executor.resolvers.resolveCall
import org.berrycrush.model.HttpRequest
import org.berrycrush.model.HttpResponse
import org.berrycrush.model.Step
import org.berrycrush.openapi.AmbiguousOperationException
import org.berrycrush.openapi.HttpMethod
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.plugin.StepContext
import org.berrycrush.plugin.adapter.ScenarioContextAdapter
import org.berrycrush.plugin.adapter.StepContextAdapter
import org.berrycrush.util.toNonNullMap
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
        step: Step,
        specRegistry: SpecRegistry,
        stepContext: StepContext,
    ): HttpResponse {
        val resolvedStep = stepContext.resolveCall(step)
        val operationId = resolvedStep.operationId
        val rawMethod = resolvedStep.rawMethod
        val rawPath = resolvedStep.rawPath

        return when {
            operationId != null -> {
                val (spec, resolvedOp) = resolve(resolvedStep, specRegistry)
                execute(resolvedStep, spec, resolvedOp, stepContext)
            }

            rawMethod != null && rawPath != null -> {
                executeRaw(resolvedStep, specRegistry, stepContext)
            }

            else -> {
                throw IllegalArgumentException("Step does not contain operationId or raw call target")
            }
        }
    }

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
            rawResponse
                .map {
                    HttpResponse(
                        statusCode = it.statusCode(),
                        statusMessage = HTTP_STATUS_MESSAGES[it.statusCode()] ?: "",
                        headers = it.headers().map(),
                        body = it.body(),
                        duration = duration,
                        timestamp = requestEndTime,
                        request = request,
                    )
                }.getOrElse { e ->
                    val wrapped = HttpExecutionException(request.url, request.method, e)
                    if (configuration.autoAssertions.enabled) {
                        throw wrapped
                    }
                    HttpResponse(
                        statusCode = -1,
                        statusMessage = e.message ?: "",
                        headers = emptyMap(),
                        duration = duration,
                        timestamp = requestEndTime,
                        request = request,
                        error = wrapped,
                    )
                }

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
    ): Pair<LoadedSpec, ResolvedOperation> = specRegistry.resolve(requireNotNull(step.operationId), step.specName, configuration.bindings)

    private fun executeRaw(
        step: Step,
        specRegistry: SpecRegistry,
        context: StepContext,
    ): HttpResponse {
        val rawMethod = requireNotNull(step.rawMethod)
        val rawPath = requireNotNull(step.rawPath)
        val methodForResolution =
            HttpMethod.fromName(rawMethod)
                ?: throw IllegalArgumentException("Unsupported HTTP method '$rawMethod' in 'call raw'.")

        val resolvedOperation = resolveRawOperation(specRegistry, step.specName, methodForResolution, rawPath)

        return if (resolvedOperation != null) {
            val (spec, operation) = resolvedOperation
            val remappedStep = remapRawPathParams(step, rawPath, operation.path)
            execute(remappedStep.copy(operationId = operation.operationId), spec, operation, context)
        } else {
            execute(buildDirectRawRequest(step, specRegistry, context), context)
        }
    }

    private fun buildDirectRawRequest(
        step: Step,
        specRegistry: SpecRegistry,
        context: StepContext,
    ): HttpRequest {
        val rawMethod = requireNotNull(step.rawMethod)
        val resolvedMethod =
            HttpMethod.fromName(rawMethod)
                ?: throw IllegalArgumentException("Unsupported HTTP method '$rawMethod' in 'call raw'.")
        val rawPath = requireNotNull(step.rawPath)
        val selectedSpec = resolvePreferredSpec(specRegistry, step.specName)
        val baseUrl = resolveRawBaseUrl(step.specName, selectedSpec, specRegistry)

        val url =
            httpBuilder.buildUrl(
                baseUrl = baseUrl,
                path = rawPath,
                pathParams = context.resolveParams(step.pathParams).toNonNullMap(),
                queryParams = context.resolveParams(step.queryParams).toNonNullMap(),
            )
        val headers =
            (configuration.defaultHeaders + (selectedSpec?.defaultHeaders ?: emptyMap()) + step.headers)
                .mapValues { (_, value) -> context.interpolate(value) }
        val body = resolveBody(step, null, context)

        return HttpRequest(
            method = resolvedMethod,
            url = url,
            headers = headers,
            body = body,
        )
    }

    private fun resolveRawBaseUrl(
        specName: String?,
        selectedSpec: LoadedSpec?,
        specRegistry: SpecRegistry,
    ): String {
        val defaultBinding = configuration.bindings[BindingConfig.DEFAULT_BINDING_NAME]

        if (specName != null) {
            val bindingUrl = configuration.bindings[specName]?.baseUrl
            return bindingUrl
                ?: selectedSpec?.baseUrl
                ?: throw IllegalArgumentException("Spec '$specName' not found for raw call")
        }

        return defaultBinding?.baseUrl
            ?: configuration.baseUrl
            ?: selectedSpec?.baseUrl
            ?: if (specRegistry.hasSpecs()) specRegistry.getDefault().baseUrl else "http://localhost"
    }

    private fun resolvePreferredSpec(
        specRegistry: SpecRegistry,
        specName: String?,
    ): LoadedSpec? = specRegistry.resolveByName(specName) ?: if (specRegistry.hasSpecs()) specRegistry.getDefault() else null

    private fun resolveRawOperation(
        specRegistry: SpecRegistry,
        specName: String?,
        method: HttpMethod,
        path: String,
    ): Pair<LoadedSpec, ResolvedOperation>? {
        val resolvedOperation =
            specRegistry.resolveByName(specName)?.let { spec ->
                spec.resolver.resolve(method, path)?.let {
                    spec to it
                }
            }
        return resolvedOperation ?: run {
            val matches =
                specRegistry.specNames().mapNotNull { name ->
                    val spec = specRegistry.get(name)
                    spec.resolver.resolve(method, path)?.let { spec to it }
                }

            when {
                matches.isEmpty() -> null
                matches.size == 1 -> matches.single()
                else -> throw AmbiguousOperationException("$method $path", matches.map { it.first.name })
            }
        }
    }

    private fun SpecRegistry.resolveByName(specName: String?): LoadedSpec? =
        specName?.let {
            if (this.contains(it)) this.get(it) else null
        }

    private fun remapRawPathParams(
        step: Step,
        rawPath: String,
        resolvedPath: String,
    ): Step {
        val sourceVars = extractPathVariables(rawPath)
        val targetVars = extractPathVariables(resolvedPath)
        if (sourceVars.isEmpty() || targetVars.isEmpty() || sourceVars.size != targetVars.size) {
            return step
        }

        val remapped = step.pathParams.toMutableMap()
        sourceVars.zip(targetVars).forEach { (sourceVar, targetVar) ->
            if (sourceVar == targetVar) return@forEach
            val value = remapped[sourceVar] ?: return@forEach
            remapped.putIfAbsent(targetVar, value)
        }

        return step.copy(pathParams = remapped)
    }

    private fun extractPathVariables(path: String): List<String> =
        Regex("\\{([^}]+)}")
            .findAll(path)
            .map { it.groupValues[1] }
            .toList()

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
