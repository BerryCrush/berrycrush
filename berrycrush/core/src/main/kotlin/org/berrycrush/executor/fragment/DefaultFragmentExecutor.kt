package org.berrycrush.executor.fragment

import org.berrycrush.context.ExecutionContext
import org.berrycrush.exception.ConfigurationException
import org.berrycrush.executor.resolvers.resolveParams
import org.berrycrush.model.FragmentRegistry
import org.berrycrush.model.Step

/**
 * Default implementation of [FragmentExecutor] for expanding fragment references.
 *
 * This implementation handles:
 * - Looking up fragments in the registry
 * - Injecting include parameters into execution context
 * - Variable interpolation in parameter values
 *
 * @property fragmentRegistry Registry for looking up fragments by name
 */
class DefaultFragmentExecutor(
    private val fragmentRegistry: FragmentRegistry?,
) : FragmentExecutor {
    /**
     * Expand a step by resolving any fragment references.
     *
     * If the step references a fragment (via fragmentName), returns the steps
     * from that fragment. Otherwise, returns a list containing just the original step.
     *
     * @param step The step to expand
     * @return List of steps to execute (fragment steps or original step)
     */
    override fun expand(step: Step): List<Step> {
        val fragmentName = step.fragmentName ?: return listOf(step)

        // Look up the fragment in the registry
        val fragment =
            fragmentRegistry?.get(fragmentName)
                ?: throw ConfigurationException(
                    "Fragment '$fragmentName' not found. " +
                        "Register it with fragmentRegistry.register() or load from a .fragment file.",
                )

        return fragment.steps
    }

    /**
     * Inject include parameters into the execution context.
     *
     * When a step has include parameters, they become available as variables
     * for interpolation in the included fragment's steps.
     *
     * @param step The include step with parameters
     * @param context The execution context to inject parameters into
     */
    override fun injectParameters(
        step: Step,
        context: ExecutionContext,
    ) {
        if (step.includeParameters.isEmpty()) return

        context.resolveParams(step.includeParameters).forEach { (key, value) -> context[key] = value }
    }
}
