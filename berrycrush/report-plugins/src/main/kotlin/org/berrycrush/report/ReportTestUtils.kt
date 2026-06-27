@file:Suppress("MagicNumber")

package org.berrycrush.report

import org.berrycrush.plugin.ResultStatus
import java.time.Duration
import java.time.Instant

/**
 * Create a test report from the given scenarios.
 *
 * Automatically calculates the summary based on scenario statuses.
 */
internal fun createTestReport(vararg scenarios: ScenarioReportEntry): TestReport {
    val passed = scenarios.count { it.status == ResultStatus.PASSED }
    val failed = scenarios.count { it.status == ResultStatus.FAILED }
    val skipped = scenarios.count { it.status == ResultStatus.SKIPPED }
    val errors = scenarios.count { it.status == ResultStatus.ERROR }

    return TestReport(
        timestamp = Instant.parse("2026-04-09T10:15:30Z"),
        duration = scenarios.fold(Duration.ZERO) { acc, s -> acc.plus(s.duration) },
        summary =
            TestSummary(
                total = scenarios.size,
                passed = passed,
                failed = failed,
                skipped = skipped,
                errors = errors,
            ),
        scenarios = scenarios.toList(),
    )
}

/**
 * Create a failing scenario report entry with a single step.
 *
 * This is a common test helper for testing failure rendering.
 *
 * @param name The scenario name
 * @param stepDescription The failing step description
 * @param failureMessage The assertion failure message
 * @param expected Expected value
 * @param actual Actual value
 * @param sourceFile Optional source file name
 */
@Suppress("LongParameterList")
internal fun createFailingScenarioEntry(
    name: String = "Failing Scenario",
    stepDescription: String = "then status should be 201",
    failureMessage: String = "Status code mismatch",
    expected: Any? = 201,
    actual: Any? = 400,
    sourceFile: String? = null,
): ScenarioReportEntry =
    ScenarioReportEntry(
        name = name,
        status = ResultStatus.FAILED,
        duration = Duration.ofMillis(500),
        steps =
            listOf(
                StepReportEntry(
                    description = stepDescription,
                    status = ResultStatus.FAILED,
                    duration = Duration.ofMillis(100),
                    failure =
                        org.berrycrush.plugin.AssertionFailure(
                            message = failureMessage,
                            expected = expected,
                            actual = actual,
                            diff = null,
                            stepDescription = "assert status 201",
                            assertionType = "STATUS_CODE",
                        ),
                ),
            ),
        sourceFile = sourceFile,
    )
