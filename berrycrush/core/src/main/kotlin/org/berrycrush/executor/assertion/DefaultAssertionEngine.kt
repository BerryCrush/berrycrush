package org.berrycrush.executor.assertion

import com.jayway.jsonpath.JsonPath
import org.berrycrush.assertion.AssertionRegistry
import org.berrycrush.assertion.SchemaValidator
import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.model.Condition
import org.berrycrush.model.ConditionOperator
import org.berrycrush.model.LogicalOperator
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.openapi.SchemaSpec
import org.berrycrush.openapi.findResponse
import tools.jackson.databind.ObjectMapper
import java.net.http.HttpResponse
import io.swagger.v3.oas.models.media.Schema as SwaggerSchema

private const val BODY_PREVIEW_LENGTH = 200
private const val MS_PER_SECOND = 1000L

/**
 * Default implementation of [AssertionEngine] that evaluates conditions
 * against HTTP responses and generates assertion messages.
 *
 * This implementation supports all condition types defined in the BerryCrush DSL:
 * - Status code conditions (exact match, range, pattern)
 * - JSON path conditions (with various operators)
 * - Header conditions
 * - Body contains conditions
 * - Schema validation conditions
 * - Response time conditions
 * - Variable conditions
 * - Custom assertions
 * - Compound conditions (AND/OR)
 * - Negated conditions
 *
 * @property configuration Optional configuration for custom assertion execution
 * @property assertionRegistry Optional registry for custom assertion definitions
 */
