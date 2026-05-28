package org.berrycrush.junit.engine

import org.berrycrush.junit.BerryCrushBindings
import org.berrycrush.junit.BerryCrushConfiguration
import org.berrycrush.junit.BerryCrushSpec
import org.berrycrush.junit.ParallelExecutionMode
import org.junit.jupiter.api.Test
import org.junit.platform.engine.UniqueId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.berrycrush.config.BerryCrushConfiguration as Configuration

/**
 * Tests for @BerryCrushConfiguration annotation handling.
 */
class BerryCrushConfigurationTest {
    @Test
    fun `ClassTestDescriptor reads configuration annotation`() {
        val uniqueId = UniqueId.forEngine("berrycrush").append("class", ConfiguredTestStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, ConfiguredTestStub::class.java)

        assertNotNull(descriptor.bindingsClass)
        assertEquals(TestBindings::class.java, descriptor.bindingsClass)
        assertEquals("test-api.yaml", descriptor.openApiSpec)
        assertEquals(60_000L, descriptor.timeout)
    }

    @Test
    fun `ClassTestDescriptor uses defaults without configuration`() {
        val uniqueId = UniqueId.forEngine("berrycrush").append("class", UnconfiguredTestStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, UnconfiguredTestStub::class.java)

        assertEquals(null, descriptor.bindingsClass)
        assertEquals(null, descriptor.openApiSpec)
        assertEquals(30_000L, descriptor.timeout)
        assertEquals(ParallelExecutionMode.CONCURRENT, descriptor.parallelExecution)
    }

    @Test
    fun `ClassTestDescriptor reads parallelExecution configuration`() {
        val uniqueId = UniqueId.forEngine("berrycrush").append("class", SequentialTestStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, SequentialTestStub::class.java)

        assertEquals(ParallelExecutionMode.SAME_THREAD, descriptor.parallelExecution)
    }

    @Test
    fun `default parallelExecution is CONCURRENT`() {
        val uniqueId = UniqueId.forEngine("berrycrush").append("class", ConfiguredTestStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, ConfiguredTestStub::class.java)

        assertEquals(ParallelExecutionMode.CONCURRENT, descriptor.parallelExecution)
    }

    @Test
    fun `TestBindings provides custom values`() {
        val bindings = TestBindings()
        val values = bindings.getBindings()

        assertEquals("http://localhost:8080", values["baseUrl"])
        assertEquals("test-token", values["authToken"])
    }
}

/**
 * Test bindings implementation for testing.
 */
class TestBindings : BerryCrushBindings {
    override fun getBindings(): Map<String, Any> =
        mapOf(
            "baseUrl" to "http://localhost:8080",
            "authToken" to "test-token",
        )

    override fun configure(config: Configuration) {
        config.baseUrl = "http://localhost:8080"
    }
}

/**
 * Sample configured test class.
 * Used only for testing configuration parsing, not for actual test execution.
 */
@BerryCrushSpec("test-api.yaml")
@BerryCrushConfiguration(
    bindings = TestBindings::class,
    timeout = 60_000L,
)
class ConfiguredTestStub

/**
 * Sample unconfigured test class.
 * Used only for testing default values, not for actual test execution.
 */
class UnconfiguredTestStub

/**
 * Sample test class configured for sequential (same-thread) execution.
 * Used for testing parallelExecution configuration.
 */
@BerryCrushConfiguration(parallelExecution = ParallelExecutionMode.SAME_THREAD)
class SequentialTestStub
