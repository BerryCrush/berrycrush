package org.berrycrush.executor.fragment

import org.berrycrush.context.ExecutionContext
import org.berrycrush.model.Step

/**
 * Executor for fragment expansion during scenario execution.
 *
 * Fragments are reusable step groups that can be included in scenarios
 * using the `include` directive.
 */
interface FragmentExecutor {
    /**
     * Expand a step that may include a fragment reference.
     *
     * If the step contains a `fragmentName` reference, this method resolves
     * the fragment and returns its expanded steps.
     * Otherwise, returns the original step unchanged.
     *
     * @param step The step to potentially expand
     * @return A list of steps after expansion (single item if no fragment)
     */
    fun expand(step: Step): List<Step>

    /**
     * Inject include parameters into the execution context.
     *
     * When a step has include parameters, they become available as variables
     * for interpolation in the included fragment's steps.
     *
     * @param step The include step with parameters
     * @param context The execution context to inject parameters into
     */
    fun injectParameters(
        step: Step,
        context: ExecutionContext,
    )
}
