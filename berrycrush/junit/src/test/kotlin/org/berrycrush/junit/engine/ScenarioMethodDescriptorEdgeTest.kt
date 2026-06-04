package org.berrycrush.junit.engine

import org.berrycrush.dsl.BerryCrushSuite
import org.berrycrush.junit.ScenarioTest
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.berrycrush.model.Scenario as BerryScenario

class ScenarioMethodDescriptorEdgeTest {
    @Test
    fun `invokeMethod should reject unsupported parameter types`() {
        val descriptor =
            ScenarioMethodDescriptor(
                uniqueId = UniqueId.forEngine("berrycrush").append("scenario", "unsupported"),
                displayName = "unsupported",
                method = InvalidScenarioMethods::class.java.getDeclaredMethod("unsupportedParam", String::class.java),
                testClass = InvalidScenarioMethods::class.java,
            )

        val error =
            assertFailsWith<IllegalArgumentException> {
                descriptor.invokeMethod(InvalidScenarioMethods(), BerryCrushSuite.create())
            }

        assertEquals(TestDescriptor.Type.TEST, descriptor.type)
        kotlin.test.assertEquals(error.message?.contains("Unsupported parameter type"), true)
    }

    @Test
    fun `invokeMethod should reject non Scenario return types`() {
        val descriptor =
            ScenarioMethodDescriptor(
                uniqueId = UniqueId.forEngine("berrycrush").append("scenario", "wrong-return"),
                displayName = "wrong-return",
                method = InvalidScenarioMethods::class.java.getDeclaredMethod("wrongReturnType"),
                testClass = InvalidScenarioMethods::class.java,
            )

        val error =
            assertFailsWith<IllegalStateException> {
                descriptor.invokeMethod(InvalidScenarioMethods(), BerryCrushSuite.create())
            }

        kotlin.test.assertEquals(error.message?.contains("must return Scenario"), true)
    }
}

private class InvalidScenarioMethods {
    @ScenarioTest
    @Suppress("UnusedParameter")
    fun unsupportedParam(unused: String): BerryScenario =
        BerryCrushSuite.create().scenario("invalid") {
            whenever("noop") {}
        }

    @ScenarioTest
    @Suppress("FunctionOnlyReturningConstant")
    fun wrongReturnType(): String = "not-a-scenario"
}
