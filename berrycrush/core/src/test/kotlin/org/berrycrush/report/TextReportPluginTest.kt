package org.berrycrush.report

import org.berrycrush.plugin.ResultStatus
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

        assertTrue(content.contains("BerryCrush Test Report"))
        assertTrue(content.contains("scenario: Test Scenario"))
        assertTrue(content.contains("✓")) // Scenario status icon
        assertTrue(content.contains("pass")) // Step status
        assertTrue(content.contains("1/1 scenarios passed"))
    }

    @Test
    fun `generates report with failure details`() {
        val outputPath = tempDir.resolve("report.txt")
        val plugin = TextReportPlugin(outputPath)

        val report = createTestReport(createFailingScenarioEntry())

        val content = plugin.formatReport(report)

        assertTrue(content.contains("✗")) // Failed scenario icon
        assertTrue(content.contains("FAIL")) // Failed step status
        assertTrue(content.contains("expected: 201"))
        assertTrue(content.contains("actual: 400"))
        assertTrue(content.contains("Failed Scenarios:"))
        assertTrue(content.contains("- Failing Scenario"))
    }

    @Test
    fun `report shows HTTP response status`() {
        val outputPath = tempDir.resolve("report.txt")
        val plugin = TextReportPlugin(outputPath)

        val report =
            createTestReport(
                ScenarioReportEntry(
                    name = "API Call Scenario",
                    status = ResultStatus.PASSED,
                    duration = Duration.ofMillis(200),
                    steps =
                        listOf(
                            StepReportEntry(
                                description = "call ^getPetById",
                                status = ResultStatus.PASSED,
                                duration = Duration.ofMillis(150),
                                response =
                                    org.berrycrush.plugin.HttpResponse(
                                        statusCode = 200,
                                        statusMessage = "OK",
                                        headers = emptyMap(),
                                        body = """{"id": 1, "name": "Max"}""",
                                        duration = Duration.ofMillis(100),
                                        timestamp = Instant.now(),
                                    ),
                            ),
                        ),
                ),
            )

        val content = plugin.formatReport(report)

        assertTrue(content.contains("200 OK")) // HTTP response status
    }
}
