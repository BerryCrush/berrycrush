package org.berrycrush.exception

import org.berrycrush.plugin.HttpMethod
import org.berrycrush.plugin.HttpRequest
import org.berrycrush.plugin.HttpResponse

private const val SIMILAR_MATCHES_LIMIT = 3
private const val RESPONSE_PREVIEW_LENGTH = 200
private const val DEFAULT_MAX_BODY_SIZE = 4096
private const val CONTEXT_LINES_BEFORE = 2
private const val CONTEXT_LINES_AFTER = 2
private const val LINE_NUMBER_PAD_WIDTH = 4

/**
 * Default headers to mask in error output for security.
 */
val DEFAULT_MASKED_HEADERS =
    setOf(
        "authorization",
        "cookie",
        "set-cookie",
        "x-api-key",
        "x-auth-token",
        "bearer",
    )

/**
 * Configuration for error context output.
 *
 * @property includeRequestBody Whether to include request body in error messages
 * @property includeResponseBody Whether to include response body in error messages
 * @property maxBodySize Maximum body size to include (larger bodies are truncated)
 * @property maskedHeaders Header names to mask (case-insensitive)
 */
data class ErrorContextConfig(
    val includeRequestBody: Boolean = true,
    val includeResponseBody: Boolean = true,
    val maxBodySize: Int = DEFAULT_MAX_BODY_SIZE,
    val maskedHeaders: Set<String> = DEFAULT_MASKED_HEADERS,
)

/**
 * Context information for scenario execution errors.
 *
 * @property scenarioName Name of the scenario being executed
 * @property scenarioFile Path to the scenario file
 * @property stepDescription Description of the current step
 * @property stepIndex Zero-based index of the step in the scenario
 * @property stepLine Source line number of the step (if available)
 * @property operationId OpenAPI operation ID (if applicable)
 */
data class ScenarioErrorContext(
    val scenarioName: String,
    val scenarioFile: String? = null,
    val stepDescription: String? = null,
    val stepIndex: Int? = null,
    val stepLine: Int? = null,
    val operationId: String? = null,
)

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
                .take(SIMILAR_MATCHES_LIMIT)
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
            (responseBody?.take(RESPONSE_PREVIEW_LENGTH) ?: "<empty>"),
        cause,
    )

/**
 * Thrown when HTTP request execution fails.
 *
 * Provides rich context including full request/response details for debugging.
 *
 * @property url The request URL
 * @property method HTTP method used
 * @property request The full HTTP request snapshot (with headers masked per config)
 * @property response The full HTTP response snapshot if available (with body truncated per config)
 * @property scenarioContext Scenario/step context where failure occurred
 * @property config Error context configuration for masking/truncation
 */
