package org.berrycrush.model

sealed interface FragmentEntry {
    val name: String
}

/**
 * A reusable fragment containing a sequence of steps.
 *
 * @property name Human-readable name of the fragment
 * @property steps Ordered list of steps in this fragment
 */
data class Fragment(
    override val name: String,
    val steps: List<Step> = emptyList(),
) : FragmentEntry {
    init {
        require(name.isNotBlank()) { "Fragment name cannot be blank" }
    }
}

/**
 * Fragment plus resolved default parameters declared in the fragment file.
 *
 * This keeps [Fragment] focused on executable steps while exposing
 * fragment-specific parameter defaults through a dedicated model.
 */
data class ParameterFragment(
    override val name: String,
    val parameters: Map<String, Any> = emptyMap(),
) : FragmentEntry {
    init {
        require(name.isNotBlank()) { "Parameter fragment name cannot be blank" }
    }
}
