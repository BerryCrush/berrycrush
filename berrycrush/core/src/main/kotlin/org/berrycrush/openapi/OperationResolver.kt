package org.berrycrush.openapi

import org.berrycrush.exception.OperationNotFoundException

private const val STATUS_CODE_RANGE_DIVISOR = 100

/**
 * Resolves OpenAPI operation IDs to path and method information.
 *
 * This class provides operation resolution for an OpenApiSpec.
 */
class OperationResolver(
    private val spec: OpenApiSpec,
) {
    private data class MethodPathKey(
        val method: HttpMethod,
        val routeShape: String,
    )

    private val operationIndex: Map<String, ResolvedOperation> by lazy {
        buildOperationIndex()
    }

    private val methodPathIndex: Map<MethodPathKey, ResolvedOperation> by lazy {
        buildMethodPathIndex()
    }

    /**
     * Resolve an operation ID to its path and HTTP method.
     *
     * @param operationId The OpenAPI operation ID
     * @return Resolved operation with path and method
     * @throws OperationNotFoundException if operation is not found
     */
    fun resolve(operationId: String): ResolvedOperation =
        operationIndex[operationId]
            ?: throw OperationNotFoundException(operationId, operationIndex.keys.toList())

    /**
     * Resolve an operation by method and path, matching on route shape.
     *
     * Route shape matching ignores path variable names, so `/pets/{id}` and
     * `/pets/{petId}` are treated as equivalent.
     */
    fun resolve(
        method: HttpMethod,
        path: String,
    ): ResolvedOperation? = methodPathIndex[MethodPathKey(method, normalizeRouteShape(path))]

    /**
     * Check if an operation ID exists.
     */
    fun hasOperation(operationId: String): Boolean = operationId in operationIndex

    /**
     * Get all available operation IDs.
     */
    fun allOperationIds(): Set<String> = operationIndex.keys

    private fun buildOperationIndex(): Map<String, ResolvedOperation> =
        spec
            .getAllOperations()
            .filter { it.operationId != null }
            .associate { op ->
                op.operationId!! to
                    ResolvedOperation(
                        operationId = op.operationId!!,
                        path = op.path,
                        method = op.method,
                        operation = op,
                    )
            }

    private fun buildMethodPathIndex(): Map<MethodPathKey, ResolvedOperation> =
        spec
            .getAllOperations()
            .associate { op ->
                MethodPathKey(op.method, normalizeRouteShape(op.path)) to
                    ResolvedOperation(
                        operationId = op.operationId ?: "${op.method} ${op.path}",
                        path = op.path,
                        method = op.method,
                        operation = op,
                    )
            }

    private fun normalizeRouteShape(path: String): String {
        val trimmed = path.trim().let { if (it == "/") it else it.trimEnd('/') }
        return trimmed
            .split('/')
            .filter { it.isNotEmpty() }
            .joinToString(separator = "/", prefix = "/") { segment ->
                if (segment.startsWith("{") && segment.endsWith("}")) {
                    "{}"
                } else {
                    segment
                }
            }
    }
}

/**
 * A resolved OpenAPI operation with path and method information.
 */
data class ResolvedOperation(
    val operationId: String,
    val path: String,
    val method: HttpMethod,
    val operation: OperationSpec,
)

/**
 * Find the response definition for a given status code.
 *
 * Tries exact match first, then wildcard (2XX, 4XX, etc.), then default.
 *
 * @param statusCode The HTTP status code to find the response for
 * @return The ApiResponse definition, or null if not found
 */
fun ResolvedOperation.findResponse(statusCode: Int): ResponseSpec? {
    val responses = operation.responses
    if (responses.isEmpty()) return null

    val wildcard = "${statusCode / STATUS_CODE_RANGE_DIVISOR}XX"
    return responses[statusCode.toString()]
        ?: responses[wildcard]
        ?: responses["default"]
}
