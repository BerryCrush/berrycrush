package org.berrycrush.plugin

import org.berrycrush.model.HttpMethod
import org.berrycrush.model.HttpRequest
import org.berrycrush.model.HttpResponse
import java.time.Duration

/**
 * Execution context for a single step within a scenario.
 *
 * Provides access to step-level details including the step description, type,
 * HTTP request/response information, and the parent scenario context.
 *
 * @property stepDescription Full step description text
 * @property stepType Type of step (CALL, ASSERT, EXTRACT, CUSTOM)
 * @property stepIndex Zero-based index of this step within the parent scenario
 * @property scenarioContext Parent scenario execution context
 * @property request HTTP request details (null for non-CALL steps or before request is made)
 * @property response HTTP response details (null until response is received)
 * @property operationId OpenAPI operation ID if applicable (null otherwise)
 */
interface StepContext {
    val stepDescription: String
    val stepType: StepType
    val stepIndex: Int
    val scenarioContext: ScenarioContext
    val request: HttpRequest?
    val response: HttpResponse?
    val operationId: String?
    val responseTime: Duration?
    val operation: StepOperation?

    fun <T : Any> resolveParam(param: T) = scenarioContext.executionContext.resolveParam(param)

    fun <T : Any> resolveParams(params: Map<String, T>) = scenarioContext.executionContext.resolveParams(params)

    fun interpolate(v: String) = scenarioContext.executionContext.interpolate(v)

    fun allExecutionVariables() = scenarioContext.executionContext.allVariables()

    operator fun set(
        key: String,
        value: Any,
    ) {
        scenarioContext.executionContext[key] = value
    }

    operator fun <T> get(key: String): T? = scenarioContext.executionContext[key]

    fun allVariables() = scenarioContext.executionContext.allVariables()
}

interface StepOperation {
    val operationId: String
    val path: String
    val method: HttpMethod
}
