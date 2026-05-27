package org.berrycrush.executor.fragment

import org.berrycrush.context.ExecutionContext
import org.berrycrush.exception.ConfigurationException
import org.berrycrush.model.Fragment
import org.berrycrush.model.FragmentRegistry
import org.berrycrush.model.Step
import org.berrycrush.model.StepType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultFragmentExecutorTest {
    // --- expand() Tests ---

    @Test
    fun `expand - step without fragment returns original step`() {
        val executor = DefaultFragmentExecutor(null)
        val step = createStep(operationId = "getPets")

        val expanded = executor.expand(step)

        assertEquals(1, expanded.size)
        assertEquals(step, expanded[0])
    }

    @Test
    fun `expand - step with fragment returns fragment steps`() {
        val fragmentSteps =
            listOf(
                createStep(operationId = "login"),
                createStep(operationId = "getProfile"),
            )
        val fragment = Fragment(name = "authenticate", steps = fragmentSteps)
        val registry = FragmentRegistry()
        registry.register(fragment)

        val executor = DefaultFragmentExecutor(registry)
        val step = createStep(fragmentName = "authenticate")

        val expanded = executor.expand(step)

        assertEquals(2, expanded.size)
        assertEquals("login", expanded[0].operationId)
        assertEquals("getProfile", expanded[1].operationId)
    }

    @Test
    fun `expand - missing fragment throws ConfigurationException`() {
        val registry = FragmentRegistry()
        val executor = DefaultFragmentExecutor(registry)
        val step = createStep(fragmentName = "nonexistent")

        val exception =
            assertFailsWith<ConfigurationException> {
                executor.expand(step)
            }

        assertTrue(exception.message!!.contains("nonexistent"))
        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `expand - null registry throws ConfigurationException for fragment reference`() {
        val executor = DefaultFragmentExecutor(null)
        val step = createStep(fragmentName = "someFragment")

        assertFailsWith<ConfigurationException> {
            executor.expand(step)
        }
    }

    // --- injectParameters() Tests ---

    @Test
    fun `injectParameters - injects simple string parameters`() {
        val executor = DefaultFragmentExecutor(null)
        val step =
            createStep(
                fragmentName = "createEntity",
                includeParameters = mapOf("entityName" to "Pet"),
            )
        val context = ExecutionContext()

        executor.injectParameters(step, context)

        assertEquals("Pet", context.get<String>("entityName"))
    }

    @Test
    fun `injectParameters - injects numeric parameters`() {
        val executor = DefaultFragmentExecutor(null)
        val step =
            createStep(
                fragmentName = "paginateResults",
                includeParameters =
                    mapOf(
                        "page" to 1,
                        "limit" to 10,
                    ),
            )
        val context = ExecutionContext()

        executor.injectParameters(step, context)

        assertEquals(1, context.get<Int>("page"))
        assertEquals(10, context.get<Int>("limit"))
    }

    @Test
    fun `injectParameters - interpolates variable references`() {
        val executor = DefaultFragmentExecutor(null)
        val step =
            createStep(
                fragmentName = "createEntity",
                includeParameters = mapOf("userId" to "${"$"}{currentUserId}"),
            )
        val context = ExecutionContext()
        context["currentUserId"] = "user123"

        executor.injectParameters(step, context)

        assertEquals("user123", context.get<String>("userId"))
    }

    @Test
    fun `injectParameters - handles mustache-style variable references`() {
        val executor = DefaultFragmentExecutor(null)
        val step =
            createStep(
                fragmentName = "createEntity",
                includeParameters = mapOf("userId" to "{{currentUserId}}"),
            )
        val context = ExecutionContext()
        context["currentUserId"] = "user456"

        executor.injectParameters(step, context)

        assertEquals("user456", context.get<String>("userId"))
    }

    @Test
    fun `injectParameters - empty parameters does nothing`() {
        val executor = DefaultFragmentExecutor(null)
        val step =
            createStep(
                fragmentName = "createEntity",
                includeParameters = emptyMap(),
            )
        val context = ExecutionContext()
        context["existing"] = "value"

        executor.injectParameters(step, context)

        // Context should remain unchanged
        assertEquals("value", context.get<String>("existing"))
    }

    @Test
    fun `injectParameters - overwrites existing variables`() {
        val executor = DefaultFragmentExecutor(null)
        val step =
            createStep(
                fragmentName = "createEntity",
                includeParameters = mapOf("name" to "NewValue"),
            )
        val context = ExecutionContext()
        context["name"] = "OldValue"

        executor.injectParameters(step, context)

        assertEquals("NewValue", context.get<String>("name"))
    }

    @Test
    fun `injectParameters - interpolates embedded variables in strings`() {
        val executor = DefaultFragmentExecutor(null)
        val step =
            createStep(
                fragmentName = "createEntity",
                includeParameters = mapOf("message" to "Hello, ${"$"}{userName}!"),
            )
        val context = ExecutionContext()
        context["userName"] = "World"

        executor.injectParameters(step, context)

        assertEquals("Hello, World!", context.get<String>("message"))
    }

    // --- Helper Functions ---

    private fun createStep(
        operationId: String? = null,
        fragmentName: String? = null,
        includeParameters: Map<String, Any> = emptyMap(),
    ): Step =
        Step(
            type = StepType.WHEN,
            description = "test step",
            operationId = operationId,
            fragmentName = fragmentName,
            includeParameters = includeParameters,
        )
}
