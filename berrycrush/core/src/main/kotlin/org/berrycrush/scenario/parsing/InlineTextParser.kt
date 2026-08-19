package org.berrycrush.scenario.parsing

import org.berrycrush.scenario.ParserState
import org.berrycrush.scenario.TokenType

private val NO_LEADING_SPACE_BEFORE =
    setOf(
        TokenType.COLON,
        TokenType.COMMA,
        TokenType.DOT,
        TokenType.CLOSE_PAREN,
        TokenType.CLOSE_BRACE,
        TokenType.CLOSE_BRACKET,
    )

private val NO_TRAILING_SPACE_AFTER =
    setOf(
        TokenType.OPEN_PAREN,
        TokenType.OPEN_BRACE,
        TokenType.OPEN_BRACKET,
        TokenType.DOT,
    )

/**
 * Parse free-form inline text until the end of the current line.
 *
 * This keeps punctuation attachment natural (for example `name: value`
 * instead of `name : value`) while still normalizing repeated whitespace.
 */
internal fun ParserState.parseInlineTextUntilLineEnd(): String {
    val builder = StringBuilder()
    var previousType: TokenType? = null

    while (!isAtEnd() && current().type != TokenType.NEWLINE && current().type != TokenType.EOF) {
        val token = advance()
        val currentType = token.type
        val tokenText = token.toInlineText()

        if (builder.isNotEmpty() && shouldInsertSpace(previousType, currentType)) {
            builder.append(' ')
        }

        builder.append(tokenText)
        previousType = currentType
    }

    return builder.toString().trim()
}

private fun shouldInsertSpace(
    previousType: TokenType?,
    currentType: TokenType,
): Boolean =
    previousType != null &&
        previousType !in NO_TRAILING_SPACE_AFTER &&
        currentType !in NO_LEADING_SPACE_BEFORE

private fun org.berrycrush.scenario.Token.toInlineText(): String =
    when (type) {
        TokenType.STRING -> "\"$value\""
        TokenType.VARIABLE -> "{{$value}}"
        else -> value
    }
