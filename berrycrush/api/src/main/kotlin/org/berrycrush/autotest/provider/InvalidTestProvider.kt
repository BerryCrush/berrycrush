package org.berrycrush.autotest.provider

import io.swagger.v3.oas.models.media.Schema
import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.ParameterLocation

/**
 * Input for invalid test-case generation.
 *
 * Providers must treat this as immutable request context and return deterministic
 * [AutoTestCase] outputs for the same input.
 */
data class InvalidTestRequest(
    /** Field name under test. For nested required tests, this can be dotted (e.g. `user.name`). */
    val fieldName: String,
    /** Field path for nested handling; defaults to [fieldName]. */
    val fieldPath: List<String> = if (fieldName.isBlank()) emptyList() else listOf(fieldName),
    /** Schema to test for this request. */
    val schema: Schema<*>,
    /** Parameter location under test. */
    val location: ParameterLocation = ParameterLocation.BODY,
    /** Base body values used as mutation source for body tests. */
    val baseBody: Map<String, Any?> = emptyMap(),
    /** Base path params used as mutation source for path tests. */
    val basePathParams: Map<String, Any?> = emptyMap(),
    /** Base headers used as mutation source for header tests. */
    val baseHeaders: Map<String, String> = emptyMap(),
    /** Required fields for object schemas (used by required providers). */
    val requiredFields: Set<String> = emptySet(),
)

/**
 * Provider interface for generating invalid request test values.
 *
 * Implement this interface to add custom invalid value generation strategies.
 * Providers are discovered via ServiceLoader, allowing extensions without
 * modifying the core library.
 *
 * ## Example Implementation
 *
 * ```kotlin
 * class NumericOverflowProvider : InvalidTestProvider {
 *     override val testType: String = "numericOverflow"
 *
 *     override fun canHandle(schema: Schema<*>): Boolean =
 *         schema.type == "integer" || schema.type == "number"
 *
 *     override fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase> {
 *         val body = request.baseBody.toMutableMap().apply {
 *             this[request.fieldName] = Long.MAX_VALUE
 *         }
 *
 *         return listOf(
 *             AutoTestCase(
 *                 type = org.berrycrush.autotest.AutoTestType.INVALID,
 *                 fieldName = request.fieldName,
 *                 invalidValue = Long.MAX_VALUE,
 *                 description = "Numeric overflow value",
 *                 location = request.location,
 *                 body = body,
 *                 pathParams = request.basePathParams,
 *                 headers = request.baseHeaders,
 *                 tag = "Invalid request - $testType",
 *             ),
 *         )
 *     }
 * }
 * ```
 *
 * ## Registration
 *
 * Add to `META-INF/services/org.berrycrush.autotest.provider.InvalidTestProvider`:
 * ```
 * com.example.NumericOverflowProvider
 * ```
 *
 * @see InvalidTestRequest
 */
interface InvalidTestProvider {
    /**
     * Unique identifier for this test type.
     *
     * Used for:
     * - Display name in test reports: `[Invalid request - {testType}]`
     * - Exclude configuration: `excludes: [{testType}]`
     * - User-provided providers override built-in ones with same testType
     */
    val testType: String

    /**
     * Check if this provider can handle the given schema.
     *
     * @param schema The OpenAPI schema to check
     * @return true if this provider can generate invalid values for the schema
     */
    fun canHandle(schema: Schema<*>): Boolean

    /**
     * Generate complete invalid [AutoTestCase] entries.
     */
    fun generateTestCases(request: InvalidTestRequest): List<AutoTestCase>

    /**
     * Priority of this provider. Higher values = higher priority.
     * User-provided providers default to 100, built-in providers default to 0.
     */
    val priority: Int get() = 0
}
