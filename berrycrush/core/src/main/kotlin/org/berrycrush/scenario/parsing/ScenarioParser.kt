@file:Suppress("TooManyFunctions")

package org.berrycrush.scenario.parsing

import org.berrycrush.scenario.BackgroundNode
import org.berrycrush.scenario.FeatureNode
import org.berrycrush.scenario.FragmentNode
import org.berrycrush.scenario.ParametersNode
import org.berrycrush.scenario.ParserState
import org.berrycrush.scenario.ScenarioNode
import org.berrycrush.scenario.TokenType

/**
 * Parse tags preceding a scenario or feature.
 * Syntax: @tag1 @tag2 @tag3
 */
internal fun ParserState.parseTags(): Set<String> {
    val tags = mutableSetOf<String>()

    while (!isAtEnd() && current().type == TokenType.TAG) {
        tags.add(current().value)
        advance()
        skipWhitespace()
        skipNewlines()
    }

    return tags
}

/**
 * Parse optional scenario-level parameters block.
 * Returns null if no parameters block is present.
 *
 * Handles the INDENT token peek/retreat pattern:
 * - If INDENT followed by PARAMETERS → parse parameters
 * - If INDENT followed by other token → retreat to let parseSteps handle it
 */
internal fun ParserState.parseOptionalParameters(): ParametersNode? {
    if (current().type != TokenType.INDENT) return null
    advance() // consume indent
    return if (current().type == TokenType.PARAMETERS) {
        parseParameters()
    } else {
        retreat()
        null
    }
}

/**
 * Parse the header portion of a scenario or outline.
 * Returns the scenario name if successful, null otherwise.
 *
 * Pattern: KEYWORD COLON NAME NEWLINES
 */
internal fun ParserState.parseScenarioHeader(expectedType: TokenType): String? {
    if (!expect(expectedType)) return null
    skipWhitespace()
    if (!expect(TokenType.COLON)) return null
    skipWhitespace()
    return parseScenarioName()?.also { skipNewlines() }
}

/**
 * Parse a scenario definition.
 *
 * Scenarios can optionally have a parameters block before their steps:
 * ```
 * scenario: Create pet with extended timeout
 *   parameters:
 *     timeout: 120
 *   when I create a pet
 *     ...
 * ```
 */
internal fun ParserState.parseScenario(tags: Set<String> = emptySet()): ScenarioNode? {
    val loc = currentLocation()
    val name = parseScenarioHeader(TokenType.SCENARIO) ?: return null
    val parameters = parseOptionalParameters()
    val steps = parseSteps()

    return ScenarioNode(
        name = name,
        steps = steps,
        isOutline = false,
        examples = null,
        tags = tags,
        parameters = parameters,
        location = loc,
    )
}

/**
 * Parse a scenario outline with examples.
 *
 * Scenario outlines can optionally have a parameters block before their steps:
 * ```
 * outline: Create pet with different names
 *   parameters:
 *     timeout: 120
 *   when I create a pet with name "<name>"
 *     ...
 *   examples:
 *     | name    |
 *     | Fluffy  |
 *     | Buddy   |
 * ```
 */
internal fun ParserState.parseScenarioOutline(tags: Set<String> = emptySet()): ScenarioNode? {
    val loc = currentLocation()
    val name = parseScenarioHeader(TokenType.OUTLINE) ?: return null
    val parameters = parseOptionalParameters()
    val steps = parseSteps()
    val examples = parseExamples()

    return ScenarioNode(
        name = name,
        steps = steps,
        isOutline = true,
        examples = examples,
        tags = tags,
        parameters = parameters,
        location = loc,
    )
}

/**
 * Parse a feature block containing grouped scenarios and optional background.
 */
