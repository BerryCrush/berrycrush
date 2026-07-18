package org.berrycrush.autotest.provider

import io.swagger.v3.oas.models.media.Schema
import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.AutoTestType
import org.berrycrush.autotest.ParameterLocation
import java.math.BigDecimal
import java.util.UUID

private const val EXTRA_LENGTH_CHARS = 10
private const val EXTRA_ARRAY_ITEMS = 5
private const val EXTRA_OBJECT_PROPERTIES = 3

/**
 * Represents an invalid test value with its description.
 */
private data class InvalidTestValue(
    /** The invalid value to send */
    val value: Any?,
    /** Human-readable description for test reports */
    val description: String,
)

/**
 * Collection of default invalid test providers.
 */
object DefaultInvalidTestProviders {
    /**
     * All built-in invalid test providers.
     */
    val all: List<InvalidTestProvider> =
        listOf(
            MinLengthProvider(),
            MaxLengthProvider(),
            PatternProvider(),
            FormatProvider(),
            EnumProvider(),
            MinimumProvider(),
            ExclusiveMinimumProvider(),
            MaximumProvider(),
            ExclusiveMaximumProvider(),
            MultipleOfProvider(),
            ConstProvider(),
            TypeProvider(),
            RequiredProvider(),
            MinItemsProvider(),
            MaxItemsProvider(),
            UniqueItemsProvider(),
            MinPropertiesProvider(),
            MaxPropertiesProvider(),
        )
}

private fun Schema<*>.checkType(name: String): Boolean = type == name || (types != null && types.contains(name))

private fun Schema<*>.checkType(names: List<String>): Boolean = names.any { checkType(it) }

/**
 * Tests for string values shorter than minLength.
 */
