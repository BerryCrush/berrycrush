package org.berrycrush.step

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.context.ExecutionContext
import org.berrycrush.executor.BerryCrushConfigurationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [StepContextImpl] focusing on shared variables support.
 */
class StepContextImplTest {
    @Test
    fun `variable returns value from execution context`() {
        val context = ExecutionContext()
        context["userId"] = "user-123"

        val stepContext = createStepContext(context)

        assertEquals("user-123", stepContext.variable("userId"))
    }

    @Test
    fun `variable returns null for missing variable when sharing disabled`() {
        val context = ExecutionContext()
        val stepContext = createStepContext(context, sharingEnabled = false)

        assertNull(stepContext.variable("missing"))
    }

    @Test
    fun `variable returns shared value when sharing enabled`() {
        val context = ExecutionContext()
        val sharedVariables = mutableMapOf<String, Any?>("sharedToken" to "token-abc")

        val stepContext =
            createStepContext(
                context,
                sharedVariables = sharedVariables,
                sharingEnabled = true,
            )

        assertEquals("token-abc", stepContext.variable("sharedToken"))
    }

    @Test
    fun `scenario variable takes precedence over shared variable`() {
        val context = ExecutionContext()
        context["key"] = "scenario-value"
        val sharedVariables = mutableMapOf<String, Any?>("key" to "shared-value")

        val stepContext =
            createStepContext(
                context,
                sharedVariables = sharedVariables,
                sharingEnabled = true,
            )

        assertEquals("scenario-value", stepContext.variable("key"))
    }

    @Test
    fun `setVariable updates execution context`() {
        val context = ExecutionContext()
        val stepContext = createStepContext(context)

        stepContext.setVariable("newVar", "new-value")

        assertEquals("new-value", context.get<String>("newVar"))
    }

    @Test
    fun `setSharedVariable updates shared variables when sharing enabled`() {
        val context = ExecutionContext()
        val sharedVariables = mutableMapOf<String, Any?>()

        val stepContext =
            createStepContext(
                context,
                sharedVariables = sharedVariables,
                sharingEnabled = true,
            )

        stepContext.setSharedVariable("sharedKey", "shared-value")

        assertEquals("shared-value", sharedVariables["sharedKey"])
    }

    @Test
    fun `setSharedVariable falls back to setVariable when sharing disabled`() {
        val context = ExecutionContext()
        val stepContext = createStepContext(context, sharingEnabled = false)

        stepContext.setSharedVariable("fallbackKey", "fallback-value")

        assertEquals("fallback-value", context.get<String>("fallbackKey"))
    }

    @Test
    fun `allVariables includes both scenario and shared variables`() {
        val context = ExecutionContext()
        context["scenarioVar"] = "scenario-value"
        val sharedVariables = mutableMapOf<String, Any?>("sharedVar" to "shared-value")

        val stepContext =
            createStepContext(
                context,
                sharedVariables = sharedVariables,
                sharingEnabled = true,
            )

        val allVars = stepContext.allVariables()

        assertEquals("scenario-value", allVars["scenarioVar"])
        assertEquals("shared-value", allVars["sharedVar"])
    }

    @Test
    fun `allVariables prioritizes scenario variables over shared`() {
        val context = ExecutionContext()
        context["key"] = "scenario-wins"
        val sharedVariables = mutableMapOf<String, Any?>("key" to "shared-loses")

        val stepContext =
            createStepContext(
                context,
                sharedVariables = sharedVariables,
                sharingEnabled = true,
            )

        assertEquals("scenario-wins", stepContext.allVariables()["key"])
    }

    private fun createStepContext(
        executionContext: ExecutionContext,
        sharedVariables: MutableMap<String, Any?>? = null,
        sharingEnabled: Boolean = false,
    ): StepContextImpl {
        val config =
            BerryCrushConfiguration().apply {
                shareVariablesAcrossScenarios = sharingEnabled
            }
        return StepContextImpl(
            executionContext = executionContext,
            configuration = BerryCrushConfigurationProvider.from(config),
            sharedVariables = sharedVariables,
            sharingEnabled = sharingEnabled,
        )
    }
}
