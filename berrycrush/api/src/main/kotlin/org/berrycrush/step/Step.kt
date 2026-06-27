package org.berrycrush.step

/**
 * Marks a method as a custom step definition.
 *
 * Apply this annotation to methods in step definition classes to define custom steps.
 * The annotated method will be called when a step matches the specified pattern.
 *
 * ## Pattern Syntax
 *
 * Patterns support placeholder extraction:
 * - `{int}` - matches an integer value
 * - `{string}` - matches a quoted string ("..." or '...')
 * - `{word}` - matches a single word (alphanumeric + underscore)
 * - `{float}` - matches a floating-point number
 * - `{any}` - matches any text until end or next placeholder
 *
 * ## Example
 *
 * ```kotlin
 * class MySteps {
 *     @Step("I have {int} pets")
 *     fun setNumberOfPets(count: Int) {
 *         // ...
 *     }
 *
 *     @Step("the pet name is {string}")
 *     fun setPetName(name: String) {
 *         // ...
 *     }
 * }
 * ```
 *
 * @property pattern The step pattern with optional placeholders
 * @property description Optional description for documentation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Step(
    val pattern: String,
    val description: String = "",
)
