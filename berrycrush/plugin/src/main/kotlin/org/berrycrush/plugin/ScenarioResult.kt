package org.berrycrush.plugin

import java.time.Duration

/**
 * Outcome of scenario execution.
 *
 * Contains the final status, execution timing, and failure/error details for an entire scenario.
 * Includes results for all executed steps.
 *
 * @property status Overall result status (PASSED, FAILED, SKIPPED, ERROR)
 * @property duration Total scenario execution time
 * @property failedStep Zero-based index of the first failed step (-1 if no failures)
 * @property error Exception that occurred if status is ERROR (null otherwise)
 * @property stepResults Results for all executed steps in order
 */
interface ScenarioResult {
    val status: ResultStatus
    val duration: Duration
    val failedStep: Int
    val error: Throwable?
    val stepResults: List<StepResult>
}