@Suppress("TooManyFunctions") // Dispatcher pattern with many condition type handlers
class DefaultAssertionEngine(
    private val configuration: BerryCrushConfiguration? = null,
    private val assertionRegistry: AssertionRegistry? = null,
) : AssertionEngine {
    private val objectMapper = ObjectMapper()
    private val schemaValidator = SchemaValidator(objectMapper)

    override fun evaluate(
        condition: Condition,
        context: AssertionContext,
    ): ConditionResult {
        val passed = evaluateCondition(context.response, condition, context)
        val message = generateMessage(condition, passed, context)
        val actual = getActualValueForCondition(context, condition)
        return ConditionResult(passed, message, actual)
    }

    override fun generateMessage(
        condition: Condition,
        passed: Boolean,
        context: AssertionContext,
    ): String = generateAssertionMessage(context, condition, passed)

    // ========== Condition Evaluation ==========

    /**
     * Evaluate a condition against the response.
     */
    @Suppress("CyclomaticComplexMethod") // Dispatcher pattern - complexity from many condition types
    private fun evaluateCondition(
        response: HttpResponse<String>?,
        condition: Condition,
        context: AssertionContext,
    ): Boolean =
        when (condition) {
            is Condition.Status -> evaluateStatusCondition(response, condition)
            is Condition.JsonPath -> evaluateJsonPathCondition(context, condition)
            is Condition.Header -> evaluateHeaderCondition(response, condition, context)
            is Condition.Variable -> evaluateVariableCondition(condition, context)
            is Condition.Negated -> !evaluateCondition(response, condition.condition, context)
            is Condition.Compound -> evaluateCompoundCondition(response, condition, context)
            is Condition.BodyContains -> evaluateBodyContainsCondition(context, condition)
            is Condition.Schema -> evaluateSchemaCondition(response, context)
            is Condition.ResponseTime -> evaluateResponseTimeCondition(condition, context)
            is Condition.CustomAssertion -> evaluateCustomAssertionCondition(condition, context)
            is Condition.Custom -> evaluateCustomPredicateCondition(condition, context)
        }

    /**
     * Evaluate a status code condition.
     */
    private fun evaluateStatusCondition(
        response: HttpResponse<String>?,
        condition: Condition.Status,
    ): Boolean {
        val actual = response?.statusCode() ?: return false
        return when (val expected = condition.expected) {
            is Number -> actual == expected.toInt()
            is IntRange -> actual in expected
            is String -> {
                // Handle patterns like "2xx", "20x", "200-299"
                val pattern = expected.lowercase()
                when {
                    pattern.contains('-') -> {
                        val (start, end) = pattern.split('-').map { it.trim().toIntOrNull() }
                        start?.let { s -> end?.let { e -> actual in s..e } } ?: false
                    }
                    pattern.contains('x') -> {
                        val regex = pattern.replace("x", "\\d").toRegex()
                        actual.toString().matches(regex)
                    }
                    else -> expected.toIntOrNull()?.let { actual == it } ?: false
                }
            }
            else -> false
        }
    }

    /**
     * Evaluate a JSON path condition.
     */
    private fun evaluateJsonPathCondition(
        context: AssertionContext,
        condition: Condition.JsonPath,
    ): Boolean {
        val body = context.responseBody ?: ""
        val actualValue = runCatching { JsonPath.read<Any>(body, condition.path) }.getOrNull()
        val expectedValue = condition.expected?.let { resolveConditionValue(it, context) }
        return evaluateOperator(actualValue, expectedValue, condition.operator)
    }

    /**
     * Evaluate a header condition.
     */
    @Suppress("CyclomaticComplexMethod") // Many operators need handling
    private fun evaluateHeaderCondition(
        response: HttpResponse<String>?,
        condition: Condition.Header,
        context: AssertionContext,
    ): Boolean {
        val headerValues = response?.headers()?.allValues(condition.name) ?: emptyList()
        val actualValue = headerValues.firstOrNull()
        val expectedValue = condition.expected?.let { resolveConditionValue(it, context) }

        return when (condition.operator) {
            ConditionOperator.EXISTS -> headerValues.isNotEmpty()
            ConditionOperator.NOT_EXISTS -> headerValues.isEmpty()
            ConditionOperator.EQUALS -> actualValue == expectedValue?.toString()
            ConditionOperator.NOT_EQUALS -> actualValue != expectedValue?.toString()
            ConditionOperator.CONTAINS -> actualValue?.contains(expectedValue?.toString() ?: "") ?: false
            ConditionOperator.NOT_CONTAINS -> !(actualValue?.contains(expectedValue?.toString() ?: "") ?: false)
            ConditionOperator.MATCHES -> actualValue?.matches((expectedValue?.toString() ?: "").toRegex()) ?: false
            ConditionOperator.GREATER_THAN, ConditionOperator.LESS_THAN,
            ConditionOperator.HAS_SIZE, ConditionOperator.NOT_EMPTY,
            -> false // Not applicable for headers
        }
    }

    /**
     * Evaluate a variable condition.
     */
    private fun evaluateVariableCondition(
        condition: Condition.Variable,
        context: AssertionContext,
    ): Boolean {
        val actual: Any? = context.variables[condition.name]
        val expected = condition.expected
        return evaluateOperator(actual, expected, condition.operator)
    }

    /**
     * Evaluate a compound condition (AND/OR).
     */
    private fun evaluateCompoundCondition(
        response: HttpResponse<String>?,
        condition: Condition.Compound,
        context: AssertionContext,
    ): Boolean {
        val leftResult = evaluateCondition(response, condition.left, context)
        return when (condition.operator) {
            LogicalOperator.AND -> leftResult && evaluateCondition(response, condition.right, context)
            LogicalOperator.OR -> leftResult || evaluateCondition(response, condition.right, context)
        }
    }

    /**
     * Evaluate a body contains condition.
     */
    private fun evaluateBodyContainsCondition(
        context: AssertionContext,
        condition: Condition.BodyContains,
    ): Boolean {
        val body = context.responseBody ?: ""
        val text = resolveConditionValue(condition.text, context).toString()
        return body.contains(text)
    }

    /**
     * Evaluate a schema condition by validating the response against the OpenAPI schema.
     */
    @Suppress("ReturnCount") // Multiple early returns for validation guards
    private fun evaluateSchemaCondition(
        response: HttpResponse<String>?,
        context: AssertionContext,
    ): Boolean {
        val operation = context.currentOperation ?: return true // Can't validate without operation
        val responseBody = response?.body() ?: return true // Empty body passes validation

        // Find the schema for this response status code
        val schemaSpec = findResponseSchema(operation, response.statusCode()) ?: return true

        // Get raw swagger schema for validation
        @Suppress("UNCHECKED_CAST")
        val rawSchema = schemaSpec.rawSchema as? SwaggerSchema<*> ?: return true

        // Validate the response body against the schema
        val errors = schemaValidator.validate(responseBody, rawSchema)
        return errors.isEmpty()
    }

    /**
     * Find the response schema for a given status code from the operation's response definitions.
     */
    private fun findResponseSchema(
        operation: ResolvedOperation,
        statusCode: Int,
    ): SchemaSpec? {
        val response = operation.findResponse(statusCode) ?: return null
        return response.content
            .values
            .first()
            .schema
    }

    /**
     * Evaluate a response time condition.
     */
    private fun evaluateResponseTimeCondition(
        condition: Condition.ResponseTime,
        context: AssertionContext,
    ): Boolean {
        val actualMs = context.responseTimeMs ?: return true
        val maxMs = condition.maxMs
        val threshold = parseTimeToMs(maxMs, context)
        return actualMs <= threshold
    }

    /**
     * Parse a time value to milliseconds.
     * Supports formats: 500, "500", "500ms", "2s"
     */
    private fun parseTimeToMs(
        value: Any,
        context: AssertionContext,
    ): Long =
        when (value) {
            is Number -> value.toLong()
            is String -> {
                val resolved = context.executionContext.interpolate(value)
                when {
                    resolved.endsWith("ms") -> resolved.dropLast(2).trim().toLongOrNull() ?: 0L
                    resolved.endsWith("s") -> {
                        (resolved.dropLast(1).trim().toDoubleOrNull() ?: 0.0).times(MS_PER_SECOND).toLong()
                    }
                    else -> resolved.toLongOrNull() ?: 0L
                }
            }
            else -> 0L
        }

    /**
     * Evaluate a custom assertion by invoking the matching assertion from the registry.
     */
    @Suppress("ReturnCount") // Multiple early returns for validation guards
    private fun evaluateCustomAssertionCondition(
        condition: Condition.CustomAssertion,
        context: AssertionContext,
    ): Boolean {
        val registry = assertionRegistry ?: return false
        val config = configuration ?: return false

        val match = registry.findMatch(condition.pattern) ?: return false

        val assertionContext =
            org.berrycrush.assertion.AssertionContextImpl(
                executionContext = context.executionContext,
                configuration = config,
                sharedVariables = null,
                sharingEnabled = false,
            )

        return runCatching {
            val method = match.definition.method
            val parameters = match.parameters.toTypedArray()

            // Check if method accepts AssertionContext as last parameter
            val methodParams = method.parameters
            val args =
                if (methodParams.isNotEmpty() &&
                    methodParams.last().type.isAssignableFrom(
                        org.berrycrush.assertion.AssertionContext::class.java,
                    )
                ) {
                    arrayOf(*parameters, assertionContext)
                } else {
                    parameters
                }

            val result = method.invoke(match.definition.instance, *args)

            when (result) {
                is org.berrycrush.assertion.AssertionResult -> result.passed
                is Boolean -> result
                else -> true // Assume passed if no AssertionResult returned
            }
        }.getOrElse { e ->
            // Unwrap InvocationTargetException
            val actualException =
                when (e) {
                    is java.lang.reflect.InvocationTargetException -> e.cause ?: e
                    else -> e
                }

            // AssertionError means the assertion failed
            actualException !is AssertionError
        }
    }

    /**
     * Evaluate a custom predicate condition (from DSL conditional).
     */
    private fun evaluateCustomPredicateCondition(
        condition: Condition.Custom,
        context: AssertionContext,
    ): Boolean {
        val testContext = org.berrycrush.context.MutableTestExecutionContext(context.executionContext)
        return runCatching {
            condition.predicate(testContext)
        }.getOrElse { false }
    }

    // ========== Operator Evaluation ==========

    /**
     * Shared operator evaluation logic for variable and JSON path conditions.
     */
    @Suppress("CyclomaticComplexMethod") // Many operators need handling
    private fun evaluateOperator(
        actual: Any?,
        expected: Any?,
        operator: ConditionOperator,
    ): Boolean =
        when (operator) {
            ConditionOperator.EXISTS -> actual != null
            ConditionOperator.NOT_EXISTS -> actual == null
            ConditionOperator.EQUALS -> actual == expected || actual?.toString() == expected?.toString()
            ConditionOperator.NOT_EQUALS -> actual != expected && actual?.toString() != expected?.toString()
            ConditionOperator.CONTAINS ->
                when (actual) {
                    is String -> actual.contains(expected?.toString() ?: "")
                    is Collection<*> -> actual.contains(expected)
                    else -> false
                }
            ConditionOperator.NOT_CONTAINS ->
                when (actual) {
                    is String -> !actual.contains(expected?.toString() ?: "")
                    is Collection<*> -> !actual.contains(expected)
                    else -> true
                }
            ConditionOperator.MATCHES -> {
                val pattern = expected?.toString() ?: ""
                actual?.toString()?.matches(pattern.toRegex()) ?: false
            }
            ConditionOperator.GREATER_THAN -> compareAsNumbers(actual, expected) { a, e -> a > e }
            ConditionOperator.LESS_THAN -> compareAsNumbers(actual, expected) { a, e -> a < e }
            ConditionOperator.HAS_SIZE -> {
                val actualSize = sizeOf(actual) ?: return false
                val expectedSize =
                    (expected as? Number)?.toInt()
                        ?: expected?.toString()?.toIntOrNull()
                        ?: return false
                actualSize == expectedSize
            }
            ConditionOperator.NOT_EMPTY ->
                when (actual) {
                    is Collection<*> -> actual.isNotEmpty()
                    is String -> actual.isNotEmpty()
                    is Array<*> -> actual.isNotEmpty()
                    else -> actual != null
                }
        }

    /**
     * Get the size of a collection, string, or array.
     */
    private fun sizeOf(value: Any?): Int? =
        when (value) {
            is Collection<*> -> value.size
            is String -> value.length
            is Array<*> -> value.size
            else -> null
        }

    /**
     * Compare two values as numbers using the provided comparison function.
     */
    private inline fun compareAsNumbers(
        actual: Any?,
        expected: Any?,
        compare: (Double, Double) -> Boolean,
    ): Boolean {
        val actualNum =
            (actual as? Number)?.toDouble()
                ?: actual?.toString()?.toDoubleOrNull()
                ?: return false
        val expectedNum =
            (expected as? Number)?.toDouble()
                ?: expected?.toString()?.toDoubleOrNull()
                ?: return false
        return compare(actualNum, expectedNum)
    }

    /**
     * Resolve condition value, handling variable interpolation.
     */
    private fun resolveConditionValue(
        value: Any,
        context: AssertionContext,
    ): Any =
        when (value) {
            is String -> context.executionContext.interpolate(value)
            else -> value
        }

    // ========== Message Generation ==========

    /**
     * Generate a human-readable message for an assertion result.
     */
    @Suppress("CyclomaticComplexMethod") // Dispatcher pattern - one branch per condition type
    private fun generateAssertionMessage(
        context: AssertionContext,
        condition: Condition,
        passed: Boolean,
    ): String =
        when (condition) {
            is Condition.Status -> statusMessage(context, condition, passed)
            is Condition.JsonPath -> jsonPathMessage(context, condition, passed)
            is Condition.Header -> headerMessage(context, condition, passed)
            is Condition.BodyContains -> bodyContainsMessage(condition, passed)
            is Condition.Schema -> if (passed) "Response matches schema" else "Response does not match schema"
            is Condition.ResponseTime -> responseTimeMessage(condition, passed, context)
            is Condition.Variable -> variableMessage(condition, passed, context)
            is Condition.Negated -> negatedMessage(context, condition, passed)
            is Condition.Compound -> if (passed) "Compound condition passed" else "Compound condition failed"
            is Condition.CustomAssertion -> customAssertionMessage(condition, passed)
            is Condition.Custom -> if (passed) "Custom predicate passed" else "Custom predicate failed"
        }

    private fun statusMessage(
        context: AssertionContext,
        condition: Condition.Status,
        passed: Boolean,
    ): String {
        val actual = context.statusCode ?: 0
        return if (passed) "Status code is $actual" else "Expected status ${condition.expected} but got $actual"
    }

    private fun jsonPathMessage(
        context: AssertionContext,
        condition: Condition.JsonPath,
        passed: Boolean,
    ): String {
        val body = context.responseBody ?: ""
        val actualValue = runCatching { JsonPath.read<Any>(body, condition.path) }.getOrNull()
        val expectedValue = condition.expected?.let { resolveConditionValue(it, context) }
        return if (passed) {
            "${condition.path} ${condition.operator.name.lowercase()} ${expectedValue ?: ""}"
        } else {
            "Assertion failed at ${condition.path}\n" +
                "  Operator: ${condition.operator.name.lowercase()}\n" +
                "  Expected: ${expectedValue ?: "(none)"}\n" +
                "  Actual:   $actualValue"
        }
    }

    private fun headerMessage(
        context: AssertionContext,
        condition: Condition.Header,
        passed: Boolean,
    ): String {
        val actualValue = context.responseHeaders[condition.name]?.firstOrNull()
        return if (passed) {
            "Header ${condition.name} ${condition.operator.name.lowercase()} ${condition.expected ?: ""}"
        } else {
            "Header assertion failed: ${condition.name} ${condition.operator.name.lowercase()}, actual: $actualValue"
        }
    }

    private fun bodyContainsMessage(
        condition: Condition.BodyContains,
        passed: Boolean,
    ): String {
        val text = condition.text.toString()
        return if (passed) "Body contains '$text'" else "Body does not contain '$text'"
    }

    private fun responseTimeMessage(
        condition: Condition.ResponseTime,
        passed: Boolean,
        context: AssertionContext,
    ): String {
        val actualMs = context.responseTimeMs
        return if (passed) {
            "Response time ${actualMs}ms is under ${condition.maxMs}"
        } else {
            "Response time exceeded: ${actualMs}ms > ${condition.maxMs}"
        }
    }

    private fun variableMessage(
        condition: Condition.Variable,
        passed: Boolean,
        context: AssertionContext,
    ): String {
        val actual = context.variables[condition.name]
        return if (passed) {
            "${condition.name} ${condition.operator.name.lowercase()} ${condition.expected}"
        } else {
            "Variable ${condition.name}: expected ${condition.expected}, got $actual"
        }
    }

    private fun negatedMessage(
        context: AssertionContext,
        condition: Condition.Negated,
        passed: Boolean,
    ): String {
        val innerMessage = generateAssertionMessage(context, condition.condition, !passed)
        return if (passed) "NOT: condition was false (assertion passed)" else "Negated assertion failed: $innerMessage"
    }

    private fun customAssertionMessage(
        condition: Condition.CustomAssertion,
        passed: Boolean,
    ): String =
        if (passed) {
            "Custom assertion passed: ${condition.pattern}"
        } else {
            "Custom assertion failed: ${condition.pattern}"
        }

    // ========== Actual Value Extraction ==========

    /**
     * Get the actual value from the response for a given condition type.
     */
    private fun getActualValueForCondition(
        context: AssertionContext,
        condition: Condition,
    ): Any? =
        when (condition) {
            is Condition.Status -> context.statusCode
            is Condition.JsonPath -> {
                val body = context.responseBody ?: ""
                runCatching { JsonPath.read<Any>(body, condition.path) }.getOrNull()
            }
            is Condition.Header -> context.responseHeaders[condition.name]?.firstOrNull()
            is Condition.BodyContains -> context.responseBody?.take(BODY_PREVIEW_LENGTH)
            is Condition.Variable -> context.variables[condition.name]
            is Condition.ResponseTime -> context.responseTimeMs
            is Condition.Negated -> getActualValueForCondition(context, condition.condition)
            else -> null
        }
}
