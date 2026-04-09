package io.github.ktakashi.lemoncheck.report

import io.github.ktakashi.lemoncheck.plugin.ResultStatus
import java.time.Duration

/**
 * Report entry for a single scenario with full execution details.
 *
 * Contains scenario-level information including name, status, duration,
 * all step results, tags, and metadata.
 *
 * @property name Scenario name from scenario file
 * @property status Overall scenario status (PASSED, FAILED, SKIPPED, ERROR)
 * @property duration Total scenario execution time
 * @property steps Execution details for all steps in the scenario
 * @property tags Scenario tags for filtering and organization
 * @property metadata Additional scenario metadata
 */
data class ScenarioReportEntry(
    val name: String,
    val status: ResultStatus,
    val duration: Duration,
    val steps: List<StepReportEntry>,
    val tags: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
)
