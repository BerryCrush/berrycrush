package org.berrycrush.plugin.adapter

import org.berrycrush.model.Assertion
import org.berrycrush.model.Condition
import org.berrycrush.model.HttpResponse
import org.berrycrush.plugin.AssertionFailure
import org.berrycrush.plugin.ResultStatus
import org.berrycrush.plugin.StepResult
import java.time.Duration
import org.berrycrush.model.StepResult as ModelStepResult

/**
 * Adapter that bridges model [ModelStepResult] with plugin [org.berrycrush.plugin.StepResult] interface.
 */
class StepResultAdapter(
    private val modelResult: ModelStepResult,
) : StepResult {
    override val status: ResultStatus
        get() = modelResult.status.mapTo()

    override val duration: Duration
        get() = modelResult.duration

    override val stepDescription: String
        get() = modelResult.step.description

    override val response: HttpResponse?
        get() = modelResult.response

    override val failure: AssertionFailure?
        get() =
            modelResult.assertionResults
                .firstOrNull { !it.passed }
                ?.let { failedAssertion ->
                    AssertionFailure(
                        message = failedAssertion.message,
                        expected = getExpectedFromAssertion(failedAssertion.assertion),
                        actual = failedAssertion.actual,
                        diff = null,
                        stepDescription = modelResult.step.description,
                        assertionType = getAssertionTypeName(failedAssertion.assertion),
                        requestSnapshot = null,
                        responseSnapshot = null,
                    )
                }

    override val error: Throwable?
        get() = modelResult.error

    override val isCustomStep: Boolean
        get() = modelResult.isCustomStep

    private fun getExpectedFromAssertion(assertion: Assertion): Any? =
        when (assertion) {
            is Assertion.BuiltinAssertion -> getExpectedFromCondition(assertion.condition)
            is Assertion.CustomAssertion -> assertion.description
            is Assertion.ConditionalAssertion -> "conditional"
        }

    private fun getAssertionTypeName(assertion: Assertion): String =
        when (assertion) {
            is Assertion.BuiltinAssertion -> getConditionTypeName(assertion.condition)
            is Assertion.CustomAssertion -> "CUSTOM"
            is Assertion.ConditionalAssertion -> "CONDITIONAL"
        }

    /**
     * Extract expected value from a Condition for error reporting.
     */
    private fun getExpectedFromCondition(condition: Condition): Any? =
        when (condition) {
            is Condition.Status -> condition.expected
            is Condition.JsonPath -> condition.expected
            is Condition.Header -> condition.expected
            is Condition.BodyContains -> condition.text
            is Condition.ResponseTime -> condition.duration
            is Condition.Variable -> condition.expected
            is Condition.Negated -> getExpectedFromCondition(condition.condition)
            is Condition.Compound -> null
            is Condition.Schema -> "schema"
            is Condition.CustomAssertion -> condition.pattern
            is Condition.Custom -> "<predicate>"
        }

    /**
     * Get a human-readable type name for a Condition.
     */
    private fun getConditionTypeName(condition: Condition): String =
        when (condition) {
            is Condition.Status -> "STATUS_CODE"
            is Condition.JsonPath -> "JSON_PATH_${condition.operator.name}"
            is Condition.Header -> "HEADER_${condition.operator.name}"
            is Condition.BodyContains -> "BODY_CONTAINS"
            is Condition.Schema -> "MATCHES_SCHEMA"
            is Condition.ResponseTime -> "RESPONSE_TIME"
            is Condition.Variable -> "VARIABLE"
            is Condition.Negated -> "NOT_${getConditionTypeName(condition.condition)}"
            is Condition.Compound -> "COMPOUND"
            is Condition.CustomAssertion -> "CUSTOM"
            is Condition.Custom -> "CUSTOM_PREDICATE"
        }
}
