package org.berrycrush.dsl

import org.berrycrush.junit.BerryCrushSuite
import org.berrycrush.model.Fragment
import org.berrycrush.model.Scenario
import org.berrycrush.model.Step
import org.berrycrush.model.StepType

/**
 * DSL scope for defining a scenario.
 */
@BerryCrushDsl
class ScenarioScope internal constructor(
    private val name: String,
    private val tags: Set<String>,
    override val suite: BerryCrushSuite,
) : ScenarioLikeScope {
    internal val steps = mutableListOf<Step>()
    internal val backgroundSteps = mutableListOf<Step>()
    override val parameterScope = ParameterScope()

    /**
     * Include a fragment's steps in this scenario.
     */
    override fun include(fragment: Fragment) {
        steps.addAll(fragment.steps)
    }

    override fun addStep(
        stepType: StepType,
        description: String,
        block: StepScope.() -> Unit,
    ) {
        val stepScope = StepScope(stepType, description, suite)
        block(stepScope)
        steps.add(stepScope.build())
    }

    internal fun build(): Scenario =
        Scenario(
            name = name,
            tags = tags,
            steps = steps.toList(),
            background = backgroundSteps.toList(),
            parameters = parameterScope.parameters.toMap(),
        )
}
