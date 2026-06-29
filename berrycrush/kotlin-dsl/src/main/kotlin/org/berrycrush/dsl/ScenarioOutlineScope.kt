package org.berrycrush.dsl

import org.berrycrush.model.ExampleRow
import org.berrycrush.model.Fragment
import org.berrycrush.model.Scenario
import org.berrycrush.model.Step
import org.berrycrush.model.StepType
import org.berrycrush.step.dsl.steps

/**
 * DSL scope for defining a scenario outline (parameterized scenario).
 */
@BerryCrushDsl
class ScenarioOutlineScope internal constructor(
    private val name: String,
    private val tags: Set<String>,
    override val suite: BerryCrushSuite,
): ScenarioLikeScope {
    companion object {
        private fun substituteParams(
            template: String,
            row: ExampleRow,
        ): String {
            var result = template
            row.values.forEach { (key, value) ->
                result = result.replace("<$key>", value.toString())
            }
            return result
        }
    }

    private val stepTemplates = mutableListOf<OutlineStep>()
    private val exampleRows = mutableListOf<ExampleRow>()
    override val parameterScope = ParameterScope()

    override fun include(fragment: Fragment) {
        stepTemplates.add(FragmentStep(fragment))
    }

    /**
     * Add example rows for parameterization.
     */
    fun examples(vararg rows: ExampleRow) {
        exampleRows.addAll(rows)
    }

    /**
     * Create an example row with named parameters.
     */
    fun row(vararg params: Pair<String, Any>): ExampleRow = ExampleRow(params.toMap())

    override fun addStep(
        stepType: StepType,
        description: String,
        block: StepScope.() -> Unit,
    ) {
        stepTemplates.add(StepTemplate(stepType, description, block))
    }

    internal fun build(): List<Scenario> {
        if (exampleRows.isEmpty()) {
            error("Scenario outline '$name' requires at least one example row")
        }

        return exampleRows.mapIndexed { index, row ->
            val expandedSteps =
                stepTemplates.flatMap { template -> template.build(row) }

            Scenario(
                name = "$name (Example ${index + 1})",
                tags = tags,
                steps = expandedSteps,
                background = emptyList(),
                parameters = parameterScope.parameters.toMap()
            )
        }
    }

    private sealed interface OutlineStep {
        fun build(row: ExampleRow): List<Step>
    }

    private class FragmentStep(private val fragment: Fragment): OutlineStep {
        override fun build(row: ExampleRow): List<Step> = fragment.steps
            .map { it.adjust(row) }

        private fun Step.adjust(row: ExampleRow): Step {
            val expandedDescription = substituteParams(description, row)
            return copy(description = expandedDescription)
        }
    }

    private inner class StepTemplate(
        val type: StepType,
        val description: String,
        val block: StepScope.() -> Unit,
    ) : OutlineStep {
        override fun build(row: ExampleRow): List<Step> {
            val expandedDescription = substituteParams(description, row)
            val stepScope = StepScope(type, expandedDescription, suite)
            block(stepScope)
            return listOf(stepScope.build())
        }
    }
}
