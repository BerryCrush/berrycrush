package io.github.ktakashi.lemoncheck.plugin.adapter

import io.github.ktakashi.lemoncheck.plugin.ResultStatus
import io.github.ktakashi.lemoncheck.plugin.ScenarioResult
import io.github.ktakashi.lemoncheck.plugin.StepResult
import java.time.Duration
import io.github.ktakashi.lemoncheck.model.ScenarioResult as ModelScenarioResult

/**
 * Adapter that bridges model [ModelScenarioResult] with plugin [ScenarioResult] interface.
 */
class ScenarioResultAdapter(
    private val modelResult: ModelScenarioResult,
) : ScenarioResult {
    override val status: ResultStatus
        get() = modelResult.status.mapTo()

    override val duration: Duration
        get() = modelResult.duration

    override val failedStep: Int
        get() =
            modelResult.stepResults.indexOfFirst {
                it.status == io.github.ktakashi.lemoncheck.model.ResultStatus.FAILED ||
                    it.status == io.github.ktakashi.lemoncheck.model.ResultStatus.ERROR
            }

    override val error: Throwable?
        get() = modelResult.stepResults.firstOrNull { it.error != null }?.error

    override val stepResults: List<StepResult>
        get() = modelResult.stepResults.map { StepResultAdapter(it) }
}
