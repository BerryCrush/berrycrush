package org.berrycrush.autotest

/**
 * Types of auto-generated tests.
 */
enum class AutoTestType {
    /** Invalid request tests - violate schema constraints */
    INVALID,

    /** Security tests - injection and attack patterns */
    SECURITY,

    /** Multi-request idempotency tests - sequential and concurrent */
    MULTI,
}

/**
 * Represents a generated test case.
 */
data class AutoTestCase(
    /** Type of auto test (invalid/security) */
    val type: AutoTestType,
    /** Test type, e.g. minLength, */
    val testType: String,
    /** Field being tested */
    val fieldName: String,
    /** The invalid/malicious value */
    val invalidValue: Any?,
    /** Human-readable description */
    val description: String,
    /** Location of the parameter (body, path, header, query) */
    val location: ParameterLocation = ParameterLocation.BODY,
    /** Complete request body for this test (for body parameters) */
    val body: Map<String, Any?> = emptyMap(),
    /** Path parameters for this test (for path parameters) */
    val pathParams: Map<String, Any?> = emptyMap(),
    /** Headers for this test (for header parameters) */
    val headers: Map<String, String> = emptyMap(),
    /** Tag to apply to this test */
    val tag: String,
)
