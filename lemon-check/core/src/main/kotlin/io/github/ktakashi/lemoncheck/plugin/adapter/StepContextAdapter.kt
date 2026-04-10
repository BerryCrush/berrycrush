package io.github.ktakashi.lemoncheck.plugin.adapter

import io.github.ktakashi.lemoncheck.model.Step
import io.github.ktakashi.lemoncheck.plugin.HttpRequest
import io.github.ktakashi.lemoncheck.plugin.HttpResponse
import io.github.ktakashi.lemoncheck.plugin.ScenarioContext
import io.github.ktakashi.lemoncheck.plugin.StepContext
import io.github.ktakashi.lemoncheck.plugin.StepType
import io.github.ktakashi.lemoncheck.model.StepType as ModelStepType

/**
 * Adapter that bridges [Step] model with [StepContext] plugin interface.
 *
 * Provides plugin-visible step context from the existing step model.
 */
class StepContextAdapter(
    private val step: Step,
    override val stepIndex: Int,
    override val scenarioContext: ScenarioContext,
    private var httpRequest: HttpRequest? = null,
    private var httpResponse: HttpResponse? = null,
) : StepContext {
    override val stepDescription: String
        get() = step.description

    override val stepType: StepType
        get() = mapStepType(step.type)

    override val operationId: String?
        get() = step.operationId

    override val request: HttpRequest?
        get() = httpRequest

    override val response: HttpResponse?
        get() = httpResponse

    /**
     * Update the request snapshot after HTTP call is prepared.
     */
    fun setRequest(request: HttpRequest) {
        this.httpRequest = request
    }

    /**
     * Update the response snapshot after HTTP call completes.
     */
    fun setResponse(response: HttpResponse) {
        this.httpResponse = response
    }

    private fun mapStepType(modelType: ModelStepType): StepType =
        when (modelType) {
            ModelStepType.GIVEN -> StepType.CUSTOM
            ModelStepType.WHEN -> StepType.CALL
            ModelStepType.THEN -> StepType.ASSERT
            ModelStepType.AND -> StepType.CUSTOM
            ModelStepType.BUT -> StepType.CUSTOM
        }
}
