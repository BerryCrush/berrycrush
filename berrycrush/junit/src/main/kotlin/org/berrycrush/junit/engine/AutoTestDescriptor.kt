package org.berrycrush.junit.engine

import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.ParameterLocation
import org.berrycrush.model.AutoTestResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import java.util.Optional

/**
 * Test descriptor for a single auto-generated test case.
 */
class AutoTestDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    testSource: TestSource? = null,
) : AbstractTestDescriptor(uniqueId, displayName, testSource) {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST

    override fun getSource(): Optional<TestSource> = Optional.empty()

    companion object {
        /** Maximum length for displaying invalid value in test name. */
        private const val MAX_VALUE_DISPLAY_LENGTH = 30

        /**
         * Create a display name for an auto-test case.
         * Format: [{tag}] {location} {fieldName} with value {value}
         */
        fun createDisplayName(testCase: AutoTestCase): String {
            val typeLabel = "[${testCase.tag}]"
            val location =
                when (testCase.location) {
                    ParameterLocation.BODY -> "request body"
                    ParameterLocation.PATH -> "path variable"
                    ParameterLocation.HEADER -> "header"
                    ParameterLocation.QUERY -> "query parameter"
                }
            val valueStr = testCase.invalidValue?.toString()?.take(MAX_VALUE_DISPLAY_LENGTH) ?: "null"
            val valueSuffix = if (valueStr.length >= MAX_VALUE_DISPLAY_LENGTH) "..." else ""
            return "$typeLabel $location ${testCase.fieldName} with value $valueStr$valueSuffix"
        }

        fun buildAutoTestFailureMessage(autoResult: AutoTestResult): String =
            buildString {
                append(createDisplayName(autoResult.testCase))
                append("\n")
                if (autoResult.error != null) {
                    append("  Error: ${autoResult.error}")
                } else {
                    append("  Status: ${autoResult.statusCode ?: "N/A"}")
                    autoResult.assertionResults.filter { !it.passed }.forEach { assertion ->
                        append("\n  - ${assertion.message}")
                    }
                }
                if (autoResult.responseBody != null) {
                    append("\n  Response: ${autoResult.responseBody}")
                }
            }
    }
}
