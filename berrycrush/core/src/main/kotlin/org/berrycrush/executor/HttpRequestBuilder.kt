package org.berrycrush.executor

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.exception.HttpExecutionException
import org.berrycrush.openapi.HttpMethod
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Builds and executes HTTP requests using java.net.http.HttpClient.
 *
 * @property client The HTTP client to use for requests
 * @property timeout Request timeout (overrides default)
 */
class HttpRequestBuilder(
    private val client: HttpClient = createDefaultClient(),
    private val timeout: Duration = Duration.ofSeconds(30),
) {
    /**
     * Create an HttpRequestBuilder with configuration settings.
     *
     * @param configuration The test configuration
     */
    constructor(configuration: BerryCrushConfiguration) : this(
        client = createClient(configuration.followRedirects),
        timeout = configuration.timeout,
    )

    /**
     * Execute an HTTP request.
     *
     * @param method HTTP method
     * @param url Full URL to request
     * @param headers HTTP headers
     * @param body Request body (for POST/PUT/PATCH)
     * @return HTTP response
     */
    fun execute(
        method: HttpMethod,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): HttpResponse<String> {
        try {
            val requestBuilder =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .method(method.name, bodyPublisher(body))

            headers.forEach { (name, value) ->
                requestBuilder.header(name, value)
            }

            // Ensure Content-Type for requests with body
            if (body != null && "Content-Type" !in headers) {
                requestBuilder.header("Content-Type", "application/json")
            }

            val request = requestBuilder.build()
            return client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw HttpExecutionException(url, method.name, e)
        }
    }

    /**
     * Build a full URL with path parameters and query parameters.
     *
     * @param baseUrl Base URL of the API
     * @param path Path template (e.g., /pets/{petId})
     * @param pathParams Path parameter values
     * @param queryParams Query parameter values
     * @return Full URL with substitutions
     */
    fun buildUrl(
        baseUrl: String,
        path: String,
        pathParams: Map<String, Any> = emptyMap(),
        queryParams: Map<String, Any> = emptyMap(),
    ): String {
        // Substitute path parameters
        var resolvedPath = path
        pathParams.forEach { (name, value) ->
            resolvedPath = resolvedPath.replace("{$name}", value.toString())
        }

        // Build base URL
        val base = baseUrl.trimEnd('/')
        val pathPart = resolvedPath.trimStart('/')
        val url = StringBuilder("$base/$pathPart")

        // Add query parameters
        if (queryParams.isNotEmpty()) {
            url.append("?")
            url.append(
                queryParams.entries.joinToString("&") { (key, value) ->
                    "${encode(key)}=${encode(value.toString())}"
                },
            )
        }

        return url.toString()
    }

    private fun bodyPublisher(body: String?): HttpRequest.BodyPublisher =
        if (body != null) {
            HttpRequest.BodyPublishers.ofString(body)
        } else {
            HttpRequest.BodyPublishers.noBody()
        }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8)

    companion object {
        fun createDefaultClient(): HttpClient = createClient(followRedirects = true)

        /**
         * Create an HTTP client with specified redirect policy.
         *
         * @param followRedirects Whether to follow HTTP redirects
         */
        fun createClient(followRedirects: Boolean): HttpClient =
            HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(
                    if (followRedirects) HttpClient.Redirect.NORMAL else HttpClient.Redirect.NEVER,
                ).connectTimeout(Duration.ofSeconds(10))
                .build()
    }
}
