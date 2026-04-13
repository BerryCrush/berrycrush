package org.berrycrush.model

/**
 * Registry for storing and retrieving reusable fragments.
 *
 * Fragments are named sequences of steps that can be included
 * in scenarios to avoid duplication.
 */
class FragmentRegistry {
    private val fragments = mutableMapOf<String, Fragment>()

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
     * Get a fragment by name.
     *
     * @param name Fragment name
     * @return Fragment or null if not found
     */
    fun get(name: String): Fragment? = fragments[name]

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
    }
}
