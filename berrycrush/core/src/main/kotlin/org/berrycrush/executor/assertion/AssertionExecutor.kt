package org.berrycrush.executor.assertion

import org.berrycrush.context.MutableTestExecutionContext
import org.berrycrush.context.ValueExtractor
import org.berrycrush.model.Assertion
import org.berrycrush.model.AssertionResult
import org.berrycrush.model.Condition
import org.berrycrush.model.ConditionalActions
import org.berrycrush.model.ConditionalAssertion
import org.berrycrush.model.CustomAssertionDefinition
import org.berrycrush.model.Step
import org.berrycrush.plugin.HttpResponse
import org.berrycrush.plugin.StepContext
import org.berrycrush.plugin.adapter.StepOperationAdapter

class AssertionExecutor(
    private val assertionEngine: AssertionEngine,
) {
    fun runAssertions(
        response: HttpResponse,
        assertions: List<Assertion>,
        context: StepContext,
    ): List<AssertionResult> = assertions.map { assertion -> runAssertion(response, assertion, context) }

    /**
     * Run a single assertion using the AssertionEngine.
     *
     * Delegates condition evaluation and message generation to the assertion engine,
     * ensuring consistent behavior between `assert` and `if` conditions.
     */
    private fun runAssertion(
        response: HttpResponse,
        assertion: Assertion,
        context: StepContext,
    ): AssertionResult {
        val assertionContext = buildAssertionContext(response, context)
        val result = assertionEngine.evaluate(assertion.condition, assertionContext)

        return AssertionResult(
            assertion = assertion,
            passed = result.passed,
            message = result.message,
            actual = result.actual,
        )
    }

    /**
     * Run conditional assertions against the response.
     *
     * Evaluates each conditional's conditions in order and runs the actions
     * for the first matching branch.
     */
    fun runConditionals(
        response: HttpResponse,
        conditionals: List<ConditionalAssertion>,
        context: StepContext,
    ): ConditionalRunResult {
        val allResults = mutableListOf<AssertionResult>()
        val allExtracted = mutableMapOf<String, Any?>()
        var failMessage: String? = null

        for (conditional in conditionals) {
            val result = runConditional(response, conditional, context)
            allResults.addAll(result.assertionResults)
            allExtracted.putAll(result.extractedValues)
            if (result.failMessage != null) {
                failMessage = result.failMessage
                break // Stop on first fail
            }
        }

        return ConditionalRunResult(
            assertionResults = allResults,
            extractedValues = allExtracted,
            failMessage = failMessage,
        )
    }

    /**
     * Run a single conditional assertion.
     */
    private fun runConditional(
        response: HttpResponse,
        conditional: ConditionalAssertion,
        context: StepContext,
    ): ConditionalRunResult {
        val assertionContext = buildAssertionContext(response, context)
        // Try branches first
        for (branch in conditional.branches) {
            if (assertionEngine.evaluate(branch.condition, assertionContext).passed) {
                return runConditionalActions(response, branch.actions, context)
            }
        }

        // Run else branch if present
        return if (conditional.elseActions != null) {
            runConditionalActions(response, conditional.elseActions, context)
        } else {
            // No branch matched - that's OK, no assertions to run
            ConditionalRunResult()
        }
    }

    /**
     * Run actions within a conditional branch.
     */
    private fun runConditionalActions(
        response: HttpResponse,
        actions: ConditionalActions,
        context: StepContext,
    ): ConditionalRunResult {
        // Check for fail first
        if (actions.failMessage != null) {
            return ConditionalRunResult(failMessage = actions.failMessage)
        }

        val assertionResults = mutableListOf<AssertionResult>()
        val extractedValues = mutableMapOf<String, Any?>()

        // Run extractions
        for (extraction in actions.extractions) {
            val value =
                runCatching {
                    val body = response.body ?: ""
                    ValueExtractor.extract(body, extraction)
                }.getOrNull()
            value?.let { context[extraction.variableName] = it }
            extractedValues[extraction.variableName] = value
        }

        // Run assertions
        assertionResults.addAll(runAssertions(response, actions.assertions, context))

        // Run nested conditionals
        for (nested in actions.nestedConditionals) {
            val nestedResult = runConditional(response, nested, context)
            assertionResults.addAll(nestedResult.assertionResults)
            extractedValues.putAll(nestedResult.extractedValues)
            if (nestedResult.failMessage != null) {
                return ConditionalRunResult(
                    assertionResults = assertionResults,
                    extractedValues = extractedValues,
                    failMessage = nestedResult.failMessage,
                )
            }
        }

        return ConditionalRunResult(
            assertionResults = assertionResults,
            extractedValues = extractedValues,
        )
    }

    /**
     * Run custom assertions defined via DSL assert blocks.
     *
     * Custom assertions receive a TestExecutionContext and can throw any exception
     * (including AssertionError from require/check/assert) to indicate failure.
     */
    fun runCustomAssertions(
        customAssertions: List<CustomAssertionDefinition>,
        context: StepContext,
    ): List<AssertionResult> =
        customAssertions.map { customAssertion ->
            runCustomAssertion(customAssertion, context)
        }

    /**
     * Run a single custom assertion.
     */
    private fun runCustomAssertion(
        customAssertion: CustomAssertionDefinition,
        context: StepContext,
    ): AssertionResult {
        val testContext = MutableTestExecutionContext(context)
        val assertion =
            Assertion(
                condition = Condition.CustomAssertion(customAssertion.description),
                description = customAssertion.description,
            )
        return runCatching {
            customAssertion.assertion(testContext)
            AssertionResult(
                assertion = assertion,
                passed = true,
                message = "Custom assertion passed: ${customAssertion.description}",
            )
        }.getOrElse { e ->
            // Unwrap AssertionFailureException if present
            AssertionResult(
                assertion = assertion,
                passed = false,
                message = e.message ?: "Custom assertion failed: ${customAssertion.description}",
                actual = e.message,
            )
        }
    }

    data class ConditionalRunResult(
        val assertionResults: List<AssertionResult> = emptyList(),
        val extractedValues: Map<String, Any?> = emptyMap(),
        val failMessage: String? = null,
    )

    /**
     * Check if a step contains any custom assertions.
     */
    fun hasCustomAssertion(step: Step): Boolean {
        // Check direct assertions
        if (step.assertions.any { it.condition is Condition.CustomAssertion }) {
            return true
        }
        // Check conditional assertions
        return step.conditionals.any { conditional ->
            hasCustomAssertionInConditional(conditional)
        }
    }

    /**
     * Check if a conditional contains any custom assertions.
     */
    private fun hasCustomAssertionInConditional(conditional: ConditionalAssertion): Boolean {
        fun ConditionalAssertion.checkBranches() =
            branches.any { branch ->
                branch.actions.assertions.any { it.condition is Condition.CustomAssertion }
            }

        fun ConditionalAssertion.checkElse() = elseActions?.assertions?.any { it.condition is Condition.CustomAssertion } == true

        fun ConditionalAssertion.checkNested() =
            branches.any { branch ->
                branch.actions.nestedConditionals.any { hasCustomAssertionInConditional(it) }
            } ||
                (elseActions?.nestedConditionals?.any { hasCustomAssertionInConditional(it) } == true)
        return conditional.checkBranches() || conditional.checkElse() || conditional.checkNested()
    }

    private fun buildAssertionContext(
        response: HttpResponse,
        context: StepContext,
    ): AssertionContext =
        AssertionContext(
            response = response,
            responseTime = context.responseTime,
            variables = context.allVariables(),
            stepContext = context,
            currentOperation =
                context.operation?.let {
                    if (it is StepOperationAdapter) {
                        it.operation
                    } else {
                        null
                    }
                },
        )
}
