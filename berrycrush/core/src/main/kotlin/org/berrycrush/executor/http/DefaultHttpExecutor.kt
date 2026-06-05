package org.berrycrush.executor.http

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.context.ExecutionContext
import org.berrycrush.executor.HttpRequestBuilder
import org.berrycrush.model.BodyProperty
import org.berrycrush.model.Step
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.util.FileLoader
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
    private val configuration: BerryCrushConfiguration,
    private val httpBuilder: HttpRequestBuilder = HttpRequestBuilder(configuration),
) : HttpExecutor {
    private val objectMapper = ObjectMapper()

    override fun execute(
        step: Step,
        spec: LoadedSpec,
        operation: ResolvedOperation,
        context: ExecutionContext,
    ): HttpResponse<String> {
        // Build the URL
        val baseUrl = configuration.baseUrl ?: spec.baseUrl
        val url =
            httpBuilder.buildUrl(
                baseUrl = baseUrl,
                path = operation.path,
                pathParams = resolveParams(step.pathParams, context),
                queryParams = resolveParams(step.queryParams, context),
            )

        // Merge headers immutably and interpolate values
        val headers =
            (configuration.defaultHeaders + spec.defaultHeaders + step.headers)
                .mapValues { (_, value) -> context.interpolate(value) }

        // Resolve body
        val body = resolveBody(step, operation, context)

        // Log request if enabled
        logRequest(operation.method, url, headers, body)

        // Record request start time for logging
        val requestStartTime = System.currentTimeMillis()

        // Execute the HTTP request
        val response =
            httpBuilder.execute(
                method = operation.method,
                url = url,
                headers = headers,
                body = body,
            )

        // Log response if enabled
        logResponse(operation.method, url, response, requestStartTime)

        return response
    }

    override fun resolveBody(
        step: Step,
        operation: ResolvedOperation?,
        context: ExecutionContext,
    ): String? {
        // Inline body takes precedence
        step.body?.let { return context.interpolate(it) }

        // Structured body properties - generate from schema and merge
        step.bodyProperties?.let { props ->
            val bodyJson = generateBodyFromProperties(props, operation, context)
            return bodyJson
        }

        // Fall back to body file
        return step.bodyFile?.let { path ->
            val content = FileLoader.load(path)
            context.interpolate(content)
        }
    }

    // ========== Parameter Resolution ==========

    private fun resolveParams(
        params: Map<String, Any>,
        context: ExecutionContext,
    ): Map<String, Any> =
        params.mapValues { (_, value) ->
            when (value) {
                is String -> context.interpolate(value)
                else -> value
            }
        }

    // ========== Body Generation ==========

    /**
     * Generate JSON body from structured properties and OpenAPI schema defaults.
     */
    private fun generateBodyFromProperties(
        props: Map<String, BodyProperty>,
        resolvedOp: ResolvedOperation?,
        context: ExecutionContext,
    ): String {
        // Get schema defaults from OpenAPI spec
        val schemaDefaults = resolvedOp?.let { getSchemaDefaults(it) } ?: emptyMap()

        // Merge schema defaults with user-provided properties (user wins)
        val merged = mergeBodyProperties(schemaDefaults, props)

        // Convert to JSON and interpolate variables
        val json = bodyPropertyToJson(merged, context)
        return context.interpolate(json)
    }

    /**
     * Extract default values from OpenAPI requestBody schema.
     */
    @Suppress("ReturnCount") // Multiple early returns for validation guards
    private fun getSchemaDefaults(resolvedOp: ResolvedOperation): Map<String, BodyProperty> {
        val requestBody = resolvedOp.operation.requestBody ?: return emptyMap()
        val content = requestBody.content
        if (content.isEmpty()) return emptyMap()

        // Prefer application/json schema
        val mediaType = content["application/json"] ?: content.values.firstOrNull() ?: return emptyMap()
        val schema = mediaType.schema ?: return emptyMap()

        return extractPropertiesFromSchemaSpec(schema)
    }

    /**
     * Extract default properties from a SchemaSpec.
     */
    private fun extractPropertiesFromSchemaSpec(schema: org.berrycrush.openapi.SchemaSpec): Map<String, BodyProperty> {
        val result = mutableMapOf<String, BodyProperty>()

        schema.properties?.forEach { (name, propSchema) ->
            val defaultValue = getSchemaSpecDefaultValue(propSchema)
            if (defaultValue != null) {
                result[name] = defaultValue
            }
        }

        return result
    }

    /**
     * Get a default value for a SchemaSpec property.
     */
    private fun getSchemaSpecDefaultValue(schema: org.berrycrush.openapi.SchemaSpec): BodyProperty? {
        // Use explicit default if provided
        schema.default?.let { return BodyProperty.Simple(it) }

        // Use example if provided
        schema.example?.let { return BodyProperty.Simple(it) }

        // Generate a sensible default based on type
        return when (schema.type) {
            "string" -> BodyProperty.Simple("")
            "integer", "number" -> BodyProperty.Simple(0)
            "boolean" -> BodyProperty.Simple(false)
            "array" -> BodyProperty.Simple(emptyList<Any>())
            "object" -> {
                val nestedProps = extractPropertiesFromSchemaSpec(schema)
                if (nestedProps.isNotEmpty()) {
                    BodyProperty.Nested(nestedProps)
                } else {
                    BodyProperty.Simple(emptyMap<String, Any>())
                }
            }
            else -> null
        }
    }

    /**
     * Merge schema defaults with user-provided properties.
     * User properties override schema defaults.
     */
    private fun mergeBodyProperties(
        defaults: Map<String, BodyProperty>,
        userProps: Map<String, BodyProperty>,
    ): Map<String, BodyProperty> {
        val result = defaults.toMutableMap()

        userProps.forEach { (key, value) ->
            val existing = result[key]
            if (existing is BodyProperty.Nested && value is BodyProperty.Nested) {
                // Deep merge nested properties
                result[key] = BodyProperty.Nested(mergeBodyProperties(existing.properties, value.properties))
            } else {
                // User property overrides
                result[key] = value
            }
        }

        return result
    }

    /**
     * Convert BodyProperty map to JSON string.
     */
    private fun bodyPropertyToJson(
        props: Map<String, BodyProperty>,
        context: ExecutionContext,
    ): String {
        val jsonMap = props.mapValues { (_, prop) -> bodyPropertyToJsonValue(prop, context) }
        return objectMapper.writeValueAsString(jsonMap)
    }

    private fun bodyPropertyToJsonValue(
        prop: BodyProperty,
        context: ExecutionContext,
    ): Any =
        when (prop) {
            is BodyProperty.Simple -> prop.value
            is BodyProperty.Container -> objectMapper.readTree(context.interpolate(prop.value))
            is BodyProperty.Nested -> prop.properties.mapValues { (_, p) -> bodyPropertyToJsonValue(p, context) }
        }

    // ========== Logging ==========

    /**
     * Log HTTP request if enabled.
     */
    private fun logRequest(
        method: org.berrycrush.openapi.HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ) {
        if (configuration.logRequests) {
            configuration.getEffectiveHttpLogger().logRequest(method, url, headers, body)
        }
    }

    /**
     * Log HTTP response if enabled.
     */
    private fun logResponse(
        method: org.berrycrush.openapi.HttpMethod,
        url: String,
        response: HttpResponse<String>,
        requestStartTime: Long,
    ) {
        if (configuration.logResponses) {
            val durationMs = System.currentTimeMillis() - requestStartTime
            configuration.getEffectiveHttpLogger().logResponse(method, url, response, durationMs)
        }
    }
}
