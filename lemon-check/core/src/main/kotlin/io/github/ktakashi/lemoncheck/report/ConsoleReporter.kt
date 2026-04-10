package io.github.ktakashi.lemoncheck.report

import io.github.ktakashi.lemoncheck.model.ResultStatus
import io.github.ktakashi.lemoncheck.model.ScenarioResult
import io.github.ktakashi.lemoncheck.model.StepResult
import java.io.PrintStream

/**
 * Console-based test reporter that outputs results to stdout.
 */
class ConsoleReporter(
    private val out: PrintStream = System.out,
    private val verbose: Boolean = false,
) : TestReporter {
    private val green = "\u001B[32m"
    private val red = "\u001B[31m"
    private val yellow = "\u001B[33m"
    private val cyan = "\u001B[36m"
    private val reset = "\u001B[0m"

    override fun onScenarioStart(scenarioName: String) {
        out.println()
        out.println("${cyan}Scenario:$reset $scenarioName")
    }

    override fun onStepComplete(stepResult: StepResult) {
        val icon =
            when (stepResult.status) {
                ResultStatus.PASSED -> "$green✓$reset"
                ResultStatus.FAILED -> "$red✗$reset"
                ResultStatus.SKIPPED -> "$yellow○$reset"
                ResultStatus.ERROR -> "$red!$reset"
                ResultStatus.PENDING -> "$yellow?$reset"
            }

        val stepType =
            stepResult.step.type.name
                .lowercase()
                .replaceFirstChar { it.uppercase() }
        out.println("  $icon $stepType ${stepResult.step.description}")

        if (stepResult.status == ResultStatus.FAILED) {
            stepResult.assertionResults
                .filter { !it.passed }
                .forEach { result ->
                    out.println("      $red→ ${result.message}$reset")
                }
        }

        if (stepResult.status == ResultStatus.ERROR && stepResult.error != null) {
            out.println("      $red→ Error: ${stepResult.error.message}$reset")
        }

        if (verbose && stepResult.extractedValues.isNotEmpty()) {
            stepResult.extractedValues.forEach { (name, value) ->
                out.println("      ${cyan}extracted: $name = $value$reset")
            }
        }
    }

    override fun onScenarioComplete(result: ScenarioResult) {
        val statusText =
            when (result.status) {
                ResultStatus.PASSED -> "${green}PASSED$reset"
                ResultStatus.FAILED -> "${red}FAILED$reset"
                ResultStatus.SKIPPED -> "${yellow}SKIPPED$reset"
                ResultStatus.ERROR -> "${red}ERROR$reset"
                ResultStatus.PENDING -> "${yellow}PENDING$reset"
            }

        out.println("  ─────────────────")
        out.println("  Result: $statusText (${result.duration.toMillis()}ms)")
    }

    override fun onSuiteComplete(results: List<ScenarioResult>) {
        out.println()
        out.println("═══════════════════════════════════════")
        out.println("Test Suite Summary")
        out.println("═══════════════════════════════════════")

        val summary = TestSummaryBuilder.fromModelStatus(results) { it.status }

        out.println("  Total:   ${summary.total} scenarios")
        out.println("  ${green}Passed:$reset  ${summary.passed}")
        if (summary.failed > 0) out.println("  ${red}Failed:$reset  ${summary.failed}")
        if (summary.skipped > 0) out.println("  ${yellow}Skipped:$reset ${summary.skipped}")
        if (summary.errors > 0) out.println("  ${red}Errors:$reset  ${summary.errors}")

        val totalDuration = results.sumOf { it.duration.toMillis() }
        out.println("  Duration: ${totalDuration}ms")

        out.println("═══════════════════════════════════════")

        if (summary.failed > 0 || summary.errors > 0) {
            out.println()
            out.println("Failed scenarios:")
            results
                .filter { it.status == ResultStatus.FAILED || it.status == ResultStatus.ERROR }
                .forEach { result ->
                    out.println("  $red✗$reset ${result.scenario.name}")
                }
        }
    }
}
