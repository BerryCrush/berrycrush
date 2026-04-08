package io.github.ktakashi.lemoncheck.junit

import io.github.ktakashi.lemoncheck.config.Configuration

/**
 * Interface for providing runtime bindings to scenario execution.
 *
 * Implement this interface to provide custom variable bindings and configuration
 * that will be available during scenario execution. Common use cases include:
 * - Providing a dynamic base URL (e.g., from Spring's @LocalServerPort)
 * - Injecting authentication tokens
 * - Setting up test-specific configuration
 *
 * Example:
 * ```java
 * public class PetstoreBindings implements LemonCheckBindings {
 *     @LocalServerPort
 *     private int port;
 *
 *     @Override
 *     public Map<String, Object> getBindings() {
 *         return Map.of("baseUrl", "http://localhost:" + port + "/api/v1");
 *     }
 * }
 * ```
 */
interface LemonCheckBindings {
    /**
     * Returns variable bindings available to scenarios.
     *
     * The returned map contains name-value pairs that can be referenced
     * in scenario files using the `${name}` syntax.
     *
     * Common bindings include:
     * - `baseUrl`: The base URL for API requests
     * - `authToken`: Authentication token for secured endpoints
     *
     * @return Map of binding names to their values
     */
    fun getBindings(): Map<String, Any>

    /**
     * Optional: Override the OpenAPI spec path.
     *
     * Return a classpath path to the OpenAPI specification file.
     * If null, the path from @LemonCheckConfiguration or @LemonCheckSpec will be used.
     *
     * @return OpenAPI spec path, or null to use default
     */
    fun getOpenApiSpec(): String? = null

    /**
     * Optional: Configure the execution context.
     *
     * Called before scenario execution begins. Use this to perform
     * additional configuration on the execution context.
     *
     * @param config The configuration to modify
     */
    fun configure(config: Configuration) {}
}