class HttpExecutionException(
    val url: String,
    val method: HttpMethod,
    cause: Throwable,
    val request: HttpRequest? = null,
    val response: HttpResponse? = null,
    val scenarioContext: ScenarioErrorContext? = null,
    val config: ErrorContextConfig = ErrorContextConfig(),
) : BerryCrushException(
        buildDetailedMessage(method, url, cause, request, response, scenarioContext, config),
        cause,
    ) {
    companion object {
        private fun buildDetailedMessage(
            method: HttpMethod,
            url: String,
            cause: Throwable,
            request: HttpRequest?,
            response: HttpResponse?,
            scenarioContext: ScenarioErrorContext?,
            config: ErrorContextConfig,
        ): String =
            buildString {
                append("HTTP request failed: $method $url")
                append("\nCause: ${cause.message}")

                scenarioContext?.let { ctx ->
                    append("\n\n--- Scenario Context ---")
                    append("\nScenario: ${ctx.scenarioName}")
                    ctx.scenarioFile?.let { append("\nFile: $it") }
                    ctx.stepDescription?.let { append("\nStep: $it") }
                    ctx.stepLine?.let { append(" (line $it)") }
                    ctx.operationId?.let { append("\nOperation: $it") }
                }

                request?.let { req ->
                    append("\n\n--- Request ---")
                    append("\n${req.method} ${req.url}")
                    appendMaskedHeaders(req.headers, config.maskedHeaders)
                    if (config.includeRequestBody && req.body != null) {
                        appendTruncatedBody(req.body, config.maxBodySize, "Request Body")
                    }
                }

                response?.let { resp ->
                    append("\n\n--- Response ---")
                    append("\nStatus: ${resp.statusCode} ${resp.statusMessage}")
                    append("\nDuration: ${resp.duration.toMillis()}ms")
                    appendHeaders(resp.headers)
                    if (config.includeResponseBody && resp.body != null) {
                        appendTruncatedBody(resp.body, config.maxBodySize, "Response Body")
                    }
                }
            }

        private fun StringBuilder.appendMaskedHeaders(
            headers: Map<String, String>,
            maskedHeaders: Set<String>,
        ) {
            headers.forEach { (name, values) ->
                val displayValues =
                    if (maskedHeaders.any { it.equals(name, ignoreCase = true) }) {
                        listOf("[MASKED]")
                    } else {
                        values
                    }
                append("\n  $name: $displayValues")
            }
        }

        private fun StringBuilder.appendHeaders(headers: Map<String, List<String>>) {
            headers.forEach { (name, values) ->
                append("\n  $name: ${values.joinToString(", ")}")
            }
        }

        private fun StringBuilder.appendTruncatedBody(
            body: String,
            maxSize: Int,
            label: String,
        ) {
            if (body.length <= maxSize) {
                append("\n$label:\n$body")
            } else {
                append("\n$label (truncated from ${body.length} to $maxSize bytes):")
                append("\n${body.take(maxSize)}...")
            }
        }
    }
}

/**
 * Thrown when schema validation fails.
 *
 * @property errors List of validation error messages
 * @property schemaPath Path in the schema where validation failed
 * @property scenarioContext Scenario/step context where failure occurred
 * @property response The response being validated (if available)
 */
class SchemaValidationException(
    val errors: List<String>,
    val schemaPath: String? = null,
    val scenarioContext: ScenarioErrorContext? = null,
    val response: HttpResponse? = null,
) : BerryCrushException(
        buildMessage(errors, schemaPath, scenarioContext, response),
    ) {
    companion object {
        private fun buildMessage(
            errors: List<String>,
            schemaPath: String?,
            scenarioContext: ScenarioErrorContext?,
            response: HttpResponse?,
        ): String =
            buildString {
                append("Schema validation failed")
                schemaPath?.let { append(" at path: $it") }
                append(":")
                errors.forEach { append("\n  - $it") }

                scenarioContext?.let { ctx ->
                    append("\n\nScenario: ${ctx.scenarioName}")
                    ctx.stepLine?.let { append(" (line $it)") }
                    ctx.operationId?.let { append("\nOperation: $it") }
                }

                response?.let { resp ->
                    append("\n\nResponse Status: ${resp.statusCode}")
                    resp.body?.let { body ->
                        val preview = body.take(RESPONSE_PREVIEW_LENGTH)
                        append("\nResponse (preview): $preview")
                        if (body.length > RESPONSE_PREVIEW_LENGTH) append("...")
                    }
                }
            }
    }
}

/**
 * Thrown when an assertion fails during test execution.
 *
 * @property expected The expected value
 * @property actual The actual value received
 * @property assertionType Type of assertion (e.g., "status", "jsonpath", "header")
 * @property expression The expression or path being asserted (e.g., JSONPath)
 * @property scenarioContext Scenario/step context where failure occurred
 * @property response The response snapshot at failure time
 */
