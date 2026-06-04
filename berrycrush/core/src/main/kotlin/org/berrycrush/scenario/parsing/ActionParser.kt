@file:Suppress("MatchingDeclarationName", "TooManyFunctions")

package org.berrycrush.scenario.parsing

import org.berrycrush.scenario.AutoTestConfig
import org.berrycrush.scenario.AutoTestType
import org.berrycrush.scenario.BodyPropertyValue
import org.berrycrush.scenario.CallNode
import org.berrycrush.scenario.ExtractNode
import org.berrycrush.scenario.IncludeNode
import org.berrycrush.scenario.ParserState
import org.berrycrush.scenario.TokenType
import org.berrycrush.scenario.ValueNode
import org.berrycrush.scenario.WebhookNode
import org.berrycrush.scenario.WebhookScope

/**
 * Result of parsing body content.
 */
internal sealed class BodyParseResult {
    data class Raw(
        val value: ValueNode?,
    ) : BodyParseResult()

    data class Properties(
        val properties: Map<String, BodyPropertyValue>,
    ) : BodyParseResult()
}

/**
 * State holder for parsing call parameters.
 */
private class CallParseState {
    val parameters = mutableMapOf<String, ValueNode>()
    val headers = mutableMapOf<String, ValueNode>()
    var body: ValueNode? = null
    var bodyProperties: Map<String, BodyPropertyValue>? = null
    var bodyFile: String? = null
    var autoTestConfig: AutoTestConfig? = null
    var autoTestExcludes: Set<String>? = null

    fun finalAutoTestConfig(): AutoTestConfig? =
        if (autoTestConfig != null && autoTestExcludes != null) {
            AutoTestConfig(autoTestConfig!!.types, autoTestExcludes!!, autoTestConfig!!.location)
        } else {
            autoTestConfig
        }
}

/**
 * Parse a call action.
 */
internal fun ParserState.parseCallAction(): CallNode? {
    val loc = currentLocation()
    advance() // consume 'call'
    skipWhitespace()

    val specName = parseUsingClause()

    // Get operation ID
    if (current().type != TokenType.OPERATION_ID && current().type != TokenType.IDENTIFIER) {
        addError("Expected operation ID")
        return null
    }

    val operationId = current().value
    advance()

    val state = CallParseState()
    parseCallParametersBlock(state)

    return CallNode(
        operationId = operationId,
        specName = specName,
        parameters = state.parameters,
        headers = state.headers,
        body = state.body,
        bodyProperties = state.bodyProperties,
        bodyFile = state.bodyFile,
        autoTestConfig = state.finalAutoTestConfig(),
        location = loc,
    )
}

/**
 * Parse optional "using spec_name" clause.
 */
private fun ParserState.parseUsingClause(): String? {
    if (current().type != TokenType.USING) return null
    advance()
    skipWhitespace()
    return if (current().type == TokenType.IDENTIFIER || current().type == TokenType.STRING) {
        val specName = current().value
        advance()
        skipWhitespace()
        specName
    } else {
        null
    }
}

/**
 * Parse optional parameters block for a call.
 */
private fun ParserState.parseCallParametersBlock(state: CallParseState) {
    skipNewlines()
    if (current().type != TokenType.INDENT) return

    advance()
    while (!isAtEnd() && current().type != TokenType.DEDENT) {
        when (current().type) {
            TokenType.IDENTIFIER, TokenType.OPERATION_ID -> parseCallParameter(state)
            TokenType.NEWLINE -> advance()
            else -> advance()
        }
    }

    if (current().type == TokenType.DEDENT) {
        advance()
    }
}

/**
 * Parse a single call parameter and update state.
 */
private fun ParserState.parseCallParameter(state: CallParseState) {
    val paramName = current().value
    advance()
    skipWhitespace()

    if (current().type == TokenType.COLON || current().type == TokenType.EQUALS) {
        advance()
        skipWhitespace()
    }

    when {
        paramName.startsWith("header_") || paramName.startsWith("Header_") -> {
            val headerName = paramName.removePrefix("header_").removePrefix("Header_")
            parseValue()?.let { state.headers[headerName] = it }
        }
        paramName == "body" -> {
            when (val bodyResult = parseBodyContent()) {
                is BodyParseResult.Raw -> state.body = bodyResult.value
                is BodyParseResult.Properties -> state.bodyProperties = bodyResult.properties
            }
        }
        paramName == "bodyFile" -> state.bodyFile = parseBodyFilePath()
        paramName == "auto" -> state.autoTestConfig = parseAutoTestConfig()
        paramName == "excludes" -> state.autoTestExcludes = parseAutoTestExcludes()
        else -> parseValue()?.let { state.parameters[paramName] = it }
    }
}

