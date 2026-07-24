package org.berrycrush.model

import org.berrycrush.context.TestExecutionContext
import java.time.Duration

typealias ConditionalAssertion = Assertion.ConditionalAssertion

/**
 * A condition with associated actions.
 *
 * @property condition The condition to evaluate
 * @property actions Actions to execute if condition is true
 */
data class ConditionBranch(
    val condition: Condition,
    val actions: ConditionalActions,
)

/**
 * Actions within a conditional branch.
 *
 * @property assertions Standard assertions to run
 * @property extractions Variables to extract
 * @property failMessage If set, fail with this message instead of running assertions
 * @property nestedConditionals Nested conditional assertions
 */
data class ConditionalActions(
    val assertions: List<Assertion> = emptyList(),
    val extractions: List<Extraction> = emptyList(),
    val failMessage: String? = null,
    val nestedConditionals: List<ConditionalAssertion> = emptyList(),
)

/**
 * Represents a condition to evaluate against the response.
 */
sealed interface Condition {
    fun toAssertion(
        description: String? = null,
        sourceLocation: SourceLocation? = null,
    ): Assertion = Assertion.BuiltinAssertion(condition = this, description = description, sourceLocation = sourceLocation)

    /**
     * Status code condition.
     *
     * @property expected Expected status code (number) or pattern (e.g., "2xx", "200-299")
     */
    data class Status(
        val expected: Any,
    ) : Condition

    /**
     * JSON path condition.
     *
     * @property path JSONPath expression
     * @property operator Comparison operator
     * @property expected Expected value for comparison
     */
    data class JsonPath(
        val path: String,
        val operator: ConditionOperator,
        val expected: Any? = null,
    ) : Condition

    /**
     * Header condition.
     *
     * @property name Header name
     * @property operator Comparison operator
     * @property expected Expected value for comparison
     */
    data class Header(
        val name: String,
        val operator: ConditionOperator,
        val expected: Any? = null,
    ) : Condition

    /**
     * Variable condition.
     *
     * @property name Variable name (e.g., "test.type")
     * @property operator Comparison operator
     * @property expected Expected value for comparison
     */
    data class Variable(
        val name: String,
        val operator: ConditionOperator,
        val expected: Any? = null,
    ) : Condition

    /**
     * Negated condition.
     *
     * @property condition The condition to negate
     */
    data class Negated(
        val condition: Condition,
    ) : Condition

    /**
     * Compound condition with logical operator.
     *
     * @property left Left operand
     * @property operator Logical operator (AND/OR)
     * @property right Right operand
     */
    data class Compound(
        val left: Condition,
        val operator: LogicalOperator,
        val right: Condition,
    ) : Condition

    /**
     * Body contains condition (assertion-specific).
     *
     * @property text The text that the body should contain
     */
    data class BodyContains(
        val text: Any,
    ) : Condition

    /**
     * Schema validation condition (assertion-specific).
     * Validates response against OpenAPI schema.
     */
    data object Schema : Condition

    /**
     * Response time condition (assertion-specific).
     *
     * @property duration Maximum response time
     */
    data class ResponseTime(
        val duration: Any,
    ) : Condition {
        constructor(maxMs: Long) : this(Duration.ofMillis(maxMs))
    }

    /**
     * Custom assertion condition (matched against AssertionRegistry).
     *
     * @property pattern The custom assertion pattern to match
     */
    data class CustomAssertion(
        val pattern: String,
    ) : Condition

    /**
     * Programmatic condition using a predicate function.
     *
     * Used by the Kotlin DSL for dynamic conditional logic.
     *
     * @property predicate Function that evaluates the condition against the test context
     */
    data class Custom(
        val predicate: (TestExecutionContext) -> Boolean,
    ) : Condition
}

/**
 * Logical operators for compound conditions.
 */
enum class LogicalOperator {
    AND,
    OR,
}

/**
 * Operators for condition comparisons.
 */
enum class ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    MATCHES,
    EXISTS,
    NOT_EXISTS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN,
    LESS_THAN_OR_EQUALS,

    /** Array/string size check */
    HAS_SIZE,

    /** Array/string not empty check */
    EMPTY,
    NOT_EMPTY,
}
