package org.berrycrush.step

import java.lang.reflect.Method

/**
 * Registry for custom step definitions.
 *
 * Manages registration and lookup of step definitions based on patterns.
 */
interface StepRegistry {
    /**
     * Registers a step definition.
     *
     * @param definition The step definition to register
     */
    fun register(definition: StepDefinition)

    /**
     * Registers multiple step definitions.
     *
     * @param definitions The step definitions to register
     */
    fun registerAll(definitions: Collection<StepDefinition>)

    /**
     * Finds a matching step definition for the given step text.
     *
     * @param stepText The step text to match
     * @return The matching step definition with extracted parameters, or null if no match
     */
    fun findMatch(stepText: String): StepMatch?

    /**
     * Returns all registered step definitions.
     *
     * @return Immutable list of all registered definitions
     */
    fun allDefinitions(): List<StepDefinition>

    /**
     * Clears all registered step definitions.
     */
    fun clear()
}

/**
 * A registered step definition.
 *
 * @property pattern The original pattern string with placeholders
 * @property method The method to invoke when the step matches
 * @property instance The instance to invoke the method on (null for static)
 * @property description Optional description of the step
 */
data class StepDefinition(
    val pattern: String,
    val method: Method,
    val instance: Any?,
    val description: String = "",
)

/**
 * Result of matching a step text against registered definitions.
 *
 * @property definition The matched step definition
 * @property parameters Extracted parameter values from the step text
 */
data class StepMatch(
    val definition: StepDefinition,
    val parameters: List<Any?>,
)