/**
 * Parse body content (raw JSON, triple-quoted, or structured properties).
 */
internal fun ParserState.parseBodyContent(): BodyParseResult {
    // Check if there's a value on the same line (raw JSON or string)
    if (current().type != TokenType.NEWLINE && current().type != TokenType.EOF) {
        val value = parseValue()
        return BodyParseResult.Raw(value)
    }

    // Check for content after newline + indent
    skipNewlines()
    if (current().type == TokenType.INDENT) {
        advance()

        // Check if next token is a STRING (could be triple-quoted content from lexer)
        if (current().type == TokenType.STRING) {
            val value = parseValue()
            if (current().type == TokenType.DEDENT) {
                advance()
            }
            return BodyParseResult.Raw(value)
        }

        val properties = parseBodyProperties()
        if (current().type == TokenType.DEDENT) {
            advance()
        }
        return BodyParseResult.Properties(properties)
    }

    return BodyParseResult.Raw(null)
}

/**
 * Parse body properties recursively.
 */
internal fun ParserState.parseBodyProperties(): Map<String, BodyPropertyValue> {
    val properties = mutableMapOf<String, BodyPropertyValue>()

    while (!isAtEnd() && current().type != TokenType.DEDENT) {
        when (current().type) {
            TokenType.IDENTIFIER -> {
                val propName = current().value
                advance()
                skipWhitespace()

                if (current().type == TokenType.COLON) {
                    advance()
                    skipWhitespace()
                }

                // Check if this is a nested object (newline + indent)
                if (current().type == TokenType.NEWLINE) {
                    skipNewlines()
                    if (current().type == TokenType.INDENT) {
                        advance()
                        val nestedProps = parseBodyProperties()
                        if (current().type == TokenType.DEDENT) {
                            advance()
                        }
                        properties[propName] = BodyPropertyValue.Nested(nestedProps)
                    }
                } else {
                    val value = parseValue()
                    if (value != null) {
                        properties[propName] = BodyPropertyValue.Simple(value)
                    }
                }
            }
            TokenType.NEWLINE -> advance()
            else -> advance()
        }
    }

    return properties
}

/**
 * Parse a body file path.
 */
internal fun ParserState.parseBodyFilePath(): String? {
    if (current().type == TokenType.STRING) {
        val value = current().value
        advance()
        return value
    }

    val sb = StringBuilder()
    while (!isAtEnd() && current().type != TokenType.NEWLINE && current().type != TokenType.DEDENT) {
        when (current().type) {
            TokenType.IDENTIFIER, TokenType.OPERATION_ID -> sb.append(current().value)
            TokenType.COLON -> sb.append(':')
            TokenType.DOT -> sb.append('.')
            TokenType.NUMBER -> sb.append(current().value)
            else -> {
                val value = current().value
                if (value.isNotBlank() && value !in listOf("{", "}", "[", "]", "(", ")", ",", "|")) {
                    sb.append(value)
                }
            }
        }
        advance()
    }

    return sb.toString().takeIf { it.isNotBlank() }
}

/**
 * Parse auto test configuration.
 */
internal fun ParserState.parseAutoTestConfig(): AutoTestConfig? {
    val loc = currentLocation()
    val types = mutableSetOf<AutoTestType>()

    if (current().type != TokenType.OPEN_BRACKET) {
        // Parse as bare identifiers without brackets
        while (!isAtEnd() && current().type != TokenType.NEWLINE && current().type != TokenType.DEDENT) {
            if (current().type == TokenType.IDENTIFIER || current().type == TokenType.OPERATION_ID) {
                val typeName = current().value.lowercase()
                when (typeName) {
                    "invalid" -> types.add(AutoTestType.INVALID)
                    "security" -> types.add(AutoTestType.SECURITY)
                    "multi" -> types.add(AutoTestType.MULTI)
                }
            }
            advance()
        }
        return if (types.isNotEmpty()) AutoTestConfig(types, emptySet(), loc) else null
    }

    advance() // consume [

    while (!isAtEnd() && current().type != TokenType.CLOSE_BRACKET) {
        when (current().type) {
            TokenType.IDENTIFIER, TokenType.OPERATION_ID -> {
                val typeName = current().value.lowercase()
                when (typeName) {
                    "invalid" -> types.add(AutoTestType.INVALID)
                    "security" -> types.add(AutoTestType.SECURITY)
                    "multi" -> types.add(AutoTestType.MULTI)
                }
                advance()
            }
            TokenType.COMMA -> advance()
            else -> advance()
        }
    }

    if (current().type == TokenType.CLOSE_BRACKET) {
        advance()
    }

    return if (types.isNotEmpty()) AutoTestConfig(types, emptySet(), loc) else null
}

