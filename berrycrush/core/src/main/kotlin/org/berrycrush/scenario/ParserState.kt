package org.berrycrush.scenario

import org.berrycrush.model.SourceLocation

/**
 * Holds the shared state for parsing scenario files.
 *
 * This class is used by extension functions in the `parsing` package
 * to access tokens, track position, and accumulate errors.
 */
@Suppress("TooManyFunctions")
sealed interface ParserState {
    val tokens: List<Token>
    val fileName: String?

    /** Current position in the token stream */
    var pos: Int

    /** Accumulated parse errors */
    val errors: MutableList<ParseError>

    companion object {
        fun forScenario(
            tokens: List<Token>,
            fileName: String? = null,
        ): ParserState = ScenarioParserState(tokens, fileName)

        fun forFragment(
            tokens: List<Token>,
            fileName: String? = null,
        ): ParserState = FragmentParserState(tokens, fileName)
    }

    /**
     * Get the current token, or the last token if at end.
     */
    fun current(): Token = if (isAtEnd()) tokens.last() else tokens[pos]

    /**
     * Get the current source location.
     */
    fun currentLocation(): SourceLocation = current().location

    /**
     * Advance to the next token and return the previous one.
     */
    fun advance(): Token {
        if (!isAtEnd()) pos++
        return tokens[pos - 1]
    }

    fun advanceIf(type: TokenType): Token =
        if (current().type == type) {
            advance()
        } else {
            current()
        }

    /**
     * Retreat to the previous token.
     * Used when we need to "unread" a token after peeking ahead.
     */
    fun retreat(): Token {
        if (pos > 0) pos--
        return tokens[pos]
    }

    /**
     * Check if we've reached the end of input.
     */
    fun isAtEnd(): Boolean = pos >= tokens.size || tokens[pos].type == TokenType.EOF

    /**
     * Expect and consume a specific token type.
     * If the current token doesn't match, adds an error and returns false.
     */
    fun expect(type: TokenType): Boolean {
        if (current().type == type) {
            advance()
            return true
        }
        errors.add(
            ParseError(
                "Unexpected token",
                currentLocation(),
                expected = type.name,
                found = current().type.name,
            ),
        )
        return false
    }

    /**
     * Skip whitespace tokens (but not indent tokens as they're significant).
     */
    @Suppress("UnconditionalJumpStatementInLoop")
    fun skipWhitespace() {
        while (!isAtEnd() && (current().type == TokenType.INDENT)) {
            // Don't skip indent tokens - they're significant
            break
        }
    }

    /**
     * Skip newline tokens.
     */
    fun skipNewlines() {
        while (!isAtEnd() && current().type == TokenType.NEWLINE) {
            advance()
        }
    }

    /**
     * advance line
     */
    fun advanceLine() {
        while (!isAtEnd() && current().type != TokenType.NEWLINE) {
            advance()
        }
        advanceIf(TokenType.NEWLINE)
    }

    /**
     * Add a parse error.
     */
    fun <T> addError(
        message: String,
        location: SourceLocation = currentLocation(),
        expected: String? = null,
        found: String? = null,
    ): T? {
        errors.add(ParseError(message, location, expected, found))
        return null
    }
}

internal data class ScenarioParserState(
    override val tokens: List<Token>,
    override val fileName: String?,
    override var pos: Int = 0,
    override val errors: MutableList<ParseError> = mutableListOf(),
) : ParserState

internal data class FragmentParserState(
    override val tokens: List<Token>,
    override val fileName: String?,
    override var pos: Int = 0,
    override val errors: MutableList<ParseError> = mutableListOf(),
) : ParserState
