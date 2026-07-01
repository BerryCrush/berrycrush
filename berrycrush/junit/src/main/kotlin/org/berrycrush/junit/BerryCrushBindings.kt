package org.berrycrush.junit

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.config.BindingConfig
import org.berrycrush.junit.binding.OpenApiSpecValue

/**
 * Interface for providing runtime bindings to scenario execution.
 *
 * Implement this interface to provide custom variable bindings and configuration
 * that will be available during scenario execution. Common use cases include:
 * - Providing a dynamic base URL (e.g., from Spring's @LocalServerPort)
 * - Injecting authentication tokens
 * - Setting up test-specific configuration
 * - Configuring multiple OpenAPI specs with their base URLs
 *
 * ## Example
 *
 * ```kotlin
 * class PetstoreBindings : BerryCrushBindings {
 *     @LocalServerPort
 *     private var port: Int = 0
 *
 *     override fun getBindings(): Map<String, Any> = mapOf(
 *         "default" to OpenApiSpecValue("petstore.yaml", "http://localhost:$port/api/v1"),
 *         "auth" to OpenApiSpecValue("auth.yaml", "http://localhost:$port/auth/api/v1"),
 *         "authToken" to "my-token"
 *     )
 * }
 * ```
 *
 * @see OpenApiSpecValue
 */
interface BerryCrushBindings {
    companion object {
        const val DEFAULT_BINDING_NAME = BindingConfig.DEFAULT_BINDING_NAME
    }

    /**
     * Returns bindings for scenario execution.
     *
     * The returned map contains name-value pairs that can be used:
     * - As variables referenced in scenario files using `${name}` syntax
     * - As OpenAPI spec configurations using [OpenApiSpecValue] (key is spec name, "default" for primary)
     *
     * Example:
     * ```kotlin
     * override fun getBindings(): Map<String, Any> = mapOf(
     *     "default" to OpenApiSpecValue("petstore.yaml", "http://localhost:8080/api"),
     *     "auth" to OpenApiSpecValue("auth.yaml", "http://localhost:8080/auth"),
     *     "authToken" to "Bearer token123"
     * )
     * ```
     *
     * @return Map of binding names to their values
     */
    fun getBindings(): Map<String, Any>

    /**
     * Optional: Configure the execution context.
     *
     * Called before scenario execution begins. Use this to perform
     * additional configuration on the execution context.
     *
     * @param config The configuration to modify
     */
    fun configure(config: BerryCrushConfiguration) {
        // NOOP
    }
}
