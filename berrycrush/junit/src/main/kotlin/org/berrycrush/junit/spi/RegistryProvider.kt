package org.berrycrush.junit.spi

import org.berrycrush.assertion.AssertionRegistry
import org.berrycrush.util.StepRegistry

/**
 * Custom step registry provider
 */
interface RegistryProvider : Provider {
    /**
     * Creates a StepRegistry instance for the given test class.
     */
    fun createStepRegistry(testClass: Class<*>): StepRegistry?

    /**
     * Creates an AssertionRegistry instance for the given test class.
     */
    fun createAssertionRegistry(testClass: Class<*>): AssertionRegistry?
}
