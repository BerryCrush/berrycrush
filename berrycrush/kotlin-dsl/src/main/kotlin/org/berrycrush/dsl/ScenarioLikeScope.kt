package org.berrycrush.dsl

import org.berrycrush.model.Fragment
import org.berrycrush.model.StepType

@PublishedApi
internal interface ScenarioLikeScope {
    val suite: BerryCrushSuite
    val parameterScope: ParameterScope

    /**
     * Parameter override
     */
    fun parameters(block: ParameterScope.() -> Unit) {
        parameterScope.block()
    }

    /**
     * Define a GIVEN step (precondition).
     */
    fun given(
        description: String,
        block: StepScope.() -> Unit = {},
    ) {
        addStep(StepType.GIVEN, description, block)
    }

    /**
     * Define a WHEN step (action).
     */
    fun whenever(
        description: String,
        block: StepScope.() -> Unit = {},
    ) {
        addStep(StepType.WHEN, description, block)
    }

    /**
     * Define a THEN step (assertion).
     */
    fun afterwards(
        description: String,
        block: StepScope.() -> Unit = {},
    ) {
        addStep(StepType.THEN, description, block)
    }

    /**
     * Define an AND step (continuation of previous).
     */
    fun and(
        description: String,
        block: StepScope.() -> Unit = {},
    ) {
        addStep(StepType.AND, description, block)
    }

    /**
     * Define a BUT step (exception/negative case).
     */
    fun otherwise(
        description: String,
        block: StepScope.() -> Unit = {},
    ) {
        addStep(StepType.BUT, description, block)
    }

    // ========== Scenario File Compatibility Aliases ==========

    /**
     * Alias for [whenever] - matches scenario file `when` keyword.
     */
    fun `when`(
        description: String,
        block: StepScope.() -> Unit = {},
    ) = whenever(description, block)

    /**
     * Alias for [afterwards] - matches scenario file `then` keyword.
     */
    fun then(
        description: String,
        block: StepScope.() -> Unit = {},
    ) = afterwards(description, block)

    /**
     * Alias for [otherwise] - matches scenario file `but` keyword.
     */
    fun but(
        description: String,
        block: StepScope.() -> Unit = {},
    ) = otherwise(description, block)

    /**
     * Include a fragment's steps in this scenario.
     */
    fun include(fragment: Fragment)

    /**
     * Include a fragment by name.
     */
    fun include(fragmentName: String) {
        val fragment =
            suite.getFragment(fragmentName)
                ?: error("Fragment '$fragmentName' not found. Register it first with suite.fragment()")
        include(fragment)
    }

    fun addStep(stepType: StepType, description: String, block: StepScope.() -> Unit)
}