@file:Suppress("MatchingDeclarationName")

package org.berrycrush.scenario.parsing

import org.berrycrush.scenario.AssertNode
import org.berrycrush.scenario.ConditionNode
import org.berrycrush.scenario.ConditionOperator
import org.berrycrush.scenario.ParserState
import org.berrycrush.scenario.SourceLocation
import org.berrycrush.scenario.TokenType
import org.berrycrush.scenario.ValueNode

private val VALID_OPERATORS =
    setOf(
        "equals",
        "=",
        "matches",
        "exists",
        "hassize",
        "size",
        "arraysize",
        "notempty",
        "contains",
        "greaterthan",
        ">",
        "lessthan",
        "<",
        ">=",
        "<=",
        "in",
    )

/**
 * Context for condition parsing - affects parsing style for some conditions.
 */
internal enum class ConditionContext {
    /** Parsing for assertion */
    ASSERT,

    /** Parsing for if/else condition */
    CONDITIONAL,
}

/**
 * Parse an assertion action.
 */
internal fun ParserState.parseAssertAction(): AssertNode? {
    val loc = currentLocation()
    advance() // consume 'assert'
    skipWhitespace()

    // Check for "not" keyword at the beginning
    var negate = false
    if (current().value.lowercase() == "not") {
        negate = true
        advance()
        skipWhitespace()
    }

    val condition = parseAssertCondition(loc, negate) ?: return null

    return AssertNode(
        condition = condition,
        location = loc,
    )
}

/**
 * Apply negation to a condition if needed.
 */
internal fun applyNegation(
    condition: ConditionNode,
    negate: Boolean,
    loc: SourceLocation,
): ConditionNode = if (negate) ConditionNode.NegatedCondition(condition, loc) else condition

/**
 * Parse a condition. All condition types are available in both assert and if contexts.
 */
internal fun ParserState.parseCondition(
    keyword: String,
    loc: SourceLocation,
    context: ConditionContext,
    negate: Boolean,
): ConditionNode? =
    when {
        keyword == "status" || keyword == "statuscode" -> {
            parseStatusCondition(loc, negate)
        }
        keyword == "schema" || keyword == "matchesschema" -> {
            parseSchemaCondition(loc, negate)
        }
        keyword == "header" -> {
            parseHeaderCondition(context, loc, negate)
        }
        keyword == "contains" || keyword == "bodycontains" -> {
            parseContainsCondition(loc, negate)
        }
        keyword == "responsetime" -> {
            parseResponseTimeCondition(loc, negate)
        }
        keyword.startsWith("$") || current().type == TokenType.JSON_PATH -> {
            parseJsonPathCondition(keyword, loc, context, negate)
        }
        else -> null
    }

private fun ParserState.parseResponseTimeCondition(
    loc: SourceLocation,
    negate: Boolean,
): ConditionNode? {
    advance()
    skipWhitespace()
    return parseValue()?.let { maxMs ->
        applyNegation(ConditionNode.ResponseTimeCondition(maxMs, loc), negate, loc)
    }
}

private fun ParserState.parseContainsCondition(
    loc: SourceLocation,
    negate: Boolean,
): ConditionNode? {
    advance()
    skipWhitespace()
    return parseValue()?.let { text ->
        applyNegation(ConditionNode.BodyContainsCondition(text, loc), negate, loc)
    }
}

private fun ParserState.parseHeaderCondition(
    context: ConditionContext,
    loc: SourceLocation,
    negate: Boolean,
): ConditionNode {
    advance()
    skipWhitespace()
    val headerName = parseHeaderName()
    skipWhitespace()

    val cond =
        if (context == ConditionContext.ASSERT) {
            if (current().type == TokenType.EQUALS || current().type == TokenType.COLON) {
                advance()
                skipWhitespace()
                ConditionNode.HeaderCondition(headerName, ConditionOperator.EQUALS, parseValue(), loc)
            } else {
                ConditionNode.HeaderCondition(headerName, ConditionOperator.EXISTS, null, loc)
            }
        } else {
            val (op, expected) = parseConditionOperatorAndValue()
            ConditionNode.HeaderCondition(headerName, op, expected, loc)
        }
    return applyNegation(cond, negate, loc)
}

private fun ParserState.parseSchemaCondition(
    loc: SourceLocation,
    negate: Boolean,
): ConditionNode {
    advance()
    skipWhitespace()
    return applyNegation(ConditionNode.SchemaCondition(loc), negate, loc)
}

private fun ParserState.parseStatusCondition(
    loc: SourceLocation,
    negate: Boolean,
): ConditionNode? {
    advance()
    skipWhitespace()
    return parseStatusValue()?.let { expected ->
        applyNegation(ConditionNode.StatusCondition(expected, loc), negate, loc)
    }
}

/**
 * Parse a JSON path condition with operator validation for assertions.
 */
