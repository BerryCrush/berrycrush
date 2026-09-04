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
class Parser private constructor(
    private val state: ParserState,
) {
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
            return Parser(ParserState.forScenario(tokens, fileName)).parse()
        }

        fun parseFragment(
            source: String,
            fileName: String? = null,
        ): FragmentParserResult {
            val lexer = Lexer(source, fileName)
            val tokens = lexer.tokenize()
            return Parser(ParserState.forFragment(tokens, fileName)).parseFragment()
        }
    }

    sealed interface ParsingResult<T : FileNode> {
        val ast: T?
        val errors: List<ParseError>
        val isSuccess: Boolean
            get() = errors.isEmpty() && ast != null
    }

    /**
     * Result of parsing.
     */
    data class ParserResult(
        override val ast: ScenarioFileNode?,
        override val errors: List<ParseError>,
    ) : ParsingResult<ScenarioFileNode>

    data class FragmentParserResult(
        override val ast: FragmentFileNode?,
        override val errors: List<ParseError>,
    ) : ParsingResult<FragmentFileNode>

    fun parseFragment(): FragmentParserResult {
        val fragments = mutableListOf<FragmentNode>()
        val parameters = mutableListOf<ParametersNode>()
        val startLocation = state.currentLocation()

        state.skipNewlines()

        while (!state.isAtEnd()) {
            when (state.current().type) {
                TokenType.PARAMETERS -> {
                    state.parseParameters()?.let {
                        it.name?.let { _ -> parameters.add(it) }
                            ?: state.addError(
                                "Unnamed parameters block in fragment," +
                                    "parameters: <name>",
                            )
                    }
                }

                TokenType.FRAGMENT -> {
                    state.parseFragment()?.let(fragments::add)
                }

                TokenType.EOF -> {
                    break
                }

                TokenType.NEWLINE, TokenType.INDENT, TokenType.DEDENT -> {
                    state.advance()
                }

                else -> {
                    state.addError<Unit>(
                        "Unexpected token",
                        expected = "parameters or fragment",
                        found = state.current().value,
                    )
                    state.advance()
                }
            }
            state.skipNewlines()
        }

        val ast =
            if (state.errors.isEmpty()) {
                FragmentFileNode(fragments, parameters, startLocation)
            } else {
                null
            }

        return FragmentParserResult(ast, state.errors)
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
                TokenType.PARAMETERS -> {
                    parameters = state.parseParameters()
                }

                TokenType.SCENARIO -> {
                    state.parseScenario(tags)?.let(stories::add)
                }

                TokenType.OUTLINE -> {
                    state.parseScenarioOutline(tags)?.let(stories::add)
                }

                TokenType.FEATURE -> {
                    state.parseFeature(tags)?.let(stories::add)
                }

                TokenType.FRAGMENT -> {
                    state.parseFragment()?.let(fragments::add)
                }

                TokenType.EOF -> {
                    break
                }

                TokenType.NEWLINE, TokenType.INDENT, TokenType.DEDENT -> {
                    state.advance()
                }

                else -> {
                    state.addError<Unit>(
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