/**
 * Parse auto test excludes configuration.
 */
internal fun ParserState.parseAutoTestExcludes(): Set<String> {
    if (current().type != TokenType.OPEN_BRACKET) {
        val excludes = mutableSetOf<String>()
        if (current().type == TokenType.IDENTIFIER || current().type == TokenType.OPERATION_ID) {
            excludes.add(current().value)
            advance()
        }
        return excludes
    }
    return parseInlineList().toSet()
}

/**
 * Parse an extract action.
 */
internal fun ParserState.parseExtractAction(): ExtractNode? {
    val loc = currentLocation()
    advance() // consume 'extract'
    skipWhitespace()

    if (current().type != TokenType.JSON_PATH && current().type != TokenType.STRING) {
        addError("Expected JSON path")
        return null
    }
    val jsonPath = current().value
    advance()
    skipWhitespace()

    if (current().type != TokenType.ARROW) {
        addError("Expected '=>' or '->'")
        return null
    }
    advance()
    skipWhitespace()

    val current = current()
    val variableName =
        when (current.type) {
            TokenType.VARIABLE, TokenType.IDENTIFIER -> current.value
            else -> {
                addError("Expected variable name")
                return null
            }
        }
    advance()

    return ExtractNode(
        variableName = variableName,
        jsonPath = jsonPath,
        location = loc,
    )
}

/**
 * Parse an include action.
 */
internal fun ParserState.parseIncludeAction(): IncludeNode? {
    val loc = currentLocation()
    advance() // consume 'include'
    skipWhitespace()

    if (current().type != TokenType.IDENTIFIER && current().type != TokenType.OPERATION_ID) {
        addError("Expected fragment name")
        return null
    }

    val fragmentName = current().value
    advance()

    val parameters = parseIncludeParameters()

    return IncludeNode(
        fragmentName = fragmentName,
        parameters = parameters,
        location = loc,
    )
}

/**
 * Parse optional parameters block for an include directive.
 *
 * Example:
 * ```
 * include create_user
 *   name: John Doe
 *   email: john@example.com
 * ```
 */
private fun ParserState.parseIncludeParameters(): Map<String, ValueNode> {
    skipNewlines()
    if (current().type != TokenType.INDENT) return emptyMap()

    val parameters = mutableMapOf<String, ValueNode>()
    advance() // consume INDENT

    while (!isAtEnd() && current().type != TokenType.DEDENT) {
        when (current().type) {
            TokenType.IDENTIFIER, TokenType.OPERATION_ID -> {
                val paramName = current().value
                advance()
                skipWhitespace()

                if (current().type == TokenType.COLON || current().type == TokenType.EQUALS) {
                    advance()
                    skipWhitespace()
                }

                parseValue()?.let { parameters[paramName] = it }
            }
            TokenType.NEWLINE -> advance()
            else -> advance()
        }
    }

    if (current().type == TokenType.DEDENT) {
        advance()
    }

    return parameters
}

