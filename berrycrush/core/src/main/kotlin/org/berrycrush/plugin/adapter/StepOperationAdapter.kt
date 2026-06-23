package org.berrycrush.plugin.adapter

import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.plugin.HttpMethod
import org.berrycrush.plugin.StepOperation

class StepOperationAdapter(
    val operation: ResolvedOperation,
) : StepOperation {
    override val operationId: String
        get() = operation.operationId

    override val method: HttpMethod
        get() = operation.method

    override val path: String
        get() = operation.path
}
