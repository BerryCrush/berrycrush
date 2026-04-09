package io.github.ktakashi.lemoncheck.report

import io.github.ktakashi.lemoncheck.plugin.ResultStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.test.assertTrue

/**
 * Tests for TextReportPlugin.
 */
class TextReportPluginTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generates valid text report with passed scenario`() {
        val outputPath = tempDir.resolve("report.txt")
        val plugin = TextReportPlugin(outputPath)

        val report =
            createTestReport(
                ScenarioReportEntry(
                    name = "Test Scenario",
                    status = ResultStatus.PASSED,
                    duration = Duration.ofMillis(123),
                    steps =
                        listOf(
                            StepReportEntry(
                                description = "when I call the API",
                                status = ResultStatus.PASSED,
                                duration = Duration.ofMillis(50),
                            ),
                            StepReportEntry(
                                description = "then I get a response",
                                status = ResultStatus.PASSED,
                                duration = Duration.ofMillis(73),
                            ),
                        ),
                ),
            )

        val content = plugin.formatReport(report)

        assertTrue(content.contains("Lemon Check Test Report"))
        assertTrue(content.contains("Test Scenario"))
        assertTrue(content.contains("[PASS]"))
        assertTrue(content.contains("✓"))
        assertTrue(content.contains("1 total, 1 passed, 0 failed"))
    }

    @Test
    fun `generates report with failure details`() {
        val outputPath = tempDir.resolve("report.txt")
        val plugin = TextReportPlugin(outputPath)

        val report =
            createTestReport(
                ScenarioReportEntry(
                    name = "Failing Scenario",
                    status = ResultStatus.FAILED,
                    duration = Duration.ofMillis(500),
                    steps =
                        listOf(
                            StepReportEntry(
                                description = "then status should be 201",
                                status = ResultStatus.FAILED,
                                duration = Duration.ofMillis(100),
                                failure =
                                    io.github.ktakashi.lemoncheck.plugin.AssertionFailure(
                                        message = "Status code mismatch",
                                        expected = 201,
                                        actual = 400,
                                        diff = null,
                                        stepDescription = "assert status 201",
                                        assertionType = "STATUS_CODE",
                                    ),
                            ),
                        ),
                ),
            )

        val content = plugin.formatReport(report)

        assertTrue(content.contains("[FAIL]"))
        assertTrue(content.contains("✗"))
        assertTrue(content.contains("Expected: 201"))
        assertTrue(content.contains("Actual: 400"))
        assertTrue(content.contains("Failed Scenarios:"))
        assertTrue(content.contains("- Failing Scenario"))
    }

    private fun createTestReport(vararg scenarios: ScenarioReportEntry): TestReport {
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
}
