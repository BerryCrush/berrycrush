package org.berrycrush.executor.response

import org.berrycrush.context.ExecutionContext
import org.berrycrush.model.Step
import org.berrycrush.model.StepResult
import java.net.http.HttpResponse
import java.time.Instant

/**
 * Processor for HTTP responses during scenario execution.
 *
 * Handles assertion evaluation, variable extraction, and result
 * building after HTTP requests complete.
 */
interface ResponseProcessor {
    /**
     * Process an HTTP response for a step.
     *
     * This method:
     * 1. Evaluates all assertions defined in the step
     * 2. Runs any conditional blocks
     * 3. Extracts variables from the response
     * 4. Builds the final step result
     *
     * @param response The HTTP response to process
     * @param step The step containing assertions and extractions
     * @param stepStartTime The time when the step started executing
     * @param context The execution context for state management
     * @return The complete step result
     */
    fun process(
        response: HttpResponse<String>,
        step: Step,
        stepStartTime: Instant,
        context: ExecutionContext,
    ): StepResult
}