class MinLengthProvider : InvalidTestProvider {
    override val testType: String = "minLength"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("string") && schema.minLength != null && schema.minLength > 0

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.minLength?.let { minLen ->
                val invalidValue = "x".repeat((minLen - 1).coerceAtLeast(0))
                listOf(InvalidTestValue(value = invalidValue, description = "String shorter than minLength ($minLen)"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for string values longer than maxLength.
 */
class MaxLengthProvider : InvalidTestProvider {
    override val testType: String = "maxLength"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("string") && schema.maxLength != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.maxLength?.let { maxLen ->
                val invalidValue = "x".repeat(maxLen + EXTRA_LENGTH_CHARS)
                listOf(InvalidTestValue(value = invalidValue, description = "String longer than maxLength ($maxLen)"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for strings not matching pattern constraint.
 */
class PatternProvider : InvalidTestProvider {
    override val testType: String = "pattern"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("string") && schema.pattern != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.pattern?.let { pattern ->
                listOf(InvalidTestValue(value = "!!!invalid_pattern!!!", description = "String not matching pattern ($pattern)"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for strings with invalid format (email, uuid, date, etc.).
 */
class FormatProvider : InvalidTestProvider {
    override val testType: String = "format"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("string") && schema.format != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.format?.let { format ->
                getInvalidValueForFormat(format)?.let { invalidValue ->
                    listOf(InvalidTestValue(value = invalidValue, description = "Invalid format ($format)"))
                }
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }

    private fun getInvalidValueForFormat(format: String): String? =
        when (format) {
            "email" -> "not-an-email"
            "uuid" -> "not-a-uuid"
            "uri", "url" -> "not-a-url"
            "date" -> "not-a-date"
            "date-time" -> "not-a-datetime"
            "ipv4" -> "not.an.ip"
            "ipv6" -> "not:an:ipv6"
            else -> "Random string! I've created, so it won't be in any format"
        }
}

/**
 * Tests for values not in enum.
 */
class EnumProvider : InvalidTestProvider {
    override val testType: String = "enum"

    override fun canHandle(schema: Schema<*>): Boolean = schema.enum != null && schema.enum.isNotEmpty()

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.enum?.let {
                listOf(InvalidTestValue(value = "INVALID_ENUM_VALUE_${UUID.randomUUID()}", description = "Value not in enum"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for numeric values below minimum.
 */
class MinimumProvider : InvalidTestProvider {
    override val testType: String = "minimum"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType(listOf("integer", "number")) && schema.minimum != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.minimum?.let { min ->
                val invalidValue = min.subtract(BigDecimal.ONE)
                listOf(InvalidTestValue(value = invalidValue, description = "Value below minimum ($min)"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for values not satisfying exclusiveMinimum boundary.
 */
class ExclusiveMinimumProvider : InvalidTestProvider {
    override val testType: String = "exclusiveMinimum"

    override fun canHandle(schema: Schema<*>): Boolean =
        schema.checkType(listOf("integer", "number")) && schema.exclusiveMinimumBoundary() != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.exclusiveMinimumBoundary()?.let { boundary ->
                listOf(
                    InvalidTestValue(
                        value = boundary,
                        description = "Value not greater than exclusiveMinimum ($boundary)",
                    ),
                )
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for numeric values above maximum.
 */
class MaximumProvider : InvalidTestProvider {
    override val testType: String = "maximum"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType(listOf("integer", "number")) && schema.maximum != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.maximum?.let { max ->
                val invalidValue = max.add(BigDecimal.ONE)
                listOf(InvalidTestValue(value = invalidValue, description = "Value above maximum ($max)"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for values not satisfying exclusiveMaximum boundary.
 */
class ExclusiveMaximumProvider : InvalidTestProvider {
    override val testType: String = "exclusiveMaximum"

    override fun canHandle(schema: Schema<*>): Boolean =
        schema.checkType(listOf("integer", "number")) && schema.exclusiveMaximumBoundary() != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.exclusiveMaximumBoundary()?.let { boundary ->
                listOf(
                    InvalidTestValue(
                        value = boundary,
                        description = "Value not less than exclusiveMaximum ($boundary)",
                    ),
                )
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for numeric values that are not a multipleOf the configured value.
 */
class MultipleOfProvider : InvalidTestProvider {
    override val testType: String = "multipleOf"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType(listOf("integer", "number")) && schema.multipleOf != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.multipleOf?.let { multipleOf ->
                val invalidValue = generateNonMultipleValue(multipleOf)
                listOf(InvalidTestValue(value = invalidValue, description = "Value not multipleOf ($multipleOf)"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for values that do not match const.
 */
class ConstProvider : InvalidTestProvider {
    override val testType: String = "const"

    override fun canHandle(schema: Schema<*>): Boolean = schema.`const` != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.`const`?.let { constValue ->
                val invalidValue = generateInvalidConstValue(constValue)
                listOf(InvalidTestValue(value = invalidValue, description = "Value different from const ($constValue)"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for type mismatches (e.g., string instead of number).
 */
class TypeProvider : InvalidTestProvider {
    override val testType: String = "type"

    override fun canHandle(schema: Schema<*>): Boolean = schema.type != null || (schema.types != null && schema.types.isNotEmpty())

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.type?.let {
                listOfNotNull(it.toInvalidTestValue())
            } ?: request.schema.types?.let {
                it.mapNotNull { type -> type.toInvalidTestValue() }
            } ?: emptyList()
        return request.toInvalidCases(testType, values)
    }

    private fun String.toInvalidTestValue(): InvalidTestValue? =
        when (this) {
            "integer", "number", "boolean", "array", "object" ->
                InvalidTestValue(
                    value = "not-a-$this",
                    description = "Invalid type (string instead of $this)",
                )
            "string" ->
                InvalidTestValue(
                    value = listOf("not", "a", "string"),
                    description = "Invalid string value",
                )
            else -> null // `null`, should be
        }
}

/**
 * Tests for missing required fields.
 */
class RequiredProvider : InvalidTestProvider {
    override val testType: String = "required"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("object")

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        if (request.location != ParameterLocation.BODY || request.fieldPath.isEmpty()) {
            return emptyList()
        }

        val modifiedBody = request.baseBody.toMutableMap()
        removeNestedField(modifiedBody, request.fieldPath)

        return listOf(
            AutoTestCase(
                type = AutoTestType.INVALID,
                testType = testType,
                fieldName = request.fieldName,
                invalidValue = null,
                description = "Missing required field '${request.fieldName}'",
                location = ParameterLocation.BODY,
                body = modifiedBody,
                tag = "Invalid request - required",
            ),
        )
    }
}

private fun removeNestedField(
    source: MutableMap<String, Any?>,
    fieldPath: List<String>,
) {
    if (fieldPath.isEmpty()) {
        return
    }

    if (fieldPath.size == 1) {
        source.remove(fieldPath.first())
        return
    }

    val parent = source[fieldPath.first()] as? Map<*, *> ?: return
    val parentMutable = parent.toMutableMap().mapKeys { it.key.toString() }.toMutableMap()
    removeNestedField(parentMutable, fieldPath.drop(1))
    source[fieldPath.first()] = parentMutable
}

/**
 * Tests for arrays with fewer items than minItems.
 */
class MinItemsProvider : InvalidTestProvider {
    override val testType: String = "minItems"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("array") && schema.minItems != null && schema.minItems > 0

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.minItems?.let { minItems ->
                listOf(InvalidTestValue(value = emptyList<Any>(), description = "Array with fewer items than minItems ($minItems)"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for arrays with more items than maxItems.
 */
class MaxItemsProvider : InvalidTestProvider {
    override val testType: String = "maxItems"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("array") && schema.maxItems != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.maxItems?.let { maxItems ->
                val tooManyItems = (0..maxItems + EXTRA_ARRAY_ITEMS).map { "item$it" }
                listOf(InvalidTestValue(value = tooManyItems, description = "Array with more items than maxItems ($maxItems)"))
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for arrays violating uniqueItems.
 */
class UniqueItemsProvider : InvalidTestProvider {
    override val testType: String = "uniqueItems"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("array") && schema.uniqueItems == true

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            if (request.schema.uniqueItems == true) {
                listOf(InvalidTestValue(value = listOf("duplicate-item", "duplicate-item"), description = "Array contains duplicate items"))
            } else {
                emptyList()
            }

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for objects with fewer properties than minProperties.
 */
class MinPropertiesProvider : InvalidTestProvider {
    override val testType: String = "minProperties"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("object") && (schema.minProperties ?: 0) > 0

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.minProperties?.takeIf { it > 0 }?.let { minProperties ->
                listOf(
                    InvalidTestValue(
                        value = buildObjectWithPropertyCount((minProperties - 1).coerceAtLeast(0)),
                        description = "Object with fewer properties than minProperties ($minProperties)",
                    ),
                )
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

/**
 * Tests for objects with more properties than maxProperties.
 */
class MaxPropertiesProvider : InvalidTestProvider {
    override val testType: String = "maxProperties"

    override fun canHandle(schema: Schema<*>): Boolean = schema.checkType("object") && schema.maxProperties != null

    override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
        val values =
            request.schema.maxProperties?.let { maxProperties ->
                val invalidPropertyCount = (maxProperties + EXTRA_OBJECT_PROPERTIES).coerceAtLeast(1)
                listOf(
                    InvalidTestValue(
                        value = buildObjectWithPropertyCount(invalidPropertyCount),
                        description = "Object with more properties than maxProperties ($maxProperties)",
                    ),
                )
            } ?: emptyList()

        return request.toInvalidCases(testType, values)
    }
}

private fun generateNonMultipleValue(multipleOf: BigDecimal): BigDecimal {
    if (multipleOf.compareTo(BigDecimal.ZERO) == 0) {
        return BigDecimal.ONE
    }

    val increments = listOf(BigDecimal.ONE, BigDecimal("0.5"), BigDecimal("0.1"))
    val candidate =
        increments
            .asSequence()
            .map { increment -> multipleOf.add(increment) }
            .firstOrNull { value -> value.remainder(multipleOf).compareTo(BigDecimal.ZERO) != 0 }

    return candidate ?: multipleOf.add(BigDecimal("0.1"))
}

private fun generateInvalidConstValue(constValue: Any): Any =
    when (constValue) {
        is String -> "${constValue}_INVALID"
        is Boolean -> !constValue
        is BigDecimal -> constValue.add(BigDecimal.ONE)
        is Int -> constValue + 1
        is Long -> constValue + 1
        is Float -> constValue + 1f
        is Double -> constValue + 1.0
        else -> "INVALID_CONST_VALUE"
    }

private fun buildObjectWithPropertyCount(propertyCount: Int): Map<String, Any> =
    (1..propertyCount).associate { index ->
        "prop$index" to "value$index"
    }

private fun Schema<*>.exclusiveMinimumBoundary(): BigDecimal? {
    if (exclusiveMinimumValue != null) {
        return exclusiveMinimumValue
    }

    return if (exclusiveMinimum == true) minimum else null
}

private fun Schema<*>.exclusiveMaximumBoundary(): BigDecimal? {
    if (exclusiveMaximumValue != null) {
        return exclusiveMaximumValue
    }

    return if (exclusiveMaximum == true) maximum else null
}

private fun InvalidTestRequest.toInvalidCases(
    testType: String,
    values: List<InvalidTestValue>,
): List<AutoTestCase> =
    values.map { value ->
        when (location) {
            ParameterLocation.BODY -> {
                val modifiedBody = baseBody.toMutableMap()
                if (value.value == null) {
                    removeNestedField(modifiedBody, fieldPath)
                } else {
                    setNestedField(modifiedBody, fieldPath, value.value)
                }
                toInvalidCase(value, testType, body = modifiedBody)
            }
            ParameterLocation.PATH -> {
                val pathParams = basePathParams.toMutableMap()
                pathParams[fieldName] = value.value
                toInvalidCase(value, testType, pathParams = pathParams)
            }
            ParameterLocation.HEADER -> {
                val headers = baseHeaders.toMutableMap()
                headers[fieldName] = value.value?.toString() ?: ""
                toInvalidCase(value, testType, headers = headers)
            }
            ParameterLocation.QUERY -> toInvalidCase(value, testType)
        }
    }

private fun InvalidTestRequest.toInvalidCase(
    value: InvalidTestValue,
    testType: String,
    body: Map<String, Any?> = baseBody,
    pathParams: Map<String, Any?> = basePathParams,
    headers: Map<String, String> = baseHeaders,
): AutoTestCase =
    AutoTestCase(
        type = AutoTestType.INVALID,
        testType = testType,
        fieldName = fieldName,
        invalidValue = value.value,
        description = value.description,
        location = location,
        body = body,
        pathParams = pathParams,
        headers = headers,
        tag = "Invalid request - $testType",
    )

private fun setNestedField(
    source: MutableMap<String, Any?>,
    fieldPath: List<String>,
    value: Any?,
) {
    if (fieldPath.isEmpty()) {
        return
    }

    if (fieldPath.size == 1) {
        source[fieldPath.first()] = value
        return
    }

    val head = fieldPath.first()
    val current = source[head] as? Map<*, *> ?: emptyMap<String, Any?>()
    val nested = current.toMutableMap().mapKeys { it.key.toString() }.toMutableMap()
    setNestedField(nested, fieldPath.drop(1), value)
    source[head] = nested
}
