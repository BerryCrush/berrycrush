package org.berrycrush.report

import org.berrycrush.plugin.ResultStatus
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
    outputPath: Path = Path.of("berrycrush/junit.xml"),
    /**
     * Test suite name to use in the report.
     */
    private val suiteName: String = "berrycrush",
) : ReportPlugin(outputPath) {
    override val id: String = "report:junit"
    override val name: String = "JUnit Report Plugin"

    companion object {
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT
        private const val MILLIS_PER_SECOND = 1000.0
    }

    override fun formatReport(report: TestReport): String =
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendTestsuitesElement(report)
            report.scenarios.groupBy { it.sourceFile ?: "unknown" }.forEach { (fileName, scenarios) ->
                appendTestsuite(fileName, scenarios)
            }
            appendLine("</testsuites>")
        }

    private fun StringBuilder.appendTestsuitesElement(report: TestReport) {
        appendLine(
            """<testsuites name="$suiteName" """ +
                """tests="${report.summary.total}" """ +
                """failures="${report.summary.failed}" """ +
                """errors="${report.summary.errors}" """ +
                """skipped="${report.summary.skipped}" """ +
                """time="${formatSeconds(report.duration.toMillis())}" """ +
                """timestamp="${TIMESTAMP_FORMAT.format(report.timestamp)}">""",
        )
    }

    private fun StringBuilder.appendTestsuite(
        fileName: String,
        scenarios: List<ScenarioReportEntry>,
    ) {
        val stats =
            TestsuiteStats(
                total = scenarios.size,
                failures = scenarios.count { it.status == ResultStatus.FAILED },
                errors = scenarios.count { it.status == ResultStatus.ERROR },
                skipped = scenarios.count { it.status == ResultStatus.SKIPPED },
                durationMillis = scenarios.sumOf { it.duration.toMillis() },
            )

        appendLine(
            """  <testsuite name="${escapeXml(fileName)}" """ +
                """tests="${stats.total}" """ +
                """failures="${stats.failures}" """ +
                """errors="${stats.errors}" """ +
                """skipped="${stats.skipped}" """ +
                """time="${formatSeconds(stats.durationMillis)}">""",
        )

        scenarios.forEach { appendTestcase(fileName, it) }
        appendLine("  </testsuite>")
    }

    private data class TestsuiteStats(
        val total: Int,
        val failures: Int,
        val errors: Int,
        val skipped: Int,
        val durationMillis: Long,
    )

    private fun StringBuilder.appendTestcase(
        className: String,
        scenario: ScenarioReportEntry,
    ) {
        val testName = escapeXml(scenario.name)
        val time = formatSeconds(scenario.duration.toMillis())

        when (scenario.status) {
            ResultStatus.PASSED ->
                appendLine(
                    """    <testcase name="$testName" classname="$className" time="$time"/>""",
                )
            ResultStatus.FAILED -> appendFailedTestcase(className, testName, time, scenario)
            ResultStatus.ERROR -> appendErrorTestcase(className, testName, time, scenario)
            ResultStatus.SKIPPED -> {
                appendLine("""    <testcase name="$testName" classname="$className" time="$time">""")
                appendLine("      <skipped/>")
                appendLine("    </testcase>")
            }
        }
    }

    private fun StringBuilder.appendFailedTestcase(
        className: String,
        testName: String,
        time: String,
        scenario: ScenarioReportEntry,
    ) {
        appendLine("""    <testcase name="$testName" classname="$className" time="$time">""")
        val failedStep = scenario.steps.firstOrNull { it.status == ResultStatus.FAILED }
        failedStep?.failure?.let { failure ->
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
        } ?: appendLine("""      <failure message="Scenario failed" type="AssertionError"/>""")
        appendLine("    </testcase>")
    }

    private fun StringBuilder.appendErrorTestcase(
        className: String,
        testName: String,
        time: String,
        scenario: ScenarioReportEntry,
    ) {
        appendLine("""    <testcase name="$testName" classname="$className" time="$time">""")
        val errorMsg =
            scenario.steps
                .firstOrNull { it.status == ResultStatus.ERROR }
                ?.failure
                ?.message ?: "Unexpected error occurred"
        appendLine("""      <error message="${escapeXml(errorMsg)}" type="Error"/>""")
        appendLine("    </testcase>")
    }

    private fun formatSeconds(millis: Long): String = String.format(java.util.Locale.US, "%.3f", millis / MILLIS_PER_SECOND)

    private fun escapeXml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
