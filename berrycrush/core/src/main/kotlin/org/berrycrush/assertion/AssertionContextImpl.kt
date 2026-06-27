package org.berrycrush.assertion

import org.berrycrush.model.HttpResponse
import org.berrycrush.plugin.StepContext

/**
 * Default implementation of [AssertionContext] that wraps an [org.berrycrush.plugin.StepContext].
 *
 * Provides read-only access to test variables, last HTTP response, and configuration
 * for custom assertion implementations.
 *
 * @property stepContext The underlying execution context
 */
class AssertionContextImpl(
    private val stepContext: StepContext,
) : AssertionContext {
    override fun variable(name: String): Any? = stepContext[name]

    override fun allVariables(): Map<String, Any?> = stepContext.allVariables()

    override val lastResponse: HttpResponse?
        get() = stepContext.response
}
