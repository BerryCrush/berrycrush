package org.berrycrush.step

/**
 * Default implementation of [StepRegistry].
 *
 * Maintains a list of registered step definitions and finds matches using [StepMatcher].
 */
class DefaultStepRegistry : StepRegistry {
    private val definitions = mutableListOf<RegisteredStep>()
    private val matcher = StepMatcher()

    override fun register(definition: StepDefinition) {
        val compiled = matcher.compile(definition.pattern)
        definitions.add(RegisteredStep(definition, compiled))
    }

    override fun registerAll(definitions: Collection<StepDefinition>) {
        definitions.forEach { register(it) }
    }

    override fun findMatch(stepText: String): StepMatch? {
        for (registered in definitions) {
            val parameters = matcher.match(stepText, registered.compiled)
            if (parameters != null) {
                return StepMatch(registered.definition, parameters)
            }
        }
        return null
    }

    override fun allDefinitions(): List<StepDefinition> = definitions.map { it.definition }.toList()

    override fun clear() {
        definitions.clear()
    }

    private data class RegisteredStep(
        val definition: StepDefinition,
        val compiled: CompiledPattern,
    )
}
