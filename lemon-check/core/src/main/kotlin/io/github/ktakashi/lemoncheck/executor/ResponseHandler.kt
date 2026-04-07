package io.github.ktakashi.lemoncheck.executor

import java.net.http.HttpResponse

/**
 * Handles HTTP response processing.
 */
class ResponseHandler {
    /**
     * Extract the response body as a string.
     */
    fun getBody(response: HttpResponse<String>): String {
        return response.body() ?: ""
    }

    /**
     * Get response status code.
     */
    fun getStatusCode(response: HttpResponse<String>): Int {
        return response.statusCode()
    }

    /**
     * Get response headers as a map.
     */
    fun getHeaders(response: HttpResponse<String>): Map<String, List<String>> {
        return response.headers().map()
    }

    /**
     * Get a specific header value (first value if multiple).
     */
    fun getHeader(
        response: HttpResponse<String>,
        name: String,
    ): String? {
        return response.headers().firstValue(name).orElse(null)
    }

    /**
     * Get all values for a specific header.
     */
    fun getHeaderValues(
        response: HttpResponse<String>,
        name: String,
    ): List<String> {
        return response.headers().allValues(name)
    }

    /**
     * Check if Content-Type is JSON.
     */
    fun isJsonResponse(response: HttpResponse<String>): Boolean {
        val contentType = getHeader(response, "Content-Type") ?: return false
        return contentType.contains("application/json", ignoreCase = true)
    }

    /**
     * Get Content-Type header.
     */
    fun getContentType(response: HttpResponse<String>): String? {
        return getHeader(response, "Content-Type")
    }
}
