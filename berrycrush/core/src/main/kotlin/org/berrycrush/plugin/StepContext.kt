package org.berrycrush.plugin

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
}
