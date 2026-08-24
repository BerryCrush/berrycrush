package org.berrycrush.executor.resolvers

import org.berrycrush.model.Step
import org.berrycrush.plugin.StepContext

internal fun StepContext.resolveCall(step: Step): Step {
    val resolvedOperationId = resolveCallValue(step.operationId, "operation ID")
    val resolvedSpecName = resolveCallValue(step.specName, "spec name", required = false)

    return if (resolvedOperationId == step.operationId && resolvedSpecName == step.specName) {
        step
    } else {
        step.copy(operationId = resolvedOperationId, specName = resolvedSpecName)
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
        resolved.contains("{{") || resolved.contains("}}")
    require(!unresolvedTemplate) {
        "Unable to resolve $label from '$raw' in step '$stepDescription'"
    }

    return resolved
}
