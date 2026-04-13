package org.berrycrush.plugin.adapter

import org.berrycrush.plugin.ResultStatus
import org.berrycrush.plugin.ScenarioResult
import org.berrycrush.plugin.StepResult
import java.time.Duration
import org.berrycrush.model.ScenarioResult as ModelScenarioResult

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
                it.status == org.berrycrush.model.ResultStatus.FAILED ||
                    it.status == org.berrycrush.model.ResultStatus.ERROR
            }

    override val error: Throwable?
        get() = modelResult.stepResults.firstOrNull { it.error != null }?.error

    override val stepResults: List<StepResult>
        get() = modelResult.stepResults.map { StepResultAdapter(it) }
}
