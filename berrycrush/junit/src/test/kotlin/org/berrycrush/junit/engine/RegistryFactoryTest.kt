package org.berrycrush.junit.engine

import org.berrycrush.assertion.Assertion
import org.berrycrush.junit.BerryCrushConfiguration
import org.berrycrush.step.Step
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegistryFactoryTest {
    @Test
    fun `createStepRegistry should return null when no classes or packages configured`() {
        assertNull(RegistryFactory.createStepRegistry(emptyArray(), emptyArray()))
    }

    @Test
    fun `createAssertionRegistry should return null when no classes or packages configured`() {
        assertNull(RegistryFactory.createAssertionRegistry(emptyArray(), emptyArray()))
    }

    @Test
    fun `createStepRegistry should scan explicit step classes`() {
        val registry = RegistryFactory.createStepRegistry(arrayOf(StepDefinitions::class))

        assertNotNull(registry)
        assertTrue(registry.allDefinitions().any { it.pattern == "a valid step" })
    }

    @Test
    fun `createAssertionRegistry should scan explicit assertion classes`() {
        val registry = RegistryFactory.createAssertionRegistry(arrayOf(AssertionDefinitions::class))

        assertNotNull(registry)
        assertTrue(registry.allDefinitions().any { it.pattern == "a valid assertion" })
    }

    @Test
    fun `create registries should scan configured packages`() {
        val packageName = this::class.java.packageName

        val stepRegistry = RegistryFactory.createStepRegistry(emptyArray(), arrayOf(packageName))
        val assertionRegistry = RegistryFactory.createAssertionRegistry(emptyArray(), arrayOf(packageName))

        assertNotNull(stepRegistry)
        assertNotNull(assertionRegistry)
        assertTrue(stepRegistry.allDefinitions().any { it.pattern == "a valid step" })
        assertTrue(assertionRegistry.allDefinitions().any { it.pattern == "a valid assertion" })
    }

    @Test
    fun `create registries from annotation should read test class configuration`() {
        val stepRegistry = RegistryFactory.createStepRegistry(ConfiguredRegistryTestClass::class.java)
        val assertionRegistry = RegistryFactory.createAssertionRegistry(ConfiguredRegistryTestClass::class.java)

        assertNotNull(stepRegistry)
        assertNotNull(assertionRegistry)
        assertEquals(1, stepRegistry.allDefinitions().count { it.pattern == "a valid step" })
        assertEquals(1, assertionRegistry.allDefinitions().count { it.pattern == "a valid assertion" })
    }

    @Test
    fun `create registries should return null when class has no configuration`() {
        assertNull(RegistryFactory.createStepRegistry(UnconfiguredRegistryTestClass::class.java))
        assertNull(RegistryFactory.createAssertionRegistry(UnconfiguredRegistryTestClass::class.java))
    }
}

class StepDefinitions {
    @Step("a valid step")
    fun validStep() = Unit
}

class AssertionDefinitions {
    @Assertion("a valid assertion")
    fun validAssertion() = Unit
}

@BerryCrushConfiguration(
    stepClasses = [StepDefinitions::class],
    assertionClasses = [AssertionDefinitions::class],
)
private class ConfiguredRegistryTestClass

private class UnconfiguredRegistryTestClass
