package org.berrycrush.executor.assertion

import org.berrycrush.model.Condition

/**
 * Result of condition evaluation by the AssertionEngine.
 */
data class ConditionResult(
    val passed: Boolean,
    val message: String,
    val actual: Any? = null,
)

/**
 * Engine for evaluating assertions and conditions against HTTP responses.
 *
 * This interface abstracts the assertion evaluation logic from the main
 * executor, allowing for easier testing and potential alternative implementations.
 */
interface AssertionEngine {
    /**
     * Evaluate a condition against the current assertion context.
     *
     * @param condition The condition to evaluate (status, jsonpath, header, etc.)
     * @param context The assertion context containing response data
     * @return The result of the condition evaluation
     */
    fun evaluate(
        condition: Condition,
        context: AssertionContext,
    ): ConditionResult

    /**
     * Generate a human-readable message for a condition result.
     *
     * @param condition The condition that was evaluated
     * @param passed Whether the condition passed
     * @param context The assertion context for additional details
     * @return A descriptive message explaining the condition outcome
     */
    fun generateMessage(
        condition: Condition,
        passed: Boolean,
        context: AssertionContext,
    ): String
}
