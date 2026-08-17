package org.berrycrush.configuration

import java.time.Duration

/**
 * Read-only configuration interface
 */
interface Configuration {
    /**
     * HTTP request timeout
     */
    val timeout: Duration

    /**
     * Base URL for HTTP requests
     */
    val defaultHeaders: Map<String, String>

    /**
     * API bindings
     */
    val bindings: Map<String, BindingConfiguration>
}

/**
 * Binding configuration interface
 */
interface BindingConfiguration {
    companion object {
        /**
         * Default binding name
         */
        const val DEFAULT_BINDING_NAME = "default"
    }

    /**
     * Name of the binding. `default` is used for default binding
     */
    val name: String

    /**
     * Base URL for the binding. If null, the `default`'s base URL is used.
     */
    val baseUrl: String?

    /**
     * API specification location of the binding.
     */
    val location: String?

    /**
     * Operation aliases for the binding.
     */
    val operationAliases: Map<String, String>
}
