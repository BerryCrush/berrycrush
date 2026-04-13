package org.berrycrush.plugin

import java.time.Duration

/**
 * Outcome of step execution.
 *
 * Contains the final status, execution timing, and failure/error details for a single step.
 *
 * @property status Result status (PASSED, FAILED, SKIPPED, ERROR)
 * @property duration Total step execution time
 * @property failure Detailed failure information if status is FAILED (null otherwise)
 * @property error Exception that occurred if status is ERROR (null otherwise)
 */
interface StepResult {
    val status: ResultStatus
    val duration: Duration
    val failure: AssertionFailure?
    val error: Throwable?
}
