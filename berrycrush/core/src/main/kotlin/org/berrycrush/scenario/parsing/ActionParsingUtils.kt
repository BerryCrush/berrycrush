package org.berrycrush.scenario.parsing

import org.berrycrush.scenario.ActionNode
import org.berrycrush.scenario.ParserState
import org.berrycrush.scenario.TokenType

private val blockEndKeywords =
    listOf(
        TokenType.DEDENT,
        TokenType.ELSE,
        TokenType.GIVEN,
        TokenType.WHEN,
        TokenType.THEN,
        TokenType.AND,
        TokenType.BUT,
        TokenType.SCENARIO,
        TokenType.OUTLINE,
        TokenType.FRAGMENT,
        TokenType.EXAMPLES,
        TokenType.EOF,
    )

/**
 * Parse actions within an indented block.
 *
 * This is a shared helper used by both step parsing and conditional parsing.
 * The [allowNestedConditionals] parameter determines whether IF tokens should
 * trigger nested conditional parsing.
 *
 * @param allowNestedConditionals If true, IF tokens will trigger [parseConditional] calls.
 *                                 If false, IF tokens will cause the loop to break.
 * @return List of parsed action nodes
 */
@Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
internal fun ParserState.parseBlockActions(allowNestedConditionals: Boolean): List<ActionNode> {
    val actions = mutableListOf<ActionNode>()

    if (current().type == TokenType.INDENT) {
        advance()

        while (!isAtEnd() && current().type !in blockEndKeywords) {
            when (current().type) {
                TokenType.CALL -> {
                    parseCallAction()?.let { actions.add(it) }
                }

                TokenType.EXTRACT -> {
                    parseExtractAction()?.let { actions.add(it) }
                }

                TokenType.ASSERT -> {
                    parseAssertAction()?.let { actions.add(it) }
                }

                TokenType.INCLUDE -> {
                    parseIncludeAction()?.let { actions.add(it) }
                }

                TokenType.FAIL -> {
                    actions.add(parseFailAction())
                }

                TokenType.WEBHOOK -> {
                    parseWebhookAction()?.let { actions.add(it) }
                }

                TokenType.IF -> {
                    if (allowNestedConditionals) {
                        parseConditional()?.let { actions.add(it) }
                    } else {
                        break
                    }
                }

                TokenType.NEWLINE -> {
                    advance()
                }

                else -> {
                    // In case of custom assertion, we can't set error here
                    advance()
                }
            }
        }

        if (current().type == TokenType.DEDENT) {
            advance()
        }
    }

    return actions
}
