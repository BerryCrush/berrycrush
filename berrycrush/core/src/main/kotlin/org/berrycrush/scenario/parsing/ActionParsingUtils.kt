package org.berrycrush.scenario.parsing

import org.berrycrush.scenario.ActionNode
import org.berrycrush.scenario.ParserState
import org.berrycrush.scenario.TokenType

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
@Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
internal fun ParserState.parseBlockActions(allowNestedConditionals: Boolean): List<ActionNode> {
    val actions = mutableListOf<ActionNode>()

    if (current().type == TokenType.INDENT) {
        advance()

        while (!isAtEnd()) {
            when (current().type) {
                TokenType.CALL -> parseCallAction()?.let { actions.add(it) }
                TokenType.EXTRACT -> parseExtractAction()?.let { actions.add(it) }
                TokenType.ASSERT -> parseAssertAction()?.let { actions.add(it) }
                TokenType.INCLUDE -> parseIncludeAction()?.let { actions.add(it) }
                TokenType.FAIL -> actions.add(parseFailAction())
                TokenType.IF -> {
                    if (allowNestedConditionals) {
                        parseConditional()?.let { actions.add(it) }
                    } else {
                        break
                    }
                }
                TokenType.NEWLINE -> advance()
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
                -> break
                else -> advance()
            }
        }

        if (current().type == TokenType.DEDENT) {
            advance()
        }
    }

    return actions
}
