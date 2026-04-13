package org.berrycrush.model

import java.time.Duration
import java.time.Instant

/**
 * Result of executing a complete scenario.
 *
 * @property scenario The scenario that was executed
 * @property status Overall result status
 * @property stepResults Results of each step
 * @property startTime When execution started
 * @property duration Total execution time
 * @property parameterValues For parameterized scenarios, the values used
 */
data class ScenarioResult(
    val scenario: Scenario,
    val status: ResultStatus,
    val stepResults: List<StepResult> = emptyList(),
    val startTime: Instant = Instant.now(),
    val duration: Duration = Duration.ZERO,
    val parameterValues: Map<String, Any?> = emptyMap(),
) {
    /**
     * Number of passed steps.
     */
    val passedCount: Int
        get() = stepResults.count { it.status == ResultStatus.PASSED }

    /**
     * Number of failed steps.
     */
    val failedCount: Int
        get() = stepResults.count { it.status == ResultStatus.FAILED }

    /**
     * Number of skipped steps.
     */
    val skippedCount: Int
        get() = stepResults.count { it.status == ResultStatus.SKIPPED }

    /**
     * Whether all steps passed.
     */
    val isSuccess: Boolean
        get() = status == ResultStatus.PASSED
}
