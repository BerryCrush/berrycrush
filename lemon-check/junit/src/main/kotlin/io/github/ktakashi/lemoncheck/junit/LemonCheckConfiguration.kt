package io.github.ktakashi.lemoncheck.junit

import io.github.ktakashi.lemoncheck.plugin.LemonCheckPlugin
import kotlin.reflect.KClass

/**
 * Annotation for configuring LemonCheck scenario execution.
 *
 * Apply this annotation to a test class to customize scenario execution behavior.
 * This includes specifying custom bindings, OpenAPI spec path overrides,
 * plugin registration, step definitions, and per-scenario timeout settings.
 *
 * @property bindings Class implementing LemonCheckBindings to provide runtime values.
 *                    Must have a public no-arg constructor. Defaults to DefaultBindings.
 * @property openApiSpec Path to the OpenAPI specification file (classpath resource).
 *                       Overrides any spec from LemonCheckSpec if specified.
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
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class LemonCheckConfiguration(
    val bindings: KClass<out LemonCheckBindings> = DefaultBindings::class,
    val openApiSpec: String = "",
    val timeout: Long = 30_000L,
    val plugins: Array<String> = [],
    val pluginClasses: Array<KClass<out LemonCheckPlugin>> = [],
    val stepClasses: Array<KClass<*>> = [],
    val stepPackages: Array<String> = [],
)
