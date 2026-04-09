package io.github.ktakashi.lemoncheck.report

import io.github.ktakashi.lemoncheck.plugin.ResultStatus
import java.nio.file.Path
import java.time.format.DateTimeFormatter

/**
 * Report plugin that generates JUnit-compatible XML for CI/CD integration.
 *
 * The JUnit XML format is widely supported by CI tools including:
 * - Jenkins
 * - GitHub Actions
 * - GitLab CI
 * - CircleCI
 * - Azure DevOps
 *
 * Format follows the standard JUnit XML schema.
 */
class JunitReportPlugin(
    outputPath: Path = Path.of("lemoncheck/junit.xml"),
    /**
     * Test suite name to use in the report.
     */
    private val suiteName: String = "lemon-check",
) : ReportPlugin(outputPath) {
    override val id: String = "report:junit"
    override val name: String = "JUnit Report Plugin"

    companion object {
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT
    }

    override fun formatReport(report: TestReport): String =
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")

            // Root testsuites element
            appendLine(
                """<testsuites name="$suiteName" """ +
                    """tests="${report.summary.total}" """ +
                    """failures="${report.summary.failed}" """ +
                    """errors="${report.summary.errors}" """ +
                    """skipped="${report.summary.skipped}" """ +
                    """time="${formatSeconds(report.duration.toMillis())}" """ +
                    """timestamp="${TIMESTAMP_FORMAT.format(report.timestamp)}">""",
            )

            // Each scenario as a testsuite
            for (scenario in report.scenarios) {
                val stepTotal = scenario.steps.size
                val stepFailures = scenario.steps.count { it.status == ResultStatus.FAILED }
                val stepErrors = scenario.steps.count { it.status == ResultStatus.ERROR }
                val stepSkipped = scenario.steps.count { it.status == ResultStatus.SKIPPED }

                appendLine(
                    """  <testsuite name="${escapeXml(scenario.name)}" """ +
                        """tests="$stepTotal" """ +
                        """failures="$stepFailures" """ +
                        """errors="$stepErrors" """ +
                        """skipped="$stepSkipped" """ +
                        """time="${formatSeconds(scenario.duration.toMillis())}">""",
                )

                // Each step as a testcase
                for (step in scenario.steps) {
                    val className = suiteName
                    val testName = escapeXml(step.description)
                    val time = formatSeconds(step.duration.toMillis())

                    when (step.status) {
                        ResultStatus.PASSED -> {
                            appendLine("""    <testcase name="$testName" classname="$className" time="$time"/>""")
                        }
                        ResultStatus.FAILED -> {
                            appendLine("""    <testcase name="$testName" classname="$className" time="$time">""")
                            step.failure?.let { failure ->
                                appendLine(
                                    """      <failure message="${escapeXml(failure.message)}" """ +
                                        """type="${escapeXml(failure.assertionType)}">""",
                                )
                                appendLine("""Expected: ${escapeXml(failure.expected?.toString() ?: "null")}""")
                                appendLine("""Actual: ${escapeXml(failure.actual?.toString() ?: "null")}""")
                                failure.diff?.let { diff ->
                                    appendLine("Diff:")
                                    appendLine(escapeXml(diff))
                                }
                                appendLine("      </failure>")
                            }
                            appendLine("    </testcase>")
                        }
                        ResultStatus.ERROR -> {
                            appendLine("""    <testcase name="$testName" classname="$className" time="$time">""")
                            appendLine("""      <error message="Unexpected error occurred" type="Error"/>""")
                            appendLine("    </testcase>")
                        }
                        ResultStatus.SKIPPED -> {
                            appendLine("""    <testcase name="$testName" classname="$className" time="$time">""")
                            appendLine("      <skipped/>")
                            appendLine("    </testcase>")
                        }
                    }
                }

                appendLine("  </testsuite>")
            }

            appendLine("</testsuites>")
        }

    private fun formatSeconds(millis: Long): String = String.format("%.3f", millis / 1000.0)

    private fun escapeXml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
