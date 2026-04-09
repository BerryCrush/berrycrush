package io.github.ktakashi.lemoncheck.report

import io.github.ktakashi.lemoncheck.plugin.ResultStatus
import java.nio.file.Path
import java.time.format.DateTimeFormatter

/**
 * Report plugin that generates human-readable text output.
 *
 * The text report includes:
 * - Summary header with execution date, duration, and totals
 * - Per-scenario details with status symbols
 * - Step-by-step results with failure details
 * - Final summary with failed scenario list
 */
class TextReportPlugin(
    outputPath: Path = Path.of("lemoncheck/report.txt"),
) : ReportPlugin(outputPath) {
    override val id: String = "report:text"
    override val name: String = "Text Report Plugin"

    companion object {
        private const val SEPARATOR = "================================================================================"
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT
    }

    override fun formatReport(report: TestReport): String =
        buildString {
            // Header
            appendLine(SEPARATOR)
            appendLine("Lemon Check Test Report")
            appendLine(SEPARATOR)
            appendLine("Execution Date: ${TIMESTAMP_FORMAT.format(report.timestamp)}")
            appendLine("Duration: ${formatDuration(report.duration.toMillis())}s")
            appendLine(
                "Scenarios: ${report.summary.total} total, " +
                    "${report.summary.passed} passed, " +
                    "${report.summary.failed} failed, " +
                    "${report.summary.skipped} skipped",
            )
            appendLine()

            // Scenarios
            for (scenario in report.scenarios) {
                val statusLabel = formatStatus(scenario.status)
                val durationStr = formatDuration(scenario.duration.toMillis())
                appendLine("[$statusLabel] ${scenario.name} (${durationStr}s)")

                for (step in scenario.steps) {
                    val symbol = statusSymbol(step.status)
                    appendLine("  $symbol ${step.description}")

                    // Show failure details
                    step.failure?.let { failure ->
                        appendLine("    Step: ${failure.stepDescription}")
                        appendLine("    Expected: ${failure.expected}")
                        appendLine("    Actual: ${failure.actual}")

                        failure.diff?.let { diff ->
                            appendLine("    Diff:")
                            diff.lines().forEach { line ->
                                appendLine("      $line")
                            }
                        }

                        failure.requestSnapshot?.let { request ->
                            appendLine("    Request:")
                            appendLine("      ${request.method} ${request.url}")
                            request.body?.let { body ->
                                appendLine("      Body: ${truncate(body, 200)}")
                            }
                        }

                        failure.responseSnapshot?.let { response ->
                            appendLine("    Response:")
                            appendLine("      ${response.statusCode} ${response.statusMessage}")
                            response.body?.let { body ->
                                appendLine("      Body: ${truncate(body, 200)}")
                            }
                        }
                    }
                }
                appendLine()
            }

            // Summary
            appendLine(SEPARATOR)
            val percentage =
                if (report.summary.total > 0) {
                    (report.summary.passed * 100.0 / report.summary.total)
                } else {
                    0.0
                }
            appendLine("Summary: ${report.summary.passed}/${report.summary.total} scenarios passed (${String.format("%.1f", percentage)}%)")

            val failedScenarios = report.scenarios.filter { it.status == ResultStatus.FAILED }
            if (failedScenarios.isNotEmpty()) {
                appendLine("Failed Scenarios:")
                for (scenario in failedScenarios) {
                    appendLine("  - ${scenario.name}")
                }
            }
            appendLine(SEPARATOR)
        }

    private fun formatStatus(status: ResultStatus): String =
        when (status) {
            ResultStatus.PASSED -> "PASS"
            ResultStatus.FAILED -> "FAIL"
            ResultStatus.SKIPPED -> "SKIP"
            ResultStatus.ERROR -> "ERR "
        }

    private fun statusSymbol(status: ResultStatus): String =
        when (status) {
            ResultStatus.PASSED -> "✓"
            ResultStatus.FAILED -> "✗"
            ResultStatus.SKIPPED -> "⊘"
            ResultStatus.ERROR -> "⚠"
        }

    private fun formatDuration(millis: Long): String = String.format("%.3f", millis / 1000.0)

    private fun truncate(
        text: String,
        maxLength: Int,
    ): String =
        if (text.length <= maxLength) {
            text
        } else {
            "${text.take(maxLength)}..."
        }
}
