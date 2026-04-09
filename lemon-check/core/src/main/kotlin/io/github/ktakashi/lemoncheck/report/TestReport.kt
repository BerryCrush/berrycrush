package io.github.ktakashi.lemoncheck.report

import java.time.Duration
import java.time.Instant

/**
 * Complete test execution report containing all scenarios and their results.
 *
 * Top-level report structure aggregating all scenario results, execution timing,
 * summary statistics, and environment metadata.
 *
 * @property timestamp When the test run started
 * @property duration Total test execution time across all scenarios
 * @property summary Aggregate statistics (total, passed, failed, etc.)
 * @property scenarios All scenario results
 * @property environment Test environment metadata (e.g., Java version, OS, etc.)
 */
data class TestReport(
    val timestamp: Instant,
    val duration: Duration,
    val summary: TestSummary,
    val scenarios: List<ScenarioReportEntry>,
    val environment: Map<String, String> = emptyMap(),
)

/**
 * Aggregate test execution statistics.
 *
 * @property total Total number of scenarios executed
 * @property passed Number of scenarios that passed
 * @property failed Number of scenarios that failed
 * @property skipped Number of scenarios that were skipped
 * @property errors Number of scenarios that encountered errors
 */
data class TestSummary(
    val total: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val errors: Int,
) {
    init {
        require(total == passed + failed + skipped + errors) {
            "Summary totals don't match: total=$total, but passed+failed+skipped+errors=${passed + failed + skipped + errors}"
        }
    }
}
