package io.github.ktakashi.lemoncheck.plugin.adapter

import io.github.ktakashi.lemoncheck.plugin.AssertionFailure
import io.github.ktakashi.lemoncheck.plugin.ResultStatus
import io.github.ktakashi.lemoncheck.plugin.StepResult
import java.time.Duration
import io.github.ktakashi.lemoncheck.model.StepResult as ModelStepResult

/**
 * Adapter that bridges model [ModelStepResult] with plugin [StepResult] interface.
 */
class StepResultAdapter(
    private val modelResult: ModelStepResult,
) : StepResult {
    override val status: ResultStatus
        get() = modelResult.status.mapTo()

    override val duration: Duration
        get() = modelResult.duration

    override val failure: AssertionFailure?
        get() =
            modelResult.assertionResults
                .firstOrNull { !it.passed }
                ?.let { failedAssertion ->
                    AssertionFailure(
                        message = failedAssertion.message,
                        expected = failedAssertion.assertion.expected,
                        actual = failedAssertion.actual,
                        diff = null,
                        stepDescription = modelResult.step.description,
                        assertionType = failedAssertion.assertion.type.name,
                        requestSnapshot = null,
                        responseSnapshot = null,
                    )
                }

    override val error: Throwable?
        get() = modelResult.error
}
