package org.berrycrush.autotest.provider

import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.ParameterLocation

/**
 * Input for security test-case generation.
 *
 * Providers must return deterministic [AutoTestCase] outputs for equivalent input.
 */
data class SecurityTestRequest(
    /** Field or parameter name under test. */
    val fieldName: String,
    /** Parameter location under test. */
    val location: ParameterLocation = ParameterLocation.BODY,
    /** Base body values used as mutation source for body tests. */
    val baseBody: Map<String, Any?> = emptyMap(),
    /** Base path params used as mutation source for path tests. */
    val basePathParams: Map<String, Any?> = emptyMap(),
    /** Base headers used as mutation source for header tests. */
    val baseHeaders: Map<String, String> = emptyMap(),
)

/**
 * Provider interface for generating security test payloads.
 *
 * Implement this interface to add custom security test payloads for
 * attack vector testing. Providers are discovered via ServiceLoader,
 * allowing extensions without modifying the core library.
 *
 * ## Example Implementation
 *
 * ```kotlin
 * class NoSqlInjectionProvider : SecurityTestProvider {
 *     override val testType: String = "NoSQLInjection"
 *
 *     override fun applicableLocations(): Set<ParameterLocation> =
 *         setOf(ParameterLocation.BODY, ParameterLocation.QUERY)
 *
 *     override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> {
 *         val payload = "{\"\$ne\": null}"
 *         val body = request.baseBody.toMutableMap().apply { this[request.fieldName] = payload }
 *
 *         return listOf(
 *             AutoTestCase(
 *                 type = org.berrycrush.autotest.AutoTestType.SECURITY,
 *                 fieldName = request.fieldName,
 *                 invalidValue = payload,
 *                 description = "$displayName: MongoDB injection",
 *                 location = request.location,
 *                 body = body,
 *                 pathParams = request.basePathParams,
 *                 headers = request.baseHeaders,
 *                 tag = "security - $displayName",
 *             ),
 *         )
 *     }
 * }
 * ```
 *
 * ## Registration
 *
 * Add to `META-INF/services/org.berrycrush.autotest.provider.SecurityTestProvider`:
 * ```
 * com.example.NoSqlInjectionProvider
 * ```
 *
 * @see SecurityTestRequest
 */
interface SecurityTestProvider {
    /**
     * Unique identifier for this security test type.
     *
     * Used for:
     * - Exclude configuration: `excludes: [{testType}]`
     * - User-provided providers override built-in ones with same testType
     */
    val testType: String

    /**
     * Human-readable display name for test reports.
     *
     * Defaults to [testType] if not overridden.
     * Used in: `[security] {displayName}: {payload.name}`
     */
    val displayName: String get() = testType

    /**
     * Parameter locations where this security test applies.
     *
     * For example:
     * - SQL injection typically applies to body and query parameters
     * - Path traversal typically applies to path parameters
     * - Header injection applies to headers
     */
    fun applicableLocations(): Set<ParameterLocation>

    /**
     * Generate complete security [AutoTestCase] entries.
     */
    fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase>

    /**
     * Priority of this provider. Higher values = higher priority.
     * User-provided providers default to 100, built-in providers default to 0.
     */
    val priority: Int get() = 0
}

/**
 * Represents a security test payload.
 */
data class SecurityPayload(
    /** Name for test reporting (e.g., "Single quote", "Script tag") */
    val name: String,
    /** The actual payload to inject */
    val payload: String,
)
