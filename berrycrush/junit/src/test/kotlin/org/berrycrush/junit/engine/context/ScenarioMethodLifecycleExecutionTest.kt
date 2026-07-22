package org.berrycrush.junit.engine.context

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.junit.BerryCrushSuite
import org.berrycrush.junit.DefaultBindings
import org.berrycrush.junit.ScenarioTest
import org.berrycrush.junit.engine.ClassTestDescriptor
import org.berrycrush.junit.engine.ScenarioMethodDiscoverer
import org.berrycrush.model.FragmentRegistry
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Scenario
import org.berrycrush.model.ScenarioResult
import org.berrycrush.model.Step
import org.berrycrush.model.StepType
import org.berrycrush.plugin.PluginRegistry
import org.berrycrush.runner.ScenarioRunner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScenarioMethodLifecycleExecutionTest {
    @Test
    fun `PER_CLASS lifecycle should execute beforeAll beforeEach afterEach and afterAll`() {
        PerClassLifecycleFixture.reset()

        val suite = BerryCrushSuite.create()
        val context = createContext(suite)
        val classDescriptor = discoverClassDescriptor(PerClassLifecycleFixture::class)
        val listener = RecordingListener()

        context.executeScenarioMethods(classDescriptor, listener, provider = null)

        assertEquals(1, PerClassLifecycleFixture.constructorCount)
        assertEquals(1, PerClassLifecycleFixture.beforeAllCount)
        assertEquals(2, PerClassLifecycleFixture.beforeEachCount)
        assertEquals(2, PerClassLifecycleFixture.afterEachCount)
        assertEquals(1, PerClassLifecycleFixture.afterAllCount)
        assertEquals("http://localhost/test/2", suite.configuration.baseUrl)
        assertEquals(2, listener.finished.size)
        assertTrue(listener.finished.values.all { it.status == TestExecutionResult.Status.SUCCESSFUL })
    }

    @Test
    fun `PER_METHOD lifecycle should create one instance per scenario method`() {
        PerMethodLifecycleFixture.reset()

        val suite = BerryCrushSuite.create()
        val context = createContext(suite)
        val classDescriptor = discoverClassDescriptor(PerMethodLifecycleFixture::class)
        val listener = RecordingListener()

        context.executeScenarioMethods(classDescriptor, listener, provider = null)

        assertEquals(1, PerMethodLifecycleFixture.beforeAllCount)
        assertEquals(2, PerMethodLifecycleFixture.constructorCount)
        assertEquals(2, PerMethodLifecycleFixture.beforeEachCount)
        assertEquals(2, PerMethodLifecycleFixture.afterEachCount)
        assertEquals(1, PerMethodLifecycleFixture.afterAllCount)
        assertEquals(2, listener.finished.size)
        assertTrue(listener.finished.values.all { it.status == TestExecutionResult.Status.SUCCESSFUL })
    }

    @Test
    fun `beforeEach failure should fail each scenario method execution`() {
        FailingBeforeEachFixture.reset()

        val suite = BerryCrushSuite.create()
        val context = createContext(suite)
        val classDescriptor = discoverClassDescriptor(FailingBeforeEachFixture::class)
        val listener = RecordingListener()

        context.executeScenarioMethods(classDescriptor, listener, provider = null)

        assertEquals(2, listener.finished.size)
        listener.finished.values.forEach { result ->
            assertEquals(TestExecutionResult.Status.FAILED, result.status)
            val throwable = result.throwable.orElse(null)
            assertNotNull(throwable)
            val failureMessage = throwable.message.orEmpty() + (throwable.cause?.message ?: "")
            assertTrue(failureMessage.contains("beforeEach failed"))
        }
    }

    private fun createContext(suite: BerryCrushSuite): TestExecutionContext {
        val runner = mock<ScenarioRunner>()
        whenever(runner.executeScenario(any(), anyOrNull(), anyOrNull(), anyOrNull())).thenAnswer { invocation ->
            val scenario = invocation.arguments[0] as Scenario
            ScenarioResult(scenario, ResultStatus.PASSED)
        }

        return TestExecutionContext(
            suite = suite,
            bindings = DefaultBindings(),
            pluginRegistry = PluginRegistry(),
            fragmentRegistry = FragmentRegistry(),
            stepRegistry = null,
            assertionRegistry = null,
            runner = runner,
        )
    }

    private fun discoverClassDescriptor(testClass: KClass<*>): ClassTestDescriptor {
        val engineDescriptor =
            object : EngineDescriptor(UniqueId.forEngine("berrycrush"), "test") {}
        ScenarioMethodDiscoverer.discoverScenariosForClass(engineDescriptor, testClass)
        return engineDescriptor.children.filterIsInstance<ClassTestDescriptor>().first()
    }
}

class RecordingListener : EngineExecutionListener {
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
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerClassLifecycleFixture {
    companion object {
        var constructorCount = 0
        var beforeAllCount = 0
        var beforeEachCount = 0
        var afterEachCount = 0
        var afterAllCount = 0

        fun reset() {
            constructorCount = 0
            beforeAllCount = 0
            beforeEachCount = 0
            afterEachCount = 0
            afterAllCount = 0
        }
    }

    init {
        constructorCount++
    }

    @BeforeAll
    fun beforeAll(config: BerryCrushConfiguration) {
        beforeAllCount++
        config.baseUrl = "http://localhost/test/0"
    }

    @BeforeEach
    fun beforeEach(config: BerryCrushConfiguration) {
        beforeEachCount++
        config.baseUrl = "http://localhost/test/$beforeEachCount"
    }

    @AfterEach
    fun afterEach() {
        afterEachCount++
    }

    @AfterAll
    fun afterAll() {
        afterAllCount++
    }

    @ScenarioTest
    fun first(suite: BerryCrushSuite): Scenario = createScenario("first", suite)

    @ScenarioTest
    fun second(suite: BerryCrushSuite): Scenario = createScenario("second", suite)
}

class PerMethodLifecycleFixture {
    companion object {
        var constructorCount = 0
        var beforeAllCount = 0
        var beforeEachCount = 0
        var afterEachCount = 0
        var afterAllCount = 0

        fun reset() {
            constructorCount = 0
            beforeAllCount = 0
            beforeEachCount = 0
            afterEachCount = 0
            afterAllCount = 0
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            beforeAllCount++
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            afterAllCount++
        }
    }

    init {
        constructorCount++
    }

    @BeforeEach
    fun beforeEach() {
        beforeEachCount++
    }

    @AfterEach
    fun afterEach() {
        afterEachCount++
    }

    @ScenarioTest
    fun first(suite: BerryCrushSuite): Scenario = createScenario("first", suite)

    @ScenarioTest
    fun second(suite: BerryCrushSuite): Scenario = createScenario("second", suite)
}

class FailingBeforeEachFixture {
    companion object {
        fun reset() = Unit
    }

    @BeforeEach
    fun beforeEach() {
        throw IllegalStateException("beforeEach failed")
    }

    @ScenarioTest
    fun first(suite: BerryCrushSuite): Scenario = createScenario("first", suite)

    @ScenarioTest
    fun second(suite: BerryCrushSuite): Scenario = createScenario("second", suite)
}

private fun createScenario(
    name: String,
    suite: BerryCrushSuite,
): Scenario =
    Scenario(
        name = "$name-${suite.configuration.baseUrl ?: "no-base-url"}",
        steps = listOf(Step(StepType.WHEN, description = "noop")),
    )