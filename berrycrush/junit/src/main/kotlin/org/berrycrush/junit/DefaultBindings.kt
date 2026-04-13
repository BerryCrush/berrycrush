package org.berrycrush.junit

import org.berrycrush.config.Configuration

/**
 * Default implementation of [BerryCrushBindings] that provides no bindings.
 *
 * This is used when no custom bindings class is specified in @BerryCrushConfiguration.
 * It provides an empty bindings map and uses default configuration.
 */
class DefaultBindings : BerryCrushBindings {
    /**
     * Returns an empty bindings map.
     *
     * Override this in a custom implementation to provide actual bindings.
     */
    override fun getBindings(): Map<String, Any> = emptyMap()

    /**
     * Returns null to use the default OpenAPI spec path.
     */
    override fun getOpenApiSpec(): String? = null

    /**
     * Does nothing - uses default configuration.
     */
    override fun configure(config: Configuration) {
        // No-op: use default configuration
    }
}