class AssertionException(
    val expected: Any?,
    val actual: Any?,
    val assertionType: String,
    val expression: String? = null,
    val scenarioContext: ScenarioErrorContext? = null,
    val response: HttpResponse? = null,
) : BerryCrushException(
        buildMessage(expected, actual, assertionType, expression, scenarioContext, response),
    ) {
    companion object {
        private fun buildMessage(
            expected: Any?,
            actual: Any?,
            assertionType: String,
            expression: String?,
            scenarioContext: ScenarioErrorContext?,
            response: HttpResponse?,
        ): String =
            buildString {
                append("Assertion failed")
                expression?.let { append(" for $it") }
                append(" [$assertionType]")

                append("\n  Expected: ${formatValue(expected)}")
                append("\n  Actual:   ${formatValue(actual)}")

                computeDiff(expected, actual)?.let { diff ->
                    append("\n  Diff:     $diff")
                }

                scenarioContext?.let { ctx ->
                    append("\n\nScenario: ${ctx.scenarioName}")
                    ctx.stepDescription?.let { append("\nStep: $it") }
                    ctx.stepLine?.let { append(" (line $it)") }
                    ctx.operationId?.let { append("\nOperation: $it") }
                }

                response?.let { resp ->
                    append("\n\nResponse Status: ${resp.statusCode} ${resp.statusMessage}")
                    resp.body?.let { body ->
                        val preview = body.take(RESPONSE_PREVIEW_LENGTH)
                        append("\nResponse Body (preview): $preview")
                        if (body.length > RESPONSE_PREVIEW_LENGTH) append("...")
                    }
                }
            }

        private fun formatValue(value: Any?): String =
            when (value) {
                null -> "<null>"
                is String -> "\"$value\""
                is Collection<*> -> value.toString()
                else -> value.toString()
            }

        private fun computeDiff(
            expected: Any?,
            actual: Any?,
        ): String? {
            if (expected is String && actual is String) {
                return when {
                    expected.length != actual.length ->
                        "length differs (expected ${expected.length}, got ${actual.length})"
                    else -> null
                }
            }
            return null
        }
    }
}

/**
 * Thrown when configuration is invalid.
 */
class ConfigurationException(
    message: String,
) : BerryCrushException(message)

/**
 * Thrown when scenario parsing fails.
 *
 * Provides source context window to help identify the exact location of the error.
 *
 * @property line Line number where error occurred (1-based)
 * @property column Column number where error occurred (1-based, optional)
 * @property sourceFile Path to the source file (optional)
 * @property sourceContent Source lines around the error for context display
 */
class ScenarioParseException(
    message: String,
    val line: Int? = null,
    val column: Int? = null,
    val sourceFile: String? = null,
    val sourceContent: List<String>? = null,
    cause: Throwable? = null,
) : BerryCrushException(
        buildMessage(message, line, column, sourceFile, sourceContent),
        cause,
    ) {
    companion object {
        /**
         * Create a parse exception with source context window.
         *
         * @param message Error message
         * @param line Line number (1-based)
         * @param column Column number (1-based, optional)
         * @param sourceFile Source file path
         * @param allLines All source lines for context extraction
         * @param cause Underlying cause
         */
        fun withSourceContext(
            message: String,
            line: Int,
            column: Int? = null,
            sourceFile: String? = null,
            allLines: List<String>,
            cause: Throwable? = null,
        ): ScenarioParseException {
            val contextLines = extractContextLines(line, allLines)
            return ScenarioParseException(message, line, column, sourceFile, contextLines, cause)
        }

        private fun extractContextLines(
            errorLine: Int,
            allLines: List<String>,
        ): List<String> {
            val startLine = maxOf(0, errorLine - 1 - CONTEXT_LINES_BEFORE)
            val endLine = minOf(allLines.size, errorLine + CONTEXT_LINES_AFTER)
            return allLines.subList(startLine, endLine)
        }

        private fun buildMessage(
            message: String,
            line: Int?,
            column: Int?,
            sourceFile: String?,
            sourceContent: List<String>?,
        ): String =
            buildString {
                append("Parse error")
                sourceFile?.let { append(" in $it") }
                if (line != null) {
                    append(" at line $line")
                    column?.let { append(", column $it") }
                }
                append(": $message")

                sourceContent?.let { lines ->
                    if (lines.isNotEmpty() && line != null) {
                        append("\n\n")
                        val startLineNum = maxOf(1, line - CONTEXT_LINES_BEFORE)
                        lines.forEachIndexed { index, lineContent ->
                            val lineNum = startLineNum + index
                            val marker = if (lineNum == line) ">" else " "
                            append("$marker ${lineNum.toString().padStart(LINE_NUMBER_PAD_WIDTH)}: $lineContent\n")
                        }
                        // Add column indicator if available
                        column?.let { col ->
                            val padding = "        " + " ".repeat(col - 1)
                            append("$padding^\n")
                        }
                    }
                }
            }
    }
}
