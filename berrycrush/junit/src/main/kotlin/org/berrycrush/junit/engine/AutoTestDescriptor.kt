package org.berrycrush.junit.engine

import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.ParameterLocation
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor

/**
 * Test descriptor for a single auto-generated test case.
 */
class AutoTestDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    val testCase: AutoTestCase,
    val stepDescription: String,
) : AbstractTestDescriptor(uniqueId, displayName) {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST

    override fun getSource(): java.util.Optional<TestSource> = java.util.Optional.empty()

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
    }
}
