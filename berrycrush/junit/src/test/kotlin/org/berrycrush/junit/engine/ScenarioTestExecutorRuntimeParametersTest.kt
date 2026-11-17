package org.berrycrush.junit.engine

import org.berrycrush.junit.BerryCrushBindings
import org.berrycrush.junit.BerryCrushConfiguration
import org.berrycrush.junit.BerryCrushScenarios
import org.berrycrush.junit.BerryCrushSpec
import org.berrycrush.junit.BerryCrushSuite
import org.berrycrush.junit.ScenarioTest
import org.berrycrush.junit.binding.OpenApiSpecValue
import org.berrycrush.junit.glue.CustomStep
import org.berrycrush.model.Scenario
import org.berrycrush.model.Step
import org.berrycrush.model.StepType
import org.junit.jupiter.api.Test
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScenarioTestExecutorRuntimeParametersTest {
    private val executor = ScenarioTestExecutor(bindingsProviders = emptyList(), registryProviders = emptyList())

    @Test
    fun `method scenario should receive runtime parameters in custom step context`() {
        val classDescriptor = classDescriptorFor(MethodScenarioWithRuntimeParameters::class)
        val listener = RecordingListener()

        executor.executeClassTests(classDescriptor, listener)

        assertTrue(listener.finished.isNotEmpty())
        assertEquals(0, listener.failedCount())
    }

    @Test
    fun `file scenario should receive runtime parameters in custom step context`() {
        val classDescriptor = classDescriptorFor(FileScenarioWithRuntimeParameters::class)
        val listener = RecordingListener()

        executor.executeClassTests(classDescriptor, listener)

        assertTrue(classDescriptor.children.isNotEmpty())
        assertEquals(0, listener.failedCount())
    }

    @Test
    fun `existing OpenApiSpecValue and baseUrl binding behavior should remain valid`() {
        val bindings = RuntimeBindings()
        val suite = BerryCrushSuite.create()

        bindings.configure(suite.configuration)
        val allBindings = bindings.getBindings()

        assertEquals("http://localhost:8080", allBindings["baseUrl"])
        val defaultSpec = allBindings["default"] as OpenApiSpecValue
        assertEquals("test-api.yaml", defaultSpec.location)
        assertEquals("http://localhost:8080", defaultSpec.baseUrl)
    }

    private fun classDescriptorFor(testClass: KClass<*>): ClassTestDescriptor {
        val engineId = UniqueId.forEngine("berrycrush")
        val engineDescriptor = object : EngineDescriptor(engineId, "test") {}

        ScenarioTestDiscoverer.discoverScenariosForClass(engineDescriptor, testClass)
        ScenarioMethodDiscoverer.discoverScenariosForClass(engineDescriptor, testClass)

        return engineDescriptor.children.filterIsInstance<ClassTestDescriptor>().first()
    }
}

private class RecordingListener : EngineExecutionListener {
    val finished = LinkedHashMap<String, TestExecutionResult>()

    override fun executionStarted(testDescriptor: TestDescriptor) = Unit

    override fun executionFinished(
        testDescriptor: TestDescriptor,
        testExecutionResult: TestExecutionResult,
    ) {
        finished[testDescriptor.uniqueId.toString()] = testExecutionResult
    }

    override fun dynamicTestRegistered(testDescriptor: TestDescriptor) = Unit

    override fun executionSkipped(
        testDescriptor: TestDescriptor,
        reason: String,
    ) = Unit

    override fun reportingEntryPublished(
        testDescriptor: TestDescriptor,
        entry: ReportEntry,
    ) = Unit

    fun failedCount(): Int = finished.values.count { it.status == TestExecutionResult.Status.FAILED }
}

@BerryCrushSpec(paths = ["classpath:/test-api.yaml"])
@BerryCrushConfiguration(
    bindings = RuntimeBindings::class,
    stepClasses = [CustomStep::class],
)
class MethodScenarioWithRuntimeParameters {
    @ScenarioTest
    fun runtimeParamsFromMethod(): Scenario =
        Scenario(
            name = "runtime parameter method test",
            steps =
                listOf(
                    Step(
                        type = StepType.THEN,
                        description = "the param name \"param.tenantId\" must be tenantA",
                    ),
                ),
        )
}

@BerryCrushScenarios(locations = ["runtime/runtime-parameters.scenario"])
@BerryCrushConfiguration(
    bindings = RuntimeBindings::class,
    stepClasses = [CustomStep::class],
)
class FileScenarioWithRuntimeParameters

class RuntimeBindings : BerryCrushBindings {
    override fun getBindings(): Map<String, Any> =
        mapOf(
            "default" to OpenApiSpecValue("test-api.yaml", "http://localhost:8080"),
            "baseUrl" to "http://localhost:8080",
        )

    override fun getRuntimeParameters(): Map<String, Any> =
        mapOf(
            "tenantId" to "tenantA",
        )
}
