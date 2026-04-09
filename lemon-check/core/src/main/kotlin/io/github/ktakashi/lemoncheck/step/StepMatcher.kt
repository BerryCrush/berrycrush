package io.github.ktakashi.lemoncheck.step

import java.util.regex.Pattern

/**
 * Matches step text against patterns and extracts parameters.
 *
 * Supports the following placeholders:
 * - `{int}` - matches an integer
 * - `{string}` - matches a quoted string
 * - `{word}` - matches a single word
 * - `{float}` - matches a floating-point number
 * - `{any}` - matches any text
 */
class StepMatcher {
    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{(int|string|word|float|any)\\}")

        private val PLACEHOLDER_PATTERNS =
            mapOf(
                "int" to "([-+]?\\d+)",
                "string" to "(?:\"([^\"]*)\"|'([^']*)')",
                "word" to "(\\w+)",
                "float" to "([-+]?\\d*\\.?\\d+)",
                "any" to "(.*?)",
            )

        private val PLACEHOLDER_CONVERTERS: Map<String, (String) -> Any?> =
            mapOf(
                "int" to { s -> s.toIntOrNull() ?: s.toLong() },
                "string" to { s -> s }, // Already extracted without quotes
                "word" to { s -> s },
                "float" to { s -> s.toDoubleOrNull() ?: s.toFloat() },
                "any" to { s -> s },
            )
    }

    /**
     * Compiles a step pattern into a compiled matcher.
     *
     * @param pattern The pattern with placeholders
     * @return A compiled pattern with metadata
     */
    fun compile(pattern: String): CompiledPattern {
        val placeholders = mutableListOf<String>()
        var regexPattern = Pattern.quote("")
        var lastEnd = 0

        PLACEHOLDER_REGEX.findAll(pattern).forEach { match ->
            val placeholder = match.groupValues[1]
            placeholders.add(placeholder)

            val beforePart = pattern.substring(lastEnd, match.range.first)
            regexPattern += Pattern.quote(beforePart)
            regexPattern += PLACEHOLDER_PATTERNS[placeholder] ?: "(.*?)"

            lastEnd = match.range.last + 1
        }

        if (lastEnd < pattern.length) {
            regexPattern += Pattern.quote(pattern.substring(lastEnd))
        }

        return CompiledPattern(
            originalPattern = pattern,
            regex = Pattern.compile("^$regexPattern$"),
            placeholders = placeholders,
        )
    }

    /**
     * Attempts to match step text against a compiled pattern.
     *
     * @param text The step text to match
     * @param compiledPattern The compiled pattern to match against
     * @return Extracted parameters if match succeeded, null otherwise
     */
    fun match(
        text: String,
        compiledPattern: CompiledPattern,
    ): List<Any?>? {
        val matcher = compiledPattern.regex.matcher(text)
        if (!matcher.matches()) {
            return null
        }

        val parameters = mutableListOf<Any?>()
        var groupIndex = 1

        for (placeholder in compiledPattern.placeholders) {
            val value =
                when (placeholder) {
                    "string" -> {
                        // String has two groups: double-quoted and single-quoted
                        val doubleQuoted = matcher.group(groupIndex)
                        val singleQuoted = matcher.group(groupIndex + 1)
                        groupIndex += 2
                        doubleQuoted ?: singleQuoted ?: ""
                    }
                    else -> {
                        val rawValue = matcher.group(groupIndex++) ?: ""
                        PLACEHOLDER_CONVERTERS[placeholder]?.invoke(rawValue) ?: rawValue
                    }
                }
            parameters.add(value)
        }

        return parameters
    }
}

/**
 * A compiled step pattern ready for matching.
 *
 * @property originalPattern The original pattern string
 * @property regex The compiled regex pattern
 * @property placeholders Ordered list of placeholder types
 */
data class CompiledPattern(
    val originalPattern: String,
    val regex: Pattern,
    val placeholders: List<String>,
)