internal fun ParserState.parseFeature(tags: Set<String> = emptySet()): FeatureNode? {
    val loc = currentLocation()
    val name = parseScenarioHeader(TokenType.FEATURE) ?: return null
    skipNewlines()
    val parameters: ParametersNode? = parseOptionalParameters()
    // Expect indent for feature body
    advanceIf(TokenType.INDENT)

    var background: BackgroundNode? = null
    val scenarios = mutableListOf<ScenarioNode>()

    while (!isAtEnd() && current().type != TokenType.DEDENT && current().type != TokenType.FEATURE) {
        skipNewlines()
        if (isAtEnd() || current().type == TokenType.DEDENT || current().type == TokenType.FEATURE) break

        // Parse tags for nested scenarios
        val scenarioTags = parseTags()

        when (current().type) {
            TokenType.BACKGROUND -> background = parseBackground()
            TokenType.SCENARIO -> parseScenario(scenarioTags)?.let(scenarios::add)
            TokenType.OUTLINE -> parseScenarioOutline(scenarioTags)?.let(scenarios::add)
            TokenType.NEWLINE, TokenType.INDENT -> advance()
            else -> {
                addParseFeatureError()
                break
            }
        }
        skipNewlines()
    }
    // Consume dedent if present
    advanceIf(TokenType.DEDENT)

    return FeatureNode(
        name = name,
        parameters = parameters,
        background = background,
        scenarios = scenarios,
        tags = tags,
        location = loc,
    )
}

private fun ParserState.addParseFeatureError() {
    if (current().type == TokenType.PARAMETERS) {
        addError<Unit>(
            "Feature parameters must appear before scenarios",
            expected = "scenario or outline",
            found = "parameters",
        )
    }
    if (current().type != TokenType.DEDENT && current().type != TokenType.EOF) {
        addError<Unit>(
            "Unexpected token in feature",
            expected = "background, scenario, or outline",
            found = current().value,
        )
        advance()
    }
}

/**
 * Parse background steps shared by all scenarios in a feature.
 */
internal fun ParserState.parseBackground(): BackgroundNode? {
    val loc = currentLocation()

    if (!expect(TokenType.BACKGROUND)) return null
    skipWhitespace()

    if (!expect(TokenType.COLON)) return null
    skipNewlines()

    val steps = parseSteps()

    return BackgroundNode(
        steps = steps,
        location = loc,
    )
}

/**
 * Parse a fragment definition.
 */
internal fun ParserState.parseFragment(): FragmentNode? {
    val loc = currentLocation()

    if (!expect(TokenType.FRAGMENT)) return null
    skipWhitespace()

    if (!expect(TokenType.COLON)) return null
    skipWhitespace()

    return parseFragmentName()?.let { name ->
        skipNewlines()
        val steps = parseSteps()
        FragmentNode(
            name = name,
            steps = steps,
            location = loc,
        )
    }
}

/**
 * Parse file-level or feature-level parameters block.
 * Supports both flat and nested parameter formats:
 *
 * Flat format:
 * ```
 * parameters:
 *   retry.maxAttempts: 3
 *   retry.delay: "500ms"
 * ```
 *
 * Nested format (flattened to dot notation internally):
 * ```
 * parameters:
 *   retry:
 *     maxAttempts: 3
 *     delay: "500ms"
 * ```
 */
internal fun ParserState.parseParameters(): ParametersNode? {
    val loc = currentLocation()

    if (!expect(TokenType.PARAMETERS)) return null
    skipWhitespace()

    if (!expect(TokenType.COLON)) return null
    skipNewlines()

    val values = mutableMapOf<String, Any>()

    // Expect indent
    if (current().type == TokenType.INDENT) {
        advance()
    }

    // Parse parameter entries (supports nested blocks)
    parseParameterEntries(values, prefix = "")

    // Handle dedent
    if (current().type == TokenType.DEDENT) {
        advance()
    }

    return ParametersNode(values, loc)
}

/**
 * Token types that indicate the end of a parameters block.
 */
private val PARAMETER_BLOCK_TERMINATORS =
    setOf(
        TokenType.SCENARIO,
        TokenType.OUTLINE,
        TokenType.FRAGMENT,
        TokenType.PARAMETERS,
        TokenType.BACKGROUND,
        TokenType.FEATURE,
    )

/**
 * Check if a token type is a keyword that can be used as a parameter name.
 */
private fun isKeywordToken(type: TokenType): Boolean =
    type in
        setOf(
            TokenType.INCLUDE,
            TokenType.USING,
        )

