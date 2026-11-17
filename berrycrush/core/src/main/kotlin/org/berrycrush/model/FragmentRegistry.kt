package org.berrycrush.model

import org.berrycrush.exception.ConfigurationException

/**
 * Registry for storing and retrieving reusable fragments.
 *
 * Fragments are named sequences of steps that can be included
 * in scenarios to avoid duplication.
 */
class FragmentRegistry {
    private val fragments = mutableMapOf<String, Fragment>()
    private val fragmentParameters = mutableMapOf<String, ParameterFragment>()

    /**
     * Register a fragment.
     *
     * @param fragment Fragment to register
     * @throws IllegalArgumentException if a fragment with the same name already exists
     */
    fun register(fragment: Fragment) {
        require(!fragments.containsKey(fragment.name)) {
            "Fragment '${fragment.name}' is already registered"
        }
        fragments[fragment.name] = fragment
    }

    /**
     * Register multiple fragments.
     *
     * @param fragmentMap Map of fragment name to Fragment
     */
    fun registerAll(fragmentMap: Map<String, Fragment>) {
        for ((name, fragment) in fragmentMap) {
            if (!fragments.containsKey(name)) {
                fragments[name] = fragment
            }
        }
    }

    /**
     * Register default parameters for a specific fragment.
     */
    fun registerFragmentParameters(
        fragmentName: String,
        parameters: ParameterFragment,
    ) {
        require(!fragmentParameters.containsKey(parameters.name)) {
            "Parameter fragment '${parameters.name}' is already registered"
        }
        fragmentParameters[fragmentName] = parameters
    }

    fun registerAllFragmentParameters(fragmentParametersMap: Map<String, ParameterFragment>) {
        for ((name, parameters) in fragmentParametersMap) {
            if (!fragmentParameters.containsKey(name)) {
                fragmentParameters[name] = parameters
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveParameters(parameters: Map<String, Any?>): Map<String, Any> =
        if (parameters.containsKey("<<")) {
            val includes = parameters["<<"] as List<String>
            val resolved = mutableMapOf<String, Any>()
            includes.forEach { name ->
                val params = getFragmentParameters(name) ?: throw ConfigurationException("Parameter fragment '$name' not found.")
                val resolvedParams = resolveParameters(params.parameters)
                resolved.putAll(resolvedParams)
            }
            resolved.putAll(parameters.filterKeys { it != "<<" }.filterValues { it != null } as Map<String, Any>)
            resolved
        } else {
            parameters.filterValues { it != null } as Map<String, Any>
        }

    /**
     * Get a fragment by name.
     *
     * @param name Fragment name
     * @return Fragment or null if not found
     */
    fun get(name: String): Fragment? = fragments[name]

    /**
     * Get default parameters for a fragment.
     */
    fun getFragmentParameters(name: String): ParameterFragment? = fragmentParameters[name]

    /**
     * Check if a fragment exists.
     *
     * @param name Fragment name
     * @return true if fragment exists
     */
    fun contains(name: String): Boolean = fragments.containsKey(name)

    /**
     * Get all registered fragments.
     *
     * @return Map of fragment name to Fragment
     */
    fun all(): Map<String, Fragment> = fragments.toMap()

    /**
     * Clear all registered fragments.
     */
    fun clear() {
        fragments.clear()
        fragmentParameters.clear()
    }
}
