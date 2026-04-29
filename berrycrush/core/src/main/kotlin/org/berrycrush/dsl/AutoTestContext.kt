package org.berrycrush.dsl

import org.berrycrush.autotest.ParameterLocation
import org.berrycrush.scenario.AutoTestType

/**
 * Context available during auto-test execution.
 *
 * Provides information about the current auto-test being executed,
 * including the test type, target field, and injected value.
 *
 * Usage in DSL:
 * ```kotlin
 * scenario("Test API") {
 *     when_("Create pet") {
 *         call("createPet") {
 *             autoTest(AutoTestType.INVALID, AutoTestType.SECURITY)
 *         }
 *         // During auto-test execution, autoTestContext is available
 *     }
 * }
 * ```
 *
 * @property type Type of auto-test (INVALID, SECURITY)
 * @property field Name of the field being tested
 * @property description Human-readable description of the test
 * @property value The injected value for the test
 * @property location Where the parameter is located (BODY, PATH, QUERY, HEADER)
 */
data class AutoTestContext(
    val type: AutoTestType,
    val field: String,
    val description: String,
    val value: Any?,
    val location: ParameterLocation,
) {
    companion object {
        /**
         * Context key for storing AutoTestContext in ExecutionContext.
         */
        const val CONTEXT_KEY = "__autoTestContext"
    }
}
