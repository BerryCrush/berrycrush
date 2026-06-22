package org.berrycrush.plugin.adapter

import org.berrycrush.model.Step
import org.berrycrush.plugin.HttpRequest
import org.berrycrush.plugin.HttpResponse
import org.berrycrush.plugin.ScenarioContext
import org.berrycrush.plugin.StepContext
import org.berrycrush.plugin.StepType
import java.time.Duration
import org.berrycrush.model.StepType as ModelStepType

/**
 * Adapter that bridges [Step] model with [StepContext] plugin interface.
 *
 * Provides plugin-visible step context from the existing step model.
 */
class StepContextAdapter(
    private val step: Step,
    override val stepIndex: Int,
    override val scenarioContext: ScenarioContext,
) : StepContext {
    private var httpRequest: HttpRequest? = scenarioContext.audits.lastOrNull()?.request
    private var httpResponse: HttpResponse? = scenarioContext.audits.lastOrNull()?.response
    private var duration: Duration? = null
    override val responseTime
        get() = duration

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

    fun updateResponseTime(responseTime: Duration) {
        this.duration = responseTime
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
