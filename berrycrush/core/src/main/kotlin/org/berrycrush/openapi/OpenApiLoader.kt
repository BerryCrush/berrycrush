package org.berrycrush.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.converter.SwaggerConverter
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.core.models.SwaggerParseResult
import java.nio.file.Path

/**
 * Loads and parses OpenAPI specifications.
 */
object OpenApiLoader {
    private val parser = sequenceOf(::OpenAPIV3Parser, ::SwaggerConverter)

    /**
     * Load an OpenAPI spec from a file path.
     *
     * @param path Path to the OpenAPI spec file (YAML or JSON)
     * @return Parsed OpenAPI model
     * @throws OpenApiParseException if parsing fails
     */
    fun load(path: String): OpenAPI =
        load { parseOptions ->
            parser.map { it().readLocation(path, null, parseOptions) }
        }

    /**
     * Load an OpenAPI spec from a Path.
     */
    fun load(path: Path): OpenAPI = load(path.toString())

    /**
     * Load an OpenAPI spec from content string.
     *
     * @param content OpenAPI spec content (YAML or JSON)
     * @return Parsed OpenAPI model
     */
    fun loadFromString(content: String): OpenAPI =
        load { parseOptions ->
            parser.map { it().readContents(content, null, parseOptions) }
        }

    private fun load(loader: (ParseOptions) -> Sequence<SwaggerParseResult>): OpenAPI {
        val parseOptions =
            ParseOptions().apply {
                isResolve = true
                isResolveFully = true
            }

        val results = loader(parseOptions)
        val result = results.firstOrNull { it.openAPI != null }

        if (result == null) {
            val errors = results.flatMap { it.messages ?: emptyList() }.joinToString("\n").ifEmpty { "Unknown error" }
            throw OpenApiParseException("Failed to parse OpenAPI spec: $errors")
        }

        return result.openAPI
    }
}

/**
 * Exception thrown when OpenAPI parsing fails.
 */
class OpenApiParseException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
