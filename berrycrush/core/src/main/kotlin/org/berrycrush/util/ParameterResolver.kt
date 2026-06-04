package org.berrycrush.util

import org.berrycrush.context.ExecutionContext

/**
 * Resolves variable references in parameter values.
 *
 * Supports the following variable reference patterns:
 * - `${env.VAR_NAME}` - Environment variable
 * - `${context.variableName}` - Variable from ExecutionContext
 * - `${param.name}` - Reference to another parameter (for parameter chaining)
 * - `${variableName}` - Shorthand for context variables
 *
 * Variable references can appear anywhere in a string value:
 * - `"https://${env.API_HOST}/api"` → `"https://example.com/api"`
 * - `"Bearer ${context.token}"` → `"Bearer abc123"`
 *
 * Unresolved variables remain as-is (no error is thrown).
 *
 * @property context Optional ExecutionContext for resolving context variables
 * @property parameters Parameter map for resolving param references
 * @property environmentProvider Function to get environment variables (defaults to System.getenv)
 */
class ParameterResolver(
    private val context: ExecutionContext? = null,
    private val parameters: Map<String, Any> = emptyMap(),
    private val environmentProvider: (String) -> String? = System::getenv,
) {
    companion object {
        /**
         * Regex pattern for matching variable references: `${...}`
         */
        private val VARIABLE_PATTERN = Regex("""\$\{([^}]+)\}""")

        /**
         * Maximum depth for recursive parameter resolution.
         */
        private const val MAX_RECURSION_DEPTH = 10
    }

    /**
     * Resolve all variable references in a string value.
     *
     * @param value The string potentially containing variable references
     * @param depth Current recursion depth (for cycle detection)
     * @return The resolved string with all references substituted
     */
    fun resolve(
        value: String,
        depth: Int = 0,
    ): String {
        if (depth > MAX_RECURSION_DEPTH) {
            return value // Prevent infinite recursion
        }

        return VARIABLE_PATTERN.replace(value) { match ->
            val variableRef = match.groupValues[1]
            resolveVariable(variableRef, depth) ?: match.value
        }
    }

    /**
     * Resolve all variable references in a parameter map.
     *
     * @param params The parameter map with potentially unresolved values
     * @return A new map with all string values resolved
     */
    fun resolveAll(params: Map<String, Any>): Map<String, Any> =
        params.mapValues { (_, value) ->
            when (value) {
                is String -> resolve(value)
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    resolveAll(value as Map<String, Any>)
                }
                is List<*> ->
                    value.map { item ->
                        when (item) {
                            is String -> resolve(item)
                            else -> item
                        }
                    }
                else -> value
            }
        }

    /**
     * Resolve a single variable reference.
     *
     * @param ref The variable reference (without ${})
     * @param depth Current recursion depth
     * @return The resolved value, or null if not found
     */
    private fun resolveVariable(
        ref: String,
        depth: Int,
    ): String? {
        val parts = ref.split(".", limit = 2)

        // Environment variable: ${env.VAR_NAME}
        return when (parts.size) {
            2 if parts[0] == "env" -> {
                environmentProvider(parts[1])
            }

            // Context variable: ${context.variableName}
            2 if parts[0] == "context" -> {
                context?.get<Any>(parts[1])?.toString()
            }

            // Parameter reference: ${param.name}
            2 if parts[0] == "param" -> {
                when (val paramValue = parameters[parts[1]]) {
                    is String -> resolve(paramValue, depth + 1)
                    else -> paramValue?.toString()
                }
            }

            // Shorthand for context variable: ${variableName}
            else -> {
                context?.get<Any>(ref)?.toString()
            }
        }
    }
}
