package org.berrycrush.openapi

import io.swagger.v3.oas.models.OpenAPI
import org.berrycrush.config.BindingConfig
import org.berrycrush.config.SpecConfiguration
import org.berrycrush.exception.ConfigurationException
import org.berrycrush.exception.OperationNotFoundException
import org.berrycrush.openapi.impl.SwaggerParserAdapter
import org.berrycrush.util.FileLoader

/**
 * Registry for managing multiple OpenAPI specifications.
 *
 * Supports both single-spec and multi-spec scenarios.
 */
class SpecRegistry {
    private data class MethodPathTarget(
        val method: HttpMethod,
        val path: String,
    )

    companion object {
        private val parser: OpenApiParser = SwaggerParserAdapter()

        /**
         * Load spec
         */
        fun load(
            name: String,
            path: String,
            config: SpecConfiguration.() -> Unit = {},
        ): LoadedSpec {
            val specConfig = SpecConfiguration(name, path).apply(config)
            val openApiSpec = parser.parseContent(FileLoader.load(path))
            return LoadedSpec(
                name = name,
                path = path,
                spec = openApiSpec,
                baseUrl = specConfig.baseUrl ?: extractBaseUrl(openApiSpec),
                defaultHeaders = specConfig.defaultHeaders.toMap(),
            )
        }

        private fun extractBaseUrl(spec: OpenApiSpec): String = spec.servers.firstOrNull()?.url ?: "http://localhost"
    }

    private val specs = mutableMapOf<String, LoadedSpec>()

    private var defaultSpec: String? = null

    /**
     * Register an OpenAPI specification.
     *
     * @param name Unique identifier for this spec
     * @param path Path to the OpenAPI spec file
     * @param config Optional configuration for this spec
     */
    fun register(
        name: String,
        path: String,
        config: SpecConfiguration.() -> Unit = {},
    ) {
        specs[name] = load(name, path, config)
        // First registered spec becomes default
        if (defaultSpec == null) {
            defaultSpec = name
        }
    }

    /**
     * Register a single spec as the default (simplified API).
     */
    fun registerDefault(
        path: String,
        config: SpecConfiguration.() -> Unit = {},
    ) {
        register(BindingConfig.DEFAULT_BINDING_NAME, path, config)
    }

    /**
     * Get a loaded spec by name.
     */
    fun get(
        name: String,
        bindings: Map<String, LoadedSpecProvider>? = null,
    ): LoadedSpec =
        bindings?.get(name)?.spec
            ?: specs[name]
            ?: throw ConfigurationException("Spec '$name' not found. Available: ${specs.keys}")

    /**
     * Get the default spec.
     */
    fun getDefault(): LoadedSpec {
        val name = defaultSpec ?: throw ConfigurationException("No specs registered")
        return get(name)
    }

    /**
     * Resolve an operation, optionally specifying which spec to use.
     *
     * If specName is null and operationId is unique across all specs, auto-resolves.
     *
     * @param operationId The operation ID to resolve
     * @param specName Optional spec name to search in
     * @return Pair of spec name and resolved operation
     * @throws OperationNotFoundException if not found
     * @throws AmbiguousOperationException if found in multiple specs without specName
     */
    fun resolve(
        operationId: String,
        specName: String? = null,
        bindings: Map<String, BindingConfig> = emptyMap(),
    ): Pair<LoadedSpec, ResolvedOperation> {
        if (specName != null) {
            val spec = get(specName, bindings)
            return if (spec.hasOperation(operationId)) {
                spec to spec.resolver.resolve(operationId)
            } else {
                handleAlias(operationId, specName, bindings) { spec.allOperationIds().toList() }
            }
        }

        // Auto-resolve: find all specs containing this operationId
        val matches = specs.values.filter { it.hasOperation(operationId) }

        return when {
            matches.isEmpty() -> {
                handleAlias(operationId, null, bindings) { specs.values.flatMap { it.allOperationIds() } }
            }

            matches.size == 1 -> {
                val spec = matches.single()
                spec to spec.resolver.resolve(operationId)
            }

            else -> {
                throw AmbiguousOperationException(
                    operationId,
                    matches.map { it.name },
                )
            }
        }
    }

    private fun handleAlias(
        operationId: String,
        specName: String?,
        bindings: Map<String, BindingConfig>,
        operationIdProvider: () -> List<String>,
    ): Pair<LoadedSpec, ResolvedOperation> {
        val aliasTarget =
            findAliasTarget(operationId, specName, bindings)
                ?: throw OperationNotFoundException(operationId, operationIdProvider())
        return resolveAliasTarget(operationId, aliasTarget, specName, bindings)
    }

