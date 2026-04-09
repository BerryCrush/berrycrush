package io.github.ktakashi.lemoncheck.step

/**
 * DSL builder for defining custom steps programmatically.
 *
 * Provides a type-safe way to define step patterns and their implementations
 * without using annotations.
 *
 * ## Example
 *
 * ```kotlin
 * val registry = DefaultStepRegistry()
 *
 * steps {
 *     step("I have {int} pets") { count: Int ->
 *         // Step implementation
 *     }
 *
 *     step("the pet name is {string}") { name: String ->
 *         // Step implementation
 *     }
 *
 *     step("I add {int} and {int}") { a: Int, b: Int ->
 *         // Step implementation returning sum
 *         a + b
 *     }
 * }.registerTo(registry)
 * ```
 */
@DslMarker
annotation class StepDslMarker

/**
 * Entry point for the step DSL.
 *
 * @param block The DSL block for defining steps
 * @return A StepBuilder containing all defined steps
 */
fun steps(block: StepBuilder.() -> Unit): StepBuilder = StepBuilder().apply(block)

/**
 * Builder class for defining steps using DSL syntax.
 */
@StepDslMarker
class StepBuilder {
    private val definitions = mutableListOf<StepDefinition>()

    /**
     * Defines a step with no parameters.
     *
     * @param pattern The step pattern
     * @param description Optional description
     * @param action The action to execute
     */
    fun step(
        pattern: String,
        description: String = "",
        action: () -> Any?,
    ) {
        val wrapper = StepWrapper0(action)
        definitions.add(createDefinition(pattern, description, wrapper, "invoke"))
    }

    /**
     * Defines a step with one parameter.
     *
     * @param pattern The step pattern with one placeholder
     * @param description Optional description
     * @param action The action to execute with the extracted parameter
     */
    fun <T1> step(
        pattern: String,
        description: String = "",
        action: (T1) -> Any?,
    ) {
        val wrapper = StepWrapper1(action)
        definitions.add(createDefinition(pattern, description, wrapper, "invoke", Any::class.java))
    }

    /**
     * Defines a step with two parameters.
     *
     * @param pattern The step pattern with two placeholders
     * @param description Optional description
     * @param action The action to execute with the extracted parameters
     */
    fun <T1, T2> step(
        pattern: String,
        description: String = "",
        action: (T1, T2) -> Any?,
    ) {
        val wrapper = StepWrapper2(action)
        definitions.add(
            createDefinition(
                pattern,
                description,
                wrapper,
                "invoke",
                Any::class.java,
                Any::class.java,
            ),
        )
    }

    /**
     * Defines a step with three parameters.
     *
     * @param pattern The step pattern with three placeholders
     * @param description Optional description
     * @param action The action to execute with the extracted parameters
     */
    fun <T1, T2, T3> step(
        pattern: String,
        description: String = "",
        action: (T1, T2, T3) -> Any?,
    ) {
        val wrapper = StepWrapper3(action)
        definitions.add(
            createDefinition(
                pattern,
                description,
                wrapper,
                "invoke",
                Any::class.java,
                Any::class.java,
                Any::class.java,
            ),
        )
    }

    /**
     * Defines a step with four parameters.
     *
     * @param pattern The step pattern with four placeholders
     * @param description Optional description
     * @param action The action to execute with the extracted parameters
     */
    fun <T1, T2, T3, T4> step(
        pattern: String,
        description: String = "",
        action: (T1, T2, T3, T4) -> Any?,
    ) {
        val wrapper = StepWrapper4(action)
        definitions.add(
            createDefinition(
                pattern,
                description,
                wrapper,
                "invoke",
                Any::class.java,
                Any::class.java,
                Any::class.java,
                Any::class.java,
            ),
        )
    }

    /**
     * Defines a step with five parameters.
     *
     * @param pattern The step pattern with five placeholders
     * @param description Optional description
     * @param action The action to execute with the extracted parameters
     */
    fun <T1, T2, T3, T4, T5> step(
        pattern: String,
        description: String = "",
        action: (T1, T2, T3, T4, T5) -> Any?,
    ) {
        val wrapper = StepWrapper5(action)
        definitions.add(
            createDefinition(
                pattern,
                description,
                wrapper,
                "invoke",
                Any::class.java,
                Any::class.java,
                Any::class.java,
                Any::class.java,
                Any::class.java,
            ),
        )
    }

    /**
     * Returns all defined step definitions.
     */
    fun build(): List<StepDefinition> = definitions.toList()

    /**
     * Registers all defined steps to the given registry.
     *
     * @param registry The registry to register steps to
     * @return The registry for chaining
     */
    fun registerTo(registry: StepRegistry): StepRegistry {
        registry.registerAll(definitions)
        return registry
    }

    private fun createDefinition(
        pattern: String,
        description: String,
        wrapper: Any,
        methodName: String,
        vararg paramTypes: Class<*>,
    ): StepDefinition {
        val method = wrapper.javaClass.getMethod(methodName, *paramTypes)
        return StepDefinition(
            pattern = pattern,
            method = method,
            instance = wrapper,
            description = description,
        )
    }
}

// Wrapper classes for different parameter counts
internal class StepWrapper0(
    private val action: () -> Any?,
) {
    fun invoke(): Any? = action()
}

internal class StepWrapper1<T1>(
    private val action: (T1) -> Any?,
) {
    @Suppress("UNCHECKED_CAST")
    fun invoke(p1: Any?): Any? = action(p1 as T1)
}

internal class StepWrapper2<T1, T2>(
    private val action: (T1, T2) -> Any?,
) {
    @Suppress("UNCHECKED_CAST")
    fun invoke(
        p1: Any?,
        p2: Any?,
    ): Any? = action(p1 as T1, p2 as T2)
}

internal class StepWrapper3<T1, T2, T3>(
    private val action: (T1, T2, T3) -> Any?,
) {
    @Suppress("UNCHECKED_CAST")
    fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
    ): Any? = action(p1 as T1, p2 as T2, p3 as T3)
}

internal class StepWrapper4<T1, T2, T3, T4>(
    private val action: (T1, T2, T3, T4) -> Any?,
) {
    @Suppress("UNCHECKED_CAST")
    fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
    ): Any? = action(p1 as T1, p2 as T2, p3 as T3, p4 as T4)
}

internal class StepWrapper5<T1, T2, T3, T4, T5>(
    private val action: (T1, T2, T3, T4, T5) -> Any?,
) {
    @Suppress("UNCHECKED_CAST")
    fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
    ): Any? = action(p1 as T1, p2 as T2, p3 as T3, p4 as T4, p5 as T5)
}