internal fun ParserState.parseJsonPathCondition(
    keyword: String,
    loc: SourceLocation,
    context: ConditionContext,
    initialNegate: Boolean,
): ConditionNode? {
    val path =
        if (current().type == TokenType.JSON_PATH) {
            current().value.also { advance() }
        } else {
            keyword.also { advance() }
        }
    skipWhitespace()

    // Check for "not" after the JSON path (e.g., "$.name not equals ...")
    val negate =
        if (current().value.lowercase() == "not") {
            advance()
            skipWhitespace()
            true
        } else {
            initialNegate
        }

    // For assertions, validate operators
    if (context == ConditionContext.ASSERT) {
        val operatorText = current().value.lowercase()
        if (!VALID_OPERATORS.contains(operatorText) &&
            current().type != TokenType.EQUALS &&
            current().type != TokenType.NEWLINE &&
            current().type != TokenType.DEDENT
        ) {
            return addError(
                "Unknown assertion action '$operatorText' for JSON path. " +
                    "Expected: equals, matches, exists, hasSize, size, arraySize, notEmpty, contains, greaterThan, lessThan, or in",
            )
        }
    }

    val (op, expected) = parseConditionOperatorAndValue()
    return applyNegation(ConditionNode.JsonPathCondition(path, op, expected, loc), negate, loc)
}

/**
 * Parse a condition for an assertion.
 * If no built-in condition matches, checks for variable conditions,
 * then treats the text as a custom assertion pattern.
 */
internal fun ParserState.parseAssertCondition(
    loc: SourceLocation,
    initialNegate: Boolean,
): ConditionNode? {
    val typeOrPath = current().value.lowercase()

    // Use unified condition parsing for built-in conditions
    val result = parseCondition(typeOrPath, loc, ConditionContext.ASSERT, initialNegate)
    if (result != null) {
        return result
    }

    // Check for variable conditions starting with {{...}}
    // (e.g., {{server.hook.length}} equals 1)
    if (current().type == TokenType.VARIABLE) {
        val savedPos = pos
        val varName = buildVariablePath()
        skipWhitespace()

        // Check if there's a condition operator following the variable
        if (current().value.lowercase() in VALID_OPERATORS) {
            val (op, expected) = parseConditionOperatorAndValue()
            val cond = ConditionNode.VariableCondition(varName, op, expected, loc)
            return if (initialNegate) {
                ConditionNode.NegatedCondition(cond, loc)
            } else {
                cond
            }
        }
        // No valid operator - restore position and fall through to custom assertion
        pos = savedPos
    }

    // No built-in condition matched - treat as custom assertion pattern
    val patternBuilder = StringBuilder()
    while (!isAtEnd() && current().type != TokenType.NEWLINE && current().type != TokenType.EOF) {
        if (patternBuilder.isNotEmpty()) {
            patternBuilder.append(" ")
        }
        // Preserve quotes for STRING tokens so custom assertion matchers can extract parameters
        val tokenValue =
            if (current().type == TokenType.STRING) {
                "\"${current().value}\""
            } else {
                current().value
            }
        patternBuilder.append(tokenValue)
        advance()
    }

    return when (val pattern = patternBuilder.toString().trim()) {
        "" -> addError("Empty assertion pattern")
        else ->
            ConditionNode.CustomAssertionCondition(pattern, loc).let { condition ->
                if (initialNegate) {
                    ConditionNode.NegatedCondition(condition, loc)
                } else {
                    condition
                }
            }
    }
}

/**
 * Parse a condition operator and expected value.
 */
internal fun ParserState.parseConditionOperatorAndValue(): Pair<ConditionOperator, ValueNode?> {
    val opText = current().value.lowercase()

    val op =
        when (opText) {
            "equals", "=" -> {
                advance()
                skipWhitespace()
                ConditionOperator.EQUALS
            }
            "contains", "in" -> {
                advance()
                skipWhitespace()
                ConditionOperator.CONTAINS
            }
            "matches" -> {
                advance()
                skipWhitespace()
                ConditionOperator.MATCHES
            }
            "exists" -> {
                advance()
                return Pair(ConditionOperator.EXISTS, null)
            }
            "greaterthan", ">" -> {
                advance()
                skipWhitespace()
                ConditionOperator.GREATER_THAN
            }
            "lessthan", "<" -> {
                advance()
                skipWhitespace()
                ConditionOperator.LESS_THAN
            }
            "hassize", "size", "arraysize" -> {
                advance()
                skipWhitespace()
                ConditionOperator.HAS_SIZE
            }
            "notempty" -> {
                advance()
                return Pair(ConditionOperator.NOT_EMPTY, null)
            }
            ">=" -> {
                advance()
                skipWhitespace()
                ConditionOperator.GREATER_THAN_OR_EQUALS
            }
            "<=" -> {
                advance()
                skipWhitespace()
                ConditionOperator.LESS_THAN_OR_EQUALS
            }
            else -> {
                ConditionOperator.EQUALS
            }
        }

    val expected = parseValue()
    return Pair(op, expected)
}

/**
 * Parse a header name which may contain hyphens.
 */
internal fun ParserState.parseHeaderName(): String {
    val sb = StringBuilder()

    if (current().type == TokenType.IDENTIFIER || current().type == TokenType.OPERATION_ID) {
        sb.append(current().value)
        advance()
    }

    while (!isAtEnd() && current().value == "-") {
        sb.append("-")
        advance()

        if (current().type == TokenType.IDENTIFIER || current().type == TokenType.OPERATION_ID) {
            sb.append(current().value)
            advance()
        } else {
            break
        }
    }

    return sb.toString()
}
