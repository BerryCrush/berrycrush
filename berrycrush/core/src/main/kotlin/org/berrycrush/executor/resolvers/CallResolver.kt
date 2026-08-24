package org.berrycrush.executor.resolvers

import org.berrycrush.model.HttpMethod
import org.berrycrush.model.Step
import org.berrycrush.plugin.StepContext

@Suppress("ThrowsCount")
internal fun StepContext.resolveCall(step: Step): Step {
    val resolvedSpecName = resolveCallValue(step.specName, "spec name", required = false)
    val isRawTarget = step.rawMethod != null || step.rawPath != null

    if (isRawTarget) {
        val resolvedMethod =
            resolveCallValue(step.rawMethod, "raw HTTP method")
                ?: throw IllegalArgumentException("Missing raw HTTP method in step '$stepDescription'")
        val resolvedPath =
            resolveCallValue(step.rawPath, "raw HTTP path")
                ?: throw IllegalArgumentException("Missing raw HTTP path in step '$stepDescription'")

        require(HttpMethod.fromName(resolvedMethod) != null) {
            "Invalid raw HTTP method '$resolvedMethod' in step '$stepDescription'. " +
                "Expected one of ${HttpMethod.entries.joinToString { it.name }}"
        }
        require(resolvedPath.startsWith('/')) {
            "Invalid raw HTTP path '$resolvedPath' in step '$stepDescription'. Expected path starting with '/'"
        }

        return if (
            resolvedMethod == step.rawMethod &&
            resolvedPath == step.rawPath &&
            resolvedSpecName == step.specName
        ) {
            step
        } else {
            step.copy(rawMethod = resolvedMethod, rawPath = resolvedPath, specName = resolvedSpecName)
        }
    }

    val resolvedOperationId =
        resolveCallValue(step.operationId, "operation ID")
            ?: throw IllegalArgumentException("Missing operation ID in step '$stepDescription'")

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
