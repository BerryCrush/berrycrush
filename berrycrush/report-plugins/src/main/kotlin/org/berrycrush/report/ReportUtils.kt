package org.berrycrush.report

import org.berrycrush.plugin.ResultStatus
import org.berrycrush.plugin.StepResult

/**
 * Build a [StepReportEntry] from a [org.berrycrush.plugin.StepResult].
 *
 * This shared function encapsulates the common logic for creating step report entries
 * across different report plugins.
 *
 * @param stepResult The step result to convert
 * @param statusMapper Optional function to map status values (defaults to identity)
 * @return A fully populated [StepReportEntry]
 */
internal fun buildStepReportEntry(
    stepResult: StepResult,
    statusMapper: (ResultStatus) -> ResultStatus = { it },
): StepReportEntry =
    StepReportEntry(
        description = stepResult.stepDescription,
        status = statusMapper(stepResult.status),
        duration = stepResult.duration,
        request = stepResult.response?.request,
        response = stepResult.response,
        failure = stepResult.failure,
        isCustomStep = stepResult.isCustomStep,
    )
