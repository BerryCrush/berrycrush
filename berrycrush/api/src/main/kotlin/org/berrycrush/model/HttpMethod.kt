package org.berrycrush.model

/**
 * HTTP methods supported by OpenAPI.
 */
enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
    TRACE,
    UNKNOWN,
    ;

    companion object {
        fun fromName(name: String): HttpMethod? =
            HttpMethod.entries.find {
                it.name == name
            }
    }
}
