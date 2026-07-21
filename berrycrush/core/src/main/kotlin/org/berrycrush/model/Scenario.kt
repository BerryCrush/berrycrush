package org.berrycrush.model

sealed interface Story {
    val name: String
    val parameters: Map<String, Any>
}

data class Feature(
    override val name: String,
    val scenarios: List<Scenario>,
    val tags: Set<String> = emptySet(),
    override val parameters: Map<String, Any> = emptyMap(),
    val sourceLocation: SourceLocation? = null,
) : Story

/**
 * Represents a BDD scenario containing a sequence of steps.
 *
 * @property name Human-readable name of the scenario
 * @property tags Set of tags for filtering/grouping scenarios
 * @property steps Ordered list of steps to execute
 * @property background Optional background steps run before the scenario
 * @property examples Optional example rows for scenario outline parameterization
 * @property parameters Scenario-level parameters (merged with file and feature parameters)
 * @property sourceLocation Optional source location for error reporting
 */
data class Scenario(
    override val name: String,
    val tags: Set<String> = emptySet(),
    val steps: List<Step> = emptyList(),
    val background: List<Step> = emptyList(),
    val examples: List<ExampleRow>? = null,
    override val parameters: Map<String, Any> = emptyMap(),
    val sourceLocation: SourceLocation? = null,
) : Story {
    init {
        require(name.isNotBlank()) { "Scenario name cannot be blank" }
    }
}
