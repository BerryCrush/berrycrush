package org.berrycrush.executor.fragment

import org.berrycrush.context.ExecutionContext
import org.berrycrush.model.Step

/**
 * Executor for fragment expansion during scenario execution.
 *
 * Fragments are reusable step groups that can be included in scenarios
 * using the `include` directive.
 */
fun interface FragmentExecutor {
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
}
