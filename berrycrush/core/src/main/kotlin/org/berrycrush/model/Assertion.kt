package org.berrycrush.model

import org.berrycrush.context.TestExecutionContext

/**
 * Represents any assertion variant that can be attached to a step.
 *
 * This sealed contract unifies built-in assertions, conditionals, and
 * programmatic custom assertions under one model type.
 */
sealed interface Assertion {
    /**
     * Source location for parser-originated assertions.
     */
    val sourceLocation: SourceLocation?

    /**
     * Built-in assertion based on a parsed/evaluated [Condition].
     */
    data class BuiltinAssertion(
        val condition: Condition,
        val description: String? = null,
        override val sourceLocation: SourceLocation? = null,
    ) : Assertion

    /**
     * Conditional assertion structure (`if` / `else if` / `else`).
     */
    data class ConditionalAssertion(
        val ifBranch: ConditionBranch,
        val elseIfBranches: List<ConditionBranch> = emptyList(),
        val elseActions: ConditionalActions? = null,
        override val sourceLocation: SourceLocation? = null,
    ) : Assertion {
        val branches: List<ConditionBranch> by lazy {
            listOf(ifBranch) + elseIfBranches
        }
    }

    /**
     * Programmatic custom assertion with access to test execution context.
     */
    data class CustomAssertion(
        val description: String,
        val assertion: (TestExecutionContext) -> Unit,
        override val sourceLocation: SourceLocation? = null,
    ) : Assertion
}