/**
 * Parse a webhook action.
 *
 * Example:
 * ```
 * webhook:
 *   name: status-server
 *   port: 0
 *   hook: markStatus
 *
 * webhook:
 *   name: multi-hook-server
 *   port: 8080
 *   hooks:
 *     - onCreated
 *     - onUpdated
 * ```
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
internal fun ParserState.parseWebhookAction(): WebhookNode? {
    val loc = currentLocation()
    advance() // consume 'webhook'
    skipWhitespace()

    // Expect colon after webhook keyword
    if (current().type == TokenType.COLON) {
        advance()
    }

    skipWhitespace()

    // Parse the webhook name (right after the colon on the same line)
    var name = parseStringOrIdentifier()

    skipNewlines()
    if (current().type != TokenType.INDENT) {
        if (name == null) {
            addError("Expected webhook name or indented webhook configuration")
        } else {
            // No indented block - error if we don't have hooks
            addError("webhook requires 'hook' or 'hooks' property", loc)
        }
        return null
    }

    advance() // consume INDENT

    var port = 0
    val hooks = mutableListOf<String>()
    var scope = WebhookScope.SCENARIO

    while (!isAtEnd() && current().type != TokenType.DEDENT) {
        when (current().type) {
            TokenType.IDENTIFIER, TokenType.OPERATION_ID -> {
                val propName = current().value.lowercase()
                advance()
                skipWhitespace()

                if (current().type == TokenType.COLON || current().type == TokenType.EQUALS) {
                    advance()
                    skipWhitespace()
                }

                when (propName) {
                    "name" -> {
                        // Allow name in block too (override if already set on same line)
                        name = parseStringOrIdentifier()
                    }
                    "port" -> {
                        port = parseIntValue() ?: 0
                    }
                    "hook" -> {
                        parseStringOrIdentifier()?.let { hooks.add(it) }
                    }
                    "hooks" -> {
                        hooks.addAll(parseHookList())
                    }
                    "scope" -> {
                        val scopeValue = parseStringOrIdentifier()?.lowercase()
                        scope =
                            when (scopeValue) {
                                "feature" -> WebhookScope.FEATURE
                                else -> WebhookScope.SCENARIO
                            }
                    }
                }
            }
            TokenType.NEWLINE -> advance()
            else -> advance()
        }
    }

    if (current().type == TokenType.DEDENT) {
        advance()
    }
    return when {
        name == null -> {
            addError("webhook requires 'name' property", loc)
            null
        }
        hooks.isEmpty() -> {
            addError("webhook requires 'hook' or 'hooks' property", loc)
            null
        }
        else ->
            WebhookNode(
                name = name,
                port = port,
                hooks = hooks,
                scope = scope,
                location = loc,
            )
    }
}

/**
 * Parse a string or identifier value.
 * Also accepts keywords that might be used as values (like "feature" for scope).
 */
private fun ParserState.parseStringOrIdentifier(): String? =
    when (current().type) {
        TokenType.STRING,
        TokenType.IDENTIFIER, TokenType.OPERATION_ID,
        // Keywords that can be used as values
        TokenType.FEATURE, TokenType.SCENARIO,
        -> {
            val value = current().value
            advance()
            value
        }
        else -> null
    }

/**
 * Parse an integer value.
 */
private fun ParserState.parseIntValue(): Int? =
    when (current().type) {
        TokenType.NUMBER -> {
            val value = current().value.toIntOrNull()
            advance()
            value
        }
        else -> null
    }

/**
 * Parse a list of hooks.
 *
 * Supports both inline and multi-line list formats:
 * ```
 * hooks: [hook1, hook2]
 *
 * hooks:
 *   - hook1
 *   - hook2
 * ```
 */
private fun ParserState.parseHookList(): List<String> {
    // Check for inline list [hook1, hook2]
    if (current().type == TokenType.OPEN_BRACKET) {
        return parseInlineList()
    }

    val hooks = mutableListOf<String>()
    // Check for multi-line list with dashes
    skipNewlines()
    if (current().type == TokenType.INDENT) {
        advance()
        while (!isAtEnd() && current().type != TokenType.DEDENT) {
            // Look for "- hookName" pattern
            when (current().type) {
                TokenType.IDENTIFIER if current().value == "-" -> {
                    advance()
                    skipWhitespace()
                    parseStringOrIdentifier()?.let { hooks.add(it) }
                }
                TokenType.STRING, TokenType.IDENTIFIER, TokenType.OPERATION_ID -> {
                    // Also accept bare identifiers (for YAML-style list items parsed differently)
                    val value = current().value
                    if (value != "-") {
                        hooks.add(value)
                    }
                    advance()
                }
                TokenType.NEWLINE -> advance()
                else -> advance()
            }
        }
        if (current().type == TokenType.DEDENT) {
            advance()
        }
    }

    return hooks
}

private fun ParserState.parseInlineList(): List<String> {
    val elements = mutableListOf<String>()
    advance() // consume [
    while (!isAtEnd() && current().type != TokenType.CLOSE_BRACKET) {
        when (current().type) {
            TokenType.STRING, TokenType.IDENTIFIER, TokenType.OPERATION_ID -> {
                elements.add(current().value)
                advance()
            }
            TokenType.COMMA -> advance()
            else -> advance()
        }
    }
    if (current().type == TokenType.CLOSE_BRACKET) {
        advance()
    }
    return elements
}
