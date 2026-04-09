package io.github.ktakashi.lemoncheck.plugin

import java.nio.file.Path
import java.time.Instant

/**
 * Execution context for a scenario, providing access to scenario metadata and runtime state.
 *
 * Provides plugins and other components with read and write access to scenario-level
 * information including variables, metadata, and execution timing.
 *
 * @property scenarioName Name of the scenario from the scenario file
 * @property scenarioFile Path to the source scenario file
 * @property variables Mutable map of variables extracted during scenario execution
 * @property metadata Read-only scenario metadata (tags, custom properties)
 * @property startTime Instant when scenario execution began
 * @property tags Scenario tags for filtering and organization
 */
interface ScenarioContext {
    val scenarioName: String
    val scenarioFile: Path
    val variables: MutableMap<String, Any>
    val metadata: Map<String, String>
    val startTime: Instant
    val tags: Set<String>
}