    /**
     * Check if any spec is registered.
     */
    fun hasSpecs(): Boolean = specs.isNotEmpty()

    /**
     * Get all registered spec names.
     */
    fun specNames(): Set<String> = specs.keys.toSet()

    private fun findAliasTarget(
        alias: String,
        specName: String?,
        bindings: Map<String, BindingConfig>,
    ): String? {
        val bindingName = specName ?: BindingConfig.DEFAULT_BINDING_NAME
        return bindings[bindingName]?.operationAliases?.get(alias)
            ?: if (specName == null) bindings[BindingConfig.DEFAULT_BINDING_NAME]?.operationAliases?.get(alias) else null
    }

    private fun resolveAliasTarget(
        alias: String,
        target: String,
        specName: String?,
        bindings: Map<String, BindingConfig>,
    ): Pair<LoadedSpec, ResolvedOperation> {
        val methodPath = parseMethodPathTarget(target)
        return if (methodPath == null) {
            resolve(target.removePrefix("^").trim(), specName, bindings)
        } else {
            resolveByMethodAndPath(alias, target, methodPath, specName, bindings)
        }
    }

    private fun resolveByMethodAndPath(
        alias: String,
        target: String,
        methodPath: MethodPathTarget,
        specName: String?,
        bindings: Map<String, BindingConfig>,
    ): Pair<LoadedSpec, ResolvedOperation> {
        if (specName != null) {
            val spec = get(specName, bindings)
            val resolved = spec.resolver.resolve(methodPath.method, methodPath.path)
            return if (resolved != null) {
                spec to resolved
            } else {
                throwOperationNotFoundException("$alias -> $target", spec.allOperationIds().toList())
            }
        }

        val matches =
            specs.values.mapNotNull { spec ->
                spec.resolver.resolve(methodPath.method, methodPath.path)?.let { spec to it }
            }

        return when {
            matches.isEmpty() -> {
                throwOperationNotFoundException("$alias -> $target", specs.values.flatMap { it.allOperationIds() })
            }

            matches.size == 1 -> {
                matches.single()
            }

            else -> {
                throw AmbiguousOperationException(
                    "$alias -> $target",
                    matches.map { it.first.name },
                )
            }
        }
    }

    private fun parseMethodPathTarget(target: String): MethodPathTarget? {
        val trimmed = target.trim()
        val split = trimmed.split(Regex("\\s+"), limit = 2)
        if (split.size != 2 || !split[1].startsWith('/')) {
            return null
        }

        return parseMethod(split[0])?.let { method ->
            MethodPathTarget(method, split[1].trim())
        }
    }

    private fun parseMethod(method: String): HttpMethod? = HttpMethod.fromName(method)

    /**
     * Update the base URL for a registered spec.
     *
     * This allows overriding the base URL after spec registration,
     * useful for file-level parameters or runtime configuration.
     *
     * @param name The spec name to update
     * @param newBaseUrl The new base URL
     * @throws IllegalArgumentException if spec is not found
     */
    fun updateBaseUrl(
        name: String,
        newBaseUrl: String,
    ) {
        val existing = specs[name] ?: throw IllegalArgumentException("Spec '$name' not found. Available: ${specs.keys}")
        specs[name] =
            existing.copy(baseUrl = newBaseUrl)
    }
}

interface LoadedSpecProvider {
    val spec: LoadedSpec?
}

/**
 * A loaded OpenAPI specification with resolver and configuration.
 */
data class LoadedSpec(
    val name: String,
    val path: String,
    val spec: OpenApiSpec,
    val baseUrl: String,
    val defaultHeaders: Map<String, String>,
) {
    internal val resolver: OperationResolver by lazy {
        OperationResolver(spec)
    }

    /**
     * Access the raw swagger OpenAPI model for backward compatibility.
     * Prefer using the spec abstraction when possible.
     */
    val openApi: OpenAPI
        get() = spec.rawModel as OpenAPI

    /**
     * Check if this spec contains the given operation ID.
     */
    fun hasOperation(operationId: String): Boolean = resolver.hasOperation(operationId)

    /**
     * Get all operation IDs in this spec.
     */
    fun allOperationIds(): Set<String> = resolver.allOperationIds()
}

/**
 * Exception thrown when an operationId exists in multiple specs.
 */
class AmbiguousOperationException(
    operationId: String,
    specs: List<String>,
) : RuntimeException(
        "Operation '$operationId' found in multiple specs: ${specs.joinToString(", ")}. " +
            "Use 'using(\"specName\")' to specify which spec to use.",
    )

private fun throwOperationNotFoundException(
    operationId: String,
    operationIds: List<String>,
): Nothing =
    throw OperationNotFoundException(
        operationId,
        operationIds,
    )
