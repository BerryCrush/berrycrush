package org.berrycrush.report

import org.berrycrush.model.ScenarioResult
import org.berrycrush.model.StepResult

/**
 * Interface for reporting test execution results.
 */
interface TestReporter {
    /**
     * Called when scenario execution starts.
     */
    fun onScenarioStart(scenarioName: String)

    /**
     * Called after each step completes.
     */
    fun onStepComplete(stepResult: StepResult)

    /**
     * Called when scenario execution completes.
     */
    fun onScenarioComplete(result: ScenarioResult)

    /**
     * Called when all scenarios in a suite complete.
     */
    fun onSuiteComplete(results: List<ScenarioResult>)
}
