package org.berrycrush.executor.http

import org.berrycrush.executor.resolvers.RequestResolver
import org.berrycrush.model.HttpRequest
import org.berrycrush.model.HttpResponse
import org.berrycrush.model.Step
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.plugin.StepContext
import org.berrycrush.plugin.adapter.ScenarioContextAdapter

/**
 * Executor for HTTP requests during scenario execution.
 *
 * This interface abstracts HTTP request building and execution from
 * the main scenario executor.
 */
interface HttpExecutor : RequestResolver {
    fun execute(
        step: Step,
        specRegistry: SpecRegistry,
        stepContext: StepContext,
    ): HttpResponse {
        val resolvedStep = step.resolveCallTargets(stepContext)
        // Resolve the operation
        val (spec, resolvedOp) = resolve(resolvedStep, specRegistry)
        // Execute the HTTP request using the HttpExecutor
        val response = execute(resolvedStep, spec, resolvedOp, stepContext)
        // Update context with response
        return response
    }

    /**
     * Execute an HTTP request for the given step.
     *
     * @param step The step containing the operation and parameters
     * @param spec The loaded OpenAPI specification (for base URL and default headers)
     * @param operation The resolved OpenAPI operation
     * @param context The execution context with variables and state
     * @return The HTTP response from the server
     */
    fun execute(
        step: Step,
        spec: LoadedSpec,
        operation: ResolvedOperation,
        context: StepContext,
    ): HttpResponse =
        run {
            val scenarioContext = context.scenarioContext
            if (scenarioContext is ScenarioContextAdapter) {
                scenarioContext.addOperation(operation)
            }
            execute(resolve(step, spec, operation, context), context)
        }

    fun execute(
        request: HttpRequest,
        context: StepContext,
    ): HttpResponse

    fun resolve(
        step: Step,
        specRegistry: SpecRegistry,
    ) = specRegistry.resolve(step.operationId!!, step.specName)

    private fun Step.resolveCallTargets(stepContext: StepContext): Step {
        val resolvedOperationId = stepContext.resolveCallValue(operationId, "operation ID")
        val resolvedSpecName = stepContext.resolveCallValue(specName, "spec name", required = false)

        return if (resolvedOperationId == operationId && resolvedSpecName == specName) {
            this
        } else {
            copy(operationId = resolvedOperationId, specName = resolvedSpecName)
        }
    }

    private fun StepContext.resolveCallValue(
        raw: String?,
        label: String,
        required: Boolean = true,
    ): String? {
        if (raw == null) {
            require(!required) { "Missing $label in step '$stepDescription'" }
            return null
        }

        val resolved = interpolate(raw).trim()
        require(resolved.isNotEmpty()) {
            "Resolved $label is empty from '$raw' in step '$stepDescription'"
        }

        val unresolvedTemplate =
            resolved.contains("{{") ||
                resolved.contains("}}") ||
                resolved.contains("\${")
        require(!unresolvedTemplate) {
            "Unable to resolve $label from '$raw' in step '$stepDescription'"
        }

        return resolved
    }
}
