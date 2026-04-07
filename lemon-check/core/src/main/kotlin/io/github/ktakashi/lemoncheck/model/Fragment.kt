package io.github.ktakashi.lemoncheck.model

/**
 * A reusable fragment containing a sequence of steps.
 *
 * @property name Human-readable name of the fragment
 * @property steps Ordered list of steps in this fragment
 */
data class Fragment(
    val name: String,
    val steps: List<Step> = emptyList(),
) {
    init {
        require(name.isNotBlank()) { "Fragment name cannot be blank" }
    }
}
