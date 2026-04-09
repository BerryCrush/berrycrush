package io.github.ktakashi.lemoncheck.report

import io.github.ktakashi.lemoncheck.plugin.LemonCheckPlugin
import io.github.ktakashi.lemoncheck.plugin.ResultStatus
import io.github.ktakashi.lemoncheck.plugin.ScenarioContext
import io.github.ktakashi.lemoncheck.plugin.ScenarioResult
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * Base class for report plugins that generate output at the end of test execution.
 *
 * Report plugins collect scenario results during execution and write a final report
 * when all scenarios have completed and generateReport() is called.
 */
abstract class ReportPlugin(
    /**
     * Output path for the report file.
     */
    val outputPath: Path,
) : LemonCheckPlugin {
    protected val scenarioReports = mutableListOf<ScenarioReportEntry>()
    protected var startTime: Instant? = null
    protected var endTime: Instant? = null

    override val priority: Int = 100 // Run after other plugins

    override fun onScenarioStart(context: ScenarioContext) {
        if (startTime == null) {
            startTime = context.startTime
        }
    }

    override fun onScenarioEnd(
        context: ScenarioContext,
        result: ScenarioResult,
    ) {
        endTime = Instant.now()
        val entry =
            ScenarioReportEntry(
                name = context.scenarioName,
                status = mapStatus(result.status),
                duration = result.duration,
                steps =
                    result.stepResults.map { stepResult ->
                        StepReportEntry(
                            description = "Step ${result.stepResults.indexOf(stepResult) + 1}",
                            status = mapStatus(stepResult.status),
                            duration = stepResult.duration,
                            request = null, // HTTP snapshots can be added later
                            response = null,
                            failure = stepResult.failure,
                        )
                    },
                tags = context.tags,
                metadata = context.metadata,
            )
        scenarioReports.add(entry)
    }

    /**
     * Called at the end of test execution to generate the report.
     *
     * This hook is automatically invoked after all scenarios have completed,
     * triggering report generation and file output.
     */
    override fun onTestExecutionEnd() {
        generateReport()
    }

    /**
     * Generate the final report and write to output path.
     *
     * Can also be called manually if needed, but typically
     * invoked automatically via onTestExecutionEnd().
     */
    fun generateReport() {
        val report = buildReport()
        val content = formatReport(report)
        writeReport(content)
    }

    /**
     * Build the TestReport from collected scenario data.
     */
    protected fun buildReport(): TestReport {
        val passed = scenarioReports.count { it.status == ResultStatus.PASSED }
        val failed = scenarioReports.count { it.status == ResultStatus.FAILED }
        val skipped = scenarioReports.count { it.status == ResultStatus.SKIPPED }
        val errors = scenarioReports.count { it.status == ResultStatus.ERROR }

        val totalDuration =
            scenarioReports.fold(Duration.ZERO) { acc, entry ->
                acc.plus(entry.duration)
            }

        return TestReport(
            timestamp = startTime ?: Instant.now(),
            duration = totalDuration,
            summary =
                TestSummary(
                    total = scenarioReports.size,
                    passed = passed,
                    failed = failed,
                    skipped = skipped,
                    errors = errors,
                ),
            scenarios = scenarioReports.toList(),
            environment = collectEnvironment(),
        )
    }

    /**
     * Format the report to a string representation.
     *
     * Subclasses must implement this to produce the output format.
     */
    abstract fun formatReport(report: TestReport): String

    /**
     * Write the report content to the output file.
     */
    protected open fun writeReport(content: String) {
        Files.createDirectories(outputPath.parent)
        Files.writeString(outputPath, content)
    }

    /**
     * Collect environment information for the report.
     */
    protected fun collectEnvironment(): Map<String, String> =
        mapOf(
            "java.version" to System.getProperty("java.version", "unknown"),
            "os.name" to System.getProperty("os.name", "unknown"),
            "os.version" to System.getProperty("os.version", "unknown"),
        )

    protected fun mapStatus(status: ResultStatus): ResultStatus =
        when (status) {
            ResultStatus.PASSED -> ResultStatus.PASSED
            ResultStatus.FAILED -> ResultStatus.FAILED
            ResultStatus.SKIPPED -> ResultStatus.SKIPPED
            ResultStatus.ERROR -> ResultStatus.ERROR
        }
}
