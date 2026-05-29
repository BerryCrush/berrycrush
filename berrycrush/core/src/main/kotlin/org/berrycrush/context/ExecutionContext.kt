package org.berrycrush.context

import org.berrycrush.openapi.ResolvedOperation
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * Execution context that holds variables and state during scenario execution.
 *
 * ## Thread Safety
 *
 * This class is designed for thread-safe usage in parallel execution scenarios:
 * - Variables are stored in a [ConcurrentHashMap]
 * - Mutable state fields use `@Volatile` for visibility
 * - [createIsolatedCopy] creates a fully independent copy for parallel scenarios
 *
 * ## Parallel Execution
 *
 * For parallel scenario execution, use [createIsolatedCopy] to ensure each
 * scenario has its own isolated context:
 *
 * ```kotlin
 * val parallelContext = sharedContext.createIsolatedCopy()
 * executor.execute(scenario, parallelContext)
 * ```
 *
 * For sequential execution with shared variables, use [createChild]:
 *
 * ```kotlin
 * val childContext = parentContext.createChild()
 * ```
 */
class ExecutionContext {
    private val variables = ConcurrentHashMap<String, Any>()

    /**
     * The last HTTP response received.
     */
    @Volatile
    var lastResponse: HttpResponse<String>? = null
        private set

    /**
     * The current resolved operation for the executing step.
     * Used for schema validation against the OpenAPI spec.
     */
    @Volatile
    var currentOperation: ResolvedOperation? = null
        private set

    /**
     * The last HTTP response time in milliseconds.
     * Set after each HTTP request completes.
     */
    @Volatile
    var lastResponseTimeMs: Long? = null
        private set

    /**
     * The last response body (cached for convenience).
     */
    val lastResponseBody: String?
        get() = lastResponse?.body()

    /**
     * The last response status code.
     */
    val lastStatusCode: Int?
        get() = lastResponse?.statusCode()

    /**
     * Store a variable value.
     */
    operator fun set(
        name: String,
        value: Any,
    ) {
        variables[name] = value
    }

    /**
     * Get a variable value.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(name: String): T? = variables[name] as? T

    /**
     * Get a variable value with a default.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrDefault(
        name: String,
        default: T,
    ): T = (variables[name] as? T) ?: default

    /**
     * Check if a variable exists.
     */
    fun contains(name: String): Boolean = variables.containsKey(name)

    /**
     * Get all variable names.
     */
    fun variableNames(): Set<String> = variables.keys.toSet()

    /**
     * Get all variables as a map.
     */
    fun allVariables(): Map<String, Any?> = variables.toMap()

    /**
     * Update the last response.
     */
    fun updateLastResponse(response: HttpResponse<String>) {
        lastResponse = response
    }

    /**
     * Update the current operation being executed.
     */
    fun updateCurrentOperation(operation: ResolvedOperation?) {
        currentOperation = operation
    }

    /**
     * Update the last response time.
     */
    fun updateLastResponseTime(timeMs: Long) {
        lastResponseTimeMs = timeMs
    }

    /**
     * Clear all variables and state.
     */
    fun clear() {
        variables.clear()
        lastResponse = null
        currentOperation = null
        lastResponseTimeMs = null
    }

    /**
     * Create a child context that inherits variables from this context.
     * The child shares variable references with the parent.
     * Use [createIsolatedCopy] for fully isolated parallel execution.
     */
    fun createChild(): ExecutionContext {
        val child = ExecutionContext()
        child.variables.putAll(variables)
        return child
    }

    /**
     * Create a fully isolated copy of this context for parallel execution.
     * All state is copied, ensuring no shared mutable state between copies.
     *
     * This is the recommended method for parallel scenario execution.
     */
    fun createIsolatedCopy(): ExecutionContext {
        val copy = ExecutionContext()
        // Copy all variables
        variables.forEach { (key, value) ->
            copy.variables[key] = value
        }
        // Copy volatile state using update methods
        lastResponse?.let { copy.updateLastResponse(it) }
        copy.updateCurrentOperation(currentOperation)
        lastResponseTimeMs?.let { copy.updateLastResponseTime(it) }
        return copy
    }

    /**
     * Resolve a string with variable interpolation.
     *
     * Variables can be referenced using:
     * - Mustache-style: {{variableName}}
     * - Dollar-style: ${variableName} or $variableName
     *
     * Escaping:
     * - Use \\{{ to produce literal {{
     * - Use \\$ to produce literal $
     */
    fun interpolate(template: String): String {
        // First, temporarily replace escaped sequences with placeholders
        val escapePlaceholder1 = "\u0001ESCAPED_OPEN_BRACE\u0001"
        val escapePlaceholder2 = "\u0001ESCAPED_DOLLAR\u0001"

        var result =
            template
                .replace("\\{{", escapePlaceholder1)
                .replace("\\$", escapePlaceholder2)

        // Replace {{name}} patterns (mustache-style, used in scenario files)
        val mustachePattern = Regex("""\{\{(\w+)}}""")
        result =
            mustachePattern.replace(result) { match ->
                val varName = match.groupValues[1]
                variables[varName]?.toString() ?: match.value
            }

        // Replace ${name} patterns
        val bracketPattern = Regex("""\$\{([^}]+)}""")
        result =
            bracketPattern.replace(result) { match ->
                val varName = match.groupValues[1]
                variables[varName]?.toString() ?: match.value
            }

        // Replace $name patterns (word boundary)
        val simplePattern = Regex("""\$(\w+)""")
        result =
            simplePattern.replace(result) { match ->
                val varName = match.groupValues[1]
                variables[varName]?.toString() ?: match.value
            }

        // Restore escaped sequences as literals
        return result
            .replace(escapePlaceholder1, "{{")
            .replace(escapePlaceholder2, "$")
    }
}
