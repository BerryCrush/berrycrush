package io.github.ktakashi.lemoncheck.model

import io.github.ktakashi.lemoncheck.report.TestSummaryBuilder
import java.time.Instant

/**
 * Aggregated test report containing results from multiple scenario executions.
 *
 * @property title Report title
 * @property timestamp When the report was generated
 * @property totalScenarios Total number of scenarios executed
 * @property passed Number of passed scenarios
 * @property failed Number of failed scenarios
 * @property skipped Number of skipped scenarios
 * @property totalDurationMs Total execution time in milliseconds
 * @property scenarioResults Individual scenario results
 * @property environment Environment metadata
 */
data class TestReport(
    val title: String,
    val timestamp: Instant = Instant.now(),
    val totalScenarios: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val totalDurationMs: Long,
    val scenarioResults: List<ScenarioResult>,
    val environment: Map<String, String> = emptyMap(),
) {
    /**
     * Success rate as a percentage (0-100).
     */
    val successRate: Double
        get() = if (totalScenarios > 0) (passed.toDouble() / totalScenarios) * 100 else 0.0

    /**
     * Whether all scenarios passed.
     */
    val isSuccess: Boolean
        get() = failed == 0 && skipped == 0

    companion object {
        /**
         * Create a test report from a list of scenario results.
         */
        fun fromResults(
            title: String,
            results: List<ScenarioResult>,
            environment: Map<String, String> = emptyMap(),
        ): TestReport {
            val summary = TestSummaryBuilder.fromModelStatus(results) { it.status }
            val totalDurationMs = results.sumOf { it.duration.toMillis() }

            return TestReport(
                title = title,
                totalScenarios = results.size,
                passed = summary.passed,
                failed = summary.failed,
                skipped = summary.skipped,
                totalDurationMs = totalDurationMs,
                scenarioResults = results,
                environment = environment,
            )
        }
    }
}
