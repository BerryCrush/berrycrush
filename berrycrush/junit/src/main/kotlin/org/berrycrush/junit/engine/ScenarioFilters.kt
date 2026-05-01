package org.berrycrush.junit.engine

/**
 * Configuration for filtering scenarios during test discovery and execution.
 *
 * Filters can be set via system properties:
 * - `berryCrush.scenarioFile` - Filter by scenario file path or name
 * - `berryCrush.scenarioName` - Filter by scenario name
 * - `berryCrush.featureName` - Filter by feature name
 *
 * @property scenarioFile Optional file path/name filter (matches path suffix or filename)
 * @property scenarioName Optional scenario name filter (matches exact name)
 * @property featureName Optional feature name filter (matches exact feature name)
 */
data class ScenarioFilters(
    val scenarioFile: String? = null,
    val scenarioName: String? = null,
    val featureName: String? = null,
) {
    /**
     * Returns true if any filter is set.
     */
    val hasFilters: Boolean
        get() = scenarioFile != null || scenarioName != null || featureName != null

    companion object {
        /** System property for filtering by scenario file */
        const val PROPERTY_SCENARIO_FILE = "berryCrush.scenarioFile"

        /** System property for filtering by scenario name */
        const val PROPERTY_SCENARIO_NAME = "berryCrush.scenarioName"

        /** System property for filtering by feature name */
        const val PROPERTY_FEATURE_NAME = "berryCrush.featureName"

        /**
         * Creates a [ScenarioFilters] instance from system properties.
         *
         * @return [ScenarioFilters] with values from system properties, or empty filters if none set
         */
        fun fromSystemProperties(): ScenarioFilters =
            ScenarioFilters(
                scenarioFile = System.getProperty(PROPERTY_SCENARIO_FILE),
                scenarioName = System.getProperty(PROPERTY_SCENARIO_NAME),
                featureName = System.getProperty(PROPERTY_FEATURE_NAME),
            )

        /**
         * Empty filter that matches all scenarios.
         */
        val EMPTY = ScenarioFilters()
    }

    /**
     * Checks if a scenario file path matches the file filter.
     *
     * Matching logic:
     * - If no file filter is set, returns true
     * - Matches if the path ends with the filter value
     * - Matches if the filename equals the filter value
     *
     * @param path The classpath path to the scenario file
     * @param filename The scenario filename
     * @return true if the file matches the filter or no filter is set
     */
    fun matchesFile(
        path: String,
        filename: String,
    ): Boolean {
        val filter = scenarioFile ?: return true
        return path.endsWith(filter) ||
            filename == filter ||
            path.contains(filter)
    }

    /**
     * Checks if a scenario name matches the name filter.
     *
     * @param name The scenario name from the parsed scenario
     * @return true if the name matches the filter or no filter is set
     */
    fun matchesScenarioName(name: String): Boolean {
        val filter = scenarioName ?: return true
        return name.equals(filter, ignoreCase = true)
    }

    /**
     * Checks if a feature name matches the feature filter.
     *
     * @param name The feature name from the parsed scenario
     * @return true if the name matches the filter or no filter is set
     */
    fun matchesFeatureName(name: String): Boolean {
        val filter = featureName ?: return true
        return name.equals(filter, ignoreCase = true)
    }
}
