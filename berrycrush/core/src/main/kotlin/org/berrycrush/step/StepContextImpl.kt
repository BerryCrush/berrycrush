package org.berrycrush.step

import org.berrycrush.context.ExecutionContext
import org.berrycrush.plugin.HttpResponse

/**
 * Default implementation of [StepContext] that wraps an [ExecutionContext].
 *
 * Provides access to test variables, last HTTP response, and configuration
 * for custom step implementations.
 *
 * @property stepContext The underlying execution context
 * @property sharedVariables Optional shared variables map for suite-scoped variables
 * @property sharingEnabled Whether variable sharing is enabled
 */
class StepContextImpl(
    private val stepContext: org.berrycrush.plugin.StepContext,
    private val sharedVariables: MutableMap<String, Any?>? = mutableMapOf(),
    private val sharingEnabled: Boolean = false,
) : StepContext {
    override fun variable(name: String): Any? {
        // First check scenario-scoped variables
        return stepContext[name]
            // Then check shared variables if sharing is enabled
            ?: if (sharingEnabled && sharedVariables != null) {
                sharedVariables[name]
            } else {
                null
            }
    }

    override fun setVariable(
        name: String,
        value: Any?,
    ) {
        if (value != null) {
            stepContext[name] = value
        }
        // Note: ExecutionContext doesn't support null values, so we just don't set
    }

    override fun setSharedVariable(
        name: String,
        value: Any?,
    ) {
        if (sharingEnabled && sharedVariables != null) {
            sharedVariables[name] = value
        } else {
            // Fall back to scenario-scoped if sharing not enabled
            setVariable(name, value)
        }
    }

    override fun allVariables(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result.putAll(stepContext.allVariables())
        if (sharingEnabled && sharedVariables != null) {
            // Shared variables are included but can be overridden by scenario variables
            sharedVariables.forEach { (key, value) ->
                if (!result.containsKey(key)) {
                    result[key] = value
                }
            }
        }
        return result
    }

    override val lastResponse: HttpResponse?
        get() = stepContext.response
}