/**
 * Parse parameter entries recursively, supporting nested blocks.
 * Nested parameters are flattened to dot notation.
 *
 * @param result The map to store parameter values
 * @param prefix The current key prefix for nested parameters (e.g., "retry.")
 */
@Suppress("CyclomaticComplexMethod")
private fun ParserState.parseParameterEntries(
    result: MutableMap<String, Any>,
    prefix: String,
) {
    while (!isAtEnd() &&
        current().type != TokenType.DEDENT &&
        current().type !in PARAMETER_BLOCK_TERMINATORS
    ) {
        if (current().type == TokenType.NEWLINE) {
            advance()
            continue
        }

        // Parse parameter name (supports keywords like 'include' as names)
        val paramName =
            when {
                current().type == TokenType.IDENTIFIER -> parseParameterName()
                isKeywordToken(current().type) -> {
                    val name = current().value
                    advance()
                    name
                }
                else -> null
            }

        if (paramName == null) break
        skipWhitespace()

        if (!expect(TokenType.COLON)) {
            addError<Unit>("Expected ':' after parameter name")
            break
        }
        skipWhitespace()

        // Check if this is a nested block (no value on same line, followed by indent)
        if (current().type == TokenType.NEWLINE) {
            advance() // consume newline
            if (current().type == TokenType.INDENT) {
                // Nested block - recurse with updated prefix
                advance() // consume indent
                parseParameterEntries(result, "$prefix$paramName.")
                // Handle dedent from nested block
                if (current().type == TokenType.DEDENT) {
                    advance()
                }
            }
            // else: empty value, continue to next parameter
        } else {
            // Value on same line - parse it
            val value = parseParameterValue()
            if (value != null) {
                val fullKey = prefix + paramName
                if (result.containsKey(fullKey) && fullKey.isAliasParameterKey()) {
                    addError<Unit>("Duplicate alias declaration in the same parameters block", found = fullKey)
                }
                result[fullKey] = value
            }
            skipNewlines()
        }
    }
}

private fun String.isAliasParameterKey(): Boolean =
    startsWith("binding.alias.") || Regex("^binding\\.[^.]+\\.alias\\.").containsMatchIn(this)

/**
 * Parse a parameter name, supporting compound names with dots and hyphens.
 */
internal fun ParserState.parseParameterName(): String? {
    if (current().type != TokenType.IDENTIFIER) {
        return null
    }

    val parts = StringBuilder(current().value)
    advance()

    while (!isAtEnd() && current().type != TokenType.COLON && current().type != TokenType.NEWLINE) {
        when (current().type) {
            TokenType.DOT -> {
                parts.append(".")
                advance()
            }
            TokenType.IDENTIFIER -> {
                parts.append(current().value)
                advance()
            }
            TokenType.NUMBER -> {
                parts.append(current().value)
                advance()
            }
            TokenType.ERROR -> {
                val value = current().value
                if (value == "-" || value.startsWith("-")) {
                    parts.append(value)
                    advance()
                } else {
                    break
                }
            }
            else -> break
        }
    }

    return parts.toString()
}

/**
 * Parse a parameter value (string, number, or boolean).
 */
internal fun ParserState.parseParameterValue(): Any? =
    when (current().type) {
        TokenType.STRING -> {
            val value = current().value
            advance()
            value
        }
        TokenType.NUMBER -> {
            val value = current().value
            advance()
            if (value.contains('.')) value.toDouble() else value.toLong()
        }
        TokenType.IDENTIFIER -> {
            val value = current().value
            advance()
            when (value.lowercase()) {
                "true" -> true
                "false" -> false
                else -> value
            }
        }
        else -> addError("Expected parameter value")
    }

/**
 * Parse a scenario or fragment name.
 */
internal fun ParserState.parseScenarioName(): String? {
    val nameParts = mutableListOf<String>()

    while (!isAtEnd() && current().type != TokenType.NEWLINE && current().type != TokenType.EOF) {
        nameParts.add(current().value)
        advance()
    }

    return if (nameParts.isEmpty()) {
        addError("Expected scenario name")
    } else {
        nameParts.joinToString(" ").trim()
    }
}

/**
 * Parse a fragment name (alias for parseScenarioName).
 */
internal fun ParserState.parseFragmentName(): String? = parseScenarioName()
