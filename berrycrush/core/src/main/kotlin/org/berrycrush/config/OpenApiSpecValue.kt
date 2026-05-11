package org.berrycrush.config

/**
 * Configuration for an OpenAPI specification.
 *
 * Use this class in [org.berrycrush.junit.BerryCrushBindings.getBindings] to configure
 * OpenAPI specifications with their base URLs. The spec name is determined by the key.
 *
 * ## Example
 *
 * ```kotlin
 * class MyBindings : BerryCrushBindings {
 *     override fun getBindings(): Map<String, Any> = mapOf(
 *         "default" to OpenApiSpecValue("petstore.yaml", "http://localhost:8080/api"),
 *         "auth" to OpenApiSpecValue("auth.yaml", "http://localhost:8080/auth"),
 *         "authToken" to "my-token"
 *     )
 * }
 * ```
 *
 * @property location Path to the OpenAPI specification file (classpath resource).
 *                    Examples: "petstore.yaml", "specs/auth.yaml"
 * @property baseUrl Base URL for API requests to this spec.
 *                   If null, the server URL from the spec will be used.
 * @see org.berrycrush.junit.BerryCrushBindings
 */
data class OpenApiSpecValue(
    val location: String,
    val baseUrl: String? = null,
) {
    companion object {
        /**
         * Creates an OpenAPI spec value with only a location.
         *
         * @param location Path to the OpenAPI specification file
         * @return OpenApiSpecValue instance
         */
        @JvmStatic
        fun of(location: String): OpenApiSpecValue = OpenApiSpecValue(location)

        /**
         * Creates an OpenAPI spec value with location and base URL.
         *
         * @param location Path to the OpenAPI specification file
         * @param baseUrl Base URL for API requests
         * @return OpenApiSpecValue instance
         */
        @JvmStatic
        fun of(
            location: String,
            baseUrl: String,
        ): OpenApiSpecValue = OpenApiSpecValue(location, baseUrl)
    }
}
