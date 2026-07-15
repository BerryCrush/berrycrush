package org.berrycrush.scenario

import org.berrycrush.scenario.parsing.parseFeature
import org.berrycrush.scenario.parsing.parseFragment
import org.berrycrush.scenario.parsing.parseParameters
import org.berrycrush.scenario.parsing.parseScenario
import org.berrycrush.scenario.parsing.parseScenarioOutline
import org.berrycrush.scenario.parsing.parseTags

/**
 * Parser for scenario files.
 *
 * Parses tokenized scenario files into an AST.
 * Supports:
 * - Scenarios and scenario outlines
 * - Features with optional parameters and background
 * - Fragments
 * - Given/When/Then steps
 * - API calls, assertions, extractions
 * - Examples tables for parameterization
 * - Conditional logic (if/else if/else)
 *
 * The parser delegates to extension functions in the `parsing` package
 * for specific parsing tasks, keeping this class focused on coordination.
 */
class Parser(
    tokens: List<Token>,
    fileName: String? = null,
) {
    private val state = ParserState(tokens, fileName)

    companion object {
        /**
         * Parse source code directly.
         */
        fun parse(
            source: String,
            fileName: String? = null,
        ): ParserResult {
            val lexer = Lexer(source, fileName)
            val tokens = lexer.tokenize()
            return Parser(tokens, fileName).parse()
        }
    }

    /**
     * Result of parsing.
     */
    data class ParserResult(
        val ast: ScenarioFileNode?,
        val errors: List<ParseError>,
    ) {
        val isSuccess: Boolean get() = errors.isEmpty() && ast != null
    }

    /**
     * Parse the token stream into an AST.
     */
    fun parse(): ParserResult {
        val stories = mutableListOf<StoryNode>()
        val fragments = mutableListOf<FragmentNode>()
        var parameters: ParametersNode? = null
        val startLocation = state.currentLocation()

        state.skipNewlines()

        while (!state.isAtEnd()) {
            // Collect tags before scenarios or features
            val tags = state.parseTags()

            when (state.current().type) {
                TokenType.PARAMETERS -> parameters = state.parseParameters()
                TokenType.SCENARIO -> state.parseScenario(tags)?.let(stories::add)
                TokenType.OUTLINE -> state.parseScenarioOutline(tags)?.let(stories::add)
                TokenType.FEATURE -> state.parseFeature(tags)?.let(stories::add)
                TokenType.FRAGMENT -> state.parseFragment()?.let(fragments::add)
                TokenType.EOF -> break
                TokenType.NEWLINE, TokenType.INDENT, TokenType.DEDENT -> state.advance()

                else -> {
                    state.addError(
                        "Unexpected token",
                        expected = "parameters, scenario, outline, feature, or fragment",
                        found = state.current().value,
                    )
                    state.advance()
                }
            }
            state.skipNewlines()
        }

        val ast =
            if (state.errors.isEmpty()) {
                ScenarioFileNode(stories, fragments, parameters, startLocation)
            } else {
                null
            }

        return ParserResult(ast, state.errors)
    }
}
