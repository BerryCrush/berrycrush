package org.berrycrush.executor.resolvers

import org.berrycrush.context.ExecutionContext

internal fun ExecutionContext.resolveParam(value: Any): Any =
    when (value) {
        is String -> this.interpolate(value)
        else -> value
    }

internal fun ExecutionContext.resolveParams(params: Map<String, Any>): Map<String, Any> =
    params.mapValues { (_, value) -> resolveParam(value) }
