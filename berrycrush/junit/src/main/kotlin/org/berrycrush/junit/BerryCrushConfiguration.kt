package org.berrycrush.junit

import org.berrycrush.plugin.BerryCrushPlugin
import kotlin.reflect.KClass

/**
 * Annotation for configuring BerryCrush scenario execution.
 *
 * Apply this annotation to a test class to customize scenario execution behavior.
 * This includes specifying custom bindings, plugin registration, step definitions,
 * and per-scenario timeout settings.
 *
 * For OpenAPI spec configuration, use [@BerryCrushSpec][BerryCrushSpec] annotation(s)
 * or configure specs in your [BerryCrushBindings] implementation.
 *
 * @property bindings Class implementing BerryCrushBindings to provide runtime values.
 *                    Must have a public no-arg constructor. Defaults to DefaultBindings.
 * @property timeout Timeout per scenario execution in milliseconds.
 *                   Default is 30000 (30 seconds).
 * @property plugins Array of plugin names to register (name-based registration).
 *                   Format: "plugin-name[:param1[:param2]]"
 *                   Examples: "report:json:output.json", "logging"
 * @property pluginClasses Array of plugin classes to instantiate and register.
 *                         Each class must have a public no-arg constructor.
 * @property stepClasses Array of step definition classes to scan for @Step methods.
 *                       Each class must have a public no-arg constructor.
 * @property stepPackages Array of package names to scan for step definition classes.
 *                        All classes in these packages with @Step methods will be registered.
 * @property assertionClasses Array of assertion definition classes to scan for @Assertion methods.
 *                            Each class must have a public no-arg constructor.
 * @property assertionPackages Array of package names to scan for assertion definition classes.
 *                             All classes in these packages with @Assertion methods will be registered.
 *
 * @see BerryCrushSpec
 * @see BerryCrushBindings
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class BerryCrushConfiguration(
    val bindings: KClass<out BerryCrushBindings> = DefaultBindings::class,
    val timeout: Long = 30_000L,
    val plugins: Array<String> = [],
    val pluginClasses: Array<KClass<out BerryCrushPlugin>> = [],
    val stepClasses: Array<KClass<*>> = [],
    val stepPackages: Array<String> = [],
    val assertionClasses: Array<KClass<*>> = [],
    val assertionPackages: Array<String> = [],
)
