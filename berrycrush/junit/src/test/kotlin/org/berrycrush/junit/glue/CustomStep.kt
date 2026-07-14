package org.berrycrush.junit.glue

import org.berrycrush.step.Step
import org.berrycrush.step.StepContext
import org.junit.jupiter.api.Assertions.assertEquals

class CustomStep {
    @Step("the param name {string} must be {any}")
    fun checkValue(
        name: String,
        expected: Any?,
        context: StepContext,
    ) {
        println("Checking $name with $expected")
        assertEquals(expected, context.variable(name).toString())
    }
}
