package org.berrycrush.executor.response

import org.berrycrush.context.ValueExtractor
import org.berrycrush.executor.assertion.AssertionExecutor
import org.berrycrush.model.HttpResponse
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Step
import org.berrycrush.model.StepResult
import org.berrycrush.plugin.StepContext
import java.time.Duration

/**
 * Processor for HTTP responses during scenario execution.
 *
 * Handles assertion evaluation, variable extraction, and result
 * building after HTTP requests complete.
 */
class ResponseProcessor(
    private val assertionExecutor: AssertionExecutor,
) {
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
     * @param context The execution context for state management
     * @return The complete step result
     */
    fun process(
        step: Step,
        response: HttpResponse,
        context: StepContext,
    ): StepResult {
        val isCustom = assertionExecutor.hasCustomAssertion(step)

        // Check for unconditional fail
        if (step.failMessage != null) {
            return StepResult(
                step = step,
                status = ResultStatus.FAILED,
                response = response,
                duration = context.response?.duration ?: Duration.ZERO,
                error = AssertionError(step.failMessage),
                isCustomStep = isCustom,
            )
        }

        val extractedValues = extractValues(step, context)
        val evaluatedAssertions = assertionExecutor.runAssertions(response, step.assertions, context)
        val assertionResults = evaluatedAssertions.assertionResults

        // Check for conditional fail
        if (evaluatedAssertions.failMessage != null) {
            return StepResult(
                step = step,
                status = ResultStatus.FAILED,
                response = response,
                duration = context.response?.duration ?: Duration.ZERO,
                extractedValues = extractedValues + evaluatedAssertions.extractedValues,
                assertionResults = assertionResults,
                error = AssertionError(evaluatedAssertions.failMessage),
                isCustomStep = isCustom,
            )
        }
        val allPassed = assertionResults.all { it.passed }

        return StepResult(
            step = step,
            status = if (allPassed) ResultStatus.PASSED else ResultStatus.FAILED,
            response = response,
            duration = context.response?.duration ?: Duration.ZERO,
            extractedValues = extractedValues + evaluatedAssertions.extractedValues,
            assertionResults = assertionResults,
            isCustomStep = isCustom,
        )
    }

    private fun extractValues(
        step: Step,
        context: StepContext,
    ): Map<String, Any?> =
        step.extractions.associate { extraction ->
            val value =
                runCatching {
                    val body = context.response?.body ?: ""
                    ValueExtractor.extract(body, extraction)
                }.getOrNull()
            value?.let { context[extraction.variableName] = it }
            extraction.variableName to value
        }
}
