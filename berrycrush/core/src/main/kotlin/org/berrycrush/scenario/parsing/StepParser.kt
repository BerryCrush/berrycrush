package org.berrycrush.scenario.parsing

import org.berrycrush.scenario.ActionNode
import org.berrycrush.scenario.ParserState
import org.berrycrush.scenario.StepKeyword
import org.berrycrush.scenario.StepNode
import org.berrycrush.scenario.TokenType

/**
 * Parse steps within a scenario or background block.
 */
private val toplevelKeywords =
    listOf(TokenType.DEDENT, TokenType.SCENARIO, TokenType.OUTLINE, TokenType.FRAGMENT, TokenType.EXAMPLES, TokenType.EOF)

internal fun ParserState.parseSteps(): List<StepNode> {
    val steps = mutableListOf<StepNode>()

    // Expect indent
    advanceIf(TokenType.INDENT)

    while (!isAtEnd() && current().type !in toplevelKeywords) {
        val stepKeyword =
            when (current().type) {
                TokenType.GIVEN -> {
                    StepKeyword.GIVEN
                }

                TokenType.WHEN -> {
                    StepKeyword.WHEN
                }

                TokenType.THEN -> {
                    StepKeyword.THEN
                }

                TokenType.AND -> {
                    StepKeyword.AND
                }

                TokenType.BUT -> {
                    StepKeyword.BUT
                }

                else -> {
                    val token = current().value
                    addError<String>("Unexpected step token $token")
                    advanceLine()
                    continue
                }
            }
        steps.add(parseStep(stepKeyword))
    }

    // Consume dedent if present
    advanceIf(TokenType.DEDENT)

    return steps
}

/**
 * Parse a single step with its keyword and actions.
 */
internal fun ParserState.parseStep(keyword: StepKeyword): StepNode {
    val loc = currentLocation()
    advance() // consume keyword
    skipWhitespace()

    val description = parseStepDescription()
    skipNewlines()

    val actions = parseActions()

    return StepNode(
        keyword = keyword,
        description = description,
        actions = actions,
        location = loc,
    )
}

/**
 * Parse step description text.
 * Preserves quotes for STRING tokens so custom step matchers can extract parameters.
 */
internal fun ParserState.parseStepDescription(): String = parseInlineTextUntilLineEnd()

/**
 * Parse actions within a step.
 */
internal fun ParserState.parseActions(): List<ActionNode> = parseBlockActions(allowNestedConditionals = true)
