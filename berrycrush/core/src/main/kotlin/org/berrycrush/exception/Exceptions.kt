package org.berrycrush.exception

/**
 * Base exception for all BerryCrush errors.
 */
open class BerryCrushException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Thrown when an OpenAPI operation cannot be found.
 */
class OperationNotFoundException(
    val operationId: String,
    val availableOperations: List<String>,
) : BerryCrushException(
        buildMessage(operationId, availableOperations),
    ) {
    companion object {
        private fun buildMessage(
            operationId: String,
            available: List<String>,
        ): String {
            val suggestions = findSimilar(operationId, available)
            return buildString {
                append("Operation '$operationId' not found in OpenAPI spec.")
                if (suggestions.isNotEmpty()) {
                    append(" Did you mean: ${suggestions.joinToString(", ")}?")
                }
            }
        }

        private fun findSimilar(
            target: String,
            candidates: List<String>,
        ): List<String> {
            val targetLower = target.lowercase()
            return candidates
                .filter { it.lowercase().contains(targetLower) || targetLower.contains(it.lowercase()) }
                .take(3)
        }
    }
}

/**
 * Thrown when value extraction fails.
 */
class ExtractionException(
    val variableName: String,
    val jsonPath: String,
    val responseBody: String?,
    cause: Throwable? = null,
) : BerryCrushException(
        "Failed to extract '$variableName' using JSONPath '$jsonPath' from response: " +
            (responseBody?.take(200) ?: "<empty>"),
        cause,
    )

/**
 * Thrown when HTTP request execution fails.
 */
class HttpExecutionException(
    val url: String,
    method: String,
    cause: Throwable,
) : BerryCrushException(
        "HTTP request failed: $method $url - ${cause.message}",
        cause,
    )

/**
 * Thrown when schema validation fails.
 */
class SchemaValidationException(
    errors: List<String>,
) : BerryCrushException(
        "Schema validation failed:\n${errors.joinToString("\n") { "  - $it" }}",
    )

/**
 * Thrown when configuration is invalid.
 */
class ConfigurationException(
    message: String,
) : BerryCrushException(message)

/**
 * Thrown when scenario parsing fails.
 */
class ScenarioParseException(
    message: String,
    val line: Int? = null,
    val column: Int? = null,
    cause: Throwable? = null,
) : BerryCrushException(
        buildString {
            if (line != null) {
                append("[$line")
                if (column != null) append(":$column")
                append("] ")
            }
            append(message)
        },
        cause,
    )
