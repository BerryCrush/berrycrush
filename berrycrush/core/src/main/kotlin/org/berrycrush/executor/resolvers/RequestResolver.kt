package org.berrycrush.executor.resolvers

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.context.ExecutionContext
import org.berrycrush.executor.HttpRequestBuilder
import org.berrycrush.model.BodyProperty
import org.berrycrush.model.Step
import org.berrycrush.openapi.HttpMethod
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.openapi.SchemaSpec
import org.berrycrush.util.FileLoader
import tools.jackson.databind.ObjectMapper

fun interface UrlResolver {
    fun resolve(
        step: Step,
        spec: LoadedSpec,
        operation: ResolvedOperation,
        context: ExecutionContext,
    ): String
}

fun interface HeaderResolver {
    fun resolve(
        step: Step,
        spec: LoadedSpec,
        context: ExecutionContext,
    ): Map<String, String>
}

interface BodyResolver {
    fun resolve(
        properties: Map<String, BodyProperty>,
        operation: ResolvedOperation?,
        context: ExecutionContext,
    ): Map<String, Any>

    fun resolve(
        properties: Map<String, BodyProperty>,
        context: ExecutionContext,
    ): Map<String, Any> = resolve(properties, null, context)
}

/**
 * Resolved request
 */
data class ResolvedRequest(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String>,
    val body: String? = null,
)

/**
 * Request resolver.
 */
class RequestResolver(
    private val urlResolver: UrlResolver,
    private val headerResolver: HeaderResolver,
    private val bodyResolver: BodyResolver,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    @JvmOverloads
    constructor(configuration: BerryCrushConfiguration, httpBuilder: HttpRequestBuilder, objectMapper: ObjectMapper = ObjectMapper()) :
        this(
            DefaultUrlResolver(configuration, httpBuilder),
            DefaultHeaderResolver(configuration),
            DefaultBodyResolver(objectMapper),
            objectMapper,
        )

    fun resolve(
        step: Step,
        spec: LoadedSpec,
        operation: ResolvedOperation,
        context: ExecutionContext,
    ): ResolvedRequest =
        ResolvedRequest(
            operation.method,
            urlResolver.resolve(step, spec, operation, context),
            headerResolver.resolve(step, spec, context),
            resolveBody(step, operation, context),
        )

    fun resolveBody(
        step: Step,
        operation: ResolvedOperation?,
        context: ExecutionContext,
    ): String? {
        // Inline body takes precedence
        step.body?.let { return context.interpolate(it) }

        // Structured body properties - generate from schema and merge
        step.bodyProperties?.let { props ->
            return objectMapper.writeValueAsString(bodyResolver.resolve(props, operation, context))
        }
        return step.bodyFile?.let { file ->
            context.interpolate(FileLoader.load(file))
        }
    }
}

// default implementations of the resolvers

private class DefaultUrlResolver(
    private val configuration: BerryCrushConfiguration,
    private val httpBuilder: HttpRequestBuilder,
) : UrlResolver {
    override fun resolve(
        step: Step,
        spec: LoadedSpec,
        operation: ResolvedOperation,
        context: ExecutionContext,
    ): String =
        httpBuilder.buildUrl(
            baseUrl = configuration.baseUrl ?: spec.baseUrl,
            path = operation.path,
            pathParams = context.resolveParams(step.pathParams),
            queryParams = context.resolveParams(step.queryParams),
        )
}

private class DefaultHeaderResolver(
    private val configuration: BerryCrushConfiguration,
) : HeaderResolver {
    override fun resolve(
        step: Step,
        spec: LoadedSpec,
        context: ExecutionContext,
    ): Map<String, String> =
        (configuration.defaultHeaders + spec.defaultHeaders + step.headers).mapValues { (_, value) -> context.interpolate(value) }
}

private class DefaultBodyResolver(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : BodyResolver {
    override fun resolve(
        properties: Map<String, BodyProperty>,
        operation: ResolvedOperation?,
        context: ExecutionContext,
    ): Map<String, Any> {
        val schemaDefaults = operation?.let { getSchemaDefaults(it) } ?: emptyMap()
        val merged = if (schemaDefaults.isEmpty()) properties else mergeBodyProperties(schemaDefaults, properties)
        return merged.mapValues { (_, value) -> resolveProperty(value, context) }
    }

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

    private fun extractPropertiesFromSchemaSpec(schema: SchemaSpec): Map<String, BodyProperty> {
        val result = mutableMapOf<String, BodyProperty>()

        schema.properties?.forEach { (name, propSchema) ->
            val defaultValue = getSchemaSpecDefaultValue(propSchema)
            if (defaultValue != null) {
                result[name] = defaultValue
            }
        }

        return result
    }

    private fun getSchemaSpecDefaultValue(schema: SchemaSpec): BodyProperty? {
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

    private fun resolveProperty(
        value: BodyProperty,
        context: ExecutionContext,
    ): Any =
        when (value) {
            is BodyProperty.Simple -> context.resolveParam(value.value)
            is BodyProperty.Container -> objectMapper.readTree(context.interpolate(value.value))
            is BodyProperty.Nested -> value.properties.mapValues { (_, value) -> resolveProperty(value, context) }
        }
}
