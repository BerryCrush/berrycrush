package org.berrycrush.formatter

import org.berrycrush.exception.AssertionException
import org.berrycrush.exception.ErrorContextConfig
import org.berrycrush.exception.HttpExecutionException
import org.berrycrush.exception.ScenarioErrorContext
import org.berrycrush.exception.SchemaValidationException
import org.berrycrush.model.HttpRequest
import org.berrycrush.model.HttpResponse

private const val DEFAULT_MAX_BODY_SIZE = 1024
private const val DIFF_CONTEXT_SIZE = 3
private const val HTTP_ERROR_STATUS_THRESHOLD = 400

/**
 * Formats enhanced error context information for test failure output.
 *
 * Provides rich, colorized formatting of error details including:
 * - Scenario context (name, file, step, line number)
 * - HTTP request/response with header masking
 * - Assertion expected/actual with diff highlighting
 * - Schema validation errors
 *
 * ## Usage
 *
 * ```kotlin
 * val formatter = ErrorContextFormatter(ColorScheme.DEFAULT)
 * val output = formatter.format(exception)
 * println(output)
 * ```
 *
 * @property colorScheme Color scheme for terminal output. Use [ColorScheme.NONE] for plain text.
 */
@Suppress("TooManyFunctions") // Rich formatting requires dedicated methods for each error type
class ErrorContextFormatter(
    private val colorScheme: ColorScheme = ColorScheme.NONE,
) {
    companion object {
        /**
         * Create a plain text formatter with no colors.
         */
        fun plain(): ErrorContextFormatter = ErrorContextFormatter(ColorScheme.NONE)

        /**
         * Create a colored formatter with the default color scheme.
         */
        fun colored(): ErrorContextFormatter = ErrorContextFormatter(ColorScheme.DEFAULT)
    }

    /**
     * Format any BerryCrush exception with appropriate context.
     *
     * @param exception The exception to format
     * @return Formatted error string
     */
    fun format(exception: Throwable): String =
        when (exception) {
            is HttpExecutionException -> formatHttpException(exception)
            is AssertionException -> formatAssertionException(exception)
            is SchemaValidationException -> formatSchemaException(exception)
            else -> formatGenericException(exception)
        }

    /**
     * Format HTTP execution exception with full request/response context.
     */
    internal fun formatHttpException(exception: HttpExecutionException): String =
        buildString {
            appendLine(colorize("HTTP Execution Error", AnsiColors.BOLD + AnsiColors.RED))
            appendLine()
            appendLine("${exception.method} ${exception.url}")
            exception.cause?.let { appendLine("Cause: ${it.message}") }

            exception.scenarioContext?.let { ctx ->
                appendLine()
                appendScenarioContext(ctx)
            }

            exception.request?.let { req ->
                appendLine()
                appendRequest(req, exception.config)
            }

            exception.response?.let { resp ->
                appendLine()
                appendResponse(resp, exception.config)
            }
        }

    /**
     * Format assertion exception with expected/actual comparison.
     */
    internal fun formatAssertionException(exception: AssertionException): String =
        buildString {
            appendLine(colorize("Assertion Failed", AnsiColors.BOLD + AnsiColors.RED))
            appendLine()
            exception.expression?.let { appendLine("Expression: $it") }
            appendLine("Type: ${exception.assertionType}")
            appendLine()

            appendLine(colorize("Expected:", AnsiColors.GREEN))
            val expected = exception.expected
            appendLine("  ${formatValue(expected)}")
            appendLine()
            appendLine(colorize("Actual:", AnsiColors.RED))
            val actual = exception.actual
            appendLine("  ${formatValue(actual)}")

            // Show diff if both are strings
            if (expected is String && actual is String) {
                val diff = computeDiff(expected, actual)
                if (diff.isNotEmpty()) {
                    appendLine()
                    appendLine(colorize("Diff:", AnsiColors.YELLOW))
                    appendLine(diff)
                }
            }

            exception.scenarioContext?.let { ctx ->
                appendLine()
                appendScenarioContext(ctx)
            }

            exception.response?.let { resp ->
                appendLine()
                appendResponsePreview(resp)
            }
        }

    /**
     * Format schema validation exception.
     */
    internal fun formatSchemaException(exception: SchemaValidationException): String =
        buildString {
            appendLine(colorize("Schema Validation Failed", AnsiColors.BOLD + AnsiColors.RED))
            appendLine()
            exception.schemaPath?.let { appendLine("Schema Path: $it") }
            appendLine()
            appendLine("Errors:")
            exception.errors.forEach { error ->
                appendLine("  ${colorize("•", AnsiColors.RED)} $error")
            }

            exception.scenarioContext?.let { ctx ->
                appendLine()
                appendScenarioContext(ctx)
            }

            exception.response?.let { resp ->
                appendLine()
                appendResponsePreview(resp)
            }
        }

    /**
     * Format a generic exception.
     */
    internal fun formatGenericException(exception: Throwable): String =
        buildString {
            appendLine(colorize("Error: ${exception::class.simpleName}", AnsiColors.BOLD + AnsiColors.RED))
            appendLine()
            appendLine(exception.message ?: "No message")
        }

    /**
     * Format scenario error context.
     */
    fun formatScenarioContext(context: ScenarioErrorContext): String =
        buildString {
            appendScenarioContext(context)
        }

    /**
     * Format HTTP request for display.
     */
    fun formatRequest(
        request: HttpRequest,
        config: ErrorContextConfig = ErrorContextConfig(),
    ): String =
        buildString {
            appendRequest(request, config)
        }

    /**
     * Format HTTP response for display.
     */
    fun formatResponse(
        response: HttpResponse,
        config: ErrorContextConfig = ErrorContextConfig(),
    ): String =
        buildString {
            appendResponse(response, config)
        }

    // --- Private helper methods ---

    private fun StringBuilder.appendScenarioContext(context: ScenarioErrorContext) {
        appendLine(colorize("━━━ Scenario Context ━━━", AnsiColors.DIM))
        appendLine("Scenario: ${colorize(context.scenarioName, AnsiColors.BOLD)}")
        context.scenarioFile?.let { appendLine("File: $it") }
        context.stepDescription?.let {
            append("Step: $it")
            context.stepLine?.let { line -> append(" ${colorize("(line $line)", AnsiColors.DIM)}") }
            appendLine()
        }
        context.stepIndex?.let { appendLine("Step Index: $it") }
        context.operationId?.let { appendLine("Operation: $it") }
    }

    private fun StringBuilder.appendRequest(
        request: HttpRequest,
        config: ErrorContextConfig,
    ) {
        appendLine(colorize("━━━ Request ━━━", AnsiColors.DIM))
        appendLine("${request.method} ${request.url}")

        if (request.headers.isNotEmpty()) {
            appendLine("Headers:")
            request.headers.forEach { (name, values) ->
                val displayValue =
                    if (config.maskedHeaders.any { it.equals(name, ignoreCase = true) }) {
                        colorize("[MASKED]", AnsiColors.YELLOW)
                    } else {
                        values
                    }
                appendLine("  $name: $displayValue")
            }
        }

        val body = request.body
        if (config.includeRequestBody && body != null) {
            appendLine()
            appendTruncatedBody("Request Body", body, config.maxBodySize)
        }
    }

    private fun StringBuilder.appendResponse(
        response: HttpResponse,
        config: ErrorContextConfig,
    ) {
        appendLine(colorize("━━━ Response ━━━", AnsiColors.DIM))
        val statusColor =
            if (response.statusCode >= HTTP_ERROR_STATUS_THRESHOLD) AnsiColors.RED else AnsiColors.GREEN
        appendLine("Status: ${colorize("${response.statusCode} ${response.statusMessage}", statusColor)}")
        appendLine("Duration: ${response.duration.toMillis()}ms")

        if (response.headers.isNotEmpty()) {
            appendLine("Headers:")
            response.headers.forEach { (name, values) ->
                appendLine("  $name: ${values.joinToString(", ")}")
            }
        }

        val body = response.body
        if (config.includeResponseBody && body != null) {
            appendLine()
            appendTruncatedBody("Response Body", body, config.maxBodySize)
        }
    }

    private fun StringBuilder.appendResponsePreview(response: HttpResponse) {
        appendLine(colorize("━━━ Response Preview ━━━", AnsiColors.DIM))
        appendLine("Status: ${response.statusCode} ${response.statusMessage}")
        response.body?.let { body ->
            val preview = body.take(DEFAULT_MAX_BODY_SIZE)
            appendLine("Body: $preview${if (body.length > DEFAULT_MAX_BODY_SIZE) "..." else ""}")
        }
    }

    private fun StringBuilder.appendTruncatedBody(
        label: String,
        body: String,
        maxSize: Int,
    ) {
        if (body.length <= maxSize) {
            appendLine("$label:")
            appendLine(body)
        } else {
            appendLine("$label ${colorize("(truncated from ${body.length} to $maxSize bytes)", AnsiColors.DIM)}:")
            appendLine(body.take(maxSize))
            appendLine(colorize("...", AnsiColors.DIM))
        }
    }

    private fun formatValue(value: Any?): String =
        when (value) {
            null -> colorize("null", AnsiColors.DIM)
            is String -> "\"$value\""
            is Collection<*> -> value.joinToString(", ", "[", "]") { formatValue(it) }
            is Map<*, *> -> value.entries.joinToString(", ", "{", "}") { "${it.key}: ${formatValue(it.value)}" }
            else -> value.toString()
        }

    private fun computeDiff(
        expected: String,
        actual: String,
    ): String {
        if (expected == actual) return ""

        val expectedLines = expected.lines()
        val actualLines = actual.lines()

        // Simple line-by-line diff for short strings
        if (expectedLines.size <= DIFF_CONTEXT_SIZE * 2 && actualLines.size <= DIFF_CONTEXT_SIZE * 2) {
            return buildString {
                expectedLines.forEachIndexed { i, line ->
                    val actualLine = actualLines.getOrNull(i)
                    when {
                        actualLine == null -> appendLine(colorize("- $line", AnsiColors.RED))
                        actualLine != line -> {
                            appendLine(colorize("- $line", AnsiColors.RED))
                            appendLine(colorize("+ $actualLine", AnsiColors.GREEN))
                        }
                    }
                }
                // Extra lines in actual
                if (actualLines.size > expectedLines.size) {
                    actualLines.drop(expectedLines.size).forEach { line ->
                        appendLine(colorize("+ $line", AnsiColors.GREEN))
                    }
                }
            }.trimEnd()
        }

        // For longer strings, show character position of first difference
        val diffPos = expected.zip(actual).indexOfFirst { (e, a) -> e != a }
        return if (diffPos >= 0) {
            "First difference at position $diffPos: " +
                "expected '${expected.getOrNull(diffPos)}' but got '${actual.getOrNull(diffPos)}'"
        } else if (expected.length != actual.length) {
            "Length differs: expected ${expected.length}, got ${actual.length}"
        } else {
            ""
        }
    }

    private fun colorize(
        text: String,
        code: String,
    ): String = if (code.isEmpty() || colorScheme == ColorScheme.NONE) text else AnsiColors.wrap(text, code)
}
